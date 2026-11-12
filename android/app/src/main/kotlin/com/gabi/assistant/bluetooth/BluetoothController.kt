package com.gabi.assistant.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.AudioDeviceInfo
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Controle Bluetooth do aparelho.
 *
 * Usos reais na Gabi:
 *  - ligar o Bluetooth e rotear a voz dela pra um fone/caixa BT;
 *  - usar o microfone de um fone Bluetooth pra falar com ela (hands-free);
 *  - listar dispositivos pareados.
 *
 * Obs.: o microfone do Echo (Alexa) NÃO é acessível por Bluetooth — o Echo só é
 * alto-falante BT; o áudio do mic dele vai pra nuvem da Amazon. Para voz via
 * Alexa, usa-se a Skill/cookie (lado do servidor).
 */
@Singleton
class BluetoothController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(BluetoothManager::class.java)
    val adapter: BluetoothAdapter? get() = bluetoothManager?.adapter

    fun isSupported(): Boolean = adapter != null
    fun isEnabled(): Boolean = runCatching { adapter?.isEnabled == true }.getOrDefault(false)

    /**
     * Liga o Bluetooth. Em Android <12 liga programaticamente; em 12+ a Google
     * proíbe ligar pelo app, então abre as Configurações p/ o usuário ativar.
     */
    fun requestEnable(): Boolean {
        val a = adapter ?: return false
        if (a.isEnabled) return true
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            @Suppress("DEPRECATION")
            runCatching { a.enable() }.getOrDefault(false)
        } else {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            false
        }
    }

    /** Nomes dos dispositivos já pareados (precisa de BLUETOOTH_CONNECT em 12+). */
    fun bondedDeviceNames(): List<String> {
        val a = adapter ?: return emptyList()
        return runCatching { a.bondedDevices?.map { it.name ?: it.address } ?: emptyList() }
            .onFailure { Log.w(TAG, "bondedDevices: ${it.message}") }
            .getOrDefault(emptyList())
    }

    /**
     * Roteia o áudio (TTS da Gabi e microfone do STT) pro fone/caixa Bluetooth
     * conectado via perfil SCO (hands-free).
     */
    fun routeAudioToBluetooth(audioManager: AudioManager): Boolean {
        return runCatching {
            val connected = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
            if (!connected) return@runCatching false
            audioManager.startBluetoothSco()
            audioManager.isBluetoothScoOn = true
            true
        }.onFailure { Log.w(TAG, "routeAudio: ${it.message}") }
            .getOrDefault(false)
    }

    fun stopAudioRouting(audioManager: AudioManager) {
        runCatching {
            audioManager.isBluetoothScoOn = false
            audioManager.stopBluetoothSco()
        }
    }

    /**
     * Busca aparelhos Bluetooth NOVOS por perto (não só os já pareados) — antes disso
     * não existia, era a lacuna real por trás de "o Bluetooth não acha nada": a Gabi
     * só sabia rotear áudio pro que já estava pareado manualmente, nunca fazia
     * descoberta de verdade. `BluetoothAdapter.startDiscovery()` é assíncrono — os
     * aparelhos chegam um por um via broadcast `ACTION_FOUND`, e o scan avisa quando
     * termina via `ACTION_DISCOVERY_FINISHED` (tipicamente ~12s). `neverForLocation`
     * já declarado no manifesto pro `BLUETOOTH_SCAN` dispensa localização em
     * Android 12+; ler o nome de cada aparelho encontrado (`device.name`) exige
     * `BLUETOOTH_CONNECT`, já concedido.
     */
    suspend fun discoverDevices(): List<String> {
        val a = adapter ?: return emptyList()
        if (!a.isEnabled) return emptyList()
        return withTimeoutOrNull(15_000) {
            suspendCancellableCoroutine { cont ->
                val found = linkedSetOf<String>()
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context, intent: Intent) {
                        when (intent.action) {
                            BluetoothDevice.ACTION_FOUND -> {
                                @Suppress("DEPRECATION")
                                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                                val name = runCatching { device?.name }.getOrNull()
                                if (!name.isNullOrBlank()) found.add(name)
                            }
                            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                                runCatching { context.unregisterReceiver(this) }
                                if (cont.isActive) cont.resume(found.toList())
                            }
                        }
                    }
                }
                val filter = IntentFilter().apply {
                    addAction(BluetoothDevice.ACTION_FOUND)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                }
                ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)

                val started = runCatching { a.startDiscovery() }.getOrDefault(false)
                if (!started) {
                    runCatching { context.unregisterReceiver(receiver) }
                    if (cont.isActive) cont.resume(emptyList())
                    return@suspendCancellableCoroutine
                }
                cont.invokeOnCancellation {
                    runCatching { context.unregisterReceiver(receiver) }
                    runCatching { a.cancelDiscovery() }
                }
            }
        } ?: emptyList()
    }

    /** Abre a tela nativa de Bluetooth do Android — pra o usuário parear um aparelho
     *  novo achado pela busca (parear programaticamente pede confirmação de PIN/tela
     *  do sistema de qualquer forma, então é mais confiável deixar o usuário tocar). */
    fun openBluetoothSettings() {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    companion object {
        private const val TAG = "BluetoothController"
    }
}
