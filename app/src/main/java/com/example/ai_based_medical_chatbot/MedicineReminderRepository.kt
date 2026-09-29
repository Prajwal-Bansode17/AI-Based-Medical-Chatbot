package com.example.ai_based_medical_chatbot

import android.content.Context
import java.util.UUID

/**
 * Local medicine-reminder storage.
 *
 * Stores:
 * - Medicine name
 * - Strength
 * - Reminder time
 * - Morning / Afternoon / Evening / Night slot
 * - Before / After food
 * - Duration
 * - Start date
 * - End date
 * - Enabled state
 *
 * This repository is intentionally independent from the prescription scanner UI.
 * It can later be connected to AlarmManager / WorkManager notifications.
 */
data class MedicineReminder(
    val id: String = UUID.randomUUID().toString(),
    val prescriptionId: String = "",
    val medicineName: String = "",
    val strength: String = "",
    val time: String = "",
    val daySlot: String = "",
    val mealTiming: String = "",
    val duration: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val instructions: String = "",
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

object MedicineReminderRepository {

    private const val PREFS_NAME = "medassist_medicine_reminders"
    private const val KEY_REMINDERS = "reminders"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(
        context: Context,
        reminder: MedicineReminder
    ): MedicineReminder {
        val reminders = getAll(context).toMutableList()

        reminders.removeAll { it.id == reminder.id }
        reminders.add(0, reminder)

        persist(context, reminders)
        return reminder
    }

    fun create(
        context: Context,
        prescriptionId: String,
        medicineName: String,
        strength: String = "",
        time: String = "",
        daySlot: String = "",
        mealTiming: String = "",
        duration: String = "",
        startDate: String = "",
        endDate: String = "",
        instructions: String = ""
    ): MedicineReminder {

        val reminder = MedicineReminder(
            prescriptionId = prescriptionId,
            medicineName = medicineName.trim(),
            strength = strength.trim(),
            time = time.trim(),
            daySlot = daySlot.trim(),
            mealTiming = mealTiming.trim(),
            duration = duration.trim(),
            startDate = startDate.trim(),
            endDate = endDate.trim(),
            instructions = instructions.trim()
        )

        return save(context, reminder)
    }

    fun getAll(context: Context): List<MedicineReminder> {
        val json = prefs(context).getString(KEY_REMINDERS, "[]") ?: "[]"

        return try {
            val array = org.json.JSONArray(json)
            val result = mutableListOf<MedicineReminder>()

            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue

                result.add(
                    MedicineReminder(
                        id = item.optString("id"),
                        prescriptionId = item.optString("prescriptionId"),
                        medicineName = item.optString("medicineName"),
                        strength = item.optString("strength"),
                        time = item.optString("time"),
                        daySlot = item.optString("daySlot"),
                        mealTiming = item.optString("mealTiming"),
                        duration = item.optString("duration"),
                        startDate = item.optString("startDate"),
                        endDate = item.optString("endDate"),
                        instructions = item.optString("instructions"),
                        enabled = item.optBoolean("enabled", true),
                        createdAt = item.optLong(
                            "createdAt",
                            System.currentTimeMillis()
                        )
                    )
                )
            }

            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getById(
        context: Context,
        reminderId: String
    ): MedicineReminder? {
        return getAll(context).firstOrNull {
            it.id == reminderId
        }
    }

    fun getForPrescription(
        context: Context,
        prescriptionId: String
    ): List<MedicineReminder> {
        if (prescriptionId.isBlank()) return emptyList()

        return getAll(context).filter {
            it.prescriptionId == prescriptionId
        }
    }

    fun getEnabled(
        context: Context
    ): List<MedicineReminder> {
        return getAll(context).filter {
            it.enabled
        }
    }

    fun updateEnabled(
        context: Context,
        reminderId: String,
        enabled: Boolean
    ): Boolean {
        val reminders = getAll(context).toMutableList()

        val index = reminders.indexOfFirst {
            it.id == reminderId
        }

        if (index == -1) return false

        reminders[index] = reminders[index].copy(
            enabled = enabled
        )

        persist(context, reminders)
        return true
    }

    fun update(
        context: Context,
        reminder: MedicineReminder
    ): Boolean {
        val reminders = getAll(context).toMutableList()

        val index = reminders.indexOfFirst {
            it.id == reminder.id
        }

        if (index == -1) return false

        reminders[index] = reminder
        persist(context, reminders)
        return true
    }

    fun delete(
        context: Context,
        reminderId: String
    ): Boolean {
        val reminders = getAll(context).toMutableList()
        val oldSize = reminders.size

        reminders.removeAll {
            it.id == reminderId
        }

        if (reminders.size == oldSize) return false

        persist(context, reminders)
        return true
    }

    fun deleteForPrescription(
        context: Context,
        prescriptionId: String
    ): Int {
        if (prescriptionId.isBlank()) return 0

        val reminders = getAll(context).toMutableList()
        val oldSize = reminders.size

        reminders.removeAll {
            it.prescriptionId == prescriptionId
        }

        persist(context, reminders)
        return oldSize - reminders.size
    }

    fun clearAll(context: Context) {
        prefs(context)
            .edit()
            .remove(KEY_REMINDERS)
            .apply()
    }

    private fun persist(
        context: Context,
        reminders: List<MedicineReminder>
    ) {
        val array = org.json.JSONArray()

        reminders.forEach { reminder ->
            array.put(
                org.json.JSONObject().apply {
                    put("id", reminder.id)
                    put("prescriptionId", reminder.prescriptionId)
                    put("medicineName", reminder.medicineName)
                    put("strength", reminder.strength)
                    put("time", reminder.time)
                    put("daySlot", reminder.daySlot)
                    put("mealTiming", reminder.mealTiming)
                    put("duration", reminder.duration)
                    put("startDate", reminder.startDate)
                    put("endDate", reminder.endDate)
                    put("instructions", reminder.instructions)
                    put("enabled", reminder.enabled)
                    put("createdAt", reminder.createdAt)
                }
            )
        }

        prefs(context)
            .edit()
            .putString(KEY_REMINDERS, array.toString())
            .apply()
    }
}
