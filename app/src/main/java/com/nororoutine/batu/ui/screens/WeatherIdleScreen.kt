package com.nororoutine.batu.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.nororoutine.batu.network.WeatherService
import com.nororoutine.batu.ui.components.GlassCard
import com.nororoutine.batu.ui.components.PulsingDot
import com.nororoutine.batu.ui.components.springPress
import com.nororoutine.batu.ui.theme.AccentPurple
import com.nororoutine.batu.ui.theme.AccentTeal
import com.nororoutine.batu.ui.theme.GlassCardBackground
import com.nororoutine.batu.ui.theme.GlassCardBorderSubtle
import com.nororoutine.batu.ui.theme.MutedWhite
import com.nororoutine.batu.ui.theme.PureWhite
import com.nororoutine.batu.ui.theme.SubtitleWhite
import kotlinx.coroutines.launch
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
    onViewRoutinesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
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

        Spacer(modifier = Modifier.height(12.dp))

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

        Spacer(modifier = Modifier.height(12.dp))

        // Weather Details Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(GlassCardBackground)
                .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x1F7CF4DE)),
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
                        Text(
                            text = "${weatherInfo.temperature.toInt()}°C",
                            color = PureWhite,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = weatherInfo.condition.label,
                            color = AccentTeal,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
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
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Routine Carousel Navigation Affordance
        if (totalTodayRoutines > 0) {
            Button(
                onClick = onViewRoutinesClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .springPress { onViewRoutinesClick() },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPurple,
                    contentColor = PureWhite
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.ViewCarousel,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Günün Rutinleri ($totalTodayRoutines)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x1F1E3250))
                    .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Şu an planlanmış aktif rutin yok. Dinlenmenin tadını çıkarın.",
                    color = SubtitleWhite,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
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

        // Sun / Moon based on time
        if (timeOfDay == TimeOfDay.NIGHT) {
            // Crescent Moon
            val moon = Path().apply {
                moveTo(w * 0.65f, h * 0.25f)
                cubicTo(w * 0.45f, h * 0.35f, w * 0.48f, h * 0.65f, w * 0.72f, h * 0.7f)
                cubicTo(w * 0.58f, h * 0.62f, w * 0.56f, h * 0.38f, w * 0.65f, h * 0.25f)
                close()
            }
            drawPath(moon, color = Color(0xFFFDE047))
            // Stars
            drawCircle(Color.White, radius = w * 0.02f, center = Offset(w * 0.25f, h * 0.35f))
            drawCircle(Color.White, radius = w * 0.015f, center = Offset(w * 0.38f, h * 0.22f))
            drawCircle(Color.White, radius = w * 0.025f, center = Offset(w * 0.32f, h * 0.52f))
        } else {
            // Sun with warm radiant glow
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFFDE047), Color(0xFFF59E0B), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.45f),
                    radius = w * 0.35f
                )
            )
            drawCircle(
                color = Color(0xFFF59E0B),
                radius = w * 0.16f,
                center = Offset(w * 0.5f, h * 0.45f)
            )
        }

        // Overlay Weather Clouds/Rain/Snow/Lightning
        when (condition) {
            WeatherCondition.CLEAR -> {
                // Gentle horizon / birds
                drawLine(Color(0x66FFFFFF), Offset(w * 0.2f, h * 0.8f), Offset(w * 0.8f, h * 0.8f), strokeWidth = w * 0.02f, cap = StrokeCap.Round)
            }
            WeatherCondition.CLOUDY -> {
                drawCloud(w * 0.5f, h * 0.55f, w * 0.7f, Color(0xCCFFFFFF))
                drawCloud(w * 0.35f, h * 0.62f, w * 0.5f, Color(0x9994A3B8))
            }
            WeatherCondition.RAINY -> {
                drawCloud(w * 0.5f, h * 0.45f, w * 0.7f, Color(0xDDE2E8F0))
                // Raindrops
                for (i in 0..4) {
                    val dropX = w * (0.32f + i * 0.09f)
                    val dropY = h * 0.68f + (i % 2) * h * 0.06f
                    drawLine(Color(0xFF38BDF8), Offset(dropX, dropY), Offset(dropX - w * 0.04f, dropY + h * 0.08f), strokeWidth = w * 0.025f, cap = StrokeCap.Round)
                }
            }
            WeatherCondition.STORMY -> {
                drawCloud(w * 0.5f, h * 0.42f, w * 0.75f, Color(0xFF475569))
                // Lightning bolt
                val bolt = Path().apply {
                    moveTo(w * 0.52f, h * 0.55f)
                    lineTo(w * 0.44f, h * 0.68f)
                    lineTo(w * 0.54f, h * 0.68f)
                    lineTo(w * 0.46f, h * 0.84f)
                }
                drawPath(bolt, color = Color(0xFFFACC15), style = Stroke(w * 0.035f, cap = StrokeCap.Round))
            }
            WeatherCondition.SNOWY -> {
                drawCloud(w * 0.5f, h * 0.45f, w * 0.7f, Color(0xEEF1F5F9))
                // Snowflakes
                for (i in 0..5) {
                    val snowX = w * (0.28f + (i * 0.09f))
                    val snowY = h * 0.66f + ((i % 3) * h * 0.05f)
                    drawCircle(Color.White, radius = w * 0.022f, center = Offset(snowX, snowY))
                }
            }
        }
    }
}

private fun DrawScope.drawCloud(centerX: Float, centerY: Float, width: Float, color: Color) {
    val h = width * 0.45f
    // Base pill
    drawRoundRect(color, Offset(centerX - width / 2f, centerY), Size(width, h * 0.7f), CornerRadius(h * 0.35f))
    // Puff circles
    drawCircle(color, radius = width * 0.22f, center = Offset(centerX - width * 0.16f, centerY + h * 0.1f))
    drawCircle(color, radius = width * 0.28f, center = Offset(centerX + width * 0.08f, centerY))
}
