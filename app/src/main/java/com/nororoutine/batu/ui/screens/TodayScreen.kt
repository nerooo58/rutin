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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.nororoutine.batu.model.Routine
import com.nororoutine.batu.model.RoutineStatus
import com.nororoutine.batu.ui.components.PulsingDot
import com.nororoutine.batu.ui.components.RoutineDetailBottomSheet
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
import androidx.compose.ui.text.style.TextOverflow
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

    // Determine state 1, 2, or 3 based on active or approaching routine
    // Active routine: routine whose scheduled time has started / inside range, and not marked done/missed
    val activeRoutine = todayRoutines.find { routine ->
        val status = viewModel.getRoutineStatus(routine, today)
        status == RoutineStatus.ACTIVE
    }

    // Approaching routine: within 15 min before start time, not done/missed
    val approachingRoutine = todayRoutines.find { routine ->
        val logKey = "${today.format(DateTimeFormatter.ISO_LOCAL_DATE)}_${routine.id}"
        val isDoneOrMissed = uiState.logs[logKey] != null
        if (isDoneOrMissed) return@find false

        val start = try {
            LocalTime.parse(routine.startTime.ifEmpty { routine.time })
        } catch (e: Exception) {
            LocalTime.of(8, 0)
        }
        val diffSeconds = Duration.between(nowLocalTime, start).seconds
        diffSeconds in 1..900 // 0 to 15 minutes before start
    }

    // Determine the main view state
    val mainViewState = remember(activeRoutine, approachingRoutine) {
        when {
            activeRoutine != null -> MainViewState.ACTIVE
            approachingRoutine != null -> MainViewState.APPROACHING
            else -> MainViewState.IDLE
        }
    }

    // If active routine is present, check if ANOTHER routine is approaching (within 15 min)
    val nextApproachingWhileActive = remember(activeRoutine, approachingRoutine, todayRoutines, nowLocalTime) {
        if (activeRoutine != null) {
            todayRoutines.find { routine ->
                if (routine.id == activeRoutine.id) return@find false
                val logKey = "${today.format(DateTimeFormatter.ISO_LOCAL_DATE)}_${routine.id}"
                val isDoneOrMissed = uiState.logs[logKey] != null
                if (isDoneOrMissed) return@find false

                val start = try {
                    LocalTime.parse(routine.startTime.ifEmpty { routine.time })
                } catch (e: Exception) {
                    LocalTime.of(8, 0)
                }
                val diffSeconds = Duration.between(nowLocalTime, start).seconds
                diffSeconds in 1..900
            }
        } else null
    }

    // Routine list for carousel: in STATE 2/3, prioritize the active/approaching routine
    val targetRoutine = activeRoutine ?: approachingRoutine ?: todayRoutines.firstOrNull()

    var focusedIndex by remember(targetRoutine?.id, todayRoutines.size) {
        val idx = if (targetRoutine != null) todayRoutines.indexOfFirst { it.id == targetRoutine.id } else 0
        mutableIntStateOf(if (idx >= 0) idx else 0)
    }

    var selectedRoutineForSheet by remember { mutableStateOf<Routine?>(null) }
    var showTodayRoutinesSheet by remember { mutableStateOf(false) }

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
            // LANDSCAPE (tablet) layout:
            // Left side: time/date/weather info
            // Right side: routine card
            // "Günün Rutinleri" count pill next to weather info on the right
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
                focusedRoutine = todayRoutines.getOrNull(focusedIndex) ?: targetRoutine,
                viewModel = viewModel,
                today = today,
                onViewRoutinesClick = { showTodayRoutinesSheet = true },
                onToggleRoutine = { routine -> viewModel.toggleRoutineStatus(routine.id, today) },
                onSelectRoutine = { routine ->
                    val idx = todayRoutines.indexOfFirst { it.id == routine.id }
                    if (idx >= 0) focusedIndex = idx
                },
                onOpenNote = { routine -> selectedRoutineForSheet = routine }
            )
        } else {
            // PORTRAIT (phone) layout:
            when (mainViewState) {
                MainViewState.IDLE -> {
                    // STATE 1 - IDLE:
                    // - Large clock in center showing current time
                    // - Date below: "Pazartesi, 12 Ekim 2026"
                    // - Weather illustration (SVG) centered
                    // - Temperature + weather condition + city/district name below illustration
                    // - "İyi Akşamlar" style greeting at top based on time of day
                    // - Bottom: compact "Günün Rutinleri (X)" pill button -> opens bottom sheet
                    WeatherIdleScreen(
                        weatherInfo = uiState.weatherInfo,
                        onRefreshWeather = { viewModel.fetchWeather(context) },
                        totalTodayRoutines = totalCount,
                        completedTodayRoutines = completedCount,
                        todayPercent = todayPercent,
                        currentStreak = currentStreak,
                        onViewRoutinesClick = {
                            if (totalCount > 0) {
                                showTodayRoutinesSheet = true
                            } else {
                                onNavigateToEdit()
                            }
                        }
                    )
                }

                MainViewState.APPROACHING, MainViewState.ACTIVE -> {
                    // STATE 2 (APPROACHING) & STATE 3 (ACTIVE):
                    // - Top bar: compact time + date + weather info in one small row
                    // - Weather illustration disappears
                    // - Center: large routine card with illustration, name, countdown
                    // - Countdown: 15 min before countdown to start; inside active counts up or remaining
                    // - Bottom: "Günün Rutinleri" pill
                    PortraitActiveApproachingLayout(
                        state = mainViewState,
                        nowTime = nowLocalTime,
                        routines = todayRoutines,
                        focusedIndex = focusedIndex,
                        onFocusChange = { focusedIndex = it },
                        uiState = uiState,
                        viewModel = viewModel,
                        today = today,
                        locationText = locationText,
                        totalCount = totalCount,
                        completedCount = completedCount,
                        todayPercent = todayPercent,
                        currentStreak = currentStreak,
                        onViewRoutinesClick = { showTodayRoutinesSheet = true },
                        onToggleRoutine = { routine -> viewModel.toggleRoutineStatus(routine.id, today) },
                        onOpenNote = { routine -> selectedRoutineForSheet = routine }
                    )
                }
            }
        }

        // Small banner slides up from bottom if ANOTHER routine is approaching (within 15 min) while STATE 3 is active
        AnimatedVisibility(
            visible = (mainViewState == MainViewState.ACTIVE && nextApproachingWhileActive != null),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 74.dp, start = 20.dp, end = 20.dp)
        ) {
            nextApproachingWhileActive?.let { approaching ->
                val start = try {
                    LocalTime.parse(approaching.startTime.ifEmpty { approaching.time })
                } catch (e: Exception) {
                    LocalTime.of(8, 0)
                }
                val minLeft = Duration.between(nowLocalTime, start).toMinutes().coerceAtLeast(1)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xF21C1635))
                        .border(1.5.dp, AccentPurple, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = AccentTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sıradaki: ${approaching.name} — $minLeft dk",
                                color = PureWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0x33A78BFA))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = approaching.startTime,
                                color = AccentPurple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
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
    }
}

