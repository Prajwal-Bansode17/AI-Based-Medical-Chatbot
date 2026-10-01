package com.example.ai_based_medical_chatbot

/**
 * Single canonical UI translation table for MEDASSIST AI.
 *
 * Supported languages (canonical names, as stored by AppLanguageController):
 *   "English", "मराठी", "हिंदी"
 *
 * appText supports both existing call styles:
 *   appText(language, key)
 *   appText(key, language)
 *
 * Lookup order: selected language -> English -> key (last resort only, when the key
 * does not exist in any table).
 */

private const val LANGUAGE_ENGLISH = "English"
private const val LANGUAGE_MARATHI = "मराठी"
private const val LANGUAGE_HINDI = "हिंदी"

/** Accepted spellings (lower-cased) mapped to the canonical language names above. */
private val languageAliases: Map<String, String> = mapOf(
    "english" to LANGUAGE_ENGLISH,
    "en" to LANGUAGE_ENGLISH,
    "मराठी" to LANGUAGE_MARATHI,
    "marathi" to LANGUAGE_MARATHI,
    "mr" to LANGUAGE_MARATHI,
    "हिंदी" to LANGUAGE_HINDI,
    "hindi" to LANGUAGE_HINDI,
    "hi" to LANGUAGE_HINDI
)

/** Returns the canonical language name for [value], or null if it is not a language. */
private fun resolveLanguage(value: String): String? =
    languageAliases[value.trim().lowercase()]

/**
 * Converts any accepted language spelling (or null/unknown) into one of the three
 * canonical names: "English", "मराठी", "हिंदी". Unknown values fall back to "English".
 */
fun normalizeAppLanguage(language: String?): String =
    language?.let { resolveLanguage(it) } ?: LANGUAGE_ENGLISH

fun appText(first: String, second: String): String {
    val firstLanguage = resolveLanguage(first)

    val language: String
    val key: String

    if (firstLanguage != null) {
        // Style 1: appText(language, key)
        language = firstLanguage
        key = second
    } else {
        // Style 2: appText(key, language)
        key = first
        language = resolveLanguage(second) ?: LANGUAGE_ENGLISH
    }

    val translated: String? = when (language) {
        LANGUAGE_MARATHI -> marathiText(key)
        LANGUAGE_HINDI -> hindiText(key)
        else -> englishText(key)
    }

    return translated?.takeIf { it.isNotBlank() }
        ?: englishText(key)
        ?: key
}

