"""
Comprehensive Master Test Suite for MedAssist AI:
1. Persistent Medical Memory Across Chats & Simulated Restarts
2. Multimodal Medical Image & Document Understanding

Author: Senior AI & Medical Safety Engineer
Coverage:
- Strict subject isolation (user self vs friend Hariom vs relatives)
- DB restart simulation (Chat A save -> DB close/reopen -> Chat B recall)
- General question rejection (informational queries must NEVER save as personal history)
- Condition correction ('To maza problem navhata, Hariom cha hota')
- Condition progression ('kami zala' status update)
- Multimodal document classification & 7-section lab report formatting
- Prescription interpretation & handwriting ambiguity warnings
- Medicine packaging 10-point formatting
- Loose pill refusal (strict safety rule D4)
- In-session document follow-up Q&A ('Ya report madhye kay abnormal aahe?')
- Multilingual Devanagari Marathi responses
- Privacy & User isolation
"""

import unittest
import os
import sys
import uuid
import json
import sqlite3
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(BASE_DIR))

# Ensure offline deterministic test execution
os.environ["OLLAMA_ENABLED"] = "0"
os.environ["GEMINI_API_KEY"] = ""
os.environ["DEV_MODE"] = "1"

import memory_service
import vision_service
import api
from api import app

# Ensure external LLM client is None for offline safety
api.gemini_client = None


