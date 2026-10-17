package com.nororoutine.batu.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.animation.Crossfade
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.nororoutine.batu.model.Routine
import com.nororoutine.batu.model.RoutineStatus
import com.nororoutine.batu.ui.components.FocusModeBottomSheet
import com.nororoutine.batu.ui.components.PulsingDot
import com.nororoutine.batu.ui.components.RoutineDetailBottomSheet
import com.nororoutine.batu.ui.components.RoutineInfoMenuBottomSheet
import com.nororoutine.batu.ui.components.MusicPlayerCard
import com.nororoutine.batu.ui.components.ScreenTimeDetailSheet
import com.nororoutine.batu.ui.components.ScreenTimeSummaryCard
import com.nororoutine.batu.ui.components.StopwatchBottomSheet
import com.nororoutine.batu.ui.components.StoryPeriod
import com.nororoutine.batu.ui.components.StoryShareBottomSheet
import com.nororoutine.batu.ui.components.SubtasksBottomSheet
import com.nororoutine.batu.ui.components.TodayRoutinesBottomSheet
import com.nororoutine.batu.ui.components.shake
import com.nororoutine.batu.ui.components.springPress
import com.nororoutine.batu.ui.illustrations.RoutineIllustration
import com.nororoutine.batu.ui.theme.AccentPurple
import com.nororoutine.batu.ui.theme.AccentTeal
import com.nororoutine.batu.ui.theme.DoneGreen
import com.nororoutine.batu.ui.theme.GlassCardBorderSubtle
import com.nororoutine.batu.ui.theme.MissedRed
import com.nororoutine.batu.ui.theme.MutedWhite
import com.nororoutine.batu.ui.theme.PureWhite
import com.nororoutine.batu.ui.theme.SubtitleWhite
import com.nororoutine.batu.viewmodel.RutinUiState
import com.nororoutine.batu.viewmodel.RutinViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * Three distinct UI states for TodayScreen:
 * STATE 1 - IDLE: No active or approaching routine.
 * STATE 2 - APPROACHING: 15 min before a routine starts. Header shrinks to compact top bar. Routine card centered with countdown.
 * STATE 3 - ACTIVE: Routine time has started, or inside a time range. Elapsed/remaining time. If another approaches, banner slides up.
 */
enum class MainViewState {
    IDLE,
    APPROACHING,
    ACTIVE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: RutinViewModel,
    uiState: RutinUiState,
    onNavigateToEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val turkishLocale = remember { Locale("tr", "TR") }

