package com.example.ai_based_medical_chatbot.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface MedicalApiService {

    @POST("predict")
    suspend fun predict(
        @Body request: PredictionRequest
    ): Response<PredictionResponse>

    @GET("chat-history")
    suspend fun getChatHistory(
        @Query("session_id") sessionId: String
    ): Response<ChatHistoryResponse>

    @DELETE("chat-history")
    suspend fun deleteChatHistory(
        @Query("session_id") sessionId: String
    ): Response<Unit>
}