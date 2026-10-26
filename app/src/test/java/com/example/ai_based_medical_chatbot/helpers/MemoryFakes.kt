package com.example.ai_based_medical_chatbot.helpers

import android.content.SharedPreferences
import com.example.ai_based_medical_chatbot.data.api.AddMemoryRequest
import com.example.ai_based_medical_chatbot.data.api.ChatHistoryResponse
import com.example.ai_based_medical_chatbot.data.api.ConversationsResponse
import com.example.ai_based_medical_chatbot.data.api.MedicalApiService
import com.example.ai_based_medical_chatbot.data.api.MemoryConsentRequest
import com.example.ai_based_medical_chatbot.data.api.MemoryConsentResponse
import com.example.ai_based_medical_chatbot.data.api.PredictionRequest
import com.example.ai_based_medical_chatbot.data.api.PredictionResponse
import com.example.ai_based_medical_chatbot.data.api.UpdateMemoryRequest
import com.example.ai_based_medical_chatbot.data.api.UserMemoriesResponse
import com.example.ai_based_medical_chatbot.data.api.UserMemoryItem
import retrofit2.Response
import java.io.IOException

class FakeSharedPreferences : SharedPreferences {
    val map = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = map
    override fun getString(key: String?, defValue: String?): String? = (map[key] as? String) ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
    override fun getInt(key: String?, defValue: Int): Int = (map[key] as? Int) ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = (map[key] as? Long) ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = (map[key] as? Float) ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
    override fun contains(key: String?): Boolean = map.containsKey(key)
    override fun edit(): SharedPreferences.Editor = Editor(this)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    class Editor(private val prefs: FakeSharedPreferences) : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()
        private val removed = mutableSetOf<String>()
        private var clear = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = this
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) removed.add(key)
            return this
        }
        override fun clear(): SharedPreferences.Editor {
            clear = true
            return this
        }
        override fun commit(): Boolean {
            apply()
            return true
        }
        override fun apply() {
            if (clear) prefs.map.clear()
            removed.forEach { prefs.map.remove(it) }
            prefs.map.putAll(temp)
        }
    }
}

class FakeMedicalApiService : MedicalApiService {
    val remoteMemories = mutableListOf<UserMemoryItem>()
    var cloudConsent = false
    var shouldFailWithNetworkError = false
    val addedKeys = mutableListOf<String>()
    val deletedIds = mutableListOf<String>()
    var clearedAll = false

    override suspend fun predict(request: PredictionRequest): Response<PredictionResponse> =
        Response.success(PredictionResponse(answer = "OK"))

    override suspend fun getChatHistory(sessionId: String): Response<ChatHistoryResponse> =
        Response.success(ChatHistoryResponse(status = "success"))

    override suspend fun deleteChatHistory(sessionId: String): Response<Unit> =
        Response.success(Unit)

    override suspend fun getUserMemories(): Response<UserMemoriesResponse> {
        if (shouldFailWithNetworkError) throw IOException("Simulated network failure")
        return Response.success(
            UserMemoriesResponse(
                status = "success",
                count = remoteMemories.size,
                memories = remoteMemories.toList()
            )
        )
    }

    override suspend fun addUserMemory(request: AddMemoryRequest): Response<Unit> {
        if (shouldFailWithNetworkError) throw IOException("Simulated network failure")
        request.key?.let { addedKeys.add(it) }
        remoteMemories.add(
            UserMemoryItem(
                id = "remote_${request.key}",
                userId = "test_user",
                memoryKey = request.key ?: "pref",
                content = request.content,
                category = request.category
            )
        )
        return Response.success(Unit)
    }

    override suspend fun updateUserMemory(id: String, request: UpdateMemoryRequest): Response<Unit> {
        if (shouldFailWithNetworkError) throw IOException("Simulated network failure")
        val idx = remoteMemories.indexOfFirst { it.id == id }
        if (idx >= 0) {
            remoteMemories[idx] = remoteMemories[idx].copy(content = request.content)
        }
        return Response.success(Unit)
    }

    override suspend fun deleteUserMemory(id: String): Response<Unit> {
        if (shouldFailWithNetworkError) throw IOException("Simulated network failure")
        deletedIds.add(id)
        remoteMemories.removeAll { it.id == id }
        return Response.success(Unit)
    }

    override suspend fun clearAllUserMemories(): Response<Unit> {
        if (shouldFailWithNetworkError) throw IOException("Simulated network failure")
        clearedAll = true
        remoteMemories.clear()
        return Response.success(Unit)
    }

    override suspend fun getMemoryConsent(): Response<MemoryConsentResponse> {
        if (shouldFailWithNetworkError) throw IOException("Simulated network failure")
        return Response.success(MemoryConsentResponse(status = "success", consent = cloudConsent))
    }

    override suspend fun setMemoryConsent(request: MemoryConsentRequest): Response<MemoryConsentResponse> {
        if (shouldFailWithNetworkError) throw IOException("Simulated network failure")
        cloudConsent = request.consent
        return Response.success(MemoryConsentResponse(status = "success", consent = cloudConsent))
    }

    override suspend fun getConversations(): Response<ConversationsResponse> =
        Response.success(ConversationsResponse(status = "success"))
}
