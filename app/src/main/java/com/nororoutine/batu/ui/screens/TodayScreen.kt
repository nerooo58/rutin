package com.nororoutine.batu.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Thermostat
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.nororoutine.batu.model.Routine
import com.nororoutine.batu.model.RoutineStatus
import com.nororoutine.batu.ui.components.PulsingDot
import com.nororoutine.batu.ui.components.RoutineDetailBottomSheet
import com.nororoutine.batu.ui.components.shake
import com.nororoutine.batu.ui.components.springPress
import com.nororoutine.batu.ui.illustrations.RoutineIllustration
import com.nororoutine.batu.ui.theme.AccentPurple
import com.nororoutine.batu.ui.theme.AccentTeal
import com.nororoutine.batu.ui.theme.ActiveBannerBg
import com.nororoutine.batu.ui.theme.ActiveBannerBorder
import com.nororoutine.batu.ui.theme.DoneCardBg
import com.nororoutine.batu.ui.theme.DoneCardBorder
import com.nororoutine.batu.ui.theme.DoneGreen
import com.nororoutine.batu.ui.theme.GlassCardBackground
import com.nororoutine.batu.ui.theme.GlassCardBorderSubtle
import com.nororoutine.batu.ui.theme.MissedCardBg
import com.nororoutine.batu.ui.theme.MissedCardBorder
import com.nororoutine.batu.ui.theme.MissedRed
import com.nororoutine.batu.ui.theme.MutedWhite
import com.nororoutine.batu.ui.theme.PureWhite
import com.nororoutine.batu.ui.theme.PurpleGradientStart
import com.nororoutine.batu.ui.theme.SubtitleWhite
import com.nororoutine.batu.viewmodel.RutinUiState
import com.nororoutine.batu.viewmodel.RutinViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class TodayDisplayMode {
    CAROUSEL,
    WEATHER_IDLE
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
    val turkishLocale = remember { Locale("tr", "TR") }

    val formattedDate = remember(today) {
        val day = today.dayOfMonth
        val month = today.format(DateTimeFormatter.ofPattern("MMMM", turkishLocale))
        val dayOfWeek = today.format(DateTimeFormatter.ofPattern("EEEE", turkishLocale))
        "$day $month $dayOfWeek"
    }

    // Fetch weather on launch
    LaunchedEffect(Unit) {
        viewModel.fetchWeather(context)
    }

    val todayRoutines = viewModel.getRoutinesForDay(today)
    val totalCount = todayRoutines.size
    val completedCount = todayRoutines.count {
        viewModel.getRoutineStatus(it, today) == RoutineStatus.DONE
    }

    val activeRoutine = todayRoutines.find {
        viewModel.getRoutineStatus(it, today) == RoutineStatus.ACTIVE
    }

    // Determine initial display mode:
    // If no active routine right now, show weather/time idle state by default
    var displayMode by remember(activeRoutine, todayRoutines.size) {
        mutableStateOf(
            if (activeRoutine != null) TodayDisplayMode.CAROUSEL else TodayDisplayMode.WEATHER_IDLE
        )
    }

    // Carousel focused index
    val initialFocusedIndex = remember(activeRoutine, todayRoutines) {
        if (activeRoutine != null) {
            val idx = todayRoutines.indexOfFirst { it.id == activeRoutine.id }
            if (idx >= 0) idx else 0
        } else 0
    }
    var focusedIndex by remember(todayRoutines.size) { mutableIntStateOf(initialFocusedIndex) }

    var selectedRoutineForSheet by remember { mutableStateOf<Routine?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Header: NORO badge, Date, and Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // NORO Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0x1F7CF4DE))
                        .border(1.dp, Color(0x337CF4DE), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    PulsingDot(color = AccentTeal, dotSize = 7.dp, pulseMaxScale = 2.2f)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NORO",
                        color = PureWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }

                // Date & Mode Switcher
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedDate,
                        color = MutedWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (totalCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                displayMode = if (displayMode == TodayDisplayMode.CAROUSEL) {
                                    TodayDisplayMode.WEATHER_IDLE
                                } else {
                                    TodayDisplayMode.CAROUSEL
                                }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (displayMode == TodayDisplayMode.WEATHER_IDLE) Color(0x337CF4DE) else Color(0x22FFFFFF))
                        ) {
                            Icon(
                                imageVector = if (displayMode == TodayDisplayMode.CAROUSEL) Icons.Rounded.Thermostat else Icons.Rounded.ViewCarousel,
                                contentDescription = "Mod Değiştir",
                                tint = if (displayMode == TodayDisplayMode.WEATHER_IDLE) AccentTeal else PureWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main View Area: Animated between Carousel Stack and Weather Idle
            AnimatedContent(
                targetState = displayMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "todayMode"
            ) { mode ->
                if (mode == TodayDisplayMode.WEATHER_IDLE || totalCount == 0) {
                    WeatherIdleScreen(
                        weatherInfo = uiState.weatherInfo,
                        onRefreshWeather = { viewModel.fetchWeather(context) },
                        totalTodayRoutines = totalCount,
                        onViewRoutinesClick = {
                            if (totalCount > 0) {
                                displayMode = TodayDisplayMode.CAROUSEL
                            } else {
                                onNavigateToEdit()
                            }
                        }
                    )
                } else {
                    // Vertical Card Stack / Carousel View
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Title & Progress Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Günün rutinleri",
                                    color = PureWhite,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$completedCount/$totalCount tamamlandı",
                                    color = AccentTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Switch to weather button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color(0x221E3250))
                                    .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(999.dp))
                                    .clickable { displayMode = TodayDisplayMode.WEATHER_IDLE }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Thermostat,
                                        contentDescription = null,
                                        tint = AccentTeal,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${uiState.weatherInfo.temperature.toInt()}°C",
                                        color = PureWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress Bar
                        val progressTarget = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
                        val animatedProgress by animateFloatAsState(
                            targetValue = progressTarget,
                            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                            label = "prog"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0x22FFFFFF))
                        ) {
                            if (animatedProgress > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedProgress)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(PurpleGradientStart, AccentTeal)
                                            )
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Vertical Card Stack Carousel
                        VerticalRoutineCardStack(
                            routines = todayRoutines,
                            focusedIndex = focusedIndex,
                            viewModel = viewModel,
                            uiState = uiState,
                            today = today,
                            onFocusChange = { newIdx ->
                                focusedIndex = newIdx.coerceIn(0, todayRoutines.lastIndex)
                            },
                            onToggleStatus = { routine ->
                                viewModel.toggleRoutineStatus(routine.id, today)
                            },
                            onOpenDetail = { routine ->
                                selectedRoutineForSheet = routine
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    }
                }
            }
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
 * Vertical Card Stack / Carousel with 3D perspective stacking and swipe gesture.
 */
