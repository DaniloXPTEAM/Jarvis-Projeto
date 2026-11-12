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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class SpeechEvent {
    object Started : SpeechEvent()
    data class Partial(val text: String) : SpeechEvent()
    data class Result(val text: String) : SpeechEvent()
    data class Error(val code: Int, val message: String) : SpeechEvent()
    object Ended : SpeechEvent()
    object Stopped : SpeechEvent()
}

@Singleton
class JavisSpeechRecognizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val handler = Handler(Looper.getMainLooper())
    private val session = RecognitionSession()
    private var recognizer: SpeechRecognizer? = null
    private var restart: Runnable? = null
    private var watchdog: Runnable? = null
    private var requested = false
    private var transientAttempts = 0
    // Events must not be conflated: repeated errors/results are separate turns.
    private val _events = MutableSharedFlow<SpeechEvent>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events = _events.asSharedFlow()
    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else handler.post { block() }
    }
    fun startListening() = onMain {
        if (session.isActive || restart != null) return@onMain
        requested = true
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            emitError(-3, "Reconhecimento de voz indisponível neste aparelho.")
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            emitError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS, "Permita o acesso ao microfone para conversar.")
        } else safeStart()
    }

    private fun safeStart() {
        if (!requested) return
        val id = session.start(SystemClock.elapsedRealtime()) ?: return
        try {
            if (recognizer == null) recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer?.setRecognitionListener(listener(id))
            Log.d(TAG, "ASR start session=$id")
            armWatchdog(id)
            recognizer?.startListening(buildIntent())
        } catch (e: Exception) {
            fail(id, SpeechRecognizer.ERROR_CLIENT)
        }
    }

    private fun listener(id: Long) = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            if (!session.accepts(id)) return
            session.ready(id, SystemClock.elapsedRealtime())
            armWatchdog(id)
            _isListening.value = true
            _events.tryEmit(SpeechEvent.Started)
        }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            if (!session.accepts(id)) return
            session.ended(id, SystemClock.elapsedRealtime())
            armWatchdog(id)
            _isListening.value = false
            Log.d(LATENCY_TAG, "T0 onEndOfSpeech t=${SystemClock.elapsedRealtime()}")
            _events.tryEmit(SpeechEvent.Ended)
        }
        override fun onError(error: Int) = fail(id, error)
        override fun onResults(results: Bundle?) {
            if (!finish(id)) return
            transientAttempts = 0
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            if (text.isBlank()) emitError(-1, "Não ouvi nada. Pode falar de novo?")
            else {
                Log.d(LATENCY_TAG, "T1 onResults t=${SystemClock.elapsedRealtime()} textLen=${text.length}")
                _events.tryEmit(SpeechEvent.Result(text))
            }
        }
        override fun onPartialResults(partialResults: Bundle?) {
            if (!session.accepts(id)) return
            val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            session.partial(id, text, SystemClock.elapsedRealtime())
            armWatchdog(id)
            if (text.isNotBlank()) _events.tryEmit(SpeechEvent.Partial(text))
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun armWatchdog(id: Long) {
        watchdog?.let(handler::removeCallbacks)
        val check = Runnable {
            if (!session.accepts(id)) return@Runnable
            if (session.timedOut(id, SystemClock.elapsedRealtime())) {
                finish(id)
                teardown()
                Log.w(TAG, "ASR watchdog: session=$id sem resultado final; liberando reconhecedor")
                emitError(WATCHDOG_TIMEOUT, "A escuta demorou para responder. Tentando novamente.")
            } else armWatchdog(id)
        }
        watchdog = check
        handler.postDelayed(check, session.remaining(id, SystemClock.elapsedRealtime()))
    }
    private fun finish(id: Long): Boolean {
        if (!session.finish(id)) return false
        watchdog?.let(handler::removeCallbacks)
        watchdog = null
        _isListening.value = false
        return true
    }
    private fun fail(id: Long, error: Int) {
        if (!finish(id)) return
        if (requested && (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) && transientAttempts < 2) {
            transientAttempts++
            teardown()
            restart = Runnable { restart = null; safeStart() }.also { handler.postDelayed(it, 500L) }
        } else {
            transientAttempts = 0
            emitError(error, "Não consegui reconhecer a fala (código $error). Tente novamente.")
        }
    }
    private fun emitError(code: Int, message: String) { _events.tryEmit(SpeechEvent.Error(code, message)) }
    private fun teardown() {
        val old = recognizer
        recognizer = null
        runCatching { old?.cancel() }
        runCatching { old?.destroy() }
    }
    /** Explicit stop invalidates late callbacks and all scheduled restarts. */
    fun stopListening(notify: Boolean = false) = onMain {
        requested = false
        session.cancel()
        transientAttempts = 0
        restart?.let(handler::removeCallbacks)
        restart = null
        watchdog?.let(handler::removeCallbacks)
        watchdog = null
        _isListening.value = false
        teardown()
        if (notify) _events.tryEmit(SpeechEvent.Stopped)
    }
    fun destroy() = stopListening()

    private fun buildIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-BR")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
    }
    companion object {
        const val WATCHDOG_TIMEOUT = -2
        private const val TAG = "JavisSpeechRecognizer"
        private const val LATENCY_TAG = "GabiLatency"
    }
}
