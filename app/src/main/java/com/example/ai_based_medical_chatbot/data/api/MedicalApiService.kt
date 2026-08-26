package com.example.ai_based_medical_chatbot.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface MedicalApiService {

    @POST("predict")
    suspend fun predict(
        @Body request: PredictionRequest
    ): Response<PredictionResponse>
}