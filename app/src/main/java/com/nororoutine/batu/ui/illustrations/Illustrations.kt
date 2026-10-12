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
import com.nororoutine.batu.network.WeatherCondition

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
    IllustrationPreset("doga_forest", "Doğa", "Orman", Color(0xFF059669), Color(0xFF10B981)),
    IllustrationPreset("doga_sunrise", "Doğa", "Gündoğumu", Color(0xFFF59E0B), Color(0xFFFB923C)),
    IllustrationPreset("doga_park", "Doğa", "Park Yürüyüşü", Color(0xFF14B8A6), Color(0xFF2DD4BF)),

    // Meditasyon
    IllustrationPreset("meditasyon_calm", "Meditasyon", "Huzur", Color(0xFF8B5CF6), Color(0xFFA78BFA)),
    IllustrationPreset("meditasyon_breathing", "Meditasyon", "Nefes", Color(0xFF06B6D4), Color(0xFF7CF4DE)),
    IllustrationPreset("meditasyon_stillness", "Meditasyon", "Dinginlik", Color(0xFF6366F1), Color(0xFF818CF8)),

    // Ders / Çalışma
    IllustrationPreset("calisma_books", "Ders / Çalışma", "Kitaplar", Color(0xFF3B82F6), Color(0xFF60A5FA)),
    IllustrationPreset("calisma_desk", "Ders / Çalışma", "Masa & Odak", Color(0xFF6366F1), Color(0xFFA855F7)),
    IllustrationPreset("calisma_focus", "Ders / Çalışma", "Hedef", Color(0xFFEC4899), Color(0xFFF472B6)),

    // Beslenme
    IllustrationPreset("beslenme_healthy_food", "Beslenme", "Sağlıklı Tabak", Color(0xFF22C55E), Color(0xFF86EFAC)),
    IllustrationPreset("beslenme_water", "Beslenme", "Su Dengesi", Color(0xFF0EA5E9), Color(0xFF38BDF8)),
    IllustrationPreset("beslenme_meal", "Beslenme", "Dengeli Öğün", Color(0xFFEAB308), Color(0xFFFACC15)),

    // Müzik
    IllustrationPreset("muzik_headphones", "Müzik", "Kulaklık", Color(0xFFA855F7), Color(0xFFC084FC)),
    IllustrationPreset("muzik_instrument", "Müzik", "Enstrüman", Color(0xFFF43F5E), Color(0xFFFB7185)),

    // Uyku / Dinlenme
    IllustrationPreset("uyku_rest", "Uyku / Dinlenme", "Yatak & Dinlenme", Color(0xFF6366F1), Color(0xFF818CF8)),
    IllustrationPreset("uyku_moon", "Uyku / Dinlenme", "Gece Uykusu", Color(0xFF312E81), Color(0xFF818CF8)),
    IllustrationPreset("uyku_relax", "Uyku / Dinlenme", "Kahve & Mola", Color(0xFFB45309), Color(0xFFD97706))
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
                        Color(0x33101C30)
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
                "spor_running" -> drawRunningSvg(w, h, preset.primaryColor, preset.secondaryColor)
                "spor_weights" -> drawWeightsSvg(w, h, preset.primaryColor, preset.secondaryColor)
                "spor_cycling" -> drawCyclingSvg(w, h, preset.primaryColor, preset.secondaryColor)

                // Doğa
                "doga_forest" -> drawForestSvg(w, h, preset.primaryColor, preset.secondaryColor)
                "doga_sunrise" -> drawSunriseSvg(w, h, preset.primaryColor, preset.secondaryColor)
                "doga_park" -> drawParkSvg(w, h, preset.primaryColor, preset.secondaryColor)

                // Meditasyon
                "meditasyon_calm" -> drawCalmSvg(w, h, preset.primaryColor, preset.secondaryColor)
                "meditasyon_breathing" -> drawBreathingSvg(w, h, preset.primaryColor, preset.secondaryColor)
                "meditasyon_stillness" -> drawStillnessSvg(w, h, preset.primaryColor, preset.secondaryColor)

                // Ders / Çalışma
                "calisma_books" -> drawBooksSvg(w, h, preset.primaryColor, preset.secondaryColor)
                "calisma_desk" -> drawDeskSvg(w, h, preset.primaryColor, preset.secondaryColor)
                "calisma_focus" -> drawFocusSvg(w, h, preset.primaryColor, preset.secondaryColor)

                // Beslenme
                "beslenme_healthy_food" -> drawBowlSvg(w, h, preset.primaryColor, preset.secondaryColor)
                "beslenme_water" -> drawWaterSvg(w, h, preset.primaryColor, preset.secondaryColor)
                "beslenme_meal" -> drawMealSvg(w, h, preset.primaryColor, preset.secondaryColor)

                // Müzik
                "muzik_headphones" -> drawHeadphonesSvg(w, h, preset.primaryColor, preset.secondaryColor)
                "muzik_instrument" -> drawMusicNotesSvg(w, h, preset.primaryColor, preset.secondaryColor)

                // Uyku / Dinlenme
                "uyku_rest" -> drawRestSvg(w, h, preset.primaryColor, preset.secondaryColor)
                "uyku_moon" -> drawMoonSvg(w, h, preset.primaryColor, preset.secondaryColor)
                "uyku_relax" -> drawCoffeeSvg(w, h, preset.primaryColor, preset.secondaryColor)

                else -> drawCalmSvg(w, h, preset.primaryColor, preset.secondaryColor)
            }
        }
    }
}

