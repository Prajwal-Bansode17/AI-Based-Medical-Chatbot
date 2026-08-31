package com.example.ai_based_medical_chatbot.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // =========================================================
    // RETRY INTERCEPTOR
    // =========================================================

    private class RetryInterceptor(
        private val maxRetries: Int = 2
    ) : Interceptor {

        override fun intercept(
            chain: Interceptor.Chain
        ): Response {

            var attempt = 0
            var lastException: Exception? = null

            while (attempt <= maxRetries) {

                try {

                    return chain.proceed(
                        chain.request()
                    )

                } catch (e: Exception) {

                    lastException = e

                    if (
                        e !is IOException ||
                        attempt == maxRetries
                    ) {
                        throw e
                    }

                    attempt++
                }
            }

            throw lastException
                ?: IllegalStateException(
                    "Request failed"
                )
        }
    }

    // =========================================================
    // FLASK BACKEND CONFIGURATION
    // =========================================================
    //
    // CHANGE ONLY THIS IP WHEN YOU RUN FLASK
    //
    // Hariom:
    // 192.168.x.x
    //
    // Omkar:
    // 192.168.x.x
    //
    // Prajwal:
    // 192.168.x.x
    //
    // Port remains 5000.
    // Phone and laptop must be on the same Wi-Fi.
    // =========================================================

    private const val FLASK_IP = "192.168.1.7"
    private const val FLASK_PORT = 5000
    private const val BASE_URL = "http://192.168.1.7:5000/"

    // =========================================================
    // HTTP CLIENT
    // =========================================================

    private val okHttpClient =
        OkHttpClient.Builder()

            .addInterceptor(
                RetryInterceptor()
            )

            .connectTimeout(
                20,
                TimeUnit.SECONDS
            )

            .readTimeout(
                90,
                TimeUnit.SECONDS
            )

            .writeTimeout(
                20,
                TimeUnit.SECONDS
            )

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