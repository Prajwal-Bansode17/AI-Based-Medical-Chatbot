import json
import pickle
import re
from pathlib import Path

from sklearn.metrics.pairwise import cosine_similarity


PROJECT_ROOT = Path(__file__).resolve().parent

MODEL_DIR = (
    PROJECT_ROOT
    / "models"
    / "medicine"
)

VECTORIZER_FILE = (
    MODEL_DIR
    / "medicine_tfidf_vectorizer.pkl"
)

MATRIX_FILE = (
    MODEL_DIR
    / "medicine_tfidf_matrix.pkl"
)

RECORDS_FILE = (
    MODEL_DIR
    / "medicine_records.json"
)


def normalize_text(value):

    value = str(value).lower()

    value = re.sub(
        r"[^a-z0-9\s]",
        " ",
        value
    )

    value = re.sub(
        r"\s+",
        " ",
        value
    )

    return value.strip()


def main():

    print("=" * 70)
    print("MEDASSIST AI - Medicine Search Test")
    print("=" * 70)

    print()
    print("Loading trained model...")

    with VECTORIZER_FILE.open("rb") as file:
        vectorizer = pickle.load(file)

    with MATRIX_FILE.open("rb") as file:
        tfidf_matrix = pickle.load(file)

    with RECORDS_FILE.open(
        "r",
        encoding="utf-8"
    ) as file:
        data = json.load(file)

    medicines = data["medicines"]

    print(
        f"Medicines loaded: {len(medicines)}"
    )

    queries = [
        "paracetamol",
        "acetaminophen",
        "crocin",
        "dolo",
        "cetirizine",
        "ibuprofen",
        "ors",
        "oral rehydration",
        "antacid",
        "omeprazole",
        "amoxicillin",
        "azithromycin",
        "miconazole",
        "cough syrup",
        "pain killer",
    ]

    print()
    print("=" * 70)
    print("SEARCH RESULTS")
    print("=" * 70)

    for query in queries:

        normalized_query = normalize_text(
            query
        )

        query_vector = vectorizer.transform(
            [normalized_query]
        )

        scores = cosine_similarity(
            query_vector,
            tfidf_matrix
        ).flatten()

        top_indices = scores.argsort()[
            -5:
        ][::-1]

        print()
        print(
            f"QUERY: {query}"
        )

        found = False

        for index in top_indices:

            score = float(
                scores[index]
            )

            if score <= 0:
                continue

            medicine = medicines[index]

            name = medicine.get(
                "medicine_name",
                ""
            )

            generic = medicine.get(
                "generic_name",
                ""
            )

            print(
                f"  {name}"
            )

            print(
                f"    Generic : {generic}"
            )

            print(
                f"    Score   : {score:.4f}"
            )

            found = True

        if not found:

            print(
                "  No matching medicine found."
            )

    print()
    print("=" * 70)
    print("SEARCH TEST COMPLETE")
    print("=" * 70)


if __name__ == "__main__":
    main()