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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.RadioButtonDefaults
import com.example.ai_based_medical_chatbot.LocalAppLanguageController
import com.example.ai_based_medical_chatbot.appText
import com.example.ai_based_medical_chatbot.ui.theme.DarkTeal
import com.example.ai_based_medical_chatbot.ui.theme.DeepTeal
import com.example.ai_based_medical_chatbot.ui.theme.Ivory
import com.example.ai_based_medical_chatbot.ui.theme.Mint
import com.example.ai_based_medical_chatbot.ui.theme.PureWhite
import com.example.ai_based_medical_chatbot.ui.theme.SageGreen
import com.example.ai_based_medical_chatbot.ui.theme.SoftMint
import com.example.ai_based_medical_chatbot.ui.theme.StatusError
import com.example.ai_based_medical_chatbot.ui.theme.StatusErrorBackground
import com.example.ai_based_medical_chatbot.ui.theme.StatusErrorBorder
import com.example.ai_based_medical_chatbot.ui.theme.StatusSuccess
import com.example.ai_based_medical_chatbot.ui.theme.StatusWarning
import com.example.ai_based_medical_chatbot.ui.theme.TextDark
import com.example.ai_based_medical_chatbot.ui.theme.TextMuted
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

    val weightFocusRequester = remember {
        FocusRequester()
    }

    val calculateAndUpdateBmi: () -> Unit = {
        val finalHeight = height.toFloatOrNull()
        val finalWeight = weight.toFloatOrNull()

        validationMessage = when {
            dateOfBirth.length != 10 -> t("bmi_validation_dob")
            parseDateOfBirth(dateOfBirth) == null -> t("bmi_validation_dob_invalid")
            calculatedAge <= 0 -> t("bmi_validation_dob_invalid")
            calculatedAge > 120 -> t("bmi_validation_age")
            gender.isBlank() -> t("bmi_validation_gender")
            finalHeight == null || finalHeight !in 50f..250f -> t("bmi_validation_height")
            finalWeight == null || finalWeight !in 2f..300f -> t("bmi_validation_weight")
            else -> ""
        }

        if (validationMessage.isBlank() && finalHeight != null && finalWeight != null) {
            BMIRepository.saveBMIData(
                context = context,
                heightCm = finalHeight,
                weightKg = finalWeight,
                dateOfBirth = dateOfBirth,
                gender = gender
            )
            saved = true
        }
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
                TextMuted

            liveBmi < 18.5f ->
                SageGreen

            liveBmi < 25f ->
                StatusSuccess

            liveBmi < 30f ->
                StatusWarning

            else ->
                StatusError
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
                        Ivory,
                        SoftMint
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
                .imePadding()
                .padding(
                    horizontal = 20.dp,
                    vertical = 14.dp
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
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(44.dp)
                        .background(PureWhite, RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, Mint), RoundedCornerShape(12.dp)),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = PureWhite,
                        contentColor = DarkTeal
                    )
                ) {

                    Icon(
                        imageVector =
                            Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = t("bmi_back"),
                        tint = DarkTeal,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = t("bmi_checker"),
                        color = DarkTeal,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )

                    Text(
                        text = t("bmi_subtitle"),
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            // =====================================================
            // HERO CARD
            // =====================================================

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DarkTeal
                ),
                border = BorderStroke(1.dp, Mint.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
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
                            .size(56.dp)
                            .background(
                                PureWhite.copy(
                                    alpha = 0.15f
                                ),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector = ScaleVectorIcon,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(14.dp)
                    )

                    Column {

                        Text(
                            text = t("bmi_know"),
                            color = PureWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = t("bmi_enter_details"),
                            color =
                                PureWhite.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            // =====================================================
            // PERSONAL INFORMATION
            // =====================================================

            Text(
                text = t("bmi_personal_info"),
                color = DarkTeal,
                fontSize = 18.sp,
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
                            tint = DeepTeal,
                            modifier =
                                Modifier.size(24.dp)
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
                    RoundedCornerShape(16.dp)
            )

            // Date error

            if (
                dateOfBirth.length == 10 &&
                parseDateOfBirth(dateOfBirth) == null
            ) {

                Text(
                    text = t("bmi_invalid_date"),
                    color =
                        StatusError,
                    fontSize = 11.sp,
                    modifier =
                        Modifier.padding(
                            start = 6.dp,
                            top = 5.dp
                        )
                )
            }

            Spacer(
                modifier = Modifier.height(14.dp)
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
                                TextMuted,
                            fontSize = 13.sp
                        )
                    }
                },

                colors =
                    bmiFieldColors(),

                shape =
                    RoundedCornerShape(16.dp)
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            // =====================================================
            // GENDER
            // =====================================================

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = PureWhite
                ),
                border = BorderStroke(1.dp, Mint),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 1.dp
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {

                    Text(
                        text = t("bmi_gender"),
                        color = DarkTeal,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
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
                modifier = Modifier.height(24.dp)
            )

            // =====================================================
            // BODY MEASUREMENTS
            // =====================================================

            Text(
                text = t("bmi_body_measurements"),
                color = DarkTeal,
                fontSize = 18.sp,
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
                        color = DeepTeal,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },

                trailingIcon = {

                    Text(
                        text = t("bmi_cm"),
                        color =
                            TextMuted,
                        fontSize = 13.sp
                    )
                },

                keyboardOptions =
                    KeyboardOptions(
                        keyboardType =
                            KeyboardType.Decimal,
                        imeAction =
                            ImeAction.Next
                    ),

                keyboardActions =
                    KeyboardActions(
                        onNext = {
                            weightFocusRequester.requestFocus()
                        }
                    ),

                colors =
                    bmiFieldColors(),

                shape =
                    RoundedCornerShape(16.dp)
            )

            Spacer(
                modifier = Modifier.height(14.dp)
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

                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(weightFocusRequester),

                singleLine = true,

                label = {
                    Text(t("bmi_weight"))
                },

                placeholder = {
                    Text(t("bmi_weight_example"))
                },

                leadingIcon = {
                    Icon(
                        imageVector = ScaleVectorIcon,
                        contentDescription = null,
                        tint = DeepTeal,
                        modifier = Modifier.size(20.dp)
                    )
                },

                trailingIcon = {

                    Text(
                        text = t("bmi_kg"),
                        color =
                            TextMuted,
                        fontSize = 13.sp
                    )
                },

                keyboardOptions =
                    KeyboardOptions(
                        keyboardType =
                            KeyboardType.Decimal,
                        imeAction =
                            ImeAction.Done
                    ),

                keyboardActions =
                    KeyboardActions(
                        onDone = {
                            calculateAndUpdateBmi()
                        }
                    ),

                colors =
                    bmiFieldColors(),

                shape =
                    RoundedCornerShape(16.dp)
            )

            Spacer(
                modifier = Modifier.height(22.dp)
            )

            // =====================================================
            // VALIDATION
            // =====================================================

            if (validationMessage.isNotBlank()) {

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(14.dp),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                StatusErrorBackground
                        ),
                    border =
                        BorderStroke(1.dp, StatusErrorBorder)
                ) {

                    Text(
                        text =
                            validationMessage,
                        modifier =
                            Modifier.padding(13.dp),
                        color =
                            StatusError,
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
                onClick = calculateAndUpdateBmi,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp),

                shape =
                    RoundedCornerShape(16.dp),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            DeepTeal,
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
                        Modifier.height(10.dp)
                )

                Text(
                    text = t("bmi_saved"),
                    modifier =
                        Modifier.fillMaxWidth(),
                    color =
                        StatusSuccess,
                    fontSize = 13.sp,
                    fontWeight =
                        FontWeight.SemiBold,
                    textAlign =
                        TextAlign.Center
                )
            }

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            // =====================================================
            // BMI RESULT
            // =====================================================

            Card(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(20.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            PureWhite
                    ),

                border =
                    BorderStroke(1.dp, Mint),

                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 2.dp
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
                            TextMuted,
                        fontSize = 13.sp,
                        fontWeight =
                            FontWeight.Medium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text = bmiText,
                        color =
                            DarkTeal,
                        fontSize = 48.sp,
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
                                        alpha = 0.12f
                                    ),
                                    RoundedCornerShape(24.dp)
                                )
                                .border(
                                    BorderStroke(1.dp, bmiStatusColor.copy(alpha = 0.35f)),
                                    RoundedCornerShape(24.dp)
                                )
                                .padding(
                                    horizontal = 18.dp,
                                    vertical = 8.dp
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
                            Modifier.height(18.dp)
                    )

                    Text(
                        text = t("bmi_categories"),
                        color =
                            DarkTeal,
                        fontSize = 12.sp,
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text = t("bmi_categories_values"),
                        modifier =
                            Modifier.fillMaxWidth(),
                        color =
                            TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        textAlign =
                            TextAlign.Center
                    )

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    SoftMint,
                                    RoundedCornerShape(14.dp)
                                )
                                .border(
                                    BorderStroke(1.dp, Mint),
                                    RoundedCornerShape(14.dp)
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
                                DeepTeal,
                            modifier =
                                Modifier.size(20.dp)
                        )

                        Spacer(
                            modifier =
                                Modifier.width(10.dp)
                        )

                        Text(
                            text = t("bmi_disclaimer"),
                            color =
                                TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
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
                    TextMuted,
                fontSize = 10.sp,
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
            TextDark,

        unfocusedTextColor =
            TextDark,

        focusedBorderColor =
            DeepTeal,

        unfocusedBorderColor =
            Mint,

        focusedLabelColor =
            DeepTeal,

        unfocusedLabelColor =
            TextMuted,

        focusedPlaceholderColor =
            TextMuted,

        unfocusedPlaceholderColor =
            TextMuted,

        focusedLeadingIconColor =
            DeepTeal,

        unfocusedLeadingIconColor =
            DeepTeal,

        focusedTrailingIconColor =
            DeepTeal,

        unfocusedTrailingIconColor =
            TextMuted,

        cursorColor =
            DeepTeal,

        focusedContainerColor =
            PureWhite,

        unfocusedContainerColor =
            PureWhite
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
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = DeepTeal,
                unselectedColor = Mint
            )
        )

        Spacer(
            modifier =
                Modifier.width(2.dp)
        )

        Text(
            text = text,
            color =
                TextDark,
            fontSize = 13.sp,
            fontWeight =
                FontWeight.Medium
        )
    }
}