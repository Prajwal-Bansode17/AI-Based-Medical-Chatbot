from pathlib import Path

import joblib
import pandas as pd

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.metrics import (
    accuracy_score,
    classification_report,
    precision_score,
    recall_score,
    f1_score,
)


BASE_DIR = Path(__file__).resolve().parent

DATA_FILE = (
    BASE_DIR
    / "data"
    / "raw"
    / "medical_intents.csv"
)

MODEL_DIR = (
    BASE_DIR
    / "models"
)

MODEL_FILE = (
    MODEL_DIR
    / "medical_intent_model.joblib"
)


def load_data():

    if not DATA_FILE.exists():
        raise FileNotFoundError(
            f"Medical dataset not found:\n{DATA_FILE}"
        )

    df = pd.read_csv(DATA_FILE)

    required_columns = {
        "intent",
        "question",
    }

    missing = (
        required_columns
        - set(df.columns)
    )

    if missing:
        raise ValueError(
            f"Missing columns: {sorted(missing)}"
        )

    df = df[
        [
            "intent",
            "question",
        ]
    ].copy()

    df = df.dropna(
        subset=[
            "intent",
            "question",
        ]
    )

    df["intent"] = (
        df["intent"]
        .astype(str)
        .str.strip()
        .str.lower()
    )

    df["question"] = (
        df["question"]
        .astype(str)
        .str.strip()
    )

    df = df[
        (df["intent"] != "")
        &
        (df["question"] != "")
    ]

    df = df.drop_duplicates(
        subset=["question"]
    )

    return df


def build_model():

    return Pipeline(
        steps=[
            (
                "tfidf",
                TfidfVectorizer(
                    lowercase=True,
                    strip_accents="unicode",
                    ngram_range=(1, 2),
                    min_df=1,
                    max_df=1.0,
                    sublinear_tf=True,
                ),
            ),
            (
                "classifier",
                LogisticRegression(
                    max_iter=2000,
                    class_weight="balanced",
                ),
            ),
        ]
    )


def main():

    print("=" * 60)
    print(
        "MEDASSIST AI - MEDICAL INTENT TRAINING"
    )
    print("=" * 60)

    df = load_data()

    print(
        f"\nTotal medical examples: {len(df)}"
    )

    print(
        f"Total medical intents: "
        f"{df['intent'].nunique()}"
    )

    print("\nIntent distribution:")
    print(
        df["intent"].value_counts()
    )

    X = df["question"]
    y = df["intent"]

    model = build_model()

    print(
        "\nTraining medical model..."
    )

    model.fit(
        X,
        y
    )

    print(
        "Medical model training completed."
    )

    predictions = model.predict(
        X
    )

    accuracy = accuracy_score(
        y,
        predictions
    )

    precision = precision_score(
        y,
        predictions,
        average="weighted",
        zero_division=0,
    )

    recall = recall_score(
        y,
        predictions,
        average="weighted",
        zero_division=0,
    )

    f1 = f1_score(
        y,
        predictions,
        average="weighted",
        zero_division=0,
    )

    print("\n" + "=" * 60)
    print("MEDICAL MODEL METRICS")
    print("=" * 60)

    print(
        f"\nAccuracy : {accuracy:.4f}"
    )

    print(
        f"Precision: {precision:.4f}"
    )

    print(
        f"Recall   : {recall:.4f}"
    )

    print(
        f"F1 Score : {f1:.4f}"
    )

    print(
        "\nClassification Report:"
    )

    print("-" * 60)

    print(
        classification_report(
            y,
            predictions,
            zero_division=0,
        )
    )

    MODEL_DIR.mkdir(
        parents=True,
        exist_ok=True
    )

    joblib.dump(
        model,
        MODEL_FILE
    )

    print("=" * 60)
    print(
        "MEDICAL MODEL SAVED SUCCESSFULLY"
    )
    print("=" * 60)

    print(
        f"\nModel file:"
    )

    print(
        MODEL_FILE
    )

    print(
        "\nTraining completed successfully."
    )


if __name__ == "__main__":
    main()