import http.client
import json
import logging
import os
import re
import sqlite3
import sys
import threading
import time
import uuid
from datetime import datetime
from pathlib import Path
from urllib.parse import urlparse

# Reconfigure console streams to UTF-8 on Windows to safely handle Devanagari (Hindi/Marathi)
if hasattr(sys.stdout, "reconfigure"):
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="backslashreplace")
    except Exception:
        pass
if hasattr(sys.stderr, "reconfigure"):
    try:
        sys.stderr.reconfigure(encoding="utf-8", errors="backslashreplace")
    except Exception:
        pass

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("medassist_api")

import joblib
import numpy as np
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

def detect_language(text, fallback_lang=None):
    """
    Detect English, Marathi, or Hindi from both native scripts and
    Roman/Latin-script speech transcription.
    Preserves explicitly requested or established conversation language
    when reliable Devanagari detection is ambiguous.
    """
    if not text:
        return fallback_lang if fallback_lang in {"mr", "hi", "en"} else "en"

    text = str(text).strip()
    if not text:
        return fallback_lang if fallback_lang in {"mr", "hi", "en"} else "en"

    lower = text.lower()

    # ---- Devanagari script ----
    devanagari_words = set(re.findall(r"[\u0900-\u097F]+", text))
    if devanagari_words:
        # ळ is uniquely Marathi in Devanagari
        if any("ळ" in w for w in devanagari_words):
            return "mr"

        marathi_markers = {
            "मला", "माझा", "माझी", "माझे", "माझ्या", "माझं", "म्हणजे", "म्हणून",
            "आहे", "आहेत", "नाही", "काय", "कशी", "कसा", "कसे", "किती", "ताप", "खोकला",
            "सर्दी", "दुखत", "दुखणे", "दुखतं", "वेदना", "पोट", "डोके", "डोकं",
            "घसा", "उलटी", "गरगर", "चक्कर", "कमजोरी", "अशक्तपणा", "औषध", "गोळी",
            "काळजी", "करू", "करायचं", "घ्यावी", "घ्यावे", "घ्यावं", "झाले", "झाली",
            "झाला", "होते", "होती", "होता", "करावा", "करावी", "करावे", "सांगा",
            "पहा", "करा", "या", "त्या", "वरून", "पासून", "पर्यंत", "मध्ये", "साठी",
            "तसेच", "कृपया", "विश्रांती", "भरपूर", "प्या", "डोकेदुखी", "पोटदुखी",
        }
        hindi_markers = {
            "मुझे", "मेरा", "मेरी", "मेरे", "मुझको", "क्या", "कैसे", "कैसा",
            "कैसी", "कितना", "कितनी", "कितने", "है", "हैं", "था", "थी", "थे",
            "होगा", "होगी", "होंगे", "बुखार", "खांसी", "जुकाम", "दर्द", "पेट",
            "सिर", "गला", "उल्टी", "चक्कर", "कमजोरी", "दवा", "गोली", "सावधानी",
            "करना", "करो", "करूं", "चाहिए", "और", "का", "के", "की", "को", "से",
            "में", "पर", "ने", "सकते", "सकती", "सकता", "करें", "लें", "दें",
            "पिएं", "खाएं", "रहें", "जाएं", "बताएं", "रखें", "लीजिए", "कीजिए",
            "आप", "आपका", "आपकी", "आपके", "यह", "वह", "इस", "उस", "इन", "उन",
            "अगर", "यदि", "तो", "नहीं", "न", "भी", "बहुत", "ज्यादा", "कम",
            "जब", "तब", "समय", "डॉक्टर", "इलाज", "मरीज", "उपचार", "शरीर",
            "लक्षण", "पानी", "घोल", "आराम", "बचें", "उबालकर", "तनाव", "बढ़ें",
            "ताजे", "फल", "भोजन", "सिरदर्द",
        }

        marathi_score = len(devanagari_words.intersection(marathi_markers))
        hindi_score = len(devanagari_words.intersection(hindi_markers))

        if marathi_score > hindi_score:
            return "mr"
        if hindi_score > marathi_score:
            return "hi"
        # When ambiguous, preserve established conversation language
        if fallback_lang in {"mr", "hi"}:
            return fallback_lang
        return "hi"

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
        "mde", "madhe", "vr", "var", "dkhvl", "dakhvl", "dakhav",
        "bgh", "bagh", "bhai", "yar", "phile", "pahile", "punha",
        "toch", "tyach", "kr", "kru", "kraycha", "kraychi", "krtoy", "krte",
        "yetoy", "yeto", "hotoy", "hot", "disat", "dist", "distoy", "nahi",
    }
    roman_hindi_words = {
        "mujhe", "mujhko", "mera", "meri", "mere", "kya", "kaise", "kaisa",
        "kaisi", "kitna", "kitni", "kitne", "hai", "hain", "tha", "thi",
        "the", "bukhar", "khansi", "jukam", "zukaam", "dard", "pet", "sir", "gala",
        "ulti", "chakkar", "kamzori", "thakan", "dawai", "dava", "goli", "savdhani",
        "karna", "karo", "karu", "karun", "chahiye", "main", "bhi",
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

    if marathi_score > hindi_score and marathi_score >= 2:
        return "mr"
    if hindi_score > marathi_score and hindi_score >= 2:
        return "hi"
    if marathi_score == hindi_score and marathi_score >= 2:
        return "mr"

    return "en"


_ENGLISH_LANG_NAME_RE = re.compile(r"\benglish\b", re.IGNORECASE)
_HINDI_LANG_NAME_RE = re.compile(r"\bhindi\b", re.IGNORECASE)
_MARATHI_LANG_NAME_RE = re.compile(r"\bmarathi\b", re.IGNORECASE)

_DEVANAGARI_LANG_NAME_TOKENS = {
    "en": ["इंग्रजी", "इंग्लिश", "इंग्रज़ी"],
    "hi": ["हिंदी", "हिन्दी"],
    "mr": ["मराठी"],
}


def detect_requested_output_language(text):
    if not text:
        return None

    text = str(text)
    mentions = []

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

    return clean


def _english_word_count(text):
    if not text:
        return 0
    return len(re.findall(r"\b[A-Za-z]{2,}\b", str(text)))


ALLOWED_CLINICAL_LATIN = {
    "ors", "bp", "ecg", "mri", "ct", "icu", "iv", "mg", "ml", "dr", "doctor",
    "paracetamol", "ibuprofen", "aspirin", "azithromycin", "amoxicillin", "cetirizine",
    "crocin", "dolo", "combiflam", "cough", "syrup", "tablet", "tablets", "capsule",
    "capsules", "injection", "spo2", "pulse", "glucose", "sugar", "sugar_level", "hba1c",
    "cbc", "covid", "hiv", "who", "opd", "ent", "o2", "vit", "vitamin", "b12", "d3",
}

ROMAN_INDIC_MARKERS = {
    # Hindi markers
    "mujhe", "mujhko", "mera", "meri", "mere", "kya", "kaise", "kaisa",
    "kaisi", "kitna", "kitni", "kitne", "hai", "hain", "tha", "thi",
    "the", "bukhar", "khansi", "jukam", "zukaam", "dard", "pet", "sir", "gala",
    "ulti", "chakkar", "kamzori", "thakan", "dawai", "dava", "goli", "savdhani",
    "karna", "karo", "karu", "karun", "chahiye", "main", "aaram", "paani",
    "peena", "peeyo", "hoga", "hogi", "rahe", "rahein", "hota", "hoti",
    "hote", "batao", "bataiye", "sambhav", "upchar", "gharelu", "ilaj",
    "accha", "theek", "din", "se", "ko", "aur", "le", "sakte", "sakti",
    # Marathi markers
    "mala", "maza", "mazi", "majha", "majhi", "majhe", "majhya", "aahe",
    "ahe", "aahat", "aahet", "kay", "kasa", "kashi", "kashe", "kiti",
    "mhanje", "mhanun", "aata", "ata", "tula", "tyala", "tyachi",
    "tyache", "zala", "zali", "zale", "zhala", "zhali", "zhalay",
    "hotay", "hota", "hoti", "hote", "dukh", "dukhat", "dukhte",
    "dukhtay", "dukhata", "tap", "taap", "khokla", "sardi", "ghasa",
    "dok", "doka", "dokyala", "garagar", "ashaktpana", "malmal",
    "oushadha", "aushadh", "aushadha", "kalji", "karu", "karaycha",
    "karaychi", "karayche", "ghyavi", "ghyava", "ghyave", "ghyav",
    "pasun", "paryant", "mde", "madhe", "dakhvl", "dakhav", "bagh",
    "pahile", "punha", "toch", "kraycha", "kraychi", "krtoy", "yetoy",
    "yeto", "disat", "dist", "distoy", "nahi", "viskranti", "bharpur",
    "pya",
}


def _looks_mixed_with_english(text, language):
    if language not in {"mr", "hi"} or not text:
        return False

    value = str(text).strip()
    lower = value.lower()

    strong_english_phrases = (
        "here is some general information",
        "general information about",
        "clinical overview for",
        "common symptoms",
        "important:",
        "seek medical attention",
        "consult a healthcare professional",
        "this information is for general guidance",
        "does not provide a diagnosis",
        "what you can do",
        "when to seek",
        "seek urgent medical care",
    )
    if any(phrase in lower for phrase in strong_english_phrases):
        return True

    devanagari_chars = len(re.findall(r"[\u0900-\u097F]", value))
    latin_words = [w for w in re.findall(r"\b[a-zA-Z]{2,}\b", lower)]

    # If text is written in Devanagari script:
    if devanagari_chars >= 15:
        # Filter out clinical terms and medical acronyms (e.g., ORS, Paracetamol, BP, Doctor)
        non_clinical = [w for w in latin_words if w not in ALLOWED_CLINICAL_LATIN]
        common_english_leaks = {
            "here", "is", "some", "general", "information", "about", "the",
            "this", "for", "guidance", "does", "not", "provide", "diagnosis",
            "seek", "attention", "healthcare", "professional", "what", "you",
            "can", "do", "when", "to", "tell", "me", "and", "we", "will",
            "go", "step", "by", "step", "should", "your", "have", "with", "from",
        }
        leaks = [w for w in non_clinical if w in common_english_leaks]
        # Flag only if significant non-clinical English leakage occurs
        return len(leaks) >= 3 or len(non_clinical) >= 8

    # If text has no/little Devanagari (Latin script):
    # Check if it's Roman Hindi or Roman Marathi
    latin_tokens = set(latin_words)
    if latin_tokens.intersection(ROMAN_INDIC_MARKERS):
        # Valid Romanized Hindi/Marathi response; not an English leak
        return False

    # Otherwise, it's genuine English text being served for a hi/mr request
    common_english_words = {
        "the", "and", "is", "in", "it", "you", "that", "he", "was", "for", "on",
        "are", "as", "with", "his", "they", "at", "be", "this", "have", "from",
        "or", "one", "had", "by", "word", "but", "not", "what", "all", "were",
        "we", "when", "your", "can", "said", "there", "use", "an", "each", "which",
        "she", "do", "how", "their", "if", "will", "up", "other", "about", "out",
        "many", "then", "them", "these", "so", "some", "her", "would", "make",
        "like", "him", "into", "time", "has", "look", "two", "more", "write",
        "go", "see", "number", "no", "way", "could", "people", "my", "than",
        "first", "water", "been", "call", "who", "oil", "its", "now", "find",
        "symptoms", "treatment", "pain", "fever", "cough", "doctor", "care",
    }
    return len(latin_tokens.intersection(common_english_words)) >= 2 or len(latin_words) >= 4


_TRANSLATION_TARGETS = {"hi": "Hindi", "mr": "Marathi"}


def _build_translation_prompt(text, target_language):
    return (
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


def translate_text_with_ollama(text, language):
    target_language = _TRANSLATION_TARGETS.get(language)
    if target_language is None or not text:
        return None

    return ollama_chat(
        "You are a precise medical translator. You only translate; you never "
        "add, remove or change medical facts.",
        _build_translation_prompt(text, target_language),
        temperature=0.1,
    )


def translate_text_with_gemini(text, language):
    if gemini_client is None or not text:
        return None

    target_language = _TRANSLATION_TARGETS.get(language)
    if target_language is None:
        return None

    try:
        response = gemini_client.models.generate_content(
            model=GEMINI_MODEL,
            contents=_build_translation_prompt(text, target_language),
        )
        translated = getattr(response, "text", None)
        return translated.strip() if translated else None
    except Exception as e:
        print("Gemini translation error:", e)
        return None


def translate_text_with_ai(text, language):
    for translator in (translate_text_with_ollama, translate_text_with_gemini):
        translated = translator(text, language)
        if translated and not _looks_mixed_with_english(translated, language):
            return translated.strip()
    return None


def localize_answer(text, language):
    if not text:
        return text

    if language == "en":
        return str(text).strip()

    value = str(text).strip()

    # Validate the requested language as well as English leakage. Ollama can
    # occasionally answer in Marathi when Hindi was requested, or vice versa.
    wrong_language = detect_language(value, fallback_lang=language) != language
    if not _looks_mixed_with_english(value, language) and not wrong_language:
        return value

    translated = translate_text_with_ai(value, language)
    if (
        translated
        and not _looks_mixed_with_english(translated, language)
        and detect_language(translated, fallback_lang=language) == language
    ):
        return translated.strip()

    # Translation failed or wasn't available.
    # Preserve safe medical warnings and guidance rather than silently replacing with a generic failure message!
    dev_count = len(re.findall(r"[\u0900-\u097F]", value))
    if dev_count >= 15:
        # Strip any English introductory line if present, while preserving all Devanagari medical content
        lines = value.split("\n")
        cleaned_lines = [
            l for l in lines
            if not any(phrase in l.lower() for phrase in (
                "clinical overview for",
                "here is some general information",
                "general information about"
            ))
        ]
        cleaned = "\n".join(cleaned_lines).strip()
        if cleaned:
            return cleaned
        return value

    # If it is Roman Hindi/Marathi, preserve it
    lower_words = set(re.findall(r"\b[a-zA-Z]{2,}\b", value.lower()))
    if lower_words.intersection(ROMAN_INDIC_MARKERS):
        return value

    # If it is an English answer and translation failed, return value to preserve
    # all safe medical guidance and warnings rather than giving a canned failure.
    if value:
        return value

    if language == "mr":
        return (
            "या प्रश्नासाठी मला सध्या पूर्ण आणि अचूक माहिती देता आली नाही. "
            "कृपया तुमची लक्षणे, ती किती वेळापासून आहेत आणि इतर काही त्रास "
            "असल्यास ते सांगा. गंभीर किंवा वाढणारी लक्षणे असल्यास डॉक्टरांचा "
            "त्वरित सल्ला घ्या."
        )

    return (
        "मुझे इस प्रश्न के लिए अभी पूरी और सटीक जानकारी नहीं मिल सकी। "
        "कृपया अपने लक्षण, वे कब से हैं और कोई अन्य परेशानी हो तो बताएं। "
        "गंभीर या बढ़ते हुए लक्षण होने पर तुरंत डॉक्टर से संपर्क करें।"
    )


# ============================================================
# AI PROVIDERS: OLLAMA (PRIMARY) + GEMINI (OPTIONAL FALLBACK)
# ============================================================

def _env_float(name, default):
    try:
        return float(os.getenv(name, default))
    except (TypeError, ValueError):
        return float(default)


def _env_int(name, default):
    try:
        return int(os.getenv(name, default))
    except (TypeError, ValueError):
        return int(default)


def _env_bool(name, default):
    value = os.getenv(name)
    if value is None:
        return bool(default)
    return value.strip().lower() not in {"0", "false", "no", "off", ""}


# ---------------------------- Gemini (fallback) ----------------------------

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-3.5-flash-lite")
gemini_client = None

if GEMINI_API_KEY:
    try:
        gemini_client = genai.Client(api_key=GEMINI_API_KEY)
        print("Gemini AI : Connected (optional fallback)")
    except Exception as e:
        print("Gemini AI : Connection failed:", e)
else:
    print("Gemini AI : API key not configured (optional fallback disabled)")


# ----------------------------- Ollama (primary) ------------------------------

def _normalize_ollama_base_url(value):
    value = (value or "").strip() or "http://localhost:11434"
    if "://" not in value:
        value = "http://" + value
    return value.rstrip("/")


OLLAMA_ENABLED = _env_bool("OLLAMA_ENABLED", True)
OLLAMA_BASE_URL = _normalize_ollama_base_url(
    os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
)
OLLAMA_MODEL = (os.getenv("OLLAMA_MODEL", "gemma3:latest") or "").strip() or "gemma3:latest"
OLLAMA_CONNECT_TIMEOUT = _env_float("OLLAMA_CONNECT_TIMEOUT", 3.0)
# Read timeout tuned to 25s to fit well within Android OkHttpClient's read timeout (90s)
# while allowing fast failover to Gemini (and offline retrieval) if Ollama is unresponsive.
OLLAMA_READ_TIMEOUT = _env_float("OLLAMA_READ_TIMEOUT", 12.0)
OLLAMA_TEMPERATURE = _env_float("OLLAMA_TEMPERATURE", 0.3)
OLLAMA_NUM_CTX = _env_int("OLLAMA_NUM_CTX", 2048)
OLLAMA_NUM_PREDICT = _env_int("OLLAMA_NUM_PREDICT", 512)
OLLAMA_RETRY_COOLDOWN = _env_float("OLLAMA_RETRY_COOLDOWN", 20.0)
OLLAMA_KEEP_ALIVE = os.getenv("OLLAMA_KEEP_ALIVE", "10m")


class OllamaError(Exception):
    def __init__(self, message, status=None):
        super().__init__(message)
        self.status = status


_ollama_lock = threading.Lock()
_ollama_state = {"down_until": 0.0, "last_error": None}
_ollama_status_cache = {"checked_at": 0.0, "result": None}


def _ollama_request(method, path, payload=None, read_timeout=None):
    parsed = urlparse(OLLAMA_BASE_URL)
    host = parsed.hostname
    if not host:
        raise OllamaError(f"Invalid OLLAMA_BASE_URL: {OLLAMA_BASE_URL}")

    connection_class = (
        http.client.HTTPSConnection if parsed.scheme == "https"
        else http.client.HTTPConnection
    )
    base_path = parsed.path.rstrip("/")
    effective_read_timeout = OLLAMA_READ_TIMEOUT if read_timeout is None else read_timeout

    conn = connection_class(host, parsed.port, timeout=OLLAMA_CONNECT_TIMEOUT)
    try:
        conn.connect()
        if conn.sock is not None:
            conn.sock.settimeout(effective_read_timeout)

        body = json.dumps(payload).encode("utf-8") if payload is not None else None
        conn.request(
            method,
            base_path + path,
            body=body,
            headers={"Content-Type": "application/json", "Accept": "application/json"},
        )
        response = conn.getresponse()
        status = response.status
        raw = response.read()
    except TimeoutError as e:
        raise OllamaError(f"Ollama request timed out after {effective_read_timeout:.1f}s at {OLLAMA_BASE_URL}: {e}") from e
    except (OSError, http.client.HTTPException) as e:
        raise OllamaError(f"cannot reach Ollama at {OLLAMA_BASE_URL}: {e}") from e
    finally:
        try:
            conn.close()
        except Exception:
            pass

    try:
        data = json.loads(raw.decode("utf-8")) if raw else {}
    except (ValueError, UnicodeDecodeError):
        data = None

    if status != 200:
        detail = data.get("error") if isinstance(data, dict) else None
        raise OllamaError(f"HTTP {status}: {detail or 'unexpected response'}", status=status)
    if not isinstance(data, dict):
        raise OllamaError("Ollama returned a response that is not valid JSON")
    return data


def _ollama_in_cooldown():
    with _ollama_lock:
        return time.monotonic() < _ollama_state["down_until"]


def _ollama_mark_down(reason):
    with _ollama_lock:
        _ollama_state["down_until"] = time.monotonic() + OLLAMA_RETRY_COOLDOWN
        _ollama_state["last_error"] = reason


def _ollama_mark_up():
    with _ollama_lock:
        _ollama_state["down_until"] = 0.0
        _ollama_state["last_error"] = None


def ollama_chat(system_prompt, user_prompt, temperature=None, messages=None):
    if not OLLAMA_ENABLED or _ollama_in_cooldown():
        return None

    if messages is not None:
        chat_messages = messages
    else:
        chat_messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ]

    payload = {
        "model": OLLAMA_MODEL,
        "messages": chat_messages,
        "stream": False,
        "keep_alive": OLLAMA_KEEP_ALIVE,
        "options": {
            "temperature": OLLAMA_TEMPERATURE if temperature is None else temperature,
            "num_ctx": OLLAMA_NUM_CTX,
            "num_predict": OLLAMA_NUM_PREDICT,
        },
    }

    try:
        data = _ollama_request("POST", "/api/chat", payload)
    except OllamaError as e:
        _ollama_mark_down(str(e))
        logger.warning("Ollama error (skipping Ollama for %.0fs): %s", OLLAMA_RETRY_COOLDOWN, e)
        if e.status == 404:
            logger.info("Ollama hint: is the model installed? Run: ollama pull %s", OLLAMA_MODEL)
        return None

    message = data.get("message")
    content = message.get("content") if isinstance(message, dict) else None
    if not isinstance(content, str) or not content.strip():
        logger.info("Ollama returned an empty answer; falling back.")
        return None

    _ollama_mark_up()
    return content.strip()


def _ollama_model_listed(model, names):
    def canonical(value):
        value = str(value or "").strip().lower()
        return value if ":" in value else value + ":latest"

    wanted = canonical(model)
    return any(canonical(name) == wanted for name in names)


def get_ollama_status(max_age=10.0):
    now = time.monotonic()
    with _ollama_lock:
        cached = _ollama_status_cache["result"]
        if cached is not None and (now - _ollama_status_cache["checked_at"]) < max_age:
            return dict(cached)

    result = {
        "enabled": OLLAMA_ENABLED,
        "base_url": OLLAMA_BASE_URL,
        "model": OLLAMA_MODEL,
        "reachable": False,
        "model_available": False,
        "error": None,
    }

    if OLLAMA_ENABLED:
        try:
            data = _ollama_request("GET", "/api/tags", read_timeout=5.0)
            names = []
            for item in data.get("models", []) or []:
                if isinstance(item, dict):
                    names.append(item.get("name") or item.get("model") or "")
            result["reachable"] = True
            result["model_available"] = _ollama_model_listed(OLLAMA_MODEL, names)
        except OllamaError as e:
            result["error"] = str(e)

    with _ollama_lock:
        _ollama_status_cache["result"] = dict(result)
        _ollama_status_cache["checked_at"] = time.monotonic()
    return result


def get_ai_status_fields():
    try:
        ollama = get_ollama_status()
    except Exception:
        ollama = {"reachable": False, "model_available": False}

    primary_pref = os.getenv("AI_PRIMARY_PROVIDER", "auto").strip().lower()
    if primary_pref == "ollama" and OLLAMA_ENABLED:
        primary = "ollama"
    elif gemini_client is not None:
        primary = "gemini"
    elif OLLAMA_ENABLED:
        primary = "ollama"
    else:
        primary = "none"

    return {
        "ai_primary_provider": primary,
        "ollama_enabled": OLLAMA_ENABLED,
        "ollama_model": OLLAMA_MODEL,
        "ollama_reachable": bool(ollama.get("reachable")),
        "ollama_model_available": bool(ollama.get("model_available")),
        "gemini_configured": gemini_client is not None,
    }


# ------------------------- shared prompt construction ------------------------

def build_session_memory_summary(session_id):
    if not session_id:
        return ""
    try:
        memories = get_medical_memory(session_id, limit=20)
    except Exception:
        return ""
    if not memories:
        return ""

    symptom_durations = {}
    measurements = []
    clinical_notes = []

    for m in memories:
        m_type = str(m.get("memory_type") or "").strip().lower()
        name = str(m.get("name") or "").strip()
        duration = str(m.get("duration") or "").strip()
        value = str(m.get("value") or "").strip()
        unit = str(m.get("unit") or "").strip()

        if m_type == "symptom" and name:
            if duration and duration != "None":
                symptom_durations[name] = duration
            elif name not in symptom_durations:
                symptom_durations[name] = ""
        elif m_type in {"blood_pressure", "pulse", "temperature", "blood_sugar", "weight"}:
            val_str = f"{value} {unit}".strip()
            item_str = f"{name}: {val_str}"
            if val_str and item_str not in measurements:
                measurements.append(item_str)
        elif m_type in {"location", "severity"} and name:
            note = f"{m_type}: {name}"
            if note not in clinical_notes:
                clinical_notes.append(note)

    summary_parts = []
    if symptom_durations:
        s_list = [f"{s} (duration: {d})" if d else s for s, d in symptom_durations.items()]
        summary_parts.append("Confirmed patient symptoms: " + ", ".join(s_list))
    if measurements:
        summary_parts.append("Recorded measurements: " + ", ".join(measurements))
    if clinical_notes:
        summary_parts.append("Clinical details: " + ", ".join(clinical_notes))

    return "\n".join(summary_parts)


def _build_medical_system_instruction(language, memory_summary=""):
    language_names = {"en": "English", "mr": "Marathi", "hi": "Hindi"}
    target_language = language_names.get(language, "English")

    instruction = (
        f"You are MEDASSIST AI, an expert medical communication assistant.\n"
        f"TARGET RESPONSE LANGUAGE: {target_language}.\n\n"
        f"INPUT UNDERSTANDING:\n"
        f"- The user may communicate in English, Hindi, Marathi, Romanized Marathi, Romanized Hindi, "
        f"or mixed Hinglish / Maraglish. Voice inputs may feature phonetics or misspellings. Extract clinical intent accurately.\n\n"
        f"CONVERSATIONAL CONTEXT RULES:\n"
        f"1. Conversational, empathetic, and direct tone. Never begin with canned intros like 'Here is some general information'.\n"
        f"2. Seamlessly continue multi-turn conversation context. If the user answers a question (e.g. giving duration, severity, location), "
        f"acknowledge that detail and move forward.\n"
        f"3. DO NOT repeatedly ask for details the patient has already provided.\n"
        f"4. If essential clinical information is still missing, ask AT MOST ONE focused, relevant follow-up question.\n"
        f"5. Resolve pronouns and references (e.g. 'that pain', 'it is getting worse', 'what should I do now?') using recent conversation turns.\n"
        f"6. Explain common causes clearly, offer practical general self-care advice, and highlight red-flag warning signs requiring urgent medical attention.\n"
        f"7. Do not provide a definitive diagnosis or claim certainty. Do not prescribe specific drug dosages.\n"
        f"8. OUTPUT LANGUAGE REQUIREMENT: Write the ENTIRE response in {target_language}. "
        f"If the target is Marathi or Hindi, use native Devanagari script. Standard medicine names, units, and proper names may remain intact.\n"
        f"9. NEVER repeat self-introductions (such as 'Hello! I am MEDASSIST AI...' or 'Welcome to MEDASSIST AI'). "
        f"Do not start every message with greeting formulas. Address the user's question directly and concisely.\n"
    )
    if memory_summary:
        instruction += f"\nCONFIRMED PATIENT FACTS IN THIS CONVERSATION:\n{memory_summary}\n"
    return instruction


def _build_multi_turn_messages(
    session_id, system_instruction, current_user_text, language="en", medical_context="", limit=8
):
    history = get_chat_history(session_id, limit=limit) if session_id else []

    messages = [{"role": "system", "content": system_instruction}]

    for item in history:
        role = str(item.get("role", "")).strip().lower()
        msg = str(item.get("message", "")).strip()
        if not msg:
            continue
        if role == "user":
            messages.append({"role": "user", "content": msg})
        elif role == "assistant":
            messages.append({"role": "assistant", "content": msg})

    language_names = {"en": "English", "mr": "Marathi", "hi": "Hindi"}
    target_language = language_names.get(language, "English")

    current_prompt = current_user_text
    if medical_context:
        current_prompt = (
            f"{current_user_text}\n\n"
            f"[Clinical reference knowledge: {medical_context}]\n"
            f"[Instructions: Respond directly in {target_language}. Acknowledge newly provided patient details naturally. "
            f"If vital details are missing, ask at most one follow-up question. Do not repeat questions already answered.]"
        )
    messages.append({"role": "user", "content": current_prompt})
    return messages


def _format_messages_for_gemini(messages):
    lines = []
    for m in messages:
        role = m.get("role", "user")
        content = m.get("content", "")
        if role == "system":
            lines.append(f"SYSTEM INSTRUCTIONS:\n{content}")
        elif role == "assistant":
            lines.append(f"MEDASSIST AI:\n{content}")
        else:
            lines.append(f"USER:\n{content}")
    return "\n\n".join(lines)


def _build_medical_user_prompt(user_text, language, medical_context, conversation_context):
    language_names = {"en": "English", "mr": "Marathi", "hi": "Hindi"}
    target_language = language_names.get(language, "English")

    return f"""
User's current message:
{user_text}

Reliable medical context from the app's knowledge base:
{medical_context or "No specific knowledge-base context is available. Use general medical knowledge conservatively."}

Recent conversation:
{conversation_context or "No previous conversation context available."}

Write the final response for the user in {target_language}.
Make it conversational, direct, and helpful. If this is a follow-up, continue the conversation seamlessly.
Do not mention system prompts, knowledge bases, or internal classifications. Return only the response text.
"""


def _gemini_generate(contents):
    if gemini_client is None:
        return None

    candidate_models = [GEMINI_MODEL]
    for alt_model in ["gemini-3.5-flash-lite", "gemini-3.5-flash", "gemini-3.8-flash"]:
        if alt_model not in candidate_models:
            candidate_models.append(alt_model)

    for current_model in candidate_models:
        for attempt in range(2):
            try:
                response = gemini_client.models.generate_content(
                    model=current_model,
                    contents=contents,
                )
                answer = getattr(response, "text", None)
                if answer and answer.strip():
                    return answer.strip()
            except Exception as e:
                print(f"Gemini generation error with {current_model} (attempt {attempt + 1}/2):", e)
                if attempt == 0:
                    time.sleep(0.4)

    return None


# ------------------------------ public generators ----------------------------

def generate_medical_response_with_provider(
    user_text, language="en", medical_context="", conversation_context="", session_id=None
):
    ollama_available = OLLAMA_ENABLED
    gemini_available = gemini_client is not None
    if not ollama_available and not gemini_available:
        return None, None

    memory_summary = build_session_memory_summary(session_id) if session_id else ""
    system_instruction = _build_medical_system_instruction(language, memory_summary=memory_summary)
    prompt = _build_medical_user_prompt(
        user_text, language, medical_context, conversation_context
    )

    structured_messages = None
    if session_id:
        structured_messages = _build_multi_turn_messages(
            session_id=session_id,
            system_instruction=system_instruction,
            current_user_text=user_text,
            language=language,
            medical_context=medical_context,
            limit=8,
        )

    primary_pref = os.getenv("AI_PRIMARY_PROVIDER", "auto").strip().lower()

    providers = []

    def _add_gemini():
        if gemini_available:
            if structured_messages:
                gemini_content = _format_messages_for_gemini(structured_messages)
            else:
                gemini_content = system_instruction + "\n\n" + prompt
            providers.append(("gemini", lambda: _gemini_generate(gemini_content)))

    def _add_ollama():
        if ollama_available:
            if structured_messages:
                providers.append(("ollama", lambda: ollama_chat(system_instruction, prompt, messages=structured_messages)))
            else:
                providers.append(("ollama", lambda: ollama_chat(system_instruction, prompt)))

    if primary_pref == "ollama":
        _add_ollama()
        _add_gemini()
    else:  # "auto" or "gemini" -> fast cloud API first with instant fallback
        _add_gemini()
        _add_ollama()

    for provider_name, call in providers:
        answer = call()
        if not answer:
            continue
        answer = answer.strip()

        if _looks_mixed_with_english(answer, language):
            translated = translate_text_with_ai(answer, language)
            if translated and not _looks_mixed_with_english(translated, language):
                answer = translated.strip()

        if not _looks_mixed_with_english(answer, language):
            return answer, provider_name

    return None, None


def generate_ai_medical_response(
    user_text, language="en", medical_context="", conversation_context=""
):
    answer, _provider = generate_medical_response_with_provider(
        user_text=user_text,
        language=language,
        medical_context=medical_context,
        conversation_context=conversation_context,
    )
    return answer


def generate_gemini_medical_response(
    user_text, language="en", medical_context="", conversation_context=""
):
    return generate_ai_medical_response(
        user_text=user_text,
        language=language,
        medical_context=medical_context,
        conversation_context=conversation_context,
    )


# ============================================================
# CHAT DATABASE
# ============================================================

def _connect():
    conn = sqlite3.connect(CHAT_DB_FILE, timeout=10)
    try:
        conn.execute("PRAGMA journal_mode=WAL")
    except sqlite3.Error:
        pass
    return conn


def init_chat_database():
    CHAT_DB_FILE.parent.mkdir(parents=True, exist_ok=True)
    conn = _connect()
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
    conn = _connect()
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
    conn = _connect()
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


def get_last_response_language(session_id):
    try:
        history = get_chat_history(session_id, limit=20)
        for item in reversed(history):
            role = str(item.get("role", "")).lower()
            if role != "assistant":
                continue

            answer = str(
                item.get("message")
                or item.get("content")
                or item.get("text")
                or ""
            ).strip()

            if answer:
                devanagari = sum(1 for ch in answer if "\u0900" <= ch <= "\u097F")
                if devanagari:
                    detected = detect_language(answer)
                    return detected if detected in {"mr", "hi"} else "mr"
                return "en"
    except Exception:
        pass

    return None


def is_ambiguous_roman_message(text):
    if not text:
        return False

    lower = str(text).lower()
    words = set(re.findall(r"[a-zA-Z]+", lower))
    return bool(words.intersection(ROMAN_INDIC_MARKERS))


def build_conversation_context(session_id, limit=12):
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
    conn = _connect()
    conn.execute("DELETE FROM chat_history WHERE session_id = ?", (session_id,))
    conn.commit()
    conn.close()


# ============================================================
# STRUCTURED MEDICAL MEMORY
# ============================================================

def init_medical_memory_table():
    conn = _connect()
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
    conn = _connect()
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
    conn = _connect()
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
    conn = _connect()
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

_NORMALIZE_REPLACEMENTS = {
    # Romanized Marathi / Hindi / Hinglish / Maraglish
    "pn": "also", "bhi": "also", "pan": "also",
    "aahe": "have", "ahe": "have", "hai": "have", "hain": "have",
    "mala": "i", "mujhe": "i", "mujhko": "i", "mi": "i", "main": "i",
    "majha": "my", "maza": "my", "majhi": "my", "mera": "my", "meri": "my", "mere": "my",
    "kay": "what", "kya": "what", "kasa": "how", "kaise": "how",
    "bukhar": "fever", "tap": "fever", "taap": "fever", "fever": "fever",
    "sardi": "cold", "jukam": "cold", "zukaam": "cold",
    "khokla": "cough", "khansi": "cough", "dhas": "cough",
    "kamjori": "weakness", "kamzori": "weakness", "ashaktpana": "weakness", "thakan": "weakness",
    "chakkar": "dizziness", "garagar": "dizziness",
    "dukht": "pain", "dukh": "pain", "dukhtay": "pain", "dard": "pain", "dukhne": "pain",
    "goli": "medicine", "dawa": "medicine", "dawai": "medicine", "aushadh": "medicine",
    "pot": "stomach", "pet": "stomach", "dok": "head", "doka": "head", "sir": "head",
    "ghasa": "throat", "gala": "throat", "chhati": "chest", "seena": "chest",
    # Devanagari script (Marathi + Hindi)
    "आहे": "have", "आहेत": "have", "है": "have", "हैं": "have",
    "मला": "i", "माझा": "my", "माझी": "my", "माझे": "my", "मुझे": "i", "मेरा": "my", "मेरी": "my", "मेरे": "my",
    "काय": "what", "क्या": "what", "कसे": "how", "कैसे": "how",
    "ताप": "fever", "बुखार": "fever",
    "सर्दी": "cold", "जुकाम": "cold",
    "खोकला": "cough", "खांसी": "cough",
    "कमजोरी": "weakness", "अशक्तपणा": "weakness", "थकवा": "weakness",
    "चक्कर": "dizziness",
    "दुखत": "pain", "दुखणे": "pain", "दुखतं": "pain", "दर्द": "pain",
    "पोट": "stomach", "पेट": "stomach", "डोके": "head", "डोकं": "head", "सिर": "head",
    "घसा": "throat", "गला": "throat", "छाती": "chest",
    "औषध": "medicine", "दवा": "medicine", "गोळी": "medicine",
}

_WORD_CHAR_PATTERN = re.compile(r"[^a-zA-Z0-9\u0900-\u097F]")
_FINAL_CHAR_PATTERN = re.compile(r"[^a-z0-9\s\u0900-\u097F]")
_WHITESPACE_PATTERN = re.compile(r"\s+")

_SPELLING_MAP = {
    "fevr": "fever", "fvr": "fever", "feaver": "fever", "fevar": "fever",
    "hedache": "headache", "headche": "headache", "headpain": "headache",
    "caugh": "cough", "coff": "cough", "vomitng": "vomiting", "vomitting": "vomiting",
    "diarhea": "diarrhea", "diarrhoea": "diarrhea", "dizines": "dizziness",
    "dizzyness": "dizziness", "weaknes": "weakness", "stomac": "stomach",
    "stomache": "stomach", "throate": "throat",
}

_PHRASE_MAP = {
    "head pain": "headache", "head ache": "headache", "head is paining": "headache",
    "body pain": "bodypain", "body ache": "bodypain", "high temperature": "fever",
    "throwing up": "vomiting", "feeling sick": "nausea", "sore throat": "sorethroat",
    "stomach ache": "stomachpain", "stomach pain": "stomachpain", "chest pain": "chestpain",
    "back pain": "backpain", "joint pain": "jointpain", "difficulty breathing": "breathing",
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
    "fever": [
        "fever", "fevr", "fvr", "feaver", "temperature", "temp", "bukhar",
        "tap", "taap", "tapa", "tapu", "ताप", "बुखार",
    ],
    "headache": [
        "headache", "headpain", "head pain", "head ache", "head is paining", "hedache",
        "doka dukhat", "dok dukhat", "dokyala dukh", "doka dukh",
        "dokedukhi", "dokdukhi", "doka dukhtay", "dokyala dukhte", "maje doke dukhtay",
        "sirdard", "sir dard", "sar dard", "sir me dard", "sir mein dard",
        "डोकेदुखी", "डोके दुखत", "डोके दुखत आहे", "डोकं दुखतंय", "डोके दुखणे",
        "सिरदर्द", "सिर दर्द", "सिर में दर्द", "मेरे सिर में दर्द है",
    ],
    "cough": [
        "cough", "caugh", "coff", "khokla", "khansi", "khokla aahe",
        "खोकला", "खांसी",
    ],
    "cold": ["cold", "sardi", "jukam", "zukaam", "running nose", "runny nose"],
    "body_pain": ["body pain", "bodypain", "body ache", "bodyache", "ang dukhat", "anga dukhat"],
    "stomach_pain": [
        "stomach pain", "stomachpain", "stomach ache", "stomachache",
        "pot dukhat", "pota dukhat", "potdukhi", "pet dard", "pet mein dard", "pet me dard",
        "पोटदुखी", "पेट दर्द", "पोट दुखत आहे", "पेट में दर्द",
    ],
    "chest_pain": ["chest pain", "chestpain", "chest ache"],
    "back_pain": [
        "back pain", "backpain", "back ache", "backache", "lower back pain", "lower back", "upper back pain", "upper back",
        "back hurts", "back is hurting", "back hurt", "pain in back",
        "पाठदुखी", "पाठ दुखत", "पाठ दुखणे", "कंबरदुखी", "कंबर दुखत", "कंबर दुखणे", "पाठीत वेदना", "पाठीत दुखणे",
        "पाठ खूप दुखत", "पाठीत खूप दुखत", "पाठ फार दुखत", "पाठीत फार दुखत",
        "pathdukhi", "pathi dukhat", "pathi dukh", "path dukhat", "path dukh", "kambar dukhat", "kambar dukhte", "kambardukhi",
        "कमर दर्द", "कमर में दर्द", "पीठ दर्द", "पीठ में दर्द", "कमरदुखी",
        "kamar dard", "kamar mein dard", "kamar me dard", "peeth dard", "peeth mein dard", "peeth me dard", "kamar dukh",
    ],
    "joint_pain": ["joint pain", "jointpain"],
    "vomiting": ["vomiting", "vomit", "vomitting", "throwing up", "ulti", "ulti hot aahe"],
    "nausea": ["nausea", "feeling sick", "sick feeling", "malmal", "mala malmal"],
    "diarrhea": ["diarrhea", "diarhea", "loose motion", "loose motions"],
    "dizziness": [
        "dizziness", "dizines", "dizzyness", "chakkar", "garagar", "चक्कर",
    ],
    "weakness": ["weakness", "weaknes", "kamjori", "kamzori", "ashaktpana", "tired", "fatigue", "thakan"],
    "breathing_problem": [
        "breathing", "breathless", "difficulty breathing", "problem breathing",
        "shortness of breath",
    ],
    "sore_throat": ["sore throat", "sorethroat", "throat pain", "throat ache"],
    "rash": ["rash", "skin rash", "itchy rash"],
    "swelling": ["swelling", "swollen"],
}


_CLAUSE_BREAK_PATTERN = re.compile(
    r"\b(?:but|par|parantu|pan|pun|pn|lekin|kintu|mag|mg|and|aur|aani|ani|also)\b|[.,;!?|]",
    re.IGNORECASE,
)


def _is_match_negated(text, start, end):
    """
    Checks if a matched symptom keyword or alias in `text` (occurring between
    indices `start` and `end`) is preceded or followed by a negation qualifier
    in English, Roman Hindi/Marathi, or Devanagari within the same clause.
    """
    prefix = text[:start].strip()
    prefix_clauses = _CLAUSE_BREAK_PATTERN.split(prefix)
    immediate_pre = prefix_clauses[-1].strip() if prefix_clauses else ""
    preceding = " ".join(immediate_pre.split()[-3:]) if immediate_pre else ""

    neg_pre = [
        r"\b(?:no|not|neither|nor|without|never|don'?t\s+have|do\s+not\s+have|didn'?t\s+have|did\s+not\s+have|haven'?t|have\s+no|has\s+no)\b",
        r"\b(?:nahi|nahin|nako|nahiye|naslyas)\b",
        r"(?:\u0928\u093e\u0939\u0940|\u0928\u0939\u0940\u0902|\u0928\u093e|\u0928\u0915\u094b)\b",
    ]
    for pat in neg_pre:
        if re.search(pat, preceding, re.IGNORECASE):
            return True

    suffix = text[end:].strip()
    suffix_clauses = _CLAUSE_BREAK_PATTERN.split(suffix)
    immediate_post = suffix_clauses[0].strip() if suffix_clauses else ""
    succeeding = " ".join(immediate_post.split()[:3]) if immediate_post else ""

    neg_post = [
        r"\b(?:nahi|nahin|nahiye|nako|hot\s+nahi|aahe\s+ka\s+nahi)\b",
        r"(?:\u0928\u093e\u0939\u0940|\u0928\u0939\u0940\u0902|\u0928\u093e|\u0928\u0915\u094b)\b",
    ]
    for pat in neg_post:
        if re.search(pat, succeeding, re.IGNORECASE):
            return True

    return False


def extract_symptoms(text):
    normalized = normalize_text(text)

    roman_marathi_normalized = {
        "tap aala": "fever", "tap ala": "fever",
        "taap aala": "fever", "taap ala": "fever",
        "mala tap": "fever", "mala taap": "fever",
        "doka dukhat": "headache", "dok dukhat": "headache",
        "pot dukhat": "stomach pain", "pathi dukhat": "back pain",
        "path dukhat": "back pain", "kambar dukhat": "back pain",
        "ang dukhat": "body pain", "ulti": "vomiting",
        "mala malmal": "nausea", "chakkar": "dizziness",
        "garagar": "dizziness", "khokla": "cough", "sardi": "cold",
        "kamjori": "weakness", "ashaktpana": "weakness",
    }

    for phrase, canonical in roman_marathi_normalized.items():
        m = re.search(r"(?<!\w)" + re.escape(phrase) + r"(?!\w)", normalized)
        if m and not _is_match_negated(normalized, m.start(), m.end()):
            normalized += " " + canonical

    detected = []

    for symptom, aliases in SYMPTOM_ALIASES.items():
        for alias in aliases:
            alias_normalized = normalize_text(alias)

            # Ignore aliases corrupted by encoding or normalization.
            if not alias_normalized:
                continue

            pattern = r"(?<!\w)" + re.escape(alias_normalized) + r"(?!\w)"
            has_affirmative_match = False
            for m in re.finditer(pattern, normalized):
                if not _is_match_negated(normalized, m.start(), m.end()):
                    has_affirmative_match = True
                    break

            if has_affirmative_match:
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
    "tap", "taap", "khokla", "sardi", "ghasa", "doka", "dok", "pot",
    "dukhat", "dukh", "ulti", "malmal", "chakkar", "garagar", "kamjori",
    "ashaktpana", "pathi", "path", "kamar", "peeth", "कमर", "पीठ", "पाठ",
    "azaala", "aazar", "ang", "tapa", "dawa", "dawai", "goli", "bukhar", "back",
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
    # Clear conversational continuations
    "yes", "no", "also", "still", "worse", "better",
    # Explicit action-asking phrases
    "what should i do", "what can i do", "what to do",
    "what care", "care should i take", "what precautions",
    "how should i care", "how to take care", "home care",
    # Marathi/Hindi conversational continuations (normalize_text maps 'kay'/'kya' -> 'what')
    "aata", "ata", "kya karu", "kay karu", "what karu",
]

