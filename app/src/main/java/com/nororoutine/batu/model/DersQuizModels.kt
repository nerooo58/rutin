package com.nororoutine.batu.model

import java.util.UUID

enum class DersQuestionType(val titleTr: String, val shortBadge: String) {
    MULTIPLE_CHOICE("Çoktan Seçmeli (A, B, C, D)", "Çoktan Seçmeli"),
    TRUE_FALSE("Doğru / Yanlış", "Doğru / Yanlış"),
    FILL_IN_THE_BLANK("Boşluk Doldurma", "Boşluk Doldurma"),
    SHORT_ANSWER("Kısa Cevaplı", "Kısa Cevap"),
    MATCHING("Eşleştirme", "Eşleştirme")
}

data class MatchingPair(
    val left: String,
    val right: String
)

data class DersQuestion(
    val id: String = UUID.randomUUID().toString(),
    val type: DersQuestionType,
    val topic: String,
    val questionText: String,
    // Çoktan Seçmeli (Tam 4 şık: A, B, C, D)
    val options: List<String> = emptyList(),
    val correctOptionIndex: Int = 0, // 0..3
    // Doğru / Yanlış
    val isTrue: Boolean = true,
    // Boşluk Doldurma ve Kısa Cevap
    val correctAnswerText: String = "",
    val acceptableAnswers: List<String> = emptyList(),
    // Eşleştirme
    val matchingPairs: List<MatchingPair> = emptyList(),
    // Eğitim ve Rehberlik
    val explanation: String, // Cevaplandıktan sonraki kısa açıklama
    val hint: String, // İpucu (cevabı vermeden)
    val sourceSnippet: String = "" // Belgeden dayanak noktası
)

data class DocumentAnalysisSummary(
    val documentTitle: String,
    val detectedTopics: List<String>,
    val documentOverview: String,
    val proposedQuestionCount: Int,
    val typeDistribution: Map<DersQuestionType, Int> = emptyMap(),
    val rawTextContent: String = "",
    val imageBase64: String? = null,
    val imageUri: String? = null
)

data class DersStudySessionRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDate: String,
    val documentSummary: String,
    val questions: List<DersQuestion>,
    val userAnswers: Map<String, String>, // questionId -> answer representation
    val isCorrectMap: Map<String, Boolean>, // questionId -> isCorrect
    val totalQuestions: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val missingTopics: List<String>, // Eksik konular
    val recommendedStudyTopics: List<String>, // Tekrar çalışılması önerilen konular
    val coachFeedback: String
)

enum class DersFlowStep {
    UPLOAD,              // 1. Belge/Not/Örnek soru yükleme
    ANALYZING,           // Yapay zeka analiz animasyonu
    CONFIRMATION,        // 2. Analiz özeti & soru adedi teyidi
    QUESTION_ACTIVE,     // 3 & 4. Soruların tek tek çözümü & anında açıklama
    SUMMARY              // 5. Özet ekranı & analiz & aynı konudan tekrar üretme
}
