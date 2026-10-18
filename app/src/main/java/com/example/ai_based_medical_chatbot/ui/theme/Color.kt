package com.example.ai_based_medical_chatbot.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// MEDASSIST AI - Global Healthcare Design Tokens
// Approved Palette: Deep Teal + Sage Green + Ivory + Mint + Coral
// ============================================================

// Primary Healthcare Teals
val DeepTeal = Color(0xFF087F8C)
val DarkTeal = Color(0xFF075E66)
val SageGreen = Color(0xFF76A99A)

// Surfaces & Backgrounds
val Ivory = Color(0xFFF7F5EF)
val Mint = Color(0xFFDCEFE8)
val SoftMint = Color(0xFFEDF6F1)
val PureWhite = Color(0xFFFFFFFF)
val PureBlack = Color(0xFF000000)

// Backward-compatible primary aliases
val PrimaryTeal = DeepTeal
val PrimaryTealDark = DarkTeal
val PrimaryTealLight = SageGreen

// Backward-compatible surface aliases
val LightGreenSurface = SoftMint
val LightGreenSoft = Ivory
val LightGreenBorder = Mint

val AppBackground = Ivory
val AppSurface = PureWhite
val AppSurfaceVariant = SoftMint

// Slate & Neutral Text Hierarchy
val TextDark = Color(0xFF263238)
val TextMuted = Color(0xFF607D8B)
val TextLight = Color(0xFF90A4AE)

// Neutral Borders and Dividers
val BorderLight = Mint
val BorderSubtle = Color(0xFFE5ECE9)
val DividerColor = Color(0xFFE2E8EA)

// Coral Accents (Used Sparingly)
val CoralAccent = Color(0xFFE7A58B)
val CoralAccentSoft = Color(0xFFFBF1EC)
val CoralAccentBorder = Color(0xFFF3C7B5)

// Status / Feedback Accents
val StatusSuccess = Color(0xFF2E7D32)
val StatusWarning = Color(0xFFF57C00)
val StatusError = Color(0xFFD32F2F)
val StatusErrorBackground = Color(0xFFFFEBEE)
val StatusErrorBorder = Color(0xFFFFCDD2)

// Backward Compatibility Aliases (maps existing names to new green palette)
val MedicalBlue = PrimaryTeal
val MedicalBlueDark = PrimaryTealDark
val MedicalBlueLight = LightGreenSurface

val MedicalTeal = PrimaryTeal
val MedicalTealDark = PrimaryTealDark
val MedicalTealLight = PrimaryTealLight

val MedicalBackground = AppBackground
val MedicalSurface = AppSurface
val MedicalSurfaceVariant = AppSurfaceVariant

val MedicalTextPrimary = TextDark
val MedicalTextSecondary = TextMuted
val MedicalTextLight = TextLight

val MedicalBorder = BorderLight
val MedicalSuccess = StatusSuccess
val MedicalWarning = StatusWarning
val MedicalError = StatusError