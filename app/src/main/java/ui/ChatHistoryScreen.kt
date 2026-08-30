package ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ai_based_medical_chatbot.data.SupabaseClient
import com.example.ai_based_medical_chatbot.data.api.ChatHistoryMessage
import com.example.ai_based_medical_chatbot.data.api.RetrofitClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistoryScreen(
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

    // =========================================================
    // LOAD CHAT HISTORY
    // =========================================================

    suspend fun loadHistory() {

        try {

            isLoading = true
            errorMessage = ""

            val sessionId =
                SupabaseClient.getChatSessionId(context)
                    ?: ""

            if (sessionId.isBlank()) {

                history = emptyList()
                isLoading = false

                return
            }

            val response =
                RetrofitClient.apiService.getChatHistory(
                    sessionId
                )

            if (response.isSuccessful) {

                val body =
                    response.body()

                history =
                    body?.history ?: emptyList()

            } else {

                errorMessage =
                    "Unable to load chat history.\n" +
                            "Server error: ${response.code()}"
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
                    Text("Chat History")
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBackClick
                    ) {

                        Icon(
                            imageVector =
                                Icons.Filled.ArrowBack,

                            contentDescription =
                                "Back"
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
                                    "Clear chat history"
                            )
                        }
                    }
                }
            )
        }

    ) { paddingValues ->

        Box(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
        ) {

            // =================================================
            // LOADING
            // =================================================

            if (isLoading) {

                CircularProgressIndicator(

                    modifier =
                        Modifier.align(
                            Alignment.Center
                        )
                )
            }

            // =================================================
            // ERROR
            // =================================================

            else if (errorMessage.isNotBlank()) {

                Column(

                    modifier =
                        Modifier
                            .align(
                                Alignment.Center
                            )
                            .padding(24.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(

                        text =
                            errorMessage,

                        style =
                            MaterialTheme
                                .typography
                                .bodyLarge
                    )

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    Button(

                        onClick = {

                            scope.launch {
                                loadHistory()
                            }
                        }

                    ) {

                        Text("Retry")
                    }
                }
            }

            // =================================================
            // EMPTY
            // =================================================

            else if (history.isEmpty()) {

                Column(

                    modifier =
                        Modifier
                            .align(
                                Alignment.Center
                            )
                            .padding(24.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(

                        text =
                            "No Chat History",

                        style =
                            MaterialTheme
                                .typography
                                .titleLarge
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(

                        text =
                            "Your conversations will appear here.",

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )
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

                                val sessionId =
                                    SupabaseClient
                                        .getChatSessionId(
                                            context
                                        )
                                        ?: ""

                                if (sessionId.isBlank()) {

                                    history =
                                        emptyList()

                                    return@launch
                                }

                                val response =
                                    RetrofitClient
                                        .apiService
                                        .deleteChatHistory(
                                            sessionId
                                        )

                                if (response.isSuccessful) {

                                    history =
                                        emptyList()

                                    errorMessage = ""

                                } else {

                                    errorMessage =
                                        "Unable to delete chat history.\n" +
                                                "Server error: ${response.code()}"
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

    val isUser =
        message.role.equals(
            "user",
            ignoreCase = true
        )

    Card(

        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(

            modifier =
                Modifier.padding(16.dp)
        ) {

            Text(

                text =
                    if (isUser) {
                        "You"
                    } else {
                        "MEDASSIST AI"
                    },

                style =
                    MaterialTheme
                        .typography
                        .labelLarge
            )

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            Text(

                text =
                    message.message,

                style =
                    MaterialTheme
                        .typography
                        .bodyLarge
            )

            if (
                !message.createdAt.isNullOrBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                Text(

                    text =
                        message.createdAt
                            ?: "",

                    style =
                        MaterialTheme
                            .typography
                            .labelSmall
                )
            }
        }
    }
}