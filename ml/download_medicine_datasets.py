from pathlib import Path
from urllib.request import urlopen
from zipfile import ZipFile
import shutil


PROJECT_ROOT = Path(__file__).resolve().parent

RAW_DIR = (
    PROJECT_ROOT
    / "data"
    / "raw"
    / "medicines"
)

DOWNLOAD_DIR = RAW_DIR / "downloads"
EXTRACT_DIR = RAW_DIR / "all_datasets"


BASE_URL = "https://download.open.fda.gov/drug/label/"

TOTAL_FILES = 14


def download_file(url, destination):

    print()
    print(f"Downloading:")
    print(f"  {url}")

    with urlopen(url) as response:
        with open(destination, "wb") as file:
            shutil.copyfileobj(response, file)

    print(f"Saved:")
    print(f"  {destination}")


def main():

    print("=" * 70)
    print("MEDASSIST AI - Download Complete Medicine Dataset")
    print("=" * 70)

    DOWNLOAD_DIR.mkdir(
        parents=True,
        exist_ok=True
    )

    EXTRACT_DIR.mkdir(
        parents=True,
        exist_ok=True
    )

    for number in range(1, TOTAL_FILES + 1):

        filename = (
            f"drug-label-{number:04d}-of-{TOTAL_FILES:04d}.json.zip"
        )

        zip_path = DOWNLOAD_DIR / filename

        extract_folder = (
            EXTRACT_DIR
            / f"dataset-{number:04d}"
        )

        # ----------------------------------------------------
        # Download
        # ----------------------------------------------------

        if zip_path.exists():

            print()
            print(f"[{number}/{TOTAL_FILES}] Already downloaded:")
            print(filename)

        else:

            url = BASE_URL + filename

            print()
            print(
                f"[{number}/{TOTAL_FILES}] Starting download..."
            )

            download_file(
                url,
                zip_path
            )

        # ----------------------------------------------------
        # Extract
        # ----------------------------------------------------

        extract_folder.mkdir(
            parents=True,
            exist_ok=True
        )

        json_filename = filename.replace(
            ".zip",
            ""
        )

        json_path = (
            extract_folder
            / json_filename
        )

        if json_path.exists():

            print(
                f"Already extracted: {json_filename}"
            )

        else:

            print(
                f"Extracting: {filename}"
            )

            with ZipFile(
                zip_path,
                "r"
            ) as zip_file:

                zip_file.extractall(
                    extract_folder
                )

            print(
                f"Extracted to: {extract_folder}"
            )

    print()
    print("=" * 70)
    print("DOWNLOAD + EXTRACTION COMPLETE")
    print("=" * 70)

    print()
    print(f"Download directory:")
    print(DOWNLOAD_DIR)

    print()
    print(f"Extracted datasets:")
    print(EXTRACT_DIR)

    print()
    print(
        "All 14 openFDA Drug Labeling files are ready."
    )

    print("=" * 70)


if __name__ == "__main__":
    main()