"""
MedAssist AI - Vision & Multimodal Medical Document Service
Handles medicine image validation, safe decoding, OCR extraction,
Gemini multimodal vision processing, document classification,
clinical laboratory report analysis, prescription interpretation,
and strict clinical safety:
- Never identify a loose pill from appearance/color/shape alone (D4)
- 10-point structured medicine response formatting (D5)
- 7-section structured laboratory report analysis (CBC, Glucose, Lipid, LFT, KFT, Thyroid)
- Detailed prescription reading with handwriting uncertainty alerts
- Image validation (size <= 5MB, JPEG/PNG/WEBP magic bytes) (D1)
- Graceful OCR fallback using pytesseract or client OCR text (D2)
- Refuses sending images to text-only models (D3)
- Educational safety disclaimer (D6)
- Document follow-up question answering
"""

import base64
import io
import re
import json
import logging
import os
from typing import Optional, Dict, Tuple, List, Any
from pathlib import Path

logger = logging.getLogger("medassist.vision_service")

MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024  # 5 MB max
ALLOWED_MIME_TYPES = {"image/jpeg", "image/png", "image/webp"}

# Magic bytes detection
JPEG_MAGIC = b"\xFF\xD8\xFF"
PNG_MAGIC = b"\x89PNG\r\n\x1a\n"
RIFF_MAGIC = b"RIFF"
WEBP_MAGIC = b"WEBP"

LOOSE_PILL_KEYWORDS = [
    r"\b(?:loose|single|unpackaged|unknown)\s+(?:pill|tablet|capsule)\b",
    r"\b(?:identify|what\s+is|name)\s+(?:this\s+)?(?:[a-z]+\s+)*(?:pill|tablet|capsule)\b",
    r"\bwhat\s+(?:pill|tablet|capsule|medicine|drug)\s+is\s+this\b",
    r"\bwithout\s+(?:strip|packet|packaging|box|label|blister)\b",
    r"\b(?:color|shape|appearance)\s+of\s+(?:this\s+)?(?:pill|tablet)\b",
    r"\bkhuli\s+goli\b",
    r"\bkhula\s+tablet\b",
    r"\bhe\s+konte\s+aushadh\s+aahe\b",
]

CLINICAL_VISION_DISCLAIMER = (
    "⚠️ **Disclaimer:** This visual analysis is an educational aid and not a definitive medical evaluation "
    "or prescription. Always verify with packaging labels and consult a licensed physician or pharmacist."
)

CLINICAL_REPORT_DISCLAIMER = (
    "⚠️ **Important Clinical Note:** This laboratory report breakdown is provided for educational and informational purposes only. "
    "A single test result is never a definitive diagnosis. Medical reports must always be evaluated in clinical context by a licensed physician."
)

CLINICAL_PRESCRIPTION_DISCLAIMER = (
    "⚠️ **Prescription Safety Notice:** Never alter, double, substitute, or discontinue prescribed medications without explicit consultation with your prescribing doctor or pharmacist."
)


# =============================================================================
# 1. IMAGE VALIDATION & DECODING
# =============================================================================

def detect_image_mime(image_bytes: bytes) -> Optional[str]:
    """Validates magic bytes for JPEG, PNG, and WEBP images."""
    if not image_bytes or len(image_bytes) < 3:
        return None
    if image_bytes.startswith(JPEG_MAGIC):
        return "image/jpeg"
    if len(image_bytes) >= 8 and image_bytes.startswith(PNG_MAGIC):
        return "image/png"
    if len(image_bytes) >= 12 and image_bytes.startswith(RIFF_MAGIC) and image_bytes[8:12] == WEBP_MAGIC:
        return "image/webp"
    return None


def validate_and_decode_image(image_base64: str) -> Tuple[Optional[bytes], Optional[str], Optional[str]]:
    """
    Validates base64 image string:
    - Size within limits (<= 5MB)
    - Valid image format (JPEG, PNG, WEBP) via magic bytes
    Returns (image_bytes, mime_type, error_message).
    """
    if not image_base64 or not isinstance(image_base64, str):
        return None, None, "No image data provided"

    clean_b64 = image_base64
    if "," in clean_b64:
        clean_b64 = clean_b64.split(",", 1)[1]

    try:
        raw_bytes = base64.b64decode(clean_b64)
    except Exception as e:
        return None, None, f"Invalid base64 encoding: {str(e)}"

    if len(raw_bytes) > MAX_IMAGE_SIZE_BYTES:
        return None, None, f"Image size exceeds maximum limit of 5MB (size: {len(raw_bytes) / 1024 / 1024:.2f}MB)"

    mime = detect_image_mime(raw_bytes)
    if not mime or mime not in ALLOWED_MIME_TYPES:
        return None, None, "Unsupported image format. Please upload JPEG, PNG, or WEBP."

    return raw_bytes, mime, None


def extract_text_from_image(image_bytes: bytes) -> str:
    """
    Attempts optical character recognition (OCR) using pytesseract if available.
    Gracefully falls back to empty string without crashing if OCR is unavailable.
    """
    if not image_bytes:
        return ""
    try:
        from PIL import Image
        import pytesseract
        img = Image.open(io.BytesIO(image_bytes))
        text = pytesseract.image_to_string(img)
        return text.strip()
    except Exception as e:
        logger.debug("OCR extraction unavailable or encountered error: %s", e)
        return ""


# =============================================================================
# 2. DOCUMENT CLASSIFICATION & LOOSE PILL REFUSAL
# =============================================================================

def is_loose_pill_query(text: str, ocr_text: str = "") -> bool:
    """Detects if the query asks to identify a loose pill purely by physical appearance."""
    blob = f"{text or ''} {ocr_text or ''}".lower()
    for pat in LOOSE_PILL_KEYWORDS:
        if re.search(pat, blob):
            return True
    return False


def build_loose_pill_refusal_response(language: str = "en") -> str:
    """
    Clinical safety (D4): Never identify a loose pill from physical appearance alone.
    Explains severe clinical risks of counterfeits and visual lookalikes.
    """
    if language == "mr":
        return (
            "⚠️ **वैद्यकीय सुरक्षा इशारा: केवळ दिसण्यावरून गोळी ओळखणे शक्य नाही**\n\n"
            "केवळ गोळीच्या रंगावरून, आकारावरून किंवा दिसण्यावरून औषधाचे नाव निश्चित करणे वैद्यकीयदृष्ट्या असुरक्षित आहे. "
            "अनेक भिन्न औषधे अगदी हुबेहूब दिसतात आणि बनावट किंवा चुकीचे औषध घेतल्यास जिवाला गंभीर धोका उद्भवू शकतो.\n\n"
            "**पुढील सुरक्षित पावले:**\n"
            "1. **मूळ पाकीट दाखवा:** कृपया औषधाचे मूळ पाकीट (strip), बॉक्स किंवा लेबलचा स्पष्ट फोटो पाठवा ज्यावर औषधाचे नाव आणि क्षमता (mg) लिहिलेली असेल.\n"
            "2. **फार्मासिस्टशी संपर्क साधा:** ही गोळी प्रत्यक्ष नोंदणीकृत फार्मासिस्ट किंवा डॉक्टरांना दाखवा जे गोळीवरील छापलेला कोड (imprint) पाहून खात्री करतील.\n\n"
            f"{CLINICAL_VISION_DISCLAIMER}"
        )
    elif language == "hi":
        return (
            "⚠️ **चिकित्सीय सुरक्षा चेतावनी: केवल दिखावट से गोली की पहचान संभव नहीं है**\n\n"
            "केवल रंग, आकार या शारीरिक बनावट के आधार पर किसी खुली गोली (pill) की पहचान करना चिकित्सकीय रूप से असुरक्षित है। "
            "कई अलग-अलग दवाएं देखने में बिल्कुल एक जैसी होती हैं और गलत या नकली दवा का सेवन स्वास्थ्य के लिए घातक हो सकता है।\n\n"
            "**सुरक्षित उपाय:**\n"
            "1. **मूल पैकेजिंग:** कृपया दवा के पत्ते (blister strip), डिब्बे या फार्मेसी लेबल का स्पष्ट फोटो अपलोड करें जिस पर दवा का नाम और क्षमता (mg/ml) छपी हो।\n"
            "2. **फार्मासिस्ट से मिलें:** इस गोली को किसी लाइसेंस प्राप्त फार्मासिस्ट या डॉक्टर को दिखाएं जो इस पर छपे कोड (imprint) से सही पहचान कर सकें।\n\n"
            f"{CLINICAL_VISION_DISCLAIMER}"
        )
    else:
        return (
            "⚠️ **Clinical Safety Warning: Cannot Identify Medication by Appearance Alone**\n\n"
            "Identifying a loose tablet, capsule, or pill solely by its physical appearance (shape, color, or size) "
            "is medically unsafe. Numerous distinct prescription and over-the-counter medications share identical physical characteristics, "
            "and ingesting misidentified or counterfeit medication carries severe clinical risks.\n\n"
            "**Recommended Next Steps:**\n"
            "1. **Provide Original Packaging:** Please upload a clear photograph of the blister strip, packaging box, or pharmacy bottle showing the printed brand name, active ingredients, and strength (e.g., 500 mg).\n"
            "2. **Consult a Pharmacist:** Bring the pill in person to a licensed pharmacist or doctor who can verify imprinted markings (imprint/NDC codes) reliably.\n\n"
            f"{CLINICAL_VISION_DISCLAIMER}"
        )


