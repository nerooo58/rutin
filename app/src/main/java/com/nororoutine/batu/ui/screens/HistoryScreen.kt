package com.nororoutine.batu.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nororoutine.batu.model.Routine
import com.nororoutine.batu.ui.components.DayDetailBottomSheet
import com.nororoutine.batu.ui.components.RoutineDetailBottomSheet
import com.nororoutine.batu.ui.components.springPress
import com.nororoutine.batu.ui.theme.AccentPurple
import com.nororoutine.batu.ui.theme.AccentTeal
import com.nororoutine.batu.ui.theme.DoneGreen
import com.nororoutine.batu.ui.theme.GlassCardBackground
import com.nororoutine.batu.ui.theme.GlassCardBorderSubtle
import com.nororoutine.batu.ui.theme.MissedRed
import com.nororoutine.batu.ui.theme.MutedWhite
import com.nororoutine.batu.ui.theme.PureWhite
import com.nororoutine.batu.ui.theme.SubtitleWhite
import com.nororoutine.batu.viewmodel.RutinUiState
import com.nororoutine.batu.viewmodel.RutinViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class HistoryViewMode {
    WEEK,
    MONTH
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
    var selectedDayForDetail by remember { mutableStateOf<LocalDate?>(null) }
    var routineForNoteSheet by remember { mutableStateOf<Routine?>(null) }

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

                Spacer(modifier = Modifier.height(18.dp))

                // Segmented control: Hafta / Ay
                SegmentedControl(
                    selectedMode = viewMode,
                    onSelect = { viewMode = it }
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // View Mode Content
            item {
                when (viewMode) {
                    HistoryViewMode.WEEK -> {
                        WeekHistoryView(
                            anchorDate = currentWeekAnchor,
                            viewModel = viewModel,
                            uiState = uiState,
                            locale = turkishLocale,
                            onPrevWeek = { currentWeekAnchor = currentWeekAnchor.minusWeeks(1) },
                            onNextWeek = { currentWeekAnchor = currentWeekAnchor.plusWeeks(1) },
                            onDayClick = { selectedDayForDetail = it }
                        )
                    }
                    HistoryViewMode.MONTH -> {
                        MonthHistoryView(
                            currentMonth = currentMonthAnchor,
                            viewModel = viewModel,
                            uiState = uiState,
                            locale = turkishLocale,
                            onPrevMonth = { currentMonthAnchor = currentMonthAnchor.minusMonths(1) },
                            onNextMonth = { currentMonthAnchor = currentMonthAnchor.plusMonths(1) },
                            onDayClick = { selectedDayForDetail = it }
                        )
                    }
                }
            }
        }

        // Day Detail Bottom Sheet
        selectedDayForDetail?.let { day ->
            DayDetailBottomSheet(
                date = day,
                viewModel = viewModel,
                uiState = uiState,
                onDismiss = { selectedDayForDetail = null },
                onOpenRoutineNote = { routine ->
                    routineForNoteSheet = routine
                }
            )
        }

        // Note Sheet opened from Day Detail
        routineForNoteSheet?.let { routine ->
            val day = selectedDayForDetail ?: LocalDate.now()
            val dateKey = "${day.format(DateTimeFormatter.ISO_LOCAL_DATE)}_${routine.id}"
            val status = viewModel.getRoutineStatus(routine, day)
            val note = uiState.logs[dateKey]?.note.orEmpty()

            RoutineDetailBottomSheet(
                routine = routine,
                status = status,
                initialNote = note,
                date = day,
                onDismiss = { routineForNoteSheet = null },
                onToggleStatus = {
                    viewModel.toggleRoutineStatus(routine.id, day)
                },
                onSaveNote = { newNote ->
                    viewModel.updateRoutineNote(routine.id, day, newNote)
                }
            )
        }
    }
}

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
            // Hafta
            val isWeek = selectedMode == HistoryViewMode.WEEK
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isWeek) AccentPurple else Color.Transparent)
                    .clickable { onSelect(HistoryViewMode.WEEK) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Hafta",
                    color = if (isWeek) Color(0xFF0D0B1E) else SubtitleWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // Ay
            val isMonth = selectedMode == HistoryViewMode.MONTH
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isMonth) AccentPurple else Color.Transparent)
                    .clickable { onSelect(HistoryViewMode.MONTH) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ay",
                    color = if (isMonth) Color(0xFF0D0B1E) else SubtitleWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun WeekHistoryView(
    anchorDate: LocalDate,
    viewModel: RutinViewModel,
    uiState: RutinUiState,
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

    Column(modifier = Modifier.fillMaxWidth()) {
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

        Spacer(modifier = Modifier.height(16.dp))

        // 7 Column Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val dayNames = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
            for (i in 0..6) {
                val dayDate = monday.plusDays(i.toLong())
                val dayRoutines = viewModel.getRoutinesForDay(dayDate)
                val isToday = dayDate == LocalDate.now()

                val doneCount = dayRoutines.count {
                    val key = "${dayDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}_${it.id}"
                    uiState.logs[key]?.isDone == true
                }
                val totalCount = dayRoutines.size

                val completionRatio = if (totalCount > 0) doneCount.toFloat() / totalCount else 0f

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isToday) Color(0x33A78BFA) else GlassCardBackground)
                        .border(
                            1.dp,
                            if (isToday) AccentPurple else GlassCardBorderSubtle,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onDayClick(dayDate) }
                        .padding(vertical = 12.dp, horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = dayNames[i],
                            color = if (isToday) AccentTeal else SubtitleWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${dayDate.dayOfMonth}",
                            color = PureWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Completion indicator dots
                        if (totalCount == 0) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x22FFFFFF))
                            )
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                dayRoutines.take(3).forEach { routine ->
                                    val key = "${dayDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}_${routine.id}"
                                    val log = uiState.logs[key]
                                    val dotColor = when {
                                        log?.isDone == true -> DoneGreen
                                        log != null && !log.isDone -> MissedRed
                                        else -> Color(0x44FFFFFF)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Completion score percentage / ratio
                        Text(
                            text = if (totalCount > 0) "$doneCount/$totalCount" else "-",
                            color = if (completionRatio >= 1f && totalCount > 0) DoneGreen else MutedWhite,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Week Summary Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(GlassCardBackground)
                .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Haftalık Özet",
                    color = PureWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Herhangi bir güne dokunarak o günün rutin detaylarını görüntüleyebilir, son 7 günün durumunu güncelleyebilirsiniz.",
                    color = SubtitleWhite,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun MonthHistoryView(
    currentMonth: YearMonth,
    viewModel: RutinViewModel,
    uiState: RutinUiState,
    locale: Locale,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit
) {
    val monthTitle = remember(currentMonth) {
        val monthName = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
        monthName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
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

            Text(
                text = monthTitle,
                color = PureWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

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

        Spacer(modifier = Modifier.height(16.dp))

        // Month Grid Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(GlassCardBackground)
                .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Day of week headers
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

                // Days of month
                val firstDayOfMonth = currentMonth.atDay(1)
                val daysInMonth = currentMonth.lengthOfMonth()
                val startDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 = Monday, 7 = Sunday
                val emptySlotsBefore = startDayOfWeek - 1

                val totalCells = ((emptySlotsBefore + daysInMonth + 6) / 7) * 7

                for (week in 0 until (totalCells / 7)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (dayCol in 0..6) {
                            val cellIndex = week * 7 + dayCol
                            val dayNumber = cellIndex - emptySlotsBefore + 1

                            if (dayNumber in 1..daysInMonth) {
                                val cellDate = currentMonth.atDay(dayNumber)
                                val dayRoutines = viewModel.getRoutinesForDay(cellDate)
                                val isToday = cellDate == LocalDate.now()

                                val doneCount = dayRoutines.count {
                                    val key = "${cellDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}_${it.id}"
                                    uiState.logs[key]?.isDone == true
                                }
                                val totalCount = dayRoutines.size

                                val dotColor = when {
                                    totalCount == 0 -> Color.Transparent
                                    doneCount == totalCount -> DoneGreen
                                    doneCount > 0 -> AccentTeal
                                    else -> MissedRed.copy(alpha = 0.5f)
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isToday) Color(0x33A78BFA) else Color.Transparent)
                                        .border(
                                            1.dp,
                                            if (isToday) AccentPurple else Color.Transparent,
                                            RoundedCornerShape(8.dp)
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
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (totalCount > 0) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(dotColor)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
