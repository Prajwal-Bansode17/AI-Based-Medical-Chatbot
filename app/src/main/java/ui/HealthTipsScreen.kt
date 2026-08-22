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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

private val healthTips =
    listOf(

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
        )
    )

@Composable
fun HealthTipsScreen(
    onBackClick: () -> Unit
) {

    val primary =
        Color(0xFF087EA4)

    val darkBlue =
        Color(0xFF123A56)

    val gray =
        Color(0xFF71818C)

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color(0xFFEAF8FC),
                                Color(0xFFD8F0F6)
                            )
                    )
                )
    ) {

        LazyColumn(
            modifier =
                Modifier.fillMaxSize(),

            contentPadding =
                PaddingValues(
                    start = 20.dp,
                    top = 18.dp,
                    end = 20.dp,
                    bottom = 25.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            // HEADER

            item {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(45.dp)
                                .background(
                                    Color.White,
                                    CircleShape
                                )
                                .clickable {
                                    onBackClick()
                                },

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text = "‹",
                            color =
                                darkBlue,
                            fontSize =
                                36.sp,
                            fontWeight =
                                FontWeight.Light
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.width(12.dp)
                    )

                    Column {

                        Text(
                            text =
                                "Health Tips",

                            color =
                                darkBlue,

                            fontSize =
                                22.sp,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(3.dp)
                        )

                        Text(
                            text =
                                "Simple habits for a healthier lifestyle",

                            color =
                                gray,

                            fontSize =
                                12.sp
                        )
                    }
                }
            }

            // HERO CARD

            item {

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(25.dp),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                primary
                        ),

                    elevation =
                        CardDefaults.cardElevation(
                            defaultElevation = 8.dp
                        )
                ) {

                    Row(
                        modifier =
                            Modifier.padding(20.dp),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .size(58.dp)
                                    .background(
                                        Color.White.copy(
                                            alpha = 0.18f
                                        ),
                                        RoundedCornerShape(
                                            17.dp
                                        )
                                    ),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Text(
                                text = "♥",
                                color =
                                    Color.White,
                                fontSize =
                                    29.sp
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.width(14.dp)
                        )

                        Column {

                            Text(
                                text =
                                    "Healthy Habits",

                                color =
                                    Color.White,

                                fontSize =
                                    19.sp,

                                fontWeight =
                                    FontWeight.Bold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )

                            Text(
                                text =
                                    "Simple steps that can support your wellbeing.",

                                color =
                                    Color.White.copy(
                                        alpha = 0.82f
                                    ),

                                fontSize =
                                    12.sp
                            )
                        }
                    }
                }
            }

            // SECTION TITLE

            item {

                Text(
                    text =
                        "Daily Wellness",

                    color =
                        darkBlue,

                    fontSize =
                        19.sp,

                    fontWeight =
                        FontWeight.Bold,

                    modifier =
                        Modifier.padding(
                            top = 3.dp
                        )
                )
            }

            // HEALTH TIPS

            items(
                healthTips
            ) { tip ->

                HealthTipCard(
                    tip =
                        tip
                )
            }

            // DISCLAIMER

            item {

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(18.dp),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color.White.copy(
                                    alpha = 0.88f
                                )
                        )
                ) {

                    Text(
                        text =
                            "⚠ Health tips are for general educational purposes and do not replace professional medical advice.",

                        modifier =
                            Modifier.padding(16.dp),

                        color =
                            gray,

                        fontSize =
                            11.sp,

                        lineHeight =
                            17.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthTipCard(
    tip: HealthTip
) {

    val primary =
        Color(0xFF087EA4)

    val darkBlue =
        Color(0xFF123A56)

    val gray =
        Color(0xFF71818C)

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(21.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 5.dp
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(17.dp),

            verticalAlignment =
                Alignment.Top
        ) {

            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .background(
                            Color(0xFFE8F7FB),
                            RoundedCornerShape(
                                15.dp
                            )
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = "♥",
                    color =
                        primary,
                    fontSize =
                        22.sp
                )
            }

            Spacer(
                modifier =
                    Modifier.width(13.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        tip.title,

                    color =
                        darkBlue,

                    fontSize =
                        16.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                Text(
                    text =
                        tip.description,

                    color =
                        gray,

                    fontSize =
                        12.sp,

                    lineHeight =
                        18.sp
                )
            }
        }
    }
}