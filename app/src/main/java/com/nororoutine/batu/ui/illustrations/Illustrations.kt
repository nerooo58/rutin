package com.nororoutine.batu.ui.illustrations

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class IllustrationPreset(
    val id: String,
    val category: String,
    val title: String,
    val primaryColor: Color,
    val secondaryColor: Color
)

val ILLUSTRATION_PRESETS: List<IllustrationPreset> = listOf(
    // Spor
    IllustrationPreset("spor_running", "Spor", "Koşu", Color(0xFFF97316), Color(0xFFFBBF24)),
    IllustrationPreset("spor_weights", "Spor", "Ağırlık", Color(0xFFEF4444), Color(0xFFF87171)),
    IllustrationPreset("spor_cycling", "Spor", "Bisiklet", Color(0xFF10B981), Color(0xFF34D399)),

    // Doğa
    IllustrationPreset("doga_forest", "Doğa", "Orman & Ağaçlar", Color(0xFF059669), Color(0xFF10B981)),
    IllustrationPreset("doga_sunrise", "Doğa", "Güneş & Manzara", Color(0xFFF59E0B), Color(0xFFFB923C)),
    IllustrationPreset("doga_park", "Doğa", "Gece Doğa & Ay", Color(0xFF14B8A6), Color(0xFF2DD4BF)),

    // Meditasyon
    IllustrationPreset("meditasyon_calm", "Meditasyon", "Bağdaş & Huzur", Color(0xFF8B5CF6), Color(0xFFA78BFA)),
    IllustrationPreset("meditasyon_breathing", "Meditasyon", "Nefes & Aura", Color(0xFF06B6D4), Color(0xFF7CF4DE)),
    IllustrationPreset("meditasyon_stillness", "Meditasyon", "Dinginlik", Color(0xFF6366F1), Color(0xFF818CF8)),

    // Ders / Çalışma
    IllustrationPreset("calisma_books", "Ders / Çalışma", "Kitaplar & Masa", Color(0xFF3B82F6), Color(0xFF60A5FA)),
    IllustrationPreset("calisma_desk", "Ders / Çalışma", "Masa & Lamba", Color(0xFF6366F1), Color(0xFFA855F7)),
    IllustrationPreset("calisma_focus", "Ders / Çalışma", "Odaklanma", Color(0xFFEC4899), Color(0xFFF472B6)),

    // Beslenme
    IllustrationPreset("beslenme_healthy_food", "Beslenme", "Renkli Meyve Kasesi", Color(0xFF22C55E), Color(0xFF86EFAC)),
    IllustrationPreset("beslenme_water", "Beslenme", "Su & Sağlık", Color(0xFF0EA5E9), Color(0xFF38BDF8)),
    IllustrationPreset("beslenme_meal", "Beslenme", "Dengeli Öğün", Color(0xFFEAB308), Color(0xFFFACC15)),

    // Müzik
    IllustrationPreset("muzik_headphones", "Müzik", "Kulaklık & Dinleme", Color(0xFFA855F7), Color(0xFFC084FC)),
    IllustrationPreset("muzik_instrument", "Müzik", "Notalar & Ritim", Color(0xFFF43F5E), Color(0xFFFB7185)),

    // Uyku / Dinlenme
    IllustrationPreset("uyku_rest", "Uyku / Dinlenme", "Ay & Yıldızlar Altında", Color(0xFF6366F1), Color(0xFF818CF8)),
    IllustrationPreset("uyku_moon", "Uyku / Dinlenme", "Gece & Hilal", Color(0xFF312E81), Color(0xFF818CF8)),
    IllustrationPreset("uyku_relax", "Uyku / Dinlenme", "Huzurlu Mola", Color(0xFFB45309), Color(0xFFD97706))
)

