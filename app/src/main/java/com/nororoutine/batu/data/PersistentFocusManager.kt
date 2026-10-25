package com.nororoutine.batu.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages persistent active focus sessions using SharedPreferences and system timestamps.
 * Each routine maintains its own independent timer state so switching routines or navigating
 * never resets, interferes with, or stops any routine's timer.
 */
object PersistentFocusManager {
    private const val PREFS_NAME = "persistent_focus_timer_prefs_v2"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun keyRunning(routineId: String) = "routine_${routineId}_is_running"
    private fun keyStartTime(routineId: String) = "routine_${routineId}_start_time"
    private fun keyAccumulated(routineId: String) = "routine_${routineId}_accumulated_seconds"
    private fun keyCountdown(routineId: String) = "routine_${routineId}_is_countdown"
    private fun keyTarget(routineId: String) = "routine_${routineId}_target_seconds"

    fun startOrResume(
        context: Context,
        routineId: String,
        isCountdown: Boolean = false,
        targetSeconds: Int = 1500
    ) {
        val prefs = getPrefs(context)
        val isRunning = prefs.getBoolean(keyRunning(routineId), false)

        val editor = prefs.edit()
        if (!isRunning) {
            editor.putLong(keyStartTime(routineId), System.currentTimeMillis())
            editor.putBoolean(keyRunning(routineId), true)
            editor.putBoolean(keyCountdown(routineId), isCountdown)
            editor.putInt(keyTarget(routineId), targetSeconds)
            editor.apply()
        }
    }

    fun pause(context: Context, routineId: String? = null) {
        val prefs = getPrefs(context)
        val editor = prefs.edit()

        if (routineId != null) {
            val isRunning = prefs.getBoolean(keyRunning(routineId), false)
            if (isRunning) {
                val startTime = prefs.getLong(keyStartTime(routineId), System.currentTimeMillis())
                val delta = ((System.currentTimeMillis() - startTime) / 1000).toInt().coerceAtLeast(0)
                val prev = prefs.getInt(keyAccumulated(routineId), 0)
                editor.putInt(keyAccumulated(routineId), prev + delta)
                editor.putBoolean(keyRunning(routineId), false)
                editor.apply()
            }
        } else {
            // Pause all running routines if no specific ID provided
            val allKeys = prefs.all.keys
            for (key in allKeys) {
                if (key.endsWith("_is_running") && prefs.getBoolean(key, false)) {
                    val rId = key.removePrefix("routine_").removeSuffix("_is_running")
                    val startTime = prefs.getLong(keyStartTime(rId), System.currentTimeMillis())
                    val delta = ((System.currentTimeMillis() - startTime) / 1000).toInt().coerceAtLeast(0)
                    val prev = prefs.getInt(keyAccumulated(rId), 0)
                    editor.putInt(keyAccumulated(rId), prev + delta)
                    editor.putBoolean(key, false)
                }
            }
            editor.apply()
        }
    }

    fun reset(context: Context, routineId: String) {
        getPrefs(context).edit()
            .putLong(keyStartTime(routineId), System.currentTimeMillis())
            .putInt(keyAccumulated(routineId), 0)
            .putBoolean(keyRunning(routineId), false)
            .apply()
    }

    fun clear(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    fun isRunning(context: Context, routineId: String): Boolean {
        return getPrefs(context).getBoolean(keyRunning(routineId), false)
    }

    fun getElapsedSeconds(context: Context, routineId: String): Int {
        val prefs = getPrefs(context)
        val acc = prefs.getInt(keyAccumulated(routineId), 0)
        val isRunning = prefs.getBoolean(keyRunning(routineId), false)

        return if (isRunning) {
            val startTime = prefs.getLong(keyStartTime(routineId), System.currentTimeMillis())
            val delta = ((System.currentTimeMillis() - startTime) / 1000).toInt().coerceAtLeast(0)
            acc + delta
        } else {
            acc
        }
    }

    /**
     * Formats duration into full HH:MM:SS digital timer display.
     */
    fun formatHms(totalSeconds: Int): String {
        val safeSeconds = totalSeconds.coerceAtLeast(0)
        val hours = safeSeconds / 3600
        val minutes = (safeSeconds % 3600) / 60
        val seconds = safeSeconds % 60
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }
}
