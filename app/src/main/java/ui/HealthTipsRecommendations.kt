package ui

/**
 * High-level clinical wellness concern classifications.
 */
enum class WellnessConcernType(val key: String) {
    DRY_SKIN("dry_skin"),
    GENERAL_TIREDNESS("general_tiredness"),
    DEHYDRATION("dehydration"),
    POOR_SLEEP("poor_sleep"),
    DIGESTIVE_DISCOMFORT("digestive_discomfort"),
    LOW_PHYSICAL_ACTIVITY("low_physical_activity"),
    STRESS_MENTAL_FATIGUE("stress_mental_fatigue"),
    NUTRITIONAL_CONCERNS("nutritional_concerns")
}

data class LocalizedWellnessConcern(
    val name: String,
    val description: String,
    val whyHelpful: String,
    val practicalSuggestions: List<String>,
    val precautions: String,
    val medicalGuidance: String
)

data class WellnessConcernItem(
    val type: WellnessConcernType,
    val relatedTipIds: List<Int>,
    val english: LocalizedWellnessConcern,
    val marathi: LocalizedWellnessConcern,
    val hindi: LocalizedWellnessConcern
) {
    fun getLocalized(language: String): LocalizedWellnessConcern {
        return when (language.lowercase().trim()) {
            "मराठी", "mr", "marathi" -> marathi
            "हिंदी", "hi", "hindi" -> hindi
            else -> english
        }
    }
}

data class LocalizedTipDetails(
    val whyUseful: String,
    val targetAudience: List<String>,
    val relevantConcerns: List<String>,
    val personalizedSuggestions: List<String>,
    val detailedBenefits: List<String>,
    val precautions: List<String>,
    val whenToConsultDoctor: String
)

data class TipDetailBundle(
    val tipId: Int,
    val relatedTipIds: List<Int>,
    val english: LocalizedTipDetails,
    val marathi: LocalizedTipDetails,
    val hindi: LocalizedTipDetails
) {
    fun getLocalized(language: String): LocalizedTipDetails {
        return when (language.lowercase().trim()) {
            "मराठी", "mr", "marathi" -> marathi
            "हिंदी", "hi", "hindi" -> hindi
            else -> english
        }
    }
}

object HealthTipsRecommendations {

    fun getDetailLabels(language: String): Map<String, String> {
        return when (language.lowercase().trim()) {
            "मराठी", "mr", "marathi" -> mapOf(
                "overview_title" to "माहिती व महत्व",
                "why_useful_label" to "ही सवय का महत्त्वाची आहे?",
                "who_useful_title" to "ही टिप कोणासाठी उपयुक्त आहे?",
                "target_audience_label" to "लक्षित व्यक्ती व परिस्थिती",
                "relevant_concerns_label" to "संबंधित आरोग्य विषय",
                "personalized_suggestions_title" to "वैयक्तिक आरोग्य मार्गदर्शन",
                "suggestions_note" to "सूचना: व्यक्तीचे वय आणि प्रकृतीनुसार गरजा बदलू शकतात.",
                "benefits_title" to "महत्वाचे आरोग्य फायदे",
                "precautions_title" to "खबरदारी व सुरक्षा",
                "when_consult_title" to "डॉक्टरांचा सल्ला केव्हा घ्यावा?",
                "emergency_alert" to "तात्काळ वैद्यकीय इशारा: तीव्र छातीत वेदना, श्वास घेण्यास अडचण किंवा अचानक तीव्र त्रास उद्भवल्यास त्वरित जवळच्या रुग्णालयात संपर्क साधा.",
                "related_tips_title" to "संबंधित आरोग्य टिप्स",
                "tip_details_screen_title" to "आरोग्य टिप सविस्तर",
                "view_detail_action" to "तपशील पहा",
                "disclaimer_banner" to "वैद्यकीय सूचना: ही माहिती सामान्य आरोग्य संवर्धनासाठी आहे. हा कोणत्याही वैद्यकीय निदान किंवा उपचारांचा पर्याय नाही."
            )
            "हिंदी", "hi", "hindi" -> mapOf(
                "overview_title" to "अवलोकन और महत्व",
                "why_useful_label" to "यह आदत क्यों जरूरी है?",
                "who_useful_title" to "यह सुझाव किसके लिए उपयोगी है?",
                "target_audience_label" to "लक्षित समूह और परिस्थितियां",
                "relevant_concerns_label" to "संबंधित स्वास्थ्य विषय",
                "personalized_suggestions_title" to "व्यक्तिगत स्वास्थ्य सुझाव",
                "suggestions_note" to "नोट: व्यक्ति की उम्र और स्वास्थ्य स्थिति के अनुसार आवश्यकताएं भिन्न हो सकती हैं।",
                "benefits_title" to "प्रमुख स्वास्थ्य लाभ",
                "precautions_title" to "सावधानियां एवं सुरक्षा",
                "when_consult_title" to "डॉक्टर से परामर्श कब लें?",
                "emergency_alert" to "आपातकालीन चेतावनी: सीने में तेज दर्द, सांस लेने में अत्यधिक कठिनाई या अचानक गंभीर लक्षण दिखने पर तत्काल नजदीकी अस्पताल में संपर्क करें।",
                "related_tips_title" to "संबंधित स्वास्थ्य सुझाव",
                "tip_details_screen_title" to "सुझाव विवरण",
                "view_detail_action" to "विवरण देखें",
                "disclaimer_banner" to "चिकित्सीय सूचना: यह सामग्री केवल सामान्य स्वास्थ्य जागरूकता के लिए है। यह किसी रोग के निदान या उपचार का विकल्प नहीं है।"
            )
            else -> mapOf(
                "overview_title" to "Overview & Science",
                "why_useful_label" to "Why This Habit Matters",
                "who_useful_title" to "Who May Find This Useful?",
                "target_audience_label" to "Target Audience & Scenarios",
                "relevant_concerns_label" to "Related Wellness Concerns",
                "personalized_suggestions_title" to "Personalized Wellness Suggestions",
                "suggestions_note" to "Note: Individual wellness needs vary based on age, metabolism, and existing conditions.",
                "benefits_title" to "Key Wellness Benefits",
                "precautions_title" to "Precautions & Safety",
                "when_consult_title" to "When to Consult a Physician",
                "emergency_alert" to "Emergency Notice: For crushing chest pain, acute dyspnea, or sudden severe symptoms, seek emergency hospital care immediately.",
                "related_tips_title" to "Related Wellness Tips",
                "tip_details_screen_title" to "Health Tip Details",
                "view_detail_action" to "View Details",
                "disclaimer_banner" to "Clinical Notice: These suggestions provide educational wellness guidance and do not replace personalized medical diagnosis or therapy."
            )
        }
    }