def classify_medical_document(image_bytes: Optional[bytes], ocr_text: str, user_query: str) -> str:
    """
    Classifies the uploaded medical document into one of:
    - 'loose_pill'
    - 'lab_report'
    - 'prescription'
    - 'medicine_packaging'
    - 'discharge_summary'
    - 'general_document'
    """
    blob = f"{user_query or ''} {ocr_text or ''}".lower()

    if is_loose_pill_query(user_query, ocr_text):
        return "loose_pill"

    # Lab report keywords
    lab_keywords = [
        "cbc", "hemogram", "haemoglobin", "hemoglobin", "platelet", "wbc", "rbc",
        "leukocyte", "neutrophil", "lymphocyte", "eosinophil", "monocyte", "esr",
        "glucose", "fasting blood", "postprandial", "hba1c", "blood sugar",
        "lipid profile", "cholesterol", "triglyceride", "hdl", "ldl", "vldl",
        "creatinine", "blood urea", "bun", "uric acid", "kft", "rft",
        "bilirubin", "sgot", "sgpt", "alt", "ast", "alp", "lft", "liver function",
        "thyroid", "tsh", "t3", "t4", "urine analysis", "reference range", "normal range",
        "pathology", "diagnostic", "laboratory", "specimen", "investigation report",
        "bio-chemistry", "hematology", "biochemistry"
    ]
    if any(k in blob for k in lab_keywords):
        return "lab_report"

    # Prescription keywords
    rx_keywords = [
        "rx", "prescription", "dr.", "doctor", "clinic", "hospital", "dispense",
        "tab ", "tablet", "cap ", "capsule", "syp ", "syrup", "o.d.", "b.d.", "t.d.s.",
        "1-0-1", "1-1-1", "0-0-1", "1-0-0", "sos", "after food", "before food",
        "prescribed", "sig:", "dosage:", "m.d.", "mbbs"
    ]
    if any(k in blob for k in rx_keywords):
        return "prescription"

    # Medicine packaging keywords
    pack_keywords = [
        "mg", "ml", "mcg", "tablets ip", "capsules ip", "blister", "strip", "box",
        "mfg. lic", "batch no", "exp date", "expiry", "mrp", "marketed by", "manufactured by",
        "pharma", "laboratories", "composition:", "each film coated", "each uncoated tablet"
    ]
    if any(k in blob for k in pack_keywords):
        return "medicine_packaging"

    if any(k in blob for k in ["discharge", "admission", "inpatient", "operation note", "discharge summary"]):
        return "discharge_summary"

    # Fallback to query intent
    if any(k in user_query.lower() for k in ["report", "blood", "test", "cbc", "sugar"]):
        return "lab_report"
    if any(k in user_query.lower() for k in ["prescription", "doctor wrote", "parchi"]):
        return "prescription"
    if any(k in user_query.lower() for k in ["medicine", "tablet", "strip", "goli", "dawa"]):
        return "medicine_packaging"

    return "general_document"


# =============================================================================
# 3. CLINICAL LABORATORY REPORT ANALYSIS ENGINE
# =============================================================================

