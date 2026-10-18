package ui

import android.util.Patterns
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
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_based_medical_chatbot.data.SupabaseClient
import kotlinx.coroutines.launch
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
import com.example.ai_based_medical_chatbot.ui.theme.StatusSuccess
import com.example.ai_based_medical_chatbot.ui.theme.TextDark
import com.example.ai_based_medical_chatbot.ui.theme.TextMuted

@Composable
fun RegisterScreen(
    onRegisterClick: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    onBackToLogin: () -> Unit
) {

    // ============================================================
    // INPUT STATE
    // ============================================================

    var fullName by remember {
        mutableStateOf("")
    }

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var confirmPassword by remember {
        mutableStateOf("")
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    var confirmPasswordVisible by remember {
        mutableStateOf(false)
    }

    // ============================================================
    // ERROR STATE
    // ============================================================

    var nameError by remember {
        mutableStateOf("")
    }

    var emailError by remember {
        mutableStateOf("")
    }

    var passwordError by remember {
        mutableStateOf("")
    }

    var confirmPasswordError by remember {
        mutableStateOf("")
    }

    var registerError by remember {
        mutableStateOf("")
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()

    val appLanguageController = LocalAppLanguageController.current
    val selectedLanguage = appLanguageController.selectedLanguage

    fun t(key: String): String = appText(key, selectedLanguage)

    fun displayRegisterMessage(message: String): String = when (message) {
        "Account created successfully!" -> t("register_success")
        "Registration failed. Please try again." -> t("register_failed")
        t("register_name_required") -> t("register_name_required")
        t("register_email_required") -> t("register_email_required")
        t("register_valid_email") -> t("register_valid_email")
        t("register_password_required") -> t("register_password_required")
        t("register_password_min") -> t("register_password_min")
        t("register_confirm_required") -> t("register_confirm_required")
        t("register_password_mismatch") -> t("register_password_mismatch")
        else -> message
    }

    val nameFocusRequester = remember { FocusRequester() }
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }

    val submitRegistration: () -> Unit = {
        if (!isLoading) {
            nameError = ""
            emailError = ""
            passwordError = ""
            confirmPasswordError = ""
            registerError = ""

            var valid = true

            // NAME
            if (fullName.trim().isEmpty()) {
                nameError = t("register_name_required")
                valid = false
            }

            // EMAIL
            val cleanEmail = email.trim()
            if (cleanEmail.isEmpty()) {
                emailError = t("register_email_required")
                valid = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                emailError = t("register_valid_email")
                valid = false
            }

            // PASSWORD
            if (password.isEmpty()) {
                passwordError = t("register_password_required")
                valid = false
            } else if (password.length < 6) {
                passwordError = t("register_password_min")
                valid = false
            }

            // CONFIRM PASSWORD
            if (confirmPassword.isEmpty()) {
                confirmPasswordError = t("register_confirm_required")
                valid = false
            } else if (password != confirmPassword) {
                confirmPasswordError = t("register_password_mismatch")
                valid = false
            }

            if (valid) {
                val registrationEmail = cleanEmail
                val registrationPassword = password
                val registrationName = fullName.trim()

                scope.launch {
                    isLoading = true
                    registerError = ""

                    try {
                        val result = SupabaseClient.registerUser(
                            email = registrationEmail,
                            password = registrationPassword,
                            fullName = registrationName
                        )

                        result.onSuccess {
                            isLoading = false
                            registerError = "Account created successfully!"
                            onRegisterClick()
                        }

                        result.onFailure { error ->
                            isLoading = false
                            registerError = error.message
                                ?: "Registration failed. Please try again."
                        }
                    } catch (e: Exception) {
                        isLoading = false
                        registerError = e.message
                            ?: "Registration failed. Please try again."
                    }
                }
            }
        }
    }


    // ============================================================
    // ANIMATION
    // ============================================================

    var showContent by remember {
        mutableStateOf(false)
    }

    val infiniteTransition = rememberInfiniteTransition(
        label = "registerAnimation"
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
        label = "glowScale"
    )

    androidx.compose.runtime.LaunchedEffect(Unit) {
        showContent = true
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
        // BACKGROUND ACCENT
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

        // ========================================================
        // MAIN CONTENT
        // ========================================================

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 18.dp,
                    vertical = 10.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ====================================================
            // BACK
            // ====================================================

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    tween(450)
                )
            ) {

                TextButton(
                    onClick = onBackToLogin,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        text = t("register_back_login"),
                        modifier = Modifier.fillMaxWidth(),
                        color = DeepTeal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            // ====================================================
            // AI DOCTOR HERO
            // ====================================================

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    tween(500)
                ) + scaleIn(
                    initialScale = 0.82f,
                    animationSpec = tween(
                        durationMillis = 550,
                        easing = FastOutSlowInEasing
                    )
                )
            ) {

                RegisterHero(
                    botScale = botScale
                )
            }

            Spacer(
                modifier = Modifier.height(17.dp)
            )

            // ====================================================
            // REGISTER CARD
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

                        Text(
                            text = t("register_create_account"),
                            color = DarkTeal,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = t("register_subtitle"),
                            color = TextMuted,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(
                            modifier = Modifier.height(18.dp)
                        )

                        // =================================================
                        // FULL NAME
                        // =================================================

                        RegisterField(
                            value = fullName,
                            onValueChange = {
                                fullName = it
                                nameError = ""
                                registerError = ""
                            },
                            label = t("register_full_name"),
                            placeholder = t("register_full_name_placeholder"),
                            leadingText = "✦",
                            isError = nameError.isNotEmpty(),
                            modifier = Modifier.focusRequester(nameFocusRequester),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { emailFocusRequester.requestFocus() }
                            )
                        )

                        if (nameError.isNotEmpty()) {
                            ErrorText(nameError)
                        }

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        // =================================================
                        // EMAIL
                        // =================================================

                        RegisterField(
                            value = email,
                            onValueChange = {
                                email = it
                                emailError = ""
                                registerError = ""
                            },
                            label = t("register_email"),
                            placeholder = t("register_email_placeholder"),
                            leadingText = "@",
                            isError = emailError.isNotEmpty(),
                            modifier = Modifier.focusRequester(emailFocusRequester),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { passwordFocusRequester.requestFocus() }
                            )
                        )

                        if (emailError.isNotEmpty()) {
                            ErrorText(emailError)
                        }

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        // =================================================
                        // PASSWORD
                        // =================================================

                        PasswordRegisterField(
                            value = password,
                            onValueChange = {
                                password = it
                                passwordError = ""
                                registerError = ""

                                confirmPasswordError =
                                    if (
                                        confirmPassword.isNotEmpty() &&
                                        confirmPassword != it
                                    ) {
                                        t("register_password_mismatch")
                                    } else {
                                        ""
                                    }
                            },
                            label = t("register_password"),
                            placeholder = t("register_password_placeholder"),
                            visible = passwordVisible,
                            onVisibilityChange = {
                                passwordVisible =
                                    !passwordVisible
                            },
                            isError = passwordError.isNotEmpty(),
                            selectedLanguage = selectedLanguage,
                            modifier = Modifier.focusRequester(passwordFocusRequester),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { confirmPasswordFocusRequester.requestFocus() }
                            )
                        )

                        if (passwordError.isNotEmpty()) {
                            ErrorText(passwordError)
                        }

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        // =================================================
                        // CONFIRM PASSWORD
                        // =================================================

                        PasswordRegisterField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                registerError = ""

                                confirmPasswordError =
                                    when {
                                        it.isEmpty() -> ""
                                        it != password ->
                                            t("register_password_mismatch")
                                        else -> ""
                                    }
                            },
                            label = t("register_confirm_password"),
                            placeholder = t("register_confirm_password_placeholder"),
                            visible = confirmPasswordVisible,
                            onVisibilityChange = {
                                confirmPasswordVisible =
                                    !confirmPasswordVisible
                            },
                            isError =
                                confirmPasswordError.isNotEmpty(),
                            selectedLanguage = selectedLanguage,
                            modifier = Modifier.focusRequester(confirmPasswordFocusRequester),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { submitRegistration() }
                            )
                        )

                        if (confirmPasswordError.isNotEmpty()) {
                            ErrorText(confirmPasswordError)
                        }

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        // =================================================
                        // PASSWORD REQUIREMENT
                        // =================================================

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (password.length >= 6) {
                                            DeepTeal
                                        } else {
                                            Mint
                                        }
                                    )
                            )

                            Spacer(
                                modifier = Modifier.width(8.dp)
                            )

                            Text(
                                text =
                                    t("register_password_requirement"),
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        // =================================================
                        // REGISTER ERROR / SUCCESS
                        // =================================================

                        if (registerError.isNotEmpty()) {

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            val success =
                                registerError ==
                                        "Account created successfully!"

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(
                                        RoundedCornerShape(12.dp)
                                    )
                                    .background(
                                        if (success) {
                                            SoftMint
                                        } else {
                                            StatusErrorBackground
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color =
                                            if (success) {
                                                Mint
                                            } else {
                                                StatusErrorBorder
                                            },
                                        shape =
                                            RoundedCornerShape(12.dp)
                                    )
                                    .padding(11.dp)
                            ) {

                                Text(
                                    text = displayRegisterMessage(registerError),
                                    color =
                                        if (success) {
                                            StatusSuccess
                                        } else {
                                            StatusError
                                        },
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    textAlign = TextAlign.Center,
                                    modifier =
                                        Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(19.dp)
                        )

                        // =================================================
                        // CREATE ACCOUNT BUTTON
                        // =================================================

                        Button(
                            onClick = { submitRegistration() },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepTeal,
                                disabledContainerColor =
                                    DeepTeal.copy(
                                        alpha = 0.40f
                                    )
                            )
                        ) {

                            if (isLoading) {

                                CircularProgressIndicator(
                                    modifier =
                                        Modifier.size(21.dp),
                                    color = PureWhite,
                                    strokeWidth = 2.dp
                                )

                            } else {

                                Text(
                                    text =
                                        t("register_create_button"),
                                    fontSize = 15.sp,
                                    fontWeight =
                                        FontWeight.Bold,
                                    color = PureWhite
                                )
                            }
                        }
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(15.dp)
            )

            // ====================================================
            // LOGIN LINK
            // ====================================================

            Row(
                horizontalArrangement =
                    Arrangement.Center,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text =
                        t("register_already"),
                    color = TextMuted,
                    fontSize = 12.sp
                )

                Spacer(
                    modifier = Modifier.width(4.dp)
                )

                Text(
                    text = t("register_login"),
                    color = DeepTeal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            if (!isLoading) {
                                onLoginClick()
                            }
                        }
                        .padding(5.dp)
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            // ====================================================
            // SECURITY
            // ====================================================

            Row(
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
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = DeepTeal,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.width(7.dp)
                )

                Text(
                    text =
                        t("register_security"),
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
   REGISTER HERO
   ================================================================ */

@Composable
private fun RegisterHero(
    botScale: Float
) {

    val selectedLanguage =
        LocalAppLanguageController.current.selectedLanguage

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(215.dp)
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

        // Decorative glow

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

        // Left content

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
                                alpha = 0.18f
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = PureWhite.copy(
                                alpha = 0.3f
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
                        text = "AI HEALTH ASSISTANT",
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
                text =
                    appText("register_hero_start", selectedLanguage),
                color = PureWhite,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(9.dp)
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(5.dp)
            ) {

                HeroBadge(appText("register_ai_powered", selectedLanguage))

                HeroBadge(appText("register_secure_badge", selectedLanguage))
            }
        }

        // AI Doctor Bot

        RegisterDoctorBot(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 13.dp)
                .scale(botScale)
        )
    }
}


