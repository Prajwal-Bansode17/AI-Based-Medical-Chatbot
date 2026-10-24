package ui

import android.content.Context
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

data class MedicineRecord(
    val name: String,
    val generic: String,
    val brand: String,
    val ingredients: String,
    val drugClass: String,
    val uses: String,
    val sideEffects: String,
    val warnings: String,
    val precautions: String,
    val interactions: String
)

private data class SearchableMedicine(
    val medicine: MedicineRecord,
    val name: String,
    val generic: String,
    val brand: String,
    val ingredients: String
)

object MedicineRepository {

    @Volatile
    private var cachedMedicines: List<MedicineRecord>? = null

    @Volatile
    private var cachedInitialMedicines: List<MedicineRecord>? = null

    private const val MAX_SEARCH_CACHE_SIZE = 64
    private val searchCache = object : LinkedHashMap<String, List<MedicineRecord>>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<MedicineRecord>>?): Boolean {
            return size > MAX_SEARCH_CACHE_SIZE
        }
    }

    // ============================================================
    // VERIFIED POPULAR & COMMON MEDICINES (HUMAN-READABLE & ACCURATE)
    // ============================================================

    val verifiedMedicines: List<MedicineRecord> = listOf(
        MedicineRecord(
            name = "Dolo 650",
            generic = "Paracetamol",
            brand = "Dolo 650",
            ingredients = "Paracetamol 650 mg",
            drugClass = "Analgesic & Antipyretic",
            uses = "Dolo 650 is used to relieve mild to moderate pain such as headache, body pain, toothache, and to reduce fever.",
            sideEffects = "Usually well tolerated. Some people may experience nausea, stomach discomfort, or skin rash.",
            warnings = "Do not exceed the recommended dose of 4000 mg per day. Avoid alcohol.",
            precautions = "Do not take more than the recommended dose.\nAvoid using with other medicines containing paracetamol.\nConsult a doctor if you have liver problems.\nAsk a doctor before use in pregnancy or if you have any other medical condition.",
            interactions = "May interact with warfarin and other blood thinners."
        ),
        MedicineRecord(
            name = "Crocin 500",
            generic = "Paracetamol",
            brand = "Crocin 500",
            ingredients = "Paracetamol 500 mg",
            drugClass = "Analgesic & Antipyretic",
            uses = "Used to relieve mild to moderate pain and reduce fever caused by common cold, flu, headache, and body pain.",
            sideEffects = "May cause nausea, stomach discomfort, or allergic reactions in some people.",
            warnings = "Overdose can cause serious liver damage. Do not take with other paracetamol products.",
            precautions = "Do not exceed the recommended dose.\nAvoid alcohol while taking this medicine.\nConsult a doctor if you have liver disease or any other serious medical condition.\nAsk a doctor before use during pregnancy or breastfeeding.",
            interactions = "Alcohol increases risk of liver toxicity."
        ),
        MedicineRecord(
            name = "Calpol 650",
            generic = "Paracetamol",
            brand = "Calpol 650",
            ingredients = "Paracetamol 650 mg",
            drugClass = "Analgesic & Antipyretic",
            uses = "Provides fast relief from fever, body aches, muscle pains, and post-immunization discomfort.",
            sideEffects = "Generally safe when taken as directed. Rare side effects include skin itching or indigestion.",
            warnings = "Do not combine with other cold and flu medicines containing acetaminophen/paracetamol.",
            precautions = "Take with or after food if stomach upset occurs.\nDo not exceed 4 tablets in 24 hours.\nKeep out of reach of children.\nConsult your doctor if fever persists for more than 3 days.",
            interactions = "Consult physician if taking anticonvulsants or cholestyramine."
        ),
        MedicineRecord(
            name = "Paracetamol 500 mg",
            generic = "Paracetamol",
            brand = "Paracetamol 500 mg Tablet",
            ingredients = "Paracetamol 500 mg",
            drugClass = "Analgesic & Antipyretic",
            uses = "Effective relief from headache, toothache, arthritis, back pain, and cold-related fever.",
            sideEffects = "Mild nausea or allergic skin reactions may occur rarely.",
            warnings = "Strictly adhere to the prescribed dosage intervals.",
            precautions = "Maintain at least 4 to 6 hours between doses.\nDo not take with other paracetamol preparations.\nInform your doctor of any pre-existing kidney or liver impairment.",
            interactions = "Inform physician about all ongoing medications."
        ),
        MedicineRecord(
            name = "Paracetamol 125 mg",
            generic = "Paracetamol",
            brand = "Paracetamol 125 mg Suspension",
            ingredients = "Paracetamol 125 mg / 5 mL",
            drugClass = "Pediatric Antipyretic & Analgesic",
            uses = "Used for reduction of fever and relief of mild to moderate pain in infants and young children.",
            sideEffects = "Usually well tolerated by children. Occasional mild loose stools or drowsiness.",
            warnings = "Use only the calibrated measuring syringe or spoon provided.",
            precautions = "Verify pediatric dosage based on child's body weight.\nShake well before use.\nConsult pediatrician if symptoms persist beyond 48 hours.",
            interactions = "Do not administer with other pediatric cold syrups containing paracetamol."
        ),
        MedicineRecord(
            name = "PCM 650",
            generic = "Paracetamol",
            brand = "PCM 650",
            ingredients = "Paracetamol 650 mg",
            drugClass = "Analgesic & Antipyretic",
            uses = "Fast acting formulation for reduction of elevated body temperature and musculoskeletal aches.",
            sideEffects = "Very low incidence of side effects at therapeutic doses.",
            warnings = "Excess dose is harmful to the liver.",
            precautions = "Do not exceed stated dose.\nAvoid concurrent consumption of alcohol.\nStore in a dry location away from heat.",
            interactions = "Check with doctor before co-administering with prescription pain relievers."
        ),
        MedicineRecord(
            name = "P-650",
            generic = "Paracetamol",
            brand = "P-650",
            ingredients = "Paracetamol 650 mg",
            drugClass = "Analgesic & Antipyretic",
            uses = "Relief of pain and fever associated with seasonal infections, headaches, and joint stiffness.",
            sideEffects = "Rare gastrointestinal upset or mild allergic rash.",
            warnings = "Take with a full glass of water.",
            precautions = "Do not take more frequently than every 4-6 hours.\nConsult physician if pregnant or planning pregnancy.\nAvoid with other acetaminophen medications.",
            interactions = "Avoid alcohol and sedatives."
        ),
        MedicineRecord(
            name = "Ibuprofen 400 mg",
            generic = "Ibuprofen",
            brand = "Ibuprofen 400 mg Tablet",
            ingredients = "Ibuprofen 400 mg",
            drugClass = "Nonsteroidal Anti-inflammatory Drug (NSAID)",
            uses = "Relief of inflammatory pain, arthritis stiffness, dental pain, menstrual cramps, and fever.",
            sideEffects = "Heartburn, nausea, abdominal discomfort, or dizziness.",
            warnings = "May cause stomach bleeding if taken excessively or on an empty stomach.",
            precautions = "Take with food or milk to minimize gastrointestinal discomfort.\nDo not use if you have active stomach ulcers or asthma sensitive to NSAIDs.\nConsult a doctor if taking blood pressure medications.",
            interactions = "Interacts with aspirin, anticoagulants, and ACE inhibitors."
        ),
        MedicineRecord(
            name = "Amoxicillin 500 mg",
            generic = "Amoxicillin",
            brand = "Amoxicillin 500 mg Capsule",
            ingredients = "Amoxicillin 500 mg",
            drugClass = "Broad-spectrum Penicillin Antibiotic",
            uses = "Treatment of bacterial infections of the respiratory tract, ear, nose, throat, skin, and urinary tract.",
            sideEffects = "Diarrhea, mild nausea, vomiting, or skin rash.",
            warnings = "Do not use if allergic to penicillin or beta-lactam antibiotics.",
            precautions = "Complete the entire prescribed course even if symptoms resolve earlier.\nStay well-hydrated throughout therapy.\nSeek immediate medical care if allergic hives or breathing difficulty occurs.",
            interactions = "May reduce effectiveness of oral contraceptives. May interact with methotrexate."
        ),
        MedicineRecord(
            name = "Cetirizine 10 mg",
            generic = "Cetirizine",
            brand = "Cetirizine 10 mg Tablet",
            ingredients = "Cetirizine Hydrochloride 10 mg",
            drugClass = "Second-generation Antihistamine",
            uses = "Relief of allergic rhinitis symptoms including sneezing, runny nose, itchy watery eyes, and hives.",
            sideEffects = "Mild drowsiness, dry mouth, headache, or fatigue in some individuals.",
            warnings = "Use caution when operating machinery or driving until you know how it affects you.",
            precautions = "Best taken in the evening if drowsiness occurs.\nAvoid combining with alcohol or central nervous system sedatives.\nConsult a healthcare professional before use in kidney disease.",
            interactions = "Alcohol and CNS depressants may enhance drowsiness."
        ),
        MedicineRecord(
            name = "Omeprazole 20 mg",
            generic = "Omeprazole",
            brand = "Omeprazole 20 mg Capsule",
            ingredients = "Omeprazole 20 mg",
            drugClass = "Proton Pump Inhibitor (PPI)",
            uses = "Relief of frequent heartburn, acid indigestion, gastroesophageal reflux disease (GERD), and stomach ulcers.",
            sideEffects = "Headache, stomach pain, gas, nausea, or diarrhea.",
            warnings = "Intended for short-term symptom management unless under continuous physician supervision.",
            precautions = "Take in the morning on an empty stomach, at least 30-60 minutes before breakfast.\nSwallow capsule whole; do not crush or chew.\nConsult a doctor if chest pain radiates to arms or shoulders.",
            interactions = "May reduce absorption of iron, calcium, and vitamin B12 with prolonged use."
        ),
        MedicineRecord(
            name = "Metformin 500 mg",
            generic = "Metformin",
            brand = "Metformin 500 mg Tablet",
            ingredients = "Metformin Hydrochloride 500 mg",
            drugClass = "Biguanide Antidiabetic",
            uses = "Management of blood glucose levels in adults with type 2 diabetes along with diet and exercise.",
            sideEffects = "Temporary stomach upset, metallic taste in mouth, mild nausea, or diarrhea.",
            warnings = "Rare risk of lactic acidosis. Avoid excessive alcohol consumption while on therapy.",
            precautions = "Take with meals to reduce gastrointestinal side effects.\nHave kidney function tested regularly.\nStay properly hydrated, especially during illness or vigorous physical activity.",
            interactions = "Inform medical team before contrast dye procedures or surgery."
        ),
        MedicineRecord(
            name = "Aspirin 75 mg",
            generic = "Aspirin",
            brand = "Aspirin 75 mg Tablet",
            ingredients = "Aspirin (Acetylsalicylic Acid) 75 mg",
            drugClass = "Cardiovascular & Antiplatelet Agent",
            uses = "Low-dose antiplatelet therapy for prevention of blood clots, recurrent heart attack, and ischemic stroke under medical guidance.",
            sideEffects = "Mild stomach irritation, heartburn, or increased tendency to bruise.",
            warnings = "Do not administer to children or teenagers with viral fever due to risk of Reye's syndrome.",
            precautions = "Take with or right after food to minimize stomach irritation.\nConsult physician if you have a history of gastric ulcers or bleeding disorders.\nInform healthcare providers before dental or surgical procedures.",
            interactions = "Increases bleeding risk with anticoagulants (warfarin, heparin) and other NSAIDs."
        ),
        MedicineRecord(
            name = "Azithromycin 500 mg",
            generic = "Azithromycin",
            brand = "Azithromycin 500 mg Tablet",
            ingredients = "Azithromycin Dihydrate 500 mg",
            drugClass = "Macrolide Antibiotic",
            uses = "Treatment of bacterial respiratory tract infections, tonsillitis, sinusitis, ear infections, and skin infections.",
            sideEffects = "Mild nausea, loose stools, abdominal cramping, or headache.",
            warnings = "Complete the entire prescribed 3-to-5 day course even if symptoms rapidly improve.",
            precautions = "Take 1 hour before or 2 hours after meals with a glass of water.\nInform doctor if you have known cardiac arrhythmia or hepatic impairment.\nDo not take aluminum- or magnesium-containing antacids simultaneously.",
            interactions = "Antacids delay absorption. May interact with digoxin, warfarin, and ergotamine."
        ),
        MedicineRecord(
            name = "Pantoprazole 40 mg",
            generic = "Pantoprazole",
            brand = "Pantoprazole 40 mg Tablet",
            ingredients = "Pantoprazole Sodium 40 mg",
            drugClass = "Proton Pump Inhibitor (PPI)",
            uses = "Treatment of gastroesophageal reflux disease (GERD), erosive esophagitis, persistent heartburn, and stomach ulcers.",
            sideEffects = "Mild headache, flatulence, abdominal discomfort, or transient loose stools.",
            warnings = "Use for the prescribed duration. Consult a physician if persistent reflux does not improve after 2 weeks.",
            precautions = "Take once daily in the morning, 30 to 60 minutes before breakfast.\nSwallow whole with water; do not crush, split, or chew.\nEnsure adequate dietary calcium and vitamin D during prolonged therapy.",
            interactions = "May reduce absorption of iron, ketoconazole, and vitamin B12."
        ),
        MedicineRecord(
            name = "Atorvastatin 10 mg",
            generic = "Atorvastatin",
            brand = "Atorvastatin 10 mg Tablet",
            ingredients = "Atorvastatin Calcium 10 mg",
            drugClass = "HMG-CoA Reductase Inhibitor (Statin)",
            uses = "Lowers elevated LDL (bad) cholesterol and triglycerides, raises HDL (good) cholesterol, and reduces cardiovascular risk.",
            sideEffects = "Mild muscle ache, joint stiffness, constipation, or mild indigestion.",
            warnings = "Contact your doctor immediately if you experience unexplained muscle pain, tenderness, or weakness.",
            precautions = "Take at the same time each day, with or without food, preferably in the evening.\nAvoid large quantities of grapefruit juice while on treatment.\nUndergo routine periodic liver function tests as advised.",
            interactions = "Concomitant gemfibrozil, clarithromycin, or cyclosporine increases myopathy risk."
        ),
        MedicineRecord(
            name = "ORS (Oral Rehydration Salts)",
            generic = "Oral Rehydration Salts",
            brand = "WHO-Formula ORS Sachet",
            ingredients = "Sodium Chloride, Potassium Chloride, Sodium Citrate, Anhydrous Dextrose",
            drugClass = "Electrolyte & Fluid Replenishment",
            uses = "Restoration of vital electrolytes and body water lost through acute diarrhea, dehydration, vomiting, or excessive sweating.",
            sideEffects = "Extremely well tolerated. Rare mild nausea if consumed too quickly.",
            warnings = "Dissolve entire packet in the exact volume of clean drinking water indicated on the sachet (typically 1 liter). Do not boil after mixing.",
            precautions = "Use freshly prepared solution within 24 hours; discard unused portion.\nDo not add sugar, salt, or fruit juices to the standard prepared solution.\nSeek immediate medical help if the patient cannot drink or dehydration worsens.",
            interactions = "Safe with routine medications. Exercise caution in severe renal impairment."
        ),
        MedicineRecord(
            name = "Montelukast 10 mg",
            generic = "Montelukast",
            brand = "Montelukast 10 mg Tablet",
            ingredients = "Montelukast Sodium 10 mg",
            drugClass = "Leukotriene Receptor Antagonist",
            uses = "Maintenance treatment and prophylaxis of chronic asthma and symptomatic relief of seasonal or perennial allergic rhinitis.",
            sideEffects = "Headache, mild dizziness, nasal congestion, or abdominal pain.",
            warnings = "Not intended for treating sudden acute bronchospasm. Always keep a rapid rescue bronchodilator accessible.",
            precautions = "Take once daily in the evening, with or without food.\nContinue taking regularly even during symptom-free periods.\nInform doctor promptly of any unexplained changes in mood, dreams, or behavior.",
            interactions = "Phenobarbital and rifampicin may accelerate hepatic metabolism of montelukast."
        ),
        MedicineRecord(
            name = "Cough Syrup (Dextromethorphan)",
            generic = "Dextromethorphan",
            brand = "Dextromethorphan Cough Syrup",
            ingredients = "Dextromethorphan Hydrobromide 10 mg / 5 mL",
            drugClass = "Antitussive (Cough Suppressant)",
            uses = "Temporary suppression of dry, irritating, non-productive cough caused by minor throat and bronchial irritation from the common cold.",
            sideEffects = "Mild drowsiness, dizziness, lightheadedness, or mild nausea.",
            warnings = "Do not take concurrently with or within 14 days of stopping a Monoamine Oxidase Inhibitor (MAOI).",
            precautions = "Always measure dose using the calibrated measuring cup provided.\nDo not use for persistent or chronic cough caused by smoking, asthma, or emphysema.\nAvoid driving or operating machinery if drowsiness occurs.",
            interactions = "Contraindicated with MAO inhibitors. Sedatives and alcohol enhance drowsiness."
        ),
        MedicineRecord(
            name = "Vitamin D3 60,000 IU",
            generic = "Cholecalciferol",
            brand = "Vitamin D3 60,000 IU Capsule",
            ingredients = "Cholecalciferol (Vitamin D3) 60,000 IU",
            drugClass = "Nutritional & Bone Health Supplement",
            uses = "Treatment and prevention of vitamin D deficiency, osteomalacia, rickets, and adjunctive therapy for osteoporosis.",
            sideEffects = "Safe when taken at prescribed weekly or monthly intervals. Excessive intake may cause hypercalcemia.",
            warnings = "This is a high-potency dose intended for weekly or monthly administration as directed. Do not consume daily.",
            precautions = "Take with the largest meal of the day containing healthy fats to maximize absorption.\nPeriodically check serum 25-hydroxyvitamin D and calcium levels.\nConsult doctor if you have a history of kidney stones.",
            interactions = "Thiazide diuretics increase hypercalcemia risk. Corticosteroids may decrease vitamin D action."
        ),
        MedicineRecord(
            name = "Multivitamin & Minerals",
            generic = "Multivitamins with Minerals",
            brand = "Daily Complete Multivitamin Tablet",
            ingredients = "Vitamins A, B-Complex, C, D3, E, Zinc, Iron, Magnesium, Copper",
            drugClass = "Dietary Supplement & Daily Wellness",
            uses = "Nutritional support to prevent micronutrient deficiencies, promote immune resistance, and maintain general vitality.",
            sideEffects = "Occasional mild stomach upset or harmless bright yellow urine coloration due to riboflavin.",
            warnings = "Do not exceed recommended daily allowance. Dietary supplements should not replace a balanced varied diet.",
            precautions = "Take with or right after food with a full glass of water.\nKeep stored in a child-resistant container out of reach of children.\nInform physician before combining with other fortified nutritional formulas.",
            interactions = "Iron may reduce absorption of fluoroquinolone and tetracycline antibiotics."
        ),
        MedicineRecord(
            name = "Antacid Liquid (Magaldrate + Simethicone)",
            generic = "Magaldrate and Simethicone",
            brand = "Rapid Antacid Oral Suspension",
            ingredients = "Magaldrate 400 mg + Simethicone 20 mg / 5 mL",
            drugClass = "Antacid & Antiflatulent",
            uses = "Fast symptomatic relief of acute acidity, burning heartburn, acid indigestion, sour stomach, and uncomfortable gastric gas.",
            sideEffects = "Usually well tolerated; occasional mild alteration in bowel habits.",
            warnings = "Shake suspension thoroughly before use. Do not take maximum dosage for longer than 2 weeks continuously without consulting a physician.",
            precautions = "Take 1 to 2 hours after meals and at bedtime, or when symptoms strike.\nSeparate administration from other prescription medications by at least 2 hours.\nConsult doctor before use if you have kidney disease.",
            interactions = "Impairs oral absorption of tetracyclines, iron salts, fluoroquinolones, and digoxin."
        ),
        MedicineRecord(
            name = "Diclofenac Gel 1%",
            generic = "Diclofenac Diethylamine",
            brand = "Diclofenac Topical Pain Relief Gel 1%",
            ingredients = "Diclofenac Diethylamine 1.16% w/w",
            drugClass = "Topical Nonsteroidal Anti-inflammatory (NSAID)",
            uses = "Targeted local relief of inflammatory pain, acute muscular sprains, joint stiffness, tendonitis, and localized osteoarthritis.",
            sideEffects = "Mild local skin erythema, slight itching, or dryness at application area.",
            warnings = "For external dermatological application on unbroken skin only. Do not apply near eyes, mucous membranes, or open wounds.",
            precautions = "Gently massage into the affected area; wash hands immediately after application.\nDo not cover treated skin with airtight bandages or heating pads.\nProtect treated regions from intense direct sun exposure during therapy.",
            interactions = "Concurrent systemic oral NSAIDs may increase the risk of systemic NSAID adverse effects."
        ),
        MedicineRecord(
            name = "Ciprofloxacin 500 mg",
            generic = "Ciprofloxacin",
            brand = "Ciprofloxacin 500 mg Tablet",
            ingredients = "Ciprofloxacin Hydrochloride 500 mg",
            drugClass = "Fluoroquinolone Antibiotic",
            uses = "Treatment of complicated bacterial infections of the urinary tract, lower respiratory tract, gastrointestinal tract, and bone/joint infections.",
            sideEffects = "Mild nausea, diarrhea, dizziness, headache, or mild dyspepsia.",
            warnings = "Discontinue immediately and seek urgent medical consultation if tendon pain, swelling, or numbness develops.",
            precautions = "Maintain generous daily fluid intake throughout therapy to prevent crystalluria.\nDo not take simultaneously with antacids, iron, or dairy products (space by 2 hours).\nAvoid excessive exposure to direct sunlight or UV lamps.",
            interactions = "Antacids, sucralfate, iron, and multivalent cations significantly reduce ciprofloxacin absorption."
        ),
        MedicineRecord(
            name = "Losartan 50 mg",
            generic = "Losartan",
            brand = "Losartan Potassium 50 mg Tablet",
            ingredients = "Losartan Potassium 50 mg",
            drugClass = "Angiotensin II Receptor Blocker (ARB)",
            uses = "Management of systemic arterial hypertension (high blood pressure) and renal protection in hypertensive type 2 diabetic patients.",
            sideEffects = "Mild dizziness, lightheadedness, nasal stuffiness, or fatigue.",
            warnings = "Do not use during pregnancy as drugs acting on the renin-angiotensin system can cause fetal injury or death.",
            precautions = "Take at approximately the same time each day, with or without food.\nStand up gradually from seated or lying postures to avoid orthostatic dizziness.\nAvoid potassium supplements or salt substitutes containing potassium without physician guidance.",
            interactions = "NSAIDs may diminish antihypertensive efficacy and increase risk of impaired renal function."
        )
    )

    // ============================================================
    // COMMON MEDICINE ALIASES
    // ============================================================

    private val medicineAliases = mapOf(
        "paracetamol" to listOf(
            "paracetamol",
            "acetaminophen",
            "dolo",
            "crocin",
            "calpol"
        ),
        "acetaminophen" to listOf(
            "acetaminophen",
            "paracetamol",
            "dolo",
            "crocin",
            "calpol"
        ),
        "crocin" to listOf(
            "crocin",
            "paracetamol",
            "acetaminophen"
        ),
        "dolo" to listOf(
            "dolo",
            "dolo 650",
            "paracetamol",
            "acetaminophen"
        ),
        "dolo 650" to listOf(
            "dolo 650",
            "dolo",
            "paracetamol",
            "acetaminophen"
        ),
        "calpol" to listOf(
            "calpol",
            "calpol 650",
            "paracetamol",
            "acetaminophen"
        ),
        "ors" to listOf(
            "ors",
            "oral rehydration",
            "oral rehydration salts",
            "oral rehydration solution"
        ),
        "oral rehydration" to listOf(
            "oral rehydration",
            "oral rehydration salts",
            "oral rehydration solution",
            "ors"
        ),
        "pain killer" to listOf(
            "pain killer",
            "painkiller",
            "analgesic",
            "pain relief"
        ),
        "painkiller" to listOf(
            "painkiller",
            "pain killer",
            "analgesic",
            "pain relief"
        )
    )

    // ============================================================
    // MALFORMED & NON-MEDICINAL RECORD FILTER
    // ============================================================

    internal fun isMalformedOrNonMedicinal(
        name: String,
        generic: String = "",
        brand: String = "",
        uses: String = ""
    ): Boolean {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return true
        if (cleanName.all { it.isDigit() }) return true
        val combined = "$cleanName $generic $brand $uses".lowercase()
        if (combined.contains("laboratory use only") || combined.contains("for laboratory use")) return true
        if (combined.contains("in vitro diagnostic") && !combined.contains("drug")) return true
        val sanitizerKeywords = listOf(
            "hand sanitizer", "hand sanitize", "hand rub", "hand disinfectant",
            "antiseptic hand rub", "instant hand sanitizer", "hand sanitizer gel",
            "antibacterial hand soap", "sanitizing hand wipe", "soap"
        )
        val nameLower = cleanName.lowercase()
        val genLower = generic.lowercase()
        return sanitizerKeywords.any { nameLower.contains(it) || genLower.contains(it) }
    }

    // ============================================================
    // NORMALIZATION
    // ============================================================

    internal fun normalizeSearchText(
        value: String
    ): String {
        return value
            .trim()
            .lowercase()
            .replace(
                Regex("[^a-z0-9\\s]"),
                " "
            )
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
    }

    // ============================================================
    // STREAMING JSON PARSER
    // ============================================================

    fun parseMedicineStream(
        inputStream: InputStream,
        limit: Int = 0
    ): List<MedicineRecord> {
        val medicines = ArrayList<MedicineRecord>(if (limit in 1..100) limit else 128)
        val reader = JsonReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        reader.use { jsonReader ->
            if (jsonReader.peek() != JsonToken.BEGIN_ARRAY) {
                return emptyList()
            }
            jsonReader.beginArray()

            while (jsonReader.hasNext()) {
                if (jsonReader.peek() != JsonToken.BEGIN_OBJECT) {
                    jsonReader.skipValue()
                    continue
                }
                jsonReader.beginObject()

                var name = ""
                var generic = ""
                var brand = ""
                var ingredients = ""
                var drugClass = ""
                var uses = ""
                var sideEffects = ""
                var warnings = ""
                var precautions = ""
                var interactions = ""

                while (jsonReader.hasNext()) {
                    val key = jsonReader.nextName()
                    if (jsonReader.peek() == JsonToken.NULL) {
                        jsonReader.nextNull()
                        continue
                    }
                    when (key) {
                        "n" -> name = jsonReader.nextStringOrEmpty()
                        "g" -> generic = jsonReader.nextStringOrEmpty()
                        "b" -> brand = jsonReader.nextStringOrEmpty()
                        "i" -> ingredients = readIngredients(jsonReader)
                        "c" -> drugClass = jsonReader.nextStringOrEmpty()
                        "u" -> uses = jsonReader.nextStringOrEmpty()
                        "s" -> sideEffects = jsonReader.nextStringOrEmpty()
                        "w" -> warnings = jsonReader.nextStringOrEmpty()
                        "p" -> precautions = jsonReader.nextStringOrEmpty()
                        "x" -> interactions = jsonReader.nextStringOrEmpty()
                        else -> jsonReader.skipValue()
                    }
                }
                jsonReader.endObject()

                medicines.add(
                    MedicineRecord(
                        name = name,
                        generic = generic,
                        brand = brand,
                        ingredients = ingredients,
                        drugClass = drugClass,
                        uses = uses,
                        sideEffects = sideEffects,
                        warnings = warnings,
                        precautions = precautions,
                        interactions = interactions
                    )
                )

                if (limit in 1..medicines.size) {
                    break
                }
            }
        }
        return medicines
    }

    private fun JsonReader.nextStringOrEmpty(): String {
        return if (peek() == JsonToken.STRING) {
            nextString()
        } else {
            skipValue()
            ""
        }
    }

    private fun readIngredients(jsonReader: JsonReader): String {
        return when (jsonReader.peek()) {
            JsonToken.BEGIN_ARRAY -> {
                jsonReader.beginArray()
                val list = mutableListOf<String>()
                while (jsonReader.hasNext()) {
                    if (jsonReader.peek() == JsonToken.STRING) {
                        list.add(jsonReader.nextString())
                    } else {
                        jsonReader.skipValue()
                    }
                }
                jsonReader.endArray()
                list.joinToString(", ")
            }
            JsonToken.STRING -> jsonReader.nextString()
            else -> {
                jsonReader.skipValue()
                ""
            }
        }
    }