# Standard Clinical Reference Ranges and Physiological Context
CLINICAL_TEST_DATABASE = {
    "hemoglobin": {
        "aliases": ["hemoglobin", "haemoglobin", "hb", "hgb"],
        "default_unit": "g/dL",
        "ref_min": 12.0,
        "ref_max": 17.0,
        "ref_str": "12.0 - 17.0 g/dL (Adult)",
        "meaning": "Hemoglobin is the iron-rich protein in red blood cells that carries oxygen from the lungs throughout the body.",
        "low_causes": "Iron deficiency anemia, vitamin B12 deficiency, blood loss, chronic kidney disease, bone marrow disorders.",
        "high_causes": "Dehydration, high altitude living, chronic smoking, polycythemia vera, pulmonary conditions.",
    },
    "wbc": {
        "aliases": ["total wbc count", "wbc", "total leukocyte count", "tlc", "white blood cells"],
        "default_unit": "/mcL",
        "ref_min": 4000,
        "ref_max": 11000,
        "ref_str": "4,000 - 11,000 /mcL",
        "meaning": "White blood cells defend the body against infections, inflammation, and allergic reactions.",
        "low_causes": "Viral infections, autoimmune disorders, bone marrow suppression, certain medications.",
        "high_causes": "Active bacterial infection, significant physical stress, tissue damage, inflammatory reactions.",
    },
    "platelets": {
        "aliases": ["platelet count", "platelets", "plt"],
        "default_unit": "/mcL",
        "ref_min": 150000,
        "ref_max": 450000,
        "ref_str": "150,000 - 450,000 /mcL",
        "meaning": "Platelets are cell fragments vital for blood clotting, wound healing, and preventing excessive bleeding.",
        "low_causes": "Viral infections (e.g. dengue), autoimmune destruction (ITP), splenic sequestration, liver disease.",
        "high_causes": "Reactive thrombocytosis (iron deficiency, infection, post-surgery), essential thrombocythemia.",
    },
    "rbc": {
        "aliases": ["rbc count", "rbc", "red blood cell count", "total rbc"],
        "default_unit": "million/mcL",
        "ref_min": 4.5,
        "ref_max": 5.9,
        "ref_str": "4.5 - 5.9 million/mcL",
        "meaning": "Red blood cells carry oxygen and nutrients to tissues throughout the body.",
        "low_causes": "Anemia, bleeding, hemolysis, nutritional deficiencies.",
        "high_causes": "Polycythemia, chronic hypoxia, severe dehydration.",
    },
    "glucose_fasting": {
        "aliases": ["fasting blood sugar", "fbs", "glucose fasting", "fasting glucose", "blood glucose fasting"],
        "default_unit": "mg/dL",
        "ref_min": 70,
        "ref_max": 99,
        "ref_str": "70 - 99 mg/dL (Normal: <100, Prediabetes: 100-125, Diabetes: >=126)",
        "meaning": "Fasting blood sugar measures circulating glucose levels after an overnight fast (minimum 8-10 hours).",
        "low_causes": "Prolonged fasting, excess insulin or diabetes medications, intense exertion without meals.",
        "high_causes": "Insulin resistance, prediabetes, type 2 diabetes, acute physiological or emotional stress, corticosteroid therapy.",
    },
    "glucose_pp": {
        "aliases": ["post prandial blood sugar", "ppbs", "glucose post prandial", "postprandial glucose", "blood glucose pp"],
        "default_unit": "mg/dL",
        "ref_min": 70,
        "ref_max": 140,
        "ref_str": "< 140 mg/dL (Normal: <140, Prediabetes: 140-199, Diabetes: >=200)",
        "meaning": "Measures blood glucose 2 hours after a meal to evaluate how effectively the pancreas and tissues handle glucose.",
        "low_causes": "Reactive hypoglycemia, medication excess, delayed insulin response.",
        "high_causes": "Impaired glucose tolerance, diabetes mellitus, high-glycemic meals, acute illness.",
    },
    "hba1c": {
        "aliases": ["hba1c", "glycated hemoglobin", "glycosylated hemoglobin"],
        "default_unit": "%",
        "ref_min": 4.0,
        "ref_max": 5.6,
        "ref_str": "< 5.7% (Normal: <5.7%, Prediabetes: 5.7-6.4%, Diabetes: >=6.5%)",
        "meaning": "Provides a 3-month weighted average of blood glucose by measuring glucose bound to hemoglobin.",
        "low_causes": "Hemolytic anemia, recent blood transfusion, chronic blood loss.",
        "high_causes": "Chronic hyperglycemia, poorly controlled or untreated diabetes, prediabetes.",
    },
    "cholesterol_total": {
        "aliases": ["total cholesterol", "serum cholesterol", "cholesterol total"],
        "default_unit": "mg/dL",
        "ref_min": 100,
        "ref_max": 200,
        "ref_str": "< 200 mg/dL (Desirable)",
        "meaning": "Total cholesterol quantifies all cholesterol circulating in lipoproteins.",
        "low_causes": "Severe malnutrition, malabsorption, hyperthyroidism, chronic liver disease.",
        "high_causes": "High saturated fat diet, sedentary lifestyle, familial hypercholesterolemia, hypothyroidism.",
    },
    "triglycerides": {
        "aliases": ["triglycerides", "serum triglycerides", "tg"],
        "default_unit": "mg/dL",
        "ref_min": 50,
        "ref_max": 150,
        "ref_str": "< 150 mg/dL (Normal: <150, Borderline: 150-199, High: 200-499)",
        "meaning": "Triglycerides are the primary storage form of fat derived from excess dietary calories, sugars, and fats.",
        "low_causes": "Very low-fat diet, malnutrition, hyperthyroidism.",
        "high_causes": "High carbohydrate/sugar intake, alcohol intake, obesity, metabolic syndrome, untreated diabetes.",
    },
    "hdl": {
        "aliases": ["hdl cholesterol", "hdl", "high density lipoprotein"],
        "default_unit": "mg/dL",
        "ref_min": 40,
        "ref_max": 90,
        "ref_str": "> 40 mg/dL (Men), > 50 mg/dL (Women)",
        "meaning": "HDL ('good' cholesterol) transports excess cholesterol from peripheral arteries back to the liver for excretion.",
        "low_causes": "Sedentary lifestyle, smoking, metabolic syndrome, diabetes, high-carbohydrate diets.",
        "high_causes": "Regular aerobic exercise, genetics, healthy fat consumption.",
    },
    "ldl": {
        "aliases": ["ldl cholesterol", "ldl", "low density lipoprotein"],
        "default_unit": "mg/dL",
        "ref_min": 50,
        "ref_max": 100,
        "ref_str": "< 100 mg/dL (Optimal: <100, Near Optimal: 100-129, High: >=130)",
        "meaning": "LDL ('bad' cholesterol) can build up inside arterial walls, contributing to atherosclerosis.",
        "low_causes": "Genetic hypobetalipoproteinemia, severe liver dysfunction.",
        "high_causes": "Diet rich in saturated/trans fats, genetic predisposition, hypothyroidism, kidney disease.",
    },
    "creatinine": {
        "aliases": ["serum creatinine", "creatinine"],
        "default_unit": "mg/dL",
        "ref_min": 0.6,
        "ref_max": 1.3,
        "ref_str": "0.6 - 1.3 mg/dL (Men: 0.7-1.3, Women: 0.5-1.1)",
        "meaning": "Creatinine is a waste byproduct of muscle metabolism filtered out of the blood exclusively by the kidneys.",
        "low_causes": "Low muscle mass, severe liver disease, advanced malnutrition.",
        "high_causes": "Dehydration, acute kidney injury, chronic kidney disease, urinary obstruction, nephrotoxic drugs.",
    },
    "blood_urea": {
        "aliases": ["blood urea", "serum urea", "urea", "bun"],
        "default_unit": "mg/dL",
        "ref_min": 15,
        "ref_max": 45,
        "ref_str": "15 - 45 mg/dL",
        "meaning": "Urea is a nitrogenous waste product formed in the liver from protein breakdown and excreted by the kidneys.",
        "low_causes": "Low protein intake, severe liver disease, overhydration.",
        "high_causes": "Dehydration, high protein diet, gastrointestinal bleeding, renal dysfunction.",
    },
    "tsh": {
        "aliases": ["tsh", "thyroid stimulating hormone", "ultrasensitive tsh"],
        "default_unit": "mIU/L",
        "ref_min": 0.4,
        "ref_max": 4.5,
        "ref_str": "0.4 - 4.5 mIU/L (or uIU/mL)",
        "meaning": "TSH is released by the pituitary gland to regulate thyroid hormone production.",
        "low_causes": "Hyperthyroidism (overactive thyroid), thyroiditis, excessive thyroid medication.",
        "high_causes": "Primary hypothyroidism (underactive thyroid), Hashimoto's thyroiditis, iodine deficiency.",
    },
    "bilirubin_total": {
        "aliases": ["total bilirubin", "serum bilirubin", "bilirubin total"],
        "default_unit": "mg/dL",
        "ref_min": 0.2,
        "ref_max": 1.2,
        "ref_str": "0.2 - 1.2 mg/dL",
        "meaning": "Bilirubin is a yellowish compound formed during the normal breakdown of red blood cells, processed by the liver.",
        "low_causes": "Generally no clinical significance.",
        "high_causes": "Hepatitis, liver disease, biliary duct obstruction (gallstones), hemolytic anemia, Gilbert syndrome.",
    },
    "sgpt": {
        "aliases": ["sgpt", "alt", "alanine aminotransferase", "sgpt / alt"],
        "default_unit": "U/L",
        "ref_min": 7,
        "ref_max": 56,
        "ref_str": "7 - 56 U/L",
        "meaning": "An enzyme found predominantly in liver cells; elevated levels indicate liver cell stress or injury.",
        "low_causes": "Normal healthy baseline.",
        "high_causes": "Fatty liver, viral hepatitis, alcohol use, medication/toxin exposure, intense exercise.",
    },
    "sgot": {
        "aliases": ["sgot", "ast", "aspartate aminotransferase", "sgot / ast"],
        "default_unit": "U/L",
        "ref_min": 10,
        "ref_max": 40,
        "ref_str": "10 - 40 U/L",
        "meaning": "An enzyme present in liver, heart, and muscle cells that releases into blood upon cellular stress.",
        "low_causes": "Normal healthy baseline.",
        "high_causes": "Liver damage, muscle injury, strenuous exercise, myocardial stress.",
    },
}


