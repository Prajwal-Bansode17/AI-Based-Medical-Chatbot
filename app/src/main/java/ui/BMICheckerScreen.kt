package ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.ai_based_medical_chatbot.ui.theme.MedicalBackground
import com.example.ai_based_medical_chatbot.ui.theme.MedicalBlue
import com.example.ai_based_medical_chatbot.ui.theme.MedicalBlueDark
import com.example.ai_based_medical_chatbot.ui.theme.MedicalSurface
import com.example.ai_based_medical_chatbot.ui.theme.MedicalSurfaceVariant
import com.example.ai_based_medical_chatbot.ui.theme.MedicalTeal
import com.example.ai_based_medical_chatbot.ui.theme.MedicalTextPrimary
import com.example.ai_based_medical_chatbot.ui.theme.MedicalTextSecondary
import com.example.ai_based_medical_chatbot.ui.theme.PureWhite
import com.example.ai_based_medical_chatbot.LocalAppLanguageController
import com.example.ai_based_medical_chatbot.appText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun BMICheckerScreen(
    onBackClick: () -> Unit
) {

    val context = LocalContext.current

    val appLanguageController = LocalAppLanguageController.current
    val selectedLanguage = appLanguageController.selectedLanguage

    fun t(key: String): String = appText(key, selectedLanguage)


    var dateOfBirth by remember {
        mutableStateOf("")
    }

    var gender by remember {
        mutableStateOf("")
    }

    var height by remember {
        mutableStateOf("")
    }

    var weight by remember {
        mutableStateOf("")
    }

    var validationMessage by remember {
        mutableStateOf("")
    }

    var saved by remember {
        mutableStateOf(false)
    }

    // =========================================================
    // AGE
    // =========================================================

    val calculatedAge =
        calculateAgeFromDateOfBirth(dateOfBirth)

    val ageText =
        if (calculatedAge > 0) {
            calculatedAge.toString()
        } else {
            ""
        }

    // =========================================================
    // BMI
    // =========================================================

    val heightValue =
        height.toFloatOrNull()

    val weightValue =
        weight.toFloatOrNull()

    val liveBmi =
        if (
            heightValue != null &&
            weightValue != null &&
            heightValue > 0f &&
            weightValue > 0f
        ) {

            val heightMeters =
                heightValue / 100f

            weightValue /
                    (heightMeters * heightMeters)

        } else {
            0f
        }

    val bmiText =
        if (liveBmi > 0f) {
            String.format(
                Locale.US,
                "%.1f",
                liveBmi
            )
        } else {
            "--"
        }

    val bmiStatusKey =
        when {
            liveBmi <= 0f -> "bmi_status_enter"
            liveBmi < 18.5f -> "bmi_status_underweight"
            liveBmi < 25f -> "bmi_status_normal"
            liveBmi < 30f -> "bmi_status_overweight"
            else -> "bmi_status_obesity"
        }

    val bmiStatus = t(bmiStatusKey)

    val bmiStatusColor =
        when {

            liveBmi <= 0f ->
                MedicalTextSecondary

            liveBmi < 18.5f ->
                Color(0xFF1976D2)

            liveBmi < 25f ->
                Color(0xFF16805C)

            liveBmi < 30f ->
                Color(0xFFE28A00)

            else ->
                Color(0xFFD64545)
        }

    // =========================================================
    // DATE PICKER
    // =========================================================

    fun openDatePicker() {

        val calendar =
            Calendar.getInstance()

        val currentYear =
            calendar.get(Calendar.YEAR)

        val currentMonth =
            calendar.get(Calendar.MONTH)

        val currentDay =
            calendar.get(Calendar.DAY_OF_MONTH)

        // If valid DOB already exists, open picker on that date.
        val existingDate =
            parseDateOfBirth(dateOfBirth)

        if (existingDate != null) {
            calendar.time = existingDate
        }

        val dialog =
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->

                    val selectedDate =
                        String.format(
                            Locale.US,
                            "%02d/%02d/%04d",
                            dayOfMonth,
                            month + 1,
                            year
                        )

                    dateOfBirth = selectedDate

                    validationMessage = ""

                    saved = false
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

        // Future DOB is not allowed.
        dialog.datePicker.maxDate =
            System.currentTimeMillis()

        dialog.show()
    }

    // =========================================================
    // ROOT
    // =========================================================

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8FCFF),
                        MedicalBackground,
                        Color(0xFFF1FAFC)
                    )
                )
            )
    ) {

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

            // =====================================================
            // HEADER
            // =====================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = onBackClick
                ) {

                    Icon(
                        imageVector =
                            Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = t("bmi_back"),
                        tint = MedicalBlueDark,
                        modifier = Modifier.size(27.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.width(5.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = t("bmi_checker"),
                        color = MedicalBlueDark,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )

                    Text(
                        text = t("bmi_subtitle"),
                        color = MedicalTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            // =====================================================
            // HERO CARD
            // =====================================================

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MedicalBlueDark
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 7.dp
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .background(
                                PureWhite.copy(
                                    alpha = 0.13f
                                ),
                                RoundedCornerShape(19.dp)
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text = "⚖",
                            color = PureWhite,
                            fontSize = 31.sp
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(15.dp)
                    )

                    Column {

                        Text(
                            text = t("bmi_know"),
                            color = PureWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = t("bmi_enter_details"),
                            color =
                                PureWhite.copy(alpha = 0.82f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(25.dp)
            )

            // =====================================================
            // PERSONAL INFORMATION
            // =====================================================

            Text(
                text = t("bmi_personal_info"),
                color = MedicalTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            // =====================================================
            // DATE OF BIRTH
            // =====================================================

            OutlinedTextField(
                value = dateOfBirth,

                onValueChange = { input ->

                    val digits =
                        input
                            .filter { it.isDigit() }
                            .take(8)

                    dateOfBirth =
                        formatDateInput(digits)

                    saved = false

                    validationMessage =
                        if (
                            dateOfBirth.length == 10 &&
                            parseDateOfBirth(dateOfBirth) == null
                        ) {
                            t("bmi_validation_date_format")
                        } else {
                            ""
                        }
                },

                modifier = Modifier.fillMaxWidth(),

                singleLine = true,

                label = {
                    Text(t("bmi_date_of_birth"))
                },

                placeholder = {
                    Text(t("bmi_date_placeholder"))
                },

                leadingIcon = {

                    Icon(
                        imageVector =
                            Icons.Default.Info,
                        contentDescription = t("bmi_date_of_birth")
                    )
                },

                trailingIcon = {

                    IconButton(
                        onClick = {
                            openDatePicker()
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.DateRange,
                            contentDescription = t("bmi_select_date"),
                            tint = MedicalBlue,
                            modifier =
                                Modifier.size(25.dp)
                        )
                    }
                },

                keyboardOptions =
                    KeyboardOptions(
                        keyboardType =
                            KeyboardType.Number
                    ),

                colors =
                    bmiFieldColors(),

                shape =
                    RoundedCornerShape(21.dp)
            )

            // Date error

            if (
                dateOfBirth.length == 10 &&
                parseDateOfBirth(dateOfBirth) == null
            ) {

                Text(
                    text = t("bmi_invalid_date"),
                    color =
                        Color(0xFFD64545),
                    fontSize = 11.sp,
                    modifier =
                        Modifier.padding(
                            start = 6.dp,
                            top = 5.dp
                        )
                )
            }

            Spacer(
                modifier = Modifier.height(13.dp)
            )

            // =====================================================
            // AGE
            // =====================================================

            OutlinedTextField(
                value = ageText,

                onValueChange = {},

                modifier = Modifier.fillMaxWidth(),

                readOnly = true,

                singleLine = true,

                label = {
                    Text(t("bmi_age"))
                },

                placeholder = {
                    Text(t("bmi_age_auto"))
                },

                leadingIcon = {

                    Icon(
                        imageVector =
                            Icons.Default.Person,
                        contentDescription = t("bmi_age")
                    )
                },

                trailingIcon = {

                    if (ageText.isNotBlank()) {

                        Text(
                            text = t("bmi_years"),
                            color =
                                MedicalTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                },

                colors =
                    bmiFieldColors(),

                shape =
                    RoundedCornerShape(21.dp)
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            // =====================================================
            // GENDER
            // =====================================================

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(21.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MedicalSurface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(17.dp)
                ) {

                    Text(
                        text = t("bmi_gender"),
                        color = MedicalTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        GenderOption(
                            text = t("bmi_male"),
                            selected =
                                gender == "Male",
                            onClick = {
                                gender = "Male"
                                validationMessage = ""
                                saved = false
                            }
                        )

                        GenderOption(
                            text = t("bmi_female"),
                            selected =
                                gender == "Female",
                            onClick = {
                                gender = "Female"
                                validationMessage = ""
                                saved = false
                            }
                        )

                        GenderOption(
                            text = t("bmi_other"),
                            selected =
                                gender == "Other",
                            onClick = {
                                gender = "Other"
                                validationMessage = ""
                                saved = false
                            }
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(25.dp)
            )

            // =====================================================
            // BODY MEASUREMENTS
            // =====================================================

            Text(
                text = t("bmi_body_measurements"),
                color = MedicalTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            // =====================================================
            // HEIGHT
            // =====================================================

            OutlinedTextField(
                value = height,

                onValueChange = { input ->

                    height =
                        sanitizeDecimalInput(
                            input,
                            6
                        )

                    validationMessage = ""
                    saved = false
                },

                modifier = Modifier.fillMaxWidth(),

                singleLine = true,

                label = {
                    Text(t("bmi_height"))
                },

                placeholder = {
                    Text(t("bmi_height_example"))
                },

                leadingIcon = {

                    Text(
                        text = "↕",
                        color = MedicalBlue,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                },

                trailingIcon = {

                    Text(
                        text = t("bmi_cm"),
                        color =
                            MedicalTextSecondary,
                        fontSize = 13.sp
                    )
                },

                keyboardOptions =
                    KeyboardOptions(
                        keyboardType =
                            KeyboardType.Decimal
                    ),

                colors =
                    bmiFieldColors(),

                shape =
                    RoundedCornerShape(21.dp)
            )

            Spacer(
                modifier = Modifier.height(13.dp)
            )

            // =====================================================
            // WEIGHT
            // =====================================================

            OutlinedTextField(
                value = weight,

                onValueChange = { input ->

                    weight =
                        sanitizeDecimalInput(
                            input,
                            6
                        )

                    validationMessage = ""
                    saved = false
                },

                modifier = Modifier.fillMaxWidth(),

                singleLine = true,

                label = {
                    Text(t("bmi_weight"))
                },

                placeholder = {
                    Text(t("bmi_weight_example"))
                },

                leadingIcon = {

                    Text(
                        text = "⚖",
                        color = MedicalTeal,
                        fontSize = 20.sp
                    )
                },

                trailingIcon = {

                    Text(
                        text = t("bmi_kg"),
                        color =
                            MedicalTextSecondary,
                        fontSize = 13.sp
                    )
                },

                keyboardOptions =
                    KeyboardOptions(
                        keyboardType =
                            KeyboardType.Decimal
                    ),

                colors =
                    bmiFieldColors(),

                shape =
                    RoundedCornerShape(21.dp)
            )

            Spacer(
                modifier = Modifier.height(21.dp)
            )

            // =====================================================
            // VALIDATION
            // =====================================================

            if (validationMessage.isNotBlank()) {

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(15.dp),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color(0xFFFFF2F2)
                        )
                ) {

                    Text(
                        text =
                            validationMessage,
                        modifier =
                            Modifier.padding(13.dp),
                        color =
                            Color(0xFFD64545),
                        fontSize = 12.sp,
                        fontWeight =
                            FontWeight.Medium
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )
            }

            // =====================================================
            // UPDATE BMI
            // =====================================================

            Button(
                onClick = {

                    val finalHeight =
                        height.toFloatOrNull()

                    val finalWeight =
                        weight.toFloatOrNull()

                    validationMessage =
                        when {

                            dateOfBirth.length != 10 ->
                                t("bmi_validation_dob")

                            parseDateOfBirth(
                                dateOfBirth
                            ) == null ->
                                t("bmi_validation_dob_invalid")

                            calculatedAge <= 0 ->
                                t("bmi_validation_dob_invalid")

                            calculatedAge > 120 ->
                                t("bmi_validation_age")

                            gender.isBlank() ->
                                t("bmi_validation_gender")

                            finalHeight == null ||
                                    finalHeight !in 50f..250f ->
                                t("bmi_validation_height")

                            finalWeight == null ||
                                    finalWeight !in 2f..300f ->
                                t("bmi_validation_weight")

                            else ->
                                ""
                        }

                    if (
                        validationMessage.isBlank()
                    ) {

                        BMIRepository.saveBMIData(
                            context =
                                context,
                            heightCm =
                                finalHeight!!,
                            weightKg =
                                finalWeight!!,
                            dateOfBirth =
                                dateOfBirth,
                            gender =
                                gender
                        )

                        saved = true
                    }

                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(58.dp),

                shape =
                    RoundedCornerShape(20.dp),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            MedicalBlue,
                        contentColor =
                            PureWhite
                    )
            ) {

                Text(
                    text = t("bmi_update"),
                    fontSize = 16.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            if (saved) {

                Spacer(
                    modifier =
                        Modifier.height(9.dp)
                )

                Text(
                    text = t("bmi_saved"),
                    modifier =
                        Modifier.fillMaxWidth(),
                    color =
                        Color(0xFF16805C),
                    fontSize = 12.sp,
                    fontWeight =
                        FontWeight.Medium,
                    textAlign =
                        TextAlign.Center
                )
            }

            Spacer(
                modifier =
                    Modifier.height(22.dp)
            )

            // =====================================================
            // BMI RESULT
            // =====================================================

            Card(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(26.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MedicalSurface
                    ),

                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 4.dp
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

                    Text(
                        text = t("bmi_your_bmi"),
                        color =
                            MedicalTextSecondary,
                        fontSize = 14.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text = bmiText,
                        color =
                            MedicalBlueDark,
                        fontSize = 50.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Box(
                        modifier =
                            Modifier
                                .background(
                                    bmiStatusColor.copy(
                                        alpha = 0.10f
                                    ),
                                    RoundedCornerShape(30.dp)
                                )
                                .padding(
                                    horizontal = 20.dp,
                                    vertical = 9.dp
                                )
                    ) {

                        Text(
                            text = bmiStatus,
                            color =
                                bmiStatusColor,
                            fontSize = 14.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    Text(
                        text = t("bmi_categories"),
                        color =
                            MedicalTextPrimary,
                        fontSize = 12.sp,
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text = t("bmi_categories_values"),
                        modifier =
                            Modifier.fillMaxWidth(),
                        color =
                            MedicalTextSecondary,
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                        textAlign =
                            TextAlign.Center
                    )

                    Spacer(
                        modifier =
                            Modifier.height(14.dp)
                    )

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    MedicalSurfaceVariant,
                                    RoundedCornerShape(15.dp)
                                )
                                .padding(12.dp),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Info,
                            contentDescription = t("bmi_information"),
                            tint =
                                MedicalBlue,
                            modifier =
                                Modifier.size(19.dp)
                        )

                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )

                        Text(
                            text = t("bmi_disclaimer"),
                            color =
                                MedicalTextSecondary,
                            fontSize = 10.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )

            Text(
                text = t("bmi_footer"),
                modifier =
                    Modifier.fillMaxWidth(),
                color =
                    MedicalTextSecondary,
                fontSize = 9.sp,
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
// DATE INPUT FORMATTER
// =============================================================

private fun formatDateInput(
    digits: String
): String {

    return when {

        digits.length <= 2 ->
            digits

        digits.length <= 4 ->
            digits.substring(
                0,
                2
            ) +
                    "/" +
                    digits.substring(
                        2
                    )

        else ->
            digits.substring(
                0,
                2
            ) +
                    "/" +
                    digits.substring(
                        2,
                        4
                    ) +
                    "/" +
                    digits.substring(
                        4
                    )
    }
}

// =============================================================
// PARSE + VALIDATE DOB
// =============================================================

private fun parseDateOfBirth(
    value: String
): java.util.Date? {

    if (value.length != 10) {
        return null
    }

    return try {

        val format =
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.US
            )

        format.isLenient = false

        val date =
            format.parse(value)
                ?: return null

        // Do not allow future DOB.
        if (
            date.after(
                Calendar.getInstance().time
            )
        ) {
            null
        } else {
            date
        }

    } catch (
        exception: Exception
    ) {
        null
    }
}

// =============================================================
// DECIMAL INPUT
// =============================================================

private fun sanitizeDecimalInput(
    input: String,
    maxLength: Int
): String {

    var result =
        input
            .filter {
                it.isDigit() || it == '.'
            }
            .take(maxLength)

    val firstDot =
        result.indexOf('.')

    if (firstDot >= 0) {

        result =
            result.substring(
                0,
                firstDot + 1
            ) +
                    result.substring(
                        firstDot + 1
                    ).replace(
                        ".",
                        ""
                    )
    }

    return result
}

// =============================================================
// AGE CALCULATOR
// =============================================================

private fun calculateAgeFromDateOfBirth(
    dateOfBirth: String
): Int {

    val birthDate =
        parseDateOfBirth(dateOfBirth)
            ?: return 0

    return try {

        val birthCalendar =
            Calendar.getInstance().apply {
                time = birthDate
            }

        val today =
            Calendar.getInstance()

        var age =
            today.get(Calendar.YEAR) -
                    birthCalendar.get(Calendar.YEAR)

        val currentMonth =
            today.get(Calendar.MONTH)

        val birthMonth =
            birthCalendar.get(Calendar.MONTH)

        val currentDay =
            today.get(Calendar.DAY_OF_MONTH)

        val birthDay =
            birthCalendar.get(
                Calendar.DAY_OF_MONTH
            )

        if (
            currentMonth < birthMonth ||
            (
                    currentMonth == birthMonth &&
                            currentDay < birthDay
                    )
        ) {
            age--
        }

        age.coerceAtLeast(0)

    } catch (
        exception: Exception
    ) {
        0
    }
}

// =============================================================
// TEXT FIELD COLORS
// =============================================================

@Composable
private fun bmiFieldColors() =
    OutlinedTextFieldDefaults.colors(

        focusedTextColor =
            MedicalTextPrimary,

        unfocusedTextColor =
            MedicalTextPrimary,

        focusedBorderColor =
            MedicalBlue,

        unfocusedBorderColor =
            Color(0xFF9AAAB8),

        focusedLabelColor =
            MedicalBlue,

        unfocusedLabelColor =
            MedicalTextSecondary,

        focusedPlaceholderColor =
            MedicalTextSecondary,

        unfocusedPlaceholderColor =
            MedicalTextSecondary,

        focusedLeadingIconColor =
            MedicalBlue,

        unfocusedLeadingIconColor =
            MedicalBlue,

        focusedTrailingIconColor =
            MedicalBlue,

        unfocusedTrailingIconColor =
            MedicalBlue,

        cursorColor =
            MedicalBlue,

        focusedContainerColor =
            Color.Transparent,

        unfocusedContainerColor =
            Color.Transparent
    )

// =============================================================
// GENDER OPTION
// =============================================================

@Composable
private fun GenderOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        RadioButton(
            selected = selected,
            onClick = onClick
        )

        Spacer(
            modifier =
                Modifier.width(2.dp)
        )

        Text(
            text = text,
            color =
                MedicalTextPrimary,
            fontSize = 13.sp,
            fontWeight =
                FontWeight.Medium
        )
    }
}