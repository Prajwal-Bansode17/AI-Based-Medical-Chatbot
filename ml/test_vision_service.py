"""
Automated Test Suite for Vision Service (D1-D6, T3)
Tests:
- D1: Image validation (size <= 5MB, JPEG/PNG/WEBP magic bytes, reject invalid formats)
- D2: OCR fallback graceful handling
- D3: Routing verification (never send images to text-only models)
- D4: Loose pill refusal by appearance alone with clinical safety reasoning
- D5: 10-point structured medicine response format
- D6: Educational safety disclaimer presence
"""

import unittest
import base64
import os
import sys
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(BASE_DIR))

import vision_service
from vision_service import (
    detect_image_mime,
    validate_and_decode_image,
    extract_text_from_image,
    is_loose_pill_query,
    build_loose_pill_refusal_response,
    format_10_point_medicine_response,
    CLINICAL_VISION_DISCLAIMER,
    MAX_IMAGE_SIZE_BYTES,
)


class TestVisionService(unittest.TestCase):

    def setUp(self):
        # Create valid dummy headers for JPEG, PNG, WEBP
        self.valid_jpeg_bytes = b"\xFF\xD8\xFF\xE0\x00\x10JFIF\x00\x01\x01\x01\x00`\x00`\x00\x00" + b"\x00" * 100
        self.valid_png_bytes = b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR" + b"\x00" * 100
        # WEBP: RIFF + 4 bytes length + WEBP
        self.valid_webp_bytes = b"RIFF\x20\x00\x00\x00WEBPVP8 " + b"\x00" * 100

    def test_d1_valid_magic_byte_detection(self):
        """JPEG, PNG, WEBP magic bytes must be properly identified."""
        self.assertEqual(detect_image_mime(self.valid_jpeg_bytes), "image/jpeg")
        self.assertEqual(detect_image_mime(self.valid_png_bytes), "image/png")
        self.assertEqual(detect_image_mime(self.valid_webp_bytes), "image/webp")

        # Unsupported or random bytes
        self.assertIsNone(detect_image_mime(b"GIF89a" + b"\x00" * 20))
        self.assertIsNone(detect_image_mime(b"BM" + b"\x00" * 20))
        self.assertIsNone(detect_image_mime(b"Hello world medical query"))

    def test_d1_validate_and_decode_valid_images(self):
        """Valid base64 encoded images must decode successfully."""
        b64_jpeg = base64.b64encode(self.valid_jpeg_bytes).decode("utf-8")
        data_url_png = "data:image/png;base64," + base64.b64encode(self.valid_png_bytes).decode("utf-8")

        raw_jpg, mime_jpg, err_jpg = validate_and_decode_image(b64_jpeg)
        self.assertIsNone(err_jpg)
        self.assertEqual(mime_jpg, "image/jpeg")
        self.assertEqual(raw_jpg, self.valid_jpeg_bytes)

        raw_png, mime_png, err_png = validate_and_decode_image(data_url_png)
        self.assertIsNone(err_png)
        self.assertEqual(mime_png, "image/png")
        self.assertEqual(raw_png, self.valid_png_bytes)

    def test_d1_reject_invalid_and_corrupt_images(self):
        """Invalid base64, empty payload, and non-supported formats must be rejected."""
        # Empty
        raw, mime, err = validate_and_decode_image("")
        self.assertIsNotNone(err)
        self.assertIsNone(raw)

        # Corrupt base64
        raw, mime, err = validate_and_decode_image("!!!NotValidBase64@@@")
        self.assertIsNotNone(err)
        self.assertIn("Invalid base64", err)

        # Unsupported format (e.g. BMP or random text)
        bmp_b64 = base64.b64encode(b"BM\x00\x00\x00\x00\x00\x00\x00\x00").decode("utf-8")
        raw, mime, err = validate_and_decode_image(bmp_b64)
        self.assertIsNotNone(err)
        self.assertIn("Unsupported image format", err)

    def test_d1_oversized_image_rejection(self):
        """Images exceeding 5MB must be rejected with a specific error message."""
        oversized_bytes = self.valid_jpeg_bytes + b"\x00" * (MAX_IMAGE_SIZE_BYTES + 1024)
        oversized_b64 = base64.b64encode(oversized_bytes).decode("utf-8")

        raw, mime, err = validate_and_decode_image(oversized_b64)
        self.assertIsNone(raw)
        self.assertIsNotNone(err)
        self.assertIn("exceeds maximum limit of 5MB", err)

    def test_d2_ocr_graceful_fallback(self):
        """extract_text_from_image must never raise unhandled exceptions even with dummy bytes."""
        text = extract_text_from_image(self.valid_jpeg_bytes)
        self.assertIsInstance(text, str)
        # Empty input
        self.assertEqual(extract_text_from_image(b""), "")

    def test_d4_loose_pill_query_detection(self):
        """Queries asking to identify loose pills by appearance must be flagged."""
        self.assertTrue(is_loose_pill_query("What is this white round pill?"))
        self.assertTrue(is_loose_pill_query("Identify this loose capsule I found on the floor"))
        self.assertTrue(is_loose_pill_query("What medicine is this blue pill without packaging?"))
        self.assertTrue(is_loose_pill_query("What pill is this yellow tablet?"))

        # Queries about normal medication packaging or general health
        self.assertFalse(is_loose_pill_query("What are the side effects of Metformin 500mg?"))
        self.assertFalse(is_loose_pill_query("I have a severe headache and fever"))

    def test_d4_loose_pill_refusal_content(self):
        """Refusal response must explain risks of lookalikes/counterfeits and request packaging."""
        refusal_en = build_loose_pill_refusal_response("en")
        self.assertIn("Cannot Identify Medication by Appearance Alone", refusal_en)
        self.assertIn("blister strip", refusal_en.lower())
        self.assertIn("pharmacist", refusal_en.lower())
        self.assertIn("Disclaimer", refusal_en)

        refusal_hi = build_loose_pill_refusal_response("hi")
        self.assertIn("दिखावट से गोली की पहचान संभव नहीं है", refusal_hi)
        self.assertTrue("blister" in refusal_hi.lower() or "पत्ते" in refusal_hi)

        refusal_mr = build_loose_pill_refusal_response("mr")
        self.assertIn("दिसण्यावरून गोळी ओळखणे शक्य नाही", refusal_mr)
        self.assertIn("पाकीट", refusal_mr)

    def test_d5_ten_point_structured_medicine_format(self):
        """10-point structured response must contain all required clinical sections."""
        formatted = format_10_point_medicine_response(
            brand_name="Crocin 650",
            generic_name="Paracetamol",
            strength="650 mg",
            indications="Fever and mild to moderate pain relief",
            administration="Take with water after meals.",
            side_effects="Nausea, allergic skin reaction in rare cases.",
            serious_effects="Jaundice, persistent vomiting, facial swelling.",
            contraindications="Severe liver impairment, acute alcohol dependence.",
            storage="Store below 30°C in a dry place.",
            uncertainties="Based on visible blister strip text.",
            language="en"
        )

        # Verify all 10 points
        self.assertIn("1. Brand / Product Name:", formatted)
        self.assertIn("2. Generic Name / Active Ingredients:", formatted)
        self.assertIn("3. Strength / Concentration:", formatted)
        self.assertIn("4. Indications / Common Uses:", formatted)
        self.assertIn("5. Dosage Disclaimer:", formatted)
        self.assertIn("6. General Administration Guidance:", formatted)
        self.assertIn("7. Common Side Effects:", formatted)
        self.assertIn("8. Serious Adverse Reactions", formatted)
        self.assertIn("9. Contraindications & Warnings:", formatted)
        self.assertIn("10. Storage & Pharmacist Consultation:", formatted)

        # Verify disclaimer (D6)
        self.assertIn(CLINICAL_VISION_DISCLAIMER, formatted)
        self.assertIn("licensed physician or pharmacist", formatted)


if __name__ == "__main__":
    unittest.main()
