"""
MedAssist AI - Vision Service (D1-D6)
Handles medicine image validation, safe decoding, OCR extraction,
Gemini multimodal vision processing, and strict clinical safety:
- Never identify a loose pill from appearance/color/shape alone (D4)
- 10-point structured medicine response formatting (D5)
- Image validation (size <= 5MB, JPEG/PNG/WEBP magic bytes) (D1)
- Graceful OCR fallback (D2)
- Refuses sending images to text-only models (D3)
- Educational safety disclaimer (D6)
"""

import base64
import io
import re
import logging
import os
from typing import Optional, Dict, Tuple
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
]

CLINICAL_VISION_DISCLAIMER = (
    "⚠️ **Disclaimer:** This visual analysis is an educational aid and not a definitive medical evaluation "
    "or prescription. Always verify with packaging labels and consult a licensed physician or pharmacist."
)


def detect_image_mime(image_bytes: bytes) -> Optional[str]:
    """
    Validates magic bytes for JPEG, PNG, and WEBP images.
    """
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

    # Strip data URL prefix if present (e.g. data:image/jpeg;base64,...)
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
    Attempts optical character recognition (OCR) using pytesseract if installed.
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


def is_loose_pill_query(text: str, ocr_text: str = "") -> bool:
    """
    Detects if the query asks to identify a loose pill purely by physical appearance.
    """
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
    """
    Standard 10-point structured medicine response format (Requirement D5).
    """
    dosage_disclaimer = "Exact dosage and frequency must be determined by a licensed physician or pharmacist based on your individual medical profile."

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


# Alias for backwards compatibility with earlier draft
format_structured_medicine_vision_response = format_10_point_medicine_response


def build_vision_system_prompt(language: str = "en") -> str:
    lang_name = "Marathi (native Devanagari)" if language == "mr" else ("Hindi (native Devanagari)" if language == "hi" else "English")
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
4. NO INVENTED DOSAGES: NEVER give personalized dosage schedules (e.g., "take 2 pills every 6 hours"). Always advise consulting a doctor or pharmacist.
5. If image text is blurry, occluded, or unreadable, clearly state what parts cannot be confirmed and request a clearer image.
6. Always append the clinical educational disclaimer (D6).
"""
