package ui


import android.content.Context
import android.content.Intent
import android.util.Log

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

import com.example.ai_based_medical_chatbot.data.api.PredictionRequest
import com.example.ai_based_medical_chatbot.data.api.RetrofitClient

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.json.JSONArray
import org.json.JSONObject



// =============================================================
// PRESCRIPTION MEMORY
// =============================================================

object PrescriptionMemory {

    fun build(context: Context): String {

        val prescriptions =
            PrescriptionRepository.getSavedPrescriptions(context)

        if (prescriptions.length() == 0) {
            return ""
        }

        val builder = StringBuilder()

        for (i in 0 until prescriptions.length()) {

            val prescription =
                prescriptions.optJSONObject(i) ?: continue

            builder.append("Prescription ${i + 1}:\n")
            builder.append("Doctor: ")
            builder.append(
                prescription.optString(
                    "doctorName",
                    "Not available"
                )
            )
            builder.append("\n")

            builder.append("Patient: ")
            builder.append(
                prescription.optString(
                    "patientName",
                    "Not available"
                )
            )
            builder.append("\n")

            builder.append("Date: ")
            builder.append(
                prescription.optString(
                    "date",
                    "Not available"
                )
            )
            builder.append("\n")

            builder.append("Diagnosis: ")
            builder.append(
                prescription.optString(
                    "diagnosis",
                    "Not available"
                )
            )
            builder.append("\n")

            val medicines =
                prescription.optJSONArray("medicines")

            if (medicines != null && medicines.length() > 0) {

                builder.append("Medicines:\n")

                for (j in 0 until medicines.length()) {

                    val medicine =
                        medicines.optJSONObject(j) ?: continue

                    builder.append("- Medicine: ")
                    builder.append(
                        medicine.optString("medicineName")
                    )
                    builder.append(", Strength: ")
                    builder.append(
                        medicine.optString("strength")
                    )
                    builder.append(", Frequency: ")
                    builder.append(
                        medicine.optString("frequency")
                    )
                    builder.append(", Duration: ")
                    builder.append(
                        medicine.optString("duration")
                    )
                    builder.append(", Instructions: ")
                    builder.append(
                        medicine.optString("instructions")
                    )
                    builder.append("\n")
                }
            }

            builder.append("\n")
        }

        return builder.toString().trim()
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

    val isError: Boolean = false
)


// =============================================================
// SAVED CHAT HISTORY
// =============================================================

data class SavedChat(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val messages: List<ChatMessage>,
    val pinned: Boolean = false,
    val isTitleCustom: Boolean = false
)

object ChatHistoryRepository {
    private const val PREFS = "medassist_chat_history"
    private const val KEY_CHATS = "saved_chats"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getChats(context: Context): List<SavedChat> {
        return try {
            val array = JSONArray(
                prefs(context).getString(KEY_CHATS, "[]") ?: "[]"
            )
            val result = mutableListOf<SavedChat>()

            for (i in 0 until array.length()) {
                val chat = array.optJSONObject(i) ?: continue
                val arr = chat.optJSONArray("messages") ?: JSONArray()
                val list = mutableListOf<ChatMessage>()

                for (j in 0 until arr.length()) {
                    val item = arr.optJSONObject(j) ?: continue
                    list.add(
                        ChatMessage(
                            text = item.optString("text"),
                            isUser = item.optBoolean("isUser"),
                            intent = item.optString("intent"),
                            confidence = item.optDouble("confidence", 0.0),
                            similarity = item.optDouble("similarity", 0.0),
                            isError = item.optBoolean("isError")
                        )
                    )
                }

                if (list.isNotEmpty()) {
                    result.add(
                        SavedChat(
                            id = chat.optString("id"),
                            title = chat.optString("title", "New conversation"),
                            updatedAt = chat.optLong("updatedAt", 0L),
                            messages = list,
                            pinned = chat.optBoolean("pinned", false),
                            isTitleCustom = chat.optBoolean("isTitleCustom", false)
                        )
                    )
                }
            }

            result.sortedWith(
                compareByDescending<SavedChat> { it.pinned }
                    .thenByDescending { it.updatedAt }
            )
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveChat(context: Context, sessionId: String, messages: List<ChatMessage>) {
        if (messages.isEmpty()) return

        val existing = getChats(context).firstOrNull { it.id == sessionId }
        val all = getChats(context)
            .filter { it.id != sessionId }
            .toMutableList()

        val generatedTitle = messages.firstOrNull { it.isUser && it.text.isNotBlank() }
            ?.text
            ?.replace("\n", " ")
            ?.trim()
            ?.let { if (it.length > 42) it.take(42) + "..." else it }
            ?: "New conversation"

        val title = if (existing?.isTitleCustom == true) {
            existing.title
        } else {
            generatedTitle
        }

        all.add(
            SavedChat(
                id = sessionId,
                title = title,
                updatedAt = System.currentTimeMillis(),
                messages = messages.toList(),
                pinned = existing?.pinned ?: false,
                isTitleCustom = existing?.isTitleCustom ?: false
            )
        )

        writeChats(context, all)
    }

    fun renameChat(context: Context, sessionId: String, newTitle: String) {
        val cleanTitle = newTitle.trim()
        if (cleanTitle.isBlank()) return

        val updated = getChats(context).map { chat ->
            if (chat.id == sessionId) {
                chat.copy(
                    title = cleanTitle.take(60),
                    isTitleCustom = true
                )
            } else {
                chat
            }
        }
        writeChats(context, updated)
    }

    fun setPinned(context: Context, sessionId: String, pinned: Boolean) {
        val updated = getChats(context).map { chat ->
            if (chat.id == sessionId) chat.copy(pinned = pinned) else chat
        }
        writeChats(context, updated)
    }

    fun deleteChat(context: Context, sessionId: String) {
        val updated = getChats(context).filter { it.id != sessionId }
        writeChats(context, updated)
    }

    private fun writeChats(context: Context, chats: List<SavedChat>) {
        val array = JSONArray()

        chats
            .sortedWith(
                compareByDescending<SavedChat> { it.pinned }
                    .thenByDescending { it.updatedAt }
            )
            .take(30)
            .forEach { chat ->
                val obj = JSONObject()
                    .put("id", chat.id)
                    .put("title", chat.title)
                    .put("updatedAt", chat.updatedAt)
                    .put("pinned", chat.pinned)
                    .put("isTitleCustom", chat.isTitleCustom)

                val arr = JSONArray()
                chat.messages.forEach { item ->
                    arr.put(
                        JSONObject()
                            .put("text", item.text)
                            .put("isUser", item.isUser)
                            .put("intent", item.intent)
                            .put("confidence", item.confidence)
                            .put("similarity", item.similarity)
                            .put("isError", item.isError)
                    )
                }
                obj.put("messages", arr)
                array.put(obj)
            }

        prefs(context).edit()
            .putString(KEY_CHATS, array.toString())
            .apply()
    }
}

// =============================================================
// CHATBOT SCREEN
// =============================================================

@Composable
fun ChatbotScreen(

    initialSessionId: String? = null,

    initialLanguage: String? = null,

    onBack: () -> Unit

) {

    val context =
        LocalContext.current

    val keyboardController =
        LocalSoftwareKeyboardController.current


    // =========================================================
    // SESSION
    // =========================================================

    var currentSessionId by remember {
        mutableStateOf(
            if (!initialSessionId.isNullOrBlank()) {
                initialSessionId.trim()
            } else {
                "android_" + System.currentTimeMillis()
            }
        )
    }


    // =========================================================
    // STATE
    // =========================================================

    var message by remember {

        mutableStateOf("")
    }


    var isTyping by remember {

        mutableStateOf(false)
    }


    var showClearDialog by remember {

        mutableStateOf(false)
    }

    var showChatSidebar by remember {
        mutableStateOf(false)
    }

    var savedChats by remember {
        mutableStateOf(ChatHistoryRepository.getChats(context))
    }

    var selectedChatForActions by remember { mutableStateOf<SavedChat?>(null) }
    var showChatActions by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }


    val messages =
        remember {

            mutableStateListOf<ChatMessage>()
        }


    val scope =
        rememberCoroutineScope()


    val chatScrollState =
        rememberScrollState()

    LaunchedEffect(Unit) {
        val existing = ChatHistoryRepository.getChats(context)
            .firstOrNull { it.id == currentSessionId }

        if (existing != null && messages.isEmpty()) {
            messages.addAll(existing.messages)
        }

        savedChats = ChatHistoryRepository.getChats(context)
    }


    // =========================================================
    // START SCREEN WELCOME
    // Shows only while there are no chat messages.
    // It disappears permanently for the current chat as soon
    // as the user sends the first message.
    // =========================================================

    // =========================================================
    // AUTO SCROLL
    // =========================================================

    LaunchedEffect(
        messages.size,
        isTyping
    ) {
        if (messages.isNotEmpty()) {
            chatScrollState.animateScrollTo(chatScrollState.maxValue)
        }
    }


    // =========================================================
    // SEND MESSAGE
    // =========================================================

    fun sendMessage(
        text: String
    ) {

        val cleanText =
            text.trim()


        if (
            cleanText.isEmpty() ||
            isTyping
        ) {

            return
        }


        // =====================================================
        // USER MESSAGE
        // =====================================================

        messages.add(

            ChatMessage(

                text =
                    cleanText,

                isUser =
                    true
            )
        )

        ChatHistoryRepository.saveChat(context, currentSessionId, messages)
        savedChats = ChatHistoryRepository.getChats(context)


        message = ""


        // =====================================================
        // HIDE KEYBOARD
        // =====================================================

        keyboardController?.hide()


        isTyping = true


        // =====================================================
        // API REQUEST
        // =====================================================

        scope.launch {

            try {

                val response =

                    withContext(
                        Dispatchers.IO
                    ) {

                        val prescriptionMemory =
                            PrescriptionMemory.build(context)

                        val chatbotText =
                            if (prescriptionMemory.isBlank()) {
                                cleanText
                            } else {
                                """
                                USER'S SAVED PRESCRIPTION MEMORY:

                                $prescriptionMemory

                                IMPORTANT: Use the saved prescription only as
                                the user's personal medical context. Do not
                                invent or change medicine name, strength,
                                dosage, frequency, duration, diagnosis,
                                doctor name, patient name, or instructions.
                                If information is missing, say that it is
                                not available in the saved prescription.

                                USER QUESTION:
                                $cleanText
                                """.trimIndent()
                            }

                        RetrofitClient
                            .apiService
                            .predict(
                                PredictionRequest(
                                    sessionId = currentSessionId,
                                    text = chatbotText
                                )
                            )
                    }


                // =================================================
                // API SUCCESS
                // =================================================

                if (
                    response.isSuccessful &&
                    response.body() != null
                ) {

                    val result =
                        response.body()!!


                    Log.d(
                        "MEDASSIST_API",
                        "QUESTION = ${result.question}"
                    )


                    Log.d(
                        "MEDASSIST_API",
                        "ANSWER = ${result.answer}"
                    )


                    Log.d(
                        "MEDASSIST_API",
                        "SOURCE = ${result.source}"
                    )


                    Log.d(
                        "MEDASSIST_API",
                        "SESSION_ID = $currentSessionId"
                    )


                    val answer =
                        result.answer
                            ?.trim()
                            .orEmpty()


                    val finalAnswer =

                        if (
                            answer.isNotEmpty()
                        ) {

                            fixMeasurementFormatting(

                                answer =
                                    answer,

                                measurements =
                                    result.measurements
                            )

                        } else {

                            "I’m sorry, I could not generate a response right now."
                        }


                    // =================================================
                    // AI MESSAGE
                    // =================================================

                    messages.add(

                        ChatMessage(

                            text =
                                finalAnswer,

                            isUser =
                                false,

                            intent =
                                result.intent,

                            confidence =
                                result.confidence,

                            similarity =
                                result.answerSimilarity
                        )
                    )

                    ChatHistoryRepository.saveChat(context, currentSessionId, messages)
                    savedChats = ChatHistoryRepository.getChats(context)

                } else {

                    // =============================================
                    // API ERROR
                    // =============================================

                    messages.add(

                        ChatMessage(

                            text =
                                "Sorry, I’m having trouble connecting to the medical AI service. " +
                                        "Please check your internet connection and try again.",

                            isUser =
                                false,

                            isError =
                                true
                        )
                    )

                    ChatHistoryRepository.saveChat(context, currentSessionId, messages)
                    savedChats = ChatHistoryRepository.getChats(context)
                }

            } catch (
                e: Exception
            ) {

                Log.e(
                    "MEDASSIST_API",
                    "API ERROR",
                    e
                )


                messages.add(

                    ChatMessage(

                        text =
                            "Unable to connect to MedAssist AI right now.\n\n" +
                                    "Please make sure the server is running and try again.",

                        isUser =
                            false,

                        isError =
                            true
                    )
                )

                ChatHistoryRepository.saveChat(context, currentSessionId, messages)
                savedChats = ChatHistoryRepository.getChats(context)

            } finally {

                isTyping = false
            }
        }
    }


    // =========================================================
    // MAIN UI — MEDASSIST TEAL THEME
    // =========================================================

    val screenBackground = Color(0xFFEFF8F7)
    val panelBackground = Color.White
    val primaryTeal = Color(0xFF176B83)
    val softTeal = Color(0xFFE4F3F4)
    val darkText = Color(0xFF17202A)
    val mutedText = Color(0xFF667085)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBackground)
    ) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            // -------------------------------------------------
            // HEADER
            // -------------------------------------------------
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp),
                color = primaryTeal,
                shape = RoundedCornerShape(
                    bottomStart = 28.dp,
                    bottomEnd = 28.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(Modifier.width(2.dp))

                    RobotAvatar(
                        size = 42.dp,
                        isError = false
                    )

                    Spacer(Modifier.width(10.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "MedAssist AI",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = if (isTyping) "Thinking..." else "AI health assistant",
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            savedChats = ChatHistoryRepository.getChats(context)
                            showChatSidebar = true
                        },
                        modifier = Modifier.size(44.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.14f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Open chat history",
                            tint = Color.White
                        )
                    }
                }
            }

            // -------------------------------------------------
            // CHAT PANEL
            // -------------------------------------------------
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                shape = RoundedCornerShape(30.dp),
                color = panelBackground,
                shadowElevation = 5.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(chatScrollState)
                            .padding(
                                start = 14.dp,
                                end = 14.dp,
                                top = 12.dp,
                                bottom = 96.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Spacer(Modifier.height(4.dp))

                        if (messages.isEmpty() && !isTyping) {
                            WelcomeSection(
                                primaryTeal = primaryTeal,
                                mutedText = mutedText
                            )
                        }

                        messages.forEach { chatMessage ->
                            MessageBubble(message = chatMessage)
                        }

                        if (isTyping) {
                            TypingIndicator()
                        }

                        Spacer(Modifier.height(4.dp))
                    }

                    // -----------------------------------------
                    // FLOATING COMPOSER
                    // -----------------------------------------
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .imePadding()
                            .navigationBarsPadding()
                            .padding(
                                horizontal = 12.dp,
                                vertical = 12.dp
                            ),
                        shape = RoundedCornerShape(28.dp),
                        color = Color.White,
                        shadowElevation = 8.dp,
                        border = BorderStroke(
                            1.dp,
                            Color(0xFFDCE8EB)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 6.dp,
                                    end = 6.dp,
                                    top = 6.dp,
                                    bottom = 6.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // Add button
                            IconButton(
                                onClick = { },
                                modifier = Modifier.size(44.dp),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = softTeal,
                                    contentColor = primaryTeal
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add",
                                    tint = primaryTeal
                                )
                            }

                            Spacer(Modifier.width(2.dp))

                            OutlinedTextField(
                                value = message,
                                onValueChange = { message = it },
                                modifier = Modifier.weight(1f),
                                placeholder = {
                                    Text(
                                        text = "Ask MedAssist...",
                                        color = Color(0xFF98A2B3),
                                        fontSize = 15.sp
                                    )
                                },
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Send
                                ),
                                keyboardActions = KeyboardActions(
                                    onSend = { sendMessage(message) }
                                ),
                                maxLines = 4,
                                shape = RoundedCornerShape(20.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = darkText,
                                    unfocusedTextColor = darkText,
                                    focusedPlaceholderColor = Color(0xFF98A2B3),
                                    unfocusedPlaceholderColor = Color(0xFF98A2B3),
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    cursorColor = primaryTeal
                                )
                            )

                            // Mic button — UI only, speech-to-text comes later.
                            IconButton(
                                onClick = { },
                                modifier = Modifier.size(44.dp),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = softTeal,
                                    contentColor = primaryTeal
                                )
                            ) {
                                Icon(
                                    imageVector = MicIcon,
                                    contentDescription = "Voice input",
                                    tint = primaryTeal
                                )
                            }

                            Spacer(Modifier.width(6.dp))

                            val canSend =
                                message.trim().isNotEmpty() && !isTyping

                            IconButton(
                                onClick = { sendMessage(message) },
                                enabled = canSend,
                                modifier = Modifier.size(44.dp),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = primaryTeal,
                                    contentColor = Color.White,
                                    disabledContainerColor = Color(0xFFE8EDF1),
                                    disabledContentColor = Color(0xFF98A2B3)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send"
                                )
                            }
                        }
                    }
                }
            }
        }


        // -----------------------------------------------------
        // SIDEBAR — FADE OVERLAY
        // -----------------------------------------------------
        AnimatedVisibility(
            visible = showChatSidebar,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(260))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.36f))
                    .clickable(
                        interactionSource = remember {
                            MutableInteractionSource()
                        },
                        indication = null,
                        onClick = { showChatSidebar = false }
                    )
            )
        }

        // -----------------------------------------------------
        // SIDEBAR — SLIDING PANEL
        // -----------------------------------------------------
        AnimatedVisibility(
            visible = showChatSidebar,
            enter = slideInHorizontally(
                animationSpec = tween(320, easing = FastOutSlowInEasing),
                initialOffsetX = { fullWidth -> -fullWidth }
            ) + fadeIn(animationSpec = tween(220)),
            exit = slideOutHorizontally(
                animationSpec = tween(280, easing = FastOutSlowInEasing),
                targetOffsetX = { fullWidth -> -fullWidth }
            ) + fadeOut(animationSpec = tween(220))
        ) {
            ChatSidebar(
                savedChats = savedChats,
                currentSessionId = currentSessionId,
                onClose = { showChatSidebar = false },
                onNewChat = {
                    ChatHistoryRepository.saveChat(
                        context,
                        currentSessionId,
                        messages
                    )

                    currentSessionId =
                        "android_" + System.currentTimeMillis()

                    messages.clear()
                    message = ""
                    isTyping = false
                    savedChats =
                        ChatHistoryRepository.getChats(context)
                    showChatSidebar = false
                },
                onSelectChat = { chat ->
                    currentSessionId = chat.id
                    messages.clear()
                    messages.addAll(chat.messages)
                    message = ""
                    isTyping = false
                    showChatSidebar = false
                },
                onClearCurrent = {
                    showChatSidebar = false
                    showClearDialog = true
                },
                onChatLongPress = { chat ->
                    selectedChatForActions = chat
                    showChatActions = true
                }
            )
        }


        // -----------------------------------------------------
        // COMPACT CHAT ACTIONS POPUP
        // -----------------------------------------------------
        if (showChatActions && selectedChatForActions != null) {
            val selectedChat = selectedChatForActions!!

            Dialog(
                onDismissRequest = {
                    showChatActions = false
                    selectedChatForActions = null
                }
            ) {
                Surface(
                    modifier = Modifier
                        .width(292.dp)
                        .shadow(18.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier.padding(
                            start = 8.dp,
                            top = 8.dp,
                            end = 8.dp,
                            bottom = 8.dp
                        )
                    ) {
                        Text(
                            text = selectedChat.title,
                            modifier = Modifier.padding(
                                start = 14.dp,
                                top = 8.dp,
                                end = 14.dp,
                                bottom = 8.dp
                            ),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF17202A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        ChatActionRow(
                            icon = PinIcon,
                            text = if (selectedChat.pinned) "Unpin Chat" else "Pin Chat",
                            onClick = {
                                ChatHistoryRepository.setPinned(
                                    context,
                                    selectedChat.id,
                                    !selectedChat.pinned
                                )
                                savedChats =
                                    ChatHistoryRepository.getChats(context)
                                showChatActions = false
                                selectedChatForActions = null
                            }
                        )

                        ChatActionRow(
                            icon = Icons.Filled.Edit,
                            text = "Rename Chat",
                            onClick = {
                                renameText = selectedChat.title
                                showChatActions = false
                                showRenameDialog = true
                            }
                        )

                        ChatActionRow(
                            icon = Icons.Filled.Share,
                            text = "Share Chat",
                            onClick = {
                                shareChat(
                                    context,
                                    selectedChat,
                                    "MedAssist AI Conversation"
                                )
                                showChatActions = false
                                selectedChatForActions = null
                            }
                        )

                        ChatActionRow(
                            icon = Icons.Filled.Delete,
                            text = "Delete Chat",
                            destructive = true,
                            onClick = {
                                ChatHistoryRepository.deleteChat(
                                    context,
                                    selectedChat.id
                                )
                                savedChats =
                                    ChatHistoryRepository.getChats(context)

                                if (selectedChat.id == currentSessionId) {
                                    messages.clear()
                                    message = ""
                                    isTyping = false
                                    currentSessionId =
                                        "android_" + System.currentTimeMillis()
                                }

                                showChatActions = false
                                selectedChatForActions = null
                            }
                        )
                    }
                }
            }
        }

        // -----------------------------------------------------
        // RENAME CHAT DIALOG
        // -----------------------------------------------------
        if (showRenameDialog && selectedChatForActions != null) {
            AlertDialog(
                onDismissRequest = {
                    showRenameDialog = false
                    selectedChatForActions = null
                },
                title = { Text("Rename Chat") },
                text = {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Chat name") }
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val chat = selectedChatForActions
                            if (chat != null && renameText.trim().isNotBlank()) {
                                ChatHistoryRepository.renameChat(
                                    context,
                                    chat.id,
                                    renameText
                                )
                                savedChats = ChatHistoryRepository.getChats(context)
                            }
                            showRenameDialog = false
                            selectedChatForActions = null
                        },
                        enabled = renameText.trim().isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryTeal
                        )
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showRenameDialog = false
                            selectedChatForActions = null
                        }
                    ) {
                        Text("Cancel", color = primaryTeal)
                    }
                }
            )
        }

        // -----------------------------------------------------
        // CLEAR CHAT DIALOG
        // -----------------------------------------------------
        if (showClearDialog) {

            AlertDialog(

                onDismissRequest = {
                    showClearDialog = false
                },

                title = {
                    Text("Clear conversation?")
                },

                text = {
                    Text("This will clear the current conversation from the screen.")
                },

                confirmButton = {
                    Button(
                        onClick = {

                            ChatHistoryRepository.deleteChat(
                                context,
                                currentSessionId
                            )

                            messages.clear()
                            message = ""
                            isTyping = false

                            savedChats =
                                ChatHistoryRepository.getChats(context)

                            showClearDialog = false
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryTeal
                        )
                    ) {
                        Text("Clear")
                    }
                },

                dismissButton = {
                    TextButton(
                        onClick = {
                            showClearDialog = false
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            color = primaryTeal
                        )
                    }
                }
            )
        }
    }
}


