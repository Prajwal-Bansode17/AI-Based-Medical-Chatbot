package ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ai_based_medical_chatbot.LocalAppLanguageController
import com.example.ai_based_medical_chatbot.appText
import com.example.ai_based_medical_chatbot.data.SupabaseClient
import com.example.ai_based_medical_chatbot.data.api.ChatHistoryMessage
import com.example.ai_based_medical_chatbot.data.api.RetrofitClient
import com.example.ai_based_medical_chatbot.ui.theme.DarkTeal
import com.example.ai_based_medical_chatbot.ui.theme.DeepTeal
import com.example.ai_based_medical_chatbot.ui.theme.Ivory
import com.example.ai_based_medical_chatbot.ui.theme.Mint
import com.example.ai_based_medical_chatbot.ui.theme.PureWhite
import com.example.ai_based_medical_chatbot.ui.theme.SageGreen
import com.example.ai_based_medical_chatbot.ui.theme.SoftMint
import com.example.ai_based_medical_chatbot.ui.theme.StatusError
import com.example.ai_based_medical_chatbot.ui.theme.StatusErrorBackground
import com.example.ai_based_medical_chatbot.ui.theme.StatusErrorBorder
import com.example.ai_based_medical_chatbot.ui.theme.TextDark
import com.example.ai_based_medical_chatbot.ui.theme.TextLight
import com.example.ai_based_medical_chatbot.ui.theme.TextMuted
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistoryScreen(
    sessionId: String? = null,
    onOpenConversation: ((String) -> Unit)? = null,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appLanguageController = LocalAppLanguageController.current
    val selectedLanguage = appLanguageController.selectedLanguage
    val t = { key: String -> appText(key, selectedLanguage) }

    var activeSessionId by remember(sessionId) {
        mutableStateOf(
            if (!sessionId.isNullOrBlank()) {
                sessionId.trim()
            } else {
                null
            }
        )
    }

    var savedChats by remember {
        mutableStateOf(ChatHistoryRepository.getChats(context))
    }

    var messages by remember {
        mutableStateOf<List<ChatHistoryMessage>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    suspend fun loadMessagesForSession(targetId: String) {
        if (targetId.isBlank()) {
            messages = emptyList()
            isLoading = false
            return
        }

        isLoading = true
        errorMessage = ""

        val localChat = ChatHistoryRepository.getChats(context).firstOrNull { it.id == targetId }
        if (localChat != null && localChat.messages.isNotEmpty()) {
            messages = localChat.messages.map { m ->
                ChatHistoryMessage(
                    role = if (m.isUser) "user" else "assistant",
                    message = m.text
                )
            }
        }

        try {
            val response = RetrofitClient.apiService.getChatHistory(targetId)
            if (response.isSuccessful) {
                val remoteHistory = response.body()?.history ?: emptyList()
                if (remoteHistory.isNotEmpty()) {
                    messages = remoteHistory
                } else if (messages.isEmpty() && localChat != null) {
                    messages = localChat.messages.map { m ->
                        ChatHistoryMessage(
                            role = if (m.isUser) "user" else "assistant",
                            message = m.text
                        )
                    }
                }
            } else if (messages.isEmpty()) {
                errorMessage = "Unable to load conversation (HTTP ${response.code()})."
            }
        } catch (netEx: Exception) {
            if (messages.isEmpty()) {
                errorMessage = netEx.localizedMessage ?: "Unable to connect to MedAssist server."
            }
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(activeSessionId) {
        savedChats = ChatHistoryRepository.getChats(context)
        val target = activeSessionId
        if (!target.isNullOrBlank()) {
            loadMessagesForSession(target)
        } else {
            messages = emptyList()
            errorMessage = ""
            isLoading = false
        }
    }

    val isConversationDetail = !activeSessionId.isNullOrBlank()
    val activeChat = remember(activeSessionId, savedChats) {
        savedChats.firstOrNull { it.id == activeSessionId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isConversationDetail) {
                                activeChat?.title ?: t("conversation_details")
                            } else {
                                t("chat_history_title")
                            },
                            fontWeight = FontWeight.Bold,
                            color = PureWhite,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isConversationDetail) {
                                if (activeChat != null && activeChat.updatedAt > 0L) {
                                    chatRelativeTime(activeChat.updatedAt, language = selectedLanguage)
                                } else {
                                    t("consultation_transcript")
                                }
                            } else {
                                if (savedChats.size == 1) {
                                    t("chat_history_one_conversation")
                                } else {
                                    t("chat_history_conversations_count").replace("%d", savedChats.size.toString())
                                }
                            },
                            color = PureWhite.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isConversationDetail && sessionId.isNullOrBlank()) {
                                activeSessionId = null
                                messages = emptyList()
                                errorMessage = ""
                            } else {
                                onBackClick()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = t("back"),
                            tint = PureWhite
                        )
                    }
                },
                actions = {
                    if (isConversationDetail) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = t("chat_delete_conversation_desc"),
                                tint = PureWhite
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkTeal
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Ivory, SoftMint)
                    )
                )
        ) {
            if (isConversationDetail) {
                // =====================================================
                // CONVERSATION DETAIL VIEW
                // =====================================================
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = DeepTeal
                    )
                } else if (errorMessage.isNotBlank() && messages.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, StatusErrorBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(StatusErrorBackground)
                                    .border(1.dp, StatusErrorBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = StatusError,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = t("connection_issue"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = {
                                    scope.launch {
                                        activeSessionId?.let { loadMessagesForSession(it) }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(t("retry"), color = PureWhite, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else if (messages.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, Mint),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp, horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(SoftMint)
                                    .border(1.dp, Mint, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = DeepTeal,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = t("no_messages_in_chat"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DarkTeal
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = t("messages_will_appear_here"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items = messages) { message ->
                            ChatHistoryItem(message = message)
                        }

                        if (onOpenConversation != null && activeSessionId != null) {
                            item {
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = { onOpenConversation(activeSessionId!!) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepTeal)
                                ) {
                                    Text(
                                        t("continue_conversation"),
                                        fontWeight = FontWeight.Bold,
                                        color = PureWhite
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // =====================================================
                // CONVERSATIONS LIST VIEW
                // =====================================================
                if (savedChats.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, Mint),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp, horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(SoftMint)
                                    .border(1.dp, Mint, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = DeepTeal,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = t("no_chat_history"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DarkTeal
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = t("chat_history_empty_desc"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val pinnedChats = savedChats.filter { it.pinned }
                        val regularChats = savedChats.filterNot { it.pinned }

                        if (pinnedChats.isNotEmpty()) {
                            item {
                                ChatGroupLabel(t("pinned"))
                            }
                            items(items = pinnedChats, key = { it.id }) { chat ->
                                ConversationSummaryCard(
                                    chat = chat,
                                    onClick = {
                                        if (onOpenConversation != null) {
                                            onOpenConversation(chat.id)
                                        } else {
                                            activeSessionId = chat.id
                                        }
                                    }
                                )
                            }
                            if (regularChats.isNotEmpty()) {
                                item { Spacer(Modifier.height(4.dp)) }
                            }
                        }

                        var lastGroup = ""
                        regularChats.forEach { chat ->
                            val group = chatGroupLabel(chat.updatedAt, language = selectedLanguage)
                            if (group != lastGroup) {
                                item {
                                    ChatGroupLabel(group)
                                }
                                lastGroup = group
                            }

                            item(key = chat.id) {
                                ConversationSummaryCard(
                                    chat = chat,
                                    onClick = {
                                        if (onOpenConversation != null) {
                                            onOpenConversation(chat.id)
                                        } else {
                                            activeSessionId = chat.id
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================================
    // BRANDED DELETE DIALOG
    // =========================================================
    if (showDeleteDialog) {
        Dialog(
            onDismissRequest = { showDeleteDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, Mint),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(StatusErrorBackground, CircleShape)
                            .border(1.dp, StatusErrorBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = StatusError,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = t("chat_history_delete_title"),
                        color = TextDark,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = t("confirm_delete_chat"),
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showDeleteDialog = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SoftMint),
                            border = BorderStroke(1.dp, Mint)
                        ) {
                            Text(
                                text = t("cancel"),
                                color = TextDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                showDeleteDialog = false

                                scope.launch {
                                    try {
                                        val sessionToDelete = activeSessionId
                                        if (!sessionToDelete.isNullOrBlank()) {
                                            try {
                                                RetrofitClient.apiService.deleteChatHistory(sessionToDelete)
                                            } catch (_: Exception) {
                                                // Ignore remote deletion errors when offline
                                            }

                                            ChatHistoryRepository.deleteChat(context, sessionToDelete)
                                        }

                                        savedChats = ChatHistoryRepository.getChats(context)
                                        activeSessionId = null
                                        messages = emptyList()
                                        errorMessage = ""

                                    } catch (e: Exception) {
                                        errorMessage = e.message ?: "Unable to delete chat history."
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                        ) {
                            Text(
                                text = t("delete"),
                                color = PureWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================
// CONVERSATION SUMMARY CARD (List Item)
// =============================================================

@Composable
private fun ConversationSummaryCard(
    chat: SavedChat,
    onClick: () -> Unit
) {
    val appLanguageController = LocalAppLanguageController.current
    val selectedLanguage = appLanguageController.selectedLanguage
    val t = { key: String -> appText(key, selectedLanguage) }
    val cardShape = RoundedCornerShape(16.dp)
    val latestMessage = chat.messages.lastOrNull()?.text?.replace("\n", " ")?.trim().orEmpty()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, Mint),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RobotAvatar(size = 40.dp, isError = false)

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (chat.updatedAt > 0L) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = chatRelativeTime(chat.updatedAt, language = selectedLanguage),
                            fontSize = 11.sp,
                            color = TextLight
                        )
                    }
                }

                if (latestMessage.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = latestMessage,
                        fontSize = 12.sp,
                        color = TextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = t("chat_open_conversation_desc"),
                tint = TextLight,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// =============================================================
// HISTORY MESSAGE CARD
// =============================================================

@Composable
private fun ChatHistoryItem(
    message: ChatHistoryMessage
) {
    val isUser = message.role.equals("user", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUser) SoftMint else PureWhite
        ),
        border = BorderStroke(
            1.dp,
            if (isUser) Mint else Mint.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isUser) SageGreen else DeepTeal)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isUser) "You" else "MEDASSIST AI",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isUser) DarkTeal else DeepTeal
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = message.message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextDark,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            if (!message.createdAt.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message.createdAt ?: "",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}
