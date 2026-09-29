package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_based_medical_chatbot.R
import com.example.ai_based_medical_chatbot.ui.theme.MedicalBackground
import com.example.ai_based_medical_chatbot.ui.theme.MedicalBlue
import com.example.ai_based_medical_chatbot.ui.theme.MedicalBlueDark
import com.example.ai_based_medical_chatbot.ui.theme.MedicalSurface
import com.example.ai_based_medical_chatbot.ui.theme.MedicalSurfaceVariant
import com.example.ai_based_medical_chatbot.ui.theme.MedicalTeal
import com.example.ai_based_medical_chatbot.ui.theme.MedicalTextPrimary
import com.example.ai_based_medical_chatbot.ui.theme.MedicalTextSecondary
import com.example.ai_based_medical_chatbot.ui.theme.PureWhite

@Composable
fun DashboardScreen(
    userName: String,
    onProfileClick: () -> Unit = {},
    onChatbotClick: () -> Unit = {},
    onBMIClick: () -> Unit = {},
    onSymptomsClick: () -> Unit = {},
    onMedicineClick: () -> Unit = {},
    onHealthTipsClick: () -> Unit = {},
    onPrescriptionClick: () -> Unit = {}
) {

    var showContent by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        showContent = true
    }

    val displayName = userName
        .trim()
        .ifBlank { "there" }

    val infiniteTransition = rememberInfiniteTransition(
        label = "dashboard_animation"
    )

    val botScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bot_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bot_glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8FCFF),
                        MedicalBackground,
                        Color(0xFFF2FAFC)
                    )
                )
            )
    ) {

        // Background decorative circles

        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.TopEnd)
                .background(
                    MedicalBlue.copy(alpha = 0.07f),
                    CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(170.dp)
                .align(Alignment.BottomStart)
                .background(
                    MedicalTeal.copy(alpha = 0.06f),
                    CircleShape
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),

            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 20.dp,
                bottom = 30.dp
            ),

            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            // =========================================================
            // HEADER
            // =========================================================

            item {

                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(
                        tween(500)
                    ) + slideInVertically(
                        initialOffsetY = { -30 },
                        animationSpec = tween(500)
                    )
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = "MEDASSIST AI",
                                color = MedicalBlue,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Text(
                                text = "Good day 👋",
                                color = MedicalTextSecondary,
                                fontSize = 13.sp
                            )

                            Spacer(
                                modifier = Modifier.height(3.dp)
                            )

                            Text(
                                text = "Hello, $displayName",
                                color = MedicalTextPrimary,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(
                                modifier = Modifier.height(3.dp)
                            )

                            Text(
                                text = "Your personal health AI",
                                color = MedicalTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(
                            modifier = Modifier.width(12.dp)
                        )

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .shadow(
                                    elevation = 6.dp,
                                    shape = CircleShape
                                )
                                .background(
                                    PureWhite,
                                    CircleShape
                                )
                                .clickable {
                                    onProfileClick()
                                },
                            contentAlignment = Alignment.Center
                        ) {

                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = MedicalBlue,
                                modifier = Modifier.size(29.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(
                    modifier = Modifier.height(22.dp)
                )
            }

            // =========================================================
            // AI ASSISTANT CARD
            // =========================================================

            item {

                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(
                        tween(
                            durationMillis = 600,
                            delayMillis = 100
                        )
                    ) + slideInVertically(
                        initialOffsetY = { 45 },
                        animationSpec = tween(
                            durationMillis = 600,
                            delayMillis = 100
                        )
                    )
                ) {

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onChatbotClick()
                            },
                        shape = RoundedCornerShape(30.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MedicalBlueDark
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 8.dp
                        )
                    ) {

                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            // Glow behind AI bot

                            Box(
                                modifier = Modifier
                                    .size(135.dp)
                                    .align(Alignment.TopEnd)
                                    .background(
                                        MedicalTeal.copy(
                                            alpha = glowAlpha
                                        ),
                                        CircleShape
                                    )
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    // AI Doctor Bot

                                    Box(
                                        modifier = Modifier
                                            .size(66.dp)
                                            .scale(botScale)
                                            .shadow(
                                                elevation = 7.dp,
                                                shape = CircleShape
                                            )
                                            .background(
                                                PureWhite,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {

                                        Text(
                                            text = "🤖",
                                            fontSize = 34.sp
                                        )
                                    }

                                    Spacer(
                                        modifier = Modifier.width(15.dp)
                                    )

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {

                                        Text(
                                            text = "MEDASSIST AI",
                                            color = PureWhite,
                                            fontSize = 21.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Spacer(
                                            modifier = Modifier.height(4.dp)
                                        )

                                        Text(
                                            text = "Your intelligent health companion",
                                            color = PureWhite.copy(
                                                alpha = 0.82f
                                            ),
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Spacer(
                                    modifier = Modifier.height(20.dp)
                                )

                                Text(
                                    text = "How can I help you today?",
                                    color = PureWhite,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(
                                    modifier = Modifier.height(11.dp)
                                )

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(17.dp),
                                    color = PureWhite.copy(
                                        alpha = 0.13f
                                    )
                                ) {

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = 15.dp,
                                                vertical = 13.dp
                                            ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {

                                        Text(
                                            text = "Ask anything about your health...",
                                            modifier = Modifier.weight(1f),
                                            color = PureWhite.copy(
                                                alpha = 0.72f
                                            ),
                                            fontSize = 13.sp
                                        )

                                        Box(
                                            modifier = Modifier
                                                .size(31.dp)
                                                .background(
                                                    PureWhite.copy(
                                                        alpha = 0.15f
                                                    ),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {

                                            Text(
                                                text = "→",
                                                color = PureWhite,
                                                fontSize = 19.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(
                    modifier = Modifier.height(26.dp)
                )
            }

            // =========================================================
            // QUICK ACTIONS TITLE
            // =========================================================

            item {

                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(
                        tween(700)
                    )
                ) {

                    Column {

                        Text(
                            text = "Quick Actions",
                            color = MedicalTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = "Choose a health tool",
                            color = MedicalTextSecondary,
                            fontSize = 12.sp
                        )

                        Spacer(
                            modifier = Modifier.height(14.dp)
                        )
                    }
                }
            }

            // =========================================================
            // BMI + SYMPTOMS
            // =========================================================

            item {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    DashboardActionCard(
                        title = "BMI Checker",
                        subtitle = "Check your BMI",
                        emoji = "⚖",
                        iconTint = MedicalBlue,
                        modifier = Modifier.weight(1f),
                        onClick = onBMIClick
                    )

                    DashboardActionCard(
                        title = "Symptoms",
                        subtitle = "Check symptoms",
                        emoji = "🩺",
                        iconTint = MedicalTeal,
                        modifier = Modifier.weight(1f),
                        onClick = onSymptomsClick
                    )
                }
            }

            item {
                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }

            // =========================================================
            // MEDICINE + HEALTH TIPS
            // =========================================================

            item {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    DashboardActionCard(
                        title = "Medicine",
                        subtitle = "Medicine info",
                        emoji = "💊",
                        iconTint = MedicalBlue,
                        modifier = Modifier.weight(1f),
                        onClick = onMedicineClick
                    )

                    DashboardActionCard(
                        title = "Health Tips",
                        subtitle = "Healthy lifestyle",
                        emoji = "💚",
                        iconTint = MedicalTeal,
                        modifier = Modifier.weight(1f),
                        onClick = onHealthTipsClick
                    )
                }
            }

            item {
                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }

            // =========================================================
            // PRESCRIPTION SCANNER
            // =========================================================

            item {

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onPrescriptionClick()
                        },
                    shape = RoundedCornerShape(23.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MedicalSurface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 5.dp
                    )
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 17.dp,
                                vertical = 15.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .background(
                                    MedicalBlue.copy(
                                        alpha = 0.10f
                                    ),
                                    RoundedCornerShape(17.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {

                            Text(
                                text = "▤",
                                color = MedicalBlue,
                                fontSize = 31.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(
                            modifier = Modifier.width(14.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = "Prescription Scanner",
                                color = MedicalTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(3.dp)
                            )

                            Text(
                                text = "Scan and organize your prescription",
                                color = MedicalTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    MedicalBlue.copy(
                                        alpha = 0.10f
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {

                            Text(
                                text = "→",
                                color = MedicalBlue,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Spacer(
                    modifier = Modifier.height(25.dp)
                )
            }

            // =========================================================
            // HEALTH INFORMATION
            // =========================================================

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MedicalBlue.copy(
                            alpha = 0.09f
                        )
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 0.dp
                    )
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(15.dp),
                        verticalAlignment = Alignment.Top
                    ) {

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    MedicalBlue.copy(
                                        alpha = 0.12f
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {

                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Health information",
                                tint = MedicalBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(
                            modifier = Modifier.width(11.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = "Health information",
                                color = MedicalBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(5.dp)
                            )

                            Text(
                                text = "MEDASSIST AI provides general health information and is not a replacement for professional medical advice.",
                                color = MedicalTextSecondary,
                                fontSize = 10.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            item {
                Spacer(
                    modifier = Modifier.height(22.dp)
                )
            }

            // =========================================================
            // FOOTER
            // =========================================================

            item {

                Text(
                    text = "MEDASSIST AI  •  Your intelligent health companion",
                    modifier = Modifier.fillMaxWidth(),
                    color = MedicalTextSecondary,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Text(
                    text = "Made for better health decisions",
                    modifier = Modifier.fillMaxWidth(),
                    color = MedicalTextSecondary.copy(
                        alpha = 0.7f
                    ),
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// =============================================================
// DASHBOARD ACTION CARD
// =============================================================

@Composable
private fun DashboardActionCard(
    title: String,
    subtitle: String,
    emoji: String,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Card(
        modifier = modifier
            .height(142.dp)
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(
            containerColor = MedicalSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        iconTint.copy(alpha = 0.10f),
                        RoundedCornerShape(17.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = emoji,
                    fontSize = 27.sp
                )
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = title,
                color = MedicalTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text = subtitle,
                color = MedicalTextSecondary,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}