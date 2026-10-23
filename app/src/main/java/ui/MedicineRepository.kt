package ui

import android.content.Context
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

data class MedicineRecord(
    val name: String,
    val generic: String,
    val brand: String,
    val ingredients: String,
    val drugClass: String,
    val uses: String,
    val sideEffects: String,
    val warnings: String,
    val precautions: String,
    val interactions: String
)

private data class SearchableMedicine(
    val medicine: MedicineRecord,
    val name: String,
    val generic: String,
    val brand: String,
    val ingredients: String
)

object MedicineRepository {

    @Volatile
    private var cachedMedicines: List<MedicineRecord>? = null

    @Volatile
    private var cachedInitialMedicines: List<MedicineRecord>? = null

    private val searchCache = LinkedHashMap<String, List<MedicineRecord>>(32)

    // ============================================================
    // VERIFIED POPULAR & COMMON MEDICINES (HUMAN-READABLE & ACCURATE)
    // ============================================================

    val verifiedMedicines: List<MedicineRecord> = listOf(
        MedicineRecord(
            name = "Dolo 650",
            generic = "Paracetamol",
            brand = "Dolo 650",
            ingredients = "Paracetamol 650 mg",
            drugClass = "Analgesic & Antipyretic",
            uses = "Dolo 650 is used to relieve mild to moderate pain such as headache, body pain, toothache, and to reduce fever.",
            sideEffects = "Usually well tolerated. Some people may experience nausea, stomach discomfort, or skin rash.",
            warnings = "Do not exceed the recommended dose of 4000 mg per day. Avoid alcohol.",
            precautions = "Do not take more than the recommended dose.\nAvoid using with other medicines containing paracetamol.\nConsult a doctor if you have liver problems.\nAsk a doctor before use in pregnancy or if you have any other medical condition.",
            interactions = "May interact with warfarin and other blood thinners."
        ),
        MedicineRecord(
            name = "Crocin 500",
            generic = "Paracetamol",
            brand = "Crocin 500",
            ingredients = "Paracetamol 500 mg",
            drugClass = "Analgesic & Antipyretic",
            uses = "Used to relieve mild to moderate pain and reduce fever caused by common cold, flu, headache, and body pain.",
            sideEffects = "May cause nausea, stomach discomfort, or allergic reactions in some people.",
            warnings = "Overdose can cause serious liver damage. Do not take with other paracetamol products.",
            precautions = "Do not exceed the recommended dose.\nAvoid alcohol while taking this medicine.\nConsult a doctor if you have liver disease or any other serious medical condition.\nAsk a doctor before use during pregnancy or breastfeeding.",
            interactions = "Alcohol increases risk of liver toxicity."
        ),
        MedicineRecord(
            name = "Calpol 650",
            generic = "Paracetamol",
            brand = "Calpol 650",
            ingredients = "Paracetamol 650 mg",
            drugClass = "Analgesic & Antipyretic",
            uses = "Provides fast relief from fever, body aches, muscle pains, and post-immunization discomfort.",
            sideEffects = "Generally safe when taken as directed. Rare side effects include skin itching or indigestion.",
            warnings = "Do not combine with other cold and flu medicines containing acetaminophen/paracetamol.",
            precautions = "Take with or after food if stomach upset occurs.\nDo not exceed 4 tablets in 24 hours.\nKeep out of reach of children.\nConsult your doctor if fever persists for more than 3 days.",
            interactions = "Consult physician if taking anticonvulsants or cholestyramine."
        ),
        MedicineRecord(
            name = "Paracetamol 500 mg",
            generic = "Paracetamol",
            brand = "Paracetamol 500 mg Tablet",
            ingredients = "Paracetamol 500 mg",
            drugClass = "Analgesic & Antipyretic",
            uses = "Effective relief from headache, toothache, arthritis, back pain, and cold-related fever.",
            sideEffects = "Mild nausea or allergic skin reactions may occur rarely.",
            warnings = "Strictly adhere to the prescribed dosage intervals.",
            precautions = "Maintain at least 4 to 6 hours between doses.\nDo not take with other paracetamol preparations.\nInform your doctor of any pre-existing kidney or liver impairment.",
            interactions = "Inform physician about all ongoing medications."
        ),
        MedicineRecord(
            name = "Paracetamol 125 mg",
            generic = "Paracetamol",
            brand = "Paracetamol 125 mg Suspension",
            ingredients = "Paracetamol 125 mg / 5 mL",
            drugClass = "Pediatric Antipyretic & Analgesic",
            uses = "Used for reduction of fever and relief of mild to moderate pain in infants and young children.",
            sideEffects = "Usually well tolerated by children. Occasional mild loose stools or drowsiness.",
            warnings = "Use only the calibrated measuring syringe or spoon provided.",
            precautions = "Verify pediatric dosage based on child's body weight.\nShake well before use.\nConsult pediatrician if symptoms persist beyond 48 hours.",
            interactions = "Do not administer with other pediatric cold syrups containing paracetamol."
        ),
        MedicineRecord(
            name = "PCM 650",
            generic = "Paracetamol",
            brand = "PCM 650",
            ingredients = "Paracetamol 650 mg",
            drugClass = "Analgesic & Antipyretic",
            uses = "Fast acting formulation for reduction of elevated body temperature and musculoskeletal aches.",
            sideEffects = "Very low incidence of side effects at therapeutic doses.",
            warnings = "Excess dose is harmful to the liver.",
            precautions = "Do not exceed stated dose.\nAvoid concurrent consumption of alcohol.\nStore in a dry location away from heat.",
            interactions = "Check with doctor before co-administering with prescription pain relievers."
        ),
        MedicineRecord(
            name = "P-650",
            generic = "Paracetamol",
            brand = "P-650",
            ingredients = "Paracetamol 650 mg",
            drugClass = "Analgesic & Antipyretic",
            uses = "Relief of pain and fever associated with seasonal infections, headaches, and joint stiffness.",
            sideEffects = "Rare gastrointestinal upset or mild allergic rash.",
            warnings = "Take with a full glass of water.",
            precautions = "Do not take more frequently than every 4-6 hours.\nConsult physician if pregnant or planning pregnancy.\nAvoid with other acetaminophen medications.",
            interactions = "Avoid alcohol and sedatives."
        ),
        MedicineRecord(
            name = "Ibuprofen 400 mg",
            generic = "Ibuprofen",
            brand = "Ibuprofen 400 mg Tablet",
            ingredients = "Ibuprofen 400 mg",
            drugClass = "Nonsteroidal Anti-inflammatory Drug (NSAID)",
            uses = "Relief of inflammatory pain, arthritis stiffness, dental pain, menstrual cramps, and fever.",
            sideEffects = "Heartburn, nausea, abdominal discomfort, or dizziness.",
            warnings = "May cause stomach bleeding if taken excessively or on an empty stomach.",
            precautions = "Take with food or milk to minimize gastrointestinal discomfort.\nDo not use if you have active stomach ulcers or asthma sensitive to NSAIDs.\nConsult a doctor if taking blood pressure medications.",
            interactions = "Interacts with aspirin, anticoagulants, and ACE inhibitors."
        ),
        MedicineRecord(
            name = "Amoxicillin 500 mg",
            generic = "Amoxicillin",
            brand = "Amoxicillin 500 mg Capsule",
            ingredients = "Amoxicillin 500 mg",
            drugClass = "Broad-spectrum Penicillin Antibiotic",
            uses = "Treatment of bacterial infections of the respiratory tract, ear, nose, throat, skin, and urinary tract.",
            sideEffects = "Diarrhea, mild nausea, vomiting, or skin rash.",
            warnings = "Do not use if allergic to penicillin or beta-lactam antibiotics.",
            precautions = "Complete the entire prescribed course even if symptoms resolve earlier.\nStay well-hydrated throughout therapy.\nSeek immediate medical care if allergic hives or breathing difficulty occurs.",
            interactions = "May reduce effectiveness of oral contraceptives. May interact with methotrexate."
        ),
        MedicineRecord(
            name = "Cetirizine 10 mg",
            generic = "Cetirizine",
            brand = "Cetirizine 10 mg Tablet",
            ingredients = "Cetirizine Hydrochloride 10 mg",
            drugClass = "Second-generation Antihistamine",
            uses = "Relief of allergic rhinitis symptoms including sneezing, runny nose, itchy watery eyes, and hives.",
            sideEffects = "Mild drowsiness, dry mouth, headache, or fatigue in some individuals.",
            warnings = "Use caution when operating machinery or driving until you know how it affects you.",
            precautions = "Best taken in the evening if drowsiness occurs.\nAvoid combining with alcohol or central nervous system sedatives.\nConsult a healthcare professional before use in kidney disease.",
            interactions = "Alcohol and CNS depressants may enhance drowsiness."
        ),
        MedicineRecord(
            name = "Omeprazole 20 mg",
            generic = "Omeprazole",
            brand = "Omeprazole 20 mg Capsule",
            ingredients = "Omeprazole 20 mg",
            drugClass = "Proton Pump Inhibitor (PPI)",
            uses = "Relief of frequent heartburn, acid indigestion, gastroesophageal reflux disease (GERD), and stomach ulcers.",
            sideEffects = "Headache, stomach pain, gas, nausea, or diarrhea.",
            warnings = "Intended for short-term symptom management unless under continuous physician supervision.",
            precautions = "Take in the morning on an empty stomach, at least 30-60 minutes before breakfast.\nSwallow capsule whole; do not crush or chew.\nConsult a doctor if chest pain radiates to arms or shoulders.",
            interactions = "May reduce absorption of iron, calcium, and vitamin B12 with prolonged use."
        ),
        MedicineRecord(
            name = "Metformin 500 mg",
            generic = "Metformin",
            brand = "Metformin 500 mg Tablet",
            ingredients = "Metformin Hydrochloride 500 mg",
            drugClass = "Biguanide Antidiabetic",
            uses = "Management of blood glucose levels in adults with type 2 diabetes along with diet and exercise.",
            sideEffects = "Temporary stomach upset, metallic taste in mouth, mild nausea, or diarrhea.",
            warnings = "Rare risk of lactic acidosis. Avoid excessive alcohol consumption while on therapy.",
            precautions = "Take with meals to reduce gastrointestinal side effects.\nHave kidney function tested regularly.\nStay properly hydrated, especially during illness or vigorous physical activity.",
            interactions = "Inform medical team before contrast dye procedures or surgery."
        )
    )

