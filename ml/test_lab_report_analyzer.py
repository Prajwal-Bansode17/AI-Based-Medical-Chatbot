"""
Automated Test Suite for Smart Medical Lab Report Analyzer & Layman Decoder Engine
Tests:
1. Regex extraction against typical pasted lab report outputs (CBC, Diabetes, Lipid, Kidney, Liver, Thyroid).
2. Out-of-range status classification ('low', 'normal', 'high', 'critical_low', 'critical_high').
3. Gauge percentage calculation and clamping (0.0 to 1.0).
4. Layman explanations, dietary advice, and targeted doctor questions across English, Marathi, and Hindi.
5. End-to-end HTTP integration on POST /analyze-lab-report.
"""

import sys
import unittest
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

import lab_report_analyzer as lra
from lab_report_analyzer import (
    extract_biomarkers_from_text,
    calculate_gauge_percent,
    classify_biomarker_status,
    analyze_lab_report,
    BIOMARKER_DATABASE,
)
from api import app


class TestLabReportAnalyzer(unittest.TestCase):

    def setUp(self):
        self.client = app.test_client()

    def test_cbc_extraction(self):
        sample_cbc = """
        COMPLETE BLOOD COUNT (CBC)
        Hemoglobin: 9.8 g/dL
        Total Leukocyte Count (WBC): 12,500 /mcL
        Platelet Count: 2.2 Lakhs
        RBC Count: 4.5 mil/mcL
        """
        extracted = extract_biomarkers_from_text(sample_cbc)
        self.assertIn("hemoglobin", extracted)
        self.assertEqual(extracted["hemoglobin"]["value"], 9.8)

        self.assertIn("wbc", extracted)
        self.assertEqual(extracted["wbc"]["value"], 12500.0)

        # Indian lakh notation parsing
        self.assertIn("platelets", extracted)
        self.assertEqual(extracted["platelets"]["value"], 220000.0)

        self.assertIn("rbc", extracted)
        self.assertEqual(extracted["rbc"]["value"], 4.5)

    def test_diabetes_and_lipid_extraction(self):
        sample_metabolic = """
        BIOCHEMISTRY REPORT
        Fasting Blood Sugar (FBS) - 138 mg/dL
        Post Prandial Blood Sugar: 210 mg/dl
        HbA1c : 7.2 %
        Total Cholesterol = 245 mg/dL
        Triglycerides: 180 mg/dL
        LDL Cholesterol: 145 mg/dL
        HDL: 38 mg/dL
        """
        extracted = extract_biomarkers_from_text(sample_metabolic)
        self.assertIn("glucose_fasting", extracted)
        self.assertEqual(extracted["glucose_fasting"]["value"], 138.0)

        self.assertIn("glucose_pp", extracted)
        self.assertEqual(extracted["glucose_pp"]["value"], 210.0)

        self.assertIn("hba1c", extracted)
        self.assertEqual(extracted["hba1c"]["value"], 7.2)

        self.assertIn("cholesterol_total", extracted)
        self.assertEqual(extracted["cholesterol_total"]["value"], 245.0)

        self.assertIn("triglycerides", extracted)
        self.assertEqual(extracted["triglycerides"]["value"], 180.0)

        self.assertIn("ldl", extracted)
        self.assertEqual(extracted["ldl"]["value"], 145.0)

        self.assertIn("hdl", extracted)
        self.assertEqual(extracted["hdl"]["value"], 38.0)

    def test_kidney_liver_thyroid_extraction(self):
        sample_kft_lft = """
        KFT & LFT PANEL
        Serum Creatinine: 1.5 mg/dL
        Blood Urea: 45 mg/dL
        Serum Uric Acid: 8.1 mg/dL
        Total Bilirubin: 2.4 mg/dL
        SGPT (ALT): 72 U/L
        SGOT (AST): 58 U/L
        TSH: 6.8 mIU/L
        """
        extracted = extract_biomarkers_from_text(sample_kft_lft)
        self.assertIn("creatinine", extracted)
        self.assertEqual(extracted["creatinine"]["value"], 1.5)

        self.assertIn("blood_urea", extracted)
        self.assertEqual(extracted["blood_urea"]["value"], 45.0)

        self.assertIn("uric_acid", extracted)
        self.assertEqual(extracted["uric_acid"]["value"], 8.1)

        self.assertIn("bilirubin_total", extracted)
        self.assertEqual(extracted["bilirubin_total"]["value"], 2.4)

        self.assertIn("sgpt_alt", extracted)
        self.assertEqual(extracted["sgpt_alt"]["value"], 72.0)

        self.assertIn("sgot_ast", extracted)
        self.assertEqual(extracted["sgot_ast"]["value"], 58.0)

        self.assertIn("tsh", extracted)
        self.assertEqual(extracted["tsh"]["value"], 6.8)

    def test_gauge_percent_calculation(self):
        # Normal value falls in middle green zone (0.35 to 0.65)
        gauge_mid = calculate_gauge_percent(14.0, 12.0, 16.0)
        self.assertTrue(0.35 <= gauge_mid <= 0.65)

        # Low value falls below 0.35
        gauge_low = calculate_gauge_percent(8.0, 12.0, 16.0)
        self.assertLess(gauge_low, 0.35)
        self.assertGreaterEqual(gauge_low, 0.0)

        # High value falls above 0.65
        gauge_high = calculate_gauge_percent(22.0, 12.0, 16.0)
        self.assertGreater(gauge_high, 0.65)
        self.assertLessEqual(gauge_high, 1.0)

        # Clamping check on extreme values
        self.assertLessEqual(calculate_gauge_percent(1000.0, 10.0, 20.0), 1.0)
        self.assertGreaterEqual(calculate_gauge_percent(-50.0, 10.0, 20.0), 0.0)

    def test_status_and_color_classification(self):
        # Normal
        status, color = classify_biomarker_status(14.0, 12.0, 16.0, crit_low=7.0, crit_high=19.0)
        self.assertEqual(status, "normal")
        self.assertEqual(color, "#4CAF50")

        # Low
        status, color = classify_biomarker_status(10.5, 12.0, 16.0, crit_low=7.0, crit_high=19.0)
        self.assertEqual(status, "low")
        self.assertEqual(color, "#FF9800")

        # High
        status, color = classify_biomarker_status(17.5, 12.0, 16.0, crit_low=7.0, crit_high=19.0)
        self.assertEqual(status, "high")
        self.assertEqual(color, "#FF9800")

        # Critical low (e.g. Hemoglobin <= 7.0)
        status, color = classify_biomarker_status(6.5, 12.0, 16.0, crit_low=7.0, crit_high=19.0)
        self.assertEqual(status, "critical_low")
        self.assertEqual(color, "#F44336")

        # Critical high (e.g. Fasting sugar >= 300.0)
        status, color = classify_biomarker_status(350.0, 70.0, 99.0, crit_low=55.0, crit_high=300.0)
        self.assertEqual(status, "critical_high")
        self.assertEqual(color, "#F44336")

    def test_multilingual_layman_analysis_marathi(self):
        sample = """
        Hemoglobin: 9.5 g/dL
        Fasting Blood Sugar: 155 mg/dL
        Serum Creatinine: 1.4 mg/dL
        """
        result = analyze_lab_report(sample, user_language="mr")
        self.assertEqual(result["status"], "success")
        self.assertEqual(result["language"], "mr")
        self.assertEqual(result["analyzed_count"], 3)
        self.assertEqual(result["flagged_count"], 3)

        # Check Marathi labels and explanations
        hb = next(b for b in result["biomarkers"] if b["key"] == "hemoglobin")
        self.assertEqual(hb["status"], "low")
        self.assertIn("हिमोग्लोबिन", hb["display_name"])
        self.assertIn("अॅनिमिया", hb["layman_explanation"])
        self.assertIn("पालक", hb["dietary_advice"])

        # Check Marathi doctor questions
        self.assertGreater(len(result["doctor_questions"]), 0)
        self.assertTrue(any("सप्लिमेंट्स" in q or "HbA1c" in q or "किडनी" in q for q in result["doctor_questions"]))

        # Check summary & disclaimer in Marathi
        self.assertIn("तपासण्यात आलेल्या", result["summary"])
        self.assertIn("सूचना", result["disclaimer"])

    def test_multilingual_layman_analysis_hindi(self):
        sample = """
        Hemoglobin: 8.9 g/dL
        HbA1c: 7.5 %
        """
        result = analyze_lab_report(sample, user_language="hi")
        self.assertEqual(result["status"], "success")
        self.assertEqual(result["language"], "hi")

        hb = next(b for b in result["biomarkers"] if b["key"] == "hemoglobin")
        self.assertIn("हीमोग्लोबिन", hb["display_name"])
        self.assertIn("एनीमिया", hb["layman_explanation"])
        self.assertIn("चुकंदर", hb["dietary_advice"])

        a1c = next(b for b in result["biomarkers"] if b["key"] == "hba1c")
        self.assertIn("डायबिटीज", a1c["layman_explanation"])

        self.assertIn("महत्वपूर्ण सूचना", result["disclaimer"])

    def test_all_normal_report_summary(self):
        sample = """
        Hemoglobin: 14.2 g/dL
        Fasting Blood Sugar: 88 mg/dL
        Platelet Count: 250000 /mcL
        Serum Creatinine: 0.9 mg/dL
        """
        result = analyze_lab_report(sample, user_language="en")
        self.assertEqual(result["analyzed_count"], 4)
        self.assertEqual(result["flagged_count"], 0)
        self.assertIn("standard healthy reference ranges", result["summary"])

    def test_api_endpoint_analyze_lab_report_post(self):
        payload = {
            "text": "Hemoglobin: 9.2 g/dL\nFasting Blood Sugar: 160 mg/dL\nPlatelets: 1.8 Lakhs",
            "language": "mr"
        }
        res = self.client.post("/analyze-lab-report", json=payload)
        self.assertEqual(res.status_code, 200)

        data = res.get_json()
        self.assertEqual(data["status"], "success")
        self.assertEqual(data["analyzed_count"], 3)
        self.assertEqual(data["flagged_count"], 2)
        self.assertEqual(len(data["biomarkers"]), 3)
        self.assertIn("disclaimer", data)

    def test_api_endpoint_dictionary_input(self):
        payload = {
            "biomarkers": {
                "hemoglobin": 9.5,
                "creatinine": {"value": 1.6, "unit": "mg/dL"}
            },
            "language": "hi"
        }
        res = self.client.post("/analyze-lab-report", json=payload)
        self.assertEqual(res.status_code, 200)

        data = res.get_json()
        self.assertEqual(data["status"], "success")
        self.assertEqual(data["analyzed_count"], 2)
        self.assertEqual(data["flagged_count"], 2)

    def test_api_endpoint_empty_body_400(self):
        res = self.client.post("/analyze-lab-report", json={})
        self.assertEqual(res.status_code, 400)
        data = res.get_json()
        self.assertEqual(data["status"], "error")


if __name__ == "__main__":
    unittest.main()
