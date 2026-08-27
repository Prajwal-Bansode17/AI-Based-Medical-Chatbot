
from pathlib import Path
import re
import sqlite3
import json
from datetime import datetime

import joblib
import pandas as pd

from flask import Flask, jsonify, request

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity


# ============================================================
# PATHS
# ============================================================

BASE_DIR = Path(__file__).resolve().parent

MODEL_FILE = (
    BASE_DIR
    / "models"
    / "medical_intent_model.joblib"
)

MEDICAL_KNOWLEDGE_DIR = (
    BASE_DIR
    / "data"
    / "raw"
    / "medical_knowledge"
)

CHAT_DB_FILE = (
    BASE_DIR
    / "data"
    / "chat_history.db"
)


# ============================================================
# FIND MEDIQ DATASET
# ============================================================

def find_mediq_dataset():

    search_root = (
        BASE_DIR
        / "data"
        / "raw"
        / "mediq"
    )

    if not search_root.exists():

        raise FileNotFoundError(
            f"MEDIQ folder not found: {search_root}"
        )

    files = list(
        search_root.rglob("mediq_full.csv")
    )

    if not files:

        raise FileNotFoundError(
            f"mediq_full.csv not found inside: {search_root}"
        )

    return files[0]


DATA_FILE = find_mediq_dataset()


# ============================================================
# FLASK APP
# ============================================================

app = Flask(__name__)


# ============================================================
# CHAT DATABASE
# ============================================================

def init_chat_database():

    CHAT_DB_FILE.parent.mkdir(
        parents=True,
        exist_ok=True
    )

    conn = sqlite3.connect(
        CHAT_DB_FILE
    )

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


def save_chat_message(
    session_id,
    role,
    message,
    symptoms=None
):

    conn = sqlite3.connect(
        CHAT_DB_FILE
    )

    symptom_text = ""

    if symptoms:

        symptom_text = ",".join(
            symptoms
        )

    conn.execute("""
        INSERT INTO chat_history
        (
            session_id,
            role,
            message,
            symptoms,
            created_at
        )
        VALUES (?, ?, ?, ?, ?)
    """, (
        session_id,
        role,
        message,
        symptom_text,
        datetime.now().isoformat()
    ))

    conn.commit()
    conn.close()


def get_chat_history(
    session_id,
    limit=20
):

    conn = sqlite3.connect(
        CHAT_DB_FILE
    )

    conn.row_factory = sqlite3.Row

    rows = conn.execute("""
        SELECT
            role,
            message,
            symptoms,
            created_at
        FROM chat_history
        WHERE session_id = ?
        ORDER BY id DESC
        LIMIT ?
    """, (
        session_id,
        limit
    )).fetchall()

    conn.close()

    # Return newest records first because callers use the first matching
    # measurement as the latest value.
    return [
        dict(row)
        for row in rows
    ]


def get_previous_symptoms(
    session_id
):

    history = get_chat_history(
        session_id,
        limit=20
    )

    symptoms = []

    for item in history:

        stored = item.get(
            "symptoms",
            ""
        )

        if not stored:
            continue

        for symptom in stored.split(","):

            symptom = symptom.strip()

            if (
                symptom
                and symptom not in symptoms
            ):

                symptoms.append(
                    symptom
                )

    return symptoms


def clear_chat_history(
    session_id
):

    conn = sqlite3.connect(
        CHAT_DB_FILE
    )

    conn.execute("""
        DELETE FROM chat_history
        WHERE session_id = ?
    """, (
        session_id,
    ))

    conn.commit()
    conn.close()


# ============================================================
# STRUCTURED MEDICAL MEMORY
# ============================================================

def init_medical_memory_table():

    conn = sqlite3.connect(
        CHAT_DB_FILE
    )

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
    session_id,
    memory_type,
    name,
    value=None,
    unit=None,
    duration=None,
    source_message=""
):

    conn = sqlite3.connect(
        CHAT_DB_FILE
    )

    conn.execute("""
        INSERT INTO medical_memory
        (
            session_id,
            memory_type,
            name,
            value,
            unit,
            duration,
            source_message,
            created_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """, (
        session_id,
        memory_type,
        name,
        str(value) if value is not None else "",
        str(unit) if unit is not None else "",
        str(duration) if duration is not None else "",
        source_message,
        datetime.now().isoformat()
    ))

    conn.commit()
    conn.close()


def get_medical_memory(
    session_id,
    limit=50
):

    conn = sqlite3.connect(
        CHAT_DB_FILE
    )

    conn.row_factory = sqlite3.Row

    rows = conn.execute("""
        SELECT
            memory_type,
            name,
            value,
            unit,
            duration,
            source_message,
            created_at
        FROM medical_memory
        WHERE session_id = ?
        ORDER BY id DESC
        LIMIT ?
    """, (
        session_id,
        limit
    )).fetchall()

    conn.close()

    # Return newest records first.  This is important because
    # memory recall must use the latest value reported by the user.
    return [
        dict(row)
        for row in rows
    ]


def clear_medical_memory(
    session_id
):

    conn = sqlite3.connect(
        CHAT_DB_FILE
    )

    conn.execute("""
        DELETE FROM medical_memory
        WHERE session_id = ?
    """, (
        session_id,
    ))

    conn.commit()
    conn.close()



# ============================================================
# CONVERSATION STATE
# ============================================================


def init_conversation_state_table():
    conn = sqlite3.connect(CHAT_DB_FILE)
    conn.execute("""
        CREATE TABLE IF NOT EXISTS conversation_state (
            session_id TEXT PRIMARY KEY,
            primary_symptom TEXT,
            duration TEXT,
            associated_symptoms TEXT,
            severity TEXT,
            step TEXT NOT NULL,
            language TEXT DEFAULT 'en',
            updated_at TEXT NOT NULL
        )
    """)
    columns = {row[1] for row in conn.execute(
        "PRAGMA table_info(conversation_state)"
    ).fetchall()}
    if "language" not in columns:
        conn.execute(
            "ALTER TABLE conversation_state ADD COLUMN language TEXT DEFAULT 'en'"
        )
    conn.commit()
    conn.close()


def detect_language(text):
    raw = str(text or "").strip().lower()
    marathi_words = {
        "मला","माझा","माझी","माझे","माझं","आहे","आहेत","ताप","पासून",
        "दिवसांपासून","कालपासून","आजपासून","किती","दिवस","डोके",
        "दुखत","खोकला","चक्कर","उलटी","जुलाब","कमजोरी","छातीत",
        "श्वास","त्रास","नाही","हो","होय","जास्त","कमी","मध्यम"
    }
    hindi_words = {
        "मुझे","मेरा","मेरी","मेरे","है","हैं","बुखार","से","दिन","कल",
        "आज","कितने","सिर","दर्द","खांसी","चक्कर","उल्टी","दस्त",
        "कमजोरी","सीने","सांस","मुश्किल","नहीं","हाँ","हां","ज्यादा",
        "कम","मध्यम"
    }
    mr_score = sum(1 for word in marathi_words if word in raw)
    hi_score = sum(1 for word in hindi_words if word in raw)
    if any(p in raw for p in ["मला ताप","ताप आला","ताप आहे","कालपासून",
                              "दिवसांपासून","मला डोके"]):
        mr_score += 3
    if any(p in raw for p in ["मुझे बुखार","बुखार है","कल से","सिर दर्द",
                              "सीने में दर्द"]):
        hi_score += 3
    if mr_score == 0 and hi_score == 0:
        return "en"
    return "mr" if mr_score >= hi_score else "hi"


DEVANAGARI_TO_ENGLISH = {
    # ---------------- MARATHI ----------------
    "मला ताप आला आहे": "i have fever",
    "मला ताप आला": "i have fever",
    "मला ताप आहे": "i have fever",
    "ताप आला आहे": "fever",
    "ताप आला": "fever",
    "ताप आहे": "fever",
    "ताप": "fever",

    "मला खोकला आहे": "i have cough",
    "खोकला": "cough",
    "सर्दी": "cold",
    "डोकेदुखी": "headache",
    "डोके दुखत आहे": "headache",
    "डोकं दुखत आहे": "headache",
    "अंगदुखी": "body pain",
    "अंग दुखत आहे": "body pain",
    "पोटदुखी": "stomach pain",
    "पोट दुखत आहे": "stomach pain",
    "छातीत दुखत आहे": "chest pain",
    "छातीत दुखणे": "chest pain",
    "छातीत वेदना": "chest pain",
    "श्वास घेण्यास त्रास": "difficulty breathing",
    "श्वास घेता येत नाही": "cannot breathe",
    "श्वास घेणे कठीण आहे": "difficulty breathing",
    "चक्कर": "dizziness",
    "उलटी": "vomiting",
    "जुलाब": "diarrhea",
    "अशक्तपणा": "weakness",
    "कमजोरी": "weakness",
    "थंडी वाजत आहे": "chills",
    "थंडी वाजणे": "chills",
    "घाम येत आहे": "sweating",

    "मला": "i",
    "माझा": "my",
    "माझी": "my",
    "माझे": "my",
    "माझं": "my",
    "आहेत": "have",
    "आहे": "have",

    # Marathi duration
    "कालपासून": "since yesterday",
    "काल पासून": "since yesterday",
    "आजपासून": "since today",
    "आज पासून": "since today",
    "दोन दिवसांपासून": "for 2 days",
    "दोन दिवसापासून": "for 2 days",
    "दोन दिवस": "2 days",
    "तीन दिवसांपासून": "for 3 days",
    "तीन दिवस": "3 days",
    "चार दिवस": "4 days",
    "पाच दिवस": "5 days",
    "एक दिवस": "1 day",
    "किती दिवस": "how many days",

    # Marathi answers/severity
    "नाही": "no",
    "नको": "no",
    "होय": "yes",
    "हो": "yes",
    "सौम्य": "mild",
    "मध्यम": "moderate",
    "तीव्र": "severe",
    "जास्त": "severe",
    "कमी": "mild",

    # ---------------- HINDI ----------------
    "मुझे बुखार है": "i have fever",
    "मुझे बुखार": "i have fever",
    "बुखार है": "fever",
    "बुखार": "fever",
    "मुझे खांसी है": "i have cough",
    "खांसी": "cough",
    "जुकाम": "cold",
    "सिर दर्द": "headache",
    "सिरदर्द": "headache",
    "बदन दर्द": "body pain",
    "शरीर दर्द": "body pain",
    "पेट दर्द": "stomach pain",
    "सीने में तेज दर्द": "severe chest pain",
    "सीने में दर्द": "chest pain",
    "सांस लेने में दिक्कत": "difficulty breathing",
    "सांस नहीं ले पा रहा": "cannot breathe",
    "चक्कर": "dizziness",
    "उल्टी": "vomiting",
    "दस्त": "diarrhea",
    "कमजोरी": "weakness",
    "ठंड लग रही है": "chills",

    "मुझे": "i",
    "मेरा": "my",
    "मेरी": "my",
    "मेरे": "my",
    "हैं": "have",
    "है": "have",

    # Hindi duration
    "कल से": "since yesterday",
    "आज से": "since today",
    "दो दिनों से": "for 2 days",
    "दो दिन से": "for 2 days",
    "दो दिन": "2 days",
    "तीन दिन": "3 days",
    "चार दिन": "4 days",
    "पांच दिन": "5 days",
    "एक दिन": "1 day",
    "कितने दिन": "how many days",

    # Hindi answers/severity
    "नहीं": "no",
    "हाँ": "yes",
    "हां": "yes",
    "हल्का": "mild",
    "हल्की": "mild",
    "मध्यम": "moderate",
    "बहुत तेज": "severe",
    "तेज": "severe",
}


