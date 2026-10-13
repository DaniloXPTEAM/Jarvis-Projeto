package com.gabi.assistant.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.gabi.assistant.MainActivity
import com.gabi.assistant.R

/**
 * Widget de atalho na tela inicial: um toque abre a Gabi já ouvindo, sem precisar
 * achar o ícone do app e depois tocar no microfone — pedido do usuário em 26/08/2026
 * como alternativa rápida (sem custo/ML) enquanto a palavra de ativação de verdade
 * ("Oi Gabi" com o app fechado) não é implementada. Ainda exige desbloquear o
 * aparelho (limitação do Android pra lançar uma Activity), mas corta os passos de
 * abrir o app manualmente e tocar no botão de ouvir.
 */
class GabiWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id -> updateWidget(context, appWidgetManager, id) }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_VOICE_TRIGGER
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val views = RemoteViews(context.packageName, R.layout.widget_gabi).apply {
            setOnClickPendingIntent(R.id.widget_gabi_root, pendingIntent)
        }
        appWidgetManager.updateAppWidget(widgetId, views)
    }
}
