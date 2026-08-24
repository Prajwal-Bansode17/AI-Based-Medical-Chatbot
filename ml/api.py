from pathlib import Path
import re

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

DATA_FILE = (
    BASE_DIR
    / "data"
    / "raw"
    / "mediq"
    / "MEDIQ-main"
    / "data"
    / "mediq_full.csv"
)


# ============================================================
# APP
# ============================================================

app = Flask(__name__)


# ============================================================
# LOAD MODEL
# ============================================================

print("Loading ML model...")

model = joblib.load(MODEL_FILE)

print("ML model loaded successfully.")


# ============================================================
# LOAD DATASET
# ============================================================

print("Loading MEDIQ dataset...")

if not DATA_FILE.exists():
    raise FileNotFoundError(
        f"MEDIQ dataset not found: {DATA_FILE}"
    )

dataset = pd.read_csv(DATA_FILE)

print("MEDIQ dataset loaded successfully.")

print("----------------------------------------")
print("Dataset shape:", dataset.shape)
print("----------------------------------------")


# ============================================================
# COLUMN DETECTION
# ============================================================

def find_column(columns, names):

    mapping = {
        str(c).strip().lower(): c
        for c in columns
    }

    for name in names:

        if name.lower() in mapping:
            return mapping[name.lower()]

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


print("----------------------------------------")
print("Detected dataset columns")
print("----------------------------------------")
print("Question :", QUESTION_COLUMN)
print("Answer   :", ANSWER_COLUMN)
print("Category :", CATEGORY_COLUMN)
print("----------------------------------------")


if QUESTION_COLUMN is None:
    raise ValueError("Question column not found.")

if ANSWER_COLUMN is None:
    raise ValueError("Answer column not found.")


# ============================================================
# CLEAN DATA
# ============================================================

dataset = dataset.copy()

dataset[QUESTION_COLUMN] = (
    dataset[QUESTION_COLUMN]
    .fillna("")
    .astype(str)
    .str.strip()
)

dataset[ANSWER_COLUMN] = (
    dataset[ANSWER_COLUMN]
    .fillna("")
    .astype(str)
    .str.strip()
)

dataset = dataset[
    (dataset[QUESTION_COLUMN] != "")
    & (dataset[ANSWER_COLUMN] != "")
].copy()

dataset.reset_index(drop=True, inplace=True)


print("Clean MEDIQ records:", len(dataset))


# ============================================================
# NORMALIZATION
# ============================================================

def normalize_text(text):

    text = str(text).lower()

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


dataset["_question_clean"] = dataset[
    QUESTION_COLUMN
].apply(normalize_text)


# ============================================================
# TF-IDF INDEX
# ============================================================

print("Creating answer retrieval index...")

vectorizer = TfidfVectorizer(
    lowercase=True,
    stop_words="english",
    ngram_range=(1, 2),
    min_df=1
)

retrieval_matrix = vectorizer.fit_transform(
    dataset["_question_clean"]
)

print("Answer retrieval index ready.")


# ============================================================
# COMMON STOP WORDS
# ============================================================

STOP_WORDS = {
    "the",
    "a",
    "an",
    "is",
    "are",
    "am",
    "i",
    "me",
    "my",
    "what",
    "where",
    "when",
    "how",
    "can",
    "could",
    "would",
    "please",
    "tell",
    "about",
    "for",
    "to",
    "of",
    "do",
    "does",
    "did",
    "have",
    "has",
    "had",
    "you",
    "your",
    "and",
    "with",
    "there",
    "any"
}


def get_words(text):

    return {
        word
        for word in normalize_text(text).split()
        if len(word) >= 3
        and word not in STOP_WORDS
    }


# ============================================================
# BAD ANSWER DETECTION
# ============================================================

def is_bad_answer(answer):

    text = normalize_text(answer)

    bad_patterns = [
        "sorry please ask another",
        "sorry please ask another appropriate question",
        "appropriate question",
        "ask another question",
        "unable to provide",
        "cannot provide any information",
        "i do not have the information",
        "i don t have the information"
    ]

    for pattern in bad_patterns:

        if pattern in text:
            return True

    return False


# ============================================================
# INTENT CORRECTION
# ============================================================

