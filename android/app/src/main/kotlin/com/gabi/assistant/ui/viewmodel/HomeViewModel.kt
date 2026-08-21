package com.gabi.assistant.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabi.assistant.domain.JavisRepository
import com.gabi.assistant.ui.components.GabiCharacterState
import com.gabi.assistant.voice.JavisSpeechRecognizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Deriva o estado visual da Gabi (Orb/personagem) a partir do reconhecimento de
 * voz e da fala — ambos Singletons compartilhados no app inteiro, então isso
 * reflete o estado real independente de qual tela disparou a escuta/fala.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    speechRecognizer: JavisSpeechRecognizer,
    repository: JavisRepository
) : ViewModel() {

    val characterState: StateFlow<GabiCharacterState> = combine(
        speechRecognizer.isListening,
        repository.isSpeaking
    ) { listening, speaking ->
        when {
            listening -> GabiCharacterState.LISTENING
            speaking -> GabiCharacterState.TALKING
            else -> GabiCharacterState.IDLE
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GabiCharacterState.IDLE)
}
