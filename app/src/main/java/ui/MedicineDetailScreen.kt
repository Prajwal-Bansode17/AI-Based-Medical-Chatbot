package ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
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

fun localizeMedicineTag(tag: String, language: String): String {
    return when (tag.lowercase().trim()) {
        "pain relief" -> appText("tag_pain_relief", language)
        "fever" -> appText("tag_fever", language)
        "antibiotic" -> appText("tag_antibiotic", language)
        "allergy relief" -> appText("tag_allergy_relief", language)
        "acidity relief" -> appText("tag_acidity_relief", language)
        "diabetes" -> appText("tag_diabetes", language)
        "general care" -> appText("tag_general_care", language)
        else -> tag
    }
}

val HeartBorderVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "HeartBorder",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = addPathNodes(
            "M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
        ),
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(DeepTeal),
        strokeLineWidth = 1.8f
    ).build()
}

val HeartFilledVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "HeartFilled",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = addPathNodes(
            "M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
        ),
        fill = SolidColor(Color(0xFFE53935))
    ).build()
}



val InteractionsVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "InteractionsIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = addPathNodes(
            "M9.01 14H2v2h7.01v3L13 15.5 9.01 12v2zm5.98-1v-3H22V8h-7.01V5L11 8.5l3.99 3.5z"
        ),
        fill = SolidColor(DeepTeal)
    ).build()
}

