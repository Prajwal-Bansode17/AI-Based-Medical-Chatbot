package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ai_based_medical_chatbot.ui.theme.SageGreen
import com.example.ai_based_medical_chatbot.ui.theme.SoftMint
import com.example.ai_based_medical_chatbot.ui.theme.TextDark
import com.example.ai_based_medical_chatbot.ui.theme.TextMuted

enum class HealthCategory(
    val key: String,
    val icon: ImageVector
) {
    ALL("cat_all", HeartPulseVectorIcon),
    NUTRITION("cat_nutrition", PillVectorIcon),
    FITNESS("cat_fitness", HeartPulseVectorIcon),
    SLEEP("cat_sleep", ClockVectorIcon),
    HYGIENE("cat_hygiene", StethoscopeVectorIcon),
    MENTAL("cat_mental", ChatVectorIcon),
    PREVENTION("cat_prevention", StethoscopeVectorIcon),
    LIFESTYLE("cat_lifestyle", ScaleVectorIcon)
}

data class HealthTip(
    val id: Int,
    val category: HealthCategory,
    val defaultTitle: String,
    val defaultDescription: String
)

private val healthTipsList = listOf(
    HealthTip(1, HealthCategory.NUTRITION, "Stay Hydrated", "Drink adequate water throughout the day (2.5-3 liters) to maintain electrolyte balance, digestion, and energy levels."),
    HealthTip(2, HealthCategory.NUTRITION, "Eat a Balanced Diet", "Fill half your plate with colorful vegetables and fruits, a quarter with lean protein, and a quarter with whole grains."),
    HealthTip(3, HealthCategory.FITNESS, "Stay Physically Active", "Engage in at least 30 minutes of moderate physical activity daily—brisk walking, cycling, or yoga keeps your heart strong."),
    HealthTip(4, HealthCategory.SLEEP, "Prioritize Quality Sleep", "Aim for 7 to 8 hours of uninterrupted restful sleep every night. Keep a consistent sleep schedule to support cellular recovery."),
    HealthTip(5, HealthCategory.HYGIENE, "Maintain Personal Hygiene", "Wash your hands frequently with soap and water, practice daily oral hygiene, and sanitize everyday contact surfaces."),
    HealthTip(6, HealthCategory.MENTAL, "Manage Daily Stress", "Practice mindful deep breathing, meditation, or spend time in nature. Chronic stress elevates cortisol and blood pressure."),
    HealthTip(7, HealthCategory.PREVENTION, "Avoid Smoking & Tobacco", "Quitting tobacco drastically cuts the risk of cardiovascular diseases, lung illness, and stroke within weeks."),
    HealthTip(8, HealthCategory.PREVENTION, "Routine Health Screenings", "Schedule annual blood tests, BP checks, and lipid panels. Early detection is the single most powerful tool in modern medicine."),
    HealthTip(9, HealthCategory.NUTRITION, "Limit Refined Sugars", "Minimize sweetened beverages, packaged juices, and refined sweets to protect insulin sensitivity and prevent weight spikes."),
    HealthTip(10, HealthCategory.NUTRITION, "Fresh Seasonal Produce", "Incorporate seasonal vegetables, leafy greens, and citrus fruits packed with bioflavonoids, fiber, and vital micronutrients."),
    HealthTip(11, HealthCategory.LIFESTYLE, "Take Active Micro-Breaks", "If you work sitting down, follow the 20-20-20 rule for eyes and stand up every 45 minutes to restore circulation."),
    HealthTip(12, HealthCategory.LIFESTYLE, "Practice Ergonomic Posture", "Keep your back straight, shoulders relaxed, and screens at eye level to prevent chronic cervical and lumbar strain."),
    HealthTip(13, HealthCategory.PREVENTION, "Sun Protection & Vitamin D", "Soak in 15 minutes of gentle morning sunlight for natural Vitamin D synthesis, while using UV protection during peak afternoon hours."),
    HealthTip(14, HealthCategory.HYGIENE, "Clean Living Spaces", "Ensure proper room ventilation, dust living areas regularly, and keep indoor air fresh to minimize respiratory allergens."),
    HealthTip(15, HealthCategory.MENTAL, "Stay Socially Connected", "Meaningful conversations with family and friends foster emotional resilience, reduce anxiety, and promote longevity."),
    HealthTip(16, HealthCategory.LIFESTYLE, "Spend Time in Nature", "Walking in green parks lowers resting pulse rate, eases cognitive fatigue, and naturally uplifts your mental mood."),
    HealthTip(17, HealthCategory.PREVENTION, "Avoid Excess Alcohol", "Limiting alcohol intake protects liver function, lowers cardiac arrhythmia risks, and prevents sleep cycle disruptions."),
    HealthTip(18, HealthCategory.PREVENTION, "Listen to Early Body Cues", "Never ignore persistent fatigue, unexplained weight shifts, or chronic headaches. Timely medical consultation saves lives."),
    HealthTip(19, HealthCategory.HYGIENE, "Safe Medication Storage", "Store medicines in cool, dry places away from sunlight. Check expiry dates and set reminders to avoid missed doses."),
    HealthTip(20, HealthCategory.HYGIENE, "Thorough Hand Washing", "Scrub hands for at least 20 seconds before meals and after outdoor trips to stop infectious pathogens before they enter your body.")
)

