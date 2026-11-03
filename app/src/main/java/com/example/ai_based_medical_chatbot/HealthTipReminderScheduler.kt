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

const val HEALTH_REMINDER_CHANNEL_ID = "medassist_health_tip_reminders"
const val HEALTH_REMINDER_CHANNEL_NAME = "Health & Wellness Reminders"
const val HEALTH_REMINDER_CHANNEL_DESC = "Daily and weekly wellness routine reminders from MedAssist AI"

class HealthTipReminderScheduler(private val context: Context) {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    init {
        createNotificationChannel()
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                HEALTH_REMINDER_CHANNEL_ID,
                HEALTH_REMINDER_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = HEALTH_REMINDER_CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun schedule(reminder: HealthTipReminder) {
        if (!reminder.enabled) {
            cancel(reminder.id)
            return
        }

        val triggerAt = HealthTipReminderRepository.calculateNextTriggerMillis(reminder)
        if (triggerAt == null) return

        val intent = Intent(context, HealthTipReminderReceiver::class.java).apply {
            putExtra("REMINDER_ID", reminder.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canExact) {
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
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        }
    }

    fun scheduleAll() {
        val enabledReminders = HealthTipReminderRepository.getEnabled(context)
        for (reminder in enabledReminders) {
            schedule(reminder)
        }
    }

    fun cancel(reminderId: String) {
        val intent = Intent(context, HealthTipReminderReceiver::class.java)
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
        val all = HealthTipReminderRepository.getAll(context)
        for (reminder in all) {
            cancel(reminder.id)
        }
    }

    fun scheduleAndSave(reminder: HealthTipReminder): HealthTipReminder {
        val saved = HealthTipReminderRepository.save(context, reminder)
        schedule(saved)
        return saved
    }

    fun deleteAndCancel(reminderId: String): Boolean {
        cancel(reminderId)
        return HealthTipReminderRepository.delete(context, reminderId)
    }

    fun setEnabled(reminderId: String, enabled: Boolean) {
        HealthTipReminderRepository.updateEnabled(context, reminderId, enabled)
        val reminder = HealthTipReminderRepository.getById(context, reminderId)
        if (reminder == null || !enabled) {
            cancel(reminderId)
        } else {
            schedule(reminder)
        }
    }
}

class HealthTipReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra("REMINDER_ID") ?: return
        val reminder = HealthTipReminderRepository.getById(context, reminderId) ?: return

        if (!reminder.enabled) return

        val scheduler = HealthTipReminderScheduler(context)
        scheduler.createNotificationChannel()

        showNotification(context, reminder)

        // If ONE-TIME reminder, disable it now. Otherwise, schedule next occurrence.
        if (reminder.frequency == HealthReminderFrequency.ONCE) {
            HealthTipReminderRepository.updateEnabled(context, reminderId, false)
        } else {
            scheduler.schedule(reminder)
        }
    }

    private fun showNotification(context: Context, reminder: HealthTipReminder) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_SCREEN", "healthTipReminders")
            putExtra("TARGET_TIP_ID", reminder.tipId)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = reminder.title.ifBlank { "Health & Wellness Routine Reminder" }
        val description = reminder.description.ifBlank {
            "It's time for your scheduled health habit routine. Stay healthy with MedAssist AI!"
        }

        val notification = NotificationCompat.Builder(context, HEALTH_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(description)
            .setStyle(NotificationCompat.BigTextStyle().bigText(description))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            try {
                NotificationManagerCompat.from(context).notify(reminder.id.hashCode(), notification)
            } catch (_: SecurityException) {
                // Graceful fallback if permission revoked concurrently
            }
        }
    }
}

class HealthTipReminderBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                HealthTipReminderScheduler(context).scheduleAll()
            }
        }
    }
}
