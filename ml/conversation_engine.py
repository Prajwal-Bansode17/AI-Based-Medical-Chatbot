"""
MedAssist AI - Conversation Engine
Implements context-aware reference resolution, clinical ambiguity clarifying questions,
multilingual dialect styling (English, Hindi, Marathi, Hinglish, Roman Marathi),
and prompt injection defense.
"""

import re
import logging
from typing import Optional, Dict, List, Tuple

logger = logging.getLogger("medassist.conversation_engine")

# Common medicine patterns to recognize in history for anaphoric resolution
COMMON_MEDICINES = [
    "paracetamol", "acetaminophen", "ibuprofen", "aspirin", "metformin",
    "amoxicillin", "cetirizine", "azithromycin", "atorvastatin", "pantoprazole",
    "omeprazole", "dextromethorphan", "ciprofloxacin", "losartan", "lisinopril",
    "am数のprazole", "insulin", "antacid", "cough syrup", "dolo", "crocin",
    "calpol", "combiflam", "allegra", "augmentin", "pan 40", "gelusil"
]

COMMON_CONDITIONS = [
    "headache", "migraine", "fever", "cough", "cold", "flu", "diabetes",
    "hypertension", "high blood pressure", "asthma", "bronchitis", "pneumonia",
    "stomach pain", "acidity", "gerd", "diarrhea", "constipation", "dengue",
    "malaria", "typhoid", "covid", "anemia", "arthritis", "back pain", "throat pain"
]

# Pronoun & anaphoric trigger phrases
REFERENCE_TRIGGERS = [
    "its side effect", "its side effects", "side effects of it", "side effect of it",
    "can i take that", "can i take it", "how long should i take it", "how long to take it",
    "is it safe", "is that safe", "what about it", "what about that",
    "dose of it", "dosage of it", "when to take it", "take it before food",
    "take it after food", "take that with", "take it with",
    "why is it happening", "will it get worse", "does it spread",
    "prevent that", "prevent it", "treat that", "treat it", "cure it",
    "te ghyava ka", "te kadhi ghyayche", "tyache side effects", "tyacha dushparinam",
    "ye kab lena hai", "iski dose", "iske side effect", "kya ye safe hai"
]

