package ui

import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_based_medical_chatbot.data.SupabaseClient
import com.example.ai_based_medical_chatbot.data.api.Measurement
import com.example.ai_based_medical_chatbot.data.api.PredictionRequest
import com.example.ai_based_medical_chatbot.data.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

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
            // Chat sessions are now tied to the authenticated Supabase user.
            // This keeps chat history separated between users.
            SupabaseClient.getChatSessionId(context)
                ?: "unauthenticated"
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

    val voiceAssistant = remember(context) {
        VoiceAssistant(context) { speaking ->
            mainHandler.post {
                isSpeaking = speaking
            }
        }
    }

    DisposableEffect(voiceAssistant) {
        onDispose {
            voiceAssistant.shutdown()
        }
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

        // Chatbot access is expected to happen after authentication.
        // Never send an anonymous/shared session to the backend.
        if (SupabaseClient.getSavedUser(context) == null) {
            messages.add(
                ChatMessage(
                    text = "Please login to use MedAssist AI.",
                    isUser = false,
                    isError = true
                )
            )
            return
        }

        if (isSpeaking) {
            voiceAssistant.stop()
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

                        voiceAssistant.speak(
                            cleanMarkdownForSpeech(finalAnswer),
                            responseLocale
                        )
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

    fun startVoiceInput() {
        if (isTyping || isListening || isSpeaking) return
        launchSpeechRecognition()
    }

    // =========================================================
    // MAIN UI
    // =========================================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FC))
    ) {

        // ----- TOP BAR -----
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1976D2)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "AI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MedAssist AI",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF17202A)
                    )
                    Text(
                        text = if (isTyping) "MedAssist is thinking…" else "Your intelligent health companion",
                        fontSize = 12.sp,
                        color = Color(0xFF667085)
                    )
                }

                IconButton(
                    onClick = { if (messages.size > 1) showClearDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear chat",
                        tint = Color(0xFF667085)
                    )
                }

                if (isTyping) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
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
                            voiceAssistant.stop()
                        } else {
                            val locale = localeForDetectedCode(
                                normalizeResponseLanguage(aiMessage.language)
                            )
                            voiceAssistant.speak(
                                cleanMarkdownForSpeech(aiMessage.text),
                                locale
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

        // ----- INPUT AREA -----
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding(),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            if (isListening) {
                                "Listening…"
                            } else {
                                "Ask MedAssist anything..."
                            }
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            speakNextResponse = false
                            sendMessage(message)
                        }
                    ),
                    maxLines = 4,
                    shape = RoundedCornerShape(22.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // ----- VOICE BUTTON -----
                val canVoice = !isTyping
                val voiceDescription = when {
                    isListening -> "Stop listening"
                    isSpeaking -> "Stop speaking"
                    else -> "Start voice input"
                }

                IconButton(
                    onClick = {
                        if (isSpeaking) {
                            voiceAssistant.stop()
                        } else if (!isListening) {
                            startVoiceInput()
                        }
                    },
                    enabled = canVoice,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isListening -> Color(0xFFD32F2F)
                                isSpeaking -> Color(0xFF43A047)
                                canVoice -> Color(0xFF1976D2)
                                else -> Color(0xFFB0BEC5)
                            }
                        )
                        .semantics { contentDescription = voiceDescription }
                ) {
                    Text(
                        text = when {
                            isListening -> "●"
                            isSpeaking -> "■"
                            else -> "🎙️"
                        },
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // ----- SEND BUTTON -----
                val canSend = message.trim().isNotEmpty() && !isTyping

                IconButton(
                    onClick = {
                        speakNextResponse = false
                        sendMessage(message)
                    },
                    enabled = canSend,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(if (canSend) Color(0xFF1976D2) else Color(0xFFB0BEC5))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White
                    )
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

            // Gemini-style response action bar.
            // Only AI responses get these actions; user bubbles remain unchanged.
            if (!message.isUser && !message.isError) {
                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
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

                    // Speaker action is separated to the right, matching
                    // the Gemini-style layout from the reference image.
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E7D32))
                            .clickable { onSpeak(message) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🔊",
                            fontSize = 20.sp,
                            modifier = Modifier.semantics {
                                contentDescription = "Speak response"
                            }
                        )
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
            val line = rawLine.trim().trimEnd()

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
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .lines()
        .map { line ->
            var cleaned = line.trim()

            // Remove headings from every line: #, ##, ###, etc.
            cleaned = cleaned.replace(
                Regex("^\\s*#{1,6}\\s*"),
                ""
            )

            // Remove Markdown bullet/list markers.
            cleaned = cleaned.replace(
                Regex("^\\s*[-*+•●◦▪·]+\\s*"),
                ""
            )

            cleaned = cleaned.replace(
                Regex("^\\s*\\d+[.)]\\s*"),
                ""
            )

            // Remove formatting markers but preserve their text.
            cleaned = cleaned
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replace(
                    Regex("(?<!\\*)\\*(?!\\*)"),
                    ""
                )

            // Safety net: no heading marker should ever reach the UI.
            cleaned = cleaned.replace("#", "")

            cleaned
        }
        .joinToString("\n")
        .trim()
}

// =============================================================
// CLEAN MARKDOWN FOR VOICE ASSISTANT
// =============================================================

/**
 * Converts the AI Markdown response into natural speech.
 * The UI keeps its formatting, while TTS receives clean plain text.
 */
private fun cleanMarkdownForSpeech(text: String): String {
    return text
        .replace("\\r\\n", "\\n")
        .replace("\\r", "\\n")
        .lines()
        .map { rawLine ->
            var line = rawLine.trim()

            // Markdown headings.
            line = line.replace(Regex("^#{1,6}\\s*"), "")

            // Markdown blockquotes.
            line = line.replace(Regex("^>\\s*"), "")

            // Bullets.
            line = line.replace(Regex("^[-*+•●◦▪·]\\s+"), "")

            // Numbered lists.
            line = line.replace(Regex("^\\d+[.)]\\s+"), "")

            // Bold, italic, underline and inline-code markers.
            line = line
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replace(Regex("(?<!\\*)\\*(?!\\*)"), "")

            // Markdown links -> visible text only.
            line = line.replace(
                Regex("\\[([^]]+)]\\([^)]*\\)"),
                "$1"
            )

            // Remove any remaining Markdown hash characters.
            line = line.replace("#", "")

            // Remove emoji/symbol characters without unsupported
            // Unicode-regex syntax. This works with Kotlin/JVM safely.
            line = line.filter { ch ->
                val cp = ch.code
                when {
                    cp in 0x1F1E6..0x1F1FF -> false
                    cp in 0x1F300..0x1F5FF -> false
                    cp in 0x1F600..0x1F64F -> false
                    cp in 0x1F680..0x1F6FF -> false
                    cp in 0x1F700..0x1F77F -> false
                    cp in 0x1F780..0x1F7FF -> false
                    cp in 0x1F800..0x1F8FF -> false
                    cp in 0x1F900..0x1F9FF -> false
                    cp in 0x1FA00..0x1FAFF -> false
                    cp in 0x2600..0x26FF -> false
                    cp in 0x2700..0x27BF -> false
                    else -> true
                }
            }

            // Normalize whitespace.
            line = line.replace(Regex("\\s+"), " ").trim()
            line
        }
        .filter { it.isNotBlank() }
        .joinToString(". ")
        .replace(Regex("\\.{2,}"), ".")
        .trim()
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
