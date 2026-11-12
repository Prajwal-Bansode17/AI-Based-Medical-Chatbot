import unittest
import json
import sys
import uuid
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

    def test_lab_report_does_not_bleed_into_roman_marathi_symptom_followup(self):
        # Prompt scaffolding with Turn 1 lab report must NEVER mark Turn 2 symptom message as lab report
        wrapped = (
            "Recent conversation:\n"
            "Patient: [CLINICAL LAB REPORT CONTEXT]\nFlagged Parameters:\n- Fasting Blood Sugar: 1.0 mg/dL (Status: low)\n"
            "Assistant: Quick Summary...\n\n"
            "User's current message:\n"
            "mala khup tap ani sardi ahe kay karu"
        )
        self.assertFalse(is_lab_report_context_prompt(wrapped))
        self.assertFalse(is_lab_report_context_prompt("mala khup tap ani sardi ahe kay karu"))

        sid = f"test_lab_then_tap_sardi_{uuid.uuid4().hex[:8]}"
        # Turn 1: Lab report context
        r1 = self.client.post("/predict", json={
            "text": (
                "[CLINICAL LAB REPORT CONTEXT]\n"
                "Flagged Parameters:\n"
                "- Hemoglobin: 9.8 g/dL (Standard Range: 12.0 - 17.5 g/dL, Status: low)\n"
                "- Fasting Blood Sugar: 165 mg/dL (Standard Range: 70 - 100 mg/dL, Status: high)\n"
                "User Question: Please explain this report"
            ),
            "session_id": sid,
            "user_name": "Hariom Peddawad",
            "language": "en",
        })
        self.assertEqual(r1.status_code, 200)
        d1 = r1.get_json()
        self.assertEqual(d1.get("intent"), "lab_report_analysis")
        self.assertNotIn("108", d1.get("answer", ""))
        self.assertNotIn("Local Weather & Health Advisory", d1.get("answer", ""))

        # Turn 2: Roman Marathi fever + cold query in SAME session
        r2 = self.client.post("/predict", json={
            "text": "mala khup tap ani sardi ahe kay karu",
            "session_id": sid,
            "user_name": "Hariom Peddawad",
        })
        self.assertEqual(r2.status_code, 200)
        d2 = r2.get_json()
        ans2 = d2.get("answer") or d2.get("response") or ""
        self.assertNotEqual(d2.get("intent"), "lab_report_analysis")
        self.assertNotEqual(d2.get("intent"), "follow_up")
        self.assertNotIn("Quick Summary", ans2)
        self.assertNotIn("Hemoglobin", ans2)
        self.assertNotIn("Blood Sugar", ans2)
        self.assertNotIn("Diabetes", ans2)
        self.assertIn("tap", ans2.lower())
        self.assertIn("sardi", ans2.lower())
        self.assertNotIn("Hariom", ans2)
        self.assertNotIn("108", ans2)
        self.assertIn("?", ans2)

    def test_greeting_name_usage_and_no_weather_or_helplines(self):
        sid = f"test_greeting_and_identity_{uuid.uuid4().hex[:8]}"
        # Turn 1 greeting -> uses first name naturally, no weather, no helplines
        g1 = self.client.post("/predict", json={
            "text": "hi",
            "session_id": sid,
            "user_id": "user_hariom_g1",
            "user_name": "Hariom Peddawad",
        })
        self.assertEqual(g1.status_code, 200)
        dg1 = g1.get_json()
        ag1 = dg1.get("answer") or ""
        self.assertEqual(dg1.get("intent"), "greeting")
        self.assertIn("Hariom", ag1)
        self.assertNotIn("Peddawad", ag1)
        self.assertNotIn("Local Weather", ag1)
        self.assertNotIn("108", ag1)

        # Turn 2 greeting -> does NOT keep repeating user's name
        g2 = self.client.post("/predict", json={
            "text": "hello",
            "session_id": sid,
            "user_id": "user_hariom_g1",
            "user_name": "Hariom Peddawad",
        })
        self.assertEqual(g2.status_code, 200)
        ag2 = g2.get_json().get("answer") or ""
        self.assertNotIn("Hariom", ag2)
        self.assertNotIn("Local Weather", ag2)
        self.assertNotIn("108", ag2)

        # Turn 3 explicit identity question -> remembers full name, no weather/helpline clutter
        g3 = self.client.post("/predict", json={
            "text": "do you know my name?",
            "session_id": sid,
            "user_id": "user_hariom_g1",
            "user_name": "Hariom Peddawad",
        })
        self.assertEqual(g3.status_code, 200)
        ag3 = g3.get_json().get("answer") or ""
        self.assertIn("Hariom", ag3)
        self.assertNotIn("Local Weather", ag3)
        self.assertNotIn("108", ag3)

    def test_multiturn_roman_marathi_fever_progression_and_weather_once_per_session(self):
        sid = f"test_multiturn_roman_mr_fever_{uuid.uuid4().hex[:8]}"
        # Turn 1: Fever + cold (no city -> transparent approximate location label, weather shown once)
        r1 = self.client.post("/predict", json={
            "text": "mala khup tap ani sardi ahe kay karu",
            "session_id": sid,
        })
        self.assertEqual(r1.status_code, 200)
        a1 = r1.get_json().get("answer") or ""
        self.assertIn("tap", a1.lower())
        self.assertIn("sardi", a1.lower())
        self.assertNotIn("cholesterol", a1.lower())
        self.assertNotIn("कोलेस्ट्रॉल", a1)
        self.assertIn("🌦️", a1)
        self.assertIn("Andaje Thikan", a1)

        # Turn 2: Narrowed down to ONLY fever + body pain -> acknowledges narrowing, adapts, asks a DIFFERENT deeper question, NO repeated weather card
        r2 = self.client.post("/predict", json={
            "text": "mala fakta taap ahe ani body pain ahe",
            "session_id": sid,
        })
        self.assertEqual(r2.status_code, 200)
        a2 = r2.get_json().get("answer") or ""
        self.assertIn("samajle", a2.lower())
        self.assertIn("angdukhi", a2.lower())
        self.assertIn("sandhyana", a2.lower())
        self.assertNotIn("thermometer", a2.lower())
        self.assertNotIn("cholesterol", a2.lower())
        self.assertNotIn("कोलेस्ट्रॉल", a2)
        self.assertNotIn("🌦️", a2)
        self.assertNotEqual(a1, a2)

        # Turn 3: Direct action request to reduce fever -> step-by-step action plan, NO redundant follow-up question, NO weather repeat
        r3 = self.client.post("/predict", json={
            "text": "are mala taap kami karnya sathi madat kar na",
            "session_id": sid,
        })
        self.assertEqual(r3.status_code, 200)
        a3 = r3.get_json().get("answer") or ""
        self.assertIn("paracetamol", a3.lower())
        self.assertIn("sponging", a3.lower())
        self.assertNotIn("cholesterol", a3.lower())
        self.assertNotIn("कोलेस्ट्रॉल", a3)
        self.assertNotIn("chhota prashna", a3.lower())
        self.assertNotIn("🌦️", a3)
        self.assertNotEqual(a2, a3)

    def test_weather_advisory_displays_explicit_city_from_message(self):
        sid = "test_weather_city_from_text_1"
        r = self.client.post("/predict", json={
            "text": "I live in Pune and have fever and cough",
            "session_id": sid,
            "language": "en",
        })
        self.assertEqual(r.status_code, 200)
        ans = r.get_json().get("answer") or ""
        self.assertIn("🌦️", ans)
        self.assertIn("Pune", ans)


if __name__ == "__main__":
    unittest.main()
