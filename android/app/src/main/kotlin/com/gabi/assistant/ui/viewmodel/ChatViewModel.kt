package com.gabi.assistant.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabi.assistant.ai.CommandType
import com.gabi.assistant.ai.ParsedCommand
import com.gabi.assistant.data.model.Message
import com.gabi.assistant.data.model.VoiceState
import com.gabi.assistant.domain.JavisRepository
import com.gabi.assistant.voice.JavisSpeechRecognizer
import com.gabi.assistant.voice.SpeechEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val voiceState: VoiceState = VoiceState(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val pendingCommand: ParsedCommand? = null,
    val inputText: String = ""
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: JavisRepository,
    private val speechRecognizer: JavisSpeechRecognizer
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val sessionId: String = repository.currentSessionId

    init {
        observeMessages()
        observeSpeech()
        observeProactiveNotifications()
        sendGreeting()
    }

    /** Enquanto esta tela (o app) estiver aberta, deixa a Gabi avisar sozinha sobre WhatsApp/Gmail novos. */
    private fun observeProactiveNotifications() {
        viewModelScope.launch {
            repository.watchProactiveNotifications(sessionId)
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            repository.getMessages(sessionId).collect { msgs ->
                _uiState.update { it.copy(messages = msgs) }
            }
        }
    }

    private fun observeSpeech() {
        viewModelScope.launch {
            speechRecognizer.events.collect { event ->
                when (event) {
                    is SpeechEvent.Started -> {
                        _uiState.update { it.copy(voiceState = it.voiceState.copy(isListening = true)) }
                    }
                    is SpeechEvent.Partial -> {
                        _uiState.update { it.copy(voiceState = it.voiceState.copy(partialText = event.text)) }
                    }
                    is SpeechEvent.Result -> {
                        _uiState.update {
                            it.copy(
                                voiceState = it.voiceState.copy(isListening = false, partialText = ""),
                                inputText = event.text
                            )
                        }
                        sendMessage(event.text)
                    }
                    is SpeechEvent.Error -> {
                        _uiState.update {
                            it.copy(
                                voiceState = it.voiceState.copy(isListening = false, partialText = ""),
                                error = if (event.code == 7 || event.code == 6) null else event.message
                            )
                        }
                    }
                    is SpeechEvent.Ended -> {
                        _uiState.update { it.copy(voiceState = it.voiceState.copy(isListening = false)) }
                    }
                    null -> {}
                }
            }
        }
    }

    private fun sendGreeting() {
        viewModelScope.launch {
            repository.greet(sessionId)
        }
    }

    /**
     * Manda a fala/texto pro repositório, que roteia via IA (ou regex offline se não
     * houver IA disponível) e decide sozinho se alguma ação precisa de confirmação —
     * a tela só reage ao resultado, não faz sua própria classificação por regex antes
     * (isso já causou um bug: uma ação identificada só pela IA nunca pedia confirmação
     * nem executava, porque essa checagem local não sabia dela).
     */
    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, inputText = "", error = null) }
            repository.sendMessage(text, sessionId).onSuccess { outcome ->
                _uiState.update { it.copy(pendingCommand = outcome.pendingCommand) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun confirmPendingCommand(confirmed: Boolean) {
        val cmd = _uiState.value.pendingCommand ?: return
        _uiState.update { it.copy(pendingCommand = null) }

        if (!confirmed) {
            viewModelScope.launch {
                repository.sayAsAssistant("Beleza, não vou fazer isso.", sessionId)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.executeConfirmedCommand(cmd)

            val reply = when (cmd.type) {
                CommandType.CALL_CONTACT -> "Ligando para ${cmd.target}…"
                CommandType.SEND_WHATSAPP -> "Enviando para ${cmd.target}…"
                CommandType.SET_ALARM -> "Configurando alarme…"
                else -> "Pronto."
            }
            repository.sayAsAssistant(reply, sessionId)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun startListening() {
        repository.stopSpeaking()
        speechRecognizer.startListening()
    }

    fun stopListening() {
        speechRecognizer.stopListening()
    }

    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        speechRecognizer.destroy()
        super.onCleared()
    }
}
