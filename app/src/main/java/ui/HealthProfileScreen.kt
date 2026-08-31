package ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_based_medical_chatbot.data.HealthProfile
import com.example.ai_based_medical_chatbot.data.SupabaseClient
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import android.app.DatePickerDialog
import java.util.Calendar
import java.util.Locale

private val ProfileGreen = Color(0xFF22C997)
private val ProfileBackground = Color(0xFF080D18)
private val ProfileText = Color(0xFFF8FAFC)
private val ProfileSecondary = Color(0xFF94A3B8)

@Composable
fun HealthProfileScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isLoading by remember {
        mutableStateOf(true)
    }

    var isSaving by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var name by remember {
        mutableStateOf("")
    }

    var gender by remember {
        mutableStateOf("")
    }

    var dateOfBirth by remember {
        mutableStateOf("")
    }

    var bloodGroup by remember {
        mutableStateOf("")
    }

    var height by remember {
        mutableStateOf("")
    }

    var weight by remember {
        mutableStateOf("")
    }

    var bmi by remember {
        mutableStateOf<Double?>(null)
    }

    /*
     * LOAD EXISTING PROFILE
     */
    LaunchedEffect(Unit) {

        val result =
            SupabaseClient.getHealthProfile(context)

        result.onSuccess { profile ->

            if (profile != null) {

                name =
                    profile.name.orEmpty()

                gender =
                    profile.gender.orEmpty()

                dateOfBirth =
                    profile.dateOfBirth.orEmpty()

                bloodGroup =
                    profile.bloodGroup.orEmpty()

                height =
                    profile.heightCm
                        ?.let {
                            if (it % 1.0 == 0.0) {
                                it.toInt().toString()
                            } else {
                                it.toString()
                            }
                        }
                        .orEmpty()

                weight =
                    profile.weightKg
                        ?.let {
                            if (it % 1.0 == 0.0) {
                                it.toInt().toString()
                            } else {
                                it.toString()
                            }
                        }
                        .orEmpty()

                bmi =
                    profile.bmi
            }

        }.onFailure {
            // Loading a profile is optional. Do not show a server error
            // before the user has interacted with the form.
            errorMessage = ""
        }

        isLoading = false
    }

    /*
     * DATE PICKER
     */
    fun openDatePicker() {
        val calendar = Calendar.getInstance()

        if (dateOfBirth.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            try {
                val parts = dateOfBirth.split("-")
                calendar.set(
                    parts[0].toInt(),
                    parts[1].toInt() - 1,
                    parts[2].toInt()
                )
            } catch (_: Exception) {
                // Use today's date if the stored date cannot be parsed.
            }
        }

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                dateOfBirth = String.format(
                    Locale.US,
                    "%04d-%02d-%02d",
                    year,
                    month + 1,
                    dayOfMonth
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    /*
     * BMI CALCULATION
     */
    fun calculateBmi() {

        val heightValue =
            height.toDoubleOrNull()

        val weightValue =
            weight.toDoubleOrNull()

        if (
            heightValue != null &&
            weightValue != null &&
            heightValue > 0 &&
            weightValue > 0
        ) {

            val heightMeters =
                heightValue / 100.0

            if (heightMeters > 0) {

                bmi =
                    weightValue /
                            (heightMeters * heightMeters)
            }

        } else {

            bmi = null
        }
    }

    /*
     * SAVE PROFILE
     */
    fun saveProfile(
        continueAfterSave: Boolean
    ) {

        scope.launch {

            isSaving = true
            errorMessage = ""

            calculateBmi()

            val cleanHeight =
                height
                    .toDoubleOrNull()
                    ?.takeIf {
                        it > 0
                    }

            val cleanWeight =
                weight
                    .toDoubleOrNull()
                    ?.takeIf {
                        it > 0
                    }

            val cleanBmi =
                bmi?.takeIf {
                    it > 0
                }

            val profile =
                HealthProfile(
                    id =
                        SupabaseClient
                            .getSavedUser(context)
                            ?.id
                            .orEmpty(),

                    name =
                        name
                            .trim()
                            .takeIf {
                                it.isNotBlank()
                            },

                    gender =
                        gender
                            .trim()
                            .takeIf {
                                it.isNotBlank()
                            },

                    dateOfBirth =
                        dateOfBirth
                            .trim()
                            .takeIf {
                                it.isNotBlank()
                            },

                    bloodGroup =
                        bloodGroup
                            .trim()
                            .takeIf {
                                it.isNotBlank()
                            },

                    heightCm =
                        cleanHeight,

                    weightKg =
                        cleanWeight,

                    bmi =
                        cleanBmi
                )

            val result =
                SupabaseClient.saveHealthProfile(
                    context = context,
                    profile = profile
                )

            result.onSuccess {

                isSaving = false

                if (continueAfterSave) {
                    onContinue()
                }

            }.onFailure {

                isSaving = false

                errorMessage =
                    it.message
                        ?: "Unable to save your personal information."
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ProfileBackground
    ) {

        if (isLoading) {

            Box(
                modifier =
                    Modifier.fillMaxSize(),
                contentAlignment =
                    Alignment.Center
            ) {

                CircularProgressIndicator(
                    color = ProfileGreen
                )
            }

            return@Surface
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        scrollState
                    )
                    .navigationBarsPadding()
        ) {

            /*
             * HEADER
             */

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 8.dp,
                            vertical = 8.dp
                        ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = onBack
                ) {

                    Icon(
                        imageVector =
                            Icons.Filled.ArrowBack,
                        contentDescription =
                            "Back",
                        tint =
                            ProfileText
                    )
                }

                Text(
                    text = "Personal Information",
                    color = ProfileText,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier =
                    Modifier.padding(
                        horizontal = 20.dp
                    )
            ) {

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text =
                        "Tell us about yourself",
                    color = ProfileText,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text =
                        "This information is optional. It helps MedAssist provide more personalized health guidance.",
                    color = ProfileSecondary,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )

                Spacer(
                    modifier =
                        Modifier.height(24.dp)
                )

                /*
                 * NAME
                 */

                ProfileLabel("Name")

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Enter your name")
                    },
                    singleLine = true,
                    colors =
                        androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ProfileText,
                            unfocusedTextColor = ProfileText,
                            focusedPlaceholderColor = Color(0xFF64748B),
                            unfocusedPlaceholderColor = Color(0xFF64748B),
                            focusedLabelColor = ProfileGreen,
                            unfocusedLabelColor = ProfileSecondary,
                            focusedBorderColor = ProfileGreen,
                            unfocusedBorderColor = Color(0xFF334155),
                            cursorColor = ProfileGreen
                        ),
                    shape =
                        RoundedCornerShape(14.dp)
                )

                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )

                /*
                 * GENDER
                 */

                ProfileLabel("Gender")

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    GenderButton(
                        text = "Female",
                        selected =
                            gender.equals(
                                "Female",
                                true
                            ),
                        onClick = {
                            gender = "Female"
                        },
                        modifier =
                            Modifier.weight(1f)
                    )

                    GenderButton(
                        text = "Male",
                        selected =
                            gender.equals(
                                "Male",
                                true
                            ),
                        onClick = {
                            gender = "Male"
                        },
                        modifier =
                            Modifier.weight(1f)
                    )

                    GenderButton(
                        text = "Other",
                        selected =
                            gender.equals(
                                "Other",
                                true
                            ),
                        onClick = {
                            gender = "Other"
                        },
                        modifier =
                            Modifier.weight(1f)
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )

                /*
                 * DATE OF BIRTH
                 */

                ProfileLabel(
                    "Date of Birth"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isSaving) {
                            openDatePicker()
                        }
                ) {
                    OutlinedTextField(
                        value = dateOfBirth,
                        onValueChange = { },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        enabled = false,
                        placeholder = {
                            Text(
                                text = "Select your date of birth",
                                color = Color(0xFF94A3B8)
                            )
                        },
                        trailingIcon = {
                            Text(
                                text = "📅",
                                fontSize = 20.sp
                            )
                        },
                        singleLine = true,
                        colors =
                            androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ProfileText,
                                unfocusedTextColor = ProfileText,
                                disabledTextColor = ProfileText,
                                focusedPlaceholderColor = Color(0xFF94A3B8),
                                unfocusedPlaceholderColor = Color(0xFF94A3B8),
                                disabledPlaceholderColor = Color(0xFF94A3B8),
                                focusedBorderColor = ProfileGreen,
                                unfocusedBorderColor = Color(0xFF334155),
                                disabledBorderColor = Color(0xFF334155),
                                cursorColor = ProfileGreen
                            ),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )

                /*
                 * BLOOD GROUP
                 */

                ProfileLabel(
                    "Blood Group"
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(7.dp)
                ) {

                    listOf(
                        "A+",
                        "A-",
                        "B+",
                        "B-",
                        "O+",
                        "O-",
                        "AB+",
                        "AB-"
                    ).forEach { group ->

                        GenderButton(
                            text = group,
                            selected =
                                bloodGroup.equals(
                                    group,
                                    true
                                ),
                            onClick = {
                                bloodGroup = group
                            },
                            modifier =
                                Modifier.weight(1f)
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )

                /*
                 * HEIGHT
                 */

                ProfileLabel(
                    "Height"
                )

                OutlinedTextField(
                    value = height,
                    onValueChange = {
                        val filtered = it
                            .filter { char -> char.isDigit() || char == '.' }
                            .let { value ->
                                val firstDot = value.indexOf('.')
                                if (firstDot >= 0) {
                                    value.substring(0, firstDot + 1) +
                                            value.substring(firstDot + 1).replace(".", "")
                                } else {
                                    value
                                }
                            }
                            .take(6)

                        height = filtered
                        calculateBmi()
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Height in cm")
                    },
                    suffix = {
                        Text("cm")
                    },
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Decimal
                        ),
                    colors =
                        androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ProfileText,
                            unfocusedTextColor = ProfileText,
                            focusedPlaceholderColor = Color(0xFF64748B),
                            unfocusedPlaceholderColor = Color(0xFF64748B),
                            focusedLabelColor = ProfileGreen,
                            unfocusedLabelColor = ProfileSecondary,
                            focusedBorderColor = ProfileGreen,
                            unfocusedBorderColor = Color(0xFF334155),
                            cursorColor = ProfileGreen
                        ),
                    shape =
                        RoundedCornerShape(14.dp)
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                /*
                 * WEIGHT
                 */

                ProfileLabel(
                    "Weight"
                )

                OutlinedTextField(
                    value = weight,
                    onValueChange = {
                        val filtered = it
                            .filter { char -> char.isDigit() || char == '.' }
                            .let { value ->
                                val firstDot = value.indexOf('.')
                                if (firstDot >= 0) {
                                    value.substring(0, firstDot + 1) +
                                            value.substring(firstDot + 1).replace(".", "")
                                } else {
                                    value
                                }
                            }
                            .take(6)

                        weight = filtered
                        calculateBmi()
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Weight in kg")
                    },
                    suffix = {
                        Text("kg")
                    },
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Decimal
                        ),
                    colors =
                        androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ProfileText,
                            unfocusedTextColor = ProfileText,
                            focusedPlaceholderColor = Color(0xFF64748B),
                            unfocusedPlaceholderColor = Color(0xFF64748B),
                            focusedLabelColor = ProfileGreen,
                            unfocusedLabelColor = ProfileSecondary,
                            focusedBorderColor = ProfileGreen,
                            unfocusedBorderColor = Color(0xFF334155),
                            cursorColor = ProfileGreen
                        ),
                    shape =
                        RoundedCornerShape(14.dp)
                )

                /*
                 * BMI
                 */

                if (bmi != null) {

                    Spacer(
                        modifier =
                            Modifier.height(20.dp)
                    )

                    BmiCard(
                        bmi = bmi!!
                    )
                }

                if (errorMessage.isNotBlank()) {

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    Text(
                        text = errorMessage,
                        color =
                            Color(0xFFF87171),
                        fontSize = 14.sp
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(28.dp)
                )

                /*
                 * SAVE
                 */

                Button(
                    onClick = {
                        saveProfile(true)
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                    enabled =
                        !isSaving,
                    shape =
                        RoundedCornerShape(16.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                ProfileGreen
                        )
                ) {

                    if (isSaving) {

                        CircularProgressIndicator(
                            modifier =
                                Modifier.height(22.dp),
                            color =
                                Color.White,
                            strokeWidth = 2.dp
                        )

                    } else {

                        Text(
                            text =
                                "Save & Continue",
                            fontSize = 16.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                /*
                 * SKIP
                 */

                TextButton(
                    onClick = {
                        onContinue()
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    enabled =
                        !isSaving
                ) {

                    Text(
                        text =
                            "Skip for now",
                        color =
                            ProfileGreen,
                        fontSize = 15.sp
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileLabel(
    text: String
) {

    Text(
        text = text,
        color = ProfileText,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier =
            Modifier.padding(
                bottom = 7.dp
            )
    )
}

@Composable
private fun GenderButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Surface(
        modifier =
            modifier
                .height(46.dp)
                .clickable(
                    onClick = onClick
                ),
        shape =
            RoundedCornerShape(12.dp),
        color =
            if (selected) {
                ProfileGreen
            } else {
                Color.White
            },
        border =
            androidx.compose.foundation.BorderStroke(
                1.dp,
                if (selected) {
                    ProfileGreen
                } else {
                    Color(0xFF334155)
                }
            )
    ) {

        Box(
            modifier =
                Modifier.fillMaxSize(),
            contentAlignment =
                Alignment.Center
        ) {

            Text(
                text = text,
                color =
                    if (selected) {
                        Color.White
                    } else {
                        ProfileText
                    },
                fontSize = 13.sp,
                fontWeight =
                    if (selected) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Medium
                    }
            )
        }
    }
}

@Composable
private fun BmiCard(
    bmi: Double
) {

    val category =
        when {

            bmi < 18.5 ->
                "Underweight"

            bmi < 25.0 ->
                "Normal"

            bmi < 30.0 ->
                "Overweight"

            else ->
                "Obesity"
        }

    Surface(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(16.dp),
        color =
            Color.White,
        shadowElevation =
            1.dp
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Column {

                Text(
                    text = "Your BMI",
                    color =
                        ProfileSecondary,
                    fontSize = 14.sp
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text =
                        String.format(
                            Locale.US,
                            "%.1f",
                            bmi
                        ),
                    color =
                        ProfileText,
                    fontSize = 28.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Text(
                text = category,
                color =
                    ProfileGreen,
                fontSize = 16.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}