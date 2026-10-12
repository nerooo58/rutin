package com.nororoutine.batu.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.nororoutine.batu.ui.theme.AccentPurple
import com.nororoutine.batu.ui.theme.AccentTeal
import com.nororoutine.batu.ui.theme.BackgroundBottom
import com.nororoutine.batu.ui.theme.BackgroundMid
import com.nororoutine.batu.ui.theme.BackgroundTop

@Composable
fun AmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        BackgroundTop,
                        BackgroundMid,
                        BackgroundBottom
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 2000f)
                )
            )
    ) {
        // Subtle ambient orbs in background (blurred radial gradients)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Middle-left purple orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AccentPurple.copy(alpha = 0.16f),
                        AccentPurple.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.1f, height * 0.35f),
                    radius = width * 0.55f
                ),
                center = Offset(width * 0.1f, height * 0.35f),
                radius = width * 0.55f
            )

            // Top-right teal ambient orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AccentTeal.copy(alpha = 0.12f),
                        AccentTeal.copy(alpha = 0.03f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.85f, height * 0.15f),
                    radius = width * 0.45f
                ),
                center = Offset(width * 0.85f, height * 0.15f),
                radius = width * 0.45f
            )

            // Bottom-right soft purple glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AccentPurple.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.75f, height * 0.8f),
                    radius = width * 0.5f
                ),
                center = Offset(width * 0.75f, height * 0.8f),
                radius = width * 0.5f
            )
        }

        content()
    }
}
