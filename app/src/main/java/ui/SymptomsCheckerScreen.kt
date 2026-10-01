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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.ai_based_medical_chatbot.LocalAppLanguageController
import com.example.ai_based_medical_chatbot.appText

@Composable
fun SymptomsCheckerScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current

    val appLanguageController = LocalAppLanguageController.current
    val selectedLanguage = appLanguageController.selectedLanguage

    fun t(key: String): String = appText(key, selectedLanguage)


    val primary = Color(0xFF087EA4)
    val darkBlue = Color(0xFF123A56)
    val gray = Color(0xFF71818C)
    val backgroundTop = Color(0xFFEAF8FC)
    val backgroundBottom = Color(0xFFD8F0F6)
    val softBlue = Color(0xFFE8F7FB)

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
                .navigationBarsPadding(),
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
                    IconButton(onClick = onBackClick) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "‹",
                                color = darkBlue,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Light
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = t("symptoms_checker"),
                            color = darkBlue,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = t("symptoms_subtitle"),
                            color = gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = primary),
                    elevation = CardDefaults.cardElevation(7.dp)
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
                                    Color.White.copy(alpha = 0.18f),
                                    RoundedCornerShape(17.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                color = Color.White,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Light
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = t("symptoms_how_feeling"),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = t("symptoms_search_help"),
                                color = Color.White.copy(alpha = 0.84f),
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
                    placeholder = {
                        Text(
                            t("symptoms_search_placeholder"),
                            color = gray
                        )
                    },
                    leadingIcon = {
                        Text(
                            text = "⌕",
                            color = primary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = { searchQuery = "" }
                            ) {
                                Text(
                                    text = "×",
                                    color = gray,
                                    fontSize = 22.sp
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(17.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = primary,
                        unfocusedBorderColor = Color(0xFFD7E7EC),
                        focusedTextColor = darkBlue,
                        unfocusedTextColor = darkBlue,
                        cursorColor = primary
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
                        color = darkBlue,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "${filteredSymptoms.size} ${t("symptoms_available")}",
                        color = primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (loading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = t("symptoms_loading"),
                                color = darkBlue,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = t("symptoms_loading_detail"),
                                color = gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            } else if (filteredSymptoms.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = t("symptoms_none"),
                                color = darkBlue,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = t("symptoms_try_another"),
                                color = gray,
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
                        targetValue = if (selected) primary else Color.White,
                        animationSpec = tween(220),
                        label = "symptom_color"
                    )

                    val cardElevation by animateDpAsState(
                        targetValue = if (selected) 8.dp else 3.dp,
                        animationSpec = tween(220),
                        label = "symptom_elevation"
                    )

                    val indicatorScale by animateFloatAsState(
                        targetValue = if (selected) 1.06f else 1f,
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
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = containerColor
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = cardElevation
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 15.dp,
                                    vertical = 13.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .scale(indicatorScale)
                                    .size(46.dp)
                                    .background(
                                        if (selected)
                                            Color.White.copy(alpha = 0.18f)
                                        else
                                            softBlue,
                                        RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = symptom
                                        .trim()
                                        .firstOrNull()
                                        ?.uppercase()
                                        ?: "+",
                                    color = if (selected) Color.White else primary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(13.dp))

                            Text(
                                text = symptom,
                                color = if (selected) Color.White else darkBlue,
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
                                        .size(28.dp)
                                        .background(Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "✓",
                                        color = primary,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
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
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(3.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = t("symptoms_selected"),
                                    color = darkBlue,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${selectedSymptoms.size}",
                                    color = primary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            selectedSymptoms.forEach { symptom ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp),
                                    shape = RoundedCornerShape(11.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = softBlue
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = 11.dp,
                                                vertical = 9.dp
                                            ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "✓",
                                            color = primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = symptom,
                                            color = darkBlue,
                                            fontSize = 12.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "×",
                                            color = gray,
                                            fontSize = 18.sp,
                                            modifier = Modifier.clickable {
                                                selectedSymptoms.remove(symptom)
                                                showResult = false
                                            }
                                        )
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
                        .height(56.dp)
                        .scale(resultButtonScale),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primary,
                        disabledContainerColor = Color(0xFFB7CBD1)
                    )
                ) {
                    Text(
                        text = if (selectedSymptoms.size < 2)
                            t("symptoms_select_two")
                        else
                            t("symptoms_check"),
                        fontSize = 14.sp,
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
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(5.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(19.dp)
                            ) {
                                Text(
                                    text = t("symptoms_analysis"),
                                    color = primary,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(7.dp))

                                Text(
                                    text = selectedSymptoms.joinToString(", "),
                                    color = darkBlue,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                if (selectedSymptoms.size < 2) {
                                    Text(
                                        text = t("symptoms_need_two"),
                                        color = gray,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp
                                    )
                                } else if (predictions.isEmpty()) {
                                    Text(
                                        text = t("symptoms_no_prediction"),
                                        color = gray,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp
                                    )
                                } else {
                                    predictions.forEachIndexed { index, prediction ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp),
                                            shape = RoundedCornerShape(15.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (index == 0)
                                                    softBlue
                                                else
                                                    Color(0xFFF7FAFB)
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
                                                        color = darkBlue,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Text(
                                                        text = "${prediction.score}%",
                                                        color = primary,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                if (prediction.matchedSymptoms.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(5.dp))
                                                    Text(
                                                        text = "${t("symptoms_matched")}: ${
                                                            prediction.matchedSymptoms.joinToString(
                                                                ", "
                                                            )
                                                        }",
                                                        color = gray,
                                                        fontSize = 11.sp,
                                                        lineHeight = 16.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = t("symptoms_ai_disclaimer"),
                                        color = gray,
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
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.9f)
                    )
                ) {
                    Text(
                        text = t("symptoms_medical_disclaimer"),
                        modifier = Modifier.padding(16.dp),
                        color = gray,
                        fontSize = 11.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}