    // Real-time ticking clock for exact countdown updates
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    val nowLocalTime = remember(currentTimeMillis) { LocalTime.now() }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.fetchWeather(context)
    }

    // Check location permission & fetch weather on launch
    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            viewModel.fetchWeather(context)
        }
    }

    val todayRoutines = viewModel.getRoutinesForDay(today)
    val totalCount = todayRoutines.size
    val todayDateStr = remember(today) { today.format(DateTimeFormatter.ISO_LOCAL_DATE) }
    val completedCount = todayRoutines.count { routine ->
        uiState.logs["${todayDateStr}_${routine.id}"]?.isDone == true
    }
    val todayPercent = if (totalCount > 0) ((completedCount * 100) / totalCount).coerceIn(0, 100) else 0
    val currentStreak = uiState.stats.currentStreak

    // Active routine (currently in scheduled interval)
    val activeRoutine = todayRoutines.find { routine ->
        val start = try { LocalTime.parse(routine.startTime) } catch (e: Exception) { LocalTime.of(8, 0) }
        val end = try {
            if (routine.isRange && !routine.endTime.isNullOrBlank()) LocalTime.parse(routine.endTime)
            else start.plusHours(1)
        } catch (e: Exception) { start.plusHours(1) }
        !nowLocalTime.isBefore(start) && !nowLocalTime.isAfter(end)
    }

    // Approaching or next upcoming routine scheduled today
    val approachingOrUpcomingRoutine = todayRoutines.find { routine ->
        val logKey = "${todayDateStr}_${routine.id}"
        val isDone = uiState.logs[logKey]?.isDone == true
        if (isDone) return@find false

        val start = try {
            LocalTime.parse(routine.startTime.ifEmpty { routine.time })
        } catch (e: Exception) {
            LocalTime.of(8, 0)
        }
        start.isAfter(nowLocalTime)
    }

    // Countdown string for approaching or upcoming routine
    val upcomingCountdownText = remember(approachingOrUpcomingRoutine, nowLocalTime) {
        if (approachingOrUpcomingRoutine == null) null
        else {
            val start = try {
                LocalTime.parse(approachingOrUpcomingRoutine.startTime.ifEmpty { approachingOrUpcomingRoutine.time })
            } catch (e: Exception) {
                LocalTime.of(8, 0)
            }
            val diffSec = Duration.between(nowLocalTime, start).seconds.coerceAtLeast(0)
            val mm = diffSec / 60
            val ss = diffSec % 60
            String.format(Locale.US, "%02d:%02d", mm, ss)
        }
    }

    val mainViewState = remember(activeRoutine, approachingOrUpcomingRoutine) {
        when {
            activeRoutine != null -> MainViewState.ACTIVE
            approachingOrUpcomingRoutine != null -> MainViewState.APPROACHING
            else -> MainViewState.IDLE
        }
    }

    // Initial focused routine index:
    // 1. If currently inside active routine's interval, center that routine!
    // 2. Else first unmarked routine of today ("bir rutini hic isaretlemediysek")!
    // 3. Else 0
    val initialFocusedIndex = remember(todayRoutines) {
        if (todayRoutines.isEmpty()) 0
        else {
            val now = LocalTime.now()
            val activeIdx = todayRoutines.indexOfFirst { r ->
                val start = try { LocalTime.parse(r.startTime) } catch (e: Exception) { LocalTime.of(8, 0) }
                val end = try {
                    if (r.isRange && !r.endTime.isNullOrBlank()) LocalTime.parse(r.endTime)
                    else start.plusHours(1)
                } catch (e: Exception) { start.plusHours(1) }
                !now.isBefore(start) && !now.isAfter(end)
            }
            if (activeIdx >= 0) activeIdx
            else {
                val unmarkedIdx = todayRoutines.indexOfFirst { r ->
                    uiState.logs["${todayDateStr}_${r.id}"]?.isDone != true
                }
                if (unmarkedIdx >= 0) unmarkedIdx else 0
            }
        }
    }

    var focusedIndex by remember(todayRoutines.size) {
        mutableIntStateOf(initialFocusedIndex)
    }

    val safeFocusedIndex = if (todayRoutines.isNotEmpty()) focusedIndex.coerceIn(0, todayRoutines.lastIndex) else 0

    // Previous routine: routine scheduled before safeFocusedIndex or before now
    val previousRoutine = remember(safeFocusedIndex, todayRoutines) {
        if (safeFocusedIndex > 0) todayRoutines.getOrNull(safeFocusedIndex - 1)
        else todayRoutines.lastOrNull { r ->
            val start = try { LocalTime.parse(r.startTime) } catch (e: Exception) { LocalTime.of(8, 0) }
            start.isBefore(nowLocalTime)
        }
    }

    var selectedRoutineForSheet by remember { mutableStateOf<Routine?>(null) }
    var showTodayRoutinesSheet by remember { mutableStateOf(false) }
    var infoMenuRoutine by remember { mutableStateOf<Routine?>(null) }
    var fullScreenFocusRoutine by remember { mutableStateOf<Routine?>(null) }
    var subtasksRoutine by remember { mutableStateOf<Routine?>(null) }
    var showStoryShareSheet by remember { mutableStateOf(false) }
    var showScreenTimeDetailSheet by remember { mutableStateOf(false) }
    var showMusicSheet by remember { mutableStateOf(false) }

    val formattedDateFull = remember(today) {
        val day = today.dayOfMonth
        val month = today.format(DateTimeFormatter.ofPattern("MMMM", turkishLocale))
        val year = today.year
        val dayOfWeek = today.format(DateTimeFormatter.ofPattern("EEEE", turkishLocale))
        "$dayOfWeek, $day $month $year"
    }

    val locationText = remember(uiState.weatherInfo.district, uiState.weatherInfo.city) {
        when {
            uiState.weatherInfo.district.isNotEmpty() && uiState.weatherInfo.city.isNotEmpty() ->
                "${uiState.weatherInfo.district}, ${uiState.weatherInfo.city}"
            uiState.weatherInfo.city.isNotEmpty() -> uiState.weatherInfo.city
            uiState.weatherInfo.district.isNotEmpty() -> uiState.weatherInfo.district
            else -> "İzmir"
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isLandscape) {
            LandscapeTodayLayout(
                mainViewState = mainViewState,
                nowTime = nowLocalTime,
                dateText = formattedDateFull,
                uiState = uiState,
                locationText = locationText,
                totalTodayRoutines = totalCount,
                completedTodayRoutines = completedCount,
                todayPercent = todayPercent,
                currentStreak = currentStreak,
                focusedIndex = safeFocusedIndex,
                onFocusChange = { focusedIndex = it },
                viewModel = viewModel,
                today = today,
                onViewRoutinesClick = { showTodayRoutinesSheet = true },
                onMarkDone = { routine -> viewModel.markRoutineDone(routine.id, today) },
                onMarkEmpty = { routine -> viewModel.markRoutineEmpty(routine.id, today) },
                onOpenNote = { routine -> selectedRoutineForSheet = routine },
                onOpenInfo = { routine -> infoMenuRoutine = routine },
                onOpenStoryShare = { showStoryShareSheet = true },
                onOpenScreenTimeDetail = { showScreenTimeDetailSheet = true }
            )
        } else {
            PortraitTodayLayout(
                nowTime = nowLocalTime,
                routines = todayRoutines,
                focusedIndex = safeFocusedIndex,
                onFocusChange = { focusedIndex = it },
                uiState = uiState,
                viewModel = viewModel,
                today = today,
                locationText = locationText,
                totalCount = totalCount,
                completedCount = completedCount,
                todayPercent = todayPercent,
                currentStreak = currentStreak,
                activeRoutine = activeRoutine,
                approachingOrUpcomingRoutine = approachingOrUpcomingRoutine,
                upcomingCountdownText = upcomingCountdownText,
                previousRoutine = previousRoutine,
                onViewRoutinesClick = { showTodayRoutinesSheet = true },
                onMarkDone = { routine -> viewModel.markRoutineDone(routine.id, today) },
                onMarkEmpty = { routine ->
                    viewModel.markRoutineEmpty(routine.id, today)
                    if (safeFocusedIndex < todayRoutines.lastIndex) {
                        focusedIndex = safeFocusedIndex + 1
                    }
                },
                onOpenNote = { routine -> selectedRoutineForSheet = routine },
                onOpenInfo = { routine -> infoMenuRoutine = routine },
                onOpenStoryShare = { showStoryShareSheet = true },
                onOpenScreenTime = { showScreenTimeDetailSheet = true },
                onOpenMusic = { showMusicSheet = true },
                onNavigateToEdit = onNavigateToEdit
            )
        }

        // Today's Routines Bottom Sheet (opened from compact pill button)
        if (showTodayRoutinesSheet) {
            TodayRoutinesBottomSheet(
                routines = todayRoutines,
                today = today,
                getStatus = { viewModel.getRoutineStatus(it, today) },
                onToggleStatus = { routine ->
                    viewModel.toggleRoutineStatus(routine.id, today)
                },
                onOpenNote = { routine ->
                    selectedRoutineForSheet = routine
                },
                onOpenInfo = { routine ->
                    infoMenuRoutine = routine
                },
                onOpenStoryShare = { showStoryShareSheet = true },
                onNavigateToEdit = onNavigateToEdit,
                onDismiss = { showTodayRoutinesSheet = false }
            )
        }

        // Note / Journal Bottom Sheet
        selectedRoutineForSheet?.let { routine ->
            val status = viewModel.getRoutineStatus(routine, today)
            val dateKey = "${today.format(DateTimeFormatter.ISO_LOCAL_DATE)}_${routine.id}"
            val currentNote = uiState.logs[dateKey]?.note.orEmpty()

            RoutineDetailBottomSheet(
                routine = routine,
                status = status,
                initialNote = currentNote,
                date = today,
                onDismiss = { selectedRoutineForSheet = null },
                onToggleStatus = {
                    viewModel.toggleRoutineStatus(routine.id, today)
                },
                onSaveNote = { note ->
                    viewModel.updateRoutineNote(routine.id, today, note)
                }
            )
        }

        // Routine Info Menu Bottom Sheet (Option 1: Odak Modu / Süre Tutma, Option 2: Alt Adımlar)
        infoMenuRoutine?.let { routine ->
            val todayDateKey = "${today.format(DateTimeFormatter.ISO_LOCAL_DATE)}_${routine.id}"
            val currentRoutine = uiState.routines.find { it.id == routine.id } ?: routine
            val currentLog = uiState.logs[todayDateKey]

            RoutineInfoMenuBottomSheet(
                routine = currentRoutine,
                todayLog = currentLog,
                onDismiss = { infoMenuRoutine = null },
                onSelectFocusMode = {
                    val r = currentRoutine
                    infoMenuRoutine = null
                    fullScreenFocusRoutine = r
                },
                onSelectSubtasks = {
                    val r = currentRoutine
                    infoMenuRoutine = null
                    subtasksRoutine = r
                }
            )
        }

        // TAM EKRAN Süre Tutma & Odak Modu (Persistent Background & Timestamp Recovery)
        fullScreenFocusRoutine?.let { routine ->
            val currentRoutine = uiState.routines.find { it.id == routine.id } ?: routine
            FullScreenFocusTimerScreen(
                routine = currentRoutine,
                today = today,
                uiState = uiState,
                viewModel = viewModel,
                onDismiss = { fullScreenFocusRoutine = null }
            )
        }

        // Subtasks Bottom Sheet (Option 2: Alt adımlar)
        subtasksRoutine?.let { routine ->
            val todayDateKey = "${today.format(DateTimeFormatter.ISO_LOCAL_DATE)}_${routine.id}"
            val currentRoutine = uiState.routines.find { it.id == routine.id } ?: routine
            val currentLog = uiState.logs[todayDateKey]

            SubtasksBottomSheet(
                routine = currentRoutine,
                todayLog = currentLog,
                onDismiss = { subtasksRoutine = null },
                onAddSubtask = { text ->
                    viewModel.addSubtask(currentRoutine.id, text)
                },
                onRemoveSubtask = { index ->
                    viewModel.removeSubtask(currentRoutine.id, index)
                },
                onToggleSubtask = { text ->
                    viewModel.toggleSubtaskDone(currentRoutine.id, text, today)
                },
                onMarkRoutineDone = {
                    viewModel.markRoutineDone(currentRoutine.id, today)
                }
            )
        }

        // Story Share Bottom Sheet (9:16 Instagram / WhatsApp achievement story card)
        if (showStoryShareSheet) {
            StoryShareBottomSheet(
                uiState = uiState,
                initialPeriod = StoryPeriod.DAILY,
                initialDate = today,
                onDismiss = { showStoryShareSheet = false }
            )
        }

        // Screen Time Detail Sheet
        if (showScreenTimeDetailSheet) {
            ScreenTimeDetailSheet(
                onDismissRequest = { showScreenTimeDetailSheet = false }
            )
        }

        // Music & Focus Audio Sheet (for phone mode)
        if (showMusicSheet) {
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { showMusicSheet = false },
                containerColor = Color(0xFF0F172A),
                scrimColor = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    MusicPlayerCard(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/**
 * PORTRAIT Layout for Phone:
 * - Top Bar: Clock, Date, Temp & Location
 * - Top Tag: Previous routine preview ("usttede onceki rutin gorunsun")
 * - Weather SVG Illustration with condition & temp
 * - Approaching routine countdown pill under weather image ("hava durumu resminin altinda yaklasan rutinin geri sayimi")
 * - Center: Large swipeable routine cards carousel with smooth spring physics
 * - Routine status: Tamamlandı or Boş Geç (never disappears when completed!)
 * - Bottom: "Günün Rutinleri" pill with completion percentage & streak
 */
@Composable
fun PortraitTodayLayout(
    nowTime: LocalTime,
    routines: List<Routine>,
    focusedIndex: Int,
    onFocusChange: (Int) -> Unit,
    uiState: RutinUiState,
    viewModel: RutinViewModel,
    today: LocalDate,
    locationText: String,
    totalCount: Int,
    completedCount: Int,
    todayPercent: Int,
    currentStreak: Int,
    activeRoutine: Routine?,
    approachingOrUpcomingRoutine: Routine?,
    upcomingCountdownText: String?,
    previousRoutine: Routine?,
    onViewRoutinesClick: () -> Unit,
    onMarkDone: (Routine) -> Unit,
    onMarkEmpty: (Routine) -> Unit,
    onOpenNote: (Routine) -> Unit,
    onOpenInfo: (Routine) -> Unit,
    onOpenStoryShare: () -> Unit,
    onOpenScreenTime: () -> Unit = {},
    onOpenMusic: () -> Unit = {},
    onNavigateToEdit: () -> Unit
) {
    val turkishLocale = remember { Locale("tr", "TR") }
    val formattedTime = remember(nowTime) { nowTime.format(DateTimeFormatter.ofPattern("HH:mm")) }
    val formattedDateShort = remember(today) {
        val d = today.dayOfMonth
        val m = today.format(DateTimeFormatter.ofPattern("MMM", turkishLocale))
        "$d $m"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Compact Top Bar: small clock, date, temp, city — all compact in one row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xF2141F33)) // Solid glass bar
                .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(999.dp))
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clock & Date
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedTime,
                        color = PureWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•",
                        color = MutedWhite,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formattedDateShort,
                        color = SubtitleWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Temp, Location & Story Share Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Thermostat,
                        contentDescription = null,
                        tint = AccentTeal,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${uiState.weatherInfo.temperature.toInt()}°C",
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = locationText.take(12),
                        color = SubtitleWhite,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Üst Bar Hikaye Paylaş Butonu (Çok belirgin gradyanlı)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))))
                            .clickable { onOpenStoryShare() }
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Başarı Hikayesi Paylaş",
                                tint = PureWhite,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Paylaş",
                                color = PureWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Previous routine preview chip at top if exists ("usttede onceki rutin gorunsun")
        if (previousRoutine != null) {
            val prevIsDone = uiState.logs["${today.format(DateTimeFormatter.ISO_LOCAL_DATE)}_${previousRoutine.id}"]?.isDone == true
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x1AFFFFFF))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.FastRewind,
                    contentDescription = null,
                    tint = SubtitleWhite,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Önceki: ${previousRoutine.name} (${previousRoutine.time})",
                    color = SubtitleWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (prevIsDone) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = DoneGreen,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }

        // Centered Weather SVG Illustration & Condition
        WeatherSvgIllustration(
            condition = uiState.weatherInfo.condition,
            timeOfDay = getCurrentTimeOfDay(),
            modifier = Modifier.size(76.dp)
        )

        Text(
            text = "${uiState.weatherInfo.condition.label} • ${uiState.weatherInfo.temperature.toInt()}°C",
            color = SubtitleWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Approaching routine countdown pill under weather image:
        // "hava durumu resminin altinda yaklasan rutinin geri sayimi"
        if (approachingOrUpcomingRoutine != null && upcomingCountdownText != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x33A78BFA))
                    .border(1.dp, AccentPurple, RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PulsingDot(
                        color = AccentPurple,
                        dotSize = 6.dp,
                        pulseMaxScale = 2.0f
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Yaklaşan: ${approachingOrUpcomingRoutine.name} (${approachingOrUpcomingRoutine.time}) — Geri sayım: $upcomingCountdownText",
                        color = PureWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else if (activeRoutine != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x3310B981))
                    .border(1.dp, DoneGreen, RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PulsingDot(
                        color = DoneGreen,
                        dotSize = 6.dp,
                        pulseMaxScale = 2.0f
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Aktif Rutin: ${activeRoutine.name} (${activeRoutine.time})",
                        color = PureWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Center: Swipeable Routine Card Carousel (never disappears!)
        if (routines.isNotEmpty()) {
            VerticalRoutineCardStack(
                routines = routines,
                focusedIndex = focusedIndex,
                nowTime = nowTime,
                mainViewState = MainViewState.ACTIVE,
                viewModel = viewModel,
                uiState = uiState,
                today = today,
                onFocusChange = onFocusChange,
                onMarkDone = onMarkDone,
                onMarkEmpty = onMarkEmpty,
                onOpenDetail = onOpenNote,
                onOpenInfo = onOpenInfo,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Bugün için kayıtlı rutin bulunmuyor.",
                        color = MutedWhite,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onNavigateToEdit,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Rutin Ekle", color = PureWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Bottom: "Günün Rutinleri" pill with completion rate and streak
        val isAllCompleted = totalCount > 0 && completedCount == totalCount
        Row(
            modifier = Modifier.padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStreak > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0x33F59E0B))
                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = "🔥 $currentStreak gün",
                        color = Color(0xFFFBBF24),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val completionColor = getCompletionColor(todayPercent, totalCount)
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (totalCount > 0) completionColor.copy(alpha = 0.22f) else Color(0xF2162035))
                    .border(1.dp, if (totalCount > 0) completionColor.copy(alpha = 0.75f) else GlassCardBorderSubtle, RoundedCornerShape(999.dp))
                    .clickable { onViewRoutinesClick() }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isAllCompleted) Icons.Rounded.Check else Icons.Rounded.ViewCarousel,
                        contentDescription = null,
                        tint = if (totalCount > 0) completionColor else AccentTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (totalCount > 0) "Günün Rutinleri: $completedCount/$totalCount (%$todayPercent)" else "Günün Rutinleri (0)",
                        color = PureWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Ekran Süresi Butonu
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x3306B6D4))
                    .border(1.dp, AccentTeal.copy(alpha = 0.7f), RoundedCornerShape(999.dp))
                    .clickable { onOpenScreenTime() }
                    .padding(horizontal = 9.dp, vertical = 9.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Smartphone,
                        contentDescription = "Ekran Süresi",
                        tint = PureWhite,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Müzik / Odak Sesi Butonu
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x3310B981))
                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.7f), RoundedCornerShape(999.dp))
                    .clickable { onOpenMusic() }
                    .padding(horizontal = 9.dp, vertical = 9.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = "Müzik",
                        tint = PureWhite,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Paylaşılabilir Hikaye Kartı Butonu
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x338B5CF6))
                    .border(1.dp, AccentPurple.copy(alpha = 0.7f), RoundedCornerShape(999.dp))
                    .clickable { onOpenStoryShare() }
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Hikaye Paylaş",
                        tint = PureWhite,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Paylaş",
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * LANDSCAPE (tablet) layout for Today screen
 */