// ---------------- SVG Vector Drawings ----------------

private fun DrawScope.drawRunningSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Runner figure with vector paths
    drawCircle(color = c2, radius = w * 0.09f, center = Offset(w * 0.58f, h * 0.28f))
    // Torso & legs
    val path = Path().apply {
        moveTo(w * 0.56f, h * 0.38f)
        lineTo(w * 0.46f, h * 0.54f)
        lineTo(w * 0.32f, h * 0.58f)
        moveTo(w * 0.46f, h * 0.54f)
        lineTo(w * 0.58f, h * 0.72f)
        lineTo(w * 0.72f, h * 0.76f)
        // Arms
        moveTo(w * 0.52f, h * 0.42f)
        lineTo(w * 0.38f, h * 0.46f)
        moveTo(w * 0.54f, h * 0.44f)
        lineTo(w * 0.68f, h * 0.48f)
    }
    drawPath(path, color = c1, style = Stroke(width = w * 0.06f, cap = StrokeCap.Round))
    // Speed dash
    drawLine(c2.copy(alpha = 0.6f), Offset(w * 0.2f, h * 0.72f), Offset(w * 0.35f, h * 0.72f), strokeWidth = w * 0.04f, cap = StrokeCap.Round)
}

private fun DrawScope.drawWeightsSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Dumbbell bar
    drawLine(c1, Offset(w * 0.28f, h * 0.5f), Offset(w * 0.72f, h * 0.5f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
    // Left weights
    drawRoundRect(c2, Offset(w * 0.22f, h * 0.32f), Size(w * 0.09f, h * 0.36f), CornerRadius(w * 0.03f))
    drawRoundRect(c1, Offset(w * 0.15f, h * 0.38f), Size(w * 0.07f, h * 0.24f), CornerRadius(w * 0.02f))
    // Right weights
    drawRoundRect(c2, Offset(w * 0.69f, h * 0.32f), Size(w * 0.09f, h * 0.36f), CornerRadius(w * 0.03f))
    drawRoundRect(c1, Offset(w * 0.78f, h * 0.38f), Size(w * 0.07f, h * 0.24f), CornerRadius(w * 0.02f))
}

private fun DrawScope.drawCyclingSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Wheels
    drawCircle(color = c2, radius = w * 0.16f, center = Offset(w * 0.32f, h * 0.62f), style = Stroke(w * 0.05f))
    drawCircle(color = c2, radius = w * 0.16f, center = Offset(w * 0.68f, h * 0.62f), style = Stroke(w * 0.05f))
    // Frame
    val frame = Path().apply {
        moveTo(w * 0.32f, h * 0.62f)
        lineTo(w * 0.48f, h * 0.62f)
        lineTo(w * 0.64f, h * 0.45f)
        lineTo(w * 0.48f, h * 0.45f)
        close()
        moveTo(w * 0.64f, h * 0.45f)
        lineTo(w * 0.68f, h * 0.62f)
    }
    drawPath(frame, color = c1, style = Stroke(w * 0.04f, cap = StrokeCap.Round))
}

