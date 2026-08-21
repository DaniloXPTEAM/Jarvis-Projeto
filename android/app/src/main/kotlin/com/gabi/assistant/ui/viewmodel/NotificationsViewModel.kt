package com.gabi.assistant.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabi.assistant.data.db.NotificationDao
import com.gabi.assistant.data.model.NotificationItem
import com.gabi.assistant.notifications.JavisNotificationListenerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationDao: NotificationDao
) : ViewModel() {

    val notifications: StateFlow<List<NotificationItem>> = notificationDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markAllRead() {
        viewModelScope.launch {
            notificationDao.markAllRead()
        }
    }

    fun dismiss(key: String) {
        JavisNotificationListenerService.getInstance()?.dismissNotification(key)
    }

    fun clearAll() {
        viewModelScope.launch {
            notificationDao.deleteAll()
        }
    }
}
