package ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Compact animated Weight Selection Dialog with an interactive semicircular speedometer dial.
 *
 * Features:
 * - Safe drawing padding and centered layout preventing navigation bar clipping or bottom layout overflow.
 * - Semicircular speedometer-style interactive dial with animated needle and value display.
 * - Precision +/- 0.1 kg buttons, and +/- 1.0 kg quick buttons.
 * - Kilograms (kg) and Pounds (lbs) toggling with exact conversions.
 * - Reliable drag & tap gestures around the dial arc clamped safely to 10f..250f kg.
 * - MedAssist AI DeepTeal, SageGreen, Mint and Ivory color palette.
 * - Confirm and Cancel actions.
 */
@Composable
fun WeightSelectionDialog(
    initialWeightKg: Float,
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit,
    t: (String) -> String
) {
    val minKg = 10f
    val maxKg = 250f

    // Working draft starts from initialWeightKg if valid, else default 50.0 kg
    val startValue = if (initialWeightKg in minKg..maxKg) initialWeightKg else 50.0f
    var draftWeightKg by remember { mutableFloatStateOf(startValue) }
    var isKgUnit by remember { mutableStateOf(true) }

    val animatedWeightKg by animateFloatAsState(
        targetValue = draftWeightKg,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "animatedWeight"
    )

    val displayedWeight = if (isKgUnit) {
        animatedWeightKg
    } else {
        animatedWeightKg * 2.20462f
    }

    val unitLabel = if (isKgUnit) "kg" else "lbs"

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
                                text = t("weight_popup_title"),
                                color = DarkTeal,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp
                            )
                            Text(
                                text = t("weight_popup_subtitle"),
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

                    // Unit Selector Pill (KG / LBS)
                    Row(
                        modifier = Modifier
                            .background(SoftMint, RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, Mint), RoundedCornerShape(12.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        UnitTogglePill(
                            label = t("bmi_kg"),
                            selected = isKgUnit,
                            onClick = { isKgUnit = true }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        UnitTogglePill(
                            label = t("bmi_lbs"),
                            selected = !isKgUnit,
                            onClick = { isKgUnit = false }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Interactive Speedometer Dial
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(155.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        InteractiveSpeedometerDial(
                            weightKg = draftWeightKg,
                            minKg = minKg,
                            maxKg = maxKg,
                            onWeightChange = { draftWeightKg = it },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Value readout in the lower center of the dial
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = String.format(Locale.US, "%.1f", displayedWeight),
                                    color = DarkTeal,
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-1).sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = unitLabel,
                                    color = DeepTeal,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 5.dp)
                                )
                            }

                            // Secondary unit representation
                            val secondaryText = if (isKgUnit) {
                                String.format(Locale.US, "≈ %.1f lbs", animatedWeightKg * 2.20462f)
                            } else {
                                String.format(Locale.US, "≈ %.1f kg", animatedWeightKg)
                            }
                            Text(
                                text = secondaryText,
                                color = TextMuted,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tactile Step Buttons: -1.0, -0.1, +0.1, +1.0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StepButton(
                            label = "-1.0",
                            onClick = {
                                val step = if (isKgUnit) 1f else (1f / 2.20462f)
                                draftWeightKg = (draftWeightKg - step).coerceIn(minKg, maxKg)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        StepButton(
                            label = "-0.1",
                            onClick = {
                                val step = if (isKgUnit) 0.1f else (0.1f / 2.20462f)
                                draftWeightKg = (draftWeightKg - step).coerceIn(minKg, maxKg)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        StepButton(
                            label = "+0.1",
                            onClick = {
                                val step = if (isKgUnit) 0.1f else (0.1f / 2.20462f)
                                draftWeightKg = (draftWeightKg + step).coerceIn(minKg, maxKg)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        StepButton(
                            label = "+1.0",
                            onClick = {
                                val step = if (isKgUnit) 1f else (1f / 2.20462f)
                                draftWeightKg = (draftWeightKg + step).coerceIn(minKg, maxKg)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Actions: Cancel and Confirm
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
                                onConfirm(draftWeightKg)
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

/**
 * Semicircular speedometer dial with touch/drag tracking and animated needle.
 * Angle spans from 180° (left, minKg) to 0°/360° (right, maxKg) over the upper semi-circle.
 */
@Composable
private fun InteractiveSpeedometerDial(
    weightKg: Float,
    minKg: Float,
    maxKg: Float,
    onWeightChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val startAngle = 180f
    val sweepAngle = 180f

    val fraction = ((weightKg - minKg) / (maxKg - minKg)).coerceIn(0f, 1f)
    val needleAngleDeg = startAngle + fraction * sweepAngle

    val onWeightChangeState = rememberUpdatedState(onWeightChange)

    val currentWeightState = rememberUpdatedState(weightKg)

    Box(
        modifier = modifier
            .pointerInput(minKg, maxKg) {
                var prevAngleDeg: Float? = null

                fun computeAngle(touchX: Float, touchY: Float, width: Float, height: Float): Float {
                    val pivotX = width / 2f
                    val pivotY = height * 0.90f
                    val dx = touchX - pivotX
                    val dy = touchY - pivotY
                    var angleRad = atan2(dy, dx)
                    var angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()
                    if (angleDeg < 0) angleDeg += 360f
                    return angleDeg
                }

                fun angleToWeight(angleDeg: Float): Float {
                    val clampedAngle = when {
                        angleDeg in 180f..360f -> angleDeg
                        angleDeg < 90f -> 360f
                        else -> 180f
                    }
                    val frac = (clampedAngle - 180f) / 180f
                    return minKg + frac * (maxKg - minKg)
                }

                detectDragGestures(
                    onDragStart = { offset ->
                        val initialDeg = computeAngle(offset.x, offset.y, size.width.toFloat(), size.height.toFloat())
                        prevAngleDeg = initialDeg
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val currentDeg = computeAngle(change.position.x, change.position.y, size.width.toFloat(), size.height.toFloat())
                        val lastDeg = prevAngleDeg
                        if (lastDeg != null) {
                            var delta = currentDeg - lastDeg
                            if (delta > 180f) delta -= 360f
                            if (delta < -180f) delta += 360f

                            // Dynamic 1:1 arc tracking without artificial damping drag bottleneck:
                            // 1 degree of angular drag = 1 degree of arc rotation
                            val deltaFraction = delta / 180f
                            val weightDelta = deltaFraction * (maxKg - minKg)
                            val updated = (currentWeightState.value + weightDelta).coerceIn(minKg, maxKg)
                            onWeightChangeState.value(updated)
                        }
                        prevAngleDeg = currentDeg
                    },
                    onDragEnd = {
                        prevAngleDeg = null
                    },
                    onDragCancel = {
                        prevAngleDeg = null
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val pivot = Offset(width / 2f, height * 0.90f)
            val radius = (width / 2f) - 22.dp.toPx()

            val arcRectTopLeft = Offset(pivot.x - radius, pivot.y - radius)
            val arcSize = Size(radius * 2f, radius * 2f)

            // 1. Background Arc Track
            drawArc(
                color = SoftMint,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = arcRectTopLeft,
                size = arcSize,
                style = Stroke(width = 15.dp.toPx(), cap = StrokeCap.Round)
            )

            // 2. Active Weight Progress Arc (Teal Gradient)
            val activeSweep = fraction * sweepAngle
            if (activeSweep > 0f) {
                drawArc(
                    brush = Brush.horizontalGradient(
                        colors = listOf(SageGreen, DeepTeal, DarkTeal)
                    ),
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = arcRectTopLeft,
                    size = arcSize,
                    style = Stroke(width = 15.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3. Dial Tick Marks
            val totalTicks = 20
            for (i in 0..totalTicks) {
                val tickFraction = i.toFloat() / totalTicks
                val tickAngleDeg = startAngle + tickFraction * sweepAngle
                val tickAngleRad = tickAngleDeg * (PI / 180f).toFloat()

                val isMajor = i % 5 == 0
                val tickLen = if (isMajor) 10.dp.toPx() else 6.dp.toPx()
                val tickColor = if (tickFraction <= fraction) DeepTeal else Mint.copy(alpha = 0.8f)

                val outerRadius = radius - 10.dp.toPx()
                val innerRadius = outerRadius - tickLen

                val pStart = Offset(
                    x = pivot.x + innerRadius * cos(tickAngleRad),
                    y = pivot.y + innerRadius * sin(tickAngleRad)
                )
                val pEnd = Offset(
                    x = pivot.x + outerRadius * cos(tickAngleRad),
                    y = pivot.y + outerRadius * sin(tickAngleRad)
                )

                drawLine(
                    color = tickColor,
                    start = pStart,
                    end = pEnd,
                    strokeWidth = if (isMajor) 2.2f else 1.2f,
                    cap = StrokeCap.Round
                )
            }

            // 4. Sleek Needle
            val needleRad = needleAngleDeg * (PI / 180f).toFloat()
            val needleLen = radius - 14.dp.toPx()

            val tip = Offset(
                x = pivot.x + needleLen * cos(needleRad),
                y = pivot.y + needleLen * sin(needleRad)
            )

            val perpAngle = needleRad + (PI / 2f).toFloat()
            val baseWidth = 4.dp.toPx()

            val b1 = Offset(
                x = pivot.x + baseWidth * cos(perpAngle),
                y = pivot.y + baseWidth * sin(perpAngle)
            )
            val b2 = Offset(
                x = pivot.x - baseWidth * cos(perpAngle),
                y = pivot.y - baseWidth * sin(perpAngle)
            )

            val needlePath = Path().apply {
                moveTo(b1.x, b1.y)
                lineTo(tip.x, tip.y)
                lineTo(b2.x, b2.y)
                close()
            }

            drawPath(path = needlePath, color = DarkTeal)

            // Needle tip accent
            drawCircle(
                color = DeepTeal,
                radius = 4.dp.toPx(),
                center = tip
            )

            // Center Pivot Hub
            drawCircle(
                color = DarkTeal,
                radius = 10.dp.toPx(),
                center = pivot
            )
            drawCircle(
                color = PureWhite,
                radius = 4.5.dp.toPx(),
                center = pivot
            )
        }
    }
}

@Composable
private fun StepButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SoftMint)
            .border(BorderStroke(1.dp, Mint), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = DarkTeal,
            fontSize = 12.5.sp,
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