    // ============================================================
    // COMMON MEDICINE ALIASES
    // ============================================================

    private val medicineAliases = mapOf(
        "paracetamol" to listOf(
            "paracetamol",
            "acetaminophen",
            "dolo",
            "crocin",
            "calpol"
        ),
        "acetaminophen" to listOf(
            "acetaminophen",
            "paracetamol",
            "dolo",
            "crocin",
            "calpol"
        ),
        "crocin" to listOf(
            "crocin",
            "paracetamol",
            "acetaminophen"
        ),
        "dolo" to listOf(
            "dolo",
            "dolo 650",
            "paracetamol",
            "acetaminophen"
        ),
        "dolo 650" to listOf(
            "dolo 650",
            "dolo",
            "paracetamol",
            "acetaminophen"
        ),
        "calpol" to listOf(
            "calpol",
            "calpol 650",
            "paracetamol",
            "acetaminophen"
        ),
        "ors" to listOf(
            "ors",
            "oral rehydration",
            "oral rehydration salts",
            "oral rehydration solution"
        ),
        "oral rehydration" to listOf(
            "oral rehydration",
            "oral rehydration salts",
            "oral rehydration solution",
            "ors"
        ),
        "pain killer" to listOf(
            "pain killer",
            "painkiller",
            "analgesic",
            "pain relief"
        ),
        "painkiller" to listOf(
            "painkiller",
            "pain killer",
            "analgesic",
            "pain relief"
        )
    )

