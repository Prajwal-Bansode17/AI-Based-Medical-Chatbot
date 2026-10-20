package ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ai_based_medical_chatbot.ui.theme.TextDark
import com.example.ai_based_medical_chatbot.ui.theme.TextMuted
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun BMICheckerScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val appLanguageController = LocalAppLanguageController.current
    val selectedLanguage = appLanguageController.selectedLanguage

    fun t(key: String): String = appText(key, selectedLanguage)

    // Load initial saved BMI data from repository
    val savedBmiData = remember { BMIRepository.getBMIData(context) }

    // Store raw digits only (up to 8 digits) for robust typing without IME digit corruption or jumping
    var dobDigits by remember {
        mutableStateOf(savedBmiData.dateOfBirth.filter { it.isDigit() }.take(8))
    }

    val dateOfBirth = remember(dobDigits) {
        formatDobDigits(dobDigits)
    }

    var gender by remember {
        mutableStateOf(savedBmiData.gender.ifBlank { "Male" })
    }

    // Default selector values for unconfigured form: 150 cm and 50 kg
    // If user already had saved data > 0, preserve those values strictly!
    var heightCm by remember {
        mutableStateOf(
            if (savedBmiData.heightCm > 0f) savedBmiData.heightCm else 150f
        )
    }

    var weightKg by remember {
        mutableStateOf(
            if (savedBmiData.weightKg > 0f) savedBmiData.weightKg else 50f
        )
    }

    // Controls whether height, weight, or calendar compact popups are visible
    var showHeightDialog by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    var validationMessage by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    // Calculated Age directly derived from parsed Date of Birth
    val calculatedAge = calculateAgeFromDateOfBirth(dateOfBirth)
    val ageText = if (calculatedAge > 0) calculatedAge.toString() else ""

    // Live BMI calculation from confirmed height & weight values
    val currentBmi = if (heightCm > 0f && weightKg > 0f) {
        val heightMeters = heightCm / 100f
        weightKg / (heightMeters * heightMeters)
    } else {
        0f
    }

    val calculateAndUpdateBmi: () -> Unit = {
        validationMessage = when {
            dobDigits.length != 8 -> t("bmi_validation_dob")
            parseDateOfBirth(dateOfBirth) == null -> t("bmi_validation_dob_invalid")
            calculatedAge <= 0 -> t("bmi_validation_dob_invalid")
            calculatedAge > 120 -> t("bmi_validation_age")
            gender.isBlank() -> t("bmi_validation_gender")
            heightCm !in 50f..250f -> t("bmi_validation_height")
            weightKg !in 2f..300f -> t("bmi_validation_weight")
            else -> ""
        }

        if (validationMessage.isBlank()) {
            BMIRepository.saveBMIData(
                context = context,
                heightCm = heightCm,
                weightKg = weightKg,
                dateOfBirth = dateOfBirth,
                gender = gender
            )
            saved = true
        }
    }

    // Compact Animated Popups
    if (showHeightDialog) {
        HeightSelectionDialog(
            initialHeightCm = heightCm,
            onConfirm = { confirmedHeight ->
                heightCm = confirmedHeight
                showHeightDialog = false
                saved = false
                // Auto-save height to centralized repository
                BMIRepository.updateHeight(context, confirmedHeight)
            },
            onDismiss = {
                showHeightDialog = false
            },
            t = ::t
        )
    }

    if (showWeightDialog) {
        WeightSelectionDialog(
            initialWeightKg = weightKg,
            onConfirm = { confirmedWeight ->
                weightKg = confirmedWeight
                showWeightDialog = false
                saved = false
                // Auto-save weight to centralized repository
                BMIRepository.updateWeight(context, confirmedWeight)
            },
            onDismiss = {
                showWeightDialog = false
            },
            t = ::t
        )
    }

    if (showDatePickerDialog) {
        MedAssistDatePickerDialog(
            initialDate = parseDateOfBirth(dateOfBirth),
            onDateSelected = { day, month, year ->
                val selectedDigits = String.format(Locale.US, "%02d%02d%04d", day, month, year)
                dobDigits = selectedDigits
                validationMessage = ""
                saved = false
                showDatePickerDialog = false
                val formattedDob = formatDobDigits(selectedDigits)
                if (parseDateOfBirth(formattedDob) != null) {
                    val current = BMIRepository.getBMIData(context)
                    BMIRepository.updatePersonalDetails(
                        context = context,
                        dateOfBirth = formattedDob,
                        gender = gender,
                        bloodGroup = current.bloodGroup,
                        fullName = current.fullName
                    )
                }
            },
            onDismiss = {
                showDatePickerDialog = false
            },
            t = ::t
        )
    }

    // Root Screen Container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Ivory, SoftMint)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Header: Back button, Title & Subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(PureWhite, RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, Mint), RoundedCornerShape(12.dp)),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = PureWhite,
                        contentColor = DarkTeal
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = t("bmi_back"),
                        tint = DarkTeal,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = t("bmi_title"),
                        color = DarkTeal,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = t("bmi_subtitle"),
                        color = TextMuted,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Hero Banner Card (Compact)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkTeal),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(DarkTeal, DeepTeal)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    PureWhite.copy(alpha = 0.16f),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = ScaleVectorIcon,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = t("bmi_hero_title"),
                                color = PureWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = t("bmi_hero_subtitle"),
                                color = PureWhite.copy(alpha = 0.88f),
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Personal Information Card (Compact)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, Mint),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = t("bmi_personal_info"),
                        color = DarkTeal,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Date of Birth: Manual typing with VisualTransformation + Calendar Dialog
                    OutlinedTextField(
                        value = dobDigits,
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() }.take(8)
                            dobDigits = clean
                            saved = false

                            if (clean.length == 8) {
                                val parsed = parseDateOfBirth(formatDobDigits(clean))
                                if (parsed != null) {
                                    validationMessage = ""
                                    val current = BMIRepository.getBMIData(context)
                                    BMIRepository.updatePersonalDetails(
                                        context = context,
                                        dateOfBirth = formatDobDigits(clean),
                                        gender = gender,
                                        bloodGroup = current.bloodGroup,
                                        fullName = current.fullName
                                    )
                                } else {
                                    validationMessage = t("bmi_invalid_date")
                                }
                            } else {
                                validationMessage = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(t("bmi_date_of_birth"), fontSize = 13.sp) },
                        placeholder = { Text(t("bmi_date_placeholder"), fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = t("bmi_date_of_birth"),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showDatePickerDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = t("bmi_select_date"),
                                    tint = DeepTeal,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        visualTransformation = DateVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = bmiFieldColors(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    if (dobDigits.length == 8 && parseDateOfBirth(dateOfBirth) == null) {
                        Text(
                            text = t("bmi_invalid_date"),
                            color = StatusError,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 6.dp, top = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Age Field (Auto-calculated from DOB)
                    OutlinedTextField(
                        value = if (ageText.isNotBlank()) "$ageText ${t("bmi_years")}" else "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(t("bmi_age"), fontSize = 13.sp) },
                        placeholder = { Text(t("bmi_age_auto"), fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = t("bmi_age"),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        colors = bmiFieldColors(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Gender Selector Row (Compact)
                    Text(
                        text = t("bmi_gender"),
                        color = DarkTeal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GenderRadioButton(
                            label = t("bmi_male"),
                            selected = gender == "Male",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                gender = "Male"
                                validationMessage = ""
                                saved = false
                                val current = BMIRepository.getBMIData(context)
                                BMIRepository.updatePersonalDetails(
                                    context = context,
                                    dateOfBirth = current.dateOfBirth,
                                    gender = "Male",
                                    bloodGroup = current.bloodGroup,
                                    fullName = current.fullName
                                )
                            }
                        )
                        GenderRadioButton(
                            label = t("bmi_female"),
                            selected = gender == "Female",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                gender = "Female"
                                validationMessage = ""
                                saved = false
                                val current = BMIRepository.getBMIData(context)
                                BMIRepository.updatePersonalDetails(
                                    context = context,
                                    dateOfBirth = current.dateOfBirth,
                                    gender = "Female",
                                    bloodGroup = current.bloodGroup,
                                    fullName = current.fullName
                                )
                            }
                        )
                        GenderRadioButton(
                            label = t("bmi_other"),
                            selected = gender == "Other",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                gender = "Other"
                                validationMessage = ""
                                saved = false
                                val current = BMIRepository.getBMIData(context)
                                BMIRepository.updatePersonalDetails(
                                    context = context,
                                    dateOfBirth = current.dateOfBirth,
                                    gender = "Other",
                                    bloodGroup = current.bloodGroup,
                                    fullName = current.fullName
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Section: Body Measurements (Height & Weight interactive selector cards)
            Text(
                text = t("bmi_body_measurements"),
                color = DarkTeal,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tappable Height Field
            MeasurementSelectorCard(
                title = t("bmi_height"),
                subtitle = t("bmi_height_subtitle"),
                valueText = String.format(Locale.US, "%.0f cm", heightCm),
                secondaryValue = run {
                    val inchesTotal = heightCm / 2.54f
                    val ft = (inchesTotal / 12f).toInt()
                    val inch = (inchesTotal % 12f).toInt()
                    "($ft' $inch\")"
                },
                iconSymbol = "↕",
                onClick = { showHeightDialog = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tappable Weight Field
            MeasurementSelectorCard(
                title = t("bmi_weight"),
                subtitle = t("bmi_weight_subtitle"),
                valueText = String.format(Locale.US, "%.1f kg", weightKg),
                secondaryValue = String.format(Locale.US, "(%.1f lbs)", weightKg * 2.20462f),
                iconVector = ScaleVectorIcon,
                onClick = { showWeightDialog = true }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Inline Validation Banner
            if (validationMessage.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusErrorBackground),
                    border = BorderStroke(1.dp, StatusErrorBorder)
                ) {
                    Text(
                        text = validationMessage,
                        modifier = Modifier.padding(10.dp),
                        color = StatusError,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Calculate & Save Action Button
            Button(
                onClick = calculateAndUpdateBmi,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(14.dp), spotColor = DeepTeal.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepTeal,
                    contentColor = PureWhite
                )
            ) {
                Text(
                    text = t("bmi_update"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (saved) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = t("bmi_saved"),
                    modifier = Modifier.fillMaxWidth(),
                    color = StatusSuccess,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Futuristic Speedometer Result Gauge (Compact & Unobstructed)
            BmiSpeedometerGauge(
                bmiValue = currentBmi,
                t = ::t
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Clean, interactive card for Body Measurement inputs (Height & Weight).
 * Tapping opens the respective compact animated popup dialog.
 */
@Composable
private fun MeasurementSelectorCard(
    title: String,
    subtitle: String,
    valueText: String,
    secondaryValue: String,
    iconSymbol: String? = null,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.2.dp, Mint),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SoftMint, RoundedCornerShape(10.dp))
                    .border(BorderStroke(1.dp, Mint.copy(alpha = 0.7f)), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (iconVector != null) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = title,
                        tint = DeepTeal,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = iconSymbol ?: "•",
                        color = DeepTeal,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = DarkTeal,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = valueText,
                    color = DeepTeal,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = secondaryValue,
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun GenderRadioButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (selected) DeepTeal else Mint
    val bgColor = if (selected) SoftMint else PureWhite

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = DeepTeal,
                    unselectedColor = TextMuted
                ),
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = label,
                color = if (selected) DarkTeal else TextMuted,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

/**
 * VisualTransformation that renders raw digit inputs (e.g. "08092004") visually as "08/09/2004"
 * with bi-directional offset mapping to preserve natural cursor navigation, backspace, and middle editing.
 */
class DateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length > 8) text.text.substring(0, 8) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 1 || i == 3) {
                out += "/"
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 1) return offset
                if (offset <= 3) return offset + 1
                if (offset <= 8) return offset + 2
                return out.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return offset.coerceAtMost(trimmed.length)
                if (offset <= 5) return (offset - 1).coerceAtMost(trimmed.length)
                if (offset <= 10) return (offset - 2).coerceAtMost(trimmed.length)
                return trimmed.length
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

fun formatDobDigits(digits: String): String {
    val clean = digits.filter { it.isDigit() }.take(8)
    return when {
        clean.length <= 2 -> clean
        clean.length <= 4 -> "${clean.substring(0, 2)}/${clean.substring(2)}"
        else -> "${clean.substring(0, 2)}/${clean.substring(2, 4)}/${clean.substring(4)}"
    }
}

fun parseDateOfBirth(value: String): Date? {
    if (value.length != 10) return null

    return try {
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        format.isLenient = false
        val date = format.parse(value) ?: return null

        if (date.after(Calendar.getInstance().time)) null else date
    } catch (_: Exception) {
        null
    }
}

fun calculateAgeFromDateOfBirth(dateOfBirth: String): Int {
    val birthDate = parseDateOfBirth(dateOfBirth) ?: return 0

    return try {
        val birthCalendar = Calendar.getInstance().apply { time = birthDate }
        val today = Calendar.getInstance()

        var age = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
        val currentMonth = today.get(Calendar.MONTH)
        val birthMonth = birthCalendar.get(Calendar.MONTH)
        val currentDay = today.get(Calendar.DAY_OF_MONTH)
        val birthDay = birthCalendar.get(Calendar.DAY_OF_MONTH)

        if (currentMonth < birthMonth || (currentMonth == birthMonth && currentDay < birthDay)) {
            age--
        }

        age.coerceAtLeast(0)
    } catch (_: Exception) {
        0
    }
}

@Composable
private fun bmiFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextDark,
    unfocusedTextColor = TextDark,
    focusedBorderColor = DeepTeal,
    unfocusedBorderColor = Mint,
    focusedLabelColor = DeepTeal,
    unfocusedLabelColor = TextMuted,
    focusedLeadingIconColor = DeepTeal,
    unfocusedLeadingIconColor = TextMuted,
    cursorColor = DeepTeal
)
