from pathlib import Path

import joblib
import pandas as pd

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, classification_report
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline


BASE_DIR = Path(__file__).resolve().parent

DATA_FILE = BASE_DIR / "data" / "raw" / "medical_intents.csv"
MODEL_DIR = BASE_DIR / "models"
MODEL_FILE = MODEL_DIR / "medical_intent_model.joblib"


def load_dataset():
    if not DATA_FILE.exists():
        raise FileNotFoundError(
            f"Dataset not found:\n{DATA_FILE}"
        )

    df = pd.read_csv(DATA_FILE)

    required_columns = {"text", "intent"}

    if not required_columns.issubset(df.columns):
        raise ValueError(
            "CSV must contain these columns: text, intent"
        )

    df = df.dropna(subset=["text", "intent"])

    df["text"] = df["text"].astype(str).str.strip()
    df["intent"] = df["intent"].astype(str).str.strip()

    df = df[
        (df["text"] != "")
        & (df["intent"] != "")
    ]

    df = df.drop_duplicates(
        subset=["text", "intent"]
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
                    ngram_range=(1, 2)
                )
            ),
            (
                "classifier",
                LogisticRegression(
                    max_iter=1000,
                    class_weight="balanced"
                )
            )
        ]
    )


def main():
    print("=" * 60)
    print("MEDASSIST AI - MEDICAL NLP MODEL")
    print("=" * 60)

    df = load_dataset()

    print(f"\nTotal records: {len(df)}")
    print(f"Total intents: {df['intent'].nunique()}")

    print("\nIntent distribution:")
    print(df["intent"].value_counts())

    X = df["text"]
    y = df["intent"]

    if y.nunique() < 2:
        raise ValueError(
            "At least two different intents are required."
        )

    minimum_class_count = y.value_counts().min()

    if minimum_class_count < 2:
        raise ValueError(
            "Each intent must contain at least 2 records."
        )

    X_train, X_test, y_train, y_test = train_test_split(
        X,
        y,
        test_size=0.25,
        random_state=42,
        stratify=y
    )

    print(f"\nTraining records: {len(X_train)}")
    print(f"Testing records: {len(X_test)}")

    model = build_model()

    print("\nTraining model...")

    model.fit(
        X_train,
        y_train
    )

    print("Training completed.")

    predictions = model.predict(X_test)

    accuracy = accuracy_score(
        y_test,
        predictions
    )

    print("\n" + "=" * 60)
    print("MODEL EVALUATION")
    print("=" * 60)

    print(f"\nAccuracy: {accuracy:.4f}")

    print("\nClassification Report:")

    print(
        classification_report(
            y_test,
            predictions,
            zero_division=0
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

    print("\n" + "=" * 60)
    print("MODEL SAVED SUCCESSFULLY")
    print("=" * 60)

    print(f"\nModel path:")
    print(MODEL_FILE)

    print("\nTraining completed successfully.")


if __name__ == "__main__":
    main()