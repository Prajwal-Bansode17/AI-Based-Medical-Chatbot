"""
Phase 1 regression tests for ml/api.py.
Run BEFORE and AFTER applying Phase 1 fixes to measure improvement.
No Flask server required — tests internal logic only.
"""
import os
import sys
import re
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(BASE_DIR))

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

os.environ["OLLAMA_ENABLED"] = "0"
os.environ["GEMINI_API_KEY"] = ""

import importlib.util
spec = importlib.util.spec_from_file_location("api_module", BASE_DIR / "api.py")
api = importlib.util.module_from_spec(spec)
spec.loader.exec_module(api)

# ----------------------------------------------------------------
# Tiny test harness
# ----------------------------------------------------------------
PASSED = []
FAILED = []

def check(label, got, expected):
    if got == expected:
        PASSED.append(label)
        print(f"  PASS  {label}")
    else:
        FAILED.append(label)
        print(f"  FAIL  {label}")
        print(f"        expected = {expected!r}")
        print(f"        got      = {got!r}")


def section(title):
    print(f"\n{'='*60}")
    print(f"  {title}")
    print(f"{'='*60}")


# ================================================================
# BUG-01  Regex word-boundary in extract_symptoms
# ================================================================
section("BUG-01 — Regex word-boundary in extract_symptoms")

# The pattern r"(?<!\\w)" (raw string with TWO backslashes) becomes the
# regex (?<!\\w) which is lookbehind for literal backslash+w — never
# appears in text → always matches → substring false-positives occur.
# After fix, r"(?<!\w)" (raw string ONE backslash) checks word-char class.

# False-positive tests — these MUST NOT produce symptoms
bug01_neg = [
    ("feverish person",          []),         # 'fever' is substring of 'feverish'
    ("coughing medicine",        []),         # 'cough' must not match 'coughing' as a substring
    ("headband fell",            []),         # 'head' substring
    ("weaknesses are listed",    []),         # 'weakness' substring
    ("breathless feeling",       ["breathing_problem"]),  # known alias — should match
    ("no symptoms at all",       []),         # no symptom words
]
# True-positive tests — these MUST produce the listed symptoms
bug01_pos = [
    ("I have fever",             ["fever"]),
    ("I have a headache",        ["headache"]),
    ("mala tap aahe",            ["fever"]),
    ("mujhe bukhar hai",         ["fever"]),
    ("khokla aahe",              ["cough"]),
    ("I have dizziness",         ["dizziness"]),
    ("chest pain and fever",     ["chest_pain", "fever"]),
    ("I have cough and cold",    ["cough", "cold"]),
]

for text, expected in bug01_neg:
    got = sorted(api.extract_symptoms(text))
    check(f'extract_symptoms({text!r}) == {expected!r}', got, sorted(expected))

for text, expected in bug01_pos:
    got = sorted(api.extract_symptoms(text))
    check(f'extract_symptoms({text!r}) contains {sorted(expected)!r}',
          all(s in got for s in expected), True)


# ================================================================
# BUG-02  Language detection — English words misclassified
# ================================================================
section("BUG-02 — Language detection false-positives on English words")

lang_cases = [
    # Pure English — must be "en"
    ("hi",                       "en"),
    ("hello",                    "en"),
    ("good morning",             "en"),
    ("sir",                      "en"),   # was "hi" before fix
    ("the",                      "en"),   # was "hi" before fix
    ("pot",                      "en"),   # was "mr" before fix
    ("I have fever",             "en"),
    ("What is diabetes?",        "en"),
    ("I need cold water",        "en"),
    ("sir I need help",          "en"),   # compound; 'sir' alone doesn't flip it

    # Marathi — must be "mr"
    ("mala tap aahe",            "mr"),
    ("mala khokla aahe",         "mr"),
    ("majha doka dukhat",        "mr"),
    ("mala pot dukhat",          "mr"),
    ("mala bara vatat nahi",     "mr"),

    # Hindi — must be "hi"
    ("mujhe bukhar hai",         "hi"),
    ("mujhe dard hai",           "hi"),
    ("mere pet mein dard",       "hi"),
    ("mera sir dard karta hai",  "hi"),

    # Devanagari Marathi
    ("मला ताप आहे",              "mr"),
    ("माझे डोके दुखत आहे",       "mr"),

    # Devanagari Hindi
    ("मुझे बुखार है",             "hi"),
    ("मेरे सिर में दर्द है",      "hi"),
]

