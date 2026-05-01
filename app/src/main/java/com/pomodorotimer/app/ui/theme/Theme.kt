package com.pomodorotimer.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

data class TimerColors(
    val activeColor: Color,
    val breakColor: Color,
    val textColor: Color,
    val progressTrackColor: Color
)

val LightTimerColors = TimerColors(
    activeColor = TimerActiveColor,
    breakColor = TimerBreakColor,
    textColor = TimerTextColor,
    progressTrackColor = TimerProgressTrackColor
)

val DarkTimerColors = TimerColors(
    activeColor = DarkPrimary,
    breakColor = DarkSecondary,
    textColor = DarkTimerTextColor,
    progressTrackColor = DarkTimerProgressTrackColor
)

val LocalTimerColors = staticCompositionLocalOf { LightTimerColors }

private val LightColorScheme = lightColorScheme(
    primary = MorandiBrownPrimary,
    onPrimary = MorandiBrownOnPrimary,
    primaryContainer = MorandiBrownPrimaryContainer,
    onPrimaryContainer = MorandiBrownOnPrimaryContainer,
    secondary = MorandiBrownSecondary,
    onSecondary = MorandiBrownOnSecondary,
    secondaryContainer = MorandiBrownSecondaryContainer,
    onSecondaryContainer = MorandiBrownOnSecondaryContainer,
    tertiary = MorandiBrownTertiary,
    onTertiary = MorandiBrownOnTertiary,
    tertiaryContainer = MorandiBrownTertiaryContainer,
    onTertiaryContainer = MorandiBrownOnTertiaryContainer,
    background = MorandiBrownBackground,
    onBackground = MorandiBrownOnBackground,
    surface = MorandiBrownSurface,
    onSurface = MorandiBrownOnSurface,
    surfaceVariant = MorandiBrownSurfaceVariant,
    onSurfaceVariant = MorandiBrownOnSurfaceVariant,
    error = MorandiBrownError,
    onError = MorandiBrownOnError,
    errorContainer = MorandiBrownErrorContainer,
    onErrorContainer = MorandiBrownOnErrorContainer,
    outline = MorandiBrownOutline,
    outlineVariant = MorandiBrownOutlineVariant,
    inverseSurface = MorandiBrownInverseSurface,
    inverseOnSurface = MorandiBrownInverseOnSurface,
    inversePrimary = MorandiBrownInversePrimary,
    surfaceTint = MorandiBrownSurfaceTint,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    inversePrimary = DarkInversePrimary,
    surfaceTint = DarkPrimary,
)

@Composable
fun PomodoroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val timerColors = if (darkTheme) DarkTimerColors else LightTimerColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalTimerColors provides timerColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