def translate_input_to_english(text):
    """
    Convert common Marathi/Hindi medical phrases to English for
    the existing ML, symptom, duration and safety logic.

    The original user message is never modified for chat display/history.
    """

    raw = str(text or "").strip()

    if not raw:
        return ""

    # Longest phrases first so:
    #   "मला ताप आला आहे" -> "i have fever"
    # happens before smaller replacements such as "ताप".
    for source, target in sorted(
        DEVANAGARI_TO_ENGLISH.items(),
        key=lambda item: len(item[0]),
        reverse=True
    ):
        raw = raw.replace(source, f" {target} ")

    # Collapse whitespace introduced by replacements.
    raw = re.sub(r"\s+", " ", raw).strip()

    return raw


def _language_from_text_or_state(session_id, text):
    detected = detect_language(text)
    if detected != "en":
        return detected
    state = get_conversation_state(session_id)
    if state and state.get("language"):
        return state.get("language") or "en"
    return "en"


def save_conversation_state(session_id, primary_symptom,
                             duration=None, associated_symptoms=None,
                             severity=None, step="duration", language="en"):
    associated_symptoms = associated_symptoms or []
    conn = sqlite3.connect(CHAT_DB_FILE)
    conn.execute("""
        INSERT INTO conversation_state
        (session_id, primary_symptom, duration, associated_symptoms,
         severity, step, language, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(session_id) DO UPDATE SET
            primary_symptom=excluded.primary_symptom,
            duration=excluded.duration,
            associated_symptoms=excluded.associated_symptoms,
            severity=excluded.severity,
            step=excluded.step,
            language=excluded.language,
            updated_at=excluded.updated_at
    """, (
        session_id, primary_symptom or "", duration or "",
        json.dumps(associated_symptoms), severity or "", step,
        language or "en", datetime.now().isoformat()
    ))
    conn.commit()
    conn.close()


def get_conversation_state(session_id):
    conn = sqlite3.connect(CHAT_DB_FILE)
    conn.row_factory = sqlite3.Row
    columns = {
        row[1] for row in conn.execute(
            "PRAGMA table_info(conversation_state)"
        ).fetchall()
    }
    language_column = ", language" if "language" in columns else ""
    row = conn.execute(
        f"""SELECT session_id, primary_symptom, duration,
                   associated_symptoms, severity, step
                   {language_column}, updated_at
            FROM conversation_state
            WHERE session_id = ?""",
        (session_id,)
    ).fetchone()
    conn.close()
    if not row:
        return None
    state = dict(row)
    state.setdefault("language", "en")
    try:
        state["associated_symptoms"] = json.loads(
            state.get("associated_symptoms") or "[]"
        )
    except Exception:
        state["associated_symptoms"] = []
    return state


def clear_conversation_state(session_id):
    conn = sqlite3.connect(CHAT_DB_FILE)
    conn.execute("DELETE FROM conversation_state WHERE session_id = ?",
                 (session_id,))
    conn.commit()
    conn.close()


def update_saved_symptom_duration(session_id, symptom, duration):
    if not symptom or not duration:
        return
    conn = sqlite3.connect(CHAT_DB_FILE)
    conn.execute("""
        UPDATE medical_memory SET duration = ?
        WHERE id = (
            SELECT id FROM medical_memory
            WHERE session_id = ? AND memory_type = 'symptom' AND name = ?
            ORDER BY id DESC LIMIT 1
        )
    """, (duration, session_id, symptom))
    conn.commit()
    conn.close()


def extract_severity(text):
    query = normalize_text(text)
    patterns = {
        "severe": ["severe", "very severe", "extreme", "extremely severe",
                   "worst", "unbearable", "intense"],
        "moderate": ["moderate", "medium"],
        "mild": ["mild", "slight", "little", "minor"]
    }
    for severity, values in patterns.items():
        if any(value in query for value in values):
            return severity
    match = re.search(r"\b(10|[0-9])\s*(?:/\s*10)?\b", query)
    if match:
        score = int(match.group(1))
        return "mild" if score <= 3 else "moderate" if score <= 6 else "severe"
    return None


def _conversation_response(session_id, text, answer, symptoms,
                            confidence, intent="conversation"):
    language = _language_from_text_or_state(session_id, text)
    translated_answer = translate_conversation_response(answer, language)
    save_chat_message(session_id, "user", text, symptoms or [])
    save_chat_message(session_id, "assistant", translated_answer, symptoms or [])
    return jsonify({
        "status": "success", "session_id": session_id, "question": text,
        "intent": intent, "confidence": round(confidence, 4),
        "symptoms": symptoms or [],
        "previous_symptoms": get_previous_symptoms(session_id),
        "medical_query": True, "answer": translated_answer,
        "answer_similarity": 1.0, "source": "conversation_engine",
        "language": language
    })


def translate_conversation_response(answer, language):
    """Translate the deterministic conversation-engine responses.

    The medical engine generates a small, known set of English templates.
    Translating those templates as complete sentences is safer than replacing
    individual English fragments, which can otherwise leave English words in
    the final response.
    """
    if not answer or language == "en":
        return answer

    # ------------------------------------------------------------
    # Duration / symptom extraction from the English engine output
    # ------------------------------------------------------------
    m = re.match(
        r"^Okay, I understand you have (.+?)\. How long have you had the (.+?)\? "
        r"For example, you can say '2 days' or 'since yesterday'\.?$",
        answer.strip(),
        re.IGNORECASE,
    )
    if m:
        symptom = m.group(1).strip()
        symptom_local = {
            "fever": "ताप" if language == "mr" else "बुखार",
            "headache": "डोकेदुखी" if language == "mr" else "सिरदर्द",
            "cough": "खोकला" if language == "mr" else "खांसी",
            "cold": "सर्दी" if language == "mr" else "जुकाम",
            "body pain": "अंगदुखी" if language == "mr" else "बदन दर्द",
            "stomach pain": "पोटदुखी" if language == "mr" else "पेट दर्द",
            "chest pain": "छातीत दुखणे" if language == "mr" else "सीने में दर्द",
        }.get(symptom.lower(), symptom)
        if language == "mr":
            return (
                f"ठीक आहे, तुम्हाला {symptom_local} आहे. तुम्हाला हा {symptom_local} "
                "किती दिवसांपासून आहे? उदाहरणार्थ, तुम्ही '2 दिवस' किंवा 'कालपासून' असे सांगू शकता."
            )
        return (
            f"ठीक है, आपको {symptom_local} है। आपको यह {symptom_local} कितने समय से है? "
            "उदाहरण के लिए, आप '2 दिन' या 'कल से' कह सकते हैं।"
        )

    m = re.match(
        r"^Okay, I understand you have (.+?) and it has been present for about (.+?)\. "
        r"Do you also have headache, cough, chills, body pain, vomiting, or dizziness\?$",
        answer.strip(), re.IGNORECASE,
    )
    if m:
        symptom = m.group(1).strip()
        duration = m.group(2).strip()
        symptom_local = {
            "fever": "ताप" if language == "mr" else "बुखार",
            "headache": "डोकेदुखी" if language == "mr" else "सिरदर्द",
            "cough": "खोकला" if language == "mr" else "खांसी",
            "chest pain": "छातीत दुखणे" if language == "mr" else "सीने में दर्द",
        }.get(symptom.lower(), symptom)
        if language == "mr":
            return (
                f"ठीक आहे, तुम्हाला {symptom_local} आहे आणि तो सुमारे {duration} पासून आहे. "
                "तुम्हाला डोकेदुखी, खोकला, थंडी वाजणे, अंगदुखी, उलटी किंवा चक्कर यापैकी काही आहे का?"
            )
        return (
            f"ठीक है, आपको {symptom_local} है और यह लगभग {duration} से है। "
            "क्या आपको सिरदर्द, खांसी, ठंड लगना, बदन दर्द, उल्टी या चक्कर भी हैं?"
        )

    m = re.match(
        r"^Got it\. You have had (.+?) for about (.+?)\. Do you also have headache, cough, chills, body pain, vomiting, or dizziness\?$",
        answer.strip(), re.IGNORECASE,
    )
    if m:
        symptom = m.group(1).strip()
        duration = m.group(2).strip()
        symptom_local = {
            "fever": "ताप" if language == "mr" else "बुखार",
            "headache": "डोकेदुखी" if language == "mr" else "सिरदर्द",
            "cough": "खोकला" if language == "mr" else "खांसी",
        }.get(symptom.lower(), symptom)
        if language == "mr":
            return (
                f"समजले. तुम्हाला {symptom_local} सुमारे {duration} पासून आहे. "
                "तुम्हाला डोकेदुखी, खोकला, थंडी वाजणे, अंगदुखी, उलटी किंवा चक्कर यापैकी काही आहे का?"
            )
        return (
            f"समझ गया। आपको {symptom_local} लगभग {duration} से है। "
            "क्या आपको सिरदर्द, खांसी, ठंड लगना, बदन दर्द, उल्टी या चक्कर भी हैं?"
        )

    m = re.match(
        r"^Thanks\. How would you describe the (.+?) severity: mild, moderate, or severe\?$",
        answer.strip(), re.IGNORECASE,
    )
    if m:
        symptom = m.group(1).strip()
        symptom_local = {
            "fever": "तापाची" if language == "mr" else "बुखार की",
            "headache": "डोकेदुखीची" if language == "mr" else "सिरदर्द की",
            "chest pain": "छातीत दुखण्याची" if language == "mr" else "सीने के दर्द की",
        }.get(symptom.lower(), symptom)
        if language == "mr":
            return f"धन्यवाद. तुमच्या {symptom_local} तीव्रतेचे वर्णन सौम्य, मध्यम किंवा तीव्र असे कसे कराल?"
        return f"धन्यवाद। आपके {symptom_local} की गंभीरता हल्की, मध्यम या तेज कैसी है?"

    exact = {
        "How severe is it? You can say mild, moderate, severe, or give a pain score from 0 to 10.": {
            "mr": "ते किती तीव्र आहे? तुम्ही सौम्य, मध्यम किंवा तीव्र असे सांगू शकता किंवा 0 ते 10 पैकी वेदना गुण देऊ शकता.",
            "hi": "यह कितना गंभीर है? आप हल्का, मध्यम या तेज कह सकते हैं या 0 से 10 तक दर्द का स्कोर बता सकते हैं।",
        },
        "Thanks. Before I give general guidance, are you having difficulty breathing, severe chest pain, fainting, confusion, heavy bleeding, or another severe/emergency symptom?": {
            "mr": "धन्यवाद. सामान्य मार्गदर्शन देण्यापूर्वी, तुम्हाला श्वास घेण्यास त्रास, तीव्र छातीत दुखणे, बेशुद्ध पडणे, गोंधळ, जास्त रक्तस्राव किंवा इतर गंभीर/आपत्कालीन लक्षण आहे का?",
            "hi": "धन्यवाद। सामान्य जानकारी देने से पहले, क्या आपको सांस लेने में दिक्कत, तेज सीने में दर्द, बेहोशी, भ्रम, ज्यादा रक्तस्राव या कोई अन्य गंभीर/आपातकालीन लक्षण है?",
        },
        "Please answer yes or no.": {
            "mr": "कृपया हो किंवा नाही असे उत्तर द्या.",
            "hi": "कृपया हाँ या नहीं में उत्तर दें।",
        },
        "Those symptoms can be signs of a medical emergency. Please seek urgent medical care now or contact your local emergency service. Do not rely on this chatbot for emergency care.": {
            "mr": "ही लक्षणे वैद्यकीय आपत्कालीन स्थितीची चिन्हे असू शकतात. कृपया तातडीने वैद्यकीय मदत घ्या किंवा स्थानिक आपत्कालीन सेवेशी संपर्क करा. आपत्कालीन उपचारांसाठी या चॅटबॉटवर अवलंबून राहू नका.",
            "hi": "ये लक्षण चिकित्सकीय आपातकाल के संकेत हो सकते हैं। कृपया तुरंत चिकित्सा सहायता लें या स्थानीय आपातकालीन सेवा से संपर्क करें। आपातकालीन उपचार के लिए इस चैटबॉट पर निर्भर न रहें।",
        },
        "Your message includes a potentially serious emergency symptom. Please seek urgent medical assessment now. If the symptom is severe, sudden, worsening, or you feel unsafe, contact your local emergency service or go to the nearest emergency department. Do not rely on this chatbot to determine the cause.": {
            "mr": "तुमच्या संदेशात संभाव्यतः गंभीर आपत्कालीन लक्षण आहे. कृपया आत्ताच तातडीची वैद्यकीय तपासणी करून घ्या. लक्षण गंभीर, अचानक किंवा वाढत असल्यास किंवा तुम्हाला असुरक्षित वाटत असल्यास स्थानिक आपत्कालीन सेवेशी संपर्क करा किंवा जवळच्या आपत्कालीन विभागात जा. कारण निश्चित करण्यासाठी या चॅटबॉटवर अवलंबून राहू नका.",
            "hi": "आपके संदेश में संभावित रूप से गंभीर आपातकालीन लक्षण है। कृपया अभी तुरंत चिकित्सा जांच करवाएं। यदि लक्षण गंभीर, अचानक या बढ़ रहे हैं, या आपको असुरक्षित महसूस हो रहा है, तो स्थानीय आपातकालीन सेवा से संपर्क करें या नजदीकी आपातकालीन विभाग में जाएं। कारण जानने के लिए इस चैटबॉट पर निर्भर न रहें।",
        },
    }
    if answer.strip() in exact:
        return exact[answer.strip()].get(language, answer)

    # Final safety net: never return a partially translated English
    # conversation response for a detected Indian-language session.
    if language == "mr":
        return "कृपया तुमची लक्षणे आणि त्यांचा कालावधी सांगा. मी सामान्य वैद्यकीय मार्गदर्शन देऊ शकतो."
    if language == "hi":
        return "कृपया अपने लक्षण और उनकी अवधि बताएं। मैं सामान्य चिकित्सकीय जानकारी दे सकता हूँ।"
    return answer


