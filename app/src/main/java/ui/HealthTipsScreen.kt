package ui

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class HealthTip(
    val title: String,
    val description: String
)

private val healthTips = listOf(

    HealthTip(
        title = "Stay Hydrated",
        description =
            "Drink adequate water throughout the day and pay attention to increased fluid needs during hot weather or physical activity."
    ),

    HealthTip(
        title = "Eat a Balanced Diet",
        description =
            "Include a variety of fruits, vegetables, whole grains, protein sources and other nutritious foods in your regular meals."
    ),

    HealthTip(
        title = "Stay Physically Active",
        description =
            "Regular physical activity can support cardiovascular health, strength, mobility and overall wellbeing."
    ),

    HealthTip(
        title = "Get Enough Sleep",
        description =
            "Maintain a regular sleep schedule and create a comfortable environment that supports good-quality sleep."
    ),

    HealthTip(
        title = "Maintain Personal Hygiene",
        description =
            "Regular handwashing, dental care and general personal hygiene can help reduce the spread of infections."
    ),

    HealthTip(
        title = "Manage Stress",
        description =
            "Consider healthy activities such as exercise, relaxation techniques, hobbies and spending time with supportive people."
    ),

    HealthTip(
        title = "Avoid Smoking",
        description =
            "Avoiding tobacco and exposure to tobacco smoke can reduce the risk of many serious health problems."
    ),

    HealthTip(
        title = "Regular Health Checkups",
        description =
            "Routine health checkups can help identify potential health concerns early. Follow recommendations from qualified healthcare professionals."
    ),

    HealthTip(
        title = "Limit Excess Sugar",
        description =
            "Reduce frequent consumption of sugary drinks, sweets and highly processed foods as part of a balanced eating pattern."
    ),

    HealthTip(
        title = "Eat More Fruits & Vegetables",
        description =
            "Include a variety of fruits and vegetables in your meals to support a nutritious and balanced diet."
    ),

    HealthTip(
        title = "Take Regular Breaks",
        description =
            "If you spend long periods sitting or working on screens, take regular short breaks and move around."
    ),

    HealthTip(
        title = "Practice Good Posture",
        description =
            "Maintain a comfortable posture while sitting, standing or using digital devices and avoid staying in one position for too long."
    ),

    HealthTip(
        title = "Protect Your Skin",
        description =
            "Protect your skin from excessive sun exposure and use appropriate sun protection when spending time outdoors."
    ),

    HealthTip(
        title = "Keep Your Surroundings Clean",
        description =
            "Keeping living and working spaces clean can support good hygiene and help maintain a healthier environment."
    ),

    HealthTip(
        title = "Stay Connected",
        description =
            "Maintain healthy relationships with family, friends and supportive people as part of overall wellbeing."
    ),

    HealthTip(
        title = "Spend Time Outdoors",
        description =
            "Regular outdoor activities can encourage movement, relaxation and a healthy daily routine."
    ),

    HealthTip(
        title = "Avoid Excess Alcohol",
        description =
            "Avoiding or limiting alcohol can reduce health risks associated with excessive alcohol consumption."
    ),

    HealthTip(
        title = "Listen to Your Body",
        description =
            "Pay attention to persistent or unusual symptoms and seek advice from a qualified healthcare professional when needed."
    ),

    HealthTip(
        title = "Keep Medicines Organized",
        description =
            "Store medicines safely, follow prescribed instructions and check labels before taking them."
    ),

    HealthTip(
        title = "Wash Your Hands Regularly",
        description =
            "Wash your hands with soap and water regularly, especially before eating and after using the restroom."
    )
)