@Composable
fun RoutineIllustration(
    illustrationId: String,
    modifier: Modifier = Modifier,
    size: Dp = 110.dp
) {
    val preset = ILLUSTRATION_PRESETS.find { it.id == illustrationId }
        ?: ILLUSTRATION_PRESETS.first()

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        preset.primaryColor.copy(alpha = 0.25f),
                        preset.secondaryColor.copy(alpha = 0.10f),
                        Color(0xFF101C30)
                    )
                )
            )
            .border(
                1.dp,
                preset.secondaryColor.copy(alpha = 0.35f),
                RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // Soft glowing backdrop orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(preset.secondaryColor.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = w * 0.45f
                )
            )

            when (preset.id) {
                // Spor
                "spor_running" -> drawRunningPersonSvg(w, h, preset.primaryColor, preset.secondaryColor)
                "spor_weights" -> drawWeightliftingPersonSvg(w, h, preset.primaryColor, preset.secondaryColor)
                "spor_cycling" -> drawCyclingSvg(w, h, preset.primaryColor, preset.secondaryColor)

                // Doğa
                "doga_forest" -> drawLandscapeWithTreesSvg(w, h, preset.primaryColor, preset.secondaryColor)
                "doga_sunrise" -> drawSunriseLandscapeSvg(w, h, preset.primaryColor, preset.secondaryColor)
                "doga_park" -> drawMoonLandscapeSvg(w, h, preset.primaryColor, preset.secondaryColor)

                // Meditasyon
                "meditasyon_calm" -> drawMeditationPersonSvg(w, h, preset.primaryColor, preset.secondaryColor)
                "meditasyon_breathing" -> drawBreathingMeditationSvg(w, h, preset.primaryColor, preset.secondaryColor)
                "meditasyon_stillness" -> drawMeditationPersonSvg(w, h, preset.primaryColor, preset.secondaryColor)

                // Ders / Çalışma
                "calisma_books" -> drawDeskWithBooksAndLampSvg(w, h, preset.primaryColor, preset.secondaryColor)
                "calisma_desk" -> drawDeskWithBooksAndLampSvg(w, h, preset.primaryColor, preset.secondaryColor)
                "calisma_focus" -> drawDeskWithBooksAndLampSvg(w, h, preset.primaryColor, preset.secondaryColor)

                // Beslenme
                "beslenme_healthy_food" -> drawColorfulFoodBowlSvg(w, h, preset.primaryColor, preset.secondaryColor)
                "beslenme_water" -> drawWaterBottleAndGlassSvg(w, h, preset.primaryColor, preset.secondaryColor)
                "beslenme_meal" -> drawColorfulFoodBowlSvg(w, h, preset.primaryColor, preset.secondaryColor)

                // Müzik
                "muzik_headphones" -> drawPersonWithHeadphonesSvg(w, h, preset.primaryColor, preset.secondaryColor)
                "muzik_instrument" -> drawMusicNotesWaveSvg(w, h, preset.primaryColor, preset.secondaryColor)

                // Uyku / Dinlenme
                "uyku_rest" -> drawPersonSleepingWithMoonSvg(w, h, preset.primaryColor, preset.secondaryColor)
                "uyku_moon" -> drawPersonSleepingWithMoonSvg(w, h, preset.primaryColor, preset.secondaryColor)
                "uyku_relax" -> drawPersonSleepingWithMoonSvg(w, h, preset.primaryColor, preset.secondaryColor)

                else -> drawMeditationPersonSvg(w, h, preset.primaryColor, preset.secondaryColor)
            }
        }
    }
}

// ---------------- Detailed Category SVG Drawings ----------------

