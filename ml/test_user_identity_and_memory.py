"""
Tests for Persistent Memory & User Identity Context in MedAssist AI.
Tests save_user_profile, get_user_profile, detect_identity_query_intent,
build_identity_response, and end-to-end /predict and /chat responses.
"""

import sys
import unittest
import json
import uuid
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

from memory_service import save_user_profile, get_user_profile, init_memory_schema
from conversation_engine import detect_identity_query_intent, build_identity_response
from api import app


class TestUserIdentityAndMemory(unittest.TestCase):

    def setUp(self):
        init_memory_schema()
        self.app = app.test_client()
        self.app.testing = True

    def test_save_and_get_user_profile_by_user_id(self):
        uid = f"usr_{uuid.uuid4().hex[:8]}"
        saved = save_user_profile(user_id=uid, user_name="Aarav Sharma", age=32, gender="Male")
        self.assertTrue(saved)

        prof = get_user_profile(user_id=uid)
        self.assertEqual(prof.get("user_name"), "Aarav Sharma")
        self.assertEqual(prof.get("age"), 32)
        self.assertEqual(prof.get("gender"), "Male")

    def test_save_and_get_user_profile_by_session_fallback(self):
        sid = f"sess_{uuid.uuid4().hex[:8]}"
        saved = save_user_profile(user_id="guest", user_name="Sneha Patil", age=26, gender="Female", session_id=sid)
        self.assertTrue(saved)

        prof = get_user_profile(user_id="guest", session_id=sid)
        self.assertEqual(prof.get("user_name"), "Sneha Patil")
        self.assertEqual(prof.get("age"), 26)
        self.assertEqual(prof.get("gender"), "Female")

    def test_detect_identity_query_intent_english(self):
        self.assertTrue(detect_identity_query_intent("what is my name?"))
        self.assertTrue(detect_identity_query_intent("do you know my name"))
        self.assertTrue(detect_identity_query_intent("who am i"))
        self.assertTrue(detect_identity_query_intent("tell me my name please"))
        self.assertTrue(detect_identity_query_intent("can you tell me my name"))

    def test_detect_identity_query_intent_marathi(self):
        self.assertTrue(detect_identity_query_intent("माझं नाव काय आहे?"))
        self.assertTrue(detect_identity_query_intent("माझे नाव काय"))
        self.assertTrue(detect_identity_query_intent("मी कोण आहे"))
        self.assertTrue(detect_identity_query_intent("maz naav kay ahe"))

    def test_detect_identity_query_intent_hindi(self):
        self.assertTrue(detect_identity_query_intent("मेरा नाम क्या है?"))
        self.assertTrue(detect_identity_query_intent("मैं कौन हूँ"))
        self.assertTrue(detect_identity_query_intent("mera naam kya hai"))

    def test_detect_identity_query_negative(self):
        self.assertFalse(detect_identity_query_intent("what is paracetamol?"))
        self.assertFalse(detect_identity_query_intent("I have high fever"))
        self.assertFalse(detect_identity_query_intent("how to lower creatinine"))

    def test_build_identity_response_with_name(self):
        en_resp = build_identity_response("Hariom", age=24, language="en")
        self.assertIn("Hariom", en_resp)
        self.assertIn("24 years old", en_resp)

        mr_resp = build_identity_response("हर्षदा", age=28, language="mr")
        self.assertIn("हर्षदा", mr_resp)
        self.assertIn("वय 28 वर्षे", mr_resp)

        hi_resp = build_identity_response("राहुल", age=35, language="hi")
        self.assertIn("राहुल", hi_resp)
        self.assertIn("उम्र 35 वर्ष", hi_resp)

    def test_build_identity_response_without_name(self):
        en_resp = build_identity_response(None, language="en")
        self.assertIn("don't have your name", en_resp.lower())

    def test_predict_identity_query_with_passed_name(self):
        sid = f"test_id_sess_{uuid.uuid4().hex[:8]}"
        payload = {
            "session_id": sid,
            "text": "Do you know what is my name?",
            "user_name": "Dr. Ramesh",
            "user_age": 45,
            "user_gender": "Male",
        }
        resp = self.app.post("/predict", data=json.dumps(payload), content_type="application/json")
        self.assertEqual(resp.status_code, 200)
        data = resp.get_json()
        self.assertEqual(data.get("intent"), "user_identity")
        self.assertIn("Dr. Ramesh", data.get("answer", ""))

    def test_predict_identity_retention_across_turns(self):
        sid = f"test_id_turn_{uuid.uuid4().hex[:8]}"
        # Turn 1: user registers name during a medical symptom query
        turn1 = {
            "session_id": sid,
            "text": "Hello, my name is Pooja Kulkarni",
            "user_name": "Pooja Kulkarni",
            "user_age": 29,
        }
        resp1 = self.app.post("/predict", data=json.dumps(turn1), content_type="application/json")
        self.assertEqual(resp1.status_code, 200)

        # Turn 2: user asks "what is my name?" without re-passing user_name
        turn2 = {
            "session_id": sid,
            "text": "what is my name?",
        }
        resp2 = self.app.post("/predict", data=json.dumps(turn2), content_type="application/json")
        self.assertEqual(resp2.status_code, 200)
        data2 = resp2.get_json()
        self.assertEqual(data2.get("intent"), "user_identity")
        self.assertIn("Pooja Kulkarni", data2.get("answer", ""))

    def test_chat_identity_query(self):
        sid = f"test_chat_id_{uuid.uuid4().hex[:8]}"
        payload = {
            "session_id": sid,
            "message": "माझं नाव काय आहे?",
            "user_name": "अमित देशमुख",
            "language": "mr",
        }
        resp = self.app.post("/chat", data=json.dumps(payload), content_type="application/json")
        self.assertEqual(resp.status_code, 200)
        data = resp.get_json()
        self.assertIn("अमित देशमुख", data.get("response", ""))


if __name__ == "__main__":
    unittest.main()