_FOLLOWUP_RE = re.compile(
    r"(?<![a-z0-9])(?:"
    + "|".join(re.escape(p) for p in sorted(FOLLOWUP_PATTERNS, key=len, reverse=True))
    + r")(?![a-z0-9])"
)


def is_followup_query(text):
    query = normalize_text(text)
    return bool(_FOLLOWUP_RE.search(query))


_GREETING_PATTERNS = {
    "hi", "hello", "hey", "namaste", "namaskar", "pranam",
    "good morning", "good evening", "good afternoon", "good night",
    "how are you", "kasa ahes", "kashi ahes", "kya haal hai",
}


def is_greeting(text):
    if not text:
        return False
    norm = normalize_text(text).strip()
    return norm in _GREETING_PATTERNS or norm.replace(" ", "") in {"hi", "hello", "hey", "namaste", "namaskar"}


def is_contextual_followup(text, session_id=None, previous_symptoms=None):
    """
    Determines if text is a conversational continuation or follow-up to an
    ongoing symptom discussion, even if it does not repeat a symptom keyword.
    """
    if not text:
        return False

    norm = normalize_text(text)
    lower = str(text).lower().strip()

    # 1. Pattern-based continuation
    if is_followup_query(text):
        return True

    # 2. Reference to previous symptoms/pain/condition
    reference_patterns = [
        "that pain", "this pain", "the pain", "that ache", "this ache",
        "that discomfort", "it is getting", "it gets", "it started",
        "it hurts", "it feels", "pain is", "fever is", "cough is",
        "getting worse", "getting better",
        "to dard", "ye dard", "woh dard", "te dukhne", "he dukhne",
        "mg azaala", "mag azaala", "azaala", "kay kru", "kya karu", "kya karein",
    ]
    if any(p in lower or p in norm for p in reference_patterns):
        return True

    # 3. If there is active previous symptom context in the session:
    if previous_symptoms:
        # User is answering with a duration or time reference
        time_reference_phrases = (
            "since yesterday", "since morning", "two days", "three days",
            "few days", "couple of days", "hours ago", "days ago",
        )
        if extract_duration(text) or any(p in lower or p in norm for p in time_reference_phrases):
            return True

        # Clinical descriptors of location, severity, or timing
        clinical_descriptors = {
            "lower", "upper", "back", "left", "right", "side", "front",
            "chest", "stomach", "head", "neck", "shoulder", "arm", "leg",
            "mild", "moderate", "severe", "sharp", "dull", "throbbing",
            "burning", "heavy", "tight", "constant", "intermittent",
            "sudden", "gradual", "morning", "night", "eating", "moving",
            "resting", "sitting", "walking", "khali", "var", "dava", "ujva",
            "kam", "jast", "thoda", "khoob", "tez", "halka", "jyada",
        }
        words = set(re.findall(r"[a-zA-Z]+", lower))
        if words.intersection(clinical_descriptors):
            return True

        # Short response (<= 8 words) during an active symptom discussion that isn't a greeting
        if len(words) <= 8 and not is_greeting(text):
            return True

    return False


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


