"""
Prepare Training Data for MedAssist AI (Alpaca + ChatML JSONL format)
====================================================================
Merges:
1. ml/data/layman_terms_mapping.json (scientific -> layman terms, emergency triage routing)
2. ml/data/raw/medical_knowledge.csv and sub-CSVs (symptoms, diseases, medicines, general health)
3. Open-source patient-doctor dialogue samples from MEDIQ (filtered for clinical relevance)
4. Comprehensive multilingual (Marathi, English, Hindi) clinical consultations, medication guidance,
   pediatric care, chronic conditions, and emergency triage.

Target Marathi Representation: 800 - 1,200 samples (~20% - 25% of overall dataset)

Outputs:
- ml/data/finetune/train.jsonl (90%)
- ml/data/finetune/val.jsonl (10%)
"""

import csv
import json
import os
import random
import re
from pathlib import Path

# Paths
BASE_DIR = Path(__file__).resolve().parent
DATA_DIR = BASE_DIR / "data"
RAW_DIR = DATA_DIR / "raw"
FINETUNE_DIR = DATA_DIR / "finetune"
LAYMAN_MAPPING_PATH = DATA_DIR / "layman_terms_mapping.json"
MED_KNOWLEDGE_CSV = RAW_DIR / "medical_knowledge.csv"
MED_KNOWLEDGE_DIR = RAW_DIR / "medical_knowledge"
MEDIQ_CSV = RAW_DIR / "mediq" / "ProxyAyush-MEDIQ-00091b1" / "data" / "mediq_full.csv"

TRAIN_OUTPUT = FINETUNE_DIR / "train.jsonl"
VAL_OUTPUT = FINETUNE_DIR / "val.jsonl"

SYSTEM_PROMPT = (
    "You are MedAssist AI, a compassionate, clinically safe medical assistant. "
    "You explain complex medical concepts in simple layman terms across English, Hindi, and Marathi, "
    "prioritize patient safety, flag medical emergencies immediately, and never provide unauthorized prescriptions. "
    "Always advise consulting a qualified doctor for definitive diagnosis and treatment."
)

MEDICAL_DISCLAIMER_EN = "\n\nDisclaimer: I am an AI medical assistant, not a doctor. Please consult a qualified healthcare professional for formal diagnosis and personalized treatment."
MEDICAL_DISCLAIMER_MR = "\n\nअस्वीकरण: मी एक एआय वैद्यकीय सहाय्यक आहे, डॉक्टर नाही. योग्य तपासणी आणि उपचारांसाठी कृपया तज्ज्ञ डॉक्टरांचा सल्ला घ्या."
MEDICAL_DISCLAIMER_HI = "\n\nअस्वीकरण: मैं एक एआई मेडिकल असिस्टेंट हूँ, डॉक्टर नहीं। सटीक निदान और उपचार के लिए कृपया योग्य चिकित्सक से परामर्श लें।"

SYMPTOM_LOCALIZATION = {
    "fever": {
        "mr_term": "ताप",
        "mr_desc": "तापामध्ये शरीराचे तापमान वाढते, थंडी वाजणे, डोकेदुखी, अशक्तपणा आणि अंगात कळ येणे अशी लक्षणे दिसतात.",
        "mr_care": "पुरेशी विश्रांती घ्या, भरपूर कोमट पाणी किंवा द्रवपदार्थ प्या, आणि ताप जास्त असल्यास कपाळावर पाण्याच्या घड्या ठेवा. ३ दिवसांपेक्षा जास्त ताप राहिल्यास डॉक्टरांचा सल्ला घ्या.",
        "hi_term": "बुखार",
        "hi_desc": "बुखार में शरीर का तापमान बढ़ना, कंपकंपी, सिरदर्द, बदन दर्द और कमजोरी जैसे लक्षण होते हैं।",
        "hi_care": "आराम करें, खूब पानी या तरल पदार्थ पिएं, और तापमान अधिक होने पर माथे पर ठंडे पानी की पट्टी रखें। यदि बुखार लगातार बना रहे तो डॉक्टर से संपर्क करें।"
    },
    "headache": {
        "mr_term": "डोकेदुखी",
        "mr_desc": "डोकेदुखी ही ताण, अपुरी झोप, पाण्याची कमतरता, मायग्रेन किंवा सर्दीमुळे होऊ शकते.",
        "mr_care": "शांत आणि अंधाऱ्या खोलीत विश्रांती घ्या, भरपूर पाणी प्या आणि डोळ्यांवर ताण देणे टाळा. तीव्र किंवा अचानक असह्य डोकेदुखी असल्यास ताबडतोब वैद्यकीय मदत घ्या.",
        "hi_term": "सिरदर्द",
        "hi_desc": "सिरदर्द तनाव, नींद की कमी, डिहाइड्रेशन, माइग्रेन या सर्दी के कारण हो सकता है।",
        "hi_care": "शांत कमरे में आराम करें, पानी पिएं और स्क्रीन का कम उपयोग करें। अचानक बहुत तेज सिरदर्द होने पर डॉक्टर को दिखाएं।"
    },
    "cough": {
        "mr_term": "खोकला",
        "mr_desc": "श्वसनमार्गात संसर्ग, धूळ, ॲलर्जी किंवा कफ साचल्यामुळे खोकला येतो.",
        "mr_care": "कोमट पाण्याच्या गुळण्या करा, कोमट पाण्यात मध व आल्याचा रस मिसळून घ्या आणि वाफ घ्या. खोकल्यासोबत श्वास घेण्यास त्रास किंवा रक्त येत असल्यास त्वरित डॉक्टरकडे जा.",
        "hi_term": "खांसी",
        "hi_desc": "श्वसन नली में संक्रमण, एलर्जी, धूल या कफ के कारण खांसी हो सकती है।",
        "hi_care": "गुनगुने पानी से गरारे करें, शहद-अदरक का सेवन करें और भाप लें। यदि सांस फूलने लगे तो तुरंत डॉक्टर को दिखाएं।"
    },
    "cold": {
        "mr_term": "सर्दी / पडसे",
        "mr_desc": "नाकातून पाणी गळणे, नाक बंद होणे, शिंका येणे, घसा दुखणे आणि डोके जड होणे ही सर्दीची लक्षणे आहेत.",
        "mr_care": "वाफ घ्या, गरम पाणी प्या आणि थंड पदार्थांचे सेवन टाळा. विश्रांती घेतल्यास २-३ दिवसांत आराम मिळतो.",
        "hi_term": "जुकाम / सर्दी",
        "hi_desc": "नाक बहना, छींकें आना, गला बैठना और सिर भारी होना जुकाम के सामान्य लक्षण हैं।",
        "hi_care": "भाप लें, गुनगुना पानी पिएं और आराम करें। ठंडी चीजों से परहेज करें।"
    },
    "body_pain": {
        "mr_term": "अंगदुखी / अंग कळा",
        "mr_desc": "अतिश्रम, अपुरी झोप, तापाचा संसर्ग किंवा डिहायड्रेशनमुळे संपूर्ण शरीर दुखू शकते.",
        "mr_care": "शरीराला पूर्ण विश्रांती द्या, कोमट पाण्याने आंघोळ करा आणि हलका पौष्टिक आहार घ्या.",
        "hi_term": "बदन दर्द",
        "hi_desc": "थकान, संक्रमण, नींद की कमी या शारीरिक श्रम से बदन में दर्द हो सकता है।",
        "hi_care": "पर्याप्त आराम करें, गर्म पानी से स्नान करें और पौष्टिक आहार लें।"
    },
    "sore_throat": {
        "mr_term": "घसा खवखवणे",
        "mr_desc": "घशात जळजळ, गिळताना त्रास, आवाज बसणे आणि कोरडेपणा जाणवतो.",
        "mr_care": "कोमट पाण्यात थोडे मीठ टाकून दिवसातून ३-४ वेळा गुळण्या करा आणि कोमट पाणी प्या.",
        "hi_term": "गले में खराश",
        "hi_desc": "गले में जलन, निगलने में दर्द और भारी आवाज इसके मुख्य लक्षण हैं।",
        "hi_care": "गुनगुने नमक के पानी से गरारे करें और गर्म तरल पदार्थों का सेवन करें।"
    },
    "stomach_pain": {
        "mr_term": "पोटदुखी",
        "mr_desc": "अपचन, गॅस, ॲसिडिटी, किंवा संसर्गामुळे पोटात पेटके किंवा वेदना होतात.",
        "mr_care": "हलका आहार घ्या (उदा. खिचडी), ताक प्या, आणि मसालेदार अन्न टाळा. तीव्र किंवा असह्य वेदना असल्यास त्वरित डॉक्टरकडे जा.",
        "hi_term": "पेट दर्द",
        "hi_desc": "अपच, गैस, एसिडिटी या इंफेक्शन के कारण पेट में दर्द या मरोड़ हो सकती है।",
        "hi_care": "हल्का भोजन लें, अधिक पानी पिएं और तीखे-मसालेदार खाने से बचें।"
    },
    "vomiting": {
        "mr_term": "उलट्या",
        "mr_desc": "पोटाचा संसर्ग, अन्नातून विषबाधा, किंवा अपचनामुळे मळमळ व उलट्या होतात.",
        "mr_care": "एकदम पाणी पिण्याऐवजी थोडे थोडे ओआरएस (ORS) किंवा लिंबू पाणी प्या जेणेकरून डिहायड्रेशन होणार नाही.",
        "hi_term": "उल्टी",
        "hi_desc": "फूड पॉइजनिंग, पेट के संक्रमण या अपच से उल्टी की समस्या हो सकती है।",
        "hi_care": "घूंट-घूंट कर ओआरएस या नींबू पानी पिएं ताकि शरीर में पानी की कमी न हो।"
    },
    "diarrhea": {
        "mr_term": "जुलाब / अतिसार",
        "mr_desc": "वारंवार पातळ शौचास होणे आणि शरीरातील पाण्याचे प्रमाण कमी होणे.",
        "mr_care": "ORS द्रावण, तांदळाची पेज, ताक प्या. शरीरात पाण्याची कमतरता भासू देऊ नका. जुलाबातून रक्त आल्यास ताबडतोब रुग्णालयात जा.",
        "hi_term": "दस्त / डायरिया",
        "hi_desc": "बार-बार पतला मल आना और शरीर में पानी और इलेक्ट्रोलाइट्स की कमी होना।",
        "hi_care": "ओआरएस का घोल, छाछ और नारियल पानी पिएं। कमजोरी अधिक होने पर डॉक्टर से संपर्क करें।"
    },
    "dizziness": {
        "mr_term": "चक्कर येणे",
        "mr_desc": "रक्तदाब कमी होणे, डिहायड्रेशन, हिमोग्लोबिन कमी असणे किंवा अशक्तपणामुळे डोळ्यांपुढे अंधारी येते.",
        "mr_care": "चक्कर आल्यास लगेच शांत बसा किंवा झोपा. पाय थोडे उंचावर ठेवा आणि गोड पाणी किंवा ओआरएस प्या. गाडी चालवू नका.",
        "hi_term": "चक्कर आना",
        "hi_desc": "कमजोरी, लो बीपी, डिहाइड्रेशन या खून की कमी के कारण सिर चकरा सकता है।",
        "hi_care": "तुरंत बैठ जाएं या लेट जाएं। ग्लूकोज या ओआरएस का पानी पिएं। अचानक खड़े होने से बचें।"
    },
    "acidity": {
        "mr_term": "ॲसिडिटी / पित्त",
        "mr_desc": "छातीत जळजळ, आंबट ढेकर येणे, मळमळणे आणि पोट फुगणे.",
        "mr_care": "थंड दूध प्या, वेळेवर जेवा, आणि उपाशी राहू नका. चहा, कॉफी आणि तळलेले पदार्थ कमी करा.",
        "hi_term": "एसिडिटी / गैस",
        "hi_desc": "सीने में जलन, खट्टी डकारें और पेट भारी लगना।",
        "hi_care": "ठंडा दूध पिएं, समय पर हल्का भोजन करें और ज्यादा मिर्च-मसाले से बचें।"
    },
    "joint_pain": {
        "mr_term": "सांधेदुखी",
        "mr_desc": "गुडघे, सांधे सुजणे, हालचाल करताना वेदना होणे आणि कडकपणा जाणवणे.",
        "mr_care": "हलका व्यायाम करा, वजन आटोक्यात ठेवा, आणि गरम शेक घ्या. डॉक्टरांच्या सल्ल्याने कॅल्शियम व व्हिटॅमिन डी तपासा.",
        "hi_term": "जोड़ों का दर्द",
        "hi_desc": "घुटनों या जोड़ों में दर्द, सूजन और अकड़न महसूस होना।",
        "hi_care": "हल्का व्यायाम करें, गर्म सिकाई करें और वजन नियंत्रित रखें।"
    }
}


def clean_text(text: str) -> str:
    """Normalize whitespace and remove excessive formatting artifacts."""
    if not text or not isinstance(text, str):
        return ""
    text = re.sub(r"\r\n|\r", "\n", text)
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def format_layman_list(items, lang="en") -> str:
    """Format list of layman synonyms naturally."""
    if isinstance(items, list):
        if not items:
            return ""
        if len(items) == 1:
            return items[0]
        if lang == "mr":
            return f"{items[0]} (किंवा {', '.join(items[1:])})"
        elif lang == "hi":
            return f"{items[0]} (या {', '.join(items[1:])})"
        else:
            return f"{items[0]} (also known as {', '.join(items[1:])})"
    return str(items)


def build_record(instruction: str, output: str, language: str, category: str) -> dict:
    """Build a unified Alpaca + ChatML format record."""
    clean_instr = clean_text(instruction)
    clean_out = clean_text(output)
    return {
        "instruction": clean_instr,
        "input": "",
        "output": clean_out,
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": clean_instr},
            {"role": "assistant", "content": clean_out},
        ],
        "language": language,
        "category": category,
    }


