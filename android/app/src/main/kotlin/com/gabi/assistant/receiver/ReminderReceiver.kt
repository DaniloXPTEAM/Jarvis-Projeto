package com.gabi.assistant.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.gabi.assistant.JavisApplication
import com.gabi.assistant.R
import com.gabi.assistant.reminder.ReminderScheduler

/** Dispara a notificação (pt-BR) do lembrete quando o alarme toca. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val subject = intent.getStringExtra(ReminderScheduler.EXTRA_SUBJECT)
            ?.takeIf { it.isNotBlank() } ?: "Lembrete"

        val notification = NotificationCompat.Builder(context, JavisApplication.CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText(subject)
            .setStyle(NotificationCompat.BigTextStyle().bigText(subject))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val mgr = context.getSystemService(NotificationManager::class.java)
        mgr?.notify(System.currentTimeMillis().toInt(), notification)
    }
}
