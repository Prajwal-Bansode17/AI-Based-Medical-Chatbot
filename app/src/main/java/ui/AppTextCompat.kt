package ui

/**
 * Compatibility bridge for existing screens in package ui.
 * The canonical translation table is in com.example.ai_based_medical_chatbot.
 */
fun appText(language: String, key: String): String =
    com.example.ai_based_medical_chatbot.appText(language, key)
