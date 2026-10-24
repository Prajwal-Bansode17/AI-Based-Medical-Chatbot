package com.example.ai_based_medical_chatbot

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeSharedPreferences : SharedPreferences {
    val map = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = map
    override fun getString(key: String?, defValue: String?): String? = (map[key] as? String) ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
    override fun getInt(key: String?, defValue: Int): Int = (map[key] as? Int) ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = (map[key] as? Long) ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = (map[key] as? Float) ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
    override fun contains(key: String?): Boolean = map.containsKey(key)
    override fun edit(): SharedPreferences.Editor = FakeEditor(this)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    class FakeEditor(private val prefs: FakeSharedPreferences) : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = this
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = this
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = this
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = this
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = this
        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) temp.remove(key)
            return this
        }
        override fun clear(): SharedPreferences.Editor {
            temp.clear()
            return this
        }
        override fun commit(): Boolean {
            prefs.map.putAll(temp)
            return true
        }
        override fun apply() {
            prefs.map.putAll(temp)
        }
    }
}

class AppLanguageTest {

    @Test
    fun testNormalizeAppLanguage_supportedLanguages() {
        assertEquals(LANGUAGE_ENGLISH, normalizeAppLanguage("English"))
        assertEquals(LANGUAGE_ENGLISH, normalizeAppLanguage("en"))
        assertEquals(LANGUAGE_ENGLISH, normalizeAppLanguage("english"))

        assertEquals(LANGUAGE_MARATHI, normalizeAppLanguage("मराठी"))
        assertEquals(LANGUAGE_MARATHI, normalizeAppLanguage("marathi"))
        assertEquals(LANGUAGE_MARATHI, normalizeAppLanguage("mr"))

        assertEquals(LANGUAGE_HINDI, normalizeAppLanguage("हिंदी"))
        assertEquals(LANGUAGE_HINDI, normalizeAppLanguage("हिन्दी"))
        assertEquals(LANGUAGE_HINDI, normalizeAppLanguage("hindi"))
        assertEquals(LANGUAGE_HINDI, normalizeAppLanguage("hi"))
    }

    @Test
    fun testNormalizeAppLanguage_fallbackOnUnknownOrNull() {
        assertEquals(LANGUAGE_ENGLISH, normalizeAppLanguage(null))
        assertEquals(LANGUAGE_ENGLISH, normalizeAppLanguage(""))
        assertEquals(LANGUAGE_ENGLISH, normalizeAppLanguage("french"))
        assertEquals(LANGUAGE_ENGLISH, normalizeAppLanguage("12345"))
    }

    @Test
    fun testAppLanguageController_initializationAndPersistence() {
        val fakePrefs = FakeSharedPreferences()
        fakePrefs.map[AppLanguageController.KEY_SELECTED_LANGUAGE] = "मराठी"

        val controller = AppLanguageController(fakePrefs, "मराठी")
        assertEquals(LANGUAGE_MARATHI, controller.selectedLanguage)

        controller.setLanguage("हिंदी")
        assertEquals(LANGUAGE_HINDI, controller.selectedLanguage)
        assertEquals(LANGUAGE_HINDI, fakePrefs.getString(AppLanguageController.KEY_SELECTED_LANGUAGE, null))

        controller.setLanguage("English")
        assertEquals(LANGUAGE_ENGLISH, controller.selectedLanguage)
        assertEquals(LANGUAGE_ENGLISH, fakePrefs.getString(AppLanguageController.KEY_SELECTED_LANGUAGE, null))
    }

    @Test
    fun testAppText_multilingualTranslations() {
        // App Language label
        assertEquals("App Language", appText("app_language", "English"))
        assertEquals("अॅपची भाषा", appText("app_language", "मराठी"))
        assertEquals("ऐप की भाषा", appText("app_language", "हिंदी"))

        // Profile labels
        assertEquals("Profile", appText("profile", "English"))
        assertEquals("प्रोफाइल", appText("profile", "मराठी"))
        assertEquals("प्रोफाइल", appText("profile", "हिंदी"))

        // Password labels
        assertEquals("Change Password", appText("change_password", "English"))
        assertEquals("पासवर्ड बदला", appText("change_password", "मराठी"))
        assertEquals("पासवर्ड बदलें", appText("change_password", "हिंदी"))

        // Chat History labels
        assertEquals("Chat History", appText("chat_history_title", "English"))
        assertEquals("संभाषण इतिहास", appText("chat_history_title", "मराठी"))
        assertEquals("बातचीत का इतिहास", appText("chat_history_title", "हिंदी"))
    }

