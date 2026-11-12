package com.gabi.assistant.voice

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.gabi.assistant.JavisApplication
import com.gabi.assistant.MainActivity
import com.gabi.assistant.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class JavisVoiceService : Service() {

    @Inject lateinit var speechRecognizer: JavisSpeechRecognizer

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LISTENING -> {
                try {
                    startForeground(NOTIFICATION_ID, buildNotification())
                } catch (e: Exception) {
                    Log.w(TAG, "startForeground falhou: ${e.message}")
                    speechRecognizer.stopListening(notify = true)
                    stopSelf()
                    return START_NOT_STICKY
                }
                speechRecognizer.startListening()
            }
            ACTION_STOP -> {
                speechRecognizer.stopListening(notify = true)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, JavisVoiceService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, JavisApplication.CHANNEL_VOICE)
            .setContentTitle(getString(R.string.notification_title_listening))
            .setContentText(getString(R.string.notification_text_tap_stop))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(tapIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        speechRecognizer.stopListening(notify = true)
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_LISTENING = "com.gabi.assistant.START_LISTENING"
        const val ACTION_STOP = "com.gabi.assistant.STOP_VOICE"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "JavisVoiceService"
    }
}
