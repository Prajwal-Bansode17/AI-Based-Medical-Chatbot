package com.example.ai_based_medical_chatbot.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ============================================================
// MEDASSIST AI - Global Healthcare Design Tokens
// Approved Palette: Deep Teal + Sage Green + Ivory + Mint + Coral
// Supports seamless, immediate, application-wide Light & Dark Themes
// ============================================================

/**
 * Encapsulates the complete MedAssist AI color token system
 * for either Light Mode or Dark Mode.
 */
class MedAssistColors(
    val isDark: Boolean,
    val deepTeal: Color,
    val darkTeal: Color,
    val sageGreen: Color,
    val ivory: Color,
    val mint: Color,
    val softMint: Color,
    val pureWhite: Color,
    val pureBlack: Color,
    val textDark: Color,
    val textMuted: Color,
    val textLight: Color,
    val borderLight: Color,
    val borderSubtle: Color,
    val dividerColor: Color,
    val coralAccent: Color,
    val coralAccentSoft: Color,
    val coralAccentBorder: Color,
    val statusSuccess: Color,
    val statusWarning: Color,
    val statusError: Color,
    val statusErrorBackground: Color,
    val statusErrorBorder: Color,
    val appBackground: Color,
    val appSurface: Color,
    val appSurfaceVariant: Color
)

val LightMedAssistColors = MedAssistColors(
    isDark = false,
    deepTeal = Color(0xFF087F8C),
    darkTeal = Color(0xFF075E66),
    sageGreen = Color(0xFF76A99A),
    ivory = Color(0xFFF7F5EF),
    mint = Color(0xFFDCEFE8),
    softMint = Color(0xFFEDF6F1),
    pureWhite = Color(0xFFFFFFFF),
    pureBlack = Color(0xFF000000),
    textDark = Color(0xFF263238),
    textMuted = Color(0xFF607D8B),
    textLight = Color(0xFF90A4AE),
    borderLight = Color(0xFFDCEFE8),
    borderSubtle = Color(0xFFE5ECE9),
    dividerColor = Color(0xFFE2E8EA),
    coralAccent = Color(0xFFE7A58B),
    coralAccentSoft = Color(0xFFFBF1EC),
    coralAccentBorder = Color(0xFFF3C7B5),
    statusSuccess = Color(0xFF2E7D32),
    statusWarning = Color(0xFFF57C00),
    statusError = Color(0xFFD32F2F),
    statusErrorBackground = Color(0xFFFFEBEE),
    statusErrorBorder = Color(0xFFFFCDD2),
    appBackground = Color(0xFFF7F5EF),
    appSurface = Color(0xFFFFFFFF),
    appSurfaceVariant = Color(0xFFEDF6F1)
)

val DarkMedAssistColors = MedAssistColors(
    isDark = true,
    deepTeal = Color(0xFF00ADB5),         // Vibrant medical cyan-teal
    darkTeal = Color(0xFF80DEEA),         // Soft bright teal for high contrast headings
    sageGreen = Color(0xFF81C784),        // Soft light sage green
    ivory = Color(0xFF0F171A),            // Deep clinical slate background
    mint = Color(0xFF23353D),             // Muted dark teal-slate border
    softMint = Color(0xFF162227),         // Subtle dark teal container
    pureWhite = Color(0xFF1B282F),        // Elevated dark card surface
    pureBlack = Color(0xFF000000),
    textDark = Color(0xFFF1F5F7),         // Crisp light slate/white text
    textMuted = Color(0xFF9CB2BD),        // Clear readable secondary text
    textLight = Color(0xFF6F8590),        // Muted tertiary text
    borderLight = Color(0xFF23353D),
    borderSubtle = Color(0xFF1D2C33),
    dividerColor = Color(0xFF1D2C33),
    coralAccent = Color(0xFFFFAB91),
    coralAccentSoft = Color(0xFF2E1C18),  // Dark coral tint for alert backgrounds
    coralAccentBorder = Color(0xFF5A2A20),
    statusSuccess = Color(0xFF66BB6A),
    statusWarning = Color(0xFFFFB74D),
    statusError = Color(0xFFEF5350),
    statusErrorBackground = Color(0xFF2F1515),
    statusErrorBorder = Color(0xFF5C2222),
    appBackground = Color(0xFF0F171A),
    appSurface = Color(0xFF1B282F),
    appSurfaceVariant = Color(0xFF162227)
)

