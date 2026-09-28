package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {

    // ============================================================
    // MEDASSIST AI THEME
    // ============================================================

    val backgroundTop = Color(0xFFF7FCFF)
    val backgroundBottom = Color(0xFFE8F7FA)

    val primaryBlue = Color(0xFF1976D2)
    val primaryBlueDark = Color(0xFF123A56)

    val teal = Color(0xFF009688)
    val softTeal = Color(0xFF4DB6AC)

    val textGray = Color(0xFF71818C)
    val borderColor = Color(0xFFDDECEF)


    // ============================================================
    // ANIMATION
    // ============================================================

    var showContent by remember {
        mutableStateOf(false)
    }

    val logoScale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0.72f,
        animationSpec = tween(
            durationMillis = 700,
            easing = FastOutSlowInEasing
        ),
        label = "SplashScale"
    )

    val infiniteTransition = rememberInfiniteTransition(
        label = "SplashAnimation"
    )

    val botFloat by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BotFloat"
    )

    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowScale"
    )


    // ============================================================
    // SPLASH TIMER
    // ============================================================

    LaunchedEffect(Unit) {

        showContent = true

        delay(2500)

        onSplashFinished()
    }


    // ============================================================
    // ROOT
    // ============================================================

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        backgroundTop,
                        backgroundBottom
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {


        // ========================================================
        // TOP RIGHT GLOW
        // ========================================================

        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.TopEnd)
                .scale(glowScale)
                .background(
                    color = primaryBlue.copy(alpha = 0.06f),
                    shape = CircleShape
                )
        )


        // ========================================================
        // BOTTOM LEFT GLOW
        // ========================================================

        Box(
            modifier = Modifier
                .size(190.dp)
                .align(Alignment.BottomStart)
                .scale(glowScale)
                .background(
                    color = teal.copy(alpha = 0.055f),
                    shape = CircleShape
                )
        )


        // ========================================================
        // MAIN CONTENT
        // ========================================================

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 22.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {


            // ====================================================
            // AI DOCTOR BOT CARD
            // ====================================================

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    tween(650)
                ) + scaleIn(
                    initialScale = 0.72f,
                    animationSpec = tween(
                        durationMillis = 700,
                        easing = FastOutSlowInEasing
                    )
                )
            ) {

                Box(
                    modifier = Modifier
                        .size(205.dp)
                        .scale(logoScale),
                    contentAlignment = Alignment.Center
                ) {

                    // Soft glow behind bot

                    Box(
                        modifier = Modifier
                            .size(195.dp)
                            .scale(glowScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        primaryBlue.copy(
                                            alpha = 0.15f
                                        ),
                                        teal.copy(
                                            alpha = 0.07f
                                        ),
                                        Color.Transparent
                                    )
                                )
                            )
                    )


                    // Bot container

                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .clip(
                                RoundedCornerShape(38.dp)
                            )
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        primaryBlue,
                                        Color(0xFF1976B8),
                                        teal
                                    )
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                color = Color.White.copy(
                                    alpha = 0.55f
                                ),
                                shape = RoundedCornerShape(38.dp)
                            )
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally,
                            modifier = Modifier
                                .scale(1f)
                        ) {

                            // Antenna

                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(15.dp)
                                    .background(
                                        Color.White.copy(
                                            alpha = 0.9f
                                        )
                                    )
                            )

                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Color(0xFF9CFFF0)
                                    )
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(5.dp)
                            )


                            // Robot head

                            Box(
                                modifier = Modifier
                                    .size(
                                        width = 92.dp,
                                        height = 76.dp
                                    )
                                    .clip(
                                        RoundedCornerShape(
                                            25.dp
                                        )
                                    )
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White,
                                                Color(0xFFDDF2F8)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = Color.White.copy(
                                            alpha = 0.85f
                                        ),
                                        shape =
                                            RoundedCornerShape(
                                                25.dp
                                            )
                                    ),
                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Column(
                                    horizontalAlignment =
                                        Alignment.CenterHorizontally
                                ) {

                                    // Eyes

                                    Row(
                                        horizontalArrangement =
                                            Arrangement.spacedBy(
                                                20.dp
                                            )
                                    ) {

                                        Box(
                                            modifier = Modifier
                                                .size(13.dp)
                                                .clip(
                                                    CircleShape
                                                )
                                                .background(
                                                    primaryBlue
                                                )
                                        )

                                        Box(
                                            modifier = Modifier
                                                .size(13.dp)
                                                .clip(
                                                    CircleShape
                                                )
                                                .background(
                                                    primaryBlue
                                                )
                                        )
                                    }

                                    Spacer(
                                        modifier =
                                            Modifier.height(8.dp)
                                    )

                                    // Smile

                                    Box(
                                        modifier = Modifier
                                            .width(31.dp)
                                            .height(5.dp)
                                            .clip(
                                                RoundedCornerShape(
                                                    50
                                                )
                                            )
                                            .background(
                                                teal
                                            )
                                    )
                                }


                                // Medical cross

                                Box(
                                    modifier = Modifier
                                        .align(
                                            Alignment.TopEnd
                                        )
                                        .padding(7.dp)
                                        .size(22.dp)
                                        .clip(
                                            CircleShape
                                        )
                                        .background(
                                            teal
                                        ),
                                    contentAlignment =
                                        Alignment.Center
                                ) {

                                    Text(
                                        text = "+",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight =
                                            FontWeight.Bold
                                    )
                                }
                            }


                            Spacer(
                                modifier =
                                    Modifier.height(5.dp)
                            )


                            // Bot body

                            Box(
                                modifier = Modifier
                                    .size(
                                        width = 72.dp,
                                        height = 31.dp
                                    )
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 18.dp,
                                            topEnd = 18.dp,
                                            bottomStart = 9.dp,
                                            bottomEnd = 9.dp
                                        )
                                    )
                                    .background(
                                        Color.White.copy(
                                            alpha = 0.94f
                                        )
                                    )
                            ) {

                                Box(
                                    modifier = Modifier
                                        .align(
                                            Alignment.Center
                                        )
                                        .width(27.dp)
                                        .height(4.dp)
                                        .clip(
                                            RoundedCornerShape(
                                                50
                                            )
                                        )
                                        .background(
                                            primaryBlue
                                        )
                                )
                            }
                        }
                    }


                    // Floating medical plus

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(
                                top = 15.dp,
                                end = 5.dp
                            )
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Color.White
                            )
                            .border(
                                1.dp,
                                borderColor,
                                CircleShape
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text = "✚",
                            color = teal,
                            fontSize = 19.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }


            Spacer(
                modifier = Modifier.height(23.dp)
            )


            // ====================================================
            // APP NAME
            // ====================================================

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    tween(
                        durationMillis = 650,
                        delayMillis = 150
                    )
                ) + slideInVertically(
                    initialOffsetY = { 20 },
                    animationSpec = tween(
                        durationMillis = 650,
                        delayMillis = 150
                    )
                )
            ) {

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "MEDASSIST",
                        color = primaryBlueDark,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )

                    Text(
                        text = "AI",
                        color = teal,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }


            Spacer(
                modifier = Modifier.height(7.dp)
            )


            // ====================================================
            // TAGLINE
            // ====================================================

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    tween(
                        durationMillis = 650,
                        delayMillis = 250
                    )
                )
            ) {

                Text(
                    text = "Your intelligent health companion",
                    color = textGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }


            Spacer(
                modifier = Modifier.height(38.dp)
            )


            // ====================================================
            // STATUS
            // ====================================================

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    tween(
                        durationMillis = 650,
                        delayMillis = 400
                    )
                )
            ) {

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    LinearProgressIndicator(
                        modifier = Modifier
                            .width(130.dp)
                            .height(4.dp),
                        color = primaryBlue,
                        trackColor =
                            primaryBlue.copy(
                                alpha = 0.12f
                            )
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = "Preparing your AI health assistant...",
                        color = textGray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }


            Spacer(
                modifier = Modifier.height(36.dp)
            )


            // ====================================================
            // SECURITY BADGE
            // ====================================================

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    tween(
                        durationMillis = 650,
                        delayMillis = 550
                    )
                )
            ) {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.Center
                ) {

                    Box(
                        modifier = Modifier
                            .size(31.dp)
                            .clip(CircleShape)
                            .background(
                                Color.White.copy(
                                    alpha = 0.85f
                                )
                            )
                            .border(
                                1.dp,
                                borderColor,
                                CircleShape
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Lock,
                            contentDescription =
                                "Secure",
                            tint = primaryBlue,
                            modifier =
                                Modifier.size(15.dp)
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text(
                        text =
                            "Secure • Simple • Intelligent",
                        color = textGray,
                        fontSize = 10.sp,
                        fontWeight =
                            FontWeight.Medium
                    )
                }
            }
        }
    }
}