# Ambiguous, underspecified single-word or short symptom queries
AMBIGUOUS_PATTERNS = {
    "vague_headache": {
        "keywords": [
            r"^(?:i have\s+)?(?:a\s+)?headache$", r"^(?:mala\s+)?doke\s*dukhat(?: aa?he)?$",
            r"^(?:mujhe\s+)?sar\s*dard(?: hai)?$", r"^head\s*hurts$"
        ],
        "en": "To understand your headache better, could you clarify: 1) Is the pain throbbing, tight, or on one side? 2) How long have you had it? 3) Do you have visual disturbances, nausea, or fever?",
        "hi": "सिरदर्द को सही तरीके से समझने के लिए, कृपया बताएं: 1) क्या दर्द एक तरफ है या पूरे सिर में? 2) क्या दर्द के साथ जी मिचलाना या आंखों में धुंधलापन है? 3) यह दर्द कब से हो रहा है?",
        "mr": "डोकेदुखीचे कारण समजण्यासाठी, कृपया स्पष्ट कराल का: 1) डोके एकाच बाजूला दुखतेय की संपूर्ण डोके? 2) डोकेदुखीसोबत मळमळ किंवा डोळ्यांसमोर अंधारी येते का? 3) हे दुखणे कधीपासून आहे?",
        "hinglish": "Sar dard ke baare mein batayein: 1) Dard ek taraf hai ya poore sar mein? 2) Kya ulti jaisa lagna ya aakhon ke aage dhundhlapan hai? 3) Ye kab se ho raha hai?",
        "roman_mr": "Dokedukhibaddal krupaya sanga: 1) Doke ekyach bajula dukhtey ki purna dokyat? 2) Sobat malmal kinva dolyansamor andhari yete ka? 3) Kiti velapasun hotey?"
    },
    "vague_pain": {
        "keywords": [
            r"^(?:i have\s+)?pain$", r"^(?:mala\s+)?dukhat\s*aa?he$",
            r"^(?:mujhe\s+)?dard\s*hai$", r"^body\s*pain$", r"^ang\s*dukhi$"
        ],
        "en": "To give you the most accurate medical guidance, could you please tell me: 1) Where exactly is the pain located? 2) How severe is it (mild, moderate, or severe)? 3) How long have you been experiencing it?",
        "hi": "सटीक चिकित्सीय सलाह के लिए, क्या आप बता सकते हैं: 1) दर्द शरीर के किस हिस्से में है? 2) दर्द कितना तेज (हल्का, मध्यम या बहुत तेज) है? 3) यह दर्द कब से हो रहा है?",
        "mr": "अचूक वैद्यकीय सल्ल्यासाठी, कृपया स्पष्ट कराल का: 1) नेमके कुठे दुखत आहे? 2) वेदना किती तीव्र आहेत (कमी, मध्यम की तीव्र)? 3) हे दुखणे कधीपासून सुरू आहे?",
        "hinglish": "Sahi medical jaankari ke liye, kya aap bata sakte hain: 1) Dard body ke kis hisse mein ho raha hai? 2) Dard kitna tez hai? 3) Aur ye kab se shuru hua hai?",
        "roman_mr": "Achooq sallyasathi, krupaya sangal ka: 1) Nemke kuthe dukhat ahe? 2) Vedna kiti teevra ahet (kam, madhyam ki jast)? 3) Ani he kadhapasun hot ahe?"
    },
    "vague_cough": {
        "keywords": [
            r"^(?:i have\s+)?cough$", r"^coughing$", r"^(?:mala\s+)?khokla(?: aa?he)?$",
            r"^(?:mujhe\s+)?khansi(?:\s+ho\s+rahi\s+hai|\s+hai)?$"
        ],
        "en": "To guide you safely, could you share a few details: 1) Is your cough dry, or does it bring up mucus/phlegm? 2) How many days has it lasted? 3) Do you also have fever or difficulty breathing?",
        "hi": "सुरक्षित मार्गदर्शन के लिए, कृपया कुछ बातें बताएं: 1) क्या यह सूखी खांसी है या बलगम/कफ आ रहा है? 2) यह कितने दिनों से है? 3) क्या साथ में बुखार या सांस लेने में तकलीफ भी है?",
        "mr": "सुरक्षित मार्गदर्शनासाठी, कृपया सांगा: 1) खोकला कोरडा आहे की कफ पडतो आहे? 2) खोकला किती दिवसांपासून आहे? 3) सोबत ताप किंवा श्वास घेण्यास त्रास होत आहे का?",
        "hinglish": "Aapki behtar madad ke liye, kripya batayein: 1) Khaansi sookhi (dry) hai ya balgam aa raha hai? 2) Ye kitne dino se hai? 3) Kya saath mein bukhar ya saans lene mein takleef bhi hai?",
        "roman_mr": "Madatisathi krupaya sanga: 1) Khokla korda ahe ki kaph padtoy? 2) Kiti divasapasun ahe? 3) Sobat taap kinva shwaas ghyayla traas hoto ka?"
    },
    "vague_stomach": {
        "keywords": [
            r"^(?:i have\s+)?stomach\s*(?:pain|ache)$", r"^(?:mala\s+)?pot\s*dukhat(?: aa?he)?$",
            r"^(?:mujhe\s+)?pet\s*dard(?: hai)?$", r"^tummy\s*ache$"
        ],
        "en": "Stomach discomfort can have many causes. Could you clarify: 1) Is the pain in the upper, lower, or middle abdomen? 2) Is it cramping, sharp, or a dull ache? 3) Do you have vomiting, diarrhea, or fever?",
        "hi": "पेट दर्द के कई कारण हो सकते हैं। कृपया स्पष्ट करें: 1) दर्द पेट के ऊपरी, निचले या बीच के हिस्से में है? 2) क्या मरोड़, जलन या हल्का मीठा दर्द है? 3) क्या उल्टी, दस्त या बुखार भी है?",
        "mr": "पोटदुखीची अनेक कारणे असू शकतात. कृपया स्पष्ट कराल का: 1) पोटदुखी वरच्या, खालच्या की मध्य भागात आहे? 2) कळ येतेय, जळजळ होतेय की हलके दुखतेय? 3) सोबत उलट्या, जुलाब किंवा ताप आहे का?",
        "hinglish": "Pet dard ke kai kaaran ho sakte hain. Kripya batayein: 1) Dard pet ke oopri hisse mein hai, neeche, ya beech mein? 2) Marod (cramp) hai ya jalan ho rahi hai? 3) Kya vomiting, loose motions ya bukhar bhi hai?",
        "roman_mr": "Pot dukhnyachi barech karne asu shaktat. Krupaya sanga: 1) Dukhne varchya bhagat ahe, khalchya ki madhye? 2) Kal yetey, jaljal hotey ki mand dukhney ahe? 3) Sobat ulti, julaab kinva taap ahe ka?"
    },
    "vague_fever": {
        "keywords": [
            r"^(?:i have\s+)?fever$", r"^(?:mala\s+)?ta?ap(?: aa?he)?$",
            r"^(?:mujhe\s+)?bukhar(?: hai)?$", r"^high\s*temperature$"
        ],
        "en": "To understand your fever better, could you let me know: 1) What is your current temperature if you have measured it? 2) How many days have you had fever? 3) Are there chills, shivering, headache, or throat pain?",
        "hi": "बुखार को बेहतर समझने के लिए, कृपया बताएं: 1) थर्मामीटर से नापने पर तापमान कितना है? 2) बुखार कितने दिनों से है? 3) क्या ठंड लगकर कंपकंपी, सिरदर्द या गले में खराश भी है?",
        "mr": "तापाची तीव्रता समजून घेण्यासाठी, कृपया सांगा: 1) थर्मामीटरने मोजल्यास तापमान किती आहे? 2) ताप किती दिवसांपासून येतो आहे? 3) थंडी वाजून येते का, आणि डोकेदुखी किंवा घसादुखी आहे का?",
        "hinglish": "Bukhar ke baare mein sahi jaankari ke liye, batayein: 1) Thermometer se naapne par temperature kitna hai? 2) Bukhar kitne dino se aa raha hai? 3) Kya thand lag rahi hai, sar dard ya gale mein kharash hai?",
        "roman_mr": "Tapachi mahiti ghenyasaathi, krupaya sanga: 1) Thermometer ne mojlyas tapman kiti ahe? 2) Tap kiti divasapasun ahe? 3) Thandi vajun yete ka, ani dokedukhi kinva ghasadukhi ahe ka?"
    },
    "vague_malaise": {
        "keywords": [
            r"^(?:i\s+)?feel\s*(?:sick|bad|unwell)$", r"^not\s*feeling\s*well$",
            r"^(?:mala\s+)?bara\s*vatat\s*nahi$", r"^(?:mujhe\s+)?tabiyat\s*kharab\s*hai$"
        ],
        "en": "I'm sorry to hear you're not feeling well. To assist you properly, what specific symptoms are you noticing right now (e.g., weakness, nausea, dizziness, body ache, or fever)?",
        "hi": "आपकी तबीयत ठीक नहीं है यह जानकर खेद हुआ। सही मदद के लिए, क्या आप बता सकते हैं कि अभी क्या महसूस हो रहा है (जैसे कमजोरी, जी मिचलाना, चक्कर, बदन दर्द या बुखार)?",
        "mr": "तुम्हाला बरे वाटत नाही हे ऐकून वाईट वाटले. योग्य मदतीसाठी, कृपया सांगा की सध्या नक्की काय त्रास होत आहे (उदा. अशक्तपणा, मळमळ, चक्कर, अंगदुखी किंवा ताप)?",
        "hinglish": "Aapki tabiyat theek nahi hai, ye sunkar dukh hua. Madad ke liye kripya batayein abhi kya takleef ho rahi hai (jaise kamzori, ulti jaisa lagna, chakkar, body pain ya bukhar)?",
        "roman_mr": "Tumhala bara vatat nahi he aikun vait vatle. Madatisathi krupaya sanga ki nemka kay traas hoto ahe (uda. ashaktpana, malmal, chakkar, angdukhi kinva taap)?"
    }
}