/* ================================================================
   AI DOCTOR BOT
   ================================================================ */

@Composable
private fun RegisterDoctorBot(
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier.size(119.dp),
        contentAlignment = Alignment.Center
    ) {

        // Glow

        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PureWhite.copy(
                                alpha = 0.18f
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
                            alpha = 0.94f
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
                            DeepTeal
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
                    alpha = 0.14f
                )
            )
            .border(
                width = 1.dp,
                color = PureWhite.copy(
                    alpha = 0.22f
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
                alpha = 0.95f
            ),
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp
        )
    }
}


/* ================================================================
   NORMAL FIELD
   ================================================================ */

@Composable
private fun RegisterField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingText: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        label = {
            Text(label)
        },
        placeholder = {
            Text(placeholder)
        },
        leadingIcon = {

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        SoftMint
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = leadingText,
                    color = DeepTeal,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        singleLine = true,
        isError = isError,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
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
                DeepTeal,
            errorBorderColor =
                StatusError
        )
    )
}


/* ================================================================
   PASSWORD FIELD
   ================================================================ */

@Composable
private fun PasswordRegisterField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    visible: Boolean,
    onVisibilityChange: () -> Unit,
    isError: Boolean,
    selectedLanguage: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        label = {
            Text(label)
        },
        placeholder = {
            Text(placeholder)
        },
        leadingIcon = {

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        SoftMint
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = "•",
                    color = DeepTeal,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        trailingIcon = {

            Text(
                text =
                    if (visible) {
                        appText("register_hide", selectedLanguage)
                    } else {
                        appText("register_show", selectedLanguage)
                    },
                color = DeepTeal,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable {
                        onVisibilityChange()
                    }
                    .padding(
                        end = 12.dp
                    )
            )
        },
        visualTransformation =
            if (visible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
        singleLine = true,
        isError = isError,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
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
                DeepTeal,
            errorBorderColor =
                StatusError
        )
    )
}


/* ================================================================
   ERROR TEXT
   ================================================================ */

@Composable
private fun ErrorText(
    text: String
) {

    Text(
        text = text,
        color = StatusError,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 5.dp,
                top = 4.dp
            )
    )
}