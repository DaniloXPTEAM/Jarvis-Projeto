package com.gabi.assistant.voice

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class ElevenLabsTts @Inject constructor(@ApplicationContext private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var activeSpeech: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS).callTimeout(60, TimeUnit.SECONDS).build()
    var apiKey: String = ""
    var voiceId: String = DEFAULT_VOICE_ID

    suspend fun speak(text: String, onDone: (() -> Unit)? = null, onStart: (() -> Unit)? = null): Result<Unit> =
        withContext(Dispatchers.Main.immediate) {
            coroutineScope {
                activeSpeech?.cancel()
                val owner = coroutineContext.job
                activeSpeech = owner
                var file: File? = null
                try {
                    if (apiKey.isBlank()) return@coroutineScope Result.failure(IllegalStateException("Chave ElevenLabs vazia"))
                    val body = JSONObject().apply {
                        put("text", text)
                        put("model_id", "eleven_turbo_v2_5")
                        put("language_code", "pt")
                        put("voice_settings", JSONObject().apply {
                            put("stability", 0.5); put("similarity_boost", 0.75)
                            put("style", 0.3); put("use_speaker_boost", true)
                        })
                    }.toString().toRequestBody("application/json".toMediaType())
                    val request = Request.Builder().url("https://api.elevenlabs.io/v1/text-to-speech/$voiceId")
                        .header("xi-api-key", apiKey).header("Accept", "audio/mpeg").post(body).build()
                    val bytes = client.newCall(request).awaitAudioBytes()
                    ensureActive()
                    Log.d(LATENCY_TAG, "T5 ElevenLabs bytes recebidos t=${SystemClock.elapsedRealtime()} bytes=${bytes.size}")
                    val audioFile = File.createTempFile("gabi_tts_", ".mp3", context.cacheDir)
                    file = audioFile
                    withContext(Dispatchers.IO) { audioFile.writeBytes(bytes) }
                    ensureActive()
                    val timeout = (text.length * 85L + 20_000L).coerceAtMost(180_000L)
                    val result = withTimeoutOrNull(timeout) { playAudioFile(audioFile, onStart) }
                        ?: Result.failure(IOException("Tempo limite da reprodução ElevenLabs"))
                    if (result.isSuccess) onDone?.invoke()
                    result
                } catch (e: CancellationException) {
                    // An interruption must never trigger fallback speech.
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "TTS falhou: ${e.javaClass.simpleName}")
                    Result.failure(e)
                } finally {
                    file?.delete()
                    if (activeSpeech === owner) activeSpeech = null
                }
            }
        }

    private suspend fun playAudioFile(file: File, onStart: (() -> Unit)?): Result<Unit> = suspendCancellableCoroutine { cont ->
        val player = MediaPlayer()
        mediaPlayer = player
        var finished = false
        fun finish(result: Result<Unit>) {
            if (finished) return
            finished = true
            player.setOnPreparedListener(null)
            player.setOnCompletionListener(null)
            player.setOnErrorListener(null)
            runCatching { player.release() }
            if (mediaPlayer === player) mediaPlayer = null
            if (cont.isActive) cont.resume(result)
        }
        cont.invokeOnCancellation {
            if (Looper.myLooper() == Looper.getMainLooper()) finish(Result.success(Unit))
            else handler.post { finish(Result.success(Unit)) }
        }
        try {
            player.setDataSource(file.absolutePath)
            player.setOnPreparedListener {
                if (!cont.isActive) { finish(Result.success(Unit)); return@setOnPreparedListener }
                try {
                    it.start()
                    Log.d(LATENCY_TAG, "T6 playback start solicitado t=${SystemClock.elapsedRealtime()}")
                    onStart?.invoke()
                } catch (e: Exception) { finish(Result.failure(e)) }
            }
            player.setOnCompletionListener { finish(Result.success(Unit)) }
            player.setOnErrorListener { _, what, extra ->
                finish(Result.failure(IOException("MediaPlayer error $what/$extra")))
                true
            }
            player.prepareAsync()
        } catch (e: Exception) { finish(Result.failure(e)) }
    }

    fun stopSpeaking() {
        if (Looper.myLooper() == Looper.getMainLooper()) activeSpeech?.cancel()
        else handler.post { activeSpeech?.cancel() }
    }
    fun isSpeaking(): Boolean = runCatching { mediaPlayer?.isPlaying == true }.getOrDefault(false)

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
