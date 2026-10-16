package com.example.ai_based_medical_chatbot.data.api

import com.example.ai_based_medical_chatbot.BuildConfig
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
    // Set your Flask server address in local.properties:
    //
    //   MEDASSIST_API_URL=http://<YOUR_IP>:5000/
    //
    // Replace <YOUR_IP> with the LAN IP of the machine
    // running Flask. Phone and laptop must be on the same Wi-Fi.
    // Do NOT commit local.properties — it is in .gitignore.
    // =========================================================

    private const val PREFS_NAME = "medassist_network_config"
    private const val KEY_SERVER_URL = "custom_server_url"

    @Volatile
    private var customBaseUrl: String? = null

    @Volatile
    private var appContext: android.content.Context? = null

    @Volatile
    private var cachedApiService: MedicalApiService? = null

    fun initialize(context: android.content.Context) {
        val app = context.applicationContext ?: context
        appContext = app
        val saved = app.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .getString(KEY_SERVER_URL, null)?.trim()
        if (!saved.isNullOrBlank()) {
            customBaseUrl = saved
        }
        cachedApiService = null
    }

    fun normalizeUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return "http://10.0.2.2:5000/"
        val withScheme = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "http://$trimmed"
        } else {
            trimmed
        }
        return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
    }

    fun getBaseUrl(context: android.content.Context? = null): String {
        customBaseUrl?.let { if (it.isNotBlank()) return normalizeUrl(it) }

        val ctx = context ?: appContext
        if (ctx != null) {
            val saved = ctx.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .getString(KEY_SERVER_URL, null)?.trim()
            if (!saved.isNullOrBlank()) {
                customBaseUrl = saved
                return normalizeUrl(saved)
            }
        }

        return normalizeUrl(BuildConfig.MEDASSIST_API_URL)
    }

    fun setCustomServerUrl(context: android.content.Context, url: String?) {
        val app = context.applicationContext ?: context
        appContext = app
        val trimmed = url?.trim()
        app.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SERVER_URL, trimmed)
            .apply()
        customBaseUrl = trimmed
        cachedApiService = null
    }

    fun clearCustomServerUrl(context: android.content.Context) {
        val app = context.applicationContext ?: context
        appContext = app
        app.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SERVER_URL)
            .apply()
        customBaseUrl = null
        cachedApiService = null
    }

    // =========================================================
    // HTTP CLIENT
    // =========================================================

    val okHttpClient: OkHttpClient =
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

    val apiService: MedicalApiService
        get() {
            val current = cachedApiService
            if (current != null) return current
            return synchronized(this) {
                cachedApiService?.let { return it }
                val newService = Retrofit.Builder()
                    .baseUrl(getBaseUrl())
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(MedicalApiService::class.java)
                cachedApiService = newService
                newService
            }
        }
}