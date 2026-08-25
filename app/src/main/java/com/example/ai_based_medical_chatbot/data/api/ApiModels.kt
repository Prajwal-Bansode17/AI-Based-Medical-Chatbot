package com.example.ai_based_medical_chatbot.data.api

import com.google.gson.annotations.SerializedName

data class PredictionRequest(

    @SerializedName("session_id")
    val sessionId: String,

    @SerializedName("text")
    val text: String
)


data class PredictionResponse(

    @SerializedName("status")
    val status: String = "",

    @SerializedName("question")
    val question: String = "",

    @SerializedName("intent")
    val intent: String = "",

    @SerializedName("confidence")
    val confidence: Double = 0.0,

    @SerializedName("answer")
    val answer: String = "",

    @SerializedName("answer_similarity")
    val answerSimilarity: Double = 0.0,

    @SerializedName("medical_query")
    val medicalQuery: Boolean = false,

    @SerializedName("duration")
    val duration: String? = null,

    @SerializedName("symptoms")
    val symptoms: List<String> = emptyList(),

    @SerializedName("previous_symptoms")
    val previousSymptoms: List<String> = emptyList(),

    @SerializedName("source")
    val source: String = "",

    @SerializedName("measurements")
    val measurements: List<Measurement> = emptyList()
)


data class Measurement(

    @SerializedName("memory_type")
    val memoryType: String = "",

    @SerializedName("name")
    val name: String = "",

    @SerializedName("value")
    val value: String = "",

    @SerializedName("unit")
    val unit: String = "",

    @SerializedName("duration")
    val duration: String? = null
)