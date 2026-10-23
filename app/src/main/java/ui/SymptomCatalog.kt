package ui

enum class SymptomCategory {
    COMMON,
    PAIN,
    DIGESTIVE,
    RESPIRATORY,
    SKIN,
    MORE
}

enum class SymptomIconType {
    HEADACHE,
    FEVER,
    COUGH,
    BACK_PAIN,
    CHEST_PAIN,
    NAUSEA,
    PAIN,
    DIGESTIVE,
    LUNGS,
    SKIN,
    EYE,
    EAR,
    THROAT,
    URINARY,
    HEART,
    BRAIN,
    DROPLET,
    THERMOMETER,
    FATIGUE,
    DIZZY,
    NOSE,
    BONE,
    BODY
}

data class SymptomModel(
    val id: String,
    val enName: String,
    val mrName: String,
    val hiName: String,
    val category: SymptomCategory,
    val iconType: SymptomIconType
) {
    fun localizedName(language: String): String {
        return when (language.lowercase()) {
            "mr", "marathi", "मराठी" -> mrName
            "hi", "hindi", "हिंदी" -> hiName
            else -> enName
        }
    }
}

object SymptomCatalog {

    val TOP_SIX_COMMON_IDS = listOf(
        "headache",
        "fever",
        "cough",
        "back pain",
        "chest pain",
        "nausea"
    )

