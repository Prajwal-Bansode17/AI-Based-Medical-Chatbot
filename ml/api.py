from pathlib import Path

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
    / "ProxyAyush-MEDIQ-00091b1"
    / "data"
    / "mediq_full.csv"
)


# ============================================================
# FLASK APP
# ============================================================

app = Flask(__name__)


# ============================================================
# LOAD ML MODEL
# ============================================================

print("Loading ML model...")

model = joblib.load(MODEL_FILE)

print("ML model loaded successfully.")


# ============================================================
# LOAD MEDIQ DATASET
# ============================================================

print("Loading MEDIQ dataset...")

dataset = pd.read_csv(DATA_FILE)

print("MEDIQ dataset loaded successfully.")
print("Dataset shape:", dataset.shape)
print("Dataset columns:", dataset.columns.tolist())


# ============================================================
# NORMALIZE COLUMN NAMES
# ============================================================

dataset.columns = [
    str(column).strip().lower()
    for column in dataset.columns
]


# ============================================================
# DETECT COLUMNS
# ============================================================

QUESTION_COLUMN = None
ANSWER_COLUMN = None
CATEGORY_COLUMN = None


for column in dataset.columns:

    if column in [
        "question",
        "questions",
        "query",
        "text",
        "input"
    ]:
        QUESTION_COLUMN = column
        break


for column in dataset.columns:

    if column in [
        "answer",
        "answers",
        "response",
        "responses",
        "reply",
        "output"
    ]:
        ANSWER_COLUMN = column
        break


for column in dataset.columns:

    if column in [
        "category",
        "categories",
        "intent",
        "label",
        "class"
    ]:
        CATEGORY_COLUMN = column
        break


print("----------------------------------------")
print("Detected dataset columns")
print("----------------------------------------")
print("Question :", QUESTION_COLUMN)
print("Answer   :", ANSWER_COLUMN)
print("Category :", CATEGORY_COLUMN)
print("----------------------------------------")


# ============================================================
# VALIDATE COLUMNS
# ============================================================

if QUESTION_COLUMN is None:
    raise ValueError(
        "Question column not found in MEDIQ dataset."
    )

if ANSWER_COLUMN is None:
    raise ValueError(
        "Answer column not found in MEDIQ dataset."
    )

if CATEGORY_COLUMN is None:
    raise ValueError(
        "Category/intent column not found in MEDIQ dataset."
    )


# ============================================================
# CLEAN DATASET
# ============================================================

dataset = dataset[
    [
        QUESTION_COLUMN,
        ANSWER_COLUMN,
        CATEGORY_COLUMN
    ]
].dropna()


dataset[QUESTION_COLUMN] = (
    dataset[QUESTION_COLUMN]
    .astype(str)
    .str.strip()
)


dataset[ANSWER_COLUMN] = (
    dataset[ANSWER_COLUMN]
    .astype(str)
    .str.strip()
)


dataset[CATEGORY_COLUMN] = (
    dataset[CATEGORY_COLUMN]
    .astype(str)
    .str.strip()
)


dataset = dataset[
    (dataset[QUESTION_COLUMN] != "")
    &
    (dataset[ANSWER_COLUMN] != "")
    &
    (dataset[CATEGORY_COLUMN] != "")
].reset_index(drop=True)


print("Clean MEDIQ records:", len(dataset))


# ============================================================
# CREATE TF-IDF RETRIEVAL INDEX
# ============================================================

print("Creating answer retrieval index...")

retrieval_vectorizer = TfidfVectorizer(
    lowercase=True,
    ngram_range=(1, 2)
)


dataset_question_vectors = (
    retrieval_vectorizer.fit_transform(
        dataset[QUESTION_COLUMN]
    )
)


print("Answer retrieval index ready.")


# ============================================================
# HOME
# ============================================================

@app.get("/")
def home():

    return jsonify({
        "status": "success",
        "service": "MEDASSIST AI Medical NLP API",
        "message": "API is running"
    })


# ============================================================
# HEALTH
# ============================================================

@app.get("/health")
def health():

    return jsonify({
        "status": "healthy",
        "model_loaded": True,
        "dataset_loaded": True,
        "dataset_records": int(len(dataset))
    })


# ============================================================
# ANSWER RETRIEVAL
# ============================================================

def get_best_answer(question, predicted_intent):

    # --------------------------------------------------------
    # Find records belonging to predicted intent/category
    # --------------------------------------------------------

    intent_matches = dataset[
        dataset[CATEGORY_COLUMN]
        .astype(str)
        .str.lower()
        ==
        str(predicted_intent).lower()
    ]


    if intent_matches.empty:

        candidate_indices = dataset.index.tolist()

    else:

        candidate_indices = intent_matches.index.tolist()


    # --------------------------------------------------------
    # Convert user question to TF-IDF
    # --------------------------------------------------------

    query_vector = retrieval_vectorizer.transform(
        [question]
    )


    # --------------------------------------------------------
    # Get candidate vectors
    # --------------------------------------------------------

    candidate_vectors = dataset_question_vectors[
        candidate_indices
    ]


    # --------------------------------------------------------
    # Calculate cosine similarity
    # --------------------------------------------------------

    similarities = cosine_similarity(
        query_vector,
        candidate_vectors
    )[0]


    # --------------------------------------------------------
    # Find best matching question
    # --------------------------------------------------------

    best_position = int(
        similarities.argmax()
    )


    best_score = float(
        similarities[best_position]
    )


    best_index = candidate_indices[
        best_position
    ]


    best_answer = dataset.loc[
        best_index,
        ANSWER_COLUMN
    ]


    return {
        "answer": str(best_answer),
        "answer_similarity": round(
            best_score,
            4
        )
    }


# ============================================================
# PREDICT
# ============================================================

@app.post("/predict")
def predict():

    try:

        # ----------------------------------------------------
        # GET JSON BODY
        # ----------------------------------------------------

        data = request.get_json(
            silent=True
        )


        if not data:

            return jsonify({
                "status": "error",
                "message": "Request body is required"
            }), 400


        # ----------------------------------------------------
        # GET QUESTION
        # ----------------------------------------------------

        text = data.get("text")


        if not text or not isinstance(
            text,
            str
        ):

            return jsonify({
                "status": "error",
                "message": "text field is required"
            }), 400


        text = text.strip()


        if not text:

            return jsonify({
                "status": "error",
                "message": "text cannot be empty"
            }), 400


        # ----------------------------------------------------
        # ML PREDICTION
        # ----------------------------------------------------

        prediction = model.predict(
            [text]
        )[0]


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

            except Exception as error:

                print(
                    "Confidence warning:",
                    str(error)
                )

                confidence = 0.0


        # ----------------------------------------------------
        # FIND ANSWER
        # ----------------------------------------------------

        answer_result = get_best_answer(
            question=text,
            predicted_intent=prediction
        )


        # ----------------------------------------------------
        # RETURN RESPONSE
        # ----------------------------------------------------

        return jsonify({

            "status": "success",

            "question": text,

            "intent": str(
                prediction
            ),

            "confidence": round(
                confidence,
                4
            ),

            "answer": answer_result[
                "answer"
            ],

            "answer_similarity":
                answer_result[
                    "answer_similarity"
                ]
        })


    except Exception as error:

        print(
            "Prediction error:",
            str(error)
        )

        return jsonify({

            "status": "error",

            "message": "Prediction failed",

            "error": str(error)

        }), 500


# ============================================================
# START FLASK SERVER
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