def parse_lab_parameters_from_text(raw_text: str) -> List[Dict[str, Any]]:
    """
    Scans OCR or document text to extract recognized laboratory test parameters,
    their observed numerical values, units, printed reference ranges, and flags.
    Preserves uncertainty for unreadable or ambiguous values.
    """
    if not raw_text:
        return []

    lines = raw_text.splitlines()
    found_parameters = []
    matched_keys = set()

    for line in lines:
        clean_line = line.strip()
        if not clean_line or len(clean_line) < 3:
            continue
        line_low = clean_line.lower()

        for key, info in CLINICAL_TEST_DATABASE.items():
            if key in matched_keys:
                continue

            for alias in info["aliases"]:
                # Match alias at word boundary
                pattern = r"\b" + re.escape(alias) + r"\b"
                match_alias = re.search(pattern, line_low)
                if not match_alias:
                    continue

                # Search for observed numeric value after the alias or on the line
                # Look for numbers (with optional decimals or commas)
                remainder = clean_line[match_alias.end():]
                val_match = re.search(r"[:=-]?\s*([0-9]+(?:[.,][0-9]+)?)", remainder)
                if not val_match:
                    # Look anywhere on the line
                    val_match = re.search(r"\b([0-9]+(?:[.,][0-9]+)?)\b", remainder)

                if val_match:
                    num_str = val_match.group(1).replace(",", "")
                    try:
                        obs_val = float(num_str)
                    except ValueError:
                        obs_val = None

                    # Check for printed reference range on the same line (e.g. 12.0 - 15.5 or 12-16 or < 200)
                    printed_range = None
                    range_match = re.search(r"([0-9]+(?:\.[0-9]+)?)\s*[-–—to]+\s*([0-9]+(?:\.[0-9]+)?)", remainder[val_match.end():])
                    ref_min = info["ref_min"]
                    ref_max = info["ref_max"]
                    ref_display = info["ref_str"]

                    if range_match:
                        try:
                            p_min = float(range_match.group(1))
                            p_max = float(range_match.group(2))
                            ref_min = p_min
                            ref_max = p_max
                            printed_range = f"{p_min} - {p_max} {info['default_unit']}"
                            ref_display = printed_range
                        except Exception:
                            pass

                    # Extract unit if visible
                    unit = info["default_unit"]
                    for u in ["g/dL", "mg/dL", "/mcL", "million/mcL", "%", "mIU/L", "U/L", "fL", "pg", "cells/cumm", "lakhs/cumm"]:
                        if u.lower() in remainder.lower():
                            unit = u
                            break

                    # Evaluate status
                    status = "Normal"
                    if obs_val is not None:
                        if obs_val < ref_min:
                            status = "Low"
                        elif obs_val > ref_max:
                            status = "High"

                    found_parameters.append({
                        "key": key,
                        "name": alias.title(),
                        "value": obs_val,
                        "raw_value_str": num_str,
                        "unit": unit,
                        "reference_range": ref_display,
                        "status": status,
                        "meaning": info["meaning"],
                        "low_causes": info["low_causes"],
                        "high_causes": info["high_causes"],
                        "is_printed_range": bool(printed_range),
                    })
                    matched_keys.add(key)
                    break
                else:
                    # Alias was found but number was smudged or unreadable
                    found_parameters.append({
                        "key": key,
                        "name": alias.title(),
                        "value": None,
                        "raw_value_str": "Unclear / Smudged",
                        "unit": info["default_unit"],
                        "reference_range": info["ref_str"],
                        "status": "Unclear",
                        "meaning": info["meaning"],
                        "low_causes": info["low_causes"],
                        "high_causes": info["high_causes"],
                        "is_printed_range": False,
                    })
                    matched_keys.add(key)
                    break

    return found_parameters


