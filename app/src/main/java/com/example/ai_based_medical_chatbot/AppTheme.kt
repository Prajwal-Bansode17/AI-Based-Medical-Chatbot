package com.example.ai_based_medical_chatbot

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.ai_based_medical_chatbot.ui.theme.AIBasedMedicalChatbotTheme

/**
 * App-wide theme controller for MedAssist AI.
 *
 * Persists the user's selected theme (Light vs Dark Mode) in SharedPreferences
 * and provides reactive state so the entire UI updates instantly upon toggle
 * without needing an app restart.
 */
class AppThemeController {

    private val preferences: SharedPreferences?
    private val darkThemeState: MutableState<Boolean>

    constructor(context: Context) {
        val app = context.applicationContext ?: context
        preferences = app.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val initial = preferences.getBoolean(KEY_DARK_THEME, false)
        darkThemeState = synchronized(stateLock) {
            val state = sharedDarkThemeState
            if (state == null) {
                mutableStateOf(initial).also { sharedDarkThemeState = it }
            } else {
                state.value = initial
                state
            }
        }
    }

    internal constructor(preferences: SharedPreferences?, initialDarkTheme: Boolean) {
        this.preferences = preferences
        this.darkThemeState = mutableStateOf(initialDarkTheme)
    }

    val isDarkTheme: Boolean
        get() = darkThemeState.value

    fun setDarkTheme(enabled: Boolean) {
        darkThemeState.value = enabled
        preferences?.edit()
            ?.putBoolean(KEY_DARK_THEME, enabled)
            ?.apply()
    }

    fun toggleDarkTheme() {
        setDarkTheme(!isDarkTheme)
    }

    companion object {
        const val PREFERENCES_NAME = "medassist_theme_preferences"
        const val KEY_DARK_THEME = "is_dark_theme"

        private val stateLock = Any()
        private var sharedDarkThemeState: MutableState<Boolean>? = null

        fun createPreviewController(isDark: Boolean = false): AppThemeController {
            return AppThemeController(null, isDark)
        }
    }
}

val LocalAppThemeController =
    compositionLocalOf<AppThemeController> {
        AppThemeController.createPreviewController()
    }

@Composable
fun ProvideAppTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val controller = remember(context) {
        AppThemeController(context)
    }

    // Reading isDarkTheme forces ProvideAppTheme to recompose immediately
    // whenever theme is toggled, propagating down the entire tree.
    val isDark = controller.isDarkTheme

    CompositionLocalProvider(
        LocalAppThemeController provides controller
    ) {
        AIBasedMedicalChatbotTheme(
            darkTheme = isDark
        ) {
            content()
        }
    }
}
