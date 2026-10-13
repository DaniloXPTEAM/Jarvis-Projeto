package com.gabi.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class AndroidTtsFallback @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    // Iniciar o motor é assíncrono (callback do Android) — sem isso, um speak() chamado
    // antes do callback terminar (ex.: a saudação, logo ao abrir o app) desistia
    // SILENCIOSAMENTE, sem erro nenhum: causa raiz de "não sai som nenhum" em 21/08/2026.
    private val readyState = CompletableDeferred<Boolean>()
    var speechRate: Float = 1.0f
    var pitch: Float = 1.05f   // leve realce; a seleção de voz já resolve o timbre feminino

    init { initTts() }

    /**
     * O motor "Google Text-to-Speech" (com.google.android.tts) traz vozes neurais de
     * qualidade bem superior às de outros fabricantes (Samsung, etc.), que costumam soar
     * robóticas. Se o Google TTS estiver instalado, usamos ele explicitamente em vez do
     * motor padrão do sistema (que pode ser outro).
     */
    private fun preferredEngine(): String? = try {
        context.packageManager.getPackageInfo(GOOGLE_TTS_PACKAGE, 0)
        GOOGLE_TTS_PACKAGE
    } catch (e: Exception) {
        null
    }

    private fun initTts() {
        val engine = preferredEngine()
        val onInit = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Tenta pt-BR, mas mesmo se o pacote de idioma não estiver instalado no
                // aparelho (LANG_MISSING_DATA), ainda marca pronto — falar com o idioma
                // padrão do motor é melhor do que ficar em silêncio total pra sempre.
                tts?.setLanguage(Locale("pt", "BR"))
                selectBestVoice()
                tts?.setSpeechRate(speechRate)
                tts?.setPitch(pitch)
                readyState.complete(true)
            } else {
                readyState.complete(false)
            }
        }
        tts = if (engine != null) TextToSpeech(context, onInit, engine) else TextToSpeech(context, onInit)
    }

    /**
     * Escolhe a melhor voz pt-BR disponível no motor, priorizando qualidade
     * (Voice.quality: VERY_HIGH > HIGH > NORMAL > LOW > VERY_LOW) — é essa qualidade,
     * não o pitch, que faz a diferença entre uma voz natural e uma robótica. Entre vozes
     * empatadas em qualidade, prioriza nomes que sugerem timbre feminino.
     */
    private fun selectBestVoice() {
        val voices = tts?.voices ?: return
        val ptBrVoices = voices.filter { v ->
            val loc = v.locale
            loc != null && loc.language == "pt" && loc.country == "BR"
        }
        if (ptBrVoices.isEmpty()) return

        val best = ptBrVoices
            .sortedWith(
                compareByDescending<Voice> { it.quality }
                    .thenByDescending { v ->
                        v.name.contains("female", ignoreCase = true) ||
                            v.name.contains("pt-br-x", ignoreCase = true)
                    }
            )
            .firstOrNull()

        best?.let { tts?.voice = it }
    }

    suspend fun speak(text: String) {
        // Espera até 5s a inicialização assíncrona do motor terminar, em vez de checar
        // um boolean e desistir na hora — evita o silêncio total na primeira fala.
        val ready = withTimeoutOrNull(5000) { readyState.await() } ?: false
        if (!ready || tts == null) return
        // Rede de segurança (13/10/2026, mesmo padrão já usado no ElevenLabsTts): se a
        // fala for interrompida de um jeito que nenhum callback do motor trate, isso
        // evita prender a conversa inteira esperando um callback que nunca chega.
        val completed = withTimeoutOrNull(20_000) { speakInternal(text) }
        if (completed == null) {
            Log.w(TAG, "Timeout esperando o TTS nativo terminar — seguindo em frente")
            tts?.stop()
        }
    }

    private suspend fun speakInternal(text: String): Unit = suspendCancellableCoroutine { cont ->
        tts?.setSpeechRate(speechRate)
        tts?.setPitch(pitch)

        val utteranceId = "javis_${System.currentTimeMillis()}"
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(u: String?) {}
            override fun onDone(u: String?) { if (u == utteranceId && cont.isActive) cont.resume(Unit) }
            override fun onError(u: String?) { if (u == utteranceId && cont.isActive) cont.resume(Unit) }
            // Faltava este callback: QUEUE_FLUSH (uma fala nova interrompendo a
            // anterior) ou stop() chamam onStop, não onDone/onError — sem tratar isso,
            // a coroutine ficava pendurada pra sempre esperando um callback que não
            // vinha, travando o resto da conversa.
            override fun onStop(u: String?, interrupted: Boolean) { if (u == utteranceId && cont.isActive) cont.resume(Unit) }
        })
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        cont.invokeOnCancellation { tts?.stop() }
    }

    fun stop() { tts?.stop() }
    fun isSpeaking() = tts?.isSpeaking == true

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    companion object {
        private const val TAG = "AndroidTtsFallback"
        private const val GOOGLE_TTS_PACKAGE = "com.google.android.tts"
    }
}
