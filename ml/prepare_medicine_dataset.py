import json
import re
from pathlib import Path


# ============================================================
# MEDASSIST AI - Complete Medicine Dataset Preparation
# ============================================================
# Source:
#   openFDA Drug Labeling
#
# Input:
#   ml/data/raw/medicines/all_datasets/dataset-0001 ... 0014
#
# Output:
#   ml/data/processed/medicines/medicines.json
#
# IMPORTANT:
#   Medicine/drug data ONLY.
#   No MEDIQ or patient dataset is used.
# ============================================================


PROJECT_ROOT = Path(__file__).resolve().parent

INPUT_DIR = (
    PROJECT_ROOT
    / "data"
    / "raw"
    / "medicines"
    / "all_datasets"
)

OUTPUT_DIR = (
    PROJECT_ROOT
    / "data"
    / "processed"
    / "medicines"
)

OUTPUT_FILE = OUTPUT_DIR / "medicines.json"

MAX_TEXT_LENGTH = 5000


# ------------------------------------------------------------
# Text helpers
# ------------------------------------------------------------

def clean_text(value):

    if value is None:
        return ""

    if isinstance(value, list):
        value = " ".join(
            str(item)
            for item in value
        )

    value = str(value)

    value = re.sub(
        r"<[^>]+>",
        " ",
        value
    )

    value = re.sub(
        r"\s+",
        " ",
        value
    )

    return value.strip()


def truncate_text(value):

    value = clean_text(value)

    if len(value) <= MAX_TEXT_LENGTH:
        return value

    return (
        value[:MAX_TEXT_LENGTH]
        .rstrip()
        + "..."
    )


def get_first(openfda, key):

    value = openfda.get(
        key,
        []
    )

    if isinstance(value, list):

        for item in value:

            item = clean_text(item)

            if item:
                return item

        return ""

    return clean_text(value)


def get_all(openfda, key):

    value = openfda.get(
        key,
        []
    )

    if not isinstance(value, list):
        value = [value]

    result = []

    for item in value:

        item = clean_text(item)

        if item and item not in result:
            result.append(item)

    return result


def get_section(record, key):

    return truncate_text(
        record.get(
            key,
            ""
        )
    )


def normalize_for_search(value):

    value = clean_text(value).lower()

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
# Medicine record creation
# ------------------------------------------------------------

def create_medicine_record(record):

    openfda = record.get(
        "openfda",
        {}
    )

    if not isinstance(openfda, dict):
        openfda = {}

    generic_name = get_first(
        openfda,
        "generic_name"
    )

    brand_name = get_first(
        openfda,
        "brand_name"
    )

    manufacturer = get_first(
        openfda,
        "manufacturer_name"
    )

    active_ingredients = get_all(
        openfda,
        "substance_name"
    )

    drug_type = get_first(
        openfda,
        "product_type"
    )

    route = get_first(
        openfda,
        "route"
    )

    # Prefer brand name.
    # If unavailable, use generic name.
    # If both unavailable, use active ingredient.
    medicine_name = (
        brand_name
        or generic_name
        or (
            active_ingredients[0]
            if active_ingredients
            else ""
        )
    )

    if not medicine_name:
        return None

    # --------------------------------------------------------
    # Search aliases
    # --------------------------------------------------------

    search_names = []

    for value in [
        medicine_name,
        generic_name,
        brand_name,
        manufacturer,
    ]:

        if value:

            normalized = normalize_for_search(
                value
            )

            if (
                normalized
                and normalized not in search_names
            ):
                search_names.append(
                    normalized
                )

    for ingredient in active_ingredients:

        normalized = normalize_for_search(
            ingredient
        )

        if (
            normalized
            and normalized not in search_names
        ):
            search_names.append(
                normalized
            )

    search_text = " ".join(
        search_names
    )

    # --------------------------------------------------------
    # Final medicine object
    # --------------------------------------------------------

    return {
        "medicine_name": medicine_name,

        "generic_name": generic_name,

        "brand_name": brand_name,

        "manufacturer": manufacturer,

        "active_ingredients": active_ingredients,

        "drug_type": drug_type,

        "route": route,

        "uses": get_section(
            record,
            "indications_and_usage"
        ),

        "how_it_works": get_section(
            record,
            "clinical_pharmacology"
        ),

        "dosage_information": get_section(
            record,
            "dosage_and_administration"
        ),

        "side_effects": get_section(
            record,
            "adverse_reactions"
        ),

        "contraindications": get_section(
            record,
            "contraindications"
        ),

        "warnings": get_section(
            record,
            "warnings"
        ),

        "precautions": get_section(
            record,
            "precautions"
        ),

        "drug_interactions": get_section(
            record,
            "drug_interactions"
        ),

        "pregnancy_information": get_section(
            record,
            "pregnancy"
        ),

        "pediatric_use": get_section(
            record,
            "pediatric_use"
        ),

        "geriatric_use": get_section(
            record,
            "geriatric_use"
        ),

        "overdose_information": get_section(
            record,
            "overdosage"
        ),

        "search_text": search_text,
    }


# ------------------------------------------------------------
# Duplicate key
# ------------------------------------------------------------