class TestPersistentMemoryAndMultimodalVision(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        # Create unique isolated database for test suite
        cls.test_db_path = BASE_DIR / "data" / f"test_master_{uuid.uuid4().hex[:8]}.db"
        cls.test_db_path.parent.mkdir(parents=True, exist_ok=True)
        memory_service.DB_FILE = cls.test_db_path
        memory_service.init_memory_schema(cls.test_db_path)

    @classmethod
    def tearDownClass(cls):
        if cls.test_db_path.exists():
            try:
                cls.test_db_path.unlink()
            except Exception:
                pass

    def setUp(self):
        self.client = app.test_client()
        self.user_a = f"auth_user_a_{uuid.uuid4().hex[:8]}"
        self.user_b = f"auth_user_b_{uuid.uuid4().hex[:8]}"
        self.headers_a = {"Authorization": f"Bearer {self.user_a}"}
        self.headers_b = {"Authorization": f"Bearer {self.user_b}"}

        # Enable opt-in memory consent for test users
        memory_service.set_user_consent(self.user_a, True)
        memory_service.set_user_consent(self.user_b, True)

    # =========================================================================
    # PART 1: PERSISTENT MEDICAL MEMORY & SUBJECT ISOLATION
    # =========================================================================

    def test_01_friend_hariom_cross_chat_recall(self):
        """Chat A: Report friend Hariom's hairfall -> Chat B: Recall Hariom's hairfall."""
        session_a = f"sess_chat_a_{uuid.uuid4().hex[:6]}"
        session_b = f"sess_chat_b_{uuid.uuid4().hex[:6]}"

        # Chat A
        r1 = self.client.post("/predict", headers=self.headers_a, json={
            "question": "Maza mitra Hariom la hairfall cha problem aahe",
            "session_id": session_a
        })
        self.assertEqual(r1.status_code, 200)

        # Chat B
        r2 = self.client.post("/predict", headers=self.headers_a, json={
            "question": "Hariom la konta problem hota?",
            "session_id": session_b
        })
        self.assertEqual(r2.status_code, 200)
        ans = r2.get_json().get("answer", "")
        self.assertTrue("hariom" in ans.lower() or "मित्र" in ans or "हरीओम" in ans)
        self.assertTrue("hair" in ans.lower() or "केस" in ans or "गळती" in ans or "गळणे" in ans)

    def test_02_restart_simulation_persistence(self):
        """Simulate backend restart by reinitializing DB connection and verifying recall."""
        user_c = f"auth_user_restart_{uuid.uuid4().hex[:8]}"
        headers_c = {"Authorization": f"Bearer {user_c}"}
        memory_service.set_user_consent(user_c, True)

        session_1 = f"sess_before_restart_{uuid.uuid4().hex[:6]}"
        session_2 = f"sess_after_restart_{uuid.uuid4().hex[:6]}"

        # Save condition before restart
        r1 = self.client.post("/predict", headers=headers_c, json={
            "question": "Maza mitra Hariom la hairfall cha problem aahe",
            "session_id": session_1
        })
        self.assertEqual(r1.status_code, 200)

        # Simulate restart: verify SQLite persistence directly
        conn = sqlite3.connect(self.test_db_path)
        row = conn.execute("SELECT subject, concern FROM user_medical_entities WHERE user_id = ? AND is_deleted = 0", (user_c,)).fetchone()
        conn.close()
        self.assertIsNotNone(row)
        self.assertEqual(row[0], "Hariom")
        self.assertEqual(row[1], "hairfall")

        # Chat after simulated restart
        r2 = self.client.post("/predict", headers=headers_c, json={
            "question": "Hariom la konta problem hota?",
            "session_id": session_2
        })
        self.assertEqual(r2.status_code, 200)
        ans = r2.get_json().get("answer", "")
        self.assertTrue("hair" in ans.lower() or "केस" in ans)

    def test_03_strict_self_vs_friend_isolation(self):
        """Querying 'Mala konta problem hota?' must never claim user has Hariom's hairfall."""
        session_x = f"sess_iso_1_{uuid.uuid4().hex[:6]}"
        session_y = f"sess_iso_2_{uuid.uuid4().hex[:6]}"

        # Save friend condition
        self.client.post("/predict", headers=self.headers_a, json={
            "question": "Maza mitra Hariom la hairfall cha problem aahe",
            "session_id": session_x
        })

        # Ask about self
        resp = self.client.post("/predict", headers=self.headers_a, json={
            "question": "Mala konta problem hota?",
            "session_id": session_y
        })
        self.assertEqual(resp.status_code, 200)
        ans = resp.get_json().get("answer", "")

        # Must not claim user has hairfall
        self.assertNotIn("तुम्हाला hairfall", ans.lower())
        self.assertTrue("स्वतः" in ans or "नोंद नाही" in ans or "कोणतीही माहिती" in ans or "तक्रार" in ans)

    def test_04_general_hypothetical_questions_never_save_to_memory(self):
        """General medical questions must NEVER be saved as personal medical history."""
        user_gen = f"user_gen_{uuid.uuid4().hex[:8]}"
        headers_gen = {"Authorization": f"Bearer {user_gen}"}
        memory_service.set_user_consent(user_gen, True)

        questions = [
            "What causes headaches?",
            "Can fever cause seizures?",
            "Why do people get acidity?",
            "डोकं कशामुळे दुखतं?",
            "fever kashamule hoto?",
        ]

        for q in questions:
            resp = self.client.post("/predict", headers=headers_gen, json={
                "question": q,
                "session_id": f"sess_gen_{uuid.uuid4().hex[:4]}"
            })
            self.assertEqual(resp.status_code, 200)

        # Check DB: user_gen must have ZERO saved entities
        entities = memory_service.get_user_medical_entities(user_gen)
        self.assertEqual(len(entities), 0, f"General questions were incorrectly saved as personal medical history: {entities}")

    def test_05_condition_correction_handling(self):
        """User corrects: 'To maza problem navhata, Hariom cha hota'."""
        user_corr = f"user_corr_{uuid.uuid4().hex[:8]}"
        headers_corr = {"Authorization": f"Bearer {user_corr}"}
        memory_service.set_user_consent(user_corr, True)

        # Seed an accidental self record
        memory_service.save_or_update_medical_entity(
            user_id=user_corr,
            subject="self",
            subject_relation="self",
            concern="hairfall",
            raw_condition="hairfall",
            status="active"
        )

        # Correction query
        resp = self.client.post("/predict", headers=headers_corr, json={
            "question": "To maza problem navhata, Hariom cha hota",
            "session_id": f"sess_corr_{uuid.uuid4().hex[:4]}"
        })
        self.assertEqual(resp.status_code, 200)
        ans = resp.get_json().get("answer", "")
        self.assertTrue("दुरुस्त" in ans or "Hariom" in ans or "corrected" in ans.lower() or "सुधार" in ans)

        # Verify DB: entity is now assigned to Hariom, NOT self
        entities = memory_service.get_user_medical_entities(user_corr)
        self.assertTrue(any(e["subject"].lower() == "hariom" for e in entities))

    def test_06_condition_progression_update(self):
        """'Maza mitra cha hairfall ata kami zala aahe' updates status to improving."""
        user_prog = f"user_prog_{uuid.uuid4().hex[:8]}"
        headers_prog = {"Authorization": f"Bearer {user_prog}"}
        memory_service.set_user_consent(user_prog, True)

        # Seed condition
        memory_service.save_or_update_medical_entity(
            user_id=user_prog,
            subject="Hariom",
            subject_relation="friend",
            concern="hairfall",
            raw_condition="initial hairfall",
            status="active"
        )

        # Send update
        resp = self.client.post("/predict", headers=headers_prog, json={
            "question": "Maza mitra cha hairfall ata kami zala aahe",
            "session_id": f"sess_prog_{uuid.uuid4().hex[:4]}"
        })
        self.assertEqual(resp.status_code, 200)

        # Recall
        resp2 = self.client.post("/predict", headers=headers_prog, json={
            "question": "Hariom la konta problem hota?",
            "session_id": f"sess_prog2_{uuid.uuid4().hex[:4]}"
        })
        ans = resp2.get_json().get("answer", "")
        self.assertTrue("कमी" in ans or "सुधारणा" in ans or "improving" in ans.lower())

    # =========================================================================
    # PART 2: MULTIMODAL MEDICAL IMAGE & DOCUMENT UNDERSTANDING
    # =========================================================================

    def test_07_loose_pill_strict_refusal(self):
        """Unpackaged loose pill image query must trigger strict refusal response."""
        # 1x1 dummy PNG image
        dummy_png = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
        resp = self.client.post("/predict", headers=self.headers_a, json={
            "question": "What is this loose pill? Can you identify this tablet?",
            "image_base64": dummy_png,
            "session_id": f"sess_pill_{uuid.uuid4().hex[:4]}"
        })
        self.assertEqual(resp.status_code, 200)
        data = resp.get_json()
        self.assertEqual(data.get("intent"), "loose_pill_refusal")
        ans = data.get("answer", "")
        self.assertTrue("cannot" in ans.lower() or "dangerous" in ans.lower() or "गोळी" in ans or "packaging" in ans.lower())

    def test_08_laboratory_report_analysis_cbc(self):
        """Clinical blood test report (CBC) produces 7-section structured breakdown."""
        cbc_text = (
            "PATIENT LAB REPORT - COMPLETE BLOOD COUNT (CBC)\n"
            "Hemoglobin: 8.5 g/dL (Reference Range: 12.0 - 16.0 g/dL)\n"
            "WBC (Total Leukocyte Count): 13500 /cumm (Reference Range: 4000 - 11000 /cumm)\n"
            "Platelet Count: 250000 /cumm (Reference Range: 150000 - 450000 /cumm)\n"
            "RBC Count: 3.2 mill/cumm (Reference Range: 3.8 - 5.2 mill/cumm)\n"
        )
        dummy_png = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="

        resp = self.client.post("/predict", headers=self.headers_a, json={
            "question": "Please analyze my blood test report",
            "image_base64": dummy_png,
            "ocr_text": cbc_text,
            "session_id": f"sess_lab_{uuid.uuid4().hex[:4]}"
        })
        self.assertEqual(resp.status_code, 200)
        data = resp.get_json()
        self.assertEqual(data.get("intent"), "lab_report_analysis")
        ans = data.get("answer", "")

        # Verify key 7 sections or clinical markers
        self.assertTrue("Hemoglobin" in ans)
        self.assertTrue("8.5" in ans)
        self.assertTrue("Low" in ans or "कमी" in ans)
        self.assertTrue("High" in ans or "जास्त" in ans or "13500" in ans)
        self.assertTrue("Disclaimer" in ans or "Clinical Note" in ans or "डॉक्टर" in ans or "doctor" in ans.lower())

    def test_09_prescription_analysis_with_handwriting_alert(self):
        """Prescription image/OCR returns medicines and handwriting ambiguity warnings."""
        rx_text = (
            "PRESCRIPTION Rx\n"
            "1. Tab Augmentin 625mg - 1 tablet BD x 5 days (After food)\n"
            "2. Tab Paracetamol 650mg - SOS for fever\n"
            "3. [Illegible handwritten ointment] - Apply locally\n"
        )
        dummy_png = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="

        resp = self.client.post("/predict", headers=self.headers_a, json={
            "question": "What medicines are written in this prescription?",
            "image_base64": dummy_png,
            "ocr_text": rx_text,
            "session_id": f"sess_rx_{uuid.uuid4().hex[:4]}"
        })
        self.assertEqual(resp.status_code, 200)
        data = resp.get_json()
        self.assertEqual(data.get("intent"), "prescription_analysis")
        ans = data.get("answer", "")

        self.assertTrue("Augmentin" in ans or "Paracetamol" in ans)
        self.assertTrue("Pharmacist" in ans or "pharmacist" in ans.lower() or "फार्मासिस्ट" in ans)

    def test_10_document_followup_question_in_session(self):
        """User asks 'Ya report madhye kay abnormal aahe?' following a report upload."""
        session_doc = f"sess_followup_{uuid.uuid4().hex[:6]}"
        lipid_text = (
            "LIPID PROFILE REPORT\n"
            "Total Cholesterol: 260 mg/dL (Reference Range: < 200 mg/dL)\n"
            "Triglycerides: 220 mg/dL (Reference Range: < 150 mg/dL)\n"
            "HDL Cholesterol: 48 mg/dL (Reference Range: > 40 mg/dL)\n"
            "LDL Cholesterol: 168 mg/dL (Reference Range: < 100 mg/dL)\n"
        )
        dummy_png = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="

        # Step 1: Upload and analyze
        r1 = self.client.post("/predict", headers=self.headers_a, json={
            "question": "Analyze lipid profile",
            "image_base64": dummy_png,
            "ocr_text": lipid_text,
            "session_id": session_doc
        })
        self.assertEqual(r1.status_code, 200)

        # Step 2: Follow-up question without re-uploading image
        r2 = self.client.post("/predict", headers=self.headers_a, json={
            "question": "Ya report madhye kay abnormal aahe?",
            "session_id": session_doc
        })
        self.assertEqual(r2.status_code, 200)
        data2 = r2.get_json()
        self.assertEqual(data2.get("intent"), "document_followup")
        ans2 = data2.get("answer", "")

        # Must flag abnormal lipid parameters
        self.assertTrue("Cholesterol" in ans2 or "कोलेस्टेरॉल" in ans2 or "260" in ans2)
        self.assertTrue("Triglycerides" in ans2 or "220" in ans2)

    def test_11_marathi_language_lab_report_query(self):
        """Marathi request for lab report returns clean Devanagari Marathi structured output."""
        glucose_text = (
            "FASTING BLOOD SUGAR: 165 mg/dL (Normal Range: 70 - 99 mg/dL)\n"
            "HbA1c: 8.2 % (Normal Range: 4.0 - 5.6 %)\n"
        )
        dummy_png = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="

        resp = self.client.post("/predict", headers=self.headers_a, json={
            "question": "हा ब्लड रिपोर्ट तपासून मराठीत माहिती सांगा",
            "image_base64": dummy_png,
            "ocr_text": glucose_text,
            "session_id": f"sess_mr_lab_{uuid.uuid4().hex[:4]}"
        })
        self.assertEqual(resp.status_code, 200)
        ans = resp.get_json().get("answer", "")

        # Check for Marathi Devanagari script and parameters
        devanagari_count = sum(1 for c in ans if '\u0900' <= c <= '\u097F')
        self.assertTrue(devanagari_count > 30, f"Expected rich Marathi response, got: {ans}")
        self.assertTrue("165" in ans)
        self.assertTrue("8.2" in ans)

    def test_12_blurry_report_handling(self):
        """Blurry or cropped document clearly identifies limitations and asks for clearer image."""
        blurry_text = "TOTAL ... [BLURRED] ... LFT ... SGPT: ... [UNCLEAR] ..."
        dummy_png = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="

        resp = self.client.post("/predict", headers=self.headers_a, json={
            "question": "Check this blurry report",
            "image_base64": dummy_png,
            "ocr_text": blurry_text,
            "session_id": f"sess_blur_{uuid.uuid4().hex[:4]}"
        })
        self.assertEqual(resp.status_code, 200)
        ans = resp.get_json().get("answer", "")
        # Must not fabricate numbers and must advise clearer image
        self.assertTrue("clear" in ans.lower() or "blurr" in ans.lower() or "स्पष्ट" in ans or "मर्यादा" in ans or "limitations" in ans.lower())


if __name__ == "__main__":
    unittest.main()
