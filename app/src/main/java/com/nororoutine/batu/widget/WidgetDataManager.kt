package com.nororoutine.batu.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.Timestamp
import com.nororoutine.batu.R
import com.nororoutine.batu.data.FirestoreRepository
import com.nororoutine.batu.model.DAY_STRING_TO_INT
import com.nororoutine.batu.model.Routine
import com.nororoutine.batu.model.RoutineLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class WidgetStats(
    val total: Int = 0,
    val completed: Int = 0,
    val percent: Int = 0,
    val activeRoutine: Routine? = null,
    val nextRoutine: Routine? = null,
    val isAllDone: Boolean = false
)

object WidgetDataManager {
    private const val TAG = "WidgetDataManager"
    private const val PREFS_NAME = "rutin_app_widget_cache"
    private const val KEY_ROUTINES_JSON = "widget_routines_json"
    private const val KEY_LOGS_JSON = "widget_logs_json"
    private const val KEY_CACHED_DATE = "widget_cached_date"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Updates widget cache from ViewModel or Firestore streams.
     */
    fun updateWidgetCache(
        context: Context,
        routines: List<Routine>,
        logs: Map<String, RoutineLog>
    ) {
        try {
            val today = LocalDate.now()
            val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

            // Save routines
            val rArray = JSONArray()
            for (r in routines) {
                val obj = JSONObject().apply {
                    put("id", r.id)
                    put("name", r.name)
                    put("category", r.category)
                    put("imageId", r.imageId)
                    put("startTime", r.startTime)
                    put("endTime", r.endTime ?: "")
                    put("isSingleTime", r.isSingleTime)
                    put("orderIndex", r.orderIndex)
                    val daysArr = JSONArray()
                    r.days.forEach { daysArr.put(it) }
                    put("days", daysArr)
                }
                rArray.put(obj)
            }

            // Save today's logs (routineId -> isDone)
            val logsObj = JSONObject()
            for ((key, log) in logs) {
                if (log.date == todayStr || key.startsWith(todayStr)) {
                    logsObj.put(log.routineId, log.isDone)
                }
            }

            getPrefs(context).edit()
                .putString(KEY_ROUTINES_JSON, rArray.toString())
                .putString(KEY_LOGS_JSON, logsObj.toString())
                .putString(KEY_CACHED_DATE, todayStr)
                .apply()

            refreshAllWidgets(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widget cache", e)
        }
    }

    /**
     * Reads all cached routines.
     */
    fun getAllCachedRoutines(context: Context): List<Routine> {
        val prefs = getPrefs(context)
        val jsonStr = prefs.getString(KEY_ROUTINES_JSON, null) ?: return emptyList()
        val list = mutableListOf<Routine>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val daysArr = obj.optJSONArray("days")
                val daysList = mutableListOf<String>()
                if (daysArr != null) {
                    for (j in 0 until daysArr.length()) {
                        daysList.add(daysArr.getString(j))
                    }
                }
                list.add(
                    Routine(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        category = obj.optString("category", "Zihin"),
                        imageId = obj.optString("imageId", "meditasyon_calm"),
                        startTime = obj.optString("startTime", "08:00"),
                        endTime = obj.optString("endTime").takeIf { it.isNotBlank() },
                        isSingleTime = obj.optBoolean("isSingleTime", true),
                        days = if (daysList.isEmpty()) listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz") else daysList,
                        orderIndex = obj.optInt("orderIndex", 0)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading cached routines", e)
        }
        return list
    }

    /**
     * Returns today's routines, sorted chronologically.
     */
    fun getTodayRoutines(context: Context): List<Routine> {
        val today = LocalDate.now()
        val dayOfWeekInt = today.dayOfWeek.value // 1..7
        val all = getAllCachedRoutines(context)
        return all
            .filter { it.daysIntSet.contains(dayOfWeekInt) }
            .sortedWith(compareBy({ it.timeToMinutes }, { it.orderIndex }))
    }

    /**
     * Checks if a routine is completed today.
     */
    fun isRoutineDone(context: Context, routineId: String): Boolean {
        val prefs = getPrefs(context)
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val cachedDate = prefs.getString(KEY_CACHED_DATE, "")
        if (cachedDate != today) {
            // New day, reset completed flags in cache
            prefs.edit().putString(KEY_CACHED_DATE, today).remove(KEY_LOGS_JSON).apply()
            return false
        }
        val jsonStr = prefs.getString(KEY_LOGS_JSON, null) ?: return false
        return try {
            val obj = JSONObject(jsonStr)
            obj.optBoolean(routineId, false)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Toggles routine status directly from widget.
     * Updates cache immediately, refreshes widgets, and syncs to Firestore.
     */
    fun toggleRoutineDone(context: Context, routineId: String) {
        val isDone = isRoutineDone(context, routineId)
        val newDoneState = !isDone
        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

        // 1. Update local cache immediately
        val prefs = getPrefs(context)
        try {
            val existingJson = prefs.getString(KEY_LOGS_JSON, null)
            val obj = if (existingJson != null) JSONObject(existingJson) else JSONObject()
            obj.put(routineId, newDoneState)
            prefs.edit()
                .putString(KEY_LOGS_JSON, obj.toString())
                .putString(KEY_CACHED_DATE, todayStr)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating local widget logs", e)
        }

        // 2. Immediately refresh all widgets for responsive UX
        refreshAllWidgets(context)

        // 3. Persist to Firestore in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val key = "${todayStr}_${routineId}"
                val repository = FirestoreRepository()
                if (newDoneState) {
                    val log = RoutineLog(
                        id = key,
                        routineId = routineId,
                        date = todayStr,
                        status = "done",
                        note = "",
                        completedAt = Timestamp.now()
                    )
                    repository.saveLog(log)
                } else {
                    repository.deleteLog(key)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing widget toggle to Firestore", e)
            }
        }
    }

    /**
     * Calculates statistics and active/next routines for today.
     */
    fun getStats(context: Context): WidgetStats {
        val todayRoutines = getTodayRoutines(context)
        val total = todayRoutines.size
        if (total == 0) return WidgetStats()

        var completed = 0
        val now = LocalTime.now()
        var active: Routine? = null
        var next: Routine? = null

        for (r in todayRoutines) {
            val done = isRoutineDone(context, r.id)
            if (done) completed++

            if (active == null) {
                val start = try { LocalTime.parse(r.startTime) } catch (e: Exception) { LocalTime.of(8, 0) }
                val end = if (r.isRange && !r.endTime.isNullOrBlank()) {
                    try { LocalTime.parse(r.endTime) } catch (e: Exception) { start.plusHours(1) }
                } else {
                    start.plusHours(1)
                }
                if (!now.isBefore(start) && !now.isAfter(end)) {
                    active = r
                }
            }

            if (next == null && !done) {
                val start = try { LocalTime.parse(r.startTime) } catch (e: Exception) { LocalTime.of(8, 0) }
                if (start.isAfter(now)) {
                    next = r
                }
            }
        }

        // If next is null and there's an unmarked routine, pick first unmarked
        if (next == null) {
            next = todayRoutines.firstOrNull { !isRoutineDone(context, it.id) }
        }

        val percent = if (total > 0) ((completed * 100) / total).coerceIn(0, 100) else 0
        val isAllDone = total > 0 && completed == total

        return WidgetStats(
            total = total,
            completed = completed,
            percent = percent,
            activeRoutine = active,
            nextRoutine = next,
            isAllDone = isAllDone
        )
    }

    /**
     * Formats current date in Turkish (e.g. "13 Ekim Salı")
     */
    fun getFormattedTodayDate(): String {
        val today = LocalDate.now()
        val monthNames = arrayOf(
            "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
            "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
        )
        val dayNames = arrayOf(
            "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar"
        )
        val dayOfMonth = today.dayOfMonth
        val monthName = monthNames[today.monthValue - 1]
        val dayName = dayNames[today.dayOfWeek.value - 1]
        return "$dayOfMonth $monthName $dayName"
    }

    /**
     * Refreshes all 3 widget variations across the home screen.
     */
    fun refreshAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return

        // 1. Refresh TodayRoutinesListWidgetProvider
        val listWidgetComponent = ComponentName(context, TodayRoutinesListWidgetProvider::class.java)
        val listIds = appWidgetManager.getAppWidgetIds(listWidgetComponent)
        if (listIds.isNotEmpty()) {
            appWidgetManager.notifyAppWidgetViewDataChanged(listIds, R.id.widget_list_view)
            val intent = Intent(context, TodayRoutinesListWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, listIds)
            }
            context.sendBroadcast(intent)
        }

        // 2. Refresh NextRoutineGlanceWidgetProvider
        val glanceComponent = ComponentName(context, NextRoutineGlanceWidgetProvider::class.java)
        val glanceIds = appWidgetManager.getAppWidgetIds(glanceComponent)
        if (glanceIds.isNotEmpty()) {
            val intent = Intent(context, NextRoutineGlanceWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, glanceIds)
            }
            context.sendBroadcast(intent)
        }

        // 3. Refresh TodaySummaryWidgetProvider
        val summaryComponent = ComponentName(context, TodaySummaryWidgetProvider::class.java)
        val summaryIds = appWidgetManager.getAppWidgetIds(summaryComponent)
        if (summaryIds.isNotEmpty()) {
            val intent = Intent(context, TodaySummaryWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, summaryIds)
            }
            context.sendBroadcast(intent)
        }
    }
}
