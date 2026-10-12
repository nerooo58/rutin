package com.nororoutine.batu.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nororoutine.batu.NoroRoutineApp
import com.nororoutine.batu.data.FirestoreRepository
import com.nororoutine.batu.model.DAY_INT_TO_STRING
import com.nororoutine.batu.model.Routine
import com.nororoutine.batu.model.RoutineLog
import com.nororoutine.batu.model.RoutineStatus
import com.nororoutine.batu.model.SettingsData
import com.nororoutine.batu.network.WeatherInfo
import com.nororoutine.batu.network.WeatherService
import com.nororoutine.batu.notification.NotificationScheduler
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class RutinUiState(
    val routines: List<Routine> = emptyList(),
    val logs: Map<String, RoutineLog> = emptyMap(), // key: "${dateString}_${routineId}"
    val categories: List<String> = listOf("Spor", "Zihin", "Beslenme"),
    val weatherInfo: WeatherInfo = WeatherInfo(),
    val isWeatherLoading: Boolean = false,
    val currentTab: Int = 0, // 0: Bugün, 1: Düzenle, 2: Geçmiş, 3: Ayarlar
    val selectedHistoryDate: LocalDate = LocalDate.now(),
    val notificationsEnabled: Boolean = true,
    val reminderMinutes: Int = 5, // 0, 5, 15, 30
    val isDarkMode: Boolean = true,
    val isFirebaseConnected: Boolean = true,
    val lastSyncText: String = "Gerçek zamanlı bağlı",
    val shakeRoutineId: String? = null,
    val shakeTrigger: Int = 0
)

