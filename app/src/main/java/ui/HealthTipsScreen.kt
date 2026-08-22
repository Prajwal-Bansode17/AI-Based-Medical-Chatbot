package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// ============================================================
// THEME COLORS
// Same as Symptoms Checker
// ============================================================

private val BackgroundColor = Color(0xFF071A33)
private val CardColor = Color(0xFF123653)
private val PrimaryTextColor = Color.White
private val SecondaryTextColor = Color(0xFFB9EAF2)
private val AccentColor = Color(0xFF087EA4)
private val LightAccentColor = Color(0xFF7DE8F0)


// ============================================================
// HEALTH TIP MODEL
// ============================================================

data class HealthTip(
    val title: String,
    val description: String,
    val icon: String
)


// ============================================================
// HEALTH TIPS
// ============================================================

private val healthTips = listOf(

    HealthTip(
        title = "Stay Hydrated",
        description = "Drink adequate water throughout the day and pay attention to increased fluid needs during hot weather or physical activity.",
        icon = "💧"
    ),

    HealthTip(
        title = "Eat a Balanced Diet",
        description = "Include a variety of fruits, vegetables, whole grains, protein sources and other nutritious foods in your regular meals.",
        icon = "🥗"
    ),

    HealthTip(
        title = "Stay Physically Active",
        description = "Regular physical activity can support cardiovascular health, strength, mobility and overall wellbeing.",
        icon = "🏃"
    ),

    HealthTip(
        title = "Get Enough Sleep",
        description = "Maintain a regular sleep schedule and create a comfortable environment that supports good-quality sleep.",
        icon = "😴"
    ),

    HealthTip(
        title = "Maintain Personal Hygiene",
        description = "Regular handwashing, dental care and general personal hygiene can help reduce the spread of infections.",
        icon = "🧼"
    ),

    HealthTip(
        title = "Manage Stress",
        description = "Consider healthy activities such as exercise, relaxation techniques, hobbies and spending time with supportive people.",
        icon = "🧘"
    ),

    HealthTip(
        title = "Avoid Smoking",
        description = "Avoiding tobacco and exposure to tobacco smoke can reduce the risk of many serious health problems.",
        icon = "🚭"
    ),

    HealthTip(
        title = "Regular Health Checkups",
        description = "Routine health checkups can help identify potential health concerns early. Follow recommendations from qualified healthcare professionals.",
        icon = "🩺"
    )
)


// ============================================================
// HEALTH TIPS SCREEN
// ============================================================

@Composable
fun HealthTipsScreen(
    onBackClick: () -> Unit
) {

    Column(

        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {

        // =====================================================
        // HEADER
        // =====================================================

        Row(

            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 10.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBackClick
            ) {

                Icon(
                    imageVector =
                        Icons.AutoMirrored.Filled.ArrowBack,

                    contentDescription =
                        "Back",

                    tint =
                        PrimaryTextColor
                )
            }

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Column {

                Text(
                    text = "Health Tips",

                    color =
                        PrimaryTextColor,

                    fontSize = 20.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text = "Simple habits for better wellbeing",

                    color =
                        SecondaryTextColor,

                    fontSize = 12.sp
                )
            }
        }


        // =====================================================
        // CONTENT
        // =====================================================

        LazyColumn(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            item {

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = "Healthy Habits",

                    color =
                        PrimaryTextColor,

                    fontSize = 22.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text =
                        "Simple habits that can support general health and wellbeing.",

                    color =
                        SecondaryTextColor,

                    fontSize = 14.sp
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }


            // =================================================
            // HEALTH TIP CARDS
            // =================================================

            items(healthTips) { tip ->

                HealthTipCard(
                    tip = tip
                )
            }


            // =================================================
            // DISCLAIMER
            // =================================================

            item {

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Card(

                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(16.dp),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color(0xFF10283D)
                        )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = "⚠ Medical Information",

                            color =
                                LightAccentColor,

                            fontSize = 14.sp,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(5.dp)
                        )

                        Text(
                            text =
                                "Health tips are for general educational purposes and do not replace professional medical advice.",

                            color =
                                SecondaryTextColor,

                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )
            }
        }
    }
}


// ============================================================
// HEALTH TIP CARD
// ============================================================

@Composable
private fun HealthTipCard(
    tip: HealthTip
) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(16.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    CardColor
            )
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),

            verticalAlignment =
                Alignment.Top
        ) {

            // =================================================
            // ICON
            // =================================================

            Text(
                text = tip.icon,

                fontSize = 30.sp
            )

            Spacer(
                modifier =
                    Modifier.width(14.dp)
            )


            // =================================================
            // TEXT
            // =================================================

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = tip.title,

                    color =
                        PrimaryTextColor,

                    fontSize = 17.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(7.dp)
                )

                Text(
                    text = tip.description,

                    color =
                        SecondaryTextColor,

                    fontSize = 13.sp,

                    lineHeight = 20.sp
                )
            }
        }
    }
}