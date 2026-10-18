from pathlib import Path
import re

p = Path("ml/api.py")
s = p.read_text(encoding="utf-8")

start = s.find("def extract_symptoms(text):")
end = s.find("\n\nMEDICAL_KEYWORDS", start)

if start == -1 or end == -1:
    raise SystemExit("ERROR: Function boundary not found. No changes made.")

new_function = '''def extract_symptoms(text):
    normalized = normalize_text(text)

    roman_marathi_normalized = {
        "tap aala": "fever", "tap ala": "fever",
        "taap aala": "fever", "taap ala": "fever",
        "mala tap": "fever", "mala taap": "fever",
        "doka dukhat": "headache", "dok dukhat": "headache",
        "pot dukhat": "stomach pain", "pathi dukhat": "back pain",
        "ang dukhat": "body pain", "ulti": "vomiting",
        "mala malmal": "nausea", "chakkar": "dizziness",
        "garagar": "dizziness", "khokla": "cough", "sardi": "cold",
        "kamjori": "weakness", "ashaktpana": "weakness",
    }

    for phrase, canonical in roman_marathi_normalized.items():
        if re.search(r"(?<!\\\\w)" + re.escape(phrase) + r"(?!\\\\w)", normalized):
            normalized += " " + canonical

    detected = []

    for symptom, aliases in SYMPTOM_ALIASES.items():
        for alias in aliases:
            alias_normalized = normalize_text(alias)

            # Ignore aliases corrupted by encoding or normalization.
            if not alias_normalized:
                continue

            pattern = r"(?<!\\\\w)" + re.escape(alias_normalized) + r"(?!\\\\w)"
            if re.search(pattern, normalized):
                if symptom not in detected:
                    detected.append(symptom)
                break

    return detected
'''

s = s[:start] + new_function + s[end:]
p.write_text(s, encoding="utf-8")
print("Updated extract_symptoms with safe word-boundary matching.")
