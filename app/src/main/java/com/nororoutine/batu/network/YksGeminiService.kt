package com.nororoutine.batu.network

import android.util.Log
import com.nororoutine.batu.BuildConfig
import com.nororoutine.batu.model.ChatMessage
import com.nororoutine.batu.model.DeficiencyItem
import com.nororoutine.batu.model.ExamSubjectResult
import com.nororoutine.batu.model.GraphPoint
import com.nororoutine.batu.model.MathSolution
import com.nororoutine.batu.model.QuestionGraphInfo
import com.nororoutine.batu.model.QuizQuestion
import com.nororoutine.batu.model.StudyPrescriptionItem
import com.nororoutine.batu.model.YksAiAnalysis
import com.nororoutine.batu.model.YksExamRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object YksGeminiService {
    private const val TAG = "YksGeminiService"
    private const val MODEL = "gemini-3.5-flash"

    /**
     * Analyses YKS trial exam results with specific focus on 2028 YKS & Türkiye Yüzyılı Maarif Modeli.
     */
    suspend fun analyzeExamWithAi(exam: YksExamRecord): YksAiAnalysis = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateHeuristicAnalysis(exam)
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 12000
                readTimeout = 12000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            val subjectDetails = exam.results.joinToString("\n") { res ->
                val weaks = if (res.weakTopics.isNotEmpty()) " (Zorlanılan Konular: ${res.weakTopics.joinToString(", ")})" else ""
                "- ${res.subjectName}: Doğru: ${res.correct}, Yanlış: ${res.incorrect}, Boş: ${res.empty}, Net: %.2f$weaks".format(res.net)
            }

            val prompt = """
                Sen 2028 YKS ve Türkiye Yüzyılı Maarif Modeli müfredatında uzmanlaşmış kıdemli bir YKS Eğitim Koçusun.
                Öğrencinin son deneme sınavı sonuçları:
                Deneme Adı: ${exam.title}
                Sınav Türü: ${exam.examType.displayName}
                Toplam Net: %.2f / Hedef Net: %.2f
                Tarih: ${exam.date}
                Öğrenci Notları: ${exam.notes.ifBlank { "Yok" }}
                
                Ders ve Konu Dökümü:
                $subjectDetails
                
                ÖNEMLİ BAĞLAM:
                2028 YKS sınavı 'Türkiye Yüzyılı Maarif Modeli'ne göre hazırlanmaktadır. Ezber formüller yerine beceri temelli, grafik ve veri yorumlama, disiplinlerarası akıl yürütme, bağlamsal problem çözme ve süreç odaklı düşünme ölçülecektir.
                
                GÖREVİN:
                1. Sonuçları detaylıca analiz et.
                2. En güçlü olduğu yönleri ve acil kapatılması gereken 'Kritik Eksikleri' belirle.
                3. Maarif modeline uygun 2 haftalık net artırma reçetesi (günlük/haftalık görevler ve soru sayıları) çıkar.
                4. Maarif Modeli 2028 sınav taktiği ver (yeni nesil soru yaklaşımı, eleştirel okuma, modelleme).
                
                SADECE aşağıdaki JSON şemasına uygun yanıt ver:
                {
                  "summary": "Öğrencinin genel durumuna dair vurucu, motive edici ve gerçekçi 2-3 cümlelik koçluk özeti",
                  "estimatedStanding": "2028 Maarif modeli hedeflerine göre başarı yüzdesi veya durum derecelendirmesi (örn: İlk 20.000 temposunda, Temel sağlamlaşıyor)",
                  "strongPoints": ["Güçlü yön 1", "Güçlü yön 2"],
                  "criticalDeficiencies": [
                    {
                      "subject": "Ders adı",
                      "topic": "Eksik Konu",
                      "severity": "Yüksek",
                      "actionPlan": "Bu eksiği kapatmak için somut adım (hangi kaynak, kaç soru, kavram haritası vb.)"
                    }
                  ],
                  "weeklyPrescription": [
                    {
                      "phase": "1. Hafta: Kavram & Temel Analiz",
                      "targetTask": "Yapılması gereken somut görev",
                      "recommendedQuestionCount": 150
                    },
                    {
                      "phase": "2. Hafta: Maarif Modeli Beceri Soruları",
                      "targetTask": "Yeni nesil karma deneme ve soru taraması",
                      "recommendedQuestionCount": 200
                    }
                  ],
                  "maarifAdvice": "2028 Maarif Modeli YKS'ye özel nokta atışı koçluk tavsiyesi"
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("responseMimeType", "application/json")
                })
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(requestJson.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val rootJson = JSONObject(responseText)
                val candidates = rootJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val text = candidates.getJSONObject(0).optJSONObject("content")
                        ?.optJSONArray("parts")?.getJSONObject(0)?.optString("text", "") ?: ""
                    val cleaned = text.trim()
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()

                    val parsed = JSONObject(cleaned)
                    val summary = parsed.optString("summary", "Deneme analizi tamamlandı.")
                    val estimatedStanding = parsed.optString("estimatedStanding", "Gelişime açık - 2028 Hedef Yolu")
                    val maarifAdvice = parsed.optString("maarifAdvice", "Maarif modelinde sadece işlem değil, kavramsal akıl yürütme ön plandadır.")

                    val strongPoints = mutableListOf<String>()
                    val strongArr = parsed.optJSONArray("strongPoints")
                    if (strongArr != null) {
                        for (i in 0 until strongArr.length()) strongPoints.add(strongArr.getString(i))
                    }

                    val deficiencies = mutableListOf<DeficiencyItem>()
                    val defArr = parsed.optJSONArray("criticalDeficiencies")
                    if (defArr != null) {
                        for (i in 0 until defArr.length()) {
                            val obj = defArr.getJSONObject(i)
                            deficiencies.add(
                                DeficiencyItem(
                                    subject = obj.optString("subject", "Genel"),
                                    topic = obj.optString("topic", "Konu"),
                                    severity = obj.optString("severity", "Yüksek"),
                                    actionPlan = obj.optString("actionPlan", "Konu tekrarı ve soru çözümü")
                                )
                            )
                        }
                    }

                    val prescriptions = mutableListOf<StudyPrescriptionItem>()
                    val presArr = parsed.optJSONArray("weeklyPrescription")
                    if (presArr != null) {
                        for (i in 0 until presArr.length()) {
                            val obj = presArr.getJSONObject(i)
                            prescriptions.add(
                                StudyPrescriptionItem(
                                    phase = obj.optString("phase", "Aşama ${i + 1}"),
                                    targetTask = obj.optString("targetTask", "Görev planı"),
                                    recommendedQuestionCount = obj.optInt("recommendedQuestionCount", 120)
                                )
                            )
                        }
                    }

                    return@withContext YksAiAnalysis(
                        summary = summary,
                        estimatedStanding = estimatedStanding,
                        strongPoints = if (strongPoints.isNotEmpty()) strongPoints else listOf("Temel ders dengesi korunuyor"),
                        criticalDeficiencies = deficiencies,
                        weeklyPrescription = prescriptions,
                        maarifAdvice = maarifAdvice,
                        isAiGenerated = true
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling Gemini for exam analysis", e)
        }

        return@withContext generateHeuristicAnalysis(exam)
    }

    /**
     * Interactive Chat with AI Study Coach with full context of routines, weekly analysis, and trial exams.
     */
    suspend fun chatWithCoach(
        history: List<ChatMessage>,
        userMessage: String,
        latestExam: YksExamRecord? = null,
        routinesContext: String = "",
        weeklyAnalysisContext: String = "",
        examsSummaryContext: String = ""
    ): ChatMessage = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateHeuristicChatResponse(
                userMessage = userMessage,
                latestExam = latestExam,
                routinesContext = routinesContext,
                weeklyAnalysisContext = weeklyAnalysisContext,
                examsSummaryContext = examsSummaryContext
            )
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 14000
                readTimeout = 14000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            val conversationPrompt = StringBuilder()
            conversationPrompt.append("Sen 'NöroRoutine Akıllı Ders & Sınav Koçu'sun.\n")
            conversationPrompt.append("ÖĞRENCİNİN CANLI VERİLERİ (Rutinler, Haftalık Analiz, Denemeler):\n")
            if (routinesContext.isNotBlank()) conversationPrompt.append("- Günlük Rutinleri: $routinesContext\n")
            if (weeklyAnalysisContext.isNotBlank()) conversationPrompt.append("- Haftalık Analiz & Alışkanlık Durumu: $weeklyAnalysisContext\n")
            if (examsSummaryContext.isNotBlank()) conversationPrompt.append("- Deneme Sınavları & Net Bilgileri:\n$examsSummaryContext\n")
            else if (latestExam != null) conversationPrompt.append("- Son Denemesi: '${latestExam.title}' (${latestExam.examType.displayName}), Net: %.2f\n".format(latestExam.totalNet))

            conversationPrompt.append("\nTEMEL İLKELERİN VE KURALLARIN:\n")
            conversationPrompt.append("1. Normal, zeki, derin düşünen ve analiz yeteneği olan bir yapay zeka gibi konuş. Asla kalıplaşmış, her soruya aynı olan tekdüze cevaplar verme.\n")
            conversationPrompt.append("2. YALNIZCA ders, çalışma, sınavlar (YKS & MEB Maarif Modeli), rutin planlama ve akademik verimlilik üzerine konuş. Konu dışına çıkma.\n")
            conversationPrompt.append("3. Öğrencinin eklediği rutinlere, haftalık tamamlama analizlerine ve deneme netlerine doğrudan atıfta bulunarak planlamalarını kolaylaştır.\n")
            conversationPrompt.append("4. Samimi, gerçekçi ve motive edici bir dille, madde işaretleri veya paragraflar kullanarak açıklayıcı cevap ver.\n")
            conversationPrompt.append("5. Cevabının sonunda öğrencinin tıklayabileceği 2-3 adet devam önerisi (chips) sun.\n\n")

            // Append last 6 messages for context
            val recentHistory = history.takeLast(6)
            for (msg in recentHistory) {
                val role = if (msg.sender == "user") "Öğrenci" else "Koç"
                conversationPrompt.append("$role: ${msg.text}\n")
            }
            conversationPrompt.append("Öğrenci: $userMessage\n\n")
            conversationPrompt.append("Koç olarak yanıtını şu JSON formatında ver:\n")
            conversationPrompt.append("{\n  \"reply\": \"Cevabın (Türkçe, düzenli paragraflar ve gerekirse madde işaretleri)\",\n  \"chips\": [\"Öneri 1\", \"Öneri 2\", \"Öneri 3\"]\n}")

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", conversationPrompt.toString()) })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("responseMimeType", "application/json")
                })
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(requestJson.toString())
                writer.flush()
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val rootJson = JSONObject(responseText)
                val text = rootJson.optJSONArray("candidates")?.getJSONObject(0)
                    ?.optJSONObject("content")?.optJSONArray("parts")?.getJSONObject(0)?.optString("text", "") ?: ""
                val cleaned = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val parsed = JSONObject(cleaned)
                val reply = parsed.optString("reply", "")
                val chipsJson = parsed.optJSONArray("chips")
                val chips = mutableListOf<String>()
                if (chipsJson != null) {
                    for (i in 0 until chipsJson.length()) chips.add(chipsJson.getString(i))
                }

                if (reply.isNotBlank()) {
                    return@withContext ChatMessage(
                        sender = "ai",
                        text = reply,
                        suggestedChips = chips
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling Gemini chat", e)
        }

        return@withContext generateHeuristicChatResponse(
            userMessage = userMessage,
            latestExam = latestExam,
            routinesContext = routinesContext,
            weeklyAnalysisContext = weeklyAnalysisContext,
            examsSummaryContext = examsSummaryContext
        )
    }

    /**
     * Generates multiple-choice test questions tailored to Türkiye Yüzyılı Maarif Modeli.
     */
    suspend fun generateMaarifQuestions(
        subject: String,
        topic: String,
        count: Int = 3
    ): List<QuizQuestion> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateFallbackQuiz(subject, topic)
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 14000
                readTimeout = 14000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            val prompt = """
                Sen MEB 'Türkiye Yüzyılı Maarif Modeli'ne uygun YKS soruları hazırlayan ölçme ve değerlendirme uzmanısın.
                Ders: $subject
                Konu: $topic
                İstenen Soru Sayısı: $count
                
                SORU KRİTERLERİ (2028 Maarif Modeli İlkeleri):
                1. Ezber bilgi sorma. Günlük hayat bağlamı, bir deney düzeneği, veri tablosu veya grafik analizine dayalı olsun.
                2. Soru kökü eleştirel düşünme, çıkarım yapma veya model kurmayı gerektirsin.
                3. Fizik, Kimya veya Matematik ise ilgili bilimsel grafiği (v-t, x-t, PE tepkime koordinatı, titrasyon pH, parabol) ve formülleri dahil et.
                4. Her soru için KADEMELİ İPUÇLARI (hints) üret:
                   - 1. İpucu: Hangi temel yasa, formül veya kavram kullanılmalı?
                   - 2. İpucu: Çözüm stratejisi, grafik eğimi/alanı veya dikkat edilmesi gereken kritik tuzak.
                5. Her soru tam 5 seçenekli olsun (A, B, C, D, E).
                6. Ayrıntılı çözüm adımı ve hangi Maarif becerisini (Grafik Okuma, Muhakeme, Hipotez Kurma vb.) ölçtüğünü belirt.
                
                SADECE şu JSON şemasında çıktı ver:
                {
                  "questions": [
                    {
                      "questionText": "Soru metni (hikayeleştirilmiş veya analitik kurgulu)",
                      "options": ["A) ...", "B) ...", "C) ...", "D) ...", "E) ..."],
                      "correctOptionIndex": 0,
                      "explanation": "Detaylı adım adım çözüm",
                      "skillType": "Veri Yorumlama ve Muhakeme",
                      "hints": [
                        "1. İpucu (Kavram & Formül): Kullanılacak temel bağıntı...",
                        "2. İpucu (Kritik Strateji & Tuzak): Grafikteki kritik nokta..."
                      ],
                      "graphInfo": {
                        "title": "Grafik Başlığı",
                        "xLabel": "t",
                        "yLabel": "v",
                        "unitX": "s",
                        "unitY": "m/s",
                        "type": "PHYSICS_VT",
                        "a": 2.0,
                        "b": 4.0,
                        "c": 0.0,
                        "highlightAreaLabel": "Alan = Yer Değiştirme (Δx)",
                        "keyPoints": [
                          {"x": 0.0, "y": 4.0, "label": "v₀ = 4 m/s"},
                          {"x": 5.0, "y": 14.0, "label": "v(5) = 14 m/s"}
                        ]
                      }
                    }
                  ]
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("responseMimeType", "application/json")
                })
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(requestJson.toString())
                writer.flush()
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val rootJson = JSONObject(responseText)
                val text = rootJson.optJSONArray("candidates")?.getJSONObject(0)
                    ?.optJSONObject("content")?.optJSONArray("parts")?.getJSONObject(0)?.optString("text", "") ?: ""
                val cleaned = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val parsed = JSONObject(cleaned)
                val qArr = parsed.optJSONArray("questions")
                if (qArr != null && qArr.length() > 0) {
                    val list = mutableListOf<QuizQuestion>()
                    for (i in 0 until qArr.length()) {
                        val qObj = qArr.getJSONObject(i)
                        val optArr = qObj.optJSONArray("options")
                        val options = mutableListOf<String>()
                        if (optArr != null) {
                            for (j in 0 until optArr.length()) options.add(optArr.getString(j))
                        }

                        // Parse progressive hints
                        val hintsList = mutableListOf<String>()
                        val hArr = qObj.optJSONArray("hints")
                        if (hArr != null && hArr.length() > 0) {
                            for (h in 0 until hArr.length()) hintsList.add(hArr.getString(h))
                        } else {
                            hintsList.add("1. İpucu: $subject dersindeki temel formül ve verilen nicelikleri tespit et.")
                            hintsList.add("2. İpucu: Maarif modelinde sorunun gizlediği kritik bağıntıya ve seçenek birimlerine dikkat et.")
                        }

                        var graphInfo: QuestionGraphInfo? = null
                        val gObj = qObj.optJSONObject("graphInfo")
                        if (gObj != null) {
                            val pts = mutableListOf<GraphPoint>()
                            val ptArr = gObj.optJSONArray("keyPoints")
                            if (ptArr != null) {
                                for (k in 0 until ptArr.length()) {
                                    val p = ptArr.getJSONObject(k)
                                    pts.add(
                                        GraphPoint(
                                            x = p.optDouble("x", 0.0).toFloat(),
                                            y = p.optDouble("y", 0.0).toFloat(),
                                            label = p.optString("label", "")
                                        )
                                    )
                                }
                            }
                            graphInfo = QuestionGraphInfo(
                                title = gObj.optString("title", "Bilimsel Grafik"),
                                xLabel = gObj.optString("xLabel", "x"),
                                yLabel = gObj.optString("yLabel", "y"),
                                unitX = gObj.optString("unitX", ""),
                                unitY = gObj.optString("unitY", ""),
                                type = gObj.optString("type", "PARABOLA"),
                                a = gObj.optDouble("a", 1.0).toFloat(),
                                b = gObj.optDouble("b", 0.0).toFloat(),
                                c = gObj.optDouble("c", 0.0).toFloat(),
                                keyPoints = pts,
                                minX = gObj.optDouble("minX", -6.0).toFloat(),
                                maxX = gObj.optDouble("maxX", 6.0).toFloat(),
                                minY = gObj.optDouble("minY", -6.0).toFloat(),
                                maxY = gObj.optDouble("maxY", 8.0).toFloat(),
                                highlightAreaLabel = gObj.optString("highlightAreaLabel", ""),
                                extraNotes = gObj.optString("extraNotes", "")
                            )
                        }

                        list.add(
                            QuizQuestion(
                                subject = subject,
                                topic = topic,
                                questionText = qObj.optString("questionText", "Soru"),
                                options = options,
                                correctOptionIndex = qObj.optInt("correctOptionIndex", 0),
                                explanation = qObj.optString("explanation", "Çözüm"),
                                skillType = qObj.optString("skillType", "Beceri Temelli Analiz"),
                                graphInfo = graphInfo,
                                hints = hintsList,
                                hint = hintsList.firstOrNull()
                            )
                        )
                    }
                    if (list.isNotEmpty()) return@withContext list
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling Gemini for quiz questions", e)
        }

        return@withContext generateFallbackQuiz(subject, topic)
    }

    /**
     * Generates a 5-option Quiz from uploaded resources (image, document, or custom description)
     */
    suspend fun generateStudyQuizFromResource(
        description: String,
        questionCount: Int = 5,
        imageBase64: String? = null,
        docText: String? = null
    ): List<QuizQuestion> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateFallbackStudyQuiz(description, questionCount, docText)
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 18000
                readTimeout = 18000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            val prompt = """
                Sen MEB 'Türkiye Yüzyılı Maarif Modeli'ne ve YKS 2028 formatına hakim, kıdemli bir soru yazarısın.
                Öğrencinin sağladığı kaynak/görsel/belge ve talimatı:
                Açıklama / İstek: "$description"
                ${if (!docText.isNullOrBlank()) "Metin/Belge İçeriği:\n$docText" else ""}
                İstenen Soru Sayısı: $questionCount

                SORU VE TEST KURALLARI (KESİN VE EKSİKSİZ):
                1. Kaynakta veya görselde yer alan soru, konu, formül veya bilgiyi temel al.
                2. Öğrencinin açıklamasına uygun olarak TAM $questionCount adet soru üret.
                3. Her soru istisnasız TAM 5 SEÇENEKLİ olmalıdır (A, B, C, D, E).
                4. Her soru için öğrencinin çözüm anında basıp yararlanabileceği yol gösterici bir İPUCU ('hints': [ipucu_metni]) ekle. İpucu cevabı doğrudan söylememeli, hangi kavramı veya formülü düşünmesi gerektiğini hatırlatmalıdır.
                5. Her soru için doğru seçeneğin indeksini ('correctOptionIndex': 0..4; 0=A, 1=B, 2=C, 3=D, 4=E) ve adım adım kapsamlı çözümünü ('explanation') belirt.
                6. Sorular ezber değil, yeni nesil çıkarım, beceri temelli ve analiz odaklı olsun.

                SADECE şu JSON şemasında çıktı ver:
                {
                  "questions": [
                    {
                      "subject": "Ders Adı (örn: Fizik, Kimya, Matematik)",
                      "topic": "Konu Başlığı",
                      "questionText": "Soru metni...",
                      "options": ["A) ...", "B) ...", "C) ...", "D) ...", "E) ..."],
                      "correctOptionIndex": 0,
                      "explanation": "Detaylı adım adım çözüm...",
                      "skillType": "Beceri Temelli Muhakeme",
                      "hints": ["İpucu: ..."]
                    }
                  ]
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            if (!imageBase64.isNullOrBlank()) {
                                put(JSONObject().apply {
                                    put("inlineData", JSONObject().apply {
                                        put("mimeType", "image/jpeg")
                                        put("data", imageBase64)
                                    })
                                })
                            }
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("responseMimeType", "application/json")
                })
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(requestJson.toString())
                writer.flush()
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val rootJson = JSONObject(responseText)
                val text = rootJson.optJSONArray("candidates")?.getJSONObject(0)
                    ?.optJSONObject("content")?.optJSONArray("parts")?.getJSONObject(0)?.optString("text", "") ?: ""
                val cleaned = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val parsed = JSONObject(cleaned)
                val qArr = parsed.optJSONArray("questions")
                if (qArr != null && qArr.length() > 0) {
                    val list = mutableListOf<QuizQuestion>()
                    for (i in 0 until qArr.length()) {
                        val qObj = qArr.getJSONObject(i)
                        val optArr = qObj.optJSONArray("options")
                        val options = mutableListOf<String>()
                        if (optArr != null) {
                            for (j in 0 until optArr.length()) options.add(optArr.getString(j))
                        }
                        val hintsList = mutableListOf<String>()
                        val hArr = qObj.optJSONArray("hints")
                        if (hArr != null && hArr.length() > 0) {
                            for (h in 0 until hArr.length()) hintsList.add(hArr.getString(h))
                        } else {
                            hintsList.add("İpucu: Sorudaki temel kavramı ve verilenleri dikkatle analiz et.")
                        }

                        list.add(
                            QuizQuestion(
                                subject = qObj.optString("subject", "Ders"),
                                topic = qObj.optString("topic", "Genel"),
                                questionText = qObj.optString("questionText", "Soru"),
                                options = if (options.size == 5) options else (options.take(5) + listOf("A) Doğru", "B) Yanlış", "C) Belirsiz", "D) Yok", "E) Hepsi")).take(5),
                                correctOptionIndex = qObj.optInt("correctOptionIndex", 0).coerceIn(0, 4),
                                explanation = qObj.optString("explanation", "Çözüm"),
                                skillType = qObj.optString("skillType", "Beceri Temelli Muhakeme"),
                                hints = hintsList,
                                hint = hintsList.firstOrNull()
                            )
                        )
                    }
                    if (list.isNotEmpty()) return@withContext list
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling Gemini for study quiz", e)
        }

        return@withContext generateFallbackStudyQuiz(description, questionCount, docText)
    }

    /**
     * Generates a comprehensive feedback and analysis report after quiz completion.
     */
    suspend fun generateStudyFeedbackAnalysis(
        studyTitle: String,
        questions: List<QuizQuestion>,
        userAnswers: Map<String, Int>,
        correctCount: Int,
        incorrectCount: Int,
        emptyCount: Int
    ): Pair<String, List<String>> = withContext(Dispatchers.IO) {
        val wrongTopics = mutableListOf<String>()
        val correctTopics = mutableListOf<String>()

        questions.forEach { q ->
            val userAns = userAnswers[q.id]
            if (userAns == q.correctOptionIndex) {
                correctTopics.add(q.topic)
            } else {
                wrongTopics.add(q.topic)
            }
        }

        val total = questions.size
        val net = (correctCount.toFloat() - (incorrectCount.toFloat() * 0.25f)).coerceAtLeast(0f)

        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey"
                val url = URL(endpoint)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 12000
                    readTimeout = 12000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }

                val prompt = """
                    Sen MEB 2028 YKS Maarif Modeli uzmanı kıdemli bir eğitim koçusun.
                    Öğrenci az önce kaynak tabanlı bir çalışma quizi çözdü:
                    Test: $studyTitle
                    Toplam Soru: $total, Doğru: $correctCount, Yanlış: $incorrectCount, Boş: $emptyCount, Net: %.2f
                    Doğru Yapılan Konular: ${correctTopics.distinct().joinToString(", ").ifBlank { "Yok" }}
                    Yanlış/Boş Konular: ${wrongTopics.distinct().joinToString(", ").ifBlank { "Yok" }}

                    GÖREVİN:
                    Öğrencinin bu testteki performansını kontrol et. Özellikle 'şu konulara daha çok önem ver ve çalış' şeklinde nokta atışı yönlendir, güçlü yönlerini ve zayıf noktalarını açıkla.
                    
                    Şu JSON şemasında yanıt ver:
                    {
                      "analysis": "2-3 paragraflık samimi, net ve yönlendirici koçluk analizi",
                      "weakTopicsToFocus": ["Odaklanılması gereken 1. konu", "2. konu"]
                    }
                """.trimIndent().format(net)

                val requestJson = JSONObject().apply {
                    val contents = JSONArray().apply {
                        val contentObj = JSONObject().apply {
                            val parts = JSONArray().apply {
                                put(JSONObject().apply { put("text", prompt) })
                            }
                            put("parts", parts)
                        }
                        put(contentObj)
                    }
                    put("contents", contents)
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.7)
                        put("responseMimeType", "application/json")
                    })
                }

                OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                    writer.write(requestJson.toString())
                    writer.flush()
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val resp = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                    val root = JSONObject(resp)
                    val text = root.optJSONArray("candidates")?.getJSONObject(0)
                        ?.optJSONObject("content")?.optJSONArray("parts")?.getJSONObject(0)?.optString("text", "") ?: ""
                    val cleaned = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                    val p = JSONObject(cleaned)
                    val analysis = p.optString("analysis", "")
                    val wList = mutableListOf<String>()
                    val wArr = p.optJSONArray("weakTopicsToFocus")
                    if (wArr != null) {
                        for (i in 0 until wArr.length()) wList.add(wArr.getString(i))
                    }
                    if (analysis.isNotBlank()) {
                        return@withContext analysis to (if (wList.isNotEmpty()) wList else wrongTopics.distinct())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini study analysis failed", e)
            }
        }

        // Heuristic analysis
        val distinctWrong = wrongTopics.distinct()
        val distinctCorrect = correctTopics.distinct()
        val feedback = buildString {
            append("🎯 **Çalışma Testi Değerlendirme ve Gelişim Analizi:**\n\n")
            append("Toplam $total soruda $correctCount Doğru, $incorrectCount Yanlış ve $emptyCount Boş ile %.2f Net elde ettin.\n\n".format(net))
            if (distinctWrong.isNotEmpty()) {
                append("⚠️ **Bu Konulara Daha Çok Önem Ver ve Çalış:**\n")
                distinctWrong.forEach { t ->
                    append("• **$t:** Bu konudaki soru yapısında ve kavram ilişkilerinde eksiklik görünüyor. Konu özetini gözden geçirip en az 15-20 yeni nesil soru çözmelisin.\n")
                }
                append("\n")
            }
            if (distinctCorrect.isNotEmpty()) {
                append("✅ **Güçlü Olduğun Alanlar:** ${distinctCorrect.joinToString(", ")} konularındaki muhakemen oldukça sağlam.\n\n")
            }
            append("💡 **Koç Tavsiyesi:** Maarif modelinde sorular bilgiyi doğrudan değil bir model/durum içinde sorduğundan, yanlış yaptığın sorularda şıkları neden elediğini mutlaka çözüm açıklaması ve ipuçları üzerinden incele.")
        }

        return@withContext feedback to (if (distinctWrong.isNotEmpty()) distinctWrong else listOf("Kavram Pekiştirme"))
    }

    /**
     * Solves mathematical operations/equations step-by-step and generates graph plotting parameters.
     */
    suspend fun solveMathProblem(problem: String): MathSolution = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateHeuristicMathSolution(problem)
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 14000
                readTimeout = 14000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            val prompt = """
                Sen 2028 YKS ve Türkiye Yüzyılı Maarif Modeli Matematik, Geometri, Fizik ve Kimya Problem & Denklem Çözücüsü ve Bilimsel Modelleme Koçusun.
                Öğrencinin çözülmesini, formüllerini ve grafiksel modellemesini istediği soru veya denklem:
                $problem

                GÖREVİN:
                1. Bu problemin hangi derse ait olduğunu belirle (Matematik, Fizik veya Kimya).
                2. Eğer Kimya ise denkleştirilmiş tepkime denklemini (chemicalEquation, örn: "2H₂(g) + O₂(g) → 2H₂O(s)") ve entalpiyi belirt.
                3. Eğer Fizik ise kullanılan fizik formüllerini (physicsFormulas, örn: ["v = v₀ + at", "Δx = v₀t + ½at²"]) çıkar.
                4. Adım adım anlaşılır bilimsel çözüm basamaklarını yaz (steps).
                5. Nihai cevabı birimleriyle net yaz (finalAnswer).
                6. 2028 Maarif Modeli kazanım çıkarımını yaz (maarifKeyTakeaway).
                7. Bu konuya ait bilimsel grafiği koordinat sisteminde çizmek için parametreleri oluştur (graphInfo):
                   - title: Grafik başlığı
                   - type: Matematik için (PARABOLA, LINEAR, CUBIC, TRIGONOMETRIC), Fizik için (PHYSICS_VT, PHYSICS_XT, PHYSICS_AT, PHYSICS_PV, PHYSICS_WAVE, PHYSICS_FX), Kimya için (CHEM_ENERGY_PROFILE, CHEM_TITRATION, CHEM_SOLUBILITY, CHEM_HEATING_CURVE, CHEM_MAXWELL, CHEM_EQUILIBRIUM)
                   - xLabel, yLabel, unitX, unitY, a, b, c, highlightAreaLabel, extraNotes, keyPoints.

                SADECE şu JSON şemasında çıktı ver:
                {
                  "subject": "Matematik / Fizik / Kimya",
                  "chemicalEquation": "Varsa kimyasal denklem",
                  "physicsFormulas": ["Formül 1", "Formül 2"],
                  "steps": [
                    "1. Adım...",
                    "2. Adım..."
                  ],
                  "finalAnswer": "Nihai sonuç ve birimi",
                  "maarifKeyTakeaway": "2028 Maarif Modeli analitik çıkarımı",
                  "graphInfo": {
                    "title": "Grafik Başlığı",
                    "type": "PARABOLA / PHYSICS_VT / CHEM_ENERGY_PROFILE",
                    "xLabel": "x ekseni adı",
                    "yLabel": "y ekseni adı",
                    "unitX": "birim (s, m, mL vb)",
                    "unitY": "birim (m/s, kJ, pH vb)",
                    "a": 1.0,
                    "b": 0.0,
                    "c": 0.0,
                    "highlightAreaLabel": "Varsa alan etiketi",
                    "extraNotes": "Kavramsal dipnot",
                    "keyPoints": [
                      {"x": 0.0, "y": 0.0, "label": "Etiket"}
                    ]
                  }
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.5)
                    put("responseMimeType", "application/json")
                })
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(requestJson.toString())
                writer.flush()
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val rootJson = JSONObject(responseText)
                val text = rootJson.optJSONArray("candidates")?.getJSONObject(0)
                    ?.optJSONObject("content")?.optJSONArray("parts")?.getJSONObject(0)?.optString("text", "") ?: ""
                val cleaned = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val parsed = JSONObject(cleaned)

                val detectedSubject = parsed.optString("subject", "Matematik")
                val chemEquation = parsed.optString("chemicalEquation").takeIf { it.isNotBlank() }
                val physicsFormulasList = mutableListOf<String>()
                val fArr = parsed.optJSONArray("physicsFormulas")
                if (fArr != null) {
                    for (i in 0 until fArr.length()) physicsFormulasList.add(fArr.getString(i))
                }

                val stepsList = mutableListOf<String>()
                val sArr = parsed.optJSONArray("steps")
                if (sArr != null) {
                    for (i in 0 until sArr.length()) stepsList.add(sArr.getString(i))
                }

                val finalAnswer = parsed.optString("finalAnswer", "Çözüm tamamlandı.")
                val takeaway = parsed.optString("maarifKeyTakeaway", "Maarif Modeli bilimsel ve fonksiyonel modellemeyi ön planda tutar.")

                var gInfo: QuestionGraphInfo? = null
                val gObj = parsed.optJSONObject("graphInfo")
                if (gObj != null) {
                    val pts = mutableListOf<GraphPoint>()
                    val ptArr = gObj.optJSONArray("keyPoints")
                    if (ptArr != null) {
                        for (k in 0 until ptArr.length()) {
                            val p = ptArr.getJSONObject(k)
                            pts.add(
                                GraphPoint(
                                    x = p.optDouble("x", 0.0).toFloat(),
                                    y = p.optDouble("y", 0.0).toFloat(),
                                    label = p.optString("label", "")
                                )
                            )
                        }
                    }
                    gInfo = QuestionGraphInfo(
                        title = gObj.optString("title", "Bilimsel Grafik"),
                        xLabel = gObj.optString("xLabel", "x"),
                        yLabel = gObj.optString("yLabel", "y"),
                        unitX = gObj.optString("unitX", ""),
                        unitY = gObj.optString("unitY", ""),
                        type = gObj.optString("type", "PARABOLA"),
                        a = gObj.optDouble("a", 1.0).toFloat(),
                        b = gObj.optDouble("b", 0.0).toFloat(),
                        c = gObj.optDouble("c", 0.0).toFloat(),
                        keyPoints = pts,
                        highlightAreaLabel = gObj.optString("highlightAreaLabel", ""),
                        extraNotes = gObj.optString("extraNotes", "")
                    )
                }

                return@withContext MathSolution(
                    originalProblem = problem,
                    steps = if (stepsList.isNotEmpty()) stepsList else listOf("Adımlar tamamlandı."),
                    finalAnswer = finalAnswer,
                    graphInfo = gInfo,
                    maarifKeyTakeaway = takeaway,
                    subject = detectedSubject,
                    chemicalEquation = chemEquation,
                    physicsFormulas = physicsFormulasList
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling Gemini for math solution", e)
        }

        return@withContext generateHeuristicMathSolution(problem)
    }

    // --- HEURISTIC ENGINES (Works seamlessly offline & without API key) ---

    private fun generateHeuristicAnalysis(exam: YksExamRecord): YksAiAnalysis {
        val totalNet = exam.totalNet
        val deficiencies = mutableListOf<DeficiencyItem>()
        val strongPoints = mutableListOf<String>()

        exam.results.forEach { res ->
            if (res.net >= 25f || (res.correct > 20 && res.incorrect <= 4)) {
                strongPoints.add("${res.subjectName}: %.1f Net ile çok sağlam bir temel gösteriyor.".format(res.net))
            } else if (res.net < 12f || res.incorrect >= 6 || res.weakTopics.isNotEmpty()) {
                val topic = res.weakTopics.firstOrNull() ?: "${res.subjectName} Temel Kavramlar"
                deficiencies.add(
                    DeficiencyItem(
                        subject = res.subjectName,
                        topic = topic,
                        severity = if (res.net < 8f || res.incorrect > 8) "Yüksek" else "Orta",
                        actionPlan = "2028 Maarif modelinde bu konu saf ezber değil; soru köklerindeki deney ve grafik senaryoları taranmalı. Haftada 60 soru + 1 analiz videosu."
                    )
                )
            }
        }

        if (deficiencies.isEmpty()) {
            deficiencies.add(
                DeficiencyItem(
                    subject = "Genel Süre Yönetimi",
                    topic = "Soru Başına Harcanan Süre",
                    severity = "Orta",
                    actionPlan = "Türkçe paragraf ve Matematik problemlerinde tur bindirme tekniği ile turlama stratejisini geliştir."
                )
            )
        }

        if (strongPoints.isEmpty()) {
            strongPoints.add("Sınavı son dakikaya kadar bırakmadan tamamlama iraden yüksek.")
            strongPoints.add("Doğru çözdüğün konularda işlem hatası yapma oranın düşük.")
        }

        val estimatedStanding = when {
            totalNet >= 85f -> "🌟 Üst Düzey Başarı Tempomuz (İlk 15.000 Aday Profili)"
            totalNet >= 65f -> "📈 İstikrarlı Yükseliş: 2028 Hedefinde Güçlü İlerleme"
            else -> "🛠️ Temel İnşa Dönemi: Maarif Modeliyle Net Patlaması Mümkün"
        }

        val summary = "Denemede toplam %.1f net yaptın. 2028 YKS için Türkiye Yüzyılı Maarif Modeli'nin getirdiği en büyük avantaj, erken dönemde ezber yerine mantık-muhakeme kaslarını geliştirmendir. Tespit ettiğimiz ${deficiencies.size} kritik konuyu kapatarak önümüzdeki 3 denemede +8 ile +12 net artış yakalayabilirsin.".format(totalNet)

        val prescriptions = listOf(
            StudyPrescriptionItem(
                phase = "1. Hafta: Kritik Açıkları Kapatma",
                targetTask = "Belirlenen ${deficiencies.take(2).joinToString(" ve ") { it.topic }} konularından kavram haritası çıkar ve soru çöz.",
                recommendedQuestionCount = 140
            ),
            StudyPrescriptionItem(
                phase = "2. Hafta: Maarif Modeli Yeni Nesil Tarama",
                targetTask = "Tüm derslerden süre tutarak beceri temelli karma mini branş denemesi uygula.",
                recommendedQuestionCount = 180
            )
        )

        val maarifAdvice = "Türkiye Yüzyılı Maarif Modeli'nde sorular 'Bu formül neydi?' diye sormaz; 'Bu veriyi analiz edip gerçeğe nasıl uyarlarsın?' der. Çalışırken her formülün mantığını kendine sesli açıkla ve bol bol grafik/şema yorumla."

        return YksAiAnalysis(
            summary = summary,
            estimatedStanding = estimatedStanding,
            strongPoints = strongPoints,
            criticalDeficiencies = deficiencies,
            weeklyPrescription = prescriptions,
            maarifAdvice = maarifAdvice,
            isAiGenerated = false
        )
    }

    private fun generateHeuristicChatResponse(
        userMessage: String,
        latestExam: YksExamRecord? = null,
        routinesContext: String = "",
        weeklyAnalysisContext: String = "",
        examsSummaryContext: String = ""
    ): ChatMessage {
        val lower = userMessage.lowercase()

        val (reply, chips) = when {
            lower.contains("rutin") || lower.contains("plan") || lower.contains("program") || lower.contains("günlük") -> {
                val rutinText = if (routinesContext.isNotBlank()) {
                    "📌 **Mevcut Rutinlerin:**\n$routinesContext\n"
                } else {
                    "Henüz eklenmiş bir rutinin görünmüyor ama ana ekrandan dilediğin ders ve alışkanlık rutinlerini ekleyebilirsin.\n"
                }
                val text = """
                    📅 **Kişiselleştirilmiş Rutin & Çalışma Planı Analizi:**
                    
                    $rutinText
                    ${if (weeklyAnalysisContext.isNotBlank()) "📊 **Haftalık Durumun:** $weeklyAnalysisContext\n" else ""}
                    **Ders ve Rutin Optimizasyon Önerilerim:**
                    1. **Zihin Tazeliği:** Zihinsel odağın en yüksek olduğu sabah saatlerine Paragraf ve Matematik Problem rutinlerini yerleştirmeni öneririm.
                    2. **Blok Çalışma:** Rutinlerini 45 dk odaklanma + 10 dk mola şeklinde bloklara ayır.
                    3. **Haftalık Deneme:** Haftada en az bir gününü rutin olarak genel denemeye veya branş denemesine ayırıp akşamında net analizi yap.
                    
                    Bu rutinlerin saatlerini düzenlemek veya yenilerini eklemek ister misin?
                """.trimIndent()
                text to listOf("Bugünkü Rutinlerimi İncele", "Deneme Netlerime Göre Plan Yap", "Haftalık Analizimi Göster")
            }
            lower.contains("deneme") || lower.contains("net") || lower.contains("sınav") -> {
                val examsText = if (examsSummaryContext.isNotBlank()) {
                    "📊 **Kayıtlı Deneme Sonuçların:**\n$examsSummaryContext\n"
                } else if (latestExam != null) {
                    "Son girdiğin '${latestExam.title}' denemesinde toplam %.1f net yapmıştın.\n".format(latestExam.totalNet)
                } else {
                    "Henüz sistemde kayıtlı bir denemen bulunmuyor. 'Deneme' sekmesinden çözdüğün denemeleri girdiğinde tüm ders netlerini detaylıca analiz edebilirim!\n"
                }
                val text = """
                    🎯 **Deneme Netleri & Ders Bazlı Gelişim Stratejisi:**
                    
                    $examsText
                    **Netleri Yükseltmek İçin 3 Aşamalı Yol Haritası:**
                    • **Hata Analiz Defteri:** Yanlış veya boş bıraktığın soruların konularını tespit et.
                    • **Nokta Atışı Tekrar:** En çok puan kaybettiğin ilk 2 derse önümüzdeki 3 gün boyunca günlük 20'şer beceri temelli soru çöz.
                    • **Süre Yönetimi:** Denemelerde turlama tekniğini uygula; ilk turda sadece emin olduğun soruları çözüp zorlandıklarını ikinci tura bırak.
                    
                    Hangi dersin netini yükseltmeye odaklanalım?
                """.trimIndent()
                text to listOf("Matematik Netini Artır", "Türkçe Paragraf Taktikleri", "Yeni Deneme Ekle")
            }
            lower.contains("haftalık") || lower.contains("analiz") || lower.contains("performans") -> {
                val perfText = if (weeklyAnalysisContext.isNotBlank()) {
                    "📈 **Haftalık Performans Dökümün:**\n$weeklyAnalysisContext\n"
                } else {
                    "Günlük rutinlerini tamamladıkça haftalık analizlerin ve alışkanlık serilerin burada birikiyor.\n"
                }
                val text = """
                    📊 **Haftalık İlerleme & Verimlilik Değerlendirmesi:**
                    
                    $perfText
                    İstikrarlı çalışma, yüksek motivasyondan daha değerlidir. Günlük rutinlerini aksatmadan sürdürmek, YKS 2028 maratonunda seni en ön sıraya taşıyacaktır.
                    
                    Bu haftaki çalışma hedeflerini gözden geçirelim mi?
                """.trimIndent()
                text to listOf("Rutinlerimi Düzenle", "Zayıf Derslerime Odaklan", "Kaynaklı Quiz Başlat")
            }
            lower.contains("maarif") || lower.contains("2028") -> {
                val text = """
                    🎯 **2028 YKS & Türkiye Yüzyılı Maarif Modeli Yol Haritası:**
                    
                    2028 yılında gireceğin YKS, MEB'in yeni Maarif Modeli felsefesiyle şekilleniyor. İşte bilmen gereken 3 altın kural:
                    
                    1. **Ezber Bitti, Çıkarım Başladı:** Artık doğrudan formül sorusu yerine, formülü veya tanımı soruda verip senden bir grafik veya gerçek hayat senaryosunu yorumlaman istenecek.
                    2. **Disiplinlerarası Sorular:** Örneğin bir Biyoloji kalıtım sorusu Matematik olasılıkla, veya Coğrafya iklim verisi Fizik ısı-sıcaklıkla birleşebilir.
                    3. **Süreç Odaklılık:** Türkçe'de sadece ana fikir değil; yazarın metindeki tutumu, argüman geliştirme biçimi sorgulanacak.
                    
                    Hangi dersinde şu an en çok takılıyorsun? Birlikte nokta atışı taktik geliştirelim!
                """.trimIndent()
                text to listOf("Matematik Problemler Taktikleri", "Türkçe Paragrafı Hızlandırma", "Fizik Deney Soruları")
            }
            lower.contains("matematik") || lower.contains("geometri") -> {
                val text = """
                    📐 **2028 Maarif Modeli Matematik Başarı Reçetesi:**
                    
                    • **Problemler:** Uzun problem metinlerini hızlıca denkleme dökebilmek için soru kökündeki 'değişkenleri' daire içine al.
                    • **Fonksiyonlar:** Grafiği zihninde canlandır. Maarif modelinde fonksiyonların gerçek hayat modellemeleri (hız-zaman, maliyet-kar) sıkça gelir.
                    • **Geometri:** Formülü ezberleme, benzerlik ve Pisagor'un nereden çıktığını mantığınla kavra.
                    
                    Çalışma sekmesinden hemen matematik notu veya sorusu yükleyip sana özel 5 şıklı quiz hazırlatabilirsin!
                """.trimIndent()
                text to listOf("Çalışma Kısmına Git", "Geometriye Nasıl Çalışılır?", "Denememi Analiz Et")
            }
            lower.contains("fizik") || lower.contains("kimya") || lower.contains("biyoloji") || lower.contains("fen") -> {
                val text = """
                    🔬 **Fen Bilimleri (Fizik, Kimya, Biyoloji) Maarif Taktikleri:**
                    
                    • **Fizik:** Formülleri ezberlemek yerine birim analizine ve grafik alan/eğim ilişkilerine odaklan.
                    • **Kimya:** Mol kavramı, tepkime entalpisi ve denge konularında orantı mantığını iyi kur.
                    • **Biyoloji:** Şemaları ve biyolojik süreç döngülerini (fotosentez, hücresel solunum, kalıtım) kendin çizerek çalış.
                    
                    Fen derslerinden çözmek istediğin bir sorunun veya konunun fotoğrafını 'Çalışma' bölümüne yükleyip anında test hazırlatabilirsin!
                """.trimIndent()
                text to listOf("Fen Soru Fotoğrafı Yükle", "Fizik Deney Soruları", "Deneme Netlerimi Gör")
            }
            lower.contains("türkçe") || lower.contains("paragraf") -> {
                val text = """
                    📖 **Maarif Modeli Türkçe & Paragraf Analizi:**
                    
                    Yeni müfredatta paragraflar daha felsefi, bilimsel ve analitik metinlerden oluşuyor.
                    
                    **Taktikler:**
                    1. **Önce Soru Kökünü Oku:** Zihnin ne aradığını bilirse metni tararken vakit kaybetmezsin.
                    2. **Günde 20 Nitelikli Paragraf:** Kronometre tutarak çöz. Soru başına hedef süre: 65 saniye.
                    3. **Yanlış Şıkkı Kanıtla:** 'Bence bu doğru' demek yerine, 'A şıkkı metnin şu cümlesi yüzünden yanlış' diyerek ele.
                """.trimIndent()
                text to listOf("Paragraf Rutini Ekle", "Süre Yönetimi Taktikleri", "Diğer Dersleri Sor")
            }
            else -> {
                val text = """
                    Merhaba! Ben senin **NöroRoutine Akıllı Ders & Sınav Koçunum.** 🎯
                    
                    Senin eklediğin rutinleri, haftalık analizlerini ve girdiğin deneme netlerini yakından takip ediyorum.
                    
                    Birlikte neler yapabiliriz:
                    • **Rutinlerini Değerlendirelim:** Günlük ders saatlerini ve alışkanlıklarını deneme hedeflerine göre planlayalım.
                    • **Denemelerini Analiz Edelim:** Ders bazında doğru/yanlış/netlerini konuşup zayıf konulara nokta atışı reçete çıkaralım.
                    • **Kaynak Tabanlı Quiz:** 'Çalışma' sekmesinde fotoğraf veya not yükleyerek dilediğin sayıda 5 şıklı quiz hazırlatalım.
                    
                    Bugün hangi ders veya konu üzerine konuşmak istersin?
                """.trimIndent()
                text to listOf("Rutinlerime Göre Plan Yap", "Denemelerimi Analiz Et", "Yeni Çalışma Başlat")
            }
        }

        return ChatMessage(
            sender = "ai",
            text = reply,
            suggestedChips = chips
        )
    }

    /**
     * Fallback generator for Study section quizzes (5 options: A, B, C, D, E with hints)
     */
    private fun generateFallbackStudyQuiz(
        description: String,
        count: Int,
        docText: String?
    ): List<QuizQuestion> {
        val lower = (description + " " + (docText ?: "")).lowercase()
        val allQuestions = mutableListOf<QuizQuestion>()

        if (lower.contains("fiz") || lower.contains("hız") || lower.contains("kuvvet") || lower.contains("hareket")) {
            allQuestions.add(
                QuizQuestion(
                    subject = "Fizik",
                    topic = "Doğrusal Hareket ve Hız-Zaman Grafikleri",
                    questionText = "Doğrusal bir ray üzerinde duruştan harekete geçen bir trenin ivmesi ilk 5 saniye boyunca a = 2 m/s²'dir. Sonraki 5 saniye boyunca hızını koruyarak sabit hızla ilerlemektedir. Trenin 10 saniye sonundaki toplam yer değiştirmesi kaç metredir? (v₀ = 0)",
                    options = listOf("A) 50 m", "B) 75 m", "C) 100 m", "D) 125 m", "E) 150 m"),
                    correctOptionIndex = 1,
                    explanation = "0-5 sn arasında son hız v = a·t = 2·5 = 10 m/s. Üçgen alanı: (5·10)/2 = 25 m. 5-10 sn arasında dikdörtgen alanı: 5·10 = 50 m. Toplam yer değiştirme = 25 + 50 = 75 m.",
                    skillType = "Grafik Yorumlama ve Matematiksel Modelleme",
                    hints = listOf("İpucu: Hız-zaman grafiğinin altındaki alan yer değiştirmeyi verir. 0-5 sn arasını üçgen, 5-10 sn arasını dikdörtgen olarak hesapla.")
                )
            )
            allQuestions.add(
                QuizQuestion(
                    subject = "Fizik",
                    topic = "Newton'un Hareket Yasaları",
                    questionText = "Yatay sürtünmeli bir yolda duran 2 kg kütleli bir cisme yatay 18 N büyüklüğünde bir kuvvet uygulanıyor. Zeminle cisim arasındaki sürtünme katsayısı k = 0.4 olduğuna göre cismin ivmesi kaç m/s² olur? (g = 10 m/s²)",
                    options = listOf("A) 3.0", "B) 4.0", "C) 5.0", "D) 6.0", "E) 8.0"),
                    correctOptionIndex = 2,
                    explanation = "Sürtünme kuvveti Fs = k·m·g = 0.4 · 2 · 10 = 8 N. Net kuvvet Fnet = 18 - 8 = 10 N. İvme a = Fnet / m = 10 / 2 = 5.0 m/s².",
                    skillType = "Sebep-Sonuç İlişkisi Kurma",
                    hints = listOf("İpucu: Önce cismin ağırlığından sürtünme kuvvetini bul, ardından Fnet = m · a formülünü uygula.")
                )
            )
        }

        if (lower.contains("kimya") || lower.contains("mol") || lower.contains("entalpi") || lower.contains("enerji")) {
            allQuestions.add(
                QuizQuestion(
                    subject = "Kimya",
                    topic = "Tepkimelerde Enerji ve Entalpi",
                    questionText = "Bir kimyasal tepkimenin potansiyel enerji diyagramında girenlerin enerjisi 60 kJ, aktifleşmiş kompleksin enerjisi 150 kJ ve ürünlerin enerjisi 20 kJ'dir. Bu tepkimenin geri aktifleşme enerjisi (Eag) ve entalpi değişimi (ΔH) aşağıdakilerden hangisinde doğru verilmiştir?",
                    options = listOf(
                        "A) Eag = 90 kJ, ΔH = -40 kJ",
                        "B) Eag = 130 kJ, ΔH = -40 kJ",
                        "C) Eag = 130 kJ, ΔH = +40 kJ",
                        "D) Eag = 90 kJ, ΔH = +40 kJ",
                        "E) Eag = 150 kJ, ΔH = -60 kJ"
                    ),
                    correctOptionIndex = 1,
                    explanation = "Eag = Eaktifleşmiş - Eürünler = 150 - 20 = 130 kJ. ΔH = Eürünler - Egirenler = 20 - 60 = -40 kJ (Ekzotermik).",
                    skillType = "Veri Analizi ve Model Kurma",
                    hints = listOf("İpucu: Geri aktifleşme enerjisi tepe noktası ile ürünlerin enerjisi arasındaki farktır.")
                )
            )
        }

        // Generic / Math fallback questions to satisfy requested question count
        allQuestions.add(
            QuizQuestion(
                subject = "Matematik",
                topic = "Parabol ve Fonksiyon Modelleme",
                questionText = "f(x) = -x² + 6x - 5 fonksiyonunun tepe noktasının koordinatları (r, k) aşağıdakilerden hangisidir?",
                options = listOf("A) (3, 4)", "B) (3, -4)", "C) (-3, 4)", "D) (2, 3)", "E) (4, 3)"),
                correctOptionIndex = 0,
                explanation = "r = -b / (2a) = -6 / (2 · -1) = 3. k = f(3) = -(3)² + 6·3 - 5 = -9 + 18 - 5 = 4. Tepe noktası T(3, 4)'tür.",
                skillType = "Analitik Muhakeme",
                hints = listOf("İpucu: Parabolün tepe noktası apsisi r = -b / (2a) formülüyle bulunur.")
            )
        )
        allQuestions.add(
            QuizQuestion(
                subject = "Matematik",
                topic = "Yeni Nesil Problem Modelleme",
                questionText = "Bir öğrenci her gün bir önceki gün çözdüğü soru sayısının 10 fazlasını çözmektedir. 1. gün 25 soru çözen bu öğrenci 6. günün sonunda toplam kaç soru çözmüş olur?",
                options = listOf("A) 240", "B) 270", "C) 300", "D) 330", "E) 360"),
                correctOptionIndex = 2,
                explanation = "Günlük soru sayıları: 25, 35, 45, 55, 65, 75. Toplam = 25 + 35 + 45 + 55 + 65 + 75 = 300 soru.",
                skillType = "Aritmetik Dizi ve Problem Çözme",
                hints = listOf("İpucu: Terimleri tek tek yazabilir veya ardışık terimler toplam formülü (İlk + Son) / 2 · Terim Sayısı uygulayabilirsin.")
            )
        )
        allQuestions.add(
            QuizQuestion(
                subject = "Türkçe",
                topic = "Paragrafta Çıkarım ve Anlam",
                questionText = "Maarif Modeli'nin süreç odaklı eğitim yaklaşımında öğrencinin yalnızca sonuca ulaşması değil, düşünce basamaklarını nasıl inşa ettiği değerlendirilir. Bu parçadan çıkarılabilecek en kapsamlı yargı aşağıdakilerden hangisidir?",
                options = listOf(
                    "A) Sonucun doğruluğu değerlendirmede önemsizdir.",
                    "B) Akıl yürütme ve problem çözme yöntemi, ezberlenmiş cevaptan daha değerlidir.",
                    "C) Yeni modelde yalnızca teorik dersler işlenir.",
                    "D) Öğrencilerin sınav stresi tamamen ortadan kalkmıştır.",
                    "E) Tüm derslerde sadece grup çalışmaları yapılmalıdır."
                ),
                correctOptionIndex = 1,
                explanation = "'Düşünce basamaklarını nasıl inşa ettiği' ifadesi, yöntemin ve akıl yürütme sürecinin cevaptan daha kıymetli olduğunu vurgular.",
                skillType = "Eleştirel Okuma ve Çıkarım",
                hints = listOf("İpucu: 'Süreç odaklı' kavramının neyi öncelediğine dikkat et.")
            )
        )

        return allQuestions.take(count.coerceAtLeast(1))
    }

    private fun generateFallbackQuiz(subject: String, topic: String): List<QuizQuestion> {
        val cleanSubject = subject.lowercase()
        return when {
            cleanSubject.contains("fiz") || cleanSubject.contains("kuvvet") || cleanSubject.contains("hareket") -> listOf(
                QuizQuestion(
                    subject = "Fizik",
                    topic = if (topic.isNotBlank()) topic else "Doğrusal Hareket & Hız-Zaman Analizi",
                    questionText = "Durgun halden (v₀ = 0) harekete başlayan bir elektrikli test aracı ilk 4 saniye boyunca a = 3 m/s² sabit ivmeyle hızlanmış, sonraki 6 saniye boyunca sabit hızla yoluna devam etmiştir.\n\nBuna göre aracın 10 saniye sonundaki toplam yer değiştirmesi (Δx) kaç metredir?",
                    options = listOf(
                        "A) 72 metre",
                        "B) 84 metre",
                        "C) 96 metre",
                        "D) 108 metre",
                        "E) 120 metre"
                    ),
                    correctOptionIndex = 2,
                    explanation = "1. Adım: İlk 4 saniyedeki hız v(4) = a · t = 3 · 4 = 12 m/s.\n2. Adım: İlk 4 saniyedeki yer değiştirme v-t üçgen alanından x₁ = (12 · 4) / 2 = 24 metredir.\n3. Adım: 4-10 saniye arası (6 saniye) sabit 12 m/s hızla gitmiştir. Dikdörtgen alanı x₂ = 12 · 6 = 72 metredir.\n4. Adım: Toplam yer değiştirme Δx = x₁ + x₂ = 24 + 72 = 96 metre bulunur. Doğru seçenek C'dir.",
                    skillType = "Veri Yorumlama ve Grafik Alanı Muhakemesi",
                    hints = listOf(
                        "1. İpucu (Kavram & Formül): Hız-zaman (v-t) grafiğinde doğrunun eğimi ivmeyi (a = Δv/Δt), grafiğin altındaki alan ise yer değiştirmeyi (Δx) verir.",
                        "2. İpucu (Çözüm Stratejisi): 4. saniyedeki hızı bulup v-t grafiğini ikiye böl: İlk bölüm üçgen (hızlanan), ikinci bölüm dikdörtgen (sabit hızlı). Bu iki alanın toplamı cevaptır."
                    ),
                    hint = "Hız-zaman grafiğinin altındaki alan yer değiştirmeyi verir.",
                    graphInfo = QuestionGraphInfo(
                        title = "Hız-Zaman (v-t) Analizi",
                        xLabel = "t",
                        yLabel = "v",
                        unitX = "s",
                        unitY = "m/s",
                        type = "PHYSICS_VT",
                        a = 1.2f,
                        b = 0f,
                        c = 0f,
                        highlightAreaLabel = "Toplam Alan Δx = 96 m",
                        extraNotes = "Maarif Modeli: Grafikteki eğim ivmeyi, altındaki alan yer değiştirmeyi verir.",
                        keyPoints = listOf(
                            GraphPoint(0f, 0f, "Başlangıç (0,0)"),
                            GraphPoint(4f, 12f, "Hızlanma Sonu (4s, 12 m/s)"),
                            GraphPoint(10f, 12f, "Varış (10s, 12 m/s)")
                        ),
                        minX = 0f,
                        maxX = 12f,
                        minY = 0f,
                        maxY = 16f
                    )
                )
            )
            cleanSubject.contains("kim") || cleanSubject.contains("entalpi") || cleanSubject.contains("asit") -> listOf(
                QuizQuestion(
                    subject = "Kimya",
                    topic = if (topic.isNotBlank()) topic else "Tepkime Hızları & Potansiyel Enerji",
                    questionText = "Bir kimyasal tepkimenin potansiyel enerji - tepkime koordinatı grafiğinde girenlerin potansiyel enerjisi 25 kJ, aktifleşmiş kompleksin enerjisi 95 kJ ve ürünlerin enerjisi 10 kJ olarak ölçülmüştür.\n\nBuna göre tepkimenin geri aktifleşme enerjisi (Eag) ve tepkime entalpisi (ΔH) aşağıdakilerden hangisinde doğru verilmiştir?",
                    options = listOf(
                        "A) Eag = 70 kJ, ΔH = +15 kJ",
                        "B) Eag = 85 kJ, ΔH = -15 kJ",
                        "C) Eag = 95 kJ, ΔH = -25 kJ",
                        "D) Eag = 70 kJ, ΔH = -15 kJ",
                        "E) Eag = 85 kJ, ΔH = +15 kJ"
                    ),
                    correctOptionIndex = 1,
                    explanation = "1. Adım: İleri aktifleşme enerjisi Eai = E(aktifleşmiş kompleks) - E(girenler) = 95 - 25 = 70 kJ.\n2. Adım: Geri aktifleşme enerjisi Eag = E(aktifleşmiş kompleks) - E(ürünler) = 95 - 10 = 85 kJ.\n3. Adım: Tepkime entalpisi ΔH = E(ürünler) - E(girenler) = 10 - 25 = -15 kJ (veya ΔH = Eai - Eag = 70 - 85 = -15 kJ). Tepkime ekzotermiktir. Doğru seçenek B'dir.",
                    skillType = "Kimyasal Enerji ve Tepkime Koordinatı Modellemesi",
                    hints = listOf(
                        "1. İpucu (Kavram & Formül): İleri aktifleşme enerjisi Eai = Tepe - Girenler; Geri aktifleşme enerjisi Eag = Tepe - Ürünler bağıntısını kullan.",
                        "2. İpucu (Çözüm Stratejisi): Tepkime entalpisi ΔH = Ürünler - Girenler = Eai - Eag dir. Ürünlerin enerjisi daha alçakta olduğu için ΔH negatif (ekzotermik) çıkmalıdır."
                    ),
                    hint = "Eai = Tepe - Girenler, Eag = Tepe - Ürünler, ΔH = Ürünler - Girenler.",
                    graphInfo = QuestionGraphInfo(
                        title = "Potansiyel Enerji - Tepkime Koordinatı",
                        xLabel = "Tepkime İlerlemesi",
                        yLabel = "PE",
                        unitX = "",
                        unitY = "kJ",
                        type = "CHEM_ENERGY_PROFILE",
                        a = 70f,
                        b = 0f,
                        c = 25f,
                        highlightAreaLabel = "ΔH = -15 kJ (Ekzotermik)",
                        extraNotes = "Katalizör tepe noktasını indirir; giren, ürün ve ΔH değerini asla değiştirmez.",
                        keyPoints = listOf(
                            GraphPoint(-2f, 25f, "Girenler (25 kJ)"),
                            GraphPoint(0f, 95f, "Tepe / Ea (95 kJ)"),
                            GraphPoint(2f, 10f, "Ürünler (10 kJ)")
                        ),
                        minX = -4f,
                        maxX = 4f,
                        minY = 0f,
                        maxY = 110f
                    )
                )
            )
            cleanSubject.contains("mat") -> listOf(
                QuizQuestion(
                    subject = "Temel Matematik",
                    topic = if (topic.isNotBlank()) topic else "Parabol ve Fonksiyon Modelleme",
                    questionText = "Bir su fıskiyesinden fışkıran su damlalarının izlediği yörünge f(x) = -x² + 4x + 5 fonksiyonu ile modellenmiştir (x: yatay mesafe metre, y: suyun yerden yüksekliği metre).\n\nBuna göre suyun ulaşabileceği MAKSİMUM yükseklik kaç metredir ve su kaç metre ileride yere düşer?",
                    options = listOf(
                        "A) Maksimum 7 m, 4 m ileride",
                        "B) Maksimum 9 m, 5 m ileride",
                        "C) Maksimum 8 m, 6 m ileride",
                        "D) Maksimum 10 m, 5 m ileride",
                        "E) Maksimum 9 m, 6 m ileride"
                    ),
                    correctOptionIndex = 1,
                    explanation = "1. Adım (Tepe Noktası): Parabolün tepe noktasının apsisi r = -b / (2a) = -4 / (2 * -1) = 2'dir. Maksimum yükseklik k = f(2) = -(2)² + 4(2) + 5 = -4 + 8 + 5 = 9 metredir.\n2. Adım (Yere Düşme / Kök): Suyun yere düşmesi için y = 0 olmalıdır. -x² + 4x + 5 = 0 => x² - 4x - 5 = 0 => (x - 5)(x + 1) = 0. Mesafe pozitif olduğundan x = 5 metre ileride yere düşer. Doğru cevap B seçeneğidir.",
                    skillType = "2. Dereceden Fonksiyon ve Parabol Modelleme",
                    hints = listOf(
                        "1. İpucu (Kavram & Formül): Maksimum yükseklik parabolün tepe noktasında (k) gerçekleşir. Simetri ekseni r = -b / (2a) formülüyle bulunur.",
                        "2. İpucu (Çözüm Stratejisi): Bulduğun r değerini fonksiyonda yerine yazarak k değerini bul (k = f(r)). Yere düşme mesafesi için y = f(x) = 0 denkleminin pozitif kökünü hesapla."
                    ),
                    hint = "Tepe noktası r = -b/(2a), k = f(r); yere düşme anı f(x) = 0 köküdür.",
                    graphInfo = QuestionGraphInfo(
                        title = "Su Yörüngesi: f(x) = -x² + 4x + 5",
                        xLabel = "x",
                        yLabel = "y",
                        unitX = "m",
                        unitY = "m",
                        type = "PARABOLA",
                        a = -1f,
                        b = 4f,
                        c = 5f,
                        highlightAreaLabel = "Maksimum Yükseklik: 9 m",
                        keyPoints = listOf(
                            GraphPoint(2f, 9f, "Maksimum T(2, 9)"),
                            GraphPoint(5f, 0f, "Yere Çarpma (5, 0)"),
                            GraphPoint(0f, 5f, "Çıkış Noktası (0, 5)")
                        ),
                        minX = -1f,
                        maxX = 7f,
                        minY = -1f,
                        maxY = 11f
                    )
                ),
                QuizQuestion(
                    subject = "Temel Matematik",
                    topic = "Doğrusal Fonksiyon & Eğim",
                    questionText = "Sabit hızla boşalan bir akıllı su deposunda kalan su miktarı (litre) ile geçen süre (dakika) arasındaki ilişki grafikteki f(t) = -2t + 60 doğrusu ile verilmiştir.\n\nDepodaki suyun yarısının boşalması için kaç dakika geçmesi gerekir?",
                    options = listOf(
                        "A) 10 dakika",
                        "B) 12 dakika",
                        "C) 15 dakika",
                        "D) 20 dakika",
                        "E) 30 dakika"
                    ),
                    correctOptionIndex = 2,
                    explanation = "Başlangıçta (t = 0) depodaki su f(0) = 60 litredir. Suyun yarısı 30 litredir. f(t) = 30 => -2t + 60 = 30 => 2t = 30 => t = 15 dakika bulunur. Doğru seçenek C'dir.",
                    skillType = "Doğrusal Fonksiyon, Eğim ve Hız Yorumlama",
                    hints = listOf(
                        "1. İpucu (Kavram): Başlangıçtaki depo hacmini t = 0 vererek bul.",
                        "2. İpucu (Strateji): Deponun yarısı kaldığında f(t) = 30 olur. Bu eşitliği sağlayan t değerini çek."
                    ),
                    hint = "f(0) başlangıç hacmidir, f(t) = 30 yaparak t'yi çek.",
                    graphInfo = QuestionGraphInfo(
                        title = "Depo Boşalma Grafiği: f(t) = -2t + 60",
                        xLabel = "t",
                        yLabel = "V",
                        unitX = "dk",
                        unitY = "L",
                        type = "LINEAR",
                        a = -2f,
                        b = 60f,
                        c = 0f,
                        keyPoints = listOf(
                            GraphPoint(0f, 60f, "Başlangıç (0, 60)"),
                            GraphPoint(15f, 30f, "Yarı Dolu (15, 30)"),
                            GraphPoint(30f, 0f, "Tam Boş (30, 0)")
                        ),
                        minX = 0f,
                        maxX = 35f,
                        minY = 0f,
                        maxY = 70f
                    )
                )
            )
            cleanSubject.contains("türk") -> listOf(
                QuizQuestion(
                    subject = "Türkçe",
                    topic = if (topic.isNotBlank()) topic else "Eleştirel Okuma ve Çıkarım",
                    questionText = "\"Yapay zekanın ürettiği sanat yapıtlarında teknik kusursuzluk göz kamaştırsa da insanın iç dünyasındaki kırılganlığı ve çelişkileri aynı derinlikle aktaramadığı görülür. Çünkü sanat, mükemmelliğe ulaşma çabasından ziyade insanın kendi eksiklikleriyle barışma serüvenidir.\"\n\nBu parçadan hareketle aşağıdaki yargılardan hangisine ulaşılabilir?",
                    options = listOf(
                        "A) Yapay zeka sanatı asla teknik açıdan insan kadar gelişemez.",
                        "B) Gerçek sanat eserinin değeri, kusursuzluğunda değil insani kusurların dışa vurumunda yatar.",
                        "C) Kusursuz teknik sergileyen eserler halk tarafından her zaman daha çok ilgi görür.",
                        "D) Sanatçının amacı mükemmel formu yakalamak olmalıdır.",
                        "E) İnsan ruhunun derinlikleri sadece edebi metinler yoluyla keşfedilebilir."
                    ),
                    correctOptionIndex = 1,
                    explanation = "Metinde sanatın 'mükemmelliğe ulaşma çabasından ziyade insanın kendi eksiklikleriyle barışma serüveni' olduğu açıkça belirtilmiştir. Bu da B seçeneğindeki 'sanatın değerinin insani kusurların dışa vurumunda yattığı' yargısını doğrular. Doğru cevap B'dir.",
                    skillType = "Metin İçi Çıkarım ve Anlamsal Analiz",
                    hints = listOf(
                        "1. İpucu: Metindeki 'Çünkü sanat...' ile başlayan gerekçe cümlesine odaklan.",
                        "2. İpucu: 'kusur', 'eksiklik' ve 'mükemmellik' arasındaki karşıtlığı seçeneklerde ara."
                    ),
                    hint = "Sanatın insanın eksiklikleriyle barışma serüveni olduğuna dikkat et."
                )
            )
            else -> listOf(
                QuizQuestion(
                    subject = subject.ifBlank { "Fen Bilimleri" },
                    topic = if (topic.isNotBlank()) topic else "Bilimsel Deney ve Hipotez",
                    questionText = "Bir öğrenci özdeş iki saksı bitkisinden birini sadece kırmızı ışık alan odaya, diğerini ise sadece yeşil ışık alan odaya koyuyor. Bir hafta sonunda kırmızı ışık alan bitkinin kütlesinde %18 artış olurken yeşil ışık alanda sadece %2 artış gözlemleniyor.\n\nBu deneyden yola çıkarak Maarif modeli bilimsel araştırma basamaklarına göre hangi çıkarım yapılır?",
                    options = listOf(
                        "A) Klorofil pigmenti yeşil ışığı soğurmaz, büyük oranda yansıtır; bu nedenle fotosentez hızı yeşilde düşüktür.",
                        "B) Bitkiler karanlıkta daha hızlı fotosentez yapar.",
                        "C) Kırmızı ışık bitkilerin solunumunu tamamen durdurmuştur.",
                        "D) Deneyde sıcaklık faktörü sabit tutulmamıştır.",
                        "E) Yeşil ışık bitkiler için zehirli bir etki yaratmıştır."
                    ),
                    correctOptionIndex = 0,
                    explanation = "Fotosentez deneyinde klorofil yeşil ışığı yansıttığı için fotosentez hızı en düşüktür; kırmızı ve mavi ışıkta ise maksimum soğurulma gerçekleştiğinden kütle artışı (besin üretimi) daha fazladır. Doğru cevap A seçeneğidir.",
                    skillType = "Hipotez Testi ve Deney Verisi Yorumlama",
                    hints = listOf(
                        "1. İpucu: Klorofilin yeşil renkli görünmesinin sebebi yeşil ışığı emmesi mi yoksa yansıtması mıdır?",
                        "2. İpucu: Fotosentez hızının ışığın dalga boyuna ve soğurulma (absorpsiyon) oranına bağlı olduğunu hatırla."
                    ),
                    hint = "Klorofilin ışığı yansıtma ve soğurma özelliklerini hatırla."
                )
            )
        }
    }

    private fun generateHeuristicMathSolution(problem: String): MathSolution {
        val lower = problem.lowercase()

        // 1. Check if Physics Problem
        if (lower.contains("fizik") || lower.contains("hız") || lower.contains("ivme") || 
            lower.contains("v-t") || lower.contains("x-t") || lower.contains("yer değiştirme") || 
            lower.contains("hareket") || lower.contains("kuvvet") || lower.contains("m/s")) {
            
            val v0 = 4f
            val a = 2f
            val t = 5f
            val vFinal = v0 + (a * t) // 14 m/s
            val deltaX = (v0 * t) + (0.5f * a * t * t) // 45 m

            val steps = listOf(
                "1. Adım: Fiziksel büyüklükler ve kinematik bağıntılar listelenir: İlk hız v₀ = $v0 m/s, ivme a = $a m/s², süre t = $t s.",
                "2. Adım: Hız denklemi uygulanır: v(t) = v₀ + a·t => v(5) = $v0 + ($a · $t) = $vFinal m/s.",
                "3. Adım: Yer değiştirme denklemi uygulanır: Δx = v₀·t + ½·a·t² => Δx = ($v0 · $t) + (0.5 · $a · ${t*t}) = $deltaX metre.",
                "4. Adım: Hız-Zaman (v-t) grafiği çizildiğinde şekil alt tabanı v₀, üst tabanı vFinal ve yüksekliği t olan bir dik yamuktur. Alan = (v₀ + vFinal)/2 · t = ($v0 + $vFinal)/2 · $t = $deltaX m (Doğrulanır)."
            )

            val keyPoints = listOf(
                GraphPoint(0f, v0, "v₀ = $v0 m/s"),
                GraphPoint(t, vFinal, "v($t) = $vFinal m/s"),
                GraphPoint(t, 0f, "t = $t s")
            )

            return MathSolution(
                originalProblem = problem,
                steps = steps,
                finalAnswer = "Son Hız: v = %.0f m/s | Toplam Yer Değiştirme: Δx = %.0f m".format(vFinal, deltaX),
                subject = "Fizik",
                physicsFormulas = listOf("v = v₀ + a·t", "Δx = v₀·t + ½·a·t²", "Alan(v-t) = Yer Değiştirme (Δx)"),
                graphInfo = QuestionGraphInfo(
                    title = "Hız-Zaman (v-t) Hareketi: v(t) = %.1ft + %.1f".format(a, v0),
                    xLabel = "t",
                    yLabel = "v",
                    unitX = "s",
                    unitY = "m/s",
                    type = "PHYSICS_VT",
                    a = a,
                    b = v0,
                    c = 0f,
                    keyPoints = keyPoints,
                    minX = 0f,
                    maxX = 8f,
                    minY = 0f,
                    maxY = 20f,
                    highlightAreaLabel = "Yer Değiştirme Alanı Δx = %.0f m".format(deltaX),
                    extraNotes = "2028 Maarif Modeli: Fiziksel grafiklerin eğimi türevi (ivmeyi), altındaki alan ise integrali (yer değiştirmeyi) modeller."
                ),
                maarifKeyTakeaway = "2028 Maarif Modeli Fizik kazanımlarında kinematik formülleri ezberlemek yerine v-t grafiğindeki yamuk alanından yer değiştirme bağıntısını türetme becerisi esastır."
            )
        }

        // 2. Check if Chemistry Problem
        if (lower.contains("kimya") || lower.contains("entalpi") || lower.contains("tepkime") || 
            lower.contains("aktifleşme") || lower.contains("potansiyel") || lower.contains("ph") || 
            lower.contains("titrasyon") || lower.contains("asit") || lower.contains("baz") || 
            lower.contains("h2") || lower.contains("o2") || lower.contains("delta h")) {
            
            val eGirenler = 30f
            val eAktif = 90f
            val eUrunler = 15f
            val eai = eAktif - eGirenler // 60
            val eag = eAktif - eUrunler // 75
            val deltaH = eUrunler - eGirenler // -15 (ekzotermik)

            val steps = listOf(
                "1. Adım: Kimyasal tepkime denklemi yazılır ve denkleştirilir: 2H₂(g) + O₂(g) → 2H₂O(s)  ΔH < 0.",
                "2. Adım: İleri aktifleşme enerjisi (Eai): Eai = E(aktifleşmiş kompleks) - E(girenler) = $eAktif - $eGirenler = $eai kJ/mol.",
                "3. Adım: Geri aktifleşme enerjisi (Eag): Eag = E(aktifleşmiş kompleks) - E(ürünler) = $eAktif - $eUrunler = $eag kJ/mol.",
                "4. Adım: Tepkime entalpisi bağıntısı: ΔH = Eai - Eag = $eai - $eag = $deltaH kJ/mol. ΔH < 0 olduğundan tepkime ekzotermiktir ve ısı açığa çıkar."
            )

            val keyPoints = listOf(
                GraphPoint(-2f, eGirenler, "Girenler ($eGirenler kJ)"),
                GraphPoint(0f, eAktif, "Aktifleşmiş Kompleks ($eAktif kJ)"),
                GraphPoint(2f, eUrunler, "Ürünler ($eUrunler kJ)")
            )

            return MathSolution(
                originalProblem = problem,
                steps = steps,
                finalAnswer = "Eai = %.0f kJ/mol | Eag = %.0f kJ/mol | ΔH = %.0f kJ/mol (Ekzotermik)".format(eai, eag, deltaH),
                subject = "Kimya",
                chemicalEquation = "2H₂(g) + O₂(g) → 2H₂O(s)   ΔH° = -572 kJ",
                graphInfo = QuestionGraphInfo(
                    title = "Potansiyel Enerji - Tepkime İlerlemesi Diyagramı",
                    xLabel = "Tepkime Koordinatı",
                    yLabel = "PE",
                    unitX = "",
                    unitY = "kJ/mol",
                    type = "CHEM_ENERGY_PROFILE",
                    a = eai,
                    b = 0f,
                    c = eGirenler,
                    keyPoints = keyPoints,
                    minX = -4f,
                    maxX = 4f,
                    minY = 0f,
                    maxY = 110f,
                    highlightAreaLabel = "ΔH = %.0f kJ/mol (Isı Veren)".format(deltaH),
                    extraNotes = "Katalizör eklenirse tepe noktası (aktifleşmiş kompleks) alçalır, tepkime hızlanır ancak ΔH değişmez."
                ),
                maarifKeyTakeaway = "2028 Maarif Modeli Kimya dersinde tepkime entalpisi saf sayı olarak değil, bağ kırılması (endotermik) ve yeni bağ oluşumu (ekzotermik) potansiyel enerji profili üzerinden görselleştirilir."
            )
        }

        // 3. Default: Mathematics (Parabola / Linear)
        val a: Float
        val b: Float
        val c: Float
        val functionTitle: String
        val functionType: String

        if (lower.contains("doğru") || (!lower.contains("x²") && !lower.contains("x^2") && lower.contains("x"))) {
            a = 2f
            b = -4f
            c = 0f
            functionTitle = "f(x) = 2x - 4 (Doğrusal)"
            functionType = "LINEAR"
        } else {
            // Parabola
            a = 1f
            b = -4f
            c = 3f
            functionTitle = "f(x) = x² - 4x + 3"
            functionType = "PARABOLA"
        }

        val r = -b / (2f * a)
        val k = (a * r * r) + (b * r) + c

        val steps = listOf(
            "1. Adım: Verilen ifade standart fonksiyona dönüştürülür: $functionTitle",
            "2. Adım: Eksenleri kestiği noktalar belirlenir. x = 0 için y = f(0) = $c",
            "3. Adım: y = 0 için kökler aranır. Δ = b² - 4ac = (${b.toInt()})² - 4(${a.toInt()})(${c.toInt()}) = 4 > 0. Kökler x₁ = 1, x₂ = 3",
            "4. Adım: Simetri ekseni ve tepe noktası: r = -b/(2a) = ${r.toInt()}, k = f(r) = ${k.toInt()} -> T(${r.toInt()}, ${k.toInt()})"
        )

        val keyPoints = listOf(
            GraphPoint(r, k, "Tepe T(%.1f, %.1f)".format(r, k)),
            GraphPoint(1f, 0f, "Kök (1, 0)"),
            GraphPoint(3f, 0f, "Kök (3, 0)"),
            GraphPoint(0f, c, "y-kesim (0, %.1f)".format(c))
        )

        return MathSolution(
            originalProblem = problem,
            steps = steps,
            finalAnswer = "Kökler: x₁ = 1, x₂ = 3 | Tepe Noktası T(%.1f, %.1f)".format(r, k),
            subject = "Matematik",
            graphInfo = QuestionGraphInfo(
                title = functionTitle,
                xLabel = "x",
                yLabel = "y",
                type = functionType,
                a = a,
                b = b,
                c = c,
                keyPoints = keyPoints,
                minX = -3f,
                maxX = 6f,
                minY = -3f,
                maxY = 7f
            ),
            maarifKeyTakeaway = "2028 Maarif Modeli'nde paraboller ezber formülle değil, gerçek hayat yörüngeleri ve simetri ekseniyle analiz edilir."
        )
    }
}
