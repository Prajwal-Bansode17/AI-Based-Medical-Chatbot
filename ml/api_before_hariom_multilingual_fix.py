import os
import re
import sqlite3
from datetime import datetime
from pathlib import Path

import joblib
import pandas as pd
from flask import Flask, jsonify, request
from google import genai
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

# ============================================================
# PATHS
# ============================================================

BASE_DIR = Path(__file__).resolve().parent
MODEL_FILE = BASE_DIR / "models" / "medical_intent_model.joblib"
MEDICAL_KNOWLEDGE_FILE = BASE_DIR / "data" / "raw" / "medical_knowledge.csv"
CHAT_DB_FILE = BASE_DIR / "data" / "chat_history.db"


def find_mediq_dataset():
    search_root = BASE_DIR / "data" / "raw" / "mediq"

    if not search_root.exists():
        raise FileNotFoundError(f"MEDIQ folder not found: {search_root}")

    files = list(search_root.rglob("mediq_full.csv"))
    if not files:
        raise FileNotFoundError(f"mediq_full.csv not found inside: {search_root}")

    return files[0]


DATA_FILE = find_mediq_dataset()

app = Flask(__name__)


# ============================================================
# MULTILINGUAL LANGUAGE HELPERS
# ============================================================

def detect_language(text):
    """
    Detect English, Marathi, or Hindi from both native scripts and
    Roman/Latin-script speech transcription.

    Examples:
        Marathi:  मला ताप आला आहे  /  mala tap aala aahe
        Hindi:    मुझे बुखार है      /  mujhe bukhar hai
        English:  I have fever

    The Android speech recognizer may return Marathi/Hindi in Roman
    letters, so script-only detection is not sufficient.
    """
    if not text:
        return "en"

    text = str(text).strip()
    if not text:
        return "en"

    lower = text.lower()

    # ---- Devanagari script ----
    devanagari_count = sum(1 for ch in text if "\u0900" <= ch <= "\u097F")

    if devanagari_count > 0:
        marathi_markers = [
            "मला", "माझा", "माझी", "माझे", "माझ्या", "माझं", "म्हणजे", "म्हणून",
            "आहे", "आहेत", "काय", "कशी", "कसा", "कसे", "किती", "ताप", "खोकला",
            "सर्दी", "दुखत", "दुखणे", "दुखतं", "वेदना", "पोट", "डोके", "डोकं",
            "घसा", "उलटी", "गरगर", "चक्कर", "कमजोरी", "अशक्तपणा", "औषध", "गोळी",
            "काळजी", "करू", "करायचं", "घ्यावी", "घ्यावे", "घ्यावं",
        ]
        hindi_markers = [
            "मुझे", "मेरा", "मेरी", "मेरे", "मुझको", "क्या", "कैसे", "कैसा",
            "कैसी", "कितना", "कितनी", "कितने", "है", "हैं", "था", "थी", "थे",
            "बुखार", "खांसी", "जुकाम", "दर्द", "पेट", "सिर", "गला", "उल्टी",
            "चक्कर", "कमजोरी", "दवा", "गोली", "सावधानी", "करना", "करो", "करूं",
            "चाहिए",
        ]

        marathi_score = sum(1 for marker in marathi_markers if marker in text)
        hindi_score = sum(1 for marker in hindi_markers if marker in text)

        if marathi_score > hindi_score:
            return "mr"
        if hindi_score > marathi_score:
            return "hi"
        # Unknown Devanagari text: Marathi is the safer default for this
        # application's target audience.
        return "mr"

    # ---- Roman/Latin script ----
    words = set(re.findall(r"[a-zA-Z]+", lower))

    roman_marathi_words = {
        "mala", "maza", "mazi", "majha", "majhi", "majhe", "majhya", "aahe",
        "ahe", "aahat", "aahet", "kay", "kasa", "kashi", "kashe", "kiti",
        "mhanje", "mhanun", "aata", "ata", "tula", "tyala", "tyachi",
        "tyache", "zala", "zali", "zale", "zhala", "zhali", "zhalay",
        "hotay", "hota", "hoti", "hote", "dukh", "dukhat", "dukhte",
        "dukhtay", "dukhata", "tap", "taap", "khokla", "sardi", "ghasa",
        "gala", "pot", "dok", "doka", "dokyala", "chakkar", "garagar",
        "kamjori", "ashaktpana", "ulti", "malmal", "oushadha", "aushadh",
        "aushadha", "goli", "tablet", "kalji", "karu", "karaycha",
        "karaychi", "karayche", "ghyavi", "ghyava", "ghyave", "ghyav",
        "pasun", "paryant", "mi", "mee", "tumhi", "apan", "aapan",
    }
    roman_hindi_words = {
        "mujhe", "mujhko", "mera", "meri", "mere", "kya", "kaise", "kaisa",
        "kaisi", "kitna", "kitni", "kitne", "hai", "hain", "tha", "thi",
        "the", "bukhar", "khansi", "jukam", "dard", "pet", "sir", "gala",
        "ulti", "chakkar", "kamzori", "dawai", "dava", "goli", "savdhani",
        "karna", "karo", "karu", "karun", "chahiye",
    }

    marathi_score = len(words.intersection(roman_marathi_words))
    hindi_score = len(words.intersection(roman_hindi_words))

    roman_marathi_phrases = [
        "mala ", "majha ", "majhi ", "majhe ", "maza ", "mazi ", "majhya ",
        "mala kay", "mala kasa", "mala kashi", "mala kashe", "mala kay karu",
        "kay karu", "kay karaycha", "kay karaychi", "kay karayche",
        "mala aahe", "mala ahe", "majha doka", "majha dok", "majhe dok",
        "majha pot", "majhe pot", "mala tap", "mala taap", "mala khokla",
        "mala sardi", "mala chakkar", "mala kamjori", "mala dukh",
        "mala dukhat", "mi kay", "mi kay karu", "pasun", "karu",
        "karaycha", "karaychi",
    ]
    for phrase in roman_marathi_phrases:
        if phrase in lower:
            marathi_score += 2

    strong_marathi_phrases = [
        "tap aahe", "taap aahe", "khokla aahe", "sardi aahe",
        "pot dukhat", "dok dukhat", "doka dukhat", "ghasa dukhat",
        "mala bara vatat nahi", "mala bar vatat nahi", "mala kay karu",
        "mala kay karaycha",
    ]
    for phrase in strong_marathi_phrases:
        if phrase in lower:
            marathi_score += 5

    roman_hindi_phrases = [
        "mujhe ", "mujhko ", "mera ", "meri ", "mere ", "mujhe kya",
        "mujhe kaise", "mujhe kaisa", "mujhe kaisi", "mujhe bukhar",
        "mujhe khansi", "mujhe dard", "mera sir", "mere sir", "mera pet",
        "mere pet", "mujhe chakkar", "mujhe kamzori", "kya karna",
        "kya karu", "kya karna chahiye",
    ]
    for phrase in roman_hindi_phrases:
        if phrase in lower:
            hindi_score += 2

    strong_hindi_phrases = [
        "mujhe bukhar hai", "mujhe khansi hai", "mujhe dard hai",
        "mere pet mein dard", "mera sir dard", "mujhe chakkar hai",
        "mujhe kamzori hai",
    ]
    for phrase in strong_hindi_phrases:
        if phrase in lower:
            hindi_score += 5

    if marathi_score > hindi_score and marathi_score > 0:
        return "mr"
    if hindi_score > marathi_score and hindi_score > 0:
        return "hi"
    if marathi_score == hindi_score and marathi_score > 0:
        return "mr"

    return "en"


