package com.nororoutine.batu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nororoutine.batu.data.YksRepository
import com.nororoutine.batu.model.ChatMessage
import com.nororoutine.batu.model.ExamSubjectResult
import com.nororoutine.batu.model.QuizQuestion
import com.nororoutine.batu.model.StudySession
import com.nororoutine.batu.model.YksExamRecord
import com.nororoutine.batu.model.YksExamType
import com.nororoutine.batu.network.YksGeminiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class YksCoachUiState(
    val currentSubTab: Int = 0, // 0: Sohbet, 1: Deneme, 2: Çalışma
    val isAiThinking: Boolean = false,
    val isAnalyzingExam: Boolean = false,
    val selectedExam: YksExamRecord? = null,
    val showAddExamSheet: Boolean = false,
    // Çalışma (Resource-based Quiz) State
    val isGeneratingStudyQuiz: Boolean = false,
    val isAnalyzingStudy: Boolean = false,
    val activeStudySession: StudySession? = null,
    val currentQuestionIndex: Int = 0,
    val selectedAnswers: Map<String, Int> = emptyMap(), // questionId -> selectedOptionIndex (0..4)
    val revealedHintIds: Set<String> = emptySet(),
    val uploadedImageUri: String? = null,
    val uploadedImageBase64: String? = null,
    val uploadedDocText: String = "",
    val promptDescription: String = "",
    val requestedQuestionCount: Int = 5
)

