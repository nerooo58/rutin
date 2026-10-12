package com.nororoutine.batu.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nororoutine.batu.network.WeatherCondition
import com.nororoutine.batu.network.WeatherInfo
import com.nororoutine.batu.ui.components.PulsingDot
import com.nororoutine.batu.ui.theme.AccentPurple
import com.nororoutine.batu.ui.theme.AccentTeal
import com.nororoutine.batu.ui.theme.GlassCardBackground
import com.nororoutine.batu.ui.theme.GlassCardBorderSubtle
import com.nororoutine.batu.ui.theme.MutedWhite
import com.nororoutine.batu.ui.theme.PureWhite
import com.nororoutine.batu.ui.theme.SubtitleWhite
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TimeOfDay(
    val title: String,
    val subtitle: String,
    val gradientColors: List<Color>,
    val accentColor: Color
) {
    DAWN(
        title = "Şafak Vakti",
        subtitle = "Yeni bir gün uyanıyor, tazelenin",
        gradientColors = listOf(Color(0xFF2E1065), Color(0xFF6B21A8), Color(0xFFEA580C)),
        accentColor = Color(0xFFFB923C)
    ),
    MORNING(
        title = "Günaydın",
        subtitle = "Güne enerjik ve kararlı başlayın",
        gradientColors = listOf(Color(0xFF0F172A), Color(0xFF0E7490), Color(0xFF14B8A6)),
        accentColor = Color(0xFF7CF4DE)
    ),
    AFTERNOON(
        title = "İyi Günler",
        subtitle = "Odaklanma ve ritmi koruma vakti",
        gradientColors = listOf(Color(0xFF0C192C), Color(0xFF1D4ED8), Color(0xFF38BDF8)),
        accentColor = Color(0xFF60A5FA)
    ),
    EVENING(
        title = "İyi Akşamlar",
        subtitle = "Günün yorgunluğunu hafifletin",
        gradientColors = listOf(Color(0xFF1E1B4B), Color(0xFF4C1D95), Color(0xFFBE185D)),
        accentColor = Color(0xFFA78BFA)
    ),
    NIGHT(
        title = "İyi Geceler",
        subtitle = "Huzur ve dinlenme zamanı",
        gradientColors = listOf(Color(0xFF050515), Color(0xFF111827), Color(0xFF1E1B4B)),
        accentColor = Color(0xFF818CF8)
    )
}

fun getCurrentTimeOfDay(): TimeOfDay {
    val hour = LocalTime.now().hour
    return when (hour) {
        in 5..7 -> TimeOfDay.DAWN
        in 8..11 -> TimeOfDay.MORNING
        in 12..16 -> TimeOfDay.AFTERNOON
        in 17..20 -> TimeOfDay.EVENING
        else -> TimeOfDay.NIGHT
    }
}

