package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    var showContent by remember {
        mutableStateOf(false)
    }

    val logoScale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0.6f,
        animationSpec = tween(
            durationMillis = 700,
            easing = FastOutSlowInEasing
        ),
        label = "logoScale"
    )

    LaunchedEffect(Unit) {
        showContent = true

        delay(2500)

        onSplashFinished()
    }

    val background = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF06152B),
            Color(0xFF082A46),
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

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    animationSpec = tween(700)
                ) + scaleIn(
                    initialScale = 0.6f,
                    animationSpec = tween(700)
                )
            ) {

                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(logoScale)
                        .background(
                            color = Color.White.copy(alpha = 0.12f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "MedAssist AI Logo",
                        tint = Color(0xFF7DE8F0),
                        modifier = Modifier.size(58.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 700,
                        delayMillis = 250
                    )
                )
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "MedAssist AI",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "Your intelligent health companion",
                        color = Color(0xFFB9EAF2),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(40.dp)
            )

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 700,
                        delayMillis = 500
                    )
                )
            ) {

                LinearProgressIndicator(
                    modifier = Modifier.size(
                        width = 100.dp,
                        height = 3.dp
                    ),
                    color = Color(0xFF7DE8F0),
                    trackColor = Color.White.copy(alpha = 0.15f)
                )
            }
        }
    }
}