# ============================================================
# EXPLICIT OUTPUT-LANGUAGE REQUEST DETECTION
# ============================================================
#
# detect_language() above answers "what language is the user's sentence
# written in". That is NOT the same question as "what language does the
# user want the ANSWER in". A user can type/speak entirely in English and
# still ask for a Marathi answer ("I have back pain, please give me
# information in Marathi"), or type mostly in Marathi/Hindi and ask for
# the answer in a different language ("मला back pain आहे, information
# English मध्ये द्या"). This function looks specifically for the user
# NAMING a language (English/Hindi/Marathi, in either script, with or
# without connecting words like "in" / "में" / "मध्ये" / "mein" / "madhe")
# anywhere in the message. If more than one language name is mentioned,
# the one mentioned LAST wins, since that is how these requests are
# phrased in practice ("...information Marathi में दो" / "...explain it
# in Hindi") — the language name comes right before/at the request itself.

_ENGLISH_LANG_NAME_RE = re.compile(r"\benglish\b", re.IGNORECASE)
_HINDI_LANG_NAME_RE = re.compile(r"\bhindi\b", re.IGNORECASE)
_MARATHI_LANG_NAME_RE = re.compile(r"\bmarathi\b", re.IGNORECASE)

_DEVANAGARI_LANG_NAME_TOKENS = {
    "en": ["इंग्रजी", "इंग्लिश", "इंग्रज़ी"],
    "hi": ["हिंदी", "हिन्दी"],
    "mr": ["मराठी"],
}


def detect_requested_output_language(text):
    """
    Return "en" / "hi" / "mr" if the message explicitly names a response
    language (e.g. "in Marathi", "मराठीत", "Hindi में", "English मध्ये"),
    else None. None means: fall back to the detected input language.
    """
    if not text:
        return None

    text = str(text)
    mentions = []  # (position_in_text, language_code)

    for pattern, lang in (
        (_ENGLISH_LANG_NAME_RE, "en"),
        (_HINDI_LANG_NAME_RE, "hi"),
        (_MARATHI_LANG_NAME_RE, "mr"),
    ):
        for match in pattern.finditer(text):
            mentions.append((match.start(), lang))

    for lang, tokens in _DEVANAGARI_LANG_NAME_TOKENS.items():
        for token in tokens:
            search_from = 0
            while True:
                idx = text.find(token, search_from)
                if idx == -1:
                    break
                mentions.append((idx, lang))
                search_from = idx + len(token)

    if not mentions:
        return None

    mentions.sort(key=lambda item: item[0])
    return mentions[-1][1]


# Exact-phrase shortcuts for a handful of common canned inputs. This is
# intentionally narrow (full-sentence, exact match only) — it's a cheap
# first pass, not a translator. Anything else in Marathi/Hindi falls
# through to normalize_text()'s word-level substitutions, and ultimately
# to Gemini for real translation. See LANGUAGE_SUPPORT_NOTES at the
# bottom of this file for what is and isn't covered.
_MARATHI_PHRASE_SHORTCUTS = {
    "मला ताप आला आहे": "I have fever",
    "मला ताप आहे": "I have fever",
    "मला घाशी खवखवत आहे": "I have sore throat",
    "माझे डोके दुखत आहे": "I have headache",
    "माझे पोट दुखत आहे": "I have stomach pain",
    "मला खोकला येत आहे": "I have cough",
    "मला सर्दी झाली आहे": "I have cold",
}
_HINDI_PHRASE_SHORTCUTS = {
    "मुझे बुखार है": "I have fever",
    "मुझे सिरदर्द है": "I have headache",
    "मुझे पेट में दर्द है": "I have stomach pain",
    "मुझे खांसी है": "I have cough",
    "मुझे जुकाम है": "I have cold",
}


def translate_input_to_english(text):
    if not text:
        return text

    clean = str(text).strip()

    if clean in _MARATHI_PHRASE_SHORTCUTS:
        return _MARATHI_PHRASE_SHORTCUTS[clean]
    if clean in _HINDI_PHRASE_SHORTCUTS:
        return _HINDI_PHRASE_SHORTCUTS[clean]

    # Preserve everything else as-is (normalize_text() handles word-level
    # substitution downstream, and Gemini handles full translation).
    return clean


_LANGUAGE_NOTICE_PREFIX = {
    "mr": "मी दिलेल्या माहितीनुसार सामान्य माहिती देत आहे. ",
    "hi": "मैं दी गई जानकारी के आधार पर सामान्य जानकारी दे रहा हूं. ",
}


def translate_text_with_gemini(text, language):
    """
    Ask Gemini to translate `text` into `language`, changing nothing else.
    This is deliberately a plain translation prompt (not the medical
    system instruction used elsewhere) so it can't add, remove, or
    reinterpret any medical content — only re-express it.
    Returns None if Gemini isn't configured or the call fails.
    """
    if gemini_client is None or not text:
        return None

    target_language = {"hi": "Hindi", "mr": "Marathi"}.get(language)
    if target_language is None:
        return None

    prompt = (
        f"Translate the ENTIRE medical assistant response into {target_language}.\n"
        f"STRICT LANGUAGE REQUIREMENT: Every sentence, heading, bullet, label, "
        f"disclaimer, and explanatory phrase MUST be written in {target_language}. "
        f"Do NOT leave English sentences or English headings in the response. "
        f"Translate ordinary medical wording too. Keep medicine names, drug names, "
        f"standard medical abbreviations, numbers, measurements, units, and proper "
        f"names unchanged when translation would be inappropriate. Preserve every "
        f"medical fact and the original meaning exactly. Do not add or remove facts. "
        f"Return ONLY the translated response, with no commentary about translation.\n\n"
        f"SOURCE RESPONSE:\n{text}"
    )

    try:
        response = gemini_client.models.generate_content(
            model=GEMINI_MODEL,
            contents=prompt,
        )
        translated = getattr(response, "text", None)
        return translated.strip() if translated else None
    except Exception as e:
        print("Gemini translation error:", e)
        return None


def localize_answer(text, language):
    """
    Make sure `text` is actually presented in `language` before it goes
    back to the user. English passes through unchanged. For Hindi/Marathi,
    try a real Gemini translation first; if Gemini isn't configured or the
    call fails, fall back to a short localized lead-in sentence so the
    response at least acknowledges the language instead of silently
    answering in English while claiming otherwise.

    Use this on any answer that was NOT already generated by Gemini with
    a language-aware prompt (e.g. text pulled straight from the knowledge
    base, MEDIQ, or a hardcoded string) right before it's returned.
    """
    if language == "en" or not text:
        return text

    translated = translate_text_with_gemini(text, language)
    if translated:
        return translated

    prefix = _LANGUAGE_NOTICE_PREFIX.get(language, "")
    return prefix + str(text)


# ============================================================
# GEMINI AI
# ============================================================

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
GEMINI_MODEL = "gemini-3.6-flash"
gemini_client = None

if GEMINI_API_KEY:
    try:
        gemini_client = genai.Client(api_key=GEMINI_API_KEY)
        print("Gemini AI : Connected")
    except Exception as e:
        print("Gemini AI : Connection failed:", e)
else:
    print("Gemini AI : API key not configured")


