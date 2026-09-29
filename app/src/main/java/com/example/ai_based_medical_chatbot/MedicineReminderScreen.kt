package ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ai_based_medical_chatbot.MedicineReminder
import com.example.ai_based_medical_chatbot.MedicineReminderRepository
import com.example.ai_based_medical_chatbot.MedicineReminderScheduler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val ReminderBlue = Color(0xFF176B83)
private val ReminderLightBlue = Color(0xFFEAF5F8)
private val ReminderBackground = Color(0xFFF6FBFC)
private val ReminderGreen = Color(0xFF2E8B72)
private val ReminderRed = Color(0xFFC94B4B)

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
fun MedicineReminderScreen(
    onBackClick: () -> Unit = {},
    initialReminder: MedicineReminder? = null
) {
    val context = LocalContext.current

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
                errorMessage = "Please allow notifications from Android Settings."
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

    Scaffold(
        containerColor = ReminderBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "My Medicine Reminders",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Set simple reminders for your medicines",
                            style = MaterialTheme.typography.labelMedium,
                            color = ReminderBlue
                        )
                    }
                },
                navigationIcon = {
                    Text(
                        text = "‹",
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clickable { onBackClick() },
                        style = MaterialTheme.typography.headlineMedium,
                        color = ReminderBlue
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ReminderLightBlue
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    Color.White,
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "💊",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Add a medicine reminder",
                                fontWeight = FontWeight.Bold,
                                color = ReminderBlue
                            )
                            Text(
                                text = "We will remind you when it is time to take it.",
                                style = MaterialTheme.typography.bodySmall
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
                            containerColor = Color(0xFFFFF7E6)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔔",
                                style = MaterialTheme.typography.titleLarge
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Allow notifications",
                                    fontWeight = FontWeight.Bold,
                                    color = ReminderBlue
                                )
                                Text(
                                    text = "So MedAssist can remind you on time.",
                                    style = MaterialTheme.typography.bodySmall
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
                                Text("Allow")
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Add Medicine",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ReminderBlue
                        )

                        OutlinedTextField(
                            value = medicineName,
                            onValueChange = {
                                medicineName = it
                                errorMessage = ""
                                successMessage = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontWeight = FontWeight.Medium),
                            label = { Text("Medicine name *") },
                            placeholder = { Text("e.g. Paracetamol") },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = strength,
                            onValueChange = { strength = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontWeight = FontWeight.Medium),
                            label = { Text("Strength (optional)") },
                            placeholder = { Text("e.g. 650 mg") },
                            singleLine = true
                        )

                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedTime,
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("What time? *") },
                                placeholder = { Text("Tap here to choose time") },
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
                            text = "When should you take it?",
                            fontWeight = FontWeight.SemiBold
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
                                    text = "☀ Morning",
                                    selected = selectedSlot == "Morning",
                                    onClick = { selectedSlot = "Morning" },
                                    modifier = Modifier.weight(1f)
                                )
                                SimpleTimeSlotChip(
                                    text = "☀ Afternoon",
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
                                    text = "🌆 Evening",
                                    selected = selectedSlot == "Evening",
                                    onClick = { selectedSlot = "Evening" },
                                    modifier = Modifier.weight(1f)
                                )
                                SimpleTimeSlotChip(
                                    text = "🌙 Night",
                                    selected = selectedSlot == "Night",
                                    onClick = { selectedSlot = "Night" },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Text(
                            text = "Before or after food?",
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SimpleTimeSlotChip(
                                text = "Before Food",
                                selected = selectedMeal == "Before Food",
                                onClick = { selectedMeal = "Before Food" },
                                modifier = Modifier.weight(1f)
                            )
                            SimpleTimeSlotChip(
                                text = "After Food",
                                selected = selectedMeal == "After Food",
                                onClick = { selectedMeal = "After Food" },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = duration,
                            onValueChange = { duration = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontWeight = FontWeight.Medium),
                            label = { Text("How many days?") },
                            placeholder = { Text("e.g. 5 days") },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = startDate,
                            onValueChange = { startDate = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontWeight = FontWeight.Medium),
                            label = { Text("Start date") },
                            placeholder = { Text("e.g. 29/09/2026") },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = endDate,
                            onValueChange = { endDate = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontWeight = FontWeight.Medium),
                            label = { Text("End date") },
                            placeholder = { Text("e.g. 03/10/2026") },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = instructions,
                            onValueChange = { instructions = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontWeight = FontWeight.Medium),
                            label = { Text("How should you take it?") },
                            placeholder = { Text("e.g. Take with water") },
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
                            onClick = {
                                when {
                                    medicineName.isBlank() -> {
                                        errorMessage = "Please enter medicine name."
                                        successMessage = ""
                                    }

                                    selectedTime.isBlank() -> {
                                        errorMessage = "Please select reminder time."
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
                                        successMessage =
                                            "Reminder saved successfully."
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ReminderBlue
                            )
                        ) {
                            Text(
                                text = "Save Medicine Reminder",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Saved Reminders",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ReminderBlue
                )
            }

            if (reminders.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🔔", style = MaterialTheme.typography.headlineMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "No medicine reminders yet", fontWeight = FontWeight.Bold)
                            Text(text = "Add your first reminder above.", style = MaterialTheme.typography.bodySmall)
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
                            text = reminderDayLabel(dayKey),
                            color = ReminderBlue,
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

private fun reminderDayLabel(dayKey: String): String {
    val parsed = try {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dayKey)
    } catch (_: Exception) { null }

    if (parsed == null) return dayKey

    val today = Calendar.getInstance()
    val target = Calendar.getInstance().apply { time = parsed }

    if (today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)) {
        return "Today • ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(parsed)}"
    }

    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    if (yesterday.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)) {
        return "Yesterday • ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(parsed)}"
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
        targetValue = if (selected) ReminderBlue else Color.White,
        animationSpec = tween(180),
        label = "slot_color"
    )

    Card(
        modifier = modifier
            .height(52.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 3.dp else 1.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (selected) Color.White else ReminderBlue,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun ReminderCard(
    reminder: MedicineReminder,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
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
                    Text(
                        text = "💊",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = reminder.medicineName.ifBlank {
                            "Medicine"
                        },
                        fontWeight = FontWeight.Bold,
                        color = ReminderBlue
                    )

                    if (reminder.strength.isNotBlank()) {
                        Text(
                            text = reminder.strength,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Text(
                        text = reminder.time,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Switch(
                    checked = reminder.enabled,
                    onCheckedChange = onToggle
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Divider()

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReminderTag(reminder.daySlot)
                ReminderTag(reminder.mealTiming)
                if (reminder.duration.isNotBlank()) {
                    ReminderTag(reminder.duration)
                }
            }

            Text(
                text = if (showDetails) "Hide details" else "View details",
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
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            text = "Reminder Summary",
                            color = ReminderBlue,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "⏰ ${reminder.time}  •  ${reminder.daySlot}",
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "🍽 ${reminder.mealTiming}" +
                                if (reminder.duration.isNotBlank()) {
                                    "  •  ${reminder.duration}"
                                } else {
                                    ""
                                }
                        )

                        if (reminder.startDate.isNotBlank() || reminder.endDate.isNotBlank()) {
                            Text(
                                text = "📅 " +
                                    listOfNotNull(
                                        reminder.startDate.takeIf { it.isNotBlank() },
                                        reminder.endDate.takeIf { it.isNotBlank() }
                                    ).joinToString(" → ")
                            )
                        }

                        if (reminder.instructions.isNotBlank()) {
                            Text(
                                text = "💡 ${reminder.instructions}"
                            )
                        }

                        Text(
                            text = "Delete Reminder",
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
            .padding(
                horizontal = 10.dp,
                vertical = 6.dp
            )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = ReminderBlue
        )
    }
}
