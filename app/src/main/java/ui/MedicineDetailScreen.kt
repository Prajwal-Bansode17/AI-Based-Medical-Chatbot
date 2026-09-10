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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MedicineDetailScreen(
    medicine: Medicine,
    onBackClick: () -> Unit
) {

    val primary = Color(0xFF087EA4)
    val darkBlue = Color(0xFF123A56)
    val gray = Color(0xFF71818C)

    val backgroundTop = Color(0xFFEAF8FC)
    val backgroundBottom = Color(0xFFD8F0F6)

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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    PaddingValues(
                        start = 20.dp,
                        top = 18.dp,
                        end = 20.dp,
                        bottom = 28.dp
                    )
                ),

            verticalArrangement =
                Arrangement.spacedBy(13.dp)
        ) {

            // HEADER

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
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

                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "‹",
                        color = darkBlue,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Light
                    )
                }

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Medicine Details",
                        color = darkBlue,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = "Complete medicine information",
                        color = gray,
                        fontSize = 12.sp
                    )
                }
            }

            // MEDICINE NAME

            Card(
                modifier = Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(27.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor = primary
                    ),

                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 8.dp
                    )
            ) {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                ) {

                    Text(
                        text = "MEDICINE NAME",
                        color =
                            Color.White.copy(
                                alpha = 0.75f
                            ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text = medicine.name,
                        color = Color.White,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // GENERIC NAME

            MedicineTextSection(
                title = "Generic Name",
                content =
                    medicine.genericName.ifBlank {
                        "Generic name information is not available in this record."
                    },
                primary = primary,
                darkBlue = darkBlue,
                gray = gray
            )

            // USES

            MedicineListSection(
                title = "Uses",
                items = medicine.uses,
                primary = primary,
                darkBlue = darkBlue,
                gray = gray
            )

            // SIDE EFFECTS

            MedicineListSection(
                title = "Side Effects",
                items = medicine.commonSideEffects,
                primary = primary,
                darkBlue = darkBlue,
                gray = gray
            )

            // WARNINGS

            MedicineTextSection(
                title = "Warnings",
                content =
                    medicine.warning.ifBlank {
                        "Warning information is not available in this record."
                    },
                primary = primary,
                darkBlue = darkBlue,
                gray = gray
            )

            // PRECAUTIONS

            MedicineListSection(
                title = "Precautions",
                items = medicine.precautions,
                primary = primary,
                darkBlue = darkBlue,
                gray = gray
            )

            // DISCLAIMER

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
                        "⚠ This medicine information is for educational purposes only. Do not start, stop, or change medication without appropriate professional advice.",

                    modifier =
                        Modifier.padding(16.dp),

                    color = gray,
                    fontSize = 11.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}


// ================================================================
// TEXT INFORMATION SECTION
// ================================================================

@Composable
private fun MedicineTextSection(
    title: String,
    content: String,
    primary: Color,
    darkBlue: Color,
    gray: Color
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(21.dp),

        colors =
            CardDefaults.cardColors(
                containerColor = Color.White
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(18.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(10.dp)
                            .background(
                                color = primary,
                                shape = CircleShape
                            )
                )

                Spacer(
                    modifier =
                        Modifier.width(9.dp)
                )

                Text(
                    text = title,
                    color = darkBlue,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(13.dp)
            )

            Text(
                text = content,
                color = gray,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}


// ================================================================
// LIST INFORMATION SECTION
// ================================================================

@Composable
private fun MedicineListSection(
    title: String,
    items: List<String>,
    primary: Color,
    darkBlue: Color,
    gray: Color
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(21.dp),

        colors =
            CardDefaults.cardColors(
                containerColor = Color.White
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(18.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(10.dp)
                            .background(
                                color = primary,
                                shape = CircleShape
                            )
                )

                Spacer(
                    modifier =
                        Modifier.width(9.dp)
                )

                Text(
                    text = title,
                    color = darkBlue,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(13.dp)
            )

            items.forEach { item ->

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                bottom = 9.dp
                            ),

                    verticalAlignment =
                        Alignment.Top
                ) {

                    Text(
                        text = "•",
                        color = primary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.width(9.dp)
                    )

                    Text(
                        text = item,
                        color = gray,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier =
                            Modifier.weight(1f)
                    )
                }
            }
        }
    }
}