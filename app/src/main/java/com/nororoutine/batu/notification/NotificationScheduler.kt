package com.nororoutine.batu.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.nororoutine.batu.model.DAY_STRING_TO_INT
import com.nororoutine.batu.model.Routine
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject

object NotificationScheduler {
    private const val TAG = "NotificationScheduler"
    const val CHANNEL_ID = "routine_reminders_channel"
    private const val PREFS_NAME = "rutin_notification_cache"
    private const val KEY_ROUTINES_JSON = "cached_routines_json"
    private const val KEY_NOTIFICATIONS_ENABLED = "cached_notifications_enabled"
    private const val WORK_NAME_PERIODIC = "RoutinePeriodicRescheduleWork"

    const val EXTRA_ROUTINE_ID = "extra_routine_id"
    const val EXTRA_ROUTINE_NAME = "extra_routine_name"
    const val EXTRA_DAY_OF_WEEK = "extra_day_of_week"
    const val EXTRA_HOUR = "extra_hour"
    const val EXTRA_MINUTE = "extra_minute"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Rutin Hatırlatıcıları"
            val descriptionText = "Rutin zamanı geldiğinde gönderilen bildirimler"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isNotificationsGloballyEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun saveCache(context: Context, routines: List<Routine>, isGlobalEnabled: Boolean) {
        val array = JSONArray()
        for (r in routines) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("name", r.name)
                put("category", r.category)
                put("imageId", r.imageId)
                put("startTime", r.startTime)
                put("endTime", r.endTime ?: "")
                put("isSingleTime", r.isSingleTime)
                put("notificationEnabled", r.notificationEnabled)
                val daysArr = JSONArray()
                r.days.forEach { daysArr.put(it) }
                put("days", daysArr)
            }
            array.put(obj)
        }
        getPrefs(context).edit()
            .putString(KEY_ROUTINES_JSON, array.toString())
            .putBoolean(KEY_NOTIFICATIONS_ENABLED, isGlobalEnabled)
            .apply()
    }

    fun loadCache(context: Context): Pair<List<Routine>, Boolean> {
        val prefs = getPrefs(context)
        val enabled = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        val jsonStr = prefs.getString(KEY_ROUTINES_JSON, null) ?: return Pair(emptyList(), enabled)

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
                        days = if (daysList.isNotEmpty()) daysList else listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"),
                        notificationEnabled = obj.optBoolean("notificationEnabled", true)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading cached routines", e)
        }
        return Pair(list, enabled)
    }

    /**
     * Schedules alarms for a single routine across its selected days.
     * Note: Single-time routines fire at startTime.
     * Range routines (e.g. 16:00-17:00) also fire at startTime only!
     */
    fun scheduleRoutine(context: Context, routine: Routine, isGlobalEnabled: Boolean = true) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // If routine is disabled or user globally disabled notifications, cancel its alarms
        if (!isGlobalEnabled || !routine.notificationEnabled) {
            cancelRoutine(context, routine.id)
            return
        }

        // Parse start time (HH:mm)
        val timeParts = parseHourMinute(routine.startTime)
        val hour = timeParts.first
        val minute = timeParts.second

        val targetDays = routine.daysIntSet.ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) }

        // First cancel any days not selected
        for (day in 1..7) {
            if (!targetDays.contains(day)) {
                cancelAlarmForDay(context, alarmManager, routine.id, day)
            }
        }

        // Schedule for each selected day
        for (day in targetDays) {
            scheduleAlarmForDay(
                context = context,
                alarmManager = alarmManager,
                routineId = routine.id,
                routineName = routine.name,
                dayOfWeek = day,
                hour = hour,
                minute = minute
            )
        }
    }

    fun scheduleAlarmForDay(
        context: Context,
        alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager,
        routineId: String,
        routineName: String,
        dayOfWeek: Int,
        hour: Int,
        minute: Int
    ) {
        val triggerMillis = getNextTriggerMillis(dayOfWeek, hour, minute)
        val pendingIntent = createAlarmPendingIntent(
            context = context,
            routineId = routineId,
            routineName = routineName,
            dayOfWeek = dayOfWeek,
            hour = hour,
            minute = minute
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled alarm for '$routineName' (day $dayOfWeek at $hour:$minute) at $triggerMillis")
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException scheduling exact alarm, falling back to inexact alarm", e)
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to schedule alarm", ex)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm", e)
        }
    }

    fun cancelRoutine(context: Context, routineId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (day in 1..7) {
            cancelAlarmForDay(context, alarmManager, routineId, day)
        }
    }

    private fun cancelAlarmForDay(
        context: Context,
        alarmManager: AlarmManager,
        routineId: String,
        dayOfWeek: Int
    ) {
        val intent = Intent(context, RoutineAlarmReceiver::class.java).apply {
            action = "com.nororoutine.batu.ACTION_ROUTINE_ALARM"
        }
        val requestCode = generateRequestCode(routineId, dayOfWeek)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm for routine $routineId day $dayOfWeek")
        }
    }

    fun cancelAll(context: Context) {
        val (cachedRoutines, _) = loadCache(context)
        for (routine in cachedRoutines) {
            cancelRoutine(context, routine.id)
        }
    }

    fun rescheduleAll(context: Context, routines: List<Routine>, isGlobalEnabled: Boolean) {
        saveCache(context, routines, isGlobalEnabled)
        if (!isGlobalEnabled) {
            cancelAll(context)
            return
        }

        for (routine in routines) {
            if (routine.notificationEnabled) {
                scheduleRoutine(context, routine, isGlobalEnabled = true)
            } else {
                cancelRoutine(context, routine.id)
            }
        }
    }

    fun rescheduleAllFromCache(context: Context) {
        val (cachedRoutines, isEnabled) = loadCache(context)
        if (!isEnabled) {
            cancelAll(context)
            return
        }
        for (routine in cachedRoutines) {
            if (routine.notificationEnabled) {
                scheduleRoutine(context, routine, isGlobalEnabled = true)
            } else {
                cancelRoutine(context, routine.id)
            }
        }
    }

    fun schedulePeriodicCheck(context: Context) {
        try {
            val periodicWork = PeriodicWorkRequestBuilder<RescheduleWorker>(
                24, TimeUnit.HOURS
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWork
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling periodic check", e)
        }
    }

    fun getNextTriggerMillis(dayOfWeekInt: Int, hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        val targetDayOfWeek = try {
            DayOfWeek.of(dayOfWeekInt.coerceIn(1, 7))
        } catch (e: Exception) {
            DayOfWeek.MONDAY
        }

        var target = now.with(TemporalAdjusters.nextOrSame(targetDayOfWeek))
            .withHour(hour.coerceIn(0, 23))
            .withMinute(minute.coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)

        // If target time is before now or within 10 seconds of now, schedule for next week
        if (!target.isAfter(now.plusSeconds(10))) {
            target = target.plusWeeks(1)
        }

        return target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun parseHourMinute(timeStr: String): Pair<Int, Int> {
        return try {
            val clean = timeStr.trim().split("–", "-")[0].trim()
            val parsed = LocalTime.parse(clean)
            Pair(parsed.hour, parsed.minute)
        } catch (e: Exception) {
            try {
                val parts = timeStr.trim().split(":")
                val h = parts[0].trim().toInt()
                val m = if (parts.size > 1) parts[1].trim().take(2).toInt() else 0
                Pair(h, m)
            } catch (ex: Exception) {
                Pair(8, 0)
            }
        }
    }

    private fun createAlarmPendingIntent(
        context: Context,
        routineId: String,
        routineName: String,
        dayOfWeek: Int,
        hour: Int,
        minute: Int
    ): PendingIntent {
        val intent = Intent(context, RoutineAlarmReceiver::class.java).apply {
            action = "com.nororoutine.batu.ACTION_ROUTINE_ALARM"
            putExtra(EXTRA_ROUTINE_ID, routineId)
            putExtra(EXTRA_ROUTINE_NAME, routineName)
            putExtra(EXTRA_DAY_OF_WEEK, dayOfWeek)
            putExtra(EXTRA_HOUR, hour)
            putExtra(EXTRA_MINUTE, minute)
        }
        val requestCode = generateRequestCode(routineId, dayOfWeek)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun generateRequestCode(routineId: String, dayOfWeek: Int): Int {
        // Deterministic integer code combining routineId hash and day of week
        return (routineId.hashCode() * 31 + dayOfWeek) and 0x7FFFFFFF
    }
}