    // ============================================================
    // NORMALIZATION
    // ============================================================

    internal fun normalizeSearchText(
        value: String
    ): String {
        return value
            .trim()
            .lowercase()
            .replace(
                Regex("[^a-z0-9\\s]"),
                " "
            )
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
    }

    // ============================================================
    // STREAMING JSON PARSER
    // ============================================================

    fun parseMedicineStream(
        inputStream: InputStream,
        limit: Int = 0
    ): List<MedicineRecord> {
        val medicines = ArrayList<MedicineRecord>(if (limit in 1..100) limit else 128)
        val reader = JsonReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        reader.use { jsonReader ->
            if (jsonReader.peek() != JsonToken.BEGIN_ARRAY) {
                return emptyList()
            }
            jsonReader.beginArray()

            while (jsonReader.hasNext()) {
                if (jsonReader.peek() != JsonToken.BEGIN_OBJECT) {
                    jsonReader.skipValue()
                    continue
                }
                jsonReader.beginObject()

                var name = ""
                var generic = ""
                var brand = ""
                var ingredients = ""
                var drugClass = ""
                var uses = ""
                var sideEffects = ""
                var warnings = ""
                var precautions = ""
                var interactions = ""

                while (jsonReader.hasNext()) {
                    val key = jsonReader.nextName()
                    if (jsonReader.peek() == JsonToken.NULL) {
                        jsonReader.nextNull()
                        continue
                    }
                    when (key) {
                        "n" -> name = jsonReader.nextStringOrEmpty()
                        "g" -> generic = jsonReader.nextStringOrEmpty()
                        "b" -> brand = jsonReader.nextStringOrEmpty()
                        "i" -> ingredients = readIngredients(jsonReader)
                        "c" -> drugClass = jsonReader.nextStringOrEmpty()
                        "u" -> uses = jsonReader.nextStringOrEmpty()
                        "s" -> sideEffects = jsonReader.nextStringOrEmpty()
                        "w" -> warnings = jsonReader.nextStringOrEmpty()
                        "p" -> precautions = jsonReader.nextStringOrEmpty()
                        "x" -> interactions = jsonReader.nextStringOrEmpty()
                        else -> jsonReader.skipValue()
                    }
                }
                jsonReader.endObject()

                medicines.add(
                    MedicineRecord(
                        name = name,
                        generic = generic,
                        brand = brand,
                        ingredients = ingredients,
                        drugClass = drugClass,
                        uses = uses,
                        sideEffects = sideEffects,
                        warnings = warnings,
                        precautions = precautions,
                        interactions = interactions
                    )
                )

                if (limit in 1..medicines.size) {
                    break
                }
            }
        }
        return medicines
    }

