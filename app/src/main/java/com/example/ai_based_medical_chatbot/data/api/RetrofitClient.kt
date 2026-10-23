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
            val isIdempotent = request.method.equals("GET", ignoreCase = true) ||
                request.method.equals("HEAD", ignoreCase = true)

            var attempt = 0
            var lastException: Exception? = null

            while (attempt <= maxRetries) {

                try {

                    return chain.proceed(request)

                } catch (e: Exception) {

                    lastException = e

                    // Do not automatically retry non-idempotent operations (such as POST /predict)
                    // on socket timeouts to avoid duplicating model calls or saving duplicate records.
                    if (
                        e !is IOException ||
                        attempt == maxRetries ||
                        (!isIdempotent && e is java.net.SocketTimeoutException)
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

    const val PREFS_NAME = "medassist_network_config"
    const val KEY_SERVER_URL = "custom_server_url"
    const val STALE_IP_MARKER = "192.168.1.12"

    @Volatile
    private var customBaseUrl: String? = null

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var cachedApiService: MedicalApiService? = null

    fun initialize(context: Context) {
        val app = context.applicationContext ?: context
        appContext = app

        val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_SERVER_URL, null)?.trim()
        if (!saved.isNullOrBlank()) {
            if (saved.contains(STALE_IP_MARKER) || saved.endsWith("/w") || saved.contains("/w/")) {
                Log.w(TAG, "Purging stale server URL override ($saved) from SharedPreferences")
                prefs.edit().remove(KEY_SERVER_URL).apply()
                customBaseUrl = null
            } else {
                customBaseUrl = normalizeUrl(saved)
            }
        } else {
            customBaseUrl = null
        }

        synchronized(this) {
            cachedApiService = null
        }
    }

    fun normalizeUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return "http://10.0.2.2:5000/"

        // Remove accidental typos or trailing path fragments like '/w' or '/w/'
        val cleaned = when {
            trimmed.endsWith("/w/") -> trimmed.removeSuffix("/w/").trimEnd('/')
            trimmed.endsWith("/w") -> trimmed.removeSuffix("/w").trimEnd('/')
            else -> trimmed
        }

        val withScheme = if (!cleaned.startsWith("http://") && !cleaned.startsWith("https://")) {
            "http://$cleaned"
        } else {
            cleaned
        }
        return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
    }

    fun getBaseUrl(context: Context? = null): String {
        customBaseUrl?.let { if (it.isNotBlank()) return normalizeUrl(it) }

        val ctx = context ?: appContext
        if (ctx != null) {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val saved = prefs.getString(KEY_SERVER_URL, null)?.trim()
            if (!saved.isNullOrBlank()) {
                if (saved.contains(STALE_IP_MARKER) || saved.endsWith("/w") || saved.contains("/w/")) {
                    prefs.edit().remove(KEY_SERVER_URL).apply()
                    customBaseUrl = null
                } else {
                    val normalized = normalizeUrl(saved)
                    customBaseUrl = normalized
                    return normalized
                }
            }
        }

        return normalizeUrl(BuildConfig.MEDASSIST_API_URL)
    }

    fun isCustomUrlConfigured(context: Context? = null): Boolean {
        customBaseUrl?.let { if (it.isNotBlank()) return true }
        val ctx = context ?: appContext ?: return false
        val saved = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SERVER_URL, null)?.trim()
        return !saved.isNullOrBlank() && !saved.contains(STALE_IP_MARKER)
    }

    fun setCustomServerUrl(context: Context, url: String?) {
        val app = context.applicationContext ?: context
        appContext = app
        val trimmed = url?.trim()
        val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        synchronized(this) {
            if (trimmed.isNullOrBlank()) {
                prefs.edit().remove(KEY_SERVER_URL).apply()
                customBaseUrl = null
            } else {
                val normalized = normalizeUrl(trimmed)
                prefs.edit().putString(KEY_SERVER_URL, normalized).apply()
                customBaseUrl = normalized
            }
            cachedApiService = null
        }
    }

    fun clearCustomServerUrl(context: Context) {
        val app = context.applicationContext ?: context
        appContext = app
        val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        synchronized(this) {
            prefs.edit().remove(KEY_SERVER_URL).apply()
            customBaseUrl = null
            cachedApiService = null
        }
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
