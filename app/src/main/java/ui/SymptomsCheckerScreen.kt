package ui

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_based_medical_chatbot.LocalAppLanguageController
import com.example.ai_based_medical_chatbot.R
import com.example.ai_based_medical_chatbot.appText
import com.example.ai_based_medical_chatbot.ui.theme.CoralAccentBorder
import com.example.ai_based_medical_chatbot.ui.theme.CoralAccentSoft
import com.example.ai_based_medical_chatbot.ui.theme.DarkTeal
import com.example.ai_based_medical_chatbot.ui.theme.DeepTeal
import com.example.ai_based_medical_chatbot.ui.theme.Ivory
import com.example.ai_based_medical_chatbot.ui.theme.Mint
import com.example.ai_based_medical_chatbot.ui.theme.PureWhite
import com.example.ai_based_medical_chatbot.ui.theme.SoftMint
import com.example.ai_based_medical_chatbot.ui.theme.StatusError
import com.example.ai_based_medical_chatbot.ui.theme.TextDark
import com.example.ai_based_medical_chatbot.ui.theme.TextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SymptomsCheckerScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val appLanguageController = LocalAppLanguageController.current
    val selectedLanguage = appLanguageController.selectedLanguage
    val keyboardController = LocalSoftwareKeyboardController.current

    fun t(key: String): String = appText(key, selectedLanguage)

    var allSymptoms by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf(SymptomCategory.COMMON) }
    var searchQuery by remember { mutableStateOf("") }
    var seeAllCommon by remember { mutableStateOf(false) }
    val selectedSymptoms = remember { mutableStateListOf<String>() }

    var showResult by remember { mutableStateOf(false) }
    var predictions by remember { mutableStateOf<List<SymptomPrediction>>(emptyList()) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var languageMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loading = true
        allSymptoms = withContext(Dispatchers.IO) {
            SymptomsRepository.getSymptoms(context)
        }
        loading = false
    }

    val topSixCommon = remember { SymptomCatalog.getTopSixCommon() }

    val displayedItems = remember(allSymptoms, searchQuery, selectedCategory, seeAllCommon, selectedLanguage) {
        val query = searchQuery.trim()
        if (query.isNotBlank()) {
            SymptomCatalog.search(query, selectedLanguage, null)
        } else if (selectedCategory == SymptomCategory.COMMON) {
            if (seeAllCommon) {
                SymptomCatalog.getByCategory(SymptomCategory.COMMON)
            } else {
                topSixCommon
            }
        } else {
            SymptomCatalog.getByCategory(selectedCategory)
        }
    }

    fun toggleSymptom(canonicalId: String) {
        if (canonicalId in selectedSymptoms) {
            selectedSymptoms.remove(canonicalId)
        } else {
            selectedSymptoms.add(canonicalId)
        }
        showResult = false
    }

    fun removeSymptom(canonicalId: String) {
        selectedSymptoms.remove(canonicalId)
        showResult = false
    }

    fun clearAllSymptoms() {
        selectedSymptoms.clear()
        showResult = false
    }

    fun performAnalysis() {
        if (selectedSymptoms.size < 2) {
            Toast.makeText(
                context,
                t("symptoms_select_at_least_two_toast"),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        coroutineScope.launch {
            isAnalyzing = true
            delay(250)
            predictions = withContext(Dispatchers.Default) {
                SymptomsRepository.predictConditions(
                    context = context,
                    selectedSymptoms = selectedSymptoms.toList(),
                    limit = 3
                )
            }
            isAnalyzing = false
            showResult = true
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Ivory, SoftMint.copy(alpha = 0.55f))
                )
            ),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!showResult) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    val isEnabled = selectedSymptoms.isNotEmpty()
                    val buttonBrush = if (isEnabled) {
                        Brush.horizontalGradient(listOf(DeepTeal, DarkTeal))
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                DeepTeal.copy(alpha = 0.45f),
                                DarkTeal.copy(alpha = 0.45f)
                            )
                        )
                    }

                    Button(
                        onClick = { performAnalysis() },
                        enabled = isEnabled && !isAnalyzing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(
                                elevation = if (isEnabled) 4.dp else 0.dp,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(buttonBrush)
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isAnalyzing) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        color = PureWhite,
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = t("symptoms_loading"),
                                        color = PureWhite,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = if (selectedSymptoms.isEmpty()) {
                                            t("symptoms_check")
                                        } else {
                                            "${t("symptoms_analyze_button")} (${selectedSymptoms.size})"
                                        },
                                        color = PureWhite,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = PureWhite,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->

        if (showResult) {
            // ==========================================
            // PREDICTION RESULTS VIEW (With Full Disclaimer)
            // ==========================================
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 10.dp,
                    end = 20.dp,
                    bottom = innerPadding.calculateBottomPadding() + 16.dp
                )
            ) {
                item(key = "result_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showResult = false },
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
                                contentDescription = "Back",
                                tint = DarkTeal,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = t("symptoms_analysis"),
                                color = DarkTeal,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${selectedSymptoms.size} ${t("symptoms_selected")}",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Selected Symptoms Chips Card
                item(key = "result_selected_chips") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, Mint.copy(alpha = 0.6f)),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = t("symptoms_selected"),
                                color = DarkTeal,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                selectedSymptoms.forEach { symptomId ->
                                    val displayName = SymptomCatalog.getDisplayName(symptomId, selectedLanguage)
                                    Box(
                                        modifier = Modifier
                                            .background(SoftMint, RoundedCornerShape(12.dp))
                                            .border(BorderStroke(1.dp, Mint), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = displayName,
                                            color = DarkTeal,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Predictions list
                if (predictions.isEmpty()) {
                    item(key = "result_no_prediction") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            border = BorderStroke(1.dp, Mint)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = t("symptoms_no_prediction"),
                                    color = DarkTeal,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                } else {
                    items(
                        count = predictions.size,
                        key = { "prediction_$it" }
                    ) { index ->
                        val prediction = predictions[index]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (index == 0) SoftMint else PureWhite
                            ),
                            border = BorderStroke(
                                1.5.dp,
                                if (index == 0) DeepTeal else Mint.copy(alpha = 0.7f)
                            ),
                            elevation = CardDefaults.cardElevation(if (index == 0) 3.dp else 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}. ${prediction.condition}",
                                        color = DarkTeal,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (index == 0) DeepTeal else Mint,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${prediction.score}%",
                                            color = if (index == 0) PureWhite else DarkTeal,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (prediction.matchedSymptoms.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val localizedMatched = prediction.matchedSymptoms.map {
                                        SymptomCatalog.getDisplayName(it, selectedLanguage)
                                    }
                                    Text(
                                        text = "${t("symptoms_matched")}: ${localizedMatched.joinToString(", ")}",
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // =======================================================
                // STEP 2 PROMINENT MEDICAL SAFETY DISCLAIMER & EMERGENCY
                // =======================================================
                item(key = "result_safety_disclaimer") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = CoralAccentSoft
                        ),
                        border = BorderStroke(1.dp, CoralAccentBorder),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = t("symptoms_disclaimer_title"),
                                    tint = StatusError,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = t("symptoms_disclaimer_title"),
                                    color = DarkTeal,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = t("symptoms_disclaimer_body"),
                                color = TextDark,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = t("symptoms_disclaimer_emergency"),
                                color = StatusError,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // General AI Disclaimer
                item(key = "result_ai_disclaimer") {
                    Text(
                        text = t("symptoms_ai_disclaimer"),
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }

                // Modify Symptoms Button
                item(key = "result_modify_button") {
                    Button(
                        onClick = { showResult = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftMint,
                            contentColor = DarkTeal
                        ),
                        border = BorderStroke(1.dp, Mint)
                    ) {
                        Text(
                            text = "← ${t("symptoms_checker")}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

        } else {
            // ==========================================
            // MAIN SYMPTOMS CHECKER SCREEN
            // ==========================================
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    top = 10.dp,
                    end = 18.dp,
                    bottom = innerPadding.calculateBottomPadding() + 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // ----------------------------------------------------
                // 1. TOP HEADER (Back, MedAssist AI Branding, Language)
                // ----------------------------------------------------
                item(key = "header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back Button
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
                                contentDescription = "Back",
                                tint = DarkTeal,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // App Title & Subtitle
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.medassist_logo),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "MedAssist AI",
                                    color = DarkTeal,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = t("symptoms_checker"),
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Language Selector Dropdown Pill
                        Box {
                            val langShortCode = when (selectedLanguage.lowercase()) {
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
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
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
                                modifier = Modifier.background(PureWhite)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "English",
                                            fontWeight = if (selectedLanguage == "English") FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedLanguage == "English") DeepTeal else TextDark
                                        )
                                    },
                                    onClick = {
                                        appLanguageController.setLanguage("English")
                                        languageMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "मराठी (Marathi)",
                                            fontWeight = if (selectedLanguage == "मराठी") FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedLanguage == "मराठी") DeepTeal else TextDark
                                        )
                                    },
                                    onClick = {
                                        appLanguageController.setLanguage("मराठी")
                                        languageMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "हिंदी (Hindi)",
                                            fontWeight = if (selectedLanguage == "हिंदी") FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedLanguage == "हिंदी") DeepTeal else TextDark
                                        )
                                    },
                                    onClick = {
                                        appLanguageController.setLanguage("हिंदी")
                                        languageMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // ----------------------------------------------------
                // 2. WELCOME HERO CARD (New Doctor Avatar)
                // ----------------------------------------------------
                item(key = "welcome_hero") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE2F4F0)
                        ),
                        border = BorderStroke(1.dp, Mint.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = t("symptoms_what_feeling"),
                                    color = DarkTeal,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 24.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = t("symptoms_search_browse_sub"),
                                    color = TextDark.copy(alpha = 0.75f),
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Image(
                                painter = painterResource(id = R.drawable.doctor_avatar),
                                contentDescription = "MedAssist Healthcare Doctor",
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                // ----------------------------------------------------
                // 3. SEARCH BAR
                // ----------------------------------------------------
                item(key = "search_field") {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            showResult = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(1.dp, RoundedCornerShape(24.dp)),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                        placeholder = {
                            Text(
                                text = t("symptoms_search_placeholder"),
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = DeepTeal,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = PureWhite,
                            focusedBorderColor = DeepTeal,
                            unfocusedBorderColor = Mint,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark,
                            cursorColor = DeepTeal
                        )
                    )
                }

                // ----------------------------------------------------
                // 4. CATEGORY CHIPS ROW
                // ----------------------------------------------------
                item(key = "category_chips") {
                    val categories = listOf(
                        SymptomCategory.COMMON to t("category_common"),
                        SymptomCategory.PAIN to t("category_pain"),
                        SymptomCategory.DIGESTIVE to t("category_digestive"),
                        SymptomCategory.RESPIRATORY to t("category_respiratory"),
                        SymptomCategory.SKIN to t("category_skin"),
                        SymptomCategory.MORE to t("category_more")
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = categories,
                            key = { it.first.name }
                        ) { (cat, label) ->
                            val isSelected = (selectedCategory == cat && searchQuery.isBlank())
                            val chipBg by animateColorAsState(
                                targetValue = if (isSelected) DeepTeal else PureWhite,
                                animationSpec = tween(200),
                                label = "cat_bg"
                            )
                            val chipTextColor by animateColorAsState(
                                targetValue = if (isSelected) PureWhite else DarkTeal,
                                animationSpec = tween(200),
                                label = "cat_text"
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(chipBg)
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isSelected) DeepTeal else Mint
                                        ),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable {
                                        selectedCategory = cat
                                        searchQuery = ""
                                        seeAllCommon = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = chipTextColor,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // ----------------------------------------------------
                // 5. SYMPTOMS SECTION (Common Grid vs Filtered List)
                // ----------------------------------------------------
                if (loading) {
                    item(key = "loading_indicator") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            border = BorderStroke(1.dp, Mint)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = DeepTeal,
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = t("symptoms_loading"),
                                    color = DarkTeal,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                } else if (searchQuery.isBlank() && selectedCategory == SymptomCategory.COMMON && !seeAllCommon) {
                    // ==========================================
                    // 3-COLUMN VISUAL CARDS GRID (Reference Image)
                    // ==========================================
                    item(key = "common_title") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = t("symptoms_common_title"),
                                color = DarkTeal,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${t("symptoms_see_all")} >",
                                color = DeepTeal,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { seeAllCommon = true }
                            )
                        }
                    }

                    item(key = "common_visual_grid") {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Row 1: Headache, Fever, Cough
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                topSixCommon.take(3).forEach { symptomModel ->
                                    val isSelected = symptomModel.id in selectedSymptoms
                                    SymptomVisualCard(
                                        model = symptomModel,
                                        isSelected = isSelected,
                                        selectedLanguage = selectedLanguage,
                                        onToggle = { toggleSymptom(symptomModel.id) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Row 2: Back Pain, Chest Pain, Nausea
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                topSixCommon.drop(3).take(3).forEach { symptomModel ->
                                    val isSelected = symptomModel.id in selectedSymptoms
                                    SymptomVisualCard(
                                        model = symptomModel,
                                        isSelected = isSelected,
                                        selectedLanguage = selectedLanguage,
                                        onToggle = { toggleSymptom(symptomModel.id) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // ==========================================
                    // FILTERED / EXTENDED SYMPTOMS LIST
                    // ==========================================
                    item(key = "filtered_title") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank())
                                    t("symptoms_matching")
                                else
                                    t("symptoms_all"),
                                color = DarkTeal,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )

                            if (seeAllCommon && searchQuery.isBlank() && selectedCategory == SymptomCategory.COMMON) {
                                Text(
                                    text = "< ${t("symptoms_show_less")}",
                                    color = DeepTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clickable { seeAllCommon = false }
                                        .padding(end = 8.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(SoftMint, RoundedCornerShape(10.dp))
                                    .border(BorderStroke(1.dp, Mint), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${displayedItems.size} ${t("symptoms_available")}",
                                    color = DeepTeal,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (displayedItems.isEmpty()) {
                        item(key = "empty_symptoms_result") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = PureWhite),
                                border = BorderStroke(1.dp, Mint)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = t("symptoms_none"),
                                        color = DarkTeal,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = t("symptoms_try_another"),
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    } else {
                        // Render in responsive pairs (2 columns) with stable bounds-checked keys
                        val rowCount = (displayedItems.size + 1) / 2
                        items(
                            count = rowCount,
                            key = { rowIndex ->
                                val firstIdx = rowIndex * 2
                                if (firstIdx < displayedItems.size) "row_${displayedItems[firstIdx].id}" else "row_$rowIndex"
                            }
                        ) { rowIndex ->
                            val firstIdx = rowIndex * 2
                            val secondIdx = firstIdx + 1

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val firstModel = displayedItems[firstIdx]
                                val isFirstSelected = firstModel.id in selectedSymptoms
                                SymptomCatalogCard(
                                    model = firstModel,
                                    isSelected = isFirstSelected,
                                    selectedLanguage = selectedLanguage,
                                    onToggle = { toggleSymptom(firstModel.id) },
                                    modifier = Modifier.weight(1f)
                                )

                                if (secondIdx < displayedItems.size) {
                                    val secondModel = displayedItems[secondIdx]
                                    val isSecondSelected = secondModel.id in selectedSymptoms
                                    SymptomCatalogCard(
                                        model = secondModel,
                                        isSelected = isSecondSelected,
                                        selectedLanguage = selectedLanguage,
                                        onToggle = { toggleSymptom(secondModel.id) },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // ----------------------------------------------------
                // 6. SELECTED SYMPTOMS PANEL
                // ----------------------------------------------------
                if (selectedSymptoms.isNotEmpty()) {
                    item(key = "selected_symptoms_panel") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            border = BorderStroke(1.dp, Mint),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = t("symptoms_selected"),
                                        color = DarkTeal,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(SoftMint, RoundedCornerShape(10.dp))
                                            .border(BorderStroke(1.dp, Mint), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${selectedSymptoms.size}",
                                            color = DeepTeal,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    Text(
                                        text = t("symptoms_clear_all"),
                                        color = StatusError,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable { clearAllSymptoms() }
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    selectedSymptoms.forEach { symptomId ->
                                        val displayName = SymptomCatalog.getDisplayName(symptomId, selectedLanguage)
                                        Box(
                                            modifier = Modifier
                                                .background(SoftMint, RoundedCornerShape(14.dp))
                                                .border(BorderStroke(1.dp, Mint), RoundedCornerShape(14.dp))
                                                .padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = displayName,
                                                    color = DarkTeal,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .clip(CircleShape)
                                                        .clickable { removeSymptom(symptomId) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Remove",
                                                        tint = TextMuted,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (selectedSymptoms.size < 2) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = t("symptoms_select_more_guidance"),
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ----------------------------------------------------
                // 7. COMPACT MEDICAL SAFETY DISCLAIMER
                // ----------------------------------------------------
                item(key = "compact_disclaimer") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = PureWhite.copy(alpha = 0.85f)
                        ),
                        border = BorderStroke(1.dp, Mint.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = DeepTeal,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = t("symptoms_disclaimer_compact"),
                                color = TextMuted,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3-column Visual Card matching reference image design with glowing circular icon badge.
 */
@Composable
private fun SymptomVisualCard(
    model: SymptomModel,
    isSelected: Boolean,
    selectedLanguage: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onToggle,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SoftMint else PureWhite
        ),
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) DeepTeal else Mint.copy(alpha = 0.65f)
        ),
        elevation = CardDefaults.cardElevation(if (isSelected) 3.dp else 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .background(DeepTeal, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = PureWhite,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SymptomVisualBadge(
                    iconType = model.iconType,
                    size = 50.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = model.localizedName(selectedLanguage),
                    color = DarkTeal,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 2-column Compact Catalog Card for extended categories and search results.
 */
@Composable
private fun SymptomCatalogCard(
    model: SymptomModel,
    isSelected: Boolean,
    selectedLanguage: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onToggle,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SoftMint else PureWhite
        ),
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) DeepTeal else Mint.copy(alpha = 0.65f)
        ),
        elevation = CardDefaults.cardElevation(if (isSelected) 2.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SymptomVisualBadge(
                iconType = model.iconType,
                size = 36.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = model.localizedName(selectedLanguage),
                color = DarkTeal,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(DeepTeal, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
