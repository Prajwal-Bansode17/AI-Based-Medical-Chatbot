package ui

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

data class SymptomCondition(
    val name: String,
    val symptoms: List<String>
)

data class SymptomPrediction(
    val condition: String,
    val matchedSymptoms: List<String>,
    val score: Int
)

private data class SymptomsMlModel(
    val classes: List<String>,
    val vocabulary: List<String>,
    val idf: FloatArray,
    val coefficients: Array<FloatArray>,
    val intercepts: FloatArray
)

object SymptomsRepository {

    private var symptomsCache: List<String>? = null
    private var conditionsCache: List<SymptomCondition>? = null
    private var modelCache: SymptomsMlModel? = null

    // ---------------------------------------------------------
    // DATASET
    // ---------------------------------------------------------

    private fun loadDataset(context: Context): JSONObject {
        val jsonText =
            context.assets
                .open("symptoms_dataset.json")
                .bufferedReader()
                .use { it.readText() }

        return JSONObject(jsonText)
    }

    fun getSymptoms(context: Context): List<String> {
        symptomsCache?.let { return it }

        val root = loadDataset(context)
        val array = root.optJSONArray("symptoms") ?: JSONArray()

        val symptoms = ArrayList<String>(array.length())

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val name = item.optString("name").trim()

            if (name.isNotBlank()) {
                symptoms.add(name)
            }
        }

        val result = symptoms
            .distinct()
            .sorted()

        symptomsCache = result

