"""
Audit-only logic tests for ml/api.py – no Flask server required.
Does NOT start Flask; imports helper functions directly.
"""
import sys, re, os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(BASE_DIR))

# ----------------------------------------------------------------
# Minimal environment so api.py doesn't crash on missing Ollama
# ----------------------------------------------------------------
os.environ.setdefault("OLLAMA_ENABLED", "0")   # disable Ollama for unit tests
os.environ.setdefault("GEMINI_API_KEY", "")    # ensure Gemini also skipped

# We only need the functions, not the Flask app startup entrypoint.
import importlib.util
spec = importlib.util.spec_from_file_location("api_module", BASE_DIR / "api.py")
api = importlib.util.module_from_spec(spec)
spec.loader.exec_module(api)

PASS = 0
FAIL = 0

def check(label, got, expected):
    global PASS, FAIL
    if got == expected:
        print(f"  PASS  {label}")
        PASS += 1
    else:
        print(f"  FAIL  {label}  |  expected={expected!r}  got={got!r}")
        FAIL += 1


# ================================================================
# 1. LANGUAGE DETECTION
# ================================================================
print("\n=== Language Detection ===")
lang_tests = [
    ("hi",                      "en"),
    ("hello",                   "en"),
    ("good morning",            "en"),
    ("I have fever",            "en"),
    ("mala tap aahe",           "mr"),
    ("mujhe bukhar hai",        "hi"),
    ("मला ताप आहे",            "mr"),
    ("मुझे बुखार है",           "hi"),
    ("majha doka dukhat",       "mr"),
    # Ambiguous short words that should NOT flip to mr/hi
    ("sir",                     "en"),  # "sir" is in roman_hindi_words → could be wrong
    ("the",                     "en"),
    ("pot",                     "en"),  # pot is in NORMALIZE_REPLACEMENTS; should stay en
]
for text, expected in lang_tests:
    got = api.detect_language(text)
    check(f'detect_language("{text}")', got, expected)


# ================================================================
# 2. SYMPTOM EXTRACTION – false-positive risk
# ================================================================
print("\n=== Symptom Extraction (false-positive check) ===")
symptom_neg_tests = [
    "Hi",
    "hello",
    "What is Python?",
    "Who is a doctor?",
    "What should I eat?",
    "What is the weather today?",
]
for text in symptom_neg_tests:
    got = api.extract_symptoms(text)
    check(f'extract_symptoms("{text}") == []', got, [])

print("\n=== Symptom Extraction (true-positive check) ===")
symptom_pos_tests = [
    ("I have fever",        ["fever"]),
    ("I have a headache",   ["headache"]),
    ("mala tap aahe",       ["fever"]),
    ("mujhe bukhar hai",    ["fever"]),
    ("khokla aahe",         ["cough"]),
]
for text, expected in symptom_pos_tests:
    got = api.extract_symptoms(text)
    check(f'extract_symptoms("{text}")', sorted(got), sorted(expected))


# ================================================================
# 3. is_medical_query – should not fire on greetings
# ================================================================
print("\n=== is_medical_query (should be False for greetings) ===")
non_medical = ["Hi", "hello", "good morning", "how are you", "what is Python"]
for text in non_medical:
    got = api.is_medical_query(text)
    check(f'is_medical_query("{text}") == False', got, False)


# ================================================================
# ================================================================
# 4. REGEX BUGS – word-boundary & negation pattern check
# ================================================================
print("\n=== Regex word-boundary & negation pattern check ===")
# "fever" should match
check("extract_symptoms('I have fever') has 'fever'", "fever" in api.extract_symptoms("I have fever"), True)
# "feverish" should NOT match (word boundary blocks it)
check("extract_symptoms('feverish') == []", api.extract_symptoms("feverish"), [])
check("extract_symptoms('feverish person') == []", api.extract_symptoms("feverish person"), [])
# Negations should NOT trigger false positive symptoms
check("extract_symptoms('not fever') == []", api.extract_symptoms("not fever"), [])
check("extract_symptoms('I do not have fever') == []", api.extract_symptoms("I do not have fever"), [])
check("extract_symptoms('not headache') == []", api.extract_symptoms("not headache"), [])


# ================================================================
# 5. DEFAULT SESSION ID – cross-session contamination risk
# ================================================================
print("\n=== Default session_id 'default_user' risk ===")
# Test client endpoints for session isolation
test_client = api.app.test_client()
res_no_sid_1 = test_client.post("/predict", json={"text": "I have headache"})
sid_1 = res_no_sid_1.get_json().get("session_id")
res_no_sid_2 = test_client.post("/predict", json={"text": "I have fever"})
sid_2 = res_no_sid_2.get_json().get("session_id")

check("Missing session_id does not default to 'default_user'", sid_1 != "default_user", True)
check("Missing session_id generates unique UUID per client", sid_1 != sid_2, True)
check("Whitespace session_id generates unique UUID", test_client.post("/predict", json={"text": "hello", "session_id": "   "}).get_json().get("session_id") != "default_user", True)
check("GET /chat-history requires session_id", test_client.get("/chat-history").status_code, 400)
check("DELETE /chat-history requires session_id", test_client.delete("/chat-history").status_code, 400)
check("GET /history/<empty> returns 400", test_client.get("/history/   ").status_code, 400)
check("DELETE /history/<empty> returns 400", test_client.delete("/history/   ").status_code, 400)


# ================================================================
# 6. is_followup_query – false positives
# ================================================================
print("\n=== is_followup_query false-positive risk ===")
followup_fp = [
    "What is the weather for today?",   # 'today' is in FOLLOWUP_PATTERNS
    "What treatment is used for fever?",  # 'treatment' is in FOLLOWUP_PATTERNS
    "What remedy works for colds?",       # 'remedy' is in FOLLOWUP_PATTERNS
    "The symptoms for this month",        # 'month' is in FOLLOWUP_PATTERNS
]
for text in followup_fp:
    got = api.is_followup_query(text)
    # None of these should trigger follow-up logic
    check(f'is_followup_query("{text}") == False', got, False)


# ================================================================
# SUMMARY
# ================================================================
print(f"\n{'='*50}")
print(f"RESULTS: {PASS} passed, {FAIL} failed")
print(f"{'='*50}")