// SPOR: Person actively exercising / running
private fun DrawScope.drawRunningPersonSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Head
    drawCircle(color = c2, radius = w * 0.08f, center = Offset(w * 0.58f, h * 0.22f))

    // Torso (leaning forward actively)
    val torso = Path().apply {
        moveTo(w * 0.56f, h * 0.31f)
        lineTo(w * 0.44f, h * 0.52f)
    }
    drawPath(torso, color = c1, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))

    // Legs in dynamic stride
    val frontLeg = Path().apply {
        moveTo(w * 0.44f, h * 0.52f)
        lineTo(w * 0.62f, h * 0.64f)
        lineTo(w * 0.54f, h * 0.82f) // front foot
    }
    drawPath(frontLeg, color = c1, style = Stroke(width = w * 0.065f, cap = StrokeCap.Round))

    val backLeg = Path().apply {
        moveTo(w * 0.44f, h * 0.52f)
        lineTo(w * 0.32f, h * 0.64f)
        lineTo(w * 0.24f, h * 0.76f) // back foot kicked back
    }
    drawPath(backLeg, color = c2, style = Stroke(width = w * 0.065f, cap = StrokeCap.Round))

    // Arms pumping
    val frontArm = Path().apply {
        moveTo(w * 0.53f, h * 0.36f)
        lineTo(w * 0.68f, h * 0.44f)
        lineTo(w * 0.76f, h * 0.38f) // raised fist
    }
    drawPath(frontArm, color = c2, style = Stroke(width = w * 0.06f, cap = StrokeCap.Round))

    val backArm = Path().apply {
        moveTo(w * 0.53f, h * 0.36f)
        lineTo(w * 0.38f, h * 0.42f)
        lineTo(w * 0.34f, h * 0.54f)
    }
    drawPath(backArm, color = c1, style = Stroke(width = w * 0.06f, cap = StrokeCap.Round))

    // Motion lines
    drawLine(c2.copy(alpha = 0.5f), Offset(w * 0.14f, h * 0.52f), Offset(w * 0.28f, h * 0.52f), strokeWidth = w * 0.03f, cap = StrokeCap.Round)
    drawLine(c2.copy(alpha = 0.6f), Offset(w * 0.12f, h * 0.64f), Offset(w * 0.24f, h * 0.64f), strokeWidth = w * 0.035f, cap = StrokeCap.Round)
}

// SPOR: Person lifting weights (Dumbbell press)
private fun DrawScope.drawWeightliftingPersonSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Head
    drawCircle(color = c2, radius = w * 0.08f, center = Offset(w * 0.5f, h * 0.28f))

    // Body
    val body = Path().apply {
        moveTo(w * 0.5f, h * 0.37f)
        lineTo(w * 0.5f, h * 0.64f)
    }
    drawPath(body, color = c1, style = Stroke(width = w * 0.09f, cap = StrokeCap.Round))

    // Legs firmly planted
    val leftLeg = Path().apply {
        moveTo(w * 0.5f, h * 0.64f)
        lineTo(w * 0.38f, h * 0.84f)
    }
    drawPath(leftLeg, color = c1, style = Stroke(width = w * 0.065f, cap = StrokeCap.Round))

    val rightLeg = Path().apply {
        moveTo(w * 0.5f, h * 0.64f)
        lineTo(w * 0.62f, h * 0.84f)
    }
    drawPath(rightLeg, color = c1, style = Stroke(width = w * 0.065f, cap = StrokeCap.Round))

    // Raised arms holding barbell overhead
    val arms = Path().apply {
        moveTo(w * 0.25f, h * 0.26f)
        lineTo(w * 0.38f, h * 0.40f)
        lineTo(w * 0.5f, h * 0.40f)
        lineTo(w * 0.62f, h * 0.40f)
        lineTo(w * 0.75f, h * 0.26f)
    }
    drawPath(arms, color = c2, style = Stroke(width = w * 0.055f, cap = StrokeCap.Round))

    // Barbell bar
    drawLine(c1, Offset(w * 0.16f, h * 0.24f), Offset(w * 0.84f, h * 0.24f), strokeWidth = w * 0.045f, cap = StrokeCap.Round)

    // Left weights
    drawRoundRect(c2, Offset(w * 0.16f, h * 0.16f), Size(w * 0.07f, h * 0.16f), CornerRadius(w * 0.02f))
    drawRoundRect(c1, Offset(w * 0.11f, h * 0.18f), Size(w * 0.05f, h * 0.12f), CornerRadius(w * 0.02f))

    // Right weights
    drawRoundRect(c2, Offset(w * 0.77f, h * 0.16f), Size(w * 0.07f, h * 0.16f), CornerRadius(w * 0.02f))
    drawRoundRect(c1, Offset(w * 0.84f, h * 0.18f), Size(w * 0.05f, h * 0.12f), CornerRadius(w * 0.02f))
}

