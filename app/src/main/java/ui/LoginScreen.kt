package ui

import android.content.Context

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
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

    val context = LocalContext.current

    var recentEmails by remember {
        mutableStateOf(
            loadRecentEmails(context)
        )
    }

    var emailFieldFocused by remember {
        mutableStateOf(false)
    }

    // =========================================================
    // MEDASSIST COLORS
    // =========================================================

    val backgroundTop = Color(0xFFF7FCFF)
    val backgroundBottom = Color(0xFFE4F5FA)

    val primaryBlue = Color(0xFF1976D2)
    val primaryDark = Color(0xFF123A56)

    val teal = Color(0xFF009688)

    val lightBlue = Color(0xFFE8F6FB)

    val textDark = Color(0xFF172B4D)
    val textGray = Color(0xFF687B8B)

    val borderColor = Color(0xFFD5E4EC)

    val errorRed = Color(0xFFD32F2F)

    // =========================================================
    // MAIN SCREEN
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
        // DECORATIVE TOP RIGHT CIRCLE
        // =====================================================

        Box(
            modifier = Modifier
                .size(210.dp)
                .align(Alignment.TopEnd)
                .background(
                    color = primaryBlue.copy(
                        alpha = 0.08f
                    ),
                    shape = CircleShape
                )
        )

        // =====================================================
        // DECORATIVE BOTTOM LEFT CIRCLE
        // =====================================================

        Box(
            modifier = Modifier
                .size(145.dp)
                .align(Alignment.BottomStart)
                .background(
                    color = teal.copy(
                        alpha = 0.06f
                    ),
                    shape = CircleShape
                )
        )

        // =====================================================
        // SCROLLABLE CONTENT
        // =====================================================

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 22.dp,
                    vertical = 20.dp
                ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            // =================================================
            // LOGO
            // =================================================

            Box(
                modifier = Modifier
                    .size(78.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(25.dp)
                    )
                    .background(
                        Color.White,
                        RoundedCornerShape(25.dp)
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
                            Brush.linearGradient(
                                colors = listOf(
                                    primaryBlue,
                                    teal
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "MEDASSIST AI",
                        tint = Color.White,
                        modifier = Modifier.size(31.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            // =================================================
            // APP NAME
            // =================================================

            Text(
                text = "MEDASSIST AI",
                color = primaryDark,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "Your intelligent health companion",
                color = textGray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            // =================================================
            // LOGIN CARD
            // =================================================

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(27.dp)
                    )
                    .background(
                        Color.White,
                        RoundedCornerShape(27.dp)
                    )
                    .padding(21.dp)
            ) {

                // =============================================
                // WELCOME
                // =============================================

                Text(
                    text = "Welcome Back 👋",
                    color = textDark,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Text(
                    text = "Sign in to continue to your health dashboard.",
                    color = textGray,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                // =============================================
                // EMAIL LABEL
                // =============================================

                Text(
                    text = "Email address",
                    color = textDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    modifier = Modifier.height(7.dp)
                )

                // =============================================
                // EMAIL FIELD
                // =============================================

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
                            .fillMaxWidth(),

                        enabled = !isLoading,

                        singleLine = true,

                        placeholder = {
                            Text(
                                text = "Enter your email",
                                color = Color(0xFF9AAAB3),
                                fontSize = 14.sp
                            )
                        },

                        leadingIcon = {
                            Icon(
                                imageVector =
                                    Icons.Default.Email,
                                contentDescription = "Email",
                                tint = primaryBlue
                            )
                        },

                        isError =
                            emailError.isNotEmpty(),

                        shape =
                            RoundedCornerShape(15.dp),

                        colors =
                            OutlinedTextFieldDefaults.colors(

                                focusedBorderColor =
                                    primaryBlue,

                                unfocusedBorderColor =
                                    borderColor,

                                focusedContainerColor =
                                    Color(0xFFFBFDFE),

                                unfocusedContainerColor =
                                    Color(0xFFFBFDFE),

                                cursorColor =
                                    primaryBlue,

                                focusedTextColor =
                                    textDark,

                                unfocusedTextColor =
                                    textDark
                            )
                    )

                    // =========================================
                    // EMAIL SUGGESTIONS
                    // =========================================

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

                        modifier =
                            Modifier.fillMaxWidth(0.92f)
                    ) {

                        filteredSuggestions.forEach {
                                suggestion ->

                            DropdownMenuItem(

                                text = {
                                    Text(
                                        text = suggestion,
                                        color = textDark,
                                        fontSize = 14.sp
                                    )
                                },

                                onClick = {
                                    email =
                                        suggestion

                                    emailError =
                                        ""

                                    emailFieldFocused =
                                        false
                                }
                            )
                        }
                    }
                }

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
                    modifier = Modifier.height(15.dp)
                )

                // =============================================
                // PASSWORD LABEL
                // =============================================

                Text(
                    text = "Password",
                    color = textDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    modifier = Modifier.height(7.dp)
                )

                // =============================================
                // PASSWORD FIELD
                // =============================================

                OutlinedTextField(

                    value = password,

                    onValueChange = {
                        password = it
                        passwordError = ""
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    enabled = !isLoading,

                    singleLine = true,

                    placeholder = {
                        Text(
                            text =
                                "Enter your password",
                            color =
                                Color(0xFF9AAAB3),
                            fontSize = 14.sp
                        )
                    },

                    leadingIcon = {
                        Icon(
                            imageVector =
                                Icons.Default.Lock,
                            contentDescription =
                                "Password",
                            tint =
                                primaryBlue
                        )
                    },

                    trailingIcon = {

                        TextButton(
                            onClick = {
                                passwordVisible =
                                    !passwordVisible
                            },
                            enabled = !isLoading
                        ) {

                            Text(
                                text =
                                    if (passwordVisible) {
                                        "HIDE"
                                    } else {
                                        "SHOW"
                                    },

                                color =
                                    primaryBlue,

                                fontSize = 11.sp,

                                fontWeight =
                                    FontWeight.Bold
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

                    shape =
                        RoundedCornerShape(15.dp),

                    colors =
                        OutlinedTextFieldDefaults.colors(

                            focusedBorderColor =
                                primaryBlue,

                            unfocusedBorderColor =
                                borderColor,

                            focusedContainerColor =
                                Color(0xFFFBFDFE),

                            unfocusedContainerColor =
                                Color(0xFFFBFDFE),

                            cursorColor =
                                primaryBlue,

                            focusedTextColor =
                                textDark,

                            unfocusedTextColor =
                                textDark
                        )
                )

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

                // =============================================
                // FORGOT PASSWORD
                // =============================================

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
                                primaryBlue,

                            fontSize =
                                12.sp,

                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }
                }

                // =============================================
                // LOGIN ERROR
                // =============================================

                if (loginError.isNotEmpty()) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color(0xFFFFF3F3),
                                RoundedCornerShape(13.dp)
                            )
                            .border(
                                1.dp,
                                Color(0xFFFFD6D6),
                                RoundedCornerShape(13.dp)
                            )
                            .padding(11.dp)
                    ) {

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(27.dp)
                                    .background(
                                        Color(0xFFFFE1E1),
                                        CircleShape
                                    ),
                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Text(
                                    text = "!",
                                    color = errorRed,
                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }

                            Spacer(
                                modifier =
                                    Modifier.width(9.dp)
                            )

                            Text(
                                text = loginError,
                                color =
                                    Color(0xFFB52F2F),
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                modifier =
                                    Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )
                }

                // =============================================
                // SIGN IN
                // =============================================

                Button(

                    onClick = {

                        emailError = ""
                        passwordError = ""

                        val cleanEmail =
                            email.trim()

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

                        recentEmails =
                            saveRecentEmail(
                                context =
                                    context,
                                email =
                                    cleanEmail,
                                currentEmails =
                                    recentEmails
                            )

                        onLoginClick(
                            cleanEmail,
                            password
                        )
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),

                    enabled = !isLoading,

                    shape =
                        RoundedCornerShape(16.dp),

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                primaryBlue,
                            disabledContainerColor =
                                Color(0xFF9FC6D5)
                        )
                ) {

                    if (isLoading) {

                        CircularProgressIndicator(
                            modifier =
                                Modifier.size(21.dp),

                            color =
                                Color.White,

                            strokeWidth =
                                2.5.dp
                        )

                    } else {

                        Text(
                            text = "Sign In",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                // =============================================
                // OR
                // =============================================

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                Color(0xFFE2EAEE)
                            )
                    )

                    Text(
                        text = "  OR  ",
                        color =
                            Color(0xFF9AAAB3),
                        fontSize = 11.sp
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                Color(0xFFE2EAEE)
                            )
                    )
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                // =============================================
                // CREATE ACCOUNT
                // =============================================

                Button(

                    onClick =
                        onRegisterClick,

                    enabled =
                        !isLoading,

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp),

                    shape =
                        RoundedCornerShape(15.dp),

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                lightBlue,
                            contentColor =
                                primaryBlue
                        )
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Person,

                        contentDescription =
                            "Register",

                        modifier =
                            Modifier.size(18.dp)
                    )

                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )

                    Text(
                        text =
                            "Create New Account",

                        fontSize =
                            14.sp,

                        fontWeight =
                            FontWeight.Bold
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
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(25.dp)
                        .background(
                            Color.White.copy(
                                alpha = 0.9f
                            ),
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

                        tint =
                            primaryBlue,

                        modifier =
                            Modifier.size(13.dp)
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(7.dp)
                )

                Text(
                    text =
                        "Secure authentication powered by Supabase",

                    color =
                        Color(0xFF5D7E8B),

                    fontSize =
                        10.sp
                )
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )
        }
    }
}


// =============================================================
// RECENT EMAIL STORAGE
// =============================================================

private const val LOGIN_PREFS_NAME =
    "medassist_login_preferences"

private const val RECENT_EMAILS_KEY =
    "recent_emails"

private const val MAX_RECENT_EMAILS =
    5


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

    val cleanEmail =
        email.trim()

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