    private fun JsonReader.nextStringOrEmpty(): String {
        return if (peek() == JsonToken.STRING) {
            nextString()
        } else {
            skipValue()
            ""
        }
    }

    private fun readIngredients(jsonReader: JsonReader): String {
        return when (jsonReader.peek()) {
            JsonToken.BEGIN_ARRAY -> {
                jsonReader.beginArray()
                val list = mutableListOf<String>()
                while (jsonReader.hasNext()) {
                    if (jsonReader.peek() == JsonToken.STRING) {
                        list.add(jsonReader.nextString())
                    } else {
                        jsonReader.skipValue()
                    }
                }
                jsonReader.endArray()
                list.joinToString(", ")
            }
            JsonToken.STRING -> jsonReader.nextString()
            else -> {
                jsonReader.skipValue()
                ""
            }
        }
    }

fun parseMedicineStreamFiltered(
        inputStream: InputStream,
        limit: Int,
        excludedKeys: Set<String> = emptySet()
    ): List<MedicineRecord> {
        val medicines = ArrayList<MedicineRecord>(if (limit in 1..250) limit else 64)
        val seenLocal = HashSet<String>(excludedKeys)
        val reader = JsonReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        reader.use { jsonReader ->
            if (jsonReader.peek() != JsonToken.BEGIN_ARRAY) {
                return emptyList()
            }
            jsonReader.beginArray()

            while (jsonReader.hasNext()) {
                if (jsonReader.peek() != JsonToken.BEGIN_OBJECT) {
                    jsonReader.skipValue()
                    continue
                }
                jsonReader.beginObject()

                var name = ""
                var generic = ""
                var brand = ""
                var ingredients = ""
                var drugClass = ""
                var uses = ""
                var sideEffects = ""
                var warnings = ""
                var precautions = ""
                var interactions = ""

                while (jsonReader.hasNext()) {
                    val key = jsonReader.nextName()
                    if (jsonReader.peek() == JsonToken.NULL) {
                        jsonReader.nextNull()
                        continue
                    }
                    when (key) {
                        "n" -> name = jsonReader.nextStringOrEmpty()
                        "g" -> generic = jsonReader.nextStringOrEmpty()
                        "b" -> brand = jsonReader.nextStringOrEmpty()
                        "i" -> ingredients = readIngredients(jsonReader)
                        "c" -> drugClass = jsonReader.nextStringOrEmpty()
                        "u" -> uses = jsonReader.nextStringOrEmpty()
                        "s" -> sideEffects = jsonReader.nextStringOrEmpty()
                        "w" -> warnings = jsonReader.nextStringOrEmpty()
                        "p" -> precautions = jsonReader.nextStringOrEmpty()
                        "x" -> interactions = jsonReader.nextStringOrEmpty()
                        else -> jsonReader.skipValue()
                    }
                }
                jsonReader.endObject()

                val cleanName = name.trim()
                if (cleanName.isBlank()) continue
                val normKey = normalizeSearchText(cleanName)
                if (normKey.isNotBlank() && seenLocal.add(normKey)) {
                    medicines.add(
                        MedicineRecord(
                            name = cleanName,
                            generic = generic.trim(),
                            brand = brand.trim(),
                            ingredients = ingredients.trim(),
                            drugClass = drugClass.trim(),
                            uses = uses.trim(),
                            sideEffects = sideEffects.trim(),
                            warnings = warnings.trim(),
                            precautions = precautions.trim(),
                            interactions = interactions.trim()
                        )
                    )
                    if (limit in 1..medicines.size) {
                        break
                    }
                }
            }
        }
        return medicines
    }

