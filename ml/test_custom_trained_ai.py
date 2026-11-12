"""
Tests for MedAssist 100% Keyless Self-Hosted Architecture & Custom Trained AI.
Verifies:
1. Default keyless flags (ENABLE_CLOUD_GEMINI=False, HF_ENABLE_REMOTE_INFERENCE=False).
2. RAM-aware guard in CustomTrainedMedicalLLM (< 1.2 GB skips GGUF, >= 1.2 GB allows).
3. Live Web Medical Search and vernacular query mapping for Roman Marathi / Hinglish.
4. End-to-end /predict response logic without cloud API keys or hospital roster hallucinations.
"""

import unittest
from unittest.mock import patch, MagicMock
from pathlib import Path
import os
import sys

# Ensure ml directory is on sys.path
ML_DIR = os.path.dirname(os.path.abspath(__file__))
if ML_DIR not in sys.path:
    sys.path.insert(0, ML_DIR)

from custom_medical_llm import CustomTrainedMedicalLLM, get_available_ram_gb
from web_medical_search import search_medical_web, _map_query_to_clinical_search
import api


class TestCustomTrainedAI(unittest.TestCase):

    def test_default_keyless_configuration(self):
        """Verifies that cloud Gemini and HF remote inference are disabled by default."""
        self.assertFalse(api.ENABLE_CLOUD_GEMINI)
        self.assertFalse(api.HF_ENABLE_REMOTE_INFERENCE)

    def test_ram_guard_below_threshold(self):
        """Simulates low-RAM environment (< 1.2 GB, e.g., Render free tier 512 MB)."""
        llm = CustomTrainedMedicalLLM(model_path="dummy.gguf")
        with patch("custom_medical_llm.get_available_ram_gb", return_value=0.5):
            with patch.dict(os.environ, {"FORCE_LOAD_GGUF": "false"}):
                self.assertFalse(llm.can_load())

    def test_ram_guard_above_threshold(self):
        """Simulates sufficient RAM (>= 1.2 GB) and installed Llama."""
        llm = CustomTrainedMedicalLLM(model_path="dummy.gguf")
        with patch("custom_medical_llm.Llama", MagicMock()):
            with patch("custom_medical_llm.get_available_ram_gb", return_value=3.5):
                with patch.object(Path, "exists", return_value=True):
                    self.assertTrue(llm.can_load())

    def test_ram_guard_force_override(self):
        """Verifies that FORCE_LOAD_GGUF=true bypasses the low RAM guard."""
        llm = CustomTrainedMedicalLLM(model_path="dummy.gguf")
        with patch("custom_medical_llm.Llama", MagicMock()):
            with patch("custom_medical_llm.get_available_ram_gb", return_value=0.4):
                with patch.dict(os.environ, {"FORCE_LOAD_GGUF": "true"}):
                    with patch.object(Path, "exists", return_value=True):
                        self.assertTrue(llm.can_load())

    def test_web_medical_search_vernacular_warm_water(self):
        """Verifies vernacular mapping for 'koshmat garam pani mhanje kay?'."""
        res = search_medical_web("koshmat garam pani mhanje kay?", language="mr")
        self.assertIsNotNone(res)
        self.assertIn("पाणी", res)
        self.assertIn("हायड्रेटेड", res)

    def test_web_medical_search_vernacular_steam(self):
        """Verifies vernacular mapping for 'vaf(steam) kasa gheu?'."""
        res = search_medical_web("vaf(steam) kasa gheu?", language="mr")
        self.assertIsNotNone(res)
        self.assertIn("वाफ", res)
        self.assertIn("नाक", res)

    def test_predict_endpoint_roman_marathi_clean_response(self):
        """Ensures /predict handles Roman Marathi queries without returning High Cholesterol or hospital rosters."""
        client = api.app.test_client()
        resp = client.post(
            "/predict",
            json={
                "message": "koshmat garam pani mhanje kay?",
                "user_id": "test_user_keyless",
            },
        )
        self.assertEqual(resp.status_code, 200)
        data = resp.get_json()
        ans = data.get("response", "")
        self.assertNotIn("उच्च कोलेस्ट्रॉल", ans)
        self.assertNotIn("King George", ans)
        self.assertNotIn("KGMU", ans)
        self.assertTrue(len(ans) > 20)


if __name__ == "__main__":
    unittest.main()
