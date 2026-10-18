package com.example.ai_based_medical_chatbot.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// ============================================================
// MEDASSIST AI - Material 3 Light Healthcare Theme
// Approved Palette: Deep Teal + Sage Green + Ivory + Mint + Coral
// ============================================================

val MedAssistColorScheme = lightColorScheme(
    primary = DeepTeal,
    onPrimary = PureWhite,

    primaryContainer = SoftMint,
    onPrimaryContainer = DarkTeal,

    secondary = DarkTeal,
    onSecondary = PureWhite,

    secondaryContainer = Mint,
    onSecondaryContainer = DarkTeal,

    tertiary = SageGreen,
    onTertiary = PureWhite,

    background = Ivory,
    onBackground = TextDark,

    surface = PureWhite,
    onSurface = TextDark,

    surfaceVariant = SoftMint,
    onSurfaceVariant = TextMuted,

    outline = Mint,
    outlineVariant = BorderSubtle,

    error = StatusError,
    onError = PureWhite,
    errorContainer = StatusErrorBackground,
    onErrorContainer = StatusError
)

@Composable
fun AIBasedMedicalChatbotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Consistent bright, clean healthcare experience across all systems
    MaterialTheme(
        colorScheme = MedAssistColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}