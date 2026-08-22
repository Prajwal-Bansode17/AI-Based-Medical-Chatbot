package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// ============================================================
// THEME COLORS
// Same theme as Symptoms Checker
// ============================================================

private val BackgroundColor = Color(0xFF071A33)
private val CardColor = Color(0xFF123653)
private val SelectedCardColor = Color(0xFF0D5C70)
private val PrimaryTextColor = Color.White
private val SecondaryTextColor = Color(0xFFB9EAF2)
private val AccentColor = Color(0xFF087EA4)
private val LightAccentColor = Color(0xFF7DE8F0)


// ============================================================
// MEDICINE DATA MODEL
// ============================================================

data class Medicine(
    val name: String,
    val category: String,
    val uses: List<String>,
    val precautions: List<String>,
    val commonSideEffects: List<String>,
    val warning: String
)


// ============================================================
// MEDICINE DATA
// ============================================================

private val medicines = listOf(

    Medicine(
        name = "Paracetamol",
        category = "Pain & Fever",
        uses = listOf(
            "Fever",
            "Mild to moderate pain",
            "Headache"
        ),
        precautions = listOf(
            "Do not exceed the recommended amount.",
            "Check other medicines for paracetamol content.",
            "People with liver problems should seek professional advice."
        ),
        commonSideEffects = listOf(
            "Usually well tolerated when used appropriately."
        ),
        warning = "This information is educational and does not replace professional medical advice."
    ),

    Medicine(
        name = "Ibuprofen",
        category = "Pain & Fever",
        uses = listOf(
            "Mild to moderate pain",
            "Inflammation",
            "Fever"
        ),
        precautions = listOf(
            "May not be suitable for people with certain stomach or kidney conditions.",
            "Take only according to the product label or professional advice.",
            "Check for interactions with other medicines."
        ),
        commonSideEffects = listOf(
            "Stomach discomfort",
            "Nausea",
            "Indigestion"
        ),
        warning = "This information is educational and does not replace professional medical advice."
    ),

    Medicine(
        name = "Cetirizine",
        category = "Allergy",
        uses = listOf(
            "Allergy symptoms",
            "Sneezing",
            "Runny nose",
            "Itching"
        ),
        precautions = listOf(
            "May cause drowsiness in some people.",
            "Avoid activities requiring alertness if you feel drowsy.",
            "Check with a healthcare professional if taking other medicines."
        ),
        commonSideEffects = listOf(
            "Drowsiness",
            "Dry mouth",
            "Tiredness"
        ),
        warning = "This information is educational and does not replace professional medical advice."
    ),

    Medicine(
        name = "Omeprazole",
        category = "Acid & Stomach",
        uses = listOf(
            "Heartburn",
            "Acid reflux",
            "Certain stomach acid conditions"
        ),
        precautions = listOf(
            "Use according to the product label or professional advice.",
            "Long-term use should be discussed with a healthcare professional.",
            "Tell your healthcare professional about other medicines you take."
        ),
        commonSideEffects = listOf(
            "Headache",
            "Stomach discomfort",
            "Nausea"
        ),
        warning = "This information is educational and does not replace professional medical advice."
    ),

    Medicine(
        name = "Antacid",
        category = "Acid & Stomach",
        uses = listOf(
            "Occasional heartburn",
            "Acid indigestion",
            "Temporary relief of stomach acidity"
        ),
        precautions = listOf(
            "Follow the instructions on the specific product.",
            "Some antacids can interact with other medicines.",
            "People with kidney problems should seek professional advice."
        ),
        commonSideEffects = listOf(
            "Constipation",
            "Diarrhea",
            "Stomach discomfort"
        ),
        warning = "Frequent or persistent heartburn should be discussed with a healthcare professional."
    ),

    Medicine(
        name = "Saline Nasal Spray",
        category = "Cold & Nasal",
        uses = listOf(
            "Nasal dryness",
            "Nasal congestion",
            "Helps moisturize nasal passages"
        ),
        precautions = listOf(
            "Use according to the product instructions.",
            "Do not share a nasal spray with another person.",
            "Keep the applicator clean."
        ),
        commonSideEffects = listOf(
            "Temporary nasal irritation",
            "Mild discomfort"
        ),
        warning = "This information is educational and does not replace professional medical advice."
    ),

    Medicine(
        name = "Antiseptic",
        category = "First Aid",
        uses = listOf(
            "Cleaning minor cuts and wounds",
            "Basic first-aid hygiene",
            "Reducing contamination of minor wounds"
        ),
        precautions = listOf(
            "Use only as directed on the product label.",
            "Avoid contact with eyes.",
            "Seek medical care for deep or serious wounds."
        ),
        commonSideEffects = listOf(
            "Skin irritation",
            "Dryness",
            "Stinging"
        ),
        warning = "Serious, deep, infected, or heavily bleeding wounds require professional medical attention."
    ),

    Medicine(
        name = "ORS",
        category = "Hydration",
        uses = listOf(
            "Helps replace fluids and electrolytes.",
            "Dehydration associated with diarrhea or vomiting."
        ),
        precautions = listOf(
            "Prepare according to the instructions on the packet.",
            "Use clean water when preparing the solution.",
            "Seek medical help for severe dehydration."
        ),
        commonSideEffects = listOf(
            "Generally well tolerated when prepared and used correctly."
        ),
        warning = "This information is educational and does not replace professional medical advice."
    ),

    Medicine(
        name = "Electrolyte Drink",
        category = "Health & Wellness",
        uses = listOf(
            "Provides fluids and electrolytes",
            "May support hydration during certain activities",
            "General hydration support"
        ),
        precautions = listOf(
            "Check the sugar and caffeine content.",
            "Not every sports or energy drink is appropriate during illness.",
            "People with certain medical conditions should seek professional advice."
        ),
        commonSideEffects = listOf(
            "Stomach discomfort may occur with some products."
        ),
        warning = "This information is educational and does not replace professional medical advice."
    ),

    Medicine(
        name = "Energy Drink Information",
        category = "Health & Wellness",
        uses = listOf(
            "Provides caffeine and other ingredients intended to increase alertness",
            "General information about energy drinks",
            "Understanding caffeine and sugar content"
        ),
        precautions = listOf(
            "Check caffeine and sugar content before consumption.",
            "Avoid excessive caffeine intake.",
            "People who are sensitive to caffeine should be cautious."
        ),
        commonSideEffects = listOf(
            "Difficulty sleeping",
            "Jitteriness",
            "Increased heart rate"
        ),
        warning = "Energy drinks are not medicines and should not be used to treat medical conditions."
    )
)


