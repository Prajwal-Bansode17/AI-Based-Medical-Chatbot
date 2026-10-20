package ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_based_medical_chatbot.ui.theme.DarkTeal
import com.example.ai_based_medical_chatbot.ui.theme.DeepTeal
import com.example.ai_based_medical_chatbot.ui.theme.Ivory
import com.example.ai_based_medical_chatbot.ui.theme.Mint
import com.example.ai_based_medical_chatbot.ui.theme.PureWhite
import com.example.ai_based_medical_chatbot.ui.theme.SageGreen
import com.example.ai_based_medical_chatbot.ui.theme.SoftMint
import com.example.ai_based_medical_chatbot.ui.theme.StatusError
import com.example.ai_based_medical_chatbot.ui.theme.StatusSuccess
import com.example.ai_based_medical_chatbot.ui.theme.StatusWarning
import com.example.ai_based_medical_chatbot.ui.theme.TextDark
import com.example.ai_based_medical_chatbot.ui.theme.TextMuted
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Premium Multicolor Speedometer Gauge for BMI Results.
 *
 * Geometry & Placement:
 * - Upper semicircle speed gauge with needle rotating strictly in the upper region.
 * - Prominent BMI numeric readout and "kg/m²" unit positioned cleanly below the pivot
 *   to completely eliminate any visual obstruction or needle collision.
 * - Categorized according to adult WHO thresholds:
 *   - < 18.5: Underweight (Cool Teal / Sage)
 *   - 18.5 .. < 25.0: Healthy Weight (Emerald Green)
 *   - 25.0 .. < 30.0: Overweight (Amber / Warm Orange)
 *   - >= 30.0: Obesity (Deep Coral / Red)
 */
