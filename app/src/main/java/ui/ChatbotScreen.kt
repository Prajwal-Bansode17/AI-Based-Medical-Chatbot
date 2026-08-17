package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

@Composable
fun ChatbotScreen(
    onBackClick: () -> Unit = {}
) {

    var message by remember {
        mutableStateOf("")
    }

    var isTyping by remember {
        mutableStateOf(false)
    }

    val messages = remember {
        mutableStateListOf<ChatMessage>()
    }

    fun sendMessage(text: String) {

        val cleanText = text.trim()

        if (cleanText.isEmpty() || isTyping) {
            return
        }

        messages.add(
            ChatMessage(
                text = cleanText,
                isUser = true
            )
        )

        message = ""
        isTyping = true
    }

    /*
     * Temporary AI response.
     *
     * This is NOT the ML model yet.
     * Later we will connect:
     *
     * Android → Flask API → ML Model → AI Response
     */
    LaunchedEffect(isTyping) {

        if (isTyping) {

            delay(1500)

            messages.add(
                ChatMessage(
                    text = "I understand your question. This feature will be connected to the trained medical AI model in the later phase.",
                    isUser = false
                )
            )

            isTyping = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF071A33))
    ) {

        // ---------------------------------------------------------
        // 4.4.2 CHAT HEADER
        // ---------------------------------------------------------

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = {
                    onBackClick()
                }
            ) {

                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Column {

                Text(
                    text = "AI Medical Assistant",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Your personal health assistant",
                    color = Color(0xFFB9EAF2),
                    fontSize = 12.sp
                )
            }
        }

        // ---------------------------------------------------------
        // CHAT CONTENT
        // ---------------------------------------------------------

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {

            if (messages.isEmpty()) {

                // -------------------------------------------------
                // 4.4.3 AI WELCOME MESSAGE
                // -------------------------------------------------

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Spacer(
                        modifier = Modifier.height(35.dp)
                    )

                    Text(
                        text = "👋",
                        fontSize = 42.sp
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = "How can I help you today?",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "Ask me about symptoms, diseases,\nmedicines or general health information.",
                        color = Color(0xFFB9EAF2),
                        fontSize = 14.sp
                    )

                    Spacer(
                        modifier = Modifier.height(28.dp)
                    )

                    // Suggested questions

                    SuggestedQuestion(
                        text = "What are the symptoms of fever?",
                        onClick = {
                            sendMessage("What are the symptoms of fever?")
                        }
                    )

                    SuggestedQuestion(
                        text = "What causes headache?",
                        onClick = {
                            sendMessage("What causes headache?")
                        }
                    )

                    SuggestedQuestion(
                        text = "How can I improve my health?",
                        onClick = {
                            sendMessage("How can I improve my health?")
                        }
                    )
                }

            } else {

                // -------------------------------------------------
                // 4.4.4 USER MESSAGE BUBBLES
                // 4.4.5 AI RESPONSE BUBBLES
                // -------------------------------------------------

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    items(messages) { chatMessage ->

                        ChatBubble(
                            message = chatMessage
                        )
                    }

                    // -------------------------------------------------
                    // 4.4.8 TYPING / LOADING INDICATOR
                    // -------------------------------------------------

                    if (isTyping) {

                        item {

                            TypingIndicator()
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------
        // 4.4.9 MEDICAL DISCLAIMER
        // ---------------------------------------------------------

        Text(
            text = "⚠ Medical information is for educational purposes only. Consult a healthcare professional for medical advice.",
            color = Color(0xFF9BBCC8),
            fontSize = 10.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 4.dp
                )
        )

        // ---------------------------------------------------------
        // 4.4.6 MESSAGE INPUT BOX
        // 4.4.7 SEND BUTTON
        // ---------------------------------------------------------

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            OutlinedTextField(
                value = message,

                onValueChange = {
                    message = it
                },

                modifier = Modifier.weight(1f),

                placeholder = {
                    Text(
                        text = "Ask something..."
                    )
                },

                singleLine = true,

                shape = RoundedCornerShape(16.dp),

                enabled = !isTyping
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            IconButton(
                onClick = {
                    sendMessage(message)
                },
                enabled = message.trim().isNotEmpty() && !isTyping
            ) {

                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (
                        message.trim().isNotEmpty() && !isTyping
                    ) {
                        Color(0xFF7DE8F0)
                    } else {
                        Color(0xFF55727D)
                    }
                )
            }
        }
    }
}


// ================================================================
// SUGGESTED QUESTION
// ================================================================

@Composable
fun SuggestedQuestion(
    text: String,
    onClick: () -> Unit
) {

    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 3.dp
            )
    ) {

        Text(
            text = text,
            color = Color(0xFF7DE8F0),
            fontSize = 13.sp
        )
    }
}


// ================================================================
// CHAT BUBBLE
// ================================================================

@Composable
fun ChatBubble(
    message: ChatMessage
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .background(
                    color = if (message.isUser) {
                        Color(0xFF087EA4)
                    } else {
                        Color(0xFF123653)
                    },
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isUser) {
                            16.dp
                        } else {
                            4.dp
                        },
                        bottomEnd = if (message.isUser) {
                            4.dp
                        } else {
                            16.dp
                        }
                    )
                )
                .padding(13.dp)
        ) {

            Text(
                text = message.text,
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}


// ================================================================
// TYPING INDICATOR
// ================================================================

@Composable
fun TypingIndicator() {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 8.dp,
                top = 4.dp,
                bottom = 4.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .background(
                    Color(0xFF123653),
                    RoundedCornerShape(16.dp)
                )
                .padding(
                    horizontal = 14.dp,
                    vertical = 10.dp
                )
        ) {

            Text(
                text = "AI is typing...",
                color = Color(0xFFB9EAF2),
                fontSize = 13.sp
            )
        }
    }
}