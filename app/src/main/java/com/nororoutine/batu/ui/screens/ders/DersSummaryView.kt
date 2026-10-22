package com.nororoutine.batu.ui.screens.ders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import com.nororoutine.batu.model.DersQuestionType
import com.nororoutine.batu.ui.theme.AccentPurple
import com.nororoutine.batu.ui.theme.AccentTeal
import com.nororoutine.batu.ui.theme.GlassCardBorderSubtle
import com.nororoutine.batu.ui.theme.PureWhite
import com.nororoutine.batu.ui.theme.SubtitleWhite
import com.nororoutine.batu.viewmodel.DersUiState

@Composable
fun DersSummaryView(
    uiState: DersUiState,
    onGenerateNewQuestions: () -> Unit,
    onStartNewDocument: () -> Unit,
    modifier: Modifier = Modifier
) {
    val total = uiState.questions.size
    val correct = uiState.questions.count { uiState.isCorrectMap[it.id] == true }
    val incorrect = total - correct
    val successPct = if (total > 0) (correct * 100) / total else 0

    var isReviewExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF16132F))
                    .border(1.dp, AccentTeal.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Test Tamamlandı! 🎯",
                            color = PureWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0x3306B6D4))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "%$successPct Başarı",
                                color = AccentTeal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Score pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x22FFFFFF))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$correct",
                                color = Color(0xFF10B981),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Doğru",
                                color = SubtitleWhite,
                                fontSize = 11.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(30.dp)
                                .width(1.dp)
                                .background(GlassCardBorderSubtle)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$incorrect",
                                color = if (incorrect > 0) Color(0xFFEF4444) else SubtitleWhite,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Yanlış",
                                color = SubtitleWhite,
                                fontSize = 11.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(30.dp)
                                .width(1.dp)
                                .background(GlassCardBorderSubtle)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$total",
                                color = PureWhite,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Toplam Soru",
                                color = SubtitleWhite,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Missing Topics & Recommended Areas
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E1B38))
                    .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Eksik Konular & Analiz",
                            color = PureWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.missingTopics.isNotEmpty()) {
                        uiState.missingTopics.forEach { topic ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFBBF24))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = topic,
                                    color = PureWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Tüm soruları başarıyla cevapladınız! Belgede belirgin bir eksik konu saptanmadı.",
                            color = Color(0xFF10B981),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.MenuBook,
                            contentDescription = null,
                            tint = AccentTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tekrar Çalışılması Önerilen Konular:",
                            color = AccentTeal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (uiState.recommendedStudyTopics.isNotEmpty()) {
                        uiState.recommendedStudyTopics.forEach { rec ->
                            Text(
                                text = "• $rec",
                                color = SubtitleWhite,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp)
                            )
                        }
                    }

                    if (uiState.coachFeedback.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x33A78BFA))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Koç Tavsiyesi: ${uiState.coachFeedback}",
                                color = PureWhite,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons: Regenerate same topic & new document
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Feature: "Kullanıcı tekrar isterse aynı konudan yeni sorular üretir"
                Button(
                    onClick = onGenerateNewQuestions,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Aynı Konudan Yeni Sorular Üret 🔄",
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onStartNewDocument,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.UploadFile,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Farklı Bir Belge Yükle",
                        color = PureWhite,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Review All Questions Toggle
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { isReviewExpanded = !isReviewExpanded }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tüm Soruları ve Cevapları İncele",
                    color = PureWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isReviewExpanded) "Gizle ▲" else "Göster ▼",
                    color = AccentTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (isReviewExpanded) {
            itemsIndexed(uiState.questions) { idx, q ->
                val isAnswerCorrect = uiState.isCorrectMap[q.id] == true
                val userAnswer = uiState.userAnswers[q.id] ?: "Cevapsız"

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1B1834))
                        .border(
                            1.dp,
                            if (isAnswerCorrect) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFFEF4444).copy(alpha = 0.5f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${idx + 1}. Soru • ${q.type.shortBadge}",
                                color = PureWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isAnswerCorrect) Color(0x3310B981) else Color(0x33EF4444)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isAnswerCorrect) "Doğru ✓" else "Yanlış ✗",
                                    color = if (isAnswerCorrect) Color(0xFF10B981) else Color(0xFFEF4444),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = q.questionText,
                            color = PureWhite,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Sizin Cevabınız: $userAnswer",
                            color = if (isAnswerCorrect) Color(0xFF10B981) else Color(0xFFEF4444),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (!isAnswerCorrect) {
                            val rightAnsStr = when (q.type) {
                                DersQuestionType.MULTIPLE_CHOICE -> {
                                    val letter = listOf("A", "B", "C", "D").getOrElse(q.correctOptionIndex) { "A" }
                                    "$letter) ${q.options.getOrElse(q.correctOptionIndex) { "" }}"
                                }
                                DersQuestionType.TRUE_FALSE -> if (q.isTrue) "Doğru" else "Yanlış"
                                DersQuestionType.FILL_IN_THE_BLANK, DersQuestionType.SHORT_ANSWER -> q.correctAnswerText
                                DersQuestionType.MATCHING -> q.matchingPairs.joinToString(", ") { "${it.left} ↔ ${it.right}" }
                            }
                            Text(
                                text = "Doğru Cevap: $rightAnsStr",
                                color = Color(0xFF10B981),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x22FFFFFF))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Açıklama: ${q.explanation}",
                                color = SubtitleWhite,
                                fontSize = 10.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
