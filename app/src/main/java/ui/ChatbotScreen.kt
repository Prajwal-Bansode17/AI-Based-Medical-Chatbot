package ui

import com.example.ai_based_medical_chatbot.LocalAppLanguageController

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import android.net.Uri
import android.Manifest
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

import androidx.activity.compose.BackHandler
import com.example.ai_based_medical_chatbot.ui.theme.PrimaryTeal
import com.example.ai_based_medical_chatbot.ui.theme.PrimaryTealDark
import com.example.ai_based_medical_chatbot.ui.theme.DeepTeal
import com.example.ai_based_medical_chatbot.ui.theme.DarkTeal
import com.example.ai_based_medical_chatbot.ui.theme.SageGreen
import com.example.ai_based_medical_chatbot.ui.theme.Ivory
import com.example.ai_based_medical_chatbot.ui.theme.Mint
import com.example.ai_based_medical_chatbot.ui.theme.SoftMint
import com.example.ai_based_medical_chatbot.ui.theme.CoralAccent
import com.example.ai_based_medical_chatbot.ui.theme.PureWhite
import com.example.ai_based_medical_chatbot.ui.theme.LightGreenSurface
import com.example.ai_based_medical_chatbot.ui.theme.LightGreenSoft
import com.example.ai_based_medical_chatbot.ui.theme.LightGreenBorder
import com.example.ai_based_medical_chatbot.ui.theme.AppBackground
import com.example.ai_based_medical_chatbot.ui.theme.AppSurface
import com.example.ai_based_medical_chatbot.ui.theme.TextDark
import com.example.ai_based_medical_chatbot.ui.theme.TextMuted
import com.example.ai_based_medical_chatbot.ui.theme.TextLight
import com.example.ai_based_medical_chatbot.ui.theme.StatusError
import com.example.ai_based_medical_chatbot.ui.theme.StatusErrorBackground
import com.example.ai_based_medical_chatbot.ui.theme.StatusErrorBorder
import com.example.ai_based_medical_chatbot.ui.theme.MedAssistSpacing

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
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import java.util.Locale
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.ai_based_medical_chatbot.R
import com.example.ai_based_medical_chatbot.appText
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

import com.example.ai_based_medical_chatbot.data.api.PredictionRequest
import com.example.ai_based_medical_chatbot.data.api.RetrofitClient

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    val isError: Boolean = false,

    val imagePaths: List<String> = emptyList()
)

internal fun resolveChatbotPrompt(text: String, hasImages: Boolean): String {
    val clean = text.trim()
    return when {
        clean.isNotEmpty() -> clean
        hasImages -> "Please analyze the attached medical prescription or clinical report image."
        else -> "Please assess my health query."
    }
}

internal fun isChatbotSendEnabled(text: String, hasImages: Boolean, isTyping: Boolean): Boolean {
    return (text.trim().isNotEmpty() || hasImages) && !isTyping
}

