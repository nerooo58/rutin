package com.nororoutine.batu.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.nororoutine.batu.R
import com.nororoutine.batu.model.Routine

class TodayRoutinesWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TodayRoutinesRemoteViewsFactory(applicationContext)
    }
}

class TodayRoutinesRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var routines: List<Routine> = emptyList()

    override fun onCreate() {
        loadData()
    }

    override fun onDataSetChanged() {
        loadData()
    }

    private fun loadData() {
        routines = WidgetDataManager.getTodayRoutines(context)
    }

    override fun onDestroy() {
        routines = emptyList()
    }

    override fun getCount(): Int = routines.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position !in routines.indices) {
            return RemoteViews(context.packageName, R.layout.widget_routine_list_item)
        }

        val routine = routines[position]
        val isDone = WidgetDataManager.isRoutineDone(context, routine.id)
        val views = RemoteViews(context.packageName, R.layout.widget_routine_list_item)

        // Routine Name
        views.setTextViewText(R.id.widget_item_name, routine.name)

        // Time Chip
        val timeDisplay = if (routine.isRange && !routine.endTime.isNullOrBlank()) {
            "${routine.startTime} – ${routine.endTime}"
        } else {
            routine.startTime.ifEmpty { routine.time }
        }
        views.setTextViewText(R.id.widget_item_time, timeDisplay)

        // Category
        views.setTextViewText(R.id.widget_item_category, routine.category)

        // Status Button styling matching the app's aesthetic
        if (isDone) {
            views.setInt(R.id.widget_item_btn_toggle, "setBackgroundResource", R.drawable.widget_btn_done_bg)
            views.setTextViewText(R.id.widget_item_status_text, "Tamamlandı")
            views.setTextColor(R.id.widget_item_status_text, Color.parseColor("#0D0B1E"))
            views.setViewVisibility(R.id.widget_item_icon_check, View.VISIBLE)
        } else {
            views.setInt(R.id.widget_item_btn_toggle, "setBackgroundResource", R.drawable.widget_btn_empty_bg)
            views.setTextViewText(R.id.widget_item_status_text, "Tamamla")
            views.setTextColor(R.id.widget_item_status_text, Color.parseColor("#94A3B8"))
            views.setViewVisibility(R.id.widget_item_icon_check, View.GONE)
        }

        // FillInIntent for toggle button action
        val fillInToggle = Intent().apply {
            putExtra(TodayRoutinesListWidgetProvider.EXTRA_ROUTINE_ID, routine.id)
            putExtra(TodayRoutinesListWidgetProvider.EXTRA_IS_TOGGLE, true)
        }
        views.setOnClickFillInIntent(R.id.widget_item_btn_toggle, fillInToggle)

        // FillInIntent for opening the app when tapping the row
        val fillInOpen = Intent().apply {
            putExtra(TodayRoutinesListWidgetProvider.EXTRA_ROUTINE_ID, routine.id)
            putExtra(TodayRoutinesListWidgetProvider.EXTRA_IS_TOGGLE, false)
        }
        views.setOnClickFillInIntent(R.id.widget_item_root, fillInOpen)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
