from pathlib import Path

import pandas as pd
from sklearn.model_selection import train_test_split


BASE_DIR = Path(__file__).resolve().parent

SOURCE_FILE = (
    BASE_DIR
    / "data"
    / "raw"
    / "mediq"
    / "ProxyAyush-MEDIQ-00091b1"
    / "data"
    / "mediq_full.csv"
)

PROCESSED_DIR = BASE_DIR / "data" / "processed"

TRAIN_FILE = PROCESSED_DIR / "train.csv"
TEST_FILE = PROCESSED_DIR / "test.csv"

MIN_TEXT_LENGTH = 5


def clean_text(value):
    if pd.isna(value):
        return ""

    value = str(value)
    value = " ".join(value.split())
    return value.strip()


def load_dataset():
    if not SOURCE_FILE.exists():
        raise FileNotFoundError(
            f"Dataset not found:\n{SOURCE_FILE}"
        )

    df = pd.read_csv(SOURCE_FILE)

    required_columns = {
        "question",
        "category",
        "ai_intent",
        "ai_medical_relevance",
    }

    missing = required_columns - set(df.columns)

    if missing:
        raise ValueError(
            f"Missing columns: {sorted(missing)}"
        )

    return df


def prepare_dataset(df):

    # Keep only useful columns
    df = df[
        [
            "question",
            "category",
            "ai_intent",
            "ai_medical_relevance",
        ]
    ].copy()

    # Clean question text
    df["question"] = df["question"].apply(clean_text)

    # Remove empty questions
    df = df[
        df["question"].str.len() >= MIN_TEXT_LENGTH
    ]

    # Remove duplicate questions
    df = df.drop_duplicates(
        subset=["question"]
    )

    # Keep medically relevant records
    df = df[
        df["ai_medical_relevance"]
        .astype(str)
        .str.lower()
        .isin(
            [
                "high",
                "medium",
            ]
        )
    ]

    # Remove rows without category
    df = df.dropna(
        subset=["category"]
    )

    df["category"] = (
        df["category"]
        .astype(str)
        .str.strip()
        .str.lower()
    )

    # Remove empty categories
    df = df[
        df["category"] != ""
    ]

    return df


def create_splits(df):

    # Categories with only one example cannot be
    # safely stratified, so remove them.
    category_counts = (
        df["category"]
        .value_counts()
    )

    valid_categories = category_counts[
        category_counts >= 2
    ].index

    df = df[
        df["category"].isin(
            valid_categories
        )
    ].copy()

    train_df, test_df = train_test_split(
        df,
        test_size=0.20,
        random_state=42,
        stratify=df["category"],
    )

    return train_df, test_df


def main():

    print("=" * 60)
    print("MEDASSIST AI - MEDIQ DATA PREPROCESSING")
    print("=" * 60)

    df = load_dataset()

    print(
        f"\nOriginal records: {len(df)}"
    )

    df = prepare_dataset(df)

    print(
        f"Cleaned records: {len(df)}"
    )

    print(
        f"Unique categories: "
        f"{df['category'].nunique()}"
    )

    print("\nTop categories:")
    print(
        df["category"]
        .value_counts()
        .head(20)
    )

    train_df, test_df = create_splits(
        df
    )

    PROCESSED_DIR.mkdir(
        parents=True,
        exist_ok=True
    )

    train_df.to_csv(
        TRAIN_FILE,
        index=False
    )

    test_df.to_csv(
        TEST_FILE,
        index=False
    )

    print("\n" + "=" * 60)
    print("PREPROCESSING COMPLETED")
    print("=" * 60)

    print(
        f"\nTraining records: "
        f"{len(train_df)}"
    )

    print(
        f"Testing records: "
        f"{len(test_df)}"
    )

    print(
        f"\nTraining file:\n{TRAIN_FILE}"
    )

    print(
        f"\nTesting file:\n{TEST_FILE}"
    )


if __name__ == "__main__":
    main()