private fun marathiText(key: String): String? = when (key) {
    "account" -> "खाते"
    "age" -> "वय"
    "app_language" -> "अॅपची भाषा"
    "app_name" -> "MEDASSIST AI"
    "back" -> "मागे"
    "bmi_information" -> "BMI माहिती"
    "check_bmi" -> "BMI तपासा"
    "check_bmi_sub" -> "तुमचे शरीराचे वजन तपासा"
    "check_symptoms" -> "लक्षणे तपासा"
    "check_symptoms_sub" -> "लक्षणे तपासा"
    "choose_need" -> "तुम्हाला काय हवे ते निवडा"
    "current_bmi" -> "सध्याचा BMI"
    "daily" -> "दररोज"
    "dashboard_ai_companion" -> "तुमचा बुद्धिमान आरोग्य साथीदार"
    "dashboard_ask_health" -> "तुमच्या आरोग्याबद्दल काहीही विचारा..."
    "dashboard_good_day" -> "तुमचा दिवस शुभ जावो 👋"
    "dashboard_greeting" -> "तुमचा दिवस शुभ जावो 👋"
    "dashboard_hello" -> "नमस्कार"
    "dashboard_help_today" -> "आज मी तुमची कशी मदत करू शकतो?"
    "dashboard_personal_ai" -> "तुमचा वैयक्तिक आरोग्य AI"
    "dob" -> "जन्मतारीख"
    "email" -> "ईमेल"
    "find_medicine" -> "औषध शोधा"
    "footer_companion" -> "MEDASSIST AI  •  तुमचा बुद्धिमान आरोग्य साथीदार"
    "footer_decisions" -> "चांगल्या आरोग्य निर्णयांसाठी तयार केलेले"
    "gender" -> "लिंग"
    "health_disclaimer" -> "MEDASSIST AI सामान्य आरोग्य माहिती देते; हे व्यावसायिक वैद्यकीय सल्ल्याचा पर्याय नाही."
    "health_information" -> "आरोग्य माहिती"
    "health_tips" -> "आरोग्य टिप्स"
    "healthy_lifestyle" -> "निरोगी जीवनशैली"
    "height" -> "उंची"
    "language" -> "भाषा"
    "login_24_7_care" -> "24/7 सेवा"
    "login_ai_powered" -> "AI समर्थित"
    "login_already_account" -> "आधीपासून खाते आहे?"
    "login_biometric_privacy" -> "तुमचा बायोमेट्रिक डेटा तुमच्या डिव्हाइसवरच राहतो."
    "login_create_account" -> "नवीन खाते तयार करा"
    "login_email_label" -> "ईमेल पत्ता"
    "login_email_placeholder" -> "तुमचा ईमेल टाका"
    "login_footer" -> "Supabase द्वारे सुरक्षित प्रमाणीकरण"
    "login_forgot_password" -> "पासवर्ड विसरलात?"
    "login_hero_companion" -> "तुमचा बुद्धिमान\nआरोग्यसेवा साथीदार."
    "login_hide" -> "लपवा"
    "login_or" -> "किंवा"
    "login_password_label" -> "पासवर्ड"
    "login_password_placeholder" -> "तुमचा पासवर्ड टाका"
    "login_register" -> "नोंदणी करा"
    "login_secure_login" -> "सुरक्षित लॉगिन"
    "login_secure_methods" -> "फिंगरप्रिंट, फेस अनलॉक,\nकिंवा तुमचा डिव्हाइस PIN / पॅटर्न वापरा."
    "login_secure_welcome" -> "MEDASSIST AI मध्ये पुन्हा स्वागत आहे"
    "login_show" -> "दाखवा"
    "login_sign_in" -> "साइन इन"
    "login_sign_in_subtitle" -> "तुमच्या आरोग्य डॅशबोर्डवर जाण्यासाठी साइन इन करा."
    "login_unlock_securely" -> "सुरक्षितपणे अनलॉक करा"
    "login_use_password" -> "ईमेल आणि पासवर्ड वापरा"
    "login_welcome_back" -> "पुन्हा स्वागत आहे"
    "login_welcome_back_short" -> "परत स्वागत आहे."
    "medicine" -> "औषध"
    "medicine_info" -> "औषधाची माहिती"
    "medicine_reminder" -> "औषधाची आठवण"
    "my_health" -> "माझी आरोग्य माहिती"
    "my_prescriptions" -> "माझी प्रिस्क्रिप्शन्स"
    "my_profile" -> "माझे प्रोफाइल"
    "my_reminders" -> "माझ्या औषधांच्या आठवणी"
    "never_miss" -> "तुमचा औषधाचा डोस चुकवू नका"
    "no_prescriptions" -> "जतन केलेली प्रिस्क्रिप्शन नाहीत"
    "no_reminders" -> "औषधांच्या आठवणी नाहीत"
    "password" -> "पासवर्ड"
    "password_protected" -> "पासवर्ड सुरक्षित आहे"
    "profile" -> "प्रोफाइल"
    "quick_actions" -> "जलद कृती"
    "reminders_here" -> "तुमच्या सक्रिय औषधांच्या आठवणी येथे दिसतील."
    "saved_body" -> "तुमची जतन केलेली शरीर मोजमापे"
    "scan_prescription" -> "प्रिस्क्रिप्शन स्कॅन करा"
    "secure_account" -> "सुरक्षित खाते"
    "sign_out" -> "साइन आउट"
    "status" -> "स्थिती"
    "take_photo" -> "तुमच्या प्रिस्क्रिप्शनचा फोटो घ्या"
    "time_not_set" -> "वेळ सेट केलेली नाही"
    "weight" -> "वजन"
    "your_account" -> "तुमचे MEDASSIST खाते"
    else -> null
}