def translate_general_response(answer, language):
    if not answer or language == "en":
        return answer
    if language == "mr":
        return (
            "मी मुख्यतः वैद्यकीय आणि आरोग्याशी संबंधित प्रश्नांमध्ये मदत करू शकतो. "
            "तुम्ही मला लक्षणे, आजार, सामान्य आरोग्यविषयक माहिती, औषधे किंवा "
            "वैद्यकीय मदत कधी घ्यावी याबद्दल विचारू शकता."
        )
    if language == "hi":
        return (
            "मैं मुख्य रूप से चिकित्सा और स्वास्थ्य से जुड़े सवालों में मदद कर सकता हूँ। "
            "आप मुझसे लक्षणों, बीमारियों, सामान्य स्वास्थ्य जानकारी, दवाओं या "
            "चिकित्सकीय सहायता कब लेनी चाहिए, इसके बारे में पूछ सकते हैं।"
        )
    return answer



def build_conversation_guidance(state):
    symptoms = []
    primary = state.get("primary_symptom")
    if primary:
        symptoms.append(primary)
    for symptom in state.get("associated_symptoms") or []:
        if symptom and symptom not in symptoms:
            symptoms.append(symptom)

    response = build_multi_symptom_response(symptoms, "assistance") if symptoms else None
    if not response:
        response = (
            "Thanks for sharing those details. I can provide general medical "
            "information, but I cannot diagnose a condition."
        )
    context = []
    if state.get("duration"):
        context.append(f"You reported these symptoms for about {state['duration']}.")
    if state.get("severity"):
        context.append(f"You described the severity as {state['severity']}.")
    if context:
        response += "\n\n" + " ".join(context)
    response += (
        "\n\nIf symptoms become severe, rapidly worsen, or you develop "
        "difficulty breathing, severe chest pain, fainting, confusion, "
        "or heavy bleeding, seek urgent medical care."
    )
    return response



def translate_medical_response(answer, language):
    """Localize common deterministic medical-knowledge phrases."""
    if not answer or language == "en":
        return answer

    translations = {
        "mr": [
            ("Here is some general medical information.",
             "ही काही सामान्य वैद्यकीय माहिती आहे."),
            ("This information does not provide a diagnosis.",
             "ही माहिती निदान देत नाही."),
            ("Fever:", "ताप:"),
            ("Common symptoms associated with fever can include increased body temperature, chills, sweating, headache, body aches, weakness, and tiredness.",
             "तापासोबत शरीराचे तापमान वाढणे, थंडी वाजणे, घाम येणे, डोकेदुखी, अंगदुखी, अशक्तपणा आणि थकवा अशी सामान्य लक्षणे दिसू शकतात."),
            ("The cause of fever can vary, so symptoms alone cannot confirm a specific disease.",
             "तापाची कारणे वेगवेगळी असू शकतात, त्यामुळे केवळ लक्षणांवरून विशिष्ट आजार निश्चित करता येत नाही."),
            ("Important:", "महत्त्वाचे:"),
            ("If symptoms are severe, worsening, persistent, or you have an emergency symptom, seek urgent medical care.",
             "लक्षणे गंभीर, वाढत जाणारी किंवा सतत राहणारी असतील किंवा आपत्कालीन लक्षण दिसत असेल तर तातडीने वैद्यकीय मदत घ्या."),
        ],
        "hi": [
            ("Here is some general medical information.",
             "यह कुछ सामान्य चिकित्सा जानकारी है।"),
            ("This information does not provide a diagnosis.",
             "यह जानकारी निदान प्रदान नहीं करती है।"),
            ("Fever:", "बुखार:"),
            ("Common symptoms associated with fever can include increased body temperature, chills, sweating, headache, body aches, weakness, and tiredness.",
             "बुखार के साथ शरीर का तापमान बढ़ना, ठंड लगना, पसीना आना, सिरदर्द, बदन दर्द, कमजोरी और थकान जैसे सामान्य लक्षण हो सकते हैं।"),
            ("The cause of fever can vary, so symptoms alone cannot confirm a specific disease.",
             "बुखार के कारण अलग-अलग हो सकते हैं, इसलिए केवल लक्षणों के आधार पर किसी विशेष बीमारी की पुष्टि नहीं की जा सकती।"),
            ("Important:", "महत्वपूर्ण:"),
            ("If symptoms are severe, worsening, persistent, or you have an emergency symptom, seek urgent medical care.",
             "यदि लक्षण गंभीर, बढ़ते हुए या लगातार बने रहें, या कोई आपातकालीन लक्षण हो, तो तुरंत चिकित्सा सहायता लें।"),
        ],
    }

    result = answer
    for source, target in translations.get(language, []):
        result = result.replace(source, target)
    return result


def handle_conversation_turn(session_id, text, symptoms, duration,
                             confidence, final_intent):
    state = get_conversation_state(session_id)
    detected_language = detect_language(text)
    language = (
        detected_language
        if detected_language != "en"
        else (state.get("language") if state else "en")
    ) or "en"
    analysis_text = (
        translate_input_to_english(text)
        if language in {"mr", "hi"}
        else text
    )
    query = normalize_text(analysis_text)

    if state:
        if language != state.get("language"):
            save_conversation_state(
                session_id, state.get("primary_symptom") or "symptom",
                state.get("duration"), state.get("associated_symptoms", []),
                state.get("severity"), state.get("step") or "duration",
                language
            )
            state["language"] = language

        step = state.get("step") or "duration"
        primary = state.get("primary_symptom") or "symptom"
        display_primary = primary.replace("_", " ")

        if step == "duration":
            if duration:
                update_saved_symptom_duration(session_id, primary, duration)
                state["duration"] = duration
                save_conversation_state(
                    session_id, primary, duration,
                    state.get("associated_symptoms", []),
                    state.get("severity"), "associated_symptoms", language
                )
                return _conversation_response(
                    session_id, text,
                    f"Got it. You have had {display_primary} for about {duration}. "
                    "Do you also have headache, cough, chills, body pain, vomiting, or dizziness?",
                    [], confidence, "conversation_follow_up"
                )
            return _conversation_response(
                session_id, text,
                f"How long have you had the {display_primary}? For example, you can say '2 days' or 'since yesterday'.",
                [], confidence, "conversation_follow_up"
            )

        if step == "associated_symptoms":
            new_symptoms = [s for s in symptoms if s != primary]
            if query in {"no", "nope", "none", "no other symptoms", "not really", "nothing else"}:
                new_symptoms = []
            for symptom in new_symptoms:
                save_medical_memory(
                    session_id, "symptom", symptom,
                    duration=state.get("duration"), source_message=text
                )
            save_conversation_state(
                session_id, primary, state.get("duration"), new_symptoms,
                state.get("severity"), "severity", language
            )
            return _conversation_response(
                session_id, text,
                f"Thanks. How would you describe the {display_primary} severity: mild, moderate, or severe?",
                new_symptoms, confidence, "conversation_follow_up"
            )

        if step == "severity":
            severity = extract_severity(analysis_text)
            if not severity:
                return _conversation_response(
                    session_id, text,
                    "How severe is it? You can say mild, moderate, severe, or give a pain score from 0 to 10.",
                    [], confidence, "conversation_follow_up"
                )
            save_conversation_state(
                session_id, primary, state.get("duration"),
                state.get("associated_symptoms", []), severity,
                "red_flags", language
            )
            return _conversation_response(
                session_id, text,
                "Thanks. Before I give general guidance, are you having difficulty breathing, severe chest pain, fainting, confusion, heavy bleeding, or another severe/emergency symptom?",
                [], confidence, "conversation_safety_check"
            )

        if step == "red_flags":
            emergency_phrases = [
                "difficulty breathing", "cannot breathe", "cant breathe",
                "severe chest pain", "fainting", "confusion", "heavy bleeding",
                "shortness of breath", "coughing blood", "vomiting blood",
                "seizure", "blue lips"
            ]
            yes = query in {"yes", "yeah", "yep"}
            has_emergency = yes or any(p in query for p in emergency_phrases)
            if has_emergency:
                clear_conversation_state(session_id)
                return _conversation_response(
                    session_id, text,
                    "Those symptoms can be signs of a medical emergency. Please seek urgent medical care now or contact your local emergency service. Do not rely on this chatbot for emergency care.",
                    [], confidence, "emergency"
                )
            if query not in {"no", "nope", "none", "not really"}:
                return _conversation_response(
                    session_id, text,
                    "Please answer yes or no. Are you having difficulty breathing, severe chest pain, fainting, confusion, heavy bleeding, or another severe/emergency symptom?",
                    [], confidence, "conversation_safety_check"
                )
            answer = build_conversation_guidance(state)
            clear_conversation_state(session_id)
            return _conversation_response(
                session_id, text, answer, [], confidence, "symptom_assistance"
            )

    if symptoms and final_intent == "symptom_assistance":
        primary = symptoms[0]
        other_symptoms = [s for s in symptoms if s != primary]
        language = detected_language if detected_language != "en" else "en"

        if duration:
            step = "associated_symptoms"
            answer = (
                f"Okay, I understand you have {primary.replace('_', ' ')} and it has been present for about {duration}. "
                "Do you also have headache, cough, chills, body pain, vomiting, or dizziness?"
            )
        else:
            step = "duration"
            answer = (
                f"Okay, I understand you have {primary.replace('_', ' ')}. "
                f"How long have you had the {primary.replace('_', ' ')}? For example, you can say '2 days' or 'since yesterday'."
            )
        save_structured_memories(session_id, text, symptoms, duration)
        save_conversation_state(
            session_id, primary, duration, other_symptoms,
            extract_severity(analysis_text), step, language
        )
        return _conversation_response(
            session_id, text, answer, symptoms, confidence, "conversation_start"
        )

    return None


init_chat_database()
init_medical_memory_table()
init_conversation_state_table()


# ============================================================
# LOAD ML MODEL
# ============================================================