def build_symptom_fallback_response(symptoms):
    names = [s.replace("_", " ") for s in symptoms]
    if len(names) == 1:
        topic = names[0]
    elif len(names) == 2:
        topic = f"{names[0]} and {names[1]}"
    else:
        topic = ", ".join(names[:-1]) + f", and {names[-1]}"

    return (
        f"I can help you with {topic}. For now, get some rest, stay hydrated, and avoid "
        "activities that clearly make the discomfort worse. The likely cause depends on "
        "how severe it is, how long it has been present, and what other symptoms you have.\n\n"
        "If it is severe or suddenly worsening, follows a significant injury, or comes with "
        "difficulty breathing, fainting, confusion, severe chest pain, or another emergency "
        "warning sign, seek urgent medical care.\n\n"
        "Tell me when it started, how severe it is, and any other symptoms you have. I can "
        "then continue with more relevant general guidance."
    )


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
        intro = f"Clinical overview for {symptom_names[0]}:"
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
    (r"(\d+)\s*(?:day|days|din|divas|divasapasun|dina|roj|दिन|दिवस)", "day", "days"),
    (r"(\d+)\s*(?:hour|hours|ghante|tas|taas|घंटे|तास)", "hour", "hours"),
    (r"(\d+)\s*(?:week|weeks|hafte|athavda|athavde|हफ्ते|आठवडा|आठवडे)", "week", "weeks"),
    (r"(\d+)\s*(?:month|months|mahine|mahina|महीने|महिना)", "month", "months"),
    (r"(?:since|from)\s*(\d+)\s*(?:day|days|din|divas|divasapasun)", "day", "days"),
]


