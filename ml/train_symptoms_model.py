import json
import random
from pathlib import Path

import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression


# ============================================================
# PATHS
# ============================================================

BASE_DIR = Path(__file__).resolve().parent

DATASET_PATH = (
    BASE_DIR.parent
    / "app"
    / "src"
    / "main"
    / "assets"
    / "symptoms_dataset.json"
)

MODEL_DIR = BASE_DIR / "models"
MODEL_DIR.mkdir(parents=True, exist_ok=True)

OUTPUT_PATH = MODEL_DIR / "symptoms_model.json"


# ============================================================
# SETTINGS
# ============================================================

RANDOM_SEED = 42

# Number of synthetic combinations generated from each condition.
COMBINATIONS_PER_CONDITION = 250

# Maximum number of symptoms in one training example.
MAX_SYMPTOMS_PER_EXAMPLE = 6

random.seed(RANDOM_SEED)
np.random.seed(RANDOM_SEED)


# ============================================================
# LOAD DATASET
# ============================================================

def load_dataset():
    if not DATASET_PATH.exists():
        raise FileNotFoundError(
            f"\nDataset not found:\n{DATASET_PATH}\n\n"
            "Make sure symptoms_dataset.json is inside:\n"
            "app/src/main/assets/"
        )

    with open(DATASET_PATH, "r", encoding="utf-8") as file:
        data = json.load(file)

    conditions = data.get("conditions", [])

    if not conditions:
        raise ValueError("No conditions found in symptoms_dataset.json")

    return conditions


# ============================================================
# NORMALIZE SYMPTOMS
# ============================================================

def normalize_symptom(text):
    return " ".join(
        str(text)
        .strip()
        .lower()
        .split()
    )


def normalize_conditions(raw_conditions):
    conditions = []

    for item in raw_conditions:
        name = str(item.get("name", "")).strip()

        raw_symptoms = item.get("symptoms", [])

        symptoms = []

        for symptom in raw_symptoms:
            normalized = normalize_symptom(symptom)

            if normalized:
                symptoms.append(normalized)

        symptoms = list(dict.fromkeys(symptoms))

        if name and len(symptoms) >= 2:
            conditions.append(
                {
                    "name": name,
                    "symptoms": symptoms,
                }
            )

    return conditions


# ============================================================
# CREATE TRAINING EXAMPLES
# ============================================================

def create_training_examples(conditions):
    texts = []
    labels = []

    for condition in conditions:
        condition_name = condition["name"]
        symptoms = condition["symptoms"]

        # ----------------------------------------------------
        # Full symptom set
        # ----------------------------------------------------

        texts.append(" ".join(symptoms))
        labels.append(condition_name)

        # ----------------------------------------------------
        # Generate realistic symptom combinations
        # ----------------------------------------------------

        for _ in range(COMBINATIONS_PER_CONDITION):

            max_count = min(
                MAX_SYMPTOMS_PER_EXAMPLE,
                len(symptoms)
            )

            # Avoid single-symptom training examples.
            # A single common symptom should not strongly
            # predict a disease.
            min_count = 2

            if max_count < min_count:
                continue

            count = random.randint(min_count, max_count)

            selected = random.sample(
                symptoms,
                count
            )

            # Keep ordering deterministic enough for
            # reproducibility.
            selected = sorted(selected)

            texts.append(" ".join(selected))
            labels.append(condition_name)

    return texts, labels


# ============================================================
# TRAIN MODEL
# ============================================================

def train_model(texts, labels):
    print("\nTraining TF-IDF + Logistic Regression model...")

    vectorizer = TfidfVectorizer(
        analyzer="word",
        token_pattern=r"(?u)\b[\w-]+\b",
        ngram_range=(1, 2),
        lowercase=True,
        sublinear_tf=True,
    )

    X = vectorizer.fit_transform(texts)

    model = LogisticRegression(
        max_iter=3000,
        C=2.0,
        class_weight="balanced",
        random_state=RANDOM_SEED,
    )

    model.fit(X, labels)

    return vectorizer, model


# ============================================================
# EXPORT MODEL FOR ANDROID
# ============================================================