def load_layman_terms_samples() -> list:
    """Generate training samples from scientific-to-layman mapping and emergency triage routing."""
    samples = []
    if not LAYMAN_MAPPING_PATH.exists():
        print(f"[WARN] Layman mapping file not found at {LAYMAN_MAPPING_PATH}")
        return samples

    with open(LAYMAN_MAPPING_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)

    # 1. Scientific terms to layman explanations
    for term in data.get("terms", []):
        sci = term["scientific_term"]
        lay_en = format_layman_list(term["layman_en"], "en")
        lay_hi = format_layman_list(term["layman_hi"], "hi")
        lay_mr = format_layman_list(term["layman_mr"], "mr")
        exp_en = term["explanation_en"]
        exp_hi = term["explanation_hi"]
        exp_mr = term["explanation_mr"]
        life_en = term.get("lifestyle_advice_en", "")
        life_mr = term.get("lifestyle_advice_mr", "")

        # English queries
        q_en_list = [
            f"What does {sci} mean in simple terms?",
            f"Doctor diagnosed me with {sci}. What is it?",
            f"Can you explain {sci} like I am a beginner?",
            f"What is {sci} and what should I do about it?",
        ]
        ans_en = f"**{sci.capitalize()}** is the medical term for **{lay_en}**.\n\n{exp_en}"
        if life_en:
            ans_en += f"\n\n**Practical Care & Lifestyle Advice:**\n{life_en}"
        ans_en += MEDICAL_DISCLAIMER_EN

        for q in q_en_list:
            samples.append(build_record(q, ans_en, "en", "term_explanation"))

        # Marathi queries
        q_mr_list = [
            f"{sci} म्हणजे काय? सोप्या भाषेत सांगा.",
            f"डॉक्टरांनी मला {sci} सांगितले आहे, याचा अर्थ काय?",
            f"{sci} चा अर्थ काय आणि घरगुती काळजी कशी घ्यावी?",
            f"वैद्यकीय भाषेत {sci} कशाला म्हणतात?",
        ]
        ans_mr = f"**{sci.capitalize()}** म्हणजे सोप्या मराठीत **{lay_mr}** होय.\n\n{exp_mr}"
        if life_mr:
            ans_mr += f"\n\n**दैनंदिन काळजी व सल्ला:**\n{life_mr}"
        ans_mr += MEDICAL_DISCLAIMER_MR

        for q in q_mr_list:
            samples.append(build_record(q, ans_mr, "mr", "term_explanation"))

        # Hindi queries
        q_hi_list = [
            f"{sci} का क्या मतलब होता है?",
            f"डॉक्टर ने मुझे {sci} बताया है, यह क्या बीमारी है?",
            f"{sci} को आसान भाषा में समझाइए।",
        ]
        ans_hi = f"**{sci.capitalize()}** को आम बोलचाल में **{lay_hi}** कहा जाता है।\n\n{exp_hi}"
        if life_en:
            ans_hi += f"\n\n**देखभाल और जीवनशैली सुझाव:**\n{life_en}"
        ans_hi += MEDICAL_DISCLAIMER_HI

        for q in q_hi_list:
            samples.append(build_record(q, ans_hi, "hi", "term_explanation"))

    # 2. Emergency triage routing samples
    for emergency in data.get("emergency_triage_routing", []):
        cond = emergency["clinical_condition"]
        urgency = emergency["triage_urgency"]
        helplines = emergency["helpline_numbers"]
        adv_en = emergency["advisory_en"]
        adv_hi = emergency["advisory_hi"]
        adv_mr = emergency["advisory_mr"]

        if isinstance(helplines, list):
            hl_str = f"National Emergency Helplines: {', '.join(helplines)}"
        elif isinstance(helplines, dict):
            hl_str = f"National Emergency Helpline: {helplines.get('emergency_ambulance', '108 / 112')}"
        else:
            hl_str = "National Emergency Helpline: 108 / 112"

        # English triggers
        for trig in emergency.get("layman_triggers_en", []):
            q_en = f"I am experiencing {trig}. What should I do?"
            ans_en = (
                f"🚨 **EMERGENCY MEDICAL WARNING ({urgency})**\n\n"
                f"Your symptoms strongly indicate a potential medical emergency: **{cond}**.\n\n"
                f"**Immediate Instructions:**\n{adv_en}\n\n"
                f"📞 **Call Emergency Services Immediately:** {hl_str}"
                f"{MEDICAL_DISCLAIMER_EN}"
            )
            samples.append(build_record(q_en, ans_en, "en", "emergency_triage"))

        # Marathi triggers
        for trig_mr in emergency.get("layman_triggers_mr", []):
            q_mr = f"{trig_mr}. मी काय करू?"
            ans_mr = (
                f"🚨 **तातडीची वैद्यकीय आणीबाणी इशारा ({urgency})**\n\n"
                f"तुमची लक्षणे संभाव्य गंभीर स्थिती दर्शवतात: **{cond}**.\n\n"
                f"**तातडीच्या सूचना:**\n{adv_mr}\n\n"
                f"📞 **त्वरित रुग्णवाहिका किंवा आपत्कालीन मदत बोलवा:** १०८ / ११२ वर संपर्क करा."
                f"{MEDICAL_DISCLAIMER_MR}"
            )
            samples.append(build_record(q_mr, ans_mr, "mr", "emergency_triage"))

        # Hindi triggers
        for trig_hi in emergency.get("layman_triggers_hi", []):
            q_hi = f"{trig_hi}. मुझे क्या करना चाहिए?"
            ans_hi = (
                f"🚨 **आपातकालीन चिकित्सा चेतावनी ({urgency})**\n\n"
                f"आपके लक्षण संभावित गंभीर समस्या का संकेत देते हैं: **{cond}**.\n\n"
                f"**तत्काल निर्देश:**\n{adv_hi}\n\n"
                f"📞 **तुरंत आपातकालीन सहायता बुलाएं:** 108 / 112 डायल करें।"
                f"{MEDICAL_DISCLAIMER_HI}"
            )
            samples.append(build_record(q_hi, ans_hi, "hi", "emergency_triage"))

    return samples


def load_csv_knowledge() -> list:
    """Load knowledge from medical_knowledge.csv and sub-CSVs with multilingual variations."""
    samples = []
    csv_files = []

    if MED_KNOWLEDGE_CSV.exists():
        csv_files.append((MED_KNOWLEDGE_CSV, "symptom_guidance"))

    if MED_KNOWLEDGE_DIR.exists():
        for f in MED_KNOWLEDGE_DIR.glob("*.csv"):
            cat = f.stem
            if "symptom" in cat:
                cat_name = "symptom_guidance"
            elif "disease" in cat:
                cat_name = "disease_information"
            elif "medicine" in cat:
                cat_name = "medication_guidance"
            else:
                cat_name = "general_health"
            csv_files.append((f, cat_name))

    seen_pairs = set()

    for csv_file, cat_name in csv_files:
        try:
            with open(csv_file, "r", encoding="utf-8") as f:
                reader = csv.DictReader(f)
                for row in reader:
                    symptom = row.get("symptom", "").strip()
                    aliases = [a.strip() for a in row.get("aliases", "").split("|") if a.strip()]
                    qtype = row.get("question_type", "").strip()
                    response = row.get("response", "").strip()

                    if not symptom or not response:
                        continue

                    pair_key = (symptom, qtype, response[:40])
                    if pair_key in seen_pairs:
                        continue
                    seen_pairs.add(pair_key)

                    # English queries
                    if qtype == "symptoms":
                        q_en = f"What are the symptoms and causes of {symptom}?"
                    elif qtype == "assistance":
                        q_en = f"I am dealing with {symptom}. What should I do for relief?"
                    else:
                        q_en = f"Tell me about {symptom}."

                    out_en = response + MEDICAL_DISCLAIMER_EN
                    samples.append(build_record(q_en, out_en, "en", cat_name))

                    # Variations with aliases
                    for alias in aliases[:2]:
                        q_alias = f"What should I do if I have {alias}?"
                        samples.append(build_record(q_alias, out_en, "en", cat_name))

                    # Multilingual Marathi and Hindi mappings for core symptoms
                    if symptom in SYMPTOM_LOCALIZATION:
                        loc = SYMPTOM_LOCALIZATION[symptom]
                        mr_term = loc["mr_term"]
                        hi_term = loc["hi_term"]

                        if qtype == "symptoms":
                            q_mr = f"{mr_term}ची मुख्य लक्षणे कोणती आहेत?"
                            a_mr = f"**{mr_term}:** {loc['mr_desc']}{MEDICAL_DISCLAIMER_MR}"
                            samples.append(build_record(q_mr, a_mr, "mr", cat_name))

                            q_hi = f"{hi_term} के क्या लक्षण होते हैं?"
                            a_hi = f"**{hi_term}:** {loc['hi_desc']}{MEDICAL_DISCLAIMER_HI}"
                            samples.append(build_record(q_hi, a_hi, "hi", cat_name))
                        elif qtype == "assistance":
                            q_mr = f"मला {mr_term}चा त्रास होत आहे, काय उपाय करू?"
                            a_mr = f"**{mr_term}साठी प्राथमिक काळजी:**\n{loc['mr_care']}{MEDICAL_DISCLAIMER_MR}"
                            samples.append(build_record(q_mr, a_mr, "mr", cat_name))

                            q_hi = f"मुझे {hi_term} की समस्या है, क्या उपाय करना चाहिए?"
                            a_hi = f"**{hi_term} के लिए प्राथमिक देखभाल:**\n{loc['hi_care']}{MEDICAL_DISCLAIMER_HI}"
                            samples.append(build_record(q_hi, a_hi, "hi", cat_name))

        except Exception as e:
            print(f"[WARN] Error reading {csv_file}: {e}")

    return samples


def load_mediq_dialogues(max_samples: int = 3500) -> list:
    """Load and filter real patient-doctor / clinical hospital dialogue from MEDIQ dataset."""
    samples = []
    if not MEDIQ_CSV.exists():
        print(f"[WARN] MEDIQ CSV not found at {MEDIQ_CSV}")
        return samples

    excluded_categories = {
        "admissions", "courses", "exams", "career", "fees", "hostel", "library",
        "tenders", "procurement", "recruitment", "administrative"
    }

    try:
        with open(MEDIQ_CSV, "r", encoding="utf-8", errors="replace") as f:
            reader = csv.DictReader(f)
            count = 0
            for row in reader:
                if count >= max_samples:
                    break

                cat = (row.get("category") or "").strip().lower()
                if cat in excluded_categories:
                    continue

                med_rel = (row.get("ai_medical_relevance") or "").strip().lower()
                if med_rel not in ["high", "medium"]:
                    continue

                q = clean_text(row.get("question", ""))
                a = clean_text(row.get("answer", ""))

                if len(q) < 10 or len(a) < 35:
                    continue

                if any(x in a.lower() for x in [
                    "i'm sorry, i don't recognize",
                    "i cannot find a department",
                    "could you please clarify the name of the department",
                    "please contact the webmaster",
                ]):
                    continue

                detected_lang = (row.get("language_detected") or "en").strip().lower()
                ai_lang = (row.get("ai_language") or "").strip().lower()

                lang = "en"
                if "hi" in detected_lang or "hi" in ai_lang:
                    lang = "hi"

                if "disclaimer" not in a.lower() and "consult" not in a.lower():
                    if lang == "hi":
                        a_out = a + MEDICAL_DISCLAIMER_HI
                    else:
                        a_out = a + MEDICAL_DISCLAIMER_EN
                else:
                    a_out = a

                samples.append(build_record(q, a_out, lang, "patient_dialogue"))
                count += 1

    except Exception as e:
        print(f"[WARN] Error reading MEDIQ CSV: {e}")

    return samples