    // ============================================================
    // LOAD MEDICINES
    // ============================================================

    fun getMedicines(
        inputStream: InputStream,
        limit: Int = 0
    ): List<MedicineRecord> {
        if (limit > 0) {
            cachedMedicines?.let {
                return it.take(limit)
            }
            cachedInitialMedicines?.let {
                if (it.size >= limit) {
                    return it.take(limit)
                }
            }
            val loaded = parseMedicineStream(inputStream, limit = limit)
            if (cachedInitialMedicines == null && limit >= 20) {
                cachedInitialMedicines = loaded
            }
            return loaded
        }

        cachedMedicines?.let {
            return it
        }

        val all = parseMedicineStream(inputStream, limit = 0)
        cachedMedicines = all
        return all
    }

    fun getMedicines(
        context: Context,
        limit: Int = 0
    ): List<MedicineRecord> {
        if (limit > 0) {
            cachedMedicines?.let {
                return it.take(limit)
            }
            if (limit <= verifiedMedicines.size) {
                return verifiedMedicines.take(limit)
            }
            cachedInitialMedicines?.let {
                if (it.size >= limit) {
                    return it.take(limit)
                }
            }
            val needed = limit - verifiedMedicines.size
            val seenKeys = verifiedMedicines.map { normalizeSearchText(it.name) }.toHashSet()
            val extra = context.assets.open("medicine_data.json").use { stream ->
                parseMedicineStreamFiltered(stream, needed, seenKeys)
            }
            val combined = (verifiedMedicines + extra).take(limit)
            if (cachedInitialMedicines == null && combined.size >= 20) {
                cachedInitialMedicines = combined
            }
            return combined
        }

        cachedMedicines?.let {
            return it
        }

        val all = context.assets.open("medicine_data.json").use { stream ->
            parseMedicineStream(stream, limit = 0)
        }
        cachedMedicines = all
        return all
    }

    // ============================================================
    // SEARCH TERMS
    // ============================================================

    internal fun getSearchTerms(
        query: String
    ): List<String> {
        val normalizedQuery = normalizeSearchText(query)
        val aliases = medicineAliases[normalizedQuery] ?: emptyList()

        return (listOf(normalizedQuery) + aliases)
            .map { normalizeSearchText(it) }
            .filter { it.isNotBlank() }
            .distinct()
    }

    // ============================================================
    // SCORE
    // ============================================================

