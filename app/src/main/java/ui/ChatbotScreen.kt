package ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_based_medical_chatbot.R
import com.example.ai_based_medical_chatbot.data.api.PredictionRequest
import com.example.ai_based_medical_chatbot.data.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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
// CHATBOT SCREEN
// =============================================================

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
                    text =
                        "Hello! 👋\n\n" +
                                "I am MedAssist AI. " +
                                "How can I help you today?",

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
                index = messages.lastIndex
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
        // API CALL
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

                                intent =
                                    result.intent,

                                confidence =
                                    result.confidence,

                                similarity =
                                    result.answer_similarity
                            )
                        )

                    } else {

                        val errorMessage =
                            try {

                                response
                                    .errorBody()
                                    ?.string()
                                    ?: "Unknown server error"

                            } catch (
                                e: Exception
                            ) {

                                "Unable to read server error"
                            }


                        messages.add(
                            ChatMessage(

                                text =
                                    "Server Error\n\n" +
                                            "HTTP Code: " +
                                            "${response.code()}\n\n" +
                                            errorMessage,

                                isUser = false,

                                isError = true
                            )
                        )
                    }

                    isTyping = false
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {

                    messages.add(
                        ChatMessage(

                            text =
                                "API Connection Error\n\n" +
                                        "Type: " +
                                        "${e.javaClass.simpleName}\n\n" +
                                        "Message:\n" +
                                        "${e.message ?: "Unknown error"}",

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
    // MAIN SCREEN
    //
    // IMPORTANT:
    // imePadding() is ONLY on bottom input bar.
    // This keeps the keyboard positioning correct.
    // =========================================================

    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color(0xFFF7F9FC)
                )
    ) {


        // =====================================================
        // HEADER
        // =====================================================

        Surface(

            modifier =
                Modifier.fillMaxWidth(),

            shadowElevation =
                4.dp
        ) {

            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                            vertical = 10.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {


                // -------------------------------------------------
                // BACK BUTTON
                // -------------------------------------------------

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


                // -------------------------------------------------
                // DOCTOR AI LOGO
                // -------------------------------------------------

                MedicalAssistantIcon(
                    size = 44.dp
                )


                Spacer(
                    modifier =
                        Modifier.width(12.dp)
                )


                // -------------------------------------------------
                // TITLE
                // -------------------------------------------------

                Column(

                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(

                        text =
                            "MedAssist AI",

                        fontSize =
                            19.sp,

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

                        fontSize =
                            12.sp,

                        color =
                            Color(0xFF667085)
                    )
                }


                // -------------------------------------------------
                // LOADING
                // -------------------------------------------------

                if (isTyping) {

                    CircularProgressIndicator(

                        modifier =
                            Modifier.size(23.dp),

                        strokeWidth =
                            2.dp
                    )
                }
            }
        }


        // =====================================================
        // CHAT AREA
        // =====================================================

        Box(

            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
        ) {

            LazyColumn(

                state =
                    listState,

                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = 12.dp
                        ),

                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {


                // -------------------------------------------------
                // TOP SPACE
                // -------------------------------------------------

                item {

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )
                }


                // -------------------------------------------------
                // CHAT MESSAGES
                // -------------------------------------------------

                items(
                    items = messages
                ) { chatMessage ->

                    MessageBubble(
                        message =
                            chatMessage
                    )
                }


                // -------------------------------------------------
                // TYPING INDICATOR
                // -------------------------------------------------

                if (isTyping) {

                    item {

                        TypingIndicator()
                    }
                }


                // -------------------------------------------------
                // BOTTOM SPACE
                // -------------------------------------------------

                item {

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )
                }
            }
        }


        // =====================================================
        // INPUT BAR
        //
        // IMPORTANT:
        // Keyboard padding ONLY HERE.
        // =====================================================

        Surface(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding(),

            shadowElevation =
                8.dp
        ) {

            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        ),

                verticalAlignment =
                    Alignment.Bottom
            ) {


                // =================================================
                // TEXT FIELD
                // =================================================

                OutlinedTextField(

                    value =
                        message,

                    onValueChange = {
                        message = it
                    },

                    modifier =
                        Modifier.weight(1f),

                    placeholder = {

                        Text(

                            text =
                                "Ask a medical question...",

                            color =
                                Color(0xFF9AA0A6),

                            fontSize =
                                15.sp
                        )
                    },


                    // -------------------------------------------------
                    // BRIGHT BLACK USER TEXT
                    // -------------------------------------------------

                    textStyle =
                        TextStyle(

                            color =
                                Color.Black,

                            fontSize =
                                16.sp,

                            fontWeight =
                                FontWeight.Medium
                        ),


                    minLines =
                        1,

                    maxLines =
                        4,


                    shape =
                        RoundedCornerShape(22.dp),


                    keyboardOptions =
                        KeyboardOptions(

                            imeAction =
                                ImeAction.Send
                        ),


                    keyboardActions =
                        KeyboardActions(

                            onSend = {

                                sendMessage(
                                    message
                                )
                            }
                        )
                )


                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )


                // =================================================
                // SEND BUTTON
                // =================================================

                IconButton(

                    onClick = {

                        sendMessage(
                            message
                        )
                    },


                    enabled =
                        message
                            .trim()
                            .isNotEmpty()
                                && !isTyping,


                    modifier =
                        Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(

                                if (
                                    message
                                        .trim()
                                        .isNotEmpty()
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
// DOCTOR + AI ASSISTANT ICON
// =============================================================

@Composable
fun MedicalAssistantIcon(
    size: Dp
) {

    Image(

        painter =
            painterResource(
                id =
                    R.drawable.doctor_ai
            ),

        contentDescription =
            "MedAssist AI",

        contentScale =
            ContentScale.Crop,

        modifier =
            Modifier
                .size(size)
                .clip(CircleShape)
    )
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
        // AI DOCTOR LOGO
        // =====================================================

        if (!message.isUser) {

            MedicalAssistantIcon(
                size = 34.dp
            )


            Spacer(
                modifier =
                    Modifier.width(7.dp)
            )
        }


        // =====================================================
        // MESSAGE BODY
        // =====================================================

        Column(

            horizontalAlignment =
                if (message.isUser)
                    Alignment.End
                else
                    Alignment.Start,

            modifier =
                Modifier.fillMaxWidth(0.86f)
        ) {


            Surface(

                shape =
                    RoundedCornerShape(

                        topStart =
                            18.dp,

                        topEnd =
                            18.dp,

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

                            horizontal =
                                15.dp,

                            vertical =
                                11.dp
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
            // INTENT + CONFIDENCE
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

                modifier =
                    Modifier
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


        // =====================================================
        // DOCTOR AI LOGO
        // =====================================================

        MedicalAssistantIcon(
            size = 34.dp
        )


        Spacer(
            modifier =
                Modifier.width(7.dp)
        )


        // =====================================================
        // THINKING BOX
        // =====================================================

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

                        horizontal =
                            16.dp,

                        vertical =
                            12.dp
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