def generate_gemini_medical_response(
    user_text, language="en", medical_context="", conversation_context=""
):
    if gemini_client is None:
        return None

    language_names = {"en": "English", "mr": "Marathi", "hi": "Hindi"}
    target_language = language_names.get(language, "English")

    system_instruction = f"""
You are MEDASSIST AI, a medical information assistant.

TARGET RESPONSE LANGUAGE: {target_language}

STRICT OUTPUT LANGUAGE RULE — HIGHEST PRIORITY:
- The complete final answer MUST be written in {target_language}.
- Do NOT mix English with {target_language}.
- Do NOT write an English heading followed by a {target_language} explanation.
- Translate EVERY heading, sentence, bullet point, label, warning, disclaimer,
  and explanatory phrase into {target_language}.
- If the supplied medical context is in English, translate it completely into
  {target_language}; never copy English sentences from the context.
- Use the native script for Marathi and Hindi whenever possible.
- Medicine names, drug names, standard medical abbreviations, numbers, units,
  and proper names may remain unchanged when they are standard terms.
- Never output phrases such as "Here is some general information", "Fever:",
  "Important:", "Disclaimer:", or other English prose when the target language
  is Marathi or Hindi. Translate those phrases too.

MEDICAL RULES:
- Do not diagnose.
- Give general medical information.
- Do not invent medical facts.
- Use supplied medical context when available.
- If information is insufficient, say so.
- For severe or worsening symptoms, recommend medical care.
- Never claim to replace a doctor.
- Do not introduce yourself in normal answers.
- Do not say "Hello", "Hi", "Namaste", "Namaskar", or "I am MEDASSIST AI"
  unless a greeting is explicitly requested.
- Answer the user's current question directly.
- For follow-up questions, stay focused on the active symptom or condition.
"""

    prompt = f"""
User question:
{user_text}

Medical context:
{medical_context or "No specific medical context available."}

Previous conversation:
{conversation_context or "No previous conversation context available."}

Write the FINAL answer only in {target_language}.
Every sentence and heading must be in {target_language}.
Do not copy English wording from the medical context.
Do not provide an English translation alongside the answer.
"""

    try:
        response = gemini_client.models.generate_content(
            model=GEMINI_MODEL,
            contents=system_instruction + "\n\n" + prompt,
        )
        answer = getattr(response, "text", None)
        if answer:
            answer = answer.strip()

            # Gemini is instructed to answer only in the requested language,
            # but it can occasionally copy an English heading/sentence from
            # the supplied medical context. For Marathi/Hindi, run one final
            # translation pass so the API does not return a mixed-language
            # answer.
            if language in ("mr", "hi"):
                localized = translate_text_with_gemini(answer, language)
                if localized:
                    return localized.strip()

            return answer
    except Exception as e:
        print("Gemini generation error:", e)

    return None


# ============================================================
# CHAT DATABASE
# ============================================================

def init_chat_database():
    CHAT_DB_FILE.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(CHAT_DB_FILE)
    conn.execute("""
        CREATE TABLE IF NOT EXISTS chat_history (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            session_id TEXT NOT NULL,
            role TEXT NOT NULL,
            message TEXT NOT NULL,
            symptoms TEXT,
            created_at TEXT NOT NULL
        )
    """)
    conn.commit()
    conn.close()


def save_chat_message(session_id, role, message, symptoms=None):
    conn = sqlite3.connect(CHAT_DB_FILE)
    symptom_text = ",".join(symptoms) if symptoms else ""

    conn.execute(
        """
        INSERT INTO chat_history
            (session_id, role, message, symptoms, created_at)
        VALUES (?, ?, ?, ?, ?)
        """,
        (session_id, role, message, symptom_text, datetime.now().isoformat()),
    )
    conn.commit()
    conn.close()


def get_chat_history(session_id, limit=20):
    conn = sqlite3.connect(CHAT_DB_FILE)
    conn.row_factory = sqlite3.Row

    rows = conn.execute(
        """
        SELECT role, message, symptoms, created_at
        FROM chat_history
        WHERE session_id = ?
        ORDER BY id DESC
        LIMIT ?
        """,
        (session_id, limit),
    ).fetchall()
    conn.close()

    return [dict(row) for row in reversed(rows)]


def build_conversation_context(session_id, limit=12):
    """Build recent conversation context for Gemini follow-ups."""
    history = get_chat_history(session_id, limit=limit)
    if not history:
        return ""

    lines = []
    for item in history:
        role = str(item.get("role", "")).strip().lower()
        message = str(item.get("message", "")).strip()
        if not message:
            continue

        if role == "user":
            prefix = "User"
        elif role == "assistant":
            prefix = "MEDASSIST AI"
        else:
            prefix = role.title() or "Message"

        lines.append(f"{prefix}: {message}")

    return "\n".join(lines)


def get_previous_symptoms(session_id):
    history = get_chat_history(session_id, limit=20)
    symptoms = []

    for item in history:
        stored = item.get("symptoms", "")
        if not stored:
            continue

        for symptom in stored.split(","):
            symptom = symptom.strip()
            if symptom and symptom not in symptoms:
                symptoms.append(symptom)

    return symptoms


def clear_chat_history(session_id):
    conn = sqlite3.connect(CHAT_DB_FILE)
    conn.execute("DELETE FROM chat_history WHERE session_id = ?", (session_id,))
    conn.commit()
    conn.close()


# ============================================================
# STRUCTURED MEDICAL MEMORY
# ============================================================

def init_medical_memory_table():
    conn = sqlite3.connect(CHAT_DB_FILE)
    conn.execute("""
        CREATE TABLE IF NOT EXISTS medical_memory (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            session_id TEXT NOT NULL,
            memory_type TEXT NOT NULL,
            name TEXT NOT NULL,
            value TEXT,
            unit TEXT,
            duration TEXT,
            source_message TEXT,
            created_at TEXT NOT NULL
        )
    """)
    conn.commit()
    conn.close()


def save_medical_memory(
    session_id, memory_type, name, value=None, unit=None, duration=None,
    source_message="",
):
    conn = sqlite3.connect(CHAT_DB_FILE)
    conn.execute(
        """
        INSERT INTO medical_memory
            (session_id, memory_type, name, value, unit, duration,
             source_message, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """,
        (
            session_id,
            memory_type,
            name,
            str(value) if value is not None else "",
            str(unit) if unit is not None else "",
            str(duration) if duration is not None else "",
            source_message,
            datetime.now().isoformat(),
        ),
    )
    conn.commit()
    conn.close()


def get_medical_memory(session_id, limit=50):
    conn = sqlite3.connect(CHAT_DB_FILE)
    conn.row_factory = sqlite3.Row

    rows = conn.execute(
        """
        SELECT memory_type, name, value, unit, duration, source_message, created_at
        FROM medical_memory
        WHERE session_id = ?
        ORDER BY id DESC
        LIMIT ?
        """,
        (session_id, limit),
    ).fetchall()
    conn.close()

    return [dict(row) for row in reversed(rows)]


def clear_medical_memory(session_id):
    conn = sqlite3.connect(CHAT_DB_FILE)
    conn.execute("DELETE FROM medical_memory WHERE session_id = ?", (session_id,))
    conn.commit()
    conn.close()


init_chat_database()
init_medical_memory_table()


# ============================================================
# LOAD ML MODEL / DATASETS
# ============================================================

print("Loading ML model...")
if not MODEL_FILE.exists():
    raise FileNotFoundError(f"ML model not found: {MODEL_FILE}")
model = joblib.load(MODEL_FILE)
print("ML model loaded successfully.")

print("Loading MEDIQ dataset...")
dataset = pd.read_csv(DATA_FILE)
print("MEDIQ dataset loaded successfully.")
print("----------------------------------------")
print("Dataset shape:", dataset.shape)
print("----------------------------------------")

print("Loading Medical Knowledge Base...")
if not MEDICAL_KNOWLEDGE_FILE.exists():
    raise FileNotFoundError(f"Medical Knowledge file not found: {MEDICAL_KNOWLEDGE_FILE}")
medical_knowledge = pd.read_csv(MEDICAL_KNOWLEDGE_FILE)
print("Medical Knowledge Base loaded successfully.")
print("----------------------------------------")
print("Medical Knowledge records:", len(medical_knowledge))
print("----------------------------------------")


def find_column(columns, possible_names):
    mapping = {str(column).strip().lower(): column for column in columns}
    for name in possible_names:
        if name.lower() in mapping:
            return mapping[name.lower()]
    return None


QUESTION_COLUMN = find_column(dataset.columns, ["question", "text", "query", "user_question"])
ANSWER_COLUMN = find_column(dataset.columns, ["answer", "response", "bot_answer", "reply"])
CATEGORY_COLUMN = find_column(dataset.columns, ["category", "intent", "topic"])

if QUESTION_COLUMN is None:
    raise ValueError("Question column not found.")
if ANSWER_COLUMN is None:
    raise ValueError("Answer column not found.")


# ============================================================
# TEXT NORMALIZATION
# ============================================================