/**
 * Observable holder for the active theme colors.
 * Reading ThemeState.current inside Compose functions automatically registers
 * state subscriptions, updating all UI components immediately upon theme toggle.
 * Also accessible in non-composable contexts like Canvas DrawScope or Lazy builders.
 */
object ThemeState {
    val colorsState = mutableStateOf(LightMedAssistColors)

    var current: MedAssistColors
        get() = colorsState.value
        set(value) {
            colorsState.value = value
        }
}

val LocalMedAssistColors = staticCompositionLocalOf { LightMedAssistColors }

// Static palette constants for backward compatibility and icon initialization
val StaticDeepTeal = Color(0xFF087F8C)
val StaticDarkTeal = Color(0xFF075E66)
val StaticSageGreen = Color(0xFF76A99A)
val StaticIvory = Color(0xFFF7F5EF)
val StaticMint = Color(0xFFDCEFE8)
val StaticSoftMint = Color(0xFFEDF6F1)
val StaticPureWhite = Color(0xFFFFFFFF)
val StaticPureBlack = Color(0xFF000000)
val StaticTextDark = Color(0xFF263238)
val StaticTextMuted = Color(0xFF607D8B)
val StaticTextLight = Color(0xFF90A4AE)

// ============================================================
// DYNAMIC DESIGN TOKENS (Reactive to active theme)
// Works seamlessly in @Composable, DrawScope, and non-composable contexts
// ============================================================

val DeepTeal: Color
    get() = ThemeState.current.deepTeal

val DarkTeal: Color
    get() = ThemeState.current.darkTeal

val SageGreen: Color
    get() = ThemeState.current.sageGreen

val Ivory: Color
    get() = ThemeState.current.ivory

val Mint: Color
    get() = ThemeState.current.mint

val SoftMint: Color
    get() = ThemeState.current.softMint

val PureWhite: Color
    get() = ThemeState.current.pureWhite

val PureBlack: Color
    get() = ThemeState.current.pureBlack

val TextDark: Color
    get() = ThemeState.current.textDark

val TextMuted: Color
    get() = ThemeState.current.textMuted

val TextLight: Color
    get() = ThemeState.current.textLight

val BorderLight: Color
    get() = ThemeState.current.borderLight

val BorderSubtle: Color
    get() = ThemeState.current.borderSubtle

val DividerColor: Color
    get() = ThemeState.current.dividerColor

val CoralAccent: Color
    get() = ThemeState.current.coralAccent

val CoralAccentSoft: Color
    get() = ThemeState.current.coralAccentSoft

val CoralAccentBorder: Color
    get() = ThemeState.current.coralAccentBorder

val StatusSuccess: Color
    get() = ThemeState.current.statusSuccess

val StatusWarning: Color
    get() = ThemeState.current.statusWarning

val StatusError: Color
    get() = ThemeState.current.statusError

val StatusErrorBackground: Color
    get() = ThemeState.current.statusErrorBackground

val StatusErrorBorder: Color
    get() = ThemeState.current.statusErrorBorder

val AppBackground: Color
    get() = ThemeState.current.appBackground

val AppSurface: Color
    get() = ThemeState.current.appSurface

val AppSurfaceVariant: Color
    get() = ThemeState.current.appSurfaceVariant

// Backward-compatible primary aliases
val PrimaryTeal: Color get() = DeepTeal
val PrimaryTealDark: Color get() = DarkTeal
val PrimaryTealLight: Color get() = SageGreen

// Backward-compatible surface aliases
val LightGreenSurface: Color get() = SoftMint
val LightGreenSoft: Color get() = Ivory
val LightGreenBorder: Color get() = Mint

// Backward Compatibility Aliases
val MedicalBlue: Color get() = PrimaryTeal
val MedicalBlueDark: Color get() = PrimaryTealDark
val MedicalBlueLight: Color get() = LightGreenSurface

val MedicalTeal: Color get() = PrimaryTeal
val MedicalTealDark: Color get() = PrimaryTealDark
val MedicalTealLight: Color get() = PrimaryTealLight

val MedicalBackground: Color get() = AppBackground
val MedicalSurface: Color get() = AppSurface
val MedicalSurfaceVariant: Color get() = AppSurfaceVariant

val MedicalTextPrimary: Color get() = TextDark
val MedicalTextSecondary: Color get() = TextMuted
val MedicalTextLight: Color get() = TextLight

val MedicalBorder: Color get() = BorderLight
val MedicalSuccess: Color get() = StatusSuccess
val MedicalWarning: Color get() = StatusWarning
val MedicalError: Color get() = StatusError
