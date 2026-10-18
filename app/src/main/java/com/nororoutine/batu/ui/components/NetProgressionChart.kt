package com.nororoutine.batu.ui.components

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nororoutine.batu.model.YksExamRecord
import com.nororoutine.batu.ui.theme.AccentPurple
import com.nororoutine.batu.ui.theme.AccentTeal
import com.nororoutine.batu.ui.theme.DoneGreen
import com.nororoutine.batu.ui.theme.GlassCardBorderSubtle
import com.nororoutine.batu.ui.theme.PureWhite
import com.nororoutine.batu.ui.theme.SubtitleWhite

@Composable
fun NetProgressionChart(
    exams: List<YksExamRecord>,
    selectedExam: YksExamRecord?,
    onSelectExam: (YksExamRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    if (exams.isEmpty()) return

    // Chronological order for progression chart (oldest to newest)
    val chronologicalExams = remember(exams) { exams.reversed() }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(exams.size) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 900))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F0D22))
            .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(AccentTeal.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TrendingUp,
                            contentDescription = null,
                            tint = AccentTeal,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Net Gelişim & Takip Grafiği",
                            color = PureWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "2028 Maarif Hedefine Göre Seyir",
                            color = SubtitleWhite,
                            fontSize = 11.sp
                        )
                    }
                }

                val latestNet = chronologicalExams.lastOrNull()?.totalNet ?: 0f
                Text(
                    text = "Son: %.1f Net".format(latestNet),
                    color = AccentTeal,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Line Chart Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF14122E))
            ) {
                val width = size.width
                val height = size.height

                val paddingX = 40f
                val paddingY = 24f
                val chartWidth = width - (paddingX * 2)
                val chartHeight = height - (paddingY * 2)

                val maxNet = 120f
                val minNet = 0f

                fun getX(index: Int): Float {
                    return if (chronologicalExams.size <= 1) {
                        width / 2f
                    } else {
                        paddingX + (index.toFloat() / (chronologicalExams.size - 1)) * chartWidth
                    }
                }

                fun getY(net: Float): Float {
                    val clamped = net.coerceIn(minNet, maxNet)
                    return height - paddingY - ((clamped - minNet) / (maxNet - minNet)) * chartHeight
                }

                // Horizontal Target Line (e.g. 90 Net target)
                val targetY = getY(90f)
                drawLine(
                    color = Color(0x66F59E0B),
                    start = Offset(paddingX, targetY),
                    end = Offset(width - paddingX, targetY),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )

                val paint = Paint().apply {
                    color = android.graphics.Color.parseColor("#F59E0B")
                    textSize = 24f
                    isAntiAlias = true
                }
                drawContext.canvas.nativeCanvas.drawText("Hedef 90 Net", width - paddingX - 110f, targetY - 6f, paint)

                // Build Curve & Fill Path
                if (chronologicalExams.isNotEmpty()) {
                    val linePath = Path()
                    val fillPath = Path()

                    val firstX = getX(0)
                    val firstY = getY(chronologicalExams[0].totalNet)

                    linePath.moveTo(firstX, firstY)
                    fillPath.moveTo(firstX, height - paddingY)
                    fillPath.lineTo(firstX, firstY)

                    for (i in 1 until chronologicalExams.size) {
                        val prevX = getX(i - 1)
                        val prevY = getY(chronologicalExams[i - 1].totalNet)
                        val currX = getX(i)
                        val currY = getY(chronologicalExams[i].totalNet)

                        // Smooth bezier curve control points
                        val midX = (prevX + currX) / 2f
                        linePath.cubicTo(midX, prevY, midX, currY, currX, currY)
                        fillPath.cubicTo(midX, prevY, midX, currY, currX, currY)
                    }

                    fillPath.lineTo(getX(chronologicalExams.lastIndex), height - paddingY)
                    fillPath.close()

                    // Draw gradient area under curve
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            listOf(
                                AccentTeal.copy(alpha = 0.25f * animProgress.value),
                                Color.Transparent
                            )
                        )
                    )

                    // Draw stroke line
                    drawPath(
                        path = linePath,
                        brush = Brush.horizontalGradient(listOf(AccentTeal, AccentPurple, DoneGreen)),
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                    )

                    // Draw Points
                    val pointPaint = Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 24f
                        isFakeBoldText = true
                        isAntiAlias = true
                    }

                    chronologicalExams.forEachIndexed { idx, exam ->
                        val px = getX(idx)
                        val py = getY(exam.totalNet)
                        val isSel = selectedExam?.id == exam.id

                        // Glowing circle
                        drawCircle(
                            color = if (isSel) DoneGreen else AccentTeal,
                            radius = if (isSel) 6f else 4.5f,
                            center = Offset(px, py)
                        )
                        drawCircle(
                            color = if (isSel) DoneGreen.copy(alpha = 0.4f) else AccentTeal.copy(alpha = 0.3f),
                            radius = if (isSel) 12f else 8f,
                            center = Offset(px, py)
                        )

                        // Net text badge
                        drawContext.canvas.nativeCanvas.drawText(
                            "%.1f".format(exam.totalNet),
                            px - 16f,
                            py - 12f,
                            pointPaint
                        )
                    }
                }
            }

            // Subject breakdown bar chart for selected exam
            if (selectedExam != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "${selectedExam.title} - Ders Net Dağılımı",
                    color = SubtitleWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                val subjectColors = mapOf(
                    "Türkçe" to Color(0xFF38BDF8),
                    "Temel Matematik" to Color(0xFF818CF8),
                    "Fen Bilimleri" to Color(0xFF34D399),
                    "Sosyal Bilimler" to Color(0xFFFBBF24)
                )

                selectedExam.results.forEach { res ->
                    val color = subjectColors[res.subjectName] ?: AccentTeal
                    val maxPossible = 40f
                    val ratio = (res.net / maxPossible).coerceIn(0f, 1f)

                    Column(modifier = Modifier.padding(vertical = 3.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = res.subjectName, color = PureWhite, fontSize = 11.5.sp)
                            Text(
                                text = "%.2f Net".format(res.net),
                                color = color,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0x33FFFFFF))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(ratio)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}
