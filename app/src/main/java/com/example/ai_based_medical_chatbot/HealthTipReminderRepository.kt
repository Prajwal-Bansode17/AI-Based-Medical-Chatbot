package com.example.ai_based_medical_chatbot

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object HealthTipReminderRepository {

    const val PREFS_NAME = "medassist_health_tip_reminders"
    const val KEY_REMINDERS = "health_tip_reminders"
    private val gson = Gson()
    private val listType = object : TypeToken<List<HealthTipReminder>>() {}.type

    private fun defaultPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAll(context: Context): List<HealthTipReminder> =
        getAll(defaultPrefs(context))

    fun getAll(prefs: SharedPreferences): List<HealthTipReminder> {
        val json = prefs.getString(KEY_REMINDERS, null) ?: return emptyList()
        return try {
            val list: List<HealthTipReminder> = gson.fromJson(json, listType) ?: emptyList()
            list.sortedByDescending { it.createdAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getById(context: Context, id: String): HealthTipReminder? =
        getById(defaultPrefs(context), id)

    fun getById(prefs: SharedPreferences, id: String): HealthTipReminder? =
        getAll(prefs).firstOrNull { it.id == id }

    fun getEnabled(context: Context): List<HealthTipReminder> =
        getEnabled(defaultPrefs(context))

    fun getEnabled(prefs: SharedPreferences): List<HealthTipReminder> =
        getAll(prefs).filter { it.enabled }

    fun save(context: Context, reminder: HealthTipReminder): HealthTipReminder =
        save(defaultPrefs(context), reminder)

    fun save(prefs: SharedPreferences, reminder: HealthTipReminder): HealthTipReminder {
        val current = getAll(prefs).toMutableList()
        val index = current.indexOfFirst { it.id == reminder.id }

        if (index >= 0) {
            current[index] = reminder
        } else {
            // Deduplicate if identical title and time already exists
            val duplicateIndex = current.indexOfFirst {
                it.title.trim().equals(reminder.title.trim(), ignoreCase = true) &&
                    it.time.trim().equals(reminder.time.trim(), ignoreCase = true)
            }
            if (duplicateIndex >= 0) {
                current[duplicateIndex] = reminder.copy(id = current[duplicateIndex].id)
            } else {
                current.add(0, reminder)
            }
        }

        persist(prefs, current)
        return reminder
    }

    fun delete(context: Context, id: String): Boolean =
        delete(defaultPrefs(context), id)

    fun delete(prefs: SharedPreferences, id: String): Boolean {
        val current = getAll(prefs).toMutableList()
        val removed = current.removeAll { it.id == id }
        if (removed) {
            persist(prefs, current)
        }
        return removed
    }

    fun updateEnabled(context: Context, id: String, enabled: Boolean): Boolean =
        updateEnabled(defaultPrefs(context), id, enabled)

    fun updateEnabled(prefs: SharedPreferences, id: String, enabled: Boolean): Boolean {
        val current = getAll(prefs).toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            current[index] = current[index].copy(enabled = enabled)
            persist(prefs, current)
            return true
        }
        return false
    }

    private fun persist(prefs: SharedPreferences, list: List<HealthTipReminder>) {
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_REMINDERS, json).apply()
    }

    /**
     * Parses time strings such as "08:00 AM", "8:00 AM", "08:00", "20:30"
     * into hour (0..23) and minute (0..59).
     */
    fun parseHourAndMinute(timeStr: String): Pair<Int, Int>? {
        val formats = listOf("hh:mm a", "h:mm a", "HH:mm", "H:mm")
        for (pattern in formats) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                val parsed = parser.parse(timeStr.trim()) ?: continue
                val cal = Calendar.getInstance().apply { time = parsed }
                return Pair(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
            } catch (_: Exception) {
                // Try next
            }
        }
        return null
    }

    /**
     * Formats hour and minute into standard "hh:mm a" string (e.g. 8, 0 -> "08:00 AM")
     */
    fun formatHourAndMinute(hourOfDay: Int, minute: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        return SimpleDateFormat("hh:mm a", Locale.US).format(cal.time)
    }

    /**
     * Calculates the exact timestamp in milliseconds when the reminder should next ring,
     * given the reminder schedule and a reference time [fromMillis].
     */
    fun calculateNextTriggerMillis(
        reminder: HealthTipReminder,
        fromMillis: Long = System.currentTimeMillis()
    ): Long? {
        val (hour, minute) = parseHourAndMinute(reminder.time) ?: return null

        val now = Calendar.getInstance().apply { timeInMillis = fromMillis }

        return when (reminder.frequency) {
            HealthReminderFrequency.ONCE,
            HealthReminderFrequency.DAILY -> {
                val target = Calendar.getInstance().apply {
                    timeInMillis = fromMillis
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)

                    if (timeInMillis <= now.timeInMillis) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                target.timeInMillis
            }

            HealthReminderFrequency.WEEKLY -> {
                val targetDays = if (reminder.selectedDays.isNotEmpty()) {
                    reminder.selectedDays.toSet()
                } else {
                    setOf(now.get(Calendar.DAY_OF_WEEK))
                }

                var earliestTrigger: Long? = null

                // Search over the next 7 days for the next scheduled weekday
                for (offset in 0..7) {
                    val candidate = Calendar.getInstance().apply {
                        timeInMillis = fromMillis
                        add(Calendar.DAY_OF_YEAR, offset)
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    if (targetDays.contains(candidate.get(Calendar.DAY_OF_WEEK)) &&
                        candidate.timeInMillis > now.timeInMillis
                    ) {
                        if (earliestTrigger == null || candidate.timeInMillis < earliestTrigger) {
                            earliestTrigger = candidate.timeInMillis
                        }
                    }
                }

                earliestTrigger
            }
        }
    }

    /**
     * Finds the soonest upcoming enabled reminder from [fromMillis].
     */
    fun getNextScheduled(
        context: Context,
        fromMillis: Long = System.currentTimeMillis()
    ): Pair<HealthTipReminder, Long>? =
        getNextScheduled(defaultPrefs(context), fromMillis)

    fun getNextScheduled(
        prefs: SharedPreferences,
        fromMillis: Long = System.currentTimeMillis()
    ): Pair<HealthTipReminder, Long>? {
        val enabledList = getEnabled(prefs)
        var closestPair: Pair<HealthTipReminder, Long>? = null

        for (reminder in enabledList) {
            val trigger = calculateNextTriggerMillis(reminder, fromMillis) ?: continue
            if (trigger >= fromMillis) {
                if (closestPair == null || trigger < closestPair.second) {
                    closestPair = Pair(reminder, trigger)
                }
            }
        }

        return closestPair
    }
}
