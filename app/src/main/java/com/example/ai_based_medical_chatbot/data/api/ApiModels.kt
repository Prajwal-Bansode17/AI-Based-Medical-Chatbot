package com.example.ai_based_medical_chatbot.data.api

data class PredictionRequest(
    val text: String
)

data class PredictionResponse(
    val status: String,
    val question: String,
    val intent: String,
    val confidence: Double,
    val answer: String,
    val answer_similarity: Double
)