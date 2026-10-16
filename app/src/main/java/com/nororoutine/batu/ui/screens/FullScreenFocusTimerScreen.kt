package com.nororoutine.batu.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nororoutine.batu.data.PersistentFocusManager
import com.nororoutine.batu.model.Routine
import com.nororoutine.batu.model.RoutineLog
import com.nororoutine.batu.ui.theme.AccentPurple
import com.nororoutine.batu.ui.theme.AccentTeal
import com.nororoutine.batu.ui.theme.DoneGreen
import com.nororoutine.batu.ui.theme.GlassCardBackground
import com.nororoutine.batu.ui.theme.GlassCardBorderSubtle
import com.nororoutine.batu.ui.theme.MutedWhite
import com.nororoutine.batu.ui.theme.PureWhite
import com.nororoutine.batu.ui.theme.SubtitleWhite
import com.nororoutine.batu.viewmodel.RutinUiState
import com.nororoutine.batu.viewmodel.RutinViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * TAM EKRAN (Full Screen) Süre Tutma & Odak Modu:
 * - Uygulama kapalı, arka planda veya telefon kilitli olsa bile süre tutmaya kesintisiz devam eder.
 * - Zaman damgası (Timestamp) tabanlı hesaplama ile cihaz yeniden başlatılsa dahi süreyi kaybetmez.
 * - Kaydedilen seanslar altta liste halinde görüntülenir.
 * - Manuel olarak süre ve seans açıklaması/notu eklenebilir.
 * - Tüm kayıtlar doğrudan Firestore ve yerel önbelleğe senkronize edilir.
 */