def medicine_key(medicine):

    generic = normalize_for_search(
        medicine.get(
            "generic_name",
            ""
        )
    )

    brand = normalize_for_search(
        medicine.get(
            "brand_name",
            ""
        )
    )

    ingredients = normalize_for_search(
        " ".join(
            medicine.get(
                "active_ingredients",
                []
            )
        )
    )

    # Use generic + ingredients where possible.
    if generic and ingredients:
        return (
            f"{generic}|{ingredients}"
        )

    if generic:
        return generic

    if brand:
        return brand

    if ingredients:
        return ingredients

    return ""


# ------------------------------------------------------------
# Process one JSON file
# ------------------------------------------------------------

def process_file(
    json_file,
    medicines,
    seen
):

    print()
    print(
        f"Processing: {json_file.name}"
    )

    try:

        with json_file.open(
            "r",
            encoding="utf-8"
        ) as file:

            data = json.load(file)

    except Exception as error:

        print(
            f"ERROR reading {json_file.name}:"
        )

        print(error)

        return 0, 0, 0

    records = data.get(
        "results",
        []
    )

    total = len(records)

    added = 0
    duplicates = 0
    skipped = 0

    print(
        f"Raw records: {total}"
    )

    for index, record in enumerate(
        records,
        start=1
    ):

        medicine = create_medicine_record(
            record
        )

        if medicine is None:

            skipped += 1
            continue

        key = medicine_key(
            medicine
        )

        if not key:

            skipped += 1
            continue

        if key in seen:

            duplicates += 1
            continue

        seen.add(key)

        medicines.append(
            medicine
        )

        added += 1

        if index % 5000 == 0:

            print(
                f"  Processed "
                f"{index}/{total}"
            )

    print(
        f"Added: {added}"
    )

    print(
        f"Duplicates: {duplicates}"
    )

    print(
        f"Skipped: {skipped}"
    )

    return added, duplicates, skipped


# ------------------------------------------------------------
# Main
# ------------------------------------------------------------

def main():

    print("=" * 70)
    print(
        "MEDASSIST AI - Complete Medicine Dataset Preparation"
    )
    print("=" * 70)

    print()
    print(
        f"Input directory:"
    )
    print(INPUT_DIR)

    print()
    print(
        f"Output file:"
    )
    print(OUTPUT_FILE)

    if not INPUT_DIR.exists():

        print()
        print(
            "ERROR: Dataset directory not found."
        )

        return

    OUTPUT_DIR.mkdir(
        parents=True,
        exist_ok=True
    )

    # --------------------------------------------------------
    # Find all 14 JSON files
    # --------------------------------------------------------

    json_files = sorted(
        INPUT_DIR.glob(
            "dataset-*/drug-label-*.json"
        )
    )

    print()
    print(
        f"Dataset files found: "
        f"{len(json_files)}"
    )

    if not json_files:

        print(
            "ERROR: No JSON datasets found."
        )

        return

    # --------------------------------------------------------
    # Process all datasets
    # --------------------------------------------------------

    medicines = []

    seen = set()

    total_raw = 0
    total_duplicates = 0
    total_skipped = 0

    for json_file in json_files:

        added, duplicates, skipped = (
            process_file(
                json_file,
                medicines,
                seen
            )
        )

        # Get raw count again for statistics
        try:

            with json_file.open(
                "r",
                encoding="utf-8"
            ) as file:

                data = json.load(file)

            total_raw += len(
                data.get(
                    "results",
                    []
                )
            )

        except Exception:
            pass

        total_duplicates += duplicates
        total_skipped += skipped

    # --------------------------------------------------------
    # Sort medicines
    # --------------------------------------------------------

    medicines.sort(
        key=lambda item:
        normalize_for_search(
            item.get(
                "medicine_name",
                ""
            )
        )
    )

    # --------------------------------------------------------
    # Save final dataset
    # --------------------------------------------------------

    output_data = {
        "dataset_name":
            "MEDASSIST AI Medicine Dataset",

        "source":
            "openFDA Drug Labeling",

        "record_count":
            len(medicines),

        "medicines":
            medicines,
    }

    print()
    print(
        "Saving complete medicine dataset..."
    )

    with OUTPUT_FILE.open(
        "w",
        encoding="utf-8"
    ) as file:

        json.dump(
            output_data,
            file,
            ensure_ascii=False,
            separators=(
                ",",
                ":"
            )
        )

    # --------------------------------------------------------
    # Final statistics
    # --------------------------------------------------------

    print()
    print("=" * 70)
    print(
        "COMPLETE MEDICINE DATASET READY"
    )
    print("=" * 70)

    print()
    print(
        f"Dataset files processed: "
        f"{len(json_files)}"
    )

    print(
        f"Raw records: "
        f"{total_raw}"
    )

    print(
        f"Unique medicines: "
        f"{len(medicines)}"
    )

    print(
        f"Duplicates removed: "
        f"{total_duplicates}"
    )

    print(
        f"Records skipped: "
        f"{total_skipped}"
    )

    print()
    print(
        f"Output:"
    )

    print(
        OUTPUT_FILE
    )

    print()
    print(
        "Medicine-only dataset preparation complete."
    )

    print("=" * 70)


if __name__ == "__main__":
    main()