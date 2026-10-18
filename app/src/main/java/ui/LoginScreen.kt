package ui

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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

import com.example.ai_based_medical_chatbot.LocalAppLanguageController
import com.example.ai_based_medical_chatbot.appText
import com.example.ai_based_medical_chatbot.ui.theme.DarkTeal
import com.example.ai_based_medical_chatbot.ui.theme.DeepTeal
import com.example.ai_based_medical_chatbot.ui.theme.Ivory
import com.example.ai_based_medical_chatbot.ui.theme.Mint
import com.example.ai_based_medical_chatbot.ui.theme.PureWhite
import com.example.ai_based_medical_chatbot.ui.theme.SageGreen
import com.example.ai_based_medical_chatbot.ui.theme.SoftMint
import com.example.ai_based_medical_chatbot.ui.theme.StatusError
import com.example.ai_based_medical_chatbot.ui.theme.StatusErrorBackground
import com.example.ai_based_medical_chatbot.ui.theme.StatusErrorBorder
import com.example.ai_based_medical_chatbot.ui.theme.TextDark
import com.example.ai_based_medical_chatbot.ui.theme.TextMuted

@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    isLoading: Boolean = false,
    loginError: String = "",
    biometricMode: Boolean = false,
    onBiometricClick: () -> Unit = {},
    onUsePasswordClick: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }

    val appLanguageController = LocalAppLanguageController.current
    val selectedLanguage = appLanguageController.selectedLanguage

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    val infiniteTransition = rememberInfiniteTransition(
        label = "aiDoctorAnimation"
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
        label = "botScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Ivory,
                        SoftMint.copy(alpha = 0.5f),
                        Ivory
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {

        // Top ambient accent
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(Mint.copy(alpha = 0.35f))
        )

        // Bottom ambient accent
        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.BottomStart)
                .clip(CircleShape)
                .background(SoftMint.copy(alpha = 0.60f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(
                    horizontal = 18.dp,
                    vertical = 12.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(4.dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    tween(500)
                ) + scaleIn(
                    tween(500)
                )
            ) {
                AIDoctorHero(
                    biometricMode = biometricMode,
                    botScale = botScale,
                    selectedLanguage = selectedLanguage
                )
            }

            Spacer(modifier = Modifier.height(17.dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    tween(
                        durationMillis = 600,
                        delayMillis = 100
                    )
                ) + slideInVertically(
                    initialOffsetY = { it / 5 },
                    animationSpec = tween(
                        durationMillis = 600,
                        delayMillis = 100
                    )
                )
            ) {

                if (biometricMode) {

                    SecureLoginCard(
                        onBiometricClick = onBiometricClick,
                        onUsePasswordClick = onUsePasswordClick,
                        isLoading = isLoading,
                        error = loginError,
                        selectedLanguage = selectedLanguage
                    )

                } else {

                    LoginFormCard(
                        email = email,
                        onEmailChange = { email = it },
                        password = password,
                        onPasswordChange = { password = it },
                        passwordVisible = passwordVisible,
                        onPasswordVisibilityChange = {
                            passwordVisible = it
                        },
                        onLoginClick = {
                            if (
                                email.isNotBlank() &&
                                password.isNotBlank()
                            ) {
                                onLoginClick(
                                    email.trim(),
                                    password
                                )
                            }
                        },
                        onRegisterClick = onRegisterClick,
                        onForgotPasswordClick = onForgotPasswordClick,
                        isLoading = isLoading,
                        loginError = loginError,
                        selectedLanguage = selectedLanguage
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!biometricMode) {
                SecurityFooter(
                    primary = DeepTeal,
                    textSecondary = TextMuted,
                    selectedLanguage = selectedLanguage
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}


/* ============================================================
   AI DOCTOR HERO
   ============================================================ */

@Composable
private fun AIDoctorHero(
    biometricMode: Boolean,
    botScale: Float,
    selectedLanguage: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(
                if (biometricMode) {
                    245.dp
                } else {
                    225.dp
                }
            )
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        DarkTeal,
                        DeepTeal,
                        SageGreen
                    )
                )
            )
    ) {

        // Soft background circles
        Box(
            modifier = Modifier
                .size(145.dp)
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(
                    PureWhite.copy(alpha = 0.08f)
                )
        )

        Box(
            modifier = Modifier
                .size(95.dp)
                .align(Alignment.BottomStart)
                .clip(CircleShape)
                .background(
                    PureWhite.copy(alpha = 0.06f)
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(
                    start = 20.dp,
                    end = 140.dp
                )
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(37.dp)
                        .clip(CircleShape)
                        .background(
                            PureWhite.copy(alpha = 0.16f)
                        )
                        .border(
                            1.dp,
                            PureWhite.copy(alpha = 0.25f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "✚",
                        color = PureWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(9.dp))

                Column(
                    modifier = Modifier.width(118.dp)
                ) {

                    // Keep the complete brand name on one line.
                    Text(
                        text = "MEDASSIST AI",
                        color = PureWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.2.sp,
                        maxLines = 1,
                        softWrap = false
                    )

                    Text(
                        text = "AI HEALTH ASSISTANT",
                        color = Mint,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (biometricMode) {
                    appText("login_welcome_back_short", selectedLanguage)
                } else {
                    appText("login_hero_companion", selectedLanguage)
                },
                color = PureWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                SmallHeroBadge(appText("login_ai_powered", selectedLanguage))

                Spacer(modifier = Modifier.width(6.dp))

                SmallHeroBadge(appText("login_24_7_care", selectedLanguage))
            }
        }

        // AI Doctor Bot
        AIDoctorBot(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 17.dp)
                .scale(botScale)
        )
    }
}


/* ============================================================
   AI DOCTOR BOT
   ============================================================ */

@Composable
private fun AIDoctorBot(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(125.dp),
        contentAlignment = Alignment.Center
    ) {

        // Bot glow
        Box(
            modifier = Modifier
                .size(115.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PureWhite.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Antenna
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(13.dp)
                    .background(
                        PureWhite.copy(alpha = 0.9f)
                    )
            )

            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(Mint)
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Robot head
            Box(
                modifier = Modifier
                    .size(82.dp, 68.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                PureWhite,
                                SoftMint
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        color = PureWhite.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(23.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {

                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    DarkTeal
                                )
                        )

                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    DarkTeal
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(7.dp))

                    Box(
                        modifier = Modifier
                            .width(30.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                DeepTeal
                            )
                    )
                }

                // Medical cross
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            DeepTeal
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "+",
                        color = PureWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Body
            Box(
                modifier = Modifier
                    .size(65.dp, 28.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 8.dp,
                            bottomEnd = 8.dp
                        )
                    )
                    .background(
                        PureWhite.copy(alpha = 0.92f)
                    )
            ) {

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(24.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            DeepTeal
                        )
                )
            }
        }
    }
}