// =============================================================
// MIC ICON (self-contained, no material-icons-extended needed)
// =============================================================

private val MicIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "MicIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = addPathNodes(
            "M12,14c1.66,0 2.99,-1.34 2.99,-3L15,5c0,-1.66 -1.34,-3 -3,-3S9,3.34 9,5v6c0,1.66 1.34,3 3,3z" +
                    "M17.3,11c0,3 -2.54,5.1 -5.3,5.1S6.7,14 6.7,11L5,11c0,3.41 2.72,6.23 6,6.72L11,21h2v-3.28" +
                    "c3.28,-0.48 6,-3.3 6,-6.72h-1.7z"
        ),
        fill = SolidColor(Color.Black)
    ).build()
}


// =============================================================
// WELCOME SECTION (fade + slide entrance)
// =============================================================

@Composable
private fun WelcomeSection(
    primaryTeal: Color,
    mutedText: Color
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(650)) +
                slideInVertically(
                    animationSpec = tween(650, easing = FastOutSlowInEasing),
                    initialOffsetY = { fullHeight -> fullHeight / 6 }
                )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 52.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RobotAvatar(
                size = 112.dp,
                isError = false
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "How can I help",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = primaryTeal,
                textAlign = TextAlign.Center
            )

            Text(
                text = "you today?",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = primaryTeal,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Ask about symptoms, medicines or your health.",
                fontSize = 13.sp,
                color = mutedText,
                textAlign = TextAlign.Center
            )
        }
    }
}


