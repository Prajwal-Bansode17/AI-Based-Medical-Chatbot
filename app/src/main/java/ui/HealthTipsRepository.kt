package ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 5 Core Categories required for MedAssist AI Health Tips:
 * Nutrition, Fitness & Exercise, Sleep & Rest, Hydration, Mental Wellness.
 * Plus ALL for unfiltered browsing.
 */
enum class HealthTipCategory(val id: String) {
    ALL("all"),
    NUTRITION("nutrition"),
    FITNESS("fitness"),
    SLEEP("sleep"),
    HYDRATION("hydration"),
    MENTAL("mental")
}

data class LocalizedTipContent(
    val title: String,
    val description: String,
    val benefits: String
)

data class HealthTipItem(
    val id: Int,
    val category: HealthTipCategory,
    val english: LocalizedTipContent,
    val marathi: LocalizedTipContent,
    val hindi: LocalizedTipContent
) {
    fun getContent(language: String): LocalizedTipContent {
        return when (language.lowercase().trim()) {
            "मराठी", "mr", "marathi" -> marathi
            "हिंदी", "hi", "hindi" -> hindi
            else -> english
        }
    }
}

val WaterDropVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "WaterDropVectorIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color(0xFF0F766E)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 2.7f)
            curveTo(12f, 2.7f, 5.5f, 10.2f, 5.5f, 14.5f)
            curveTo(5.5f, 18.09f, 8.41f, 21f, 12f, 21f)
            curveTo(15.59f, 21f, 18.5f, 18.09f, 18.5f, 14.5f)
            curveTo(18.5f, 10.2f, 12f, 2.7f, 12f, 2.7f)
            close()
        }
    }.build()
}

val NutritionVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "NutritionVectorIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color(0xFF0F766E)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 5f)
            curveTo(12f, 2.5f, 14f, 2f, 15f, 2f)
            moveTo(12f, 5f)
            curveTo(9.5f, 2.5f, 5f, 3.5f, 4f, 7.5f)
            curveTo(3f, 12.5f, 6f, 19f, 10f, 21.5f)
            curveTo(11.5f, 22.5f, 12.5f, 22.5f, 14f, 21.5f)
            curveTo(18f, 19f, 21f, 12.5f, 20f, 7.5f)
            curveTo(19f, 3.5f, 14.5f, 2.5f, 12f, 5f)
            close()
        }
    }.build()
}

val MindWellnessVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "MindWellnessVectorIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color(0xFF0F766E)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 4f)
            curveTo(10f, 8f, 10f, 14f, 12f, 19f)
            curveTo(14f, 14f, 14f, 8f, 12f, 4f)
            close()
            moveTo(12f, 19f)
            curveTo(7f, 17f, 4f, 12f, 5f, 8f)
            curveTo(8f, 9f, 11f, 13f, 12f, 19f)
            close()
            moveTo(12f, 19f)
            curveTo(17f, 17f, 20f, 12f, 19f, 8f)
            curveTo(16f, 9f, 13f, 13f, 12f, 19f)
            close()
        }
    }.build()
}

val BenefitCheckVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "BenefitCheckVectorIcon",
        defaultWidth = 16.dp,
        defaultHeight = 16.dp,
        viewportWidth = 16f,
        viewportHeight = 16f
    ).apply {
        path(
            stroke = SolidColor(Color(0xFF0F766E)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(3f, 8.5f)
            lineTo(6.5f, 12f)
            lineTo(13f, 4.5f)
        }
    }.build()
}

object HealthTipsRepository {

    fun getCategoryIcon(category: HealthTipCategory): ImageVector {
        return when (category) {
            HealthTipCategory.ALL -> HeartPulseVectorIcon
            HealthTipCategory.NUTRITION -> NutritionVectorIcon
            HealthTipCategory.FITNESS -> HeartPulseVectorIcon
            HealthTipCategory.SLEEP -> ClockVectorIcon
            HealthTipCategory.HYDRATION -> WaterDropVectorIcon
            HealthTipCategory.MENTAL -> MindWellnessVectorIcon
        }
    }

    fun getCategoryTitle(category: HealthTipCategory, language: String): String {
        return when (language.lowercase().trim()) {
            "मराठी", "mr", "marathi" -> when (category) {
                HealthTipCategory.ALL -> "सर्व टिप्स"
                HealthTipCategory.NUTRITION -> "पोषण व आहार"
                HealthTipCategory.FITNESS -> "व्यायाम व फिटनेस"
                HealthTipCategory.SLEEP -> "झोप व विश्रांती"
                HealthTipCategory.HYDRATION -> "हायड्रेशन व पाणी"
                HealthTipCategory.MENTAL -> "मानसिक स्वास्थ्य"
            }
            "हिंदी", "hi", "hindi" -> when (category) {
                HealthTipCategory.ALL -> "सभी सुझाव"
                HealthTipCategory.NUTRITION -> "पोषण एवं आहार"
                HealthTipCategory.FITNESS -> "व्यायाम एवं फिटनेस"
                HealthTipCategory.SLEEP -> "नींद एवं विश्राम"
                HealthTipCategory.HYDRATION -> "जलयोजन एवं पानी"
                HealthTipCategory.MENTAL -> "मानसिक स्वास्थ्य"
            }
            else -> when (category) {
                HealthTipCategory.ALL -> "All Tips"
                HealthTipCategory.NUTRITION -> "Nutrition & Diet"
                HealthTipCategory.FITNESS -> "Fitness & Exercise"
                HealthTipCategory.SLEEP -> "Sleep & Rest"
                HealthTipCategory.HYDRATION -> "Hydration & Water"
                HealthTipCategory.MENTAL -> "Mental Wellness"
            }
        }
    }

    fun getHeaderTitle(language: String): String {
        return when (language.lowercase().trim()) {
            "मराठी", "mr", "marathi" -> "आरोग्य टिप्स"
            "हिंदी", "hi", "hindi" -> "स्वास्थ्य सुझाव"
            else -> "Health Tips"
        }
    }

    fun getHeaderSubtitle(language: String): String {
        return when (language.lowercase().trim()) {
            "मराठी", "mr", "marathi" -> "दैनंदिन आरोग्य, जीवनशैली व प्रतिबंधात्मक सवयी"
            "हिंदी", "hi", "hindi" -> "दैनिक कल्याण, जीवनशैली और निवारक आदतें"
            else -> "Daily wellness, lifestyle and preventive habits"
        }
    }

