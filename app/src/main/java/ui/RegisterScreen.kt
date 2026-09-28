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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_based_medical_chatbot.data.SupabaseClient
import kotlinx.coroutines.launch

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

    // ============================================================
    // MEDASSIST AI COLORS
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

        // ========================================================
        // MAIN CONTENT
        // ========================================================

        Column(
            modifier = Modifier
                .fillMaxSize()
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
                        text = "←  Back to Login",
                        modifier = Modifier.fillMaxWidth(),
                        color = primaryBlue,
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
                    shape = RoundedCornerShape(27.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 5.dp
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = borderColor
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(
                            horizontal = 20.dp,
                            vertical = 21.dp
                        )
                    ) {

                        Text(
                            text = "Create Account",
                            color = primaryBlueDark,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = "Create your MEDASSIST AI account to access your personal health assistant.",
                            color = textGray,
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
                            label = "Full Name",
                            placeholder = "Enter your full name",
                            leadingText = "✦",
                            isError = nameError.isNotEmpty()
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
                            label = "Email Address",
                            placeholder = "Enter your email",
                            leadingText = "@",
                            isError = emailError.isNotEmpty()
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
                                        "Passwords do not match"
                                    } else {
                                        ""
                                    }
                            },
                            label = "Password",
                            placeholder = "Create a strong password",
                            visible = passwordVisible,
                            onVisibilityChange = {
                                passwordVisible =
                                    !passwordVisible
                            },
                            isError = passwordError.isNotEmpty()
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
                                            "Passwords do not match"
                                        else -> ""
                                    }
                            },
                            label = "Confirm Password",
                            placeholder = "Re-enter your password",
                            visible = confirmPasswordVisible,
                            onVisibilityChange = {
                                confirmPasswordVisible =
                                    !confirmPasswordVisible
                            },
                            isError =
                                confirmPasswordError.isNotEmpty()
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
                                            teal
                                        } else {
                                            Color(0xFFB8C7CD)
                                        }
                                    )
                            )

                            Spacer(
                                modifier = Modifier.width(8.dp)
                            )

                            Text(
                                text =
                                    "Password must contain at least 6 characters",
                                color = textGray,
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
                                        RoundedCornerShape(13.dp)
                                    )
                                    .background(
                                        if (success) {
                                            Color(0xFFEAF8F1)
                                        } else {
                                            Color(0xFFFFF1F1)
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color =
                                            if (success) {
                                                Color(0xFFB9E5CF)
                                            } else {
                                                Color(0xFFF1C4C4)
                                            },
                                        shape =
                                            RoundedCornerShape(13.dp)
                                    )
                                    .padding(11.dp)
                            ) {

                                Text(
                                    text = registerError,
                                    color =
                                        if (success) {
                                            Color(0xFF21824D)
                                        } else {
                                            Color(0xFFC62828)
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
                            onClick = {

                                nameError = ""
                                emailError = ""
                                passwordError = ""
                                confirmPasswordError = ""
                                registerError = ""

                                var valid = true

                                // NAME

                                if (
                                    fullName.trim().isEmpty()
                                ) {
                                    nameError =
                                        "Name is required"
                                    valid = false
                                }

                                // EMAIL

                                val cleanEmail =
                                    email.trim()

                                if (cleanEmail.isEmpty()) {

                                    emailError =
                                        "Email is required"
                                    valid = false

                                } else if (
                                    !Patterns.EMAIL_ADDRESS
                                        .matcher(cleanEmail)
                                        .matches()
                                ) {

                                    emailError =
                                        "Enter a valid email address"
                                    valid = false
                                }

                                // PASSWORD

                                if (password.isEmpty()) {

                                    passwordError =
                                        "Password is required"
                                    valid = false

                                } else if (
                                    password.length < 6
                                ) {

                                    passwordError =
                                        "Password must be at least 6 characters"
                                    valid = false
                                }

                                // CONFIRM PASSWORD

                                if (confirmPassword.isEmpty()) {

                                    confirmPasswordError =
                                        "Please confirm your password"
                                    valid = false

                                } else if (
                                    password != confirmPassword
                                ) {

                                    confirmPasswordError =
                                        "Passwords do not match"
                                    valid = false
                                }

                                if (!valid) {
                                    return@Button
                                }

                                val registrationEmail =
                                    cleanEmail

                                val registrationPassword =
                                    password

                                val registrationName =
                                    fullName.trim()

                                scope.launch {

                                    isLoading = true
                                    registerError = ""

                                    try {

                                        val result =
                                            SupabaseClient.registerUser(
                                                email =
                                                    registrationEmail,
                                                password =
                                                    registrationPassword,
                                                fullName =
                                                    registrationName
                                            )

                                        result.onSuccess {

                                            isLoading = false

                                            registerError =
                                                "Account created successfully!"

                                            onRegisterClick()
                                        }

                                        result.onFailure { error ->

                                            isLoading = false

                                            registerError =
                                                error.message
                                                    ?: "Registration failed. Please try again."
                                        }

                                    } catch (
                                        e: Exception
                                    ) {

                                        isLoading = false

                                        registerError =
                                            e.message
                                                ?: "Registration failed. Please try again."
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryBlue,
                                disabledContainerColor =
                                    primaryBlue.copy(
                                        alpha = 0.40f
                                    )
                            )
                        ) {

                            if (isLoading) {

                                CircularProgressIndicator(
                                    modifier =
                                        Modifier.size(21.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )

                            } else {

                                Text(
                                    text =
                                        "Create Account  →",
                                    fontSize = 15.sp,
                                    fontWeight =
                                        FontWeight.Bold
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
                        "Already have an account?",
                    color = textGray,
                    fontSize = 12.sp
                )

                Spacer(
                    modifier = Modifier.width(4.dp)
                )

                Text(
                    text = "Login",
                    color = primaryBlue,
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
                        .size(29.dp)
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

                    Text(
                        text = "✓",
                        color = teal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.width(7.dp)
                )

                Text(
                    text =
                        "Secure account creation with MEDASSIST AI",
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
   REGISTER HERO
   ================================================================ */

@Composable
private fun RegisterHero(
    botScale: Float
) {

    val primaryBlue = Color(0xFF1976D2)
    val teal = Color(0xFF009688)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(215.dp)
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

        // Decorative glow

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
                text =
                    "Start your journey\nwith MEDASSIST AI.",
                color = Color.White,
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

                HeroBadge("AI POWERED")

                HeroBadge("SECURE")
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

    val primaryBlue = Color(0xFF1976D2)
    val teal = Color(0xFF009688)

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
                            Color.White.copy(
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
                                    primaryBlue
                                )
                        )

                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(
                                    primaryBlue
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
                                teal
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
                            teal
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
                            primaryBlue
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
    isError: Boolean
) {

    val primaryBlue = Color(0xFF1976D2)
    val primaryBlueDark = Color(0xFF123A56)
    val textGray = Color(0xFF71818C)
    val borderColor = Color(0xFFD8E5E9)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
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
                        Color(0xFFE8F7FB)
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = leadingText,
                    color = primaryBlue,
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
                primaryBlue,
            unfocusedBorderColor =
                borderColor,
            focusedLabelColor =
                primaryBlue,
            unfocusedLabelColor =
                textGray,
            focusedTextColor =
                primaryBlueDark,
            unfocusedTextColor =
                primaryBlueDark,
            cursorColor =
                primaryBlue,
            errorBorderColor =
                Color(0xFFD32F2F)
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
    isError: Boolean
) {

    val primaryBlue = Color(0xFF1976D2)
    val primaryBlueDark = Color(0xFF123A56)
    val textGray = Color(0xFF71818C)
    val borderColor = Color(0xFFD8E5E9)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
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
                        Color(0xFFE8F7FB)
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = "•",
                    color = primaryBlue,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        trailingIcon = {

            Text(
                text =
                    if (visible) {
                        "HIDE"
                    } else {
                        "SHOW"
                    },
                color = primaryBlue,
                fontSize = 10.sp,
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
                primaryBlue,
            unfocusedBorderColor =
                borderColor,
            focusedLabelColor =
                primaryBlue,
            unfocusedLabelColor =
                textGray,
            focusedTextColor =
                primaryBlueDark,
            unfocusedTextColor =
                primaryBlueDark,
            cursorColor =
                primaryBlue,
            errorBorderColor =
                Color(0xFFD32F2F)
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
        color = Color(0xFFD32F2F),
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