@Composable
fun VerticalRoutineCardStack(
    routines: List<Routine>,
    focusedIndex: Int,
    viewModel: RutinViewModel,
    uiState: RutinUiState,
    today: LocalDate,
    onFocusChange: (Int) -> Unit,
    onToggleStatus: (Routine) -> Unit,
    onOpenDetail: (Routine) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragAccumulator by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    val draggableState = rememberDraggableState { delta ->
        dragAccumulator += delta
        if (dragAccumulator < -60f && focusedIndex < routines.lastIndex) {
            onFocusChange(focusedIndex + 1)
            dragAccumulator = 0f
        } else if (dragAccumulator > 60f && focusedIndex > 0) {
            onFocusChange(focusedIndex - 1)
            dragAccumulator = 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .draggable(
                state = draggableState,
                orientation = Orientation.Vertical,
                onDragStopped = { dragAccumulator = 0f }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Navigation Indicators (Up / Down)
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

        // Render cards in order of distance so active is on top
        routines.forEachIndexed { index, routine ->
            val delta = index - focusedIndex
            val absDelta = abs(delta)

            // Only display cards within visible window (-2 to 2)
            if (absDelta <= 2) {
                val status = viewModel.getRoutineStatus(routine, today)
                val isCenter = delta == 0
                val zIndexVal = (10 - absDelta).toFloat()

                // Target scale & alpha & translation
                val targetScale = when (absDelta) {
                    0 -> 1.0f
                    1 -> 0.88f
                    else -> 0.76f
                }
                val targetAlpha = when (absDelta) {
                    0 -> 1.0f
                    1 -> 0.65f
                    else -> 0.30f
                }
                val targetOffsetY = when {
                    delta < 0 -> -90.dp * absDelta
                    delta > 0 -> 90.dp * absDelta
                    else -> 0.dp
                }

                val animatedScale by animateFloatAsState(
                    targetValue = targetScale,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "cardScale"
                )
                val animatedAlpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "cardAlpha"
                )

                Box(
                    modifier = Modifier
                        .offset(y = targetOffsetY)
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

@Composable
fun CarouselRoutineCard(
    routine: Routine,
    status: RoutineStatus,
    isCenter: Boolean,
    isShaking: Boolean,
    shakeTrigger: Int,
    onToggle: () -> Unit,
    onOpenDetail: () -> Unit
) {
    val cardBg by animateColorAsState(
        targetValue = when (status) {
            RoutineStatus.DONE -> DoneCardBg
            RoutineStatus.MISSED -> MissedCardBg
            RoutineStatus.ACTIVE -> ActiveBannerBg
            RoutineStatus.UPCOMING -> GlassCardBackground
        },
        animationSpec = tween(300),
        label = "cBg"
    )

    val cardBorder by animateColorAsState(
        targetValue = when (status) {
            RoutineStatus.DONE -> DoneCardBorder
            RoutineStatus.MISSED -> MissedCardBorder
            RoutineStatus.ACTIVE -> AccentPurple
            RoutineStatus.UPCOMING -> GlassCardBorderSubtle
        },
        animationSpec = tween(300),
        label = "cBorder"
    )

    val titleColor = when (status) {
        RoutineStatus.DONE -> DoneGreen
        RoutineStatus.MISSED -> MissedRed
        RoutineStatus.ACTIVE -> PureWhite
        RoutineStatus.UPCOMING -> PureWhite.copy(alpha = 0.7f)
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
            // Header Row: Time + Status Badge
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
                        .background(Color(0x1FA78BFA))
                        .border(1.dp, Color(0x33A78BFA), RoundedCornerShape(999.dp))
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

            // Center SVG Illustration (Large on active center card)
            RoutineIllustration(
                illustrationId = routine.illustrationId,
                size = if (isCenter) 110.dp else 70.dp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Routine Title
            Text(
                text = routine.name,
                color = titleColor,
                fontSize = if (isCenter) 20.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (isCenter) {
                Spacer(modifier = Modifier.height(6.dp))

                // Status text
                val statusText = when (status) {
                    RoutineStatus.ACTIVE -> "Şu an Aktif Zaman Dilimi"
                    RoutineStatus.DONE -> "Tamamlandı ✓"
                    RoutineStatus.MISSED -> "Kaçırıldı ✕"
                    RoutineStatus.UPCOMING -> "Yaklaşan Rutin"
                }
                Text(
                    text = statusText,
                    color = when (status) {
                        RoutineStatus.DONE -> DoneGreen
                        RoutineStatus.MISSED -> MissedRed
                        RoutineStatus.ACTIVE -> AccentTeal
                        else -> SubtitleWhite
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons Row (Interactive on center card)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Toggle Done / Missed Button
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

                    // Journal / Note Button
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
