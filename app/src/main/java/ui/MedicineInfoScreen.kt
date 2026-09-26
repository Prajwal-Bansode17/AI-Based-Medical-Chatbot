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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class Medicine(
    val name: String,
    val category: String,
    val uses: List<String>,
    val precautions: List<String>,
    val commonSideEffects: List<String>,
    val warning: String,
    val genericName: String = ""
)

private fun MedicineRecord.toMedicine(): Medicine {

    val useList = uses
        .split("\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .take(8)

    val precautionList = precautions
        .split("\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .take(8)

    val sideEffectList = sideEffects
        .split("\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .take(8)

    return Medicine(
        name = name.ifBlank {
            generic.ifBlank {
                brand.ifBlank {
                    "Unknown Medicine"
                }
            }
        },
        category = drugClass.ifBlank {
            "Medicine"
        },
        uses = useList.ifEmpty {
            listOf(
                "Uses information is not available in this record."
            )
        },
        precautions = precautionList.ifEmpty {
            listOf(
                "Check the official product label or consult a qualified healthcare professional."
            )
        },
        commonSideEffects = sideEffectList.ifEmpty {
            listOf(
                "Side-effect information is not available in this record."
            )
        },
        warning = warnings.ifBlank {
            "This information is educational and does not replace professional medical advice."
        },
        genericName = generic
    )
}

private suspend fun loadMedicineResults(
    context: android.content.Context,
    query: String
): List<MedicineRecord> {

    return withContext(Dispatchers.IO) {

        if (query.isBlank()) {

            MedicineRepository
                .getMedicines(context)
                .take(20)

        } else {

            MedicineRepository.searchMedicines(
                context = context,
                query = query,
                limit = 20
            )
        }
    }
}

@Composable
fun MedicineInfoScreen(
    onBackClick: () -> Unit,
    onMedicineClick: (Medicine) -> Unit
) {

    val context = LocalContext.current

    var searchQuery by remember {
        mutableStateOf("")
    }

    var searchResults by remember {
        mutableStateOf<List<MedicineRecord>>(emptyList())
    }

    var isSearching by remember {
        mutableStateOf(true)
    }

    /*
     * Search is delayed by 300 ms so that the database
     * is not searched for every character typed.
     *
     * The actual medicine search runs on Dispatchers.IO,
     * keeping the Compose UI thread responsive.
     */
    LaunchedEffect(searchQuery) {

        isSearching = true

        delay(300)

        val results = loadMedicineResults(
            context = context,
            query = searchQuery
        )

        searchResults = results

        isSearching = false
    }

    val filteredMedicines = remember(searchResults) {
        searchResults.map {
            it.toMedicine()
        }
    }

    val primary = Color(0xFF087EA4)
    val darkBlue = Color(0xFF123A56)
    val gray = Color(0xFF71818C)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFEAF8FC),
                        Color(0xFFD8F0F6)
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

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            // =====================================================
            // HEADER
            // =====================================================

            item {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(45.dp)
                            .background(
                                Color.White,
                                CircleShape
                            )
                            .clickable {
                                onBackClick()
                            },

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text = "‹",
                            color = darkBlue,
                            fontSize = 36.sp,
                            fontWeight =
                                FontWeight.Light
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.width(12.dp)
                    )

                    Column {

                        Text(
                            text =
                                "Medicine Information",

                            color =
                                darkBlue,

                            fontSize =
                                22.sp,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(3.dp)
                        )

                        Text(
                            text =
                                "Search medicines by name, generic or brand",

                            color =
                                gray,

                            fontSize =
                                12.sp
                        )
                    }
                }
            }

            // =====================================================
            // SEARCH
            // =====================================================

            item {

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(22.dp),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color.White
                        ),

                    elevation =
                        CardDefaults.cardElevation(
                            defaultElevation = 5.dp
                        )
                ) {

                    OutlinedTextField(

                        value =
                            searchQuery,

                        onValueChange = { value ->
                            searchQuery = value
                        },

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(7.dp),

                        singleLine = true,

                        placeholder = {

                            Text(
                                text =
                                    "Search any medicine..."
                            )
                        },

                        leadingIcon = {

                            Text(
                                text = "⌕",
                                color =
                                    primary,
                                fontSize =
                                    27.sp
                            )
                        },

                        shape =
                            RoundedCornerShape(17.dp),

                        colors =
                            OutlinedTextFieldDefaults.colors(

                                focusedTextColor =
                                    darkBlue,

                                unfocusedTextColor =
                                    darkBlue,

                                focusedPlaceholderColor =
                                    gray,

                                unfocusedPlaceholderColor =
                                    gray,

                                focusedBorderColor =
                                    primary,

                                unfocusedBorderColor =
                                    Color(0xFFD8E5E9),

                                cursorColor =
                                    primary
                            )
                    )
                }
            }

            // =====================================================
            // INTRO CARD
            // =====================================================

            item {

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(25.dp),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                primary
                        ),

                    elevation =
                        CardDefaults.cardElevation(
                            defaultElevation = 8.dp
                        )
                ) {

                    Row(
                        modifier =
                            Modifier.padding(20.dp),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .size(57.dp)
                                    .background(
                                        Color.White.copy(
                                            alpha = 0.18f
                                        ),
                                        RoundedCornerShape(17.dp)
                                    ),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Image(
                                painter =
                                    painterResource(
                                        id =
                                            R.drawable.medassist_logo
                                    ),

                                contentDescription =
                                    "MEDASSIST AI",

                                modifier =
                                    Modifier.size(42.dp),

                                contentScale =
                                    ContentScale.Fit
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.width(14.dp)
                        )

                        Column {

                            Text(
                                text =
                                    "Universal Medicine Search",

                                color =
                                    Color.White,

                                fontSize =
                                    19.sp,

                                fontWeight =
                                    FontWeight.Bold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )

                            Text(
                                text =
                                    "Search across the MEDASSIST medicine database.",

                                color =
                                    Color.White.copy(
                                        alpha = 0.82f
                                    ),

                                fontSize =
                                    12.sp
                            )
                        }
                    }
                }
            }

            // =====================================================
            // SECTION TITLE
            // =====================================================

            item {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 3.dp)
                ) {

                    Text(
                        text =
                            if (searchQuery.isBlank()) {
                                "Available Medicines"
                            } else {
                                "Search Results"
                            },

                        color =
                            darkBlue,

                        fontSize =
                            19.sp,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(3.dp)
                    )

                    val resultText =
                        if (isSearching) {
                            "Searching medicines..."
                        } else if (searchQuery.isBlank()) {
                            "Showing ${filteredMedicines.size} medicines"
                        } else {
                            "${filteredMedicines.size} result${
                                if (filteredMedicines.size == 1) {
                                    ""
                                } else {
                                    "s"
                                }
                            } found"
                        }

                    Text(
                        text = resultText,
                        color = gray,
                        fontSize = 12.sp
                    )
                }
            }

            // =====================================================
            // SEARCHING
            // =====================================================

            if (isSearching) {

                item {

                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),

                        shape =
                            RoundedCornerShape(20.dp),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    Color.White
                            )
                    ) {

                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(25.dp),

                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(
                                text =
                                    "Searching...",

                                color =
                                    primary,

                                fontSize =
                                    15.sp,

                                fontWeight =
                                    FontWeight.SemiBold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(5.dp)
                            )

                            Text(
                                text =
                                    "Finding matching medicines",

                                color =
                                    gray,

                                fontSize =
                                    12.sp
                            )
                        }
                    }
                }

            } else if (filteredMedicines.isEmpty()) {

                // =================================================
                // NO RESULTS
                // =================================================

                item {

                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),

                        shape =
                            RoundedCornerShape(20.dp),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    Color.White
                            )
                    ) {

                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(25.dp),

                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = "⌕",

                                color =
                                    primary,

                                fontSize =
                                    35.sp
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            Text(
                                text =
                                    "No medicines found",

                                color =
                                    darkBlue,

                                fontSize =
                                    16.sp,

                                fontWeight =
                                    FontWeight.Bold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )

                            Text(
                                text =
                                    "Try the generic name, brand name or another spelling.",

                                color =
                                    gray,

                                fontSize =
                                    12.sp
                            )
                        }
                    }
                }

            } else {

                // =================================================
                // RESULTS
                // =================================================

                items(
                    items = filteredMedicines
                ) { medicine ->

                    MedicineCard(
                        medicine = medicine,

                        onClick = {
                            onMedicineClick(medicine)
                        }
                    )
                }
            }

            // =====================================================
            // DISCLAIMER
            // =====================================================

            item {

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(18.dp),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color.White.copy(
                                    alpha = 0.88f
                                )
                        )
                ) {

                    Text(
                        text =
                            "⚠ Medicine information is for educational purposes only. Always follow the product label or consult a qualified healthcare professional.",

                        modifier =
                            Modifier.padding(16.dp),

                        color =
                            gray,

                        fontSize =
                            11.sp,

                        lineHeight =
                            17.sp
                    )
                }
            }
        }
    }
}

// =============================================================
// MEDICINE CARD
// =============================================================

@Composable
private fun MedicineCard(
    medicine: Medicine,
    onClick: () -> Unit
) {

    val primary =
        Color(0xFF087EA4)

    val darkBlue =
        Color(0xFF123A56)

    val gray =
        Color(0xFF71818C)

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                },

        shape =
            RoundedCornerShape(20.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 5.dp
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(17.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier =
                    Modifier
                        .size(52.dp)
                        .background(
                            Color(0xFFE8F7FB),
                            RoundedCornerShape(16.dp)
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = "✚",

                    color =
                        primary,

                    fontSize =
                        22.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.width(14.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        medicine.name,

                    color =
                        darkBlue,

                    fontSize =
                        16.sp,

                    fontWeight =
                        FontWeight.Bold,

                    maxLines = 2
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text =
                        medicine.category,

                    color =
                        gray,

                    fontSize =
                        12.sp,

                    maxLines = 2
                )
            }

            Text(
                text = "›",

                color =
                    primary,

                fontSize =
                    30.sp
            )
        }
    }
}