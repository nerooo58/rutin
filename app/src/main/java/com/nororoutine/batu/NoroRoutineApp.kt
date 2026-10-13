package com.nororoutine.batu

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.nororoutine.batu.notification.NotificationScheduler

class NoroRoutineApp : Application() {

    companion object {
        private const val TAG = "NoroRoutineApp"
        lateinit var instance: NoroRoutineApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. Initialize Firebase with try-catch and robust fallback
        initFirebase()

        // 2. Global uncaught exception handler to prevent silent crashes
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "FATAL UNCAUGHT EXCEPTION on thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        try {
            // Setup notification channel
            NotificationScheduler.createNotificationChannel(this)
            // Register periodic background rescheduling worker
            NotificationScheduler.schedulePeriodicCheck(this)
            // Reschedule active alarms on app launch
            NotificationScheduler.rescheduleAllFromCache(this)
            // Refresh widgets with latest cache on app launch
            com.nororoutine.batu.widget.WidgetDataManager.refreshAllWidgets(this)
            Log.d(TAG, "NoroRoutineApp initialized notification channel and scheduled check successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing NoroRoutineApp notification subsystem", e)
        }
    }

    private fun initFirebase() {
        try {
            // First check if already initialized or initialize with default options from google-services.json
            if (FirebaseApp.getApps(this).isEmpty()) {
                val app = FirebaseApp.initializeApp(this)
                if (app != null) {
                    Log.d(TAG, "FirebaseApp.initializeApp(Context) successful: ${app.name}")
                    return
                }
            } else {
                Log.d(TAG, "FirebaseApp already initialized")
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "Default FirebaseApp.initializeApp failed, attempting fallback with FirebaseOptions", e)
        }

        // Fallback initialization using explicit options from google-services.json if resource generation had an issue
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:256301131205:android:f4fea65c4a1b4a8cbf17ac")
                    .setProjectId("noroai-2563a")
                    .setApiKey("AIzaSyCcMTY2WNpFD2v1pv7rJDOYU2IDPSn56lo")
                    .setStorageBucket("noroai-2563a.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.d(TAG, "FirebaseApp fallback initialization with FirebaseOptions succeeded")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fatal: FirebaseApp fallback initialization also failed", e)
        }
    }
}
