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
import androidx.compose.ui.platform.LocalContext
import com.example.ai_based_medical_chatbot.data.SupabaseClient
import com.example.ai_based_medical_chatbot.ui.theme.AIBasedMedicalChatbotTheme
import kotlinx.coroutines.launch
import ui.ChatbotScreen
import ui.DashboardScreen
import ui.ForgotPasswordScreen
import ui.HealthTipsScreen
import ui.LoginScreen
import ui.MedicineDetailScreen
import ui.MedicineInfoScreen
import ui.ProfileScreen
import ui.RegisterScreen
import ui.SplashScreen
import ui.SymptomsCheckerScreen


class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
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

    // =========================================================
    // CONTEXT
    // =========================================================

    val context =
        LocalContext.current


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
        mutableStateOf("")
    }

    var userEmail by remember {
        mutableStateOf("")
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
    // SELECTED MEDICINE
    // =========================================================

    var selectedMedicine by remember {
        mutableStateOf<ui.Medicine?>(null)
    }


    // =========================================================
    // CURRENT SCREEN
    // =========================================================

    val currentScreen =
        screenStack.lastOrNull()
            ?: "login"


    // =========================================================
    // NAVIGATION
    // =========================================================

    fun navigateTo(
        screen: String
    ) {

        screenStack.add(screen)
    }


    fun navigateBack() {

        if (
            screenStack.size > 1
        ) {

            screenStack.removeAt(
                screenStack.lastIndex
            )
        }
    }


    // =========================================================
    // ANDROID BACK BUTTON
    // =========================================================

    BackHandler(
        enabled =
            screenStack.size > 1
    ) {

        navigateBack()
    }


    // =========================================================
    // SCREEN ROUTING
    // =========================================================

    when (currentScreen) {


        // =====================================================
        // SPLASH
        // =====================================================

        "splash" -> {

            SplashScreen(

                onSplashFinished = {

                    screenStack.clear()

                    screenStack.add(
                        "login"
                    )
                }
            )
        }


        // =====================================================
        // LOGIN
        // =====================================================

        "login" -> {

            LoginScreen(

                onLoginClick = {
                        email,
                        password ->

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
                            // LOGIN SUCCESS
                            // =================================

                            result.onSuccess { user ->

                                userEmail =
                                    user.email

                                userName =
                                    if (
                                        user.fullName
                                            .isNotBlank()
                                    ) {

                                        user.fullName

                                    } else {

                                        user.email
                                            .substringBefore(
                                                "@"
                                            )
                                            .replaceFirstChar {
                                                it.uppercase()
                                            }
                                    }

                                loginLoading = false
                                loginError = ""

                                screenStack.clear()

                                screenStack.add(
                                    "dashboard"
                                )
                            }


                            // =================================
                            // LOGIN FAILURE
                            // =================================

                            result.onFailure { error ->

                                loginLoading = false

                                loginError =
                                    error.message
                                        ?: "Login failed. Please try again."

                                Log.e(
                                    "SupabaseLogin",
                                    loginError,
                                    error
                                )
                            }

                        } catch (
                            e: Exception
                        ) {

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
        // REGISTER
        // =====================================================

        "register" -> {

            RegisterScreen(

                onRegisterClick = {

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

                    navigateTo(
                        "chatbot"
                    )
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
                }
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

                    // Clear saved Supabase session
                    SupabaseClient.clearSession(
                        context
                    )

                    // Clear UI user data
                    userName = ""
                    userEmail = ""

                    loginError = ""
                    loginLoading = false

                    // Go to login
                    screenStack.clear()

                    screenStack.add(
                        "login"
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

                onMedicineClick = {
                        medicine ->

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

            if (
                medicine == null
            ) {

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