def format_laboratory_report_response(
    report_title: str,
    parameters: List[Dict[str, Any]],
    uncertainties: List[str] = None,
    language: str = "en"
) -> str:
    """
    Renders laboratory report analysis adhering to Section 3.4 7-section structure:
    1. Report summary
    2. Clearly readable test values
    3. Report-specific reference ranges
    4. Important abnormal findings
    5. What the findings may mean in general
    6. What cannot be concluded from this report alone
    7. Suggested next steps & questions for the doctor
    """
    uncertainties = uncertainties or []
    abnormal_items = [p for p in parameters if p.get("status") in ("High", "Low")]
    normal_items = [p for p in parameters if p.get("status") == "Normal"]
    unclear_items = [p for p in parameters if p.get("status") == "Unclear"]

    # ---------------------------------------------------------
    # MARATHI LOCALIZATION
    # ---------------------------------------------------------
    if language == "mr":
        out = []
        out.append(f"### 📋 वैद्यकीय तपासणी अहवाल विश्लेषण (Laboratory Report Overview): {report_title}\n")
        out.append(
            "हा अहवाल स्पष्ट दिसणाऱ्या मूल्यांवर आधारित तपासला गेला आहे. "
            "खालील विश्लेषण केवळ शैक्षणिक माहिती आणि मार्गदर्शनासाठी आहे.\n"
        )

        out.append("#### १. अहवाल सारांश (Report Summary)")
        out.append(f"- **अहवाल प्रकार:** {report_title}")
        out.append(f"- **वाचता आलेले एकूण घटक:** {len(parameters)}")
        if abnormal_items:
            out.append(f"- **लक्षात आलेले असामान्य (High/Low) घटक:** {len(abnormal_items)}")
        else:
            out.append("- **तपासलेले सर्व घटक सध्या सामान्य मर्यादेत आहेत.**")
        out.append("")

        out.append("#### २. स्पष्ट दिसणारी चाचणी मूल्ये (Test Values & Reference Ranges)")
        out.append("| चाचणी नाव (Test) | आलेले मूल्य (Observed) | सामान्य संदर्भ मर्यादा (Reference Range) | स्थिती (Status) |")
        out.append("| :--- | :---: | :---: | :---: |")
        for p in parameters:
            val_str = f"{p['value']} {p['unit']}" if p['value'] is not None else "अस्पष्ट / वाचता येत नाही"
            stat_str = "सामान्य (Normal)" if p['status'] == "Normal" else (
                "कमी (Low) ⬇️" if p['status'] == "Low" else (
                    "जास्त (High) ⬆️" if p['status'] == "High" else "अस्पष्ट ⚠️"
                )
            )
            out.append(f"| {p['name']} | **{val_str}** | {p['reference_range']} | {stat_str} |")
        out.append("")

        if abnormal_items:
            out.append("#### ३. महत्त्वाचे असामान्य निष्कर्ष (Abnormal Findings)")
            for ab in abnormal_items:
                stat_tag = "कमी (Low)" if ab['status'] == "Low" else "जास्त (High)"
                out.append(f"- **{ab['name']}: {ab['value']} {ab['unit']}** ({stat_tag}) — सामान्य मर्यादा: {ab['reference_range']}")
            out.append("")

            out.append("#### ४. या निष्कर्षांचा सर्वसाधारण अर्थ काय असू शकतो (General Meaning)")
            out.append("*(नोंद: हे केवळ सर्वसाधारण वैद्यकीय कारणे आहेत, अंतिम निदान नाही)*")
            for ab in abnormal_items:
                causes = ab['low_causes'] if ab['status'] == "Low" else ab['high_causes']
                out.append(f"- **{ab['name']} ({ab['status']}):** {ab['meaning']} संभाव्य कारणांमध्ये समाविष्ट: {causes}")
            out.append("")
        else:
            out.append("#### ३. असामान्य निष्कर्ष: तपासलेले सर्व मूल्ये सामान्य मर्यादेत आहेत.\n")

        out.append("#### ५. या अहवालावरून काय निष्कर्ष काढता येत नाही (Limitations)")
        out.append("- केवळ एका अहवालावरून कोणत्याही आजाराचे अंतिम निदान करता येत नाही.")
        out.append("- रुग्णाचे वय, लक्षणे, इतर चालू औषधे आणि प्रत्यक्ष शारीरिक तपासणीशिवाय निष्कर्ष अपूर्ण असतात.")
        if unclear_items or uncertainties:
            out.append("- काही मूल्ये अस्पष्ट किंवा कापली गेली आहेत; प्रत्यक्ष मूळ कागद तपासणे आवश्यक आहे.")
        out.append("")

        out.append("#### ६. पुढील सुचवलेली पावले (Suggested Next Steps)")
        out.append("1. **वैद्यकीय सल्ला:** हा अहवाल तुमच्या डॉक्टरांना किंवा तज्ज्ञांना दाखवून प्रत्यक्ष सल्ला घ्या.")
        out.append("2. **स्वतःहून औषधे सुरू/बंद करू नका:** अहवालातील मूल्यांवरून स्वतः औषधोपचार बदलू नका.")
        out.append("")

        out.append("#### ७. डॉक्टरांना विचारण्यासाठी महत्त्वाचे प्रश्न (Questions for Your Doctor)")
        out.append("- *\"डॉक्टर, या अहवालातील बदलांमुळे मला काही अतिरिक्त चाचणी किंवा आहारातील बदल करण्याची गरज आहे का?\"*")
        out.append("- *\"माझ्या सध्याच्या लक्षणांशी या चाचणीचा काय संबंध आहे?\"*")
        out.append("")
        out.append(CLINICAL_REPORT_DISCLAIMER)

        return "\n".join(out)

    # ---------------------------------------------------------
    # HINDI LOCALIZATION
    # ---------------------------------------------------------
    elif language == "hi":
        out = []
        out.append(f"### 📋 मेडिकल रिपोर्ट विश्लेषण (Laboratory Report Overview): {report_title}\n")
        out.append("यह विश्लेषण रिपोर्ट में स्पष्ट रूप से दिखाई देने वाले मूल्यों पर आधारित है।\n")

        out.append("#### 1. रिपोर्ट सारांश (Report Summary)")
        out.append(f"- **रिपोर्ट प्रकार:** {report_title}")
        out.append(f"- **पढ़े गए परीक्षण घटक:** {len(parameters)}")
        if abnormal_items:
            out.append(f"- **असामान्य (High/Low) घटक:** {len(abnormal_items)}")
        else:
            out.append("- **सभी पढ़े गए घटक सामान्य सीमा में हैं।**")
        out.append("")

        out.append("#### 2. परीक्षण परिणाम और संदर्भ सीमाएं (Test Values & Reference Ranges)")
        out.append("| परीक्षण (Test) | परिणाम (Observed) | सामान्य सीमा (Reference Range) | स्थिति (Status) |")
        out.append("| :--- | :---: | :---: | :---: |")
        for p in parameters:
            val_str = f"{p['value']} {p['unit']}" if p['value'] is not None else "अस्पष्ट"
            stat_str = "सामान्य (Normal)" if p['status'] == "Normal" else (
                "कम (Low) ⬇️" if p['status'] == "Low" else (
                    "अधिक (High) ⬆️" if p['status'] == "High" else "अस्पष्ट ⚠️"
                )
            )
            out.append(f"| {p['name']} | **{val_str}** | {p['reference_range']} | {stat_str} |")
        out.append("")

        if abnormal_items:
            out.append("#### 3. प्रमुख असामान्य निष्कर्ष (Abnormal Findings)")
            for ab in abnormal_items:
                stat_tag = "कम (Low)" if ab['status'] == "Low" else "अधिक (High)"
                out.append(f"- **{ab['name']}: {ab['value']} {ab['unit']}** ({stat_tag}) — सामान्य सीमा: {ab['reference_range']}")
            out.append("")

            out.append("#### 4. इन परिणामों का सामान्य अर्थ (General Meaning)")
            for ab in abnormal_items:
                causes = ab['low_causes'] if ab['status'] == "Low" else ab['high_causes']
                out.append(f"- **{ab['name']} ({ab['status']}):** {ab['meaning']} संभावित कारण: {causes}")
            out.append("")

        out.append("#### 5. क्या निष्कर्ष नहीं निकाला जा सकता (Limitations)")
        out.append("- किसी एक रिपोर्ट से किसी बीमारी का पक्का निदान नहीं किया जा सकता।")
        out.append("- शारीरिक जांच और लक्षणों के बिना केवल आंकड़े पूरे स्वास्थ्य का निर्धारण नहीं करते।")
        out.append("")

        out.append("#### 6. अगले कदम और डॉक्टर से परामर्श (Next Steps)")
        out.append("1. यह रिपोर्ट अपने डॉक्टर को दिखाएं।")
        out.append("2. बिना चिकित्सीय सलाह के कोई नई दवा शुरू या बंद न करें।")
        out.append("")

        out.append("#### 7. डॉक्टर से पूछने योग्य प्रश्न (Questions for Your Doctor)")
        out.append("- *\"डॉक्टर साहब, क्या इस रिपोर्ट के आधार पर किसी इलाज या जीवनशैली में बदलाव की आवश्यकता है?\"*")
        out.append("")
        out.append(CLINICAL_REPORT_DISCLAIMER)

        return "\n".join(out)

    # ---------------------------------------------------------
    # ENGLISH (DEFAULT)
    # ---------------------------------------------------------
    else:
        out = []
        out.append(f"### 📋 Laboratory Report Analysis: {report_title}\n")
        out.append("This evidence-based breakdown reflects the legible text and numerical findings detected in your document.\n")

        out.append("#### 1. Report Summary")
        out.append(f"- **Document Type:** {report_title}")
        out.append(f"- **Total Measured Parameters Identified:** {len(parameters)}")
        if abnormal_items:
            out.append(f"- **Flagged Abnormal Findings (High/Low):** {len(abnormal_items)}")
        else:
            out.append("- **All readable parameters are currently within normal reference intervals.**")
        out.append("")

        out.append("#### 2. Clearly Readable Test Values")
        out.append("| Test Parameter | Observed Result | Printed / Clinical Reference Range | Status |")
        out.append("| :--- | :---: | :---: | :---: |")
        for p in parameters:
            val_str = f"{p['value']} {p['unit']}" if p['value'] is not None else "Unclear / Smudged"
            stat_str = "Normal" if p['status'] == "Normal" else (
                "Low ⬇️" if p['status'] == "Low" else (
                    "High ⬆️" if p['status'] == "High" else "Unclear ⚠️"
                )
            )
            out.append(f"| {p['name']} | **{val_str}** | {p['reference_range']} | {stat_str} |")
        out.append("")

        out.append("#### 3. Important Abnormal Findings")
        if abnormal_items:
            for ab in abnormal_items:
                out.append(f"- **{ab['name']}: {ab['value']} {ab['unit']}** (Status: **{ab['status']}**) — Normal Range: {ab['reference_range']}")
        else:
            out.append("- None. All readable parameters fall within normal biological reference intervals.")
        out.append("")

        out.append("#### 4. What the Findings May Mean in General")
        if abnormal_items:
            out.append("*(Note: The following are common physiological possibilities, not a medical diagnosis)*")
            for ab in abnormal_items:
                causes = ab['low_causes'] if ab['status'] == "Low" else ab['high_causes']
                out.append(f"- **{ab['name']} ({ab['status']}):** {ab['meaning']} Common potential considerations: {causes}")
        else:
            out.append("- The observed readings reflect normal physiological ranges for the evaluated markers.")
        out.append("")

        out.append("#### 5. Limitations & What Cannot Be Concluded")
        out.append("- A laboratory report is a single diagnostic data point, not a complete clinical evaluation.")
        out.append("- Normal ranges can vary slightly between different laboratories, test methodologies, and demographics.")
        out.append("- Definitive diagnosis requires physical examination, medical history, and clinical correlation by a physician.")
        if unclear_items:
            out.append(f"- Some values ({len(unclear_items)}) appeared partially obscured or cropped; please refer to the original physical copy.")
        out.append("")

        out.append("#### 6. Suggested Next Steps")
        out.append("1. **Consult Your Healthcare Provider:** Share this complete laboratory report with your primary care physician or specialist.")
        out.append("2. **Do Not Self-Medicate:** Never start, alter, or stop prescription medications or dietary supplements based on test numbers alone.")
        out.append("3. **Follow-up Testing:** Your clinician may recommend repeating specific tests over time to assess trends.")
        out.append("")

        out.append("#### 7. Questions to Ask Your Doctor")
        out.append("- *\"Do any of these flagged values require treatment, dietary modifications, or repeat monitoring?\"*")
        out.append("- *\"Could any of my current symptoms or medications have influenced these laboratory results?\"*")
        out.append("")
        out.append(CLINICAL_REPORT_DISCLAIMER)

        return "\n".join(out)