# NOTE ON MULTILINGUAL SUPPORT: everything downstream (symptom detection,
# medical-query detection, MEDIQ retrieval) runs the text through this
# function first. The single-word substitutions below are what let
# Romanized *and* Devanagari-script Marathi/Hindi symptom words get
# recognized by the rest of the (English-built) pipeline — this is doing
# real work, not just cosmetic cleanup.
_NORMALIZE_REPLACEMENTS = {
    # Romanized Marathi / Hindi
    "pn": "also",
    "aahe": "have",
    "ahe": "have",
    "mala": "i",
    "majha": "my",
    "maza": "my",
    "majhi": "my",
    "mujhe": "i",
    "mera": "my",
    "meri": "my",
    "mere": "my",
    "kay": "what",
    "kya": "what",
    "bukhar": "fever",
    "tap": "fever",
    "taap": "fever",
    "sardi": "cold",
    "khokla": "cough",
    "khansi": "cough",
    "kamjori": "weakness",
    "kamzori": "weakness",
    "ashaktpana": "weakness",
    "chakkar": "dizziness",
    "dukht": "pain",
    "dukh": "pain",
    "dard": "pain",
    # Devanagari script (Marathi + Hindi)
    "आहे": "have",
    "आहेत": "have",
    "मला": "i",
    "माझा": "my",
    "माझी": "my",
    "माझे": "my",
    "मुझे": "i",
    "मेरा": "my",
    "मेरी": "my",
    "मेरे": "my",
    "काय": "what",
    "क्या": "what",
    "ताप": "fever",
    "बुखार": "fever",
    "सर्दी": "cold",
    "जुकाम": "cold",
    "खोकला": "cough",
    "खांसी": "cough",
    "कमजोरी": "weakness",
    "अशक्तपणा": "weakness",
    "चक्कर": "dizziness",
    "दुखत": "pain",
    "दुखणे": "pain",
    "दुखतं": "pain",
    "दर्द": "pain",
}

# Devanagari is outside a-z/0-9, so any character-class filter used on
# this text must include the Devanagari block (\u0900-\u097F) — otherwise
# Marathi/Hindi script input gets silently stripped down to nothing
# before it ever reaches symptom or intent detection.
_WORD_CHAR_PATTERN = re.compile(r"[^a-zA-Z0-9\u0900-\u097F]")
_FINAL_CHAR_PATTERN = re.compile(r"[^a-z0-9\s\u0900-\u097F]")
_WHITESPACE_PATTERN = re.compile(r"\s+")

_SPELLING_MAP = {
    "fevr": "fever",
    "fvr": "fever",
    "feaver": "fever",
    "fevar": "fever",
    "hedache": "headache",
    "headche": "headache",
    "headpain": "headache",
    "caugh": "cough",
    "coff": "cough",
    "vomitng": "vomiting",
    "vomitting": "vomiting",
    "diarhea": "diarrhea",
    "diarrhoea": "diarrhea",
    "dizines": "dizziness",
    "dizzyness": "dizziness",
    "weaknes": "weakness",
    "stomac": "stomach",
    "stomache": "stomach",
    "throate": "throat",
}

_PHRASE_MAP = {
    "head pain": "headache",
    "head ache": "headache",
    "head is paining": "headache",
    "body pain": "bodypain",
    "body ache": "bodypain",
    "high temperature": "fever",
    "throwing up": "vomiting",
    "feeling sick": "nausea",
    "sore throat": "sorethroat",
    "stomach ache": "stomachpain",
    "stomach pain": "stomachpain",
    "chest pain": "chestpain",
    "back pain": "backpain",
    "joint pain": "jointpain",
    "difficulty breathing": "breathing",
    "problem breathing": "breathing",
}


def normalize_text(text):
    text = str(text).lower()

    words = text.split()
    normalized_words = []
    for word in words:
        clean_word = _WORD_CHAR_PATTERN.sub("", word)
        clean_word = _NORMALIZE_REPLACEMENTS.get(clean_word, clean_word)
        normalized_words.append(clean_word)
    text = " ".join(normalized_words)

    words = text.split()
    words = [_SPELLING_MAP.get(word, word) for word in words]
    text = " ".join(words)

    for old, new in _PHRASE_MAP.items():
        text = text.replace(old, new)

    text = _FINAL_CHAR_PATTERN.sub(" ", text)
    text = _WHITESPACE_PATTERN.sub(" ", text)
    return text.strip()


# ============================================================
# CLEAN MEDIQ
# ============================================================

dataset = dataset.copy()
dataset[QUESTION_COLUMN] = dataset[QUESTION_COLUMN].fillna("").astype(str).str.strip()
dataset[ANSWER_COLUMN] = dataset[ANSWER_COLUMN].fillna("").astype(str).str.strip()
dataset = dataset[(dataset[QUESTION_COLUMN] != "") & (dataset[ANSWER_COLUMN] != "")].copy()
dataset.reset_index(drop=True, inplace=True)
dataset["_question_clean"] = dataset[QUESTION_COLUMN].apply(normalize_text)


# ============================================================
# TF-IDF
# ============================================================

print("Creating MEDIQ retrieval index...")
vectorizer = TfidfVectorizer(
    lowercase=True, stop_words="english", ngram_range=(1, 2), min_df=1
)
retrieval_matrix = vectorizer.fit_transform(dataset["_question_clean"])
print("MEDIQ retrieval index ready.")


# ============================================================
# SYMPTOM ALIASES
# ============================================================

SYMPTOM_ALIASES = {
    "fever": ["fever", "fevr", "fvr", "feaver", "temperature", "bukhar"],
    "headache": ["headache", "headpain", "head pain", "head ache", "head is paining", "hedache"],
    "cough": ["cough", "caugh", "coff", "khokla"],
    "cold": ["cold", "sardi", "running nose", "runny nose"],
    "body_pain": ["body pain", "bodypain", "body ache", "bodyache"],
    "stomach_pain": ["stomach pain", "stomachpain", "stomach ache", "stomachache"],
    "chest_pain": ["chest pain", "chestpain", "chest ache"],
    "back_pain": ["back pain", "backpain"],
    "joint_pain": ["joint pain", "jointpain"],
    "vomiting": ["vomiting", "vomit", "vomitting", "throwing up"],
    "nausea": ["nausea", "feeling sick", "sick feeling"],
    "diarrhea": ["diarrhea", "diarhea", "loose motion", "loose motions"],
    "dizziness": ["dizziness", "dizines", "dizzyness", "chakkar"],
    "weakness": ["weakness", "weaknes", "kamjori", "tired", "fatigue"],
    "breathing_problem": [
        "breathing", "breathless", "difficulty breathing", "problem breathing",
        "shortness of breath",
    ],
    "sore_throat": ["sore throat", "sorethroat", "throat pain", "throat ache"],
    "rash": ["rash", "skin rash", "itchy rash"],
    "swelling": ["swelling", "swollen"],
}


def extract_symptoms(text):
    normalized = normalize_text(text)
    detected = []

    for symptom, aliases in SYMPTOM_ALIASES.items():
        for alias in aliases:
            if normalize_text(alias) in normalized:
                if symptom not in detected:
                    detected.append(symptom)
                break

    return detected


MEDICAL_KEYWORDS = {
    "symptom", "symptoms", "disease", "illness", "medicine", "medication",
    "tablet", "doctor", "hospital", "treatment", "diagnosis", "pain",
    "fever", "headache", "cough", "cold", "vomiting", "nausea", "weakness",
    "dizziness", "rash", "swelling", "breathing", "infection", "blood",
    "health", "healthy", "pregnancy", "diabetes", "cancer", "asthma",
    "allergy", "emergency", "injury", "wound", "dose", "dosage",
}


def is_medical_query(text):
    normalized = normalize_text(text)
    words = set(normalized.split())

    if words.intersection(MEDICAL_KEYWORDS):
        return True
    if extract_symptoms(text):
        return True
    if extract_medical_measurements(text):
        return True

    return False


