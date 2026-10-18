package com.nororoutine.batu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nororoutine.batu.data.YksRepository
import com.nororoutine.batu.model.ChatMessage
import com.nororoutine.batu.model.DeficiencyItem
import com.nororoutine.batu.model.ExamSubjectResult
import com.nororoutine.batu.model.QuizQuestion
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

import com.nororoutine.batu.model.MathSolution
import com.nororoutine.batu.model.QuestionGraphInfo
import com.nororoutine.batu.model.GraphPoint

data class YksCoachUiState(
    val currentSubTab: Int = 0, // 0: Sohbet, 1: Denemeler, 2: Bilim & Grafik Lab, 3: Soru Çöz
    val isAiThinking: Boolean = false,
    val isAnalyzingExam: Boolean = false,
    val selectedExam: YksExamRecord? = null,
    val quizQuestions: List<QuizQuestion> = emptyList(),
    val isLoadingQuiz: Boolean = false,
    val quizSubject: String = "Temel Matematik",
    val quizTopic: String = "Parabol ve Fonksiyon Modelleme",
    val showAddExamSheet: Boolean = false,
    // Science, Math & Graph Lab state
    val labSubject: String = "Matematik", // Matematik, Fizik, Kimya
    val mathProblemInput: String = "f(x) = x² - 4x + 3",
    val mathSolution: MathSolution? = null,
    val isSolvingMath: Boolean = false,
    val customGraphA: Float = 1f,
    val customGraphB: Float = -4f,
    val customGraphC: Float = 3f,
    val customGraphType: String = "PARABOLA",
    val customUnitX: String = "",
    val customUnitY: String = "",
    val customAreaLabel: String = ""
)

