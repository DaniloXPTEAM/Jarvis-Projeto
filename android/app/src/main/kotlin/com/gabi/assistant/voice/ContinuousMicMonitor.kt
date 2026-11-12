package com.gabi.assistant.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Fase 4 do plano de latência (alexa-referencia/PLANO_DE_ALTERACOES.md) — detector de
 * voz LEVE, próprio, que roda em paralelo ao `SpeechRecognizer` pesado do sistema.
 *
 * Por que isso existe: o `JavisSpeechRecognizer` (via `SpeechRecognizer` do Android)
 * precisa de foco de áudio pra funcionar — é o que causa a Gabi pausar qualquer música
 * tocando toda vez que volta a "ouvir" no modo contínuo, mesmo só esperando em silêncio.
 * Este monitor usa `AudioRecord` direto, que NÃO pede foco de áudio — grava o microfone
 * sem interromper nada tocando. Só quando ele detecta voz de verdade é que a Gabi liga o
 * reconhecedor pesado (que aí sim, momentaneamente, pede foco — é o comportamento normal
 * de qualquer assistente de voz respondendo).
 *
 * Também serve pra interromper a própria fala dela (barge-in): se tocado enquanto ela
 * está falando, detectar voz do usuário já é o gatilho pra parar o TTS e abrir a escuta
 * de verdade, em vez de esperar ela terminar.
 *
 * Detecção: energia (RMS) do áudio acima de um limiar, sustentada por alguns frames
 * seguidos (evita disparar com um estalo rápido). **Não é reconhecimento de palavra
 * nenhuma** — é só "tem alguém falando perto do microfone agora". O limiar abaixo é um
 * chute inicial; precisa calibrar com o aparelho real do usuário (varia por microfone/
 * ambiente — mesma ressalva que o plano faz sobre o AEC).
 */
@Singleton
class ContinuousMicMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var audioRecord: AudioRecord? = null
    private var echoCanceler: AcousticEchoCanceler? = null

    val isRunning: Boolean get() = job?.isActive == true

    /** Começa a monitorar o microfone sem pedir foco de áudio. [onVoiceDetected] é
     *  chamado na main thread assim que detectar voz sustentada — quem chamar deve
     *  parar o monitor (ou chamar [stop]) antes de abrir o reconhecedor pesado. */
    fun start(onVoiceDetected: () -> Unit) {
        if (isRunning) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_HZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) {
            Log.w(TAG, "AudioRecord.getMinBufferSize inválido neste aparelho — monitor desativado")
            return
        }

        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE_HZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                minBuffer * 2
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "Sem permissão pra abrir o AudioRecord: ${e.message}")
            return
        }

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.w(TAG, "AudioRecord não inicializou — monitor desativado")
            record.release()
            return
        }
        audioRecord = record

        // AEC opcional (mesmo padrão do Alexa, PARAMETROS_DE_VOZ.md seção 1): se o
        // aparelho não suportar, segue sem — detectar voz alta o suficiente ainda
        // funciona, só fica mais sujeito a falso positivo quando ela está falando alto.
        if (AcousticEchoCanceler.isAvailable()) {
            echoCanceler = try {
                AcousticEchoCanceler.create(record.audioSessionId)?.apply { enabled = true }
            } catch (e: Exception) {
                null
            }
        }

        try {
            record.startRecording()
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao iniciar gravação: ${e.message}")
            releaseAudio()
            return
        }

        job = scope.launch {
            val buffer = ShortArray(minBuffer)
            var sustainedFrames = 0
            try {
                while (isActive) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read < 0) throw IllegalStateException("AudioRecord read error $read")
                    if (read == 0) continue
                    val rms = rmsOf(buffer, read)
                    if (rms > VOICE_RMS_THRESHOLD) {
                        sustainedFrames++
                        if (sustainedFrames >= SUSTAINED_FRAMES_REQUIRED) {
                            withContext(Dispatchers.Main) { onVoiceDetected() }
                            return@launch
                        }
                    } else {
                        sustainedFrames = 0
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Monitor parou com erro: ${e.message}")
            } finally {
                withContext(NonCancellable + Dispatchers.Main) {
                    if (audioRecord === record) releaseAudio()
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        releaseAudio()
    }

    private fun releaseAudio() {
        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) audioRecord?.stop()
        } catch (_: Exception) {}
        audioRecord?.release()
        audioRecord = null
        echoCanceler?.release()
        echoCanceler = null
    }

    private fun rmsOf(buffer: ShortArray, len: Int): Double {
        var sum = 0.0
        for (i in 0 until len) sum += buffer[i].toDouble() * buffer[i].toDouble()
        return sqrt(sum / len)
    }

    companion object {
        private const val TAG = "ContinuousMicMonitor"
        private const val SAMPLE_RATE_HZ = 16_000
        // Chute inicial (amplitude RMS de um PCM16, escala 0-32767) — precisa calibrar
        // no aparelho real: alto demais perde fala baixa, baixo demais dispara com
        // ruído ambiente/a própria música tocando no alto-falante.
        private const val VOICE_RMS_THRESHOLD = 1200.0
        private const val SUSTAINED_FRAMES_REQUIRED = 3
    }
}
