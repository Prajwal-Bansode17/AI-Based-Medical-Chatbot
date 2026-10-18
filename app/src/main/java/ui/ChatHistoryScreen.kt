package ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
import com.example.ai_based_medical_chatbot.ui.theme.TextMuted
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistoryScreen(
    sessionId: String? = null,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var history by remember {
        mutableStateOf<List<ChatHistoryMessage>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    var currentTargetSessionId by remember(sessionId) {
        mutableStateOf(
            if (!sessionId.isNullOrBlank()) {
                sessionId.trim()
            } else {
                ChatHistoryRepository.getActiveSessionId(context)
                    ?: ChatHistoryRepository.getChats(context).firstOrNull()?.id
                    ?: SupabaseClient.getChatSessionId(context)
                    ?: ""
            }
        )
    }

    // =========================================================
    // LOAD CHAT HISTORY
    // =========================================================

    suspend fun loadHistory(sessionToLoad: String = currentTargetSessionId) {
        try {
            isLoading = true
            errorMessage = ""

            val resolvedId = if (sessionToLoad.isBlank()) {
                val localChats = ChatHistoryRepository.getChats(context)
                val fallbackId = localChats.firstOrNull()?.id ?: ""
                currentTargetSessionId = fallbackId
                fallbackId
            } else {
                sessionToLoad
            }

            if (resolvedId.isBlank()) {
                history = emptyList()
                isLoading = false
                return
            }

            // Immediately load from local cache if available so UI never hangs in an endless spinner
            val initialLocal = ChatHistoryRepository.getChats(context)
                .firstOrNull { it.id == resolvedId }
            if (initialLocal != null && initialLocal.messages.isNotEmpty()) {
                history = initialLocal.messages.map { m ->
                    ChatHistoryMessage(
                        role = if (m.isUser) "user" else "assistant",
                        message = m.text
                    )
                }
            }

            try {
                val response = RetrofitClient.apiService.getChatHistory(resolvedId)

                if (response.isSuccessful) {
                    val body = response.body()
                    val remoteHistory = body?.history ?: emptyList()
                    if (remoteHistory.isNotEmpty()) {
                        history = remoteHistory
                    } else if (history.isEmpty()) {
                        // Check local fallback if not already loaded
                        val local = ChatHistoryRepository.getChats(context)
                            .firstOrNull { it.id == resolvedId }
                        history = local?.messages?.map { m ->
                            ChatHistoryMessage(
                                role = if (m.isUser) "user" else "assistant",
                                message = m.text
                            )
                        } ?: emptyList()
                    }
                } else if (history.isEmpty()) {
                    val local = ChatHistoryRepository.getChats(context)
                        .firstOrNull { it.id == resolvedId }
                    if (local != null && local.messages.isNotEmpty()) {
                        history = local.messages.map { m ->
                            ChatHistoryMessage(
                                role = if (m.isUser) "user" else "assistant",
                                message = m.text
                            )
                        }
                    } else {
                        errorMessage = "Unable to load chat history.\nServer error: ${response.code()}"
                    }
                }
            } catch (netEx: Exception) {
                // If local cache already provided messages, do not display a blocking error
                if (history.isEmpty()) {
                    val local = ChatHistoryRepository.getChats(context)
                        .firstOrNull { it.id == resolvedId }
                    if (local != null && local.messages.isNotEmpty()) {
                        history = local.messages.map { m ->
                            ChatHistoryMessage(
                                role = if (m.isUser) "user" else "assistant",
                                message = m.text
                            )
                        }
                    } else {
                        errorMessage = netEx.message ?: "Unable to load chat history."
                    }
                }
            }

        } catch (e: Exception) {
            errorMessage = e.message ?: "Unable to load chat history."
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadHistory()
    }

    // =========================================================
    // SCREEN
    // =========================================================

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Chat History",
                            fontWeight = FontWeight.Bold,
                            color = PureWhite,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Consultation transcript",
                            color = PureWhite.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PureWhite
                        )
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear chat history",
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
            // =================================================
            // LOADING
            // =================================================
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = DeepTeal
                )
            }
            // =================================================
            // ERROR
            // =================================================
            else if (errorMessage.isNotBlank()) {
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
                            text = "Connection Issue",
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
                                    loadHistory()
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
                            Text("Retry", color = PureWhite, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            // =================================================
            // EMPTY
            // =================================================
            else if (history.isEmpty()) {
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
                            text = "No Chat History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DarkTeal
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Your consultations and advice with MedAssist AI will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            // =================================================
            // HISTORY LIST
            // =================================================
            else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = history) { message ->
                        ChatHistoryItem(message = message)
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
                        text = "Clear Chat History?",
                        color = TextDark,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Are you sure you want to permanently delete this chat history?",
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
                                text = "Cancel",
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
                                        val sessionToDelete = currentTargetSessionId
                                        if (sessionToDelete.isNotBlank()) {
                                            try {
                                                RetrofitClient.apiService.deleteChatHistory(sessionToDelete)
                                            } catch (_: Exception) {
                                                // Ignore remote deletion errors when offline
                                            }

                                            ChatHistoryRepository.deleteChat(context, sessionToDelete)
                                        }

                                        val remainingChats = ChatHistoryRepository.getChats(context)
                                        val nextSessionId = remainingChats.firstOrNull()?.id ?: ""
                                        currentTargetSessionId = nextSessionId

                                        if (nextSessionId.isNotBlank()) {
                                            loadHistory(nextSessionId)
                                        } else {
                                            history = emptyList()
                                            errorMessage = ""
                                        }

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
                                text = "Delete",
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