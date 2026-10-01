package ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import org.json.JSONArray
import org.json.JSONObject
import com.example.ai_based_medical_chatbot.MedicineReminderRepository
import com.example.ai_based_medical_chatbot.LocalAppLanguageController
import com.example.ai_based_medical_chatbot.appText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    userName: String,
    userEmail: String,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSetReminderClick: (
        medicineName: String,
        strength: String,
        frequency: String,
        duration: String,
        instructions: String,
        timing: String,
        mealTiming: String,
        howToTake: String
    ) -> Unit = { _, _, _, _, _, _, _, _ -> }
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

    // Android Context used by BMI, prescription, reminder,
    // and language preference storage.
    val context = androidx.compose.ui.platform.LocalContext.current

    // =========================================================
    // APP LANGUAGE
    // =========================================================

    val appLanguage = LocalAppLanguageController.current
    val selectedLanguage = appLanguage.selectedLanguage

    var languageMenuExpanded by remember {
        mutableStateOf(false)
    }

    val supportedLanguages = listOf(
        "English",
        "मराठी",
        "हिंदी"
    )

    fun saveLanguage(language: String) {
        appLanguage.setLanguage(language)
        languageMenuExpanded = false
    }

    // =========================================================
    // BMI DATA
    // =========================================================

    var bmiData by remember {
        mutableStateOf(
            BMIRepository.getBMIData(context)
        )
    }

    // =========================================================
    // SAVED PRESCRIPTIONS
    // =========================================================

    var savedPrescriptions by remember {
        mutableStateOf(
            PrescriptionRepository.getSavedPrescriptions(context).toString()
        )
    }

    var selectedPrescriptionId by remember {
        mutableStateOf<String?>(null)
    }

    var savedReminders by remember {
        mutableStateOf(MedicineReminderRepository.getAll(context))
    }

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    fun refreshPrescriptions() {
        savedPrescriptions =
            PrescriptionRepository.getSavedPrescriptions(context).toString()
    }

    // Refresh saved prescriptions whenever this Profile screen is entered.
    LaunchedEffect(Unit) {
        refreshPrescriptions()
        savedReminders = MedicineReminderRepository.getAll(context)
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
                            appText("back", selectedLanguage),
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
                    text = appText("my_profile", selectedLanguage),
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

                    GenderProfileAvatar(
                        gender = genderValue,
                        primaryBlue = primaryBlue,
                        teal = teal
                    )

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
                                appText("secure_account", selectedLanguage),
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
                    appText("account", selectedLanguage),
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
                            appText("profile", selectedLanguage),
                        subtitle =
                            appText("your_account", selectedLanguage),
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
                            appText("email", selectedLanguage),
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
                                    appText("password", selectedLanguage),
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
                                    appText("password", selectedLanguage),
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
                                        appText("password_protected", selectedLanguage)
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
            // LANGUAGE SECTION
            // =================================================

            androidx.compose.material3.Text(
                text = appText("language", selectedLanguage),
                color = textPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(11.dp)
            )

            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = surface
                ),
                elevation = androidx.compose.material3.CardDefaults.cardElevation(
                    defaultElevation = 3.dp
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                languageMenuExpanded = true
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    surfaceVariant,
                                    RoundedCornerShape(13.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Text(
                                text = "🌐",
                                fontSize = 21.sp
                            )
                        }

                        Spacer(
                            modifier = Modifier.width(12.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            androidx.compose.material3.Text(
                                text = appText("app_language", selectedLanguage),
                                color = textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(
                                modifier = Modifier.height(3.dp)
                            )

                            androidx.compose.material3.Text(
                                text = selectedLanguage,
                                color = textSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        androidx.compose.material3.Text(
                            text = if (languageMenuExpanded) "⌃" else "⌄",
                            color = primaryBlue,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    DropdownMenu(
                        expanded = languageMenuExpanded,
                        onDismissRequest = {
                            languageMenuExpanded = false
                        }
                    ) {
                        supportedLanguages.forEach { language ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        androidx.compose.material3.Text(
                                            text = if (language == selectedLanguage) {
                                                "✓ "
                                            } else {
                                                "   "
                                            },
                                            color = primaryBlue,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        androidx.compose.material3.Text(
                                            text = language,
                                            color = textPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = if (language == selectedLanguage) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.Normal
                                            }
                                        )
                                    }
                                },
                                onClick = {
                                    saveLanguage(language)
                                }
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
                    appText("my_health", selectedLanguage),
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
                                    appText("bmi_information", selectedLanguage),
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
                                    appText("saved_body", selectedLanguage),
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
                                    appText("current_bmi", selectedLanguage),
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
                                    appText("status", selectedLanguage),
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
                        title = appText("dob", selectedLanguage),
                        value = dobValue
                    )

                    HealthDataDivider()

                    HealthDataRow(
                        title = appText("age", selectedLanguage),
                        value = ageValue
                    )

                    HealthDataDivider()

                    HealthDataRow(
                        title = appText("gender", selectedLanguage),
                        value = genderValue
                    )

                    HealthDataDivider()

                    HealthDataRow(
                        title = appText("height", selectedLanguage),
                        value = heightValue
                    )

                    HealthDataDivider()

                    HealthDataRow(
                        title = appText("weight", selectedLanguage),
                        value = weightValue
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(25.dp)
            )

            // =================================================
            // MY MEDICINE REMINDERS
            // =================================================

            androidx.compose.material3.Text(
                text = appText("my_reminders", selectedLanguage),
                color = textPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(11.dp))

            if (savedReminders.isEmpty()) {
                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = surface
                    )
                ) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        androidx.compose.material3.Text(
                            text = "🔔 No medicine reminders",
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        androidx.compose.material3.Text(
                            text = appText("reminders_here", selectedLanguage),
                            color = textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                val groupedReminders = savedReminders
                    .sortedByDescending { it.createdAt }
                    .groupBy { profileReminderDayKey(it.createdAt) }

                groupedReminders.forEach { (dayKey, dayReminders) ->
                    androidx.compose.material3.Text(
                        text = profileReminderDayLabel(dayKey),
                        color = teal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 7.dp)
                    )

                    dayReminders.forEach { reminder ->
                        androidx.compose.material3.Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = surfaceVariant
                            )
                        ) {
                            Column(Modifier.fillMaxWidth().padding(15.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    androidx.compose.material3.Text(
                                        text = "💊",
                                        fontSize = 25.sp
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        androidx.compose.material3.Text(
                                            text = reminder.medicineName.ifBlank { appText("medicine", selectedLanguage) },
                                            color = textPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (reminder.strength.isNotBlank()) {
                                            androidx.compose.material3.Text(
                                                text = reminder.strength,
                                                color = textSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    androidx.compose.material3.Text(
                                        text = if (reminder.enabled) "ON" else "OFF",
                                        color = if (reminder.enabled) teal else textSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(Modifier.height(9.dp))
                                androidx.compose.material3.Text(
                                    text = "⏰ ${reminder.time.ifBlank { appText("time_not_set", selectedLanguage) }} • ${reminder.daySlot.ifBlank { appText("daily", selectedLanguage) }}",
                                    color = textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (reminder.mealTiming.isNotBlank()) {
                                    androidx.compose.material3.Text(
                                        text = "🍽 ${reminder.mealTiming}",
                                        color = textSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (reminder.startDate.isNotBlank() || reminder.endDate.isNotBlank()) {
                                    androidx.compose.material3.Text(
                                        text = "📅 ${listOfNotNull(reminder.startDate.takeIf { it.isNotBlank() }, reminder.endDate.takeIf { it.isNotBlank() }).joinToString(" → ")}",
                                        color = textSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // =================================================
            // MY PRESCRIPTIONS
            // =================================================

            androidx.compose.material3.Text(
                text = appText("my_prescriptions", selectedLanguage),
                color = textPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(11.dp)
            )

            val prescriptionArray =
                JSONArray(savedPrescriptions)

            if (prescriptionArray.length() == 0) {

                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = surface
                    ),
                    elevation = androidx.compose.material3.CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        androidx.compose.material3.Text(
                            text = "📄",
                            fontSize = 36.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        androidx.compose.material3.Text(
                            text = appText("no_prescriptions", selectedLanguage),
                            color = textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        androidx.compose.material3.Text(
                            text = "Your scanned prescriptions will appear here after you save them.",
                            color = textSecondary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )
                    }
                }

            } else {

                for (i in 0 until prescriptionArray.length()) {

                    val prescription =
                        prescriptionArray.optJSONObject(i) ?: continue

                    val prescriptionId =
                        prescription.optString("id")

                    val doctor =
                        prescription.optString("doctorName")

                    val patient =
                        prescription.optString("patientName")

                    val date =
                        prescription.optString("date")

                    val diagnosisText =
                        prescription.optString("diagnosis")

                    val hospitalText =
                        prescription.optString("hospitalName")

                    val medicines =
                        prescription.optJSONArray("medicines")
                            ?: JSONArray()

                    val medicineCount =
                        medicines.length()

                    androidx.compose.material3.Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedPrescriptionId = prescriptionId
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = surface
                        ),
                        elevation = androidx.compose.material3.CardDefaults.cardElevation(
                            defaultElevation = 3.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            surfaceVariant,
                                            RoundedCornerShape(14.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.material3.Text(
                                        text = "📄",
                                        fontSize = 25.sp
                                    )
                                }

                                Spacer(
                                    modifier = Modifier.width(12.dp)
                                )

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    androidx.compose.material3.Text(
                                        text = if (doctor.isNotBlank()) {
                                            "Dr. $doctor"
                                        } else {
                                            "Saved Prescription"
                                        },
                                        color = textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(
                                        modifier = Modifier.height(4.dp)
                                    )

                                    androidx.compose.material3.Text(
                                        text = buildString {
                                            if (date.isNotBlank()) {
                                                append(date)
                                            }
                                            if (medicineCount > 0) {
                                                if (isNotEmpty()) append("  •  ")
                                                append("$medicineCount medicine")
                                                if (medicineCount != 1) append("s")
                                            }
                                            if (isEmpty()) {
                                                append("Prescription details")
                                            }
                                        },
                                        color = textSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                androidx.compose.material3.Text(
                                    text = "›",
                                    color = primaryBlue,
                                    fontSize = 27.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (hospitalText.isNotBlank() ||
                                patient.isNotBlank() ||
                                diagnosisText.isNotBlank()
                            ) {
                                Spacer(modifier = Modifier.height(18.dp))

                                if (hospitalText.isNotBlank()) {
                                    androidx.compose.material3.Text(
                                        text = "Hospital: $hospitalText",
                                        color = textSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (patient.isNotBlank()) {
                                    androidx.compose.material3.Text(
                                        text = "Patient: $patient",
                                        color = textSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (diagnosisText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(3.dp))

                                    androidx.compose.material3.Text(
                                        text = "Diagnosis: $diagnosisText",
                                        color = textSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )
                }
            }

            // =================================================
            // PRESCRIPTION DETAIL
            // =================================================

            selectedPrescriptionId?.let { selectedId ->

                val selectedPrescription =
                    (0 until prescriptionArray.length())
                        .mapNotNull {
                            prescriptionArray.optJSONObject(it)
                        }
                        .firstOrNull {
                            it.optString("id") == selectedId
                        }

                if (selectedPrescription != null) {

                    val selectedMedicines =
                        selectedPrescription.optJSONArray("medicines")
                            ?: JSONArray()

                    fun shareSavedPrescription() {
                        val shareLines = mutableListOf<String>()
                        shareLines += "MEDASSIST AI - Prescription"

                        val shareHospital =
                            selectedPrescription.optString("hospitalName")
                        val shareDoctor =
                            selectedPrescription.optString("doctorName")
                        val sharePatient =
                            selectedPrescription.optString("patientName")
                        val shareDate =
                            selectedPrescription.optString("date")
                        val shareDiagnosis =
                            selectedPrescription.optString("diagnosis")

                        if (shareHospital.isNotBlank()) {
                            shareLines += "Hospital / Clinic: $shareHospital"
                        }
                        if (shareDoctor.isNotBlank()) {
                            shareLines += "Doctor: $shareDoctor"
                        }
                        if (sharePatient.isNotBlank()) {
                            shareLines += "Patient: $sharePatient"
                        }
                        if (shareDate.isNotBlank()) {
                            shareLines += "Date: $shareDate"
                        }
                        if (shareDiagnosis.isNotBlank()) {
                            shareLines += "Diagnosis: $shareDiagnosis"
                        }

                        for (k in 0 until selectedMedicines.length()) {
                            val medicine =
                                selectedMedicines.optJSONObject(k) ?: continue

                            val name = medicine.optString("medicineName")
                            val strength = medicine.optString("strength")
                            val timing = medicine.optString("timing")
                            val mealTiming = medicine.optString("mealTiming")
                            val duration = medicine.optString("duration")
                            val howToTake = medicine.optString("howToTake")
                            val instructions = medicine.optString("instructions")

                            shareLines += ""
                            shareLines += "Medicine ${k + 1}: $name"
                            if (strength.isNotBlank()) shareLines += "Strength: $strength"
                            if (timing.isNotBlank()) shareLines += "Time: $timing"
                            if (mealTiming.isNotBlank()) shareLines += "Food: $mealTiming"
                            if (duration.isNotBlank()) shareLines += "Duration: $duration"
                            if (howToTake.isNotBlank()) {
                                shareLines += "How to take: $howToTake"
                            } else if (instructions.isNotBlank()) {
                                shareLines += "Instructions: $instructions"
                            }
                        }

                        val imageUri =
                            selectedPrescription.optString("imageUri")

                        val intent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                "MEDASSIST Prescription"
                            )
                            putExtra(
                                Intent.EXTRA_TEXT,
                                shareLines.joinToString("\n")
                            )

                            if (imageUri.isNotBlank()) {
                                type = "image/*"
                                putExtra(
                                    Intent.EXTRA_STREAM,
                                    android.net.Uri.parse(imageUri)
                                )
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            } else {
                                type = "text/plain"
                            }
                        }

                        context.startActivity(
                            Intent.createChooser(
                                intent,
                                "Share Prescription"
                            )
                        )
                    }

                    androidx.compose.material3.Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = surface
                        ),
                        elevation = androidx.compose.material3.CardDefaults.cardElevation(
                            defaultElevation = 4.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    androidx.compose.material3.Text(
                                        text = "📋 Prescription Details",
                                        color = primaryDark,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(3.dp))

                                    androidx.compose.material3.Text(
                                        text = "Saved prescription",
                                        color = textSecondary,
                                        fontSize = 10.sp
                                    )
                                }

                                androidx.compose.material3.Text(
                                    text = "×",
                                    modifier = Modifier
                                        .clickable {
                                            selectedPrescriptionId = null
                                        }
                                        .padding(6.dp),
                                    color = textSecondary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            val detailDoctor =
                                selectedPrescription.optString("doctorName")
                            val detailPatient =
                                selectedPrescription.optString("patientName")
                            val detailDate =
                                selectedPrescription.optString("date")
                            val detailDiagnosis =
                                selectedPrescription.optString("diagnosis")
                            val detailHospital =
                                selectedPrescription.optString("hospitalName")

                            if (detailHospital.isNotBlank()) {
                                HealthDataRow(
                                    title = "Hospital / Clinic",
                                    value = detailHospital
                                )
                                HealthDataDivider()
                            }

                            if (detailDoctor.isNotBlank()) {
                                HealthDataRow(
                                    title = "Doctor",
                                    value = detailDoctor
                                )
                                HealthDataDivider()
                            }

                            if (detailPatient.isNotBlank()) {
                                HealthDataRow(
                                    title = "Patient",
                                    value = detailPatient
                                )
                                HealthDataDivider()
                            }

                            if (detailDate.isNotBlank()) {
                                HealthDataRow(
                                    title = "Date",
                                    value = detailDate
                                )
                                HealthDataDivider()
                            }

                            if (detailDiagnosis.isNotBlank()) {
                                HealthDataRow(
                                    title = "Diagnosis",
                                    value = detailDiagnosis
                                )
                            }

                            if (detailDoctor.isNotBlank() ||
                                detailPatient.isNotBlank() ||
                                detailDate.isNotBlank() ||
                                detailDiagnosis.isNotBlank()
                            ) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            androidx.compose.material3.Text(
                                text = "💊 Medicines",
                                color = primaryDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (selectedMedicines.length() == 0) {
                                androidx.compose.material3.Text(
                                    text = "No medicine details saved.",
                                    color = textSecondary,
                                    fontSize = 11.sp
                                )
                            } else {
                                for (j in 0 until selectedMedicines.length()) {

                                    val medicine =
                                        selectedMedicines.optJSONObject(j)
                                            ?: JSONObject()

                                    val name =
                                        medicine.optString("medicineName")
                                    val strength =
                                        medicine.optString("strength")
                                    val frequency =
                                        medicine.optString("frequency")
                                    val duration =
                                        medicine.optString("duration")
                                    val instructions =
                                        medicine.optString("instructions")
                                    val timing =
                                        medicine.optString("timing")
                                    val mealTiming =
                                        medicine.optString("mealTiming")
                                    val howToTake =
                                        medicine.optString("howToTake")

                                    androidx.compose.material3.Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        shape = RoundedCornerShape(15.dp),
                                        colors = androidx.compose.material3.CardDefaults.cardColors(
                                            containerColor = surfaceVariant
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp)
                                        ) {
                                            androidx.compose.material3.Text(
                                                text = "${j + 1}. ${name.ifBlank { appText("medicine", selectedLanguage) }}",
                                                color = textPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )

                                            if (strength.isNotBlank()) {
                                                androidx.compose.material3.Text(
                                                    text = "Strength: $strength",
                                                    color = textSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            if (timing.isNotBlank()) {
                                                androidx.compose.material3.Text(
                                                    text = "Time: $timing",
                                                    color = textSecondary,
                                                    fontSize = 11.sp
                                                )
                                            } else if (frequency.isNotBlank()) {
                                                androidx.compose.material3.Text(
                                                    text = "Frequency / Schedule: $frequency",
                                                    color = textSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            if (mealTiming.isNotBlank()) {
                                                androidx.compose.material3.Text(
                                                    text = "Food: $mealTiming",
                                                    color = textSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            if (duration.isNotBlank()) {
                                                androidx.compose.material3.Text(
                                                    text = "Duration: $duration",
                                                    color = textSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            if (howToTake.isNotBlank()) {
                                                androidx.compose.material3.Text(
                                                    text = "How to take: $howToTake",
                                                    color = textSecondary,
                                                    fontSize = 11.sp,
                                                    lineHeight = 16.sp
                                                )
                                            } else if (instructions.isNotBlank()) {
                                                androidx.compose.material3.Text(
                                                    text = "Instructions: $instructions",
                                                    color = textSecondary,
                                                    fontSize = 11.sp,
                                                    lineHeight = 16.sp
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            androidx.compose.material3.Button(
                                                onClick = {
                                                    onSetReminderClick(
                                                        name,
                                                        strength,
                                                        frequency,
                                                        duration,
                                                        instructions,
                                                        timing,
                                                        mealTiming,
                                                        howToTake
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                    containerColor = teal
                                                )
                                            ) {
                                                androidx.compose.material3.Text(
                                                    text = "🔔 Set Reminder",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            androidx.compose.material3.Text(
                                text = "⚠ Prescription information is saved for reference. Always follow your doctor's instructions.",
                                color = textSecondary,
                                fontSize = 10.sp,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            androidx.compose.material3.Button(
                                onClick = {
                                    shareSavedPrescription()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = primaryBlue
                                )
                            ) {
                                androidx.compose.material3.Text(
                                    text = "↗ Share Prescription",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            androidx.compose.material3.Button(
                                onClick = {
                                    showDeleteDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = logoutBackground,
                                    contentColor = logoutText
                                )
                            ) {
                                androidx.compose.material3.Text(
                                    text = "🗑 Delete Prescription",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // =================================================
            // DELETE CONFIRMATION
            // =================================================

            if (showDeleteDialog && selectedPrescriptionId != null) {

                androidx.compose.material3.AlertDialog(
                    onDismissRequest = {
                        showDeleteDialog = false
                    },
                    title = {
                        androidx.compose.material3.Text(
                            text = "Delete Prescription?"
                        )
                    },
                    text = {
                        androidx.compose.material3.Text(
                            text = "This saved prescription will be removed from My Prescriptions."
                        )
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                selectedPrescriptionId?.let { id ->
                                    PrescriptionRepository.delete(
                                        context,
                                        id
                                    )
                                }

                                showDeleteDialog = false
                                selectedPrescriptionId = null
                                refreshPrescriptions()
                            }
                        ) {
                            androidx.compose.material3.Text(
                                text = "Delete",
                                color = logoutText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                showDeleteDialog = false
                            }
                        ) {
                            androidx.compose.material3.Text("Cancel")
                        }
                    }
                )
            }

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
                                appText("sign_out", selectedLanguage),
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
// GENDER PROFILE AVATAR
// =============================================================

@Composable
private fun GenderProfileAvatar(
    gender: String,
    primaryBlue: Color,
    teal: Color
) {
    val isMale = gender.equals("Male", ignoreCase = true)
    val isFemale = gender.equals("Female", ignoreCase = true)

    Box(
        modifier =
            Modifier
                .size(100.dp)
                .shadow(
                    elevation = 9.dp,
                    shape = CircleShape
                )
                .background(
                    Brush.linearGradient(
                        colors =
                            if (isFemale) {
                                listOf(
                                    Color(0xFFE7B8F3),
                                    Color(0xFFB85BCF)
                                )
                            } else {
                                listOf(
                                    primaryBlue,
                                    teal
                                )
                            }
                    ),
                    CircleShape
                ),
        contentAlignment = Alignment.Center
    ) {

        androidx.compose.foundation.Canvas(
            modifier = Modifier.size(82.dp)
        ) {

            val cx = size.width / 2f

            val skin = Color(0xFFF2C6A5)
            val hair = Color(0xFF34251F)
            val shirt =
                if (isFemale) {
                    Color(0xFFFFFFFF)
                } else {
                    Color(0xFFEAF7FF)
                }

            val headRadius = size.width * 0.205f
            val headY = size.height * 0.31f

            // Shoulders / body
            drawRoundRect(
                color = shirt,
                topLeft =
                    Offset(
                        size.width * 0.18f,
                        size.height * 0.58f
                    ),
                size =
                    Size(
                        size.width * 0.64f,
                        size.height * 0.34f
                    ),
                cornerRadius =
                    androidx.compose.ui.geometry.CornerRadius(
                        22f,
                        22f
                    )
            )

            // Neck
            drawRoundRect(
                color = skin,
                topLeft =
                    Offset(
                        cx - 8f,
                        size.height * 0.45f
                    ),
                size = Size(16f, 18f),
                cornerRadius =
                    androidx.compose.ui.geometry.CornerRadius(
                        5f,
                        5f
                    )
            )

            if (isFemale) {

                // Long hair behind face
                drawCircle(
                    color = hair,
                    radius = headRadius * 1.34f,
                    center =
                        Offset(cx, headY + 2f)
                )

                // Face
                drawCircle(
                    color = skin,
                    radius = headRadius,
                    center =
                        Offset(cx, headY)
                )

                // Front hair
                drawArc(
                    color = hair,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft =
                        Offset(
                            cx - headRadius,
                            headY - headRadius
                        ),
                    size =
                        Size(
                            headRadius * 2f,
                            headRadius * 1.18f
                        )
                )

                // Hair side locks
                drawRoundRect(
                    color = hair,
                    topLeft =
                        Offset(
                            cx - headRadius * 1.22f,
                            headY - 3f
                        ),
                    size =
                        Size(
                            headRadius * 0.34f,
                            headRadius * 1.70f
                        ),
                    cornerRadius =
                        androidx.compose.ui.geometry.CornerRadius(
                            10f,
                            10f
                        )
                )

                drawRoundRect(
                    color = hair,
                    topLeft =
                        Offset(
                            cx + headRadius * 0.88f,
                            headY - 3f
                        ),
                    size =
                        Size(
                            headRadius * 0.34f,
                            headRadius * 1.70f
                        ),
                    cornerRadius =
                        androidx.compose.ui.geometry.CornerRadius(
                            10f,
                            10f
                        )
                )

            } else {

                // Face
                drawCircle(
                    color = skin,
                    radius = headRadius,
                    center =
                        Offset(cx, headY)
                )

                // Short male hair
                drawArc(
                    color = hair,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft =
                        Offset(
                            cx - headRadius,
                            headY - headRadius
                        ),
                    size =
                        Size(
                            headRadius * 2f,
                            headRadius * 1.10f
                        )
                )
            }

            // Eyes
            drawCircle(
                color = Color(0xFF292929),
                radius = 2.4f,
                center =
                    Offset(
                        cx - 7f,
                        headY
                    )
            )

            drawCircle(
                color = Color(0xFF292929),
                radius = 2.4f,
                center =
                    Offset(
                        cx + 7f,
                        headY
                    )
            )

            // Smile
            drawArc(
                color = Color(0xFF9C5549),
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft =
                    Offset(
                        cx - 8f,
                        headY + 5f
                    ),
                size =
                    Size(16f, 10f),
                style =
                    androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 2f
                    )
            )
        }
    }
}

// =============================================================
// HEALTH DATA ROW
// =============================================================

private fun profileReminderDayKey(timeMillis: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timeMillis))

private fun profileReminderDayLabel(dayKey: String): String {
    val parsed = try {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dayKey)
    } catch (_: Exception) { null }
    if (parsed == null) return dayKey

    val today = java.util.Calendar.getInstance()
    val target = java.util.Calendar.getInstance().apply { time = parsed }
    if (today.get(java.util.Calendar.YEAR) == target.get(java.util.Calendar.YEAR) &&
        today.get(java.util.Calendar.DAY_OF_YEAR) == target.get(java.util.Calendar.DAY_OF_YEAR)) {
        return "Today • ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(parsed)}"
    }

    val yesterday = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
    if (yesterday.get(java.util.Calendar.YEAR) == target.get(java.util.Calendar.YEAR) &&
        yesterday.get(java.util.Calendar.DAY_OF_YEAR) == target.get(java.util.Calendar.DAY_OF_YEAR)) {
        return "Yesterday • ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(parsed)}"
    }

    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(parsed)
}

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
            text = title,
            modifier = Modifier.width(105.dp),
            color = Color(0xFF71818C),
            fontSize = 12.sp,
            maxLines = 1,
            softWrap = false
        )

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        androidx.compose.material3.Text(
            text = value,
            modifier = Modifier.weight(1f),
            color = Color(0xFF172B4D),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
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