_WORD_NUMBERS = {
    "one": "1", "two": "2", "three": "3", "four": "4", "five": "5",
    "six": "6", "seven": "7", "eight": "8", "nine": "9", "ten": "10",
    "couple of": "2", "a couple of": "2", "few": "3", "a few": "3",
    "ek": "1", "don": "2", "do": "2", "teen": "3", "tin": "3", "char": "4",
    "chaar": "4", "panch": "5", "paanch": "5",
    "एक": "1", "दो": "2", "दोन": "2", "तीन": "3", "चार": "4", "पाच": "5",
}

_DEVANAGARI_DIGITS = {
    "०": "0", "१": "1", "२": "2", "३": "3", "४": "4",
    "५": "5", "६": "6", "७": "7", "८": "8", "९": "9",
}


def extract_duration(text):
    if not text:
        return None

    query = str(text).lower()
    for d, a in _DEVANAGARI_DIGITS.items():
        query = query.replace(d, a)

    for word, num in _WORD_NUMBERS.items():
        query = re.sub(r"(?<!\w)" + re.escape(word) + r"(?!\w)", num, query)

    # Relative day and morning expressions in English, Hindi, and Marathi
    if re.search(
        r"\b(?:yesterday|last\s+night|kal\s+se|kalpasun|kal\s+pasun)\b|कल\s*से|कालपासून|काल\s*पासून",
        query,
    ):
        return "1 day"
    if re.search(
        r"\b(?:since|from)?\s*(?:this\s+)?morning\b|आज\s*सकाळपासून|सुबह\s*से|aaj\s*sakali|sakali\s*pasun|subah\s*se",
        query,
    ):
        return "since morning"

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