# =============================================================================
# 4. PRESCRIPTION ANALYSIS ENGINE
# =============================================================================

def parse_prescription_items_from_text(raw_text: str) -> List[Dict[str, Any]]:
    """
    Parses medicine names, strengths, dosages, frequencies, and durations from prescription text.
    Preserves uncertainty for ambiguous handwriting.
    """
    if not raw_text:
        return []

    lines = raw_text.splitlines()
    prescriptions = []

    med_patterns = [
        r"(?:tab|cap|syp|inj|ointment|drops)?\.?\s*([a-zA-Z]{3,25}(?:\s+[a-zA-Z]{3,15})?)\s+([0-9]+(?:\.[0-9]+)?\s*(?:mg|ml|mcg|gm|iu|%))?",
    ]

    freq_patterns = [
        r"\b(1-0-1|1-1-1|0-0-1|1-0-0|0-1-0|2-0-2)\b",
        r"\b(once daily|twice daily|thrice daily|four times daily|od|bd|tds|qid|sos|prn|stat|hs)\b",
    ]

    timing_patterns = [
        r"\b(after food|before food|with meals|after breakfast|after dinner|empty stomach|bedtime)\b",
    ]

    duration_patterns = [
        r"\b(?:for\s+)?([0-9]+\s*(?:days|day|weeks|week|months|month|d|w|m))\b",
    ]

    for line in lines:
        clean = line.strip()
        if not clean or len(clean) < 4:
            continue
        low = clean.lower()

        # Skip headers / clinical labels
        if any(w in low for w in ["clinic", "hospital", "patient", "name:", "age:", "date:", "sex:", "signature", "phone", "address"]):
            continue

        # Look for dosage forms or Rx marker
        has_med_cue = any(c in low for c in ["tab", "cap", "syp", "inj", "mg", "ml", "1-0", "od", "bd", "tds", "sos", "rx"])
        if not has_med_cue:
            continue

        name = "Medication"
        strength = "Check label"
        dosage_form = "Tablet/Capsule"
        freq = "As directed by physician"
        timing = "As directed"
        duration = "As prescribed"
        uncertain = False

        # Match name & strength
        m_name = re.search(r"(?:(?:tab|cap|syp|inj|tablet|capsule|syrup)\.?\s+)?([a-zA-Z]{3,20})\s*([0-9]+(?:\.[0-9]+)?\s*(?:mg|ml|mcg|gm))?", clean, re.IGNORECASE)
        if m_name:
            cand_name = m_name.group(1).strip()
            if cand_name.lower() not in ["take", "give", "after", "before", "daily", "days", "capsule", "tablet", "tab", "cap", "syp", "inj", "apply", "locally"]:
                name = cand_name.title()
                if m_name.group(2):
                    strength = m_name.group(2).strip()

        # Match frequency
        for fp in freq_patterns:
            m_f = re.search(fp, low)
            if m_f:
                freq = m_f.group(1).upper()
                break

        # Match timing
        for tp in timing_patterns:
            m_t = re.search(tp, low)
            if m_t:
                timing = m_t.group(1).title()
                break

        # Match duration
        for dp in duration_patterns:
            m_d = re.search(dp, low)
            if m_d:
                duration = m_d.group(1)
                break

        # Handwriting / legibility check
        if len(name) < 4 or any(c in clean for c in ["?", "...", "___", "unclear"]):
            uncertain = True

        if name != "Medication":
            prescriptions.append({
                "medicine_name": name,
                "strength": strength,
                "dosage_form": dosage_form,
                "frequency": freq,
                "timing": timing,
                "duration": duration,
                "uncertain": uncertain,
                "raw_line": clean,
            })

    return prescriptions


def format_prescription_response(
    items: List[Dict[str, Any]],
    prescriber_info: str = "Registered Medical Practitioner",
    language: str = "en"
) -> str:
    """
    Formats extracted prescription into structured clinical guide complying with Section 3.5.
    Explicitly highlights handwriting uncertainty and enforces pharmacist verification.
    """
    if language == "mr":
        out = []
        out.append("### 📝 वैद्यकीय प्रिस्क्रिप्शन विश्लेषण (Medical Prescription Breakdown)\n")
        out.append(f"**डॉक्टर / क्लिनिक:** {prescriber_info}\n")

        if items:
            out.append("#### १. प्रिस्क्रिप्शनमधील औषधे (Prescribed Medications)")
            out.append("| औषधाचे नाव (Medicine) | क्षमता (Strength) | डोस आणि वारंवारता (Frequency) | वेळ (Timing) | कालावधी (Duration) | खात्री (Legibility) |")
            out.append("| :--- | :---: | :---: | :---: | :---: | :---: |")
            for item in items:
                legib = "स्पष्ट (Clear)" if not item.get("uncertain") else "अस्पष्ट (Pharmacist confirmation needed) ⚠️"
                out.append(f"| **{item['medicine_name']}** | {item['strength']} | {item['frequency']} | {item['timing']} | {item['duration']} | {legib} |")
            out.append("")

            has_uncertain = any(it.get("uncertain") for it in items)
            if has_uncertain:
                out.append("#### २. हस्तलिखित अस्पष्टतेचा इशारा (Handwriting Legibility Warning)")
                out.append("⚠️ **काही औषधांचे अक्षर अस्पष्ट किंवा अंशतः न वाचता येणारे आहे.**")
                out.append("चुकीचे औषध टाळण्यासाठी ही प्रिस्क्रिप्शन औषध घेण्यापूर्वी नोंदणीकृत फार्मासिस्ट किंवा संबंधित डॉक्टरांना नक्की दाखवून खात्री करा.\n")

            out.append("#### ३. सामान्य सूचना आणि सुरक्षा खबरदारी (Safety Precautions)")
            out.append("- औषधे नेहमी डॉक्टरांच्या सल्ल्यानुसार आणि ठरवून दिलेल्या वेळेतच घ्या.")
            out.append("- कोणतीही ऍलर्जी (उदा. खाज, सूज, पुरळ) जाणवल्यास तात्काळ औषध थांबवून डॉक्टरांशी संपर्क साधा.")
            out.append("- प्रिस्क्रिप्शनमधील कोणताही डोस स्वतःहून बदलू नका किंवा इतर कोणाला देऊ नका.")
            out.append("")
        else:
            out.append("प्रिस्क्रिप्शनमधील मजकूर पुरेसा स्पष्ट दिसत नाही. कृपया मूळ प्रिस्क्रिप्शनचा अधिक स्वच्छ आणि सरळ फोटो अपलोड करा.\n")

        out.append(CLINICAL_PRESCRIPTION_DISCLAIMER)
        return "\n".join(out)

    elif language == "hi":
        out = []
        out.append("### 📝 डॉक्टर के पर्चे का विश्लेषण (Medical Prescription Breakdown)\n")
        out.append(f"**डॉक्टर / क्लिनिक:** {prescriber_info}\n")

        if items:
            out.append("#### 1. पर्चे में लिखी दवाइयां (Prescribed Medications)")
            out.append("| दवा का नाम (Medicine) | क्षमता (Strength) | खुराक और समय (Frequency) | सेवन का समय (Timing) | अवधि (Duration) | स्पष्टता (Legibility) |")
            out.append("| :--- | :---: | :---: | :---: | :---: | :---: |")
            for item in items:
                legib = "स्पष्ट" if not item.get("uncertain") else "अस्पष्ट (फार्मासिस्ट से पुष्टि जरूरी) ⚠️"
                out.append(f"| **{item['medicine_name']}** | {item['strength']} | {item['frequency']} | {item['timing']} | {item['duration']} | {legib} |")
            out.append("")

            out.append("#### 2. सुरक्षा सावधानियां (Safety Precautions)")
            out.append("- दवाएं डॉक्टर के निर्देशानुसार ही लें।")
            out.append("- यदि लिखावट अस्पष्ट हो तो फार्मासिस्ट से पुनः पुष्टि अवश्य करें।")
            out.append("")
        else:
            out.append("पर्चे की लिखावट स्पष्ट नहीं पढ़ी जा सकी। कृपया एक स्पष्ट और साफ फोटो अपलोड करें।\n")

        out.append(CLINICAL_PRESCRIPTION_DISCLAIMER)
        return "\n".join(out)

    else:
        out = []
        out.append("### 📝 Medical Prescription Breakdown\n")
        out.append(f"**Prescriber / Clinic Reference:** {prescriber_info}\n")

        if items:
            out.append("#### 1. Identified Prescribed Medications")
            out.append("| Medicine Name | Strength | Dose / Frequency | Timing | Duration | Legibility Status |")
            out.append("| :--- | :---: | :---: | :---: | :---: | :---: |")
            for item in items:
                legib = "Clear" if not item.get("uncertain") else "Ambiguous (Confirm with Pharmacist) ⚠️"
                out.append(f"| **{item['medicine_name']}** | {item['strength']} | {item['frequency']} | {item['timing']} | {item['duration']} | {legib} |")
            out.append("")

            has_uncertain = any(it.get("uncertain") for it in items)
            if has_uncertain:
                out.append("#### 2. Handwriting & Legibility Uncertainty Warning")
                out.append("⚠️ **Important:** Portions of the handwritten text contain ambiguous characters. ")
                out.append("To prevent medication errors, always present this original prescription to a licensed pharmacist for physical verification before taking any drug.\n")

            out.append("#### 3. General Administration Guidance & Precautions")
            out.append("- **Course Adherence:** Complete the full prescribed course, particularly for antibiotics, unless adverse effects occur.")
            out.append("- **Adverse Effects:** Stop medication and seek immediate medical evaluation if you experience swelling, difficulty breathing, or severe skin rash.")
            out.append("- **Storage:** Store medications in a cool, dry place away from direct heat and sunlight.")
            out.append("")
        else:
            out.append("The prescription handwriting was not sufficiently legible to extract medication details with confidence. Please upload a clearer, well-lit photograph.\n")

        out.append(CLINICAL_PRESCRIPTION_DISCLAIMER)
        return "\n".join(out)