private fun hindiText(key: String): String? = when (key) {
    "account" -> "खाता"
    "age" -> "उम्र"
    "app_language" -> "ऐप की भाषा"
    "app_name" -> "MEDASSIST AI"
    "back" -> "वापस"
    "bmi_information" -> "BMI जानकारी"
    "check_bmi" -> "BMI जांचें"
    "check_bmi_sub" -> "अपना शरीर का वजन जांचें"
    "check_symptoms" -> "लक्षण जांचें"
    "check_symptoms_sub" -> "लक्षणों की जांच करें"
    "choose_need" -> "अपनी जरूरत चुनें"
    "current_bmi" -> "वर्तमान BMI"
    "daily" -> "रोज़ाना"
    "dashboard_ai_companion" -> "आपका बुद्धिमान स्वास्थ्य साथी"
    "dashboard_ask_health" -> "अपने स्वास्थ्य के बारे में कुछ भी पूछें..."
    "dashboard_good_day" -> "आपका दिन शुभ हो 👋"
    "dashboard_greeting" -> "आपका दिन शुभ हो 👋"
    "dashboard_hello" -> "नमस्ते"
    "dashboard_help_today" -> "आज मैं आपकी कैसे मदद कर सकता हूँ?"
    "dashboard_personal_ai" -> "आपका व्यक्तिगत स्वास्थ्य AI"
    "dob" -> "जन्म तिथि"
    "email" -> "ईमेल"
    "find_medicine" -> "दवा खोजें"
    "footer_companion" -> "MEDASSIST AI  •  आपका बुद्धिमान स्वास्थ्य साथी"
    "footer_decisions" -> "बेहतर स्वास्थ्य निर्णयों के लिए बनाया गया"
    "gender" -> "लिंग"
    "health_disclaimer" -> "MEDASSIST AI सामान्य स्वास्थ्य जानकारी देता है और यह पेशेवर चिकित्सा सलाह का विकल्प नहीं है।"
    "health_information" -> "स्वास्थ्य जानकारी"
    "health_tips" -> "स्वास्थ्य सुझाव"
    "healthy_lifestyle" -> "स्वस्थ जीवनशैली"
    "height" -> "ऊंचाई"
    "language" -> "भाषा"
    "login_24_7_care" -> "24/7 सेवा"
    "login_ai_powered" -> "AI संचालित"
    "login_already_account" -> "क्या आपके पास पहले से खाता है?"
    "login_biometric_privacy" -> "आपका बायोमेट्रिक डेटा आपके डिवाइस पर ही रहता है।"
    "login_create_account" -> "नया खाता बनाएँ"
    "login_email_label" -> "ईमेल पता"
    "login_email_placeholder" -> "अपना ईमेल दर्ज करें"
    "login_footer" -> "Supabase द्वारा सुरक्षित प्रमाणीकरण"
    "login_forgot_password" -> "पासवर्ड भूल गए?"
    "login_hero_companion" -> "आपका बुद्धिमान\nस्वास्थ्य सेवा साथी।"
    "login_hide" -> "छिपाएँ"
    "login_or" -> "या"
    "login_password_label" -> "पासवर्ड"
    "login_password_placeholder" -> "अपना पासवर्ड दर्ज करें"
    "login_register" -> "रजिस्टर करें"
    "login_secure_login" -> "सुरक्षित लॉगिन"
    "login_secure_methods" -> "फिंगरप्रिंट, फेस अनलॉक,\nया अपने डिवाइस का PIN / पैटर्न इस्तेमाल करें।"
    "login_secure_welcome" -> "MEDASSIST AI में आपका स्वागत है"
    "login_show" -> "दिखाएँ"
    "login_sign_in" -> "साइन इन"
    "login_sign_in_subtitle" -> "अपने हेल्थ डैशबोर्ड पर जाने के लिए साइन इन करें।"
    "login_unlock_securely" -> "सुरक्षित रूप से अनलॉक करें"
    "login_use_password" -> "ईमेल और पासवर्ड इस्तेमाल करें"
    "login_welcome_back" -> "वापस स्वागत है"
    "login_welcome_back_short" -> "वापस स्वागत है।"
    "medicine" -> "दवा"
    "medicine_info" -> "दवा की जानकारी"
    "medicine_reminder" -> "दवा की याद"
    "my_health" -> "मेरी स्वास्थ्य जानकारी"
    "my_prescriptions" -> "मेरे प्रिस्क्रिप्शन"
    "my_profile" -> "मेरा प्रोफाइल"
    "my_reminders" -> "मेरी दवा की याद दिलाने वाली सूचनाएं"
    "never_miss" -> "अपनी दवा की खुराक न भूलें"
    "no_prescriptions" -> "कोई सहेजा हुआ प्रिस्क्रिप्शन नहीं"
    "no_reminders" -> "दवा की कोई याद नहीं"
    "password" -> "पासवर्ड"
    "password_protected" -> "पासवर्ड सुरक्षित है"
    "profile" -> "प्रोफाइल"
    "quick_actions" -> "त्वरित विकल्प"
    "reminders_here" -> "आपकी सक्रिय दवा की याद यहां दिखाई देगी।"
    "saved_body" -> "आपके सहेजे गए शरीर के माप"
    "scan_prescription" -> "प्रिस्क्रिप्शन स्कैन करें"
    "secure_account" -> "सुरक्षित खाता"
    "sign_out" -> "साइन आउट"
    "status" -> "स्थिति"
    "take_photo" -> "अपने प्रिस्क्रिप्शन की फोटो लें"
    "time_not_set" -> "समय सेट नहीं है"
    "weight" -> "वजन"
    "your_account" -> "आपका MEDASSIST खाता"
    else -> null
}

