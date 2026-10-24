package com.nororoutine.batu.ui.screens.phone

import android.content.ContentUris
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import com.nororoutine.batu.ui.illustrations.RoutineIllustration
import com.nororoutine.batu.ui.screens.tablet.RoutineCardTheme
import com.nororoutine.batu.ui.screens.tablet.getRoutineCardTheme
import com.nororoutine.batu.ui.theme.AccentPurple
import com.nororoutine.batu.ui.theme.AccentTeal
import com.nororoutine.batu.ui.theme.DoneGreen
import com.nororoutine.batu.ui.theme.GlassCardBorderSubtle
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
 * TELEFON İÇİN SIFIRDAN YENİDEN YAZILAN "BUGÜN" EKRANI
 *
 * GENEL TASARIM:
 * - Karanlık cam efekti (dark glassmorphism)
 * - Arka plan derin gece tonları (#0D0B1E, #13112A)
 * - Kartlar hafif şeffaf, köşeleri yuvarlatılmış (16-20dp)
 *
 * ÜST KISIM:
 * - Tek satırda saat, tarih ve hava durumu bilgisi — sade ve temiz
 * - "Önceki rutin" chip'i kaldırılmıştır
 * - Üstteki hızlı erişim buton çubuğu (Günün Rutinleri, ekran, müzik, Ders, Paylaş) tamamen kaldırılmıştır
 *
 * ORTA KISIM - Rutin Kartları:
 * - Aktif rutin kartı ekranın %65'ini kaplar
 * - Kartın altından bir sonraki rutinin üst kısmı hafifçe görünür ("kaydırabilirsin" hissi verir)
 * - Kartlar hem sola/sağa ok butonlarıyla hem de parmakla yukarı/aşağı/yana kaydırarak geçilebilir
 * - Her rutin kartı kendi renginde gradient arka plana sahip:
 *   * Sabah: turuncu/sarı tonlar
 *   * Gece: mor/lacivert tonlar
 *   * Diğer: kategori / özel renge göre
 * - Kart içeriği:
 *   * Sol kenarda kalın renk şeridi
 *   * Büyük rutin ikonu
 *   * Bold rutin adı
 *   * Küçük saat yazısı
 *   * Süre sayacı (başlat/durdur/sıfırla)
 *   * Alt rutinlerin özet listesi (işaretlenebilir)
 *   * O rutine özel not alanı
 *
 * ALT KISIM:
 * - Tamamla butonu büyük ve tam genişlikte, yeşil
 * - Boş Geç ve Düzenle küçük ikon butonları Tamamla'nın yanında
 * - Müzik şeridi: küçük albüm kapağı + şarkı adı + sanatçı adı + oynat/durdur + ileri/geri butonları
 *   (Ses kontrolü KESİNLİKLE OLMAYACAK)
 */
@Composable
fun PhoneTodayLayout(
    viewModel: RutinViewModel,
    uiState: RutinUiState,
    today: LocalDate,
    nowTime: LocalTime,
    locationText: String,
    focusedIndex: Int,
    onFocusChange: (Int) -> Unit,
    onNavigateToEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val turkishLocale = remember { Locale("tr", "TR") }
    val formattedTime = remember(nowTime) { nowTime.format(DateTimeFormatter.ofPattern("HH:mm")) }
    val formattedDate = remember(today) {
        today.format(DateTimeFormatter.ofPattern("d MMMM EEEE", turkishLocale))
    }

    val routines = remember(uiState.routines) {
        uiState.routines.sortedBy { it.startTime.ifEmpty { it.time } }
    }
    val safeIndex = focusedIndex.coerceIn(0, (routines.size - 1).coerceAtLeast(0))
    val currentRoutine: Routine? = routines.getOrNull(safeIndex)

    // Gesture Accumulator for vertical & horizontal swipe
    var dragAccumulatorY by remember { mutableFloatStateOf(0f) }
    var dragAccumulatorX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0D0B1E),
                        Color(0xFF13112A),
                        Color(0xFF0A0818)
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ─────────────────────────────────────────────────────────────
            // 1. ÜST KISIM: TEK SATIRDA SAAT, TARİH VE HAVA DURUMU (SADE VE TEMİZ)
            // ─────────────────────────────────────────────────────────────
            PhoneTopBar(
                formattedTime = formattedTime,
                formattedDate = formattedDate,
                temperature = uiState.weatherInfo.temperature.toInt(),
                weatherCondition = uiState.weatherInfo.condition.label,
                locationText = locationText
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ─────────────────────────────────────────────────────────────
            // 2. ORTA KISIM: %65 YÜKSEKLİKTE AKTİF KART + ALTINDA PEEK KART
            // ─────────────────────────────────────────────────────────────
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(safeIndex, routines.size) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumulatorY += dragAmount.y
                                dragAccumulatorX += dragAmount.x
                            },
                            onDragEnd = {
                                val threshold = 55f
                                if (dragAccumulatorY < -threshold || dragAccumulatorX < -threshold) {
                                    // Parmakla yukarı veya sola kaydırma: Sonraki rutin
                                    if (safeIndex < routines.lastIndex) {
                                        onFocusChange(safeIndex + 1)
                                    }
                                } else if (dragAccumulatorY > threshold || dragAccumulatorX > threshold) {
                                    // Parmakla aşağı veya sağa kaydırma: Önceki rutin
                                    if (safeIndex > 0) {
                                        onFocusChange(safeIndex - 1)
                                    }
                                }
                                dragAccumulatorY = 0f
                                dragAccumulatorX = 0f
                            },
                            onDragCancel = {
                                dragAccumulatorY = 0f
                                dragAccumulatorX = 0f
                            }
                        )
                    }
            ) {
                val totalAvailableHeight = maxHeight

                if (routines.isEmpty()) {
                    // Kayıtlı rutin yoksa boş durum
                    PhoneEmptyRoutinesView(
                        onNavigateToEdit = onNavigateToEdit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (currentRoutine != null) {
                    val hasNext = safeIndex < routines.lastIndex
                    val nextRoutine = if (hasNext) routines[safeIndex + 1] else null

                    // Aktif kart yaklaşık %65-70 yer kaplar, sonraki kart alttan hafifçe görünür
                    val activeCardHeight = if (hasNext) totalAvailableHeight * 0.76f else totalAvailableHeight

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Aktif Rutin Kartı
                        PhoneActiveRoutineCard(
                            routine = currentRoutine,
                            currentIndex = safeIndex,
                            totalCount = routines.size,
                            today = today,
                            uiState = uiState,
                            viewModel = viewModel,
                            onPreviousClick = {
                                if (safeIndex > 0) onFocusChange(safeIndex - 1)
                            },
                            onNextClick = {
                                if (safeIndex < routines.lastIndex) onFocusChange(safeIndex + 1)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(activeCardHeight)
                        )

                        // Kartın altından sonraki rutinin üst kısmı hafifçe görünür ("kaydırabilirsin" hissi)
                        if (hasNext && nextRoutine != null) {
                            PhonePeekingNextCard(
                                nextRoutine = nextRoutine,
                                onClick = {
                                    onFocusChange(safeIndex + 1)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ─────────────────────────────────────────────────────────────
            // 3. ALT KISIM: TAMAMLA (BÜYÜK, YEŞİL) + BOŞ GEÇ & DÜZENLE
            //               + MÜZİK ŞERİDİ (SES KONTROLÜ OLMAYACAK)
            // ─────────────────────────────────────────────────────────────
            if (currentRoutine != null) {
                val todayDateKey = "${today.format(DateTimeFormatter.ISO_LOCAL_DATE)}_${currentRoutine.id}"
                val isDone = uiState.logs[todayDateKey]?.isDone == true

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tamamla Butonu (Büyük ve tam genişlikte, yeşil)
                    Button(
                        onClick = {
                            viewModel.markRoutineDone(currentRoutine.id, today)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDone) Color(0xFF059669) else DoneGreen
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDone) "Tamamlandı ✓" else "Tamamla",
                            color = PureWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Boş Geç Küçük İkon Butonu
                    IconButton(
                        onClick = {
                            viewModel.markRoutineEmpty(currentRoutine.id, today)
                            if (safeIndex < routines.lastIndex) {
                                onFocusChange(safeIndex + 1)
                            }
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x33EF4444))
                            .border(1.dp, Color(0x66EF4444), RoundedCornerShape(16.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Boş Geç",
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Düzenle Küçük İkon Butonu
                    IconButton(
                        onClick = onNavigateToEdit,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x22FFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Düzenle",
                            tint = PureWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Müzik Şeridi (Ses kontrolü KESİNLİKLE OLMAYACAK)
            PhoneMusicStrip(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * ÜST KISIM: Tek satırda saat, tarih ve hava durumu bilgisi — sade ve temiz.
 */
@Composable
private fun PhoneTopBar(
    formattedTime: String,
    formattedDate: String,
    temperature: Int,
    weatherCondition: String,
    locationText: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xD913112A))
            .border(1.dp, Color(0x2AFFFFFF), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Saat ve Tarih
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = AccentTeal,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formattedTime,
                    color = PureWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "•",
                    color = MutedWhite,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formattedDate,
                    color = SubtitleWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Hava Durumu (Sade & Temiz)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Thermostat,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "$temperature°C",
                    color = PureWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = weatherCondition,
                    color = SubtitleWhite,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * ORTA KISIM: Aktif Rutin Kartı
 * - Sol kenarda kalın renk şeridi
 * - Büyük rutin ikonu
 * - Bold rutin adı
 * - Küçük saat yazısı
 * - Süre sayacı
 * - Alt rutinlerin özet listesi (işaretlenebilir)
 * - O rutine özel not alanı
 * - Sağ/sol ok butonlarıyla geçiş kontrolü
 */
@Composable
private fun PhoneActiveRoutineCard(
    routine: Routine,
    currentIndex: Int,
    totalCount: Int,
    today: LocalDate,
    uiState: RutinUiState,
    viewModel: RutinViewModel,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val theme = remember(routine) { getRoutineCardTheme(routine) }
    val todayDateKey = "${today.format(DateTimeFormatter.ISO_LOCAL_DATE)}_${routine.id}"
    val currentLog = uiState.logs[todayDateKey]
    val isDone = currentLog?.isDone == true

    // Persistent Focus Manager Timer State
    var isTimerRunning by remember(routine.id) { mutableStateOf(false) }
    var timerSeconds by remember(routine.id) {
        mutableIntStateOf(PersistentFocusManager.getElapsedSeconds(context, routine.id))
    }

    LaunchedEffect(routine.id, isTimerRunning) {
        if (isTimerRunning) {
            while (true) {
                delay(1000L)
                timerSeconds = PersistentFocusManager.getElapsedSeconds(context, routine.id)
            }
        }
    }

    // Rutine Özel Not Alanı
    var noteText by remember(routine.id, currentLog?.note) {
        mutableStateOf(currentLog?.note.orEmpty())
    }

    // Alt Rutin Ekleme
    var newSubtaskText by remember(routine.id) { mutableStateOf("") }
    var showAddSubtaskInput by remember(routine.id) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xE613112A))
            .background(theme.gradientBrush)
            .border(
                1.5.dp,
                theme.primaryColor.copy(alpha = 0.5f),
                RoundedCornerShape(20.dp)
            )
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Sol kenarda kalın renk şeridi
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(theme.stripeColor, shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
            )

            // Kart İçeriği (İç kaydırma ile tüm detaylar kompakt ve net)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. ÜST SATIR: İKON + BOLD AD + SAAT + OK BUTONLARI
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                            size = 46.dp
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = routine.name,
                                color = PureWhite,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = routine.startTime.ifEmpty { routine.time },
                                    color = theme.accentColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (routine.durationMinutes > 0) {
                                    Text(
                                        text = " • ${routine.durationMinutes} dk",
                                        color = SubtitleWhite,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Sola/Sağa Ok Butonları ve Sayaç Göstergesi
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = onPreviousClick,
                            enabled = currentIndex > 0,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Önceki Rutin",
                                tint = if (currentIndex > 0) PureWhite else Color(0x33FFFFFF),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "${currentIndex + 1}/$totalCount",
                            color = SubtitleWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = onNextClick,
                            enabled = currentIndex < totalCount - 1,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "Sonraki Rutin",
                                tint = if (currentIndex < totalCount - 1) PureWhite else Color(0x33FFFFFF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 2. DİJİTAL SÜRE SAYACI
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x660D0B1E))
                        .border(1.dp, theme.accentColor.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = theme.accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))

                            val mins = timerSeconds / 60
                            val secs = timerSeconds % 60
                            Text(
                                text = "%02d:%02d".format(mins, secs),
                                color = theme.accentColor,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Başlat / Durdur
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
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (isTimerRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isTimerRunning) "Durdur" else "Başlat",
                                    color = PureWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Sıfırla
                            IconButton(
                                onClick = {
                                 PersistentFocusManager.reset(context, routine.id)
                                 isTimerRunning = false
                                 timerSeconds = 0
                            },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x22FFFFFF))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Sıfırla",
                                    tint = SubtitleWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // 3. ALT RUTİNLERİN ÖZET LİSTESİ (İŞARETLENEBİLİR)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Alt Rutinler (${routine.subtasks.size})",
                            color = PureWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = { showAddSubtaskInput = !showAddSubtaskInput },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Alt Rutin Ekle",
                                tint = theme.accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (showAddSubtaskInput) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newSubtaskText,
                                onValueChange = { newSubtaskText = it },
                                placeholder = { Text("Yeni alt rutin...", color = SubtitleWhite, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = theme.accentColor,
                                    unfocusedBorderColor = Color(0x33FFFFFF),
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = PureWhite
                                ),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (newSubtaskText.isNotBlank()) {
                                            viewModel.addSubtask(routine.id, newSubtaskText)
                                            newSubtaskText = ""
                                            showAddSubtaskInput = false
                                        }
                                    }
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    if (newSubtaskText.isNotBlank()) {
                                        viewModel.addSubtask(routine.id, newSubtaskText)
                                        newSubtaskText = ""
                                        showAddSubtaskInput = false
                                    }
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(theme.primaryColor)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Kaydet",
                                    tint = PureWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (routine.subtasks.isEmpty()) {
                        Text(
                            text = "Henüz alt rutin eklenmedi.",
                            color = MutedWhite,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    } else {
                        routine.subtasks.forEach { subtask ->
                            val isSubtaskDone = currentLog?.completedSubtasks?.contains(subtask) == true
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSubtaskDone) Color(0x1A10B981) else Color(0x1AFFFFFF))
                                    .clickable {
                                        viewModel.toggleSubtaskDone(routine.id, subtask, today)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSubtaskDone) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSubtaskDone) DoneGreen else SubtitleWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = subtask,
                                    color = if (isSubtaskDone) MutedWhite else PureWhite,
                                    fontSize = 12.sp,
                                    textDecoration = if (isSubtaskDone) TextDecoration.LineThrough else null,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // 4. O RUTİNE ÖZEL NOT ALANI
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Rutin Notu",
                                color = PureWhite,
                                fontSize = 13.sp,
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
                                text = "Bu rutine özel not ekleyin...",
                                color = SubtitleWhite,
                                fontSize = 11.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.accentColor,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            cursorColor = theme.accentColor
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }
    }
}

/**
 * PEEKING NEXT CARD:
 * Kartın altından bir sonraki rutinin üst kısmı hafifçe görünür,
 * kullanıcıya "kaydırabilirsin" hissi verir.
 */
@Composable
private fun PhonePeekingNextCard(
    nextRoutine: Routine,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = remember(nextRoutine) { getRoutineCardTheme(nextRoutine) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xD915122F))
            .background(theme.gradientBrush)
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sol renk şeridi
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(theme.stripeColor, shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoutineIllustration(
                        illustrationId = nextRoutine.illustrationId,
                        size = 30.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Sonraki: ${nextRoutine.name}",
                            color = PureWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${nextRoutine.startTime.ifEmpty { nextRoutine.time }} • Kaydır veya tıkla",
                            color = SubtitleWhite,
                            fontSize = 10.sp
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Sonraki",
                    tint = theme.accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * MÜZİK ŞERİDİ:
 * Küçük albüm kapağı + şarkı adı + sanatçı adı + oynat/durdur + ileri/geri butonları
 * Ses kontrolü KESİNLİKLE OLMAYACAK.
 */
@Composable
private fun PhoneMusicStrip(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tracks by DeviceMusicManager.tracks.collectAsState()
    val trackIndex by DeviceMusicManager.currentTrackIndex.collectAsState()
    val isPlaying by DeviceMusicManager.isPlaying.collectAsState()

    LaunchedEffect(Unit) {
        DeviceMusicManager.refreshDeviceTracks(context)
    }

    val currentTrack: DeviceTrack? = remember(tracks, trackIndex) {
        DeviceMusicManager.getCurrentTrack()
    }

    val albumArtUri: Uri? = remember(currentTrack) {
        if (currentTrack != null && currentTrack.albumId > 0) {
            ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), currentTrack.albumId)
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xD913112A))
            .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Küçük Albüm Kapağı + Şarkı Adı + Sanatçı
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Küçük Albüm Kapağı
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x338B5CF6)),
                    contentAlignment = Alignment.Center
                ) {
                    if (albumArtUri != null) {
                        AsyncImage(
                            model = albumArtUri,
                            contentDescription = "Albüm Kapağı",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = AccentTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentTrack?.title ?: "Müzik / Odak Sesi",
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentTrack?.artist ?: "Cihaz Medyası / Spotify",
                        color = SubtitleWhite,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Oynatma Kontrolleri (Geri, Oynat/Durdur, İleri) — Ses Kontrolü YOK
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = { DeviceMusicManager.prevTrack(context) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Önceki Şarkı",
                        tint = PureWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { DeviceMusicManager.togglePlayPause(context) },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(AccentPurple)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Durdur" else "Oynat",
                        tint = PureWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { DeviceMusicManager.nextTrack(context) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Sonraki Şarkı",
                        tint = PureWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Boş Rutin Ekranı
 */
@Composable
private fun PhoneEmptyRoutinesView(
    onNavigateToEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xD913112A))
            .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(20.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Bugün için kayıtlı rutin bulunmuyor",
                color = PureWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Gününüzü planlamak için yeni bir rutin oluşturun.",
                color = SubtitleWhite,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onNavigateToEdit,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Rutin Ekle",
                    color = PureWhite,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
