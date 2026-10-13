package com.nororoutine.batu.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.nororoutine.batu.MainActivity
import com.nororoutine.batu.R

class TodayRoutinesListWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE_ROUTINE = "com.nororoutine.batu.widget.ACTION_TOGGLE_ROUTINE"
        const val ACTION_REFRESH_WIDGET = "com.nororoutine.batu.widget.ACTION_REFRESH_WIDGET"
        const val EXTRA_ROUTINE_ID = "extra_routine_id"
        const val EXTRA_IS_TOGGLE = "extra_is_toggle"
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE_ROUTINE -> {
                val isToggle = intent.getBooleanExtra(EXTRA_IS_TOGGLE, true)
                val routineId = intent.getStringExtra(EXTRA_ROUTINE_ID)
                if (isToggle && !routineId.isNullOrBlank()) {
                    WidgetDataManager.toggleRoutineDone(context, routineId)
                } else {
                    // Open App
                    val appIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra(EXTRA_ROUTINE_ID, routineId)
                    }
                    context.startActivity(appIntent)
                }
            }
            ACTION_REFRESH_WIDGET -> {
                WidgetDataManager.refreshAllWidgets(context)
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
        val views = RemoteViews(context.packageName, R.layout.widget_today_list)

        // 1. Date & Header
        val dateText = WidgetDataManager.getFormattedTodayDate()
        views.setTextViewText(R.id.widget_header_date, dateText)

        // 2. Stats & Progress
        val stats = WidgetDataManager.getStats(context)
        views.setTextViewText(R.id.widget_progress_text, "${stats.completed} / ${stats.total}")
        views.setProgressBar(R.id.widget_progress_bar, 100, stats.percent, false)

        // 3. Remote Adapter for list view
        val serviceIntent = Intent(context, TodayRoutinesWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.widget_list_view, serviceIntent)
        views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_view)

        // 4. PendingIntent Template for item clicks / toggles
        val clickIntent = Intent(context, TodayRoutinesListWidgetProvider::class.java).apply {
            action = ACTION_TOGGLE_ROUTINE
        }
        val clickPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setPendingIntentTemplate(R.id.widget_list_view, clickPendingIntent)

        // 5. Refresh Button Action
        val refreshIntent = Intent(context, TodayRoutinesListWidgetProvider::class.java).apply {
            action = ACTION_REFRESH_WIDGET
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId + 1000,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)

        // 6. Header tap opens MainActivity
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId + 2000,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_header_date, openAppPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
