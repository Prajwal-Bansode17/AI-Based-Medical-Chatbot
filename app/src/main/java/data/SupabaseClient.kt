package com.example.ai_based_medical_chatbot.data

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

    // =========================================================
    // SUPABASE CONFIGURATION
    // =========================================================

    private const val SUPABASE_URL =
        "https://zjpquzefbtvcortpzkts.supabase.co"

    private const val SUPABASE_KEY =
        "sb_publishable_OkgH1qAH5aEY-b1D7wq0bQ_Vo_O0BHk"


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

                val url =
                    URL(
                        "$SUPABASE_URL/auth/v1/signup"
                    )


                val connection =
                    url.openConnection()
                            as HttpURLConnection


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
                    JSONObject()


                body.put(
                    "email",
                    email.trim()
                )

                body.put(
                    "password",
                    password
                )


                body.put(
                    "data",

                    JSONObject().apply {

                        put(
                            "full_name",
                            fullName.trim()
                        )
                    }
                )


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

                    if (
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


                connection.disconnect()


                if (
                    responseCode in 200..299
                ) {

                    Result.success(
                        "Registration successful"
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

            } catch (e: Exception) {

                Result.failure(
                    Exception(
                        e.message
                            ?: "Unable to connect to Supabase"
                    )
                )
            }
        }


    // =========================================================
    // LOGIN USER
    // =========================================================

    suspend fun loginUser(
        email: String,
        password: String
    ): Result<SupabaseUser> =
        withContext(Dispatchers.IO) {

            try {

                val cleanEmail =
                    email.trim()


                val url =
                    URL(
                        "$SUPABASE_URL/auth/v1/token?grant_type=password"
                    )


                val connection =
                    url.openConnection()
                            as HttpURLConnection


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
                    JSONObject()


                body.put(
                    "email",
                    cleanEmail
                )

                body.put(
                    "password",
                    password
                )


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

                    if (
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
                            ?: "Login failed"
                    }


                connection.disconnect()


                // =================================================
                // LOGIN FAILED
                // =================================================

                if (
                    responseCode !in 200..299
                ) {

                    val message =
                        extractLoginErrorMessage(
                            responseText
                        )


                    return@withContext Result.failure(
                        Exception(message)
                    )
                }


                // =================================================
                // PARSE RESPONSE
                // =================================================

                val json =
                    JSONObject(responseText)


                val user =
                    json.optJSONObject("user")


                if (user == null) {

                    return@withContext Result.failure(
                        Exception(
                            "Login failed: user information was not returned."
                        )
                    )
                }


                val userId =
                    user.optString("id")


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


                // =================================================
                // SUCCESS
                // =================================================

                Result.success(

                    SupabaseUser(

                        id =
                            userId,

                        email =
                            userEmail,

                        fullName =
                            fullName
                    )
                )

            } catch (e: Exception) {

                Result.failure(
                    Exception(
                        e.message
                            ?: "Unable to connect to Supabase"
                    )
                )
            }
        }


    // =========================================================
    // LOGIN ERROR MESSAGE
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


            // -----------------------------------------------
            // EMAIL NOT CONFIRMED
            // -----------------------------------------------

            if (
                errorCode == "email_not_confirmed" ||
                errorDescription
                    .contains(
                        "Email not confirmed",
                        ignoreCase = true
                    ) ||
                message
                    .contains(
                        "Email not confirmed",
                        ignoreCase = true
                    )
            ) {

                return "Email not confirmed. Please verify your email first."
            }


            // -----------------------------------------------
            // INVALID LOGIN
            // -----------------------------------------------

            if (
                errorCode == "invalid_credentials" ||
                errorDescription
                    .contains(
                        "Invalid login credentials",
                        ignoreCase = true
                    ) ||
                message
                    .contains(
                        "Invalid login credentials",
                        ignoreCase = true
                    )
            ) {

                return "Account not found or password is incorrect. If you don't have an account, please create one first."
            }


            // -----------------------------------------------
            // OTHER ERRORS
            // -----------------------------------------------

            when {

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
    // GENERAL ERROR MESSAGE
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


            if (
                errorCode == "user_already_exists"
            ) {

                return "An account with this email already exists. Please login."
            }


            when {

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

            response.ifBlank {
                "Registration failed. Please try again."
            }
        }
    }
}