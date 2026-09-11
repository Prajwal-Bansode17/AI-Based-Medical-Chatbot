package ui

import androidx.compose.foundation.Image
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_based_medical_chatbot.R
import kotlin.math.roundToInt

@Composable
fun SymptomsCheckerScreen(
    onBackClick: () -> Unit = {}
) {

    val context = LocalContext.current

    // =========================================================
    // COLORS
    // =========================================================

    val primary = Color(0xFF087EA4)
    val darkBlue = Color(0xFF123A56)
    val gray = Color(0xFF71818C)

    val backgroundTop = Color(0xFFEAF8FC)
    val backgroundBottom = Color(0xFFD8F0F6)

    val softBlue = Color(0xFFF2FAFC)
    val softGreen = Color(0xFFEAF8F1)
    val green = Color(0xFF2E8B57)
    val softOrange = Color(0xFFFFF6E8)
    val orange = Color(0xFFB96A00)

    // =========================================================
    // DATA
    // =========================================================

    val symptoms = remember {
        SymptomsRepository.getSymptoms(context)
    }

    val selectedSymptoms = remember {
        mutableStateListOf<String>()
    }

    // Pre-normalize symptom names once so typing does not repeatedly
    // call trim/lowercase on the complete dataset.
    val normalizedSymptoms = remember(symptoms) {
        symptoms.map { symptom ->
            symptom to symptom.trim().lowercase()
        }
    }

    var symptomSearch by remember {
        mutableStateOf("")
    }

    var dropdownExpanded by remember {
        mutableStateOf(false)
    }

    var showResult by remember {
        mutableStateOf(false)
    }

    val predictions = remember(
        showResult,
        selectedSymptoms.toList()
    ) {
        if (showResult) {
            SymptomsRepository.predictConditions(
                context = context,
                selectedSymptoms = selectedSymptoms.toList(),
                limit = 3
            )
        } else {
            emptyList()
        }
    }

    val filteredSymptoms = remember(
        normalizedSymptoms,
        symptomSearch,
        selectedSymptoms.toList()
    ) {

        val search = symptomSearch
            .trim()
            .lowercase()

        val selectedSet = selectedSymptoms.toSet()

        normalizedSymptoms
            .asSequence()
            .filter { (symptom, _) ->
                !selectedSet.contains(symptom)
            }
            .filter { (_, normalizedName) ->
                search.isBlank() || normalizedName.contains(search)
            }
            .map { (symptom, _) -> symptom }
            .toList()
    }

    // =========================================================
    // SCREEN
    // =========================================================

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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),

            contentPadding = PaddingValues(
                start = 20.dp,
                top = 18.dp,
                end = 20.dp,
                bottom = 25.dp
            ),

            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // =====================================================
            // HEADER
            // =====================================================

            item {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(45.dp)
                            .background(
                                Color.White,
                                RoundedCornerShape(50.dp)
                            )
                            .clickable {
                                onBackClick()
                            },

                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "‹",
                            color = darkBlue,
                            fontSize = 36.sp
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(12.dp)
                    )

                    Column {

                        Text(
                            text = "Symptoms Checker",
                            color = darkBlue,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(3.dp)
                        )

                        Text(
                            text = "Understand what you are experiencing",
                            color = gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // =====================================================
            // INTRO CARD
            // =====================================================

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(25.dp),

                    colors = CardDefaults.cardColors(
                        containerColor = primary
                    ),

                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 8.dp
                    )
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),

                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .background(
                                    Color.White.copy(alpha = 0.18f),
                                    RoundedCornerShape(17.dp)
                                ),

                            contentAlignment = Alignment.Center
                        ) {

                            Image(
                                painter = painterResource(
                                    id = R.drawable.medassist_logo
                                ),

                                contentDescription = "MEDASSIST AI",

                                modifier = Modifier.size(42.dp),

                                contentScale = ContentScale.Fit
                            )
                        }

                        Spacer(
                            modifier = Modifier.width(14.dp)
                        )

                        Column {

                            Text(
                                text = "How are you feeling?",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(5.dp)
                            )

                            Text(
                                text = "Select all symptoms you are currently experiencing.",
                                color = Color.White.copy(alpha = 0.82f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // =====================================================
            // SELECT SYMPTOMS
            // =====================================================

            item {

                Column {

                    Text(
                        text = "Select Symptoms",
                        color = darkBlue,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = if (selectedSymptoms.isEmpty()) {
                            "Search and select one or more symptoms"
                        } else {
                            "${selectedSymptoms.size} symptom${
                                if (selectedSymptoms.size == 1) "" else "s"
                            } selected"
                        },

                        color = gray,
                        fontSize = 12.sp
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Card(
                            modifier = Modifier.fillMaxWidth(),

                            shape = RoundedCornerShape(18.dp),

                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),

                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 4.dp
                            )
                        ) {

                            OutlinedTextField(
                                value = symptomSearch,

                                onValueChange = {
                                    symptomSearch = it
                                    dropdownExpanded = true
                                },

                                modifier = Modifier.fillMaxWidth(),

                                singleLine = true,

                                placeholder = {
                                    Text(
                                        text = "Search symptoms..."
                                    )
                                },

                                trailingIcon = {

                                    Text(
                                        text = if (dropdownExpanded) "⌃" else "⌄",

                                        color = primary,

                                        fontSize = 22.sp,

                                        modifier = Modifier.clickable {
                                            dropdownExpanded =
                                                !dropdownExpanded
                                        }
                                    )
                                },

                                shape = RoundedCornerShape(17.dp),

                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = darkBlue,
                                    unfocusedTextColor = darkBlue,
                                    focusedPlaceholderColor = gray,
                                    unfocusedPlaceholderColor = gray,
                                    focusedBorderColor = primary,
                                    unfocusedBorderColor =
                                        Color(0xFFD8E5E9),
                                    cursorColor = primary
                                )
                            )
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = {
                                dropdownExpanded = false
                            },
                            modifier = Modifier
                                .width(340.dp)
                                .background(Color.White)
                        ) {

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(360.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {

                                if (filteredSymptoms.isEmpty()) {

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "No matching symptoms",
                                                color = gray
                                            )
                                        },
                                        onClick = {
                                            dropdownExpanded = false
                                        }
                                    )

                                } else {

                                    filteredSymptoms.forEach { symptom ->

                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = symptom,
                                                    color = darkBlue,
                                                    fontSize = 14.sp
                                                )
                                            },
                                            onClick = {
                                                if (!selectedSymptoms.contains(symptom)) {
                                                    selectedSymptoms.add(symptom)
                                                }

                                                symptomSearch = ""
                                                dropdownExpanded = false
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

            // =====================================================
            // SELECTED SYMPTOM CHIPS
            // =====================================================

            if (selectedSymptoms.isNotEmpty()) {

                item {

                    Card(
                        modifier = Modifier.fillMaxWidth(),

                        shape = RoundedCornerShape(20.dp),

                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),

                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 3.dp
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(15.dp)
                        ) {

                            Text(
                                text = "Selected Symptoms",
                                color = darkBlue,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(10.dp)
                            )

                            Column {

                                selectedSymptoms
                                    .chunked(2)
                                    .forEach { rowSymptoms ->

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),

                                            horizontalArrangement =
                                                Arrangement.spacedBy(8.dp)
                                        ) {

                                            rowSymptoms.forEach { symptom ->

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .background(
                                                            primary,
                                                            RoundedCornerShape(14.dp)
                                                        )
                                                        .clickable {

                                                            selectedSymptoms.remove(
                                                                symptom
                                                            )

                                                            showResult = false
                                                        }
                                                        .padding(
                                                            horizontal = 12.dp,
                                                            vertical = 9.dp
                                                        )
                                                ) {

                                                    Row(
                                                        verticalAlignment =
                                                            Alignment.CenterVertically
                                                    ) {

                                                        Text(
                                                            text = symptom,

                                                            color = Color.White,

                                                            fontSize = 12.sp,

                                                            modifier = Modifier
                                                                .weight(1f)
                                                        )

                                                        Spacer(
                                                            modifier =
                                                                Modifier.width(5.dp)
                                                        )

                                                        Text(
                                                            text = "×",

                                                            color = Color.White,

                                                            fontSize = 17.sp,

                                                            fontWeight =
                                                                FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }

                                            if (rowSymptoms.size == 1) {

                                                Spacer(
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }

                                        Spacer(
                                            modifier = Modifier.height(7.dp)
                                        )
                                    }
                            }
                        }
                    }
                }
            }

            // =====================================================
            // CHECK BUTTON
            // =====================================================

            item {

                Button(
                    onClick = {
                        showResult = true
                    },

                    enabled = selectedSymptoms.isNotEmpty(),

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),

                    shape = RoundedCornerShape(18.dp),

                    colors = ButtonDefaults.buttonColors(
                        containerColor = primary,
                        contentColor = Color.White,
                        disabledContentColor = Color.White,

                        disabledContainerColor =
                            Color(0xFFB7CBD1)
                    )
                ) {

                    Text(
                        text = "Check Symptoms",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // =====================================================
            // RESULTS
            // =====================================================

            if (showResult) {

                item {

                    Column {

                        // -------------------------------------------------
                        // RESULT HEADER
                        // -------------------------------------------------

                        Text(
                            text = "Health Screening Result",
                            color = darkBlue,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = "Possible conditions based on your selected symptoms",
                            color = gray,
                            fontSize = 12.sp
                        )

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        if (predictions.isEmpty()) {

                            Card(
                                modifier = Modifier.fillMaxWidth(),

                                shape = RoundedCornerShape(20.dp),

                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                ),

                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 5.dp
                                )
                            ) {

                                Column(
                                    modifier = Modifier.padding(20.dp)
                                ) {

                                    Text(
                                        text = "No close match found",
                                        color = darkBlue,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(
                                        modifier = Modifier.height(7.dp)
                                    )

                                    Text(
                                        text = "The selected symptoms do not closely match the conditions in the current screening dataset. If your symptoms continue or worsen, please consult a qualified healthcare professional.",

                                        color = gray,

                                        fontSize = 12.sp,

                                        lineHeight = 18.sp
                                    )
                                }
                            }

                        } else {

                            predictions.forEachIndexed {
                                    index,
                                    prediction ->

                                val matchedCount =
                                    prediction.matchedSymptoms.size

                                val selectedCount =
                                    selectedSymptoms.size

                                val matchPercentage =
                                    if (selectedCount > 0) {
                                        (
                                                matchedCount.toDouble() /
                                                        selectedCount.toDouble()
                                                ) * 100.0
                                    } else {
                                        0.0
                                    }

                                val roundedMatch =
                                    matchPercentage
                                        .roundToInt()
                                        .coerceIn(0, 100)

                                val isTopResult =
                                    index == 0

                                // -----------------------------------------
                                // TOP RESULT
                                // -----------------------------------------

                                Card(
                                    modifier = Modifier.fillMaxWidth(),

                                    shape = RoundedCornerShape(
                                        if (isTopResult) 22.dp else 18.dp
                                    ),

                                    colors = CardDefaults.cardColors(
                                        containerColor =
                                            if (isTopResult) {
                                                Color.White
                                            } else {
                                                softBlue
                                            }
                                    ),

                                    elevation =
                                        CardDefaults.cardElevation(
                                            defaultElevation =
                                                if (isTopResult) 7.dp else 3.dp
                                        )
                                ) {

                                    Column(
                                        modifier = Modifier.padding(
                                            if (isTopResult) 20.dp else 15.dp
                                        )
                                    ) {

                                        // ---------------------------------
                                        // RANK
                                        // ---------------------------------

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),

                                            verticalAlignment =
                                                Alignment.CenterVertically
                                        ) {

                                            Box(
                                                modifier = Modifier
                                                    .size(
                                                        if (isTopResult) {
                                                            42.dp
                                                        } else {
                                                            34.dp
                                                        }
                                                    )
                                                    .background(
                                                        if (isTopResult) {
                                                            primary
                                                        } else {
                                                            Color.White
                                                        },
                                                        RoundedCornerShape(50.dp)
                                                    ),

                                                contentAlignment =
                                                    Alignment.Center
                                            ) {

                                                Text(
                                                    text = if (isTopResult) {
                                                        "★"
                                                    } else {
                                                        "${index + 1}"
                                                    },

                                                    color = if (isTopResult) {
                                                        Color.White
                                                    } else {
                                                        primary
                                                    },

                                                    fontSize =
                                                        if (isTopResult) {
                                                            20.sp
                                                        } else {
                                                            14.sp
                                                        },

                                                    fontWeight =
                                                        FontWeight.Bold
                                                )
                                            }

                                            Spacer(
                                                modifier =
                                                    Modifier.width(12.dp)
                                            )

                                            Column(
                                                modifier =
                                                    Modifier.weight(1f)
                                            ) {

                                                if (isTopResult) {

                                                    Text(
                                                        text = "MOST POSSIBLE",

                                                        color = primary,

                                                        fontSize = 10.sp,

                                                        fontWeight =
                                                            FontWeight.Bold
                                                    )

                                                    Spacer(
                                                        modifier =
                                                            Modifier.height(2.dp)
                                                    )
                                                }

                                                Text(
                                                    text = prediction.condition,

                                                    color = darkBlue,

                                                    fontSize =
                                                        if (isTopResult) {
                                                            18.sp
                                                        } else {
                                                            15.sp
                                                        },

                                                    fontWeight =
                                                        FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(
                                    modifier = Modifier.height(9.dp)
                                )
                            }
                        }
                    }
                }

                // =====================================================
                // CHECK AGAIN BUTTON
                // =====================================================

                item {

                    Button(
                        onClick = {
                            selectedSymptoms.clear()
                            symptomSearch = ""
                            dropdownExpanded = false
                            showResult = false
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),

                        shape = RoundedCornerShape(17.dp),

                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = primary
                        )
                    ) {
                        Text(
                            text = "Check Again",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // =====================================================
            // DISCLAIMER
            // =====================================================

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(19.dp),

                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.90f)
                    ),

                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 1.dp
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(
                                text = "⚠",
                                color = orange,
                                fontSize = 19.sp
                            )

                            Spacer(
                                modifier = Modifier.width(8.dp)
                            )

                            Text(
                                text = "Important Medical Notice",
                                color = darkBlue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(7.dp)
                        )

                        Text(
                            text = "This tool provides educational symptom screening only. The results are possible matches, not a medical diagnosis and should not replace advice from a qualified healthcare professional.",

                            color = gray,

                            fontSize = 11.sp,

                            lineHeight = 17.sp
                        )

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )

                        Text(
                            text = "If you have severe, sudden, or worsening symptoms, seek appropriate medical care promptly.",

                            color = gray,

                            fontSize = 11.sp,

                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}