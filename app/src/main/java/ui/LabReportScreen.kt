package ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_based_medical_chatbot.LocalAppLanguageController
import com.example.ai_based_medical_chatbot.data.api.BiomarkerDto
import com.example.ai_based_medical_chatbot.data.api.LabReportRequest
import com.example.ai_based_medical_chatbot.data.api.LabReportResponse
import com.example.ai_based_medical_chatbot.data.api.RetrofitClient
import com.example.ai_based_medical_chatbot.ui.theme.CoralAccent
import com.example.ai_based_medical_chatbot.ui.theme.CoralAccentBorder
import com.example.ai_based_medical_chatbot.ui.theme.CoralAccentSoft
import com.example.ai_based_medical_chatbot.ui.theme.DarkTeal
import com.example.ai_based_medical_chatbot.ui.theme.DeepTeal
import com.example.ai_based_medical_chatbot.ui.theme.Ivory
import com.example.ai_based_medical_chatbot.ui.theme.Mint
import com.example.ai_based_medical_chatbot.ui.theme.PureWhite
import com.example.ai_based_medical_chatbot.ui.theme.SoftMint
import com.example.ai_based_medical_chatbot.ui.theme.StaticDarkTeal
import com.example.ai_based_medical_chatbot.ui.theme.StaticDeepTeal
import com.example.ai_based_medical_chatbot.ui.theme.StatusError
import com.example.ai_based_medical_chatbot.ui.theme.StatusSuccess
import com.example.ai_based_medical_chatbot.ui.theme.StatusWarning
import com.example.ai_based_medical_chatbot.ui.theme.TextDark
import com.example.ai_based_medical_chatbot.ui.theme.TextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ============================================================
// VECTOR ICONS (Consistent MedAssist Theme)
// ============================================================

val LabFlaskVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "LabFlaskVectorIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(StaticDeepTeal),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(9f, 3f)
            lineTo(15f, 3f)
            moveTo(10f, 3f)
            lineTo(10f, 8f)
            lineTo(4.5f, 18.5f)
            curveTo(3.8f, 19.8f, 4.7f, 21f, 6.2f, 21f)
            lineTo(17.8f, 21f)
            curveTo(19.3f, 21f, 20.2f, 19.8f, 19.5f, 18.5f)
            lineTo(14f, 8f)
            lineTo(14f, 3f)
            moveTo(7.5f, 15f)
            lineTo(16.5f, 15f)
        }
    }.build()
}

val DoctorBadgeVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "DoctorBadgeVectorIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(StaticDarkTeal),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 2f)
            lineTo(20f, 6f)
            lineTo(20f, 12f)
            curveTo(20f, 17f, 16f, 21f, 12f, 22f)
            curveTo(8f, 21f, 4f, 17f, 4f, 12f)
            lineTo(4f, 6f)
            close()
            moveTo(12f, 8f)
            lineTo(12f, 16f)
            moveTo(8f, 12f)
            lineTo(16f, 12f)
        }
    }.build()
}

// Sample lab reports for 1-tap testing
private const val SAMPLE_CBC = """COMPLETE BLOOD COUNT (CBC)
Hemoglobin: 9.8 g/dL
Total Leukocyte Count (WBC): 12,500 /mcL
Platelet Count: 2.2 Lakhs
RBC Count: 4.5 mil/mcL"""

private const val SAMPLE_DIABETES = """BIOCHEMISTRY REPORT
Fasting Blood Sugar (FBS): 138 mg/dL
Post Prandial Blood Sugar: 210 mg/dL
HbA1c: 7.2 %
Total Cholesterol: 245 mg/dL
Triglycerides: 180 mg/dL
LDL Cholesterol: 145 mg/dL
HDL: 38 mg/dL"""

private const val SAMPLE_KIDNEY_LIVER = """KFT & LFT PANEL
Serum Creatinine: 1.5 mg/dL
Blood Urea: 45 mg/dL
Serum Uric Acid: 8.1 mg/dL
Total Bilirubin: 2.4 mg/dL
SGPT (ALT): 72 U/L
SGOT (AST): 58 U/L
TSH: 6.8 mIU/L"""

/**
 * Smart Medical Lab Report Analyzer Screen
 * Decodes clinical laboratory values into layman explanations, visual speedometer gauges,
 * doctor questions, dietary advice, and provides 1-tap deep links to MedAssist AI Chatbot.
 */