def save_structured_memories(session_id, text, symptoms, duration, previous_symptoms=None):
    active_symptoms = list(symptoms) if symptoms else (list(previous_symptoms) if previous_symptoms else [])

    if active_symptoms:
        for symptom in active_symptoms:
            save_medical_memory(
                session_id=session_id, memory_type="symptom", name=symptom,
                duration=duration, source_message=text,
            )
    elif duration:
        save_medical_memory(
            session_id=session_id, memory_type="duration", name="duration",
            duration=duration, source_message=text,
        )

    # Capture location descriptors if present
    norm = normalize_text(text).lower()
    for loc in ["lower back", "upper back", "left side", "right side", "chest", "stomach", "head", "neck", "shoulder"]:
        if loc in norm:
            save_medical_memory(
                session_id=session_id, memory_type="location", name=loc,
                source_message=text,
            )
            break

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
            if alias_normalized:
                pattern = r"(?<!\w)" + re.escape(alias_normalized) + r"(?!\w)"
                if re.search(pattern, query):
                    return symptom

    for name, aliases in MEASUREMENT_ALIASES.items():
        for alias in aliases:
            alias_normalized = normalize_text(alias)
            if alias_normalized:
                pattern = r"(?<!\w)" + re.escape(alias_normalized) + r"(?!\w)"
                if re.search(pattern, query):
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

    if previous_symptoms:
        names = [s.replace("_", " ") for s in previous_symptoms]
        topic = ", ".join(names)
        return (
            f"Thank you for the update regarding your {topic}. "
            "Please rest, stay hydrated, and monitor your condition closely. "
            "If your symptoms worsen or do not improve, please consult a healthcare professional."
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


_MEDIQ_BAD_MASK = np.array(
    [is_bad_answer(a) for a in dataset[ANSWER_COLUMN]], dtype=bool
)
_MEDIQ_CATEGORY_NORM = (
    np.array([normalize_text(c) for c in dataset[CATEGORY_COLUMN]])
    if CATEGORY_COLUMN is not None
    else None
)


def retrieve_mediq_answer(user_text, final_intent):
    query = normalize_text(user_text)
    query_vector = vectorizer.transform([query])
    similarities = cosine_similarity(query_vector, retrieval_matrix)[0]

    raw_scores = np.array(similarities, dtype=float)
    raw_scores = np.where(_MEDIQ_BAD_MASK, -1.0, raw_scores)

    # Require actual text similarity before using category as a tie-breaker;
    # otherwise a generic query like "symptoms" can retrieve unrelated records.
    raw_best_index = int(raw_scores.argmax())
    raw_best_score = float(raw_scores[raw_best_index])
    if raw_best_score < 0.30:
        return None

    scores = raw_scores.copy()
    if _MEDIQ_CATEGORY_NORM is not None:
        scores = scores + 0.05 * (_MEDIQ_CATEGORY_NORM == normalize_text(final_intent))
    scores = np.where(_MEDIQ_BAD_MASK, -1.0, scores)

    best_index = int(scores.argmax())
    best_score = float(raw_scores[best_index])
    if best_score < 0.30:
        return None

    answer = str(dataset.iloc[best_index][ANSWER_COLUMN])

    return {
        "answer": answer,
        "similarity": round(best_score, 4),
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
        **get_ai_status_fields(),
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
        **get_ai_status_fields(),
    })