@Composable
fun FullScreenFocusTimerScreen(
    routine: Routine,
    today: LocalDate = LocalDate.now(),
    uiState: RutinUiState,
    viewModel: RutinViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateKey = "${today.format(DateTimeFormatter.ISO_LOCAL_DATE)}_${routine.id}"
    val currentLog = uiState.logs[dateKey]

    // Interval / Range vs Stopwatch
    val isCountdownMode = routine.isRange && routine.durationMinutes > 0
    val targetDurationSeconds = if (isCountdownMode) routine.durationMinutes * 60 else 0

    // Persistent state initialization
    var isRunning by remember {
        mutableStateOf(PersistentFocusManager.isRunning(context, routine.id))
    }

    var elapsedSeconds by remember {
        mutableIntStateOf(PersistentFocusManager.getElapsedSeconds(context, routine.id))
    }

    var showManualAddDialog by remember { mutableStateOf(false) }
    var showFinishConfirmDialog by remember { mutableStateOf(false) }
    var finishSessionNote by remember { mutableStateOf("") }

    // Heartbeat ticker: Updates UI every second from PersistentFocusManager timestamp
    LaunchedEffect(routine.id, isRunning) {
        while (true) {
            val updatedElapsed = PersistentFocusManager.getElapsedSeconds(context, routine.id)
            elapsedSeconds = updatedElapsed
            isRunning = PersistentFocusManager.isRunning(context, routine.id)
            delay(500L)
        }
    }

    // Auto-vibrate on countdown completion
    LaunchedEffect(isCountdownMode, elapsedSeconds, isRunning) {
        if (isCountdownMode && targetDurationSeconds > 0 && elapsedSeconds >= targetDurationSeconds && isRunning) {
            try {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(500L)
                }
            } catch (e: Exception) {
                // Ignore vibration failure
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF070510),
                            Color(0xFF0F0D24),
                            Color(0xFF141733),
                            Color(0xFF0B0A1A)
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Kapat",
                            tint = PureWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = routine.name,
                            color = PureWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (routine.isRange) AccentTeal.copy(alpha = 0.25f) else AccentPurple.copy(alpha = 0.25f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (routine.isRange) "Aralıklı Odak (${routine.durationMinutes} dk)" else "Süre Tutucu",
                                    color = if (routine.isRange) AccentTeal else AccentPurple,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (isRunning) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color(0x3310B981))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "🟢 Arka Planda Aktif",
                                        color = DoneGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Manuel Süre Ekleme Butonu
                    IconButton(
                        onClick = { showManualAddDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AccentTeal.copy(alpha = 0.25f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Elle Süre Ekle",
                            tint = AccentTeal,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Giant Animated Glowing Timer Dial
                val progress = if (isCountdownMode && targetDurationSeconds > 0) {
                    (elapsedSeconds.toFloat() / targetDurationSeconds.toFloat()).coerceIn(0f, 1f)
                } else {
                    1f
                }

                val pulseTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by pulseTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = if (isRunning) 1.05f else 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )

                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer pulsing glow
                    Box(
                        modifier = Modifier
                            .size(210.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        if (isRunning) (if (routine.isRange) AccentTeal else AccentPurple).copy(alpha = 0.28f) else Color.Transparent,
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Track Ring
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(190.dp),
                        color = Color(0x22FFFFFF),
                        strokeWidth = 10.dp,
                    )

                    // Active Progress Ring
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(190.dp),
                        color = if (routine.isRange) AccentTeal else AccentPurple,
                        strokeWidth = 10.dp,
                    )

                    // Centered Digital Time Display
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val displaySeconds = if (isCountdownMode && targetDurationSeconds > 0) {
                            (targetDurationSeconds - elapsedSeconds).coerceAtLeast(0)
                        } else {
                            elapsedSeconds
                        }

                        val hrs = displaySeconds / 3600
                        val mins = (displaySeconds % 3600) / 60
                        val secs = displaySeconds % 60

                        val timeString = if (hrs > 0) {
                            String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
                        } else {
                            String.format(Locale.US, "%02d:%02d", mins, secs)
                        }

                        Text(
                            text = timeString,
                            color = PureWhite,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = when {
                                isCountdownMode && displaySeconds == 0 -> "Süre Tamamlandı! 🎉"
                                isRunning -> if (isCountdownMode) "Kalan Süre (Odaklanılıyor)" else "Süre İlerliyor..."
                                elapsedSeconds > 0 -> "Duraklatıldı ⏸️"
                                else -> "Başlamaya Hazır"
                            },
                            color = if (isRunning) (if (routine.isRange) AccentTeal else AccentPurple) else SubtitleWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Controls: Play / Pause / Finish / Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset Button
                    IconButton(
                        onClick = {
                            PersistentFocusManager.reset(context, routine.id)
                            elapsedSeconds = 0
                            isRunning = false
                            Toast.makeText(context, "Süre sıfırlandı", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Sıfırla",
                            tint = SubtitleWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Play / Pause Main Button
                    Button(
                        onClick = {
                            if (isRunning) {
                                PersistentFocusManager.pause(context)
                                isRunning = false
                            } else {
                                PersistentFocusManager.startOrResume(
                                    context = context,
                                    routineId = routine.id,
                                    isCountdown = isCountdownMode,
                                    targetSeconds = targetDurationSeconds
                                )
                                isRunning = true
                            }
                        },
                        modifier = Modifier
                            .height(56.dp)
                            .width(150.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) Color(0xFFEF4444) else (if (routine.isRange) AccentTeal else AccentPurple)
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = if (isRunning) PureWhite else Color(0xFF0F172A),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRunning) "Durdur" else "Başlat",
                                color = if (isRunning) PureWhite else Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Finish & Save Button
                    IconButton(
                        onClick = {
                            if (elapsedSeconds > 0) {
                                showFinishConfirmDialog = true
                            } else {
                                Toast.makeText(context, "Önce süre başlatın veya elle ekleyin", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(DoneGreen.copy(alpha = 0.25f))
                            .border(1.dp, DoneGreen.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Bitir & Kaydet",
                            tint = DoneGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Session History & Stats Panel ("kaydettiğimiz süreyi altta görebilelim")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xE612192E))
                        .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Kaydedilen Odak Seansları",
                                    color = PureWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                val totalSec = currentLog?.focusDurationSeconds ?: 0
                                val totalHrs = totalSec / 3600
                                val totalMins = (totalSec % 3600) / 60
                                val totalFormatted = when {
                                    totalHrs > 0 && totalMins > 0 -> "${totalHrs} saat ${totalMins} dk"
                                    totalHrs > 0 -> "${totalHrs} saat"
                                    totalMins > 0 -> "${totalMins} dakika"
                                    totalSec > 0 -> "${totalSec} saniye"
                                    else -> "0 dk"
                                }
                                Text(
                                    text = "Bugünkü Toplam: $totalFormatted",
                                    color = AccentTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Elle Süre Ekle Hızlı Butonu
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(AccentPurple.copy(alpha = 0.3f))
                                    .clickable { showManualAddDialog = true }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = null,
                                        tint = PureWhite,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Manuel Ekle",
                                        color = PureWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val sessionNotes = currentLog?.focusSessionNotes.orEmpty()
                        if (sessionNotes.isEmpty() && (currentLog?.focusDurationSeconds ?: 0) == 0) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        tint = MutedWhite,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Henüz bu rutin için süre kaydedilmedi.\nBaşlat'a basarak veya Manuel Ekle ile süre kaydedebilirsiniz.",
                                        color = MutedWhite,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(sessionNotes.reversed()) { entry ->
                                    // Format: "HH:mm|seconds|note"
                                    val parts = entry.split("|")
                                    val time = parts.getOrNull(0) ?: "Bugün"
                                    val secs = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                    val note = parts.getOrNull(2) ?: "Odak Seansı"

                                    val m = secs / 60
                                    val s = secs % 60
                                    val durStr = if (m > 0) "${m} dk" else "${s} sn"

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0x22FFFFFF))
                                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(AccentTeal)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = note,
                                                    color = PureWhite,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Saat $time",
                                                    color = SubtitleWhite,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(Color(0x3310B981))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "+$durStr",
                                                color = DoneGreen,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Modal Dialog: Manuel Süre Ekleme
    if (showManualAddDialog) {
        ManualDurationAddDialog(
            routineName = routine.name,
            onDismiss = { showManualAddDialog = false },
            onConfirmAdd = { durationMinutes, note ->
                val durationSec = durationMinutes * 60
                viewModel.saveFocusSession(
                    routineId = routine.id,
                    durationSeconds = durationSec,
                    markRoutineComplete = true,
                    sessionNote = note.ifBlank { "Manuel Eklendi ($durationMinutes dk)" },
                    date = today
                )
                showManualAddDialog = false
                Toast.makeText(context, "$durationMinutes dk başarıyla Firestore'a kaydedildi!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal Dialog: Seansı Bitir & Kaydet Onayı
    if (showFinishConfirmDialog) {
        FinishSessionDialog(
            routineName = routine.name,
            elapsedSeconds = elapsedSeconds,
            sessionNote = finishSessionNote,
            onNoteChange = { finishSessionNote = it },
            onDismiss = { showFinishConfirmDialog = false },
            onConfirm = {
                PersistentFocusManager.pause(context)
                PersistentFocusManager.reset(context, routine.id)
                viewModel.saveFocusSession(
                    routineId = routine.id,
                    durationSeconds = elapsedSeconds,
                    markRoutineComplete = true,
                    sessionNote = finishSessionNote.ifBlank { "Odak Seansı (${elapsedSeconds / 60} dk)" },
                    date = today
                )
                elapsedSeconds = 0
                isRunning = false
                showFinishConfirmDialog = false
                Toast.makeText(context, "Odak süresi başarıyla Firestore'a kaydedildi! 🎯", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
        )
    }
}

/**
 * Manuel Süre Ekleme Dialogu
 */
@Composable
private fun ManualDurationAddDialog(
    routineName: String,
    onDismiss: () -> Unit,
    onConfirmAdd: (minutes: Int, note: String) -> Unit
) {
    var selectedMinutes by remember { mutableIntStateOf(30) }
    var customMinutesText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    val quickOptions = listOf(15, 30, 45, 60, 90, 120)

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF13192B))
                .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Manuel Süre Ekle",
                    color = PureWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = routineName,
                    color = AccentTeal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Minutes Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickOptions.take(3).forEach { mins ->
                        val isSelected = selectedMinutes == mins && customMinutesText.isEmpty()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AccentPurple else Color(0x22FFFFFF))
                                .clickable {
                                    selectedMinutes = mins
                                    customMinutesText = ""
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$mins dk",
                                color = if (isSelected) PureWhite else SubtitleWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickOptions.drop(3).forEach { mins ->
                        val isSelected = selectedMinutes == mins && customMinutesText.isEmpty()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AccentPurple else Color(0x22FFFFFF))
                                .clickable {
                                    selectedMinutes = mins
                                    customMinutesText = ""
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$mins dk",
                                color = if (isSelected) PureWhite else SubtitleWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Minute Input
                OutlinedTextField(
                    value = customMinutesText,
                    onValueChange = {
                        customMinutesText = it.filter { char -> char.isDigit() }.take(4)
                        it.toIntOrNull()?.let { num -> selectedMinutes = num }
                    },
                    label = { Text("Veya Özel Dakika Girin", color = SubtitleWhite, fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        focusedBorderColor = AccentTeal,
                        unfocusedBorderColor = GlassCardBorderSubtle
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Session Note
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Seans Açıklaması (Örn: Matematik 40 soru)", color = SubtitleWhite, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        focusedBorderColor = AccentTeal,
                        unfocusedBorderColor = GlassCardBorderSubtle
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("İptal", color = SubtitleWhite)
                    }

                    Button(
                        onClick = {
                            val finalMins = customMinutesText.toIntOrNull() ?: selectedMinutes
                            if (finalMins > 0) {
                                onConfirmAdd(finalMins, noteText)
                            }
                        },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                    ) {
                        Text("Kaydet", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Seansı Bitir & Kaydet Onay Dialogu
 */
@Composable
private fun FinishSessionDialog(
    routineName: String,
    elapsedSeconds: Int,
    sessionNote: String,
    onNoteChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val mins = elapsedSeconds / 60
    val secs = elapsedSeconds % 60
    val durationFormatted = if (mins > 0) "${mins} dakika ${secs} saniye" else "${secs} saniye"

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF13192B))
                .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Odak Seansını Kaydet",
                    color = PureWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$routineName • $durationFormatted",
                    color = DoneGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = sessionNote,
                    onValueChange = onNoteChange,
                    label = { Text("Ne yaptın? (Örn: 50 soru, Konu özeti)", color = SubtitleWhite, fontSize = 12.sp) },
                    singleLine = false,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        focusedBorderColor = DoneGreen,
                        unfocusedBorderColor = GlassCardBorderSubtle
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Vazgeç", color = SubtitleWhite)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DoneGreen)
                    ) {
                        Text("Kaydet & Tamamla", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
