package ui

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.core.content.ContextCompat

import com.example.ai_based_medical_chatbot.R

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslateLanguage

import org.json.JSONArray
import org.json.JSONObject



// ============================================================================
// SAVED PRESCRIPTION MODELS
// ============================================================================

data class PrescriptionMedicineDraft(
    val medicineName: String = "",
    val strength: String = "",
    val frequency: String = "",
    val duration: String = "",
    val instructions: String = ""
)

data class PrescriptionDraft(
    val doctorName: String = "",
    val patientName: String = "",
    val date: String = "",
    val diagnosis: String = "",
    val medicines: List<PrescriptionMedicineDraft> = listOf(PrescriptionMedicineDraft())
)

private data class MedicineVerification(
    val recognized: Boolean,
    val matchedName: String = ""
)


private fun normalizeMedicineQuery(value: String): String =
    value.trim().lowercase().replace(Regex("[^a-z0-9]+"), "")

private fun levenshteinDistance(a: String, b: String): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length

    var previous = IntArray(b.length + 1) { it }
    var current = IntArray(b.length + 1)

    for (i in 1..a.length) {
        current[0] = i
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            current[j] = minOf(
                current[j - 1] + 1,
                previous[j] + 1,
                previous[j - 1] + cost
            )
        }
        val swap = previous
        previous = current
        current = swap
    }
    return previous[b.length]
}

private fun verifyMedicineInDataset(
    context: Context,
    medicineName: String
): MedicineVerification {
    val query = normalizeMedicineQuery(medicineName)
    if (query.isBlank()) return MedicineVerification(false)

    val medicines = MedicineRepository.getMedicines(context)

    // Exact normalized match first.
    medicines.firstOrNull { record ->
        listOf(record.name, record.generic, record.brand)
            .any { normalizeMedicineQuery(it) == query }
    }?.let { record ->
        return MedicineVerification(
            recognized = true,
            matchedName = record.name.ifBlank { record.generic.ifBlank { record.brand } }
        )
    }

    // Fuzzy OCR correction. This is deliberately conservative:
    // short OCR strings need a very close match; longer names may tolerate
    // a few character errors.
    var bestName = ""
    var bestDistance = Int.MAX_VALUE

    medicines.forEach { record ->
        listOf(record.name, record.generic, record.brand).forEach { candidate ->
            val normalized = normalizeMedicineQuery(candidate)
            if (normalized.length < 4 || query.length < 4) return@forEach

            val distance = levenshteinDistance(query, normalized)
            val maxAllowed = when {
                query.length <= 5 -> 1
                query.length <= 8 -> 2
                query.length <= 12 -> 3
                else -> 4
            }

            val containsMatch =
                normalized.contains(query) || query.contains(normalized)

            if ((distance <= maxAllowed || containsMatch) && distance < bestDistance) {
                bestDistance = distance
                bestName = candidate
            }
        }
    }

    return if (bestName.isNotBlank()) {
        MedicineVerification(true, bestName)
    } else {
        MedicineVerification(false)
    }
}

object PrescriptionRepository {



    private const val PREFS_NAME = "medassist_prescription_preferences"
    private const val KEY_PRESCRIPTIONS = "saved_prescriptions"

    fun save(context: Context, draft: PrescriptionDraft): String {
        val id = "rx_${System.currentTimeMillis()}"

        val item = JSONObject().apply {
            put("id", id)
            put("savedAt", System.currentTimeMillis())
            put("doctorName", draft.doctorName)
            put("patientName", draft.patientName)
            put("date", draft.date)
            put("diagnosis", draft.diagnosis)

            val medicineArray = JSONArray()
            draft.medicines.forEach { medicine ->
                medicineArray.put(
                    JSONObject().apply {
                        put("medicineName", medicine.medicineName)
                        put("strength", medicine.strength)
                        put("frequency", medicine.frequency)
                        put("duration", medicine.duration)
                        put("instructions", medicine.instructions)
                    }
                )
            }
            put("medicines", medicineArray)
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = JSONArray(prefs.getString(KEY_PRESCRIPTIONS, "[]") ?: "[]")
        val updated = JSONArray()

        updated.put(item)
        for (i in 0 until current.length()) {
            updated.put(current.getJSONObject(i))
        }

        prefs.edit().putString(KEY_PRESCRIPTIONS, updated.toString()).apply()
        return id
    }

    fun getSavedPrescriptions(context: Context): JSONArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return JSONArray(prefs.getString(KEY_PRESCRIPTIONS, "[]") ?: "[]")
    }

    fun delete(context: Context, id: String) {
        val current = getSavedPrescriptions(context)
        val updated = JSONArray()

        for (i in 0 until current.length()) {
            val item = current.getJSONObject(i)
            if (item.optString("id") != id) {
                updated.put(item)
            }
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PRESCRIPTIONS, updated.toString())
            .apply()
    }
}

private fun valueAfterLabel(line: String, labels: List<String>): String {
    val lower = line.lowercase()
    for (label in labels) {
        val index = lower.indexOf(label.lowercase())
        if (index >= 0) {
            return line.substring(index + label.length).trim().trim(':', '-', ' ')
        }
    }
    return ""
}


