package ui

import android.app.DatePickerDialog
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_based_medical_chatbot.LocalAppLanguageController
import com.example.ai_based_medical_chatbot.appText
import com.example.ai_based_medical_chatbot.data.HealthProfile
import com.example.ai_based_medical_chatbot.data.SupabaseClient
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
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

@Composable
fun HealthProfileScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val appLanguageController = LocalAppLanguageController.current
    val selectedLanguage = appLanguageController.selectedLanguage

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var saveSuccessMessage by remember { mutableStateOf("") }

    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var bmi by remember { mutableStateOf<Double?>(null) }
    val weightFocusRequester = remember { FocusRequester() }

    /*
     * LOAD EXISTING PROFILE FROM SUPABASE
     */
    LaunchedEffect(Unit) {
        val result = SupabaseClient.getHealthProfile(context)

        result.onSuccess { profile ->
            if (profile != null) {
                name = profile.name.orEmpty()
                gender = profile.gender.orEmpty()
                dateOfBirth = profile.dateOfBirth.orEmpty()
                bloodGroup = profile.bloodGroup.orEmpty()
                height = profile.heightCm?.let {
                    if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
                }.orEmpty()
                weight = profile.weightKg?.let {
                    if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
                }.orEmpty()
                bmi = profile.bmi
            }
        }.onFailure {
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
        val heightValue = height.toDoubleOrNull()
        val weightValue = weight.toDoubleOrNull()

        if (heightValue != null && weightValue != null && heightValue > 0 && weightValue > 0) {
            val heightMeters = heightValue / 100.0
            if (heightMeters > 0) {
                bmi = weightValue / (heightMeters * heightMeters)
            }
        } else {
            bmi = null
        }
    }

    /*
     * SAVE PROFILE TO SUPABASE
     */
    fun saveProfile(continueAfterSave: Boolean) {
        scope.launch {
            isSaving = true
            errorMessage = ""
            saveSuccessMessage = ""

            calculateBmi()

            val cleanHeight = height.toDoubleOrNull()?.takeIf { it > 0 }
            val cleanWeight = weight.toDoubleOrNull()?.takeIf { it > 0 }
            val cleanBmi = bmi?.takeIf { it > 0 }

            val profile = HealthProfile(
                id = SupabaseClient.getSavedUser(context)?.id.orEmpty(),
                name = name.trim().takeIf { it.isNotBlank() },
                gender = gender.trim().takeIf { it.isNotBlank() },
                dateOfBirth = dateOfBirth.trim().takeIf { it.isNotBlank() },
                bloodGroup = bloodGroup.trim().takeIf { it.isNotBlank() },
                heightCm = cleanHeight,
                weightKg = cleanWeight,
                bmi = cleanBmi
            )

            val result = SupabaseClient.saveHealthProfile(
                context = context,
                profile = profile
            )

            result.onSuccess {
                isSaving = false
                saveSuccessMessage = "Health profile updated successfully."
                if (continueAfterSave) {
                    onContinue()
                }
            }.onFailure {
                isSaving = false
                errorMessage = it.message ?: "Unable to save your health information."
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Ivory, SoftMint)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DeepTeal)
            }
            return@Box
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            /*
             * TOP BAR
             */
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PureWhite)
                        .border(1.dp, Mint, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = appText("back", selectedLanguage),
                        tint = DarkTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "My Health Profile",
                        color = DarkTeal,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Personal & Medical Overview",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            /*
             * HERO INTRO CARD
             */
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, Mint),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(SoftMint)
                            .border(1.dp, Mint, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = StethoscopeVectorIcon,
                            contentDescription = null,
                            tint = DeepTeal,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Health Information",
                            color = DarkTeal,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Secure health data helps MedAssist AI provide personalized advice and medical awareness.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            /*
             * FORM SECTION CONTAINER
             */
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, Mint),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // NAME FIELD
                    ProfileLabel("Full Name")
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        placeholder = { Text("Enter your full name", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepTeal,
                            unfocusedBorderColor = Mint,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = PureWhite,
                            cursorColor = DeepTeal
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // GENDER FIELD
                    ProfileLabel("Gender")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Female", "Male", "Other").forEach { item ->
                            GenderButton(
                                text = item,
                                selected = gender.equals(item, ignoreCase = true),
                                onClick = { gender = item },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // DATE OF BIRTH FIELD
                    ProfileLabel("Date of Birth")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isSaving) { openDatePicker() }
                    ) {
                        OutlinedTextField(
                            value = dateOfBirth,
                            onValueChange = { },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            enabled = false,
                            placeholder = { Text("YYYY-MM-DD", color = TextMuted) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Select Date",
                                    tint = DeepTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = Mint,
                                disabledTextColor = TextDark,
                                disabledContainerColor = PureWhite,
                                disabledPlaceholderColor = TextMuted
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // BLOOD GROUP FIELD
                    ProfileLabel("Blood Group")
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val row1 = listOf("A+", "A-", "B+", "B-")
                        val row2 = listOf("O+", "O-", "AB+", "AB-")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row1.forEach { group ->
                                GenderButton(
                                    text = group,
                                    selected = bloodGroup.equals(group, ignoreCase = true),
                                    onClick = { bloodGroup = group },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row2.forEach { group ->
                                GenderButton(
                                    text = group,
                                    selected = bloodGroup.equals(group, ignoreCase = true),
                                    onClick = { bloodGroup = group },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // HEIGHT FIELD
                    ProfileLabel("Height")
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
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. 172", color = TextMuted) },
                        suffix = { Text("cm", color = DarkTeal, fontWeight = FontWeight.SemiBold) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { weightFocusRequester.requestFocus() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepTeal,
                            unfocusedBorderColor = Mint,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = PureWhite,
                            cursorColor = DeepTeal
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // WEIGHT FIELD
                    ProfileLabel("Weight")
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(weightFocusRequester),
                        placeholder = { Text("e.g. 68.5", color = TextMuted) },
                        suffix = { Text("kg", color = DarkTeal, fontWeight = FontWeight.SemiBold) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (!isSaving) {
                                    saveProfile(continueAfterSave = true)
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepTeal,
                            unfocusedBorderColor = Mint,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = PureWhite,
                            cursorColor = DeepTeal
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // BMI COMPUTATION PREVIEW
                    if (bmi != null) {
                        Spacer(modifier = Modifier.height(18.dp))
                        BmiCard(bmi = bmi!!)
                    }

                    // ERROR MESSAGE FEEDBACK
                    if (errorMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(StatusErrorBackground, RoundedCornerShape(12.dp))
                                .border(1.dp, StatusErrorBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = errorMessage,
                                color = StatusError,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // SUCCESS MESSAGE FEEDBACK
                    if (saveSuccessMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SoftMint, RoundedCornerShape(12.dp))
                                .border(1.dp, Mint, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = saveSuccessMessage,
                                color = StatusSuccess,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    /*
                     * SAVE BUTTON
                     */
                    Button(
                        onClick = { saveProfile(continueAfterSave = true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !isSaving,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeepTeal,
                            contentColor = PureWhite
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = PureWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Save & Update Profile",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    /*
                     * CANCEL / SKIP BUTTON
                     */
                    TextButton(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving
                    ) {
                        Text(
                            text = "Back to Profile",
                            color = TextMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ProfileLabel(text: String) {
    Text(
        text = text,
        color = DarkTeal,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 6.dp)
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
        modifier = modifier
            .height(46.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) DeepTeal else PureWhite,
        border = BorderStroke(
            1.dp,
            if (selected) DeepTeal else Mint
        ),
        shadowElevation = if (selected) 2.dp else 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (selected) PureWhite else TextDark,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun BmiCard(bmi: Double) {
    val category = when {
        bmi < 18.5 -> "Underweight"
        bmi < 25.0 -> "Normal"
        bmi < 30.0 -> "Overweight"
        else -> "Obesity"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SoftMint),
        border = BorderStroke(1.dp, Mint)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Calculated BMI",
                    color = TextMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = String.format(Locale.US, "%.1f", bmi),
                    color = DarkTeal,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = category,
                color = DeepTeal,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(PureWhite, RoundedCornerShape(8.dp))
                    .border(1.dp, Mint, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}