@Composable
fun LandscapeTodayLayout(
    mainViewState: MainViewState,
    nowTime: LocalTime,
    dateText: String,
    uiState: RutinUiState,
    locationText: String,
    totalTodayRoutines: Int,
    completedTodayRoutines: Int,
    todayPercent: Int,
    currentStreak: Int,
    focusedIndex: Int,
    onFocusChange: (Int) -> Unit,
    viewModel: RutinViewModel,
    today: LocalDate,
    onViewRoutinesClick: () -> Unit,
    onMarkDone: (Routine) -> Unit,
    onMarkEmpty: (Routine) -> Unit,
    onOpenNote: (Routine) -> Unit,
    onOpenInfo: (Routine) -> Unit,
    onOpenStoryShare: () -> Unit,
    onOpenScreenTimeDetail: () -> Unit
) {
    val formattedTime = remember(nowTime) { nowTime.format(DateTimeFormatter.ofPattern("HH:mm")) }
    val todayRoutines = viewModel.getRoutinesForDay(today)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Side: Time, Date, Weather Card, Music Player & Screen Time Card
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = formattedTime,
                        color = PureWhite,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateText,
                        color = SubtitleWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Streak Badge
                if (currentStreak > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0x33F59E0B))
                            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "🔥 $currentStreak Gün Seri",
                            color = Color(0xFFFBBF24),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Weather Card with Location
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xF2131D31))
                    .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Thermostat,
                                contentDescription = null,
                                tint = AccentTeal,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${uiState.weatherInfo.temperature.toInt()}°C • ${uiState.weatherInfo.condition.label}",
                                color = PureWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = locationText,
                                color = SubtitleWhite,
                                fontSize = 11.sp
                            )
                        }
                    }

                    val completionColor = getCompletionColor(todayPercent, totalTodayRoutines)
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (totalTodayRoutines > 0) "%$todayPercent" else "-",
                            color = completionColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "$completedTodayRoutines / $totalTodayRoutines Tamamlandı",
                            color = MutedWhite,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Tablet Music Player & Focus Soundscape Card
            MusicPlayerCard(
                modifier = Modifier.fillMaxWidth()
            )

            // Tablet Screen Time Card (Tap to open detailed stats)
            ScreenTimeSummaryCard(
                onOpenDetail = { onOpenScreenTimeDetail() },
                modifier = Modifier.fillMaxWidth()
            )

            // Today's Routine List Preview (Quick Checklist for Tablet)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xE6131D31))
                    .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Günün Rutinleri",
                            color = PureWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(AccentPurple.copy(alpha = 0.3f))
                                    .clickable { onOpenStoryShare() }
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Share,
                                        contentDescription = "Hikaye Paylaş",
                                        tint = PureWhite,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Paylaş",
                                        color = PureWhite,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(AccentTeal.copy(alpha = 0.25f))
                                    .clickable { onViewRoutinesClick() }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Tümünü Gör",
                                    color = AccentTeal,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (todayRoutines.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Bugün için rutin planlanmadı.",
                                color = MutedWhite,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(todayRoutines.size, key = { todayRoutines[it].id }) { idx ->
                                val r = todayRoutines[idx]
                                val logKey = "${today.format(DateTimeFormatter.ISO_LOCAL_DATE)}_${r.id}"
                                val isDone = uiState.logs[logKey]?.isDone == true
                                val timeStr = r.startTime.ifEmpty { r.time }

                                val isSelected = idx == focusedIndex
                                val rowBg = when {
                                    isSelected -> Color(0x33A78BFA)
                                    isDone -> Color(0x2210B981)
                                    else -> Color(0x14FFFFFF)
                                }
                                val rowBorder = when {
                                    isSelected -> AccentPurple
                                    isDone -> Color(0xFF10B981).copy(alpha = 0.5f)
                                    else -> Color(0x1AFFFFFF)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(rowBg)
                                        .border(1.dp, rowBorder, RoundedCornerShape(10.dp))
                                        .clickable { onFocusChange(idx) }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(if (isDone) Color(0xFF10B981) else Color(0x33FFFFFF))
                                                .clickable {
                                                    if (isDone) onMarkEmpty(r) else onMarkDone(r)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isDone) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = Color(0xFF0D0B1E),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = r.name,
                                                color = if (isDone) MutedWhite else PureWhite,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (timeStr.isNotEmpty()) {
                                                Text(
                                                    text = timeStr,
                                                    color = SubtitleWhite,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (r.isRange) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0x337CF4DE))
                                                    .border(1.dp, AccentTeal.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "⏳ ${r.durationMinutes}dk",
                                                    color = AccentTeal,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0x22A78BFA))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = r.category,
                                                color = AccentPurple,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        IconButton(
                                            onClick = { onOpenInfo(r) },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x22FFFFFF))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Info,
                                                contentDescription = "Rutin Seçenekleri",
                                                tint = if (r.isRange) AccentTeal else AccentPurple,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Right Side: Active / Focused Routine Cards Carousel (kaydırmalı kartlar tıpkı telefondaki gibi)
        Box(
            modifier = Modifier
                .weight(1.15f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            if (todayRoutines.isNotEmpty()) {
                VerticalRoutineCardStack(
                    routines = todayRoutines,
                    focusedIndex = focusedIndex,
                    nowTime = nowTime,
                    mainViewState = mainViewState,
                    viewModel = viewModel,
                    uiState = uiState,
                    today = today,
                    onFocusChange = onFocusChange,
                    onMarkDone = onMarkDone,
                    onMarkEmpty = onMarkEmpty,
                    onOpenDetail = onOpenNote,
                    onOpenInfo = onOpenInfo,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xF2162035))
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Bugün için kayıtlı aktif rutin bulunmuyor.",
                        color = MutedWhite,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}


/**
 * Vertical Card Stack / Carousel with smooth spring drag physics and solid glass cards.
 */
@Composable
fun VerticalRoutineCardStack(
    routines: List<Routine>,
    focusedIndex: Int,
    nowTime: LocalTime,
    mainViewState: MainViewState,
    viewModel: RutinViewModel,
    uiState: RutinUiState,
    today: LocalDate,
    onFocusChange: (Int) -> Unit,
    onMarkDone: (Routine) -> Unit,
    onMarkEmpty: (Routine) -> Unit,
    onOpenDetail: (Routine) -> Unit,
    onOpenInfo: (Routine) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val dragOffsetAnim = remember { Animatable(0f) }

    val draggableState = rememberDraggableState { delta ->
        scope.launch {
            val current = dragOffsetAnim.value + delta
            val bounded = if ((focusedIndex == 0 && current > 0) || (focusedIndex == routines.lastIndex && current < 0)) {
                dragOffsetAnim.value + delta * 0.4f
            } else {
                current
            }
            dragOffsetAnim.snapTo(bounded)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .draggable(
                state = draggableState,
                orientation = Orientation.Vertical,
                onDragStopped = { velocity ->
                    scope.launch {
                        val threshold = 90f
                        val currentVal = dragOffsetAnim.value
                        val targetChange = when {
                            currentVal < -threshold || (velocity < -500f && currentVal < 0) -> 1
                            currentVal > threshold || (velocity > 500f && currentVal > 0) -> -1
                            else -> 0
                        }

                        if (targetChange != 0) {
                            val newIdx = (focusedIndex + targetChange).coerceIn(0, routines.lastIndex)
                            if (newIdx != focusedIndex) {
                                onFocusChange(newIdx)
                            }
                        }

                        // Smooth spring glide back to position naturally
                        dragOffsetAnim.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Navigation Indicators
        if (focusedIndex > 0) {
            IconButton(
                onClick = { onFocusChange(focusedIndex - 1) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x33101C30))
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Önceki Rutin",
                    tint = PureWhite
                )
            }
        }

        if (focusedIndex < routines.lastIndex) {
            IconButton(
                onClick = { onFocusChange(focusedIndex + 1) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x33101C30))
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Sonraki Rutin",
                    tint = PureWhite
                )
            }
        }

        val currentDragOffset = dragOffsetAnim.value

        routines.forEachIndexed { index, routine ->
            val delta = index - focusedIndex
            val absDelta = abs(delta)

            if (absDelta <= 2) {
                val status = viewModel.getRoutineStatus(routine, today)
                val isCenter = delta == 0
                val zIndexVal = if (isCenter) 20f else (10 - absDelta).toFloat()

                val baseOffsetY = when {
                    delta < 0 -> -96.dp * absDelta
                    delta > 0 -> 96.dp * absDelta
                    else -> 0.dp
                }

                val animatedBaseOffsetY by animateFloatAsState(
                    targetValue = baseOffsetY.value,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "baseOffsetY"
                )

                val targetScale = when (absDelta) {
                    0 -> 1.0f
                    1 -> 0.88f
                    else -> 0.76f
                }
                // Center active card is fully opaque 1.0f!
                val targetAlpha = when (absDelta) {
                    0 -> 1.0f
                    1 -> 0.70f
                    else -> 0.35f
                }

                val animatedScale by animateFloatAsState(
                    targetValue = targetScale,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "cardScale"
                )
                val animatedAlpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "cardAlpha"
                )

                val finalOffsetY = (animatedBaseOffsetY.dp + (currentDragOffset * 0.75f).dp)

                Box(
                    modifier = Modifier
                        .offset(y = finalOffsetY)
                        .zIndex(zIndexVal)
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
                            alpha = animatedAlpha
                        }
                        .clickable {
                            if (!isCenter) {
                                onFocusChange(index)
                            }
                        }
                ) {
                    CarouselRoutineCard(
                        routine = routine,
                        status = status,
                        nowTime = nowTime,
                        mainViewState = mainViewState,
                        isCenter = isCenter,
                        isShaking = uiState.shakeRoutineId == routine.id,
                        shakeTrigger = uiState.shakeTrigger,
                        onMarkDone = { onMarkDone(routine) },
                        onMarkEmpty = { onMarkEmpty(routine) },
                        onOpenDetail = { onOpenDetail(routine) },
                        onOpenInfo = { onOpenInfo(routine) }
                    )
                }
            }
        }
    }
}

