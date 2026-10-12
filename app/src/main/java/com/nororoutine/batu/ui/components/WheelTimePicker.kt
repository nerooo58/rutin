package com.nororoutine.batu.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nororoutine.batu.ui.theme.AccentPurple
import com.nororoutine.batu.ui.theme.AccentTeal
import com.nororoutine.batu.ui.theme.GlassCardBorderSubtle
import com.nororoutine.batu.ui.theme.MutedWhite
import com.nororoutine.batu.ui.theme.PureWhite
import com.nororoutine.batu.ui.theme.SubtitleWhite
import kotlinx.coroutines.launch

private const val WHEEL_REPEAT_COUNT = 1000
private val ITEM_HEIGHT: Dp = 44.dp
private val PICKER_HEIGHT: Dp = 132.dp

enum class TimePickerMode(val label: String) {
    SINGLE("Tek Saat"),
    RANGE("Aralık")
}

@Composable
fun TimeRangeWheelPicker(
    initialIsRange: Boolean = false,
    initialStartTime: String = "08:00",
    initialEndTime: String = "09:00",
    onTimeConfigChange: (isRange: Boolean, startTime: String, endTime: String?, displayTime: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember(initialIsRange) {
        mutableStateOf(if (initialIsRange) TimePickerMode.RANGE else TimePickerMode.SINGLE)
    }

    var startTime by remember(initialStartTime) { mutableStateOf(initialStartTime) }
    var endTime by remember(initialEndTime) { mutableStateOf(initialEndTime) }

    LaunchedEffect(mode, startTime, endTime) {
        val isRange = mode == TimePickerMode.RANGE
        val display = if (isRange) "$startTime – $endTime" else startTime
        onTimeConfigChange(isRange, startTime, if (isRange) endTime else null, display)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Mode Selector: Tek Saat / Aralık
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0x33101C30))
                .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(999.dp))
                .padding(3.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Tek Saat
                val isSingle = mode == TimePickerMode.SINGLE
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isSingle) AccentPurple else Color.Transparent)
                        .clickable { mode = TimePickerMode.SINGLE },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tek Saat",
                        color = if (isSingle) Color(0xFF0D0B1E) else SubtitleWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Aralık
                val isRange = mode == TimePickerMode.RANGE
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isRange) AccentPurple else Color.Transparent)
                        .clickable { mode = TimePickerMode.RANGE },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aralık",
                        color = if (isRange) Color(0xFF0D0B1E) else SubtitleWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Mode explanation subtitle
        Text(
            text = if (mode == TimePickerMode.SINGLE) {
                "Bildirim tam bu saatte ($startTime) iletilir."
            } else {
                "Bildirim başlangıç saatinde ($startTime) iletilir."
            },
            color = MutedWhite,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 2.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Pickers container
        AnimatedContent(
            targetState = mode,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "timePickerMode"
        ) { currentMode ->
            when (currentMode) {
                TimePickerMode.SINGLE -> {
                    SingleTimeDrumPicker(
                        time = startTime,
                        onTimeChange = { startTime = it }
                    )
                }
                TimePickerMode.RANGE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Başlangıç Picker
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Başlangıç: $startTime",
                                color = AccentTeal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            SingleTimeDrumPicker(
                                time = startTime,
                                onTimeChange = { startTime = it },
                                compact = true
                            )
                        }

                        // Bitiş Picker
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bitiş: $endTime",
                                color = AccentPurple,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            SingleTimeDrumPicker(
                                time = endTime,
                                onTimeChange = { endTime = it },
                                compact = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SingleTimeDrumPicker(
    time: String,
    onTimeChange: (String) -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val initialHour = remember(time) {
        try { time.split(":")[0].toInt().coerceIn(0, 23) } catch (e: Exception) { 8 }
    }
    val initialMinute = remember(time) {
        try { time.split(":")[1].toInt().coerceIn(0, 59) } catch (e: Exception) { 0 }
    }

    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PICKER_HEIGHT)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x33101C30))
            .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Highlight Ribbon
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ITEM_HEIGHT)
                .padding(horizontal = if (compact) 6.dp else 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x267CF4DE))
                .border(1.dp, Color(0x4D7CF4DE), RoundedCornerShape(12.dp))
        )

        // Drum Roll Columns
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelDrumColumn(
                itemsCount = 24,
                initialValue = initialHour,
                compact = compact,
                onValueSelected = { hour ->
                    selectedHour = hour
                    onTimeChange(String.format("%02d:%02d", selectedHour, selectedMinute))
                }
            )

            Text(
                text = ":",
                color = AccentTeal,
                fontSize = if (compact) 18.sp else 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = if (compact) 4.dp else 12.dp)
            )

            WheelDrumColumn(
                itemsCount = 60,
                initialValue = initialMinute,
                compact = compact,
                onValueSelected = { minute ->
                    selectedMinute = minute
                    onTimeChange(String.format("%02d:%02d", selectedHour, selectedMinute))
                }
            )
        }

        // 3D Shadows
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(listOf(Color(0xEE0D0B1E), Color.Transparent))
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE0D0B1E)))
                )
        )
    }
}

@Composable
private fun WheelDrumColumn(
    itemsCount: Int,
    initialValue: Int,
    compact: Boolean = false,
    onValueSelected: (Int) -> Unit
) {
    val totalItems = itemsCount * WHEEL_REPEAT_COUNT
    val startIndex = (WHEEL_REPEAT_COUNT / 2) * itemsCount + initialValue
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val itemHeightPx = with(density) { ITEM_HEIGHT.toPx() }

    val centeredIndex by remember {
        derivedStateOf {
            val offset = listState.firstVisibleItemScrollOffset
            if (offset > itemHeightPx / 2) {
                listState.firstVisibleItemIndex + 1
            } else {
                listState.firstVisibleItemIndex
            }
        }
    }

    val selectedValue by remember {
        derivedStateOf {
            (centeredIndex % itemsCount + itemsCount) % itemsCount
        }
    }

    LaunchedEffect(selectedValue) {
        onValueSelected(selectedValue)
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(vertical = ITEM_HEIGHT),
        modifier = Modifier
            .width(if (compact) 48.dp else 68.dp)
            .height(PICKER_HEIGHT)
    ) {
        items(
            count = totalItems,
            key = { index -> index }
        ) { index ->
            val value = index % itemsCount
            val isCentered = index == centeredIndex

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ITEM_HEIGHT)
                    .clickable {
                        scope.launch {
                            listState.animateScrollToItem(index)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = String.format("%02d", value),
                    color = if (isCentered) PureWhite else SubtitleWhite.copy(alpha = 0.35f),
                    fontSize = if (isCentered) (if (compact) 18.sp else 22.sp) else (if (compact) 13.sp else 16.sp),
                    fontWeight = if (isCentered) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
