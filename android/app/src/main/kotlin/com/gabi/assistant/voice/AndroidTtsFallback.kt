package com.gabi.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class AndroidTtsFallback @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    private var isReady = false
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
                // A Gabi fala em português do Brasil.
                val result = tts?.setLanguage(Locale("pt", "BR"))
                selectBestVoice()
                tts?.setSpeechRate(speechRate)
                tts?.setPitch(pitch)
                isReady = result != TextToSpeech.LANG_MISSING_DATA &&
                          result != TextToSpeech.LANG_NOT_SUPPORTED
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

    suspend fun speak(text: String): Unit = suspendCancellableCoroutine { cont ->
        if (!isReady || tts == null) {
            cont.resume(Unit)
            return@suspendCancellableCoroutine
        }
        tts?.setSpeechRate(speechRate)
        tts?.setPitch(pitch)

        val utteranceId = "javis_${System.currentTimeMillis()}"
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(u: String?) {}
            override fun onDone(u: String?) { if (u == utteranceId) cont.resume(Unit) }
            override fun onError(u: String?) { if (u == utteranceId) cont.resume(Unit) }
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
        isReady = false
    }

    companion object {
        private const val GOOGLE_TTS_PACKAGE = "com.google.android.tts"
    }
}
