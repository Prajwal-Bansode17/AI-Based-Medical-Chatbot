package com.example.ai_based_medical_chatbot.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class SupabaseUser(
    val id: String,
    val email: String,
    val fullName: String
)

object SupabaseClient {

    private const val SUPABASE_URL =
        "https://zjpquzefbtvcortpzkts.supabase.co"

    private const val SUPABASE_KEY =
        "sb_publishable_OkgH1qAH5aEY-b1D7wq0bQ_Vo_O0BHk"

    private const val PREFS_NAME =
        "medassist_auth"

    private const val KEY_USER_ID =
        "user_id"

    private const val KEY_EMAIL =
        "email"

    private const val KEY_FULL_NAME =
        "full_name"

    private val jsonMediaType =
        "application/json; charset=utf-8".toMediaType()

    /*
     * Supabase login is intentionally handled with OkHttp.
     *
     * The project already contains OkHttp 4.12.0 in build.gradle.kts.
     * This avoids the Ktor Android-engine timeout that was happening
     * during the Supabase Auth request.
     */
    private val httpClient =
        OkHttpClient.Builder()
            .connectTimeout(
                30,
                TimeUnit.SECONDS
            )
            .readTimeout(
                30,
                TimeUnit.SECONDS
            )
            .writeTimeout(
                30,
                TimeUnit.SECONDS
            )
            .callTimeout(
                35,
                TimeUnit.SECONDS
            )
            .retryOnConnectionFailure(true)
            .build()

