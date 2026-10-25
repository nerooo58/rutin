package com.nororoutine.batu.ui.screens.tablet

import android.content.ContentUris
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nororoutine.batu.audio.DeviceMusicManager
import com.nororoutine.batu.audio.DeviceTrack
import com.nororoutine.batu.data.PersistentFocusManager
import com.nororoutine.batu.model.Routine
import com.nororoutine.batu.model.RoutineLog
import com.nororoutine.batu.ui.illustrations.RoutineIllustration
import com.nororoutine.batu.ui.theme.AccentPurple
import com.nororoutine.batu.ui.theme.AccentTeal
import com.nororoutine.batu.ui.theme.DoneGreen
import com.nororoutine.batu.ui.theme.GlassCardBorderSubtle
import com.nororoutine.batu.ui.theme.MissedCardBg
import com.nororoutine.batu.ui.theme.MissedCardBorder
import com.nororoutine.batu.ui.theme.MissedRed
import com.nororoutine.batu.ui.theme.MutedWhite
import com.nororoutine.batu.ui.theme.PureWhite
import com.nororoutine.batu.ui.theme.SubtitleWhite
import com.nororoutine.batu.viewmodel.RutinUiState
import com.nororoutine.batu.viewmodel.RutinViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Data class representing the visual styling and gradient theme for a routine card.
 */
data class RoutineCardTheme(
    val primaryColor: Color,
    val accentColor: Color,
    val stripeColor: Color,
    val gradientBrush: Brush,
    val isMorning: Boolean,
    val isNight: Boolean
)

/**
 * Computes custom gradient and color scheme according to routine timing and category:
 * - Sabah rutinleri: Turuncu/sarı tonlar
 * - Gece rutinleri: Mor/lacivert tonlar
 * - Diğer rutinler: Kategoriye özel canlı tonlar
 */
fun getRoutineCardTheme(routine: Routine): RoutineCardTheme {
    val cleanTime = routine.startTime.ifEmpty { routine.time }
    val hour = cleanTime.split(":", "–", "-").firstOrNull()?.trim()?.toIntOrNull() ?: 12
    val nameLower = routine.name.lowercase(Locale("tr", "TR"))
    val categoryLower = routine.category.lowercase(Locale("tr", "TR"))

    val isMorning = hour in 5..11 || nameLower.contains("sabah") || nameLower.contains("kahvaltı") || nameLower.contains("uyan")
    val isNight = hour >= 20 || hour < 5 || nameLower.contains("gece") || nameLower.contains("uyku") || nameLower.contains("akşam")

    return when {
        isMorning -> {
            RoutineCardTheme(
                primaryColor = Color(0xFFF59E0B),
                accentColor = Color(0xFFFBBF24),
                stripeColor = Color(0xFFF59E0B),
                gradientBrush = Brush.horizontalGradient(
                    listOf(
                        Color(0x40F97316),
                        Color(0x22FBBF24),
                        Color(0x1213112A)
                    )
                ),
                isMorning = true,
                isNight = false
            )
        }
        isNight -> {
            RoutineCardTheme(
                primaryColor = Color(0xFF8B5CF6),
                accentColor = Color(0xFFA78BFA),
                stripeColor = Color(0xFF8B5CF6),
                gradientBrush = Brush.horizontalGradient(
                    listOf(
                        Color(0x408B5CF6),
                        Color(0x226366F1),
                        Color(0x1213112A)
                    )
                ),
                isMorning = false,
                isNight = true
            )
        }
        categoryLower.contains("spor") -> {
            RoutineCardTheme(
                primaryColor = Color(0xFF10B981),
                accentColor = Color(0xFF34D399),
                stripeColor = Color(0xFF10B981),
                gradientBrush = Brush.horizontalGradient(
                    listOf(
                        Color(0x4010B981),
                        Color(0x2234D399),
                        Color(0x1213112A)
                    )
                ),
                isMorning = false,
                isNight = false
            )
        }
        categoryLower.contains("beslenme") -> {
            RoutineCardTheme(
                primaryColor = Color(0xFFEC4899),
                accentColor = Color(0xFFF472B6),
                stripeColor = Color(0xFFEC4899),
                gradientBrush = Brush.horizontalGradient(
                    listOf(
                        Color(0x40EC4899),
                        Color(0x22F472B6),
                        Color(0x1213112A)
                    )
                ),
                isMorning = false,
                isNight = false
            )
        }
        else -> {
            RoutineCardTheme(
                primaryColor = Color(0xFF06B6D4),
                accentColor = Color(0xFF22D3EE),
                stripeColor = Color(0xFF06B6D4),
                gradientBrush = Brush.horizontalGradient(
                    listOf(
                        Color(0x4006B6D4),
                        Color(0x223B82F6),
                        Color(0x1213112A)
                    )
                ),
                isMorning = false,
                isNight = false
            )
        }
    }
}

