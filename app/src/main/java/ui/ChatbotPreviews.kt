package ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_based_medical_chatbot.ui.theme.AIBasedMedicalChatbotTheme
import com.example.ai_based_medical_chatbot.ui.theme.CoralAccent
import com.example.ai_based_medical_chatbot.ui.theme.DarkTeal
import com.example.ai_based_medical_chatbot.ui.theme.DeepTeal
import com.example.ai_based_medical_chatbot.ui.theme.Ivory
import com.example.ai_based_medical_chatbot.ui.theme.Mint
import com.example.ai_based_medical_chatbot.ui.theme.PureWhite
import com.example.ai_based_medical_chatbot.ui.theme.SageGreen
import com.example.ai_based_medical_chatbot.ui.theme.SoftMint
import com.example.ai_based_medical_chatbot.ui.theme.TextDark
import com.example.ai_based_medical_chatbot.ui.theme.TextLight
import com.example.ai_based_medical_chatbot.ui.theme.TextMuted

// =============================================================
// PREVIEW HEADER HELPER
// =============================================================

@Composable
private fun PreviewChatbotHeader(
    isThinking: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp),
        color = DeepTeal,
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = PureWhite
                )
            }

            Spacer(Modifier.width(2.dp))

            RobotAvatar(size = 38.dp, isError = false)

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MedAssist AI",
                    color = PureWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isThinking) CoralAccent else SageGreen)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (isThinking) "Thinking..." else "AI Health Assistant",
                        color = PureWhite.copy(alpha = 0.90f),
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(
                onClick = {},
                modifier = Modifier.size(38.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = PureWhite.copy(alpha = 0.16f),
                    contentColor = PureWhite
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "New chat",
                    tint = PureWhite,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(Modifier.width(6.dp))

            IconButton(
                onClick = {},
                modifier = Modifier.size(38.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = PureWhite.copy(alpha = 0.16f),
                    contentColor = PureWhite
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "History",
                    tint = PureWhite,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

// =============================================================
// PREVIEW COMPOSER HELPER
// =============================================================

@Composable
private fun PreviewChatComposer(
    text: String = "",
    hasImageAttachment: Boolean = false,
    canSend: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = PureWhite,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, Mint.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            if (hasImageAttachment) {
                Row(
                    modifier = Modifier.padding(start = 6.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SoftMint)
                            .border(1.dp, Mint, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Photo", fontSize = 10.sp, color = DeepTeal)
                        IconButton(
                            onClick = {},
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.TopEnd)
                                .background(PureWhite, CircleShape)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(38.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = SoftMint,
                        contentColor = DeepTeal
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add attachment",
                        tint = DeepTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(4.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = {},
                    placeholder = {
                        Text("Ask MedAssist...", color = TextLight, fontSize = 13.5.sp)
                    },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                IconButton(
                    onClick = {},
                    modifier = Modifier.size(38.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = SoftMint,
                        contentColor = DeepTeal
                    )
                ) {
                    Icon(
                        imageVector = MicIcon,
                        contentDescription = "Voice input",
                        tint = DeepTeal,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(4.dp))

                IconButton(
                    onClick = {},
                    enabled = canSend,
                    modifier = Modifier.size(38.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = DeepTeal,
                        contentColor = PureWhite,
                        disabledContainerColor = SoftMint.copy(alpha = 0.6f),
                        disabledContentColor = TextLight
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (canSend) PureWhite else TextLight,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

// =============================================================
// 1. EMPTY CHATBOT WELCOME STATE PREVIEW
// =============================================================

@Preview(name = "1. Empty Welcome State", showBackground = true, widthDp = 380, heightDp = 640)
@Composable
fun ChatbotEmptyWelcomePreview() {
    AIBasedMedicalChatbotTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Ivory)
        ) {
            PreviewChatbotHeader(isThinking = false)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                color = PureWhite,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, Mint.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    WelcomeSection(
                        primaryTeal = DeepTeal,
                        mutedText = TextMuted,
                        onPopulateMessage = {}
                    )

                    PreviewChatComposer(canSend = false)
                }
            }
        }
    }
}

// =============================================================
// 2. CONVERSATION WITH USER AND AI MESSAGES PREVIEW
// =============================================================

@Preview(name = "2. Active Conversation", showBackground = true, widthDp = 380, heightDp = 640)
@Composable
fun ChatbotConversationPreview() {
    AIBasedMedicalChatbotTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Ivory)
        ) {
            PreviewChatbotHeader(isThinking = false)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                color = PureWhite,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, Mint.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MessageBubble(
                            message = ChatMessage(
                                text = "I have had a mild fever (99.8°F) and dry cough since yesterday.",
                                isUser = true
                            )
                        )

                        MessageBubble(
                            message = ChatMessage(
                                text = "A low-grade fever with a dry cough is commonly associated with viral upper respiratory infections.\n\n• Stay well-hydrated with warm fluids\n• Rest adequately\n• Monitor your temperature twice daily\n\nIf the fever exceeds 101°F or you develop shortness of breath, please consult a healthcare professional promptly.",
                                isUser = false,
                                intent = "symptom_inquiry",
                                confidence = 0.94
                            )
                        )
                    }

                    PreviewChatComposer(canSend = false)
                }
            }
        }
    }
}

// =============================================================
// 3. LONG AI RESPONSE PREVIEW
// =============================================================

@Preview(name = "3. Long AI Response", showBackground = true, widthDp = 380, heightDp = 640)
@Composable
fun ChatbotLongResponsePreview() {
    AIBasedMedicalChatbotTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Ivory)
        ) {
            PreviewChatbotHeader(isThinking = false)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                color = PureWhite,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, Mint.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MessageBubble(
                            message = ChatMessage(
                                text = "Can you explain how to manage hypertension through lifestyle modifications?",
                                isUser = true
                            )
                        )

                        MessageBubble(
                            message = ChatMessage(
                                text = "### Hypertension Management Guidelines:\n\nHypertension (high blood pressure) requires systematic daily care to prevent cardiovascular complications:\n\n• Dietary Approaches (DASH diet): Emphasize vegetables, whole grains, and fruits while limiting sodium to under 2,000 mg/day.\n• Regular Physical Activity: Strive for at least 150 minutes of moderate-intensity exercise weekly.\n• Stress Reduction: Incorporate daily breathing exercises or mindfulness.\n• Medication Adherence: Always take prescribed antihypertensives at the same scheduled time every day.\n\n### Critical Notice:\nIf you experience severe headache, vision changes, or chest pain, seek immediate emergency care.",
                                isUser = false,
                                intent = "chronic_disease_guidance",
                                confidence = 0.98
                            )
                        )
                    }

                    PreviewChatComposer(canSend = false)
                }
            }
        }
    }
}

