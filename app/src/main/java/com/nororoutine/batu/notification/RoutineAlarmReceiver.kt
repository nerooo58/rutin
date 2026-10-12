package com.nororoutine.batu.notification

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nororoutine.batu.MainActivity
import com.nororoutine.batu.R

class RoutineAlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "RoutineAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val routineId = intent.getStringExtra(NotificationScheduler.EXTRA_ROUTINE_ID) ?: return
        val routineName = intent.getStringExtra(NotificationScheduler.EXTRA_ROUTINE_NAME) ?: "Rutin"
        val dayOfWeek = intent.getIntExtra(NotificationScheduler.EXTRA_DAY_OF_WEEK, -1)
        val hour = intent.getIntExtra(NotificationScheduler.EXTRA_HOUR, 8)
        val minute = intent.getIntExtra(NotificationScheduler.EXTRA_MINUTE, 0)

        Log.d(TAG, "Alarm received for routine: '$routineName' ($routineId), day: $dayOfWeek, time: $hour:$minute")

        // Check if notifications are globally enabled
        if (!NotificationScheduler.isNotificationsGloballyEnabled(context)) {
            Log.d(TAG, "Notifications are globally disabled in settings. Skipping notification.")
            return
        }

        // Ensure channel exists
        NotificationScheduler.createNotificationChannel(context)

        // Build Intent to open MainActivity directly on Today's screen (tab 0)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_NAVIGATE_TAB", 0)
            putExtra("EXTRA_ROUTINE_ID", routineId)
        }
        val requestCode = NotificationScheduler.generateRequestCode(routineId, dayOfWeek)
        val openPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val builder = NotificationCompat.Builder(context, NotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_routine)
            .setContentTitle(routineName)
            .setContentText("Rutin zamanı geldi")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Cannot post notification.")
                // Still reschedule for next week
                rescheduleNextWeek(context, routineId, routineName, dayOfWeek, hour, minute)
                return
            }
        }

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(requestCode, builder.build())
            Log.d(TAG, "Notification posted successfully for '$routineName'")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException posting notification", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error posting notification", e)
        }

        // Reschedule this day's alarm for next week so it repeats seamlessly!
        rescheduleNextWeek(context, routineId, routineName, dayOfWeek, hour, minute)
    }

    private fun rescheduleNextWeek(
        context: Context,
        routineId: String,
        routineName: String,
        dayOfWeek: Int,
        hour: Int,
        minute: Int
    ) {
        if (dayOfWeek in 1..7) {
            NotificationScheduler.scheduleAlarmForDay(
                context = context,
                routineId = routineId,
                routineName = routineName,
                dayOfWeek = dayOfWeek,
                hour = hour,
                minute = minute
            )
            Log.d(TAG, "Rescheduled alarm for '$routineName' for next week (day $dayOfWeek)")
        }
    }
}
