package com.gabi.assistant.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabi.assistant.data.model.AiProvider
import com.gabi.assistant.domain.JavisRepository
import com.gabi.assistant.memory.MemoryManager
import com.gabi.assistant.storage.JavisPreferences
import com.gabi.assistant.voice.ElevenLabsTts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val groqApiKey: String = "",
    val elevenLabsApiKey: String = "",
    val deepSeekApiKey: String = "",
    val anthropicApiKey: String = "",
    val elevenLabsVoiceId: String = "",
    val aiProvider: AiProvider = AiProvider.GROQ,
    val speechRate: Float = 1.0f,
    val ttsPitch: Float = 1.15f,   // tom feminino caloroso (acima de 1.0)
    val useElevenLabs: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val continuousMode: Boolean = false,
    val proactiveNotifications: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: JavisPreferences,
    private val memoryManager: MemoryManager,
    private val repository: JavisRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            prefs.groqApiKey,
            prefs.elevenLabsApiKey,
            prefs.deepSeekApiKey,
            prefs.elevenLabsVoiceId,
            prefs.aiProvider
        ) { groq, el, ds, voiceId, provider ->
            PartialA(groq, el, ds, voiceId, provider)
        },
        combine(
            prefs.speechRate,
            prefs.ttsPitch,
            prefs.useElevenLabs,
            prefs.notificationsEnabled,
            prefs.continuousMode
        ) { rate, pitch, useEl, notif, cont ->
            PartialB(rate, pitch, useEl, notif, cont)
        },
        prefs.anthropicApiKey,
        prefs.proactiveNotifications
    ) { a, b, anthropic, proactive ->
        SettingsUiState(
            groqApiKey = a.groqApiKey,
            elevenLabsApiKey = a.elevenLabsApiKey,
            deepSeekApiKey = a.deepSeekApiKey,
            anthropicApiKey = anthropic,
            elevenLabsVoiceId = a.elevenLabsVoiceId,
            aiProvider = a.aiProvider,
            speechRate = b.speechRate,
            ttsPitch = b.ttsPitch,
            useElevenLabs = b.useElevenLabs,
            notificationsEnabled = b.notificationsEnabled,
            continuousMode = b.continuousMode,
            proactiveNotifications = proactive
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    private val _savedMessage = MutableStateFlow<String?>(null)
    val savedMessage: StateFlow<String?> = _savedMessage

    fun save(state: SettingsUiState) {
        viewModelScope.launch {
            prefs.setGroqApiKey(state.groqApiKey)
            prefs.setElevenLabsApiKey(state.elevenLabsApiKey)
            prefs.setDeepSeekApiKey(state.deepSeekApiKey)
            prefs.setAnthropicApiKey(state.anthropicApiKey)
            prefs.setElevenLabsVoiceId(
                state.elevenLabsVoiceId.ifBlank { ElevenLabsTts.DEFAULT_VOICE_ID }
            )
            prefs.setAiProvider(state.aiProvider)
            prefs.setSpeechRate(state.speechRate)
            prefs.setTtsPitch(state.ttsPitch)
            prefs.setUseElevenLabs(state.useElevenLabs)
            prefs.setNotificationsEnabled(state.notificationsEnabled)
            prefs.setContinuousMode(state.continuousMode)
            prefs.setProactiveNotifications(state.proactiveNotifications)
            _savedMessage.value = "Ajustes salvos"
        }
    }

    fun clearMemory() {
        viewModelScope.launch {
            memoryManager.clearAll()
            _savedMessage.value = "Memória limpa"
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _savedMessage.value = "Histórico de conversas limpo"
        }
    }

    fun dismissSaved() {
        _savedMessage.value = null
    }

    private data class PartialA(
        val groqApiKey: String,
        val elevenLabsApiKey: String,
        val deepSeekApiKey: String,
        val elevenLabsVoiceId: String,
        val aiProvider: AiProvider
    )

    private data class PartialB(
        val speechRate: Float,
        val ttsPitch: Float,
        val useElevenLabs: Boolean,
        val notificationsEnabled: Boolean,
        val continuousMode: Boolean
    )
}
