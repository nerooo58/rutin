package com.nororoutine.batu.ui.screens.ders

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nororoutine.batu.model.DersFlowStep
import com.nororoutine.batu.ui.theme.AccentTeal
import com.nororoutine.batu.ui.theme.PureWhite
import com.nororoutine.batu.ui.theme.SubtitleWhite
import com.nororoutine.batu.viewmodel.DersViewModel

@Composable
fun DersScreen(
    dersViewModel: DersViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by dersViewModel.uiState.collectAsState()

    // Back handling based on current flow
    BackHandler(enabled = uiState.viewingPastSession != null || uiState.flowStep != DersFlowStep.UPLOAD) {
        when {
            uiState.viewingPastSession != null -> dersViewModel.closePastSessionView()
            uiState.flowStep == DersFlowStep.QUESTION_ACTIVE || uiState.flowStep == DersFlowStep.CONFIRMATION || uiState.flowStep == DersFlowStep.SUMMARY -> {
                dersViewModel.startNewDocument()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // App / Section Header: "Ders"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
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
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Belge & Not Analizi • Maarif Modeli Soru Asistanı",
                        color = SubtitleWhite,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Animated Step Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            AnimatedContent(
                targetState = uiState.flowStep,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "dersFlowTransition"
            ) { step ->
                when (step) {
                    DersFlowStep.UPLOAD -> DersUploadView(
                        uiState = uiState,
                        onSetImage = { uri, b64 -> dersViewModel.setUploadedImage(uri, b64) },
                        onRemoveImage = { dersViewModel.removeUploadedImage() },
                        onSetDocument = { name, text -> dersViewModel.setDocumentFile(name, text) },
                        onRemoveDocument = { dersViewModel.removeDocumentFile() },
                        onSetNotesText = { dersViewModel.setManualNotesText(it) },
                        onStartAnalysis = { dersViewModel.startDocumentAnalysis() },
                        onViewPastSession = { dersViewModel.viewPastSession(it) },
                        onDeletePastSession = { dersViewModel.deletePastSession(it) }
                    )

                    DersFlowStep.ANALYZING -> DersAnalyzingView(
                        title = if (uiState.isGeneratingQuestions) "Sorular Hazırlanıyor..." else "Belgeler Analiz Ediliyor...",
                        subtitle = if (uiState.isGeneratingQuestions) "5 farklı soru tipi belgeden dengeli biçimde üretiliyor" else "Belgedeki temel kazanımlar ve kavram haritası çıkarılıyor"
                    )

                    DersFlowStep.CONFIRMATION -> {
                        val summary = uiState.analysisSummary
                        if (summary != null) {
                            DersConfirmationView(
                                summary = summary,
                                selectedCount = uiState.selectedQuestionCount,
                                onSelectCount = { dersViewModel.setQuestionCount(it) },
                                onConfirmStart = { dersViewModel.startQuizSession() },
                                onBackToUpload = { dersViewModel.startNewDocument() }
                            )
                        } else {
                            dersViewModel.startNewDocument()
                        }
                    }

                    DersFlowStep.QUESTION_ACTIVE -> DersQuestionView(
                        uiState = uiState,
                        onSelectOption = { dersViewModel.selectOption(it) },
                        onSelectBoolean = { dersViewModel.selectBoolean(it) },
                        onSetTextInput = { dersViewModel.setTextInputAnswer(it) },
                        onSelectMatchingLeft = { dersViewModel.selectMatchingLeft(it) },
                        onLinkMatchingRight = { dersViewModel.linkMatchingRight(it) },
                        onRemoveMatchingPair = { dersViewModel.removeMatchingPair(it) },
                        onRevealHint = { dersViewModel.revealHint() },
                        onSubmitAnswer = { dersViewModel.submitCurrentAnswer() },
                        onNextOrFinish = { dersViewModel.goToNextQuestionOrFinish() },
                        onExitQuiz = { dersViewModel.startNewDocument() }
                    )

                    DersFlowStep.SUMMARY -> DersSummaryView(
                        uiState = uiState,
                        onGenerateNewQuestions = { dersViewModel.generateNewQuestionsOnSameTopic() },
                        onStartNewDocument = { dersViewModel.startNewDocument() }
                    )
                }
            }
        }
    }

    // Past session review modal sheet
    val viewingPast = uiState.viewingPastSession
    if (viewingPast != null) {
        DersPastSessionDetailSheet(
            session = viewingPast,
            onDismiss = { dersViewModel.closePastSessionView() }
        )
    }
}
