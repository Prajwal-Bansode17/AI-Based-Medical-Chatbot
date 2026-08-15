package com.example.ai_based_medical_chatbot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.ai_based_medical_chatbot.ui.theme.AIBasedMedicalChatbotTheme
import ui.LoginScreen
import ui.RegisterScreen
import ui.SplashScreen
import ui.ForgotPasswordScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AIBasedMedicalChatbotTheme {

                var currentScreen by remember {
                    mutableStateOf("splash")
                }

                when (currentScreen) {

                    "splash" -> {
                        SplashScreen(
                            onSplashFinished = {
                                currentScreen = "login"
                            }
                        )
                    }

                    "login" -> {
                        LoginScreen(
                            onLoginClick = {
                                // Dashboard will be connected later
                            },
                            onRegisterClick = {
                                currentScreen = "register"
                            },
                            onForgotPasswordClick = {
                                currentScreen = "forgotPassword"
                            }
                        )
                    }

                    "register" -> {
                        RegisterScreen(
                            onBackToLogin = {
                                currentScreen = "login"
                            }
                        )
                    }

                    "forgotPassword" -> {
                        ForgotPasswordScreen(
                            onBackToLogin = {
                                currentScreen = "login"
                            }
                        )
                    }
                }
            }
        }
    }
}