package ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_based_medical_chatbot.ui.theme.DarkTeal
import com.example.ai_based_medical_chatbot.ui.theme.DeepTeal
import com.example.ai_based_medical_chatbot.ui.theme.Ivory
import com.example.ai_based_medical_chatbot.ui.theme.Mint
import com.example.ai_based_medical_chatbot.ui.theme.PureWhite
import com.example.ai_based_medical_chatbot.ui.theme.SageGreen
import com.example.ai_based_medical_chatbot.ui.theme.SoftMint
import com.example.ai_based_medical_chatbot.ui.theme.TextDark
import com.example.ai_based_medical_chatbot.ui.theme.TextMuted

@Composable
fun HealthTipDetailScreen(
    tipId: Int,
    screenLanguage: String,
    onBackClick: () -> Unit,
    onLanguageChange: (String) -> Unit = {},
    onRelatedTipClick: (Int) -> Unit = {}
) {
    BackHandler { onBackClick() }

    val tip = remember(tipId) { HealthTipsRepository.getTipById(tipId) }
    val detailBundle = remember(tipId) { HealthTipsRecommendations.getTipDetails(tipId) }
    val labels = remember(screenLanguage) { HealthTipsRecommendations.getDetailLabels(screenLanguage) }

    var languageMenuExpanded by remember { mutableStateOf(false) }

    if (tip == null || detailBundle == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Ivory),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Health tip not found",
                    color = TextDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(44.dp)
                        .background(PureWhite, RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, Mint), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DarkTeal
                    )
                }
            }
        }
        return
    }

    val content = tip.getContent(screenLanguage)
    val details = detailBundle.getLocalized(screenLanguage)
    val categoryTitle = HealthTipsRepository.getCategoryTitle(tip.category, screenLanguage)
    val categoryIcon = HealthTipsRepository.getCategoryIcon(tip.category)
    val relatedTips = remember(tipId) { HealthTipsRecommendations.getRelatedTips(tipId) }

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
                start = 18.dp,
                top = 16.dp,
                end = 18.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // 1. TOP APP BAR WITH BACK & LANGUAGE PILL
            // ==========================================
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

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = labels["tip_details_screen_title"] ?: "Health Tip Details",
                            color = DarkTeal,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = categoryTitle,
                            color = DeepTeal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Language Selector Pill
                    Box {
                        val langShortCode = when (screenLanguage.lowercase().trim()) {
                            "mr", "marathi", "मराठी" -> "MR ▾"
                            "hi", "hindi", "हिंदी" -> "HI ▾"
                            else -> "EN ▾"
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(PureWhite)
                                .border(BorderStroke(1.dp, Mint), RoundedCornerShape(20.dp))
                                .clickable { languageMenuExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = langShortCode,
                                color = DarkTeal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        DropdownMenu(
                            expanded = languageMenuExpanded,
                            onDismissRequest = { languageMenuExpanded = false },
                            modifier = Modifier
                                .background(PureWhite)
                                .border(BorderStroke(1.dp, Mint.copy(alpha = 0.6f)), RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "English",
                                        fontWeight = if (screenLanguage == "English") FontWeight.Bold else FontWeight.Normal,
                                        color = if (screenLanguage == "English") DeepTeal else TextDark
                                    )
                                },
                                onClick = {
                                    onLanguageChange("English")
                                    languageMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "मराठी (Marathi)",
                                        fontWeight = if (screenLanguage == "मराठी") FontWeight.Bold else FontWeight.Normal,
                                        color = if (screenLanguage == "मराठी") DeepTeal else TextDark
                                    )
                                },
                                onClick = {
                                    onLanguageChange("मराठी")
                                    languageMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "हिंदी (Hindi)",
                                        fontWeight = if (screenLanguage == "हिंदी") FontWeight.Bold else FontWeight.Normal,
                                        color = if (screenLanguage == "हिंदी") DeepTeal else TextDark
                                    )
                                },
                                onClick = {
                                    onLanguageChange("हिंदी")
                                    languageMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 2. HERO BANNER CARD
            // ==========================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkTeal),
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
                        Column(modifier = Modifier.fillMaxWidth()) {
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
                                            PureWhite.copy(alpha = 0.18f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = categoryIcon,
                                        contentDescription = null,
                                        tint = PureWhite,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = categoryTitle,
                                        color = PureWhite,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = String.format("#%02d", tip.id),
                                    color = PureWhite.copy(alpha = 0.85f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background(
                                            PureWhite.copy(alpha = 0.16f),
                                            RoundedCornerShape(16.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = categoryIcon,
                                        contentDescription = null,
                                        tint = PureWhite,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Text(
                                    text = content.title,
                                    color = PureWhite,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 25.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 3. SECTION A: OVERVIEW & WHY USEFUL
            // ==========================================
            item {
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
                            .padding(18.dp)
                    ) {
                        Text(
                            text = labels["overview_title"] ?: "Overview & Science",
                            color = DarkTeal,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = content.description,
                            color = TextDark.copy(alpha = 0.9f),
                            fontSize = 13.5.sp,
                            lineHeight = 21.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Why This Habit Matters Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    SoftMint.copy(alpha = 0.7f),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    BorderStroke(1.dp, Mint.copy(alpha = 0.8f)),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = HeartPulseVectorIcon,
                                        contentDescription = null,
                                        tint = DeepTeal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = labels["why_useful_label"] ?: "Why This Habit Matters",
                                        color = DeepTeal,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = details.whyUseful,
                                    color = TextDark,
                                    fontSize = 12.5.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 4. SECTION B: WHO MAY FIND THIS USEFUL?
            // ==========================================
            item {
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
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(SoftMint, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = DeepTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = labels["who_useful_title"] ?: "Who May Find This Useful?",
                                color = DarkTeal,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = labels["target_audience_label"] ?: "Target Audience & Scenarios",
                            color = DeepTeal,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        details.targetAudience.forEach { group ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .size(6.dp)
                                        .background(DeepTeal, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = group,
                                    color = TextDark.copy(alpha = 0.88f),
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = labels["relevant_concerns_label"] ?: "Related Wellness Concerns",
                            color = DeepTeal,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            details.relevantConcerns.forEach { concern ->
                                Box(
                                    modifier = Modifier
                                        .background(
                                            SoftMint,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .border(
                                            BorderStroke(1.dp, Mint),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = concern,
                                        color = DeepTeal,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 5. SECTION C: PERSONALIZED WELLNESS SUGGESTIONS
            // ==========================================
            item {
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
                            .padding(18.dp)
                    ) {
                        Text(
                            text = labels["personalized_suggestions_title"] ?: "Personalized Wellness Suggestions",
                            color = DarkTeal,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        details.personalizedSuggestions.forEachIndexed { idx, suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(SoftMint, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        color = DeepTeal,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = suggestion,
                                    color = TextDark,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = labels["suggestions_note"] ?: "Note: Individual wellness needs vary.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // ==========================================
            // 6. SECTION D: KEY BENEFITS
            // ==========================================
            item {
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
                            .padding(18.dp)
                    ) {
                        Text(
                            text = labels["benefits_title"] ?: "Key Wellness Benefits",
                            color = DarkTeal,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        details.detailedBenefits.forEach { benefit ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = BenefitCheckVectorIcon,
                                    contentDescription = null,
                                    tint = DeepTeal,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = benefit,
                                    color = TextDark,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 7. SECTION E: PRECAUTIONS & SAFETY
            // ==========================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftMint.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, Mint)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = DeepTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = labels["precautions_title"] ?: "Precautions & Safety",
                                color = DarkTeal,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        details.precautions.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .size(5.dp)
                                        .background(DeepTeal, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = item,
                                    color = TextDark,
                                    fontSize = 12.5.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // When to consult doctor
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PureWhite, RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, Mint), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = labels["when_consult_title"] ?: "When to Consult a Physician",
                                    color = DeepTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = details.whenToConsultDoctor,
                                    color = TextDark,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Emergency Warning
                        Text(
                            text = labels["emergency_alert"] ?: "Emergency: Seek emergency hospital care for severe acute symptoms.",
                            color = DeepTeal,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ==========================================
            // 8. SECTION F: RELATED WELLNESS TIPS
            // ==========================================
            if (relatedTips.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = labels["related_tips_title"] ?: "Related Wellness Tips",
                            color = DarkTeal,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        relatedTips.forEach { relTip ->
                            val relContent = relTip.getContent(screenLanguage)
                            val relCatTitle = HealthTipsRepository.getCategoryTitle(relTip.category, screenLanguage)
                            val relCatIcon = HealthTipsRepository.getCategoryIcon(relTip.category)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onRelatedTipClick(relTip.id) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = PureWhite),
                                border = BorderStroke(1.dp, Mint),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(SoftMint, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = relCatIcon,
                                            contentDescription = null,
                                            tint = DeepTeal,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = relCatTitle,
                                            color = DeepTeal,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = relContent.title,
                                            color = DarkTeal,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "View",
                                        tint = DeepTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 9. DISCLAIMER BANNER & FOOTER
            // ==========================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftMint),
                    border = BorderStroke(1.dp, Mint)
                ) {
                    Text(
                        text = labels["disclaimer_banner"] ?: "Clinical Notice: General informational guidance.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = HeartPulseVectorIcon,
                        contentDescription = null,
                        tint = DeepTeal,
                        modifier = Modifier.size(13.dp)
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
            }
        }
    }
}
