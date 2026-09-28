package ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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

@Composable
fun ForgotPasswordScreen(
    onBackToLogin: () -> Unit
) {
    var email by remember {
        mutableStateOf("")
    }

    var showContent by remember {
        mutableStateOf(true)
    }

    BackHandler {
        onBackToLogin()
    }

    // ============================================================
    // MEDASSIST AI THEME
    // ============================================================

    val backgroundTop = Color(0xFFF7FCFF)
    val backgroundBottom = Color(0xFFE8F7FA)

    val primaryBlue = Color(0xFF1976D2)
    val primaryBlueDark = Color(0xFF123A56)

    val teal = Color(0xFF009688)

    val textGray = Color(0xFF71818C)
    val borderColor = Color(0xFFDDECEF)

    val infiniteTransition = rememberInfiniteTransition(
        label = "ForgotPasswordAnimation"
    )

    val botScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BotScale"
    )

    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowScale"
    )

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
        // BACKGROUND GLOW
        // ========================================================

        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.TopEnd)
                .scale(glowScale)
                .clip(CircleShape)
                .background(
                    primaryBlue.copy(alpha = 0.055f)
                )
        )

        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.BottomStart)
                .scale(glowScale)
                .clip(CircleShape)
                .background(
                    teal.copy(alpha = 0.05f)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 18.dp,
                    vertical = 12.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            // ====================================================
            // BACK BUTTON
            // ====================================================

            TextButton(
                onClick = onBackToLogin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "←  Back to Login",
                    modifier = Modifier.fillMaxWidth(),
                    color = primaryBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            // ====================================================
            // AI DOCTOR HERO
            // ====================================================

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    tween(500)
                ) + scaleIn(
                    initialScale = 0.85f,
                    animationSpec = tween(550)
                )
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(225.dp)
                        .clip(
                            RoundedCornerShape(30.dp)
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
                ) {

                    // Decorative circle

                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(
                                Color.White.copy(
                                    alpha = 0.07f
                                )
                            )
                    )

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .align(Alignment.BottomStart)
                            .clip(CircleShape)
                            .background(
                                Color.White.copy(
                                    alpha = 0.06f
                                )
                            )
                    )

                    // =================================================
                    // LEFT CONTENT
                    // =================================================

                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(
                                start = 20.dp,
                                end = 145.dp
                            )
                    ) {

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Color.White.copy(
                                            alpha = 0.15f
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(
                                            alpha = 0.25f
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Text(
                                    text = "✚",
                                    color = Color.White,
                                    fontSize = 19.sp,
                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }

                            Spacer(
                                modifier = Modifier.width(9.dp)
                            )

                            Column {

                                Text(
                                    text = "MEDASSIST",
                                    color = Color.White,
                                    fontSize = 19.sp,
                                    fontWeight =
                                        FontWeight.ExtraBold
                                )

                                Text(
                                    text = "AI HEALTH ASSISTANT",
                                    color = Color(0xFFB9FFF5),
                                    fontSize = 7.sp,
                                    fontWeight =
                                        FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Text(
                            text = "Forgot your password?",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(5.dp)
                        )

                        Text(
                            text =
                                "Don't worry.\nWe'll help you get back in.",
                            color = Color.White.copy(
                                alpha = 0.90f
                            ),
                            fontSize = 11.sp,
                            lineHeight = 17.sp
                        )

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(5.dp)
                        ) {

                            HeroBadge(
                                text = "AI POWERED"
                            )

                            HeroBadge(
                                text = "SECURE"
                            )
                        }
                    }

                    // =================================================
                    // AI DOCTOR BOT
                    // =================================================

                    ForgotPasswordBot(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 14.dp)
                            .scale(botScale)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(17.dp)
            )

            // ====================================================
            // RESET CARD
            // ====================================================

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    tween(
                        durationMillis = 600,
                        delayMillis = 100
                    )
                ) + slideInVertically(
                    initialOffsetY = { it / 6 },
                    animationSpec = tween(
                        durationMillis = 600,
                        delayMillis = 100
                    )
                )
            ) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(27.dp),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            Color.White.copy(
                                alpha = 0.97f
                            )
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 5.dp
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = borderColor
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(
                            horizontal = 20.dp,
                            vertical = 22.dp
                        )
                    ) {

                        Text(
                            text = "Reset Password",
                            color = primaryBlueDark,
                            fontSize = 25.sp,
                            fontWeight =
                                FontWeight.ExtraBold
                        )

                        Spacer(
                            modifier = Modifier.height(5.dp)
                        )

                        Text(
                            text =
                                "Enter your registered email address and we'll help you reset your password.",
                            color = textGray,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(
                            modifier = Modifier.height(19.dp)
                        )

                        // =================================================
                        // EMAIL
                        // =================================================

                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    "Email address"
                                )
                            },
                            placeholder = {
                                Text(
                                    "Enter your registered email"
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector =
                                        Icons.Default.Email,
                                    contentDescription =
                                        "Email",
                                    tint =
                                        primaryBlue
                                )
                            },
                            singleLine = true,
                            shape =
                                RoundedCornerShape(16.dp),
                            colors =
                                OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor =
                                        primaryBlue,
                                    unfocusedBorderColor =
                                        borderColor,
                                    focusedLabelColor =
                                        primaryBlue,
                                    cursorColor =
                                        primaryBlue
                                )
                        )

                        Spacer(
                            modifier = Modifier.height(17.dp)
                        )

                        // =================================================
                        // SEND BUTTON
                        // =================================================

                        Button(
                            onClick = {
                                // Keep existing
                                // reset-password flow here.
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            enabled = email.isNotBlank(),
                            shape =
                                RoundedCornerShape(16.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor =
                                        primaryBlue,
                                    disabledContainerColor =
                                        primaryBlue.copy(
                                            alpha = 0.40f
                                        )
                                )
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Lock,
                                contentDescription =
                                    null,
                                modifier =
                                    Modifier.size(18.dp)
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(8.dp)
                            )

                            Text(
                                text =
                                    "Send Reset Link  →",
                                fontSize = 15.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(14.dp)
                        )

                        Text(
                            text =
                                "We'll send password-reset instructions to your registered email.",
                            color = textGray,
                            fontSize = 10.sp,
                            textAlign =
                                TextAlign.Center,
                            modifier =
                                Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(17.dp)
            )

            // ====================================================
            // SECURITY INFO
            // ====================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.Center,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(30.dp)
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
                            Modifier.size(14.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.width(7.dp)
                )

                Text(
                    text =
                        "Your account security is protected.",
                    color = textGray,
                    fontSize = 10.sp
                )
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )
        }
    }
}


