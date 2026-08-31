package ui

import android.content.Context

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

    // =========================================================
    // INPUT STATE
    // =========================================================

    var email by remember {
        mutableStateOf("")
    }

    // Recent email suggestions. These are stored locally on the device.
    val context = LocalContext.current

    var recentEmails by remember {
        mutableStateOf(loadRecentEmails(context))
    }

    var emailFieldFocused by remember {
        mutableStateOf(false)
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
    // COLORS
    // =========================================================

    // =========================================================
    // MIDNIGHT AI PREMIUM PALETTE
    // =========================================================

    val backgroundTop = Color(0xFF080D18)
    val backgroundBottom = Color(0xFF0D1422)

    val primaryBlue = Color(0xFF4F7CFF)
    val darkBlue = Color(0xFFF8FAFC)
    val lightBlue = Color(0xFF16233A)

    val textDark = Color(0xFFF8FAFC)
    val textGray = Color(0xFF94A3B8)

    val errorRed = Color(0xFFF87171)

    val cardColor = Color(0xFF111A2A)
    val inputColor = Color(0xFF0D1422)
    val borderColor = Color(0xFF26354A)
    val accentCyan = Color(0xFF22D3EE)
    val successGreen = Color(0xFF22C997)


    // =========================================================
    // MAIN BACKGROUND
    // =========================================================

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

        // =====================================================
        // DECORATIVE BACKGROUND CIRCLE - TOP RIGHT
        // =====================================================

        Box(
            modifier = Modifier
                .size(190.dp)
                .align(Alignment.TopEnd)
                .background(
                    color = Color(0x224F7CFF),
                    shape = CircleShape
                )
        )


        // =====================================================
        // DECORATIVE BACKGROUND CIRCLE - BOTTOM LEFT
        // =====================================================

        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.BottomStart)
                .background(
                    color = Color(0x184F7CFF),
                    shape = CircleShape
                )
        )


        // =====================================================
        // MAIN CONTENT
        // =====================================================

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 22.dp,
                    vertical = 24.dp
                ),

            horizontalAlignment = Alignment.CenterHorizontally,

            verticalArrangement = Arrangement.Center
        ) {


            // =================================================
            // APP LOGO
            // =================================================

            Box(
                modifier = Modifier
                    .size(82.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(25.dp)
                    )
                    .background(
                        color = cardColor,
                        shape = RoundedCornerShape(25.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(25.dp)
                    ),

                contentAlignment = Alignment.Center
            ) {

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            color = primaryBlue,
                            shape = CircleShape
                        ),

                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "+",

                        color = Color.White,

                        fontSize = 39.sp,

                        fontWeight = FontWeight.Bold
                    )
                }
            }


            Spacer(
                modifier = Modifier.height(14.dp)
            )


            // =================================================
            // APP NAME
            // =================================================

            Text(
                text = "MedAssist AI",

                color = darkBlue,

                fontSize = 29.sp,

                fontWeight = FontWeight.Bold
            )


            Spacer(
                modifier = Modifier.height(4.dp)
            )


            // =================================================
            // APP TAGLINE
            // =================================================

            Text(
                text = "Your intelligent health companion",

                color = textGray,

                fontSize = 13.sp,

                textAlign = TextAlign.Center
            )


            Spacer(
                modifier = Modifier.height(26.dp)
            )


            // =================================================
            // LOGIN CARD
            // =================================================

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 14.dp,
                        shape = RoundedCornerShape(28.dp)
                    )
                    .background(
                        color = cardColor,
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(23.dp)
            ) {


                // =================================================
                // WELCOME HEADER
                // =================================================

                Text(
                    text = "Welcome Back",

                    color = textDark,

                    fontSize = 25.sp,

                    fontWeight = FontWeight.Bold
                )


                Spacer(
                    modifier = Modifier.height(5.dp)
                )


                Text(
                    text = "Sign in to continue to your health dashboard.",

                    color = textGray,

                    fontSize = 13.sp,

                    lineHeight = 19.sp
                )


                Spacer(
                    modifier = Modifier.height(22.dp)
                )


                // =================================================
                // EMAIL LABEL
                // =================================================

                Text(
                    text = "Email address",

                    color = textDark,

                    fontSize = 13.sp,

                    fontWeight = FontWeight.SemiBold
                )


                Spacer(
                    modifier = Modifier.height(7.dp)
                )


                // =================================================
                // EMAIL FIELD
                // =================================================

                // =================================================
                // EMAIL FIELD + RECENT EMAIL SUGGESTIONS
                // =================================================

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    OutlinedTextField(
                        value = email,

                        onValueChange = {
                            email = it
                            emailError = ""
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged {
                                emailFieldFocused = it.isFocused
                            },

                        enabled = !isLoading,

                        singleLine = true,

                        placeholder = {
                            Text(
                                text = "Enter your email",

                                color = Color(0xFF64748B),

                                fontSize = 14.sp
                            )
                        },

                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,

                                contentDescription = "Email",

                                tint = primaryBlue
                            )
                        },

                        isError = emailError.isNotEmpty(),

                        shape = RoundedCornerShape(15.dp),

                        colors = OutlinedTextFieldDefaults.colors(

                            focusedBorderColor = primaryBlue,

                            unfocusedBorderColor = borderColor,

                            focusedContainerColor = inputColor,

                            unfocusedContainerColor = inputColor,

                            cursorColor = primaryBlue,

                            focusedTextColor = textDark,

                            unfocusedTextColor = textDark
                        )
                    )

                    val filteredSuggestions =
                        recentEmails.filter {
                            email.isBlank() ||
                                    it.contains(
                                        email.trim(),
                                        ignoreCase = true
                                    )
                        }

                    DropdownMenu(
                        expanded =
                            emailFieldFocused &&
                                    !isLoading &&
                                    filteredSuggestions.isNotEmpty(),

                        onDismissRequest = {
                            emailFieldFocused = false
                        },

                        modifier = Modifier.fillMaxWidth(0.90f)
                    ) {

                        filteredSuggestions.forEach { suggestion ->

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = suggestion,
                                        color = textDark,
                                        fontSize = 14.sp
                                    )
                                },

                                onClick = {
                                    email = suggestion
                                    emailError = ""
                                    emailFieldFocused = false
                                }
                            )
                        }
                    }
                }


                // =================================================
                // EMAIL ERROR
                // =================================================

                if (emailError.isNotEmpty()) {

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = emailError,

                        color = errorRed,

                        fontSize = 11.sp
                    )
                }


                Spacer(
                    modifier = Modifier.height(16.dp)
                )


                // =================================================
                // PASSWORD LABEL
                // =================================================

                Text(
                    text = "Password",

                    color = textDark,

                    fontSize = 13.sp,

                    fontWeight = FontWeight.SemiBold
                )


                Spacer(
                    modifier = Modifier.height(7.dp)
                )


                // =================================================
                // PASSWORD FIELD
                // =================================================

                OutlinedTextField(
                    value = password,

                    onValueChange = {
                        password = it
                        passwordError = ""
                    },

                    modifier = Modifier.fillMaxWidth(),

                    enabled = !isLoading,

                    singleLine = true,

                    placeholder = {
                        Text(
                            text = "Enter your password",

                            color = Color(0xFF64748B),

                            fontSize = 14.sp
                        )
                    },

                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,

                            contentDescription = "Password",

                            tint = primaryBlue
                        )
                    },

                    // =================================================
                    // SHOW / HIDE PASSWORD
                    // =================================================

                    trailingIcon = {

                        TextButton(
                            onClick = {
                                passwordVisible = !passwordVisible
                            },

                            enabled = !isLoading
                        ) {

                            Text(
                                text = if (passwordVisible) {
                                    "HIDE"
                                } else {
                                    "SHOW"
                                },

                                color = primaryBlue,

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

                    isError = passwordError.isNotEmpty(),

                    shape = RoundedCornerShape(15.dp),

                    colors = OutlinedTextFieldDefaults.colors(

                        focusedBorderColor = primaryBlue,

                        unfocusedBorderColor = borderColor,

                        focusedContainerColor = inputColor,

                        unfocusedContainerColor = inputColor,

                        cursorColor = primaryBlue,

                        focusedTextColor = textDark,

                        unfocusedTextColor = textDark
                    )
                )


                // =================================================
                // PASSWORD ERROR
                // =================================================

                if (passwordError.isNotEmpty()) {

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = passwordError,

                        color = errorRed,

                        fontSize = 11.sp
                    )
                }


                // =================================================
                // FORGOT PASSWORD
                // =================================================

                Row(
                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.End
                ) {

                    TextButton(
                        onClick = onForgotPasswordClick,

                        enabled = !isLoading
                    ) {

                        Text(
                            text = "Forgot password?",

                            color = primaryBlue,

                            fontSize = 12.sp,

                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }


                // =================================================
                // SUPABASE LOGIN ERROR
                // =================================================

                if (loginError.isNotEmpty()) {

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFFFFF1F1),

                                shape = RoundedCornerShape(13.dp)
                            )
                            .border(
                                width = 1.dp,

                                color = Color(0xFFFFD4D4),

                                shape = RoundedCornerShape(13.dp)
                            )
                            .padding(12.dp)
                    ) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // =====================================
                            // ERROR ICON
                            // =====================================

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        color = Color(0xFFFFE1E1),

                                        shape = CircleShape
                                    ),

                                contentAlignment = Alignment.Center
                            ) {

                                Text(
                                    text = "!",

                                    color = errorRed,

                                    fontSize = 15.sp,

                                    fontWeight = FontWeight.Bold
                                )
                            }


                            Spacer(
                                modifier = Modifier.width(9.dp)
                            )


                            Text(
                                text = loginError,

                                color = Color(0xFFB52F2F),

                                fontSize = 12.sp,

                                lineHeight = 17.sp,

                                modifier = Modifier.weight(1f)
                            )
                        }
                    }


                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )
                }


                // =================================================
                // SIGN IN BUTTON
                // =================================================

                Button(
                    onClick = {

                        // -----------------------------------------
                        // RESET ERRORS
                        // -----------------------------------------

                        emailError = ""

                        passwordError = ""


                        // -----------------------------------------
                        // CLEAN EMAIL
                        // -----------------------------------------

                        val cleanEmail =
                            email.trim()


                        // -----------------------------------------
                        // EMAIL EMPTY
                        // -----------------------------------------

                        if (cleanEmail.isEmpty()) {

                            emailError =
                                "Please enter your email."

                            return@Button
                        }


                        // -----------------------------------------
                        // EMAIL FORMAT
                        // -----------------------------------------

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


                        // -----------------------------------------
                        // PASSWORD EMPTY
                        // -----------------------------------------

                        if (password.isEmpty()) {

                            passwordError =
                                "Please enter your password."

                            return@Button
                        }


                        // -----------------------------------------
                        // PASSWORD LENGTH
                        // -----------------------------------------

                        if (password.length < 6) {

                            passwordError =
                                "Password must contain at least 6 characters."

                            return@Button
                        }


                        // -----------------------------------------
                        // SEND TO MAIN ACTIVITY / SUPABASE
                        // -----------------------------------------

                        // Save the frequently used email locally so it can
                        // be suggested the next time the user logs in.
                        recentEmails =
                            saveRecentEmail(
                                context = context,
                                email = cleanEmail,
                                currentEmails = recentEmails
                            )

                        onLoginClick(
                            cleanEmail,
                            password
                        )
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),

                    enabled = !isLoading,

                    shape = RoundedCornerShape(16.dp),

                    colors = ButtonDefaults.buttonColors(

                        containerColor = primaryBlue,

                        disabledContainerColor =
                            Color(0xFF9AC5D2)
                    )
                ) {

                    // =================================================
                    // LOADING STATE
                    // =================================================

                    if (isLoading) {

                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),

                            color = Color.White,

                            strokeWidth = 2.5.dp
                        )

                    } else {

                        Text(
                            text = "Sign In",

                            color = Color.White,

                            fontSize = 16.sp,

                            fontWeight = FontWeight.Bold
                        )
                    }
                }


                Spacer(
                    modifier = Modifier.height(18.dp)
                )


                // =================================================
                // OR DIVIDER
                // =================================================

                Row(
                    modifier = Modifier.fillMaxWidth(),

                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                Color(0xFFE4ECEF)
                            )
                    )


                    Text(
                        text = "  OR  ",

                        color = Color(0xFF64748B),

                        fontSize = 11.sp
                    )


                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                Color(0xFFE4ECEF)
                            )
                    )
                }


                Spacer(
                    modifier = Modifier.height(14.dp)
                )


                // =================================================
                // REGISTER BUTTON
                // =================================================

                Button(
                    onClick = onRegisterClick,

                    enabled = !isLoading,

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),

                    shape = RoundedCornerShape(15.dp),

                    colors = ButtonDefaults.buttonColors(

                        containerColor = lightBlue,

                        contentColor = primaryBlue,

                        disabledContainerColor =
                            Color(0xFFE5F1F4),

                        disabledContentColor =
                            Color(0xFF8BAAB5)
                    )
                ) {

                    Icon(
                        imageVector = Icons.Default.Person,

                        contentDescription = "Register",

                        modifier = Modifier.size(18.dp)
                    )


                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )


                    Text(
                        text = "Create New Account",

                        fontSize = 14.sp,

                        fontWeight = FontWeight.Bold
                    )
                }
            }


            Spacer(
                modifier = Modifier.height(16.dp)
            )


            // =================================================
            // SECURITY BADGE
            // =================================================

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = Color.White.copy(
                                alpha = 0.85f
                            ),

                            shape = CircleShape
                        ),

                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.Lock,

                        contentDescription = "Secure",

                        tint = primaryBlue,

                        modifier = Modifier.size(13.dp)
                    )
                }


                Spacer(
                    modifier = Modifier.width(7.dp)
                )


                Text(
                    text =
                        "Secure authentication powered by Supabase",

                    color = Color(0xFF5D7E8B),

                    fontSize = 10.sp
                )
            }
        }
    }
}

