package com.gabi.assistant.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabi.assistant.data.db.NotificationDao
import com.gabi.assistant.data.db.ReminderDao
import com.gabi.assistant.domain.JavisRepository
import com.gabi.assistant.skills.WeatherAgent
import com.gabi.assistant.skills.WeatherSummary
import com.gabi.assistant.ui.components.GabiCharacterState
import com.gabi.assistant.voice.JavisSpeechRecognizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeDashboardState(
    val weather: WeatherSummary? = null,
    val weatherLoading: Boolean = true,
    val reminderCount: Int = 0,
    val notificationCount: Int = 0
)

/**
 * Deriva o estado visual da Gabi (Orb/personagem) a partir do reconhecimento de
 * voz e da fala — ambos Singletons compartilhados no app inteiro, então isso
 * reflete o estado real independente de qual tela disparou a escuta/fala.
 *
 * Também monta o [dashboard] com dado real (clima, lembretes de hoje, avisos
 * não lidos) pros 3 QuickCard da Home, em vez do texto fixo que tinha antes.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    speechRecognizer: JavisSpeechRecognizer,
    repository: JavisRepository,
    private val weatherAgent: WeatherAgent,
    reminderDao: ReminderDao,
    notificationDao: NotificationDao
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

    private val _dashboard = MutableStateFlow(HomeDashboardState())
    val dashboard: StateFlow<HomeDashboardState> = _dashboard.asStateFlow()

    init {
        viewModelScope.launch {
            val summary = weatherAgent.getCurrentWeatherSummary()
            _dashboard.update { it.copy(weather = summary, weatherLoading = false) }
        }
        viewModelScope.launch {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = today.timeInMillis
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000L - 1
            reminderDao.observeDueTodayCount(startOfDay, endOfDay).collect { count ->
                _dashboard.update { it.copy(reminderCount = count) }
            }
        }
        viewModelScope.launch {
            notificationDao.observeUnreadCount().collect { count ->
                _dashboard.update { it.copy(notificationCount = count) }
            }
        }
    }
}
