package ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp



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
// MEDICINE / HEALTH INFORMATION DATABASE
// Educational information only
// ============================================================

private val medicines = listOf(

    // --------------------------------------------------------
    // PAIN / FEVER
    // --------------------------------------------------------

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
        warning = "Use according to the product label or advice from a qualified healthcare professional."
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
        warning = "Not suitable for everyone. Follow the product label or professional medical advice."
    ),

    Medicine(
        name = "Topical Pain Relief Gel",
        category = "Pain Relief",
        uses = listOf(
            "Temporary relief of minor muscle pain",
            "Minor joint discomfort",
            "Localized aches"
        ),
        precautions = listOf(
            "For external use only.",
            "Avoid contact with eyes and broken skin.",
            "Wash hands after application unless hands are the treatment area."
        ),
        commonSideEffects = listOf(
            "Skin irritation",
            "Redness",
            "Mild burning sensation"
        ),
        warning = "Check the specific product ingredients and instructions before use."
    ),


    // --------------------------------------------------------
    // ALLERGY
    // --------------------------------------------------------

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
        warning = "Use according to the product label or professional advice."
    ),


    // --------------------------------------------------------
    // ACID / STOMACH
    // --------------------------------------------------------

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
        warning = "Persistent or severe symptoms should be evaluated by a healthcare professional."
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


    // --------------------------------------------------------
    // COLD / NASAL
    // --------------------------------------------------------

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
        warning = "This is general educational information. Follow the specific product instructions."
    ),

    Medicine(
        name = "Cough & Cold Information",
        category = "Cold & Cough",
        uses = listOf(
            "Information about common cold symptoms",
            "Cough symptom management",
            "General cold-care guidance"
        ),
        precautions = listOf(
            "Different cough and cold products contain different ingredients.",
            "Check labels carefully before combining products.",
            "Children may require age-specific medical advice."
        ),
        commonSideEffects = listOf(
            "Depends on the active ingredients in the specific product."
        ),
        warning = "Do not treat this entry as a recommendation for a specific cough or cold medicine."
    ),


    // --------------------------------------------------------
    // FIRST AID
    // --------------------------------------------------------

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
        name = "Wound Care",
        category = "First Aid",
        uses = listOf(
            "Basic care of minor cuts",
            "Basic care of minor abrasions",
            "Keeping a minor wound clean and protected"
        ),
        precautions = listOf(
            "Wash hands before handling a wound.",
            "Use clean materials.",
            "Monitor the wound for signs of infection."
        ),
        commonSideEffects = listOf(
            "Minor irritation depending on the dressing or product used."
        ),
        warning = "Seek professional medical care for serious injuries or signs of infection."
    ),


    // --------------------------------------------------------
    // HYDRATION / WELLNESS
    // --------------------------------------------------------

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
        warning = "Follow the preparation instructions carefully. Severe dehydration requires medical attention."
    ),

    Medicine(
        name = "Electrolyte Drink",
        category = "Health & Wellness",
        uses = listOf(
            "Provides fluids and electrolytes",
            "May support hydration during certain activities",
            "Useful when fluid and electrolyte replacement is needed"
        ),
        precautions = listOf(
            "Check the sugar and caffeine content.",
            "Not every sports or energy drink is appropriate for hydration during illness.",
            "People with certain medical conditions should seek professional advice."
        ),
        commonSideEffects = listOf(
            "Stomach discomfort may occur with some products."
        ),
        warning = "Check the product label. Electrolyte drinks are not a substitute for medical treatment."
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

@OptIn(ExperimentalMaterial3Api::class)
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


    // ========================================================
    // CATEGORY LIST
    // ========================================================

    val categories = listOf(
        "All",
        "Pain & Fever",
        "Pain Relief",
        "Allergy",
        "Acid & Stomach",
        "Cold & Nasal",
        "Cold & Cough",
        "First Aid",
        "Hydration",
        "Health & Wellness"
    )


    // ========================================================
    // SEARCH + CATEGORY FILTER
    // ========================================================

    val filteredMedicines = medicines.filter { medicine ->

        val query = searchQuery.trim()

        val matchesSearch =
            query.isEmpty() ||
                    medicine.name.contains(
                        query,
                        ignoreCase = true
                    ) ||
                    medicine.category.contains(
                        query,
                        ignoreCase = true
                    ) ||
                    medicine.uses.any {
                        it.contains(
                            query,
                            ignoreCase = true
                        )
                    }

        val matchesCategory =
            selectedCategory == "All" ||
                    medicine.category.equals(
                        selectedCategory,
                        ignoreCase = true
                    )

        matchesSearch && matchesCategory
    }


    // ========================================================
    // SCREEN
    // ========================================================

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text(
                        text = "Medicine & Health Information",
                        fontWeight = FontWeight.Bold
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBackClick
                    ) {

                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }

    ) { paddingValues ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {

            Spacer(
                modifier = Modifier.height(8.dp)
            )


            // =================================================
            // SEARCH BAR
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
                        contentDescription = "Search"
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
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },

                placeholder = {
                    Text(
                        "Search medicine, category, or use..."
                    )
                }
            )


            Spacer(
                modifier = Modifier.height(16.dp)
            )


            // =================================================
            // CATEGORY TITLE
            // =================================================

            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )


            Spacer(
                modifier = Modifier.height(8.dp)
            )


            // =================================================
            // CATEGORY CHIPS
            // =================================================

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

                    FilterChip(

                        selected =
                            selectedCategory == category,

                        onClick = {
                            selectedCategory = category
                        },

                        label = {
                            Text(category)
                        }
                    )
                }
            }


            Spacer(
                modifier = Modifier.height(16.dp)
            )


            // =================================================
            // RESULT COUNT
            // =================================================

            Text(

                text = when (filteredMedicines.size) {

                    0 -> "No information found"

                    1 -> "1 result found"

                    else ->
                        "${filteredMedicines.size} results found"
                },

                style =
                    MaterialTheme.typography.bodyMedium,

                fontWeight = FontWeight.Medium
            )


            Spacer(
                modifier = Modifier.height(8.dp)
            )


            // =================================================
            // INFORMATION LIST
            // =================================================

            if (filteredMedicines.isEmpty()) {

                Column(

                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Search,

                        contentDescription = null,

                        modifier =
                            Modifier.size(48.dp),

                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )


                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )


                    Text(
                        text = "No information found",

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,

                        fontWeight =
                            FontWeight.Bold
                    )


                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )


                    Text(
                        text =
                            "Try another medicine, symptom, or category.",

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )

                }

            } else {

                LazyColumn(

                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    items(filteredMedicines) { medicine ->

                        MedicineCard(

                            medicine = medicine,

                            onClick = {
                                onMedicineClick(
                                    medicine
                                )
                            }
                        )
                    }
                }
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

        "Pain & Fever",
        "Pain Relief" -> "💊"

        "Allergy" -> "🤧"

        "Acid & Stomach" -> "🫃"

        "Cold & Nasal",
        "Cold & Cough" -> "🌡️"

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

        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {

        Row(

            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),

            verticalAlignment = Alignment.CenterVertically
        ) {

            // Category icon
            Text(
                text = categoryIcon,
                fontSize = 32.sp
            )

            Spacer(
                modifier = Modifier.width(14.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = medicine.name,

                    style = MaterialTheme
                        .typography
                        .titleMedium,

                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = medicine.category,

                    style = MaterialTheme
                        .typography
                        .bodyMedium
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = medicine.uses
                        .take(2)
                        .joinToString(" • "),

                    style = MaterialTheme
                        .typography
                        .bodySmall
                )
            }
        }
    }
}