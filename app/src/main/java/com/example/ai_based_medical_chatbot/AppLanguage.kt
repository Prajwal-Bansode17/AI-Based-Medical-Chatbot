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

/**
 * Single app-wide language controller.
 *
 * Supported languages:
 * - English
 * - मराठी
 * - हिंदी
 *
 * The selected language is persisted locally and exposed through
 * LocalAppLanguageController so every Compose screen can react immediately.
 */
class AppLanguageController {

    private val preferences: SharedPreferences?
    private val languageState: MutableState<String>

    constructor(context: Context) {
        val app = context.applicationContext ?: context
        preferences = app.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val initial = normalizeAppLanguage(
            preferences.getString(KEY_SELECTED_LANGUAGE, null)
        )
        languageState = synchronized(stateLock) {
            val state = sharedLanguageState
            if (state == null) {
                mutableStateOf(initial).also { sharedLanguageState = it }
            } else {
                state.value = initial
                state
            }
        }
    }

    internal constructor(preferences: SharedPreferences?, initialLanguage: String) {
        this.preferences = preferences
        this.languageState = mutableStateOf(normalizeAppLanguage(initialLanguage))
    }

    val selectedLanguage: String
        get() = languageState.value

    fun setLanguage(language: String) {
        val normalized = normalizeAppLanguage(language)
        languageState.value = normalized
        preferences?.edit()
            ?.putString(KEY_SELECTED_LANGUAGE, normalized)
            ?.commit()
    }

    companion object {
        const val PREFERENCES_NAME = "medassist_language_preferences"
        const val KEY_SELECTED_LANGUAGE = "selected_language"

        private val stateLock = Any()
        private var sharedLanguageState: MutableState<String>? = null

        fun createPreviewController(language: String = LANGUAGE_ENGLISH): AppLanguageController {
            return AppLanguageController(null, language)
        }
    }
}

val LocalAppLanguageController =
    compositionLocalOf<AppLanguageController> {
        AppLanguageController.createPreviewController()
    }

@Composable
fun ProvideAppLanguage(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val controller = remember(context) {
        AppLanguageController(context)
    }
    // Reading selectedLanguage forces ProvideAppLanguage to recompose
    // whenever language changes, propagating across the entire tree.
    val activeLanguage = controller.selectedLanguage

    CompositionLocalProvider(
        LocalAppLanguageController provides controller
    ) {
        content()
    }
}
