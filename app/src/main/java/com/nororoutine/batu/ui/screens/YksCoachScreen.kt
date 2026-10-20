package com.nororoutine.batu.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nororoutine.batu.ui.screens.yks.AddExamBottomSheet
import com.nororoutine.batu.ui.screens.yks.CoachChatView
import com.nororoutine.batu.ui.screens.yks.ExamAnalysisView
import com.nororoutine.batu.ui.screens.yks.StudySectionView
import com.nororoutine.batu.ui.theme.AccentPurple
import com.nororoutine.batu.ui.theme.AccentTeal
import com.nororoutine.batu.ui.theme.GlassCardBorderSubtle
import com.nororoutine.batu.ui.theme.PureWhite
import com.nororoutine.batu.ui.theme.SubtitleWhite
import com.nororoutine.batu.viewmodel.RutinViewModel
import com.nororoutine.batu.viewmodel.YksCoachViewModel
import java.time.LocalDate

@Composable
fun YksCoachScreen(
    coachViewModel: YksCoachViewModel = viewModel(),
    rutinViewModel: RutinViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by coachViewModel.uiState.collectAsState()
    val examRecords by coachViewModel.examRecords.collectAsState()
    val studySessions by coachViewModel.studySessions.collectAsState()
    val chatHistory by coachViewModel.chatHistory.collectAsState()
    val rutinUiState by rutinViewModel.uiState.collectAsState()

    // Context from routines and weekly stats
    val today = remember { LocalDate.now() }
    val routinesSummary = remember(rutinUiState.routines, rutinUiState.logs) {
        if (rutinUiState.routines.isEmpty()) {
            "Henüz eklenmiş rutin yok."
        } else {
            rutinUiState.routines.joinToString("\n") { r ->
                val logKey = "${today}_${r.id}"
                val isDone = rutinUiState.logs[logKey]?.isDone == true
                "- ${r.name} (${r.startTime.ifEmpty { r.time }}): ${if (isDone) "Bugün Tamamlandı ✓" else "Henüz Yapılmadı"}"
            }
        }
    }

    val currentStreak = rutinUiState.stats.currentStreak
    val weeklySummary = remember(currentStreak, rutinUiState.routines) {
        "Alışkanlık Serisi: $currentStreak Gün. Toplam Rutin Sayısı: ${rutinUiState.routines.size}."
    }

    // Back handling: pop active study first, then subtabs
    BackHandler(enabled = uiState.activeStudySession != null || uiState.currentSubTab != 0) {
        if (uiState.activeStudySession != null) {
            coachViewModel.closeActiveStudy()
        } else if (uiState.currentSubTab != 0) {
            coachViewModel.setSubTab(0)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // --- SCREEN HEADER (NAME: "Ders") ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentTeal.copy(alpha = 0.2f))
                        .border(1.dp, AccentTeal.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.School,
                        contentDescription = null,
                        tint = AccentTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "Ders",
                        color = PureWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Yapay Zeka Koçluk, Deneme Analizi & Çalışma",
                        color = SubtitleWhite,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // --- SUBTABS: EXACTLY 3 TABS (Sohbet, Deneme, Çalışma) ---
        // As requested: "kısıma girince üstte sadece şu 3 şey olsun sohbet, deneme, Çalışma. GRAFİK DİYE BİR KISIM OLMAAYACAK."
        val subTabs = listOf(
            Triple("Sohbet", Icons.Rounded.Forum, 0),
            Triple("Deneme", Icons.Rounded.BarChart, 1),
            Triple("Çalışma", Icons.Rounded.MenuBook, 2)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            subTabs.forEach { (title, icon, index) ->
                val isSelected = uiState.currentSubTab == index

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) AccentTeal.copy(alpha = 0.22f)
                            else Color(0x33171330)
                        )
                        .border(
                            1.dp,
                            if (isSelected) AccentTeal else GlassCardBorderSubtle,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { coachViewModel.setSubTab(index) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (isSelected) AccentTeal else SubtitleWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            color = if (isSelected) AccentTeal else SubtitleWhite,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- SUB-SCREEN CONTENT ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            AnimatedContent(
                targetState = uiState.currentSubTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "subTabTransition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> CoachChatView(
                        chatHistory = chatHistory,
                        isThinking = uiState.isAiThinking,
                        routinesSummary = routinesSummary,
                        weeklySummary = weeklySummary,
                        onSendMessage = { text, rSum, wSum ->
                            coachViewModel.sendMessage(text, rSum, wSum)
                        },
                        onClearChat = { coachViewModel.clearChat() }
                    )
                    1 -> ExamAnalysisView(
                        exams = examRecords,
                        selectedExam = uiState.selectedExam,
                        isAnalyzing = uiState.isAnalyzingExam,
                        onSelectExam = { coachViewModel.selectExam(it) },
                        onAnalyzeExam = { coachViewModel.analyzeExam(it) },
                        onDeleteExam = { coachViewModel.deleteExam(it) },
                        onOpenAddSheet = { coachViewModel.setShowAddExamSheet(true) },
                        onDiscussInChat = { prompt ->
                            coachViewModel.setSubTab(0)
                            coachViewModel.sendMessage(prompt, routinesSummary, weeklySummary)
                        }
                    )
                    2 -> StudySectionView(
                        pastStudies = studySessions,
                        activeSession = uiState.activeStudySession,
                        currentQuestionIndex = uiState.currentQuestionIndex,
                        selectedAnswers = uiState.selectedAnswers,
                        revealedHintIds = uiState.revealedHintIds,
                        isGenerating = uiState.isGeneratingStudyQuiz,
                        isAnalyzing = uiState.isAnalyzingStudy,
                        onStartQuiz = { desc, count, b64, uri, doc ->
                            coachViewModel.startGeneratingStudyQuiz(desc, count, b64, uri, doc)
                        },
                        onSelectAnswer = { qId, optIndex ->
                            coachViewModel.selectStudyAnswer(qId, optIndex)
                        },
                        onRevealHint = { qId ->
                            coachViewModel.revealStudyHint(qId)
                        },
                        onNextQuestion = {
                            coachViewModel.goToNextQuestion()
                        },
                        onPreviousQuestion = {
                            coachViewModel.goToPreviousQuestion()
                        },
                        onSetQuestionIndex = { idx ->
                            coachViewModel.setQuestionIndex(idx)
                        },
                        onFinishQuiz = {
                            coachViewModel.finishStudyQuiz()
                        },
                        onViewPastStudy = { session ->
                            coachViewModel.viewPastStudySession(session)
                        },
                        onDeletePastStudy = { sId ->
                            coachViewModel.deletePastStudy(sId)
                        },
                        onCloseActiveStudy = {
                            coachViewModel.closeActiveStudy()
                        }
                    )
                }
            }
        }
    }

    // Modal Sheet for adding a new trial exam
    if (uiState.showAddExamSheet) {
        AddExamBottomSheet(
            onDismiss = { coachViewModel.setShowAddExamSheet(false) },
            onSave = { title, date, type, results, notes ->
                coachViewModel.saveNewExam(title, date, type, results, notes)
            }
        )
    }
}
