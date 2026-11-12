"""
MedAssist AI - Conversation Engine
Implements context-aware reference resolution, clinical ambiguity clarifying questions,
multilingual dialect styling (English, Hindi, Marathi, Hinglish, Roman Marathi),
and prompt injection defense.
"""

import re
import logging
from typing import Optional, Dict, List, Tuple, Any

from environmental_health import (
    get_atmospheric_context,
    get_regional_health_alert,
    get_citizen_advisory,
    format_atmospheric_advisory_block,
)

try:
    from lab_report_analyzer import analyze_lab_report, BIOMARKER_DATABASE
except ImportError:
    try:
        from ml.lab_report_analyzer import analyze_lab_report, BIOMARKER_DATABASE
    except ImportError:
        analyze_lab_report = None
        BIOMARKER_DATABASE = {}

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
    "explain", "report", "above", "result", "results", "what does this mean",
    "what do these mean", "meaning of this", "what does it mean",
    "is this normal", "are these normal", "explain this", "explain that",
    "explain above", "above report", "above results", "check my report",
    "te ghyava ka", "te kadhi ghyayche", "tyache side effects", "tyacha dushparinam",
    "ye kab lena hai", "iski dose", "iske side effect", "kya ye safe hai",
    "iska matlab", "kya matlab", "arth kay", "arth kya", "sang", "sanga"
]

CLINICAL_FOLLOWUP_TRIGGERS = [
    "explain", "report", "above", "result", "results", "what does this mean",
    "what do these mean", "what does that mean", "meaning", "interpretation",
    "test", "tests", "normal", "abnormal", "high", "low", "bad", "risky",
    "dangerous", "serious", "why", "what should i do", "what to do",
    "kya matlab", "iska matlab", "arth kay", "arth kya", "sang", "sanga",
    "samjhav", "samjhao", "spasht kara", "theek hai kya", "bar ahe ka",
    "kasa ahe", "kaisa hai"
]

def sanitize_kgmu_references(text: str) -> str:
    """
    Purges and sanitizes any external hospital branding (KGMU / King George's Medical University)
    from answers so no legacy portal references leak into user responses.
    """
    if not text or not isinstance(text, str):
        return text

    t = re.sub(r"https?://\S*kgmu\S*", "https://medassist.ai", text, flags=re.IGNORECASE)
    t = re.sub(r"www\.kgmu\.org\S*", "www.medassist.ai", t, flags=re.IGNORECASE)
    t = re.sub(r"\b\S*kgmu\S*@\S*", "support@medassist.ai", t, flags=re.IGNORECASE)
    t = re.sub(r"[a-zA-Z0-9._%+-]*kgmu[a-zA-Z0-9._%+-]*@[a-zA-Z0-9.-]+", "support@medassist.ai", t, flags=re.IGNORECASE)
    t = re.sub(r"\bKing George[’']?s? Medical University\s*(?:\(KGMU\))?", "MedAssist AI", t, flags=re.IGNORECASE)
    t = re.sub(r"\bKing George[’']?s?\b", "MedAssist AI", t, flags=re.IGNORECASE)
    t = re.sub(r"\bKGMU\b", "MedAssist AI", t, flags=re.IGNORECASE)

    t = re.sub(r"(?i)\bbook an appointment at MedAssist AI\b", "consult a qualified doctor or healthcare facility", t)
    t = re.sub(r"(?i)\bat MedAssist AI\b", "with your healthcare provider", t)
    t = re.sub(r"(?i)\bfrom MedAssist AI\b", "from your healthcare provider", t)
    t = re.sub(r"(?i)\bto MedAssist AI\b", "to your medical specialist", t)

    return t


def format_hf_chat_prompt(
    system_prompt: Optional[str] = None,
    user_prompt: str = "",
    messages: Optional[List[dict]] = None,
) -> str:
    """
    Formats multi-turn messages into Hugging Face chat template tokens (<|system|>, <|user|>, <|assistant|>)
    specifically structured for MedAssist-AI-1B.
    """
    full_prompt = ""
    if system_prompt:
        full_prompt += f"<|system|>\n{system_prompt}\n"
    if messages and isinstance(messages, list):
        for msg in messages:
            role = msg.get("role", "user")
            content = msg.get("content", "")
            if role == "system" and not system_prompt:
                full_prompt += f"<|system|>\n{content}\n"
            elif role in ("user", "assistant"):
                full_prompt += f"<|{role}|>\n{content}\n"
        if not full_prompt.endswith("<|assistant|>\n"):
            full_prompt += "<|assistant|>\n"
    else:
        full_prompt += f"<|user|>\n{user_prompt}\n<|assistant|>\n"
    return full_prompt


def is_quota_exhausted_error(err_str: str) -> bool:
    """
    Detects if an error from Gemini/AI provider is a 429 RESOURCE_EXHAUSTED / quota exceeded error.
    """
    if not err_str:
        return False
    lower = str(err_str).lower()
    return any(marker in lower for marker in (
        "429", "resource_exhausted", "resourceexhausted", "quota", "rate limit", "rate_limit"
    ))


def has_clinical_history_context(session_history: List[dict]) -> bool:
    """
    Checks if recent messages in session history contain clinical context,
    symptoms, or a lab report summary.
    """
    if not session_history:
        return False

    clinical_markers = [
        "[clinical lab report context]", "lab report", "biomarker", "hemoglobin",
        "glucose", "creatinine", "sugar", "cholesterol", "platelet", "wbc", "rbc",
        "urea", "uric acid", "bilirubin", "sgpt", "sgot", "tsh", "mg/dl", "g/dl",
        "doctor", "hospital", "medicine", "prescription", "symptom", "pain", "fever",
        "cough", "diagnosis", "treatment", "infection", "blood test", "flagged parameters"
    ]

    for item in session_history[-6:]:
        msg = str(item.get("message") or item.get("content") or "").lower()
        syms = item.get("symptoms") or []
        if syms:
            return True
        if any(marker in msg for marker in clinical_markers):
            return True
    return False

