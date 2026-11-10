import os
import sys
import unittest
import json
from pathlib import Path
from unittest.mock import patch, MagicMock

BASE_DIR = Path(__file__).resolve().parent
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

from conversation_engine import (
    sanitize_kgmu_references,
    has_clinical_history_context,
    is_conversational_followup_query,
    is_greeting_message,
)
import api


class TestKgmuSanitizationAndFollowup(unittest.TestCase):
    def test_sanitize_kgmu_references(self):
        samples = [
            ("Visit https://kgmu.org for more information.", "Visit our medical center for more information."),
            ("King George's Medical University is located in Lucknow.", "MedAssist AI Hospital & Research Center is located in Lucknow."),
            ("Contact us at test@kgmu.org or info@kgmu.edu.", "Contact us at support@medassist.ai or info@medassist.ai."),
            ("Call the KGMU helpline today.", "Call the MedAssist AI Medical Center helpline today."),
            ("Normal text with no institution mentions.", "Normal text with no institution mentions.")
        ]
        for inp, expected in samples:
            out = sanitize_kgmu_references(inp)
            self.assertNotIn("kgmu.org", out.lower())
            self.assertNotIn("kgmu", out.lower())
            self.assertNotIn("king george", out.lower())

    def test_clinical_history_context_detection(self):
        # When history has lab report context
        history = [
            {"sender": "user", "message": "Here is my blood test report: Hemoglobin 9.2 g/dL, WBC 14000"},
            {"sender": "assistant", "message": "Your hemoglobin is low (mild anemia) and WBC is elevated."}
        ]
        self.assertTrue(has_clinical_history_context(history))

        # When history is purely casual
        casual_history = [
            {"sender": "user", "message": "Hi there"},
            {"sender": "assistant", "message": "Hello, how can I help you today?"}
        ]
        self.assertFalse(has_clinical_history_context(casual_history))

    def test_conversational_followup_detection(self):
        history = [
            {"sender": "user", "message": "My platelet count is 90000"},
            {"sender": "assistant", "message": "This indicates thrombocytopenia."}
        ]
        queries = [
            "explain me above results",
            "helo explain me",
            "what does this mean?",
            "can you explain the above report?",
            "is that dangerous?",
        ]
        for q in queries:
            self.assertTrue(
                is_conversational_followup_query(q, history),
                f"Failed for query: '{q}'"
            )

    def test_greeting_not_hijacked_when_asking_to_explain(self):
        # "helo explain me" should NOT be treated as a pure casual greeting
        self.assertFalse(is_greeting_message("helo explain me"))
        self.assertFalse(is_greeting_message("hello explain the results"))
        self.assertTrue(is_greeting_message("hello"))
        self.assertTrue(is_greeting_message("hi doctor"))

    def test_predict_followup_with_lab_context_avoids_general_fallback(self):
        client = api.app.test_client()
        session_id = "test_lab_session_12345"

        # Seed chat history with a lab report exchange
        api.save_chat_message(session_id, "user", "Blood test: Hemoglobin 8.5 g/dL, Creatinine 2.1 mg/dL")
        api.save_chat_message(session_id, "assistant", "Your Hemoglobin indicates anemia and Creatinine is elevated.")

        # Post conversational follow-up
        response = client.post(
            "/predict",
            json={
                "text": "helo explain me",
                "session_id": session_id,
                "language": "en"
            }
        )
        self.assertEqual(response.status_code, 200)
        data = response.get_json()

        # Must NOT be general_fallback
        self.assertNotEqual(data.get("source"), "general_fallback")
        self.assertTrue(data.get("medical_query"))
        self.assertEqual(data.get("intent"), "follow_up")
        self.assertNotIn("kgmu", data.get("answer", "").lower())
        self.assertNotIn("king george", data.get("answer", "").lower())


if __name__ == "__main__":
    unittest.main()
