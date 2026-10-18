package com.nororoutine.batu.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import com.nororoutine.batu.model.GraphPoint
import com.nororoutine.batu.model.MathSolution
import com.nororoutine.batu.model.QuestionGraphInfo
import com.nororoutine.batu.ui.components.InteractiveGraphCanvas
import com.nororoutine.batu.ui.components.NetProgressionChart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nororoutine.batu.model.ChatMessage
import com.nororoutine.batu.model.DeficiencyItem
import com.nororoutine.batu.model.ExamSubjectResult
import com.nororoutine.batu.model.QuizQuestion
import com.nororoutine.batu.model.YksExamRecord
import com.nororoutine.batu.model.YksExamType
import com.nororoutine.batu.ui.theme.AccentPurple
import com.nororoutine.batu.ui.theme.AccentTeal
import com.nororoutine.batu.ui.theme.DoneGreen
import com.nororoutine.batu.ui.theme.GlassCardBorderSubtle
import com.nororoutine.batu.ui.theme.MissedRed
import com.nororoutine.batu.ui.theme.MutedWhite
import com.nororoutine.batu.ui.theme.PureWhite
import com.nororoutine.batu.ui.theme.SubtitleWhite
import com.nororoutine.batu.viewmodel.YksCoachViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YksCoachScreen(
    coachViewModel: YksCoachViewModel = viewModel()
) {
    val uiState by coachViewModel.uiState.collectAsState()
    val examRecords by coachViewModel.examRecords.collectAsState()
    val chatHistory by coachViewModel.chatHistory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // --- TOP HEADER: 2028 Maarif Modeli Brand & Tag ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(AccentTeal, AccentPurple)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.School,
                            contentDescription = "YKS Koç",
                            tint = PureWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "2028 YKS & Maarif Modeli",
                            color = PureWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Yapay Zeka Çalışma & Başarı Koçu",
                            color = SubtitleWhite,
                            fontSize = 12.sp
                        )
                    }
                }

                // Maarif model badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentTeal.copy(alpha = 0.15f))
                        .border(1.dp, AccentTeal.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "MEB 2028",
                        color = AccentTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 4 SUB-TABS: Koç Sohbeti | Deneme Analizleri | Grafik & Mat Lab | Soru Çöz ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x331E1B4B))
                    .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val tabs = listOf(
                    Triple(0, "Sohbet", Icons.Rounded.Psychology),
                    Triple(1, "Denemeler", Icons.Rounded.TrendingUp),
                    Triple(2, "Grafik & Mat", Icons.Rounded.ShowChart),
                    Triple(3, "Soru Çöz", Icons.Rounded.Quiz)
                )

                tabs.forEach { (index, title, icon) ->
                    val isSelected = uiState.currentSubTab == index
                    val tabBg by animateColorAsState(
                        targetValue = if (isSelected) AccentTeal.copy(alpha = 0.25f) else Color.Transparent,
                        label = "tabBg"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) PureWhite else SubtitleWhite,
                        label = "tabContent"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(tabBg)
                            .clickable { coachViewModel.setSubTab(index) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = contentColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = title,
                                color = contentColor,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // --- SUB-SCREEN CONTENT ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (uiState.currentSubTab) {
                0 -> CoachChatView(
                    chatHistory = chatHistory,
                    isThinking = uiState.isAiThinking,
                    onSendMessage = { coachViewModel.sendMessage(it) },
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
                    onSolveDeficiencyQuestions = { deficiency ->
                        coachViewModel.startQuizForDeficiency(deficiency)
                    }
                )
                2 -> MathGraphLabView(
                    mathProblem = uiState.mathProblemInput,
                    mathSolution = uiState.mathSolution,
                    isSolving = uiState.isSolvingMath,
                    customA = uiState.customGraphA,
                    customB = uiState.customGraphB,
                    customC = uiState.customGraphC,
                    customType = uiState.customGraphType,
                    onProblemChange = { coachViewModel.setMathProblemInput(it) },
                    onSolveProblem = { coachViewModel.solveMathProblem(it) },
                    onUpdateGraph = { a, b, c, t -> coachViewModel.updateCustomGraph(a, b, c, t) }
                )
                3 -> MaarifQuizView(
                    questions = uiState.quizQuestions,
                    isLoading = uiState.isLoadingQuiz,
                    subject = uiState.quizSubject,
                    topic = uiState.quizTopic,
                    onGenerateQuestions = { s, t, c -> coachViewModel.generateQuiz(s, t, c) },
                    onSelectAnswer = { qId, index -> coachViewModel.answerQuizQuestion(qId, index) }
                )
            }
        }
    }

    // Modal Sheet for adding a new trial exam
    if (uiState.showAddExamSheet) {
        AddExamBottomSheet(
            onDismiss = { coachViewModel.setShowAddExamSheet(false) },
            onSave = { title, type, results, notes ->
                coachViewModel.saveNewExam(title, type, results, notes)
            }
        )
    }
}

