package com.example.ai_based_medical_chatbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ui.HealthTipCategory
import ui.HealthTipsRecommendations
import ui.HealthTipsRepository
import ui.WellnessConcernType

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

    @Test
    fun allTwentyTwoTipsHaveCompleteDetailedContentInThreeLanguages() {
        val languages = listOf("English", "मराठी", "हिंदी")
        val allTips = HealthTipsRepository.getAllTips()
        assertEquals(22, allTips.size)

        for (tip in allTips) {
            val bundle = HealthTipsRecommendations.getTipDetails(tip.id)
            assertNotNull("Detail bundle for tip #${tip.id} must exist", bundle)

            for (lang in languages) {
                val details = bundle!!.getLocalized(lang)
                assertFalse("Why useful for #${tip.id} in $lang must not be blank", details.whyUseful.isBlank())
                assertTrue("Target audience for #${tip.id} in $lang must have >= 2 items", details.targetAudience.size >= 2)
                details.targetAudience.forEach {
                    assertFalse("Target audience item in $lang must not be blank", it.isBlank())
                }

                assertTrue("Relevant concerns for #${tip.id} in $lang must have >= 1 items", details.relevantConcerns.isNotEmpty())
                details.relevantConcerns.forEach {
                    assertFalse("Relevant concern in $lang must not be blank", it.isBlank())
                }

                assertTrue("Personalized suggestions for #${tip.id} in $lang must have >= 3 items", details.personalizedSuggestions.size >= 3)
                details.personalizedSuggestions.forEach {
                    assertFalse("Personalized suggestion in $lang must not be blank", it.isBlank())
                }

                assertTrue("Detailed benefits for #${tip.id} in $lang must have >= 3 items", details.detailedBenefits.size >= 3)
                details.detailedBenefits.forEach {
                    assertFalse("Detailed benefit in $lang must not be blank", it.isBlank())
                }

                assertTrue("Precautions for #${tip.id} in $lang must have >= 2 items", details.precautions.size >= 2)
                details.precautions.forEach {
                    assertFalse("Precaution in $lang must not be blank", it.isBlank())
                }

                assertFalse("When to consult doctor for #${tip.id} in $lang must not be blank", details.whenToConsultDoctor.isBlank())
            }
        }
    }

    @Test
    fun relatedTipsAreValidAndExist() {
        val allTips = HealthTipsRepository.getAllTips()
        for (tip in allTips) {
            val related = HealthTipsRecommendations.getRelatedTips(tip.id)
            assertTrue("Tip #${tip.id} must have >= 2 related tips", related.size >= 2)
            assertTrue("Related tips must not include the tip itself", related.none { it.id == tip.id })
            related.forEach { rel ->
                assertNotNull("Related tip must exist in repository", HealthTipsRepository.getTipById(rel.id))
            }
        }
    }

    @Test
    fun wellnessConcernsSystemIsComplete() {
        val concerns = HealthTipsRecommendations.getAllWellnessConcerns()
        assertEquals("Must support 8 clinical wellness concern classifications", 8, concerns.size)

        val languages = listOf("English", "मराठी", "हिंदी")

        for (concern in concerns) {
            assertTrue("Concern ${concern.type} must link to >= 2 health tips", concern.relatedTipIds.size >= 2)
            concern.relatedTipIds.forEach { tipId ->
                assertNotNull("Tip #$tipId mapped to concern ${concern.type} must exist", HealthTipsRepository.getTipById(tipId))
            }

            for (lang in languages) {
                val loc = concern.getLocalized(lang)
                assertFalse("Concern name in $lang must not be blank", loc.name.isBlank())
                assertFalse("Concern description in $lang must not be blank", loc.description.isBlank())
                assertFalse("Concern whyHelpful in $lang must not be blank", loc.whyHelpful.isBlank())
                assertFalse("Concern precautions in $lang must not be blank", loc.precautions.isBlank())
                assertFalse("Concern medicalGuidance in $lang must not be blank", loc.medicalGuidance.isBlank())
                assertTrue("Practical suggestions in $lang must have >= 3 items", loc.practicalSuggestions.size >= 3)
                loc.practicalSuggestions.forEach {
                    assertFalse("Practical suggestion in $lang must not be blank", it.isBlank())
                }
            }
        }
    }

    @Test
    fun detailScreenLabelsAreCompleteInAllThreeLanguages() {
        val languages = listOf("English", "मराठी", "हिंदी")
        val requiredKeys = listOf(
            "overview_title",
            "why_useful_label",
            "who_useful_title",
            "target_audience_label",
            "relevant_concerns_label",
            "personalized_suggestions_title",
            "suggestions_note",
            "benefits_title",
            "precautions_title",
            "when_consult_title",
            "emergency_alert",
            "related_tips_title",
            "tip_details_screen_title",
            "view_detail_action",
            "disclaimer_banner"
        )

        for (lang in languages) {
            val labels = HealthTipsRecommendations.getDetailLabels(lang)
            for (key in requiredKeys) {
                val value = labels[key]
                assertNotNull("Label '$key' must exist for language $lang", value)
                assertFalse("Label '$key' must not be blank for language $lang", value!!.isBlank())
            }
        }
    }
}
