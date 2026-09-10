package ui

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun BMICheckerScreen(
    onBackClick: () -> Unit
) {

    // =========================================================
    // COLORS
    // =========================================================

    val backgroundTop = Color(0xFFF7FBFF)
    val backgroundBottom = Color(0xFFEAF5FA)

    val darkBlue = Color(0xFF123A56)
    val primaryBlue = Color(0xFF1976D2)
    val bannerBlue = Color(0xFF17609A)

    val textPrimary = Color(0xFF172B4D)
    val textSecondary = Color(0xFF667085)

    val fieldText = Color(0xFF173F63)
    val fieldBorder = Color(0xFF7B8C9B)
    val focusedBorder = Color(0xFF5B9BE8)
    val placeholderColor = Color(0xFF71879A)

    val surface = Color.White
    val surfaceVariant = Color(0xFFEAF5FA)

    // =========================================================
    // CONTEXT
    // =========================================================

    val context =
        androidx.compose.ui.platform.LocalContext.current

    // =========================================================
    // IMPORTANT
    // =========================================================
    // Every time this screen is opened, the input fields start
    // EMPTY. Saved BMI data is NOT loaded into these fields.
    // Data will only be saved when "Update BMI" is pressed.
    // =========================================================

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

    // =========================================================
    // CALCULATE AGE FROM DOB
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
    // LIVE BMI PREVIEW
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
                Locale.getDefault(),
                "%.1f",
                liveBmi
            )
        } else {
            "--"
        }

    val bmiStatus =
        when {
            liveBmi <= 0f ->
                "Not Available"

            liveBmi < 18.5f ->
                "Underweight"

            liveBmi < 25f ->
                "Normal"

            liveBmi < 30f ->
                "Overweight"

            else ->
                "Obese"
        }

    // =========================================================
    // ROOT
    // =========================================================

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                backgroundTop,
                                backgroundBottom
                            )
                    )
                )
    ) {

        Column(
            modifier =
                Modifier
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
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                IconButton(
                    onClick =
                        onBackClick
                ) {

                    Icon(
                        imageVector =
                            Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription =
                            "Back",
                        tint =
                            darkBlue,
                        modifier =
                            Modifier.size(27.dp)
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(5.dp)
                )

                Column {

                    Text(
                        text =
                            "BMI Checker",
                        color =
                            darkBlue,
                        fontSize =
                            22.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(2.dp)
                    )

                    Text(
                        text =
                            "Check and manage your body mass index",
                        color =
                            textSecondary,
                        fontSize =
                            12.sp
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(22.dp)
            )

            // =================================================
            // KNOW YOUR BMI BANNER
            // =================================================

            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(27.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            bannerBlue
                    ),
                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation =
                            5.dp
                    )
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(62.dp)
                                .background(
                                    Color.White.copy(
                                        alpha = 0.13f
                                    ),
                                    RoundedCornerShape(18.dp)
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Info,
                            contentDescription =
                                "BMI information",
                            tint =
                                Color.White,
                            modifier =
                                Modifier.size(31.dp)
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.width(15.dp)
                    )

                    Column {

                        Text(
                            text =
                                "Know your BMI",
                            color =
                                Color.White,
                            fontSize =
                                20.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                "Keep your health information updated",
                            color =
                                Color.White.copy(
                                    alpha = 0.85f
                                ),
                            fontSize =
                                12.sp
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(28.dp)
            )

            // =================================================
            // PERSONAL INFORMATION
            // =================================================

            Text(
                text =
                    "Personal Information",
                color =
                    darkBlue,
                fontSize =
                    21.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            // =================================================
            // DATE OF BIRTH
            // =================================================

            OutlinedTextField(
                value =
                    dateOfBirth,
                onValueChange = { input ->

                    val digitsOnly =
                        input.filter {
                            it.isDigit()
                        }.take(8)

                    dateOfBirth =
                        when {
                            digitsOnly.length <= 2 ->
                                digitsOnly

                            digitsOnly.length <= 4 ->
                                digitsOnly.substring(0, 2) +
                                        "/" +
                                        digitsOnly.substring(2)

                            else ->
                                digitsOnly.substring(0, 2) +
                                        "/" +
                                        digitsOnly.substring(2, 4) +
                                        "/" +
                                        digitsOnly.substring(4)
                        }
                },
                modifier =
                    Modifier.fillMaxWidth(),
                singleLine =
                    true,
                label = {
                    Text(
                        text =
                            "Date of Birth"
                    )
                },
                placeholder = {
                    Text(
                        text =
                            "DD/MM/YYYY"
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector =
                            Icons.Default.Info,
                        contentDescription =
                            "Date of Birth"
                    )
                },
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType =
                            KeyboardType.Number
                    ),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedTextColor =
                            fieldText,
                        unfocusedTextColor =
                            fieldText,

                        focusedBorderColor =
                            focusedBorder,
                        unfocusedBorderColor =
                            fieldBorder,

                        focusedLabelColor =
                            primaryBlue,
                        unfocusedLabelColor =
                            textSecondary,

                        focusedPlaceholderColor =
                            placeholderColor,
                        unfocusedPlaceholderColor =
                            placeholderColor,

                        focusedLeadingIconColor =
                            primaryBlue,
                        unfocusedLeadingIconColor =
                            primaryBlue,

                        cursorColor =
                            primaryBlue,

                        focusedContainerColor =
                            Color.Transparent,
                        unfocusedContainerColor =
                            Color.Transparent
                    ),
                shape =
                    RoundedCornerShape(22.dp)
            )

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            // =================================================
            // AGE
            // =================================================
            // Age is automatically calculated from DOB.
            // It is intentionally NOT editable.
            // =================================================

            OutlinedTextField(
                value =
                    ageText,
                onValueChange = {
                    // Age cannot be manually changed.
                    // It is calculated from Date of Birth.
                },
                modifier =
                    Modifier.fillMaxWidth(),
                readOnly =
                    true,
                singleLine =
                    true,
                label = {
                    Text(
                        text =
                            "Age"
                    )
                },
                placeholder = {
                    Text(
                        text =
                            "Calculated from date of birth"
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector =
                            Icons.Default.Person,
                        contentDescription =
                            "Age"
                    )
                },
                trailingIcon = {
                    Text(
                        text =
                            "years",
                        color =
                            fieldText,
                        fontSize =
                            14.sp,
                        fontWeight =
                            FontWeight.Medium
                    )
                },
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedTextColor =
                            fieldText,
                        unfocusedTextColor =
                            fieldText,

                        focusedBorderColor =
                            focusedBorder,
                        unfocusedBorderColor =
                            fieldBorder,

                        focusedLabelColor =
                            primaryBlue,
                        unfocusedLabelColor =
                            textSecondary,

                        focusedPlaceholderColor =
                            placeholderColor,
                        unfocusedPlaceholderColor =
                            placeholderColor,

                        focusedLeadingIconColor =
                            primaryBlue,
                        unfocusedLeadingIconColor =
                            primaryBlue,

                        focusedTrailingIconColor =
                            fieldText,
                        unfocusedTrailingIconColor =
                            fieldText,

                        cursorColor =
                            primaryBlue,

                        focusedContainerColor =
                            Color.Transparent,
                        unfocusedContainerColor =
                            Color.Transparent
                    ),
                shape =
                    RoundedCornerShape(22.dp)
            )

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            // =================================================
            // GENDER
            // =================================================

            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(22.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            surface
                    ),
                elevation =
                    CardDefaults.cardElevation(
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

                    Text(
                        text =
                            "Gender",
                        color =
                            darkBlue,
                        fontSize =
                            16.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(9.dp)
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        GenderOption(
                            text =
                                "Male",
                            selected =
                                gender == "Male",
                            onClick = {
                                gender =
                                    "Male"
                            }
                        )

                        GenderOption(
                            text =
                                "Female",
                            selected =
                                gender == "Female",
                            onClick = {
                                gender =
                                    "Female"
                            }
                        )

                        GenderOption(
                            text =
                                "Other",
                            selected =
                                gender == "Other",
                            onClick = {
                                gender =
                                    "Other"
                            }
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(27.dp)
            )

            // =================================================
            // BODY MEASUREMENTS
            // =================================================

            Text(
                text =
                    "Body Measurements",
                color =
                    darkBlue,
                fontSize =
                    21.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            // =================================================
            // HEIGHT
            // =================================================

            OutlinedTextField(
                value =
                    height,
                onValueChange = {
                    height =
                        it
                            .filter {
                                    character ->
                                character.isDigit() ||
                                        character == '.'
                            }
                            .take(6)
                },
                modifier =
                    Modifier.fillMaxWidth(),
                singleLine =
                    true,
                label = {
                    Text(
                        text =
                            "Height"
                    )
                },
                placeholder = {
                    Text(
                        text =
                            "Enter your height"
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector =
                            Icons.Default.Info,
                        contentDescription =
                            "Height"
                    )
                },
                trailingIcon = {
                    Text(
                        text =
                            "cm",
                        color =
                            fieldText,
                        fontSize =
                            14.sp,
                        fontWeight =
                            FontWeight.Medium
                    )
                },
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType =
                            KeyboardType.Decimal
                    ),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedTextColor =
                            fieldText,
                        unfocusedTextColor =
                            fieldText,

                        focusedBorderColor =
                            focusedBorder,
                        unfocusedBorderColor =
                            fieldBorder,

                        focusedLabelColor =
                            primaryBlue,
                        unfocusedLabelColor =
                            textSecondary,

                        focusedPlaceholderColor =
                            placeholderColor,
                        unfocusedPlaceholderColor =
                            placeholderColor,

                        focusedLeadingIconColor =
                            primaryBlue,
                        unfocusedLeadingIconColor =
                            primaryBlue,

                        focusedTrailingIconColor =
                            fieldText,
                        unfocusedTrailingIconColor =
                            fieldText,

                        cursorColor =
                            primaryBlue,

                        focusedContainerColor =
                            Color.Transparent,
                        unfocusedContainerColor =
                            Color.Transparent
                    ),
                shape =
                    RoundedCornerShape(22.dp)
            )

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            // =================================================
            // WEIGHT
            // =================================================

            OutlinedTextField(
                value =
                    weight,
                onValueChange = {
                    weight =
                        it
                            .filter {
                                    character ->
                                character.isDigit() ||
                                        character == '.'
                            }
                            .take(6)
                },
                modifier =
                    Modifier.fillMaxWidth(),
                singleLine =
                    true,
                label = {
                    Text(
                        text =
                            "Weight"
                    )
                },
                placeholder = {
                    Text(
                        text =
                            "Enter your weight"
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector =
                            Icons.Default.Info,
                        contentDescription =
                            "Weight"
                    )
                },
                trailingIcon = {
                    Text(
                        text =
                            "kg",
                        color =
                            fieldText,
                        fontSize =
                            14.sp,
                        fontWeight =
                            FontWeight.Medium
                    )
                },
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType =
                            KeyboardType.Decimal
                    ),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedTextColor =
                            fieldText,
                        unfocusedTextColor =
                            fieldText,

                        focusedBorderColor =
                            focusedBorder,
                        unfocusedBorderColor =
                            fieldBorder,

                        focusedLabelColor =
                            primaryBlue,
                        unfocusedLabelColor =
                            textSecondary,

                        focusedPlaceholderColor =
                            placeholderColor,
                        unfocusedPlaceholderColor =
                            placeholderColor,

                        focusedLeadingIconColor =
                            primaryBlue,
                        unfocusedLeadingIconColor =
                            primaryBlue,

                        focusedTrailingIconColor =
                            fieldText,
                        unfocusedTrailingIconColor =
                            fieldText,

                        cursorColor =
                            primaryBlue,

                        focusedContainerColor =
                            Color.Transparent,
                        unfocusedContainerColor =
                            Color.Transparent
                    ),
                shape =
                    RoundedCornerShape(22.dp)
            )

            Spacer(
                modifier =
                    Modifier.height(22.dp)
            )

            // =================================================
            // UPDATE BMI
            // =================================================

            Button(
                onClick = {

                    val finalHeight =
                        height.toFloatOrNull()

                    val finalWeight =
                        weight.toFloatOrNull()

                    if (
                        finalHeight != null &&
                        finalWeight != null &&
                        finalHeight > 0f &&
                        finalWeight > 0f &&
                        dateOfBirth.isNotBlank() &&
                        calculatedAge > 0 &&
                        gender.isNotBlank()
                    ) {

                        BMIRepository.saveBMIData(
                            context =
                                context,
                            heightCm =
                                finalHeight,
                            weightKg =
                                finalWeight,
                            dateOfBirth =
                                dateOfBirth,
                            gender =
                                gender
                        )
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                shape =
                    RoundedCornerShape(22.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            primaryBlue,
                        contentColor =
                            Color.White
                    )
            ) {

                Text(
                    text =
                        "Update BMI",
                    fontSize =
                        16.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(22.dp)
            )

            // =================================================
            // BMI RESULT
            // =================================================

            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(25.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            surface
                    ),
                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation =
                            4.dp
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
                        text =
                            "Your BMI",
                        color =
                            textSecondary,
                        fontSize =
                            15.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.height(5.dp)
                    )

                    Text(
                        text =
                            bmiText,
                        color =
                            Color(0xFF17619A),
                        fontSize =
                            52.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(7.dp)
                    )

                    Box(
                        modifier =
                            Modifier
                                .background(
                                    surfaceVariant,
                                    RoundedCornerShape(30.dp)
                                )
                                .padding(
                                    horizontal = 22.dp,
                                    vertical = 10.dp
                                )
                    ) {

                        Text(
                            text =
                                bmiStatus,
                            color =
                                Color(0xFF17619A),
                            fontSize =
                                14.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(17.dp)
                    )

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFFF0F6FB),
                                    RoundedCornerShape(15.dp)
                                )
                                .padding(12.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Info,
                            contentDescription =
                                "BMI information",
                            tint =
                                primaryBlue,
                            modifier =
                                Modifier.size(20.dp)
                        )

                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )

                        Text(
                            text =
                                "BMI is a general indicator and may not be accurate for everyone.",
                            color =
                                textSecondary,
                            fontSize =
                                10.sp,
                            lineHeight =
                                15.sp
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(25.dp)
            )

            // =================================================
            // FOOTER
            // =================================================

            Text(
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
                    Modifier.height(15.dp)
            )
        }
    }
}

// =============================================================
// AGE CALCULATOR
// =============================================================


 private fun calculateAgeFromDateOfBirth(
    dateOfBirth: String
): Int {

    if (dateOfBirth.length != 10) {
        return 0
    }

    return try {

        val format =
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            )

        format.isLenient = false

        val birthDate =
            format.parse(dateOfBirth)
                ?: return 0

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
            birthCalendar.get(Calendar.DAY_OF_MONTH)

        if (
            currentMonth < birthMonth ||
            (
                    currentMonth == birthMonth &&
                            currentDay < birthDay
                    )
        ) {
            age--
        }

        if (age < 0) {
            0
        } else {
            age
        }

    } catch (
        exception: Exception
    ) {
        0
    }
}

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
            selected =
                selected,
            onClick =
                onClick
        )

        Spacer(
            modifier =
                Modifier.width(2.dp)
        )

        Text(
            text =
                text,
            color =
                Color(0xFF273B53),
            fontSize =
                13.sp,
            fontWeight =
                FontWeight.Medium
        )
    }
}