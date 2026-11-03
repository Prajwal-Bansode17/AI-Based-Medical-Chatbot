package com.example.ai_based_medical_chatbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ui.HealthTipCategory
import ui.HealthTipsRepository

class HealthTipsTest {

    @Test
    fun allTipsArePopulatedAcrossAllFiveCategories() {
        val allTips = HealthTipsRepository.getAllTips()
        assertTrue("Total tips should be at least 20", allTips.size >= 20)

        val nutritionTips = HealthTipsRepository.getTipsByCategory(HealthTipCategory.NUTRITION)
        val fitnessTips = HealthTipsRepository.getTipsByCategory(HealthTipCategory.FITNESS)
        val sleepTips = HealthTipsRepository.getTipsByCategory(HealthTipCategory.SLEEP)
        val hydrationTips = HealthTipsRepository.getTipsByCategory(HealthTipCategory.HYDRATION)
        val mentalTips = HealthTipsRepository.getTipsByCategory(HealthTipCategory.MENTAL)

        assertTrue("Nutrition tips should be at least 4", nutritionTips.size >= 4)
        assertTrue("Fitness tips should be at least 4", fitnessTips.size >= 4)
        assertTrue("Sleep tips should be at least 4", sleepTips.size >= 4)
        assertTrue("Hydration tips should be at least 4", hydrationTips.size >= 4)
        assertTrue("Mental wellness tips should be at least 4", mentalTips.size >= 4)

        val totalCategorized = nutritionTips.size + fitnessTips.size + sleepTips.size + hydrationTips.size + mentalTips.size
        assertEquals("Sum of categories must match total tips", allTips.size, totalCategorized)
    }

    @Test
    fun eachTipContainsValidTitleDescriptionAndBenefitsInThreeLanguages() {
        val languages = listOf("English", "मराठी", "हिंदी")
        val allTips = HealthTipsRepository.getAllTips()

        for (tip in allTips) {
            assertTrue("Tip ID must be positive", tip.id > 0)
            assertNotNull("Category must not be null", tip.category)

            for (lang in languages) {
                val content = tip.getContent(lang)
                assertFalse("Title for tip #${tip.id} in $lang must not be blank", content.title.isBlank())
                assertFalse("Description for tip #${tip.id} in $lang must not be blank", content.description.isBlank())
                assertFalse("Benefits for tip #${tip.id} in $lang must not be blank", content.benefits.isBlank())
            }
        }
    }

    @Test
    fun categoryFilteringBehavesCorrectly() {
        val all = HealthTipsRepository.getTipsByCategory(HealthTipCategory.ALL)
        assertEquals(all.size, HealthTipsRepository.getAllTips().size)

        for (cat in HealthTipCategory.values()) {
            if (cat == HealthTipCategory.ALL) continue
            val filtered = HealthTipsRepository.getTipsByCategory(cat)
            assertTrue("Filtered list for $cat must not be empty", filtered.isNotEmpty())
            assertTrue("All items in filtered list must match category $cat", filtered.all { it.category == cat })
        }
    }

    @Test
    fun metadataAndDisclaimersAreAvailableInThreeLanguages() {
        val languages = listOf("English", "मराठी", "हिंदी")

        for (lang in languages) {
            assertFalse("Header title in $lang must not be blank", HealthTipsRepository.getHeaderTitle(lang).isBlank())
            assertFalse("Header subtitle in $lang must not be blank", HealthTipsRepository.getHeaderSubtitle(lang).isBlank())
            assertFalse("Hero title in $lang must not be blank", HealthTipsRepository.getHeroTitle(lang).isBlank())
            assertFalse("Hero desc in $lang must not be blank", HealthTipsRepository.getHeroDescription(lang).isBlank())
            assertFalse("Daily title in $lang must not be blank", HealthTipsRepository.getDailySectionTitle(lang).isBlank())
            assertFalse("Count label in $lang must not be blank", HealthTipsRepository.getCountLabel(lang).isBlank())
            assertFalse("Benefits label in $lang must not be blank", HealthTipsRepository.getBenefitsLabel(lang).isBlank())
            assertFalse("Disclaimer in $lang must not be blank", HealthTipsRepository.getDisclaimer(lang).isBlank())

            for (cat in HealthTipCategory.values()) {
                val catTitle = HealthTipsRepository.getCategoryTitle(cat, lang)
                assertFalse("Category title for $cat in $lang must not be blank", catTitle.isBlank())
            }
        }
    }
}
