package com.example.ai_based_medical_chatbot.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = MedicalBlue,
    onPrimary = PureWhite,

    primaryContainer = MedicalBlueLight,
    onPrimaryContainer = MedicalTextPrimary,

    secondary = MedicalTeal,
    onSecondary = PureWhite,

    secondaryContainer = MedicalTealLight,
    onSecondaryContainer = MedicalTextPrimary,

    background = MedicalBackground,
    onBackground = MedicalTextPrimary,

    surface = MedicalSurface,
    onSurface = MedicalTextPrimary,

    surfaceVariant = MedicalSurfaceVariant,
    onSurfaceVariant = MedicalTextSecondary,

    outline = MedicalBorder,

    error = MedicalError,
    onError = PureWhite
)

private val DarkColorScheme = darkColorScheme(
    primary = MedicalBlueLight,
    onPrimary = MedicalTextPrimary,

    primaryContainer = MedicalBlueDark,
    onPrimaryContainer = PureWhite,

    secondary = MedicalTealLight,
    onSecondary = MedicalTextPrimary,

    secondaryContainer = MedicalTealDark,
    onSecondaryContainer = PureWhite,

    background = DarkBackground,
    onBackground = DarkTextPrimary,

    surface = DarkSurface,
    onSurface = DarkTextPrimary,

    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,

    outline = MedicalTextSecondary,

    error = MedicalError,
    onError = PureWhite
)

@Composable
fun AIBasedMedicalChatbotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}