// =========================================================================
// TAB 0: COACH CHAT VIEW
// =========================================================================

@Composable
private fun CoachChatView(
    chatHistory: List<ChatMessage>,
    isThinking: Boolean,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit
) {
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(chatHistory.size, isThinking) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(chatHistory, key = { it.id }) { message ->
                ChatMessageItem(
                    message = message,
                    onChipClick = { onSendMessage(it) }
                )
            }

            if (isThinking) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = AccentTeal,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Maarif Modeli Koçu düşünüyor...",
                            color = AccentTeal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xD90E0D1E))
                .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        text = "Koça bir soru sor veya konu yaz...",
                        color = MutedWhite,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentTeal,
                    unfocusedBorderColor = GlassCardBorderSubtle,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite,
                    focusedContainerColor = Color(0x331E1B4B),
                    unfocusedContainerColor = Color(0x331E1B4B)
                ),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText)
                        inputText = ""
                    }
                })
            )

            Spacer(modifier = Modifier.width(10.dp))

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText)
                        inputText = ""
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(AccentTeal, AccentPurple)
                        )
                    )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Send,
                    contentDescription = "Gönder",
                    tint = PureWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    onChipClick: (String) -> Unit
) {
    val isUser = message.sender == "user"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(AccentTeal.copy(alpha = 0.2f))
                        .border(1.dp, AccentTeal, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "AI",
                        tint = AccentTeal,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .background(
                        if (isUser) {
                            Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF4338CA)))
                        } else {
                            Brush.linearGradient(listOf(Color(0x551E1B4B), Color(0x33121028)))
                        }
                    )
                    .border(
                        1.dp,
                        if (isUser) AccentPurple.copy(alpha = 0.4f) else GlassCardBorderSubtle,
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.text,
                    color = PureWhite,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                )
            }
        }

        // Suggested Chips
        if (!isUser && message.suggestedChips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(start = 38.dp, end = 12.dp)
            ) {
                items(message.suggestedChips) { chip ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(AccentTeal.copy(alpha = 0.12f))
                            .border(1.dp, AccentTeal.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .clickable { onChipClick(chip) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = chip,
                            color = AccentTeal,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 1: EXAM ANALYSIS VIEW
// =========================================================================

@Composable
private fun ExamAnalysisView(
    exams: List<YksExamRecord>,
    selectedExam: YksExamRecord?,
    isAnalyzing: Boolean,
    onSelectExam: (YksExamRecord) -> Unit,
    onAnalyzeExam: (YksExamRecord) -> Unit,
    onDeleteExam: (String) -> Unit,
    onOpenAddSheet: () -> Unit,
    onSolveDeficiencyQuestions: (DeficiencyItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Exam Selector & Add button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kayıtlı Denemeler",
                    color = PureWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = onOpenAddSheet,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Ekle",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Yeni Deneme",
                        color = Color(0xFF0F172A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Net Progression Line Chart (Visual analytics across denemeler)
        if (exams.isNotEmpty()) {
            item {
                NetProgressionChart(
                    exams = exams,
                    selectedExam = selectedExam,
                    onSelectExam = onSelectExam
                )
            }
        }

        // Horizontal Exam Chips
        if (exams.isNotEmpty()) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(exams) { exam ->
                        val isSelected = selectedExam?.id == exam.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) AccentPurple.copy(alpha = 0.3f) else Color(0x331E1B4B)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) AccentTeal else GlassCardBorderSubtle,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { onSelectExam(exam) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column {
                                Text(
                                    text = exam.title,
                                    color = PureWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "%.1f Net".format(exam.totalNet),
                                        color = AccentTeal,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = " • ${exam.date}",
                                        color = SubtitleWhite,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedExam != null) {
            // Selected Exam Score Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1E1B4B), Color(0xFF131032))
                            )
                        )
                        .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = selectedExam.title,
                                    color = PureWhite,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = selectedExam.examType.displayName,
                                    color = SubtitleWhite,
                                    fontSize = 12.sp
                                )
                            }

                            IconButton(
                                onClick = { onDeleteExam(selectedExam.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Sil",
                                    tint = SubtitleWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Subject Nets Summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            selectedExam.results.forEach { res ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = res.subjectName.take(6),
                                        color = SubtitleWhite,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "%.1f".format(res.net),
                                        color = AccentTeal,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${res.correct}D ${res.incorrect}Y",
                                        color = MutedWhite,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Total Net Banner
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x33000000))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Toplam Deneme Neti:",
                                color = PureWhite,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "%.2f Net".format(selectedExam.totalNet),
                                color = DoneGreen,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // AI Analysis Section
            item {
                if (selectedExam.aiAnalysis == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x221E1B4B))
                        .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(20.dp))
                        .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = AccentTeal,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "2028 Maarif Modeli Eksik Analizi",
                                color = PureWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Yapay zeka netlerini inceleyip en çok puan kaybettiğin kritik konuları ve çözüm reçetesini çıkarsın.",
                                color = SubtitleWhite,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { onAnalyzeExam(selectedExam) },
                                enabled = !isAnalyzing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentPurple
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                if (isAnalyzing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = PureWhite,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Analiz Ediliyor...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Yapay Zeka ile Analiz Et", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    val analysis = selectedExam.aiAnalysis
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Standing & Summary Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0x334F46E5), Color(0x220EA5E9))
                                    )
                                )
                                .border(1.dp, AccentTeal.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lightbulb,
                                        contentDescription = null,
                                        tint = AccentTeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = analysis.estimatedStanding,
                                        color = AccentTeal,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = analysis.summary,
                                    color = PureWhite,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp
                                )
                            }
                        }

                        // Critical Deficiencies Card
                        Text(
                            text = "🎯 Tespit Edilen Kritik Eksikler",
                            color = PureWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        analysis.criticalDeficiencies.forEach { def ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0x331E1B4B))
                                    .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${def.subject} • ${def.topic}",
                                            color = PureWhite,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (def.severity == "Yüksek") MissedRed.copy(alpha = 0.2f) else AccentTeal.copy(alpha = 0.2f)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${def.severity} Öncelik",
                                                color = if (def.severity == "Yüksek") MissedRed else AccentTeal,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = def.actionPlan,
                                        color = SubtitleWhite,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = { onSolveDeficiencyQuestions(def) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AccentTeal.copy(alpha = 0.15f)
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Quiz,
                                            contentDescription = null,
                                            tint = AccentTeal,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Bu Konudan Soru Çöz",
                                            color = AccentTeal,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Weekly Prescription
                        Text(
                            text = "📋 2 Haftalık Net Artırma Reçetesi",
                            color = PureWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        analysis.weeklyPrescription.forEach { item ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x221E1B4B))
                                    .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = DoneGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.phase,
                                            color = PureWhite,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = item.targetTask,
                                            color = SubtitleWhite,
                                            fontSize = 11.5.sp
                                        )
                                    }
                                    Text(
                                        text = "${item.recommendedQuestionCount} Soru",
                                        color = AccentTeal,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Maarif Model Strategic Advice
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x220EA5E9))
                                .border(1.dp, AccentTeal.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = "💡 2028 Maarif Modeli Sınav Tavsiyesi",
                                    color = AccentTeal,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = analysis.maarifAdvice,
                                    color = PureWhite,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 2: MATH & GRAPH LAB VIEW
// =========================================================================

@Composable
private fun MathGraphLabView(
    mathProblem: String,
    mathSolution: MathSolution?,
    isSolving: Boolean,
    customA: Float,
    customB: Float,
    customC: Float,
    customType: String,
    onProblemChange: (String) -> Unit,
    onSolveProblem: (String) -> Unit,
    onUpdateGraph: (Float, Float, Float, String) -> Unit
) {
    var problemInput by remember { mutableStateOf(mathProblem) }

    // Live-computed key points for the active function
    val activeKeyPoints = remember(customA, customB, customC, customType) {
        val pts = mutableListOf<GraphPoint>()
        if (customType == "PARABOLA" && customA != 0f) {
            val r = -customB / (2f * customA)
            val k = (customA * r * r) + (customB * r) + customC
            pts.add(GraphPoint(r, k, "Tepe T(%.1f, %.1f)".format(r, k)))

            val delta = (customB * customB) - (4f * customA * customC)
            if (delta > 0f) {
                val sqrtDelta = kotlin.math.sqrt(delta.toDouble()).toFloat()
                val x1 = (-customB - sqrtDelta) / (2f * customA)
                val x2 = (-customB + sqrtDelta) / (2f * customA)
                pts.add(GraphPoint(x1, 0f, "x₁(%.1f)".format(x1)))
                pts.add(GraphPoint(x2, 0f, "x₂(%.1f)".format(x2)))
            } else if (delta == 0f) {
                pts.add(GraphPoint(r, 0f, "Çift Kök (%.1f)".format(r)))
            }
            pts.add(GraphPoint(0f, customC, "y-kesim (0, %.1f)".format(customC)))
        } else if (customType == "LINEAR") {
            if (customA != 0f) {
                val rootX = -customB / customA
                pts.add(GraphPoint(rootX, 0f, "Kök (%.1f, 0)".format(rootX)))
            }
            pts.add(GraphPoint(0f, customB, "y-kesim (0, %.1f)".format(customB)))
        }
        pts
    }

    val currentGraphInfo = remember(customA, customB, customC, customType, activeKeyPoints) {
        val title = if (customType == "LINEAR") {
            "f(x) = %.1fx %s %.1f".format(customA, if (customB >= 0) "+" else "-", kotlin.math.abs(customB))
        } else {
            "f(x) = %.1fx² %s %.1fx %s %.1f".format(
                customA,
                if (customB >= 0) "+" else "-", kotlin.math.abs(customB),
                if (customC >= 0) "+" else "-", kotlin.math.abs(customC)
            )
        }
        QuestionGraphInfo(
            title = title,
            xLabel = "x",
            yLabel = "f(x)",
            type = customType,
            a = customA,
            b = customB,
            c = customC,
            keyPoints = activeKeyPoints,
            minX = -6f,
            maxX = 6f,
            minY = -8f,
            maxY = 10f
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Interactive Canvas Graph Card ---
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fonksiyon & Grafik Laboratuvarı",
                        color = PureWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentTeal.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Canlı Çizim",
                            color = AccentTeal,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Presets
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        PresetChip("Parabol: x² - 4x + 3") {
                            onUpdateGraph(1f, -4f, 3f, "PARABOLA")
                        }
                    }
                    item {
                        PresetChip("Ters Parabol: -x² + 5") {
                            onUpdateGraph(-1f, 0f, 5f, "PARABOLA")
                        }
                    }
                    item {
                        PresetChip("Doğrusal: 2x - 4") {
                            onUpdateGraph(2f, -4f, 0f, "LINEAR")
                        }
                    }
                    item {
                        PresetChip("Geniş Parabol: 0.5x² - 2") {
                            onUpdateGraph(0.5f, 0f, -2f, "PARABOLA")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Render Interactive Canvas
                InteractiveGraphCanvas(
                    graphInfo = currentGraphInfo,
                    heightDp = 220
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Analytic Indicators (Tepe Noktası, Kökler, Diskriminant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (customType == "PARABOLA" && customA != 0f) {
                        val r = -customB / (2f * customA)
                        val k = (customA * r * r) + (customB * r) + customC
                        val delta = (customB * customB) - (4f * customA * customC)

                        InfoPill(
                            title = "Tepe T(r, k)",
                            value = "(%.1f, %.1f)".format(r, k),
                            color = AccentTeal,
                            modifier = Modifier.weight(1f)
                        )
                        InfoPill(
                            title = "Diskriminant Δ",
                            value = "%.1f %s".format(delta, if (delta > 0) "(2 Kök)" else if (delta == 0f) "(Çift)" else "(Reel Yok)"),
                            color = if (delta >= 0) DoneGreen else MissedRed,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        val slope = customA
                        InfoPill(
                            title = "Eğim (m)",
                            value = "m = %.1f".format(slope),
                            color = AccentTeal,
                            modifier = Modifier.weight(1f)
                        )
                        InfoPill(
                            title = "y-kesim",
                            value = "(0, %.1f)".format(customB),
                            color = DoneGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // --- 2. Live Coefficient Sliders ---
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x331E1B4B))
                    .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "Katsayıları Canlı Ayarla",
                        color = PureWhite,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Değerleri kaydırarak parabolün ve doğrunun şekil değişimini incele",
                        color = SubtitleWhite,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Slider for 'a'
                    CoefficientSlider(
                        label = "a (Kolları ve Eğimi Belirler): %.1f".format(customA),
                        value = customA,
                        range = -4f..4f,
                        onValueChange = { newA ->
                            val safeA = if (newA == 0f) 0.1f else newA
                            onUpdateGraph(safeA, customB, customC, customType)
                        }
                    )

                    // Slider for 'b'
                    CoefficientSlider(
                        label = "b (Tepe Noktası Kayması): %.1f".format(customB),
                        value = customB,
                        range = -8f..8f,
                        onValueChange = { newB ->
                            onUpdateGraph(customA, newB, customC, customType)
                        }
                    )

                    // Slider for 'c' (y-intercept)
                    if (customType == "PARABOLA") {
                        CoefficientSlider(
                            label = "c (y-kesim noktası): %.1f".format(customC),
                            value = customC,
                            range = -10f..10f,
                            onValueChange = { newC ->
                                onUpdateGraph(customA, customB, newC, customType)
                            }
                        )
                    }
                }
            }
        }

        // --- 3. AI Math & Problem Solver Card ---
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1E1B4B), Color(0xFF110E2D))
                        )
                    )
                    .border(1.dp, AccentTeal.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(AccentTeal.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = AccentTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Yapay Zeka Matematiksel Çözücü",
                                color = PureWhite,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Denklem veya işlem gir; adım adım çözüp grafiğe döksün",
                                color = SubtitleWhite,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Example chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            QuickExampleChip("x² - 4x + 3 = 0") {
                                problemInput = "x² - 4x + 3 = 0 denkleminin kökleri ve tepe noktası nedir?"
                                onProblemChange(problemInput)
                            }
                        }
                        item {
                            QuickExampleChip("f(x) = -x² + 6x") {
                                problemInput = "f(x) = -x² + 6x fonksiyonunun maksimum değeri ve kökleri"
                                onProblemChange(problemInput)
                            }
                        }
                        item {
                            QuickExampleChip("2x - 8 = 0") {
                                problemInput = "2x - 8 = 0 doğrusunun grafiği ve eksen kesimleri"
                                onProblemChange(problemInput)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = problemInput,
                        onValueChange = {
                            problemInput = it
                            onProblemChange(it)
                        },
                        placeholder = { Text("Örn: 2x² - 8x + 6 = 0 veya tepe noktasını bul...", color = MutedWhite, fontSize = 12.5.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentTeal,
                            unfocusedBorderColor = GlassCardBorderSubtle,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedContainerColor = Color(0x33121028),
                            unfocusedContainerColor = Color(0x33121028)
                        ),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onSolveProblem(problemInput) },
                        enabled = !isSolving && problemInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSolving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF0F172A),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Matematiksel İşlem Çözülüyor...", color = Color(0xFF0F172A))
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Çöz ve Grafiğini Çiz",
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Solution Display
                    if (mathSolution != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x33000000))
                                .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = "🎯 Nihai Çözüm",
                                    color = AccentTeal,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = mathSolution.finalAnswer,
                                    color = DoneGreen,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Adım Adım Çözüm:",
                                    color = PureWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                mathSolution.steps.forEach { step ->
                                    Text(
                                        text = "• $step",
                                        color = SubtitleWhite,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }

                                if (mathSolution.maarifKeyTakeaway.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(AccentPurple.copy(alpha = 0.2f))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = "💡 Maarif Modeli Notu: ${mathSolution.maarifKeyTakeaway}",
                                            color = PureWhite,
                                            fontSize = 11.5.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }

                                // Show graph if returned
                                mathSolution.graphInfo?.let { gInfo ->
                                    Spacer(modifier = Modifier.height(12.dp))
                                    InteractiveGraphCanvas(
                                        graphInfo = gInfo,
                                        heightDp = 200
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

@Composable
private fun PresetChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x331E1B4B))
            .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = label, color = PureWhite, fontSize = 11.5.sp)
    }
}

