package com.example.ai_based_medical_chatbot.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// ============================================================
// MEDASSIST AI - Material 3 Light & Dark Healthcare Themes
// Approved Palette: Deep Teal + Sage Green + Ivory + Mint + Coral
// ============================================================

val MedAssistLightColorScheme = lightColorScheme(
    primary = LightMedAssistColors.deepTeal,
    onPrimary = LightMedAssistColors.pureWhite,

    primaryContainer = LightMedAssistColors.softMint,
    onPrimaryContainer = LightMedAssistColors.darkTeal,

    secondary = LightMedAssistColors.darkTeal,
    onSecondary = LightMedAssistColors.pureWhite,

    secondaryContainer = LightMedAssistColors.mint,
    onSecondaryContainer = LightMedAssistColors.darkTeal,

    tertiary = LightMedAssistColors.sageGreen,
    onTertiary = LightMedAssistColors.pureWhite,

    background = LightMedAssistColors.ivory,
    onBackground = LightMedAssistColors.textDark,

    surface = LightMedAssistColors.pureWhite,
    onSurface = LightMedAssistColors.textDark,

    surfaceVariant = LightMedAssistColors.softMint,
    onSurfaceVariant = LightMedAssistColors.textMuted,

    outline = LightMedAssistColors.mint,
    outlineVariant = LightMedAssistColors.borderSubtle,

    error = LightMedAssistColors.statusError,
    onError = LightMedAssistColors.pureWhite,
    errorContainer = LightMedAssistColors.statusErrorBackground,
    onErrorContainer = LightMedAssistColors.statusError
)

val MedAssistDarkColorScheme = darkColorScheme(
    primary = DarkMedAssistColors.deepTeal,
    onPrimary = Color.Black,

    primaryContainer = DarkMedAssistColors.softMint,
    onPrimaryContainer = DarkMedAssistColors.darkTeal,

    secondary = DarkMedAssistColors.darkTeal,
    onSecondary = Color.Black,

    secondaryContainer = DarkMedAssistColors.mint,
    onSecondaryContainer = DarkMedAssistColors.textDark,

    tertiary = DarkMedAssistColors.sageGreen,
    onTertiary = Color.Black,

    background = DarkMedAssistColors.ivory,
    onBackground = DarkMedAssistColors.textDark,

    surface = DarkMedAssistColors.pureWhite,
    onSurface = DarkMedAssistColors.textDark,

    surfaceVariant = DarkMedAssistColors.softMint,
    onSurfaceVariant = DarkMedAssistColors.textMuted,

    outline = DarkMedAssistColors.mint,
    outlineVariant = DarkMedAssistColors.borderSubtle,

    error = DarkMedAssistColors.statusError,
    onError = Color.Black,
    errorContainer = DarkMedAssistColors.statusErrorBackground,
    onErrorContainer = DarkMedAssistColors.statusError
)

// Backward-compatible alias
val MedAssistColorScheme = MedAssistLightColorScheme

// ============================================================
// MOON & SUN VECTOR ICONS (For Dark Mode Toggling)
// ============================================================

val MoonVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "MoonVectorIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(StaticDeepTeal),
            fill = SolidColor(StaticDeepTeal.copy(alpha = 0.18f)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(21f, 12.79f)
            curveTo(20.84f, 17.61f, 16.9f, 21.49f, 12.08f, 21.49f)
            curveTo(6.87f, 21.49f, 2.65f, 17.27f, 2.65f, 12.06f)
            curveTo(2.65f, 7.24f, 6.53f, 3.3f, 11.35f, 3.14f)
            curveTo(11.89f, 3.12f, 12.28f, 3.65f, 12.08f, 4.15f)
            curveTo(11.14f, 6.44f, 11.45f, 9.15f, 13.04f, 11.13f)
            curveTo(14.63f, 13.11f, 17.22f, 14.03f, 19.67f, 13.52f)
            curveTo(20.21f, 13.41f, 20.67f, 13.88f, 20.57f, 14.41f)
            curveTo(20.73f, 13.88f, 21.05f, 13.33f, 21f, 12.79f)
            close()
        }
    }.build()
}

val SunVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "SunVectorIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(StaticDeepTeal),
            fill = SolidColor(StaticDeepTeal.copy(alpha = 0.22f)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 7f)
            curveTo(9.24f, 7f, 7f, 9.24f, 7f, 12f)
            curveTo(7f, 14.76f, 9.24f, 17f, 12f, 17f)
            curveTo(14.76f, 17f, 17f, 14.76f, 17f, 12f)
            curveTo(17f, 9.24f, 14.76f, 7f, 12f, 7f)
            close()
            moveTo(12f, 1f); lineTo(12f, 3f)
            moveTo(12f, 21f); lineTo(12f, 23f)
            moveTo(4.22f, 4.22f); lineTo(5.64f, 5.64f)
            moveTo(18.36f, 18.36f); lineTo(19.78f, 19.78f)
            moveTo(1f, 12f); lineTo(3f, 12f)
            moveTo(21f, 12f); lineTo(23f, 12f)
            moveTo(4.22f, 19.78f); lineTo(5.64f, 18.36f)
            moveTo(18.36f, 5.64f); lineTo(19.78f, 4.22f)
        }
    }.build()
}

@Composable
fun AIBasedMedicalChatbotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkMedAssistColors else LightMedAssistColors
    val colorScheme = if (darkTheme) MedAssistDarkColorScheme else MedAssistLightColorScheme

    // Sync global observable ThemeState with active darkTheme setting
    ThemeState.current = colors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colors.ivory.toArgb()
                window.navigationBarColor = colors.ivory.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalMedAssistColors provides colors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
