package com.nororoutine.batu.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

val DAY_INT_TO_STRING = mapOf(
    1 to "Pzt",
    2 to "Sal",
    3 to "Çar",
    4 to "Per",
    5 to "Cum",
    6 to "Cmt",
    7 to "Paz"
)

val DAY_STRING_TO_INT = mapOf(
    "Pzt" to 1,
    "Sal" to 2,
    "Çar" to 3,
    "Per" to 4,
    "Cum" to 5,
    "Cmt" to 6,
    "Paz" to 7
)

@IgnoreExtraProperties
data class Routine(
    val id: String = "",
    val name: String = "",
    val category: String = "Zihin",
    val imageId: String = "meditasyon_calm",
    val startTime: String = "08:00",
    val endTime: String? = null,
    val isSingleTime: Boolean = true,
    val days: List<String> = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"),
    val notificationEnabled: Boolean = true,
    val createdAt: Timestamp? = null,
    val orderIndex: Int = 0
) {
    // Computed properties for Compose UI compatibility
    val illustrationId: String get() = imageId.ifEmpty { "meditasyon_calm" }
    val isRange: Boolean get() = !isSingleTime
    val time: String get() = if (isSingleTime || endTime.isNullOrBlank()) startTime else "$startTime – $endTime"
    val daysIntSet: Set<Int> get() = days.mapNotNull { DAY_STRING_TO_INT[it] }.toSet()
    val timeToMinutes: Int
        get() {
            val clean = (if (startTime.isNotBlank()) startTime else time).trim()
            return try {
                val firstTime = clean.split("–", "-", " ").first().trim()
                val parts = firstTime.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                h * 60 + m
            } catch (e: Exception) {
                0
            }
        }
}

enum class RoutineStatus {
    UPCOMING,
    ACTIVE,
    DONE,
    EMPTY,
    MISSED
}

@IgnoreExtraProperties
data class RoutineLog(
    val id: String = "",
    val routineId: String = "",
    val date: String = "", // yyyy-MM-dd
    val status: String = "done", // done / missed / skipped
    val note: String = "",
    val completedAt: Timestamp? = null
) {
    // Computed UI helper properties
    val dateString: String get() = date
    val isDone: Boolean get() = status == "done"
    val updatedAt: Long get() = completedAt?.toDate()?.time ?: System.currentTimeMillis()
}

@IgnoreExtraProperties
data class CategoryItem(
    val id: String = "",
    val name: String = "",
    val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SettingsData(
    val notificationsEnabled: Boolean = true,
    val reminderMinutes: Int = 5,
    val theme: String = "dark",
    val lastSync: Timestamp? = null
)

@IgnoreExtraProperties
data class UserStats(
    val todayCompleted: Int = 0,
    val todayTotal: Int = 0,
    val todayPercent: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val weeklyCompleted: Int = 0,
    val weeklyTotal: Int = 0,
    val weeklyPercent: Int = 0,
    val monthlyCompleted: Int = 0,
    val monthlyTotal: Int = 0,
    val monthlyPercent: Int = 0,
    val yearlyCompleted: Int = 0,
    val yearlyTotal: Int = 0,
    val yearlyPercent: Int = 0,
    val lastUpdated: Timestamp? = null
)

