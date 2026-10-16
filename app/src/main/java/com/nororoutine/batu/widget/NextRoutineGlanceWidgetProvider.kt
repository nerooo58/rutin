package com.nororoutine.batu.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.nororoutine.batu.MainActivity
import com.nororoutine.batu.R

class NextRoutineGlanceWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE_GLANCE = "com.nororoutine.batu.widget.ACTION_TOGGLE_GLANCE"
        const val EXTRA_ROUTINE_ID = "extra_glance_routine_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_GLANCE) {
            val routineId = intent.getStringExtra(EXTRA_ROUTINE_ID)
            if (!routineId.isNullOrBlank()) {
                WidgetDataManager.toggleRoutineDone(context, routineId)
            }
        }
    }

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
        val views = RemoteViews(context.packageName, R.layout.widget_next_routine)
        val stats = WidgetDataManager.getStats(context)

        // Progress Pill
        views.setTextViewText(R.id.widget_glance_progress, "${stats.completed}/${stats.total}")

        val targetRoutine = stats.activeRoutine ?: stats.nextRoutine

        if (stats.total == 0) {
            // No routines today
            views.setTextViewText(R.id.widget_status_badge, "Plan Yok")
            views.setTextViewText(R.id.widget_glance_name, "Bugün için planlanmış rutin bulunmuyor.")
            views.setTextViewText(R.id.widget_glance_time, "--:--")
            views.setTextViewText(R.id.widget_glance_category, "Rutin")
            views.setTextViewText(R.id.widget_glance_btn_text, "Rutin Ekle")
            views.setInt(R.id.widget_glance_btn_done, "setBackgroundResource", R.drawable.widget_btn_empty_bg)
            views.setTextColor(R.id.widget_glance_btn_text, Color.WHITE)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_glance_btn_done, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        } else if (stats.isAllDone) {
            // All completed today!
            views.setTextViewText(R.id.widget_status_badge, "Tamamlandı ✨")
            views.setTextViewText(R.id.widget_glance_name, "Tüm Rutinler Bitti! 🎉")
            views.setTextViewText(R.id.widget_glance_time, "%100")
            views.setTextViewText(R.id.widget_glance_category, "Başarılı")
            views.setTextViewText(R.id.widget_glance_btn_text, "Harika Bir Gün! 👏")
            views.setInt(R.id.widget_glance_btn_done, "setBackgroundResource", R.drawable.widget_btn_done_bg)
            views.setTextColor(R.id.widget_glance_btn_text, Color.parseColor("#0D0B1E"))

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_glance_btn_done, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        } else if (targetRoutine != null) {
            val isActive = stats.activeRoutine != null && stats.activeRoutine.id == targetRoutine.id
            val isDone = WidgetDataManager.isRoutineDone(context, targetRoutine.id)

            views.setTextViewText(
                R.id.widget_status_badge,
                if (isActive) "Şu An Aktif" else "Sıradaki Rutin"
            )
            views.setTextViewText(R.id.widget_glance_name, targetRoutine.name)

            val timeDisplay = if (targetRoutine.isRange && !targetRoutine.endTime.isNullOrBlank()) {
                "${targetRoutine.startTime} – ${targetRoutine.endTime}"
            } else {
                targetRoutine.startTime.ifEmpty { targetRoutine.time }
            }
            views.setTextViewText(R.id.widget_glance_time, timeDisplay)
            views.setTextViewText(R.id.widget_glance_category, targetRoutine.category)

            if (isDone) {
                views.setInt(R.id.widget_glance_btn_done, "setBackgroundResource", R.drawable.widget_btn_empty_bg)
                views.setTextViewText(R.id.widget_glance_btn_text, "Tamamlandı ✓")
                views.setTextColor(R.id.widget_glance_btn_text, Color.parseColor("#4ADE80"))
            } else {
                views.setInt(R.id.widget_glance_btn_done, "setBackgroundResource", R.drawable.widget_btn_done_bg)
                views.setTextViewText(R.id.widget_glance_btn_text, "Tamamla")
                views.setTextColor(R.id.widget_glance_btn_text, Color.parseColor("#0D0B1E"))
            }

            // Button click toggles completion
            val toggleIntent = Intent(context, NextRoutineGlanceWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_GLANCE
                putExtra(EXTRA_ROUTINE_ID, targetRoutine.id)
            }
            val togglePending = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_glance_btn_done, togglePending)

            // Card background click opens app
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("extra_routine_id", targetRoutine.id)
            }
            val openPending = PendingIntent.getActivity(
                context,
                appWidgetId + 500,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openPending)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