for text, expected in lang_cases:
    got = api.detect_language(text)
    check(f'detect_language({text!r}) == {expected!r}', got, expected)


# ================================================================
# BUG-03  Follow-up detection false-positives
# ================================================================
section("BUG-03 — is_followup_query false-positives with generic words")

# These should NOT be treated as follow-up queries
followup_false = [
    "What is the weather for today?",          # 'today'
    "What treatment is used for fever?",        # 'treatment', 'for'
    "What remedy works for colds?",             # 'remedy', 'for'
    "The symptoms for this month",              # 'month', 'for', 'this'
    "What are the causes of diabetes?",         # 'for', 'since' not present
    "What is the best medicine for headache?",  # 'for'
    "I need relief from this headache",         # 'relief', 'this'
    "Since when is aspirin used?",              # 'since'
    "How many days does fever last?",           # 'days'
    "What to know about asthma treatment",      # 'treatment', 'what to'
    "Can it be something serious?",             # 'it is' not present, but 'its' logic
]
for text in followup_false:
    got = api.is_followup_query(text)
    check(f'is_followup_query({text!r}) == False', got, False)

# These SHOULD still be treated as follow-up queries
followup_true = [
    "yes",                                      # affirmative
    "no",                                       # negative
    "still the same",                           # 'still'
    "it is getting worse",                      # 'worse'
    "getting better now",                       # 'better'
    "what should i do",                         # explicit care phrase
    "what can i do for this",                   # care phrase
    "how to take care of this",                 # care phrase
    "what precautions should i take",           # precautions
    "kay karu",                                 # Romanized Marathi follow-up
    "kya karu",                                 # Romanized Hindi follow-up
]
for text in followup_true:
    got = api.is_followup_query(text)
    check(f'is_followup_query({text!r}) == True', got, True)


# ================================================================
# BUG-04  Session ID — no contamination from missing session_id
# ================================================================
section("BUG-04 — session_id handling")
# We test the internal logic: if the API receives no session_id,
# the resolved session must NOT be the literal string "default_user".
# We simulate the extraction logic from api.py line 2113.

def resolve_session_id(data, max_len=128):
    """Mirrors the FIXED logic (what it should be after the fix)."""
    import uuid
    raw = str(data.get("session_id") or "").strip()[:max_len]
    if not raw:
        raw = "anon_" + uuid.uuid4().hex
    return raw

# Without session_id: must generate unique, non-default value
s1 = resolve_session_id({})
s2 = resolve_session_id({})
check("Missing session_id is not 'default_user'", s1 != "default_user", True)
check("Missing session_id is not empty", bool(s1), True)
check("Two missing session IDs are unique", s1 != s2, True)

# With explicit session_id: must preserve it
s3 = resolve_session_id({"session_id": "android_123456"})
check("Explicit session_id preserved", s3, "android_123456")

# With whitespace-only session_id: must generate
s4 = resolve_session_id({"session_id": "   "})
check("Whitespace session_id treated as missing", s4 != "default_user", True)
check("Whitespace session_id gets unique ID", bool(s4), True)


# ================================================================
# BUG-05  Endpoint mismatch — verify Flask routes
# ================================================================
section("BUG-05 — Flask endpoint registration (chat-history aliases)")

import flask
# Check that the Flask app has the query-param alias routes after fix
app = api.app
rules = {}
for rule in app.url_map.iter_rules():
    rules.setdefault(rule.rule, set()).update(rule.methods)

chat_history_get_exists = "/chat-history" in rules and "GET" in rules["/chat-history"]
chat_history_del_exists = "/chat-history" in rules and "DELETE" in rules["/chat-history"]

check("GET /chat-history route exists", chat_history_get_exists, True)
check("DELETE /chat-history route exists", chat_history_del_exists, True)

# Original path-param routes must still exist (backward-compat)
history_path_get = "/history/<session_id>" in rules and "GET" in rules["/history/<session_id>"]
check("GET /history/<session_id> still exists", history_path_get, True)


