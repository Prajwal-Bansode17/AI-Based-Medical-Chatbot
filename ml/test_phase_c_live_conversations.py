"""
Live multi-turn conversation and memory verification for MedAssist AI (Phase C).
Uses live Ollama (gemma3:latest) or fallback to verify actual model generation quality.
"""
import os
import sys
import json
import uuid
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(BASE_DIR))

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

# Enable live inference
os.environ["OLLAMA_ENABLED"] = "1"

import importlib.util
spec = importlib.util.spec_from_file_location("api_module", BASE_DIR / "api.py")
api = importlib.util.module_from_spec(spec)
spec.loader.exec_module(api)

app = api.app
client = app.test_client()

PASSED = []
FAILED = []

def check(label, condition, details=""):
    if condition:
        PASSED.append(label)
        print(f"  [PASS] {label}")
    else:
        FAILED.append(label)
        print(f"  [FAIL] {label}")
        if details:
            print(f"         {details}")

def section(title):
    print(f"\n{'='*70}\n  {title}\n{'='*70}")

def post_msg(session_id, text):
    res = client.post("/predict", json={"text": text, "session_id": session_id})
    return res.status_code, res.get_json() or {}


# ================================================================
# TEST 1: English Multi-turn Context & No Repetitive Questions
# ================================================================
section("1. English Multi-turn: Back Pain -> Duration & Location")
sess1 = "test_c_en_" + uuid.uuid4().hex[:8]

code1, r1 = post_msg(sess1, "I have back pain")
print(f"Turn 1 Intent: {r1.get('intent')} | Source: {r1.get('source')}")
print(f"Turn 1 Answer snippet: {r1.get('answer', '')[:120]}...\n")
check("Turn 1 HTTP 200", code1 == 200)
check("Turn 1 detects back_pain", "back_pain" in r1.get("symptoms", []))

code2, r2 = post_msg(sess1, "Two days, lower back")
ans2 = r2.get("answer", "")
print(f"Turn 2 Intent: {r2.get('intent')} | Source: {r2.get('source')}")
print(f"Turn 2 Full Answer:\n{ans2}\n")
check("Turn 2 HTTP 200", code2 == 200)
check("Turn 2 routed to follow_up", r2.get("intent") == "follow_up")
# Verify model doesn't re-ask how long it has been
reask_duration = ("how long" in ans2.lower() or "when did it start" in ans2.lower())
check("Model does NOT re-ask duration", not reask_duration, f"Found duration re-asking: {ans2[:200]}")
# Verify model acknowledged or understood lower back
acknowledged = ("lower back" in ans2.lower() or "back" in ans2.lower() or "two days" in ans2.lower() or "2 days" in ans2.lower())
check("Model acknowledges back / context", acknowledged)


# ================================================================
# TEST 2: Marathi Devanagari Multi-turn
# ================================================================
section("2. Marathi Devanagari Multi-turn: पाठदुखी")
sess2 = "test_c_mr_" + uuid.uuid4().hex[:8]

code2_1, r2_1 = post_msg(sess2, "मला पाठ दुखत आहे")
ans2_1 = r2_1.get("answer", "")
print(f"Turn 1 Lang: {r2_1.get('language')} | Intent: {r2_1.get('intent')} | Source: {r2_1.get('source')}")
print(f"Turn 1 Answer:\n{ans2_1[:150]}...\n")
check("Marathi Turn 1 HTTP 200", code2_1 == 200)
check("Marathi Turn 1 language is 'mr'", r2_1.get("language") == "mr")
check("Marathi Turn 1 detects back_pain", "back_pain" in r2_1.get("symptoms", []))

code2_2, r2_2 = post_msg(sess2, "दोन दिवस झाले, जास्त त्रास होतोय")
ans2_2 = r2_2.get("answer", "")
print(f"Turn 2 Lang: {r2_2.get('language')} | Intent: {r2_2.get('intent')} | Source: {r2_2.get('source')}")
print(f"Turn 2 Answer:\n{ans2_2}\n")
check("Marathi Turn 2 HTTP 200", code2_2 == 200)
check("Marathi Turn 2 language is 'mr'", r2_2.get("language") == "mr")
check("Marathi Turn 2 retains back_pain", "back_pain" in r2_2.get("symptoms", []))


# ================================================================
# TEST 3: Roman Marathi Reference ("mg azaala kay kru")
# ================================================================
section("3. Roman Marathi Multi-turn: 'mg azaala kay kru'")
sess3 = "test_c_romr_" + uuid.uuid4().hex[:8]

code3_1, r3_1 = post_msg(sess3, "mala khokla ahe")
print(f"Turn 1 Lang: {r3_1.get('language')} | Symptoms: {r3_1.get('symptoms')}")
check("Roman Marathi Turn 1 detects cough", "cough" in r3_1.get("symptoms", []))

code3_2, r3_2 = post_msg(sess3, "mg azaala kay kru")
ans3_2 = r3_2.get("answer", "")
print(f"Turn 2 Intent: {r3_2.get('intent')} | Source: {r3_2.get('source')}")
print(f"Turn 2 Answer snippet:\n{ans3_2[:160]}...\n")
check("Turn 2 routes to follow_up", r3_2.get("intent") == "follow_up")
check("Turn 2 provides guidance for cough", len(ans3_2) > 30)