@Composable
fun HealthTipsScreen(
    onBackClick: () -> Unit
) {

    val primaryBlue = Color(0xFF087EA4)
    val darkBlue = Color(0xFF123A56)
    val teal = Color(0xFF18A6A6)
    val backgroundTop = Color(0xFFEAF8FC)
    val backgroundBottom = Color(0xFFD8F0F6)
    val gray = Color(0xFF71818C)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        backgroundTop,
                        backgroundBottom
                    )
                )
            )
            .statusBarsPadding()
    ) {

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),

            contentPadding = PaddingValues(
                start = 20.dp,
                top = 18.dp,
                end = 20.dp,
                bottom = 30.dp
            ),

            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // HEADER
            item {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(45.dp)
                            .background(
                                Color.White,
                                CircleShape
                            )
                            .clickable {
                                onBackClick()
                            },

                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "‹",
                            color = darkBlue,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Light
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(12.dp)
                    )

                    Column {

                        Text(
                            text = "Health Tips",
                            color = darkBlue,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(3.dp)
                        )

                        Text(
                            text = "Simple habits for a healthier lifestyle",
                            color = gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // HERO CARD
            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(25.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = primaryBlue
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 5.dp
                    )
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),

                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(55.dp)
                                .background(
                                    Color.White.copy(alpha = 0.18f),
                                    CircleShape
                                ),

                            contentAlignment = Alignment.Center
                        ) {

                            Text(
                                text = "♥",
                                color = Color.White,
                                fontSize = 27.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(
                            modifier = Modifier.width(15.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = "Build Better Habits",
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(5.dp)
                            )

                            Text(
                                text = "Small healthy choices every day can support your overall wellbeing.",
                                color = Color.White.copy(alpha = 0.88f),
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }

            // DAILY WELLNESS
            item {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Daily Wellness",
                            color = darkBlue,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(3.dp)
                        )

                        Text(
                            text = "Explore simple health habits",
                            color = gray,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                Color.White,
                                RoundedCornerShape(20.dp)
                            )
                            .padding(
                                horizontal = 12.dp,
                                vertical = 7.dp
                            )
                    ) {

                        Text(
                            text = "${healthTips.size} Tips",
                            color = primaryBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 20 HEALTH TIPS
            items(healthTips) { tip ->

                HealthTipCard(
                    tip = tip,
                    primaryBlue = primaryBlue,
                    darkBlue = darkBlue,
                    gray = gray
                )
            }

            // DISCLAIMER
            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(18.dp),

                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),

                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    )
                ) {

                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {

                        Text(
                            text = "⚠",
                            color = Color(0xFFE49A25),
                            fontSize = 22.sp
                        )

                        Spacer(
                            modifier = Modifier.width(10.dp)
                        )

                        Text(
                            text = "Health tips are for general educational purposes and do not replace professional medical advice.",
                            color = gray,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // FOOTER
            item {

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "♥",
                        color = teal,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.width(6.dp)
                    )

                    Text(
                        text = "MEDASSIST AI",
                        color = darkBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthTipCard(
    tip: HealthTip,
    primaryBlue: Color,
    darkBlue: Color,
    gray: Color
) {

    val symbol = when (tip.title) {

        "Stay Hydrated" -> "💧"

        "Eat a Balanced Diet" -> "🥗"

        "Stay Physically Active" -> "🏃"

        "Get Enough Sleep" -> "😴"

        "Maintain Personal Hygiene" -> "🧼"

        "Manage Stress" -> "🧘"

        "Avoid Smoking" -> "🚭"

        "Regular Health Checkups" -> "🩺"

        "Limit Excess Sugar" -> "🍬"

        "Eat More Fruits & Vegetables" -> "🍎"

        "Take Regular Breaks" -> "☕"

        "Practice Good Posture" -> "🧍"

        "Protect Your Skin" -> "☀"

        "Keep Your Surroundings Clean" -> "🏠"

        "Stay Connected" -> "👥"

        "Spend Time Outdoors" -> "🌳"

        "Avoid Excess Alcohol" -> "⚠"

        "Listen to Your Body" -> "♥"

        "Keep Medicines Organized" -> "💊"

        "Wash Your Hands Regularly" -> "🧼"

        else -> "✓"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(20.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),

            verticalAlignment = Alignment.Top
        ) {

            // SYMBOL

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color(0xFFEAF8FC),
                        CircleShape
                    ),

                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = symbol,
                    fontSize = 23.sp
                )
            }

            Spacer(
                modifier = Modifier.width(13.dp)
            )

            // CONTENT

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = tip.title,
                    color = darkBlue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = tip.description,
                    color = gray,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}