/**
 * 3-Column Dedicated Tablet Layout for the "Bugün" (Today) screen.
 */
@Composable
fun TabletTodayLayout(
    viewModel: RutinViewModel,
    uiState: RutinUiState,
    today: LocalDate,
    nowTime: LocalTime,
    locationText: String,
    focusedIndex: Int,
    onFocusChange: (Int) -> Unit,
    onNavigateToEdit: () -> Unit,
    onOpenScreenTimeDetail: () -> Unit,
    onOpenStoryShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayRoutines = remember(uiState.routines, today) {
        viewModel.getRoutinesForDay(today)
    }

    val safeFocusedIndex = remember(focusedIndex, todayRoutines.size) {
        if (todayRoutines.isEmpty()) 0 else focusedIndex.coerceIn(0, todayRoutines.lastIndex)
    }

    val selectedRoutine = remember(todayRoutines, safeFocusedIndex) {
        todayRoutines.getOrNull(safeFocusedIndex)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D0B1E), Color(0xFF13112A))
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // ==========================================
            // SOL PANEL - Rutin Listesi
            // ==========================================
            TabletRoutineListPanel(
                routines = todayRoutines,
                selectedIndex = safeFocusedIndex,
                onSelectRoutine = onFocusChange,
                uiState = uiState,
                today = today,
                nowTime = nowTime,
                locationText = locationText,
                onNavigateToEdit = onNavigateToEdit,
                onOpenScreenTimeDetail = onOpenScreenTimeDetail,
                onOpenStoryShare = onOpenStoryShare,
                modifier = Modifier
                    .weight(1.05f)
                    .fillMaxHeight()
            )

            // ==========================================
            // ORTA PANEL - Rutin Detayları
            // ==========================================
            TabletRoutineDetailPanel(
                routine = selectedRoutine,
                today = today,
                uiState = uiState,
                viewModel = viewModel,
                onNavigateToEdit = onNavigateToEdit,
                modifier = Modifier
                    .weight(if (uiState.showTabletMusicCard) 1.35f else 2.4f)
                    .fillMaxHeight()
            )

            // ==========================================
            // SAĞ PANEL - Müzik Kartı (Sabit kalır)
            // ==========================================
            if (uiState.showTabletMusicCard) {
                TabletMusicCardPanel(
                    modifier = Modifier
                        .weight(1.05f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

/**
 * SOL PANEL: Dikey sıralı rutin kartları, aktif kart vurgusu, kalın sol renk şeridi,
 * ve ayarlar toggle'ına bağlı opsiyonel Hava Durumu ve Ekran Süresi başlığı.
 */
@Composable
fun TabletRoutineListPanel(
    routines: List<Routine>,
    selectedIndex: Int,
    onSelectRoutine: (Int) -> Unit,
    uiState: RutinUiState,
    today: LocalDate,
    nowTime: LocalTime,
    locationText: String,
    onNavigateToEdit: () -> Unit,
    onOpenScreenTimeDetail: () -> Unit,
    onOpenStoryShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val turkishLocale = remember { Locale("tr", "TR") }
    val formattedTime = remember(nowTime) { nowTime.format(DateTimeFormatter.ofPattern("HH:mm")) }
    val formattedDateFull = remember(today) {
        val d = today.dayOfMonth
        val m = today.format(DateTimeFormatter.ofPattern("MMMM", turkishLocale))
        val dayName = today.format(DateTimeFormatter.ofPattern("EEEE", turkishLocale))
        "$d $m $dayName"
    }

    val completedCount = remember(routines, uiState.logs, today) {
        val dStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        routines.count { uiState.logs["${dStr}_${it.id}"]?.isDone == true }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Sol Panel Header: Saat, Tarih & Seri
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xE613112A))
                .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = formattedTime,
                            color = PureWhite,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = formattedDateFull,
                            color = SubtitleWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (uiState.stats.currentStreak > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color(0x33F59E0B))
                                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "🔥 ${uiState.stats.currentStreak} Gün",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Paylaş Butonu
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(AccentPurple.copy(alpha = 0.25f))
                                .clickable { onOpenStoryShare() }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "Paylaş",
                                color = PureWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Ayarlar Toggle 1: Hava Durumu Göster/Gizle
                if (uiState.showTabletWeather) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x331E1B3E))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Thermostat,
                                contentDescription = null,
                                tint = AccentTeal,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${uiState.weatherInfo.temperature.toInt()}°C • ${uiState.weatherInfo.condition.label}",
                                color = PureWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = locationText,
                                color = SubtitleWhite,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Ayarlar Toggle 2: Ekran Süresi Göster/Gizle
                if (uiState.showTabletScreenTime) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x331E1B3E))
                            .clickable { onOpenScreenTimeDetail() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Smartphone,
                                contentDescription = null,
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Ekran Süresi",
                                color = PureWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "Detaylar →",
                            color = AccentTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Rutinler Başlık ve Sayım
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Rutinler (${routines.size})",
                color = PureWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$completedCount / ${routines.size} Tamamlandı",
                color = if (completedCount == routines.size && routines.isNotEmpty()) DoneGreen else AccentTeal,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Rutin Kartları Dikey Listesi
        if (routines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x9913112A))
                    .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(18.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Bugün için planlanmış rutin yok",
                        color = MutedWhite,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onNavigateToEdit,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Rutin Ekle", color = PureWhite, fontSize = 12.sp)
                    }
                }
            }
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                itemsIndexed(routines) { index, routine ->
                    val isSelected = index == selectedIndex
                    val theme = remember(routine) { getRoutineCardTheme(routine) }
                    val logKey = "${today.format(DateTimeFormatter.ISO_LOCAL_DATE)}_${routine.id}"
                    val isDone = uiState.logs[logKey]?.isDone == true

                    // Rutin Kartı (Sol kalın renk şeridi, kendi gradient rengi, büyük ikon, bold isim, küçük saat)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(theme.gradientBrush)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) theme.accentColor else Color(0x22FFFFFF),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectRoutine(index) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Kalın Sol Renk Şeridi
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .fillMaxHeight()
                                    .background(theme.stripeColor)
                            )

                            // Kart İçeriği
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Büyük Rutin İkonu
                                    RoutineIllustration(
                                        illustrationId = routine.illustrationId,
                                        size = 44.dp
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = routine.name,
                                            color = if (isDone) MutedWhite else PureWhite,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = routine.time,
                                            color = SubtitleWhite,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // Durum Göstergesi (Tamamlandı veya Bekliyor)
                                if (isDone) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Tamamlandı",
                                        tint = DoneGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(theme.accentColor)
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

/**
 * ORTA PANEL: Seçili rutine göre dinamik değişen detay paneli.
 * - Seçili rutinin tema renkleriyle uyumlu
 * - Büyük dijital süre sayacı (başlat/durdur/sıfırla)
 * - Alt rutinler listesi (her biri işaretlenebilir checkbox ile)
 * - O rutine özel not ekleme/düzenleme alanı
 * - Tamamla ve Boş Geç butonları
 */
@Composable
fun TabletRoutineDetailPanel(
    routine: Routine?,
    today: LocalDate,
    uiState: RutinUiState,
    viewModel: RutinViewModel,
    onNavigateToEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (routine == null) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xE613112A))
                .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(20.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Görüntülenecek rutin bulunmuyor",
                    color = SubtitleWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    val theme = remember(routine) { getRoutineCardTheme(routine) }
    val logKey = "${today.format(DateTimeFormatter.ISO_LOCAL_DATE)}_${routine.id}"
    val currentLog = uiState.logs[logKey]
    val isDone = currentLog?.isDone == true

    // Note field state
    var noteText by remember(routine.id, currentLog?.note) {
        mutableStateOf(currentLog?.note.orEmpty())
    }

    // New subtask inline input
    var newSubtaskText by remember(routine.id) { mutableStateOf("") }

    // Digital Timer State (Persistent / Local ticking)
    val context = LocalContext.current
    var isTimerRunning by remember(routine.id) { mutableStateOf(false) }
    var timerSeconds by remember(routine.id) {
        mutableIntStateOf(PersistentFocusManager.getElapsedSeconds(context, routine.id))
    }

    // Timer Ticker
    LaunchedEffect(routine.id, isTimerRunning) {
        if (isTimerRunning) {
            while (true) {
                delay(1000L)
                timerSeconds = PersistentFocusManager.getElapsedSeconds(context, routine.id)
            }
        }
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xE613112A))
            .border(1.dp, theme.accentColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Rutin Başlık & İkon Başlığı
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoutineIllustration(
                        illustrationId = routine.illustrationId,
                        size = 54.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(theme.primaryColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = when {
                                        theme.isMorning -> "Sabah Rutini"
                                        theme.isNight -> "Gece Rutini"
                                        else -> routine.category
                                    },
                                    color = theme.accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = routine.time,
                                color = SubtitleWhite,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = routine.name,
                            color = PureWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Durum Rozeti
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isDone) DoneGreen.copy(alpha = 0.2f) else theme.primaryColor.copy(alpha = 0.15f))
                        .border(
                            1.dp,
                            if (isDone) DoneGreen.copy(alpha = 0.6f) else theme.accentColor.copy(alpha = 0.4f),
                            RoundedCornerShape(999.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (isDone) "✓ Tamamlandı" else "Devam Ediyor",
                        color = if (isDone) DoneGreen else theme.accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 1. BÜYÜK DİJİTAL SÜRE SAYACI
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x590D0B1E))
                    .border(1.dp, theme.accentColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Odak Sayacı",
                            color = SubtitleWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isTimerRunning) "● Süre Akıyor" else "Durduruldu",
                            color = if (isTimerRunning) DoneGreen else MutedWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val mins = timerSeconds / 60
                    val secs = timerSeconds % 60
                    val timerFormatted = "%02d:%02d".format(mins, secs)

                    Text(
                        text = timerFormatted,
                        color = theme.accentColor,
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Başlat / Durdur Butonu
                        Button(
                            onClick = {
                                if (isTimerRunning) {
                                    PersistentFocusManager.pause(context)
                                    isTimerRunning = false
                                } else {
                                    PersistentFocusManager.startOrResume(
                                        context = context,
                                        routineId = routine.id,
                                        isCountdown = false,
                                        targetSeconds = 1500
                                    )
                                    isTimerRunning = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTimerRunning) Color(0xFFEF4444) else theme.primaryColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Icon(
                                imageVector = if (isTimerRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isTimerRunning) "Durdur" else "Başlat",
                                tint = PureWhite,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isTimerRunning) "Durdur" else "Başlat",
                                color = PureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Sıfırla Butonu
                        OutlinedButton(
                            onClick = {
                                PersistentFocusManager.reset(context, routine.id)
                                isTimerRunning = false
                                timerSeconds = 0
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SubtitleWhite),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Sıfırla",
                                tint = SubtitleWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sıfırla", fontSize = 12.sp)
                        }
                    }
                }
            }

            // 2. ALT RUTİNLER LİSTESİ (Checklist)
            val subtasks = routine.subtasks
            val completedSubtasks = currentLog?.completedSubtasks.orEmpty()

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Alt Rutinler",
                        color = PureWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${completedSubtasks.size} / ${subtasks.size} Tamam",
                        color = if (completedSubtasks.size == subtasks.size && subtasks.isNotEmpty()) DoneGreen else theme.accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (subtasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x331E1B3E))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Henüz alt rutin eklenmedi. Aşağıdan ekleyebilirsiniz.",
                            color = SubtitleWhite,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    subtasks.forEachIndexed { index, subtask ->
                        val isSubtaskDone = completedSubtasks.contains(subtask)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSubtaskDone) Color(0x1F10B981) else Color(0x221E1B3E))
                                .border(
                                    1.dp,
                                    if (isSubtaskDone) DoneGreen.copy(alpha = 0.4f) else Color(0x1FFFFFFF),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.toggleSubtaskDone(routine.id, subtask, today)
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSubtaskDone,
                                    onCheckedChange = {
                                        viewModel.toggleSubtaskDone(routine.id, subtask, today)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = DoneGreen,
                                        uncheckedColor = SubtitleWhite,
                                        checkmarkColor = PureWhite
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = subtask,
                                    color = if (isSubtaskDone) MutedWhite else PureWhite,
                                    fontSize = 13.sp,
                                    textDecoration = if (isSubtaskDone) TextDecoration.LineThrough else TextDecoration.None,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Alt Rutini Silme Butonu
                            IconButton(
                                onClick = { viewModel.removeSubtask(routine.id, index) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Alt Rutini Sil",
                                    tint = Color(0xFFF87171).copy(alpha = 0.85f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Hızlı Alt Rutin Ekleme Satırı (Tamamen net, ferah ve kesilme yapmayan tasarım)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x550D0B1E))
                        .border(1.dp, theme.accentColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = newSubtaskText,
                        onValueChange = { newSubtaskText = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = PureWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (newSubtaskText.isNotBlank()) {
                                    viewModel.addSubtask(routine.id, newSubtaskText)
                                    newSubtaskText = ""
                                }
                            }
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(theme.accentColor),
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        decorationBox = { innerTextField ->
                            if (newSubtaskText.isEmpty()) {
                                Text(
                                    text = "Yeni alt rutin yazın...",
                                    color = SubtitleWhite,
                                    fontSize = 12.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    IconButton(
                        onClick = {
                            if (newSubtaskText.isNotBlank()) {
                                viewModel.addSubtask(routine.id, newSubtaskText)
                                newSubtaskText = ""
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.primaryColor)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Alt Rutin Ekle",
                            tint = PureWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 3. O RUTİNE ÖZEL NOT ALANI
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.EditNote,
                            contentDescription = null,
                            tint = theme.accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Rutin Notu",
                            color = PureWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (noteText.isNotBlank()) {
                        Text(
                            text = "Otomatik kaydedilir",
                            color = SubtitleWhite,
                            fontSize = 10.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { newNote ->
                        noteText = newNote
                        viewModel.updateRoutineNote(routine.id, today, newNote)
                    },
                    placeholder = {
                        Text(
                            text = "Bu rutine dair not, düşünce veya kazanımlarınızı yazın...",
                            color = SubtitleWhite,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.accentColor,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        cursorColor = theme.accentColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // 4. TAMAMLA VE BOŞ GEÇ BUTONLARI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tamamla Butonu
                Button(
                    onClick = {
                        if (isDone) {
                            viewModel.markRoutineEmpty(routine.id, today)
                        } else {
                            viewModel.markRoutineDone(routine.id, today)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDone) DoneGreen else theme.primaryColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = if (isDone) Icons.Rounded.CheckCircle else Icons.Rounded.Check,
                        contentDescription = "Tamamla",
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isDone) "Tamamlandı ✓" else "Tamamla",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Boş Geç Butonu
                OutlinedButton(
                    onClick = {
                        viewModel.markRoutineEmpty(routine.id, today)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MissedRed
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MissedRed.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Boş Geç",
                        tint = MissedRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Boş Geç",
                        color = MissedRed,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * SAĞ PANEL: Sabit Müzik Kartı
 * - Sabit kalır, rutin değişse bile panel değişmez
 * - Büyük albüm kapağı görseli (AsyncImage Coil / şık vinyl görseli)
 * - Şarkı adı (bold, büyük)
 * - Sanatçı adı (küçük, soluk)
 * - Oynat/Durdur butonu, İleri ve Geri butonları
 * - Ses kontrolü OLMAYACAK
 * - Spotify veya cihaz medyasından parça bilgileri
 */
@Composable
fun TabletMusicCardPanel(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tracks by DeviceMusicManager.tracks.collectAsState()
    val trackIndex by DeviceMusicManager.currentTrackIndex.collectAsState()
    val isPlaying by DeviceMusicManager.isPlaying.collectAsState()
    val currentPositionMs by DeviceMusicManager.currentPositionMs.collectAsState()

    // Refresh device music on start
    LaunchedEffect(Unit) {
        DeviceMusicManager.refreshDeviceTracks(context)
    }

    val liveSpotifyTrack by DeviceMusicManager.liveSpotifyTrack.collectAsState()

    val currentTrack: DeviceTrack? = liveSpotifyTrack ?: remember(tracks, trackIndex) {
        DeviceMusicManager.getCurrentTrack()
    }

    val coverModel: Any? = remember(currentTrack) {
        if (!currentTrack?.coverUrl.isNullOrEmpty()) {
            currentTrack?.coverUrl
        } else if (currentTrack != null && currentTrack.albumId > 0) {
            ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), currentTrack.albumId)
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xE613112A))
            .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) Color(0x3306B6D4) else Color(0x338B5CF6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = if (isPlaying) AccentTeal else AccentPurple,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Müzik & Odak",
                        color = PureWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Dış müzik uygulamasını aç (Spotify / vb.)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x2210B981))
                        .clickable { DeviceMusicManager.openMusicApp(context) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInNew,
                            contentDescription = "Müzik Uygulaması",
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Spotify / Medya",
                            color = Color(0xFF34D399),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // BÜYÜK ALBÜM KAPAĞI GÖRSELİ
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF312E81), Color(0xFF0F172A))
                        )
                    )
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                    .clickable {
                        if (currentTrack != null) {
                            DeviceMusicManager.launchSpotifyTrack(context, currentTrack)
                        } else {
                            DeviceMusicManager.openMusicApp(context)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (coverModel != null) {
                    AsyncImage(
                        model = coverModel,
                        contentDescription = "Spotify Albüm Kapağı",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Stylized Vinyl Disc Art
                    VinylDiscArtwork(isPlaying = isPlaying)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Şarkı Adı (Bold, Büyük) ve Sanatçı Adı (Küçük, Soluk)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentTrack?.title ?: if (isPlaying) "Cihaz Medyası Çalıyor" else "Lofi Beats & Deep Focus",
                    color = PureWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = currentTrack?.artist ?: "Spotify • Odak Çalışma Ambiyansı",
                    color = SubtitleWhite,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Çalma İlerleme Çizgisi (varsa)
            if (currentTrack != null && currentTrack.durationMs > 0) {
                val progress = (currentPositionMs.toFloat() / currentTrack.durationMs.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0x33FFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(999.dp))
                            .background(AccentTeal)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = DeviceMusicManager.formatTime(currentPositionMs),
                        color = SubtitleWhite,
                        fontSize = 10.sp
                    )
                    Text(
                        text = DeviceMusicManager.formatTime(currentTrack.durationMs),
                        color = SubtitleWhite,
                        fontSize = 10.sp
                    )
                }
            }

            // OYNAT/DURDUR, İLERİ VE GERİ BUTONLARI (Ses kontrolü OLMAYACAK)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Geri Butonu
                IconButton(
                    onClick = { DeviceMusicManager.prevTrack(context) },
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Önceki Parça",
                        tint = PureWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Büyük Oynat / Durdur Butonu
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(AccentTeal, AccentPurple)
                            )
                        )
                        .clickable { DeviceMusicManager.togglePlayPause(context) }
                        .shadow(elevation = 6.dp, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Duraklat" else "Oynat",
                        tint = PureWhite,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // İleri Butonu
                IconButton(
                    onClick = { DeviceMusicManager.nextTrack(context) },
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Sonraki Parça",
                        tint = PureWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Canlı Ekolayzır Göstergesi (Alt Kısım)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabletEqualizerWave(isPlaying = isPlaying)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isPlaying) "Çalıyor" else "Duraklatıldı",
                    color = if (isPlaying) AccentTeal else SubtitleWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Procedural Vinyl Disc artwork for the large album art container when no device track cover exists.
 */
@Composable
fun VinylDiscArtwork(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer vinyl disc
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF232338), Color(0xFF0F0E1E), Color(0xFF07060F))
                    )
                )
                .border(2.dp, Color(0x33A78BFA), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Grooves
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color(0x22FFFFFF), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(75.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color(0x1AFFFFFF), CircleShape)
            )

            // Center Label
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF6366F1), Color(0xFF06B6D4))
                        )
                    )
                    .border(1.dp, Color(0x66FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Dancing equalizer wave visualizer for playback state.
 */
@Composable
fun TabletEqualizerWave(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tabletEqualizer")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = if (isPlaying) 0.95f else 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = if (isPlaying) 1.0f else 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(310, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (isPlaying) 0.85f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h3"
    )

    Row(
        modifier = modifier
            .height(16.dp)
            .width(18.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(h1, h2, h3).forEach { frac ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(frac.coerceIn(0.2f, 1f))
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isPlaying) AccentTeal else MutedWhite.copy(alpha = 0.4f))
            )
        }
    }
}
