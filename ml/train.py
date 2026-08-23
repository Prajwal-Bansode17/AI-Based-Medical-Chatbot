from pathlib import Path

import joblib
import pandas as pd

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    accuracy_score,
    classification_report,
    precision_score,
    recall_score,
    f1_score,
)
from sklearn.pipeline import Pipeline


BASE_DIR = Path(__file__).resolve().parent

TRAIN_FILE = BASE_DIR / "data" / "processed" / "train.csv"
TEST_FILE = BASE_DIR / "data" / "processed" / "test.csv"

MODEL_DIR = BASE_DIR / "models"
MODEL_FILE = MODEL_DIR / "medical_intent_model.joblib"


def load_data():

    if not TRAIN_FILE.exists():
        raise FileNotFoundError(
            f"Training file not found:\n{TRAIN_FILE}"
        )

    if not TEST_FILE.exists():
        raise FileNotFoundError(
            f"Testing file not found:\n{TEST_FILE}"
        )

    train_df = pd.read_csv(TRAIN_FILE)
    test_df = pd.read_csv(TEST_FILE)

    required_columns = {"question", "category"}

    if not required_columns.issubset(train_df.columns):
        raise ValueError(
            "Training CSV must contain: question, category"
        )

    if not required_columns.issubset(test_df.columns):
        raise ValueError(
            "Testing CSV must contain: question, category"
        )

    train_df = train_df.dropna(
        subset=["question", "category"]
    )

    test_df = test_df.dropna(
        subset=["question", "category"]
    )

    train_df["question"] = (
        train_df["question"]
        .astype(str)
        .str.strip()
    )

    test_df["question"] = (
        test_df["question"]
        .astype(str)
        .str.strip()
    )

    train_df["category"] = (
        train_df["category"]
        .astype(str)
        .str.strip()
    )

    test_df["category"] = (
        test_df["category"]
        .astype(str)
        .str.strip()
    )

    return train_df, test_df


def build_model():

    return Pipeline(
        steps=[
            (
                "tfidf",
                TfidfVectorizer(
                    lowercase=True,
                    strip_accents="unicode",
                    ngram_range=(1, 2),
                    min_df=2,
                    max_df=0.95,
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
    print("MEDASSIST AI - LARGE DATASET TRAINING")
    print("=" * 60)

    train_df, test_df = load_data()

    X_train = train_df["question"]
    y_train = train_df["category"]

    X_test = test_df["question"]
    y_test = test_df["category"]

    print(f"\nTraining records : {len(train_df)}")
    print(f"Testing records  : {len(test_df)}")
    print(f"Training classes : {y_train.nunique()}")

    print("\nTraining distribution:")
    print(y_train.value_counts())

    model = build_model()

    print("\nTraining model...")

    model.fit(
        X_train,
        y_train
    )

    print("Training completed.")

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
        zero_division=0,
    )

    recall = recall_score(
        y_test,
        predictions,
        average="weighted",
        zero_division=0,
    )

    f1 = f1_score(
        y_test,
        predictions,
        average="weighted",
        zero_division=0,
    )

    print("\n" + "=" * 60)
    print("MODEL EVALUATION")
    print("=" * 60)

    print(f"\nAccuracy : {accuracy:.4f}")
    print(f"Precision: {precision:.4f}")
    print(f"Recall   : {recall:.4f}")
    print(f"F1 Score : {f1:.4f}")

    print("\nClassification Report:")
    print("-" * 60)

    print(
        classification_report(
            y_test,
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
    print("MODEL SAVED SUCCESSFULLY")
    print("=" * 60)

    print(f"\nModel:")
    print(MODEL_FILE)

    print("\nTraining completed successfully.")


if __name__ == "__main__":
    main()