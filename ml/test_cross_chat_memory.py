"""
Automated Test Suite for MedAssist AI:
Persistent Medical Memory Across Different Chats & Multilingual Recall (PART 1 & PART 2)

Tests:
1. Entity extraction for third-party relations and proper names (Roman Marathi & Devanagari).
2. Entity extraction for self (ensuring subject is 'self' without false name detection).
3. Cross-chat recall: Chat A save -> Chat B recall across different session_ids.
4. Strict subject isolation: 'Mala konta problem hota?' does NOT return friend's issue.
5. Broad recall: 'Mi tula adhi kay sangitla hota?' recalls saved entities under proper relations.
6. Condition progression & update: Chat C update ('kami zala') -> Chat D updated recall.
7. User isolation: User A's medical entities are completely invisible to User B.
8. Consent & Guest protection: Guests and unconsented users never persist or recall entities.
9. Marathi output consistency: Marathi input produces clean Devanagari Marathi output.
"""

import unittest
import os
import sys
import uuid
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(BASE_DIR))

# Disable external LLM calls during automated unit test
os.environ["OLLAMA_ENABLED"] = "0"
os.environ["GEMINI_API_KEY"] = ""
os.environ["DEV_MODE"] = "1"

import memory_service
import api
from api import app

# Ensure Gemini is disabled in memory
api.gemini_client = None

