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
        val KEY_PROACTIVE_NOTIFICATIONS = booleanPreferencesKey("proactive_notifications")
        val KEY_FILES_TREE_URI = stringPreferencesKey("files_tree_uri")
        val KEY_GABI_LAST_FED_AT = longPreferencesKey("gabi_last_fed_at")
        val KEY_GABI_FIRST_USE_AT = longPreferencesKey("gabi_first_use_at")
        val KEY_GABI_SELECTED_OUTFIT_ID = stringPreferencesKey("gabi_selected_outfit_id")
        val KEY_GABI_UNLOCK_INTERVAL_DAYS = intPreferencesKey("gabi_unlock_interval_days")
    }

    val groqApiKey: Flow<String> = secureKeys.asFlow(SecureKeyStore.KEY_GROQ)
    val elevenLabsApiKey: Flow<String> = secureKeys.asFlow(SecureKeyStore.KEY_ELEVENLABS)
    val deepSeekApiKey: Flow<String> = secureKeys.asFlow(SecureKeyStore.KEY_DEEPSEEK)
    val anthropicApiKey: Flow<String> = secureKeys.asFlow(SecureKeyStore.KEY_ANTHROPIC)
    val spotifyClientId: Flow<String> = secureKeys.asFlow(SecureKeyStore.KEY_SPOTIFY_CLIENT_ID)
    val spotifyClientSecret: Flow<String> = secureKeys.asFlow(SecureKeyStore.KEY_SPOTIFY_CLIENT_SECRET)
    val elevenLabsVoiceId: Flow<String> = ds.data.map { it[KEY_ELEVENLABS_VOICE_ID] ?: "" }
    // Padrão trocado pra DEEPSEEK (15/10/2026): a chave nativa do Groq tinha sido
    // revogada/morta (confirmado HTTP 401 "Invalid API Key" via curl direto) sem
    // ninguém perceber — como o roteamento por IA sempre tenta o "principal" primeiro
    // e só cai pro outro se falhar, TODA mensagem pagava o custo de uma chamada Groq
    // fadada a falhar antes de cair pro DeepSeek (que já tinha chave própria do
    // usuário configurada e funcionando) — dobrando a latência de toda resposta.
    // Decisão do usuário: DeepSeek principal agora, Groq fica como secundário (chave
    // já renovada em paralelo, ver local.properties).
    val aiProvider: Flow<AiProvider> = ds.data.map {
        try { AiProvider.valueOf(it[KEY_AI_PROVIDER] ?: AiProvider.DEEPSEEK.name) }
        catch (e: Exception) { AiProvider.DEEPSEEK }
    }
    val speechRate: Flow<Float> = ds.data.map { it[KEY_SPEECH_RATE] ?: 1.0f }
    val ttsPitch: Flow<Float> = ds.data.map { it[KEY_TTS_PITCH] ?: 1.05f }
    val useElevenLabs: Flow<Boolean> = ds.data.map { it[KEY_USE_ELEVENLABS] ?: false }
    val notificationsEnabled: Flow<Boolean> = ds.data.map { it[KEY_NOTIFICATIONS_ENABLED] ?: true }
    val continuousMode: Flow<Boolean> = ds.data.map { it[KEY_CONTINUOUS_MODE] ?: false }
    val userOnboarded: Flow<Boolean> = ds.data.map { it[KEY_USER_ONBOARDED] ?: false }
    /** Se a Gabi avisa sozinha ("Você recebeu um zap de X, quer que eu leia ou é segredo?")
     *  quando chega WhatsApp/Gmail novo, em vez de só responder quando perguntada. Desligado por padrão. */
    val proactiveNotifications: Flow<Boolean> = ds.data.map { it[KEY_PROACTIVE_NOTIFICATIONS] ?: false }
    /** URI (SAF, ACTION_OPEN_DOCUMENT_TREE) da pasta que o usuário escolheu pra
     *  Gabi gerenciar. Vazio até o usuário escolher uma pasta pela primeira vez. */
    val filesTreeUri: Flow<String> = ds.data.map { it[KEY_FILES_TREE_URI] ?: "" }
    /** Timestamp (millis) da última vez que alimentou a Gabi no Quarto — 0 = nunca. */
    val gabiLastFedAt: Flow<Long> = ds.data.map { it[KEY_GABI_LAST_FED_AT] ?: 0L }
    /** Timestamp (millis) da primeira vez que o Quarto foi aberto — marca o início da
     *  contagem do guarda-roupa. 0 = ainda não abriu o Quarto nenhuma vez. */
    val gabiFirstUseAt: Flow<Long> = ds.data.map { it[KEY_GABI_FIRST_USE_AT] ?: 0L }
    val gabiSelectedOutfitId: Flow<String> = ds.data.map { it[KEY_GABI_SELECTED_OUTFIT_ID] ?: "" }
    /** De quantos em quantos dias um look novo desbloqueia — sorteado uma vez só (não
     *  a cada checagem) pra ficar fixo depois de decidido. 0 = ainda não sorteado. */
    val gabiUnlockIntervalDays: Flow<Int> = ds.data.map { it[KEY_GABI_UNLOCK_INTERVAL_DAYS] ?: 0 }

    suspend fun setGroqApiKey(key: String) = secureKeys.set(SecureKeyStore.KEY_GROQ, key)
    suspend fun setElevenLabsApiKey(key: String) = secureKeys.set(SecureKeyStore.KEY_ELEVENLABS, key)
    suspend fun setDeepSeekApiKey(key: String) = secureKeys.set(SecureKeyStore.KEY_DEEPSEEK, key)
    suspend fun setAnthropicApiKey(key: String) = secureKeys.set(SecureKeyStore.KEY_ANTHROPIC, key)
    suspend fun setSpotifyClientId(key: String) = secureKeys.set(SecureKeyStore.KEY_SPOTIFY_CLIENT_ID, key)
    suspend fun setSpotifyClientSecret(key: String) = secureKeys.set(SecureKeyStore.KEY_SPOTIFY_CLIENT_SECRET, key)
    suspend fun setElevenLabsVoiceId(id: String) = ds.edit { it[KEY_ELEVENLABS_VOICE_ID] = id }
    suspend fun setAiProvider(p: AiProvider) = ds.edit { it[KEY_AI_PROVIDER] = p.name }
    suspend fun setSpeechRate(r: Float) = ds.edit { it[KEY_SPEECH_RATE] = r }
    suspend fun setTtsPitch(p: Float) = ds.edit { it[KEY_TTS_PITCH] = p }
    suspend fun setUseElevenLabs(b: Boolean) = ds.edit { it[KEY_USE_ELEVENLABS] = b }
    suspend fun setNotificationsEnabled(b: Boolean) = ds.edit { it[KEY_NOTIFICATIONS_ENABLED] = b }
    suspend fun setContinuousMode(b: Boolean) = ds.edit { it[KEY_CONTINUOUS_MODE] = b }
    suspend fun setUserOnboarded(b: Boolean) = ds.edit { it[KEY_USER_ONBOARDED] = b }
    suspend fun setProactiveNotifications(b: Boolean) = ds.edit { it[KEY_PROACTIVE_NOTIFICATIONS] = b }
    suspend fun setFilesTreeUri(uri: String) = ds.edit { it[KEY_FILES_TREE_URI] = uri }
    suspend fun setGabiLastFedAt(millis: Long) = ds.edit { it[KEY_GABI_LAST_FED_AT] = millis }
    suspend fun setGabiFirstUseAt(millis: Long) = ds.edit { it[KEY_GABI_FIRST_USE_AT] = millis }
    suspend fun setGabiSelectedOutfitId(id: String) = ds.edit { it[KEY_GABI_SELECTED_OUTFIT_ID] = id }
    suspend fun setGabiUnlockIntervalDays(days: Int) = ds.edit { it[KEY_GABI_UNLOCK_INTERVAL_DAYS] = days }
}