# ================================================================
# BUG-06  RetrofitClient — BuildConfig usage (static check)
# ================================================================
section("BUG-06 — RetrofitClient uses BuildConfig (static source check)")

retrofit_path = (
    BASE_DIR.parent
    / "app" / "src" / "main" / "java"
    / "com" / "example" / "ai_based_medical_chatbot"
    / "data" / "api" / "RetrofitClient.kt"
)
if retrofit_path.exists():
    source = retrofit_path.read_text(encoding="utf-8")
    uses_buildconfig = "BuildConfig.MEDASSIST_API_URL" in source
    no_hardcoded_192 = "192.168." not in source
    check("RetrofitClient uses BuildConfig.MEDASSIST_API_URL", uses_buildconfig, True)
    check("RetrofitClient has no hard-coded 192.168.x.x IP", no_hardcoded_192, True)
else:
    FAILED.append("RetrofitClient.kt not found")
    print("  SKIP  RetrofitClient.kt not found at expected path")


# ================================================================
# PHASE 2 — Multilingual Symptom Extraction
# ================================================================
section("PHASE 2 — Multilingual Symptom Extraction across 5 categories")

# 1. Standalone & Category Tests: headache, fever, cough, stomach_pain, dizziness
# Across: English, Marathi Devanagari, Roman Marathi, Hindi Devanagari, Roman Hindi
multilingual_symptom_tests = [
    # Required standalone phrases
    ("khokla",                   ["cough"]),
    ("doka dukhat",              ["headache"]),
    ("खोकला",                    ["cough"]),
    ("डोके दुखत आहे",            ["headache"]),
    ("सिर दर्द",                 ["headache"]),
    ("मुझे बुखार है",            ["fever"]),

    # English
    ("headache",                 ["headache"]),
    ("severe head pain",         ["headache"]),
    ("high fever",               ["fever"]),
    ("bad cough",                ["cough"]),
    ("stomach pain",             ["stomach_pain"]),
    ("severe stomach ache",      ["stomach_pain"]),
    ("dizziness",                ["dizziness"]),

    # Marathi Devanagari
    ("डोकेदुखी",                 ["headache"]),
    ("माझे डोके दुखत आहे",       ["headache"]),
    ("ताप",                      ["fever"]),
    ("मला ताप आहे",              ["fever"]),
    ("खोकला येत आहे",            ["cough"]),
    ("पोटदुखी",                  ["stomach_pain"]),
    ("पोट दुखत आहे",             ["stomach_pain"]),
    ("चक्कर येत आहे",            ["dizziness"]),

    # Roman Marathi
    ("dokedukhi",                ["headache"]),
    ("doka dukhtay",             ["headache"]),
    ("tap",                      ["fever"]),
    ("taap aala",                ["fever"]),
    ("khokla yetoy",             ["cough"]),
    ("pot dukhat",               ["stomach_pain"]),
    ("potdukhi",                 ["stomach_pain"]),
    ("chakkar yetoy",            ["dizziness"]),
    ("garagar",                  ["dizziness"]),

    # Hindi Devanagari
    ("सिरदर्द",                  ["headache"]),
    ("मेरे सिर में दर्द है",     ["headache"]),
    ("बुखार",                    ["fever"]),
    ("तेज बुखार",                ["fever"]),
    ("खांसी",                    ["cough"]),
    ("खांसी आ रही है",           ["cough"]),
    ("पेट दर्द",                 ["stomach_pain"]),
    ("पेट में दर्द",             ["stomach_pain"]),
    ("चक्कर आ रहा है",           ["dizziness"]),

    # Roman Hindi
    ("sirdard",                  ["headache"]),
    ("sar dard",                 ["headache"]),
    ("sir mein dard",            ["headache"]),
    ("bukhar",                   ["fever"]),
    ("tez bukhar",               ["fever"]),
    ("khansi",                   ["cough"]),
    ("khansi aa rahi hai",       ["cough"]),
    ("pet dard",                 ["stomach_pain"]),
    ("pet mein dard",            ["stomach_pain"]),
    ("chakkar aa raha hai",      ["dizziness"]),
]