# Prompt injection markers to strip or disarm
PROMPT_INJECTION_PATTERNS = [
    r"(?i)\bignore\s+(?:all\s+)?(?:previous|prior|above)\s+instructions\b",
    r"(?i)\byou\s+are\s+now\s+a\s+(?:licensed\s+doctor|surgeon|physician)\b",
    r"(?i)\b(?:reveal|print|show|repeat|display|output)\s+(?:your\s+)?(?:full\s+)?(?:system\s+prompt|system\s+instructions|developer\s+mode|instructions|guidelines)\b",
    r"(?i)\b(?:system\s+prompt|developer\s+instructions)\b",
    r"(?i)\bjailbreak\b",
    r"(?i)\b(?:act\s+as\s+)?dan(?:\s+mode)?\b",
    r"(?i)\byou\s+are\s+now\s+dan\b",
    r"(?i)\bact\s+as\s+an?\s+(?:unrestricted|dan|jailbreak)\b",
    r"(?i)\bprescribe\s+me\s+a\s+controlled\s+substance\b"
]


class DialectResult(dict):
    def __init__(self, language: str, script: str, dialect: str):
        super().__init__(language=language, script=script, dialect=dialect)
        self.language = language
        self.script = script
        self.dialect = dialect

    def __str__(self):
        return self.dialect

    def __eq__(self, other):
        if isinstance(other, str):
            return self.dialect == other or self.language == other
        return super().__eq__(other)


