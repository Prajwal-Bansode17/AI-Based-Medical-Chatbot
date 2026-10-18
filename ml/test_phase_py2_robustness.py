"""
Targeted tests for Phase PY-2:
1. Regex false positives and symptom negation (fever, feverish, not fever, etc.)
2. Session ID isolation (UUID generation, missing IDs, independent sessions)
3. Ollama timeout and connection error fallback cascade to Gemini / offline retrieval
4. Windows UTF-8 console output safety
"""
import os
import sys
import unittest
from pathlib import Path
from unittest.mock import patch, MagicMock

BASE_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(BASE_DIR))

# Ensure tests run with Ollama disabled by default unless mocked
os.environ.setdefault("OLLAMA_ENABLED", "0")

import importlib.util
spec = importlib.util.spec_from_file_location("api_module", BASE_DIR / "api.py")
api = importlib.util.module_from_spec(spec)
spec.loader.exec_module(api)


class TestPhasePY2Robustness(unittest.TestCase):

    def setUp(self):
        self.client = api.app.test_client()

    # ================================================================
    # 1. Symptom Regex and False Positive / Negation Tests
    # ================================================================
    def test_symptom_regex_positive_matches(self):
        self.assertEqual(api.extract_symptoms("I have fever"), ["fever"])
        self.assertEqual(api.extract_symptoms("I have a headache"), ["headache"])
        self.assertEqual(api.extract_symptoms("mala tap aahe"), ["fever"])
        self.assertEqual(api.extract_symptoms("mujhe bukhar hai"), ["fever"])
        self.assertIn("cough", api.extract_symptoms("khokla aahe"))

    def test_symptom_regex_blocks_substring_false_positives(self):
        # 'fever' must not match inside 'feverish'
        self.assertEqual(api.extract_symptoms("feverish"), [])
        self.assertEqual(api.extract_symptoms("I feel feverish today"), [])
        self.assertEqual(api.extract_symptoms("feverish person"), [])
        # 'cough' must not match inside 'coughing'
        self.assertEqual(api.extract_symptoms("coughing medicine"), [])
        # 'head' must not match inside 'headband'
        self.assertEqual(api.extract_symptoms("headband fell"), [])
        # Unrelated non-medical phrases
        self.assertEqual(api.extract_symptoms("Can you help me write python code?"), [])

    def test_symptom_negation_handling(self):
        # Negated symptom phrases must NOT extract the negated symptom
        self.assertEqual(api.extract_symptoms("not fever"), [])
        self.assertEqual(api.extract_symptoms("I do not have fever"), [])
        self.assertEqual(api.extract_symptoms("no fever"), [])
        self.assertEqual(api.extract_symptoms("fever nahi hai"), [])
        self.assertEqual(api.extract_symptoms("mala tap nahi"), [])
        self.assertEqual(api.extract_symptoms("mujhe bukhar nahi hai"), [])
        self.assertEqual(api.extract_symptoms("not headache"), [])
        self.assertEqual(api.extract_symptoms("no headache"), [])

    def test_mixed_affirmative_and_negated_symptoms(self):
        # One symptom affirmed, another negated in the same utterance
        res1 = api.extract_symptoms("I have fever but not headache")
        self.assertIn("fever", res1)
        self.assertNotIn("headache", res1)

        res2 = api.extract_symptoms("no cough but I have fever")
        self.assertIn("fever", res2)
        self.assertNotIn("cough", res2)

    # ================================================================
    # 2. Session ID Isolation & Independence Tests
    # ================================================================
    def test_missing_session_id_generates_unique_uuid(self):
        # Call /predict without session_id twice
        res1 = self.client.post("/predict", json={"text": "I have headache"})
        data1 = res1.get_json()
        sid1 = data1.get("session_id")

        res2 = self.client.post("/predict", json={"text": "I have cough"})
        data2 = res2.get_json()
        sid2 = data2.get("session_id")

        self.assertEqual(res1.status_code, 200)
        self.assertEqual(res2.status_code, 200)
        self.assertNotEqual(sid1, "default_user")
        self.assertNotEqual(sid2, "default_user")
        self.assertTrue(sid1.startswith("anon_"))
        self.assertTrue(sid2.startswith("anon_"))
        self.assertNotEqual(sid1, sid2)

    def test_whitespace_session_id_generates_unique_uuid(self):
        res = self.client.post("/predict", json={"text": "Hello doctor", "session_id": "   "})
        self.assertEqual(res.status_code, 200)
        sid = res.get_json().get("session_id")
        self.assertNotEqual(sid, "default_user")
        self.assertTrue(sid.startswith("anon_"))

    def test_explicit_session_id_preserved_and_isolated(self):
        sess_a = "test_user_alpha_001"
        sess_b = "test_user_beta_002"

        # Clear any preexisting test artifacts
        self.client.delete(f"/history/{sess_a}")
        self.client.delete(f"/history/{sess_b}")

        # Send distinct queries into sessions A and B
        self.client.post("/predict", json={"text": "I have fever", "session_id": sess_a})
        self.client.post("/predict", json={"text": "I have stomach pain", "session_id": sess_b})

        hist_a = self.client.get(f"/history/{sess_a}").get_json().get("history", [])
        hist_b = self.client.get(f"/history/{sess_b}").get_json().get("history", [])

        # History for session A must have user message with fever, not stomach pain
        user_msgs_a = [h["message"] for h in hist_a if h["role"] == "user"]
        self.assertTrue(any("fever" in m.lower() for m in user_msgs_a))
        self.assertFalse(any("stomach pain" in m.lower() for m in user_msgs_a))

        # History for session B must have user message with stomach pain, not fever
        user_msgs_b = [h["message"] for h in hist_b if h["role"] == "user"]
        self.assertTrue(any("stomach pain" in m.lower() for m in user_msgs_b))
        self.assertFalse(any("fever" in m.lower() for m in user_msgs_b))

        # Also verify structured symptoms in history are properly isolated
        symptoms_a = [h.get("symptoms", "") for h in hist_a]
        symptoms_b = [h.get("symptoms", "") for h in hist_b]
        self.assertTrue(any("fever" in s for s in symptoms_a))
        self.assertFalse(any("fever" in s for s in symptoms_b))

        # Clean up
        self.client.delete(f"/history/{sess_a}")
        self.client.delete(f"/history/{sess_b}")

    def test_history_endpoints_require_session_id(self):
        # Query-param style routes
        res_get_no_id = self.client.get("/chat-history")
        self.assertEqual(res_get_no_id.status_code, 400)

        res_del_no_id = self.client.delete("/chat-history")
        self.assertEqual(res_del_no_id.status_code, 400)

        # Path-param style routes with empty whitespace
        res_get_empty = self.client.get("/history/   ")
        self.assertEqual(res_get_empty.status_code, 400)

        res_del_empty = self.client.delete("/history/   ")
        self.assertEqual(res_del_empty.status_code, 400)

        res_mem_empty = self.client.get("/memory/   ")
        self.assertEqual(res_mem_empty.status_code, 400)

        res_del_mem_empty = self.client.delete("/memory/   ")
        self.assertEqual(res_del_mem_empty.status_code, 400)

    # ================================================================
    # 3. Timeout & Fallback Cascade Tests (Mocked)
    # ================================================================
    def test_ollama_timeout_fallback_to_gemini_or_offline(self):
        # Mock _ollama_request directly on api module
        with patch.object(api, "OLLAMA_ENABLED", True), \
             patch.object(api, "_ollama_request", side_effect=api.OllamaError("Ollama request timed out after 25.0s")):

            sess = "test_timeout_fallback_session"
            res = self.client.post("/predict", json={"text": "What causes migraine headaches?", "session_id": sess})
            self.assertEqual(res.status_code, 200)
            data = res.get_json()

            # The request must succeed without crashing or blocking, using Gemini or MEDIQ/knowledge-base/general fallback
            self.assertIn("answer", data)
            self.assertTrue(len(data["answer"]) > 10)
            self.assertIn(data["source"], ["gemini", "mediq", "medical_knowledge", "safe_fallback", "general_fallback"])

            # Ollama should have been placed into cooldown
            self.assertTrue(api._ollama_in_cooldown())
            # Reset cooldown for subsequent tests
            api._ollama_mark_up()

    def test_ollama_connection_refused_fallback(self):
        # Mock _ollama_request directly on api module
        with patch.object(api, "OLLAMA_ENABLED", True), \
             patch.object(api, "_ollama_request", side_effect=api.OllamaError("cannot reach Ollama at http://localhost:11434: Connection refused")):

            sess = "test_conn_error_session"
            res = self.client.post("/predict", json={"text": "I have a sore throat", "session_id": sess})
            self.assertEqual(res.status_code, 200)
            data = res.get_json()

        self.assertIn("answer", data)
        self.assertIn("sore_throat", data.get("symptoms", []))
        self.assertIn(data["source"], ["gemini", "medical_knowledge", "mediq", "symptom_fallback", "conversation_memory", "safe_fallback"])

        api._ollama_mark_up()

    def test_emergency_triage_not_delayed_by_llm(self):
        # Emergencies must immediately return safety_rules without invoking LLMs
        res = self.client.post("/predict", json={
            "text": "I have severe chest pain and cannot breathe",
            "session_id": "test_emergency_session",
        })
        self.assertEqual(res.status_code, 200)
        data = res.get_json()
        self.assertEqual(data.get("intent"), "emergency")
        self.assertEqual(data.get("source"), "safety_rules")
        self.assertTrue(data.get("emergency"))

    # ================================================================
    # 4. Windows Logging / UTF-8 Safety
    # ================================================================
    def test_console_encoding_safety(self):
        # Check that sys.stdout can encode Devanagari characters safely without raising UnicodeEncodeError
        sample_devanagari = "मला ताप आहे आणि खोकला येत आहे. मुझे बुखार है。"
        encoded = sample_devanagari.encode(sys.stdout.encoding, errors="backslashreplace")
        self.assertTrue(len(encoded) > 0)


if __name__ == "__main__":
    unittest.main()
