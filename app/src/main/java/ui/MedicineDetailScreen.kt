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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import com.example.ai_based_medical_chatbot.LocalAppLanguageController
import com.example.ai_based_medical_chatbot.appText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.foundation.border
import com.example.ai_based_medical_chatbot.ui.theme.DeepTeal
import com.example.ai_based_medical_chatbot.ui.theme.DarkTeal
import com.example.ai_based_medical_chatbot.ui.theme.SageGreen
import com.example.ai_based_medical_chatbot.ui.theme.Ivory
import com.example.ai_based_medical_chatbot.ui.theme.Mint
import com.example.ai_based_medical_chatbot.ui.theme.SoftMint
import com.example.ai_based_medical_chatbot.ui.theme.CoralAccent
import com.example.ai_based_medical_chatbot.ui.theme.AppBackground
import com.example.ai_based_medical_chatbot.ui.theme.AppSurface
import com.example.ai_based_medical_chatbot.ui.theme.LightGreenBorder
import com.example.ai_based_medical_chatbot.ui.theme.LightGreenSoft
import com.example.ai_based_medical_chatbot.ui.theme.LightGreenSurface
import com.example.ai_based_medical_chatbot.ui.theme.PrimaryTeal
import com.example.ai_based_medical_chatbot.ui.theme.PrimaryTealDark
import com.example.ai_based_medical_chatbot.ui.theme.PureWhite
import com.example.ai_based_medical_chatbot.ui.theme.TextDark
import com.example.ai_based_medical_chatbot.ui.theme.TextMuted

@Composable
fun MedicineDetailScreen(
    medicine: Medicine,
    onBackClick: () -> Unit
) {

    val primary = DeepTeal
    val darkBlue = DarkTeal
    val gray = TextMuted

    val appLanguageController = LocalAppLanguageController.current
    val selectedLanguage = appLanguageController.selectedLanguage

    fun t(key: String): String = appText(key, selectedLanguage)

    val backgroundTop = Ivory
    val backgroundBottom = SoftMint

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
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    PaddingValues(
                        start = 20.dp,
                        top = 18.dp,
                        end = 20.dp,
                        bottom = 28.dp
                    )
                ),

            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {

            // HEADER

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
                        tint = DarkTeal
                    )
                }

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = t("medicine_detail_title"),
                        color = DarkTeal,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = t("medicine_detail_subtitle"),
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            // MEDICINE NAME

            Card(
                modifier = Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(20.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor = DarkTeal
                    ),

                border = BorderStroke(1.dp, Mint.copy(alpha = 0.4f)),

                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    )
            ) {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                ) {

                    Text(
                        text = t("medicine_detail_name"),
                        color =
                            PureWhite.copy(
                                alpha = 0.8f
                            ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text = medicine.name,
                        color = PureWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // GENERIC NAME

            MedicineTextSection(
                title = t("medicine_detail_generic"),
                content =
                    medicine.genericName.ifBlank {
                        t("medicine_detail_generic_unavailable")
                    },
                primary = primary,
                darkBlue = darkBlue,
                gray = gray
            )

            // USES

            MedicineListSection(
                title = t("medicine_detail_uses"),
                items = medicine.uses,
                primary = primary,
                darkBlue = darkBlue,
                gray = gray
            )

            // SIDE EFFECTS

            MedicineListSection(
                title = t("medicine_detail_side_effects"),
                items = medicine.commonSideEffects,
                primary = primary,
                darkBlue = darkBlue,
                gray = gray
            )

            // WARNINGS

            MedicineTextSection(
                title = t("medicine_detail_warnings"),
                content =
                    medicine.warning.ifBlank {
                        t("medicine_detail_warning_unavailable")
                    },
                primary = primary,
                darkBlue = darkBlue,
                gray = gray
            )

            // PRECAUTIONS

            MedicineListSection(
                title = t("medicine_detail_precautions"),
                items = medicine.precautions,
                primary = primary,
                darkBlue = darkBlue,
                gray = gray
            )

            // DISCLAIMER

            Card(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(16.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            SoftMint
                    ),

                border = BorderStroke(1.dp, Mint)
            ) {

                Text(
                    text =
                        t("medicine_detail_disclaimer"),

                    modifier =
                        Modifier.padding(14.dp),

                    color = DarkTeal,
                    fontSize = 11.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}


// ================================================================
// TEXT INFORMATION SECTION
// ================================================================

@Composable
private fun MedicineTextSection(
    title: String,
    content: String,
    primary: Color,
    darkBlue: Color,
    gray: Color
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor = PureWhite
            ),

        border = BorderStroke(1.dp, Mint),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(18.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(10.dp)
                            .background(
                                color = DeepTeal,
                                shape = CircleShape
                            )
                )

                Spacer(
                    modifier =
                        Modifier.width(9.dp)
                )

                Text(
                    text = title,
                    color = DarkTeal,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(11.dp)
            )

            Text(
                text = content,
                color = TextDark,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}


// ================================================================
// LIST INFORMATION SECTION
// ================================================================

@Composable
private fun MedicineListSection(
    title: String,
    items: List<String>,
    primary: Color,
    darkBlue: Color,
    gray: Color
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor = PureWhite
            ),

        border = BorderStroke(1.dp, Mint),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(18.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(10.dp)
                            .background(
                                color = DeepTeal,
                                shape = CircleShape
                            )
                )

                Spacer(
                    modifier =
                        Modifier.width(9.dp)
                )

                Text(
                    text = title,
                    color = DarkTeal,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(11.dp)
            )

            items.forEach { item ->

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                bottom = 9.dp
                            ),

                    verticalAlignment =
                        Alignment.Top
                ) {

                    Text(
                        text = "•",
                        color = DeepTeal,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.width(9.dp)
                    )

                    Text(
                        text = item,
                        color = TextDark,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier =
                            Modifier.weight(1f)
                    )
                }
            }
        }
    }
}