def detect_dialect_and_script(text: str) -> DialectResult:
    """
    Detects whether the input is:
    - 'en': English (Latin)
    - 'hi': Hindi (Devanagari)
    - 'mr': Marathi (Devanagari)
    - 'hinglish': Romanized Hindi (Latin)
    - 'roman_mr': Romanized Marathi (Latin)
    """
    if not text:
        return DialectResult("en", "latin", "en")

    clean = text.strip()
    devanagari_chars = [ch for ch in clean if '\u0900' <= ch <= '\u097F']
    if devanagari_chars:
        # ळ is uniquely Marathi
        if any(ch == 'ळ' for ch in devanagari_chars):
            return DialectResult("mr", "devanagari", "mr")
        marathi_markers = {"मला", "माझा", "माझी", "माझे", "आहे", "नाही", "काय", "कसा", "कशी", "खोकला", "ताप", "डोके", "पोट"}
        words = set(re.findall(r"[\u0900-\u097F]+", clean))
        if words.intersection(marathi_markers):
            return DialectResult("mr", "devanagari", "mr")
        return DialectResult("hi", "devanagari", "hi")

    lower = clean.lower()
    roman_mr_markers = {
        "mala", "majha", "maza", "majhi", "aahe", "ahe", "nahi", "nahin", "kay",
        "kasa", "kashi", "kase", "kiti", "khokla", "doka", "dok", "doke", "pot", "dukhat",
        "dukh", "ang", "ashaktpana", "garagar", "bar", "bara", "vatat", "pan", "pn"
    }
    roman_hi_markers = {
        "mujhe", "mera", "meri", "mere", "hai", "hain", "kya", "kaise", "kaisi",
        "kitna", "kitni", "khansi", "sir", "pet", "dard", "bukhar", "kamzori",
        "thakan", "chahiye", "kuch", "hota", "hoti", "raha", "rahi"
    }

    words = set(re.findall(r"[a-z]+", lower))
    mr_count = len(words.intersection(roman_mr_markers))
    hi_count = len(words.intersection(roman_hi_markers))

    if mr_count > 0 and mr_count >= hi_count:
        return DialectResult("mr", "latin", "roman_mr")
    elif hi_count > 0 and hi_count > mr_count:
        return DialectResult("hi", "latin", "hinglish")
    return DialectResult("en", "latin", "en")