def is_conversational_followup_query(text: str, session_history: Optional[List[dict]] = None) -> bool:
    """
    Detects if a user query is a conversational follow-up to ongoing clinical discussion,
    such as 'explain me', 'what does this mean', 'explain above results',
    even when preceded by a casual greeting ('helo explain me...').
    """
    if not text:
        return False
    lower = text.lower().strip()

    # Strip leading casual greeting formulas like 'helo ', 'hi ', 'hey '
    stripped = re.sub(r"^(?:hi|hello|helo|hey|namaste|namaskar|pranam|hlo|hii)\b[\s,!-]*", "", lower).strip()
    target = stripped if stripped else lower

    has_trigger = any(re.search(r"\b" + re.escape(trig) + r"\b", target) for trig in CLINICAL_FOLLOWUP_TRIGGERS)
    if not has_trigger:
        has_trigger = any(trig in target for trig in [
            "what does this mean", "what do these mean", "what does that mean",
            "explain me", "explain this", "explain that", "explain above",
            "above report", "above result", "above results", "check my report"
        ])

    if has_trigger:
        if session_history is not None:
            return has_clinical_history_context(session_history)
        return True

    return False

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
        "mala", "majha", "maza", "majhi", "majhe", "aahe", "ahe", "nahi", "nahin", "kay",
        "kasa", "kashi", "kase", "kiti", "khokla", "doka", "dok", "doke", "pot", "dukhat",
        "dukh", "ang", "ashaktpana", "garagar", "bar", "bara", "vatat", "pan", "pn",
        "tap", "taap", "sardi", "ani", "aani", "khup", "angdukhi", "ghasadukhi",
        "dokedukhi", "potdukhi", "karu", "karaycha"
    }
    roman_hi_markers = {
        "mujhe", "mera", "meri", "mere", "hai", "hain", "kya", "kaise", "kaisi",
        "kitna", "kitni", "khansi", "khasi", "sir", "pet", "dard", "bukhar", "jukam",
        "zukam", "zukaam", "gala", "kamzori", "thakan", "chahiye", "kuch", "hota",
        "hoti", "raha", "rahi"
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
    symptom, clinical condition, or lab report for anaphoric pronoun resolution.
    """
    last_medicine = ""
    last_condition = ""
    last_lab_report = ""

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

        if not last_lab_report:
            if any(k in msg for k in ["[clinical lab report context]", "flagged parameters", "biomarkers evaluated", "blood test", "hemoglobin", "glucose", "creatinine"]):
                last_lab_report = "the laboratory blood test results"

        if last_medicine and last_condition and last_lab_report:
            break

    return {
        "last_medicine": last_medicine,
        "last_condition": last_condition,
        "last_lab_report": last_lab_report,
    }


def resolve_followup_references(user_text: str, history: List[dict]) -> Tuple[str, Optional[str]]:
    """
    Resolves conversational pronouns ('it', 'that', 'its side effects') and
    follow-up requests ('explain', 'what does this mean', 'above report')
    using tracked entities from recent conversation history.
    Returns (enriched_prompt, resolved_entity_note).
    """
    if not user_text or not history:
        return user_text, None

    lower = user_text.lower().strip()
    is_reference = (
        any(trig in lower for trig in REFERENCE_TRIGGERS)
        or bool(re.search(r"\b(?:that|it|its|this|these|above|report|results)\b", lower))
        or is_conversational_followup_query(user_text, history)
    )
    if not is_reference:
        return user_text, None

    entities = extract_referenced_entities_from_history(history)
    resolved_entity = entities["last_medicine"] or entities["last_condition"] or entities.get("last_lab_report")
    if not resolved_entity and has_clinical_history_context(history):
        resolved_entity = "the medical findings and test results discussed earlier in this conversation"

    if not resolved_entity:
        return user_text, None

    enriched_prompt = (
        f"{user_text}\n"
        f"[Context resolution: The user is asking to explain or interpret '{resolved_entity}' "
        f"discussed in recent turns of this conversation.]"
    )
    return enriched_prompt, resolved_entity


CANONICAL_SYMPTOM_MAP = {
    "vague_cough": "cough",
    "vague_headache": "headache",
    "vague_stomach": "stomach_pain",
    "vague_fever": "fever",
    "vague_pain": "body_pain",
    "vague_malaise": "weakness"
}


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

                canonical = CANONICAL_SYMPTOM_MAP.get(category, category.replace("vague_", ""))
                return {
                    "category": category,
                    "canonical_symptom": canonical,
                    "clarification_question": q_text,
                    "is_ambiguous": True
                }

    return None


# ============================================================
# LONGITUDINAL GREETING, CHECK-IN & CLINICAL FOLLOW-UP HELPERS
# ============================================================

GREETING_PATTERNS = [
    r"^(hi|hello|hey|namaste|namaskar|pranam|good\s+morning|good\s+evening|good\s+afternoon|hlo|hii|helo)(\s+(medassist|doc|doctor|bot|there|assistant|bhai|sir|mam))?[\s!.,?]*$",
    r"^(नमस्कार|नमस्ते|हाय|हॅलो|प्रणाम)(\s+(डॉक्टर|मेडॉसिस्ट|सर))?[\s!.,?]*$",
    r"^(kem\s+cho|vanakkam|sat\s+sri\s+akal)[\s!.,?]*$",
]

RESOLUTION_PATTERNS = [
    r"(?i)\b(it'?s\s+fine\s+now|its\s+fine\s+now|fine\s+now|better\s+now|much\s+better|feeling\s+better|cured|healed|recovered)\b",
    r"(?i)\b(pain\s+is\s+gone|pain\s+gone|no\s+pain\s+now|not\s+hurting\s+anymore|no\s+more\s+pain|fever\s+is\s+gone|gone\s+now)\b",
    r"(?i)\b(bar[ea]?\s+va?tat[ae]y|theek\s+ho\s+gaya|thik\s+ho\s+gaya|theek\s+hai\s+ab|aaram\s+hai|aram\s+ahe)\b",
    r"(?<!\S)(आता\s+बरे\s+वाटते|आता\s+ठीक\s+आहे|बरे\s+वाटतेय|आराम\s+वाटतोय|आराम\s+आहे|आता\s+काही\s+त्रास\s+नाही)(?:[.,!?]|\s|$)",
    r"(?<!\S)(अब\s+ठीक\s+है|दर्द\s+चला\s+गया|राहत\s+मिल\s+गई|दर्द\s+ठीक\s+हो\s+गया|अब\s+कोई\s+तकलीफ\s+नहीं)(?:[.,!?]|\s|$)",
]

SYMPTOM_LOCALIZED_NAMES = {
    "neck_pain": {"en": "neck pain", "mr": "मानदुखी", "hi": "गर्दन का दर्द"},
    "neck pain": {"en": "neck pain", "mr": "मानदुखी", "hi": "गर्दन का दर्द"},
    "headache": {"en": "headache", "mr": "डोकेदुखी", "hi": "सिरदर्द"},
    "fever": {"en": "fever", "mr": "ताप", "hi": "बुखार"},
    "cough": {"en": "cough", "mr": "खोकला", "hi": "खांसी"},
    "stomach_pain": {"en": "stomach pain", "mr": "पोटदुखी", "hi": "पेट दर्द"},
    "stomach pain": {"en": "stomach pain", "mr": "पोटदुखी", "hi": "पेट दर्द"},
    "back_pain": {"en": "back pain", "mr": "पाठदुखी", "hi": "पीठ दर्द"},
    "back pain": {"en": "back pain", "mr": "पाठदुखी", "hi": "पीठ दर्द"},
    "acidity": {"en": "acidity", "mr": "पित्त/छातीत जळजळ", "hi": "एसिडिटी"},
    "cold": {"en": "cold", "mr": "सर्दी", "hi": "जुकाम"},
    "body_pain": {"en": "body pain", "mr": "अंगदुखी", "hi": "बदन दर्द"},
    "body pain": {"en": "body pain", "mr": "अंगदुखी", "hi": "बदन दर्द"},
    "dizziness": {"en": "dizziness", "mr": "चक्कर येणे", "hi": "चक्कर आना"},
    "weakness": {"en": "weakness", "mr": "अशक्तपणा", "hi": "कमजोरी"},
    "vomiting": {"en": "vomiting", "mr": "उलटी", "hi": "उल्टी"},
    "nausea": {"en": "nausea", "mr": "मळमळ", "hi": "जी मिचलाना"},
    "joint_pain": {"en": "joint pain", "mr": "सांधेदुखी", "hi": "जोड़ों का दर्द"},
    "joint pain": {"en": "joint pain", "mr": "सांधेदुखी", "hi": "जोड़ों का दर्द"},
    "sore_throat": {"en": "sore throat", "mr": "घसा खवखवणे", "hi": "गले में खराश"},
    "sore throat": {"en": "sore throat", "mr": "घसा खवखवणे", "hi": "गले में खराश"},
}


def is_greeting_message(text: str) -> bool:
    """Checks if the user message is a standalone greeting."""
    if not text:
        return False
    clean = text.strip().lower()
    if is_conversational_followup_query(clean):
        return False
    return any(re.search(pat, clean) for pat in GREETING_PATTERNS)


def detect_symptom_resolution(text: str) -> Tuple[bool, Optional[str]]:
    """
    Detects if the user is reporting that their previous symptom has improved or resolved.
    Returns (is_resolved, resolved_symptom_hint).
    """
    if not text:
        return False, None
    clean = text.strip().lower()
    for pat in RESOLUTION_PATTERNS:
        if re.search(pat, clean):
            return True, None
    return False, None


def get_localized_symptom_name(symptom_name: str, language: str = "en") -> str:
    """Translates symptom key into natural localized noun."""
    lang = (language or "en").strip().lower()
    clean = str(symptom_name or "").strip().lower()
    if clean in SYMPTOM_LOCALIZED_NAMES:
        mapping = SYMPTOM_LOCALIZED_NAMES[clean]
        if lang in ("mr", "roman_mr"):
            return mapping.get("mr", clean.replace("_", " "))
        elif lang in ("hi", "hinglish"):
            return mapping.get("hi", clean.replace("_", " "))
        return mapping.get("en", clean.replace("_", " "))
    return clean.replace("_", " ")


def build_proactive_checkin_message(symptom_name: str, language: str = "en") -> str:
    """
    Constructs a warm, proactive check-in greeting for active symptoms:
    - English: "Hello! Before we begin, I wanted to check in: how has your neck pain been feeling? Is it any better?"
    - Marathi: "नमस्कार! सुरू करण्यापूर्वी, तुमची मानदुखी आता कशी आहे? थोडा आराम वाटतोय का?"
    - Hindi: "नमस्ते! आगे बढ़ने से पहले, आपकी गर्दन का दर्द अब कैसा है? कुछ राहत मिली?"
    """
    lang = (language or "en").strip().lower()
    loc_sym = get_localized_symptom_name(symptom_name, lang)

    if lang in ("mr", "roman_mr"):
        return f"नमस्कार! सुरू करण्यापूर्वी, तुमची {loc_sym} आता कशी आहे? थोडा आराम वाटतोय का?"
    elif lang in ("hi", "hinglish"):
        return f"नमस्ते! आगे बढ़ने से पहले, आपकी {loc_sym} अब कैसी है? कुछ राहत मिली?"
    return f"Hello! Before we begin, I wanted to check in: how has your {loc_sym} been feeling? Is it any better?"


def build_resolution_acknowledgment(language: str = "en") -> str:
    """Warm acknowledgment when a tracked symptom is resolved."""
    lang = (language or "en").strip().lower()
    if lang in ("mr", "roman_mr"):
        return "तुम्हाला बरे वाटत आहे हे ऐकून खूप आनंद झाला! स्वतःची काळजी घ्या. इतर काही लक्षणे किंवा प्रश्न असल्यास नक्की सांगा."
    elif lang in ("hi", "hinglish"):
        return "यह जानकर बहुत खुशी हुई कि अब आप बेहतर महसूस कर रहे हैं! अपना ध्यान रखें। यदि कोई अन्य समस्या या प्रश्न हो तो अवश्य बताएं।"
    return "I'm really glad to hear that you're feeling better! Take care of yourself, and let me know if you need anything else or if any new symptoms appear."


def get_quick_reply_chips(language: str = "en") -> List[str]:
    """Returns standard clinical quick-reply chips localized to EN/MR/HI."""
    lang = (language or "en").strip().lower()
    if lang in ("mr", "roman_mr"):
        return ["खूप बरे वाटते", "अजूनही दुखतेय", "त्रास वाढला", "नवीन लक्षण"]
    elif lang in ("hi", "hinglish"):
        return ["काफी बेहतर", "अभी भी दर्द है", "हालत बिगड़ गई", "नया लक्षण"]
    return ["Much better", "Still hurting", "Got worse", "New symptom"]


def generate_clinical_follow_up_questions(symptoms: List[str], language: str = "en") -> List[str]:
    """
    Generates 2 targeted follow-up clarifying questions for non-emergency symptom advice responses.
    """
    lang = (language or "en").strip().lower()
    sym_set = {s.lower().replace("-", "_").replace(" ", "_") for s in (symptoms or [])}

    if "headache" in sym_set:
        if lang in ("mr", "roman_mr"):
            return [
                "ही डोकेदुखी किती दिवसांपासून जाणवते आहे?",
                "डोकेदुखीसोबत मळमळ, प्रकाशाचा त्रास किंवा मान आखडणे असे काही होते का?"
            ]
        elif lang in ("hi", "hinglish"):
            return [
                "यह सिरदर्द कितने दिनों या घंटों से हो रहा है?",
                "क्या सिरदर्द के साथ उल्टी, चक्कर या रोशनी से परेशानी महसूस हो रही है?"
            ]
        return [
            "How long have you been experiencing this headache?",
            "Is the headache accompanied by nausea, light sensitivity, or neck stiffness?"
        ]

    if "neck_pain" in sym_set:
        if lang in ("mr", "roman_mr"):
            return [
                "ही मानदुखी खांद्यांकडे किंवा हाताकडे जाते का, किंवा मुंग्या येतात का?",
                "चुकीच्या पद्धतीने झोपल्याने किंवा वजन उचलल्याने हा त्रास सुरू झाला का?"
            ]
        elif lang in ("hi", "hinglish"):
            return [
                "क्या गर्दन का दर्द कंधों या हाथों की तरफ फैलता है, या सुन्नपन होता है?",
                "क्या यह गलत तरीके से सोने या भारी सामान उठाने के बाद शुरू हुआ?"
            ]
        return [
            "Does the neck pain radiate into your shoulders or arms, or cause numbness?",
            "Did this pain start after awkward sleep posture or lifting heavy objects?"
        ]

    if "fever" in sym_set:
        if lang in ("mr", "roman_mr"):
            return [
                "सध्या अंगात किती ताप आहे, आणि थंडी वाजून येते का?",
                "या तापासोबत अंगदुखी, खोकला किंवा घसा खवखवणे जाणवते का?"
            ]
        elif lang in ("hi", "hinglish"):
            return [
                "वर्तमान में शरीर का तापमान कितना है, और क्या कंपकंपी या ठंड लग रही है?",
                "क्या बुखार के साथ बदन दर्द, खांसी या गले में खराश भी है?"
            ]
        return [
            "What is your current temperature, and have you experienced chills or shivering?",
            "Are there associated symptoms such as body ache, cough, or sore throat?"
        ]

    if "cough" in sym_set or "cold" in sym_set:
        if lang in ("mr", "roman_mr"):
            return [
                "हा कोरडा खोकला आहे की कफ पडतोय?",
                "हा खोकला किती दिवसांपासून आहे, आणि छातीत जडपणा जाणवतो का?"
            ]
        elif lang in ("hi", "hinglish"):
            return [
                "क्या यह सूखी खांसी है या बलगम निकल रहा है?",
                "खांसी कितने दिनों से है, और क्या सीने में भारीपन महसूस हो रहा है?"
            ]
        return [
            "Is it a dry cough or are you bringing up phlegm or mucus?",
            "How many days has this cough lasted, and do you feel any chest congestion?"
        ]

    if "stomach_pain" in sym_set or "acidity" in sym_set:
        if lang in ("mr", "roman_mr"):
            return [
                "पोटात सतत दुखतेय की जेवणानंतर कळा येतात?",
                "यासोबत मळमळ, उलटी, छातीत जळजळ किंवा जुलाब असा त्रास आहे का?"
            ]
        elif lang in ("hi", "hinglish"):
            return [
                "क्या पेट दर्द लगातार बना हुआ है या खाने के बाद ऐंठन होती है?",
                "क्या इसके साथ जी मिचलाना, उल्टी, सीने में जलन या दस्त की समस्या है?"
            ]
        return [
            "Is the abdominal pain constant, sharp, or cramp-like after eating?",
            "Are you experiencing nausea, heartburn, fever, or loose stools?"
        ]

    # Default general follow-ups
    if lang in ("mr", "roman_mr"):
        return [
            "हा त्रास साधारण किती दिवसांपासून जाणवतो आहे?",
            "या लक्षणासोबत ताप, चक्कर किंवा इतर काही त्रास दिसतोय का?"
        ]
    elif lang in ("hi", "hinglish"):
        return [
            "यह समस्या लगभग कितने दिनों से महसूस हो रही है?",
            "क्या इस लक्षण के साथ बुखार, कमजोरी या कोई अन्य परेशानी भी है?"
        ]
    return [
        "How many days have you been noticing these symptoms?",
        "Are you experiencing any associated symptoms such as fever, fatigue, or dizziness?"
    ]


def attach_atmospheric_advisory(
    base_response: str,
    symptoms: Any,
    lat: Optional[float] = None,
    lon: Optional[float] = None,
    city: Optional[str] = None,
    language: str = "en",
    include_helplines: bool = False
) -> str:
    """Appends localized atmospheric intelligence (and helplines only when include_helplines=True)."""
    block = format_atmospheric_advisory_block(
        symptoms,
        lat=lat,
        lon=lon,
        city=city,
        language=language,
        include_helplines=include_helplines,
    )
    if block and block not in base_response:
        return f"{base_response}\n\n{block}"
    return base_response


# -------------------------------------------------------------
# USER IDENTITY & CONVERSATIONAL MEMORY ENGINE
# -------------------------------------------------------------

IDENTITY_QUERY_PATTERNS = [
    # English
    r"\b(?:what(?:'s|\s+is)\s+my\s+name)\b",
    r"\b(?:who\s+am\s+i)\b",
    r"\b(?:do\s+you\s+know\s+my\s+name)\b",
    r"\b(?:do\s+you\s+know\s+who\s+i\s+am)\b",
    r"\b(?:tell\s+me\s+my\s+name)\b",
    r"\b(?:can\s+you\s+tell\s+me\s+my\s+name)\b",
    r"\b(?:remember\s+my\s+name)\b",
    r"\b(?:do\s+you\s+remember\s+me)\b",
    r"\b(?:my\s+name\s*\?)\b",
    # Marathi (Devanagari & Roman Marathi)
    r"(?:माझं|माझे|माझ)\s+(?:नाव|नांव)",
    r"मी\s+कोण\s+आहे",
    r"तुम्हाला\s+माझं\s+नाव",
    r"\b(?:maz[a-z]*|majh[a-z]*)\s+(?:naav|nav)\s+(?:kay|kaay)\b",
    r"\b(?:mi\s+kon\s+ahe)\b",
    # Hindi (Devanagari & Hinglish)
    r"(?:मेरा|मेरी)\s+नाम\s+(?:क्या\s+है|बताओ|याद\s+है)",
    r"मैं\s+कौन\s+(?:हूँ|हू|हु)",
    r"मेरा\s+नाम",
    r"मेरा\s+नाम\s+जानते",
    r"\b(?:mera|meri)\s+naam\s+(?:kya\s+hai|batao|yaad\s+hai)\b",
    r"\b(?:main\s+kaun\s+hoon|main\s+kaun\s+hu)\b",
]


def detect_identity_query_intent(text: str) -> bool:
    """
    Detects if the user query is asking about their own name, identity, or personal recognition.
    """
    if not text or not isinstance(text, str):
        return False
    clean = text.strip().lower()
    for pat in IDENTITY_QUERY_PATTERNS:
        if re.search(pat, clean, re.IGNORECASE):
            return True
    return False


def build_identity_response(
    user_name: Optional[str] = None,
    age: Optional[int] = None,
    gender: Optional[str] = None,
    language: str = "en"
) -> str:
    """
    Builds a warm, personalized identity response acknowledging user's name and demographics.
    """
    lang = (language or "en").strip().lower()
    has_name = bool(user_name and str(user_name).strip())
    name = str(user_name).strip() if has_name else ""

    if has_name:
        if lang in ("mr", "roman_mr"):
            age_str = f", वय {age} वर्षे" if age and int(age) > 0 else ""
            return f"तुमचे नाव {name}{age_str} आहे. आपल्या प्रोफाइलनुसार मला तुमचे नाव आणि माहिती आठवते आहे! आज मी तुमच्या आरोग्यासाठी कशी मदत करू?"
        elif lang in ("hi", "hinglish"):
            age_str = f", उम्र {age} वर्ष" if age and int(age) > 0 else ""
            return f"आपका नाम {name}{age_str} है। आपकी प्रोफाइल और हमारी बातचीत के आधार पर मुझे यह याद है! आज मैं आपकी सेहत के लिए क्या सहायता कर सकता हूँ?"
        else:
            age_str = f", {age} years old" if age and int(age) > 0 else ""
            return f"Your name is {name}{age_str}. I remember you from your profile! How can I assist you with your health today?"
    else:
        if lang in ("mr", "roman_mr"):
            return "आपल्या सध्याच्या संभाषणात तुमचे नाव अजून नोंदवले गेलेले नाही. कृपया तुमचे नाव मला सांगू शकाल का?"
        elif lang in ("hi", "hinglish"):
            return "वर्तमान बातचीत में आपका नाम अभी दर्ज नहीं है। क्या आप कृपया मुझे अपना नाम बता सकते हैं?"
        else:
            return "I don't have your name saved in our current conversation yet. Could you please share your name with me?"


def strip_prompt_scaffolding(text: str) -> str:
    """
    Extracts only the user's current message from wrapped LLM prompts that may
    include `Recent conversation:` history or system instructions.
    """
    if not text or not isinstance(text, str):
        return ""
    extracted = text
    for marker in (
        "User's current message:",
        "Current user message:",
        "User Query:",
    ):
        if marker in extracted:
            extracted = extracted.rsplit(marker, 1)[-1].strip()
            break
    if "Recent conversation:" in extracted and "User's current message:" not in text:
        extracted = extracted.split("Recent conversation:", 1)[0].strip() or extracted
    return extracted.strip()


_LAB_BIOMARKER_NAME_RE = re.compile(
    r"\b(?:hemoglobin|haemoglobin|hb|hgb|wbc|rbc|platelets?|platelet\s+count|"
    r"glucose|fasting\s+sugar|pp\s+sugar|blood\s+sugar|hba1c|cholesterol|ldl|hdl|"
    r"triglycerides?|creatinine|urea|bun|bilirubin|sgot|sgpt|alt|ast|tsh|t3|t4|"
    r"ferritin|vitamin\s*d|vitamin\s*b12|sodium|potassium|uric\s*acid|esr|crp|"
    r"mcv|mch|mchc|hematocrit|pcv|alkaline\s+phosphatase)\b",
    re.IGNORECASE,
)

_LAB_UNIT_RE = re.compile(
    r"\b(?:\d+(?:\.\d+)?)\s*(?:mg/dl|g/dl|u/l|iu/l|ng/ml|pg/ml|mmol/l|meq/l|"
    r"cells/mcl|/mcl|/ul|k/ul|lakhs/cumm|million/cumm|%)\b",
    re.IGNORECASE,
)


def is_lab_report_context_prompt(text: str) -> bool:
    """
    Detects if the CURRENT user message contains clinical lab report context or biomarkers.
    Strips conversation history scaffolding so prior lab reports never bleed into
    unrelated follow-up symptom queries.
    """
    if not text or not isinstance(text, str):
        return False
    current = strip_prompt_scaffolding(text)
    lower = current.lower()
    if (
        "[clinical lab report context]" in lower
        or "flagged parameters:" in lower
        or "biomarker decoder" in lower
        or "[lab report context]" in lower
    ):
        return True

    # Require at least 2 distinct lab biomarker names AND lab units in the current message
    biomarker_matches = {m.group(0).lower() for m in _LAB_BIOMARKER_NAME_RE.finditer(current)}
    unit_matches = _LAB_UNIT_RE.findall(current)
    return len(biomarker_matches) >= 2 and len(unit_matches) >= 2


def is_faculty_or_staff_roster(text: str, instruction: str = "") -> bool:
    """
    Checks if a text or instruction contains faculty rosters, staff directories,
    doctor lists, or department professor names that should never be returned to patients.
    """
    combined = f"{instruction} {text}".lower().strip()
    if not combined:
        return False
    patterns = [
        r"\b(?:faculty|staff|professors?|doctors?)\s+(?:directory|list|roster|schedule)\b",
        r"\b(?:list\s+of\s+(?:doctors|professors|faculty|specialists|surgeons|physicians))\b",
        r"\bdepartment\s+of\s+[a-z\s,&]+\s+(?:doctors?\s+list|faculty|professors?)\b",
        r"\bdepartment\s+of\s+obstetrics\s+&\s+gynecology\s+doctors\b",
        r"\bhead\s+of\s+department\b",
        r"\bopd\s+doctor\s+schedule\b",
        r"\bfaculty\s+members?\b",
        r"\bfaculty\s+directory\b",
        r"\bdr\s+[a-z\s]+in\s+the\s+faculty\s+directory\b",
        r"\bdean|vice-chancellor|chancellor\b",
        r"\bwho\s+are\s+the\s+doctors\s+in\b",
        r"\bconsultant\s+list\b",
        r"\bdoctor\s+names?\s+in\b",
        r"\bdoctors?\s+in\s+[a-z\s]+\s+department\b",
        r"\bdr\s+[a-z\s]+\s+appointment\b",
        r"\bclinical\s+hematology\s+opd\s+doctor\s+schedule\b",
    ]
    for pat in patterns:
        if re.search(pat, combined):
            return True
    return False


def build_lab_report_interpretation_response(raw_text: str, language: str = "en") -> str:
    """
    Generates a structured, patient-friendly clinical interpretation of lab report biomarkers
    adhering strictly to 4 required sections:
    1) Quick Summary
    2) Clinical Significance & Normal Ranges
    3) Dietary & Lifestyle Advice
    4) Questions for Your Doctor
    Bypasses all external hospital directories, faculty lists, and raw TF-IDF matching.
    """
    lang = (language or "en").strip().lower()
    if lang in ("mr", "roman_mr"):
        target_lang = "mr"
    elif lang in ("hi", "hinglish"):
        target_lang = "hi"
    else:
        target_lang = "en"

    clean_raw_text = strip_prompt_scaffolding(raw_text)
    report = None
    if analyze_lab_report is not None:
        try:
            report = analyze_lab_report(clean_raw_text, user_language=target_lang)
        except Exception as ex:
            logger.warning("analyze_lab_report error in conversation engine: %s", ex)
            report = None

    biomarkers = report.get("biomarkers", []) if report else []
    flagged = [b for b in biomarkers if b.get("status") != "normal"]
    doctor_questions = report.get("doctor_questions", []) if report else []

    # If analyze_lab_report missed lines from explicit "Flagged Parameters:", parse regex
    if not biomarkers or len(flagged) == 0:
        param_matches = re.findall(
            r"-\s*([^:\n]+):\s*([\d.]+)\s*([^\s(\n]*)\s*(?:\([^)]*Status:\s*([a-zA-Z_]+)\))?",
            clean_raw_text,
            re.IGNORECASE
        )
        for p_name, p_val, p_unit, p_stat in param_matches:
            clean_name = p_name.strip()
            if not any(b.get("name", "").lower() == clean_name.lower() for b in biomarkers):
                try:
                    num_val = float(p_val.strip())
                except ValueError:
                    num_val = 0.0
                status_str = (p_stat or "abnormal").strip().lower()
                biomarkers.append({
                    "name": clean_name,
                    "display_name": clean_name,
                    "value": num_val,
                    "unit": p_unit.strip(),
                    "reference_range": "See standard reference values",
                    "status": status_str,
                    "layman_explanation": f"{clean_name} is outside typical standard limits ({status_str.upper()}).",
                    "dietary_advice": "Focus on a balanced diet with proper hydration and consult your physician.",
                    "what_it_is": f"Standard clinical biomarker {clean_name}.",
                })
        flagged = [b for b in biomarkers if b.get("status") != "normal"]

    lines = []

    # --- SECTION 1: Quick Summary ---
    if target_lang == "mr":
        lines.append("### १. संक्षिप्त सारांश (Quick Summary)")
        if flagged:
            for b in flagged:
                name = b.get("display_name") or b.get("name")
                val = b.get("value")
                unit = b.get("unit") or ""
                stat = b.get("status") or "असामान्य"
                stat_str = "कमी (Low)" if "low" in stat.lower() else ("जास्त (High)" if "high" in stat.lower() else stat)
                expl = b.get("layman_explanation") or ""
                lines.append(f"- **{name} ({val} {unit})**: स्थिती **{stat_str}** आहे. {expl}")
        else:
            lines.append("तपासण्यात आलेले सर्व लॅब घटक सामान्य मर्यादेत आहेत. कोणतीही चिंताजनक बाब आढळलेली नाही.")
    elif target_lang == "hi":
        lines.append("### १. त्वरित सारांश (Quick Summary)")
        if flagged:
            for b in flagged:
                name = b.get("display_name") or b.get("name")
                val = b.get("value")
                unit = b.get("unit") or ""
                stat = b.get("status") or "असामान्य"
                stat_str = "कम (Low)" if "low" in stat.lower() else ("अधिक (High)" if "high" in stat.lower() else stat)
                expl = b.get("layman_explanation") or ""
                lines.append(f"- **{name} ({val} {unit})**: स्थिति **{stat_str}** है। {expl}")
        else:
            lines.append("जांचे गए सभी लैब पैरामीटर सामान्य सीमा में हैं। कोई भी असामान्य घटक नहीं पाया गया।")
    else:
        lines.append("### 1. Quick Summary")
        if flagged:
            for b in flagged:
                name = b.get("display_name") or b.get("name")
                val = b.get("value")
                unit = b.get("unit") or ""
                stat = b.get("status") or "abnormal"
                stat_str = "LOW" if "low" in stat.lower() else ("HIGH" if "high" in stat.lower() else stat.upper())
                expl = b.get("layman_explanation") or ""
                lines.append(f"- **{name} ({val} {unit})**: Status is **{stat_str}**. {expl}")
        else:
            lines.append("All evaluated biomarkers are currently within standard reference ranges. No abnormal parameters detected.")
    lines.append("")

    # --- SECTION 2: Clinical Significance & Normal Ranges ---
    if target_lang == "mr":
        lines.append("### २. वैद्यकीय महत्त्व आणि प्रमाणित मर्यादा (Clinical Significance)")
        display_items = flagged if flagged else biomarkers
        for b in display_items:
            name = b.get("display_name") or b.get("name")
            val = b.get("value")
            unit = b.get("unit") or ""
            ref = b.get("reference_range") or "प्रमाणित मर्यादा"
            stat = b.get("status") or "सामान्य"
            stat_lbl = "मर्यादेपेक्षा कमी" if "low" in stat.lower() else ("मर्यादेपेक्षा जास्त" if "high" in stat.lower() else "सामान्य")
            what = b.get("what_it_is") or ""
            lines.append(f"- **{name}**: तुमचा रिपोर्ट = **{val} {unit}** (प्रमाणित मर्यादा: {ref}) — **{stat_lbl}**.")
            if what:
                lines.append(f"  *{what}*")
    elif target_lang == "hi":
        lines.append("### २. नैदानिक महत्व और सामान्य सीमाएं (Clinical Significance)")
        display_items = flagged if flagged else biomarkers
        for b in display_items:
            name = b.get("display_name") or b.get("name")
            val = b.get("value")
            unit = b.get("unit") or ""
            ref = b.get("reference_range") or "सामान्य सीमा"
            stat = b.get("status") or "सामान्य"
            stat_lbl = "सीमा से कम" if "low" in stat.lower() else ("सीमा से अधिक" if "high" in stat.lower() else "सामान्य")
            what = b.get("what_it_is") or ""
            lines.append(f"- **{name}**: आपका मान = **{val} {unit}** (सामान्य सीमा: {ref}) — **{stat_lbl}**.")
            if what:
                lines.append(f"  *{what}*")
    else:
        lines.append("### 2. Clinical Significance & Normal Ranges")
        display_items = flagged if flagged else biomarkers
        for b in display_items:
            name = b.get("display_name") or b.get("name")
            val = b.get("value")
            unit = b.get("unit") or ""
            ref = b.get("reference_range") or "Standard Range"
            stat = b.get("status") or "normal"
            stat_lbl = "Below Normal" if "low" in stat.lower() else ("Above Normal" if "high" in stat.lower() else "Normal")
            what = b.get("what_it_is") or ""
            lines.append(f"- **{name}**: Your Result = **{val} {unit}** (Standard Range: {ref}) — **{stat_lbl}**.")
            if what:
                lines.append(f"  *{what}*")
    lines.append("")

    # --- SECTION 3: Dietary & Lifestyle Advice ---
    if target_lang == "mr":
        lines.append("### ३. आहार आणि जीवनशैली सल्ला (Dietary & Lifestyle Advice)")
        seen_diet = set()
        for b in (flagged if flagged else biomarkers):
            diet = b.get("dietary_advice")
            name = b.get("display_name") or b.get("name")
            if diet and diet not in seen_diet:
                seen_diet.add(diet)
                lines.append(f"- **{name} साठी सल्ला**: {diet}")
        if not seen_diet:
            lines.append("- समतोल, पौष्टिक आहार घ्या, भरपूर पाणी प्या आणि दररोज ७-८ तास पुरेशी झोप घ्या.")
    elif target_lang == "hi":
        lines.append("### ३. आहार और जीवनशैली संबंधी सलाह (Dietary & Lifestyle Advice)")
        seen_diet = set()
        for b in (flagged if flagged else biomarkers):
            diet = b.get("dietary_advice")
            name = b.get("display_name") or b.get("name")
            if diet and diet not in seen_diet:
                seen_diet.add(diet)
                lines.append(f"- **{name} के लिए सलाह**: {diet}")
        if not seen_diet:
            lines.append("- संतुलित, पोषक तत्वों से भरपूर आहार लें, पर्याप्त पानी पिएं और रोजाना ७-८ घंटे की अच्छी नींद लें।")
    else:
        lines.append("### 3. Dietary & Lifestyle Advice")
        seen_diet = set()
        for b in (flagged if flagged else biomarkers):
            diet = b.get("dietary_advice")
            name = b.get("display_name") or b.get("name")
            if diet and diet not in seen_diet:
                seen_diet.add(diet)
                lines.append(f"- **For {name}**: {diet}")
        if not seen_diet:
            lines.append("- Maintain a balanced, nutrient-dense diet rich in seasonal vegetables, whole grains, and lean proteins, and stay well hydrated.")
    lines.append("")

    # --- SECTION 4: Questions for Your Doctor ---
    if target_lang == "mr":
        lines.append("### ४. डॉक्टरांना विचारण्यासाठी प्रश्न")
        qs = doctor_questions[:3] if doctor_questions else [
            "माझे हे रिपोर्ट पाहता मला काही औषधे किंवा सप्लिमेंट्स सुरू करण्याची गरज आहे का?",
            "हे घटक सामान्य करण्यासाठी आहाराव्यतिरिक्त आणखी काय काळजी घ्यावी?",
            "पुढील फॉलो-अप तपासणी कधी करावी लागेल?"
        ]
        for q in qs:
            lines.append(f"- \"{q}\"")
        lines.append("")
        lines.append("**पुढची तपासणी:** या रिपोर्टसोबत तुम्हाला थकवा, चक्कर किंवा इतर कोणती लक्षणे जाणवत आहेत का?")
        lines.append("")
        lines.append("*अस्वीकरण: मी एक एआय सहाय्यक आहे, डॉक्टर नाही. अचूक निदानासाठी डॉक्टरांचा सल्ला घ्या.*")
    elif target_lang == "hi":
        lines.append("### ४. डॉक्टर से पूछने के लिए प्रश्न")
        qs = doctor_questions[:3] if doctor_questions else [
            "मेरी इस रिपोर्ट के अनुसार क्या मुझे किसी दवा या सप्लीमेंट की आवश्यकता है?",
            "इन घटकों को सामान्य करने के लिए आहार के अलावा और क्या सावधानियां रखनी चाहिए?",
            "अगली फॉलो-अप जांच कब करानी चाहिए?"
        ]
        for q in qs:
            lines.append(f"- \"{q}\"")
        lines.append("")
        lines.append("**आगे की जानकारी:** क्या इस रिपोर्ट के साथ आपको थकान, चक्कर या कोई अन्य लक्षण महसूस हो रहे हैं?")
        lines.append("")
        lines.append("*अस्वीकरण: मैं एक एआई सहायक हूँ, डॉक्टर नहीं। सटीक निदान के लिए अपने डॉक्टर से परामर्श लें।*")
    else:
        lines.append("### 4. Recommended Questions for Your Doctor")
        qs = doctor_questions[:3] if doctor_questions else [
            "Based on these results, do I need any specific supplements or treatment adjustments?",
            "Are there follow-up tests needed to investigate these findings further?",
            "When should I repeat this lab panel?"
        ]
        for q in qs:
            lines.append(f"- \"{q}\"")
        lines.append("")
        lines.append("**Quick check:** Are you currently experiencing fatigue, dizziness, or any new symptoms alongside these results?")
        lines.append("")
        lines.append("*Disclaimer: I am an AI assistant, not a doctor. Discuss these results with your physician for formal evaluation.*")

    return "\n".join(lines)


def extract_first_name(full_name: Optional[str]) -> Optional[str]:
    """Extracts a clean patient first name, preserving respectful 'Dr. Name' if titled."""
    if not full_name or not isinstance(full_name, str):
        return None
    cleaned = re.sub(r"\s+", " ", full_name.strip())
    if not cleaned:
        return None
    parts = cleaned.split(" ")
    if parts[0].lower().rstrip(".") in ("dr", "mr", "mrs", "ms") and len(parts) >= 2:
        return f"{parts[0]} {parts[1]}" if parts[0].lower().startswith("dr") else parts[1]
    return parts[0]


def get_clean_italic_disclaimer(language: str = "en", dialect: Optional[str] = None) -> str:
    """Returns a single concise italic medical disclaimer matching the user's language/script."""
    d = (dialect or "").strip().lower()
    lang = (language or "en").strip().lower()
    if d == "roman_mr":
        return "*Note: Mi ek AI sahayyak aahe, doctor nahi. Lakshane tiklyas doctoranna bheta.*"
    if d == "hinglish":
        return "*Note: Main ek AI assistant hoon, doctor nahi. Lakshan bane rahein toh doctor se milein.*"
    if lang == "mr":
        return "*अस्वीकरण: मी एक एआय सहाय्यक आहे, डॉक्टर नाही. लक्षणे कायम राहिल्यास डॉक्टरांचा सल्ला घ्या.*"
    if lang == "hi":
        return "*अस्वीकरण: मैं एक एआई सहायक हूँ, डॉक्टर नहीं। लक्षण बने रहने पर डॉक्टर से परामर्श लें।*"
    return "*Disclaimer: I am an AI assistant, not a doctor. Consult a physician if symptoms persist.*"


