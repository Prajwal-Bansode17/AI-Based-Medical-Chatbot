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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_based_medical_chatbot.LocalAppLanguageController
import com.example.ai_based_medical_chatbot.appText
import com.example.ai_based_medical_chatbot.ui.theme.DarkTeal
import com.example.ai_based_medical_chatbot.ui.theme.DeepTeal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.rememberCoroutineScope
import com.example.ai_based_medical_chatbot.data.SupabaseClient
import com.example.ai_based_medical_chatbot.ui.theme.Ivory
import com.example.ai_based_medical_chatbot.ui.theme.Mint
import com.example.ai_based_medical_chatbot.ui.theme.PureWhite
import com.example.ai_based_medical_chatbot.ui.theme.SageGreen
import com.example.ai_based_medical_chatbot.ui.theme.SoftMint
import com.example.ai_based_medical_chatbot.ui.theme.StatusError
import com.example.ai_based_medical_chatbot.ui.theme.StatusErrorBackground
import com.example.ai_based_medical_chatbot.ui.theme.StatusErrorBorder
import com.example.ai_based_medical_chatbot.ui.theme.StatusSuccess
import com.example.ai_based_medical_chatbot.ui.theme.TextDark
import com.example.ai_based_medical_chatbot.ui.theme.TextMuted
import kotlinx.coroutines.launch

