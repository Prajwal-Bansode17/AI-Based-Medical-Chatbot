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

                    // Retry only network/connection failures.
                    // HTTP 4xx/5xx responses are not retried.
                    if (e !is IOException || attempt == maxRetries) {
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
    // FLASK BACKEND
    // =========================================================
    //
    // Current Flask server:
    //
    // Laptop IPv4 : 192.168.1.7
    // Flask Port  : 5000
    //
    // Phone and laptop must be connected
    // to the same Wi-Fi/network.
    //
    // Flask:
    // http://192.168.1.7:5000/
    // =========================================================

    private const val BASE_URL =
        "http://192.168.1.8:5000/"


    // =========================================================
    // HTTP CLIENT
    // =========================================================

    private val okHttpClient =
        OkHttpClient.Builder()

            .addInterceptor(
                RetryInterceptor()
            )

            // Time allowed to establish connection
            .connectTimeout(
                20,
                TimeUnit.SECONDS
            )

            // Time allowed to receive Flask response
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