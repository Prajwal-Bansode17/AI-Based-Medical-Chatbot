package ui

import android.util.Patterns
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.ai_based_medical_chatbot.data.SupabaseClient
import kotlinx.coroutines.launch


@Composable
fun RegisterScreen(
    onRegisterClick: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    onBackToLogin: () -> Unit
) {

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


    val scope =
        rememberCoroutineScope()


    val background =
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF071A33),
                Color(0xFF0A2D4F),
                Color(0xFF064E63)
            )
        )


    Box(

        modifier =
            Modifier
                .fillMaxSize()
                .background(background)
                .padding(24.dp)
    ) {

        Column(

            modifier =
                Modifier.fillMaxSize(),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Center
        ) {


            Text(
                text = "Create Account",

                color =
                    Color.White,

                fontSize = 28.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            Text(
                text =
                    "Create your MedAssist AI account",

                color =
                    Color(0xFFB9EAF2),

                fontSize = 14.sp,

                textAlign =
                    TextAlign.Center
            )


            Spacer(
                modifier =
                    Modifier.height(28.dp)
            )


            // =================================================
            // FULL NAME
            // =================================================

            OutlinedTextField(

                value =
                    fullName,

                onValueChange = {

                    fullName = it

                    nameError = ""
                    registerError = ""
                },

                modifier =
                    Modifier.fillMaxWidth(),

                label = {
                    Text("Full Name")
                },

                leadingIcon = {

                    Icon(
                        imageVector =
                            Icons.Default.Person,

                        contentDescription =
                            "Full Name"
                    )
                },

                isError =
                    nameError.isNotEmpty(),

                singleLine = true,

                shape =
                    RoundedCornerShape(14.dp)
            )


            if (nameError.isNotEmpty()) {

                Text(
                    text =
                        nameError,

                    color =
                        Color(0xFFFF8A80),

                    fontSize =
                        12.sp,

                    modifier =
                        Modifier.fillMaxWidth()
                )
            }


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )


            // =================================================
            // EMAIL
            // =================================================

            OutlinedTextField(

                value =
                    email,

                onValueChange = {

                    email = it

                    emailError = ""
                    registerError = ""
                },

                modifier =
                    Modifier.fillMaxWidth(),

                label = {
                    Text("Email")
                },

                leadingIcon = {

                    Icon(
                        imageVector =
                            Icons.Default.Email,

                        contentDescription =
                            "Email"
                    )
                },

                isError =
                    emailError.isNotEmpty(),

                singleLine = true,

                shape =
                    RoundedCornerShape(14.dp)
            )


            if (emailError.isNotEmpty()) {

                Text(
                    text =
                        emailError,

                    color =
                        Color(0xFFFF8A80),

                    fontSize =
                        12.sp,

                    modifier =
                        Modifier.fillMaxWidth()
                )
            }


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )


            // =================================================
            // PASSWORD
            // =================================================

            OutlinedTextField(

                value =
                    password,

                onValueChange = {

                    password = it

                    passwordError = ""
                    registerError = ""

                    if (
                        confirmPassword.isNotEmpty() &&
                        confirmPassword != it
                    ) {

                        confirmPasswordError =
                            "Passwords do not match"

                    } else {

                        confirmPasswordError = ""
                    }
                },

                modifier =
                    Modifier.fillMaxWidth(),

                label = {
                    Text("Password")
                },

                leadingIcon = {

                    Icon(
                        imageVector =
                            Icons.Default.Lock,

                        contentDescription =
                            "Password"
                    )
                },

                trailingIcon = {

                    IconButton(

                        onClick = {

                            passwordVisible =
                                !passwordVisible
                        }
                    ) {

                        Text(
                            text =
                                if (passwordVisible) {
                                    "◉"
                                } else {
                                    "◌"
                                },

                            fontSize =
                                20.sp
                        )
                    }
                },

                visualTransformation =
                    if (passwordVisible) {

                        VisualTransformation.None

                    } else {

                        PasswordVisualTransformation()
                    },

                isError =
                    passwordError.isNotEmpty(),

                singleLine = true,

                shape =
                    RoundedCornerShape(14.dp)
            )


            if (passwordError.isNotEmpty()) {

                Text(
                    text =
                        passwordError,

                    color =
                        Color(0xFFFF8A80),

                    fontSize =
                        12.sp,

                    modifier =
                        Modifier.fillMaxWidth()
                )
            }


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )


            // =================================================
            // CONFIRM PASSWORD
            // =================================================

            OutlinedTextField(

                value =
                    confirmPassword,

                onValueChange = {

                    confirmPassword = it

                    registerError = ""

                    confirmPasswordError =
                        when {

                            it.isEmpty() ->
                                ""

                            it != password ->
                                "Passwords do not match"

                            else ->
                                ""
                        }
                },

                modifier =
                    Modifier.fillMaxWidth(),

                label = {
                    Text("Confirm Password")
                },

                leadingIcon = {

                    Icon(
                        imageVector =
                            Icons.Default.Lock,

                        contentDescription =
                            "Confirm Password"
                    )
                },

                trailingIcon = {

                    IconButton(

                        onClick = {

                            confirmPasswordVisible =
                                !confirmPasswordVisible
                        }
                    ) {

                        Text(
                            text =
                                if (
                                    confirmPasswordVisible
                                ) {
                                    "◉"
                                } else {
                                    "◌"
                                },

                            fontSize =
                                20.sp
                        )
                    }
                },

                visualTransformation =
                    if (
                        confirmPasswordVisible
                    ) {

                        VisualTransformation.None

                    } else {

                        PasswordVisualTransformation()
                    },

                isError =
                    confirmPasswordError.isNotEmpty(),

                singleLine = true,

                shape =
                    RoundedCornerShape(14.dp)
            )


            if (
                confirmPasswordError.isNotEmpty()
            ) {

                Text(
                    text =
                        confirmPasswordError,

                    color =
                        Color(0xFFFF8A80),

                    fontSize =
                        12.sp,

                    modifier =
                        Modifier.fillMaxWidth()
                )
            }


            // =================================================
            // REGISTER ERROR
            // =================================================

            if (registerError.isNotEmpty()) {

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text =
                        registerError,

                    color =
                        Color(0xFFFFD54F),

                    fontSize =
                        13.sp,

                    textAlign =
                        TextAlign.Center,

                    modifier =
                        Modifier.fillMaxWidth()
                )
            }


            Spacer(
                modifier =
                    Modifier.height(22.dp)
            )


            // =================================================
            // CREATE ACCOUNT BUTTON
            // =================================================

            Button(

                enabled =
                    !isLoading,

                onClick = {

                    nameError = ""
                    emailError = ""
                    passwordError = ""
                    confirmPasswordError = ""
                    registerError = ""


                    var valid = true


                    // -----------------------------
                    // NAME
                    // -----------------------------

                    if (
                        fullName.trim().isEmpty()
                    ) {

                        nameError =
                            "Name is required"

                        valid = false
                    }


                    // -----------------------------
                    // EMAIL
                    // -----------------------------

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


                    // -----------------------------
                    // PASSWORD
                    // -----------------------------

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


                    // -----------------------------
                    // CONFIRM PASSWORD
                    // -----------------------------

                    if (
                        confirmPassword.isEmpty()
                    ) {

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


                    // -----------------------------
                    // STOP HERE
                    // -----------------------------

                    if (!valid) {
                        return@Button
                    }


                    // =================================================
                    // SAVE VALUES BEFORE COROUTINE
                    // =================================================

                    val registrationEmail =
                        cleanEmail

                    val registrationPassword =
                        password

                    val registrationName =
                        fullName.trim()


                    // =================================================
                    // SUPABASE REGISTRATION
                    // =================================================

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

                        } catch (e: Exception) {

                            isLoading = false

                            registerError =
                                e.message
                                    ?: "Registration failed. Please try again."
                        }
                    }
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp),

                shape =
                    RoundedCornerShape(15.dp),

                colors =
                    ButtonDefaults.buttonColors(

                        containerColor =
                            Color(0xFF087EA4),

                        disabledContainerColor =
                            Color(0xFF527C87)
                    )
            ) {

                if (isLoading) {

                    CircularProgressIndicator(

                        modifier =
                            Modifier.height(20.dp),

                        color =
                            Color.White,

                        strokeWidth =
                            2.dp
                    )

                } else {

                    Text(
                        text =
                            "Create Account",

                        fontSize =
                            16.sp,

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )


            // =================================================
            // LOGIN
            // =================================================

            Row(

                horizontalArrangement =
                    Arrangement.Center,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text =
                        "Already have an account? ",

                    color =
                        Color.White,

                    fontSize =
                        14.sp
                )


                Text(
                    text =
                        "Login",

                    color =
                        Color(0xFF7DE8F0),

                    fontSize =
                        14.sp,

                    fontWeight =
                        FontWeight.Bold,

                    modifier =
                        Modifier
                            .padding(start = 2.dp)
                            .clickable {

                                if (!isLoading) {
                                    onBackToLogin()
                                }
                            }
                )
            }
        }
    }
}