package com.example.ai_based_medical_chatbot

import com.google.gson.Gson
import java.util.UUID

enum class HealthReminderFrequency {
    ONCE,
    DAILY,
    WEEKLY
}

data class HealthTipReminder(
    val id: String = UUID.randomUUID().toString(),
    val tipId: Int = 0,
    val title: String = "",
    val description: String = "",
    val time: String = "08:00 AM",
    val frequency: HealthReminderFrequency = HealthReminderFrequency.DAILY,
    val selectedDays: List<Int> = emptyList(), // java.util.Calendar.SUNDAY (1) .. java.util.Calendar.SATURDAY (7)
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): String = gson.toJson(this)

    companion object {
        private val gson = Gson()

        fun fromJson(jsonStr: String): HealthTipReminder =
            gson.fromJson(jsonStr, HealthTipReminder::class.java)
    }
}