// =============================================================
// CHAT HISTORY SIDEBAR CONTENT
// =============================================================

@Composable
private fun ChatSidebar(
    savedChats: List<SavedChat>,
    currentSessionId: String,
    onClose: () -> Unit,
    onNewChat: () -> Unit,
    onSelectChat: (SavedChat) -> Unit,
    onClearCurrent: () -> Unit,
    onChatLongPress: (SavedChat) -> Unit
) {
    val primaryTeal = Color(0xFF176B83)
    val panelShape = RoundedCornerShape(
        topEnd = 28.dp,
        bottomEnd = 28.dp
    )

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(310.dp)
            .shadow(12.dp, panelShape)
            .background(Color.White, panelShape)
            .clickable(
                interactionSource = remember {
                    MutableInteractionSource()
                },
                indication = null,
                onClick = { }
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(18.dp)
    ) {

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RobotAvatar(size = 42.dp, isError = false)

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "MedAssist AI",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF17202A)
                )
                Text(
                    "Your conversations",
                    fontSize = 12.sp,
                    color = Color(0xFF667085)
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color(0xFFF2F5F7),
                    contentColor = Color(0xFF667085)
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF667085)
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // New chat
        Button(
            onClick = onNewChat,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryTeal,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "New Chat",
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            "Older Chats",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF667085)
        )

        Spacer(Modifier.height(8.dp))

        if (savedChats.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 35.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RobotAvatar(size = 64.dp, isError = false)
                Spacer(Modifier.height(12.dp))
                Text(
                    "No older chats yet",
                    fontSize = 14.sp,
                    color = Color(0xFF667085)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val pinnedChats = savedChats.filter { it.pinned }
                val regularChats = savedChats.filterNot { it.pinned }

                if (pinnedChats.isNotEmpty()) {
                    ChatGroupLabel("Pinned")
                    pinnedChats.forEach { chat ->
                        ChatHistoryItem(
                            chat = chat,
                            isCurrent = chat.id == currentSessionId,
                            onClick = { onSelectChat(chat) },
                            onLongPress = { onChatLongPress(chat) }
                        )
                    }
                    if (regularChats.isNotEmpty()) Spacer(Modifier.height(8.dp))
                }

                var lastGroup = ""
                regularChats.forEach { chat ->
                    val group = chatGroupLabel(chat.updatedAt)
                    if (group != lastGroup) {
                        ChatGroupLabel(group)
                        lastGroup = group
                    }

                    ChatHistoryItem(
                        chat = chat,
                        isCurrent = chat.id == currentSessionId,
                        onClick = { onSelectChat(chat) },
                        onLongPress = { onChatLongPress(chat) }
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        TextButton(
            onClick = onClearCurrent,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = Color(0xFFD32F2F),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Clear Current Chat",
                color = Color(0xFFD32F2F),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// =============================================================
// CHAT HISTORY HELPERS
// =============================================================

private fun chatRelativeTime(updatedAt: Long, now: Long = System.currentTimeMillis()): String {
    if (updatedAt <= 0L) return ""

    val diff = (now - updatedAt).coerceAtLeast(0L)
    val minute = 60_000L
    val hour = 60 * minute
    val day = 24 * hour

    return when {
        diff < minute -> "Just now"
        diff < hour -> "${diff / minute} min ago"
        diff < day -> "${diff / hour} hrs ago"
        diff < 2 * day -> "Yesterday"
        diff < 7 * day -> "${diff / day} days ago"
        diff < 14 * day -> "1 week ago"
        diff < 30 * day -> "${diff / (7 * day)} weeks ago"
        diff < 60 * day -> "1 month ago"
        else -> "${diff / (30 * day)} months ago"
    }
}

private fun chatGroupLabel(updatedAt: Long, now: Long = System.currentTimeMillis()): String {
    val diff = (now - updatedAt).coerceAtLeast(0L)
    val day = 24 * 60 * 60 * 1000L
    return when {
        diff < day -> "Today"
        diff < 2 * day -> "Yesterday"
        diff < 7 * day -> "Previous 7 Days"
        diff < 30 * day -> "Previous 30 Days"
        else -> "Older"
    }
}

private fun buildChatShareText(chat: SavedChat): String {
    val builder = StringBuilder()
    builder.append("MedAssist AI Chat\n")
    builder.append("${chat.title}\n")
    builder.append("${chatRelativeTime(chat.updatedAt)}\n")
    builder.append("------------------------------\n\n")

    chat.messages.forEach { message ->
        builder.append(if (message.isUser) "You" else "MedAssist AI")
        builder.append(":\n")
        builder.append(message.text.trim())
        builder.append("\n\n")
    }

    return builder.toString().trim()
}

private fun shareChat(context: Context, chat: SavedChat, chooserTitle: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, chat.title)
        putExtra(Intent.EXTRA_TEXT, buildChatShareText(chat))
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

@Composable
private fun ChatGroupLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp, start = 2.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF667085)
    )
}

private val PinIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "PinIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = addPathNodes(
            "M16,3L8,3L8,5L10,5L10,10L7,13L7,15L11,15L11,21L13,21L13,15L17,15L17,13L14,10L14,5L16,5Z"
        ),
        fill = SolidColor(Color.Black)
    ).build()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatHistoryItem(
    chat: SavedChat,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val rowShape = RoundedCornerShape(15.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp)
            .clip(rowShape)
            .background(
                if (isCurrent) Color(0xFFE4F3F4) else Color(0xFFF7F9FA)
            )
            .border(
                width = 1.dp,
                color = if (isCurrent) Color(0xFFB9DDE2) else Color(0xFFE6EAED),
                shape = rowShape
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RobotAvatar(size = 34.dp, isError = false)
        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (chat.pinned) {
                    Icon(
                        imageVector = PinIcon,
                        contentDescription = "Pinned",
                        tint = Color(0xFF176B83),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = chat.title,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFF17202A),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = chatRelativeTime(chat.updatedAt),
                fontSize = 11.sp,
                color = Color(0xFF98A2B3),
                maxLines = 1
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open chat",
            tint = Color(0xFF98A2B3),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ChatActionRow(
    icon: ImageVector,
    text: String,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (destructive) Color(0xFFD32F2F) else Color(0xFF176B83),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = if (destructive) Color(0xFFD32F2F) else Color(0xFF17202A),
            textAlign = TextAlign.Start
        )
    }
}

// =============================================================
// FIX MEDICAL MEASUREMENT DISPLAY
// =============================================================

private fun fixMeasurementFormatting(

    answer: String,

    measurements:
    List<com.example.ai_based_medical_chatbot.data.api.Measurement>

): String {

    var fixedAnswer =
        answer


    for (
    measurement
    in measurements
    ) {

        val name =
            measurement.name.trim()

        val value =
            measurement.value.trim()

        val unit =
            measurement.unit.trim()


        if (
            name.isBlank() ||
            value.isBlank()
        ) {

            continue
        }


        /*
         * Avoid changing values which are already
         * correctly formatted.
         */

        if (
            !value.contains(".")
        ) {

            continue
        }


        val escapedName =
            Regex.escape(
                name.replace(
                    "_",
                    " "
                )
            )


        val unitPart =

            if (
                unit.isNotBlank()
            ) {

                "\\s*" +
                        Regex.escape(
                            unit
                        )

            } else {

                ""
            }


        val pattern =
            Regex(

                "(?i)" +
                        "(\\b" +
                        escapedName +
                        "\\b\\s*[:=]?\\s*)" +
                        "\\d+(?:\\.\\d+)?" +
                        unitPart
            )


        fixedAnswer =
            pattern.replace(
                fixedAnswer
            ) { match ->

                val prefix =
                    match.groupValues[1]


                if (
                    unit.isNotBlank()
                ) {

                    "$prefix$value $unit"

                } else {

                    "$prefix$value"
                }
            }
    }


    return fixedAnswer
}


// =============================================================
// MEDASSIST ROBOT AVATAR
// =============================================================

@Composable
private fun RobotAvatar(
    size: androidx.compose.ui.unit.Dp,
    isError: Boolean
) {
    val backgroundColor =
        if (isError) Color(0xFFFFE8EC) else Color(0xFFE8F5F8)

    val robotColor =
        if (isError) Color(0xFFD32F2F) else Color(0xFF176B83)

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = if (isError) Color(0xFFFFC7D0) else Color(0xFFD5EBF0),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        // Antenna
        Box(
            modifier = Modifier
                .size(size * 0.10f)
                .clip(CircleShape)
                .background(robotColor)
                .align(Alignment.TopCenter)
                .padding(top = size * 0.08f)
        )

        // Robot head
        Box(
            modifier = Modifier
                .size(size * 0.62f, size * 0.50f)
                .clip(RoundedCornerShape(size * 0.14f))
                .background(robotColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = size * 0.13f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(size * 0.095f)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Box(
                    Modifier
                        .size(size * 0.095f)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}

// =============================================================
// MESSAGE BUBBLE
// =============================================================

@Composable
fun MessageBubble(

    message: ChatMessage

) {

    val bubbleShape =
        if (message.isUser) {
            RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = 20.dp,
                bottomEnd = 6.dp
            )
        } else {
            RoundedCornerShape(
                topStart = 6.dp,
                topEnd = 20.dp,
                bottomStart = 20.dp,
                bottomEnd = 20.dp
            )
        }

    val bubbleColor =
        when {
            message.isError -> Color(0xFFFFEBEE)
            message.isUser -> Color(0xFF176B83)
            else -> Color(0xFFF3F8F9)
        }

    val bubbleBorder =
        when {
            message.isError -> BorderStroke(1.dp, Color(0xFFFFCDD2))
            message.isUser -> null
            else -> BorderStroke(1.dp, Color(0xFFE1ECEF))
        }

    Row(

        modifier =
            Modifier.fillMaxWidth(),

        horizontalArrangement =
            if (message.isUser) {
                Arrangement.End
            } else {
                Arrangement.Start
            },

        verticalAlignment =
            Alignment.Bottom
    ) {

        // AI icon
        if (!message.isUser) {

            RobotAvatar(
                size = 36.dp,
                isError = message.isError
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )
        }

        Column(

            modifier =
                Modifier.fillMaxWidth(0.84f),

            horizontalAlignment =
                if (message.isUser) {
                    Alignment.End
                } else {
                    Alignment.Start
                }
        ) {

            Surface(

                shape = bubbleShape,

                color = bubbleColor,

                border = bubbleBorder,

                shadowElevation =
                    if (message.isUser) 2.dp else 0.dp
            ) {

                Column(
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    )
                ) {

                    FormattedMessageText(
                        text = message.text,
                        isUser = message.isUser
                    )
                }
            }

            // AI response meta
            if (
                !message.isUser &&
                !message.isError
            ) {

                Row(
                    modifier = Modifier.padding(
                        start = 6.dp,
                        top = 4.dp
                    ),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    if (message.intent.isNotBlank()) {

                        Text(
                            text =
                                message.intent
                                    .replace("_", " ")
                                    .replaceFirstChar {
                                        it.uppercase()
                                    },
                            fontSize = 10.sp,
                            color = Color(0xFF98A2B3)
                        )
                    }
                }
            }
        }
    }
}


// =============================================================
// FORMATTED MESSAGE
// =============================================================

@Composable
private fun FormattedMessageText(

    text: String,

    isUser: Boolean

) {

    val textColor =

        if (
            isUser
        )

            Color.White

        else

            Color(
                0xFF344054
            )


    val lines =
        text
            .replace(
                "\r\n",
                "\n"
            )
            .split(
                "\n"
            )


    Column(

        verticalArrangement =
            Arrangement.spacedBy(
                6.dp
            )
    ) {

        lines.forEach { rawLine ->

            val line =
                rawLine.trimEnd()


            // =================================================
            // EMPTY LINE
            // =================================================

            if (
                line.isBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            2.dp
                        )
                )

                return@forEach
            }


            // =================================================
            // BULLET
            // =================================================

            val isBullet =

                line.startsWith(
                    "•"
                ) ||

                        line.startsWith(
                            "- "
                        ) ||

                        line.startsWith(
                            "* "
                        )


            if (
                isBullet
            ) {

                val bulletText =

                    when {

                        line.startsWith(
                            "•"
                        ) ->

                            line.removePrefix(
                                "•"
                            ).trim()

                        else ->

                            line.substring(
                                2
                            ).trim()
                    }


                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.Top
                ) {

                    Text(

                        text =
                            "•",

                        fontSize =
                            15.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            textColor
                    )


                    Spacer(

                        modifier =
                            Modifier.width(
                                7.dp
                            )
                    )


                    Text(

                        text =
                            cleanMarkdown(
                                bulletText
                            ),

                        fontSize =
                            15.sp,

                        lineHeight =
                            22.sp,

                        color =
                            textColor,

                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )
                }

            } else {


                // =================================================
                // HEADING / NORMAL TEXT
                // =================================================

                val cleaned =
                    cleanMarkdown(
                        line
                    )


                val isHeading =

                    line.startsWith(
                        "###"
                    ) ||

                            line.startsWith(
                                "##"
                            ) ||

                            line.startsWith(
                                "# "
                            ) ||

                            line.endsWith(
                                ":"
                            ) &&
                            line.length < 80


                Text(

                    text =
                        cleaned,

                    fontSize =

                        if (
                            isHeading
                        )

                            16.sp

                        else

                            15.sp,

                    lineHeight =
                        23.sp,

                    fontWeight =

                        if (
                            isHeading
                        )

                            FontWeight.SemiBold

                        else

                            FontWeight.Normal,

                    color =
                        textColor
                )
            }
        }
    }
}