fun parseMedicineStreamFiltered(
        inputStream: InputStream,
        limit: Int,
        excludedKeys: Set<String> = emptySet()
    ): List<MedicineRecord> {
        val medicines = ArrayList<MedicineRecord>(if (limit in 1..250) limit else 64)
        val seenLocal = HashSet<String>(excludedKeys)
        val reader = JsonReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        reader.use { jsonReader ->
            if (jsonReader.peek() != JsonToken.BEGIN_ARRAY) {
                return emptyList()
            }
            jsonReader.beginArray()

            while (jsonReader.hasNext()) {
                if (jsonReader.peek() != JsonToken.BEGIN_OBJECT) {
                    jsonReader.skipValue()
                    continue
                }
                jsonReader.beginObject()

                var name = ""
                var generic = ""
                var brand = ""
                var ingredients = ""
                var drugClass = ""
                var uses = ""
                var sideEffects = ""
                var warnings = ""
                var precautions = ""
                var interactions = ""

                while (jsonReader.hasNext()) {
                    val key = jsonReader.nextName()
                    if (jsonReader.peek() == JsonToken.NULL) {
                        jsonReader.nextNull()
                        continue
                    }
                    when (key) {
                        "n" -> name = jsonReader.nextStringOrEmpty()
                        "g" -> generic = jsonReader.nextStringOrEmpty()
                        "b" -> brand = jsonReader.nextStringOrEmpty()
                        "i" -> ingredients = readIngredients(jsonReader)
                        "c" -> drugClass = jsonReader.nextStringOrEmpty()
                        "u" -> uses = jsonReader.nextStringOrEmpty()
                        "s" -> sideEffects = jsonReader.nextStringOrEmpty()
                        "w" -> warnings = jsonReader.nextStringOrEmpty()
                        "p" -> precautions = jsonReader.nextStringOrEmpty()
                        "x" -> interactions = jsonReader.nextStringOrEmpty()
                        else -> jsonReader.skipValue()
                    }
                }
                jsonReader.endObject()

                val cleanName = name.trim()
                if (cleanName.isBlank()) continue
                if (isMalformedOrNonMedicinal(cleanName, generic, brand, uses)) continue
                val normKey = normalizeSearchText(cleanName)
                if (normKey.isNotBlank() && seenLocal.add(normKey)) {
                    medicines.add(
                        MedicineRecord(
                            name = cleanName,
                            generic = generic.trim(),
                            brand = brand.trim(),
                            ingredients = ingredients.trim(),
                            drugClass = drugClass.trim(),
                            uses = uses.trim(),
                            sideEffects = sideEffects.trim(),
                            warnings = warnings.trim(),
                            precautions = precautions.trim(),
                            interactions = interactions.trim()
                        )
                    )
                    if (limit in 1..medicines.size) {
                        break
                    }
                }
            }
        }
        return medicines
    }

    // ============================================================
    // LOAD MEDICINES
    // ============================================================

    fun getMedicines(
        inputStream: InputStream,
        limit: Int = 0
    ): List<MedicineRecord> {
        val effectiveLimit = if (limit <= 0) 50 else limit
        cachedMedicines?.let {
            return it.take(effectiveLimit)
        }
        cachedInitialMedicines?.let {
            if (it.size >= effectiveLimit) {
                return it.take(effectiveLimit)
            }
        }
        val loaded = parseMedicineStream(inputStream, limit = effectiveLimit)
        if (cachedInitialMedicines == null && effectiveLimit >= 20) {
            cachedInitialMedicines = loaded
        }
        return loaded
    }

    fun getMedicines(
        context: Context,
        limit: Int = 0
    ): List<MedicineRecord> {
        val effectiveLimit = if (limit <= 0) 50 else limit
        cachedMedicines?.let {
            return it.take(effectiveLimit)
        }
        if (effectiveLimit <= verifiedMedicines.size) {
            return verifiedMedicines.take(effectiveLimit)
        }
        cachedInitialMedicines?.let {
            if (it.size >= effectiveLimit) {
                return it.take(effectiveLimit)
            }
        }
        val needed = effectiveLimit - verifiedMedicines.size
        val seenKeys = verifiedMedicines.map { normalizeSearchText(it.name) }.toHashSet()
        val extra = context.assets.open("medicine_data.json").use { stream ->
            parseMedicineStreamFiltered(stream, needed, seenKeys)
        }
        val combined = (verifiedMedicines + extra).take(effectiveLimit)
        if (cachedInitialMedicines == null && combined.size >= 20) {
            cachedInitialMedicines = combined
        }
        return combined
    }

    // ============================================================
    // SEARCH TERMS
    // ============================================================

    internal fun getSearchTerms(
        query: String
    ): List<String> {
        val normalizedQuery = normalizeSearchText(query)
        val aliases = medicineAliases[normalizedQuery] ?: emptyList()

        return (listOf(normalizedQuery) + aliases)
            .map { normalizeSearchText(it) }
            .filter { it.isNotBlank() }
            .distinct()
    }

    // ============================================================
    // SCORE
    // ============================================================

    internal fun calculateScore(
        name: String,
        generic: String,
        brand: String,
        ingredients: String,
        searchTerm: String
    ): Int {
        if (name == searchTerm) return 1000
        if (generic == searchTerm) return 950
        if (brand == searchTerm) return 900
        if (name.startsWith(searchTerm)) return 850
        if (generic.startsWith(searchTerm)) return 840
        if (brand.startsWith(searchTerm)) return 830
        if (name.contains(searchTerm)) return 750
        if (generic.contains(searchTerm)) return 740
        if (brand.contains(searchTerm)) return 730
        if (ingredients.contains(searchTerm)) return 650
        return 0
    }

    // ============================================================
    // SEARCH MEDICINES (STREAMING)
    // ============================================================

    fun searchMedicinesStream(
        inputStream: InputStream,
        query: String,
        limit: Int = 20
    ): List<MedicineRecord> {
        val normalizedQuery = normalizeSearchText(query)
        if (normalizedQuery.isBlank()) {
            return getMedicines(inputStream, limit = limit)
        }

        val canonicalAliases = setOf(
            "paracetamol",
            "acetaminophen",
            "crocin",
            "dolo",
            "dolo 650"
        )
        val isCanonicalAlias = normalizedQuery in canonicalAliases

        val searchTerms = if (isCanonicalAlias) {
            listOf("acetaminophen", "paracetamol")
        } else {
            getSearchTerms(normalizedQuery)
        }

        val seenKeys = HashSet<String>()
        val scoredResults = ArrayList<Pair<MedicineRecord, Int>>()
        val acetaminophenCandidates = ArrayList<MedicineRecord>()
        var recordsScanned = 0

        val reader = JsonReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        reader.use { jsonReader ->
            if (jsonReader.peek() != JsonToken.BEGIN_ARRAY) {
                return emptyList()
            }
            jsonReader.beginArray()

            while (jsonReader.hasNext()) {
                recordsScanned++
                if ((recordsScanned and 63) == 0 && Thread.currentThread().isInterrupted) {
                    return emptyList()
                }

                if (jsonReader.peek() != JsonToken.BEGIN_OBJECT) {
                    jsonReader.skipValue()
                    continue
                }
                jsonReader.beginObject()

                var name = ""
                var generic = ""
                var brand = ""
                var ingredients = ""
                var drugClass = ""
                var uses = ""
                var sideEffects = ""
                var warnings = ""
                var precautions = ""
                var interactions = ""
                var evaluatedScore: Int? = null

                while (jsonReader.hasNext()) {
                    val key = jsonReader.nextName()
                    if (jsonReader.peek() == JsonToken.NULL) {
                        jsonReader.nextNull()
                        continue
                    }

                    // Fast-skip: If we already evaluated search fields and found no match,
                    // skip reading remaining long clinical descriptions without heap allocation
                    if (evaluatedScore == 0 && (key == "u" || key == "s" || key == "w" || key == "p" || key == "x")) {
                        jsonReader.skipValue()
                        continue
                    }

                    when (key) {
                        "n" -> name = jsonReader.nextStringOrEmpty()
                        "g" -> generic = jsonReader.nextStringOrEmpty()
                        "b" -> brand = jsonReader.nextStringOrEmpty()
                        "i" -> ingredients = readIngredients(jsonReader)
                        "c" -> {
                            drugClass = jsonReader.nextStringOrEmpty()
                            val normName = normalizeSearchText(name)
                            val normGeneric = normalizeSearchText(generic)
                            val normBrand = normalizeSearchText(brand)
                            val normIng = normalizeSearchText(ingredients)

                            if (isCanonicalAlias) {
                                evaluatedScore = if (
                                    normGeneric == "acetaminophen" ||
                                    normGeneric.contains("acetaminophen") ||
                                    normIng.contains("acetaminophen")
                                ) 900 else 0
                            } else {
                                var best = 0
                                for (term in searchTerms) {
                                    val s = calculateScore(normName, normGeneric, normBrand, normIng, term)
                                    if (s > best) best = s
                                }
                                evaluatedScore = best
                            }
                        }
                        "u" -> uses = jsonReader.nextStringOrEmpty()
                        "s" -> sideEffects = jsonReader.nextStringOrEmpty()
                        "w" -> warnings = jsonReader.nextStringOrEmpty()
                        "p" -> precautions = jsonReader.nextStringOrEmpty()
                        "x" -> interactions = jsonReader.nextStringOrEmpty()
                        else -> jsonReader.skipValue()
                    }
                }
                jsonReader.endObject()

                val normName = normalizeSearchText(name)
                val normGeneric = normalizeSearchText(generic)
                val normBrand = normalizeSearchText(brand)
                val normIng = normalizeSearchText(ingredients)

                if (isMalformedOrNonMedicinal(name, generic, brand, uses)) {
                    continue
                }

                val uniqueKey = when {
                    normName.isNotBlank() -> normName
                    normGeneric.isNotBlank() -> normGeneric
                    normBrand.isNotBlank() -> normBrand
                    else -> normIng
                }
                if (uniqueKey.isBlank() || !seenKeys.add(uniqueKey)) {
                    continue
                }

                val score = evaluatedScore ?: run {
                    if (isCanonicalAlias) {
                        if (normGeneric == "acetaminophen" || normGeneric.contains("acetaminophen") || normIng.contains("acetaminophen")) 900 else 0
                    } else {
                        var best = 0
                        for (term in searchTerms) {
                            val s = calculateScore(normName, normGeneric, normBrand, normIng, term)
                            if (s > best) best = s
                        }
                        best
                    }
                }

                if (score <= 0) continue

                val record = MedicineRecord(
                    name = name,
                    generic = generic,
                    brand = brand,
                    ingredients = ingredients,
                    drugClass = drugClass,
                    uses = uses,
                    sideEffects = sideEffects,
                    warnings = warnings,
                    precautions = precautions,
                    interactions = interactions
                )

                if (isCanonicalAlias) {
                    acetaminophenCandidates.add(record)
                    if (acetaminophenCandidates.size >= limit * 2) {
                        break
                    }
                } else {
                    scoredResults.add(Pair(record, score))
                    if (scoredResults.size >= limit * 5) {
                        break
                    }
                }
            }
        }

        if (isCanonicalAlias) {
            return acetaminophenCandidates
                .sortedWith(
                    compareBy<MedicineRecord> {
                        val g = normalizeSearchText(it.generic)
                        when {
                            g == "acetaminophen" -> 0
                            g.contains("acetaminophen") -> 1
                            else -> 2
                        }
                    }.thenBy {
                        it.name.length
                    }
                )
                .take(limit)
        }

        return scoredResults
            .sortedWith(
                compareByDescending<Pair<MedicineRecord, Int>> { it.second }
                    .thenBy { it.first.name.lowercase() }
            )
            .take(limit)
            .map { it.first }
    }

    // ============================================================
    // SEARCH FROM IN-MEMORY LIST (IF ALREADY LOADED)
    // ============================================================

    private fun searchMedicinesFromList(
        medicines: List<MedicineRecord>,
        normalizedQuery: String,
        limit: Int = 20
    ): List<MedicineRecord> {
        val canonicalAliases = setOf(
            "paracetamol",
            "acetaminophen",
            "crocin",
            "dolo",
            "dolo 650"
        )
        val isCanonicalAlias = normalizedQuery in canonicalAliases

        val seenKeys = HashSet<String>()
        val uniqueMedicines = ArrayList<MedicineRecord>()
        for (m in medicines) {
            val normName = normalizeSearchText(m.name)
            val normGeneric = normalizeSearchText(m.generic)
            val normBrand = normalizeSearchText(m.brand)
            val normIng = normalizeSearchText(m.ingredients)
            val uniqueKey = when {
                normName.isNotBlank() -> normName
                normGeneric.isNotBlank() -> normGeneric
                normBrand.isNotBlank() -> normBrand
                else -> normIng
            }
            if (uniqueKey.isNotBlank() && seenKeys.add(uniqueKey)) {
                uniqueMedicines.add(m)
            }
        }

        if (isCanonicalAlias) {
            return uniqueMedicines
                .filter { m ->
                    val g = normalizeSearchText(m.generic)
                    val ing = normalizeSearchText(m.ingredients)
                    g == "acetaminophen" || g.contains("acetaminophen") || ing.contains("acetaminophen")
                }
                .sortedWith(
                    compareBy<MedicineRecord> {
                        val g = normalizeSearchText(it.generic)
                        when {
                            g == "acetaminophen" -> 0
                            g.contains("acetaminophen") -> 1
                            else -> 2
                        }
                    }.thenBy {
                        it.name.length
                    }
                )
                .take(limit)
        }

        val searchTerms = getSearchTerms(normalizedQuery)
        val scoredResults = ArrayList<Pair<MedicineRecord, Int>>()

        for (m in uniqueMedicines) {
            val normName = normalizeSearchText(m.name)
            val normGeneric = normalizeSearchText(m.generic)
            val normBrand = normalizeSearchText(m.brand)
            val normIng = normalizeSearchText(m.ingredients)

            var bestScore = 0
            for (term in searchTerms) {
                val score = calculateScore(
                    name = normName,
                    generic = normGeneric,
                    brand = normBrand,
                    ingredients = normIng,
                    searchTerm = term
                )
                if (score > bestScore) {
                    bestScore = score
                }
            }
            if (bestScore > 0) {
                scoredResults.add(Pair(m, bestScore))
            }
        }

        return scoredResults
            .sortedWith(
                compareByDescending<Pair<MedicineRecord, Int>> { it.second }
                    .thenBy { it.first.name.lowercase() }
            )
            .take(limit)
            .map { it.first }
    }

    // ============================================================
    // SEARCH MEDICINES (PUBLIC DISPATCHER)
    // ============================================================

    fun searchMedicines(
        inputStream: InputStream,
        query: String,
        limit: Int = 20
    ): List<MedicineRecord> {
        return searchMedicinesStream(inputStream, query, limit)
    }

    fun searchMedicines(
        context: Context,
        query: String,
        limit: Int = 20
    ): List<MedicineRecord> {
        val normalizedQuery = normalizeSearchText(query)
        if (normalizedQuery.isBlank()) {
            return getMedicines(context, limit = limit)
        }

        synchronized(searchCache) {
            searchCache[normalizedQuery]?.let {
                return it.take(limit)
            }
        }

        // Search verified medicines first for clean, human-readable results (Dolo, Crocin, Calpol, Paracetamol, etc.)
        val verifiedMatches = searchVerifiedMedicines(normalizedQuery, limit)
        val needed = limit - verifiedMatches.size

        val results = if (needed <= 0) {
            verifiedMatches.take(limit)
        } else {
            val verifiedKeys = verifiedMatches.map { normalizeSearchText(it.name) }.toHashSet()
            val inMemory = cachedMedicines
            val datasetMatches = if (inMemory != null) {
                searchMedicinesFromList(inMemory, normalizedQuery, limit = needed * 2)
            } else {
                context.assets.open("medicine_data.json").use { stream ->
                    searchMedicinesStream(stream, normalizedQuery, limit = needed * 2)
                }
            }.filter { verifiedKeys.add(normalizeSearchText(it.name)) }

            (verifiedMatches + datasetMatches).take(limit)
        }

        synchronized(searchCache) {
            searchCache[normalizedQuery] = results
        }

        return results
    }

    // ============================================================
    // TARGETED MEDICINE LOOKUP (FAST & MEMORY-SAFE)
    // ============================================================

    fun findMedicine(
        context: Context,
        nameOrQuery: String
    ): MedicineRecord? {
        val norm = normalizeSearchText(nameOrQuery)
        if (norm.isBlank()) return null

        // 1. Check verified medicines first (instant, zero memory allocation)
        verifiedMedicines.firstOrNull {
            val n = normalizeSearchText(it.name)
            val g = normalizeSearchText(it.generic)
            val b = normalizeSearchText(it.brand)
            n == norm || g == norm || b == norm || n.contains(norm) || norm.contains(n)
        }?.let { return it }

        // 2. Targeted search with limit = 1
        val results = searchMedicines(context, norm, limit = 1)
        return results.firstOrNull()
    }

    // ============================================================
    // SEARCH VERIFIED MEDICINES
    // ============================================================

    private fun searchVerifiedMedicines(
        normalizedQuery: String,
        limit: Int = 20
    ): List<MedicineRecord> {
        val terms = getSearchTerms(normalizedQuery)
        val scored = ArrayList<Pair<MedicineRecord, Int>>()

        for (m in verifiedMedicines) {
            val n = normalizeSearchText(m.name)
            val g = normalizeSearchText(m.generic)
            val b = normalizeSearchText(m.brand)
            val i = normalizeSearchText(m.ingredients)

            var best = 0
            for (term in terms) {
                val s = calculateScore(n, g, b, i, term)
                if (s > best) best = s
            }
            if (best > 0) {
                scored.add(Pair(m, best))
            }
        }

        return scored
            .sortedWith(
                compareByDescending<Pair<MedicineRecord, Int>> { it.second }
                    .thenBy { it.first.name.lowercase() }
            )
            .take(limit)
            .map { it.first }
    }

    // ============================================================
    // RELATED MEDICINES (SAME ACTIVE INGREDIENT)
    // ============================================================

    fun getRelatedMedicines(
        query: String,
        primaryResults: List<MedicineRecord>
    ): List<MedicineRecord> {
        val norm = normalizeSearchText(query)
        val primaryNames = primaryResults.map { it.name.lowercase() }.toSet()

        // For paracetamol/acetaminophen queries, PCM 650 and P-650 are related formulations
        if (norm == "paracetamol" || norm == "acetaminophen" || norm == "dolo" || norm == "crocin" || norm == "calpol") {
            return verifiedMedicines.filter {
                (it.name == "PCM 650" || it.name == "P-650") && !primaryNames.contains(it.name.lowercase())
            }
        }

        val activeIngredients = primaryResults
            .map { it.generic.lowercase() }
            .filter { it.isNotBlank() }
            .distinct()

        if (activeIngredients.isNotEmpty()) {
            return verifiedMedicines.filter { vm ->
                !primaryNames.contains(vm.name.lowercase()) &&
                activeIngredients.any { ing ->
                    vm.generic.lowercase().contains(ing) || vm.ingredients.lowercase().contains(ing)
                }
            }.take(3)
        }

        return emptyList()
    }

    // ============================================================
    // CLEAR CACHE
    // ============================================================

    fun clearCache() {
        cachedMedicines = null
        cachedInitialMedicines = null
        synchronized(searchCache) {
            searchCache.clear()
        }
    }
}