@Composable
fun ForgotPasswordScreen(
    onBackToLogin: () -> Unit
) {
    val appLanguageController = LocalAppLanguageController.current
    val selectedLanguage = appLanguageController.selectedLanguage
    val keyboardController = LocalSoftwareKeyboardController.current

    fun t(key: String): String = appText(key, selectedLanguage)

    val scope = rememberCoroutineScope()

    var email by remember {
        mutableStateOf("")
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var successMessage by remember {
        mutableStateOf("")
    }

    fun performResetPassword() {
        val cleanEmail = email.trim()
        if (isLoading) return

        keyboardController?.hide()
        errorMessage = ""
        successMessage = ""

        if (cleanEmail.isBlank()) {
            errorMessage = "Please enter your email address."
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            errorMessage = "Please enter a valid email address (e.g. name@example.com)."
            return
        }

        isLoading = true
        scope.launch {
            val result = SupabaseClient.sendPasswordResetEmail(cleanEmail)
            isLoading = false
            result.onSuccess {
                successMessage = "A password recovery email has been sent to $cleanEmail. Please check your inbox and spam folder."
                errorMessage = ""
            }.onFailure { ex ->
                errorMessage = ex.message?.takeIf { it.isNotBlank() }
                    ?: "Failed to send reset email. Please try again."
                successMessage = ""
            }
        }
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
                        Ivory,
                        SoftMint.copy(alpha = 0.5f),
                        Ivory
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
                    Mint.copy(alpha = 0.35f)
                )
        )

        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.BottomStart)
                .scale(glowScale)
                .clip(CircleShape)
                .background(
                    SoftMint.copy(alpha = 0.60f)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .imePadding()
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
                    text = t("forgot_back_login"),
                    modifier = Modifier.fillMaxWidth(),
                    color = DeepTeal,
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
                            RoundedCornerShape(26.dp)
                        )
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

                    // Decorative circle

                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(
                                PureWhite.copy(
                                    alpha = 0.08f
                                )
                            )
                    )

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .align(Alignment.BottomStart)
                            .clip(CircleShape)
                            .background(
                                PureWhite.copy(
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
                                        PureWhite.copy(
                                            alpha = 0.16f
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = PureWhite.copy(
                                            alpha = 0.25f
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Text(
                                    text = "✚",
                                    color = PureWhite,
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
                                    color = PureWhite,
                                    fontSize = 19.sp,
                                    fontWeight =
                                        FontWeight.ExtraBold
                                )

                                Text(
                                    text = t("forgot_health_assistant"),
                                    color = Mint,
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
                            text = t("forgot_title"),
                            color = PureWhite,
                            fontSize = 14.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(5.dp)
                        )

                        Text(
                            text =
                                t("forgot_hero_message"),
                            color = PureWhite.copy(
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
                                text = t("forgot_ai_powered")
                            )

                            HeroBadge(
                                text = t("forgot_secure_badge")
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
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = PureWhite
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    ),
                    border = androidx.compose.foundation.BorderStroke(
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

                        Text(
                            text = t("forgot_reset_password"),
                            color = DarkTeal,
                            fontSize = 22.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(5.dp)
                        )

                        Text(
                            text =
                                t("forgot_reset_description"),
                            color = TextMuted,
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
                                if (errorMessage.isNotBlank()) errorMessage = ""
                                if (successMessage.isNotBlank()) successMessage = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    performResetPassword()
                                }
                            ),
                            label = {
                                Text(
                                    t("forgot_email_label")
                                )
                            },
                            placeholder = {
                                Text(
                                    t("forgot_email_placeholder")
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector =
                                        Icons.Default.Email,
                                    contentDescription =
                                        t("forgot_email_content"),
                                    tint =
                                        DeepTeal
                                )
                            },
                            singleLine = true,
                            shape =
                                RoundedCornerShape(16.dp),
                            colors =
                                OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor =
                                        DeepTeal,
                                    unfocusedBorderColor =
                                        Mint,
                                    focusedLabelColor =
                                        DeepTeal,
                                    unfocusedLabelColor =
                                        TextMuted,
                                    focusedTextColor =
                                        TextDark,
                                    unfocusedTextColor =
                                        TextDark,
                                    cursorColor =
                                        DeepTeal
                                )
                        )

                        // Error message banner
                        if (errorMessage.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = StatusErrorBackground
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StatusErrorBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = StatusError,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = errorMessage,
                                        color = StatusError,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        // Success message banner
                        if (successMessage.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = SoftMint
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Mint)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = StatusSuccess,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = successMessage,
                                        color = DarkTeal,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(17.dp)
                        )

                        // =================================================
                        // SEND BUTTON
                        // =================================================

                        Button(
                            onClick = {
                                performResetPassword()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            enabled = email.isNotBlank() && !isLoading,
                            shape =
                                RoundedCornerShape(16.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor =
                                        DeepTeal,
                                    disabledContainerColor =
                                        DeepTeal.copy(
                                            alpha = 0.40f
                                        )
                                )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = PureWhite,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sending...",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite
                                )
                            } else {
                                Icon(
                                    imageVector =
                                        Icons.Default.Lock,
                                    contentDescription =
                                        null,
                                    modifier =
                                        Modifier.size(18.dp),
                                    tint = PureWhite
                                )

                                Spacer(
                                    modifier =
                                        Modifier.width(8.dp)
                                )

                                Text(
                                    text =
                                        t("forgot_send_reset"),
                                    fontSize = 15.sp,
                                    fontWeight =
                                        FontWeight.Bold,
                                    color = PureWhite
                                )
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(14.dp)
                        )

                        Text(
                            text =
                                t("forgot_reset_note"),
                            color = TextMuted,
                            fontSize = 11.sp,
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
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            SoftMint
                        )
                        .border(
                            1.dp,
                            Mint,
                            CircleShape
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Lock,
                        contentDescription =
                            t("forgot_secure"),
                        tint = DeepTeal,
                        modifier =
                            Modifier.size(14.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.width(7.dp)
                )

                Text(
                    text =
                        t("forgot_security"),
                    color = TextMuted,
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
                            PureWhite.copy(
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
                        PureWhite.copy(
                            alpha = 0.9f
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        Mint
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
                                PureWhite,
                                SoftMint
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        color = PureWhite.copy(
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
                                    DarkTeal
                                )
                        )

                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(
                                    DarkTeal
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
                                DeepTeal
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
                            DeepTeal
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text = "+",
                        color = PureWhite,
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
                        PureWhite.copy(
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
                            DarkTeal
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
                PureWhite.copy(
                    alpha = 0.12f
                )
            )
            .border(
                width = 1.dp,
                color = PureWhite.copy(
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
            color = PureWhite.copy(
                alpha = 0.92f
            ),
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp
        )
    }
}