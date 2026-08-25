package com.example.ai_based_medical_chatbot.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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

    // =========================================================
    // SAVE USER SESSION
    // =========================================================

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
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_FULL_NAME, user.fullName)
            .apply()
    }

    // =========================================================
    // GET SAVED USER
    // =========================================================

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

    // =========================================================
    // GET CHAT SESSION ID
    // =========================================================

    fun getChatSessionId(
        context: Context
    ): String? {

        val user =
            getSavedUser(context)
                ?: return null

        return "supabase_user_${user.id}"
    }

    // =========================================================
    // CLEAR SESSION
    // =========================================================

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

    // =========================================================
    // REGISTER USER
    // =========================================================

    suspend fun registerUser(
        email: String,
        password: String,
        fullName: String
    ): Result<String> =
        withContext(Dispatchers.IO) {

            try {

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

                val url =
                    URL(
                        "$SUPABASE_URL/auth/v1/signup"
                    )

                val connection =
                    url.openConnection()
                            as HttpURLConnection

                try {

                    connection.requestMethod =
                        "POST"

                    connection.doOutput =
                        true

                    connection.connectTimeout =
                        15000

                    connection.readTimeout =
                        15000

                    connection.setRequestProperty(
                        "Content-Type",
                        "application/json"
                    )

                    connection.setRequestProperty(
                        "Accept",
                        "application/json"
                    )

                    connection.setRequestProperty(
                        "apikey",
                        SUPABASE_KEY
                    )

                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer $SUPABASE_KEY"
                    )

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

                    connection.outputStream.use { output ->

                        output.write(
                            body.toString()
                                .toByteArray(
                                    Charsets.UTF_8
                                )
                        )
                    }

                    val responseCode =
                        connection.responseCode

                    val responseText =
                        readResponse(
                            connection,
                            responseCode
                        )

                    if (
                        responseCode in 200..299
                    ) {

                        Result.success(
                            "Registration successful. Please verify your email before logging in."
                        )

                    } else {

                        Result.failure(
                            Exception(
                                extractErrorMessage(
                                    responseText
                                )
                            )
                        )
                    }

                } finally {

                    connection.disconnect()
                }

            } catch (e: Exception) {

                Result.failure(
                    Exception(
                        e.message
                            ?: "Unable to connect to Supabase."
                    )
                )
            }
        }

    // =========================================================
    // LOGIN USER
    // =========================================================

    suspend fun loginUser(
        email: String,
        password: String,
        context: Context
    ): Result<SupabaseUser> =
        withContext(Dispatchers.IO) {

            try {

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

                val url =
                    URL(
                        "$SUPABASE_URL/auth/v1/token?grant_type=password"
                    )

                val connection =
                    url.openConnection()
                            as HttpURLConnection

                try {

                    connection.requestMethod =
                        "POST"

                    connection.doOutput =
                        true

                    connection.connectTimeout =
                        15000

                    connection.readTimeout =
                        15000

                    connection.setRequestProperty(
                        "Content-Type",
                        "application/json"
                    )

                    connection.setRequestProperty(
                        "Accept",
                        "application/json"
                    )

                    connection.setRequestProperty(
                        "apikey",
                        SUPABASE_KEY
                    )

                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer $SUPABASE_KEY"
                    )

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

                    connection.outputStream.use { output ->

                        output.write(
                            body.toString()
                                .toByteArray(
                                    Charsets.UTF_8
                                )
                        )
                    }

                    val responseCode =
                        connection.responseCode

                    val responseText =
                        readResponse(
                            connection,
                            responseCode
                        )

                    if (
                        responseCode !in 200..299
                    ) {

                        return@withContext Result.failure(
                            Exception(
                                extractLoginErrorMessage(
                                    responseText
                                )
                            )
                        )
                    }

                    val json =
                        JSONObject(responseText)

                    val user =
                        json.optJSONObject("user")

                            ?: return@withContext Result.failure(
                                Exception(
                                    "Login failed: user information was not returned."
                                )
                            )

                    val userId =
                        user.optString("id")

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

                    // Save login session
                    saveUser(
                        context,
                        loggedInUser
                    )

                    Result.success(
                        loggedInUser
                    )

                } finally {

                    connection.disconnect()
                }

            } catch (e: Exception) {

                Result.failure(
                    Exception(
                        e.message
                            ?: "Unable to connect to Supabase."
                    )
                )
            }
        }

    // =========================================================
    // READ RESPONSE
    // =========================================================

    private fun readResponse(
        connection: HttpURLConnection,
        responseCode: Int
    ): String {

        return if (
            responseCode in 200..299
        ) {

            connection.inputStream
                .bufferedReader()
                .use {
                    it.readText()
                }

        } else {

            connection.errorStream
                ?.bufferedReader()
                ?.use {
                    it.readText()
                }
                ?: "Unknown Supabase error"
        }
    }

    // =========================================================
    // LOGIN ERROR
    // =========================================================

    private fun extractLoginErrorMessage(
        response: String
    ): String {

        return try {

            val json =
                JSONObject(response)

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

            when {

                errorCode ==
                        "email_not_confirmed" ->

                    "Email not confirmed. Please verify your email first."

                errorDescription.contains(
                    "Email not confirmed",
                    ignoreCase = true
                ) ->

                    "Email not confirmed. Please verify your email first."

                message.contains(
                    "Email not confirmed",
                    ignoreCase = true
                ) ->

                    "Email not confirmed. Please verify your email first."

                errorCode ==
                        "invalid_credentials" ->

                    "Account not found or password is incorrect."

                errorDescription.contains(
                    "Invalid login credentials",
                    ignoreCase = true
                ) ->

                    "Account not found or password is incorrect."

                message.contains(
                    "Invalid login credentials",
                    ignoreCase = true
                ) ->

                    "Account not found or password is incorrect."

                errorDescription.isNotBlank() ->
                    errorDescription

                message.isNotBlank() ->
                    message

                normalMessage.isNotBlank() ->
                    normalMessage

                else ->
                    "Login failed. Please check your email and password."
            }

        } catch (_: Exception) {

            "Login failed. Please check your email and password."
        }
    }

    // =========================================================
    // GENERAL ERROR
    // =========================================================

    private fun extractErrorMessage(
        response: String
    ): String {

        return try {

            val json =
                JSONObject(response)

            val errorCode =
                json.optString(
                    "error_code",
                    ""
                )

            val message =
                json.optString(
                    "msg",
                    ""
                )

            val errorDescription =
                json.optString(
                    "error_description",
                    ""
                )

            val normalMessage =
                json.optString(
                    "message",
                    ""
                )

            when {

                errorCode ==
                        "user_already_exists" ->

                    "An account with this email already exists. Please login."

                message.isNotBlank() ->
                    message

                errorDescription.isNotBlank() ->
                    errorDescription

                normalMessage.isNotBlank() ->
                    normalMessage

                else ->
                    "Registration failed. Please try again."
            }

        } catch (_: Exception) {

            if (
                response.isNotBlank()
            ) {

                response

            } else {

                "Registration failed. Please try again."
            }
        }
    }
}