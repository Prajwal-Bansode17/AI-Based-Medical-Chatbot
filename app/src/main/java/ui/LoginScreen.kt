package ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
    onLoginClick: (String, String) -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    isLoading: Boolean = false,
    loginError: String = ""
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


    // =========================================================
    // BACKGROUND
    // =========================================================

    val backgroundBrush =
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF06152B),
                Color(0xFF083B56),
                Color(0xFF087EA4)
            )
        )


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 24.dp,
                    vertical = 28.dp
                ),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Center
        ) {


            // =================================================
            // LOGO
            // =================================================

            Box(
                modifier = Modifier
                    .size(82.dp)
                    .background(
                        Color.White,
                        RoundedCornerShape(24.dp)
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = "+",
                    color = Color(0xFF087EA4),
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold
                )
            }


            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )


            Text(
                text = "MedAssist AI",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )


            Text(
                text = "Your intelligent health companion",
                color = Color(0xFFBCEAF2),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )


            Spacer(
                modifier =
                    Modifier.height(26.dp)
            )


            // =================================================
            // LOGIN CARD
            // =================================================

            Column(

                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color.White,
                        RoundedCornerShape(24.dp)
                    )
                    .padding(22.dp)
            ) {


                Text(
                    text = "Welcome Back",
                    color = Color(0xFF09233F),
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(5.dp)
                )


                Text(
                    text = "Sign in to continue",
                    color = Color(0xFF6D7D8A),
                    fontSize = 14.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )


                // =================================================
                // EMAIL
                // =================================================

                OutlinedTextField(

                    value = email,

                    onValueChange = {

                        email = it

                        emailError = ""
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    enabled =
                        !isLoading,

                    singleLine = true,

                    label = {
                        Text("Email")
                    },

                    placeholder = {
                        Text("Enter your email")
                    },

                    isError =
                        emailError.isNotEmpty(),

                    shape =
                        RoundedCornerShape(14.dp)
                )


                if (emailError.isNotEmpty()) {

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text = emailError,
                        color = Color(0xFFD32F2F),
                        fontSize = 12.sp
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )


                // =================================================
                // PASSWORD
                // =================================================

                OutlinedTextField(

                    value = password,

                    onValueChange = {

                        password = it

                        passwordError = ""
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    enabled =
                        !isLoading,

                    singleLine = true,

                    label = {
                        Text("Password")
                    },

                    placeholder = {
                        Text("Enter your password")
                    },

                    isError =
                        passwordError.isNotEmpty(),

                    visualTransformation =
                        if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },

                    trailingIcon = {

                        TextButton(
                            onClick = {
                                passwordVisible =
                                    !passwordVisible
                            },

                            enabled =
                                !isLoading
                        ) {

                            Text(
                                text =
                                    if (passwordVisible) {
                                        "HIDE"
                                    } else {
                                        "SHOW"
                                    },

                                color =
                                    Color(0xFF087EA4),

                                fontSize = 11.sp,

                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    },

                    shape =
                        RoundedCornerShape(14.dp)
                )


                if (passwordError.isNotEmpty()) {

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text = passwordError,
                        color = Color(0xFFD32F2F),
                        fontSize = 12.sp
                    )
                }


                // =================================================
                // FORGOT PASSWORD
                // =================================================

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.End
                ) {

                    TextButton(

                        onClick =
                            onForgotPasswordClick,

                        enabled =
                            !isLoading
                    ) {

                        Text(
                            text =
                                "Forgot password?",

                            color =
                                Color(0xFF087EA4),

                            fontSize = 13.sp
                        )
                    }
                }


                // =================================================
                // SUPABASE LOGIN ERROR
                // =================================================

                if (loginError.isNotEmpty()) {

                    Box(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFFFFEBEE),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(13.dp)
                    ) {

                        Text(
                            text =
                                loginError,

                            color =
                                Color(0xFFC62828),

                            fontSize = 13.sp,

                            fontWeight =
                                FontWeight.Medium
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )
                }


                // =================================================
                // SIGN IN
                // =================================================

                Button(

                    onClick = {

                        emailError = ""
                        passwordError = ""

                        val cleanEmail =
                            email.trim()


                        // -----------------------------
                        // EMAIL
                        // -----------------------------

                        if (cleanEmail.isEmpty()) {

                            emailError =
                                "Please enter your email."

                            return@Button
                        }


                        if (
                            !android.util.Patterns
                                .EMAIL_ADDRESS
                                .matcher(cleanEmail)
                                .matches()
                        ) {

                            emailError =
                                "Please enter a valid email."

                            return@Button
                        }


                        // -----------------------------
                        // PASSWORD
                        // -----------------------------

                        if (password.isEmpty()) {

                            passwordError =
                                "Please enter your password."

                            return@Button
                        }


                        if (password.length < 6) {

                            passwordError =
                                "Password must contain at least 6 characters."

                            return@Button
                        }


                        // -----------------------------
                        // LOGIN
                        // -----------------------------

                        onLoginClick(
                            cleanEmail,
                            password
                        )
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(54.dp),

                    enabled =
                        !isLoading,

                    shape =
                        RoundedCornerShape(15.dp),

                    colors =
                        ButtonDefaults.buttonColors(

                            containerColor =
                                Color(0xFF087EA4),

                            disabledContainerColor =
                                Color(0xFF8CBCC7)
                        )
                ) {

                    if (isLoading) {

                        CircularProgressIndicator(

                            modifier =
                                Modifier.size(22.dp),

                            color =
                                Color.White,

                            strokeWidth = 2.dp
                        )

                    } else {

                        Text(
                            text = "Sign In",

                            color =
                                Color.White,

                            fontSize = 16.sp,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )


                // =================================================
                // REGISTER
                // =================================================

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.Center,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            "Don't have an account?",

                        color =
                            Color(0xFF6D7D8A),

                        fontSize = 13.sp
                    )


                    TextButton(

                        onClick =
                            onRegisterClick,

                        enabled =
                            !isLoading
                    ) {

                        Text(
                            text = "Register",

                            color =
                                Color(0xFF087EA4),

                            fontSize = 14.sp,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(15.dp)
            )


            Text(
                text =
                    "Secure authentication powered by Supabase",

                color =
                    Color(0xFFBCEAF2),

                fontSize = 11.sp,

                textAlign =
                    TextAlign.Center
            )
        }
    }
}