@Composable
private fun QuickExampleChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AccentTeal.copy(alpha = 0.12f))
            .border(1.dp, AccentTeal.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = AccentTeal, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InfoPill(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x221E1B4B))
            .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(text = title, color = SubtitleWhite, fontSize = 10.5.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CoefficientSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = PureWhite, fontSize = 11.5.sp)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = AccentTeal,
                activeTrackColor = AccentTeal,
                inactiveTrackColor = Color(0x33FFFFFF)
            )
        )
    }
}

// =========================================================================
// TAB 3: MAARIF QUIZ VIEW
// =========================================================================

@Composable
private fun MaarifQuizView(
    questions: List<QuizQuestion>,
    isLoading: Boolean,
    subject: String,
    topic: String,
    onGenerateQuestions: (String, String, Int) -> Unit,
    onSelectAnswer: (String, Int) -> Unit
) {
    var selectedSubject by remember { mutableStateOf(subject) }
    var selectedTopic by remember { mutableStateOf(topic) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Topic Config Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x331E1B4B))
                    .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Maarif Modeli Beceri Testi Üretici",
                        color = PureWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "2028 müfredatına özel analiz, modelleme ve çıkarım soruları",
                        color = SubtitleWhite,
                        fontSize = 11.5.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Subject Selector Chips
                    val subjects = listOf("Temel Matematik", "Türkçe", "Fen Bilimleri", "Sosyal Bilimler")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(subjects) { s ->
                            val isSel = selectedSubject == s
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSel) AccentTeal else Color(0x22FFFFFF)
                                    )
                                    .clickable { selectedSubject = s }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = s,
                                    color = if (isSel) Color(0xFF0F172A) else PureWhite,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = selectedTopic,
                        onValueChange = { selectedTopic = it },
                        label = { Text("Konu veya Eksik Alanı", color = SubtitleWhite, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentTeal,
                            unfocusedBorderColor = GlassCardBorderSubtle,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onGenerateQuestions(selectedSubject, selectedTopic, 3) },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF0F172A),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sorular Hazırlanıyor...", color = Color(0xFF0F172A))
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Quiz,
                                contentDescription = null,
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "3 Yeni Nesil Soru Üret",
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Questions List
        if (questions.isNotEmpty()) {
            itemsIndexed(questions) { index, question ->
                QuizQuestionCard(
                    questionIndex = index + 1,
                    question = question,
                    onSelectOption = { optIdx -> onSelectAnswer(question.id, optIdx) }
                )
            }
        }
    }
}

