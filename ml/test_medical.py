from pathlib import Path

import joblib


BASE_DIR = Path(__file__).resolve().parent

MODEL_FILE = (
    BASE_DIR
    / "models"
    / "medical_intent_model.joblib"
)


model = joblib.load(
    MODEL_FILE
)


questions = [
    "fever",
    "diabetes",
    "headache",
    "hemoglobin",
    "anemia",
    "cough",
    "asthma",
    "migraine",
    "dengue",
    "malaria",
    "blood pressure",
    "dehydration",
    "paracetamol",
    "ibuprofen",
    "nutrition",
    "healthy food",
    "how can I prevent diabetes",
    "what are symptoms of diabetes",
    "my hemoglobin is low",
    "I have severe chest pain",
    "I have a headache",
    "I have been coughing",
    "what is anemia",
    "what is paracetamol used for",
]


print("=" * 60)
print("MEDASSIST AI - MEDICAL MODEL REAL-WORLD TEST")
print("=" * 60)


for question in questions:

    prediction = model.predict(
        [question]
    )[0]

    probabilities = (
        model.predict_proba(
            [question]
        )[0]
    )

    confidence = (
        max(probabilities)
        * 100
    )

    print(
        f"\nQuestion : {question}"
    )

    print(
        f"Intent   : {prediction}"
    )

    print(
        f"Confidence: {confidence:.2f}%"
    )


print("\n" + "=" * 60)
print("TEST COMPLETED")
print("=" * 60)