@Composable
fun WeatherIdleScreen(
    weatherInfo: WeatherInfo,
    onRefreshWeather: () -> Unit,
    totalTodayRoutines: Int,
    completedTodayRoutines: Int = 0,
    todayPercent: Int = 0,
    currentStreak: Int = 0,
    onViewRoutinesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val timeOfDay = remember { getCurrentTimeOfDay() }
    val now = remember { LocalTime.now() }
    val today = remember { LocalDate.now() }
    val turkishLocale = remember { Locale("tr", "TR") }

    val formattedTime = remember(now) {
        now.format(DateTimeFormatter.ofPattern("HH:mm"))
    }
    val formattedDate = remember(today) {
        today.format(DateTimeFormatter.ofPattern("d MMMM yyyy", turkishLocale))
    }
    val dayOfWeek = remember(today) {
        today.format(DateTimeFormatter.ofPattern("EEEE", turkishLocale))
    }

    var permissionRequested by remember { mutableStateOf(false) }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onRefreshWeather()
    }

    // Determine location text from geocoded district & city
    val locationText = remember(weatherInfo.district, weatherInfo.city) {
        when {
            weatherInfo.district.isNotEmpty() && weatherInfo.city.isNotEmpty() ->
                "${weatherInfo.district}, ${weatherInfo.city}"
            weatherInfo.city.isNotEmpty() -> weatherInfo.city
            weatherInfo.district.isNotEmpty() -> weatherInfo.district
            else -> "Konum alınıyor..."
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Top Greeting Banner
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x22FFFFFF))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                PulsingDot(color = timeOfDay.accentColor, dotSize = 7.dp, pulseMaxScale = 2.0f)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = timeOfDay.title,
                    color = PureWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = formattedTime,
                color = PureWhite,
                fontSize = 54.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp
            )

            Text(
                text = "$dayOfWeek, $formattedDate",
                color = SubtitleWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = timeOfDay.subtitle,
                color = MutedWhite,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Center Atmospheric SVG Weather & Time Illustration
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            timeOfDay.gradientColors[1].copy(alpha = 0.5f),
                            timeOfDay.gradientColors[0].copy(alpha = 0.8f)
                        )
                    )
                )
                .border(
                    1.dp,
                    timeOfDay.accentColor.copy(alpha = 0.35f),
                    RoundedCornerShape(32.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            WeatherSvgIllustration(
                condition = weatherInfo.condition,
                timeOfDay = timeOfDay,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Weather Details Card with City/District location info & Right Compact Routine Pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xE6131D31))
                .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Side: Weather Icon + Temperature + Condition + City/District Location
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0x227CF4DE))
                            .border(1.dp, Color(0x337CF4DE), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Thermostat,
                            contentDescription = null,
                            tint = AccentTeal,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${weatherInfo.temperature.toInt()}°C",
                                color = PureWhite,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = weatherInfo.condition.label,
                                color = AccentTeal,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        // Reverse Geocoded Location Info (City & District)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                if (!permissionRequested) {
                                    permissionRequested = true
                                    locationLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                } else {
                                    onRefreshWeather()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = locationText,
                                color = SubtitleWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Right Side: Routine Count Compact Pill & Refresh Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isAllCompleted = totalTodayRoutines > 0 && completedTodayRoutines == totalTodayRoutines
                    val completionColor = getCompletionColor(todayPercent, totalTodayRoutines)
                    val pillBg = when {
                        totalTodayRoutines > 0 -> completionColor.copy(alpha = 0.22f)
                        else -> Color(0x22FFFFFF)
                    }
                    val pillBorder = when {
                        totalTodayRoutines > 0 -> completionColor.copy(alpha = 0.75f)
                        else -> Color(0x22FFFFFF)
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (currentStreak > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color(0x33F59E0B))
                                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "🔥 $currentStreak gün",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Günün Rutinleri Pill Button (e.g. "4/4 (%100)")
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(pillBg)
                                .border(1.dp, pillBorder, RoundedCornerShape(999.dp))
                                .clickable { onViewRoutinesClick() }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isAllCompleted) Icons.Rounded.Check else Icons.Rounded.ViewCarousel,
                                    contentDescription = null,
                                    tint = if (totalTodayRoutines > 0) completionColor else PureWhite,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (totalTodayRoutines > 0) "$completedTodayRoutines/$totalTodayRoutines (%$todayPercent)" else "Rutinler",
                                    color = PureWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Refresh Button
                    IconButton(
                        onClick = {
                            if (!permissionRequested) {
                                permissionRequested = true
                                locationLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            } else {
                                onRefreshWeather()
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Hava Durumunu Güncelle",
                            tint = PureWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
fun WeatherSvgIllustration(
    condition: WeatherCondition,
    timeOfDay: TimeOfDay,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Background Atmospheric Stars / Sunlight Rays
        if (timeOfDay == TimeOfDay.NIGHT) {
            // Stars in the night sky
            drawCircle(Color.White, radius = w * 0.02f, center = Offset(w * 0.22f, h * 0.28f))
            drawCircle(Color(0xFFE2E8F0), radius = w * 0.014f, center = Offset(w * 0.38f, h * 0.18f))
            drawCircle(Color.White, radius = w * 0.022f, center = Offset(w * 0.30f, h * 0.48f))
            drawCircle(Color(0xFFFEF08A), radius = w * 0.016f, center = Offset(w * 0.78f, h * 0.36f))
            drawCircle(Color.White, radius = w * 0.012f, center = Offset(w * 0.84f, h * 0.22f))

            // Realistic Crescent Moon with Crater details
            val moon = Path().apply {
                moveTo(w * 0.65f, h * 0.22f)
                cubicTo(w * 0.42f, h * 0.34f, w * 0.45f, h * 0.66f, w * 0.72f, h * 0.72f)
                cubicTo(w * 0.56f, h * 0.64f, w * 0.54f, h * 0.36f, w * 0.65f, h * 0.22f)
                close()
            }
            drawPath(moon, color = Color(0xFFFDE047))
            // Moon crater dots
            drawCircle(Color(0xFFEAB308).copy(alpha = 0.5f), radius = w * 0.02f, center = Offset(w * 0.57f, h * 0.42f))
            drawCircle(Color(0xFFEAB308).copy(alpha = 0.5f), radius = w * 0.015f, center = Offset(w * 0.60f, h * 0.54f))
        } else {
            // Sun with radiant aura and defined rays
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFFDE047).copy(alpha = 0.7f), Color(0xFFF59E0B).copy(alpha = 0.3f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.44f),
                    radius = w * 0.38f
                )
            )
            // Sun body
            drawCircle(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFFFEF08A), Color(0xFFF59E0B)),
                    startY = h * 0.30f,
                    endY = h * 0.58f
                ),
                radius = w * 0.16f,
                center = Offset(w * 0.5f, h * 0.44f)
            )
            // Sun rays
            for (i in 0..7) {
                val angle = Math.toRadians((i * 45.0))
                val r1 = w * 0.19f
                val r2 = w * 0.26f
                val x1 = (w * 0.5f + Math.cos(angle) * r1).toFloat()
                val y1 = (h * 0.44f + Math.sin(angle) * r1).toFloat()
                val x2 = (w * 0.5f + Math.cos(angle) * r2).toFloat()
                val y2 = (h * 0.44f + Math.sin(angle) * r2).toFloat()
                drawLine(
                    color = Color(0xFFFBBF24),
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = w * 0.025f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Overlay Weather Clouds/Rain/Snow/Lightning
        when (condition) {
            WeatherCondition.CLEAR -> {
                // Clear sky horizon with flying birds
                val bird1 = Path().apply {
                    moveTo(w * 0.24f, h * 0.74f)
                    cubicTo(w * 0.28f, h * 0.70f, w * 0.32f, h * 0.70f, w * 0.36f, h * 0.74f)
                    cubicTo(w * 0.40f, h * 0.70f, w * 0.44f, h * 0.70f, w * 0.48f, h * 0.74f)
                }
                drawPath(bird1, color = Color(0x99FFFFFF), style = Stroke(w * 0.02f, cap = StrokeCap.Round))

                val bird2 = Path().apply {
                    moveTo(w * 0.60f, h * 0.68f)
                    cubicTo(w * 0.63f, h * 0.65f, w * 0.66f, h * 0.65f, w * 0.69f, h * 0.68f)
                    cubicTo(w * 0.72f, h * 0.65f, w * 0.75f, h * 0.65f, w * 0.78f, h * 0.68f)
                }
                drawPath(bird2, color = Color(0x66FFFFFF), style = Stroke(w * 0.016f, cap = StrokeCap.Round))
            }

            WeatherCondition.CLOUDY -> {
                // Background dark puff
                drawCloudDetailed(w * 0.36f, h * 0.56f, w * 0.54f, Color(0x9964748B))
                // Foreground fluffy white cloud
                drawCloudDetailed(w * 0.52f, h * 0.52f, w * 0.70f, Color(0xF2F8FAFC))
            }

            WeatherCondition.RAINY -> {
                // Storm cloud
                drawCloudDetailed(w * 0.5f, h * 0.42f, w * 0.72f, Color(0xEE64748B))
                // Detailed angled teardrop raindrops
                for (i in 0..5) {
                    val dropX = w * (0.26f + i * 0.09f)
                    val dropY = h * 0.64f + (i % 3) * h * 0.05f
                    val dropPath = Path().apply {
                        moveTo(dropX, dropY)
                        cubicTo(dropX - w * 0.025f, dropY + h * 0.05f, dropX - w * 0.045f, dropY + h * 0.07f, dropX - w * 0.03f, dropY + h * 0.08f)
                        cubicTo(dropX - w * 0.015f, dropY + h * 0.09f, dropX + w * 0.005f, dropY + h * 0.07f, dropX, dropY)
                        close()
                    }
                    drawPath(dropPath, color = Color(0xFF38BDF8))
                }
            }

            WeatherCondition.STORMY -> {
                // Deep dark thundercloud
                drawCloudDetailed(w * 0.5f, h * 0.38f, w * 0.76f, Color(0xFF334155))
                // Dynamic zig-zag Lightning Bolt with glow
                val boltGlow = Path().apply {
                    moveTo(w * 0.54f, h * 0.50f)
                    lineTo(w * 0.44f, h * 0.64f)
                    lineTo(w * 0.55f, h * 0.64f)
                    lineTo(w * 0.42f, h * 0.85f)
                }
                drawPath(boltGlow, color = Color(0x66FDE047), style = Stroke(w * 0.07f, cap = StrokeCap.Round))
                drawPath(boltGlow, color = Color(0xFFFACC15), style = Stroke(w * 0.035f, cap = StrokeCap.Round))

                // Raindrops under storm
                for (i in 0..3) {
                    val dropX = w * (0.28f + i * 0.15f)
                    val dropY = h * 0.68f
                    drawLine(Color(0xFF38BDF8), Offset(dropX, dropY), Offset(dropX - w * 0.03f, dropY + h * 0.06f), strokeWidth = w * 0.02f, cap = StrokeCap.Round)
                }
            }

            WeatherCondition.SNOWY -> {
                // Gentle winter cloud
                drawCloudDetailed(w * 0.5f, h * 0.42f, w * 0.70f, Color(0xEE94A3B8))
                // 6-pointed geometric snowflakes
                for (i in 0..4) {
                    val snowX = w * (0.28f + (i * 0.11f))
                    val snowY = h * 0.66f + ((i % 3) * h * 0.06f)
                    drawDetailedSnowflake(snowX, snowY, w * 0.045f, Color.White)
                }
            }
        }
    }
}