// =============================================================
// CLEAN MARKDOWN
// =============================================================

private fun cleanMarkdown(
    text: String
): String {

    return text

        // Bold
        .replace(
            Regex(
                "\\*\\*(.*?)\\*\\*"
            ),
            "$1"
        )

        // Italic
        .replace(
            Regex(
                "(?<!\\*)\\*(.*?)\\*(?!\\*)"
            ),
            "$1"
        )

        // Markdown headings
        .replace(
            Regex(
                "^#{1,6}\\s*"
            ),
            ""
        )

        // Inline code
        .replace(
            "`",
            ""
        )

        .trim()
}



// =============================================================
// TYPING INDICATOR
// =============================================================

@Composable
fun TypingIndicator() {

    Row(

        modifier =
            Modifier.fillMaxWidth(),

        verticalAlignment =
            Alignment.Bottom
    ) {

        RobotAvatar(
            size = 36.dp,
            isError = false
        )

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        Surface(

            shape = RoundedCornerShape(
                topStart = 6.dp,
                topEnd = 20.dp,
                bottomStart = 20.dp,
                bottomEnd = 20.dp
            ),

            color = Color(0xFFF3F8F9),

            border = BorderStroke(1.dp, Color(0xFFE1ECEF))
        ) {

            Row(

                modifier = Modifier.padding(
                    horizontal = 18.dp,
                    vertical = 15.dp
                ),

                horizontalArrangement =
                    Arrangement.spacedBy(6.dp),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                TypingDot(delayMillis = 0)

                TypingDot(delayMillis = 160)

                TypingDot(delayMillis = 320)
            }
        }
    }
}


// =============================================================
// TYPING DOT
// =============================================================

@Composable
private fun TypingDot(
    delayMillis: Int
) {

    val transition =
        rememberInfiniteTransition(label = "typing_dot")

    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 520,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(delayMillis)
        ),
        label = "typing_progress"
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .graphicsLayer {
                val scale = 0.75f + 0.35f * progress
                scaleX = scale
                scaleY = scale
                translationY = -3.dp.toPx() * progress
                alpha = 0.35f + 0.65f * progress
            }
            .clip(CircleShape)
            .background(Color(0xFF176B83))
    )
}