package com.nororoutine.batu.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PostAdd
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.nororoutine.batu.ui.components.DAY_LABELS
import com.nororoutine.batu.ui.components.GlassCard
import com.nororoutine.batu.ui.components.RoutineFormBottomSheet
import com.nororoutine.batu.ui.components.springPress
import com.nororoutine.batu.ui.theme.AccentPurple
import com.nororoutine.batu.ui.theme.AccentTeal
import com.nororoutine.batu.ui.theme.GlassCardBackground
import com.nororoutine.batu.ui.theme.GlassCardBorder
import com.nororoutine.batu.ui.theme.GlassCardBorderSubtle
import com.nororoutine.batu.ui.theme.MissedRed
import com.nororoutine.batu.ui.theme.MutedWhite
import com.nororoutine.batu.ui.theme.PureWhite
import com.nororoutine.batu.ui.theme.SubtitleWhite
import com.nororoutine.batu.viewmodel.RutinUiState
import com.nororoutine.batu.viewmodel.RutinViewModel

@Composable
fun EditScreen(
    viewModel: RutinViewModel,
    uiState: RutinUiState,
    modifier: Modifier = Modifier
) {
    var isAddSheetOpen by remember { mutableStateOf(false) }
    var routineToEdit by remember { mutableStateOf<Routine?>(null) }
    var routineToDelete by remember { mutableStateOf<Routine?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            // Header Row: Title + Add Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Düzenle",
                            color = PureWhite,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tüm rutinleriniz ve akış ayarlarınız",
                            color = MutedWhite,
                            fontSize = 13.sp
                        )
                    }

                    // Top-right Add Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(AccentPurple)
                            .springPress {
                                routineToEdit = null
                                isAddSheetOpen = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Yeni Rutin Ekle",
                            tint = PureWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Routines list (empty by default)
            if (uiState.routines.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(GlassCardBackground)
                            .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(20.dp))
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x1FA78BFA))
                                    .border(1.dp, Color(0x33A78BFA), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PostAdd,
                                    contentDescription = null,
                                    tint = AccentPurple,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = "Rutin listeniz boş",
                                color = PureWhite,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Yukarıdaki '+' butonuna dokunarak şablonlardan seçebilir veya kendi rutininizi oluşturabilirsiniz.",
                                color = MutedWhite,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    routineToEdit = null
                                    isAddSheetOpen = true
                                },
                                modifier = Modifier.springPress {
                                    routineToEdit = null
                                    isAddSheetOpen = true
                                },
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentPurple,
                                    contentColor = PureWhite
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Rutin Ekle",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = uiState.routines,
                    key = { _, item -> item.id }
                ) { index, routine ->
                    EditableRoutineCard(
                        routine = routine,
                        isFirst = index == 0,
                        isLast = index == uiState.routines.lastIndex,
                        onMoveUp = { viewModel.moveRoutineUp(index) },
                        onMoveDown = { viewModel.moveRoutineDown(index) },
                        onEdit = {
                            routineToEdit = routine
                            isAddSheetOpen = true
                        },
                        onDelete = {
                            routineToDelete = routine
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // Add/Edit Bottom Sheet
        if (isAddSheetOpen) {
            RoutineFormBottomSheet(
                routineToEdit = routineToEdit,
                availableCategories = uiState.categories,
                onAddNewCategory = { newCategory ->
                    viewModel.addCategory(newCategory)
                },
                onDismiss = {
                    isAddSheetOpen = false
                    routineToEdit = null
                },
                onSave = { name, time, isRange, startTime, endTime, days, category, illustrationId ->
                    val editing = routineToEdit
                    if (editing != null) {
                        viewModel.updateRoutine(
                            id = editing.id,
                            name = name,
                            time = time,
                            isRange = isRange,
                            startTime = startTime,
                            endTime = endTime,
                            days = days,
                            category = category,
                            illustrationId = illustrationId
                        )
                    } else {
                        viewModel.addRoutine(
                            name = name,
                            time = time,
                            isRange = isRange,
                            startTime = startTime,
                            endTime = endTime,
                            days = days,
                            category = category,
                            illustrationId = illustrationId
                        )
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        routineToDelete?.let { routine ->
            AlertDialog(
                onDismissRequest = { routineToDelete = null },
                containerColor = Color(0xFF16132C),
                titleContentColor = PureWhite,
                textContentColor = SubtitleWhite,
                title = {
                    Text(text = "Rutini Sil", fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(text = "'${routine.name}' rutinini silmek istediğinize emin misiniz? Bu işlem geri alınamaz.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteRoutine(routine.id)
                            routineToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MissedRed)
                    ) {
                        Text(text = "Sil", color = PureWhite, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { routineToDelete = null }) {
                        Text(text = "İptal", color = PureWhite)
                    }
                }
            )
        }
    }
}

@Composable
fun EditableRoutineCard(
    routine: Routine,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassCardBackground)
            .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(16.dp))
            .clickable { onEdit() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Reorder up/down handles
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowUpward,
                        contentDescription = "Yukarı Taşı",
                        tint = if (!isFirst) AccentTeal else Color(0x33FFFFFF),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowDownward,
                        contentDescription = "Aşağı Taşı",
                        tint = if (!isLast) AccentTeal else Color(0x33FFFFFF),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            com.nororoutine.batu.ui.illustrations.RoutineIllustration(
                illustrationId = routine.illustrationId,
                size = 44.dp
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Center: Routine Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = routine.name,
                        color = PureWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0x1FA78BFA))
                            .border(1.dp, Color(0x33A78BFA), RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = routine.category,
                            color = AccentPurple,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

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
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Days indicator pills
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        DAY_LABELS.forEach { (dayIndex, label) ->
                            val isDaySelected = routine.days.contains(label)
                            Text(
                                text = label,
                                color = if (isDaySelected) AccentTeal else Color(0x33FFFFFF),
                                fontSize = 10.sp,
                                fontWeight = if (isDaySelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Right: Edit & Delete buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Düzenle",
                        tint = SubtitleWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Sil",
                        tint = MissedRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
