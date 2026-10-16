package com.nororoutine.batu.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nororoutine.batu.model.Routine
import com.nororoutine.batu.model.RoutineLog
import com.nororoutine.batu.ui.illustrations.RoutineIllustration
import com.nororoutine.batu.ui.theme.AccentPurple
import com.nororoutine.batu.ui.theme.AccentTeal
import com.nororoutine.batu.ui.theme.DoneGreen
import com.nororoutine.batu.ui.theme.GlassCardBorderSubtle
import com.nororoutine.batu.ui.theme.MutedWhite
import com.nororoutine.batu.ui.theme.PureWhite
import com.nororoutine.batu.ui.theme.SubtitleWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineInfoMenuBottomSheet(
    routine: Routine,
    todayLog: RoutineLog?,
    onDismiss: () -> Unit,
    onSelectFocusMode: () -> Unit,
    onSelectSubtasks: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val completedSubtasksCount = todayLog?.completedSubtasks?.size ?: 0
    val totalSubtasksCount = routine.subtasks.size

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF121024),
        scrimColor = Color(0x99000000),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            // Header: Title & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoutineIllustration(
                        illustrationId = routine.illustrationId,
                        size = 38.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = routine.name,
                            color = PureWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = routine.time,
                                color = SubtitleWhite,
                                fontSize = 12.sp
                            )
                            if (routine.isRange) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• ⏳ ${routine.durationMinutes} dk Aralık",
                                    color = AccentTeal,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• ⏱️ Sabit Saat",
                                    color = AccentPurple,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Kapat",
                        tint = SubtitleWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "RUTİN SEÇENEKLERİ",
                color = MutedWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Option 1: Odak Modu (for Range) or Süre Tutucu (for Single-time)
            if (routine.isRange) {
                InfoMenuOptionCard(
                    icon = Icons.Rounded.SelfImprovement,
                    iconTint = AccentTeal,
                    title = "1. Odak Modu",
                    subtitle = "${routine.durationMinutes} dakikalık aralıklı odak seansı başlat",
                    badgeText = "Aralıklı Rutin",
                    badgeColor = AccentTeal,
                    onClick = {
                        onDismiss()
                        onSelectFocusMode()
                    }
                )
            } else {
                InfoMenuOptionCard(
                    icon = Icons.Rounded.Timer,
                    iconTint = AccentPurple,
                    title = "1. Süre Tutma (Kronometre)",
                    subtitle = "Rutin üzerinde çalıştığın süreyi kronometre ile kaydet",
                    badgeText = "Süre Tutma",
                    badgeColor = AccentPurple,
                    onClick = {
                        onDismiss()
                        onSelectFocusMode()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Option 2: Alt Adımlar (Checklist)
            val subtasksBadge = if (totalSubtasksCount > 0) {
                "$completedSubtasksCount / $totalSubtasksCount"
            } else {
                "Yeni Ekle"
            }
            InfoMenuOptionCard(
                icon = Icons.Rounded.Checklist,
                iconTint = DoneGreen,
                title = "2. Alt Adımlar",
                subtitle = if (totalSubtasksCount > 0) {
                    "$totalSubtasksCount adım tanımlı • Kontrol listesini tamamla veya düzenle"
                } else {
                    "Bu rutine parçalayabileceğin alt adımlar ve görevler ekle"
                },
                badgeText = subtasksBadge,
                badgeColor = if (totalSubtasksCount > 0 && completedSubtasksCount == totalSubtasksCount) DoneGreen else AccentPurple,
                onClick = {
                    onDismiss()
                    onSelectSubtasks()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun InfoMenuOptionCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x661E3250))
            .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(16.dp))
            .springPress(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = 0.15f))
                    .border(1.dp, iconTint.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        color = PureWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(badgeColor.copy(alpha = 0.2f))
                            .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = subtitle,
                    color = SubtitleWhite,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
