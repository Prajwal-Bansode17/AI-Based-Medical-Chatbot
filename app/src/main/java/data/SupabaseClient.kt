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

data class HealthProfile(
    val id: String,
    val name: String? = null,
    val gender: String? = null,
    val dateOfBirth: String? = null,
    val bloodGroup: String? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val bmi: Double? = null
)

object SupabaseClient {

    private const val SUPABASE_URL =
        "https://pdqbptnodhqxwdriypxi.supabase.co"

    private const val SUPABASE_KEY =
        "sb_publishable_pdpU_5XNz-thcx0bv3UPYg_iEulHfsa"

    private const val PREFS_NAME =
        "medassist_auth"

    private const val KEY_USER_ID =
        "user_id"

    private const val KEY_EMAIL =
        "email"

    private const val KEY_FULL_NAME =
        "full_name"

    private const val KEY_ACCESS_TOKEN =
        "access_token"

    private val jsonMediaType =
        "application/json; charset=utf-8".toMediaType()

    private val httpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(35, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

    private fun saveUser(
        context: Context,
        user: SupabaseUser,
        accessToken: String? = null
    ) {
        val editor = context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_FULL_NAME, user.fullName)

        if (!accessToken.isNullOrBlank()) {
            editor.putString(
                KEY_ACCESS_TOKEN,
                accessToken
            )
        }

        editor.apply()
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
            preferences.getString(KEY_USER_ID, null)

        val email =
            preferences.getString(KEY_EMAIL, null)

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

    private fun getAccessToken(
        context: Context
    ): String? {
        return context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .getString(
                KEY_ACCESS_TOKEN,
                null
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

    suspend fun restoreSessionUser(
        context: Context
    ): SupabaseUser? =
        withContext(Dispatchers.IO) {
            getSavedUser(context)
        }

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

            val cleanEmail = email.trim()
            val cleanName = fullName.trim()

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
                        put("email", cleanEmail)
                        put("password", password)
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

                        /*
                         * If email confirmation is disabled,
                         * Supabase may return an access token and
                         * an authenticated user immediately.
                         *
                         * If confirmation is enabled, there may
                         * be no token. The normal login flow will
                         * still work after email verification.
                         */
                        val json =
                            JSONObject(responseText)

                        val accessToken =
                            json.optString(
                                "access_token",
                                ""
                            )

                        val user =
                            json.optJSONObject("user")

                        Result.success(
                            "Registration successful."
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

            val cleanEmail = email.trim()

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
                        put("email", cleanEmail)
                        put("password", password)
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

                        val accessToken =
                            json.optString(
                                "access_token",
                                ""
                            )

                        saveUser(
                            context = context,
                            user = loggedInUser,
                            accessToken = accessToken
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

    /*
     * Load the authenticated user's profile.
     */
    suspend fun getHealthProfile(
        context: Context
    ): Result<HealthProfile?> =
        withContext(Dispatchers.IO) {

            val user =
                getSavedUser(context)
                    ?: return@withContext Result.success(null)

            val token =
                getAccessToken(context)
                    ?: return@withContext Result.failure(
                        Exception(
                            "Your session has expired. Please login again."
                        )
                    )

            try {

                val request =
                    Request.Builder()
                        .url(
                            "$SUPABASE_URL/rest/v1/profiles" +
                                    "?id=eq.${user.id}" +
                                    "&select=id,name,gender,date_of_birth,blood_group,height_cm,weight_kg,bmi"
                        )
                        .get()
                        .header(
                            "apikey",
                            SUPABASE_KEY
                        )
                        .header(
                            "Authorization",
                            "Bearer $token"
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
                                    buildString {
                                        append("Unable to load personal information. Server error: ${response.code}")
                                        if (responseText.isNotBlank()) {
                                            append(" - ")
                                            append(responseText)
                                        }
                                    }
                                )
                            )
                        }

                        val array =
                            org.json.JSONArray(
                                responseText
                            )

                        if (array.length() == 0) {
                            return@withContext Result.success(
                                null
                            )
                        }

                        val json =
                            array.getJSONObject(0)

                        Result.success(
                            healthProfileFromJson(json)
                        )
                    }

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
                            ?: "Unable to load personal information."
                    )
                )
            }
        }

    /*
     * Create/update the authenticated user's profile.
     * All health fields are optional.
     */
    suspend fun saveHealthProfile(
        context: Context,
        profile: HealthProfile
    ): Result<HealthProfile> =
        withContext(Dispatchers.IO) {

            val user =
                getSavedUser(context)
                    ?: return@withContext Result.failure(
                        Exception(
                            "Please login before saving personal information."
                        )
                    )

            val token =
                getAccessToken(context)
                    ?: return@withContext Result.failure(
                        Exception(
                            "Your session has expired. Please login again."
                        )
                    )

            try {

                val body =
                    JSONObject().apply {
                        put("id", user.id)
                        putNullable("name", profile.name)
                        putNullable("gender", profile.gender)
                        putNullable(
                            "date_of_birth",
                            profile.dateOfBirth
                        )
                        putNullable(
                            "blood_group",
                            profile.bloodGroup
                        )
                        putNullable(
                            "height_cm",
                            profile.heightCm
                        )
                        putNullable(
                            "weight_kg",
                            profile.weightKg
                        )
                        putNullable(
                            "bmi",
                            profile.bmi
                        )
                    }

                val request =
                    Request.Builder()
                        .url(
                            "$SUPABASE_URL/rest/v1/profiles?on_conflict=id"
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
                            "Bearer $token"
                        )
                        .header(
                            "Prefer",
                            "resolution=merge-duplicates,return=representation"
                        )
                        .header(
                            "Content-Type",
                            "application/json"
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
                                    buildString {
                                        append("Unable to save personal information. Server error: ${response.code}")
                                        if (responseText.isNotBlank()) {
                                            append(" - ")
                                            append(responseText)
                                        }
                                    }
                                )
                            )
                        }

                        val array =
                            org.json.JSONArray(
                                responseText
                            )

                        if (array.length() == 0) {
                            return@withContext Result.success(
                                profile.copy(
                                    id = user.id
                                )
                            )
                        }

                        Result.success(
                            healthProfileFromJson(
                                array.getJSONObject(0)
                            )
                        )
                    }

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
                            ?: "Unable to save personal information."
                    )
                )
            }
        }

    private fun JSONObject.putNullable(
        key: String,
        value: Any?
    ) {
        put(
            key,
            value ?: JSONObject.NULL
        )
    }

    private fun healthProfileFromJson(
        json: JSONObject
    ): HealthProfile {

        return HealthProfile(
            id = json.optString("id"),
            name = json.optNullableString("name"),
            gender = json.optNullableString("gender"),
            dateOfBirth =
                json.optNullableString(
                    "date_of_birth"
                ),
            bloodGroup =
                json.optNullableString(
                    "blood_group"
                ),
            heightCm =
                json.optNullableDouble(
                    "height_cm"
                ),
            weightKg =
                json.optNullableDouble(
                    "weight_kg"
                ),
            bmi =
                json.optNullableDouble(
                    "bmi"
                )
        )
    }

    private fun JSONObject.optNullableString(
        key: String
    ): String? {

        if (
            isNull(key)
        ) {
            return null
        }

        return optString(
            key,
            ""
        ).takeIf {
            it.isNotBlank()
        }
    }

    private fun JSONObject.optNullableDouble(
        key: String
    ): Double? {

        if (isNull(key)) {
            return null
        }

        val value =
            optDouble(
                key,
                Double.NaN
            )

        return value.takeIf {
            !it.isNaN()
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
