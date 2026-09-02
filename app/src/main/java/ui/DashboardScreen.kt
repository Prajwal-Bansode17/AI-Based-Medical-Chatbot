package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_based_medical_chatbot.ui.theme.DarkBackground
import com.example.ai_based_medical_chatbot.ui.theme.MedicalBackground
import com.example.ai_based_medical_chatbot.ui.theme.MedicalBlue
import com.example.ai_based_medical_chatbot.ui.theme.MedicalBlueDark
import com.example.ai_based_medical_chatbot.ui.theme.MedicalBlueLight
import com.example.ai_based_medical_chatbot.ui.theme.MedicalBorder
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
    onSymptomsClick: () -> Unit = {},
    onMedicineClick: () -> Unit = {},
    onHealthTipsClick: () -> Unit = {}
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF7FCFF),
                        MedicalBackground
                    )
                )
            )
    ) {

        // ---------------------------------------------------------
        // Decorative background circles
        // ---------------------------------------------------------

        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.TopEnd)
                .background(
                    color = MedicalBlue.copy(alpha = 0.08f),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.BottomStart)
                .background(
                    color = MedicalTeal.copy(alpha = 0.06f),
                    shape = CircleShape
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 26.dp,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            // =====================================================
            // HEADER
            // =====================================================

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
                                style = MaterialTheme.typography.titleMedium,
                                color = MedicalBlue,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )

                            Spacer(
                                modifier = Modifier.height(9.dp)
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
                                fontSize = 27.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Text(
                                text = "Your personal health AI",
                                color = MedicalTextSecondary,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(
                            modifier = Modifier.width(12.dp)
                        )

                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .shadow(
                                    elevation = 5.dp,
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
                                modifier = Modifier.size(28.dp)
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

            // =====================================================
            // MAIN AI CARD
            // =====================================================

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
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MedicalBlueDark
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 8.dp
                        )
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                // MEDASSIST brand mark
                                Box(
                                    modifier = Modifier
                                        .size(58.dp)
                                        .clip(CircleShape)
                                        .background(
                                            PureWhite
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {

                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "MEDASSIST AI",
                                        tint = MedicalBlue,
                                        modifier = Modifier.size(31.dp)
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
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Spacer(
                                modifier = Modifier.height(19.dp)
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
                                    alpha = 0.14f
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

                                    Text(
                                        text = "→",
                                        color = PureWhite,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(
                    modifier = Modifier.height(25.dp)
                )
            }

            // =====================================================
            // QUICK ACTIONS
            // =====================================================

            item {

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
                    text = "Start with what you need",
                    color = MedicalTextSecondary,
                    fontSize = 12.sp
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )
            }

            item {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    DashboardActionCard(
                        title = "Ask AI",
                        subtitle = "Medical chat",
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "Ask AI",
                                tint = MedicalBlue,
                                modifier = Modifier.size(27.dp)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        onClick = onChatbotClick
                    )

                    DashboardActionCard(
                        title = "Symptoms",
                        subtitle = "Check symptoms",
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Symptoms",
                                tint = MedicalTeal,
                                modifier = Modifier.size(27.dp)
                            )
                        },
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

            item {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    DashboardActionCard(
                        title = "Medicine",
                        subtitle = "Medicine info",
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Medicine",
                                tint = MedicalBlue,
                                modifier = Modifier.size(27.dp)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        onClick = onMedicineClick
                    )

                    DashboardActionCard(
                        title = "Health Tips",
                        subtitle = "Healthy lifestyle",
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Health Tips",
                                tint = MedicalTeal,
                                modifier = Modifier.size(27.dp)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        onClick = onHealthTipsClick
                    )
                }
            }

            item {
                Spacer(
                    modifier = Modifier.height(25.dp)
                )
            }

            // =====================================================
            // RECENT / ASSISTANT INFORMATION
            // =====================================================

            item {

                Text(
                    text = "Your Assistant",
                    color = MedicalTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = PureWhite
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    )
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(17.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(
                                    MedicalSurfaceVariant,
                                    RoundedCornerShape(15.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {

                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Health",
                                tint = MedicalBlue,
                                modifier = Modifier.size(25.dp)
                            )
                        }

                        Spacer(
                            modifier = Modifier.width(13.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = "Need medical information?",
                                color = MedicalTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(
                                modifier = Modifier.height(3.dp)
                            )

                            Text(
                                text = "MEDASSIST can help you understand general health information.",
                                color = MedicalTextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            item {
                Spacer(
                    modifier = Modifier.height(18.dp)
                )
            }

            // =====================================================
            // DISCLAIMER
            // =====================================================

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MedicalSurfaceVariant
                    )
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(15.dp)
                    ) {

                        Text(
                            text = "⚕ Health information",
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

            item {
                Spacer(
                    modifier = Modifier.height(20.dp)
                )
            }

            item {

                Text(
                    text = "MEDASSIST AI  •  Your intelligent health companion",
                    modifier = Modifier.fillMaxWidth(),
                    color = MedicalTextSecondary,
                    fontSize = 10.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}


// =============================================================
// ACTION CARD
// =============================================================

@Composable
private fun DashboardActionCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Card(
        modifier = modifier
            .height(136.dp)
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(22.dp),
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
                    .size(50.dp)
                    .background(
                        MedicalSurfaceVariant,
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Spacer(
                modifier = Modifier.height(9.dp)
            )

            Text(
                text = title,
                color = MedicalTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = subtitle,
                color = MedicalTextSecondary,
                fontSize = 10.sp
            )
        }
    }
}


// =============================================================
// Small offset helpers
// =============================================================

