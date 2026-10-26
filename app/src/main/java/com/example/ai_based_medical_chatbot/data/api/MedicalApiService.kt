package com.example.ai_based_medical_chatbot.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
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

    // =========================================================
    // LONG-TERM MEMORY & CONVERSATIONS ENDPOINTS (C1-C8, H1)
    // =========================================================

    @GET("api/v1/memory")
    suspend fun getUserMemories(): Response<UserMemoriesResponse>

    @POST("api/v1/memory")
    suspend fun addUserMemory(
        @Body request: AddMemoryRequest
    ): Response<Unit>

    @PUT("api/v1/memory/{id}")
    suspend fun updateUserMemory(
        @Path("id") id: String,
        @Body request: UpdateMemoryRequest
    ): Response<Unit>

    @DELETE("api/v1/memory/{id}")
    suspend fun deleteUserMemory(
        @Path("id") id: String
    ): Response<Unit>

    @DELETE("api/v1/memory")
    suspend fun clearAllUserMemories(): Response<Unit>

    @GET("api/v1/memory/consent")
    suspend fun getMemoryConsent(): Response<MemoryConsentResponse>

    @POST("api/v1/memory/consent")
    suspend fun setMemoryConsent(
        @Body request: MemoryConsentRequest
    ): Response<MemoryConsentResponse>

    @GET("api/v1/conversations")
    suspend fun getConversations(): Response<ConversationsResponse>
}
