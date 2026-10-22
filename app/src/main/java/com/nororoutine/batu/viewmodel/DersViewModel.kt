package com.nororoutine.batu.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nororoutine.batu.model.DersFlowStep
import com.nororoutine.batu.model.DersQuestion
import com.nororoutine.batu.model.DersQuestionType
import com.nororoutine.batu.model.DersStudySessionRecord
import com.nororoutine.batu.model.DocumentAnalysisSummary
import com.nororoutine.batu.model.MatchingPair
import com.nororoutine.batu.network.DersQuestionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DersUiState(
    val flowStep: DersFlowStep = DersFlowStep.UPLOAD,
    val isAnalyzing: Boolean = false,
    val isGeneratingQuestions: Boolean = false,
    val isGeneratingSummary: Boolean = false,

    // Upload inputs
    val uploadedImageUri: String? = null,
    val uploadedImageBase64: String? = null,
    val uploadedFileName: String? = null,
    val uploadedDocumentText: String = "",
    val manualNotesText: String = "",

    // Analysis & Confirmation
    val analysisSummary: DocumentAnalysisSummary? = null,
    val selectedQuestionCount: Int = 8,

    // Active Question Solving (Tek tek)
    val questions: List<DersQuestion> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val isCurrentAnswerSubmitted: Boolean = false,
    val isCurrentAnswerCorrect: Boolean? = null,
    val isHintRevealed: Boolean = false,

    // Current user inputs for the active question:
    val selectedOptionIndex: Int? = null, // MULTIPLE_CHOICE (0..3)
    val selectedBoolean: Boolean? = null, // TRUE_FALSE
    val textInputAnswer: String = "", // FILL_IN_THE_BLANK & SHORT_ANSWER
    val matchingUserPairs: Map<Int, Int> = emptyMap(), // leftIdx -> rightIdx for MATCHING
    val selectedMatchingLeft: Int? = null,

    // Answer records
    val userAnswers: Map<String, String> = emptyMap(), // questionId -> answerStr
    val isCorrectMap: Map<String, Boolean> = emptyMap(), // questionId -> isCorrect

    // Summary
    val missingTopics: List<String> = emptyList(),
    val recommendedStudyTopics: List<String> = emptyList(),
    val coachFeedback: String = "",
    val currentRoundNumber: Int = 1,

    // Past saved sessions
    val pastSessions: List<DersStudySessionRecord> = emptyList(),
    val viewingPastSession: DersStudySessionRecord? = null
)

class DersViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("ders_quiz_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(DersUiState())
    val uiState: StateFlow<DersUiState> = _uiState.asStateFlow()

    init {
        loadPastSessions()
    }

    // --- 1. UPLOAD STEP ACTIONS ---

    fun setUploadedImage(uri: Uri?, base64: String?) {
        _uiState.update {
            it.copy(
                uploadedImageUri = uri?.toString(),
                uploadedImageBase64 = base64
            )
        }
    }

    fun removeUploadedImage() {
        _uiState.update {
            it.copy(
                uploadedImageUri = null,
                uploadedImageBase64 = null
            )
        }
    }

    fun setDocumentFile(fileName: String, content: String) {
        _uiState.update {
            it.copy(
                uploadedFileName = fileName,
                uploadedDocumentText = content
            )
        }
    }

    fun removeDocumentFile() {
        _uiState.update {
            it.copy(
                uploadedFileName = null,
                uploadedDocumentText = ""
            )
        }
    }

    fun setManualNotesText(text: String) {
        _uiState.update {
            it.copy(manualNotesText = text)
        }
    }

    /**
     * Step 2: Analyzes uploaded documents, notes or image.
     */
    fun startDocumentAnalysis() {
        val state = _uiState.value
        val combinedText = buildString {
            if (state.uploadedDocumentText.isNotBlank()) {
                append(state.uploadedDocumentText)
                append("\n\n")
            }
            if (state.manualNotesText.isNotBlank()) {
                append(state.manualNotesText)
            }
        }.trim()

        if (combinedText.isBlank() && state.uploadedImageBase64.isNullOrBlank()) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    flowStep = DersFlowStep.ANALYZING,
                    isAnalyzing = true
                )
            }

            val summary = DersQuestionService.analyzeDocument(
                documentText = combinedText,
                imageBase64 = state.uploadedImageBase64,
                fileName = state.uploadedFileName
            )

            _uiState.update {
                it.copy(
                    flowStep = DersFlowStep.CONFIRMATION,
                    isAnalyzing = false,
                    analysisSummary = summary,
                    selectedQuestionCount = summary.proposedQuestionCount
                )
            }
        }
    }

    // --- 2. CONFIRMATION STEP ACTIONS ---

    fun setQuestionCount(count: Int) {
        _uiState.update {
            it.copy(selectedQuestionCount = count.coerceIn(4, 15))
        }
    }

    fun startQuizSession() {
        val state = _uiState.value
        val summary = state.analysisSummary ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    flowStep = DersFlowStep.ANALYZING,
                    isGeneratingQuestions = true
                )
            }

            val questions = DersQuestionService.generateGroundedQuestions(
                documentText = summary.rawTextContent,
                imageBase64 = summary.imageBase64,
                questionCount = state.selectedQuestionCount,
                topics = summary.detectedTopics,
                roundNumber = state.currentRoundNumber
            )

            _uiState.update {
                it.copy(
                    flowStep = DersFlowStep.QUESTION_ACTIVE,
                    isGeneratingQuestions = false,
                    questions = questions,
                    currentQuestionIndex = 0,
                    isCurrentAnswerSubmitted = false,
                    isCurrentAnswerCorrect = null,
                    isHintRevealed = false,
                    selectedOptionIndex = null,
                    selectedBoolean = null,
                    textInputAnswer = "",
                    matchingUserPairs = emptyMap(),
                    selectedMatchingLeft = null,
                    userAnswers = emptyMap(),
                    isCorrectMap = emptyMap()
                )
            }
        }
    }

    // --- 3 & 4. QUESTION SOLVING (TEK TEK SORMA & ANINDA AÇIKLAMA) ---

    fun revealHint() {
        _uiState.update { it.copy(isHintRevealed = true) }
    }

    fun selectOption(index: Int) {
        if (_uiState.value.isCurrentAnswerSubmitted) return
        _uiState.update { it.copy(selectedOptionIndex = index) }
    }

    fun selectBoolean(value: Boolean) {
        if (_uiState.value.isCurrentAnswerSubmitted) return
        _uiState.update { it.copy(selectedBoolean = value) }
    }

    fun setTextInputAnswer(text: String) {
        if (_uiState.value.isCurrentAnswerSubmitted) return
        _uiState.update { it.copy(textInputAnswer = text) }
    }

    fun selectMatchingLeft(leftIndex: Int) {
        if (_uiState.value.isCurrentAnswerSubmitted) return
        _uiState.update { it.copy(selectedMatchingLeft = leftIndex) }
    }

    fun linkMatchingRight(rightIndex: Int) {
        val currentLeft = _uiState.value.selectedMatchingLeft ?: return
        if (_uiState.value.isCurrentAnswerSubmitted) return

        _uiState.update { state ->
            val updated = state.matchingUserPairs.toMutableMap()
            // Remove any existing match pointing to rightIndex
            val existingKey = updated.entries.firstOrNull { it.value == rightIndex }?.key
            if (existingKey != null) {
                updated.remove(existingKey)
            }
            updated[currentLeft] = rightIndex
            state.copy(
                matchingUserPairs = updated,
                selectedMatchingLeft = null
            )
        }
    }

    fun removeMatchingPair(leftIndex: Int) {
        if (_uiState.value.isCurrentAnswerSubmitted) return
        _uiState.update { state ->
            val updated = state.matchingUserPairs.toMutableMap()
            updated.remove(leftIndex)
            state.copy(matchingUserPairs = updated)
        }
    }

    /**
     * Submits answer for the active question and immediately evaluates correctness + displays explanation.
     */
    fun submitCurrentAnswer() {
        val state = _uiState.value
        val question = state.questions.getOrNull(state.currentQuestionIndex) ?: return
        if (state.isCurrentAnswerSubmitted) return

        var isCorrect = false
        var answerRepresentation = ""

        when (question.type) {
            DersQuestionType.MULTIPLE_CHOICE -> {
                val selected = state.selectedOptionIndex ?: return
                isCorrect = selected == question.correctOptionIndex
                val optLetter = listOf("A", "B", "C", "D").getOrElse(selected) { "${selected + 1}" }
                answerRepresentation = "$optLetter Şıkkı"
            }
            DersQuestionType.TRUE_FALSE -> {
                val selected = state.selectedBoolean ?: return
                isCorrect = selected == question.isTrue
                answerRepresentation = if (selected) "Doğru" else "Yanlış"
            }
            DersQuestionType.FILL_IN_THE_BLANK -> {
                val input = state.textInputAnswer.trim()
                if (input.isEmpty()) return
                val normalizedInput = input.lowercase(Locale("tr"))
                val normalizedTarget = question.correctAnswerText.trim().lowercase(Locale("tr"))
                val matchesTarget = normalizedInput == normalizedTarget
                val matchesAcceptable = question.acceptableAnswers.any {
                    normalizedInput == it.trim().lowercase(Locale("tr"))
                }
                isCorrect = matchesTarget || matchesAcceptable
                answerRepresentation = input
            }
            DersQuestionType.SHORT_ANSWER -> {
                val input = state.textInputAnswer.trim()
                if (input.isEmpty()) return
                val normalizedInput = input.lowercase(Locale("tr"))
                val normalizedTarget = question.correctAnswerText.trim().lowercase(Locale("tr"))
                val matchesTarget = normalizedInput == normalizedTarget
                val matchesAcceptable = question.acceptableAnswers.any {
                    normalizedInput == it.trim().lowercase(Locale("tr"))
                }
                isCorrect = matchesTarget || matchesAcceptable || (normalizedTarget.isNotEmpty() && normalizedInput.contains(normalizedTarget))
                answerRepresentation = input
            }
            DersQuestionType.MATCHING -> {
                val userPairs = state.matchingUserPairs
                if (userPairs.size < question.matchingPairs.size) return
                // Check if all left indices match their canonical right pairs
                // question.matchingPairs has left at index i, right at index i
                var allMatched = true
                for (i in question.matchingPairs.indices) {
                    if (userPairs[i] != i) {
                        allMatched = false
                        break
                    }
                }
                isCorrect = allMatched
                answerRepresentation = "${userPairs.size} Eşleştirme Yapıldı"
            }
        }

        val updatedUserAnswers = state.userAnswers.toMutableMap()
        updatedUserAnswers[question.id] = answerRepresentation

        val updatedCorrectMap = state.isCorrectMap.toMutableMap()
        updatedCorrectMap[question.id] = isCorrect

        _uiState.update {
            it.copy(
                isCurrentAnswerSubmitted = true,
                isCurrentAnswerCorrect = isCorrect,
                userAnswers = updatedUserAnswers,
                isCorrectMap = updatedCorrectMap
            )
        }
    }

    /**
     * Move to next question, or transition to Step 5 (Summary) if on last question.
     */
    fun goToNextQuestionOrFinish() {
        val state = _uiState.value
        val nextIndex = state.currentQuestionIndex + 1

        if (nextIndex < state.questions.size) {
            _uiState.update {
                it.copy(
                    currentQuestionIndex = nextIndex,
                    isCurrentAnswerSubmitted = false,
                    isCurrentAnswerCorrect = null,
                    isHintRevealed = false,
                    selectedOptionIndex = null,
                    selectedBoolean = null,
                    textInputAnswer = "",
                    matchingUserPairs = emptyMap(),
                    selectedMatchingLeft = null
                )
            }
        } else {
            // All questions finished -> Generate summary
            finishQuizAndShowSummary()
        }
    }

    // --- 5. SUMMARY & REVIEW ---

    private fun finishQuizAndShowSummary() {
        val state = _uiState.value
        val questions = state.questions
        val correctMap = state.isCorrectMap
        val userAnswers = state.userAnswers
        val docTitle = state.analysisSummary?.documentTitle ?: "Ders Notu Testi"

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    flowStep = DersFlowStep.SUMMARY,
                    isGeneratingSummary = true
                )
            }

            val (missing, recommended, feedback) = DersQuestionService.generateDersStudySummary(
                questions = questions,
                isCorrectMap = correctMap,
                userAnswers = userAnswers,
                documentTitle = docTitle
            )

            val now = LocalDateTime.now()
            val formattedDate = now.format(DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale("tr")))
            val totalCorrect = questions.count { correctMap[it.id] == true }
            val totalIncorrect = questions.size - totalCorrect

            val sessionRecord = DersStudySessionRecord(
                title = docTitle,
                formattedDate = formattedDate,
                documentSummary = state.analysisSummary?.documentOverview ?: "",
                questions = questions,
                userAnswers = userAnswers,
                isCorrectMap = correctMap,
                totalQuestions = questions.size,
                correctCount = totalCorrect,
                incorrectCount = totalIncorrect,
                missingTopics = missing,
                recommendedStudyTopics = recommended,
                coachFeedback = feedback
            )

            saveSessionLocally(sessionRecord)

            _uiState.update {
                it.copy(
                    isGeneratingSummary = false,
                    missingTopics = missing,
                    recommendedStudyTopics = recommended,
                    coachFeedback = feedback,
                    pastSessions = listOf(sessionRecord) + it.pastSessions
                )
            }
        }
    }

    /**
     * Special feature: "Kullanıcı tekrar isterse aynı konudan yeni sorular üretir"
     */
    fun generateNewQuestionsOnSameTopic() {
        val state = _uiState.value
        val summary = state.analysisSummary ?: return
        val nextRound = state.currentRoundNumber + 1

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    flowStep = DersFlowStep.ANALYZING,
                    isGeneratingQuestions = true,
                    currentRoundNumber = nextRound
                )
            }

            val questions = DersQuestionService.generateGroundedQuestions(
                documentText = summary.rawTextContent,
                imageBase64 = summary.imageBase64,
                questionCount = state.selectedQuestionCount,
                topics = summary.detectedTopics,
                roundNumber = nextRound
            )

            _uiState.update {
                it.copy(
                    flowStep = DersFlowStep.QUESTION_ACTIVE,
                    isGeneratingQuestions = false,
                    questions = questions,
                    currentQuestionIndex = 0,
                    isCurrentAnswerSubmitted = false,
                    isCurrentAnswerCorrect = null,
                    isHintRevealed = false,
                    selectedOptionIndex = null,
                    selectedBoolean = null,
                    textInputAnswer = "",
                    matchingUserPairs = emptyMap(),
                    selectedMatchingLeft = null,
                    userAnswers = emptyMap(),
                    isCorrectMap = emptyMap()
                )
            }
        }
    }

    fun startNewDocument() {
        _uiState.update {
            DersUiState(
                flowStep = DersFlowStep.UPLOAD,
                pastSessions = it.pastSessions
            )
        }
    }

    fun viewPastSession(record: DersStudySessionRecord) {
        _uiState.update {
            it.copy(viewingPastSession = record)
        }
    }

    fun closePastSessionView() {
        _uiState.update {
            it.copy(viewingPastSession = null)
        }
    }

    fun deletePastSession(recordId: String) {
        val updated = _uiState.value.pastSessions.filterNot { it.id == recordId }
        _uiState.update { it.copy(pastSessions = updated) }
        viewModelScope.launch(Dispatchers.IO) {
            persistAllSessions(updated)
        }
    }

    // --- PERSISTENCE HELPERS ---

    private fun loadPastSessions() {
        viewModelScope.launch(Dispatchers.IO) {
            val json = prefs.getString("saved_ders_sessions", null) ?: return@launch
            val list = mutableListOf<DersStudySessionRecord>()
            try {
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(deserializeSession(obj))
                }
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(pastSessions = list) }
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
    }

    private fun saveSessionLocally(record: DersStudySessionRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = listOf(record) + _uiState.value.pastSessions.filterNot { it.id == record.id }
            persistAllSessions(updated)
        }
    }

    private fun persistAllSessions(list: List<DersStudySessionRecord>) {
        val array = JSONArray()
        list.take(20).forEach { session ->
            array.put(serializeSession(session))
        }
        prefs.edit().putString("saved_ders_sessions", array.toString()).apply()
    }

    private fun serializeSession(s: DersStudySessionRecord): JSONObject {
        return JSONObject().apply {
            put("id", s.id)
            put("title", s.title)
            put("timestamp", s.timestamp)
            put("formattedDate", s.formattedDate)
            put("documentSummary", s.documentSummary)
            put("totalQuestions", s.totalQuestions)
            put("correctCount", s.correctCount)
            put("incorrectCount", s.incorrectCount)
            put("coachFeedback", s.coachFeedback)

            val mArr = JSONArray()
            s.missingTopics.forEach { mArr.put(it) }
            put("missingTopics", mArr)

            val rArr = JSONArray()
            s.recommendedStudyTopics.forEach { rArr.put(it) }
            put("recommendedStudyTopics", rArr)

            val qArr = JSONArray()
            s.questions.forEach { q ->
                val qObj = JSONObject().apply {
                    put("id", q.id)
                    put("type", q.type.name)
                    put("topic", q.topic)
                    put("questionText", q.questionText)
                    put("explanation", q.explanation)
                    put("hint", q.hint)
                    put("correctOptionIndex", q.correctOptionIndex)
                    put("isTrue", q.isTrue)
                    put("correctAnswerText", q.correctAnswerText)

                    val optArr = JSONArray()
                    q.options.forEach { optArr.put(it) }
                    put("options", optArr)

                    val pArr = JSONArray()
                    q.matchingPairs.forEach { p ->
                        pArr.put(JSONObject().apply {
                            put("left", p.left)
                            put("right", p.right)
                        })
                    }
                    put("matchingPairs", pArr)
                }
                qArr.put(qObj)
            }
            put("questions", qArr)
        }
    }

    private fun deserializeSession(obj: JSONObject): DersStudySessionRecord {
        val missing = mutableListOf<String>()
        val mArr = obj.optJSONArray("missingTopics")
        if (mArr != null) {
            for (i in 0 until mArr.length()) missing.add(mArr.getString(i))
        }

        val recommended = mutableListOf<String>()
        val rArr = obj.optJSONArray("recommendedStudyTopics")
        if (rArr != null) {
            for (i in 0 until rArr.length()) recommended.add(rArr.getString(i))
        }

        val questions = mutableListOf<DersQuestion>()
        val qArr = obj.optJSONArray("questions")
        if (qArr != null) {
            for (i in 0 until qArr.length()) {
                val qObj = qArr.getJSONObject(i)
                val typeName = qObj.optString("type", "MULTIPLE_CHOICE")
                val type = try { DersQuestionType.valueOf(typeName) } catch (e: Exception) { DersQuestionType.MULTIPLE_CHOICE }

                val opts = mutableListOf<String>()
                val optArr = qObj.optJSONArray("options")
                if (optArr != null) {
                    for (j in 0 until optArr.length()) opts.add(optArr.getString(j))
                }

                val pairs = mutableListOf<MatchingPair>()
                val pArr = qObj.optJSONArray("matchingPairs")
                if (pArr != null) {
                    for (k in 0 until pArr.length()) {
                        val pObj = pArr.getJSONObject(k)
                        pairs.add(MatchingPair(pObj.optString("left"), pObj.optString("right")))
                    }
                }

                questions.add(
                    DersQuestion(
                        id = qObj.optString("id"),
                        type = type,
                        topic = qObj.optString("topic", "Genel"),
                        questionText = qObj.optString("questionText", ""),
                        options = opts,
                        correctOptionIndex = qObj.optInt("correctOptionIndex", 0),
                        isTrue = qObj.optBoolean("isTrue", true),
                        correctAnswerText = qObj.optString("correctAnswerText", ""),
                        matchingPairs = pairs,
                        explanation = qObj.optString("explanation", ""),
                        hint = qObj.optString("hint", "")
                    )
                )
            }
        }

        return DersStudySessionRecord(
            id = obj.optString("id"),
            title = obj.optString("title", "Ders Çalışması"),
            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
            formattedDate = obj.optString("formattedDate", ""),
            documentSummary = obj.optString("documentSummary", ""),
            questions = questions,
            userAnswers = emptyMap(),
            isCorrectMap = emptyMap(),
            totalQuestions = obj.optInt("totalQuestions", questions.size),
            correctCount = obj.optInt("correctCount", 0),
            incorrectCount = obj.optInt("incorrectCount", 0),
            missingTopics = missing,
            recommendedStudyTopics = recommended,
            coachFeedback = obj.optString("coachFeedback", "")
        )
    }
}
