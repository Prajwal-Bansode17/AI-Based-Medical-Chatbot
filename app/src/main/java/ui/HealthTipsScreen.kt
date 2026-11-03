package ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_based_medical_chatbot.LocalAppLanguageController
import com.example.ai_based_medical_chatbot.ui.theme.DarkTeal
import com.example.ai_based_medical_chatbot.ui.theme.DeepTeal
import com.example.ai_based_medical_chatbot.ui.theme.Ivory
import com.example.ai_based_medical_chatbot.ui.theme.Mint
import com.example.ai_based_medical_chatbot.ui.theme.PureWhite
import com.example.ai_based_medical_chatbot.ui.theme.SageGreen
import com.example.ai_based_medical_chatbot.ui.theme.SoftMint
import com.example.ai_based_medical_chatbot.ui.theme.TextDark
import com.example.ai_based_medical_chatbot.ui.theme.TextMuted

// Backwards compatibility aliases if needed
typealias HealthCategory = HealthTipCategory
typealias HealthTip = HealthTipItem

@Composable
fun HealthTipsScreen(
    onBackClick: () -> Unit
) {
    val appLanguageController = LocalAppLanguageController.current

    // Dedicated independent language selector state: defaults to current app language
    // but stays independent of global application state so other screens are unaffected.
    var screenLanguage by rememberSaveable { mutableStateOf(appLanguageController.selectedLanguage) }
    var languageMenuExpanded by remember { mutableStateOf(false) }

    var selectedCategory by rememberSaveable { mutableStateOf(HealthTipCategory.ALL) }
    var selectedTipId by rememberSaveable { mutableStateOf<Int?>(null) }

    // If a tip is selected, display its dedicated detail screen with back navigation
    if (selectedTipId != null) {
        HealthTipDetailScreen(
            tipId = selectedTipId!!,
            screenLanguage = screenLanguage,
            onBackClick = { selectedTipId = null },
            onLanguageChange = { newLang -> screenLanguage = newLang },
            onRelatedTipClick = { newTipId -> selectedTipId = newTipId }
        )
        return
    }

    val filteredTips = remember(selectedCategory) {
        HealthTipsRepository.getTipsByCategory(selectedCategory)
    }

    val headerTitle = remember(screenLanguage) { HealthTipsRepository.getHeaderTitle(screenLanguage) }
    val headerSubtitle = remember(screenLanguage) { HealthTipsRepository.getHeaderSubtitle(screenLanguage) }
    val heroTitle = remember(screenLanguage) { HealthTipsRepository.getHeroTitle(screenLanguage) }
    val heroDesc = remember(screenLanguage) { HealthTipsRepository.getHeroDescription(screenLanguage) }
    val dailyTitle = remember(screenLanguage) { HealthTipsRepository.getDailySectionTitle(screenLanguage) }
    val dailySubtitle = remember(screenLanguage) { HealthTipsRepository.getDailySectionSubtitle(screenLanguage) }
    val countLabel = remember(screenLanguage) { HealthTipsRepository.getCountLabel(screenLanguage) }
    val disclaimerText = remember(screenLanguage) { HealthTipsRepository.getDisclaimer(screenLanguage) }
    val benefitsLabel = remember(screenLanguage) { HealthTipsRepository.getBenefitsLabel(screenLanguage) }
    val labels = remember(screenLanguage) { HealthTipsRecommendations.getDetailLabels(screenLanguage) }
    val viewDetailsLabel = labels["view_detail_action"] ?: "View Details"

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
                start = 16.dp,
                top = 14.dp,
                end = 16.dp,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ==========================================
            // 1. TOP HEADER WITH INDEPENDENT LANGUAGE PILL
            // ==========================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val backInteraction = remember { MutableInteractionSource() }
                    val isBackPressed by backInteraction.collectIsPressedAsState()
                    val backScale by animateFloatAsState(
                        targetValue = if (isBackPressed) 0.94f else 1f,
                        label = "backScale"
                    )

                    IconButton(
                        onClick = onBackClick,
                        interactionSource = backInteraction,
                        modifier = Modifier
                            .size(42.dp)
                            .graphicsLayer {
                                scaleX = backScale
                                scaleY = backScale
                            }
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
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = headerTitle,
                            color = DarkTeal,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = headerSubtitle,
                            color = TextMuted,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Dedicated Independent Language Selector Dropdown Pill
                    Box {
                        val langShortCode = when (screenLanguage.lowercase().trim()) {
                            "mr", "marathi", "मराठी" -> "MR ▾"
                            "hi", "hindi", "हिंदी" -> "HI ▾"
                            else -> "EN ▾"
                        }

                        val langInteraction = remember { MutableInteractionSource() }
                        val isLangPressed by langInteraction.collectIsPressedAsState()
                        val langScale by animateFloatAsState(
                            targetValue = if (isLangPressed) 0.95f else 1f,
                            label = "langScale"
                        )

                        Row(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = langScale
                                    scaleY = langScale
                                }
                                .clip(RoundedCornerShape(18.dp))
                                .background(PureWhite)
                                .border(BorderStroke(1.dp, Mint), RoundedCornerShape(18.dp))
                                .clickable(
                                    interactionSource = langInteraction,
                                    indication = null
                                ) { languageMenuExpanded = true }
                                .padding(horizontal = 11.dp, vertical = 6.dp),
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
                                    screenLanguage = "English"
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
                                    screenLanguage = "मराठी"
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
                                    screenLanguage = "हिंदी"
                                    languageMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 2. HERO CARD
            // ==========================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DarkTeal
                    ),
                    border = BorderStroke(1.dp, Mint.copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        PureWhite.copy(alpha = 0.18f),
                                        RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = HeartPulseVectorIcon,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = heroTitle,
                                    color = PureWhite,
                                    fontSize = 17.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = heroDesc,
                                    color = PureWhite.copy(alpha = 0.90f),
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 3. CATEGORY FILTER CHIPS (Animated Transitions)
            // ==========================================
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HealthTipCategory.values().forEach { cat ->
                        val isSelected = cat == selectedCategory
                        val catTitle = HealthTipsRepository.getCategoryTitle(cat, screenLanguage)
                        val catIcon = HealthTipsRepository.getCategoryIcon(cat)

                        val chipBgColor by animateColorAsState(
                            targetValue = if (isSelected) DeepTeal else PureWhite,
                            animationSpec = tween(220),
                            label = "chipBg"
                        )
                        val chipBorderColor by animateColorAsState(
                            targetValue = if (isSelected) DeepTeal else Mint,
                            animationSpec = tween(220),
                            label = "chipBorder"
                        )
                        val chipTextColor by animateColorAsState(
                            targetValue = if (isSelected) PureWhite else DarkTeal,
                            animationSpec = tween(220),
                            label = "chipText"
                        )
                        val chipIconColor by animateColorAsState(
                            targetValue = if (isSelected) PureWhite else DeepTeal,
                            animationSpec = tween(220),
                            label = "chipIcon"
                        )

                        val chipInteraction = remember { MutableInteractionSource() }
                        val isChipPressed by chipInteraction.collectIsPressedAsState()
                        val chipScale by animateFloatAsState(
                            targetValue = if (isChipPressed) 0.94f else 1f,
                            label = "chipScale"
                        )

                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = chipScale
                                    scaleY = chipScale
                                }
                                .clip(RoundedCornerShape(12.dp))
                                .background(chipBgColor)
                                .border(
                                    BorderStroke(1.dp, chipBorderColor),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable(
                                    interactionSource = chipInteraction,
                                    indication = null
                                ) { selectedCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = catIcon,
                                    contentDescription = null,
                                    tint = chipIconColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = catTitle,
                                    color = chipTextColor,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 4. SECTION HEADER WITH COUNT BADGE
            // ==========================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = dailyTitle,
                            color = DarkTeal,
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dailySubtitle,
                            color = TextMuted,
                            fontSize = 11.5.sp
                        )
                    }

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
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${filteredTips.size} $countLabel",
                            color = DeepTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ==========================================
            // 5. CLICKABLE HEALTH TIP CARDS (Opens Details Screen)
            // ==========================================
            if (filteredTips.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, Mint)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = HealthTipsRepository.getEmptyStateMessage(screenLanguage),
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = filteredTips,
                    key = { _, tip -> tip.id }
                ) { _, tip ->
                    val content = tip.getContent(screenLanguage)
                    val catTitle = HealthTipsRepository.getCategoryTitle(tip.category, screenLanguage)
                    val catIcon = HealthTipsRepository.getCategoryIcon(tip.category)

                    HealthTipCard(
                        id = tip.id,
                        categoryTitle = catTitle,
                        title = content.title,
                        description = content.description,
                        benefits = content.benefits,
                        benefitsLabel = benefitsLabel,
                        viewDetailsLabel = viewDetailsLabel,
                        icon = catIcon,
                        onClick = { selectedTipId = tip.id }
                    )
                }
            }

            // ==========================================
            // 6. MEDICAL SAFETY DISCLAIMER CARD
            // ==========================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftMint),
                    border = BorderStroke(1.dp, Mint)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Disclaimer",
                            tint = DeepTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(9.dp))
                        Text(
                            text = disclaimerText,
                            color = TextMuted,
                            fontSize = 10.5.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // ==========================================
            // 7. FOOTER
            // ==========================================
            item {
                Spacer(modifier = Modifier.height(2.dp))
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
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun HealthTipCard(
    id: Int,
    categoryTitle: String,
    title: String,
    description: String,
    benefits: String,
    benefitsLabel: String,
    viewDetailsLabel: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val cardInteraction = remember { MutableInteractionSource() }
    val isCardPressed by cardInteraction.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isCardPressed) 0.98f else 1f,
        label = "tipCardScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = cardInteraction,
                indication = null
            ) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, Mint),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Category Badge & ID Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .background(
                            SoftMint,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = DeepTeal,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = categoryTitle,
                        color = DeepTeal,
                        fontSize = 10.5.sp,
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

            // Title & Icon Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            Ivory,
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            BorderStroke(1.dp, Mint),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = DarkTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

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
                        color = TextDark.copy(alpha = 0.85f),
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dedicated Benefits Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        SoftMint.copy(alpha = 0.65f),
                        RoundedCornerShape(10.dp)
                    )
                    .border(
                        BorderStroke(1.dp, Mint.copy(alpha = 0.7f)),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = BenefitCheckVectorIcon,
                        contentDescription = null,
                        tint = DeepTeal,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = benefitsLabel,
                            color = DeepTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = benefits,
                            color = TextDark.copy(alpha = 0.9f),
                            fontSize = 11.5.sp,
                            lineHeight = 16.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action: View Details Hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = viewDetailsLabel,
                    color = DeepTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = DeepTeal,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