def sanitize_prompt_input(text: str) -> Tuple[str, bool]:
    """
    Strips prompt injection attempts from user input.
    Returns (sanitized_text, injection_detected).
    """
    if not text:
        return "", False

    cleaned = text
    detected = False
    for pat in PROMPT_INJECTION_PATTERNS:
        if re.search(pat, cleaned):
            detected = True
            cleaned = re.sub(pat, "[safe medical inquiry]", cleaned)

    return cleaned.strip(), detected


def extract_referenced_entities_from_history(history: List[dict]) -> Dict[str, str]:
    """
    Scans the recent conversation history to identify the last mentioned medicine,
    symptom, or clinical condition for anaphoric pronoun resolution.
    """
    last_medicine = ""
    last_condition = ""

    for item in reversed(history[-6:]):
        msg = str(item.get("message") or item.get("content") or "").lower()

        if not last_medicine:
            for med in COMMON_MEDICINES:
                if re.search(r"\b" + re.escape(med) + r"\b", msg):
                    last_medicine = med
                    break

        if not last_condition:
            for cond in COMMON_CONDITIONS:
                if re.search(r"\b" + re.escape(cond) + r"\b", msg):
                    last_condition = cond
                    break

        if last_medicine and last_condition:
            break

    return {
        "last_medicine": last_medicine,
        "last_condition": last_condition
    }


def resolve_followup_references(user_text: str, history: List[dict]) -> Tuple[str, Optional[str]]:
    """
    Resolves conversational pronouns ('it', 'that', 'its side effects')
    using tracked entities from the recent conversation history.
    Returns (enriched_prompt, resolved_entity_note).
    """
    if not user_text or not history:
        return user_text, None

    lower = user_text.lower().strip()
    is_reference = any(trig in lower for trig in REFERENCE_TRIGGERS) or bool(re.search(r"\b(?:that|it|its|this)\b", lower))
    if not is_reference:
        return user_text, None

    entities = extract_referenced_entities_from_history(history)
    resolved_entity = entities["last_medicine"] or entities["last_condition"]
    if not resolved_entity:
        return user_text, None

    enriched_prompt = (
        f"{user_text}\n"
        f"[Context resolution: The user's pronouns ('it' / 'that') refer to '{resolved_entity}' "
        f"discussed in recent turns of this conversation.]"
    )
    return enriched_prompt, resolved_entity


def check_for_ambiguous_query(user_text: str, detected_dialect: str = "en") -> Optional[Dict[str, str]]:
    """
    Checks if user query is clinically underspecified/ambiguous and generates
    a focused clarifying question rather than guessing a diagnosis or giving irrelevant advice.
    """
    if not user_text:
        return None

    clean = user_text.strip().lower()
    # Check if query is very brief (1 to 4 words)
    words = clean.split()
    if len(words) > 5:
        return None

    for category, cfg in AMBIGUOUS_PATTERNS.items():
        for pat in cfg["keywords"]:
            if re.search(pat, clean):
                # Pick localized question
                if detected_dialect in cfg:
                    q_text = cfg[detected_dialect]
                elif detected_dialect in ("hi", "hinglish"):
                    q_text = cfg["hinglish"]
                elif detected_dialect in ("mr", "roman_mr"):
                    q_text = cfg["roman_mr"]
                else:
                    q_text = cfg["en"]

                return {
                    "category": category,
                    "clarification_question": q_text,
                    "is_ambiguous": True
                }

    return None
