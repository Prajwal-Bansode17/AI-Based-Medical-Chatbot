package com.example.ai_based_medical_chatbot.data.api

import com.google.gson.annotations.SerializedName

data class PredictionRequest(
    @SerializedName("session_id")
    val sessionId: String,

    @SerializedName("text")
    val text: String,

    @SerializedName("image_base64")
    val imageBase64: String? = null,

    @SerializedName("ocr_text")
    val ocrText: String? = null,

    @SerializedName("user_id")
    val userId: String? = null,

    @SerializedName("conversation_id")
    val conversationId: String? = null,

    @SerializedName("is_temporary")
    val isTemporary: Boolean = false,

    @SerializedName("correlation_id")
    val correlationId: String? = null,

    @SerializedName("user_name")
    val userName: String? = null,

    @SerializedName("user_age")
    val userAge: Int? = null,

    @SerializedName("user_gender")
    val userGender: String? = null
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
    val measurements: List<Measurement> = emptyList(),

    @SerializedName("session_id")
    val sessionId: String? = null,

    @SerializedName("correlation_id")
    val correlationId: String? = null,

    @SerializedName("user_name")
    val userName: String? = null,

    @SerializedName("user_age")
    val userAge: Int? = null,

    @SerializedName("user_gender")
    val userGender: String? = null
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

/*
 * ============================================================
 * USER LONG-TERM MEMORY & CONVERSATIONS (C1-C8, H1)
 * ============================================================
 */

data class UserMemoryItem(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("user_id")
    val userId: String = "",

    @SerializedName("memory_key")
    val memoryKey: String = "",

    @SerializedName("content")
    val content: String = "",

    @SerializedName("category")
    val category: String = "PREFERENCE",

    @SerializedName("source")
    val source: String = "user_chat",

    @SerializedName("status")
    val status: String = "user-reported",

    @SerializedName("created_at")
    val createdAt: String = "",

    @SerializedName("updated_at")
    val updatedAt: String = ""
)

data class UserMemoriesResponse(
    @SerializedName("status")
    val status: String = "",

    @SerializedName("user_id")
    val userId: String? = null,

    @SerializedName("count")
    val count: Int = 0,

    @SerializedName("memories")
    val memories: List<UserMemoryItem> = emptyList(),

    @SerializedName("message")
    val message: String? = null
)

data class AddMemoryRequest(
    @SerializedName("key")
    val key: String? = null,

    @SerializedName("content")
    val content: String,

    @SerializedName("category")
    val category: String = "PREFERENCE"
)

data class UpdateMemoryRequest(
    @SerializedName("content")
    val content: String
)

data class MemoryConsentResponse(
    @SerializedName("status")
    val status: String = "",

    @SerializedName("user_id")
    val userId: String? = null,

    @SerializedName("consent")
    val consent: Boolean = false
)

data class MemoryConsentRequest(
    @SerializedName("consent")
    val consent: Boolean
)

data class ConversationSummary(
    @SerializedName("session_id")
    val sessionId: String = "",

    @SerializedName("conversation_id")
    val conversationId: String = "",

    @SerializedName("last_message")
    val lastMessage: String = "",

    @SerializedName("created_at")
    val createdAt: String = ""
)

data class ConversationsResponse(
    @SerializedName("status")
    val status: String = "",

    @SerializedName("user_id")
    val userId: String? = null,

    @SerializedName("count")
    val count: Int = 0,

    @SerializedName("conversations")
    val conversations: List<ConversationSummary> = emptyList(),

    @SerializedName("message")
    val message: String? = null
)

/*
 * ============================================================
 * SMART MEDICAL LAB REPORT ANALYZER
 * ============================================================
 */

data class LabReportRequest(
    @SerializedName("text")
    val text: String? = null,

    @SerializedName("biomarkers")
    val biomarkers: Map<String, Any>? = null,

    @SerializedName("language")
    val language: String = "en"
)

data class BiomarkerDto(
    @SerializedName("key")
    val key: String = "",

    @SerializedName("name")
    val name: String = "",

    @SerializedName("display_name")
    val displayName: String = "",

    @SerializedName("value")
    val value: Double = 0.0,

    @SerializedName("unit")
    val unit: String = "",

    @SerializedName("reference_range")
    val referenceRange: String = "",

    @SerializedName("status")
    val status: String = "normal",

    @SerializedName("gauge_percent")
    val gaugePercent: Float = 0.5f,

    @SerializedName("severity_color")
    val severityColor: String = "#4CAF50",

    @SerializedName("what_it_is")
    val whatItIs: String = "",

    @SerializedName("layman_explanation")
    val laymanExplanation: String = "",

    @SerializedName("dietary_advice")
    val dietaryAdvice: String = ""
)

data class LabReportResponse(
    @SerializedName("status")
    val status: String = "",

    @SerializedName("language")
    val language: String = "en",

    @SerializedName("analyzed_count")
    val analyzedCount: Int = 0,

    @SerializedName("flagged_count")
    val flaggedCount: Int = 0,

    @SerializedName("biomarkers")
    val biomarkers: List<BiomarkerDto> = emptyList(),

    @SerializedName("doctor_questions")
    val doctorQuestions: List<String> = emptyList(),

    @SerializedName("summary")
    val summary: String = "",

    @SerializedName("disclaimer")
    val disclaimer: String = ""
)

