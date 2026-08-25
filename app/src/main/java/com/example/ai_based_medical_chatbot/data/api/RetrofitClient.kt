package com.example.ai_based_medical_chatbot.data.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // =========================================================
    // RENDER CLOUD API
    // =========================================================
    // Flask API is deployed on Render.
    // Laptop वर api.py चालू ठेवण्याची गरज नाही.
    // =========================================================

    private const val BASE_URL =
        "https://ai-based-medical-chatbot.onrender.com/"


    // =========================================================
    // HTTP CLIENT
    // =========================================================

    private val okHttpClient =
        OkHttpClient.Builder()

            // Time to establish connection
            .connectTimeout(
                20,
                TimeUnit.SECONDS
            )

            // Time to wait for Flask response
            .readTimeout(
                90,
                TimeUnit.SECONDS
            )

            // Time allowed to send request
            .writeTimeout(
                20,
                TimeUnit.SECONDS
            )

            // Maximum total request time
            .callTimeout(
                120,
                TimeUnit.SECONDS
            )

            .build()


    // =========================================================
    // RETROFIT API SERVICE
    // =========================================================

    val apiService: MedicalApiService by lazy {

        Retrofit.Builder()

            .baseUrl(
                BASE_URL
            )

            .client(
                okHttpClient
            )

            .addConverterFactory(
                GsonConverterFactory.create()
            )

            .build()

            .create(
                MedicalApiService::class.java
            )
    }
}