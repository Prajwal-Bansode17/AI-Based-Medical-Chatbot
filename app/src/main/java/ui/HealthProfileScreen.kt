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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import com.example.ai_based_medical_chatbot.LocalAppLanguageController
import com.example.ai_based_medical_chatbot.appText
import com.example.ai_based_medical_chatbot.data.HealthProfile
import com.example.ai_based_medical_chatbot.data.SupabaseClient
import com.example.ai_based_medical_chatbot.ui.theme.DarkTeal
import com.example.ai_based_medical_chatbot.ui.theme.DeepTeal
import com.example.ai_based_medical_chatbot.ui.theme.Ivory
import com.example.ai_based_medical_chatbot.ui.theme.Mint
import com.example.ai_based_medical_chatbot.ui.theme.PureWhite
import com.example.ai_based_medical_chatbot.ui.theme.SoftMint
import com.example.ai_based_medical_chatbot.ui.theme.StatusError
import com.example.ai_based_medical_chatbot.ui.theme.StatusErrorBackground
import com.example.ai_based_medical_chatbot.ui.theme.StatusErrorBorder
import com.example.ai_based_medical_chatbot.ui.theme.StatusSuccess
import com.example.ai_based_medical_chatbot.ui.theme.TextDark
import com.example.ai_based_medical_chatbot.ui.theme.TextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale

@Composable
fun HealthProfileScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val appLanguageController = LocalAppLanguageController.current
    val selectedLanguage = appLanguageController.selectedLanguage

    fun t(key: String): String = appText(key, selectedLanguage)

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
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val weightFocusRequester = remember { FocusRequester() }

    /*
     * LIVE BMI CALCULATION
     */
    fun recalculateBmi(hStr: String = height, wStr: String = weight) {
        val h = hStr.toDoubleOrNull()
        val w = wStr.toDoubleOrNull()
        if (h != null && w != null && h > 0 && w > 0) {
            val hMeters = h / 100.0
            bmi = w / (hMeters * hMeters)
        } else {
            bmi = null
        }
    }

    /*
     * CLOUD SYNC HELPER (BACKGROUND FIRE-AND-FORGET)
     */
    fun syncToCloudInBackground(
        overrideGender: String? = null,
        overrideBloodGroup: String? = null
    ) {
        val user = SupabaseClient.getSavedUser(context)
        if (user == null) {
            Log.w("HealthProfileScreen", "syncToCloudInBackground: User session is null (guest/unauthenticated). Skipping Supabase sync.")
            return
        }

        val effectiveGender = (overrideGender ?: gender).trim().takeIf { it.isNotBlank() }
        val effectiveBloodGroup = (overrideBloodGroup ?: bloodGroup).trim().takeIf { it.isNotBlank() }
        val cleanHeight = height.toDoubleOrNull()?.takeIf { it > 0 }
        val cleanWeight = weight.toDoubleOrNull()?.takeIf { it > 0 }
        val cleanBmi = bmi?.takeIf { it > 0 }

        val maskedId = if (user.id.length >= 8) "${user.id.take(8)}***" else "***"
        Log.d("HealthProfileScreen", "syncToCloudInBackground: Triggering remote sync for user $maskedId")

        scope.launch {
            val profile = HealthProfile(
                id = user.id,
                name = name.trim().takeIf { it.isNotBlank() },
                gender = effectiveGender,
                dateOfBirth = BMIRepository.toIsoDate(dateOfBirth).takeIf { it.isNotBlank() },
                bloodGroup = effectiveBloodGroup,
                heightCm = cleanHeight,
                weightKg = cleanWeight,
                bmi = cleanBmi
            )
            val result = SupabaseClient.saveHealthProfile(context = context, profile = profile)
            result.onSuccess {
                Log.d("HealthProfileScreen", "syncToCloudInBackground: Successfully synced profile to Supabase.")
            }.onFailure { error ->
                Log.e("HealthProfileScreen", "syncToCloudInBackground: Remote sync failed: ${error.message}")
            }
        }
    }

    /*
     * LOAD EXISTING PROFILE FROM LOCAL CENTRAL REPOSITORY FIRST,
     * THEN SUPPLEMENT FROM SUPABASE IF AVAILABLE
     */
    LaunchedEffect(Unit) {
        // 1. Instantly populate from local central repository (ensures offline persistence)
        val localData = BMIRepository.getBMIData(context)
        if (localData.fullName.isNotBlank()) name = localData.fullName
        if (localData.gender.isNotBlank()) gender = localData.gender
        if (localData.dateOfBirth.isNotBlank()) {
            dateOfBirth = BMIRepository.toDisplayDate(localData.dateOfBirth)
        }
        if (localData.bloodGroup.isNotBlank()) bloodGroup = localData.bloodGroup
        if (localData.heightCm > 0f) {
            height = BMIRepository.formatMeasurement(localData.heightCm)
        }
        if (localData.weightKg > 0f) {
            weight = BMIRepository.formatMeasurement(localData.weightKg)
        }
        if (localData.bmi > 0f) {
            bmi = localData.bmi.toDouble()
        }

        // 2. Fetch latest remote profile if user is authenticated with Supabase
        val result = SupabaseClient.getHealthProfile(context)
        result.onSuccess { profile ->
            if (profile != null) {
                if (profile.name?.isNotBlank() == true) name = profile.name
                if (profile.gender?.isNotBlank() == true) gender = profile.gender
                if (profile.dateOfBirth?.isNotBlank() == true) {
                    dateOfBirth = BMIRepository.toDisplayDate(profile.dateOfBirth)
                }
                if (profile.bloodGroup?.isNotBlank() == true) bloodGroup = profile.bloodGroup
                profile.heightCm?.let {
                    if (it > 0) height = BMIRepository.formatMeasurement(it)
                }
                profile.weightKg?.let {
                    if (it > 0) weight = BMIRepository.formatMeasurement(it)
                }
                if (profile.bmi != null && profile.bmi > 0) {
                    bmi = profile.bmi
                }

                // Sync remote values back to local repository so offline use is always current
                BMIRepository.saveBMIData(
                    context = context,
                    heightCm = height.toFloatOrNull() ?: localData.heightCm,
                    weightKg = weight.toFloatOrNull() ?: localData.weightKg,
                    dateOfBirth = dateOfBirth,
                    gender = gender,
                    bloodGroup = bloodGroup,
                    fullName = name
                )
            }
        }.onFailure {
            // Offline or guest mode is fine; local central data is already displayed
            errorMessage = ""
        }

        isLoading = false
    }

    /*
     * DEBOUNCED AUTO-SAVE FOR NAME (600ms)
     */
    LaunchedEffect(name) {
        if (!isLoading && name.isNotBlank()) {
            delay(600)
            BMIRepository.updateFullName(context, name)
            syncToCloudInBackground()
        }
    }

    /*
     * DEBOUNCED AUTO-SAVE FOR HEIGHT (600ms)
     */
    LaunchedEffect(height) {
        if (!isLoading) {
            recalculateBmi()
            val hVal = height.toFloatOrNull()
            if (hVal != null && hVal in 30f..280f) {
                delay(600)
                BMIRepository.updateHeight(context, hVal)
                syncToCloudInBackground()
            }
        }
    }

    /*
     * DEBOUNCED AUTO-SAVE FOR WEIGHT (600ms)
     */
    LaunchedEffect(weight) {
        if (!isLoading) {
            recalculateBmi()
            val wVal = weight.toFloatOrNull()
            if (wVal != null && wVal in 2f..350f) {
                delay(600)
                BMIRepository.updateWeight(context, wVal)
                syncToCloudInBackground()
            }
        }
    }

    /*
     * DATE SELECTION HANDLER USING SHARED CALENDAR
     */
    if (showDatePickerDialog) {
        val initialParsedDate: Date? = if (dateOfBirth.isNotBlank()) {
            BMIRepository.parseDateOfBirth(dateOfBirth)
        } else null

        MedAssistDatePickerDialog(
            initialDate = initialParsedDate,
            title = t("health_profile_select_dob"),
            allowFutureDates = false,
            onDateSelected = { day, month, year ->
                val newDob = String.format(Locale.US, "%02d/%02d/%04d", day, month, year)
                dateOfBirth = newDob
                showDatePickerDialog = false
                BMIRepository.updateDateOfBirth(context, newDob)
                syncToCloudInBackground()
                saveSuccessMessage = t("health_profile_dob_updated")
            },
            onDismiss = {
                showDatePickerDialog = false
            },
            t = ::t
        )
    }

    /*
     * EXPLICIT MANUAL SAVE/CONFIRM BUTTON
     */
    fun saveProfile(continueAfterSave: Boolean) {
        scope.launch {
            isSaving = true
            errorMessage = ""
            saveSuccessMessage = ""

            recalculateBmi()

            val cleanHeight = height.toDoubleOrNull()?.takeIf { it > 0 }
            val cleanWeight = weight.toDoubleOrNull()?.takeIf { it > 0 }
            val cleanBmi = bmi?.takeIf { it > 0 }

            // 1. Immediately persist locally to BMIRepository (Single source of truth)
            BMIRepository.saveBMIData(
                context = context,
                heightCm = cleanHeight?.toFloat() ?: 0f,
                weightKg = cleanWeight?.toFloat() ?: 0f,
                dateOfBirth = dateOfBirth,
                gender = gender,
                bloodGroup = bloodGroup,
                fullName = name
            )

            // 2. Persist to remote Supabase if logged in
            val user = SupabaseClient.getSavedUser(context)
            if (user != null) {
                val maskedId = if (user.id.length >= 8) "${user.id.take(8)}***" else "***"
                Log.d("HealthProfileScreen", "saveProfile: Saving profile to Supabase for user $maskedId")
                val profile = HealthProfile(
                    id = user.id,
                    name = name.trim().takeIf { it.isNotBlank() },
                    gender = gender.trim().takeIf { it.isNotBlank() },
                    dateOfBirth = BMIRepository.toIsoDate(dateOfBirth).takeIf { it.isNotBlank() },
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
                    saveSuccessMessage = t("health_profile_updated")
                    Log.d("HealthProfileScreen", "saveProfile: Supabase save succeeded")
                    if (continueAfterSave) {
                        onContinue()
                    }
                }.onFailure { error ->
                    // Local data is saved, but make the cloud-sync failure visible.
                    isSaving = false
                    errorMessage = error.message ?: t("health_profile_saved_locally")
                    saveSuccessMessage = t("health_profile_saved_locally")
                    Log.e("HealthProfileScreen", "saveProfile: Supabase save failed: ${error.message}")
                    if (continueAfterSave) {
                        onContinue()
                    }
                }
            } else {
                // Guest / Local mode
                isSaving = false
                saveSuccessMessage = t("health_profile_updated")
                Log.w("HealthProfileScreen", "saveProfile: User is in guest mode / not logged in to Supabase. Saved locally only.")
                if (continueAfterSave) {
                    onContinue()
                }
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
                        text = t("my_health_profile"),
                        color = DarkTeal,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = t("health_profile_subtitle"),
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
                            text = t("health_info_title"),
                            color = DarkTeal,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = t("health_info_desc"),
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
                    ProfileLabel(t("full_name"))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        placeholder = { Text(t("health_profile_name_placeholder"), color = TextMuted) },
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
                                onClick = {
                                    gender = item
                                    BMIRepository.updateGender(context, item)
                                    syncToCloudInBackground(overrideGender = item)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // DATE OF BIRTH FIELD
                    ProfileLabel(t("date_of_birth"))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isSaving) { showDatePickerDialog = true }
                    ) {
                        OutlinedTextField(
                            value = dateOfBirth,
                            onValueChange = { },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            enabled = false,
                            placeholder = { Text("DD/MM/YYYY", color = TextMuted) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = t("health_profile_select_dob"),
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
                    ProfileLabel(t("blood_group"))
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
                                    onClick = {
                                        bloodGroup = group
                                        BMIRepository.updateBloodGroup(context, group)
                                        syncToCloudInBackground(overrideBloodGroup = group)
                                    },
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
                                    onClick = {
                                        bloodGroup = group
                                        BMIRepository.updateBloodGroup(context, group)
                                        syncToCloudInBackground(overrideBloodGroup = group)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // HEIGHT FIELD
                    ProfileLabel(t("height"))
                    OutlinedTextField(
                        value = height,
                        onValueChange = {
                            val filtered = it
                                .filter { char -> char.isDigit() || char == '.' }
                                .let { value ->
                                    val firstDot = value.indexOf('.')
                                    if (firstDot >= 0) {
                                        val intPart = value.substring(0, firstDot + 1)
                                        val decPart = value.substring(firstDot + 1).replace(".", "").take(2)
                                        intPart + decPart
                                    } else {
                                        value
                                    }
                                }
                                .take(6)

                            height = filtered
                            recalculateBmi(hStr = filtered)
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
                    ProfileLabel(t("weight"))
                    OutlinedTextField(
                        value = weight,
                        onValueChange = {
                            val filtered = it
                                .filter { char -> char.isDigit() || char == '.' }
                                .let { value ->
                                    val firstDot = value.indexOf('.')
                                    if (firstDot >= 0) {
                                        val intPart = value.substring(0, firstDot + 1)
                                        val decPart = value.substring(firstDot + 1).replace(".", "").take(2)
                                        intPart + decPart
                                    } else {
                                        value
                                    }
                                }
                                .take(6)

                            weight = filtered
                            recalculateBmi(wStr = filtered)
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
                                    saveProfile(continueAfterSave = false)
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
                        onClick = { saveProfile(continueAfterSave = false) },
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
                                text = t("health_profile_save"),
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
                            text = t("health_profile_back"),
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
    val selectedLanguage = LocalAppLanguageController.current.selectedLanguage
    val category = when {
        bmi < 18.5 -> appText("bmi_underweight", selectedLanguage)
        bmi < 25.0 -> appText("bmi_normal", selectedLanguage)
        bmi < 30.0 -> appText("bmi_overweight", selectedLanguage)
        else -> appText("bmi_obese", selectedLanguage)
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
                    text = appText("bmi_calculated_label", selectedLanguage),
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
