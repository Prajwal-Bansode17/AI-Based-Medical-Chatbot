package ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SymptomVisualBadge(
    iconType: SymptomIconType,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    val (glowBg, mainColor, accentColor) = when (iconType) {
        SymptomIconType.HEADACHE -> Triple(Color(0xFFFEECEB), Color(0xFFE55755), Color(0xFFFA8987))
        SymptomIconType.FEVER -> Triple(Color(0xFFEEF3FF), Color(0xFFEF5350), Color(0xFF42A5F5))
        SymptomIconType.COUGH -> Triple(Color(0xFFEAF5FF), Color(0xFF3892E3), Color(0xFF64B5F6))
        SymptomIconType.BACK_PAIN -> Triple(Color(0xFFE8F8F3), Color(0xFF0D8A7D), Color(0xFFE74C3C))
        SymptomIconType.CHEST_PAIN -> Triple(Color(0xFFFEECEB), Color(0xFFE53935), Color(0xFFFFFFFF))
        SymptomIconType.NAUSEA -> Triple(Color(0xFFFFF0EE), Color(0xFFE55755), Color(0xFFFFA726))
        SymptomIconType.PAIN -> Triple(Color(0xFFFFF3E0), Color(0xFFFB8C00), Color(0xFFF57C00))
        SymptomIconType.DIGESTIVE -> Triple(Color(0xFFF1F8E9), Color(0xFF43A047), Color(0xFF81C784))
        SymptomIconType.LUNGS -> Triple(Color(0xFFE1F5FE), Color(0xFF039BE5), Color(0xFF4FC3F7))
        SymptomIconType.SKIN -> Triple(Color(0xFFFCE4EC), Color(0xFFD81B60), Color(0xFFF06292))
        SymptomIconType.HEART -> Triple(Color(0xFFFFEBEE), Color(0xFFE53935), Color(0xFFEF5350))
        SymptomIconType.EYE -> Triple(Color(0xFFEDE7F6), Color(0xFF5E35B1), Color(0xFF7E57C2))
        SymptomIconType.EAR -> Triple(Color(0xFFE0F2F1), Color(0xFF00897B), Color(0xFF26A69A))
        SymptomIconType.URINARY -> Triple(Color(0xFFFFF8E1), Color(0xFFFBC02D), Color(0xFFFFA000))
        else -> Triple(Color(0xFFEDF6F1), Color(0xFF087F8C), Color(0xFF76A99A))
    }

    Box(
        modifier = modifier
            .size(size)
            .background(
                Brush.radialGradient(
                    listOf(glowBg, glowBg.copy(alpha = 0.5f)),
                    radius = 90f
                ),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.65f)) {
            val w = this.size.width
            val h = this.size.height

            when (iconType) {
                SymptomIconType.HEADACHE -> {
                    // Brain hemispheres
                    val brainPath = Path().apply {
                        moveTo(w * 0.5f, h * 0.25f)
                        // Left hemisphere
                        cubicTo(w * 0.2f, h * 0.15f, w * 0.05f, h * 0.5f, w * 0.25f, h * 0.75f)
                        cubicTo(w * 0.35f, h * 0.85f, w * 0.45f, h * 0.8f, w * 0.5f, h * 0.75f)
                        // Right hemisphere
                        cubicTo(w * 0.55f, h * 0.8f, w * 0.65f, h * 0.85f, w * 0.75f, h * 0.75f)
                        cubicTo(w * 0.95f, h * 0.5f, w * 0.8f, h * 0.15f, w * 0.5f, h * 0.25f)
                        close()
                    }
                    drawPath(brainPath, color = accentColor, style = Fill)
                    drawPath(brainPath, color = mainColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

                    // Center groove
                    drawLine(
                        color = mainColor,
                        start = Offset(w * 0.5f, h * 0.25f),
                        end = Offset(w * 0.5f, h * 0.72f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Pain radiating sparks (top-right)
                    drawLine(
                        color = mainColor,
                        start = Offset(w * 0.82f, h * 0.22f),
                        end = Offset(w * 0.96f, h * 0.12f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = mainColor,
                        start = Offset(w * 0.88f, h * 0.32f),
                        end = Offset(w * 0.98f, h * 0.32f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                SymptomIconType.FEVER -> {
                    // Thermometer glass stem
                    val stemWidth = w * 0.22f
                    val stemHeight = h * 0.65f
                    val stemLeft = (w - stemWidth) / 2
                    val stemTop = h * 0.08f

                    // Glass border
                    drawRoundRect(
                        color = Color(0xFF90CAF9),
                        topLeft = Offset(stemLeft, stemTop),
                        size = Size(stemWidth, stemHeight),
                        cornerRadius = CornerRadius(stemWidth / 2, stemWidth / 2),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Glass bulb
                    val bulbRadius = w * 0.24f
                    val bulbCenter = Offset(w * 0.5f, h * 0.75f)
                    drawCircle(color = Color(0xFF90CAF9), radius = bulbRadius, center = bulbCenter, style = Stroke(width = 2.dp.toPx()))

                    // Red Mercury Bulb
                    drawCircle(color = mainColor, radius = bulbRadius * 0.75f, center = bulbCenter, style = Fill)

                    // Red Mercury Column
                    val mercuryWidth = stemWidth * 0.55f
                    val mercuryLeft = (w - mercuryWidth) / 2
                    drawRoundRect(
                        color = mainColor,
                        topLeft = Offset(mercuryLeft, h * 0.25f),
                        size = Size(mercuryWidth, h * 0.5f),
                        cornerRadius = CornerRadius(mercuryWidth / 2, mercuryWidth / 2),
                        style = Fill
                    )

                    // Tick lines
                    drawLine(color = Color(0xFF64B5F6), start = Offset(stemLeft + stemWidth + 2.dp.toPx(), h * 0.28f), end = Offset(stemLeft + stemWidth + 6.dp.toPx(), h * 0.28f), strokeWidth = 1.5.dp.toPx())
                    drawLine(color = Color(0xFF64B5F6), start = Offset(stemLeft + stemWidth + 2.dp.toPx(), h * 0.40f), end = Offset(stemLeft + stemWidth + 6.dp.toPx(), h * 0.40f), strokeWidth = 1.5.dp.toPx())
                    drawLine(color = Color(0xFF64B5F6), start = Offset(stemLeft + stemWidth + 2.dp.toPx(), h * 0.52f), end = Offset(stemLeft + stemWidth + 6.dp.toPx(), h * 0.52f), strokeWidth = 1.5.dp.toPx())
                }

                SymptomIconType.COUGH, SymptomIconType.LUNGS -> {
                    // Left lung
                    val leftLung = Path().apply {
                        moveTo(w * 0.44f, h * 0.28f)
                        cubicTo(w * 0.25f, h * 0.25f, w * 0.12f, h * 0.5f, w * 0.18f, h * 0.75f)
                        cubicTo(w * 0.22f, h * 0.85f, w * 0.38f, h * 0.85f, w * 0.44f, h * 0.7f)
                        close()
                    }
                    // Right lung
                    val rightLung = Path().apply {
                        moveTo(w * 0.56f, h * 0.28f)
                        cubicTo(w * 0.75f, h * 0.25f, w * 0.88f, h * 0.5f, w * 0.82f, h * 0.75f)
                        cubicTo(w * 0.78f, h * 0.85f, w * 0.62f, h * 0.85f, w * 0.56f, h * 0.7f)
                        close()
                    }
                    drawPath(leftLung, color = mainColor, style = Fill)
                    drawPath(rightLung, color = mainColor, style = Fill)

                    // Trachea / windpipe
                    drawLine(
                        color = Color(0xFF1E88E5),
                        start = Offset(w * 0.5f, h * 0.12f),
                        end = Offset(w * 0.5f, h * 0.35f),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Cough air waves (to right side)
                    drawLine(color = Color(0xFF42A5F5), start = Offset(w * 0.85f, h * 0.45f), end = Offset(w * 0.98f, h * 0.45f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(color = Color(0xFF42A5F5), start = Offset(w * 0.85f, h * 0.55f), end = Offset(w * 0.96f, h * 0.55f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                }

                SymptomIconType.BACK_PAIN -> {
                    // Spine vertebral column
                    val cx = w * 0.45f
                    val vertebraCount = 5
                    for (i in 0 until vertebraCount) {
                        val vy = h * (0.2f + i * 0.13f)
                        val vWidth = w * (0.28f + (if (i == 3) 0.06f else 0f))
                        val vHeight = h * 0.08f
                        drawRoundRect(
                            color = mainColor,
                            topLeft = Offset(cx - vWidth / 2, vy),
                            size = Size(vWidth, vHeight),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                            style = Fill
                        )
                    }
                    // Spinal cord line
                    drawLine(
                        color = Color(0xFF004D40),
                        start = Offset(cx, h * 0.15f),
                        end = Offset(cx, h * 0.82f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Lumbar pain glowing disc (on 3rd-4th vertebra)
                    val painY = h * 0.59f
                    drawCircle(
                        color = Color(0xFFE74C3C).copy(alpha = 0.45f),
                        radius = w * 0.22f,
                        center = Offset(cx + w * 0.1f, painY)
                    )
                    drawCircle(
                        color = Color(0xFFE74C3C),
                        radius = w * 0.1f,
                        center = Offset(cx + w * 0.1f, painY)
                    )
                }

                SymptomIconType.CHEST_PAIN -> {
                    // Heart shape
                    val heartPath = Path().apply {
                        moveTo(w * 0.5f, h * 0.78f)
                        cubicTo(w * 0.15f, h * 0.55f, w * 0.1f, h * 0.28f, w * 0.32f, h * 0.22f)
                        cubicTo(w * 0.44f, h * 0.18f, w * 0.5f, h * 0.3f, w * 0.5f, h * 0.3f)
                        cubicTo(w * 0.5f, h * 0.3f, w * 0.56f, h * 0.18f, w * 0.68f, h * 0.22f)
                        cubicTo(w * 0.9f, h * 0.28f, w * 0.85f, h * 0.55f, w * 0.5f, h * 0.78f)
                        close()
                    }
                    drawPath(heartPath, color = mainColor, style = Fill)

                    // ECG pulse wave across heart
                    val ecgPath = Path().apply {
                        moveTo(w * 0.24f, h * 0.48f)
                        lineTo(w * 0.38f, h * 0.48f)
                        lineTo(w * 0.44f, h * 0.36f)
                        lineTo(w * 0.52f, h * 0.62f)
                        lineTo(w * 0.58f, h * 0.42f)
                        lineTo(w * 0.64f, h * 0.52f)
                        lineTo(w * 0.76f, h * 0.48f)
                    }
                    drawPath(ecgPath, color = Color.White, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

                    // Pain sparks
                    drawLine(color = mainColor, start = Offset(w * 0.82f, h * 0.62f), end = Offset(w * 0.94f, h * 0.68f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(color = mainColor, start = Offset(w * 0.78f, h * 0.72f), end = Offset(w * 0.86f, h * 0.82f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                }

                SymptomIconType.NAUSEA, SymptomIconType.DIGESTIVE -> {
                    // Stomach shape
                    val stomachPath = Path().apply {
                        moveTo(w * 0.45f, h * 0.2f) // esophagus
                        lineTo(w * 0.55f, h * 0.2f)
                        cubicTo(w * 0.75f, h * 0.25f, w * 0.85f, h * 0.5f, w * 0.75f, h * 0.72f) // greater curvature
                        cubicTo(w * 0.65f, h * 0.88f, w * 0.35f, h * 0.85f, w * 0.25f, h * 0.75f)
                        cubicTo(w * 0.15f, h * 0.65f, w * 0.2f, h * 0.45f, w * 0.35f, h * 0.38f) // lesser curvature
                        cubicTo(w * 0.45f, h * 0.35f, w * 0.42f, h * 0.25f, w * 0.45f, h * 0.2f)
                        close()
                    }
                    drawPath(stomachPath, color = mainColor, style = Fill)
                    drawPath(stomachPath, color = Color(0xFFC62828), style = Stroke(width = 1.5.dp.toPx()))

                    // Soft digestive wave / nausea swirl
                    val swirlPath = Path().apply {
                        moveTo(w * 0.38f, h * 0.58f)
                        cubicTo(w * 0.48f, h * 0.5f, w * 0.52f, h * 0.68f, w * 0.62f, h * 0.6f)
                    }
                    drawPath(swirlPath, color = Color.White.copy(alpha = 0.8f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                }

                SymptomIconType.SKIN -> {
                    // Hand/skin surface with gentle touch
                    val skinPath = Path().apply {
                        moveTo(w * 0.2f, h * 0.6f)
                        cubicTo(w * 0.35f, h * 0.4f, w * 0.65f, h * 0.4f, w * 0.8f, h * 0.6f)
                        lineTo(w * 0.8f, h * 0.78f)
                        lineTo(w * 0.2f, h * 0.78f)
                        close()
                    }
                    drawPath(skinPath, color = mainColor, style = Fill)
                    // Sparkles / rash spots
                    drawCircle(color = accentColor, radius = 3.dp.toPx(), center = Offset(w * 0.4f, h * 0.38f))
                    drawCircle(color = accentColor, radius = 4.dp.toPx(), center = Offset(w * 0.6f, h * 0.32f))
                }

                else -> {
                    // Medical Cross / Sparkle
                    val crossPath = Path().apply {
                        moveTo(w * 0.4f, h * 0.2f)
                        lineTo(w * 0.6f, h * 0.2f)
                        lineTo(w * 0.6f, h * 0.4f)
                        lineTo(w * 0.8f, h * 0.4f)
                        lineTo(w * 0.8f, h * 0.6f)
                        lineTo(w * 0.6f, h * 0.6f)
                        lineTo(w * 0.6f, h * 0.8f)
                        lineTo(w * 0.4f, h * 0.8f)
                        lineTo(w * 0.4f, h * 0.6f)
                        lineTo(w * 0.2f, h * 0.6f)
                        lineTo(w * 0.2f, h * 0.4f)
                        lineTo(w * 0.4f, h * 0.4f)
                        close()
                    }
                    drawPath(crossPath, color = mainColor, style = Fill)
                }
            }
        }
    }
}
