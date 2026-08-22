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

    // ============================================================
    // MEDASSIST AI THEME COLORS
    // ============================================================

    val primary = Color(0xFF087EA4)
    val darkBlue = Color(0xFF123A56)
    val gray = Color(0xFF71818C)

    val backgroundTop = Color(0xFFEAF8FC)
    val backgroundBottom = Color(0xFFD8F0F6)

    // ============================================================
    // MAIN SCREEN
    // ============================================================

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

            // ====================================================
            // HEADER
            // ====================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                // BACK BUTTON

                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .background(
                            color = Color.White,
                            shape = CircleShape
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
                        text = "Educational medicine information",
                        color = gray,
                        fontSize = 12.sp
                    )
                }

                // MEDICINE ICON

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = Color.White,
                            shape = CircleShape
                        ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text = "Rx",
                        color = primary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ====================================================
            // MEDICINE HERO CARD
            // ====================================================

            Card(
                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(27.dp),

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
                        .padding(22.dp),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    // LARGE MEDICINE ICON

                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .background(
                                color =
                                    Color.White.copy(
                                        alpha = 0.18f
                                    ),
                                shape =
                                    RoundedCornerShape(20.dp)
                            ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text = "Rx",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(15.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = medicine.name,
                            color = Color.White,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(5.dp)
                        )

                        Text(
                            text = medicine.category,
                            color =
                                Color.White.copy(
                                    alpha = 0.85f
                                ),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // ====================================================
            // GENERAL USES
            // ====================================================

            MedicineInformationSection(
                title = "General Uses",
                items = medicine.uses,
                primary = primary,
                darkBlue = darkBlue,
                gray = gray
            )

            // ====================================================
            // PRECAUTIONS
            // ====================================================

            MedicineInformationSection(
                title = "Precautions",
                items = medicine.precautions,
                primary = primary,
                darkBlue = darkBlue,
                gray = gray
            )

            // ====================================================
            // COMMON SIDE EFFECTS
            // ====================================================

            MedicineInformationSection(
                title = "Common Side Effects",
                items = medicine.commonSideEffects,
                primary = primary,
                darkBlue = darkBlue,
                gray = gray
            )

            // ====================================================
            // IMPORTANT INFORMATION
            // ====================================================

            Card(
                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(21.dp),

                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),

                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    color = Color(0xFFFFF4E5),
                                    shape =
                                        RoundedCornerShape(13.dp)
                                ),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Text(
                                text = "!",
                                color = Color(0xFFE39A2B),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(
                            modifier = Modifier.width(12.dp)
                        )

                        Text(
                            text = "Important Information",
                            color = darkBlue,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = medicine.warning,
                        color = gray,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // ====================================================
            // MEDICAL DISCLAIMER
            // ====================================================

            Card(
                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(18.dp),

                colors = CardDefaults.cardColors(
                    containerColor =
                        Color.White.copy(
                            alpha = 0.88f
                        )
                )
            ) {

                Text(
                    text =
                        "⚠ This medicine information is for educational purposes only. Do not start, stop, or change medication without appropriate professional advice.",

                    modifier = Modifier.padding(16.dp),

                    color = gray,

                    fontSize = 11.sp,

                    lineHeight = 17.sp
                )
            }
        }
    }
}


// =================================================================
// INFORMATION SECTION
// =================================================================

@Composable
private fun MedicineInformationSection(
    title: String,
    items: List<String>,
    primary: Color,
    darkBlue: Color,
    gray: Color
) {

    Card(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(21.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            // SECTION HEADER

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = primary,
                            shape = CircleShape
                        )
                )

                Spacer(
                    modifier = Modifier.width(9.dp)
                )

                Text(
                    text = title,
                    color = darkBlue,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(13.dp)
            )

            // ITEMS

            items.forEach { item ->

                Row(
                    modifier = Modifier
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
                        modifier = Modifier.width(9.dp)
                    )

                    Text(
                        text = item,
                        color = gray,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}