package com.example.ai_based_medical_chatbot

import com.example.ai_based_medical_chatbot.data.api.RetrofitClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionContinuityAndNetworkTest {

    @Test
    fun testNormalizeUrl_addsTrailingSlashAndScheme() {
        assertEquals(
            "http://192.168.1.12:5000/",
            RetrofitClient.normalizeUrl("http://192.168.1.12:5000")
        )
        assertEquals(
            "http://192.168.1.12:5000/",
            RetrofitClient.normalizeUrl("http://192.168.1.12:5000/")
        )
        assertEquals(
            "http://192.168.1.50:5000/",
            RetrofitClient.normalizeUrl("192.168.1.50:5000")
        )
        assertEquals(
            "https://my-medassist.example.com/",
            RetrofitClient.normalizeUrl("https://my-medassist.example.com")
        )
        assertEquals(
            "http://10.0.2.2:5000/",
            RetrofitClient.normalizeUrl("   ")
        )
    }

    @Test
    fun testCustomServerUrl_survivesProcessRestartSimulation() {
        // Simulates SharedPreferences backing storage across process lifecycle
        val persistentStorage = mutableMapOf<String, String>()
        val keyServerUrl = "custom_server_url"

        // Step 1: User enters custom LAN IP on the Settings / Profile screen
        val enteredIp = "192.168.0.105:5000"
        persistentStorage[keyServerUrl] = enteredIp

        // Step 2: Simulate OS Process Death (all static references customBaseUrl, appContext, cachedApiService reset)
        var simulatedCustomBaseUrl: String? = null
        var simulatedCachedService: Any? = null
        assertNull(simulatedCustomBaseUrl)
        assertNull(simulatedCachedService)

        // Step 3: Application process restart (MainActivity.onCreate triggers RetrofitClient.initialize)
        val loadedUrl = persistentStorage[keyServerUrl]?.trim()
        if (!loadedUrl.isNullOrBlank()) {
            simulatedCustomBaseUrl = loadedUrl
        }

        // Step 4: Verify URL is restored and correctly normalized
        assertEquals("192.168.0.105:5000", simulatedCustomBaseUrl)
        val resolvedBaseUrl = RetrofitClient.normalizeUrl(simulatedCustomBaseUrl ?: "http://10.0.2.2:5000/")
        assertEquals("http://192.168.0.105:5000/", resolvedBaseUrl)

        // Step 5: Verify fallback when storage is cleared
        persistentStorage.remove(keyServerUrl)
        val clearedUrl = persistentStorage[keyServerUrl]?.trim()
        val fallbackBaseUrl = RetrofitClient.normalizeUrl(clearedUrl ?: "")
        assertEquals("http://10.0.2.2:5000/", fallbackBaseUrl)
    }

    @Test
    fun testAccountAwareIsolation_userAAndUserBCannotSeeEachOtherChats() {
        // Simulating the ChatHistoryRepository account partitioning
        fun getAccountKey(userId: String?): String {
            return if (!userId.isNullOrBlank()) {
                "account_${userId.trim()}"
            } else {
                "account_guest"
            }
        }

        fun chatsKey(userId: String?): String = "saved_chats_" + getAccountKey(userId)
        fun activeSessionKey(userId: String?): String = "active_session_id_" + getAccountKey(userId)

        val storage = mutableMapOf<String, String>()

        val userAlice = "usr_alice_123"
        val userBob = "usr_bob_456"

        // Alice logs in and saves a conversation
        val aliceChatData = """[{"id":"session_alice_1","title":"Headache consultation","messages":[{"text":"I have a headache","isUser":true}]}]"""
        storage[chatsKey(userAlice)] = aliceChatData
        storage[activeSessionKey(userAlice)] = "session_alice_1"

        // Verify Bob's storage is completely isolated and empty
        val bobsChatsBefore = storage[chatsKey(userBob)]
        val bobsActiveBefore = storage[activeSessionKey(userBob)]
        assertNull("Bob must not see any chats before creating them", bobsChatsBefore)
        assertNull("Bob must not inherit Alice's active session", bobsActiveBefore)

        // Bob logs in and creates his own conversation
        val bobChatData = """[{"id":"session_bob_1","title":"Stomach ache inquiry","messages":[{"text":"My stomach hurts","isUser":true}]}]"""
        storage[chatsKey(userBob)] = bobChatData
        storage[activeSessionKey(userBob)] = "session_bob_1"

        // Alice logs back in: verify Alice's chats are preserved and separate from Bob
        val aliceChatsAfter = storage[chatsKey(userAlice)]
        val bobsChatsAfter = storage[chatsKey(userBob)]

        assertTrue(aliceChatsAfter!!.contains("session_alice_1"))
        assertFalse(aliceChatsAfter.contains("session_bob_1"))

        assertTrue(bobsChatsAfter!!.contains("session_bob_1"))
        assertFalse(bobsChatsAfter.contains("session_alice_1"))

        // Simulate logout of Alice: only active session pointer is cleared, data remains intact
        storage.remove(activeSessionKey(userAlice))
        assertNull(storage[activeSessionKey(userAlice)])
        assertTrue("Alice's chats must persist after logout for privacy without data loss", storage.containsKey(chatsKey(userAlice)))
    }

    @Test
    fun testLogoutOrdering_clearsActiveSessionForAuthenticatedUser() {
        val storage = mutableMapOf<String, String>()
        var authenticatedUserId: String? = "usr_alice_123"

        fun getAccountKey(userId: String?): String {
            return if (!userId.isNullOrBlank()) "account_${userId.trim()}" else "account_guest"
        }
        fun activeSessionKey(userId: String?): String = "active_session_id_" + getAccountKey(userId)

        // Setup active session for Alice
        storage[activeSessionKey(authenticatedUserId)] = "session_alice_active"
        assertEquals("session_alice_active", storage["active_session_id_account_usr_alice_123"])

        // Flawed order: wipe session first, then clear active session -> wrongly targets account_guest!
        val wrongTargetKey = activeSessionKey(null) // because user already wiped
        assertEquals("active_session_id_account_guest", wrongTargetKey)

        // Correct order: resolve user ID first, then clear active session, then wipe auth credentials
        val currentUserId = authenticatedUserId
        storage.remove(activeSessionKey(currentUserId))
        authenticatedUserId = null

        // Alice's active session is correctly cleared
        assertNull(storage["active_session_id_account_usr_alice_123"])
        assertNull(storage["active_session_id_account_guest"])
    }

    @Test
    fun testLegacyChatMigration_runsSafelyWithoutDuplicatingOrLosingChats() {
        val storage = mutableMapOf<String, String>()

        // Simulate existing pre-upgrade app state with unpartitioned legacy keys
        val legacyChatsJson = """[{"id":"legacy_session_99","title":"Legacy conversation","messages":[{"text":"Cold symptoms","isUser":true}]}]"""
        storage["saved_chats"] = legacyChatsJson
        storage["active_session_id"] = "legacy_session_99"

        fun migrateLegacyIfNeeded(targetAccountKey: String) {
            val legacyChats = storage["saved_chats"]
            val targetKey = "saved_chats_$targetAccountKey"
            if (!legacyChats.isNullOrBlank() && !storage.containsKey(targetKey)) {
                val legacyActive = storage["active_session_id"]
                storage[targetKey] = legacyChats
                if (!legacyActive.isNullOrBlank()) {
                    storage["active_session_id_$targetAccountKey"] = legacyActive
                }
                storage.remove("saved_chats")
                storage.remove("active_session_id")
            }
        }

        // Migrate to Alice
        migrateLegacyIfNeeded("account_alice")

        // Verify target account received the exact chats
        assertEquals(legacyChatsJson, storage["saved_chats_account_alice"])
        assertEquals("legacy_session_99", storage["active_session_id_account_alice"])

        // Verify legacy keys were cleaned up
        assertFalse(storage.containsKey("saved_chats"))
        assertFalse(storage.containsKey("active_session_id"))

        // Running migration again must be a no-op and never duplicate or overwrite
        migrateLegacyIfNeeded("account_alice")
        assertEquals(legacyChatsJson, storage["saved_chats_account_alice"])
    }

    @Test
    fun testConversationDeletion_advancesToNextRemainingOrEmptyState() {
        data class SavedChatStub(val id: String, val title: String)

        var chatList = mutableListOf(
            SavedChatStub("chat_1", "First conversation"),
            SavedChatStub("chat_2", "Second conversation"),
            SavedChatStub("chat_3", "Third conversation")
        )
        var activeSessionId: String? = "chat_1"

        fun deleteChat(idToDelete: String) {
            chatList = chatList.filter { it.id != idToDelete }.toMutableList()
            if (activeSessionId == idToDelete) {
                activeSessionId = chatList.firstOrNull()?.id
            }
        }

        // Case 1: Delete active conversation "chat_1" -> advances to next remaining ("chat_2")
        deleteChat("chat_1")
        assertEquals("chat_2", activeSessionId)
        assertEquals(2, chatList.size)
        assertEquals("chat_2", chatList[0].id)
        assertEquals("chat_3", chatList[1].id)

        // Case 2: Delete non-active conversation "chat_3" -> active remains "chat_2"
        deleteChat("chat_3")
        assertEquals("chat_2", activeSessionId)
        assertEquals(1, chatList.size)

        // Case 3: Delete last remaining conversation "chat_2" -> active becomes null, empty state
        deleteChat("chat_2")
        assertNull("Active session must become null when all conversations are deleted", activeSessionId)
        assertTrue("Chat list must be empty", chatList.isEmpty())
    }

    @Test
    fun testSessionIdGeneration_startsCorrectPrefixAndUnique() {
        val id1 = "android_" + System.currentTimeMillis()
        Thread.sleep(2)
        val id2 = "android_" + System.currentTimeMillis()

        assertTrue(id1.startsWith("android_"))
        assertTrue(id2.startsWith("android_"))
        assertNotEquals(id1, id2)
    }

    @Test
    fun testDuplicateMessagePrevention_inFlightGuard() {
        var isTyping = false
        fun canSend(text: String, hasImages: Boolean): Boolean {
            val clean = text.trim()
            if (clean.isEmpty() && !hasImages) return false
            if (isTyping) return false
            return true
        }

        assertTrue(canSend("I have a headache", false))
        assertTrue(canSend("", true))
        assertFalse(canSend("   ", false))

        // When a request is in flight, duplicate send is blocked
        isTyping = true
        assertFalse(canSend("I have a headache", false))
        assertFalse(canSend("Another message", true))

        // Once finished, sending is unlocked again
        isTyping = false
        assertTrue(canSend("Next follow-up question", false))
    }

    @Test
    fun testSessionContinuity_resolutionLogic() {
        // Simulating the resolution order:
        // 1. explicit initialSessionId (e.g. from history screen or deep link)
        // 2. stored active session if it exists in saved list
        // 3. most recent saved chat
        // 4. fresh session ID
        val savedSessionIds = listOf("android_1001", "android_1002")

        fun resolveSession(
            initialId: String?,
            storedActiveId: String?,
            savedList: List<String>
        ): String {
            if (!initialId.isNullOrBlank()) return initialId.trim()
            if (!storedActiveId.isNullOrBlank() && savedList.contains(storedActiveId)) {
                return storedActiveId
            }
            if (savedList.isNotEmpty()) {
                return savedList.first()
            }
            return "android_fresh"
        }

        // Resumes explicit session
        assertEquals("android_9999", resolveSession("android_9999", "android_1001", savedSessionIds))

        // Resumes active session when returning from another screen
        assertEquals("android_1002", resolveSession(null, "android_1002", savedSessionIds))

        // Falls back to latest chat if active session was deleted
        assertEquals("android_1001", resolveSession(null, "android_deleted", savedSessionIds))

        // Generates fresh ID when no saved chats exist
        assertEquals("android_fresh", resolveSession(null, null, emptyList()))
    }

    @Test
    fun testRetryMessage_findsLastUserMessageAndRemovesError() {
        data class TestMessage(val text: String, val isUser: Boolean, val isError: Boolean)

        val chat = mutableListOf(
            TestMessage("I have severe cough for 3 days", isUser = true, isError = false),
            TestMessage("Connection timed out", isUser = false, isError = true)
        )

        val errorIndex = chat.indexOfLast { !it.isUser && it.isError }
        assertEquals(1, errorIndex)

        // Remove the error message on retry
        chat.removeAt(errorIndex)
        assertEquals(1, chat.size)

        // The message to retry is the last user message
        val toRetry = chat.lastOrNull { it.isUser }
        assertEquals("I have severe cough for 3 days", toRetry?.text)
    }
}
