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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SymptomsCheckerScreen(
    onBackClick: () -> Unit = {}
) {

    /*
     * ============================================================
     * SYMPTOMS
     * ============================================================
     */

    val symptoms = listOf(
        "Fever",
        "Headache",
        "Cough",
        "Fatigue",
        "Nausea",
        "Body Pain"
    )

    /*
     * ============================================================
     * SELECTED SYMPTOMS
     * ============================================================
     */

    val selectedSymptoms =
        remember {
            mutableStateListOf<String>()
        }

    var showResult by remember {
        mutableStateOf(false)
    }

    /*
     * ============================================================
     * APP COLORS
     * ============================================================
     */

    val primary = Color(0xFF087EA4)
    val darkBlue = Color(0xFF123A56)
    val gray = Color(0xFF71818C)

    val backgroundTop = Color(0xFFEAF8FC)
    val backgroundBottom = Color(0xFFD8F0F6)

    /*
     * ============================================================
     * MAIN SCREEN
     * ============================================================
     */

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
            modifier = Modifier.fillMaxSize(),

            contentPadding = PaddingValues(
                start = 20.dp,
                top = 18.dp,
                end = 20.dp,
                bottom = 25.dp
            ),

            verticalArrangement = Arrangement.spacedBy(
                12.dp
            )
        ) {

            /*
             * ====================================================
             * HEADER
             * ====================================================
             */

            item {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    /*
                     * BACK BUTTON
                     *
                     * Text arrow is used instead of Material icon
                     * to avoid icon dependency errors.
                     */

                    IconButton(
                        onClick = onBackClick
                    ) {

                        Text(
                            text = "‹",
                            color = darkBlue,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Light
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(5.dp)
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

            /*
             * ====================================================
             * INTRO CARD
             * ====================================================
             */

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(
                        25.dp
                    ),

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

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        /*
                         * SIMPLE HEALTH LOGO
                         */

                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .background(
                                    color = Color.White.copy(
                                        alpha = 0.18f
                                    ),
                                    shape = RoundedCornerShape(
                                        17.dp
                                    )
                                ),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Text(
                                text = "+",
                                color = Color.White,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Light
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
                                text = "Select all symptoms you currently have.",
                                color = Color.White.copy(
                                    alpha = 0.82f
                                ),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            /*
             * ====================================================
             * SECTION TITLE
             * ====================================================
             */

            item {

                Text(
                    text = "Select Symptoms",
                    color = darkBlue,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        top = 4.dp
                    )
                )
            }

            /*
             * ====================================================
             * SYMPTOM CARDS
             * ====================================================
             */

            items(
                items = symptoms
            ) { symptom ->

                val selected =
                    selectedSymptoms.contains(
                        symptom
                    )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {

                            if (selected) {

                                selectedSymptoms.remove(
                                    symptom
                                )

                            } else {

                                selectedSymptoms.add(
                                    symptom
                                )
                            }

                            showResult = false
                        },

                    shape = RoundedCornerShape(
                        19.dp
                    ),

                    colors = CardDefaults.cardColors(
                        containerColor =
                            if (selected) {
                                primary
                            } else {
                                Color.White
                            }
                    ),

                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    )
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(17.dp),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        /*
                         * SYMPTOM INDICATOR
                         */

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color =
                                        if (selected) {
                                            Color.White.copy(
                                                alpha = 0.18f
                                            )
                                        } else {
                                            Color(0xFFE8F7FB)
                                        },

                                    shape =
                                        RoundedCornerShape(
                                            14.dp
                                        )
                                ),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Text(
                                text =
                                    when (symptom) {
                                        "Fever" -> "°"
                                        "Headache" -> "H"
                                        "Cough" -> "C"
                                        "Fatigue" -> "F"
                                        "Nausea" -> "N"
                                        "Body Pain" -> "P"
                                        else -> "+"
                                    },

                                color =
                                    if (selected) {
                                        Color.White
                                    } else {
                                        primary
                                    },

                                fontSize = 18.sp,

                                fontWeight =
                                    FontWeight.Bold
                            )
                        }

                        Spacer(
                            modifier = Modifier.width(13.dp)
                        )

                        Text(
                            text = symptom,

                            color =
                                if (selected) {
                                    Color.White
                                } else {
                                    darkBlue
                                },

                            fontSize = 15.sp,

                            fontWeight =
                                FontWeight.Medium,

                            modifier =
                                Modifier.weight(1f)
                        )

                        /*
                         * CHECK INDICATOR
                         */

                        if (selected) {

                            Box(
                                modifier = Modifier
                                    .size(27.dp)
                                    .background(
                                        Color.White,
                                        RoundedCornerShape(
                                            50
                                        )
                                    ),

                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Text(
                                    text = "✓",
                                    color = primary,
                                    fontSize = 17.sp,
                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            /*
             * ====================================================
             * CHECK BUTTON
             * ====================================================
             */

            item {

                Button(
                    onClick = {
                        showResult = true
                    },

                    enabled =
                        selectedSymptoms.isNotEmpty(),

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),

                    shape =
                        RoundedCornerShape(18.dp),

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = primary,
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

            /*
             * ====================================================
             * RESULT CARD
             * ====================================================
             */

            if (showResult) {

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
                                defaultElevation = 6.dp
                            )
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(20.dp)
                        ) {

                            Text(
                                text =
                                    "Selected Symptoms",

                                color =
                                    primary,

                                fontSize =
                                    18.sp,

                                fontWeight =
                                    FontWeight.Bold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(10.dp)
                            )

                            Text(
                                text =
                                    selectedSymptoms
                                        .joinToString(
                                            separator = ", "
                                        ),

                                color =
                                    darkBlue,

                                fontSize =
                                    14.sp
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(12.dp)
                            )

                            Text(
                                text =
                                    "AI-based symptom prediction will be available after the medical model is integrated.",

                                color =
                                    gray,

                                fontSize =
                                    12.sp,

                                lineHeight =
                                    18.sp
                            )
                        }
                    }
                }
            }

            /*
             * ====================================================
             * MEDICAL DISCLAIMER
             * ====================================================
             */

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
                            "⚠ This tool is not a medical diagnosis. Please consult a qualified healthcare professional for medical advice.",

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