private fun englishText(key: String): String? = when (key) {
    "account" -> "Account"
    "age" -> "Age"
    "app_language" -> "App Language"
    "app_name" -> "MEDASSIST AI"
    "back" -> "Back"
    "bmi_information" -> "BMI Information"
    "check_bmi" -> "Check BMI"
    "check_bmi_sub" -> "Check your body weight"
    "check_symptoms" -> "Check Symptoms"
    "check_symptoms_sub" -> "Check symptoms"
    "choose_need" -> "Choose what you need"
    "current_bmi" -> "Current BMI"
    "daily" -> "Daily"
    "dashboard_ai_companion" -> "Your intelligent health companion"
    "dashboard_ask_health" -> "Ask anything about your health..."
    "dashboard_good_day" -> "Good day 👋"
    "dashboard_greeting" -> "Good day 👋"
    "dashboard_hello" -> "Hello"
    "dashboard_help_today" -> "How can I help you today?"
    "dashboard_personal_ai" -> "Your personal health AI"
    "dob" -> "Date of Birth"
    "email" -> "Email"
    "find_medicine" -> "Find Medicine"
    "footer_companion" -> "MEDASSIST AI  •  Your intelligent health companion"
    "footer_decisions" -> "Made for better health decisions"
    "gender" -> "Gender"
    "health_disclaimer" -> "MEDASSIST AI provides general health information and is not a replacement for professional medical advice."
    "health_information" -> "Health information"
    "health_tips" -> "Health Tips"
    "healthy_lifestyle" -> "Healthy lifestyle"
    "height" -> "Height"
    "language" -> "Language"
    "login_24_7_care" -> "24/7 CARE"
    "login_ai_powered" -> "AI POWERED"
    "login_already_account" -> "Already have an account?"
    "login_biometric_privacy" -> "Your biometric data stays on your device."
    "login_create_account" -> "Create New Account"
    "login_email_label" -> "Email address"
    "login_email_placeholder" -> "Enter your email"
    "login_footer" -> "Secure authentication powered by Supabase"
    "login_forgot_password" -> "Forgot Password?"
    "login_hero_companion" -> "Your intelligent\nhealthcare companion."
    "login_hide" -> "HIDE"
    "login_or" -> "OR"
    "login_password_label" -> "Password"
    "login_password_placeholder" -> "Enter your password"
    "login_register" -> "Register"
    "login_secure_login" -> "Secure Login"
    "login_secure_methods" -> "Use fingerprint, face unlock,\nor your device PIN / pattern."
    "login_secure_welcome" -> "Welcome back to MEDASSIST AI"
    "login_show" -> "SHOW"
    "login_sign_in" -> "Sign In"
    "login_sign_in_subtitle" -> "Sign in to continue to your health dashboard."
    "login_unlock_securely" -> "Unlock Securely"
    "login_use_password" -> "Use Email & Password"
    "login_welcome_back" -> "Welcome Back"
    "login_welcome_back_short" -> "Welcome back."
    "medicine" -> "Medicine"
    "medicine_info" -> "Medicine info"
    "medicine_reminder" -> "Medicine Reminder"
    "my_health" -> "My Health Information"
    "my_prescriptions" -> "My Prescriptions"
    "my_profile" -> "My Profile"
    "my_reminders" -> "My Medicine Reminders"
    "never_miss" -> "Never miss your medicine dose"
    "no_prescriptions" -> "No saved prescriptions"
    "no_reminders" -> "No medicine reminders"
    "password" -> "Password"
    "password_protected" -> "Password protected"
    "profile" -> "Profile"
    "quick_actions" -> "Quick Actions"
    "reminders_here" -> "Your active medicine reminders will appear here."
    "saved_body" -> "Your saved body measurements"
    "scan_prescription" -> "Scan Prescription"
    "secure_account" -> "Secure account"
    "sign_out" -> "Sign Out"
    "status" -> "Status"
    "take_photo" -> "Take a photo of your prescription"
    "time_not_set" -> "Time not set"
    "weight" -> "Weight"
    "your_account" -> "Your MEDASSIST account"
    else -> null
}