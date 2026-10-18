package com.nororoutine.batu.ui.components

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nororoutine.batu.model.QuestionGraphInfo
import com.nororoutine.batu.ui.theme.AccentPurple
import com.nororoutine.batu.ui.theme.AccentTeal
import com.nororoutine.batu.ui.theme.DoneGreen
import com.nororoutine.batu.ui.theme.GlassCardBorderSubtle
import com.nororoutine.batu.ui.theme.PureWhite
import com.nororoutine.batu.ui.theme.SubtitleWhite
import kotlin.math.roundToInt

@Composable
fun InteractiveGraphCanvas(
    graphInfo: QuestionGraphInfo,
    modifier: Modifier = Modifier,
    heightDp: Int = 220
) {
    var inspectPoint by remember { mutableStateOf<Offset?>(null) }
    var inspectedMathCoords by remember { mutableStateOf<Pair<Float, Float>?>(null) }

    // Smooth entry animation for curve
    val progress = remember { Animatable(0f) }
    LaunchedEffect(graphInfo.a, graphInfo.b, graphInfo.c, graphInfo.type) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D0B1E))
            .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            // Header: Title and Live Coordinates Tracker
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = graphInfo.title,
                        color = PureWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val domainSubtitle = when {
                        graphInfo.type.startsWith("PHYSICS") -> "2028 Maarif Modeli Fizik Dinamiği"
                        graphInfo.type.startsWith("CHEM") -> "2028 Maarif Modeli Kimyasal Analiz"
                        else -> "2028 Maarif Modeli Matematik & Fonksiyon"
                    }
                    Text(
                        text = domainSubtitle,
                        color = SubtitleWhite,
                        fontSize = 10.sp
                    )
                }

                if (inspectedMathCoords != null) {
                    val (xVal, yVal) = inspectedMathCoords!!
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentTeal.copy(alpha = 0.2f))
                            .border(1.dp, AccentTeal, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "(x: %.2f, y: %.2f)".format(xVal, yVal),
                            color = AccentTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Graph Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightDp.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF13102C))
                    .pointerInput(graphInfo) {
                        detectTapGestures(
                            onTap = { offset ->
                                inspectPoint = offset
                            }
                        )
                    }
                    .pointerInput(graphInfo) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            inspectPoint = change.position
                        }
                    }
            ) {
                val width = size.width
                val height = size.height

                val minX = graphInfo.minX
                val maxX = graphInfo.maxX
                val minY = graphInfo.minY
                val maxY = graphInfo.maxY

                // Coordinate conversion functions
                fun mathToScreenX(mathX: Float): Float = ((mathX - minX) / (maxX - minX)) * width
                fun mathToScreenY(mathY: Float): Float = height - (((mathY - minY) / (maxY - minY)) * height)
                fun screenToMathX(screenX: Float): Float = minX + (screenX / width) * (maxX - minX)
                fun screenToMathY(screenY: Float): Float = minY + ((height - screenY) / height) * (maxY - minY)

                // 1. Draw Grid Lines (ticks every 1 or 2 units)
                val stepX = if (maxX - minX > 10f) 2f else 1f
                val stepY = if (maxY - minY > 10f) 2f else 1f

                val gridColor = Color(0x228B5CF6)
                var currX = (minX / stepX).toInt() * stepX
                while (currX <= maxX) {
                    val px = mathToScreenX(currX)
                    drawLine(
                        color = gridColor,
                        start = Offset(px, 0f),
                        end = Offset(px, height),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )
                    currX += stepX
                }

                var currY = (minY / stepY).toInt() * stepY
                while (currY <= maxY) {
                    val py = mathToScreenY(currY)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, py),
                        end = Offset(width, py),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )
                    currY += stepY
                }

                // 2. Draw Main Axes (X = 0 and Y = 0)
                val originX = mathToScreenX(0f).coerceIn(0f, width)
                val originY = mathToScreenY(0f).coerceIn(0f, height)

                // X-Axis (Horizontal)
                drawLine(
                    color = Color(0x99A5B4FC),
                    start = Offset(0f, originY),
                    end = Offset(width, originY),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
                // Y-Axis (Vertical)
                drawLine(
                    color = Color(0x99A5B4FC),
                    start = Offset(originX, height),
                    end = Offset(originX, 0f),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )

                // Axis Labels with units
                val textPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 28f
                    isAntiAlias = true
                }
                val labelPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#06B6D4") // Teal
                    textSize = 30f
                    isFakeBoldText = true
                    isAntiAlias = true
                }

                val xLabelWithUnit = if (graphInfo.unitX.isNotBlank()) "${graphInfo.xLabel} (${graphInfo.unitX})" else graphInfo.xLabel
                val yLabelWithUnit = if (graphInfo.unitY.isNotBlank()) "${graphInfo.yLabel} (${graphInfo.unitY})" else graphInfo.yLabel

                drawContext.canvas.nativeCanvas.apply {
                    drawText(xLabelWithUnit, width - 110f, originY - 10f, labelPaint)
                    drawText(yLabelWithUnit, originX + 10f, 32f, labelPaint)
                    drawText("0", originX + 6f, originY + 28f, textPaint)
                }

                // 3. Compute and Plot Function Curve for Math, Physics, and Chemistry
                fun evalFunction(x: Float): Float {
                    return when (graphInfo.type) {
                        "LINEAR" -> (graphInfo.a * x) + graphInfo.b
                        "CUBIC" -> (graphInfo.a * x * x * x) + (graphInfo.b * x * x) + (graphInfo.c * x)
                        "TRIGONOMETRIC" -> (graphInfo.a * kotlin.math.sin((graphInfo.b * x).toDouble()).toFloat()) + graphInfo.c
                        "HYPERBOLA", "PHYSICS_PV" -> {
                            if (x > 0.08f) (graphInfo.a / x).coerceIn(minY, maxY) else maxY
                        }
                        "PHYSICS_VT" -> {
                            // v(t) = v0 + a*t
                            (graphInfo.a * x) + graphInfo.b
                        }
                        "PHYSICS_XT" -> {
                            // x(t) = x0 + v0*t + 0.5*a*t^2
                            (0.5f * graphInfo.a * x * x) + (graphInfo.b * x) + graphInfo.c
                        }
                        "PHYSICS_AT" -> {
                            // a(t) = constant acceleration or step
                            graphInfo.a
                        }
                        "PHYSICS_WAVE" -> {
                            // Simple Harmonic Motion / Wave: y(x) = A * sin(k*x)
                            graphInfo.a * kotlin.math.sin((graphInfo.b * x).toDouble()).toFloat()
                        }
                        "PHYSICS_FX" -> {
                            // Hooke's Law / Force vs Distance F(x) = -k*x or linear
                            graphInfo.a * x
                        }
                        "CHEM_ENERGY_PROFILE", "ENERGY_PROFILE" -> {
                            // Chemistry PE-Reaction Coordinate: activation energy peak
                            val dist = (x - graphInfo.b)
                            val peak = graphInfo.a * kotlin.math.exp((-(dist * dist) / 2.8).toDouble()).toFloat()
                            val baseline = if (x < graphInfo.b) graphInfo.c else (graphInfo.c - 12f)
                            baseline + peak
                        }
                        "CHEM_TITRATION", "SIGMOID" -> {
                            // Chemistry pH Titration curve: c + a / (1 + e^(-0.8 * (x - b)))
                            val exponent = (-0.85f * (x - graphInfo.b)).coerceIn(-12f, 12f)
                            graphInfo.c + (graphInfo.a / (1f + kotlin.math.exp(exponent.toDouble()).toFloat()))
                        }
                        "CHEM_SOLUBILITY", "SOLUBILITY" -> {
                            // Chemistry Solubility vs Temperature: S(T)
                            val growth = Math.pow(1.025, x.toDouble().coerceIn(-50.0, 100.0)).toFloat()
                            (graphInfo.a * growth) + graphInfo.b
                        }
                        "CHEM_HEATING_CURVE" -> {
                            // Hal Değişimi: katı -> erime platosu -> sıvı -> kaynama platosu -> gaz
                            when {
                                x < 1f -> (graphInfo.a * x) + graphInfo.c
                                x in 1f..3f -> (graphInfo.a * 1f) + graphInfo.c // Erime platosu
                                x in 3f..5f -> ((graphInfo.a * 1f) + graphInfo.c) + (graphInfo.b * (x - 3f))
                                x in 5f..7.5f -> ((graphInfo.a * 1f) + graphInfo.c) + (graphInfo.b * 2f) // Kaynama platosu
                                else -> ((graphInfo.a * 1f) + graphInfo.c) + (graphInfo.b * 2f) + (graphInfo.a * (x - 7.5f))
                            }
                        }
                        "CHEM_MAXWELL" -> {
                            // Maxwell-Boltzmann Kinetic Energy distribution
                            if (x <= 0f) 0f
                            else {
                                val tFactor = graphInfo.b.coerceAtLeast(0.5f)
                                val peak = graphInfo.a * x * x * kotlin.math.exp((-tFactor * x * x * 0.15).toDouble()).toFloat()
                                peak.coerceAtLeast(0f)
                            }
                        }
                        "CHEM_EQUILIBRIUM" -> {
                            // Concentration reaching equilibrium plateau
                            val expVal = kotlin.math.exp((-0.8 * x).toDouble()).toFloat()
                            graphInfo.c + (graphInfo.a * expVal)
                        }
                        else -> (graphInfo.a * x * x) + (graphInfo.b * x) + graphInfo.c // Default PARABOLA
                    }
                }

                val curvePath = Path()
                val areaPath = Path()
                val totalSteps = 200
                val animatedStepLimit = (totalSteps * progress.value).toInt()

                var started = false
                val startX = minX
                val zeroYScreen = mathToScreenY(0f).coerceIn(0f, height)

                areaPath.moveTo(mathToScreenX(startX), zeroYScreen)

                for (i in 0..animatedStepLimit) {
                    val mathX = minX + (i.toFloat() / totalSteps) * (maxX - minX)
                    val mathY = evalFunction(mathX)

                    val screenX = mathToScreenX(mathX)
                    val screenY = mathToScreenY(mathY)

                    if (!started) {
                        curvePath.moveTo(screenX, screenY)
                        started = true
                    } else {
                        curvePath.lineTo(screenX, screenY)
                    }
                    areaPath.lineTo(screenX, screenY)
                }

                // Close shaded area path back to baseline
                val lastMathX = minX + (animatedStepLimit.toFloat() / totalSteps) * (maxX - minX)
                areaPath.lineTo(mathToScreenX(lastMathX), zeroYScreen)
                areaPath.close()

                // Highlight Area Shading if requested or for physics v-t / chem Maxwell
                val shouldHighlightArea = graphInfo.highlightAreaLabel.isNotBlank() || 
                    graphInfo.type == "PHYSICS_VT" || graphInfo.type == "CHEM_MAXWELL"
                if (shouldHighlightArea) {
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            listOf(AccentTeal.copy(alpha = 0.22f), Color.Transparent)
                        )
                    )

                    // Draw area text annotation
                    val areaLabel = if (graphInfo.highlightAreaLabel.isNotBlank()) {
                        graphInfo.highlightAreaLabel
                    } else if (graphInfo.type == "PHYSICS_VT") {
                        "Alan = Yer Değiştirme (Δx)"
                    } else {
                        "Tepkimeye Giren Tanecikler"
                    }

                    val areaPaint = Paint().apply {
                        color = android.graphics.Color.parseColor("#34D399")
                        textSize = 26f
                        isFakeBoldText = true
                        isAntiAlias = true
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        areaLabel,
                        width * 0.35f,
                        zeroYScreen - 25f,
                        areaPaint
                    )
                }

                // Draw curve gradient glow
                drawPath(
                    path = curvePath,
                    brush = Brush.horizontalGradient(
                        listOf(AccentTeal, AccentPurple, Color(0xFF38BDF8))
                    ),
                    style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                )

                // 4. Draw Key Points (roots, vertex, custom key points)
                val keyPointPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#34D399") // Green
                    textSize = 26f
                    isFakeBoldText = true
                    isAntiAlias = true
                }

                graphInfo.keyPoints.forEach { pt ->
                    val kx = mathToScreenX(pt.x)
                    val ky = mathToScreenY(pt.y)

                    if (kx in 0f..width && ky in 0f..height) {
                        // Outer glowing circle
                        drawCircle(
                            color = DoneGreen.copy(alpha = 0.35f),
                            radius = 10f,
                            center = Offset(kx, ky)
                        )
                        // Inner solid point
                        drawCircle(
                            color = DoneGreen,
                            radius = 5f,
                            center = Offset(kx, ky)
                        )

                        // Label
                        val labelText = if (pt.label.isNotBlank()) pt.label else "(%.1f, %.1f)".format(pt.x, pt.y)
                        drawContext.canvas.nativeCanvas.drawText(
                            labelText,
                            kx + 8f,
                            ky - 8f,
                            keyPointPaint
                        )
                    }
                }

                // 5. Inspect Marker (when tapped/dragged by student)
                inspectPoint?.let { pos ->
                    val mathX = screenToMathX(pos.x).coerceIn(minX, maxX)
                    val mathY = evalFunction(mathX)
                    inspectedMathCoords = mathX to mathY

                    val targetPos = Offset(mathToScreenX(mathX), mathToScreenY(mathY))

                    // Crosshair guides
                    drawLine(
                        color = Color(0x66FFFFFF),
                        start = Offset(targetPos.x, 0f),
                        end = Offset(targetPos.x, height),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f))
                    )
                    drawLine(
                        color = Color(0x66FFFFFF),
                        start = Offset(0f, targetPos.y),
                        end = Offset(width, targetPos.y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f))
                    )

                    // Target Dot
                    drawCircle(
                        color = Color(0xFFFFD700),
                        radius = 6f,
                        center = targetPos
                    )
                    drawCircle(
                        color = Color(0x44FFD700),
                        radius = 14f,
                        center = targetPos
                    )
                }
            }
        }

        if (graphInfo.extraNotes.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentTeal.copy(alpha = 0.12f))
                    .border(1.dp, AccentTeal.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "🔬 Maarif Çıkarımı: ${graphInfo.extraNotes}",
                    color = PureWhite,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