class YksCoachViewModel(
    private val repository: YksRepository = YksRepository()
) : ViewModel() {

    val examRecords: StateFlow<List<YksExamRecord>> = repository.examRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatHistory: StateFlow<List<ChatMessage>> = repository.chatHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(YksCoachUiState())
    val uiState: StateFlow<YksCoachUiState> = _uiState.asStateFlow()

    init {
        // Default select latest exam if available
        viewModelScope.launch {
            repository.examRecords.collect { list ->
                if (_uiState.value.selectedExam == null && list.isNotEmpty()) {
                    _uiState.update { it.copy(selectedExam = list.first()) }
                }
            }
        }
    }

    fun setSubTab(tabIndex: Int) {
        _uiState.update { it.copy(currentSubTab = tabIndex) }
    }

    fun selectExam(exam: YksExamRecord) {
        _uiState.update { it.copy(selectedExam = exam) }
    }

    fun setShowAddExamSheet(show: Boolean) {
        _uiState.update { it.copy(showAddExamSheet = show) }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        val userMsg = ChatMessage(sender = "user", text = userText.trim())

        viewModelScope.launch {
            repository.addChatMessage(userMsg)
            _uiState.update { it.copy(isAiThinking = true) }

            val latestExam = _uiState.value.selectedExam ?: examRecords.value.firstOrNull()
            val aiResponse = YksGeminiService.chatWithCoach(
                history = chatHistory.value,
                userMessage = userText,
                latestExam = latestExam
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
        examType: YksExamType,
        results: List<ExamSubjectResult>,
        notes: String
    ) {
        viewModelScope.launch {
            val totalNet = results.sumOf { it.net.toDouble() }.toFloat()
            val newRecord = YksExamRecord(
                title = title.ifBlank { "${examType.displayName} Denemesi" },
                date = LocalDate.now().toString(),
                examType = examType,
                results = results,
                totalNet = totalNet,
                notes = notes
            )
            repository.saveExam(newRecord)
            _uiState.update { it.copy(selectedExam = newRecord, showAddExamSheet = false) }

            // Trigger AI analysis automatically
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

    fun generateQuiz(subject: String, topic: String, count: Int = 3) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingQuiz = true,
                    quizSubject = subject,
                    quizTopic = topic
                )
            }
            val questions = YksGeminiService.generateMaarifQuestions(subject, topic, count)
            _uiState.update {
                it.copy(
                    isLoadingQuiz = false,
                    quizQuestions = questions
                )
            }
        }
    }

    fun answerQuizQuestion(questionId: String, selectedIndex: Int) {
        val currentQuestions = _uiState.value.quizQuestions.map { q ->
            if (q.id == questionId) {
                q.copy(selectedOptionIndex = selectedIndex)
            } else {
                q
            }
        }
        _uiState.update { it.copy(quizQuestions = currentQuestions) }
    }

    fun revealQuestionHint(questionId: String) {
        val currentQuestions = _uiState.value.quizQuestions.map { q ->
            if (q.id == questionId) {
                val maxHints = if (q.hints.isNotEmpty()) q.hints.size else if (q.hint != null) 1 else 2
                val nextLevel = (q.revealedHintLevel + 1).coerceAtMost(maxHints)
                q.copy(revealedHintLevel = nextLevel)
            } else {
                q
            }
        }
        _uiState.update { it.copy(quizQuestions = currentQuestions) }
    }

    fun setLabSubject(subject: String) {
        when (subject) {
            "Fizik" -> {
                _uiState.update {
                    it.copy(
                        labSubject = "Fizik",
                        mathProblemInput = "v0 = 4 m/s hızla başlayan araç a = 2 m/s² ivmeyle hızlanıyor. 5 saniyedeki yer değiştirmesi?",
                        customGraphType = "PHYSICS_VT",
                        customGraphA = 2f,
                        customGraphB = 4f,
                        customGraphC = 0f,
                        customUnitX = "s",
                        customUnitY = "m/s",
                        customAreaLabel = "Alan = Yer Değiştirme (Δx)"
                    )
                }
            }
            "Kimya" -> {
                _uiState.update {
                    it.copy(
                        labSubject = "Kimya",
                        mathProblemInput = "2H₂(g) + O₂(g) → 2H₂O(s) tepkimesinde Ea = 140 kJ, ΔH = -572 kJ. Potansiyel enerji grafiği ve entalpi?",
                        customGraphType = "CHEM_ENERGY_PROFILE",
                        customGraphA = 60f,
                        customGraphB = 0f,
                        customGraphC = 25f,
                        customUnitX = "Tepkime Koordinatı",
                        customUnitY = "PE (kJ)",
                        customAreaLabel = "ΔH = -15 kJ (Ekzotermik)"
                    )
                }
            }
            else -> { // Matematik
                _uiState.update {
                    it.copy(
                        labSubject = "Matematik",
                        mathProblemInput = "f(x) = x² - 4x + 3",
                        customGraphType = "PARABOLA",
                        customGraphA = 1f,
                        customGraphB = -4f,
                        customGraphC = 3f,
                        customUnitX = "",
                        customUnitY = "",
                        customAreaLabel = ""
                    )
                }
            }
        }
    }

    fun setMathProblemInput(text: String) {
        _uiState.update { it.copy(mathProblemInput = text) }
    }

    fun solveMathProblem(problemText: String) {
        if (problemText.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSolvingMath = true) }
            val solution = YksGeminiService.solveMathProblem(problemText.trim())
            _uiState.update {
                it.copy(
                    isSolvingMath = false,
                    mathSolution = solution,
                    labSubject = solution.subject.ifBlank { it.labSubject },
                    customGraphA = solution.graphInfo?.a ?: it.customGraphA,
                    customGraphB = solution.graphInfo?.b ?: it.customGraphB,
                    customGraphC = solution.graphInfo?.c ?: it.customGraphC,
                    customGraphType = solution.graphInfo?.type ?: it.customGraphType,
                    customUnitX = solution.graphInfo?.unitX ?: it.customUnitX,
                    customUnitY = solution.graphInfo?.unitY ?: it.customUnitY,
                    customAreaLabel = solution.graphInfo?.highlightAreaLabel ?: it.customAreaLabel
                )
            }
        }
    }

    fun updateCustomGraph(
        a: Float,
        b: Float,
        c: Float,
        type: String,
        unitX: String = "",
        unitY: String = "",
        areaLabel: String = ""
    ) {
        _uiState.update {
            it.copy(
                customGraphA = a,
                customGraphB = b,
                customGraphC = c,
                customGraphType = type,
                customUnitX = unitX,
                customUnitY = unitY,
                customAreaLabel = areaLabel
            )
        }
    }

    fun startQuizForDeficiency(deficiency: DeficiencyItem) {
        _uiState.update {
            it.copy(
                currentSubTab = 3, // Go to Quiz Tab (tab index 3)
                quizSubject = deficiency.subject,
                quizTopic = deficiency.topic
            )
        }
        generateQuiz(deficiency.subject, deficiency.topic, count = 3)
    }
}
