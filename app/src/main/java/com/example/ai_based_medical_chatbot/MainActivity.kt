package com.example.ai_based_medical_chatbot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.ai_based_medical_chatbot.ui.theme.AIBasedMedicalChatbotTheme
import ui.ChatbotScreen
import ui.DashboardScreen
import ui.ForgotPasswordScreen
import ui.HealthTipsScreen
import ui.LoginScreen
import ui.MedicineDetailScreen
import ui.MedicineInfoScreen
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

    // Navigation stack
    val screenStack = remember {
        mutableStateListOf("splash")
    }

    // Logged-in user's name
    val userNameState = remember {
        mutableStateOf("")
    }

    // Currently selected medicine
    val selectedMedicineState = remember {
        mutableStateOf<ui.Medicine?>(null)
    }

    // Current screen
    val currentScreen = screenStack.last()

    // Navigate to a new screen
    fun navigateTo(screen: String) {
        screenStack.add(screen)
    }

    // Go back to previous screen
    fun navigateBack() {
        if (screenStack.size > 1) {
            screenStack.removeAt(screenStack.lastIndex)
        }
    }

    // Handle Android/mobile back button
    BackHandler(
        enabled = screenStack.size > 1
    ) {
        navigateBack()
    }

    when (currentScreen) {

        // ------------------------------------------------
        // SPLASH
        // ------------------------------------------------

        "splash" -> {

            SplashScreen(
                onSplashFinished = {

                    // Splash should not remain in back stack
                    screenStack.clear()
                    screenStack.add("login")
                }
            )
        }

        // ------------------------------------------------
        // LOGIN
        // ------------------------------------------------

        "login" -> {

            LoginScreen(

                onLoginClick = { email ->

                    val namePart = email.substringBefore("@")

                    userNameState.value = when {

                        namePart.contains(".") ->
                            namePart
                                .substringBefore(".")
                                .replaceFirstChar {
                                    it.uppercase()
                                }

                        namePart.contains("_") ->
                            namePart
                                .substringBefore("_")
                                .replaceFirstChar {
                                    it.uppercase()
                                }

                        namePart.contains("-") ->
                            namePart
                                .substringBefore("-")
                                .replaceFirstChar {
                                    it.uppercase()
                                }

                        else ->
                            namePart
                                .replace(
                                    Regex("([a-z])([A-Z]).*"),
                                    "$1"
                                )
                                .replaceFirstChar {
                                    it.uppercase()
                                }
                    }

                    // Login successful
                    navigateTo("dashboard")
                },

                onRegisterClick = {
                    navigateTo("register")
                },

                onForgotPasswordClick = {
                    navigateTo("forgotPassword")
                }
            )
        }

        // ------------------------------------------------
        // REGISTER
        // ------------------------------------------------

        "register" -> {

            RegisterScreen(
                onBackToLogin = {
                    navigateBack()
                }
            )
        }

        // ------------------------------------------------
        // FORGOT PASSWORD
        // ------------------------------------------------

        "forgotPassword" -> {

            ForgotPasswordScreen(
                onBackToLogin = {
                    navigateBack()
                }
            )
        }

        // ------------------------------------------------
        // DASHBOARD
        // ------------------------------------------------

        "dashboard" -> {

            DashboardScreen(
                userName = userNameState.value,

                onChatbotClick = {
                    navigateTo("chatbot")
                },

                onMedicineClick = {
                    navigateTo("medicine")
                },

                onSymptomsClick = {
                    navigateTo("symptoms")
                },

                onHealthTipsClick = {
                    navigateTo("healthTips")
                }
            )
        }

        // ------------------------------------------------
        // CHATBOT
        // ------------------------------------------------

        "chatbot" -> {

            ChatbotScreen(
                onBackClick = {
                    navigateBack()
                }
            )
        }

        // ------------------------------------------------
        // SYMPTOMS CHECKER
        // ------------------------------------------------

        "symptoms" -> {

            SymptomsCheckerScreen(
                onBackClick = {
                    navigateBack()
                }
            )
        }

        // ------------------------------------------------
        // MEDICINE INFORMATION
        // ------------------------------------------------

        "medicine" -> {

            MedicineInfoScreen(
                onBackClick = {
                    navigateBack()
                },

                onMedicineClick = { medicine ->

                    selectedMedicineState.value = medicine

                    navigateTo("medicineDetail")
                }
            )
        }

        // ------------------------------------------------
        // MEDICINE DETAIL
        // ------------------------------------------------

        "medicineDetail" -> {

            val selectedMedicine = selectedMedicineState.value

            if (selectedMedicine == null) {

                navigateBack()

            } else {

                MedicineDetailScreen(
                    medicine = selectedMedicine,

                    onBackClick = {
                        navigateBack()
                    }
                )
            }
        }

        // ------------------------------------------------
        // HEALTH TIPS
        // ------------------------------------------------

        "healthTips" -> {

            HealthTipsScreen(
                onBackClick = {
                    navigateBack()
                }
            )
        }
    }
}