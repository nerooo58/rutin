package com.nororoutine.batu.model

import java.util.UUID

enum class YksExamType(val displayName: String) {
    TYT("TYT (Temel Yeterlilik)"),
    AYT_SAYISAL("AYT Sayısal"),
    AYT_ESIT_AGIRLIK("AYT Eşit Ağırlık"),
    AYT_SOZEL("AYT Sözel")
}

data class ExamSubjectResult(
    val subjectName: String,
    val correct: Int,
    val incorrect: Int,
    val empty: Int,
    val net: Float = (correct.toFloat() - (incorrect.toFloat() * 0.25f)).coerceAtLeast(0f),
    val weakTopics: List<String> = emptyList()
)

data class DeficiencyItem(
    val subject: String,
    val topic: String,
    val severity: String = "Yüksek", // Yüksek, Orta, Hafif
    val actionPlan: String
)

data class StudyPrescriptionItem(
    val phase: String, // örn: "Hafta 1: Odak", "Hafta 2: Pekiştirme"
    val targetTask: String,
    val recommendedQuestionCount: Int
)

data class YksAiAnalysis(
    val summary: String,
    val estimatedStanding: String, // 2028 Maarif Modeli perspektifinde durum
    val strongPoints: List<String>,
    val criticalDeficiencies: List<DeficiencyItem>,
    val weeklyPrescription: List<StudyPrescriptionItem>,
    val maarifAdvice: String,
    val isAiGenerated: Boolean = true
)

data class YksExamRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: String,
    val examType: YksExamType,
    val results: List<ExamSubjectResult>,
    val totalNet: Float = results.sumOf { it.net.toDouble() }.toFloat(),
    val targetNet: Float = 90f,
    val notes: String = "",
    val aiAnalysis: YksAiAnalysis? = null
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val suggestedChips: List<String> = emptyList()
)

data class GraphPoint(
    val x: Float,
    val y: Float,
    val label: String = ""
)

data class QuestionGraphInfo(
    val title: String,
    val xLabel: String = "x",
    val yLabel: String = "y",
    val unitX: String = "",
    val unitY: String = "",
    val type: String = "PARABOLA", // Math: PARABOLA, LINEAR, CUBIC, TRIGONOMETRIC | Physics: PHYSICS_VT, PHYSICS_XT, PHYSICS_AT, PHYSICS_WAVE, PHYSICS_PV, PHYSICS_FX | Chem: CHEM_ENERGY_PROFILE, CHEM_TITRATION, CHEM_SOLUBILITY, CHEM_HEATING_CURVE, CHEM_MAXWELL, CHEM_EQUILIBRIUM
    val a: Float = 1f,
    val b: Float = 0f,
    val c: Float = 0f,
    val keyPoints: List<GraphPoint> = emptyList(),
    val minX: Float = -6f,
    val maxX: Float = 6f,
    val minY: Float = -6f,
    val maxY: Float = 8f,
    val highlightAreaLabel: String = "",
    val extraNotes: String = ""
)

data class QuizQuestion(
    val id: String = UUID.randomUUID().toString(),
    val subject: String,
    val topic: String,
    val questionText: String,
    val options: List<String>, // 5 options: A, B, C, D, E
    val correctOptionIndex: Int, // 0..4
    val explanation: String,
    val skillType: String = "Beceri Temelli Muhakeme", // Maarif modeli beceri alanı
    val selectedOptionIndex: Int? = null,
    val graphInfo: QuestionGraphInfo? = null,
    val hint: String? = null,
    val hints: List<String> = emptyList(), // Progressive hints: [Hint 1: Kavram/Formül, Hint 2: Çözüm Stratejisi/Tuzak]
    val revealedHintLevel: Int = 0 // 0: gizli, 1: 1. ipucu açık, 2: 2. ipucu açık
)

data class MathSolution(
    val originalProblem: String,
    val steps: List<String>,
    val finalAnswer: String,
    val graphInfo: QuestionGraphInfo? = null,
    val maarifKeyTakeaway: String,
    val subject: String = "Matematik", // Matematik, Fizik, Kimya
    val chemicalEquation: String? = null,
    val physicsFormulas: List<String> = emptyList()
)

