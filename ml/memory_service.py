"""
MedAssist AI - Long-Term Memory Service
User-isolated, cross-conversation memory store in SQLite with:
- Strict user-isolation (user_id partitioning)
- Provenance tracking (source, status: user-reported, clinician-confirmed, AI-inferred, unverified)
- Non-sensitive extraction and safety validation (rejects credentials, diagnoses, prescriptions, allergies, vitals)
- Contradiction handling and stale-data updates
- Explicit user opt-in consent enforcement
"""

import logging
import re
import sqlite3
import uuid
from datetime import datetime
from pathlib import Path
from typing import Optional, Dict, List, Tuple

logger = logging.getLogger("medassist.memory_service")

BASE_DIR = Path(__file__).resolve().parent
DB_FILE = BASE_DIR / "data" / "chat_history.db"

# Safety regexes to prevent saving sensitive clinical details as permanent long-term memory
SENSITIVE_MEDICAL_DIAGNOSES = [
    r"(?i)\b(diagnos|suffering from|chronic condition|stage [1-4]|malignant|benign|coronary|artery disease)\b",
    r"(?i)\b(cancer|tumor|diabetes|hypertension|hiv|aids|covid|tuberculosis|depression|bipolar|schizophrenia)\b",
    r"(?i)\b(asthma|arthritis|dementia|parkinson|stroke|heart attack|angina|cirrhosis|hepatitis)\b",
    r"(?i)\b(infection|pneumonia|bronchitis|ulcer|epilepsy|seizure)\b"
]

SENSITIVE_MEDICATIONS = [
    r"(?i)\b(prescription|prescribed|medication|medicine|tablet|capsule|pill|dosage|dose|mg|ml|mcg)\b",
    r"(?i)\b(paracetamol|metformin|atorvastatin|ibuprofen|amoxicillin|aspirin|antibiotic|insulin|inhaler)\b",
    r"(?i)\b(antidepressant|steroid|chemotherapy|sedative|injection)\b"
]

SENSITIVE_ALLERGIES = [
    r"(?i)\b(allergy|allergic to|anaphylaxis|adverse reaction)\b"
]

SENSITIVE_VITALS = [
    r"(?i)\b(blood pressure|bp:|heart rate|pulse rate|spo2|oxygen saturation)\b",
    r"(?i)\b(blood sugar|glucose level|hba1c|cholesterol|creatinine|biopsy|lab report)\b"
]

CREDENTIAL_PATTERNS = [
    r"(?i)(?:\b|_)(password|passwd|token|access_token|refresh_token|bearer|apikey|api[ _]?key|anon_key|secret|pin|jwt)(?:\b|_)",
    r"(?i)\b(credit card|cvv|ssn|social security)\b",
    r"(?i)eyj[a-z0-9_-]{10,}"
]


def _connect():
    conn = sqlite3.connect(DB_FILE, timeout=10)
    try:
        conn.execute("PRAGMA journal_mode=WAL")
    except sqlite3.Error:
        pass
    return conn


