package ui

import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_based_medical_chatbot.data.api.Measurement
import com.example.ai_based_medical_chatbot.data.api.PredictionRequest
import com.example.ai_based_medical_chatbot.data.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

// =============================================================
// VOICE LANGUAGE SUPPORT
// =============================================================
//
// The backend already detects English / Hindi / Marathi from the
// user's actual text and generates the answer in that language.
// ML Kit below is used on the AI response before TTS so the spoken
// response automatically uses the matching voice.
//
// Requires:
// implementation("com.google.mlkit:language-id:17.0.6")

private const val RESPONSE_UTTERANCE_ID = "medassist_response"

private fun localeForDetectedCode(languageCode: String): Locale = when {
    languageCode.startsWith("hi", ignoreCase = true) -> Locale.forLanguageTag("hi-IN")
    languageCode.startsWith("mr", ignoreCase = true) -> Locale.forLanguageTag("mr-IN")
    else -> Locale.forLanguageTag("en-IN")
}

// The Flask backend returns the language actually used for the response
// as result.language ("en", "hi", or "mr"). The Android client uses that
// value directly for TTS so the frontend and backend stay synchronized.
// If the backend ever omits the field, English is used as a safe fallback.
private fun normalizeResponseLanguage(languageCode: String?): String {
    return when {
        languageCode?.startsWith("hi", ignoreCase = true) == true -> "hi"
        languageCode?.startsWith("mr", ignoreCase = true) == true -> "mr"
        else -> "en"
    }
}

// =============================================================
// CHAT MESSAGE
// =============================================================

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val intent: String = "",
    val confidence: Double = 0.0,
    val similarity: Double = 0.0,
    val isError: Boolean = false,
    val language: String = ""
)

// Backend contract used by ChatbotScreen:
// /predict returns `language` as "en", "hi", or "mr".
// This screen does not need to re-detect the generated answer.

// =============================================================
// CHATBOT SCREEN
// =============================================================

