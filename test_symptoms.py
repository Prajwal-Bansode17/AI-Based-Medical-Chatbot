import requests

tests = [
    "Mala taap ani dokedukhi aahe.",
    "माझं डोकं दुखतं आणि ताप आहे.",
    "I have headache and fever.",
    "Mujhe sir dard aur bukhar hai."
]

for i, q in enumerate(tests):
    try:
        r = requests.post(
            "http://127.0.0.1:5000/predict",
            json={"text": q, "session_id": f"symptom-test-{i}"},
            timeout=60
        )
        print("\nINPUT:", q)
        print("STATUS:", r.status_code)
        print("RESULT:", r.text[:500])
    except Exception as e:
        print("ERROR:", e)