@Composable
private fun QuizQuestionCard(
    questionIndex: Int,
    question: QuizQuestion,
    onSelectOption: (Int) -> Unit
) {
    var showExplanation by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x331E1B4B))
            .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Soru $questionIndex • ${question.subject}",
                    color = AccentTeal,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentPurple.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = question.skillType,
                        color = AccentPurple,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = question.questionText,
                color = PureWhite,
                fontSize = 13.5.sp,
                lineHeight = 20.sp
            )

            // Optional Canvas Graph for Math / Physics / Data Analysis questions
            question.graphInfo?.let { graph ->
                Spacer(modifier = Modifier.height(10.dp))
                InteractiveGraphCanvas(graphInfo = graph, heightDp = 180)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Options A, B, C, D, E
            question.options.forEachIndexed { optIndex, optionText ->
                val isSelected = question.selectedOptionIndex == optIndex
                val isAnswered = question.selectedOptionIndex != null
                val isCorrect = optIndex == question.correctOptionIndex

                val borderColor by animateColorAsState(
                    targetValue = when {
                        !isAnswered -> if (isSelected) AccentTeal else GlassCardBorderSubtle
                        isCorrect -> DoneGreen
                        isSelected -> MissedRed
                        else -> GlassCardBorderSubtle
                    },
                    label = "border"
                )

                val bgColor by animateColorAsState(
                    targetValue = when {
                        !isAnswered -> if (isSelected) AccentTeal.copy(alpha = 0.15f) else Color(0x22121028)
                        isCorrect -> DoneGreen.copy(alpha = 0.18f)
                        isSelected -> MissedRed.copy(alpha = 0.18f)
                        else -> Color(0x22121028)
                    },
                    label = "bg"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                        .clickable(enabled = !isAnswered) { onSelectOption(optIndex) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = optionText,
                            color = PureWhite,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )

                        if (isAnswered) {
                            if (isCorrect) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Doğru",
                                    tint = DoneGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Yanlış",
                                    tint = MissedRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Explanation Section
            if (question.selectedOptionIndex != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showExplanation = !showExplanation }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.HelpOutline,
                        contentDescription = null,
                        tint = AccentTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showExplanation) "Çözümü Gizle" else "Detaylı Maarif Çözümünü Gör",
                        color = AccentTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                AnimatedVisibility(visible = showExplanation) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x221E1B4B))
                            .border(1.dp, AccentTeal.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = question.explanation,
                            color = PureWhite,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// BOTTOM SHEET: ADD NEW TRIAL EXAM
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExamBottomSheet(
    onDismiss: () -> Unit,
    onSave: (String, YksExamType, List<ExamSubjectResult>, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var examTitle by remember { mutableStateOf("Yeni Deneme Sınavı") }
    var examType by remember { mutableStateOf(YksExamType.TYT) }

    // Subjects input
    var turkceC by remember { mutableStateOf("30") }
    var turkceI by remember { mutableStateOf("6") }
    var turkceE by remember { mutableStateOf("4") }
    var turkceWeaks by remember { mutableStateOf("Paragraf, Anlatım Bozukluğu") }

    var matC by remember { mutableStateOf("25") }
    var matI by remember { mutableStateOf("5") }
    var matE by remember { mutableStateOf("10") }
    var matWeaks by remember { mutableStateOf("Problemler, Fonksiyonlar") }

    var fenC by remember { mutableStateOf("12") }
    var fenI by remember { mutableStateOf("4") }
    var fenE by remember { mutableStateOf("4") }
    var fenWeaks by remember { mutableStateOf("Optik, Kimya Mol") }

    var sosC by remember { mutableStateOf("15") }
    var sosI by remember { mutableStateOf("3") }
    var sosE by remember { mutableStateOf("2") }
    var sosWeaks by remember { mutableStateOf("Coğrafya İklim") }

    var notes by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF121028)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Yeni Deneme Sonucu Ekle",
                color = PureWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Netlerini ve zorlandığın konuları gir, yapay zeka Maarif modeline göre analiz etsin.",
                color = SubtitleWhite,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = examTitle,
                onValueChange = { examTitle = it },
                label = { Text("Deneme Adı / Yayın", color = SubtitleWhite) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentTeal,
                    unfocusedBorderColor = GlassCardBorderSubtle,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Net Inputs
            SubjectRowInput("Türkçe", turkceC, turkceI, turkceE, turkceWeaks,
                onCChange = { turkceC = it }, onIChange = { turkceI = it },
                onEChange = { turkceE = it }, onWeaksChange = { turkceWeaks = it })

            SubjectRowInput("Matematik", matC, matI, matE, matWeaks,
                onCChange = { matC = it }, onIChange = { matI = it },
                onEChange = { matE = it }, onWeaksChange = { matWeaks = it })

            SubjectRowInput("Fen Bilimleri", fenC, fenI, fenE, fenWeaks,
                onCChange = { fenC = it }, onIChange = { fenI = it },
                onEChange = { fenE = it }, onWeaksChange = { fenWeaks = it })

            SubjectRowInput("Sosyal Bilimler", sosC, sosI, sosE, sosWeaks,
                onCChange = { sosC = it }, onIChange = { sosI = it },
                onEChange = { sosE = it }, onWeaksChange = { sosWeaks = it })

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val results = listOf(
                        ExamSubjectResult(
                            "Türkçe",
                            turkceC.toIntOrNull() ?: 0,
                            turkceI.toIntOrNull() ?: 0,
                            turkceE.toIntOrNull() ?: 0,
                            weakTopics = turkceWeaks.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        ),
                        ExamSubjectResult(
                            "Temel Matematik",
                            matC.toIntOrNull() ?: 0,
                            matI.toIntOrNull() ?: 0,
                            matE.toIntOrNull() ?: 0,
                            weakTopics = matWeaks.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        ),
                        ExamSubjectResult(
                            "Fen Bilimleri",
                            fenC.toIntOrNull() ?: 0,
                            fenI.toIntOrNull() ?: 0,
                            fenE.toIntOrNull() ?: 0,
                            weakTopics = fenWeaks.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        ),
                        ExamSubjectResult(
                            "Sosyal Bilimler",
                            sosC.toIntOrNull() ?: 0,
                            sosI.toIntOrNull() ?: 0,
                            sosE.toIntOrNull() ?: 0,
                            weakTopics = sosWeaks.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        )
                    )

                    onSave(examTitle, examType, results, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Kaydet ve Yapay Zekayla Analiz Et",
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SubjectRowInput(
    subject: String,
    c: String,
    i: String,
    e: String,
    weaks: String,
    onCChange: (String) -> Unit,
    onIChange: (String) -> Unit,
    onEChange: (String) -> Unit,
    onWeaksChange: (String) -> Unit
) {
    val net = ((c.toIntOrNull() ?: 0) - ((i.toIntOrNull() ?: 0) * 0.25f)).coerceAtLeast(0f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x221E1B4B))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = subject, color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = "%.2f Net".format(net), color = AccentTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = c,
                onValueChange = onCChange,
                label = { Text("D", fontSize = 10.sp) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PureWhite, unfocusedTextColor = PureWhite)
            )
            OutlinedTextField(
                value = i,
                onValueChange = onIChange,
                label = { Text("Y", fontSize = 10.sp) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PureWhite, unfocusedTextColor = PureWhite)
            )
            OutlinedTextField(
                value = e,
                onValueChange = onEChange,
                label = { Text("B", fontSize = 10.sp) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PureWhite, unfocusedTextColor = PureWhite)
            )
            OutlinedTextField(
                value = weaks,
                onValueChange = onWeaksChange,
                label = { Text("Zorlandığın Konu", fontSize = 10.sp) },
                modifier = Modifier.weight(2.5f),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PureWhite, unfocusedTextColor = PureWhite)
            )
        }
    }
}