    private val detailsList: List<TipDetailBundle> = listOf(
        // Tip 1: Balanced Macronutrient Plate (Nutrition)
        TipDetailBundle(
            tipId = 1,
            relatedTipIds = listOf(2, 3, 4),
            english = LocalizedTipDetails(
                whyUseful = "Balancing macronutrients prevents reactive hypoglycemia spikes, delivers essential amino acids for tissue maintenance, and provides sustained stamina without energy crashes.",
                targetAudience = listOf(
                    "People experiencing mid-day energy dips and fatigue after meals",
                    "Individuals aiming to manage healthy body composition and weight",
                    "Those looking to stabilize blood sugar fluctuations through natural diet",
                    "Anyone relying heavily on single-macronutrient refined meals"
                ),
                relevantConcerns = listOf("General Tiredness", "Nutritional Gaps", "Metabolic Balance"),
                personalizedSuggestions = listOf(
                    "Fill half your plate with raw green salads and steamed seasonal vegetables first.",
                    "Include a clean protein source (boiled lentils, tofu, paneer, sprouts, or eggs) in every major meal.",
                    "Swap refined white rice or maida flatbreads for fiber-rich millets, quinoa, or whole wheat rotis.",
                    "Dress salads with a teaspoon of cold-pressed oil or crushed seeds for healthy lipid assimilation."
                ),
                detailedBenefits = listOf(
                    "Prevents post-prandial glycemic spikes and insulin surges.",
                    "Promotes sustained satiety lasting 3 to 4 hours between meals.",
                    "Delivers a broad spectrum of fat-soluble and water-soluble micronutrients.",
                    "Supports cellular protein synthesis and immune antibody production."
                ),
                precautions = listOf(
                    "Individuals with chronic kidney disease should consult their nephrologist regarding total daily protein allowances.",
                    "Diabetic patients should monitor carbohydrate portions and check post-meal glucose readings.",
                    "Do not eliminate any major macronutrient group entirely without clinical supervision."
                ),
                whenToConsultDoctor = "Consult a physician or registered dietitian if you experience unexplained rapid weight loss, persistent polyuria (excessive urination), or recurrent lightheadedness after meals."
            ),
            marathi = LocalizedTipDetails(
                whyUseful = "संतुलित आहारामुळे रक्तातील साखरेची पातळी अचानक वाढत किंवा कमी होत नाही, शरीराला दिवसभर शाश्वत ऊर्जा मिळते आणि पेशींच्या निर्मितीसाठी आवश्यक प्रथिने मिळतात.",
                targetAudience = listOf(
                    "जेवणानंतर लगेच सुस्ती किंवा थकवा जाणवणारे लोक",
                    "शरीराचे वजन आणि पोटाची चरबी नियंत्रित ठेवू इच्छिणारे",
                    "रक्तातील साखरेचे प्रमाण नैसर्गिक आहाराने स्थिर ठेवू इच्छिणारे",
                    "केवळ भात किंवा पोळीवर जास्त अवलंबून असणारे कुटुंब"
                ),
                relevantConcerns = listOf("सततचा थकवा", "पोषणाचा अभाव", "चयापचय संतुलन"),
                personalizedSuggestions = listOf(
                    "ताटातील निम्मी जागा ताज्या हिरव्या भाज्या आणि काकडी, गाजर, टोमॅटोच्या सॅलडने भरा.",
                    "प्रत्येक जेवणात डाळी, मोड आलेली कडधान्ये, पनीर, अंडी यांसारख्या सकस प्रथिनांचा समावेश करा.",
                    "पांढऱ्या भाताऐवजी ज्वारी, बाजरी, नाचणी किंवा ब्राऊन राईसचा वापर वाढवा.",
                    "सॅलडवर थोडे तीळ किंवा जवसाची पूड टाकून आवश्यक स्निग्ध घटक मिळवा."
                ),
                detailedBenefits = listOf(
                    "जेवणानंतर रक्तातील साखर वेगाने वाढणे टळते.",
                    "३ ते ४ तास पोट भरल्यासारखे वाटते आणि अवेळी भूक लागत नाही.",
                    "शरीराला विविध जीवनसत्त्वे आणि खनिजे नैसर्गिक स्वरूपात मिळतात.",
                    "स्नायूंची ताकद आणि रोगप्रतिकारक पेशी मजबूत होतात."
                ),
                precautions = listOf(
                    "किडनीचा जुनाट आजार असलेल्या रुग्णांनी डाळी व प्रथिनांचे प्रमाण डॉक्टरांच्या सल्ल्यानेच ठरवावे.",
                    "मधुमेहाच्या रुग्णांनी पिष्टमय पदार्थांचे प्रमाण मोजून घ्यावे.",
                    "कोणताही मुख्य अन्नघटक पूर्णपणे बंद करू नका."
                ),
                whenToConsultDoctor = "जेवणानंतर चक्कर येणे, अचानक वजन खूप कमी होणे किंवा सतत अति तहान लागल्यास त्वरित डॉक्टरांशी संपर्क साधा."
            ),
            hindi = LocalizedTipDetails(
                whyUseful = "मैक्रोन्यूट्रिएंट्स का संतुलन भोजन के बाद ब्लड शुगर को अचानक बढ़ने से रोकता है, मांसपेशियों के लिए जरूरी प्रोटीन देता है और दिनभर निरंतर ऊर्जा प्रदान करता है।",
                targetAudience = listOf(
                    "भोजन के तुरंत बाद आलस्य और थकान महसूस करने वाले लोग",
                    "वजन और पेट की चर्बी को नियंत्रित रखने के इच्छुक व्यक्ति",
                    "ब्लड शुगर को संतुलित और स्थिर रखना चाहने वाले लोग",
                    "केवल कार्बोहाइड्रेट प्रधान भोजन करने वाले परिवार"
                ),
                relevantConcerns = listOf("सामान्य थकान", "पोषण की कमी", "मेटाबॉलिक संतुलन"),
                personalizedSuggestions = listOf(
                    "अपनी थाली का आधा हिस्सा हरी पत्तेदार सब्जियों और ताजे सलाद से भरें।",
                    "प्रत्येक भोजन में दाल, अंकुरित अनाज, पनीर या अंडे जैसे प्रोटीन अवश्य जोड़ें।",
                    "मैदा और सफेद चावल की जगह बाजरा, ज्वार, रागी या साबुत गेहूं की रोटी खाएं।",
                    "सलाद में एक चम्मच अलसी या कद्दू के बीज मिलाकर स्वस्थ वसा प्राप्त करें।"
                ),
                detailedBenefits = listOf(
                    "भोजन के बाद शुगर का स्तर स्थिर रहता है।",
                    "भूख नियंत्रित रहती है और बार-बार मीठा खाने की इच्छा नहीं होती।",
                    "शरीर को जरूरी विटामिन्स और एंटीऑक्सीडेंट्स भरपूर मात्रा में मिलते हैं।",
                    "मांसपेशियों का विकास और शरीर की रोग प्रतिरोधक क्षमता सुधरती है।"
                ),
                precautions = listOf(
                    "गुर्दे (किडनी) के मरीज प्रोटीन की मात्रा डॉक्टर की सलाह अनुसार ही लें।",
                    "मधुमेह के रोगी भोजन में कार्बोहाइड्रेट की मात्रा पर विशेष ध्यान दें।",
                    "बिना डॉक्टर के किसी भी मुख्य पोषक तत्व को पूरी तरह भोजन से न हटाएं।"
                ),
                whenToConsultDoctor = "यदि भोजन के बाद अत्यधिक कमजोरी, चक्कर आना या अत्यधिक प्यास लगने के लक्षण दिखें तो तुरंत चिकित्सक से संपर्क करें।"
            )
        ),

        // Tip 2: High-Fiber Foods & Gut Health (Nutrition)
        TipDetailBundle(
            tipId = 2,
            relatedTipIds = listOf(1, 4, 15),
            english = LocalizedTipDetails(
                whyUseful = "Dietary fiber ferments into short-chain fatty acids (SCFAs) like butyrate, reinforcing the intestinal epithelial barrier and enhancing bile acid excretion.",
                targetAudience = listOf(
                    "Individuals troubled by chronic constipation or sluggish bowel transit",
                    "People managing borderline elevated LDL cholesterol or triglycerides",
                    "Those dealing with recurring abdominal bloating and irregular digestion",
                    "Individuals with low dietary intake of fresh whole plants and legumes"
                ),
                relevantConcerns = listOf("Digestive Discomfort", "Nutritional Gaps", "Heart Health"),
                personalizedSuggestions = listOf(
                    "Gradually increase whole grain intake (rolled oats, barley, chia seeds) over 2-3 weeks to avoid bloating.",
                    "Eat whole fruits with edible skin (apples, pears, guavas) rather than straining juice.",
                    "Pair increased fiber with an additional 500-750 ml of water daily to facilitate smooth stool bulk.",
                    "Incorporate fermented probiotic curd or kefir alongside fiber to diversify intestinal flora."
                ),
                detailedBenefits = listOf(
                    "Promotes regular, effortless gastrointestinal transit and softens stool.",
                    "Acts as a prebiotic fertilizer for beneficial Bifidobacteria and Lactobacilli.",
                    "Binds intestinal dietary cholesterol, assisting cardiovascular protection.",
                    "Attenuates glucose absorption rate, supporting insulin economy."
                ),
                precautions = listOf(
                    "Increasing fiber too rapidly without sufficient water can cause gas, cramping, or impaction.",
                    "Individuals with active Crohn's disease, ulcerative colitis flare-ups, or strictures require physician-modified low-residue diets.",
                    "Take prescribed medications at least 1 hour apart from concentrated fiber supplements like psyllium husk."
                ),
                whenToConsultDoctor = "Seek medical care if constipation is accompanied by severe abdominal cramping, vomiting, rectal bleeding, or lasts longer than two weeks despite dietary adjustments."
            ),
            marathi = LocalizedTipDetails(
                whyUseful = "आहारातील फायबर आतड्यांमधील चांगल्या बॅक्टेरियांचे पोषण करते, विष्ठा मऊ करून शौचास साफ होण्यास मदत करते आणि कोलेस्टेरॉल शोषून शरीराबाहेर टाकते.",
                targetAudience = listOf(
                    "बद्धकोष्ठता, मूळव्याध किंवा पोट साफ न होण्याची तक्रार असणारे लोक",
                    "कोलेस्टेरॉलची पातळी जास्त असलेले व्यक्ती",
                    "सतत गॅसेस व पोट फुगण्याची समस्या असणारे",
                    "आहारात कच्च्या भाज्या व फळांचा अभाव असणारे"
                ),
                relevantConcerns = listOf("अपचन व पोट फुगणे", "पोषणाचा अभाव", "हृदय स्वास्थ्य"),
                personalizedSuggestions = listOf(
                    "ओट्स, मोड आलेली कडधान्ये आणि चिया सीड्स आहारात हळूहळू वाढवा.",
                    "फळांचा रस पिण्याऐवजी सालासकट ताजी फळे (सफरचंद, पेरू) चावून खा.",
                    "फायबर वाढवताना दररोज २ ते ३ ग्लास अतिरिक्त पाणी प्या, जेणेकरून पोटात गॅस होणार नाही.",
                    "दुपारच्या जेवणात ताज्या दह्याचा समावेश करून आतड्यांचे आरोग्य सुधारा."
                ),
                detailedBenefits = listOf(
                    "पोट रोज सकाळी नैसर्गिकरित्या साफ होते.",
                    "रक्तातील वाईट कोलेस्टेरॉल कमी होण्यास थेट मदत होते.",
                    "पोटात चांगल्या जीवाणूंची वाढ होऊन पचनशक्ती सुधारते.",
                    "रक्तातील साखरेचे प्रमाण अचानक वाढत नाही."
                ),
                precautions = listOf(
                    "पुरेसे पाणी न पिता एकदम जास्त फायबर खाल्ल्यास पोट फुगणे व दुखणे होऊ शकते.",
                    "आतड्यांचा तीव्र दाह (अल्सरेटिव्ह कोलायटिस) असणाऱ्यांनी डॉक्टरांच्या सल्ल्यानेच फायबर घ्यावे.",
                    "इसबगोल किंवा फायबर पावडर घेताना औषधे किमान १ तास आधी किंवा नंतर घ्यावीत."
                ),
                whenToConsultDoctor = "बद्धकोष्ठतेसोबत पोटात तीव्र दुखणे, उलट्या होणे किंवा शौचातून रक्त पडल्यास ताबडतोब डॉक्टरांकडे जा."
            ),
            hindi = LocalizedTipDetails(
                whyUseful = "डाइटरी फाइबर आंतों के मित्र बैक्टीरिया को पोषण देकर ब्यूटायरेट जैसे स्वस्थ फैटी एसिड बनाता है, मल को सुगम बनाता है और हानिकारक कोलेस्ट्रॉल को घटाता है।",
                targetAudience = listOf(
                    "कब्ज, गैस और पेट ठीक से साफ न होने से परेशान लोग",
                    "उच्च कोलेस्ट्रॉल या ट्राइग्लिसराइड्स वाले व्यक्ति",
                    "भोजन के बाद भारीपन और पेट फूलने की समस्या वाले लोग",
                    "फलों और सलाद का कम सेवन करने वाले व्यक्ति"
                ),
                relevantConcerns = listOf("पाचन संबंधी असुविधा", "पोषण की कमी", "हृदय स्वास्थ्य"),
                personalizedSuggestions = listOf(
                    "आहार में दलिया, साबुत दालें और अलसी के बीज धीरे-धीरे शामिल करें।",
                    "फलों का जूस पीने की बजाय उन्हें छिलके सहित (सेब, अमरूद) चबाकर खाएं।",
                    "फाइबर के साथ पानी की मात्रा अवश्य बढ़ाएं ताकि मल आसानी से निष्कासित हो सके।",
                    "भोजन में दही या छाछ शामिल कर आंतों के बैक्टीरिया को मजबूत बनाएं।"
                ),
                detailedBenefits = listOf(
                    "कब्ज से राहत मिलती है और पेट हल्का रहता है।",
                    "धमनियों में जमा होने वाला एलडीएल कोलेस्ट्रॉल घटता है।",
                    "पाचन तंत्र के लाभकारी बैक्टीरिया की संख्या बढ़ती है।",
                    "ब्लड शुगर नियंत्रित रखने में सहायता मिलती है।"
                ),
                precautions = listOf(
                    "बिना पर्याप्त पानी पिए अधिक फाइबर लेने से पेट में मरोड़ या गैस हो सकती है।",
                    "आंतों की गंभीर सूजन वाले मरीज डॉक्टर की सलाह अनुसार ही फाइबर लें।",
                    "फाइबर सप्लीमेंट्स और दवाओं के बीच कम से कम 1 घंटे का अंतराल रखें।"
                ),
                whenToConsultDoctor = "यदि कब्ज के साथ पेट में असहनीय दर्द, उल्टी या शौच में खून आए, तो तुरंत डॉक्टर को दिखाएं।"
            )
        ),

        // Tip 3: Minimize Ultra-Processed Sugars (Nutrition)
        TipDetailBundle(
            tipId = 3,
            relatedTipIds = listOf(1, 2, 15),
            english = LocalizedTipDetails(
                whyUseful = "Excess free fructose and refined sucrose overload hepatic glycogen storage, triggering de novo lipogenesis, fatty liver accumulation, and systemic micro-inflammation.",
                targetAudience = listOf(
                    "People with frequent sugar cravings and afternoon energy crashes",
                    "Individuals with fatty liver index (NAFLD) or elevated triglycerides",
                    "Those diagnosed with prediabetes, metabolic syndrome, or PCOS",
                    "Anyone consuming sweetened carbonated drinks, packed juices, or bakery goods"
                ),
                relevantConcerns = listOf("General Tiredness", "Metabolic Health", "Nutritional Gaps"),
                personalizedSuggestions = listOf(
                    "Substitute sweet soda and commercial bottled tea with lemon-infused water or fresh tender coconut water.",
                    "Satisfy evening sugar cravings with whole berries, an orange, or a soaked Medjool date with walnuts.",
                    "Scrutinize packaged food nutrition labels for hidden sugars: high fructose corn syrup, maltodextrin, and dextrose.",
                    "Gradually taper table sugar in morning beverages by half over two consecutive weeks."
                ),
                detailedBenefits = listOf(
                    "Reduces triglyceride synthesis and shields hepatic liver tissue from steatosis.",
                    "Rebalances dopamine reward pathways, ending compulsive sugar cravings.",
                    "Stabilizes daytime alertness and prevents sudden mental brain fog.",
                    "Decreases advanced glycation end-products (AGEs), preserving skin collagen elasticity."
                ),
                precautions = listOf(
                    "Diabetic patients on insulin or sulfonylureas should monitor glucose when cutting carbs to avoid unexpected hypoglycemia.",
                    "Do not replace sugar with heavy amounts of synthetic artificial sweeteners without moderation.",
                    "Focus on sustainable reduction rather than extreme starvation diets."
                ),
                whenToConsultDoctor = "Consult a physician if you experience intense sugar cravings accompanied by severe unquenchable thirst, frequent night urination, or tingling in the extremities."
            ),
            marathi = LocalizedTipDetails(
                whyUseful = "अति साखरेमुळे यकृतावर चरबी साचते, इन्सुलिनची कार्यक्षमता खालावते आणि शरीरात जुनाट सूज व सुस्ती निर्माण होते.",
                targetAudience = listOf(
                    "सतत गोड खाण्याची तीव्र इच्छा होणारे लोक",
                    "फॅटी लिव्हर किंवा ट्रायग्लिसराइड्स वाढलेले रुग्ण",
                    "प्री-डायबेटिस, मधुमेह किंवा पीसीओएस असणाऱ्या महिला",
                    "शीतपेये, पॅकबंद ज्यूस किंवा बेकरी बिस्किटे रोज खाणारे"
                ),
                relevantConcerns = listOf("सततचा थकवा", "चयापचय स्वास्थ्य", "पोषणाचा अभाव"),
                personalizedSuggestions = listOf(
                    "शीतपेयांना पर्याय म्हणून लिंबू पाणी, ताक किंवा शहाळ्याचे पाणी प्या.",
                    "गोड खावेसे वाटल्यास १-२ खजूर, सुकामेवा किंवा ताजी हंगामी फळे खा.",
                    "पॅकबंद खाद्यपदार्थ खरेदी करताना त्यातील साखरेचे प्रमाण तपासा.",
                    "चहा किंवा कॉफीमधील साखरेचे प्रमाण हळूहळू निम्म्यावर आणा."
                ),
                detailedBenefits = listOf(
                    "यकृतावर चरबी साचणे थांबते आणि लिव्हर सुरक्षित राहते.",
                    "रक्तातील साखरेची पातळी नियंत्रित राहून मधुमेह टळतो.",
                    "त्वचेवरील मुरुमे व सुरकुत्या कमी होऊन त्वचेचा तजेला वाढतो.",
                    "दिवसभरातील उत्साह वाढतो आणि थकवा नाहीसा होतो."
                ),
                precautions = listOf(
                    "इन्सुलिन घेणाऱ्या मधुमेही रुग्णांनी आहारातील साखर कमी करताना डॉक्टरांशी चर्चा करावी.",
                    "कृत्रिम साखरेचा (आर्टिफिशियल स्वीटनर) अतिवापर करणे टाळा.",
                    "गोड कमी करताना शरीराला अशक्तपणा येणार नाही असा पौष्टिक आहार ठेवा."
                ),
                whenToConsultDoctor = "सतत अति तहान लागणे, वारंवार लघवीला जावे लागणे किंवा हातापायांना मुंग्या येत असल्यास तात्काळ रक्त तपासणी करून डॉक्टरांचा सल्ला घ्या."
            ),
            hindi = LocalizedTipDetails(
                whyUseful = "अधिक रिफाइंड चीनी लिवर में फैट जमा करती है, इंसुलिन रेजिस्टेंस बढ़ाती है और शरीर में लगातार सूजन व सुस्ती पैदा करती है।",
                targetAudience = listOf(
                    "बार-बार मीठा खाने की तीव्र इच्छा महसूस करने वाले लोग",
                    "फैटी लिवर या बढ़े हुए ट्राइग्लिसराइड्स के मरीज",
                    "प्री-डायबिटीज, मेटाबॉलिक सिंड्रोम या पीसीओडी से पीड़ित महिलाएं",
                    "सॉफ्ट ड्रिंक्स, पैकेज्ड जूस और बेकरी उत्पादों के नियमित उपभोक्ता"
                ),
                relevantConcerns = listOf("सामान्य थकान", "मेटाबॉलिक स्वास्थ्य", "पोषण की कमी"),
                personalizedSuggestions = listOf(
                    "कोल्ड ड्रिंक्स की जगह नींबू पानी, छाछ या नारियल पानी पिएं।",
                    "मीठे की लालसा होने पर मौसमी फल, 1-2 खजूर या मुट्ठी भर मेवे खाएं।",
                    "पैकेज्ड फूड खरीदते समय लेबल पर छिपी हुई शुगर की जांच करें।",
                    "चाय-कॉफी में चीनी की मात्रा को धीरे-धीरे आधा करें।"
                ),
                detailedBenefits = listOf(
                    "फैटी लिवर का खतरा काफी कम होता है।",
                    "ब्लड शुगर स्थिर रहती है और डायबिटीज से बचाव होता है।",
                    "त्वचा की चमक सुधरती है और मुंहासों में कमी आती है।",
                    "ऊर्जा का स्तर पूरे दिन स्थिर बना रहता है।"
                ),
                precautions = listOf(
                    "डायबिटीज की दवा ले रहे मरीज चीनी घटाते समय शुगर लेवल नियमित मापें।",
                    "कृत्रिम मिठास का अत्यधिक उपयोग करने से बचें।",
                    "अचानक बहुत सख्त उपवास करने की बजाय धीरे-धीरे आदत बदलें।"
                ),
                whenToConsultDoctor = "यदि अत्यधिक प्यास, बार-बार पेशाब आना और अनियंत्रित वजन घटने के लक्षण दिखें तो तुरंत चिकित्सक से परामर्श करें।"
            )
        ),

        // Tip 4: Mindful Eating & Slow Chewing (Nutrition)
        TipDetailBundle(
            tipId = 4,
            relatedTipIds = listOf(1, 2, 14),
            english = LocalizedTipDetails(
                whyUseful = "Thorough mastication mixes food with salivary amylase and lingual lipase while allowing the 20-minute hormonal feedback loop of leptin and CCK to register satiety in the hypothalamus.",
                targetAudience = listOf(
                    "People suffering from functional dyspepsia, post-meal bloating, and heartburn",
                    "Individuals who habitually eat while working at computers or watching screens",
                    "Those struggling with portion control and habitual overeating",
                    "Anyone who finishes complete meals in under 10 minutes"
                ),
                relevantConcerns = listOf("Digestive Discomfort", "Metabolic Balance", "General Nutrition"),
                personalizedSuggestions = listOf(
                    "Place your utensils down between each bite to consciously slow your pace.",
                    "Chew solid food until it reaches a semi-liquid consistency before swallowing.",
                    "Avoid stressful conversations, social media scrolling, or news feeds during meal times.",
                    "Follow the 80% fullness guideline (Hara Hachi Bu): stop eating when comfortably nourished."
                ),
                detailedBenefits = listOf(
                    "Prevents gastric distension, post-meal belching, and acid reflux.",
                    "Maximizes enzymatic surface area for superior nutrient bioavailability.",
                    "Reduces caloric overconsumption naturally without restrictive dieting.",
                    "Lowers sympathetic stress response during autonomic digestion."
                ),
                precautions = listOf(
                    "Do not drink large quantities of ice-cold water during meals; it dilutes gastric enzymes.",
                    "Individuals with dysphagia (difficulty swallowing) must follow medical speech pathology guidance.",
                    "Chewing slowly should feel calm and rhythmic, not stressful or rigid."
                ),
                whenToConsultDoctor = "Consult a gastroenterologist if you experience persistent painful swallowing, recurrent vomiting, or chronic heartburn unresponsive to lifestyle modifications."
            ),
            marathi = LocalizedTipDetails(
                whyUseful = "घास नीट चावून खाल्ल्याने लाळेतील पाचक रस अन्नात पूर्णपणे मिसळतात आणि मेंदूला पोट भरल्याचे संकेत मिळण्यासाठी लागणारा २० मिनिटांचा वेळ मिळतो.",
                targetAudience = listOf(
                    "जेवणानंतर पोटात जळजळ (ॲसिडिटी), गॅसेस व अपचन होणारे लोक",
                    "टीव्ही किंवा मोबाईल पाहत घाईघाईत जेवणारे व्यक्ती",
                    "अति जेवणावर नियंत्रण ठेवू न शकणारे लोक",
                    "५ ते १० मिनिटांत भरभर जेवण संपवणारे"
                ),
                relevantConcerns = listOf("अपचन व पोट फुगणे", "चयापचय संतुलन", "पोषणाचा अभाव"),
                personalizedSuggestions = listOf(
                    "प्रत्येक घासानंतर चमचा किंवा पोळीचा तुकडा खाली ठेवा आणि सावकाश चावून खा.",
                    "अन्न तोंडात बारीक पाणी होईपर्यंत (किमान २०-२५ वेळा) चावून मगच गिळा.",
                    "जेवताना मोबाईल किंवा टीव्ही पाहणे पूर्णपणे बंद करा.",
                    "पोट ८०% भरल्याची जाणीव होताच जेवणे थांबवा, अति खाणे टाळा."
                ),
                detailedBenefits = listOf(
                    "ॲसिडिटी, आंबट ढेकर आणि छातीतील जळजळ थांबते.",
                    "अन्नातील सर्व पोषक घटकांचे शरीरात पूर्ण शोषण होते.",
                    "अतिरिक्त खाणे टळून वजन नियंत्रणात राहते.",
                    "पोट हलके आणि शांत वाटते."
                ),
                precautions = listOf(
                    "जेवताना एकाच वेळी खूप थंड पाणी पिऊ नका, यामुळे पाचक रस पातळ होतात.",
                    "अन्न गिळताना त्रास होत असल्यास स्वतःहून उपचार न करता डॉक्टरांना भेटा.",
                    "सावकाश जेवणे ही सवय शांतपणे अंगीकारा, तणाव घेऊन नाही."
                ),
                whenToConsultDoctor = "घास गिळताना अडकल्यासारखे वाटणे, वारंवार उलट्या होणे किंवा तीव्र छातीत जळजळ होत असल्यास ताबडतोब डॉक्टरांचा सल्ला घ्या."
            ),
            hindi = LocalizedTipDetails(
                whyUseful = "भोजन को अच्छी तरह चबाने से लार के पाचक एंजाइम्स भोजन में मिलते हैं और मस्तिष्क के तृप्ति केंद्र को सही समय पर संकेत मिलता है।",
                targetAudience = listOf(
                    "भोजन के बाद एसिडिटी, खट्टी डकार और पेट फूलने से ग्रस्त लोग",
                    "मोबाइल या टीवी देखते हुए जल्दबाजी में भोजन करने वाले व्यक्ति",
                    "अधिक भोजन की आदत से परेशान लोग",
                    "10 मिनट से कम समय में भोजन खत्म करने वाले व्यक्ति"
                ),
                relevantConcerns = listOf("पाचन संबंधी असुविधा", "मेटाबॉलिक संतुलन", "सामान्य पोषण"),
                personalizedSuggestions = listOf(
                    "हर निवाले को मुंह में रखकर 20 से 25 बार अच्छी तरह चबाएं।",
                    "भोजन के समय स्क्रीन, टीवी और फोन को पूरी तरह दूर रखें।",
                    "पेट 80% भरने पर भोजन समाप्त करने का नियम बनाएं।",
                    "भोजन करते समय मन शांत रखें और स्वाद का आनंद लें।"
                ),
                detailedBenefits = listOf(
                    "एसिडिटी और सीने की जलन से प्राकृतिक राहत मिलती है।",
                    "पोषक तत्वों का शरीर में अवशोषण बेहतर होता है।",
                    "वजन संतुलित रहता है और मोटापा रुकता है।",
                    "पाचन तंत्र पर अतिरिक्त दबाव नहीं पड़ता।"
                ),
                precautions = listOf(
                    "भोजन के बीच में बहुत ज्यादा ठंडा पानी न पिएं।",
                    "निगलने में दर्द या रुकावट महसूस होने पर तुरंत डॉक्टर से जांच कराएं।",
                    "चबाने की प्रक्रिया को तनावपूर्ण न बनाएं, सहजता से खाएं।"
                ),
                whenToConsultDoctor = "यदि भोजन निगलने में लगातार कठिनाई, खून की उल्टी या पेट में तीव्र जलन बनी रहे तो तुरंत विशेषज्ञ से मिलें।"
            )
        ),

        // Tip 5: Anti-Inflammatory Culinary Spices (Nutrition)
        TipDetailBundle(
            tipId = 5,
            relatedTipIds = listOf(1, 2, 18),
            english = LocalizedTipDetails(
                whyUseful = "Bioactive phytochemicals such as curcumin (turmeric), gingerol (ginger), and allicin (garlic) inhibit NF-kB inflammatory cascades and scavenge oxidative reactive oxygen species.",
                targetAudience = listOf(
                    "Individuals with mild joint stiffness, muscular aches, or osteoarthritic discomfort",
                    "People seeking natural immune modulation against recurring seasonal colds",
                    "Those looking to enhance circulatory vitality and vascular endothelial health",
                    "Individuals aiming to protect skin health and tone through diet"
                ),
                relevantConcerns = listOf("Dry Skin", "Nutritional Gaps", "Joint Mobility"),
                personalizedSuggestions = listOf(
                    "Always pair turmeric with a tiny pinch of freshly ground black pepper (piperine dramatically boosts absorption).",
                    "Simmer fresh crushed ginger and a cinnamon stick in warm water for an afternoon restorative tea.",
                    "Crush raw garlic cloves and let them rest for 5 minutes before cooking to activate allicin.",
                    "Use spices as natural whole culinary seasonings rather than megadosing unverified extracts."
                ),
                detailedBenefits = listOf(
                    "Suppresses systemic inflammatory cytokines (TNF-alpha and IL-6).",
                    "Protects joint synovial membranes and eases stiffness upon waking.",
                    "Shields dermal collagen from oxidative UV damage, supporting skin glow.",
                    "Provides antimicrobial and immune-supportive properties against pathogens."
                ),
                precautions = listOf(
                    "Individuals on prescription anticoagulant or antiplatelet blood thinners must avoid high-dose spice extracts.",
                    "Those with active gallstones or bile duct obstruction should avoid excessive concentrated turmeric.",
                    "Excessive raw ginger or garlic on an empty stomach can trigger mucosal gastric irritation."
                ),
                whenToConsultDoctor = "Consult a physician if joint stiffness is accompanied by persistent warmth, significant swelling, redness, fever, or morning rigidity lasting over 60 minutes."
            ),
            marathi = LocalizedTipDetails(
                whyUseful = "हळद, आले आणि लसूण यांमधील नैसर्गिक औषधी घटक शरीरातील सूज कमी करतात, पेशींचे ऑक्सिडेटिव्ह ताणापासून रक्षण करतात आणि प्रतिकारशक्ती वाढवतात.",
                targetAudience = listOf(
                    "सांधेदुखी, स्नायू आखडणे किंवा सकाळी उठल्यावर अंगदुखी असणारे लोक",
                    "वारंवार सर्दी-खोकला व संसर्ग होणारे व्यक्ती",
                    "रक्ताभिसरण आणि त्वचेचा तजेला सुधारू इच्छिणारे",
                    "नैसर्गिक मसाल्यांनी प्रतिकारशक्ती वाढवू इच्छिणारे"
                ),
                relevantConcerns = listOf("त्वचेचा कोरडेपणा", "पोषणाचा अभाव", "सांधेदुखी"),
                personalizedSuggestions = listOf(
                    "हळदीसोबत नेहमी चिमूटभर काळी मिरी वापरा, ज्यामुळे हळदीचे शोषण अनेक पटीने वाढते.",
                    "दुपारी चहाऐवजी आले आणि दालचिनी घातलेले कोमट पाणी किंवा काढा प्या.",
                    "लसूण चिरल्यानंतर ५ मिनिटे थांबून मगच फोडणीत घाला, ज्यामुळे त्यातील ॲलिसिन सक्रिय होते.",
                    "मसाले जेवणात योग्य प्रमाणात वापरा, अति प्रमाणात पावडर किंवा गोळ्या खाऊ नका."
                ),
                detailedBenefits = listOf(
                    "शरीरातील अंतर्गत सूज व सांध्यांचे आखडणे कमी होते.",
                    "त्वचेचे संरक्षण होऊन चेहऱ्यावरील चमक टिकून राहते.",
                    "हंगामी सर्दी-पडशाविरुद्ध नैसर्गिक प्रतिकारशक्ती मिळते.",
                    "रक्तवाहिन्यांचे आरोग्य निरोगी राहते."
                ),
                precautions = listOf(
                    "रक्त पातळ करणारी औषधे घेणाऱ्यांनी जास्त प्रमाणात मसाल्यांचे अर्क घेऊ नयेत.",
                    "पित्ताशयात खडे असणाऱ्यांनी हळदीचा अतिरेक टाळावा.",
                    "रिकाम्या पोटी जास्त तिखट आले किंवा कच्चा लसूण खाल्ल्यास जळजळ होऊ शकते."
                ),
                whenToConsultDoctor = "सांध्यांवर तीव्र सूज, लालसरपणा, ताप किंवा सांधे हलवताच न येणे अशी लक्षणे दिसल्यास तातडीने संधिवात तज्ज्ञांचा सल्ला घ्या."
            ),
            hindi = LocalizedTipDetails(
                whyUseful = "हल्दी का करक्यूमिन, अदरक का जिंजरॉल और लहसुन का एलिसिन शरीर की कोशिकाओं की सूजन को घटाते हैं और एंटीऑक्सीडेंट सुरक्षा प्रदान करते हैं।",
                targetAudience = listOf(
                    "जोड़ों के दर्द, मांसपेशियों में अकड़न या सुस्ती से परेशान लोग",
                    "बार-बार सर्दी-जुकाम और मौसमी संक्रमण से जूझने वाले व्यक्ति",
                    "त्वचा की चमक और रक्त संचार में सुधार चाहने वाले लोग",
                    "घरेलू मसालों से इम्युनिटी मजबूत करना चाहने वाले परिवार"
                ),
                relevantConcerns = listOf("त्वचा का रूखापन", "पोषण की कमी", "जोड़ों का स्वास्थ्य"),
                personalizedSuggestions = listOf(
                    "हल्दी के साथ हमेशा थोड़ी काली मिर्च लें ताकि करक्यूमिन का पूरा लाभ मिल सके।",
                    "अदरक और दालचीनी का गुनगुना काढ़ा दोपहर में पिएं।",
                    "लहसुन की कलियों को कूटने के बाद 5 मिनट रखकर ही भोजन में पकाएं।",
                    "मसालों को संतुलित मात्रा में भोजन का हिस्सा बनाएं, अत्यधिक सप्लीमेंट्स से बचें।"
                ),
                detailedBenefits = listOf(
                    "जोड़ों की सूजन कम होती है और लचीलापन बढ़ता है।",
                    "त्वचा में कोलेजन सुरक्षित रहता है और निखार आता है।",
                    "मौसमी बीमारियों के विरुद्ध रोग प्रतिरोधक क्षमता सुदृढ़ होती है।",
                    "हृदय और रक्त वाहिकाओं की सेहत अच्छी रहती है।"
                ),
                precautions = listOf(
                    "खून पतला करने की दवा ले रहे मरीज मसालों के अत्यधिक अर्क या कैप्सूल से बचें।",
                    "पित्त की थैली में पथरी होने पर अत्यधिक हल्दी न लें।",
                    "खाली पेट बहुत अधिक कच्चा लहसुन खाने से सीने में जलन हो सकती है।"
                ),
                whenToConsultDoctor = "यदि जोड़ों में तेज सूजन, गर्माहट, लालिमा या बुखार के साथ असहनीय दर्द हो तो तुरंत डॉक्टर से परामर्श लें।"
            )
        ),

        // Tip 6: 150 Minutes Weekly Aerobic Activity (Fitness)
        TipDetailBundle(
            tipId = 6,
            relatedTipIds = listOf(7, 8, 11),
            english = LocalizedTipDetails(
                whyUseful = "Rhythmic aerobic locomotion increases left ventricular end-diastolic volume, upregulates endothelial nitric oxide synthase (eNOS), and enhances mitochondrial density.",
                targetAudience = listOf(
                    "Sedentary individuals working desk or remote computer jobs",
                    "People with borderline blood pressure or elevated resting heart rate",
                    "Those seeking to increase cardiovascular stamina and lung endurance",
                    "Individuals managing mild stress, anxiety, or low daytime mood"
                ),
                relevantConcerns = listOf("Low Physical Activity", "General Tiredness", "Cardiovascular Fitness"),
                personalizedSuggestions = listOf(
                    "Break down the 150-minute target into brisk 30-minute walks 5 days a week.",
                    "Maintain a conversational pace: you should be able to speak full sentences but not sing.",
                    "Incorporate outdoor brisk walking, cycling, or stair climbing into daily commutes.",
                    "Wear well-cushioned supportive footwear to protect knee and ankle joints."
                ),
                detailedBenefits = listOf(
                    "Lowers resting systolic and diastolic blood pressure naturally.",
                    "Elevates circulating endorphins and brain-derived neurotrophic factor (BDNF).",
                    "Improves cellular insulin sensitivity and glycogen turnover.",
                    "Strengthens respiratory diaphragm and pulmonary ventilation capacity."
                ),
                precautions = listOf(
                    "Individuals with existing ischemic heart disease or arrhythmia must obtain medical clearance before initiating aerobic regimens.",
                    "Avoid vigorous high-intensity exercise outdoors during extreme heat or severe air pollution.",
                    "Stop immediately if you experience dizziness, chest tightness, or radiating arm pain."
                ),
                whenToConsultDoctor = "Seek urgent emergency medical attention if physical activity causes acute substernal chest pressure, radiation of pain to the jaw or left arm, severe shortness of breath, or syncope (fainting)."
            ),
            marathi = LocalizedTipDetails(
                whyUseful = "नियमित चालणे व एरोबिक व्यायामामुळे हृदयाचे स्नायू बळकट होतात, रक्तवाहिन्या लवचिक राहतात आणि फुफ्फुसांची ऑक्सिजन घेण्याची क्षमता वाढते.",
                targetAudience = listOf(
                    "दिवसभर बसून काम करणारे कर्मचारी व आयटी व्यावसायिक",
                    "रक्तदाब वाढू लागलेले किंवा हृदयाचे ठोके जलद असणारे लोक",
                    "शरीराचा स्टॅमिना व ऊर्जा वाढवू इच्छिणारे",
                    "मानसिक ताण व चिंतेपासून आराम मिळवू इच्छिणारे"
                ),
                relevantConcerns = listOf("शारीरिक निष्क्रियता", "सततचा थकवा", "हृदय स्वास्थ्य"),
                personalizedSuggestions = listOf(
                    "आठवड्यातून ५ दिवस दररोज ३० मिनिटे जलद चाला किंवा सायकल चालवा.",
                    "चालण्याचा वेग असा असावा की तुम्हाला बोलता यायला हवे, पण धाप लागून गाता येऊ नये.",
                    "सकाळी मोकळ्या बागेत किंवा संध्याकाळी जेवणानंतर शतपावली करा.",
                    "गुडघे व घोट्यांचे रक्षण करण्यासाठी योग्य पादत्राणे (स्पोर्ट्स शूज) वापरा."
                ),
                detailedBenefits = listOf(
                    "रक्तदाब नैसर्गिकरित्या नियंत्रणात राहतो.",
                    "मेंदूमध्ये उत्साहवर्धक हार्मोन्स पाझरतात आणि तणाव दूर होतो.",
                    "रक्तातील साखरेचे नियंत्रण सुधारून मधुमेह टळतो.",
                    "रात्री शांत व गाढ झोप लागण्यास मदत होते."
                ),
                precautions = listOf(
                    "हृदयविकार किंवा दम्याचा त्रास असणाऱ्यांनी डॉक्टरांच्या सल्ल्यानेच व्यायामाची तीव्रता ठरवावी.",
                    "दुपारच्या कडक उन्हात किंवा जास्त प्रदूषणात धावणे टाळा.",
                    "व्यायामादरम्यान चक्कर आल्यास किंवा छातीत दुखल्यास लगेच थांबा."
                ),
                whenToConsultDoctor = "चालताना छातीत कळ येणे, डाव्या हातात किंवा जबड्यात वेदना पसरणे, तीव्र धाप लागणे किंवा चक्कर येऊन पडल्यास तात्काळ कार्डिओलॉजिस्ट किंवा आपत्कालीन विभागाशी संपर्क साधा."
            ),
            hindi = LocalizedTipDetails(
                whyUseful = "नियमित एरोबिक व्यायाम दिल की पंपिंग क्षमता बढ़ाता है, धमनियों को लचीला रखता है और शरीर में ऑक्सीजन का स्तर सुधारता है।",
                targetAudience = listOf(
                    "कुर्सी पर बैठकर लंबे समय तक काम करने वाले कर्मचारी",
                    "हल्के उच्च रक्तचाप या अधिक विश्राम हृदय गति वाले लोग",
                    "शारीरिक सहनशक्ति और सांस की क्षमता बढ़ाने के इच्छुक व्यक्ति",
                    "तनाव और सुस्ती से राहत पाने के इच्छुक लोग"
                ),
                relevantConcerns = listOf("शारीरिक निष्क्रियता", "सामान्य थकान", "कार्डियोवैस्कुलर फिटनेस"),
                personalizedSuggestions = listOf(
                    "सप्ताह में कम से कम 5 दिन 30 मिनट तेज गति से टहलें।",
                    "गति ऐसी रखें जिससे बात करना संभव हो, लेकिन सांस पूरी तरह न फूले।",
                    "लिफ्ट की जगह सीढ़ियों का उपयोग करें और साइकिल चलाएं।",
                    "पैरों और घुटनों की सुरक्षा के लिए अच्छे जूते पहनें।"
                ),
                detailedBenefits = listOf(
                    "रक्तचाप सामान्य और नियंत्रित रहता है।",
                    "मस्तिष्क में ताजगी और सकारात्मक ऊर्जा का संचार होता है।",
                    "ब्लड शुगर नियंत्रित होती है और फैट बर्न होता है।",
                    "फेफड़ों की कार्यक्षमता और सहनशक्ति बढ़ती है।"
                ),
                precautions = listOf(
                    "हृदय रोग के मरीज व्यायाम शुरू करने से पहले डॉक्टर से जांच कराएं।",
                    "अत्यधिक धूप या प्रदूषण में कसरत न करें।",
                    "सीने में असहजता या चक्कर आने पर तुरंत रुक जाएं।"
                ),
                whenToConsultDoctor = "कसरत के दौरान सीने में दबाव, जबड़े या बाएं हाथ में दर्द, अत्यधिक सांस फूलना या बेहोशी जैसे लक्षण दिखें तो तत्काल आपातकालीन सहायता लें।"
            )
        ),

        // Tip 7: Progressive Resistance & Strength Training (Fitness)
        TipDetailBundle(
            tipId = 7,
            relatedTipIds = listOf(6, 9, 10),
            english = LocalizedTipDetails(
                whyUseful = "Mechanical tension stimulated by resistance exercises triggers myofibrillar protein synthesis via mTOR pathways and creates piezoelectric osteoblast stimulation for bone remineralization.",
                targetAudience = listOf(
                    "Adults over 30 seeking to prevent age-related muscle wasting (sarcopenia)",
                    "Post-menopausal women seeking to maintain bone mineral density against osteoporosis",
                    "Individuals with joint hypermobility or poor muscular stability",
                    "Those aiming to increase resting metabolic rate and functional strength"
                ),
                relevantConcerns = listOf("Low Physical Activity", "Bone & Joint Density", "Metabolic Rate"),
                personalizedSuggestions = listOf(
                    "Begin with functional bodyweight movements: assisted chair squats, wall push-ups, and glute bridges.",
                    "Perform 2 to 3 sets of 8 to 12 repetitions targeting major muscle groups twice weekly.",
                    "Allow 48 hours of recovery between working the exact same muscle group.",
                    "Focus strictly on controlled movement tempo rather than lifting heavy excessive weights."
                ),
                detailedBenefits = listOf(
                    "Preserves skeletal muscle mass and functional physical autonomy.",
                    "Stimulates bone trabecular density, preventing osteoporotic fractures.",
                    "Increases GLUT-4 transporter expression, optimizing glucose disposal.",
                    "Improves joint stability and diminishes chronic arthritic pain."
                ),
                precautions = listOf(
                    "Individuals with uncontrolled severe hypertension must avoid holding their breath while straining.",
                    "Maintain strict spinal neutrality to protect intervertebral discs.",
                    "Always warm up joints thoroughly prior to resistance work."
                ),
                whenToConsultDoctor = "Consult an orthopedic specialist or physical therapist if you experience sharp localized joint pain, swelling, or audible joint popping accompanied by instability."
            ),
            marathi = LocalizedTipDetails(
                whyUseful = "स्ट्रेंथ ट्रेनिंगमुळे स्नायूंची ताकद टिकून राहते, हाडांची घनता वाढून ऑस्टिओपोरोसिसचा धोका टळतो आणि शरीरातील चयापचय (मेटाबॉलिझम) वेगवान होते.",
                targetAudience = listOf(
                    "३० वर्षांवरील व्यक्ती ज्यांना स्नायूंची ताकद टिकवायची आहे",
                    "हाडे ठिसूळ होण्याची शक्यता असणाऱ्या महिला",
                    "अंग कमकुवत वाटणारे किंवा शरीराचा तोल न सांभाळता येणारे",
                    "वजन आणि फॅट्स नियंत्रणात ठेवू इच्छिणारे"
                ),
                relevantConcerns = listOf("शारीरिक निष्क्रियता", "हाडांची बळकटी", "चयापचय दर"),
                personalizedSuggestions = listOf(
                    "सुरुवातीला स्वतःच्या वजनावर आधारित सोपे व्यायाम करा—जसे की चेअर स्क्वॅट्स, वॉल पुश-अप्स.",
                    "आठवड्यातून २ ते ३ दिवस प्रत्येकी २०-२५ मिनिटे स्नायूंचे व्यायाम करा.",
                    "एकाच स्नायूवर सलग दोन दिवस ताण देऊ नका, स्नायूंना विश्रांती आवश्यक आहे.",
                    "जड वजने उचलण्यापेक्षा योग्य पद्धत पाळण्यावर लक्ष द्या."
                ),
                detailedBenefits = listOf(
                    "वय वाढताना स्नायू कमजोर होणे टळते.",
                    "हाडे मजबूत होऊन फ्रॅक्चरचा धोका कमी होतो.",
                    "शरीराची ऊर्जा वापरण्याची क्षमता वाढून चरबी जळते.",
                    "दैनंदिन कामे करताना थकवा जाणवत नाही."
                ),
                precautions = listOf(
                    "उच्च रक्तदाब असणाऱ्यांनी व्यायाम करताना श्वास रोखून धरू नये.",
                    "पाठीचा कणा नेहमी ताठ ठेवा, वाकून जड व्यायाम करू नका.",
                    "व्यायामापूर्वी सांध्यांची हलकी हालचाल करणे अनिवार्य आहे."
                ),
                whenToConsultDoctor = "व्यायामानंतर सांध्यात तीव्र टोचणारी वेदना, सूज किंवा अवयव हलवता न आल्यास त्वरित डॉक्टरांचा सल्ला घ्या."
            ),
            hindi = LocalizedTipDetails(
                whyUseful = "प्रतिरोध प्रशिक्षण मांसपेशियों को मजबूत बनाता है, हड्डियों के घनत्व को बढ़ाता है और शरीर के कैलोरी बर्न करने की दर को तेज करता है।",
                targetAudience = listOf(
                    "मांसपेशियों की कमजोरी और थकान से परेशान वयस्क",
                    "हड्डियों की कमजोरी (ऑस्टियोपोरोसिस) से बचाव चाहने वाली महिलाएं",
                    "शरीर का संतुलन और शारीरिक बल बढ़ाने के इच्छुक लोग",
                    "मेटाबॉलिज्म तेज कर चर्बी घटाना चाहने वाले व्यक्ति"
                ),
                relevantConcerns = listOf("शारीरिक निष्क्रियता", "हड्डियों की मजबूती", "मेटाबॉलिक दर"),
                personalizedSuggestions = listOf(
                    "शुरुआत में बॉडीवेट व्यायाम करें—जैसे दीवार के सहारे पुश-अप्स और चेयर स्क्वैट्स।",
                    "सप्ताह में 2 से 3 दिन 20-30 मिनट मांसपेशियों का व्यायाम करें।",
                    "व्यायाम करते समय गति को नियंत्रित रखें और सांस सामान्य रखें।",
                    "एक ही मांसपेशी समूह को लगातार दो दिन कसरत न कराएं।"
                ),
                detailedBenefits = listOf(
                    "उम्र बढ़ने के साथ मांसपेशियों की मजबूती बनी रहती है।",
                    "हड्डियां सघन व मजबूत होती हैं।",
                    "इंसुलिन संवेदनशीलता सुधरती है और शुगर घटती है।",
                    "जोड़ों का दर्द और जकड़न कम होती है।"
                ),
                precautions = listOf(
                    "हाई ब्लड प्रेशर के मरीज भारी वजन उठाते समय सांस न रोकें।",
                    "रीढ़ की हड्डी को हमेशा सीधा रखें और झुककर वजन न उठाएं।",
                    "कसरत से पहले जोड़ों को वार्म-अप करना न भूलें।"
                ),
                whenToConsultDoctor = "यदि व्यायाम के बाद जोड़ों में तीव्र दर्द, सूजन या चलने में असमर्थता हो तो तुरंत हड्डी रोग विशेषज्ञ से संपर्क करें।"
            )
        )
    )

