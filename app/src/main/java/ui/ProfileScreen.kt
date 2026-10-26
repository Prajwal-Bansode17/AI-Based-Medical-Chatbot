package ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.example.ai_based_medical_chatbot.ChatMemoryItem
import com.example.ai_based_medical_chatbot.ChatMemoryRepository
import com.example.ai_based_medical_chatbot.MemoryCategory
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ai_based_medical_chatbot.LocalAppLanguageController
import com.example.ai_based_medical_chatbot.MedicineReminder
import com.example.ai_based_medical_chatbot.MedicineReminderRepository
import com.example.ai_based_medical_chatbot.appText
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
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    userName: String,
    userEmail: String,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onHealthProfileClick: () -> Unit = {},
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // -------------------------------------------------------------
    // App Language
    // -------------------------------------------------------------
    val appLanguage = LocalAppLanguageController.current
    val selectedLanguage = appLanguage.selectedLanguage

    val passwordText: (String) -> String = { key -> appText(key, selectedLanguage) }

    var languageMenuExpanded by remember { mutableStateOf(false) }
    val supportedLanguages = listOf("English", "मराठी", "हिंदी")

    fun saveLanguage(language: String) {
        appLanguage.setLanguage(language)
        languageMenuExpanded = false
    }

    // -------------------------------------------------------------
    // Password state
    // -------------------------------------------------------------
    var passwordVisible by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var changePasswordLoading by remember { mutableStateOf(false) }
    var changePasswordError by remember { mutableStateOf("") }

    val currentPasswordFocusRequester = remember { FocusRequester() }
    val newPasswordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }

    // -------------------------------------------------------------
    // Data states: BMI, Prescriptions, Reminders
    // -------------------------------------------------------------
    var bmiData by remember { mutableStateOf(BMIRepository.getBMIData(context)) }
    var savedPrescriptions by remember {
        mutableStateOf(PrescriptionRepository.getSavedPrescriptions(context).toString())
    }
    var selectedPrescriptionId by remember { mutableStateOf<String?>(null) }
    var savedReminders by remember { mutableStateOf(MedicineReminderRepository.getAll(context)) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    fun refreshPrescriptions() {
        savedPrescriptions = PrescriptionRepository.getSavedPrescriptions(context).toString()
    }

    val currentUser = SupabaseClient.getSavedUser(context)
    val isGuest = currentUser == null
    val memoryUserId = currentUser?.id.orEmpty()
    var memoryConsentEnabled by remember {
        mutableStateOf(if (!isGuest && memoryUserId.isNotBlank()) ChatMemoryRepository.isMemoryConsentEnabled(context, memoryUserId) else false)
    }
    var savedMemories by remember {
        mutableStateOf<List<ChatMemoryItem>>(
            if (!isGuest && memoryUserId.isNotBlank()) ChatMemoryRepository.getMemories(context, memoryUserId) else emptyList()
        )
    }

    LaunchedEffect(Unit) {
        refreshPrescriptions()
        savedReminders = MedicineReminderRepository.getAll(context)
        bmiData = BMIRepository.getBMIData(context)
        if (!isGuest && memoryUserId.isNotBlank()) {
            memoryConsentEnabled = ChatMemoryRepository.syncConsentFromCloud(context, memoryUserId)
            savedMemories = if (memoryConsentEnabled) {
                ChatMemoryRepository.syncMemoriesFromCloud(context, memoryUserId)
            } else {
                ChatMemoryRepository.getMemories(context, memoryUserId)
            }
        }
    }

    // -------------------------------------------------------------
    // Expandable Section Toggles (Saved via rememberSaveable)
    // -------------------------------------------------------------
    var accountExpanded by rememberSaveable { mutableStateOf(true) }
    var healthSummaryExpanded by rememberSaveable { mutableStateOf(true) }
    var remindersExpanded by rememberSaveable { mutableStateOf(true) }
    var prescriptionsExpanded by rememberSaveable { mutableStateOf(true) }
    var memoryExpanded by rememberSaveable { mutableStateOf(false) }

    // User values
    val displayName = userName.trim().ifBlank { "MedAssist User" }
    val displayEmail = userEmail.trim().ifBlank { "Email not available" }

    // BMI computed values
    val hasBmiData = bmiData.bmi > 0f || bmiData.heightCm > 0f || bmiData.weightKg > 0f ||
            bmiData.dateOfBirth.isNotBlank() || bmiData.gender.isNotBlank()
    val bmiValue = if (bmiData.bmi > 0f) {
        String.format(Locale.getDefault(), "%.1f", bmiData.bmi)
    } else {
        "Not Available"
    }
    val ageValue = if (bmiData.age > 0) "${bmiData.age} years" else "Not Available"
    val heightValue = if (bmiData.heightCm > 0f) {
        String.format(Locale.getDefault(), "%.0f cm", bmiData.heightCm)
    } else {
        "Not Available"
    }
    val weightValue = if (bmiData.weightKg > 0f) {
        String.format(Locale.getDefault(), "%.1f kg", bmiData.weightKg)
    } else {
        "Not Available"
    }
    val dobValue = bmiData.dateOfBirth.ifBlank { "Not Available" }
    val genderValue = bmiData.gender.ifBlank { "Not Available" }
    val bmiStatusValue = bmiData.bmiStatus

    val prescriptionArray = try {
        JSONArray(savedPrescriptions)
    } catch (_: Exception) {
        JSONArray()
    }

    // -------------------------------------------------------------
    // Root Layout
    // -------------------------------------------------------------
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
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // =========================================================
            // COMPACT TOP BAR
            // =========================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
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
                        text = appText("my_profile", selectedLanguage),
                        color = DarkTeal,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Account & Health Overview",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // =========================================================
            // 1. PROFILE HEADER CARD
            // =========================================================
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
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Modern Clean Avatar
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .shadow(4.dp, CircleShape)
                            .background(
                                Brush.linearGradient(listOf(DarkTeal, DeepTeal)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = displayName
                            .split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
                            .map { it.first().uppercase() }
                            .joinToString("")
                            .ifBlank { "M" }

                        Text(
                            text = initials,
                            color = PureWhite,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = displayName,
                        color = TextDark,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = displayEmail,
                        color = TextMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Verified Secure Account Badge
                    Row(
                        modifier = Modifier
                            .background(SoftMint, RoundedCornerShape(50.dp))
                            .border(1.dp, Mint, RoundedCornerShape(50.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Secure",
                            tint = DeepTeal,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = appText("secure_account", selectedLanguage),
                            color = DeepTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================
            // 2. MY HEALTH PROFILE ENTRY CARD
            // =========================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHealthProfileClick() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SoftMint),
                border = BorderStroke(1.dp, Mint),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(PureWhite, RoundedCornerShape(12.dp))
                            .border(1.dp, Mint, RoundedCornerShape(12.dp)),
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
                            text = "My Health Profile",
                            color = DarkTeal,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "View & update comprehensive medical data",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Text(
                        text = "›",
                        color = DeepTeal,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // =========================================================
            // 3. ACCOUNT & PREFERENCES (EXPANDABLE)
            // =========================================================
            SectionHeaderWithToggle(
                title = appText("account", selectedLanguage),
                isExpanded = accountExpanded,
                onToggle = { accountExpanded = !accountExpanded }
            )

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = accountExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = BorderStroke(1.dp, Mint),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Profile Info Row
                        ProfileInfoRow(
                            icon = Icons.Default.Person,
                            title = appText("profile", selectedLanguage),
                            subtitle = appText("your_account", selectedLanguage),
                            iconColor = DeepTeal
                        )

                        ProfileDivider()

                        // Email Info Row
                        ProfileInfoRow(
                            icon = Icons.Default.Email,
                            title = appText("email", selectedLanguage),
                            subtitle = displayEmail,
                            iconColor = SageGreen
                        )

                        ProfileDivider()

                        // Password Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(SoftMint, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = appText("password", selectedLanguage),
                                    tint = DeepTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = appText("password", selectedLanguage),
                                    color = TextDark,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (passwordVisible) {
                                        appText("password_protected", selectedLanguage)
                                    } else {
                                        "••••••••"
                                    },
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }

                            IconButton(
                                onClick = { passwordVisible = !passwordVisible }
                            ) {
                                Text(
                                    text = if (passwordVisible) "◉" else "◌",
                                    color = DeepTeal,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Change Password Action Button
                        TextButton(
                            onClick = {
                                currentPassword = ""
                                newPassword = ""
                                confirmNewPassword = ""
                                changePasswordError = ""
                                showChangePasswordDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                        ) {
                            Text(
                                text = passwordText("change_password"),
                                color = DeepTeal,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        ProfileDivider()

                        // Language Preference Picker
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { languageMenuExpanded = true }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(SoftMint, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Language",
                                        tint = DeepTeal,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = appText("app_language", selectedLanguage),
                                        color = TextDark,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = selectedLanguage,
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select Language",
                                    tint = DeepTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = languageMenuExpanded,
                                onDismissRequest = { languageMenuExpanded = false },
                                modifier = Modifier
                                    .background(PureWhite)
                                    .border(1.dp, Mint, RoundedCornerShape(12.dp))
                            ) {
                                supportedLanguages.forEach { language ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (language == selectedLanguage) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = DeepTeal,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                } else {
                                                    Spacer(modifier = Modifier.width(22.dp))
                                                }
                                                Text(
                                                    text = language,
                                                    color = if (language == selectedLanguage) DarkTeal else TextDark,
                                                    fontSize = 14.sp,
                                                    fontWeight = if (language == selectedLanguage) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        },
                                        onClick = { saveLanguage(language) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================
            // 4. HEALTH SUMMARY & BMI (Rendered only when saved data exists)
            // =========================================================
            if (hasBmiData) {
                Spacer(modifier = Modifier.height(20.dp))

                SectionHeaderWithToggle(
                    title = appText("my_health", selectedLanguage),
                    isExpanded = healthSummaryExpanded,
                    onToggle = { healthSummaryExpanded = !healthSummaryExpanded }
                )

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedVisibility(
                    visible = healthSummaryExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, Mint),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            // BMI Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(SoftMint, RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "BMI",
                                        tint = DeepTeal,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = appText("bmi_information", selectedLanguage),
                                        color = TextDark,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = appText("saved_body", selectedLanguage),
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Current BMI Value Card
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SoftMint, RoundedCornerShape(16.dp))
                                    .border(1.dp, Mint, RoundedCornerShape(16.dp))
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = appText("current_bmi", selectedLanguage),
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = bmiValue,
                                        color = DarkTeal,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = appText("status", selectedLanguage),
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = bmiStatusValue,
                                        color = DeepTeal,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Details
                            HealthDataRow(title = appText("dob", selectedLanguage), value = dobValue)
                            HealthDataDivider()
                            HealthDataRow(title = appText("age", selectedLanguage), value = ageValue)
                            HealthDataDivider()
                            HealthDataRow(title = appText("gender", selectedLanguage), value = genderValue)
                            HealthDataDivider()
                            HealthDataRow(title = appText("height", selectedLanguage), value = heightValue)
                            HealthDataDivider()
                            HealthDataRow(title = appText("weight", selectedLanguage), value = weightValue)
                        }
                    }
                }
            }

            // =========================================================
            // 5. MY MEDICINE REMINDERS (EXPANDABLE)
            // =========================================================
            Spacer(modifier = Modifier.height(20.dp))

            SectionHeaderWithToggle(
                title = "${appText("my_reminders", selectedLanguage)} (${savedReminders.size})",
                isExpanded = remindersExpanded,
                onToggle = { remindersExpanded = !remindersExpanded }
            )

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = remindersExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (savedReminders.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            border = BorderStroke(1.dp, Mint),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = DeepTeal,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "No medicine reminders",
                                        color = TextDark,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = appText("reminders_here", selectedLanguage),
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    } else {
                        val groupedReminders = savedReminders
                            .sortedByDescending { it.createdAt }
                            .groupBy { profileReminderDayKey(it.createdAt) }

                        groupedReminders.forEach { (dayKey, dayReminders) ->
                            Text(
                                text = profileReminderDayLabel(dayKey),
                                color = DarkTeal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
                            )

                            dayReminders.forEach { reminder ->
                                ReminderCard(
                                    reminder = reminder,
                                    selectedLanguage = selectedLanguage
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================
            // 6. MY PRESCRIPTIONS (EXPANDABLE)
            // =========================================================
            Spacer(modifier = Modifier.height(20.dp))

            SectionHeaderWithToggle(
                title = "${appText("my_prescriptions", selectedLanguage)} (${prescriptionArray.length()})",
                isExpanded = prescriptionsExpanded,
                onToggle = { prescriptionsExpanded = !prescriptionsExpanded }
            )

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = prescriptionsExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (prescriptionArray.length() == 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            border = BorderStroke(1.dp, Mint),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = DocumentScannerVectorIcon,
                                    contentDescription = null,
                                    tint = DeepTeal,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = appText("no_prescriptions", selectedLanguage),
                                    color = TextDark,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Your scanned prescriptions will appear here after you save them.",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        for (i in 0 until prescriptionArray.length()) {
                            val prescription = prescriptionArray.optJSONObject(i) ?: continue
                            val prescriptionId = prescription.optString("id")
                            val doctor = prescription.optString("doctorName")
                            val patient = prescription.optString("patientName")
                            val date = prescription.optString("date")
                            val diagnosisText = prescription.optString("diagnosis")
                            val hospitalText = prescription.optString("hospitalName")
                            val medicines = prescription.optJSONArray("medicines") ?: JSONArray()
                            val medicineCount = medicines.length()

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                                    .clickable { selectedPrescriptionId = prescriptionId },
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
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .background(SoftMint, RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = DocumentScannerVectorIcon,
                                                contentDescription = null,
                                                tint = DeepTeal,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (doctor.isNotBlank()) "Dr. $doctor" else "Saved Prescription",
                                                color = TextDark,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = buildString {
                                                    if (date.isNotBlank()) append(date)
                                                    if (medicineCount > 0) {
                                                        if (isNotEmpty()) append("  •  ")
                                                        append("$medicineCount medicine")
                                                        if (medicineCount != 1) append("s")
                                                    }
                                                    if (isEmpty()) append("Prescription details")
                                                },
                                                color = TextMuted,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Text(
                                            text = "›",
                                            color = DeepTeal,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (hospitalText.isNotBlank() || patient.isNotBlank() || diagnosisText.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        if (hospitalText.isNotBlank()) {
                                            Text(
                                                text = "Hospital: $hospitalText",
                                                color = TextMuted,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (patient.isNotBlank()) {
                                            Text(
                                                text = "Patient: $patient",
                                                color = TextMuted,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (diagnosisText.isNotBlank()) {
                                            Text(
                                                text = "Diagnosis: $diagnosisText",
                                                color = TextMuted,
                                                fontSize = 11.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================
            // PRESCRIPTION DETAIL CARD (WHEN SELECTED)
            // =========================================================
            selectedPrescriptionId?.let { selectedId ->
                val selectedPrescription = (0 until prescriptionArray.length())
                    .mapNotNull { prescriptionArray.optJSONObject(it) }
                    .firstOrNull { it.optString("id") == selectedId }

                if (selectedPrescription != null) {
                    val selectedMedicines = selectedPrescription.optJSONArray("medicines") ?: JSONArray()

                    fun shareSavedPrescription() {
                        val shareLines = mutableListOf<String>()
                        shareLines += "MEDASSIST AI - Prescription"

                        val shareHospital = selectedPrescription.optString("hospitalName")
                        val shareDoctor = selectedPrescription.optString("doctorName")
                        val sharePatient = selectedPrescription.optString("patientName")
                        val shareDate = selectedPrescription.optString("date")
                        val shareDiagnosis = selectedPrescription.optString("diagnosis")

                        if (shareHospital.isNotBlank()) shareLines += "Hospital / Clinic: $shareHospital"
                        if (shareDoctor.isNotBlank()) shareLines += "Doctor: $shareDoctor"
                        if (sharePatient.isNotBlank()) shareLines += "Patient: $sharePatient"
                        if (shareDate.isNotBlank()) shareLines += "Date: $shareDate"
                        if (shareDiagnosis.isNotBlank()) shareLines += "Diagnosis: $shareDiagnosis"

                        for (k in 0 until selectedMedicines.length()) {
                            val medicine = selectedMedicines.optJSONObject(k) ?: continue
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

                        val imageUri = selectedPrescription.optString("imageUri")
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_SUBJECT, "MEDASSIST Prescription")
                            putExtra(Intent.EXTRA_TEXT, shareLines.joinToString("\n"))

                            if (imageUri.isNotBlank()) {
                                type = "image/*"
                                putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(imageUri))
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            } else {
                                type = "text/plain"
                            }
                        }

                        context.startActivity(Intent.createChooser(intent, appText("share_prescription", selectedLanguage)))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.5.dp, DeepTeal),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = DocumentScannerVectorIcon,
                                        contentDescription = null,
                                        tint = DarkTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = appText("prescription_details", selectedLanguage),
                                            color = DarkTeal,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Saved digital record",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { selectedPrescriptionId = null },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val detailDoctor = selectedPrescription.optString("doctorName")
                            val detailPatient = selectedPrescription.optString("patientName")
                            val detailDate = selectedPrescription.optString("date")
                            val detailDiagnosis = selectedPrescription.optString("diagnosis")
                            val detailHospital = selectedPrescription.optString("hospitalName")

                            if (detailHospital.isNotBlank()) {
                                HealthDataRow(title = "Hospital / Clinic", value = detailHospital)
                                HealthDataDivider()
                            }
                            if (detailDoctor.isNotBlank()) {
                                HealthDataRow(title = "Doctor", value = detailDoctor)
                                HealthDataDivider()
                            }
                            if (detailPatient.isNotBlank()) {
                                HealthDataRow(title = "Patient", value = detailPatient)
                                HealthDataDivider()
                            }
                            if (detailDate.isNotBlank()) {
                                HealthDataRow(title = "Date", value = detailDate)
                                HealthDataDivider()
                            }
                            if (detailDiagnosis.isNotBlank()) {
                                HealthDataRow(title = "Diagnosis", value = detailDiagnosis)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = PillVectorIcon,
                                    contentDescription = null,
                                    tint = DarkTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Medicines (${selectedMedicines.length()})",
                                    color = DarkTeal,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (selectedMedicines.length() == 0) {
                                Text(
                                    text = "No medicine details saved.",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            } else {
                                for (j in 0 until selectedMedicines.length()) {
                                    val medicine = selectedMedicines.optJSONObject(j) ?: JSONObject()
                                    val name = medicine.optString("medicineName")
                                    val strength = medicine.optString("strength")
                                    val frequency = medicine.optString("frequency")
                                    val duration = medicine.optString("duration")
                                    val instructions = medicine.optString("instructions")
                                    val timing = medicine.optString("timing")
                                    val mealTiming = medicine.optString("mealTiming")
                                    val howToTake = medicine.optString("howToTake")

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = SoftMint),
                                        border = BorderStroke(1.dp, Mint)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                text = "${j + 1}. ${name.ifBlank { appText("medicine", selectedLanguage) }}",
                                                color = TextDark,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (strength.isNotBlank()) {
                                                Text(
                                                    text = "Strength: $strength",
                                                    color = TextMuted,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            if (timing.isNotBlank()) {
                                                Text(
                                                    text = "Time: $timing",
                                                    color = TextMuted,
                                                    fontSize = 11.sp
                                                )
                                            } else if (frequency.isNotBlank()) {
                                                Text(
                                                    text = "Schedule: $frequency",
                                                    color = TextMuted,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            if (mealTiming.isNotBlank()) {
                                                Text(
                                                    text = "Meal: $mealTiming",
                                                    color = TextMuted,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            if (duration.isNotBlank()) {
                                                Text(
                                                    text = "Duration: $duration",
                                                    color = TextMuted,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            if (howToTake.isNotBlank()) {
                                                Text(
                                                    text = "How to take: $howToTake",
                                                    color = TextMuted,
                                                    fontSize = 11.sp
                                                )
                                            } else if (instructions.isNotBlank()) {
                                                Text(
                                                    text = "Instructions: $instructions",
                                                    color = TextMuted,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Button(
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
                                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = DeepTeal)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Notifications,
                                                    contentDescription = null,
                                                    tint = PureWhite,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = appText("set_reminder", selectedLanguage),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PureWhite
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { shareSavedPrescription() },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkTeal)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = appText("share_prescription", selectedLanguage),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StatusErrorBackground,
                                    contentColor = StatusError
                                ),
                                border = BorderStroke(1.dp, StatusErrorBorder)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = StatusError,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = appText("delete_prescription", selectedLanguage),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusError
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================
            // 7. AI CHATBOT LONG-TERM MEMORY (EXPANDABLE)
            // =========================================================
            Spacer(modifier = Modifier.height(20.dp))

            SectionHeaderWithToggle(
                title = "${appText("memory_title", selectedLanguage)} (${if (isGuest) 0 else savedMemories.size})",
                isExpanded = memoryExpanded,
                onToggle = {
                    memoryExpanded = !memoryExpanded
                    if (!isGuest && memoryUserId.isNotBlank()) {
                        savedMemories = ChatMemoryRepository.getMemories(context, memoryUserId)
                        scope.launch {
                            savedMemories = ChatMemoryRepository.syncMemoriesFromCloud(context, memoryUserId)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = memoryExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (isGuest) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            border = BorderStroke(1.dp, Mint),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = DeepTeal,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = appText("memory_guest_notice", selectedLanguage),
                                    color = TextMuted,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // Consent Switch Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            border = BorderStroke(1.dp, Mint),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = appText("memory_consent_label", selectedLanguage),
                                            color = TextDark,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = appText("memory_consent_desc", selectedLanguage),
                                            color = TextMuted,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Switch(
                                        checked = memoryConsentEnabled,
                                        onCheckedChange = { isChecked ->
                                            memoryConsentEnabled = isChecked
                                            scope.launch {
                                                ChatMemoryRepository.setConsentWithCloudSync(context, memoryUserId, isChecked)
                                                savedMemories = ChatMemoryRepository.getMemories(context, memoryUserId)
                                            }
                                            Toast.makeText(
                                                context,
                                                if (isChecked) "Personalized memory enabled" else "Personalized memory disabled",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = PureWhite,
                                            checkedTrackColor = DeepTeal,
                                            uncheckedThumbColor = PureWhite,
                                            uncheckedTrackColor = Mint
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Memory List or Empty State
                        if (savedMemories.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = PureWhite),
                                border = BorderStroke(1.dp, Mint),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = DeepTeal,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = appText("memory_no_items", selectedLanguage),
                                        color = TextDark,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = appText("memory_subtitle", selectedLanguage),
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            savedMemories.forEach { memoryItem ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                                    border = BorderStroke(1.dp, Mint),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Box(
                                                modifier = Modifier
                                                    .background(SoftMint, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = memoryItem.category.name,
                                                    color = DeepTeal,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = memoryItem.content,
                                                color = TextDark,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    ChatMemoryRepository.deleteMemoryWithCloudSync(context, memoryUserId, memoryItem.id)
                                                    savedMemories = ChatMemoryRepository.getMemories(context, memoryUserId)
                                                }
                                                Toast.makeText(
                                                    context,
                                                    appText("memory_deleted", selectedLanguage),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete memory",
                                                tint = StatusError,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        ChatMemoryRepository.clearAllMemoriesWithCloudSync(context, memoryUserId)
                                        savedMemories = emptyList()
                                    }
                                    Toast.makeText(
                                        context,
                                        appText("memory_all_cleared", selectedLanguage),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, StatusErrorBorder),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = StatusError
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = StatusError,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = appText("memory_clear_all", selectedLanguage),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================
            // 8. SIGN OUT (REDESIGNED LUXURY CARD)
            // =========================================================
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLogoutClick() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = StatusErrorBackground),
                border = BorderStroke(1.dp, StatusErrorBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(StatusErrorBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "↪",
                            color = StatusError,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = appText("sign_out", selectedLanguage),
                            color = StatusError,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Sign out safely from this device",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Text(
                        text = "›",
                        color = StatusError,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Footer
            Text(
                text = "MEDASSIST AI  •  Secure • Simple • Intelligent",
                modifier = Modifier.fillMaxWidth(),
                color = TextMuted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // =============================================================
    // EMBEDDED DIALOG: DELETE PRESCRIPTION
    // =============================================================
    if (showDeleteDialog && selectedPrescriptionId != null) {
        Dialog(
            onDismissRequest = { showDeleteDialog = false }
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, Mint),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(StatusErrorBackground, CircleShape)
                            .border(1.dp, StatusErrorBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = StatusError,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Delete Prescription?",
                        color = TextDark,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "This saved prescription will be permanently removed from your device.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showDeleteDialog = false },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SoftMint),
                            border = BorderStroke(1.dp, Mint)
                        ) {
                            Text(
                                text = "Cancel",
                                color = TextDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                selectedPrescriptionId?.let { id ->
                                    PrescriptionRepository.delete(context, id)
                                }
                                showDeleteDialog = false
                                selectedPrescriptionId = null
                                refreshPrescriptions()
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                        ) {
                            Text(
                                text = "Delete",
                                color = PureWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // =============================================================
    // EMBEDDED DIALOG: CHANGE PASSWORD
    // =============================================================
    if (showChangePasswordDialog) {
        val changePasswordScrollState = rememberScrollState()

        val submitPasswordChange: () -> Unit = {
            if (!changePasswordLoading) {
                when {
                    currentPassword.isBlank() -> changePasswordError = passwordText("current_password")
                    newPassword.length < 6 -> changePasswordError = passwordText("password_minimum")
                    newPassword != confirmNewPassword -> changePasswordError = passwordText("password_mismatch")
                    else -> {
                        changePasswordLoading = true
                        changePasswordError = ""

                        scope.launch {
                            val result = SupabaseClient.changePassword(
                                context = context,
                                email = displayEmail,
                                currentPassword = currentPassword,
                                newPassword = newPassword
                            )

                            changePasswordLoading = false

                            result.onSuccess {
                                currentPassword = ""
                                newPassword = ""
                                confirmNewPassword = ""
                                changePasswordError = ""
                                showChangePasswordDialog = false

                                Toast.makeText(
                                    context,
                                    passwordText("password_changed"),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            result.onFailure { error ->
                                changePasswordError = error.message
                                    ?: "Unable to change password. Please try again."
                            }
                        }
                    }
                }
            }
        }

        Dialog(
            onDismissRequest = {
                if (!changePasswordLoading) {
                    showChangePasswordDialog = false
                    changePasswordError = ""
                }
            }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .imePadding(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, Mint),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(changePasswordScrollState)
                        .padding(horizontal = 18.dp, vertical = 18.dp)
                ) {
                    // Dialog Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(SoftMint, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = passwordText("change_password"),
                                tint = DeepTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = passwordText("change_password"),
                                color = DarkTeal,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = passwordText("change_password_subtitle"),
                                color = TextMuted,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Email badge
                    Text(
                        text = displayEmail,
                        color = DarkTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SoftMint, RoundedCornerShape(10.dp))
                            .border(1.dp, Mint, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Current Password
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = {
                            currentPassword = it
                            changePasswordError = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .focusRequester(currentPasswordFocusRequester),
                        label = {
                            Text(passwordText("current_password"), fontSize = 12.sp)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = DeepTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        enabled = !changePasswordLoading,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { newPasswordFocusRequester.requestFocus() }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepTeal,
                            unfocusedBorderColor = Mint,
                            focusedLabelColor = DeepTeal,
                            unfocusedLabelColor = TextMuted,
                            cursorColor = DeepTeal,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = PureWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // New Password
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            changePasswordError = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .focusRequester(newPasswordFocusRequester),
                        label = {
                            Text(passwordText("new_password"), fontSize = 12.sp)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = DeepTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        enabled = !changePasswordLoading,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { confirmPasswordFocusRequester.requestFocus() }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepTeal,
                            unfocusedBorderColor = Mint,
                            focusedLabelColor = DeepTeal,
                            unfocusedLabelColor = TextMuted,
                            cursorColor = DeepTeal,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = PureWhite
                        )
                    )

                    Text(
                        text = passwordText("password_minimum"),
                        color = TextMuted,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Confirm Password
                    OutlinedTextField(
                        value = confirmNewPassword,
                        onValueChange = {
                            confirmNewPassword = it
                            changePasswordError = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .focusRequester(confirmPasswordFocusRequester),
                        label = {
                            Text(passwordText("confirm_password"), fontSize = 12.sp)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = DeepTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        enabled = !changePasswordLoading,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submitPasswordChange() }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepTeal,
                            unfocusedBorderColor = Mint,
                            focusedLabelColor = DeepTeal,
                            unfocusedLabelColor = TextMuted,
                            cursorColor = DeepTeal,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = PureWhite
                        )
                    )

                    if (changePasswordError.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = changePasswordError,
                            color = StatusError,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dialog Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                if (!changePasswordLoading) {
                                    showChangePasswordDialog = false
                                    changePasswordError = ""
                                }
                            },
                            enabled = !changePasswordLoading
                        ) {
                            Text(
                                text = passwordText("cancel"),
                                color = TextMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = { submitPasswordChange() },
                            enabled = !changePasswordLoading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepTeal,
                                contentColor = PureWhite
                            ),
                            modifier = Modifier.height(44.dp)
                        ) {
                            if (changePasswordLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = PureWhite,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = passwordText("update_password"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================
// SUB-COMPONENTS & HELPERS
// =============================================================

@Composable
private fun SectionHeaderWithToggle(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "expandRotation")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = DarkTeal,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = DeepTeal,
            modifier = Modifier
                .size(22.dp)
                .rotate(rotation)
        )
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(SoftMint, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: MedicineReminder,
    selectedLanguage: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, Mint),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(SoftMint, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = PillVectorIcon,
                        contentDescription = null,
                        tint = DeepTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.medicineName.ifBlank { appText("medicine", selectedLanguage) },
                        color = TextDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (reminder.strength.isNotBlank()) {
                        Text(
                            text = reminder.strength,
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Text(
                    text = if (reminder.enabled) "ACTIVE" else "PAUSED",
                    color = if (reminder.enabled) StatusSuccess else TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            if (reminder.enabled) SoftMint else Ivory,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${reminder.time.ifBlank { appText("time_not_set", selectedLanguage) }} • ${reminder.daySlot.ifBlank { appText("daily", selectedLanguage) }}",
                color = TextDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (reminder.mealTiming.isNotBlank()) {
                Text(
                    text = reminder.mealTiming,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            if (reminder.startDate.isNotBlank() || reminder.endDate.isNotBlank()) {
                Text(
                    text = listOfNotNull(reminder.startDate.takeIf { it.isNotBlank() }, reminder.endDate.takeIf { it.isNotBlank() }).joinToString(" → "),
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

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
        today.get(java.util.Calendar.DAY_OF_YEAR) == target.get(java.util.Calendar.DAY_OF_YEAR)
    ) {
        return "Today • ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(parsed)}"
    }

    val yesterday = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
    if (yesterday.get(java.util.Calendar.YEAR) == target.get(java.util.Calendar.YEAR) &&
        yesterday.get(java.util.Calendar.DAY_OF_YEAR) == target.get(java.util.Calendar.DAY_OF_YEAR)
    ) {
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.width(110.dp),
            color = TextMuted,
            fontSize = 12.sp,
            maxLines = 1,
            softWrap = false
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = value,
            modifier = Modifier.weight(1f),
            color = TextDark,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun HealthDataDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Mint.copy(alpha = 0.6f))
    )
}

@Composable
private fun ProfileDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(Mint.copy(alpha = 0.6f))
    )
}