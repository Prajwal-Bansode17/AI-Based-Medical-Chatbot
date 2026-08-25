"""MEDIQ Dataset Loader — quick helper to load the dataset into pandas."""

import json
import os
import pandas as pd


def load_mediq(path=None, format="csv"):
    """Load MEDIQ dataset as a pandas DataFrame.

    Args:
        path: Path to the MEDIQ repo root. Defaults to parent of scripts/.
        format: 'csv' or 'json'. Default 'csv'.

    Returns:
        pandas.DataFrame with columns:
            id, question, answer, timestamp, consent,
            consentTimestamp, language_detected
    """
    if path is None:
        path = os.path.join(os.path.dirname(__file__), "..")

    if format == "csv":
        csv_path = os.path.join(path, "data", "mediq_full.csv")
        return pd.read_csv(csv_path, parse_dates=["timestamp", "consentTimestamp"])

    json_path = os.path.join(path, "data", "mediq_full.json")
    with open(json_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    rows = []
    for date_str, entries in data.get("data", {}).items():
        for entry in entries:
            entry["date"] = date_str
            rows.append(entry)

    df = pd.DataFrame(rows)
    df["timestamp"] = pd.to_datetime(df["timestamp"])
    if "consentTimestamp" in df.columns:
        df["consentTimestamp"] = pd.to_datetime(df["consentTimestamp"])
    return df


if __name__ == "__main__":
    df = load_mediq()
    print(f"Loaded {len(df)} entries")
    print(f"\nHeuristic language distribution:")
    print(df["language_detected"].value_counts())
    if "ai_language" in df.columns:
        print(f"\nAI language distribution (corrected):")
        print(df["ai_language"].value_counts())
        print(f"\nAI intent distribution:")
        print(df["ai_intent"].value_counts())
        print(f"\nMedical relevance:")
        print(df["ai_medical_relevance"].value_counts())
        print(f"\nUser types:")
        print(df["ai_user_type_guess"].value_counts())
    print(f"\nDate range: {df['timestamp'].min()} to {df['timestamp'].max()}")
    print(f"\nSample entry:")
    print(df.iloc[0].to_dict())
