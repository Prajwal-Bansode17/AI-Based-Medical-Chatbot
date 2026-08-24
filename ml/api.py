from pathlib import Path
import re
import sqlite3
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

MEDICAL_KNOWLEDGE_FILE = (
    BASE_DIR
    / "data"
    / "raw"
    / "medical_knowledge.csv"
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


init_chat_database()
init_medical_memory_table()


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

if not MEDICAL_KNOWLEDGE_FILE.exists():

    raise FileNotFoundError(
        "Medical Knowledge file not found: "
        f"{MEDICAL_KNOWLEDGE_FILE}"
    )

medical_knowledge = pd.read_csv(
    MEDICAL_KNOWLEDGE_FILE
)

print(
    "Medical Knowledge Base loaded successfully."
)

print("----------------------------------------")
print(
    "Medical Knowledge records:",
    len(medical_knowledge)
)
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

    query = normalize_text(
        text
    )

    patterns = [

        r"(\d+)\s*(day|days)",
        r"(\d+)\s*(hour|hours)",
        r"(\d+)\s*(week|weeks)",
        r"(\d+)\s*(month|months)"
    ]


    for pattern in patterns:

        match = re.search(
            pattern,
            query
        )

        if match:

            number = match.group(
                1
            )

            unit = match.group(
                2
            )

            unit_map = {

                "day":
                    "day" if number == "1"
                    else "days",

                "days":
                    "day" if number == "1"
                    else "days",

                "hour":
                    "hour" if number == "1"
                    else "hours",

                "hours":
                    "hour" if number == "1"
                    else "hours",

                "week":
                    "week" if number == "1"
                    else "weeks",

                "weeks":
                    "week" if number == "1"
                    else "weeks",

                "month":
                    "month" if number == "1"
                    else "months",

                "months":
                    "month" if number == "1"
                    else "months"
            }

            return (
                f"{number} "
                f"{unit_map.get(unit, unit)}"
            )


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

    query = normalize_text(
        text
    )

    words = set(
        query.split()
    )

    symptoms = extract_symptoms(
        text
    )


    if (
        "opd" in words
        or
        "outpatient" in words
    ):

        return "opd"


    emergency_words = {

        "emergency",
        "unconscious",
        "critical",
        "trauma"
    }


    if words.intersection(
        emergency_words
    ):

        return "emergency"


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
        and
        any(
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
        "symptoms"
    ]


    if (
        symptoms
        and
        any(
            pattern in query
            for pattern in information_patterns
        )
    ):

        return "symptom_information"


    if symptoms:

        return "symptom_assistance"


    return str(
        model_intent
    )


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
            [text]
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
                        [text]
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
            text
        )

        duration = extract_duration(
            text
        )

        measurements = (
            extract_medical_measurements(
                text
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
                text
            )
        )


        # ====================================================
        # NORMALIZED QUERY
        # ====================================================

        normalized_query = (
            normalize_text(
                text
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
                text
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
                    "general_fallback"
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
        # INTENT
        # ====================================================

        final_intent = (
            determine_final_intent(
                text,
                model_prediction,
                confidence
            )
        )


        # ====================================================
        # MEDICAL RESPONSE
        # ====================================================

        if symptoms:

            question_type = (
                get_question_type(
                    text
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
                        medical_answer,

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
                text,
                final_intent
            )
        )


        if mediq_result is not None:

            answer = (
                mediq_result[
                    "answer"
                ]
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