@Composable
fun BmiSpeedometerGauge(
    bmiValue: Float,
    t: (String) -> String,
    modifier: Modifier = Modifier
) {
    val minDisplayBmi = 12f
    val maxDisplayBmi = 40f

    val safeBmi = if (bmiValue > 0f) bmiValue.coerceIn(minDisplayBmi, maxDisplayBmi) else 0f

    val animatedBmiValue = remember { Animatable(0f) }
    LaunchedEffect(safeBmi) {
        animatedBmiValue.animateTo(
            targetValue = safeBmi,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        )
    }

    val (categoryTitle, categoryColor, categoryDesc) = when {
        safeBmi <= 0f -> Triple("—", TextMuted, "")
        safeBmi < 18.5f -> Triple(
            t("bmi_underweight"),
            SageGreen,
            t("bmi_underweight_desc")
        )
        safeBmi < 25.0f -> Triple(
            t("bmi_normal"),
            StatusSuccess,
            t("bmi_normal_desc")
        )
        safeBmi < 30.0f -> Triple(
            t("bmi_overweight"),
            StatusWarning,
            t("bmi_overweight_desc")
        )
        else -> Triple(
            t("bmi_obese"),
            StatusError,
            t("bmi_obese_desc")
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(22.dp), spotColor = DeepTeal.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, Mint)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = t("bmi_result_title"),
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Upper Speedometer Arc Canvas (Clean Upper Semi-circle)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentAlignment = Alignment.Center
            ) {
                SpeedometerCanvas(
                    currentBmi = animatedBmiValue.value,
                    minBmi = minDisplayBmi,
                    maxBmi = maxDisplayBmi,
                    modifier = Modifier.fillMaxWidth().height(130.dp)
                )
            }

            // Unobstructed Large Numeric Readout positioned cleanly below the gauge pivot
            Column(
                modifier = Modifier.padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val displayStr = if (animatedBmiValue.value > 0f) {
                    String.format(Locale.US, "%.1f", animatedBmiValue.value)
                } else {
                    "--"
                }

                Text(
                    text = displayStr,
                    color = DarkTeal,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )

                Text(
                    text = "kg/m²",
                    color = DeepTeal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Health Category Badge
            Box(
                modifier = Modifier
                    .background(categoryColor.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .border(BorderStroke(1.2.dp, categoryColor.copy(alpha = 0.45f)), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 5.dp)
            ) {
                Text(
                    text = categoryTitle,
                    color = categoryColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Category Explanation Guidance
            if (categoryDesc.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = categoryDesc,
                    color = TextDark,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Healthy Range Reference Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SoftMint, RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, Mint), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = t("bmi_healthy_range_label"),
                    color = DarkTeal,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = t("bmi_healthy_range_value"),
                    color = DeepTeal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Medical Disclaimer Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Ivory, RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, Mint.copy(alpha = 0.6f)), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = DeepTeal,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = t("bmi_disclaimer_text"),
                    color = TextMuted,
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

/**
 * Semicircular Canvas drawing the 4 WHO BMI color segments and animated needle.
 *
 * Needle rotates from 180° (left) to 360° (right) across the upper region,
 * ensuring it never descends below pivotY.
 */
@Composable
private fun SpeedometerCanvas(
    currentBmi: Float,
    minBmi: Float,
    maxBmi: Float,
    modifier: Modifier = Modifier
) {
    val startAngle = 180f
    val totalSweep = 180f

    val needleFraction = if (currentBmi <= 0f) {
        0f
    } else {
        ((currentBmi - minBmi) / (maxBmi - minBmi)).coerceIn(0f, 1f)
    }

    val needleAngle = startAngle + needleFraction * totalSweep

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val pivot = Offset(width / 2f, height - 12.dp.toPx())
        val radius = (width / 2f) - 20.dp.toPx()

        val arcRectTopLeft = Offset(pivot.x - radius, pivot.y - radius)
        val arcSize = Size(radius * 2f, radius * 2f)
        val trackThickness = 14.dp.toPx()

        val totalSpan = maxBmi - minBmi

        val sweepUnderweight = ((18.5f - 12f) / totalSpan) * totalSweep
        val sweepNormal = ((25f - 18.5f) / totalSpan) * totalSweep
        val sweepOverweight = ((30f - 25f) / totalSpan) * totalSweep
        val sweepObese = ((40f - 30f) / totalSpan) * totalSweep

        // Segment 1: Underweight (Cool Sage)
        drawArc(
            color = SageGreen,
            startAngle = startAngle,
            sweepAngle = sweepUnderweight - 1.5f,
            useCenter = false,
            topLeft = arcRectTopLeft,
            size = arcSize,
            style = Stroke(width = trackThickness, cap = StrokeCap.Round)
        )

        // Segment 2: Healthy Weight (Emerald Green)
        drawArc(
            color = StatusSuccess,
            startAngle = startAngle + sweepUnderweight,
            sweepAngle = sweepNormal - 1.5f,
            useCenter = false,
            topLeft = arcRectTopLeft,
            size = arcSize,
            style = Stroke(width = trackThickness, cap = StrokeCap.Round)
        )

        // Segment 3: Overweight (Amber/Orange)
        drawArc(
            color = StatusWarning,
            startAngle = startAngle + sweepUnderweight + sweepNormal,
            sweepAngle = sweepOverweight - 1.5f,
            useCenter = false,
            topLeft = arcRectTopLeft,
            size = arcSize,
            style = Stroke(width = trackThickness, cap = StrokeCap.Round)
        )

        // Segment 4: Obese (Coral Red)
        drawArc(
            color = StatusError,
            startAngle = startAngle + sweepUnderweight + sweepNormal + sweepOverweight,
            sweepAngle = sweepObese - 1.5f,
            useCenter = false,
            topLeft = arcRectTopLeft,
            size = arcSize,
            style = Stroke(width = trackThickness, cap = StrokeCap.Round)
        )

        // Scale marker ticks: 18.5, 25, 30
        val boundaryValues = listOf(18.5f, 25f, 30f)
        boundaryValues.forEach { bound ->
            val tickFrac = (bound - minBmi) / totalSpan
            val tickAngle = startAngle + tickFrac * totalSweep
            val tickRad = tickAngle * (PI / 180f).toFloat()

            val innerR = radius - 16.dp.toPx()
            val outerR = radius - 6.dp.toPx()

            drawLine(
                color = PureWhite,
                start = Offset(pivot.x + innerR * cos(tickRad), pivot.y + innerR * sin(tickRad)),
                end = Offset(pivot.x + outerR * cos(tickRad), pivot.y + outerR * sin(tickRad)),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
        }

        // Animated Needle
        if (currentBmi > 0f) {
            val needleRad = needleAngle * (PI / 180f).toFloat()
            val needleLength = radius - 12.dp.toPx()

            val needleTip = Offset(
                x = pivot.x + needleLength * cos(needleRad),
                y = pivot.y + needleLength * sin(needleRad)
            )

            val perpAngle = needleRad + (PI / 2f).toFloat()
            val baseWidth = 4.dp.toPx()

            val baseLeft = Offset(
                x = pivot.x + baseWidth * cos(perpAngle),
                y = pivot.y + baseWidth * sin(perpAngle)
            )
            val baseRight = Offset(
                x = pivot.x - baseWidth * cos(perpAngle),
                y = pivot.y - baseWidth * sin(perpAngle)
            )

            val needlePath = Path().apply {
                moveTo(baseLeft.x, baseLeft.y)
                lineTo(needleTip.x, needleTip.y)
                lineTo(baseRight.x, baseRight.y)
                close()
            }

            drawPath(path = needlePath, color = DarkTeal)

            // Needle tip accent ring
            drawCircle(
                color = DeepTeal,
                radius = 4.dp.toPx(),
                center = needleTip
            )

            // Center Pivot Hub
            drawCircle(
                color = DarkTeal,
                radius = 9.dp.toPx(),
                center = pivot
            )
            drawCircle(
                color = PureWhite,
                radius = 4.dp.toPx(),
                center = pivot
            )
        }
    }
}
