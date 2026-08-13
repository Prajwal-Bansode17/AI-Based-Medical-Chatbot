package com.example.ai_based_medical_chatbot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_based_medical_chatbot.ui.theme.AIBasedMedicalChatbotTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AIBasedMedicalChatbotTheme {
                SplashScreen()
            }
        }
    }
}

@Composable
fun SplashScreen() {

    // Logo animation
    val infiniteTransition = rememberInfiniteTransition(label = "logoAnimation")

    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )

    // Background
    val background = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF071A33),
            Color(0xFF0A2D4F),
            Color(0xFF064E63)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Logo
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(logoScale)
                    .background(
                        color = Color.White,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                MedicalAiLogo()
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App name
            Text(
                text = "MedAssist AI",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Your Intelligent Medical Assistant",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFFB9EAF2),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Loading indicator
            LoadingDots()
        }
    }
}

@Composable
fun MedicalAiLogo() {

    Canvas(
        modifier = Modifier.size(76.dp)
    ) {

        val centerX = size.width / 2
        val centerY = size.height / 2

        // Medical cross
        drawLine(
            color = Color(0xFF00A9E8),
            start = androidx.compose.ui.geometry.Offset(
                centerX,
                centerY - 22
            ),
            end = androidx.compose.ui.geometry.Offset(
                centerX,
                centerY + 22
            ),
            strokeWidth = 10f,
            cap = StrokeCap.Round
        )

        drawLine(
            color = Color(0xFF00A9E8),
            start = androidx.compose.ui.geometry.Offset(
                centerX - 22,
                centerY
            ),
            end = androidx.compose.ui.geometry.Offset(
                centerX + 22,
                centerY
            ),
            strokeWidth = 10f,
            cap = StrokeCap.Round
        )

        // AI circuit dots
        drawCircle(
            color = Color(0xFF00D4C7),
            radius = 5f,
            center = androidx.compose.ui.geometry.Offset(
                centerX + 25,
                centerY - 25
            )
        )

        drawCircle(
            color = Color(0xFF00D4C7),
            radius = 5f,
            center = androidx.compose.ui.geometry.Offset(
                centerX - 25,
                centerY + 25
            )
        )
    }
}

@Composable
fun LoadingDots() {

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        repeat(3) { index ->

            val infiniteTransition =
                rememberInfiniteTransition(label = "dotAnimation")

            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600,
                        delayMillis = index * 200
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dotAlpha"
            )

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = Color.White.copy(alpha = alpha),
                        shape = RoundedCornerShape(50)
                    )
            )
        }
    }
}