print("Loading ML model...")

if not MODEL_FILE.exists():

    raise FileNotFoundError(
        f"ML model not found: {MODEL_FILE}"
    )

model = joblib.load(
    MODEL_FILE
)

print(
    "ML model loaded successfully."
)


# ============================================================
# LOAD MEDIQ DATASET
# ============================================================

print("Loading MEDIQ dataset...")

dataset = pd.read_csv(
    DATA_FILE
)

print(
    "MEDIQ dataset loaded successfully."
)

print("----------------------------------------")
print(
    "Dataset shape:",
    dataset.shape
)
print("----------------------------------------")


# ============================================================
# LOAD MEDICAL KNOWLEDGE
# ============================================================

print(
    "Loading Medical Knowledge Base..."
)


def load_medical_knowledge():

    if not MEDICAL_KNOWLEDGE_DIR.exists():
        raise FileNotFoundError(
            "Medical Knowledge directory not found: "
            f"{MEDICAL_KNOWLEDGE_DIR}"
        )

    csv_files = sorted(MEDICAL_KNOWLEDGE_DIR.glob("*.csv"))

    if not csv_files:
        raise FileNotFoundError(
            "No medical knowledge CSV files found in: "
            f"{MEDICAL_KNOWLEDGE_DIR}"
        )

    frames = []
    required_columns = {"symptom", "aliases", "question_type", "response"}

    for csv_file in csv_files:
        try:
            frame = pd.read_csv(csv_file)
        except Exception as exc:
            print(f"Skipping {csv_file.name}: {exc}")
            continue

        frame.columns = [str(c).strip().lower() for c in frame.columns]
        missing = required_columns - set(frame.columns)

        if missing:
            print(f"Skipping {csv_file.name}: missing columns {sorted(missing)}")
            continue

        frame = frame[["symptom", "aliases", "question_type", "response"]].copy()

        for column in ["symptom", "aliases", "question_type", "response"]:
            frame[column] = frame[column].fillna("").astype(str).str.strip()

        frame["symptom"] = frame["symptom"].str.lower()
        frame["question_type"] = frame["question_type"].str.lower()
        frame = frame[(frame["symptom"] != "") & (frame["response"] != "")]

        if not frame.empty:
            frames.append(frame)
            print(f"Loaded medical KB: {csv_file.name} ({len(frame)} records)")

    if not frames:
        raise ValueError("No valid medical knowledge records were loaded.")

    return pd.concat(frames, ignore_index=True).drop_duplicates(
        subset=["symptom", "aliases", "question_type", "response"]
    ).reset_index(drop=True)


medical_knowledge = load_medical_knowledge()

print("Medical Knowledge Base loaded successfully.")
print("----------------------------------------")
print("Medical Knowledge records:", len(medical_knowledge))
print("Medical KB files:", len(list(MEDICAL_KNOWLEDGE_DIR.glob("*.csv"))))
print("----------------------------------------")


# ============================================================
# FIND DATASET COLUMNS
# ============================================================

def find_column(
    columns,
    possible_names
):

    mapping = {
        str(column).strip().lower(): column
        for column in columns
    }

    for name in possible_names:

        if name.lower() in mapping:

            return mapping[
                name.lower()
            ]

    return None


QUESTION_COLUMN = find_column(
    dataset.columns,
    [
        "question",
        "text",
        "query",
        "user_question"
    ]
)


ANSWER_COLUMN = find_column(
    dataset.columns,
    [
        "answer",
        "response",
        "bot_answer",
        "reply"
    ]
)


CATEGORY_COLUMN = find_column(
    dataset.columns,
    [
        "category",
        "intent",
        "topic"
    ]
)


if QUESTION_COLUMN is None:

    raise ValueError(
        "Question column not found."
    )


if ANSWER_COLUMN is None:

    raise ValueError(
        "Answer column not found."
    )


# ============================================================
# TEXT NORMALIZATION
# ============================================================

def normalize_text(text):

    text = str(text).lower()

    replacements = {

        "pn": "also",
        "aahe": "have",
        "ahe": "have",

        "mala": "i",
        "majha": "my",
        "maza": "my",
        "majhi": "my",

        "kay": "what",

        "bukhar": "fever",
        "sardi": "cold",
        "khokla": "cough",

        "kamjori": "weakness",
        "chakkar": "dizziness",

        "dukht": "pain",
        "dukh": "pain",
        "dard": "pain"
    }

    words = text.split()

    normalized_words = []

    for word in words:

        clean_word = re.sub(
            r"[^a-zA-Z0-9]",
            "",
            word
        )

        if clean_word in replacements:

            clean_word = replacements[
                clean_word
            ]

        normalized_words.append(
            clean_word
        )

    text = " ".join(
        normalized_words
    )


    # ========================================================
    # SPELLING CORRECTION
    # ========================================================

    spelling_map = {

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

        "throate": "throat"
    }

    words = text.split()

    words = [
        spelling_map.get(
            word,
            word
        )
        for word in words
    ]

    text = " ".join(words)


    # ========================================================
    # PHRASE NORMALIZATION
    # ========================================================

    phrase_map = {

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

        "problem breathing": "breathing"
    }

    for old, new in phrase_map.items():

        text = text.replace(
            old,
            new
        )


    text = re.sub(
        r"[^a-z0-9\s]",
        " ",
        text
    )

    text = re.sub(
        r"\s+",
        " ",
        text
    )

    return text.strip()


# ============================================================
# CLEAN MEDIQ
# ============================================================

dataset = dataset.copy()

dataset[
    QUESTION_COLUMN
] = (
    dataset[
        QUESTION_COLUMN
    ]
    .fillna("")
    .astype(str)
    .str.strip()
)

dataset[
    ANSWER_COLUMN
] = (
    dataset[
        ANSWER_COLUMN
    ]
    .fillna("")
    .astype(str)
    .str.strip()
)

dataset = dataset[
    (dataset[QUESTION_COLUMN] != "")
    &
    (dataset[ANSWER_COLUMN] != "")
].copy()

dataset.reset_index(
    drop=True,
    inplace=True
)

dataset[
    "_question_clean"
] = (
    dataset[
        QUESTION_COLUMN
    ]
    .apply(normalize_text)
)


# ============================================================
# TF-IDF
# ============================================================

print(
    "Creating MEDIQ retrieval index..."
)

vectorizer = TfidfVectorizer(
    lowercase=True,
    stop_words="english",
    ngram_range=(1, 2),
    min_df=1
)

retrieval_matrix = (
    vectorizer.fit_transform(
        dataset[
            "_question_clean"
        ]
    )
)

print(
    "MEDIQ retrieval index ready."
)


# ============================================================
# SYMPTOM ALIASES
# ============================================================

SYMPTOM_ALIASES = {

    "fever": [
        "fever",
        "fevr",
        "fvr",
        "feaver",
        "temperature",
        "bukhar"
    ],

    "headache": [
        "headache",
        "headpain",
        "head pain",
        "head ache",
        "head is paining",
        "hedache"
    ],

    "cough": [
        "cough",
        "caugh",
        "coff",
        "khokla"
    ],

    "cold": [
        "cold",
        "sardi",
        "running nose",
        "runny nose"
    ],

    "body_pain": [
        "body pain",
        "bodypain",
        "body ache",
        "bodyache"
    ],

    "stomach_pain": [
        "stomach pain",
        "stomachpain",
        "stomach ache",
        "stomachache"
    ],

    "chest_pain": [
        "chest pain",
        "chestpain",
        "chest ache"
    ],

    "back_pain": [
        "back pain",
        "backpain"
    ],

    "joint_pain": [
        "joint pain",
        "jointpain"
    ],

    "vomiting": [
        "vomiting",
        "vomit",
        "vomitting",
        "throwing up"
    ],

    "nausea": [
        "nausea",
        "feeling sick",
        "sick feeling"
    ],

    "diarrhea": [
        "diarrhea",
        "diarhea",
        "loose motion",
        "loose motions"
    ],

    "dizziness": [
        "dizziness",
        "dizines",
        "dizzyness",
        "chakkar"
    ],

    "weakness": [
        "weakness",
        "weaknes",
        "kamjori",
        "tired",
        "fatigue"
    ],

    "breathing_problem": [
        "breathing",
        "breathless",
        "difficulty breathing",
        "problem breathing",
        "shortness of breath"
    ],

    "sore_throat": [
        "sore throat",
        "sorethroat",
        "throat pain",
        "throat ache"
    ],

    "rash": [
        "rash",
        "skin rash",
        "itchy rash"
    ],

    "swelling": [
        "swelling",
        "swollen"
    ]
}

# ============================================================
# ADDITIONAL MEDICAL TOPIC ALIASES
# ============================================================

MEDICAL_TOPIC_ALIASES = {
    "diabetes": ["diabetes", "diabetic", "sugar disease", "blood sugar"],
    "anemia": ["anemia", "anaemia", "low hemoglobin", "low haemoglobin"],
    "hemoglobin": ["hemoglobin", "haemoglobin", "hb level", "low hb"],
    "asthma": ["asthma", "asthmatic"],
    "migraine": ["migraine", "migraine headache"],
    "dengue": ["dengue"],
    "malaria": ["malaria"],
    "typhoid": ["typhoid"],
    "hypertension": ["hypertension", "high blood pressure", "high bp"],
    "hypotension": ["hypotension", "low blood pressure", "low bp"],
    "dehydration": ["dehydration", "dehydrated"],
    "paracetamol": ["paracetamol", "acetaminophen", "calpol"],
    "ibuprofen": ["ibuprofen"],
    "aspirin": ["aspirin"],
    "cetirizine": ["cetirizine"],
    "antibiotic": ["antibiotic", "antibiotics"]
}


def extract_medical_topics(text):

    normalized = normalize_text(text)
    detected = []

    for topic, aliases in MEDICAL_TOPIC_ALIASES.items():
        for alias in aliases:
            alias_normalized = normalize_text(alias)

            if (
                normalized == alias_normalized
                or f" {alias_normalized} " in f" {normalized} "
            ):
                if topic not in detected:
                    detected.append(topic)
                break

    # Automatically use aliases from every medical knowledge CSV.
    for _, row in medical_knowledge.iterrows():
        topic = str(row.get("symptom", "")).strip().lower()
        aliases = str(row.get("aliases", "")).split("|")

        if not topic:
            continue

        for alias in [topic] + aliases:
            alias_normalized = normalize_text(alias)

            if (
                alias_normalized
                and (
                    normalized == alias_normalized
                    or f" {alias_normalized} " in f" {normalized} "
                )
            ):
                if topic not in detected:
                    detected.append(topic)
                break

    return detected


# ============================================================
# EXTRACT SYMPTOMS
# ============================================================

def extract_symptoms(text):

    normalized = normalize_text(
        text
    )

    detected = []

    for symptom, aliases in (
        SYMPTOM_ALIASES.items()
    ):

        for alias in aliases:

            alias_normalized = (
                normalize_text(alias)
            )

            if (
                alias_normalized
                in normalized
            ):

                if symptom not in detected:

                    detected.append(
                        symptom
                    )

                break

    return detected


# ============================================================
# MEDICAL KEYWORDS
# ============================================================

MEDICAL_KEYWORDS = {

    "symptom",
    "symptoms",
    "disease",
    "illness",

    "medicine",
    "medication",
    "tablet",

    "doctor",
    "hospital",

    "treatment",
    "diagnosis",

    "pain",
    "fever",
    "headache",
    "cough",
    "cold",

    "vomiting",
    "nausea",

    "weakness",
    "dizziness",

    "rash",
    "swelling",

    "breathing",
    "infection",

    "blood",
    "health",
    "healthy",

    "pregnancy",
    "diabetes",
    "cancer",
    "asthma",
    "allergy",

    "emergency",
    "injury",
    "wound",

    "dose",
    "dosage"
}