def build_followup_prompt_line(
    topic: str = "general",
    language: str = "en",
    dialect: Optional[str] = None,
) -> str:
    """Returns ONE crisp contextual follow-up question in the user's language/script."""
    d = (dialect or "").strip().lower()
    lang = (language or "en").strip().lower()
    if d == "roman_mr" or lang == "roman_mr":
        return "**Chhota prashna:** Hi lakshane kiti divsanpasun aahet, ani sobat tap, sardi kiva angdukhi aahe ka?"
    if d == "hinglish" or lang == "hinglish":
        return "**Chhota sawaal:** Ye lakshan kitne dino se hain, aur kya saath mein bukhar, thakan ya dard bhi hai?"
    if lang == "mr":
        return "**पुढचा प्रश्न:** ही लक्षणे किती दिवसांपासून आहेत आणि सोबत इतर काही त्रास जाणवतोय का?"
    if lang == "hi":
        return "**अगला सवाल:** ये लक्षण कितने दिनों से हैं और क्या साथ में कोई अन्य परेशानी भी है?"
    if topic == "medicine":
        return "**Quick check:** Would you like to know typical precautions, side effects, or when to avoid this medicine?"
    return "**Quick check:** How many days have you noticed this, and are you having any other symptoms like fever or body ache?"


def build_greeting_response(
    language: str = "en",
    dialect: Optional[str] = None,
    first_name: Optional[str] = None,
    use_name: bool = False,
) -> str:
    """Builds a warm, concise greeting without injecting weather advisories or helplines."""
    d = (dialect or "").strip().lower()
    lang = (language or "en").strip().lower()
    name_token = first_name.strip() if (use_name and first_name and first_name.strip()) else None

    if d == "roman_mr" or lang == "roman_mr":
        if name_token:
            return f"Namaskar {name_token}! Aaj mi tumchya aarogyasathi kashi madat karu shakto?"
        return "Namaskar! Aaj mi tumchya aarogyasathi kashi madat karu shakto?"
    if d == "hinglish" or lang == "hinglish":
        if name_token:
            return f"Namaste {name_token}! Aaj main aapki sehat se judi kis baat mein madad kar sakta hoon?"
        return "Namaste! Aaj main aapki sehat se judi kis baat mein madad kar sakta hoon?"
    if lang == "mr":
        if name_token:
            return f"नमस्कार {name_token}! आज मी तुमच्या आरोग्यासाठी कशी मदत करू शकतो?"
        return "नमस्कार! आज मी तुमच्या आरोग्यासाठी कशी मदत करू शकतो?"
    if lang == "hi":
        if name_token:
            return f"नमस्ते {name_token}! आज मैं आपकी सेहत से जुड़ी किस बात में मदद कर सकता हूँ?"
        return "नमस्ते! आज मैं आपकी सेहत से जुड़ी किस बात में मदद कर सकता हूँ?"
    if name_token:
        return f"Hello {name_token}! How can I help with your health questions today?"
    return "Hello! How can I help with your health questions today?"


