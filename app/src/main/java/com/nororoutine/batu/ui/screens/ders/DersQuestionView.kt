package com.nororoutine.batu.ui.screens.ders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nororoutine.batu.model.DersQuestion
import com.nororoutine.batu.model.DersQuestionType
import com.nororoutine.batu.ui.theme.AccentPurple
import com.nororoutine.batu.ui.theme.AccentTeal
import com.nororoutine.batu.ui.theme.GlassCardBorderSubtle
import com.nororoutine.batu.ui.theme.PureWhite
import com.nororoutine.batu.ui.theme.SubtitleWhite
import com.nororoutine.batu.viewmodel.DersUiState

@Composable
fun DersQuestionView(
    uiState: DersUiState,
    onSelectOption: (Int) -> Unit,
    onSelectBoolean: (Boolean) -> Unit,
    onSetTextInput: (String) -> Unit,
    onSelectMatchingLeft: (Int) -> Unit,
    onLinkMatchingRight: (Int) -> Unit,
    onRemoveMatchingPair: (Int) -> Unit,
    onRevealHint: () -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextOrFinish: () -> Unit,
    onExitQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    val questions = uiState.questions
    val currentIndex = uiState.currentQuestionIndex
    val question = questions.getOrNull(currentIndex) ?: return
    val totalCount = questions.size

    val isSubmitted = uiState.isCurrentAnswerSubmitted
    val isCorrect = uiState.isCurrentAnswerCorrect == true
    val isLast = currentIndex == totalCount - 1

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .navigationBarsPadding()
    ) {
        // Top Header: Counter, Type Badge & Exit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Soru ${currentIndex + 1} / $totalCount",
                        color = AccentTeal,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentPurple.copy(alpha = 0.25f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = question.type.shortBadge,
                            color = AccentPurple,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = question.topic,
                    color = SubtitleWhite,
                    fontSize = 11.sp
                )
            }

            IconButton(onClick = onExitQuiz) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Testi Kapat",
                    tint = SubtitleWhite
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = { (currentIndex + 1).toFloat() / totalCount.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = AccentTeal,
            trackColor = Color(0x33FFFFFF)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Scrollable Question Body
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Question Statement Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1B38))
                        .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = question.questionText,
                            color = PureWhite,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Hint Feature (Yapay zeka cevabı vermeden ipucu verir)
            item {
                if (!uiState.isHintRevealed) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x22FBBF24))
                            .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable { onRevealHint() }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Lightbulb,
                                contentDescription = "İpucu",
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "💡 Bu Soru İçin İpucu İste",
                                color = Color(0xFFFBBF24),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x22F59E0B))
                            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Lightbulb,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Yapay Zeka Pedagojik İpucu:",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = question.hint,
                                color = PureWhite,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            // Interactive Answer Area depending on question type
            when (question.type) {
                DersQuestionType.MULTIPLE_CHOICE -> {
                    // 4 Options (A, B, C, D)
                    val optionLetters = listOf("A", "B", "C", "D")
                    itemsIndexed(question.options.take(4)) { idx, optText ->
                        val isSelected = uiState.selectedOptionIndex == idx
                        val isAnswer = question.correctOptionIndex == idx
                        val letter = optionLetters.getOrElse(idx) { "${idx + 1}" }

                        val cardBg = when {
                            isSubmitted && isAnswer -> Color(0x3310B981)
                            isSubmitted && isSelected && !isCorrect -> Color(0x33EF4444)
                            isSelected -> AccentTeal.copy(alpha = 0.22f)
                            else -> Color(0xFF16132C)
                        }

                        val borderColor = when {
                            isSubmitted && isAnswer -> Color(0xFF10B981)
                            isSubmitted && isSelected && !isCorrect -> Color(0xFFEF4444)
                            isSelected -> AccentTeal
                            else -> GlassCardBorderSubtle
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(cardBg)
                                .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                                .clickable(enabled = !isSubmitted) { onSelectOption(idx) }
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSubmitted && isAnswer -> Color(0xFF10B981)
                                                isSubmitted && isSelected && !isCorrect -> Color(0xFFEF4444)
                                                isSelected -> AccentTeal
                                                else -> Color(0x22FFFFFF)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = letter,
                                        color = if (isSelected || (isSubmitted && isAnswer)) Color.Black else PureWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = optText.removePrefix("$letter) ").removePrefix("$letter-) "),
                                    color = PureWhite,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                DersQuestionType.TRUE_FALSE -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // DOĞRU Button
                            val isTrueSelected = uiState.selectedBoolean == true
                            val trueBg = when {
                                isSubmitted && question.isTrue -> Color(0x3310B981)
                                isSubmitted && isTrueSelected && !question.isTrue -> Color(0x33EF4444)
                                isTrueSelected -> Color(0x3310B981)
                                else -> Color(0xFF16132C)
                            }
                            val trueBorder = when {
                                isSubmitted && question.isTrue -> Color(0xFF10B981)
                                isSubmitted && isTrueSelected && !question.isTrue -> Color(0xFFEF4444)
                                isTrueSelected -> Color(0xFF10B981)
                                else -> GlassCardBorderSubtle
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(58.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(trueBg)
                                    .border(1.dp, trueBorder, RoundedCornerShape(14.dp))
                                    .clickable(enabled = !isSubmitted) { onSelectBoolean(true) },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "DOĞRU",
                                        color = PureWhite,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // YANLIŞ Button
                            val isFalseSelected = uiState.selectedBoolean == false
                            val falseBg = when {
                                isSubmitted && !question.isTrue -> Color(0x3310B981)
                                isSubmitted && isFalseSelected && question.isTrue -> Color(0x33EF4444)
                                isFalseSelected -> Color(0x33EF4444)
                                else -> Color(0xFF16132C)
                            }
                            val falseBorder = when {
                                isSubmitted && !question.isTrue -> Color(0xFF10B981)
                                isSubmitted && isFalseSelected && question.isTrue -> Color(0xFFEF4444)
                                isFalseSelected -> Color(0xFFEF4444)
                                else -> GlassCardBorderSubtle
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(58.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(falseBg)
                                    .border(1.dp, falseBorder, RoundedCornerShape(14.dp))
                                    .clickable(enabled = !isSubmitted) { onSelectBoolean(false) },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "YANLIŞ",
                                        color = PureWhite,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                DersQuestionType.FILL_IN_THE_BLANK, DersQuestionType.SHORT_ANSWER -> {
                    item {
                        Column {
                            Text(
                                text = if (question.type == DersQuestionType.FILL_IN_THE_BLANK) "Cümledeki boşluğa gelecek kavramı yazınız:" else "Kısa cevabınızı yazınız:",
                                color = SubtitleWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = uiState.textInputAnswer,
                                onValueChange = { onSetTextInput(it) },
                                enabled = !isSubmitted,
                                placeholder = {
                                    Text("Cevabınızı buraya yazınız...", color = SubtitleWhite.copy(alpha = 0.5f), fontSize = 12.sp)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentTeal,
                                    unfocusedBorderColor = GlassCardBorderSubtle,
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = PureWhite
                                )
                            )

                            if (isSubmitted) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Beklenen Cevap: ${question.correctAnswerText}",
                                    color = if (isCorrect) Color(0xFF10B981) else Color(0xFFFBBF24),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                DersQuestionType.MATCHING -> {
                    item {
                        Column {
                            Text(
                                text = "Sol taraftaki kavramı seçip sağdaki eşleşen tanıma dokununuz:",
                                color = SubtitleWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val pairs = question.matchingPairs
                            // Shuffled right elements (fixed order per question)
                            val shuffledRight = remember(question.id) {
                                pairs.mapIndexed { idx, p -> idx to p.right }.shuffled()
                            }

                            pairs.forEachIndexed { leftIdx, pair ->
                                val isSelectedLeft = uiState.selectedMatchingLeft == leftIdx
                                val matchedRightIdx = uiState.matchingUserPairs[leftIdx]
                                val matchedRightText = matchedRightIdx?.let { pairs.getOrNull(it)?.right }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelectedLeft) AccentTeal.copy(alpha = 0.2f)
                                            else if (matchedRightIdx != null) Color(0xFF1D193E)
                                            else Color(0xFF15132A)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelectedLeft) AccentTeal else if (matchedRightIdx != null) AccentPurple else GlassCardBorderSubtle,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable(enabled = !isSubmitted) {
                                            if (matchedRightIdx != null) {
                                                onRemoveMatchingPair(leftIdx)
                                            } else {
                                                onSelectMatchingLeft(leftIdx)
                                            }
                                        }
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${leftIdx + 1}. ${pair.left}",
                                            color = PureWhite,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        if (matchedRightText != null) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "➔ $matchedRightText",
                                                    color = AccentTeal,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (!isSubmitted) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(
                                                        imageVector = Icons.Rounded.Close,
                                                        contentDescription = "Eşleşmeyi Kaldır",
                                                        tint = Color(0xFFEF4444),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = if (isSelectedLeft) "Sağdan Eşini Seçin" else "Seçmek için dokunun",
                                                color = if (isSelectedLeft) AccentTeal else SubtitleWhite,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Right side choices
                            if (!isSubmitted && uiState.selectedMatchingLeft != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Eşleşecek Tanımı Seçiniz:",
                                    color = AccentTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                shuffledRight.forEach { (actualIndex, rightText) ->
                                    val isAlreadyUsed = uiState.matchingUserPairs.values.contains(actualIndex)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isAlreadyUsed) Color(0x11FFFFFF) else Color(0x33A78BFA))
                                            .border(1.dp, if (isAlreadyUsed) Color.Transparent else AccentPurple, RoundedCornerShape(8.dp))
                                            .clickable(enabled = !isAlreadyUsed) {
                                                onLinkMatchingRight(actualIndex)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = rightText,
                                            color = if (isAlreadyUsed) SubtitleWhite.copy(alpha = 0.4f) else PureWhite,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Immediate Feedback & Short Explanation (Step 4)
            if (isSubmitted) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isCorrect) Color(0x2210B981) else Color(0x22EF4444)
                            )
                            .border(
                                1.dp,
                                if (isCorrect) Color(0xFF10B981) else Color(0xFFEF4444),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isCorrect) Icons.Rounded.CheckCircle else Icons.Rounded.Close,
                                    contentDescription = null,
                                    tint = if (isCorrect) Color(0xFF10B981) else Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isCorrect) "Tebrikler, Doğru Cevap! 🎉" else "Yanlış Cevap ❌",
                                    color = if (isCorrect) Color(0xFF10B981) else Color(0xFFEF4444),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "📌 Açıklama: ${question.explanation}",
                                color = PureWhite,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Buttons at Bottom
        if (!isSubmitted) {
            val canSubmit = when (question.type) {
                DersQuestionType.MULTIPLE_CHOICE -> uiState.selectedOptionIndex != null
                DersQuestionType.TRUE_FALSE -> uiState.selectedBoolean != null
                DersQuestionType.FILL_IN_THE_BLANK, DersQuestionType.SHORT_ANSWER -> uiState.textInputAnswer.isNotBlank()
                DersQuestionType.MATCHING -> uiState.matchingUserPairs.size == question.matchingPairs.size
            }

            Button(
                onClick = onSubmitAnswer,
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentTeal,
                    disabledContainerColor = Color(0x3306B6D4)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Cevabı Onayla",
                    color = if (canSubmit) Color.Black else SubtitleWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Button(
                onClick = onNextOrFinish,
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = if (isLast) "Testi Tamamla & Analizi Gör 🎯" else "Sonraki Soruya Geç ➔",
                    color = Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