@app.get("/history/<session_id>")
def history(session_id):
    sid = str(session_id or "").strip()
    if not sid:
        return jsonify({"status": "error", "message": "session_id is required"}), 400
    return jsonify({
        "status": "success",
        "session_id": sid,
        "history": get_chat_history(sid, limit=50),
    })


@app.delete("/history/<session_id>")
def delete_history(session_id):
    sid = str(session_id or "").strip()
    if not sid:
        return jsonify({"status": "error", "message": "session_id is required"}), 400
    clear_chat_history(sid)
    return jsonify({
        "status": "success",
        "message": "Chat history cleared",
        "session_id": sid,
    })


# ------------------------------------------------------------------
# Android-compatible alias routes  (query-param style)
# The Android app calls GET /chat-history?session_id=...
# and DELETE /chat-history?session_id=...
# The original /history/<session_id> routes are kept for
# backward compatibility with any other callers.
# ------------------------------------------------------------------

@app.get("/chat-history")
def chat_history_get():
    session_id = request.args.get("session_id", "").strip()
    if not session_id:
        return jsonify({"status": "error", "message": "session_id is required"}), 400
    return jsonify({
        "status": "success",
        "session_id": session_id,
        "history": get_chat_history(session_id, limit=50),
    })


@app.delete("/chat-history")
def chat_history_delete():
    session_id = request.args.get("session_id", "").strip()
    if not session_id:
        return jsonify({"status": "error", "message": "session_id is required"}), 400
    clear_chat_history(session_id)
    return jsonify({
        "status": "success",
        "message": "Chat history cleared",
        "session_id": session_id,
    })


@app.get("/memory/<session_id>")
def memory(session_id):
    sid = str(session_id or "").strip()
    if not sid:
        return jsonify({"status": "error", "message": "session_id is required"}), 400
    return jsonify({
        "status": "success",
        "session_id": sid,
        "memory": get_medical_memory(sid, limit=100),
    })


@app.delete("/memory/<session_id>")
def delete_memory(session_id):
    sid = str(session_id or "").strip()
    if not sid:
        return jsonify({"status": "error", "message": "session_id is required"}), 400
    clear_medical_memory(sid)
    return jsonify({
        "status": "success",
        "message": "Structured medical memory cleared",
        "session_id": sid,
    })


# ============================================================
# SAFETY RULES (emergency + crisis red flags)
# ============================================================