    @Test
    fun testAppText_phase1DialogAndTimelineKeys() {
        // Chatbot Rename & Clear dialogs
        assertEquals("Rename Chat", appText("chat_rename_dialog_title", "English"))
        assertEquals("संभाषणाचे नाव बदला", appText("chat_rename_dialog_title", "मराठी"))
        assertEquals("बातचीत का नाम बदलें", appText("chat_rename_dialog_title", "हिंदी"))

        assertEquals("Clear conversation?", appText("chat_clear_dialog_title", "English"))
        assertEquals("संभाषण साफ करायचे का?", appText("chat_clear_dialog_title", "मराठी"))
        assertEquals("बातचीत साफ़ करें?", appText("chat_clear_dialog_title", "हिंदी"))

        // Timeline relative strings
        assertEquals("Just now", appText("time_just_now", "English"))
        assertEquals("आत्ताच", appText("time_just_now", "मराठी"))
        assertEquals("अभी-अभी", appText("time_just_now", "हिंदी"))

        assertEquals("Previous 7 Days", appText("time_group_7_days", "English"))
        assertEquals("मागील 7 दिवस", appText("time_group_7_days", "मराठी"))
        assertEquals("पिछले 7 दिन", appText("time_group_7_days", "हिंदी"))

        // Medicine Clear Search
        assertEquals("Clear Search", appText("medicine_clear_search", "English"))
        assertEquals("शोध साफ करा", appText("medicine_clear_search", "मराठी"))
        assertEquals("खोज साफ़ करें", appText("medicine_clear_search", "हिंदी"))
    }

    @Test
    fun testChatRelativeTimeAndGroupLabel_multilingualOutput() {
        val now = 1000000000L
        val thirtyMinsAgo = now - 30 * 60 * 1000L
        val threeDaysAgo = now - 3 * 24 * 60 * 60 * 1000L

        // Relative time formatting
        assertEquals("30 min ago", ui.chatRelativeTime(thirtyMinsAgo, now = now, language = "English"))
        assertEquals("30 मिनिटांपूर्वी", ui.chatRelativeTime(thirtyMinsAgo, now = now, language = "मराठी"))
        assertEquals("30 मिनट पहले", ui.chatRelativeTime(thirtyMinsAgo, now = now, language = "हिंदी"))

        // Group label formatting
        assertEquals("Previous 7 Days", ui.chatGroupLabel(threeDaysAgo, now = now, language = "English"))
        assertEquals("मागील 7 दिवस", ui.chatGroupLabel(threeDaysAgo, now = now, language = "मराठी"))
        assertEquals("पिछले 7 दिन", ui.chatGroupLabel(threeDaysAgo, now = now, language = "हिंदी"))
    }

    @Test
    fun testAppText_fallbackBehaviorForMissingKey() {
        // Missing key should fallback to English or snake_case titlecase
        val result = appText("non_existent_key_sample", "मराठी")
        assertEquals("Non Existent Key Sample", result)

        val emptyResult = appText("", "English")
        assertEquals("", emptyResult)
    }

    @Test
    fun testAppText_bothParameterOrdersSupported() {
        // (key, language)
        val style1 = appText("account", "मराठी")
        // (language, key)
        val style2 = appText("मराठी", "account")

        assertEquals(style1, style2)
        assertEquals("खाते", style1)
    }

