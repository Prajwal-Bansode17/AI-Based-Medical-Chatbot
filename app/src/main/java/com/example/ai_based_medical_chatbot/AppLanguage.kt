package com.example.ai_based_medical_chatbot

import android.content.Context
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
class AppLanguageController(context: Context) {

    private val preferences =
        context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

    private val languageState: MutableState<String> =
        synchronized(stateLock) {
            sharedLanguageState ?: mutableStateOf(
                normalizeAppLanguage(
                    preferences.getString(KEY_SELECTED_LANGUAGE, null)
                )
            ).also { sharedLanguageState = it }
        }

    val selectedLanguage: String
        get() = languageState.value

    fun setLanguage(language: String) {
        val normalized = normalizeAppLanguage(language)
        languageState.value = normalized
        preferences.edit()
            .putString(KEY_SELECTED_LANGUAGE, normalized)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "medassist_language_preferences"
        const val KEY_SELECTED_LANGUAGE = "selected_language"

        private val stateLock = Any()
        private var sharedLanguageState: MutableState<String>? = null
    }
}

val LocalAppLanguageController =
    compositionLocalOf<AppLanguageController> {
        error("AppLanguageController is not provided.")
    }

@Composable
fun ProvideAppLanguage(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val controller = remember(context) {
        AppLanguageController(context)
    }

    CompositionLocalProvider(
        LocalAppLanguageController provides controller
    ) {
        content()
    }
}
