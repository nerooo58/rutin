package com.nororoutine.batu.data

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.nororoutine.batu.model.Routine
import com.nororoutine.batu.model.RoutineLog
import com.nororoutine.batu.model.UserStats
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object NoroContextSync {
    private const val TAG = "NoroContextSync"
    private const val COLLECTION_NORO_CONTEXT = "noro_context"
    private const val DOC_LIVE_STATUS = "live_status"

    private val db: FirebaseFirestore?
        get() {
            return try {
                if (FirebaseApp.getApps(com.nororoutine.batu.NoroRoutineApp.instance).isNotEmpty()) {
                    FirebaseFirestore.getInstance()
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Firebase unavailable for Noro sync", e)
                null
            }
        }

    /**
     * Publishes a rich, AI-ready snapshot of today's routines, focus durations,
     * subtasks, notes, and streak to Firestore under noro_context/live_status.
     * Noro AI Assistant can simply listen to this single document to know everything.
     */
    fun sync(
        routines: List<Routine>,
        logs: Map<String, RoutineLog>,
        stats: UserStats,
        today: LocalDate
    ) {
        val firestore = db ?: return

        try {
            val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val todayDayOfWeek = today.dayOfWeek.value // 1..7 (1=Pzt..7=Paz)

            // Routines scheduled for today
            val todayRoutines = routines.filter { r ->
                r.daysIntSet.isEmpty() || r.daysIntSet.contains(todayDayOfWeek)
            }

            var completedCount = 0
            var totalFocusSeconds = 0L
            val categoryMinutes = mutableMapOf<String, Long>()

            val routineDetailsList = mutableListOf<Map<String, Any?>>()
            val completedNames = mutableListOf<String>()
            val pendingNames = mutableListOf<String>()

            todayRoutines.forEach { r ->
                val logKey = "${todayStr}_${r.id}"
                val log = logs[logKey]
                val isDone = log?.isDone == true

                if (isDone) {
                    completedCount++
                    completedNames.add(r.name)
                } else {
                    pendingNames.add(r.name)
                }

                val focusSec = (log?.focusDurationSeconds ?: 0).toLong()
                totalFocusSeconds += focusSec

                val focusMin = focusSec / 60L
                if (focusMin > 0L) {
                    categoryMinutes[r.category] = (categoryMinutes[r.category] ?: 0L) + focusMin
                }

                val subtaskDetails = r.subtasks.map { subtaskName ->
                    mapOf(
                        "title" to subtaskName,
                        "isDone" to (log?.completedSubtasks?.contains(subtaskName) == true)
                    )
                }

                routineDetailsList.add(
                    mapOf(
                        "id" to r.id,
                        "name" to r.name,
                        "category" to r.category,
                        "time" to r.startTime.ifEmpty { r.time },
                        "isDone" to isDone,
                        "focusMinutes" to focusMin,
                        "note" to (log?.note ?: ""),
                        "sessionNotes" to (log?.focusSessionNotes ?: emptyList<String>()),
                        "subtasks" to subtaskDetails
                    )
                )
            }

            val totalCount = todayRoutines.size
            val completionRate = if (totalCount > 0) ((completedCount.toDouble() / totalCount.toDouble()) * 100).toInt() else 0
            val totalFocusHours = totalFocusSeconds / 3600L
            val remainingMinutes = (totalFocusSeconds % 3600L) / 60L

            val focusFormatted = if (totalFocusHours > 0L) {
                "${totalFocusHours} saat ${remainingMinutes} dakika"
            } else {
                "${remainingMinutes} dakika"
            }

            // AI ready pre-formatted context text for Noro Assistant LLM prompt
            val aiBriefBuilder = StringBuilder()
            aiBriefBuilder.append("Kullanıcı Adı: Batu. ")
            aiBriefBuilder.append("Tarih: $todayStr. ")
            aiBriefBuilder.append("Günün Rutin Durumu: $totalCount rutinden $completedCount tanesi tamamlandı (%$completionRate). ")
            aiBriefBuilder.append("Toplam Odaklanma / Çalışma Süresi: $focusFormatted. ")
            aiBriefBuilder.append("Aktif Seri (Streak): ${stats.currentStreak} gün. ")

            if (categoryMinutes.isNotEmpty()) {
                val catText = categoryMinutes.entries.joinToString(", ") { "${it.key}: ${it.value} dk" }
                aiBriefBuilder.append("Kategori Bazında Çalışma: [$catText]. ")
            }

            if (completedNames.isNotEmpty()) {
                aiBriefBuilder.append("Tamamlanan Rutinler: ${completedNames.joinToString(", ")}. ")
            }

            if (pendingNames.isNotEmpty()) {
                aiBriefBuilder.append("Henüz Yapılmayan Rutinler: ${pendingNames.joinToString(", ")}. ")
            }

            val payload = hashMapOf<String, Any?>(
                "updatedAt" to Timestamp.now(),
                "date" to todayStr,
                "user" to "Batu",
                "totalRoutines" to totalCount,
                "completedRoutines" to completedCount,
                "completionPercentage" to completionRate,
                "currentStreak" to stats.currentStreak,
                "totalFocusSeconds" to totalFocusSeconds,
                "totalFocusMinutes" to (totalFocusSeconds / 60L),
                "totalFocusFormatted" to focusFormatted,
                "categoryMinutes" to categoryMinutes,
                "routines" to routineDetailsList,
                "completedList" to completedNames,
                "pendingList" to pendingNames,
                "aiContextPrompt" to aiBriefBuilder.toString()
            )

            firestore.collection(COLLECTION_NORO_CONTEXT).document(DOC_LIVE_STATUS)
                .set(payload, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Noro AI context successfully synced to Firestore!")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to sync Noro AI context", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Noro context sync", e)
        }
    }
}
