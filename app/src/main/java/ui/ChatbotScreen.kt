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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_based_medical_chatbot.data.api.PredictionRequest
import com.example.ai_based_medical_chatbot.data.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val intent: String = "",
    val confidence: Double = 0.0,
    val similarity: Double = 0.0,
    val isError: Boolean = false
)

@Composable
fun ChatbotScreen(
    onBack: () -> Unit = {}
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

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()


    // =========================================================
    // WELCOME MESSAGE
    // =========================================================

    LaunchedEffect(Unit) {

        if (messages.isEmpty()) {

            messages.add(
                ChatMessage(
                    text = "Hello! 👋\n\nI am MedAssist AI. How can I help you today?",
                    isUser = false
                )
            )
        }
    }


    // =========================================================
    // AUTO SCROLL
    // =========================================================

    LaunchedEffect(
        messages.size,
        isTyping
    ) {

        if (messages.isNotEmpty()) {

            listState.animateScrollToItem(
                index = messages.size - 1
            )
        }
    }


    // =========================================================
    // SEND MESSAGE
    // =========================================================

    fun sendMessage(text: String) {

        val cleanText = text.trim()

        if (
            cleanText.isEmpty() ||
            isTyping
        ) {
            return
        }


        // -----------------------------------------------------
        // USER MESSAGE
        // -----------------------------------------------------

        messages.add(
            ChatMessage(
                text = cleanText,
                isUser = true
            )
        )

        message = ""
        isTyping = true


        // -----------------------------------------------------
        // API REQUEST
        // -----------------------------------------------------

        scope.launch(Dispatchers.IO) {

            try {

                val response =
                    RetrofitClient.apiService.predict(
                        PredictionRequest(
                            text = cleanText
                        )
                    )


                withContext(Dispatchers.Main) {

                    if (
                        response.isSuccessful &&
                        response.body() != null
                    ) {

                        val result =
                            response.body()!!


                        messages.add(
                            ChatMessage(
                                text = result.answer,
                                isUser = false,
                                intent = result.intent,
                                confidence = result.confidence,
                                similarity =
                                    result.answer_similarity
                            )
                        )

                    } else {

                        // -------------------------------------
                        // HTTP ERROR
                        // -------------------------------------

                        val errorMessage =
                            try {

                                response.errorBody()
                                    ?.string()

                                    ?: "Unknown server error"

                            } catch (e: Exception) {

                                "Unable to read server error"
                            }


                        messages.add(
                            ChatMessage(
                                text =
                                    "Server Error\n\n" +
                                            "HTTP Code: ${response.code()}\n\n" +
                                            errorMessage,
                                isUser = false,
                                isError = true
                            )
                        )
                    }

                    isTyping = false
                }

            } catch (e: Exception) {

                // ------------------------------------------------
                // ACTUAL EXCEPTION
                // ------------------------------------------------

                withContext(Dispatchers.Main) {

                    val errorType =
                        e.javaClass.simpleName

                    val errorMessage =
                        e.message
                            ?: "No error message available"


                    messages.add(
                        ChatMessage(
                            text =
                                "API Connection Error\n\n" +
                                        "Type: $errorType\n\n" +
                                        "Message:\n$errorMessage",
                            isUser = false,
                            isError = true
                        )
                    )

                    isTyping = false
                }
            }
        }
    }


    // =========================================================
    // MAIN UI
    // =========================================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color(0xFFF7F9FC)
            )
    ) {


        // =====================================================
        // TOP BAR
        // =====================================================

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 4.dp
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 12.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = onBack
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.ArrowBack,
                        contentDescription =
                            "Back"
                    )
                }


                // AI ICON

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Color(0xFF1976D2)
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text = "AI",
                        color = Color.White,
                        fontWeight =
                            FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }


                Spacer(
                    modifier =
                        Modifier.width(12.dp)
                )


                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text = "MedAssist AI",
                        fontSize = 19.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            Color(0xFF17202A)
                    )


                    Text(
                        text =
                            if (isTyping)
                                "Thinking..."
                            else
                                "Your intelligent health companion",
                        fontSize = 12.sp,
                        color =
                            Color(0xFF667085)
                    )
                }


                if (isTyping) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }


        // =====================================================
        // CHAT AREA
        // =====================================================

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            item {

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )
            }


            items(messages) { chatMessage ->

                MessageBubble(
                    message = chatMessage
                )
            }


            if (isTyping) {

                item {

                    TypingIndicator()
                }
            }


            item {

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )
            }
        }


        // =====================================================
        // INPUT AREA
        // =====================================================

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding(),
            shadowElevation = 8.dp
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                OutlinedTextField(
                    value = message,

                    onValueChange = {
                        message = it
                    },

                    modifier =
                        Modifier.weight(1f),

                    placeholder = {

                        Text(
                            text =
                                "Ask a medical question..."
                        )
                    },

                    maxLines = 4,

                    shape =
                        RoundedCornerShape(22.dp)
                )


                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )


                IconButton(
                    onClick = {

                        sendMessage(message)
                    },

                    enabled =
                        message.trim().isNotEmpty()
                                && !isTyping,

                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(

                            if (
                                message.trim().isNotEmpty()
                                && !isTyping
                            ) {

                                Color(0xFF1976D2)

                            } else {

                                Color(0xFFB0BEC5)
                            }
                        )
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Send,

                        contentDescription =
                            "Send",

                        tint =
                            Color.White
                    )
                }
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

    Row(
        modifier =
            Modifier.fillMaxWidth(),

        horizontalArrangement =
            if (message.isUser)
                Arrangement.End
            else
                Arrangement.Start,

        verticalAlignment =
            Alignment.Bottom
    ) {


        // =====================================================
        // AI ICON
        // =====================================================

        if (!message.isUser) {

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        Color(0xFF1976D2)
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = "AI",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }


            Spacer(
                modifier =
                    Modifier.width(7.dp)
            )
        }


        Column(
            horizontalAlignment =
                if (message.isUser)
                    Alignment.End
                else
                    Alignment.Start,

            modifier =
                Modifier.fillMaxWidth(0.86f)
        ) {


            // =================================================
            // MESSAGE
            // =================================================

            Surface(

                shape =
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,

                        bottomStart =
                            if (message.isUser)
                                18.dp
                            else
                                4.dp,

                        bottomEnd =
                            if (message.isUser)
                                4.dp
                            else
                                18.dp
                    ),

                color =
                    when {

                        message.isError ->
                            Color(0xFFFFEBEE)

                        message.isUser ->
                            Color(0xFF1976D2)

                        else ->
                            Color.White
                    },

                shadowElevation =
                    if (message.isUser)
                        0.dp
                    else
                        2.dp
            ) {

                Text(
                    text =
                        message.text,

                    modifier =
                        Modifier.padding(
                            horizontal = 15.dp,
                            vertical = 11.dp
                        ),

                    fontSize =
                        15.sp,

                    lineHeight =
                        21.sp,

                    color =
                        if (message.isUser)
                            Color.White
                        else
                            Color(0xFF1F2937)
                )
            }


            // =================================================
            // AI DETAILS
            // =================================================

            if (
                !message.isUser &&
                message.intent.isNotEmpty() &&
                !message.isError
            ) {

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )


                Text(
                    text =
                        "Intent: ${message.intent}  •  " +
                                "Confidence: ${
                                    String.format(
                                        "%.1f",
                                        message.confidence * 100
                                    )
                                }%",

                    fontSize =
                        10.sp,

                    color =
                        Color(0xFF7A869A),

                    modifier =
                        Modifier.padding(
                            start = 6.dp
                        )
                )
            }
        }


        // =====================================================
        // USER ICON
        // =====================================================

        if (message.isUser) {

            Spacer(
                modifier =
                    Modifier.width(7.dp)
            )


            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        Color(0xFFE3F2FD)
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Person,

                    contentDescription =
                        "User",

                    tint =
                        Color(0xFF1976D2),

                    modifier =
                        Modifier.size(19.dp)
                )
            }
        }
    }
}


// =============================================================
// TYPING INDICATOR
// =============================================================

@Composable
fun TypingIndicator() {

    Row(
        modifier =
            Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.Start,

        verticalAlignment =
            Alignment.CenterVertically
    ) {


        // AI ICON

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    Color(0xFF1976D2)
                ),

            contentAlignment =
                Alignment.Center
        ) {

            Text(
                text = "AI",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }


        Spacer(
            modifier =
                Modifier.width(7.dp)
        )


        Surface(
            shape =
                RoundedCornerShape(18.dp),

            color =
                Color.White,

            shadowElevation =
                2.dp
        ) {

            Row(
                modifier =
                    Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                CircularProgressIndicator(
                    modifier =
                        Modifier.size(16.dp),

                    strokeWidth =
                        2.dp
                )


                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )


                Text(
                    text =
                        "Thinking...",

                    fontSize =
                        13.sp,

                    color =
                        Color(0xFF667085)
                )
            }
        }
    }
}