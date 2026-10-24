package com.example.ai_based_medical_chatbot.data.api

import android.content.Context
import android.util.Log
import com.example.ai_based_medical_chatbot.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val TAG = "RetrofitClient"

    /**
     * Default URL for the Android Studio emulator.
     * 10.0.2.2 maps to the host machine's localhost.
     */
    private const val DEFAULT_EMULATOR_BASE_URL =
        "http://10.0.2.2:5000/"

    // =========================================================
    // RETRY INTERCEPTOR
    // =========================================================

    private class RetryInterceptor(
        private val maxRetries: Int = 2
    ) : Interceptor {

        override fun intercept(
            chain: Interceptor.Chain
        ): Response {
            val request = chain.request()

            // Retry only safe, idempotent requests.
            // Never automatically retry POST requests such as /predict.
            val isRetrySafe =
                request.method.equals("GET", ignoreCase = true) ||
                        request.method.equals("HEAD", ignoreCase = true)

            var attempt = 0
            var lastException: IOException? = null

            while (true) {
                try {
                    return chain.proceed(request)
                } catch (e: IOException) {
                    lastException = e

                    if (!isRetrySafe || attempt >= maxRetries) {
                        throw e
                    }

                    attempt++

                    Log.w(
                        TAG,
                        "Network request failed; retry $attempt/$maxRetries",
                        e
                    )
                }
            }
        }
    }

    // =========================================================
    // FLASK BACKEND CONFIGURATION
    // =========================================================

    const val PREFS_NAME = "medassist_network_config"
    const val KEY_SERVER_URL = "custom_server_url"

    // Previously used/stale IP marker.
    const val STALE_IP_MARKER = "192" + ".168.1.12"

    @Volatile
    private var customBaseUrl: String? = null

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var cachedApiService: MedicalApiService? = null

    /**
     * Call this from the Application or Activity startup.
     * Loads any saved custom server URL.
     */
    fun initialize(context: Context) {
        val app = context.applicationContext
        appContext = app

        val prefs = app.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val saved = prefs.getString(KEY_SERVER_URL, null)?.trim()

        synchronized(this) {
            if (!saved.isNullOrBlank()) {
                if (isStaleUrl(saved)) {
                    Log.w(
                        TAG,
                        "Removing stale server URL override"
                    )

                    prefs.edit()
                        .remove(KEY_SERVER_URL)
                        .apply()

                    customBaseUrl = null
                } else {
                    customBaseUrl = normalizeUrl(saved)
                }
            } else {
                customBaseUrl = null
            }

            cachedApiService = null
        }

        Log.i(TAG, "API configuration initialized")
    }

    private fun isStaleUrl(url: String): Boolean {
        val normalized = url.trim().trimEnd('/')

        return normalized.contains(STALE_IP_MARKER) ||
                normalized.endsWith("/w") ||
                normalized.contains("/w/")
    }

    /**
     * Normalizes a server URL and ensures it ends with '/'.
     */
    fun normalizeUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()

        if (trimmed.isEmpty()) {
            return DEFAULT_EMULATOR_BASE_URL
        }

        val cleaned = when {
            trimmed.endsWith("/w/") ->
                trimmed.removeSuffix("/w/").trimEnd('/')

            trimmed.endsWith("/w") ->
                trimmed.removeSuffix("/w").trimEnd('/')

            else -> trimmed
        }

        val withScheme =
            if (
                !cleaned.startsWith("http://", ignoreCase = true) &&
                !cleaned.startsWith("https://", ignoreCase = true)
            ) {
                "http://$cleaned"
            } else {
                cleaned
            }

        return if (withScheme.endsWith("/")) {
            withScheme
        } else {
            "$withScheme/"
        }
    }

    /**
     * Resolution order:
     * 1. Saved custom server URL
     * 2. BuildConfig.MEDASSIST_API_URL
     * 3. Android emulator default URL
     */
    fun getBaseUrl(context: Context? = null): String {
        customBaseUrl?.let {
            if (it.isNotBlank() && !isStaleUrl(it)) {
                return normalizeUrl(it)
            }
        }

        val ctx = context ?: appContext

        if (ctx != null) {
            val prefs = ctx.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

            val saved = prefs.getString(
                KEY_SERVER_URL,
                null
            )?.trim()

            if (!saved.isNullOrBlank()) {
                if (isStaleUrl(saved)) {
                    prefs.edit()
                        .remove(KEY_SERVER_URL)
                        .apply()

                    customBaseUrl = null
                } else {
                    val normalized = normalizeUrl(saved)
                    customBaseUrl = normalized
                    return normalized
                }
            }
        }

        val configuredUrl = BuildConfig.MEDASSIST_API_URL
            .trim()

        if (
            configuredUrl.isBlank() ||
            configuredUrl.contains("<YOUR_IP>") ||
            configuredUrl.contains(STALE_IP_MARKER)
        ) {
            return DEFAULT_EMULATOR_BASE_URL
        }

        return normalizeUrl(configuredUrl)
    }

    fun isCustomUrlConfigured(context: Context? = null): Boolean {
        customBaseUrl?.let {
            if (it.isNotBlank() && !isStaleUrl(it)) {
                return true
            }
        }

        val ctx = context ?: appContext ?: return false

        val saved = ctx.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).getString(KEY_SERVER_URL, null)?.trim()

        return !saved.isNullOrBlank() && !isStaleUrl(saved)
    }

    /**
     * Saves a custom Flask server URL.
     * Pass null or blank to clear the override.
     */
    fun setCustomServerUrl(
        context: Context,
        url: String?
    ) {
        val app = context.applicationContext
        appContext = app

        val trimmed = url?.trim()

        val prefs = app.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        synchronized(this) {
            if (trimmed.isNullOrBlank()) {
                prefs.edit()
                    .remove(KEY_SERVER_URL)
                    .apply()

                customBaseUrl = null
            } else {
                val normalized = normalizeUrl(trimmed)

                prefs.edit()
                    .putString(KEY_SERVER_URL, normalized)
                    .apply()

                customBaseUrl = normalized
            }

            cachedApiService = null
        }

        Log.i(TAG, "Custom API URL configuration updated")
    }

    fun clearCustomServerUrl(context: Context) {
        val app = context.applicationContext
        appContext = app

        val prefs = app.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        synchronized(this) {
            prefs.edit()
                .remove(KEY_SERVER_URL)
                .apply()

            customBaseUrl = null
            cachedApiService = null
        }

        Log.i(TAG, "Custom API URL cleared")
    }

    fun invalidateApiService() {
        synchronized(this) {
            cachedApiService = null
        }
    }

    // =========================================================
    // HTTP CLIENT
    // =========================================================

    val okHttpClient: OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(RetryInterceptor())
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .build()

    // =========================================================
    // RETROFIT API SERVICE
    // =========================================================

    val apiService: MedicalApiService
        get() {
            cachedApiService?.let {
                return it
            }

            return synchronized(this) {
                cachedApiService?.let {
                    return@synchronized it
                }

                val baseUrl = getBaseUrl()

                Log.i(TAG, "Creating API service for configured server")

                val service = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(
                        GsonConverterFactory.create()
                    )
                    .build()
                    .create(MedicalApiService::class.java)

                cachedApiService = service
                service
            }
        }
}
