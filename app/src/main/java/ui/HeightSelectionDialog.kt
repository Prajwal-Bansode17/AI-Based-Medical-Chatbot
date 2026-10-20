package ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai_based_medical_chatbot.ui.theme.DarkTeal
import com.example.ai_based_medical_chatbot.ui.theme.DeepTeal
import com.example.ai_based_medical_chatbot.ui.theme.Ivory
import com.example.ai_based_medical_chatbot.ui.theme.Mint
import com.example.ai_based_medical_chatbot.ui.theme.PureWhite
import com.example.ai_based_medical_chatbot.ui.theme.SageGreen
import com.example.ai_based_medical_chatbot.ui.theme.SoftMint
import com.example.ai_based_medical_chatbot.ui.theme.TextDark
import com.example.ai_based_medical_chatbot.ui.theme.TextMuted
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Compact, beautifully animated Height Selection Dialog.
 *
 * Features:
 * - Safe drawing padding and centered layout preventing navigation bar clipping.
 * - Simultaneous large metric display (cm) and secondary imperial display (ft' in").
 * - Metric / Imperial tab toggle without losing precision.
 * - Interactive vertical ruler with millimeter/centimeter ticks and drag gestures.
 * - Minus & Plus tactile controls (1 cm and 5 cm steps).
 * - Highlighted active indicator with MedAssist DeepTeal glow.
 * - Confirm and Cancel actions.
 */
@Composable
fun HeightSelectionDialog(
    initialHeightCm: Float,
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit,
    t: (String) -> String
) {
    val minCm = 50f
    val maxCm = 250f

    val startValue = if (initialHeightCm in minCm..maxCm) initialHeightCm else 150f
    var draftHeightCm by remember { mutableFloatStateOf(startValue) }
    var isCmUnit by remember { mutableStateOf(true) }

    val animatedHeightCm by animateFloatAsState(
        targetValue = draftHeightCm,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "animatedHeight"
    )

    val totalInches = draftHeightCm / 2.54f
    val feet = (totalInches / 12f).toInt()
    val inches = (totalInches % 12f).roundToInt()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .wrapContentHeight()
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(26.dp), spotColor = DeepTeal.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.5.dp, Mint)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header: Title & Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = t("height_popup_title"),
                                color = DarkTeal,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp
                            )
                            Text(
                                text = t("height_popup_subtitle"),
                                color = TextMuted,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .background(SoftMint, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = t("btn_cancel"),
                                tint = DarkTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Unit Selector Pill (CM / FT-IN)
                    Row(
                        modifier = Modifier
                            .background(SoftMint, RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, Mint), RoundedCornerShape(12.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        UnitTogglePill(
                            label = t("bmi_cm"),
                            selected = isCmUnit,
                            onClick = { isCmUnit = true }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        UnitTogglePill(
                            label = t("bmi_ft_in"),
                            selected = !isCmUnit,
                            onClick = { isCmUnit = false }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Large Value Display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(SoftMint, Ivory)
                                ),
                                RoundedCornerShape(18.dp)
                            )
                            .border(BorderStroke(1.dp, Mint.copy(alpha = 0.8f)), RoundedCornerShape(18.dp))
                            .padding(vertical = 10.dp, horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isCmUnit) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = String.format(Locale.US, "%.0f", animatedHeightCm),
                                        color = DarkTeal,
                                        fontSize = 38.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-1).sp
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "cm",
                                        color = DeepTeal,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 5.dp)
                                    )
                                }
                                Text(
                                    text = "≈ $feet' $inches\"",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "$feet' $inches\"",
                                        color = DarkTeal,
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-1).sp
                                    )
                                }
                                Text(
                                    text = "≈ " + String.format(Locale.US, "%.1f cm", animatedHeightCm),
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Interactive Precision Controls & Vertical Ruler
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Minus / Plus precision buttons
                        Column(
                            modifier = Modifier.width(68.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            PrecisionAdjustmentButton(
                                label = "+5",
                                onClick = {
                                    draftHeightCm = (draftHeightCm + 5f).coerceIn(minCm, maxCm)
                                }
                            )
                            PrecisionAdjustmentButton(
                                label = "+1",
                                onClick = {
                                    draftHeightCm = (draftHeightCm + 1f).coerceIn(minCm, maxCm)
                                }
                            )
                            PrecisionAdjustmentButton(
                                label = "-1",
                                onClick = {
                                    draftHeightCm = (draftHeightCm - 1f).coerceIn(minCm, maxCm)
                                }
                            )
                            PrecisionAdjustmentButton(
                                label = "-5",
                                onClick = {
                                    draftHeightCm = (draftHeightCm - 5f).coerceIn(minCm, maxCm)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Vertical Interactive Ruler
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(PureWhite, RoundedCornerShape(14.dp))
                                .border(BorderStroke(1.dp, Mint), RoundedCornerShape(14.dp))
                                .clip(RoundedCornerShape(14.dp))
                        ) {
                            InteractiveHeightRuler(
                                currentHeight = draftHeightCm,
                                minHeight = minCm,
                                maxHeight = maxCm,
                                onHeightChange = { draftHeightCm = it },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons: Cancel and Confirm
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.3.dp, Mint),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark)
                        ) {
                            Text(
                                text = t("btn_cancel"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                onConfirm(draftHeightCm)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepTeal,
                                contentColor = PureWhite
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = t("btn_confirm"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveHeightRuler(
    currentHeight: Float,
    minHeight: Float,
    maxHeight: Float,
    onHeightChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val dpPerCm = 8f
    val currentHeightState = rememberUpdatedState(currentHeight)

    Box(
        modifier = modifier
            .pointerInput(minHeight, maxHeight) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val deltaCm = -dragAmount.y / dpPerCm
                    val newHeight = (currentHeightState.value + deltaCm).coerceIn(minHeight, maxHeight)
                    onHeightChange(newHeight)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasHeight = size.height
            val canvasWidth = size.width
            val centerY = canvasHeight / 2f

            val visibleSpan = (canvasHeight / (2f * dpPerCm)).toInt() + 2
            val centerCmInt = currentHeight.roundToInt()

            val startCm = (centerCmInt - visibleSpan).coerceAtLeast(minHeight.toInt())
            val endCm = (centerCmInt + visibleSpan).coerceAtMost(maxHeight.toInt())

            for (cm in startCm..endCm) {
                val y = centerY - (cm - currentHeight) * dpPerCm
                if (y in -10f..(canvasHeight + 10f)) {
                    val isMajor = cm % 10 == 0
                    val isMedium = cm % 5 == 0

                    val tickWidth = when {
                        isMajor -> canvasWidth * 0.55f
                        isMedium -> canvasWidth * 0.38f
                        else -> canvasWidth * 0.22f
                    }

                    val tickColor = when {
                        isMajor -> DarkTeal
                        isMedium -> SageGreen
                        else -> Mint
                    }

                    val strokeWidth = if (isMajor) 2.5f else if (isMedium) 1.8f else 1.2f

                    drawLine(
                        color = tickColor,
                        start = Offset(x = canvasWidth - tickWidth, y = y),
                        end = Offset(x = canvasWidth - 10f, y = y),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Draw glowing Center Indicator Line
            drawLine(
                color = DeepTeal,
                start = Offset(x = 10f, y = centerY),
                end = Offset(x = canvasWidth - 10f, y = centerY),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )

            // Center triangular pointer
            val trianglePath = Path().apply {
                moveTo(8f, centerY - 5f)
                lineTo(16f, centerY)
                lineTo(8f, centerY + 5f)
                close()
            }
            drawPath(path = trianglePath, color = DeepTeal)
        }
    }
}

@Composable
private fun PrecisionAdjustmentButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(SoftMint, RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, Mint), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = DarkTeal,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun UnitTogglePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) DeepTeal else Color.Transparent
    val textColor = if (selected) PureWhite else TextMuted
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