/* ================================================================
   AI DOCTOR BOT
   ================================================================ */

@Composable
private fun ForgotPasswordBot(
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {

        // Bot glow

        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(
                                alpha = 0.17f
                            ),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            // Antenna

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(12.dp)
                    .background(
                        Color.White.copy(
                            alpha = 0.9f
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        Color(0xFF9CFFF0)
                    )
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            // Robot head

            Box(
                modifier = Modifier
                    .size(
                        width = 80.dp,
                        height = 66.dp
                    )
                    .clip(
                        RoundedCornerShape(22.dp)
                    )
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White,
                                Color(0xFFDCEFF8)
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        color = Color.White.copy(
                            alpha = 0.8f
                        ),
                        shape =
                            RoundedCornerShape(22.dp)
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(17.dp)
                    ) {

                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(
                                    Color(0xFF1976D2)
                                )
                        )

                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(
                                    Color(0xFF1976D2)
                                )
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(7.dp)
                    )

                    Box(
                        modifier = Modifier
                            .width(27.dp)
                            .height(4.dp)
                            .clip(
                                RoundedCornerShape(50)
                            )
                            .background(
                                Color(0xFF009688)
                            )
                    )
                }

                // Medical cross

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(19.dp)
                        .clip(CircleShape)
                        .background(
                            Color(0xFF009688)
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text = "+",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            // Body

            Box(
                modifier = Modifier
                    .size(
                        width = 62.dp,
                        height = 27.dp
                    )
                    .clip(
                        RoundedCornerShape(
                            topStart = 15.dp,
                            topEnd = 15.dp,
                            bottomStart = 8.dp,
                            bottomEnd = 8.dp
                        )
                    )
                    .background(
                        Color.White.copy(
                            alpha = 0.93f
                        )
                    )
            ) {

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(23.dp)
                        .height(3.dp)
                        .clip(
                            RoundedCornerShape(50)
                        )
                        .background(
                            Color(0xFF1976D2)
                        )
                )
            }
        }
    }
}


/* ================================================================
   HERO BADGE
   ================================================================ */

@Composable
private fun HeroBadge(
    text: String
) {

    Box(
        modifier = Modifier
            .clip(
                RoundedCornerShape(50)
            )
            .background(
                Color.White.copy(
                    alpha = 0.12f
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(
                    alpha = 0.18f
                ),
                shape =
                    RoundedCornerShape(50)
            )
            .padding(
                horizontal = 7.dp,
                vertical = 4.dp
            )
    ) {

        Text(
            text = text,
            color = Color.White.copy(
                alpha = 0.92f
            ),
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp
        )
    }
}