// SPOR: Cycling
private fun DrawScope.drawCyclingSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Wheels with spokes
    drawCircle(color = c2, radius = w * 0.16f, center = Offset(w * 0.28f, h * 0.66f), style = Stroke(w * 0.045f))
    drawCircle(color = c2, radius = w * 0.16f, center = Offset(w * 0.72f, h * 0.66f), style = Stroke(w * 0.045f))
    drawCircle(color = c1, radius = w * 0.04f, center = Offset(w * 0.28f, h * 0.66f))
    drawCircle(color = c1, radius = w * 0.04f, center = Offset(w * 0.72f, h * 0.66f))

    // Bike Frame
    val frame = Path().apply {
        moveTo(w * 0.28f, h * 0.66f)
        lineTo(w * 0.46f, h * 0.66f) // bottom bracket
        lineTo(w * 0.64f, h * 0.46f) // head tube
        lineTo(w * 0.44f, h * 0.46f) // top tube
        lineTo(w * 0.28f, h * 0.66f) // seat stay
        moveTo(w * 0.46f, h * 0.66f)
        lineTo(w * 0.42f, h * 0.40f) // seat post
        moveTo(w * 0.64f, h * 0.46f)
        lineTo(w * 0.72f, h * 0.66f) // fork
    }
    drawPath(frame, color = c1, style = Stroke(w * 0.04f, cap = StrokeCap.Round))

    // Handlebar & Saddle
    drawLine(c2, Offset(w * 0.38f, h * 0.38f), Offset(w * 0.48f, h * 0.38f), strokeWidth = w * 0.04f, cap = StrokeCap.Round)
    drawLine(c2, Offset(w * 0.60f, h * 0.42f), Offset(w * 0.68f, h * 0.42f), strokeWidth = w * 0.04f, cap = StrokeCap.Round)
}