class TestCrossChatMemory(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        # Isolated test database
        cls.test_db = BASE_DIR / "data" / f"test_cross_memory_{uuid.uuid4().hex[:8]}.db"
        cls.test_db.parent.mkdir(parents=True, exist_ok=True)
        memory_service.DB_FILE = cls.test_db
        memory_service.init_memory_schema(cls.test_db)

    @classmethod
    def tearDownClass(cls):
        if cls.test_db.exists():
            try:
                cls.test_db.unlink()
            except Exception:
                pass

    def setUp(self):
        self.client = app.test_client()
        self.user_a = f"user_a_{uuid.uuid4().hex[:8]}"
        self.user_b = f"user_b_{uuid.uuid4().hex[:8]}"
        self.headers_a = {"Authorization": f"Bearer {self.user_a}"}
        self.headers_b = {"Authorization": f"Bearer {self.user_b}"}

        # Enable consent for authenticated test users
        memory_service.set_user_consent(self.user_a, True)
        memory_service.set_user_consent(self.user_b, True)

    # -------------------------------------------------------------------------
    # 1. ENTITY EXTRACTION UNIT TESTS
    # -------------------------------------------------------------------------

    def test_extract_entity_friend_hariom_roman_marathi(self):
        """Extracts relation=friend, subject=Hariom, concern=hairfall from Roman Marathi."""
        text = "Maza mitra Hariom la hairfall cha problem aahe"
        entity = memory_service.extract_medical_entity(text)
        self.assertIsNotNone(entity, "Failed to extract entity from Roman Marathi")
        self.assertEqual(entity["subject"], "Hariom")
        self.assertEqual(entity["subject_relation"], "friend")
        self.assertIn("hair", entity["concern"].lower())
        self.assertEqual(entity["status"], "active")

    def test_extract_entity_devanagari_marathi(self):
        """Extracts Devanagari third party entity correctly."""
        text = "माझा मित्र हरीश ला डोकेदुखी चा त्रास आहे"
        entity = memory_service.extract_medical_entity(text)
        self.assertIsNotNone(entity)
        self.assertIn(entity["subject"], ["हरीश", "Harish", "harish", "मित्र"])
        self.assertEqual(entity["subject_relation"], "friend")

    def test_extract_entity_self_isolation(self):
        """Self query must have subject='self' and never extract verbs/pronouns as names."""
        text = "Mala 3 divasapasun potat dukhatahe"
        entity = memory_service.extract_medical_entity(text)
        self.assertIsNotNone(entity)
        self.assertEqual(entity["subject"], "self")
        self.assertEqual(entity["subject_relation"], "self")
        # Ensure 'mala' or 'divas' was not extracted as a proper name
        self.assertNotEqual(entity["subject"], "Mala")

    def test_extract_entity_status_progression(self):
        """Extracts improving status from update phrase."""
        text = "Maza mitra cha hairfall ata kami zala aahe"
        entity = memory_service.extract_medical_entity(text)
        self.assertIsNotNone(entity)
        self.assertEqual(entity["status"], "improving")
        self.assertEqual(entity["subject_relation"], "friend")

    # -------------------------------------------------------------------------
    # 2. END-TO-END CROSS-CHAT CONVERSATION FLOW TESTS
    # -------------------------------------------------------------------------

    def test_cross_chat_recall_and_person_isolation(self):
        """
        Verify:
        Chat A (Session 1): User reports friend Hariom's hairfall.
        Chat B (Session 2): User asks about Hariom -> Recalls Hariom's hairfall.
        Chat B (Session 2): User asks about self -> Strictly asserts user does NOT have hairfall.
        """
        session_1 = f"session_chat_a_{uuid.uuid4().hex[:6]}"
        session_2 = f"session_chat_b_{uuid.uuid4().hex[:6]}"

        # Turn 1: Chat A - Friend Hariom's condition
        resp1 = self.client.post("/predict", headers=self.headers_a, json={
            "question": "Maza mitra Hariom la hairfall cha problem aahe, kay karava?",
            "session_id": session_1
        })
        self.assertEqual(resp1.status_code, 200)
        data1 = resp1.get_json()
        self.assertEqual(data1["status"], "success")

        # Verify entity saved in DB for user_a
        entities = memory_service.get_user_medical_entities(self.user_a)
        self.assertTrue(len(entities) > 0, "Medical entity was not saved in DB for user_a")
        saved_subj = [e["subject"].lower() for e in entities]
        self.assertIn("hariom", saved_subj)

        # Turn 2: Chat B (different session) - User asks about Hariom
        resp2 = self.client.post("/predict", headers=self.headers_a, json={
            "question": "Hariom la konta problem hota?",
            "session_id": session_2
        })
        self.assertEqual(resp2.status_code, 200)
        data2 = resp2.get_json()
        ans2 = data2.get("answer", "")

        # Verify recall contains Hariom and hairfall
        self.assertTrue("hariom" in ans2.lower() or "हरीओम" in ans2 or "मित्र" in ans2,
                        f"Recall missing Hariom/friend in Chat B. Answer: {ans2}")
        self.assertTrue("hair" in ans2.lower() or "केस" in ans2 or "गळणे" in ans2,
                        f"Recall missing hairfall concern in Chat B. Answer: {ans2}")

        # Turn 3: Chat B (same or new session) - Strict Self Isolation
        # User asks: 'Mala konta problem hota?' (What problem did I have?)
        resp3 = self.client.post("/predict", headers=self.headers_a, json={
            "question": "Mala konta problem hota?",
            "session_id": session_2
        })
        self.assertEqual(resp3.status_code, 200)
        data3 = resp3.get_json()
        ans3 = data3.get("answer", "")

        # CRITICAL SAFETY CHECK: Hariom's hairfall must NEVER be attributed to the user
        self.assertNotIn("तुम्हाला hairfall", ans3.lower())
        self.assertNotIn("तुम्हाला केस गळणे", ans3)
        # Should indicate no recorded personal problem for user self
        self.assertTrue("नोंद नाही" in ans3 or "कोणतीही तक्रार" in ans3 or "माहिती नाही" in ans3 or "माहिती दिलेली नाही" in ans3 or "स्वतःच्या" in ans3,
                        f"Expected self-isolation response indicating no self records, got: {ans3}")

    def test_broad_recall_across_chats(self):
        """
        Chat B (Session 2): 'Mi tula adhi kay sangitla hota?'
        Must list friend Hariom's hairfall problem under friend relation.
        """
        # Save entity first for user_a
        memory_service.save_or_update_medical_entity(
            user_id=self.user_a,
            subject="Hariom",
            subject_relation="friend",
            concern="hairfall",
            raw_condition="Maza mitra Hariom la hairfall cha problem aahe",
            status="active"
        )

        session_recall = f"session_broad_{uuid.uuid4().hex[:6]}"
        resp = self.client.post("/predict", headers=self.headers_a, json={
            "question": "Mi tula adhi kay sangitla hota?",
            "session_id": session_recall
        })
        self.assertEqual(resp.status_code, 200)
        ans = resp.get_json().get("answer", "")

        self.assertTrue("hariom" in ans.lower() or "मित्र" in ans, f"Broad recall missing friend. Ans: {ans}")
        self.assertTrue("hair" in ans.lower() or "केस" in ans or "गळणे" in ans, f"Broad recall missing concern. Ans: {ans}")

    def test_condition_progression_update_cycle(self):
        """
        Chat C: 'Maza mitra cha hairfall ata kami zala aahe' updates status to improving.
        Chat D: 'Hariom la konta problem hota?' reflects the updated 'improving' status.
        """
        # Seed initial condition
        memory_service.save_or_update_medical_entity(
            user_id=self.user_a,
            subject="Hariom",
            subject_relation="friend",
            concern="hairfall",
            raw_condition="Maza mitra Hariom la hairfall cha problem aahe",
            status="active"
        )

        session_c = f"session_c_{uuid.uuid4().hex[:6]}"
        session_d = f"session_d_{uuid.uuid4().hex[:6]}"

        # Chat C: Status update
        resp_c = self.client.post("/predict", headers=self.headers_a, json={
            "question": "Maza mitra cha hairfall ata kami zala aahe",
            "session_id": session_c
        })
        self.assertEqual(resp_c.status_code, 200)

        # Verify DB status updated to improving
        entities = memory_service.get_user_medical_entities(self.user_a, subject="Hariom")
        self.assertEqual(len(entities), 1)
        self.assertEqual(entities[0]["status"], "improving")

        # Chat D: Recall in new session
        resp_d = self.client.post("/predict", headers=self.headers_a, json={
            "question": "Hariom la konta problem hota?",
            "session_id": session_d
        })
        self.assertEqual(resp_d.status_code, 200)
        ans_d = resp_d.get_json().get("answer", "")

        # Verify answer mentions improvement (e.g. 'कमी झाले आहे' or 'सुधारणा होत आहे')
        self.assertTrue("कमी" in ans_d or "सुधारणा" in ans_d or "improving" in ans_d.lower(),
                        f"Recall did not reflect updated status 'improving'. Ans: {ans_d}")

    # -------------------------------------------------------------------------
    # 3. PRIVACY & SECURITY ISOLATION TESTS
    # -------------------------------------------------------------------------

    def test_user_isolation(self):
        """User B must never be able to recall User A's medical entities."""
        # User A has Hariom entity
        memory_service.save_or_update_medical_entity(
            user_id=self.user_a,
            subject="Hariom",
            subject_relation="friend",
            concern="hairfall",
            raw_condition="Maza mitra Hariom la hairfall cha problem aahe",
            status="active"
        )

        session_b = f"session_user_b_{uuid.uuid4().hex[:6]}"
        resp = self.client.post("/predict", headers=self.headers_b, json={
            "question": "Hariom la konta problem hota?",
            "session_id": session_b
        })
        self.assertEqual(resp.status_code, 200)
        ans = resp.get_json().get("answer", "")

        # User B should have no record of Hariom
        self.assertNotIn("hairfall", ans.lower())
        self.assertTrue("नोंद नाही" in ans or "कोणतीही माहिती" in ans or "कोणताही संदर्भ नाही" in ans or "तक्रार" in ans)

    def test_guest_user_cannot_read_or_write_entities(self):
        """Guest user (guest_xxx) must not persist or recall cross-chat entities."""
        guest_headers = {"Authorization": "Bearer guest_anonymous_123"}
        session_guest = f"session_guest_{uuid.uuid4().hex[:6]}"

        resp = self.client.post("/predict", headers=guest_headers, json={
            "question": "Maza mitra Hariom la hairfall cha problem aahe",
            "session_id": session_guest
        })
        self.assertEqual(resp.status_code, 200)

        # Check DB: no entities should exist for guest
        entities = memory_service.get_user_medical_entities("guest_anonymous_123")
        self.assertEqual(len(entities), 0, "Guest user must not persist medical entities in long-term memory")

    def test_unconsented_user_cannot_save_entities(self):
        """User with consent=False must not save cross-chat medical entities."""
        unconsented_user = f"user_unconsented_{uuid.uuid4().hex[:8]}"
        headers = {"Authorization": f"Bearer {unconsented_user}"}
        # Explicitly disable consent
        memory_service.set_user_consent(unconsented_user, False)

        resp = self.client.post("/predict", headers=headers, json={
            "question": "Maza mitra Hariom la hairfall cha problem aahe",
            "session_id": f"session_unconsented_{uuid.uuid4().hex[:6]}"
        })
        self.assertEqual(resp.status_code, 200)

        entities = memory_service.get_user_medical_entities(unconsented_user)
        self.assertEqual(len(entities), 0, "Unconsented user must not save medical entities")

    # -------------------------------------------------------------------------
    # 4. MARATHI OUTPUT CONSISTENCY TEST
    # -------------------------------------------------------------------------

    def test_marathi_input_produces_marathi_output(self):
        """Roman Marathi and Devanagari Marathi inputs must produce Marathi output with Devanagari script."""
        # Seed an entity
        memory_service.save_or_update_medical_entity(
            user_id=self.user_a,
            subject="Hariom",
            subject_relation="friend",
            concern="hairfall",
            raw_condition="Maza mitra Hariom la hairfall cha problem aahe",
            status="active"
        )

        resp = self.client.post("/predict", headers=self.headers_a, json={
            "question": "Hariom la konta problem hota?",
            "session_id": f"session_lang_{uuid.uuid4().hex[:6]}"
        })
        self.assertEqual(resp.status_code, 200)
        ans = resp.get_json().get("answer", "")

        # Check for Devanagari characters
        devanagari_chars = [c for c in ans if '\u0900' <= c <= '\u097F']
        self.assertTrue(len(devanagari_chars) > 10,
                        f"Expected Marathi answer with Devanagari script, got: {ans}")


if __name__ == "__main__":
    unittest.main()
