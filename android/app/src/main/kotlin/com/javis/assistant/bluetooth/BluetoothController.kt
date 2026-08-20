package com.javis.assistant.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

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
    fun isEnabled(): Boolean = adapter?.isEnabled == true

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

    companion object {
        private const val TAG = "BluetoothController"
    }
}