def determine_final_intent(text, model_intent, confidence):

    query = normalize_text(text)

    words = set(query.split())

    # --------------------------------------------------------
    # OPD
    # --------------------------------------------------------

    if "opd" in words or "outpatient" in words:

        return "opd"


    # --------------------------------------------------------
    # EMERGENCY
    # --------------------------------------------------------

    emergency_words = {
        "emergency",
        "unconscious",
        "breathing",
        "severe",
        "critical",
        "trauma"
    }

    if words.intersection(emergency_words):

        return "emergency"


    # --------------------------------------------------------
    # SYMPTOM ASSISTANCE
    # User is saying that they HAVE symptoms
    # --------------------------------------------------------

    symptom_words = {
        "fever",
        "headache",
        "cough",
        "cold",
        "pain",
        "vomiting",
        "nausea",
        "weakness",
        "dizziness",
        "fatigue",
        "diarrhea",
        "breathing"
    }

    has_symptom = bool(
        words.intersection(symptom_words)
    )

    assistance_patterns = [
        "i have",
        "i am having",
        "i feel",
        "i am feeling",
        "suffering from",
        "having",
        "experiencing",
        "my"
    ]

    is_assistance = any(
        pattern in query
        for pattern in assistance_patterns
    )

    if has_symptom and is_assistance:

        return "symptom_assistance"


    # --------------------------------------------------------
    # SYMPTOM INFORMATION
    # User is asking about symptoms
    # --------------------------------------------------------

    information_patterns = [
        "symptoms of",
        "symptoms for",
        "what are the symptoms",
        "signs of",
        "how do i know if"
    ]

    if any(
        pattern in query
        for pattern in information_patterns
    ):

        return "symptom_information"


    # --------------------------------------------------------
    # LOW CONFIDENCE
    # --------------------------------------------------------

    if confidence < 0.40:

        if has_symptom:

            return "symptom_information"


    return str(model_intent)


# ============================================================
# SPECIAL TOPIC CHECKS
# ============================================================

def is_yellow_fever(text):

    query = normalize_text(text)

    return (
        "yellow fever" in query
        or "yellow fever vaccine" in query
        or "yellow fever vaccination" in query
    )


def is_yellow_fever_record(question, answer):

    text = (
        normalize_text(question)
        + " "
        + normalize_text(answer)
    )

    return (
        "yellow fever" in text
        or "yellow fever vaccine" in text
        or "yellow fever vaccination" in text
    )


# ============================================================
# OPD QUESTION TYPE
# ============================================================

def opd_question_type(text):

    query = normalize_text(text)

    if any(
        phrase in query
        for phrase in [
            "where is",
            "where can i find",
            "location",
            "located",
            "where do i go",
            "which building",
            "which department"
        ]
    ):

        return "location"


    if any(
        phrase in query
        for phrase in [
            "book",
            "booking",
            "appointment",
            "register"
        ]
    ):

        return "booking"


    if any(
        phrase in query
        for phrase in [
            "timing",
            "timings",
            "time",
            "when does",
            "when is",
            "hours",
            "open"
        ]
    ):

        return "timing"


    return "general"


# ============================================================
# RETRIEVE ANSWER
# ============================================================

