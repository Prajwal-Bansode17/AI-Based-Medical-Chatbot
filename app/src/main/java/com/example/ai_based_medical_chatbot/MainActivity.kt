package com.example.ai_based_medical_chatbot

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import com.example.ai_based_medical_chatbot.data.SupabaseClient
import com.example.ai_based_medical_chatbot.data.api.RetrofitClient
import com.example.ai_based_medical_chatbot.ui.theme.AIBasedMedicalChatbotTheme
import kotlinx.coroutines.launch
import ui.BMICheckerScreen
import ui.ChatbotScreen
import ui.ChatHistoryRepository
import ui.ChatHistoryScreen
import ui.DashboardScreen
import ui.ForgotPasswordScreen
import ui.HealthProfileScreen
import ui.HealthTipsScreen
import ui.LoginScreen
import ui.MedicineDetailScreen
import ui.MedicineInfoScreen
import ui.MedicineReminderScreen
import ui.ProfileScreen
import ui.PrescriptionScannerScreen
import ui.RegisterScreen
import ui.ResetPasswordScreen
import ui.SplashScreen
import ui.SymptomsCheckerScreen

private const val AUTH_PREFS = "medassist_auth_preferences"
private const val BIOMETRIC_ENABLED = "biometric_enabled"

private fun biometricEnabled(context: Context): Boolean =
    context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
        .getBoolean(BIOMETRIC_ENABLED, false)

private fun setBiometricEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE).edit {
        putBoolean(BIOMETRIC_ENABLED, enabled)
    }
}

private fun deviceAuthenticationAvailable(context: Context): Boolean {
    val authenticators =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

    return BiometricManager.from(context).canAuthenticate(authenticators) ==
            BiometricManager.BIOMETRIC_SUCCESS
}

private fun openDeviceAuthentication(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)

    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence
            ) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
            }
        }
    )

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock MEDASSIST AI")
        .setSubtitle("Use fingerprint, face, PIN, pattern, or device password")
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()

    prompt.authenticate(promptInfo)
}

class MainActivity : FragmentActivity() {

    private var recoveryIntentUri = mutableStateOf<android.net.Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        RetrofitClient.initialize(applicationContext)

        intent?.data?.let { uri ->
            if (uri.scheme == "medassist" && uri.host == "reset-password") {
                recoveryIntentUri.value = uri
            }
        }

