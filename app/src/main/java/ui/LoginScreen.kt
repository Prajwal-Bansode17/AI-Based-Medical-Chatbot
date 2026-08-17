package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    onLoginClick: (String) -> Unit = {},
    onRegisterClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {}
) {

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    var emailError by remember {
        mutableStateOf("")
    }

    var passwordError by remember {
        mutableStateOf("")
    }

    var showContent by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        showContent = true
    }

    // Background gradient
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
            .background(background)
            .padding(horizontal = 24.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // =================================================
            // APP LOGO
            // =================================================

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    animationSpec = tween(600)
                ) + slideInVertically(
                    initialOffsetY = { -80 },
                    animationSpec = tween(600)
                )
            ) {

                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.12f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .background(
                                color = Color.White,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "MedAssist AI",
                            tint = Color(0xFF00A9E8),
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            // =================================================
            // WELCOME TEXT
            // =================================================

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 600,
                        delayMillis = 150
                    )
                ) + slideInVertically(
                    initialOffsetY = { 50 },
                    animationSpec = tween(
                        durationMillis = 600,
                        delayMillis = 150
                    )
                )
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Welcome Back",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(7.dp)
                    )

                    Text(
                        text = "Sign in to continue to MedAssist AI",
                        color = Color(0xFFB9EAF2),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(30.dp)
            )

            // =================================================
            // EMAIL
            // =================================================

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 600,
                        delayMillis = 300
                    )
                ) + slideInVertically(
                    initialOffsetY = { 70 },
                    animationSpec = tween(
                        durationMillis = 600,
                        delayMillis = 300
                    )
                )
            ) {

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Email")
                        },
                        placeholder = {
                            Text("Enter your email")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email"
                            )
                        },
                        isError = emailError.isNotEmpty(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )

                    if (emailError.isNotEmpty()) {

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = emailError,
                            color = Color(0xFFFF8A80),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            // =================================================
            // PASSWORD
            // =================================================

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 600,
                        delayMillis = 400
                    )
                ) + slideInVertically(
                    initialOffsetY = { 70 },
                    animationSpec = tween(
                        durationMillis = 600,
                        delayMillis = 400
                    )
                )
            ) {

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Password")
                        },
                        placeholder = {
                            Text("Enter your password")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Password"
                            )
                        },
                        trailingIcon = {

                            IconButton(
                                onClick = {
                                    passwordVisible = !passwordVisible
                                }
                            ) {

                                Text(
                                    text = if (passwordVisible) {
                                        "◉"
                                    } else {
                                        "◌"
                                    },
                                    fontSize = 20.sp,
                                    color = Color(0xFF00A9E8)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        isError = passwordError.isNotEmpty(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )

                    if (passwordError.isNotEmpty()) {

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = passwordError,
                            color = Color(0xFFFF8A80),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            // =================================================
            // FORGOT PASSWORD
            // =================================================

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 600,
                        delayMillis = 500
                    )
                )
            ) {

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {

                    Text(
                        text = "Forgot Password?",
                        color = Color(0xFF7DE8F0),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            onForgotPasswordClick()
                        }
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            // =================================================
            // LOGIN BUTTON
            // =================================================

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 600,
                        delayMillis = 550
                    )
                ) + slideInVertically(
                    initialOffsetY = { 80 },
                    animationSpec = tween(
                        durationMillis = 600,
                        delayMillis = 550
                    )
                )
            ) {

                Button(
                    onClick = {

                        emailError = ""
                        passwordError = ""

                        var isValid = true

                        // Email validation
                        if (email.isBlank()) {

                            emailError = "Email is required"
                            isValid = false

                        } else if (
                            !android.util.Patterns.EMAIL_ADDRESS
                                .matcher(email)
                                .matches()
                        ) {

                            emailError = "Enter a valid email address"
                            isValid = false
                        }

                        // Password validation
                        if (password.isBlank()) {

                            passwordError = "Password is required"
                            isValid = false

                        } else if (password.length < 6) {

                            passwordError =
                                "Password must be at least 6 characters"

                            isValid = false
                        }

                        // Login
                        if (isValid) {
                            onLoginClick(email)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF087EA4)
                    )
                ) {

                    Text(
                        text = "Sign In",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(22.dp)
            )

            // =================================================
            // REGISTER
            // =================================================

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 600,
                        delayMillis = 650
                    )
                )
            ) {

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "Don't have an account? ",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp
                    )

                    Text(
                        text = "Register",
                        color = Color(0xFF7DE8F0),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            onRegisterClick()
                        }
                    )
                }
            }
        }
    }
}