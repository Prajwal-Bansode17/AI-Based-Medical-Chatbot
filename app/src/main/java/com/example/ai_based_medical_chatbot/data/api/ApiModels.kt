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
    val answer: String? = null,

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

    @SerializedName("language")
    val language: String = "",

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

/*
 * ============================================================
 * CHAT HISTORY
 * ============================================================
 */

data class ChatHistoryResponse(
    @SerializedName("status")
    val status: String = "",

    @SerializedName("session_id")
    val sessionId: String = "",

    @SerializedName("history")
    val history: List<ChatHistoryMessage> = emptyList(),

    @SerializedName("message")
    val message: String? = null
)

data class ChatHistoryMessage(
    @SerializedName("role")
    val role: String = "",

    @SerializedName("message")
    val message: String = "",

    @SerializedName("symptoms")
    val symptoms: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("timestamp")
    val timestamp: String? = null
)