@Composable
fun ChatbotScreen(
    initialSessionId: String? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    // NOTE: `messages`, `sessionId`, etc. live in composition state (via
    // `remember`), not in a ViewModel. That means a configuration change
    // (e.g. screen rotation) will currently clear the conversation, since
    // Compose has to re-run this whole function from scratch. Flagging it
    // here rather than restructuring the file, since I can't see how this
    // screen is wired into the rest of your app.

    // =========================================================
    // SESSION
    // =========================================================

    val sessionId = remember {
        if (!initialSessionId.isNullOrBlank()) {
            initialSessionId.trim()
        } else {
            val preferences = context.getSharedPreferences(
                "medassist_preferences",
                Context.MODE_PRIVATE
            )

            preferences.getString("session_id", null) ?: run {
                val newSession = "android_${UUID.randomUUID()}"
                preferences.edit().putString("session_id", newSession).apply()
                newSession
            }
        }
    }

    // =========================================================
    // STATE
    // =========================================================

    var message by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<ChatMessage>() }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val topicScrollState = rememberScrollState()

    // =========================================================
    // VOICE ASSISTANT
    // =========================================================

    var isListening by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    // True only while we're waiting to speak the *next* AI response
    // (i.e. it was triggered by voice input). Reset to false any time a
    // response is spoken, fails, or the user sends a message by typing —
    // otherwise a failed voice request can leave this stuck "true" and a
    // later typed message gets read aloud unexpectedly.
    var speakNextResponse by remember { mutableStateOf(false) }
    var lastUserQuestion by remember { mutableStateOf("") }

    val textToSpeech = remember(context) {
        TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.d("MEDASSIST_TTS", "Text to speech initialized")
            } else {
                Log.e("MEDASSIST_TTS", "Text to speech initialization failed")
            }
        }
    }

    fun speakResponse(text: String, locale: Locale, utteranceId: String = RESPONSE_UTTERANCE_ID) {
        if (text.isBlank()) return

        textToSpeech.stop()

        val languageResult = textToSpeech.setLanguage(locale)
        if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
            languageResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            // Try the language without the India region first. Some Android
            // TTS engines provide hi/mr but not hi-IN/mr-IN.
            val languageOnly = Locale.forLanguageTag(locale.language)
            val languageOnlyResult = textToSpeech.setLanguage(languageOnly)

            if (languageOnlyResult == TextToSpeech.LANG_MISSING_DATA ||
                languageOnlyResult == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                Log.w(
                    "MEDASSIST_TTS",
                    "Voice for $locale is not installed/supported; falling back to English"
                )
                textToSpeech.setLanguage(Locale.US)
            }
        }

        isSpeaking = true
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    val popularTopics = listOf(
        "🌡️ I have fever",
        "🤢 I have vomiting",
        "🦴 I have back pain",
        "🤕 I have headache",
        "🤧 I have cold",
        "😷 I have cough",
        "🫃 I have stomach pain",
        "😴 I feel weak",
        "💪 I have body pain",
        "👄 I have sore throat",
        "😵 I feel dizzy",
        "🔥 I have acidity"
    )

    // =========================================================
    // CONTINUOUS TOPIC SLIDER
    // =========================================================

    LaunchedEffect(Unit) {
        while (true) {
            delay(2200)

            val maxScroll = topicScrollState.maxValue
            if (maxScroll > 0) {
                val nextPosition = topicScrollState.value + 170
                if (nextPosition >= maxScroll) {
                    topicScrollState.scrollTo(0)
                } else {
                    topicScrollState.animateScrollTo(nextPosition)
                }
            }
        }
    }

    // =========================================================
    // ONE-TIME WELCOME
    // =========================================================

    LaunchedEffect(Unit) {
        val preferences = context.getSharedPreferences(
            "medassist_preferences",
            Context.MODE_PRIVATE
        )

        val welcomeShown = preferences.getBoolean("welcome_message_shown", false)

        if (!welcomeShown && messages.isEmpty()) {
            messages.add(
                ChatMessage(
                    text = "Hello! 👋\n\n" +
                            "I’m MedAssist AI, your intelligent health assistant.\n\n" +
                            "You can ask me about:\n" +
                            "• Symptoms and general health\n" +
                            "• Diseases and conditions\n" +
                            "• Medicines and precautions\n" +
                            "• When medical attention may be needed\n\n" +
                            "How can I help you today?",
                    isUser = false
                )
            )

            preferences.edit().putBoolean("welcome_message_shown", true).apply()
        }
    }

    // =========================================================
    // AUTO SCROLL
    // =========================================================

    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    // =========================================================
    // SEND MESSAGE
    // =========================================================

    fun sendMessage(text: String) {
        val cleanText = text.trim()
        if (cleanText.isEmpty() || isTyping) return

        if (isSpeaking) {
            textToSpeech.stop()
            isSpeaking = false
        }

        isListening = false

        messages.add(ChatMessage(text = cleanText, isUser = true))
        lastUserQuestion = cleanText
        message = ""
        keyboardController?.hide()
        isTyping = true

        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.predict(
                        PredictionRequest(sessionId = sessionId, text = cleanText)
                    )
                }

                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!

                    Log.d("MEDASSIST_API", "QUESTION = ${result.question}")
                    Log.d("MEDASSIST_API", "ANSWER = ${result.answer}")
                    Log.d("MEDASSIST_API", "SOURCE = ${result.source}")
                    Log.d("MEDASSIST_API", "LANGUAGE = ${result.language}")
                    Log.d("MEDASSIST_API", "SESSION_ID = $sessionId")

                    val answer = result.answer?.trim().orEmpty()
                    val finalAnswer = if (answer.isNotEmpty()) {
                        fixMeasurementFormatting(answer = answer, measurements = result.measurements)
                    } else {
                        "I’m sorry, I could not generate a response right now."
                    }

                    messages.add(
                        ChatMessage(
                            text = finalAnswer,
                            isUser = false,
                            intent = result.intent,
                            confidence = result.confidence,
                            similarity = result.answerSimilarity,
                            language = normalizeResponseLanguage(result.language)
                        )
                    )

                    if (speakNextResponse) {
                        // IMPORTANT:
                        // Flask /predict already detects the user's language and
                        // returns the actual response language as result.language.
                        // Use that backend value directly instead of running a
                        // second language detector on the generated answer.
                        val responseLanguage = normalizeResponseLanguage(result.language)
                        val responseLocale = localeForDetectedCode(responseLanguage)

                        Log.d(
                            "MEDASSIST_VOICE",
                            "Backend response language = $responseLanguage, TTS locale = $responseLocale"
                        )

                        speakResponse(finalAnswer, responseLocale)
                        speakNextResponse = false
                    }
                } else {
                    speakNextResponse = false
                    messages.add(
                        ChatMessage(
                            text = "Sorry, I’m having trouble connecting to the medical AI service. " +
                                    "Please check your internet connection and try again.",
                            isUser = false,
                            isError = true
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("MEDASSIST_API", "API ERROR", e)

                speakNextResponse = false
                messages.add(
                    ChatMessage(
                        text = "Unable to connect to MedAssist AI right now.\n\n" +
                                "Please make sure the server is running and try again.",
                        isUser = false,
                        isError = true
                    )
                )
            } finally {
                isTyping = false
            }
        }
    }

    // =========================================================
    // SPEECH RECOGNITION
    // =========================================================

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false

        val spokenText = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()

        if (spokenText.isNotBlank()) {
            // The backend detects English / Hindi / Marathi from the
            // actual spoken text. No language button is required.
            Log.d("MEDASSIST_VOICE", "Speech text = $spokenText")
            message = spokenText
            speakNextResponse = true
            sendMessage(spokenText)
        } else {
            speakNextResponse = false
        }
    }

    fun launchSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            // Do not hard-code EN / HI / MR here. The recognizer uses the
            // device's available speech-recognition configuration to produce
            // the transcription. The Flask backend then detects English,
            // Hindi, or Marathi from the received text and controls the
            // response language.
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to MedAssist AI")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        try {
            isListening = true
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            isListening = false
            speakNextResponse = false
            messages.add(
                ChatMessage(
                    text = "Voice input is not available on this device right now. You can still type your question.",
                    isUser = false,
                    isError = true
                )
            )
            Log.e("MEDASSIST_VOICE", "Speech recognition unavailable", e)
        }
    }

    // Keep UI state synchronized with the actual TTS engine.
    fun onSpeechFinished(utteranceId: String?, completedNormally: Boolean) {
        mainHandler.post {
            isSpeaking = false
        }
    }

    fun startVoiceInput() {
        if (isTyping || isListening || isSpeaking) return
        launchSpeechRecognition()
    }

    // Keep the UI state synchronized with the actual TTS engine, and chain
    // the greeting into listening once it finishes speaking. Without this,
    // the voice button can stay stuck in the "speaking" state after the
    // response has actually finished.
    DisposableEffect(textToSpeech) {
        textToSpeech.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    onSpeechFinished(utteranceId, completedNormally = true)
                }

                override fun onError(utteranceId: String?) {
                    speakNextResponse = false
                    onSpeechFinished(utteranceId, completedNormally = false)
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    speakNextResponse = false
                    onSpeechFinished(utteranceId, completedNormally = false)
                }
            }
        )

        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    // =========================================================
    // MAIN UI
    // =========================================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FC))
    ) {

        // ----- PROFESSIONAL TOP BAR -----
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1976D2)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("AI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = "MedAssist AI",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF17202A)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isTyping) "Thinking…" else "AI health assistant",
                            fontSize = 11.sp,
                            color = Color(0xFF667085)
                        )
                    }
                }

                IconButton(
                    onClick = { if (!isTyping && messages.size > 1) showClearDialog = true },
                    enabled = !isTyping
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear conversation",
                        tint = Color(0xFF667085)
                    )
                }
            }
        }

        // ----- POPULAR HEALTH TOPICS -----
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp)) {
                Text(
                    text = "Common health questions",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF667085)
                )

                Spacer(modifier = Modifier.height(7.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(topicScrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    popularTopics.forEach { topic ->
                        QuickPrompt(
                            text = topic,
                            onClick = {
                                val query = topic.replace(Regex("^[^A-Za-z]+"), "")
                                sendMessage(query)
                            }
                        )
                    }
                }
            }
        }

        // ----- CHAT AREA -----
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            items(items = messages) { chatMessage ->
                MessageBubble(
                    message = chatMessage,
                    context = context,
                    onRegenerate = {
                        if (!isTyping && lastUserQuestion.isNotBlank()) {
                            sendMessage(lastUserQuestion)
                        }
                    },
                    onSpeak = { aiMessage ->
                        if (isSpeaking) {
                            textToSpeech.stop()
                            isSpeaking = false
                        } else {
                            val locale = localeForDetectedCode(
                                normalizeResponseLanguage(aiMessage.language)
                            )
                            speakResponse(
                                aiMessage.text,
                                locale,
                                "manual_${UUID.randomUUID()}"
                            )
                        }
                    }
                )
            }

            if (isTyping) {
                item { TypingIndicator() }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // ----- GEMINI-STYLE INPUT AREA -----
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding(),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFFF3F5F7),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE4E7EC))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                Toast.makeText(context, "More options coming soon", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Text(
                                text = "+",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Light,
                                color = Color(0xFF475467)
                            )
                        }

                        BasicTextField(
                            value = message,
                            onValueChange = { message = it },
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 15.sp,
                                color = Color(0xFF344054)
                            ),
                            singleLine = false,
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    speakNextResponse = false
                                    sendMessage(message)
                                }
                            ),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 9.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (message.isEmpty()) {
                                        Text(
                                            text = if (isListening) "Listening…" else "Ask MedAssist anything…",
                                            fontSize = 15.sp,
                                            color = Color(0xFF98A2B3)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        val canVoice = !isTyping
                        Surface(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable(enabled = canVoice) {
                                    when {
                                        isSpeaking -> {
                                            textToSpeech.stop()
                                            isSpeaking = false
                                        }
                                        !isListening -> startVoiceInput()
                                    }
                                }
                                .semantics {
                                    contentDescription = when {
                                        isListening -> "Stop listening"
                                        isSpeaking -> "Stop speaking"
                                        else -> "Start voice input"
                                    }
                                },
                            shape = CircleShape,
                            color = when {
                                isListening -> Color(0xFFFFEBEE)
                                isSpeaking -> Color(0xFFE8F5E9)
                                else -> Color.Transparent
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                when {
                                    isListening -> {
                                        Text(
                                            text = "■",
                                            fontSize = 14.sp,
                                            color = Color(0xFFD32F2F),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    isSpeaking -> {
                                        SpeakerIcon(tint = Color(0xFF188038))
                                    }
                                    else -> {
                                        MicIcon(
                                            tint = if (canVoice) {
                                                Color(0xFF475467)
                                            } else {
                                                Color(0xFF98A2B3)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                val canSend = message.trim().isNotEmpty() && !isTyping
                Surface(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .clickable(enabled = canSend) {
                            speakNextResponse = false
                            sendMessage(message)
                        },
                    shape = CircleShape,
                    color = if (canSend) Color(0xFF1976D2) else Color(0xFFE4E7EC)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (canSend) Color.White else Color(0xFF98A2B3),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }

    // ----- CLEAR CHAT DIALOG -----
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear conversation?") },
            text = { Text("This will clear the current conversation from the screen.") },
            confirmButton = {
                Button(
                    onClick = {
                        messages.clear()

                        val preferences = context.getSharedPreferences(
                            "medassist_preferences",
                            Context.MODE_PRIVATE
                        )
                        preferences.edit().putBoolean("welcome_message_shown", true).apply()

                        showClearDialog = false

                        messages.add(
                            ChatMessage(
                                text = "Hello! 👋\n\n" +
                                        "I’m MedAssist AI, your intelligent health assistant.\n\n" +
                                        "How can I help you today?",
                                isUser = false
                            )
                        )
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =============================================================
// QUICK PROMPT
// =============================================================

@Composable
fun QuickPrompt(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            fontSize = 12.sp,
            color = Color(0xFF1976D2)
        )
    }
}

// =============================================================
// FIX MEDICAL MEASUREMENT DISPLAY
// =============================================================

private fun fixMeasurementFormatting(
    answer: String,
    measurements: List<Measurement>
): String {
    var fixedAnswer = answer

    for (measurement in measurements) {
        val name = measurement.name.trim()
        val value = measurement.value.trim()
        val unit = measurement.unit.trim()

        if (name.isBlank() || value.isBlank()) continue

        // Avoid touching values that are already formatted as whole numbers.
        if (!value.contains(".")) continue

        val escapedName = Regex.escape(name.replace("_", " "))
        val unitPart = if (unit.isNotBlank()) "\\s*" + Regex.escape(unit) else ""

        val pattern = Regex(
            "(?i)(\\b$escapedName\\b\\s*[:=]?\\s*)\\d+(?:\\.\\d+)?$unitPart"
        )

        fixedAnswer = pattern.replace(fixedAnswer) { match ->
            val prefix = match.groupValues[1]
            if (unit.isNotBlank()) "$prefix$value $unit" else "$prefix$value"
        }
    }

    return fixedAnswer
}

// =============================================================
// MESSAGE BUBBLE
// =============================================================

@Composable
fun MessageBubble(
    message: ChatMessage,
    context: Context,
    onRegenerate: () -> Unit,
    onSpeak: (ChatMessage) -> Unit
) {
    var liked by remember(message.text) { mutableStateOf<Boolean?>(null) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!message.isUser) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (message.isError) Color(0xFFD32F2F)
                        else Color(0xFF1976D2)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AI",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(7.dp))
        }

        Column(
            modifier = Modifier.fillMaxWidth(0.86f),
            horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = if (message.isUser) {
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 5.dp
                    )
                } else {
                    RoundedCornerShape(
                        topStart = 5.dp,
                        topEnd = 20.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 20.dp
                    )
                },
                color = when {
                    message.isError -> Color(0xFFFFEBEE)
                    message.isUser -> Color(0xFF1976D2)
                    else -> Color.White
                },
                shadowElevation = if (message.isUser) 0.dp else 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 11.dp
                    )
                ) {
                    FormattedMessageText(
                        text = message.text,
                        isUser = message.isUser
                    )
                }
            }

            // Professional Gemini-style response action bar.
            // User messages remain unchanged.
            if (!message.isUser && !message.isError) {
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActionIcon(
                        icon = Icons.Default.ThumbUp,
                        description = "Helpful",
                        active = liked == true,
                        onClick = {
                            liked = if (liked == true) null else true
                            Toast.makeText(
                                context,
                                if (liked == true) "Marked helpful" else "Feedback cleared",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )

                    ActionIcon(
                        icon = null,
                        textIcon = "👎",
                        description = "Not helpful",
                        active = liked == false,
                        onClick = {
                            liked = if (liked == false) null else false
                            Toast.makeText(
                                context,
                                if (liked == false) "Feedback recorded" else "Feedback cleared",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )

                    ActionIcon(
                        icon = Icons.Default.Refresh,
                        description = "Regenerate",
                        onClick = onRegenerate
                    )

                    ActionIcon(
                        icon = Icons.Default.Share,
                        description = "Share",
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, message.text)
                            }
                            context.startActivity(
                                Intent.createChooser(
                                    shareIntent,
                                    "Share MedAssist response"
                                )
                            )
                        }
                    )

                    ActionIcon(
                        icon = null,
                        textIcon = "⧉",
                        description = "Copy",
                        onClick = {
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE)
                                    as? ClipboardManager

                            clipboard?.setPrimaryClip(
                                ClipData.newPlainText(
                                    "MedAssist response",
                                    message.text
                                )
                            )

                            Toast.makeText(
                                context,
                                "Response copied",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )

                    ActionIcon(
                        icon = Icons.Default.MoreVert,
                        description = "More",
                        onClick = {
                            Toast.makeText(
                                context,
                                "More options coming soon",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Dedicated response speaker button.
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { onSpeak(message) }
                            .semantics {
                                contentDescription = "Read response aloud"
                            },
                        shape = CircleShape,
                        color = Color(0xFFE8F5E9),
                        shadowElevation = 1.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            SpeakerIcon(tint = Color(0xFF188038))
                        }
                    }
                }
            }

            if (!message.isUser && !message.isError && message.intent.isNotBlank()) {
                Row(
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = message.intent
                            .replace("_", " ")
                            .replaceFirstChar { it.uppercase() },
                        fontSize = 9.sp,
                        color = Color(0xFF98A2B3)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    description: String,
    active: Boolean = false,
    textIcon: String? = null,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(38.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = if (active) Color(0xFF1976D2) else Color(0xFF667085),
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = textIcon ?: "",
                color = if (active) Color(0xFF1976D2) else Color(0xFF667085),
                fontSize = 20.sp,
                modifier = Modifier.semantics {
                    contentDescription = description
                }
            )
        }
    }
}

// =============================================================
// FORMATTED MESSAGE
// =============================================================

@Composable
private fun FormattedMessageText(text: String, isUser: Boolean) {
    val textColor = if (isUser) Color.White else Color(0xFF344054)
    val lines = text.replace("\r\n", "\n").split("\n")

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        lines.forEach { rawLine ->
            val line = rawLine.trimEnd()

            if (line.isBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                return@forEach
            }

            val isBullet = line.startsWith("•") || line.startsWith("- ") || line.startsWith("* ")

            if (isBullet) {
                val bulletText = if (line.startsWith("•")) {
                    line.removePrefix("•").trim()
                } else {
                    line.substring(2).trim()
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(text = "•", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = cleanMarkdown(bulletText),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                val cleaned = cleanMarkdown(line)
                val isHeading = line.startsWith("###") || line.startsWith("##") || line.startsWith("# ") ||
                        (line.endsWith(":") && line.length < 80)

                Text(
                    text = cleaned,
                    fontSize = if (isHeading) 15.sp else 14.sp,
                    lineHeight = 21.sp,
                    fontWeight = if (isHeading) FontWeight.SemiBold else FontWeight.Normal,
                    color = textColor
                )
            }
        }
    }
}

// =============================================================
// CLEAN MARKDOWN
// =============================================================

private fun cleanMarkdown(text: String): String {
    return text
        .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1") // Bold
        .replace(Regex("(?<!\\*)\\*(.*?)\\*(?!\\*)"), "$1") // Italic
        .replace(Regex("^#{1,6}\\s*"), "") // Markdown headings
        .replace("`", "") // Inline code
        .trim()
}

// =============================================================
// CUSTOM VOICE ICONS
// =============================================================

@Composable
private fun MicIcon(tint: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.36f, h * 0.08f),
            size = androidx.compose.ui.geometry.Size(w * 0.28f, h * 0.52f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.14f, w * 0.14f)
        )
        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.22f, h * 0.30f),
            size = androidx.compose.ui.geometry.Size(w * 0.56f, h * 0.48f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.78f),
            end = androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.92f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.92f),
            end = androidx.compose.ui.geometry.Offset(w * 0.65f, h * 0.92f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun SpeakerIcon(tint: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()
        drawRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.12f, h * 0.40f),
            size = androidx.compose.ui.geometry.Size(w * 0.18f, h * 0.20f)
        )
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.30f, h * 0.40f)
            lineTo(w * 0.48f, h * 0.22f)
            lineTo(w * 0.48f, h * 0.78f)
            lineTo(w * 0.30f, h * 0.60f)
            close()
        }
        drawPath(path = path, color = tint)
        drawArc(
            color = tint,
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.43f, h * 0.26f),
            size = androidx.compose.ui.geometry.Size(w * 0.36f, h * 0.48f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = tint,
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.14f),
            size = androidx.compose.ui.geometry.Size(w * 0.48f, h * 0.72f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

// =============================================================
// TYPING INDICATOR
// =============================================================

@Composable
fun TypingIndicator() {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFF1976D2)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "AI", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(7.dp))

        Surface(shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 1.dp) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TypingDot()
                TypingDot()
                TypingDot()
            }
        }
    }
}

@Composable
private fun TypingDot() {
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(Color(0xFF98A2B3))
    )
}