@Composable
fun HealthTipsScreen(
    onBackClick: () -> Unit
) {
    val appLanguageController = LocalAppLanguageController.current
    val selectedLanguage = appLanguageController.selectedLanguage

    var selectedCategory by remember { mutableStateOf(HealthCategory.ALL) }

    val filteredTips = remember(selectedCategory) {
        if (selectedCategory == HealthCategory.ALL) {
            healthTipsList
        } else {
            healthTipsList.filter { it.category == selectedCategory }
        }
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DarkTeal,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = appText("health_tips_title", selectedLanguage),
                            color = DarkTeal,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
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
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        DarkTeal,
                                        DeepTeal
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = appText("health_tips_hero_title", selectedLanguage),
                                    color = PureWhite,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = appText("health_tips_hero_description", selectedLanguage),
                                    color = PureWhite.copy(alpha = 0.88f),
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // CATEGORY FILTER CHIPS
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HealthCategory.values().forEach { cat ->
                            val isSelected = cat == selectedCategory
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) DeepTeal else PureWhite
                                    )
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isSelected) DeepTeal else Mint
                                        ),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) PureWhite else DeepTeal,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = appText(cat.key, selectedLanguage),
                                        color = if (isSelected) PureWhite else DarkTeal,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION HEADER WITH COUNT BADGE
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = appText("health_tips_daily_title", selectedLanguage),
                            color = DarkTeal,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
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
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${filteredTips.size} ${appText("health_tips_count", selectedLanguage)}",
                            color = DeepTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // HEALTH TIP CARDS
            itemsIndexed(
                items = filteredTips,
                key = { _, tip -> tip.id }
            ) { _, tip ->
                val localizedTitle = appText("health_tip_${tip.id}_title", selectedLanguage)
                    .takeIf { it.isNotBlank() && it != "health_tip_${tip.id}_title" }
                    ?: tip.defaultTitle
                val localizedDesc = appText("health_tip_${tip.id}_description", selectedLanguage)
                    .takeIf { it.isNotBlank() && it != "health_tip_${tip.id}_description" }
                    ?: tip.defaultDescription
                val categoryName = appText(tip.category.key, selectedLanguage)

                HealthTipCard(
                    id = tip.id,
                    title = localizedTitle,
                    description = localizedDesc,
                    categoryName = categoryName,
                    icon = tip.category.icon
                )
            }

            // DISCLAIMER CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftMint),
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
                        Spacer(modifier = Modifier.width(10.dp))
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
                Spacer(modifier = Modifier.height(4.dp))
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
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MEDASSIST AI • CLINICAL WELLNESS GUIDE",
                        color = DarkTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun HealthTipCard(
    id: Int,
    title: String,
    description: String,
    categoryName: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, Mint),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // CATEGORY PILL & ID BADGE ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .background(
                            SoftMint,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = DeepTeal,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = categoryName,
                        color = DeepTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = String.format("#%02d", id),
                    color = SageGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // TITLE AND DESCRIPTION
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            Ivory,
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            BorderStroke(1.dp, Mint),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = DarkTeal,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = DarkTeal,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        color = TextDark.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}