private fun DrawScope.drawForestSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Layered pine trees
    fun drawPine(centerX: Float, centerY: Float, scale: Float, col: Color) {
        val tree = Path().apply {
            moveTo(centerX, centerY - h * 0.24f * scale)
            lineTo(centerX + w * 0.18f * scale, centerY + h * 0.12f * scale)
            lineTo(centerX - w * 0.18f * scale, centerY + h * 0.12f * scale)
            close()
        }
        drawPath(tree, color = col)
        // Trunk
        drawRect(Color(0xFF854D0E), Offset(centerX - w * 0.03f * scale, centerY + h * 0.12f * scale), Size(w * 0.06f * scale, h * 0.08f * scale))
    }
    drawPine(w * 0.35f, h * 0.52f, 0.85f, c1)
    drawPine(w * 0.65f, h * 0.54f, 0.75f, c1.copy(alpha = 0.8f))
    drawPine(w * 0.5f, h * 0.48f, 1.1f, c2)
}

private fun DrawScope.drawSunriseSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Rising sun
    drawCircle(brush = Brush.verticalGradient(listOf(c2, c1)), radius = w * 0.22f, center = Offset(w * 0.5f, h * 0.52f))
    // Horizon hills
    val hill1 = Path().apply {
        moveTo(0f, h * 0.72f)
        cubicTo(w * 0.3f, h * 0.55f, w * 0.7f, h * 0.78f, w, h * 0.65f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(hill1, color = Color(0xFF1E293B).copy(alpha = 0.85f))
    // Sun rays
    for (i in -3..3) {
        val angle = Math.toRadians((i * 25 - 90).toDouble())
        val startX = (w * 0.5f + Math.cos(angle) * w * 0.28f).toFloat()
        val startY = (h * 0.52f + Math.sin(angle) * h * 0.28f).toFloat()
        val endX = (w * 0.5f + Math.cos(angle) * w * 0.4f).toFloat()
        val endY = (h * 0.52f + Math.sin(angle) * h * 0.4f).toFloat()
        drawLine(c2.copy(alpha = 0.6f), Offset(startX, startY), Offset(endX, endY), strokeWidth = w * 0.035f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawParkSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Path through park
    val path = Path().apply {
        moveTo(w * 0.42f, h)
        cubicTo(w * 0.48f, h * 0.7f, w * 0.55f, h * 0.6f, w * 0.52f, h * 0.42f)
        lineTo(w * 0.56f, h * 0.42f)
        cubicTo(w * 0.6f, h * 0.6f, w * 0.54f, h * 0.75f, w * 0.62f, h)
        close()
    }
    drawPath(path, color = c2.copy(alpha = 0.4f))
    // Park trees
    drawCircle(c1, radius = w * 0.15f, center = Offset(w * 0.28f, h * 0.42f))
    drawCircle(c2, radius = w * 0.12f, center = Offset(w * 0.72f, h * 0.45f))
}

private fun DrawScope.drawCalmSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Lotus petals / aura
    val p1 = Path().apply {
        moveTo(w * 0.5f, h * 0.28f)
        cubicTo(w * 0.38f, h * 0.45f, w * 0.4f, h * 0.65f, w * 0.5f, h * 0.75f)
        cubicTo(w * 0.6f, h * 0.65f, w * 0.62f, h * 0.45f, w * 0.5f, h * 0.28f)
    }
    drawPath(p1, color = c2.copy(alpha = 0.8f))
    // Side petals
    val pLeft = Path().apply {
        moveTo(w * 0.5f, h * 0.75f)
        cubicTo(w * 0.3f, h * 0.7f, w * 0.22f, h * 0.55f, w * 0.32f, h * 0.42f)
        cubicTo(w * 0.36f, h * 0.55f, w * 0.42f, h * 0.68f, w * 0.5f, h * 0.75f)
    }
    drawPath(pLeft, color = c1)
    val pRight = Path().apply {
        moveTo(w * 0.5f, h * 0.75f)
        cubicTo(w * 0.7f, h * 0.7f, w * 0.78f, h * 0.55f, w * 0.68f, h * 0.42f)
        cubicTo(w * 0.64f, h * 0.55f, w * 0.58f, h * 0.68f, w * 0.5f, h * 0.75f)
    }
    drawPath(pRight, color = c1)
}

private fun DrawScope.drawBreathingSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Concentric breathing rings
    drawCircle(c1.copy(alpha = 0.3f), radius = w * 0.35f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(w * 0.04f))
    drawCircle(c2.copy(alpha = 0.6f), radius = w * 0.24f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(w * 0.05f))
    drawCircle(c2, radius = w * 0.12f, center = Offset(w * 0.5f, h * 0.5f))
}

private fun DrawScope.drawStillnessSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Balanced zen stones
    drawOval(c1, Offset(w * 0.25f, h * 0.68f), Size(w * 0.5f, h * 0.16f))
    drawOval(c2, Offset(w * 0.32f, h * 0.52f), Size(w * 0.36f, h * 0.14f))
    drawOval(c1, Offset(w * 0.38f, h * 0.38f), Size(w * 0.24f, h * 0.12f))
    drawCircle(c2, radius = w * 0.06f, center = Offset(w * 0.5f, h * 0.3f))
}

