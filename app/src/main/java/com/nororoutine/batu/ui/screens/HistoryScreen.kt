package com.nororoutine.batu.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nororoutine.batu.model.Routine
import com.nororoutine.batu.model.RoutineStatus
import com.nororoutine.batu.model.UserStats
import com.nororoutine.batu.ui.illustrations.RoutineIllustration
import com.nororoutine.batu.ui.theme.AccentPurple
import com.nororoutine.batu.ui.theme.AccentTeal
import com.nororoutine.batu.ui.theme.DoneCardBg
import com.nororoutine.batu.ui.theme.DoneCardBorder
import com.nororoutine.batu.ui.theme.DoneGreen
import com.nororoutine.batu.ui.theme.GlassCardBackground
import com.nororoutine.batu.ui.theme.GlassCardBorder
import com.nororoutine.batu.ui.theme.GlassCardBorderSubtle
import com.nororoutine.batu.ui.theme.MissedCardBg
import com.nororoutine.batu.ui.theme.MissedCardBorder
import com.nororoutine.batu.ui.theme.MissedRed
import com.nororoutine.batu.ui.theme.MutedWhite
import com.nororoutine.batu.ui.theme.PureWhite
import com.nororoutine.batu.ui.theme.SubtitleWhite
import com.nororoutine.batu.viewmodel.RutinUiState
import com.nororoutine.batu.viewmodel.RutinViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class HistoryViewMode {
    WEEK,
    MONTH,
    YEAR
}

/**
 * Returns color based on completion percentage in logical progression:
 * - 0% - 20%: Kırmızı (Düşük)
 * - 21% - 40%: Turuncu (Gelişiyor)
 * - 41% - 60%: Sarı (Orta)
 * - 61% - 80%: Mavi (İyi)
 * - 81% - 100%: Yeşil (Mükemmel)
 */
fun getCompletionColor(percent: Int, total: Int): Color {
    if (total == 0) return Color(0x33FFFFFF)
    return when {
        percent <= 20 -> Color(0xFFEF4444) // Kırmızı
        percent <= 40 -> Color(0xFFF97316) // Turuncu
        percent <= 60 -> Color(0xFFFBBF24) // Sarı
        percent <= 80 -> Color(0xFF38BDF8) // Mavi
        else -> Color(0xFF10B981)          // Yeşil
    }
}

