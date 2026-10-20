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
import com.nororoutine.batu.model.QuizQuestion
import com.nororoutine.batu.model.StudySession
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

    private val _studySessions = MutableStateFlow<List<StudySession>>(emptyList())
    val studySessions: StateFlow<List<StudySession>> = _studySessions.asStateFlow()

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

        // Load Study Sessions (Kaynak/Fotoğraf tabanlı çalışmalar)
        val studiesJson = prefs.getString("saved_study_sessions", null)
        val loadedStudies = mutableListOf<StudySession>()
        if (!studiesJson.isNullOrBlank()) {
            try {
                val array = JSONArray(studiesJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    loadedStudies.add(deserializeStudySession(obj))
                }
            } catch (e: Exception) {
                Log.e("YksRepository", "Failed loading local study sessions", e)
            }
        }

        if (loadedStudies.isEmpty()) {
            // Starter sample study session so the user immediately sees past history with questions & analysis
            loadedStudies.add(
                StudySession(
                    title = "Kuvvet & Hareket Grafikleri Analizi",
                    formattedDate = "18 Ekim 2026, 16:45",
                    description = "Görseldeki hız-zaman ve kuvvet grafiğine benzer 2028 Maarif modeli 3 soru hazırla",
                    isCompleted = true,
                    correctCount = 2,
                    incorrectCount = 1,
                    emptyCount = 0,
                    netScore = 1.75f,
                    analysisFeedback = "Hız-zaman grafiğinin altındaki alanın yer değiştirmeyi verdiğini başarıyla uyguladın. Ancak eğimin ivme olduğunu hesaplarken işaret hatası yaptın. Newton yasalarında sürtünmeli eğik düzlem ve grafik eğim analizine ağırlık vermelisin.",
                    weakTopicsToFocus = listOf("Hız-Zaman Grafik Eğimi (İvme)", "Eğik Düzlem Sürtünme Kuvveti"),
                    userAnswers = mapOf("q1" to 1, "q2" to 2, "q3" to 0),
                    questions = listOf(
                        QuizQuestion(
                            id = "q1",
                            subject = "Fizik",
                            topic = "Kuvvet ve Hareket",
                            questionText = "Doğrusal bir yolda hareket eden bir aracın hız-zaman grafiği verilmiştir. Araç 0-4 saniye aralığında sabit 3 m/s² ivmeyle hızlanıp, 4-8 saniye aralığında sabit hızla hareket etmektedir. 8 saniye sonundaki toplam yer değiştirmesi kaç metredir? (İlk hız v₀ = 0)",
                            options = listOf("A) 48 m", "B) 72 m", "C) 96 m", "D) 120 m", "E) 144 m"),
                            correctOptionIndex = 1,
                            explanation = "0-4 sn aralığında son hız v = a·t = 3·4 = 12 m/s. Üçgen alanı: (4 · 12) / 2 = 24 m. 4-8 sn aralığında dikdörtgen alanı: 4 · 12 = 48 m. Toplam yer değiştirme = 24 + 48 = 72 metredir.",
                            skillType = "Grafik Yorumlama ve Matematiksel Modelleme",
                            hints = listOf("1. İpucu: Hız-zaman grafiğinde alan yer değiştirmeyi verir.", "2. İpucu: 0-4 sn arasını üçgen, 4-8 sn arasını dikdörtgen olarak böl.")
                        ),
                        QuizQuestion(
                            id = "q2",
                            subject = "Fizik",
                            topic = "Newton'un Hareket Yasaları",
                            questionText = "Kütlesi 4 kg olan bir cisme yatay sürtünmeli zeminde F = 20 N büyüklüğünde yatay bir kuvvet uygulanmaktadır. Zemin ile cisim arasındaki sürtünme katsayısı k = 0.25 olduğuna göre cismin ivmesi kaç m/s²'dir? (g = 10 m/s²)",
                            options = listOf("A) 1.5", "B) 2.0", "C) 2.5", "D) 3.0", "E) 4.0"),
                            correctOptionIndex = 2,
                            explanation = "Sürtünme kuvveti Fs = k·m·g = 0.25 · 4 · 10 = 10 N. Net kuvvet Fnet = F - Fs = 20 - 10 = 10 N. İvme a = Fnet / m = 10 / 4 = 2.5 m/s².",
                            skillType = "Neden-Sonuç ve Süreç Analizi",
                            hints = listOf("1. İpucu: Önce yüzeyin tepki kuvvetinden sürtünme kuvvetini bul.", "2. İpucu: Fnet = m · a formülünü uygula.")
                        ),
                        QuizQuestion(
                            id = "q3",
                            subject = "Kimya",
                            topic = "Kimyasal Tepkimelerde Enerji",
                            questionText = "Standart koşullarda gerçekleşen bir tepkimede girenlerin potansiyel enerjisi 45 kJ, aktifleşmiş kompleksin enerjisi 110 kJ ve ürünlerin enerjisi 15 kJ'dir. Bu tepkimenin ileri aktifleşme enerjisi (Eai) ve tepkime entalpisi (ΔH) aşağıdakilerden hangisidir?",
                            options = listOf("A) Eai = 65 kJ, ΔH = -30 kJ", "B) Eai = 65 kJ, ΔH = +30 kJ", "C) Eai = 95 kJ, ΔH = -30 kJ", "D) Eai = 110 kJ, ΔH = -15 kJ", "E) Eai = 45 kJ, ΔH = +65 kJ"),
                            correctOptionIndex = 0,
                            explanation = "Eai = Eaktifleşmiş - Egirenler = 110 - 45 = 65 kJ. ΔH = Eürünler - Egirenler = 15 - 45 = -30 kJ (Ekzotermik).",
                            skillType = "Veri Analizi ve Çıkarım Yapma",
                            hints = listOf("1. İpucu: İleri aktifleşme enerjisi tepe noktası ile girenler arasındaki farktır.", "2. İpucu: ΔH = Ürünler - Girenler formülünü kullan.")
                        )
                    )
                )
            )
        }
        _studySessions.value = loadedStudies.sortedByDescending { it.timestamp }
    }

    suspend fun saveStudySession(session: StudySession) = withContext(Dispatchers.IO) {
        val current = _studySessions.value.toMutableList()
        val index = current.indexOfFirst { it.id == session.id }
        if (index >= 0) {
            current[index] = session
        } else {
            current.add(0, session)
        }
        _studySessions.value = current.sortedByDescending { it.timestamp }
        persistStudiesLocally()
    }

    suspend fun deleteStudySession(sessionId: String) = withContext(Dispatchers.IO) {
        val current = _studySessions.value.filterNot { it.id == sessionId }
        _studySessions.value = current
        persistStudiesLocally()
    }

    private fun persistStudiesLocally() {
        val array = JSONArray()
        for (session in _studySessions.value) {
            array.put(serializeStudySession(session))
        }
        prefs.edit().putString("saved_study_sessions", array.toString()).apply()
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

    private fun serializeStudySession(session: StudySession): JSONObject {
        return JSONObject().apply {
            put("id", session.id)
            put("title", session.title)
            put("timestamp", session.timestamp)
            put("formattedDate", session.formattedDate)
            put("description", session.description)
            put("imageUri", session.imageUri ?: "")
            put("isCompleted", session.isCompleted)
            put("correctCount", session.correctCount)
            put("incorrectCount", session.incorrectCount)
            put("emptyCount", session.emptyCount)
            put("netScore", session.netScore.toDouble())
            put("analysisFeedback", session.analysisFeedback)

            val weaksArr = JSONArray()
            session.weakTopicsToFocus.forEach { weaksArr.put(it) }
            put("weakTopicsToFocus", weaksArr)

            val ansObj = JSONObject()
            session.userAnswers.forEach { (k, v) -> ansObj.put(k, v) }
            put("userAnswers", ansObj)

            val qArr = JSONArray()
            session.questions.forEach { q ->
                qArr.put(serializeQuizQuestion(q))
            }
            put("questions", qArr)
        }
    }

    private fun deserializeStudySession(obj: JSONObject): StudySession {
        val weaks = mutableListOf<String>()
        val wArr = obj.optJSONArray("weakTopicsToFocus")
        if (wArr != null) {
            for (i in 0 until wArr.length()) weaks.add(wArr.getString(i))
        }

        val answers = mutableMapOf<String, Int>()
        val ansObj = obj.optJSONObject("userAnswers")
        if (ansObj != null) {
            val keys = ansObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                answers[k] = ansObj.optInt(k, -1)
            }
        }

        val qList = mutableListOf<QuizQuestion>()
        val qArr = obj.optJSONArray("questions")
        if (qArr != null) {
            for (i in 0 until qArr.length()) {
                qList.add(deserializeQuizQuestion(qArr.getJSONObject(i)))
            }
        }

        return StudySession(
            id = obj.optString("id"),
            title = obj.optString("title", "Çalışma Testi"),
            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
            formattedDate = obj.optString("formattedDate", ""),
            description = obj.optString("description", ""),
            imageUri = obj.optString("imageUri").takeIf { it.isNotBlank() },
            questions = qList,
            userAnswers = answers,
            isCompleted = obj.optBoolean("isCompleted", false),
            correctCount = obj.optInt("correctCount", 0),
            incorrectCount = obj.optInt("incorrectCount", 0),
            emptyCount = obj.optInt("emptyCount", 0),
            netScore = obj.optDouble("netScore", 0.0).toFloat(),
            analysisFeedback = obj.optString("analysisFeedback", ""),
            weakTopicsToFocus = weaks
        )
    }

    private fun serializeQuizQuestion(q: QuizQuestion): JSONObject {
        return JSONObject().apply {
            put("id", q.id)
            put("subject", q.subject)
            put("topic", q.topic)
            put("questionText", q.questionText)
            put("correctOptionIndex", q.correctOptionIndex)
            put("explanation", q.explanation)
            put("skillType", q.skillType)
            put("revealedHintLevel", q.revealedHintLevel)

            val optArr = JSONArray()
            q.options.forEach { optArr.put(it) }
            put("options", optArr)

            val hArr = JSONArray()
            q.hints.forEach { hArr.put(it) }
            put("hints", hArr)
        }
    }

    private fun deserializeQuizQuestion(qObj: JSONObject): QuizQuestion {
        val opts = mutableListOf<String>()
        val optArr = qObj.optJSONArray("options")
        if (optArr != null) {
            for (i in 0 until optArr.length()) opts.add(optArr.getString(i))
        }

        val hints = mutableListOf<String>()
        val hArr = qObj.optJSONArray("hints")
        if (hArr != null) {
            for (i in 0 until hArr.length()) hints.add(hArr.getString(i))
        }

        return QuizQuestion(
            id = qObj.optString("id"),
            subject = qObj.optString("subject", "Ders"),
            topic = qObj.optString("topic", "Konu"),
            questionText = qObj.optString("questionText", ""),
            options = if (opts.isNotEmpty()) opts else listOf("A) Doğru", "B) Yanlış", "C) Belirsiz", "D) Yok", "E) Hepsi"),
            correctOptionIndex = qObj.optInt("correctOptionIndex", 0),
            explanation = qObj.optString("explanation", ""),
            skillType = qObj.optString("skillType", "Beceri Temelli Muhakeme"),
            hints = hints,
            revealedHintLevel = qObj.optInt("revealedHintLevel", 0)
        )
    }
}
