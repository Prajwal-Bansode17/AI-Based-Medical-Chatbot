package ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
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

    // Accordion expand/collapse states (default expanded for optimal discoverability)
    var isWhoUsefulExpanded by rememberSaveable { mutableStateOf(true) }
    var isSuggestionsExpanded by rememberSaveable { mutableStateOf(true) }
    var isBenefitsExpanded by rememberSaveable { mutableStateOf(true) }
    var isPrecautionsExpanded by rememberSaveable { mutableStateOf(true) }

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
                start = 16.dp,
                top = 14.dp,
                end = 16.dp,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ==========================================
            // 1. TOP APP BAR WITH BACK & LANGUAGE PILL
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

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = labels["tip_details_screen_title"] ?: "Health Tip Details",
                            color = DarkTeal,
                            fontSize = 19.sp,
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
            // 2. HERO BANNER CARD (Compact & Modern)
            // ==========================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkTeal),
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
                                            PureWhite.copy(alpha = 0.2f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = categoryIcon,
                                        contentDescription = null,
                                        tint = PureWhite,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = categoryTitle,
                                        color = PureWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = String.format("#%02d", tip.id),
                                    color = PureWhite.copy(alpha = 0.85f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(
                                            PureWhite.copy(alpha = 0.18f),
                                            RoundedCornerShape(14.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = categoryIcon,
                                        contentDescription = null,
                                        tint = PureWhite,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = content.title,
                                    color = PureWhite,
                                    fontSize = 17.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 23.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 3. SECTION A: OVERVIEW & SCIENCE (Always Open)
            // ==========================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = BorderStroke(1.dp, Mint),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = labels["overview_title"] ?: "Overview & Science",
                            color = DarkTeal,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = content.description,
                            color = TextDark.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Why This Habit Matters Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    SoftMint.copy(alpha = 0.75f),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    BorderStroke(1.dp, Mint.copy(alpha = 0.8f)),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = HeartPulseVectorIcon,
                                        contentDescription = null,
                                        tint = DeepTeal,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = labels["why_useful_label"] ?: "Why This Habit Matters",
                                        color = DeepTeal,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = details.whyUseful,
                                    color = TextDark,
                                    fontSize = 12.sp,
                                    lineHeight = 17.5.sp
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 4. SECTION B: WHO MAY FIND THIS USEFUL? (Accordion)
            // ==========================================
            item {
                val arrowRotation by animateFloatAsState(
                    targetValue = if (isWhoUsefulExpanded) 180f else 0f,
                    animationSpec = tween(250),
                    label = "whoUsefulChevron"
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = BorderStroke(1.dp, Mint),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Clickable Accordion Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isWhoUsefulExpanded = !isWhoUsefulExpanded },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(SoftMint, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = DeepTeal,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = labels["who_useful_title"] ?: "Who May Find This Useful?",
                                color = DarkTeal,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isWhoUsefulExpanded) "Collapse" else "Expand",
                                tint = DeepTeal,
                                modifier = Modifier
                                    .size(22.dp)
                                    .rotate(arrowRotation)
                            )
                        }

                        AnimatedVisibility(
                            visible = isWhoUsefulExpanded,
                            enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                            exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = labels["target_audience_label"] ?: "Target Audience & Scenarios",
                                    color = DeepTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                details.targetAudience.forEach { group ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.5.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 6.dp)
                                                .size(5.dp)
                                                .background(DeepTeal, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(9.dp))
                                        Text(
                                            text = group,
                                            color = TextDark.copy(alpha = 0.88f),
                                            fontSize = 12.5.sp,
                                            lineHeight = 18.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = labels["relevant_concerns_label"] ?: "Related Wellness Concerns",
                                    color = DeepTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    details.relevantConcerns.forEach { concern ->
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    SoftMint,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .border(
                                                    BorderStroke(1.dp, Mint),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 9.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = concern,
                                                color = DeepTeal,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 5. SECTION C: PERSONALIZED SUGGESTIONS (Accordion)
            // ==========================================
            item {
                val arrowRotation by animateFloatAsState(
                    targetValue = if (isSuggestionsExpanded) 180f else 0f,
                    animationSpec = tween(250),
                    label = "suggestionsChevron"
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = BorderStroke(1.dp, Mint),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Clickable Accordion Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isSuggestionsExpanded = !isSuggestionsExpanded },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = labels["personalized_suggestions_title"] ?: "Personalized Wellness Suggestions",
                                color = DarkTeal,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isSuggestionsExpanded) "Collapse" else "Expand",
                                tint = DeepTeal,
                                modifier = Modifier
                                    .size(22.dp)
                                    .rotate(arrowRotation)
                            )
                        }

                        AnimatedVisibility(
                            visible = isSuggestionsExpanded,
                            enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                            exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Spacer(modifier = Modifier.height(10.dp))

                                details.personalizedSuggestions.forEachIndexed { idx, suggestion ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.5.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(19.dp)
                                                .background(SoftMint, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${idx + 1}",
                                                color = DeepTeal,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(9.dp))
                                        Text(
                                            text = suggestion,
                                            color = TextDark,
                                            fontSize = 12.5.sp,
                                            lineHeight = 18.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = labels["suggestions_note"] ?: "Note: Individual wellness needs vary.",
                                    color = TextMuted,
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 6. SECTION D: KEY BENEFITS (Accordion)
            // ==========================================
            item {
                val arrowRotation by animateFloatAsState(
                    targetValue = if (isBenefitsExpanded) 180f else 0f,
                    animationSpec = tween(250),
                    label = "benefitsChevron"
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = BorderStroke(1.dp, Mint),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Clickable Accordion Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isBenefitsExpanded = !isBenefitsExpanded },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = labels["benefits_title"] ?: "Key Wellness Benefits",
                                color = DarkTeal,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isBenefitsExpanded) "Collapse" else "Expand",
                                tint = DeepTeal,
                                modifier = Modifier
                                    .size(22.dp)
                                    .rotate(arrowRotation)
                            )
                        }

                        AnimatedVisibility(
                            visible = isBenefitsExpanded,
                            enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                            exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Spacer(modifier = Modifier.height(10.dp))

                                details.detailedBenefits.forEach { benefit ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.5.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = BenefitCheckVectorIcon,
                                            contentDescription = null,
                                            tint = DeepTeal,
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(9.dp))
                                        Text(
                                            text = benefit,
                                            color = TextDark,
                                            fontSize = 12.5.sp,
                                            lineHeight = 18.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 7. SECTION E: PRECAUTIONS & SAFETY (Accordion)
            // ==========================================
            item {
                val arrowRotation by animateFloatAsState(
                    targetValue = if (isPrecautionsExpanded) 180f else 0f,
                    animationSpec = tween(250),
                    label = "precautionsChevron"
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftMint.copy(alpha = 0.65f)),
                    border = BorderStroke(1.dp, Mint)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Clickable Accordion Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isPrecautionsExpanded = !isPrecautionsExpanded },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = DeepTeal,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = labels["precautions_title"] ?: "Precautions & Safety",
                                color = DarkTeal,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isPrecautionsExpanded) "Collapse" else "Expand",
                                tint = DeepTeal,
                                modifier = Modifier
                                    .size(22.dp)
                                    .rotate(arrowRotation)
                            )
                        }

                        AnimatedVisibility(
                            visible = isPrecautionsExpanded,
                            enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                            exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
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
                                        Spacer(modifier = Modifier.width(9.dp))
                                        Text(
                                            text = item,
                                            color = TextDark,
                                            fontSize = 12.sp,
                                            lineHeight = 17.5.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // When to consult doctor
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(PureWhite, RoundedCornerShape(12.dp))
                                        .border(BorderStroke(1.dp, Mint), RoundedCornerShape(12.dp))
                                        .padding(11.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = labels["when_consult_title"] ?: "When to Consult a Physician",
                                            color = DeepTeal,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = details.whenToConsultDoctor,
                                            color = TextDark,
                                            fontSize = 11.5.sp,
                                            lineHeight = 16.5.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Emergency Warning
                                Text(
                                    text = labels["emergency_alert"] ?: "Emergency: Seek emergency hospital care for severe acute symptoms.",
                                    color = DeepTeal,
                                    fontSize = 11.sp,
                                    lineHeight = 15.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 8. SECTION F: RELATED WELLNESS TIPS (Interactive & Scaled)
            // ==========================================
            if (relatedTips.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = labels["related_tips_title"] ?: "Related Wellness Tips",
                            color = DarkTeal,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold
                        )

                        relatedTips.forEach { relTip ->
                            val relContent = relTip.getContent(screenLanguage)
                            val relCatTitle = HealthTipsRepository.getCategoryTitle(relTip.category, screenLanguage)
                            val relCatIcon = HealthTipsRepository.getCategoryIcon(relTip.category)

                            val relInteraction = remember { MutableInteractionSource() }
                            val isRelPressed by relInteraction.collectIsPressedAsState()
                            val relScale by animateFloatAsState(
                                targetValue = if (isRelPressed) 0.98f else 1f,
                                label = "relScale"
                            )

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = relScale
                                        scaleY = relScale
                                    }
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(
                                        interactionSource = relInteraction,
                                        indication = null
                                    ) { onRelatedTipClick(relTip.id) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = PureWhite),
                                border = BorderStroke(1.dp, Mint),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(SoftMint, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = relCatIcon,
                                            contentDescription = null,
                                            tint = DeepTeal,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = relCatTitle,
                                            color = DeepTeal,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = relContent.title,
                                            color = DarkTeal,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "View",
                                        tint = DeepTeal,
                                        modifier = Modifier.size(18.dp)
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
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftMint),
                    border = BorderStroke(1.dp, Mint)
                ) {
                    Text(
                        text = labels["disclaimer_banner"] ?: "Clinical Notice: General informational guidance.",
                        color = TextMuted,
                        fontSize = 10.5.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(11.dp)
                    )
                }

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
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "MEDASSIST AI • CLINICAL WELLNESS GUIDE",
                        color = DarkTeal,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }
    }
}
