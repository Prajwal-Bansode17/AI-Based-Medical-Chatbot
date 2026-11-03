package ui

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai_based_medical_chatbot.HealthReminderFrequency
import com.example.ai_based_medical_chatbot.HealthTipReminder
import com.example.ai_based_medical_chatbot.HealthTipReminderRepository
import com.example.ai_based_medical_chatbot.appText
import com.example.ai_based_medical_chatbot.ui.theme.DarkTeal
import com.example.ai_based_medical_chatbot.ui.theme.DeepTeal
import com.example.ai_based_medical_chatbot.ui.theme.Ivory
import com.example.ai_based_medical_chatbot.ui.theme.Mint
import com.example.ai_based_medical_chatbot.ui.theme.PureWhite
import com.example.ai_based_medical_chatbot.ui.theme.SoftMint
import com.example.ai_based_medical_chatbot.ui.theme.StatusError
import com.example.ai_based_medical_chatbot.ui.theme.TextDark
import com.example.ai_based_medical_chatbot.ui.theme.TextMuted
import java.util.Calendar

@Composable
fun AddHealthTipReminderDialog(
    initialReminder: HealthTipReminder? = null,
    tipId: Int = 0,
    defaultTitle: String = "",
    defaultDescription: String = "",
    screenLanguage: String = "English",
    onDismiss: () -> Unit,
    onSave: (HealthTipReminder) -> Unit
) {
    val context = LocalContext.current
    fun t(key: String): String = appText(key, screenLanguage)

    // Pre-populate fields
    var title by remember {
        mutableStateOf(
            initialReminder?.title
                ?: defaultTitle.ifBlank { "Morning Wellness Routine" }
        )
    }

    var description by remember {
        mutableStateOf(
            initialReminder?.description
                ?: defaultDescription.ifBlank { "Time for your daily health habit routine." }
        )
    }

    // Time parsing (Hour: 1..12, Minute: 0..59, isAm: true/false)
    val parsedTime = remember {
        val timeString = initialReminder?.time ?: "08:00 AM"
        val (h24, m) = HealthTipReminderRepository.parseHourAndMinute(timeString) ?: Pair(8, 0)
        val isAm = h24 < 12
        val h12 = when {
            h24 == 0 -> 12
            h24 > 12 -> h24 - 12
            else -> h24
        }
        Triple(h12, m, isAm)
    }

    var selectedHour by remember { mutableIntStateOf(parsedTime.first) }
    var selectedMinute by remember { mutableIntStateOf(parsedTime.second) }
    var isAm by remember { mutableStateOf(parsedTime.third) }

    var selectedFrequency by remember {
        mutableStateOf(initialReminder?.frequency ?: HealthReminderFrequency.DAILY)
    }

    val selectedDays = remember {
        mutableStateListOf<Int>().apply {
            if (initialReminder != null && initialReminder.selectedDays.isNotEmpty()) {
                addAll(initialReminder.selectedDays)
            } else {
                // Default: Mon to Fri
                addAll(listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY))
            }
        }
    }

    var titleError by remember { mutableStateOf("") }

    // Android 13+ Notification permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                context,
                "Notifications permission is required to sound routine alarms.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            border = BorderStroke(1.dp, Mint)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(SoftMint, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = DeepTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (initialReminder != null) t("btn_edit_reminder") else t("health_tip_add_reminder"),
                            color = DarkTeal,
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = t("reminders_screen_subtitle"),
                            color = TextMuted,
                            fontSize = 11.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title Input
                Text(
                    text = t("reminder_title_label"),
                    color = DarkTeal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (titleError.isNotEmpty()) titleError = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepTeal,
                        unfocusedBorderColor = Mint,
                        focusedContainerColor = SoftMint.copy(alpha = 0.5f),
                        unfocusedContainerColor = SoftMint.copy(alpha = 0.3f),
                        cursorColor = DeepTeal
                    ),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = TextDark,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                if (titleError.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = titleError,
                        color = StatusError,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Description Input
                Text(
                    text = t("reminder_desc_label"),
                    color = DarkTeal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepTeal,
                        unfocusedBorderColor = Mint,
                        focusedContainerColor = SoftMint.copy(alpha = 0.5f),
                        unfocusedContainerColor = SoftMint.copy(alpha = 0.3f),
                        cursorColor = DeepTeal
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = TextDark,
                        fontSize = 13.sp
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Time Selector
                Text(
                    text = t("reminder_time_label"),
                    color = DarkTeal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SoftMint, RoundedCornerShape(14.dp))
                        .border(BorderStroke(1.dp, Mint), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hour Stepper
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "▲",
                                color = DeepTeal,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        selectedHour = if (selectedHour >= 12) 1 else selectedHour + 1
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                            Text(
                                text = String.format("%02d", selectedHour),
                                color = DarkTeal,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "▼",
                                color = DeepTeal,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        selectedHour = if (selectedHour <= 1) 12 else selectedHour - 1
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = ":",
                            color = DarkTeal,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Minute Stepper
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "▲",
                                color = DeepTeal,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        selectedMinute = (selectedMinute + 5) % 60
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                            Text(
                                text = String.format("%02d", selectedMinute),
                                color = DarkTeal,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "▼",
                                color = DeepTeal,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        selectedMinute = if (selectedMinute <= 0) 55 else selectedMinute - 5
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // AM / PM Toggle Pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(PureWhite)
                                .border(BorderStroke(1.dp, Mint), RoundedCornerShape(12.dp))
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isAm) DeepTeal else Color.Transparent)
                                    .clickable { isAm = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "AM",
                                    color = if (isAm) PureWhite else DarkTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (!isAm) DeepTeal else Color.Transparent)
                                    .clickable { isAm = false }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "PM",
                                    color = if (!isAm) PureWhite else DarkTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Frequency Selector (Once / Daily / Weekly)
                Text(
                    text = t("reminder_frequency_label"),
                    color = DarkTeal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val frequencies = listOf(
                        HealthReminderFrequency.ONCE to t("frequency_once"),
                        HealthReminderFrequency.DAILY to t("frequency_daily"),
                        HealthReminderFrequency.WEEKLY to t("frequency_weekly")
                    )

                    frequencies.forEach { (freq, label) ->
                        val isSelected = selectedFrequency == freq
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) DeepTeal else SoftMint)
                                .border(
                                    BorderStroke(1.dp, if (isSelected) DeepTeal else Mint),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedFrequency = freq }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) PureWhite else DarkTeal,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Weekday Selection (shown when WEEKLY is selected)
                AnimatedVisibility(
                    visible = selectedFrequency == HealthReminderFrequency.WEEKLY,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = t("repeat_on_days"),
                            color = DarkTeal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val days = listOf(
                            Calendar.MONDAY to t("day_mon"),
                            Calendar.TUESDAY to t("day_tue"),
                            Calendar.WEDNESDAY to t("day_wed"),
                            Calendar.THURSDAY to t("day_thu"),
                            Calendar.FRIDAY to t("day_fri"),
                            Calendar.SATURDAY to t("day_sat"),
                            Calendar.SUNDAY to t("day_sun")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            days.forEach { (dayInt, dayLabel) ->
                                val isDaySelected = selectedDays.contains(dayInt)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isDaySelected) DeepTeal else SoftMint)
                                        .border(
                                            BorderStroke(1.dp, if (isDaySelected) DeepTeal else Mint),
                                            CircleShape
                                        )
                                        .clickable {
                                            if (isDaySelected) {
                                                if (selectedDays.size > 1) {
                                                    selectedDays.remove(dayInt)
                                                }
                                            } else {
                                                selectedDays.add(dayInt)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayLabel.take(2),
                                        color = if (isDaySelected) PureWhite else DarkTeal,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions (Cancel / Save)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text(
                            text = "Cancel",
                            color = TextMuted,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            val trimmedTitle = title.trim()
                            if (trimmedTitle.isBlank()) {
                                titleError = "Please enter a reminder title"
                                return@Button
                            }

                            // Calculate 24-hr hour
                            val h24 = when {
                                isAm && selectedHour == 12 -> 0
                                !isAm && selectedHour < 12 -> selectedHour + 12
                                else -> selectedHour
                            }
                            val formattedTimeStr = HealthTipReminderRepository.formatHourAndMinute(h24, selectedMinute)

                            // Request notification permission if needed
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                                android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }

                            val reminderToSave = (initialReminder ?: HealthTipReminder()).copy(
                                tipId = tipId,
                                title = trimmedTitle,
                                description = description.trim(),
                                time = formattedTimeStr,
                                frequency = selectedFrequency,
                                selectedDays = if (selectedFrequency == HealthReminderFrequency.WEEKLY) selectedDays.toList() else emptyList(),
                                enabled = true
                            )

                            onSave(reminderToSave)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = t("btn_save_reminder"),
                            color = PureWhite,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