_PERSONAL_TOKENS = {
    "i", "my", "me", "im", "am", "he", "she", "him", "her", "his",
    "mother", "father", "mom", "dad", "wife", "husband", "son",
    "daughter", "baby", "child", "friend", "brother", "sister",
    "patient", "someone",
}
_INFO_ONLY_PHRASES = (
    "symptoms of", "signs of", "what are the symptoms", "what are the signs",
    "causes of", "symptom of",
)


def _compile_all(patterns):
    return [re.compile(p, re.IGNORECASE) for p in patterns]


_CRISIS_PATTERNS = _compile_all([
    r"\b(suicide|suicidal|kill\s+myself|end\s+my\s+life|end\s+it\s+all|"
    r"want\s+to\s+die|wanna\s+die|hurt\s+myself|harm\s+myself|self[-\s]?harm|"
    r"don'?t\s+want\s+to\s+live|no\s+reason\s+to\s+live)\b",
    r"आत्महत्या|खुदकुशी|मरावेसे\s*वाटत|मरायचे\s*आहे|मरायचं\s*आहे|"
    r"जगावेसे\s*वाटत\s*नाही|मरना\s*चाहत|जीना\s*नहीं\s*चाहत",
    r"atmahatya|aatmhatya|khudkushi|khudkhushi|marave\s*vat|"
    r"mala\s*marayche|marna\s*chahta|marna\s*chahti|"
    r"jeena\s*nahi\s*chahta|jeena\s*nahi\s*chahti",
])

_EMERGENCY_PATTERNS = _compile_all([
    r"\b(can'?t|cannot|can\s+not|unable\s+to|not\s+able\s+to|couldn'?t)\s+breathe?\b",
    r"\b(gasping|choking)\b",
    r"\b(throat|tongue)\s+(is\s+)?(swelling|swollen|closing)\b",
    r"\b(unconscious|unresponsive|passed\s+out|collapsed|not\s+responding|not\s+waking\s+up)\b",
    r"\b(seizure|seizures|convulsion|convulsions)\b",
    r"\b(face|facial)\s+(is\s+)?droop|\bslurred\s+speech\b",
    r"\b(heavy|severe|uncontrolled|profuse)\s+bleeding\b",
    r"\bbleeding\s+(heavily|a\s+lot|won'?t\s+stop|not\s+stopping)\b",
    r"\b(vomit|vomiting|vomited|cough|coughing|coughed)\s+(up\s+)?blood\b",
    r"\bblood\s+in\s+(my\s+)?vomit\b",
    r"\b(overdose|overdosed|poisoned|poisoning|snake\s*bite|bitten\s+by\s+a\s+snake)\b",
    r"\bswallowed\s+(poison|pesticide|bleach|acid)\b",
    r"severe\s+chest\s*pain|chest\s*pain\s+(is\s+)?(severe|unbearable|crushing)",
    r"श्वास\s*घेता\s*येत\s*नाही|सांस\s*(नहीं|नही)\s*(ले|आ)",
    r"बेशुद्ध|बेहोश",
    r"आकडी|फिट\s*(आली|येत)|मिर्गी\s*का\s*दौरा",
    r"रक्ताची\s*उलटी|खून\s*की\s*उल्टी|खूप\s*रक्त|बहुत\s*खून",
    r"सर्पदंश|साप\s*चावला|सांप\s*(ने\s*)?काट|जहर\s*खा",
    r"shwas\s*ghet\s*(yet\s*)?nahi|saans\s*nahi\s*(le|aa)|sans\s*nahi\s*(le|aa)",
    r"beshuddh|behosh|besudh",
    r"aakdi|akdi|fit\s*(aali|aala)",
    r"khup\s*rakt|rakta\s*chi\s*ulti|khoon\s*ki\s*ulti|bahut\s*khoon",
    r"sap\s*chavla|saap\s*chavla|saanp\s*(ne\s*)?kata|zahar\s*kha",
])

_CHEST_RE = re.compile(
    r"chest\s*(pain|tightness|pressure|heaviness)|chestpain|छातीत|सीने|chhati|seene",
    re.IGNORECASE,
)
_CHEST_COMPANION_RE = re.compile(
    r"breath|sweat|jaw|left\s+arm|crushing|radiat|श्वास|सांस|घाम|पसीना|"
    r"shwas|saans|ghaam|pasina|dhap|धाप",
    re.IGNORECASE,
)


def detect_safety_flag(raw_text, analysis_text, normalized_query):
    blob = " ".join([
        str(raw_text or "").lower(),
        str(analysis_text or "").lower(),
        str(normalized_query or ""),
    ])
    tokens = set(re.findall(r"[a-z]+", str(normalized_query or "")))
    informational = (
        any(p in blob for p in _INFO_ONLY_PHRASES)
        and not tokens.intersection(_PERSONAL_TOKENS)
    )
    if informational:
        return None

    if any(p.search(blob) for p in _CRISIS_PATTERNS):
        return "crisis"
    if any(p.search(blob) for p in _EMERGENCY_PATTERNS):
        return "emergency"
    if _CHEST_RE.search(blob) and _CHEST_COMPANION_RE.search(blob):
        return "emergency"
    return None


SAFETY_MESSAGES = {
    "emergency": {
        "en": (
            "This may be a medical emergency. Please call 112 (or your local "
            "ambulance service, such as 108 in many Indian states) or go to the "
            "nearest hospital emergency department right now. If someone is "
            "with you, ask them to stay with you and help. Do not wait for the "
            "symptoms to pass or try to treat this at home. This assistant "
            "cannot examine you or give emergency care."
        ),
        "mr": (
            "ही वैद्यकीय आणीबाणी असू शकते. कृपया त्वरित 112 (किंवा 108 रुग्णवाहिका) "
            "वर कॉल करा किंवा जवळच्या रुग्णालयातील आपत्कालीन विभागात जा. शक्य "
            "असल्यास जवळच्या व्यक्तीला तुमच्यासोबत राहायला सांगा. लक्षणे आपोआप "
            "कमी होण्याची वाट पाहू नका आणि घरी उपचार करण्याचा प्रयत्न करू नका. "
            "हा सहाय्यक तुमची तपासणी करू शकत नाही किंवा आपत्कालीन उपचार देऊ शकत नाही."
        ),
        "hi": (
            "यह चिकित्सा आपातकाल हो सकता है। कृपया तुरंत 112 (या 108 एम्बुलेंस) पर "
            "कॉल करें या नज़दीकी अस्पताल के आपातकालीन विभाग में जाएं। यदि संभव हो तो "
            "किसी पास के व्यक्ति से अपने साथ रहने को कहें। लक्षण अपने आप ठीक होने का "
            "इंतज़ार न करें और घर पर इलाज करने की कोशिश न करें। यह सहायक आपकी जांच नहीं "
            "कर सकता और आपातकालीन उपचार नहीं दे सकता।"
        ),
    },
    "crisis": {
        "en": (
            "I'm really sorry you're going through this, and I'm glad you said "
            "it out loud. You don't have to face it alone. If you might act on "
            "these thoughts or are in immediate danger, please call 112 now or "
            "go to the nearest emergency department, and ask someone you trust "
            "to stay with you. In India you can also call Tele-MANAS at 14416 "
            "(free, 24/7) to talk with a trained counsellor. I'm here with you."
        ),
        "mr": (
            "तुम्ही हे बोलून दाखवलं याबद्दल मला आनंद आहे, आणि तुम्ही ज्या त्रासातून "
            "जात आहात त्याबद्दल मला खूप वाईट वाटतं. हे तुम्हाला एकट्याने सहन करावं "
            "लागणार नाही. जर तुम्ही स्वतःला इजा करण्याची शक्यता असेल किंवा तात्काळ "
            "धोक्यात असाल, तर कृपया आत्ताच 112 वर कॉल करा किंवा जवळच्या रुग्णालयातील "
            "आपत्कालीन विभागात जा आणि विश्वासू व्यक्तीला तुमच्यासोबत राहायला सांगा. "
            "भारतात तुम्ही प्रशिक्षित समुपदेशकाशी बोलण्यासाठी Tele-MANAS ला 14416 वर "
            "(मोफत, २४x७) कॉल करू शकता. मी तुमच्यासोबत आहे."
        ),
        "hi": (
            "आपने यह बात कही, मुझे इसकी खुशी है, और आप जिस तकलीफ से गुज़र रहे हैं उसके "
            "लिए मुझे बहुत दुख है। आपको इसे अकेले नहीं झेलना है। अगर आप खुद को नुकसान "
            "पहुँचा सकते हैं या तुरंत खतरे में हैं, तो कृपया अभी 112 पर कॉल करें या "
            "नज़दीकी अस्पताल के आपातकालीन विभाग में जाएं, और किसी भरोसेमंद व्यक्ति से "
            "अपने साथ रहने को कहें। भारत में आप प्रशिक्षित काउंसलर से बात करने के लिए "
            "Tele-MANAS को 14416 पर (निःशुल्क, 24x7) कॉल कर सकते हैं। मैं आपके साथ हूँ।"
        ),
    },
}


def build_safety_response(kind, language):
    messages = SAFETY_MESSAGES.get(kind, SAFETY_MESSAGES["emergency"])
    return messages.get(language) or messages["en"]


MEMORY_QUESTION_WORDS = [
    "remember", "previously", "earlier", "before", "told", "said",
    "shared", "reported", "gave", "mentioned",
]

