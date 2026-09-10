import json
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parent

DATASET_FILE = (
    PROJECT_ROOT
    / "data"
    / "processed"
    / "medicines"
    / "medicines.json"
)


QUERIES = [
    "acetaminophen",
    "dolo",
    "crocin",
    "paracetamol",
]


def main():

    print("=" * 70)
    print("MEDASSIST AI - Common Medicine Verification")
    print("=" * 70)

    with DATASET_FILE.open(
        "r",
        encoding="utf-8"
    ) as file:
        data = json.load(file)

    medicines = data.get(
        "medicines",
        []
    )

    print()
    print(
        f"Total medicines: {len(medicines)}"
    )

    for query in QUERIES:

        query = query.lower()

        matches = []

        for medicine in medicines:

            searchable_text = " ".join([
                medicine.get(
                    "medicine_name",
                    ""
                ),
                medicine.get(
                    "generic_name",
                    ""
                ),
                medicine.get(
                    "brand_name",
                    ""
                ),
                medicine.get(
                    "search_text",
                    ""
                ),
            ]).lower()

            if query in searchable_text:
                matches.append(medicine)

        print()
        print("-" * 70)
        print(
            f"{query.upper()} -> {len(matches)} matches"
        )

        for medicine in matches[:10]:

            print(
                f"  Medicine : "
                f"{medicine.get('medicine_name', '')}"
            )

            print(
                f"  Generic  : "
                f"{medicine.get('generic_name', '')}"
            )

            print(
                f"  Brand    : "
                f"{medicine.get('brand_name', '')}"
            )

            print()

    print("=" * 70)
    print("VERIFICATION COMPLETE")
    print("=" * 70)


if __name__ == "__main__":
    main()