// =============================================================
// 4. LOADING / THINKING STATE PREVIEW
// =============================================================

@Preview(name = "4. Thinking State", showBackground = true, widthDp = 380, heightDp = 640)
@Composable
fun ChatbotThinkingStatePreview() {
    AIBasedMedicalChatbotTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Ivory)
        ) {
            PreviewChatbotHeader(isThinking = true)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                color = PureWhite,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, Mint.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MessageBubble(
                            message = ChatMessage(
                                text = "What is the recommended dosage for Paracetamol 500mg?",
                                isUser = true
                            )
                        )

                        TypingIndicator()
                    }

                    PreviewChatComposer(canSend = false)
                }
            }
        }
    }
}

// =============================================================
// 5. NETWORK ERROR AND RETRY STATE PREVIEW
// =============================================================

@Preview(name = "5. Network Error & Retry", showBackground = true, widthDp = 380, heightDp = 640)
@Composable
fun ChatbotNetworkErrorPreview() {
    AIBasedMedicalChatbotTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Ivory)
        ) {
            PreviewChatbotHeader(isThinking = false)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                color = PureWhite,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, Mint.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MessageBubble(
                            message = ChatMessage(
                                text = "Tell me about Azithromycin uses",
                                isUser = true
                            )
                        )

                        MessageBubble(
                            message = ChatMessage(
                                text = "The connection to MedAssist AI timed out. The medical model may still be generating or the server is busy. Tap Retry to try again.",
                                isUser = false,
                                isError = true
                            ),
                            onRetry = {}
                        )
                    }

                    PreviewChatComposer(canSend = false)
                }
            }
        }
    }
}

// =============================================================
// 6. ATTACHMENT PREVIEW AND COMPOSER PREVIEW
// =============================================================

@Preview(name = "6. Attachment & Composer", showBackground = true, widthDp = 380, heightDp = 640)
@Composable
fun ChatbotComposerWithAttachmentPreview() {
    AIBasedMedicalChatbotTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Ivory)
        ) {
            PreviewChatbotHeader(isThinking = false)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                color = PureWhite,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, Mint.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MessageBubble(
                            message = ChatMessage(
                                text = "Please inspect this prescription image and summarize the morning dosage.",
                                isUser = true
                            )
                        )
                    }

                    PreviewChatComposer(
                        text = "Attached prescription image for review",
                        hasImageAttachment = true,
                        canSend = true
                    )
                }
            }
        }
    }
}

// =============================================================
// 7. NARROW SCREEN PREVIEW (320dp width)
// =============================================================

@Preview(name = "7. Narrow Screen (320dp)", showBackground = true, widthDp = 320, heightDp = 600)
@Composable
fun ChatbotNarrowScreenPreview() {
    ChatbotEmptyWelcomePreview()
}

// =============================================================
// 8. LARGE FONT SCALE PREVIEW (1.3x)
// =============================================================

@Preview(name = "8. Large Font Scale (1.3x)", showBackground = true, widthDp = 380, heightDp = 640, fontScale = 1.3f)
@Composable
fun ChatbotLargeFontPreview() {
    ChatbotConversationPreview()
}