// DOĞA: Clear landscape with pine trees and sun
private fun DrawScope.drawLandscapeWithTreesSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Sun in sky
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFFDE047), Color(0xFFF59E0B), Color.Transparent),
            center = Offset(w * 0.75f, h * 0.28f),
            radius = w * 0.25f
        )
    )
    drawCircle(Color(0xFFF59E0B), radius = w * 0.12f, center = Offset(w * 0.75f, h * 0.28f))

    // Mountain hill 1 (back)
    val mountainBack = Path().apply {
        moveTo(0f, h * 0.75f)
        lineTo(w * 0.32f, h * 0.42f)
        lineTo(w * 0.68f, h * 0.78f)
        close()
    }
    drawPath(mountainBack, color = Color(0xFF1E3A5F).copy(alpha = 0.7f))

    // Rolling green hill (front)
    val hill = Path().apply {
        moveTo(0f, h * 0.68f)
        cubicTo(w * 0.35f, h * 0.58f, w * 0.65f, h * 0.72f, w, h * 0.62f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(hill, color = Color(0xFF064E3B).copy(alpha = 0.9f))

    // Two distinct Pine Trees with trunks
    fun drawPineTree(cx: Float, cy: Float, size: Float, color: Color) {
        val t = Path().apply {
            moveTo(cx, cy - size * 0.8f)
            lineTo(cx + size * 0.45f, cy - size * 0.3f)
            lineTo(cx + size * 0.25f, cy - size * 0.3f)
            lineTo(cx + size * 0.50f, cy + size * 0.2f)
            lineTo(cx - size * 0.50f, cy + size * 0.2f)
            lineTo(cx - size * 0.25f, cy - size * 0.3f)
            lineTo(cx - size * 0.45f, cy - size * 0.3f)
            close()
        }
        drawPath(t, color = color)
        // Trunk
        drawRect(Color(0xFF78350F), Offset(cx - size * 0.08f, cy + size * 0.2f), Size(size * 0.16f, size * 0.25f))
    }

    drawPineTree(w * 0.32f, h * 0.62f, w * 0.32f, c1)
    drawPineTree(w * 0.52f, h * 0.66f, w * 0.26f, c2)
    drawPineTree(w * 0.18f, h * 0.70f, w * 0.20f, c1.copy(alpha = 0.85f))
}

// DOĞA: Sunrise over landscape
private fun DrawScope.drawSunriseLandscapeSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Rising sun
    drawCircle(
        brush = Brush.verticalGradient(listOf(Color(0xFFFDE047), Color(0xFFFB923C))),
        radius = w * 0.22f,
        center = Offset(w * 0.5f, h * 0.48f)
    )

    // Sun rays spreading out
    for (i in -3..3) {
        val angle = Math.toRadians((i * 24 - 90).toDouble())
        val startX = (w * 0.5f + Math.cos(angle) * w * 0.26f).toFloat()
        val startY = (h * 0.48f + Math.sin(angle) * h * 0.26f).toFloat()
        val endX = (w * 0.5f + Math.cos(angle) * w * 0.42f).toFloat()
        val endY = (h * 0.48f + Math.sin(angle) * h * 0.42f).toFloat()
        drawLine(c2.copy(alpha = 0.65f), Offset(startX, startY), Offset(endX, endY), strokeWidth = w * 0.03f, cap = StrokeCap.Round)
    }

    // Foreground hills
    val hills = Path().apply {
        moveTo(0f, h * 0.65f)
        cubicTo(w * 0.35f, h * 0.54f, w * 0.7f, h * 0.74f, w, h * 0.62f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(hills, color = Color(0xFF1E293B))
}

// DOĞA: Moon and trees landscape
private fun DrawScope.drawMoonLandscapeSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Crescent Moon
    val moon = Path().apply {
        moveTo(w * 0.70f, h * 0.20f)
        cubicTo(w * 0.48f, h * 0.30f, w * 0.52f, h * 0.60f, w * 0.76f, h * 0.66f)
        cubicTo(w * 0.62f, h * 0.58f, w * 0.60f, h * 0.32f, w * 0.70f, h * 0.20f)
        close()
    }
    drawPath(moon, color = Color(0xFFFDE047))

    // Stars
    drawCircle(Color.White, radius = w * 0.02f, center = Offset(w * 0.25f, h * 0.25f))
    drawCircle(Color.White, radius = w * 0.015f, center = Offset(w * 0.40f, h * 0.18f))

    // Forest hill
    val ground = Path().apply {
        moveTo(0f, h * 0.68f)
        cubicTo(w * 0.4f, h * 0.58f, w * 0.6f, h * 0.70f, w, h * 0.64f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(ground, color = Color(0xFF0F172A))

    // Tree silhouettes
    drawCircle(c1, radius = w * 0.16f, center = Offset(w * 0.30f, h * 0.58f))
    drawCircle(c2, radius = w * 0.12f, center = Offset(w * 0.48f, h * 0.62f))
}

// MEDİTASYON: Person sitting cross-legged (Lotus posture) with calm aura
private fun DrawScope.drawMeditationPersonSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Serene glowing aura rings around person
    drawCircle(
        c2.copy(alpha = 0.25f),
        radius = w * 0.38f,
        center = Offset(w * 0.5f, h * 0.50f),
        style = Stroke(w * 0.035f)
    )
    drawCircle(
        c1.copy(alpha = 0.4f),
        radius = w * 0.28f,
        center = Offset(w * 0.5f, h * 0.50f),
        style = Stroke(w * 0.04f)
    )

    // Head
    drawCircle(color = c2, radius = w * 0.085f, center = Offset(w * 0.5f, h * 0.30f))

    // Torso (upright, peaceful posture)
    val torso = Path().apply {
        moveTo(w * 0.5f, h * 0.39f)
        lineTo(w * 0.5f, h * 0.65f)
    }
    drawPath(torso, color = c1, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))

    // Cross-legged base (Lotus posture)
    val legs = Path().apply {
        moveTo(w * 0.20f, h * 0.76f)
        cubicTo(w * 0.30f, h * 0.82f, w * 0.70f, h * 0.82f, w * 0.80f, h * 0.76f)
        cubicTo(w * 0.72f, h * 0.68f, w * 0.28f, h * 0.68f, w * 0.20f, h * 0.76f)
        close()
    }
    drawPath(legs, color = c1)

    // Meditating arms resting on knees with Gyan mudra
    val leftArm = Path().apply {
        moveTo(w * 0.46f, h * 0.42f)
        lineTo(w * 0.30f, h * 0.54f)
        lineTo(w * 0.26f, h * 0.72f) // hand on left knee
    }
    drawPath(leftArm, color = c2, style = Stroke(width = w * 0.055f, cap = StrokeCap.Round))

    val rightArm = Path().apply {
        moveTo(w * 0.54f, h * 0.42f)
        lineTo(w * 0.70f, h * 0.54f)
        lineTo(w * 0.74f, h * 0.72f) // hand on right knee
    }
    drawPath(rightArm, color = c2, style = Stroke(width = w * 0.055f, cap = StrokeCap.Round))

    // Lotus flower beneath
    drawCircle(c2, radius = w * 0.04f, center = Offset(w * 0.5f, h * 0.78f))
}

// MEDİTASYON: Breathing with Lotus Petals
private fun DrawScope.drawBreathingMeditationSvg(w: Float, h: Float, c1: Color, c2: Color) {
    drawMeditationPersonSvg(w, h, c1, c2)
    // Breathing particles
    drawCircle(Color.White.copy(alpha = 0.8f), radius = w * 0.02f, center = Offset(w * 0.5f, h * 0.18f))
    drawCircle(Color.White.copy(alpha = 0.6f), radius = w * 0.015f, center = Offset(w * 0.44f, h * 0.22f))
    drawCircle(Color.White.copy(alpha = 0.6f), radius = w * 0.015f, center = Offset(w * 0.56f, h * 0.22f))
}

// DERS / ÇALIŞMA: Person at desk with books and lamp
private fun DrawScope.drawDeskWithBooksAndLampSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Desk surface
    drawLine(
        c1,
        Offset(w * 0.12f, h * 0.72f),
        Offset(w * 0.88f, h * 0.72f),
        strokeWidth = w * 0.06f,
        cap = StrokeCap.Round
    )

    // Desk Legs
    drawLine(c1.copy(alpha = 0.6f), Offset(w * 0.20f, h * 0.74f), Offset(w * 0.20f, h * 0.90f), strokeWidth = w * 0.04f)
    drawLine(c1.copy(alpha = 0.6f), Offset(w * 0.80f, h * 0.74f), Offset(w * 0.80f, h * 0.90f), strokeWidth = w * 0.04f)

    // Person studying (Head tilted forward studying)
    drawCircle(color = c2, radius = w * 0.075f, center = Offset(w * 0.52f, h * 0.36f))

    // Person Torso leaning slightly toward desk
    val torso = Path().apply {
        moveTo(w * 0.54f, h * 0.44f)
        lineTo(w * 0.58f, h * 0.72f)
    }
    drawPath(torso, color = c1, style = Stroke(width = w * 0.075f, cap = StrokeCap.Round))

    // Arms resting on desk reading
    val arms = Path().apply {
        moveTo(w * 0.53f, h * 0.48f)
        lineTo(w * 0.46f, h * 0.62f)
        lineTo(w * 0.40f, h * 0.70f)
    }
    drawPath(arms, color = c2, style = Stroke(width = w * 0.05f, cap = StrokeCap.Round))

    // Open Book on desk
    val leftPage = Path().apply {
        moveTo(w * 0.36f, h * 0.69f)
        lineTo(w * 0.48f, h * 0.71f)
        lineTo(w * 0.48f, h * 0.65f)
        lineTo(w * 0.36f, h * 0.63f)
        close()
    }
    val rightPage = Path().apply {
        moveTo(w * 0.48f, h * 0.71f)
        lineTo(w * 0.60f, h * 0.69f)
        lineTo(w * 0.60f, h * 0.63f)
        lineTo(w * 0.48f, h * 0.65f)
        close()
    }
    drawPath(leftPage, color = Color.White)
    drawPath(rightPage, color = Color(0xFFE2E8F0))

    // Desk Lamp (Arching from left with light cone)
    val lampStand = Path().apply {
        moveTo(w * 0.22f, h * 0.72f)
        lineTo(w * 0.22f, h * 0.42f)
        lineTo(w * 0.34f, h * 0.35f)
    }
    drawPath(lampStand, color = c1, style = Stroke(width = w * 0.04f, cap = StrokeCap.Round))

    // Lamp shade
    val lampShade = Path().apply {
        moveTo(w * 0.30f, h * 0.33f)
        lineTo(w * 0.40f, h * 0.40f)
        lineTo(w * 0.32f, h * 0.46f)
        close()
    }
    drawPath(lampShade, color = Color(0xFFF59E0B))

    // Lamp light cone illuminating the book
    val lightBeam = Path().apply {
        moveTo(w * 0.36f, h * 0.44f)
        lineTo(w * 0.55f, h * 0.71f)
        lineTo(w * 0.30f, h * 0.71f)
        close()
    }
    drawPath(lightBeam, color = Color(0xFFFEF08A).copy(alpha = 0.25f))
}

