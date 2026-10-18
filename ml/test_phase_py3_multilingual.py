"""
Test suite for Phase PY-3:
- Language detection across English, Hindi, Marathi, Hinglish, Maraglish, short tokens
- Symptom extraction under mixed negation, multiple symptoms, transliteration
- Multilingual duration extraction (English, Hindi, Marathi, Devanagari numerals)
- Emergency detection across languages (English, Hindi, Marathi, transliterated)
- Follow-up query, medicine query, and non-medical greeting classification
"""
import os
import sys
import unittest
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(BASE_DIR))
os.environ.setdefault("OLLAMA_ENABLED", "0")

import importlib.util
spec = importlib.util.spec_from_file_location("api_module", BASE_DIR / "api.py")
api = importlib.util.module_from_spec(spec)
spec.loader.exec_module(api)


class TestPhasePY3MultilingualClinical(unittest.TestCase):

    def setUp(self):
        self.client = api.app.test_client()

    # ================================================================
    # 1. Multilingual Language Detection
    # ================================================================
    def test_language_detection_core_examples(self):
        cases = [
            ("I have had a fever since yesterday.", "en"),
            ("मुझे कल से बुखार है।", "hi"),
            ("mujhe kal se bukhar hai", "hi"),
            ("मला कालपासून ताप आहे.", "mr"),
            ("mala kalpasun tap aahe", "mr"),
            ("Mala 2 days se fever aa raha hai.", "mr"),
        ]
        for text, expected in cases:
            with self.subTest(text=text):
                self.assertEqual(api.detect_language(text), expected)

    def test_language_detection_short_english_tokens_not_misclassified(self):
        tokens = ["to", "me", "so", "or", "in", "on", "at", "an", "is", "it", "if", "we", "he", "no", "do", "go"]
        for token in tokens:
            with self.subTest(token=token):
                self.assertEqual(api.detect_language(token), "en")

    # ================================================================
    # 2. Symptom Extraction & Negation Handling
    # ================================================================
    def test_symptom_mixed_negation_in_one_sentence(self):
        # Only the negated symptom should be excluded; affirmative symptoms preserved
        cases = [
            ("I have a headache but no fever", ["headache"]),
            ("no fever but severe cough", ["cough"]),
            ("fever nahi hai par khansi hai", ["cough"]),
            ("mala tap nahi pan khokla aahe", ["cough"]),
            ("I do not have cough, only headache and cold", ["headache", "cold"]),
            ("mala potat dukh nahi pan sardi aani khokla aahe", ["cold", "cough"]),
        ]
        for text, expected in cases:
            with self.subTest(text=text):
                detected = api.extract_symptoms(text)
                self.assertEqual(sorted(detected), sorted(expected))

    def test_symptom_extraction_does_not_infer_unmentioned_symptoms(self):
        self.assertEqual(api.extract_symptoms("I need to see a doctor for advice"), [])
        self.assertEqual(api.extract_symptoms("Can you explain how human biology works?"), [])
        self.assertEqual(api.extract_symptoms("Where is the hospital located?"), [])

    # ================================================================
    # 3. Multilingual Duration Extraction
    # ================================================================
    def test_multilingual_duration_expressions(self):
        cases = [
            ("I have had a fever since 3 days", "3 days"),
            ("kal se bukhar hai", "1 day"),
            ("do din se bukhar hai", "2 days"),
            ("don divasapasun tap aahe", "2 days"),
            ("teen din se", "3 days"),
            ("char divas", "4 days"),
            ("since yesterday", "1 day"),
            ("कल से बुखार है", "1 day"),
            ("२ दिन से", "2 days"),
            ("२ दिवस झाले", "2 days"),
            ("since 2 weeks", "2 weeks"),
            ("5 hours ago", "5 hours"),
        ]
        for text, expected in cases:
            with self.subTest(text=text):
                self.assertEqual(api.extract_duration(text), expected)

    # ================================================================
    # 4. Emergency Triage Across Languages & Transliterations
    # ================================================================
    def test_emergency_triage_multilingual(self):
        emergencies = [
            # English
            "I have severe chest pain and cannot breathe",
            "coughing up blood heavily",
            # Hindi Devanagari
            "सीने में बहुत तेज दर्द है और सांस नहीं आ रही",
            "खूनी उल्टी हो रही है बहुत खून आ रहा है",
            # Hindi Roman / Hinglish
            "khoon ki ulti ho rahi hai",
            "mere seene me dard hai aur saans nahi aa rahi",
            # Marathi Devanagari
            "छातीत खूप दुखत आहे आणि श्वास घेता येत नाही",
            "रक्ताची उलटी होत आहे खूप रक्त येत आहे",
            # Marathi Roman / Maraglish
            "rakta chi ulti hot aahe",
            "chhatit dukh aani shwas ghet nahi yet",
        ]
        for phrase in emergencies:
            with self.subTest(phrase=phrase):
                norm = api.normalize_text(phrase)
                flag = api.detect_safety_flag(phrase, phrase, norm)
                self.assertEqual(flag, "emergency")

    # ================================================================
    # 5. Intent and Context Disambiguation
    # ================================================================
    def test_greeting_vs_medical_query(self):
        self.assertTrue(api.is_greeting("hello"))
        self.assertTrue(api.is_greeting("namaste"))
        self.assertTrue(api.is_greeting("namaskar"))
        self.assertFalse(api.is_medical_query("hello"))
        self.assertFalse(api.is_medical_query("namaskar"))
        self.assertTrue(api.is_medical_query("I have fever and headache"))
        self.assertTrue(api.is_medical_query("mala tap aahe"))

    def test_followup_detection_with_context(self):
        # Short clinical descriptor reply during an ongoing symptom conversation is classified as follow-up
        self.assertTrue(api.is_contextual_followup("Two days, lower back", previous_symptoms=["back_pain"]))
        self.assertTrue(api.is_contextual_followup("since yesterday", previous_symptoms=["fever"]))
        self.assertTrue(api.is_contextual_followup("it is getting worse", previous_symptoms=["cough"]))
        self.assertTrue(api.is_contextual_followup("that pain", previous_symptoms=["stomach_pain"]))

        # Non-followup question without previous symptoms is False
        self.assertFalse(api.is_contextual_followup("What is Python?", previous_symptoms=[]))
        self.assertFalse(api.is_contextual_followup("What is diabetes?", previous_symptoms=[]))
        self.assertFalse(api.is_contextual_followup("today is Monday", previous_symptoms=[]))


if __name__ == "__main__":
    unittest.main()