# ============================================================
# MEDICAL QUERY
# ============================================================

def is_medical_query(text):

    normalized = normalize_text(
        text
    )

    words = set(
        normalized.split()
    )

    if words.intersection(
        MEDICAL_KEYWORDS
    ):

        return True

    if extract_symptoms(text):

        return True

    if extract_medical_topics(text):

        return True

    if extract_medical_measurements(text):

        return True

    return False


# ============================================================
# FOLLOW-UP DETECTION
# ============================================================

def is_followup_query(text):

    query = normalize_text(
        text
    )

    followup_patterns = [

        "since",
        "for",
        "from",
        "days",
        "day",
        "hours",
        "hour",
        "weeks",
        "week",
        "months",
        "month",

        "yes",
        "no",

        "also",
        "still",
        "now",
        "today",
        "yesterday",

        "same",
        "worse",
        "better",

        "it is",
        "its",
        "this",
        "that"
    ]

    for pattern in followup_patterns:

        if pattern in query:

            return True

    return False


# ============================================================
# GET QUESTION TYPE
# ============================================================

def get_question_type(text):

    query = normalize_text(
        text
    )

    information_patterns = [

        "what are the symptoms",
        "symptoms of",
        "symptoms for",
        "signs of",
        "symptom of",
        "what symptoms",
        "symptoms"
    ]

    for pattern in information_patterns:

        if pattern in query:

            return "symptoms"


    assistance_patterns = [

        "i have",
        "i am having",
        "i feel",
        "i am feeling",
        "suffering from",
        "experiencing",
        "mala",
        "majha",
        "maza",
        "mujhe",
        "mere"
    ]

    for pattern in assistance_patterns:

        if pattern in query:

            return "assistance"


    return "symptoms"


# ============================================================
# GET KNOWLEDGE RESPONSE
# ============================================================

def get_knowledge_response(
    symptom,
    question_type
):

    rows = medical_knowledge[
        medical_knowledge[
            "symptom"
        ]
        .astype(str)
        .str.strip()
        == symptom
    ]

    if rows.empty:

        return None


    matching = rows[
        rows[
            "question_type"
        ]
        .astype(str)
        .str.strip()
        .str.lower()
        ==
        question_type
    ]


    if matching.empty:

        matching = rows


    if matching.empty:

        return None


    return str(
        matching.iloc[0][
            "response"
        ]
    ).strip()


# ============================================================
# DIRECT MEDICAL TOPIC RESPONSE
# ============================================================

def get_topic_knowledge_response(
    topics,
    question_type
):
    responses = []

    for topic in topics:
        rows = medical_knowledge[
            medical_knowledge["symptom"]
            .astype(str)
            .str.strip()
            .str.lower()
            == topic.lower()
        ]

        if rows.empty:
            continue

        matching = rows[
            rows["question_type"]
            .astype(str)
            .str.strip()
            .str.lower()
            == question_type.lower()
        ]

        if matching.empty:
            matching = rows

        response = str(
            matching.iloc[0]["response"]
        ).strip()

        if response:
            title = topic.replace("_", " ").title()
            responses.append(f"{title}:\n{response}")

    if not responses:
        return None

    return (
        "Here is some general medical information. "
        "This information does not provide a diagnosis.\n\n"
        + "\n\n".join(responses)
        + "\n\nImportant: If symptoms are severe, worsening, "
        "persistent, or you have an emergency symptom, seek "
        "urgent medical care."
    )


# ============================================================
# MULTI SYMPTOM RESPONSE
# ============================================================

def build_multi_symptom_response(
    symptoms,
    question_type
):

    responses = []

    for symptom in symptoms:

        response = (
            get_knowledge_response(
                symptom,
                question_type
            )
        )

        if response:

            responses.append(
                (
                    symptom,
                    response
                )
            )


    if not responses:

        return None


    symptom_names = [

        symptom.replace(
            "_",
            " "
        )

        for symptom, _ in responses
    ]


    if len(symptom_names) == 1:

        intro = (
            f"Here is some general information "
            f"about {symptom_names[0]}."
        )

    elif len(symptom_names) == 2:

        intro = (
            f"You mentioned {symptom_names[0]} "
            f"and {symptom_names[1]}. "
            "These symptoms can occur together "
            "for several reasons, and symptoms alone "
            "cannot confirm a specific disease."
        )

    else:

        intro = (
            "You mentioned multiple symptoms. "
            "These symptoms can have different causes, "
            "and symptoms alone cannot confirm a "
            "specific disease."
        )


    sections = []

    for symptom, response in responses:

        title = (
            symptom
            .replace(
                "_",
                " "
            )
            .title()
        )

        sections.append(
            f"{title}:\n{response}"
        )


    combined = (
        intro
        + "\n\n"
        + "\n\n".join(
            sections
        )
    )


    combined += (

        "\n\nImportant: This information is for "
        "general guidance and does not provide a "
        "diagnosis. If your symptoms are severe, "
        "worsening, persistent, or you develop "
        "difficulty breathing, confusion, fainting, "
        "severe chest pain, or another emergency "
        "symptom, seek urgent medical care."
    )


    return combined


# ============================================================
# EXTRACT DURATION
# ============================================================

def extract_duration(text):

    query = normalize_text(text)

    # --------------------------------------------------------
    # Natural-language duration phrases
    # --------------------------------------------------------
    # These are especially important when the user reports the
    # duration in the same message as the symptom, e.g.
    # "I have fever since yesterday".
    relative_phrases = [
        (r"\bsince yesterday\b", "since yesterday"),
        (r"\bsince today\b", "since today"),
        (r"\bsince this morning\b", "since this morning"),
        (r"\bsince this afternoon\b", "since this afternoon"),
        (r"\bsince this evening\b", "since this evening"),
        (r"\bsince last night\b", "since last night"),
        (r"\bsince last evening\b", "since last evening"),
        (r"\bsince last week\b", "since last week"),
        (r"\bsince yesterday morning\b", "since yesterday morning"),
        (r"\bsince yesterday evening\b", "since yesterday evening"),
        (r"\bsince yesterday night\b", "since yesterday night"),
    ]

    for pattern, value in relative_phrases:
        if re.search(pattern, query):
            return value

    # "since 2 days ago", "for 2 days", "2 days"
    patterns = [
        r"(?:since|for)\s+(\d+)\s*(day|days|hour|hours|week|weeks|month|months)(?:\s+ago)?",
        r"(\d+)\s*(day|days|hour|hours|week|weeks|month|months)",
    ]

    for pattern in patterns:
        match = re.search(pattern, query)

        if match:
            number = match.group(1)
            unit = match.group(2)

            unit_map = {
                "day": "day" if number == "1" else "days",
                "days": "day" if number == "1" else "days",
                "hour": "hour" if number == "1" else "hours",
                "hours": "hour" if number == "1" else "hours",
                "week": "week" if number == "1" else "weeks",
                "weeks": "week" if number == "1" else "weeks",
                "month": "month" if number == "1" else "months",
                "months": "month" if number == "1" else "months",
            }

            prefix = "since " if re.search(r"\bsince\b", match.group(0)) else ""
            return f"{prefix}{number} {unit_map.get(unit, unit)}".strip()

    return None


# ============================================================
# STRUCTURED MEDICAL INFORMATION EXTRACTION
# ============================================================

def extract_medical_measurements(text):

    query = normalize_text(text)

    results = []

    patterns = [

        (
            "hemoglobin",
            r"(?:hemoglobin|haemoglobin|hb)\s*"
            r"(?:is|of|around|about|=|:)?\s*"
            r"(\d+(?:\.\d+)?)\s*"
            r"(g\s*/?\s*dl|gdl)?",
            "g/dL"
        ),

        (
            "blood_pressure",
            r"(?:blood pressure|bp)\s*"
            r"(?:is|of|around|about|=|:)?\s*"
            r"(\d{2,3})\s*(?:/|over)\s*"
            r"(\d{2,3})",
            "mmHg"
        ),

        (
            "temperature",
            r"(?:temperature|temp)\s*"
            r"(?:is|of|around|about|=|:)?\s*"
            r"(\d+(?:\.\d+)?)\s*"
            r"(f|c|fahrenheit|celsius)?",
            ""
        ),

        (
            "blood_sugar",
            r"(?:blood sugar|sugar|glucose)\s*"
            r"(?:is|of|around|about|=|:)?\s*"
            r"(\d+(?:\.\d+)?)\s*"
            r"(mg\s*/?\s*dl|mgdl)?",
            "mg/dL"
        ),

        (
            "heart_rate",
            r"(?:heart rate|pulse)\s*"
            r"(?:is|of|around|about|=|:)?\s*"
            r"(\d{2,3})\s*"
            r"(?:bpm)?",
            "bpm"
        ),

        (
            "oxygen_saturation",
            r"(?:oxygen saturation|spo2|sao2|oxygen level)\s*"
            r"(?:is|of|around|about|=|:)?\s*"
            r"(\d{2,3})\s*"
            r"(?:%)?",
            "%"
        ),

        (
            "weight",
            r"(?:weight)\s*"
            r"(?:is|of|around|about|=|:)?\s*"
            r"(\d+(?:\.\d+)?)\s*"
            r"(kg|kgs|kilograms|lb|lbs)?",
            ""
        )
    ]


    for name, pattern, default_unit in patterns:

        match = re.search(
            pattern,
            query,
            re.IGNORECASE
        )

        if not match:
            continue


        if name == "blood_pressure":

            value = (
                f"{match.group(1)}/"
                f"{match.group(2)}"
            )

            unit = default_unit

        else:

            value = match.group(1)

            unit = (
                match.group(2)
                if match.lastindex
                and match.lastindex >= 2
                and match.group(2)
                else default_unit
            )


            if name == "hemoglobin":

                unit = "g/dL"

            elif name == "blood_sugar":

                unit = "mg/dL"


        results.append({

            "memory_type":
                "measurement",

            "name":
                name,

            "value":
                value,

            "unit":
                unit,

            "duration":
                None
        })


    return results


# ============================================================
# MEASUREMENT RESPONSE
# ============================================================

def build_measurement_response(
    measurements
):

    lines = [
        "I have recorded the health "
        "measurement(s) you reported:"
    ]

    for item in measurements:

        name = (
            item["name"]
            .replace("_", " ")
            .title()
        )

        value = (
            item.get("value")
            or ""
        )

        unit = (
            item.get("unit")
            or ""
        )

        lines.append(
            f"- {name}: {value} {unit}".strip()
        )


    lines.append(
        "These values are stored as information "
        "you provided. They do not by themselves "
        "establish a diagnosis. If you want, you "
        "can also tell me your symptoms, duration, "
        "age, or other relevant health information."
    )

    return "\n".join(lines)


# ============================================================
# SAVE STRUCTURED MEMORIES
# ============================================================

def save_structured_memories(
    session_id,
    text,
    symptoms,
    duration
):

    for symptom in symptoms:

        save_medical_memory(

            session_id=session_id,

            memory_type="symptom",

            name=symptom,

            duration=duration,

            source_message=text
        )


    measurements = (
        extract_medical_measurements(
            text
        )
    )


    for item in measurements:

        save_medical_memory(

            session_id=session_id,

            memory_type=item[
                "memory_type"
            ],

            name=item[
                "name"
            ],

            value=item[
                "value"
            ],

            unit=item[
                "unit"
            ],

            duration=item[
                "duration"
            ],

            source_message=text
        )


    return measurements


# ============================================================
# RECENT MEMORY SUMMARY
# ============================================================