def export_model(vectorizer, model, conditions):
    print("\nExporting Android-compatible model...")

    vocabulary = vectorizer.vocabulary_

    # Convert vocabulary into ordered feature list.
    feature_names = [
        ""
    ] * len(vocabulary)

    for word, index in vocabulary.items():
        feature_names[index] = word

    idf = vectorizer.idf_.tolist()

    coefficients = model.coef_.tolist()

    classes = model.classes_.tolist()

    # LogisticRegression uses one coefficient row per class
    # for multiclass classification.
    #
    # In some sklearn versions, binary classification has
    # only one coefficient row. We handle that safely here.

    if len(coefficients) == 1 and len(classes) == 2:
        first = (-np.array(coefficients[0])).tolist()
        second = np.array(coefficients[0]).tolist()

        coefficients = [
            first,
            second,
        ]

    exported = {
        "model_type": "tfidf_logistic_regression",
        "version": 1,

        "classes": classes,

        "vocabulary": feature_names,

        "idf": idf,

        "coefficients": coefficients,

        "intercepts": model.intercept_.tolist(),

        "max_symptoms_per_input": MAX_SYMPTOMS_PER_EXAMPLE,

        "minimum_meaningful_symptoms": 2,

        "training_info": {
            "conditions": len(conditions),
            "training_examples": len(
                conditions
            ) * COMBINATIONS_PER_CONDITION
            + len(conditions),
            "random_seed": RANDOM_SEED,
        },
    }

    with open(
        OUTPUT_PATH,
        "w",
        encoding="utf-8"
    ) as file:
        json.dump(
            exported,
            file,
            ensure_ascii=False,
            separators=(",", ":"),
        )

    return exported


# ============================================================
# TEST MODEL
# ============================================================

def predict_sample(
    vectorizer,
    model,
    symptoms,
):
    text = " ".join(
        normalize_symptom(s)
        for s in symptoms
    )

    X = vectorizer.transform([text])

    probabilities = model.predict_proba(X)[0]

    indices = np.argsort(probabilities)[::-1][:5]

    results = []

    for index in indices:
        results.append(
            (
                model.classes_[index],
                float(probabilities[index]),
            )
        )

    return results


def run_tests(vectorizer, model):
    print("\n")
    print("=" * 60)
    print("MODEL TESTS")
    print("=" * 60)

    test_cases = [
        [
            "fatigue"
        ],
        [
            "fever",
            "chills",
            "sweating",
            "headache",
        ],
        [
            "runny nose",
            "sneezing",
            "sore throat",
            "cough",
        ],
        [
            "frequent urination",
            "excessive thirst",
            "increased hunger",
            "fatigue",
        ],
        [
            "itchy rash",
            "blisters",
            "fever",
        ],
        [
            "cough",
            "mucus",
            "shortness of breath",
            "chest pain",
        ],
    ]

    for symptoms in test_cases:
        print("\nSymptoms:")
        print(" + ".join(symptoms))

        results = predict_sample(
            vectorizer,
            model,
            symptoms,
        )

        print("\nTop predictions:")

        for condition, probability in results[:3]:
            print(
                f"  {condition:<30} "
                f"{probability * 100:.2f}%"
            )


# ============================================================
# MAIN
# ============================================================

def main():
    print("=" * 60)
    print("MEDASSIST AI - SYMPTOMS ML MODEL TRAINING")
    print("=" * 60)

    print("\nDataset:")
    print(DATASET_PATH)

    # --------------------------------------------------------
    # Load
    # --------------------------------------------------------

    raw_conditions = load_dataset()

    conditions = normalize_conditions(
        raw_conditions
    )

    print(
        f"\nConditions loaded: {len(conditions)}"
    )

    # --------------------------------------------------------
    # Training examples
    # --------------------------------------------------------

    texts, labels = create_training_examples(
        conditions
    )

    print(
        f"Training examples: {len(texts)}"
    )

    # --------------------------------------------------------
    # Train
    # --------------------------------------------------------

    vectorizer, model = train_model(
        texts,
        labels
    )

    print(
        f"Vocabulary size: "
        f"{len(vectorizer.vocabulary_)}"
    )

    print(
        f"Classes: {len(model.classes_)}"
    )

    # --------------------------------------------------------
    # Export
    # --------------------------------------------------------

    exported = export_model(
        vectorizer,
        model,
        conditions
    )

    print("\nModel exported successfully.")

    print(
        f"Model file:\n{OUTPUT_PATH}"
    )

    print(
        f"Model size: "
        f"{OUTPUT_PATH.stat().st_size / 1024 / 1024:.2f} MB"
    )

    # --------------------------------------------------------
    # Tests
    # --------------------------------------------------------

    run_tests(
        vectorizer,
        model
    )

    print("\n")
    print("=" * 60)
    print("TRAINING COMPLETED")
    print("=" * 60)
    print(
        "\nNext step:"
        "\nCopy symptoms_model.json into Android assets."
    )


if __name__ == "__main__":
    main()