private fun DrawScope.drawBooksSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Stack of books
    drawRoundRect(c1, Offset(w * 0.22f, h * 0.64f), Size(w * 0.56f, h * 0.14f), CornerRadius(w * 0.02f))
    drawRoundRect(c2, Offset(w * 0.26f, h * 0.48f), Size(w * 0.48f, h * 0.14f), CornerRadius(w * 0.02f))
    drawRoundRect(c1, Offset(w * 0.3f, h * 0.32f), Size(w * 0.4f, h * 0.14f), CornerRadius(w * 0.02f))
    // Bookmark ribbon
    drawLine(Color.White, Offset(w * 0.62f, h * 0.32f), Offset(w * 0.62f, h * 0.48f), strokeWidth = w * 0.03f)
}

private fun DrawScope.drawDeskSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Desk surface
    drawLine(c1, Offset(w * 0.18f, h * 0.72f), Offset(w * 0.82f, h * 0.72f), strokeWidth = w * 0.06f, cap = StrokeCap.Round)
    // Laptop
    drawRoundRect(c2, Offset(w * 0.35f, h * 0.42f), Size(w * 0.3f, h * 0.22f), CornerRadius(w * 0.02f))
    drawLine(c2, Offset(w * 0.3f, h * 0.66f), Offset(w * 0.7f, h * 0.66f), strokeWidth = w * 0.04f, cap = StrokeCap.Round)
    // Desk lamp
    val lamp = Path().apply {
        moveTo(w * 0.22f, h * 0.7f)
        lineTo(w * 0.22f, h * 0.45f)
        lineTo(w * 0.32f, h * 0.35f)
    }
    drawPath(lamp, color = c1, style = Stroke(w * 0.035f, cap = StrokeCap.Round))
}

private fun DrawScope.drawFocusSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Target bullseye with dart
    drawCircle(c1, radius = w * 0.32f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(w * 0.04f))
    drawCircle(c2, radius = w * 0.2f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(w * 0.04f))
    drawCircle(c2, radius = w * 0.08f, center = Offset(w * 0.5f, h * 0.5f))
    // Arrow
    drawLine(Color.White, Offset(w * 0.25f, h * 0.25f), Offset(w * 0.48f, h * 0.48f), strokeWidth = w * 0.04f, cap = StrokeCap.Round)
}

private fun DrawScope.drawBowlSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Bowl
    val bowl = Path().apply {
        moveTo(w * 0.22f, h * 0.52f)
        lineTo(w * 0.78f, h * 0.52f)
        cubicTo(w * 0.75f, h * 0.8f, w * 0.25f, h * 0.8f, w * 0.22f, h * 0.52f)
    }
    drawPath(bowl, color = c1)
    // Veggies / Salad leaves
    drawCircle(c2, radius = w * 0.08f, center = Offset(w * 0.38f, h * 0.45f))
    drawCircle(Color(0xFFEF4444), radius = w * 0.06f, center = Offset(w * 0.52f, h * 0.43f))
    drawCircle(c2, radius = w * 0.09f, center = Offset(w * 0.64f, h * 0.46f))
}

private fun DrawScope.drawWaterSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Water drop
    val drop = Path().apply {
        moveTo(w * 0.5f, h * 0.24f)
        cubicTo(w * 0.68f, h * 0.48f, w * 0.72f, h * 0.72f, w * 0.5f, h * 0.76f)
        cubicTo(w * 0.28f, h * 0.72f, w * 0.32f, h * 0.48f, w * 0.5f, h * 0.24f)
    }
    drawPath(drop, color = c2)
    // Ripple rings
    drawOval(c1.copy(alpha = 0.6f), Offset(w * 0.28f, h * 0.78f), Size(w * 0.44f, h * 0.09f), style = Stroke(w * 0.03f))
}

private fun DrawScope.drawMealSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Plate
    drawCircle(c1, radius = w * 0.34f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(w * 0.05f))
    drawCircle(c2.copy(alpha = 0.25f), radius = w * 0.26f, center = Offset(w * 0.5f, h * 0.5f))
    // Fork & knife
    drawLine(c2, Offset(w * 0.2f, h * 0.3f), Offset(w * 0.2f, h * 0.7f), strokeWidth = w * 0.035f, cap = StrokeCap.Round)
    drawLine(c2, Offset(w * 0.8f, h * 0.3f), Offset(w * 0.8f, h * 0.7f), strokeWidth = w * 0.035f, cap = StrokeCap.Round)
}

