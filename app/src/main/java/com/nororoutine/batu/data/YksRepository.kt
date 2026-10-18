package com.nororoutine.batu.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.nororoutine.batu.NoroRoutineApp
import com.nororoutine.batu.model.ChatMessage
import com.nororoutine.batu.model.DeficiencyItem
import com.nororoutine.batu.model.ExamSubjectResult
import com.nororoutine.batu.model.StudyPrescriptionItem
import com.nororoutine.batu.model.YksAiAnalysis
import com.nororoutine.batu.model.YksExamRecord
import com.nororoutine.batu.model.YksExamType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class YksRepository(
    private val context: Context = NoroRoutineApp.instance
) {
    private val prefs = context.getSharedPreferences("yks_coach_storage", Context.MODE_PRIVATE)

    private val _examRecords = MutableStateFlow<List<YksExamRecord>>(emptyList())
    val examRecords: StateFlow<List<YksExamRecord>> = _examRecords.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val db: FirebaseFirestore?
        get() = try {
            if (FirebaseApp.getApps(context).isNotEmpty()) FirebaseFirestore.getInstance() else null
        } catch (e: Exception) {
            null
        }

    init {
        loadLocalData()
    }

    private fun loadLocalData() {
        val examsJson = prefs.getString("saved_exams", null)
        val loadedExams = mutableListOf<YksExamRecord>()
        if (!examsJson.isNullOrBlank()) {
            try {
                val array = JSONArray(examsJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    loadedExams.add(deserializeExam(obj))
                }
            } catch (e: Exception) {
                Log.e("YksRepository", "Failed loading local exams", e)
            }
        }

        // If no exams yet, add a starter sample Maarif model TYT deneme so the student sees immediate value!
        if (loadedExams.isEmpty()) {
            val sample = YksExamRecord(
                title = "Özdebir Türkiye Geneli TYT-1",
                date = "2026-10-15",
                examType = YksExamType.TYT,
                results = listOf(
                    ExamSubjectResult("Türkçe", 32, 5, 3, 30.75f, listOf("Paragrafta Çıkarım", "Anlatım Teknikleri")),
                    ExamSubjectResult("Temel Matematik", 24, 6, 10, 22.5f, listOf("Problemler", "Fonksiyonlar", "Kombinatorik")),
                    ExamSubjectResult("Fen Bilimleri", 14, 4, 2, 13.0f, listOf("Fizik - Optik", "Kimya - Mol")),
                    ExamSubjectResult("Sosyal Bilimler", 16, 3, 1, 15.25f, listOf("Coğrafya - İklim"))
                ),
                notes = "Süre yönetiminde son 15 dakikada matematikte acele ettim."
            )
            loadedExams.add(sample)
        }

        _examRecords.value = loadedExams.sortedByDescending { it.date }

        val chatJson = prefs.getString("saved_chat", null)
        val loadedChat = mutableListOf<ChatMessage>()
        if (!chatJson.isNullOrBlank()) {
            try {
                val array = JSONArray(chatJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val chipsArr = obj.optJSONArray("chips")
                    val chips = mutableListOf<String>()
                    if (chipsArr != null) {
                        for (j in 0 until chipsArr.length()) chips.add(chipsArr.getString(j))
                    }
                    loadedChat.add(
                        ChatMessage(
                            id = obj.optString("id"),
                            sender = obj.optString("sender"),
                            text = obj.optString("text"),
                            timestamp = obj.optLong("timestamp"),
                            suggestedChips = chips
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("YksRepository", "Failed loading local chat", e)
            }
        }

        if (loadedChat.isEmpty()) {
            loadedChat.add(
                ChatMessage(
                    sender = "ai",
                    text = "Merhaba! Ben senin 2028 YKS & Türkiye Yüzyılı Maarif Modeli Başarı Koçunum. 🎯\n\nBurada deneme sonuçlarını analiz edebilir, en çok puan kaybettiğin kritik eksiklerini nokta atışı tespit edebilir ve Maarif Modelinin yeni nesil soru tarzına göre birlikte çalışabiliriz.\n\nBugün hangi konuyu veya denemeni konuşmak istersin?",
                    suggestedChips = listOf(
                        "2028 Maarif Modeli Nedir?",
                        "Son Denememi Analiz Et",
                        "Matematik Neti Nasıl Artar?"
                    )
                )
            )
        }
        _chatHistory.value = loadedChat
    }

    suspend fun saveExam(exam: YksExamRecord) = withContext(Dispatchers.IO) {
        val current = _examRecords.value.toMutableList()
        val index = current.indexOfFirst { it.id == exam.id }
        if (index >= 0) {
            current[index] = exam
        } else {
            current.add(0, exam)
        }
        _examRecords.value = current.sortedByDescending { it.date }
        persistExamsLocally()

        // Sync to Firestore
        try {
            db?.collection("yks_exams")?.document(exam.id)?.set(
                mapOf(
                    "id" to exam.id,
                    "title" to exam.title,
                    "date" to exam.date,
                    "examType" to exam.examType.name,
                    "totalNet" to exam.totalNet,
                    "notes" to exam.notes,
                    "timestamp" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
        } catch (e: Exception) {
            Log.w("YksRepository", "Firestore exam sync ignored: ${e.message}")
        }
    }

    suspend fun deleteExam(examId: String) = withContext(Dispatchers.IO) {
        val current = _examRecords.value.filterNot { it.id == examId }
        _examRecords.value = current
        persistExamsLocally()

        try {
            db?.collection("yks_exams")?.document(examId)?.delete()
        } catch (e: Exception) {
            Log.w("YksRepository", "Firestore exam delete ignored: ${e.message}")
        }
    }

    suspend fun addChatMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        val current = _chatHistory.value.toMutableList()
        current.add(message)
        _chatHistory.value = current
        persistChatLocally()
    }

    suspend fun clearChat() = withContext(Dispatchers.IO) {
        _chatHistory.value = listOf(
            ChatMessage(
                sender = "ai",
                text = "Sohbet temizlendi. 2028 YKS ve Maarif Modeli hakkında her an yanındayım! Ne sormak istersin?",
                suggestedChips = listOf("Deneme Analizi Yap", "Matematik Taktikleri", "Yeni Nesil Soru Çöz")
            )
        )
        persistChatLocally()
    }

    private fun persistExamsLocally() {
        val array = JSONArray()
        for (exam in _examRecords.value) {
            array.put(serializeExam(exam))
        }
        prefs.edit().putString("saved_exams", array.toString()).apply()
    }

    private fun persistChatLocally() {
        val array = JSONArray()
        for (msg in _chatHistory.value.takeLast(50)) {
            val obj = JSONObject().apply {
                put("id", msg.id)
                put("sender", msg.sender)
                put("text", msg.text)
                put("timestamp", msg.timestamp)
                val chipsArr = JSONArray()
                msg.suggestedChips.forEach { chipsArr.put(it) }
                put("chips", chipsArr)
            }
            array.put(obj)
        }
        prefs.edit().putString("saved_chat", array.toString()).apply()
    }

    private fun serializeExam(exam: YksExamRecord): JSONObject {
        return JSONObject().apply {
            put("id", exam.id)
            put("title", exam.title)
            put("date", exam.date)
            put("examType", exam.examType.name)
            put("totalNet", exam.totalNet.toDouble())
            put("targetNet", exam.targetNet.toDouble())
            put("notes", exam.notes)

            val resultsArr = JSONArray()
            for (res in exam.results) {
                val rObj = JSONObject().apply {
                    put("subjectName", res.subjectName)
                    put("correct", res.correct)
                    put("incorrect", res.incorrect)
                    put("empty", res.empty)
                    put("net", res.net.toDouble())
                    val weaks = JSONArray()
                    res.weakTopics.forEach { weaks.put(it) }
                    put("weakTopics", weaks)
                }
                resultsArr.put(rObj)
            }
            put("results", resultsArr)

            if (exam.aiAnalysis != null) {
                val aObj = JSONObject().apply {
                    put("summary", exam.aiAnalysis.summary)
                    put("estimatedStanding", exam.aiAnalysis.estimatedStanding)
                    put("maarifAdvice", exam.aiAnalysis.maarifAdvice)
                    put("isAiGenerated", exam.aiAnalysis.isAiGenerated)

                    val spArr = JSONArray()
                    exam.aiAnalysis.strongPoints.forEach { spArr.put(it) }
                    put("strongPoints", spArr)

                    val defArr = JSONArray()
                    exam.aiAnalysis.criticalDeficiencies.forEach { def ->
                        val dObj = JSONObject().apply {
                            put("subject", def.subject)
                            put("topic", def.topic)
                            put("severity", def.severity)
                            put("actionPlan", def.actionPlan)
                        }
                        defArr.put(dObj)
                    }
                    put("criticalDeficiencies", defArr)

                    val wpArr = JSONArray()
                    exam.aiAnalysis.weeklyPrescription.forEach { item ->
                        val wpObj = JSONObject().apply {
                            put("phase", item.phase)
                            put("targetTask", item.targetTask)
                            put("recommendedQuestionCount", item.recommendedQuestionCount)
                        }
                        wpArr.put(wpObj)
                    }
                    put("weeklyPrescription", wpArr)
                }
                put("aiAnalysis", aObj)
            }
        }
    }

    private fun deserializeExam(obj: JSONObject): YksExamRecord {
        val examType = try {
            YksExamType.valueOf(obj.optString("examType", "TYT"))
        } catch (e: Exception) {
            YksExamType.TYT
        }

        val resultsList = mutableListOf<ExamSubjectResult>()
        val resultsArr = obj.optJSONArray("results")
        if (resultsArr != null) {
            for (i in 0 until resultsArr.length()) {
                val rObj = resultsArr.getJSONObject(i)
                val weaks = mutableListOf<String>()
                val wArr = rObj.optJSONArray("weakTopics")
                if (wArr != null) {
                    for (j in 0 until wArr.length()) weaks.add(wArr.getString(j))
                }
                resultsList.add(
                    ExamSubjectResult(
                        subjectName = rObj.optString("subjectName", "Ders"),
                        correct = rObj.optInt("correct", 0),
                        incorrect = rObj.optInt("incorrect", 0),
                        empty = rObj.optInt("empty", 0),
                        net = rObj.optDouble("net", 0.0).toFloat(),
                        weakTopics = weaks
                    )
                )
            }
        }

        var aiAnalysis: YksAiAnalysis? = null
        val aObj = obj.optJSONObject("aiAnalysis")
        if (aObj != null) {
            val strongPoints = mutableListOf<String>()
            val spArr = aObj.optJSONArray("strongPoints")
            if (spArr != null) {
                for (i in 0 until spArr.length()) strongPoints.add(spArr.getString(i))
            }

            val deficiencies = mutableListOf<DeficiencyItem>()
            val defArr = aObj.optJSONArray("criticalDeficiencies")
            if (defArr != null) {
                for (i in 0 until defArr.length()) {
                    val d = defArr.getJSONObject(i)
                    deficiencies.add(
                        DeficiencyItem(
                            subject = d.optString("subject"),
                            topic = d.optString("topic"),
                            severity = d.optString("severity", "Yüksek"),
                            actionPlan = d.optString("actionPlan")
                        )
                    )
                }
            }

            val prescriptions = mutableListOf<StudyPrescriptionItem>()
            val wpArr = aObj.optJSONArray("weeklyPrescription")
            if (wpArr != null) {
                for (i in 0 until wpArr.length()) {
                    val wp = wpArr.getJSONObject(i)
                    prescriptions.add(
                        StudyPrescriptionItem(
                            phase = wp.optString("phase"),
                            targetTask = wp.optString("targetTask"),
                            recommendedQuestionCount = wp.optInt("recommendedQuestionCount", 100)
                        )
                    )
                }
            }

            aiAnalysis = YksAiAnalysis(
                summary = aObj.optString("summary"),
                estimatedStanding = aObj.optString("estimatedStanding"),
                strongPoints = strongPoints,
                criticalDeficiencies = deficiencies,
                weeklyPrescription = prescriptions,
                maarifAdvice = aObj.optString("maarifAdvice"),
                isAiGenerated = aObj.optBoolean("isAiGenerated", false)
            )
        }

        return YksExamRecord(
            id = obj.optString("id"),
            title = obj.optString("title", "Deneme"),
            date = obj.optString("date", "2026-10-18"),
            examType = examType,
            results = resultsList,
            totalNet = obj.optDouble("totalNet", resultsList.sumOf { it.net.toDouble() }).toFloat(),
            targetNet = obj.optDouble("targetNet", 90.0).toFloat(),
            notes = obj.optString("notes", ""),
            aiAnalysis = aiAnalysis
        )
    }
}