    private val ALL_ITEMS: List<SymptomModel> = listOf(
        SymptomModel("acidic taste", "Acidic Taste", "आंबट चव", "खट्टा स्वाद", SymptomCategory.DIGESTIVE, SymptomIconType.DIGESTIVE),
        SymptomModel("anxiety", "Anxiety", "चिंता / अस्वस्थता", "चिंता / घबराहट", SymptomCategory.MORE, SymptomIconType.HEART),
        SymptomModel("back pain", "Back Pain", "पाठदुखी", "पीठ दर्द", SymptomCategory.COMMON, SymptomIconType.BACK_PAIN),
        SymptomModel("blisters", "Blisters", "फोड येणे", "छाले / फफोले", SymptomCategory.SKIN, SymptomIconType.SKIN),
        SymptomModel("bloating", "Bloating", "पोट फुगणे", "पेट फूलना", SymptomCategory.DIGESTIVE, SymptomIconType.DIGESTIVE),
        SymptomModel("blood in urine", "Blood in Urine", "लघवीतून रक्त येणे", "पेशाब में खून आना", SymptomCategory.MORE, SymptomIconType.URINARY),
        SymptomModel("blurred vision", "Blurred Vision", "धूसर दिसणे", "धुंधली दृष्टि", SymptomCategory.MORE, SymptomIconType.EYE),
        SymptomModel("body pain", "Body Pain", "अंगदुखी", "बदन दर्द", SymptomCategory.PAIN, SymptomIconType.PAIN),
        SymptomModel("bone pain", "Bone Pain", "हाडे दुखणे", "हड्डियों में दर्द", SymptomCategory.PAIN, SymptomIconType.BONE),
        SymptomModel("burning urination", "Burning Urination", "लघवी करताना जळजळ", "पेशाब में जलन", SymptomCategory.MORE, SymptomIconType.URINARY),
        SymptomModel("chest discomfort", "Chest Discomfort", "छातीत अस्वस्थता", "सीने में बेचैनी", SymptomCategory.RESPIRATORY, SymptomIconType.HEART),
        SymptomModel("chest pain", "Chest Pain", "छातीत दुखणे", "सीने में दर्द", SymptomCategory.COMMON, SymptomIconType.CHEST_PAIN),
        SymptomModel("chest tightness", "Chest Tightness", "छाती आवळल्यासारखी वाटणे", "सीने में जकड़न", SymptomCategory.RESPIRATORY, SymptomIconType.LUNGS),
        SymptomModel("chills", "Chills", "थंडी वाजणे", "कंपकंपी", SymptomCategory.COMMON, SymptomIconType.FEVER),
        SymptomModel("cloudy urine", "Cloudy Urine", "गढूळ लघवी", "धुंधला पेशाब", SymptomCategory.MORE, SymptomIconType.URINARY),
        SymptomModel("cold intolerance", "Cold Intolerance", "थंडी सहन न होणे", "ठंड बर्दाश्त न होना", SymptomCategory.MORE, SymptomIconType.THERMOMETER),
        SymptomModel("confusion", "Confusion", "गोंधळलेली अवस्था", "भ्रम / उलझन", SymptomCategory.MORE, SymptomIconType.BRAIN),
        SymptomModel("constipation", "Constipation", "बद्धकोष्ठता", "कब्ज", SymptomCategory.DIGESTIVE, SymptomIconType.DIGESTIVE),
        SymptomModel("cough", "Cough", "खोकला", "खांसी", SymptomCategory.COMMON, SymptomIconType.COUGH),
        SymptomModel("dark urine", "Dark Urine", "गडद रंगाची लघवी", "गहरे रंग का पेशाब", SymptomCategory.MORE, SymptomIconType.URINARY),
        SymptomModel("dehydration", "Dehydration", "पाण्याची कमतरता", "निर्जलीकरण / पानी की कमी", SymptomCategory.MORE, SymptomIconType.DROPLET),
        SymptomModel("diarrhea", "Diarrhea", "जुलाब / अतिसार", "दस्त / अतिसार", SymptomCategory.DIGESTIVE, SymptomIconType.DIGESTIVE),
        SymptomModel("difficulty swallowing", "Difficulty Swallowing", "घास गिळताना त्रास", "निगलने में कठिनाई", SymptomCategory.DIGESTIVE, SymptomIconType.THROAT),
        SymptomModel("dizziness", "Dizziness", "चक्कर येणे", "चक्कर आना", SymptomCategory.COMMON, SymptomIconType.DIZZY),
        SymptomModel("dry cough", "Dry Cough", "कोरडा खोकला", "सूखी खांसी", SymptomCategory.RESPIRATORY, SymptomIconType.COUGH),
        SymptomModel("dry mouth", "Dry Mouth", "तोंड सुकणे", "मुंह सूखना", SymptomCategory.MORE, SymptomIconType.DROPLET),
        SymptomModel("dry skin", "Dry Skin", "कोरडी त्वचा", "रूखी त्वचा", SymptomCategory.SKIN, SymptomIconType.SKIN),
        SymptomModel("ear discharge", "Ear Discharge", "कानातून पाणी वाहणे", "कान से मवाद बहना", SymptomCategory.MORE, SymptomIconType.EAR),
        SymptomModel("ear pain", "Ear Pain", "कानदुखी", "कान दर्द", SymptomCategory.PAIN, SymptomIconType.EAR),
        SymptomModel("excessive thirst", "Excessive Thirst", "जास्त तहान लागणे", "अत्यधिक प्यास", SymptomCategory.MORE, SymptomIconType.DROPLET),
        SymptomModel("eye discharge", "Eye Discharge", "डोळ्यातून पाणी/घाण येणे", "आंखों से स्राव आना", SymptomCategory.MORE, SymptomIconType.EYE),
        SymptomModel("eye irritation", "Eye Irritation", "डोळ्यांची जळजळ", "आंखों में जलन", SymptomCategory.MORE, SymptomIconType.EYE),
        SymptomModel("eye strain", "Eye Strain", "डोळ्यांवर ताण", "आंखों में खिंचाव", SymptomCategory.MORE, SymptomIconType.EYE),
        SymptomModel("facial pain", "Facial Pain", "चेहऱ्यावर दुखणे", "चेहरे में दर्द", SymptomCategory.PAIN, SymptomIconType.PAIN),
        SymptomModel("fast heartbeat", "Fast Heartbeat", "हृदयाचे ठोके वाढणे", "तेज दिल की धड़कन", SymptomCategory.MORE, SymptomIconType.HEART),
        SymptomModel("fatigue", "Fatigue", "थकवा", "थकान", SymptomCategory.COMMON, SymptomIconType.FATIGUE),
        SymptomModel("fever", "Fever", "ताप", "बुखार", SymptomCategory.COMMON, SymptomIconType.FEVER),
        SymptomModel("frequent urination", "Frequent Urination", "वारंवार लघवी होणे", "बार-बार पेशाब आना", SymptomCategory.MORE, SymptomIconType.URINARY),
        SymptomModel("gas", "Gas", "पोटात गॅस होणे", "पेट में गैस", SymptomCategory.DIGESTIVE, SymptomIconType.DIGESTIVE),
        SymptomModel("hard stool", "Hard Stool", "कठीण शौच", "कठिन मल", SymptomCategory.DIGESTIVE, SymptomIconType.DIGESTIVE),
        SymptomModel("headache", "Headache", "डोकेदुखी", "सिरदर्द", SymptomCategory.COMMON, SymptomIconType.HEADACHE),
        SymptomModel("hearing difficulty", "Hearing Difficulty", "ऐकण्यास त्रास", "सुनने में कठिनाई", SymptomCategory.MORE, SymptomIconType.EAR),
        SymptomModel("heartburn", "Heartburn", "छातीत जळजळ (अॅसिडिटी)", "सीने में जलन (एसिडिटी)", SymptomCategory.DIGESTIVE, SymptomIconType.DIGESTIVE),
        SymptomModel("heat intolerance", "Heat Intolerance", "उष्णता सहन न होणे", "गर्मी बर्दाश्त न होना", SymptomCategory.MORE, SymptomIconType.THERMOMETER),
        SymptomModel("high fever", "High Fever", "तीव्र ताप", "तेज बुखार", SymptomCategory.COMMON, SymptomIconType.FEVER),
        SymptomModel("increased hunger", "Increased Hunger", "जास्त भूक लागणे", "अधिक भूख लगना", SymptomCategory.MORE, SymptomIconType.DIGESTIVE),
        SymptomModel("itchy eyes", "Itchy Eyes", "डोळ्यांना खाज सुटणे", "आंखों में खुजली", SymptomCategory.MORE, SymptomIconType.EYE),
        SymptomModel("itchy nose", "Itchy Nose", "नाकाला खाज सुटणे", "नाक में खुजली", SymptomCategory.RESPIRATORY, SymptomIconType.NOSE),
        SymptomModel("itchy rash", "Itchy Rash", "खाज सुटणारे पुरळ", "खुजलीदार दाने", SymptomCategory.SKIN, SymptomIconType.SKIN),
        SymptomModel("itchy skin", "Itchy Skin", "त्वचेला खाज सुटणे", "त्वचा में खुजली", SymptomCategory.SKIN, SymptomIconType.SKIN),
        SymptomModel("loss of appetite", "Loss of Appetite", "भूक मंदावणे", "भूख में कमी", SymptomCategory.DIGESTIVE, SymptomIconType.DIGESTIVE),
        SymptomModel("loss of taste or smell", "Loss of Taste or Smell", "चव किंवा वास न येणे", "स्वाद या गंध की कमी", SymptomCategory.MORE, SymptomIconType.NOSE),
        SymptomModel("low mood", "Low Mood", "उदासीनता", "उदास मन / सुस्ती", SymptomCategory.MORE, SymptomIconType.BRAIN),
        SymptomModel("lower abdominal pain", "Lower Abdominal Pain", "ओटीपोटात दुखणे", "पेड़ू में दर्द", SymptomCategory.PAIN, SymptomIconType.DIGESTIVE),
        SymptomModel("mild fever", "Mild Fever", "हलका ताप", "हल्का बुखार", SymptomCategory.COMMON, SymptomIconType.FEVER),
        SymptomModel("mild headache", "Mild Headache", "हलकी डोकेदुखी", "हल्का सिरदर्द", SymptomCategory.PAIN, SymptomIconType.HEADACHE),
        SymptomModel("mucus", "Mucus", "कफ / शेंबूड", "बलगम / कफ", SymptomCategory.RESPIRATORY, SymptomIconType.LUNGS),
        SymptomModel("muscle weakness", "Muscle Weakness", "स्नायूंमधील अशक्तपणा", "मांसपेशियों में कमजोरी", SymptomCategory.MORE, SymptomIconType.PAIN),
        SymptomModel("nasal congestion", "Nasal Congestion", "नाक चोंदणे", "नाक बंद होना", SymptomCategory.RESPIRATORY, SymptomIconType.NOSE),
        SymptomModel("nausea", "Nausea", "मळमळ", "मतली / उल्टी जैसा लगना", SymptomCategory.COMMON, SymptomIconType.NAUSEA),
        SymptomModel("neck pain", "Neck Pain", "मानदुखी", "गर्दन दर्द", SymptomCategory.PAIN, SymptomIconType.PAIN),
        SymptomModel("neck swelling", "Neck Swelling", "मानेवर सूज", "गर्दन में सूजन", SymptomCategory.MORE, SymptomIconType.THROAT),
        SymptomModel("night cough", "Night Cough", "रात्री येणारा खोकला", "रात में खांसी", SymptomCategory.RESPIRATORY, SymptomIconType.COUGH),
        SymptomModel("nosebleed", "Nosebleed", "नाकातून रक्त येणे", "नाक से खून आना", SymptomCategory.RESPIRATORY, SymptomIconType.NOSE),
        SymptomModel("pain behind eyes", "Pain Behind Eyes", "डोळ्यांमागे दुखणे", "आंखों के पीछे दर्द", SymptomCategory.PAIN, SymptomIconType.EYE),
        SymptomModel("painful urination", "Painful Urination", "लघवी करताना वेदना", "पेशाब में दर्द", SymptomCategory.PAIN, SymptomIconType.URINARY),
        SymptomModel("pale skin", "Pale Skin", "फिके पडलेले शरीर", "त्वचा का पीलापन", SymptomCategory.SKIN, SymptomIconType.SKIN),
        SymptomModel("postnasal drip", "Postnasal Drip", "घशात कफ उतरणे", "गले में बलगम गिरना", SymptomCategory.RESPIRATORY, SymptomIconType.THROAT),
        SymptomModel("rash", "Rash", "अंगावर पुरळ", "शरीर पर दाने", SymptomCategory.SKIN, SymptomIconType.SKIN),
        SymptomModel("red eyes", "Red Eyes", "डोळे लाल होणे", "आंखें लाल होना", SymptomCategory.MORE, SymptomIconType.EYE),
        SymptomModel("red skin", "Red Skin", "त्वचा लाल होणे", "त्वचा का लाल होना", SymptomCategory.SKIN, SymptomIconType.SKIN),
        SymptomModel("regurgitation", "Regurgitation", "अन्न घशात परत येणे", "खाना मुंह में वापस आना", SymptomCategory.DIGESTIVE, SymptomIconType.DIGESTIVE),
        SymptomModel("runny nose", "Runny Nose", "नाक वाहणे", "नाक बहना", SymptomCategory.RESPIRATORY, SymptomIconType.NOSE),
        SymptomModel("sensitivity to light", "Sensitivity to Light", "प्रकाशाचा त्रास", "रोशनी से परेशानी", SymptomCategory.MORE, SymptomIconType.EYE),
        SymptomModel("sensitivity to sound", "Sensitivity to Sound", "आवाजाचा त्रास", "आवाज से संवेदनशीलता", SymptomCategory.MORE, SymptomIconType.EAR),
        SymptomModel("severe headache", "Severe Headache", "तीव्र डोकेदुखी", "तेज सिरदर्द", SymptomCategory.PAIN, SymptomIconType.HEADACHE),
        SymptomModel("severe side pain", "Severe Side Pain", "कुशीत तीव्र वेदना", "पसलियों में तेज दर्द", SymptomCategory.PAIN, SymptomIconType.PAIN),
        SymptomModel("shaking", "Shaking", "थरथरणे", "कांपना", SymptomCategory.MORE, SymptomIconType.BRAIN),
        SymptomModel("shortness of breath", "Shortness of Breath", "श्वास घेण्यास त्रास", "सांस लेने में तकलीफ", SymptomCategory.RESPIRATORY, SymptomIconType.LUNGS),
        SymptomModel("shoulder pain", "Shoulder Pain", "खांदा दुखणे", "कंधे में दर्द", SymptomCategory.PAIN, SymptomIconType.PAIN),
        SymptomModel("skin rash", "Skin Rash", "त्वचेवर पुरळ", "त्वचा पर दाने", SymptomCategory.SKIN, SymptomIconType.SKIN),
        SymptomModel("skin swelling", "Skin Swelling", "त्वचेवर सूज", "त्वचा पर सूजन", SymptomCategory.SKIN, SymptomIconType.SKIN),
        SymptomModel("slow heart rate", "Slow Heart Rate", "हृदयाचे ठोके मंदावणे", "धीमी दिल की धड़कन", SymptomCategory.MORE, SymptomIconType.HEART),
        SymptomModel("slow wound healing", "Slow Wound Healing", "जखमा उशिरा भरणे", "घाव देर से भरना", SymptomCategory.SKIN, SymptomIconType.SKIN),
        SymptomModel("sneezing", "Sneezing", "शिंका येणे", "छींक आना", SymptomCategory.RESPIRATORY, SymptomIconType.NOSE),
        SymptomModel("sore throat", "Sore Throat", "घसा खवखवणे", "गले में खराश", SymptomCategory.RESPIRATORY, SymptomIconType.THROAT),
        SymptomModel("stomach pain", "Stomach Pain", "पोटदुखी", "पेट दर्द", SymptomCategory.PAIN, SymptomIconType.DIGESTIVE),
        SymptomModel("straining during bowel movement", "Straining During Bowel Movement", "शौचास त्रास होणे", "मलत्याग में कठिनाई", SymptomCategory.DIGESTIVE, SymptomIconType.DIGESTIVE),
        SymptomModel("sweating", "Sweating", "खूप घाम येणे", "अत्यधिक पसीना", SymptomCategory.SKIN, SymptomIconType.DROPLET),
        SymptomModel("swollen neck glands", "Swollen Neck Glands", "मानेतील गाठी सुजणे", "गर्दन की ग्रंथियों में सूजन", SymptomCategory.MORE, SymptomIconType.THROAT),
        SymptomModel("swollen tonsils", "Swollen Tonsils", "टॉन्सिल्स सुजणे", "टॉन्सिल में सूजन", SymptomCategory.MORE, SymptomIconType.THROAT),
        SymptomModel("thirst", "Thirst", "तहान लागणे", "प्यास लगना", SymptomCategory.MORE, SymptomIconType.DROPLET),
        SymptomModel("tremor", "Tremor", "हात थरथरणे", "हाथ कांपना", SymptomCategory.MORE, SymptomIconType.BRAIN),
        SymptomModel("urgent urination", "Urgent Urination", "लघवीची तातडीची भावना", "पेशाब की तीव्र इच्छा", SymptomCategory.MORE, SymptomIconType.URINARY),
        SymptomModel("vomiting", "Vomiting", "उलटी", "उल्टी", SymptomCategory.COMMON, SymptomIconType.DIGESTIVE),
        SymptomModel("watery eyes", "Watery Eyes", "डोळ्यातून पाणी येणे", "आंखों से पानी आना", SymptomCategory.MORE, SymptomIconType.EYE),
        SymptomModel("weakness", "Weakness", "अशक्तपणा", "कमजोरी", SymptomCategory.COMMON, SymptomIconType.FATIGUE),
        SymptomModel("weight gain", "Weight Gain", "वजन वाढणे", "वजन बढ़ना", SymptomCategory.MORE, SymptomIconType.BODY),
        SymptomModel("weight loss", "Weight Loss", "वजन कमी होणे", "वजन कम होना", SymptomCategory.MORE, SymptomIconType.BODY),
        SymptomModel("wheezing", "Wheezing", "श्वास घेताना घरघर", "सांस में घरघराहट", SymptomCategory.RESPIRATORY, SymptomIconType.LUNGS)
    )

