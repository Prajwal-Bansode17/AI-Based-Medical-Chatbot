package com.example.ai_based_medical_chatbot.data.api

import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private class RetryInterceptor(
        private val maxRetries: Int = 1
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

                    if (attempt == maxRetries) {
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
    // LOCAL FLASK API
    // =========================================================
    // Flask backend is running on Prajwal's laptop.
    //
    // Current laptop IPv4:
    // 172.26.114.98
    //
    // Physical Android phone and laptop must be connected
    // to the same Wi-Fi/network.
    //
    // Flask API:
    // http://172.26.114.98:5000/
    // =========================================================

    private const val BASE_URL =
        "http://172.26.114.98:5000/"


    // =========================================================
    // HTTP CLIENT
    // =========================================================

    private val okHttpClient =
        OkHttpClient.Builder()

            .addInterceptor(
                RetryInterceptor()
            )

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