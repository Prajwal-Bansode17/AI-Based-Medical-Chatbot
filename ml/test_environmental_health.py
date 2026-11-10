"""
Automated Test Suite for Atmospheric Intelligence & Regional Epidemiological Advisory Engine
Tests:
1. In-memory weather & AQI caching with 30-minute TTL expiration.
2. Offline fallback behavior when Open-Meteo is unreachable or times out.
3. Seasonal disease alert mapping for winter, monsoon, and summer across EN, MR, and HI.
4. Statutory citizen health advisory formulation with helplines (108, 104, 112, 14416), PHC navigation, and Ayushman Bharat PM-JAY.
5. End-to-end integration across /chat and /predict endpoints with weather block attachment.
"""

import sys
import time
import unittest
from pathlib import Path
from unittest.mock import patch, MagicMock

BASE_DIR = Path(__file__).resolve().parent
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

import environmental_health as eh
from environmental_health import (
    get_atmospheric_context,
    get_regional_health_alert,
    get_citizen_advisory,
    format_atmospheric_advisory_block,
    clear_atmospheric_cache,
    CITY_COORDINATES,
    DEFAULT_LAT,
    DEFAULT_LON,
)
from conversation_engine import attach_atmospheric_advisory
from api import app


class TestAtmosphericIntelligence(unittest.TestCase):

    def setUp(self):
        clear_atmospheric_cache()
        self.client = app.test_client()

    def tearDown(self):
        clear_atmospheric_cache()

    def test_city_coordinates_resolution(self):
        c_lat, c_lon = eh._resolve_coordinates(city="pune")
        self.assertAlmostEqual(c_lat, 18.5204, places=3)
        self.assertAlmostEqual(c_lon, 73.8567, places=3)

        # Mumbai
        m_lat, m_lon = eh._resolve_coordinates(city="mumbai")
        self.assertAlmostEqual(m_lat, 19.0760, places=3)

        # Default fallback
        d_lat, d_lon = eh._resolve_coordinates(city="unknown_location")
        self.assertEqual(d_lat, DEFAULT_LAT)
        self.assertEqual(d_lon, DEFAULT_LON)

    def test_in_memory_cache_and_ttl(self):
        # 1. Fetch live or cached data
        ctx1 = get_atmospheric_context(lat=19.8762, lon=75.3433)
        self.assertIn("temperature_c", ctx1)
        self.assertIn("aqi", ctx1)

        # Ensure it is in cache
        cache_key = (round(19.8762, 2), round(75.3433, 2))
        self.assertIn(cache_key, eh._ATMOSPHERIC_CACHE)

        # 2. Modify cached item to verify fast hit
        ts, cached_dict = eh._ATMOSPHERIC_CACHE[cache_key]
        cached_dict["temperature_c"] = 99.9  # Marker
        ctx2 = get_atmospheric_context(lat=19.8762, lon=75.3433)
        self.assertEqual(ctx2["temperature_c"], 99.9)

        # 3. Fast-forward time beyond TTL (1800 seconds)
        eh._ATMOSPHERIC_CACHE[cache_key] = (time.time() - 1801, cached_dict)
        # Should re-fetch or fallback and not return the old 99.9 value
        with patch("urllib.request.urlopen") as mock_open:
            mock_open.side_effect = Exception("Simulated network outage")
            ctx3 = get_atmospheric_context(lat=19.8762, lon=75.3433)
            self.assertNotEqual(ctx3["temperature_c"], 99.9)

    def test_offline_fallback_behavior(self):
        clear_atmospheric_cache()
        with patch("urllib.request.urlopen") as mock_open:
            mock_open.side_effect = Exception("Connection timeout / offline")
            fallback = get_atmospheric_context(lat=18.5204, lon=73.8567)

            self.assertTrue(fallback.get("is_fallback"))
            self.assertEqual(fallback.get("source"), "seasonal_fallback")
            self.assertIn(fallback.get("season"), ("winter", "monsoon", "summer"))
            self.assertTrue(10.0 <= fallback["temperature_c"] <= 45.0)
            self.assertTrue(0 <= fallback["aqi"] <= 500)

    def test_seasonal_disease_alert_mapping(self):
        # Winter alert (Month 12 - December)
        w_alert_en = get_regional_health_alert("cough and asthma", month=12, language="en")
        self.assertIn("NCDC", w_alert_en)
        self.assertIn("ARI", w_alert_en)

        w_alert_mr = get_regional_health_alert("खोकला व दमा", month=1, language="mr")
        self.assertIn("एनसीडीसी", w_alert_mr)
        self.assertIn("श्वसनविकार", w_alert_mr)

        w_alert_hi = get_regional_health_alert("सांस की तकलीफ", month=2, language="hi")
        self.assertIn("एनसीडीसी", w_alert_hi)
        self.assertIn("इन्फ्लूएंजा", w_alert_hi)

        # Monsoon alert (Month 8 - August)
        m_vec_en = get_regional_health_alert("fever and joint pain", month=8, language="en")
        self.assertIn("Dengue", m_vec_en)
        self.assertIn("Malaria", m_vec_en)

        m_vec_mr = get_regional_health_alert("ताप आणि अंगदुखी", month=8, language="mr")
        self.assertIn("डेंग्यू", m_vec_mr)
        self.assertIn("मलेरिया", m_vec_mr)

        m_gastro_hi = get_regional_health_alert("पेट दर्द और उल्टी", month=7, language="hi")
        self.assertIn("गैस्ट्रोएंटेराइटिस", m_gastro_hi)
        self.assertIn("ओआरएस", m_gastro_hi)

        # Summer alert (Month 5 - May)
        s_heat_en = get_regional_health_alert("dehydration and dizziness", month=5, language="en")
        self.assertIn("NDMA", s_heat_en)
        self.assertIn("Heat", s_heat_en)

        s_heat_mr = get_regional_health_alert("उष्माघात आणि चक्कर", month=4, language="mr")
        self.assertIn("उष्माघात", s_heat_mr)
        self.assertIn("डिहायड्रेशन", s_heat_mr)

        s_heat_hi = get_regional_health_alert("लू और कमजोरी", month=5, language="hi")
        self.assertIn("उष्माघात", s_heat_hi)
        self.assertIn("लू", s_heat_hi)

    def test_statutory_citizen_advisory_helplines(self):
        adv_en = get_citizen_advisory(language="en")
        self.assertIn("108", adv_en)
        self.assertIn("104", adv_en)
        self.assertIn("14416", adv_en)
        self.assertIn("Primary Health Centre", adv_en)
        self.assertIn("Ayushman Bharat", adv_en)

        adv_mr = get_citizen_advisory(language="mr")
        self.assertIn("१०८", adv_mr)
        self.assertIn("१०४", adv_mr)
        self.assertIn("१४४१६", adv_mr)
        self.assertIn("प्राथमिक आरोग्य केंद्र", adv_mr)
        self.assertIn("आयुष्मान भारत", adv_mr)

        adv_hi = get_citizen_advisory(language="hi")
        self.assertIn("108", adv_hi)
        self.assertIn("104", adv_hi)
        self.assertIn("14416", adv_hi)
        self.assertIn("प्राथमिक स्वास्थ्य केंद्र", adv_hi)
        self.assertIn("आयुष्मान भारत", adv_hi)

    def test_format_atmospheric_advisory_block_filtering(self):
        # Unrelated symptom (e.g. fracture / sprain) -> should return None
        block_irrelevant = format_atmospheric_advisory_block(
            symptoms=["bone_fracture"],
            city="pune",
            language="en"
        )
        self.assertIsNone(block_irrelevant)

        # Respiratory symptom -> returns formatted block with header and helplines
        block_respiratory = format_atmospheric_advisory_block(
            symptoms=["cough", "cold"],
            city="pune",
            language="en"
        )
        self.assertIsNotNone(block_respiratory)
        self.assertIn("🌦️ **Local Weather & Health Advisory", block_respiratory)
        self.assertIn("108", block_respiratory)

        # Marathi block
        block_mr = format_atmospheric_advisory_block(
            symptoms=["खोकला", "ताप"],
            city="aurangabad",
            language="mr"
        )
        self.assertIsNotNone(block_mr)
        self.assertIn("स्थानिक हवामान व आरोग्य सूचना", block_mr)
        self.assertIn("१०८", block_mr)

    def test_attach_atmospheric_advisory_helper(self):
        base = "Take rest and drink plenty of water."
        attached = attach_atmospheric_advisory(
            base_response=base,
            symptoms=["cough"],
            city="pune",
            language="en"
        )
        self.assertTrue(len(attached) > len(base))
        self.assertIn(base, attached)
        self.assertIn("🌦️", attached)

    def test_chat_endpoint_environmental_integration(self):
        res = self.client.post("/chat", json={
            "message": "I have been having bad cough and throat irritation",
            "city": "pune",
            "language": "en"
        })
        self.assertEqual(res.status_code, 200)
        data = res.get_json()
        self.assertIn("108", data["response"])
        self.assertIsNotNone(data.get("atmospheric_context"))
        self.assertIn("temperature_c", data["atmospheric_context"])
        self.assertIsNotNone(data.get("atmospheric_advisory"))

    def test_predict_endpoint_environmental_integration(self):
        res = self.client.post("/predict", json={
            "text": "मला २ दिवसांपासून खूप खोकला आणि सर्दी आहे",
            "city": "aurangabad",
            "language": "mr"
        })
        self.assertEqual(res.status_code, 200)
        data = res.get_json()
        ans = data.get("answer") or data.get("response") or ""
        self.assertIn("१०८", ans)
        self.assertIsNotNone(data.get("atmospheric_context"))
        self.assertIn("तापमान", ans)


if __name__ == "__main__":
    unittest.main()