private fun DrawScope.drawDetailedSnowflake(cx: Float, cy: Float, radius: Float, color: Color) {
    for (a in 0..2) {
        val rad = Math.toRadians((a * 60.0))
        val dx = (Math.cos(rad) * radius).toFloat()
        val dy = (Math.sin(rad) * radius).toFloat()
        drawLine(color, Offset(cx - dx, cy - dy), Offset(cx + dx, cy + dy), strokeWidth = radius * 0.3f, cap = StrokeCap.Round)
    }
    drawCircle(color, radius = radius * 0.35f, center = Offset(cx, cy))
}

private fun DrawScope.drawCloudDetailed(centerX: Float, centerY: Float, width: Float, color: Color) {
    val h = width * 0.48f
    // Base rounded container
    drawRoundRect(
        color = color,
        topLeft = Offset(centerX - width / 2f, centerY + h * 0.1f),
        size = Size(width, h * 0.65f),
        cornerRadius = CornerRadius(h * 0.35f)
    )
    // Left puff
    drawCircle(color, radius = width * 0.22f, center = Offset(centerX - width * 0.20f, centerY + h * 0.15f))
    // Center big puff
    drawCircle(color, radius = width * 0.28f, center = Offset(centerX + width * 0.02f, centerY + h * 0.02f))
    // Right puff
    drawCircle(color, radius = width * 0.20f, center = Offset(centerX + width * 0.24f, centerY + h * 0.20f))
}