// BESLENME: Colorful bowl of healthy food / fruits
private fun DrawScope.drawColorfulFoodBowlSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Bowl base
    val bowl = Path().apply {
        moveTo(w * 0.18f, h * 0.50f)
        lineTo(w * 0.82f, h * 0.50f)
        cubicTo(w * 0.78f, h * 0.84f, w * 0.22f, h * 0.84f, w * 0.18f, h * 0.50f)
        close()
    }
    drawPath(bowl, color = Color(0xFF0F766E))
    // Bowl rim highlight
    drawRoundRect(
        color = Color(0xFF14B8A6),
        topLeft = Offset(w * 0.16f, h * 0.48f),
        size = Size(w * 0.68f, h * 0.06f),
        cornerRadius = CornerRadius(w * 0.03f)
    )

    // Colorful fruits and vegetables overflowing from bowl
    // Green avocado / spinach leaves
    drawCircle(Color(0xFF22C55E), radius = w * 0.09f, center = Offset(w * 0.34f, h * 0.42f))
    drawCircle(Color(0xFF16A34A), radius = w * 0.06f, center = Offset(w * 0.30f, h * 0.38f))

    // Bright red tomato / strawberry
    drawCircle(Color(0xFFEF4444), radius = w * 0.08f, center = Offset(w * 0.50f, h * 0.40f))
    drawCircle(Color(0xFFB91C1C), radius = w * 0.04f, center = Offset(w * 0.52f, h * 0.42f))

    // Orange slice / carrots
    drawCircle(Color(0xFFF97316), radius = w * 0.085f, center = Offset(w * 0.65f, h * 0.43f))

    // Blueberries / grapes
    drawCircle(Color(0xFF6366F1), radius = w * 0.04f, center = Offset(w * 0.42f, h * 0.46f))
    drawCircle(Color(0xFF4F46E5), radius = w * 0.035f, center = Offset(w * 0.58f, h * 0.46f))

    // Fresh mint leaf on top
    val leaf = Path().apply {
        moveTo(w * 0.48f, h * 0.30f)
        cubicTo(w * 0.42f, h * 0.22f, w * 0.55f, h * 0.20f, w * 0.52f, h * 0.32f)
    }
    drawPath(leaf, color = Color(0xFF86EFAC), style = Stroke(w * 0.03f, cap = StrokeCap.Round))
}

