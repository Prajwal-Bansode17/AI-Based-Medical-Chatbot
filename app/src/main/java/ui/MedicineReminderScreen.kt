package ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.content.Intent
import android.provider.Settings
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.core.content.ContextCompat
import com.example.ai_based_medical_chatbot.MedicineReminder
import com.example.ai_based_medical_chatbot.MedicineReminderRepository
import com.example.ai_based_medical_chatbot.MedicineReminderScheduler
import com.example.ai_based_medical_chatbot.ui.theme.DeepTeal
import com.example.ai_based_medical_chatbot.ui.theme.DarkTeal
import com.example.ai_based_medical_chatbot.ui.theme.SageGreen
import com.example.ai_based_medical_chatbot.ui.theme.Ivory
import com.example.ai_based_medical_chatbot.ui.theme.Mint
import com.example.ai_based_medical_chatbot.ui.theme.SoftMint
import com.example.ai_based_medical_chatbot.ui.theme.CoralAccent
import com.example.ai_based_medical_chatbot.ui.theme.PureWhite
import com.example.ai_based_medical_chatbot.ui.theme.AppBackground
import com.example.ai_based_medical_chatbot.ui.theme.AppSurface
import com.example.ai_based_medical_chatbot.ui.theme.LightGreenBorder
import com.example.ai_based_medical_chatbot.ui.theme.LightGreenSoft
import com.example.ai_based_medical_chatbot.ui.theme.LightGreenSurface
import com.example.ai_based_medical_chatbot.ui.theme.PrimaryTeal
import com.example.ai_based_medical_chatbot.ui.theme.PrimaryTealDark
import com.example.ai_based_medical_chatbot.ui.theme.StatusError
import com.example.ai_based_medical_chatbot.ui.theme.StatusErrorBackground
import com.example.ai_based_medical_chatbot.ui.theme.StatusErrorBorder
import com.example.ai_based_medical_chatbot.ui.theme.StatusSuccess
import com.example.ai_based_medical_chatbot.ui.theme.StatusWarning
import com.example.ai_based_medical_chatbot.ui.theme.TextDark
import com.example.ai_based_medical_chatbot.ui.theme.TextMuted
import com.example.ai_based_medical_chatbot.ui.theme.TextLight
import com.example.ai_based_medical_chatbot.LocalAppLanguageController
import com.example.ai_based_medical_chatbot.appText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val ReminderBlue = DeepTeal
private val ReminderDarkBlue = DarkTeal
private val ReminderLightBlue = SoftMint
private val ReminderBackground = Ivory
private val ReminderCardSurface = PureWhite
private val ReminderMintBorder = Mint
private val ReminderGreen = SageGreen
private val ReminderRed = StatusError

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
fun MedicineReminderScreen(
    onBackClick: () -> Unit = {},
    initialReminder: MedicineReminder? = null
) {
    val context = LocalContext.current
    val appLanguageController = LocalAppLanguageController.current
    val selectedLanguage = appLanguageController.selectedLanguage
    fun t(key: String): String = appText(key, selectedLanguage)

    fun localizedSlot(slot: String): String = when (slot) {
        "Morning" -> t("slot_morning")
        "Afternoon" -> t("slot_afternoon")
        "Evening" -> t("slot_evening")
        "Night" -> t("slot_night")
        else -> slot
    }

    fun localizedMeal(meal: String): String = when (meal) {
        "Before Food" -> t("meal_before_food")
        "After Food" -> t("meal_after_food")
        else -> meal
    }

    fun slotFromPrescription(value: String): String {
        val text = value.lowercase()
        return when {
            "morning" in text -> "Morning"
            "afternoon" in text -> "Afternoon"
            "evening" in text -> "Evening"
            "night" in text -> "Night"
            else -> "Morning"
        }
    }

    fun mealFromPrescription(value: String): String {
        val text = value.lowercase()
        return when {
            "before" in text -> "Before Food"
            "after" in text -> "After Food"
            else -> "After Food"
        }
    }

    var medicineName by remember(initialReminder) {
        mutableStateOf(initialReminder?.medicineName.orEmpty())
    }
    var strength by remember(initialReminder) {
        mutableStateOf(initialReminder?.strength.orEmpty())
    }
    var selectedTime by remember(initialReminder) {
        mutableStateOf(initialReminder?.time.orEmpty())
    }
    var selectedSlot by remember(initialReminder) {
        mutableStateOf(slotFromPrescription(initialReminder?.daySlot.orEmpty()))
    }
    var selectedMeal by remember(initialReminder) {
        mutableStateOf(mealFromPrescription(initialReminder?.mealTiming.orEmpty()))
    }
    var duration by remember(initialReminder) {
        mutableStateOf(initialReminder?.duration.orEmpty())
    }
    var startDate by remember(initialReminder) {
        mutableStateOf(initialReminder?.startDate.orEmpty())
    }
    var endDate by remember(initialReminder) {
        mutableStateOf(initialReminder?.endDate.orEmpty())
    }
    var instructions by remember(initialReminder) {
        mutableStateOf(initialReminder?.instructions.orEmpty())
    }

    val strengthFocusRequester = remember { FocusRequester() }
    val durationFocusRequester = remember { FocusRequester() }
    val startDateFocusRequester = remember { FocusRequester() }
    val endDateFocusRequester = remember { FocusRequester() }
    val instructionsFocusRequester = remember { FocusRequester() }

    var reminders by remember {
        mutableStateOf(MedicineReminderRepository.getAll(context))
    }

    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    var notificationPermissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            notificationPermissionGranted = granted
            if (granted) {
                errorMessage = ""
            } else {
                errorMessage = t("reminder_error_permission")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                    )
                }
            }
        }

    LaunchedEffect(Unit) {
        notificationPermissionGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
    }

    fun refresh() {
        reminders = MedicineReminderRepository.getAll(context)
    }

    fun clearForm() {
        medicineName = ""
        strength = ""
        selectedTime = ""
        selectedSlot = "Morning"
        selectedMeal = "After Food"
        duration = ""
        startDate = ""
        endDate = ""
        instructions = ""
    }

    fun showTimePicker() {
        val calendar = Calendar.getInstance()

        TimePickerDialog(
            context,
            { _, hour, minute ->
                val suffix = if (hour >= 12) "PM" else "AM"
                val displayHour = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }

                selectedTime =
                    String.format(
                        "%02d:%02d %s",
                        displayHour,
                        minute,
                        suffix
                    )
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    fun saveReminder() {
        when {
            medicineName.isBlank() -> {
                errorMessage = t("reminder_error_enter_name")
                successMessage = ""
            }

            selectedTime.isBlank() -> {
                errorMessage = t("reminder_error_select_time")
                successMessage = ""
            }

            else -> {
                val reminder =
                    MedicineReminder(
                        medicineName = medicineName.trim(),
                        strength = strength.trim(),
                        time = selectedTime.trim(),
                        daySlot = selectedSlot,
                        mealTiming = selectedMeal,
                        duration = duration.trim(),
                        startDate = startDate.trim(),
                        endDate = endDate.trim(),
                        instructions = instructions.trim(),
                        enabled = true
                    )

                MedicineReminderScheduler(context)
                    .scheduleAndSave(reminder)

                refresh()
                clearForm()

                errorMessage = ""
                successMessage = t("reminder_success_saved")
            }
        }
    }

    Scaffold(
        containerColor = ReminderBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = t("reminder_title"),
                            fontWeight = FontWeight.Bold,
                            color = ReminderDarkBlue
                        )
                        Text(
                            text = t("reminder_subtitle"),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = t("back"),
                            tint = ReminderBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ReminderCardSurface
                )
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ReminderLightBlue
                    ),
                    border = BorderStroke(1.dp, ReminderMintBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    ReminderCardSurface,
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = PillVectorIcon,
                                contentDescription = null,
                                tint = ReminderBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = t("reminder_intro_title"),
                                fontWeight = FontWeight.Bold,
                                color = ReminderBlue
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = t("reminder_intro_desc"),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            item {
                if (!notificationPermissionGranted) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = ReminderLightBlue
                        ),
                        border = BorderStroke(1.dp, ReminderMintBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notification",
                                tint = ReminderDarkBlue,
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = t("reminder_allow_notifications"),
                                    fontWeight = FontWeight.Bold,
                                    color = ReminderDarkBlue
                                )
                                Text(
                                    text = t("reminder_notifications_desc"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }

                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        if (ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.POST_NOTIFICATIONS
                                            ) != PackageManager.PERMISSION_GRANTED
                                        ) {
                                            try {
                                                notificationPermissionLauncher.launch(
                                                    Manifest.permission.POST_NOTIFICATIONS
                                                )
                                            } catch (_: Exception) {
                                                context.startActivity(
                                                    Intent(
                                                        Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                                    ).apply {
                                                        putExtra(
                                                            Settings.EXTRA_APP_PACKAGE,
                                                            context.packageName
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    } else {
                                        notificationPermissionGranted = true
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ReminderBlue
                                )
                            ) {
                                Text(t("reminder_allow_btn"))
                            }
                        }
                    }
                }
            }

            item {
                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ReminderBlue,
                    unfocusedBorderColor = ReminderMintBorder,
                    focusedLabelColor = ReminderBlue,
                    unfocusedLabelColor = TextMuted,
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark,
                    focusedContainerColor = ReminderCardSurface,
                    unfocusedContainerColor = ReminderLightBlue.copy(alpha = 0.25f)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ReminderCardSurface
                    ),
                    border = BorderStroke(1.dp, ReminderMintBorder),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = t("reminder_add_medicine"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ReminderDarkBlue
                        )

                        OutlinedTextField(
                            value = medicineName,
                            onValueChange = {
                                medicineName = it
                                errorMessage = ""
                                successMessage = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { strengthFocusRequester.requestFocus() }
                            ),
                            textStyle = TextStyle(fontWeight = FontWeight.Medium),
                            label = { Text(t("reminder_med_name_label")) },
                            placeholder = { Text(t("reminder_med_name_placeholder")) },
                            colors = fieldColors,
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = strength,
                            onValueChange = { strength = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(strengthFocusRequester),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { durationFocusRequester.requestFocus() }
                            ),
                            textStyle = TextStyle(fontWeight = FontWeight.Medium),
                            label = { Text(t("reminder_strength_label")) },
                            placeholder = { Text(t("reminder_strength_placeholder")) },
                            colors = fieldColors,
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedTime,
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(t("reminder_what_time")) },
                                placeholder = { Text(t("reminder_choose_time_tap")) },
                                colors = fieldColors,
                                shape = RoundedCornerShape(12.dp),
                                readOnly = true,
                                singleLine = true
                            )

                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable {
                                        showTimePicker()
                                    }
                            )
                        }

                        Text(
                            text = t("reminder_when_take"),
                            fontWeight = FontWeight.SemiBold,
                            color = ReminderDarkBlue
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SimpleTimeSlotChip(
                                    text = localizedSlot("Morning"),
                                    selected = selectedSlot == "Morning",
                                    onClick = { selectedSlot = "Morning" },
                                    modifier = Modifier.weight(1f)
                                )
                                SimpleTimeSlotChip(
                                    text = localizedSlot("Afternoon"),
                                    selected = selectedSlot == "Afternoon",
                                    onClick = { selectedSlot = "Afternoon" },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SimpleTimeSlotChip(
                                    text = localizedSlot("Evening"),
                                    selected = selectedSlot == "Evening",
                                    onClick = { selectedSlot = "Evening" },
                                    modifier = Modifier.weight(1f)
                                )
                                SimpleTimeSlotChip(
                                    text = localizedSlot("Night"),
                                    selected = selectedSlot == "Night",
                                    onClick = { selectedSlot = "Night" },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Text(
                            text = t("reminder_before_after_food"),
                            fontWeight = FontWeight.SemiBold,
                            color = ReminderDarkBlue
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SimpleTimeSlotChip(
                                text = localizedMeal("Before Food"),
                                selected = selectedMeal == "Before Food",
                                onClick = { selectedMeal = "Before Food" },
                                modifier = Modifier.weight(1f)
                            )
                            SimpleTimeSlotChip(
                                text = localizedMeal("After Food"),
                                selected = selectedMeal == "After Food",
                                onClick = { selectedMeal = "After Food" },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = duration,
                            onValueChange = { duration = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(durationFocusRequester),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { startDateFocusRequester.requestFocus() }
                            ),
                            textStyle = TextStyle(fontWeight = FontWeight.Medium),
                            label = { Text(t("reminder_how_many_days")) },
                            placeholder = { Text(t("reminder_days_placeholder")) },
                            colors = fieldColors,
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = startDate,
                            onValueChange = { startDate = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(startDateFocusRequester),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { endDateFocusRequester.requestFocus() }
                            ),
                            textStyle = TextStyle(fontWeight = FontWeight.Medium),
                            label = { Text(t("reminder_start_date")) },
                            placeholder = { Text("e.g. 29/09/2026") },
                            colors = fieldColors,
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = endDate,
                            onValueChange = { endDate = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(endDateFocusRequester),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { instructionsFocusRequester.requestFocus() }
                            ),
                            textStyle = TextStyle(fontWeight = FontWeight.Medium),
                            label = { Text(t("reminder_end_date")) },
                            placeholder = { Text("e.g. 03/10/2026") },
                            colors = fieldColors,
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = instructions,
                            onValueChange = { instructions = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(instructionsFocusRequester),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { saveReminder() }
                            ),
                            textStyle = TextStyle(fontWeight = FontWeight.Medium),
                            label = { Text(t("reminder_how_take")) },
                            placeholder = { Text(t("reminder_instructions_placeholder")) },
                            colors = fieldColors,
                            shape = RoundedCornerShape(12.dp),
                            minLines = 2,
                            maxLines = 4
                        )

                        if (errorMessage.isNotBlank()) {
                            Text(
                                text = errorMessage,
                                color = ReminderRed,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (successMessage.isNotBlank()) {
                            Text(
                                text = successMessage,
                                color = ReminderGreen,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = { saveReminder() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ReminderBlue
                            )
                        ) {
                            Text(
                                text = t("reminder_save_button"),
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = t("reminder_saved_reminders"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ReminderDarkBlue
                )
            }

            if (reminders.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = ReminderCardSurface),
                        border = BorderStroke(1.dp, ReminderMintBorder)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = ReminderDarkBlue,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = t("reminder_no_reminders_yet"), fontWeight = FontWeight.Bold, color = ReminderDarkBlue)
                            Text(text = t("reminder_add_first_above"), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                    }
                }
            } else {
                val groupedReminders = reminders
                    .sortedByDescending { it.createdAt }
                    .groupBy { reminderDayKey(it.createdAt) }

                groupedReminders.forEach { (dayKey, dayReminders) ->
                    item {
                        Text(
                            text = reminderDayLabel(dayKey, ::t),
                            color = ReminderDarkBlue,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    items(
                        items = dayReminders,
                        key = { it.id }
                    ) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            localizedSlot = ::localizedSlot,
                            localizedMeal = ::localizedMeal,
                            t = ::t,
                            onToggle = { enabled ->
                                MedicineReminderScheduler(context).updateEnabled(reminder.id, enabled)
                                refresh()
                            },
                            onDelete = {
                                MedicineReminderScheduler(context).cancel(reminder.id)
                                MedicineReminderRepository.delete(context, reminder.id)
                                refresh()
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun reminderDayKey(timeMillis: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timeMillis))

private fun reminderDayLabel(dayKey: String, t: (String) -> String): String {
    val parsed = try {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dayKey)
    } catch (_: Exception) { null }

    if (parsed == null) return dayKey

    val today = Calendar.getInstance()
    val target = Calendar.getInstance().apply { time = parsed }

    if (today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)) {
        return "${t("today")} • ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(parsed)}"
    }

    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    if (yesterday.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)) {
        return "${t("yesterday")} • ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(parsed)}"
    }

    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(parsed)
}

@androidx.compose.runtime.Composable
private fun SimpleTimeSlotChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) ReminderBlue else ReminderCardSurface,
        animationSpec = tween(180),
        label = "slot_color"
    )

    Card(
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, if (selected) ReminderBlue else ReminderMintBorder),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 2.dp else 0.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (selected) PureWhite else ReminderDarkBlue,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun ReminderCard(
    reminder: MedicineReminder,
    localizedSlot: (String) -> String,
    localizedMeal: (String) -> String,
    t: (String) -> String,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = ReminderCardSurface
        ),
        border = BorderStroke(1.dp, ReminderMintBorder),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            ReminderLightBlue,
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = PillVectorIcon,
                        contentDescription = null,
                        tint = ReminderBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = reminder.medicineName.ifBlank {
                            t("medicine")
                        },
                        fontWeight = FontWeight.Bold,
                        color = ReminderBlue
                    )

                    if (reminder.strength.isNotBlank()) {
                        Text(
                            text = reminder.strength,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }

                    Text(
                        text = reminder.time,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }

                Switch(
                    checked = reminder.enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PureWhite,
                        checkedTrackColor = ReminderBlue,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = ReminderLightBlue,
                        uncheckedBorderColor = ReminderMintBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Divider(color = ReminderMintBorder.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReminderTag(localizedSlot(reminder.daySlot))
                ReminderTag(localizedMeal(reminder.mealTiming))
                if (reminder.duration.isNotBlank()) {
                    ReminderTag(reminder.duration)
                }
            }

            Text(
                text = if (showDetails) t("reminder_hide_details") else t("reminder_view_details"),
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clickable {
                        showDetails = !showDetails
                    },
                color = ReminderBlue,
                fontWeight = FontWeight.SemiBold
            )

            AnimatedVisibility(visible = showDetails) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ReminderLightBlue
                    ),
                    border = BorderStroke(1.dp, ReminderMintBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            text = t("reminder_details_summary"),
                            color = ReminderDarkBlue,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "${reminder.time}  •  ${localizedSlot(reminder.daySlot)}",
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )

                        Text(
                            text = localizedMeal(reminder.mealTiming) +
                                    if (reminder.duration.isNotBlank()) {
                                        "  •  ${reminder.duration}"
                                    } else {
                                        ""
                                    },
                            color = TextDark
                        )

                        if (reminder.startDate.isNotBlank() || reminder.endDate.isNotBlank()) {
                            Text(
                                text = listOfNotNull(
                                    reminder.startDate.takeIf { it.isNotBlank() },
                                    reminder.endDate.takeIf { it.isNotBlank() }
                                ).joinToString(" → "),
                                color = TextDark
                            )
                        }

                        if (reminder.instructions.isNotBlank()) {
                            Text(
                                text = reminder.instructions,
                                color = TextDark
                            )
                        }

                        Text(
                            text = t("reminder_delete_btn"),
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clickable { onDelete() },
                            color = ReminderRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun ReminderTag(
    text: String
) {
    if (text.isBlank()) return

    Box(
        modifier = Modifier
            .background(
                ReminderLightBlue,
                RoundedCornerShape(10.dp)
            )
            .border(
                BorderStroke(1.dp, ReminderMintBorder),
                RoundedCornerShape(10.dp)
            )
            .padding(
                horizontal = 10.dp,
                vertical = 6.dp
            )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = ReminderDarkBlue,
            fontWeight = FontWeight.Medium
        )
    }
}
