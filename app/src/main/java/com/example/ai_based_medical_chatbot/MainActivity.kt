package com.example.ai_based_medical_chatbot

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.ai_based_medical_chatbot.data.SupabaseClient
import com.example.ai_based_medical_chatbot.ui.theme.AIBasedMedicalChatbotTheme
import kotlinx.coroutines.launch
import ui.ChatHistoryScreen
import ui.ChatbotScreen
import ui.DashboardScreen
import ui.ForgotPasswordScreen
import ui.HealthTipsScreen
import ui.HealthProfileScreen
import ui.LoginScreen
import ui.MedicineDetailScreen
import ui.MedicineInfoScreen
import ui.ProfileScreen
import ui.RegisterScreen
import ui.SplashScreen
import ui.SymptomsCheckerScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AIBasedMedicalChatbotTheme {
                MedicalChatbotNavigation()
            }
        }
    }
}

@Composable
private fun MedicalChatbotNavigation() {

    val context = androidx.compose.ui.platform.LocalContext.current

    // =========================================================
    // NAVIGATION STACK
    // =========================================================

    val screenStack =
        remember {
            mutableStateListOf("splash")
        }

    // =========================================================
    // COROUTINE SCOPE
    // =========================================================

    val scope =
        rememberCoroutineScope()

    // =========================================================
    // USER INFORMATION
    // =========================================================

    var userName by remember {
        mutableStateOf(
            SupabaseClient.getSavedUser(context)?.fullName.orEmpty()
        )
    }

    var userEmail by remember {
        mutableStateOf(
            SupabaseClient.getSavedUser(context)?.email.orEmpty()
        )
    }

    // =========================================================
    // LOGIN STATE
    // =========================================================

    var loginLoading by remember {
        mutableStateOf(false)
    }

    var loginError by remember {
        mutableStateOf("")
    }

    // =========================================================
    // CHATBOT ACCESS STATE
    // =========================================================

    // True when the user requested the chatbot and must return
    // to the chatbot after authentication/profile setup.
    var pendingChatbotAccess by remember {
        mutableStateOf(false)
    }

    // =========================================================
    // SELECTED MEDICINE
    // =========================================================

    var selectedMedicine by remember {
        mutableStateOf<ui.Medicine?>(null)
    }

    // =========================================================
    // CURRENT SCREEN
    // =========================================================

    val currentScreen =
        screenStack.lastOrNull() ?: "dashboard"

    // =========================================================
    // NAVIGATION FUNCTIONS
    // =========================================================

    fun navigateTo(screen: String) {
        screenStack.add(screen)
    }

    fun navigateBack() {

        if (screenStack.size > 1) {

            screenStack.removeAt(
                screenStack.lastIndex
            )
        }
    }

    // =========================================================
    // ANDROID BACK BUTTON
    // =========================================================

    BackHandler(
        enabled = screenStack.size > 1
    ) {
        navigateBack()
    }

    // =========================================================
    // SCREEN ROUTING
    // =========================================================

    when (currentScreen) {

        // =====================================================
        // SPLASH SCREEN
        // =====================================================

        "splash" -> {

            SplashScreen(

                onSplashFinished = {

                    val savedUser =
                        SupabaseClient.getSavedUser(context)

                    screenStack.clear()

                    if (savedUser != null) {

                        userEmail =
                            savedUser.email

                        userName =
                            savedUser.fullName.ifBlank {
                                savedUser.email
                                    .substringBefore("@")
                                    .replaceFirstChar {
                                        it.uppercase()
                                    }
                            }

                        screenStack.add(
                            "dashboard"
                        )

                    } else {

                        // Login is intentionally NOT required at app startup.
                        // Users can explore the dashboard and authenticate only
                        // when they choose to open the chatbot.
                        screenStack.add(
                            "dashboard"
                        )
                    }
                }
            )
        }

        // =====================================================
        // LOGIN SCREEN
        // =====================================================

        "login" -> {

            LoginScreen(

                onLoginClick = { email, password ->

                    scope.launch {

                        loginLoading = true
                        loginError = ""

                        try {

                            val result =
                                SupabaseClient.loginUser(
                                    email = email,
                                    password = password,
                                    context = context
                                )

                            // =================================
                            // SUCCESS
                            // =================================

                            result.onSuccess { user ->

                                userEmail =
                                    user.email

                                userName =
                                    if (
                                        user.fullName.isNotBlank()
                                    ) {

                                        user.fullName

                                    } else {

                                        user.email
                                            .substringBefore("@")
                                            .replaceFirstChar {
                                                it.uppercase()
                                            }
                                    }

                                loginLoading = false
                                loginError = ""

                                // If login was requested because the user
                                // wanted the chatbot, continue to the optional
                                // personal-health profile first.
                                screenStack.clear()

                                if (pendingChatbotAccess) {
                                    screenStack.add(
                                        "healthProfile"
                                    )
                                } else {
                                    screenStack.add(
                                        "dashboard"
                                    )
                                }

                                pendingChatbotAccess = false
                            }

                            // =================================
                            // FAILURE
                            // =================================

                            result.onFailure { error ->

                                loginLoading = false

                                loginError =
                                    error.message
                                        ?: "Login failed. Please try again."

                                Log.e(
                                    "SupabaseLogin",
                                    loginError
                                )
                            }

                        } catch (e: Exception) {

                            loginLoading = false

                            loginError =
                                e.message
                                    ?: "Unable to connect to Supabase."

                            Log.e(
                                "SupabaseLogin",
                                loginError,
                                e
                            )
                        }
                    }
                },

                // =============================================
                // REGISTER
                // =============================================

                onRegisterClick = {

                    loginError = ""

                    navigateTo(
                        "register"
                    )
                },

                // =============================================
                // FORGOT PASSWORD
                // =============================================

                onForgotPasswordClick = {

                    loginError = ""

                    navigateTo(
                        "forgotPassword"
                    )
                },

                // =============================================
                // LOADING
                // =============================================

                isLoading =
                    loginLoading,

                // =============================================
                // ERROR
                // =============================================

                loginError =
                    loginError
            )
        }

        // =====================================================
        // REGISTER SCREEN
        // =====================================================

        "register" -> {

            RegisterScreen(

                onRegisterClick = {

                    // Registration successful
                    // Return to Login
                    navigateBack()
                },

                onLoginClick = {

                    navigateBack()
                },

                onBackToLogin = {

                    navigateBack()
                }
            )
        }

        // =====================================================
        // FORGOT PASSWORD
        // =====================================================

        "forgotPassword" -> {

            ForgotPasswordScreen(

                onBackToLogin = {

                    navigateBack()
                }
            )
        }

        // =====================================================
        // DASHBOARD
        // =====================================================

        "dashboard" -> {

            DashboardScreen(

                userName =
                    userName,

                onProfileClick = {

                    navigateTo(
                        "profile"
                    )
                },

                onChatbotClick = {

                    val savedUser =
                        SupabaseClient.getSavedUser(context)

                    if (savedUser == null) {

                        // Authentication is required only when the
                        // user chooses to access the chatbot.
                        pendingChatbotAccess = true

                        navigateTo(
                            "login"
                        )

                    } else {

                        // Existing users go through the optional
                        // personal-information page before entering chat.
                        pendingChatbotAccess = true

                        navigateTo(
                            "healthProfile"
                        )
                    }
                },

                onSymptomsClick = {

                    navigateTo(
                        "symptoms"
                    )
                },

                onMedicineClick = {

                    navigateTo(
                        "medicine"
                    )
                },

                onHealthTipsClick = {

                    navigateTo(
                        "healthTips"
                    )
                },

                // =================================================
                // NEW: CHAT HISTORY
                // =================================================


            )
        }

        // =====================================================
        // PROFILE
        // =====================================================

        "profile" -> {

            ProfileScreen(

                userName =
                    userName,

                userEmail =
                    userEmail,

                onBackClick = {

                    navigateBack()
                },

                onLogoutClick = {

                    SupabaseClient.clearSession(
                        context
                    )

                    userName = ""
                    userEmail = ""
                    loginError = ""
                    loginLoading = false

                    pendingChatbotAccess = false

                    screenStack.clear()

                    screenStack.add(
                        "dashboard"
                    )
                }
            )
        }

        // =====================================================
        // PERSONAL HEALTH PROFILE
        // =====================================================

        "healthProfile" -> {

            HealthProfileScreen(

                onBack = {

                    pendingChatbotAccess = false
                    navigateBack()
                },

                onContinue = {

                    pendingChatbotAccess = false

                    screenStack.removeAll {
                        it == "healthProfile"
                    }

                    screenStack.add(
                        "chatbot"
                    )
                }
            )
        }

        // =====================================================
        // CHATBOT
        // =====================================================

        "chatbot" -> {

            ChatbotScreen(

                onBack = {

                    navigateBack()
                }
            )
        }

        // =====================================================
        // CHAT HISTORY
        // =====================================================

        "chatHistory" -> {

            ChatHistoryScreen(

                onBackClick = {

                    navigateBack()
                }
            )
        }

        // =====================================================
        // SYMPTOMS CHECKER
        // =====================================================

        "symptoms" -> {

            SymptomsCheckerScreen(

                onBackClick = {

                    navigateBack()
                }
            )
        }

        // =====================================================
        // MEDICINE INFORMATION
        // =====================================================

        "medicine" -> {

            MedicineInfoScreen(

                onBackClick = {

                    navigateBack()
                },

                onMedicineClick = { medicine ->

                    selectedMedicine =
                        medicine

                    navigateTo(
                        "medicineDetail"
                    )
                }
            )
        }

        // =====================================================
        // MEDICINE DETAIL
        // =====================================================

        "medicineDetail" -> {

            val medicine =
                selectedMedicine

            if (medicine == null) {

                navigateBack()

            } else {

                MedicineDetailScreen(

                    medicine =
                        medicine,

                    onBackClick = {

                        navigateBack()
                    }
                )
            }
        }

        // =====================================================
        // HEALTH TIPS
        // =====================================================

        "healthTips" -> {

            HealthTipsScreen(

                onBackClick = {

                    navigateBack()
                }
            )
        }
    }
}