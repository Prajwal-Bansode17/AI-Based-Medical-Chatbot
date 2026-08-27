package ui


import android.content.Context
import android.util.Log

import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.ai_based_medical_chatbot.data.api.PredictionRequest
import com.example.ai_based_medical_chatbot.data.api.RetrofitClient

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.security.MessageDigest


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

    val sessionId =
        remember {

            if (
                !initialSessionId
                    .isNullOrBlank()
            ) {

                initialSessionId.trim()

            } else {

                val preferences =
                    context.getSharedPreferences(
                        "medassist_preferences",
                        Context.MODE_PRIVATE
                    )

                preferences.getString(
                    "session_id",
                    null
                ) ?: run {

                    val newSession =
                        "android_" +
                                System.currentTimeMillis()

                    preferences.edit()
                        .putString(
                            "session_id",
                            newSession
                        )
                        .apply()

                    newSession
                }
            }
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


    val messages =
        remember {

            mutableStateListOf<ChatMessage>()
        }


    val scope =
        rememberCoroutineScope()


    val listState =
        rememberLazyListState()

    val topicScrollState =
        rememberScrollState()

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

            val maxScroll =
                topicScrollState.maxValue

            if (maxScroll > 0) {

                val nextPosition =
                    topicScrollState.value + 170

                if (nextPosition >= maxScroll) {

                    topicScrollState.scrollTo(0)

                } else {

                    topicScrollState.animateScrollTo(
                        nextPosition
                    )
                }
            }
        }
    }


    // =========================================================
    // ONE-TIME WELCOME
    // =========================================================

    LaunchedEffect(Unit) {

        val preferences =
            context.getSharedPreferences(
                "medassist_preferences",
                Context.MODE_PRIVATE
            )


        val welcomeShown =
            preferences.getBoolean(
                "welcome_message_shown",
                false
            )


        if (
            !welcomeShown &&
            messages.isEmpty()
        ) {

            messages.add(

                ChatMessage(

                    text =
                        "Hello! 👋\n\n" +
                                "I’m MedAssist AI, " +
                                "your intelligent health assistant.\n\n" +
                                "You can ask me about:\n" +
                                "• Symptoms and general health\n" +
                                "• Diseases and conditions\n" +
                                "• Medicines and precautions\n" +
                                "• When medical attention may be needed\n\n" +
                                "How can I help you today?",

                    isUser = false
                )
            )


            preferences.edit()
                .putBoolean(
                    "welcome_message_shown",
                    true
                )
                .apply()
        }
    }


    // =========================================================
    // AUTO SCROLL
    // =========================================================

    LaunchedEffect(
        messages.size,
        isTyping
    ) {

        if (
            messages.isNotEmpty()
        ) {

            listState.animateScrollToItem(
                messages.lastIndex
            )
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

                        RetrofitClient
                            .apiService
                            .predict(

                                PredictionRequest(

                                    sessionId =
                                        sessionId,

                                    text =
                                        cleanText
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
                        "SESSION_ID = $sessionId"
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

            } finally {

                isTyping = false
            }
        }
    }


    // =========================================================
    // MAIN UI
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
        // TOP BAR
        // =====================================================

        Surface(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),

            shadowElevation =
                4.dp
        ) {

            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 18.dp,
                            bottom = 14.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {


                // =================================================
                // AI LOGO
                // =================================================

                Box(

                    modifier =
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Color(0xFF1976D2)
                            ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(

                        text =
                            "AI",

                        color =
                            Color.White,

                        fontWeight =
                            FontWeight.Bold,

                        fontSize =
                            13.sp
                    )
                }


                Spacer(
                    modifier =
                        Modifier.width(12.dp)
                )


                // =================================================
                // TITLE
                // =================================================

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
                                "MedAssist is thinking…"
                            else
                                "Your intelligent health companion",

                        fontSize =
                            12.sp,

                        color =
                            Color(0xFF667085)
                    )
                }


                // =================================================
                // CLEAR CHAT
                // =================================================

                IconButton(

                    onClick = {

                        if (
                            messages.size > 1
                        ) {

                            showClearDialog = true
                        }
                    }
                ) {

                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear chat",
                        tint = Color(0xFF667085)
                    )

                }


                // =================================================
                // TYPING INDICATOR
                // =================================================

                if (isTyping) {

                    CircularProgressIndicator(

                        modifier =
                            Modifier.size(24.dp),

                        strokeWidth =
                            2.dp
                    )
                }
            }
        }


        // =====================================================
        // ALWAYS-VISIBLE POPULAR HEALTH TOPICS
        // =====================================================

        Surface(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    ),

            shape =
                RoundedCornerShape(
                    16.dp
                ),

            color = Color.White,

            shadowElevation = 1.dp

        ) {

            Column(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 10.dp,
                            vertical = 9.dp
                        )
            ) {

                Text(
                    text = "Common health questions",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF667085)
                )

                Spacer(
                    modifier = Modifier.height(7.dp)
                )

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(
                                topicScrollState
                            ),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    popularTopics.forEach { topic ->

                        QuickPrompt(
                            text = topic,
                            onClick = {
                                val query =
                                    topic
                                        .replace(
                                            Regex("^[^A-Za-z]+"),
                                            ""
                                        )

                                sendMessage(query)
                            }
                        )
                    }
                }
            }
        }


        // =====================================================
        // CHAT AREA
        // =====================================================

        LazyColumn(

            state =
                listState,

            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp
                    ),

            verticalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {


            item {

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )
            }


            // =================================================
            // MESSAGES
            // =================================================

            items(

                items =
                    messages
            ) {

                    chatMessage ->

                MessageBubble(

                    message =
                        chatMessage
                )
            }


            // =================================================
            // TYPING
            // =================================================

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
                        .padding(10.dp),

                verticalAlignment =
                    Alignment.CenterVertically
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
                            "Ask MedAssist anything..."
                        )
                    },

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
                        ),

                    maxLines =
                        4,

                    shape =
                        RoundedCornerShape(
                            22.dp
                        )
                )


                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )


                // =================================================
                // SEND BUTTON
                // =================================================

                val canSend =

                    message
                        .trim()
                        .isNotEmpty() &&
                            !isTyping


                IconButton(

                    onClick = {

                        sendMessage(
                            message
                        )
                    },

                    enabled =
                        canSend,

                    modifier =
                        Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(

                                if (canSend)

                                    Color(
                                        0xFF1976D2
                                    )

                                else

                                    Color(
                                        0xFFB0BEC5
                                    )
                            )
                ) {

                    Icon(

                        imageVector =
                            Icons.AutoMirrored.Filled.Send,

                        contentDescription =
                            "Send",

                        tint =
                            Color.White
                    )
                }
            }
        }
    }


    // =========================================================
    // CLEAR CHAT DIALOG
    // =========================================================

    if (
        showClearDialog
    ) {

        AlertDialog(

            onDismissRequest = {

                showClearDialog =
                    false
            },

            title = {

                Text(
                    "Clear conversation?"
                )
            },

            text = {

                Text(
                    "This will clear the current conversation from the screen."
                )
            },

            confirmButton = {

                Button(

                    onClick = {

                        messages.clear()


                        context
                            .getSharedPreferences(
                                "medassist_preferences",
                                Context.MODE_PRIVATE
                            )
                            .edit()
                            .putBoolean(
                                "welcome_message_shown",
                                false
                            )
                            .apply()


                        showClearDialog =
                            false


                        messages.add(

                            ChatMessage(

                                text =
                                    "Hello! 👋\n\n" +
                                            "I’m MedAssist AI, " +
                                            "your intelligent health assistant.\n\n" +
                                            "How can I help you today?",

                                isUser =
                                    false
                            )
                        )


                        context
                            .getSharedPreferences(
                                "medassist_preferences",
                                Context.MODE_PRIVATE
                            )
                            .edit()
                            .putBoolean(
                                "welcome_message_shown",
                                true
                            )
                            .apply()
                    }
                ) {

                    Text(
                        "Clear"
                    )
                }
            },

            dismissButton = {

                TextButton(

                    onClick = {

                        showClearDialog =
                            false
                    }
                ) {

                    Text(
                        "Cancel"
                    )
                }
            }
        )
    }
}


