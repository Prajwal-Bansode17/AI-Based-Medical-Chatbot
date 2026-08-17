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

    /*
     * Navigation stack.
     *
     * Example:
     *
     * login
     *    ↓
     * dashboard
     *    ↓
     * medicine
     *    ↓
     * medicineDetail
     *
     * Stack:
     *
     * [login, dashboard, medicine, medicineDetail]
     */
    val screenStack = remember {
        mutableStateListOf("splash")
    }

    /*
     * Stores the logged-in user's name.
     */
    val userNameState = remember {
        mutableStateOf("")
    }

    /*
     * Stores the medicine selected by the user.
     */
    val selectedMedicineState = remember {
        mutableStateOf<ui.Medicine?>(null)
    }

    /*
     * Current screen is always the last item
     * in the navigation stack.
     */
    val currentScreen = screenStack.last()

    /*
     * Open a new screen.
     */
    fun navigateTo(screen: String) {
        screenStack.add(screen)
    }

    /*
     * Go back to the previous screen.
     */
    fun navigateBack() {
        if (screenStack.size > 1) {
            screenStack.removeAt(screenStack.lastIndex)
        }
    }

    /*
     * Android/mobile BACK button.
     *
     * Example:
     *
     * Dashboard
     *     ↓
     * Medicine
     *
     * Mobile Back
     *     ↓
     * Dashboard
     */
    BackHandler(
        enabled = screenStack.size > 1
    ) {
        navigateBack()
    }

    when (currentScreen) {

        // ====================================================
        // SPLASH
        // ====================================================

        "splash" -> {

            SplashScreen(
                onSplashFinished = {

                    /*
                     * Splash should not remain in the
                     * navigation history.
                     *
                     * Splash → Login
                     */
                    screenStack.clear()
                    screenStack.add("login")
                }
            )
        }

        // ====================================================
        // LOGIN
        // ====================================================

        "login" -> {

            LoginScreen(

                onLoginClick = { email ->

                    val namePart = email.substringBefore("@")

                    userNameState.value = when {

                        namePart.contains(".") -> {
                            namePart
                                .substringBefore(".")
                                .replaceFirstChar {
                                    it.uppercase()
                                }
                        }

                        namePart.contains("_") -> {
                            namePart
                                .substringBefore("_")
                                .replaceFirstChar {
                                    it.uppercase()
                                }
                        }

                        namePart.contains("-") -> {
                            namePart
                                .substringBefore("-")
                                .replaceFirstChar {
                                    it.uppercase()
                                }
                        }

                        else -> {
                            namePart
                                .replace(
                                    Regex("([a-z])([A-Z]).*"),
                                    "$1"
                                )
                                .replaceFirstChar {
                                    it.uppercase()
                                }
                        }
                    }

                    /*
                     * Login successful.
                     *
                     * Login → Dashboard
                     */
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

        // ====================================================
        // REGISTER
        // ====================================================

        "register" -> {

            RegisterScreen(
                onBackToLogin = {
                    navigateBack()
                }
            )
        }

        // ====================================================
        // FORGOT PASSWORD
        // ====================================================

        "forgotPassword" -> {

            ForgotPasswordScreen(
                onBackToLogin = {
                    navigateBack()
                }
            )
        }

        // ====================================================
        // DASHBOARD
        // ====================================================

        "dashboard" -> {

            DashboardScreen(

                userName = userNameState.value,

                /*
                 * Dashboard → Chatbot
                 */
                onChatbotClick = {
                    navigateTo("chatbot")
                },

                /*
                 * Dashboard → Symptoms Checker
                 */
                onSymptomsClick = {
                    navigateTo("symptoms")
                },

                /*
                 * Dashboard → Medicine Information
                 */
                onMedicineClick = {
                    navigateTo("medicine")
                },

                /*
                 * Dashboard → Health Tips
                 */
                onHealthTipsClick = {
                    navigateTo("healthTips")
                }
            )
        }

        // ====================================================
        // CHATBOT
        // ====================================================

        "chatbot" -> {

            ChatbotScreen(
                onBackClick = {
                    navigateBack()
                }
            )
        }

        // ====================================================
        // SYMPTOMS CHECKER
        // ====================================================

        "symptoms" -> {

            SymptomsCheckerScreen(
                onBackClick = {
                    navigateBack()
                }
            )
        }

        // ====================================================
        // MEDICINE INFORMATION
        // ====================================================

        "medicine" -> {

            MedicineInfoScreen(

                /*
                 * Medicine screen → Dashboard
                 */
                onBackClick = {
                    navigateBack()
                },

                /*
                 * Medicine selected.
                 *
                 * Medicine → Medicine Detail
                 */
                onMedicineClick = { medicine ->

                    selectedMedicineState.value = medicine

                    navigateTo("medicineDetail")
                }
            )
        }

        // ====================================================
        // MEDICINE DETAIL
        // ====================================================

        "medicineDetail" -> {

            val selectedMedicine =
                selectedMedicineState.value

            /*
             * Safety check.
             *
             * If no medicine is selected,
             * return to Medicine Information.
             */
            if (selectedMedicine == null) {

                navigateBack()

            } else {

                MedicineDetailScreen(

                    medicine = selectedMedicine,

                    /*
                     * Medicine Detail → Medicine Information
                     */
                    onBackClick = {
                        navigateBack()
                    }
                )
            }
        }

        // ====================================================
        // HEALTH TIPS
        // ====================================================

        "healthTips" -> {

            HealthTipsScreen(
                onBackClick = {
                    navigateBack()
                }
            )
        }
    }
}