def get_recent_memory_summary(
    session_id,
    limit=20
):

    memories = get_medical_memory(
        session_id,
        limit=limit
    )

    symptoms = []
    measurements = []
    latest_measurements = {}


    for item in memories:

        if item[
            "memory_type"
        ] == "symptom":

            existing = [

                (
                    x["name"],
                    x.get("duration") or ""
                )

                for x in symptoms
            ]

            key = (

                item["name"],

                item.get(
                    "duration"
                ) or ""
            )

            if key not in existing:

                symptoms.append(
                    item
                )


        elif item[
            "memory_type"
        ] == "measurement":

            # get_medical_memory() is newest-first internally. Keep only
            # the latest value for each measurement so an old value such
            # as 10 g/dL cannot be shown together with a newer 11 g/dL.
            measurement_name = normalize_text(
                item.get("name", "")
            )

            if measurement_name not in latest_measurements:
                latest_measurements[measurement_name] = item


    measurements = list(
        latest_measurements.values()
    )

    return {

        "symptoms":
            symptoms,

        "measurements":
            measurements
    }


# ============================================================
# GENERAL MEMORY RECALL
# ============================================================

def build_memory_recall_response(
    session_id
):

    summary = (
        get_recent_memory_summary(
            session_id,
            limit=30
        )
    )

    symptom_items = (
        summary["symptoms"]
    )

    measurement_items = (
        summary["measurements"]
    )


    if (
        not symptom_items
        and
        not measurement_items
    ):

        return (
            "I do not have any structured health "
            "information saved for this session yet. "
            "You can tell me your symptoms, duration, "
            "or health measurements."
        )


    parts = [

        "Here is the health information "
        "you previously shared with me:"
    ]


    if symptom_items:

        symptom_lines = []

        for item in symptom_items:

            name = (
                item["name"]
                .replace("_", " ")
            )

            duration = (
                item.get("duration")
                or ""
            )

            if duration:

                symptom_lines.append(
                    f"- {name.title()} "
                    f"(you reported it for {duration})"
                )

            else:

                symptom_lines.append(
                    f"- {name.title()}"
                )


        parts.append(
            "Symptoms:\n"
            + "\n".join(
                symptom_lines
            )
        )


    if measurement_items:

        measurement_lines = []

        for item in measurement_items:

            name = (
                item["name"]
                .replace("_", " ")
                .title()
            )

            value = (
                item.get("value")
                or ""
            )

            unit = (
                item.get("unit")
                or ""
            )

            measurement_lines.append(
                f"- {name}: {value} {unit}".strip()
            )


        parts.append(
            "Reported measurements:\n"
            + "\n".join(
                measurement_lines
            )
        )


    parts.append(
        "This is a record of information you "
        "reported; it is not a diagnosis. If you "
        "are concerned about a result or symptom, "
        "consult a qualified healthcare professional."
    )


    return "\n\n".join(
        parts
    )


# ============================================================
# SPECIFIC MEMORY RECALL
# ============================================================

def build_specific_memory_response(
    session_id,
    text
):

    query = normalize_text(
        text
    )


    memories = get_medical_memory(
        session_id,
        limit=100
    )


    if not memories:

        return None


    requested_name = None


    # ========================================================
    # SYMPTOM DETECTION
    # ========================================================

    for symptom, aliases in (
        SYMPTOM_ALIASES.items()
    ):

        all_names = [
            symptom
        ] + aliases


        for alias in all_names:

            alias_normalized = (
                normalize_text(alias)
            )


            if (
                alias_normalized
                and
                alias_normalized in query
            ):

                requested_name = symptom

                break


        if requested_name:

            break


    # ========================================================
    # MEASUREMENT DETECTION
    # ========================================================

    measurement_aliases = {

        "hemoglobin": [
            "hemoglobin",
            "haemoglobin",
            "hb"
        ],

        "blood_pressure": [
            "blood pressure",
            "bp"
        ],

        "temperature": [
            "temperature",
            "temp"
        ],

        "blood_sugar": [
            "blood sugar",
            "sugar",
            "glucose"
        ],

        "heart_rate": [
            "heart rate",
            "pulse"
        ],

        "oxygen_saturation": [
            "oxygen saturation",
            "spo2",
            "oxygen level"
        ],

        "weight": [
            "weight"
        ]
    }


    if requested_name is None:

        for name, aliases in (
            measurement_aliases.items()
        ):

            for alias in aliases:

                alias_normalized = (
                    normalize_text(alias)
                )


                if (
                    alias_normalized
                    and
                    alias_normalized in query
                ):

                    requested_name = name

                    break


            if requested_name:

                break


    if requested_name is None:

        return None


    # ========================================================
    # FIND MATCHING MEMORY
    # ========================================================

    matching = []


    requested_normalized = (
        normalize_text(
            requested_name
        )
    )


    for item in memories:

        saved_name = normalize_text(
            item.get(
                "name",
                ""
            )
        )


        if (
            saved_name
            ==
            requested_normalized
        ):

            matching.append(
                item
            )


    if not matching:

        display_name = (
            requested_name
            .replace(
                "_",
                " "
            )
        )

        return (
            f"I don't have any saved information "
            f"about {display_name} from this session."
        )


    # ========================================================
    # BUILD RESPONSE
    # ========================================================

    display_name = (
        requested_name
        .replace(
            "_",
            " "
        )
        .title()
    )


    symptom_items = [

        item

        for item in matching

        if item.get(
            "memory_type"
        ) == "symptom"
    ]


    measurement_items = [

        item

        for item in matching

        if item.get(
            "memory_type"
        ) == "measurement"
    ]

    # For a specific measurement recall, show only the latest saved value.
    # This prevents an older value (for example 10) from being repeated
    # after the user has reported a newer value (for example 11).
    if measurement_items:
        # get_medical_memory() returns newest-first.
        # Keep only the latest value for this measurement.
        measurement_items = [
            measurement_items[0]
        ]


    lines = [

        f"Yes. You previously told me "
        f"about your {display_name.lower()}."
    ]


    # ========================================================
    # SYMPTOM MEMORY
    # ========================================================

    for item in symptom_items:

        duration = (
            item.get(
                "duration"
            )
            or ""
        )


        if duration:

            lines.append(

                f"- You reported "
                f"{display_name.lower()} "
                f"for {duration}."
            )

        else:

            lines.append(

                f"- You reported "
                f"{display_name.lower()}."
            )


    # ========================================================
    # MEASUREMENT MEMORY
    # ========================================================

    for item in measurement_items:

        value = (
            item.get(
                "value"
            )
            or ""
        )

        unit = (
            item.get(
                "unit"
            )
            or ""
        )


        lines.append(

            f"- Your reported "
            f"{display_name} was "
            f"{value} {unit}.".strip()
        )


    lines.append(

        "This is based only on information "
        "you previously reported. It is not "
        "a diagnosis."
    )


    return "\n".join(
        lines
    )


# ============================================================
# FOLLOW-UP RESPONSE
# ============================================================

def build_followup_response(
    session_id,
    text
):

    history = get_chat_history(
        session_id,
        limit=20
    )


    if not history:

        return None


    previous_symptoms = (
        get_previous_symptoms(
            session_id
        )
    )


    duration = extract_duration(
        text
    )


    if (
        duration
        and
        previous_symptoms
    ):

        names = [

            s.replace(
                "_",
                " "
            )

            for s in previous_symptoms
        ]


        if len(names) == 1:

            symptom_text = names[0]

        elif len(names) == 2:

            symptom_text = (
                f"{names[0]} and "
                f"{names[1]}"
            )

        else:

            symptom_text = (
                ", ".join(
                    names[:-1]
                )
                +
                f", and {names[-1]}"
            )


        return (
            f"You previously mentioned "
            f"{symptom_text}, and now you've "
            f"indicated that this has been present "
            f"for about {duration}. Persistent or "
            f"worsening symptoms should be assessed "
            f"by a qualified healthcare professional. "
            f"Please monitor your symptoms and seek "
            f"medical care if they become severe or "
            f"you develop concerning symptoms."
        )


    query = normalize_text(
        text
    )


    if query in {
        "yes",
        "yeah",
        "yep"
    }:

        return (
            "Thank you for confirming. Please provide "
            "any additional symptoms, how long they "
            "have been present, or any relevant "
            "information so I can provide general "
            "guidance."
        )


    if query in {
        "no",
        "nope"
    }:

        return (
            "Understood. Please continue monitoring "
            "your symptoms. If they become severe, "
            "worsen, or new concerning symptoms "
            "develop, seek medical attention."
        )


    return None


# ============================================================
# GENERAL FALLBACK
# ============================================================

def general_fallback(
    text
):

    query = normalize_text(
        text
    )


    if (
        "mobile phone" in query
        or
        (
            "mobile" in query
            and
            "phone" in query
        )
    ):

        return (
            "A mobile phone is a portable electronic "
            "device used for communication, internet "
            "access, applications, photography and "
            "other digital services."
        )


    if (
        "what is computer" in query
        or
        "what is a computer" in query
    ):

        return (
            "A computer is an electronic device that "
            "processes data and performs tasks according "
            "to instructions given by software."
        )


    if (
        "what is python" in query
    ):

        return (
            "Python is a high-level programming language "
            "commonly used for web development, data "
            "analysis, automation, artificial intelligence "
            "and machine learning."
        )


    return (
        "I can mainly help with medical and "
        "health-related questions. You can ask me "
        "about symptoms, diseases, general health "
        "information, medicines, or when medical "
        "attention may be needed."
    )


# ============================================================
# BAD ANSWER
# ============================================================

def is_bad_answer(
    answer
):

    text = normalize_text(
        answer
    )


    bad_patterns = [

        "sorry please ask another",
        "appropriate question",
        "ask another question",
        "unable to provide",
        "cannot provide any information",
        "i do not have the information",
        "i dont have the information"
    ]


    for pattern in bad_patterns:

        if pattern in text:

            return True


    return False


# ============================================================
# INTENT
# ============================================================

def determine_final_intent(
    text,
    model_intent,
    confidence
):
    query = normalize_text(text)
    words = set(query.split())

    symptoms = extract_symptoms(text)
    medical_topics = extract_medical_topics(text)

    # OPD / outpatient is deliberately not a supported intent.

    emergency_words = {
        "emergency",
        "unconscious",
        "critical",
        "trauma"
    }

    emergency_phrases = [
        "cannot breathe",
        "cant breathe",
        "severe chest pain",
        "heavy bleeding",
        "difficulty breathing",
        "blue lips",
        "collapsed",
        "medical emergency"
    ]

    if (
        words.intersection(emergency_words)
        or any(
            phrase in query
            for phrase in emergency_phrases
        )
    ):
        return "emergency"

    medicine_topics = {
        "paracetamol",
        "ibuprofen",
        "aspirin",
        "cetirizine",
        "antibiotic"
    }

    disease_topics = {
        "diabetes",
        "anemia",
        "asthma",
        "migraine",
        "dengue",
        "malaria",
        "typhoid",
        "hypertension",
        "hypotension",
        "dehydration"
    }

    if any(
        topic in medicine_topics
        for topic in medical_topics
    ):
        return "medicine_information"

    if any(
        topic in disease_topics
        for topic in medical_topics
    ):
        return "disease_information"

    if "hemoglobin" in medical_topics:
        return "symptom_information"

    assistance_patterns = [
        "i have",
        "i am having",
        "i feel",
        "i am feeling",
        "suffering from",
        "experiencing",
        "mala",
        "majha",
        "maza",
        "mujhe",
        "mere"
    ]

    if (
        symptoms
        and any(
            pattern in query
            for pattern in assistance_patterns
        )
    ):
        return "symptom_assistance"

    information_patterns = [
        "symptoms of",
        "symptoms for",
        "what are the symptoms",
        "signs of",
        "symptom of",
        "symptoms",
        "what is",
        "what are",
        "tell me about"
    ]

    if (
        symptoms
        and any(
            pattern in query
            for pattern in information_patterns
        )
    ):
        return "symptom_information"

    if symptoms:
        return "symptom_assistance"

    return str(model_intent)


# ============================================================
# MEDIQ RETRIEVAL
# ============================================================

