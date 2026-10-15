package com.gabi.assistant.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda as chaves de API criptografadas via Android Keystore
 * (EncryptedSharedPreferences), em vez de texto puro como antes no DataStore.
 * Chave e valor ficam cifrados em disco; a chave mestra nunca sai do Keystore
 * do aparelho.
 */
@Singleton
class SecureKeyStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "gabi_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun get(key: String): String = prefs.getString(key, "") ?: ""

    fun set(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun asFlow(key: String): Flow<String> = callbackFlow {
        trySend(get(key))
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, changedKey ->
            if (changedKey == key) trySend(sp.getString(key, "") ?: "")
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    companion object {
        const val KEY_GROQ = "groq_api_key"
        const val KEY_ELEVENLABS = "elevenlabs_api_key"
        const val KEY_DEEPSEEK = "deepseek_api_key"
        const val KEY_OPENROUTER = "openrouter_api_key"
        const val KEY_ANTHROPIC = "anthropic_api_key"
        const val KEY_SPOTIFY_CLIENT_ID = "spotify_client_id"
        const val KEY_SPOTIFY_CLIENT_SECRET = "spotify_client_secret"
    }
}
