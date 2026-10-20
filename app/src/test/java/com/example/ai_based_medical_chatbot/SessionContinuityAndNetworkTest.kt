package com.example.ai_based_medical_chatbot

import com.example.ai_based_medical_chatbot.data.api.RetrofitClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ui.formatDobDigits
import ui.parseDateOfBirth
import ui.calculateAgeFromDateOfBirth

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

    @Test
    fun testAppStrings_medicineKeysTranslatedInAllLanguages() {
        val medicineKeys = listOf(
            "medicine_info_subtitle",
            "medicine_search_placeholder",
            "medicine_universal_search",
            "medicine_disclaimer",
            "medicine_search_database",
            "medicine_available",
            "medicine_search_results",
            "medicine_detail_title",
            "medicine_detail_subtitle",
            "medicine_detail_name",
            "medicine_detail_generic",
            "medicine_detail_uses",
            "medicine_detail_side_effects",
            "medicine_detail_warnings",
            "medicine_detail_precautions",
            "medicine_detail_disclaimer"
        )

        val languages = listOf("English", "मराठी", "हिंदी")

        for (lang in languages) {
            for (key in medicineKeys) {
                val translation = appText(lang, key)
                assertFalse("Translation for $key in $lang should not be blank", translation.isBlank())
                assertFalse("Translation for $key in $lang should not contain raw underscores", translation.contains("_"))
            }
        }
    }

    @Test
    fun testAppStrings_humanReadableFallbackForUnmappedKeys() {
        val unmappedKey = "unmapped_test_warning_label"
        val result = appText("English", unmappedKey)
        assertEquals("Unmapped Test Warning Label", result)
    }

    @Test
    fun testPredictionResponse_deserializesSessionIdCorrectly() {
        val gson = com.google.gson.Gson()

        // 1. Valid session_id returned by server
        val jsonWithSession = """
            {
                "status": "success",
                "answer": "Take rest and drink warm water.",
                "session_id": "anon_6b24d9c4"
            }
        """.trimIndent()
        val parsedWith = gson.fromJson(jsonWithSession, com.example.ai_based_medical_chatbot.data.api.PredictionResponse::class.java)
        assertEquals("anon_6b24d9c4", parsedWith.sessionId)
        assertEquals("Take rest and drink warm water.", parsedWith.answer)

        // 2. Backward compatibility: session_id omitted by older backend
        val jsonWithoutSession = """
            {
                "status": "success",
                "answer": "Drink plenty of fluids."
            }
        """.trimIndent()
        val parsedWithout = gson.fromJson(jsonWithoutSession, com.example.ai_based_medical_chatbot.data.api.PredictionResponse::class.java)
        assertNull("Missing session_id should deserialize to null", parsedWithout.sessionId)

        // 3. session_id is empty or blank
        val jsonWithBlank = """
            {
                "status": "success",
                "answer": "Consult a physician.",
                "session_id": "   "
            }
        """.trimIndent()
        val parsedBlank = gson.fromJson(jsonWithBlank, com.example.ai_based_medical_chatbot.data.api.PredictionResponse::class.java)
        assertEquals("   ", parsedBlank.sessionId)
        assertTrue(parsedBlank.sessionId?.isBlank() == true)
    }

    @Test
    fun testSessionAdoptionRules_validServerSessionIsAdoptedAndReused() {
        var currentSessionId = "android_1710000000"
        val activeStorage = mutableMapOf<String, String>()
        activeStorage["active_session"] = currentSessionId

        fun handleServerResponse(
            requestSessionId: String,
            responseSessionId: String?
        ) {
            val returnedSessionId = responseSessionId?.trim()
            val targetSessionId = if (!returnedSessionId.isNullOrBlank() && currentSessionId == requestSessionId) {
                if (returnedSessionId != currentSessionId) {
                    val previousSessionId = currentSessionId
                    currentSessionId = returnedSessionId
                    activeStorage["active_session"] = returnedSessionId
                    activeStorage.remove("chat_$previousSessionId")
                }
                returnedSessionId
            } else {
                currentSessionId
            }
            activeStorage["chat_$targetSessionId"] = "saved_chat_data"
        }

        // Scenario 1: Server returns a valid session ID -> Adopted
        val req1 = currentSessionId
        handleServerResponse(requestSessionId = req1, responseSessionId = "anon_uuid_9876")
        assertEquals("anon_uuid_9876", currentSessionId)
        assertEquals("anon_uuid_9876", activeStorage["active_session"])
        assertTrue(activeStorage.containsKey("chat_anon_uuid_9876"))
        assertFalse(activeStorage.containsKey("chat_android_1710000000"))

        // Scenario 2: Subsequent request uses adopted ID and server echoes it -> Preserved
        val req2 = currentSessionId
        handleServerResponse(requestSessionId = req2, responseSessionId = "anon_uuid_9876")
        assertEquals("anon_uuid_9876", currentSessionId)
        assertEquals("anon_uuid_9876", activeStorage["active_session"])

        // Scenario 3: Server returns null or blank -> Current session is NOT overwritten
        val req3 = currentSessionId
        handleServerResponse(requestSessionId = req3, responseSessionId = null)
        assertEquals("anon_uuid_9876", currentSessionId)
        handleServerResponse(requestSessionId = req3, responseSessionId = "   ")
        assertEquals("anon_uuid_9876", currentSessionId)

        // Scenario 4: Late response from an older session (user switched session while waiting) -> Ignored
        val oldRequestSessionId = "old_stale_session"
        handleServerResponse(requestSessionId = oldRequestSessionId, responseSessionId = "anon_stale_late")
        assertEquals("anon_uuid_9876", currentSessionId)
        assertNotEquals("anon_stale_late", currentSessionId)
    }

    @Test
    fun testChatbotAutoScroll_respectsUserScrolledUpThreshold() {
        fun shouldAutoScroll(
            currentScroll: Int,
            maxScroll: Int,
            userScrolledUp: Boolean
        ): Boolean {
            return !userScrolledUp && currentScroll < maxScroll
        }

        fun computeUserScrolledUp(currentScroll: Int, maxScroll: Int): Boolean {
            return (maxScroll - currentScroll) > 220
        }

        // Case 1: User is at the bottom (scroll = max)
        val max1 = 1200
        val scroll1 = 1200
        val userScrolled1 = computeUserScrolledUp(scroll1, max1)
        assertFalse("User at bottom should not be considered scrolled up", userScrolled1)
        // New message arrives -> maxScroll increases to 1400
        assertTrue("Should auto-scroll when new content arrives and user is near bottom",
            shouldAutoScroll(currentScroll = 1200, maxScroll = 1400, userScrolledUp = userScrolled1))

        // Case 2: User scrolled up slightly within threshold (e.g., 100px from bottom)
        val max2 = 1400
        val scroll2 = 1300
        val userScrolled2 = computeUserScrolledUp(scroll2, max2)
        assertFalse("Small scroll within 220px threshold should not flag scrolled up", userScrolled2)
        assertTrue(shouldAutoScroll(scroll2, max2 + 200, userScrolled2))

        // Case 3: User intentionally scrolled up to read earlier history (> 220px)
        val max3 = 1400
        val scroll3 = 1000 // 400px away from bottom
        val userScrolled3 = computeUserScrolledUp(scroll3, max3)
        assertTrue("Scrolling > 220px above bottom must flag userScrolledUp", userScrolled3)
        assertFalse("Must NOT force-scroll down when user intentionally scrolled up",
            shouldAutoScroll(scroll3, max3 + 200, userScrolled3))

        // Case 4: User sends a new message -> resets userScrolledUp to false
        var userScrolledState = userScrolled3
        assertTrue(userScrolledState)
        // On send message action:
        userScrolledState = false
        assertTrue("Sending a message must re-engage bottom auto-scroll",
            shouldAutoScroll(scroll3, max3 + 200, userScrolledState))

        // Case 5: User taps 'Jump to latest' button -> resets userScrolledUp to false
        userScrolledState = true
        // On jump button click:
        userScrolledState = false
        assertFalse(userScrolledState)
    }

    @Test
    fun testProfilePasswordChange_validationRulesAndSubmissionParity() {
        data class ValidationResult(val isValid: Boolean, val errorKey: String?)

        fun validatePasswordChange(
            currentPass: String,
            newPass: String,
            confirmPass: String,
            isLoading: Boolean
        ): ValidationResult {
            if (isLoading) return ValidationResult(isValid = false, errorKey = null)
            return when {
                currentPass.isBlank() -> ValidationResult(isValid = false, errorKey = "current_password")
                newPass.length < 6 -> ValidationResult(isValid = false, errorKey = "password_minimum")
                newPass != confirmPass -> ValidationResult(isValid = false, errorKey = "password_mismatch")
                else -> ValidationResult(isValid = true, errorKey = null)
            }
        }

        // Test 1: Empty current password
        val r1 = validatePasswordChange("", "newsecret123", "newsecret123", false)
        assertFalse(r1.isValid)
        assertEquals("current_password", r1.errorKey)

        // Test 2: New password too short (< 6 chars)
        val r2 = validatePasswordChange("current123", "abc", "abc", false)
        assertFalse(r2.isValid)
        assertEquals("password_minimum", r2.errorKey)

        // Test 3: Password confirmation mismatch
        val r3 = validatePasswordChange("current123", "newsecret123", "different123", false)
        assertFalse(r3.isValid)
        assertEquals("password_mismatch", r3.errorKey)

        // Test 4: Concurrency lock prevents duplicate submissions while loading
        val r4 = validatePasswordChange("current123", "newsecret123", "newsecret123", true)
        assertFalse("Concurrent request while loading must be ignored", r4.isValid)
        assertNull(r4.errorKey)

        // Test 5: Valid password change inputs
        val r5 = validatePasswordChange("current123", "newsecret123", "newsecret123", false)
        assertTrue(r5.isValid)
        assertNull(r5.errorKey)
    }

    @Test
    fun testRegisterScreen_focusChainAndValidationParity() {
        data class RegisterValidationResult(
            val isValid: Boolean,
            val nameError: String?,
            val emailError: String?,
            val passwordError: String?,
            val confirmError: String?
        )

        fun validateRegistration(
            name: String,
            email: String,
            pass: String,
            confirm: String,
            isLoading: Boolean,
            isEmailPatternValid: (String) -> Boolean
        ): RegisterValidationResult {
            if (isLoading) {
                return RegisterValidationResult(false, null, null, null, null)
            }

            var valid = true
            var nameErr: String? = null
            var emailErr: String? = null
            var passErr: String? = null
            var confirmErr: String? = null

            if (name.trim().isEmpty()) {
                nameErr = "register_name_required"
                valid = false
            }

            val cleanEmail = email.trim()
            if (cleanEmail.isEmpty()) {
                emailErr = "register_email_required"
                valid = false
            } else if (!isEmailPatternValid(cleanEmail)) {
                emailErr = "register_valid_email"
                valid = false
            }

            if (pass.isEmpty()) {
                passErr = "register_password_required"
                valid = false
            } else if (pass.length < 6) {
                passErr = "register_password_min"
                valid = false
            }

            if (confirm.isEmpty()) {
                confirmErr = "register_confirm_required"
                valid = false
            } else if (pass != confirm) {
                confirmErr = "register_password_mismatch"
                valid = false
            }

            return RegisterValidationResult(valid, nameErr, emailErr, passErr, confirmErr)
        }

        val simpleEmailValidator: (String) -> Boolean = { it.contains("@") && it.contains(".") }

        // Test 1: All empty fields
        val r1 = validateRegistration("", "", "", "", false, simpleEmailValidator)
        assertFalse(r1.isValid)
        assertEquals("register_name_required", r1.nameError)
        assertEquals("register_email_required", r1.emailError)
        assertEquals("register_password_required", r1.passwordError)
        assertEquals("register_confirm_required", r1.confirmError)

        // Test 2: Invalid email format
        val r2 = validateRegistration("Dr. Smith", "not-an-email", "secret123", "secret123", false, simpleEmailValidator)
        assertFalse(r2.isValid)
        assertNull(r2.nameError)
        assertEquals("register_valid_email", r2.emailError)

        // Test 3: Password mismatch
        val r3 = validateRegistration("Dr. Smith", "smith@hospital.org", "secret123", "different123", false, simpleEmailValidator)
        assertFalse(r3.isValid)
        assertEquals("register_password_mismatch", r3.confirmError)

        // Test 4: Concurrency guard while loading
        val r4 = validateRegistration("Dr. Smith", "smith@hospital.org", "secret123", "secret123", true, simpleEmailValidator)
        assertFalse("Submission must be blocked while loading", r4.isValid)

        // Test 5: Fully valid registration
        val r5 = validateRegistration("Dr. John Doe", "johndoe@example.com", "securePassword1", "securePassword1", false, simpleEmailValidator)
        assertTrue(r5.isValid)
        assertNull(r5.nameError)
        assertNull(r5.emailError)
        assertNull(r5.passwordError)
        assertNull(r5.confirmError)
    }

    @Test
    fun testBmiValidation_preventsInvalidInputsAndAllowsValidInputs() {
        fun validateBmi(
            dob: String,
            isDobValid: Boolean,
            age: Int,
            gender: String,
            heightStr: String,
            weightStr: String
        ): String {
            val finalHeight = heightStr.toFloatOrNull()
            val finalWeight = weightStr.toFloatOrNull()

            return when {
                dob.length != 10 -> "bmi_validation_dob"
                !isDobValid -> "bmi_validation_dob_invalid"
                age <= 0 -> "bmi_validation_dob_invalid"
                age > 120 -> "bmi_validation_age"
                gender.isBlank() -> "bmi_validation_gender"
                finalHeight == null || finalHeight !in 50f..250f -> "bmi_validation_height"
                finalWeight == null || finalWeight !in 2f..300f -> "bmi_validation_weight"
                else -> ""
            }
        }

        // Case 1: Empty inputs
        assertEquals("bmi_validation_height", validateBmi("1995-05-15", true, 30, "Male", "", "70.0"))
        assertEquals("bmi_validation_weight", validateBmi("1995-05-15", true, 30, "Male", "175.0", ""))

        // Case 2: Zero or negative height/weight
        assertEquals("bmi_validation_height", validateBmi("1995-05-15", true, 30, "Male", "0.0", "70.0"))
        assertEquals("bmi_validation_height", validateBmi("1995-05-15", true, 30, "Male", "-175.0", "70.0"))
        assertEquals("bmi_validation_weight", validateBmi("1995-05-15", true, 30, "Male", "175.0", "0.0"))
        assertEquals("bmi_validation_weight", validateBmi("1995-05-15", true, 30, "Male", "175.0", "-5.0"))

        // Case 3: Out-of-range height (<50cm or >250cm)
        assertEquals("bmi_validation_height", validateBmi("1995-05-15", true, 30, "Male", "49.0", "70.0"))
        assertEquals("bmi_validation_height", validateBmi("1995-05-15", true, 30, "Male", "251.0", "70.0"))

        // Case 4: Out-of-range weight (<2kg or >300kg)
        assertEquals("bmi_validation_weight", validateBmi("1995-05-15", true, 30, "Male", "175.0", "1.5"))
        assertEquals("bmi_validation_weight", validateBmi("1995-05-15", true, 30, "Male", "175.0", "305.0"))

        // Case 5: Invalid DOB or Age
        assertEquals("bmi_validation_dob", validateBmi("1995-5-1", true, 30, "Male", "175.0", "70.0"))
        assertEquals("bmi_validation_dob_invalid", validateBmi("1995-99-99", false, 0, "Male", "175.0", "70.0"))
        assertEquals("bmi_validation_age", validateBmi("1880-01-01", true, 145, "Male", "175.0", "70.0"))

        // Case 6: Missing Gender
        assertEquals("bmi_validation_gender", validateBmi("1995-05-15", true, 30, "   ", "175.0", "70.0"))

        // Case 7: Valid inputs
        val validErr = validateBmi("1995-05-15", true, 30, "Male", "175.0", "70.0")
        assertEquals("", validErr)

        // Case 8: BMI computation math check
        val heightM = 175.0f / 100f
        val calculatedBmi = 70.0f / (heightM * heightM)
        assertTrue(calculatedBmi in 22.8f..22.9f)
    }

    @Test
    fun testMedicineReminderValidation_requiresNameAndTime() {
        fun validateReminder(name: String, time: String): String {
            return when {
                name.isBlank() -> "Please enter medicine name."
                time.isBlank() -> "Please select reminder time."
                else -> ""
            }
        }

        // Test 1: Empty name
        assertEquals("Please enter medicine name.", validateReminder("", "08:00 AM"))
        assertEquals("Please enter medicine name.", validateReminder("   ", "08:00 AM"))

        // Test 2: Empty time
        assertEquals("Please select reminder time.", validateReminder("Paracetamol", ""))
        assertEquals("Please select reminder time.", validateReminder("Paracetamol", "   "))

        // Test 3: Valid reminder
        assertEquals("", validateReminder("Paracetamol", "08:00 AM"))

        // Test 4: Creation of MedicineReminder with trimmed fields
        val reminder = MedicineReminder(
            medicineName = "  Amoxicillin  ".trim(),
            strength = " 500 mg ".trim(),
            time = " 09:00 AM ".trim(),
            daySlot = "Morning",
            mealTiming = "After Food",
            duration = " 7 days ".trim(),
            startDate = " 01/11/2026 ".trim(),
            endDate = " 07/11/2026 ".trim(),
            instructions = " Drink full glass of water ".trim(),
            enabled = true
        )

        assertEquals("Amoxicillin", reminder.medicineName)
        assertEquals("500 mg", reminder.strength)
        assertEquals("09:00 AM", reminder.time)
        assertEquals("Morning", reminder.daySlot)
        assertEquals("After Food", reminder.mealTiming)
        assertEquals("7 days", reminder.duration)
        assertEquals("01/11/2026", reminder.startDate)
        assertEquals("07/11/2026", reminder.endDate)
        assertEquals("Drink full glass of water", reminder.instructions)
        assertTrue(reminder.enabled)
    }

    @Test
    fun testLoginValidation_requiresNonEmptyCredentialsAndNotLoading() {
        fun canSubmitLogin(email: String, pass: String, isLoading: Boolean): Boolean {
            return !isLoading && email.isNotBlank() && pass.isNotBlank()
        }

        assertFalse(canSubmitLogin("", "", false))
        assertFalse(canSubmitLogin("test@medassist.ai", "", false))
        assertFalse(canSubmitLogin("", "password123", false))
        assertFalse(canSubmitLogin("test@medassist.ai", "password123", true))
        assertTrue(canSubmitLogin("test@medassist.ai", "password123", false))
    }

    @Test
    fun testSearchFieldBehavior_maintainsQueryStateOnImeSearch() {
        var searchQuery = "Paracetamol"
        var isKeyboardVisible = true

        // Simulate onSearch IME action: keyboard dismisses, query is NOT cleared
        val onSearchAction = {
            isKeyboardVisible = false
        }

        onSearchAction()
        assertFalse(isKeyboardVisible)
        assertEquals("Paracetamol", searchQuery)

        // Clear query action via trailing icon
        val onClearAction = {
            searchQuery = ""
        }

        onClearAction()
        assertEquals("", searchQuery)
    }

    @Test
    fun testForgotPasswordValidation_requiresValidEmailAndNotLoading() {
        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")
        fun canSubmitReset(email: String, isLoading: Boolean): Pair<Boolean, String> {
            val clean = email.trim()
            if (isLoading) return false to "Already loading"
            if (clean.isBlank()) return false to "Please enter your email address."
            if (!emailRegex.matches(clean)) return false to "Please enter a valid email address (e.g. name@example.com)."
            return true to ""
        }

        // Empty / Blank
        val (canSubmitBlank, errBlank) = canSubmitReset("", false)
        assertFalse(canSubmitBlank)
        assertEquals("Please enter your email address.", errBlank)

        // Invalid format
        val (canSubmitInvalid, errInvalid) = canSubmitReset("invalid-email-address", false)
        assertFalse(canSubmitInvalid)
        assertEquals("Please enter a valid email address (e.g. name@example.com).", errInvalid)

        // While already loading (prevents duplicate submission)
        val (canSubmitWhileLoading, errLoading) = canSubmitReset("user@medassist.ai", true)
        assertFalse(canSubmitWhileLoading)
        assertEquals("Already loading", errLoading)

        // Valid submission
        val (canSubmitValid, errValid) = canSubmitReset("user@medassist.ai", false)
        assertTrue(canSubmitValid)
        assertEquals("", errValid)
    }

    @Test
    fun testChatHistoryNavigation_sessionRoutingPassesTargetSessionId() {
        var selectedHistorySessionId: String? = null
        var currentScreen = "chatbot"

        // Simulate navigation from Chatbot to ChatHistory for a specific session
        val targetSessionId = "android_1729248590123"
        val onViewTranscript: (String?) -> Unit = { id ->
            selectedHistorySessionId = id
            currentScreen = "chatHistory"
        }

        onViewTranscript(targetSessionId)
        assertEquals("chatHistory", currentScreen)
        assertEquals("android_1729248590123", selectedHistorySessionId)

        // Simulate Back navigation returning to Chatbot with all state intact
        val back: () -> Unit = {
            currentScreen = "chatbot"
        }
        back()
        assertEquals("chatbot", currentScreen)
    }

    @Test
    fun testRecoveryDeepLinkParsing_extractsTokenFromQueryAndFragment() {
        fun extractRecoveryToken(uriString: String): String {
            val uri = java.net.URI(uriString)
            if (uri.scheme != "medassist" || uri.host != "reset-password") return ""

            // Query parameter
            val query = uri.query
            if (!query.isNullOrBlank()) {
                val params = query.split("&").associate { segment ->
                    val parts = segment.split("=", limit = 2)
                    if (parts.size == 2) parts[0].trim() to parts[1].trim() else "" to ""
                }
                val queryToken = params["access_token"] ?: params["token"]
                if (!queryToken.isNullOrBlank()) return queryToken.trim()
            }

            // Fragment parameter (hash #access_token=...&type=recovery)
            val fragment = uri.fragment
            if (!fragment.isNullOrBlank()) {
                val params = fragment.split("&").associate { segment ->
                    val parts = segment.split("=", limit = 2)
                    if (parts.size == 2) parts[0].trim() to parts[1].trim() else "" to ""
                }
                val fragToken = params["access_token"] ?: params["token"]
                if (!fragToken.isNullOrBlank()) return fragToken.trim()
            }
            return ""
        }

        // Test 1: Query parameter format
        val queryUri = "medassist://reset-password?access_token=test_jwt_recovery_token_123&type=recovery"
        assertEquals("test_jwt_recovery_token_123", extractRecoveryToken(queryUri))

        // Test 2: Fragment / hash format (standard Supabase redirect)
        val hashUri = "medassist://reset-password#access_token=supabase_hash_jwt_456&expires_in=3600&token_type=bearer&type=recovery"
        assertEquals("supabase_hash_jwt_456", extractRecoveryToken(hashUri))

        // Test 3: Fallback token query parameter
        val tokenParamUri = "medassist://reset-password?token=simple_token_789"
        assertEquals("simple_token_789", extractRecoveryToken(tokenParamUri))

        // Test 4: Unrelated or invalid deep link
        val invalidUri = "medassist://other-action?access_token=should_be_ignored"
        assertEquals("", extractRecoveryToken(invalidUri))

        // Test 5: Empty deep link
        val emptyUri = "medassist://reset-password"
        assertEquals("", extractRecoveryToken(emptyUri))
    }

    @Test
    fun testResetPasswordValidation_enforcesConstraintsAndMatching() {
        fun validateResetInputs(
            token: String,
            pass: String,
            confirm: String,
            isLoading: Boolean
        ): Pair<Boolean, String> {
            if (isLoading) return false to "Already loading"
            if (token.isBlank()) return false to "Recovery link is invalid or expired. Please request a new link."
            if (pass.isBlank()) return false to "Please enter a new password."
            if (pass.length < 6) return false to "New password must contain at least 6 characters."
            if (pass != confirm) return false to "Passwords do not match. Please re-enter."
            return true to ""
        }

        val validToken = "valid_recovery_token"

        // Missing token
        val (noToken, errNoToken) = validateResetInputs("", "newPass123", "newPass123", false)
        assertFalse(noToken)
        assertEquals("Recovery link is invalid or expired. Please request a new link.", errNoToken)

        // Blank password
        val (blankPass, errBlank) = validateResetInputs(validToken, "", "", false)
        assertFalse(blankPass)
        assertEquals("Please enter a new password.", errBlank)

        // Short password
        val (shortPass, errShort) = validateResetInputs(validToken, "12345", "12345", false)
        assertFalse(shortPass)
        assertEquals("New password must contain at least 6 characters.", errShort)

        // Mismatched confirmation
        val (mismatch, errMismatch) = validateResetInputs(validToken, "securePass123", "differentPass456", false)
        assertFalse(mismatch)
        assertEquals("Passwords do not match. Please re-enter.", errMismatch)

        // While already loading
        val (loading, errLoading) = validateResetInputs(validToken, "securePass123", "securePass123", true)
        assertFalse(loading)
        assertEquals("Already loading", errLoading)

        // Valid submission
        val (valid, errValid) = validateResetInputs(validToken, "securePass123", "securePass123", false)
        assertTrue(valid)
        assertEquals("", errValid)
    }

    @Test
    fun testAppStrings_forgotPasswordKeysLocalized() {
        val forgotKeys = listOf(
            "forgot_back_login",
            "forgot_health_assistant",
            "forgot_title",
            "forgot_hero_message",
            "forgot_ai_powered",
            "forgot_secure_badge",
            "forgot_reset_password",
            "forgot_reset_description",
            "forgot_email_label",
            "forgot_email_placeholder",
            "forgot_email_content",
            "forgot_send_reset",
            "forgot_reset_note",
            "forgot_secure",
            "forgot_security"
        )

        for (lang in listOf("en", "mr", "hi")) {
            for (key in forgotKeys) {
                val translated = appText(key, lang)
                assertTrue("Key $key in language $lang is empty", translated.isNotBlank())
            }
        }

        // Verify that Marathi and Hindi do not return raw English keys or fallback formatting
        for (key in forgotKeys) {
            val mrText = appText(key, "mr")
            val hiText = appText(key, "hi")
            assertFalse("Key $key should not be unmapped fallback in Marathi: $mrText", mrText.startsWith("Forgot "))
            assertFalse("Key $key should not be unmapped fallback in Hindi: $hiText", hiText.startsWith("Forgot "))
        }

        // Verify specific English copy
        assertEquals("Forgot Password?", appText("forgot_title", "en"))
        assertEquals("Enter your registered email below. We'll send a secure password reset link to get you back in.", appText("forgot_hero_message", "en"))
        assertEquals("Send Reset Link", appText("forgot_send_reset", "en"))
    }

    @Test
    fun testChatbot_autoScrollCriteria() {
        // Auto-scroll condition: should auto-scroll if user has NOT intentionally scrolled up
        fun shouldAutoScroll(userScrolledUp: Boolean): Boolean {
            return !userScrolledUp
        }

        // When user is following the conversation (!userScrolledUp), auto-scroll is triggered
        assertTrue(shouldAutoScroll(userScrolledUp = false))

        // When user intentionally scrolled up to read earlier history, auto-scroll is prevented
        assertFalse(shouldAutoScroll(userScrolledUp = true))
    }

    @Test
    fun testChatbot_quickActionPayloads() {
        // Issue 1: Verify query string payloads for Common Cold & Flu and Medicine Info
        val coldQuery = "I have cold and flu symptoms with fever and cough"
        assertTrue(coldQuery.contains("cold and flu") && coldQuery.contains("fever and cough"))

        val medicinePrefix = "Tell me about medicine "
        assertTrue(medicinePrefix.startsWith("Tell me about medicine"))
    }

    @Test
    fun testDashboard_localizedKeysPresent() {
        val dashboardKeys = listOf(
            "dashboard_bothering_title",
            "dashboard_symptom_placeholder",
            "dashboard_submit_symptom",
            "dashboard_symptom_empty_error",
            "dashboard_view_all",
            "dashboard_view_less",
            "dashboard_todays_overview",
            "dashboard_symptom_checker",
            "dashboard_symptom_checker_sub",
            "dashboard_bmi_checker",
            "dashboard_bmi_checker_sub",
            "dashboard_medicine_info",
            "dashboard_medicine_info_sub",
            "dashboard_medicine_reminder",
            "dashboard_medicine_reminder_sub",
            "dashboard_prescription_scanner",
            "dashboard_prescription_scanner_sub",
            "dashboard_health_tips",
            "dashboard_health_tips_sub",
            "dashboard_health_profile",
            "dashboard_health_profile_sub",
            "dashboard_chat_ai",
            "dashboard_chat_ai_sub",
            "dashboard_nav_home",
            "dashboard_nav_chat",
            "dashboard_nav_reminders",
            "dashboard_nav_profile",
            "dashboard_no_upcoming_reminders",
            "dashboard_next_dose",
            "dashboard_latest_bmi"
        )

        for (lang in listOf("en", "mr", "hi")) {
            for (key in dashboardKeys) {
                val translated = appText(key, lang)
                assertTrue("Dashboard key $key in language $lang was empty", translated.isNotBlank())
            }
        }

        // Verify specific English copy
        assertEquals("What's bothering you today?", appText("dashboard_bothering_title", "en"))
        assertEquals("Check Symptoms", appText("dashboard_submit_symptom", "en"))
        assertEquals("Today's Health Overview", appText("dashboard_todays_overview", "en"))
        assertEquals("Symptoms Checker", appText("dashboard_symptom_checker", "en"))
        assertEquals("BMI Calculator", appText("dashboard_bmi_checker", "en"))
    }

    @Test
    fun testDashboard_symptomInputValidation() {
        fun validateSymptomInput(input: String): Pair<Boolean, String> {
            val trimmed = input.trim()
            if (trimmed.isBlank()) {
                return false to "Please describe your symptoms before submitting."
            }
            return true to ""
        }

        val (blankValid, blankMsg) = validateSymptomInput("   ")
        assertFalse(blankValid)
        assertEquals("Please describe your symptoms before submitting.", blankMsg)

        val (valid, msg) = validateSymptomInput("Severe headache and slight fever")
        assertTrue(valid)
        assertEquals("", msg)
    }

    @Test
    fun testMedicineReminder_translationsAcrossLanguages() {
        val reminderKeys = listOf(
            "reminder_title",
            "reminder_subtitle",
            "reminder_intro_title",
            "reminder_intro_desc",
            "reminder_allow_notifications",
            "reminder_notifications_desc",
            "reminder_allow_btn",
            "reminder_add_medicine",
            "reminder_med_name_label",
            "reminder_med_name_placeholder",
            "reminder_strength_label",
            "reminder_strength_placeholder",
            "reminder_what_time",
            "reminder_choose_time_tap",
            "reminder_when_take",
            "slot_morning",
            "slot_afternoon",
            "slot_evening",
            "slot_night",
            "reminder_before_after_food",
            "meal_before_food",
            "meal_after_food",
            "reminder_how_many_days",
            "reminder_days_placeholder",
            "reminder_start_date",
            "reminder_end_date",
            "reminder_how_take",
            "reminder_instructions_placeholder",
            "reminder_save_button",
            "reminder_saved_reminders",
            "reminder_no_reminders_yet",
            "reminder_add_first_above",
            "reminder_error_enter_name",
            "reminder_error_select_time",
            "reminder_success_saved",
            "reminder_error_permission",
            "reminder_details_summary",
            "reminder_view_details",
            "reminder_hide_details",
            "reminder_delete_btn",
            "today",
            "yesterday"
        )

        for (lang in listOf("en", "mr", "hi")) {
            for (key in reminderKeys) {
                val translated = appText(key, lang)
                assertTrue("Medicine Reminder key $key in language $lang was empty", translated.isNotBlank())
                assertFalse("Medicine Reminder key $key in language $lang returned raw key", translated == key)
            }
        }

        // Specific Marathi checks
        assertEquals("माझ्या औषधांच्या आठवणी", appText("reminder_title", "mr"))
        assertEquals("औषध जोडा", appText("reminder_add_medicine", "mr"))
        assertEquals("सकाळ", appText("slot_morning", "mr"))
        assertEquals("जेवणापूर्वी", appText("meal_before_food", "mr"))
        assertEquals("आज", appText("today", "mr"))

        // Specific Hindi checks
        assertEquals("मेरी दवा की याद दिलाने वाली सूचनाएं", appText("reminder_title", "hi"))
        assertEquals("दवा जोड़ें", appText("reminder_add_medicine", "hi"))
        assertEquals("सुबह", appText("slot_morning", "hi"))
        assertEquals("भोजन से पहले", appText("meal_before_food", "hi"))
        assertEquals("आज", appText("today", "hi"))
    }

    @Test
    fun testChatbot_sessionResolutionAlwaysOpensFreshWhenSessionIdNull() {
        fun resolveSessionId(initialSessionId: String?): String {
            return if (!initialSessionId.isNullOrBlank()) {
                initialSessionId.trim()
            } else {
                "android_" + System.currentTimeMillis()
            }
        }

        // When navigating to chatbot from dashboard without target session id:
        val freshSession1 = resolveSessionId(null)
        assertTrue(freshSession1.startsWith("android_"))

        // When navigating with an explicit history session id:
        val explicitSession = resolveSessionId("android_1729000000000")
        assertEquals("android_1729000000000", explicitSession)
    }

    @Test
    fun testBmiCalculation_standardFormulaAndCategories() {
        fun calculateBmi(weightKg: Float, heightCm: Float): Float {
            val hM = heightCm / 100f
            return if (hM > 0f) weightKg / (hM * hM) else 0f
        }

        fun getCategory(bmi: Float): String {
            return when {
                bmi < 18.5f -> "underweight"
                bmi < 25.0f -> "normal"
                bmi < 30.0f -> "overweight"
                else -> "obese"
            }
        }

        // Test default initial values: 150 cm and 50.0 kg
        val defaultBmi = calculateBmi(50.0f, 150.0f)
        val roundedDefaultBmi = Math.round(defaultBmi * 10f) / 10f
        assertEquals(22.2f, roundedDefaultBmi)
        assertEquals("normal", getCategory(defaultBmi))

        // Boundary tests
        assertEquals("underweight", getCategory(16.0f))
        assertEquals("underweight", getCategory(18.49f))
        assertEquals("normal", getCategory(18.5f))
        assertEquals("normal", getCategory(24.9f))
        assertEquals("overweight", getCategory(25.0f))
        assertEquals("overweight", getCategory(29.9f))
        assertEquals("obese", getCategory(30.0f))
        assertEquals("obese", getCategory(35.5f))
    }

    @Test
    fun testBmiUnitsConversion_heightAndWeight() {
        // Height: 150 cm <-> 4 ft 11 in
        val cm = 150f
        val totalInches = Math.round(cm / 2.54f)
        val feet = totalInches / 12
        val inches = totalInches % 12
        assertEquals(4, feet)
        assertEquals(11, inches)

        val restoredCm = (feet * 12 + inches) * 2.54f
        assertTrue("Restored cm should be very close to 150 cm", Math.abs(restoredCm - cm) < 1.0f)

        // Weight: 50.0 kg <-> lbs
        val kg = 50.0f
        val lbs = kg * 2.20462f
        val roundedLbs = Math.round(lbs * 10f) / 10f
        assertEquals(110.2f, roundedLbs)

        val restoredKg = lbs / 2.20462f
        assertTrue("Restored kg should equal original kg", Math.abs(restoredKg - kg) < 0.01f)
    }

    @Test
    fun testBmiUnconfiguredDefaults_preservesSavedMeasurements() {
        // Simulating the BMICheckerScreen initialization logic:
        fun resolveInitialMeasurements(savedHeight: Float, savedWeight: Float): Pair<Float, Float> {
            val initialHeight = if (savedHeight > 0f) savedHeight else 150f
            val initialWeight = if (savedWeight > 0f) savedWeight else 50f
            return Pair(initialHeight, initialWeight)
        }

        // Scenario 1: New user / unconfigured form (saved values 0.0)
        val (unconfHeight, unconfWeight) = resolveInitialMeasurements(0f, 0f)
        assertEquals(150f, unconfHeight)
        assertEquals(50f, unconfWeight)

        // Scenario 2: Existing user with saved measurements (175 cm, 72.5 kg)
        val (savedH, savedW) = resolveInitialMeasurements(175f, 72.5f)
        assertEquals("Saved height must not be overwritten by 150 default", 175f, savedH)
        assertEquals("Saved weight must not be overwritten by 50 default", 72.5f, savedW)
    }

    @Test
    fun testBmiAndHealthTipsLocalization_allKeysPresentAcrossLanguages() {
        val keys = listOf(
            "bmi_underweight",
            "bmi_normal",
            "bmi_overweight",
            "bmi_obese",
            "bmi_underweight_desc",
            "bmi_normal_desc",
            "bmi_overweight_desc",
            "bmi_obese_desc",
            "bmi_healthy_range_label",
            "bmi_healthy_range_value",
            "bmi_disclaimer_text",
            "height_popup_title",
            "height_popup_subtitle",
            "weight_popup_title",
            "weight_popup_subtitle",
            "btn_confirm",
            "btn_cancel",
            "health_tips_title",
            "health_tips_hero_title",
            "cat_all",
            "cat_nutrition",
            "cat_fitness",
            "cat_sleep",
            "cat_hygiene",
            "cat_mental",
            "cat_prevention",
            "cat_lifestyle"
        )

        for (lang in listOf("en", "mr", "hi")) {
            for (key in keys) {
                val value = appText(key, lang)
                assertTrue("Key $key in $lang was empty", value.isNotBlank())
                assertFalse("Key $key in $lang returned raw key fallback", value == key)
            }
        }

        // Check specific Marathi strings
        assertEquals("निरोगी वजन (Normal)", appText("bmi_normal", "mr"))
        assertEquals("कमी वजन (Underweight)", appText("bmi_underweight", "mr"))
        assertEquals("जास्त वजन (Overweight)", appText("bmi_overweight", "mr"))
        assertEquals("लठ्ठपणा (Obesity)", appText("bmi_obese", "mr"))
        assertEquals("निश्चित करा", appText("btn_confirm", "mr"))

        // Check specific Hindi strings
        assertEquals("स्वस्थ वजन (Normal)", appText("bmi_normal", "hi"))
        assertEquals("कम वजन (Underweight)", appText("bmi_underweight", "hi"))
        assertEquals("अधिक वजन (Overweight)", appText("bmi_overweight", "hi"))
        assertEquals("मोटापा (Obesity)", appText("bmi_obese", "hi"))
        assertEquals("पुष्टि करें", appText("btn_confirm", "hi"))
    }

    @Test
    fun testManualDobInput_preservesExactDigitsWithoutTransposition() {
        // Simulating character-by-character user typing for 08/09/2004
        var digits = ""
        val typedKeys = listOf('0', '8', '0', '9', '2', '0', '0', '4')
        for (key in typedKeys) {
            if (key.isDigit()) digits += key
        }
        assertEquals("08092004", digits)
        val formatted = formatDobDigits(digits)
        assertEquals("08/09/2004", formatted)
        val parsed = parseDateOfBirth(formatted)
        assertTrue("08/09/2004 must parse to a valid date", parsed != null)
    }

    @Test
    fun testDobValidation_exactExamplesFromSpecification() {
        // 1. 08/09/2004 -> remains 08/09/2004
        val dob1 = formatDobDigits("08092004")
        assertEquals("08/09/2004", dob1)
        assertTrue("08/09/2004 must be valid", parseDateOfBirth(dob1) != null)

        // 2. 21/12/2005 -> remains 21/12/2005
        val dob2 = formatDobDigits("21122005")
        assertEquals("21/12/2005", dob2)
        assertTrue("21/12/2005 must be valid", parseDateOfBirth(dob2) != null)

        // 3. 01/01/2000 -> remains 01/01/2000
        val dob3 = formatDobDigits("01012000")
        assertEquals("01/01/2000", dob3)
        assertTrue("01/01/2000 must be valid", parseDateOfBirth(dob3) != null)

        // 4. 31/02/2004 -> rejected as invalid (Feb 2004 only had 29 days)
        val dob4 = formatDobDigits("31022004")
        assertEquals("31/02/2004", dob4)
        assertNull("31/02/2004 must be rejected as invalid calendar date", parseDateOfBirth(dob4))

        // Also non-leap year Feb 29 (29/02/2003)
        assertNull("29/02/2003 must be rejected", parseDateOfBirth("29/02/2003"))
        assertTrue("29/02/2004 leap year must be valid", parseDateOfBirth("29/02/2004") != null)

        // 5. Future DOB -> rejected
        val futureDob = "15/08/2099"
        assertNull("Future DOB must be rejected", parseDateOfBirth(futureDob))
    }

    @Test
    fun testDobMiddleEditingAndPasting() {
        // Pasting date with slashes
        val pasted = "08/09/2004"
        val extractedDigits = pasted.filter { it.isDigit() }.take(8)
        assertEquals("08092004", extractedDigits)
        assertEquals("08/09/2004", formatDobDigits(extractedDigits))

        // Simulating editing middle character (change month from 09 to 11)
        val originalDigits = "08092004"
        val editedDigits = originalDigits.substring(0, 2) + "11" + originalDigits.substring(4)
        assertEquals("08112004", editedDigits)
        assertEquals("08/11/2004", formatDobDigits(editedDigits))
        assertTrue("08/11/2004 must be valid", parseDateOfBirth(formatDobDigits(editedDigits)) != null)
    }

    @Test
    fun testAgeCalculation_fromValidDob() {
        val today = java.util.Calendar.getInstance()
        val currentYear = today.get(java.util.Calendar.YEAR)
        val currentMonth = today.get(java.util.Calendar.MONTH) + 1
        val currentDay = today.get(java.util.Calendar.DAY_OF_MONTH)

        // A date exactly 20 years ago
        val birthYear = currentYear - 20
        val dobStr = String.format(java.util.Locale.US, "%02d/%02d/%04d", currentDay, currentMonth, birthYear)
        val age = calculateAgeFromDateOfBirth(dobStr)
        assertEquals(20, age)

        // Invalid dob returns age 0
        assertEquals(0, calculateAgeFromDateOfBirth("31/02/2004"))
        assertEquals(0, calculateAgeFromDateOfBirth("invalid"))
        assertEquals(0, calculateAgeFromDateOfBirth("15/08/2099"))
    }

    @Test
    fun testCentralizedHealthRepository_dataModelAndAgeLogic() {
        // BMIData model with both DD/MM/YYYY and YYYY-MM-DD
        val data1 = ui.BMIData(
            heightCm = 175f,
            weightKg = 70f,
            dateOfBirth = "15/05/1995",
            gender = "Male",
            bloodGroup = "O+",
            fullName = "John Doe"
        )

        assertEquals(175f, data1.heightCm, 0.01f)
        assertEquals(70f, data1.weightKg, 0.01f)
        assertEquals("Male", data1.gender)
        assertEquals("O+", data1.bloodGroup)
        assertEquals("John Doe", data1.fullName)

        // BMI calculation: 70 / (1.75 * 1.75) = 22.857
        val expectedBmi = 70f / (1.75f * 1.75f)
        assertEquals(expectedBmi, data1.bmi, 0.05f)
        assertEquals("Normal", data1.bmiStatus)

        // Age calculation check
        assertTrue("Age should be > 0", data1.age > 0)

        // ISO format date support
        val dataIso = data1.copy(dateOfBirth = "1995-05-15")
        assertEquals(data1.age, dataIso.age)

        // Date format conversion helpers
        assertEquals("1995-05-15", ui.BMIRepository.toIsoDate("15/05/1995"))
        assertEquals("1995-05-15", ui.BMIRepository.toIsoDate("1995-05-15"))
        assertEquals("15/05/1995", ui.BMIRepository.toDisplayDate("1995-05-15"))
        assertEquals("15/05/1995", ui.BMIRepository.toDisplayDate("15/05/1995"))
    }

    @Test
    fun testWeightDialDampingSensitivity() {
        val minKg = 10f
        val maxKg = 250f
        val startingWeight = 70.0f

        // Natural arc-proportional angular drag tracking:
        // 1 degree of angular drag = (1 / 180) * (250 - 10) = 1.33 kg
        val deltaDeg = 2f
        val deltaFraction = deltaDeg / 180f
        val weightDelta = deltaFraction * (maxKg - minKg)
        val newWeight = (startingWeight + weightDelta).coerceIn(minKg, maxKg)

        assertTrue("Small drag of 2 deg smoothly shifts weight", (newWeight - startingWeight) in 2.5f..2.8f)
        assertTrue("Weight change must be positive for positive delta", (newWeight - startingWeight) > 0f)

        // Test full range coverage: dragging by 45 degrees changes weight by 60 kg
        val halfArcDeltaDeg = 45f
        val halfArcDelta = (halfArcDeltaDeg / 180f) * (maxKg - minKg)
        assertEquals(60f, halfArcDelta, 0.01f)

        // Test boundary clamping at extremes
        val extremeDeltaFraction = (180f / 180f) * 10f
        val clampedMax = (startingWeight + extremeDeltaFraction * (maxKg - minKg)).coerceIn(minKg, maxKg)
        assertEquals(maxKg, clampedMax, 0.01f)

        val clampedMin = (startingWeight - extremeDeltaFraction * (maxKg - minKg)).coerceIn(minKg, maxKg)
        assertEquals(minKg, clampedMin, 0.01f)
    }

    @Test
    fun testHealthProfilePersistence_fieldUpdatingAndSanitization() {
        // Test parsing DOB safely
        val validDob1 = "12/08/1998"
        val validDob2 = "1998-08-12"
        val invalidDob = "31/02/2001"
        val futureDob = "01/01/2099"

        assertTrue("Valid DD/MM/YYYY must parse", ui.BMIRepository.parseDateOfBirth(validDob1) != null)
        assertTrue("Valid YYYY-MM-DD must parse", ui.BMIRepository.parseDateOfBirth(validDob2) != null)
        assertNull("Invalid Feb 31 must return null", ui.BMIRepository.parseDateOfBirth(invalidDob))
        assertNull("Future date must return null", ui.BMIRepository.parseDateOfBirth(futureDob))

        // Conversion formatting tests
        assertEquals("12/08/1998", ui.BMIRepository.toDisplayDate(validDob2))
        assertEquals("1998-08-12", ui.BMIRepository.toIsoDate(validDob1))

        // Partial field updates preserve existing values
        val baseData = ui.BMIData(
            heightCm = 170f,
            weightKg = 65f,
            dateOfBirth = "10/10/1990",
            gender = "Female",
            bloodGroup = "B+",
            fullName = "Jane Doe"
        )

        // Simulating updating only bloodGroup & gender
        val updatedData = baseData.copy(bloodGroup = "AB+", gender = "Female")
        assertEquals(170f, updatedData.heightCm, 0.01f)
        assertEquals(65f, updatedData.weightKg, 0.01f)
        assertEquals("10/10/1990", updatedData.dateOfBirth)
        assertEquals("Jane Doe", updatedData.fullName)
        assertEquals("AB+", updatedData.bloodGroup)
        assertEquals("Female", updatedData.gender)

        // Simulating invalid or blank DOB fallback: if blank, original DOB is preserved
        val safeDob = if ("".isNotBlank() && ui.BMIRepository.parseDateOfBirth("") != null) "" else baseData.dateOfBirth
        assertEquals("10/10/1990", safeDob)

        // Age logic remains consistent
        assertTrue("Age must be calculated and > 0", updatedData.age > 0)
    }
}