private fun extractPrescriptionDraft(text: String): PrescriptionDraft {
    val rawLines = text.lines()
        .map { it.replace('\u00A0', ' ').trim() }
        .filter { it.isNotBlank() }

    fun clean(value: String): String =
        value.replace(Regex("\\s+"), " ")
            .trim()
            .trim(':', '-', '–', '—', '.', ',', ';', ' ')

    fun normalize(value: String): String =
        clean(value).lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()

    fun isInstructionLike(value: String): Boolean {
        val v = normalize(value)
        return v.contains("drink plenty") ||
                v.contains("follow up") ||
                v.contains("after food") ||
                v.contains("before food") ||
                v.contains("with food") ||
                v.contains("empty stomach") ||
                v.contains("at bedtime") ||
                v.contains("apply") ||
                v.contains("do not crush") ||
                v.contains("take with") ||
                v.contains("swallow") ||
                v.contains("continue") ||
                v.contains("stop") ||
                v.contains("avoid")
    }

    fun cleanPatient(value: String): String {
        return clean(value)
            .replace(Regex("(?i)^(mr|mrs|ms|miss)\\.?\\s+"), "")
            .trim()
    }

    fun isBadField(value: String): Boolean {
        val v = normalize(value)
        return v.isBlank() ||
                v in setOf("rx", "r x", "dx", "d x", "diagnosis", "diagnoses", "doctor", "doctor name")
    }

    fun extractDate(value: String): String {
        val patterns = listOf(
            Regex("""(?i)\b\d{1,2}[-/]\d{1,2}[-/]\d{2,4}\b"""),
            Regex("""(?i)\b\d{1,2}[-/][A-Za-z]{3,9}[-/]\d{2,4}\b"""),
            Regex("""(?i)\b\d{1,2}\s+[A-Za-z]{3,9}\s+\d{2,4}\b""")
        )
        return patterns.firstNotNullOfOrNull { it.find(value)?.value?.let(::clean) } ?: ""
    }

    fun valueAfterLabel(line: String, labels: List<String>): String {
        val lower = line.lowercase()
        for (label in labels) {
            val index = lower.indexOf(label.lowercase())
            if (index >= 0) {
                return clean(line.substring(index + label.length))
            }
        }
        return ""
    }

    // ---------- Doctor ----------
    fun extractDoctor(line: String): String {
        val m = Regex(
            """(?i)\b(?:dr|doctor|consultant|physician)\.?\s*[:.-]?\s*(.+)$"""
        ).find(line) ?: return ""

        var candidate = clean(m.groupValues[1])

        // OCR sometimes glues the doctor's name to advice printed later.
        val stopRegex = Regex(
            """(?i)\b(?:patient|date|diagnosis|diagnoses|rx|medicine|medicines|drink|drinking|plenty|water|follow|follow\s+up|take|tablet|tab|capsule|cap|syrup|cream|ointment|apply|advice|instructions)\b"""
        )
        val stop = stopRegex.find(candidate)
        if (stop != null) candidate = clean(candidate.substring(0, stop.range.first))

        candidate = candidate
            .replace(Regex("""\b(?:mbbs|md|ms|dm|dnb|frcs|phd)\b.*$""", RegexOption.IGNORE_CASE), "")
            .trim()

        if (candidate.length < 2 || isInstructionLike(candidate)) return ""
        if (candidate.split(" ").size > 5) {
            candidate = candidate.split(" ").take(5).joinToString(" ")
        }

        return if (candidate.any { it.isLetter() }) candidate else ""
    }

    var doctor = ""
    var patient = ""
    var date = ""
    var diagnosis = ""

    rawLines.forEachIndexed { index, line ->
        val lower = line.lowercase()
        val next = rawLines.getOrNull(index + 1).orEmpty()

        if (doctor.isBlank()) {
            val labelled = valueAfterLabel(
                line,
                listOf("doctor name", "doctor", "consultant", "physician")
            )
            val labelledDoctor = if (labelled.isNotBlank()) extractDoctor("Dr. $labelled") else ""
            doctor = labelledDoctor.ifBlank { extractDoctor(line) }

            if (doctor.isBlank() && lower.matches(Regex("""(?:dr|doctor|doctor name)\.?\s*"""))) {
                doctor = clean(next)
            }
        }

        if (patient.isBlank()) {
            val labelled = valueAfterLabel(line, listOf("patient name", "patient"))
            if (labelled.isNotBlank() && !isBadField(labelled)) {
                patient = cleanPatient(labelled)
            } else if (Regex("""(?i)^(mr|mrs|ms|miss)\.?\s+""").containsMatchIn(line)) {
                patient = cleanPatient(line)
            }
        }

        if (date.isBlank()) {
            val same = extractDate(line)
            val nextDate = extractDate(next)
            if (same.isNotBlank()) date = same
            else if (lower.contains("date") && nextDate.isNotBlank()) date = nextDate
        }

        if (diagnosis.isBlank()) {
            val labelled = valueAfterLabel(
                line,
                listOf("provisional diagnosis", "final diagnosis", "diagnosis", "diagnoses", "dx")
            )
            if (!isBadField(labelled) && !isInstructionLike(labelled)) {
                diagnosis = labelled
            } else if (
                lower.contains("diagnosis") || Regex("""(?i)^\s*dx\b""").containsMatchIn(line)
            ) {
                val nextValue = clean(next)
                if (!isBadField(nextValue) && !isInstructionLike(nextValue)) {
                    diagnosis = nextValue
                }
            }
        }
    }

    // ---------- Medicine / dosage helpers ----------
    val formRegex = Regex(
        """(?i)\b(?:tab(?:let)?s?|cap(?:sule)?s?|syp|syrup|cream|ointment|gel|drops?|inj(?:ection)?|sachet|susp(?:ension)?)\b"""
    )
    val strengthRegex = Regex(
        """(?i)\b\d+(?:\.\d+)?\s*(?:mg|mcg|g|kg|ml|iu|units?|%)\b"""
    )
    val frequencyRegexes = listOf(
        Regex("""(?i)\b(?:od|bd|tds|qid|qhs|hs|sos|stat|once|twice|thrice|daily|weekly)\b"""),
        Regex("""(?i)\b(?:once|twice|thrice)\s+(?:a|per)\s+(?:day|daily)\b"""),
        Regex("""(?i)\b(?:every|each)\s+\d+\s+(?:hours?|hrs?|days?)\b"""),
        Regex("""\b\d+\s*[-/]\s*\d+\s*[-/]\s*\d+\b"""),
        Regex("""\b\d+\s*(?:times?)\s*(?:a|per)\s*day\b""")
    )
    val durationRegexes = listOf(
        Regex("""(?i)\bfor\s+\d+(?:\.\d+)?\s*(?:days?|d|weeks?|wks?|months?|m)\b"""),
        Regex("""(?i)\b\d+(?:\.\d+)?\s*(?:days?|d|weeks?|wks?|months?|m)\b"""),
        Regex("""(?i)\b\d+\s*[dDwWmM]\b""")
    )
    val instructionRegexes = listOf(
        Regex("""(?i)\b(?:before|after|with)\s+(?:food|meal|breakfast|lunch|dinner)\b"""),
        Regex("""(?i)\bempty\s+stomach\b"""),
        Regex("""(?i)\bat\s+bedtime\b"""),
        Regex("""(?i)\bdrink\s+plenty\s+of\s+water\b"""),
        Regex("""(?i)\bfollow\s+up(?:\s+after\s+\d+\s*(?:days?|weeks?|months?))?\b"""),
        Regex("""(?i)\bdo\s+not\s+crush\b"""),
        Regex("""(?i)\bapply\s+(?:thinly|locally|to\s+\w+)\b"""),
        Regex("""(?i)\btake\s+(?:with|after|before)\s+[^,.;\n]+""")
    )

    fun firstMatch(regexes: List<Regex>, value: String): String =
        regexes.firstNotNullOfOrNull { it.find(value)?.value?.let(::clean) } ?: ""

    fun looksLikeMedicine(line: String): Boolean {
        val lower = line.lowercase()
        if (line.length > 180) return false
        if (lower.contains("blood pressure") || lower.contains("temperature") ||
            lower.contains("weight") || lower.contains("height") ||
            lower.contains("pulse") || lower.contains("diagnosis") ||
            lower.contains("doctor") || lower.contains("patient")
        ) return false

        if (formRegex.containsMatchIn(line) || strengthRegex.containsMatchIn(line)) return true

        val letters = line.filter { it.isLetter() }
        val words = line.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val mostlyUpper = letters.isNotEmpty() &&
                letters.count { it.isUpperCase() }.toDouble() / letters.length >= 0.75
        return letters.length in 4..35 && words.size <= 4 && mostlyUpper
    }

    fun medicineNameFromLine(line: String): String {
        var name = line
        name = strengthRegex.replace(name, " ")
        name = formRegex.replace(name, " ")
        name = Regex("""(?i)\b(?:od|bd|tds|qid|qhs|hs|sos|stat|once|twice|thrice|daily|weekly)\b""")
            .replace(name, " ")
        name = Regex("""\b\d+\s*[-/]\s*\d+\s*[-/]\s*\d+\b""").replace(name, " ")
        name = Regex("""(?i)\bfor\s+\d+(?:\.\d+)?\s*(?:days?|d|weeks?|wks?|months?|m)\b""")
            .replace(name, " ")
        name = Regex("""[:\-–—]+""").replace(name, " ")
        name = Regex("""[*#•]+""").replace(name, " ")
        name = Regex("""\s+""").replace(name, " ").trim()
        name = name.replace(Regex("""(?i)^(?:rx|r\s*x)\s+"""), "").trim()
        return clean(name)
    }

    val medicineStartIndices = rawLines.mapIndexedNotNull { index, line ->
        if (looksLikeMedicine(line)) index else null
    }

    val medicines = mutableListOf<PrescriptionMedicineDraft>()

    medicineStartIndices.forEachIndexed { position, startIndex ->
        val endExclusive = medicineStartIndices
            .getOrNull(position + 1)
            ?: minOf(rawLines.size, startIndex + 5)

        val blockLines = rawLines.subList(startIndex, endExclusive)
        val block = blockLines.joinToString(" ")

        val name = medicineNameFromLine(rawLines[startIndex])
        if (name.isBlank()) return@forEachIndexed

        val strength = strengthRegex.find(block)?.value?.let(::clean) ?: ""
        val frequency = firstMatch(frequencyRegexes, block)
        val duration = firstMatch(durationRegexes, block)
        val instruction = firstMatch(instructionRegexes, block)

        medicines.add(
            PrescriptionMedicineDraft(
                medicineName = name,
                strength = strength,
                frequency = frequency,
                duration = duration,
                instructions = instruction
            )
        )
    }

    // If diagnosis was not explicitly labelled, only use a high-confidence
    // medical condition found before the first medicine. Never use Rx/advice text.
    if (diagnosis.isBlank()) {
        val diagnosisTerms = listOf(
            "hypertension", "diabetes", "migraine", "asthma", "pneumonia",
            "bronchitis", "gastritis", "infection", "fever", "cough",
            "common cold", "chickenpox", "thyroid", "hypothyroidism",
            "hyperthyroidism", "allergy", "allergic rhinitis", "sinusitis",
            "tonsillitis", "pharyngitis", "otitis", "uti", "urinary infection",
            "dermatitis", "eczema", "acidity", "viral infection", "bacterial infection",
            "fungal infection", "pain", "headache", "flu", "rhinitis"
        )
        val beforeMedicines = if (medicineStartIndices.isNotEmpty()) {
            rawLines.take(medicineStartIndices.first())
        } else {
            rawLines
        }

        val found = beforeMedicines.firstOrNull { line ->
            val n = normalize(line)
            !isInstructionLike(line) && diagnosisTerms.any { n.contains(it) }
        }
        if (found != null) diagnosis = clean(found)
    }

    val uniqueMedicines = medicines.distinctBy {
        "${normalize(it.medicineName)}|${normalize(it.strength)}|${normalize(it.frequency)}|${normalize(it.duration)}"
    }

    return PrescriptionDraft(
        doctorName = doctor,
        patientName = patient,
        date = date,
        diagnosis = diagnosis,
        medicines = uniqueMedicines.ifEmpty {
            listOf(PrescriptionMedicineDraft())
        }
    )
}

