package com.nororoutine.batu.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nororoutine.batu.network.GeminiStoryAnalysis
import com.nororoutine.batu.network.GeminiStoryHelper
import com.nororoutine.batu.ui.theme.AccentPurple
import com.nororoutine.batu.ui.theme.AccentTeal
import com.nororoutine.batu.ui.theme.DoneGreen
import com.nororoutine.batu.ui.theme.GlassCardBorderSubtle
import com.nororoutine.batu.ui.theme.MutedWhite
import com.nororoutine.batu.ui.theme.PureWhite
import com.nororoutine.batu.ui.theme.SubtitleWhite
import com.nororoutine.batu.viewmodel.RutinUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryShareBottomSheet(
    uiState: RutinUiState,
    initialPeriod: StoryPeriod = StoryPeriod.DAILY,
    initialDate: LocalDate = LocalDate.now(),
    initialRoutineId: String? = null,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedPeriod by remember { mutableStateOf(initialPeriod) }
    var selectedRoutineId by remember { mutableStateOf(initialRoutineId) }
    var isSharingImage by remember { mutableStateOf(false) }
    var isAnalyzingWithAi by remember { mutableStateOf(false) }
    var aiAnalysisResult by remember { mutableStateOf<GeminiStoryAnalysis?>(null) }

    // Recompute base data when filters change
    val storyData = remember(selectedPeriod, selectedRoutineId, uiState, initialDate, aiAnalysisResult) {
        StoryCardHelper.computeStoryData(
            period = selectedPeriod,
            uiState = uiState,
            anchorDate = initialDate,
            selectedRoutineId = selectedRoutineId,
            aiAnalysis = aiAnalysisResult
        )
    }

    // Function to trigger Gemini AI analysis
    fun triggerAiAnalysis() {
        scope.launch {
            isAnalyzingWithAi = true
            val targetRoutine = uiState.routines.find { it.id == selectedRoutineId }
            val routineName = targetRoutine?.name ?: "Tüm Rutinler"

            // Collect notes and sessions
            val rawNotes = mutableListOf<String>()
            for (log in uiState.logs.values) {
                if (selectedRoutineId == null || log.routineId == selectedRoutineId) {
                    val rName = uiState.routines.find { it.id == log.routineId }?.name ?: ""
                    if (log.focusSessionNotes.isNotEmpty()) {
                        rawNotes.addAll(log.focusSessionNotes.map { "$rName: $it" })
                    } else if (log.note.isNotBlank()) {
                        rawNotes.add("$rName: ${log.note}")
                    }
                }
            }

            val analysis = GeminiStoryHelper.analyzeStoryHighlights(
                routineTitle = routineName,
                periodTitle = selectedPeriod.label,
                totalFocusFormatted = storyData.formattedTotalFocus,
                rawNotesAndSessions = rawNotes,
                defaultTop3 = storyData.top3Accomplishments
            )
            aiAnalysisResult = analysis
            isAnalyzingWithAi = false
            Toast.makeText(context, "Yapay zeka analizi başarıyla tamamlandı! ✨", Toast.LENGTH_SHORT).show()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0C091A),
        scrimColor = Color(0xCC000000),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 16.dp, bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Başarı Hikayesi Kartı",
                        color = PureWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tuttuğun Süreler ve Başarı Detayları (9:16 Story)",
                        color = SubtitleWhite,
                        fontSize = 12.sp
                    )
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

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Period Selector Tabs: Günlük / Haftalık / Aylık / Yıllık
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x2A1E293B))
                    .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(999.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StoryPeriod.values().forEach { period ->
                    val isSelected = selectedPeriod == period
                    val tabIcon = when (period) {
                        StoryPeriod.DAILY -> Icons.Rounded.CalendarToday
                        StoryPeriod.WEEKLY -> Icons.Rounded.DateRange
                        StoryPeriod.MONTHLY -> Icons.Rounded.CalendarMonth
                        StoryPeriod.YEARLY -> Icons.Rounded.WorkspacePremium
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isSelected) AccentPurple else Color.Transparent)
                            .clickable {
                                selectedPeriod = period
                                aiAnalysisResult = null // Reset so fresh AI can be run or fallback
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = tabIcon,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF0D0B1E) else SubtitleWhite,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = period.label,
                                color = if (isSelected) Color(0xFF0D0B1E) else SubtitleWhite,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Routine Filter Chips (Tüm Rutinler veya Tek Bir Rutin: Örn Ders Çalışma)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "Tüm Rutinler" Chip
                val isAllSelected = selectedRoutineId == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isAllSelected) AccentTeal else Color(0x22FFFFFF))
                        .border(1.dp, if (isAllSelected) AccentTeal else Color(0x1AFFFFFF), RoundedCornerShape(999.dp))
                        .clickable {
                            selectedRoutineId = null
                            aiAnalysisResult = null
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🌟 Tüm Rutinler",
                        color = if (isAllSelected) Color(0xFF0F172A) else PureWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Individual Routine Chips
                uiState.routines.forEach { r ->
                    val isThisSelected = selectedRoutineId == r.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isThisSelected) AccentPurple else Color(0x1AFFFFFF))
                            .border(1.dp, if (isThisSelected) AccentPurple else Color(0x1AFFFFFF), RoundedCornerShape(999.dp))
                            .clickable {
                                selectedRoutineId = r.id
                                aiAnalysisResult = null
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = r.name,
                            color = if (isThisSelected) PureWhite else SubtitleWhite,
                            fontSize = 11.sp,
                            fontWeight = if (isThisSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. AI Analysis Button ("Yapay Zeka ile Analiz Et & Kartı Yenile")
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))))
                    .clickable {
                        if (!isAnalyzingWithAi) {
                            triggerAiAnalysis()
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isAnalyzingWithAi) {
                        CircularProgressIndicator(
                            color = PureWhite,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Yapay Zeka Notlarını İnceliyor...",
                            color = PureWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (aiAnalysisResult != null) "✨ Yapay Zeka Özeti Hazır (Yeniden Analiz Et)" else "✨ Yapay Zeka ile Notlarımı Analiz Et & Kartı Oluştur",
                            color = PureWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Live 9:16 Aspect Ratio Composable Preview
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .widthIn(max = 340.dp)
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.5.dp, GlassCardBorderSubtle, RoundedCornerShape(24.dp))
            ) {
                StoryCardLivePreview(data = storyData, modifier = Modifier.fillMaxSize())
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Action Buttons (Share Image / Share Text / Save Image)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary: Share Image to Instagram / WhatsApp
                Button(
                    onClick = {
                        scope.launch {
                            isSharingImage = true
                            val bitmap = withContext(Dispatchers.Default) {
                                StoryCardHelper.createStoryBitmap(context, storyData)
                            }
                            StoryCardHelper.shareStoryImage(context, bitmap, storyData.buildShareText())
                            isSharingImage = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentTeal
                    ),
                    enabled = !isSharingImage
                ) {
                    if (isSharingImage) {
                        CircularProgressIndicator(
                            color = Color(0xFF0F172A),
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Görsel Hazırlanıyor...",
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hikayede Paylaş (Görsel)",
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Row for Secondary actions: Share Text & Save Image
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            StoryCardHelper.shareStoryText(context, storyData.buildShareText())
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0x1AFFFFFF),
                            contentColor = PureWhite
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorderSubtle)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Metin Paylaş",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val bitmap = withContext(Dispatchers.Default) {
                                    StoryCardHelper.createStoryBitmap(context, storyData)
                                }
                                val folder = File(context.cacheDir, "images").apply { mkdirs() }
                                val file = File(folder, "rutin_story_${System.currentTimeMillis()}.png")
                                withContext(Dispatchers.IO) {
                                    FileOutputStream(file).use { out ->
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                    }
                                }
                                Toast.makeText(context, "Hikaye kartı hazırlandı ve galeriye kaydedildi!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0x1AFFFFFF),
                            contentColor = PureWhite
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorderSubtle)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            tint = DoneGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Görseli Kaydet",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * 9:16 Aspect Ratio Live Composable Preview mirroring the generated Story Bitmap
 */
@Composable
private fun StoryCardLivePreview(
    data: StoryCardData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF080614),
                        Color(0xFF100E26),
                        Color(0xFF181A38)
                    )
                )
            )
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "✦ RUTİN • ODAK RAPORU ✦",
                    color = AccentPurple,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (data.targetRoutineName == "Tüm Rutinler") data.periodTitle else data.targetRoutineName,
                    color = PureWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = data.dateSubtitle,
                    color = SubtitleWhite,
                    fontSize = 10.sp
                )
            }

            // Hero Card: Toplam Tutulan Odak Süresi
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0x33A78BFA), Color(0x221E3860))
                        )
                    )
                    .border(1.dp, AccentPurple.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TOPLAM TUTULAN SÜRE",
                        color = AccentTeal,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = data.formattedTotalFocus,
                        color = PureWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (data.isAiGenerated) Color(0x338B5CF6) else Color(0x3310B981))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (data.isAiGenerated) "🤖 Yapay Zeka (Gemini)" else "⏱️ Kaydedilen Odak Seansları",
                            color = if (data.isAiGenerated) AccentPurple else DoneGreen,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Top 3 Accomplishments in this duration
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "🏆 BU SÜREDE NELER YAPILDI? (TOP 3)",
                    color = AccentTeal,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                data.top3Accomplishments.take(3).forEachIndexed { index, item ->
                    val cleanText = item.removePrefix("•").trim()
                    val badgeColor = when (index) {
                        0 -> AccentTeal
                        1 -> AccentPurple
                        else -> Color(0xFFFBBF24)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x22FFFFFF))
                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(badgeColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = Color(0xFF0F172A),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = cleanText,
                            color = PureWhite,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Motivational Quote
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x26201944))
                    .border(1.dp, AccentPurple.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = data.achievementBadge,
                        color = AccentTeal,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "\"${data.motivationalQuote}\"",
                        color = PureWhite,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Footer
            Text(
                text = "RUTİN APP • DİSİPLİN VE ODAK 🚀",
                color = MutedWhite,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
