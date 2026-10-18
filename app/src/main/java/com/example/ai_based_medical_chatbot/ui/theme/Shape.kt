package com.example.ai_based_medical_chatbot.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ============================================================
// MEDASSIST AI - Global Shape & Spacing Tokens
// ============================================================

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

object MedAssistSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val default = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp

    val cardElevation = 1.dp
    val cardCornerRadius = 18.dp
    val buttonCornerRadius = 14.dp
    val bubbleCornerRadius = 18.dp
    val inputCornerRadius = 24.dp

    // Component dimension guidelines
    val primaryButtonHeight = 50.dp
    val inputFieldHeight = 54.dp
    val minTouchTarget = 48.dp
    val iconSizeSmall = 18.dp
    val iconSizeMedium = 22.dp
    val iconSizeLarge = 28.dp
    val iconContainerSmall = 40.dp
    val iconContainerMedium = 46.dp
    val iconContainerLarge = 54.dp
}