@Composable
fun HistoryScreen(
    viewModel: RutinViewModel,
    uiState: RutinUiState,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(HistoryViewMode.WEEK) }
    var currentWeekAnchor by remember { mutableStateOf(LocalDate.now()) }
    var currentMonthAnchor by remember { mutableStateOf(YearMonth.now()) }
    var currentYearAnchor by remember { mutableIntStateOf(LocalDate.now().year) }
    var selectedDayForDetail by remember { mutableStateOf<LocalDate?>(null) }
    var selectedMonthDetail by remember { mutableStateOf<Int?>(null) }

    val turkishLocale = remember { Locale("tr", "TR") }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            // Header
            item {
                Text(
                    text = "Geçmiş",
                    color = PureWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Rutin tamamlama geçmişinizi analiz edin",
                    color = MutedWhite,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Streak & Completion Statistics Card (Günlük, Hafta, Ay, Yıl)
                StatisticsStreakOverviewCard(stats = uiState.stats)

                Spacer(modifier = Modifier.height(18.dp))

                // Segmented control: Hafta / Ay / Yıl
                SegmentedControl(
                    selectedMode = viewMode,
                    onSelect = {
                        viewMode = it
                        selectedDayForDetail = null
                        selectedMonthDetail = null
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // View Mode Content: Week, Month, or Year with horizontal slide animation
            item {
                when (viewMode) {
                    HistoryViewMode.WEEK -> {
                        AnimatedContent(
                            targetState = currentWeekAnchor,
                            transitionSpec = {
                                if (targetState.isAfter(initialState)) {
                                    (slideInHorizontally(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        ),
                                        initialOffsetX = { fullWidth -> fullWidth }
                                    ) + fadeIn()).togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            ),
                                            targetOffsetX = { fullWidth -> -fullWidth }
                                        ) + fadeOut()
                                    )
                                } else {
                                    (slideInHorizontally(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        ),
                                        initialOffsetX = { fullWidth -> -fullWidth }
                                    ) + fadeIn()).togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            ),
                                            targetOffsetX = { fullWidth -> fullWidth }
                                        ) + fadeOut()
                                    )
                                }
                            },
                            label = "weekSlide"
                        ) { anchor ->
                            WeekHistoryView(
                                anchorDate = anchor,
                                viewModel = viewModel,
                                uiState = uiState,
                                selectedDay = selectedDayForDetail,
                                locale = turkishLocale,
                                onPrevWeek = { currentWeekAnchor = currentWeekAnchor.minusWeeks(1) },
                                onNextWeek = { currentWeekAnchor = currentWeekAnchor.plusWeeks(1) },
                                onDayClick = { day ->
                                    selectedDayForDetail = if (selectedDayForDetail == day) null else day
                                }
                            )
                        }
                    }

                    HistoryViewMode.MONTH -> {
                        AnimatedContent(
                            targetState = currentMonthAnchor,
                            transitionSpec = {
                                if (targetState.isAfter(initialState)) {
                                    (slideInHorizontally(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        ),
                                        initialOffsetX = { fullWidth -> fullWidth }
                                    ) + fadeIn()).togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            ),
                                            targetOffsetX = { fullWidth -> -fullWidth }
                                        ) + fadeOut()
                                    )
                                } else {
                                    (slideInHorizontally(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        ),
                                        initialOffsetX = { fullWidth -> -fullWidth }
                                    ) + fadeIn()).togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            ),
                                            targetOffsetX = { fullWidth -> fullWidth }
                                        ) + fadeOut()
                                    )
                                }
                            },
                            label = "monthSlide"
                        ) { anchorMonth ->
                            MonthHistoryView(
                                currentMonth = anchorMonth,
                                viewModel = viewModel,
                                uiState = uiState,
                                selectedDay = selectedDayForDetail,
                                locale = turkishLocale,
                                onPrevMonth = { currentMonthAnchor = currentMonthAnchor.minusMonths(1) },
                                onNextMonth = { currentMonthAnchor = currentMonthAnchor.plusMonths(1) },
                                onDayClick = { day ->
                                    selectedDayForDetail = if (selectedDayForDetail == day) null else day
                                }
                            )
                        }
                    }

                    HistoryViewMode.YEAR -> {
                        AnimatedContent(
                            targetState = currentYearAnchor,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    (slideInHorizontally(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        ),
                                        initialOffsetX = { fullWidth -> fullWidth }
                                    ) + fadeIn()).togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            ),
                                            targetOffsetX = { fullWidth -> -fullWidth }
                                        ) + fadeOut()
                                    )
                                } else {
                                    (slideInHorizontally(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        ),
                                        initialOffsetX = { fullWidth -> -fullWidth }
                                    ) + fadeIn()).togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            ),
                                            targetOffsetX = { fullWidth -> fullWidth }
                                        ) + fadeOut()
                                    )
                                }
                            },
                            label = "yearSlide"
                        ) { year ->
                            YearHistoryView(
                                year = year,
                                viewModel = viewModel,
                                uiState = uiState,
                                selectedMonth = selectedMonthDetail,
                                locale = turkishLocale,
                                onPrevYear = { currentYearAnchor -= 1 },
                                onNextYear = { currentYearAnchor += 1 },
                                onMonthClick = { m ->
                                    selectedMonthDetail = if (selectedMonthDetail == m) null else m
                                },
                                onOpenMonthInCalendar = { m ->
                                    currentMonthAnchor = YearMonth.of(year, m)
                                    viewMode = HistoryViewMode.MONTH
                                    selectedMonthDetail = null
                                }
                            )
                        }
                    }
                }
            }

            // Inline Day Detail Section (slides down into empty space below calendar without any popup/overlay!)
            item {
                AnimatedVisibility(
                    visible = selectedDayForDetail != null,
                    enter = expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    selectedDayForDetail?.let { day ->
                        Column(modifier = Modifier.padding(top = 18.dp)) {
                            InlineDayDetailCard(
                                date = day,
                                viewModel = viewModel,
                                uiState = uiState,
                                locale = turkishLocale,
                                onClose = { selectedDayForDetail = null }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Segmented Control for switching between Hafta, Ay, Yıl
 */
@Composable
fun SegmentedControl(
    selectedMode: HistoryViewMode,
    onSelect: (HistoryViewMode) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0x33101C30))
            .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(999.dp))
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            val modes = listOf(
                HistoryViewMode.WEEK to "Hafta",
                HistoryViewMode.MONTH to "Ay",
                HistoryViewMode.YEAR to "Yıl"
            )
            modes.forEach { (mode, label) ->
                val isSelected = selectedMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isSelected) AccentPurple else Color.Transparent)
                        .clickable { onSelect(mode) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color(0xFF0D0B1E) else SubtitleWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * HAFTALIK SÜTUN GRAFİĞİ:
 * 7 gün için renkli sütun grafiği:
 * - Üstte: %lik oran
 * - Ortada: Renkli dikey sütun grafiği (yüksekliği %ye göre, rengi Kırmızı/Turuncu/Sarı/Mavi/Yeşil)
 * - Altında: Gün ve tarih (örn: Pzt 12)
 */
@Composable
fun WeekHistoryView(
    anchorDate: LocalDate,
    viewModel: RutinViewModel,
    uiState: RutinUiState,
    selectedDay: LocalDate?,
    locale: Locale,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onDayClick: (LocalDate) -> Unit
) {
    val monday = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val sunday = monday.plusDays(6)

    val dateRangeLabel = remember(monday, sunday) {
        val startDay = monday.dayOfMonth
        val endDay = sunday.dayOfMonth
        val month = monday.format(DateTimeFormatter.ofPattern("MMM", locale))
        "$startDay - $endDay $month"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 40) {
                        onPrevWeek()
                    } else if (dragAmount < -40) {
                        onNextWeek()
                    }
                }
            }
    ) {
        // Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrevWeek,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = "Önceki Hafta",
                    tint = PureWhite
                )
            }

            Text(
                text = dateRangeLabel,
                color = PureWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = onNextWeek,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "Sonraki Hafta",
                    tint = PureWhite
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 7 Column Bar Chart Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val dayNames = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
            for (i in 0..6) {
                val dayDate = monday.plusDays(i.toLong())
                val dayRoutines = viewModel.getRoutinesForDay(dayDate)
                val isToday = dayDate == LocalDate.now()
                val isSelected = dayDate == selectedDay

                val doneCount = dayRoutines.count {
                    val key = "${dayDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}_${it.id}"
                    uiState.logs[key]?.isDone == true
                }
                val totalCount = dayRoutines.size
                val percent = if (totalCount > 0) ((doneCount * 100) / totalCount).coerceIn(0, 100) else 0
                val barColor = getCompletionColor(percent, totalCount)

                val animatedHeight by animateFloatAsState(
                    targetValue = if (totalCount > 0) (74f * percent / 100f).coerceAtLeast(if (percent > 0) 6f else 0f) else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "weekBarHeight"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            when {
                                isSelected -> Color(0xF2162035)
                                isToday -> Color(0x2E1E173C)
                                else -> GlassCardBackground
                            }
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = when {
                                isSelected -> AccentTeal
                                isToday -> AccentPurple
                                else -> GlassCardBorderSubtle
                            },
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onDayClick(dayDate) }
                        .padding(vertical = 10.dp, horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // 1. %lik oran üstte
                        Text(
                            text = if (totalCount > 0) "%$percent" else "-",
                            color = if (totalCount > 0) barColor else MutedWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // 2. Renkli dikey sütun grafiği
                        Box(
                            modifier = Modifier
                                .width(13.dp)
                                .height(74.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0x1FFFFFFF)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (totalCount > 0 && percent > 0) {
                                Box(
                                    modifier = Modifier
                                        .width(13.dp)
                                        .height(animatedHeight.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(barColor, barColor.copy(alpha = 0.65f))
                                            )
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(5.dp))

                        // 3. Tamamlanan / Toplam sayısı
                        Text(
                            text = if (totalCount > 0) "$doneCount/$totalCount" else "-",
                            color = MutedWhite,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 4. Gün adı ve tarih altında
                        Text(
                            text = dayNames[i],
                            color = if (isToday) AccentTeal else SubtitleWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${dayDate.dayOfMonth}",
                            color = PureWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * AYLIK SÜTUN GRAFİĞİ:
 * - Ayın tüm günleri için yatay kaydırılabilir sütun grafiği çubukları
 * - Ve ay takvim ızgarası ile renkli mini sütun grafikleri
 */
@Composable
fun MonthHistoryView(
    currentMonth: YearMonth,
    viewModel: RutinViewModel,
    uiState: RutinUiState,
    selectedDay: LocalDate?,
    locale: Locale,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit
) {
    val monthTitle = remember(currentMonth) {
        val monthName = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
        monthName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    val daysInMonth = currentMonth.lengthOfMonth()
    val dayNamesShort = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

    // Calculate month total & completion
    var monthTotal = 0
    var monthDone = 0
    for (day in 1..daysInMonth) {
        val d = currentMonth.atDay(day)
        val dRoutines = viewModel.getRoutinesForDay(d)
        monthTotal += dRoutines.size
        monthDone += dRoutines.count {
            val key = "${d.format(DateTimeFormatter.ISO_LOCAL_DATE)}_${it.id}"
            uiState.logs[key]?.isDone == true
        }
    }
    val monthPercent = if (monthTotal > 0) ((monthDone * 100) / monthTotal).coerceIn(0, 100) else 0
    val monthColor = getCompletionColor(monthPercent, monthTotal)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 40) {
                        onPrevMonth()
                    } else if (dragAmount < -40) {
                        onNextMonth()
                    }
                }
            }
    ) {
        // Month Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrevMonth,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = "Önceki Ay",
                    tint = PureWhite
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = monthTitle,
                    color = PureWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Aylık Oran: %$monthPercent ($monthDone/$monthTotal)",
                    color = monthColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(
                onClick = onNextMonth,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "Sonraki Ay",
                    tint = PureWhite
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Ayın Günlük Sütun Grafiği (Horizontal Scroll Bar Chart)
        Text(
            text = "Günlük İlerleme Grafiği",
            color = SubtitleWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(GlassCardBackground)
                .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(16.dp))
                .horizontalScroll(scrollState)
                .padding(vertical = 12.dp, horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (dayNum in 1..daysInMonth) {
                val dayDate = currentMonth.atDay(dayNum)
                val dayRoutines = viewModel.getRoutinesForDay(dayDate)
                val isToday = dayDate == LocalDate.now()
                val isSelected = dayDate == selectedDay

                val doneCount = dayRoutines.count {
                    val key = "${dayDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}_${it.id}"
                    uiState.logs[key]?.isDone == true
                }
                val totalCount = dayRoutines.size
                val percent = if (totalCount > 0) ((doneCount * 100) / totalCount).coerceIn(0, 100) else 0
                val barColor = getCompletionColor(percent, totalCount)
                val dayOfWeekIndex = dayDate.dayOfWeek.value - 1

                val animatedBarHeight by animateFloatAsState(
                    targetValue = if (totalCount > 0) (64f * percent / 100f).coerceAtLeast(if (percent > 0) 5f else 0f) else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "monthDayBar"
                )

                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                isSelected -> Color(0xF2162035)
                                isToday -> Color(0x33A78BFA)
                                else -> Color(0x11FFFFFF)
                            }
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = when {
                                isSelected -> AccentTeal
                                isToday -> AccentPurple
                                else -> Color.Transparent
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onDayClick(dayDate) }
                        .padding(vertical = 8.dp, horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // % Oran
                        Text(
                            text = if (totalCount > 0) "%$percent" else "-",
                            color = if (totalCount > 0) barColor else MutedWhite,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Dikey Sütun
                        Box(
                            modifier = Modifier
                                .width(11.dp)
                                .height(64.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0x1FFFFFFF)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (totalCount > 0 && percent > 0) {
                                Box(
                                    modifier = Modifier
                                        .width(11.dp)
                                        .height(animatedBarHeight.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(barColor, barColor.copy(alpha = 0.65f))
                                            )
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Oran
                        Text(
                            text = if (totalCount > 0) "$doneCount/$totalCount" else "-",
                            color = MutedWhite,
                            fontSize = 8.sp
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        // Gün ve Tarih
                        Text(
                            text = dayNamesShort.getOrElse(dayOfWeekIndex) { "" },
                            color = if (isToday) AccentTeal else SubtitleWhite,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$dayNum",
                            color = PureWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Month Grid Container with mini-bars
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(GlassCardBackground)
                .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                val dayHeaders = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dayHeaders.forEach { header ->
                        Text(
                            text = header,
                            color = SubtitleWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val firstDayOfMonth = currentMonth.atDay(1)
                val startDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 = Monday, 7 = Sunday
                val emptySlotsBefore = startDayOfWeek - 1
                val totalCells = ((emptySlotsBefore + daysInMonth + 6) / 7) * 7

                for (week in 0 until (totalCells / 7)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (dayCol in 0..6) {
                            val cellIndex = week * 7 + dayCol
                            val dayNumber = cellIndex - emptySlotsBefore + 1

                            if (dayNumber in 1..daysInMonth) {
                                val cellDate = currentMonth.atDay(dayNumber)
                                val dayRoutines = viewModel.getRoutinesForDay(cellDate)
                                val isToday = cellDate == LocalDate.now()
                                val isSelected = cellDate == selectedDay

                                val doneCount = dayRoutines.count {
                                    val key = "${cellDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}_${it.id}"
                                    uiState.logs[key]?.isDone == true
                                }
                                val totalCount = dayRoutines.size
                                val cellPercent = if (totalCount > 0) ((doneCount * 100) / totalCount).coerceIn(0, 100) else 0
                                val cellColor = getCompletionColor(cellPercent, totalCount)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(0.9f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                isSelected -> Color(0xF2162035)
                                                isToday -> Color(0x33A78BFA)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = when {
                                                isSelected -> AccentTeal
                                                isToday -> AccentPurple
                                                else -> Color.Transparent
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onDayClick(cellDate) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "$dayNumber",
                                            color = PureWhite,
                                            fontSize = 12.sp,
                                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (totalCount > 0) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            // Mini Sütun
                                            Box(
                                                modifier = Modifier
                                                    .width(6.dp)
                                                    .height(14.dp)
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(Color(0x22FFFFFF)),
                                                contentAlignment = Alignment.BottomCenter
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(6.dp)
                                                        .height((14f * cellPercent / 100f).coerceAtLeast(3f).dp)
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .background(cellColor)
                                                )
                                            }
                                            Text(
                                                text = "%$cellPercent",
                                                color = cellColor,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(0.9f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * YILLIK SÜTUN GRAFİĞİ:
 * 12 Ayın (Ocak'tan Aralık'a kadar) tamamlama yüzdesi ve renkli dikey sütun grafiği!
 * - Üstte: %lik oran
 * - Ortada: Renkli sütun grafiği barı (Kırmızı/Turuncu/Sarı/Mavi/Yeşil)
 * - Altında: Ay adı (Oca, Şub, Mar, vb.)
 */
@Composable
fun YearHistoryView(
    year: Int,
    viewModel: RutinViewModel,
    uiState: RutinUiState,
    selectedMonth: Int?,
    locale: Locale,
    onPrevYear: () -> Unit,
    onNextYear: () -> Unit,
    onMonthClick: (Int) -> Unit,
    onOpenMonthInCalendar: (Int) -> Unit
) {
    val monthNamesShort = listOf(
        "Oca", "Şub", "Mar", "Nis", "May", "Haz",
        "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara"
    )

    // Calculate yearly totals
    var yearlyTotal = 0
    var yearlyDone = 0
    val monthStatsList = (1..12).map { monthNum ->
        val ym = YearMonth.of(year, monthNum)
        val dCount = ym.lengthOfMonth()
        var mTotal = 0
        var mDone = 0
        for (day in 1..dCount) {
            val d = ym.atDay(day)
            val dRoutines = viewModel.getRoutinesForDay(d)
            mTotal += dRoutines.size
            mDone += dRoutines.count {
                val key = "${d.format(DateTimeFormatter.ISO_LOCAL_DATE)}_${it.id}"
                uiState.logs[key]?.isDone == true
            }
        }
        yearlyTotal += mTotal
        yearlyDone += mDone
        val mPercent = if (mTotal > 0) ((mDone * 100) / mTotal).coerceIn(0, 100) else 0
        Triple(mTotal, mDone, mPercent)
    }

    val yearlyPercent = if (yearlyTotal > 0) ((yearlyDone * 100) / yearlyTotal).coerceIn(0, 100) else 0
    val yearlyColor = getCompletionColor(yearlyPercent, yearlyTotal)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Year Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrevYear,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = "Önceki Yıl",
                    tint = PureWhite
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$year Yılı",
                    color = PureWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Yıllık Oran: %$yearlyPercent ($yearlyDone/$yearlyTotal)",
                    color = yearlyColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(
                onClick = onNextYear,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "Sonraki Yıl",
                    tint = PureWhite
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 12 Months Column Bar Chart (Horizontal Scrollable for large spacious bars)
        Text(
            text = "12 Aylık Tamamlama Grafiği",
            color = SubtitleWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(GlassCardBackground)
                .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(18.dp))
                .horizontalScroll(scrollState)
                .padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            monthStatsList.forEachIndexed { index, (mTotal, mDone, mPercent) ->
                val monthNum = index + 1
                val isSelected = selectedMonth == monthNum
                val isCurrentMonth = year == LocalDate.now().year && monthNum == LocalDate.now().monthValue
                val barColor = getCompletionColor(mPercent, mTotal)

                val animatedFillHeight by animateFloatAsState(
                    targetValue = if (mTotal > 0) (85f * mPercent / 100f).coerceAtLeast(if (mPercent > 0) 6f else 0f) else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "yearMonthBar"
                )

                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                isSelected -> Color(0xF2162035)
                                isCurrentMonth -> Color(0x33A78BFA)
                                else -> Color(0x14FFFFFF)
                            }
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = when {
                                isSelected -> AccentTeal
                                isCurrentMonth -> AccentPurple
                                else -> Color.Transparent
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onMonthClick(monthNum) }
                        .padding(vertical = 10.dp, horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // 1. %lik oran üstte
                        Text(
                            text = if (mTotal > 0) "%$mPercent" else "-",
                            color = if (mTotal > 0) barColor else MutedWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // 2. Renkli Dikey Sütun Grafiği
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(85.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0x1FFFFFFF)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (mTotal > 0 && mPercent > 0) {
                                Box(
                                    modifier = Modifier
                                        .width(14.dp)
                                        .height(animatedFillHeight.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(barColor, barColor.copy(alpha = 0.65f))
                                            )
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(5.dp))

                        // 3. Tamamlanan / Toplam
                        Text(
                            text = if (mTotal > 0) "$mDone/$mTotal" else "-",
                            color = MutedWhite,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 4. Ay adı altında
                        Text(
                            text = monthNamesShort[index],
                            color = if (isCurrentMonth) AccentTeal else PureWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Selected Month Details Expander
        selectedMonth?.let { monthNum ->
            val (mTotal, mDone, mPercent) = monthStatsList[monthNum - 1]
            val barColor = getCompletionColor(mPercent, mTotal)
            val monthFullName = YearMonth.of(year, monthNum).format(DateTimeFormatter.ofPattern("MMMM", locale))

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xF2162035))
                    .border(1.dp, barColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$monthFullName $year Detayları",
                                color = PureWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$mDone tamamlandı, $mTotal planlandı",
                                color = SubtitleWhite,
                                fontSize = 12.sp
                            )
                        }

                        Text(
                            text = "%$mPercent",
                            color = barColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Button(
                        onClick = { onOpenMonthInCalendar(monthNum) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPurple,
                            contentColor = Color(0xFF0D0B1E)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$monthFullName Ayını Takvimde Gör",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Inline Card revealed below calendar showing selected day's routines, status, and inline editable notes.
 */
@Composable
fun InlineDayDetailCard(
    date: LocalDate,
    viewModel: RutinViewModel,
    uiState: RutinUiState,
    locale: Locale,
    onClose: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val daysDiff = ChronoUnit.DAYS.between(date, today)
    // User can change status (done <-> missed) for the last 7 days. Older days are view-only.
    val isEditable = daysDiff in 0..7

    val dayRoutines = viewModel.getRoutinesForDay(date)
    val formattedDateTitle = remember(date) {
        val day = date.dayOfMonth
        val month = date.format(DateTimeFormatter.ofPattern("MMMM", locale))
        val dayOfWeek = date.format(DateTimeFormatter.ofPattern("EEEE", locale))
        "$day $month $dayOfWeek"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xF2162035)) // Solid glass effect
            .border(1.5.dp, AccentPurple.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formattedDateTitle,
                        color = PureWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isEditable) "Düzenlenebilir (Son 7 gün)" else "Sadece Görüntüleme",
                        color = if (isEditable) AccentTeal else MutedWhite,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Kapat",
                        tint = PureWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (dayRoutines.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(GlassCardBackground)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Bu gün için planlanmış rutin bulunmuyor.",
                        color = MutedWhite,
                        fontSize = 13.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    dayRoutines.forEach { routine ->
                        InlineRoutineItem(
                            routine = routine,
                            date = date,
                            viewModel = viewModel,
                            uiState = uiState,
                            isEditable = isEditable
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineRoutineItem(
    routine: Routine,
    date: LocalDate,
    viewModel: RutinViewModel,
    uiState: RutinUiState,
    isEditable: Boolean
) {
    val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    val logKey = "${dateString}_${routine.id}"
    val log = uiState.logs[logKey]
    val status = viewModel.getRoutineStatus(routine, date)

    val isDone = log?.isDone == true
    val isMissed = log != null && !log.isDone

    var inlineNote by remember(log?.note) { mutableStateOf(log?.note.orEmpty()) }

    val statusColor = when (status) {
        RoutineStatus.DONE -> DoneGreen
        RoutineStatus.MISSED -> MissedRed
        RoutineStatus.ACTIVE -> AccentTeal
        RoutineStatus.UPCOMING -> AccentPurple
    }

    val cardBg by animateColorAsState(
        targetValue = when {
            isDone -> DoneCardBg
            isMissed -> MissedCardBg
            else -> GlassCardBackground
        },
        animationSpec = tween(300),
        label = "inlineCardBg"
    )

    val cardBorder by animateColorAsState(
        targetValue = when {
            isDone -> DoneCardBorder
            isMissed -> MissedCardBorder
            else -> GlassCardBorderSubtle
        },
        animationSpec = tween(300),
        label = "inlineCardBorder"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    RoutineIllustration(
                        illustrationId = routine.illustrationId,
                        size = 40.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = routine.name,
                            color = if (isDone) DoneGreen else if (isMissed) MissedRed else PureWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = SubtitleWhite,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = routine.time,
                                color = SubtitleWhite,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "•",
                                color = MutedWhite,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = routine.category,
                                color = AccentPurple,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Status Toggle Button (only for last 7 days)
                if (isEditable) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(statusColor.copy(alpha = 0.2f))
                            .border(1.dp, statusColor, RoundedCornerShape(999.dp))
                            .clickable { viewModel.toggleRoutineStatus(routine.id, date) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (status) {
                                    RoutineStatus.DONE -> Icons.Rounded.Check
                                    RoutineStatus.MISSED -> Icons.Rounded.Close
                                    else -> Icons.Rounded.RadioButtonUnchecked
                                },
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (status) {
                                    RoutineStatus.DONE -> "Tamamlandı"
                                    RoutineStatus.MISSED -> "Kaçırıldı"
                                    else -> "Bekliyor"
                                },
                                color = statusColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Older days are view-only
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Kilitli",
                            tint = MutedWhite,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (status) {
                                RoutineStatus.DONE -> "Tamamlandı"
                                RoutineStatus.MISSED -> "Kaçırıldı"
                                else -> "Kayıt yok"
                            },
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Inline Editable Note Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x330A101D))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EditNote,
                        contentDescription = null,
                        tint = AccentTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (isEditable) {
                        BasicTextField(
                            value = inlineNote,
                            onValueChange = { newText ->
                                inlineNote = newText
                                viewModel.updateRoutineNote(routine.id, date, newText)
                            },
                            textStyle = TextStyle(
                                color = PureWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(AccentTeal),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (inlineNote.isEmpty()) {
                                    Text(
                                        text = "Not veya detay ekleyin...",
                                        color = MutedWhite.copy(alpha = 0.6f),
                                        fontSize = 12.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                    } else {
                        Text(
                            text = if (inlineNote.isNotEmpty()) inlineNote else "Bu güne ait not bulunmuyor.",
                            color = if (inlineNote.isNotEmpty()) PureWhite else MutedWhite.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticsStreakOverviewCard(
    stats: UserStats,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xE6131D31))
            .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Streak Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x33F59E0B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🔥", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${stats.currentStreak} Günlük Seri",
                            color = PureWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (stats.currentStreak > 0) "Harika gidiyorsun! Ritmi koru." else "Bugün bir rutin tamamla ve seri başlat!",
                            color = SubtitleWhite,
                            fontSize = 11.sp
                        )
                    }
                }

                if (stats.bestStreak > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0x22FBBF24))
                            .border(1.dp, Color(0x44FBBF24), RoundedCornerShape(999.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "⭐ En İyi: ${stats.bestStreak}g",
                            color = Color(0xFFFBBF24),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Divider line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0x1AFFFFFF))
            )

            // 4 Completion Metrics: Bugün, Hafta, Ay, Yıl
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricTile(
                    title = "Bugün",
                    value = "${stats.todayCompleted}/${stats.todayTotal}",
                    percent = stats.todayPercent,
                    color = getCompletionColor(stats.todayPercent, stats.todayTotal),
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = "Bu Hafta",
                    value = "${stats.weeklyCompleted}/${stats.weeklyTotal}",
                    percent = stats.weeklyPercent,
                    color = getCompletionColor(stats.weeklyPercent, stats.weeklyTotal),
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = "Bu Ay",
                    value = "${stats.monthlyCompleted}/${stats.monthlyTotal}",
                    percent = stats.monthlyPercent,
                    color = getCompletionColor(stats.monthlyPercent, stats.monthlyTotal),
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = "Bu Yıl",
                    value = "${stats.yearlyCompleted}/${stats.yearlyTotal}",
                    percent = stats.yearlyPercent,
                    color = getCompletionColor(stats.yearlyPercent, stats.yearlyTotal),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    percent: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x1F1C2B42))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                color = SubtitleWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "%$percent",
                color = color,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = value,
                color = MutedWhite,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x22FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percent.coerceIn(0, 100) / 100f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(color)
                )
            }
        }
    }
}
