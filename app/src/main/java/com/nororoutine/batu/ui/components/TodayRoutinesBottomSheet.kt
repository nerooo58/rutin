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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nororoutine.batu.model.Routine
import com.nororoutine.batu.model.RoutineStatus
import com.nororoutine.batu.ui.illustrations.RoutineIllustration
import com.nororoutine.batu.ui.theme.AccentPurple
import com.nororoutine.batu.ui.theme.AccentTeal
import com.nororoutine.batu.ui.theme.DoneGreen
import com.nororoutine.batu.ui.theme.GlassCardBackground
import com.nororoutine.batu.ui.theme.GlassCardBorderSubtle
import com.nororoutine.batu.ui.theme.MissedRed
import com.nororoutine.batu.ui.theme.MutedWhite
import com.nororoutine.batu.ui.theme.PureWhite
import com.nororoutine.batu.ui.theme.SubtitleWhite
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayRoutinesBottomSheet(
    routines: List<Routine>,
    today: LocalDate,
    getStatus: (Routine) -> RoutineStatus,
    onToggleStatus: (Routine) -> Unit,
    onOpenNote: (Routine) -> Unit,
    onNavigateToEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF13192B),
        scrimColor = Color(0xCC000000),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color(0x44FFFFFF))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Günün Rutinleri",
                        color = PureWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${routines.count { getStatus(it) == RoutineStatus.DONE }}/${routines.size} tamamlandı",
                        color = AccentTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(
                    onClick = {
                        onDismiss()
                        onNavigateToEdit()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Rutin Ekle",
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (routines.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Bugün için henüz rutin eklenmemiş.",
                        color = SubtitleWhite,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(routines, key = { it.id }) { routine ->
                        val status = getStatus(routine)
                        TodayRoutineSheetItem(
                            routine = routine,
                            status = status,
                            onToggleStatus = { onToggleStatus(routine) },
                            onOpenNote = { onOpenNote(routine) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayRoutineSheetItem(
    routine: Routine,
    status: RoutineStatus,
    onToggleStatus: () -> Unit,
    onOpenNote: () -> Unit
) {
    val itemBg by animateColorAsState(
        targetValue = when (status) {
            RoutineStatus.DONE -> Color(0x264ADE80)
            RoutineStatus.ACTIVE -> Color(0x33A78BFA)
            RoutineStatus.UPCOMING -> Color(0x221E3250)
            else -> GlassCardBackground
        },
        animationSpec = tween(250),
        label = "sheetItemBg"
    )

    val itemBorder by animateColorAsState(
        targetValue = when (status) {
            RoutineStatus.DONE -> Color(0x4D4ADE80)
            RoutineStatus.ACTIVE -> AccentPurple
            RoutineStatus.UPCOMING -> GlassCardBorderSubtle
            else -> GlassCardBorderSubtle
        },
        animationSpec = tween(250),
        label = "sheetItemBorder"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(itemBg)
            .border(1.dp, itemBorder, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Routine illustration icon
        RoutineIllustration(
            illustrationId = routine.illustrationId,
            size = 46.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Time
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = routine.name,
                color = when (status) {
                    RoutineStatus.DONE -> DoneGreen
                    else -> PureWhite
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = SubtitleWhite,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = routine.time,
                    color = SubtitleWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "• ${routine.category}",
                    color = MutedWhite,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Note button
        IconButton(
            onClick = onOpenNote,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0x22FFFFFF))
        ) {
            Icon(
                imageVector = Icons.Rounded.EditNote,
                contentDescription = "Not Ekle / Oku",
                tint = AccentTeal,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Mark Done Toggle Button
        IconButton(
            onClick = onToggleStatus,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (status == RoutineStatus.DONE) DoneGreen else Color(0x22FFFFFF)
                )
        ) {
            Icon(
                imageVector = if (status == RoutineStatus.DONE) Icons.Rounded.Check else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = "Durum Değiştir",
                tint = if (status == RoutineStatus.DONE) Color(0xFF0F172A) else PureWhite,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