def retrieve_mediq_answer(
    user_text,
    final_intent
):

    query = normalize_text(
        user_text
    )


    query_vector = (
        vectorizer.transform(
            [query]
        )
    )


    similarities = (
        cosine_similarity(
            query_vector,
            retrieval_matrix
        )[0]
    )


    candidates = []


    for index in range(
        len(dataset)
    ):

        answer = str(
            dataset.iloc[index][
                ANSWER_COLUMN
            ]
        )


        if is_bad_answer(
            answer
        ):

            continue


        score = float(
            similarities[index]
        )


        if CATEGORY_COLUMN is not None:

            category = normalize_text(
                dataset.iloc[index][
                    CATEGORY_COLUMN
                ]
            )


            if category == normalize_text(
                final_intent
            ):

                score += 0.20


        candidates.append(
            (
                index,
                score
            )
        )


    if not candidates:

        return None


    candidates.sort(
        key=lambda item: item[1],
        reverse=True
    )


    best_index = candidates[0][0]

    best_score = candidates[0][1]


    answer = str(
        dataset.iloc[
            best_index
        ][
            ANSWER_COLUMN
        ]
    )


    return {

        "answer":
            answer,

        "similarity":
            round(
                min(
                    best_score,
                    1.0
                ),
                4
            ),

        "source":
            "mediq"
    }


# ============================================================
# HOME
# ============================================================

@app.get("/")
def home():

    return jsonify({

        "status":
            "success",

        "service":
            "MEDASSIST AI Medical NLP API",

        "message":
            "API is running",

        "model_loaded":
            True,

        "dataset_loaded":
            True,

        "medical_knowledge_loaded":
            True,

        "chat_memory":
            "SQLite",

        "mediq_records":
            int(
                len(dataset)
            ),

        "medical_knowledge_records":
            int(
                len(medical_knowledge)
            )
    })


# ============================================================
# HEALTH
# ============================================================

@app.get("/health")
def health():

    return jsonify({

        "status":
            "healthy",

        "model_loaded":
            True,

        "dataset_loaded":
            True,

        "medical_knowledge_loaded":
            True,

        "chat_memory":
            "SQLite"
    })


# ============================================================
# CHAT HISTORY
# ============================================================

@app.get(
    "/history/<session_id>"
)
def history(
    session_id
):

    return jsonify({

        "status":
            "success",

        "session_id":
            session_id,

        "history":
            get_chat_history(
                session_id,
                limit=50
            )
    })


# ============================================================
# DELETE CHAT HISTORY
# ============================================================

@app.delete(
    "/history/<session_id>"
)
def delete_history(
    session_id
):

    clear_chat_history(
        session_id
    )


    return jsonify({

        "status":
            "success",

        "message":
            "Chat history cleared",

        "session_id":
            session_id
    })


# ============================================================
# MEDICAL MEMORY
# ============================================================

@app.get(
    "/memory/<session_id>"
)
def memory(
    session_id
):

    return jsonify({

        "status":
            "success",

        "session_id":
            session_id,

        "memory":
            get_medical_memory(
                session_id,
                limit=100
            )
    })


# ============================================================
# DELETE MEDICAL MEMORY
# ============================================================

@app.delete(
    "/memory/<session_id>"
)
def delete_memory(
    session_id
):

    clear_medical_memory(
        session_id
    )


    return jsonify({

        "status":
            "success",

        "message":
            "Structured medical memory cleared",

        "session_id":
            session_id
    })


# ============================================================
# DIRECT MEASUREMENT MEMORY QUERY
# ============================================================

def build_latest_measurement_response(session_id, text):

    query = normalize_text(text)

    measurement_aliases = {
        "hemoglobin": ["hemoglobin", "haemoglobin", "hb"],
        "blood_pressure": ["blood pressure", "bp"],
        "temperature": ["temperature", "temp"],
        "blood_sugar": ["blood sugar", "sugar", "glucose"],
        "heart_rate": ["heart rate", "pulse"],
        "oxygen_saturation": ["oxygen saturation", "spo2", "oxygen level"],
        "weight": ["weight"]
    }

    requested_name = None

    for name, aliases in measurement_aliases.items():
        for alias in aliases:
            alias_normalized = normalize_text(alias)
            if alias_normalized and alias_normalized in query:
                requested_name = name
                break
        if requested_name:
            break

    if requested_name is None:
        return None

    # Only treat the query as a direct memory question when it asks for
    # the user's saved value, not when it is reporting a new measurement.
    query_patterns = [
        "what is my",
        "what was my",
        "what is the latest",
        "what was the latest",
        "tell me my",
        "show me my",
        "do you know my",
        "my latest",
        "my current"
    ]

    if not any(pattern in query for pattern in query_patterns):
        return None

    memories = get_medical_memory(session_id, limit=100)
    requested_normalized = normalize_text(requested_name)

    matching = [
        item for item in memories
        if item.get("memory_type") == "measurement"
        and normalize_text(item.get("name", "")) == requested_normalized
    ]

    if not matching:
        display_name = requested_name.replace("_", " ").title()
        return (
            f"I don't have any saved information about {display_name} "
            "from this session."
        )

    # get_medical_memory returns newest-first, so the first match is latest.
    latest = matching[0]
    display_name = requested_name.replace("_", " ").title()
    value = str(latest.get("value") or "").strip()
    unit = str(latest.get("unit") or "").strip()

    return (
        f"Your latest reported {display_name} is {value} {unit}."
        "\n\nThis is based only on information you previously reported "
        "and is not a diagnosis."
    )


# ============================================================
# PREDICT
# ============================================================

