package com.nororoutine.batu.network

import android.util.Log
import com.nororoutine.batu.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class GeminiStoryAnalysis(
    val topHighlights: List<String>,
    val motivationalQuote: String,
    val isAiGenerated: Boolean
)

object GeminiStoryHelper {
    private const val TAG = "GeminiStoryHelper"
    private const val MODEL = "gemini-3.5-flash"

    suspend fun analyzeStoryHighlights(
        routineTitle: String,
        periodTitle: String,
        totalFocusFormatted: String,
        rawNotesAndSessions: List<String>,
        defaultTop3: List<String>
    ): GeminiStoryAnalysis = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext fallbackAnalysis(rawNotesAndSessions, defaultTop3, totalFocusFormatted)
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            val notesContent = if (rawNotesAndSessions.isNotEmpty()) {
                rawNotesAndSessions.joinToString("\n") { "• $it" }
            } else {
                "Detaylı seans açıklaması belirtilmedi."
            }

            val prompt = """
                Sen kişisel bir rutin ve başarı koçusun. Kullanıcının tuttuğu süreler ve bu sürelerde yaptıklarına dair yazdığı notlar şunlardır:
                Dönem: $periodTitle
                Rutin: $routineTitle
                Toplam Odak Süresi: $totalFocusFormatted
                Kullanıcının Yazdığı Açıklamalar ve Seans Detayları:
                $notesContent

                GÖREVİN:
                1. Kullanıcının yazdığı açıklamalara ve tuttuğu sürelere dayanarak en çok yaptığı ve başardığı EN ÇOK 3 şeyi (Top 3) çıkar. Her madde kısa, net ve etkileyici olsun (Örn: '📐 Matematik: 50 soru ve fonksiyonlar', '📖 Kitap: 45 sayfa felsefe okuması', vb.).
                2. Instagram / WhatsApp Hikayesi için kullanıcının yazdığı detaylara özel, 1 cümlelik vurucu ve havalı bir motivasyon / başarı sözü yaz.

                SADECE VE SADECE aşağıdaki geçerli JSON formatında yanıt ver, başka hiçbir metin ekleme:
                {
                  "highlights": ["1. madde", "2. madde", "3. madde"],
                  "quote": "Motivasyon sözü"
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
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text", "")
                        val cleaned = text.trim()
                            .removePrefix("```json")
                            .removePrefix("```")
                            .removeSuffix("```")
                            .trim()

                        val parsedResult = JSONObject(cleaned)
                        val highlightsJson = parsedResult.optJSONArray("highlights")
                        val highlightsList = mutableListOf<String>()
                        if (highlightsJson != null) {
                            for (i in 0 until highlightsJson.length()) {
                                val item = highlightsJson.getString(i).trim()
                                if (item.isNotBlank()) highlightsList.add(item)
                            }
                        }
                        val quote = parsedResult.optString("quote", "").trim()

                        if (highlightsList.isNotEmpty()) {
                            return@withContext GeminiStoryAnalysis(
                                topHighlights = highlightsList.take(3),
                                motivationalQuote = if (quote.isNotBlank()) quote else "Hedeflerine bir adım daha yaklaştın! 🚀",
                                isAiGenerated = true
                            )
                        }
                    }
                }
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                Log.w(TAG, "Gemini API error code: $responseCode, $errorStream")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call Gemini API, using fallback", e)
        }

        fallbackAnalysis(rawNotesAndSessions, defaultTop3, totalFocusFormatted)
    }

    private fun fallbackAnalysis(
        rawNotesAndSessions: List<String>,
        defaultTop3: List<String>,
        totalFocusFormatted: String
    ): GeminiStoryAnalysis {
        val extracted = mutableListOf<String>()
        for (item in rawNotesAndSessions) {
            // Check if format has note part "time|seconds|note"
            val parts = item.split("|")
            val note = if (parts.size >= 3) parts[2].trim() else item.trim()
            if (note.isNotBlank() && note != "Odak Seansı" && !extracted.contains(note)) {
                extracted.add("• $note")
            }
        }

        val highlights = if (extracted.isNotEmpty()) {
            extracted.take(3)
        } else if (defaultTop3.isNotEmpty()) {
            defaultTop3.take(3)
        } else {
            listOf(
                "• Toplam Odak: $totalFocusFormatted",
                "• Planlanan hedefler eksiksiz tamamlandı",
                "• Rutin disiplini sürdürüldü"
            )
        }

        val quote = "Disiplin, hedefler ile başarı arasındaki köprüdür. 🚀"
        return GeminiStoryAnalysis(
            topHighlights = highlights,
            motivationalQuote = quote,
            isAiGenerated = false
        )
    }
}