    private val detailsList2: List<TipDetailBundle> = listOf(
        // Tip 8: Desk Mobility & The 20-20-20 Rule (Fitness)
        TipDetailBundle(
            tipId = 8,
            relatedTipIds = listOf(6, 9, 15),
            english = LocalizedTipDetails(
                whyUseful = "Breaking up prolonged sedentary postures restores microvascular blood flow to the lower limbs and resets the ciliary eye muscles to prevent digital asthenopia.",
                targetAudience = listOf(
                    "Knowledge workers and students seated in front of monitors for over 6 hours daily",
                    "Individuals suffering from frequent dry eyes, blurred vision, or tension headaches",
                    "People with lower leg heaviness, calf tightness, or dependent ankle edema",
                    "Anyone struggling with midday cognitive fatigue and physical sluggishness"
                ),
                relevantConcerns = listOf("Low Physical Activity", "Dry Skin & Eye Strain", "General Tiredness"),
                personalizedSuggestions = listOf(
                    "Set an hourly timer to stand up, roll your shoulders, and walk for 90 seconds.",
                    "Every 20 minutes, gaze at an object situated 20 feet away for 20 continuous seconds.",
                    "Perform 10 seated ankle pumps and calf raises every hour to stimulate the venous calf pump.",
                    "Consciously blink completely: screen users blink 60% less frequently than normal."
                ),
                detailedBenefits = listOf(
                    "Prevents venous blood stasis and reduces deep vein thrombosis risk.",
                    "Relieves spasm of the ciliary ocular muscles, eliminating digital eye strain.",
                    "Decompresses intervertebral lumbar discs and eases gluteal muscle tightness.",
                    "Restores cerebral arterial blood flow, boosting focus and mental clarity."
                ),
                precautions = listOf(
                    "Avoid sudden jerky neck twists; keep cervical spine movements smooth and slow.",
                    "Position screens roughly arm's length away with the top third at eye level.",
                    "Ensure workplace lighting does not create direct glare on digital monitors."
                ),
                whenToConsultDoctor = "Consult an ophthalmologist if eye strain is accompanied by persistent double vision, sudden visual field loss, or excruciating eye pain."
            ),
            marathi = LocalizedTipDetails(
                whyUseful = "सतत बसून काम केल्याने पायात रक्त साचते आणि स्क्रीनकडे सतत पाहिल्याने डोळ्यांचे स्नायू आखडतात. थोड्या-थोड्या अंतराने हालचाल केल्याने हे सर्व त्रास टळतात.",
                targetAudience = listOf(
                    "दिवसातून ६ पेक्षा जास्त तास लॅपटॉप किंवा संगणकावर काम करणारे",
                    "डोळे कोरडे पडणे, लाल होणे किंवा डोकेदुखी होणारे लोक",
                    "पायांना सूज येणे किंवा पाय जड वाटणारे कर्मचारी",
                    "दुपारी काम करताना कामात लक्ष न लागणे आणि कंटाळा येणारे"
                ),
                relevantConcerns = listOf("शारीरिक निष्क्रियता", "डोळ्यांचा ताण", "सततचा थकवा"),
                personalizedSuggestions = listOf(
                    "दर ४५ मिनिटांनी जागेवरून उठून २ मिनिटे थोडे चाला आणि हात-पाय ताणून घ्या.",
                    "दर २० मिनिटांनी २० फूट अंतरावरील वस्तूवर २० सेकंद लक्ष केंद्रित करा.",
                    "बसल्या जागी घोटे गोल फिरवा आणि पायांच्या टाचा वर-खाली करा.",
                    "स्क्रीनकडे पाहताना डोळ्यांची नियमित उघडझाप जाणीवपूर्वक करा."
                ),
                detailedBenefits = listOf(
                    "पायातील रक्तप्रवाह सुरळीत राहून शिरांचे आजार टळतात.",
                    "डोळ्यांचा ताण, जळजळ आणि डोकेदुखी दूर होते.",
                    "मणक्यावरील ताण कमी होऊन पाठदुखी टळते.",
                    "मेंदूला ताजी ऑक्सिजन मिळून कामात एकाग्रता वाढते."
                ),
                precautions = listOf(
                    "मानेला अचानक जोराचा झटका देऊन फिरवू नका, हालचाल हळुवार करा.",
                    "स्क्रीन डोळ्यांपासून एक हात लांब आणि डोळ्यांच्या किंचित खाली ठेवा.",
                    "खोलीतील दिव्यांचा थेट प्रकाश स्क्रीनवर पडणार नाही याची काळजी घ्या."
                ),
                whenToConsultDoctor = "एकाच वस्तू दोन दिसणे (डबल व्हिजन), डोळ्यांत तीव्र वेदना किंवा दृष्टी अचानक अंधुक झाल्यास ताबडतोब नेत्रतज्ज्ञांना भेटा."
            ),
            hindi = LocalizedTipDetails(
                whyUseful = "लगातार बैठे रहने से पैरों में खून का दौरा धीमा पड़ता है और स्क्रीन की रोशनी आंखों की मांसपेशियों को थका देती है। छोटे ब्रेक इस नुकसान को रोकते हैं।",
                targetAudience = listOf(
                    "कंप्यूटर पर 6 घंटे से अधिक समय बिताने वाले पेशेवर और छात्र",
                    "आंखों में सूखापन, जलन या सिरदर्द से परेशान लोग",
                    "पैरों में भारीपन या पिंडलियों में अकड़न महसूस करने वाले व्यक्ति",
                    "काम के बीच में एकाग्रता खोने वाले लोग"
                ),
                relevantConcerns = listOf("शारीरिक निष्क्रियता", "आंखों का तनाव", "सामान्य थकान"),
                personalizedSuggestions = listOf(
                    "प्रत्येक 45 मिनट में 1-2 मिनट खड़े होकर स्ट्रेचिंग करें।",
                    "हर 20 मिनट पर 20 फीट दूर किसी वस्तु को 20 सेकंड तक देखें।",
                    "कुर्सी पर बैठे-बैठे पैरों के पंजों को ऊपर-नीचे और गोल घुमाएं।",
                    "काम करते समय पलकें बार-बार झपकाते रहें।"
                ),
                detailedBenefits = listOf(
                    "पैरों में ब्लड सर्कुलेशन सही रहता है और सूजन नहीं आती।",
                    "आंखों की थकान और कंप्यूटर विजन सिंड्रोम से बचाव होता है।",
                    "कमर और गर्दन का तनाव कम होता है।",
                    "दिमागी ताजगी और कार्यकुशलता बनी रहती है।"
                ),
                precautions = listOf(
                    "गर्दन को तेजी से झटका देकर न घुमाएं।",
                    "स्क्रीन को आंखों से लगभग 20-24 इंच दूर रखें।",
                    "स्क्रीन पर सीधे पड़ने वाली तेज रोशनी से बचें।"
                ),
                whenToConsultDoctor = "यदि आंखों में गंभीर दर्द, अचानक धुंधलापन या कोई वस्तु दोहरी दिखाई दे, तो तुरंत नेत्र विशेषज्ञ से संपर्क करें।"
            )
        ),

        // Tip 9: Core Stability & Ergonomic Posture (Fitness)
        TipDetailBundle(
            tipId = 9,
            relatedTipIds = listOf(7, 8, 10),
            english = LocalizedTipDetails(
                whyUseful = "Neutral spine alignment balances gravitational loading across the intervertebral discs and prevents anterior pelvic tilt and cervical lordosis distortion.",
                targetAudience = listOf(
                    "People experiencing dull chronic low-back aching or lumbar strain",
                    "Individuals with rounded shoulders and forward-head ('tech neck') carriage",
                    "Those who drive or sit in unsupported chairs for prolonged hours",
                    "Anyone seeking to protect their spinal column health over a lifetime"
                ),
                relevantConcerns = listOf("Low Physical Activity", "Spine & Posture", "General Fatigue"),
                personalizedSuggestions = listOf(
                    "Adjust chair height so knees form a 90-degree angle with feet flat on the floor.",
                    "Use a small lumbar support cushion in the small of your lower back while seated.",
                    "Perform isometric pelvic tilts and bird-dog core stability holds on a yoga mat daily.",
                    "Keep telephone displays elevated near chest level rather than bending your chin downwards."
                ),
                detailedBenefits = listOf(
                    "Reduces shearing strain on lumbar intervertebral discs.",
                    "Prevents chronic cervical muscular spasm and tension headaches.",
                    "Expands the thoracic ribcage, allowing deeper diaphragm breathing.",
                    "Improves physical presence, balance, and core abdominal endurance."
                ),
                precautions = listOf(
                    "Never force your back into an exaggerated rigid arch; aim for a natural relaxed S-curve.",
                    "Avoid lifting heavy objects by bending at the waist—always bend at the hips and knees.",
                    "Do not ignore radiating numbness or tingling down the legs."
                ),
                whenToConsultDoctor = "Seek urgent medical attention if back pain radiates down past your knee with progressive foot weakness, or if accompanied by loss of bowel or bladder control."
            ),
            marathi = LocalizedTipDetails(
                whyUseful = "योग्य शारीरिक मुद्रेमुळे पाठीच्या कण्यावर अतिरिक्त ताण पडत नाही, मानेच्या नसा मोकळ्या राहतात आणि फुफ्फुसांना पूर्ण श्वास घेता येतो.",
                targetAudience = listOf(
                    "सतत कंबरदुखी किंवा पाठदुखीची तक्रार असणारे लोक",
                    "मान पुढे झुकवून आणि खांदे पाडून बसणारे व्यक्ती",
                    "लांब पल्ल्याचे ड्रायव्हिंग करणारे किंवा साध्या खुर्चीत बसणारे",
                    "मणक्याचे आजार टाळू इच्छिणारे सर्व वयोगटातील लोक"
                ),
                relevantConcerns = listOf("शारीरिक निष्क्रियता", "पाठीचा कणा", "सततचा थकवा"),
                personalizedSuggestions = listOf(
                    "खुर्चीची उंची अशी ठेवा की पाय जमिनीवर पूर्ण टेकतील आणि गुडघे ९० अंशात राहतील.",
                    "बसताना कमरेच्या मागे एक छोटी उशी (लंबर सपोर्ट) ठेवा.",
                    "दररोज ५ मिनिटे जमिनीवर झोपून पोटाचे व पाठीचे हलके व्यायाम करा.",
                    "मोबाईल वापरताना मान खाली वाकवण्याऐवजी मोबाईल डोळ्यांच्या पातळीवर धरा."
                ),
                detailedBenefits = listOf(
                    "मणक्यावरील आणि चकत्यांवरील ताण कमी होतो.",
                    "मानेच्या शिरा आखडणे आणि डोकेदुखी थांबते.",
                    "छाती मोकळी होऊन फुफ्फुसांची कार्यक्षमता वाढते.",
                    "शरीराचा तोल आणि चालण्याची ढब सुधारते."
                ),
                precautions = listOf(
                    "पाठीला अति ताठ करू नका, नैसर्गिक वळण ठेवा.",
                    "जड वस्तू उचलताना कमरेतून वाकू नका, गुडघ्यातून वाकून उचला.",
                    "पायात मुंग्या येत असल्यास दुर्लक्ष करू नका."
                ),
                whenToConsultDoctor = "कंबरेतील वेदना पायात खाली उतरत असल्यास, पायाची ताकद कमी झाल्यासारखी वाटल्यास किंवा लघवीवरील नियंत्रण सुटल्यास तात्काळ मणकेतज्ज्ञांना भेटा."
            ),
            hindi = LocalizedTipDetails(
                whyUseful = "रीढ़ की हड्डी को सीधी और प्राकृतिक स्थिति में रखने से मणकों पर दबाव नहीं पड़ता, गर्दन की नसें सुरक्षित रहती हैं और कमर दर्द से बचाव होता है।",
                targetAudience = listOf(
                    "कमर दर्द, गर्दन दर्द या पीठ की जकड़न से परेशान लोग",
                    "गर्दन झुकाकर मोबाइल या लैपटॉप चलाने वाले व्यक्ति",
                    "लंबे समय तक गाड़ी चलाने वाले या अनुचित कुर्सी पर बैठने वाले",
                    "रीढ़ की हड्डी को स्वस्थ रखना चाहने वाले सभी लोग"
                ),
                relevantConcerns = listOf("शारीरिक निष्क्रियता", "रीढ़ व पोस्चर", "सामान्य थकान"),
                personalizedSuggestions = listOf(
                    "कुर्सी की ऊंचाई ऐसी रखें कि पैर जमीन पर सीधे टिकें और घुटने 90 डिग्री पर हों।",
                    "कमर के निचले हिस्से के पीछे एक छोटा तकिया या लंबर सपोर्ट लगाएं।",
                    "प्रतिदिन 5-10 मिनट भुजंगासन या कोर मजबूत करने वाले व्यायाम करें।",
                    "मोबाइल का उपयोग करते समय उसे आंखों के स्तर पर उठाएं, गर्दन न झुकाएं।"
                ),
                detailedBenefits = listOf(
                    "रीढ़ के मणकों पर पड़ने वाला अनावश्यक दबाव हटता है।",
                    "गर्दन और कंधों का दर्द दूर होता है।",
                    "फेफड़े पूरी तरह फैलते हैं और सांस लेना आसान होता है।",
                    "शारीरिक संतुलन और आत्मविश्वास बढ़ता है।"
                ),
                precautions = listOf(
                    "पीठ को बहुत ज्यादा तानकर अकड़ाएं नहीं, स्वाभाविक मुद्रा रखें।",
                    "जमीन से भारी सामान उठाते समय कमर की बजाय घुटनों को मोड़ें।",
                    "पैरों में सुन्नपन या झुनझुनी को नजरअंदाज न करें।"
                ),
                whenToConsultDoctor = "यदि पीठ दर्द पैर के नीचे तक जाए, पैरों में कमजोरी महसूस हो या मल-मूत्र पर नियंत्रण प्रभावित हो, तो तुरंत रीढ़ विशेषज्ञ से संपर्क करें।"
            )
        ),

        // Tip 10: Dynamic Warm-Up & Cooldown Recovery (Fitness)
        TipDetailBundle(
            tipId = 10,
            relatedTipIds = listOf(6, 7, 9),
            english = LocalizedTipDetails(
                whyUseful = "Dynamic warmups elevate intra-muscular temperature, lower myofibrillar viscoelastic resistance, and prime neuromuscular motor unit firing.",
                targetAudience = listOf(
                    "Recreational athletes and runners prone to muscle pulls and hamstring strains",
                    "Individuals returning to fitness training after months of inactivity",
                    "Those who experience severe delayed onset muscle soreness (DOMS)",
                    "Anyone engaging in high-velocity or resistance workouts"
                ),
                relevantConcerns = listOf("Low Physical Activity", "Muscular Recovery", "Stiffness"),
                personalizedSuggestions = listOf(
                    "Spend 5-7 minutes on progressive mobility: hip rotations, leg swings, arm circles, and high knees.",
                    "Conclude every workout with 5 minutes of static floor stretches held for 20-30 seconds each.",
                    "Hydrate with lukewarm water and natural electrolytes immediately after vigorous training.",
                    "Include deep diaphragm breathing during cool-down to switch to recovery mode."
                ),
                detailedBenefits = listOf(
                    "Dramatically lowers the incidence of acute tendon and ligament ruptures.",
                    "Increases synovial fluid lubrication within joint capsules.",
                    "Accelerates systemic clearance of metabolic lactic acid.",
                    "Diminishes next-day muscular stiffness and soreness."
                ),
                precautions = listOf(
                    "Never bounce aggressively during static stretches.",
                    "Do not substitute static stretching for dynamic warm-up prior to high-power activity.",
                    "Respect pain signals: sharp stabbing joint sensations are an immediate signal to halt."
                ),
                whenToConsultDoctor = "Seek medical assessment if an exercise is followed by immediate joint swelling, an audible loud popping sound, inability to bear weight, or persistent bruising."
            ),
            marathi = LocalizedTipDetails(
                whyUseful = "व्यायामापूर्वी वॉर्म-अप केल्याने सांध्यांमध्ये वंगण तयार होते, स्नायू लवचिक बनतात आणि व्यायामानंतर कूल-डाऊन केल्याने स्नायू आखडणे टळते.",
                targetAudience = listOf(
                    "धावणे, सायकलिंग किंवा खेळ खेळणारे खेळाडू",
                    "अनेक दिवसांनंतर नव्याने व्यायाम सुरू करणारे लोक",
                    "व्यायामानंतर अंगदुखी व स्नायू आखडण्याचा त्रास होणारे",
                    "कोणत्याही प्रकारचा व्यायाम किंवा योगासने करणारे"
                ),
                relevantConcerns = listOf("शारीरिक निष्क्रियता", "स्नायूंची ताकद", "सांधेदुखी"),
                personalizedSuggestions = listOf(
                    "व्यायाम सुरू करण्यापूर्वी ५ मिनिटे खांदे, कंबर आणि घोट्यांची गोल हालचाल करा.",
                    "व्यायाम संपल्यावर ५ मिनिटे शांत बसून हलके स्ट्रेचिंग आणि दीर्घ श्वसन करा.",
                    "व्यायामानंतर कोमट पाणी किंवा ताक पिऊन शरीरातील पाण्याची कमतरता भरून काढा.",
                    "शरीराला घाईगडबडीत ताणू नका, हळूहळू लवचिकता वाढवा."
                ),
                detailedBenefits = listOf(
                    "स्नायू किंवा लिगामेंट दुखावण्याचा धोका अत्यंत कमी होतो.",
                    "सांध्यांमधील हालचाल सुलभ व वेदनाविरहित होते.",
                    "व्यायामानंतर दुसऱ्या दिवशी होणारी अंगदुखी टळते.",
                    "हृदयाचे ठोके हळूहळू सामान्य होतात."
                ),
                precautions = listOf(
                    "स्ट्रेचिंग करताना शरीराला झटके देऊ नका.",
                    "थेट जड व्यायामाला सुरुवात करू नका.",
                    "कोणत्याही हालचालीदरम्यान सांध्यात तीव्र कळ आल्यास ताबडतोब थांबा."
                ),
                whenToConsultDoctor = "व्यायामादरम्यान सांध्यातून कडकड असा आवाज येऊन तीव्र सूज आल्यास, पायावर भार देता येत नसल्यास त्वरित अस्थिरोग तज्ज्ञांना भेटा."
            ),
            hindi = LocalizedTipDetails(
                whyUseful = "कसरत से पहले वार्म-अप मांसपेशियों का तापमान बढ़ाकर उन्हें लचीला बनाता है और बाद में कूल-डाउन मांसपेशियों की जकड़न को रोकता है।",
                targetAudience = listOf(
                    "दौड़ने, जिम जाने या खेलकूद में भाग लेने वाले लोग",
                    "लंबे अंतराल के बाद दोबारा कसरत शुरू करने वाले व्यक्ति",
                    "कसरत के बाद तेज बदन दर्द से परेशान रहने वाले लोग",
                    "मांसपेशियों के खिंचाव से बचना चाहने वाले सभी लोग"
                ),
                relevantConcerns = listOf("शारीरिक निष्क्रियता", "मांसपेशियों का स्वास्थ्य", "जकड़न"),
                personalizedSuggestions = listOf(
                    "व्यायाम से पहले 5 मिनट जोड़ों को घुमाने और हल्का वार्म-अप करने में लगाएं।",
                    "व्यायाम खत्म होने पर 5 मिनट हल्की स्ट्रेचिंग और गहरी सांस लें।",
                    "कसरत के बाद थोड़ा पानी या नारियल पानी पीकर डिहाइड्रेशन से बचें।",
                    "स्ट्रेचिंग करते समय शरीर को झटके न दें, धीरे-धीरे खिंचाव दें।"
                ),
                detailedBenefits = listOf(
                    "मांसपेशियों के खिंचाव और चोट का खतरा घटता है।",
                    "जोड़ों का लचीलापन और कार्यक्षमता बढ़ती है।",
                    "कसरत के बाद होने वाला बदन दर्द कम होता है।",
                    "दिल की धड़कन सामान्य गति में आसानी से लौटती है।"
                ),
                precautions = listOf(
                    "बिना वार्म-अप के कभी भी भारी वजन या तेज दौड़ शुरू न करें।",
                    "स्ट्रेचिंग करते समय दर्द की सीमा से आगे जोर न लगाएं।",
                    "किसी भी जोड़ में अचानक तीव्र दर्द होने पर कसरत रोक दें।"
                ),
                whenToConsultDoctor = "यदि कसरत के दौरान जोड़ में तेज आवाज के साथ अचानक सूजन आए या पैर पर वजन न रखा जाए, तो तुरंत ऑर्थोपेडिक डॉक्टर से मिलें।"
            )
        ),

        // Tip 11: Consistent Circadian Sleep Schedule (Sleep)
        TipDetailBundle(
            tipId = 11,
            relatedTipIds = listOf(12, 13, 14),
            english = LocalizedTipDetails(
                whyUseful = "The suprachiasmatic nucleus requires regular photic and temporal cues to entrain nocturnal pineal melatonin release and coordinate peripheral organ clocks.",
                targetAudience = listOf(
                    "Individuals suffering from fragmented sleep, chronic insomnia, or erratic bedtimes",
                    "Shift workers or professionals with social jetlag between weekdays and weekends",
                    "People who wake up feeling chronically exhausted despite 8 hours in bed",
                    "Anyone struggling with morning brain fog and dependency on stimulants"
                ),
                relevantConcerns = listOf("Poor Sleep", "General Tiredness", "Mental Wellness"),
                personalizedSuggestions = listOf(
                    "Anchor your wake-up time: get out of bed at the exact same minute 7 days a week.",
                    "Step outdoors into direct morning sunlight for 10-15 minutes within an hour of waking.",
                    "If sleeping late on weekends, limit variance to under 45 minutes to prevent Sunday insomnia.",
                    "Establish a predictable 30-minute relaxing wind-down routine prior to lights out."
                ),
                detailedBenefits = listOf(
                    "Deepens restorative slow-wave Stage 3 non-REM physical sleep.",
                    "Synchronizes growth hormone surges for nocturnal cellular tissue repair.",
                    "Reduces sleep latency (the duration required to fall asleep).",
                    "Clears morning adenosine sleep inertia, boosting spontaneous alertness."
                ),
                precautions = listOf(
                    "Do not stay in bed tossing and turning for over 25 minutes; get up and do a quiet dim-light task.",
                    "Do not use daytime naps longer than 20 minutes if you struggle with nighttime insomnia.",
                    "Avoid sleeping with television or room lights turned on."
                ),
                whenToConsultDoctor = "Consult a sleep medicine specialist if you suffer from chronic insomnia lasting over 3 months, loud choking snorts during sleep, or uncontrollable daytime sleepiness."
            ),
            marathi = LocalizedTipDetails(
                whyUseful = "ठराविक वेळी झोपल्याने मेंदूतील जैविक घड्याळ योग्य राहते, मेलाटोनिन या झोपेच्या संप्रेरकाची निर्मिती वेळेवर होते आणि शरीराची झोपेत दुरुस्ती होते.",
                targetAudience = listOf(
                    "शांत व सलग झोप न लागणारे किंवा वारंवार जाग येणारे लोक",
                    "सुट्टीच्या दिवशी खूप उशिरा झोपणारे आणि उशिरा उठणारे",
                    "रात्री ७-८ तास झोपूनही सकाळी थकवा जाणवणारे",
                    "सकाळी उठण्यासाठी चहा-कॉफीवर अति अवलंबून असणारे"
                ),
                relevantConcerns = listOf("अपुरी झोप", "सततचा थकवा", "मानसिक स्वास्थ्य"),
                personalizedSuggestions = listOf(
                    "रोज सकाळी एकाच ठराविक वेळी उठण्याचा नियम करा (शनिवार-रविवारसुद्धा).",
                    "सकाळी उठल्यावर पहिल्या तासाभरात १५ मिनिटे कोवळ्या उन्हात चाला.",
                    "सुट्टीच्या दिवशी जास्तीत जास्त ३० ते ४५ मिनिटांपेक्षा जास्त उशीर करू नका.",
                    "झोपण्यापूर्वी ३० मिनिटे डोळे व मन शांत ठेवणारी सवय लावा."
                ),
                detailedBenefits = listOf(
                    "गाढ व शांत झोप लागून सकाळी ताजेतवाने वाटते.",
                    "झोपेत शरीराची आणि मेंदूची पुनर्निर्मिती वेगाने होते.",
                    "बिछान्यावर पडताच लवकर झोप लागण्यास मदत होते.",
                    "दिवसभर मन प्रसन्न आणि एकाग्र राहते."
                ),
                precautions = listOf(
                    "झोप येत नसेल तर बिछान्यावर तळमळत पडू नका, उठून अंधुक प्रकाशात पुस्तक वाचा.",
                    "दिवसा २० मिनिटांपेक्षा जास्त झोपू नका, अन्यथा रात्री झोपमोड होईल.",
                    "खोलीत दिवे किंवा टीव्ही चालू ठेवून झोपणे टाळा."
                ),
                whenToConsultDoctor = "३ महिन्यांपेक्षा जास्त काळ झोप न येणे, झोपेत घोरणे किंवा अचानक श्वास कोंडल्यासारखे वाटत असल्यास स्लीप स्पेशालिस्टचा सल्ला घ्या."
            ),
            hindi = LocalizedTipDetails(
                whyUseful = "सोने-जागने का एक ही समय शरीर की आंतरिक जैविक घड़ी को स्थिर रखता है, जिससे मेलाटोनिन समय पर निकलता है और नींद गहरी होती है।",
                targetAudience = listOf(
                    "अनिद्रा, रात में बार-बार जागने या बेचैनी से परेशान लोग",
                    "सप्ताहांत पर देर से सोने और देर से जागने वाले व्यक्ति",
                    "8 घंटे की नींद के बाद भी सुबह थकान महसूस करने वाले लोग",
                    "सुबह उठने में बहुत कठिनाई महसूस करने वाले व्यक्ति"
                ),
                relevantConcerns = listOf("खराब नींद", "सामान्य थकान", "मानसिक स्वास्थ्य"),
                personalizedSuggestions = listOf(
                    "सप्ताह के सातों दिन सुबह एक ही निश्चित समय पर बिस्तर छोड़ने का नियम बनाएं।",
                    "सुबह उठते ही 10-15 मिनट खुली धूप में जाएं।",
                    "सप्ताहांत पर भी उठने के समय में 30-45 मिनट से अधिक अंतर न रखें।",
                    "सोने से 30 मिनट पहले मन को शांत करने वाली दिनचर्या अपनाएं।"
                ),
                detailedBenefits = listOf(
                    "गहरी नींद मिलती है जिससे मानसिक थकान मिटती है।",
                    "कोशिकाओं की मरम्मत और विकास सुचारू रूप से होता है।",
                    "बिस्तर पर जाते ही जल्दी नींद आ जाती है।",
                    "दिनभर सतर्कता और ऊर्जा बनी रहती है।"
                ),
                precautions = listOf(
                    "नींद न आने पर बिस्तर पर करवटें न बदलते रहें, उठकर शांत वातावरण में बैठें।",
                    "दिन में 20 मिनट से लंबी झपकी न लें।",
                    "कमरे में तेज रोशनी जलाकर न सोएं।"
                ),
                whenToConsultDoctor = "यदि 3 महीने से अधिक समय से अनिद्रा बनी रहे या नींद में खर्राटे व सांस रुकने की समस्या हो तो तुरंत चिकित्सक से परामर्श लें।"
            )
        ),

        // Tip 12: Digital Sunset & Blue Light Curfew (Sleep)
        TipDetailBundle(
            tipId = 12,
            relatedTipIds = listOf(11, 13, 14),
            english = LocalizedTipDetails(
                whyUseful = "Blue-wavelength light (450-480 nm) directly excites intrinsically photosensitive retinal ganglion cells, suppressing natural melatonin synthesis by up to 85%.",
                targetAudience = listOf(
                    "People who scroll social media, watch shows, or answer emails in bed",
                    "Individuals who take 45+ minutes of frustrating tossing to fall asleep",
                    "Those waking up with dry, stinging eyes and heavy frontal headaches",
                    "Parents and teenagers with delayed sleep-phase syndrome"
                ),
                relevantConcerns = listOf("Poor Sleep", "Eye Strain", "Mental Wellness"),
                personalizedSuggestions = listOf(
                    "Enforce a firm Digital Sunset 60 minutes before scheduled sleep.",
                    "Charge smartphones outside the bedroom overnight or place them across the room.",
                    "Engage in non-digital evening rituals: light stretching, paper journaling, or physical book reading.",
                    "If screens are mandatory late at night, utilize warm-shift amber filters."
                ),
                detailedBenefits = listOf(
                    "Restores the physiological evening melatonin surge without synthetic pills.",
                    "Reduces sleep onset latency, helping you drift off naturally within 15 minutes.",
                    "Eliminates pre-sleep dopamine overstimulation and anxiety spikes.",
                    "Protects cornea and ciliary eye muscles from digital fatigue."
                ),
                precautions = listOf(
                    "Blue-blocking glasses are not a license to stare at screens until late night.",
                    "Avoid emotionally charged media, distressing news, or work conflicts right before bed.",
                    "Keep bedroom ambient lighting dim and warm-hued in the late evening."
                ),
                whenToConsultDoctor = "Consult a physician if insomnia persists despite strict digital curfews, or if you suspect an underlying mood or anxiety disorder."
            ),
            marathi = LocalizedTipDetails(
                whyUseful = "मोबाईल आणि टीव्हीच्या स्क्रीनमधून निघणारा निळा प्रकाश मेंदूतील मेलाटोनिन तयार होण्याची प्रक्रिया रोखतो, ज्यामुळे मेंदूला रात्र झाल्याचे समजत नाही आणि झोप येत नाही.",
                targetAudience = listOf(
                    "झोपताना बिछान्यात मोबाईल किंवा सोशल मीडिया वापरणारे लोक",
                    "झोपायला गेल्यानंतर तासनतास डोळ्याला डोळा न लागणारे",
                    "सकाळी डोळे चुरचुरणे किंवा डोके जड वाटणारे व्यक्ती",
                    "रात्री उशिरापर्यंत लॅपटॉपवर काम करणारे कर्मचारी"
                ),
                relevantConcerns = listOf("अपुरी झोप", "डोळ्यांचा ताण", "मानसिक स्वास्थ्य"),
                personalizedSuggestions = listOf(
                    "झोपण्याच्या किमान १ तास आधी मोबाईल, टीव्ही आणि लॅपटॉप पूर्णपणे बंद करा.",
                    "मोबाईल रात्री बिछान्यापासून दूर ठेवा किंवा सायलेंट मोडवर ठेवा.",
                    "स्क्रीनऐवजी एखादे चांगले पुस्तक वाचा किंवा कुटुंबासोबत गप्पा मारा.",
                    "रात्री खोलीत मंद आणि पिवळसर प्रकाशाचे दिवे वापरा."
                ),
                detailedBenefits = listOf(
                    "नैसर्गिक झोपेचे संप्रेरक वेळेवर तयार होते.",
                    "बिछान्यावर पडल्या पडल्या १५ मिनिटांत शांत झोप लागते.",
                    "रात्री झोपण्यापूर्वी मनातील अनाठायी चिंता शांत होतात.",
                    "डोळ्यांची जळजळ आणि थकवा नाहीसा होतो."
                ),
                precautions = listOf(
                    "स्क्रीन फिल्टर लावून उशिरापर्यंत मोबाईल पाहणे टाळा.",
                    "झोपण्यापूर्वी अस्वस्थ करणाऱ्या बातम्या किंवा वादविवाद टाळा.",
                    "लहान मुलांना झोपण्यापूर्वी मोबाईल हाताळू देऊ नका."
                ),
                whenToConsultDoctor = "स्क्रीन बंद करूनही सतत महिनाभर झोप लागत नसेल तर मानसिक ताण किंवा इतर कारणांच्या तपासणीसाठी डॉक्टरांचा सल्ला घ्या."
            ),
            hindi = LocalizedTipDetails(
                whyUseful = "स्मार्टफोन की नीली रोशनी आंखों के जरिए मस्तिष्क को धोखा देती है कि अभी दिन है, जिससे मेलाटोनिन हार्मोन का स्राव रुक जाता है और नींद नहीं आती।",
                targetAudience = listOf(
                    "बिस्तर पर लेटकर फोन चलाने या रील्स देखने वाले लोग",
                    "बिस्तर पर जाने के बाद घंटों करवटें बदलने वाले व्यक्ति",
                    "सुबह उठने पर आंखों में जलन और सिर भारी महसूस करने वाले",
                    "देर रात तक काम करने वाले छात्र और पेशेवर"
                ),
                relevantConcerns = listOf("खराब नींद", "आंखों का तनाव", "मानसिक स्वास्थ्य"),
                personalizedSuggestions = listOf(
                    "सोने से 1 घंटा पहले फोन, टीवी और लैपटॉप को बंद कर दें।",
                    "फोन को तकिए के पास रखने की बजाय कमरे में दूर रखें।",
                    "स्क्रीन के बदले कोई किताब पढ़ें या शांत संगीत सुनें।",
                    "रात के समय कमरे में बहुत तेज रोशनी न जलाएं।"
                ),
                detailedBenefits = listOf(
                    "बिना दवा के प्राकृतिक और गहरी नींद आती है।",
                    "बिस्तर पर जाते ही 10-15 मिनट में नींद आ जाती है।",
                    "सोने से पहले की बेचैनी और तनाव खत्म होता है।",
                    "आंखों का सूखापन और सिरदर्द कम होता है।"
                ),
                precautions = listOf(
                    "ब्लू-लाइट चश्मा लगाकर भी देर रात तक स्क्रीन न देखें।",
                    "सोने से ठीक पहले उत्तेजक या तनावपूर्ण समाचार न देखें।",
                    "शाम के समय कमरे में पीली व हल्की रोशनी का उपयोग करें।"
                ),
                whenToConsultDoctor = "यदि स्क्रीन बंद रखने के बाद भी अनिद्रा और बेचैनी बनी रहे, तो डॉक्टर से सलाह लें।"
            )
        ),

        // Tip 13: Cool, Dark & Quiet Sleep Sanctuary (Sleep)
        TipDetailBundle(
            tipId = 13,
            relatedTipIds = listOf(11, 12, 14),
            english = LocalizedTipDetails(
                whyUseful = "Human sleep onset is biologically contingent upon a 1°C drop in core body temperature, while nocturnal light pollution fragments micro-sleep architecture.",
                targetAudience = listOf(
                    "People who wake up multiple times drenched in nocturnal perspiration",
                    "Light sleepers easily disturbed by ambient street noise or dawn light",
                    "Individuals living in dense urban apartments or noisy neighborhoods",
                    "Anyone seeking to optimize deep physical and cognitive sleep stages"
                ),
                relevantConcerns = listOf("Poor Sleep", "General Tiredness", "Sensory Overload"),
                personalizedSuggestions = listOf(
                    "Maintain room ambient temperature between 18°C and 21°C (65-70°F).",
                    "Install heavy blackout curtains or wear a contoured comfortable sleep mask.",
                    "Eliminate standby LEDs on appliances or cover them with dark tape.",
                    "Use a steady fan or white noise machine to mask sudden street sounds."
                ),
                detailedBenefits = listOf(
                    "Facilitates thermoregulatory vasodilation for rapid REM sleep entry.",
                    "Prevents cortical micro-arousals triggered by ambient noise spikes.",
                    "Maximizes brain metabolic waste clearance during deep sleep.",
                    "Eliminates morning awakenings triggered by early summer dawn light."
                ),
                precautions = listOf(
                    "Do not make the bedroom excessively freezing, which causes shivering.",
                    "Keep bedding and sleepwear breathable, favoring pure natural cotton.",
                    "Ensure adequate room air ventilation to avoid stuffiness."
                ),
                whenToConsultDoctor = "Consult a physician if you experience severe drenching night sweats accompanied by unexplained weight loss or persistent fever."
            ),
            marathi = LocalizedTipDetails(
                whyUseful = "शांत आणि गाढ झोप लागण्यासाठी शरीराचे तापमान १ अंश सेल्सिअसने कमी व्हावे लागते. अंधार आणि थंडावा मेंदूला गाढ झोपेसाठी तयार करतो.",
                targetAudience = listOf(
                    "रात्री वारंवार जाग येऊन झोपमोड होणारे लोक",
                    "खिडकीतून येणाऱ्या प्रकाशाने किंवा आवाजाने पटकन जागे होणारे",
                    "शहरातील गजबजलेल्या भागात राहणारे लोक",
                    "झोपेत खूप घाम येऊन अस्वस्थ वाटणारे"
                ),
                relevantConcerns = listOf("अपुरी झोप", "सततचा थकवा", "मानसिक ताण"),
                personalizedSuggestions = listOf(
                    "बेडरूमचे तापमान सुखद व हलके थंड ठेवा.",
                    "खिडकीवर गडद पडदे लावा जेणेकरून खोलीत पूर्ण अंधार राहील.",
                    "खोलीतील लहान दिवे बंद करा किंवा झाकून ठेवा.",
                    "बाहेरील आवाज रोखण्यासाठी पंखा किंवा हलका आवाज असणारा कुलर वापरा."
                ),
                detailedBenefits = listOf(
                    "रात्री गाढ झोप लागते आणि मध्येच जाग येत नाही.",
                    "मेंदूतील विषारी घटक बाहेर पडण्याची नैसर्गिक प्रक्रिया जलद होते.",
                    "सकाळी उठल्यावर मन पूर्ण ताजेतवाने आणि उत्साही वाटते.",
                    "हृदय आणि रक्तदाब शांत राहण्यास मदत होते."
                ),
                precautions = listOf(
                    "खोली अति थंड करू नका, ज्यामुळे अंग दुखेल.",
                    "झोपताना सुती आणि सैल कपडे वापरा.",
                    "खोलीत ताजी हवा खेळती राहण्यासाठी योग्य वायुवीजन ठेवा."
                ),
                whenToConsultDoctor = "रात्री प्रचंड घाम येणे, सतत वजन कमी होणे किंवा रात्री ताप येत असल्यास त्वरीत डॉक्टरांकडून तपासणी करून घ्या."
            ),
            hindi = LocalizedTipDetails(
                whyUseful = "गहरी नींद के लिए शरीर का तापमान थोड़ा कम होना आवश्यक है। अंधेरा और शांत वातावरण दिमाग को पूरी तरह विश्राम की स्थिति में ले जाता है।",
                targetAudience = listOf(
                    "रात में बार-बार नींद टूटने से परेशान लोग",
                    "हल्की सी आवाज या रोशनी से तुरंत जाग जाने वाले व्यक्ति",
                    "शोरगुल वाले इलाकों में रहने वाले लोग",
                    "रात में अत्यधिक गर्मी और बेचैनी महसूस करने वाले"
                ),
                relevantConcerns = listOf("खराब नींद", "सामान्य थकान", "मानसिक तनाव"),
                personalizedSuggestions = listOf(
                    "कमरे का तापमान आरामदायक और हल्का ठंडा रखें।",
                    "कमरे में पूरा अंधेरा करने के लिए मोटे पर्दे लगाएं या आई-मास्क पहनें।",
                    "बिजली के उपकरणों की लाइटों को बंद या ढक दें।",
                    "शोर से बचने के लिए पंखे की हल्की आवाज या ईयरप्लग का उपयोग करें।"
                ),
                detailedBenefits = listOf(
                    "नींद की गुणवत्ता सुधरती है और गहरी नींद का समय बढ़ता है।",
                    "मस्तिष्क से दिनभर की थकान पूरी तरह मिटती है।",
                    "सुबह उठने पर ताजगी और ऊर्जा का अनुभव होता है।",
                    "ब्लड प्रेशर और तनाव हार्मोन नियंत्रित रहते हैं।"
                ),
                precautions = listOf(
                    "कमरे को बहुत ज्यादा ठंडा न करें जिससे मांसपेशियां अकड़ें।",
                    "हल्के और सूती वस्त्र पहनकर सोएं।",
                    "कमरे में ताजी हवा आने की उचित व्यवस्था रखें।"
                ),
                whenToConsultDoctor = "यदि रात में पसीने से भीग जाना, बुखार या लगातार वजन घटने की समस्या हो तो तुरंत चिकित्सक से संपर्क करें।"
            )
        ),

        // Tip 14: Afternoon Caffeine & Dinner Buffer (Sleep)
        TipDetailBundle(
            tipId = 14,
            relatedTipIds = listOf(4, 11, 13),
            english = LocalizedTipDetails(
                whyUseful = "Caffeine antagonizes adenosine receptors with an elimination half-life of 5-7 hours, while late heavy meals keep the gastrointestinal tract metabolically active and warm.",
                targetAudience = listOf(
                    "Individuals lying awake at night with a racing mind or heart flutter",
                    "People suffering from nocturnal acid reflux or coughing",
                    "Heavy consumers of afternoon tea, coffee, energy drinks, or chocolate",
                    "Those who routinely eat heavy dinners within 1 hour of sleep"
                ),
                relevantConcerns = listOf("Poor Sleep", "Digestive Discomfort", "Cardiovascular Health"),
                personalizedSuggestions = listOf(
                    "Establish a firm caffeine cutoff at 2:00 PM: switch to herbal chamomile or peppermint tea.",
                    "Finish dinner at least 2.5 to 3 hours prior to lying down in bed.",
                    "Keep nighttime meals light, favoring steamed vegetables, light soups, or plain khichdi.",
                    "Take a gentle 10-minute slow stroll after dinner to facilitate gastric emptying."
                ),
                detailedBenefits = listOf(
                    "Ensures full adenosine pressure buildup for effortless sleep induction.",
                    "Completely eliminates nocturnal acid reflux and chest burning.",
                    "Lowers resting heart rate and sympathetic tone before bed.",
                    "Prevents waking up with nausea, dry mouth, or sour stomach."
                ),
                precautions = listOf(
                    "Do not abruptly stop high daily caffeine without tapering to avoid withdrawal headaches.",
                    "Be mindful of hidden caffeine in pre-workout supplements and sodas.",
                    "If hungry right before bed, opt for a small handful of raw almonds rather than heavy snacks."
                ),
                whenToConsultDoctor = "Consult a physician if you experience severe persistent regurgitation, coughing choking spells at night, or chest burning unresponsive to dietary timing."
            ),
            marathi = LocalizedTipDetails(
                whyUseful = "कॅफिनचा प्रभाव शरीरात ६ ते ८ तास राहतो आणि रात्री उशिरा जड जेवल्याने पचनसंस्था चालू राहून शरीराचे तापमान वाढते, ज्यामुळे झोपमोड होते.",
                targetAudience = listOf(
                    "रात्री बिछान्यात पडल्यावर छातीत धडधड होणारे किंवा विचार चक्र चालू असणारे लोक",
                    "रात्री छातीत जळजळ किंवा खोकला येणारे",
                    "दुपारी आणि संध्याकाळी वारंवार चहा-कॉफी पिणारे",
                    "रात्री उशिरा जेवून लगेच झोपणारे"
                ),
                relevantConcerns = listOf("अपुरी झोप", "अपचन व ॲसिडिटी", "हृदय स्वास्थ्य"),
                personalizedSuggestions = listOf(
                    "दुपारी २ नंतर चहा किंवा कॉफी पिणे पूर्णपणे बंद करा; त्याऐवजी कोमट पाणी किंवा ग्रीन टी घ्या.",
                    "रात्रीचे जेवण झोपण्याच्या किमान अडीच ते तीन तास आधी संपवा.",
                    "रात्री हलके जेवण घ्या—जसे की मूग डाळ खिचडी, सूप किंवा साधी पोळी-भाजी.",
                    "जेवणानंतर लगेच झोपण्याऐवजी घरात १० मिनिटे हळूवार शतपावली करा."
                ),
                detailedBenefits = listOf(
                    "मेंदू शांत होऊन बिछान्यावर पडताच गाढ झोप लागते.",
                    "रात्री छातीत जळजळ आणि ॲसिडिटीची समस्या नष्ट होते.",
                    "हृदयाचे ठोके सामान्य राहतात आणि बेचैनी टळते.",
                    "सकाळी पोट साफ होते आणि तोंडाला दुर्गंधी येत नाही."
                ),
                precautions = listOf(
                    "चहा अचानक बंद करू नका, प्रमाण हळूहळू कमी करा जेणेकरून डोके दुखणार नाही.",
                    "कोल्ड ड्रिंक्स, डार्क चॉकलेट आणि औषधांमध्येही कॅफिन असते, हे लक्षात ठेवा.",
                    "रात्री भूक लागल्यास थोडे मखाने किंवा चार बदाम खा, जड अन्न टाळा."
                ),
                whenToConsultDoctor = "रात्री छातीत तीव्र जळजळ होऊन अन्नाची उलटी घशात येत असल्यास किंवा रात्री श्वास गुदमरल्यास गॅस्ट्रोएन्टेरोलॉजिस्टचा सल्ला घ्या."
            ),
            hindi = LocalizedTipDetails(
                whyUseful = "कैफीन का असर शरीर में 6 से 8 घंटे तक रहता है, जो नींद लाने वाले रिसेप्टर्स को ब्लॉक करता है। देर रात भारी भोजन पाचन तंत्र को सक्रिय रखकर नींद में खलल डालता है।",
                targetAudience = listOf(
                    "रात में दिल की तेज धड़कन या बेचैनी महसूस करने वाले लोग",
                    "रात में सीने में जलन, एसिडिटी या खट्टी डकार से परेशान व्यक्ति",
                    "दोपहर बाद कई बार चाय या कॉफी पीने वाले लोग",
                    "रात को देर से भोजन कर सीधे सोने वाले व्यक्ति"
                ),
                relevantConcerns = listOf("खराब नींद", "पाचन संबंधी असुविधा", "हृदय स्वास्थ्य"),
                personalizedSuggestions = listOf(
                    "दोपहर 2 बजे के बाद चाय या कॉफी न पिएं; शाम को हर्बल चाय का विकल्प चुनें।",
                    "रात का खाना सोने से कम से कम 2.5 से 3 घंटे पहले खाएं।",
                    "रात का भोजन हल्का और सुपाच्य रखें—जैसे मूंग दाल खिचड़ी या दलिया।",
                    "रात के खाने के बाद 10 मिनट हल्की सैर करें।"
                ),
                detailedBenefits = listOf(
                    "नींद गहरी आती है और रात में बेचैनी नहीं होती।",
                    "रात में एसिडिटी और सीने की जलन से पूरी राहत मिलती है।",
                    "सुबह उठने पर पेट हल्का और ताजगी से भरा रहता है।",
                    "पाचन क्रिया दुरुस्त रहती है।"
                ),
                precautions = listOf(
                    "चाय-कॉफी की मात्रा को धीरे-धीरे घटाएं ताकि सिरदर्द न हो।",
                    "सोडा, एनर्जी ड्रिंक्स और डार्क चॉकलेट में भी कैफीन होता है, इनसे शाम को बचें।",
                    "देर रात तेज मसालेदार या तला-भुना भोजन बिल्कुल न करें।"
                ),
                whenToConsultDoctor = "यदि रात में सीने में तेज जलन, खाना गले तक लौटना या लगातार खांसी की समस्या बनी रहे तो तुरंत डॉक्टर को दिखाएं।"
            )
        ),

        // Tip 15: Baseline Daily Fluid Intake (Hydration)
        TipDetailBundle(
            tipId = 15,
            relatedTipIds = listOf(16, 17, 18),
            english = LocalizedTipDetails(
                whyUseful = "Adequate circulating water maintains vascular volume, supports glomerular kidney filtration, lubricates synovial joints, and sustains cellular nutrient delivery.",
                targetAudience = listOf(
                    "People suffering from frequent dull tension headaches and mental fatigue",
                    "Individuals troubled by dry skin, chapped lips, or dry mucous membranes",
                    "Those who frequently forget to drink fluids during busy work schedules",
                    "Anyone with a history of urinary tract infections or kidney stones"
                ),
                relevantConcerns = listOf("Dehydration", "Dry Skin", "General Tiredness"),
                personalizedSuggestions = listOf(
                    "Keep a filled 1-liter glass or stainless steel water bottle visible at your work desk.",
                    "Sip water continuously throughout the day rather than chugging large amounts at once.",
                    "Consume an extra 500 ml for every 45 minutes of physical exercise or intense heat.",
                    "Infuse plain water with sliced cucumber, mint leaves, or lemon wedges for natural flavor."
                ),
                detailedBenefits = listOf(
                    "Flushes metabolic urea and uric acid through optimal renal clearance.",
                    "Maintains intracellular hydration in skin, restoring plumpness and elasticity.",
                    "Prevents dehydration-induced cognitive lapses and midday lethargy.",
                    "Dilutes urinary calcium oxalate, preventing kidney stone crystal nucleation."
                ),
                precautions = listOf(
                    "Patients with heart failure or kidney disease must adhere to physician-prescribed fluid restrictions.",
                    "Do not consume extreme volumes rapidly, which can trigger hyponatremia.",
                    "Prefer room temperature or lukewarm water over freezing cold ice water."
                ),
                whenToConsultDoctor = "Seek medical care if you experience complete cessation of urination, extreme lethargy, sunken eyes, or severe dizziness when standing up."
            ),
            marathi = LocalizedTipDetails(
                whyUseful = "शरीरात मुबलक पाणी असल्यामुळे रक्ताभिसरण सुरळीत राहते, किडनी विषारी घटक बाहेर टाकते, सांध्यांमध्ये वंगण राहते आणि त्वचा तजेलदार बनते.",
                targetAudience = listOf(
                    "सतत डोकेदुखी, थकवा आणि चक्कर येण्याची तक्रार असणारे",
                    "त्वचा कोरडी पडणे, ओठ फाटणे किंवा घसा सुकणारे लोक",
                    "कामाच्या गडबडीत पाणी पिण्यास विसरणारे कर्मचारी",
                    "मुतखडा किंवा मूत्रमार्गाचा संसर्ग झालेले लोक"
                ),
                relevantConcerns = listOf("पाण्याची कमतरता", "त्वचेचा कोरडेपणा", "सततचा थकवा"),
                personalizedSuggestions = listOf(
                    "कामाच्या टेबलावर १ लिटरची पाण्याची बाटली भरून ठेवा आणि डोळ्यांसमोर ठेवा.",
                    "एकाच वेळी भरपूर पाणी पिण्याऐवजी दिवसभर थोडे-थोडे घोट घेत पाणी प्या.",
                    "कडक उन्हात जाताना किंवा व्यायामानंतर २ ग्लास अतिरिक्त पाणी प्या.",
                    "पाण्यात काकडी, पुदिना किंवा लिंबाचे काप टाकून नैसर्गिक चव आणा."
                ),
                detailedBenefits = listOf(
                    "किडनीमधून विषारी घटक वेगाने बाहेर पडून रक्त शुद्ध होते.",
                    "त्वचेचा कोरडेपणा दूर होऊन चेहऱ्यावर नैसर्गिक तेज येते.",
                    "डोकेदुखी व थकवा नाहीसा होऊन दिवसभर ताजेतवाने वाटते.",
                    "मुतखडा होण्याचा धोका अत्यंत कमी होतो."
                ),
                precautions = listOf(
                    "हृदयविकार किंवा किडनीचे रुग्ण यांनी डॉक्टरांनी ठरवून दिलेल्या मर्यादेतच पाणी प्यावे.",
                    "एकाच वेळी खूप जास्त पाणी पिणे शरीरातील मिठाचे प्रमाण कमी करू शकते.",
                    "अति थंड पाणी पिण्याऐवजी माठातील किंवा साधे पाणी प्या."
                ),
                whenToConsultDoctor = "लघवी पूर्णपणे बंद होणे, तीव्र चक्कर येऊन तोल जाणे किंवा डोळे आत जाणे अशी लक्षणे दिसल्यास तातडीने रुग्णालयात जा."
            ),
            hindi = LocalizedTipDetails(
                whyUseful = "पर्याप्त पानी पीने से रक्त संचार ठीक रहता है, गुर्दे विषैले पदार्थों को आसानी से बाहर निकालते हैं, जोड़ों में चिकनाई रहती है और त्वचा में चमक आती है।",
                targetAudience = listOf(
                    "सिरदर्द, कमजोरी और चक्कर आने की शिकायत वाले लोग",
                    "रूखी त्वचा, फटे होंठ और सूखे गले से परेशान व्यक्ति",
                    "काम की व्यस्तता में पानी पीना भूल जाने वाले लोग",
                    "गुर्दे की पथरी या यूरिन इन्फेक्शन से पीड़ित व्यक्ति"
                ),
                relevantConcerns = listOf("निर्जलीकरण", "त्वचा का रूखापन", "सामान्य थकान"),
                personalizedSuggestions = listOf(
                    "अपने कार्यस्थल पर पानी की एक बोतल हमेशा सामने भरकर रखें।",
                    "एक साथ बहुत सारा पानी पीने के बजाय दिनभर घूंट-घूंट कर पानी पिएं।",
                    "धूप में जाने या व्यायाम करने पर 1-2 गिलास अतिरिक्त पानी पिएं।",
                    "पानी में खीरा, नींबू या पुदीने की पत्तियां डालकर स्वाद और पोषण बढ़ाएं।"
                ),
                detailedBenefits = listOf(
                    "गुर्दे सुचारू रूप से कार्य करते हैं और खून साफ होता है।",
                    "त्वचा में नमी बनी रहती है और रूखापन मिटता है।",
                    "डिहाइड्रेशन से होने वाला सिरदर्द और थकान दूर होती है।",
                    "गुर्दे में पथरी बनने का जोखिम बहुत कम हो जाता है।"
                ),
                precautions = listOf(
                    "हृदय रोग या किडनी फेलियर के मरीज डॉक्टर द्वारा निर्धारित मात्रा में ही पानी पिएं।",
                    "एक साथ जरूरत से ज्यादा पानी न पिएं जिससे शरीर में सोडियम की कमी हो।",
                    "बर्फ के बहुत ठंडे पानी की जगह मटके या सामान्य तापमान का पानी पिएं।"
                ),
                whenToConsultDoctor = "पेशाब बिल्कुल बंद होना, अत्यधिक कमजोरी या खड़े होने पर चक्कर खाकर गिरने जैसे लक्षण दिखें तो तुरंत अस्पताल जाएं।"
            )
        )
    )