def generate_comprehensive_marathi_dataset() -> list:
    """
    Generate comprehensive, authentic, high-quality Marathi samples across:
    1. Common Regional Symptoms & Complaints
    2. Over-the-Counter & Common Medicines Advice
    3. Home Care, Ayurveda & Lifestyle Remedies
    4. Chronic Condition Support (Diabetes, BP, Thyroid, Asthma)
    5. Pediatric & Child Healthcare Red Flags
    6. Women's Health & Maternal Care
    7. Emergency Triage, First Aid & Accidents
    8. Seasonal & Infectious Diseases (Dengue, Malaria, Flu)
    9. Diagnostic Reports & Preventive Wellness
    10. Practical Patient-Doctor Consultations
    """
    samples = []

    # Comprehensive Topic Registry
    topics = [
        # --- Category 1: Common Regional Symptoms (20 topics) ---
        {
            "cat": "symptom_guidance",
            "ans": "ताप आणि थंडी वाजत असल्यास खालील काळजी घ्या:\n\n1. **विश्रांती:** शरीराला पूर्ण विश्रांती द्या, धावपळ टाळा.\n2. **पाण्याच्या घड्या:** कपाळावर आणि मानेवर सामान्य खोलीच्या पाण्याच्या सुती पट्ट्या ठेवा (बर्फाचे अतिथंड पाणी वापरू नका).\n3. **हायड्रेशन:** कोमट पाणी, तांदळाची पेज, किंवा लिंबू पाणी भरपूर प्या.\n4. **आहार:** हलका व ताजा आहार घ्या, जसे की मुगाची मऊ खिचडी किंवा पातळ सूप.\n\n🚨 **डॉक्टरांकडे जाण्याची वेळ:** ताप ३ दिवसांपेक्षा जास्त राहिल्यास, उलट्या सुरू झाल्यास, किंवा श्वास घेण्यास त्रास झाल्यास त्वरित वैद्यकीय तपासणी करून घ्या.",
            "queries": [
                "मला दोन दिवसांपासून खूप ताप आहे आणि थंडी वाजत आहे, काय करू?",
                "डॉक्टर, अंगात खूप कणकण आणि ताप भरला आहे, काय करावे?",
                "अचानक थंडी वाजून ताप आला तर घरी काय प्रथमोपचार करावेत?",
                "तापावर घरगुती काय काळजी घ्यावी आणि डॉक्टरांना कधी दाखवावे?",
                "माझ्या आईला ताप आला आहे आणि अंग थरथर कापत आहे, काय उपाय आहे?",
                "ताप १०२ अंश झाला आहे, त्वरित काय करता येईल?",
                "थंडी वाजून ताप आल्यास औषध कधी घ्यावे?",
                "ताप आल्यावर अंघोळ करावी का?",
                "ताप अंगात मुरू नये म्हणून काय खावे?",
                "कणकणी आल्यावर लगेच कोणती काळजी घ्यावी?"
            ]
        },
        {
            "cat": "symptom_guidance",
            "ans": "तीव्र डोकेदुखी व मायग्रेनच्या त्रासासाठी उपाय:\n\n1. **शांत व अंधारी खोली:** प्रकाश आणि मोठा आवाज टाळून शांत खोलीत डोळे मिटून झोपा.\n2. **थंड किंवा कोमट शेक:** मानेवर किंवा कपाळावर हलका कोल्ड पॅक ठेवा.\n3. **पाणी पिणे:** अनेकदा डिहायड्रेशनमुळे डोकेदुखी वाढते, त्यामुळे भरपूर पाणी प्या.\n4. **स्क्रीनपासून दूर राहा:** मोबाईल व लॅपटॉपचा वापर ताबडतोब थांबवा.\n\n⚠️ **धोक्याची सूचना:** डोकेदुखी अचानक सुरू होऊन असह्य झाल्यास, किंवा हातापायात मुंग्या/अशक्तपणा आल्यास तातडीने आपत्कालीन कक्ष गाठा.",
            "queries": [
                "माझ्या डोक्याची एकच बाजू खूप जोरात ठणकत आहे, काय करावे?",
                "मायग्रेनचा त्रास कसा कमी करता येईल?",
                "वारंवार डोकेदुखी होणे कशाचे लक्षण असू शकते?",
                "डोकेदुखीवर काही सुरक्षित आरामदायी उपाय सांगा ना.",
                "उन्हात गेल्यावर डोके खूप दुखू लागते आणि मळमळते, काय करू?",
                "स्क्रीन जास्त पाहिल्याने डोळे आणि कपाळ दुखते, काय सल्ला द्याल?",
                "मायग्रेनच्या वेदनेवर घरगुती काय शेक द्यावा?",
                "सकाळी उठल्या उठल्या डोके का दुखते?",
                "डोकेदुखी थांबवण्यासाठी चहा किंवा कॉफी घेणे योग्य आहे का?",
                "डोकेदुखीसोबत उलटीची भावना होत असल्यास काय करावे?"
            ]
        },
        {
            "cat": "symptom_guidance",
            "ans": "कोरडा खोकला आणि घशातील खवखवेसाठी घरगुती काळजी:\n\n1. **मिठाच्या पाण्याच्या गुळण्या:** कोमट पाण्यात चिमूटभर मीठ घालून दिवसातून ३-४ वेळा गुळण्या करा.\n2. **हळदीचे दूध:** रात्री झोपताना कोमट दुधात अर्धा चमचा हळद घालून प्या.\n3. **मध आणि आल्याचा रस:** १ चमचा मधात आल्याचा थोडा रस मिसळून हळूहळू चाटण करा.\n4. **वाफ घेणे:** दिवसातून २ वेळा कोमट पाण्याची वाफ घ्या.\n\n⚠️ खोकल्यासोबत कफातून रक्त येत असल्यास किंवा २ आठवड्यांपेक्षा जास्त खोकला राहिल्यास क्षयरोगाची (TB) किंवा छातीची तपासणी आवश्यक आहे.",
            "queries": [
                "रात्री झोपताना खूप कोरडा खोकला येतो, काय उपाय करावा?",
                "घशात जळजळ आणि खवखव होत आहे, काही घरगुती उपाय सांगा.",
                "खोकल्यामुळे छातीत कळा येतात, यावर काय करता येईल?",
                "कोरड्या खोकल्यावर सर्वात चांगला काढा कोणता?",
                "घसा बसला आहे आणि बोलताना त्रास होतो, काय काळजी घ्यावी?",
                "सतत येणारा खोकला कसा थांबवावा?",
                "खोकल्यावर आले आणि मधाचा वापर कसा करावा?",
                "कफ दाटून छातीत घरघर आवाज येत असल्यास काय करावे?",
                "दोन आठवड्यांपेक्षा जास्त खोकला असल्यास काय धोका असू शकतो?",
                "धुळीमुळे खोकला उफाळून येत असल्यास काय खबरदारी घ्यावी?"
            ]
        },
        {
            "cat": "symptom_guidance",
            "ans": "पोटदुखी व ॲसिडिटीसाठी प्राथमिक सल्ला:\n\n1. **थंड सायविरहित दूध:** छातीत जळजळ होत असल्यास अर्धा कप थंड दूध प्यायल्याने त्वरित थंडावा मिळतो.\n2. **जिरे आणि ओव्याचे पाणी:** १ ग्लास पाण्यात जिरे व ओवा उकळून कोमट प्या, यामुळे गॅस व पोटदुखी कमी होते.\n3. **वेळेवर जेवण:** जास्त वेळ उपाशी राहू नका आणि अति तेलकट-तिखट जेवण टाळा.\n4. **जेवणानंतर लगेच झोपू नका:** जेवण आणि झोप यामध्ये किमान २ तासांचे अंतर ठेवा.\n\n🚨 **तातडीची सूचना:** पोटदुखी पोटाच्या उजव्या बाजूला खाली सरकल्यास (अपेंडिक्सची शंका) किंवा उलट्या होऊन पोट कडक झाल्यास त्वरित सर्जनचा सल्ला घ्या.",
            "queries": [
                "माझ्या पोटात खूप दुखत आहे आणि गॅस झाला आहे, काय करू?",
                "जेवणानंतर छातीत जळजळ आणि आंबट ढेकर येतात, काय उपाय आहे?",
                "पोटात अचानक पेटके येत आहेत, घरगुती उपाय सांगा.",
                "ॲसिडिटीचा कायमचा त्रास कसा कमी करता येईल?",
                "पोट फुगल्यासारखे वाटते आणि अस्वस्थ वाटत आहे, काय करावे?",
                "तीव्र पोटदुखीवर डॉक्टरांकडे कधी धाव घ्यावी?",
                "नाभीजवळ (बेंबीजवळ) पोटात मुरडा का येतो?",
                "पित्ताचा त्रास वाढल्यावर उलटी का होते?",
                "ॲसिडिटीवर थंड दूध पिणे खरोखर गुणकारी आहे का?",
                "अपचनामुळे पोटात दुखत असल्यास काय खावे?"
            ]
        },
        {
            "cat": "symptom_guidance",
            "ans": "जुलाब होत असल्यास डिहायड्रेशन रोखणे हा सर्वात महत्त्वाचा उपचार आहे:\n\n1. **ORS द्रावण:** १ लिटर स्वच्छ उकळून थंड केलेल्या पाण्यात १ पाकीट ओआरएस मिसळा आणि प्रत्येक जुलाबानंतर अर्धा ते १ ग्लास पीत राहा.\n2. **तांदळाची पेज व ताक:** तांदळाची मऊ पेज आणि जिरे-मीठ घातलेले ताक प्या.\n3. **केळी आणि सफरचंद:** केळ्यातील पोटॅशियम शरीराची ताकद टिकवून ठेवण्यास मदत करते.\n4. **तळलेले व दुधाचे पदार्थ टाळा:** काही दिवस जड अन्न पूर्णपणे टाळा.\n\n🚨 जुलाबातून रक्त पडल्यास, किंवा डोळे खोल जाऊन लघवी बंद झाल्यास त्वरित आयव्ही सलाईनसाठी रुग्णालयात जा.",
            "queries": [
                "मला सकाळपासून पातळ शौचास होत आहे, काय करू?",
                "जुलाब थांबवण्यासाठी तात्काळ काय काळजी घ्यावी?",
                "लूज मोशन झाल्यावर ओआरएस (ORS) कसे प्यावे?",
                "जुलाब झाल्यावर कोणता आहार घ्यावा?",
                "वारंवार संडासला होत आहे आणि खूप अशक्तपणा आला आहे, काय करावे?",
                "जुलाब किती दिवस राहिल्यास धोकादायक असते?",
                "जुलाबात तांदळाची पेज आणि ताक पिण्याचे काय फायदे आहेत?",
                "अतिसारामध्ये पोटात पेटके येत असल्यास काय करावे?",
                "जुलाब झाल्यावर दूध पिणे का टाळावे?",
                "लूज मोशनसाठी डॉक्टरांकडे कधी जावे?"
            ]
        },
        {
            "cat": "symptom_guidance",
            "ans": "उलट्या आणि मळमळीवर काळजी:\n\n1. **घोट घोट पाणी:** एकदम भरपूर पाणी पिऊ नका, त्यामुळे पुन्हा उलटी होऊ शकते. दर १० मिनिटांनी १-२ घोट पाणी किंवा लिंबू पाणी प्या.\n2. **आल्याचा तुकडा:** आल्याचा लहान तुकडा तोंडात चघळल्याने मळमळ कमी होते.\n3. **पुदिनाचा अर्क:** पुदिना आणि लिंबाचा रस घेतल्यास पोटाला शांतता मिळते.\n4. **विश्रांती:** डोके थोडे उंचावर ठेवून झोपा.\n\n⚠️ २४ तासांत ३ पेक्षा जास्त वेळा उलट्या झाल्यास आणि पाणीही पोटात टिकत नसल्यास त्वरित डॉक्टरांकडून अँटी-इमेटिक इंजेक्शन किंवा सलाईन घेणे गरजेचे आहे.",
            "queries": [
                "काही खाल्ले की लगेच उलटी होते, काय करावे?",
                "सतत मळमळत आहे आणि अस्वस्थ वाटत आहे, काय उपाय करू?",
                "उलट्या थांबवण्यासाठी घरगुती काय करता येईल?",
                "अन्नाची उलटी होत आहे, पाणी पिणे योग्य आहे का?",
                "प्रवासात चक्कर आणि उलट्या होतात, काय उपाय करावा?",
                "उलट्या झाल्यावर डिहायड्रेशन कसे ओळखावे?",
                "उलटी थांबवण्यासाठी पुदिना आणि लिंबू कसे घ्यावे?",
                "पित्ताची पिवळी उलटी होत असल्यास काय करावे?",
                "उलटी झाल्यानंतर लगेच जेवण करावे का?",
                "उलट्या जास्त झाल्याने अंग थरथर कापत असल्यास काय काळजी घ्यावी?"
            ]
        },
        {
            "cat": "symptom_guidance",
            "ans": "चक्कर येणे (Vertigo / Dizziness) यावरील उपाय:\n\n1. **लगेच खाली बसा किंवा झोपा:** उभे राहू नका, ज्यामुळे पडण्याचा आणि मार लागण्याचा धोका टळेल.\n2. **पाय उंचावर ठेवा:** झोपून पायांखाली २ उशा ठेवा जेणेकरून मेंदूकडे रक्तपुरवठा वाढेल.\n3. **गोड पाणी किंवा ओआरएस:** रक्तातील साखर किंवा बीपी कमी झाला असल्यास त्वरित १ ग्लास पाण्यात साखर व चिमूटभर मीठ टाकून प्या.\n4. **गाडी चालवू नका:** चक्कर येत असताना वाहन किंवा यंत्रे चालवणे टाळा.\n\n⚠️ चक्कर येण्यासोबत हात-पाय लुळे पडणे किंवा बोलण्यात अडखळणे जाणवल्यास हा पक्षाघाताचा झटका असू शकतो, त्वरित आपत्कालीन मदत घ्या.",
            "queries": [
                "उठून उभे राहिल्यावर लगेच चक्कर येते, काय कारण असू शकते?",
                "डोळ्यांसमोर अचानक काजवे चमकतात आणि अंधारी येते, काय करू?",
                "सतत चक्कर येत असल्यास कोणती तपासणी करावी?",
                "चक्कर आल्यावर तात्काळ काय करावे?",
                "कमी रक्तदाबामुळे चक्कर येत असल्यास काय उपाय आहे?",
                "चक्कर येऊन पडल्यास काय प्रथमोपचार करावा?",
                "मानेच्या मणक्यामुळे (Cervical) चक्कर येते का?",
                "उन्हाळ्यात चक्कर का येते आणि कसे टाळावे?",
                "चक्कर आल्यावर पाणी कसे पाजावे?",
                "कानातील दोषामुळे चक्कर येते का?"
            ]
        },
        {
            "cat": "symptom_guidance",
            "ans": "सांधेदुखी आणि गुडघेदुखीवर सल्ला:\n\n1. **गरम पाण्याचा शेक:** सांध्यांवर गरम पाण्याच्या पिशवीने दिवसातून २ वेळा शेका.\n2. **वजन आटोक्यात ठेवणे:** अंगावरचे अतिरिक्त वजन कमी केल्यास गुडघ्यांवरील ताण ७०% कमी होतो.\n3. **कॅल्शियम आणि व्हिटॅमिन डी:** दूध, तीळ, नाचणी आणि रोज सकाळी १० मिनिटे कोवळ्या उन्हात बसा.\n4. **जमीनवर बसणे टाळा:** खुर्ची किंवा पाश्चात्त्य (कमोड) शौचालयाचा वापर करा.\n5. **हलके चालणे:** दररोज सपाट रस्त्यावर २० मिनिटे सावकाश चाला, पायऱ्या चढणे-उतरणे टाळा.",
            "queries": [
                "माझे दोन्ही गुडघे खूप दुखतात आणि चालताना कटकट आवाज येतो, काय करू?",
                "सांधेदुखीवर घरगुती काय शेक किंवा उपाय करावा?",
                "हिवाळ्यात सांधे जास्त का दुखतात आणि काय काळजी घ्यावी?",
                "गुडघ्याची झीज भरून काढण्यासाठी काय खावे?",
                "संधिवात (Arthritis) नियंत्रणात कसा ठेवावा?",
                "सांधेदुखीसाठी कोणता हलका व्यायाम चांगला आहे?",
                "गुडघे सुजल्यास त्यावर बर्फ लावावा की गरम शेक द्यावा?",
                "सांधेदुखीसाठी नाचणी आणि तीळ खाण्याचे काय फायदे आहेत?",
                "वयस्कर व्यक्तींमध्ये गुडघेदुखी कशी आटोक्यात ठेवावी?",
                "सांधेदुखीवर एरंडेल तेल किंवा मोहरीच्या तेलाने मसाज करावा का?"
            ]
        },
        {
            "cat": "symptom_guidance",
            "ans": "कंबरदुखी व पाठीच्या कण्यावरील ताणावर उपाय:\n\n1. **योग्य आसन (Posture):** ताठ बसा, पाठीमागे उशीचा आधार घ्या आणि दर ४५ मिनिटांनी थोडे चाला.\n2. **सपाट गादीवर झोपणे:** खूप मऊ किंवा खचणाऱ्या गादीऐवजी मध्यम कडक गादी वापरा. कडेवर झोपताना दोन्ही गुडघ्यांमध्ये उशी ठेवा.\n3. **गरम व थंड शेक:** सुरुवातीचे २ दिवस बर्फाने शेका, त्यानंतर गरम पाण्याचा शेक द्या.\n4. **वजन उचलू नका:** वाकून जड वस्तू उचलणे टाळा; वस्तू उचलताना गुडघे वाकवून उचला.\n\n🚨 लघवीवरचे नियंत्रण सुटल्यास किंवा पायात बधीरपणा आल्यास तात्काळ मणकेतज्ज्ञांना (Spine Specialist) भेटा.",
            "queries": [
                "खूप वेळ बसून काम केल्याने कंबर खूप दुखते, काय उपाय आहे?",
                "सायटिकाच्या कळा कमरेतून पायाकडे जातात, काय करावे?",
                "कंबरदुखीवर झोपण्याची योग्य पद्धत कोणती?",
                "कमरेला लचक भरल्यास काय प्रथमोपचार करावा?",
                "कंबरदुखीसाठी कोणते योगासन किंवा स्ट्रेचिंग फायदेशीर आहे?",
                "कंबरेचे दुखणे कधी गंभीर समजले पाहिजे?",
                "संगणकावर काम करताना पाठीचा कणा ताठ ठेवण्यासाठी काय करावे?",
                "कंबरेत उसण भरल्यास काय मलम लावावा?",
                "कंबरदुखीवर भुजंगासन करणे योग्य आहे का?",
                "जड वस्तू उचलताना कंबर का चमकते?"
            ]
        },
        {
            "cat": "symptom_guidance",
            "ans": "लघवीतील जळजळ व संसर्गावर (UTI) काळजी:\n\n1. **भरपूर पाणी पिणे:** दिवसाला किमान ३ ते ४ लिटर पाणी प्या, ज्यामुळे लघवीवाटे जंतू बाहेर पडण्यास मदत होते.\n2. **नारळ पाणी आणि ताक:** रोज २ शहाळ्याचे पाणी किंवा पातळ ताक प्यायल्याने पोटातील उष्णता कमी होते.\n3. **सिट्रस ड्रिंक्स:** लिंबू पाणी प्या, यामुळे लघवीचा आम्लपणा कमी होतो.\n4. **स्वच्छता:** लघवी जास्त वेळ रोखून ठेवू नका आणि गुप्तांगाची स्वच्छता कोरडी व स्वच्छ ठेवा.\n\n⚠️ लघवीतून रक्त पडत असल्यास, पाठीच्या बाजूला तीव्र वेदना असल्यास किंवा थंडी वाजून ताप आल्यास डॉक्टरांकडून अँटिबायोटिक्स घेणे अनिवार्य आहे.",
            "queries": [
                "लघवी करताना खूप जळजळ आणि आग होते, काय उपाय करावा?",
                "वारंवार लघवीला जावे लागते पण लघवी थेंब थेंब होते, काय कारण असू शकते?",
                "युरिन इन्फेक्शन (UTI) घरगुती उपायांनी कसे बरे करावे?",
                "लघवी पिवळी होत आहे आणि दुर्गंधी येत आहे, काय काळजी घ्यावी?",
                "महिलांमध्ये लघवीची जळजळ कशी थांबवावी?",
                "लघवीत जळजळ झाल्यावर किती पाणी प्यावे?",
                "युरिन इन्फेक्शनमध्ये शहाळ्याचे पाणी पिणे किती फायदेशीर आहे?",
                "लघवी रोखून धरल्याने संसर्ग वाढतो का?",
                "लघवीतून रक्त आल्यास त्वरित काय तपासणी करावी?",
                "उन्हाळ्यात लघवीची जळजळ का वाढते?"
            ]
        },
        {
            "cat": "symptom_guidance",
            "ans": "दातदुखी आणि हिरड्या सुजण्यावर घरगुती उपाय:\n\n1. **लवंग तेल:** दुखणाऱ्या दातावर लवंगेचे तेल लावून कापूस ठेवावा, लवंगेतील युजेनॉल वेदना त्वरित बधीर करते.\n2. **मिठाच्या पाण्याच्या गुळण्या:** कोमट पाण्यात मीठ घालून दिवसातून ३-४ वेळा खळखळून गुळण्या कराव्यात, यामुळे जंतू नष्ट होतात.\n3. **गार किंवा अतिगोड अन्न टाळा:** दात सेन्सिटिव्ह झाले असल्यास अतिथंड किंवा अतिउष्ण पदार्थ खाणे टाळा.\n4. **ब्रश करण्याची पद्धत:** मऊ ब्रशने दिवसातून २ वेळा हळुवार दात घासावेत.\n\n⚠️ दातात पू (Pus) झाला असल्यास किंवा जबडा सुजल्यास ताबडतोब दंतवैद्यांना (Dentist) दाखवा.",
            "queries": [
                "माझा दात खूप ठणकत आहे आणि हिरडी सुजली आहे, काय करू?",
                "दातदुखीवर लवंगाचे तेल कसे वापरावे?",
                "हिरड्यांमधून रक्त येत असल्यास कोणती काळजी घ्यावी?",
                "रात्री दाढ दुखून असह्य त्रास होत असल्यास तात्पुरता काय उपाय आहे?",
                "दात सेन्सिटिव्ह (थंड पाणी लागणे) झाल्यावर काय करावे?",
                "हिरड्यांची सूज कमी करण्यासाठी मिठाच्या पाण्याच्या गुळण्या कशा कराव्यात?",
                "दातदुखीवर घरगुती काय शेक द्यावा?",
                "अक्कलदाढ (Wisdom tooth) येताना होणाऱ्या दुखण्यावर काय उपाय आहे?"
            ]
        },
        {
            "cat": "symptom_guidance",
            "ans": "तोंड येणे आणि जिभेवर फोड (Mouth Ulcers) यावर उपाय:\n\n1. **बी-कॉम्प्लेक्सची कमतरता:** तोंड येणे हे प्रामुख्याने व्हिटॅमिन बी१२ आणि लोहाच्या कमतरतेमुळे किंवा पोटातील उष्णतेमुळे होते.\n2. **मध किंवा तूप लावणे:** फोडांवर शुद्ध मध किंवा थोडे गाईचे तूप लावल्यास जळजळ कमी होते.\n3. **मसालेदार अन्न बंद करा:** तिखट, मसालेदार आणि आंबट पदार्थ खाणे पूर्णपणे थांबवा.\n4. **दही व ताक:** आहारात थंड ताक आणि दह्याचा समावेश करा ज्यामुळे पोटातील उष्णता शांत होईल.\n\n⚠️ तोंडात आलेला व्रण (अल्सर) २ आठवड्यांपेक्षा जास्त काळ बरा न झाल्यास तज्ज्ञ डॉक्टरांकडून बायोप्सी किंवा तपासणी करून घ्यावी.",
            "queries": [
                "माझे तोंड आले आहे आणि जीभ खूप जळजळत आहे, काय खावे?",
                "तोंडात वारंवार फोड येण्याचे काय कारण असते?",
                "तोंडातील अल्सरवर घरगुती काय लावावे?",
                "तोंड आल्यावर बी-कॉम्प्लेक्स गोळी घेणे योग्य आहे का?",
                "तोंडातील फोड लवकर भरून येण्यासाठी काय पथ्य पाळावे?",
                "ओठांच्या आत अल्सर झाला आहे, काय लावावे?",
                "जास्त तिखट खाल्ल्याने तोंड आले असल्यास काय उपाय करावा?",
                "तोंड येणे आणि पोटातील उष्णता यांचा काय संबंध आहे?"
            ]
        },
        {
            "cat": "symptom_guidance",
            "ans": "त्वचेवर खाज, पुरळ किंवा पित्त (Skin Itching / Urticaria) यावर उपाय:\n\n1. **थंड पाण्याचा शेक:** खाज सुटलेल्या भागावर बर्फ किंवा थंड पाण्याच्या घड्या ठेवाव्यात.\n2. **खोबरेल तेल आणि कापूर:** शुद्ध खोबरेल तेलात चिमूटभर कापूर मिसळून लावल्यास खाज व अंगाची आग त्वरित कमी होते.\n3. **सुती सैल कपडे:** घामाने पुरळ वाढू नये म्हणून सैल, सुती आणि स्वच्छ कपडे वापरा.\n4. **खाजवणे टाळा:** नखांनी खाजवू नका, यामुळे जखम होऊन बॅक्टेरियल संसर्ग पसरू शकतो.\n\n⚠️ खाजण्यासोबत ओठ किंवा डोळे सुजणे, आणि श्वास घेण्यास त्रास झाल्यास हा तीव्र ॲलर्जीचा झटका (Anaphylaxis) असू शकतो, त्वरित आपत्कालीन इंजेक्शन आवश्यक असते.",
            "queries": [
                "अंगावर बारीक बारीक पुरळ उठून खूप खाज सुटत आहे, काय उपाय करू?",
                "अंगावर गांधी (पित्त) उठल्यावर घरगुती काय लावावे?",
                "उन्हाळ्यात घामोळ्या आणि त्वचेची जळजळ कशी थांबवावी?",
                "त्वचेवर खोबरेल तेल आणि कापूर लावण्याचे काय फायदे आहेत?",
                "साबणामुळे त्वचेची ॲलर्जी होत असल्यास काय काळजी घ्यावी?",
                "रात्री झोपताना अंग खाजवत असल्यास काय करावे?",
                "फंगल इन्फेक्शन (गजकर्ण) वर काय खबरदारी घ्यावी?",
                "अचानक अंगावर लाल चट्टे उठल्यास डॉक्टरांकडे कधी जावे?"
            ]
        },
        {
            "cat": "symptom_guidance",
            "ans": "टाचदुखी आणि सकाळी उठल्यावर पाय टेकताना होणारा त्रास (Plantar Fasciitis):\n\n1. **सकाळी उठल्याबरोबर व्यायाम:** सकाळी जमिनीवर पाय ठेवण्यापूर्वी तळपाय आणि बोटे हलके स्ट्रेच करा.\n2. **बर्फाचा रोलर:** पाण्याच्या बाटलीत पाणी गोठवून ती बाटली तळपायाखाली ठेवून १० मिनिटे पुढे-मागे फिरवा.\n3. **मऊ चप्पल:** घरात अनवाणी पायाने फिरू नका; मऊ सोल असलेल्या ऑर्थोपेडिक चपला वापरा.\n4. **वजन कमी करणे:** शरीराचे अतिरिक्त वजन टाचांवर थेट दाब आणते.\n\nनियमित स्ट्रेचिंगने टाचेचा ताण २-३ आठवड्यांत लक्षणीयरीत्या कमी होतो.",
            "queries": [
                "सकाळी उठल्यावर टाच जमिनीवर टेकवत नाही, इतकी कळ येते, काय कारण असू शकते?",
                "टाचदुखीवर घरगुती काय शेक द्यावा?",
                "टाचांच्या दुखण्यावर बर्फाने मसाज कसा करावा?",
                "खूप वेळ उभे राहिल्याने टाचा दुखत असल्यास काय काळजी घ्यावी?",
                "टाचदुखीसाठी कोणत्या प्रकारची चप्पल वापरावी?",
                "टाचेचे हाड वाढल्यास काय उपाय करावा?",
                "टाचांच्या दुखण्यावर कोणते व्यायाम उपयुक्त आहेत?",
                "टाचदुखी किती दिवसांत बरी होते?"
            ]
        },

        # --- Category 2: Over-the-Counter & Common Medicines Advice (10 topics) ---
        {
            "cat": "medication_guidance",
            "ans": "💊 **पॅरासिटामॉल / डोलो ६५० बद्दल मार्गदर्शक सूचना:**\n\n1. **वापर:** ताप कमी करण्यासाठी आणि सौम्य अंगदुखी, डोकेदुखीवर हे औषध वापरले जाते.\n2. **कधी घ्यावी:** नेहमी जेवणानंतर किंवा चहा-नाश्त्यानंतर कोमट पाण्यासोबत घ्यावी (उपाशीपोटी घेणे टाळावे).\n3. **अंतर:** दोन गोळ्यांमध्ये किमान ६ ते ८ तासांचे अंतर असावे. २४ तासांत प्रौढांनी जास्तीत जास्त ३ ते ४ ग्रॅमपेक्षा (४ ते ६ गोळ्या) जास्त घेऊ नये.\n4. **काळजी:** यकृताचा (Liver) आजार असल्यास डॉक्टरांच्या सल्ल्याशिवाय घेऊ नका.\n5. **मुलांचा डोस:** लहान मुलांना त्यांच्या वजनानुसार पॅरासिटामॉल ड्रॉप्स किंवा सिरप दिले जाते, स्वतःहून डोस ठरवू नका.",
            "queries": [
                "पॅरासिटामॉल ६५० गोळी कधी आणि कशी घ्यावी?",
                "डोलो ६५० उपाशीपोटी घेऊ शकतो का?",
                "तापावर पॅरासिटामॉल दिवसातून किती वेळा घेता येते?",
                "पॅरासिटामॉलचे काय दुष्परिणाम (side effects) होऊ शकतात?",
                "लहान मुलांना पॅरासिटामॉल सिरप कसे द्यावे?",
                "डोलो आणि पॅरासिटामॉल एकच औषध आहे का?",
                "पॅरासिटामॉल घेतल्यावर घाम का सुटतो?",
                "ताप कमी झाल्यावर पॅरासिटामॉल बंद करावी का?",
                "पॅरासिटामॉल दुधासोबत घेऊ शकतो का?",
                "पॅरासिटामॉलचा जास्त डोस घेतल्यास काय धोका असतो?"
            ]
        },
        {
            "cat": "medication_guidance",
            "ans": "💧 **ओआरएस (ORS) वापराची योग्य पद्धत:**\n\n1. **बनवण्याची पद्धत:** १ संपूर्ण ओआरएस पाकीट १ लिटर स्वच्छ (उकळून थंड केलेल्या) पाण्यात पूर्णपणे विरघळवा.\n2. **साठवणूक:** तयार केलेले ओआरएस द्रावण झाकून ठेवा आणि २४ तासांच्या आतच वापरा. २४ तासांनंतर उरलेले पाणी टाकून द्या आणि नवीन बनवा.\n3. **पिण्याची पद्धत:** एकदम न पिता घोट घोट प्या. प्रत्येक जुलाब किंवा उलटीनंतर अर्धा ते एक कप ओआरएस प्यावे.\n4. **चूक टाळा:** ओआरएस द्रावणात स्वतःहून साखर, ग्लुकोज किंवा दूध मिसळू नका.\n\nओआरएस हे शरीरातील मीठ आणि पाण्याचे प्रमाण संतुलित ठेवून रुग्णाचा जीव वाचवते.",
            "queries": [
                "ओआरएस (ORS) चे पाणी कसे तयार करावे?",
                "ओआरएस पाकीट किती पाण्यात मिसळावे?",
                "तयार केलेले ओआरएस पाणी किती वेळ वापरता येते?",
                "जुलाब आणि उलट्यांमध्ये ओआरएस पिण्याचे काय फायदे आहेत?",
                "इलेक्ट्रॉल आणि ओआरएस मध्ये काय फरक आहे?",
                "घरी ओआरएस कसे बनवावे (मीठ-साखर पाणी)?",
                "लहान बाळांना ओआरएस चमच्याने कसे पाजावे?",
                "ओआरएस जास्त पिल्याने काही नुकसान होते का?",
                "उन्हाळ्यात थकवा आल्यावर ओआरएस पिता येते का?",
                "ओआरएस गरम पाण्यात बनवावे का साध्या पाण्यात?"
            ]
        },
        {
            "cat": "medication_guidance",
            "ans": "🌿 **ॲण्टासिड औषधांविषयी माहिती:**\n\n1. **वापर:** छातीत जळजळ, आंबट ढेकर आणि पोटातील अतिरिक्त ॲसिड त्वरित निष्प्रभ करण्यासाठी.\n2. **वेळ:** जेवणानंतर १ तासाने किंवा जळजळ सुरू झाल्यावर २ चमचे सिरप प्यावे. चघळण्याच्या गोळ्या असतील तर त्या चघळून खाव्यात, गिळू नयेत.\n3. **पँटोप्राझोल / ओमेझ (PPIs):** पँटोप्राझोल किंवा रॅबेप्राझोल या गोळ्या सकाळी उठल्यावर **उपाशीपोटी** जेवणाच्या ३० मिनिटे आधी घेतल्यास दिवसभर ॲसिडिटीपासून संरक्षण मिळते.\n4. **काळजी:** सलग २ आठवड्यांपेक्षा जास्त दिवस रोज ॲण्टासिड घेऊ नका, कारण त्यामुळे मूळ पचनसंस्थेचे आजार लपून राहू शकतात.",
            "queries": [
                "डायजीन किंवा ॲण्टासिड सिरप कधी प्यावे?",
                "ॲसिडिटीची गोळी जेवणापूर्वी घ्यावी की जेवणानंतर?",
                "पोटात जळजळ झाल्यावर जेलुसिल सिरप किती चमचे घ्यावे?",
                "ॲण्टासिड गोळी चावून खावी की गिळावी?",
                "रोज ॲसिडिटीची औषधे घेणे सुरक्षित आहे का?",
                "पँटोप्राझोल गोळी उपाशीपोटीच का घ्यावी लागते?",
                "ओमेझ आणि पँटोप्राझोलमध्ये काय फरक आहे?",
                "ॲसिडिटीच्या सिरपचा प्रभाव किती वेळ राहतो?",
                "गरोदरपणात ॲसिडिटीचे सिरप पिता येते का?",
                "ॲसिडिटीच्या औषधांचे दुष्परिणाम काय असू शकतात?"
            ]
        },
        {
            "cat": "medication_guidance",
            "ans": "🛡️ **सिट्रिझिन (Cetirizine) मार्गदर्शक सूचना:**\n\n1. **उपयोग:** ॲलर्जी, सतत शिंका येणे, नाकातून पाणी गळणे, डोळ्यांना खाज सुटणे किंवा त्वचेवर पित्त/गांधी उठणे यावर उपयुक्त.\n2. **वेळ:** ही गोळी सहसा **रात्री झोपताना** घ्यावी, कारण यामुळे सुस्ती आणि झोप येते.\n3. **सावधगिरी:** सिट्रिझिन घेतल्यानंतर वाहन चालवणे, जड यंत्रसामग्री हाताळणे किंवा मद्यपान करणे पूर्णपणे टाळावे.\n4. **डोस:** प्रौढांसाठी दिवसातून एकदा (१० मिग्रॅ) पुरेशी असते. गरोदर किंवा स्तनपान देणाऱ्या महिलांनी डॉक्टरांच्या सल्ल्याशिवाय घेऊ नये.",
            "queries": [
                "सिट्रिझिन गोळी कशासाठी वापरली जाते?",
                "सिट्रिझिन गोळी घेतल्यावर झोप का येते?",
                "सिट्रिझिन दिवसा घ्यावी की रात्री?",
                "शिंका आणि नाकातून पाणी गळत असल्यास सिट्रिझिन घ्यावी का?",
                "सिट्रिझिन घेतल्यानंतर गाडी चालवणे सुरक्षित आहे का?",
                "सिट्रिझिनचे मुख्य साइड इफेक्ट्स कोणते आहेत?",
                "सिट्रिझिन लहान मुलांना देता येते का?",
                "धुळीच्या ॲलर्जीवर सिट्रिझिन किती दिवस घ्यावी?",
                "सिट्रिझिन रिकाम्या पोटी घ्यावी की जेवणानंतर?",
                "त्वचेवरील खाजेसाठी सिट्रिझिन उपयुक्त आहे का?"
            ]
        },
        {
            "cat": "medication_guidance",
            "ans": "🍵 **कफ सिरप वापरताना घ्यायची काळजी:**\n\n1. **प्रकार ओळखा:**\n   - **कोरडा खोकला:** यासाठी 'कफ सप्रेसंट' (Dextromethorphan युक्त) सिरप लागते.\n   - **ओला कफ (कफ पडणारा):** यासाठी 'एक्सपेक्टोरंट' (Ambroxol / Guaiphenesin युक्त) सिरप लागते जे कफ पातळ करून बाहेर काढते.\n2. **पाणी पिणे टाळा:** कफ सिरप प्यायल्यानंतर लगेच पाणी पिऊ नका, किमान १५ मिनिटे थांबा जेणेकरून औषधाचा लेप घशावर राहील.\n3. **मुलांसाठी कडक इशारा:** प्रौढांचे कफ सिरप लहान मुलांना कधीही देऊ नका. लहान मुलांना बालरोगतज्ज्ञांच्या सल्ल्यानेच औषध द्या.\n4. **अतिवापर टाळा:** खोकला ३ आठवड्यांपेक्षा जास्त काळ राहिल्यास केवळ सिरपवर विसंबून न राहता छातीचा एक्स-रे काढून घ्या.",
            "queries": [
                "खोकल्याचे औषध (कफ सिरप) कसे निवडावे?",
                "कोरड्या खोकल्यासाठी आणि कफाच्या खोकल्यासाठी एकच सिरप चालते का?",
                "कफ सिरप घेतल्यावर पाणी प्यावे का?",
                "लहान मुलांना प्रौढांचे कफ सिरप देणे सुरक्षित आहे का?",
                "कफ सिरपचे काय दुष्परिणाम असू शकतात?",
                "कफ सिरप पिल्याने गुंगी का येते?",
                "कफ सिरप दिवसातून किती वेळा आणि किती एमएल प्यावे?",
                "कफ सिरपची बाटली उघडल्यानंतर किती दिवस चांगली राहते?",
                "कफ पातळ करण्यासाठी सिरप कसे काम करते?",
                "गरोदरपणात कफ सिरप घेणे सुरक्षित असते का?"
            ]
        },
        {
            "cat": "medication_guidance",
            "ans": "🦴 **व्हिटॅमिन डी३ आणि कॅल्शियम मार्गदर्शक तत्त्वे:**\n\n1. **व्हिटॅमिन डी३ (६०,००० IU):** हे सहसा आठवड्यातून एकदा जेवणानंतर दुधासोबत घ्यायचे असते (कारण व्हिटॅमिन डी स्निग्ध पदार्थांमध्ये विरघळते).\n2. **कॅल्शियमची गोळी:** दुपारच्या जेवणानंतर किंवा रात्रीच्या जेवणानंतर भरपूर पाण्यासोबत घ्यावी.\n3. **महत्त्वाचा नियम:** कॅल्शियम आणि आयर्न (लोह) च्या गोळ्या कधीही एकाच वेळी घेऊ नयेत! कॅल्शियममुळे शरीरात लोह शोषले जात नाही. दोन्हीमध्ये किमान ४ तासांचे अंतर ठेवा.\n4. **नैसर्गिक स्रोत:** कोवळे ऊन, दूध, दही, पनीर, नाचणी, आणि तीळ आहारात ठेवा.",
            "queries": [
                "व्हिटॅमिन डी३ ची गोळी किंवा सॅशे कधी घ्यावे?",
                "कॅल्शियमच्या गोळ्या कधी आणि कशासोबत घ्याव्यात?",
                "हाडे मजबूत करण्यासाठी कॅल्शियम सप्लिमेंट कसे काम करते?",
                "व्हिटॅमिन डी आठवड्यातून एकदा घ्यायचे असते का?",
                "कॅल्शियम आणि आयर्नच्या गोळ्या एकत्र घेऊ शकतो का?",
                "व्हिटॅमिन डी३ दुधात मिसळून का घ्यावे लागते?",
                "कॅल्शियमच्या गोळ्या उपाशीपोटी का घेऊ नयेत?",
                "हाडांमधील कटकट आवाज कमी करण्यासाठी कोणते व्हिटॅमिन लागते?",
                "कॅल्शियमच्या गोळ्यांमुळे मुतखडा (Kidney Stone) होतो का?",
                "उन्हातून व्हिटॅमिन डी मिळवण्यासाठी सर्वोत्तम वेळ कोणती?"
            ]
        },
        {
            "cat": "medication_guidance",
            "ans": "🩸 **आयर्न (लोह) आणि फॉलिक ॲसिड वापर सूचना:**\n\n1. **शोषणासाठी व्हिटॅमिन सी:** लोहाची गोळी लिंबू पाणी किंवा संत्र्याच्या रसासोबत घेतल्यास शरीरात लोह उत्तम प्रकारे शोषले जाते.\n2. **चहा-कॉफी टाळा:** गोळी घेण्याच्या आधी आणि नंतर किमान २ तास चहा, कॉफी किंवा दूध पिऊ नका; यामुळे लोहाचे शोषण थांबते.\n3. **सामान्य बदल:** आयर्न गोळी सुरू असताना शौचास काळा रंग येणे पूर्णपणे सामान्य आहे, घाबरण्याचे कारण नाही.\n4. **पोटात त्रास:** उपाशीपोटी घेतल्यास मळमळ किंवा बद्धकोष्ठता होऊ शकते, म्हणून हलक्या जेवणानंतर १ तासाने घेणे सोयीचे ठरते.",
            "queries": [
                "रक्तातील हिमोग्लोबिन वाढवण्यासाठी आयर्नची गोळी कशी घ्यावी?",
                "आयर्न गोळी उपाशीपोटी घ्यावी की जेवणानंतर?",
                "लोहाची गोळी चहासोबत का घेऊ नये?",
                "आयर्नची गोळी घेतल्यावर शौचास काळे होते का?",
                "गरोदरपणात फॉलिक ॲसिड का महत्त्वाचे आहे?",
                "आयर्नच्या गोळीसोबत लिंबू पाणी पिण्याचे काय फायदे आहेत?",
                "लोहाच्या गोळीमुळे बद्धकोष्ठता झाल्यास काय करावे?",
                "ॲनिमिया बरा होण्यासाठी आयर्न गोळ्या किती महिने घ्याव्या लागतात?",
                "रक्तातील लोहाची पातळी वाढवण्यासाठी आहारात काय खावे?",
                "फॉलिक ॲसिड बाळाच्या विकासासाठी का आवश्यक आहे?"
            ]
        },

        # --- Category 3: Home Care, Ayurveda & Lifestyle Remedies (8 topics) ---
        {
            "cat": "general_health",
            "ans": "☕ **रोगप्रतिकारक घरगुती काढा बनवण्याची कृती:**\n\n1. **साहित्य:** २ कप पाणी, ४-५ तुळशीची पाने, १ तुकडा ठेचलेले आले, २ लवंगा, ३ काळीमिरी आणि थोडी दालचिनी.\n2. **कृती:** मंद आचेवर पाणी निम्मे होईपर्यंत उकळा. गाळून कोमट झाल्यावर चवीसाठी थोडा गूळ किंवा मध घाला.\n3. **फायदे:** श्वसनमार्गातील जंतू नष्ट होतात, घशाला आराम मिळतो आणि शरीराची नैसर्गिक प्रतिकारशक्ती वाढते.\n4. **सावधगिरी:** काढा अतिप्रमाणात किंवा खूप कडक पिऊ नका, दिवसातून एकदा किंवा दोनदा अर्धा कप काढा पुरेसा असतो.",
            "queries": [
                "सर्दी-खोकल्यावर घरगुती काढा कसा बनवावा?",
                "तुळस, आले आणि लवंगाचा काढा पिण्याचे काय फायदे आहेत?",
                "काढा दिवसातून किती वेळा प्यावा?",
                "काढा पिल्याने ॲसिडिटी होऊ नये म्हणून काय करावे?",
                "रोगप्रतिकारशक्ती वाढवण्यासाठी सर्वोत्तम घरगुती पेय कोणते?",
                "काळीमिरी आणि दालचिनीचा काढा खोकल्यावर कसा काम करतो?",
                "काढा उपाशीपोटी प्यावा की जेवणानंतर?",
                "लहान मुलांना काढा देताना काय काळजी घ्यावी?",
                "हिवाळ्यात दररोज काढा पिणे शरीरासाठी सुरक्षित आहे का?",
                "काढ्यामध्ये गूळ टाकावा की मध?"
            ]
        },
        {
            "cat": "general_health",
            "ans": "🥛 **हळदीचे दूध (गोल्डन मिल्क) पिण्याचे फायदे:**\n\n1. **नैसर्गिक अँटिबायोटिक:** हळदीमध्ये 'कर्क्युमिन' असते, जे शरीरातील सूज आणि संसर्ग कमी करण्यास मदत करते.\n2. **बनवण्याची पद्धत:** १ कप कोमट दुधात अर्धा चमचा शुद्ध हळद आणि चिमूटभर काळीमिरी पावडर मिसळा (काळीमिरीमुळे हळदीचे शोषण शरीरात चांगले होते).\n3. **फायदे:** रात्री झोपताना घेतल्यास अंगदुखी कमी होते, शांत झोप लागते आणि घशाची खवखव बरी होते.\n4. **कफ असल्यास:** कफ जास्त असल्यास गाईचे किंवा साय नसलेले पातळ दूध वापरावे.",
            "queries": [
                "अंगदुखी आणि घसादुखीवर हळदीचे दूध पिणे फायदेशीर आहे का?",
                "हळदीचे दूध कसे बनवावे?",
                "रात्री हळदीचे दूध पिल्याने काय फायदे होतात?",
                "हळदीच्या दुधात काळीमिरी का टाकावी?",
                "खोकल्यासाठी हळदीचे दूध कसे प्यावे?",
                "हळदीचे दूध रोगप्रतिकारक शक्ती कशी वाढवते?",
                "हळदीचे दूध पिल्याने जखम लवकर भरते का?",
                "लहान मुलांना हळदीचे दूध देणे सुरक्षित आहे का?",
                "हळदीच्या दुधात साखर घालावी की गूळ?",
                "हळदीचे दूध दिवसातून किती वेळा प्यावे?"
            ]
        },
        {
            "cat": "general_health",
            "ans": "♨️ **वाफ घेण्याची शास्त्रीय पद्धत (Steam Inhalation):**\n\n1. **पद्धत:** एका मोठ्या भांड्यात पाणी उकळून घ्या. डोक्यावरून सुती चादर किंवा टॉवेल पांघरून नाकाने दीर्घ श्वास घेत वाफ आत ओढा.\n2. **निलगिरी तेल:** १-२ थेंब निलगिरी तेल पाण्यात टाकल्यास सायनस आणि छातीतील कफ पटकन सैल होतो.\n3. **वेळ:** ५ ते १० मिनिटे वाफ पुरेशी असते. दिवसातून २ वेळा वाफ घेतल्यास नाक व घसा मोकळा होतो.\n4. **खबरदारी:** लहान मुलांना वाफ देताना गरम पाण्याचे भांडे अंगावर सांडणार नाही याची अत्यंत दक्षता घ्या; शक्यतो फेशिअल स्टीमर वापरा.",
            "queries": [
                "चोंदलेले नाक मोकळे करण्यासाठी वाफ कशी घ्यावी?",
                "वाफेमध्ये निलगिरी तेल किंवा विक्स टाकणे योग्य आहे का?",
                "दिवसातून किती वेळा वाफ घ्यावी?",
                "सायनसच्या त्रासावर वाफ घेण्याचे काय फायदे आहेत?",
                "लहान मुलांना वाफ देताना काय काळजी घ्यावी?",
                "वाफ घेताना डोळे बंद ठेवावे का?",
                "छातीतील कफ मोकळा करण्यासाठी वाफेचा काय फायदा होतो?",
                "वाफ घेतल्यानंतर लगेच थंड हवेत जावे का?",
                "फेशिअल स्टीमर वापरणे सुरक्षित असते का?",
                "वाफेमध्ये ओवा टाकून वाफ घेतल्यास काय फायदा होतो?"
            ]
        },
        {
            "cat": "general_health",
            "ans": "🌾 **जिरे व ओव्याचे पाचक पाणी:**\n\n1. **पचनशक्ती वाढवणे:** ओव्यातील थायमॉल घटक पोटातील पाचक रस उत्तेजित करतो आणि पोट फुगणे, आंबट ढेकर थांबवतो.\n2. **कृती:** १ ग्लास पाण्यात १ चमचा जिरे आणि अर्धा चमचा ओवा घालून ५ मिनिटे उकळा. कोमट झाल्यावर गाळून प्या.\n3. **वेळ:** जेवणानंतर किंवा सकाळी उपाशीपोटी घेतल्यास पोट हलके राहते.\n4. **बाळंतपण:** बाळंतिणीच्या पोटाचे स्नायू बळकट करण्यासाठी आणि पचनासाठी ओव्याचे पाणी परंपरेने अत्यंत गुणकारी मानले जाते.",
            "queries": [
                "अपचन आणि पोट फुगल्यावर ओव्याचे पाणी कसे फायदेशीर ठरते?",
                "जिरे आणि ओवा उकळून पाणी पिल्याने वजन कमी होते का?",
                "गॅस आणि पोटदुखीवर ओव्याचा काय उपाय करावा?",
                "बाळंतिणीला ओव्याचे पाणी का दिले जाते?",
                "सकाळी रिकाम्या पोटी ओव्याचे पाणी पिणे योग्य आहे का?",
                "जिऱ्याच्या पाण्याने पचनक्रिया कशी सुधारते?",
                "पोटात जडपणा आल्यास ओवा आणि काळे मीठ कसे घ्यावे?",
                "जिरे-ओव्याचे पाणी गरम प्यावे की थंड?",
                "ॲसिडिटीवर जिऱ्याचे पाणी कसे मदत करते?",
                "जिरे आणि धने उकळून पाणी पिण्याचे काय फायदे आहेत?"
            ]
        },
        {
            "cat": "general_health",
            "ans": "🌿 **बद्धकोष्ठतेवरील नैसर्गिक व सुरक्षित उपाय:**\n\n1. **इसबगोल (Psyllium Husk):** १ ते २ चमचे इसबगोल १ ग्लास कोमट पाण्यात किंवा दुधात मिसळून रात्री झोपताना लगेच प्यावे. इसबगोल आतड्यातील मऊपणा वाढवून शौचास साफ करतो.\n2. **त्रिफळा चूर्ण:** १ चमचा त्रिफळा चूर्ण कोमट पाण्यातून रात्री घेतल्यास पचनसंस्था स्वच्छ होते.\n3. **पाणी आणि फायबर:** दिवसातून ३-४ लिटर पाणी प्या. हिरव्या पालेभाज्या, पपई, पेरू, अंजिर आणि ओट्स आहारात ठेवा.\n4. **शौचाची वेळ न टाळणे:** सकाळी शौचाची भावना झाल्यावर ती रोखून धरू नका.",
            "queries": [
                "शौचास साफ न होण्याची समस्या कशी दूर करावी?",
                "बद्धकोष्ठतेवर इसबगोल (Isabgol) कसे घ्यावे?",
                "त्रिफळा चूर्ण रात्री घेण्याचे काय नियम आहेत?",
                "पोट साफ राहण्यासाठी आहारात फायबर कसे वाढवावे?",
                "तीव्र बद्धकोष्ठतेमुळे मूळव्याध होऊ नये म्हणून काय करावे?",
                "इसबगोल गरम दुधात घ्यावे का थंड पाण्यात?",
                "बद्धकोष्ठतेवर पपई आणि अंजिर खाण्याचे काय फायदे आहेत?",
                "सकाळी उठल्यावर कोमट पाणी पिल्याने पोट साफ होते का?",
                "इसबगोल सवय लागणारे औषध आहे का?",
                "वृद्धांमधील बद्धकोष्ठता घरगुती कशी आटोक्यात आणावी?"
            ]
        },

        # --- Category 4: Chronic Condition Support (8 topics) ---
        {
            "cat": "chronic_care",
            "ans": "🥗 **मधुमेह (डायबेटीस) नियंत्रणासाठी सुवर्ण नियम:**\n\n1. **आहारातील पथ्ये:** पांढरा भात, मैदा, साखर, बटाटा, गूळ आणि गोड शीतपेये पूर्णपणे टाळा. ज्वारी, बाजरी, नाचणीची भाकरी आणि भरपूर हिरव्या भाज्या खा.\n2. **सुरक्षित फळे:** पेरू, सफरचंद, जांभूळ, पपई मर्यादित प्रमाणात खा. आंबा, चिकू, द्राक्षे टाळा.\n3. **व्यायाम:** दररोज सकाळी किंवा संध्याकाळी किमान ४० मिनिटे जलद चाला.\n4. **पायांची काळजी (Foot Care):** पायाला बारीक जखम किंवा भेगा पडू देऊ नका. रोज पाय कोमट पाण्याने धुवून कोरडे करा आणि मऊ चप्पल वापरा.\n5. **लक्ष्य:** फास्टिंग ब्लड शुगर ८०-११० mg/dL आणि जेवणानंतर १४० mg/dL पेक्षा कमी ठेवण्याचा प्रयत्न करा.",
            "queries": [
                "शुगर (Diabetes) नियंत्रणात ठेवण्यासाठी रोजच्या आहारात काय बदल करावेत?",
                "मधुमेहाच्या रुग्णांनी कोणती फळे खावीत आणि कोणती टाळावीत?",
                "फास्टिंग शुगर किती असायला हवी?",
                "मधुमेहामध्ये पायांची काळजी कशी घ्यावी?",
                "चालण्याने रक्तातील साखर कशी कमी होते?",
                "डायबेटीस असलेल्या व्यक्तीला जखम भरून न आल्यास काय करावे?",
                "मधुमेहामध्ये इन्सुलिनची गरज कधी भासते?",
                "रक्तातील साखर अचानक खूप कमी (Hypoglycemia) झाल्यास काय खावे?",
                "मधुमेहाच्या रुग्णांनी नाचणीची भाकरी खाणे का चांगले असते?",
                "HbA1c टेस्ट किती महिन्यांनी करावी लागते?"
            ]
        },
        {
            "cat": "chronic_care",
            "ans": "❤️ **उच्च रक्तदाब (Hypertension) व्यवस्थापन:**\n\n1. **मिठावर नियंत्रण:** आहारात मिठाचे प्रमाण दिवसाला १ चमच्यापेक्षा कमी (५ ग्रॅम) ठेवा. लोणची, पापड, फरसाण, खारवलेले शेंगदाणे पूर्णपणे बंद करा.\n2. **सामान्य बीपी:** सामान्यतः १२०/८० mmHg हा आदर्श रक्तदाब मानला जातो. १४०/९० पेक्षा जास्त असल्यास तो उच्च रक्तदाब ठरतो.\n3. **औषधांची नियमितता:** डॉक्टरांनी लिहून दिलेली बीपीची गोळी एकाच वेळी रोज न चुकता घ्या. स्वतःहून गोळी बंद केल्यास ब्रेन हॅमरेज किंवा हार्ट अटॅकचा धोका उद्भवू शकतो.\n4. **प्राणायाम:** दररोज १५ मिनिटे अनुलोम-विलोम व भ्रामरी प्राणायाम करा, ज्यामुळे मज्जासंस्था शांत राहून रक्तदाब नियंत्रणात राहतो.",
            "queries": [
                "हाय ब्लड प्रेशर (BP) कमी करण्यासाठी काय उपाय आहेत?",
                "बीपीच्या रुग्णांनी आहारात मीठ किती खावे?",
                "रक्तदाब किती असल्यास सामान्य मानला जातो?",
                "उच्च रक्तदाबाची गोळी आयुष्यभर घ्यावी लागते का?",
                "तणाव आणि रागामुळे बीपी कसा वाढतो?",
                "बीपी वाढल्याची धोकिसची लक्षणे कोणती?",
                "बीपी नियंत्रणात ठेवण्यासाठी अनुलोम-विलोम प्राणायाम कसा करावा?",
                "लोणचे आणि पापड खाल्ल्याने बीपी का वाढतो?",
                "घरच्या घरी डिजिटल बीपी मशीनने रक्तदाब कसा मोजावा?",
                "रक्तदाब अचानक खूप कमी झाल्यास काय प्रथमोपचार करावा?"
            ]
        },
        {
            "cat": "chronic_care",
            "ans": "🦋 **हायपोथायरॉईड (Hypothyroidism) काळजी व जीवनशैली:**\n\n1. **गोळी घेण्याचा नियम:** थायरॉईडची गोळी (Thyronorm/Eltroxin) सकाळी उठल्याबरोबर **उपाशीपोटी १ ग्लास साध्या पाण्यासोबत** घ्यावी. त्यानंतर किमान ४५ मिनिटे चहा, कॉफी, नाश्ता काहीही घेऊ नये.\n2. **खाद्यपदार्थांचे पथ्य:** कोबी, फ्लॉवर, सोयाबीन, ब्रोकोली कच्चे खाणे टाळा (उकळून खाऊ शकता). आहारात आयोडिनयुक्त मीठ वापरा.\n3. **वजन व्यवस्थापन:** थायरॉईडमुळे चयापचय (Metabolism) मंदावतो, म्हणून दररोज एरोबिक व्यायाम किंवा चालणे आवश्यक आहे.\n4. **तपासणी:** दर ३ ते ६ महिन्यांनी रक्तातील TSH पातळी तपासून गोळीचा डोस ॲडजस्ट करून घ्यावा.",
            "queries": [
                "थायरॉईडच्या त्रासात वजन का वाढते आणि काय काळजी घ्यावी?",
                "थायरॉईडची गोळी सकाळी उठल्यावर कशी घ्यावी?",
                "थायरॉईडमध्ये काय खावे आणि काय खाऊ नये?",
                "थायरॉक्सिन गोळी घेतल्यावर चहा कधी प्यावा?",
                "TSH पातळी किती असावी?",
                "थायरॉईड असलेल्या महिलांना पाळीचा त्रास का होतो?",
                "हायपोथायरॉईड आणि हायपरथायरॉईडमध्ये काय फरक आहे?",
                "थायरॉईडमध्ये केस गळणे कसे थांबवावे?",
                "थायरॉईडची गोळी विसरल्यास त्याच दिवशी नंतर घ्यावी का?",
                "थायरॉईडच्या रुग्णांनी धन्याचे पाणी पिणे योग्य आहे का?"
            ]
        },
        {
            "cat": "chronic_care",
            "ans": "🫁 **दमा (अस्थमा) आणि श्वसनविकारावरील मार्गदर्शन:**\n\n1. **इनहेलरबद्दल गैरसमज:** इनहेलरची सवय लागत नाही! इनहेलर थेट फुफ्फुसांमध्ये जात असल्याने त्याचा डोस अतिशय सूक्ष्म (मायक्रोग्रॅममध्ये) असतो, त्यामुळे गोळ्यांपेक्षा इनहेलरचे दुष्परिणाम नगण्य असतात.\n2. **ट्रिगर्स टाळा:** धूळ, अगरबत्तीचा धूर, डासांची अगरबत्ती, तीव्र अत्तरे, थंड वारे आणि पाळीव प्राण्यांच्या केसांपासून दूर राहा.\n3. **झटका आल्यास:** रुग्णाला ताठ बसवा (झोपवू नका), सैल कपडे करा, आणि तातडीचा रिलीव्हर इनहेलर (उदा. Salbutamol) च्या २-४ पुफ्स स्पेसरद्वारे द्या.\n4. **हिवाळ्यात काळजी:** बाहेर पडताना नाकातोंडावर मफलर किंवा मास्क वापरा.",
            "queries": [
                "अस्थमा (दमा) रुग्णांनी हिवाळ्यात आणि धूळ-धुरात काय काळजी घ्यावी?",
                "इनहेलर वापरण्याची सवय लागते का?",
                "दम्याचा झटका आल्यास तात्काळ काय करावे?",
                "दम्यासाठी इनहेलर गोळ्यांपेक्षा अधिक सुरक्षित का मानला जातो?",
                "श्वास कोंडल्यावर कोणती पोझिशन आरामदायक असते?",
                "अस्थमा रुग्णांनी कोणत्या ॲलर्जी घटकांपासून सावध राहावे?",
                "दमा रुग्णांनी प्राणायाम करावा का?",
                "इनहेलर घेताना स्पेसर वापरणे का गरजेचे असते?",
                "छातीत घरघर आवाज आल्यास कोणता इनहेलर वापरावा?",
                "थंड पाणी किंवा आईस्क्रीम खाल्ल्याने दम्याचा त्रास का वाढतो?"
            ]
        },

        # --- Category 5: Pediatric & Child Healthcare Red Flags (6 topics) ---
        {
            "cat": "pediatric_guidance",
            "ans": "👶 **लहान मुलांमधील ताप (Pediatric Fever Care):**\n\n1. **सामान्य पाण्याचे स्पंजिंग:** बाळाचे कपडे सैल करा. सामान्य खोलीच्या तापमानाच्या पाण्यात सुती कापड भिजवून मान, काखा आणि कपाळ हळुवार पुसा. (कधीही बर्फाचे किंवा अतिथंड पाणी वापरू नका).\n2. **ओव्हर-ड्रेसिंग टाळा:** बाळाला जाड पांघरुणात किंवा स्वेटरमध्ये गुंडाळून ठेवू नका, यामुळे शरीराचे तापमान अजून वाढते.\n3. **द्रवपदार्थ:** बाळाला स्तनपान किंवा कोमट पाणी, सूप थोड्या थोड्या अंतराने पाजत राहा.\n\n🚨 **धोक्याची चिन्हे (Red Flags):** बाळ सुस्त पडून प्रतिसाद देत नसल्यास, अंग ताठ होऊन आचके/फेफरे आल्यास, किंवा बाळ सतत रडत असल्यास एका मिनिटाचाही विलंब न करता बालरोगतज्ज्ञांकडे (Pediatrician) धाव घ्या.",
            "queries": [
                "बाळाला खूप ताप आला आहे, घरी काय करावे?",
                "लहान मुलांचा ताप आल्यावर अंगावर पाण्याच्या घड्या कशा ठेवाव्यात?",
                "तापात लहान मुलाचे हातपाय थरथरल्यास (आचके आल्यास) काय करावे?",
                "लहान मुलांना बर्फाच्या पाण्याने पुसणे योग्य आहे का?",
                "लहान मुलांचा ताप कधी धोकादायक मानला जातो?",
                "बाळाला तापात पॅरासिटामॉल ड्रॉप्स किती थेंब द्यावे?",
                "तापात बाळाचे पाय थंड आणि डोके गरम का लागते?",
                "लहान मुलांच्या तापावर घरगुती काय काढा किंवा लेप लावावा का?",
                "लसीकरणानंतर बाळाला ताप आल्यास काय काळजी घ्यावी?",
                "बाळ सुस्त पडल्यास बालरोगतज्ज्ञांकडे तात्काळ कधी जावे?"
            ]
        },
        {
            "cat": "pediatric_guidance",
            "ans": "🍼 **लहान मुलांमधील जुलाब व डिहायड्रेशन प्रतिबंध:**\n\n1. **स्तनपान सुरू ठेवा:** ६ महिन्यांपेक्षा लहान बाळाला आईचे दूध पाजणे सुरू ठेवा, ते बाळाचे सर्वोत्तम औषध आहे.\n2. **ORS आणि झिंक:** डॉक्टरांच्या सल्ल्याने ओआरएसचे पाणी चमचा-चमच्याने पाजा. १४ दिवस झिंक सिरप दिल्यास आतड्यांची झीज भरून येते आणि पुढील जुलाब टळतात.\n3. **डिहायड्रेशनची लक्षणे:** टाळू खोल जाणे, डोळे खोल जाणे, रडताना डोळ्यातून अश्रू न येणे, जीभ सुकणे आणि ६ तासांपेक्षा जास्त वेळ लघवी न होणे ही गंभीर डिहायड्रेशनची लक्षणे आहेत.\n4. **स्वच्छता:** बाळाची खेळणी, बाटली आणि आईचे हात नेहमी साबणाने स्वच्छ धुवावेत.",
            "queries": [
                "लहान बाळ सतत पातळ संडास करत आहे, काय काळजी घ्यावी?",
                "बाळामध्ये डिहायड्रेशन (पाण्याची कमतरता) कशी ओळखावी?",
                "मुलांना जुलाब झाल्यावर झिंक सिरप देणे का गरजेचे असते?",
                "लहान मुलांना ओआरएस पाणी कसे पाजावे?",
                "जुलाब झालेल्या बाळाला दूध पाजणे चालू ठेवावे का?",
                "लहान मुलांच्या जुलाबात रक्त आल्यास काय करावे?",
                "बाळाची टाळू खोल जाणे हे कशाचे लक्षण आहे?",
                "लहान मुलांचे जुलाब घरगुती कसे आटोक्यात आणावेत?",
                "जुलाबामध्ये बाळाला तांदळाची मऊ पेज देणे योग्य आहे का?",
                "बाळाने ६ तास लघवी केली नाही तर काय समजायचे?"
            ]
        },
        {
            "cat": "pediatric_guidance",
            "ans": "🧸 **लहान मुलांमधील सर्दी-खोकला व नाक चोंदणे:**\n\n1. **सलाईन ड्रॉप्स (Saline Nasal Drops):** बाळाचे नाक चोंदल्यास दोन्ही नाकपुड्यांमध्ये २-२ थेंब साध्या सलाईनचे थेंब टाका. यामुळे कफ मऊ होऊन नाक मोकळे होते. विशेषतः दूध पाजण्यापूर्वी हे करावे.\n2. **डोके थोडे उंचावर ठेवा:** झोपताना बाळाच्या डोक्याखाली पातळ घडी ठेवा.\n3. **मध नियम:** **१ वर्षाखालील बाळाला कधीही मध देऊ नये** (बोट्युलिझमचा धोका असतो). १ वर्षानंतरच मध देणे सुरक्षित असते.\n\n🚨 जर बाळाच्या बरगड्या आत ओढल्या जात असतील (श्वास घेताना पोट खूप खचत असेल), नाकपुड्या थरथरत असतील तर तातडीने ऑक्सिजन व वैद्यकीय मदतीची गरज असते.",
            "queries": [
                "बाळाचे नाक चोंदल्यामुळे बाळ दूध पीत नाही, काय करावे?",
                "लहान मुलांसाठी नेझल सलाईन ड्रॉप्स सुरक्षित आहेत का?",
                "बाळाच्या छातीतून घरघर आवाज येत असल्यास काय करावे?",
                "लहान मुलांचा कफ घरगुती कसा मोकळा करावा?",
                "बाळाला रात्री श्वास घेताना त्रास होत आहे, काय उपाय आहे?",
                "मुलांच्या खोकल्यावर मध देणे कितव्या महिन्यापासून सुरक्षित आहे?",
                "बाळाच्या छातीवर ओवा-कापूरची पुरचुंडी शेकणे योग्य आहे का?",
                "लहान मुलांमध्ये न्युमोनियाची लक्षणे कशी ओळखावीत?",
                "बाळाला वाफ देताना काय दक्षता घ्यावी लागते?",
                "खोकल्यामुळे बाळाला उलटी का होते?"
            ]
        },

        # --- Category 6: Women's Health & Maternal Care (4 topics) ---
        {
            "cat": "womens_health",
            "ans": "🌸 **मासिक पाळीतील पोटदुखी (Dysmenorrhea) निवारण:**\n\n1. **गरम पाण्याचा शेक:** ओटीपोटावर आणि कंबरेवर गरम पाण्याच्या पिशवीने (Hot water bag) शेक दिल्यास गर्भाशयाचे स्नायू शिथिल होऊन कळा कमी होतात.\n2. **कोमट पाणी व आले चहा:** कोमट पाणी भरपूर प्या. आल्याचा चहा किंवा ओव्याचे पाणी पिल्याने क्रॅम्प्स कमी होतात.\n3. **हलके स्ट्रेचिंग:** हलके चालणे किंवा बटरफ्लाय आसन (Baddha Konasana) केल्याने पेल्विक भागातील रक्ताभिसरण सुधारते.\n4. **लोहयुक्त आहार:** पाळीच्या दिवसांत रक्तस्त्राव होत असल्याने खजूर, बीट, गूळ-शेंगदाणे आणि हिरव्या पालेभाज्या भरपूर खा.\n\n⚠️ वेदना खूप तीव्र असतील आणि दैनंदिन कामे करणे अशक्य होत असेल तर एंडोमेट्रिओसिस किंवा सिस्टची तपासणी करण्यासाठी स्त्रीरोगतज्ज्ञांना (Gynecologist) भेटा.",
            "queries": [
                "मासिक पाळीच्या वेळी पोटात असह्य कळा येतात, काय उपाय करावा?",
                "पाळीच्या पोटदुखीवर गरम पाण्याच्या पिशवीने शेकणे फायदेशीर आहे का?",
                "मासिक पाळीत जास्त रक्तस्त्राव आणि अशक्तपणा आल्यास काय करावे?",
                "पाळी नियमित येण्यासाठी काय खावे?",
                "पाळीच्या दिवसांत कोणती औषधे किंवा गोळ्या सुरक्षित असतात?",
                "मासिक पाळीतील वेदनांसाठी कोणते घरगुती उपाय चांगले आहेत?",
                "पाळीच्या दिवसांत मेफ्टाल-स्पास गोळी घेणे सुरक्षित आहे का?",
                "अनियमित पाळीवर ओव्याचा काढा कसा गुणकारी ठरतो?",
                "पाळीमध्ये पाय आणि कंबर खूप का दुखतात?",
                "पाळीच्या दिवसांत जड व्यायाम करावा का?"
            ]
        },
        {
            "cat": "womens_health",
            "ans": "🤰 **गरोदरपणातील पहिली तिमाही (First Trimester) काळजी:**\n\n1. **मॉर्निंग सिकनेसवर उपाय:** सकाळी उठल्याबरोबर एकदम पाणी पिण्याऐवजी कोरडा टोस्ट किंवा खाकरा खावा. दिवसातून ३ मोठे जेवण घेण्याऐवजी २-२ तासांनी लहान हलके आहार घ्यावेत.\n2. **फॉलिक ॲसिड:** बाळाच्या मेंदूच्या आणि मणक्याच्या विकासासाठी पहिल्या ३ महिन्यांत फॉलिक ॲसिडची गोळी रोज वेळेवर घेणे अत्यंत आवश्यक आहे.\n3. **काय टाळावे:** कच्ची पपई, अननस, जड वजन उचलणे, लांबचा खडतर प्रवास आणि स्वतःहून कोणतीही ॲलोपॅथिक औषधे घेणे टाळा.\n4. **सोनोग्राफी:** बाळाचे ठोके आणि गर्भ पिशवीत योग्य ठिकाणी आहे का हे तपासण्यासाठी ६ ते ८ आठवड्यांत पहिली सोनोग्राफी करून घ्यावी.",
            "queries": [
                "गरोदरपणात सकाळी खूप मळमळ आणि उलटी होते (Morning Sickness), काय करावे?",
                "गरोदरपणाच्या पहिल्या तीन महिन्यांत कोणती काळजी घ्यावी?",
                "गरोदरपणात फॉलिक ॲसिडची गोळी का दिली जाते?",
                "गरोदर स्त्रीने कोणता आहार घ्यावा आणि काय टाळावे?",
                "गरोदरपणात पपई किंवा अननस खाणे का टाळावे?",
                "गरोदरपणात डॉक्टरांकडे नियमित कधी जावे?",
                "गरोदर स्त्रीने डाव्या कुशीवर झोपणे का महत्त्वाचे असते?",
                "गरोदरपणात पोटात बारीक बारीक दुखल्यास काय करावे?",
                "गरोदरपणातील रक्तदाब (BP) आणि हिमोग्लोबिन तपासणी कधी करावी?",
                "गरोदरपणात मळमळ थांबवण्यासाठी आल्याचा तुकडा चघळावा का?"
            ]
        },

        # --- Category 7: Emergency Triage, First Aid & Accidents (6 topics) ---
        {
            "cat": "emergency_triage",
            "ans": "🚨 **हृदयविकाराचा झटका (Heart Attack) - आणीबाणी मार्गदर्शक:**\n\n1. **लक्षणे:** छातीच्या मधोमध किंवा डाव्या बाजूला असह्य दाब किंवा जड वजन ठेवल्यासारखे वाटणे, ही कळ डाव्या हाताकडे, जबड्याकडे किंवा पाठीकडे जाणे, आणि दरदरून थंड घाम सुटणे.\n2. **त्वरित कृती:**\n   - त्वरित **१०८ किंवा ११२** नंबरवर फोन करून कार्डिॲक रुग्णवाहिका बोलवा.\n   - रुग्णाला अजिबात चालू किंवा हालचाल करू देऊ नका; त्यांना अर्धवट बसलेल्या स्थितीत (W-position) शांत ठेवा.\n   - उपलब्ध असल्यास डॉक्टरांच्या पूर्वपरवानगीनुसार ३०० मिग्रॅ अस्पिरीन (Aspirin) ची गोळी चावून खायला द्यावी.\n3. **स्वतः गाडी चालवू नका:** रुग्णाला स्वतः दुचाकी किंवा गाडी चालवून नेण्याचा धोका पत्करू नका.",
            "queries": [
                "हार्ट अटॅकची मुख्य लक्षणे कोणती असतात?",
                "छातीत डाव्या बाजूला दाब येऊन घाम फुटल्यास काय करावे?",
                "हार्ट अटॅक आल्यावर प्राथमिक उपचार काय करावा?",
                "हार्ट अटॅकच्या रुग्णाला कोणती गोळी तात्काळ द्यावी?",
                "रुग्णवाहिकेशी संपर्क करण्यासाठी कोणता नंबर डायल करावा?",
                "हार्ट अटॅक आणि ॲसिडिटीच्या छातीतील वेदनांमध्ये कसा फरक ओळखायचा?",
                "हार्ट अटॅक आल्यावर रुग्णाला कसे बसवावे?",
                "हार्ट अटॅक आल्यावर अस्पिरीन गोळी चावून का खावी लागते?",
                "हृदयाचे ठोके अचानक खूप वाढल्यास काय करावे?",
                "छातीत दुखत असताना रुग्णाला पाणी पाजावे का?"
            ]
        },
        {
            "cat": "emergency_triage",
            "ans": "🧠 **पक्षाघात (Brain Stroke) ओळखण्याचा 'FAST' फॉर्म्युला:**\n\n- **F (Face - चेहरा):** रुग्णाला हसायला सांगा; चेहऱ्याची किंवा तोंडाची एक बाजू वाकडी झाली आहे का?\n- **A (Arms - हात):** दोन्ही हात वर करायला सांगा; एक हात खाली पडतोय किंवा लुळा पडला आहे का?\n- **S (Speech - बोलणे):** साधे वाक्य बोलायला सांगा; बोलणे अडखळतेय किंवा बोबडे झाले आहे का?\n- **T (Time - वेळ):** वरीलपैकी एकही लक्षण दिसल्यास त्वरित **'गोल्डन अवर' (साडेचार तासांच्या आत)** स्ट्रोक-सज्ज रुग्णालयात पोहोचवा!\n\n⚠️ **सावधान:** रुग्णाला काहीही खाऊ किंवा पिऊ घालू नका (घशाचे स्नायू लुळे पडल्याने पाणी फुफ्फुसात जाऊन गुदमरण्याचा धोका असतो).",
            "queries": [
                "पक्षाघात (Stroke / Paralysis) चे झटके कसे ओळखावेत?",
                "स्ट्रोक ओळखण्यासाठी FAST नियम काय आहे?",
                "एका बाजूचे तोंड वाकडे झाल्यास काय करावे?",
                "स्ट्रोक आल्यावर किती तासांच्या आत रुग्णालयात पोहोचणे आवश्यक असते?",
                "पक्षाघाताच्या रुग्णाला पाणी पाजावे का?",
                "अचानक बोलणे बोबडे पडल्यास काय करावे?",
                "पक्षाघाताच्या झटक्यात हातातील ताकद जाणे कशाचे लक्षण आहे?",
                "स्ट्रोक रुग्णाला त्वरित सीटी स्कॅन (CT Scan) करणे का गरजेचे असते?",
                "गोल्डन अवर म्हणजे काय आणि त्याचे काय महत्त्व आहे?",
                "स्ट्रोक आल्यावर डोक्याला मालिश करावी का?"
            ]
        },
        {
            "cat": "emergency_triage",
            "ans": "🔥 **भाजल्यावरील (Burns First Aid) शास्त्रीय प्रथमोपचार:**\n\n1. **वाहते पाणी:** भाजलेल्या भागावर किमान १५ ते २० मिनिटे नळाचे साधे थंड पाणी हळुवार ओता. (बर्फ लावू नका, बर्फामुळे ऊतींचे अधिक नुकसान होते).\n2. **काय करू नका:** भाजलेल्या त्वचेवर टूथपेस्ट, तूप, तेल, शाई किंवा हळद कधीही लावू नका; यामुळे गंभीर इन्फेक्शन होते.\n3. **फोड फोडू नका:** भाजल्यामुळे त्वचेवर आलेले पाण्याचे फोड नैसर्गिक संरक्षण असतात, ते मुद्दाम फोडू नका.\n4. **आच्छादन:** स्वच्छ, निर्जंतुक कोरड्या कापडाने हलके झाका आणि सिल्व्हर सल्फाडायझिन (Silver Sulfadiazine) मलम लावा.\n5. जर चेहरा, हात किंवा जननेंद्रिये भाजली असतील तर त्वरित बर्न वॉर्ड असलेल्या रुग्णालयात जा.",
            "queries": [
                "गरम पाणी किंवा तेल अंगावर सांडून भाजल्यास तात्काळ काय करावे?",
                "भाजलेल्या जागेवर टूथपेस्ट किंवा शाई लावणे योग्य आहे का?",
                "भाजल्यामुळे उठलेले फोड फोडावेत का?",
                "भाजलेल्या त्वचेवर कोणता मलम लावावा?",
                "गंभीररीत्या भाजलेल्या रुग्णावर काय प्रथमोपचार करावा?",
                "भाजल्यावर बर्फ लावावा की साधे पाणी?",
                "दिवाळीत फटाक्याने हात भाजल्यास तात्काळ काय करावे?",
                "भाजलेल्या त्वचेवर हळद किंवा तूप का लावू नये?",
                "भाजलेली जागा स्वच्छ कापडाने झाकणे का गरजेचे असते?",
                "केमिकलने (ॲसिडने) त्वचा भाजल्यास काय प्रथमोपचार करावा?"
            ]
        },
        {
            "cat": "emergency_triage",
            "ans": "🐍 **सर्पदंश आणि विंचूदंश - जीवनरक्षक नियम:**\n\n1. **रुग्णाला शांत ठेवा:** घाबरल्यामुळे हृदयाचे ठोके वाढून विष शरीरात वेगाने पसरते. रुग्णाला हालचाल करू देऊ नका.\n2. **काय करू नये (Strict Don'ts):**\n   - दंशाच्या ठिकाणी ब्लेडने कापू नका किंवा तोंड लावून रक्त शोषू नका.\n   - अतिघट्ट दोरी किंवा वायरने आवळू नका (यामुळे पाय सडण्याची शक्यता निर्माण होते).\n3. **काय करावे (Do's):** जखम साबण-पाण्याने स्वच्छ धुवा, अवयव हलका स्थिर ठेवा (उदा. लाकडी पट्टी बांधून).\n4. **तातडीने सरकारी रुग्णालयात जा:** सर्व सरकारी उपजिल्हा रुग्णालयांमध्ये **Anti-Snake Venom (ASV)** मोफत उपलब्ध असते. मांत्रिक किंवा झाडफूक करणाऱ्यांकडे जाऊन वेळ वाया घालवू नका.",
            "queries": [
                "साप चावल्यास सर्वात आधी काय केले पाहिजे?",
                "सापाच्या चाव्यावर ब्लेडने कापून विष काढण्याचा प्रयत्न करावा का?",
                "साप चावलेल्या पायावर घट्ट दोरी बांधणे योग्य आहे का?",
                "विंचू चावल्यावर काय प्रथमोपचार करावा?",
                "सापाच्या विषाविरुद्धचे औषध (Anti-Snake Venom) कुठे मिळते?",
                "साप चावलेल्या व्यक्तीला जागे का ठेवावे लागते?",
                "सापाचा दंश विषारी आहे की बिनविषारी हे कसे ओळखायचे?",
                "सर्पदंशावर घरगुती झाडफूक करण्याने काय धोके होतात?",
                "विंचवाच्या डंखावर बर्फ लावणे योग्य आहे का?",
                "साप चावल्यानंतर रुग्णाला धावू का देऊ नये?"
            ]
        },

        # --- Category 8: Seasonal & Infectious Illnesses (4 topics) ---
        {
            "cat": "symptom_guidance",
            "ans": "🦟 **डेंग्यू ताप (Dengue Care Guidelines):**\n\n1. **लक्षणे:** तीव्र ताप, डोळ्यांच्या मागे असह्य दुखणे, सांधेदुखी आणि अंगावर बारीक लाल पुरळ.\n2. **पाणी व हायड्रेशन:** डेंग्यूचा खरा उपचार म्हणजे भरपूर हायड्रेशन! दिवसभरात ३-४ लिटर पाणी, ओआरएस, सूप आणि शहाळ्याचे पाणी प्या.\n3. **महत्त्वाचा इशारा (No Brufen/Aspirin):** डेंग्यूमध्ये केवळ **पॅरासिटामॉल** वापरावी. ब्रुफेन, डिक्लोफेनॅक किंवा अस्पिरीन कधीही घेऊ नका, यामुळे अंतर्गत रक्तस्त्रावाचा धोका निर्माण होतो.\n4. **धोक्याची चिन्हे:** हिरड्यांतून रक्त येणे, पोटात असह्य दुखणे, सतत उलट्या किंवा काळी शौचास होणे ही धोक्याची लक्षणे आहेत, अशी लक्षणे दिसताच त्वरित रुग्णालयात दाखल व्हा.",
            "queries": [
                "डेंग्यू ताप आल्याची लक्षणे कोणती असतात?",
                "डेंग्यूमध्ये प्लेटलेट्स कमी झाल्यास काय करावे?",
                "डेंग्यूच्या रुग्णाला पपईच्या पानांचा रस देणे योग्य आहे का?",
                "डेंग्यूमध्ये कोणता पेनकिलर कधीही घेऊ नये?",
                "डेंग्यू तापामध्ये धोक्याची चिन्हे कोणती असतात?",
                "डेंग्यूचा डास कोणत्या वेळी चावतो?",
                "डेंग्यूच्या रुग्णाने किती पाणी प्यावे?",
                "डेंग्यूमध्ये डोळ्यांच्या मागे दुखणे का होते?",
                "प्लेटलेट काउंट ५०,००० च्या खाली आल्यास काय करावे?",
                "डेंग्यू बरा होण्यासाठी किती दिवस लागतात?"
            ]
        },
        {
            "cat": "symptom_guidance",
            "ans": "🟡 **कावीळ (Jaundice) आणि यकृताची (Liver) काळजी:**\n\n1. **लक्षणे:** डोळे आणि नखे पिवळी होणे, लघवी गडद पिवळी होणे, भूक मंदावणे, मळमळ आणि अत्यंत थकवा.\n2. **आहाराचे पथ्य:**\n   - तेलकट, तूपकट आणि तळलेले पदार्थ पूर्णपणे बंद करा, कारण आजारी यकृत चरबी पचवू शकत नाही.\n   - हलका आहार घ्या: उकडलेला भात, लापशी, मुगाची डाळ, फळे आणि उसाचा स्वच्छ रस.\n3. **पाणी:** नेहमी उकळून थंड केलेलेच पाणी प्या, बाहेरचे उघड्यावरील अन्न खाऊ नका.\n4. **विश्रांती:** यकृताच्या पेशी पुन्हा पूर्ववत होण्यासाठी पूर्ण शारीरिक विश्रांती अत्यंत आवश्यक असते.\n5. **तपासणी:** सीरम बिलीरुबिन (Serum Bilirubin) आणि SGPT/SGOT रक्त तपासणी करून डॉक्टरांच्या सल्ल्याने उपचार घ्या.",
            "queries": [
                "कावीळ (Jaundice) झाल्यास आहारात काय पथ्ये पाळावीत?",
                "काविळीची मुख्य लक्षणे कोणती आहेत?",
                "डोळे आणि लघवी पिवळी झाल्यास काय तपासणी करावी?",
                "काविळीच्या रुग्णाने गोड पदार्थ खावे की नाही?",
                "दूषित पाण्यामुळे होणारी कावीळ कशी टाळावी?",
                "कावीळ बरी होण्यासाठी किती दिवस लागतात?",
                "काविळीमध्ये तेलकट आणि तळलेले अन्न का खाऊ नये?",
                "कावीळ झालेल्या व्यक्तीने उसाचा रस पिणे कितपत योग्य आहे?",
                "लहान मुलांमधील कावीळ कशी ओळखावी?",
                "बिलीरुबिन वाढल्यास डॉक्टरांकडे त्वरित का जावे?"
            ]
        },

        # --- Category 9: Diagnostics & Laboratory Tests (4 topics) ---
        {
            "cat": "general_health",
            "ans": "🔬 **सीबीसी (Complete Blood Count) रिपोर्ट कसा समजून घ्यावा:**\n\n1. **हिमोग्लोबिन (Hb):** पुरुषांमध्ये १३ ते १७ gm/dL आणि महिलांमध्ये १२ ते १५ gm/dL हे सामान्य प्रमाण आहे. यापेक्षा कमी असल्यास ॲनिमिया दर्शवतो.\n2. **पांढऱ्या पेशी (WBC):** ४,००० ते ११,००० प्रति मायक्रोलिटर. शरीरात कोणताही संसर्ग (Infection) किंवा सूज असल्यास WBC काउंट वाढतो.\n3. **प्लेटलेट्स (Platelets):** १.५ लाख ते ४.५ लाख प्रति मायक्रोलिटर. डेंग्यू, मलेरिया किंवा व्हायरल इन्फेक्शनमध्ये हा काउंट कमी होऊ शकतो.\n4. **तयारी:** सामान्य सीबीसी तपासणीसाठी उपाशीपोटी राहण्याची सक्ती नसते, दिवसाच्या कोणत्याही वेळी रक्त देता येते.",
            "queries": [
                "सीबीसी (CBC) टेस्ट रिपोर्टमध्ये काय काय तपासले जाते?",
                "रक्तातील पांढऱ्या पेशी (WBC) वाढल्याचा अर्थ काय होतो?",
                "हिमोग्लोबिनची सामान्य पातळी किती असावी?",
                "प्लेटलेट काउंट किती असणे निरोगी शरीराचे लक्षण आहे?",
                "सीबीसी टेस्ट करण्यासाठी उपाशीपोटी जावे लागते का?",
                "रक्तातील संसर्ग कसा ओळखायचा?",
                "सीबीसी रिपोर्टमध्ये ॲनिमिया कसा समजून घ्यावा?",
                "डब्ल्यूबीसी कमी झाल्याने रोगप्रतिकारशक्तीवर काय परिणाम होतो?",
                "प्लेटलेट्स २ लाखांपेक्षा जास्त असणे चांगले असते का?",
                "ताप आल्यावर डॉक्टर सीबीसी करायला का सांगतात?"
            ]
        },
        {
            "cat": "chronic_care",
            "ans": "🩸 **शुगर चाचण्या (Blood Sugar Tests) मार्गदर्शक:**\n\n1. **फास्टिंग शुगर (Fasting):** यासाठी रात्रीच्या जेवणानंतर किमान ८ ते १० तास काहीही न खाता पाणी पिऊन सकाळी रक्त द्यावे. ७० ते ९९ mg/dL ही सामान्य श्रेणी आहे.\n2. **पोस्ट-प्रँडियल (PP):** दुपारचे जेवण सुरू केल्याच्या बरोबर २ तासांनी ही तपासणी करावी. १४० mg/dL पेक्षा कमी असणे चांगले.\n3. **HbA1c (३ महिन्यांची सरासरी):**\n   - ५.७% पेक्षा कमी: सामान्य\n   - ५.७% ते ६.४%: प्री-डायबेटीस (सावधगिरीची वेळ)\n   - ६.५% किंवा जास्त: मधुमेह.\nडायबेटीसच्या रुग्णांनी HbA1c ७% च्या आत ठेवणे आवश्यक असते.",
            "queries": [
                "फास्टिंग शुगर टेस्टसाठी किती तास उपाशी राहावे लागते?",
                "HbA1c टेस्ट म्हणजे काय आणि ती का केली जाते?",
                "जेवणानंतरची शुगर (Post-Prandial) कधी मोजावी?",
                "HbA1c रिपोर्ट किती असल्यास मधुमेह नियंत्रणात मानला जातो?",
                "घरच्या घरी ग्लुकोमीटरने शुगर मोजताना काय काळजी घ्यावी?",
                "प्री-डायबेटीस म्हणजे काय?",
                "फास्टिंग शुगर १२० आल्यास मधुमेह आहे का?",
                "शुगर मोजण्यापूर्वी गोळी घ्यावी की नंतर?",
                "रक्तातील साखरेची चढ-उतार कशी रोखावी?",
                "HbA1c चाचणी वर्षातून किती वेळा करावी?"
            ]
        },

        # --- Category 10: Everyday Conversational Patient Queries (6 topics) ---
        {
            "cat": "patient_dialogue",
            "ans": "जर तुम्ही नियमित औषधाची गोळी घ्यायला विसरलात, तर खालील नियम पाळा:\n\n1. **लगेच आठवल्यास:** जर थोडाच वेळ झाला असेल, तर आठवताच गोळी घेऊन घ्या.\n2. **पुढच्या डोसची वेळ जवळ असल्यास:** जर पुढील गोळीची वेळ जवळ आली असेल (उदा. २-३ तासांवर), तर विसरलेली गोळी सोडून द्या आणि ठरलेल्या वेळी पुढचा नियमित डोस घ्या.\n3. **दुप्पट गोळी घेऊ नका:** विसरलेली भरपाई करण्यासाठी एकाच वेळी दोन गोळ्या कधीही घेऊ नका, यामुळे दुष्परिणाम होऊ शकतात.",
            "queries": [
                "डॉक्टर, औषध घेताना गोळीचा डोस विसरला तर काय करावे?",
                "बीपी किंवा शुगरची गोळी वेळेवर घ्यायला विसरलो, आता काय करू?",
                "दुपारची गोळी रात्री आठवल्यास दोन गोळ्या एकत्र घ्याव्यात का?",
                "गोळी विसरल्याने काही धोका होतो का?",
                "औषधांची वेळ लक्षात ठेवण्यासाठी काय सोपी युक्ती करावी?",
                "नियमित गोळी चुकल्यास शरीरावर काय परिणाम होतो?",
                "अँटिबायोटिकचा डोस विसरल्यास काय काळजी घ्यावी?",
                "गोळी घेण्याची वेळ पुढे-मागे झाली तर चालते का?"
            ]
        },
        {
            "cat": "patient_dialogue",
            "ans": "नाही, औषधांच्या गोळ्या कधीही चहा, कॉफी, दूध किंवा शीतपेयांसोबत घेऊ नयेत:\n\n1. चहा-कॉफीमधील 'कॅफिन' आणि 'टॅनिन' घटकांमुळे औषधांमधील रसायनांचे शोषण रोखले जाते.\n2. दुधातील कॅल्शियममुळे अँटिबायोटिक्स आणि लोहाचे शोषण कमी होते.\n3. औषधे नेहमी **सामान्य साध्या पाण्यासोबतच** घेणे सर्वात सुरक्षित आणि परिणामकारक ठरते.",
            "queries": [
                "औषधांच्या गोळ्या चहा किंवा कॉफीसोबत घेणे चालते का?",
                "गोळी दुधासोबत का घेऊ नये?",
                "चहासोबत पॅरासिटामॉल घेतल्यास काही दुष्परिणाम होतात का?",
                "गोळी घेण्यासाठी गरम पाणी वापरावे की साधे पाणी?",
                "कोल्ड्रिंक किंवा ज्यूससोबत औषध घेणे सुरक्षित आहे का?",
                "गोळी घेताना किती पाणी प्यावे?",
                "गोळी गिळताना घशात अडकल्यासारखे वाटल्यास काय करावे?",
                "गोळी अर्धी तोडून घेतली तर औषधाचा प्रभाव कमी होतो का?"
            ]
        },
        {
            "cat": "patient_dialogue",
            "ans": "अजिबात नाही! अँटिबायोटिक्सचा कोर्स अर्धवट सोडणे अत्यंत धोक्याचे आहे:\n\n1. **बॅक्टेरियल रेझिस्टन्स:** ३ दिवसांत लक्षणे बरी झाली तरी शरीरातील काही जिवाणू जिवंत असतात. गोळ्या थांबवल्यास ते औषधांना दाद न देणारे (ड्रग-रेझिस्टंट) बनतात.\n2. **आजार उलटणे:** आजार पुन्हा अधिक तीव्रतेने उलटून येतो.\n3. म्हणून डॉक्टरांनी सांगितलेला संपूर्ण ५ किंवा ७ दिवसांचा कोर्स पूर्ण करणे अनिवार्य आहे.",
            "queries": [
                "मला अँटिबायोटिकचा ५ दिवसांचा कोर्स दिला होता, ३ दिवसांत बरे वाटले तर गोळ्या बंद करू का?",
                "अँटिबायोटिक्सचा कोर्स अर्धवट सोडल्याने काय नुकसान होते?",
                "औषधाने बरे वाटू लागले तरी गोळ्या सुरू ठेवाव्यात का?",
                "अँटिबायोटिक रेझिस्टन्स म्हणजे काय?",
                "डॉक्टरांनी सांगितलेले दिवस पूर्ण का करावे लागतात?",
                "अँटिबायोटिक गोळ्यांमुळे पोटात उष्णता किंवा जुलाब का होतात?",
                "अँटिबायोटिक्ससोबत बी-कॉम्प्लेक्स का दिली जाते?",
                "वारंवार अँटिबायोटिक्स घेतल्याने प्रतिकारशक्ती कमी होते का?"
            ]
        },
        {
            "cat": "general_health",
            "ans": "रात्री उशिरा जेवल्याने पचनाचे अनेक त्रास होतात:\n\n1. **ॲसिड रिफ्लक्स (GERD):** जेवल्यानंतर लगेच झोपल्यास पोटातील ॲसिड अन्ननलिकेत उलट येते, ज्यामुळे छातीत तीव्र जळजळ होते.\n2. **वजन वाढणे:** रात्री चयापचय मंदावल्याने अन्नातील उष्मांक चरबीच्या रूपात साठवले जातात.\n3. **अपुरी झोप:** पचनक्रिया सुरू राहिल्याने गाढ झोप लागत नाही.\n4. **नियम:** झोपण्याच्या किमान २ ते ३ तास आधी हलके जेवण पूर्ण करा.",
            "queries": [
                "रात्री उशिरा जेवल्याने पचनसंस्थेवर काय वाईट परिणाम होतात?",
                "जेवण आणि झोप यामध्ये किती तासांचे अंतर असावे?",
                "रात्री जेवल्यानंतर छातीत जळजळ का होते?",
                "उशिरा जेवल्यामुळे वजन वाढते का?",
                "रात्री हलका आहार का घ्यावा?",
                "जेवणानंतर शतपावली (१०० पावले चालणे) का करावी?",
                "जेवल्यानंतर लगेच पाणी पिणे योग्य आहे का?",
                "रात्री झोपण्यापूर्वी दूध पिण्याची योग्य वेळ कोणती?"
            ]
        },
        {
            "cat": "general_health",
            "ans": "प्रौढ व्यक्तीसाठी पाणी पिण्याचे योग्य प्रमाण:\n\n1. **प्रमाण:** साधारणपणे दिवसाला २.५ ते ३.५ लिटर (८ ते १० मोठे ग्लास) पाणी पुरेसे असते.\n2. **हवामान व श्रम:** उन्हाळ्यात किंवा कष्टाचे काम करत असल्यास घामावाटे पाणी निघून जात असल्याने ४ लिटरपर्यंत पाणी प्यावे.\n3. **ओळख:** तुमची लघवी हलकी पारदर्शक किंवा फिकट पिवळी असल्यास शरीरातील पाण्याचे प्रमाण योग्य आहे हे समजते.\n4. **हृदय किंवा किडनीचे रुग्ण:** किडनी किंवा हृदयाचे आजार असलेल्या रुग्णांनी डॉक्टरांनी ठरवून दिलेल्या मर्यादेतच पाणी प्यावे.",
            "queries": [
                "निरोगी राहण्यासाठी दिवसाला किती पाणी पिणे आवश्यक आहे?",
                "दिवसभरात किती लिटर पाणी प्यावे?",
                "पाणी कमी पिल्याने कोणकोणते आजार होऊ शकतात?",
                "शरीरात पाण्याचे प्रमाण योग्य आहे हे कसे ओळखावे?",
                "उन्हाळ्यात डिहायड्रेशन टाळण्यासाठी किती पाणी प्यावे?",
                "एकदम जास्त पाणी पिणे शरीरासाठी चांगले असते का?",
                "सकाळी रिकाम्या पोटी कोमट पाणी पिण्याचे काय फायदे आहेत?",
                "किडनी रुग्णांनी पाणी पिताना काय मर्यादा पाळावी?"
            ]
        },
        {
            "cat": "general_health",
            "ans": "शांत व गाढ झोप (Deep Sleep) लागण्यासाठी खालील सवयी लावा:\n\n1. **स्क्रीन टाइम बंद:** झोपण्यापूर्वी किमान १ तास आधी मोबाईल, लॅपटॉप किंवा टीव्हीचा वापर बंद करा (निळ्या प्रकाशामुळे मेलाटोनिन संप्रेरक कमी होते).\n2. **नियमित वेळ:** दररोज एकाच वेळी झोपण्याची आणि उठण्याची शिस्त ठेवा.\n3. **कॅफिन टाळा:** संध्याकाळी ६ नंतर चहा, कॉफी किंवा एनर्जी ड्रिंक्स पिणे टाळा.\n4. **शांत वातावरण:** बेडरूम अंधारी, शांत आणि थंड ठेवावी.\n5. **दीर्घ श्वसन:** झोपण्यापूर्वी १० मिनिटे शांत बसून दीर्घ श्वासोच्छ्वास किंवा ध्यान करा.",
            "queries": [
                "रात्री गाढ झोप येत नाही (Insomnia), काय उपाय करावा?",
                "झोप न येण्याची मुख्य कारणे कोणती असतात?",
                "मोबाईल वापरल्याने झोपेवर काय वाईट परिणाम होतो?",
                "शांत झोप येण्यासाठी रात्री काय खावे किंवा प्यावे?",
                "झोपण्यापूर्वी जायफळ घातलेले दूध पिणे उपयुक्त आहे का?",
                "अनिद्रा दूर करण्यासाठी कोणता प्राणायाम करावा?",
                "रात्री वारंवार जाग का येते?",
                "झोपेच्या गोळ्यांची सवय लागते का?"
            ]
        }
    ]

    for item in topics:
        ans_with_disc = item["ans"] + MEDICAL_DISCLAIMER_MR
        for q in item["queries"]:
            samples.append(build_record(q, ans_with_disc, "mr", item["cat"]))
            # Also add respectful / conversational variants
            samples.append(build_record(f"डॉक्टर, {q}", ans_with_disc, "mr", item["cat"]))

    return samples


