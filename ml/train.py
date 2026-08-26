from pathlib import Path

import joblib
import pandas as pd

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.model_selection import train_test_split
from sklearn.metrics import (
    accuracy_score,
    classification_report,
    precision_score,
    recall_score,
    f1_score,
)


# ============================================================
# PATHS
# ============================================================

BASE_DIR = Path(__file__).resolve().parent

DATA_FILE = (
    BASE_DIR
    / "data"
    / "raw"
    / "medical_intents.csv"
)

SPLIT_DIR = (
    BASE_DIR
    / "data"
    / "processed"
    / "intent_split"
)

MODEL_DIR = (
    BASE_DIR
    / "models"
)

MODEL_FILE = (
    MODEL_DIR
    / "medical_intent_model.joblib"
)


# ============================================================
# LOAD DATA
# ============================================================

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
        subset=["intent", "question"]
    )

    df.reset_index(
        drop=True,
        inplace=True
    )

    return df


# ============================================================
# BUILD MODEL
# ============================================================

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


# ============================================================
# SAVE SPLITS
# ============================================================

def save_splits(
    train_df,
    validation_df,
    test_df
):

    SPLIT_DIR.mkdir(
        parents=True,
        exist_ok=True
    )

    train_df.to_csv(
        SPLIT_DIR / "train.csv",
        index=False
    )

    validation_df.to_csv(
        SPLIT_DIR / "validation.csv",
        index=False
    )

    test_df.to_csv(
        SPLIT_DIR / "test.csv",
        index=False
    )


# ============================================================
# MAIN
# ============================================================

def main():

    print("=" * 70)
    print(
        "MEDASSIST AI - MEDICAL INTENT MODEL TRAINING"
    )
    print("=" * 70)

    # --------------------------------------------------------
    # LOAD
    # --------------------------------------------------------

    df = load_data()

    print(
        f"\nTotal examples: {len(df)}"
    )

    print(
        f"Total intents: {df['intent'].nunique()}"
    )

    print("\nIntent distribution:")
    print(
        df["intent"].value_counts()
    )

    # --------------------------------------------------------
    # STRATIFIED SPLIT
    # --------------------------------------------------------

    train_df, temp_df = train_test_split(
        df,
        test_size=0.20,
        stratify=df["intent"],
        random_state=42,
    )

    validation_df, test_df = train_test_split(
        temp_df,
        test_size=0.50,
        stratify=temp_df["intent"],
        random_state=42,
    )

    print("\n" + "=" * 70)
    print("DATASET SPLIT")
    print("=" * 70)

    print(
        f"\nTrain      : {len(train_df)}"
    )

    print(
        f"Validation : {len(validation_df)}"
    )

    print(
        f"Test       : {len(test_df)}"
    )

    # --------------------------------------------------------
    # SAVE SPLITS
    # --------------------------------------------------------

    save_splits(
        train_df,
        validation_df,
        test_df
    )

    print(
        "\nSplit files saved successfully."
    )

    # --------------------------------------------------------
    # BUILD MODEL
    # --------------------------------------------------------

    model = build_model()

    print(
        "\nTraining model..."
    )

    # --------------------------------------------------------
    # TRAIN
    # --------------------------------------------------------

    model.fit(
        train_df["question"],
        train_df["intent"]
    )

    print(
        "Model training completed."
    )

    # --------------------------------------------------------
    # VALIDATION
    # --------------------------------------------------------

    validation_predictions = model.predict(
        validation_df["question"]
    )

    validation_accuracy = accuracy_score(
        validation_df["intent"],
        validation_predictions
    )

    # --------------------------------------------------------
    # TEST
    # --------------------------------------------------------

    test_predictions = model.predict(
        test_df["question"]
    )

    test_accuracy = accuracy_score(
        test_df["intent"],
        test_predictions
    )

    test_precision = precision_score(
        test_df["intent"],
        test_predictions,
        average="weighted",
        zero_division=0,
    )

    test_recall = recall_score(
        test_df["intent"],
        test_predictions,
        average="weighted",
        zero_division=0,
    )

    test_f1 = f1_score(
        test_df["intent"],
        test_predictions,
        average="weighted",
        zero_division=0,
    )

    # --------------------------------------------------------
    # RESULTS
    # --------------------------------------------------------

    print("\n" + "=" * 70)
    print("MODEL EVALUATION")
    print("=" * 70)

    print(
        f"\nValidation Accuracy : "
        f"{validation_accuracy:.4f}"
    )

    print(
        f"Test Accuracy       : "
        f"{test_accuracy:.4f}"
    )

    print(
        f"Test Precision      : "
        f"{test_precision:.4f}"
    )

    print(
        f"Test Recall         : "
        f"{test_recall:.4f}"
    )

    print(
        f"Test F1 Score       : "
        f"{test_f1:.4f}"
    )

    print(
        "\nClassification Report:"
    )

    print("-" * 70)

    print(
        classification_report(
            test_df["intent"],
            test_predictions,
            zero_division=0,
        )
    )

    # --------------------------------------------------------
    # SAVE FINAL MODEL
    # --------------------------------------------------------

    MODEL_DIR.mkdir(
        parents=True,
        exist_ok=True
    )

    joblib.dump(
        model,
        MODEL_FILE
    )

    print("=" * 70)
    print(
        "FINAL MEDICAL MODEL SAVED SUCCESSFULLY"
    )
    print("=" * 70)

    print(
        f"\nModel file:"
    )

    print(
        MODEL_FILE
    )

    print(
        "\nTraining pipeline completed successfully."
    )


if __name__ == "__main__":
    main()