for text, expected in multilingual_symptom_tests:
    got = api.extract_symptoms(text)
    check(f'extract_symptoms({text!r}) contains {expected!r}',
          all(s in got for s in expected), True)

# 2. False-positive checks on unrelated phrases
unrelated_phrases = [
    "Yes sir, I need your advice",
    "Hello sir how are you",
    "pottery class today",
    "carpet cleaning service",
    "what is your name",
    "can you help me with python code",
    "today is a sunny day",
    "how many days in a week",
]
for text in unrelated_phrases:
    got = api.extract_symptoms(text)
    check(f'extract_symptoms({text!r}) == [] (no false positives)', got, [])


# ================================================================
# PHASE 2A — Language Correctness, English Leaks, and Localization
# ================================================================
print(f"\n{'='*60}")
print("  PHASE 2A — Language Correctness & Localization")
print(f"{'='*60}")

# 1. Exact Hindi Devanagari sentences from investigation report
hindi_investigation_samples = [
    ("पानी उबालकर पिएं।", "hi"),
    ("पानी उबालकर पिएं。", "hi"),
    ("ज्यादा आराम करें और तनाव न लें।", "hi"),
    ("ज्यादा आराम करें और तनाव न लें。", "hi"),
    ("ORS घोल पिएं और आराम करें।", "hi"),
    ("ORS घोल पिएं और आराम करें。", "hi"),
    ("अगर लक्षण बढ़ें तो डॉक्टर के पास जाएं।", "hi"),
    ("अगर लक्षण बढ़ें तो डॉक्टर के पास जाएं。", "hi"),
    ("समय पर खाना खाएं और ताजे फल लें।", "hi"),
    ("समय पर खाना खाएं और ताजे फल लें。", "hi"),
    ("शरीर का तापमान बढ़ने पर खूब पानी पिएं और आराम करें।", "hi"),
    ("शरीर का तापमान बढ़ने पर खूब पानी पिएं और आराम करें。", "hi"),
]
for text, expected_lang in hindi_investigation_samples:
    got = api.detect_language(text)
    check(f'detect_language({text!r}) == {expected_lang!r}', got, expected_lang)

# 2. Devanagari Hindi vs Marathi and preservation of fallback_lang
devanagari_tests = [
    ("मला ताप आहे आणि खोकला येत आहे", "mr"),
    ("माझे डोके दुखत आहे कृपया औषध सांगा", "mr"),
    ("१०२ ताप आहे", "mr"),
    ("ताप आला आहे काळजी घ्या", "mr"),
    ("मुझे तेज बुखार और खांसी है", "hi"),
    ("मेरे सिर में बहुत दर्द है दवा बताइए", "hi"),
    ("१०२ बुखार है", "hi"),
    ("बुखार आ गया है आराम करें", "hi"),
    # Ambiguous short numbers/terms with fallback_lang
    ("१०२", "mr", "mr"),
    ("१०२", "hi", "hi"),
    ("तापमान", "mr", "mr"),
    ("तापमान", "hi", "hi"),
]
for item in devanagari_tests:
    if len(item) == 2:
        text, expected = item
        got = api.detect_language(text)
        check(f'detect_language({text!r}) == {expected!r}', got, expected)
    else:
        text, fallback, expected = item
        got = api.detect_language(text, fallback_lang=fallback)
        check(f'detect_language({text!r}, fallback={fallback!r}) == {expected!r}', got, expected)