    private val BY_ID: Map<String, SymptomModel> = ALL_ITEMS.associateBy { it.id.lowercase() }

    fun getAll(): List<SymptomModel> = ALL_ITEMS

    fun get(id: String): SymptomModel? = BY_ID[id.trim().lowercase()]

    fun getDisplayName(id: String, language: String): String {
        return get(id)?.localizedName(language)
            ?: id.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    fun getTopSixCommon(): List<SymptomModel> {
        return TOP_SIX_COMMON_IDS.mapNotNull { get(it) }
    }

    fun getByCategory(category: SymptomCategory): List<SymptomModel> {
        return when (category) {
            SymptomCategory.COMMON -> ALL_ITEMS.filter { it.category == SymptomCategory.COMMON }
            SymptomCategory.PAIN -> ALL_ITEMS.filter { it.category == SymptomCategory.PAIN || it.id in listOf("back pain", "chest pain", "headache") }
            SymptomCategory.DIGESTIVE -> ALL_ITEMS.filter { it.category == SymptomCategory.DIGESTIVE || it.id == "nausea" }
            SymptomCategory.RESPIRATORY -> ALL_ITEMS.filter { it.category == SymptomCategory.RESPIRATORY || it.id in listOf("cough", "chest pain") }
            SymptomCategory.SKIN -> ALL_ITEMS.filter { it.category == SymptomCategory.SKIN }
            SymptomCategory.MORE -> ALL_ITEMS.filter { it.category == SymptomCategory.MORE }
        }
    }

    fun search(query: String, language: String, category: SymptomCategory? = null): List<SymptomModel> {
        val q = query.trim().lowercase()
        if (q.isBlank()) {
            return if (category != null) getByCategory(category) else ALL_ITEMS
        }
        val source = if (category != null && category != SymptomCategory.COMMON) getByCategory(category) else ALL_ITEMS
        return source.filter { item ->
            item.id.contains(q, ignoreCase = true) ||
            item.enName.contains(q, ignoreCase = true) ||
            item.mrName.contains(q, ignoreCase = true) ||
            item.hiName.contains(q, ignoreCase = true)
        }
    }
}