    fun getHeroTitle(language: String): String {
        return when (language.lowercase().trim()) {
            "मराठी", "mr", "marathi" -> "दैनिक आरोग्य साथी"
            "हिंदी", "hi", "hindi" -> "दैनिक स्वास्थ्य साथी"
            else -> "Daily Wellness Companion"
        }
    }

    fun getHeroDescription(language: String): String {
        return when (language.lowercase().trim()) {
            "मराठी", "mr", "marathi" -> "डॉक्टरांनी शिफारस केलेल्या, शास्त्रीय सवयी ज्या तुमच्या शारीरिक आरोग्याला व प्रतिकारशक्तीला मजबूत ठेवतात."
            "हिंदी", "hi", "hindi" -> "डॉक्टरों द्वारा समर्थित, वैज्ञानिक आदतें जो आपकी शारीरिक ऊर्जा, रोग प्रतिरोधक क्षमता और दैनिक स्वास्थ्य को बेहतर बनाती हैं।"
            else -> "Doctor-aligned, evidence-informed habits to nurture your physical vitality, immunity, and everyday wellbeing."
        }
    }

    fun getDailySectionTitle(language: String): String {
        return when (language.lowercase().trim()) {
            "मराठी", "mr", "marathi" -> "दैनंदिन आरोग्य सवयी"
            "हिंदी", "hi", "hindi" -> "दैनिक स्वस्थ आदतें"
            else -> "Daily Healthy Habits"
        }
    }

    fun getDailySectionSubtitle(language: String): String {
        return when (language.lowercase().trim()) {
            "मराठी", "mr", "marathi" -> "सदाबहार आरोग्यासाठी सोप्या पायऱ्या"
            "हिंदी", "hi", "hindi" -> "दीर्घकालिक ऊर्जा और स्वास्थ्य के लिए सरल कदम"
            else -> "Simple steps for lasting vitality"
        }
    }

    fun getCountLabel(language: String): String {
        return when (language.lowercase().trim()) {
            "मराठी", "mr", "marathi" -> "सवयी"
            "हिंदी", "hi", "hindi" -> "सुझाव"
            else -> "curated habits"
        }
    }

    fun getBenefitsLabel(language: String): String {
        return when (language.lowercase().trim()) {
            "मराठी", "mr", "marathi" -> "आरोग्य फायदा"
            "हिंदी", "hi", "hindi" -> "स्वास्थ्य लाभ"
            else -> "Wellness Benefit"
        }
    }

    fun getDisclaimer(language: String): String {
        return when (language.lowercase().trim()) {
            "मराठी", "mr", "marathi" -> "सूचना: या आरोग्य व वेलनेस टिप्स सामान्य मार्गदर्शन आणि निरोगी सवयींसाठी आहेत. हा वैद्यकीय निदान किंवा उपचारांचा पर्याय नाही. कोणत्याही शारीरिक त्रासासाठी नेहमी तज्ज्ञ डॉक्टरांचा सल्ला घ्या."
            "हिंदी", "hi", "hindi" -> "अस्वीकरण: ये स्वास्थ्य और कल्याणकारी सुझाव सामान्य मार्गदर्शन और स्वस्थ आदतों के लिए हैं। यह किसी चिकित्सीय निदान या उपचार का विकल्प नहीं हैं। किसी भी स्वास्थ्य समस्या के लिए योग्य चिकित्सक से परामर्श लें।"
            else -> "Disclaimer: These wellness tips are curated for informational guidance and healthy habits. They do not substitute clinical diagnosis or personalized medical treatment. Always consult a qualified physician."
        }
    }

    fun getEmptyStateMessage(language: String): String {
        return when (language.lowercase().trim()) {
            "मराठी", "mr", "marathi" -> "या वर्गवारीत सध्या कोणत्याही टिप्स उपलब्ध नाहीत."
            "हिंदी", "hi", "hindi" -> "इस श्रेणी में फिलहाल कोई सुझाव उपलब्ध नहीं हैं।"
            else -> "No health tips available in this category."
        }
    }