    private fun saveUser(
        context: Context,
        user: SupabaseUser
    ) {
        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                KEY_USER_ID,
                user.id
            )
            .putString(
                KEY_EMAIL,
                user.email
            )
            .putString(
                KEY_FULL_NAME,
                user.fullName
            )
            .apply()
    }

    fun getSavedUser(
        context: Context
    ): SupabaseUser? {

        val preferences =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val id =
            preferences.getString(
                KEY_USER_ID,
                null
            )

        val email =
            preferences.getString(
                KEY_EMAIL,
                null
            )

        val fullName =
            preferences.getString(
                KEY_FULL_NAME,
                ""
            ) ?: ""

        if (
            id.isNullOrBlank() ||
            email.isNullOrBlank()
        ) {
            return null
        }

        return SupabaseUser(
            id = id,
            email = email,
            fullName = fullName
        )
    }

    fun getChatSessionId(
        context: Context
    ): String? {

        val user =
            getSavedUser(context)
                ?: return null

        return "supabase_user_${user.id}"
    }

    /*
     * Persistent login:
     * MainActivity can call this when the application starts.
     * The saved user is returned immediately, so reopening the app
     * does not force the user to enter the password again.
     */
    suspend fun restoreSessionUser(
        context: Context
    ): SupabaseUser? =
        withContext(Dispatchers.IO) {
            getSavedUser(context)
        }

    /*
     * Local logout. This is deliberately non-suspend because it only
     * clears SharedPreferences and MainActivity can call it directly.
     */
    fun clearSession(
        context: Context
    ) {
        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .clear()
            .apply()
    }

    suspend fun logoutUser(
        context: Context
    ) {
        clearSession(context)
    }

    suspend fun registerUser(
        email: String,
        password: String,
        fullName: String
    ): Result<String> =
        withContext(Dispatchers.IO) {

            val cleanEmail =
                email.trim()

            val cleanName =
                fullName.trim()

            if (cleanEmail.isBlank()) {
                return@withContext Result.failure(
                    Exception(
                        "Please enter your email address."
                    )
                )
            }

            if (password.length < 6) {
                return@withContext Result.failure(
                    Exception(
                        "Password must contain at least 6 characters."
                    )
                )
            }

            if (cleanName.isBlank()) {
                return@withContext Result.failure(
                    Exception(
                        "Please enter your full name."
                    )
                )
            }

            try {

                val body =
                    JSONObject().apply {
                        put(
                            "email",
                            cleanEmail
                        )
                        put(
                            "password",
                            password
                        )
                        put(
                            "data",
                            JSONObject().apply {
                                put(
                                    "full_name",
                                    cleanName
                                )
                            }
                        )
                    }

                val request =
                    Request.Builder()
                        .url(
                            "$SUPABASE_URL/auth/v1/signup"
                        )
                        .post(
                            body
                                .toString()
                                .toRequestBody(
                                    jsonMediaType
                                )
                        )
                        .header(
                            "apikey",
                            SUPABASE_KEY
                        )
                        .header(
                            "Authorization",
                            "Bearer $SUPABASE_KEY"
                        )
                        .header(
                            "Accept",
                            "application/json"
                        )
                        .build()

                httpClient
                    .newCall(request)
                    .execute()
                    .use { response ->

                        val responseText =
                            response.body
                                ?.string()
                                .orEmpty()

                        if (!response.isSuccessful) {
                            return@withContext Result.failure(
                                Exception(
                                    extractAuthError(
                                        responseText,
                                        false
                                    )
                                )
                            )
                        }

                        Result.success(
                            "Registration successful. Please verify your email before logging in."
                        )
                    }

            } catch (e: java.net.SocketTimeoutException) {

                Result.failure(
                    Exception(
                        "Supabase request timed out. Please try again."
                    )
                )

            } catch (e: IOException) {

                Result.failure(
                    Exception(
                        "Unable to connect to Supabase. Please check your internet connection."
                    )
                )

            } catch (e: Exception) {

                Result.failure(
                    Exception(
                        e.message
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: "Registration failed. Please try again."
                    )
                )
            }
        }

    suspend fun loginUser(
        email: String,
        password: String,
        context: Context
    ): Result<SupabaseUser> =
        withContext(Dispatchers.IO) {

            val cleanEmail =
                email.trim()

            if (cleanEmail.isBlank()) {
                return@withContext Result.failure(
                    Exception(
                        "Please enter your email address."
                    )
                )
            }

            if (password.isBlank()) {
                return@withContext Result.failure(
                    Exception(
                        "Please enter your password."
                    )
                )
            }

            try {

                val body =
                    JSONObject().apply {
                        put(
                            "email",
                            cleanEmail
                        )
                        put(
                            "password",
                            password
                        )
                    }

                val request =
                    Request.Builder()
                        .url(
                            "$SUPABASE_URL/auth/v1/token?grant_type=password"
                        )
                        .post(
                            body
                                .toString()
                                .toRequestBody(
                                    jsonMediaType
                                )
                        )
                        .header(
                            "apikey",
                            SUPABASE_KEY
                        )
                        .header(
                            "Authorization",
                            "Bearer $SUPABASE_KEY"
                        )
                        .header(
                            "Accept",
                            "application/json"
                        )
                        .build()

                httpClient
                    .newCall(request)
                    .execute()
                    .use { response ->

                        val responseText =
                            response.body
                                ?.string()
                                .orEmpty()

                        if (!response.isSuccessful) {
                            return@withContext Result.failure(
                                Exception(
                                    extractAuthError(
                                        responseText,
                                        true
                                    )
                                )
                            )
                        }

                        val json =
                            JSONObject(
                                responseText
                            )

                        val user =
                            json.optJSONObject(
                                "user"
                            )
                                ?: return@withContext Result.failure(
                                    Exception(
                                        "Login failed: user information was not returned."
                                    )
                                )

                        val userId =
                            user.optString(
                                "id"
                            )

                        if (userId.isBlank()) {
                            return@withContext Result.failure(
                                Exception(
                                    "Login failed: user ID was not returned."
                                )
                            )
                        }

                        val userEmail =
                            user.optString(
                                "email",
                                cleanEmail
                            )

                        val metadata =
                            user.optJSONObject(
                                "user_metadata"
                            )

                        val fullName =
                            metadata?.optString(
                                "full_name",
                                ""
                            ) ?: ""

                        val loggedInUser =
                            SupabaseUser(
                                id = userId,
                                email = userEmail,
                                fullName = fullName
                            )

                        saveUser(
                            context,
                            loggedInUser
                        )

                        Result.success(
                            loggedInUser
                        )
                    }

            } catch (e: java.net.SocketTimeoutException) {

                Result.failure(
                    Exception(
                        "Supabase request timed out. Please try again."
                    )
                )

            } catch (e: IOException) {

                Result.failure(
                    Exception(
                        "Unable to connect to Supabase. Please check your internet connection."
                    )
                )

            } catch (e: Exception) {

                Result.failure(
                    Exception(
                        e.message
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: "Login failed. Please try again."
                    )
                )
            }
        }

    private fun extractAuthError(
        response: String,
        isLogin: Boolean
    ): String {

        if (response.isBlank()) {
            return if (isLogin) {
                "Login failed. Please try again."
            } else {
                "Registration failed. Please try again."
            }
        }

        return try {

            val json =
                JSONObject(
                    response
                )

            val errorCode =
                json.optString(
                    "error_code",
                    ""
                )

            val errorDescription =
                json.optString(
                    "error_description",
                    ""
                )

            val message =
                json.optString(
                    "msg",
                    ""
                )

            val normalMessage =
                json.optString(
                    "message",
                    ""
                )

            val combined =
                listOf(
                    errorCode,
                    errorDescription,
                    message,
                    normalMessage
                ).joinToString(" ")

            when {

                combined.contains(
                    "email_not_confirmed",
                    ignoreCase = true
                ) ||
                        combined.contains(
                            "email not confirmed",
                            ignoreCase = true
                        ) ->
                    "Email not confirmed. Please verify your email first."

                combined.contains(
                    "invalid_credentials",
                    ignoreCase = true
                ) ||
                        combined.contains(
                            "invalid login credentials",
                            ignoreCase = true
                        ) ->
                    "Account not found or password is incorrect."

                combined.contains(
                    "user_already_exists",
                    ignoreCase = true
                ) ||
                        combined.contains(
                            "already registered",
                            ignoreCase = true
                        ) ->
                    "An account with this email already exists. Please login."

                errorDescription.isNotBlank() ->
                    errorDescription

                message.isNotBlank() ->
                    message

                normalMessage.isNotBlank() ->
                    normalMessage

                errorCode.isNotBlank() ->
                    errorCode

                else ->
                    if (isLogin) {
                        "Login failed. Please check your email and password."
                    } else {
                        "Registration failed. Please try again."
                    }
            }

        } catch (_: Exception) {

            if (isLogin) {
                "Login failed. Please check your email and password."
            } else {
                "Registration failed. Please try again."
            }
        }
    }
}