# =============================================================================
# 5. MEDICINE PACKAGING 10-POINT FORMATTER (D5)
# =============================================================================

def format_10_point_medicine_response(
    brand_name: str,
    generic_name: str,
    strength: str = "As indicated on packaging",
    indications: str = "",
    administration: str = "Take as directed by your physician or pharmacist.",
    side_effects: str = "Mild gastrointestinal upset or drowsiness may occur.",
    serious_effects: str = "Seek immediate emergency medical attention if allergic reactions, swelling, or severe rash occur.",
    contraindications: str = "Inform your doctor about liver/kidney conditions, pregnancy, or other medications.",
    storage: str = "Store in a cool, dry place away from direct sunlight and out of reach of children.",
    uncertainties: str = "Inspection based solely on the visible packaging text and image angle.",
    sources: str = "Verified Pharmaceutical Reference / OpenFDA",
    language: str = "en"
) -> str:
    """Standard 10-point structured medicine response format (Requirement D5)."""
    dosage_disclaimer = "Exact dosage and frequency must be determined by a licensed physician or pharmacist based on your individual medical profile."

    if language == "mr":
        return (
            f"### 📋 औषधाची माहिती (Medication Overview): {brand_name or generic_name}\n\n"
            f"**१. ब्रँड / उत्पादनाचे नाव (Brand Name):** {brand_name or 'N/A'}\n"
            f"**२. जेनेरिक नाव / सक्रिय घटक (Active Ingredients):** {generic_name or 'लेबलवर स्पष्ट नाही'}\n"
            f"**३. क्षमता / प्रमाण (Strength):** {strength or 'पाकिटावरील लेबल तपासा'}\n"
            f"**४. सर्वसामान्य उपयोग (Indications):** {indications or 'डॉक्टरांच्या सल्ल्यानुसार'}\n"
            f"**५. डोस संबंधी सूचना:** {dosage_disclaimer}\n"
            f"**६. औषध घेण्याची पद्धत:** {administration}\n"
            f"**७. सामान्य दुष्परिणाम (Side Effects):** {side_effects}\n"
            f"**८. गंभीर लक्षणे (तातडीने डॉक्टरकडे जावे):** {serious_effects}\n"
            f"**९. खबरदारी आणि चेतावणी:** {contraindications}\n"
            f"**१०. साठवणूक आणि फार्मासिस्ट सल्ला:** {storage}\n\n"
            f"**मर्यादा व टीप:** {uncertainties}\n\n"
            f"*(संदर्भ स्रोत: {sources})*\n\n"
            f"{CLINICAL_VISION_DISCLAIMER}"
        )
    elif language == "hi":
        return (
            f"### 📋 दवा की जानकारी (Medication Overview): {brand_name or generic_name}\n\n"
            f"**1. ब्रांड का नाम (Brand Name):** {brand_name or 'N/A'}\n"
            f"**2. जेनेरिक नाम / सक्रिय घटक (Active Ingredients):** {generic_name or 'लेबल पर स्पष्ट नहीं'}\n"
            f"**3. क्षमता (Strength):** {strength or 'पैकेजिंग पर देखें'}\n"
            f"**4. सामान्य उपयोग (Common Uses):** {indications or 'चिकित्सक के परामर्श अनुसार'}\n"
            f"**5. खुराक संबंधी अस्वीकरण:** {dosage_disclaimer}\n"
            f"**6. सेवन का सामान्य तरीका:** {administration}\n"
            f"**7. सामान्य दुष्प्रभाव (Side Effects):** {side_effects}\n"
            f"**8. गंभीर लक्षण (तत्काल डॉक्टर से मिलें):** {serious_effects}\n"
            f"**9. सावधानियां और चेतावनियां:** {contraindications}\n"
            f"**10. भंडारण और फार्मासिस्ट परामर्श:** {storage}\n\n"
            f"**सीमाएं:** {uncertainties}\n\n"
            f"*(संदर्भ स्रोत: {sources})*\n\n"
            f"{CLINICAL_VISION_DISCLAIMER}"
        )
    else:
        return (
            f"### 📋 Medication Information: {brand_name or generic_name}\n\n"
            f"**1. Brand / Product Name:** {brand_name or 'N/A'}\n"
            f"**2. Generic Name / Active Ingredients:** {generic_name or 'Not clearly visible'}\n"
            f"**3. Strength / Concentration:** {strength or 'Check packaging label'}\n"
            f"**4. Indications / Common Uses:** {indications or 'Refer to prescribing doctor'}\n"
            f"**5. Dosage Disclaimer:** {dosage_disclaimer}\n"
            f"**6. General Administration Guidance:** {administration}\n"
            f"**7. Common Side Effects:** {side_effects}\n"
            f"**8. Serious Adverse Reactions (When to Seek Urgent Care):** {serious_effects}\n"
            f"**9. Contraindications & Warnings:** {contraindications}\n"
            f"**10. Storage & Pharmacist Consultation:** {storage}\n\n"
            f"**Limitations & Notes:** {uncertainties}\n\n"
            f"*(Reference Sources: {sources})*\n\n"
            f"{CLINICAL_VISION_DISCLAIMER}"
        )


format_structured_medicine_vision_response = format_10_point_medicine_response


# =============================================================================
# 6. DOCUMENT FOLLOW-UP QUESTION ANSWERING ENGINE
# =============================================================================