_ROMAN_CLINICAL_SYMPTOM_MAP = {
    "fever": ("tap", "taap", "bukhar", "bukhaar", "fever", "garam", "temperature", "ताप", "बुखार"),
    "cold": ("sardi", "jukam", "zukam", "zukaam", "cold", "runny nose", "naak", "सर्दी", "जुकाम"),
    "cough": ("khokla", "khansi", "khasi", "cough", "sore throat", "ghasadukhi", "ghasa", "gala", "खोकला", "खांसी"),
    "body_pain": ("angdukhi", "ang dukhat", "ang dukh", "body pain", "body ache", "bodyache", "badan dard", "myalgia", "haath paay", "अंगदुखी", "बदन दर्द"),
    "headache": ("dokedukhi", "doka dukhat", "dok dukhat", "doke", "headache", "sir dard", "migraine", "डोकेदुखी", "सिरदर्द"),
    "stomach_pain": ("potdukhi", "pot dukhat", "pot", "stomach", "pet dard", "abdominal", "पोटदुखी", "पेट दर्द"),
    "acidity": ("acidity", "pitta", "ulcer", "heartburn", "gas", "jalan", "पित्त", "एसिडिटी"),
    "vomiting": ("ulti", "ulati", "vomiting", "vomit", "malmal", "nausea", "उलटी", "उल्टी", "मळमळ"),
    "diarrhea": ("julab", "julaab", "loose motion", "diarrhea", "dast", "जुलाब", "दस्त"),
    "dizziness": ("chakkar", "garagar", "dizziness", "dizzy", "vertigo", "चक्कर"),
    "weakness": ("ashaktapana", "ashaktpana", "kamjori", "kamzori", "weakness", "fatigue", "thakava", "thakan", "अशक्तपणा", "कमजोरी"),
    "breathing_problem": ("shwas", "saans", "breathing", "breathless", "asthma", "दमा", "श्वास", "सांस"),
}

