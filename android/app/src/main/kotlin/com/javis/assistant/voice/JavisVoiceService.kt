package com.javis.assistant.voice

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
                try {
                    startForeground(NOTIFICATION_ID, buildNotification())
                } catch (e: Exception) {
                    // Sem a permissão de microfone o startForeground pode lançar
                    // SecurityException — o reconhecimento segue, só sem o serviço em primeiro plano.
                    Log.w(TAG, "startForeground falhou: ${e.message}")
                }63378f4 (chore: finish Gabi setup)
                speechRecognizer.startListening()
            }
            ACTION_STOP -> {
                speechRecognizer.stopListening()
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
        speechRecognizer.stopListening()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_LISTENING = "com.javis.assistant.START_LISTENING"
        const val ACTION_STOP = "com.javis.assistant.STOP_VOICE"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "JavisVoiceService"
    }
}
