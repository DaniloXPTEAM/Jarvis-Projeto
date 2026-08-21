package com.gabi.assistant.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabi.assistant.storage.JavisPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Decide se o app deve abrir na tela de Permissões (primeira vez) ou direto na Home.
 * `userOnboarded` começa `null` (ainda lendo o DataStore) até resolver pra true/false —
 * o NavGraph espera esse valor antes de montar o grafo, pra não ter flash da tela errada.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: JavisPreferences
) : ViewModel() {

    val userOnboarded: StateFlow<Boolean?> = prefs.userOnboarded
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun markOnboarded() {
        viewModelScope.launch { prefs.setUserOnboarded(true) }
    }
}
