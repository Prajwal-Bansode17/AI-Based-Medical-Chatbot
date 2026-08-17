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


data class Medicine(
    val name: String,
    val category: String,
    val uses: List<String>,
    val precautions: List<String>,
    val commonSideEffects: List<String>,
    val warning: String
)

private val medicines = listOf(

    Medicine(
        name = "Paracetamol",
        category = "Pain reliever / Fever reducer",
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
        category = "Pain reliever / Anti-inflammatory",
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
        category = "Antihistamine",
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
        category = "Acid-reducing medicine",
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
        name = "ORS",
        category = "Oral Rehydration Solution",
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
    )
)

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

    val categories = listOf(
        "All",
        "Pain",
        "Allergy",
        "Acid",
        "Hydration"
    )

    /*
     * Search works across:
     * 1. Medicine name
     * 2. Category
     * 3. Uses / symptoms
     */
    val filteredMedicines = medicines.filter { medicine ->

        val query = searchQuery.trim()

        val matchesSearch = query.isEmpty() ||
                medicine.name.contains(query, ignoreCase = true) ||
                medicine.category.contains(query, ignoreCase = true) ||
                medicine.uses.any {
                    it.contains(query, ignoreCase = true)
                }

        val matchesCategory = when (selectedCategory) {

            "All" -> true

            "Pain" ->
                medicine.category.contains(
                    "Pain",
                    ignoreCase = true
                )

            "Allergy" ->
                medicine.category.contains(
                    "Antihistamine",
                    ignoreCase = true
                )

            "Acid" ->
                medicine.category.contains(
                    "Acid",
                    ignoreCase = true
                )

            "Hydration" ->
                medicine.category.contains(
                    "Rehydration",
                    ignoreCase = true
                )

            else -> true
        }

        matchesSearch && matchesCategory
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text(
                        text = "Medicine Information",
                        fontWeight = FontWeight.Bold
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBackClick
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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

            // Search bar
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
                    Text("Search medicine or symptom...")
                }
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            // Categories
            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(
                        rememberScrollState()
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                categories.forEach { category ->

                    FilterChip(

                        selected = selectedCategory == category,

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

            // Result count
            Text(
                text = when (filteredMedicines.size) {
                    0 -> "No medicines found"
                    1 -> "1 medicine found"
                    else -> "${filteredMedicines.size} medicines found"
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            if (filteredMedicines.isEmpty()) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = "No medicines found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = "Try another medicine, symptom, or category.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

            } else {

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    items(filteredMedicines) { medicine ->

                        MedicineCard(
                            medicine = medicine,
                            onClick = {
                                onMedicineClick(medicine)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicineCard(
    medicine: Medicine,
    onClick: () -> Unit
) {

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

            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Medicine",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(
                modifier = Modifier.width(14.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = medicine.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = medicine.category,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = medicine.uses.take(2).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}