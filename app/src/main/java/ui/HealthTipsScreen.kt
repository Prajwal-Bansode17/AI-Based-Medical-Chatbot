package ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_based_medical_chatbot.LocalAppLanguageController
import com.example.ai_based_medical_chatbot.appText
import com.example.ai_based_medical_chatbot.ui.theme.DarkTeal
import com.example.ai_based_medical_chatbot.ui.theme.DeepTeal
import com.example.ai_based_medical_chatbot.ui.theme.Ivory
import com.example.ai_based_medical_chatbot.ui.theme.Mint
import com.example.ai_based_medical_chatbot.ui.theme.PureWhite
import com.example.ai_based_medical_chatbot.ui.theme.SoftMint
import com.example.ai_based_medical_chatbot.ui.theme.TextDark
import com.example.ai_based_medical_chatbot.ui.theme.TextMuted

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

    val appLanguageController = LocalAppLanguageController.current
    val selectedLanguage = appLanguageController.selectedLanguage

    val localizedHealthTips = healthTips.mapIndexed { index, tip ->
        HealthTip(
            title = appText("health_tip_${index + 1}_title", selectedLanguage),
            description = appText("health_tip_${index + 1}_description", selectedLanguage)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Ivory,
                        SoftMint
                    )
                )
            )
    ) {

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
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

                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(44.dp)
                            .background(PureWhite, RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, Mint), RoundedCornerShape(12.dp)),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = PureWhite,
                            contentColor = DarkTeal
                        )
                    ) {

                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DarkTeal,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(12.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = appText("health_tips_title", selectedLanguage),
                            color = DarkTeal,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(2.dp)
                        )

                        Text(
                            text = appText("health_tips_subtitle", selectedLanguage),
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // HERO CARD
            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DarkTeal
                    ),
                    border = BorderStroke(1.dp, Mint.copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 2.dp
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
                                .size(56.dp)
                                .background(
                                    PureWhite.copy(alpha = 0.15f),
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = HeartPulseVectorIcon,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(
                            modifier = Modifier.width(14.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = appText("health_tips_hero_title", selectedLanguage),
                                color = PureWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Text(
                                text = appText("health_tips_hero_description", selectedLanguage),
                                color = PureWhite.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                lineHeight = 18.sp
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
                            text = appText("health_tips_daily_title", selectedLanguage),
                            color = DarkTeal,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(2.dp)
                        )

                        Text(
                            text = appText("health_tips_daily_subtitle", selectedLanguage),
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                SoftMint,
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                BorderStroke(1.dp, Mint),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(
                                horizontal = 10.dp,
                                vertical = 4.dp
                            )
                    ) {

                        Text(
                            text = "${healthTips.size} ${appText("health_tips_count", selectedLanguage)}",
                            color = DeepTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 20 HEALTH TIPS
            itemsIndexed(
                items = localizedHealthTips,
                key = { index, _ -> index }
            ) { index, tip ->

                HealthTipCard(
                    tip = tip
                )
            }

            // DISCLAIMER
            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SoftMint
                    ),
                    border = BorderStroke(1.dp, Mint)
                ) {

                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {

                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Disclaimer",
                            tint = DeepTeal,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(10.dp)
                        )

                        Text(
                            text = appText("health_tips_disclaimer", selectedLanguage),
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // FOOTER
            item {

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = HeartPulseVectorIcon,
                        contentDescription = null,
                        tint = DeepTeal,
                        modifier = Modifier.size(14.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(6.dp)
                    )

                    Text(
                        text = "MEDASSIST AI",
                        color = DarkTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }
        }
    }
}

@Composable
private fun HealthTipCard(
    tip: HealthTip
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = PureWhite
        ),
        border = BorderStroke(1.dp, Mint),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
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
                    .size(44.dp)
                    .background(
                        SoftMint,
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        BorderStroke(1.dp, Mint.copy(alpha = 0.6f)),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = HeartPulseVectorIcon,
                    contentDescription = null,
                    tint = DeepTeal,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(
                modifier = Modifier.width(14.dp)
            )

            // CONTENT
            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = tip.title,
                    color = DarkTeal,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = tip.description,
                    color = TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}