internal fun shouldShowChatbotWelcome(messagesEmpty: Boolean, isTyping: Boolean): Boolean {
    return messagesEmpty && !isTyping
}


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
    private const val LEGACY_KEY_CHATS = "saved_chats"
    private const val LEGACY_KEY_ACTIVE = "active_session_id"

    private const val KEY_CHATS_PREFIX = "saved_chats_"
    private const val KEY_ACTIVE_PREFIX = "active_session_id_"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getAccountKey(context: Context, accountOverride: String? = null): String {
        if (!accountOverride.isNullOrBlank()) {
            return "account_${accountOverride.trim()}"
        }
        val user = com.example.ai_based_medical_chatbot.data.SupabaseClient.getSavedUser(context)
        return if (user != null && user.id.isNotBlank()) {
            "account_${user.id.trim()}"
        } else {
            "account_guest"
        }
    }

    private fun chatsKey(context: Context, accountOverride: String? = null): String =
        KEY_CHATS_PREFIX + getAccountKey(context, accountOverride)

    private fun activeSessionKey(context: Context, accountOverride: String? = null): String =
        KEY_ACTIVE_PREFIX + getAccountKey(context, accountOverride)

    private fun migrateLegacyIfNeeded(context: Context, accountOverride: String? = null) {
        val sp = prefs(context)
        val legacyChats = sp.getString(LEGACY_KEY_CHATS, null)
        val targetKey = chatsKey(context, accountOverride)
        if (!legacyChats.isNullOrBlank() && !sp.contains(targetKey)) {
            val legacyActive = sp.getString(LEGACY_KEY_ACTIVE, null)
            val editor = sp.edit()
            editor.putString(targetKey, legacyChats)
            if (!legacyActive.isNullOrBlank()) {
                editor.putString(activeSessionKey(context, accountOverride), legacyActive)
            }
            editor.remove(LEGACY_KEY_CHATS)
            editor.remove(LEGACY_KEY_ACTIVE)
            editor.apply()
        }
    }

    fun getActiveSessionId(context: Context, accountOverride: String? = null): String? {
        migrateLegacyIfNeeded(context, accountOverride)
        return prefs(context).getString(activeSessionKey(context, accountOverride), null)
    }

    fun setActiveSessionId(context: Context, sessionId: String?, accountOverride: String? = null) {
        val editor = prefs(context).edit()
        val key = activeSessionKey(context, accountOverride)
        if (sessionId.isNullOrBlank()) {
            editor.remove(key)
        } else {
            editor.putString(key, sessionId.trim())
        }
        editor.apply()
    }

    fun clearActiveSession(context: Context, accountOverride: String? = null) {
        prefs(context).edit().remove(activeSessionKey(context, accountOverride)).apply()
    }

    fun getChats(context: Context, accountOverride: String? = null): List<SavedChat> {
        migrateLegacyIfNeeded(context, accountOverride)
        return try {
            val array = JSONArray(
                prefs(context).getString(chatsKey(context, accountOverride), "[]") ?: "[]"
            )
            val result = mutableListOf<SavedChat>()

            for (i in 0 until array.length()) {
                val chat = array.optJSONObject(i) ?: continue
                val arr = chat.optJSONArray("messages") ?: JSONArray()
                val list = mutableListOf<ChatMessage>()

                for (j in 0 until arr.length()) {
                    val item = arr.optJSONObject(j) ?: continue
                    val imagePathsArray = item.optJSONArray("imagePaths") ?: JSONArray()
                    val imagePaths = mutableListOf<String>()
                    for (k in 0 until imagePathsArray.length()) {
                        val path = imagePathsArray.optString(k)
                        if (path.isNotBlank()) imagePaths.add(path)
                    }

                    list.add(
                        ChatMessage(
                            text = item.optString("text"),
                            isUser = item.optBoolean("isUser"),
                            intent = item.optString("intent"),
                            confidence = item.optDouble("confidence", 0.0),
                            similarity = item.optDouble("similarity", 0.0),
                            isError = item.optBoolean("isError"),
                            imagePaths = imagePaths
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

    fun saveChat(context: Context, sessionId: String, messages: List<ChatMessage>, accountOverride: String? = null) {
        if (messages.isEmpty()) return

        val existing = getChats(context, accountOverride).firstOrNull { it.id == sessionId }
        val all = getChats(context, accountOverride)
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

        writeChats(context, all, accountOverride)
    }

    fun renameChat(context: Context, sessionId: String, newTitle: String, accountOverride: String? = null) {
        val cleanTitle = newTitle.trim()
        if (cleanTitle.isBlank()) return

        val updated = getChats(context, accountOverride).map { chat ->
            if (chat.id == sessionId) {
                chat.copy(
                    title = cleanTitle.take(60),
                    isTitleCustom = true
                )
            } else {
                chat
            }
        }
        writeChats(context, updated, accountOverride)
    }

    fun setPinned(context: Context, sessionId: String, pinned: Boolean, accountOverride: String? = null) {
        val updated = getChats(context, accountOverride).map { chat ->
            if (chat.id == sessionId) chat.copy(pinned = pinned) else chat
        }
        writeChats(context, updated, accountOverride)
    }

    fun deleteChat(context: Context, sessionId: String, accountOverride: String? = null) {
        val updated = getChats(context, accountOverride).filter { it.id != sessionId }
        writeChats(context, updated, accountOverride)
        if (getActiveSessionId(context, accountOverride) == sessionId) {
            val nextActive = updated.firstOrNull()?.id
            setActiveSessionId(context, nextActive, accountOverride)
        }
    }

    private fun writeChats(context: Context, chats: List<SavedChat>, accountOverride: String? = null) {
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
                            .put(
                                "imagePaths",
                                JSONArray().apply {
                                    item.imagePaths.forEach { path -> put(path) }
                                }
                            )
                    )
                }
                obj.put("messages", arr)
                array.put(obj)
            }

        prefs(context).edit()
            .putString(chatsKey(context, accountOverride), array.toString())
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

    initialUserQuery: String? = null,

    onViewTranscript: ((String?) -> Unit)? = null,

    onBack: () -> Unit

) {

    val appLanguageController = LocalAppLanguageController.current
    val selectedLanguage = appLanguageController.selectedLanguage
    fun t(key: String): String = appText(key, selectedLanguage)



    val context =
        LocalContext.current

    val keyboardController =
        LocalSoftwareKeyboardController.current


    // =========================================================
    // SESSION
    // =========================================================

    val initialResolvedSessionId = remember(initialSessionId) {
        if (!initialSessionId.isNullOrBlank()) {
            initialSessionId.trim()
        } else {
            val fresh = "android_" + System.currentTimeMillis()
            ChatHistoryRepository.setActiveSessionId(context, fresh)
            fresh
        }
    }

    var currentSessionId by remember {
        mutableStateOf(initialResolvedSessionId)
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
    var editingMessageIndex by remember { mutableStateOf<Int?>(null) }
    var editingDraft by remember { mutableStateOf("") }
    var showEditMessageDialog by remember { mutableStateOf(false) }
    var viewerImagePath by remember { mutableStateOf<String?>(null) }

    // =========================================================
    // ATTACHMENT / PLUS MENU
    // =========================================================

    var showAttachmentMenu by remember { mutableStateOf(false) }
    var attachmentLabel by remember { mutableStateOf("") }
    val selectedImages = remember { mutableStateListOf<Bitmap>() }
    var pendingImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    LaunchedEffect(pendingImageUris) {
        val uris = pendingImageUris
        if (uris.isEmpty()) return@LaunchedEffect

        val decoded = withContext(Dispatchers.IO) {
            uris.mapNotNull { uri ->
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }

        if (decoded.isNotEmpty()) {
            selectedImages.addAll(decoded)
            attachmentLabel = ""
        }

        pendingImageUris = emptyList()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            pendingImageUris = uris
            attachmentLabel = ""
        }
    }

    val reportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            attachmentLabel = "Report attached"
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            selectedImages.add(bitmap)
            attachmentLabel = ""
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                Log.e("MEDASSIST_CAMERA", "Unable to open camera", e)
            }
        } else {
            attachmentLabel = "Camera permission required"
        }
    }

    fun persistSelectedImages(): List<String> {
        if (selectedImages.isEmpty()) return emptyList()

        val paths = mutableListOf<String>()

        selectedImages.forEachIndexed { index, bitmap ->
            try {
                val file = java.io.File(
                    context.filesDir,
                    "medassist_${currentSessionId}_${System.currentTimeMillis()}_$index.jpg"
                )

                file.outputStream().use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)
                }

                paths.add(file.absolutePath)
            } catch (e: Exception) {
                Log.e("MEDASSIST_IMAGE", "Unable to save attached image", e)
            }
        }

        return paths
    }

    val messages =
        remember {

            mutableStateListOf<ChatMessage>()
        }


    val scope =
        rememberCoroutineScope()


    val chatScrollState =
        rememberScrollState()
    val isDragged by chatScrollState.interactionSource.collectIsDraggedAsState()
    var userScrolledUp by remember { mutableStateOf(false) }

    var currentlySpeakingText by remember { mutableStateOf<String?>(null) }
    val voiceAssistant = remember(context) {
        VoiceAssistant(context) { isSpeaking ->
            if (!isSpeaking) {
                currentlySpeakingText = null
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceAssistant.shutdown()
        }
    }

    fun startNewChat() {
        voiceAssistant.stop()
        currentlySpeakingText = null
        if (messages.isNotEmpty()) {
            ChatHistoryRepository.saveChat(context, currentSessionId, messages)
        }
        val newId = "android_" + System.currentTimeMillis()
        currentSessionId = newId
        ChatHistoryRepository.setActiveSessionId(context, newId)
        messages.clear()
        message = ""
        isTyping = false
        userScrolledUp = false
        selectedImages.clear()
        attachmentLabel = ""
        pendingImageUris = emptyList()
        editingMessageIndex = null
        savedChats = ChatHistoryRepository.getChats(context)
        showChatSidebar = false
    }

    fun handleBack() {
        voiceAssistant.stop()
        currentlySpeakingText = null
        if (showChatSidebar) {
            showChatSidebar = false
        } else if (showAttachmentMenu) {
            showAttachmentMenu = false
        } else {
            if (messages.isNotEmpty()) {
                ChatHistoryRepository.saveChat(context, currentSessionId, messages)
                ChatHistoryRepository.setActiveSessionId(context, currentSessionId)
            }
            onBack()
        }
    }

    BackHandler(enabled = true) {
        handleBack()
    }

    LaunchedEffect(currentSessionId) {
        ChatHistoryRepository.setActiveSessionId(context, currentSessionId)
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
    // Tracks when the user is already near bottom to auto-scroll
    // smoothly when messages arrive or content grows, while
    // avoiding pulling the user away if they scrolled up to read.
    // =========================================================



    // ONLY detect user scrolling when the user is actively dragging the screen with their finger!
    // Programmatic scrolling (animateScrollTo, scrollTo) never emits DragInteraction,
    // completely eliminating false triggers where userScrolledUp was set to true on message arrival.
    LaunchedEffect(isDragged, chatScrollState.value, chatScrollState.maxValue) {
        val distanceToBottom = chatScrollState.maxValue - chatScrollState.value
        if (isDragged) {
            if (distanceToBottom > 80) {
                userScrolledUp = true
            } else if (distanceToBottom <= 20) {
                userScrolledUp = false
            }
        } else {
            // When drag finishes or during natural settling, if user reaches the bottom, re-enable auto-follow
            if (distanceToBottom <= 20) {
                userScrolledUp = false
            }
        }
    }

    // Follow layout height growth (e.g. streaming responses, keyboard opening, message rendering)
    // without yanking the user if they deliberately scrolled up to read history.
    LaunchedEffect(Unit) {
        snapshotFlow { chatScrollState.maxValue }
            .collect { targetMax ->
                if (!userScrolledUp && targetMax > 0) {
                    val currentDist = targetMax - chatScrollState.value
                    if (currentDist > 0 && currentDist <= 300) {
                        chatScrollState.scrollTo(targetMax)
                    }
                }
            }
    }

    // Auto-scroll on new message added or typing state change
    LaunchedEffect(messages.size, isTyping) {
        if (!userScrolledUp) {
            delay(60)
            if (chatScrollState.maxValue > 0) {
                chatScrollState.animateScrollTo(chatScrollState.maxValue)
            }
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
            (cleanText.isEmpty() && selectedImages.isEmpty()) ||
            isTyping
        ) {

            return
        }


        // =====================================================
        // USER MESSAGE
        // =====================================================

        val attachedImagePaths = persistSelectedImages()
        val editIndex = editingMessageIndex

        if (editIndex != null && editIndex in messages.indices) {
            while (messages.size > editIndex) {
                messages.removeAt(messages.lastIndex)
            }
        }

        messages.add(
            ChatMessage(
                text = cleanText,
                isUser = true,
                imagePaths = attachedImagePaths
            )
        )

        editingMessageIndex = null
        userScrolledUp = false
        voiceAssistant.stop()
        currentlySpeakingText = null

        ChatHistoryRepository.saveChat(context, currentSessionId, messages)
        savedChats = ChatHistoryRepository.getChats(context)

        message = ""
        attachmentLabel = ""
        selectedImages.clear()
        pendingImageUris = emptyList()


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

                        val queryPrompt = resolveChatbotPrompt(cleanText, attachedImagePaths.isNotEmpty())
                        val chatbotText =
                            if (prescriptionMemory.isBlank()) {
                                queryPrompt
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
                                $queryPrompt
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
                    // AI MESSAGE (with deduplication)
                    // =================================================

                    val isDuplicateResponse = messages.lastOrNull()?.let { last ->
                        !last.isUser && last.text.trim() == finalAnswer.trim()
                    } ?: false

                    if (!isDuplicateResponse) {
                        messages.add(
                            ChatMessage(
                                text = finalAnswer,
                                isUser = false,
                                intent = result.intent,
                                confidence = result.confidence,
                                similarity = result.answerSimilarity
                            )
                        )
                    }

                    val requestSessionId = currentSessionId

                    // Adopt server-returned session ID if valid and non-blank.
                    // If the server assigned a new canonical ID (e.g. from an empty or auto-generated session),
                    // migrate the existing chat history under the adopted ID.
                    val returnedSessionId = result.sessionId?.trim()
                    val targetSessionId = if (!returnedSessionId.isNullOrBlank() && currentSessionId == requestSessionId) {
                        if (returnedSessionId != currentSessionId) {
                            val previousSessionId = currentSessionId
                            currentSessionId = returnedSessionId
                            ChatHistoryRepository.setActiveSessionId(context, returnedSessionId)
                            ChatHistoryRepository.deleteChat(context, previousSessionId)
                        }
                        returnedSessionId
                    } else {
                        currentSessionId
                    }

                    ChatHistoryRepository.saveChat(context, targetSessionId, messages)
                    savedChats = ChatHistoryRepository.getChats(context)

                } else {

                    // =============================================
                    // API ERROR
                    // =============================================

                    val code = response.code()
                    messages.add(
                        ChatMessage(
                            text = "Server returned error (HTTP $code). Please verify the backend logs and tap Retry.",
                            isUser = false,
                            isError = true
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

                val errText = when (e) {
                    is java.net.SocketTimeoutException ->
                        "The connection to MedAssist AI timed out. The medical model may still be generating or the server is busy. Tap Retry to try again."
                    is java.net.ConnectException ->
                        "Unable to connect to the MedAssist server at ${RetrofitClient.getBaseUrl(context)}.\n\nPlease verify that Flask is running and that your phone and computer are on the same Wi-Fi network."
                    is java.net.UnknownHostException ->
                        "Cannot resolve the MedAssist server address (${RetrofitClient.getBaseUrl(context)}).\n\nPlease check your network connection and server configuration."
                    else ->
                        "Unable to connect to MedAssist AI (${e.localizedMessage ?: "Network error"}).\n\nPlease check your server connection and try again."
                }

                messages.add(
                    ChatMessage(
                        text = errText,
                        isUser = false,
                        isError = true
                    )
                )

                ChatHistoryRepository.saveChat(context, currentSessionId, messages)
                savedChats = ChatHistoryRepository.getChats(context)

            } finally {

                isTyping = false
            }
        }
    }

    // Process initial query passed from Dashboard symptom card
    var initialQueryConsumed by remember { mutableStateOf(false) }
    LaunchedEffect(initialUserQuery) {
        if (!initialUserQuery.isNullOrBlank() && !initialQueryConsumed) {
            initialQueryConsumed = true
            sendMessage(initialUserQuery.trim())
        }
    }

    // =========================================================
    // RETRY FAILED MESSAGE
    // =========================================================

    fun retryMessage(failedIndex: Int? = null) {
        if (isTyping) return
        userScrolledUp = false

        val errorIdx = failedIndex ?: messages.indexOfLast { !it.isUser && it.isError }
        if (errorIdx in messages.indices) {
            messages.removeAt(errorIdx)
        }

        val lastUserMessage = messages.lastOrNull { it.isUser } ?: return

        voiceAssistant.stop()
        currentlySpeakingText = null
        isTyping = true
        scope.launch {
            try {
                val prescriptionMemory = PrescriptionMemory.build(context)
                val retryPrompt = resolveChatbotPrompt(lastUserMessage.text, lastUserMessage.imagePaths.isNotEmpty())
                val chatbotText = if (prescriptionMemory.isBlank()) {
                    retryPrompt
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
                    $retryPrompt
                    """.trimIndent()
                }

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.predict(
                        PredictionRequest(
                            sessionId = currentSessionId,
                            text = chatbotText
                        )
                    )
                }

                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    val answer = result.answer?.trim().orEmpty()
                    val finalAnswer = if (answer.isNotEmpty()) {
                        fixMeasurementFormatting(
                            answer = answer,
                            measurements = result.measurements
                        )
                    } else {
                        "I’m sorry, I could not generate a response right now."
                    }

                    val isDuplicateRetry = messages.lastOrNull()?.let { last ->
                        !last.isUser && last.text.trim() == finalAnswer.trim()
                    } ?: false

                    if (!isDuplicateRetry) {
                        messages.add(
                            ChatMessage(
                                text = finalAnswer,
                                isUser = false,
                                intent = result.intent,
                                confidence = result.confidence,
                                similarity = result.answerSimilarity
                            )
                        )
                    }

                    val requestSessionId = currentSessionId

                    // Adopt server-returned session ID if valid and non-blank.
                    val returnedSessionId = result.sessionId?.trim()
                    val targetSessionId = if (!returnedSessionId.isNullOrBlank() && currentSessionId == requestSessionId) {
                        if (returnedSessionId != currentSessionId) {
                            val previousSessionId = currentSessionId
                            currentSessionId = returnedSessionId
                            ChatHistoryRepository.setActiveSessionId(context, returnedSessionId)
                            ChatHistoryRepository.deleteChat(context, previousSessionId)
                        }
                        returnedSessionId
                    } else {
                        currentSessionId
                    }

                    ChatHistoryRepository.saveChat(context, targetSessionId, messages)
                    savedChats = ChatHistoryRepository.getChats(context)
                } else {
                    val code = response.code()
                    messages.add(
                        ChatMessage(
                            text = "Server returned error (HTTP $code). Tap Retry to try again.",
                            isUser = false,
                            isError = true
                        )
                    )
                    ChatHistoryRepository.saveChat(context, currentSessionId, messages)
                    savedChats = ChatHistoryRepository.getChats(context)
                }
            } catch (e: Exception) {
                Log.e("MEDASSIST_API", "RETRY ERROR", e)
                val errText = when (e) {
                    is java.net.SocketTimeoutException ->
                        "The connection to MedAssist AI timed out. Tap Retry to try again."
                    is java.net.ConnectException ->
                        "Unable to connect to the MedAssist server at ${RetrofitClient.getBaseUrl(context)}. Please check that Flask is running."
                    is java.net.UnknownHostException ->
                        "Cannot resolve the server address (${RetrofitClient.getBaseUrl(context)}). Please check your connection."
                    else ->
                        "Unable to connect to MedAssist AI (${e.localizedMessage ?: "Network error"}). Tap Retry to try again."
                }
                messages.add(
                    ChatMessage(
                        text = errText,
                        isUser = false,
                        isError = true
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
    // VOICE INPUT
    // =========================================================

    var isListening by remember { mutableStateOf(false) }

    val speechRecognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    fun createSpeechIntent(): Intent {
        val speechLocaleTag = when (selectedLanguage) {
            "मराठी" -> "mr-IN"
            "हिंदी" -> "hi-IN"
            else -> "en-IN"
        }
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLocaleTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, speechLocaleTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            // Let supported Google speech services switch languages automatically.
            putExtra("android.speech.extra.ENABLE_LANGUAGE_DETECTION", true)
            putExtra("android.speech.extra.LANGUAGE_SWITCH_ALLOWED", true)
            putExtra("android.speech.extra.LANGUAGE_SWITCH_INITIAL_ACTIVE_DURATION", 15)
        }
    }

    val speechPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                val intent = createSpeechIntent()
                speechRecognizer?.startListening(intent)
                isListening = speechRecognizer != null
            } catch (e: Exception) {
                Log.e("MEDASSIST_VOICE", "Unable to start speech recognition", e)
                isListening = false
            }
        }
    }

    LaunchedEffect(speechRecognizer) {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {
                isListening = true
            }

            override fun onBeginningOfSpeech() {
                isListening = true
            }

            override fun onRmsChanged(rmsdB: Float) = Unit

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onError(error: Int) {
                isListening = false
                Log.d("MEDASSIST_VOICE", "Speech recognition error: $error")
            }

            override fun onResults(results: android.os.Bundle?) {
                isListening = false
                val spokenText = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()

                if (spokenText.isNotEmpty()) {
                    message = spokenText
                    sendMessage(spokenText)
                }
            }

            override fun onPartialResults(partialResults: android.os.Bundle?) {
                val partialText = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()

                if (partialText.isNotEmpty()) {
                    message = partialText
                }
            }

            override fun onEvent(eventType: Int, params: android.os.Bundle?) = Unit
        })
    }

    androidx.compose.runtime.DisposableEffect(speechRecognizer) {
        onDispose {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (_: Exception) {
            }
        }
    }

    // =========================================================
    // MAIN UI — APPROVED HEALTHCARE PALETTE DESIGN SYSTEM
    // =========================================================

    val screenBackground = Ivory
    val panelBackground = PureWhite
    val primaryTeal = DeepTeal
    val darkTeal = DarkTeal
    val softTeal = SoftMint
    val mintBorder = Mint
    val darkText = TextDark
    val mutedText = TextMuted

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
                    .shadow(elevation = 3.dp),
                color = primaryTeal,
                shape = RoundedCornerShape(
                    bottomStart = 24.dp,
                    bottomEnd = 24.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { handleBack() },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = t("back"),
                            tint = PureWhite
                        )
                    }

                    Spacer(Modifier.width(2.dp))

                    RobotAvatar(
                        size = 40.dp,
                        isError = false
                    )

                    Spacer(Modifier.width(10.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "MedAssist AI",
                            color = PureWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isTyping) CoralAccent else SageGreen)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = if (isTyping) t("chat_status_thinking") else t("chat_assistant_subtitle"),
                                color = PureWhite.copy(alpha = 0.88f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = { startNewChat() },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = PureWhite.copy(alpha = 0.16f),
                            contentColor = PureWhite
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "New chat",
                            tint = PureWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }



                    Spacer(Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            savedChats = ChatHistoryRepository.getChats(context)
                            showChatSidebar = true
                        },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = PureWhite.copy(alpha = 0.16f),
                            contentColor = PureWhite
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Open chat history",
                            tint = PureWhite,
                            modifier = Modifier.size(20.dp)
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
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                shape = RoundedCornerShape(22.dp),
                color = panelBackground,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, mintBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                ) {

                    // 1. Message list container occupying available space above the composer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(chatScrollState)
                                .padding(
                                    start = 12.dp,
                                    end = 12.dp,
                                    top = 8.dp,
                                    bottom = 12.dp
                                ),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                        Spacer(Modifier.height(2.dp))

                        if (shouldShowChatbotWelcome(messages.isEmpty(), isTyping)) {
                            WelcomeSection(
                                primaryTeal = primaryTeal,
                                mutedText = mutedText,
                                onPopulateMessage = { prompt ->
                                    message = prompt
                                }
                            )
                        }

                        messages.forEachIndexed { index, chatMessage ->
                            MessageBubble(
                                message = chatMessage,
                                onEdit = if (chatMessage.isUser && !chatMessage.isError) {
                                    {
                                        editingMessageIndex = index
                                        editingDraft = chatMessage.text
                                        showEditMessageDialog = true
                                    }
                                } else null,
                                onImageClick = { path ->
                                    viewerImagePath = path
                                },
                                onRetry = if (!chatMessage.isUser && chatMessage.isError) {
                                    { retryMessage(index) }
                                } else null,
                                isSpeaking = currentlySpeakingText == chatMessage.text,
                                onToggleSpeak = if (!chatMessage.isUser && !chatMessage.isError && chatMessage.text.isNotBlank()) {
                                    {
                                        if (currentlySpeakingText == chatMessage.text) {
                                            voiceAssistant.stop()
                                            currentlySpeakingText = null
                                        } else {
                                            currentlySpeakingText = chatMessage.text
                                            val ttsLocale = when (selectedLanguage) {
                                                "मराठी", "mr", "mr-IN" -> Locale.forLanguageTag("mr-IN")
                                                "हिंदी", "hi", "hi-IN" -> Locale.forLanguageTag("hi-IN")
                                                else -> Locale.US
                                            }
                                            voiceAssistant.speak(chatMessage.text, ttsLocale)
                                        }
                                    }
                                } else null
                            )
                        }

                        if (isTyping) {
                            TypingIndicator()
                        }

                        Spacer(Modifier.height(2.dp))
                    }

                        // Floating Jump to Latest Button (when user scrolled up)
                        androidx.compose.animation.AnimatedVisibility(
                            visible = userScrolledUp && (chatScrollState.maxValue - chatScrollState.value > 60) && messages.isNotEmpty(),
                            enter = fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.8f),
                            exit = fadeOut(animationSpec = tween(150)),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 12.dp)
                        ) {
                            Surface(
                                onClick = {
                                    userScrolledUp = false
                                    scope.launch {
                                        chatScrollState.animateScrollTo(chatScrollState.maxValue)
                                    }
                                },
                                shape = CircleShape,
                                color = primaryTeal,
                                contentColor = PureWhite,
                                shadowElevation = 5.dp,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowDown,
                                        contentDescription = "Scroll to bottom",
                                        tint = PureWhite,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    // -----------------------------------------
                    // BOTTOM COMPOSER (docked below messages)
                    // -----------------------------------------
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(
                                horizontal = 10.dp,
                                vertical = 6.dp
                            ),
                        shape = RoundedCornerShape(26.dp),
                        color = PureWhite,
                        shadowElevation = 4.dp,
                        border = BorderStroke(
                            1.dp,
                            mintBorder
                        )
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 6.dp,
                                    end = 6.dp,
                                    top = 4.dp,
                                    bottom = 4.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // Plus button + compact ChatGPT-style attachment popup.
                            // The popup is anchored to the plus button and keeps
                            // the existing attachment/API behavior intact.
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {

                                // -------------------------------------------------
                                // ATTACHED IMAGE PREVIEW STRIP
                                // -------------------------------------------------
                                if (selectedImages.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(
                                                start = 8.dp,
                                                end = 8.dp,
                                                top = 4.dp,
                                                bottom = 6.dp
                                            ),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        selectedImages.forEachIndexed { index, bitmap ->
                                            var previewVisible by remember { mutableStateOf(false) }

                                            LaunchedEffect(bitmap) {
                                                previewVisible = true
                                            }

                                            val previewScale by androidx.compose.animation.core.animateFloatAsState(
                                                targetValue = if (previewVisible) 1f else 0.82f,
                                                animationSpec = tween(
                                                    durationMillis = 220,
                                                    easing = FastOutSlowInEasing
                                                ),
                                                label = "attachment_preview_scale_$index"
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .size(72.dp)
                                                    .graphicsLayer {
                                                        scaleX = previewScale
                                                        scaleY = previewScale
                                                    }
                                            ) {
                                                Image(
                                                    bitmap = bitmap.asImageBitmap(),
                                                    contentDescription = "Attached image ${index + 1}",
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .border(
                                                            1.dp,
                                                            mintBorder,
                                                            RoundedCornerShape(14.dp)
                                                        )
                                                )

                                                IconButton(
                                                    onClick = {
                                                        if (index in selectedImages.indices) {
                                                            selectedImages.removeAt(index)
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .align(Alignment.TopEnd)
                                                        .offset(x = 6.dp, y = (-6).dp)
                                                        .shadow(
                                                            elevation = 3.dp,
                                                            shape = CircleShape
                                                        )
                                                        .clip(CircleShape)
                                                        .background(PureWhite)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Close,
                                                        contentDescription = "Remove image",
                                                        tint = darkText,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = 4.dp,
                                            end = 4.dp,
                                            top = 2.dp,
                                            bottom = 2.dp
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box {
                                        IconButton(
                                            onClick = { showAttachmentMenu = true },
                                            modifier = Modifier.size(42.dp),
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = softTeal,
                                                contentColor = primaryTeal
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Add,
                                                contentDescription = "Add attachment",
                                                tint = primaryTeal,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        if (showAttachmentMenu) {
                                            Popup(
                                                alignment = Alignment.BottomStart,
                                                offset = androidx.compose.ui.unit.IntOffset(0, -10),
                                                onDismissRequest = { showAttachmentMenu = false },
                                                properties = PopupProperties(
                                                    focusable = true,
                                                    dismissOnClickOutside = true,
                                                    dismissOnBackPress = true
                                                )
                                            ) {
                                                AttachmentMenuPopup(
                                                    softTeal = softTeal,
                                                    primaryTeal = primaryTeal,
                                                    darkText = darkText,
                                                    mutedText = mutedText,
                                                    onTakePhoto = {
                                                        showAttachmentMenu = false

                                                        val permissionGranted =
                                                            ContextCompat.checkSelfPermission(
                                                                context,
                                                                Manifest.permission.CAMERA
                                                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                                        if (permissionGranted) {
                                                            try {
                                                                cameraLauncher.launch(null)
                                                            } catch (e: Exception) {
                                                                Log.e(
                                                                    "MEDASSIST_CAMERA",
                                                                    "Unable to launch camera",
                                                                    e
                                                                )
                                                            }
                                                        } else {
                                                            cameraPermissionLauncher.launch(
                                                                Manifest.permission.CAMERA
                                                            )
                                                        }
                                                    },
                                                    onUploadImage = {
                                                        showAttachmentMenu = false
                                                        galleryLauncher.launch("image/*")
                                                    },
                                                    onUploadReport = {
                                                        showAttachmentMenu = false
                                                        reportLauncher.launch("application/pdf")
                                                    },
                                                    onPrescription = {
                                                        showAttachmentMenu = false
                                                        attachmentLabel =
                                                            "Prescription context selected"
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.width(2.dp))

                                     OutlinedTextField(
                                        value = message,
                                        onValueChange = { message = it },
                                        modifier = Modifier.weight(1f),
                                        placeholder = {
                                            Text(
                                                text = if (attachmentLabel.isBlank()) {
                                                    t("chat_input_placeholder")
                                                } else {
                                                    attachmentLabel
                                                },
                                                color = if (attachmentLabel.isBlank()) {
                                                    TextLight
                                                } else {
                                                    primaryTeal
                                                },
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
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
                                            focusedPlaceholderColor = TextLight,
                                            unfocusedPlaceholderColor = TextLight,
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            cursorColor = primaryTeal
                                        )
                                    )

                                    // Voice input
                                    IconButton(
                                        onClick = {
                                            if (isTyping) return@IconButton

                                            if (speechRecognizer == null) {
                                                Log.w("MEDASSIST_VOICE", "Speech recognition is not available on this device")
                                                return@IconButton
                                            }

                                            val permissionGranted =
                                                ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.RECORD_AUDIO
                                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                            if (permissionGranted) {
                                                try {
                                                    val intent = createSpeechIntent()
                                                    speechRecognizer.startListening(intent)
                                                    isListening = true
                                                } catch (e: Exception) {
                                                    Log.e("MEDASSIST_VOICE", "Unable to start speech recognition", e)
                                                    isListening = false
                                                }
                                            } else {
                                                speechPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        },
                                        modifier = Modifier.size(42.dp),
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = if (isListening) CoralAccent.copy(alpha = 0.25f) else softTeal,
                                            contentColor = if (isListening) CoralAccent else primaryTeal
                                        )
                                    ) {
                                        Icon(
                                            imageVector = MicIcon,
                                            contentDescription = if (isListening) "Listening" else "Voice input",
                                            tint = if (isListening) CoralAccent else primaryTeal,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(Modifier.width(4.dp))

                                    val canSend = isChatbotSendEnabled(message, selectedImages.isNotEmpty(), isTyping)

                                    IconButton(
                                        onClick = { sendMessage(message) },
                                        enabled = canSend,
                                        modifier = Modifier.size(42.dp),
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = primaryTeal,
                                            contentColor = PureWhite,
                                            disabledContainerColor = SoftMint.copy(alpha = 0.6f),
                                            disabledContentColor = TextLight
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Send",
                                            tint = if (canSend) PureWhite else TextLight,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }

                                }
                            }
                        }
                    }
                }

            }
        }

        // -----------------------------------------------------
        // COMPACT MESSAGE EDITOR
        // -----------------------------------------------------
        if (showEditMessageDialog) {
            Dialog(
                onDismissRequest = {
                    showEditMessageDialog = false
                    editingMessageIndex = null
                    editingDraft = ""
                },
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                    usePlatformDefaultWidth = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .background(Color.Black.copy(alpha = 0.58f))
                        .padding(horizontal = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Soft glass/blurred outer surface.
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 360.dp)
                            .shadow(
                                elevation = 22.dp,
                                shape = RoundedCornerShape(22.dp)
                            ),
                        shape = RoundedCornerShape(22.dp),
                        color = PureWhite,
                        border = BorderStroke(
                            1.dp,
                            mintBorder
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Edit message",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = darkText
                                )

                                IconButton(
                                    onClick = {
                                        showEditMessageDialog = false
                                        editingMessageIndex = null
                                        editingDraft = ""
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Close editor",
                                        tint = mutedText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            OutlinedTextField(
                                value = editingDraft,
                                onValueChange = { editingDraft = it },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 6,
                                placeholder = {
                                    Text(
                                        "Edit your message...",
                                        color = TextLight
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = darkText,
                                    unfocusedTextColor = darkText,
                                    focusedBorderColor = primaryTeal,
                                    unfocusedBorderColor = mintBorder,
                                    focusedContainerColor = SoftMint.copy(alpha = 0.4f),
                                    unfocusedContainerColor = SoftMint.copy(alpha = 0.25f),
                                    cursorColor = primaryTeal
                                )
                            )

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = "Your chat stays paused until you finish editing.",
                                fontSize = 11.sp,
                                color = mutedText,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )

                            Spacer(Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        showEditMessageDialog = false
                                        editingMessageIndex = null
                                        editingDraft = ""
                                    }
                                ) {
                                    Text(
                                        text = "Cancel",
                                        color = mutedText
                                    )
                                }

                                Spacer(Modifier.width(6.dp))

                                Button(
                                    onClick = {
                                        val index = editingMessageIndex
                                        val cleanEditedText = editingDraft.trim()

                                        if (
                                            index != null &&
                                            index in messages.indices &&
                                            cleanEditedText.isNotBlank()
                                        ) {
                                            // Replace only the edited user message.
                                            // Do not send it automatically and do not
                                            // trigger the API while editing.
                                            val oldMessage = messages[index]

                                            messages[index] = oldMessage.copy(
                                                text = cleanEditedText
                                            )

                                            ChatHistoryRepository.saveChat(
                                                context,
                                                currentSessionId,
                                                messages
                                            )
                                            savedChats =
                                                ChatHistoryRepository.getChats(context)
                                        }

                                        showEditMessageDialog = false
                                        editingMessageIndex = null
                                        editingDraft = ""
                                    },
                                    enabled = editingDraft.trim().isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = primaryTeal,
                                        contentColor = PureWhite,
                                        disabledContainerColor = SoftMint.copy(alpha = 0.6f),
                                        disabledContentColor = TextLight
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Save")
                                }
                            }
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------
        // IMAGE VIEWER
        // -----------------------------------------------------
        if (viewerImagePath != null) {
            Dialog(
                onDismissRequest = { viewerImagePath = null }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = PureWhite,
                    shadowElevation = 18.dp,
                    border = BorderStroke(1.dp, mintBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Attached image",
                                modifier = Modifier.weight(1f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = darkText
                            )
                            IconButton(
                                onClick = { viewerImagePath = null }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close image",
                                    tint = darkText
                                )
                            }
                        }

                        val viewerBitmap = remember(viewerImagePath) {
                            viewerImagePath?.let { BitmapFactory.decodeFile(it) }
                        }

                        if (viewerBitmap != null) {
                            Image(
                                bitmap = viewerBitmap.asImageBitmap(),
                                contentDescription = "Opened attached image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 520.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(1.dp, mintBorder, RoundedCornerShape(18.dp))
                                    .clickable { }
                            )

                            Spacer(Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val savedUri = saveImageToGalleryAndGetUri(
                                        context,
                                        viewerBitmap
                                    )

                                    if (savedUri != null) {
                                        val clipboard =
                                            context.getSystemService(
                                                Context.CLIPBOARD_SERVICE
                                            ) as ClipboardManager

                                        clipboard.setPrimaryClip(
                                            ClipData.newUri(
                                                context.contentResolver,
                                                "MedAssist image",
                                                savedUri
                                            )
                                        )

                                        Toast.makeText(
                                            context,
                                            "Image saved and copied",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Unable to copy image",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryTeal,
                                    contentColor = PureWhite
                                )
                            ) {
                                Text("Save & Copy Image")
                            }
                        } else {
                            Text(
                                text = "Image is no longer available.",
                                color = mutedText,
                                modifier = Modifier.padding(16.dp)
                            )
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
                    startNewChat()
                },
                onViewTranscript = if (onViewTranscript != null) {
                    { targetId ->
                        if (messages.isNotEmpty()) {
                            ChatHistoryRepository.saveChat(context, currentSessionId, messages)
                        }
                        showChatSidebar = false
                        onViewTranscript(targetId)
                    }
                } else null,
                onSelectChat = { chat ->
                    if (messages.isNotEmpty()) {
                        ChatHistoryRepository.saveChat(context, currentSessionId, messages)
                    }
                    currentSessionId = chat.id
                    ChatHistoryRepository.setActiveSessionId(context, chat.id)
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
                    color = PureWhite,
                    border = BorderStroke(1.dp, mintBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            start = 8.dp,
                            top = 10.dp,
                            end = 8.dp,
                            bottom = 10.dp
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
                            color = darkText,
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

                        if (onViewTranscript != null) {
                            ChatActionRow(
                                icon = Icons.Filled.Info,
                                text = "View Transcript",
                                onClick = {
                                    showChatActions = false
                                    selectedChatForActions = null
                                    onViewTranscript(selectedChat.id)
                                }
                            )
                        }

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

                            val newId = "android_" + System.currentTimeMillis()
                            currentSessionId = newId
                            ChatHistoryRepository.setActiveSessionId(context, newId)

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

// =============================================================
// VECTOR ICONS FOR CHAT SUGGESTIONS & ACTIONS
// =============================================================

private val SpeakerVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "SpeakerVectorIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = addPathNodes(
            "M3,9v6h4l5,5V4L7,9H3zm13.5,3c0,-1.77 -1.02,-3.29 -2.5,-4.03v8.05c1.48,-0.73 2.5,-2.25 2.5,-4.02zM14,3.23v2.06c2.89,0.86 5,3.54 5,6.71s-2.11,5.85 -5,6.71v2.06c4.01,-0.91 7,-4.49 7,-8.77s-2.99,-7.86 -7,-8.77z"
        ),
        fill = SolidColor(Color.Black)
    ).build()
}

private val StopSpeakerVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "StopSpeakerVectorIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = addPathNodes(
            "M6,6h12v12H6z"
        ),
        fill = SolidColor(Color.Black)
    ).build()
}

private val ShareVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ShareVectorIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = addPathNodes(
            "M18,16.08c-0.76,0 -1.44,0.3 -1.96,0.77L8.91,12.7c0.05,-0.23 0.09,-0.46 0.09,-0.7s-0.04,-0.47 -0.09,-0.7l7.05,-4.11c0.54,0.5 1.25,0.81 2.04,0.81 1.66,0 3,-1.34 3,-3s-1.34,-3 -3,-3 -3,1.34 -3,3c0,0.24 0.04,0.47 0.09,0.7L8.04,9.81C7.5,9.31 6.79,9 6,9c-1.66,0 -3,1.34 -3,3s1.34,3 3,3c0.79,0 1.5,-0.31 2.04,-0.81l7.12,4.16c-0.05,0.21 -0.08,0.43 -0.08,0.65 0,1.61 1.31,2.92 2.92,2.92 1.61,0 2.92,-1.31 2.92,-2.92s-1.31,-2.92 -2.92,-2.92z"
        ),
        fill = SolidColor(Color.Black)
    ).build()
}

@Composable
private fun AiResponseActionRow(
    messageText: String,
    isSpeaking: Boolean,
    onToggleSpeak: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLang = LocalAppLanguageController.current.selectedLanguage

    var toolbarVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        toolbarVisible = true
    }

    AnimatedVisibility(
        visible = toolbarVisible,
        enter = fadeIn(animationSpec = tween(180)) + slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = tween(180)
        )
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 2.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Copy Action
            val copyLabel = appText("chat_action_copy_btn", currentLang)
            val copyDesc = appText("chat_action_copy", currentLang)
            Surface(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = ClipData.newPlainText("MedAssist Response", messageText)
                    clipboard?.setPrimaryClip(clip)
                    Toast.makeText(context, appText("chat_copy_done", currentLang), Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(8.dp),
                color = Color.Transparent,
                contentColor = DeepTeal,
                modifier = Modifier
                    .height(32.dp)
                    .defaultMinSize(minWidth = 48.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = CopyVectorIcon,
                        contentDescription = copyDesc,
                        tint = DeepTeal,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = copyLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepTeal
                    )
                }
            }

            // 2. Share Action
            val shareLabel = appText("chat_action_share_btn", currentLang)
            val shareDesc = appText("chat_action_share", currentLang)
            val shareTitle = appText("chat_share_title", currentLang)
            Surface(
                onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, messageText)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, shareTitle)
                    context.startActivity(shareIntent)
                },
                shape = RoundedCornerShape(8.dp),
                color = Color.Transparent,
                contentColor = DeepTeal,
                modifier = Modifier
                    .height(32.dp)
                    .defaultMinSize(minWidth = 48.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = ShareVectorIcon,
                        contentDescription = shareDesc,
                        tint = DeepTeal,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = shareLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepTeal
                    )
                }
            }

            // 3. Read Aloud / Stop Action
            if (onToggleSpeak != null) {
                val ttsLabel = if (isSpeaking) {
                    appText("chat_action_stop_btn", currentLang)
                } else {
                    appText("chat_action_read_btn", currentLang)
                }
                val ttsDesc = if (isSpeaking) {
                    appText("chat_action_stop_speaking", currentLang)
                } else {
                    appText("chat_action_read_aloud", currentLang)
                }
                Surface(
                    onClick = onToggleSpeak,
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSpeaking) SoftMint else Color.Transparent,
                    contentColor = DeepTeal,
                    modifier = Modifier
                        .height(32.dp)
                        .defaultMinSize(minWidth = 48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) StopSpeakerVectorIcon else SpeakerVectorIcon,
                            contentDescription = ttsDesc,
                            tint = DeepTeal,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = ttsLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DeepTeal
                        )
                    }
                }
            }
        }
    }
}

private val CopyVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CopyVectorIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = addPathNodes(
            "M16,1H4c-1.1,0 -2,0.9 -2,2v14h2V3h12V1zm3,4H8c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h11c1.1,0 2,-0.9 2,-2V7c0,-1.1 -0.9,-2 -2,-2zm0,16H8V7h11v14z"
        ),
        fill = SolidColor(Color.Black)
    ).build()
}

private val DocumentVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "DocumentVectorIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = addPathNodes(
            "M14,2H6c-1.1,0 -2,0.9 -2,2v16c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V8l-6,-6zm2,16H8v-2h8v2zm0,-4H8v-2h8v2zm-3,-5V3.5L18.5,9H13z"
        ),
        fill = SolidColor(Color.Black)
    ).build()
}

private val WellnessVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "WellnessVectorIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = addPathNodes(
            "M12,21.35l-1.45,-1.32C5.4,15.36 2,12.28 2,8.5 2,5.42 4.42,3 7.5,3c1.74,0 3.41,0.81 4.5,2.09C13.09,3.81 14.76,3 16.5,3 19.58,3 22,5.42 22,8.5c0,3.78 -3.4,6.86 -8.55,11.54L12,21.35z"
        ),
        fill = SolidColor(Color.Black)
    ).build()
}

internal val MicIcon: ImageVector by lazy {
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
internal fun WelcomeSection(
    primaryTeal: Color,
    mutedText: Color,
    onPopulateMessage: (String) -> Unit = {}
) {
    val appLanguageController = LocalAppLanguageController.current
    val selectedLanguage = appLanguageController.selectedLanguage
    fun t(key: String): String = appText(key, selectedLanguage)

    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(400)) +
                slideInVertically(
                    animationSpec = tween(400, easing = FastOutSlowInEasing),
                    initialOffsetY = { fullHeight -> fullHeight / 6 }
                )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 10.dp, start = 6.dp, end = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RobotAvatar(
                size = 58.dp,
                isError = false
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = t("chat_welcome_title"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = primaryTeal,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = t("chat_welcome_subtitle"),
                fontSize = 12.5.sp,
                color = mutedText,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(Modifier.height(14.dp))

            // 4 Compact Healthcare Suggestion Chips (2 rows of 2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestionCard(
                    title = t("chat_chip_symptoms"),
                    subtitle = t("chat_chip_symptoms_sub"),
                    icon = StethoscopeVectorIcon,
                    primaryTeal = primaryTeal,
                    mutedText = mutedText,
                    modifier = Modifier.weight(1f),
                    onClick = { onPopulateMessage(t("chat_chip_symptoms_prompt")) }
                )

                SuggestionCard(
                    title = t("chat_chip_medicines"),
                    subtitle = t("chat_chip_medicines_sub"),
                    icon = PillVectorIcon,
                    primaryTeal = primaryTeal,
                    mutedText = mutedText,
                    modifier = Modifier.weight(1f),
                    onClick = { onPopulateMessage(t("chat_chip_medicines_prompt")) }
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestionCard(
                    title = t("chat_chip_reports"),
                    subtitle = t("chat_chip_reports_sub"),
                    icon = DocumentVectorIcon,
                    primaryTeal = primaryTeal,
                    mutedText = mutedText,
                    modifier = Modifier.weight(1f),
                    onClick = { onPopulateMessage(t("chat_chip_reports_prompt")) }
                )

                SuggestionCard(
                    title = t("chat_chip_wellness"),
                    subtitle = t("chat_chip_wellness_sub"),
                    icon = WellnessVectorIcon,
                    primaryTeal = primaryTeal,
                    mutedText = mutedText,
                    modifier = Modifier.weight(1f),
                    onClick = { onPopulateMessage(t("chat_chip_wellness_prompt")) }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Compact Medical Safety Disclaimer Note
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Ivory,
                border = BorderStroke(1.dp, Mint.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = SageGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = t("chat_disclaimer_short"),
                        fontSize = 10.5.sp,
                        color = TextMuted,
                        lineHeight = 14.5.sp
                    )
                }
            }
        }
    }
}

@Composable
internal fun SuggestionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    primaryTeal: Color,
    mutedText: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        color = SoftMint.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, Mint.copy(alpha = 0.6f)),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = primaryTeal,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryTeal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = mutedText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 13.sp
            )
        }
    }
}

// =============================================================
// SAVE IMAGE TO GALLERY + RETURN CONTENT URI
// =============================================================

private fun saveImageToGalleryAndGetUri(
    context: Context,
    bitmap: Bitmap
): Uri? {
    return try {
        val resolver = context.contentResolver

        val values = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "MedAssist_${System.currentTimeMillis()}.jpg"
            )
            put(
                MediaStore.Images.Media.MIME_TYPE,
                "image/jpeg"
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "Pictures/MedAssist AI"
                )
                put(
                    MediaStore.Images.Media.IS_PENDING,
                    1
                )
            }
        }

        val uri = resolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: return null

        resolver.openOutputStream(uri)?.use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val completed = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(uri, completed, null, null)
        }

        uri
    } catch (e: Exception) {
        Log.e("MEDASSIST_IMAGE", "Unable to save image to gallery", e)
        null
    }
}


// =============================================================
// ATTACHMENT POPUP
// =============================================================

@Composable
private fun AttachmentMenuPopup(
    softTeal: Color,
    primaryTeal: Color,
    darkText: Color,
    mutedText: Color,
    onTakePhoto: () -> Unit,
    onUploadImage: () -> Unit,
    onUploadReport: () -> Unit,
    onPrescription: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(224.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        color = PureWhite,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, Mint)
    ) {
        Column(
            modifier = Modifier.padding(7.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(softTeal),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = primaryTeal,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(9.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Add to chat",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkText
                    )
                    Text(
                        text = "Attach something",
                        fontSize = 10.sp,
                        color = mutedText
                    )
                }
            }

            AttachmentMenuItem(
                icon = DocumentScannerVectorIcon,
                title = "Take Photo",
                onClick = onTakePhoto,
                softTeal = softTeal,
                primaryTeal = primaryTeal,
                darkText = darkText
            )

            AttachmentMenuItem(
                icon = DocumentScannerVectorIcon,
                title = "Upload Image",
                onClick = onUploadImage,
                softTeal = softTeal,
                primaryTeal = primaryTeal,
                darkText = darkText
            )

            AttachmentMenuItem(
                icon = DocumentScannerVectorIcon,
                title = "Upload Report",
                onClick = onUploadReport,
                softTeal = softTeal,
                primaryTeal = primaryTeal,
                darkText = darkText
            )

            AttachmentMenuItem(
                icon = PillVectorIcon,
                title = "Prescription",
                onClick = onPrescription,
                softTeal = softTeal,
                primaryTeal = primaryTeal,
                darkText = darkText
            )
        }
    }
}


// =============================================================
// COMPACT ATTACHMENT MENU ITEM
// =============================================================

@Composable
private fun AttachmentMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    softTeal: Color,
    primaryTeal: Color,
    darkText: Color
) {
    var pressed by remember { mutableStateOf(false) }

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(100),
        label = "attachment_item_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Transparent)
            .clickable {
                pressed = true
                onClick()
                pressed = false
            }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(softTeal),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = primaryTeal,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = darkText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = primaryTeal.copy(alpha = 0.55f),
            modifier = Modifier.size(17.dp)
        )
    }
}


// =============================================================
// ATTACHMENT OPTION
// =============================================================

@Composable
private fun AttachmentOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(LightGreenSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DeepTeal,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
    onViewTranscript: ((String?) -> Unit)? = null,
    onSelectChat: (SavedChat) -> Unit,
    onClearCurrent: () -> Unit,
    onChatLongPress: (SavedChat) -> Unit
) {
    val primaryTeal = DeepTeal
    val panelShape = RoundedCornerShape(
        topEnd = 24.dp,
        bottomEnd = 24.dp
    )

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(310.dp)
            .shadow(16.dp, panelShape)
            .background(PureWhite, panelShape)
            .clickable(
                interactionSource = remember {
                    MutableInteractionSource()
                },
                indication = null,
                onClick = { }
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RobotAvatar(size = 40.dp, isError = false)

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "MedAssist AI",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    "Your conversations",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }


            IconButton(
                onClick = onClose,
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = SoftMint,
                    contentColor = TextMuted
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = TextMuted
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
                contentColor = PureWhite
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

        if (onViewTranscript != null) {
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    onViewTranscript(currentSessionId)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftMint,
                    contentColor = DarkTeal
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Mint)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = DarkTeal,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Consultation Transcript",
                    fontWeight = FontWeight.SemiBold,
                    color = DarkTeal
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            "Older Chats",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMuted
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
                    color = TextMuted
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
                tint = StatusError,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Clear Current Chat",
                color = StatusError,
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
        color = TextMuted
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
    val rowShape = RoundedCornerShape(14.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clip(rowShape)
            .background(
                if (isCurrent) SoftMint else PureWhite
            )
            .border(
                width = 1.dp,
                color = if (isCurrent) Mint else Mint.copy(alpha = 0.5f),
                shape = rowShape
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RobotAvatar(size = 32.dp, isError = false)
        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (chat.pinned) {
                    Icon(
                        imageVector = PinIcon,
                        contentDescription = "Pinned",
                        tint = DeepTeal,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = chat.title,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    color = TextDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = chatRelativeTime(chat.updatedAt),
                fontSize = 11.sp,
                color = TextLight,
                maxLines = 1
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open chat",
            tint = TextLight,
            modifier = Modifier.size(18.dp)
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
            tint = if (destructive) StatusError else DeepTeal,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = if (destructive) StatusError else TextDark,
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
// =============================================================
// MEDASSIST ROBOT AVATAR
// =============================================================

@Composable
internal fun RobotAvatar(
    size: androidx.compose.ui.unit.Dp,
    isError: Boolean
) {
    val backgroundColor =
        if (isError) StatusErrorBackground else SoftMint

    val borderColor =
        if (isError) StatusErrorBorder else Mint

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.doctor_avatar),
            contentDescription = "MedAssist Medical Robot Doctor",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

// =============================================================
// MESSAGE BUBBLE
// =============================================================

@Composable
fun MessageBubble(
    message: ChatMessage,
    onEdit: (() -> Unit)? = null,
    onImageClick: ((String) -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    isSpeaking: Boolean = false,
    onToggleSpeak: (() -> Unit)? = null
) {
    var showMessage by remember { mutableStateOf(true) }

    val bubbleShape =
        if (message.isUser) {
            RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 4.dp
            )
        } else {
            RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 18.dp
            )
        }

    val hasImages = message.imagePaths.isNotEmpty()

    val bubbleColor =
        when {
            message.isError -> StatusErrorBackground
            message.isUser && hasImages -> PureWhite
            message.isUser -> DeepTeal
            else -> PureWhite
        }

    val bubbleBorder =
        when {
            message.isError -> BorderStroke(1.dp, StatusErrorBorder)
            message.isUser && hasImages -> BorderStroke(1.dp, Mint)
            message.isUser -> null
            else -> BorderStroke(1.dp, Mint.copy(alpha = 0.5f))
        }

    AnimatedVisibility(
        visible = showMessage,
        enter =
            fadeIn(
                animationSpec = tween(
                    durationMillis = 240,
                    easing = FastOutSlowInEasing
                )
            ) +
                    slideInVertically(
                        initialOffsetY = { it / 8 },
                        animationSpec = tween(
                            durationMillis = 280,
                            easing = FastOutSlowInEasing
                        )
                    ) +
                    scaleIn(
                        initialScale = 0.96f,
                        animationSpec = tween(
                            durationMillis = 280,
                            easing = FastOutSlowInEasing
                        )
                    )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                if (message.isUser) {
                    Arrangement.End
                } else {
                    Arrangement.Start
                },
            verticalAlignment = Alignment.Bottom
        ) {

            if (!message.isUser) {
                RobotAvatar(
                    size = 32.dp,
                    isError = message.isError
                )

                Spacer(Modifier.width(6.dp))
            }

            Column(
                modifier = Modifier.widthIn(max = 296.dp),
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
                        if (message.isUser) 1.5.dp else 0.5.dp
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = if (hasImages) 6.dp else 14.dp,
                            vertical = if (hasImages) 6.dp else 10.dp
                        )
                    ) {

                        // Compact image strip inside the message bubble.
                        if (message.imagePaths.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .widthIn(max = 280.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                message.imagePaths.forEachIndexed { imageIndex, path ->
                                    val bitmap = remember(path) {
                                        BitmapFactory.decodeFile(path)
                                    }

                                    if (bitmap != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(110.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(
                                                    1.dp,
                                                    Mint,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    onImageClick?.invoke(path)
                                                }
                                        ) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription =
                                                    "Uploaded image ${imageIndex + 1}",
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }

                            if (message.text.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                            }
                        }

                        if (message.text.isNotBlank()) {
                            FormattedMessageText(
                                text = message.text,
                                isUser = message.isUser && !hasImages
                            )
                        }

                        if (message.isError && onRetry != null) {
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StatusError)
                                    .clickable { onRetry() }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry",
                                    tint = PureWhite,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Retry",
                                    color = PureWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Speaker label + edit action.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = if (message.isUser) 0.dp else 4.dp,
                            end = if (message.isUser) 2.dp else 0.dp,
                            top = 3.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement =
                        if (message.isUser) {
                            Arrangement.End
                        } else {
                            Arrangement.Start
                        }
                ) {
                    Text(
                        text =
                            if (message.isUser) {
                                "You"
                            } else {
                                "MEDASSIST AI"
                            },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color =
                            if (message.isUser) {
                                DeepTeal
                            } else {
                                TextMuted
                            }
                    )


                    if (message.isUser && onEdit != null) {
                        Spacer(Modifier.width(4.dp))

                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit message",
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    if (
                        !message.isUser &&
                        !message.isError &&
                        message.intent.isNotBlank()
                    ) {
                        Spacer(Modifier.width(7.dp))

                        Text(
                            text = message.intent
                                .replace("_", " ")
                                .replaceFirstChar { it.uppercase() },
                            fontSize = 8.sp,
                            color = SageGreen
                        )
                    }

                }

                // ChatGPT-style Action Row (Copy, Share, Read Aloud) for completed AI responses
                if (!message.isUser && !message.isError && message.text.isNotBlank()) {
                    AiResponseActionRow(
                        messageText = message.text,
                        isSpeaking = isSpeaking,
                        onToggleSpeak = onToggleSpeak
                    )
                }
            }
        }
    }
}

// =============================================================
// FORMATTED MESSAGE
// =============================================================

@Composable
internal fun FormattedMessageText(

    text: String,

    isUser: Boolean

) {

    val textColor =
        if (isUser) PureWhite else TextDark

    val bulletColor =
        if (isUser) PureWhite else DeepTeal

    val lines =
        text
            .replace("\r\n", "\n")
            .split("\n")

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        lines.forEach { rawLine ->
            val line = rawLine.trimEnd()

            // =================================================
            // EMPTY LINE
            // =================================================
            if (line.isBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                return@forEach
            }

            // =================================================
            // BULLET
            // =================================================
            val isBullet =
                line.startsWith("•") ||
                        line.startsWith("- ") ||
                        line.startsWith("* ")

            if (isBullet) {
                val bulletText =
                    when {
                        line.startsWith("•") -> line.removePrefix("•").trim()
                        else -> line.substring(2).trim()
                    }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "•",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = bulletColor
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
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {

        RobotAvatar(
            size = 30.dp,
            isError = false
        )

        Spacer(
            modifier = Modifier.width(6.dp)
        )

        Surface(
            shape = RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 18.dp
            ),
            color = Color.White,
            border = BorderStroke(1.dp, LightGreenBorder.copy(alpha = 0.7f)),
            shadowElevation = 1.dp
        ) {

            Row(
                modifier = Modifier.padding(
                    horizontal = 14.dp,
                    vertical = 11.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
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
            .size(7.dp)
            .graphicsLayer {
                val scale = 0.75f + 0.35f * progress
                scaleX = scale
                scaleY = scale
                translationY = -2.5f.dp.toPx() * progress
                alpha = 0.35f + 0.65f * progress
            }
            .clip(CircleShape)
            .background(PrimaryTeal)
    )
}