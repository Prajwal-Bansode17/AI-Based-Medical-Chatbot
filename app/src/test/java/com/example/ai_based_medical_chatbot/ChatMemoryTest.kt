package com.example.ai_based_medical_chatbot

import android.content.SharedPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.example.ai_based_medical_chatbot.helpers.FakeSharedPreferences
import com.example.ai_based_medical_chatbot.helpers.FakeMedicalApiService
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
import kotlinx.coroutines.runBlocking
import retrofit2.Response
import java.io.IOException

class ChatMemoryTest {

    private lateinit var fakePrefs: FakeSharedPreferences

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        ChatMemoryRepository.customPrefs = fakePrefs
    }

    @After
    fun tearDown() {
        ChatMemoryRepository.customPrefs = null
        ChatMemoryRepository.customApiService = null
    }

    // =========================================================================
    // 1. MEMORY PERMISSION AND CONSENT
    // =========================================================================

    @Test
    fun testExplicitConsentRequirement_disabledByDefault() {
        val userId = "user_auth_123"

        // Defaults to disabled: neither read nor write permitted
        assertFalse(
            "Memory consent must default to false",
            ChatMemoryRepository.isMemoryConsentEnabled(context = null, userId = userId)
        )
        assertFalse(
            "Memory operations must not be permitted without explicit consent",
            ChatMemoryRepository.isMemoryPermitted(context = null, userId = userId)
        )

        val memoryItem = ChatMemoryItem(
            userId = userId,
            key = "comm_concise",
            content = "Prefers concise, brief responses",
            category = MemoryCategory.COMMUNICATION
        )

        // Write must fail when consent is not granted
        val saved = ChatMemoryRepository.saveMemory(context = null, userId = userId, item = memoryItem)
        assertFalse("saveMemory must return false when consent is disabled", saved)

        // Read must return empty list when consent is not granted
        val memories = ChatMemoryRepository.getMemories(context = null, userId = userId)
        assertTrue("getMemories must return empty list when consent is disabled", memories.isEmpty())

        // Explicitly enable consent
        ChatMemoryRepository.setMemoryConsentEnabled(context = null, userId = userId, enabled = true)
        assertTrue(
            "Consent must be enabled after explicit opt-in",
            ChatMemoryRepository.isMemoryConsentEnabled(context = null, userId = userId)
        )
        assertTrue(
            "Memory operations must be permitted once consent is enabled",
            ChatMemoryRepository.isMemoryPermitted(context = null, userId = userId)
        )

        // Now save should succeed
        val savedAfterConsent = ChatMemoryRepository.saveMemory(context = null, userId = userId, item = memoryItem)
        assertTrue("saveMemory must succeed after consent is enabled", savedAfterConsent)

        val retrieved = ChatMemoryRepository.getMemories(context = null, userId = userId)
        assertEquals(1, retrieved.size)
        assertEquals("comm_concise", retrieved[0].key)
    }

    // =========================================================================
    // 2. GUEST MODE SAFETY
    // =========================================================================

    @Test
    fun testGuestMode_neverReadsOrWritesMemory() {
        val guestIds = listOf(null, "", "   ", "guest", "Guest", "GUEST", "account_guest", "account_guest_session")

        for (guestId in guestIds) {
            assertTrue("isGuestUser must recognize guest ID: $guestId", ChatMemoryRepository.isGuestUser(guestId))

            // Consent cannot be enabled for guest
            ChatMemoryRepository.setMemoryConsentEnabled(context = null, userId = guestId, enabled = true)
            assertFalse(
                "Guest users must never have memory consent enabled",
                ChatMemoryRepository.isMemoryConsentEnabled(context = null, userId = guestId)
            )
            assertFalse(
                "Memory must never be permitted in guest mode",
                ChatMemoryRepository.isMemoryPermitted(context = null, userId = guestId)
            )

            val item = ChatMemoryItem(
                userId = guestId.orEmpty(),
                key = "pref",
                content = "Prefers short responses",
                category = MemoryCategory.COMMUNICATION
            )

            // Write must be rejected
            val saved = ChatMemoryRepository.saveMemory(context = null, userId = guestId, item = item)
            assertFalse("Guest memory save must be rejected", saved)

            // Read must return empty
            val list = ChatMemoryRepository.getMemories(context = null, userId = guestId)
            assertTrue("Guest memory read must be empty", list.isEmpty())
        }
    }

    // =========================================================================
    // 3. TEMPORARY CHAT MODE
    // =========================================================================

    @Test
    fun testTemporaryChatMode_strictlyBlocksMemoryOperations() {
        val userId = "user_authenticated_456"
        ChatMemoryRepository.setMemoryConsentEnabled(context = null, userId = userId, enabled = true)

        // Normal chat permits memory
        assertTrue(
            "Normal chat with consent must permit memory",
            ChatMemoryRepository.isMemoryPermitted(context = null, userId = userId, isTemporaryChat = false)
        )

        // Temporary chat strictly denies memory
        assertFalse(
            "Temporary chat must strictly disallow memory even for consented users",
            ChatMemoryRepository.isMemoryPermitted(context = null, userId = userId, isTemporaryChat = true)
        )

        // Pre-save a memory in normal mode
        val item = ChatMemoryItem(
            userId = userId,
            key = "diet_veg",
            content = "Follows a vegetarian diet",
            category = MemoryCategory.DIET
        )
        ChatMemoryRepository.saveMemory(context = null, userId = userId, item = item, isTemporaryChat = false)
        assertEquals(1, ChatMemoryRepository.getMemories(context = null, userId = userId, isTemporaryChat = false).size)

        // Read during temporary chat must return empty list to protect privacy
        val tempRead = ChatMemoryRepository.getMemories(context = null, userId = userId, isTemporaryChat = true)
        assertTrue("getMemories during temporary chat must return empty list", tempRead.isEmpty())

        // Write during temporary chat must be rejected
        val tempItem = ChatMemoryItem(
            userId = userId,
            key = "goal_walk",
            content = "Walks 30 minutes daily",
            category = MemoryCategory.LIFESTYLE
        )
        val tempSaved = ChatMemoryRepository.saveMemory(context = null, userId = userId, item = tempItem, isTemporaryChat = true)
        assertFalse("saveMemory during temporary chat must be rejected", tempSaved)

        // Verify the original item is still intact and temp item was not saved
        val normalList = ChatMemoryRepository.getMemories(context = null, userId = userId, isTemporaryChat = false)
        assertEquals(1, normalList.size)
        assertEquals("diet_veg", normalList[0].key)
    }

    // =========================================================================
    // 4. USER ISOLATION
    // =========================================================================

    @Test
    fun testUserIsolation_memoriesStrictlySeparated() {
        val userA = "user_alpha"
        val userB = "user_beta"

        ChatMemoryRepository.setMemoryConsentEnabled(context = null, userId = userA, enabled = true)
        ChatMemoryRepository.setMemoryConsentEnabled(context = null, userId = userB, enabled = true)

        ChatMemoryRepository.saveMemory(
            context = null,
            userId = userA,
            item = ChatMemoryItem(userId = userA, key = "a_pref", content = "User A communication preference", category = MemoryCategory.COMMUNICATION)
        )

        ChatMemoryRepository.saveMemory(
            context = null,
            userId = userB,
            item = ChatMemoryItem(userId = userB, key = "b_diet", content = "User B vegetarian diet", category = MemoryCategory.DIET)
        )

        val memoriesA = ChatMemoryRepository.getMemories(context = null, userId = userA)
        val memoriesB = ChatMemoryRepository.getMemories(context = null, userId = userB)

        assertEquals(1, memoriesA.size)
        assertEquals("a_pref", memoriesA[0].key)
        assertEquals("user_alpha", memoriesA[0].userId)

        assertEquals(1, memoriesB.size)
        assertEquals("b_diet", memoriesB[0].key)
        assertEquals("user_beta", memoriesB[0].userId)

        // Clearing user A must NOT touch user B
        ChatMemoryRepository.clearAllMemories(context = null, userId = userA)
        assertTrue(ChatMemoryRepository.getMemories(context = null, userId = userA).isEmpty())
        assertEquals(1, ChatMemoryRepository.getMemories(context = null, userId = userB).size)
    }

    // =========================================================================
    // 5. SAFETY FILTER & SENSITIVE MEDICAL REJECTION
    // =========================================================================

    @Test
    fun testMemorySafetyFilter_strictlyRejectsMedicalDiagnoses() {
        val sensitiveDiagnoses = listOf(
            "I was diagnosed with Type 2 diabetes",
            "I have hypertension and high blood pressure",
            "Suffering from acute asthma attacks",
            "My doctor diagnosed me with coronary artery disease",
            "I have severe clinical depression and anxiety",
            "Diagnosed with cancer last year"
        )

        for (diagnosis in sensitiveDiagnoses) {
            assertFalse(
                "Safety filter must reject diagnosis: $diagnosis",
                MemorySafetyFilter.isSafeForLongTermMemory(diagnosis)
            )
            assertNull(
                "Extractor must not extract memory from diagnosis: $diagnosis",
                MemoryExtractor.extractFromUserMessage("user1", diagnosis)
            )
        }
    }

    @Test
    fun testMemorySafetyFilter_strictlyRejectsPrescriptionsAndMedications() {
        val sensitiveMeds = listOf(
            "I take Metformin 500mg twice a day",
            "Doctor prescribed Lisinopril 10mg daily",
            "I was given Amoxicillin 250mg capsule",
            "Taking Paracetamol 650mg for body pain",
            "I inject 10 units of insulin every evening",
            "My daily tablet is Atorvastatin"
        )

        for (med in sensitiveMeds) {
            assertFalse(
                "Safety filter must reject medication: $med",
                MemorySafetyFilter.isSafeForLongTermMemory(med)
            )
            assertNull(
                "Extractor must not extract memory from medication: $med",
                MemoryExtractor.extractFromUserMessage("user1", med)
            )
        }
    }

    @Test
    fun testMemorySafetyFilter_strictlyRejectsAllergies() {
        val sensitiveAllergies = listOf(
            "I have a severe penicillin allergy",
            "I am allergic to amoxicillin and sulfa drugs",
            "Allergic to aspirin, causes facial swelling",
            "Severe drug allergy to cephalosporin"
        )

        for (allergy in sensitiveAllergies) {
            assertFalse(
                "Safety filter must reject allergy: $allergy",
                MemorySafetyFilter.isSafeForLongTermMemory(allergy)
            )
            assertNull(
                "Extractor must not extract memory from allergy: $allergy",
                MemoryExtractor.extractFromUserMessage("user1", allergy)
            )
        }
    }

    @Test
    fun testMemorySafetyFilter_strictlyRejectsClinicalVitalsAndLabs() {
        val sensitiveVitals = listOf(
            "My blood pressure is 140/90 mmHg",
            "My HbA1c came back at 8.2%",
            "Current blood sugar is 210 mg/dL",
            "My heart rate is 115 bpm at rest",
            "Oxygen saturation spO2 is 92%"
        )

        for (vital in sensitiveVitals) {
            assertFalse(
                "Safety filter must reject clinical vitals: $vital",
                MemorySafetyFilter.isSafeForLongTermMemory(vital)
            )
            assertNull(
                "Extractor must not extract memory from clinical vitals: $vital",
                MemoryExtractor.extractFromUserMessage("user1", vital)
            )
        }
    }

    @Test
    fun testMemorySafetyFilter_strictlyRejectsCredentialsAndSecrets() {
        val credentials = listOf(
            "my password is SuperSecretPassword123!",
            "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0",
            "supabase_anon_key: abcdef123456789",
            "my api key is sk_live_51M001234567890",
            "my secret token is tok_test_12345"
        )

        for (cred in credentials) {
            assertFalse(
                "Safety filter must reject credentials: $cred",
                MemorySafetyFilter.isSafeForLongTermMemory(cred)
            )
            assertNull(
                "Extractor must not extract memory from credentials: $cred",
                MemoryExtractor.extractFromUserMessage("user1", cred)
            )
        }
    }

    // =========================================================================
    // 6. NON-SENSITIVE EXTRACTION (SAFE PREFERENCES ONLY)
    // =========================================================================

    @Test
    fun testMemoryExtractor_extractsValidNonSensitivePreferences() {
        val userId = "user_extract"

        // Communication preferences
        val concise = MemoryExtractor.extractFromUserMessage(userId, "Please keep your answers brief and concise.")
        assertNotNull(concise)
        assertEquals(MemoryCategory.COMMUNICATION, concise!!.category)
        assertTrue(concise.content.contains("concise", ignoreCase = true))

        val bullets = MemoryExtractor.extractFromUserMessage(userId, "I prefer explanations with bullet points.")
        assertNotNull(bullets)
        assertEquals(MemoryCategory.COMMUNICATION, bullets!!.category)
        assertTrue(bullets.content.contains("bullet points", ignoreCase = true))

        val simple = MemoryExtractor.extractFromUserMessage(userId, "Please explain in simple terms without medical jargon.")
        assertNotNull(simple)
        assertEquals(MemoryCategory.COMMUNICATION, simple!!.category)

        // Dietary preferences
        val veg = MemoryExtractor.extractFromUserMessage(userId, "I follow a strict vegetarian diet.")
        assertNotNull(veg)
        assertEquals(MemoryCategory.DIET, veg!!.category)

        val caffeine = MemoryExtractor.extractFromUserMessage(userId, "I avoid caffeine after 4pm.")
        assertNotNull(caffeine)
        assertEquals(MemoryCategory.DIET, caffeine!!.category)

        // Lifestyle & routine
        val walking = MemoryExtractor.extractFromUserMessage(userId, "I walk 30 minutes every morning.")
        assertNotNull(walking)
        assertEquals(MemoryCategory.LIFESTYLE, walking!!.category)

        val desk = MemoryExtractor.extractFromUserMessage(userId, "I have a sedentary desk job all day.")
        assertNotNull(desk)
        assertEquals(MemoryCategory.LIFESTYLE, desk!!.category)

        // Wellness goals
        val water = MemoryExtractor.extractFromUserMessage(userId, "I aim to drink 2 liters of water daily.")
        assertNotNull(water)
        assertEquals(MemoryCategory.GOAL, water!!.category)

        val sleepGoal = MemoryExtractor.extractFromUserMessage(userId, "My goal is to improve my sleep routine and consistency.")
        assertNotNull(sleepGoal)
        assertEquals(MemoryCategory.GOAL, sleepGoal!!.category)

        // Irrelevant / Conversational queries must return null (NEVER INVENT MEMORIES)
        assertNull(MemoryExtractor.extractFromUserMessage(userId, "What is the capital of Maharashtra?"))
        assertNull(MemoryExtractor.extractFromUserMessage(userId, "Good morning, how are you doing?"))
        assertNull(MemoryExtractor.extractFromUserMessage(userId, "What does paracetamol do?"))
    }

    // =========================================================================
    // 7. MEMORY RETRIEVAL & RELEVANCE RANKING
    // =========================================================================

    @Test
    fun testMemoryRetriever_relevanceScoringAndCapping() {
        val userId = "user_retrieve"
        val memories = listOf(
            ChatMemoryItem(userId = userId, key = "diet_veg", content = "Follows vegetarian diet", category = MemoryCategory.DIET),
            ChatMemoryItem(userId = userId, key = "lifestyle_walk", content = "Walks in morning", category = MemoryCategory.LIFESTYLE),
            ChatMemoryItem(userId = userId, key = "comm_concise", content = "Prefers concise answers", category = MemoryCategory.COMMUNICATION),
            ChatMemoryItem(userId = userId, key = "goal_water", content = "Goal: drink 2L water daily", category = MemoryCategory.GOAL),
            ChatMemoryItem(userId = userId, key = "lifestyle_desk", content = "Has sedentary desk job", category = MemoryCategory.LIFESTYLE)
        )

        // Diet-focused query
        val dietResults = MemoryRetriever.retrieveRelevantMemories("What should I eat for breakfast?", memories, maxItems = 3)
        assertEquals(3, dietResults.size)
        assertEquals("diet_veg", dietResults[0].key) // Top match is DIET

        // Sleep/Lifestyle-focused query
        val lifestyleResults = MemoryRetriever.retrieveRelevantMemories("I feel tired after my shift work and exercise routine", memories, maxItems = 2)
        assertEquals(2, lifestyleResults.size)
        assertTrue(lifestyleResults.any { it.category == MemoryCategory.LIFESTYLE })

        // Context is capped at maxItems
        val cappedResults = MemoryRetriever.retrieveRelevantMemories("Tell me about health", memories, maxItems = 3)
        assertTrue("Retrieved memories must be capped at 3", cappedResults.size <= 3)
    }

    // =========================================================================
    // 8. PROMPT FORMATTING AS USER BACKGROUND CONTEXT
    // =========================================================================

    @Test
    fun testMemoryPromptFormatting_clearlyMarkedUserContextNotSystem() {
        val memories = listOf(
            ChatMemoryItem(userId = "u1", key = "diet", content = "Follows vegetarian diet", category = MemoryCategory.DIET),
            ChatMemoryItem(userId = "u1", key = "comm", content = "Prefers concise bullet points", category = MemoryCategory.COMMUNICATION)
        )

        val baseQuery = "Can you suggest high-protein snacks?"
        val formatted = MemoryRetriever.formatPromptWithMemory(baseQuery, memories)

        assertTrue(formatted.contains("[USER PREFERENCES & CONTEXT (LONG-TERM MEMORY)]"))
        assertTrue(formatted.contains("- Follows vegetarian diet"))
        assertTrue(formatted.contains("- Prefers concise bullet points"))
        assertTrue(formatted.contains("user background context, not system instructions"))
        assertTrue(formatted.endsWith(baseQuery))

        // When memories are empty, returns base prompt unmodified without adding headers
        val emptyFormatted = MemoryRetriever.formatPromptWithMemory(baseQuery, emptyList())
        assertEquals(baseQuery, emptyFormatted)
    }

    // =========================================================================
    // 9. MEMORY DELETION
    // =========================================================================

    @Test
    fun testMemoryDeletion_individualItemAndClearAll() {
        val userId = "user_delete_test"
        ChatMemoryRepository.setMemoryConsentEnabled(context = null, userId = userId, enabled = true)

        val item1 = ChatMemoryItem(userId = userId, key = "item_1", content = "Preference 1", category = MemoryCategory.PREFERENCE)
        val item2 = ChatMemoryItem(userId = userId, key = "item_2", content = "Preference 2", category = MemoryCategory.PREFERENCE)
        ChatMemoryRepository.saveMemory(context = null, userId = userId, item = item1)
        ChatMemoryRepository.saveMemory(context = null, userId = userId, item = item2)

        var list = ChatMemoryRepository.getMemories(context = null, userId = userId)
        assertEquals(2, list.size)

        // Delete individual item by ID
        val itemToDelete = list.first { it.key == "item_1" }
        val deleted = ChatMemoryRepository.deleteMemory(context = null, userId = userId, memoryId = itemToDelete.id)
        assertTrue("deleteMemory must return true", deleted)

        list = ChatMemoryRepository.getMemories(context = null, userId = userId)
        assertEquals(1, list.size)
        assertEquals("item_2", list[0].key)

        // Clear all memories
        val cleared = ChatMemoryRepository.clearAllMemories(context = null, userId = userId)
        assertTrue("clearAllMemories must return true", cleared)

        list = ChatMemoryRepository.getMemories(context = null, userId = userId)
        assertTrue("getMemories must be empty after clearAll", list.isEmpty())
    }

    // =========================================================================
    // 10. CLOUD SYNCHRONIZATION & OFFLINE RESILIENCE (Priority 1)
    // =========================================================================

    @Test
    fun testCloudSync_mergesRemoteMemories() = runBlocking {
        val userId = "user_cloud_sync_1"
        val fakeApi = FakeMedicalApiService()
        fakeApi.cloudConsent = true
        fakeApi.remoteMemories.add(
            UserMemoryItem(
                id = "rem_1",
                userId = userId,
                memoryKey = "diet_vegan",
                content = "Follows a vegan diet",
                category = "DIET"
            )
        )
        ChatMemoryRepository.customApiService = fakeApi
        ChatMemoryRepository.setMemoryConsentEnabled(context = null, userId = userId, enabled = true)

        // Pre-save local memory
        val localItem = ChatMemoryItem(
            userId = userId,
            key = "comm_concise",
            content = "Prefers concise answers",
            category = MemoryCategory.COMMUNICATION
        )
        ChatMemoryRepository.saveMemory(context = null, userId = userId, item = localItem)

        // Run sync
        val synced = ChatMemoryRepository.syncMemoriesFromCloud(context = null, userId = userId)
        assertEquals(2, synced.size)
        assertTrue(synced.any { it.key == "diet_vegan" && it.content == "Follows a vegan diet" })
        assertTrue(synced.any { it.key == "comm_concise" && it.content == "Prefers concise answers" })

        // Check local persistence updated
        val localStored = ChatMemoryRepository.getMemories(context = null, userId = userId)
        assertEquals(2, localStored.size)
    }

    @Test
    fun testCloudSync_networkFailure_preservesLocalData() = runBlocking {
        val userId = "user_offline_resilience"
        val fakeApi = FakeMedicalApiService()
        fakeApi.shouldFailWithNetworkError = true
        ChatMemoryRepository.customApiService = fakeApi
        ChatMemoryRepository.setMemoryConsentEnabled(context = null, userId = userId, enabled = true)

        // Pre-save local memory
        val localItem = ChatMemoryItem(
            userId = userId,
            key = "lifestyle_yoga",
            content = "Practices daily morning yoga",
            category = MemoryCategory.LIFESTYLE
        )
        ChatMemoryRepository.saveMemory(context = null, userId = userId, item = localItem)

        // Network fails during sync: must NOT crash, must preserve local item
        val synced = ChatMemoryRepository.syncMemoriesFromCloud(context = null, userId = userId)
        assertEquals(1, synced.size)
        assertEquals("lifestyle_yoga", synced[0].key)

        val localStored = ChatMemoryRepository.getMemories(context = null, userId = userId)
        assertEquals(1, localStored.size)
        assertEquals("lifestyle_yoga", localStored[0].key)
    }

    @Test
    fun testCloudConsentSync_updatesLocalPreference() = runBlocking {
        val userId = "user_consent_sync"
        val fakeApi = FakeMedicalApiService()
        fakeApi.cloudConsent = true
        ChatMemoryRepository.customApiService = fakeApi

        assertFalse(ChatMemoryRepository.isMemoryConsentEnabled(context = null, userId = userId))

        val consent = ChatMemoryRepository.syncConsentFromCloud(context = null, userId = userId)
        assertTrue("syncConsentFromCloud must return remote consent", consent)
        assertTrue("Local preference must be updated from cloud", ChatMemoryRepository.isMemoryConsentEnabled(context = null, userId = userId))
    }

    @Test
    fun testSaveMemoryWithCloudSync_offlineFallback() = runBlocking {
        val userId = "user_save_offline"
        val fakeApi = FakeMedicalApiService()
        fakeApi.shouldFailWithNetworkError = true
        ChatMemoryRepository.customApiService = fakeApi
        ChatMemoryRepository.setMemoryConsentEnabled(context = null, userId = userId, enabled = true)

        val item = ChatMemoryItem(
            userId = userId,
            key = "hydration",
            content = "Goal: drink 2L water daily",
            category = MemoryCategory.GOAL
        )

        // Even with network failure, local save must succeed
        val saved = ChatMemoryRepository.saveMemoryWithCloudSync(context = null, userId = userId, item = item)
        assertTrue("saveMemoryWithCloudSync must succeed locally when offline", saved)

        val localStored = ChatMemoryRepository.getMemories(context = null, userId = userId)
        assertEquals(1, localStored.size)
        assertEquals("hydration", localStored[0].key)
    }

    @Test
    fun testDeleteMemoryWithCloudSync_removesFromLocalAndCloud() = runBlocking {
        val userId = "user_delete_sync"
        val fakeApi = FakeMedicalApiService()
        ChatMemoryRepository.customApiService = fakeApi
        ChatMemoryRepository.setMemoryConsentEnabled(context = null, userId = userId, enabled = true)

        val item = ChatMemoryItem(
            id = "mem_to_del",
            userId = userId,
            key = "del_key",
            content = "To be deleted",
            category = MemoryCategory.PREFERENCE
        )
        ChatMemoryRepository.saveMemory(context = null, userId = userId, item = item)
        assertEquals(1, ChatMemoryRepository.getMemories(context = null, userId = userId).size)

        val deleted = ChatMemoryRepository.deleteMemoryWithCloudSync(context = null, userId = userId, memoryId = "mem_to_del")
        assertTrue(deleted)
        assertTrue(ChatMemoryRepository.getMemories(context = null, userId = userId).isEmpty())
        assertTrue(fakeApi.deletedIds.contains("mem_to_del"))
    }

    @Test
    fun testClearAllMemoriesWithCloudSync_removesFromLocalAndCloud() = runBlocking {
        val userId = "user_clear_all_sync"
        val fakeApi = FakeMedicalApiService()
        ChatMemoryRepository.customApiService = fakeApi
        ChatMemoryRepository.setMemoryConsentEnabled(context = null, userId = userId, enabled = true)

        ChatMemoryRepository.saveMemory(
            context = null,
            userId = userId,
            item = ChatMemoryItem(userId = userId, key = "k1", content = "Content 1")
        )
        ChatMemoryRepository.saveMemory(
            context = null,
            userId = userId,
            item = ChatMemoryItem(userId = userId, key = "k2", content = "Content 2")
        )
        assertEquals(2, ChatMemoryRepository.getMemories(context = null, userId = userId).size)

        val cleared = ChatMemoryRepository.clearAllMemoriesWithCloudSync(context = null, userId = userId)
        assertTrue(cleared)
        assertTrue(ChatMemoryRepository.getMemories(context = null, userId = userId).isEmpty())
        assertTrue(fakeApi.clearedAll)
    }

    @Test
    fun testNewChatMemoryRecall_injectsBackgroundContext() {
        val userId = "user_new_chat_recall"
        ChatMemoryRepository.setMemoryConsentEnabled(context = null, userId = userId, enabled = true)

        val item = ChatMemoryItem(
            userId = userId,
            key = "diet_veg",
            content = "Follows a strict vegetarian diet",
            category = MemoryCategory.DIET
        )
        ChatMemoryRepository.saveMemory(context = null, userId = userId, item = item)

        // Starting a new conversation: query about meals
        val newChatQuery = "What should I make for lunch?"
        val userMemories = ChatMemoryRepository.getMemories(context = null, userId = userId)
        val relevant = MemoryRetriever.retrieveRelevantMemories(newChatQuery, userMemories)

        assertEquals(1, relevant.size)
        assertEquals("diet_veg", relevant[0].key)

        val prompt = MemoryRetriever.formatPromptWithMemory(newChatQuery, relevant)
        assertTrue(prompt.contains("[USER PREFERENCES & CONTEXT (LONG-TERM MEMORY)]"))
        assertTrue(prompt.contains("Follows a strict vegetarian diet"))
        assertTrue(prompt.contains(newChatQuery))
    }
}