FOLLOWUP_PATTERNS = [
    "since", "for", "from", "days", "day", "hours", "hour", "weeks", "week",
    "months", "month", "yes", "no", "also", "still", "now", "today",
    "yesterday", "same", "worse", "better", "what should i do",
    "what can i do", "what to do", "what care", "care should i take",
    "what precautions", "precautions", "how should i care",
    "how to take care", "home care", "treatment", "remedy", "relief",
    "it is", "its", "this", "that",
]


def is_followup_query(text):
    query = normalize_text(text)
    return any(pattern in query for pattern in FOLLOWUP_PATTERNS)


INFORMATION_PATTERNS = [
    "what are the symptoms", "symptoms of", "symptoms for", "signs of",
    "symptom of", "what symptoms", "symptoms",
]

ASSISTANCE_PATTERNS = [
    "i have", "i am having", "i feel", "i am feeling", "suffering from",
    "experiencing", "what should i do", "what can i do", "what to do",
    "what care", "care should i take", "what precautions", "precautions",
    "how should i care", "how to take care", "home care", "treatment",
    "remedy", "relief", "mala", "majha", "maza", "mujhe", "mere",
]


def get_question_type(text):
    query = normalize_text(text)

    if any(pattern in query for pattern in INFORMATION_PATTERNS):
        return "symptoms"
    if any(pattern in query for pattern in ASSISTANCE_PATTERNS):
        return "assistance"

    return "symptoms"


def get_knowledge_response(symptom, question_type):
    rows = medical_knowledge[medical_knowledge["symptom"].astype(str).str.strip() == symptom]
    if rows.empty:
        return None

    matching = rows[
        rows["question_type"].astype(str).str.strip().str.lower() == question_type
    ]
    if matching.empty:
        matching = rows
    if matching.empty:
        return None

    return str(matching.iloc[0]["response"]).strip()


def build_multi_symptom_response(symptoms, question_type):
    responses = []
    for symptom in symptoms:
        response = get_knowledge_response(symptom, question_type)
        if response:
            responses.append((symptom, response))

    if not responses:
        return None

    symptom_names = [symptom.replace("_", " ") for symptom, _ in responses]

    if len(symptom_names) == 1:
        intro = f"Here is some general information about {symptom_names[0]}."
    elif len(symptom_names) == 2:
        intro = (
            f"You mentioned {symptom_names[0]} and {symptom_names[1]}. "
            "These symptoms can occur together for several reasons, and "
            "symptoms alone cannot confirm a specific disease."
        )
    else:
        intro = (
            "You mentioned multiple symptoms. These symptoms can have "
            "different causes, and symptoms alone cannot confirm a "
            "specific disease."
        )

    sections = [
        f"{symptom.replace('_', ' ').title()}:\n{response}"
        for symptom, response in responses
    ]

    combined = intro + "\n\n" + "\n\n".join(sections)
    combined += (
        "\n\nImportant: This information is for general guidance and does "
        "not provide a diagnosis. If your symptoms are severe, worsening, "
        "persistent, or you develop difficulty breathing, confusion, "
        "fainting, severe chest pain, or another emergency symptom, seek "
        "urgent medical care."
    )

    return combined


DURATION_PATTERNS = [
    (r"(\d+)\s*(day|days)", "day", "days"),
    (r"(\d+)\s*(hour|hours)", "hour", "hours"),
    (r"(\d+)\s*(week|weeks)", "week", "weeks"),
    (r"(\d+)\s*(month|months)", "month", "months"),
]


def extract_duration(text):
    query = normalize_text(text)

    for pattern, singular, plural in DURATION_PATTERNS:
        match = re.search(pattern, query)
        if match:
            number = match.group(1)
            unit = singular if number == "1" else plural
            return f"{number} {unit}"

    return None


MEASUREMENT_PATTERNS = [
    (
        "hemoglobin",
        r"(?:hemoglobin|haemoglobin|hb)\s*(?:is|of|around|about|=|:)?\s*"
        r"(\d+(?:\.\d+)?)\s*(g\s*/?\s*dl|gdl)?",
        "g/dL",
    ),
    (
        "blood_pressure",
        r"(?:blood pressure|bp)\s*(?:is|of|around|about|=|:)?\s*"
        r"(\d{2,3})\s*(?:/|over)\s*(\d{2,3})",
        "mmHg",
    ),
    (
        "temperature",
        r"(?:temperature|temp)\s*(?:is|of|around|about|=|:)?\s*"
        r"(\d+(?:\.\d+)?)\s*(f|c|fahrenheit|celsius)?",
        "",
    ),
    (
        "blood_sugar",
        r"(?:blood sugar|sugar|glucose)\s*(?:is|of|around|about|=|:)?\s*"
        r"(\d+(?:\.\d+)?)\s*(mg\s*/?\s*dl|mgdl)?",
        "mg/dL",
    ),
    (
        "heart_rate",
        r"(?:heart rate|pulse)\s*(?:is|of|around|about|=|:)?\s*(\d{2,3})\s*(?:bpm)?",
        "bpm",
    ),
    (
        "oxygen_saturation",
        r"(?:oxygen saturation|spo2|sao2|oxygen level)\s*"
        r"(?:is|of|around|about|=|:)?\s*(\d{2,3})\s*(?:%)?",
        "%",
    ),
    (
        "weight",
        r"(?:weight)\s*(?:is|of|around|about|=|:)?\s*"
        r"(\d+(?:\.\d+)?)\s*(kg|kgs|kilograms|lb|lbs)?",
        "",
    ),
]


def extract_medical_measurements(text):
    query = normalize_text(text)
    results = []

    for name, pattern, default_unit in MEASUREMENT_PATTERNS:
        match = re.search(pattern, query, re.IGNORECASE)
        if not match:
            continue

        if name == "blood_pressure":
            value = f"{match.group(1)}/{match.group(2)}"
            unit = default_unit
        else:
            value = match.group(1)
            unit = match.group(2) if (match.lastindex or 0) >= 2 and match.group(2) else default_unit
            if name == "hemoglobin":
                unit = "g/dL"
            elif name == "blood_sugar":
                unit = "mg/dL"

        results.append({
            "memory_type": "measurement",
            "name": name,
            "value": value,
            "unit": unit,
            "duration": None,
        })

    return results


def build_measurement_response(measurements):
    lines = ["I have recorded the health measurement(s) you reported:"]

    for item in measurements:
        name = item["name"].replace("_", " ").title()
        value = item.get("value") or ""
        unit = item.get("unit") or ""
        lines.append(f"- {name}: {value} {unit}".strip())

    lines.append(
        "These values are stored as information you provided. They do not "
        "by themselves establish a diagnosis. If you want, you can also "
        "tell me your symptoms, duration, age, or other relevant health "
        "information."
    )

    return "\n".join(lines)


def save_structured_memories(session_id, text, symptoms, duration):
    for symptom in symptoms:
        save_medical_memory(
            session_id=session_id, memory_type="symptom", name=symptom,
            duration=duration, source_message=text,
        )

    measurements = extract_medical_measurements(text)
    for item in measurements:
        save_medical_memory(
            session_id=session_id,
            memory_type=item["memory_type"],
            name=item["name"],
            value=item["value"],
            unit=item["unit"],
            duration=item["duration"],
            source_message=text,
        )

    return measurements


def get_recent_memory_summary(session_id, limit=20):
    memories = get_medical_memory(session_id, limit=limit)

    symptoms = []
    latest_measurements = {}

    for item in memories:
        if item["memory_type"] == "symptom":
            key = (item["name"], item.get("duration") or "")
            existing_keys = [(x["name"], x.get("duration") or "") for x in symptoms]
            if key not in existing_keys:
                symptoms.append(item)

        elif item["memory_type"] == "measurement":
            # get_medical_memory() is newest-first internally. Keep only
            # the latest value for each measurement so an old value such
            # as 10 g/dL cannot be shown together with a newer 11 g/dL.
            measurement_name = normalize_text(item.get("name", ""))
            if measurement_name not in latest_measurements:
                latest_measurements[measurement_name] = item

    measurements = list(reversed(list(latest_measurements.values())))
    return {"symptoms": symptoms, "measurements": measurements}


