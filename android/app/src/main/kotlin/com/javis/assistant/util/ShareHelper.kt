package com.javis.assistant.util

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Compartilhamento Android (ACTION_SEND).
 * Abre a folha de compartilhar do sistema (WhatsApp, Telegram, e-mail, etc.).
 */
@Singleton
class ShareHelper @Inject constructor(@ApplicationContext private val context: Context) {

    fun shareText(text: String) {
        if (text.isBlank()) return
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Gabi")
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(sendIntent, "Compartilhar via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(chooser) }
    }
}
