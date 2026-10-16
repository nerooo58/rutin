package com.nororoutine.batu.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages persistent active focus sessions using SharedPreferences and system timestamps.
 * This guarantees that even if the app is closed, killed by the OS, backgrounded,
 * or the device is turned off, the elapsed time continues accurately without interruption.
 */
object PersistentFocusManager {
    private const val PREFS_NAME = "persistent_focus_timer_prefs"

    private const val KEY_ROUTINE_ID = "active_routine_id"
    private const val KEY_START_TIME_MILLIS = "active_start_time_millis"
    private const val KEY_ACCUMULATED_SECONDS = "active_accumulated_seconds"
    private const val KEY_IS_RUNNING = "active_is_running"
    private const val KEY_IS_COUNTDOWN = "active_is_countdown"
    private const val KEY_TARGET_SECONDS = "active_target_seconds"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun startOrResume(
        context: Context,
        routineId: String,
        isCountdown: Boolean,
        targetSeconds: Int
    ) {
        val prefs = getPrefs(context)
        val currentActiveId = prefs.getString(KEY_ROUTINE_ID, null)

        val editor = prefs.edit()
        if (currentActiveId != routineId) {
            // New routine session
            editor.putString(KEY_ROUTINE_ID, routineId)
            editor.putLong(KEY_START_TIME_MILLIS, System.currentTimeMillis())
            editor.putInt(KEY_ACCUMULATED_SECONDS, 0)
            editor.putBoolean(KEY_IS_RUNNING, true)
            editor.putBoolean(KEY_IS_COUNTDOWN, isCountdown)
            editor.putInt(KEY_TARGET_SECONDS, targetSeconds)
        } else {
            // Resume existing
            val isRunning = prefs.getBoolean(KEY_IS_RUNNING, false)
            if (!isRunning) {
                editor.putLong(KEY_START_TIME_MILLIS, System.currentTimeMillis())
                editor.putBoolean(KEY_IS_RUNNING, true)
            }
        }
        editor.apply()
    }

    fun pause(context: Context) {
        val prefs = getPrefs(context)
        val isRunning = prefs.getBoolean(KEY_IS_RUNNING, false)
        if (isRunning) {
            val startTime = prefs.getLong(KEY_START_TIME_MILLIS, System.currentTimeMillis())
            val deltaSec = ((System.currentTimeMillis() - startTime) / 1000).toInt().coerceAtLeast(0)
            val prevAcc = prefs.getInt(KEY_ACCUMULATED_SECONDS, 0)
            val newAcc = prevAcc + deltaSec

            prefs.edit()
                .putInt(KEY_ACCUMULATED_SECONDS, newAcc)
                .putBoolean(KEY_IS_RUNNING, false)
                .apply()
        }
    }

    fun reset(context: Context, routineId: String) {
        val prefs = getPrefs(context)
        val currentId = prefs.getString(KEY_ROUTINE_ID, null)
        if (currentId == routineId) {
            val isCountdown = prefs.getBoolean(KEY_IS_COUNTDOWN, false)
            val target = prefs.getInt(KEY_TARGET_SECONDS, 0)
            prefs.edit()
                .putLong(KEY_START_TIME_MILLIS, System.currentTimeMillis())
                .putInt(KEY_ACCUMULATED_SECONDS, 0)
                .putBoolean(KEY_IS_RUNNING, false)
                .putBoolean(KEY_IS_COUNTDOWN, isCountdown)
                .putInt(KEY_TARGET_SECONDS, target)
                .apply()
        }
    }

    fun clear(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    fun getActiveRoutineId(context: Context): String? {
        return getPrefs(context).getString(KEY_ROUTINE_ID, null)
    }

    fun isRunning(context: Context, routineId: String): Boolean {
        val prefs = getPrefs(context)
        val currentId = prefs.getString(KEY_ROUTINE_ID, null)
        return currentId == routineId && prefs.getBoolean(KEY_IS_RUNNING, false)
    }

    fun getElapsedSeconds(context: Context, routineId: String): Int {
        val prefs = getPrefs(context)
        val currentId = prefs.getString(KEY_ROUTINE_ID, null)
        if (currentId != routineId) return 0

        val acc = prefs.getInt(KEY_ACCUMULATED_SECONDS, 0)
        val isRunning = prefs.getBoolean(KEY_IS_RUNNING, false)
        return if (isRunning) {
            val startTime = prefs.getLong(KEY_START_TIME_MILLIS, System.currentTimeMillis())
            val delta = ((System.currentTimeMillis() - startTime) / 1000).toInt().coerceAtLeast(0)
            acc + delta
        } else {
            acc
        }
    }
}
