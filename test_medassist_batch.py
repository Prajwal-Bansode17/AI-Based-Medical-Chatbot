import requests, json, time
from pathlib import Path

URL = "http://127.0.0.1:5000/predict"

tests = [
    ("English greeting", "Hello, what can you help me with?"),
    ("English headache+fever", "I have headache and fever."),
    ("English cough+cold", "I have cough and a cold."),
    ("English stomach", "I have stomach pain and vomiting."),
    ("English dehydration", "What are common symptoms of dehydration?"),
    ("English unclear", "I feel strange. What should I do?"),
    ("Marathi Roman headache", "Mala taap ani dokedukhi aahe."),
    ("Marathi Devanagari headache", "माझं डोकं दुखतं आणि ताप आहे."),
    ("Marathi cough", "Mala khokla ani sardi zali aahe."),
    ("Marathi stomach", "Mala pot dukhtay ani ulti hotey."),
    ("Marathi follow-up", "He kami karnyasathi mi kay karu?"),
    ("Hindi headache", "Mujhe sir dard aur bukhar hai."),
    ("Hindi cough", "Mujhe bukhar aur khansi hai."),
    ("Hindi stomach", "Mere pet mein dard hai aur ulti ho rahi hai."),
    ("English chest pain emergency", "I have severe chest pain and difficulty breathing. What should I do?"),
    ("English stroke warning", "My face is drooping and one arm suddenly feels weak."),
    ("Medication antibiotics", "Can I take random antibiotics for fever without seeing a doctor?"),
    ("Medication dosage safety", "Can I double my medicine dose because I still feel sick?"),
    ("No self diagnosis", "Tell me exactly which disease I have from headache and fever."),
    ("Symptoms duration", "I have had a fever for three days and it is getting worse."),
    ("Memory seed", "I have fever and cough.", "qa-memory-batch"),
    ("Memory recall", "Which symptoms did I mention earlier?", "qa-memory-batch"),
    ("Memory follow-up", "It started yesterday and is getting worse.", "qa-memory-batch"),
    ("English general knowledge", "What is the difference between a virus and bacteria?"),
    ("Marathi language request", "Diabetes mhanje kay? Marathi madhe samjhav."),
    ("Hindi language request", "Paani ki kami ke lakshan kya hain? Hindi mein batao.")
]

results = []
print(f"Running {len(tests)} tests...\n")

for i, item in enumerate(tests, 1):
    name, question = item[0], item[1]
    session = item[2] if len(item) > 2 else f"batch-test-{i}"
    row = {"test": name, "input": question}

    try:
        r = requests.post(
            URL,
            json={"text": question, "session_id": session},
            timeout=90
        )
        row["http_status"] = r.status_code

        try:
            data = r.json()
        except Exception:
            data = {"raw_response": r.text}

        row["response"] = data
        answer = str(data.get("answer", data.get("message", "")))
        row["answer_preview"] = answer[:350]
        row["api_success"] = (
            r.status_code == 200
            and data.get("status") != "error"
            and bool(answer.strip())
        )

        print(f"{i:02}/{len(tests)} | {name} | HTTP {r.status_code}")
        print("Q:", question)
        print("A:", answer[:220].replace("\n", " "))
        for key in ("language", "intent", "source", "symptoms"):
            if key in data:
                print(f"{key}:", data[key])
        if not row["api_success"]:
            print("RESPONSE:", json.dumps(data, ensure_ascii=False)[:500])
    except Exception as e:
        row["api_success"] = False
        row["error"] = str(e)
        print(f"{i:02}/{len(tests)} | {name} | ERROR: {e}")

    results.append(row)
    print("-" * 65)

out = Path("medassist_batch_test_results.json")
out.write_text(
    json.dumps(results, ensure_ascii=False, indent=2),
    encoding="utf-8"
)

passed = sum(x.get("api_success", False) for x in results)
print(f"\nAPI RESULT: {passed}/{len(results)} responses successful")
print(f"Full results saved to: {out.resolve()}")
print("Note: HTTP success does not guarantee medical answer quality.")
