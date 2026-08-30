package ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * Dedicated voice assistant for MEDASSIST.
 *
 * The chatbot UI keeps its own display text.
 * This class creates a completely separate plain-text version
 * before sending anything to Android Text-to-Speech.
 */
class VoiceAssistant(
    context: Context,
    private val onSpeakingChanged: (Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "MEDASSIST_TTS"
        private const val UTTERANCE_ID = "medassist_response"
    }

    private val appContext = context.applicationContext

    private val textToSpeech =
        TextToSpeech(appContext, this)

    private var initialized = false

    override fun onInit(status: Int) {

        if (status == TextToSpeech.SUCCESS) {

            initialized = true

            textToSpeech.setOnUtteranceProgressListener(
                object : UtteranceProgressListener() {

                    override fun onStart(utteranceId: String?) {
                        onSpeakingChanged(true)
                    }

                    override fun onDone(utteranceId: String?) {
                        onSpeakingChanged(false)
                    }

                    @Deprecated("Deprecated by Android API")
                    override fun onError(utteranceId: String?) {
                        onSpeakingChanged(false)
                    }

                    override fun onError(
                        utteranceId: String?,
                        errorCode: Int
                    ) {
                        Log.e(TAG, "TTS error: $errorCode")
                        onSpeakingChanged(false)
                    }
                }
            )

            Log.d(TAG, "TTS initialized")

        } else {

            initialized = false

            Log.e(
                TAG,
                "TTS initialization failed"
            )
        }
    }

    /**
     * Speaks the supplied AI response after completely
     * removing Markdown/UI formatting.
     */
    fun speak(
        originalText: String,
        locale: Locale
    ) {

        if (!initialized) {
            Log.w(TAG, "TTS is not initialized")
            return
        }

        /*
         * IMPORTANT:
         * Cleaning happens immediately before TTS.
         * Therefore raw ###, **, emojis, etc. cannot reach
         * TextToSpeech.
         */
        val speechText =
            cleanForSpeech(originalText)

        if (speechText.isBlank()) {
            Log.w(TAG, "No readable text after cleaning")
            return
        }

        Log.d(
            TAG,
            "TTS cleaned text: $speechText"
        )

        textToSpeech.stop()

        setBestLanguage(locale)

        textToSpeech.speak(
            speechText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            UTTERANCE_ID
        )
    }

    fun stop() {

        if (initialized) {
            textToSpeech.stop()
        }

        onSpeakingChanged(false)
    }

    fun shutdown() {

        textToSpeech.stop()
        textToSpeech.shutdown()

        initialized = false

        onSpeakingChanged(false)
    }

    private fun setBestLanguage(
        requestedLocale: Locale
    ): Boolean {

        var result =
            textToSpeech.setLanguage(
                requestedLocale
            )

        if (
            result != TextToSpeech.LANG_MISSING_DATA &&
            result != TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            return true
        }

        val languageOnly =
            Locale(requestedLocale.language)

        result =
            textToSpeech.setLanguage(
                languageOnly
            )

        if (
            result != TextToSpeech.LANG_MISSING_DATA &&
            result != TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            return true
        }

        textToSpeech.setLanguage(Locale.US)

        return false
    }

    /**
     * Converts Gemini/Markdown text into plain natural speech.
     *
     * This function is deliberately aggressive:
     * the final output is guaranteed not to contain '#'
     * or Markdown formatting.
     */
    private fun cleanForSpeech(
        text: String
    ): String {

        return text
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .lines()

            .mapNotNull { rawLine ->

                var line = rawLine.trim()

                if (line.isBlank()) {
                    return@mapNotNull null
                }

                // Remove heading markers from the START of every line.
                // Handles:
                // ### Heading
                // ###**Heading**
                // ### **Heading**
                // ###### Heading
                line = line.replace(
                    Regex("""^\s*#{1,6}\s*"""),
                    ""
                )

                // Remove Markdown bullets.
                line = line.replace(
                    Regex("""^\s*[-*+•●◦▪·]+\s*"""),
                    ""
                )

                // Remove numbered-list markers.
                line = line.replace(
                    Regex("""^\s*\d+[.)]\s*"""),
                    ""
                )

                // Remove bold / italic / underline / code markers.
                line = line
                    .replace("**", "")
                    .replace("__", "")
                    .replace("`", "")

                line = line.replace(
                    Regex("""(?<!\*)\*(?!\*)"""),
                    ""
                )

                // Convert Markdown links to visible text.
                line = line.replace(
                    Regex("""\[([^\]]+)]\([^)]+\)"""),
                    "$1"
                )

                // Remove URLs.
                line = line.replace(
                    Regex("""https?://\S+"""),
                    ""
                )

                /*
                 * FINAL HASH SAFETY:
                 *
                 * Even if # appears somewhere that was not
                 * recognised as a heading, remove it here.
                 *
                 * This guarantees that TTS never receives #.
                 */
                line = line.replace("#", "")

                // Remove common emoji/supplementary Unicode pairs.
                line = line.replace(
                    Regex("""[\uD800-\uDBFF][\uDC00-\uDFFF]"""),
                    ""
                )

                // Remove decorative formatting.
                line = line.replace(
                    Regex("""[~^]+"""),
                    ""
                )

                // Clean whitespace.
                line = line
                    .replace(
                        Regex("""\s+"""),
                        " "
                    )
                    .trim()

                if (line.isBlank()) {
                    null
                } else {
                    line
                }
            }

            .joinToString(". ")

            // Remove accidental repeated sentence punctuation.
            .replace(
                Regex("""\.\s*\."""),
                "."
            )

            /*
             * ABSOLUTE FINAL SAFETY PASS.
             *
             * These run on the exact string that will be sent
             * to TextToSpeech.
             */
            .replace("#", "")
            .replace("**", "")
            .replace("`", "")

            .replace(
                Regex("""\s+"""),
                " "
            )

            .trim()
    }
}