_ROMAN_TOPIC_MAP = {
    "diabetes": ("sugar", "diabetes", "madhumeh", "diabetic", "blood sugar", "मधुमेह", "शुगर"),
    "blood_pressure": ("bp", "blood pressure", "hypertension", "raktadaab", "रक्तदाब", "बीपी"),
}

_ACTION_REQUEST_PHRASES = (
    "kami karnya", "kami karaych", "kami karu", "kami kar", "madat kar",
    "upay", "ilaj", "ilaaj", "kam karne", "kam kar", "uttar", "aaram mil",
    "reduce fever", "bring down", "lower fever", "what should i take",
    "immediate relief", "right now", "tablet", "medicine for", "goli", "dawa", "aushadh",
    "कमी करण्यासाठी", "उपाय", "मदत कर", "कम करने", "इलाज",
)


def extract_roman_clinical_symptoms(text: str) -> Dict[str, Any]:
    """
    Extracts canonical symptoms, clinical topics, 'only/fakta' narrowing cues,
    and immediate action-request intent from Romanized Marathi, Hinglish, Marathi,
    Hindi, or English messages.
    """
    if not text or not isinstance(text, str):
        return {"symptoms": [], "topics": [], "is_only_narrowing": False, "is_action_request": False}

    lower = re.sub(r"\s+", " ", text.lower()).strip()
    found_symptoms = []
    for canonical, aliases in _ROMAN_CLINICAL_SYMPTOM_MAP.items():
        for alias in aliases:
            if any("\u0900" <= ch <= "\u097F" for ch in alias):
                idx = lower.find(alias)
                matched = idx != -1
                m_start, m_end = idx, idx + len(alias)
            else:
                m = re.search(r"(?<![a-z0-9])" + re.escape(alias) + r"(?![a-z0-9])", lower)
                matched = m is not None
                m_start, m_end = (m.start(), m.end()) if m else (-1, -1)
            if matched:
                # Skip if negated in a 35-char window before ("do not have fever", "no cough") or right after ("sardi nahi")
                pre_window = lower[max(0, m_start - 35):m_start]
                post_window = lower[m_end:min(len(lower), m_end + 15)]
                if re.search(r"\b(?:no|not|without|never|don't|do not|doesn't|does not|didn't|did not|haven't|hasn't|nahi|naahi|nhi)\b", pre_window):
                    break
                if re.search(r"^\s*(?:nahi|naahi|nhi|nahiye)\b", post_window):
                    break
                if canonical not in found_symptoms:
                    found_symptoms.append(canonical)
                break

    found_topics = []
    for topic, aliases in _ROMAN_TOPIC_MAP.items():
        for alias in aliases:
            if any("\u0900" <= ch <= "\u097F" for ch in alias):
                matched = alias in lower
            else:
                matched = bool(re.search(r"(?<![a-z0-9])" + re.escape(alias) + r"(?![a-z0-9])", lower))
            if matched:
                if topic not in found_topics:
                    found_topics.append(topic)
                break

    is_only_narrowing = bool(re.search(r"\b(?:fakta|fakt|phakta|only|sirf|keval|just)\b", lower))
    is_action_request = any(p in lower for p in _ACTION_REQUEST_PHRASES)

    return {
        "symptoms": found_symptoms,
        "topics": found_topics,
        "is_only_narrowing": is_only_narrowing,
        "is_action_request": is_action_request,
    }


def analyze_conversation_progression(
    raw_text: str,
    current_symptoms: list,
    recent_history: Optional[list] = None,
) -> Dict[str, Any]:
    """
    Inspects prior conversation turns so the assistant progresses naturally:
    - 'initial': first symptom report -> structured relief + 1-2 clarifying questions
    - 'clarification': user narrowed down or added details (e.g. 'fakta taap ani body pain')
      -> acknowledge ruled-out symptoms, adapt advice, ask a DIFFERENT deeper question
    - 'action_request': user asks for immediate reduction/help (e.g. 'taap kami karnya sathi madat kar')
      -> direct step-by-step action plan with NO redundant follow-up question
    """
    parsed = extract_roman_clinical_symptoms(raw_text or "")
    history = recent_history or []

    prior_user_msgs = []
    prior_assistant_msgs = []
    for item in history:
        role = str(item.get("role", "")).lower()
        msg = str(item.get("message") or item.get("content") or "")
        if not msg:
            continue
        if role == "user":
            prior_user_msgs.append(msg)
        elif role == "assistant":
            prior_assistant_msgs.append(msg)

    # Ignore current message if it's already appended at the end of recent_history
    if prior_user_msgs and raw_text and prior_user_msgs[-1].strip().lower() == raw_text.strip().lower():
        prior_user_msgs = prior_user_msgs[:-1]

    prior_syms = []
    for u_msg in prior_user_msgs:
        for s in extract_roman_clinical_symptoms(u_msg)["symptoms"]:
            if s not in prior_syms:
                prior_syms.append(s)

    curr_set = {str(s).strip().lower().replace(" ", "_") for s in (current_symptoms or []) if str(s).strip()}
    curr_set.update(parsed["symptoms"])

    asked_topics = set()
    for a_msg in prior_assistant_msgs:
        a_low = a_msg.lower()
        if any(k in a_low for k in ("kiti divas", "how many days", "kitne dino", "किती दिवसांपासून", "कितने दिनों")):
            asked_topics.add("duration")
        if any(k in a_low for k in ("thermometer", "temperature", "102", "kitne degree", "किती तापमान")):
            asked_topics.add("temperature")
        if any(k in a_low for k in ("ghasadukhi", "angdukhi", "body ache", "sore throat", "badan dard")):
            asked_topics.add("associated")

    ruled_out = []
    if prior_syms and (parsed["is_only_narrowing"] or (curr_set and set(prior_syms) - curr_set)):
        ruled_out = [s for s in prior_syms if s not in curr_set]

    if parsed["is_action_request"] and len(prior_user_msgs) >= 1:
        stage = "action_request"
    elif len(prior_user_msgs) >= 1 and (parsed["is_only_narrowing"] or ruled_out or (curr_set & set(prior_syms) != set(curr_set) and prior_syms)):
        stage = "clarification"
    else:
        stage = "initial"

    return {
        "stage": stage,
        "ruled_out": ruled_out,
        "asked_topics": asked_topics,
        "prior_turn_count": len(prior_user_msgs),
        "parsed": parsed,
    }


