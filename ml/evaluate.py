from pathlib import Path

import joblib
import pandas as pd

from sklearn.metrics import (
    accuracy_score,
    classification_report,
    confusion_matrix,
    f1_score,
    precision_score,
    recall_score
)
from sklearn.model_selection import train_test_split


BASE_DIR = Path(__file__).resolve().parent

DATA_FILE = (
    BASE_DIR
    / "data"
    / "raw"
    / "medical_intents.csv"
)

MODEL_FILE = (
    BASE_DIR
    / "models"
    / "medical_intent_model.joblib"
)


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

    df = df.dropna(
        subset=["text", "intent"]
    )

    df["text"] = (
        df["text"]
        .astype(str)
        .str.strip()
    )

    df["intent"] = (
        df["intent"]
        .astype(str)
        .str.strip()
    )

    df = df[
        (df["text"] != "")
        & (df["intent"] != "")
    ]

    df = df.drop_duplicates(
        subset=["text", "intent"]
    )

    return df


def main():

    print("=" * 60)
    print("MEDASSIST AI - TEST SET EVALUATION")
    print("=" * 60)

    if not MODEL_FILE.exists():
        raise FileNotFoundError(
            "Trained model not found.\n"
            "Please run train.py first."
        )

    df = load_dataset()

    print(
        f"\nTotal dataset records: {len(df)}"
    )

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

    # Use the exact same split configuration
    # used during training.
    X_train, X_test, y_train, y_test = train_test_split(
        X,
        y,
        test_size=0.25,
        random_state=42,
        stratify=y
    )

    print(
        f"Training records : {len(X_train)}"
    )

    print(
        f"Test records     : {len(X_test)}"
    )

    model = joblib.load(
        MODEL_FILE
    )

    predictions = model.predict(
        X_test
    )

    accuracy = accuracy_score(
        y_test,
        predictions
    )

    precision = precision_score(
        y_test,
        predictions,
        average="weighted",
        zero_division=0
    )

    recall = recall_score(
        y_test,
        predictions,
        average="weighted",
        zero_division=0
    )

    f1 = f1_score(
        y_test,
        predictions,
        average="weighted",
        zero_division=0
    )

    print("\n" + "=" * 60)
    print("TEST SET METRICS")
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

    print("\nClassification Report")
    print("-" * 40)

    print(
        classification_report(
            y_test,
            predictions,
            zero_division=0
        )
    )

    print("\nConfusion Matrix")
    print("-" * 40)

    print(
        confusion_matrix(
            y_test,
            predictions
        )
    )

    print("\nEvaluation completed successfully.")


if __name__ == "__main__":
    main()