    private val detailsList3: List<TipDetailBundle> = listOf(
        // Tip 16: Morning Lukewarm Rehydration (Hydration)
        TipDetailBundle(
            tipId = 16,
            relatedTipIds = listOf(15, 17, 18),
            english = LocalizedTipDetails(
                whyUseful = "During 7-8 hours of sleep, roughly 500-800 ml of moisture is lost via respiration and dermal evaporation. Warm water stimulates the gastrocolic reflex and promotes portal vein circulation.",
                targetAudience = listOf(
                    "People waking up with dry mouth, thick saliva, or morning sluggishness",
                    "Individuals troubled by slow morning bowel movements or mild constipation",
                    "Those who immediately drink caffeinated coffee/tea on an empty stomach",
                    "Anyone seeking a gentle natural metabolic activator upon waking"
                ),
                relevantConcerns = listOf("Dehydration", "Digestive Discomfort", "General Tiredness"),
                personalizedSuggestions = listOf(
                    "Drink 1 to 2 glasses (300-500 ml) of lukewarm water right after rising from bed.",
                    "Sip calmly while seated rather than standing up in a hurry.",
                    "You may add a small squeeze of fresh lemon juice for vitamin C and palate freshness.",
                    "Wait 20-30 minutes before having morning tea, coffee, or heavy breakfast."
                ),
                detailedBenefits = listOf(
                    "Instantly restores vascular fluid volume lost during nocturnal sleep.",
                    "Naturally stimulates gastrointestinal peristalsis for effortless bowel clearance.",
                    "Jumpstarts hepatic detoxification and renal metabolic filtration.",
                    "Wakes up cognitive focus and clears morning grogginess naturally."
                ),
                precautions = listOf(
                    "Ensure water temperature is pleasantly warm, not scalding hot, to prevent thermal injury.",
                    "Avoid adding heavy honey or sugar to morning water if managing diabetes or weight.",
                    "If you experience acid reflux, do not add raw lemon on an empty stomach."
                ),
                whenToConsultDoctor = "Consult a physician if drinking water causes painful burning, difficulty swallowing, or persistent morning nausea."
            ),
            marathi = LocalizedTipDetails(
                whyUseful = "रात्रभरच्या झोपेत घामावाटे व श्वासावाटे सुमारे अर्धा लिटर पाणी कमी होते. सकाळी कोमट पाणी प्यायल्याने आतड्यांची हालचाल सुरू होते आणि पोट सहज साफ होते.",
                targetAudience = listOf(
                    "सकाळी उठल्यावर तोंड कोरडे पडणे किंवा सुस्ती असणारे लोक",
                    "सकाळी पोट साफ होण्यासाठी त्रास होणारे व्यक्ती",
                    "सकाळी रिकाम्या पोटी थेट चहा किंवा कॉफी पिणारे",
                    "पचनक्रिया आणि चयापचय सुधारू इच्छिणारे"
                ),
                relevantConcerns = listOf("पाण्याची कमतरता", "अपचन व पोट फुगणे", "सततचा थकवा"),
                personalizedSuggestions = listOf(
                    "सकाळी उठल्यावर चूळ भरून शांतपणे बसून १ ते २ ग्लास कोमट पाणी प्या.",
                    "पाणी घाईघाईत उभ्याने पिण्याऐवजी खाली बसून घोट-घोट प्या.",
                    "ॲसिडिटी नसेल तर पाण्यात थोडे लिंबू पिळून घेऊ शकता.",
                    "पाणी पिल्यानंतर २० मिनिटांनीच चहा किंवा नाश्ता घ्या."
                ),
                detailedBenefits = listOf(
                    "रात्रभर कमी झालेले पाणी भरून निघून शरीर ताजेतवाने होते.",
                    "आतड्यांची हालचाल सुधारून पोट सकाळी लगेच साफ होते.",
                    "यकृत आणि किडनीचे कार्य वेगाने सुरू होते.",
                    "सकाळचा आळस दूर होऊन दिवसभर तरतरी राहते."
                ),
                precautions = listOf(
                    "पाणी जास्त गरम नसावे, कोमट असावे जेणेकरून घसा भाजणार नाही.",
                    "मधुमेह असल्यास पाण्यात मध किंवा साखर घालू नका.",
                    "ॲसिडिटीचा त्रास असल्यास रिकाम्या पोटी लिंबू पिळणे टाळा."
                ),
                whenToConsultDoctor = "सकाळी पाणी पिताना घशात दुखणे किंवा उलट्या होत असल्यास डॉक्टरांना भेटा."
            ),
            hindi = LocalizedTipDetails(
                whyUseful = "रातभर की नींद में सांस और पसीने से पानी की कमी हो जाती है। सुबह गुनगुना पानी पीने से आंतें सक्रिय होती हैं और पेट खुलकर साफ होता है।",
                targetAudience = listOf(
                    "सुबह उठने पर सूखा मुंह और सुस्ती महसूस करने वाले लोग",
                    "सुबह पेट साफ न होने या कब्ज की शिकायत वाले व्यक्ति",
                    "खाली पेट सीधे चाय या कॉफी पीने के आदी लोग",
                    "शरीर को डिटॉक्स और मेटाबॉलिज्म तेज करना चाहने वाले"
                ),
                relevantConcerns = listOf("निर्जलीकरण", "पाचन संबंधी असुविधा", "सामान्य थकान"),
                personalizedSuggestions = listOf(
                    "सुबह उठते ही बैठकर 1 से 2 गिलास गुनगुना पानी पिएं।",
                    "पानी को खड़े होकर तेजी से पीने की बजाय घूंट-घूंट कर पिएं।",
                    "एसिडिटी न हो तो थोड़ा नींबू का रस मिला सकते हैं।",
                    "पानी पीने के 20-30 मिनट बाद ही नाश्ता या चाय लें।"
                ),
                detailedBenefits = listOf(
                    "शरीर में रातभर की पानी की कमी तुरंत पूरी होती है।",
                    "आंतों की गतिशीलता बढ़ती है और कब्ज दूर होती है।",
                    "लिवर और किडनी से विषैले तत्व बाहर निकलते हैं।",
                    "सुबह की सुस्ती खत्म होकर शरीर ऊर्जावान बनता है।"
                ),
                precautions = listOf(
                    "पानी बहुत ज्यादा गर्म न हो, गुनगुना हो ताकि गले को नुकसान न पहुंचे।",
                    "वजन या शुगर की समस्या में शहद या मीठा न मिलाएं।",
                    "एसिडिटी के मरीज खाली पेट नींबू का उपयोग न करें।"
                ),
                whenToConsultDoctor = "यदि पानी पीने पर सीने में तेज दर्द या उल्टी की शिकायत हो तो तुरंत डॉक्टर से संपर्क करें।"
            )
        ),

        // Tip 17: Urine Color Hydration Check (Hydration)
        TipDetailBundle(
            tipId = 17,
            relatedTipIds = listOf(15, 16, 18),
            english = LocalizedTipDetails(
                whyUseful = "Urine color serves as an immediate clinical bio-marker of urinary specific gravity and renal concentrating capacity, reflecting whole-body hydration balance without expensive tests.",
                targetAudience = listOf(
                    "Athletes and outdoor workers exposed to sun, sweating, and humidity",
                    "Individuals with recurring kidney stones or crystal formation",
                    "Anyone who struggles to know whether they are drinking sufficient fluids",
                    "Elderly individuals who may experience diminished innate thirst sensations"
                ),
                relevantConcerns = listOf("Dehydration", "Kidney Health", "General Fatigue"),
                personalizedSuggestions = listOf(
                    "Check your urine color during midday voids: aim for a light pale straw or transparent lemonade hue.",
                    "If urine appears dark amber or honey-toned, drink 2-3 glasses of water over the next two hours.",
                    "Be aware that B-complex vitamins turn urine fluorescent neon yellow harmlessly.",
                    "Do not strive for completely crystal-clear water-like urine all day, which may indicate over-hydration."
                ),
                detailedBenefits = listOf(
                    "Provides an instantaneous self-monitoring metric for daily fluid adequacy.",
                    "Dramatically reduces the risk of urinary crystal aggregation and kidney stones.",
                    "Helps prevent acute heat exhaustion and heat stroke during summer months.",
                    "Protects kidney nephrons from concentrating strain and toxic metabolite crystallization."
                ),
                precautions = listOf(
                    "Food items like beetroot and certain medications alter urine color.",
                    "If urine is persistently dark brown despite drinking water, it may indicate hepatic or biliary illness.",
                    "Red, pink, or cloudy urine warrants immediate clinical evaluation."
                ),
                whenToConsultDoctor = "Seek prompt medical care if you observe pink, red, or cola-colored urine, foamy urine, or pain during urination."
            ),
            marathi = LocalizedTipDetails(
                whyUseful = "लघवीचा रंग शरीरातील पाण्याचे प्रमाण अचूक दर्शवतो. हलका पिवळा रंग उत्तम आरोग्य दाखवतो, तर गडद रंग पाण्याचे प्रमाण कमी असल्याचे दर्शवतो.",
                targetAudience = listOf(
                    "उन्हात काम करणारे शेतकरी, कामगार आणि खेळाडू",
                    "मुतखड्याचा त्रास झालेले किंवा धोका असणारे रुग्ण",
                    "दिवसभरात पुरेसे पाणी पितो की नाही हे समजू न शकणारे लोक",
                    "वृद्ध व्यक्ती ज्यांना तहान लागल्याची जाणीव कमी होते"
                ),
                relevantConcerns = listOf("पाण्याची कमतरता", "किडनी स्वास्थ्य", "सततचा थकवा"),
                personalizedSuggestions = listOf(
                    "दुपारी लघवीच्या रंगाकडे लक्ष द्या: हलका पिवळा किंवा पारदर्शक रंग उत्तम आहे.",
                    "रंग गडद पिवळा किंवा लालसर दिसल्यास लगेच २-३ ग्लास पाणी प्या.",
                    "लक्षात ठेवा की ब-कॉम्प्लेक्स जीवनसत्त्वांच्या गोळ्या घेतल्यास लघवीचा रंग तेजस्वी पिवळा होतो.",
                    "अति पाणी पिऊन लघवी एकदम पांढरीशुभ्र करणे आवश्यक नाही, हलका पिवळा रंग स्वाभाविक आहे."
                ),
                detailedBenefits = listOf(
                    "शरीरातील पाण्याची पातळी स्वतः तपासता येते.",
                    "किडनीमध्ये खडे तयार होण्यापासून संरक्षण होते.",
                    "उन्हाळ्यात उष्माघाताचा धोका टळतो.",
                    "किडनीचे आरोग्य सुरक्षित राहते."
                ),
                precautions = listOf(
                    "बीट खाल्ल्याने लघवीचा रंग लालसर दिसू शकतो.",
                    "भरपूर पाणी पिऊनही लघवी चहासारखी गडद पिवळी किंवा तपकिरी राहिल्यास कावीळ असू शकते.",
                    "लघवीतून फेस येत असल्यास किंवा दुर्गंधी येत असल्यास डॉक्टरांना सांगा."
                ),
                whenToConsultDoctor = "लघवीतून रक्त पडणे, लघवी करताना तीव्र जळजळ होणे किंवा लघवी अडकून राहिल्यास तात्काळ डॉक्टरांकडे जा."
            ),
            hindi = LocalizedTipDetails(
                whyUseful = "मूत्र का रंग शरीर में पानी के स्तर का सबसे आसान और सटीक संकेत है। हल्का पीला रंग सही हाइड्रेशन दर्शाता है, जबकि गहरा पीला रंग पानी की कमी बताता है।",
                targetAudience = listOf(
                    "धूप और पसीने में काम करने वाले मजदूर और खिलाड़ी",
                    "गुर्दे की पथरी के मरीज या जिन्हें बार-बार इन्फेक्शन होता हो",
                    "जो यह नहीं जान पाते कि वे पर्याप्त पानी पी रहे हैं या नहीं",
                    "बुजुर्ग व्यक्ति जिन्हें प्यास का अहसास कम होता है"
                ),
                relevantConcerns = listOf("निर्जलीकरण", "गुर्दे का स्वास्थ्य", "सामान्य थकान"),
                personalizedSuggestions = listOf(
                    "दिन में पेशाब के रंग पर ध्यान दें: हल्का पीला या नींबू जैसा पारदर्शी रंग आदर्श है।",
                    "गहरा पीला रंग दिखने पर अगले 2 घंटों में 2 गिलास पानी अवश्य पिएं।",
                    "विटामिन बी की गोलियां लेने पर रंग चमकदार पीला हो सकता है, जो सामान्य है।",
                    "पानी का संतुलन बनाए रखें, बहुत अधिक पानी पीकर पेशाब को बिल्कुल पानी जैसा सफेद न करें।"
                ),
                detailedBenefits = listOf(
                    "पानी की कमी का तुरंत पता चलता है और रोकथाम आसान होती है।",
                    "गुर्दे की पथरी का खतरा बहुत घट जाता है।",
                    "गर्मी में लू और डिहाइड्रेशन से बचाव होता है।",
                    "किडनी पर दबाव नहीं पड़ता और कार्यप्रणाली अच्छी रहती है।"
                ),
                precautions = listOf(
                    "चुकंदर खाने या कुछ दवाओं से पेशाब का रंग बदल सकता है।",
                    "पानी पीने के बाद भी यदि पेशाब चाय जैसा गहरा हो तो लिवर की जांच कराएं।",
                    "पेशाब में झाग या अत्यधिक दुर्गंध को अनदेखा न करें।"
                ),
                whenToConsultDoctor = "यदि पेशाब में खून दिखे, लाल या भूरा रंग हो, या पेशाब करते समय तेज जलन व दर्द हो, तो तुरंत यूरोलॉजिस्ट को दिखाएं।"
            )
        ),

        // Tip 18: Natural Electrolyte Balancing (Hydration)
        TipDetailBundle(
            tipId = 18,
            relatedTipIds = listOf(15, 16, 17),
            english = LocalizedTipDetails(
                whyUseful = "Sweat depletes sodium, chloride, potassium, and magnesium. Rehydrating solely with plain water can precipitate exercise-associated hyponatremia; natural mineral drinks restore cellular osmotic pressure.",
                targetAudience = listOf(
                    "People experiencing painful nocturnal calf cramps or post-exercise spasms",
                    "Workers and athletes exercising in humid, hot conditions for over an hour",
                    "Individuals recovering from bouts of mild diarrheal illness or heavy perspiration",
                    "Anyone seeking healthy mineral replenishment without artificial sugary sports drinks"
                ),
                relevantConcerns = listOf("Dehydration", "Muscle Cramps", "General Tiredness"),
                personalizedSuggestions = listOf(
                    "Sip fresh tender coconut water (rich in potassium and magnesium) after sweating.",
                    "Prepare traditional Indian chaas (buttermilk) with roasted cumin powder and a pinch of black salt.",
                    "Drink homemade shikanji (lemon water with rock salt and a touch of mint) on hot afternoons.",
                    "Eat water-dense electrolyte fruits: watermelon, muskmelon, oranges, and cucumbers."
                ),
                detailedBenefits = listOf(
                    "Prevents painful skeletal muscle cramping and neuromuscular spasms.",
                    "Restores intra-cellular and extra-cellular electrolyte gradients.",
                    "Speeds up systemic rehydration faster than plain water alone.",
                    "Prevents heat exhaustion, weakness, and orthostatic dizziness."
                ),
                precautions = listOf(
                    "Patients with chronic kidney disease must strictly restrict high-potassium fluids like coconut water.",
                    "Hypertensive patients on sodium-restricted diets should not over-add table salt to drinks.",
                    "Avoid commercial bottled energy drinks loaded with artificial food colorings and excessive sugar."
                ),
                whenToConsultDoctor = "Seek emergency medical care if muscle spasms are accompanied by confusion, seizure activity, severe vomiting, or persistent inability to retain oral fluids."
            ),
            marathi = LocalizedTipDetails(
                whyUseful = "घामावाटे शरीरातील सोडियम, पोटॅशियम आणि मॅग्नेशियम हे क्षार बाहेर पडतात. केवळ साधे पाणी पिण्याऐवजी नैसर्गिक इलेक्ट्रोलाइट्स घेतल्याने पेशींमध्ये पाण्याचे संतुलन टिकून राहते.",
                targetAudience = listOf(
                    "रात्री पायांच्या पोटऱ्यांत पेटके (क्रॅम्प्स) येणारे लोक",
                    "उन्हाळ्यात कडक उन्हात काम करणारे किंवा घाम गाळणारे",
                    "अतिसार किंवा उलट्यांनंतर अशक्तपणा आलेले रुग्ण",
                    "कृत्रिम एनर्जी ड्रिंक्स टाळून नैसर्गिक क्षार हवे असणारे"
                ),
                relevantConcerns = listOf("पाण्याची कमतरता", "स्नायूंना पेटके", "सततचा थकवा"),
                personalizedSuggestions = listOf(
                    "व्यायामानंतर किंवा उन्हातून आल्यावर ताजे शहाळ्याचे पाणी प्या.",
                    "दुपारी जिरे, कोथिंबीर आणि सैंधव मीठ घातलेले ताजे ताक प्या.",
                    "लिंबू पाण्यात चिमूटभर काळे मीठ टाकून घरगुती शिकंजी बनवा.",
                    "कलिंगड, खरबूज, संत्री आणि काकडी यांसारखी फळे खा."
                ),
                detailedBenefits = listOf(
                    "पायांचे स्नायू आखडणे आणि पेटके येणे थांबते.",
                    "शरीराला तत्काळ ऊर्जा मिळते आणि थकवा दूर होतो.",
                    "उन्हाळ्यात उष्माघातापासून संरक्षण होते.",
                    "रक्तातील क्षारांचे संतुलन नैसर्गिकरित्या राखले जाते."
                ),
                precautions = listOf(
                    "किडनीचे आजार असणाऱ्या रुग्णांनी शहाळ्याचे पाणी पिण्यापूर्वी डॉक्टरांचा सल्ला घ्यावा.",
                    "उच्च रक्तदाब असणाऱ्यांनी पाण्यात जास्त मीठ घालणे टाळावे.",
                    "बाजारातील रासायनिक एनर्जी ड्रिंक्स पिणे टाळा."
                ),
                whenToConsultDoctor = "अतिसारामुळे प्रचंड अशक्तपणा येणे, डोळे फिरणे, उलट्या न थांबणे किंवा बेशुद्ध पडल्यास तातडीने सलाईन लावण्यासाठी रुग्णालयात जा."
            ),
            hindi = LocalizedTipDetails(
                whyUseful = "पसीने के साथ शरीर से जरूरी लवण निकल जाते हैं। प्राकृतिक इलेक्ट्रोलाइट्स मांसपेशियों की ऐंठन रोकते हैं और तुरंत ऊर्जा देते हैं।",
                targetAudience = listOf(
                    "रात में पिंडलियों या पैरों में तेज ऐंठन से परेशान लोग",
                    "गर्मी और धूप में पसीना बहाने वाले मजदूर और खिलाड़ी",
                    "दस्त या उल्टी के बाद अत्यधिक कमजोरी महसूस करने वाले व्यक्ति",
                    "कृत्रिम स्पोर्ट्स ड्रिंक से बचना चाहने वाले लोग"
                ),
                relevantConcerns = listOf("निर्जलीकरण", "मांसपेशियों में ऐंठन", "सामान्य थकान"),
                personalizedSuggestions = listOf(
                    "धूप से आने के बाद या व्यायाम के बाद ताजा नारियल पानी पिएं।",
                    "भुना जीरा और थोड़ा काला नमक मिलाकर घर की बनी छाछ पिएं।",
                    "नींबू पानी में चुटकी भर सेंधा नमक मिलाकर पिएं।",
                    "तरबूज, खरबूजा और खीरा जैसे पानी से भरपूर फल खाएं।"
                ),
                detailedBenefits = listOf(
                    "मांसपेशियों के दर्द और ऐंठन से तुरंत राहत मिलती है।",
                    "शरीर में पानी और लवणों का संतुलन बना रहता है।",
                    "लू और कमजोरी से बचाव होता है।",
                    "थकान मिटती है और चुस्ती-फुर्ती लौटती है।"
                ),
                precautions = listOf(
                    "किडनी के गंभीर मरीज नारियल पानी पीने से पहले डॉक्टर से पूछें।",
                    "हाई ब्लड प्रेशर के मरीज अतिरिक्त नमक मिलाने से बचें।",
                    "बाजार के केमिकल युक्त एनर्जी ड्रिंक्स से परहेज करें।"
                ),
                whenToConsultDoctor = "यदि अत्यधिक उल्टी-दस्त से मरीज बेहोश होने लगे, पेशाब बिल्कुल न आए या दौरे पड़ें तो तुरंत आपातकालीन चिकित्सा लें।"
            )
        ),

        // Tip 19: 4-7-8 Mindful Breathwork for Stress (Mental)
        TipDetailBundle(
            tipId = 19,
            relatedTipIds = listOf(20, 21, 22),
            english = LocalizedTipDetails(
                whyUseful = "Prolonged exhalation stimulates pulmonary stretch receptors, driving cholinergic vagal nerve stimulation that immediately decelerates sinoatrial heart rate and dampens sympathetic norepinephrine release.",
                targetAudience = listOf(
                    "People experiencing acute anxiety episodes, sudden panic, or racing thoughts",
                    "Individuals with occupational burnout and workplace tension",
                    "Those who have trouble calming down their nervous system before bed",
                    "Anyone seeking an instantaneous, zero-cost self-regulation tool"
                ),
                relevantConcerns = listOf("Stress & Mental Fatigue", "Poor Sleep", "Heart Rate Regulation"),
                personalizedSuggestions = listOf(
                    "Inhale quietly through your nose for 4 counts.",
                    "Gently hold your breath without straining for 7 counts.",
                    "Exhale completely with a whooshing sound through parted lips for 8 counts.",
                    "Complete 4 full breath cycles consecutively twice daily (morning and before sleep)."
                ),
                detailedBenefits = listOf(
                    "Instantly shifts the autonomic nervous system into parasympathetic mode.",
                    "Lowers elevated systolic blood pressure and calms cardiac palpitations.",
                    "Reduces acute salivary cortisol spikes during psychological stress.",
                    "Fosters emotional balance and clears mental panic within 2 minutes."
                ),
                precautions = listOf(
                    "Always practice seated or lying down in a safe position; do not perform while driving.",
                    "Do not strain excessively during the 7-count breath retention.",
                    "Individuals with severe asthma or COPD should breathe at their own natural comfortable cadence."
                ),
                whenToConsultDoctor = "Seek mental health or medical evaluation if panic symptoms include acute crushing chest pain, numbness in fingers, or recurrent panic attacks impairing daily functioning."
            ),
            marathi = LocalizedTipDetails(
                whyUseful = "दीर्घ श्वासोच्छ्वास केल्याने मेंदूतील 'वॅगस नर्व्ह' सक्रिय होते, ज्यामुळे हृदयाचे जलद ठोके शांत होतात, तणावाचे संप्रेरक कमी होतात आणि मन शांत होते.",
                targetAudience = listOf(
                    "अचानक भीती, चिंता किंवा घबराट वाटणारे लोक",
                    "ऑफिसच्या कामामुळे मानसिक ताण आणि चिडचिड होणारे व्यक्ती",
                    "झोपण्यापूर्वी मनात विचारांचे काहूर माजणारे लोक",
                    "कोणत्याही गोळ्यांशिवाय मन शांत करू इच्छिणारे"
                ),
                relevantConcerns = listOf("मानसिक ताण व थकवा", "अपुरी झोप", "हृदय ठोके नियंत्रण"),
                personalizedSuggestions = listOf(
                    "नाकाने ४ सेकंद हळुवार श्वास आत घ्या.",
                    "७ सेकंद श्वास रोखून धरा.",
                    "तोंडाने हळूहळू ८ सेकंदात पूर्ण श्वास बाहेर सोडा.",
                    "दररोज सकाळी आणि झोपण्यापूर्वी असे ४ वेळा सलग करा."
                ),
                detailedBenefits = listOf(
                    "२ मिनिटांत मनावरील ताण आणि भीती नाहीशी होते.",
                    "हृदयाचे ठोके आणि रक्तदाब शांत होतो.",
                    "मेंदूला शांतता मिळून विचारांवर ताबा मिळवता येतो.",
                    "रात्री गाढ झोप लागण्यास मोठी मदत होते."
                ),
                precautions = listOf(
                    "हा प्राणायाम नेहमी शांत बसून किंवा झोपून करा; गाडी चालवताना करू नका.",
                    "श्वास रोखून धरताना चक्कर आल्यासारखे वाटल्यास सेकंद कमी करा.",
                    "दम्याचा त्रास असणाऱ्यांनी जास्त वेळ श्वास रोखून धरू नये."
                ),
                whenToConsultDoctor = "छातीत असहनीय दुखणे, वारंवार पॅनिक अटॅक येणे किंवा सतत भीती वाटून दैनंदिन कामे थांबल्यास डॉक्टरांचा सल्ला घ्या."
            ),
            hindi = LocalizedTipDetails(
                whyUseful = "लंबी सांस बाहर छोड़ने से वेगस नर्व सक्रिय होती है, जो दिल की तेज धड़कन को धीमा करती है और तनाव हार्मोन कोर्टिसोल को तुरंत घटाकर शांति देती है।",
                targetAudience = listOf(
                    "अचानक घबराहट, चिंता या तेज दिल की धड़कन महसूस करने वाले लोग",
                    "काम के दबाव और मानसिक थकान से जूझ रहे पेशेवर",
                    "सोने से पहले मन में अत्यधिक नकारात्मक विचारों से परेशान लोग",
                    "बिना दवा के मन को शांत करना चाहने वाले सभी लोग"
                ),
                relevantConcerns = listOf("मानसिक तनाव और थकान", "खराब नींद", "हृदय गति नियंत्रण"),
                personalizedSuggestions = listOf(
                    "नाक से 4 सेकंड में शांत होकर सांस अंदर खींचें।",
                    "7 सेकंड तक सांस को रोककर रखें।",
                    "मुंह से धीरे-धीरे 8 सेकंड में पूरी सांस बाहर छोड़ें।",
                    "सुबह और रात को सोने से पहले इसे 4 बार दोहराएं।"
                ),
                detailedBenefits = listOf(
                    "2 मिनट में तनाव और बेचैनी दूर होती है।",
                    "ब्लड प्रेशर सामान्य होता है और दिल की धड़कन शांत होती है।",
                    "दिमागी स्पष्टता और मानसिक संतुलन लौटता है।",
                    "रात में गहरी नींद लाने में बेहद मददगार है।"
                ),
                precautions = listOf(
                    "इसे हमेशा बैठकर या लेटकर करें, वाहन चलाते समय न करें।",
                    "यदि सांस रोकने में चक्कर आए तो समय थोड़ा कम कर लें।",
                    "दमे के मरीज जबरदस्ती सांस न रोकें, सामान्य गति से करें।"
                ),
                whenToConsultDoctor = "यदि घबराहट के साथ सीने में तेज दर्द हो, बार-बार पैनिक अटैक आएं या रोजमर्रा का जीवन प्रभावित हो तो तुरंत डॉक्टर से संपर्क करें।"
            )
        ),

        // Tip 20: Daily Gratitude & Micro-Mindfulness (Mental)
        TipDetailBundle(
            tipId = 20,
            relatedTipIds = listOf(19, 21, 22),
            english = LocalizedTipDetails(
                whyUseful = "Conscious gratitude activates medial prefrontal cortex circuits, dampens amygdala threat-vigilance, and releases endogenous dopamine and serotonin.",
                targetAudience = listOf(
                    "Individuals trapped in cycles of chronic worrying, pessimism, or rumination",
                    "People suffering from corporate burnout and persistent dissatisfaction",
                    "Those undergoing stressful life transitions, exam stress, or grief",
                    "Anyone wishing to build long-term psychological resilience"
                ),
                relevantConcerns = listOf("Stress & Mental Fatigue", "Depressive Mood", "Overall Wellbeing"),
                personalizedSuggestions = listOf(
                    "Keep a bedside journal: write down 3 specific things you are grateful for every night.",
                    "Practice non-judgmental observation: notice your current emotions without self-criticism.",
                    "Anchor gratitude to sensory details (e.g. warmth of tea, birdsong, a supportive text).",
                    "Spend 2 minutes sending silent well-wishes to someone in your life."
                ),
                detailedBenefits = listOf(
                    "Rewires neural pathways towards optimism and positive cognitive appraisal.",
                    "Dampens hyper-reactivity of the amygdala to daily minor frustrations.",
                    "Elevates baseline emotional satisfaction and reduces feelings of envy.",
                    "Improves subjective sleep quality by replacing pre-sleep worries."
                ),
                precautions = listOf(
                    "Do not practice toxic positivity—acknowledge genuine sadness and grief without suppression.",
                    "Gratitude is a wellness habit, not an alternative to psychiatric treatment for major depression.",
                    "Be patient: neuro-plastic mental changes require consistent daily practice over weeks."
                ),
                whenToConsultDoctor = "Consult a licensed mental health professional or psychiatrist if you experience feelings of persistent hopelessness, severe anhedonia, or thoughts of self-harm."
            ),
            marathi = LocalizedTipDetails(
                whyUseful = "कृतज्ञता व्यक्त केल्याने मेंदूतील आनंदाची संप्रेरके पाझरतात, नकारात्मक विचारांचे चक्र थांबते आणि जीवनाकडे पाहण्याचा दृष्टिकोन सकारात्मक बनतो.",
                targetAudience = listOf(
                    "सतत नकारात्मक विचार, चिंता किंवा असंतोष वाटणारे लोक",
                    "नोकरी किंवा कौटुंबिक ताणामुळे मन उदास असणारे",
                    "परीक्षेचा ताण किंवा आयुष्यातील बदलांमुळे चिंताग्रस्त असणारे",
                    "मानसिक समाधान आणि शांतता शोधणारे"
                ),
                relevantConcerns = listOf("मानसिक ताण व थकवा", "उदास मन", "एकूण स्वास्थ्य"),
                personalizedSuggestions = listOf(
                    "रोज रात्री झोपण्यापूर्वी एका डायरीत दिवसभरातील चांगल्या ३ गोष्टी लिहून काढा.",
                    "मनात येणाऱ्या विचारांवर लगेच टीका न करता शांतपणे साक्षीभावाने पाहा.",
                    "छोट्या-छोट्या गोष्टींचे (कुटुंबाचे प्रेम, अन्न, निसर्ग) मनापासून आभार माना.",
                    "दिवसातून एकदा एखाद्या व्यक्तीचे मनापासून कौतुक करा किंवा धन्यवाद म्हणा."
                ),
                detailedBenefits = listOf(
                    "मनातील निराशा आणि चिंता हळूहळू दूर होते.",
                    "सकारात्मक विचारसरणी विकसित होऊन मन उत्साही राहते.",
                    "रात्री शांत झोप लागण्यास मदत होते.",
                    "कठीण प्रसंगांना तोंड देण्याचे मानसिक बळ मिळते."
                ),
                precautions = listOf(
                    "दुःखाच्या भावना दडपून ठेवू नका, वाईट वाटणे स्वाभाविक आहे हे स्वीकारा.",
                    "हा सराव नैराश्यावरील उपचारांचा पर्याय नाही.",
                    "मनाचे बदल होण्यासाठी किमान २-३ आठवडे नियमित सराव आवश्यक आहे."
                ),
                whenToConsultDoctor = "सतत २ आठवड्यांपेक्षा जास्त काळ प्रचंड नैराश्य, कशातही रस न वाटणे किंवा आत्महत्येचे विचार मनात आल्यास मानसोपचारतज्ज्ञांची मदत घ्या."
            ),
            hindi = LocalizedTipDetails(
                whyUseful = "आभार और सजगता का अभ्यास मस्तिष्क में डोपामाइन और सेरोटोनिन को सक्रिय करता है, जिससे नकारात्मक विचारों का चक्र टूटता है और मानसिक शांति मिलती है।",
                targetAudience = listOf(
                    "लगातार चिंता, तनाव और नकारात्मक सोच से परेशान लोग",
                    "काम के दबाव से निराश और थके हुए व्यक्ति",
                    "जीवन के उतार-चढ़ाव में भावनात्मक रूप से परेशान लोग",
                    "सकारात्मक सोच और मानसिक शांति चाहने वाले सभी व्यक्ति"
                ),
                relevantConcerns = listOf("मानसिक तनाव और थकान", "उदास मन", "समग्र कल्याण"),
                personalizedSuggestions = listOf(
                    "रोज रात को सोने से पहले डायरी में 3 ऐसी बातें लिखें जिनके लिए आप आभारी हैं।",
                    "वर्तमान क्षण में जीना सीखें और बिना किसी पछतावे के मन को शांत रखें।",
                    "छोटी-छोटी खुशियों और अपनों के साथ का आभार व्यक्त करें।",
                    "दिन में कम से कम एक व्यक्ति को धन्यवाद कहें या उसकी प्रशंसा करें।"
                ),
                detailedBenefits = listOf(
                    "मन की उदासी और चिंता धीरे-धीरे खत्म होती है।",
                    "सकारात्मक दृष्टिकोण और मानसिक मजबूती बढ़ती है।",
                    "रात में सुकून भरी नींद आती है।",
                    "कठिन परिस्थितियों से निपटने का आत्मविश्वास बढ़ता है।"
                ),
                precautions = listOf(
                    "सच्चे दुख को जबरदस्ती दबाएं नहीं, अपनी भावनाओं को स्वीकार करें।",
                    "यह गंभीर डिप्रेशन के डॉक्टरी इलाज का विकल्प नहीं है।",
                    "नियमित अभ्यास से ही मन में स्थाई सकारात्मक बदलाव आते हैं।"
                ),
                whenToConsultDoctor = "यदि लगातार गहरी निराशा बनी रहे, किसी काम में मन न लगे या खुद को नुकसान पहुंचाने के विचार आएं तो तुरंत मनोचिकित्सक से मिलें।"
            )
        ),

        // Tip 21: Nurturing Meaningful Social Bonds (Mental)
        TipDetailBundle(
            tipId = 21,
            relatedTipIds = listOf(19, 20, 22),
            english = LocalizedTipDetails(
                whyUseful = "Face-to-face prosocial contact and emotional validation trigger oxytocin synthesis, dampen cortisol reactivity, and prevent loneliness-induced systemic immune dysregulation.",
                targetAudience = listOf(
                    "Individuals feeling socially isolated, lonely, or disconnected from friends",
                    "Elderly citizens living alone or retirees with shrinking social circles",
                    "Remote workers spending entire weeks communicating only via text screens",
                    "Anyone dealing with grief, personal loss, or emotional vulnerability"
                ),
                relevantConcerns = listOf("Mental Wellness", "Stress & Anxiety", "Loneliness"),
                personalizedSuggestions = listOf(
                    "Schedule a weekly uninterrupted 30-minute phone call or meetup with a close friend.",
                    "Engage in community volunteering, local cultural groups, or neighborhood walks.",
                    "Practice active listening: focus entirely on the other person without checking your phone.",
                    "Share your genuine feelings with trusted companions rather than hiding stress."
                ),
                detailedBenefits = listOf(
                    "Releases oxytocin, buffering against cardiovascular stress responses.",
                    "Measurably lowers the long-term risk of cognitive decline and dementia.",
                    "Provides vital emotional validation during distressing life events.",
                    "Fosters a deep sense of belonging, purpose, and mutual community support."
                ),
                precautions = listOf(
                    "Limit time with toxic relationships that consistently drain your mental peace.",
                    "Do not confuse superficial social media followers with authentic bonds.",
                    "Respect personal boundaries and allow relationships to develop naturally."
                ),
                whenToConsultDoctor = "Seek medical or psychological consultation if social isolation turns into severe social anxiety, agoraphobia, or clinical depression."
            ),
            marathi = LocalizedTipDetails(
                whyUseful = "मित्र आणि कुटुंबाशी आत्मीय संवाद साधल्याने शरीरात ऑक्सिटोसिन हे प्रेम व समाधानाचे संप्रेरक पाझरते, एकटेपणा दूर होतो आणि मानसिक ताण हलका होतो.",
                targetAudience = listOf(
                    "एकटेपणा, नैराश्य किंवा मित्रांच्या अभावामुळे अस्वस्थ असणारे",
                    "एकटे राहणारे ज्येष्ठ नागरिक किंवा सेवानिवृत्त लोक",
                    "घरून काम करणारे आणि लोकांशी संपर्क तुटलेले कर्मचारी",
                    "दुःख किंवा संकटातून जात असणारे व्यक्ती"
                ),
                relevantConcerns = listOf("मानसिक स्वास्थ्य", "ताण व चिंता", "एकटेपणा"),
                personalizedSuggestions = listOf(
                    "आठवड्यातून किमान एकदा जवळच्या मित्राला किंवा नातेवाईकाला फोन करून मनमोकळा संवाद साधा.",
                    "कुटुंबासोबत एकत्र बसून जेवा आणि मोबाईल बाजूला ठेवा.",
                    "स्थानिक समाजोपयोगी उपक्रमात किंवा समविचारी लोकांच्या समूहात सहभागी व्हा.",
                    "मनातील भावना विश्वासू व्यक्तीजवळ मोकळेपणाने व्यक्त करा."
                ),
                detailedBenefits = listOf(
                    "एकटेपणा आणि नैराश्याची भावना नाहीशी होते.",
                    "हृदयाचे आरोग्य सुधारते आणि रक्तातील ताण कमी होतो.",
                    "म्हातारपणात स्मृतीभ्रंश होण्याचा धोका कमी होतो.",
                    "जीवनाला आधार आणि भावनिक सुरक्षितता मिळते."
                ),
                precautions = listOf(
                    "मानसिक शांतता भंग करणाऱ्या लोकांपासून आवश्यक अंतर ठेवा.",
                    "सोशल मीडियावरील आभासी मित्रांना खऱ्या मित्रांचा पर्याय मानू नका.",
                    "नात्यांमध्ये एकमेकांच्या वैयक्तिक स्वातंत्र्याचा आदर करा."
                ),
                whenToConsultDoctor = "लोकांना भेटण्याची प्रचंड भीती वाटणे, घराबाहेर पडणे अशक्य वाटणे किंवा तीव्र नैराश्य आल्यास मानसोपचारतज्ज्ञांची मदत घ्या."
            ),
            hindi = LocalizedTipDetails(
                whyUseful = "अपनों से खुलकर बातचीत करने और मिलना-जुलना मस्तिष्क में ऑक्सीटोसिन हार्मोन बढ़ाता है, जिससे अकेलापन और तनाव दूर होता है और जीवन में भरोसा बढ़ता है।",
                targetAudience = listOf(
                    "अकेलापन, उदासी या दोस्तों से दूरी महसूस करने वाले लोग",
                    "अकेले रहने वाले बुजुर्ग या रिटायर्ड व्यक्ति",
                    "वर्क फ्रॉम होम में अकेलेपन से जूझ रहे कर्मचारी",
                    "भावनात्मक संकट या दुख के दौर से गुजर रहे लोग"
                ),
                relevantConcerns = listOf("मानसिक स्वास्थ्य", "तनाव व चिंता", "अकेलापन"),
                personalizedSuggestions = listOf(
                    "हफ्ते में कम से कम एक बार अपने पुराने मित्र या परिजन से फोन या मिलकर बात करें।",
                    "परिवार के साथ बैठकर खाना खाएं और उस समय फोन का उपयोग न करें।",
                    "किसी सामाजिक या रुचि से जुड़े समूह में भाग लें।",
                    "अपनी परेशानियां किसी भरोसेमंद साथी से साझा करें।"
                ),
                detailedBenefits = listOf(
                    "अकेलापन और अवसाद की भावना दूर होती है।",
                    "दिल का स्वास्थ्य अच्छा रहता है और रक्तचाप नियंत्रित रहता है।",
                    "उम्र बढ़ने पर याददाश्त कमजोर होने का जोखिम घटता है।",
                    "जीवन में सुरक्षा और अपनेपन का अहसास होता है।"
                ),
                precautions = listOf(
                    "नकारात्मकता फैलाने वाले लोगों से दूरी बनाए रखें।",
                    "सोशल मीडिया के आभासी संपर्कों को असली दोस्तों का विकल्प न समझें।",
                    "संबंधों में एक-दूसरे की सीमाओं का सम्मान करें।"
                ),
                whenToConsultDoctor = "यदि सामाजिक मेलजोल से अत्यधिक डर लगे, घर से बाहर निकलना बंद हो जाए या गहरा अवसाद हो तो डॉक्टर से मिलें।"
            )
        ),

        // Tip 22: Restorative Immersion in Nature (Mental)
        TipDetailBundle(
            tipId = 22,
            relatedTipIds = listOf(6, 19, 20),
            english = LocalizedTipDetails(
                whyUseful = "Exposure to natural phytoncides (plant aerosols) and organic fractal visuals reduces prefrontal cortex hyper-activation, lowering salivary cortisol and boosting natural killer immune cell activity.",
                targetAudience = listOf(
                    "Urban residents enclosed in concrete buildings without natural greenery",
                    "Individuals experiencing cognitive mental exhaustion and creative block",
                    "Those suffering from sensory overload caused by traffic, screens, and noise",
                    "Anyone seeking a peaceful meditative reset for mental peace"
                ),
                relevantConcerns = listOf("Stress & Mental Fatigue", "Sensory Overload", "Immunity"),
                personalizedSuggestions = listOf(
                    "Take a 20-30 minute mindful walk in a tree-filled park, garden, or nature reserve 2-3 times weekly.",
                    "Leave headphones and screens inside your bag: tune into birdsong, rustling leaves, and wind.",
                    "Practice deep rhythmic breathing while standing barefoot on clean green grass.",
                    "Add indoor potted plants to your living room or workspace."
                ),
                detailedBenefits = listOf(
                    "Significantly reduces circulating cortisol and cardiac stress.",
                    "Restores directed attention capacity, boosting creative problem-solving.",
                    "Increases natural killer white blood cells, strengthening immunity.",
                    "Uplifts subjective mood and alleviates feelings of claustrophobia."
                ),
                precautions = listOf(
                    "Individuals with severe seasonal pollen allergies should check pollen counts before visiting blooming gardens.",
                    "Use insect repellent or wear long sleeves if walking in areas prone to mosquito-borne illnesses.",
                    "Stay on safe, well-lit pedestrian pathways in natural parks."
                ),
                whenToConsultDoctor = "Consult a physician if outdoor walking provokes sudden wheezing, chest tightness, or severe allergic hives."
            ),
            marathi = LocalizedTipDetails(
                whyUseful = "निसर्गाच्या सानिध्यात झाडांच्या संपर्कात आल्याने शरीरातील ताणतणावाचे हार्मोन्स कमी होतात, मन शांत होते आणि प्रतिकारशक्ती वाढते.",
                targetAudience = listOf(
                    "शहरातील सिमेंटच्या इमारतींमध्ये व बंद फ्लॅटमध्ये राहणारे लोक",
                    "कामाच्या ताणामुळे मेंदू थकलेले आणि चिडचिड होणारे व्यक्ती",
                    "गाड्यांचा आवाज आणि स्क्रीनच्या झगमगाटाने वैतागलेले लोक",
                    "मानसिक शांतता व निसर्गाचा आनंद घेऊ इच्छिणारे"
                ),
                relevantConcerns = listOf("मानसिक ताण व थकवा", "आवाजाचा त्रास", "प्रतिकारशक्ती"),
                personalizedSuggestions = listOf(
                    "आठवड्यातून २-३ वेळा बागेत किंवा झाडांच्या सानिध्यात २० ते ३० मिनिटे शांत चाला.",
                    "चालताना कानात हेडफोन लावू नका; पानांची सळसळ, पक्ष्यांचे आवाज अनुभवा.",
                    "सकाळी स्वच्छ हिरव्या गवतावर अनवाणी चाला.",
                    "घरामध्ये किंवा खिडकीत कुंड्यांमध्ये छोटी रोपे लावा."
                ),
                detailedBenefits = listOf(
                    "रक्तातील तणावाचे प्रमाण कमी होऊन मन शांत होते.",
                    "मेंदूचा थकवा दूर होऊन नवीन विचार करण्याची शक्ती वाढते.",
                    "रोगप्रतिकारक पांढऱ्या पेशींची ताकद वाढते.",
                    "मन प्रसन्न राहते आणि रात्री छान झोप लागते."
                ),
                precautions = listOf(
                    "फुलांच्या परागकणांची ॲलर्जी असणाऱ्यांनी ऋतूबदलाच्या वेळी काळजी घ्यावी.",
                    "डासांपासून संरक्षणासाठी पूर्ण बाह्यांचे कपडे घालावेत.",
                    "उद्यानात सुरक्षित व स्वच्छ पायवाटेवरच चालावे."
                ),
                whenToConsultDoctor = "बागेत फिरताना अचानक धाप लागणे, श्वास घेताना शिटीसारखा आवाज येणे किंवा तीव्र ॲलर्जी झाल्यास डॉक्टरांशी संपर्क साधा."
            ),
            hindi = LocalizedTipDetails(
                whyUseful = "पेड़-पौधों के संपर्क में आने से तनाव हार्मोन कोर्टिसोल तेजी से घटता है, दिमाग की नसों को आराम मिलता है और रोग प्रतिरोधक कोशिकाएं सक्रिय होती हैं।",
                targetAudience = listOf(
                    "शहर के कंक्रीट के मकानों में रहने वाले लोग",
                    "दिमागी थकान, काम के बोझ और चिड़चिड़ापन से परेशान व्यक्ति",
                    "शोरगुल, ट्रैफिक और लगातार स्क्रीन से तंग आ चुके लोग",
                    "मन को शांत और ताजगी से भरना चाहने वाले सभी लोग"
                ),
                relevantConcerns = listOf("मानसिक तनाव और थकान", "शोर का तनाव", "इम्युनिटी"),
                personalizedSuggestions = listOf(
                    "सप्ताह में 2-3 बार किसी पार्क या हरियाली के बीच 20-30 मिनट टहलें।",
                    "टहलते समय हेडफोन न लगाएं; पक्षियों की चहचहाहट और हवा की आवाज सुनें।",
                    "सुबह साफ हरी घास पर नंगे पैर टहलें।",
                    "घर के अंदर या बालकनी में गमलों में पौधे लगाएं।"
                ),
                detailedBenefits = listOf(
                    "तनाव हार्मोन तेजी से कम होता है और मानसिक शांति मिलती है।",
                    "दिमाग की एकाग्रता और सोचने की क्षमता में सुधार होता है।",
                    "शरीर की रोग प्रतिरोधक क्षमता मजबूत होती है।",
                    "उदासी दूर होकर सकारात्मक ऊर्जा का संचार होता है।"
                ),
                precautions = listOf(
                    "फूलों के पराग से एलर्जी वाले लोग वसंत ऋतु में सावधानी बरतें।",
                    "मच्छरों से बचाव के लिए पूरी बाजू के कपड़े पहनें।",
                    "पार्क में साफ और सुरक्षित रास्ते पर ही टहलें।"
                ),
                whenToConsultDoctor = "यदि खुली हवा में जाने पर अचानक सांस में घरघराहट या त्वचा पर तेज चकत्ते हों तो तुरंत डॉक्टर को दिखाएं।"
            )
        )
    )