MEMORY_RECALL_PATTERNS = [
    "what symptoms did i mention earlier", "what symptoms did i mention before",
    "what symptoms have i mentioned", "which symptoms did i mention earlier",
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


MAX_TEXT_LENGTH = 2000
MAX_SESSION_ID_LENGTH = 128


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
    if len(text) > MAX_TEXT_LENGTH:
        return jsonify({
            "status": "error",
            "message": f"text is too long (maximum {MAX_TEXT_LENGTH} characters)",
        }), 400

    _raw_sid = str(data.get("session_id") or "").strip()[:MAX_SESSION_ID_LENGTH]
    session_id = _raw_sid if _raw_sid else ("anon_" + uuid.uuid4().hex)
    previous_response_language = get_last_response_language(session_id)

    requested_output_language = detect_requested_output_language(text)
    detected_language = detect_language(
        text,
        fallback_lang=requested_output_language or previous_response_language,
    )

    if requested_output_language:
        final_language = requested_output_language
    elif detected_language in {"mr", "hi"}:
        final_language = detected_language
    elif is_ambiguous_roman_message(text) and previous_response_language in {"mr", "hi"}:
        final_language = previous_response_language
    else:
        final_language = detected_language

    # Support transliterated / mixed Hinglish and Maraglish
    if detected_language in {"mr", "hi"} or is_ambiguous_roman_message(text):
        analysis_text = translate_input_to_english(text)
    else:
        analysis_text = text

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
        # SAFETY RULES (highest priority)
        # ----------------------------------------------------
        safety_flag = detect_safety_flag(text, analysis_text, normalized_query)
        if safety_flag:
            safety_answer = build_safety_response(safety_flag, final_language)
            safety_symptoms = symptoms if safety_flag == "emergency" else []

            save_chat_message(session_id, "user", text, safety_symptoms)
            if safety_flag == "emergency":
                save_structured_memories(
                    session_id=session_id, text=text, symptoms=symptoms,
                    duration=duration,
                )
            save_chat_message(session_id, "assistant", safety_answer, safety_symptoms)

            return jsonify({
                "status": "success",
                "session_id": session_id,
                "question": text,
                "intent": "emergency" if safety_flag == "emergency" else "crisis_support",
                "confidence": round(confidence, 4),
                "symptoms": safety_symptoms,
                "previous_symptoms": previous_symptoms,
                "duration": duration,
                "measurements": measurements,
                "medical_query": True,
                "answer": safety_answer,
                "answer_similarity": 1.0,
                "source": "safety_rules",
                "language": final_language,
                "emergency": True,
                "safety_flag": safety_flag,
            })

        # ----------------------------------------------------
        # SPECIFIC MEMORY RECALL
        # ----------------------------------------------------
        has_memory_language = any(
            word in normalized_query.split() for word in MEMORY_QUESTION_WORDS
        )

        specific_memory_answer = None
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
        is_followup = is_contextual_followup(text, session_id=session_id, previous_symptoms=previous_symptoms)
        if is_followup and previous_symptoms and (not symptoms or set(symptoms).issubset(set(previous_symptoms))):
            followup_context = ""
            if any(phrase in normalized_query for phrase in CARE_PHRASES):
                followup_context = build_multi_symptom_response(
                    previous_symptoms, "assistance"
                ) or ""

            ai_answer, ai_provider = generate_medical_response_with_provider(
                user_text=text,
                language=final_language,
                medical_context=followup_context,
                conversation_context=conversation_context,
                session_id=session_id,
            )

            if ai_answer:
                followup_answer = ai_answer
                source = ai_provider
            else:
                followup_raw = build_followup_response(session_id, text)
                followup_answer = localize_answer(followup_raw, final_language) if followup_raw else None
                source = "conversation_memory"

            if followup_answer:
                save_structured_memories(
                    session_id=session_id,
                    text=text,
                    symptoms=symptoms,
                    duration=duration,
                    previous_symptoms=previous_symptoms,
                )
                save_chat_message(session_id, "user", text, previous_symptoms)
                save_chat_message(session_id, "assistant", followup_answer, previous_symptoms)

                return jsonify({
                    "status": "success",
                    "session_id": session_id,
                    "question": text,
                    "intent": "follow_up",
                    "confidence": round(confidence, 4),
                    "symptoms": previous_symptoms,
                    "previous_symptoms": previous_symptoms,
                    "duration": duration,
                    "measurements": measurements,
                    "medical_query": True,
                    "answer": followup_answer,
                    "answer_similarity": 1.0,
                    "source": source,
                    "language": final_language,
                })

        # ----------------------------------------------------
        # GENERAL (non-medical) QUERY
        # ----------------------------------------------------
        if not medical_query and not is_followup:
            ai_answer, ai_provider = generate_medical_response_with_provider(
                user_text=text,
                language=final_language,
                medical_context="",
                conversation_context=conversation_context,
                session_id=session_id,
            )

            if ai_answer:
                save_chat_message(session_id, "user", text, symptoms)
                save_chat_message(session_id, "assistant", ai_answer, symptoms)

                return jsonify({
                    "status": "success",
                    "session_id": session_id,
                    "question": text,
                    "intent": "general",
                    "confidence": round(confidence, 4),
                    "symptoms": symptoms,
                    "medical_query": False,
                    "answer": ai_answer,
                    "answer_similarity": 1.0,
                    "source": ai_provider,
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
        # MEASUREMENTS
        # ----------------------------------------------------
        if measurements and not symptoms:
            measurement_context = build_measurement_response(measurements)
            ai_measurement_answer, measurement_provider = generate_medical_response_with_provider(
                user_text=text,
                language=final_language,
                medical_context=measurement_context,
                conversation_context=conversation_context,
                session_id=session_id,
            )
            final_answer = ai_measurement_answer or localize_answer(
                measurement_context, final_language
            )

            save_structured_memories(
                session_id=session_id,
                text=text,
                symptoms=symptoms,
                duration=duration,
                previous_symptoms=previous_symptoms,
            )
            save_chat_message(session_id, "user", text, symptoms)
            save_chat_message(session_id, "assistant", final_answer, symptoms)

            return jsonify({
                "status": "success",
                "session_id": session_id,
                "question": text,
                "intent": "measurement",
                "confidence": round(confidence, 4),
                "symptoms": symptoms,
                "previous_symptoms": previous_symptoms,
                "duration": duration,
                "measurements": measurements,
                "medical_query": True,
                "answer": final_answer,
                "answer_similarity": 1.0,
                "source": measurement_provider if ai_measurement_answer else "measurement",
                "language": final_language,
            })

        # ----------------------------------------------------
        # MEDICAL RESPONSE (symptom-based)
        # ----------------------------------------------------
        if symptoms:
            # Accumulate with previous symptoms in ongoing conversation for holistic clinical guidance
            combined_symptoms = list(dict.fromkeys(symptoms + (previous_symptoms or [])))
            question_type = get_question_type(text)
            medical_answer = build_multi_symptom_response(combined_symptoms, question_type)

            ai_medical_answer, symptom_provider = generate_medical_response_with_provider(
                user_text=text,
                language=final_language,
                medical_context=medical_answer or "No specific knowledge-base entry was found. Give conservative general medical guidance.",
                conversation_context=conversation_context,
                session_id=session_id,
            )

            if ai_medical_answer:
                final_answer = localize_answer(ai_medical_answer, final_language)
                source = symptom_provider
            elif medical_answer:
                final_answer = localize_answer(medical_answer, final_language)
                source = "medical_knowledge"
            else:
                final_answer = localize_answer(
                    build_symptom_fallback_response(combined_symptoms), final_language
                )
                source = "symptom_fallback"

            save_chat_message(session_id, "user", text, combined_symptoms)
            save_structured_memories(
                session_id=session_id,
                text=text,
                symptoms=symptoms,
                duration=duration,
                previous_symptoms=previous_symptoms,
            )
            save_chat_message(session_id, "assistant", final_answer, combined_symptoms)

            return jsonify({
                "status": "success",
                "session_id": session_id,
                "question": text,
                "intent": final_intent,
                "confidence": round(confidence, 4),
                "symptoms": combined_symptoms,
                "new_symptoms": symptoms,
                "previous_symptoms": previous_symptoms,
                "duration": duration,
                "measurements": measurements,
                "medical_query": True,
                "answer": final_answer,
                "answer_similarity": 1.0,
                "source": source,
                "language": final_language,
            })

        # ----------------------------------------------------
        # MEDIQ FALLBACK
        # ----------------------------------------------------
        mediq_result = retrieve_mediq_answer(analysis_text, model_prediction)

        if mediq_result is not None:
            answer = mediq_result["answer"]

            ai_mediq_answer, mediq_provider = generate_medical_response_with_provider(
                user_text=text,
                language=final_language,
                medical_context=answer,
                conversation_context=conversation_context,
                session_id=session_id,
            )

            if ai_mediq_answer:
                answer = localize_answer(ai_mediq_answer, final_language)
            else:
                answer = localize_answer(answer, final_language)

            save_structured_memories(
                session_id=session_id,
                text=text,
                symptoms=symptoms,
                duration=duration,
                previous_symptoms=previous_symptoms,
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
                "source": mediq_provider if ai_mediq_answer else "mediq",
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
            session_id=session_id,
            text=text,
            symptoms=symptoms,
            duration=duration,
            previous_symptoms=previous_symptoms,
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
    _ollama_info = get_ollama_status(max_age=0)
    if OLLAMA_ENABLED:
        print("AI Primary   : Ollama")
        print("Ollama URL   :", OLLAMA_BASE_URL)
        if not _ollama_info["reachable"]:
            print("Ollama Status: NOT reachable -", _ollama_info["error"])
        elif _ollama_info["model_available"]:
            print("Ollama Status: Connected, model ready:", OLLAMA_MODEL)
        else:
            print("Ollama Status: Connected, but model not installed. Run: ollama pull", OLLAMA_MODEL)
    else:
        print("AI Primary   : Ollama disabled (OLLAMA_ENABLED=0)")
    print("Gemini       :", "Configured (fallback)" if gemini_client is not None else "Not configured")
    print("API Port     : 5000")
    print("LAN URL      : http://<YOUR-LAPTOP-IP>:5000")
    print("Android BASE_URL must point to the laptop running Flask.")
    print("----------------------------------------")

    app.run(host="0.0.0.0", port=5000, debug=False)