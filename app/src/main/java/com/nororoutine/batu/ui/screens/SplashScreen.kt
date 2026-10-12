package com.nororoutine.batu.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

// Living aurora colors
private val AuroraColors = listOf(
    Color(0xFF0F0C29),
    Color(0xFF302B63),
    Color(0xFF24243E),
    Color(0xFF7B5AF7),
    Color(0xFF7CF4DE)
)

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // Overall screen fade out at the end
    val screenAlpha = remember { Animatable(1f) }

    // Text reveal animations: spring scale + fade in
    val textAlpha = remember { Animatable(0f) }
    val textScale = remember { Animatable(0.7f) }

    // Shimmer/glow effect for the text
    val textGlowAlpha = remember { Animatable(0.3f) }

    // Infinite breathing/shifting animation for the background aurora & orbs
    val infiniteTransition = rememberInfiniteTransition(label = "auroraTransition")

    // Slow oscillating float values for gradient and floating orbs
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraShift"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    // Sequence coordinator
    LaunchedEffect(Unit) {
        // Small initial pause for smooth display
        delay(150)

        // Text spring entrance: scale up and fade in
        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        delay(150)
        textScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        // Subtle pulsing glow on text
        textGlowAlpha.animateTo(
            targetValue = 0.9f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    // After 2.1 seconds, smoothly fade out everything and complete transition around 2.5s
    LaunchedEffect(Unit) {
        delay(2000)
        screenAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha.value)
    ) {
        // 1. LIVING, BREATHING AURORA GRADIENT BACKGROUND
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Calculate moving gradient start/end coordinates based on animProgress
            val angle = animProgress * 2f * Math.PI.toFloat()
            val startX = width * (0.2f + 0.3f * cos(angle))
            val startY = height * (0.1f + 0.3f * sin(angle))
            val endX = width * (0.8f - 0.2f * cos(angle))
            val endY = height * (0.9f - 0.2f * sin(angle))

            // Background dynamic aurora mesh
            drawRect(
                brush = Brush.linearGradient(
                    colors = AuroraColors,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY)
                )
            )

            // 2. AMBIENT ORBS: 3 large blurred radial gradient orbs floating & pulsing
            // Orb 1: Violet/Purple glowing orb floating top-left to center
            val orb1Center = Offset(
                x = width * (0.25f + 0.15f * sin(animProgress * 3.14f)),
                y = height * (0.3f + 0.12f * cos(animProgress * 3.14f))
            )
            val orb1Radius = width * 0.65f * pulseScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF7B5AF7).copy(alpha = 0.45f),
                        Color(0xFF302B63).copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = orb1Center,
                    radius = orb1Radius
                ),
                center = orb1Center,
                radius = orb1Radius
            )

            // Orb 2: Teal/Cyan luminous orb floating bottom-right
            val orb2Center = Offset(
                x = width * (0.8f - 0.18f * cos(animProgress * 3.14f)),
                y = height * (0.65f - 0.15f * sin(animProgress * 3.14f))
            )
            val orb2Radius = width * 0.55f * (2.1f - pulseScale)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF7CF4DE).copy(alpha = 0.38f),
                        Color(0xFF24243E).copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = orb2Center,
                    radius = orb2Radius
                ),
                center = orb2Center,
                radius = orb2Radius
            )

            // Orb 3: Central mystic indigo pulse orb
            val orb3Center = Offset(
                x = width * (0.5f + 0.1f * sin(animProgress * 6.28f)),
                y = height * (0.5f - 0.08f * cos(animProgress * 6.28f))
            )
            val orb3Radius = width * 0.75f * (0.9f + pulseScale * 0.2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF7B5AF7).copy(alpha = 0.25f),
                        Color(0xFF0F0C29).copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = orb3Center,
                    radius = orb3Radius
                ),
                center = orb3Center,
                radius = orb3Radius
            )
        }

        // 3. ELEGANT GREETING TEXT IN THE CENTER
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            val dynamicGlowColor = Color(0xFF7CF4DE).copy(
                alpha = 0.4f * (0.7f + 0.3f * sin(shimmerOffset * 6.28f))
            )

            Text(
                text = "Hoşgeldiniz efendim",
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 32.sp,
                    letterSpacing = 1.2.sp,
                    color = Color.White,
                    shadow = Shadow(
                        color = dynamicGlowColor,
                        offset = Offset(0f, 0f),
                        blurRadius = 24f * textGlowAlpha.value
                    )
                ),
                modifier = Modifier
                    .scale(textScale.value)
                    .alpha(textAlpha.value)
            )
        }
    }
}