        return result
    }

    fun getConditions(context: Context): List<SymptomCondition> {
        conditionsCache?.let { return it }

        val root = loadDataset(context)
        val array = root.optJSONArray("conditions") ?: JSONArray()

        val conditions =
            ArrayList<SymptomCondition>(array.length())

        for (i in 0 until array.length()) {

            val item = array.optJSONObject(i) ?: continue

            val name =
                item.optString("name")
                    .trim()

            val symptomArray =
                item.optJSONArray("symptoms")
                    ?: JSONArray()

            val conditionSymptoms =
                ArrayList<String>(symptomArray.length())

            for (j in 0 until symptomArray.length()) {

                val symptom =
                    symptomArray
                        .optString(j)
                        .trim()
                        .lowercase()

                if (symptom.isNotBlank()) {
                    conditionSymptoms.add(symptom)
                }
            }

            if (
                name.isNotBlank() &&
                conditionSymptoms.isNotEmpty()
            ) {
                conditions.add(
                    SymptomCondition(
                        name = name,
                        symptoms = conditionSymptoms.distinct()
                    )
                )
            }
        }

        conditionsCache = conditions

        return conditions
    }

    // ---------------------------------------------------------
    // LOAD LOCAL ML MODEL
    // ---------------------------------------------------------

    private fun loadModel(context: Context): SymptomsMlModel {

        modelCache?.let { return it }

        val jsonText =
            context.assets
                .open("symptoms_model.json")
                .bufferedReader()
                .use { it.readText() }

        val root = JSONObject(jsonText)

        // Classes
        val classesArray =
            root.optJSONArray("classes")
                ?: JSONArray()

        val classes =
            ArrayList<String>(classesArray.length())

        for (i in 0 until classesArray.length()) {
            classes.add(
                classesArray.optString(i)
            )
        }

        // Vocabulary
        val vocabularyArray =
            root.optJSONArray("vocabulary")
                ?: JSONArray()

        val vocabulary =
            ArrayList<String>(vocabularyArray.length())

        for (i in 0 until vocabularyArray.length()) {
            vocabulary.add(
                vocabularyArray.optString(i)
            )
        }

        // IDF
        val idfArray =
            root.optJSONArray("idf")
                ?: JSONArray()

        val idf =
            FloatArray(idfArray.length())

        for (i in idf.indices) {
            idf[i] =
                idfArray
                    .optDouble(i, 1.0)
                    .toFloat()
        }

        // Coefficients
        val coefficientsArray =
            root.optJSONArray("coefficients")
                ?: JSONArray()

        val coefficients =
            Array(coefficientsArray.length()) {
                FloatArray(vocabulary.size)
            }

        for (i in coefficients.indices) {

            val row =
                coefficientsArray
                    .optJSONArray(i)
                    ?: JSONArray()

            for (j in vocabulary.indices) {

                coefficients[i][j] =
                    row
                        .optDouble(j, 0.0)
                        .toFloat()
            }
        }

        // Intercepts
        val interceptArray =
            root.optJSONArray("intercepts")
                ?: JSONArray()

        val intercepts =
            FloatArray(interceptArray.length())

        for (i in intercepts.indices) {

            intercepts[i] =
                interceptArray
                    .optDouble(i, 0.0)
                    .toFloat()
        }

        val model =
            SymptomsMlModel(
                classes = classes,
                vocabulary = vocabulary,
                idf = idf,
                coefficients = coefficients,
                intercepts = intercepts
            )

        modelCache = model

        return model
    }

    // ---------------------------------------------------------
    // TEXT TOKENIZATION
    // ---------------------------------------------------------

    private fun tokenize(text: String): List<String> {

        return text
            .lowercase()
            .split(
                Regex("[^\\p{L}\\p{N}_-]+")
            )
            .filter { it.isNotBlank() }
    }

    // ---------------------------------------------------------
    // CREATE WORD + BIGRAM FEATURES
    // ---------------------------------------------------------

    private fun createNgrams(
        tokens: List<String>
    ): List<String> {

        if (tokens.isEmpty()) {
            return emptyList()
        }

        val result =
            ArrayList<String>()

        // Unigrams
        for (token in tokens) {
            result.add(token)
        }

        // Bigrams
        for (i in 0 until tokens.size - 1) {

            result.add(
                "${tokens[i]} ${tokens[i + 1]}"
            )
        }

        return result
    }

    // ---------------------------------------------------------
    // TF-IDF VECTOR
    // ---------------------------------------------------------

    private fun createTfIdfVector(
        model: SymptomsMlModel,
        text: String
    ): FloatArray {

        val vector =
            FloatArray(model.vocabulary.size)

        val tokens =
            tokenize(text)

        val ngrams =
            createNgrams(tokens)

        if (ngrams.isEmpty()) {
            return vector
        }

        // Count terms
        val counts =
            HashMap<String, Int>()

        for (ngram in ngrams) {
            counts[ngram] =
                (counts[ngram] ?: 0) + 1
        }

        // Vocabulary lookup
        val vocabularyIndex =
            HashMap<String, Int>(
                model.vocabulary.size
            )

        for (i in model.vocabulary.indices) {
            vocabularyIndex[
                model.vocabulary[i]
            ] = i
        }

        // Sublinear TF + IDF
        for ((term, count) in counts) {

            val index =
                vocabularyIndex[term]
                    ?: continue

            // sklearn sublinear_tf=True:
            // tf = 1 + log(tf)
            val tf =
                if (count > 0) {
                    1.0 + ln(count.toDouble())
                } else {
                    0.0
                }

            vector[index] =
                (
                        tf *
                                model.idf[index]
                        ).toFloat()
        }

        // L2 normalization
        var sumSquares = 0.0

        for (value in vector) {
            sumSquares +=
                value.toDouble() *
                        value.toDouble()
        }

        val norm =
            sqrt(sumSquares)

        if (norm > 0.0) {

            for (i in vector.indices) {
                vector[i] =
                    (
                            vector[i].toDouble() /
                                    norm
                            ).toFloat()
            }
        }

        return vector
    }

    // ---------------------------------------------------------
    // SOFTMAX
    // ---------------------------------------------------------

    private fun softmax(
        values: FloatArray
    ): FloatArray {

        if (values.isEmpty()) {
            return FloatArray(0)
        }

        var maxValue =
            values[0]

        for (i in 1 until values.size) {
            if (values[i] > maxValue) {
                maxValue = values[i]
            }
        }

        val exponentials =
            FloatArray(values.size)

        var sum = 0.0

        for (i in values.indices) {

            val value =
                exp(
                    (
                            values[i] -
                                    maxValue
                            ).toDouble()
                )

            exponentials[i] =
                value.toFloat()

            sum += value
        }

        if (sum == 0.0) {
            return FloatArray(values.size)
        }

        for (i in exponentials.indices) {
            exponentials[i] =
                (
                        exponentials[i].toDouble() /
                                sum
                        ).toFloat()
        }

        return exponentials
    }

    // ---------------------------------------------------------
    // LOCAL ML PREDICTION
    // ---------------------------------------------------------

    private fun predictWithModel(
        context: Context,
        selectedSymptoms: List<String>
    ): List<Pair<String, Float>> {

        val model =
            loadModel(context)

        val text =
            selectedSymptoms
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(" ")

        if (text.isBlank()) {
            return emptyList()
        }

        val vector =
            createTfIdfVector(
                model = model,
                text = text
            )

        val logits =
            FloatArray(model.classes.size)

        for (classIndex in model.classes.indices) {

            var score =
                if (
                    classIndex <
                    model.intercepts.size
                ) {
                    model.intercepts[classIndex]
                } else {
                    0f
                }

            if (
                classIndex <
                model.coefficients.size
            ) {

                val coefficients =
                    model.coefficients[classIndex]

                val featureCount =
                    minOf(
                        vector.size,
                        coefficients.size
                    )

                for (featureIndex in 0 until featureCount) {

                    score +=
                        vector[featureIndex] *
                                coefficients[featureIndex]
                }
            }

            logits[classIndex] =
                score
        }

        val probabilities =
            softmax(logits)

        val results =
            ArrayList<Pair<String, Float>>(
                model.classes.size
            )

        for (i in model.classes.indices) {

            results.add(
                model.classes[i] to
                        probabilities[i]
            )
        }

        return results.sortedByDescending {
            it.second
        }
    }

    // ---------------------------------------------------------
    // PUBLIC PREDICTION FUNCTION
    // ---------------------------------------------------------

    fun predictConditions(
        context: Context,
        selectedSymptoms: List<String>,
        limit: Int = 3
    ): List<SymptomPrediction> {

        val selected =
            selectedSymptoms
                .map {
                    it.trim().lowercase()
                }
                .filter {
                    it.isNotBlank()
                }
                .distinct()

        /*
         * One common symptom is not enough information.
         *
         * Example:
         * fatigue
         *
         * Instead of guessing a disease, the UI will
         * receive an empty result and can show:
         *
         * "Not enough information"
         */
        if (selected.size < 2) {
            return emptyList()
        }

        val mlResults =
            predictWithModel(
                context = context,
                selectedSymptoms = selected
            )

        if (mlResults.isEmpty()) {
            return emptyList()
        }

        val conditions =
            getConditions(context)

        val predictions =
            ArrayList<SymptomPrediction>()

        for ((conditionName, probability) in mlResults) {

            if (predictions.size >= limit) {
                break
            }

            val condition =
                conditions.firstOrNull {
                    it.name.equals(
                        conditionName,
                        ignoreCase = true
                    )
                }

            val matchedSymptoms =
                if (condition != null) {

                    condition.symptoms.filter {
                        selected.contains(it)
                    }

                } else {
                    emptyList()
                }

            /*
             * Don't show completely unrelated results.
             */
            if (matchedSymptoms.isEmpty()) {
                continue
            }

            val percentage =
                (
                        probability * 100f
                        ).toInt()
                    .coerceIn(0, 100)

            predictions.add(
                SymptomPrediction(
                    condition = conditionName,
                    matchedSymptoms =
                        matchedSymptoms,
                    score = percentage
                )
            )
        }

        return predictions
    }

    // ---------------------------------------------------------
    // CLEAR CACHE
    // ---------------------------------------------------------

    fun clearCache() {

        symptomsCache = null
        conditionsCache = null
        modelCache = null
    }
}