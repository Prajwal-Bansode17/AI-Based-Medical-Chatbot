import unittest
import json
import sys
from pathlib import Path

# Ensure ml directory is on sys.path
BASE_ML_DIR = Path(__file__).resolve().parent
if str(BASE_ML_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_ML_DIR))

from api import app, init_local_finetune_retriever, retrieve_local_finetune_answer
from conversation_engine import (
    is_lab_report_context_prompt,
    is_faculty_or_staff_roster,
    build_lab_report_interpretation_response,
)


class TestLabReportBypassAndFacultyPurge(unittest.TestCase):

    def setUp(self):
        self.client = app.test_client()

    def test_is_lab_report_context_detection(self):
        self.assertTrue(is_lab_report_context_prompt("[CLINICAL LAB REPORT CONTEXT]\nSummary: test"))
        self.assertTrue(is_lab_report_context_prompt("Flagged Parameters:\n- Hemoglobin: 9.8 g/dL"))
        self.assertTrue(is_lab_report_context_prompt("Biomarker Decoder Summary:\nNormal"))
        self.assertTrue(is_lab_report_context_prompt("[lab report context] here are results"))
        self.assertFalse(is_lab_report_context_prompt("I have a mild headache since yesterday"))
        self.assertFalse(is_lab_report_context_prompt("What is paracetamol dose?"))

    def test_is_faculty_or_staff_roster_detection(self):
        self.assertTrue(is_faculty_or_staff_roster("Department of Obstetrics & Gynecology doctors list"))
        self.assertTrue(is_faculty_or_staff_roster("Here is the faculty directory for the department"))
        self.assertTrue(is_faculty_or_staff_roster("Clinical hematology opd doctor schedule"))
        self.assertTrue(is_faculty_or_staff_roster("List of doctors in oncology"))
        self.assertTrue(is_faculty_or_staff_roster("Dr Bindu in the faculty directory"))
        self.assertFalse(is_faculty_or_staff_roster("Low hemoglobin indicates anemia, eat spinach and beetroots."))
        self.assertFalse(is_faculty_or_staff_roster("Elevated WBC count suggests the immune system is responding to infection."))

    def test_build_lab_report_interpretation_response_english(self):
        sample = (
            "[CLINICAL LAB REPORT CONTEXT]\n"
            "Summary: 2 out-of-range parameters\n"
            "Flagged Parameters:\n"
            "- Hemoglobin: 9.8 g/dL (Standard Range: 12.0 - 17.5 g/dL, Status: low)\n"
            "- Total WBC: 12500 cells/mcL (Standard Range: 4000 - 11000 cells/mcL, Status: high)\n\n"
            "User Question: What do these numbers mean?"
        )
        res = build_lab_report_interpretation_response(sample, language="en")
        self.assertIn("1. Quick Summary", res)
        self.assertIn("2. Clinical Significance & Normal Ranges", res)
        self.assertIn("3. Dietary & Lifestyle Advice", res)
        self.assertIn("4. Recommended Questions for Your Doctor", res)
        self.assertIn("Hemoglobin", res)
        self.assertIn("WBC", res)
        self.assertNotIn("Obstetrics & Gynecology", res)
        self.assertNotIn("faculty", res.lower())

    def test_build_lab_report_interpretation_response_marathi(self):
        sample = (
            "[CLINICAL LAB REPORT CONTEXT]\n"
            "Flagged Parameters:\n"
            "- Hemoglobin: 9.8 g/dL (Standard Range: 12.0 - 17.5 g/dL, Status: low)\n"
            "- Total WBC: 12500 cells/mcL (Standard Range: 4000 - 11000 cells/mcL, Status: high)\n"
        )
        res = build_lab_report_interpretation_response(sample, language="mr")
        self.assertIn("संक्षिप्त सारांश", res)
        self.assertIn("वैद्यकीय महत्त्व", res)
        self.assertIn("आहार आणि जीवनशैली सल्ला", res)
        self.assertIn("प्रश्न", res)
        self.assertNotIn("faculty", res.lower())

    def test_predict_endpoint_lab_report_bypass(self):
        payload = {
            "text": (
                "[CLINICAL LAB REPORT CONTEXT]\n"
                "Flagged Parameters:\n"
                "- Hemoglobin: 9.8 g/dL (Standard Range: 12.0 - 17.5 g/dL, Status: low)\n"
                "- Total WBC: 12500 cells/mcL (Standard Range: 4000 - 11000 cells/mcL, Status: high)\n"
                "User Question: Please explain this report"
            ),
            "session_id": "test_lab_session_1",
            "language": "en"
        }
        resp = self.client.post("/predict", json=payload)
        self.assertEqual(resp.status_code, 200)
        data = resp.get_json()
        self.assertEqual(data.get("status"), "success")
        answer = data.get("answer") or ""
        self.assertIn("Quick Summary", answer)
        self.assertIn("Clinical Significance", answer)
        self.assertIn("Dietary & Lifestyle Advice", answer)
        self.assertIn("Recommended Questions", answer)
        self.assertNotIn("Obstetrics & Gynecology", answer)
        self.assertNotIn("faculty directory", answer.lower())

    def test_chat_endpoint_lab_report_bypass(self):
        payload = {
            "message": (
                "[CLINICAL LAB REPORT CONTEXT]\n"
                "Flagged Parameters:\n"
                "- Hemoglobin: 9.8 g/dL (Standard Range: 12.0 - 17.5 g/dL, Status: low)\n"
                "- Total WBC: 12500 cells/mcL (Standard Range: 4000 - 11000 cells/mcL, Status: high)\n"
            ),
            "session_id": "test_lab_session_chat",
            "language": "en"
        }
        resp = self.client.post("/chat", json=payload)
        self.assertEqual(resp.status_code, 200)
        data = resp.get_json()
        self.assertEqual(data.get("status"), "success")
        ans = data.get("response") or data.get("answer") or ""
        self.assertIn("Quick Summary", ans)
        self.assertIn("Clinical Significance", ans)
        self.assertIn("Dietary & Lifestyle Advice", ans)
        self.assertNotIn("Obstetrics & Gynecology", ans)
        self.assertNotIn("faculty directory", ans.lower())

    def test_faculty_roster_purged_from_local_retriever(self):
        init_local_finetune_retriever()
        ans = retrieve_local_finetune_answer("Obstetrics & Gynecology doctors list", language="en")
        self.assertFalse(is_faculty_or_staff_roster(ans))


if __name__ == "__main__":
    unittest.main()
