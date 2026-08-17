package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DashboardScreen(
    userName: String,
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

    val background = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF06152B),
            Color(0xFF082A46),
            Color(0xFF064E63)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {

            // =================================================
            // HEADER
            // =================================================

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    animationSpec = tween(600)
                ) + slideInVertically(
                    initialOffsetY = { -80 },
                    animationSpec = tween(600)
                )
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Hello, $userName 👋",
                            color = Color.White,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(5.dp)
                        )

                        Text(
                            text = "How can I help you today?",
                            color = Color(0xFFB9EAF2),
                            fontSize = 14.sp
                        )
                    }

                    // Profile icon
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                color = Color.White,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color(0xFF087EA4),
                            modifier = Modifier.size(29.dp)
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            // =================================================
            // AI ASSISTANT CARD
            // =================================================

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 650,
                        delayMillis = 150
                    )
                ) + slideInVertically(
                    initialOffsetY = { 80 },
                    animationSpec = tween(
                        durationMillis = 650,
                        delayMillis = 150
                    )
                )
            ) {

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onChatbotClick()
                        },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0D6175)
                    )
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // AI icon
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .background(
                                    color = Color.White,
                                    shape = RoundedCornerShape(18.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {

                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "AI Medical Assistant",
                                tint = Color(0xFF00A9E8),
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Spacer(
                            modifier = Modifier.size(16.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = "AI Medical Assistant",
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(5.dp)
                            )

                            Text(
                                text = "Ask your health-related questions",
                                color = Color(0xFFB9EAF2),
                                fontSize = 13.sp
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Text(
                                text = "Tap to chat →",
                                color = Color(0xFF7DE8F0),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            // =================================================
            // QUICK SERVICES TITLE
            // =================================================

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 600,
                        delayMillis = 300
                    )
                )
            ) {

                Column {

                    Text(
                        text = "Quick Services",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    Text(
                        text = "Choose a service to get started",
                        color = Color(0xFF8FCBD5),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            // =================================================
            // SERVICE ROW 1
            // =================================================

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 600,
                        delayMillis = 400
                    )
                ) + slideInVertically(
                    initialOffsetY = { 70 },
                    animationSpec = tween(
                        durationMillis = 600,
                        delayMillis = 400
                    )
                )
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    DashboardServiceCard(
                        title = "Symptoms",
                        subtitle = "Check symptoms",
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Symptoms Checker",
                                tint = Color(0xFF00A9E8),
                                modifier = Modifier.size(30.dp)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        onClick = onSymptomsClick
                    )

                    DashboardServiceCard(
                        title = "Medicine",
                        subtitle = "Medicine info",
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Medicine Information",
                                tint = Color(0xFF00A9E8),
                                modifier = Modifier.size(30.dp)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        onClick = onMedicineClick
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            // =================================================
            // SERVICE ROW 2
            // =================================================

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 600,
                        delayMillis = 500
                    )
                ) + slideInVertically(
                    initialOffsetY = { 70 },
                    animationSpec = tween(
                        durationMillis = 600,
                        delayMillis = 500
                    )
                )
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    DashboardServiceCard(
                        title = "Health Tips",
                        subtitle = "Healthy lifestyle",
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Health Tips",
                                tint = Color(0xFF00A9E8),
                                modifier = Modifier.size(30.dp)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        onClick = onHealthTipsClick
                    )

                    // Empty card space
                    Spacer(
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(
                modifier = Modifier.weight(1f)
            )

            // =================================================
            // FOOTER
            // =================================================

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 700,
                        delayMillis = 650
                    )
                )
            ) {

                Text(
                    text = "MedAssist AI • Your health companion",
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF6FAEB8),
                    fontSize = 11.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}


// =========================================================
// REUSABLE SERVICE CARD
// =========================================================

@Composable
private fun DashboardServiceCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {

    Card(
        modifier = modifier
            .height(145.dp)
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Icon container
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = Color(0xFFE8F8FC),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                icon()
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = title,
                color = Color(0xFF071A33),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text = subtitle,
                color = Color(0xFF607D8B),
                fontSize = 11.sp
            )
        }
    }
}