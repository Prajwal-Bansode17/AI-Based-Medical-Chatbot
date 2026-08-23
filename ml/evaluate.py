from pathlib import Path

import joblib
import pandas as pd

from sklearn.metrics import (
    accuracy_score,
    classification_report,
    confusion_matrix,
    precision_score,
    recall_score,
    f1_score,
)


BASE_DIR = Path(__file__).resolve().parent

TEST_FILE = BASE_DIR / "data" / "processed" / "test.csv"
MODEL_FILE = BASE_DIR / "models" / "medical_intent_model.joblib"


def main():

    print("=" * 60)
    print("MEDASSIST AI - TEST SET EVALUATION")
    print("=" * 60)

    if not TEST_FILE.exists():
        raise FileNotFoundError(
            f"Test dataset not found:\n{TEST_FILE}"
        )

    if not MODEL_FILE.exists():
        raise FileNotFoundError(
            f"Model not found:\n{MODEL_FILE}"
        )

    test_df = pd.read_csv(TEST_FILE)

    test_df = test_df.dropna(
        subset=["question", "category"]
    )

    X_test = (
        test_df["question"]
        .astype(str)
        .str.strip()
    )

    y_test = (
        test_df["category"]
        .astype(str)
        .str.strip()
    )

    model = joblib.load(MODEL_FILE)

    predictions = model.predict(X_test)

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

    print("\nDataset")
    print("-" * 40)

    print(
        f"Total test records: {len(test_df)}"
    )

    print(
        f"Total categories: {y_test.nunique()}"
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
            zero_division=0,
        )
    )

    labels = sorted(
        set(y_test) | set(predictions)
    )

    print("\nConfusion Matrix")
    print("-" * 40)

    print(
        confusion_matrix(
            y_test,
            predictions,
            labels=labels,
        )
    )

    print("\nLabels:")
    print(labels)

    print("\nEvaluation completed successfully.")


if __name__ == "__main__":
    main()