def retrieve_answer(user_text, final_intent):

    query = normalize_text(user_text)

    query_words = get_words(user_text)

    query_vector = vectorizer.transform(
        [query]
    )

    similarities = cosine_similarity(
        query_vector,
        retrieval_matrix
    )[0]


    candidates = []


    # ========================================================
    # SPECIAL OPD FILTERING
    # ========================================================

    opd_type = None

    if final_intent == "opd":

        opd_type = opd_question_type(
            user_text
        )


    # ========================================================
    # SCORE EVERY DATASET RECORD
    # ========================================================

    for index in range(len(dataset)):

        question = str(
            dataset.iloc[index][QUESTION_COLUMN]
        )

        answer = str(
            dataset.iloc[index][ANSWER_COLUMN]
        )

        clean_question = normalize_text(
            question
        )


        # ----------------------------------------------------
        # Skip bad answers
        # ----------------------------------------------------

        if is_bad_answer(answer):
            continue


        # ----------------------------------------------------
        # Yellow fever filtering
        # ----------------------------------------------------

        if (
            "fever" in query_words
            and not is_yellow_fever(user_text)
            and is_yellow_fever_record(
                question,
                answer
            )
        ):

            continue


        # ----------------------------------------------------
        # TF-IDF
        # ----------------------------------------------------

        tfidf_score = float(
            similarities[index]
        )


        # ----------------------------------------------------
        # Keyword overlap
        # ----------------------------------------------------

        question_words = get_words(
            question
        )

        if query_words:

            overlap = len(
                query_words.intersection(
                    question_words
                )
            )

            keyword_score = (
                overlap / len(query_words)
            )

        else:

            keyword_score = 0.0


        # ----------------------------------------------------
        # Intent/category score
        # ----------------------------------------------------

        category_score = 0.0

        if CATEGORY_COLUMN is not None:

            category = normalize_text(
                dataset.iloc[index][CATEGORY_COLUMN]
            )

            if category == normalize_text(
                final_intent
            ):

                category_score = 0.30


        # ----------------------------------------------------
        # Phrase bonus
        # ----------------------------------------------------

        phrase_bonus = 0.0

        if query == clean_question:

            phrase_bonus = 0.50


        # ----------------------------------------------------
        # OPD LOCATION / BOOKING / TIMING
        # ----------------------------------------------------

        opd_bonus = 0.0

        if final_intent == "opd":

            answer_text = normalize_text(
                answer
            )

            question_text = normalize_text(
                question
            )


            if opd_type == "location":

                location_words = [
                    "where",
                    "location",
                    "located",
                    "find",
                    "department",
                    "building",
                    "room"
                ]

                booking_words = [
                    "book",
                    "booking",
                    "appointment",
                    "register",
                    "ors"
                ]

                if any(
                    word in question_text
                    or word in answer_text
                    for word in location_words
                ):

                    opd_bonus += 0.25


                if any(
                    word in answer_text
                    for word in booking_words
                ):

                    opd_bonus -= 0.20


            elif opd_type == "booking":

                booking_words = [
                    "book",
                    "booking",
                    "appointment",
                    "register",
                    "ors"
                ]

                if any(
                    word in question_text
                    or word in answer_text
                    for word in booking_words
                ):

                    opd_bonus += 0.30


            elif opd_type == "timing":

                timing_words = [
                    "timing",
                    "timings",
                    "time",
                    "hours",
                    "open"
                ]

                if any(
                    word in question_text
                    or word in answer_text
                    for word in timing_words
                ):

                    opd_bonus += 0.30


        # ----------------------------------------------------
        # Symptom bonus
        # ----------------------------------------------------

        symptom_bonus = 0.0

        if final_intent in [
            "symptom_assistance",
            "symptom_information"
        ]:

            symptom_words = {
                "fever",
                "headache",
                "cough",
                "cold",
                "pain",
                "vomiting",
                "nausea",
                "weakness",
                "dizziness",
                "fatigue",
                "diarrhea"
            }

            matched_symptoms = (
                query_words.intersection(
                    symptom_words
                )
                .intersection(
                    question_words
                )
            )

            symptom_bonus = (
                len(matched_symptoms) * 0.15
            )


        # ----------------------------------------------------
        # Final score
        # ----------------------------------------------------

        final_score = (
            (tfidf_score * 0.50)
            + (keyword_score * 0.25)
            + category_score
            + phrase_bonus
            + opd_bonus
            + symptom_bonus
        )


        candidates.append(
            (
                index,
                final_score,
                tfidf_score
            )
        )


    # ========================================================
    # NO CANDIDATE
    # ========================================================

    if not candidates:

        return (
            "I could not find a suitable answer in the "
            "medical knowledge dataset. Please consult "
            "a qualified healthcare professional.",
            0.0
        )


    # ========================================================
    # SORT
    # ========================================================

    candidates.sort(
        key=lambda x: x[1],
        reverse=True
    )


    best_index = candidates[0][0]
    best_score = candidates[0][1]


    answer = str(
        dataset.iloc[
            best_index
        ][ANSWER_COLUMN]
    )


    return (
        answer,
        round(
            min(best_score, 1.0),
            4
        )
    )


# ============================================================
# HOME
# ============================================================

@app.get("/")
def home():

    return jsonify({

        "status": "success",

        "service":
            "MEDASSIST AI Medical NLP API",

        "message":
            "API is running",

        "model_loaded":
            True,

        "dataset_loaded":
            True,

        "dataset_records":
            int(len(dataset))

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

        "dataset_records":
            int(len(dataset))

    })


# ============================================================
# PREDICT
# ============================================================

@app.post("/predict")
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
        or not isinstance(text, str)
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


    try:

        # ----------------------------------------------------
        # ML MODEL
        # ----------------------------------------------------

        model_prediction = model.predict(
            [text]
        )[0]

        model_prediction = str(
            model_prediction
        )


        # ----------------------------------------------------
        # CONFIDENCE
        # ----------------------------------------------------

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
                    max(probabilities)
                )

            except Exception:

                confidence = 0.0


        # ----------------------------------------------------
        # FINAL INTENT
        # ----------------------------------------------------

        final_intent = determine_final_intent(
            text,
            model_prediction,
            confidence
        )


        # ----------------------------------------------------
        # ANSWER
        # ----------------------------------------------------

        answer, answer_similarity = (
            retrieve_answer(
                text,
                final_intent
            )
        )


        # ----------------------------------------------------
        # RESPONSE
        # ----------------------------------------------------

        return jsonify({

            "status":
                "success",

            "question":
                text,

            "intent":
                final_intent,

            "confidence":
                round(
                    confidence,
                    4
                ),

            "answer":
                answer,

            "answer_similarity":
                answer_similarity

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
    print("Model       : Loaded")
    print("Dataset     : Loaded")
    print("Records     :", len(dataset))
    print("API Port    : 5000")
    print("----------------------------------------")

    app.run(
        host="0.0.0.0",
        port=5000,
        debug=False
    )