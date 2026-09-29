package com.example.ai_based_medical_chatbot

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val REMINDER_CHANNEL_ID = "medassist_medicine_reminders"
private const val REMINDER_CHANNEL_NAME = "Medicine Reminders"
private const val REMINDER_CHANNEL_DESCRIPTION = "Medicine dose reminders from MedAssist AI"

class MedicineReminderScheduler(private val context: Context) {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    init {
        initialize()
    }

    fun initialize() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                REMINDER_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = REMINDER_CHANNEL_DESCRIPTION
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun schedule(reminder: MedicineReminder) {
        if (!reminder.enabled) {
            cancel(reminder.id)
            return
        }

        val triggerAt = getNextTriggerMillis(reminder.time)
        if (triggerAt == null) return

        val intent = Intent(context, MedicineReminderReceiver::class.java).apply {
            putExtra("REMINDER_ID", reminder.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        } else {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } catch (_: SecurityException) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
        }
    }

    fun scheduleAll() {
        MedicineReminderRepository.getEnabled(context).forEach { schedule(it) }
    }

    fun cancel(reminderId: String) {
        val intent = Intent(context, MedicineReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun cancelAll() {
        MedicineReminderRepository.getAll(context).forEach {
            cancel(it.id)
        }
    }

    fun scheduleAndSave(reminder: MedicineReminder) {
        MedicineReminderRepository.save(context, reminder)
        schedule(reminder)
    }

    fun updateEnabled(reminderId: String, enabled: Boolean) {
        MedicineReminderRepository.updateEnabled(context, reminderId, enabled)

        val reminder = MedicineReminderRepository.getById(context, reminderId)
        if (reminder == null || !enabled) {
            cancel(reminderId)
        } else {
            schedule(reminder)
        }
    }

    private fun getNextTriggerMillis(time: String): Long? {
        val formats = listOf("hh:mm a", "HH:mm")

        for (pattern in formats) {
            try {
                val parsed = SimpleDateFormat(pattern, Locale.getDefault()).apply {
                    isLenient = false
                }.parse(time.trim()) ?: continue

                val parsedCalendar = Calendar.getInstance().apply {
                    timeInMillis = parsed.time
                }

                val now = Calendar.getInstance()

                val target = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, parsedCalendar.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, parsedCalendar.get(Calendar.MINUTE))
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)

                    if (timeInMillis <= now.timeInMillis) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }

                return target.timeInMillis
            } catch (_: Exception) {
                // Try the next supported format.
            }
        }

        return null
    }
}

class MedicineReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra("REMINDER_ID") ?: return
        val reminder = MedicineReminderRepository.getById(context, reminderId) ?: return

        if (!reminder.enabled) return

        MedicineReminderScheduler(context).initialize()
        showNotification(context, reminder)

        // Schedule the next daily occurrence.
        MedicineReminderScheduler(context).schedule(reminder)
    }

    private fun showNotification(
        context: Context,
        reminder: MedicineReminder
    ) {
        val title = buildString {
            append("💊 ")
            append(reminder.medicineName.ifBlank { "Medicine Reminder" })
            if (reminder.strength.isNotBlank()) {
                append(" • ")
                append(reminder.strength)
            }
        }

        val message = buildString {
            if (reminder.daySlot.isNotBlank()) {
                append(reminder.daySlot)
            }

            if (reminder.mealTiming.isNotBlank()) {
                if (isNotEmpty()) append(" • ")
                append(reminder.mealTiming)
            }

            if (reminder.duration.isNotBlank()) {
                if (isNotEmpty()) append(" • ")
                append(reminder.duration)
            }

            if (reminder.instructions.isNotBlank()) {
                if (isNotEmpty()) append("\n")
                append(reminder.instructions)
            }

            if (isEmpty()) {
                append("It's time to take your medicine.")
            }
        }

        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context)
                .notify(reminder.id.hashCode(), notification)
        }
    }
}

class MedicineReminderBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                MedicineReminderScheduler(context).scheduleAll()
            }
        }
    }
}
