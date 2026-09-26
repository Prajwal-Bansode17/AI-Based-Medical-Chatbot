package ui

import android.content.Context
import org.json.JSONArray

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

    private var cachedMedicines: List<MedicineRecord>? = null

    private var cachedSearchMedicines:
            List<SearchableMedicine>? = null

    // ============================================================
    // COMMON MEDICINE ALIASES
    // ============================================================

    private val medicineAliases = mapOf(

        "paracetamol" to listOf(
            "paracetamol",
            "acetaminophen"
        ),

        "acetaminophen" to listOf(
            "acetaminophen",
            "paracetamol"
        ),

        "crocin" to listOf(
            "crocin",
            "paracetamol",
            "acetaminophen"
        ),

        "dolo" to listOf(
            "dolo",
            "paracetamol",
            "acetaminophen"
        ),

        "dolo 650" to listOf(
            "dolo 650",
            "dolo",
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

    private fun normalizeSearchText(
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
    // LOAD MEDICINES
    // ============================================================

    fun getMedicines(
        context: Context
    ): List<MedicineRecord> {

        cachedMedicines?.let {
            return it
        }

        val jsonText =
            context.assets
                .open("medicine_data.json")
                .bufferedReader()
                .use {
                    it.readText()
                }

        val jsonArray =
            JSONArray(jsonText)

        val medicines =
            ArrayList<MedicineRecord>(
                jsonArray.length()
            )

        for (i in 0 until jsonArray.length()) {

            val item =
                jsonArray.optJSONObject(i)
                    ?: continue

            medicines.add(
                MedicineRecord(

                    name =
                        item.optString("n"),

                    generic =
                        item.optString("g"),

                    brand =
                        item.optString("b"),

                    ingredients =
                        item.optString("i"),

                    drugClass =
                        item.optString("c"),

                    uses =
                        item.optString("u"),

                    sideEffects =
                        item.optString("s"),

                    warnings =
                        item.optString("w"),

                    precautions =
                        item.optString("p"),

                    interactions =
                        item.optString("x")
                )
            )
        }

        cachedMedicines =
            medicines

        return medicines
    }

    // ============================================================
    // BUILD SEARCH CACHE
    // ============================================================

    private fun getSearchableMedicines(
        context: Context
    ): List<SearchableMedicine> {

        cachedSearchMedicines?.let {
            return it
        }

        val medicines =
            getMedicines(context)

        val uniqueMedicines =
            LinkedHashMap<String, SearchableMedicine>()

        for (medicine in medicines) {

            val normalizedName =
                normalizeSearchText(
                    medicine.name
                )

            val normalizedGeneric =
                normalizeSearchText(
                    medicine.generic
                )

            val normalizedBrand =
                normalizeSearchText(
                    medicine.brand
                )

            val normalizedIngredients =
                normalizeSearchText(
                    medicine.ingredients
                )

            /*
             * Medicine-level deduplication.
             *
             * Same name + same generic = one medicine.
             */
            val uniqueKey =
                when {

                    normalizedName.isNotBlank() ->
                        normalizedName

                    normalizedGeneric.isNotBlank() ->
                        normalizedGeneric

                    normalizedBrand.isNotBlank() ->
                        normalizedBrand

                    else ->
                        normalizedIngredients
                }

            if (
                uniqueKey.isBlank()
            ) {
                continue
            }

            if (
                !uniqueMedicines.containsKey(
                    uniqueKey
                )
            ) {

                uniqueMedicines[
                    uniqueKey
                ] =
                    SearchableMedicine(
                        medicine = medicine,
                        name = normalizedName,
                        generic = normalizedGeneric,
                        brand = normalizedBrand,
                        ingredients =
                            normalizedIngredients
                    )
            }
        }

        val result =
            uniqueMedicines.values.toList()

        cachedSearchMedicines =
            result

        return result
    }

    // ============================================================
    // SEARCH TERMS
    // ============================================================

    private fun getSearchTerms(
        query: String
    ): List<String> {

        val normalizedQuery =
            normalizeSearchText(
                query
            )

        val aliases =
            medicineAliases[
                normalizedQuery
            ]
                ?: emptyList()

        return (
                listOf(
                    normalizedQuery
                ) + aliases
                )
            .map {
                normalizeSearchText(it)
            }
            .filter {
                it.isNotBlank()
            }
            .distinct()
    }

    // ============================================================
    // SCORE
    // ============================================================

    private fun calculateScore(
        medicine: SearchableMedicine,
        searchTerm: String
    ): Int {

        if (
            medicine.name ==
            searchTerm
        ) {
            return 1000
        }

        if (
            medicine.generic ==
            searchTerm
        ) {
            return 950
        }

        if (
            medicine.brand ==
            searchTerm
        ) {
            return 900
        }

        if (
            medicine.name.startsWith(
                searchTerm
            )
        ) {
            return 850
        }

        if (
            medicine.generic.startsWith(
                searchTerm
            )
        ) {
            return 840
        }

        if (
            medicine.brand.startsWith(
                searchTerm
            )
        ) {
            return 830
        }

        if (
            medicine.name.contains(
                searchTerm
            )
        ) {
            return 750
        }

        if (
            medicine.generic.contains(
                searchTerm
            )
        ) {
            return 740
        }

        if (
            medicine.brand.contains(
                searchTerm
            )
        ) {
            return 730
        }

        if (
            medicine.ingredients.contains(
                searchTerm
            )
        ) {
            return 650
        }

        return 0
    }

    // ============================================================
    // SEARCH MEDICINES
    // ============================================================

    fun searchMedicines(
        context: Context,
        query: String,
        limit: Int = 20
    ): List<MedicineRecord> {

        val normalizedQuery =
            normalizeSearchText(query)

        if (normalizedQuery.isBlank()) {

            return getSearchableMedicines(context)
                .take(limit)
                .map {
                    it.medicine
                }
        }

        val medicines =
            getSearchableMedicines(context)

        // =========================================================
        // SPECIAL ALIAS SEARCH
        // =========================================================
        //
        // Paracetamol, Crocin and Dolo are commonly used names
        // for medicines containing acetaminophen/paracetamol.
        //
        // For these aliases we intentionally return the best
        // canonical acetaminophen record instead of showing
        // many different acetaminophen products.
        // =========================================================

        val canonicalAliases =
            setOf(
                "paracetamol",
                "acetaminophen",
                "crocin",
                "dolo",
                "dolo 650"
            )

        if (normalizedQuery in canonicalAliases) {

            val acetaminophenResults =
                medicines
                    .filter { medicine ->

                        medicine.generic == "acetaminophen" ||
                                medicine.generic.contains(
                                    "acetaminophen"
                                ) ||
                                medicine.ingredients.contains(
                                    "acetaminophen"
                                )
                    }
                    .sortedWith(
                        compareBy<SearchableMedicine> {

                            when {

                                it.generic == "acetaminophen" ->
                                    0

                                it.generic.contains(
                                    "acetaminophen"
                                ) ->
                                    1

                                else ->
                                    2
                            }

                        }.thenBy {
                            it.name.length
                        }
                    )

            return acetaminophenResults
                .take(1)
                .map {
                    it.medicine
                }
        }

        // =========================================================
        // NORMAL SEARCH
        // =========================================================

        val searchTerms =
            getSearchTerms(
                normalizedQuery
            )

        val scoredResults =
            ArrayList<Pair<MedicineRecord, Int>>()

        for (medicine in medicines) {

            var bestScore = 0

            for (term in searchTerms) {

                val score =
                    calculateScore(
                        medicine = medicine,
                        searchTerm = term
                    )

                if (score > bestScore) {
                    bestScore = score
                }
            }

            if (bestScore > 0) {

                scoredResults.add(
                    Pair(
                        medicine.medicine,
                        bestScore
                    )
                )
            }
        }

        return scoredResults
            .sortedWith(
                compareByDescending<
                        Pair<MedicineRecord, Int>
                        > {
                    it.second
                }.thenBy {
                    it.first.name.lowercase()
                }
            )
            .take(limit)
            .map {
                it.first
            }
    }
    // ============================================================
    // CLEAR CACHE
    // ============================================================

    fun clearCache() {

        cachedMedicines =
            null

        cachedSearchMedicines =
            null
    }
}