    internal fun calculateScore(
        name: String,
        generic: String,
        brand: String,
        ingredients: String,
        searchTerm: String
    ): Int {
        if (name == searchTerm) return 1000
        if (generic == searchTerm) return 950
        if (brand == searchTerm) return 900
        if (name.startsWith(searchTerm)) return 850
        if (generic.startsWith(searchTerm)) return 840
        if (brand.startsWith(searchTerm)) return 830
        if (name.contains(searchTerm)) return 750
        if (generic.contains(searchTerm)) return 740
        if (brand.contains(searchTerm)) return 730
        if (ingredients.contains(searchTerm)) return 650
        return 0
    }

    // ============================================================
    // SEARCH MEDICINES (STREAMING)
    // ============================================================

    fun searchMedicinesStream(
        inputStream: InputStream,
        query: String,
        limit: Int = 20
    ): List<MedicineRecord> {
        val normalizedQuery = normalizeSearchText(query)
        if (normalizedQuery.isBlank()) {
            return getMedicines(inputStream, limit = limit)
        }

        val canonicalAliases = setOf(
            "paracetamol",
            "acetaminophen",
            "crocin",
            "dolo",
            "dolo 650"
        )
        val isCanonicalAlias = normalizedQuery in canonicalAliases

        val searchTerms = if (isCanonicalAlias) {
            listOf("acetaminophen", "paracetamol")
        } else {
            getSearchTerms(normalizedQuery)
        }

        val seenKeys = HashSet<String>()
        val scoredResults = ArrayList<Pair<MedicineRecord, Int>>()
        val acetaminophenCandidates = ArrayList<MedicineRecord>()

        val reader = JsonReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        reader.use { jsonReader ->
            if (jsonReader.peek() != JsonToken.BEGIN_ARRAY) {
                return emptyList()
            }
            jsonReader.beginArray()

            while (jsonReader.hasNext()) {
                if (jsonReader.peek() != JsonToken.BEGIN_OBJECT) {
                    jsonReader.skipValue()
                    continue
                }
                jsonReader.beginObject()

                var name = ""
                var generic = ""
                var brand = ""
                var ingredients = ""
                var drugClass = ""
                var uses = ""
                var sideEffects = ""
                var warnings = ""
                var precautions = ""
                var interactions = ""

                while (jsonReader.hasNext()) {
                    val key = jsonReader.nextName()
                    if (jsonReader.peek() == JsonToken.NULL) {
                        jsonReader.nextNull()
                        continue
                    }
                    when (key) {
                        "n" -> name = jsonReader.nextStringOrEmpty()
                        "g" -> generic = jsonReader.nextStringOrEmpty()
                        "b" -> brand = jsonReader.nextStringOrEmpty()
                        "i" -> ingredients = readIngredients(jsonReader)
                        "c" -> drugClass = jsonReader.nextStringOrEmpty()
                        "u" -> uses = jsonReader.nextStringOrEmpty()
                        "s" -> sideEffects = jsonReader.nextStringOrEmpty()
                        "w" -> warnings = jsonReader.nextStringOrEmpty()
                        "p" -> precautions = jsonReader.nextStringOrEmpty()
                        "x" -> interactions = jsonReader.nextStringOrEmpty()
                        else -> jsonReader.skipValue()
                    }
                }
                jsonReader.endObject()

                val normName = normalizeSearchText(name)
                val normGeneric = normalizeSearchText(generic)
                val normBrand = normalizeSearchText(brand)
                val normIng = normalizeSearchText(ingredients)

                val uniqueKey = when {
                    normName.isNotBlank() -> normName
                    normGeneric.isNotBlank() -> normGeneric
                    normBrand.isNotBlank() -> normBrand
                    else -> normIng
                }
                if (uniqueKey.isBlank() || !seenKeys.add(uniqueKey)) {
                    continue
                }

                val record = MedicineRecord(
                    name = name,
                    generic = generic,
                    brand = brand,
                    ingredients = ingredients,
                    drugClass = drugClass,
                    uses = uses,
                    sideEffects = sideEffects,
                    warnings = warnings,
                    precautions = precautions,
                    interactions = interactions
                )

                if (isCanonicalAlias) {
                    if (normGeneric == "acetaminophen" ||
                        normGeneric.contains("acetaminophen") ||
                        normIng.contains("acetaminophen")
                    ) {
                        acetaminophenCandidates.add(record)
                    }
                } else {
                    var bestScore = 0
                    for (term in searchTerms) {
                        val score = calculateScore(
                            name = normName,
                            generic = normGeneric,
                            brand = normBrand,
                            ingredients = normIng,
                            searchTerm = term
                        )
                        if (score > bestScore) {
                            bestScore = score
                        }
                    }
                    if (bestScore > 0) {
                        scoredResults.add(Pair(record, bestScore))
                    }
                }
            }
        }

        if (isCanonicalAlias) {
            return acetaminophenCandidates
                .sortedWith(
                    compareBy<MedicineRecord> {
                        val g = normalizeSearchText(it.generic)
                        when {
                            g == "acetaminophen" -> 0
                            g.contains("acetaminophen") -> 1
                            else -> 2
                        }
                    }.thenBy {
                        it.name.length
                    }
                )
                .take(limit)
        }

        return scoredResults
            .sortedWith(
                compareByDescending<Pair<MedicineRecord, Int>> { it.second }
                    .thenBy { it.first.name.lowercase() }
            )
            .take(limit)
            .map { it.first }
    }

