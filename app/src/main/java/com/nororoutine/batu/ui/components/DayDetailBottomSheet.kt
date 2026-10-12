package com.nororoutine.batu.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nororoutine.batu.model.Routine
import com.nororoutine.batu.model.RoutineStatus
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DayDetailBottomSheet(
    date: LocalDate,
    viewModel: RutinViewModel,
    uiState: RutinUiState,
    onDismiss: () -> Unit,
    onOpenRoutineNote: (Routine) -> Unit
) {
    val turkishLocale = remember { Locale("tr", "TR") }
    val formattedDateTitle = remember(date) {
        val day = date.dayOfMonth
        val month = date.format(DateTimeFormatter.ofPattern("MMMM", turkishLocale))
        val dayOfWeek = date.format(DateTimeFormatter.ofPattern("EEEE", turkishLocale))
        "$day $month $dayOfWeek"
    }

    val today = remember { LocalDate.now() }
    val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(date, today)
    // "last 7 days editable"
    val isEditable = daysDiff in 0..7

    val dayRoutines = viewModel.getRoutinesForDay(date)
    val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Color(0xFF13112A))
            .border(1.dp, GlassCardBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x33FFFFFF))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                    onClick = onDismiss,
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
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(dayRoutines, key = { it.id }) { routine ->
                        val logKey = "${dateString}_${routine.id}"
                        val log = uiState.logs[logKey]
                        val isDone = log?.isDone == true
                        val isMissed = log != null && !log.isDone

                        val cardBg by animateColorAsState(
                            targetValue = when {
                                isDone -> DoneCardBg
                                isMissed -> MissedCardBg
                                else -> GlassCardBackground
                            },
                            animationSpec = tween(300),
                            label = "cardBg"
                        )

                        val cardBorder by animateColorAsState(
                            targetValue = when {
                                isDone -> DoneCardBorder
                                isMissed -> MissedCardBorder
                                else -> GlassCardBorderSubtle
                            },
                            animationSpec = tween(300),
                            label = "cardBorder"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(cardBg)
                                .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                                .clickable {
                                    if (isEditable) {
                                        onOpenRoutineNote(routine)
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    com.nororoutine.batu.ui.illustrations.RoutineIllustration(
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
                                            Text(
                                                text = "${routine.time} • ${routine.category}",
                                                color = SubtitleWhite,
                                                fontSize = 12.sp
                                            )
                                            if (!log?.note.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Rounded.EditNote,
                                                    contentDescription = null,
                                                    tint = AccentTeal,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (isEditable) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isDone -> DoneGreen.copy(alpha = 0.2f)
                                                    isMissed -> MissedRed.copy(alpha = 0.2f)
                                                    else -> Color(0x22FFFFFF)
                                                }
                                            )
                                            .springPress {
                                                viewModel.toggleRoutineStatus(routine.id, date)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when {
                                            isDone -> Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = "Tamamlandı",
                                                tint = DoneGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            isMissed -> Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Yapılamadı",
                                                tint = MissedRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            else -> Icon(
                                                imageVector = Icons.Rounded.RadioButtonUnchecked,
                                                contentDescription = "Durum Değiştir",
                                                tint = SubtitleWhite,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = "Kilitli",
                                        tint = Color(0x44FFFFFF),
                                        modifier = Modifier.size(16.dp)
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