def init_memory_schema(db_path=None):
    """
    Creates additive tables for user-isolated long-term memory and consent,
    and ensures conversation history supports user_id.
    """
    target_db = Path(db_path) if db_path else DB_FILE
    target_db.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(target_db, timeout=10)
    try:
        conn.execute("PRAGMA journal_mode=WAL")
    except sqlite3.Error:
        pass

    # 1. Ensure chat_history exists and has user_id, conversation_id, is_deleted
    conn.execute("""
        CREATE TABLE IF NOT EXISTS chat_history (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            session_id TEXT NOT NULL,
            role TEXT NOT NULL,
            message TEXT NOT NULL,
            symptoms TEXT,
            created_at TEXT NOT NULL,
            user_id TEXT DEFAULT 'guest',
            conversation_id TEXT DEFAULT '',
            is_deleted INTEGER DEFAULT 0
        )
    """)

    cursor = conn.cursor()
    cursor.execute("PRAGMA table_info(chat_history)")
    existing_cols = {row[1] for row in cursor.fetchall()}

    if "user_id" not in existing_cols:
        try:
            conn.execute("ALTER TABLE chat_history ADD COLUMN user_id TEXT DEFAULT 'guest'")
        except Exception:
            pass

    if "conversation_id" not in existing_cols:
        try:
            conn.execute("ALTER TABLE chat_history ADD COLUMN conversation_id TEXT DEFAULT ''")
        except Exception:
            pass

    if "is_deleted" not in existing_cols:
        try:
            conn.execute("ALTER TABLE chat_history ADD COLUMN is_deleted INTEGER DEFAULT 0")
        except Exception:
            pass

    # Index for fast user/conversation lookups
    conn.execute("CREATE INDEX IF NOT EXISTS idx_chat_user_conv ON chat_history (user_id, session_id)")

    # 2. Long-term memory store
    conn.execute("""
        CREATE TABLE IF NOT EXISTS user_long_term_memory (
            id TEXT PRIMARY KEY,
            user_id TEXT NOT NULL,
            memory_key TEXT NOT NULL,
            content TEXT NOT NULL,
            category TEXT NOT NULL,
            source TEXT NOT NULL DEFAULT 'user_chat',
            status TEXT NOT NULL DEFAULT 'user-reported',
            created_at TEXT NOT NULL,
            updated_at TEXT NOT NULL,
            is_deleted INTEGER DEFAULT 0,
            UNIQUE(user_id, memory_key)
        )
    """)
    conn.execute("CREATE INDEX IF NOT EXISTS idx_user_memory_user ON user_long_term_memory (user_id, is_deleted)")
    conn.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_user_memory_unique ON user_long_term_memory (user_id, memory_key)")

    # 3. User consent store
    conn.execute("""
        CREATE TABLE IF NOT EXISTS user_memory_consent (
            user_id TEXT PRIMARY KEY,
            consent_enabled INTEGER NOT NULL DEFAULT 0,
            updated_at TEXT NOT NULL
        )
    """)

    # 4. Cross-chat persistent user medical entities store
    conn.execute("""
        CREATE TABLE IF NOT EXISTS user_medical_entities (
            id TEXT PRIMARY KEY,
            user_id TEXT NOT NULL,
            subject TEXT NOT NULL,
            subject_relation TEXT NOT NULL DEFAULT 'self',
            concern TEXT NOT NULL,
            raw_condition TEXT NOT NULL,
            duration TEXT,
            severity TEXT,
            status TEXT NOT NULL DEFAULT 'active',
            source_session_id TEXT,
            source_message TEXT,
            created_at TEXT NOT NULL,
            updated_at TEXT NOT NULL,
            is_deleted INTEGER DEFAULT 0
        )
    """)
    conn.execute("CREATE INDEX IF NOT EXISTS idx_user_entities_user ON user_medical_entities (user_id, is_deleted)")
    conn.execute("CREATE INDEX IF NOT EXISTS idx_user_entities_subject ON user_medical_entities (user_id, subject, is_deleted)")

    # Ensure additive columns for user_medical_entities
    cursor = conn.cursor()
    cursor.execute("PRAGMA table_info(user_medical_entities)")
    ent_cols = {row[1] for row in cursor.fetchall()}
    for col, col_type in [("medication", "TEXT"), ("test_findings", "TEXT"), ("notes", "TEXT")]:
        if col not in ent_cols:
            try:
                conn.execute(f"ALTER TABLE user_medical_entities ADD COLUMN {col} {col_type}")
            except Exception:
                pass

    # 5. Persistent multimodal medical documents store
    conn.execute("""
        CREATE TABLE IF NOT EXISTS user_medical_documents (
            id TEXT PRIMARY KEY,
            user_id TEXT NOT NULL,
            session_id TEXT,
            document_type TEXT NOT NULL,
            file_name TEXT,
            extracted_text TEXT,
            summary TEXT,
            extracted_data_json TEXT,
            clinical_flags_json TEXT,
            created_at TEXT NOT NULL,
            updated_at TEXT NOT NULL,
            is_deleted INTEGER DEFAULT 0
        )
    """)
    conn.execute("CREATE INDEX IF NOT EXISTS idx_user_docs_user ON user_medical_documents (user_id, is_deleted)")
    conn.execute("CREATE INDEX IF NOT EXISTS idx_user_docs_session ON user_medical_documents (session_id, is_deleted)")

    # 6. Ensure medical_memory table supports user_id
    try:
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
                created_at TEXT NOT NULL,
                user_id TEXT DEFAULT ''
            )
        """)
        cursor = conn.cursor()
        cursor.execute("PRAGMA table_info(medical_memory)")
        med_cols = {row[1] for row in cursor.fetchall()}
        if "user_id" not in med_cols:
            conn.execute("ALTER TABLE medical_memory ADD COLUMN user_id TEXT DEFAULT ''")
    except Exception:
        pass

    conn.commit()
    conn.close()


def is_guest_user(user_id: Optional[str]) -> bool:
    if not user_id:
        return True
    clean = str(user_id).strip().lower()
    return clean in {"", "guest", "null", "none"} or clean.startswith("guest") or clean.startswith("account_guest") or clean.startswith("anon_")


def is_safe_for_memory(text: str) -> bool:
    """
    Returns True ONLY if the statement contains no credentials, clinical diagnoses,
    prescriptions, allergies, or vitals.
    """
    if not text or not text.strip():
        return False
    clean = text.strip()

    for pattern_list in [CREDENTIAL_PATTERNS, SENSITIVE_MEDICAL_DIAGNOSES, SENSITIVE_MEDICATIONS, SENSITIVE_ALLERGIES, SENSITIVE_VITALS]:
        for pat in pattern_list:
            if re.search(pat, clean):
                return False

    return True


def get_user_consent(user_id: Optional[str]) -> bool:
    """
    Returns True if user has explicitly opted into long-term memory.
    Defaults strictly to False. Guests cannot enable consent.
    """
    if is_guest_user(user_id):
        return False

    conn = _connect()
    row = conn.execute(
        "SELECT consent_enabled FROM user_memory_consent WHERE user_id = ?",
        (str(user_id).strip(),)
    ).fetchone()
    conn.close()

    if not row:
        return False
    return bool(row[0])


def set_user_consent(user_id: Optional[str], enabled: bool) -> bool:
    """
    Sets the explicit opt-in memory consent for an authenticated user.
    """
    if is_guest_user(user_id):
        return False

    clean_id = str(user_id).strip()
    conn = _connect()
    now = datetime.utcnow().isoformat()

    conn.execute("""
        INSERT INTO user_memory_consent (user_id, consent_enabled, updated_at)
        VALUES (?, ?, ?)
        ON CONFLICT(user_id) DO UPDATE SET
            consent_enabled = excluded.consent_enabled,
            updated_at = excluded.updated_at
    """, (clean_id, 1 if enabled else 0, now))

    conn.commit()
    conn.close()
    return True


def get_user_memories(user_id: Optional[str], is_temporary: bool = False) -> List[Dict]:
    """
    Retrieves all non-deleted long-term memories for an authenticated user.
    Returns empty list if guest, temporary chat, or consent is disabled.
    """
    if is_temporary or is_guest_user(user_id) or not get_user_consent(user_id):
        return []

    clean_id = str(user_id).strip()
    conn = _connect()
    conn.row_factory = sqlite3.Row

    rows = conn.execute("""
        SELECT id, user_id, memory_key, content, category, source, status, created_at, updated_at
        FROM user_long_term_memory
        WHERE user_id = ? AND is_deleted = 0
        ORDER BY updated_at DESC
    """, (clean_id,)).fetchall()
    conn.close()

    return [dict(r) for r in rows]


def save_user_memory(
    user_id: Optional[str],
    memory_key: str,
    content: str,
    category: str,
    source: str = "user_chat",
    status: str = "user-reported",
    is_temporary: bool = False
) -> bool:
    """
    Saves or updates a memory item for an authenticated user.
    Strictly checks consent, non-temporary mode, and safety filter.
    """
    if is_temporary or is_guest_user(user_id) or not get_user_consent(user_id):
        return False

    if not is_safe_for_memory(content):
        logger.warning("Memory rejected by safety filter: %s", memory_key)
        return False

    clean_id = str(user_id).strip()
    clean_key = memory_key.strip().lower()
    now = datetime.utcnow().isoformat()
    new_id = str(uuid.uuid4())

    conn = _connect()
    # Check if record already exists for this (user_id, memory_key)
    cur = conn.cursor()
    cur.execute("SELECT id FROM user_long_term_memory WHERE user_id = ? AND memory_key = ?", (clean_id, clean_key))
    existing = cur.fetchone()

    if existing:
        conn.execute("""
            UPDATE user_long_term_memory
            SET content = ?,
                category = ?,
                source = ?,
                status = ?,
                updated_at = ?,
                is_deleted = 0
            WHERE user_id = ? AND memory_key = ?
        """, (content.strip(), category.upper(), source, status, now, clean_id, clean_key))
    else:
        conn.execute("""
            INSERT INTO user_long_term_memory (
                id, user_id, memory_key, content, category, source, status, created_at, updated_at, is_deleted
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
        """, (new_id, clean_id, clean_key, content.strip(), category.upper(), source, status, now, now))

    conn.commit()
    conn.close()
    return True


def update_user_memory(user_id: Optional[str], memory_id: str, new_content: str) -> bool:
    """
    Updates/corrects an existing memory item for an authenticated user.
    """
    if is_guest_user(user_id):
        return False
    if not is_safe_for_memory(new_content):
        return False

    clean_id = str(user_id).strip()
    now = datetime.utcnow().isoformat()
    conn = _connect()

    cursor = conn.execute("""
        UPDATE user_long_term_memory
        SET content = ?, updated_at = ?
        WHERE id = ? AND user_id = ? AND is_deleted = 0
    """, (new_content.strip(), now, memory_id, clean_id))

    success = cursor.rowcount > 0
    conn.commit()
    conn.close()
    return success


def delete_user_memory(user_id: Optional[str], memory_id: str) -> bool:
    """
    Soft-deletes a specific memory item for an authenticated user.
    """
    if is_guest_user(user_id):
        return False

    clean_id = str(user_id).strip()
    now = datetime.utcnow().isoformat()
    conn = _connect()

    cursor = conn.execute("""
        UPDATE user_long_term_memory
        SET is_deleted = 1, updated_at = ?
        WHERE id = ? AND user_id = ?
    """, (now, memory_id, clean_id))

    success = cursor.rowcount > 0
    conn.commit()
    conn.close()
    return success


def clear_all_user_memories(user_id: Optional[str]) -> bool:
    """
    Soft-deletes all memories for an authenticated user.
    """
    if is_guest_user(user_id):
        return False

    clean_id = str(user_id).strip()
    now = datetime.utcnow().isoformat()
    conn = _connect()

    conn.execute("""
        UPDATE user_long_term_memory
        SET is_deleted = 1, updated_at = ?
        WHERE user_id = ?
    """, (now, clean_id))

    conn.commit()
    conn.close()
    return True


def extract_safe_preferences_from_text(user_id: str, text: str) -> Optional[Tuple[str, str, str]]:
    """
    Extracts non-sensitive communication, lifestyle, diet, or wellness goals.
    Returns (memory_key, content, category) or None.
    """
    if is_guest_user(user_id) or not is_safe_for_memory(text):
        return None

    clean = text.strip()
    lower = clean.lower()

    # 1. Communication
    if any(p in lower for p in ["prefer short", "keep it concise", "brief response", "short answers"]):
        return ("comm_brevity", "Prefers concise, brief responses", "COMMUNICATION")
    if any(p in lower for p in ["bullet points", "use bullets", "in bullets"]):
        return ("comm_format", "Prefers explanations formatted with bullet points", "COMMUNICATION")
    if any(p in lower for p in ["explain simply", "simple terms", "no medical jargon", "explain like"]):
        return ("comm_complexity", "Prefers plain, simple language without complex medical jargon", "COMMUNICATION")

    # 2. Dietary habits
    if "vegetarian" in lower:
        return ("diet_type", "Follows a vegetarian diet", "DIET")
    if "vegan" in lower:
        return ("diet_type", "Follows a vegan diet", "DIET")
    if any(p in lower for p in ["avoid caffeine", "no coffee after", "no caffeine after"]):
        return ("diet_caffeine", "Avoids caffeine in the evening", "DIET")
    if any(p in lower for p in ["avoid dairy", "dairy-free"]):
        return ("diet_dairy", "Prefers dairy-free options", "DIET")

    # 3. Lifestyle
    if "night shift" in lower or "night shifts" in lower:
        return ("lifestyle_work", "Works night shifts or irregular hours", "LIFESTYLE")
    if "desk job" in lower or "sedentary" in lower:
        return ("lifestyle_activity", "Has a sedentary desk job", "LIFESTYLE")
    if (("walk" in lower and ("morning" in lower or "daily" in lower)) or "daily yoga" in lower):
        return ("lifestyle_exercise", "Engages in regular morning walking or yoga", "LIFESTYLE")

    # 4. Goals
    if "water" in lower and any(p in lower for p in ["drink", "liter", "litres", "hydration"]):
        return ("goal_hydration", "Goal: maintain consistent daily water hydration", "GOAL")
    if any(p in lower for p in ["sleep routine", "sleep quality", "improve my sleep"]):
        return ("goal_sleep", "Goal: improve daily sleep routine and consistency", "GOAL")

    return None


def retrieve_relevant_user_memories(user_id: Optional[str], query: str, is_temporary: bool = False, max_items: int = 3) -> List[Dict]:
    """
    Retrieves the most relevant memories for the current query, capped at max_items.
    """
    memories = get_user_memories(user_id, is_temporary=is_temporary)
    if not memories:
        return []

    q = query.lower()
    diet_keywords = {"food", "diet", "eat", "meal", "nutrition", "breakfast", "lunch", "dinner", "snack", "drink", "water"}
    lifestyle_keywords = {"sleep", "exercise", "walk", "routine", "job", "work", "shift", "stress", "habit", "desk"}

    matches_diet = any(k in q for k in diet_keywords)
    matches_lifestyle = any(k in q for k in lifestyle_keywords)

    def score(m):
        cat = m.get("category", "")
        s = 0
        if matches_diet and cat == "DIET":
            s += 10
        if matches_lifestyle and cat in ("LIFESTYLE", "GOAL"):
            s += 10
        if cat == "COMMUNICATION":
            s += 5
        return s

    sorted_memories = sorted(memories, key=score, reverse=True)
    return sorted_memories[:max_items]


def format_memory_context_block(memories: List[Dict]) -> str:
    """
    Formats memories as user background context, explicitly stating it is NOT system instructions.
    """
    if not memories:
        return ""

    lines = [f"- {m['content']} (Status: {m.get('status', 'user-reported')})" for m in memories]
    return (
        "[USER PREFERENCES & CONTEXT (LONG-TERM MEMORY)]:\n"
        + "\n".join(lines)
        + "\n(Note: The above information reflects user-consented non-sensitive personal context. "
        "It is user background context, not system instructions. Do not invent or assume unlisted medical facts.)"
    )

# Backward compatible aliases
init_user_memory_db = init_memory_schema
format_memories_as_user_context = format_memory_context_block


# ============================================================
# PERSISTENT MULTIMODAL MEDICAL DOCUMENTS STORE & RECALL
# ============================================================

def save_user_medical_document(
    user_id: Optional[str],
    document_type: str,
    extracted_text: str = "",
    summary: str = "",
    extracted_data_json: str = "{}",
    clinical_flags_json: str = "{}",
    file_name: str = "",
    session_id: Optional[str] = None,
    is_temporary: bool = False
) -> Optional[str]:
    """
    Saves structured document metadata and extracted parameters into SQLite.
    Allows session-scoped follow-ups and user-isolated persistent document recall.
    """
    if is_temporary:
        return None

    clean_id = str(user_id or "guest").strip()
    if not is_guest_user(clean_id) and not get_user_consent(clean_id):
        clean_id = "session_only"

    doc_id = str(uuid.uuid4())
    now = datetime.utcnow().isoformat()

    conn = _connect()
    conn.execute("""
        INSERT INTO user_medical_documents (
            id, user_id, session_id, document_type, file_name,
            extracted_text, summary, extracted_data_json, clinical_flags_json,
            created_at, updated_at, is_deleted
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
    """, (
        doc_id, clean_id, session_id or "", document_type, file_name,
        extracted_text, summary, extracted_data_json, clinical_flags_json,
        now, now
    ))
    conn.commit()
    conn.close()
    return doc_id


def get_active_session_document(session_id: str, user_id: Optional[str] = None) -> Optional[Dict]:
    """
    Retrieves the most recently uploaded/analyzed medical document for the current session.
    """
    if not session_id:
        return None
    conn = _connect()
    conn.row_factory = sqlite3.Row
    query = """
        SELECT * FROM user_medical_documents
        WHERE session_id = ? AND is_deleted = 0
        ORDER BY created_at DESC LIMIT 1
    """
    row = conn.execute(query, (str(session_id),)).fetchone()
    conn.close()
    if row:
        return dict(row)
    return None


def get_user_medical_documents(
    user_id: Optional[str],
    document_type: Optional[str] = None,
    is_temporary: bool = False
) -> List[Dict]:
    """
    Retrieves all persistent medical documents for an authenticated user.
    Strictly user-isolated and requires opt-in consent.
    """
    if is_temporary or is_guest_user(user_id) or not get_user_consent(user_id):
        return []

    clean_id = str(user_id).strip()
    conn = _connect()
    conn.row_factory = sqlite3.Row

    query = "SELECT * FROM user_medical_documents WHERE user_id = ? AND is_deleted = 0"
    params = [clean_id]

    if document_type:
        query += " AND document_type = ?"
        params.append(document_type)

    query += " ORDER BY created_at DESC"
    rows = conn.execute(query, params).fetchall()
    conn.close()
    return [dict(r) for r in rows]


def delete_user_medical_document(user_id: Optional[str], document_id: str) -> bool:
    """Soft-deletes a medical document record for an authenticated user."""
    if is_guest_user(user_id):
        return False
    clean_id = str(user_id).strip()
    now = datetime.utcnow().isoformat()
    conn = _connect()
    cur = conn.execute("""
        UPDATE user_medical_documents
        SET is_deleted = 1, updated_at = ?
        WHERE id = ? AND user_id = ?
    """, (now, document_id, clean_id))
    success = cur.rowcount > 0
    conn.commit()
    conn.close()
    return success


def clear_all_user_medical_documents(user_id: Optional[str]) -> bool:
    """Soft-deletes all medical document records for an authenticated user."""
    if is_guest_user(user_id):
        return False
    clean_id = str(user_id).strip()
    now = datetime.utcnow().isoformat()
    conn = _connect()
    conn.execute("""
        UPDATE user_medical_documents
        SET is_deleted = 1, updated_at = ?
        WHERE user_id = ?
    """, (now, clean_id))
    conn.commit()
    conn.close()
    return True


# ============================================================
# PERSISTENT CROSS-CHAT MEDICAL ENTITIES & RECALL
# ============================================================

RELATION_MAPPINGS = {
    "mitra": "friend", "friend": "friend", "dost": "friend", "yaar": "friend",
    "मित्रा": "friend", "मित्र": "friend", "दोस्त": "friend",
    "bhau": "brother", "bhai": "brother", "brother": "brother",
    "भाऊ": "brother", "भाई": "brother",
    "bahin": "sister", "behen": "sister", "sister": "sister",
    "बहीण": "sister", "बहन": "sister",
    "aai": "mother", "mom": "mother", "mother": "mother", "maa": "mother",
    "आई": "mother", "माँ": "mother", "माता": "mother",
    "baba": "father", "dad": "father", "father": "father", "pitaji": "father",
    "बाबा": "father", "वडील": "father", "पिताजी": "father",
    "patni": "wife", "wife": "wife", "baiko": "wife",
    "बायको": "wife", "पत्नी": "wife",
    "pati": "husband", "husband": "husband", "navra": "husband",
    "नवरा": "husband", "पति": "husband",
    "mulga": "son", "son": "son", "beta": "son",
    "मुलगा": "son", "बेटा": "son",
    "mulgi": "daughter", "daughter": "daughter", "beti": "daughter",
    "मुलगी": "daughter", "बेटी": "daughter"
}

PRONOUN_STOPWORDS = {
    "mala", "tula", "tyala", "tila", "aamha", "aamhala", "aaplya", "aaplyala", "tyanna",
    "majha", "maza", "mazi", "mazya", "tumhi", "apan", "aapan", "mi", "mee",
    "mujhe", "tujhe", "use", "humein", "mera", "meri", "mere", "apna", "apni",
    "i", "you", "he", "she", "we", "they", "me", "my", "us", "him", "her",
    "konta", "konti", "konte", "kay", "kasa", "kashi", "kashe", "kiti",
    "kya", "kaise", "kaisa", "kaisi", "kitna", "what", "which",
    "cha", "chi", "che", "chya", "la", "ne", "ni", "shi", "hun", "var", "madhe", "mde",
    "ka", "ki", "ke", "ko", "se", "me", "par",
    "aahe", "ahe", "hota", "hoti", "hote", "zala", "zali", "zale", "zhala",
    "hai", "hain", "tha", "thi", "the", "hua", "hui", "hue",
    "problem", "tras", "issue", "takleef", "dikkat",
    "sangit", "sangitla", "sangitle", "sangitli", "sangitlat", "sangitlet",
    "bolla", "bolli", "bolle", "bataya", "bole", "adhi", "adhich", "pehle",
    "सांगितले", "सांगितलं", "सांगितला", "सांगितली", "बोललो", "आधी", "पूर्वी", "बताया"
}

CONCERN_VOCABULARY = {
    "hairfall": "hairfall", "hair fall": "hairfall", "hair loss": "hairfall", "alopecia": "hairfall",
    "केस गळती": "hairfall", "केस गळणे": "hairfall", "बाल झड़ना": "hairfall", "बाल झड़ना": "hairfall",
    "kes galti": "hairfall", "kes galne": "hairfall", "bal jhadna": "hairfall",
    "headache": "headache", "doke dukhi": "headache", "dok dukh": "headache", "doka dukh": "headache",
    "डोकेदुखी": "headache", "डोकं दुखणे": "headache", "सिरदर्द": "headache", "sir dard": "headache",
    "fever": "fever", "tap": "fever", "taap": "fever", "bukhar": "fever", "ताप": "fever", "बुखार": "fever",
    "cough": "cough", "khokla": "cough", "khansi": "cough", "खोकला": "cough", "खांसी": "cough",
    "cold": "cold", "sardi": "cold", "jukam": "cold", "zukaam": "cold", "सर्दी": "cold", "जुकाम": "cold",
    "stomach pain": "stomach pain", "pot dukh": "stomach pain", "potduki": "stomach pain", "pet dard": "stomach pain",
    "पोटदुखी": "stomach pain", "पोट दुखणे": "stomach pain", "पेट दर्द": "stomach pain",
    "back pain": "back pain", "path dukh": "back pain", "kamar dard": "back pain",
    "पाठदुखी": "back pain", "कमर दर्द": "back pain",
    "knee pain": "knee pain", "gudghe dukh": "knee pain", "ghutne ka dard": "knee pain",
    "गुडघेदुखी": "knee pain", "घुटने का दर्द": "knee pain",
    "chest pain": "chest pain", "chhati dukh": "chest pain", "छाती दुखी": "chest pain", "सीने में दर्द": "chest pain",
    "acidity": "acidity", "heartburn": "acidity", "पित्त": "acidity", "अ‍ॅसिडिटी": "acidity", "एसिडिटी": "acidity",
    "vomiting": "vomiting", "ulti": "vomiting", "उलटी": "vomiting", "वमन": "vomiting",
    "dizziness": "dizziness", "chakkar": "dizziness", "चक्कर": "dizziness",
    "weakness": "weakness", "ashaktpana": "weakness", "kamjori": "weakness", "अशक्तपणा": "weakness", "कमजोरी": "weakness",
    "dandruff": "dandruff", "कोंडा": "dandruff", "रूसी": "dandruff",
    "insomnia": "insomnia", "sleeplessness": "insomnia", "jhop na yene": "insomnia", "अनिद्रा": "insomnia", "झोप न येणे": "insomnia",
    "diabetes": "diabetes", "sugar": "diabetes", "blood sugar": "diabetes", "मधुमेह": "diabetes",
    "blood pressure": "blood pressure", "bp": "blood pressure", "उच्च रक्तदाब": "blood pressure", "रक्तदाब": "blood pressure",
    "asthma": "asthma", "दमा": "asthma",
    "allergy": "allergy", "अ‍ॅलर्जी": "allergy", "एलर्जी": "allergy"
}

CONCERN_LABELS_MR = {
    "hairfall": "केस गळती (Hair fall)",
    "headache": "डोकेदुखी (Headache)",
    "fever": "ताप (Fever)",
    "cough": "खोकला (Cough)",
    "cold": "सर्दी (Cold)",
    "stomach pain": "पोटदुखी (Stomach pain)",
    "back pain": "पाठदुखी (Back pain)",
    "knee pain": "गुडघेदुखी (Knee pain)",
    "chest pain": "छातीदुखी (Chest pain)",
    "acidity": "अ‍ॅसिडिटी/पित्त (Acidity)",
    "vomiting": "उलटी (Vomiting)",
    "dizziness": "चक्कर (Dizziness)",
    "weakness": "अशक्तपणा (Weakness)",
    "dandruff": "कोंडा (Dandruff)",
    "insomnia": "झोप न येणे (Insomnia)",
    "diabetes": "मधुमेह (Diabetes)",
    "blood pressure": "रक्तदाब (Blood pressure)",
    "asthma": "दमा (Asthma)",
    "allergy": "अ‍ॅलर्जी (Allergy)"
}

GENERAL_QUESTION_PATTERNS = [
    r"(?i)\b(?:what|why|how)\s+(?:causes?|is|are|leads? to)\b",
    r"(?i)\b(?:causes?|reasons?|symptoms?|signs?|treatment|cure|prevention)\s+of\b",
    r"(?i)\b(?:can|does|do|could|would)\s+(?:fever|headache|cough|cold|[a-z]+)\s+cause\b",
    r"(?i)\b(?:is it possible|is it normal|hypothetically|in general|generally speaking|tell me about)\b",
    r"(?i)\b(?:what are the symptoms|how to prevent|how to treat)\b",
    # Hindi patterns
    r"(?i)\b(?:क्या कारण|क्यों होता|क्यों होती|की वजह|के लक्षण|का इलाज|का उपचार|से क्या होता)\b",
    r"(?i)\b(?:क्या बुखार से|क्या सिरदर्द से)\b",
    # Marathi patterns
    r"(?i)\b(?:कशामुळे होते|कशामुळे होतो|कारणे काय|लक्षणे काय|उपाय काय|माहिती द्या|का होते|का होतो|कसा होतो|कशी होते|का येते|का येतो)\b",
    r"(?i)\b(?:ताप आल्यावर काय|डोकेदुखी कशामुळे|डोकं कशामुळे)\b",
    # Roman Marathi / Hindi patterns
    r"(?i)\b(?:kashamule hoto|kashamule hote|ka hoto|ka hote|kay karave|karne kay|lakshane kay|upay kay|upchar kay|kyu hota|kyun hota|kyu hoti|kya karan)\b",
]


def is_general_or_hypothetical_question(text: str) -> bool:
    """
    Returns True if the text is asking a general or informational question about a medical condition,
    ensuring it is NOT accidentally stored as a personal health history entry.
    """
    if not text:
        return False
    t = text.strip().lower()

    # Personal symptom reporting markers
    personal_assertion_markers = [
        r"(?i)\b(?:i have|i am having|i've got|i am suffering|my|mala|majha|maza|mazi|mujhe|mera|meri)\b",
        r"(?i)\b(?:मला|माझा|माझी|माझे|मला त्रास|मला ताप|मला खोकला|मुझे|मेरा|मेरी)\b"
    ]

    for pat in GENERAL_QUESTION_PATTERNS:
        if re.search(pat, t):
            # Check if there is an explicit first-person symptom assertion
            is_personal = any(re.search(p, t) for p in personal_assertion_markers)
            if not is_personal:
                return True
            # Even with personal pronouns, questions like "What causes fever?" or "Tell me about diabetes" are general
            if re.search(r"(?i)^(what causes|why do people get|causes of|what is|tell me about)\b", t):
                return True

    return False


def extract_medical_entity(text: str, default_subject: str = "self") -> Optional[Dict]:
    """
    Extracts structured medical entities (subject, relation, concern, status)
    from Marathi, Hindi, English, or Romanized conversation statements.
    Strictly ignores general/informational questions to prevent false health facts.
    """
    if not text or not str(text).strip():
        return None

    raw_text = str(text).strip()

    # Guard: Do NOT extract medical entities from general/hypothetical questions
    if is_general_or_hypothetical_question(raw_text):
        return None

    low = raw_text.lower()
    tokens = re.findall(r"[a-zA-Z\u0900-\u097F]+", low)

    # 1. Status detection
    status = "active"
    if any(w in low for w in ["kami zala", "kami zali", "kami ahe", "kam hua", "kam ho gaya", "reduced", "improved", "better", "कमी झाले", "कमी झाली", "कमी झाला", "कमी आहे"]):
        status = "improving"
    elif any(w in low for w in ["thik zala", "thik ahe", "purna thik", "cured", "resolved", "बरे झाले", "ठीक झाले"]):
        status = "resolved"
    elif any(w in low for w in ["vadla", "vadli", "vadhla", "badh gaya", "worse", "increased", "वाढले", "वाढली", "वाढला"]):
        status = "worsening"

    # 2. Subject & Relation detection
    subject = None
    relation = "self"

    # Pattern A: Relation + optional person name (e.g. 'Maza mitra Hariom', 'my friend Hariom', 'माझा मित्र हरीओम')
    m_rel = re.search(
        r"(?:maza|majha|my|mera|majhya|माझा|माझ्या|माझे|मेरा|मेरे)\s+([a-zA-Z\u0900-\u097F]+)(?:\s+([a-zA-Z\u0900-\u097F]+))?",
        raw_text, re.IGNORECASE
    )
    if m_rel:
        w1 = m_rel.group(1).lower()
        w2 = m_rel.group(2).strip() if m_rel.group(2) else None
        if w1 in RELATION_MAPPINGS:
            relation = RELATION_MAPPINGS[w1]
            if w2 and w2.lower() not in PRONOUN_STOPWORDS and w2.lower() not in {"has", "is", "aahe", "ahe", "la", "cha", "chi", "che", "chya"}:
                subject = w2.title()
            else:
                subject = relation.title()

    # Pattern B: Third-party named entity with optional case marker (e.g. 'Hariom la', 'Hariom's', 'हरिओमला')
    if not subject:
        m_case = re.search(
            r"\b([a-zA-Z\u0900-\u097F]{2,25})\s*(?:'s|’s|\s+(?:la|cha|chi|che|chya|ko|ka|ki|ke|ला|चा|ची|चे|च्या|को|का|की|के))\b",
            raw_text, re.IGNORECASE
        )
        if m_case:
            cand = m_case.group(1).strip()
            if cand.lower() not in PRONOUN_STOPWORDS:
                subject = cand.title()
                relation = "third_party"

    # Pattern B2: English auxiliary or preposition + subject (e.g. 'did Hariom have', 'was Hariom suffering from', 'about Hariom')
    if not subject:
        m_en = re.search(r"\b(?:did|does|was|is|for|about|with)\s+([a-zA-Z]{3,20})\s*(?:'s|’s|have|experience|suffer)?\b", raw_text, re.IGNORECASE)
        if m_en:
            cand = m_en.group(1).strip()
            if cand.lower() not in PRONOUN_STOPWORDS and cand.lower() not in {"health", "problem", "medical", "condition", "what", "which", "your", "my"}:
                subject = cand.title()
                relation = "third_party"

    # Pattern C: Suffix attached (e.g. 'Hariomla', 'हरिओमला')
    if not subject:
        for tok in re.findall(r"[a-zA-Z\u0900-\u097F]+", raw_text):
            tok_low = tok.lower()
            if tok_low in PRONOUN_STOPWORDS:
                continue
            # Roman suffix
            m_suf = re.match(r"^([a-zA-Z]{3,20})(?:la|cha|chi|che|chya|ko|ka|ki|ke)$", tok_low)
            if m_suf and m_suf.group(1) not in PRONOUN_STOPWORDS:
                subject = m_suf.group(1).title()
                relation = "third_party"
                break
            # Devanagari suffix
            m_dev_suf = re.match(r"^([\u0900-\u097F]{2,20})(?:ला|चा|ची|चे|च्या|ने|कडे)$", tok)
            if m_dev_suf and m_dev_suf.group(1) not in {"मला", "तुला", "त्याला", "तिला", "आम्हाला", "आपल्याला", "त्यांना", "झाला"}:
                subject = m_dev_suf.group(1)
                relation = "third_party"
                break

    # Pattern D: Self check
    if not subject:
        self_tokens = {"mala", "majha", "maza", "mazi", "mi", "mee", "i", "my", "me", "myself", "मला", "माझा", "माझी", "मी", "मुझे", "मेरा", "मेरी", "मैं"}
        if any(w in tokens for w in self_tokens):
            subject = "self"
            relation = "self"

    # 3. Concern detection
    concern = None
    for kw, canon in CONCERN_VOCABULARY.items():
        if kw in low:
            concern = canon
            break

    if not concern:
        m_concern_pat = re.search(
            r"([a-zA-Z\u0900-\u097F]{2,25})\s+(?:cha|chi|che|ka|ki|ke|चा|ची|चे|च्या)?\s*(?:problem|tras|issue|takleef|dikkat|त्रास|समस्या|तक्रार|तकलीफ)",
            raw_text, re.IGNORECASE
        )
        if m_concern_pat:
            cand = m_concern_pat.group(1).strip().lower()
            if cand not in PRONOUN_STOPWORDS and cand not in {"konta", "kay", "kasa", "kashi", "hota", "aahe"}:
                concern = cand

    if not subject and not concern:
        return None

    # Guard: If no subject was identified and no explicit self tokens were present,
    # do NOT assume 'self' for an orphaned concern
    if not subject:
        self_tokens = {"mala", "majha", "maza", "mazi", "mi", "mee", "i", "my", "me", "myself", "मला", "माझा", "माझी", "मी", "मुझे", "मेरा", "मेरी", "मैं"}
        has_self_token = any(w in tokens for w in self_tokens)
        if has_self_token:
            subject = "self"
            relation = "self"
        elif default_subject != "self":
            subject = default_subject
            relation = "self"
        else:
            return None

    return {
        "subject": subject,
        "relation": relation,
        "subject_relation": relation,
        "concern": concern,
        "status": status,
        "raw_condition": raw_text
    }


def save_or_update_medical_entity(
    user_id: Optional[str],
    subject: str,
    subject_relation: str,
    concern: str,
    raw_condition: str,
    duration: Optional[str] = None,
    severity: Optional[str] = None,
    status: str = "active",
    source_session_id: Optional[str] = None,
    source_message: str = "",
    is_temporary: bool = False,
    medication: Optional[str] = None,
    test_findings: Optional[str] = None,
    notes: Optional[str] = None
) -> bool:
    """
    Saves or updates a structured medical entity tied to user_id.
    Strictly isolated per authenticated user; never saves for guests or temporary chats.
    """
    if is_temporary or is_guest_user(user_id) or not get_user_consent(user_id):
        return False

    if not subject or not concern:
        return False

    clean_id = str(user_id).strip()
    clean_sub = str(subject).strip()
    clean_concern = str(concern).strip().lower()
    now = datetime.utcnow().isoformat()
    new_id = str(uuid.uuid4())

    conn = _connect()
    cursor = conn.cursor()

    # Look for existing active/recorded entity for this user, subject, and concern
    cursor.execute("""
        SELECT id, status FROM user_medical_entities
        WHERE user_id = ? AND LOWER(subject) = LOWER(?) AND LOWER(concern) = LOWER(?) AND is_deleted = 0
        ORDER BY updated_at DESC LIMIT 1
    """, (clean_id, clean_sub, clean_concern))
    existing = cursor.fetchone()

    if existing:
        entity_id, _prev_status = existing
        cursor.execute("""
            UPDATE user_medical_entities
            SET status = ?,
                raw_condition = ?,
                duration = COALESCE(?, duration),
                severity = COALESCE(?, severity),
                medication = COALESCE(?, medication),
                test_findings = COALESCE(?, test_findings),
                notes = COALESCE(?, notes),
                source_session_id = COALESCE(?, source_session_id),
                source_message = ?,
                updated_at = ?
            WHERE id = ?
        """, (status, raw_condition, duration, severity, medication, test_findings, notes, source_session_id, source_message, now, entity_id))
    else:
        cursor.execute("""
            INSERT INTO user_medical_entities (
                id, user_id, subject, subject_relation, concern, raw_condition,
                duration, severity, status, medication, test_findings, notes,
                source_session_id, source_message,
                created_at, updated_at, is_deleted
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
        """, (
            new_id, clean_id, clean_sub, subject_relation, clean_concern, raw_condition,
            duration, severity, status, medication, test_findings, notes,
            source_session_id, source_message, now, now
        ))

    conn.commit()
    conn.close()
    return True


def get_user_medical_entities(
    user_id: Optional[str],
    subject: Optional[str] = None,
    status: Optional[str] = None,
    is_temporary: bool = False
) -> List[Dict]:
    """
    Retrieves saved medical entities for an authenticated user.
    Returns empty list if guest, temporary, or consent not enabled.
    """
    if is_temporary or is_guest_user(user_id) or not get_user_consent(user_id):
        return []

    clean_id = str(user_id).strip()
    conn = _connect()
    conn.row_factory = sqlite3.Row

    query = "SELECT * FROM user_medical_entities WHERE user_id = ? AND is_deleted = 0"
    params = [clean_id]

    if subject:
        if subject.lower() == "self":
            query += " AND (LOWER(subject) = 'self' OR LOWER(subject_relation) = 'self')"
        else:
            query += " AND (LOWER(subject) = LOWER(?) OR LOWER(subject_relation) = LOWER(?))"
            params.extend([subject, subject])

    if status:
        query += " AND status = ?"
        params.append(status)

    query += " ORDER BY updated_at DESC"

    rows = conn.execute(query, params).fetchall()
    conn.close()
    return [dict(r) for r in rows]


def delete_user_medical_entity(user_id: Optional[str], entity_id: str) -> bool:
    """Soft-deletes a medical entity record for an authenticated user."""
    if is_guest_user(user_id):
        return False
    clean_id = str(user_id).strip()
    now = datetime.utcnow().isoformat()
    conn = _connect()
    cur = conn.execute("""
        UPDATE user_medical_entities
        SET is_deleted = 1, updated_at = ?
        WHERE id = ? AND user_id = ?
    """, (now, entity_id, clean_id))
    success = cur.rowcount > 0
    conn.commit()
    conn.close()
    return success


def clear_all_user_medical_entities(user_id: Optional[str]) -> bool:
    """Soft-deletes all medical entity records for an authenticated user."""
    if is_guest_user(user_id):
        return False
    clean_id = str(user_id).strip()
    now = datetime.utcnow().isoformat()
    conn = _connect()
    conn.execute("""
        UPDATE user_medical_entities
        SET is_deleted = 1, updated_at = ?
        WHERE user_id = ?
    """, (now, clean_id))
    conn.commit()
    conn.close()
    return True


def detect_memory_query_intent(query_text: str) -> Dict:
    """
    Classifies a user query into one of:
    - correction (e.g. 'To maza problem navhata, Hariom cha hota')
    - specific_person (e.g. 'Hariom la konta problem hota?')
    - self (e.g. 'Mala konta problem hota?')
    - all (e.g. 'Mi tula adhi kay sangitla hota?')
    - update (e.g. 'Maza mitra cha hairfall ata kami zala aahe')
    - none
    """
    if not query_text or not str(query_text).strip():
        return {"is_memory_query": False, "target_type": None, "target_subject": None}

    raw = str(query_text).strip()
    low = raw.lower()
    tokens = set(re.findall(r"[a-zA-Z\u0900-\u097F]+", low))

    # 0. Correction check (e.g. 'To maza problem navhata, Hariom cha hota')
    is_correction = any(re.search(pat, low) for pat in [
        r"(?:to|he|te)\s+maza\s+(?:problem|tras|takleef)?\s*(?:navhata|nahavta|nahota|nahi)",
        r"that\s+was\s+not\s+my\s+(?:problem|issue|concern|symptom)",
        r"not\s+my\s+(?:problem|issue)",
        r"wo\s+mera\s+(?:problem|tras)?\s*nahi\s+tha",
        r"तो\s+माझा\s+(?:त्रास|प्रॉब्लेम)?\s*नव्हता",
        r"माझा\s+नव्हता",
    ])
    if is_correction:
        cand_subject = None
        cand_relation = "friend"
        m_correct_target = re.search(
            r"([a-zA-Z\u0900-\u097F]{2,20})\s*(?:cha|chi|che|chya|ka|ki|ke|'s|चा|ची|चे|च्या)?\s*(?:hota|hoti|hote|tha|thi|the|होता|होती)",
            raw, re.IGNORECASE
        )
        if m_correct_target:
            w = m_correct_target.group(1).strip()
            if w.lower() not in PRONOUN_STOPWORDS and w.lower() not in {"navhata", "nahavta", "problem", "tras"}:
                cand_subject = w.title()
        if not cand_subject:
            for tok in re.findall(r"[a-zA-Z\u0900-\u097F]+", raw):
                if tok.lower() not in PRONOUN_STOPWORDS and tok.lower() not in {"maza", "problem", "navhata", "hota", "to", "he", "te", "not", "my"}:
                    cand_subject = tok.title()
                    break

        return {
            "is_memory_query": True,
            "target_type": "correction",
            "target_subject": cand_subject or "friend",
            "target_relation": cand_relation,
            "raw_text": raw
        }

    # 1. Status update detection
    status = None
    if any(w in low for w in ["kami zala", "kami zali", "kami ahe", "kam hua", "kam ho gaya", "reduced", "improved", "better", "कमी झाले", "कमी झाली", "कमी झाला", "कमी आहे"]):
        status = "improving"
    elif any(w in low for w in ["thik zala", "thik ahe", "purna thik", "cured", "resolved", "बरे झाले", "ठीक झाले"]):
        status = "resolved"
    elif any(w in low for w in ["vadla", "vadli", "vadhla", "badh gaya", "worse", "increased", "वाढले", "वाढली", "वाढला"]):
        status = "worsening"

    # Check if this is an update statement on a condition
    extracted = extract_medical_entity(raw)
    if status and extracted and extracted.get("concern"):
        return {
            "is_memory_query": True,
            "target_type": "update",
            "target_subject": extracted.get("subject"),
            "target_relation": extracted.get("relation"),
            "target_concern": extracted.get("concern"),
            "new_status": status,
            "raw_text": raw
        }

    # 2. General / Broad recall check
    has_prior_word = any(w in low for w in [
        "adhi", "before", "earlier", "previously", "pehle", "past", "history",
        "आधी", "पूर्वी", "मागील", "पहले"
    ])
    has_tell_word = any(w in low for w in [
        "sangitla", "sangitle", "sangitlat", "sangitli", "told", "said", "mentioned", "shared",
        "bataya", "bole", "सांगितले", "सांगितलं", "सांगितला", "सांगितलीस", "बताया"
    ])
    has_question_word = any(w in low for w in [
        "kay", "kya", "what", "which", "konta", "konti", "konte", "काय", "कोणता", "कोणती", "क्या"
    ])

    if (has_prior_word and (has_tell_word or has_question_word)) or any(p in low for p in [
        "what did i tell you", "what did i say", "my previous symptoms", "health history",
        "adhi kay sangitla", "pehle kya bataya", "आधी काय सांगितलं"
    ]):
        # Distinguish if it is targeted to a specific person or self
        if "mala" in tokens or "majha" in tokens or "my" in tokens or "मला" in tokens or "मुझे" in tokens:
            if not any(rel in low for rel in ["mitra", "friend", "dost", "aai", "mother", "bhau", "brother"]):
                return {
                    "is_memory_query": True,
                    "target_type": "self",
                    "target_subject": "self",
                    "raw_text": raw
                }
        if any(rel in low for rel in ["mitra", "friend", "dost", "aai", "mother", "bhau", "brother"]) or (extracted and extracted.get("subject") != "self"):
            return {
                "is_memory_query": True,
                "target_type": "specific_person",
                "target_subject": extracted.get("subject") if extracted else None,
                "target_relation": extracted.get("relation") if extracted else None,
                "raw_text": raw
            }
        return {
            "is_memory_query": True,
            "target_type": "all",
            "target_subject": None,
            "raw_text": raw
        }

    # 3. Question targeting a person or self
    # e.g. 'Hariom la konta problem hota?', 'Mala konta problem hota?'
    has_problem_or_condition = any(w in low for w in [
        "problem", "tras", "issue", "takleef", "dikkat", "bimar", "ajhar",
        "त्रास", "समस्या", "तक्रार", "दिक्कत", "तकलीफ"
    ]) or bool(extracted and extracted.get("concern"))

    has_state_or_past = any(w in low for w in [
        "hota", "hoti", "hote", "zala", "zali", "had", "was", "did", "tha", "thi", "the",
        "aahe", "ahe", "होता", "होती", "होते", "झाला", "झाली", "आहे"
    ])

    if has_question_word and (has_problem_or_condition or has_state_or_past):
        subj = extracted.get("subject") if extracted else None
        rel = extracted.get("relation") if extracted else "friend"

        if not subj:
            m_en = re.search(r"\b(?:did|does|was|is|for|about|with)\s+([a-zA-Z]{3,20})\s*(?:'s|’s|have|experience|suffer)?\b", raw, re.IGNORECASE)
            if m_en:
                cand = m_en.group(1).strip()
                if cand.lower() not in PRONOUN_STOPWORDS and cand.lower() not in {"health", "problem", "medical", "condition", "what", "which", "your", "my"}:
                    subj = cand.title()
                    rel = "third_party"
            if not subj:
                m_poss = re.search(r"\b([a-zA-Z]{3,20})\s*(?:'s|’s)\b", raw, re.IGNORECASE)
                if m_poss:
                    cand = m_poss.group(1).strip()
                    if cand.lower() not in PRONOUN_STOPWORDS and cand.lower() not in {"health", "problem", "medical", "condition", "what", "which", "your", "my"}:
                        subj = cand.title()
                        rel = "third_party"

        if subj == "self":
            return {
                "is_memory_query": True,
                "target_type": "self",
                "target_subject": "self",
                "raw_text": raw
            }
        elif subj:
            return {
                "is_memory_query": True,
                "target_type": "specific_person",
                "target_subject": subj,
                "target_relation": rel,
                "raw_text": raw
            }

    return {"is_memory_query": False, "target_type": None, "target_subject": None}


def build_cross_chat_memory_response(
    user_id: Optional[str],
    query_text: str,
    language: str = "en",
    is_temporary: bool = False
) -> Optional[str]:
    """
    Generates a localized, highly accurate response to cross-chat memory queries.
    Strictly enforces subject isolation (self vs third party).
    """
    if is_temporary or is_guest_user(user_id) or not get_user_consent(user_id):
        return None

    intent = detect_memory_query_intent(query_text)
    if not intent.get("is_memory_query"):
        return None

    target_type = intent.get("target_type")
    target_subject = intent.get("target_subject")
    target_relation = intent.get("target_relation") or "friend"
    all_entities = get_user_medical_entities(user_id, is_temporary=is_temporary)

    is_mr = language == "mr"
    is_hi = language == "hi"

    # -------------------------------------------------------------------------
    # CASE 0: CORRECTION (e.g. 'To maza problem navhata, Hariom cha hota')
    # -------------------------------------------------------------------------
    if target_type == "correction":
        target_name = target_subject or "मित्र"
        # Find any active entity assigned to 'self'
        self_entities = [e for e in all_entities if e.get("subject", "").lower() == "self"]
        if self_entities:
            latest_self = self_entities[0]
            conn = _connect()
            conn.execute("""
                UPDATE user_medical_entities
                SET subject = ?, subject_relation = ?, updated_at = ?
                WHERE id = ?
            """, (target_name, target_relation, datetime.utcnow().isoformat(), latest_self["id"]))
            conn.commit()
            conn.close()

        if is_mr:
            return f"माफ करा, मी चूक दुरुस्त केली आहे. ही नोंद आता तुमच्या नावावरून काढून तुमचे मित्र ({target_name}) यांच्या नावावर नोंदवली आहे."
        elif is_hi:
            return f"क्षमा करें, मैंने सुधार कर लिया है। यह विवरण आपके रिकॉर्ड से हटाकर ({target_name}) के रिकॉर्ड में दर्ज कर दिया गया है।"
        else:
            return f"I apologize for the confusion. I have corrected the record so that it is attributed to {target_name}, not your personal profile."

    # -------------------------------------------------------------------------
    # CASE 1: STATUS UPDATE (e.g. 'Maza mitra cha hairfall ata kami zala aahe')
    # -------------------------------------------------------------------------
    if target_type == "update":
        new_status = intent.get("new_status", "improving")
        concern = intent.get("target_concern")
        subj = target_subject or "friend"

        # Resolve generic "friend" / "mitra" to specific person if already recorded
        resolved_subject = subj
        if subj.lower() in ("friend", "mitra", "third_party"):
            for e in all_entities:
                if (e.get("subject_relation") == target_relation or e.get("concern") == concern) and e.get("subject", "").lower() != "self":
                    resolved_subject = e.get("subject")
                    break

        # Update matching record in DB
        save_or_update_medical_entity(
            user_id=user_id,
            subject=resolved_subject,
            subject_relation=target_relation,
            concern=concern,
            raw_condition=query_text,
            status=new_status,
            is_temporary=is_temporary
        )

        display_name = resolved_subject if resolved_subject.lower() not in ("friend", "mitra", "third_party") else "तुमचा मित्र"
        concern_mr = CONCERN_LABELS_MR.get(concern, concern)
        if is_mr:
            if new_status == "improving":
                return (
                    f"हे ऐकून समाधान वाटले की {display_name} याच्या {concern_mr} त्रासात आता सुधारणा होत आहे. "
                    "योग्य आहार, पुरेशी विश्रांती आणि आवश्यक काळजी घेणे सुरू ठेवावे. "
                    "त्रास पुन्हा वाढल्यास डॉक्टरांचा सल्ला घ्यावा."
                )
            elif new_status == "resolved":
                return (
                    f"खूप छान बातमी! {display_name} याच्या {concern_mr} त्रास आता पूर्णपणे बरा झाला आहे. "
                    "आरोग्याची काळजी घेत राहा."
                )
            else:
                return (
                    f"मी नोंदवून घेतले आहे की {display_name} याच्या {concern_mr} त्रासात बदल झाला आहे. "
                    "त्रास जास्त असल्यास वैद्यकीय सल्ला नक्की घ्या."
                )
        elif is_hi:
            display_name_hi = resolved_subject if resolved_subject.lower() not in ("friend", "mitra", "third_party") else "आपके दोस्त"
            return (
                f"यह जानकर अच्छा लगा कि {display_name_hi} की {concern} की परेशानी में अब सुधार हो रहा है। "
                "संतुलित आहार और पर्याप्त आराम जारी रखें। यदि परेशानी बढ़े तो डॉक्टर से परामर्श लें।"
            )
        else:
            display_name_en = resolved_subject if resolved_subject.lower() not in ("friend", "mitra", "third_party") else "your friend"
            return (
                f"It is good to hear that {display_name_en}'s {concern} problem has improved. "
                "Encourage healthy habits, balanced nutrition, and adequate rest. "
                "Consult a healthcare professional if symptoms persist."
            )

    # -------------------------------------------------------------------------
    # CASE 2: SPECIFIC PERSON RECALL (e.g. 'Hariom la konta problem hota?')
    # -------------------------------------------------------------------------
    if target_type == "specific_person":
        matching = []
        for e in all_entities:
            sub = e.get("subject", "").lower()
            rel = e.get("subject_relation", "").lower()
            target_low = (target_subject or "").lower()
            if target_low in sub or sub in target_low or target_low in rel:
                matching.append(e)

        if matching:
            top = matching[0]
            subj_name = top.get("subject")
            concern = top.get("concern")
            dur = top.get("duration")
            status_val = top.get("status", "active")
            concern_mr = CONCERN_LABELS_MR.get(concern, concern)

            if is_mr:
                status_note = ""
                if status_val == "improving":
                    status_note = ", आणि नंतर तुम्ही सांगितले होते की हा त्रास आता कमी झाला आहे"
                elif status_val == "resolved":
                    status_note = ", आणि हा त्रास आता पूर्णपणे बरा झाला आहे"

                dur_note = f" ({dur} पासून)" if dur else ""
                return (
                    f"तुम्ही आधी सांगितले होते की तुमचा {top.get('subject_relation', 'मित्र')} "
                    f"({subj_name}) याला{dur_note} {concern_mr} चा त्रास आहे{status_note}."
                )
            elif is_hi:
                status_note = ""
                if status_val == "improving":
                    status_note = ", और बाद में आपने बताया था कि इसमें सुधार हो रहा है"
                elif status_val == "resolved":
                    status_note = ", और यह परेशानी अब ठीक हो चुकी है"
                dur_note = f" ({dur} से)" if dur else ""
                return (
                    f"आपने पहले बताया था कि आपके {top.get('subject_relation', 'दोस्त')} "
                    f"({subj_name}) को{dur_note} {concern} की समस्या है{status_note}।"
                )
            else:
                status_note = ""
                if status_val == "improving":
                    status_note = ", and you later mentioned it has improved"
                elif status_val == "resolved":
                    status_note = ", and this condition is now resolved"
                dur_note = f" for {dur}" if dur else ""
                return (
                    f"You previously mentioned that your {top.get('subject_relation', 'friend')} "
                    f"({subj_name}) has a {concern} problem{dur_note}{status_note}."
                )
        else:
            if is_mr:
                return f"माझ्याकडे {target_subject or 'तुमच्या मित्राबद्दल'} आधीची कोणतीही वैद्यकीय नोंद नाही."
            elif is_hi:
                return f"मेरे पास {target_subject or 'आपके दोस्त'} के बारे में पहले से कोई जानकारी दर्ज नहीं है।"
            else:
                return f"I do not have any saved medical records for {target_subject or 'your friend'}."

    # -------------------------------------------------------------------------
    # CASE 3: SELF RECALL (e.g. 'Mala konta problem hota?')
    # -------------------------------------------------------------------------
    if target_type == "self":
        self_records = [
            e for e in all_entities
            if e.get("subject", "").lower() == "self" or e.get("subject_relation", "").lower() == "self"
        ]

        if self_records:
            items_mr = [
                f"- {CONCERN_LABELS_MR.get(e['concern'], e['concern'])}{' (' + e['duration'] + ' पासून)' if e.get('duration') else ''}"
                for e in self_records
            ]
            items_en = [
                f"- {e['concern'].title()}{' (for ' + e['duration'] + ')' if e.get('duration') else ''}"
                for e in self_records
            ]

            if is_mr:
                return (
                    "तुम्ही आधी स्वतःसाठी खालील त्रासांची नोंद केली होती:\n"
                    + "\n".join(items_mr)
                )
            elif is_hi:
                return (
                    "आपने पहले अपने लिए निम्नलिखित लक्षणों की जानकारी दी थी:\n"
                    + "\n".join([f"- {e['concern']}" for e in self_records])
                )
            else:
                return (
                    "You previously reported the following health concerns for yourself:\n"
                    + "\n".join(items_en)
                )
        else:
            # CRITICAL PERSON ISOLATION: User has NO self records
            # Mention third-party records if present so user sees context, without conflating
            other_records = [e for e in all_entities if e.get("subject", "").lower() != "self"]
            if other_records:
                first_other = other_records[0]
                other_name = first_other.get("subject")
                other_concern = first_other.get("concern")
                other_concern_mr = CONCERN_LABELS_MR.get(other_concern, other_concern)

                if is_mr:
                    return (
                        "तुम्ही स्वतःच्या कोणत्याही त्रासाबद्दल आधी माहिती दिलेली नाही. "
                        f"(तुम्ही आधी तुमच्या मित्राबद्दल - {other_name} - {other_concern_mr} चा त्रास सांगितला होता.)"
                    )
                elif is_hi:
                    return (
                        "आपने अपने लिए कोई समस्या दर्ज नहीं की है। "
                        f"(आपने अपने दोस्त {other_name} के लिए {other_concern} की समस्या बताई थी।)"
                    )
                else:
                    return (
                        "You haven't reported any personal health concerns yet. "
                        f"(You previously mentioned that {other_name} had a {other_concern} problem.)"
                    )
            else:
                if is_mr:
                    return "तुम्ही स्वतःच्या कोणत्याही त्रासाबद्दल आधी माहिती दिलेली नाही."
                elif is_hi:
                    return "आपने अपने किसी लक्षण या परेशानी के बारे में पहले नहीं बताया है।"
                else:
                    return "You haven't reported any personal health concerns yet."

    # -------------------------------------------------------------------------
    # CASE 4: BROAD / ALL RECALL (e.g. 'Mi tula adhi kay sangitla hota?')
    # -------------------------------------------------------------------------
    if target_type == "all":
        if not all_entities:
            if is_mr:
                return (
                    "माझ्याकडे तुमच्या पूर्वीच्या कोणत्याही संभाषणातील वैद्यकीय नोंदी उपलब्ध नाहीत. "
                    "तुम्ही तुमची किंवा इतरांची लक्षणे सांगू शकता."
                )
            elif is_hi:
                return (
                    "मेरे पास आपकी पिछली बातचीत का कोई स्वास्थ्य विवरण दर्ज नहीं है।"
                )
            else:
                return (
                    "I do not have any structured health information saved from your previous chats yet."
                )

        lines_mr = []
        lines_en = []
        for e in all_entities:
            sub = e.get("subject", "")
            rel = e.get("subject_relation", "self")
            concern = e.get("concern", "")
            stat = e.get("status", "active")
            concern_mr = CONCERN_LABELS_MR.get(concern, concern)

            stat_mr = " (सुधारणा होत आहे)" if stat == "improving" else (" (बरा झाला)" if stat == "resolved" else "")
            stat_en = " (Status: Improving)" if stat == "improving" else (" (Status: Resolved)" if stat == "resolved" else "")

            if sub.lower() == "self":
                lines_mr.append(f"- स्वतः: {concern_mr}{stat_mr}")
                lines_en.append(f"- Yourself: {concern.title()}{stat_en}")
            else:
                lines_mr.append(f"- {rel.title()} ({sub}): {concern_mr}{stat_mr}")
                lines_en.append(f"- {rel.title()} ({sub}): {concern.title()}{stat_en}")

        if is_mr:
            return (
                "तुम्ही पूर्वी खालील माहिती सांगितली होती:\n"
                + "\n".join(lines_mr)
                + "\n\n(टीप: ही तुम्ही नोंदवलेली माहिती आहे, वैद्यकीय निदान नाही. त्रास वाढल्यास डॉक्टरांचा सल्ला घ्या.)"
            )
        elif is_hi:
            return (
                "आपने पहले निम्नलिखित जानकारी साझा की थी:\n"
                + "\n".join([f"- {e.get('subject')}: {e.get('concern')}" for e in all_entities])
            )
        else:
            return (
                "Here is the health information you previously shared with me across conversations:\n"
                + "\n".join(lines_en)
                + "\n\n(Note: This is a record of information you reported, not a medical diagnosis.)"
            )

    return None


def format_medical_entities_context(user_id: Optional[str], is_temporary: bool = False) -> str:
    """
    Formats active medical entities as context for injection into LLM prompts.
    """
    entities = get_user_medical_entities(user_id, is_temporary=is_temporary)
    if not entities:
        return ""

    lines = []
    for e in entities:
        sub = e.get("subject", "")
        rel = e.get("subject_relation", "self")
        concern = e.get("concern", "")
        stat = e.get("status", "active")
        lines.append(f"- Subject: {sub} (Relation: {rel}), Concern: {concern}, Status: {stat}")

    return (
        "[PERSISTENT MEDICAL HISTORY ACROSS CONVERSATIONS (USER-CONSENTED)]:\n"
        + "\n".join(lines)
        + "\n(Enforce strict person isolation: do not attribute a friend's condition to the user, and vice versa.)"
    )


# Initialize schema on load
init_memory_schema()


