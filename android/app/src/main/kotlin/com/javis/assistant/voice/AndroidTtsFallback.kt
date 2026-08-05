package com.javis.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
    var pitch: Float = 1.15f   // > 1.0 = tom mais agudo/feminino; a Gabi tem voz feminina calorosa

    init { initTts() }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // A Gabi fala em português do Brasil.
                val result = tts?.setLanguage(Locale("pt", "BR"))
                // Tenta selecionar uma voz feminina pt-BR, se o motor de TTS do aparelho oferecer.
                tts?.voices?.let { voices ->
                    voices.firstOrNull { v ->
                        val loc = v.locale
                        loc != null && loc.language == "pt" && loc.country == "BR" &&
                            (v.name.contains("female", ignoreCase = true) ||
                             v.name.contains("pt-br-x", ignoreCase = true))
                    }?.let { tts?.voice = it }
                }
                tts?.setSpeechRate(speechRate)
                tts?.setPitch(pitch)
                isReady = result != TextToSpeech.LANG_MISSING_DATA &&
                          result != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
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
}
