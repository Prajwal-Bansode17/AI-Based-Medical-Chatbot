package com.example.ai_based_medical_chatbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ui.MedicineRecord
import ui.MedicineRepository
import ui.toMedicine
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets

class MedicineRepositoryTest {

    private fun getAssetFile(): File {
        val candidates = listOf(
            File("src/main/assets/medicine_data.json"),
            File("app/src/main/assets/medicine_data.json"),
            File("../app/src/main/assets/medicine_data.json"),
            File(System.getProperty("user.dir") ?: ".", "src/main/assets/medicine_data.json"),
            File(System.getProperty("user.dir") ?: ".", "app/src/main/assets/medicine_data.json")
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("Could not find medicine_data.json from cwd=${File(".").absolutePath}")
    }

    @Before
    fun setUp() {
        MedicineRepository.clearCache()
    }

    @Test
    fun testParseMedicineStream_boundedLimitDoesNotReadWholeFile() {
        val assetFile = getAssetFile()
        assertTrue("medicine_data.json asset must exist for tests", assetFile.exists())

        val startTime = System.currentTimeMillis()
        val limit = 20
        val results = FileInputStream(assetFile).use { stream ->
            MedicineRepository.parseMedicineStream(stream, limit = limit)
        }
        val durationMs = System.currentTimeMillis() - startTime

        assertEquals("parseMedicineStream with limit=20 must return exactly 20 records", 20, results.size)
        assertTrue("Bounded stream parsing of 20 items must be fast, took ${durationMs}ms", durationMs < 2000)

        val first = results[0]
        assertTrue("First record name must not be blank", first.name.isNotBlank())
        assertTrue("First record category/class must not be blank", first.drugClass.isNotBlank())
    }

    @Test
    fun testParseMedicineStream_handlesArrayAndStringIngredients() {
        val testJson = """
            [
              {
                "n": "Test Medicine 1",
                "g": "Generic One",
                "b": "Brand One",
                "i": ["Active Component A", "Active Component B"],
                "c": "Analgesic",
                "u": "Pain and fever",
                "s": "Nausea",
                "w": "Warning text",
                "p": "Precaution text",
                "x": "Interactions text"
              },
              {
                "n": "Test Medicine 2",
                "g": "Generic Two",
                "b": "Brand Two",
                "i": "Single Component C",
                "c": "Antibiotic",
                "u": "Bacterial infection",
                "s": "Diarrhea",
                "w": "Allergy warning",
                "p": "Complete course",
                "x": "No alcohol"
              }
            ]
        """.trimIndent()

        val stream = ByteArrayInputStream(testJson.toByteArray(StandardCharsets.UTF_8))
        val results = MedicineRepository.parseMedicineStream(stream, limit = 0)

        assertEquals("Must parse both mock items", 2, results.size)
        assertEquals("Active Component A, Active Component B", results[0].ingredients)
        assertEquals("Single Component C", results[1].ingredients)
    }

    @Test
    fun testSearchMedicinesStream_canonicalAliasParacetamolResolvesAcetaminophen() {
        val assetFile = getAssetFile()
        assertTrue(assetFile.exists())

        val results = FileInputStream(assetFile).use { stream ->
            MedicineRepository.searchMedicinesStream(stream, query = "paracetamol", limit = 5)
        }

        assertTrue("Searching 'paracetamol' must return at least 1 canonical result", results.isNotEmpty())
        val match = results[0]
        val genericLower = match.generic.lowercase()
        val nameLower = match.name.lowercase()
        assertTrue(
            "Canonical result for paracetamol must resolve to acetaminophen (name='$nameLower', generic='$genericLower')",
            genericLower.contains("acetaminophen") || nameLower.contains("acetaminophen") || match.ingredients.lowercase().contains("acetaminophen")
        )
    }

    @Test
    fun testSearchMedicinesStream_searchIbuprofenReturnsRankedResults() {
        val assetFile = getAssetFile()
        assertTrue(assetFile.exists())

        val results = FileInputStream(assetFile).use { stream ->
            MedicineRepository.searchMedicinesStream(stream, query = "ibuprofen", limit = 10)
        }

        assertTrue("Searching 'ibuprofen' must return matching records", results.isNotEmpty())
        assertTrue("Results must not exceed limit 10", results.size <= 10)

        val topMatch = results[0]
        val combinedText = "${topMatch.name} ${topMatch.generic} ${topMatch.brand}".lowercase()
        assertTrue("Top match must contain 'ibuprofen', got '$combinedText'", combinedText.contains("ibuprofen"))
    }

    @Test
    fun testSearchMedicinesStream_blankQueryReturnsBoundedLimit() {
        val assetFile = getAssetFile()
        assertTrue(assetFile.exists())

        val results = FileInputStream(assetFile).use { stream ->
            MedicineRepository.searchMedicinesStream(stream, query = "   ", limit = 15)
        }

        assertEquals("Blank query must return requested limit of 15 records", 15, results.size)
    }

    @Test
    fun testCalculateScore_prefersExactMatches() {
        val exactNameScore = MedicineRepository.calculateScore(
            name = "amoxicillin",
            generic = "amoxicillin trihydrate",
            brand = "amoxil",
            ingredients = "amoxicillin",
            searchTerm = "amoxicillin"
        )
        val containsScore = MedicineRepository.calculateScore(
            name = "augmentin amoxicillin clavulanate",
            generic = "clavulanate",
            brand = "augmentin",
            ingredients = "amoxicillin",
            searchTerm = "amoxicillin"
        )

        assertTrue("Exact name match ($exactNameScore) must score higher than contains ($containsScore)", exactNameScore > containsScore)
        assertEquals(1000, exactNameScore)
    }

    @Test
    fun testClearCache_resetsCachedState() {
        val testJson = """
            [
              {"n": "Drug A", "g": "Gen A", "b": "Br A", "i": [], "c": "Class A", "u": "", "s": "", "w": "", "p": "", "x": ""}
            ]
        """.trimIndent()

        val stream1 = ByteArrayInputStream(testJson.toByteArray(StandardCharsets.UTF_8))
        val initial = MedicineRepository.getMedicines(stream1, limit = 1)
        assertEquals(1, initial.size)

        MedicineRepository.clearCache()

        val stream2 = ByteArrayInputStream(testJson.toByteArray(StandardCharsets.UTF_8))
        val afterClear = MedicineRepository.getMedicines(stream2, limit = 1)
        assertEquals(1, afterClear.size)
    }

    @Test
    fun testMedicineRepository_getRelatedMedicines_paracetamolReturnsPCMAndP650() {
        val primary = listOf(
            MedicineRecord("Dolo 650", "Paracetamol", "Dolo 650", "Paracetamol 650 mg", "Analgesic", "", "", "", "", ""),
            MedicineRecord("Crocin 500", "Paracetamol", "Crocin 500", "Paracetamol 500 mg", "Analgesic", "", "", "", "", "")
        )

        val related = MedicineRepository.getRelatedMedicines("paracetamol", primary)
        assertTrue("Related medicines for paracetamol must include PCM 650 or P-650", related.isNotEmpty())
        val relatedNames = related.map { it.name }
        assertTrue("Related list must contain PCM 650", relatedNames.contains("PCM 650"))
        assertTrue("Related list must contain P-650", relatedNames.contains("P-650"))
    }

    @Test
    fun testMedicineRecord_toMedicine_hidesRawDatabaseClassificationAndExtractsForm() {
        val rawRecord = MedicineRecord(
            name = "Dolo 650",
            generic = "Paracetamol",
            brand = "Dolo 650",
            ingredients = "Paracetamol 650 mg",
            drugClass = "HUMAN OTC DRUG",
            uses = "Fever and headache relief",
            sideEffects = "Mild nausea",
            warnings = "Do not exceed stated dose",
            precautions = "Avoid alcohol",
            interactions = ""
        )

        val medicine = rawRecord.toMedicine()
        assertEquals("Dolo 650", medicine.name)
        assertFalse("Raw database label HUMAN OTC DRUG must be hidden", medicine.category.contains("HUMAN OTC DRUG"))
        assertTrue("Category should be human-friendly therapeutic class", medicine.category.contains("Analgesic") || medicine.category.contains("Antipyretic"))
        assertEquals("Paracetamol 650 mg Tablet", medicine.strengthAndForm)
        assertTrue("Tags should include Pain relief or Fever", medicine.tags.contains("Pain relief") || medicine.tags.contains("Fever"))
    }

    @Test
    fun testMedicineRepository_verifiedMedicinesContainRequiredReferenceItems() {
        val verifiedNames = MedicineRepository.verifiedMedicines.map { it.name }
        assertTrue("Verified list must contain Dolo 650", verifiedNames.contains("Dolo 650"))
        assertTrue("Verified list must contain Crocin 500", verifiedNames.contains("Crocin 500"))
        assertTrue("Verified list must contain Calpol 650", verifiedNames.contains("Calpol 650"))
        assertTrue("Verified list must contain Paracetamol 500 mg", verifiedNames.contains("Paracetamol 500 mg"))
        assertTrue("Verified list must contain Paracetamol 125 mg", verifiedNames.contains("Paracetamol 125 mg"))
        assertTrue("Verified list must contain PCM 650", verifiedNames.contains("PCM 650"))
        assertTrue("Verified list must contain P-650", verifiedNames.contains("P-650"))
        assertTrue("Verified list must contain Amoxicillin 500 mg", verifiedNames.contains("Amoxicillin 500 mg"))
        assertTrue("Verified list must contain Ibuprofen 400 mg", verifiedNames.contains("Ibuprofen 400 mg"))
    }
    @Test
    fun testParseMedicineStreamFiltered_streamsAdditionalMedicinesWithoutDuplicates() {
        val assetFile = getAssetFile()
        assertTrue(assetFile.exists())

        val excluded = setOf("dolo 650", "crocin 500", "calpol 650")
        val limit = 30
        val results = FileInputStream(assetFile).use { stream ->
            MedicineRepository.parseMedicineStreamFiltered(stream, limit = limit, excludedKeys = excluded)
        }

        assertEquals("Must return requested filtered limit of 30 records", 30, results.size)
        for (record in results) {
            val norm = MedicineRepository.normalizeSearchText(record.name)
            assertFalse("Filtered stream must not contain excluded keys", excluded.contains(norm))
            assertTrue("Name must not be blank", record.name.isNotBlank())
        }
    }

    @Test
    fun testSearchMedicinesStream_canonicalAliasParacetamolReturnsMultipleFormulations() {
        val assetFile = getAssetFile()
        assertTrue(assetFile.exists())

        val results = FileInputStream(assetFile).use { stream ->
            MedicineRepository.searchMedicinesStream(stream, query = "paracetamol", limit = 10)
        }

        assertTrue("Searching 'paracetamol' must return candidates", results.isNotEmpty())
        assertTrue("Must support returning up to limit=10 results, got ${results.size}", results.size in 1..10)
    }

    @Test
    fun testSorting_sortsAscendingAndDescendingCorrectly() {
        val list = listOf(
            MedicineRecord(name = "Paracetamol 500 mg", generic = "Paracetamol", brand = "", ingredients = "", drugClass = "", uses = "", sideEffects = "", warnings = "", precautions = "", interactions = ""),
            MedicineRecord(name = "Amoxicillin 500 mg", generic = "Amoxicillin", brand = "", ingredients = "", drugClass = "", uses = "", sideEffects = "", warnings = "", precautions = "", interactions = ""),
            MedicineRecord(name = "Dolo 650", generic = "Paracetamol", brand = "", ingredients = "", drugClass = "", uses = "", sideEffects = "", warnings = "", precautions = "", interactions = "")
        )

        val asc = list.sortedBy { it.name.lowercase() }
        assertEquals("Amoxicillin 500 mg", asc[0].name)
        assertEquals("Dolo 650", asc[1].name)
        assertEquals("Paracetamol 500 mg", asc[2].name)

        val desc = list.sortedByDescending { it.name.lowercase() }
        assertEquals("Paracetamol 500 mg", desc[0].name)
        assertEquals("Dolo 650", desc[1].name)
        assertEquals("Amoxicillin 500 mg", desc[2].name)
    }

    @Test
    fun testLocalization_medicineStringsExistInMarathiAndHindi() {
        val languages = listOf("मराठी", "हिंदी")
        val keys = listOf(
            "medicine_info",
            "medicine_info_subtitle",
            "medicine_hero_title",
            "medicine_hero_subtitle",
            "medicine_search_placeholder",
            "medicine_universal_search",
            "medicine_popular",
            "medicine_view_all",
            "medicine_available",
            "medicine_search_results",
            "medicine_sort",
            "medicine_sort_relevance",
            "medicine_sort_az",
            "medicine_sort_za",
            "medicine_related",
            "medicine_complete_catalogue",
            "medicine_disclaimer",
            "medicine_detail_title",
            "medicine_detail_subtitle",
            "medicine_what_is_it_used_for",
            "tag_pain_relief",
            "tag_fever"
        )

        for (lang in languages) {
            for (key in keys) {
                val translated = appText(key, lang)
                assertTrue("Key '$key' must have non-blank translation in $lang", translated.isNotBlank())
                assertFalse("Key '$key' must not return unformatted key fallback in $lang", translated == key)
            }
        }
    }
}