@Composable
fun MedicineDetailScreen(
    medicine: Medicine,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appLanguageController = LocalAppLanguageController.current
    val currentLang = appLanguageController.selectedLanguage
    val t = { key: String -> appText(key, currentLang) }

    var isFavorite by remember { mutableStateOf(medicine.isFavorite) }
    val heartScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.2f else 1.0f,
        animationSpec = spring(dampingRatio = 0.4f),
        label = "heartScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Ivory)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 24.dp
                    )
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // =========================================================
            // A. TOP APP BAR
            // =========================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(42.dp)
                        .background(PureWhite, RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, Mint), RoundedCornerShape(12.dp)),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = PureWhite,
                        contentColor = DarkTeal
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = t("back"),
                        tint = DarkTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = t("medicine_detail_title"),
                        color = DarkTeal,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = t("medicine_detail_subtitle"),
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // =========================================================
            // B. PRODUCT HEADER CARD
            // =========================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, Mint.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProductPackagingIllustration(
                        medicineName = medicine.name,
                        form = medicine.strengthAndForm,
                        modifier = Modifier.size(width = 68.dp, height = 54.dp)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = medicine.name,
                            color = DarkTeal,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        )

                        val subtitle = if (medicine.strengthAndForm.isNotBlank()) {
                            medicine.strengthAndForm
                        } else if (medicine.genericName.isNotBlank()) {
                            medicine.genericName
                        } else {
                            medicine.category
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            color = TextMuted,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // Tags & Rx/OTC Badges
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rx or OTC Badge (only when supported by data)
                            if (medicine.isPrescription != null) {
                                val isRx = medicine.isPrescription == true
                                val badgeBg = if (isRx) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)
                                val badgeBorder = if (isRx) Color(0xFFFECACA) else Color(0xFFBBF7D0)
                                val badgeText = if (isRx) Color(0xFF991B1B) else Color(0xFF166534)
                                val badgeLabel = if (isRx) t("medicine_rx_badge") else t("medicine_otc_badge")

                                Box(
                                    modifier = Modifier
                                        .background(badgeBg, RoundedCornerShape(6.dp))
                                        .border(BorderStroke(1.dp, badgeBorder), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                                ) {
                                    Text(
                                        text = badgeLabel,
                                        color = badgeText,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            val tagsToShow = medicine.tags.take(2)
                            for (tag in tagsToShow) {
                                Box(
                                    modifier = Modifier
                                        .background(SoftMint.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                        .border(BorderStroke(1.dp, Mint.copy(alpha = 0.5f)), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                                ) {
                                    Text(
                                        text = localizeMedicineTag(tag, currentLang),
                                        color = DeepTeal,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Favorite Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = if (isFavorite) Color(0xFFFFEBEE) else SoftMint.copy(alpha = 0.45f),
                                shape = CircleShape
                            )
                            .border(
                                border = BorderStroke(
                                    1.dp,
                                    if (isFavorite) Color(0xFFFFCDD2) else Mint.copy(alpha = 0.5f)
                                ),
                                shape = CircleShape
                            )
                            .clickable { isFavorite = !isFavorite },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFavorite) HeartFilledVectorIcon else HeartBorderVectorIcon,
                            contentDescription = t("chat_save_favorite_desc"),
                            tint = if (isFavorite) Color(0xFFE53935) else DeepTeal,
                            modifier = Modifier
                                .size(20.dp)
                                .scale(heartScale)
                        )
                    }
                }
            }

            // =========================================================
            // C. MEDICINE SPECIFICATIONS / OVERVIEW CARD
            // Displays Generic, Brand, Strength, Dosage Form
            // =========================================================
            val hasSpecifications = medicine.genericName.isNotBlank() ||
                medicine.brandName.isNotBlank() ||
                medicine.strength.isNotBlank() ||
                medicine.dosageForm.isNotBlank() ||
                medicine.category.isNotBlank()

            if (hasSpecifications) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = BorderStroke(1.dp, Mint.copy(alpha = 0.6f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(SoftMint, CircleShape)
                                    .border(BorderStroke(1.dp, Mint.copy(alpha = 0.6f)), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = PillVectorIcon,
                                    contentDescription = null,
                                    tint = DeepTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = t("medicine_detail_name"),
                                color = DarkTeal,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Specification Key-Value Pairs
                        if (medicine.genericName.isNotBlank()) {
                            SpecificationRow(
                                label = t("medicine_detail_generic"),
                                value = medicine.genericName
                            )
                        }

                        if (medicine.brandName.isNotBlank() && !medicine.brandName.equals(medicine.name, ignoreCase = true)) {
                            SpecificationRow(
                                label = t("medicine_detail_brand"),
                                value = medicine.brandName
                            )
                        }

                        if (medicine.strength.isNotBlank()) {
                            SpecificationRow(
                                label = t("medicine_detail_strength"),
                                value = medicine.strength
                            )
                        }

                        if (medicine.dosageForm.isNotBlank()) {
                            SpecificationRow(
                                label = t("medicine_detail_dosage_form"),
                                value = medicine.dosageForm
                            )
                        }

                        if (medicine.category.isNotBlank()) {
                            SpecificationRow(
                                label = t("medicine_sort"),
                                value = medicine.category
                            )
                        }
                    }
                }
            }

            // =========================================================
            // D. SECTION 1: USES
            // =========================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, Mint.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    SectionHeader(
                        icon = PillVectorIcon,
                        iconTint = DeepTeal,
                        iconBg = SoftMint,
                        iconBorder = Mint.copy(alpha = 0.6f),
                        title = t("medicine_what_is_it_used_for")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (medicine.uses.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            for (item in medicine.uses) {
                                BulletRow(text = item, bulletColor = DeepTeal)
                            }
                        }
                    } else {
                        UnavailableInformationNotice(t("medicine_detail_not_available"))
                    }
                }
            }

            // =========================================================
            // E. SECTION 2: SIDE EFFECTS
            // =========================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, Mint.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    SectionHeader(
                        icon = Icons.Default.Info,
                        iconTint = Color(0xFFD97706),
                        iconBg = Color(0xFFFEF3C7),
                        iconBorder = Color(0xFFFDE68A),
                        title = t("medicine_detail_side_effects")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (medicine.commonSideEffects.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            for (item in medicine.commonSideEffects) {
                                BulletRow(text = item, bulletColor = Color(0xFFD97706))
                            }
                        }
                    } else {
                        UnavailableInformationNotice(t("medicine_detail_not_available"))
                    }
                }
            }

            // =========================================================
            // F. SECTION 3: PRECAUTIONS
            // =========================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, Mint.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    SectionHeader(
                        icon = Icons.Default.Warning,
                        iconTint = Color(0xFFDC2626),
                        iconBg = Color(0xFFFEE2E2),
                        iconBorder = Color(0xFFFECACA),
                        title = t("medicine_detail_precautions")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (medicine.precautions.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            for (item in medicine.precautions) {
                                BulletRow(text = item, bulletColor = Color(0xFFDC2626))
                            }
                        }
                    } else {
                        UnavailableInformationNotice(t("medicine_detail_not_available"))
                    }
                }
            }

            // =========================================================
            // G. SECTION 4: INTERACTIONS
            // =========================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, Mint.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    SectionHeader(
                        icon = InteractionsVectorIcon,
                        iconTint = DeepTeal,
                        iconBg = SoftMint,
                        iconBorder = Mint.copy(alpha = 0.6f),
                        title = t("medicine_detail_interactions")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (medicine.interactions.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            for (item in medicine.interactions) {
                                BulletRow(text = item, bulletColor = DeepTeal)
                            }
                        }
                    } else {
                        UnavailableInformationNotice(t("medicine_detail_not_available"))
                    }
                }
            }

            // =========================================================
            // H. WARNING CARD (Only displayed if warnings exist in dataset)
            // =========================================================
            if (medicine.warning.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    border = BorderStroke(1.dp, Color(0xFFFCD34D))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color(0xFFFDE68A), CircleShape)
                                .border(BorderStroke(1.dp, Color(0xFFF59E0B)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = t("medicine_detail_warnings"),
                                color = Color(0xFF92400E),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = medicine.warning,
                                color = Color(0xFF78350F),
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            // =========================================================
            // I. PROMINENT EDUCATIONAL SAFETY NOTICE
            // =========================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                border = BorderStroke(1.dp, Color(0xFF86EFAC))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFDCFCE7), CircleShape)
                            .border(BorderStroke(1.dp, Color(0xFF4ADE80)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ShieldCheckVectorIcon,
                            contentDescription = t("medicine_safety_notice_title"),
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(11.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = t("medicine_safety_notice_title"),
                            color = Color(0xFF166534),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = t("medicine_safety_notice_body"),
                            color = Color(0xFF14532D),
                            fontSize = 11.5.sp,
                            lineHeight = 16.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    iconBorder: Color,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(iconBg, CircleShape)
                .border(BorderStroke(1.dp, iconBorder), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = title,
            color = DarkTeal,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BulletRow(
    text: String,
    bulletColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(5.dp)
                .background(bulletColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = TextDark.copy(alpha = 0.85f),
            fontSize = 12.5.sp,
            lineHeight = 17.5.sp
        )
    }
}

@Composable
private fun SpecificationRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(110.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            color = TextDark,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun UnavailableInformationNotice(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9FAFB), RoundedCornerShape(10.dp))
            .border(BorderStroke(1.dp, Color(0xFFE5E7EB)), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Text(
            text = message,
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
