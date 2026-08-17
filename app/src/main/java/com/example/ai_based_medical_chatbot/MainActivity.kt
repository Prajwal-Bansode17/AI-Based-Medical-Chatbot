package com.example.ai_based_medical_chatbot

import ui.DashboardScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.ai_based_medical_chatbot.ui.theme.AIBasedMedicalChatbotTheme
import ui.LoginScreen
import ui.RegisterScreen
import ui.SplashScreen
import ui.ForgotPasswordScreen
import ui.ChatbotScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AIBasedMedicalChatbotTheme {

                var currentScreen by remember {
                    mutableStateOf("splash")
                }

                var userName by remember {
                    mutableStateOf("")
                }

                when (currentScreen) {

                    "dashboard" -> {
                        DashboardScreen(
                            userName = userName,
                            onChatbotClick = {
                                currentScreen = "chatbot"
                            }
                        )
                    }

                    "splash" -> {
                        SplashScreen(
                            onSplashFinished = {
                                currentScreen = "login"
                            }
                        )
                    }

                    "login" -> {
                        LoginScreen(
                            onLoginClick = { email ->

                                val namePart = email.substringBefore("@")

                                userName = when {
                                    namePart.contains(".") ->
                                        namePart.substringBefore(".")
                                            .replaceFirstChar { it.uppercase() }

                                    namePart.contains("_") ->
                                        namePart.substringBefore("_")
                                            .replaceFirstChar { it.uppercase() }

                                    namePart.contains("-") ->
                                        namePart.substringBefore("-")
                                            .replaceFirstChar { it.uppercase() }

                                    else ->
                                        namePart
                                            .replace(Regex("([a-z])([A-Z]).*"), "$1")
                                            .replaceFirstChar { it.uppercase() }
                                }

                                currentScreen = "dashboard"
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
                    "chatbot" -> {
                        ChatbotScreen(
                            onBackClick = {
                                currentScreen = "dashboard"
                            }
                        )
                    }
                }
            }
        }
    }
}