    private val allDetailsMap: Map<Int, TipDetailBundle> by lazy {
        (detailsList + detailsList2 + detailsList3).associateBy { it.tipId }
    }

    // ==========================================
    // SYMPTOM / WELLNESS CONCERNS MAPPINGS (Section 5)
    // ==========================================
    private val wellnessConcerns: List<WellnessConcernItem> = listOf(
        WellnessConcernItem(
            type = WellnessConcernType.DRY_SKIN,
            relatedTipIds = listOf(15, 5, 2, 8),
            english = LocalizedWellnessConcern(
                name = "Dry or Sensitive Skin",
                description = "Skin lacking elasticity, moisture barrier integrity, or showing dullness and flaking.",
                whyHelpful = "Adequate cellular water intake and dietary polyphenol antioxidants restore stratum corneum hydration and shield dermal collagen.",
                practicalSuggestions = listOf(
                    "Drink at least 2.5 to 3 liters of water daily to maintain skin turgor.",
                    "Incorporate anti-inflammatory culinary turmeric and colorful produce rich in vitamins C and E.",
                    "Avoid excessively long, steaming hot showers that strip natural dermal lipids.",
                    "Moisturize skin within 3 minutes of bathing while the skin remains slightly damp."
                ),
                precautions = "Do not apply harsh astringents or unverified home concoctions to severely cracked or inflamed skin.",
                medicalGuidance = "Consult a dermatologist if dry skin is accompanied by painful fissures, severe pruritus (itching) disturbing sleep, weeping crusts, or signs of secondary bacterial infection."
            ),
            marathi = LocalizedWellnessConcern(
                name = "त्वचेचा कोरडेपणा व संवेदनशीलता",
                description = "त्वचेतील ओलावा कमी होणे, त्वचा निस्तेज दिसणे किंवा पांढरे पापुद्रे येणे.",
                whyHelpful = "मुबलक पाणी पिणे आणि आहारातील अँटीऑक्सिडंट्समुळे त्वचेच्या आतील पेशींना ओलावा मिळतो आणि त्वचा चमकदार राहते.",
                practicalSuggestions = listOf(
                    "दररोज २.५ ते ३ लिटर पाणी पिऊन त्वचेतील हायड्रेशन टिकवून ठेवा.",
                    "आहारात हळद, आवळा, गाजर आणि व्हिटॅमिन सी युक्त फळे खा.",
                    "अति गरम पाण्याने जास्त वेळ आंघोळ करणे टाळा, यामुळे त्वचेतील नैसर्गिक तेल नष्ट होते.",
                    "आंघोळीनंतर लगेच अंगावर सौम्य मॉइश्चरायझर किंवा खोबरेल तेल लावा."
                ),
                precautions = "त्वचा फाटलेली किंवा लाल झालेली असताना त्यावर तीव्र रसायने किंवा घरगुती लेप लावू नका.",
                medicalGuidance = "त्वचेला भेगा पडून रक्त येणे, तीव्र खाज सुटून झोपमोड होणे किंवा पू भरल्यास त्वरित त्वचारोग तज्ज्ञांना भेटा."
            ),
            hindi = LocalizedWellnessConcern(
                name = "रूखी और बेजान त्वचा",
                description = "त्वचा में नमी की कमी, खिंचाव, रूखापन या पपड़ीदार त्वचा।",
                whyHelpful = "पर्याप्त जलपान और एंटीऑक्सीडेंट युक्त आहार त्वचा की आंतरिक परतों को नमी प्रदान करते हैं और चमक बनाए रखते हैं।",
                practicalSuggestions = listOf(
                    "प्रतिदिन 2.5 से 3 लीटर पानी पीकर त्वचा में जल संतुलन बनाए रखें।",
                    "आहार में हल्दी, मौसमी फल और विटामिन सी व ई युक्त सब्जियां लें।",
                    "बहुत तेज गर्म पानी से लंबे समय तक स्नान न करें।",
                    "स्नान के तुरंत बाद हल्की नमी में नारियल तेल या मॉइश्चराइजर लगाएं।"
                ),
                precautions = "कटी या अत्यधिक सूजी हुई त्वचा पर तीखे घरेलू लेप न लगाएं।",
                medicalGuidance = "यदि त्वचा में दरारें पड़कर खून आए, असहनीय खुजली हो या संक्रमण के लक्षण दिखें तो तुरंत त्वचा रोग विशेषज्ञ से संपर्क करें।"
            )
        ),
        WellnessConcernItem(
            type = WellnessConcernType.GENERAL_TIREDNESS,
            relatedTipIds = listOf(1, 6, 11, 15),
            english = LocalizedWellnessConcern(
                name = "General Tiredness & Sluggishness",
                description = "Recurring afternoon energy slumps, physical lethargy, and low morning motivation.",
                whyHelpful = "Balancing macronutrient plates stabilizes glucose levels, while regular hydration and circadian sleep prevent cellular energy depletion.",
                practicalSuggestions = listOf(
                    "Replace refined sugary snacks with balanced protein-rich alternatives to avoid reactive crashes.",
                    "Start your day with 1-2 glasses of water before caffeine.",
                    "Take a 10-minute brisk walk outdoors during midday slumps for an instant oxygen boost.",
                    "Keep nighttime sleep and wake times strictly consistent."
                ),
                precautions = "Do not rely on multiple energy drinks or excessive espresso shots to mask chronic fatigue.",
                medicalGuidance = "Consult a physician if fatigue persists for more than 4 weeks, or is accompanied by unexplained weight loss, night sweats, pale conjunctiva (anemia), or swollen lymph nodes."
            ),
            marathi = LocalizedWellnessConcern(
                name = "सततचा थकवा व सुस्ती",
                description = "दुपारी अचानक गळून गेल्यासारखे वाटणे, कामात उत्साह नसणे आणि सकाळी उठल्यावरही थकवा जाणवणे.",
                whyHelpful = "संतुलित आहार, पुरेसे पाणी आणि नियमित झोपेमुळे रक्तातील साखरेचे चढउतार टळतात आणि शरीराला अखंड ऊर्जा मिळते.",
                practicalSuggestions = listOf(
                    "गोड पदार्थांऐवजी मूग डाळ, हरभरे किंवा सुकामेवा खा जेणेकरून ऊर्जा टिकून राहील.",
                    "सकाळी उठल्यावर चहाऐवजी आधी १-२ ग्लास कोमट पाणी प्या.",
                    "दुपारी थकवा जाणवल्यास १० मिनिटे मोकळ्या हवेत जलद चालून या.",
                    "दररोज रात्री एकाच ठराविक वेळी झोपण्याचा आणि उठण्याचा नियम पाळा."
                ),
                precautions = "थकवा घालवण्यासाठी वारंवार कडक चहा, कॉफी किंवा एनर्जी ड्रिंक्स पिणे टाळा.",
                medicalGuidance = "महिनाभर सतत थकवा राहिल्यास, डोळे किंवा नखे पांढरी पडल्यास (ॲनिमिया) अथवा अचानक वजन घटल्यास रक्त तपासणीसाठी डॉक्टरांचा सल्ला घ्या."
            ),
            hindi = LocalizedWellnessConcern(
                name = "सामान्य थकान और कमजोरी",
                description = "दिनभर सुस्ती रहना, दोपहर में ऊर्जा की भारी कमी और सुबह ताजगी न लगना।",
                whyHelpful = "संतुलित भोजन और पर्याप्त पानी शुगर के स्तर को स्थिर रखते हैं, जिससे शरीर को निरंतर ऊर्जा मिलती है।",
                practicalSuggestions = listOf(
                    "मीठे स्नैक्स के बदले अंकुरित दालें या मेवे खाएं ताकि शुगर क्रैश न हो।",
                    "सुबह की शुरुआत चाय की जगह 1-2 गिलास गुनगुने पानी से करें।",
                    "सुस्ती आने पर 10 मिनट खुली हवा में टहलें।",
                    "सोने और जागने का समय प्रतिदिन नियमित रखें।"
                ),
                precautions = "थकान मिटाने के लिए अत्यधिक चाय-कॉफी या एनर्जी ड्रिंक्स पर निर्भर न रहें।",
                medicalGuidance = "यदि थकान एक महीने से अधिक बनी रहे, चेहरे या नाखूनों में पीलापन दिखे या चक्कर आए तो तुरंत खून की जांच करवाकर डॉक्टर से मिलें।"
            )
        ),
        WellnessConcernItem(
            type = WellnessConcernType.DEHYDRATION,
            relatedTipIds = listOf(15, 16, 17, 18),
            english = LocalizedWellnessConcern(
                name = "Dehydration & Fluid Imbalance",
                description = "Thirst, dark concentrated urine, dry lips, headaches, and muscle cramping.",
                whyHelpful = "Timed fluid replenishment restores vascular volume and cellular electrolyte homeostasis.",
                practicalSuggestions = listOf(
                    "Carry a reusable 1-liter water bottle everywhere and monitor your daily refill tally.",
                    "Supplement plain water with lemon shikanji or tender coconut water when sweating heavily.",
                    "Use urine color (pale straw vs amber) as an objective self-check throughout the day.",
                    "Drink a glass of water immediately upon waking."
                ),
                precautions = "Do not gulp 2-3 liters in one go; spread hydration evenly to protect kidney electrolyte filtration.",
                medicalGuidance = "Seek emergency hospital treatment for severe dehydration symptoms: confusion, dizziness upon standing, lack of tears, or no urination for over 8-10 hours."
            ),
            marathi = LocalizedWellnessConcern(
                name = "शरीरातील पाण्याची कमतरता (निर्जलीकरण)",
                description = "वारंवार तहान लागणे, लघवीचा रंग गडद पिवळा होणे, ओठ कोरडे पडणे आणि डोके दुखणे.",
                whyHelpful = "वेळेवर पाणी व नैसर्गिक क्षार घेतल्याने शरीरातील पेशी ताजेतवाने राहतात आणि किडनीचे कार्य सुरळीत चालते.",
                practicalSuggestions = listOf(
                    "कामाच्या ठिकाणी १ लिटरची बाटली नेहमी जवळ ठेवा आणि पाणी मोजून प्या.",
                    "खूप घाम आल्यावर साध्या पाण्यासोबत शहाळ्याचे पाणी किंवा लिंबू पाणी प्या.",
                    "लघवीच्या रंगाकडे लक्ष ठेवून पाण्याची पातळी तपासा.",
                    "सकाळी उठल्या उठल्या १ ग्लास पाणी पिण्याची सवय ठेवा."
                ),
                precautions = "एकाच वेळी २-३ लिटर पाणी पिऊन पोट गच्च करू नका, दिवसभरात थोडे-थोडे प्या.",
                medicalGuidance = "८-१० तास लघवी न होणे, प्रचंड चक्कर येणे किंवा बोलताना अडखळल्यास तात्काळ सलाईन लावण्यासाठी रुग्णालयात जा."
            ),
            hindi = LocalizedWellnessConcern(
                name = "निर्जलीकरण और पानी की कमी",
                description = "गला सूखना, गहरा पीला पेशाब, सिरदर्द, थकान और मांसपेशियों में ऐंठन।",
                whyHelpful = "समय पर पर्याप्त जल और लवणों की पूर्ति से कोशिकाओं में संतुलन लौटता है और कमजोरी दूर होती है।",
                practicalSuggestions = listOf(
                    "अपने पास 1 लीटर की पानी की बोतल रखें और दिन में 2-3 बार भरें।",
                    "पसीना अधिक आने पर नारियल पानी या शिकंजी का सेवन करें।",
                    "पेशाब के हल्के पीले रंग को सामान्य मानक मानकर पानी पिएं।",
                    "सुबह उठते ही एक गिलास गुनगुना पानी पिएं।"
                ),
                precautions = "एक साथ बहुत सारा पानी न पिएं, दिनभर नियमित अंतराल पर पिएं।",
                medicalGuidance = "यदि 8 घंटे से पेशाब न आए, अत्यधिक चक्कर आएं या आंखों के आगे अंधेरा छाए तो तुरंत अस्पताल में दिखाएं।"
            )
        ),
        WellnessConcernItem(
            type = WellnessConcernType.POOR_SLEEP,
            relatedTipIds = listOf(11, 12, 13, 14),
            english = LocalizedWellnessConcern(
                name = "Poor Sleep Quality & Insomnia",
                description = "Difficulty falling asleep, frequent nighttime awakenings, or unrefreshing sleep.",
                whyHelpful = "Circadian entrainment, blue-light curfews, and early dinners permit natural melatonin release and deep slow-wave recovery.",
                practicalSuggestions = listOf(
                    "Power down all digital screens at least 60 minutes prior to bedtime.",
                    "Go to sleep and rise at the exact same hour every day, including weekends.",
                    "Stop caffeine intake strictly after 2:00 PM and eat dinner 3 hours before sleep.",
                    "Keep your bedroom cool (18-21°C), pitch dark, and silent."
                ),
                precautions = "Avoid relying on unprescribed OTC sedating antihistamines or alcohol to force sleep.",
                medicalGuidance = "Consult a physician or sleep clinic if insomnia lasts longer than 3 months, or if accompanied by loud gasping snores, daytime nodding off, or severe restless leg sensations."
            ),
            marathi = LocalizedWellnessConcern(
                name = "अपुरी किंवा अस्वस्थ झोप (अनिद्रा)",
                description = "रात्री उशिरापर्यंत झोप न लागणे, वारंवार जाग येणे किंवा सकाळी उठूनही थकवा वाटणे.",
                whyHelpful = "ठराविक झोपेचे वेळापत्रक, स्क्रीन बंद ठेवणे आणि संध्याकाळी चहा टाळल्याने मेंदूत झोपेचे हार्मोन्स वेळेवर तयार होतात.",
                practicalSuggestions = listOf(
                    "झोपण्याच्या १ तास आधी मोबाईल, टीव्ही पूर्ण बंद करा.",
                    "सुट्टीच्या दिवशीसुद्धा रोजच्याच वेळी उठण्याचा नियम करा.",
                    "दुपारी २ नंतर चहा किंवा कॉफी पिणे टाळा आणि रात्री हलके जेवा.",
                    "बेडरूम शांत, अंधारलेली आणि हवेशीर ठेवा."
                ),
                precautions = "झोप येण्यासाठी मद्यपान किंवा डॉक्टरांच्या सल्ल्याशिवाय झोपेच्या गोळ्या घेऊ नका.",
                medicalGuidance = "३ महिन्यांपेक्षा जास्त काळ झोप न लागणे किंवा झोपेत प्रचंड घोरणे व दम लागणे अशी लक्षणे असल्यास स्लीप स्पेशालिस्टचा सल्ला घ्या."
            ),
            hindi = LocalizedWellnessConcern(
                name = "खराब नींद और अनिद्रा",
                description = "बिस्तर पर जाने के बाद नींद न आना, रात में बार-बार जागना या सुबह थकान रहना।",
                whyHelpful = "नियमित समय पर सोने, स्क्रीन से दूरी और जल्दी भोजन करने से मेलाटोनिन बढ़ता है और गहरी नींद आती है।",
                practicalSuggestions = listOf(
                    "सोने से 1 घंटा पहले फोन और टीवी को पूरी तरह बंद कर दें।",
                    "सप्ताह के सातों दिन एक ही समय पर जागने की आदत डालें।",
                    "दोपहर 2 बजे के बाद चाय-कॉफी न लें और हल्का डिनर करें।",
                    "कमरे में पूरा अंधेरा, ठंडक और शांति बनाए रखें।"
                ),
                precautions = "नींद के लिए शराब या बिना डॉक्टर की नींद की गोलियों का सहारा न लें।",
                medicalGuidance = "यदि अनिद्रा 3 महीने से अधिक चले या खर्राटों के साथ सांस रुकने का अहसास हो तो तुरंत डॉक्टर से परामर्श लें।"
            )
        ),
        WellnessConcernItem(
            type = WellnessConcernType.DIGESTIVE_DISCOMFORT,
            relatedTipIds = listOf(2, 4, 14, 16),
            english = LocalizedWellnessConcern(
                name = "Digestive Discomfort & Bloating",
                description = "Post-meal heaviness, gas, acidity, slow transit, and irregular bowel habits.",
                whyHelpful = "High fiber, thorough mastication, warm morning water, and dinner buffers restore enzymatic digestion and intestinal motility.",
                practicalSuggestions = listOf(
                    "Chew every bite slowly (20-25 times) without phone or television distractions.",
                    "Increase daily dietary fiber through oats, leafy greens, and whole fruits with plenty of water.",
                    "Drink a glass of warm water right after waking up to gently stimulate bowel peristalsis.",
                    "Finish dinner at least 2.5 to 3 hours before lying down to prevent acid reflux."
                ),
                precautions = "Do not take fiber supplements dry; always dissolve in plenty of water. Avoid carbonated sodas.",
                medicalGuidance = "Consult a gastroenterologist if symptoms include persistent vomiting, unintended weight loss, black tarry stools, or severe focal abdominal pain."
            ),
            marathi = LocalizedWellnessConcern(
                name = "अपचन, गॅसेस व पोट फुगणे",
                description = "जेवणानंतर पोट जड वाटणे, आंबट ढेकर, पोटात गॅस होणे आणि पोट साफ न होणे.",
                whyHelpful = "घास चावून खाणे, फायबरयुक्त अन्न, सकाळी कोमट पाणी आणि रात्री लवकर जेवल्याने पचनसंस्था निरोगी राहते.",
                practicalSuggestions = listOf(
                    "प्रत्येक घास सावकाश २० ते २५ वेळा चावून शांतपणे जेवा.",
                    "आहारात ओट्स, पालेभाज्या आणि फळांचा समावेश करा आणि भरपूर पाणी प्या.",
                    "सकाळी उठल्यावर १ ग्लास कोमट पाणी पिऊन पोट साफ करा.",
                    "रात्रीचे जेवण झोपण्याच्या किमान अडीच ते तीन तास आधी घ्या."
                ),
                precautions = "फायबर पावडर घेताना भरपूर पाणी प्या. थंड शीतपेये पिणे टाळा.",
                medicalGuidance = "काळ्या रंगाची विष्ठा, शौचातून रक्त पडणे, सतत उलट्या होणे किंवा पोटात असह्य दुखल्यास तातडीने डॉक्टरांकडे जा."
            ),
            hindi = LocalizedWellnessConcern(
                name = "पाचन की समस्या और पेट फूलना",
                description = "भोजन के बाद भारीपन, गैस, एसिडिटी और पेट साफ न होना।",
                whyHelpful = "भोजन को अच्छी तरह चबाने, फाइबर युक्त आहार और गुनगुने पानी से पाचन शक्ति और आंतों की सेहत सुधरती है।",
                practicalSuggestions = listOf(
                    "हर निवाले को 20 से 25 बार अच्छी तरह चबाकर खाएं।",
                    "आहार में दलिया, साबुत दालें और मौसमी फल शामिल करें।",
                    "सुबह उठते ही एक गिलास गुनगुना पानी पिएं।",
                    "सोने से कम से कम 2.5-3 घंटे पहले रात का भोजन कर लें।"
                ),
                precautions = "फाइबर के साथ पानी की कमी न होने दें। कोल्ड ड्रिंक्स और तले-भुने भोजन से बचें।",
                medicalGuidance = "यदि शौच में खून दिखे, पेट में तेज चुभन वाला दर्द हो या बार-बार उल्टी आए तो तुरंत पेट रोग विशेषज्ञ से संपर्क करें।"
            )
        ),
        WellnessConcernItem(
            type = WellnessConcernType.LOW_PHYSICAL_ACTIVITY,
            relatedTipIds = listOf(6, 7, 8, 9),
            english = LocalizedWellnessConcern(
                name = "Sedentary Routine & Joint Stiffness",
                description = "Prolonged sitting at desks, back tightness, neck aches, and low physical stamina.",
                whyHelpful = "Aerobic sessions, resistance movements, and desk mobility breaks restore circulation and joint synovial lubrication.",
                practicalSuggestions = listOf(
                    "Aim for 30 minutes of moderate brisk walking 5 days every week.",
                    "Stand up and do a 90-second posture reset every 45 minutes of desk work.",
                    "Perform basic bodyweight squats and push-ups 2-3 times weekly.",
                    "Keep your screen at eye level with feet flat on the floor."
                ),
                precautions = "Do not attempt extreme heavy lifting without warm-up or proper form.",
                medicalGuidance = "Consult an orthopedic specialist if joint stiffness is accompanied by hot swelling, joint redness, or back pain radiating down past the knee with numbness."
            ),
            marathi = LocalizedWellnessConcern(
                name = "शारीरिक निष्क्रियता व सांधे आखडणे",
                description = "दिवसभर बसून काम करणे, पाठ व कंबर दुखणे, मान आखडणे आणि थकवा जाणवणे.",
                whyHelpful = "नियमित चालणे, स्ट्रेचिंग आणि स्नायूंच्या व्यायामामुळे सांध्यांमधील वंगण टिकून राहते आणि रक्ताभिसरण सुधारते.",
                practicalSuggestions = listOf(
                    "आठवड्यातून ५ दिवस किमान ३० मिनिटे जलद चालण्याचा सराव करा.",
                    "कामादरम्यान दर ४५ मिनिटांनी उभे राहून थोडे चाला आणि स्ट्रेचिंग करा.",
                    "आठवड्यातून २-३ वेळा हलके जोर-बैठका (स्क्वॅट्स) करा.",
                    "संगणकाचा स्क्रीन डोळ्यांच्या रेषेत ठेवून पाठीचा कणा ताठ ठेवा."
                ),
                precautions = "वॉर्म-अप केल्याशिवाय एकदम जड व्यायाम करू नका.",
                medicalGuidance = "सांध्यांवर लालसर सूज येणे, सांधे प्रचंड गरम लागणे किंवा कंबरेतील वेदना पायात खाली उतरत असल्यास डॉक्टरांचा सल्ला घ्या."
            ),
            hindi = LocalizedWellnessConcern(
                name = "शारीरिक निष्क्रियता और जोड़ों की जकड़न",
                description = "लंबे समय तक बैठे रहना, कमर व गर्दन में दर्द, अकड़न और कम सहनशक्ति।",
                whyHelpful = "रोजाना टहलने, स्ट्रेचिंग और हल्की कसरत से जोड़ों का लचीलापन और मांसपेशियों की ताकत बढ़ती है।",
                practicalSuggestions = listOf(
                    "सप्ताह में 5 दिन कम से कम 30 मिनट तेज गति से टहलें।",
                    "डेस्क पर काम करते समय हर 45 मिनट में 1-2 मिनट खड़े होकर हाथ-पैर स्ट्रेच करें।",
                    "हफ्ते में 2-3 बार दंड-बैठक या बॉडीवेट कसरत करें।",
                    "कंप्यूटर स्क्रीन को आंखों के सामने रखें और कुर्सी पर सीधे बैठें।"
                ),
                precautions = "बिना वार्म-अप के भारी वजन न उठाएं और रीढ़ की हड्डी को न झुकाएं।",
                medicalGuidance = "यदि जोड़ों में तेज सूजन, लाली या कमर का दर्द पैर के नीचे तक सुन्नपन के साथ जाए तो तुरंत डॉक्टर को दिखाएं।"
            )
        ),
        WellnessConcernItem(
            type = WellnessConcernType.STRESS_MENTAL_FATIGUE,
            relatedTipIds = listOf(19, 20, 21, 22),
            english = LocalizedWellnessConcern(
                name = "Stress & Cognitive Mental Fatigue",
                description = "Nervous tension, feeling overwhelmed, racing heartbeat, and burnout.",
                whyHelpful = "Mindful breathing stimulates vagal parasympathetic tone, while gratitude and nature immersion reduce circulating stress cortisol.",
                practicalSuggestions = listOf(
                    "Practice the 4-7-8 breathing method for 4 cycles whenever you feel overwhelmed.",
                    "Write down 3 things you are grateful for each morning or before bed.",
                    "Spend 20-30 minutes in a green park or garden without checking your phone.",
                    "Have a meaningful face-to-face or phone conversation with a trusted friend weekly."
                ),
                precautions = "Wellness breathwork supports calm; it is not a replacement for professional clinical psychiatric care.",
                medicalGuidance = "Seek mental health evaluation if feelings of severe anxiety, panic attacks, clinical depression, or thoughts of self-harm arise."
            ),
            marathi = LocalizedWellnessConcern(
                name = "मानसिक ताण व मेंदूचा थकवा",
                description = "सतत चिंता वाटणे, डोके भणभणणे, छातीत धडधड होणे आणि मानसिक थकवा जाणवणे.",
                whyHelpful = "प्राणायाम, कृतज्ञता आणि निसर्गाच्या सानिध्यात वेळ घालवल्याने ताणाचे संप्रेरक कमी होऊन मनाला शांतता मिळते.",
                practicalSuggestions = listOf(
                    "तणाव जाणवताच ४-७-८ श्वास पद्धतीचा वापर करून मन शांत करा.",
                    "रोज रात्री डायरीत दिवसभरातील चांगल्या ३ गोष्टी लिहून कृतज्ञता व्यक्त करा.",
                    "आठवड्यातून २-३ वेळा बागेत किंवा झाडांच्या सानिध्यात २० मिनिटे शांत फिरा.",
                    "जवळच्या मित्राशी किंवा कुटुंबाशी आठवड्यातून एकदा मनमोकळ्या गप्पा मारा."
                ),
                precautions = "प्राणायाम हा मनाच्या समाधानासाठी आहे, तीव्र मानसिक आजारांवर डॉक्टरी उपचारांची गरज असते.",
                medicalGuidance = "सतत भीती वाटणे, तीव्र नैराश्य, कामात लक्ष न लागणे किंवा आत्महत्येचे विचार आल्यास त्वरित मानसोपचारतज्ज्ञांची मदत घ्या."
            ),
            hindi = LocalizedWellnessConcern(
                name = "मानसिक तनाव और थकान",
                description = "घबराहट, अत्यधिक चिंता, दिल की तेज धड़कन और मानसिक रूप से टूटा हुआ महसूस करना।",
                whyHelpful = "गहरी सांस लेने से वेगस नर्व शांत होती है, और आभार व प्रकृति के संपर्क से तनाव हार्मोन कोर्टिसोल तेजी से घटता है।",
                practicalSuggestions = listOf(
                    "घबराहट होने पर 4-7-8 श्वास तकनीक का 4 बार अभ्यास करें।",
                    "प्रतिदिन 3 ऐसी बातें लिखें जिनके लिए आप आभारी हैं।",
                    "सप्ताह में 2-3 बार किसी पार्क में 20 मिनट बिना फोन के टहलें।",
                    "अपने किसी आत्मीय मित्र से दिल खोलकर बातचीत करें।"
                ),
                precautions = "यह सामान्य तनाव प्रबंधन के लिए है, गंभीर डिप्रेशन या एंग्जायटी में डॉक्टरी सलाह आवश्यक है।",
                medicalGuidance = "यदि अत्यधिक घबराहट के दौरे पड़ें, लगातार उदासी बनी रहे या खुद को नुकसान पहुंचाने के विचार आएं तो तुरंत डॉक्टर से संपर्क करें।"
            )
        ),
        WellnessConcernItem(
            type = WellnessConcernType.NUTRITIONAL_CONCERNS,
            relatedTipIds = listOf(1, 2, 3, 5),
            english = LocalizedWellnessConcern(
                name = "Nutritional Gaps & General Diet Quality",
                description = "Diet lacking whole produce, frequent reliance on fast food, and low micronutrient intake.",
                whyHelpful = "Emphasizing whole unprocessed grains, anti-inflammatory spices, and colorful produce provides the necessary vitamins, minerals, and antioxidants.",
                practicalSuggestions = listOf(
                    "Ensure half your plate consists of colorful seasonal vegetables and fresh salads.",
                    "Choose whole grains and legumes over refined white flour (maida) products.",
                    "Incorporate natural anti-inflammatory spices like turmeric and ginger into cooking.",
                    "Limit ultra-processed sugary snacks and bottled beverages."
                ),
                precautions = "Consult a physician before taking high-dose single-vitamin or mineral supplements.",
                medicalGuidance = "Consult a doctor for unexplained weight loss, chronic mouth ulcers, brittle spoon-shaped nails, or persistent hair thinning."
            ),
            marathi = LocalizedWellnessConcern(
                name = "पोषणाचा अभाव व आहाराची गुणवत्ता",
                description = "आहारात ताज्या भाज्या व फळांचा अभाव असणे, जंक फूडचे अतिसेवन आणि शरीरात आवश्यक जीवनसत्त्वांची कमतरता.",
                whyHelpful = "संपूर्ण अन्न, पालेभाज्या आणि औषधी मसाल्यांमुळे शरीराला आवश्यक जीवनसत्त्वे, खनिजे आणि प्रतिकारशक्ती मिळते.",
                practicalSuggestions = listOf(
                    "ताटातील अर्धा भाग ताज्या भाज्या आणि सॅलडने भरा.",
                    "मैद्याच्या पदार्थांऐवजी ज्वारी, बाजरी आणि डाळींचा आहारात समावेश करा.",
                    "हळद, आले, लसूण यांसारखे नैसर्गिक मसाले रोजच्या आहारात वापरा.",
                    "पॅकबंद गोड पदार्थ आणि शीतपेये पिणे पूर्णपणे टाळा."
                ),
                precautions = "डॉक्टरांच्या सल्ल्याशिवाय स्वतःहून महागड्या व्हिटॅमिन गोळ्यांचा अतिरेक करू नका.",
                medicalGuidance = "अचानक वजन घटणे, वारंवार तोंडात अल्सर होणे किंवा केस प्रचंड गळत असल्यास डॉक्टरांकडून रक्त तपासणी करून घ्या."
            ),
            hindi = LocalizedWellnessConcern(
                name = "पोषण की कमी और असंतुलित आहार",
                description = "भोजन में ताजे फलों-सब्जियों की कमी, बाहर के खाने पर अधिक निर्भरता और आवश्यक विटामिन्स का अभाव।",
                whyHelpful = "साबुत अनाज, हरी सब्जियां और घरेलू मसाले शरीर को जरूरी पोषण और एंटीऑक्सीडेंट सुरक्षा देते हैं।",
                practicalSuggestions = listOf(
                    "अपनी थाली का आधा भाग ताजी सब्जियों और सलाद से भरें।",
                    "मैदे की जगह साबुत अनाज और दालों को प्राथमिकता दें।",
                    "हल्दी, अदरक और दालचीनी जैसे घरेलू मसालों का उपयोग करें।",
                    "प्रोसेस्ड मीठे स्नैक्स और कोल्ड ड्रिंक्स से दूरी बनाएं।"
                ),
                precautions = "बिना डॉक्टर की सलाह के मनमाने ढंग से मल्टीविटामिन की गोलियां न लें।",
                medicalGuidance = "अचानक वजन घटना, बार-बार मुंह में छाले होना या अत्यधिक बाल झड़ने पर डॉक्टर से रक्त परीक्षण कराएं।"
            )
        )
    )

    fun getTipDetails(tipId: Int): TipDetailBundle? = allDetailsMap[tipId]

    fun getRelatedTips(tipId: Int): List<HealthTipItem> {
        val bundle = allDetailsMap[tipId] ?: return emptyList()
        val all = HealthTipsRepository.getAllTips().associateBy { it.id }
        return bundle.relatedTipIds.mapNotNull { all[it] }
    }

    fun getAllWellnessConcerns(): List<WellnessConcernItem> = wellnessConcerns

    fun getWellnessConcernsForTip(tipId: Int): List<WellnessConcernItem> {
        return wellnessConcerns.filter { it.relatedTipIds.contains(tipId) }
    }

    fun getTipsForConcern(type: WellnessConcernType): List<HealthTipItem> {
        val concern = wellnessConcerns.firstOrNull { it.type == type } ?: return emptyList()
        val all = HealthTipsRepository.getAllTips().associateBy { it.id }
        return concern.relatedTipIds.mapNotNull { all[it] }
    }
}