    // ============================================================
    // SEARCH FROM IN-MEMORY LIST (IF ALREADY LOADED)
    // ============================================================

    private fun searchMedicinesFromList(
        medicines: List<MedicineRecord>,
        normalizedQuery: String,
        limit: Int = 20
    ): List<MedicineRecord> {
        val canonicalAliases = setOf(
            "paracetamol",
            "acetaminophen",
            "crocin",
            "dolo",
            "dolo 650"
        )
        val isCanonicalAlias = normalizedQuery in canonicalAliases

        val seenKeys = HashSet<String>()
        val uniqueMedicines = ArrayList<MedicineRecord>()
        for (m in medicines) {
            val normName = normalizeSearchText(m.name)
            val normGeneric = normalizeSearchText(m.generic)
            val normBrand = normalizeSearchText(m.brand)
            val normIng = normalizeSearchText(m.ingredients)
            val uniqueKey = when {
                normName.isNotBlank() -> normName
                normGeneric.isNotBlank() -> normGeneric
                normBrand.isNotBlank() -> normBrand
                else -> normIng
            }
            if (uniqueKey.isNotBlank() && seenKeys.add(uniqueKey)) {
                uniqueMedicines.add(m)
            }
        }

        if (isCanonicalAlias) {
            return uniqueMedicines
                .filter { m ->
                    val g = normalizeSearchText(m.generic)
                    val ing = normalizeSearchText(m.ingredients)
                    g == "acetaminophen" || g.contains("acetaminophen") || ing.contains("acetaminophen")
                }
                .sortedWith(
                    compareBy<MedicineRecord> {
                        val g = normalizeSearchText(it.generic)
                        when {
                            g == "acetaminophen" -> 0
                            g.contains("acetaminophen") -> 1
                            else -> 2
                        }
                    }.thenBy {
                        it.name.length
                    }
                )
                .take(limit)
        }

        val searchTerms = getSearchTerms(normalizedQuery)
        val scoredResults = ArrayList<Pair<MedicineRecord, Int>>()

        for (m in uniqueMedicines) {
            val normName = normalizeSearchText(m.name)
            val normGeneric = normalizeSearchText(m.generic)
            val normBrand = normalizeSearchText(m.brand)
            val normIng = normalizeSearchText(m.ingredients)

            var bestScore = 0
            for (term in searchTerms) {
                val score = calculateScore(
                    name = normName,
                    generic = normGeneric,
                    brand = normBrand,
                    ingredients = normIng,
                    searchTerm = term
                )
                if (score > bestScore) {
                    bestScore = score
                }
            }
            if (bestScore > 0) {
                scoredResults.add(Pair(m, bestScore))
            }
        }

        return scoredResults
            .sortedWith(
                compareByDescending<Pair<MedicineRecord, Int>> { it.second }
                    .thenBy { it.first.name.lowercase() }
            )
            .take(limit)
            .map { it.first }
    }