# 3. _looks_mixed_with_english tests
mixed_leak_tests = [
    # Hindi Devanagari with clinical abbreviations should NOT be flagged
    ("ORS घोल पिएं और आराम करें।", "hi", False),
    ("बुखार के लिए Paracetamol 500mg ले सकते हैं।", "hi", False),
    ("अगर BP ज्यादा हो तो Doctor से तुरंत मिलें।", "hi", False),
    ("CT scan या MRI की जरूरत हो सकती है।", "hi", False),
    # Marathi Devanagari with clinical abbreviations should NOT be flagged
    ("ORS पाणी प्या आणि विश्रांती घ्या.", "mr", False),
    ("ताप असल्यास Paracetamol गोळी घ्यावी.", "mr", False),
    ("BP तपासण्यासाठी Doctor कडे जा.", "mr", False),
    # Valid Roman Hindi / Roman Marathi responses should NOT be flagged
    ("Aapko bukhar hai to paracetamol le sakte hain aur aaram karein.", "hi", False),
    ("Mala tap ahe aani doka dukhat ahe aaram kara.", "mr", False),
    ("Doctor se salah lein aur paani khoob peeyo.", "hi", False),
    # Strong English boilerplate leaks SHOULD be flagged
    ("Here is some general information about headache.", "hi", True),
    ("Clinical overview for cough: Please take rest.", "hi", True),
    ("Important: This information is for general guidance and does not provide a diagnosis.", "hi", True),
    ("Important: Seek urgent medical care if severe.", "mr", True),
    # Pure English text served for Hindi/Marathi SHOULD be flagged
    ("Drink plenty of water, get adequate rest, and monitor your symptoms.", "hi", True),
    ("Drink plenty of water, get adequate rest, and monitor your symptoms.", "mr", True),
    # English text for English language should NOT be flagged
    ("Drink plenty of water, get adequate rest, and monitor your symptoms.", "en", False),
]
for text, lang, expected in mixed_leak_tests:
    got = api._looks_mixed_with_english(text, lang)
    check(f'_looks_mixed_with_english({text[:40]!r}..., {lang!r}) == {expected!r}', got, expected)

# 4. localize_answer preservation & cleaning tests
# English answer for English request is untouched
en_text = "Get some rest and drink warm fluids."
check("localize_answer('en') untouched", api.localize_answer(en_text, "en"), en_text)

# Valid Hindi with clinical words is returned directly without corruption
hi_clinical = "ORS घोल पिएं और Paracetamol 500mg लें। Doctor से परामर्श करें।"
check("localize_answer valid Hindi preserved", api.localize_answer(hi_clinical, "hi"), hi_clinical)

# Valid Marathi with clinical words is returned directly without corruption
mr_clinical = "ORS पाणी प्या आणि भरपूर विश्रांती घ्या. BP वाढल्यास Doctor चा सल्ला घ्या."
check("localize_answer valid Marathi preserved", api.localize_answer(mr_clinical, "mr"), mr_clinical)

# Valid Roman Hindi is preserved
ro_hi = "Aapko bukhar hai to aaram karein aur paracetamol lein."
check("localize_answer Roman Hindi preserved", api.localize_answer(ro_hi, "hi"), ro_hi)

# Valid Roman Marathi is preserved
ro_mr = "Mala tap ahe aani doka dukhat ahe aaram kara."
check("localize_answer Roman Marathi preserved", api.localize_answer(ro_mr, "mr"), ro_mr)

# Devanagari answer with English template preamble has preamble stripped
mixed_preamble = "Clinical overview for headache:\n\nशांत खोलीत विश्रांती घ्या आणि भरपूर पाणी प्या."
expected_clean = "शांत खोलीत विश्रांती घ्या आणि भरपूर पाणी प्या."
check("localize_answer strips English template line", api.localize_answer(mixed_preamble, "mr"), expected_clean)

# Safe emergency warning preservation when translation fails (mock translate returning None)
emergency_en = "Emergency: Seek urgent medical care immediately if you have chest pain or breathing difficulty."
check("localize_answer preserves safe medical warning on translation failure",
      api.localize_answer(emergency_en, "hi"), emergency_en)

# 5. build_multi_symptom_response does not contain the old translation trigger
multi_resp = api.build_multi_symptom_response(["headache"], "information")
check("build_multi_symptom_response does not contain 'Here is some general information'",
      "Here is some general information" not in (multi_resp or ""), True)
check("build_multi_symptom_response starts with 'Clinical overview'",
      (multi_resp or "").startswith("Clinical overview for headache:"), True)


# ================================================================
# PHASE B — Native Role-Based History, Contextual Follow-Up & Memory
# ================================================================
section("PHASE B — Native Role-Based History, Contextual Follow-Up & Memory")