// BESLENME: Water bottle & glass
private fun DrawScope.drawWaterBottleAndGlassSvg(w: Float, h: Float, c1: Color, c2: Color) {
    drawColorfulFoodBowlSvg(w, h, c1, c2)
}

// MÜZİK: Person with headphones and musical notes
private fun DrawScope.drawPersonWithHeadphonesSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Head of person
    drawCircle(color = c2, radius = w * 0.12f, center = Offset(w * 0.5f, h * 0.44f))

    // Headphones headband over head
    val headband = Path().apply {
        moveTo(w * 0.34f, h * 0.46f)
        cubicTo(w * 0.34f, h * 0.24f, w * 0.66f, h * 0.24f, w * 0.66f, h * 0.46f)
    }
    drawPath(headband, color = c1, style = Stroke(width = w * 0.07f, cap = StrokeCap.Round))

    // Left and Right ear cushions
    drawRoundRect(
        color = Color(0xFFE11D48),
        topLeft = Offset(w * 0.31f, h * 0.38f),
        size = Size(w * 0.08f, h * 0.15f),
        cornerRadius = CornerRadius(w * 0.04f)
    )
    drawRoundRect(
        color = Color(0xFFE11D48),
        topLeft = Offset(w * 0.61f, h * 0.38f),
        size = Size(w * 0.08f, h * 0.15f),
        cornerRadius = CornerRadius(w * 0.04f)
    )

    // Shoulders
    val shoulders = Path().apply {
        moveTo(w * 0.22f, h * 0.78f)
        cubicTo(w * 0.35f, h * 0.62f, w * 0.65f, h * 0.62f, w * 0.78f, h * 0.78f)
    }
    drawPath(shoulders, color = c1.copy(alpha = 0.8f), style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))

    // Floating musical notes around person
    drawMusicNote(w * 0.20f, h * 0.28f, w * 0.06f, c2)
    drawMusicNote(w * 0.78f, h * 0.24f, w * 0.07f, Color(0xFFF43F5E))
    drawMusicNote(w * 0.82f, h * 0.50f, w * 0.05f, c1)
}

