from pathlib import Path
import sys

import joblib


BASE_DIR = Path(__file__).resolve().parent

MODEL_FILE = (
    BASE_DIR
    / "models"
    / "medical_intent_model.joblib"
)


def load_model():
    if not MODEL_FILE.exists():
        raise FileNotFoundError(
            "Trained model not found. "
            "Please run train.py first."
        )

    return joblib.load(MODEL_FILE)


def predict_intent(text):
    model = load_model()

    prediction = model.predict([text])[0]

    confidence = None

    if hasattr(model, "predict_proba"):
        probabilities = model.predict_proba([text])[0]
        confidence = float(max(probabilities))

    return prediction, confidence


def main():

    if len(sys.argv) < 2:
        print(
            'Usage: python predict.py "your question"'
        )
        return

    text = " ".join(
        sys.argv[1:]
    ).strip()

    if not text:
        print("Please enter a question.")
        return

    intent, confidence = predict_intent(
        text
    )

    print("\nMEDASSIST AI PREDICTION")
    print("-" * 40)

    print(
        f"Question  : {text}"
    )

    print(
        f"Intent    : {intent}"
    )

    if confidence is not None:
        print(
            f"Confidence: {confidence:.4f}"
        )


if __name__ == "__main__":
    main()