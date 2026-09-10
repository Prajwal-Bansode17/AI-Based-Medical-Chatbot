package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileScreen(
    userName: String,
    userEmail: String,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit
) {

    // =========================================================
    // MEDASSIST COLORS
    // =========================================================

    val backgroundTop = Color(0xFFF7FCFF)
    val backgroundBottom = Color(0xFFE5F5FA)

    val primaryBlue = Color(0xFF1976D2)
    val primaryDark = Color(0xFF123A56)
    val teal = Color(0xFF009688)

    val textPrimary = Color(0xFF172B4D)
    val textSecondary = Color(0xFF71818C)

    val surface = Color.White
    val surfaceVariant = Color(0xFFEAF6FA)

    val logoutBackground = Color(0xFFFFF3F2)
    val logoutBorder = Color(0xFFFFD9D5)
    val logoutText = Color(0xFFD32F2F)

    // =========================================================
    // PASSWORD UI STATE
    // =========================================================

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    // =========================================================
    // BMI DATA
    // =========================================================

    val context = androidx.compose.ui.platform.LocalContext.current

    var bmiData by remember {
        mutableStateOf(
            BMIRepository.getBMIData(context)
        )
    }

    // =========================================================
    // USER VALUES
    // =========================================================

    val displayName =
        userName.trim().ifBlank {
            "MedAssist User"
        }

    val displayEmail =
        userEmail.trim().ifBlank {
            "Email not available"
        }

    // =========================================================
    // BMI DISPLAY VALUES
    // =========================================================

    val bmiValue =
        if (bmiData.bmi > 0f) {
            String.format(
                java.util.Locale.getDefault(),
                "%.1f",
                bmiData.bmi
            )
        } else {
            "Not Available"
        }

    val ageValue =
        if (bmiData.age > 0) {
            "${bmiData.age} years"
        } else {
            "Not Available"
        }

    val heightValue =
        if (bmiData.heightCm > 0f) {
            String.format(
                java.util.Locale.getDefault(),
                "%.0f cm",
                bmiData.heightCm
            )
        } else {
            "Not Available"
        }

    val weightValue =
        if (bmiData.weightKg > 0f) {
            String.format(
                java.util.Locale.getDefault(),
                "%.1f kg",
                bmiData.weightKg
            )
        } else {
            "Not Available"
        }

    val dobValue =
        bmiData.dateOfBirth.ifBlank {
            "Not Available"
        }

    val genderValue =
        bmiData.gender.ifBlank {
            "Not Available"
        }

    val bmiStatusValue = bmiData.bmiStatus

    // =========================================================
    // ROOT
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

        // =====================================================
        // TOP RIGHT DECORATION
        // =====================================================

        Box(
            modifier = Modifier
                .size(210.dp)
                .align(Alignment.TopEnd)
                .background(
                    primaryBlue.copy(alpha = 0.06f),
                    CircleShape
                )
        )

        // =====================================================
        // BOTTOM LEFT DECORATION
        // =====================================================

        Box(
            modifier = Modifier
                .size(155.dp)
                .align(Alignment.BottomStart)
                .background(
                    teal.copy(alpha = 0.055f),
                    CircleShape
                )
        )

        // =====================================================
        // MAIN CONTENT
        // =====================================================

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 20.dp,
                    vertical = 12.dp
                )
        ) {

            // =================================================
            // HEADER
            // =================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                androidx.compose.material3.IconButton(
                    onClick = onBackClick
                ) {

                    androidx.compose.material3.Icon(
                        imageVector =
                            Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription =
                            "Back",
                        tint =
                            primaryDark,
                        modifier =
                            Modifier.size(25.dp)
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(5.dp)
                )

                androidx.compose.material3.Text(
                    text = "My Profile",
                    color = primaryDark,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(22.dp)
            )

            // =================================================
            // PROFILE HEADER CARD
            // =================================================

            androidx.compose.material3.Card(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(27.dp),
                colors =
                    androidx.compose.material3.CardDefaults.cardColors(
                        containerColor =
                            surface
                    ),
                elevation =
                    androidx.compose.material3.CardDefaults.cardElevation(
                        defaultElevation =
                            5.dp
                    )
            ) {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    // -----------------------------------------
                    // AVATAR
                    // -----------------------------------------

                    Box(
                        modifier =
                            Modifier
                                .size(92.dp)
                                .shadow(
                                    elevation = 9.dp,
                                    shape = CircleShape
                                )
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            primaryBlue,
                                            teal
                                        )
                                    ),
                                    CircleShape
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        androidx.compose.material3.Icon(
                            imageVector =
                                Icons.Default.Person,
                            contentDescription =
                                "Profile",
                            tint =
                                Color.White,
                            modifier =
                                Modifier.size(48.dp)
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(13.dp)
                    )

                    // -----------------------------------------
                    // NAME
                    // -----------------------------------------

                    androidx.compose.material3.Text(
                        text =
                            displayName,
                        color =
                            textPrimary,
                        fontSize =
                            22.sp,
                        fontWeight =
                            FontWeight.Bold,
                        maxLines =
                            1,
                        overflow =
                            TextOverflow.Ellipsis
                    )

                    Spacer(
                        modifier =
                            Modifier.height(5.dp)
                    )

                    // -----------------------------------------
                    // EMAIL
                    // -----------------------------------------

                    androidx.compose.material3.Text(
                        text =
                            displayEmail,
                        color =
                            textSecondary,
                        fontSize =
                            12.sp,
                        maxLines =
                            1,
                        overflow =
                            TextOverflow.Ellipsis
                    )

                    Spacer(
                        modifier =
                            Modifier.height(13.dp)
                    )

                    // -----------------------------------------
                    // SECURE BADGE
                    // -----------------------------------------

                    Row(
                        modifier =
                            Modifier
                                .background(
                                    surfaceVariant,
                                    RoundedCornerShape(50.dp)
                                )
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 7.dp
                                ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        androidx.compose.material3.Icon(
                            imageVector =
                                Icons.Default.Lock,
                            contentDescription =
                                "Secure",
                            tint =
                                primaryBlue,
                            modifier =
                                Modifier.size(14.dp)
                        )

                        Spacer(
                            modifier =
                                Modifier.width(6.dp)
                        )

                        androidx.compose.material3.Text(
                            text =
                                "Secure account",
                            color =
                                primaryBlue,
                            fontSize =
                                10.sp,
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(25.dp)
            )

            // =================================================
            // ACCOUNT SECTION
            // =================================================

            androidx.compose.material3.Text(
                text =
                    "Account",
                color =
                    textPrimary,
                fontSize =
                    19.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(11.dp)
            )

            androidx.compose.material3.Card(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(22.dp),
                colors =
                    androidx.compose.material3.CardDefaults.cardColors(
                        containerColor =
                            surface
                    ),
                elevation =
                    androidx.compose.material3.CardDefaults.cardElevation(
                        defaultElevation =
                            3.dp
                    )
            ) {

                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    // -----------------------------------------
                    // PROFILE
                    // -----------------------------------------

                    ProfileInfoRow(
                        icon =
                            Icons.Default.Person,
                        title =
                            "Profile",
                        subtitle =
                            "Your MEDASSIST account",
                        iconColor =
                            primaryBlue
                    )

                    ProfileDivider()

                    // -----------------------------------------
                    // EMAIL
                    // -----------------------------------------

                    ProfileInfoRow(
                        icon =
                            Icons.Default.Email,
                        title =
                            "Email",
                        subtitle =
                            displayEmail,
                        iconColor =
                            teal
                    )

                    ProfileDivider()

                    // -----------------------------------------
                    // PASSWORD
                    // -----------------------------------------

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 16.dp,
                                    end = 8.dp,
                                    top = 16.dp,
                                    bottom = 16.dp
                                ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .size(44.dp)
                                    .background(
                                        surfaceVariant,
                                        RoundedCornerShape(13.dp)
                                    ),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            androidx.compose.material3.Icon(
                                imageVector =
                                    Icons.Default.Lock,
                                contentDescription =
                                    "Password",
                                tint =
                                    primaryBlue,
                                modifier =
                                    Modifier.size(21.dp)
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

                            androidx.compose.material3.Text(
                                text =
                                    "Password",
                                color =
                                    textPrimary,
                                fontSize =
                                    13.sp,
                                fontWeight =
                                    FontWeight.SemiBold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(3.dp)
                            )

                            androidx.compose.material3.Text(
                                text =
                                    if (passwordVisible) {
                                        "Password protected"
                                    } else {
                                        "••••••••"
                                    },
                                color =
                                    textSecondary,
                                fontSize =
                                    13.sp,
                                fontWeight =
                                    if (passwordVisible) {
                                        FontWeight.Medium
                                    } else {
                                        FontWeight.Normal
                                    }
                            )
                        }

                        androidx.compose.material3.IconButton(
                            onClick = {
                                passwordVisible =
                                    !passwordVisible
                            }
                        ) {

                            androidx.compose.material3.Text(
                                text =
                                    if (passwordVisible) {
                                        "◉"
                                    } else {
                                        "◌"
                                    },
                                color =
                                    primaryBlue,
                                fontSize =
                                    22.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )

            // =================================================
            // SAVED HEALTH & BMI SECTION
            // =================================================

            androidx.compose.material3.Text(
                text =
                    "My Health Information",
                color =
                    textPrimary,
                fontSize =
                    19.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(11.dp)
            )

            // =================================================
            // BMI SUMMARY CARD
            // =================================================

            androidx.compose.material3.Card(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(22.dp),
                colors =
                    androidx.compose.material3.CardDefaults.cardColors(
                        containerColor =
                            surface
                    ),
                elevation =
                    androidx.compose.material3.CardDefaults.cardElevation(
                        defaultElevation =
                            3.dp
                    )
            ) {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                ) {

                    // -----------------------------------------
                    // BMI HEADER
                    // -----------------------------------------

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .size(50.dp)
                                    .background(
                                        surfaceVariant,
                                        RoundedCornerShape(16.dp)
                                    ),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            androidx.compose.material3.Icon(
                                imageVector =
                                    Icons.Default.Info,
                                contentDescription =
                                    "BMI",
                                tint =
                                    primaryBlue,
                                modifier =
                                    Modifier.size(27.dp)
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.width(13.dp)
                        )

                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            androidx.compose.material3.Text(
                                text =
                                    "BMI Information",
                                color =
                                    textPrimary,
                                fontSize =
                                    15.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(3.dp)
                            )

                            androidx.compose.material3.Text(
                                text =
                                    "Your saved body measurements",
                                color =
                                    textSecondary,
                                fontSize =
                                    11.sp
                            )
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(18.dp)
                    )

                    // -----------------------------------------
                    // BMI VALUE
                    // -----------------------------------------

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    surfaceVariant,
                                    RoundedCornerShape(17.dp)
                                )
                                .padding(16.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            androidx.compose.material3.Text(
                                text =
                                    "Current BMI",
                                color =
                                    textSecondary,
                                fontSize =
                                    11.sp
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(3.dp)
                            )

                            androidx.compose.material3.Text(
                                text =
                                    bmiValue,
                                color =
                                    primaryDark,
                                fontSize =
                                    28.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }

                        Column(
                            horizontalAlignment =
                                Alignment.End
                        ) {

                            androidx.compose.material3.Text(
                                text =
                                    "Status",
                                color =
                                    textSecondary,
                                fontSize =
                                    10.sp
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )

                            androidx.compose.material3.Text(
                                text =
                                    bmiStatusValue,
                                color =
                                    primaryBlue,
                                fontSize =
                                    13.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(18.dp)
                    )

                    // -----------------------------------------
                    // PERSONAL DETAILS
                    // -----------------------------------------

                    HealthDataRow(
                        title = "Date of Birth",
                        value = dobValue
                    )

                    HealthDataDivider()

                    HealthDataRow(
                        title = "Age",
                        value = ageValue
                    )

                    HealthDataDivider()

                    HealthDataRow(
                        title = "Gender",
                        value = genderValue
                    )

                    HealthDataDivider()

                    HealthDataRow(
                        title = "Height",
                        value = heightValue
                    )

                    HealthDataDivider()

                    HealthDataRow(
                        title = "Weight",
                        value = weightValue
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(25.dp)
            )

            // =================================================
            // SIGN OUT
            // =================================================

            androidx.compose.material3.Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            onLogoutClick()
                        }
                        .border(
                            width = 1.dp,
                            color = logoutBorder,
                            shape =
                                RoundedCornerShape(20.dp)
                        ),
                shape =
                    RoundedCornerShape(20.dp),
                colors =
                    androidx.compose.material3.CardDefaults.cardColors(
                        containerColor =
                            logoutBackground
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
                                .size(46.dp)
                                .background(
                                    Color(0xFFFFE3E0),
                                    RoundedCornerShape(14.dp)
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        androidx.compose.material3.Text(
                            text =
                                "↪",
                            color =
                                logoutText,
                            fontSize =
                                26.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.width(13.dp)
                    )

                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        androidx.compose.material3.Text(
                            text =
                                "Sign Out",
                            color =
                                logoutText,
                            fontSize =
                                15.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(3.dp)
                        )

                        androidx.compose.material3.Text(
                            text =
                                "Sign out safely from this device",
                            color =
                                textSecondary,
                            fontSize =
                                11.sp
                        )
                    }

                    androidx.compose.material3.Text(
                        text =
                            "›",
                        color =
                            logoutText,
                        fontSize =
                            27.sp
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            // =================================================
            // FOOTER
            // =================================================

            androidx.compose.material3.Text(
                text =
                    "MEDASSIST AI  •  Secure • Simple • Intelligent",
                modifier =
                    Modifier.fillMaxWidth(),
                color =
                    textSecondary,
                fontSize =
                    9.sp,
                textAlign =
                    TextAlign.Center
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )
        }
    }
}

// =============================================================
// PROFILE INFO ROW
// =============================================================

@Composable
private fun ProfileInfoRow(
    icon:
    androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .background(
                        Color(0xFFEAF6FA),
                        RoundedCornerShape(13.dp)
                    ),
            contentAlignment =
                Alignment.Center
        ) {

            androidx.compose.material3.Icon(
                imageVector =
                    icon,
                contentDescription =
                    title,
                tint =
                    iconColor,
                modifier =
                    Modifier.size(21.dp)
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

            androidx.compose.material3.Text(
                text =
                    title,
                color =
                    Color(0xFF172B4D),
                fontSize =
                    13.sp,
                fontWeight =
                    FontWeight.SemiBold
            )

            Spacer(
                modifier =
                    Modifier.height(3.dp)
            )

            androidx.compose.material3.Text(
                text =
                    subtitle,
                color =
                    Color(0xFF71818C),
                fontSize =
                    11.sp,
                maxLines =
                    1,
                overflow =
                    TextOverflow.Ellipsis
            )
        }
    }
}

// =============================================================
// HEALTH DATA ROW
// =============================================================

@Composable
private fun HealthDataRow(
    title: String,
    value: String
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 11.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        androidx.compose.material3.Text(
            text =
                title,
            modifier =
                Modifier.weight(1f),
            color =
                Color(0xFF71818C),
            fontSize =
                12.sp
        )

        androidx.compose.material3.Text(
            text =
                value,
            color =
                Color(0xFF172B4D),
            fontSize =
                13.sp,
            fontWeight =
                FontWeight.SemiBold,
            textAlign =
                TextAlign.End
        )
    }
}

// =============================================================
// HEALTH DATA DIVIDER
// =============================================================

@Composable
private fun HealthDataDivider() {

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Color(0xFFE8EEF2)
                )
    )
}

// =============================================================
// DIVIDER
// =============================================================

@Composable
private fun ProfileDivider() {

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp
                )
                .height(1.dp)
                .background(
                    Color(0xFFE8EEF2)
                )
    )
}