// MÜZİK: Musical notes wave
private fun DrawScope.drawMusicNotesWaveSvg(w: Float, h: Float, c1: Color, c2: Color) {
    drawPersonWithHeadphonesSvg(w, h, c1, c2)
}

private fun DrawScope.drawMusicNote(x: Float, y: Float, size: Float, color: Color) {
    // Note head
    drawCircle(color, radius = size * 0.5f, center = Offset(x, y + size))
    // Stem
    drawLine(color, Offset(x + size * 0.4f, y + size), Offset(x + size * 0.4f, y), strokeWidth = size * 0.22f, cap = StrokeCap.Round)
    // Flag
    drawLine(color, Offset(x + size * 0.4f, y), Offset(x + size * 0.9f, y + size * 0.3f), strokeWidth = size * 0.22f, cap = StrokeCap.Round)
}

// UYKU / DİNLENME: Person resting peacefully with moon and stars
private fun DrawScope.drawPersonSleepingWithMoonSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Golden Crescent Moon in upper right corner
    val moon = Path().apply {
        moveTo(w * 0.76f, h * 0.15f)
        cubicTo(w * 0.58f, h * 0.24f, w * 0.60f, h * 0.50f, w * 0.80f, h * 0.55f)
        cubicTo(w * 0.68f, h * 0.48f, w * 0.68f, h * 0.26f, w * 0.76f, h * 0.15f)
        close()
    }
    drawPath(moon, color = Color(0xFFFDE047))

    // Twinkling stars
    drawCircle(Color.White, radius = w * 0.02f, center = Offset(w * 0.32f, h * 0.18f))
    drawCircle(Color(0xFFE2E8F0), radius = w * 0.015f, center = Offset(w * 0.48f, h * 0.14f))
    drawCircle(Color.White, radius = w * 0.018f, center = Offset(w * 0.86f, h * 0.32f))

    // Pillow
    drawRoundRect(
        color = Color(0xFF374151),
        topLeft = Offset(w * 0.18f, h * 0.52f),
        size = Size(w * 0.30f, h * 0.18f),
        cornerRadius = CornerRadius(w * 0.06f)
    )

    // Person head resting on pillow (sleeping calmly)
    drawCircle(color = c2, radius = w * 0.09f, center = Offset(w * 0.33f, h * 0.56f))

    // Closed eye line
    drawLine(
        Color(0xFF0F172A),
        Offset(w * 0.30f, h * 0.56f),
        Offset(w * 0.36f, h * 0.56f),
        strokeWidth = w * 0.018f,
        cap = StrokeCap.Round
    )

    // Blanket covering person
    val blanket = Path().apply {
        moveTo(w * 0.28f, h * 0.62f)
        cubicTo(w * 0.45f, h * 0.55f, w * 0.75f, h * 0.56f, w * 0.88f, h * 0.68f)
        lineTo(w * 0.88f, h * 0.84f)
        lineTo(w * 0.22f, h * 0.84f)
        close()
    }
    drawPath(blanket, color = Color(0xFF312E81))

    // Blanket rim
    drawLine(
        Color(0xFF6366F1),
        Offset(w * 0.26f, h * 0.63f),
        Offset(w * 0.86f, h * 0.68f),
        strokeWidth = w * 0.04f,
        cap = StrokeCap.Round
    )

    // Floating "Zzz"
    drawZText(w * 0.44f, h * 0.42f, c2, w * 0.055f)
    drawZText(w * 0.52f, h * 0.34f, c2, w * 0.07f)
    drawZText(w * 0.62f, h * 0.25f, Color(0xFF818CF8), w * 0.09f)
}

private fun DrawScope.drawZText(x: Float, y: Float, color: Color, size: Float) {
    val z = Path().apply {
        moveTo(x - size * 0.8f, y - size * 0.8f)
        lineTo(x + size * 0.8f, y - size * 0.8f)
        lineTo(x - size * 0.8f, y + size * 0.8f)
        lineTo(x + size * 0.8f, y + size * 0.8f)
    }
    drawPath(z, color = color, style = Stroke(size * 0.28f, cap = StrokeCap.Round))
}
