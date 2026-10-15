package com.gabi.assistant.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class SpeechEvent {
    object Started : SpeechEvent()
    data class Partial(val text: String) : SpeechEvent()
    data class Result(val text: String) : SpeechEvent()
    data class Error(val code: Int, val message: String) : SpeechEvent()
    object Ended : SpeechEvent()
}

/**
 * Reconhecimento de voz da Gabi.
 *
 * As causas mais comuns do "client error" (ERROR_CLIENT) do Android são tratadas aqui:
 *  - chamar startListening sem a permissão RECORD_AUDIO concedida;
 *  - chamar startListening de uma thread que não seja a main;
 *  - destruir e recriar o SpeechRecognizer a cada chamada sem dar tempo do serviço
 *    antigo ser liberado (resulta em "recognizer busy" / client error);
 *  - chamar startListening enquanto o serviço ainda está ativo.
 *
 * Estratégia: reutilizar UMA instância, operar sempre na main thread e, em erro
 * transiente (client/busy), destruir e recriar após um atraso antes de tentar de novo.
 */
@Singleton
class JavisSpeechRecognizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    private var recognizer: SpeechRecognizer? = null
    private var restartRunnable: Runnable? = null
    private var transientAttempts = 0
    // O próprio stopListening() pode disparar um onError (ex.: ERROR_CLIENT) como
    // efeito colateral de cancelar a sessão em andamento — sem essa flag, esse erro
    // era tratado como "transiente" e reagendava um reinício sozinho, mesmo com o
    // usuário tendo acabado de mandar parar. Confirmado ao vivo em log (13/10/2026):
    // "Erro transiente" chegava 14ms depois do stopListening().
    private var stoppedByUser = false

    private val _events = MutableStateFlow<SpeechEvent?>(null)
    val events: StateFlow<SpeechEvent?> = _events

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            transientAttempts = 0
            _isListening.value = true
            _events.value = SpeechEvent.Started
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d(LATENCY_TAG, "T0 onEndOfSpeech t=${SystemClock.elapsedRealtime()}")
            _isListening.value = false
            _events.value = SpeechEvent.Ended
        }

        override fun onError(error: Int) {
            _isListening.value = false

            if (stoppedByUser) {
                stoppedByUser = false
                return
            }

            // Erros transientes do serviço de reconhecimento: recria e tenta de novo.
            if (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                if (transientAttempts < MAX_TRANSIENT_RETRIES) {
                    transientAttempts++
                    Log.w(TAG, "Erro transiente do reconhecimento ($error) — recriando e tentando de novo")
                    scheduleRestart()
                    return
                }
                transientAttempts = 0
            }
            emitError(error, errorMessage(error))
        }

        override fun onResults(results: Bundle?) {
            transientAttempts = 0
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull().orEmpty()
            if (text.isNotBlank()) {
                Log.d(LATENCY_TAG, "T1 onResults t=${SystemClock.elapsedRealtime()} textLen=${text.length}")
                _events.value = SpeechEvent.Result(text)
            } else {
                // onResults pode disparar com a lista de resultados VAZIA em vez de
                // onError — comportamento real confirmado no aparelho do usuário
                // (Xiaomi/HyperOS, sessão 20-21/09/2026 e 15/10/2026): ela "ouviu" mas o
                // serviço de voz do sistema não transcreveu nada. Como nenhum evento era
                // emitido nesse caso, o modo contínuo ficava preso pra sempre em
                // "Escutando…" sem reiniciar — o usuário via a tela travada, tinha que
                // tocar manualmente de novo. Emite o mesmo erro -1 que o resto do app já
                // trata como "transiente, só tenta ouvir de novo" (ChatViewModel reage
                // reabrindo o monitor de microfone).
                emitError(-1, "Não ouvi nada. Pode falar de novo?")
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull().orEmpty()
            if (partial.isNotBlank()) _events.value = SpeechEvent.Partial(partial)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun startListening() {
        if (_isListening.value) return
        stoppedByUser = false

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            emitError(-1, "Reconhecimento de voz indisponível neste aparelho.")
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            emitError(
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
                "Sem permissão de microfone. Permita o acesso e tente de novo."
            )
            return
        }

        // O SpeechRecognizer exige ser operado na main thread.
        restartRunnable?.let { mainHandler.removeCallbacks(it) }
        restartRunnable = null
        mainHandler.post { safeStart() }
    }

    private fun safeStart() {
        if (_isListening.value) return
        try {
            if (recognizer == null) {
                recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(listener)
                }
            }
            recognizer?.startListening(buildIntent())
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao iniciar o reconhecimento", e)
            emitError(SpeechRecognizer.ERROR_CLIENT, "Não consegui iniciar o reconhecimento de voz. Tente de novo.")
        }
    }

    private fun scheduleRestart() {
        restartRunnable?.let { mainHandler.removeCallbacks(it) }
        val restart = Runnable {
            restartRunnable = null
            teardownRecognizer()
            if (_isListening.value) return@Runnable
            safeStart()
        }
        restartRunnable = restart
        mainHandler.postDelayed(restart, RESTART_DELAY_MS)
    }

    private fun buildIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR") // A Gabi entende português do Brasil
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-BR")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
        // 1500ms/1000ms cortava a fala no meio de uma pausa natural de frase (o usuário
        // ainda segurando o botão e falando) — aumentado pra dar mais margem antes do
        // reconhecedor decidir que a fala acabou.
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
    }

    fun stopListening() {
        _isListening.value = false
        stoppedByUser = true
        restartRunnable?.let { mainHandler.removeCallbacks(it) }
        restartRunnable = null
        try {
            recognizer?.stopListening()
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao parar o reconhecimento: ${e.message}")
        }
    }

    private fun teardownRecognizer() {
        try {
            recognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao destruir o recognizer: ${e.message}")
        }
        recognizer = null
    }

    fun destroy() {
        _isListening.value = false
        restartRunnable?.let { mainHandler.removeCallbacks(it) }
        restartRunnable = null
        teardownRecognizer()
    }

    private fun emitError(code: Int, message: String) {
        _events.value = SpeechEvent.Error(code, message)
    }

    private fun errorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Problema com o áudio do microfone."
        SpeechRecognizer.ERROR_CLIENT -> "Erro do cliente de reconhecimento de voz. Tente de novo."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Sem permissão de microfone. Permita o acesso e tente de novo."
        SpeechRecognizer.ERROR_NETWORK -> "Sem conexão com a internet. Tente de novo."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "A conexão com o reconhecimento de voz deu timeout."
        SpeechRecognizer.ERROR_NO_MATCH -> "Não entendi o que você falou. Pode repetir?"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "O reconhecimento de voz está ocupado. Espere um instante e tente de novo."
        SpeechRecognizer.ERROR_SERVER -> "Erro no servidor de reconhecimento de voz. Tente de novo."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Não ouvi nada. Pode falar de novo?"
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "O idioma português não é suportado neste aparelho."
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "O pacote de idioma pt-BR não está instalado no aparelho."
        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Muitas solicitações. Espere um pouco e tente de novo."
        else -> "Erro ao reconhecer a fala (código $error)."
    }

    companion object {
        private const val TAG = "JavisSpeechRecognizer"
        private const val MAX_TRANSIENT_RETRIES = 2
        private const val RESTART_DELAY_MS = 300L
        // Fase 0 do plano de latência (alexa-referencia/PLANO_DE_ALTERACOES.md) — só
        // medição, filtrar no logcat com `adb logcat -s GabiLatency`.
        private const val LATENCY_TAG = "GabiLatency"
    }
}
