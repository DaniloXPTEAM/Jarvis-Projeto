package com.gabi.assistant.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.gabi.assistant.data.model.AiProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "javis_prefs")

@Singleton
class JavisPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureKeys: SecureKeyStore
) {
    private val ds = context.dataStore

    companion object {
        val KEY_ELEVENLABS_VOICE_ID = stringPreferencesKey("elevenlabs_voice_id")
        val KEY_AI_PROVIDER = stringPreferencesKey("ai_provider")
        val KEY_SPEECH_RATE = floatPreferencesKey("speech_rate")
        val KEY_TTS_PITCH = floatPreferencesKey("tts_pitch")
        val KEY_USE_ELEVENLABS = booleanPreferencesKey("use_elevenlabs")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val KEY_USER_ONBOARDED = booleanPreferencesKey("user_onboarded")
        val KEY_CONTINUOUS_MODE = booleanPreferencesKey("continuous_mode")
    }

    val groqApiKey: Flow<String> = secureKeys.asFlow(SecureKeyStore.KEY_GROQ)
    val elevenLabsApiKey: Flow<String> = secureKeys.asFlow(SecureKeyStore.KEY_ELEVENLABS)
    val deepSeekApiKey: Flow<String> = secureKeys.asFlow(SecureKeyStore.KEY_DEEPSEEK)
    val anthropicApiKey: Flow<String> = secureKeys.asFlow(SecureKeyStore.KEY_ANTHROPIC)
    val elevenLabsVoiceId: Flow<String> = ds.data.map { it[KEY_ELEVENLABS_VOICE_ID] ?: "" }
    val aiProvider: Flow<AiProvider> = ds.data.map {
        try { AiProvider.valueOf(it[KEY_AI_PROVIDER] ?: AiProvider.GROQ.name) }
        catch (e: Exception) { AiProvider.GROQ }
    }
    val speechRate: Flow<Float> = ds.data.map { it[KEY_SPEECH_RATE] ?: 1.0f }
    val ttsPitch: Flow<Float> = ds.data.map { it[KEY_TTS_PITCH] ?: 1.05f }
    val useElevenLabs: Flow<Boolean> = ds.data.map { it[KEY_USE_ELEVENLABS] ?: false }
    val notificationsEnabled: Flow<Boolean> = ds.data.map { it[KEY_NOTIFICATIONS_ENABLED] ?: true }
    val continuousMode: Flow<Boolean> = ds.data.map { it[KEY_CONTINUOUS_MODE] ?: false }
    val userOnboarded: Flow<Boolean> = ds.data.map { it[KEY_USER_ONBOARDED] ?: false }

    suspend fun setGroqApiKey(key: String) = secureKeys.set(SecureKeyStore.KEY_GROQ, key)
    suspend fun setElevenLabsApiKey(key: String) = secureKeys.set(SecureKeyStore.KEY_ELEVENLABS, key)
    suspend fun setDeepSeekApiKey(key: String) = secureKeys.set(SecureKeyStore.KEY_DEEPSEEK, key)
    suspend fun setAnthropicApiKey(key: String) = secureKeys.set(SecureKeyStore.KEY_ANTHROPIC, key)
    suspend fun setElevenLabsVoiceId(id: String) = ds.edit { it[KEY_ELEVENLABS_VOICE_ID] = id }
    suspend fun setAiProvider(p: AiProvider) = ds.edit { it[KEY_AI_PROVIDER] = p.name }
    suspend fun setSpeechRate(r: Float) = ds.edit { it[KEY_SPEECH_RATE] = r }
    suspend fun setTtsPitch(p: Float) = ds.edit { it[KEY_TTS_PITCH] = p }
    suspend fun setUseElevenLabs(b: Boolean) = ds.edit { it[KEY_USE_ELEVENLABS] = b }
    suspend fun setNotificationsEnabled(b: Boolean) = ds.edit { it[KEY_NOTIFICATIONS_ENABLED] = b }
    suspend fun setContinuousMode(b: Boolean) = ds.edit { it[KEY_CONTINUOUS_MODE] = b }
    suspend fun setUserOnboarded(b: Boolean) = ds.edit { it[KEY_USER_ONBOARDED] = b }
}