def build_memory_recall_response(session_id):
    summary = get_recent_memory_summary(session_id, limit=30)
    symptom_items = summary["symptoms"]
    measurement_items = summary["measurements"]

    if not symptom_items and not measurement_items:
        return (
            "I do not have any structured health information saved for "
            "this session yet. You can tell me your symptoms, duration, "
            "or health measurements."
        )

    parts = ["Here is the health information you previously shared with me:"]

    if symptom_items:
        symptom_lines = []
        for item in symptom_items:
            name = item["name"].replace("_", " ")
            duration = item.get("duration") or ""
            if duration:
                symptom_lines.append(f"- {name.title()} (you reported it for {duration})")
            else:
                symptom_lines.append(f"- {name.title()}")
        parts.append("Symptoms:\n" + "\n".join(symptom_lines))

    if measurement_items:
        measurement_lines = []
        for item in measurement_items:
            name = item["name"].replace("_", " ").title()
            value = item.get("value") or ""
            unit = item.get("unit") or ""
            measurement_lines.append(f"- {name}: {value} {unit}".strip())
        parts.append("Reported measurements:\n" + "\n".join(measurement_lines))

    parts.append(
        "This is a record of information you reported; it is not a "
        "diagnosis. If you are concerned about a result or symptom, "
        "consult a qualified healthcare professional."
    )

    return "\n\n".join(parts)


MEASUREMENT_ALIASES = {
    "hemoglobin": ["hemoglobin", "haemoglobin", "hb"],
    "blood_pressure": ["blood pressure", "bp"],
    "temperature": ["temperature", "temp"],
    "blood_sugar": ["blood sugar", "sugar", "glucose"],
    "heart_rate": ["heart rate", "pulse"],
    "oxygen_saturation": ["oxygen saturation", "spo2", "oxygen level"],
    "weight": ["weight"],
}


def _find_requested_memory_name(query):
    for symptom, aliases in SYMPTOM_ALIASES.items():
        for alias in [symptom] + aliases:
            alias_normalized = normalize_text(alias)
            if alias_normalized and alias_normalized in query:
                return symptom

    for name, aliases in MEASUREMENT_ALIASES.items():
        for alias in aliases:
            alias_normalized = normalize_text(alias)
            if alias_normalized and alias_normalized in query:
                return name

    return None


def build_specific_memory_response(session_id, text):
    query = normalize_text(text)
    memories = get_medical_memory(session_id, limit=100)
    if not memories:
        return None

    requested_name = _find_requested_memory_name(query)
    if requested_name is None:
        return None

    requested_normalized = normalize_text(requested_name)
    matching = [
        item for item in memories
        if normalize_text(item.get("name", "")) == requested_normalized
    ]

    display_name = requested_name.replace("_", " ")

    if not matching:
        return f"I don't have any saved information about {display_name} from this session."

    display_name_title = display_name.title()
    symptom_items = [item for item in matching if item.get("memory_type") == "symptom"]
    measurement_items = [item for item in matching if item.get("memory_type") == "measurement"]

    # For a specific measurement recall, show only the latest saved value.
    # This prevents an older value (for example 10) from being repeated
    # after the user has reported a newer value (for example 11).
    if measurement_items:
        measurement_items = [measurement_items[-1]]

    lines = [f"Yes. You previously told me about your {display_name.lower()}."]

    for item in symptom_items:
        duration = item.get("duration") or ""
        if duration:
            lines.append(f"- You reported {display_name.lower()} for {duration}.")
        else:
            lines.append(f"- You reported {display_name.lower()}.")

    for item in measurement_items:
        value = item.get("value") or ""
        unit = item.get("unit") or ""
        lines.append(f"- Your reported {display_name_title} was {value} {unit}.".strip())

    lines.append(
        "This is based only on information you previously reported. It is "
        "not a diagnosis."
    )

    return "\n".join(lines)


def build_followup_response(session_id, text):
    history = get_chat_history(session_id, limit=20)
    if not history:
        return None

    previous_symptoms = get_previous_symptoms(session_id)
    duration = extract_duration(text)

    if duration and previous_symptoms:
        names = [s.replace("_", " ") for s in previous_symptoms]
        if len(names) == 1:
            symptom_text = names[0]
        elif len(names) == 2:
            symptom_text = f"{names[0]} and {names[1]}"
        else:
            symptom_text = ", ".join(names[:-1]) + f", and {names[-1]}"

        return (
            f"You previously mentioned {symptom_text}, and now you've "
            f"indicated that this has been present for about {duration}. "
            "Persistent or worsening symptoms should be assessed by a "
            "qualified healthcare professional. Please monitor your "
            "symptoms and seek medical care if they become severe or you "
            "develop concerning symptoms."
        )

    query = normalize_text(text)

    if query in {"yes", "yeah", "yep"}:
        return (
            "Thank you for confirming. Please provide any additional "
            "symptoms, how long they have been present, or any relevant "
            "information so I can provide general guidance."
        )

    if query in {"no", "nope"}:
        return (
            "Understood. Please continue monitoring your symptoms. If "
            "they become severe, worsen, or new concerning symptoms "
            "develop, seek medical attention."
        )

    return None


def general_fallback(text):
    query = normalize_text(text)

    if "mobile phone" in query or ("mobile" in query and "phone" in query):
        return (
            "A mobile phone is a portable electronic device used for "
            "communication, internet access, applications, photography "
            "and other digital services."
        )

    if "what is computer" in query or "what is a computer" in query:
        return (
            "A computer is an electronic device that processes data and "
            "performs tasks according to instructions given by software."
        )

    if "what is python" in query:
        return (
            "Python is a high-level programming language commonly used "
            "for web development, data analysis, automation, artificial "
            "intelligence and machine learning."
        )

    return (
        "I can mainly help with medical and health-related questions. "
        "You can ask me about symptoms, diseases, general health "
        "information, medicines, or when medical attention may be needed."
    )


BAD_ANSWER_PATTERNS = [
    "sorry please ask another", "appropriate question", "ask another question",
    "unable to provide", "cannot provide any information",
    "i do not have the information", "i dont have the information",
]


def is_bad_answer(answer):
    text = normalize_text(answer)
    return any(pattern in text for pattern in BAD_ANSWER_PATTERNS)


def determine_final_intent(text, model_intent, confidence):
    query = normalize_text(text)
    words = set(query.split())
    symptoms = extract_symptoms(text)

    if "opd" in words or "outpatient" in words:
        return "opd"

    if words.intersection({"emergency", "unconscious", "critical", "trauma"}):
        return "emergency"

    if symptoms and any(pattern in query for pattern in ASSISTANCE_PATTERNS):
        return "symptom_assistance"

    if symptoms and any(pattern in query for pattern in INFORMATION_PATTERNS):
        return "symptom_information"

    if symptoms:
        return "symptom_assistance"

    return str(model_intent)


def retrieve_mediq_answer(user_text, final_intent):
    query = normalize_text(user_text)
    query_vector = vectorizer.transform([query])
    similarities = cosine_similarity(query_vector, retrieval_matrix)[0]

    candidates = []
    for index in range(len(dataset)):
        answer = str(dataset.iloc[index][ANSWER_COLUMN])
        if is_bad_answer(answer):
            continue

        score = float(similarities[index])

        if CATEGORY_COLUMN is not None:
            category = normalize_text(dataset.iloc[index][CATEGORY_COLUMN])
            if category == normalize_text(final_intent):
                score += 0.20

        candidates.append((index, score))

    if not candidates:
        return None

    candidates.sort(key=lambda item: item[1], reverse=True)
    best_index, best_score = candidates[0]
    answer = str(dataset.iloc[best_index][ANSWER_COLUMN])

    return {
        "answer": answer,
        "similarity": round(min(best_score, 1.0), 4),
        "source": "mediq",
    }