// ============================================================
// MEDICINE INFORMATION SCREEN
// ============================================================

@Composable
fun MedicineInfoScreen(
    onBackClick: () -> Unit,
    onMedicineClick: (Medicine) -> Unit
) {

    var searchQuery by remember {
        mutableStateOf("")
    }

    var selectedCategory by remember {
        mutableStateOf("All")
    }

    val categories = listOf(
        "All",
        "Pain & Fever",
        "Allergy",
        "Acid & Stomach",
        "Cold & Nasal",
        "First Aid",
        "Hydration",
        "Health & Wellness"
    )

    val filteredMedicines = medicines.filter { medicine ->

        val query = searchQuery.trim()

        val matchesSearch =
            query.isEmpty() ||
                    medicine.name.contains(query, ignoreCase = true) ||
                    medicine.category.contains(query, ignoreCase = true) ||
                    medicine.uses.any {
                        it.contains(query, ignoreCase = true)
                    }

        val matchesCategory =
            selectedCategory == "All" ||
                    medicine.category == selectedCategory

        matchesSearch && matchesCategory
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {

        // =====================================================
        // HEADER
        // =====================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBackClick
            ) {

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = PrimaryTextColor
                )
            }

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Column {

                Text(
                    text = "Medicine Information",
                    color = PrimaryTextColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Medicines, first aid & wellness",
                    color = SecondaryTextColor,
                    fontSize = 12.sp
                )
            }
        }


        // =====================================================
        // CONTENT
        // =====================================================

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            item {

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "Medicine & Health Information",
                    color = PrimaryTextColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "Search medicines and explore general health information.",
                    color = SecondaryTextColor,
                    fontSize = 14.sp
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )


                // =================================================
                // SEARCH
                // =================================================

                OutlinedTextField(

                    value = searchQuery,

                    onValueChange = {
                        searchQuery = it
                    },

                    modifier = Modifier.fillMaxWidth(),

                    singleLine = true,

                    leadingIcon = {

                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = LightAccentColor
                        )
                    },

                    trailingIcon = {

                        if (searchQuery.isNotEmpty()) {

                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                }
                            ) {

                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = SecondaryTextColor
                                )
                            }
                        }
                    },

                    placeholder = {
                        Text(
                            text = "Search medicine or symptom...",
                            color = SecondaryTextColor
                        )
                    },

                    colors = OutlinedTextFieldDefaults.colors(

                        focusedTextColor = PrimaryTextColor,
                        unfocusedTextColor = PrimaryTextColor,

                        focusedBorderColor = LightAccentColor,
                        unfocusedBorderColor = Color(0xFF456B82),

                        cursorColor = LightAccentColor,

                        focusedLabelColor = LightAccentColor,
                        unfocusedLabelColor = SecondaryTextColor
                    ),

                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )


                // =================================================
                // CATEGORY TITLE
                // =================================================

                Text(
                    text = "Categories",
                    color = PrimaryTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }


            // =====================================================
            // CATEGORY CHIPS
            // =====================================================

            item {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(
                            rememberScrollState()
                        ),

                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    categories.forEach { category ->

                        Card(

                            modifier = Modifier.clickable {

                                selectedCategory = category

                            },

                            shape =
                                RoundedCornerShape(20.dp),

                            colors =
                                CardDefaults.cardColors(

                                    containerColor =
                                        if (
                                            selectedCategory == category
                                        ) {
                                            SelectedCardColor
                                        } else {
                                            CardColor
                                        }
                                )
                        ) {

                            Text(
                                text = category,

                                color =
                                    if (
                                        selectedCategory == category
                                    ) {
                                        LightAccentColor
                                    } else {
                                        PrimaryTextColor
                                    },

                                fontSize = 13.sp,

                                fontWeight =
                                    FontWeight.Medium,

                                modifier =
                                    Modifier.padding(
                                        horizontal = 15.dp,
                                        vertical = 9.dp
                                    )
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(4.dp)
                )
            }


            // =====================================================
            // RESULT COUNT
            // =====================================================

            item {

                Text(

                    text =
                        when (filteredMedicines.size) {

                            0 ->
                                "No information found"

                            1 ->
                                "1 result found"

                            else ->
                                "${filteredMedicines.size} results found"
                        },

                    color = SecondaryTextColor,

                    fontSize = 13.sp,

                    fontWeight =
                        FontWeight.Medium
                )
            }


            // =====================================================
            // MEDICINE LIST
            // =====================================================

            if (filteredMedicines.isEmpty()) {

                item {

                    Card(

                        modifier = Modifier.fillMaxWidth(),

                        shape =
                            RoundedCornerShape(16.dp),

                        colors =
                            CardDefaults.cardColors(
                                containerColor = CardColor
                            )
                    ) {

                        Column(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),

                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = "🔎",
                                fontSize = 38.sp
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Text(
                                text = "No information found",
                                color = PrimaryTextColor,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Text(
                                text = "Try another medicine or category.",
                                color = SecondaryTextColor,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

            } else {

                items(filteredMedicines) { medicine ->

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

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Card(

                    modifier = Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(16.dp),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color(0xFF10283D)
                        )
                ) {

                    Text(
                        text =
                            "⚠ This information is for educational purposes only and does not replace professional medical advice.",

                        color =
                            SecondaryTextColor,

                        fontSize = 12.sp,

                        modifier =
                            Modifier.padding(16.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )
            }
        }
    }
}


// ============================================================
// MEDICINE CARD
// ============================================================

@Composable
private fun MedicineCard(
    medicine: Medicine,
    onClick: () -> Unit
) {

    val categoryIcon = when (medicine.category) {

        "Pain & Fever" -> "💊"

        "Allergy" -> "🤧"

        "Acid & Stomach" -> "🫃"

        "Cold & Nasal" -> "🌡️"

        "First Aid" -> "🩹"

        "Hydration" -> "🥤"

        "Health & Wellness" -> "❤️"

        else -> "⚕️"
    }


    Card(

        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },

        shape =
            RoundedCornerShape(16.dp),

        colors =
            CardDefaults.cardColors(
                containerColor = CardColor
            )
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = categoryIcon,
                fontSize = 30.sp
            )

            Spacer(
                modifier = Modifier.width(14.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = medicine.name,
                    color = PrimaryTextColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = medicine.category,
                    color = LightAccentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Text(
                    text = medicine.uses
                        .take(2)
                        .joinToString(" • "),

                    color = SecondaryTextColor,
                    fontSize = 12.sp
                )
            }
        }
    }
}