from pathlib import Path

import joblib
from flask import Flask, jsonify, request


BASE_DIR = Path(__file__).resolve().parent
MODEL_FILE = BASE_DIR / "models" / "medical_intent_model.joblib"

app = Flask(__name__)

model = joblib.load(MODEL_FILE)


@app.get("/")
def home():
    return jsonify({
        "status": "success",
        "service": "MEDASSIST AI Medical NLP API",
        "message": "API is running"
    })


@app.get("/health")
def health():
    return jsonify({
        "status": "healthy",
        "model_loaded": True
    })


@app.post("/predict")
def predict():

    data = request.get_json(silent=True)

    if not data:
        return jsonify({
            "status": "error",
            "message": "Request body is required"
        }), 400

    text = data.get("text")

    if not text or not isinstance(text, str):
        return jsonify({
            "status": "error",
            "message": "text field is required"
        }), 400

    text = text.strip()

    if not text:
        return jsonify({
            "status": "error",
            "message": "text cannot be empty"
        }), 400

    prediction = model.predict([text])[0]

    confidence = 0.0

    if hasattr(model, "predict_proba"):
        probabilities = model.predict_proba([text])[0]
        confidence = float(max(probabilities))

    return jsonify({
        "status": "success",
        "question": text,
        "intent": prediction,
        "confidence": round(confidence, 4)
    })


if __name__ == "__main__":
    app.run(
        host="0.0.0.0",
        port=5000,
        debug=False
    )