# ============================================================
# ROUTES
# ============================================================

@app.get("/")
def home():
    return jsonify({
        "status": "success",
        "service": "MEDASSIST AI Medical NLP API",
        "message": "API is running",
        "model_loaded": True,
        "dataset_loaded": True,
        "medical_knowledge_loaded": True,
        "chat_memory": "SQLite",
        "mediq_records": int(len(dataset)),
        "medical_knowledge_records": int(len(medical_knowledge)),
    })


@app.get("/health")
def health():
    return jsonify({
        "status": "healthy",
        "model_loaded": True,
        "dataset_loaded": True,
        "medical_knowledge_loaded": True,
        "chat_memory": "SQLite",
    })


@app.get("/history/<session_id>")
def history(session_id):
    return jsonify({
        "status": "success",
        "session_id": session_id,
        "history": get_chat_history(session_id, limit=50),
    })


@app.delete("/history/<session_id>")
def delete_history(session_id):
    clear_chat_history(session_id)
    return jsonify({
        "status": "success",
        "message": "Chat history cleared",
        "session_id": session_id,
    })


@app.get("/memory/<session_id>")
def memory(session_id):
    return jsonify({
        "status": "success",
        "session_id": session_id,
        "memory": get_medical_memory(session_id, limit=100),
    })


@app.delete("/memory/<session_id>")
def delete_memory(session_id):
    clear_medical_memory(session_id)
    return jsonify({
        "status": "success",
        "message": "Structured medical memory cleared",
        "session_id": session_id,
    })


MEMORY_QUESTION_WORDS = [
    "remember", "previously", "earlier", "before", "told", "said",
    "shared", "reported", "gave", "mentioned",
]

MEMORY_RECALL_PATTERNS = [
    "what did i tell you before", "what did i tell you earlier",
    "what did i say before", "what did i say earlier",
    "what did i tell you about my health",
    "what did i tell you about my health before",
    "what did i tell you about my health earlier",
    "what did i say about my health", "what did i say about my health before",
    "what health information did i give you",
    "what health information did i tell you",
    "what health information did i share",
    "what did i share about my health",
    "what did i share with you before", "what did i share with you earlier",
    "tell me my health history", "show me my health history",
    "my previous symptoms", "my past symptoms",
    "my previous health", "my past health", "my health history",
    "previous health information", "past health information",
    "do you remember my symptoms", "do you remember my health",
    "do you remember my health history", "do you remember what i told you",
    "do you remember what i said", "what do you remember about my health",
    "what do you remember about me",
]

CARE_PHRASES = [
    "what should i do", "what can i do", "what to do", "what care",
    "care should i take", "what precautions", "precautions",
    "how should i care", "how to take care", "home care", "treatment",
    "remedy", "relief",
]