/**
 * Fully opaque, solid glass Routine Card with countdown timer / elapsed timer calculation
 */
@Composable
fun CarouselRoutineCard(
    routine: Routine,
    status: RoutineStatus,
    nowTime: LocalTime,
    mainViewState: MainViewState,
    isCenter: Boolean,
    isShaking: Boolean,
    shakeTrigger: Int,
    onMarkDone: () -> Unit,
    onMarkEmpty: () -> Unit,
    onOpenDetail: () -> Unit,
    onOpenInfo: () -> Unit
) {
    val solidActiveCenterBg = Color(0xF2162035) // Deep solid midnight blue glass
    val solidActiveCenterBorder = AccentPurple

    val cardBg by animateColorAsState(
        targetValue = when {
            isCenter -> when (status) {
                RoutineStatus.DONE -> Color(0xF2122822)
                RoutineStatus.ACTIVE -> Color(0xF2201944)
                else -> solidActiveCenterBg
            }
            else -> when (status) {
                RoutineStatus.DONE -> Color(0x99122822)
                RoutineStatus.ACTIVE -> Color(0xAA201944)
                else -> Color(0x991E3250)
            }
        },
        animationSpec = tween(300),
        label = "cBg"
    )

    val cardBorder by animateColorAsState(
        targetValue = when {
            isCenter -> when (status) {
                RoutineStatus.DONE -> DoneGreen
                RoutineStatus.ACTIVE -> AccentPurple
                else -> solidActiveCenterBorder
            }
            else -> when (status) {
                RoutineStatus.DONE -> DoneGreen.copy(alpha = 0.5f)
                RoutineStatus.ACTIVE -> AccentPurple.copy(alpha = 0.6f)
                else -> GlassCardBorderSubtle
            }
        },
        animationSpec = tween(300),
        label = "cBorder"
    )

    // Calculate Countdown / Elapsed string
    val countdownText = remember(nowTime, routine) {
        val start = try {
            LocalTime.parse(routine.startTime.ifEmpty { routine.time })
        } catch (e: Exception) {
            LocalTime.of(8, 0)
        }

        if (routine.isRange && !routine.endTime.isNullOrBlank()) {
            val end = try {
                LocalTime.parse(routine.endTime)
            } catch (e: Exception) {
                start.plusHours(1)
            }

            if (!nowTime.isBefore(start) && !nowTime.isAfter(end)) {
                // Inside range: show time remaining until end time
                val remSeconds = Duration.between(nowTime, end).seconds.coerceAtLeast(0)
                val mm = remSeconds / 60
                val ss = remSeconds % 60
                String.format(Locale.US, "Kalan: %02d:%02d", mm, ss)
            } else if (nowTime.isBefore(start)) {
                val toStart = Duration.between(nowTime, start).seconds.coerceAtLeast(0)
                val mm = toStart / 60
                val ss = toStart % 60
                String.format(Locale.US, "Başlamaya: %02d:%02d", mm, ss)
            } else {
                "Süre Tamamlandı"
            }
        } else {
            // Single time routine:
            val diffSec = Duration.between(nowTime, start).seconds
            if (diffSec > 0) {
                // Counting down to start time (e.g. 15:00, 14:59...)
                val mm = diffSec / 60
                val ss = diffSec % 60
                String.format(Locale.US, "Geri sayım: %02d:%02d", mm, ss)
            } else {
                // Started: counts UP / elapsed time
                val elapsed = abs(diffSec)
                val mm = elapsed / 60
                val ss = elapsed % 60
                String.format(Locale.US, "Geçen süre: +%02d:%02d", mm, ss)
            }
        }
    }

    Box(
        modifier = Modifier
            .widthIn(max = if (isCenter) 345.dp else 295.dp)
            .fillMaxWidth(if (isCenter) 0.96f else 0.85f)
            .clip(RoundedCornerShape(24.dp))
            .background(cardBg)
            .border(if (isCenter) 2.dp else 1.dp, cardBorder, RoundedCornerShape(24.dp))
            .shake(if (isShaking) shakeTrigger else 0)
            .padding(horizontal = 14.dp, vertical = 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header Row: Time + Interval Tag + Category Pill + Information Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Time & Interval Indicator (if isRange)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = SubtitleWhite,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = routine.time,
                        color = PureWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (routine.isRange) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0x337CF4DE))
                                .border(1.dp, AccentTeal.copy(alpha = 0.6f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "⏳ ${routine.durationMinutes} dk Aralık",
                                color = AccentTeal,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Right: Category Pill + Information (İ) Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0x33A78BFA))
                            .border(1.dp, Color(0x55A78BFA), RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = routine.category,
                            color = AccentPurple,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Information (İ) Button (Odak Modu & Alt Adımlar)
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                            .border(1.dp, GlassCardBorderSubtle, CircleShape)
                            .clickable { onOpenInfo() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "Rutin Seçenekleri",
                            tint = if (routine.isRange) AccentTeal else AccentPurple,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Center SVG Illustration
            RoutineIllustration(
                illustrationId = routine.illustrationId,
                size = if (isCenter) 110.dp else 70.dp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Routine Title
            Text(
                text = routine.name,
                color = PureWhite,
                fontSize = if (isCenter) 20.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (isCenter) {
                Spacer(modifier = Modifier.height(8.dp))

                // Countdown / Elapsed Timer Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0x337CF4DE))
                        .border(1.dp, AccentTeal.copy(alpha = 0.6f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (routine.isRange) Icons.Rounded.Schedule else Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = AccentTeal,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (routine.isRange) "Aralık: ${routine.durationMinutes} dk • $countdownText" else countdownText,
                            color = PureWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons Row: Tamamlandı & Boş Geç & Not
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isDone = status == RoutineStatus.DONE

                    // "Tamamlandı" Button
                    Button(
                        onClick = onMarkDone,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp)
                            .springPress { onMarkDone() },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDone) DoneGreen else Color(0x3310B981),
                            contentColor = if (isDone) Color(0xFF0F172A) else DoneGreen
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DoneGreen)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isDone) Icons.Rounded.CheckCircle else Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isDone) "Tamamlandı" else "Tamamla",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // "Boş Geç" Button
                    OutlinedButton(
                        onClick = onMarkEmpty,
                        modifier = Modifier
                            .weight(1.0f)
                            .height(44.dp)
                            .springPress { onMarkEmpty() },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0x1AFFFFFF),
                            contentColor = SubtitleWhite
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Boş Geç",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Not Button
                    IconButton(
                        onClick = onOpenDetail,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x22FFFFFF))
                            .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EditNote,
                            contentDescription = "Not Ekle / Oku",
                            tint = AccentTeal,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}