/**
 * PORTRAIT Layout for STATE 2 (Approaching) & STATE 3 (Active)
 */
@Composable
fun PortraitActiveApproachingLayout(
    state: MainViewState,
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
    onViewRoutinesClick: () -> Unit,
    onToggleRoutine: (Routine) -> Unit,
    onOpenNote: (Routine) -> Unit
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
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Compact Top Bar: small clock, date, temp, city — all compact in one row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xF2141F33)) // Solid glass bar
                .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(999.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
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

                // Temp & Location
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Thermostat,
                        contentDescription = null,
                        tint = AccentTeal,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${uiState.weatherInfo.temperature.toInt()}°C",
                        color = PureWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = AccentPurple,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = locationText.take(16),
                        color = SubtitleWhite,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // State indicator pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(if (state == MainViewState.ACTIVE) Color(0x3310B981) else Color(0x33A78BFA))
                .border(1.dp, if (state == MainViewState.ACTIVE) DoneGreen else AccentPurple, RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulsingDot(
                    color = if (state == MainViewState.ACTIVE) DoneGreen else AccentPurple,
                    dotSize = 6.dp,
                    pulseMaxScale = 2.0f
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (state == MainViewState.ACTIVE) "AKTİF RUTİN" else "YAKLAŞAN RUTİN (15 DK İÇİNDE)",
                    color = PureWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Center: Large routine card with smooth spring physics
        VerticalRoutineCardStack(
            routines = routines,
            focusedIndex = focusedIndex,
            nowTime = nowTime,
            mainViewState = state,
            viewModel = viewModel,
            uiState = uiState,
            today = today,
            onFocusChange = onFocusChange,
            onToggleStatus = onToggleRoutine,
            onOpenDetail = onOpenNote,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        // Bottom: "Günün Rutinleri" pill with completion rate and streak
        val isAllCompleted = totalCount > 0 && completedCount == totalCount
        Row(
            modifier = Modifier.padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStreak > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0x33F59E0B))
                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 10.dp)
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
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (totalCount > 0) completionColor.copy(alpha = 0.22f) else Color(0xF2162035))
                    .border(1.dp, if (totalCount > 0) completionColor.copy(alpha = 0.75f) else GlassCardBorderSubtle, RoundedCornerShape(999.dp))
                    .clickable { onViewRoutinesClick() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isAllCompleted) Icons.Rounded.Check else Icons.Rounded.ViewCarousel,
                        contentDescription = null,
                        tint = if (totalCount > 0) completionColor else AccentTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (totalCount > 0) "Günün Rutinleri: $completedCount/$totalCount (%$todayPercent)" else "Günün Rutinleri (0)",
                        color = PureWhite,
                        fontSize = 13.sp,
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
    focusedRoutine: Routine?,
    viewModel: RutinViewModel,
    today: LocalDate,
    onViewRoutinesClick: () -> Unit,
    onToggleRoutine: (Routine) -> Unit,
    onSelectRoutine: (Routine) -> Unit = {},
    onOpenNote: (Routine) -> Unit
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
        // Left Side: Time, Date, Weather Card & Rich Routine Summary / Checklist
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
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
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateText,
                        color = SubtitleWhite,
                        fontSize = 14.sp,
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
                            .padding(horizontal = 10.dp, vertical = 6.dp)
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

            Spacer(modifier = Modifier.height(14.dp))

            // Weather Card with Location
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xF2131D31))
                    .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(18.dp))
                    .padding(14.dp)
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
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${uiState.weatherInfo.temperature.toInt()}°C • ${uiState.weatherInfo.condition.label}",
                                color = PureWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = locationText,
                                color = SubtitleWhite,
                                fontSize = 12.sp
                            )
                        }
                    }

                    val completionColor = getCompletionColor(todayPercent, totalTodayRoutines)
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (totalTodayRoutines > 0) "%$todayPercent" else "-",
                            color = completionColor,
                            fontSize = 16.sp,
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

            Spacer(modifier = Modifier.height(12.dp))

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
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(AccentPurple.copy(alpha = 0.25f))
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

                                val isSelected = focusedRoutine?.id == r.id
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
                                        .clickable { onSelectRoutine(r) }
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
                                                .clickable { onToggleRoutine(r) },
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
                                }
                            }
                        }
                    }
                }
            }
        }

        // Right Side: Active / Focused Routine Card
        Box(
            modifier = Modifier
                .weight(1.15f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            if (focusedRoutine != null) {
                val status = viewModel.getRoutineStatus(focusedRoutine, today)
                CarouselRoutineCard(
                    routine = focusedRoutine,
                    status = status,
                    nowTime = nowTime,
                    mainViewState = mainViewState,
                    isCenter = true,
                    isShaking = false,
                    shakeTrigger = 0,
                    onToggle = { onToggleRoutine(focusedRoutine) },
                    onOpenDetail = { onOpenNote(focusedRoutine) }
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
    onToggleStatus: (Routine) -> Unit,
    onOpenDetail: (Routine) -> Unit,
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
                        onToggle = { onToggleStatus(routine) },
                        onOpenDetail = { onOpenDetail(routine) }
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
    onToggle: () -> Unit,
    onOpenDetail: () -> Unit
) {
    val solidActiveCenterBg = Color(0xF2162035) // Deep solid midnight blue glass
    val solidActiveCenterBorder = AccentPurple

    val cardBg by animateColorAsState(
        targetValue = when {
            isCenter -> when (status) {
                RoutineStatus.DONE -> Color(0xF2122822)
                RoutineStatus.MISSED -> Color(0xF22B141E)
                RoutineStatus.ACTIVE -> Color(0xF2201944)
                RoutineStatus.UPCOMING -> solidActiveCenterBg
            }
            else -> when (status) {
                RoutineStatus.DONE -> Color(0x99122822)
                RoutineStatus.MISSED -> Color(0x992B141E)
                RoutineStatus.ACTIVE -> Color(0xAA201944)
                RoutineStatus.UPCOMING -> Color(0x991E3250)
            }
        },
        animationSpec = tween(300),
        label = "cBg"
    )

    val cardBorder by animateColorAsState(
        targetValue = when {
            isCenter -> when (status) {
                RoutineStatus.DONE -> DoneGreen
                RoutineStatus.MISSED -> MissedRed
                RoutineStatus.ACTIVE -> AccentPurple
                RoutineStatus.UPCOMING -> solidActiveCenterBorder
            }
            else -> when (status) {
                RoutineStatus.DONE -> DoneGreen.copy(alpha = 0.5f)
                RoutineStatus.MISSED -> MissedRed.copy(alpha = 0.5f)
                RoutineStatus.ACTIVE -> AccentPurple.copy(alpha = 0.6f)
                RoutineStatus.UPCOMING -> GlassCardBorderSubtle
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
            .width(if (isCenter) 330.dp else 290.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(cardBg)
            .border(if (isCenter) 2.dp else 1.dp, cardBorder, RoundedCornerShape(24.dp))
            .shake(if (isShaking) shakeTrigger else 0)
            .padding(18.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header Row: Time + Category Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                }

                // Category pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0x33A78BFA))
                        .border(1.dp, Color(0x55A78BFA), RoundedCornerShape(999.dp))
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = routine.category,
                        color = AccentPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
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
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = AccentTeal,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = countdownText,
                            color = PureWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onToggle,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .springPress { onToggle() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (status) {
                                RoutineStatus.DONE -> DoneGreen.copy(alpha = 0.25f)
                                RoutineStatus.MISSED -> MissedRed.copy(alpha = 0.25f)
                                else -> AccentPurple.copy(alpha = 0.25f)
                            },
                            contentColor = when (status) {
                                RoutineStatus.DONE -> DoneGreen
                                RoutineStatus.MISSED -> MissedRed
                                else -> PureWhite
                            }
                        )
                    ) {
                        Icon(
                            imageVector = when (status) {
                                RoutineStatus.DONE -> Icons.Rounded.Check
                                RoutineStatus.MISSED -> Icons.Rounded.Close
                                else -> Icons.Rounded.RadioButtonUnchecked
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (status) {
                                RoutineStatus.DONE -> "Tamamlandı"
                                RoutineStatus.MISSED -> "Kaçırıldı"
                                else -> "İşaretle"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    IconButton(
                        onClick = onOpenDetail,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x22FFFFFF))
                            .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EditNote,
                            contentDescription = "Not Ekle / Oku",
                            tint = AccentTeal,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
