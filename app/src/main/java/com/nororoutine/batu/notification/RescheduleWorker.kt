package com.nororoutine.batu.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class RescheduleWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "RescheduleWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "RescheduleWorker executing background check")
        return try {
            NotificationScheduler.createNotificationChannel(applicationContext)
            NotificationScheduler.rescheduleAllFromCache(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in RescheduleWorker", e)
            Result.retry()
        }
    }
}