    private val allTips: List<HealthTipItem> = listOf(
        // ==========================================
        // 1. NUTRITION & DIET (5 Tips)
        // ==========================================
        HealthTipItem(
            id = 1,
            category = HealthTipCategory.NUTRITION,
            english = LocalizedTipContent(
                title = "Balanced Macronutrient Plate",
                description = "Structure every meal using the plate method: half of your plate filled with colorful vegetables and raw greens, one quarter with high-quality lean protein (beans, lentils, eggs, or paneer), and one quarter with complex whole grains.",
                benefits = "Stabilizes post-meal blood glucose, supports sustained metabolic energy, and provides essential micronutrients for cellular repair."
            ),
            marathi = LocalizedTipContent(
                title = "संतुलित थाळी आहार पद्धती",
                description = "प्रत्येक जेवणात थाळी पद्धतीचा वापर करा: अर्धी थाळी रंगीबेरंगी ताज्या भाज्या व सॅलड, पाव भाग सकस प्रथिने (डाळी, कडधान्ये, अंडी किंवा पनीर), आणि पाव भाग संपूर्ण तृणधान्ये (ज्वारी, बाजरी, ब्राऊन राईस) असावी.",
                benefits = "रक्तातील साखरेची पातळी नियंत्रित राहते, शरीराला दिवसभर ऊर्जा मिळते आणि पेशींच्या पुनरुज्जीवनासाठी आवश्यक पोषण मिळते."
            ),
            hindi = LocalizedTipContent(
                title = "संतुलित थाली आहार नियम",
                description = "प्रत्येक भोजन में थाली के विभाजन का नियम अपनाएं: आधी थाली रंग-बिरंगी सब्जियों और सलाद से, एक-चौथाई पौष्टिक प्रोटीन (दालें, छोले, अंडे या पनीर) से, और एक-चौथाई साबुत अनाज से भरें।",
                benefits = "भोजन के बाद रक्त शर्करा नियंत्रित रहती है, दिनभर सक्रिय ऊर्जा मिलती है और कोशिकाओं की मरम्मत के लिए जरूरी पोषण प्राप्त होता है।"
            )
        ),
        HealthTipItem(
            id = 2,
            category = HealthTipCategory.NUTRITION,
            english = LocalizedTipContent(
                title = "High-Fiber Foods & Gut Health",
                description = "Aim for 25 to 35 grams of daily dietary fiber by incorporating whole oats, flaxseeds, chia seeds, legumes, leafy spinach, and crisp apples into your weekly diet.",
                benefits = "Nourishes healthy gut microbiota, prevents gastrointestinal constipation, and naturally helps bind and lower excess LDL cholesterol."
            ),
            marathi = LocalizedTipContent(
                title = "फायबरयुक्त अन्न व निरोगी पोट",
                description = "दररोज २५ ते ३५ ग्रॅम आहारातील फायबर मिळवण्यासाठी ओट्स, जवस (फ्लॅक्ससीड्स), चिया सीड्स, पालेभाज्या, मोड आलेली कडधान्ये आणि फळांचा आहारात समावेश करा.",
                benefits = "पचनसंस्थेतील चांगल्या बॅक्टेरियांचे पोषण होते, बद्धकोष्ठता टळते आणि शरीरातील वाईट कोलेस्टेरॉल नैसर्गिकरित्या कमी होण्यास मदत होते."
            ),
            hindi = LocalizedTipContent(
                title = "फाइबर युक्त भोजन और आंत स्वास्थ्य",
                description = "प्रतिदिन 25 से 35 ग्राम फाइबर प्राप्त करने के लिए आहार में ओट्स, अलसी, चिया बीज, दालें, पत्तेदार सब्जियां और ताजे फल नियमित रूप से शामिल करें।",
                benefits = "पाचन तंत्र के मित्र बैक्टीरिया स्वस्थ रहते हैं, कब्ज की समस्या दूर होती है और हानिकारक एलडीएल कोलेस्ट्रॉल कम करने में मदद मिलती है।"
            )
        ),
        HealthTipItem(
            id = 3,
            category = HealthTipCategory.NUTRITION,
            english = LocalizedTipContent(
                title = "Minimize Ultra-Processed Sugars",
                description = "Drastically limit carbonated soft drinks, commercial packaged fruit juices, and refined bakery confections. Replace refined sugars with natural seasonal fruits or a pinch of pure jaggery.",
                benefits = "Protects pancreatic beta-cell insulin sensitivity, prevents visceral abdominal fat accumulation, and mitigates systemic inflammation."
            ),
            marathi = LocalizedTipContent(
                title = "प्रक्रिया केलेले गोड पदार्थ टाळा",
                description = "शीतपेये, बाजारातील डबाबंद ज्यूस आणि पांढऱ्या साखरेपासून बनवलेले बेकरी पदार्थ कमी करा. गोड खाण्याची इच्छा झाल्यास ताज्या फळांचा किंवा थोड्या गुळाचा पर्याय निवडा.",
                benefits = "इन्सुलिनची संवेदनशीलता टिकून राहते, पोटावरील चरबी वाढत नाही आणि शरीरातील जुनाट सूज (इन्फ्लेमेशन) कमी होते."
            ),
            hindi = LocalizedTipContent(
                title = "प्रसंस्कृत चीनी और मीठे से परहेज",
                description = "कोल्ड ड्रिंक्स, पैकेज्ड जूस और मैदे-चीनी से बने बेकरी उत्पादों का सेवन बेहद सीमित करें। मीठे की तलब होने पर ताजे फल या थोड़ी मात्रा में गुड़ खाएं।",
                benefits = "इंसुलिन संवेदनशीलता मजबूत होती है, पेट की अतिरिक्त चर्बी रुकती है और शरीर में सूजन व लिवर रोगों का खतरा घटता है।"
            )
        ),
        HealthTipItem(
            id = 4,
            category = HealthTipCategory.NUTRITION,
            english = LocalizedTipContent(
                title = "Mindful Eating & Slow Chewing",
                description = "Dine away from screens and mobile distractions. Chew each bite thoroughly (20-30 times) and stop eating when you feel comfortably 80% satiated.",
                benefits = "Allows satiety hormones (leptin and CCK) to reach the brain, prevents acid reflux, and significantly enhances nutrient bio-absorption."
            ),
            marathi = LocalizedTipContent(
                title = "सावकाश व एकाग्रतेने जेवण्याची सवय",
                description = "मोबाईल किंवा टीव्ही पाहत जेवणे टाळा. प्रत्येक घास २० ते ३० वेळा चांगला चावून खा आणि पोट ८०% भरल्याची जाणीव झाल्यावर जेवणे थांबवा.",
                benefits = "मेंदूला तृप्तीचे संकेत योग्य वेळी मिळतात, ॲसिडिटी व अपचन टळते आणि आहारातील पोषकांचे शरीरात संपूर्ण शोषण होते."
            ),
            hindi = LocalizedTipContent(
                title = "सजग भोजन और अच्छी तरह चबाना",
                description = "मोबाइल या टीवी देखते हुए खाना न खाएं। भोजन के हर निवाले को अच्छी तरह चबाकर खाएं और पेट 80% भरने पर भोजन समाप्त करें।",
                benefits = "मस्तिष्क को समय पर तृप्ति का संदेश मिलता है, एसिडिटी और अपच से बचाव होता है तथा पोषक तत्वों का अवशोषण बेहतर होता है।"
            )
        ),
        HealthTipItem(
            id = 5,
            category = HealthTipCategory.NUTRITION,
            english = LocalizedTipContent(
                title = "Anti-Inflammatory Culinary Spices",
                description = "Incorporate authentic healing spices into daily cooking: turmeric with a pinch of black pepper (enhances curcumin absorption by 2000%), grated ginger, fresh garlic, and Ceylon cinnamon.",
                benefits = "Delivers potent bioactive polyphenol antioxidants, shields vascular linings, and bolsters immune resilience against seasonal infections."
            ),
            marathi = LocalizedTipContent(
                title = "औषधी मसाल्यांचा दैनंदिन वापर",
                description = "रोजच्या स्वयंपाकात हळद (काळ्या मिरीसह - ज्यामुळे करक्युमिनचे शोषण वाढते), आले, लसूण आणि दालचिनी यांसारख्या नैसर्गिक मसाल्यांचा योग्य वापर करा.",
                benefits = "नैसर्गिक ॲन्टीऑक्सिडंट्स मिळतात, सांधे व रक्तवाहिन्यांचे रक्षण होते आणि हंगामी आजारांविरुद्ध रोगप्रतिकारशक्ती सक्षम होते."
            ),
            hindi = LocalizedTipContent(
                title = "सूजनरोधी मसालों का नियमित उपयोग",
                description = "दैनिक भोजन में हल्दी (काली मिर्च के साथ, जिससे करक्यूमिन का अवशोषण बढ़ता है), अदरक, ताजा लहसुन और दालचीनी का प्रयोग करें।",
                benefits = "शक्तिशाली एंटीऑक्सीडेंट सुरक्षा मिलती है, जोड़ों और धमनियों की सूजन कम होती है तथा मौसमी बीमारियों से बचाव होता है।"
            )
        ),

        // ==========================================
        // 2. FITNESS & EXERCISE (5 Tips)
        // ==========================================
        HealthTipItem(
            id = 6,
            category = HealthTipCategory.FITNESS,
            english = LocalizedTipContent(
                title = "150 Minutes Weekly Aerobic Activity",
                description = "Commit to at least 30 minutes of moderate aerobic cardiovascular activity 5 days a week—such as brisk walking at 5 km/h, steady cycling, outdoor swimming, or brisk stair climbing.",
                benefits = "Elevates cardiac stroke volume, decreases resting heart rate, regulates arterial blood pressure, and promotes vascular elasticity."
            ),
            marathi = LocalizedTipContent(
                title = "आठवड्यातून १५० मिनिटे एरोबिक व्यायाम",
                description = "आठवड्यातून किमान ५ दिवस दररोज ३० मिनिटे मध्यम स्वरूपाचा व्यायाम करा—जसे की जलद चालणे, सायकल चालवणे, पोहणे किंवा पायऱ्या चढणे-उतरणे.",
                benefits = "हृदयाची कार्यक्षमता वाढते, रक्तदाब नियंत्रित राहतो, शरीरातील रक्ताभिसरण सुधारते आणि हृदयविकाराचा धोका कमी होतो."
            ),
            hindi = LocalizedTipContent(
                title = "सप्ताह में 150 मिनट मध्यम एरोबिक व्यायाम",
                description = "हफ्ते में कम से कम 5 दिन प्रतिदिन 30 मिनट मध्यम गति का व्यायाम करें—जैसे तेज चलना, साइकिल चलाना, तैराकी या सीढ़ियां चढ़ना।",
                benefits = "हृदय की मांसपेशियां मजबूत होती हैं, रक्तचाप सामान्य रहता है, रक्त संचार सुधरता है और हृदय रोगों का खतरा घटता है।"
            )
        ),
        HealthTipItem(
            id = 7,
            category = HealthTipCategory.FITNESS,
            english = LocalizedTipContent(
                title = "Progressive Resistance & Strength Training",
                description = "Dedicate 2 to 3 sessions weekly to muscle-strengthening exercises targeting major groups using bodyweight calisthenics (squats, push-ups, planks) or resistance bands.",
                benefits = "Reverses age-related sarcopenia, stimulates bone mineral deposition against osteoporosis, and boosts resting basal metabolic rate."
            ),
            marathi = LocalizedTipContent(
                title = "स्नायू बळकटीकरण व स्ट्रेंथ ट्रेनिंग",
                description = "आठवड्यातून २ ते ३ वेळा स्नायूंना बळकटी देणारे व्यायाम करा—जसे की स्क्वॅट्स, पुश-अप्स, प्लॅन्क्स किंवा रेसिस्टन्स बँडचा वापर करून व्यायाम करणे.",
                benefits = "स्नायूंची ताकद टिकून राहते, हाडांची घनता वाढून ऑस्टिओपोरोसिसचा धोका टळतो आणि शरीरातील चयापचय (मेटाबॉलिझम) गतिमान होते."
            ),
            hindi = LocalizedTipContent(
                title = "मांसपेशियों की मजबूती और स्ट्रेंथ ट्रेनिंग",
                description = "हफ्ते में 2 से 3 बार मांसपेशियों को मजबूत करने वाले व्यायाम करें—जैसे स्क्वैट्स, पुश-अप्स, प्लैंक या रेसिस्टेंस बैंड से कसरत।",
                benefits = "उम्र के साथ मांसपेशियों का क्षय रुकता है, हड्डियों का घनत्व बढ़ता है और शरीर की कैलोरी बर्न करने की क्षमता तेज होती है।"
            )
        ),
        HealthTipItem(
            id = 8,
            category = HealthTipCategory.FITNESS,
            english = LocalizedTipContent(
                title = "Desk Mobility & The 20-20-20 Rule",
                description = "If seated at a desk, stand up every 45 minutes for a 2-minute posture reset. Every 20 minutes, focus your eyes on an object at least 20 feet away for 20 continuous seconds.",
                benefits = "Prevents venous pooling in the lower legs, relieves lumbar spine disc compression, and minimizes digital ocular fatigue and headaches."
            ),
            marathi = LocalizedTipContent(
                title = "कामादरम्यान हालचाल व डोळ्यांची २०-२०-२० पद्धत",
                description = "सलग बसून काम करताना दर ४५ मिनिटांनी २ मिनिटे उभे राहून स्ट्रेचिंग करा. दर २० मिनिटांनी २० फूट अंतरावरील वस्तूवर २० सेकंद लक्ष केंद्रित करा.",
                benefits = "पायातील शिरांमध्ये रक्ताची गुठळी होण्याचा धोका टळतो, मणक्यावरील ताण कमी होतो आणि डोळ्यांचा थकवा व डोकेदुखी दूर होते."
            ),
            hindi = LocalizedTipContent(
                title = "डेस्क मोबिलिटी और 20-20-20 नियम",
                description = "कुर्सी पर लगातार काम करते समय हर 45 मिनट पर 2 मिनट के लिए खड़े हों। हर 20 मिनट में 20 फीट दूर किसी वस्तु को 20 सेकंड तक देखें।",
                benefits = "पैरों में रक्त संचलन सही रहता है, रीढ़ की हड्डी का तनाव कम होता है और कंप्यूटर स्क्रीन से होने वाला आंखों का तनाव मिटता है।"
            )
        ),
        HealthTipItem(
            id = 9,
            category = HealthTipCategory.FITNESS,
            english = LocalizedTipContent(
                title = "Core Stability & Ergonomic Posture",
                description = "Position computer monitors precisely at eye height with shoulders back and elbows at 90 degrees. Engage deep transversus abdominis core muscles while sitting or walking.",
                benefits = "Eliminates forward-head posture ('tech neck'), prevents chronic lower back pain, and optimizes diaphragm expansion for effortless oxygenation."
            ),
            marathi = LocalizedTipContent(
                title = "योग्य शारीरिक मुद्रा व पोटाचे स्नायू",
                description = "संगणकाचा स्क्रीन डोळ्यांच्या रेषेत ठेवा, खांदे सैल व मागे ठेवा आणि कोपर ९० अंशात ठेवा. बसताना व चालताना पोटाच्या स्नायूंना हलके आत खेचून ताठ बसा.",
                benefits = "मानेचा व पाठीचा जुनाट त्रास टळतो, शरीराचा तोल सुधारतो आणि फुफ्फुसांना मोकळा श्वास घेण्यासाठी पुरेशी जागा मिळते."
            ),
            hindi = LocalizedTipContent(
                title = "सही शारीरिक मुद्रा और कोर स्थिरता",
                description = "कंप्यूटर स्क्रीन को आंखों के ठीक सामने रखें, कंधों को सीधा और कोहनी को 90 डिग्री पर रखें। बैठते व चलते समय रीढ़ को सीधा रखें।",
                benefits = "गर्दन और पीठ के पुराने दर्द से मुक्ति मिलती है, शारीरिक संतुलन बेहतर होता है और फेफड़ों को पूरी ऑक्सीजन मिलती है।"
            )
        ),
        HealthTipItem(
            id = 10,
            category = HealthTipCategory.FITNESS,
            english = LocalizedTipContent(
                title = "Dynamic Warm-Up & Cooldown Recovery",
                description = "Never begin intense workouts cold. Spend 5-7 minutes performing dynamic joint circles and leg swings before training, followed by 5 minutes of static stretching and deep breathing afterward.",
                benefits = "Preheats synovial joint fluid, drastically reduces ligament tears, and speeds up clearance of muscular lactic acid metabolites."
            ),
            marathi = LocalizedTipContent(
                title = "व्यायामापूर्वी वॉर्म-अप आणि नंतर कूल-डाऊन",
                description = "थेट जड व्यायामाला सुरुवात करू नका. आधी ५-७ मिनिटे सांध्यांची हलकी हालचाल (वॉर्म-अप) करा आणि व्यायामानंतर ५ मिनिटे शांत स्ट्रेचिंग व प्राणायाम करा.",
                benefits = "सांध्यांमधील वंगण सक्रिय होऊन स्नायू दुखावण्याचा धोका टळतो आणि व्यायामानंतर स्नायू आखडणे व वेदना लवकर कमी होतात."
            ),
            hindi = LocalizedTipContent(
                title = "व्यायाम से पूर्व वार्म-अप और बाद में कूल-डाउन",
                description = "बिना तैयारी के भारी कसरत शुरू न करें। कसरत से पहले 5-7 मिनट जोड़ों को गतिशील करें और कसरत के बाद 5 मिनट हल्की स्ट्रेचिंग करें।",
                benefits = "मांसपेशियों के फटने का जोखिम कम होता है, जोड़ों का लचीलापन बढ़ता है और व्यायाम के बाद होने वाला मांसपेशियों का दर्द तेजी से ठीक होता है।"
            )
        ),

        // ==========================================
        // 3. SLEEP & REST (4 Tips)
        // ==========================================
        HealthTipItem(
            id = 11,
            category = HealthTipCategory.SLEEP,
            english = LocalizedTipContent(
                title = "Consistent Circadian Sleep Schedule",
                description = "Anchor your biological master clock by going to sleep and waking up at the exact same hour 7 days a week (including weekends), varying by no more than 30 minutes.",
                benefits = "Synchronizes circadian cortisol-melatonin balance, deepens slow-wave stage 3 sleep, and eliminates groggy morning sleep inertia."
            ),
            marathi = LocalizedTipContent(
                title = "नियमित व ठराविक झोपेची वेळ",
                description = "शरीराचे जैविक घड्याळ व्यवस्थित ठेवण्यासाठी आठवड्याचे सातीही दिवस (सुट्टीच्या दिवशीसुद्धा) एकाच ठराविक वेळी झोपा आणि एकाच वेळी जागे व्हा.",
                benefits = "झोपेचे हार्मोन्स संतुलित राहतात, गाढ व शांत झोप लागते आणि सकाळी उठल्यावर ताजेतवाने व उत्साही वाटते."
            ),
            hindi = LocalizedTipContent(
                title = "सोने और जागने का नियमित समय",
                description = "अपनी जैविक घड़ी को दुरुस्त रखने के लिए सप्ताह के सातों दिन एक ही समय पर सोएं और एक ही समय पर जागें (सप्ताहांत में भी 30 मिनट से अधिक अंतर न रखें)।",
                benefits = "मेलाटोनिन हार्मोन का स्राव सही समय पर होता है, गहरी नींद मिलती है और सुबह उठने पर ताजगी व ऊर्जा महसूस होती है।"
            )
        ),
        HealthTipItem(
            id = 12,
            category = HealthTipCategory.SLEEP,
            english = LocalizedTipContent(
                title = "Digital Sunset & Blue Light Curfew",
                description = "Institute a strict technology curfew at least 60 minutes before bed. Silence notifications, switch displays to warm night mode, and read a physical paper book or journal instead.",
                benefits = "Prevents retinal melanopsin stimulation from suppressing melatonin synthesis, cutting the time required to fall asleep by up to 50%."
            ),
            marathi = LocalizedTipContent(
                title = "झोपण्यापूर्वी स्क्रीनचा वापर पूर्णपणे थांबवा",
                description = "झोपण्याच्या किमान १ तास आधी मोबाईल, लॅपटॉप आणि टीव्ही बंद करा. नोटिफिकेशन्स सायलेंट करा आणि त्याऐवजी एखादे पुस्तक वाचा किंवा शांत संगीत ऐका.",
                benefits = "मेंदूमध्ये मेलाटोनिनची निर्मिती सुरळीत होते, झोप लवकर लागते आणि रात्री वारंवार जाग येण्याची समस्या दूर होते."
            ),
            hindi = LocalizedTipContent(
                title = "सोने से पूर्व डिजिटल स्क्रीन से दूरी",
                description = "सोने के कम से कम 60 मिनट पहले स्मार्टफोन, टीवी और लैपटॉप बंद कर दें। इसके बदले कोई अच्छी किताब पढ़ें या शांत संगीत सुनें।",
                benefits = "स्क्रीन की नीली रोशनी से मेलाटोनिन हार्मोन रुकता नहीं, बिस्तर पर जाते ही जल्दी नींद आती है और नींद की गुणवत्ता सुधरती है।"
            )
        ),
        HealthTipItem(
            id = 13,
            category = HealthTipCategory.SLEEP,
            english = LocalizedTipContent(
                title = "Cool, Dark & Quiet Sleep Sanctuary",
                description = "Maintain your bedroom environment between 18°C and 21°C (65-70°F). Use heavy blackout curtains, eliminate standby LED diodes, and introduce white noise or earplugs if needed.",
                benefits = "Triggers the natural thermoregulatory core temperature drop required for REM sleep and brain glymphatic neuro-waste clearance."
            ),
            marathi = LocalizedTipContent(
                title = "शांत, अंधारलेली व थंडगार बेडरूम",
                description = "बेडरूमचे तापमान सुखद (१८ ते २१ अंश सेल्सिअस) ठेवा. खोलीत संपूर्ण अंधार करण्यासाठी पडदे लावा, घरातील इलेक्ट्रॉनिक दिव्यांचे प्रकाश टाळा आणि खोली शांत ठेवा.",
                benefits = "शरीराचे तापमान योग्य पद्धतीने कमी होऊन गाढ झोप लागते आणि झोपेत मेंदूतील विषारी घटक बाहेर टाकण्याची प्रक्रिया जलद होते."
            ),
            hindi = LocalizedTipContent(
                title = "शांत, अंधेरा और ठंडा शयनकक्ष",
                description = "कमरे का तापमान हल्का ठंडा व आरामदायक (18-21°C) रखें। खिड़कियों पर पर्दे लगाकर पूरा अंधेरा करें और किसी भी रोशनी या आवाज को रोकें।",
                benefits = "शरीर का तापमान नियंत्रित होकर गहरी नींद को प्रेरित करता है और मस्तिष्क से विषाक्त पदार्थों की सफाई सुचारू रूप से होती है।"
            )
        ),
        HealthTipItem(
            id = 14,
            category = HealthTipCategory.SLEEP,
            english = LocalizedTipContent(
                title = "Afternoon Caffeine & Dinner Buffer",
                description = "Enforce a caffeine cutoff at 2:00 PM (caffeine has a 5-7 hour half-life), and finish dinner at least 2.5 to 3 hours before retiring for the night.",
                benefits = "Ensures adenosine sleep-pressure receptors remain unobstructed and protects against nocturnal acid reflux and cardiac palpitations."
            ),
            marathi = LocalizedTipContent(
                title = "दुपारनंतर चहा-कॉफी व रात्री हलके जेवण",
                description = "दुपारी २ नंतर चहा किंवा कॉफी पिणे टाळा (कॅफिनचा प्रभाव शरीरात ६ ते ८ तास राहतो) आणि रात्रीचे जेवण झोपण्याच्या किमान अडीच ते तीन तास आधी घ्या.",
                benefits = "मेंदू शांत राहून नैसर्गिक झोप येते, रात्री छातीत जळजळ (ॲसिडिटी) होत नाही आणि पचनसंस्थेला विश्रांती मिळते."
            ),
            hindi = LocalizedTipContent(
                title = "दोपहर बाद कैफीन से दूरी और जल्दी डिनर",
                description = "दोपहर 2 बजे के बाद चाय या कॉफी न पिएं (कैफीन का असर 6-8 घंटे रहता है) और रात का भोजन सोने से कम से कम 2.5 से 3 घंटे पहले समाप्त कर लें।",
                benefits = "मस्तिष्क को शांति मिलती है, रात में बेचैनी व एसिडिटी नहीं होती तथा हृदय गति सामान्य रहकर गहरी नींद मिलती है।"
            )
        ),

        // ==========================================
        // 4. HYDRATION & WATER (4 Tips)
        // ==========================================
        HealthTipItem(
            id = 15,
            category = HealthTipCategory.HYDRATION,
            english = LocalizedTipContent(
                title = "Baseline Daily Fluid Intake",
                description = "Consume 2.5 to 3.5 liters of pure water distributed evenly throughout waking hours. Increase intake by 500 ml for every 45 minutes of vigorous exercise or high heat.",
                benefits = "Maintains optimal blood volume and pressure, lubricates cartilage joints, flushes renal metabolites, and wards off mental fatigue."
            ),
            marathi = LocalizedTipContent(
                title = "दररोज २.५ ते ३.५ लिटर पुरेसे पाणी",
                description = "दिवसभरात नियमित अंतराने २.५ ते ३.५ लिटर शुद्ध पाणी प्या. उन्हाळ्यात किंवा कडक व्यायाम करताना घामावाटे जाणारे पाणी भरून काढण्यासाठी अतिरिक्त पाणी प्या.",
                benefits = "रक्ताभिसरण सुरळीत राहते, सांध्यांमधील वंगण टिकून राहते, किडनीतून विषारी घटक बाहेर पडतात आणि थकवा व डोकेदुखी टळते."
            ),
            hindi = LocalizedTipContent(
                title = "प्रतिदिन 2.5 से 3.5 लीटर पर्याप्त जल सेवन",
                description = "दिनभर में नियमित अंतराल पर 2.5 से 3.5 लीटर शुद्ध पानी पिएं। कड़ी मेहनत, पसीना या गर्मी के मौसम में पानी की मात्रा थोड़ी और बढ़ाएं।",
                benefits = "रक्त संचार संतुलित रहता है, जोड़ों में चिकनाई बनी रहती है, गुर्दे सुचारू रूप से काम करते हैं और शारीरिक कमजोरी दूर होती है।"
            )
        ),
        HealthTipItem(
            id = 16,
            category = HealthTipCategory.HYDRATION,
            english = LocalizedTipContent(
                title = "Morning Lukewarm Rehydration",
                description = "Drink 1 to 2 glasses of lukewarm water immediately upon waking, before consuming tea or breakfast, to rehydrate following 8 hours of respiratory moisture loss.",
                benefits = "Gently stimulates gastrocolic peristalsis, assists hepatic and renal filtration, and fires up morning metabolic alertness."
            ),
            marathi = LocalizedTipContent(
                title = "सकाळी उठल्यावर कोमट पाणी पिणे",
                description = "सकाळी उठल्याबरोबर दात घासून चहा-नाश्त्यापूर्वी १ ते २ ग्लास कोमट पाणी प्या. रात्रीच्या ८ तासांच्या झोपेत शरीरातील कमी झालेले पाणी भरून निघते.",
                benefits = "आतड्यांची हालचाल सुधारून पोट स्वच्छ होते, यकृत व किडनीचे कार्य गतिमान होते आणि दिवसभरासाठी शरीराला ताजी ऊर्जा मिळते."
            ),
            hindi = LocalizedTipContent(
                title = "सुबह उठकर गुनगुना पानी पीना",
                description = "सुबह उठते ही चाय या नाश्ते से पहले 1 से 2 गिलास गुनगुना पानी पिएं ताकि रातभर की नींद में कम हुए पानी की तुरंत पूर्ति हो सके।",
                benefits = "आंतों की गतिशीलता सुधरती है और पेट साफ होता है, शरीर के विषैले तत्व बाहर निकलते हैं और चयापचय तेज होता है।"
            )
        ),
        HealthTipItem(
            id = 17,
            category = HealthTipCategory.HYDRATION,
            english = LocalizedTipContent(
                title = "Urine Color Hydration Check",
                description = "Routinely inspect your urine color during the day: pale straw or light transparent lemonade reflects prime hydration, while dark honey or amber indicates dehydration.",
                benefits = "Provides an instantaneous, zero-cost personal clinical metric to avoid hypohydration-induced cognitive lapses and kidney stone precipitation."
            ),
            marathi = LocalizedTipContent(
                title = "लघवीच्या रंगावरून पाण्याची पातळी ओळखा",
                description = "दिवसभरात लघवीच्या रंगाकडे लक्ष द्या: हलका पिवळा किंवा पारदर्शक रंग उत्तम हायड्रेशन दर्शवतो, तर गडद पिवळा रंग शरीरात पाणी कमी असल्याचे दर्शवतो.",
                benefits = "शरीरातील पाण्याची स्थिती लगेच समजते, निर्जलीकरणामुळे होणारी डोकेदुखी टळते आणि मुतखडा (किडनी स्टोन) होण्याचा धोका कमी होतो."
            ),
            hindi = LocalizedTipContent(
                title = "मूत्र के रंग से हाइड्रेशन की जांच",
                description = "दिनभर में मूत्र के रंग पर ध्यान दें: हल्का पीला या पारदर्शी रंग सही हाइड्रेशन का प्रतीक है, जबकि गहरा पीला रंग पानी की कमी दर्शाता है।",
                benefits = "पानी की कमी का तुरंत पता चलता है, डिहाइड्रेशन से होने वाले सिरदर्द से बचाव होता है और गुर्दे की पथरी का खतरा कम होता है।"
            )
        ),
        HealthTipItem(
            id = 18,
            category = HealthTipCategory.HYDRATION,
            english = LocalizedTipContent(
                title = "Natural Electrolyte Balancing",
                description = "Pair plain water with natural electrolyte sources—such as fresh tender coconut water, diluted cumin-spiced buttermilk (chaas), or lemon water with a pinch of Himalayan pink salt.",
                benefits = "Restores vital cellular sodium, potassium, and magnesium ratios, preventing heat exhaustion and painful skeletal muscle cramps."
            ),
            marathi = LocalizedTipContent(
                title = "नैसर्गिक इलेक्ट्रोलाइट्सचा समतोल",
                description = "साध्या पाण्यासोबत नैसर्गिक इलेक्ट्रोलाइट्स घ्या—जसे की नारळपाणी, जिरे घातलेले ताक (छास), किंवा चिमूटभर सैंधव मीठ घातलेले लिंबू पाणी प्या.",
                benefits = "शरीरातील सोडियम, पोटॅशियम व मॅग्नेशियमचे प्रमाण संतुलित राहते, उष्माघात टळतो आणि स्नायूंना पेटके (क्रॅम्प्स) येत नाहीत."
            ),
            hindi = LocalizedTipContent(
                title = "प्राकृतिक इलेक्ट्रोलाइट संतुलन",
                description = "सादे पानी के साथ प्राकृतिक इलेक्ट्रोलाइट्स लें—जैसे ताजा नारियल पानी, जीरा युक्त छाछ या चुटकी भर सेंधा नमक मिला नींबू पानी।",
                benefits = "कोशिकाओं में सोडियम, पोटैशियम व मैग्नीशियम का संतुलन बना रहता है, लू से बचाव होता है और मांसपेशियों की ऐंठन मिटती है।"
            )
        ),

        // ==========================================
        // 5. MENTAL WELLNESS (4 Tips)
        // ==========================================
        HealthTipItem(
            id = 19,
            category = HealthTipCategory.MENTAL,
            english = LocalizedTipContent(
                title = "4-7-8 Mindful Breathwork for Stress",
                description = "When overwhelmed, practice the 4-7-8 breathing method: inhale quietly through your nose for 4 seconds, gently hold your breath for 7 seconds, and exhale completely through your mouth for 8 seconds. Repeat 4 times.",
                benefits = "Stimulates vagus nerve parasympathetic activation, instantly dampening acute fight-or-flight cortisol surges and slowing heart rate."
            ),
            marathi = LocalizedTipContent(
                title = "४-७-८ प्राणायाम व ताणतणाव मुक्ती",
                description = "तणाव जाणवल्यास ४-७-८ श्वास तंत्र वापरा: नाकाने ४ सेकंद हळुवार श्वास आत घ्या, ७ सेकंद श्वास रोखून धरा, आणि तोंडाने ८ सेकंदात हळूहळू श्वास सोडा. हे ४ वेळा करा.",
                benefits = "मेंदूतील ताण नियंत्रित करणारी नस सक्रिय होते, हृदयाचे ठोके शांत होतात आणि अचानक येणारी चिंता व भीती लगेच दूर होते."
            ),
            hindi = LocalizedTipContent(
                title = "तनाव मुक्ति हेतु 4-7-8 श्वास तकनीक",
                description = "तनाव महसूस होने पर 4-7-8 श्वास नियम अपनाएं: नाक से 4 सेकंड सांस अंदर लें, 7 सेकंड सांस रोकें, और मुंह से 8 सेकंड में धीरे-धीरे बाहर छोड़ें। इसे 4 बार दोहराएं।",
                benefits = "वेगस नर्व सक्रिय होकर तनाव हार्मोन कोर्टिसोल को कम करती है, हृदय गति सामान्य होती है और तत्काल मानसिक शांति मिलती है।"
            )
        ),
        HealthTipItem(
            id = 20,
            category = HealthTipCategory.MENTAL,
            english = LocalizedTipContent(
                title = "Daily Gratitude & Micro-Mindfulness",
                description = "Dedicate 3 to 5 minutes each morning to write down three specific things you genuinely appreciate. Practice anchoring attention in the present moment without judging your passing thoughts.",
                benefits = "Rewires positive neuro-synaptic pathways in the prefrontal cortex, reduces amygdala hyper-reactivity, and elevates emotional resilience."
            ),
            marathi = LocalizedTipContent(
                title = "दैनिक कृतज्ञता व मन:शांतीची सवय",
                description = "दररोज सकाळी ३ ते ५ मिनिटे काढून स्वतःच्या आयुष्यातील चांगल्या ३ गोष्टींची नोंद करा आणि मनातील नकारात्मक विचारांना बाजूला ठेवून वर्तमान क्षणाचा आनंद घ्या.",
                benefits = "मेंदूमध्ये सकारात्मक विचारसरणी दृढ होते, निराशा व चिंता कमी होते आणि जीवनाबद्दलचा उत्साह व मानसिक समाधान वाढते."
            ),
            hindi = LocalizedTipContent(
                title = "दैनिक आभार और माइंडफुलनेस अभ्यास",
                description = "रोज सुबह 3 से 5 मिनट निकालकर अपने जीवन की 3 अच्छी बातों को याद करें या डायरी में लिखें। वर्तमान क्षण में बिना किसी पूर्वग्रह के शांत रहें।",
                benefits = "मस्तिष्क में सकारात्मक न्यूरोलॉजिकल बदलाव आते हैं, घबराहट और नकारात्मकता घटती है तथा भावनात्मक मजबूती बढ़ती है।"
            )
        ),
        HealthTipItem(
            id = 21,
            category = HealthTipCategory.MENTAL,
            english = LocalizedTipContent(
                title = "Nurturing Meaningful Social Bonds",
                description = "Spend dedicated, unhurried time conversing with loved ones, close friends, or supportive mentors each week. Share authentic feelings and engage in mutual active listening.",
                benefits = "Stimulates endogenous oxytocin release, buffers against depressive isolation, and measurably lowers long-term cognitive decline risk."
            ),
            marathi = LocalizedTipContent(
                title = "कुटुंब व मित्रांशी आत्मीय संवाद",
                description = "आठवड्यातून काही वेळ कुटुंब, मित्र किंवा हितचिंतकांशी मनमोकळा संवाद साधण्यासाठी द्या. एकमेकांच्या भावना समजून घ्या आणि सुख-दुःख वाटून घ्या.",
                benefits = "ऑक्सिटोसिन हार्मोन स्रवून एकटेपणा व नैराश्य दूर होते, भावनिक आधार मिळतो आणि दीर्घायुष्यासाठी मानसिक बळ मिळते."
            ),
            hindi = LocalizedTipContent(
                title = "अर्थपूर्ण सामाजिक संबंध और बातचीत",
                description = "सप्ताह में परिवार, मित्रों या शुभचिंतकों के साथ दिल खोलकर बातचीत करने के लिए समय निकालें। एक-दूसरे की बात ध्यान से सुनें और साझा करें।",
                benefits = "अकेलापन और अवसाद दूर होता है, मन को सुकून मिलता है और मानसिक स्वास्थ्य लंबे समय तक मजबूत रहता है।"
            )
        ),
        HealthTipItem(
            id = 22,
            category = HealthTipCategory.MENTAL,
            english = LocalizedTipContent(
                title = "Restorative Immersion in Nature",
                description = "Spend at least 20 minutes walking or resting in a tree-filled park, botanical garden, or natural landscape without checking mobile devices or listening to loud audio.",
                benefits = "Lowers circulating salivary cortisol, eases mental sensory fatigue, and restores executive cognitive bandwidth and creativity."
            ),
            marathi = LocalizedTipContent(
                title = "निसर्गाच्या सानिध्यात विश्रांती",
                description = "आठवड्यातून किमान २-३ वेळा २० मिनिटे कोणत्याही उद्यानात किंवा झाडांच्या सानिध्यात शांत चाला. यावेळी मोबाईल किंवा स्क्रीनचा वापर पूर्णपणे टाळा.",
                benefits = "रक्तातील तणाव हार्मोन्स कमी होतात, मनाचा थकवा दूर होऊन एकाग्रता वाढते आणि मन प्रसन्न व शांत राहते."
            ),
            hindi = LocalizedTipContent(
                title = "प्रकृति और हरियाली के बीच विश्राम",
                description = "हफ्ते में 2-3 बार किसी पार्क या पेड़ों के बीच 20 मिनट शांत टहलें। इस दौरान मोबाइल फोन या तेज आवाज से पूरी तरह दूर रहें।",
                benefits = "तनाव हार्मोन कोर्टिसोल का स्तर घटता है, मानसिक थकान दूर होती है और सोचने-समझने की क्षमता व रचनात्मकता बढ़ती है।"
            )
        )
    )

    fun getAllTips(): List<HealthTipItem> = allTips

    fun getTipById(id: Int): HealthTipItem? = allTips.firstOrNull { it.id == id }

    fun getTipsByCategory(category: HealthTipCategory): List<HealthTipItem> {
        return if (category == HealthTipCategory.ALL) {
            allTips
        } else {
            allTips.filter { it.category == category }
        }
    }
}
