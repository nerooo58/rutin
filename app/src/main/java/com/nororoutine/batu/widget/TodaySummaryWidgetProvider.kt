package com.nororoutine.batu.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.nororoutine.batu.MainActivity
import com.nororoutine.batu.R

class TodaySummaryWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_today_summary)
        val stats = WidgetDataManager.getStats(context)

        // Date
        val dateText = WidgetDataManager.getFormattedTodayDate()
        views.setTextViewText(R.id.widget_summary_date, dateText)

        // Progress Badges
        views.setTextViewText(R.id.widget_summary_progress, "${stats.completed} / ${stats.total}")
        views.setTextViewText(R.id.widget_summary_percent, "%${stats.percent}")

        // Target routine
        val targetRoutine = stats.activeRoutine ?: stats.nextRoutine

        if (stats.total == 0) {
            views.setTextViewText(R.id.widget_summary_routine_name, "Planlanmış rutin yok")
            views.setTextViewText(R.id.widget_summary_routine_time, "--:--")
        } else if (stats.isAllDone) {
            views.setTextViewText(R.id.widget_summary_routine_name, "Tüm rutinler tamamlandı! 🎉")
            views.setTextViewText(R.id.widget_summary_routine_time, "%100 Başarı")
        } else if (targetRoutine != null) {
            views.setTextViewText(R.id.widget_summary_routine_name, targetRoutine.name)
            val prefix = if (stats.activeRoutine != null && stats.activeRoutine.id == targetRoutine.id) {
                "Şu An Aktif"
            } else {
                "Sıradaki"
            }
            views.setTextViewText(
                R.id.widget_summary_routine_time,
                "${targetRoutine.startTime} • $prefix"
            )
        }

        // Tap opens MainActivity
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