@app.post(
    "/predict"
)
def predict():

    data = request.get_json(
        silent=True
    )


    if not data:

        return jsonify({

            "status":
                "error",

            "message":
                "Request body is required"

        }), 400


    text = data.get(
        "text"
    )


    if (
        not text
        or
        not isinstance(
            text,
            str
        )
    ):

        return jsonify({

            "status":
                "error",

            "message":
                "text field is required"

        }), 400


    text = text.strip()

    # ========================================================
    # LANGUAGE + INTERNAL MEDICAL ANALYSIS TEXT
    # ========================================================
    # Keep the user's original text for display/storage, but use an
    # English-normalized representation for the existing ML/NLP pipeline.
    detected_language = detect_language(text)
    analysis_text = (
        translate_input_to_english(text)
        if detected_language in {"mr", "hi"}
        else text
    )


    if not text:

        return jsonify({

            "status":
                "error",

            "message":
                "text cannot be empty"

        }), 400


    # ========================================================
    # SESSION ID
    # ========================================================

    session_id = data.get(
        "session_id",
        "default_user"
    )


    session_id = str(
        session_id
    ).strip()


    if not session_id:

        session_id = "default_user"


    try:

        # ====================================================
        # ML MODEL
        # ====================================================

        model_prediction = model.predict(
            [analysis_text]
        )[0]


        model_prediction = str(
            model_prediction
        )


        # ====================================================
        # CONFIDENCE
        # ====================================================

        confidence = 0.0


        if hasattr(
            model,
            "predict_proba"
        ):

            try:

                probabilities = (
                    model.predict_proba(
                        [analysis_text]
                    )[0]
                )


                confidence = float(
                    max(
                        probabilities
                    )
                )


            except Exception:

                confidence = 0.0


        # ====================================================
        # CURRENT DATA
        # ====================================================

        symptoms = extract_symptoms(
            analysis_text
        )

        duration = extract_duration(
            analysis_text
        )

        measurements = (
            extract_medical_measurements(
                analysis_text
            )
        )


        # ====================================================
        # PREVIOUS SYMPTOMS
        # ====================================================

        previous_symptoms = (
            get_previous_symptoms(
                session_id
            )
        )


        # ====================================================
        # MEDICAL QUERY
        # ====================================================

        medical_query = (
            is_medical_query(
                analysis_text
            )
        )


        # ====================================================
        # EMERGENCY-FIRST SAFETY GATE
        # ====================================================
        # Red-flag symptoms must be handled before the
        # conversational question flow. This prevents the
        # assistant from asking routine questions such as
        # duration before giving urgent-care guidance.
        emergency_text = normalize_text(analysis_text)

        # normalize_text() can collapse phrases such as
        # "chest pain" -> "chestpain". Keep both natural and
        # normalized forms so red-flag detection is robust.
        emergency_patterns = [
            "severe chest pain",
            "severe chestpain",
            "crushing chest pain",
            "crushing chestpain",
            "pressure in my chest",
            "pressure in my chest",
            "chest pain and difficulty breathing",
            "chestpain and difficulty breathing",
            "chest pain with difficulty breathing",
            "chestpain with difficulty breathing",
            "difficulty breathing",
            "difficultybreathing",
            "cannot breathe",
            "cannotbreathe",
            "can't breathe",
            "cant breathe",
            "shortness of breath",
            "shortnessofbreath",
            "fainting",
            "passed out",
            "passedout",
            "loss of consciousness",
            "lossofconsciousness",
            "severe bleeding",
            "severebleeding",
            "heavy bleeding",
            "heavybleeding",
            "coughing blood",
            "coughingblood",
            "vomiting blood",
            "vomitingblood",
            "blood in vomit",
            "bloodinvomit",
            "severe confusion",
            "severeconfusion",
            "sudden confusion",
            "suddenconfusion",
            "seizure",
            "convulsion",
            "blue lips",
            "bluelips",
            "stroke symptoms",
            "strokesymptoms",
            "face drooping",
            "facedrooping",
            "difficulty speaking",
            "difficultyspeaking",
            "sudden weakness on one side",
            "suddenweaknessononeside"
        ]

        emergency_detected = any(
            pattern in emergency_text
            for pattern in emergency_patterns
        )

        if emergency_detected:
            emergency_answer = (
                "Your message includes a potentially serious emergency "
                "symptom. Please seek urgent medical assessment now. "
                "If the symptom is severe, sudden, worsening, or you "
                "feel unsafe, contact your local emergency service or "
                "go to the nearest emergency department. Do not rely "
                "on this chatbot to determine the cause."
            )
            emergency_answer = translate_conversation_response(
                emergency_answer, detected_language
            )

            save_structured_memories(
                session_id=session_id,
                text=text,
                symptoms=symptoms,
                duration=duration
            )

            save_chat_message(
                session_id,
                "user",
                text,
                symptoms
            )

            save_chat_message(
                session_id,
                "assistant",
                emergency_answer,
                symptoms
            )

            return jsonify({
                "status": "success",
                "session_id": session_id,
                "question": text,
                "intent": "emergency",
                "confidence": round(confidence, 4),
                "symptoms": symptoms,
                "previous_symptoms": previous_symptoms,
                "duration": duration,
                "measurements": measurements,
                "medical_query": True,
                "answer": emergency_answer,
                "answer_similarity": 1.0,
                "source": "emergency_safety_gate",
                "language": detected_language
            })


        # ========================================================
        # NORMALIZED QUERY
        # ========================================================

        normalized_query = (
            normalize_text(
                analysis_text
            )
        )


        # ====================================================
        # SPECIFIC MEMORY RECALL
        # ====================================================
        #
        # IMPORTANT:
        # This runs BEFORE generic memory and MEDIQ.
        #
        # Examples:
        #
        # Do you remember my fever?
        # What did I tell you about my fever?
        # Do you remember my hemoglobin?
        # What was my BP?
        #
        # ====================================================

        memory_question_words = [

            "remember",
            "previously",
            "earlier",
            "before",
            "told",
            "said",
            "shared",
            "reported",
            "gave",
            "mentioned"
        ]


        has_memory_language = any(

            word in normalized_query.split()

            for word in memory_question_words
        )


        specific_memory_answer = None


        # ====================================================
        # DIRECT MEASUREMENT RECALL
        # ====================================================
        # Handles questions such as:
        #   "what is my hemoglobin?"
        #   "what was my BP?"
        #   "what is my latest temperature?"
        # This must run before the general fallback/MEDIQ retrieval.
        # A new measurement such as "my hemoglobin is 12 g/dL" is not
        # treated as recall because measurements is non-empty.
        if not measurements:
            direct_measurement_answer = (
                build_latest_measurement_response(
                    session_id,
                    text
                )
            )

            if direct_measurement_answer:
                save_chat_message(
                    session_id,
                    "user",
                    text,
                    []
                )

                save_chat_message(
                    session_id,
                    "assistant",
                    direct_measurement_answer,
                    previous_symptoms
                )

                return jsonify({
                    "status": "success",
                    "session_id": session_id,
                    "question": text,
                    "intent": "measurement_recall",
                    "confidence": round(confidence, 4),
                    "symptoms": previous_symptoms,
                    "medical_query": True,
                    "answer": direct_measurement_answer,
                    "answer_similarity": 1.0,
                    "source": "structured_memory"
                })


        # A NEW measurement must always be treated as a current report.
        # Do not let memory-recall logic answer a query such as
        # "my hemoglobin is 11 g/dL" using an older saved value.
        if has_memory_language and not measurements:

            specific_memory_answer = (
                build_specific_memory_response(
                    session_id,
                    text
                )
            )


        if specific_memory_answer:

            save_chat_message(
                session_id,
                "user",
                text,
                []
            )


            save_chat_message(
                session_id,
                "assistant",
                specific_memory_answer,
                previous_symptoms
            )


            return jsonify({

                "status":
                    "success",

                "session_id":
                    session_id,

                "question":
                    text,

                "intent":
                    "memory_recall",

                "confidence":
                    round(
                        confidence,
                        4
                    ),

                "symptoms":
                    previous_symptoms,

                "medical_query":
                    True,

                "answer":
                    specific_memory_answer,

                "answer_similarity":
                    1.0,

                "source":
                    "structured_memory"
            })


        # ====================================================
        # GENERAL MEMORY RECALL
        # ====================================================

        memory_recall_patterns = [

            "what did i tell you before",
            "what did i tell you earlier",
            "what did i say before",
            "what did i say earlier",

            "what did i tell you about my health",
            "what did i tell you about my health before",
            "what did i tell you about my health earlier",

            "what did i say about my health",
            "what did i say about my health before",

            "what health information did i give you",
            "what health information did i tell you",
            "what health information did i share",

            "what did i share about my health",
            "what did i share with you before",
            "what did i share with you earlier",

            "tell me my health history",
            "show me my health history",

            "my previous symptoms",
            "my past symptoms",

            "my previous health",
            "my past health",

            "my health history",

            "previous health information",
            "past health information",

            "do you remember my symptoms",
            "do you remember my health",
            "do you remember my health history",

            "do you remember what i told you",
            "do you remember what i said",

            "what do you remember about my health",
            "what do you remember about me"
        ]


        if any(

            pattern in normalized_query

            for pattern
            in memory_recall_patterns

        ):

            memory_answer = (
                build_memory_recall_response(
                    session_id
                )
            )


            save_chat_message(
                session_id,
                "user",
                text,
                []
            )


            save_chat_message(
                session_id,
                "assistant",
                memory_answer,
                previous_symptoms
            )


            return jsonify({

                "status":
                    "success",

                "session_id":
                    session_id,

                "question":
                    text,

                "intent":
                    "memory_recall",

                "confidence":
                    round(
                        confidence,
                        4
                    ),

                "symptoms":
                    previous_symptoms,

                "medical_query":
                    True,

                "answer":
                    memory_answer,

                "answer_similarity":
                    1.0,

                "source":
                    "structured_memory"
            })


        # ====================================================
        # CONVERSATIONAL MEDICAL ENGINE
        # ====================================================
        # Run this BEFORE the legacy generic follow-up handler.
        # This is important because replies such as "2 days" are
        # generic follow-ups, but inside an active conversation they
        # must advance the conversation state instead of returning
        # the older generic follow-up message.

        final_intent = determine_final_intent(
            text,
            model_prediction,
            confidence
        )

        conversation_response = handle_conversation_turn(
            session_id=session_id,
            text=text,
            symptoms=symptoms,
            duration=duration,
            confidence=confidence,
            final_intent=final_intent
        )

        if conversation_response is not None:
            return conversation_response


        # ====================================================
        # FOLLOW-UP
        # ====================================================

        if (
            is_followup_query(text)
            and
            not symptoms
        ):

            followup_answer = (
                build_followup_response(
                    session_id,
                    text
                )
            )


            if followup_answer:

                save_chat_message(
                    session_id,
                    "user",
                    text,
                    []
                )


                save_chat_message(
                    session_id,
                    "assistant",
                    followup_answer,
                    previous_symptoms
                )


                return jsonify({

                    "status":
                        "success",

                    "session_id":
                        session_id,

                    "question":
                        text,

                    "intent":
                        "follow_up",

                    "confidence":
                        round(
                            confidence,
                            4
                        ),

                    "symptoms":
                        previous_symptoms,

                    "medical_query":
                        True,

                    "answer":
                        followup_answer,

                    "answer_similarity":
                        1.0,

                    "source":
                        "conversation_memory"
                })


        # ====================================================
        # GENERAL QUERY
        # ====================================================

        if not medical_query:

            answer = general_fallback(
                analysis_text
            )
            answer = translate_general_response(
                answer, detected_language
            )


            save_chat_message(
                session_id,
                "user",
                text,
                []
            )


            save_chat_message(
                session_id,
                "assistant",
                answer,
                []
            )


            return jsonify({

                "status":
                    "success",

                "session_id":
                    session_id,

                "question":
                    text,

                "intent":
                    "general",

                "confidence":
                    round(
                        confidence,
                        4
                    ),

                "symptoms":
                    [],

                "medical_query":
                    False,

                "answer":
                    answer,

                "answer_similarity":
                    1.0,

                "source":
                    "general_fallback",
                "language": detected_language
            })


        # ====================================================
        # MEASUREMENT ONLY
        # ====================================================

        if measurements and not symptoms:

            measurement_answer = (
                build_measurement_response(
                    measurements
                )
            )


            save_structured_memories(
                session_id=session_id,
                text=text,
                symptoms=[],
                duration=duration
            )


            save_chat_message(
                session_id,
                "user",
                text,
                []
            )


            save_chat_message(
                session_id,
                "assistant",
                measurement_answer,
                []
            )


            return jsonify({

                "status":
                    "success",

                "session_id":
                    session_id,

                "question":
                    text,

                "intent":
                    "measurement_report",

                "confidence":
                    round(
                        confidence,
                        4
                    ),

                "symptoms":
                    [],

                "previous_symptoms":
                    previous_symptoms,

                "duration":
                    duration,

                "measurements":
                    measurements,

                "medical_query":
                    True,

                "answer":
                    measurement_answer,

                "answer_similarity":
                    1.0,

                "source":
                    "structured_memory"
            })


        # ====================================================
        # DIRECT MEDICAL TOPIC RESPONSE
        # ====================================================

        medical_topics = extract_medical_topics(analysis_text)

        if medical_topics:
            topic_question_type = get_question_type(analysis_text)

            topic_answer = get_topic_knowledge_response(
                medical_topics,
                topic_question_type
            )

            if topic_answer:
                save_chat_message(
                    session_id,
                    "user",
                    text,
                    symptoms
                )

                save_structured_memories(
                    session_id=session_id,
                    text=text,
                    symptoms=symptoms,
                    duration=duration
                )

                save_chat_message(
                    session_id,
                    "assistant",
                    topic_answer,
                    symptoms
                )

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
                    "answer": topic_answer,
                    "answer_similarity": 1.0,
                    "source": "medical_knowledge"
                })

        # ====================================================
        # MEDICAL RESPONSE
        # ====================================================

        if symptoms:

            question_type = (
                get_question_type(
                    analysis_text
                )
            )


            medical_answer = (
                build_multi_symptom_response(
                    symptoms,
                    question_type
                )
            )


            if medical_answer:

                save_chat_message(
                    session_id,
                    "user",
                    text,
                    symptoms
                )


                save_structured_memories(
                    session_id=session_id,
                    text=text,
                    symptoms=symptoms,
                    duration=duration
                )


                save_chat_message(
                    session_id,
                    "assistant",
                    medical_answer,
                    symptoms
                )


                return jsonify({

                    "status":
                        "success",

                    "session_id":
                        session_id,

                    "question":
                        text,

                    "intent":
                        final_intent,

                    "confidence":
                        round(
                            confidence,
                            4
                        ),

                    "symptoms":
                        symptoms,

                    "previous_symptoms":
                        previous_symptoms,

                    "duration":
                        duration,

                    "measurements":
                        measurements,

                    "medical_query":
                        True,

                    "answer":
                        translate_medical_response(
                            medical_answer,
                            detected_language
                        ),

                    "answer_similarity":
                        1.0,

                    "source":
                        "medical_knowledge"
                })


        # ====================================================
        # MEDIQ FALLBACK
        # ====================================================

        mediq_result = (
            retrieve_mediq_answer(
                analysis_text,
                final_intent
            )
        )


        if mediq_result is not None:

            answer = translate_medical_response(
                mediq_result[
                    "answer"
                ],
                detected_language
            )


            save_structured_memories(
                session_id=session_id,
                text=text,
                symptoms=symptoms,
                duration=duration
            )


            save_chat_message(
                session_id,
                "user",
                text,
                symptoms
            )


            save_chat_message(
                session_id,
                "assistant",
                answer,
                symptoms
            )


            return jsonify({

                "status":
                    "success",

                "session_id":
                    session_id,

                "question":
                    text,

                "intent":
                    final_intent,

                "confidence":
                    round(
                        confidence,
                        4
                    ),

                "symptoms":
                    symptoms,

                "previous_symptoms":
                    previous_symptoms,

                "duration":
                    duration,

                "measurements":
                    measurements,

                "medical_query":
                    True,

                "answer":
                    answer,

                "answer_similarity":
                    mediq_result[
                        "similarity"
                    ],

                "source":
                    "mediq"
            })


        # ====================================================
        # SAFE FALLBACK
        # ====================================================

        answer = (
            "I could not find a suitable answer "
            "in my current medical knowledge base. "
            "Please consult a qualified healthcare "
            "professional for personalized medical advice."
        )


        save_structured_memories(
            session_id=session_id,
            text=text,
            symptoms=symptoms,
            duration=duration
        )


        save_chat_message(
            session_id,
            "user",
            text,
            symptoms
        )


        save_chat_message(
            session_id,
            "assistant",
            answer,
            symptoms
        )


        return jsonify({

            "status":
                "success",

            "session_id":
                session_id,

            "question":
                text,

            "intent":
                final_intent,

            "confidence":
                round(
                    confidence,
                    4
                ),

            "symptoms":
                symptoms,

            "previous_symptoms":
                previous_symptoms,

            "medical_query":
                True,

            "answer":
                answer,

            "answer_similarity":
                0.0,

            "source":
                "safe_fallback"
        })


    except Exception as e:

        return jsonify({

            "status":
                "error",

            "message":
                "Prediction failed",

            "error":
                str(e)

        }), 500


# ============================================================
# START SERVER
# ============================================================

if __name__ == "__main__":

    print("----------------------------------------")
    print("MEDASSIST AI API")
    print("----------------------------------------")

    print(
        "Model        : Loaded"
    )

    print(
        "MEDIQ        : Loaded"
    )

    print(
        "Medical KB   : Loaded"
    )

    print(
        "Chat Memory  : SQLite"
    )

    print(
        "Chat DB      :",
        CHAT_DB_FILE
    )

    print(
        "MEDIQ Records:",
        len(dataset)
    )

    print(
        "Medical KB Records:",
        len(medical_knowledge)
    )

    print(
        "API Port     : 5000"
    )

    print("----------------------------------------")


    app.run(
        host="0.0.0.0",
        port=5000,
        debug=False
    )


