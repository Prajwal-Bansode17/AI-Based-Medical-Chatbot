package com.example.ai_based_medical_chatbot

import com.example.ai_based_medical_chatbot.ui.theme.DarkMedAssistColors
import com.example.ai_based_medical_chatbot.ui.theme.LightMedAssistColors
import com.example.ai_based_medical_chatbot.ui.theme.ThemeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppThemeTest {

    @Test
    fun testDefaultThemeIsLight() {
        val controller = AppThemeController.createPreviewController(isDark = false)
        assertFalse(controller.isDarkTheme)
    }

    @Test
    fun testToggleThemeSwitchesMode() {
        val controller = AppThemeController.createPreviewController(isDark = false)
        assertFalse(controller.isDarkTheme)

        controller.toggleDarkTheme()
        assertTrue(controller.isDarkTheme)

        controller.toggleDarkTheme()
        assertFalse(controller.isDarkTheme)
    }

    @Test
    fun testSetDarkThemeUpdatesState() {
        val controller = AppThemeController.createPreviewController(isDark = false)
        controller.setDarkTheme(true)
        assertTrue(controller.isDarkTheme)

        controller.setDarkTheme(false)
        assertFalse(controller.isDarkTheme)
    }

    @Test
    fun testThemeStatePaletteSwitching() {
        // Light mode
        ThemeState.current = LightMedAssistColors
        assertFalse(ThemeState.current.isDark)
        assertEquals(LightMedAssistColors.ivory, ThemeState.current.ivory)
        assertEquals(LightMedAssistColors.textDark, ThemeState.current.textDark)

        // Dark mode
        ThemeState.current = DarkMedAssistColors
        assertTrue(ThemeState.current.isDark)
        assertEquals(DarkMedAssistColors.ivory, ThemeState.current.ivory)
        assertEquals(DarkMedAssistColors.textDark, ThemeState.current.textDark)

        // Ensure background colors differ significantly between modes
        assertNotEquals(LightMedAssistColors.ivory, DarkMedAssistColors.ivory)
        assertNotEquals(LightMedAssistColors.pureWhite, DarkMedAssistColors.pureWhite)
        assertNotEquals(LightMedAssistColors.textDark, DarkMedAssistColors.textDark)

        // Clean up: reset to light
        ThemeState.current = LightMedAssistColors
    }

    @Test
    fun testDarkModeStringsInSupportedLanguages() {
        assertEquals("Dark Mode", appText("dark_mode", LANGUAGE_ENGLISH))
        assertEquals("डार्क मोड", appText("dark_mode", LANGUAGE_HINDI))
        assertEquals("डार्क मोड", appText("dark_mode", LANGUAGE_MARATHI))

        assertEquals("Enabled", appText("dark_mode_enabled", LANGUAGE_ENGLISH))
        assertEquals("सक्रिय", appText("dark_mode_enabled", LANGUAGE_HINDI))
        assertEquals("सुरू", appText("dark_mode_enabled", LANGUAGE_MARATHI))

        assertEquals("Disabled", appText("dark_mode_disabled", LANGUAGE_ENGLISH))
        assertEquals("निष्क्रिय", appText("dark_mode_disabled", LANGUAGE_HINDI))
        assertEquals("बंद", appText("dark_mode_disabled", LANGUAGE_MARATHI))
    }
}