// =========================================================
// RECENT EMAIL SUGGESTIONS
// =========================================================

private const val LOGIN_PREFS_NAME = "medassist_login_preferences"
private const val RECENT_EMAILS_KEY = "recent_emails"
private const val MAX_RECENT_EMAILS = 5

private fun loadRecentEmails(
    context: Context
): List<String> {

    val preferences =
        context.getSharedPreferences(
            LOGIN_PREFS_NAME,
            Context.MODE_PRIVATE
        )

    return preferences
        .getStringSet(
            RECENT_EMAILS_KEY,
            emptySet()
        )
        ?.toList()
        ?.sorted()
        ?: emptyList()
}

private fun saveRecentEmail(
    context: Context,
    email: String,
    currentEmails: List<String>
): List<String> {

    val cleanEmail = email.trim()

    if (cleanEmail.isBlank()) {
        return currentEmails
    }

    val updated =
        buildList {

            add(cleanEmail)

            currentEmails.forEach { existing ->

                if (
                    !existing.equals(
                        cleanEmail,
                        ignoreCase = true
                    )
                ) {
                    add(existing)
                }
            }
        }
            .take(MAX_RECENT_EMAILS)

    context
        .getSharedPreferences(
            LOGIN_PREFS_NAME,
            Context.MODE_PRIVATE
        )
        .edit()
        .putStringSet(
            RECENT_EMAILS_KEY,
            updated.toSet()
        )
        .apply()

    return updated
}
