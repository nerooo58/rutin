package com.nororoutine.batu.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nororoutine.batu.NoroRoutineApp
import com.nororoutine.batu.data.FirestoreRepository
import com.nororoutine.batu.data.NoroContextSync
import com.nororoutine.batu.model.DAY_INT_TO_STRING
import com.nororoutine.batu.model.Routine
import com.nororoutine.batu.model.RoutineLog
import com.nororoutine.batu.model.RoutineStatus
import com.nororoutine.batu.model.SettingsData
import com.nororoutine.batu.model.UserStats
import com.nororoutine.batu.network.WeatherInfo
import com.nororoutine.batu.network.WeatherService
import com.nororoutine.batu.notification.NotificationScheduler
import com.nororoutine.batu.widget.WidgetDataManager
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.UUID

data class RutinUiState(
    val routines: List<Routine> = emptyList(),
    val logs: Map<String, RoutineLog> = emptyMap(), // key: "${dateString}_${routineId}"
    val categories: List<String> = listOf("Spor", "Zihin", "Beslenme"),
    val stats: UserStats = UserStats(),
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
    val shakeTrigger: Int = 0,
    val showTabletWeather: Boolean = true,
    val showTabletScreenTime: Boolean = true,
    val showTabletMusicCard: Boolean = true
)

