package ui

import android.content.Context
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class BMIData(
    val heightCm: Float = 0f,
    val weightKg: Float = 0f,
    val dateOfBirth: String = "",
    val gender: String = "",
    val bloodGroup: String = "",
    val fullName: String = "",
    val lastCalculatedTimestamp: Long = 0L
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
                val clean = dateOfBirth.trim()
                val format = if (clean.contains("-")) {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                } else {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                }
                format.isLenient = false

                val birthDate = format.parse(clean) ?: return 0
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
    private const val KEY_BLOOD_GROUP = "blood_group"
    private const val KEY_FULL_NAME = "full_name"
    private const val KEY_TIMESTAMP = "last_calculated_timestamp"

    // Backwards-compatible / Dashboard cached keys
    private const val KEY_DASHBOARD_BMI = "bmi"
    private const val KEY_DASHBOARD_BMI_STATUS = "bmi_status"

    /**
     * Converts a DD/MM/YYYY date string to YYYY-MM-DD for standard storage and Supabase sync.
     */
    fun toIsoDate(dob: String): String {
        val clean = dob.trim()
        if (clean.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return clean
        if (clean.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))) {
            val parts = clean.split("/")
            return "${parts[2]}-${parts[1]}-${parts[0]}"
        }
        return clean
    }

    /**
     * Converts a YYYY-MM-DD date string to DD/MM/YYYY for friendly UI display.
     */
    fun toDisplayDate(dob: String): String {
        val clean = dob.trim()
        if (clean.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))) return clean
        if (clean.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            val parts = clean.split("-")
            return "${parts[2]}/${parts[1]}/${parts[0]}"
        }
        return clean
    }

    /**
     * Formats height or weight for clean user display with at most 2 decimal places
     * and without unnecessary trailing zeros (e.g. 182.37537 -> 182.38, 75.00 -> 75, 75.20 -> 75.2).
     */
    fun formatMeasurement(value: Double): String {
        if (value <= 0.0 || value.isNaN() || value.isInfinite()) return ""
        val df = DecimalFormat("0.##", DecimalFormatSymbols(Locale.US))
        return df.format(value)
    }

    fun formatMeasurement(value: Float): String = formatMeasurement(value.toDouble())

    fun parseDateOfBirth(value: String): Date? {
        val clean = value.trim()
        if (clean.length != 10) return null

        return try {
            val format = if (clean.contains("-")) {
                SimpleDateFormat("yyyy-MM-dd", Locale.US)
            } else {
                SimpleDateFormat("dd/MM/yyyy", Locale.US)
            }
            format.isLenient = false
            val parsed = format.parse(clean) ?: return null
            if (parsed.after(Calendar.getInstance().time)) null else parsed
        } catch (_: Exception) {
            null
        }
    }

    fun saveBMIData(
        context: Context,
        heightCm: Float,
        weightKg: Float,
        dateOfBirth: String,
        gender: String,
        bloodGroup: String = "",
        fullName: String = ""
    ) {
        val current = getBMIData(context)
        val finalHeight = if (heightCm > 0f) heightCm else current.heightCm
        val finalWeight = if (weightKg > 0f) weightKg else current.weightKg
        val finalDob = if (dateOfBirth.isNotBlank()) toDisplayDate(dateOfBirth) else current.dateOfBirth
        val finalGender = if (gender.isNotBlank()) gender else current.gender
        val finalBlood = if (bloodGroup.isNotBlank()) bloodGroup else current.bloodGroup
        val finalName = if (fullName.isNotBlank()) fullName else current.fullName

        val data = BMIData(
            heightCm = finalHeight,
            weightKg = finalWeight,
            dateOfBirth = finalDob,
            gender = finalGender,
            bloodGroup = finalBlood,
            fullName = finalName,
            lastCalculatedTimestamp = System.currentTimeMillis()
        )

        val editor = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_HEIGHT, finalHeight)
            .putFloat(KEY_WEIGHT, finalWeight)
            .putString(KEY_DOB, finalDob)
            .putString(KEY_GENDER, finalGender)
            .putString(KEY_BLOOD_GROUP, finalBlood)
            .putString(KEY_FULL_NAME, finalName)
            .putLong(KEY_TIMESTAMP, data.lastCalculatedTimestamp)

        // Cache calculated BMI and status for Dashboard & widgets
        if (data.bmi > 0f) {
            editor.putFloat(KEY_DASHBOARD_BMI, data.bmi)
            editor.putString(KEY_DASHBOARD_BMI_STATUS, data.bmiStatus)
        }

        editor.apply()
    }

    fun getBMIData(context: Context): BMIData {
        val preferences =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val height = preferences.getFloat(KEY_HEIGHT, 0f)
        val weight = preferences.getFloat(KEY_WEIGHT, 0f)
        val dob = preferences.getString(KEY_DOB, "") ?: ""
        val gender = preferences.getString(KEY_GENDER, "") ?: ""
        val bloodGroup = preferences.getString(KEY_BLOOD_GROUP, "") ?: ""
        val fullName = preferences.getString(KEY_FULL_NAME, "") ?: ""
        val timestamp = preferences.getLong(KEY_TIMESTAMP, 0L)

        return BMIData(
            heightCm = height,
            weightKg = weight,
            dateOfBirth = dob,
            gender = gender,
            bloodGroup = bloodGroup,
            fullName = fullName,
            lastCalculatedTimestamp = timestamp
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
        val current = getBMIData(context)
        val finalHeight = if (heightCm > 0f) heightCm else current.heightCm
        val finalWeight = if (weightKg > 0f) weightKg else current.weightKg
        val updated = current.copy(
            heightCm = finalHeight,
            weightKg = finalWeight,
            lastCalculatedTimestamp = System.currentTimeMillis()
        )

        val editor = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_HEIGHT, finalHeight)
            .putFloat(KEY_WEIGHT, finalWeight)
            .putLong(KEY_TIMESTAMP, updated.lastCalculatedTimestamp)

        if (updated.bmi > 0f) {
            editor.putFloat(KEY_DASHBOARD_BMI, updated.bmi)
            editor.putString(KEY_DASHBOARD_BMI_STATUS, updated.bmiStatus)
        }

        editor.apply()
    }

    fun updateHeight(context: Context, heightCm: Float) {
        if (heightCm <= 0f) return
        val current = getBMIData(context)
        updateHeightAndWeight(context, heightCm, current.weightKg)
    }

    fun updateWeight(context: Context, weightKg: Float) {
        if (weightKg <= 0f) return
        val current = getBMIData(context)
        updateHeightAndWeight(context, current.heightCm, weightKg)
    }

    fun updateFullName(context: Context, fullName: String) {
        val clean = fullName.trim()
        if (clean.isBlank()) return
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FULL_NAME, clean)
            .apply()
    }

    fun updateGender(context: Context, gender: String) {
        val clean = gender.trim()
        if (clean.isBlank()) return
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_GENDER, clean)
            .apply()
    }

    fun updateBloodGroup(context: Context, bloodGroup: String) {
        val clean = bloodGroup.trim()
        if (clean.isBlank()) return
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BLOOD_GROUP, clean)
            .apply()
    }

    fun updateDateOfBirth(context: Context, dateOfBirth: String) {
        val clean = dateOfBirth.trim()
        if (clean.isBlank()) return
        // Only save if valid calendar date and not in the future
        if (parseDateOfBirth(clean) == null) return
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DOB, toDisplayDate(clean))
            .apply()
    }

    fun updatePersonalDetails(
        context: Context,
        dateOfBirth: String,
        gender: String,
        bloodGroup: String = "",
        fullName: String = ""
    ) {
        val current = getBMIData(context)
        val finalDob = if (dateOfBirth.isNotBlank() && parseDateOfBirth(dateOfBirth) != null) {
            toDisplayDate(dateOfBirth)
        } else current.dateOfBirth
        val finalGender = if (gender.isNotBlank()) gender else current.gender
        val finalBlood = if (bloodGroup.isNotBlank()) bloodGroup else current.bloodGroup
        val finalName = if (fullName.isNotBlank()) fullName else current.fullName

        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DOB, finalDob)
            .putString(KEY_GENDER, finalGender)
            .putString(KEY_BLOOD_GROUP, finalBlood)
            .putString(KEY_FULL_NAME, finalName)
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
