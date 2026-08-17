package ui

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

    val background = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF071A33),
            Color(0xFF0A2D4F),
            Color(0xFF064E63)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(20.dp)
    ) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            // ------------------------------------------------
            // TOP SECTION
            // ------------------------------------------------

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column {

                    Text(
                        text = "Hello, $userName 👋",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = "How can I help you today?",
                        color = Color(0xFFB9EAF2),
                        fontSize = 14.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color(0xFF00A9E8),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            // ------------------------------------------------
            // AI CHATBOT CARD
            // ------------------------------------------------

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onChatbotClick()
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0D5C70)
                )
            ) {

                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = Color.White,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "AI Medical Assistant",
                            tint = Color(0xFF00A9E8),
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(
                        modifier = Modifier.size(16.dp)
                    )

                    Column {

                        Text(
                            text = "AI Medical Assistant",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = "Ask your health-related questions",
                            color = Color(0xFFB9EAF2),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            // ------------------------------------------------
            // QUICK SERVICES
            // ------------------------------------------------

            Text(
                text = "Quick Services",
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            // ------------------------------------------------
            // FIRST ROW
            // ------------------------------------------------

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                DashboardServiceCard(
                    title = "Symptoms",
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

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            // ------------------------------------------------
            // SECOND ROW
            // ------------------------------------------------

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                DashboardServiceCard(
                    title = "Health Tips",
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

                // Empty space to keep the layout balanced.
                Spacer(
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ------------------------------------------------
// REUSABLE SERVICE CARD
// ------------------------------------------------

@Composable
private fun DashboardServiceCard(
    title: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {

    Card(
        modifier = modifier.clickable {
            onClick()
        },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            icon()

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = title,
                color = Color(0xFF071A33),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}