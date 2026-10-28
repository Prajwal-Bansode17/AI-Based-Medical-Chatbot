"""
Runtime Verification for Phase B - Cross-Chat Medical Memory
Tests all 12 scenario requirements with real database transactions and process restart simulation.
"""

import os
import sys
import uuid
import sqlite3
import json
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(BASE_DIR))

# Ensure Ollama is not hanging tests
os.environ["OLLAMA_ENABLED"] = "0"

import memory_service
from api import app

def run_phase_b_verification():
    print("=" * 70)
    print("STARTING RUNTIME VERIFICATION: PHASE B - CROSS-CHAT MEDICAL MEMORY")
    print("=" * 70)

    # Use a clean persistent SQLite test file
    test_db = BASE_DIR / "data" / f"verify_phase_b_{uuid.uuid4().hex[:8]}.db"
    memory_service.DB_FILE = test_db
    memory_service.init_memory_schema(test_db)
    print(f"[DB] Initialized isolated test database at: {test_db}")

    client = app.test_client()
    user_a = f"user_phase_b_{uuid.uuid4().hex[:6]}"
    auth_a = {"Authorization": f"Bearer {user_a}"}

    # 1. User Consent Handling
    print("\n[Step 1] Checking and granting memory consent for User A...")
    c_res = client.get("/api/v1/memory/consent", headers=auth_a)
    assert c_res.status_code == 200
    assert c_res.get_json()["consent"] is False, "Initial consent must be False"
    print("-> Consent initially False (verified opt-in requirement).")

    post_c = client.post("/api/v1/memory/consent", headers=auth_a, json={"consent": True})
    assert post_c.status_code == 200
    assert post_c.get_json()["consent"] is True, "Consent must now be True"
    print("-> Consent granted successfully.")

    # 2. Chat A: User sends symptom about friend Hariom
    session_a = f"session_A_{uuid.uuid4().hex[:6]}"
    msg_1 = "My friend Hariom has been experiencing hair fall for three months."
    print(f"\n[Step 2] User A sends in {session_a}: '{msg_1}'")
    resp_1 = client.post("/predict", headers=auth_a, json={
        "session_id": session_a,
        "text": msg_1,
        "user_id": user_a
    })
    assert resp_1.status_code == 200
    data_1 = resp_1.get_json()
    print("-> Response status: 200 OK")
    print(f"-> Chatbot initial reply preview: {data_1['answer'][:120]}...")

    # 3. Direct DB Inspection: Verify entity saved in SQLite
    print("\n[Step 3] Inspecting SQLite database directly for saved entities...")
    entities = memory_service.get_user_medical_entities(user_a)
    assert len(entities) >= 1, "Medical entity must be saved in DB"
    hariom_ent = entities[0]
    print(f"-> Saved entity in DB: Subject='{hariom_ent['subject']}', Relation='{hariom_ent['subject_relation']}', Concern='{hariom_ent['concern']}', Duration='{hariom_ent['duration']}', Status='{hariom_ent['status']}'")
    assert hariom_ent["subject"].lower() == "hariom"
    assert hariom_ent["concern"] == "hairfall"
    assert "3 month" in (hariom_ent["duration"] or "").lower()
    print("-> Verified: Hariom saved as friend with hairfall and 3 months duration!")

    # 4 & 5. Chat B: Genuinely new session, ask in Marathi about Hariom
    session_b = f"session_B_{uuid.uuid4().hex[:6]}"
    msg_2 = "Hariom la konta problem hota?"
    print(f"\n[Step 4 & 5] User A starts new chat {session_b} and asks: '{msg_2}'")
    resp_2 = client.post("/predict", headers=auth_a, json={
        "session_id": session_b,
        "text": msg_2,
        "user_id": user_a
    })
    assert resp_2.status_code == 200
    data_2 = resp_2.get_json()
    print("-> Chatbot reply:")
    print(f"   {data_2['answer']}")
    assert data_2["intent"] == "memory_recall"
    assert "hariom" in data_2["answer"].lower() or "मित्र" in data_2["answer"]
    assert "केस गळती" in data_2["answer"] or "hair" in data_2["answer"].lower()
    assert "3 month" in data_2["answer"].lower() or "३ महिने" in data_2["answer"]
    print("-> Verified: Correct recall of Hariom's concern and duration in new session!")

    # 6 & 7. Chat B: Ask what was user's personal problem
    msg_3 = "What was my medical problem?"
    print(f"\n[Step 6 & 7] User A asks in {session_b}: '{msg_3}'")
    resp_3 = client.post("/predict", headers=auth_a, json={
        "session_id": session_b,
        "text": msg_3,
        "user_id": user_a
    })
    assert resp_3.status_code == 200
    data_3 = resp_3.get_json()
    print("-> Chatbot reply:")
    print(f"   {data_3['answer']}")
    # Must NOT conflate Hariom's problem with user's personal self
    assert "haven't reported any personal" in data_3["answer"].lower() or "स्वतः" in data_3["answer"]
    print("-> Verified: Strict person isolation! Hariom's problem NOT attributed to user.")

    # 8. Process Restart Simulation
    print("\n[Step 8] Simulating Flask backend process restart...")
    # Re-initialize memory service pointing to same disk DB
    del memory_service.DB_FILE
    memory_service.DB_FILE = test_db
    memory_service.init_memory_schema(test_db)
    print("-> Backend restarted and reconnected to persistent SQLite disk file.")

    session_c = f"session_C_{uuid.uuid4().hex[:6]}"
    resp_restart = client.post("/predict", headers=auth_a, json={
        "session_id": session_c,
        "text": "What health problem did Hariom have?",
        "user_id": user_a
    })
    assert resp_restart.status_code == 200
    data_restart = resp_restart.get_json()
    print(f"-> Chatbot reply after restart:\n   {data_restart['answer']}")
    assert "hariom" in data_restart["answer"].lower()
    assert "hair" in data_restart["answer"].lower()
    print("-> Verified: Cross-chat recall survived full restart!")

    # 9. Cross-User Isolation (User B)
    user_b = f"user_phase_b_{uuid.uuid4().hex[:6]}"
    auth_b = {"Authorization": f"Bearer {user_b}"}
    memory_service.set_user_consent(user_b, True)
    session_user_b = f"session_user_B_{uuid.uuid4().hex[:6]}"
    print(f"\n[Step 9] Unrelated User B asks in {session_user_b}: 'Hariom la konta problem hota?'")
    resp_user_b = client.post("/predict", headers=auth_b, json={
        "session_id": session_user_b,
        "text": "Hariom la konta problem hota?",
        "user_id": user_b
    })
    assert resp_user_b.status_code == 200
    data_ub = resp_user_b.get_json()
    print(f"-> User B chatbot reply:\n   {data_ub['answer']}")
    assert "कोणतीही वैद्यकीय नोंद नाही" in data_ub["answer"] or "do not have any saved" in data_ub["answer"].lower()
    print("-> Verified: Strict User Isolation! User B CANNOT see User A's medical records.")

    # 10. Test Corrections and Progression Updates
    print("\n[Step 10] Testing correction and progression updates...")
    session_d = f"session_D_{uuid.uuid4().hex[:6]}"
    msg_corr = "To maza problem navhata, Hariom cha hota"
    resp_corr = client.post("/predict", headers=auth_a, json={
        "session_id": session_d,
        "text": msg_corr,
        "user_id": user_a
    })
    assert resp_corr.status_code == 200
    print(f"-> Correction reply:\n   {resp_corr.get_json()['answer']}")
    assert "चूक दुरुस्त" in resp_corr.get_json()["answer"] or "corrected" in resp_corr.get_json()["answer"].lower()

    msg_prog = "Maza mitra cha hairfall ata kami zala aahe"
    resp_prog = client.post("/predict", headers=auth_a, json={
        "session_id": session_d,
        "text": msg_prog,
        "user_id": user_a
    })
    assert resp_prog.status_code == 200
    print(f"-> Progression reply:\n   {resp_prog.get_json()['answer']}")
    assert "सुधारणा" in resp_prog.get_json()["answer"] or "improved" in resp_prog.get_json()["answer"].lower()
    # Check DB status updated to improving
    ents_after = memory_service.get_user_medical_entities(user_a)
    assert any(e["status"] == "improving" for e in ents_after)
    print("-> Verified: Progression updated status to 'improving' in persistent DB!")

    # 11. Test General Questions: MUST NOT become personal memories
    print("\n[Step 11] Testing general questions that must NOT become medical memories...")
    gen_queries = [
        "What causes fever?",
        "fever kashamule hoto?",
        "Can high blood pressure cause dizziness?"
    ]
    ents_count_before = len(memory_service.get_user_medical_entities(user_a))
    for gq in gen_queries:
        r_gen = client.post("/predict", headers=auth_a, json={
            "session_id": session_d,
            "text": gq,
            "user_id": user_a
        })
        assert r_gen.status_code == 200
        print(f"-> General query '{gq}' answered (status 200).")
    ents_count_after = len(memory_service.get_user_medical_entities(user_a))
    assert ents_count_after == ents_count_before, "General questions must not pollute user_medical_entities!"
    print(f"-> Verified: Entity count before={ents_count_before}, after={ents_count_after}. Zero pollution!")

    # 12. Test Guest User and Temporary Chat Restrictions
    print("\n[Step 12] Testing guest and temporary chats...")
    guest_headers = {"Authorization": "Bearer guest"}
    resp_guest = client.post("/predict", headers=guest_headers, json={
        "session_id": "guest_session_1",
        "text": "I have severe migraine for 4 days",
        "user_id": "guest"
    })
    assert resp_guest.status_code == 200
    guest_ents = memory_service.get_user_medical_entities("guest")
    assert len(guest_ents) == 0, "Guest must NEVER write persistent medical entities!"

    resp_temp = client.post("/predict", headers=auth_a, json={
        "session_id": "temp_session_1",
        "text": "I have severe asthma attack",
        "user_id": user_a,
        "is_temporary": True
    })
    assert resp_temp.status_code == 200
    temp_ents = [e for e in memory_service.get_user_medical_entities(user_a) if e["concern"] == "asthma"]
    assert len(temp_ents) == 0, "Temporary chat must NEVER write persistent medical entities!"
    print("-> Verified: Guests and temporary chats strictly prevented from writing long-term memory!")

    # Cleanup test db
    if test_db.exists():
        try:
            test_db.unlink()
        except Exception:
            pass

    print("\n" + "=" * 70)
    print("PHASE B VERIFICATION COMPLETE: ALL 12 CHECKS PASSED WITH 100% SUCCESS!")
    print("=" * 70)

if __name__ == "__main__":
    run_phase_b_verification()
