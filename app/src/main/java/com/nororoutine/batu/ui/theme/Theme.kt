package com.nororoutine.batu.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentPurple,
    onPrimary = BackgroundTop,
    primaryContainer = ActiveBannerBg,
    onPrimaryContainer = PureWhite,
    secondary = AccentTeal,
    onSecondary = BackgroundTop,
    secondaryContainer = ChipSelectedBg,
    onSecondaryContainer = PureWhite,
    tertiary = DoneGreen,
    onTertiary = BackgroundTop,
    background = BackgroundTop,
    onBackground = PureWhite,
    surface = GlassCardBackground,
    onSurface = PureWhite,
    surfaceVariant = GlassCardHover,
    onSurfaceVariant = SubtitleWhite,
    outline = GlassCardBorder,
    error = MissedRed,
    onError = PureWhite
)

@Composable
fun RutinTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BackgroundTop.toArgb()
            window.navigationBarColor = BackgroundBottom.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