// =============================================================
// QUICK PROMPT
// =============================================================

@Composable
fun QuickPrompt(

    text: String,

    onClick: () -> Unit
) {

    Surface(

        modifier =
            Modifier.clickable {
                onClick()
            },

        shape =
            RoundedCornerShape(
                18.dp
            ),

        color =
            Color.White,

        shadowElevation =
            1.dp
    ) {

        Text(

            text =
                text,

            modifier =
                Modifier.padding(
                    horizontal = 14.dp,
                    vertical = 9.dp
                ),

            fontSize =
                12.sp,

            color =
                Color(0xFF1976D2)
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

            if (
                message.isUser
            )

                Arrangement.End

            else

                Arrangement.Start,

        verticalAlignment =
            Alignment.Bottom
    ) {


        // =====================================================
        // AI ICON
        // =====================================================

        if (
            !message.isUser
        ) {

            Box(

                modifier =
                    Modifier
                        .size(34.dp)
                        .clip(
                            CircleShape
                        )
                        .background(

                            if (
                                message.isError
                            )

                                Color(
                                    0xFFD32F2F
                                )

                            else

                                Color(
                                    0xFF1976D2
                                )
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(

                    text =
                        "AI",

                    color =
                        Color.White,

                    fontSize =
                        10.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }


            Spacer(

                modifier =
                    Modifier.width(
                        7.dp
                    )
            )
        }


        // =====================================================
        // MESSAGE COLUMN
        // =====================================================

        Column(

            modifier =
                Modifier.fillMaxWidth(
                    0.86f
                ),

            horizontalAlignment =

                if (
                    message.isUser
                )

                    Alignment.End

                else

                    Alignment.Start
        ) {


            Surface(

                shape =

                    if (
                        message.isUser
                    )

                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = 20.dp,
                            bottomEnd = 5.dp
                        )

                    else

                        RoundedCornerShape(
                            topStart = 5.dp,
                            topEnd = 20.dp,
                            bottomStart = 20.dp,
                            bottomEnd = 20.dp
                        ),

                color =

                    when {

                        message.isError ->

                            Color(
                                0xFFFFEBEE
                            )

                        message.isUser ->

                            Color(
                                0xFF1976D2
                            )

                        else ->

                            Color.White
                    },

                shadowElevation =

                    if (
                        message.isUser
                    )

                        0.dp

                    else

                        1.dp
            ) {

                Column(

                    modifier =
                        Modifier.padding(
                            horizontal = 14.dp,
                            vertical = 11.dp
                        )
                ) {

                    // =================================================
                    // FORMATTED MESSAGE
                    // =================================================

                    FormattedMessageText(

                        text =
                            message.text,

                        isUser =
                            message.isUser
                    )
                }
            }


            // =====================================================
            // AI RESPONSE META
            // =====================================================

            if (
                !message.isUser &&
                !message.isError
            ) {

                Row(

                    modifier =
                        Modifier.padding(
                            start = 4.dp,
                            top = 4.dp
                        ),

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {

                    if (
                        message.intent.isNotBlank()
                    ) {

                        Text(

                            text =
                                message.intent
                                    .replace(
                                        "_",
                                        " "
                                    )
                                    .replaceFirstChar {
                                        it.uppercase()
                                    },

                            fontSize =
                                9.sp,

                            color =
                                Color(
                                    0xFF98A2B3
                                )
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
                5.dp
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
                            14.sp,

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
                            14.sp,

                        lineHeight =
                            20.sp,

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

                            15.sp

                        else

                            14.sp,

                    lineHeight =
                        21.sp,

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

        Box(

            modifier =
                Modifier
                    .size(34.dp)
                    .clip(
                        CircleShape
                    )
                    .background(
                        Color(
                            0xFF1976D2
                        )
                    ),

            contentAlignment =
                Alignment.Center
        ) {

            Text(

                text =
                    "AI",

                color =
                    Color.White,

                fontSize =
                    10.sp,

                fontWeight =
                    FontWeight.Bold
            )
        }


        Spacer(

            modifier =
                Modifier.width(
                    7.dp
                )
        )


        Surface(

            shape =
                RoundedCornerShape(
                    18.dp
                ),

            color =
                Color.White,

            shadowElevation =
                1.dp
        ) {

            Row(

                modifier =
                    Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),

                horizontalArrangement =
                    Arrangement.spacedBy(
                        5.dp
                    ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                TypingDot()

                TypingDot()

                TypingDot()
            }
        }
    }
}


// =============================================================
// TYPING DOT
// =============================================================

@Composable
private fun TypingDot() {

    Box(

        modifier =
            Modifier
                .size(6.dp)
                .clip(
                    CircleShape
                )
                .background(
                    Color(
                        0xFF98A2B3
                    )
                )
    )
}


// =============================================================
// SHA-256 HELPER
// =============================================================

private fun sha256(
    value: String
): String {

    val digest =
        MessageDigest.getInstance(
            "SHA-256"
        )


    val bytes =
        digest.digest(
            value.toByteArray(
                Charsets.UTF_8
            )
        )


    return bytes.joinToString(
        ""
    ) {

        "%02x".format(
            it
        )
    }
}