@app.post("/predict")
def predict():
    data = request.get_json(silent=True)
    if not data:
        return jsonify({"status": "error", "message": "Request body is required"}), 400

    text = data.get("text")
    if not text or not isinstance(text, str):
        return jsonify({"status": "error", "message": "text field is required"}), 400

    text = text.strip()
    if not text:
        return jsonify({"status": "error", "message": "text cannot be empty"}), 400

    # Auto-detect the spoken/typed language up front. `analysis_text` is
    # what the ML/NLP pipeline actually reasons over; `text` (original,
    # in whatever language/script it arrived in) is kept for storage,
    # memory recall, and Gemini prompts. See LANGUAGE_SUPPORT_NOTES below
    # for exactly what this covers.
    detected_language = detect_language(text)

    # requested_output_language is set only when the user EXPLICITLY named
    # a response language (e.g. "in Marathi", "Hindi में"). When present it
    # always wins over the language the sentence happens to be written in.
    # final_language is what actually goes into every "language" field in
    # the JSON response and into every Gemini/localization call below —
    # detected_language is used only to decide how to pre-process the
    # input text (analysis_text), never for the response language.
    requested_output_language = detect_requested_output_language(text)
    final_language = requested_output_language or detected_language

    analysis_text = (
        translate_input_to_english(text)
        if detected_language in {"mr", "hi"}
        else text
    )

    session_id = str(data.get("session_id", "default_user")).strip() or "default_user"

    try:
        model_prediction = str(model.predict([analysis_text])[0])

        confidence = 0.0
        if hasattr(model, "predict_proba"):
            try:
                probabilities = model.predict_proba([analysis_text])[0]
                confidence = float(max(probabilities))
            except Exception:
                confidence = 0.0

        final_intent = determine_final_intent(analysis_text, model_prediction, confidence)
        symptoms = extract_symptoms(analysis_text)
        duration = extract_duration(analysis_text)
        measurements = extract_medical_measurements(analysis_text)
        previous_symptoms = get_previous_symptoms(session_id)
        conversation_context = build_conversation_context(session_id, limit=12)
        medical_query = is_medical_query(analysis_text)
        normalized_query = normalize_text(text)

        # ----------------------------------------------------
        # SPECIFIC MEMORY RECALL
        # e.g. "Do you remember my fever?" / "What was my BP?"
        # Runs before generic memory and MEDIQ.
        # ----------------------------------------------------
        has_memory_language = any(
            word in normalized_query.split() for word in MEMORY_QUESTION_WORDS
        )

        specific_memory_answer = None
        # A NEW measurement must always be treated as a current report, not
        # a recall of an older saved value (e.g. "my hemoglobin is 11 g/dL").
        if has_memory_language and not measurements:
            specific_memory_answer = build_specific_memory_response(session_id, text)

        if specific_memory_answer:
            localized_answer = localize_answer(specific_memory_answer, final_language)

            save_chat_message(session_id, "user", text, [])
            save_chat_message(session_id, "assistant", localized_answer, previous_symptoms)

            return jsonify({
                "status": "success",
                "session_id": session_id,
                "question": text,
                "intent": "memory_recall",
                "confidence": round(confidence, 4),
                "symptoms": previous_symptoms,
                "medical_query": True,
                "answer": localized_answer,
                "answer_similarity": 1.0,
                "source": "structured_memory",
                "language": final_language,
            })

        # ----------------------------------------------------
        # GENERAL MEMORY RECALL
        # ----------------------------------------------------
        if any(pattern in normalized_query for pattern in MEMORY_RECALL_PATTERNS):
            memory_answer = build_memory_recall_response(session_id)
            localized_answer = localize_answer(memory_answer, final_language)

            save_chat_message(session_id, "user", text, [])
            save_chat_message(session_id, "assistant", localized_answer, previous_symptoms)

            return jsonify({
                "status": "success",
                "session_id": session_id,
                "question": text,
                "intent": "memory_recall",
                "confidence": round(confidence, 4),
                "symptoms": previous_symptoms,
                "medical_query": True,
                "answer": localized_answer,
                "answer_similarity": 1.0,
                "source": "structured_memory",
                "language": final_language,
            })

        # ----------------------------------------------------
        # FOLLOW-UP
        # ----------------------------------------------------
        if is_followup_query(text) and not symptoms and previous_symptoms:
            followup_context = ""
            if any(phrase in normalized_query for phrase in CARE_PHRASES):
                followup_context = build_multi_symptom_response(
                    previous_symptoms, "assistance"
                ) or ""

            followup_answer = generate_gemini_medical_response(
                user_text=text,
                language=final_language,
                medical_context=followup_context,
                conversation_context=conversation_context,
            )

            if not followup_answer:
                followup_answer = build_followup_response(session_id, text)
                if followup_answer:
                    followup_answer = localize_answer(followup_answer, final_language)

            if followup_answer:
                save_chat_message(session_id, "user", text, [])
                save_chat_message(session_id, "assistant", followup_answer, previous_symptoms)

                return jsonify({
                    "status": "success",
                    "session_id": session_id,
                    "question": text,
                    "intent": "follow_up",
                    "confidence": round(confidence, 4),
                    "symptoms": previous_symptoms,
                    "previous_symptoms": previous_symptoms,
                    "medical_query": True,
                    "answer": followup_answer,
                    "answer_similarity": 1.0,
                    "source": "conversation_memory",
                    "language": final_language,
                })

        # ----------------------------------------------------
        # GENERAL (non-medical) QUERY
        # ----------------------------------------------------
        if not medical_query:
            gemini_answer = generate_gemini_medical_response(
                user_text=text,
                language=final_language,
                medical_context="",
                conversation_context=conversation_context,
            )

            if gemini_answer:
                save_chat_message(session_id, "user", text, symptoms)
                save_chat_message(session_id, "assistant", gemini_answer, symptoms)

                return jsonify({
                    "status": "success",
                    "session_id": session_id,
                    "question": text,
                    "intent": "general",
                    "confidence": round(confidence, 4),
                    "symptoms": symptoms,
                    "medical_query": False,
                    "answer": gemini_answer,
                    "answer_similarity": 1.0,
                    "source": "gemini",
                    "language": final_language,
                })

            answer = localize_answer(general_fallback(analysis_text), final_language)

            save_chat_message(session_id, "user", text, symptoms)
            save_chat_message(session_id, "assistant", answer, symptoms)

            return jsonify({
                "status": "success",
                "session_id": session_id,
                "question": text,
                "intent": "general",
                "confidence": round(confidence, 4),
                "symptoms": symptoms,
                "medical_query": False,
                "answer": answer,
                "answer_similarity": 1.0,
                "source": "general_fallback",
                "language": final_language,
            })

        # ----------------------------------------------------
        # MEDICAL RESPONSE (symptom-based)
        # ----------------------------------------------------
        if symptoms:
            question_type = get_question_type(text)
            medical_answer = build_multi_symptom_response(symptoms, question_type)

            # The knowledge-base answer is supplied to Gemini as factual
            # context; Gemini rewrites/explains it in the user's language.
            gemini_medical_answer = None
            if medical_answer:
                gemini_medical_answer = generate_gemini_medical_response(
                    user_text=text,
                    language=final_language,
                    medical_context=medical_answer,
                    conversation_context=conversation_context,
                )

            if gemini_medical_answer:
                final_answer = gemini_medical_answer
            elif medical_answer:
                final_answer = localize_answer(medical_answer, final_language)
            else:
                final_answer = None

            if final_answer:
                save_chat_message(session_id, "user", text, symptoms)
                save_structured_memories(
                    session_id=session_id, text=text, symptoms=symptoms, duration=duration
                )
                save_chat_message(session_id, "assistant", final_answer, symptoms)

                return jsonify({
                    "status": "success",
                    "session_id": session_id,
                    "question": text,
                    "intent": final_intent,
                    "confidence": round(confidence, 4),
                    "symptoms": symptoms,
                    "previous_symptoms": previous_symptoms,
                    "duration": duration,
                    "measurements": measurements,
                    "medical_query": True,
                    "answer": final_answer,
                    "answer_similarity": 1.0,
                    "source": "gemini" if gemini_medical_answer else "medical_knowledge",
                    "language": final_language,
                })

        # ----------------------------------------------------
        # MEDIQ FALLBACK
        # ----------------------------------------------------
        mediq_result = retrieve_mediq_answer(analysis_text, model_prediction)

        if mediq_result is not None:
            answer = mediq_result["answer"]

            gemini_mediq_answer = generate_gemini_medical_response(
                user_text=text,
                language=final_language,
                medical_context=answer,
                conversation_context=conversation_context,
            )

            if gemini_mediq_answer:
                answer = gemini_mediq_answer
            else:
                answer = localize_answer(answer, final_language)

            save_structured_memories(
                session_id=session_id, text=text, symptoms=symptoms, duration=duration
            )
            save_chat_message(session_id, "user", text, symptoms)
            save_chat_message(session_id, "assistant", answer, symptoms)

            return jsonify({
                "status": "success",
                "session_id": session_id,
                "question": text,
                "intent": final_intent,
                "confidence": round(confidence, 4),
                "symptoms": symptoms,
                "previous_symptoms": previous_symptoms,
                "duration": duration,
                "measurements": measurements,
                "medical_query": True,
                "answer": answer,
                "answer_similarity": mediq_result["similarity"],
                "source": "gemini" if gemini_mediq_answer else "mediq",
                "language": final_language,
            })

        # ----------------------------------------------------
        # SAFE FALLBACK
        # ----------------------------------------------------
        answer = localize_answer(
            "I could not find a suitable answer in my current medical "
            "knowledge base. Please consult a qualified healthcare "
            "professional for personalized medical advice.",
            final_language,
        )

        save_structured_memories(
            session_id=session_id, text=text, symptoms=symptoms, duration=duration
        )
        save_chat_message(session_id, "user", text, symptoms)
        save_chat_message(session_id, "assistant", answer, symptoms)

        return jsonify({
            "status": "success",
            "session_id": session_id,
            "question": text,
            "intent": final_intent,
            "confidence": round(confidence, 4),
            "symptoms": symptoms,
            "previous_symptoms": previous_symptoms,
            "medical_query": True,
            "answer": answer,
            "answer_similarity": 0.0,
            "source": "safe_fallback",
            "language": final_language,
        })

    except Exception as e:
        return jsonify({
            "status": "error",
            "message": "Prediction failed",
            "error": str(e),
        }), 500


# ============================================================
# LANGUAGE_SUPPORT_NOTES
# ============================================================
#
# What "auto-detect and respond in that language" covers now:
#
# 1. detect_language() looks at every incoming message (script +
#    Romanized word/phrase scoring) and returns "en" / "hi" / "mr".
# 2. normalize_text() used to silently strip all Devanagari characters
#    before symptom/intent detection ever saw them (its character
#    filters only kept a-z0-9). That's fixed here, and the word-level
#    substitution table now covers common symptom words in Devanagari
#    as well as Romanized script, so single-word symptom mentions in
#    either script are recognized correctly by extract_symptoms() /
#    is_medical_query().
# 3. localize_answer() is now applied to every hardcoded / knowledge-base
#    / MEDIQ answer that ISN'T already generated by Gemini in the target
#    language, so every JSON response's "language" field now matches the
#    actual language of its "answer" text. Previously several branches
#    (structured memory recall in particular) always answered in English
#    regardless of detected_language, and a couple of others claimed a
#    language in the response without actually translating the text.
#
# What this does NOT fully solve, honestly:
#
# - Free-form Devanagari or Romanized sentences beyond the recognized
#   single symptom words still won't be understood by the ML classifier
#   or MEDIQ's TF-IDF retrieval (both are trained on English text) —
#   those paths still rely on Gemini to carry the conversation. If
#   GEMINI_API_KEY isn't set, non-English input outside the exact
#   phrase shortcuts / recognized symptom words will mostly land in
#   general_fallback() or the safe fallback, just now at least
#   correctly labeled/localized rather than silently English.
# - localize_answer()'s Gemini path makes one extra API call for any
#   fallback branch that hits it in a non-English conversation (mainly
#   memory recall right now, since Gemini wasn't wired into those
#   branches before) — a latency/cost tradeoff worth knowing about.
# - The Devanagari/Romanized translations added throughout this file
#   are limited to common symptom vocabulary and haven't been reviewed
#   by a native speaker; worth a sanity check before this goes in front
#   of real users.


if __name__ == "__main__":
    print("----------------------------------------")
    print("MEDASSIST AI API")
    print("----------------------------------------")
    print("Model        : Loaded")
    print("MEDIQ        : Loaded")
    print("Medical KB   : Loaded")
    print("Chat Memory  : SQLite")
    print("Chat DB      :", CHAT_DB_FILE)
    print("MEDIQ Records:", len(dataset))
    print("Medical KB Records:", len(medical_knowledge))
    print("API Port     : 5000")
    print("----------------------------------------")

    app.run(host="0.0.0.0", port=5000, debug=False)