class YksCoachViewModel(
    private val repository: YksRepository = YksRepository()
) : ViewModel() {

    val examRecords: StateFlow<List<YksExamRecord>> = repository.examRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studySessions: StateFlow<List<StudySession>> = repository.studySessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatHistory: StateFlow<List<ChatMessage>> = repository.chatHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(YksCoachUiState())
    val uiState: StateFlow<YksCoachUiState> = _uiState.asStateFlow()

    init {
        // Automatically select the latest exam if available
        viewModelScope.launch {
            repository.examRecords.collect { list ->
                if (_uiState.value.selectedExam == null && list.isNotEmpty()) {
                    _uiState.update { it.copy(selectedExam = list.first()) }
                }
            }
        }
    }

    fun setSubTab(tabIndex: Int) {
        _uiState.update { it.copy(currentSubTab = tabIndex.coerceIn(0, 2)) }
    }

    // --- SOHBET (Chat) ---
    fun sendMessage(
        userText: String,
        routinesSummary: String = "",
        weeklySummary: String = ""
    ) {
        if (userText.isBlank()) return
        val userMsg = ChatMessage(sender = "user", text = userText.trim())

        viewModelScope.launch {
            repository.addChatMessage(userMsg)
            _uiState.update { it.copy(isAiThinking = true) }

            val exams = examRecords.value
            val latestExam = _uiState.value.selectedExam ?: exams.firstOrNull()
            val examsSummary = if (exams.isNotEmpty()) {
                exams.take(3).joinToString("\n") { ex ->
                    "- ${ex.title} (${ex.date}): Toplam Net: %.2f (D: %d, Y: %d, B: %d)".format(
                        ex.totalNet, ex.totalCorrect, ex.totalIncorrect, ex.totalEmpty
                    ) + " | Dersler: " + ex.results.joinToString(", ") { "${it.subjectName}: %.2f".format(it.net) }
                }
            } else ""

            val aiResponse = YksGeminiService.chatWithCoach(
                history = chatHistory.value,
                userMessage = userText,
                latestExam = latestExam,
                routinesContext = routinesSummary,
                weeklyAnalysisContext = weeklySummary,
                examsSummaryContext = examsSummary
            )

            repository.addChatMessage(aiResponse)
            _uiState.update { it.copy(isAiThinking = false) }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChat()
        }
    }

    // --- DENEME (Mock Exam Management) ---
    fun selectExam(exam: YksExamRecord) {
        _uiState.update { it.copy(selectedExam = exam) }
    }

    fun setShowAddExamSheet(show: Boolean) {
        _uiState.update { it.copy(showAddExamSheet = show) }
    }

    fun analyzeExam(exam: YksExamRecord) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzingExam = true) }
            val analysis = YksGeminiService.analyzeExamWithAi(exam)
            val updatedExam = exam.copy(aiAnalysis = analysis)
            repository.saveExam(updatedExam)
            _uiState.update { it.copy(isAnalyzingExam = false, selectedExam = updatedExam) }
        }
    }

    fun saveNewExam(
        title: String,
        date: String,
        examType: YksExamType,
        results: List<ExamSubjectResult>,
        notes: String
    ) {
        viewModelScope.launch {
            val totalNet = results.sumOf { it.net.toDouble() }.toFloat()
            val newRecord = YksExamRecord(
                title = title.ifBlank { "${examType.displayName} Denemesi" },
                date = date.ifBlank { LocalDate.now().toString() },
                examType = examType,
                results = results,
                totalNet = totalNet,
                notes = notes
            )
            repository.saveExam(newRecord)
            _uiState.update { it.copy(selectedExam = newRecord, showAddExamSheet = false) }

            // Automatically analyze with AI
            analyzeExam(newRecord)
        }
    }

    fun deleteExam(examId: String) {
        viewModelScope.launch {
            repository.deleteExam(examId)
            val remaining = examRecords.value.filterNot { it.id == examId }
            _uiState.update { it.copy(selectedExam = remaining.firstOrNull()) }
        }
    }

    // --- ÇALIŞMA (Study - Resource Quiz) ---
    fun setPromptDescription(desc: String) {
        _uiState.update { it.copy(promptDescription = desc) }
    }

    fun setUploadedDocText(text: String) {
        _uiState.update { it.copy(uploadedDocText = text) }
    }

    fun setQuestionCount(count: Int) {
        _uiState.update { it.copy(requestedQuestionCount = count.coerceIn(1, 10)) }
    }

    fun setUploadedImage(uri: String?, base64: String?) {
        _uiState.update {
            it.copy(
                uploadedImageUri = uri,
                uploadedImageBase64 = base64
            )
        }
    }

    fun startGeneratingStudyQuiz(
        description: String,
        count: Int,
        imageBase64: String?,
        imageUri: String?,
        docText: String?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingStudyQuiz = true) }

            val cleanDesc = description.ifBlank { "Yüklenen kaynaktan 2028 Maarif modeli 5 şıklı YKS soruları hazırla" }
            val questions = YksGeminiService.generateStudyQuizFromResource(
                description = cleanDesc,
                questionCount = count,
                imageBase64 = imageBase64,
                docText = docText
            )

            val now = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale("tr"))
            val formattedDate = now.format(formatter)

            val newSession = StudySession(
                title = cleanDesc.take(45),
                formattedDate = formattedDate,
                description = cleanDesc,
                imageUri = imageUri,
                questions = questions,
                userAnswers = emptyMap(),
                isCompleted = false
            )

            _uiState.update {
                it.copy(
                    isGeneratingStudyQuiz = false,
                    activeStudySession = newSession,
                    currentQuestionIndex = 0,
                    selectedAnswers = emptyMap(),
                    revealedHintIds = emptySet()
                )
            }
        }
    }

    fun selectStudyAnswer(questionId: String, optionIndex: Int) {
        _uiState.update { state ->
            val updated = state.selectedAnswers.toMutableMap()
            updated[questionId] = optionIndex
            state.copy(selectedAnswers = updated)
        }
    }

    fun revealStudyHint(questionId: String) {
        _uiState.update { state ->
            state.copy(revealedHintIds = state.revealedHintIds + questionId)
        }
    }

    fun goToNextQuestion() {
        _uiState.update { state ->
            val session = state.activeStudySession ?: return@update state
            val nextIdx = (state.currentQuestionIndex + 1).coerceAtMost(session.questions.size - 1)
            state.copy(currentQuestionIndex = nextIdx)
        }
    }

    fun goToPreviousQuestion() {
        _uiState.update { state ->
            val prevIdx = (state.currentQuestionIndex - 1).coerceAtLeast(0)
            state.copy(currentQuestionIndex = prevIdx)
        }
    }

    fun setQuestionIndex(index: Int) {
        _uiState.update { state ->
            val session = state.activeStudySession ?: return@update state
            state.copy(currentQuestionIndex = index.coerceIn(0, session.questions.size - 1))
        }
    }

    fun finishStudyQuiz() {
        val session = _uiState.value.activeStudySession ?: return
        val answers = _uiState.value.selectedAnswers

        var correct = 0
        var incorrect = 0
        var empty = 0

        session.questions.forEach { q ->
            val chosen = answers[q.id]
            when {
                chosen == null -> empty++
                chosen == q.correctOptionIndex -> correct++
                else -> incorrect++
            }
        }

        val net = (correct.toFloat() - (incorrect.toFloat() * 0.25f)).coerceAtLeast(0f)

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzingStudy = true) }

            val (analysisText, weakTopics) = YksGeminiService.generateStudyFeedbackAnalysis(
                studyTitle = session.title,
                questions = session.questions,
                userAnswers = answers,
                correctCount = correct,
                incorrectCount = incorrect,
                emptyCount = empty
            )

            val completedSession = session.copy(
                userAnswers = answers,
                isCompleted = true,
                correctCount = correct,
                incorrectCount = incorrect,
                emptyCount = empty,
                netScore = net,
                analysisFeedback = analysisText,
                weakTopicsToFocus = weakTopics
            )

            repository.saveStudySession(completedSession)

            _uiState.update {
                it.copy(
                    isAnalyzingStudy = false,
                    activeStudySession = completedSession
                )
            }
        }
    }

    fun viewPastStudySession(session: StudySession) {
        _uiState.update {
            it.copy(
                activeStudySession = session,
                currentQuestionIndex = 0,
                selectedAnswers = session.userAnswers,
                revealedHintIds = session.questions.map { q -> q.id }.toSet()
            )
        }
    }

    fun deletePastStudy(sessionId: String) {
        viewModelScope.launch {
            repository.deleteStudySession(sessionId)
            if (_uiState.value.activeStudySession?.id == sessionId) {
                _uiState.update { it.copy(activeStudySession = null) }
            }
        }
    }

    fun closeActiveStudy() {
        _uiState.update {
            it.copy(
                activeStudySession = null,
                currentQuestionIndex = 0,
                selectedAnswers = emptyMap(),
                revealedHintIds = emptySet()
            )
        }
    }
}