@Composable
fun PrescriptionScannerScreen(
    onBackClick: () -> Unit = {}
) {

    val context = LocalContext.current
    val verificationScope = rememberCoroutineScope()

    val primary = Color(0xFF087EA4)
    val darkBlue = Color(0xFF123A56)
    val gray = Color(0xFF71818C)

    val backgroundTop = Color(0xFFEAF8FC)
    val backgroundBottom = Color(0xFFD8F0F6)

    // Review & Edit fields: keep extracted/editable text clearly visible.
    val reviewFieldColors = TextFieldDefaults.colors(
        focusedTextColor = darkBlue,
        unfocusedTextColor = darkBlue,
        disabledTextColor = darkBlue,
        focusedLabelColor = primary,
        unfocusedLabelColor = gray,
        disabledLabelColor = gray,
        focusedPlaceholderColor = gray,
        unfocusedPlaceholderColor = gray,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        disabledContainerColor = Color.White,
        cursorColor = primary,
        focusedIndicatorColor = primary,
        unfocusedIndicatorColor = gray,
        disabledIndicatorColor = gray
    )

    var selectedImageUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var cameraUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var extractedText by remember {
        mutableStateOf("")
    }

    var translatedText by remember {
        mutableStateOf("")
    }

    var isProcessing by remember {
        mutableStateOf(false)
    }

    var ocrError by remember {
        mutableStateOf("")
    }

    var isReviewMode by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }
    var savedPrescriptionId by remember { mutableStateOf("") }

    var doctorName by remember { mutableStateOf("") }
    var patientName by remember { mutableStateOf("") }
    var prescriptionDate by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }
    var medicineRows by remember {
        mutableStateOf(listOf(PrescriptionMedicineDraft()))
    }

    var medicineVerification by remember {
        mutableStateOf(emptyMap<Int, MedicineVerification>())
    }

    var isVerifyingMedicines by remember {
        mutableStateOf(false)
    }

    // Prevent an older OCR callback from overwriting a newer prescription.
    var ocrRequestId by remember { mutableStateOf(0L) }


    // =============================================================
    // LANGUAGE
    // =============================================================

    var selectedLanguage by remember {
        mutableStateOf("English")
    }

    var languageMenuExpanded by remember {
        mutableStateOf(false)
    }


    // =============================================================
    // LANGUAGE TEXT
    // =============================================================

    val scannerTitle = when (selectedLanguage) {

        "मराठी" -> "प्रिस्क्रिप्शन स्कॅनर"

        "हिन्दी" -> "प्रिस्क्रिप्शन स्कैनर"

        else -> "Prescription Scanner"
    }


    val scannerDescription = when (selectedLanguage) {

        "मराठी" ->
            "प्रिस्क्रिप्शन स्कॅन करा आणि त्यातील माहिती सहज समजून घ्या."

        "हिन्दी" ->
            "प्रिस्क्रिप्शन स्कैन करें और जानकारी को आसानी से समझें."

        else ->
            "Scan a prescription and understand the information more easily."
    }


    val scanTitle = when (selectedLanguage) {

        "मराठी" ->
            "तुमचे प्रिस्क्रिप्शन स्कॅन करा"

        "हिन्दी" ->
            "अपना प्रिस्क्रिप्शन स्कैन करें"

        else ->
            "Scan your prescription"
    }


    val scanDescription = when (selectedLanguage) {

        "मराठी" ->
            "स्पष्ट फोटो काढा किंवा आधीपासून असलेले प्रिस्क्रिप्शन निवडा."

        "हिन्दी" ->
            "एक साफ फोटो लें या मौजूदा प्रिस्क्रिप्शन चुनें."

        else ->
            "Take a clear photo or select an existing prescription."
    }


    val selectedTitle = when (selectedLanguage) {

        "मराठी" ->
            "प्रिस्क्रिप्शन निवडले"

        "हिन्दी" ->
            "प्रिस्क्रिप्शन चुना गया"

        else ->
            "Prescription selected"
    }


    val chooseAgainText = when (selectedLanguage) {

        "मराठी" ->
            "पुन्हा निवडा"

        "हिन्दी" ->
            "फिर से चुनें"

        else ->
            "Choose Again"
    }


    val retakeText = when (selectedLanguage) {

        "मराठी" ->
            "पुन्हा फोटो"

        "हिन्दी" ->
            "फिर से फोटो"

        else ->
            "Retake"
    }


    val readingText = when (selectedLanguage) {

        "मराठी" ->
            "प्रिस्क्रिप्शन वाचत आहे..."

        "हिन्दी" ->
            "प्रिस्क्रिप्शन पढ़ा जा रहा है..."

        else ->
            "Reading prescription..."
    }


    val extractedTitle = when (selectedLanguage) {

        "मराठी" ->
            "📝 मिळालेली माहिती"

        "हिन्दी" ->
            "📝 निकाली गई जानकारी"

        else ->
            "📝 Extracted Information"
    }


    val languageLabel = when (selectedLanguage) {

        "मराठी" ->
            "🌐 परिणामाची भाषा"

        "हिन्दी" ->
            "🌐 परिणाम की भाषा"

        else ->
            "🌐 Result language"
    }


    val warningText = when (selectedLanguage) {

        "मराठी" ->
            "⚠ हस्तलिखित प्रिस्क्रिप्शन OCR द्वारे अचूकपणे वाचले जाऊ शकत नाही. औषधाचे नाव, strength आणि dosage डॉक्टर किंवा फार्मासिस्टकडून नेहमी तपासा."

        "हिन्दी" ->
            "⚠ हस्तलिखित प्रिस्क्रिप्शन को OCR सही तरीके से नहीं पढ़ सकता। दवा का नाम, strength और dosage डॉक्टर या फार्मासिस्ट से हमेशा सत्यापित करें."

        else ->
            "⚠ Handwritten prescriptions may not be read accurately by OCR. Always verify extracted medicine names, strength and dosage with a doctor or pharmacist."
    }


    val reviewTitle = when (selectedLanguage) {
        "मराठी" -> "✏️ माहिती तपासा व बदला"
        "हिन्दी" -> "✏️ जानकारी जाँचें और बदलें"
        else -> "✏️ Review & Edit"
    }
    val doctorLabel = when (selectedLanguage) {
        "मराठी" -> "डॉक्टरचे नाव"
        "हिन्दी" -> "डॉक्टर का नाम"
        else -> "Doctor Name"
    }
    val patientLabel = when (selectedLanguage) {
        "मराठी" -> "रुग्णाचे नाव"
        "हिन्दी" -> "मरीज़ का नाम"
        else -> "Patient Name"
    }
    val dateLabel = when (selectedLanguage) {
        "मराठी" -> "तारीख"
        "हिन्दी" -> "तारीख"
        else -> "Date"
    }
    val diagnosisLabel = when (selectedLanguage) {
        "मराठी" -> "निदान"
        "हिन्दी" -> "निदान"
        else -> "Diagnosis"
    }
    val medicinesLabel = when (selectedLanguage) {
        "मराठी" -> "💊 औषधे"
        "हिन्दी" -> "💊 दवाइयाँ"
        else -> "💊 Medicines"
    }
    val medicineNameLabel = when (selectedLanguage) {
        "मराठी" -> "औषधाचे नाव"
        "हिन्दी" -> "दवा का नाम"
        else -> "Medicine Name"
    }
    val strengthLabel = "Strength"
    val frequencyLabel = "Frequency / Schedule"
    val durationLabel = when (selectedLanguage) {
        "मराठी" -> "कालावधी"
        "हिन्दी" -> "अवधि"
        else -> "Duration"
    }
    val instructionsLabel = when (selectedLanguage) {
        "मराठी" -> "सूचना"
        "हिन्दी" -> "निर्देश"
        else -> "Instructions"
    }
    val addMedicineText = when (selectedLanguage) {
        "मराठी" -> "+ आणखी औषध"
        "हिन्दी" -> "+ एक और दवा"
        else -> "+ Add Medicine"
    }
    val savePrescriptionText = when (selectedLanguage) {
        "मराठी" -> "💾 प्रिस्क्रिप्शन सेव्ह करा"
        "हिन्दी" -> "💾 प्रिस्क्रिप्शन सेव करें"
        else -> "💾 Save Prescription"
    }
    val summaryTitle = when (selectedLanguage) {
        "मराठी" -> "📋 प्रिस्क्रिप्शन सारांश"
        "हिन्दी" -> "📋 प्रिस्क्रिप्शन सारांश"
        else -> "📋 Prescription Summary"
    }
    val savedText = when (selectedLanguage) {
        "मराठी" -> "✓ प्रिस्क्रिप्शन यशस्वीपणे सेव्ह झाले"
        "हिन्दी" -> "✓ प्रिस्क्रिप्शन सफलतापूर्वक सेव हो गया"
        else -> "✓ Prescription saved successfully"
    }
    val verifyText = when (selectedLanguage) {
        "मराठी" -> "⚠ OCR माहिती फक्त reference साठी आहे. औषधाचे नाव, strength आणि dosage डॉक्टर किंवा फार्मासिस्टकडून verify करा."
        "हिन्दी" -> "⚠ OCR जानकारी केवल reference के लिए है। दवा का नाम, strength और dosage डॉक्टर या फार्मासिस्ट से verify करें."
        else -> "⚠ OCR information is for reference only. Verify medicine name, strength and dosage with a doctor or pharmacist."
    }

    // =============================================================
    // PROTECT IMPORTANT PRESCRIPTION VALUES
    // =============================================================

    fun shouldKeepPrescriptionLine(
        line: String
    ): Boolean {

        val value = line.trim()

        if (value.isEmpty()) {
            return true
        }

        val hasNumber = value.any {
            it.isDigit()
        }

        val hasMedicineUnit = Regex(
            "(?i)\\b(ml|mg|mcg|g|kg|mmhg|bpm|iu|units?|tablet|tab|capsule|cap|cream|ointment|drops?)\\b"
        ).containsMatchIn(value)

        val hasDosagePattern = Regex(
            "(?i)\\b(od|bd|tds|qid|hs|sos|stat|once|twice|thrice)\\b"
        ).containsMatchIn(value)

        val hasFrequencyPattern = Regex(
            "\\b\\d+\\s*[-/]\\s*\\d+\\s*[-/]\\s*\\d+\\b"
        ).containsMatchIn(value)

        return hasNumber &&
                (
                        hasMedicineUnit ||
                                hasDosagePattern ||
                                hasFrequencyPattern
                        )
    }


    // =============================================================
    // SENSITIVE VALUE LINE
    // =============================================================

    fun isSensitiveValueLine(
        line: String
    ): Boolean {

        val lower = line.lowercase()

        return lower.contains("patient name") ||
                lower.contains("doctor name") ||
                lower.contains("patient id") ||
                lower.contains("doctor id") ||
                lower.contains("phone") ||
                lower.contains("mobile") ||
                lower.contains("date") ||
                lower.contains("address") ||
                lower.contains("mr.") ||
                lower.contains("mrs.") ||
                lower.contains("ms.")
    }


    // =============================================================
    // TRANSLATE PRESCRIPTION
    // =============================================================

    fun translatePrescriptionText(
        sourceText: String,
        language: String
    ) {

        if (sourceText.isBlank()) {

            translatedText = ""
            isProcessing = false

            return
        }


        // ---------------------------------------------------------
        // ENGLISH
        // ---------------------------------------------------------

        if (language == "English") {

            translatedText = sourceText
            isProcessing = false

            return
        }


        isProcessing = true
        ocrError = ""


        val targetLanguage =
            if (language == "मराठी") {

                TranslateLanguage.MARATHI

            } else {

                TranslateLanguage.HINDI
            }


        val options =
            TranslatorOptions.Builder()
                .setSourceLanguage(
                    TranslateLanguage.ENGLISH
                )
                .setTargetLanguage(
                    targetLanguage
                )
                .build()


        val translator =
            Translation.getClient(options)


        // ---------------------------------------------------------
        // DOWNLOAD LANGUAGE MODEL
        // ---------------------------------------------------------

        translator
            .downloadModelIfNeeded()

            .addOnSuccessListener {

                val lines = sourceText.lines()

                val output =
                    MutableList(lines.size) {
                        ""
                    }


                fun translateNext(
                    index: Int
                ) {

                    // -------------------------------------------------
                    // ALL LINES COMPLETE
                    // -------------------------------------------------

                    if (index >= lines.size) {

                        translatedText =
                            output.joinToString("\n")

                        isProcessing = false

                        translator.close()

                        return
                    }


                    val line = lines[index]
                    val trimmed = line.trim()


                    // -------------------------------------------------
                    // EMPTY LINE
                    // -------------------------------------------------

                    if (trimmed.isEmpty()) {

                        output[index] = line

                        translateNext(index + 1)

                        return
                    }


                    // -------------------------------------------------
                    // MEDICINE / DOSAGE / STRENGTH LINE
                    // -------------------------------------------------

                    if (shouldKeepPrescriptionLine(line)) {

                        output[index] = line

                        translateNext(index + 1)

                        return
                    }


                    // -------------------------------------------------
                    // PERSONAL INFORMATION
                    // -------------------------------------------------

                    if (
                        trimmed.contains(":") &&
                        isSensitiveValueLine(trimmed)
                    ) {

                        val separatorIndex =
                            trimmed.indexOf(":")


                        val label =
                            trimmed.substring(
                                0,
                                separatorIndex + 1
                            )


                        val value =
                            trimmed.substring(
                                separatorIndex + 1
                            ).trim()


                        // IMPORTANT:
                        // translate() is the ML Kit API method.
                        translator
                            .translate(label)

                            .addOnSuccessListener { translatedLabel ->

                                output[index] =
                                    if (value.isNotEmpty()) {

                                        "$translatedLabel $value"

                                    } else {

                                        translatedLabel
                                    }

                                translateNext(index + 1)
                            }

                            .addOnFailureListener {

                                output[index] = line

                                translateNext(index + 1)
                            }


                        return
                    }


                    // -------------------------------------------------
                    // NORMAL TEXT
                    // -------------------------------------------------

                    translator
                        .translate(line)

                        .addOnSuccessListener { translated ->

                            output[index] = translated

                            translateNext(index + 1)
                        }

                        .addOnFailureListener {

                            // Never remove OCR information
                            output[index] = line

                            translateNext(index + 1)
                        }
                }


                translateNext(0)
            }

            .addOnFailureListener {

                translatedText = sourceText

                isProcessing = false

                translator.close()

                ocrError =
                    when (language) {

                        "मराठी" ->
                            "भाषांतरासाठी language model download करता आला नाही. Internet connection तपासा."

                        else ->
                            "भाषा मॉडल डाउनलोड नहीं हो सका। Internet connection जाँचें."
                    }
            }
    }


    // =============================================================
    // OCR
    // =============================================================

    fun runPrescriptionOcr(uri: Uri) {
        val requestId = ocrRequestId
        isProcessing = true
        extractedText = ""
        translatedText = ""
        ocrError = ""

        fun finishWithText(text: String) {
            if (requestId != ocrRequestId) return
            extractedText = text.trim()
            if (extractedText.isBlank()) {
                ocrError = when (selectedLanguage) {
                    "मराठी" -> "वाचता येणारा मजकूर सापडला नाही. कृपया फोटो सरळ आणि स्पष्ट ठेवा."
                    "हिन्दी" -> "पढ़ने योग्य टेक्स्ट नहीं मिला। कृपया फोटो सीधा और साफ रखें."
                    else -> "No readable text was found. Please keep the prescription straight and clear."
                }
                isProcessing = false
                return
            }
            if (selectedLanguage == "English") {
                translatedText = extractedText
                isProcessing = false
            } else {
                translatePrescriptionText(extractedText, selectedLanguage)
            }
        }

        fun scorePrescriptionText(text: String): Int {
            val l = text.lowercase()
            var score = 0
            if (Regex("(?i)\\b(dr\\.?|doctor|consultant|physician)\\b").containsMatchIn(text)) score += 2
            if (Regex("(?i)\\b(mr\\.?|mrs\\.?|ms\\.?|patient)\\b").containsMatchIn(text)) score += 2
            if (Regex("(?i)\\b(tab|tablet|cap|capsule|syrup|syp|cream|ointment|gel|inj)\\b").containsMatchIn(text)) score += 3
            if (Regex("(?i)\\b\\d+(?:\\.\\d+)?\\s*(mg|mcg|g|ml|iu|units?|%)\\b").containsMatchIn(text)) score += 2
            if (l.contains("diagnosis") || l.contains("hypertension") || l.contains("diabetes")) score += 2
            if (Regex("(?i)\\b\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}\\b").containsMatchIn(text) ||
                Regex("(?i)\\b\\d{1,2}[-/][a-z]{3,9}[-/]\\d{2,4}\\b").containsMatchIn(text)) score += 1
            return score
        }

        fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
            if (degrees == 0f) return source
            val matrix = Matrix().apply { postRotate(degrees) }
            return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        }

        fun processBitmap(bitmap: Bitmap, degrees: Float, onDone: (String) -> Unit) {
            val rotated = rotateBitmap(bitmap, degrees)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(InputImage.fromBitmap(rotated, 0))
                .addOnSuccessListener { result ->
                    val text = result.textBlocks
                        .flatMap { block -> block.lines }
                        .sortedWith(
                            compareBy<com.google.mlkit.vision.text.Text.Line> { it.boundingBox?.top ?: Int.MAX_VALUE }
                                .thenBy { it.boundingBox?.left ?: Int.MAX_VALUE }
                        )
                        .map { it.text.trim() }
                        .filter { it.isNotBlank() }
                        .joinToString("\n")
                        .trim()
                    recognizer.close()
                    if (rotated !== bitmap) rotated.recycle()
                    onDone(text)
                }
                .addOnFailureListener {
                    recognizer.close()
                    if (rotated !== bitmap) rotated.recycle()
                    onDone("")
                }
        }

        // First use ML Kit's normal file-path handling (keeps camera metadata).
        try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val firstText = result.textBlocks
                        .flatMap { block -> block.lines }
                        .sortedWith(
                            compareBy<com.google.mlkit.vision.text.Text.Line> { it.boundingBox?.top ?: Int.MAX_VALUE }
                                .thenBy { it.boundingBox?.left ?: Int.MAX_VALUE }
                        )
                        .map { it.text.trim() }
                        .filter { it.isNotBlank() }
                        .joinToString("\n")
                        .trim()
                    recognizer.close()
                    if (scorePrescriptionText(firstText) >= 4 || firstText.length < 20) {
                        finishWithText(firstText)
                        return@addOnSuccessListener
                    }

                    // Some gallery/camera images are physically sideways. Retry 90/180/270 degrees.
                    try {
                        context.contentResolver.openInputStream(uri).use { input ->
                            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                            val bitmap = BitmapFactory.decodeStream(input, null, options)
                            if (bitmap == null) {
                                finishWithText(firstText)
                                return@use
                            }
                            val candidates = listOf(90f, 270f, 180f)
                            fun tryNext(index: Int, bestText: String, bestScore: Int) {
                                if (index >= candidates.size) {
                                    finishWithText(if (bestScore > scorePrescriptionText(firstText)) bestText else firstText)
                                    bitmap.recycle()
                                    return
                                }
                                processBitmap(bitmap, candidates[index]) { candidate ->
                                    val candidateScore = scorePrescriptionText(candidate)
                                    if (candidateScore >= 4) {
                                        finishWithText(candidate)
                                        bitmap.recycle()
                                    } else {
                                        val newBest = if (candidateScore > bestScore) candidate else bestText
                                        val newScore = maxOf(candidateScore, bestScore)
                                        tryNext(index + 1, newBest, newScore)
                                    }
                                }
                            }
                            tryNext(0, firstText, scorePrescriptionText(firstText))
                        }
                    } catch (_: Exception) {
                        finishWithText(firstText)
                    }
                }
                .addOnFailureListener {
                    recognizer.close()
                    finishWithText("")
                }
        } catch (_: Exception) {
            ocrError = when (selectedLanguage) {
                "मराठी" -> "प्रिस्क्रिप्शन इमेज process करता आली नाही."
                "हिन्दी" -> "प्रिस्क्रिप्शन इमेज को प्रोसेस नहीं किया जा सका."
                else -> "Unable to process this prescription image."
            }
            isProcessing = false
        }
    }

    // =============================================================
    // START A COMPLETELY NEW PRESCRIPTION
    // =============================================================

    fun resetForNewPrescription() {
        ocrRequestId += 1L
        extractedText = ""
        translatedText = ""
        ocrError = ""
        isProcessing = false
        isReviewMode = false
        isSaved = false
        savedPrescriptionId = ""
        doctorName = ""
        patientName = ""
        prescriptionDate = ""
        diagnosis = ""
        medicineRows = listOf(PrescriptionMedicineDraft())
        medicineVerification = emptyMap()
        isVerifyingMedicines = false
        selectedImageUri = null
        cameraUri = null
    }

    // =============================================================
    // GALLERY
    // =============================================================

    val galleryLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {

                resetForNewPrescription()
                selectedImageUri = uri
                cameraUri = null

                runPrescriptionOcr(uri)
            }
        }


    // =============================================================
    // CAMERA
    // =============================================================

    val cameraLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->

            if (success && cameraUri != null) {
                val capturedUri = cameraUri
                resetForNewPrescription()
                selectedImageUri = capturedUri
                capturedUri?.let { uri ->
                    runPrescriptionOcr(uri)
                }
            }
        }


    // =============================================================
    // CAMERA PERMISSION
    // =============================================================

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                val uri =
                    createImageUri(context)

                if (uri != null) {

                    cameraUri = uri

                    cameraLauncher.launch(uri)
                }
            }
        }


    // =============================================================
    // OPEN CAMERA
    // =============================================================

    fun openCamera() {

        val permissionGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED


        if (permissionGranted) {

            val uri =
                createImageUri(context)

            if (uri != null) {

                cameraUri = uri

                cameraLauncher.launch(uri)
            }

        } else {

            cameraPermissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }


    fun startReview() {
        val draft = extractPrescriptionDraft(extractedText)
        doctorName = draft.doctorName
        patientName = draft.patientName
        prescriptionDate = draft.date
        diagnosis = draft.diagnosis
        medicineRows = draft.medicines
        medicineVerification = emptyMap()
        isVerifyingMedicines = false
        isReviewMode = true
        isSaved = false
        savedPrescriptionId = ""
    }

    fun verifyCurrentMedicines() {
        val rowsToVerify = medicineRows.mapIndexedNotNull { index, medicine ->
            if (medicine.medicineName.isBlank()) null else index to medicine.medicineName
        }

        if (rowsToVerify.isEmpty()) {
            medicineVerification = emptyMap()
            return
        }

        isVerifyingMedicines = true
        medicineVerification = emptyMap()

        verificationScope.launch {
            val result = withContext(Dispatchers.IO) {
                rowsToVerify.associate { (index, name) ->
                    index to verifyMedicineInDataset(context, name)
                }
            }
            medicineVerification = result
            isVerifyingMedicines = false
        }
    }

    fun saveCurrentPrescription() {
        val cleanMedicines = medicineRows.filter {
            it.medicineName.isNotBlank() ||
                    it.strength.isNotBlank() ||
                    it.frequency.isNotBlank() ||
                    it.duration.isNotBlank() ||
                    it.instructions.isNotBlank()
        }

        val draft = PrescriptionDraft(
            doctorName = doctorName.trim(),
            patientName = patientName.trim(),
            date = prescriptionDate.trim(),
            diagnosis = diagnosis.trim(),
            medicines = cleanMedicines.ifEmpty {
                listOf(PrescriptionMedicineDraft())
            }
        )

        savedPrescriptionId = PrescriptionRepository.save(context, draft)
        doctorName = draft.doctorName
        patientName = draft.patientName
        prescriptionDate = draft.date
        diagnosis = draft.diagnosis
        medicineRows = draft.medicines
        isSaved = true
        isReviewMode = false
        isVerifyingMedicines = false
    }

    // =============================================================
    // MAIN SCREEN
    // =============================================================

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        backgroundTop,
                        backgroundBottom
                    )
                )
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 20.dp,
                    vertical = 18.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {


            // =====================================================
            // BACK BUTTON
            // =====================================================

            Button(
                onClick = onBackClick,
                modifier = Modifier.size(48.dp),
                contentPadding =
                    PaddingValues(0.dp),
                shape =
                    RoundedCornerShape(50.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = darkBlue
                    )
            ) {

                Text(
                    text = "‹",
                    fontSize = 32.sp
                )
            }


            // =====================================================
            // HEADER
            // =====================================================

            Text(
                text = scannerTitle,
                color = darkBlue,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )


            Text(
                text = scannerDescription,
                color = gray,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )


            Spacer(
                modifier = Modifier.height(2.dp)
            )


            // =====================================================
            // LANGUAGE SELECTOR
            // =====================================================

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {

                    Text(
                        text = languageLabel,
                        color = darkBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )


                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )


                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Button(
                            onClick = {
                                languageMenuExpanded = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape =
                                RoundedCornerShape(14.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor =
                                        Color(0xFFF0F6F8),
                                    contentColor =
                                        darkBlue
                                )
                        ) {

                            Row(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.SpaceBetween,
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Text(
                                    text =
                                        when (selectedLanguage) {

                                            "मराठी" ->
                                                "मराठी"

                                            "हिन्दी" ->
                                                "हिन्दी"

                                            else ->
                                                "English"
                                        },
                                    fontSize = 14.sp,
                                    fontWeight =
                                        FontWeight.Medium
                                )


                                Text(
                                    text = "⌄",
                                    fontSize = 20.sp
                                )
                            }
                        }


                        DropdownMenu(
                            expanded =
                                languageMenuExpanded,
                            onDismissRequest = {
                                languageMenuExpanded = false
                            }
                        ) {

                            // -------------------------------------------------
                            // ENGLISH
                            // -------------------------------------------------

                            DropdownMenuItem(
                                text = {
                                    Text("English")
                                },
                                onClick = {

                                    selectedLanguage =
                                        "English"

                                    languageMenuExpanded =
                                        false


                                    if (
                                        extractedText.isNotEmpty()
                                    ) {

                                        translatePrescriptionText(
                                            sourceText =
                                                extractedText,
                                            language =
                                                "English"
                                        )
                                    }
                                }
                            )


                            // -------------------------------------------------
                            // MARATHI
                            // -------------------------------------------------

                            DropdownMenuItem(
                                text = {
                                    Text("मराठी")
                                },
                                onClick = {

                                    selectedLanguage =
                                        "मराठी"

                                    languageMenuExpanded =
                                        false


                                    if (
                                        extractedText.isNotEmpty()
                                    ) {

                                        translatePrescriptionText(
                                            sourceText =
                                                extractedText,
                                            language =
                                                "मराठी"
                                        )
                                    }
                                }
                            )


                            // -------------------------------------------------
                            // HINDI
                            // -------------------------------------------------

                            DropdownMenuItem(
                                text = {
                                    Text("हिन्दी")
                                },
                                onClick = {

                                    selectedLanguage =
                                        "हिन्दी"

                                    languageMenuExpanded =
                                        false


                                    if (
                                        extractedText.isNotEmpty()
                                    ) {

                                        translatePrescriptionText(
                                            sourceText =
                                                extractedText,
                                            language =
                                                "हिन्दी"
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }


            // =====================================================
            // SCANNER CARD
            // =====================================================

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 5.dp
                    )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {


                    // =================================================
                    // NO IMAGE
                    // =================================================

                    if (selectedImageUri == null) {

                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .background(
                                    primary.copy(
                                        alpha = 0.12f
                                    ),
                                    RoundedCornerShape(22.dp)
                                ),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Text(
                                text = "📄",
                                fontSize = 38.sp
                            )
                        }


                        Spacer(
                            modifier =
                                Modifier.height(16.dp)
                        )


                        Text(
                            text = scanTitle,
                            color = darkBlue,
                            fontSize = 19.sp,
                            fontWeight =
                                FontWeight.Bold
                        )


                        Spacer(
                            modifier =
                                Modifier.height(7.dp)
                        )


                        Text(
                            text = scanDescription,
                            color = gray,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )


                        Spacer(
                            modifier =
                                Modifier.height(18.dp)
                        )


                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(10.dp)
                        ) {

                            Button(
                                onClick = {
                                    openCamera()
                                },
                                modifier =
                                    Modifier.weight(1f),
                                shape =
                                    RoundedCornerShape(16.dp),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            primary
                                    )
                            ) {

                                Text(
                                    text =
                                        "📷 Camera",
                                    fontSize = 13.sp,
                                    fontWeight =
                                        FontWeight.SemiBold
                                )
                            }


                            Button(
                                onClick = {
                                    galleryLauncher.launch(
                                        "image/*"
                                    )
                                },
                                modifier =
                                    Modifier.weight(1f),
                                shape =
                                    RoundedCornerShape(16.dp),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            darkBlue
                                    )
                            ) {

                                Text(
                                    text =
                                        "🖼️ Gallery",
                                    fontSize = 13.sp,
                                    fontWeight =
                                        FontWeight.SemiBold
                                )
                            }
                        }


                    } else {


                        // =================================================
                        // IMAGE SELECTED
                        // =================================================

                        Text(
                            text = selectedTitle,
                            color = darkBlue,
                            fontSize = 18.sp,
                            fontWeight =
                                FontWeight.Bold
                        )


                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )


                        PrescriptionImagePreview(
                            uri =
                                selectedImageUri,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .background(
                                        Color(0xFFF5F8FA),
                                        RoundedCornerShape(18.dp)
                                    )
                        )


                        Spacer(
                            modifier =
                                Modifier.height(15.dp)
                        )


                        // =================================================
                        // IMAGE BUTTONS
                        // =================================================

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(10.dp)
                        ) {

                            Button(
                                onClick = {

                                    selectedImageUri =
                                        null

                                    cameraUri =
                                        null

                                    extractedText =
                                        ""

                                    translatedText =
                                        ""

                                    ocrError =
                                        ""

                                    isProcessing =
                                        false

                                    isReviewMode = false
                                    isSaved = false
                                    savedPrescriptionId = ""
                                    doctorName = ""
                                    patientName = ""
                                    prescriptionDate = ""
                                    diagnosis = ""
                                    medicineRows = listOf(PrescriptionMedicineDraft())
                                    medicineVerification = emptyMap()
                                },
                                modifier =
                                    Modifier.weight(1f),
                                shape =
                                    RoundedCornerShape(16.dp),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            Color(0xFFE8EEF2),
                                        contentColor =
                                            darkBlue
                                    )
                            ) {

                                Text(
                                    text =
                                        chooseAgainText,
                                    fontSize = 12.sp
                                )
                            }


                            Button(
                                onClick = {
                                    openCamera()
                                },
                                modifier =
                                    Modifier.weight(1f),
                                shape =
                                    RoundedCornerShape(16.dp),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            primary
                                    )
                            ) {

                                Text(
                                    text =
                                        "📷 $retakeText",
                                    fontSize = 12.sp
                                )
                            }
                        }


                        Spacer(
                            modifier =
                                Modifier.height(18.dp)
                        )


                        // =================================================
                        // PROCESSING
                        // =================================================

                        if (isProcessing) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(32.dp),
                                color = primary
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )


                            Text(
                                text = readingText,
                                color = darkBlue,
                                fontSize = 13.sp,
                                fontWeight =
                                    FontWeight.Medium
                            )
                        }


                        // =================================================
                        // ERROR
                        // =================================================

                        if (ocrError.isNotEmpty()) {

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )


                            Text(
                                text =
                                    "⚠ $ocrError",
                                color =
                                    Color(0xFFB45309),
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }


                        // =================================================
                        // RESULT / REVIEW / SUMMARY

                        if (!isReviewMode && !isSaved) {

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF5F9FB)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = extractedTitle,
                                        color = darkBlue,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    (
                                            if (translatedText.isNotEmpty()) translatedText
                                            else extractedText
                                            )
                                        .lines()
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }
                                        .forEach { line ->
                                            Text(
                                                text = line,
                                                color = Color(0xFF263943),
                                                fontSize = 13.sp,
                                                lineHeight = 20.sp,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 7.dp)
                                            )
                                        }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = { startReview() },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = primary
                                        )
                                    ) {
                                        Text(
                                            text = reviewTitle,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        if (isReviewMode) {

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 4.dp
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = reviewTitle,
                                        color = darkBlue,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Review the OCR result before saving. Do not change the prescribed dosage without professional advice.",
                                        color = gray,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = doctorName,
                                        onValueChange = { doctorName = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text(doctorLabel) },
                                        singleLine = true,
                                        colors = reviewFieldColors
                                    )

                                    Spacer(modifier = Modifier.height(9.dp))

                                    OutlinedTextField(
                                        value = patientName,
                                        onValueChange = { patientName = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text(patientLabel) },
                                        singleLine = true,
                                        colors = reviewFieldColors
                                    )

                                    Spacer(modifier = Modifier.height(9.dp))

                                    OutlinedTextField(
                                        value = prescriptionDate,
                                        onValueChange = { prescriptionDate = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text(dateLabel) },
                                        singleLine = true,
                                        colors = reviewFieldColors
                                    )

                                    Spacer(modifier = Modifier.height(9.dp))

                                    OutlinedTextField(
                                        value = diagnosis,
                                        onValueChange = { diagnosis = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text(diagnosisLabel) },
                                        minLines = 2,
                                        maxLines = 3,
                                        colors = reviewFieldColors
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = medicinesLabel,
                                        color = darkBlue,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    medicineRows.forEachIndexed { index, medicine ->

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 10.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(0xFFF5F9FB)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp)
                                            ) {
                                                Text(
                                                    text = "Medicine ${index + 1}",
                                                    color = darkBlue,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                OutlinedTextField(
                                                    value = medicine.medicineName,
                                                    onValueChange = { value ->
                                                        medicineRows =
                                                            medicineRows.toMutableList().also {
                                                                it[index] =
                                                                    it[index].copy(
                                                                        medicineName = value
                                                                    )
                                                            }
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    label = { Text(medicineNameLabel) },
                                                    singleLine = true,
                                                    colors = reviewFieldColors
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                OutlinedTextField(
                                                    value = medicine.strength,
                                                    onValueChange = { value ->
                                                        medicineRows =
                                                            medicineRows.toMutableList().also {
                                                                it[index] =
                                                                    it[index].copy(
                                                                        strength = value
                                                                    )
                                                            }
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    label = { Text(strengthLabel) },
                                                    singleLine = true,
                                                    colors = reviewFieldColors
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                OutlinedTextField(
                                                    value = medicine.frequency,
                                                    onValueChange = { value ->
                                                        medicineRows =
                                                            medicineRows.toMutableList().also {
                                                                it[index] =
                                                                    it[index].copy(
                                                                        frequency = value
                                                                    )
                                                            }
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    label = { Text(frequencyLabel) },
                                                    singleLine = true,
                                                    colors = reviewFieldColors
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                OutlinedTextField(
                                                    value = medicine.duration,
                                                    onValueChange = { value ->
                                                        medicineRows =
                                                            medicineRows.toMutableList().also {
                                                                it[index] =
                                                                    it[index].copy(
                                                                        duration = value
                                                                    )
                                                            }
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    label = { Text(durationLabel) },
                                                    singleLine = true,
                                                    colors = reviewFieldColors
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                OutlinedTextField(
                                                    value = medicine.instructions,
                                                    onValueChange = { value ->
                                                        medicineRows =
                                                            medicineRows.toMutableList().also {
                                                                it[index] =
                                                                    it[index].copy(
                                                                        instructions = value
                                                                    )
                                                            }
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    label = { Text(instructionsLabel) },
                                                    minLines = 2,
                                                    maxLines = 4,
                                                    colors = reviewFieldColors
                                                )


                                                val verification =
                                                    medicineVerification[index]

                                                if (verification != null) {
                                                    Spacer(
                                                        modifier = Modifier.height(8.dp)
                                                    )

                                                    androidx.compose.material3.Text(
                                                        text = if (verification.recognized) {
                                                            "✓ Medicine recognized in MEDASSIST database"
                                                        } else {
                                                            "⚠ Medicine information is not available in the local database"
                                                        },
                                                        color = if (verification.recognized) {
                                                            Color(0xFF18794E)
                                                        } else {
                                                            Color(0xFFB45309)
                                                        },
                                                        fontSize = 10.sp,
                                                        lineHeight = 15.sp
                                                    )

                                                    if (verification.recognized &&
                                                        verification.matchedName.isNotBlank() &&
                                                        verification.matchedName.lowercase() !=
                                                        medicine.medicineName.trim().lowercase()
                                                    ) {
                                                        Text(
                                                            text = "Matched record: ${verification.matchedName}",
                                                            color = gray,
                                                            fontSize = 10.sp,
                                                            lineHeight = 15.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (!isVerifyingMedicines) {
                                                verifyCurrentMedicines()
                                            }
                                        },
                                        enabled = !isVerifyingMedicines,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFEAF6FA),
                                            contentColor = darkBlue,
                                            disabledContainerColor = Color(0xFFEAF6FA),
                                            disabledContentColor = gray
                                        )
                                    ) {
                                        if (isVerifyingMedicines) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = primary
                                            )
                                            Spacer(modifier = Modifier.size(8.dp))
                                        }
                                        Text(
                                            text = if (isVerifyingMedicines) {
                                                when (selectedLanguage) {
                                                    "मराठी" -> "औषधे तपासत आहे..."
                                                    "हिन्दी" -> "दवाइयाँ जाँची जा रही हैं..."
                                                    else -> "Checking medicines..."
                                                }
                                            } else {
                                                when (selectedLanguage) {
                                                    "मराठी" -> "✓ औषध डेटाबेसमध्ये तपासा"
                                                    "हिन्दी" -> "✓ दवा डेटाबेस में जाँचें"
                                                    else -> "✓ Verify Medicines in MEDASSIST Database"
                                                }
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            medicineRows =
                                                medicineRows + PrescriptionMedicineDraft()
                                            medicineVerification = emptyMap()
                                            isVerifyingMedicines = false
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFE8EEF2),
                                            contentColor = darkBlue
                                        )
                                    ) {
                                        Text(text = addMedicineText, fontSize = 12.sp)
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = { saveCurrentPrescription() },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = primary
                                        )
                                    ) {
                                        Text(
                                            text = savePrescriptionText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        if (isSaved) {

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 4.dp
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = savedText,
                                        color = Color(0xFF18794E),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = summaryTitle,
                                        color = darkBlue,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (doctorName.isNotBlank()) {
                                        Text(
                                            text = "$doctorLabel: $doctorName",
                                            color = Color(0xFF263943),
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                    }

                                    if (patientName.isNotBlank()) {
                                        Text(
                                            text = "$patientLabel: $patientName",
                                            color = Color(0xFF263943),
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                    }

                                    if (prescriptionDate.isNotBlank()) {
                                        Text(
                                            text = "$dateLabel: $prescriptionDate",
                                            color = Color(0xFF263943),
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                    }

                                    if (diagnosis.isNotBlank()) {
                                        Text(
                                            text = "$diagnosisLabel: $diagnosis",
                                            color = Color(0xFF263943),
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(bottom = 10.dp)
                                        )
                                    }

                                    Text(
                                        text = medicinesLabel,
                                        color = darkBlue,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    medicineRows
                                        .filter {
                                            it.medicineName.isNotBlank() ||
                                                    it.strength.isNotBlank() ||
                                                    it.frequency.isNotBlank() ||
                                                    it.duration.isNotBlank() ||
                                                    it.instructions.isNotBlank()
                                        }
                                        .forEachIndexed { index, medicine ->

                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 8.dp),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = Color(0xFFF5F9FB)
                                                )
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp)
                                                ) {
                                                    Text(
                                                        text = "${index + 1}. ${medicine.medicineName.ifBlank { "Medicine" }}",
                                                        color = darkBlue,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )

                                                    if (medicine.strength.isNotBlank()) {
                                                        Text(
                                                            text = "$strengthLabel: ${medicine.strength}",
                                                            color = Color(0xFF263943),
                                                            fontSize = 12.sp
                                                        )
                                                    }

                                                    if (medicine.frequency.isNotBlank()) {
                                                        Text(
                                                            text = "$frequencyLabel: ${medicine.frequency}",
                                                            color = Color(0xFF263943),
                                                            fontSize = 12.sp
                                                        )
                                                    }

                                                    if (medicine.duration.isNotBlank()) {
                                                        Text(
                                                            text = "$durationLabel: ${medicine.duration}",
                                                            color = Color(0xFF263943),
                                                            fontSize = 12.sp
                                                        )
                                                    }

                                                    if (medicine.instructions.isNotBlank()) {
                                                        Text(
                                                            text = "$instructionsLabel: ${medicine.instructions}",
                                                            color = Color(0xFF263943),
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = verifyText,
                                        color = gray,
                                        fontSize = 11.sp,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }

                        // SAFETY NOTE
                        // =================================================

                        Card(
                            modifier =
                                Modifier.fillMaxWidth(),
                            shape =
                                RoundedCornerShape(16.dp),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        primary.copy(
                                            alpha = 0.08f
                                        )
                                )
                        ) {

                            Text(
                                text =
                                    when (selectedLanguage) {

                                        "मराठी" ->
                                            "ℹ️ OCR ने मिळालेला मजकूर फक्त reference साठी आहे. औषधाचे नाव, strength आणि dosage स्वतः बदलू नका."

                                        "हिन्दी" ->
                                            "ℹ️ OCR से निकाला गया टेक्स्ट केवल reference के लिए है। दवा का नाम, strength और dosage स्वयं न बदलें."

                                        else ->
                                            "ℹ️ OCR extracted text is for reference only. Do not change medicine name, strength or dosage yourself."
                                    },
                                modifier =
                                    Modifier.padding(14.dp),
                                color =
                                    darkBlue,
                                fontSize = 11.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }


        // =====================================================
        // LANGUAGE INFO
        // =====================================================

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        primary.copy(alpha = 0.08f)
                )
        ) {

            Text(
                text =
                    "$languageLabel: English • मराठी • हिन्दी",
                modifier =
                    Modifier.padding(16.dp),
                color = darkBlue,
                fontSize = 13.sp,
                fontWeight =
                    FontWeight.Medium
            )
        }


        // =====================================================
        // DISCLAIMER
        // =====================================================

        Text(
            text = warningText,
            color = gray,
            fontSize = 11.sp,
            lineHeight = 17.sp
        )
    }
}


// =================================================================
// CAMERA IMAGE URI
// =================================================================

private fun createImageUri(
    context: Context
): Uri? {

    val values =
        ContentValues().apply {

            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "medassist_prescription_${System.currentTimeMillis()}.jpg"
            )

            put(
                MediaStore.Images.Media.MIME_TYPE,
                "image/jpeg"
            )
        }


    return context.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        values
    )
}


// =================================================================
// PRESCRIPTION IMAGE PREVIEW
// =================================================================

@Composable
private fun PrescriptionImagePreview(
    uri: Uri?,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current


    val bitmap =
        remember(uri) {

            uri?.let {

                context.contentResolver
                    .openInputStream(it)
                    ?.use { inputStream ->

                        BitmapFactory.decodeStream(
                            inputStream
                        )
                    }
            }
        }


    if (bitmap != null) {

        Image(
            bitmap =
                bitmap.asImageBitmap(),
            contentDescription =
                "Selected prescription",
            modifier =
                modifier,
            contentScale =
                ContentScale.Fit
        )

    } else {

        Image(
            painter =
                painterResource(
                    id = R.drawable.medassist_logo
                ),
            contentDescription =
                "Prescription",
            modifier =
                modifier,
            contentScale =
                ContentScale.Fit
        )
    }
}