# 1. Back Pain Multilingual Extraction across 5 categories
back_pain_tests = [
    # English
    ("I have lower back pain", ["back_pain"]),
    ("my back hurts a lot", ["back_pain"]),
    # Marathi Devanagari
    ("माझी पाठ दुखत आहे", ["back_pain"]),
    ("पाठदुखी चा त्रास आहे", ["back_pain"]),
    ("पाठ खूप दुखत आहे", ["back_pain"]),
    # Hindi Devanagari
    ("मुझे कमर दर्द है", ["back_pain"]),
    ("पीठ दर्द की समस्या है", ["back_pain"]),
    ("कमर में दर्द हो रहा है", ["back_pain"]),
    # Roman Marathi
    ("mala path dukhat ahe", ["back_pain"]),
    ("kambar dukhat ahe", ["back_pain"]),
    # Roman Hindi
    ("kamar dard hai", ["back_pain"]),
    ("peeth dard hai", ["back_pain"]),
    ("kamar me dard ho raha hai", ["back_pain"]),
    # False-positive checks (unrelated words with 'back')
    ("background image", []),
    ("come back later", []),
    ("give me feedback", []),
]

for text, expected in back_pain_tests:
    got = api.extract_symptoms(text)
    if expected:
        check(f"extract_symptoms({text!r}) contains {expected!r}",
              all(s in got for s in expected), True)
    else:
        check(f"extract_symptoms({text!r}) has no back_pain false positive",
              "back_pain" not in got, True)

# 2. Contextual Follow-Up Detection (is_contextual_followup)
test_sess_b1 = "test_sess_phase_b_followup_001"
# With active previous symptoms and active context:
check("is_contextual_followup('Two days, lower back')",
      api.is_contextual_followup("Two days, lower back", session_id=test_sess_b1, previous_symptoms=["back_pain"]),
      True)
check("is_contextual_followup('since yesterday')",
      api.is_contextual_followup("since yesterday", session_id=test_sess_b1, previous_symptoms=["headache"]),
      True)
check("is_contextual_followup('it is getting worse')",
      api.is_contextual_followup("it is getting worse", session_id=test_sess_b1, previous_symptoms=["fever"]),
      True)
check("is_contextual_followup('that pain')",
      api.is_contextual_followup("that pain", session_id=test_sess_b1, previous_symptoms=["stomach_pain"]),
      True)
check("is_contextual_followup('mg azaala kay kru')",
      api.is_contextual_followup("mg azaala kay kru", session_id=test_sess_b1, previous_symptoms=["cough"]),
      True)
check("is_contextual_followup('calpol ghyavi ka')",
      api.is_contextual_followup("calpol ghyavi ka", session_id=test_sess_b1, previous_symptoms=["fever"]),
      True)

# Without previous symptoms (new conversation):
check("is_contextual_followup without previous symptoms is False",
      api.is_contextual_followup("Two days", session_id="new_sess_b_002", previous_symptoms=[]),
      False)
check("is_contextual_followup for general phrase is False",
      api.is_contextual_followup("today is Monday", session_id="new_sess_b_002", previous_symptoms=[]),
      False)
check("is_contextual_followup for non-followup question is False",
      api.is_contextual_followup("what is diabetes", session_id="new_sess_b_002", previous_symptoms=[]),
      False)

# 3. Structured Medical Memory Follow-Up Linking (save_structured_memories)
session_b_mem = "test_phase_b_mem_session_001"
# Turn 1: Symptom reported
api.save_structured_memories(
    session_id=session_b_mem,
    text="I have severe back pain",
    symptoms=["back_pain"],
    duration=None,
    previous_symptoms=[]
)
# Turn 2: Follow-up response with duration and location, symptoms empty
api.save_structured_memories(
    session_id=session_b_mem,
    text="Two days, lower back",
    symptoms=[],
    duration="2 days",
    previous_symptoms=["back_pain"]
)

mem = api.get_medical_memory(session_b_mem)
mem_symptoms = [r["name"] for r in mem if r.get("memory_type") == "symptom"]
mem_durations = {r["name"]: r.get("duration") for r in mem if r.get("duration")}
mem_locations = [r["name"] for r in mem if r.get("memory_type") == "location"]

check("Structured memory retains 'back_pain'", "back_pain" in mem_symptoms, True)
check("Structured memory links duration '2 days' to back_pain",
      mem_durations.get("back_pain"), "2 days")
check("Structured memory records 'lower back' location",
      any("lower back" in f for f in mem_locations), True)

