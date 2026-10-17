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

        const val FIREBASE_APP_ID = "1:421580678905:android:88407aa4fb9700818b6428"
        const val FIREBASE_PROJECT_ID = "werostudios"
        const val FIREBASE_API_KEY = "AIzaSyBCL16msxF4QY_9tGmOAPCpZqrOfGM5JjE"
        const val FIREBASE_STORAGE_BUCKET = "werostudios.firebasestorage.app"
        const val FIREBASE_GCM_SENDER_ID = "421580678905"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. Initialize Firebase with valid configuration and robust options
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
        val validOptions = FirebaseOptions.Builder()
            .setApplicationId(FIREBASE_APP_ID)
            .setProjectId(FIREBASE_PROJECT_ID)
            .setApiKey(FIREBASE_API_KEY)
            .setStorageBucket(FIREBASE_STORAGE_BUCKET)
            .setGcmSenderId(FIREBASE_GCM_SENDER_ID)
            .build()

        try {
            val apps = FirebaseApp.getApps(this)
            if (apps.isNotEmpty()) {
                val current = FirebaseApp.getInstance()
                val currentOptions = current.options
                if (currentOptions.applicationId == FIREBASE_APP_ID &&
                    currentOptions.projectId == FIREBASE_PROJECT_ID &&
                    currentOptions.apiKey == FIREBASE_API_KEY) {
                    Log.d(TAG, "FirebaseApp already initialized with valid options: ${current.name}")
                    return
                } else {
                    Log.w(TAG, "FirebaseApp had outdated/mismatched options (${currentOptions.projectId}), deleting to recreate...")
                    current.delete()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception checking existing FirebaseApp instance", e)
        }

        // Try standard resource-based initialization first
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val app = FirebaseApp.initializeApp(this)
                if (app != null && app.options.applicationId == FIREBASE_APP_ID && app.options.projectId == FIREBASE_PROJECT_ID) {
                    Log.d(TAG, "FirebaseApp initialized via google-services resources: ${app.name}")
                    return
                } else if (app != null) {
                    Log.w(TAG, "FirebaseApp initialized with unexpected resource config, deleting to apply explicit options")
                    app.delete()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Default FirebaseApp.initializeApp(Context) failed, applying explicit options", e)
        }

        // Explicit initialization using exact credentials from google-services.json
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val app = FirebaseApp.initializeApp(this, validOptions)
                Log.d(TAG, "FirebaseApp explicitly initialized with werostudios options: ${app.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fatal: FirebaseApp initialization with explicit options failed", e)
        }
    }
}
