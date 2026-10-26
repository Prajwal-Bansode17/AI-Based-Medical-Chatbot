package com.example.ai_based_medical_chatbot.data

import android.content.Context
import android.util.Log
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

    private const val KEY_REFRESH_TOKEN =
        "refresh_token"

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
        accessToken: String? = null,
        refreshToken: String? = null
    ) {
        val appContext = context.applicationContext
        val editor = appContext
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

        if (!refreshToken.isNullOrBlank()) {
            editor.putString(
                KEY_REFRESH_TOKEN,
                refreshToken
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

    fun getAccessToken(
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

    fun getRefreshToken(
        context: Context
    ): String? {
        return context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .getString(
                KEY_REFRESH_TOKEN,
                null
            )
    }

    suspend fun refreshSession(
        context: Context
    ): Result<String> = withContext(Dispatchers.IO) {
        val refreshToken = getRefreshToken(context)
        if (refreshToken.isNullOrBlank()) {
            Log.w("SupabaseClient", "refreshSession: No refresh token stored in SharedPreferences.")
            return@withContext Result.failure(Exception("No refresh token available."))
        }

        try {
            val body = JSONObject().apply {
                put("refresh_token", refreshToken)
            }

            val request = Request.Builder()
                .url("$SUPABASE_URL/auth/v1/token?grant_type=refresh_token")
                .post(body.toString().toRequestBody(jsonMediaType))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer $SUPABASE_KEY")
                .header("Accept", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.e("SupabaseClient", "refreshSession failed: HTTP ${response.code}")
                    return@withContext Result.failure(Exception("Session refresh failed: HTTP ${response.code}"))
                }

                val json = JSONObject(responseText)
                val newAccessToken = json.optString("access_token", "")
                val newRefreshToken = json.optString("refresh_token", "")
                val savedUser = getSavedUser(context)

                if (newAccessToken.isNotBlank() && savedUser != null) {
                    saveUser(
                        context = context.applicationContext,
                        user = savedUser,
                        accessToken = newAccessToken,
                        refreshToken = newRefreshToken.takeIf { it.isNotBlank() } ?: refreshToken
                    )
                    Log.d("SupabaseClient", "refreshSession: Access token refreshed successfully.")
                    return@withContext Result.success(newAccessToken)
                }

                Log.w("SupabaseClient", "refreshSession: response did not contain a usable session.")
                Result.failure(Exception("Invalid session refresh response. Please login again."))
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "refreshSession error: ${e.message}")
            Result.failure(e)
        }
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

                        val refreshToken =
                            json.optString(
                                "refresh_token",
                                ""
                            )

                        saveUser(
                            context = context,
                            user = loggedInUser,
                            accessToken = accessToken,
                            refreshToken = refreshToken
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

    /**
     * Re-authenticate the user with the current password and then update
     * the Supabase Auth password using the fresh access token.
     */
    suspend fun changePassword(
        context: Context,
        email: String,
        currentPassword: String,
        newPassword: String
    ): Result<Unit> = withContext(Dispatchers.IO) {

        val cleanEmail = email.trim()

        if (cleanEmail.isBlank()) {
            return@withContext Result.failure(
                Exception("Email address is required.")
            )
        }

        if (currentPassword.isBlank()) {
            return@withContext Result.failure(
                Exception("Please enter your current password.")
            )
        }

        if (newPassword.length < 6) {
            return@withContext Result.failure(
                Exception("New password must contain at least 6 characters.")
            )
        }

        if (currentPassword == newPassword) {
            return@withContext Result.failure(
                Exception("New password must be different from your current password.")
            )
        }

        try {
            val loginResult = loginUser(
                email = cleanEmail,
                password = currentPassword,
                context = context
            )

            if (loginResult.isFailure) {
                return@withContext Result.failure(
                    loginResult.exceptionOrNull()
                        ?: Exception("Current password is incorrect.")
                )
            }

            val token =
                getAccessToken(context)
                    ?: return@withContext Result.failure(
                        Exception("Your session has expired. Please login again.")
                    )

            val body =
                JSONObject().apply {
                    put("password", newPassword)
                }

            val request =
                Request.Builder()
                    .url("$SUPABASE_URL/auth/v1/user")
                    .put(
                        body
                            .toString()
                            .toRequestBody(jsonMediaType)
                    )
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/json")
                    .build()

            httpClient
                .newCall(request)
                .execute()
                .use { response ->
                    val responseText =
                        response.body?.string().orEmpty()

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

                    Result.success(Unit)
                }

        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(
                Exception("Supabase request timed out. Please try again.")
            )
        } catch (e: IOException) {
            Result.failure(
                Exception("Unable to connect to Supabase. Please check your internet connection.")
            )
        } catch (e: Exception) {
            Result.failure(
                Exception(
                    e.message?.takeIf { it.isNotBlank() }
                        ?: "Unable to change password. Please try again."
                )
            )
        }
    }

    /*
     * Send password reset recovery email via Supabase Auth REST endpoint.
     */
    suspend fun sendPasswordResetEmail(
        email: String,
        redirectTo: String = "medassist://reset-password"
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            val cleanEmail = email.trim()

            if (cleanEmail.isBlank()) {
                return@withContext Result.failure(
                    Exception("Please enter your email address.")
                )
            }

            try {
                val body = JSONObject().apply {
                    put("email", cleanEmail)
                }

                val encodedRedirect = java.net.URLEncoder.encode(redirectTo, "UTF-8")
                val requestUrl = "$SUPABASE_URL/auth/v1/recover?redirect_to=$encodedRedirect"

                val request = Request.Builder()
                    .url(requestUrl)
                    .post(
                        body.toString().toRequestBody(jsonMediaType)
                    )
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer $SUPABASE_KEY")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val responseText = response.body?.string().orEmpty()

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

                    Result.success(Unit)
                }
            } catch (e: java.net.SocketTimeoutException) {
                Result.failure(
                    Exception("Supabase request timed out. Please check your connection and try again.")
                )
            } catch (e: IOException) {
                Result.failure(
                    Exception("Unable to connect to Supabase. Please check your internet connection.")
                )
            } catch (e: Exception) {
                Result.failure(
                    Exception(
                        e.message?.takeIf { it.isNotBlank() }
                            ?: "Failed to send password reset email. Please try again."
                    )
                )
            }
        }

    /*
     * Fetch user profile using a recovery / access token.
     */
    suspend fun getUserWithToken(
        accessToken: String
    ): Result<SupabaseUser> =
        withContext(Dispatchers.IO) {
            val cleanToken = accessToken.trim()
            if (cleanToken.isBlank()) {
                return@withContext Result.failure(
                    Exception("Invalid or missing session token.")
                )
            }

            try {
                val request = Request.Builder()
                    .url("$SUPABASE_URL/auth/v1/user")
                    .get()
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer $cleanToken")
                    .header("Accept", "application/json")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val responseText = response.body?.string().orEmpty()

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

                    val user = JSONObject(responseText)
                    val userId = user.optString("id")
                    if (userId.isBlank()) {
                        return@withContext Result.failure(
                            Exception("Unable to retrieve user information from recovery token.")
                        )
                    }

                    val userEmail = user.optString("email", "")
                    val metadata = user.optJSONObject("user_metadata")
                    val fullName = metadata?.optString("full_name", "") ?: ""

                    Result.success(
                        SupabaseUser(
                            id = userId,
                            email = userEmail,
                            fullName = fullName
                        )
                    )
                }
            } catch (e: java.net.SocketTimeoutException) {
                Result.failure(
                    Exception("Request timed out. Please check your connection and try again.")
                )
            } catch (e: IOException) {
                Result.failure(
                    Exception("Unable to connect to Supabase. Please check your internet connection.")
                )
            } catch (e: Exception) {
                Result.failure(
                    Exception(
                        e.message?.takeIf { it.isNotBlank() }
                            ?: "Failed to verify recovery token. The link may have expired."
                    )
                )
            }
        }

    /*
     * Reset password using a valid recovery access token (from email link).
     * Does NOT require the old password.
     */
    suspend fun resetPasswordWithToken(
        accessToken: String,
        newPassword: String,
        context: Context? = null
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            val cleanToken = accessToken.trim()

            if (cleanToken.isBlank()) {
                return@withContext Result.failure(
                    Exception("Your recovery token is missing or invalid. Please request a new recovery link.")
                )
            }

            if (newPassword.length < 6) {
                return@withContext Result.failure(
                    Exception("New password must contain at least 6 characters.")
                )
            }

            try {
                val body = JSONObject().apply {
                    put("password", newPassword)
                }

                val request = Request.Builder()
                    .url("$SUPABASE_URL/auth/v1/user")
                    .put(
                        body.toString().toRequestBody(jsonMediaType)
                    )
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer $cleanToken")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val responseText = response.body?.string().orEmpty()

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

                    // If context is supplied, also update/save the user profile and session
                    if (context != null) {
                        try {
                            val userJson = JSONObject(responseText)
                            val userId = userJson.optString("id")
                            val userEmail = userJson.optString("email")
                            val metadata = userJson.optJSONObject("user_metadata")
                            val fullName = metadata?.optString("full_name", "") ?: ""

                            if (userId.isNotBlank()) {
                                saveUser(
                                    context = context,
                                    user = SupabaseUser(
                                        id = userId,
                                        email = userEmail,
                                        fullName = fullName
                                    ),
                                    accessToken = cleanToken
                                )
                            }
                        } catch (_: Exception) {
                            // Non-critical: password was updated successfully regardless
                        }
                    }

                    Result.success(Unit)
                }
            } catch (e: java.net.SocketTimeoutException) {
                Result.failure(
                    Exception("Supabase request timed out. Please check your connection and try again.")
                )
            } catch (e: IOException) {
                Result.failure(
                    Exception("Unable to connect to Supabase. Please check your internet connection.")
                )
            } catch (e: Exception) {
                Result.failure(
                    Exception(
                        e.message?.takeIf { it.isNotBlank() }
                            ?: "Failed to reset password. Please try again or request a new reset link."
                    )
                )
            }
        }

    /*
     * Load the signed-in user's data from both tables:
     * - profiles: account/profile details, including name
     * - health_profiles: health-specific details keyed by user_id
     */
    suspend fun getHealthProfile(
        context: Context
    ): Result<HealthProfile?> = withContext(Dispatchers.IO) {
        val user = getSavedUser(context)
            ?: return@withContext Result.success(null)
        val token = getAccessToken(context)
            ?: return@withContext Result.failure(
                Exception("Your session has expired. Please login again.")
            )

        try {
            fun getRequest(url: String) = Request.Builder()
                .url(url)
                .get()
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .build()

            val profileUrl = "$SUPABASE_URL/rest/v1/profiles" +
                "?id=eq.${user.id}&select=id,name,gender,date_of_birth,blood_group,height_cm,weight_kg,bmi"
            val healthUrl = "$SUPABASE_URL/rest/v1/health_profiles" +
                "?user_id=eq.${user.id}&select=user_id,gender,date_of_birth,blood_group,height_cm,weight_kg,bmi,allergies,medical_conditions,medications"

            val profileJson = httpClient.newCall(getRequest(profileUrl)).execute().use { response ->
                val responseText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Unable to load profile information. Server error: ${response.code}")
                    )
                }
                val rows = org.json.JSONArray(responseText)
                if (rows.length() > 0) rows.getJSONObject(0) else null
            }

            val healthJson = httpClient.newCall(getRequest(healthUrl)).execute().use { response ->
                val responseText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Unable to load health information. Server error: ${response.code}")
                    )
                }
                val rows = org.json.JSONArray(responseText)
                if (rows.length() > 0) rows.getJSONObject(0) else null
            }

            if (profileJson == null && healthJson == null) {
                return@withContext Result.success(null)
            }

            // Prefer values in health_profiles, falling back to profiles for existing data.
            val combined = JSONObject().apply {
                put("id", user.id)
                put("user_id", user.id)
                put("name", profileJson?.opt("name") ?: JSONObject.NULL)
                listOf("gender", "date_of_birth", "blood_group", "height_cm", "weight_kg", "bmi").forEach { key ->
                    val healthValue = healthJson?.opt(key)
                    val profileValue = profileJson?.opt(key)
                    put(key, when {
                        healthValue != null && healthValue != JSONObject.NULL -> healthValue
                        profileValue != null -> profileValue
                        else -> JSONObject.NULL
                    })
                }
            }
            Result.success(healthProfileFromJson(combined))
        } catch (e: IOException) {
            Result.failure(Exception("Unable to connect to Supabase. Please check your internet connection."))
        } catch (e: Exception) {
            Result.failure(Exception(e.message?.takeIf { it.isNotBlank() } ?: "Unable to load health profile."))
        }
    }

    /*
     * Save profile details to profiles and health details to health_profiles.
     * Both tables are updated atomically using PostgREST upsert (on_conflict).
     * Omitted/null fields are omitted from the payload to preserve existing values.
     */
    suspend fun saveHealthProfile(
        context: Context,
        profile: HealthProfile
    ): Result<HealthProfile> = withContext(Dispatchers.IO) {
        val user = getSavedUser(context)
        if (user == null) {
            Log.w("SupabaseClient", "saveHealthProfile: No authenticated user session found in SharedPreferences.")
            return@withContext Result.failure<HealthProfile>(
                Exception("Please login before saving personal information.")
            )
        }

        var token = getAccessToken(context)
        if (token.isNullOrBlank()) {
            Log.w("SupabaseClient", "saveHealthProfile: No access token found. Attempting session refresh...")
            val refreshResult = refreshSession(context)
            if (refreshResult.isSuccess) {
                token = refreshResult.getOrNull()
            }
            if (token.isNullOrBlank()) {
                Log.w("SupabaseClient", "saveHealthProfile aborted: Session expired and refresh failed.")
                return@withContext Result.failure<HealthProfile>(
                    Exception("Your session has expired. Please login again.")
                )
            }
        }

        try {
            val maskedId = if (user.id.length >= 8) "${user.id.take(8)}***" else "***"
            Log.d("SupabaseClient", "saveHealthProfile: Initiating upsert for user $maskedId")

            fun requestBuilder(url: String, currentToken: String) = Request.Builder()
                .url(url)
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer $currentToken")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")

            fun serverMessage(responseText: String, fallback: String): String {
                if (responseText.isBlank()) return fallback
                return try {
                    val errorJson = JSONObject(responseText)
                    val message = errorJson.optString("message", "")
                    val details = errorJson.optString("details", "")
                    val hint = errorJson.optString("hint", "")
                    listOf(message, details, hint)
                        .filter { it.isNotBlank() && it != "null" }
                        .joinToString(" | ")
                        .ifBlank { fallback }
                        .take(700)
                } catch (_: Exception) {
                    fallback
                }
            }

            // 1. Upsert account/profile details to public.profiles keyed by id
            val profileBody = JSONObject().apply {
                put("id", user.id)
                if (!profile.name.isNullOrBlank()) put("name", profile.name)
                if (!profile.gender.isNullOrBlank()) put("gender", profile.gender)
                if (!profile.dateOfBirth.isNullOrBlank()) put("date_of_birth", profile.dateOfBirth)
                if (!profile.bloodGroup.isNullOrBlank()) put("blood_group", profile.bloodGroup)
                if (profile.heightCm != null && profile.heightCm > 0.0) put("height_cm", profile.heightCm)
                if (profile.weightKg != null && profile.weightKg > 0.0) put("weight_kg", profile.weightKg)
                if (profile.bmi != null && profile.bmi > 0.0) put("bmi", profile.bmi)
            }
            var profileRequest = requestBuilder(
                "$SUPABASE_URL/rest/v1/profiles?on_conflict=id",
                token
            )
                .post(profileBody.toString().toRequestBody(jsonMediaType))
                .header("Prefer", "resolution=merge-duplicates,return=minimal")
                .build()

            var profileResponse = httpClient.newCall(profileRequest).execute()
            var profileResponseText = profileResponse.body?.string().orEmpty()
            Log.d("SupabaseClient", "profiles upsert response: HTTP ${profileResponse.code}")

            if (profileResponse.code == 401) {
                profileResponse.close()
                Log.w("SupabaseClient", "profiles upsert got HTTP 401. Attempting session refresh...")
                val refreshResult = refreshSession(context)
                if (refreshResult.isSuccess) {
                    token = refreshResult.getOrThrow()
                    profileRequest = requestBuilder(
                        "$SUPABASE_URL/rest/v1/profiles?on_conflict=id",
                        token
                    )
                        .post(profileBody.toString().toRequestBody(jsonMediaType))
                        .header("Prefer", "resolution=merge-duplicates,return=minimal")
                        .build()
                    profileResponse = httpClient.newCall(profileRequest).execute()
                    profileResponseText = profileResponse.body?.string().orEmpty()
                    Log.d("SupabaseClient", "profiles upsert retry response: HTTP ${profileResponse.code}")
                }
            }

            profileResponse.use { response ->
                if (!response.isSuccessful) {
                    val errMsg = serverMessage(profileResponseText, "Please check the profiles table permissions.")
                    Log.e("SupabaseClient", "profiles upsert failed (HTTP ${response.code}): $errMsg")
                    return@withContext Result.failure<HealthProfile>(
                        Exception("Unable to save profiles (HTTP ${response.code}): $errMsg")
                    )
                }
            }

            // 2. Atomic upsert to public.health_profiles keyed by user_id
            val healthBody = JSONObject().apply {
                put("user_id", user.id)
                if (!profile.gender.isNullOrBlank()) put("gender", profile.gender)
                if (!profile.dateOfBirth.isNullOrBlank()) put("date_of_birth", profile.dateOfBirth)
                if (!profile.bloodGroup.isNullOrBlank()) put("blood_group", profile.bloodGroup)
                if (profile.heightCm != null && profile.heightCm > 0.0) put("height_cm", profile.heightCm)
                if (profile.weightKg != null && profile.weightKg > 0.0) put("weight_kg", profile.weightKg)
                if (profile.bmi != null && profile.bmi > 0.0) put("bmi", profile.bmi)
            }
            var healthRequest = requestBuilder(
                "$SUPABASE_URL/rest/v1/health_profiles?on_conflict=user_id",
                token
            )
                .post(healthBody.toString().toRequestBody(jsonMediaType))
                .header("Prefer", "resolution=merge-duplicates,return=representation")
                .build()

            var healthResponse = httpClient.newCall(healthRequest).execute()
            var healthResponseText = healthResponse.body?.string().orEmpty()
            Log.d("SupabaseClient", "health_profiles upsert response: HTTP ${healthResponse.code}")

            if (healthResponse.code == 401) {
                healthResponse.close()
                Log.w("SupabaseClient", "health_profiles upsert got HTTP 401. Attempting session refresh...")
                val refreshResult = refreshSession(context)
                if (refreshResult.isSuccess) {
                    token = refreshResult.getOrThrow()
                    healthRequest = requestBuilder(
                        "$SUPABASE_URL/rest/v1/health_profiles?on_conflict=user_id",
                        token
                    )
                        .post(healthBody.toString().toRequestBody(jsonMediaType))
                        .header("Prefer", "resolution=merge-duplicates,return=representation")
                        .build()
                    healthResponse = httpClient.newCall(healthRequest).execute()
                    healthResponseText = healthResponse.body?.string().orEmpty()
                    Log.d("SupabaseClient", "health_profiles upsert retry response: HTTP ${healthResponse.code}")
                }
            }

            val savedProfile = healthResponse.use { response ->
                if (!response.isSuccessful) {
                    val errMsg = serverMessage(
                        healthResponseText,
                        "Check health_profiles INSERT/UPDATE permissions, RLS policies, and required columns."
                    )
                    Log.e("SupabaseClient", "health_profiles upsert failed (HTTP ${response.code}): $errMsg")
                    return@withContext Result.failure<HealthProfile>(
                        Exception("Unable to save health_profiles (HTTP ${response.code}): $errMsg")
                    )
                }

                try {
                    val array = org.json.JSONArray(healthResponseText)
                    if (array.length() > 0) {
                        val parsed = healthProfileFromJson(array.getJSONObject(0))
                        parsed.copy(
                            id = user.id,
                            name = parsed.name?.takeIf { it.isNotBlank() } ?: profile.name
                        )
                    } else {
                        profile.copy(id = user.id)
                    }
                } catch (_: Exception) {
                    profile.copy(id = user.id)
                }
            }

            Log.d("SupabaseClient", "saveHealthProfile succeeded for user $maskedId")
            Result.success(savedProfile)
        } catch (e: java.net.SocketTimeoutException) {
            Log.e("SupabaseClient", "saveHealthProfile timeout: ${e.message}")
            Result.failure<HealthProfile>(
                Exception("Supabase request timed out. Please check your connection and try again.")
            )
        } catch (e: IOException) {
            Log.e("SupabaseClient", "saveHealthProfile network error: ${e.message}")
            Result.failure<HealthProfile>(
                Exception("Unable to connect to Supabase. Please check your internet connection.")
            )
        } catch (e: Exception) {
            Log.e("SupabaseClient", "saveHealthProfile error: ${e.message}", e)
            Result.failure<HealthProfile>(
                Exception(e.message?.takeIf { it.isNotBlank() } ?: "Unable to save health profile.")
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
            id = json.optString("id", json.optString("user_id")),
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