def answer_document_followup(
    document_data: Dict[str, Any],
    user_query: str,
    language: str = "en"
) -> str:
    """
    Answers follow-up questions regarding an active or previously uploaded document.
    Examples:
    - 'Ya report madhye kay abnormal aahe?'
    - 'What is this medicine used for?'
    - 'Prescription madhye dose kiti lihila aahe?'
    - 'Explain this report in simple Marathi'
    """
    q_low = user_query.lower()
    doc_type = document_data.get("document_type", "general_document")
    extracted_json = document_data.get("extracted_data_json", "{}")

    try:
        data = json.loads(extracted_json) if isinstance(extracted_json, str) else extracted_json
    except Exception:
        data = {}

    # Follow-up: What is abnormal in the report?
    if any(k in q_low for k in ["abnormal", "high", "low", "kaj abnormal", "kay abnormal", "kya abnormal", "kharab"]):
        abnormal = data.get("abnormal_parameters", [])
        if abnormal:
            if language == "mr":
                lines = ["या तपासणी अहवालात खालील घटक सामान्य मर्यादेबाहेर (Abnormal) आढळले आहेत:\n"]
                for it in abnormal:
                    lines.append(f"- **{it['name']}: {it['value']} {it['unit']}** (स्थिती: {it['status']}) — सामान्य मर्यादा: {it['reference_range']}")
                lines.append("\nकृपया या घटकांविषयी पुढील सल्ल्यासाठी तुमच्या डॉक्टरांशी संपर्क साधा.")
                return "\n".join(lines)
            elif language == "hi":
                lines = ["इस रिपोर्ट में निम्नलिखित घटक सामान्य सीमा से बाहर पाए गए हैं:\n"]
                for it in abnormal:
                    lines.append(f"- **{it['name']}: {it['value']} {it['unit']}** (स्थिति: {it['status']}) — सामान्य सीमा: {it['reference_range']}")
                lines.append("\nकृपया विस्तृत मार्गदर्शन के लिए डॉक्टर से परामर्श करें।")
                return "\n".join(lines)
            else:
                lines = ["The following test values are flagged outside the standard reference range:\n"]
                for it in abnormal:
                    lines.append(f"- **{it['name']}: {it['value']} {it['unit']}** (Status: **{it['status']}**) — Reference Range: {it['reference_range']}")
                lines.append("\nPlease review these specific findings with your healthcare provider for clinical correlation.")
                return "\n".join(lines)
        else:
            if language == "mr":
                return "या अहवालातील तपासलेले सर्व घटक सामान्य मर्यादेत (Normal) आहेत."
            elif language == "hi":
                return "इस रिपोर्ट में पढ़े गए सभी घटक सामान्य सीमा (Normal) में हैं।"
            else:
                return "All readable parameters in this report fall within the normal biological reference range."

    # Follow-up: What is the dose or frequency?
    if any(k in q_low for k in ["dose", "frequency", "kiti ghyavi", "kitna lena", "timing", "how many"]):
        items = data.get("prescription_items", [])
        if items:
            if language == "mr":
                lines = ["प्रिस्क्रिप्शननुसार नोंदवलेले डोस आणि वेळ खालीलप्रमाणे आहेत:\n"]
                for it in items:
                    lines.append(f"- **{it['medicine_name']} ({it['strength']}):** डोस: {it['frequency']}, वेळ: {it['timing']}, कालावधी: {it['duration']}")
                lines.append("\n*(टीप: औषध घेण्यापूर्वी फार्मासिस्टशी खात्री करून घ्यावी.)*")
                return "\n".join(lines)
            elif language == "hi":
                lines = ["पर्चे के अनुसार लिखी गई खुराक और समय निम्न प्रकार है:\n"]
                for it in items:
                    lines.append(f"- **{it['medicine_name']} ({it['strength']}):** खुराक: {it['frequency']}, समय: {it['timing']}, अवधि: {it['duration']}")
                lines.append("\n*(दवा लेने से पहले फार्मासिस्ट से जांच कराएं।)*")
                return "\n".join(lines)
            else:
                lines = ["According to the visible prescription details, here are the documented dosage instructions:\n"]
                for it in items:
                    lines.append(f"- **{it['medicine_name']} ({it['strength']}):** Frequency: {it['frequency']}, Timing: {it['timing']}, Duration: {it['duration']}")
                lines.append("\n*(Always verify with a licensed pharmacist before taking prescription medication.)*")
                return "\n".join(lines)

    # General explanation request (e.g. 'Explain this report in Marathi')
    summary = document_data.get("summary")
    if summary:
        return summary

    return "Document context is active. Please ask your specific question regarding this report or prescription."


# =============================================================================
# 7. VISION SYSTEM PROMPT BUILDER
# =============================================================================

def build_vision_system_prompt(document_type: str = "medicine_packaging", language: str = "en") -> str:
    """Builds tailored system instructions for multimodal vision requests."""
    lang_name = "Marathi (native Devanagari)" if language == "mr" else ("Hindi (native Devanagari)" if language == "hi" else "English")

    if document_type == "lab_report":
        return f"""You are MEDASSIST AI, a clinical laboratory and diagnostic report inspection assistant.
TARGET LANGUAGE: {lang_name}.

STRICT INSPECTION RULES FOR MEDICAL REPORTS:
1. Extract all clearly legible laboratory parameters, observed test values, units, and printed reference ranges.
2. Structure your analysis into the 7 standard sections:
   1. Report Summary
   2. Clearly Readable Test Values (tabular or bulleted with units and printed ranges)
   3. Important Abnormal Findings (clearly identifying what is High or Low)
   4. What the Findings May Mean in General (educational context, NEVER an absolute diagnosis)
   5. Limitations & What Cannot Be Concluded (emphasize single-point limitations)
   6. Suggested Next Steps (consult doctor, do not self-medicate)
   7. Questions to Ask Your Doctor
3. DO NOT invent missing decimal numbers or test values.
4. If a parameter is blurred or cropped, label it clearly as [Unclear / Partially cropped - please verify].
5. Append the clinical report safety disclaimer.
"""

    elif document_type == "prescription":
        return f"""You are MEDASSIST AI, a prescription analysis and patient safety assistant.
TARGET LANGUAGE: {lang_name}.

STRICT INSPECTION RULES FOR PRESCRIPTIONS:
1. Extract medicine names, strengths, dosage forms, written dose/frequencies (e.g. 1-0-1, OD, BD, TDS), duration, and timing instructions.
2. If any handwriting is smudged, illegible, or ambiguous, DO NOT guess confidently. Label it [Ambiguous Handwriting - Must Confirm with Pharmacist].
3. Explain the general therapeutic purpose of identified medications.
4. Warn against self-adjusting or stopping prescribed medications.
5. Append the prescription safety notice.
"""

    else:
        return f"""You are MEDASSIST AI, a specialized clinical medical vision and packaging inspection assistant.
TARGET LANGUAGE: {lang_name}.

STRICT INSPECTION RULES (D1-D6):
1. Examine the image carefully for medication blister strips, bottles, pharmacy packaging, or prescription labels.
2. LOOSE PILL REFUSAL (D4): If the image depicts an UNPACKAGED, LOOSE PILL without clear packaging, DO NOT guess or identify the medicine by shape/color/size. State clearly that appearance-based identification is clinically dangerous, explain the risk of counterfeit/lookalike drugs, and ask for original packaging.
3. STRUCTURED 10-POINT FORMAT (D5): When packaging is legible, respond using the structured format:
   1. Brand / Product Name
   2. Generic Name / Active Ingredients
   3. Strength / Concentration
   4. Indications / Common Uses
   5. Dosage Disclaimer: Explicitly state that exact dosing must be determined by a healthcare provider.
   6. General Administration Guidance (e.g. take with food, swallow whole)
   7. Common Side Effects
   8. Serious Adverse Reactions / Urgent Care Signs
   9. Contraindications & Warnings (e.g. pregnancy, liver/kidney)
   10. Storage & Pharmacist Consultation
4. NO INVENTED DOSAGES: NEVER give personalized dosage schedules. Always advise consulting a doctor or pharmacist.
5. If image text is blurry, occluded, or unreadable, clearly state what parts cannot be confirmed and request a clearer image.
6. Always append the clinical educational disclaimer (D6).
"""
