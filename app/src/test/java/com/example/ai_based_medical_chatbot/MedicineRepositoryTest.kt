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
import ui.extractStrengthAndForm
import ui.humanReadableCategory
import ui.extractStrength
import ui.extractDosageForm
import ui.extractPrescriptionClassification
import com.example.ai_based_medical_chatbot.data.api.PredictionRequest
import com.google.gson.Gson
import org.junit.Assert.assertNull
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
@Test
    fun testGetMedicines_defaultZeroLimitIsBoundedToFifty() {
        val assetFile = getAssetFile()
        assertTrue(assetFile.exists())

        // Previously limit=0 loaded all 16,827 items causing 134MB OutOfMemoryError.
        // It must now safely bound to 50 items.
        val results = FileInputStream(assetFile).use { stream ->
            MedicineRepository.getMedicines(stream, limit = 0)
        }

        assertEquals("Limit=0 must be safely bounded to 50 records to prevent OOM", 50, results.size)
    }

    @Test
    fun testPredictionRequest_serializationIncludesImageAndOcrFields() {
        val req = PredictionRequest(
            sessionId = "test_sess_123",
            text = "What is this medicine?",
            imageBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
            ocrText = "Paracetamol Tablets IP 500 mg"
        )

        val json = Gson().toJson(req)
        assertTrue("JSON must contain session_id", json.contains("session_id"))
        assertTrue("JSON must contain text", json.contains("What is this medicine?"))
        assertTrue("JSON must contain image_base64", json.contains("image_base64"))
        assertTrue("JSON must contain ocr_text", json.contains("Paracetamol Tablets IP 500 mg"))
    }

    @Test
    fun testFindMedicine_verifiedMedicineFastLookup() {
        // Find Dolo 650
        val dolo = MedicineRepository.verifiedMedicines.firstOrNull {
            MedicineRepository.normalizeSearchText(it.name) == "dolo 650"
        }
        assertNotNull("Dolo 650 must exist in verifiedMedicines", dolo)
        assertEquals("Paracetamol", dolo!!.generic)

        // Find Crocin 500
        val crocin = MedicineRepository.verifiedMedicines.firstOrNull {
            MedicineRepository.normalizeSearchText(it.name) == "crocin 500"
        }
        assertNotNull("Crocin 500 must exist in verifiedMedicines", crocin)
        assertEquals("Paracetamol", crocin!!.generic)
    }

    @Test
    fun testSearchMedicinesStream_rapidSearchDoesNotExhaustMemory() {
        val assetFile = getAssetFile()
        assertTrue(assetFile.exists())

        val queries = listOf("aspirin", "amoxicillin", "cetirizine", "vitamin", "syrup")
        for (q in queries) {
            val start = System.currentTimeMillis()
            val results = FileInputStream(assetFile).use { stream ->
                MedicineRepository.searchMedicinesStream(stream, query = q, limit = 10)
            }
            val elapsed = System.currentTimeMillis() - start
            assertTrue("Results for $q must be bounded to at most 10", results.size <= 10)
            assertTrue("Stream search for $q must complete under 3500ms, took ${elapsed}ms", elapsed < 3500)
        }
    }

    @Test
    fun testVerifiedMedicines_expandedEssentialList() {
        val verifiedNames = MedicineRepository.verifiedMedicines.map { it.name }
        assertTrue("Must contain Dolo 650", verifiedNames.contains("Dolo 650"))
        assertTrue("Must contain Crocin 500", verifiedNames.contains("Crocin 500"))
        assertTrue("Must contain Paracetamol 500 mg", verifiedNames.contains("Paracetamol 500 mg"))
        assertTrue("Must contain Ibuprofen 400 mg", verifiedNames.contains("Ibuprofen 400 mg"))
        assertTrue("Must contain Amoxicillin 500 mg", verifiedNames.contains("Amoxicillin 500 mg"))
        assertTrue("Must contain Cetirizine 10 mg", verifiedNames.contains("Cetirizine 10 mg"))
        assertTrue("Must contain Omeprazole 20 mg", verifiedNames.contains("Omeprazole 20 mg"))
        assertTrue("Must contain Metformin 500 mg", verifiedNames.contains("Metformin 500 mg"))
        assertTrue("Must contain Aspirin 75 mg", verifiedNames.contains("Aspirin 75 mg"))
        assertTrue("Must contain Azithromycin 500 mg", verifiedNames.contains("Azithromycin 500 mg"))
        assertTrue("Must contain Pantoprazole 40 mg", verifiedNames.contains("Pantoprazole 40 mg"))
        assertTrue("Must contain Atorvastatin 10 mg", verifiedNames.contains("Atorvastatin 10 mg"))
        assertTrue("Must contain ORS", verifiedNames.any { it.contains("ORS") })
        assertTrue("Verified list must have at least 20 items to guarantee clean unsearched initial catalogue", verifiedNames.size >= 20)
    }

    @Test
    fun testIsMalformedOrNonMedicinal_filtersLaboratoryAndPureNumbers() {
        assertTrue("Laboratory use must be filtered", MedicineRepository.isMalformedOrNonMedicinal("0.9% Benzyl Alcohol Solution (Laboratory Use Only)"))
        assertTrue("Laboratory use in uses must be filtered", MedicineRepository.isMalformedOrNonMedicinal("Chemical X", "", "", "For laboratory use only"))
        assertTrue("Pure number name must be filtered", MedicineRepository.isMalformedOrNonMedicinal("1168"))
        assertTrue("Blank name must be filtered", MedicineRepository.isMalformedOrNonMedicinal("   "))
        assertTrue("Hand sanitizer must be filtered", MedicineRepository.isMalformedOrNonMedicinal("Instant Hand Sanitizer"))
        assertTrue("Hand soap must be filtered", MedicineRepository.isMalformedOrNonMedicinal("1438 DynaSoap"))

        assertFalse("Real patch must NOT be filtered", MedicineRepository.isMalformedOrNonMedicinal("0.025% Capsaicin Pain relief Patch"))
        assertFalse("Real lidocaine must NOT be filtered", MedicineRepository.isMalformedOrNonMedicinal("0.5% LIDOCAINE HCl"))
        assertFalse("Real sodium chloride must NOT be filtered", MedicineRepository.isMalformedOrNonMedicinal("0.9% SODIUM CHLORIDE"))
        assertFalse("Real hydrocortisone must NOT be filtered", MedicineRepository.isMalformedOrNonMedicinal("1% Hydrocortisone Anti-itch"))
        assertFalse("Real paracetamol must NOT be filtered", MedicineRepository.isMalformedOrNonMedicinal("Paracetamol 500 mg"))
    }

    @Test
    fun testExtractStrengthAndForm_accurateFormsNotDefaultTablet() {
        val capsaicinForm = extractStrengthAndForm("0.025% Capsaicin Pain relief Patch", "CAPSAICIN", "", "HUMAN OTC DRUG")
        assertEquals("0.025% Transdermal Patch", capsaicinForm)
        assertFalse("Capsaicin patch must not be classified as Capsule", capsaicinForm.contains("Capsule"))

        val lidocaineForm = extractStrengthAndForm("0.5% LIDOCAINE HCl", "LIDOCAINE HCL", "", "HUMAN PRESCRIPTION DRUG", "Lidocaine Hydrochloride Injection, USP")
        assertTrue("Lidocaine injection must be recognized as Injection", lidocaineForm.contains("Injection"))
        assertFalse("Lidocaine injection must not be classified as Tablet", lidocaineForm.contains("Tablet"))

        val sodiumChlorideForm = extractStrengthAndForm("0.9% SODIUM CHLORIDE", "0.9% SODIUM CHLORIDE", "", "HUMAN PRESCRIPTION DRUG", "parenteral preparation for intravenous infusion")
        assertTrue("Sodium chloride infusion must be recognized as IV Infusion / Injection", sodiumChlorideForm.contains("Injection") || sodiumChlorideForm.contains("Infusion"))
        assertFalse("Sodium chloride must not be classified as Tablet", sodiumChlorideForm.contains("Tablet"))

        val shampooForm = extractStrengthAndForm("1% ketoconazole anti dandruff", "1% KETOCONAZOLE ANTI DANDRUFF SHAMPOO", "", "HUMAN OTC DRUG")
        assertEquals("1% Medicated Shampoo", shampooForm)

        val doloForm = extractStrengthAndForm("Dolo 650", "Paracetamol", "Paracetamol 650 mg", "Analgesic")
        assertEquals("Paracetamol 650 mg Tablet", doloForm)
    }

    @Test
    fun testHumanReadableCategory_accurateTherapeuticClasses() {
        val lidoCat = humanReadableCategory("HUMAN PRESCRIPTION DRUG", "", "local or regional anesthesia", "0.5% LIDOCAINE HCL", "0.5% LIDOCAINE HCl")
        assertEquals("Local Anesthetic", lidoCat)

        val capsaicinCat = humanReadableCategory("HUMAN OTC DRUG", "", "temporary relief of minor aches and pains", "CAPSAICIN", "0.025% Capsaicin Pain relief Patch")
        assertEquals("Topical Pain Relief", capsaicinCat)

        val hydroCat = humanReadableCategory("HUMAN OTC DRUG", "", "temporary relief of itching", "HYDROCORTISONE", "1% Hydrocortisone Anti-itch")
        assertEquals("Corticosteroid & Anti-inflammatory", hydroCat)

        val naclCat = humanReadableCategory("HUMAN PRESCRIPTION DRUG", "", "diluting or reconstituting drugs for intravenous injection", "SODIUM CHLORIDE", "0.9% SODIUM CHLORIDE")
        assertEquals("Electrolyte & Fluid Replenishment", naclCat)

        val azelaicCat = humanReadableCategory("HUMAN OTC DRUG", "", "For the treatment of acne.", "AZELAIC ACID CREAM", "10% Azelaic Acid")
        assertEquals("Dermatological Care", azelaicCat)
        assertFalse("Azelaic acid must not be miscategorized as Acidity Reducer", azelaicCat.contains("Acidity"))

        val paraCat = humanReadableCategory("Analgesic & Antipyretic", "Paracetamol 500 mg", "Fever and headache", "Paracetamol", "Paracetamol 500 mg")
        assertEquals("Analgesic & Antipyretic", paraCat)
    }

    @Test
    fun testCleanedDataset_hasNoLaboratoryChemicalsOrPureNumbers() {
        val assetFile = getAssetFile()
        assertTrue(assetFile.exists())

        val allRecords = FileInputStream(assetFile).use { stream ->
            MedicineRepository.parseMedicineStream(stream, limit = 0)
        }

        assertTrue("Cleaned dataset must have valid records (> 15000)", allRecords.size > 15000)
        for (r in allRecords) {
            val combined = "${r.name} ${r.generic} ${r.uses}".lowercase()
            assertFalse("Must not contain laboratory use only (${r.name})", combined.contains("laboratory use only"))
            assertFalse("Must not contain pure number names (${r.name})", r.name.trim().all { it.isDigit() })
            assertFalse("Must not contain hand sanitizer (${r.name})", r.name.lowercase().contains("hand sanitizer"))
        }
    }

    @Test
    fun testToMedicine_mapsSpecificationsAndPrescriptionAccurately() {
        val record = MedicineRecord(
            name = "Amoxicillin 500 mg",
            generic = "Amoxicillin Trihydrate",
            brand = "Amoxil",
            ingredients = "Amoxicillin 500 mg",
            drugClass = "HUMAN PRESCRIPTION DRUG",
            uses = "Bacterial infections\nEar infection",
            sideEffects = "Mild diarrhea\nNausea",
            warnings = "Hypersensitivity warning",
            precautions = "Take full course\nConsult if allergic to penicillin",
            interactions = "Methotrexate\nOral contraceptives"
        )

        val medicine = record.toMedicine()

        assertEquals("Amoxicillin 500 mg", medicine.name)
        assertEquals("Amoxicillin Trihydrate", medicine.genericName)
        assertEquals("Amoxil", medicine.brandName)
        assertEquals("500 mg", medicine.strength)
        // In this test case, Amoxicillin 500 mg has no explicit form keyword, so dosageForm is empty
        assertEquals("", medicine.dosageForm)
        assertEquals(true, medicine.isPrescription)
        assertEquals(2, medicine.uses.size)
        assertEquals("Bacterial infections", medicine.uses[0])
        assertEquals(2, medicine.commonSideEffects.size)
        assertEquals(2, medicine.precautions.size)
        assertEquals(2, medicine.interactions.size)
        assertEquals("Methotrexate", medicine.interactions[0])
        assertEquals("Hypersensitivity warning", medicine.warning)
    }

    @Test
    fun testToMedicine_neverFabricatesMissingData() {
        val record = MedicineRecord(
            name = "Minimal Medicine",
            generic = "",
            brand = "",
            ingredients = "",
            drugClass = "",
            uses = "",
            sideEffects = "",
            warnings = "",
            precautions = "",
            interactions = ""
        )

        val medicine = record.toMedicine()

        assertEquals("Minimal Medicine", medicine.name)
        assertEquals("", medicine.genericName)
        assertEquals("", medicine.brandName)
        assertEquals("", medicine.strength)
        assertEquals("", medicine.dosageForm)
        assertNull("Ambiguous prescription status must be null", medicine.isPrescription)
        assertTrue("Uses must be empty when not present in record", medicine.uses.isEmpty())
        assertTrue("Side effects must be empty when not present in record", medicine.commonSideEffects.isEmpty())
        assertTrue("Precautions must be empty when not present in record", medicine.precautions.isEmpty())
        assertTrue("Interactions must be empty when not present in record", medicine.interactions.isEmpty())
        assertEquals("", medicine.warning)
    }

    @Test
    fun testExtractPrescriptionClassification_accurateClassification() {
        val rx1 = extractPrescriptionClassification("HUMAN PRESCRIPTION DRUG", "Amoxicillin", "Amoxicillin")
        assertEquals(true, rx1)

        val rx2 = extractPrescriptionClassification("Antibiotic", "Ciprofloxacin 500 mg", "Ciprofloxacin")
        assertEquals(true, rx2)

        val otc1 = extractPrescriptionClassification("HUMAN OTC DRUG", "Paracetamol", "Paracetamol")
        assertEquals(false, otc1)

        val otc2 = extractPrescriptionClassification("Analgesic", "Dolo 650", "Paracetamol")
        assertEquals(false, otc2)

        val ambiguous = extractPrescriptionClassification("Herbal Supplement", "Unknown Extract", "Extract")
        assertNull("Unspecified non-standard class must yield null", ambiguous)
    }

    @Test
    fun testExtractStrengthAndDosageForm_accurateParsing() {
        assertEquals("650 mg", extractStrength("Dolo 650 mg", "Paracetamol", "Paracetamol 650 mg"))
        assertEquals("0.5%", extractStrength("0.5% LIDOCAINE HCl", "LIDOCAINE HCL", ""))
        assertEquals("Tablet", extractDosageForm("Paracetamol 500 mg Tablet", "Paracetamol", "", "Analgesic", "For pain"))
        assertEquals("Transdermal Patch", extractDosageForm("0.025% Capsaicin Pain relief Patch", "Capsaicin", "", "OTC", ""))
        assertEquals("IV Infusion", extractDosageForm("0.9% Sodium Chloride IV Infusion", "Sodium Chloride", "", "Prescription", "intravenous infusion"))
    }
}
