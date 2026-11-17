package com.gabi.assistant.voice

import android.content.Context
import android.media.MediaPlayer
import android.os.SystemClock
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ElevenLabsTts @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null
    // Callback da reprodução em andamento que ainda não disparou (onCompletion/onError
    // do MediaPlayer). stopSpeaking() precisa chamar isso explicitamente — ver comentário
    // no próprio stopSpeaking() pra entender por quê.
    private var onDoneCallback: (() -> Unit)? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    var apiKey: String = ""
    var voiceId: String = DEFAULT_VOICE_ID

    /**
     * Retorna Result.failure quando não conseguiu tocar áudio nenhum (chave vazia,
     * erro HTTP, voz não adicionada à VoiceLab, etc.) — quem chama decide se cai
     * pro TTS nativo em vez de ficar em silêncio total, que era o comportamento
     * antigo (erro só ia pro Logcat, nunca aparecia pro usuário nem tinha fallback).
     */
    suspend fun speak(text: String, onDone: (() -> Unit)? = null): Result<Unit> = withContext(Dispatchers.IO) {
        stopSpeaking()

        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Chave da API ElevenLabs vazia"))
        }

        try {
            val body = JSONObject().apply {
                put("text", text)
                // eleven_turbo_v2 só "adivinha" o idioma pelo texto (sem travar), e às
                // vezes confunde português com espanhol em frases curtas — v2_5 aceita
                // language_code pra travar pt de verdade. Testado ao vivo em 21/08/2026.
                put("model_id", "eleven_turbo_v2_5")
                put("language_code", "pt")
                put("voice_settings", JSONObject().apply {
                    put("stability", 0.5)
                    put("similarity_boost", 0.75)
                    put("style", 0.3)
                    put("use_speaker_boost", true)
                })
            }.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://api.elevenlabs.io/v1/text-to-speech/$voiceId")
                .header("xi-api-key", apiKey)
                .header("Accept", "audio/mpeg")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val audioBytes = response.body?.bytes()
                Log.d(LATENCY_TAG, "T5 ElevenLabs bytes recebidos t=${SystemClock.elapsedRealtime()} bytes=${audioBytes?.size ?: 0}")
                if (audioBytes != null && audioBytes.isNotEmpty()) {
                    val tempFile = File(context.cacheDir, "javis_tts_${System.currentTimeMillis()}.mp3")
                    tempFile.writeBytes(audioBytes)
                    // playAudioFile() só INICIA o MediaPlayer e volta na hora — sem esperar
                    // o áudio terminar de verdade, speak() retornava (e isSpeaking virava
                    // false) assim que o play começava, não quando acabava. Isso quebrava
                    // qualquer coisa que dependesse de "ela ainda tá falando" (voltar a
                    // ouvir cedo demais, o avatar trocar de pose cedo demais). Suspende até
                    // o onDone real (onCompletion/onError do MediaPlayer) disparar.
                    //
                    // withTimeoutOrNull é rede de segurança: se o MediaPlayer for
                    // derrubado por fora (ex.: stopSpeaking() chamado de outro lugar
                    // enquanto isso espera) sem passar pelos listeners de completion/erro,
                    // o app inteiro travava esperando um callback que nunca vinha —
                    // reportado em 25/08/2026 ("trava a Gabi depois que peço pra tocar ou
                    // mandar mensagem"). Sem isso, um MediaPlayer travado prendia a
                    // conversa inteira pra sempre.
                    //
                    // Limite FIXO de 20s (valor original) cortava qualquer resposta falada
                    // que passasse disso NO MEIO da fala — ela simplesmente parava de falar,
                    // mesmo com tudo funcionando normal, só porque o áudio era longo.
                    // Reportado pelo diagnóstico de voz (alexa-referencia/, 15/10/2026).
                    // Agora o limite escala com o tamanho do texto (chute conservador de
                    // ~12 caracteres/segundo falado, com folga), com piso de 20s (igual
                    // antes, pra textos curtos) e teto de 3min (ainda protege contra um
                    // MediaPlayer travado de verdade pra sempre).
                    withContext(Dispatchers.Main) {
                        val timeoutMs = (text.length * 85L + 20_000L).coerceAtMost(180_000L)
                        val completed = withTimeoutOrNull(timeoutMs) {
                            suspendCancellableCoroutine<Unit> { cont ->
                                val resumeOnce: () -> Unit = {
                                    // Só chama onDone/resume se ainda for a corrida ativa — se o
                                    // timeout acima já cancelou, evita chamar onDone duas vezes.
                                    if (cont.isActive) {
                                        onDoneCallback = null
                                        cont.resume(Unit)
                                        onDone?.invoke()
                                    }
                                }
                                // stopSpeaking() (chamado de fora, ex.: barge-in da Fase 4 ou
                                // "Gabi, vai dormir") só faz mediaPlayer.stop()+release() — isso
                                // NÃO dispara onCompletion/onError, então sem guardar este
                                // callback aqui a coroutine ficava pendurada até o timeout (até
                                // 3min) antes de "ela terminar de falar" pro resto do app.
                                // Bug real confirmado em 15/10/2026 (barge-in "travava" a escuta).
                                onDoneCallback = resumeOnce
                                playAudioFile(tempFile, resumeOnce)
                                cont.invokeOnCancellation { stopSpeaking() }
                            }
                        }
                        if (completed == null) {
                            Log.w(TAG, "Timeout esperando o áudio da ElevenLabs terminar — seguindo em frente")
                            stopSpeaking()
                            onDone?.invoke()
                        }
                    }
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("ElevenLabs devolveu áudio vazio"))
                }
            } else {
                val errorBody = response.body?.string()?.take(300) ?: ""
                Log.e(TAG, "ElevenLabs error: ${response.code} $errorBody")
                Result.failure(Exception("ElevenLabs HTTP ${response.code}: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "ElevenLabs TTS failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * `prepare()` (síncrono) bloqueia a thread até o MediaPlayer terminar de bufferizar
     * o áudio — chamado na Main thread (ver `speak()`), isso podia travar a UI inteira
     * por um instante a cada fala. Trocado pro par assíncrono `prepareAsync()` +
     * `setOnPreparedListener`, que não bloqueia nada.
     */
    private fun playAudioFile(file: File, onDone: (() -> Unit)?) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnPreparedListener {
                    Log.d(LATENCY_TAG, "T6 primeiro som t=${SystemClock.elapsedRealtime()}")
                    it.start()
                }
                setOnCompletionListener {
                    it.release()
                    file.delete()
                    onDone?.invoke()
                }
                setOnErrorListener { mp, _, _ ->
                    mp.release()
                    file.delete()
                    onDone?.invoke()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaPlayer error: ${e.message}")
            file.delete()
            onDone?.invoke()
        }
    }

    /**
     * MediaPlayer.stop() NÃO dispara onCompletion/onError — chamar isso de fora (barge-in
     * da Fase 4, "Gabi, vai dormir", ou o próprio speak() interrompendo uma fala anterior)
     * sem mais nada deixava a coroutine do speak() em andamento pendurada até o timeout
     * (até 3min) esperando um callback que nunca vinha. Dispara o callback pendente
     * manualmente pra "ela terminar de falar" ser reconhecido na hora.
     */
    fun stopSpeaking() {
        val pending = onDoneCallback
        onDoneCallback = null
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Stop error: ${e.message}")
        }
        pending?.invoke()
    }

    fun isSpeaking(): Boolean = mediaPlayer?.isPlaying == true

    companion object {
        private const val TAG = "ElevenLabsTts"
        // Fase 0 do plano de latência (alexa-referencia/PLANO_DE_ALTERACOES.md) — só
        // medição, filtrar no logcat com `adb logcat -s GabiLatency`.
        private const val LATENCY_TAG = "GabiLatency"

        // 🎙️ Voz da GABI — feminina, brasileira (pt-BR), calorosa e conversacional.
        // Default: "Roberta — For Conversational" (jovem, amigável, natural em pt-BR).
        // Voice IDs obtidos da Voice Library da ElevenLabs (vozes nativas em português do Brasil).
        const val DEFAULT_VOICE_ID = "RGymW84CSmfVugnA5tvA" // Roberta — conversacional, amigável

        // Outras vozes femininas nativas pt-BR (troque o DEFAULT se preferir outra):
        const val VOICE_ROBERTA  = "RGymW84CSmfVugnA5tvA" // Roberta — jovem, amigável, conversacional
        const val VOICE_RAQUEL   = "GDzHdQOi6jjf8zaXhCYD" // Raquel — forte, jovem, amigável, conversacional
        const val VOICE_KEREN    = "33B4UnXyTNbgLmdEDh5P" // Keren — doce, vibrante, jovem
        const val VOICE_CARLA    = "eVXYtPVYB9wDoz9NVTIy" // Carla — jovem, animada
        const val VOICE_ANA_DIAS = "MZxV5lN3cv7hi1376O0m" // Ana Dias — levemente grave, envolvente

        // ⚠️ Para usar uma voz da Voice Library via API: acesse
        // elevenlabs.io/app/voice-library, encontre a voz, clique em "Add to VoiceLab"
        // (grátis) e então o Voice ID passa a funcionar com a sua chave de API.
    }
}