        setContent {
            AIBasedMedicalChatbotTheme {
                ProvideAppLanguage {
                    androidx.compose.material3.Surface(
                        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.background
                    ) {
                        MedicalChatbotNavigation(
                            recoveryUri = recoveryIntentUri.value,
                            onRecoveryUriHandled = { recoveryIntentUri.value = null }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let { uri ->
            if (uri.scheme == "medassist" && uri.host == "reset-password") {
                recoveryIntentUri.value = uri
            }
        }
    }
}

@Composable
private fun MedicalChatbotNavigation(
    recoveryUri: android.net.Uri? = null,
    onRecoveryUriHandled: () -> Unit = {}
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val screenStack = remember {
        mutableStateListOf("splash")
    }

    var userName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var loginLoading by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf("") }
    var biometricError by remember { mutableStateOf("") }
    var selectedMedicine by remember { mutableStateOf<ui.Medicine?>(null) }
    var reminderPrefill by remember { mutableStateOf<MedicineReminder?>(null) }
    var selectedHistorySessionId by remember { mutableStateOf<String?>(null) }
    var activeChatSessionId by remember { mutableStateOf<String?>(null) }
    var recoveryAccessToken by remember { mutableStateOf("") }
    var initialChatQuery by remember { mutableStateOf<String?>(null) }

    val currentScreen = screenStack.lastOrNull() ?: "login"

    // Deep Link Recovery Handler
    androidx.compose.runtime.LaunchedEffect(recoveryUri) {
        val uri = recoveryUri ?: return@LaunchedEffect
        // Safe extraction without logging raw sensitive tokens
        var token = ""

        // Check 1: Query parameters (e.g. ?token=... or ?access_token=...)
        val queryToken = uri.getQueryParameter("access_token")
            ?: uri.getQueryParameter("token")
        if (!queryToken.isNullOrBlank()) {
            token = queryToken.trim()
        }

        // Check 2: Fragment / Hash parameters (Supabase Auth default: #access_token=...&type=recovery)
        if (token.isBlank()) {
            val fragment = uri.fragment
            if (!fragment.isNullOrBlank()) {
                val params = fragment.split("&").associate { segment ->
                    val parts = segment.split("=", limit = 2)
                    if (parts.size == 2) parts[0].trim() to parts[1].trim() else "" to ""
                }
                val fragToken = params["access_token"] ?: params["token"]
                if (!fragToken.isNullOrBlank()) {
                    token = fragToken
                }
            }
        }

        if (token.isNotBlank()) {
            recoveryAccessToken = token
            screenStack.clear()
            screenStack.add("resetPassword")
        }
        onRecoveryUriHandled()
    }

    fun go(screen: String) {
        screenStack.add(screen)
    }

    fun back() {
        if (screenStack.size > 1) {
            screenStack.removeAt(screenStack.lastIndex)
        }
    }

    BackHandler(enabled = currentScreen != "login" && currentScreen != "dashboard") {
        when (currentScreen) {
            "forgotPassword", "register", "biometric", "resetPassword" -> {
                screenStack.clear()
                screenStack.add("login")
            }
            else -> back()
        }
    }

    when (currentScreen) {

        "splash" -> {
            SplashScreen(
                onSplashFinished = {

                    val savedUser =
                        SupabaseClient.getSavedUser(context)

                    screenStack.clear()

                    /*
                     * IMPORTANT:
                     * If Supabase has a saved user, always show
                     * the secure login screen. We do NOT require
                     * biometric availability just to display it.
                     */
                    if (savedUser != null && biometricEnabled(context)) {

                        userEmail = savedUser.email
                        userName =
                            if (savedUser.fullName.isNotBlank()) {
                                savedUser.fullName
                            } else {
                                savedUser.email
                                    .substringBefore("@")
                                    .replaceFirstChar { it.uppercase() }
                            }

                        biometricError = ""
                        screenStack.add("biometric")

                    } else {
                        screenStack.add("login")
                    }
                }
            )
        }

        "login" -> {
            LoginScreen(
                onLoginClick = { email, password ->

                    scope.launch {

                        loginLoading = true
                        loginError = ""

                        try {

                            val result = SupabaseClient.loginUser(
                                email = email,
                                password = password,
                                context = context
                            )

                            result.onSuccess { user ->

                                userEmail = user.email

                                userName =
                                    if (user.fullName.isNotBlank()) {
                                        user.fullName
                                    } else {
                                        user.email
                                            .substringBefore("@")
                                            .replaceFirstChar { it.uppercase() }
                                    }

                                loginLoading = false
                                loginError = ""

                                /*
                                 * IMPORTANT:
                                 * Enable the secure-login gate after every
                                 * successful normal login. Do not depend on
                                 * biometric hardware here.
                                 */
                                setBiometricEnabled(
                                    context,
                                    true
                                )

                                screenStack.clear()
                                screenStack.add("dashboard")
                            }

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

                onRegisterClick = {
                    loginError = ""
                    go("register")
                },

                onForgotPasswordClick = {
                    loginError = ""
                    go("forgotPassword")
                },

                isLoading = loginLoading,
                loginError = loginError
            )
        }

        "biometric" -> {
            LoginScreen(
                onLoginClick = { _, _ -> },

                onRegisterClick = {
                    screenStack.clear()
                    screenStack.add("login")
                },

                onForgotPasswordClick = {
                    screenStack.clear()
                    screenStack.add("forgotPassword")
                },

                biometricMode = true,

                onBiometricClick = {

                    biometricError = ""

                    if (!deviceAuthenticationAvailable(context)) {

                        biometricError =
                            "No fingerprint, face, PIN, pattern, or device password is configured on this phone."

                    } else {

                        val activity =
                            context as? FragmentActivity

                        if (activity == null) {

                            biometricError =
                                "Unable to open device authentication."

                        } else {

                            openDeviceAuthentication(
                                activity = activity,

                                onSuccess = {

                                    val savedUser =
                                        SupabaseClient.getSavedUser(context)

                                    if (savedUser == null) {

                                        setBiometricEnabled(
                                            context,
                                            false
                                        )

                                        biometricError =
                                            "Session expired. Please login with email and password."

                                        screenStack.clear()
                                        screenStack.add("login")

                                    } else {

                                        userEmail =
                                            savedUser.email

                                        userName =
                                            if (savedUser.fullName.isNotBlank()) {
                                                savedUser.fullName
                                            } else {
                                                savedUser.email
                                                    .substringBefore("@")
                                                    .replaceFirstChar {
                                                        it.uppercase()
                                                    }
                                            }

                                        biometricError = ""

                                        screenStack.clear()
                                        screenStack.add("dashboard")
                                    }
                                },

                                onError = { message ->
                                    biometricError = message
                                }
                            )
                        }
                    }
                },

                onUsePasswordClick = {
                    biometricError = ""
                    loginError = ""
                    screenStack.clear()
                    screenStack.add("login")
                },

                isLoading = false,
                loginError = biometricError
            )
        }

        "register" -> {
            RegisterScreen(
                onRegisterClick = {
                    back()
                },
                onLoginClick = {
                    screenStack.clear()
                    screenStack.add("login")
                },
                onBackToLogin = {
                    screenStack.clear()
                    screenStack.add("login")
                }
            )
        }

        "forgotPassword" -> {
            ForgotPasswordScreen(
                onBackToLogin = {
                    screenStack.clear()
                    screenStack.add("login")
                }
            )
        }

        "resetPassword" -> {
            ResetPasswordScreen(
                recoveryToken = recoveryAccessToken,
                onPasswordResetSuccess = {
                    recoveryAccessToken = ""
                    loginError = ""
                    screenStack.clear()
                    screenStack.add("login")
                },
                onCancel = {
                    recoveryAccessToken = ""
                    screenStack.clear()
                    screenStack.add("login")
                }
            )
        }

        "dashboard" -> {
            DashboardScreen(
                userName = userName,

                onProfileClick = {
                    go("profile")
                },

                onChatbotClick = {
                    initialChatQuery = null
                    go("chatbot")
                },

                onSymptomSubmit = { symptomQuery ->
                    initialChatQuery = symptomQuery
                    go("chatbot")
                },

                onBMIClick = {
                    go("bmi")
                },

                onSymptomsClick = {
                    go("symptoms")
                },

                onMedicineClick = {
                    go("medicine")
                },

                onHealthTipsClick = {
                    go("healthTips")
                },

                onPrescriptionClick = {
                    go("prescription")
                },

                onMedicineReminderClick = {
                    go("medicineReminder")
                },

                onHealthProfileClick = {
                    go("healthProfile")
                },

                onLogoutClick = {
                    val currentUserId = SupabaseClient.getSavedUser(context)?.id
                    ChatHistoryRepository.clearActiveSession(context, currentUserId)
                    SupabaseClient.clearSession(context)

                    setBiometricEnabled(
                        context,
                        false
                    )

                    userName = ""
                    userEmail = ""
                    loginError = ""
                    loginLoading = false
                    biometricError = ""

                    screenStack.clear()
                    screenStack.add("login")
                }
            )
        }

        "profile" -> {
            ProfileScreen(
                userName = userName,
                userEmail = userEmail,

                onBackClick = {
                    back()
                },

                onHealthProfileClick = {
                    go("healthProfile")
                },

                onSetReminderClick = { name, strength, frequency, duration, instructions, timing, mealTiming, howToTake ->
                    reminderPrefill = MedicineReminder(
                        medicineName = name,
                        strength = strength,
                        daySlot = timing.ifBlank { frequency },
                        mealTiming = mealTiming,
                        duration = duration,
                        instructions = if (howToTake.isNotBlank()) {
                            howToTake
                        } else {
                            instructions
                        }
                    )
                    go("medicineReminder")
                },

                onLogoutClick = {

                    val currentUserId = SupabaseClient.getSavedUser(context)?.id
                    ChatHistoryRepository.clearActiveSession(context, currentUserId)
                    SupabaseClient.clearSession(context)

                    setBiometricEnabled(
                        context,
                        false
                    )

                    userName = ""
                    userEmail = ""
                    loginError = ""
                    loginLoading = false
                    biometricError = ""

                    screenStack.clear()
                    screenStack.add("login")
                }
            )
        }

        "chatbot" -> {
            ChatbotScreen(
                initialSessionId = activeChatSessionId,
                initialUserQuery = initialChatQuery,
                onViewTranscript = { targetSessionId ->
                    selectedHistorySessionId = targetSessionId
                    go("chatHistory")
                },
                onBack = {
                    initialChatQuery = null
                    activeChatSessionId = null
                    back()
                }
            )
        }

        "chatHistory" -> {
            ChatHistoryScreen(
                sessionId = selectedHistorySessionId,
                onOpenConversation = { targetSessionId ->
                    activeChatSessionId = targetSessionId
                    selectedHistorySessionId = null
                    if (!screenStack.contains("chatbot")) {
                        go("chatbot")
                    } else {
                        while (screenStack.isNotEmpty() && screenStack.last() != "chatbot") {
                            screenStack.removeAt(screenStack.lastIndex)
                        }
                    }
                },
                onBackClick = {
                    selectedHistorySessionId = null
                    back()
                }
            )
        }

        "bmi" -> {
            BMICheckerScreen(
                onBackClick = {
                    back()
                }
            )
        }

        "symptoms" -> {
            SymptomsCheckerScreen(
                onBackClick = {
                    back()
                }
            )
        }

        "medicine" -> {
            MedicineInfoScreen(
                onBackClick = {
                    back()
                },

                onMedicineClick = { medicine ->
                    selectedMedicine = medicine
                    go("medicineDetail")
                }
            )
        }

        "medicineDetail" -> {

            val medicine = selectedMedicine

            if (medicine == null) {
                back()
            } else {
                MedicineDetailScreen(
                    medicine = medicine,
                    onBackClick = {
                        back()
                    }
                )
            }
        }

        "medicineReminder" -> {
            MedicineReminderScreen(
                initialReminder = reminderPrefill,
                onBackClick = {
                    reminderPrefill = null
                    back()
                }
            )
        }

        "prescription" -> {
            PrescriptionScannerScreen(
                onBackClick = {
                    back()
                }
            )
        }

        "healthTips" -> {
            HealthTipsScreen(
                onBackClick = {
                    back()
                }
            )
        }

        "healthProfile" -> {
            HealthProfileScreen(
                onBack = {
                    back()
                },
                onContinue = {
                    back()
                }
            )
        }

        else -> {
            DashboardScreen(
                userName = userName,
                onProfileClick = { go("profile") },
                onChatbotClick = { initialChatQuery = null; go("chatbot") },
                onSymptomSubmit = { symptomQuery -> initialChatQuery = symptomQuery; go("chatbot") },
                onBMIClick = { go("bmi") },
                onSymptomsClick = { go("symptoms") },
                onMedicineClick = { go("medicine") },
                onHealthTipsClick = { go("healthTips") },
                onPrescriptionClick = { go("prescription") },
                onMedicineReminderClick = { go("medicineReminder") },
                onHealthProfileClick = { go("healthProfile") },
                onLogoutClick = {
                    val currentUserId = SupabaseClient.getSavedUser(context)?.id
                    ChatHistoryRepository.clearActiveSession(context, currentUserId)
                    SupabaseClient.clearSession(context)
                    setBiometricEnabled(context, false)
                    userName = ""
                    userEmail = ""
                    loginError = ""
                    loginLoading = false
                    biometricError = ""
                    screenStack.clear()
                    screenStack.add("login")
                }
            )
        }
    }
}