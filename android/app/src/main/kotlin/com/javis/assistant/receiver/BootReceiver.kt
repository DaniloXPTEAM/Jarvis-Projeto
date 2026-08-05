package com.javis.assistant.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.room.Room
import com.javis.assistant.data.db.JavisDatabase
import com.javis.assistant.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("BootReceiver", "Dispositivo ligado — reagendando lembretes da Gabi")
            // Reagenda os lembretes pendentes (AlarmManager se perde no reboot)
            val pendingResult = goAsync()
            scope.launch {
                try {
                    val db = Room.databaseBuilder(
                        context, JavisDatabase::class.java, "javis_db"
                    ).fallbackToDestructiveMigration().build()
                    val scheduler = ReminderScheduler(db.reminderDao(), context)
                    scheduler.rescheduleAll()
                } catch (e: Exception) {
                    Log.e("BootReceiver", "erro ao reagendar: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