    // ============================================================
    // SEARCH MEDICINES (PUBLIC DISPATCHER)
    // ============================================================

    fun searchMedicines(
        inputStream: InputStream,
        query: String,
        limit: Int = 20
    ): List<MedicineRecord> {
        return searchMedicinesStream(inputStream, query, limit)
    }

    fun searchMedicines(
        context: Context,
        query: String,
        limit: Int = 20
    ): List<MedicineRecord> {
        val normalizedQuery = normalizeSearchText(query)
        if (normalizedQuery.isBlank()) {
            return getMedicines(context, limit = limit)
        }

        synchronized(searchCache) {
            searchCache[normalizedQuery]?.let {
                return it.take(limit)
            }
        }

        // Search verified medicines first for clean, human-readable results (Dolo, Crocin, Calpol, Paracetamol, etc.)
        val verifiedMatches = searchVerifiedMedicines(normalizedQuery, limit)
        if (verifiedMatches.isNotEmpty()) {
            synchronized(searchCache) {
                if (searchCache.size > 50) searchCache.clear()
                searchCache[normalizedQuery] = verifiedMatches
            }
            return verifiedMatches
        }

        val inMemory = cachedMedicines
        val results = if (inMemory != null) {
            searchMedicinesFromList(inMemory, normalizedQuery, limit)
        } else {
            context.assets.open("medicine_data.json").use { stream ->
                searchMedicinesStream(stream, normalizedQuery, limit)
            }
        }

        synchronized(searchCache) {
            if (searchCache.size > 50) {
                searchCache.clear()
            }
            searchCache[normalizedQuery] = results
        }

        return results
    }

    // ============================================================
    // SEARCH VERIFIED MEDICINES
    // ============================================================

    private fun searchVerifiedMedicines(
        normalizedQuery: String,
        limit: Int = 20
    ): List<MedicineRecord> {
        val terms = getSearchTerms(normalizedQuery)
        val scored = ArrayList<Pair<MedicineRecord, Int>>()

        for (m in verifiedMedicines) {
            val n = normalizeSearchText(m.name)
            val g = normalizeSearchText(m.generic)
            val b = normalizeSearchText(m.brand)
            val i = normalizeSearchText(m.ingredients)

            var best = 0
            for (term in terms) {
                val s = calculateScore(n, g, b, i, term)
                if (s > best) best = s
            }
            if (best > 0) {
                scored.add(Pair(m, best))
            }
        }

        return scored
            .sortedWith(
                compareByDescending<Pair<MedicineRecord, Int>> { it.second }
                    .thenBy { it.first.name.lowercase() }
            )
            .take(limit)
            .map { it.first }
    }

    // ============================================================
    // RELATED MEDICINES (SAME ACTIVE INGREDIENT)
    // ============================================================

    fun getRelatedMedicines(
        query: String,
        primaryResults: List<MedicineRecord>
    ): List<MedicineRecord> {
        val norm = normalizeSearchText(query)
        val primaryNames = primaryResults.map { it.name.lowercase() }.toSet()

        // For paracetamol/acetaminophen queries, PCM 650 and P-650 are related formulations
        if (norm == "paracetamol" || norm == "acetaminophen" || norm == "dolo" || norm == "crocin" || norm == "calpol") {
            return verifiedMedicines.filter {
                (it.name == "PCM 650" || it.name == "P-650") && !primaryNames.contains(it.name.lowercase())
            }
        }

        val activeIngredients = primaryResults
            .map { it.generic.lowercase() }
            .filter { it.isNotBlank() }
            .distinct()

        if (activeIngredients.isNotEmpty()) {
            return verifiedMedicines.filter { vm ->
                !primaryNames.contains(vm.name.lowercase()) &&
                activeIngredients.any { ing ->
                    vm.generic.lowercase().contains(ing) || vm.ingredients.lowercase().contains(ing)
                }
            }.take(3)
        }

        return emptyList()
    }

    // ============================================================
    // CLEAR CACHE
    // ============================================================

    fun clearCache() {
        cachedMedicines = null
        cachedInitialMedicines = null
        synchronized(searchCache) {
            searchCache.clear()
        }
    }
}
