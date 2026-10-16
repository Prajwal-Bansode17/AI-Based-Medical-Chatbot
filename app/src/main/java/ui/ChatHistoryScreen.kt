package ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_based_medical_chatbot.data.SupabaseClient
import com.example.ai_based_medical_chatbot.data.api.ChatHistoryMessage
import com.example.ai_based_medical_chatbot.data.api.RetrofitClient
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

            try {
                val response =
                    RetrofitClient.apiService.getChatHistory(
                        resolvedId
                    )

                if (response.isSuccessful) {
                    val body = response.body()
                    val remoteHistory = body?.history ?: emptyList()
                    if (remoteHistory.isNotEmpty()) {
                        history = remoteHistory
                    } else {
                        // Check local fallback
                        val local = ChatHistoryRepository.getChats(context)
                            .firstOrNull { it.id == resolvedId }
                        history = local?.messages?.map { m ->
                            ChatHistoryMessage(
                                role = if (m.isUser) "user" else "assistant",
                                message = m.text
                            )
                        } ?: emptyList()
                    }
                } else {
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
                        errorMessage =
                            "Unable to load chat history.\n" +
                                    "Server error: ${response.code()}"
                    }
                }
            } catch (netEx: Exception) {
                // Offline fallback to local repository
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
                    errorMessage =
                        netEx.message ?: "Unable to load chat history."
                }
            }

        } catch (e: Exception) {

            errorMessage =
                e.message
                    ?: "Unable to load chat history."

        } finally {

            isLoading = false
        }
    }

    // =========================================================
    // INITIAL LOAD
    // =========================================================

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
                    Text(
                        text = "Chat History",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBackClick
                    ) {

                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,

                            contentDescription =
                                "Back",
                            tint = Color.White
                        )
                    }
                },

                actions = {

                    if (history.isNotEmpty()) {

                        IconButton(

                            onClick = {
                                showDeleteDialog = true
                            }

                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Delete,

                                contentDescription =
                                    "Clear chat history",
                                tint = Color.White
                            )
                        }
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF00897B)
                )
            )
        }

    ) { paddingValues ->

        Box(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF7FBFB))
        ) {

            // =================================================
            // LOADING
            // =================================================

            if (isLoading) {

                CircularProgressIndicator(

                    modifier =
                        Modifier.align(
                            Alignment.Center
                        ),
                    color = Color(0xFF00897B)
                )
            }

            // =================================================
            // ERROR
            // =================================================

            else if (errorMessage.isNotBlank()) {

                Card(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp)
                            .fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F8)),
                    border = BorderStroke(1.dp, Color(0xFFFFCDD2))
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
                                .background(Color(0xFFFFEBEE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Connection Issue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB71C1C)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF5A626A),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    loadHistory()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // =================================================
            // EMPTY
            // =================================================

            else if (history.isEmpty()) {

                Card(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp)
                            .fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE0F2F1))
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
                                .background(Color(0xFFE0F2F1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF00897B),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No Chat History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Your consultations and advice with MedAssist AI will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6B7280),
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

                    modifier =
                        Modifier.fillMaxSize(),

                    contentPadding =
                        PaddingValues(16.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    items(
                        items = history
                    ) { message ->

                        ChatHistoryItem(
                            message = message
                        )
                    }
                }
            }
        }
    }

    // =========================================================
    // DELETE DIALOG
    // =========================================================

    if (showDeleteDialog) {

        AlertDialog(

            onDismissRequest = {

                showDeleteDialog = false
            },

            title = {

                Text(
                    "Clear Chat History?"
                )
            },

            text = {

                Text(
                    "Are you sure you want to delete your chat history?"
                )
            },

            confirmButton = {

                TextButton(

                    onClick = {

                        showDeleteDialog = false

                        scope.launch {

                            try {

                                val sessionToDelete = currentTargetSessionId
                                if (sessionToDelete.isNotBlank()) {
                                    try {
                                        RetrofitClient.apiService.deleteChatHistory(
                                            sessionToDelete
                                        )
                                    } catch (_: Exception) {
                                        // Ignore remote deletion errors when offline
                                    }

                                    ChatHistoryRepository.deleteChat(
                                        context,
                                        sessionToDelete
                                    )
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

                                errorMessage =
                                    e.message
                                        ?: "Unable to delete chat history."
                            }
                        }
                    }

                ) {

                    Text("Delete")
                }
            },

            dismissButton = {

                TextButton(

                    onClick = {

                        showDeleteDialog =
                            false
                    }

                ) {

                    Text("Cancel")
                }
            }
        )
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
            containerColor = if (isUser) Color(0xFFE0F2F1) else Color.White
        ),
        border = BorderStroke(
            1.dp,
            if (isUser) Color(0xFFB2DFDB) else Color(0xFFE5E7EB)
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
                        .background(if (isUser) Color(0xFF00796B) else Color(0xFF0288D1))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isUser) "You" else "MEDASSIST AI",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isUser) Color(0xFF004D40) else Color(0xFF01579B)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = message.message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1F2937),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            if (!message.createdAt.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message.createdAt ?: "",
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF)
                )
            }
        }
    }
}