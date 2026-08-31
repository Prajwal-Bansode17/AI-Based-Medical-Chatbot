package com.example.ai_based_medical_chatbot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MidnightColorScheme = darkColorScheme(
    primary = MidnightPrimary,
    onPrimary = Color.White,

    primaryContainer = MidnightSurfaceVariant,
    onPrimaryContainer = MidnightText,

    secondary = MidnightSecondary,
    onSecondary = MidnightBackground,

    secondaryContainer = MidnightSurfaceVariant,
    onSecondaryContainer = MidnightText,

    tertiary = MidnightSuccess,
    onTertiary = MidnightBackground,

    background = MidnightBackground,
    onBackground = MidnightText,

    surface = MidnightSurface,
    onSurface = MidnightText,

    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = MidnightTextSecondary,

    outline = MidnightOutline
)

@Composable
fun AIBasedMedicalChatbotTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MidnightColorScheme,
        typography = Typography,
        content = content
    )
}