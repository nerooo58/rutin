package com.nororoutine.batu.data

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.nororoutine.batu.model.CategoryItem
import com.nororoutine.batu.model.Routine
import com.nororoutine.batu.model.RoutineLog
import com.nororoutine.batu.model.SettingsData
import com.nororoutine.batu.model.UserStats
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

class FirestoreRepository {

    companion object {
        private const val TAG = "FirestoreRepository"
        private const val COLLECTION_ROUTINES = "routines"
        private const val COLLECTION_LOGS = "logs"
        private const val COLLECTION_CATEGORIES = "categories"
        private const val COLLECTION_SETTINGS = "settings"
        private const val COLLECTION_STATS = "stats"
        const val DEFAULT_USER_ID = "user_default"
    }

    private val db: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(com.nororoutine.batu.NoroRoutineApp.instance).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else {
                Log.w(TAG, "FirebaseApp is not initialized; Firestore unavailable")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FirebaseFirestore instance", e)
            null
        }
    }

    /**
     * Real-time snapshot listener for routines collection.
     * Emits a sorted list of routines whenever the collection changes in Firestore.
     */
    fun observeRoutines(): Flow<List<Routine>> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            Log.w(TAG, "observeRoutines: Firestore instance is null, closing stream")
            awaitClose { }
            return@callbackFlow
        }

        var registration: ListenerRegistration? = null
        try {
            registration = firestore.collection(COLLECTION_ROUTINES)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error observing routines from Firestore", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val routines = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Routine::class.java)?.copy(id = doc.id)
                        }.sortedWith(compareBy({ it.timeToMinutes }, { it.orderIndex }))
                        trySend(routines)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception attaching snapshot listener to routines", e)
        }

        awaitClose { registration?.remove() }
    }

    /**
     * Real-time snapshot listener for logs collection.
     * Emits all logs whenever changes occur.
     */
    fun observeLogs(): Flow<List<RoutineLog>> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            Log.w(TAG, "observeLogs: Firestore instance is null, closing stream")
            awaitClose { }
            return@callbackFlow
        }

        var registration: ListenerRegistration? = null
        try {
            registration = firestore.collection(COLLECTION_LOGS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error observing logs from Firestore", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val logs = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(RoutineLog::class.java)?.copy(id = doc.id)
                        }
                        trySend(logs)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception attaching snapshot listener to logs", e)
        }

        awaitClose { registration?.remove() }
    }

    /**
     * Real-time snapshot listener for categories collection.
     */
    fun observeCategories(): Flow<List<CategoryItem>> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            Log.w(TAG, "observeCategories: Firestore instance is null, closing stream")
            awaitClose { }
            return@callbackFlow
        }

        var registration: ListenerRegistration? = null
        try {
            registration = firestore.collection(COLLECTION_CATEGORIES)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error observing categories from Firestore", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val categories = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(CategoryItem::class.java)?.copy(id = doc.id)
                        }
                        trySend(categories)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception attaching snapshot listener to categories", e)
        }

        awaitClose { registration?.remove() }
    }

    /**
     * Real-time snapshot listener for user settings.
     */
    fun observeSettings(userId: String = DEFAULT_USER_ID): Flow<SettingsData?> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            Log.w(TAG, "observeSettings: Firestore instance is null, closing stream")
            awaitClose { }
            return@callbackFlow
        }

        var registration: ListenerRegistration? = null
        try {
            registration = firestore.collection(COLLECTION_SETTINGS).document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error observing settings from Firestore", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        trySend(snapshot.toObject(SettingsData::class.java))
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception attaching snapshot listener to settings", e)
        }

        awaitClose { registration?.remove() }
    }

    /**
     * Real-time snapshot listener for user stats (streak, daily, weekly, monthly, yearly completions).
     */
    fun observeStats(userId: String = DEFAULT_USER_ID): Flow<UserStats?> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            Log.w(TAG, "observeStats: Firestore instance is null, closing stream")
            awaitClose { }
            return@callbackFlow
        }

        var registration: ListenerRegistration? = null
        try {
            registration = firestore.collection(COLLECTION_STATS).document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error observing stats from Firestore", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        trySend(snapshot.toObject(UserStats::class.java))
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception attaching snapshot listener to stats", e)
        }

        awaitClose { registration?.remove() }
    }

    /**
     * Create or update a routine in the routines collection.
     */
    fun saveRoutine(routine: Routine) {
        val firestore = db
        if (firestore == null) {
            Log.w(TAG, "saveRoutine: Firestore not available, skipping save")
            return
        }

        try {
            val docId = routine.id.ifEmpty { UUID.randomUUID().toString() }
            val toSave = routine.copy(
                id = docId,
                createdAt = routine.createdAt ?: Timestamp.now()
            )
            val payload = hashMapOf<String, Any?>(
                "name" to toSave.name,
                "category" to toSave.category,
                "imageId" to toSave.imageId,
                "startTime" to toSave.startTime,
                "endTime" to toSave.endTime,
                "isSingleTime" to toSave.isSingleTime,
                "days" to toSave.days,
                "notificationEnabled" to toSave.notificationEnabled,
                "createdAt" to (toSave.createdAt ?: Timestamp.now()),
                "orderIndex" to toSave.orderIndex
            )

            firestore.collection(COLLECTION_ROUTINES).document(docId)
                .set(payload, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Routine successfully saved: $docId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to save routine: $docId", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in saveRoutine", e)
        }
    }

    /**
     * Delete a routine from routines collection.
     */
    fun deleteRoutine(routineId: String) {
        val firestore = db
        if (firestore == null) {
            Log.w(TAG, "deleteRoutine: Firestore not available, skipping delete")
            return
        }

        try {
            firestore.collection(COLLECTION_ROUTINES).document(routineId)
                .delete()
                .addOnSuccessListener {
                    Log.d(TAG, "Routine successfully deleted: $routineId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to delete routine: $routineId", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in deleteRoutine", e)
        }
    }

    /**
     * Save a log in the logs collection.
     * Document ID is {date}_{routineId} to allow fast lookups and overwrite per day.
     */
    fun saveLog(log: RoutineLog) {
        val firestore = db
        if (firestore == null) {
            Log.w(TAG, "saveLog: Firestore not available, skipping save")
            return
        }

        try {
            val docId = log.id.ifEmpty { "${log.date}_${log.routineId}" }
            val payload = hashMapOf<String, Any?>(
                "routineId" to log.routineId,
                "date" to log.date,
                "status" to log.status,
                "note" to log.note,
                "completedAt" to (log.completedAt ?: Timestamp.now())
            )

            firestore.collection(COLLECTION_LOGS).document(docId)
                .set(payload, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Log successfully saved: $docId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to save log: $docId", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in saveLog", e)
        }
    }

    /**
     * Delete a log from logs collection (e.g. when marking empty / unchecking a routine).
     */
    fun deleteLog(logId: String) {
        val firestore = db
        if (firestore == null) {
            Log.w(TAG, "deleteLog: Firestore not available, skipping delete")
            return
        }

        try {
            firestore.collection(COLLECTION_LOGS).document(logId)
                .delete()
                .addOnSuccessListener {
                    Log.d(TAG, "Log successfully deleted: $logId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to delete log: $logId", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in deleteLog", e)
        }
    }

    /**
     * Save a new category to categories collection.
     */
    fun saveCategory(name: String) {
        val firestore = db
        if (firestore == null) {
            Log.w(TAG, "saveCategory: Firestore not available, skipping save")
            return
        }

        try {
            val trimmed = name.trim()
            if (trimmed.isBlank()) return
            val docId = trimmed.lowercase().replace("\\s+".toRegex(), "_")
            val payload = hashMapOf<String, Any?>(
                "name" to trimmed,
                "createdAt" to Timestamp.now()
            )

            firestore.collection(COLLECTION_CATEGORIES).document(docId)
                .set(payload, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Category successfully saved: $trimmed")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to save category: $trimmed", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in saveCategory", e)
        }
    }

    /**
     * Save settings to settings collection.
     */
    fun saveSettings(settings: SettingsData, userId: String = DEFAULT_USER_ID) {
        val firestore = db
        if (firestore == null) {
            Log.w(TAG, "saveSettings: Firestore not available, skipping save")
            return
        }

        try {
            val payload = hashMapOf<String, Any?>(
                "notificationsEnabled" to settings.notificationsEnabled,
                "reminderMinutes" to settings.reminderMinutes,
                "theme" to settings.theme,
                "lastSync" to Timestamp.now()
            )

            firestore.collection(COLLECTION_SETTINGS).document(userId)
                .set(payload, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Settings successfully saved for $userId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to save settings for $userId", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in saveSettings", e)
        }
    }

    /**
     * Save user completion stats and streak to stats collection in Firestore.
     */
    fun saveStats(stats: UserStats, userId: String = DEFAULT_USER_ID) {
        val firestore = db
        if (firestore == null) {
            Log.w(TAG, "saveStats: Firestore not available, skipping save")
            return
        }

        try {
            val payload = hashMapOf<String, Any?>(
                "todayCompleted" to stats.todayCompleted,
                "todayTotal" to stats.todayTotal,
                "todayPercent" to stats.todayPercent,
                "currentStreak" to stats.currentStreak,
                "bestStreak" to stats.bestStreak,
                "weeklyCompleted" to stats.weeklyCompleted,
                "weeklyTotal" to stats.weeklyTotal,
                "weeklyPercent" to stats.weeklyPercent,
                "monthlyCompleted" to stats.monthlyCompleted,
                "monthlyTotal" to stats.monthlyTotal,
                "monthlyPercent" to stats.monthlyPercent,
                "yearlyCompleted" to stats.yearlyCompleted,
                "yearlyTotal" to stats.yearlyTotal,
                "yearlyPercent" to stats.yearlyPercent,
                "lastUpdated" to (stats.lastUpdated ?: Timestamp.now())
            )

            firestore.collection(COLLECTION_STATS).document(userId)
                .set(payload, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "User stats successfully saved to Firestore for $userId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to save user stats for $userId", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in saveStats", e)
        }
    }

    /**
     * Resets all routines and logs from Firestore.
     */
    fun resetAllData(userId: String = DEFAULT_USER_ID) {
        val firestore = db
        if (firestore == null) {
            Log.w(TAG, "resetAllData: Firestore not available, skipping reset")
            return
        }

        try {
            firestore.collection(COLLECTION_ROUTINES).get().addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit()
            }
            firestore.collection(COLLECTION_LOGS).get().addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit()
            }
            firestore.collection(COLLECTION_STATS).document(userId).delete()
        } catch (e: Exception) {
            Log.e(TAG, "Exception in resetAllData", e)
        }
    }
}