def build_structured_symptom_guidance(
    symptoms: list,
    raw_text: str = "",
    language: str = "en",
    dialect: Optional[str] = None,
    severity: str = "low",
    recent_history: Optional[list] = None,
) -> str:
    """
    Generates a clean, ChatGPT/Gemini-style, context-aware clinical response that:
    - Adapts dynamically across Turn 1 (initial), Turn 2 (clarification/narrowing), and Turn 3 (action request)
    - Never repeats identical canned follow-up questions across consecutive turns
    - Matches the exact language & script (Roman Marathi, Hinglish, Marathi, Hindi, English).
    """
    d = (dialect or "").strip().lower()
    lang = (language or "en").strip().lower()
    progression = analyze_conversation_progression(raw_text, symptoms, recent_history=recent_history)
    stage = progression["stage"]
    ruled_out = progression["ruled_out"]
    asked_topics = progression["asked_topics"]
    parsed = progression["parsed"]

    sym_set = {str(s).strip().lower().replace(" ", "_") for s in (symptoms or []) if str(s).strip()}
    if parsed["is_only_narrowing"] and parsed["symptoms"]:
        sym_set = set(parsed["symptoms"])
    else:
        sym_set = (sym_set | set(parsed["symptoms"])) - set(ruled_out)
    topics = set(parsed["topics"])

    has_fever = "fever" in sym_set or "high_fever" in sym_set or "mild_fever" in sym_set
    has_cold = "cold" in sym_set or "runny_nose" in sym_set or "nasal_congestion" in sym_set
    has_cough = "cough" in sym_set or "dry_cough" in sym_set or "sore_throat" in sym_set
    has_body_pain = "body_pain" in sym_set or "body_ache" in sym_set or "back_pain" in sym_set
    has_headache = "headache" in sym_set or "migraine" in sym_set
    has_stomach = bool(sym_set & {"stomach_pain", "abdominal_pain", "acidity", "vomiting", "nausea", "diarrhea"})
    has_weakness = "weakness" in sym_set or "dizziness" in sym_set

    # 1) Romanized Marathi (`roman_mr`)
    if d == "roman_mr" or lang == "roman_mr":
        # Turn 3 / Action Request: Immediate fever-reduction steps (crisp, zero redundant question)
        if stage == "action_request" and has_fever:
            return (
                "Taap lavkar kami karnyasathi aata he **pratyaksha upay (immediate steps)** kara:\n\n"
                "- **Koshmat Panyachi Patti (Lukewarm Sponging):** Kapal, man, haath ani paayavar koshmat (sadha/halka garam) panyacha kapda firva — barfa kiva atishay thand pani vapru naka.\n"
                "- **Hydration & ORS:** Dar 30–45 minitanni thode-thode koshmat pani, ORS, naral pani kiva daliche pani pya.\n"
                "- **Paracetamol Guidance:** Doctor kiva pharmacist chya sallyane samanya *Paracetamol 500mg* (adult sathi 6–8 tasanchya antarane, kahi khallyanantar) gheta yete. Rikamya poti strong painkillers (ibuprofen/diclofenac) gheu naka.\n"
                "- **Halke Suti Kapde:** Jast jad panghrun gheun gham kadhnyacha prayatna karu naka; kholi haveshir theva.\n"
                "- **Tatkal Doctor Kadhi Pahave:** Taap **103°F (39.4°C)** var gelyas, 48–72 tasant kami na jhalyas, kiva chakkar/shwas ghenyala tras jhalyas tatkal doctoranna bheta.\n\n"
                + get_clean_italic_disclaimer("mr", "roman_mr")
            )

        # Turn 2 / Clarification: Fever + Body Pain (or Fever without Cold)
        if has_fever and (has_body_pain or stage == "clarification") and not has_cold:
            ack = (
                "Samajle — tumhala sardi-khokla nahiye, fakta **taap (fever)** ani **angdukhi (body pain)** aahe. "
                if ("cold" in ruled_out or "cough" in ruled_out or has_body_pain)
                else "Tumhala **taap (fever)** ani **angdukhi (body pain)** aslyas "
            )
            followup = (
                "**Chhota prashna:** Thermometer var taap kiti degree (e.g., 100°F–102°F) disat aahe, ani sobat thandi vajun yete ka?"
                if "temperature" not in asked_topics
                else "**Chhota prashna:** Sandhyana (joints) madhe khup vedana aahet ka, kiva angavar lal chatte (rash) disat aahet ka?"
            )
            return (
                f"{ack}Ha sanjog sahasa **viral fever**, **dengue/chikungunya**, kiva **shariirik thakavyamule** disun yeto.\n\n"
                "**Fever + Body Pain Sathi Kay Karal:**\n"
                "- **Electrolytes & Fluids:** Sadha panyabarobar ORS, naral pani kiva limbu-pani pya — angdukhi ani ashaktapana kami hoto.\n"
                "- **Purna Vishranti:** Sharirala angdukhi ani viral shi ladhnyasathi aata purna aaram dya.\n"
                "- **Safe Relief:** Taap va angdukhisathi pharmacist/doctoranchya sallyane *Paracetamol* vaparta yete; *aspirin* kiva *ibuprofen* doctoranshivay gheu naka.\n"
                "- **Red Flags:** Tap **102°F+** rahilyas, sandhyana suj aalyas, dolyamage dukhlyas kiva 2–3 divsant sudhar na jhalyas blood test (CBC/Dengue) sathi doctoranna bheta.\n\n"
                f"{followup}\n\n"
                + get_clean_italic_disclaimer("mr", "roman_mr")
            )

        if has_fever and (has_cold or has_cough):
            followup = (
                "**Chhota prashna:** Ha tap ani sardi kiti divsanpasun aahe, ani thermometer var taap kiti yetoy?"
                if "duration" not in asked_topics
                else "**Chhota prashna:** Sobat angdukhi, ghasadukhi kiva thandi vajun yete ka?"
            )
            return (
                "Tumhala **tap (fever)** ani **sardi** aslyamule he samanya **viral infection** kiva hawaaman badlamule honara seasonal flu asnyachi shakyaata aahe. "
                "Yogya vishranti ani gharghuti kaljine sahasa 2–3 divsant aaram padto.\n\n"
                "**Aata Kay Karal (Home Care):**\n"
                "- **Garme ani Hydration:** Koshmat garam pani, aale-tulasicha chaha, kiva soup pya — ghasyala aaram milto ani dehydration talte.\n"
                "- **Garmechi Vaf (Steam):** Divsatun 2 vela sadhya panyachi vaf ghetlyas naak mokale hote.\n"
                "- **Pureshi Vishranti:** Sharirala infection shi ladhnyasathi pureshi jhop dya.\n\n"
                "**Aushadh va Kalji (Medicines & Precautions):**\n"
                "- **Tap va Angdukhisathi:** Pharmacist kiva doctoranchya sallyane *Paracetamol* gheta yete; doctoranshivay antibiotics gheu naka.\n"
                "- **Thand Padarth Tala:** Ice-cream, thand pani ani telkat padarth kahi divas talave.\n\n"
                "**Doctoranna Kadhi Bhetave (When to See a Doctor):**\n"
                "- Tap **102°F (38.9°C)** peksa jast rahilyas kiva **3 divsanpeksa jast** tiklyas.\n"
                "- Shwas ghenyala tras, chhatit dukhane kiva khup ashaktapana janavlyas.\n\n"
                f"{followup}\n\n"
                + get_clean_italic_disclaimer("mr", "roman_mr")
            )

        if has_fever:
            return (
                "Tumhala **taap (fever)** aslyas body infection kiva viral shi ladhat aahe he samajun ghya.\n\n"
                "**Aata Kay Karal:**\n"
                "- **Koshmat Panyachi Patti & Hydration:** Kapalavar koshmat panyachi patti theva ani ORS/pani pyat raha.\n"
                "- **Vishranti & Halka Aahar:** Purna aaram ghya ani khichdi/soup sarkha halka aahar ghya.\n"
                "- **Doctoranna Kadhi Bhetave:** Taap **102°F+** gelyas kiva **3 divsanpeksa jast** tiklyas blood checkup sathi doctoranna bheta.\n\n"
                "**Chhota prashna:** Taap kiti divsanpasun aahe ani thermometer var kiti reading aahe?\n\n"
                + get_clean_italic_disclaimer("mr", "roman_mr")
            )

        if has_body_pain:
            return (
                "**Angdukhi (body pain)** sahasa **viral thakava, dehydration, kiva snayuncha tanav** yamule hote.\n\n"
                "**Aata Kay Karal:**\n"
                "- **Hydration & Electrolytes:** Pani, ORS kiva naral pani pya; kami panyamule angdukhi vadhte.\n"
                "- **Garam Shek & Vishranti:** Dukhinarya bhagavar koshmat shek dya ani sharirala purna aaram dya.\n"
                "- **Doctoranna Kadhi Bhetave:** Angdukhi sobat unch taap, suj, kiva khup ashaktapana 2–3 divas tiklyas doctoranna dakhva.\n\n"
                "**Chhota prashna:** Hi angdukhi purna sharirat aahe ka kambar/paay ya bhagat, ani taap pan aahe ka?\n\n"
                + get_clean_italic_disclaimer("mr", "roman_mr")
            )

        if has_cough or has_cold:
            return (
                "**Sardi ani khokla** he bahuda **viral respiratory infection** kiva dhul-hawaaman allergy mule hote.\n\n"
                "**Aata Kay Karal (Home Care):**\n"
                "- **Mithachya Panyachya Gulnya:** Koshmat panyat chimutbhar mith takun divsatun 2–3 vela gulnya kara.\n"
                "- **Vaf (Steam Inhalation):** Band naak ughadnyasathi 5–10 minite vaf ghya.\n"
                "- **Koshmat Pani ani Madha:** Garme panyat ardhya chammach madha ghetlyas khoklyala aaram milto.\n\n"
                "**Doctoranna Kadhi Bhetave:**\n"
                "- Khokla **1 aathavdyapeksa jast** tiklyas, unch tap aalyas, kiva shwas laglyas doctoranna bheta.\n\n"
                "**Chhota prashna:** Tumhala khokla korda aahe ka kaf yet aahe, ani kiti divsanpasun aahe?\n\n"
                + get_clean_italic_disclaimer("mr", "roman_mr")
            )
        if has_headache:
            return (
                "**Dokedukhi (headache)** sahasa **pani kami pine, tanav, pureshi jhop na milne**, kiva sardi-sinus mule hote.\n\n"
                "**Aata Kay Karal (Home Care):**\n"
                "- **Pani Pya:** Ek glass sadhe kiva koshmat pani pya — dehydration he dokedukhiche mukhya karan aste.\n"
                "- **Shant Kholi:** Kahi vel mobile-screen pasun dur rahun shant, kami prakashachya kholit vishranti ghya.\n"
                "- **Kapalaavar Shek:** Garam kiva thand kapadyachi patti thevlyas aaram milto.\n\n"
                "**Doctoranna Kadhi Bhetave:**\n"
                "- Achanak asahya dokedukhi, drishti dhurkat hone, satat ulti kiva maan akhadlyas tatkal doctoranna bheta.\n\n"
                "**Chhota prashna:** Hi dokedukhi ekach bajula aahe ka purna dokyat, ani kiti velapasun aahe?\n\n"
                + get_clean_italic_disclaimer("mr", "roman_mr")
            )
        if has_stomach:
            return (
                "**Potdukhi kiva acidity** bahuda **apachan, jhatpat kiva tikhat jevan, kiva panyachi kamtarata** yamule hote.\n\n"
                "**Aata Kay Karal (Home Care):**\n"
                "- **Halke Jevan:** Kahi divas dal-bhat, khichdi, taak kiva kele ya sarkha sopya pachan honara aahar ghya.\n"
                "- **Thode-Thode Pani:** Thodya-thodya velane pani kiva ORS pya.\n"
                "- **Tikhat-Telkat Tala:** Chaha-coffee, tikhat ani baherache padarth talave.\n\n"
                "**Doctoranna Kadhi Bhetave:**\n"
                "- Potat asahya vedana, satat ultya, tap kiva sandasamadhe rakta dislyas tatkal doctoranna bheta.\n\n"
                "**Chhota prashna:** Potdukhi sobat acidity, ulati kiva julab hot aahet ka?\n\n"
                + get_clean_italic_disclaimer("mr", "roman_mr")
            )
        if has_weakness:
            return (
                "**Chakkar kiva ashaktapana (weakness)** sahasa **dehydration, kami BP, rakta kamtarata (low hemoglobin), kiva pureshi jhop na milne** yamule hote.\n\n"
                "**Aata Kay Karal:**\n"
                "- **Tatkal Basa / Aaram Ghya:** Chakkar yet aslyas lagech basun kiva jhopun ghya ani paay thode unch theva.\n"
                "- **ORS / Limbu-Sakhareche Pani:** Lagech pani, ORS kiva thode khajur/fale khave.\n"
                "- **Doctoranna Kadhi Bhetave:** Varinvar chakkar, gham sutne kiva beshuddh padnyasarkhe vatlyas doctoranna bheta.\n\n"
                "**Chhota prashna:** He chakkar/ashaktapana achanak suru zale aahe ka kahi divsanpasun aahe?\n\n"
                + get_clean_italic_disclaimer("mr", "roman_mr")
            )
        if "diabetes" in topics:
            return (
                "**Blood Sugar / Diabetes** niyantranat thevnyasathi niyamit aahar, vyayam ani doctoranchya sallyane aushadhe ghene mahattvache aste.\n\n"
                "**Dainandin Kalji:**\n"
                "- **Balanced Aahar:** Sakhar, maida ani god padarth talun bhajya, kaddhanya va fiber-rich aahar ghya.\n"
                "- **Niyamit Chalne:** Roz 30 minite vegane chalnyane insulin sensitivity sudharte.\n"
                "- **Regular Checkup:** Fasting, PP sugar ani HbA1c niyamit tapasun ghya.\n\n"
                "**Chhota prashna:** Tumchi sadhyachi fasting kiva PP blood sugar reading kiti aali aahe?\n\n"
                + get_clean_italic_disclaimer("mr", "roman_mr")
            )
        if "blood_pressure" in topics:
            return (
                "**Blood Pressure (BP)** santulit thevnyasathi mithache praman, tanav ani jhop yavar laksha dene garjeche aste.\n\n"
                "**Aata Kay Karal:**\n"
                "- **Shant Basun BP Tapasa:** 5 minite shant baslyanantar BP check kara; mith ani processed padarth kami kara.\n"
                "- **Red Flags:** BP **180/120+** gelyas, chhatit dukhlyas kiva drishti dhurkat jhalyas tatkal emergency madhe dakhva.\n\n"
                "**Chhota prashna:** Tumchi sadhyachi BP reading kiti aahe ani sobat dokedukhi kiva chakkar aahe ka?\n\n"
                + get_clean_italic_disclaimer("mr", "roman_mr")
            )

        sym_label = ", ".join(s.replace("_", " ") for s in sym_set) if sym_set else "lakshane"
        return (
            f"Tumchya **{sym_label}** ya lakshanansathi prathamik kalji ani nireekshan mahattvache aahe.\n\n"
            "**Aata Kay Karal (Home Care):**\n"
            "- **Vishranti ani Hydration:** Pureshi jhop ghya ani divasbhar koshmat pani/ORS pyat raha.\n"
            "- **Lakshananchi Nond:** Taap, vedana kiva tras kadhi vadhato yachi nond theva.\n"
            "- **Swatahun Aushadh Tala:** Doctoranchya sallyashivay antibiotics kiva strong painkillers gheu naka.\n\n"
            "**Doctoranna Kadhi Bhetave (When to See a Doctor):**\n"
            "- Lakshane **2–3 divsant kami na jhalyas** kiva achanak vadhlyas.\n"
            "- Unch taap, shwas ghenyala tras kiva khup ashaktapana janavlyas.\n\n"
            + build_followup_prompt_line("general", "mr", "roman_mr")
            + "\n\n"
            + get_clean_italic_disclaimer("mr", "roman_mr")
        )

    # 2) Hinglish (Roman Hindi)
    if d == "hinglish" or lang == "hinglish":
        if stage == "action_request" and has_fever:
            return (
                "Bukhar jaldi kam karne ke liye abhi ye **immediate steps** follow karein:\n\n"
                "- **Gungune Pani Ki Patti:** Maathe, haath-pair aur gardan par gungune pani se patti rakhein (barf ya bahut thanda pani na lagayein).\n"
                "- **Hydration & ORS:** Har 30–45 minute mein gunguna pani, ORS, nariyal pani ya soup piyein.\n"
                "- **Paracetamol Guidance:** Doctor ya pharmacist ki salah se *Paracetamol 500mg* (6–8 ghante ke gap par, kuch khane ke baad) li ja sakti hai; khali pet painkillers na lein.\n"
                "- **Halke Kapde & Aaram:** Zyada mote kambal odhne se bachein aur kamre mein hawa aane dein.\n"
                "- **Doctor Ko Kab Dikhayein:** Agar bukhar **103°F+** jaaye ya 48–72 ghante mein kam na ho.\n\n"
                + get_clean_italic_disclaimer("hi", "hinglish")
            )
        if has_fever and (has_body_pain or stage == "clarification") and not has_cold:
            return (
                "Samajh gaya — aapko sardi-khansi nahi hai, sirf **bukhar** aur **badan dard** hai. Ye **viral fever** ya **thakan** ka sanket ho sakta hai.\n\n"
                "**Abhi Kya Karein:**\n"
                "- **ORS & Fluids:** Pani ke saath ORS ya nariyal pani piyein taaki kamzori aur badan dard kam ho.\n"
                "- **Poora Aaram:** Shareer ko recovery ke liye poori neend dein.\n"
                "- **Dawa:** Salah se *Paracetamol* le sakte hain; bina doctor ki salah ke ibuprofen/antibiotics na lein.\n\n"
                "**Chhota sawaal:** Thermometer mein bukhar kitna aa raha hai aur kya thand lagkar bukhar aa raha hai?\n\n"
                + get_clean_italic_disclaimer("hi", "hinglish")
            )
        if has_fever and (has_cold or has_cough):
            return (
                "Aapko **bukhar (fever)** aur **sardi/zukam** hai, jo aam taur par **viral infection** ya mausam badalne ki wajah se hota hai.\n\n"
                "**Abhi Kya Karein (Home Care):**\n"
                "- **Garam Liquid Aur Hydration:** Gunguna pani, adrak-tulsi chai ya soup piyein.\n"
                "- **Steam (Bhaap):** Din mein 2 baar saade pani ki bhaap lein taaki band naak khule.\n"
                "- **Poori Neend Aur Aaram:** Shareer ko viral se ladne ke liye aaram dein.\n\n"
                "**Doctor Ko Kab Dikhayein:**\n"
                "- Agar bukhar **102°F** se zyada rahe, **3 din se zyada** chale, ya saans lene mein takleef ho.\n\n"
                "**Chhota sawaal:** Ye bukhar aur sardi kitne dino se hai, aur kya gale mein dard ya badan dard bhi hai?\n\n"
                + get_clean_italic_disclaimer("hi", "hinglish")
            )
        sym_label = ", ".join(s.replace("_", " ") for s in sym_set) if sym_set else "lakshan"
        return (
            f"Aapke **{sym_label}** ke liye aaram, hydration aur lakshanon par nazar rakhna sabse zaroori hai.\n\n"
            "**Abhi Kya Karein (Home Care):**\n"
            "- **Aaram Aur Hydration:** Poori neend lein aur thode-thode waqt par pani ya ORS piyein.\n"
            "- **Halka Bhojan:** Asaani se pachne wala ghar ka bana sada khana khayein.\n"
            "- **Bina Salah Dawa Na Lein:** Khud se antibiotics ya tez painkillers lene se bachein.\n\n"
            "**Doctor Ko Kab Dikhayein:**\n"
            "- Agar takleef **2–3 din mein kam na ho**, tez bukhar ya saans fulne lage toh turant doctor se milein.\n\n"
            + build_followup_prompt_line("general", "hi", "hinglish")
            + "\n\n"
            + get_clean_italic_disclaimer("hi", "hinglish")
        )

    # 3) Devanagari Marathi (`mr`)
    if lang == "mr":
        if has_fever and (has_cold or has_cough):
            return (
                "तुम्हाला **ताप आणि सर्दी** ही लक्षणे सामान्य **व्हायरल इन्फेक्शन** किंवा हवामान बदलामुळे (seasonal flu) असण्याची शक्यता आहे. "
                "पुरेशी विश्रांती आणि घरगुती काळजीने बहुतेक वेळा २–३ दिवसांत आराम मिळतो.\n\n"
                "**तात्काळ घरगुती उपाय (Home Care):**\n"
                "- **कोमट पाणी आणि द्रवपदार्थ:** कोमट पाणी, सूप किंवा आले-तुळशीचा चहा घ्या — घसा आणि नाकाला आराम मिळतो.\n"
                "- **वाफ घेणे (Steam):** दिवसातून २ वेळा वाफ घेतल्यास बंद नाक मोकळे होते.\n"
                "- **पुरेशी विश्रांती:** शरीराला संसर्गाशी लढण्यासाठी पुरेशी झोप द्या.\n\n"
                "**औषधे आणि काळजी (Medicines & Precautions):**\n"
                "- **तापासाठी:** डॉक्टरांच्या किंवा फार्मासिस्टच्या सल्ल्याने *Paracetamol* घेता येते; डॉक्टरांशिवाय अँटीबायोटिक्स घेऊ नका.\n"
                "- **थंड पदार्थ टाळा:** थंड पाणी, आईस्क्रीम आणि तेलकट पदार्थ काही दिवस टाळा.\n\n"
                "**डॉक्टरांना कधी भेटावे (When to See a Doctor):**\n"
                "- ताप **१०२°F पेक्षा जास्त** राहिल्यास किंवा **३ दिवसांपेक्षा जास्त** टिकल्यास.\n"
                "- श्वास घेण्यास त्रास, छातीत दुखणे किंवा तीव्र अशक्तपणा जाणवल्यास.\n\n"
                "**पुढचा प्रश्न:** हा ताप आणि सर्दी किती दिवसांपासून आहे आणि सोबत अंगदुखी किंवा घसादुखी आहे का?\n\n"
                + get_clean_italic_disclaimer("mr", None)
            )
        sym_label = ", ".join(symptoms) if symptoms else "ही लक्षणे"
        return (
            f"तुमच्या **{sym_label}** या लक्षणांसाठी प्राथमिक विश्रांती आणि लक्षणांचे निरीक्षण महत्त्वाचे आहे.\n\n"
            "**तात्काळ घरगुती उपाय (Home Care):**\n"
            "- **विश्रांती आणि पाणी:** दिवसभर पुरेसे कोमट पाणी किंवा ओआरएस घ्या आणि आराम करा.\n"
            "- **हलका आहार:** पचायला सोपा आणि ताजा घरगुती आहार घ्या.\n"
            "- **स्वतःहून औषधे टाळा:** वैद्यकीय सल्ल्याशिवाय अँटीबायोटिक्स घेऊ नका.\n\n"
            "**डॉक्टरांना कधी भेटावे (When to See a Doctor):**\n"
            "- लक्षणे **२–३ दिवसांत कमी न झाल्यास** किंवा वाढत गेल्यास.\n"
            "- तीव्र ताप, श्वास घेण्यास त्रास किंवा असह्य वेदना झाल्यास.\n\n"
            + build_followup_prompt_line("general", "mr", None)
            + "\n\n"
            + get_clean_italic_disclaimer("mr", None)
        )

    # 4) Devanagari Hindi (`hi`)
    if lang == "hi":
        if has_fever and (has_cold or has_cough):
            return (
                "आपको **बुखार और सर्दी** के लक्षण सामान्य **वायरल संक्रमण** या मौसम बदलने (seasonal flu) के कारण हो सकते हैं। "
                "सही आराम और घरेलू देखभाल से अधिकांश मामलों में २–३ दिनों में राहत मिल जाती है।\n\n"
                "**अभी क्या करें (Home Care):**\n"
                "- **गुनगुना पानी और तरल पदार्थ:** गुनगुना पानी, सूप या अदरक-तुलसी की चाय लें ताकि गले और शरीर को आराम मिले।\n"
                "- **भाप लें (Steam):** दिन में २ बार भाप लेने से बंद नाक और जकड़न में राहत मिलती है।\n"
                "- **पर्याप्त आराम:** शरीर को संक्रमण से लड़ने के लिए पूरी नींद दें।\n\n"
                "**दवा और सावधानी (Medicines & Precautions):**\n"
                "- **बुखार के लिए:** डॉक्टर या फार्मासिस्ट की सलाह से *Paracetamol* ली जा सकती है; बिना सलाह के एंटीबायोटिक्स न लें।\n"
                "- **ठंडी चीजें टालें:** ठंडा पानी, आइसक्रीम और तला-भुना खाना कुछ दिन न खाएं।\n\n"
                "**डॉक्टर को कब दिखाएं (When to See a Doctor):**\n"
                "- यदि बुखार **१०२°F से अधिक** रहे या **३ दिन से ज्यादा** बना रहे।\n"
                "- सांस लेने में तकलीफ, सीने में दर्द या बहुत अधिक कमजोरी महसूस होने पर।\n\n"
                "**अगला सवाल:** यह बुखार और सर्दी कितने दिनों से है, और क्या साथ में गले में दर्द या बदन दर्द भी है?\n\n"
                + get_clean_italic_disclaimer("hi", None)
            )
        sym_label = ", ".join(symptoms) if symptoms else "ये लक्षण"
        return (
            f"आपके **{sym_label}** के लिए पर्याप्त आराम, पानी और लक्षणों पर ध्यान देना सबसे जरूरी है।\n\n"
            "**अभी क्या करें (Home Care):**\n"
            "- **आराम और हाइड्रेशन:** पर्याप्त नींद लें और दिनभर पानी या ओआरएस पीते रहें।\n"
            "- **हल्का भोजन:** आसानी से पचने वाला ताजा घरेलू भोजन करें।\n"
            "- **बिना सलाह दवा न लें:** डॉक्टर की सलाह के बिना एंटीबायोटिक्स या तेज दर्द निवारक न लें।\n\n"
            "**डॉक्टर को कब दिखाएं (When to See a Doctor):**\n"
            "- यदि लक्षण **२–३ दिनों में कम न हों** या अचानक बढ़ जाएं।\n"
            "- तेज बुखार, सांस फूलने या गंभीर दर्द होने पर तुरंत डॉक्टर से मिलें।\n\n"
            + build_followup_prompt_line("general", "hi", None)
            + "\n\n"
            + get_clean_italic_disclaimer("hi", None)
        )

    # 5) English (`en`)
    if has_fever and (has_cold or has_cough):
        return (
            "Your **fever and cold** symptoms most commonly point to a mild **viral upper respiratory infection** (seasonal flu/common cold). "
            "With rest and fluids, most viral episodes improve within 2–3 days.\n\n"
            "**What You Can Do Now (Home Care):**\n"
            "- **Stay Hydrated:** Drink warm water, clear broths, herbal tea, or electrolyte fluids regularly.\n"
            "- **Steam & Rest:** Take warm steam inhalation twice daily to ease congestion and prioritize sleep.\n"
            "- **Lukewarm Compress:** Use a lukewarm cloth on your forehead if fever feels uncomfortable.\n\n"
            "**Medicines & Precautions:**\n"
            "- **Fever Relief:** Standard *Paracetamol (Acetaminophen)* may help reduce fever if advised by your doctor/pharmacist; avoid taking antibiotics without a prescription.\n"
            "- **Avoid Triggers:** Skip very cold beverages, smoking, and dusty air until recovered.\n\n"
            "**When to See a Doctor:**\n"
            "- Fever stays **above 102°F (38.9°C)** or lasts longer than **3 days**.\n"
            "- Shortness of breath, chest pain, severe weakness, or persistent vomiting develops.\n\n"
            "**Quick check:** How many days have you had the fever and cold, and do you also have body aches or a sore throat?\n\n"
            + get_clean_italic_disclaimer("en", None)
        )
    if has_cough or has_cold:
        return (
            "A **cold or cough** is usually triggered by a mild **viral infection** or **environmental irritation/allergies** and typically settles within a few days.\n\n"
            "**What You Can Do Now (Home Care):**\n"
            "- **Warm Salt-Water Gargles:** Gargle 2–3 times daily to soothe throat irritation.\n"
            "- **Steam & Fluids:** Inhale warm steam and sip warm water or honey-lemon tea.\n\n"
            "**When to See a Doctor:**\n"
            "- Cough persists beyond **7–10 days** or brings up blood-tinged mucus.\n"
            "- High fever, wheezing, or difficulty breathing occurs.\n\n"
            "**Quick check:** Is your cough dry or producing phlegm, and how long has it lasted?\n\n"
            + get_clean_italic_disclaimer("en", None)
        )
    if has_headache:
        return (
            "A **headache** is most often linked to **dehydration, eye strain, lack of sleep, tension, or sinus congestion**.\n\n"
            "**What You Can Do Now (Home Care):**\n"
            "- **Rehydrate & Rest:** Drink a full glass of water and rest in a quiet, dimly lit room away from screens.\n"
            "- **Warm or Cool Compress:** Apply a cool compress to your forehead or a warm compress to tight neck muscles.\n\n"
            "**When to See a Doctor:**\n"
            "- Sudden severe headache, blurred vision, confusion, or repeated vomiting.\n"
            "- Headache accompanied by high fever or neck stiffness.\n\n"
            "**Quick check:** Is the headache throbbing on one side or across the forehead, and how long has it lasted?\n\n"
            + get_clean_italic_disclaimer("en", None)
        )
    if has_stomach:
        return (
            "**Stomach discomfort or acidity** is commonly caused by **indigestion, spicy meals, mild gastritis, or dehydration**.\n\n"
            "**What You Can Do Now (Home Care):**\n"
            "- **Bland Diet & Small Sips:** Eat light foods (rice, bananas, toast) and sip water or ORS slowly.\n"
            "- **Avoid Irritants:** Skip caffeine, fried/spicy foods, and lying flat right after eating.\n\n"
            "**When to See a Doctor:**\n"
            "- Severe or worsening abdominal pain, persistent vomiting, high fever, or blood in stool.\n\n"
            "**Quick check:** Are you also having nausea, heartburn, loose stools, or cramping after meals?\n\n"
            + get_clean_italic_disclaimer("en", None)
        )

    sym_list_str = ", ".join(symptoms) if symptoms else "your symptoms"
    return (
        f"For **{sym_list_str}**, early supportive care and monitoring how your symptoms evolve is the safest first step.\n\n"
        "**What You Can Do Now (Home Care):**\n"
        "- **Rest & Hydration:** Get adequate rest and drink fluids consistently throughout the day.\n"
        "- **Track Changes:** Note when symptoms started, what relieves them, and whether temperature rises.\n"
        "- **Safe Medication Use:** Avoid self-medicating with antibiotics or high-dose painkillers without guidance.\n\n"
        "**When to See a Doctor:**\n"
        "- Symptoms worsen over **24–48 hours** or fail to improve after **3 days**.\n"
        "- You develop high fever, breathing difficulty, chest pain, or severe weakness.\n\n"
        + build_followup_prompt_line("general", "en", None)
        + "\n\n"
        + get_clean_italic_disclaimer("en", None)
    )