class RutinViewModel(
    private val repository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(RutinUiState())
    val uiState: StateFlow<RutinUiState> = _uiState.asStateFlow()

    init {
        // Start real-time Firestore listeners on launch
        observeFirestoreData()
    }

    private fun observeFirestoreData() {
        // Real-time synchronization for Routines
        viewModelScope.launch {
            repository.observeRoutines().collect { firestoreRoutines ->
                _uiState.update { state ->
                    state.copy(
                        routines = firestoreRoutines,
                        isFirebaseConnected = true
                    )
                }
                // Reschedule all active notifications on app launch / data sync
                try {
                    NotificationScheduler.rescheduleAll(
                        context = NoroRoutineApp.instance,
                        routines = firestoreRoutines,
                        isGlobalEnabled = _uiState.value.notificationsEnabled
                    )
                } catch (e: Exception) {
                    android.util.Log.e("RutinViewModel", "Error syncing routine notifications", e)
                }
            }
        }

        // Real-time synchronization for Logs
        viewModelScope.launch {
            repository.observeLogs().collect { firestoreLogs ->
                val logsMap = firestoreLogs.associateBy { "${it.date}_${it.routineId}" }
                _uiState.update { state ->
                    state.copy(
                        logs = logsMap,
                        isFirebaseConnected = true
                    )
                }
            }
        }

        // Real-time synchronization for Categories
        viewModelScope.launch {
            repository.observeCategories().collect { firestoreCategories ->
                val defaultCats = listOf("Spor", "Zihin", "Beslenme")
                val combined = (defaultCats + firestoreCategories.map { it.name }).distinct()
                _uiState.update { state ->
                    state.copy(
                        categories = combined,
                        isFirebaseConnected = true
                    )
                }
            }
        }

        // Real-time synchronization for Settings
        viewModelScope.launch {
            repository.observeSettings().collect { settings ->
                if (settings != null) {
                    val lastSyncStr = settings.lastSync?.toDate()?.let { date ->
                        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                        "Son eşitleme: ${LocalTime.now().format(timeFormatter)}"
                    } ?: "Gerçek zamanlı bağlı"

                    val oldNotificationsEnabled = _uiState.value.notificationsEnabled
                    _uiState.update { state ->
                        state.copy(
                            notificationsEnabled = settings.notificationsEnabled,
                            isDarkMode = settings.theme == "dark",
                            lastSyncText = lastSyncStr,
                            isFirebaseConnected = true
                        )
                    }

                    if (oldNotificationsEnabled != settings.notificationsEnabled) {
                        try {
                            if (settings.notificationsEnabled) {
                                NotificationScheduler.rescheduleAll(
                                    context = NoroRoutineApp.instance,
                                    routines = _uiState.value.routines,
                                    isGlobalEnabled = true
                                )
                            } else {
                                NotificationScheduler.cancelAll(NoroRoutineApp.instance)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("RutinViewModel", "Error applying settings notification changes", e)
                        }
                    }
                }
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(currentTab = tabIndex) }
    }

    fun fetchWeather(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isWeatherLoading = true) }
            val location = WeatherService.getLastKnownLocation(context)
            val lat = location?.first ?: 41.0082 // Default Istanbul
            val lon = location?.second ?: 28.9784
            val weather = WeatherService.fetchWeather(lat, lon)
            _uiState.update {
                it.copy(
                    weatherInfo = weather,
                    isWeatherLoading = false
                )
            }
        }
    }

    fun addCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank() && !_uiState.value.categories.contains(trimmed)) {
            _uiState.update { state ->
                state.copy(categories = state.categories + trimmed)
            }
            repository.saveCategory(trimmed)
        }
    }

    fun addRoutine(
        name: String,
        time: String,
        isRange: Boolean = false,
        startTime: String = "08:00",
        endTime: String? = null,
        days: Set<Int>,
        category: String,
        illustrationId: String = "meditasyon_calm"
    ) {
        val daysList = if (days.isEmpty()) {
            listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
        } else {
            days.sorted().mapNotNull { DAY_INT_TO_STRING[it] }
        }

        val newRoutine = Routine(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            category = category,
            imageId = illustrationId,
            startTime = startTime,
            endTime = if (isRange) endTime else null,
            isSingleTime = !isRange,
            days = daysList,
            notificationEnabled = true,
            createdAt = Timestamp.now(),
            orderIndex = _uiState.value.routines.size
        )

        // Optimistic UI update
        _uiState.update { state ->
            state.copy(routines = state.routines + newRoutine)
        }

        // Persist immediately to Firestore
        repository.saveRoutine(newRoutine)

        // Immediately schedule its notifications
        try {
            NotificationScheduler.scheduleRoutine(
                context = NoroRoutineApp.instance,
                routine = newRoutine,
                isGlobalEnabled = _uiState.value.notificationsEnabled
            )
            NotificationScheduler.saveCache(
                context = NoroRoutineApp.instance,
                routines = _uiState.value.routines,
                isGlobalEnabled = _uiState.value.notificationsEnabled
            )
        } catch (e: Exception) {
            android.util.Log.e("RutinViewModel", "Error scheduling added routine", e)
        }
    }

    fun updateRoutine(
        id: String,
        name: String,
        time: String,
        isRange: Boolean = false,
        startTime: String = "08:00",
        endTime: String? = null,
        days: Set<Int>,
        category: String,
        illustrationId: String = "meditasyon_calm"
    ) {
        val daysList = if (days.isEmpty()) {
            listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
        } else {
            days.sorted().mapNotNull { DAY_INT_TO_STRING[it] }
        }

        val existing = _uiState.value.routines.find { it.id == id }
        val updated = (existing ?: Routine(id = id)).copy(
            name = name.trim(),
            category = category,
            imageId = illustrationId,
            startTime = startTime,
            endTime = if (isRange) endTime else null,
            isSingleTime = !isRange,
            days = daysList
        )

        _uiState.update { state ->
            val updatedList = state.routines.map { if (it.id == id) updated else it }
            state.copy(routines = updatedList)
        }

        // Persist immediately to Firestore
        repository.saveRoutine(updated)

        // Immediately schedule updated notifications
        try {
            NotificationScheduler.scheduleRoutine(
                context = NoroRoutineApp.instance,
                routine = updated,
                isGlobalEnabled = _uiState.value.notificationsEnabled
            )
            NotificationScheduler.saveCache(
                context = NoroRoutineApp.instance,
                routines = _uiState.value.routines,
                isGlobalEnabled = _uiState.value.notificationsEnabled
            )
        } catch (e: Exception) {
            android.util.Log.e("RutinViewModel", "Error scheduling updated routine", e)
        }
    }

    fun deleteRoutine(id: String) {
        _uiState.update { state ->
            val updated = state.routines.filter { it.id != id }
            state.copy(routines = updated)
        }
        repository.deleteRoutine(id)

        // Immediately cancel its notifications
        try {
            NotificationScheduler.cancelRoutine(NoroRoutineApp.instance, id)
            NotificationScheduler.saveCache(
                context = NoroRoutineApp.instance,
                routines = _uiState.value.routines,
                isGlobalEnabled = _uiState.value.notificationsEnabled
            )
        } catch (e: Exception) {
            android.util.Log.e("RutinViewModel", "Error cancelling deleted routine", e)
        }
    }

    fun moveRoutineUp(index: Int) {
        if (index <= 0) return
        val list = _uiState.value.routines.toMutableList()
        val temp = list[index]
        list[index] = list[index - 1]
        list[index - 1] = temp

        val updatedList = list.mapIndexed { idx, routine -> routine.copy(orderIndex = idx) }
        _uiState.update { state -> state.copy(routines = updatedList) }

        // Persist updated orders
        repository.saveRoutine(updatedList[index - 1])
        repository.saveRoutine(updatedList[index])
    }

    fun moveRoutineDown(index: Int) {
        val size = _uiState.value.routines.size
        if (index >= size - 1) return
        val list = _uiState.value.routines.toMutableList()
        val temp = list[index]
        list[index] = list[index + 1]
        list[index + 1] = temp

        val updatedList = list.mapIndexed { idx, routine -> routine.copy(orderIndex = idx) }
        _uiState.update { state -> state.copy(routines = updatedList) }

        // Persist updated orders
        repository.saveRoutine(updatedList[index])
        repository.saveRoutine(updatedList[index + 1])
    }

    fun toggleRoutineStatus(routineId: String, date: LocalDate = LocalDate.now()) {
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val key = "${dateString}_${routineId}"
        val currentLog = _uiState.value.logs[key]

        // Status flow: None/Upcoming -> DONE -> MISSED -> DONE
        val newIsDone = if (currentLog == null) {
            true // first tap marks done
        } else {
            !currentLog.isDone // toggle between done and missed
        }

        val updatedLog = RoutineLog(
            id = key,
            routineId = routineId,
            date = dateString,
            status = if (newIsDone) "done" else "missed",
            note = currentLog?.note.orEmpty(),
            completedAt = Timestamp.now()
        )

        _uiState.update { state ->
            val updatedLogs = state.logs + (key to updatedLog)
            val shouldShake = !newIsDone
            state.copy(
                logs = updatedLogs,
                shakeRoutineId = if (shouldShake) routineId else null,
                shakeTrigger = if (shouldShake) state.shakeTrigger + 1 else state.shakeTrigger
            )
        }

        // Persist immediately to Firestore
        repository.saveLog(updatedLog)
    }

    fun updateRoutineNote(routineId: String, date: LocalDate, note: String) {
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val key = "${dateString}_${routineId}"
        val currentLog = _uiState.value.logs[key] ?: RoutineLog(
            id = key,
            routineId = routineId,
            date = dateString,
            status = "done",
            note = note,
            completedAt = Timestamp.now()
        )

        val updatedLog = currentLog.copy(note = note, completedAt = Timestamp.now())

        _uiState.update { state ->
            state.copy(logs = state.logs + (key to updatedLog))
        }

        // Persist immediately to Firestore
        repository.saveLog(updatedLog)
    }

    fun setSelectedHistoryDate(date: LocalDate) {
        _uiState.update { it.copy(selectedHistoryDate = date) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
        repository.saveSettings(
            SettingsData(
                notificationsEnabled = enabled,
                theme = if (_uiState.value.isDarkMode) "dark" else "light",
                lastSync = Timestamp.now()
            )
        )
        try {
            if (enabled) {
                NotificationScheduler.rescheduleAll(
                    context = NoroRoutineApp.instance,
                    routines = _uiState.value.routines,
                    isGlobalEnabled = true
                )
            } else {
                NotificationScheduler.cancelAll(NoroRoutineApp.instance)
                NotificationScheduler.saveCache(
                    context = NoroRoutineApp.instance,
                    routines = _uiState.value.routines,
                    isGlobalEnabled = false
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("RutinViewModel", "Error toggling notifications", e)
        }
    }

    fun setReminderMinutes(minutes: Int) {
        _uiState.update { it.copy(reminderMinutes = minutes) }
    }

    fun setDarkMode(enabled: Boolean) {
        _uiState.update { it.copy(isDarkMode = enabled) }
        repository.saveSettings(
            SettingsData(
                notificationsEnabled = _uiState.value.notificationsEnabled,
                theme = if (enabled) "dark" else "light",
                lastSync = Timestamp.now()
            )
        )
    }

    fun resetAllData() {
        _uiState.update {
            RutinUiState(
                routines = emptyList(),
                logs = emptyMap(),
                currentTab = 0
            )
        }
        try {
            NotificationScheduler.cancelAll(NoroRoutineApp.instance)
            NotificationScheduler.saveCache(NoroRoutineApp.instance, emptyList(), false)
        } catch (e: Exception) {
            android.util.Log.e("RutinViewModel", "Error clearing notifications on reset", e)
        }
        repository.resetAllData()
    }

    fun getRoutineStatus(routine: Routine, date: LocalDate = LocalDate.now()): RoutineStatus {
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val key = "${dateString}_${routine.id}"
        val log = _uiState.value.logs[key]

        if (log != null) {
            return if (log.isDone) RoutineStatus.DONE else RoutineStatus.MISSED
        }

        // If today, check time slot
        if (date == LocalDate.now()) {
            val now = LocalTime.now()

            if (routine.isRange && !routine.endTime.isNullOrBlank()) {
                val start = try { LocalTime.parse(routine.startTime) } catch (e: Exception) { LocalTime.of(8, 0) }
                val end = try { LocalTime.parse(routine.endTime) } catch (e: Exception) { start.plusHours(1) }

                return when {
                    !now.isBefore(start) && !now.isAfter(end) -> RoutineStatus.ACTIVE
                    now.isAfter(end) -> RoutineStatus.MISSED
                    else -> RoutineStatus.UPCOMING
                }
            } else {
                val routineTime = try {
                    LocalTime.parse(routine.startTime.ifEmpty { routine.time })
                } catch (e: Exception) {
                    LocalTime.of(8, 0)
                }

                val diffMinutes = java.time.Duration.between(routineTime, now).toMinutes()
                return when {
                    diffMinutes in -15..30 -> RoutineStatus.ACTIVE
                    diffMinutes > 30 -> RoutineStatus.MISSED
                    else -> RoutineStatus.UPCOMING
                }
            }
        }

        return if (date.isBefore(LocalDate.now())) RoutineStatus.MISSED else RoutineStatus.UPCOMING
    }

    fun getRoutinesForDay(date: LocalDate): List<Routine> {
        val dayOfWeekInt = date.dayOfWeek.value // 1 = Monday, 7 = Sunday
        return _uiState.value.routines.filter { it.daysIntSet.contains(dayOfWeekInt) }
    }
}