@Composable
fun LabReportScreen(
    onBackClick: () -> Unit = {},
    onDiscussInChat: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val appLanguageController = LocalAppLanguageController.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Normalize initial language code (en, mr, hi)
    val defaultLang = when (appLanguageController.selectedLanguage.lowercase()) {
        "mr", "marathi" -> "mr"
        "hi", "hindi" -> "hi"
        else -> "en"
    }

    var selectedLanguage by remember { mutableStateOf(defaultLang) }
    var inputText by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var analysisResult by remember { mutableStateOf<LabReportResponse?>(null) }

    // Map to track expanded state of each biomarker accordion
    val expandedCards = remember { mutableStateMapOf<String, Boolean>() }

    fun executeAnalysis() {
        val trimmed = inputText.trim()
        if (trimmed.isEmpty()) {
            Toast.makeText(
                context,
                when (selectedLanguage) {
                    "mr" -> "कृपया लॅब रिपोर्टचा मजकूर टाका किंवा खालील नमुना निवडा."
                    "hi" -> "कृपया लैब रिपोर्ट का टेक्स्ट पेस्ट करें या नीचे दिया गया सैंपल चुनें।"
                    else -> "Please paste lab report text or choose a sample report below."
                },
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        keyboardController?.hide()
        isAnalyzing = true
        errorMessage = null

        coroutineScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.analyzeLabReport(
                        LabReportRequest(
                            text = trimmed,
                            language = selectedLanguage
                        )
                    )
                }

                if (response.isSuccessful && response.body() != null) {
                    analysisResult = response.body()
                    // Auto-expand flagged biomarkers
                    analysisResult?.biomarkers?.forEach { b ->
                        if (b.status != "normal") {
                            expandedCards[b.key] = true
                        }
                    }
                } else {
                    val errCode = response.code()
                    errorMessage = when (selectedLanguage) {
                        "mr" -> "सर्व्हरकडून त्रुटी मिळाली ($errCode). कृपया पुन्हा प्रयत्न करा."
                        "hi" -> "सर्वर से त्रुटि प्राप्त हुई ($errCode)। कृपया पुनः प्रयास करें।"
                        else -> "Server returned error ($errCode). Please try again."
                    }
                }
            } catch (e: Exception) {
                errorMessage = when (selectedLanguage) {
                    "mr" -> "सर्व्हरशी संपर्क होऊ शकला नाही. कृपया इंटरनेट तपासा."
                    "hi" -> "सर्वर से कनेक्ट नहीं हो सका। कृपया इंटरनेट जांचें।"
                    else -> "Unable to connect to MedAssist AI server. Please verify your connection."
                }
            } finally {
                isAnalyzing = false
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Ivory, SoftMint.copy(alpha = 0.55f))
                )
            ),
        containerColor = Color.Transparent,
        topBar = {
            Surface(
                color = PureWhite.copy(alpha = 0.95f),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DarkTeal
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(SoftMint, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = LabFlaskVectorIcon,
                            contentDescription = null,
                            tint = DeepTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (selectedLanguage) {
                                "mr" -> "स्मार्ट लॅब रिपोर्ट विश्लेषक"
                                "hi" -> "स्मार्ट लैब रिपोर्ट विश्लेषक"
                                else -> "Smart Lab Report Analyzer"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when (selectedLanguage) {
                                "mr" -> "बायोमार्कर डिकोडर व संदर्भ श्रेणी"
                                "hi" -> "बायोमार्कर डिकोडर व संदर्भ सीमा"
                                else -> "Biomarker Decoder & Clinical Ranges"
                            },
                            fontSize = 12.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ============================================================
            // 1. COMPACT INPUT CARD
            // ============================================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = BorderStroke(1.dp, Mint)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Section Header & Language Pill Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (selectedLanguage) {
                                    "mr" -> "रिपोर्ट मजकूर टाका:"
                                    "hi" -> "रिपोर्ट टेक्स्ट दर्ज करें:"
                                    else -> "Paste Lab Report Text:"
                                },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = TextDark
                            )

                            // Compact 3-segment language selector
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(SoftMint)
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                val languages = listOf("en" to "EN", "mr" to "MR", "hi" to "HI")
                                languages.forEach { (code, label) ->
                                    val isSelected = selectedLanguage == code
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) DeepTeal else Color.Transparent)
                                            .clickable {
                                                selectedLanguage = code
                                                // If already analyzed, re-trigger with new language
                                                if (inputText.isNotBlank() && analysisResult != null) {
                                                    executeAnalysis()
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp,
                                            color = if (isSelected) PureWhite else TextMuted
                                        )
                                    }
                                }
                            }
                        }

                        // Multi-line Outlined Text Field (Capped Height ~120dp)
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(10.dp),
                            placeholder = {
                                Text(
                                    text = when (selectedLanguage) {
                                        "mr" -> "उदा. Hemoglobin: 10.2 g/dL, Fasting Sugar: 145 mg/dL, Creatinine: 1.6 mg/dL..."
                                        "hi" -> "उदा. Hemoglobin: 10.2 g/dL, Fasting Sugar: 145 mg/dL, Creatinine: 1.6 mg/dL..."
                                        else -> "e.g. Hemoglobin: 10.2 g/dL, Fasting Sugar: 145 mg/dL, Creatinine: 1.6 mg/dL..."
                                    },
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            },
                            trailingIcon = {
                                if (inputText.isNotEmpty()) {
                                    IconButton(
                                        onClick = { inputText = "" },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { executeAnalysis() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DeepTeal,
                                unfocusedBorderColor = Mint,
                                focusedContainerColor = PureWhite,
                                unfocusedContainerColor = PureWhite,
                                focusedTextColor = TextDark,
                                unfocusedTextColor = TextDark
                            )
                        )

                        // Sample Report Presets (Horizontal Scrollable Chips)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (selectedLanguage) {
                                    "mr" -> "नमुने:"
                                    "hi" -> "नमूने:"
                                    else -> "Samples:"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMuted
                            )

                            CompactSampleChip(
                                label = "🩸 CBC",
                                onClick = {
                                    inputText = SAMPLE_CBC
                                    executeAnalysis()
                                }
                            )

                            CompactSampleChip(
                                label = "🍬 Diabetes",
                                onClick = {
                                    inputText = SAMPLE_DIABETES
                                    executeAnalysis()
                                }
                            )

                            CompactSampleChip(
                                label = "🧪 KFT / LFT",
                                onClick = {
                                    inputText = SAMPLE_KIDNEY_LIVER
                                    executeAnalysis()
                                }
                            )
                        }

                        // Analyze Action Button
                        Button(
                            onClick = { executeAnalysis() },
                            enabled = !isAnalyzing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepTeal,
                                disabledContainerColor = DeepTeal.copy(alpha = 0.5f)
                            )
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = PureWhite
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (selectedLanguage) {
                                        "mr" -> "विश्लेषण सुरू आहे..."
                                        "hi" -> "विश्लेषण हो रहा है..."
                                        else -> "Decoding Report..."
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (selectedLanguage) {
                                        "mr" -> "रिपोर्टचे विश्लेषण करा"
                                        "hi" -> "रिपोर्ट का विश्लेषण करें"
                                        else -> "Analyze Lab Report"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite
                                )
                            }
                        }
                    }
                }
            }

            // Error Banner (if any)
            if (errorMessage != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CoralAccentSoft),
                        border = BorderStroke(1.dp, CoralAccentBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = StatusError,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                fontSize = 12.sp,
                                color = TextDark
                            )
                        }
                    }
                }
            }

            // ============================================================
            // 2. ANALYSIS RESULTS DASHBOARD
            // ============================================================
            val result = analysisResult
            if (result != null) {
                // Summary Card with Badge Pill
                item {
                    val hasFlagged = result.flaggedCount > 0
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (hasFlagged) CoralAccentSoft else SoftMint
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (hasFlagged) CoralAccentBorder else Mint
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Analyzed Badge
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = PureWhite.copy(alpha = 0.9f)
                                    ) {
                                        Text(
                                            text = "📊 ${result.analyzedCount} Analyzed",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    // Flagged Badge
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (hasFlagged) CoralAccent else StatusSuccess.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (hasFlagged) "⚠️ ${result.flaggedCount} Flagged" else "✅ All Normal",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hasFlagged) PureWhite else StatusSuccess,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = result.language.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted
                                )
                            }

                            Text(
                                text = result.summary,
                                fontSize = 13.sp,
                                color = TextDark,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Header for Biomarkers
                if (result.biomarkers.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (selectedLanguage) {
                                    "mr" -> "तपासलेले बायोमार्कर्स (${result.biomarkers.size})"
                                    "hi" -> "जांचे गए बायोमार्कर्स (${result.biomarkers.size})"
                                    else -> "Biomarkers Evaluated (${result.biomarkers.size})"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkTeal
                            )
                            Text(
                                text = when (selectedLanguage) {
                                    "mr" -> "तपशिलासाठी कार्डवर टॅप करा"
                                    "hi" -> "विवरण के लिए कार्ड पर टैप करें"
                                    else -> "Tap card to expand details"
                                },
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    // Biomarker Card List
                    items(result.biomarkers, key = { it.key }) { biomarker ->
                        val isExpanded = expandedCards[biomarker.key] ?: false
                        BiomarkerCard(
                            biomarker = biomarker,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedCards[biomarker.key] = !isExpanded
                            }
                        )
                    }
                }

                // ============================================================
                // 3. DOCTOR QUESTIONS CARD
                // ============================================================
                if (result.doctorQuestions.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            border = BorderStroke(1.dp, Mint)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(SoftMint, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = DoctorBadgeVectorIcon,
                                            contentDescription = null,
                                            tint = DarkTeal,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = when (selectedLanguage) {
                                                "mr" -> "डॉक्टरांना विचारण्यासाठीचे प्रश्न"
                                                "hi" -> "डॉक्टर से पूछने योग्य प्रश्न"
                                                else -> "Questions for Your Doctor"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = DarkTeal
                                        )
                                        Text(
                                            text = when (selectedLanguage) {
                                                "mr" -> "पुढील तपासणी वेळी हे प्रश्न सोबत ठेवा"
                                                "hi" -> "अगले परामर्श में ये सवाल पूछें"
                                                else -> "Take these to your next clinical consultation"
                                            },
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                }

                                result.doctorQuestions.forEachIndexed { index, question ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "${index + 1}.",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepTeal
                                        )
                                        Text(
                                            text = question,
                                            fontSize = 12.sp,
                                            color = TextDark,
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ============================================================
                // 4. DEEP-LINK ACTION CARD: DISCUSS WITH MEDASSIST AI
                // ============================================================
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SoftMint.copy(alpha = 0.85f)
                        ),
                        border = BorderStroke(1.dp, DeepTeal.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "💬", fontSize = 18.sp)
                                    Column {
                                        Text(
                                            text = when (selectedLanguage) {
                                                "mr" -> "MedAssist AI शी सविस्तर चर्चा करा"
                                                "hi" -> "MedAssist AI से विस्तृत चर्चा करें"
                                                else -> "Discuss Report with MedAssist AI"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = DarkTeal
                                        )
                                        Text(
                                            text = when (selectedLanguage) {
                                                "mr" -> "या रिपोर्टवर आधारित प्रश्न विचारा"
                                                "hi" -> "इस रिपोर्ट पर आधारित प्रश्न पूछें"
                                                else -> "Ask questions directly about these results"
                                            },
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }

                            // Dynamic Prompt Suggestion Chips based on flagged biomarkers
                            val flaggedMarkers = result.biomarkers.filter { it.status != "normal" }
                            val promptChips = mutableListOf<String>()

                            if (flaggedMarkers.isNotEmpty()) {
                                flaggedMarkers.take(3).forEach { b ->
                                    when (selectedLanguage) {
                                        "mr" -> promptChips.add("${b.displayName} नियंत्रित करण्यासाठी काय करावे?")
                                        "hi" -> promptChips.add("${b.displayName} को नियंत्रित कैसे करें?")
                                        else -> promptChips.add("How to manage ${b.displayName} (${b.value} ${b.unit})?")
                                    }
                                }
                            } else {
                                when (selectedLanguage) {
                                    "mr" -> {
                                        promptChips.add("माझे सर्व रिपोर्ट सामान्य ठेवण्यासाठी आहार?")
                                        promptChips.add("पुढील नियमित आरोग्य तपासणी कधी करावी?")
                                    }
                                    "hi" -> {
                                        promptChips.add("सभी रिपोर्ट सामान्य बनाए रखने के लिए आहार?")
                                        promptChips.add("अगली नियमित स्वास्थ्य जांच कब कराएं?")
                                    }
                                    else -> {
                                        promptChips.add("Diet to maintain normal parameters?")
                                        promptChips.add("When to do next preventive checkup?")
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                promptChips.forEach { chipPrompt ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = PureWhite,
                                        border = BorderStroke(1.dp, Mint),
                                        modifier = Modifier.clickable {
                                            val fullPrompt = buildContextualChatPrompt(result, chipPrompt)
                                            onDiscussInChat(fullPrompt)
                                        }
                                    ) {
                                        Text(
                                            text = chipPrompt,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = DeepTeal,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }

                            // Full Chat Bridge Button
                            Button(
                                onClick = {
                                    val defaultQuestion = when (selectedLanguage) {
                                        "mr" -> "कृपया माझ्या या लॅब रिपोर्टचे सविस्तर विश्लेषण सांगा आणि आहाराबद्दल मार्गदर्शन करा."
                                        "hi" -> "कृपया मेरे इस लैब रिपोर्ट का विस्तृत विश्लेषण बताएं और आहार संबंधी मार्गदर्शन दें।"
                                        else -> "Please provide a comprehensive review of my lab report results and recommend dietary next steps."
                                    }
                                    val fullPrompt = buildContextualChatPrompt(result, defaultQuestion)
                                    onDiscussInChat(fullPrompt)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkTeal)
                            ) {
                                Text(
                                    text = when (selectedLanguage) {
                                        "mr" -> "सर्व रिपोर्टसह चॅट सुरू करा ➜"
                                        "hi" -> "सभी रिपोर्ट के साथ चैट शुरू करें ➜"
                                        else -> "Open Chat with Full Report Context ➜"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite
                                )
                            }
                        }
                    }
                }

                // ============================================================
                // 5. STATUTORY CLINICAL DISCLAIMER
                // ============================================================
                item {
                    Text(
                        text = result.disclaimer,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ============================================================
// HELPER COMPOSABLES
// ============================================================

@Composable
private fun CompactSampleChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SoftMint,
        border = BorderStroke(1.dp, Mint),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = DeepTeal,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun BiomarkerCard(
    biomarker: BiomarkerDto,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val isOutRange = biomarker.status != "normal"
    val isCritical = biomarker.status.contains("critical")

    val statusColor = when {
        isCritical -> StatusError
        isOutRange -> StatusWarning
        else -> StatusSuccess
    }

    val animatedGauge by animateFloatAsState(
        targetValue = biomarker.gaugePercent.coerceIn(0f, 1f),
        animationSpec = tween(650),
        label = "BiomarkerGaugeAnimation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(
            1.dp,
            if (isCritical) CoralAccentBorder else if (isOutRange) CoralAccentSoft else Mint
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header: Name, Value + Unit, Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = biomarker.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Ref: ${biomarker.referenceRange} ${biomarker.unit}".trim(),
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Observed Value
                    Text(
                        text = "${biomarker.value} ${biomarker.unit}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = if (isOutRange) statusColor else TextDark
                    )

                    // Status Pill
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = statusColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = when (biomarker.status) {
                                "critical_high" -> "🔴 Crit High"
                                "critical_low" -> "🔴 Crit Low"
                                "high" -> "🟡 High"
                                "low" -> "🟡 Low"
                                else -> "🟢 Normal"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Chevron expand icon
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Animated Speedometer / Gauge Bar (4dp height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                LinearProgressIndicator(
                    progress = { animatedGauge },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = statusColor,
                    trackColor = SoftMint
                )
            }

            // Collapsible Layman Accordion
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // What it is
                    if (biomarker.whatItIs.isNotBlank()) {
                        DetailSection(
                            icon = "📘",
                            title = "What it is",
                            description = biomarker.whatItIs
                        )
                    }

                    // Layman Explanation
                    if (biomarker.laymanExplanation.isNotBlank()) {
                        DetailSection(
                            icon = "💡",
                            title = "What this means",
                            description = biomarker.laymanExplanation
                        )
                    }

                    // Dietary Advice
                    if (biomarker.dietaryAdvice.isNotBlank()) {
                        DetailSection(
                            icon = "🥗",
                            title = "Dietary & Lifestyle Advice",
                            description = biomarker.dietaryAdvice
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSection(
    icon: String,
    title: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SoftMint.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = icon, fontSize = 11.sp)
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = DarkTeal
                )
            }
            Text(
                text = description,
                fontSize = 12.sp,
                color = TextDark,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Builds a structured medical context prompt string for seamless handoff to ChatbotScreen.
 */
private fun buildContextualChatPrompt(report: LabReportResponse, userQuestion: String): String {
    return buildString {
        appendLine("[CLINICAL LAB REPORT CONTEXT]")
        appendLine("Summary: ${report.summary}")
        val flagged = report.biomarkers.filter { it.status != "normal" }
        if (flagged.isNotEmpty()) {
            appendLine("Flagged Parameters:")
            flagged.forEach { b ->
                appendLine("- ${b.displayName}: ${b.value} ${b.unit} (Standard Range: ${b.referenceRange}, Status: ${b.status})")
            }
        } else {
            appendLine("All evaluated parameters are within normal reference ranges.")
        }
        appendLine()
        append("User Question: $userQuestion")
    }
}
