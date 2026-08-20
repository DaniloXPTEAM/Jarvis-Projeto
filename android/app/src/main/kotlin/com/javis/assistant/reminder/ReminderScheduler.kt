package com.javis.assistant.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.javis.assistant.data.db.ReminderDao
import com.javis.assistant.data.model.Reminder
import com.javis.assistant.receiver.ReminderReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Agenda lembretes via AlarmManager e os persiste em Room (pra sobreviver a reboot).
 */
@Singleton
class ReminderScheduler @Inject constructor(
    private val reminderDao: ReminderDao,
    @ApplicationContext private val context: Context
) {
    suspend fun schedule(subject: String, triggerAtMillis: Long): Long {
        val id = reminderDao.insert(Reminder(subject = subject, triggerAt = triggerAtMillis))
        setAlarm(id, subject, triggerAtMillis)
        Log.d(TAG, "Lembrete agendado id=$id às $triggerAtMillis: $subject")
        return id
    }

    /** Reagenda tudo pendente (usado após boot). */
    suspend fun rescheduleAll() {
        reminderDao.getPending().forEach { setAlarm(it.id, it.subject, it.triggerAt) }
    }

    suspend fun cancel(id: Long) {
        val intent = buildIntent(id, "")
        val pi = PendingIntent.getBroadcast(
            context, id.toInt(), intent, flags()
        )
        context.getSystemService(AlarmManager::class.java)?.cancel(pi)
        reminderDao.delete(id)
    }

    private fun setAlarm(id: Long, subject: String, triggerAtMillis: Long) {
        val pi = PendingIntent.getBroadcast(
            context, id.toInt(), buildIntent(id, subject), flags()
        )
        val mgr = context.getSystemService(AlarmManager::class.java) ?: return
        try {
            mgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } catch (se: SecurityException) {
            // Sem permissão de alarme exato (Android 12+): cai para inexact
            mgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            Log.w(TAG, "Alarme exato negado; usando inexact.")
        }
    }

    private fun buildIntent(id: Long, subject: String) =
        Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_SUBJECT, subject)
        }

    private fun flags() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT

    companion object {
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_SUBJECT = "reminder_subject"
        private const val TAG = "ReminderScheduler"
    }
}
