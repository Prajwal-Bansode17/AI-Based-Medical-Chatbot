import os
import sys
import time
import unittest
from pathlib import Path
from unittest.mock import patch, MagicMock

BASE_DIR = Path(__file__).resolve().parent
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

import api
from conversation_engine import is_quota_exhausted_error, format_hf_chat_prompt


class TestHuggingFaceAndGeminiFallback(unittest.TestCase):
    def setUp(self):
        # Reset cooldown and pipeline state for tests
        api._gemini_quota_exhausted_until = 0.0
        api._local_hf_pipeline = None
        api._local_hf_pipeline_attempted = False
        api._last_hf_source = "remote"

    def test_is_quota_exhausted_error_detection(self):
        self.assertTrue(is_quota_exhausted_error("429 ResourceExhausted"))
        self.assertTrue(is_quota_exhausted_error("google.genai.errors.ClientError: 429 RESOURCE_EXHAUSTED"))
        self.assertTrue(is_quota_exhausted_error("Quota exceeded for quota metric..."))
        self.assertTrue(is_quota_exhausted_error("Rate limit exceeded"))
        self.assertFalse(is_quota_exhausted_error("503 Service Unavailable"))
        self.assertFalse(is_quota_exhausted_error("404 Not Found"))
        self.assertFalse(is_quota_exhausted_error(None))

    def test_format_hf_chat_prompt(self):
        prompt = format_hf_chat_prompt(
            system_prompt="You are a clinical AI",
            user_prompt="I have a fever",
            messages=[{"role": "user", "content": "Hello"}, {"role": "assistant", "content": "Hi"}],
        )
        self.assertIn("<|system|>\nYou are a clinical AI", prompt)
        self.assertIn("<|user|>\nHello", prompt)
        self.assertIn("<|assistant|>\nHi", prompt)

    @patch("api.query_hf_chat")
    def test_hf_inference_remote_success(self, mock_query):
        mock_query.return_value = "Remote HF generated advice."
        res = api._generate_huggingface(user_prompt="What should I take for mild fever?")
        self.assertEqual(res, "Remote HF generated advice.")
        self.assertEqual(api._last_hf_source, "remote")

    @patch("api.query_hf_chat")
    def test_hf_local_pipeline_success_when_remote_fails(self, mock_query):
        # Remote fails with HTTP 400 or network error
        mock_query.return_value = None

        mock_pipe = MagicMock()
        mock_pipe.return_value = [{"generated_text": "Local pipeline clinical response."}]

        with patch("api._get_local_hf_pipeline", return_value=mock_pipe):
            res = api._generate_huggingface(user_prompt="I have headache and fever")
            self.assertEqual(res, "Local pipeline clinical response.")
            self.assertEqual(api._last_hf_source, "local_pipeline")

    @patch("api.query_hf_chat")
    def test_hf_immediate_clinical_retriever_fallback_when_pipeline_unavailable(self, mock_query):
        # Remote fails
        mock_query.return_value = None

        # Local pipeline unavailable (torch/transformers missing)
        with patch("api._get_local_hf_pipeline", return_value=None):
            res = api._generate_huggingface(user_prompt="I have a severe fever and cough")
            self.assertIsNotNone(res)
            self.assertTrue(len(res) > 10)
            self.assertEqual(api._last_hf_source, "local_clinical_retriever")

    @patch("api.gemini_client")
    def test_gemini_429_eliminates_cascade_delay_and_no_retry(self, mock_client):
        # Simulate Gemini returning 429 ResourceExhausted
        mock_client.models.generate_content.side_effect = Exception("429 RESOURCE_EXHAUSTED: quota exceeded")

        t0 = time.monotonic()
        res = api._gemini_generate("Test prompt", correlation_id="test-429")
        elapsed = time.monotonic() - t0

        self.assertIsNone(res)
        # Should NOT retry repeatedly or sleep 15 seconds
        self.assertLess(elapsed, 2.0)
        # Quota cooldown should be active
        self.assertGreater(api._gemini_quota_exhausted_until, time.monotonic())
        # Should only have called once (no repeated candidate retries)
        self.assertEqual(mock_client.models.generate_content.call_count, 1)

    @patch("api.gemini_client")
    def test_gemini_quota_cooldown_skips_immediately(self, mock_client):
        api._gemini_quota_exhausted_until = time.monotonic() + 50.0

        t0 = time.monotonic()
        res = api._gemini_generate("Test prompt", correlation_id="test-cooldown")
        elapsed = time.monotonic() - t0

        self.assertIsNone(res)
        self.assertLess(elapsed, 0.1)
        mock_client.models.generate_content.assert_not_called()

    @patch("api.query_hf_chat")
    @patch("api.gemini_client")
    def test_gemini_429_immediately_serves_local_medical_retriever(self, mock_gemini, mock_hf):
        # HF disabled/none
        mock_hf.return_value = None
        # Gemini 429
        mock_gemini.models.generate_content.side_effect = Exception("429 RESOURCE_EXHAUSTED: Quota exceeded")

        # Explicitly configure Gemini as primary to test the 429 fallback journey
        with patch.dict(os.environ, {"AI_PRIMARY_PROVIDER": "gemini"}), \
             patch("api.OLLAMA_ENABLED", False):
            t0 = time.monotonic()
            ans, prov = api.generate_medical_response_with_provider(
                user_text="What are the symptoms and home remedies for migraine?",
                language="en",
            )
            elapsed = time.monotonic() - t0

            self.assertIsNotNone(ans)
            self.assertTrue(len(ans) > 20)
            self.assertEqual(prov, "local_medical_retrieval")
            self.assertLess(elapsed, 3.0)  # Zero 15-second delay!

    def test_predict_lab_report_context_long_text_no_400(self):
        client = api.app.test_client()
        # Simulated long lab report context (e.g. 3500 characters)
        long_lab_context = "[CLINICAL LAB REPORT CONTEXT]\nSummary: Routine health check.\n" + (
            "Flagged Parameters:\n- Hemoglobin: 10.2 g/dL (Normal: 12.0-15.5 g/dL, Status: Low)\n"
            "- Fasting Blood Sugar: 145 mg/dL (Normal: 70-99 mg/dL, Status: High)\n"
            "- Serum Creatinine: 1.1 mg/dL (Normal: 0.6-1.2 mg/dL, Status: Normal)\n"
        ) * 20 + "\nUser Question: What do these results mean?"

        res = client.post("/predict", json={
            "session_id": "test_lab_session_123",
            "text": long_lab_context,
            "user_id": "user_supabase_uuid_999",
            "user_name": "Aarav",
            "user_age": 30,
            "user_gender": "Male",
            "age": 30,
            "gender": "Male",
        })
        self.assertEqual(res.status_code, 200, f"Predict failed: {res.get_json()}")
        data = res.get_json()
        self.assertEqual(data.get("status"), "success")
        self.assertIsNotNone(data.get("answer"))

    def test_predict_null_or_missing_optional_fields_no_400(self):
        client = api.app.test_client()
        # All optional demographic fields null or missing
        res = client.post("/predict", json={
            "session_id": "test_null_demographics",
            "text": "I have mild fever and sore throat",
            "user_name": None,
            "user_age": None,
            "user_gender": None,
            "age": None,
            "gender": None,
            "ocr_text": None,
            "image_base64": None,
        })
        self.assertEqual(res.status_code, 200, f"Predict failed: {res.get_json()}")
        data = res.get_json()
        self.assertEqual(data.get("status"), "success")

    def test_predict_message_field_alternative_no_400(self):
        client = api.app.test_client()
        # Client sends 'message' instead of 'text'
        res = client.post("/predict", json={
            "session_id": "test_msg_field",
            "message": "What is the recommended treatment for dehydration?",
        })
        self.assertEqual(res.status_code, 200, f"Predict failed: {res.get_json()}")
        data = res.get_json()
        self.assertEqual(data.get("status"), "success")


if __name__ == "__main__":
    unittest.main()
