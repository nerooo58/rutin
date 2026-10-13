package com.nororoutine.batu.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "BootReceiver triggered with action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            // 1. Immediately reschedule from cache
            try {
                NotificationScheduler.createNotificationChannel(context)
                NotificationScheduler.rescheduleAllFromCache(context)
                NotificationScheduler.schedulePeriodicCheck(context)
                Log.d(TAG, "Alarms rescheduled from local cache successfully on boot")
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling alarms from cache on boot", e)
            }

            // 2. Also enqueue RescheduleWorker to ensure background reliability
            try {
                val workRequest = OneTimeWorkRequestBuilder<RescheduleWorker>().build()
                WorkManager.getInstance(context).enqueue(workRequest)
            } catch (e: Exception) {
                Log.e(TAG, "Error enqueueing RescheduleWorker on boot", e)
            }

            // 3. Refresh home screen widgets on reboot
            try {
                com.nororoutine.batu.widget.WidgetDataManager.refreshAllWidgets(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing widgets on boot", e)
            }
        }
    }
}