# ================================================================
# TEST 4: Hindi Devanagari / Roman Hindi ("kamar mein dard hai")
# ================================================================
section("4. Hindi Multi-turn: 'kamar mein dard hai' -> 'do din se hai'")
sess4 = "test_c_hi_" + uuid.uuid4().hex[:8]

code4_1, r4_1 = post_msg(sess4, "kamar mein dard hai")
print(f"Turn 1 Lang: {r4_1.get('language')} | Symptoms: {r4_1.get('symptoms')}")
check("Hindi Turn 1 detects back_pain", "back_pain" in r4_1.get("symptoms", []))

code4_2, r4_2 = post_msg(sess4, "do din se hai")
ans4_2 = r4_2.get("answer", "")
print(f"Turn 2 Intent: {r4_2.get('intent')} | Lang: {r4_2.get('language')} | Source: {r4_2.get('source')}")
print(f"Turn 2 Answer snippet:\n{ans4_2[:160]}...\n")
check("Hindi Turn 2 routes to follow_up", r4_2.get("intent") == "follow_up")
check("Hindi Turn 2 retains back_pain", "back_pain" in r4_2.get("symptoms", []))


# ================================================================
# TEST 5: Pronoun & Reference Resolution ("that pain", "getting worse")
# ================================================================
section("5. Reference Resolution: 'that pain' / 'it is getting worse'")
sess5 = "test_c_ref_" + uuid.uuid4().hex[:8]

code5_1, r5_1 = post_msg(sess5, "I have had bad stomach pain since morning")
check("Turn 1 detects stomach_pain", "stomach_pain" in r5_1.get("symptoms", []))

code5_2, r5_2 = post_msg(sess5, "it is getting worse and nausea started")
ans5_2 = r5_2.get("answer", "")
print(f"Turn 2 Intent: {r5_2.get('intent')} | Symptoms: {r5_2.get('symptoms')}")
print(f"Turn 2 Answer snippet:\n{ans5_2[:160]}...\n")
check("Turn 2 preserves stomach_pain context", "stomach_pain" in r5_2.get("symptoms", []))

code5_3, r5_3 = post_msg(sess5, "what care should I take for that pain?")
ans5_3 = r5_3.get("answer", "")
print(f"Turn 3 Intent: {r5_3.get('intent')} | Source: {r5_3.get('source')}")
check("Turn 3 resolves 'that pain' to stomach_pain follow_up", r5_3.get("intent") == "follow_up")
check("Turn 3 retains stomach_pain", "stomach_pain" in r5_3.get("symptoms", []))


# ================================================================
# TEST 6: Session Isolation & History Persistence
# ================================================================
section("6. Session Persistence & Cross-Session Isolation")
sess_a = "test_c_isolated_A_" + uuid.uuid4().hex[:8]
sess_b = "test_c_isolated_B_" + uuid.uuid4().hex[:8]

# User A reports symptoms and measurement
post_msg(sess_a, "I have severe fever and my bp is 140/90")
# User B asks about health
code_b, r_b = post_msg(sess_b, "what did I tell you about my health earlier?")
ans_b = r_b.get("answer", "")
print(f"Session B recall response: {ans_b}")
check("Session B has NO knowledge of Session A's fever", "fever" not in ans_b.lower())
check("Session B has NO knowledge of Session A's BP", "140" not in ans_b)

# Reopen Session A and verify context persists
code_a, r_a = post_msg(sess_a, "what symptoms did I mention earlier?")
ans_a = r_a.get("answer", "")
print(f"Session A recall response: {ans_a}")
check("Session A correctly recalls fever", "fever" in ans_a.lower())


# ================================================================
# TEST 7: Emergency Warning & Safety Rules
# ================================================================
section("7. Emergency Warning & Red-Flag Clinical Detection")
sess_emerg = "test_c_emerg_" + uuid.uuid4().hex[:8]

# Severe chest pain with sweating and breathlessness
code_em, r_em = post_msg(sess_emerg, "I have crushing chest pain and cold sweating and trouble breathing")
ans_em = r_em.get("answer", "")
print(f"Emergency Intent: {r_em.get('intent')} | Emergency flag: {r_em.get('emergency')}")
print(f"Emergency Answer snippet:\n{ans_em[:180]}...\n")
check("Emergency trigger HTTP 200", code_em == 200)
check("Emergency intent flagged", r_em.get("intent") == "emergency")
check("Emergency flag set True", r_em.get("emergency") is True)
check("Advises calling 112 / ambulance / hospital", ("112" in ans_em or "ambulance" in ans_em or "hospital" in ans_em or "emergency" in ans_em))


# ================================================================
# Summary
# ================================================================
total = len(PASSED) + len(FAILED)
print(f"\n{'='*70}")
print(f"  Live Conversation & Quality Suite: {len(PASSED)}/{total} passed, {len(FAILED)} failed")
print(f"{'='*70}")
if FAILED:
    print("\nFailed tests:")
    for f in FAILED:
        print(f"  - {f}")
sys.exit(0 if not FAILED else 1)
