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
import androidx.compose.foundation.layout.size
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

    val scope = rememberCoroutineScope()

    // ============================================================
    // MEDASSIST AI THEME
    // ============================================================

    val primary = Color(0xFF087EA4)
    val darkBlue = Color(0xFF123A56)
    val gray = Color(0xFF71818C)

    val backgroundTop = Color(0xFFEAF8FC)
    val backgroundBottom = Color(0xFFD8F0F6)

    val lightPrimary = Color(0xFFE8F7FB)

    // ============================================================
    // MAIN SCREEN
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
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 20.dp,
                    vertical = 22.dp
                ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            // ====================================================
            // LOGO
            // ====================================================

            Box(
                modifier = Modifier
                    .size(82.dp)
                    .background(
                        Color.White,
                        CircleShape
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Box(
                    modifier = Modifier
                        .size(65.dp)
                        .background(
                            lightPrimary,
                            CircleShape
                        ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text = "+",
                        color = primary,
                        fontSize = 43.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(15.dp)
            )

            // ====================================================
            // TITLE
            // ====================================================

            Text(
                text = "Create Account",
                color = darkBlue,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = "Create your MedAssist AI account",
                color = gray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(22.dp)
            )

            // ====================================================
            // REGISTRATION CARD
            // ====================================================

            Card(
                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(28.dp),

                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),

                elevation = CardDefaults.cardElevation(
                    defaultElevation = 7.dp
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {

                    Text(
                        text = "Personal Details",
                        color = darkBlue,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(17.dp)
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

                        leadingText = "👤",

                        isError =
                            nameError.isNotEmpty()
                    )

                    if (nameError.isNotEmpty()) {

                        ErrorText(
                            text = nameError
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(13.dp)
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

                        isError =
                            emailError.isNotEmpty()
                    )

                    if (emailError.isNotEmpty()) {

                        ErrorText(
                            text = emailError
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(13.dp)
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

                        label = "Password",

                        placeholder =
                            "Create a strong password",

                        visible =
                            passwordVisible,

                        onVisibilityChange = {

                            passwordVisible =
                                !passwordVisible
                        },

                        isError =
                            passwordError.isNotEmpty()
                    )

                    if (passwordError.isNotEmpty()) {

                        ErrorText(
                            text = passwordError
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(13.dp)
                    )

                    // =================================================
                    // CONFIRM PASSWORD
                    // =================================================

                    PasswordRegisterField(
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

                        label =
                            "Confirm Password",

                        placeholder =
                            "Re-enter your password",

                        visible =
                            confirmPasswordVisible,

                        onVisibilityChange = {

                            confirmPasswordVisible =
                                !confirmPasswordVisible
                        },

                        isError =
                            confirmPasswordError.isNotEmpty()
                    )

                    if (
                        confirmPasswordError.isNotEmpty()
                    ) {

                        ErrorText(
                            text =
                                confirmPasswordError
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )

                    // =================================================
                    // PASSWORD REQUIREMENT
                    // =================================================

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .size(7.dp)
                                    .background(
                                        color =
                                            if (
                                                password.length >= 6
                                            ) {
                                                primary
                                            } else {
                                                Color(0xFFB7CBD1)
                                            },

                                        shape =
                                            CircleShape
                                    )
                        )

                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )

                        Text(
                            text =
                                "Password must contain at least 6 characters",

                            color =
                                gray,

                            fontSize =
                                11.sp
                        )
                    }

                    // =================================================
                    // REGISTER MESSAGE
                    // =================================================

                    if (
                        registerError.isNotEmpty()
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )

                        Card(
                            modifier =
                                Modifier.fillMaxWidth(),

                            shape =
                                RoundedCornerShape(14.dp),

                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        if (
                                            registerError ==
                                            "Account created successfully!"
                                        ) {
                                            Color(0xFFE7F7EF)
                                        } else {
                                            Color(0xFFFFF1F1)
                                        }
                                )
                        ) {

                            Text(
                                text =
                                    registerError,

                                modifier =
                                    Modifier.padding(12.dp),

                                color =
                                    if (
                                        registerError ==
                                        "Account created successfully!"
                                    ) {
                                        Color(0xFF217A4A)
                                    } else {
                                        Color(0xFFC62828)
                                    },

                                fontSize =
                                    12.sp,

                                textAlign =
                                    TextAlign.Center
                            )
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(20.dp)
                    )

                    // =================================================
                    // CREATE ACCOUNT
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

                            if (
                                cleanEmail.isEmpty()
                            ) {

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

                            if (
                                password.isEmpty()
                            ) {

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

                            if (!valid) {
                                return@Button
                            }

                            // SAVE VALUES

                            val registrationEmail =
                                cleanEmail

                            val registrationPassword =
                                password

                            val registrationName =
                                fullName.trim()

                            // SUPABASE

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

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(55.dp),

                        shape =
                            RoundedCornerShape(17.dp),

                        colors =
                            ButtonDefaults.buttonColors(

                                containerColor =
                                    primary,

                                disabledContainerColor =
                                    Color(0xFFB7CBD1)
                            )
                    ) {

                        if (isLoading) {

                            CircularProgressIndicator(

                                modifier =
                                    Modifier.size(21.dp),

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
                                    15.sp,

                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            // ====================================================
            // LOGIN
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

                    color =
                        gray,

                    fontSize =
                        13.sp
                )

                Spacer(
                    modifier =
                        Modifier.width(5.dp)
                )

                Text(
                    text =
                        "Login",

                    color =
                        primary,

                    fontSize =
                        13.sp,

                    fontWeight =
                        FontWeight.Bold,

                    modifier =
                        Modifier
                            .padding(4.dp)
                            .clickable {

                                if (!isLoading) {
                                    onBackToLogin()
                                }
                            }
                )
            }

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            // ====================================================
            // SECURITY TEXT
            // ====================================================

            Text(
                text =
                    "Your account information is securely managed through Supabase.",

                color =
                    gray,

                fontSize =
                    10.sp,

                textAlign =
                    TextAlign.Center,

                modifier =
                    Modifier.fillMaxWidth()
            )
        }
    }
}


// =================================================================
// NORMAL REGISTER FIELD
// =================================================================

@Composable
private fun RegisterField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingText: String,
    isError: Boolean
) {

    val primary =
        Color(0xFF087EA4)

    val darkBlue =
        Color(0xFF123A56)

    val gray =
        Color(0xFF71818C)

    OutlinedTextField(

        value =
            value,

        onValueChange =
            onValueChange,

        modifier =
            Modifier.fillMaxWidth(),

        label = {
            Text(
                text = label
            )
        },

        placeholder = {
            Text(
                text = placeholder
            )
        },

        leadingIcon = {

            Box(
                modifier =
                    Modifier
                        .size(30.dp)
                        .background(
                            Color(0xFFE8F7FB),
                            CircleShape
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        leadingText,

                    color =
                        primary,

                    fontSize =
                        13.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }
        },

        singleLine =
            true,

        isError =
            isError,

        shape =
            RoundedCornerShape(17.dp),

        colors =
            OutlinedTextFieldDefaults.colors(

                focusedBorderColor =
                    primary,

                unfocusedBorderColor =
                    Color(0xFFD8E5E9),

                focusedLabelColor =
                    primary,

                unfocusedLabelColor =
                    gray,

                focusedTextColor =
                    darkBlue,

                unfocusedTextColor =
                    darkBlue,

                cursorColor =
                    primary,

                errorBorderColor =
                    Color(0xFFD32F2F)
            )
    )
}


// =================================================================
// PASSWORD FIELD
// =================================================================

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

    val primary =
        Color(0xFF087EA4)

    val darkBlue =
        Color(0xFF123A56)

    val gray =
        Color(0xFF71818C)

    OutlinedTextField(

        value =
            value,

        onValueChange =
            onValueChange,

        modifier =
            Modifier.fillMaxWidth(),

        label = {
            Text(
                text =
                    label
            )
        },

        placeholder = {
            Text(
                text =
                    placeholder
            )
        },

        leadingIcon = {

            Box(
                modifier =
                    Modifier
                        .size(30.dp)
                        .background(
                            Color(0xFFE8F7FB),
                            CircleShape
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = "•",

                    color =
                        primary,

                    fontSize =
                        20.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }
        },

        trailingIcon = {

            Text(
                text =
                    if (visible) {
                        "Hide"
                    } else {
                        "Show"
                    },

                color =
                    primary,

                fontSize =
                    11.sp,

                fontWeight =
                    FontWeight.Bold,

                modifier =
                    Modifier
                        .padding(
                            end = 12.dp
                        )
                        .clickable {
                            onVisibilityChange()
                        }
            )
        },

        visualTransformation =
            if (visible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },

        singleLine =
            true,

        isError =
            isError,

        shape =
            RoundedCornerShape(17.dp),

        colors =
            OutlinedTextFieldDefaults.colors(

                focusedBorderColor =
                    primary,

                unfocusedBorderColor =
                    Color(0xFFD8E5E9),

                focusedLabelColor =
                    primary,

                unfocusedLabelColor =
                    gray,

                focusedTextColor =
                    darkBlue,

                unfocusedTextColor =
                    darkBlue,

                cursorColor =
                    primary,

                errorBorderColor =
                    Color(0xFFD32F2F)
            )
    )
}


// =================================================================
// ERROR TEXT
// =================================================================

@Composable
private fun ErrorText(
    text: String
) {

    Text(
        text =
            text,

        color =
            Color(0xFFD32F2F),

        fontSize =
            11.sp,

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = 5.dp,
                    top = 4.dp
                )
    )
}