private fun DrawScope.drawHeadphonesSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Headband
    val band = Path().apply {
        moveTo(w * 0.26f, h * 0.56f)
        cubicTo(w * 0.26f, h * 0.26f, w * 0.74f, h * 0.26f, w * 0.74f, h * 0.56f)
    }
    drawPath(band, color = c1, style = Stroke(w * 0.07f, cap = StrokeCap.Round))
    // Ear cushions
    drawRoundRect(c2, Offset(w * 0.2f, h * 0.52f), Size(w * 0.12f, h * 0.24f), CornerRadius(w * 0.04f))
    drawRoundRect(c2, Offset(w * 0.68f, h * 0.52f), Size(w * 0.12f, h * 0.24f), CornerRadius(w * 0.04f))
}

private fun DrawScope.drawMusicNotesSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Music notes
    drawCircle(c1, radius = w * 0.08f, center = Offset(w * 0.36f, h * 0.68f))
    drawCircle(c1, radius = w * 0.08f, center = Offset(w * 0.66f, h * 0.58f))
    drawLine(c2, Offset(w * 0.42f, h * 0.68f), Offset(w * 0.42f, h * 0.32f), strokeWidth = w * 0.04f)
    drawLine(c2, Offset(w * 0.72f, h * 0.58f), Offset(w * 0.72f, h * 0.22f), strokeWidth = w * 0.04f)
    drawLine(c1, Offset(w * 0.42f, h * 0.32f), Offset(w * 0.72f, h * 0.22f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
}

private fun DrawScope.drawRestSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Bed / pillow
    drawRoundRect(c1, Offset(w * 0.2f, h * 0.54f), Size(w * 0.6f, h * 0.24f), CornerRadius(w * 0.04f))
    drawRoundRect(c2, Offset(w * 0.24f, h * 0.42f), Size(w * 0.26f, h * 0.14f), CornerRadius(w * 0.03f))
    // Zzz
    drawTextZ(w * 0.65f, h * 0.32f, c2, w * 0.08f)
    drawTextZ(w * 0.76f, h * 0.24f, c2, w * 0.06f)
}

private fun DrawScope.drawMoonSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Crescent Moon
    val moon = Path().apply {
        moveTo(w * 0.52f, h * 0.22f)
        cubicTo(w * 0.28f, h * 0.35f, w * 0.32f, h * 0.72f, w * 0.62f, h * 0.78f)
        cubicTo(w * 0.44f, h * 0.68f, w * 0.42f, h * 0.4f, w * 0.52f, h * 0.22f)
        close()
    }
    drawPath(moon, color = c2)
    // Little stars
    drawCircle(c1, radius = w * 0.03f, center = Offset(w * 0.68f, h * 0.32f))
    drawCircle(c1, radius = w * 0.02f, center = Offset(w * 0.76f, h * 0.46f))
    drawCircle(c1, radius = w * 0.025f, center = Offset(w * 0.32f, h * 0.26f))
}

private fun DrawScope.drawCoffeeSvg(w: Float, h: Float, c1: Color, c2: Color) {
    // Coffee cup
    val cup = Path().apply {
        moveTo(w * 0.3f, h * 0.45f)
        lineTo(w * 0.7f, h * 0.45f)
        cubicTo(w * 0.68f, h * 0.76f, w * 0.32f, h * 0.76f, w * 0.3f, h * 0.45f)
    }
    drawPath(cup, color = c1)
    // Handle
    drawArc(c1, 270f, 180f, false, Offset(w * 0.62f, h * 0.48f), Size(w * 0.16f, h * 0.18f), style = Stroke(w * 0.04f))
    // Saucer
    drawRoundRect(c2, Offset(w * 0.24f, h * 0.78f), Size(w * 0.52f, h * 0.06f), CornerRadius(w * 0.02f))
}

private fun DrawScope.drawTextZ(x: Float, y: Float, color: Color, size: Float) {
    val z = Path().apply {
        moveTo(x - size, y - size)
        lineTo(x + size, y - size)
        lineTo(x - size, y + size)
        lineTo(x + size, y + size)
    }
    drawPath(z, color = color, style = Stroke(size * 0.35f, cap = StrokeCap.Round))
}
