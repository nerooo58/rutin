package com.nororoutine.batu.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nororoutine.batu.ui.theme.AccentTeal

@Composable
fun PulsingDot(
    modifier: Modifier = Modifier,
    color: Color = AccentTeal,
    dotSize: Dp = 8.dp,
    pulseMaxScale: Float = 2.4f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = pulseMaxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier.size(dotSize * pulseMaxScale),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring
        Canvas(modifier = Modifier.size(dotSize * pulseMaxScale)) {
            val center = this.center
            val radius = (dotSize.toPx() / 2f) * scale
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius,
                center = center
            )
        }
        // Inner solid dot
        Canvas(modifier = Modifier.size(dotSize)) {
            val center = this.center
            val radius = dotSize.toPx() / 2f
            drawCircle(
                color = color,
                radius = radius,
                center = center
            )
        }
    }
}