class RutinViewModel(
    private val repository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(RutinUiState())
    val uiState: StateFlow<RutinUiState> = _uiState.asStateFlow()

    init {
        // Load tablet customization preferences
        try {
            val tabletPrefs = NoroRoutineApp.instance.getSharedPreferences("tablet_customization_prefs", Context.MODE_PRIVATE)
            val showWeather = tabletPrefs.getBoolean("show_tablet_weather", true)
            val showScreenTime = tabletPrefs.getBoolean("show_tablet_screen_time", true)
            val showMusicCard = tabletPrefs.getBoolean("show_tablet_music_card", true)
            _uiState.update {
                it.copy(
                    showTabletWeather = showWeather,
                    showTabletScreenTime = showScreenTime,
                    showTabletMusicCard = showMusicCard
                )
            }
        } catch (_: Exception) {}

        // Start real-time Firestore listeners on launch
        observeFirestoreData()
    }

    private fun observeFirestoreData() {
        // Real-time synchronization for Routines
        viewModelScope.launch {
            repository.observeRoutines().collect { firestoreRoutines ->
                val sortedRoutines = firestoreRoutines.sortedWith(compareBy({ it.timeToMinutes }, { it.orderIndex }))
                _uiState.update { state ->
                    state.copy(
                        routines = sortedRoutines,
                        isFirebaseConnected = true
                    )
                }
                recalculateAndSaveStats(sortedRoutines, _uiState.value.logs)

                // Update home screen widgets with fresh routines
                try {
                    WidgetDataManager.updateWidgetCache(
                        context = NoroRoutineApp.instance,
                        routines = sortedRoutines,
                        logs = _uiState.value.logs
                    )
                } catch (e: Exception) {
                    android.util.Log.e("RutinViewModel", "Error syncing widget routines cache", e)
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
                recalculateAndSaveStats(_uiState.value.routines, logsMap)

                // Update home screen widgets with fresh logs
                try {
                    WidgetDataManager.updateWidgetCache(
                        context = NoroRoutineApp.instance,
                        routines = _uiState.value.routines,
                        logs = logsMap
                    )
                } catch (e: Exception) {
                    android.util.Log.e("RutinViewModel", "Error syncing widget logs cache", e)
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
                            reminderMinutes = settings.reminderMinutes,
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

        // Real-time synchronization for Stats
        viewModelScope.launch {
            repository.observeStats().collect { firestoreStats ->
                if (firestoreStats != null) {
                    _uiState.update { state ->
                        state.copy(stats = firestoreStats)
                    }
                }
            }
        }
    }

    private fun recalculateAndSaveStats(
        routines: List<Routine>,
        logs: Map<String, RoutineLog>
    ) {
        val newStats = calculateStats(routines, logs)
        _uiState.update { it.copy(stats = newStats) }
        repository.saveStats(newStats)

        // Automatically publish AI-ready routine & focus context for Noro AI Assistant
        try {
            NoroContextSync.sync(
                routines = routines,
                logs = logs,
                stats = newStats,
                today = LocalDate.now()
            )
        } catch (e: Exception) {
            android.util.Log.e("RutinViewModel", "Error syncing Noro AI context", e)
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(currentTab = tabIndex) }
    }

    fun fetchWeather(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isWeatherLoading = true) }
            val location = WeatherService.getLastKnownLocation(context)
            val isLocationAvailable = location != null

            // If location is available, use user's GPS coordinates; otherwise default to İzmir
            val lat = location?.first ?: WeatherService.IZMIR_LAT
            val lon = location?.second ?: WeatherService.IZMIR_LON

            val weather = WeatherService.fetchWeather(lat, lon)

            val (district, city) = if (isLocationAvailable) {
                val geocoded = WeatherService.reverseGeocodeLocation(context, lat, lon)
                Pair(
                    geocoded.first.ifEmpty { "" },
                    geocoded.second.ifEmpty { "Konumunuz" }
                )
            } else {
                Pair(WeatherService.IZMIR_DISTRICT, WeatherService.IZMIR_CITY)
            }

            val updatedWeather = weather.copy(
                district = district,
                city = city
            )
            _uiState.update {
                it.copy(
                    weatherInfo = updatedWeather,
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

        val updatedList = _uiState.value.routines + newRoutine
        _uiState.update { state -> state.copy(routines = updatedList) }

        // Persist immediately to Firestore
        repository.saveRoutine(newRoutine)
        recalculateAndSaveStats(updatedList, _uiState.value.logs)

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

        val updatedList = _uiState.value.routines.map { if (it.id == id) updated else it }
        _uiState.update { state -> state.copy(routines = updatedList) }

        // Persist immediately to Firestore
        repository.saveRoutine(updated)
        recalculateAndSaveStats(updatedList, _uiState.value.logs)

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
        val updatedList = _uiState.value.routines.filter { it.id != id }
        _uiState.update { state -> state.copy(routines = updatedList) }
        repository.deleteRoutine(id)
        recalculateAndSaveStats(updatedList, _uiState.value.logs)

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

    fun markRoutineDone(routineId: String, date: LocalDate = LocalDate.now()) {
        if (date.isAfter(LocalDate.now())) return // Future days are locked
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val key = "${dateString}_${routineId}"
        val currentLog = _uiState.value.logs[key]

        val updatedLog = RoutineLog(
            id = key,
            routineId = routineId,
            date = dateString,
            status = "done",
            note = currentLog?.note.orEmpty(),
            completedAt = Timestamp.now()
        )

        val updatedLogs = _uiState.value.logs + (key to updatedLog)
        _uiState.update { state -> state.copy(logs = updatedLogs) }

        // Persist immediately to Firestore
        repository.saveLog(updatedLog)
        recalculateAndSaveStats(_uiState.value.routines, updatedLogs)

        if (date == LocalDate.now()) {
            try {
                WidgetDataManager.updateWidgetCache(
                    context = NoroRoutineApp.instance,
                    routines = _uiState.value.routines,
                    logs = updatedLogs
                )
            } catch (e: Exception) {
                android.util.Log.e("RutinViewModel", "Error updating widget cache on markDone", e)
            }
        }
    }

    fun markRoutineEmpty(routineId: String, date: LocalDate = LocalDate.now()) {
        if (date.isAfter(LocalDate.now())) return // Future days are locked
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val key = "${dateString}_${routineId}"
        val updatedLogs = _uiState.value.logs - key
        _uiState.update { state -> state.copy(logs = updatedLogs) }

        // Delete from Firestore
        repository.deleteLog(key)
        recalculateAndSaveStats(_uiState.value.routines, updatedLogs)

        if (date == LocalDate.now()) {
            try {
                WidgetDataManager.updateWidgetCache(
                    context = NoroRoutineApp.instance,
                    routines = _uiState.value.routines,
                    logs = updatedLogs
                )
            } catch (e: Exception) {
                android.util.Log.e("RutinViewModel", "Error updating widget cache on markEmpty", e)
            }
        }
    }

    fun toggleRoutineStatus(routineId: String, date: LocalDate = LocalDate.now()) {
        if (date.isAfter(LocalDate.now())) return // Future days are locked
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val key = "${dateString}_${routineId}"
        val currentLog = _uiState.value.logs[key]

        if (currentLog?.isDone == true) {
            markRoutineEmpty(routineId, date)
        } else {
            markRoutineDone(routineId, date)
        }
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
        val updatedLogs = _uiState.value.logs + (key to updatedLog)

        _uiState.update { state ->
            state.copy(logs = updatedLogs)
        }

        // Persist immediately to Firestore
        repository.saveLog(updatedLog)
        recalculateAndSaveStats(_uiState.value.routines, updatedLogs)
    }

    fun addSubtask(routineId: String, subtaskText: String) {
        val trimmed = subtaskText.trim()
        if (trimmed.isBlank()) return
        val routine = _uiState.value.routines.find { it.id == routineId } ?: return
        if (routine.subtasks.contains(trimmed)) return

        val updatedRoutine = routine.copy(subtasks = routine.subtasks + trimmed)
        val updatedList = _uiState.value.routines.map { if (it.id == routineId) updatedRoutine else it }
        _uiState.update { it.copy(routines = updatedList) }
        repository.saveRoutine(updatedRoutine)
    }

    fun removeSubtask(routineId: String, subtaskIndex: Int) {
        val routine = _uiState.value.routines.find { it.id == routineId } ?: return
        if (subtaskIndex !in routine.subtasks.indices) return

        val updatedSubtasks = routine.subtasks.toMutableList().apply { removeAt(subtaskIndex) }
        val updatedRoutine = routine.copy(subtasks = updatedSubtasks)
        val updatedList = _uiState.value.routines.map { if (it.id == routineId) updatedRoutine else it }
        _uiState.update { it.copy(routines = updatedList) }
        repository.saveRoutine(updatedRoutine)
    }

    fun toggleSubtaskDone(routineId: String, subtaskText: String, date: LocalDate = LocalDate.now()) {
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val key = "${dateString}_${routineId}"
        val currentLog = _uiState.value.logs[key] ?: RoutineLog(
            id = key,
            routineId = routineId,
            date = dateString,
            status = "empty",
            completedAt = null
        )

        val completedSet = currentLog.completedSubtasks.toMutableSet()
        if (completedSet.contains(subtaskText)) {
            completedSet.remove(subtaskText)
        } else {
            completedSet.add(subtaskText)
        }

        val updatedLog = currentLog.copy(
            completedSubtasks = completedSet.toList()
        )
        val updatedLogs = _uiState.value.logs + (key to updatedLog)
        _uiState.update { it.copy(logs = updatedLogs) }
        repository.saveLog(updatedLog)
    }

    fun saveFocusSession(
        routineId: String,
        durationSeconds: Int,
        markRoutineComplete: Boolean,
        sessionNote: String = "",
        date: LocalDate = LocalDate.now()
    ) {
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val key = "${dateString}_${routineId}"
        val currentLog = _uiState.value.logs[key] ?: RoutineLog(
            id = key,
            routineId = routineId,
            date = dateString,
            status = if (markRoutineComplete) "done" else "empty",
            completedAt = if (markRoutineComplete) Timestamp.now() else null
        )

        val newTotalDuration = currentLog.focusDurationSeconds + durationSeconds
        val nowTimeStr = try {
            LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) {
            "12:00"
        }
        val formattedSessionEntry = if (sessionNote.isNotBlank()) {
            "$nowTimeStr|$durationSeconds|$sessionNote"
        } else {
            "$nowTimeStr|$durationSeconds|Odak Seansı"
        }
        val updatedSessionNotes = currentLog.focusSessionNotes + formattedSessionEntry

        val updatedLog = currentLog.copy(
            status = if (markRoutineComplete) "done" else currentLog.status,
            completedAt = if (markRoutineComplete) (currentLog.completedAt ?: Timestamp.now()) else currentLog.completedAt,
            focusDurationSeconds = newTotalDuration,
            focusSessionNotes = updatedSessionNotes
        )
        val updatedLogs = _uiState.value.logs + (key to updatedLog)
        _uiState.update { it.copy(logs = updatedLogs) }
        repository.saveLog(updatedLog)
        recalculateAndSaveStats(_uiState.value.routines, updatedLogs)

        if (date == LocalDate.now()) {
            try {
                WidgetDataManager.updateWidgetCache(
                    context = NoroRoutineApp.instance,
                    routines = _uiState.value.routines,
                    logs = updatedLogs
                )
            } catch (e: Exception) {
                android.util.Log.e("RutinViewModel", "Error updating widget cache on focus session", e)
            }
        }
    }

    fun setSelectedHistoryDate(date: LocalDate) {
        _uiState.update { it.copy(selectedHistoryDate = date) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
        repository.saveSettings(
            SettingsData(
                notificationsEnabled = enabled,
                reminderMinutes = _uiState.value.reminderMinutes,
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
        repository.saveSettings(
            SettingsData(
                notificationsEnabled = _uiState.value.notificationsEnabled,
                reminderMinutes = minutes,
                theme = if (_uiState.value.isDarkMode) "dark" else "light",
                lastSync = Timestamp.now()
            )
        )
    }

    fun setDarkMode(enabled: Boolean) {
        _uiState.update { it.copy(isDarkMode = enabled) }
        repository.saveSettings(
            SettingsData(
                notificationsEnabled = _uiState.value.notificationsEnabled,
                reminderMinutes = _uiState.value.reminderMinutes,
                theme = if (enabled) "dark" else "light",
                lastSync = Timestamp.now()
            )
        )
    }

    fun setTabletWeatherEnabled(enabled: Boolean) {
        _uiState.update { it.copy(showTabletWeather = enabled) }
        try {
            NoroRoutineApp.instance.getSharedPreferences("tablet_customization_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("show_tablet_weather", enabled).apply()
        } catch (_: Exception) {}
    }

    fun setTabletScreenTimeEnabled(enabled: Boolean) {
        _uiState.update { it.copy(showTabletScreenTime = enabled) }
        try {
            NoroRoutineApp.instance.getSharedPreferences("tablet_customization_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("show_tablet_screen_time", enabled).apply()
        } catch (_: Exception) {}
    }

    fun setTabletMusicCardEnabled(enabled: Boolean) {
        _uiState.update { it.copy(showTabletMusicCard = enabled) }
        try {
            NoroRoutineApp.instance.getSharedPreferences("tablet_customization_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("show_tablet_music_card", enabled).apply()
        } catch (_: Exception) {}
    }

    fun resetAllData() {
        _uiState.update {
            RutinUiState(
                routines = emptyList(),
                logs = emptyMap(),
                stats = UserStats(),
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

        if (log != null && log.isDone) {
            return RoutineStatus.DONE
        }

        // If today, check time slot
        if (date == LocalDate.now()) {
            val now = LocalTime.now()

            if (routine.isRange && !routine.endTime.isNullOrBlank()) {
                val start = try { LocalTime.parse(routine.startTime) } catch (e: Exception) { LocalTime.of(8, 0) }
                val end = try { LocalTime.parse(routine.endTime) } catch (e: Exception) { start.plusHours(1) }

                return when {
                    !now.isBefore(start) && !now.isAfter(end) -> RoutineStatus.ACTIVE
                    now.isBefore(start) -> RoutineStatus.UPCOMING
                    else -> RoutineStatus.EMPTY
                }
            } else {
                val routineTime = try {
                    LocalTime.parse(routine.startTime.ifEmpty { routine.time })
                } catch (e: Exception) {
                    LocalTime.of(8, 0)
                }

                val diffMinutes = java.time.Duration.between(routineTime, now).toMinutes()
                return when {
                    diffMinutes in 0..60 -> RoutineStatus.ACTIVE
                    routineTime.isAfter(now) -> RoutineStatus.UPCOMING
                    else -> RoutineStatus.EMPTY
                }
            }
        }

        return RoutineStatus.EMPTY
    }

    fun getRoutinesForDay(date: LocalDate): List<Routine> {
        val dayOfWeekInt = date.dayOfWeek.value // 1 = Monday, 7 = Sunday
        return _uiState.value.routines
            .filter { it.daysIntSet.contains(dayOfWeekInt) }
            .sortedWith(compareBy({ it.timeToMinutes }, { it.orderIndex }))
    }

    /**
     * Calculates dynamic statistics:
     * - Today's completed and total count (e.g. 4/4) and percentage
     * - Current active streak and best streak
     * - Weekly completion count and percentage
     * - Monthly completion count and percentage
     * - Yearly completion count and percentage
     */
    fun calculateStats(
        routines: List<Routine> = _uiState.value.routines,
        logs: Map<String, RoutineLog> = _uiState.value.logs,
        today: LocalDate = LocalDate.now()
    ): UserStats {
        val todayRoutines = routines.filter { it.daysIntSet.contains(today.dayOfWeek.value) }
        val todayTotal = todayRoutines.size
        val todayDateStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val todayCompleted = todayRoutines.count { routine ->
            logs["${todayDateStr}_${routine.id}"]?.isDone == true
        }
        val todayPercent = if (todayTotal > 0) ((todayCompleted * 100) / todayTotal).coerceIn(0, 100) else 0

        // Streak calculation: check consecutive days backwards
        var streak = 0
        val isTodayActive = (todayTotal > 0 && todayCompleted >= todayTotal) || (todayCompleted > 0)

        var checkDate = if (isTodayActive) {
            streak = 1
            today.minusDays(1)
        } else {
            today.minusDays(1)
        }

        var dayCount = 0
        while (dayCount < 365) {
            val pastRoutines = routines.filter { it.daysIntSet.contains(checkDate.dayOfWeek.value) }
            val pastDateStr = checkDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val pastDone = pastRoutines.count { logs["${pastDateStr}_${it.id}"]?.isDone == true }

            if (pastRoutines.isNotEmpty()) {
                if (pastDone > 0) {
                    streak++
                    checkDate = checkDate.minusDays(1)
                } else {
                    break // streak broken
                }
            } else {
                // Rest day: no routines scheduled, don't break streak
                checkDate = checkDate.minusDays(1)
            }
            dayCount++
        }

        val previousBest = _uiState.value.stats.bestStreak
        val bestStreak = maxOf(streak, previousBest)

        // Weekly calculation (Monday to today of current week)
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        var weeklyTotal = 0
        var weeklyCompleted = 0
        var wDate = startOfWeek
        while (!wDate.isAfter(today)) {
            val dRoutines = routines.filter { it.daysIntSet.contains(wDate.dayOfWeek.value) }
            val dStr = wDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            weeklyTotal += dRoutines.size
            weeklyCompleted += dRoutines.count { logs["${dStr}_${it.id}"]?.isDone == true }
            wDate = wDate.plusDays(1)
        }
        val weeklyPercent = if (weeklyTotal > 0) ((weeklyCompleted * 100) / weeklyTotal).coerceIn(0, 100) else 0

        // Monthly calculation (1st of month to today)
        val startOfMonth = today.withDayOfMonth(1)
        var monthlyTotal = 0
        var monthlyCompleted = 0
        var mDate = startOfMonth
        while (!mDate.isAfter(today)) {
            val dRoutines = routines.filter { it.daysIntSet.contains(mDate.dayOfWeek.value) }
            val dStr = mDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            monthlyTotal += dRoutines.size
            monthlyCompleted += dRoutines.count { logs["${dStr}_${it.id}"]?.isDone == true }
            mDate = mDate.plusDays(1)
        }
        val monthlyPercent = if (monthlyTotal > 0) ((monthlyCompleted * 100) / monthlyTotal).coerceIn(0, 100) else 0

        // Yearly calculation (Jan 1 of year to today)
        val startOfYear = today.withDayOfYear(1)
        var yearlyTotal = 0
        var yearlyCompleted = 0
        var yDate = startOfYear
        while (!yDate.isAfter(today)) {
            val dRoutines = routines.filter { it.daysIntSet.contains(yDate.dayOfWeek.value) }
            val dStr = yDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            yearlyTotal += dRoutines.size
            yearlyCompleted += dRoutines.count { logs["${dStr}_${it.id}"]?.isDone == true }
            yDate = yDate.plusDays(1)
        }
        val yearlyPercent = if (yearlyTotal > 0) ((yearlyCompleted * 100) / yearlyTotal).coerceIn(0, 100) else 0

        return UserStats(
            todayCompleted = todayCompleted,
            todayTotal = todayTotal,
            todayPercent = todayPercent,
            currentStreak = streak,
            bestStreak = bestStreak,
            weeklyCompleted = weeklyCompleted,
            weeklyTotal = weeklyTotal,
            weeklyPercent = weeklyPercent,
            monthlyCompleted = monthlyCompleted,
            monthlyTotal = monthlyTotal,
            monthlyPercent = monthlyPercent,
            yearlyCompleted = yearlyCompleted,
            yearlyTotal = yearlyTotal,
            yearlyPercent = yearlyPercent,
            lastUpdated = Timestamp.now()
        )
    }
}