def main():
    print("=" * 70)
    print("MedAssist AI - Training & Fine-Tuning Data Preparation Pipeline")
    print("=" * 70)

    FINETUNE_DIR.mkdir(parents=True, exist_ok=True)

    all_samples = []

    # 1. Layman terms & Emergency Triage
    print("\n[1/4] Processing Layman Terms & Emergency Triage Routing...")
    layman_samples = load_layman_terms_samples()
    print(f"  -> Generated {len(layman_samples)} layman & triage samples.")
    all_samples.extend(layman_samples)

    # 2. Medical Knowledge CSVs
    print("\n[2/4] Processing Medical Knowledge CSVs (symptoms, diseases, medicines)...")
    csv_samples = load_csv_knowledge()
    print(f"  -> Generated {len(csv_samples)} structured knowledge samples.")
    all_samples.extend(csv_samples)

    # 3. Public Patient Dialogue (MEDIQ)
    print("\n[3/4] Processing MEDIQ Patient-Doctor Dialogue Dataset...")
    mediq_samples = load_mediq_dialogues(max_samples=3500)
    print(f"  -> Filtered & parsed {len(mediq_samples)} high-relevance clinical dialogue samples.")
    all_samples.extend(mediq_samples)

    # 4. Synthesized Comprehensive Marathi Consultations & Clinical Guidance
    print("\n[4/4] Processing Comprehensive High-Quality Marathi Clinical Consultations...")
    marathi_samples = generate_comprehensive_marathi_dataset()
    print(f"  -> Generated {len(marathi_samples)} native Marathi medical interaction samples.")
    all_samples.extend(marathi_samples)

    total_count = len(all_samples)
    print(f"\n========================================================")
    print(f"Total Combined Samples: {total_count}")
    print(f"========================================================")

    # Deterministic Shuffle and 90/10 Train/Val Split
    random.seed(42)
    random.shuffle(all_samples)

    split_idx = int(total_count * 0.90)
    train_samples = all_samples[:split_idx]
    val_samples = all_samples[split_idx:]

    print(f"\nSplitting Dataset (90% Train / 10% Validation):")
    print(f"  -> Train count: {len(train_samples)}")
    print(f"  -> Val count:   {len(val_samples)}")

    # Write train.jsonl
    with open(TRAIN_OUTPUT, "w", encoding="utf-8") as f:
        for item in train_samples:
            f.write(json.dumps(item, ensure_ascii=False) + "\n")
    print(f"Saved: {TRAIN_OUTPUT} ({os.path.getsize(TRAIN_OUTPUT)} bytes)")

    # Write val.jsonl
    with open(VAL_OUTPUT, "w", encoding="utf-8") as f:
        for item in val_samples:
            f.write(json.dumps(item, ensure_ascii=False) + "\n")
    print(f"Saved: {VAL_OUTPUT} ({os.path.getsize(VAL_OUTPUT)} bytes)")

    # Verification: Read back line by line
    for name, path in [("train", TRAIN_OUTPUT), ("val", VAL_OUTPUT)]:
        with open(path, "r", encoding="utf-8") as f:
            lines = [json.loads(line) for line in f]
        print(f"Verified {name}.jsonl: successfully parsed {len(lines)} JSON objects.")

    # Dataset Statistics Calculation
    lang_counts = {}
    cat_counts = {}
    for sample in all_samples:
        lang = sample.get("language", "unknown")
        cat = sample.get("category", "unknown")
        lang_counts[lang] = lang_counts.get(lang, 0) + 1
        cat_counts[cat] = cat_counts.get(cat, 0) + 1

    print("\n" + "=" * 50)
    print("DATASET STATISTICS")
    print("=" * 50)
    print(f"Total Samples: {total_count}")
    print(f"Training Samples: {len(train_samples)} ({len(train_samples)/total_count*100:.1f}%)")
    print(f"Validation Samples: {len(val_samples)} ({len(val_samples)/total_count*100:.1f}%)")

    print("\nLanguage Breakdown:")
    for lang, cnt in sorted(lang_counts.items(), key=lambda x: x[1], reverse=True):
        print(f"  - {lang.upper():<5}: {cnt:>5} ({cnt/total_count*100:5.2f}%)")

    print("\nCategory Breakdown:")
    for cat, cnt in sorted(cat_counts.items(), key=lambda x: x[1], reverse=True):
        print(f"  - {cat:<24}: {cnt:>5} ({cnt/total_count*100:5.2f}%)")

    # Sample Inspection (3 diverse newly generated Marathi entries)
    print("\n" + "=" * 50)
    print("3 DIVERSE NEW MARATHI SAMPLE ENTRIES")
    print("=" * 50)

    # 1. Symptom Advice
    mr_sym = next((s for s in train_samples if s["category"] == "symptom_guidance" and s["language"] == "mr" and "थंडी" in s["instruction"]), None)
    if not mr_sym:
        mr_sym = next((s for s in train_samples if s["category"] == "symptom_guidance" and s["language"] == "mr"), None)

    # 2. Medication Guidance
    mr_med = next((s for s in train_samples if s["category"] == "medication_guidance" and s["language"] == "mr"), None)

    # 3. Pediatric / Chronic Care
    mr_ped_chron = next((s for s in train_samples if s["category"] in ["pediatric_guidance", "chronic_care"] and s["language"] == "mr" and ("डायबेटीस" in s["output"] or "ताप" in s["instruction"])), None)
    if not mr_ped_chron:
        mr_ped_chron = next((s for s in train_samples if s["category"] in ["pediatric_guidance", "chronic_care"] and s["language"] == "mr"), None)

    samples_to_show = [
        ("1. Regional Symptom Advice (मराठी लक्षणे व घरगुती काळजी)", mr_sym),
        ("2. OTC / Medication Guidance (मराठी औषधोपचार व नियम)", mr_med),
        ("3. Pediatric & Chronic Condition Care (बालसंगोपन व जुनाट आजार व्यवस्थापन)", mr_ped_chron),
    ]

    for idx, (title, sample) in enumerate(samples_to_show, 1):
        if sample:
            print(f"\n--- [SAMPLE {idx}] {title} ---")
            print(f"Language: {sample['language']} | Category: {sample['category']}")
            print(f"Instruction: {sample['instruction']}")
            print(f"Output:\n{sample['output']}")
            print(f"Messages count: {len(sample['messages'])}")


if __name__ == "__main__":
    main()