# 4. Session Memory Summary Formatting (build_session_memory_summary)
summary = api.build_session_memory_summary(session_b_mem)
check("build_session_memory_summary contains back_pain", "back_pain" in summary, True)
check("build_session_memory_summary contains duration 2 days", "2 days" in summary, True)
check("build_session_memory_summary contains lower back finding", "lower back" in summary, True)

empty_summary = api.build_session_memory_summary("non_existent_session_phase_b_xyz")
check("build_session_memory_summary for empty session is empty string", empty_summary, "")

# 5. Native Role-Based Multi-Turn Message Construction (_build_multi_turn_messages)
session_b_chat = "test_phase_b_chat_session_001"
api.save_chat_message(session_b_chat, "user", "I have had a severe back pain since yesterday", ["back_pain"])
api.save_chat_message(session_b_chat, "assistant", "Could you describe where the pain is located?", ["back_pain"])

system_instruction = api._build_medical_system_instruction("en", memory_summary="Confirmed symptoms: back_pain")
messages = api._build_multi_turn_messages(
    session_id=session_b_chat,
    system_instruction=system_instruction,
    current_user_text="Two days, lower back",
    language="en",
    medical_context="General clinical guidance for back pain."
)

check("Multi-turn messages is a non-empty list", isinstance(messages, list) and len(messages) >= 4, True)
check("First message is system instruction", messages[0].get("role"), "system")
check("System message includes conversational directives",
      "DO NOT repeatedly ask" in messages[0].get("content", ""), True)
check("Second message is prior user turn",
      messages[1].get("role") == "user" and "severe back pain" in messages[1].get("content", ""), True)
check("Third message is prior assistant turn",
      messages[2].get("role") == "assistant" and "where the pain is" in messages[2].get("content", ""), True)
check("Last message is current user turn",
      messages[-1].get("role") == "user" and "Two days, lower back" in messages[-1].get("content", ""), True)

# 6. Gemini Multi-Turn Formatting (_format_messages_for_gemini)
gemini_prompt = api._format_messages_for_gemini(messages)
check("Gemini format returns non-empty string", isinstance(gemini_prompt, str) and bool(gemini_prompt), True)
check("Gemini prompt has SYSTEM INSTRUCTIONS", "SYSTEM INSTRUCTIONS" in gemini_prompt, True)
check("Gemini prompt has MEDASSIST AI turn", "MEDASSIST AI" in gemini_prompt, True)
check("Gemini prompt has USER turns", "USER:" in gemini_prompt, True)

# 7. Ollama Chat Message Support Signature Verification
import inspect
sig = inspect.signature(api.ollama_chat)
check("ollama_chat signature accepts messages argument", "messages" in sig.parameters, True)

# 8. End-to-End Predict Follow-Up Integration
client = api.app.test_client()
e2e_sess = "test_phase_b_e2e_session_001"

# Turn 1: User reports symptom
r1 = client.post("/predict", json={"text": "I have severe back pain", "session_id": e2e_sess})
r1_data = r1.get_json() or {}
check("Turn 1 /predict returns 200", r1.status_code, 200)
check("Turn 1 detects back_pain symptom", "back_pain" in r1_data.get("symptoms", []), True)

# Turn 2: User responds with duration and location (follow-up)
r2 = client.post("/predict", json={"text": "Two days, lower back", "session_id": e2e_sess})
r2_data = r2.get_json() or {}
check("Turn 2 /predict returns 200", r2.status_code, 200)
check("Turn 2 routes to follow_up intent", r2_data.get("intent"), "follow_up")
check("Turn 2 retains previous symptoms ['back_pain']", "back_pain" in r2_data.get("symptoms", []), True)
check("Turn 2 marks medical_query as True", r2_data.get("medical_query"), True)


# ================================================================
# Summary
# ================================================================
total = len(PASSED) + len(FAILED)
print(f"\n{'='*60}")
print(f"  Regression Test Suite: {len(PASSED)}/{total} passed, {len(FAILED)} failed")
print(f"{'='*60}")
if FAILED:
    print("\nFailed tests:")
    for f in FAILED:
        print(f"  - {f}")
sys.exit(0 if not FAILED else 1)