_LEADING_NAME_GREETING_RE = re.compile(
    r"^(?:[Hh]ello|[Hh]i|[Hh]ey|[Nn]amaste|[Nn]amaskar|नमस्ते|नमस्कार)\s+"
    r"(?:(?:Dr\.?|Mr\.?|Mrs\.?|Ms\.?)\s+)?[A-Z\u0900-\u097F][a-zA-Z\u0900-\u097F\s]{1,35}[,!:?]\s*(?:\n+)?",
)


def _strip_existing_disclaimers_and_footers(text: str) -> str:
    """Removes duplicate/unformatted disclaimer lines so one clean italic disclaimer can sit at the end."""
    cleaned_lines = []
    for line in (text or "").splitlines():
        stripped = line.strip()
        lower = stripped.lower().strip("*_ ")
        if (
            lower.startswith("disclaimer:")
            or lower.startswith("medical disclaimer:")
            or lower.startswith("अस्वीकरण:")
            or lower.startswith("note: mi ek ai")
            or lower.startswith("note: main ek ai")
            or "i am an ai medical assistant, not a doctor" in lower
            or "i am an ai assistant, not a doctor" in lower
            or "i am an ai assistant providing" in lower
            or "मी एक एआय सहाय्यक आहे, डॉक्टर नाही" in lower
            or "मी एक एआय वैद्यकीय सहाय्यक आहे" in lower
            or "मैं एक एआई सहायक हूँ, डॉक्टर नहीं" in lower
            or "मैं एक एआई मेडिकल असिस्टेंट हूँ" in lower
        ):
            continue
        cleaned_lines.append(line)
    # Trim trailing blank lines
    while cleaned_lines and not cleaned_lines[-1].strip():
        cleaned_lines.pop()
    return "\n".join(cleaned_lines).strip()