    @Test
    fun testPreservesMedicalTermsWithoutIncorrectTranslations() {
        // Medicine specifications and badges
        assertEquals("Prescription (Rx)", appText("medicine_rx_badge", "English"))
        assertEquals("प्रिस्क्रिप्शन आवश्यक (Rx)", appText("medicine_rx_badge", "मराठी"))
        assertEquals("पर्चे की दवा (Rx)", appText("medicine_rx_badge", "हिंदी"))

        assertEquals("Over-the-Counter (OTC)", appText("medicine_otc_badge", "English"))
        assertEquals("काउंटरवर उपलब्ध (OTC)", appText("medicine_otc_badge", "मराठी"))
        assertEquals("बिना पर्चे उपलब्ध (OTC)", appText("medicine_otc_badge", "हिंदी"))
    }
    @Test
    fun testAppText_phase2RemainingKeys() {
        // Dashboard
        assertEquals("Hello, %s", appText("greeting_hello", "English"))
        assertEquals("नमस्कार, %s", appText("greeting_hello", "मराठी"))
        assertEquals("नमस्ते, %s", appText("greeting_hello", "हिंदी"))

        assertEquals("Describe your symptoms for instant AI medical triage", appText("dashboard_bothering_sub", "English"))
        assertEquals("त्वरित AI वैद्यकीय तपासणीसाठी तुमची लक्षणे सांगा", appText("dashboard_bothering_sub", "मराठी"))
        assertEquals("तुरंत AI मेडिकल ट्राइएज के लिए अपने लक्षण बताएं", appText("dashboard_bothering_sub", "हिंदी"))

        assertEquals("Verified Medical AI", appText("dashboard_verified_ai", "English"))
        assertEquals("सत्यापित वैद्यकीय AI", appText("dashboard_verified_ai", "मराठी"))
        assertEquals("सत्यापित मेडिकल AI", appText("dashboard_verified_ai", "हिंदी"))

        assertEquals("Prescriptions", appText("dashboard_prescriptions_title", "English"))
        assertEquals("प्रिस्क्रिप्शन", appText("dashboard_prescriptions_title", "मराठी"))
        assertEquals("प्रिस्क्रिप्शन", appText("dashboard_prescriptions_title", "हिंदी"))

        assertEquals("%d Saved", appText("dashboard_prescriptions_saved", "English"))
        assertEquals("%d सेव्ह केले", appText("dashboard_prescriptions_saved", "मराठी"))
        assertEquals("%d सेव किए", appText("dashboard_prescriptions_saved", "हिंदी"))

        assertEquals("Optical records", appText("dashboard_optical_records", "English"))
        assertEquals("ऑप्टिकल नोंदी", appText("dashboard_optical_records", "मराठी"))
        assertEquals("ऑप्टिकल रिकॉर्ड्स", appText("dashboard_optical_records", "हिंदी"))

        // Health Profile
        assertEquals("Select Date of Birth", appText("health_profile_select_dob", "English"))
        assertEquals("जन्मतारीख निवडा", appText("health_profile_select_dob", "मराठी"))
        assertEquals("जन्म तिथि चुनें", appText("health_profile_select_dob", "हिंदी"))

        assertEquals("Date of birth updated.", appText("health_profile_dob_updated", "English"))
        assertEquals("जन्मतारीख अपडेट केली.", appText("health_profile_dob_updated", "मराठी"))
        assertEquals("जन्म तिथि अपडेट की गई.", appText("health_profile_dob_updated", "हिंदी"))

        assertEquals("Health profile updated successfully.", appText("health_profile_updated", "English"))
        assertEquals("आरोग्य प्रोफाइल यशस्वीपणे अपडेट केले.", appText("health_profile_updated", "मराठी"))
        assertEquals("हेल्थ प्रोफाइल सफलतापूर्वक अपडेट किया गया.", appText("health_profile_updated", "हिंदी"))

        assertEquals("My Health Profile", appText("my_health_profile", "English"))
        assertEquals("माझे आरोग्य प्रोफाइल", appText("my_health_profile", "मराठी"))
        assertEquals("मेरी हेल्थ प्रोफाइल", appText("my_health_profile", "हिंदी"))

        assertEquals("Full Name", appText("full_name", "English"))
        assertEquals("पूर्ण नाव", appText("full_name", "मराठी"))
        assertEquals("पूरा नाम", appText("full_name", "हिंदी"))

        assertEquals("Blood Group", appText("blood_group", "English"))
        assertEquals("रक्तगट", appText("blood_group", "मराठी"))
        assertEquals("रक्त समूह", appText("blood_group", "हिंदी"))

        assertEquals("Calculated BMI", appText("bmi_calculated_label", "English"))
        assertEquals("गणना केलेला BMI", appText("bmi_calculated_label", "मराठी"))
        assertEquals("गणना किया गया BMI", appText("bmi_calculated_label", "हिंदी"))

        // Prescription & Reminder
        assertEquals("Camera permission is required to scan prescriptions.", appText("camera_permission_required", "English"))
        assertEquals("प्रिस्क्रिप्शन स्कॅन करण्यासाठी कॅमेरा परवानगी आवश्यक आहे.", appText("camera_permission_required", "मराठी"))
        assertEquals("प्रिस्क्रिप्शन स्कैन करने के लिए कैमरा अनुमति आवश्यक है.", appText("camera_permission_required", "हिंदी"))

        assertEquals("Reminder deleted successfully.", appText("reminder_deleted_success", "English"))
        assertEquals("आठवण यशस्वीपणे हटवली.", appText("reminder_deleted_success", "मराठी"))
        assertEquals("रिमाइंडर सफलतापूर्वक हटा दिया गया.", appText("reminder_deleted_success", "हिंदी"))
    }
}
