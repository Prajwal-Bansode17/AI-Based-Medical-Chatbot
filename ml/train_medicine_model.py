import json
import pickle
import re
from pathlib import Path

from sklearn.feature_extraction.text import TfidfVectorizer


# ============================================================
# MEDASSIST AI - Improved Medicine Search Model
# ============================================================
# Medicine-only search model
#
# Features:
#   1. Exact name matching
#   2. Generic name matching
#   3. Brand name matching
#   4. Active ingredient matching
#   5. Manufacturer matching
#   6. TF-IDF n-gram search
#   7. Fuzzy-friendly character n-grams
#
# Flask/API is NOT required for this model.
# ============================================================


PROJECT_ROOT = Path(__file__).resolve().parent

INPUT_FILE = (
    PROJECT_ROOT
    / "data"
    / "processed"
    / "medicines"
    / "medicines.json"
)

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

SEARCH_INDEX_FILE = (
    MODEL_DIR
    / "medicine_search_index.pkl"
)


# ------------------------------------------------------------
# Text normalization
# ------------------------------------------------------------

def normalize_text(value):

    if value is None:
        return ""

    if isinstance(value, list):

        value = " ".join(
            str(item)
            for item in value
        )

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


# ------------------------------------------------------------
# Build searchable document
# ------------------------------------------------------------

def build_document(medicine):

    medicine_name = medicine.get(
        "medicine_name",
        ""
    )

    generic_name = medicine.get(
        "generic_name",
        ""
    )

    brand_name = medicine.get(
        "brand_name",
        ""
    )

    manufacturer = medicine.get(
        "manufacturer",
        ""
    )

    ingredients = medicine.get(
        "active_ingredients",
        []
    )

    if not isinstance(
        ingredients,
        list
    ):
        ingredients = [ingredients]

    ingredient_text = " ".join(
        str(item)
        for item in ingredients
    )

    # Give names multiple occurrences.
    # This increases their importance in TF-IDF.
    parts = [
        medicine_name,
        medicine_name,
        generic_name,
        generic_name,
        brand_name,
        brand_name,
        ingredient_text,
        manufacturer,
    ]

    return normalize_text(
        " ".join(
            part
            for part in parts
            if part
        )
    )


# ------------------------------------------------------------
# Build search metadata
# ------------------------------------------------------------

def build_search_index(medicines):

    search_index = []

    for index, medicine in enumerate(
        medicines
    ):

        name = normalize_text(
            medicine.get(
                "medicine_name",
                ""
            )
        )

        generic = normalize_text(
            medicine.get(
                "generic_name",
                ""
            )
        )

        brand = normalize_text(
            medicine.get(
                "brand_name",
                ""
            )
        )

        ingredients = medicine.get(
            "active_ingredients",
            []
        )

        if not isinstance(
            ingredients,
            list
        ):
            ingredients = [ingredients]

        normalized_ingredients = [
            normalize_text(item)
            for item in ingredients
            if item
        ]

        search_index.append(
            {
                "index": index,
                "medicine_name": name,
                "generic_name": generic,
                "brand_name": brand,
                "active_ingredients":
                    normalized_ingredients,
            }
        )

    return search_index


# ------------------------------------------------------------
# Main training
# ------------------------------------------------------------

def main():

    print("=" * 70)
    print("MEDASSIST AI - Improved Medicine Search Model")
    print("=" * 70)

    print()
    print(
        f"Input dataset:"
    )
    print(INPUT_FILE)

    print()
    print(
        f"Model directory:"
    )
    print(MODEL_DIR)

    if not INPUT_FILE.exists():

        print()
        print(
            "ERROR: Medicine dataset not found."
        )

        return

    MODEL_DIR.mkdir(
        parents=True,
        exist_ok=True
    )

    # --------------------------------------------------------
    # Load dataset
    # --------------------------------------------------------

    print()
    print(
        "Loading medicine dataset..."
    )

    with INPUT_FILE.open(
        "r",
        encoding="utf-8"
    ) as file:

        data = json.load(file)

    medicines = data.get(
        "medicines",
        []
    )

    if not medicines:

        print(
            "ERROR: No medicine records found."
        )

        return

    print(
        f"Medicines loaded: {len(medicines)}"
    )

    # --------------------------------------------------------
    # Build documents
    # --------------------------------------------------------

    print()
    print(
        "Building medicine search documents..."
    )

    documents = [
        build_document(medicine)
        for medicine in medicines
    ]

    # --------------------------------------------------------
    # TF-IDF
    # --------------------------------------------------------

    print()
    print(
        "Training TF-IDF search model..."
    )

    vectorizer = TfidfVectorizer(
        lowercase=True,
        analyzer="word",
        strip_accents="unicode",
        ngram_range=(1, 3),
        sublinear_tf=True,
        max_features=100000,
    )

    tfidf_matrix = vectorizer.fit_transform(
        documents
    )

    print(
        f"TF-IDF matrix shape: "
        f"{tfidf_matrix.shape}"
    )

    print(
        f"Vocabulary size: "
        f"{len(vectorizer.vocabulary_)}"
    )

    # --------------------------------------------------------
    # Search index
    # --------------------------------------------------------

    print()
    print(
        "Building medicine search index..."
    )

    search_index = build_search_index(
        medicines
    )

    # --------------------------------------------------------
    # Save vectorizer
    # --------------------------------------------------------

    print()
    print(
        "Saving vectorizer..."
    )

    with VECTORIZER_FILE.open(
        "wb"
    ) as file:

        pickle.dump(
            vectorizer,
            file,
            protocol=pickle.HIGHEST_PROTOCOL
        )

    # --------------------------------------------------------
    # Save matrix
    # --------------------------------------------------------

    print(
        "Saving TF-IDF matrix..."
    )

    with MATRIX_FILE.open(
        "wb"
    ) as file:

        pickle.dump(
            tfidf_matrix,
            file,
            protocol=pickle.HIGHEST_PROTOCOL
        )

    # --------------------------------------------------------
    # Save records
    # --------------------------------------------------------

    print(
        "Saving medicine records..."
    )

    model_records = {
        "dataset_name":
            "MEDASSIST AI Medicine Search Dataset",

        "source":
            data.get(
                "source",
                "openFDA Drug Labeling"
            ),

        "record_count":
            len(medicines),

        "medicines":
            medicines,
    }

    with RECORDS_FILE.open(
        "w",
        encoding="utf-8"
    ) as file:

        json.dump(
            model_records,
            file,
            ensure_ascii=False,
            separators=(
                ",",
                ":"
            )
        )

    # --------------------------------------------------------
    # Save search index
    # --------------------------------------------------------

    print(
        "Saving search index..."
    )

    with SEARCH_INDEX_FILE.open(
        "wb"
    ) as file:

        pickle.dump(
            search_index,
            file,
            protocol=pickle.HIGHEST_PROTOCOL
        )

    # --------------------------------------------------------
    # Final
    # --------------------------------------------------------

    print()
    print("=" * 70)
    print("IMPROVED MODEL TRAINING COMPLETE")
    print("=" * 70)

    print()
    print(
        f"Medicines indexed: {len(medicines)}"
    )

    print()
    print(
        "Created files:"
    )

    print(
        f"1. {VECTORIZER_FILE.name}"
    )

    print(
        f"2. {MATRIX_FILE.name}"
    )

    print(
        f"3. {RECORDS_FILE.name}"
    )

    print(
        f"4. {SEARCH_INDEX_FILE.name}"
    )

    print()
    print(
        "The improved medicine search model is ready."
    )

    print("=" * 70)


if __name__ == "__main__":
    main()