/* ============================================================
   HERO BADGE
   ============================================================ */

@Composable
private fun SmallHeroBadge(
    text: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                PureWhite.copy(alpha = 0.14f)
            )
            .border(
                width = 1.dp,
                color = PureWhite.copy(alpha = 0.22f),
                shape = RoundedCornerShape(50)
            )
            .padding(
                horizontal = 8.dp,
                vertical = 4.dp
            )
    ) {

        Text(
            text = text,
            color = PureWhite.copy(alpha = 0.95f),
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}


/* ============================================================
   LOGIN FORM
   ============================================================ */

@Composable
private fun LoginFormCard(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: (Boolean) -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    isLoading: Boolean,
    loginError: String,
    selectedLanguage: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = PureWhite
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        border = BorderStroke(
            width = 1.dp,
            color = Mint
        )
    ) {

        Column(
            modifier = Modifier.padding(
                horizontal = 20.dp,
                vertical = 22.dp
            )
        ) {
            val passwordFocusRequester = remember { FocusRequester() }

            Text(
                text = appText("login_welcome_back", selectedLanguage),
                color = DarkTeal,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = appText("login_sign_in_subtitle", selectedLanguage),
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { passwordFocusRequester.requestFocus() }
                ),
                label = {
                    Text(appText("login_email_label", selectedLanguage))
                },
                placeholder = {
                    Text(appText("login_email_placeholder", selectedLanguage))
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = appText("login_email_label", selectedLanguage),
                        tint = DeepTeal
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepTeal,
                    unfocusedBorderColor = Mint,
                    focusedLabelColor = DeepTeal,
                    unfocusedLabelColor = TextMuted,
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark,
                    cursorColor = DeepTeal
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(passwordFocusRequester),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (!isLoading && email.isNotBlank() && password.isNotBlank()) {
                            onLoginClick()
                        }
                    }
                ),
                label = {
                    Text(appText("login_password_label", selectedLanguage))
                },
                placeholder = {
                    Text(appText("login_password_placeholder", selectedLanguage))
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = appText("login_password_label", selectedLanguage),
                        tint = DeepTeal
                    )
                },
                trailingIcon = {
                    TextButton(
                        onClick = {
                            onPasswordVisibilityChange(
                                !passwordVisible
                            )
                        }
                    ) {

                        Text(
                            text = if (passwordVisible) {
                                appText("login_hide", selectedLanguage)
                            } else {
                                appText("login_show", selectedLanguage)
                            },
                            color = DeepTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                visualTransformation =
                    if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepTeal,
                    unfocusedBorderColor = Mint,
                    focusedLabelColor = DeepTeal,
                    unfocusedLabelColor = TextMuted,
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark,
                    cursorColor = DeepTeal
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {

                TextButton(
                    onClick = onForgotPasswordClick
                ) {

                    Text(
                        text = appText("login_forgot_password", selectedLanguage),
                        color = DarkTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (loginError.isNotBlank()) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            StatusErrorBackground
                        )
                        .border(
                            width = 1.dp,
                            color = StatusErrorBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(10.dp)
                ) {

                    Text(
                        text = loginError,
                        color = StatusError,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled =
                    !isLoading &&
                            email.isNotBlank() &&
                            password.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepTeal,
                    disabledContainerColor = DeepTeal.copy(
                        alpha = 0.40f
                    )
                )
            ) {

                if (isLoading) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(21.dp),
                        color = PureWhite,
                        strokeWidth = 2.dp
                    )

                } else {

                    Text(
                        text = appText("login_sign_in", selectedLanguage) + "  →",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(15.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            Mint
                        )
                )

                Text(
                    text = "  ${appText("login_or", selectedLanguage)}  ",
                    color = TextMuted,
                    fontSize = 11.sp
                )

                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            Mint
                        )
                )
            }

            Spacer(modifier = Modifier.height(13.dp))

            OutlinedButton(
                onClick = onRegisterClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = 1.2.dp,
                    color = DeepTeal
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = DeepTeal
                )
            ) {

                Text(
                    text = "＋  " + appText("login_create_account", selectedLanguage),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = appText("login_already_account", selectedLanguage),
                    color = TextMuted,
                    fontSize = 11.sp
                )

                TextButton(
                    onClick = onRegisterClick
                ) {

                    Text(
                        text = appText("login_register", selectedLanguage),
                        color = DeepTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


/* ============================================================
   SECURE LOGIN CARD
   ============================================================ */

@Composable
private fun SecureLoginCard(
    onBiometricClick: () -> Unit,
    onUsePasswordClick: () -> Unit,
    isLoading: Boolean,
    error: String,
    selectedLanguage: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = PureWhite
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        border = BorderStroke(
            width = 1.dp,
            color = Mint
        )
    ) {

        Column(
            modifier = Modifier.padding(
                horizontal = 22.dp,
                vertical = 24.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                DarkTeal,
                                DeepTeal
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = appText("login_secure_login", selectedLanguage),
                    tint = PureWhite,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(15.dp))

            Text(
                text = appText("login_secure_login", selectedLanguage),
                color = DarkTeal,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = appText("login_secure_welcome", selectedLanguage),
                color = DeepTeal,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(7.dp))

            Text(
                text = appText("login_secure_methods", selectedLanguage),
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onBiometricClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepTeal
                )
            ) {

                if (isLoading) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(21.dp),
                        color = PureWhite,
                        strokeWidth = 2.dp
                    )

                } else {

                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = appText("login_unlock_securely", selectedLanguage),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(11.dp))

            OutlinedButton(
                onClick = onUsePasswordClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = 1.1.dp,
                    color = Mint
                )
            ) {

                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = DeepTeal
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = appText("login_use_password", selectedLanguage),
                    color = DarkTeal,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (error.isNotBlank()) {

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            StatusErrorBackground
                        )
                        .border(
                            width = 1.dp,
                            color = StatusErrorBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(10.dp)
                ) {

                    Text(
                        text = error,
                        color = StatusError,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(15.dp))

            Text(
                text = appText("login_biometric_privacy", selectedLanguage),
                color = TextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}


/* ============================================================
   FOOTER
   ============================================================ */

@Composable
private fun SecurityFooter(
    primary: Color,
    textSecondary: Color,
    selectedLanguage: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = primary.copy(alpha = 0.75f),
            modifier = Modifier.size(13.dp)
        )

        Spacer(modifier = Modifier.width(5.dp))

        Text(
            text = appText("login_footer", selectedLanguage),
            color = textSecondary,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}