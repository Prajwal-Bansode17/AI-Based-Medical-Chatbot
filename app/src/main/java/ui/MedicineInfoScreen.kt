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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

@Composable
fun MedicineInfoScreen(
    onBackClick: () -> Unit,
    onMedicineClick: (Medicine) -> Unit
) {

    var searchQuery by remember {
        mutableStateOf("")
    }

    val filteredMedicines = medicines.filter { medicine ->

        medicine.name.contains(
            searchQuery,
            ignoreCase = true
        ) ||

                medicine.category.contains(
                    searchQuery,
                    ignoreCase = true
                )
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
            modifier = Modifier.fillMaxSize(),

            contentPadding = PaddingValues(
                start = 20.dp,
                top = 18.dp,
                end = 20.dp,
                bottom = 25.dp
            ),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            // HEADER

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
                        modifier = Modifier.width(12.dp)
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
                                "Explore common medicine information",

                            color =
                                gray,

                            fontSize =
                                12.sp
                        )
                    }
                }
            }

            // SEARCH CARD

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

                        onValueChange = {
                            searchQuery = it
                        },

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(7.dp),

                        singleLine = true,

                        placeholder = {
                            Text(
                                text =
                                    "Search medicines..."
                            )
                        },

                        leadingIcon = {

                            Text(
                                text = "⌕",
                                color =
                                    primary,
                                fontSize = 27.sp
                            )
                        },

                        shape =
                            RoundedCornerShape(17.dp),

                        colors =
                            OutlinedTextFieldDefaults.colors(

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

            // INTRO CARD

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
                                        RoundedCornerShape(
                                            17.dp
                                        )
                                    ),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Text(
                                text = "Rx",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.width(14.dp)
                        )

                        Column {

                            Text(
                                text =
                                    "Common Medicines",

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
                                    "Tap a medicine to view detailed information.",

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

            // SECTION TITLE

            item {

                Text(
                    text =
                        if (searchQuery.isBlank())
                            "Available Medicines"
                        else
                            "Search Results",

                    color =
                        darkBlue,

                    fontSize =
                        19.sp,

                    fontWeight =
                        FontWeight.Bold,

                    modifier =
                        Modifier.padding(
                            top = 3.dp
                        )
                )
            }

            // MEDICINES

            if (filteredMedicines.isEmpty()) {

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
                                    "Try another medicine name.",

                                color =
                                    gray,

                                fontSize =
                                    12.sp
                            )
                        }
                    }
                }

            } else {

                items(
                    filteredMedicines
                ) { medicine ->

                    MedicineCard(
                        medicine =
                            medicine,

                        onClick = {
                            onMedicineClick(
                                medicine
                            )
                        }
                    )
                }
            }

            // DISCLAIMER

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
                    text = "Rx",
                    color =
                        primary,
                    fontSize =
                        17.sp,
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
                        FontWeight.Bold
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
                        12.sp
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