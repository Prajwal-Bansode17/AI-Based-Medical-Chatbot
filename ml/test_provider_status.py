import os
import sys
import unittest
from pathlib import Path
from unittest.mock import patch, MagicMock

BASE_DIR = Path(__file__).resolve().parent
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

# Import the status function from api
from api import get_ai_status_fields


class TestAiProviderStatus(unittest.TestCase):
    """
    Tests get_ai_status_fields() provider-priority matrix to verify that
    status reporting accurately reflects runtime text and vision execution routing.
    Priority order under auto:
    1. Hugging Face (Custom Trained Model: MedAssist-AI-1B)
    2. Gemini
    3. Ollama (demoted / debug flag)
    """

    @patch("api.get_ollama_status")
    def test_auto_with_hf_enabled(self, mock_ollama_status):
        mock_ollama_status.return_value = {"reachable": True, "model_available": True}
        with patch.dict(os.environ, {"AI_PRIMARY_PROVIDER": "auto"}):
            with patch("api.HF_ENABLED", True), patch("api.OLLAMA_ENABLED", True), patch("api.gemini_client", MagicMock()):
                fields = get_ai_status_fields()
                self.assertEqual(fields["ai_primary_provider"], "huggingface")
                self.assertEqual(fields["vision_provider"], "gemini")
                self.assertTrue(fields["gemini_configured"])
                self.assertTrue(fields["ollama_enabled"])

    @patch("api.get_ollama_status")
    def test_auto_fallback_to_gemini_when_hf_disabled(self, mock_ollama_status):
        mock_ollama_status.return_value = {"reachable": True, "model_available": True}
        with patch.dict(os.environ, {"AI_PRIMARY_PROVIDER": "auto"}):
            with patch("api.HF_ENABLED", False), patch("api.OLLAMA_ENABLED", True), patch("api.gemini_client", MagicMock()):
                fields = get_ai_status_fields()
                self.assertEqual(fields["ai_primary_provider"], "gemini")
                self.assertEqual(fields["vision_provider"], "gemini")

    @patch("api.get_ollama_status")
    def test_auto_fallback_to_ollama_when_hf_and_gemini_disabled(self, mock_ollama_status):
        mock_ollama_status.return_value = {"reachable": True, "model_available": True}
        with patch.dict(os.environ, {"AI_PRIMARY_PROVIDER": "auto"}):
            with patch("api.HF_ENABLED", False), patch("api.OLLAMA_ENABLED", True), patch("api.gemini_client", None):
                fields = get_ai_status_fields()
                self.assertEqual(fields["ai_primary_provider"], "ollama")
                self.assertEqual(fields["vision_provider"], "none")

    @patch("api.get_ollama_status")
    def test_explicit_huggingface_preference(self, mock_ollama_status):
        mock_ollama_status.return_value = {"reachable": True, "model_available": True}
        with patch.dict(os.environ, {"AI_PRIMARY_PROVIDER": "huggingface"}):
            with patch("api.HF_ENABLED", True), patch("api.OLLAMA_ENABLED", True), patch("api.gemini_client", MagicMock()):
                fields = get_ai_status_fields()
                self.assertEqual(fields["ai_primary_provider"], "huggingface")
                self.assertEqual(fields["vision_provider"], "gemini")

    @patch("api.get_ollama_status")
    def test_explicit_gemini_preference(self, mock_ollama_status):
        mock_ollama_status.return_value = {"reachable": True, "model_available": True}
        with patch.dict(os.environ, {"AI_PRIMARY_PROVIDER": "gemini"}):
            with patch("api.HF_ENABLED", True), patch("api.OLLAMA_ENABLED", True), patch("api.gemini_client", MagicMock()):
                fields = get_ai_status_fields()
                self.assertEqual(fields["ai_primary_provider"], "gemini")
                self.assertEqual(fields["vision_provider"], "gemini")

    @patch("api.get_ollama_status")
    def test_explicit_ollama_preference(self, mock_ollama_status):
        mock_ollama_status.return_value = {"reachable": True, "model_available": True}
        with patch.dict(os.environ, {"AI_PRIMARY_PROVIDER": "ollama"}):
            with patch("api.HF_ENABLED", True), patch("api.OLLAMA_ENABLED", True), patch("api.gemini_client", MagicMock()):
                fields = get_ai_status_fields()
                self.assertEqual(fields["ai_primary_provider"], "ollama")

    @patch("api.get_ollama_status")
    def test_all_providers_unavailable(self, mock_ollama_status):
        mock_ollama_status.return_value = {"reachable": False, "model_available": False}
        with patch.dict(os.environ, {"AI_PRIMARY_PROVIDER": "auto"}):
            with patch("api.HF_ENABLED", False), patch("api.OLLAMA_ENABLED", False), patch("api.gemini_client", None):
                fields = get_ai_status_fields()
                self.assertEqual(fields["ai_primary_provider"], "none")
                self.assertEqual(fields["vision_provider"], "none")
                self.assertFalse(fields["gemini_configured"])
                self.assertFalse(fields["ollama_enabled"])


if __name__ == "__main__":
    unittest.main()
