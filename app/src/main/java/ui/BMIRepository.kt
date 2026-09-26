package ui

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class BMIData(
    val heightCm: Float = 0f,
    val weightKg: Float = 0f,
    val dateOfBirth: String = "",
    val gender: String = ""
) {
    val bmi: Float
        get() {
            if (heightCm <= 0f || weightKg <= 0f) return 0f

            val heightMeters = heightCm / 100f
            return weightKg / (heightMeters * heightMeters)
        }

    val bmiStatus: String
        get() {
            return when {
                bmi <= 0f -> "Not Available"
                bmi < 18.5f -> "Underweight"
                bmi < 25f -> "Normal"
                bmi < 30f -> "Overweight"
                else -> "Obese"
            }
        }

    val age: Int
        get() {
            if (dateOfBirth.isBlank()) return 0

            return try {
                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                format.isLenient = false

                val birthDate = format.parse(dateOfBirth) ?: return 0
                val birthCalendar = Calendar.getInstance().apply {
                    time = birthDate
                }

                val today = Calendar.getInstance()

                var calculatedAge =
                    today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)

                val currentMonth = today.get(Calendar.MONTH)
                val birthMonth = birthCalendar.get(Calendar.MONTH)

                if (
                    currentMonth < birthMonth ||
                    (
                            currentMonth == birthMonth &&
                                    today.get(Calendar.DAY_OF_MONTH) <
                                    birthCalendar.get(Calendar.DAY_OF_MONTH)
                            )
                ) {
                    calculatedAge--
                }

                calculatedAge.coerceAtLeast(0)
            } catch (e: Exception) {
                0
            }
        }
}

object BMIRepository {

    private const val PREFS_NAME = "medassist_bmi_preferences"

    private const val KEY_HEIGHT = "height_cm"
    private const val KEY_WEIGHT = "weight_kg"
    private const val KEY_DOB = "date_of_birth"
    private const val KEY_GENDER = "gender"

    fun saveBMIData(
        context: Context,
        heightCm: Float,
        weightKg: Float,
        dateOfBirth: String,
        gender: String
    ) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_HEIGHT, heightCm)
            .putFloat(KEY_WEIGHT, weightKg)
            .putString(KEY_DOB, dateOfBirth)
            .putString(KEY_GENDER, gender)
            .apply()
    }

    fun getBMIData(context: Context): BMIData {

        val preferences =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        return BMIData(
            heightCm = preferences.getFloat(KEY_HEIGHT, 0f),
            weightKg = preferences.getFloat(KEY_WEIGHT, 0f),
            dateOfBirth = preferences.getString(KEY_DOB, "") ?: "",
            gender = preferences.getString(KEY_GENDER, "") ?: ""
        )
    }

    fun hasBMIData(context: Context): Boolean {

        val data = getBMIData(context)

        return data.heightCm > 0f &&
                data.weightKg > 0f &&
                data.dateOfBirth.isNotBlank() &&
                data.gender.isNotBlank()
    }

    fun updateHeightAndWeight(
        context: Context,
        heightCm: Float,
        weightKg: Float
    ) {

        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_HEIGHT, heightCm)
            .putFloat(KEY_WEIGHT, weightKg)
            .apply()
    }

    fun updatePersonalDetails(
        context: Context,
        dateOfBirth: String,
        gender: String
    ) {

        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DOB, dateOfBirth)
            .putString(KEY_GENDER, gender)
            .apply()
    }

    fun clearBMIData(context: Context) {

        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}