def ensure_modern_clinical_formatting(
    text: str,
    language: str = "en",
    dialect: Optional[str] = None,
    include_followup: bool = True,
    topic: str = "general",
    allow_name: bool = False,
) -> str:
    """
    Post-processes any clinical/AI response to match a clean ChatGPT/Gemini style:
    - Strips repetitive user-name greetings unless `allow_name=True`
    - Strips redundant disclaimers / walls of boilerplate
    - Ensures at most ONE crisp follow-up question at the bottom (when `include_followup=True`)
    - Appends a single clean italic disclaimer line at the bottom.
    """
    if not text or not isinstance(text, str):
        return text

    body = text.strip()
    if not allow_name:
        body = _LEADING_NAME_GREETING_RE.sub("", body, count=1).strip()

    body = _strip_existing_disclaimers_and_footers(body)
    if not body:
        return get_clean_italic_disclaimer(language, dialect)

    # Collapse 3+ consecutive newlines down to 2
    body = re.sub(r"\n{3,}", "\n\n", body)

    if include_followup:
        # Check if last non-empty line already contains a follow-up question
        non_empty = [ln.strip() for ln in body.splitlines() if ln.strip()]
        has_tail_question = bool(
            non_empty
            and (
                non_empty[-1].endswith("?")
                or non_empty[-1].lower().startswith("**quick check:")
                or non_empty[-1].lower().startswith("**chhota prashna:")
                or non_empty[-1].lower().startswith("**chhota sawaal:")
                or "**पुढचा प्रश्न:**" in non_empty[-1]
                or "**अगला सवाल:**" in non_empty[-1]
            )
        )
        if not has_tail_question:
            body = f"{body}\n\n{build_followup_prompt_line(topic, language, dialect)}"

    disclaimer = get_clean_italic_disclaimer(language, dialect)
    return f"{body}\n\n{disclaimer}"

