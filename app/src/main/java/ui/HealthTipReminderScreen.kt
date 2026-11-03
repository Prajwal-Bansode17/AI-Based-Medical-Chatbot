package ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_based_medical_chatbot.HealthReminderFrequency
import com.example.ai_based_medical_chatbot.HealthTipReminder
import com.example.ai_based_medical_chatbot.HealthTipReminderRepository
import com.example.ai_based_medical_chatbot.HealthTipReminderScheduler
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
import com.example.ai_based_medical_chatbot.ui.theme.StatusSuccess
import com.example.ai_based_medical_chatbot.ui.theme.TextDark
import com.example.ai_based_medical_chatbot.ui.theme.TextMuted
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HealthTipReminderScreen(
    onBackClick: () -> Unit,
    onNavigateToTip: (Int) -> Unit = {}
) {
    BackHandler { onBackClick() }

    val context = LocalContext.current
    val appLanguage = LocalAppLanguageController.current.selectedLanguage
    fun t(key: String): String = appText(key, appLanguage)

    val scheduler = remember { HealthTipReminderScheduler(context) }

    var reminders by remember { mutableStateOf<List<HealthTipReminder>>(emptyList()) }
    var nextScheduledPair by remember { mutableStateOf<Pair<HealthTipReminder, Long>?>(null) }

    var reminderToEdit by remember { mutableStateOf<HealthTipReminder?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var reminderToDelete by remember { mutableStateOf<HealthTipReminder?>(null) }

    fun refreshList() {
        reminders = HealthTipReminderRepository.getAll(context)
        nextScheduledPair = HealthTipReminderRepository.getNextScheduled(context)
    }

    LaunchedEffect(Unit) {
        refreshList()
    }

    // Dialog for adding or editing
    if (showAddDialog || reminderToEdit != null) {
        AddHealthTipReminderDialog(
            initialReminder = reminderToEdit,
            screenLanguage = appLanguage,
            onDismiss = {
                showAddDialog = false
                reminderToEdit = null
            },
            onSave = { savedReminder ->
                scheduler.scheduleAndSave(savedReminder)
                showAddDialog = false
                reminderToEdit = null
                refreshList()
                Toast.makeText(
                    context,
                    if (reminderToEdit != null) t("reminder_updated_success") else t("reminder_saved_success"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    // Confirmation dialog for deletion
    if (reminderToDelete != null) {
        val rem = reminderToDelete!!
        AlertDialog(
            onDismissRequest = { reminderToDelete = null },
            title = {
                Text(
                    text = t("confirm_delete_reminder_title"),
                    color = DarkTeal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = t("confirm_delete_reminder_msg") + "\n\n\"${rem.title}\"",
                    color = TextDark,
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scheduler.deleteAndCancel(rem.id)
                        reminderToDelete = null
                        refreshList()
                        Toast.makeText(context, t("reminder_deleted_success"), Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(t("btn_delete_reminder"), color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { reminderToDelete = null }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = PureWhite,
            shape = RoundedCornerShape(18.dp)
        )
    }

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 14.dp,
                end = 16.dp,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top App Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(42.dp)
                            .background(PureWhite, RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, Mint), RoundedCornerShape(12.dp)),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = PureWhite,
                            contentColor = DarkTeal
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DarkTeal,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = t("reminders_screen_title"),
                            color = DarkTeal,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = t("reminders_screen_subtitle"),
                            color = TextMuted,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Add Button
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = t("btn_add_new_reminder"),
                            color = PureWhite,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Next Scheduled Reminder Highlight Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkTeal),
                    border = BorderStroke(1.dp, Mint.copy(alpha = 0.35f)),
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
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .background(
                                            PureWhite.copy(alpha = 0.2f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = PureWhite,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = t("next_reminder_banner_title"),
                                        color = PureWhite,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (nextScheduledPair != null) {
                                    val triggerMillis = nextScheduledPair!!.second
                                    val dateFmt = SimpleDateFormat("EEE, hh:mm a", Locale.getDefault()).format(Date(triggerMillis))
                                    Text(
                                        text = dateFmt,
                                        color = PureWhite.copy(alpha = 0.9f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (nextScheduledPair != null) {
                                val nextRem = nextScheduledPair!!.first
                                Text(
                                    text = nextRem.title,
                                    color = PureWhite,
                                    fontSize = 16.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (nextRem.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = nextRem.description,
                                        color = PureWhite.copy(alpha = 0.88f),
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 16.sp
                                    )
                                }
                            } else {
                                Text(
                                    text = t("next_reminder_banner_none"),
                                    color = PureWhite.copy(alpha = 0.9f),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Reminders List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Scheduled Routines (${reminders.size})",
                        color = DarkTeal,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Empty State
            if (reminders.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, Mint)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(SoftMint, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = DeepTeal,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = t("no_reminders_title"),
                                color = DarkTeal,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = t("no_reminders_desc"),
                                color = TextMuted,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            } else {
                items(
                    items = reminders,
                    key = { it.id }
                ) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        screenLanguage = appLanguage,
                        onToggle = { isEnabled ->
                            scheduler.setEnabled(reminder.id, isEnabled)
                            refreshList()
                        },
                        onEdit = { reminderToEdit = reminder },
                        onDelete = { reminderToDelete = reminder },
                        onTipClick = {
                            if (reminder.tipId > 0) {
                                onNavigateToTip(reminder.tipId)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: HealthTipReminder,
    screenLanguage: String,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTipClick: () -> Unit
) {
    fun t(key: String): String = appText(key, screenLanguage)

    val freqLabel = when (reminder.frequency) {
        HealthReminderFrequency.ONCE -> t("frequency_once")
        HealthReminderFrequency.DAILY -> t("frequency_daily")
        HealthReminderFrequency.WEEKLY -> {
            val daysStr = if (reminder.selectedDays.isNotEmpty()) {
                val dayNames = reminder.selectedDays.sorted().map { dayInt ->
                    when (dayInt) {
                        Calendar.MONDAY -> t("day_mon")
                        Calendar.TUESDAY -> t("day_tue")
                        Calendar.WEDNESDAY -> t("day_wed")
                        Calendar.THURSDAY -> t("day_thu")
                        Calendar.FRIDAY -> t("day_fri")
                        Calendar.SATURDAY -> t("day_sat")
                        else -> t("day_sun")
                    }
                }
                " (${dayNames.joinToString(", ")})"
            } else ""
            "${t("frequency_weekly")}$daysStr"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
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
            // Header Row: Time badge + Enabled Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (reminder.enabled) SoftMint else PureWhite,
                                RoundedCornerShape(8.dp)
                            )
                            .border(BorderStroke(1.dp, Mint), RoundedCornerShape(8.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = reminder.time,
                            color = if (reminder.enabled) DeepTeal else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .background(SoftMint.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = freqLabel,
                            color = DeepTeal,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Switch(
                    checked = reminder.enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PureWhite,
                        checkedTrackColor = DeepTeal,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = Mint
                    ),
                    modifier = Modifier.size(width = 44.dp, height = 24.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title & Description
            Text(
                text = reminder.title,
                color = if (reminder.enabled) DarkTeal else TextMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            if (reminder.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = reminder.description,
                    color = TextDark.copy(alpha = if (reminder.enabled) 0.85f else 0.5f),
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Actions row (Edit & Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (reminder.tipId > 0) {
                    Text(
                        text = "Tip #${reminder.tipId}",
                        color = SageGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onTipClick() }
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = DeepTeal,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = StatusError,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
