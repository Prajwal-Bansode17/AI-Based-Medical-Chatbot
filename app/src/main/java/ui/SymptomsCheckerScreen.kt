package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import com.example.ai_based_medical_chatbot.LocalAppLanguageController
import com.example.ai_based_medical_chatbot.appText
import com.example.ai_based_medical_chatbot.ui.theme.DarkTeal
import com.example.ai_based_medical_chatbot.ui.theme.DeepTeal
import com.example.ai_based_medical_chatbot.ui.theme.Ivory
import com.example.ai_based_medical_chatbot.ui.theme.Mint
import com.example.ai_based_medical_chatbot.ui.theme.PureWhite
import com.example.ai_based_medical_chatbot.ui.theme.SageGreen
import com.example.ai_based_medical_chatbot.ui.theme.SoftMint
import com.example.ai_based_medical_chatbot.ui.theme.StatusError
import com.example.ai_based_medical_chatbot.ui.theme.StatusSuccess
import com.example.ai_based_medical_chatbot.ui.theme.TextDark
import com.example.ai_based_medical_chatbot.ui.theme.TextMuted

@Composable
fun SymptomsCheckerScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current

    val appLanguageController = LocalAppLanguageController.current
    val selectedLanguage = appLanguageController.selectedLanguage
    val keyboardController = LocalSoftwareKeyboardController.current

    fun t(key: String): String = appText(key, selectedLanguage)

    val primary = DeepTeal
    val darkBlue = DarkTeal
    val gray = TextMuted
    val backgroundTop = Ivory
    val backgroundBottom = SoftMint
    val softBlue = SoftMint

    var allSymptoms by remember { mutableStateOf<List<String>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    val selectedSymptoms = remember { mutableStateListOf<String>() }
    var showResult by remember { mutableStateOf(false) }
    var predictions by remember { mutableStateOf<List<SymptomPrediction>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        allSymptoms = withContext(Dispatchers.IO) {
            SymptomsRepository.getSymptoms(context)
        }
        loading = false
    }

    val filteredSymptoms = remember(allSymptoms, searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) {
            allSymptoms
        } else {
            allSymptoms.filter {
                it.contains(query, ignoreCase = true)
            }
        }
    }

    val resultButtonScale by animateFloatAsState(
        targetValue = if (selectedSymptoms.isNotEmpty()) 1f else 0.98f,
        animationSpec = tween(250),
        label = "result_button_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(backgroundTop, backgroundBottom)
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            contentPadding = PaddingValues(
                start = 18.dp,
                top = 12.dp,
                end = 18.dp,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

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
                            text = t("symptoms_checker"),
                            color = DarkTeal,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = t("symptoms_subtitle"),
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkTeal),
                    border = BorderStroke(1.dp, Mint.copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(2.dp)
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
                                imageVector = StethoscopeVectorIcon,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = t("symptoms_how_feeling"),
                                color = PureWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = t("symptoms_search_help"),
                                color = PureWhite.copy(alpha = 0.85f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        showResult = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                        }
                    ),
                    placeholder = {
                        Text(
                            text = t("symptoms_search_placeholder"),
                            color = TextMuted,
                            fontSize = 14.sp
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
                            IconButton(
                                onClick = { searchQuery = "" }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
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

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchQuery.isBlank())
                            t("symptoms_all")
                        else
                            t("symptoms_matching"),
                        color = DarkTeal,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

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
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${filteredSymptoms.size} ${t("symptoms_available")}",
                            color = DeepTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (loading) {
                item {
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
                                text = t("symptoms_loading"),
                                color = DarkTeal,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = t("symptoms_loading_detail"),
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            } else if (filteredSymptoms.isEmpty()) {
                item {
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
                                text = t("symptoms_none"),
                                color = DarkTeal,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = t("symptoms_try_another"),
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                items(
                    items = filteredSymptoms,
                    key = { it }
                ) { symptom ->

                    val selected = selectedSymptoms.contains(symptom)

                    val containerColor by animateColorAsState(
                        targetValue = if (selected) DeepTeal else PureWhite,
                        animationSpec = tween(220),
                        label = "symptom_color"
                    )

                    val cardElevation by animateDpAsState(
                        targetValue = if (selected) 4.dp else 1.dp,
                        animationSpec = tween(220),
                        label = "symptom_elevation"
                    )

                    val indicatorScale by animateFloatAsState(
                        targetValue = if (selected) 1.04f else 1f,
                        animationSpec = tween(220),
                        label = "indicator_scale"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selected) {
                                    selectedSymptoms.remove(symptom)
                                } else {
                                    selectedSymptoms.add(symptom)
                                }
                                showResult = false
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = containerColor
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (selected) DarkTeal else Mint
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = cardElevation
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 14.dp,
                                    vertical = 12.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .scale(indicatorScale)
                                    .size(42.dp)
                                    .background(
                                        if (selected)
                                            PureWhite.copy(alpha = 0.18f)
                                        else
                                            SoftMint,
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = symptom
                                        .trim()
                                        .firstOrNull()
                                        ?.uppercase()
                                        ?: "+",
                                    color = if (selected) PureWhite else DeepTeal,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = symptom,
                                color = if (selected) PureWhite else DarkTeal,
                                fontSize = 14.sp,
                                fontWeight = if (selected)
                                    FontWeight.SemiBold
                                else
                                    FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            AnimatedVisibility(
                                visible = selected,
                                enter = fadeIn(tween(180)) + scaleIn(tween(180)),
                                exit = fadeOut(tween(120))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .background(PureWhite, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = DeepTeal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = selectedSymptoms.isNotEmpty(),
                    enter = fadeIn(tween(220)) + scaleIn(tween(220)),
                    exit = fadeOut(tween(150))
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = PureWhite
                        ),
                        border = BorderStroke(1.dp, Mint),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = t("symptoms_selected"),
                                    color = DarkTeal,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .background(SoftMint, RoundedCornerShape(10.dp))
                                        .border(BorderStroke(1.dp, Mint), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${selectedSymptoms.size}",
                                        color = DeepTeal,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            selectedSymptoms.forEach { symptom ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = SoftMint
                                    ),
                                    border = BorderStroke(1.dp, Mint)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = 12.dp,
                                                vertical = 8.dp
                                            ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = DeepTeal,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = symptom,
                                            color = DarkTeal,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clickable {
                                                    selectedSymptoms.remove(symptom)
                                                    showResult = false
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "×",
                                                color = TextMuted,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        if (selectedSymptoms.size >= 2) {
                            predictions = SymptomsRepository.predictConditions(
                                context = context,
                                selectedSymptoms = selectedSymptoms.toList(),
                                limit = 3
                            )
                        } else {
                            predictions = emptyList()
                        }
                        showResult = true
                    },
                    enabled = selectedSymptoms.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .scale(resultButtonScale),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepTeal,
                        contentColor = PureWhite,
                        disabledContainerColor = Mint.copy(alpha = 0.7f),
                        disabledContentColor = TextMuted
                    )
                ) {
                    Text(
                        text = if (selectedSymptoms.size < 2)
                            t("symptoms_select_two")
                        else
                            t("symptoms_check"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (showResult) {
                item {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(300)) + scaleIn(tween(250))
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = PureWhite
                            ),
                            border = BorderStroke(1.dp, Mint),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp)
                            ) {
                                Text(
                                    text = t("symptoms_analysis"),
                                    color = DarkTeal,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = selectedSymptoms.joinToString(", "),
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                if (selectedSymptoms.size < 2) {
                                    Text(
                                        text = t("symptoms_need_two"),
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp
                                    )
                                } else if (predictions.isEmpty()) {
                                    Text(
                                        text = t("symptoms_no_prediction"),
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp
                                    )
                                } else {
                                    predictions.forEachIndexed { index, prediction ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (index == 0)
                                                    SoftMint
                                                else
                                                    Ivory
                                            ),
                                            border = BorderStroke(
                                                1.dp,
                                                if (index == 0) DeepTeal.copy(alpha = 0.35f) else Mint
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(13.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${index + 1}. ${prediction.condition}",
                                                        color = DarkTeal,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                if (index == 0) DeepTeal else Mint,
                                                                RoundedCornerShape(8.dp)
                                                            )
                                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                                    ) {
                                                        Text(
                                                            text = "${prediction.score}%",
                                                            color = if (index == 0) PureWhite else DarkTeal,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                if (prediction.matchedSymptoms.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = "${t("symptoms_matched")}: ${
                                                            prediction.matchedSymptoms.joinToString(
                                                                ", "
                                                            )
                                                        }",
                                                        color = TextMuted,
                                                        fontSize = 11.sp,
                                                        lineHeight = 16.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = t("symptoms_ai_disclaimer"),
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SoftMint
                    ),
                    border = BorderStroke(1.dp, Mint)
                ) {
                    Text(
                        text = t("symptoms_medical_disclaimer"),
                        modifier = Modifier.padding(14.dp),
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}
