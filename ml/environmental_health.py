"""
MedAssist AI - Atmospheric Intelligence & Regional Epidemiological Advisory Engine
Engine for Indian citizens adhering to NCDC (National Centre for Disease Control) &
IDSP (Integrated Disease Surveillance Programme) guidelines.

Features:
- Lightweight Weather & AQI integration via Open-Meteo (zero API key, native urllib.request).
- In-memory 30-minute TTL cache with seasonal fallback for offline/timeout resilience.
- Regional seasonal epidemiological alerts (Winter ARI/Smog/H1N1, Monsoon Dengue/Malaria/Gastro, Summer Heat/Dehydration).
- Statutory Citizen Health Advisory (108 Ambulance, 104 Medical Advice, 112 Universal, 14416 Tele-MANAS, PHC/Civil Hospitals, Ayushman Bharat PM-JAY / MJPJAY).
- Ultra-low RAM overhead (< 1 MB) for Render 512MB free tier compatibility.
"""

import json
import logging
import re
import time
import urllib.request
import urllib.error
from datetime import datetime
from typing import Dict, Optional, Tuple, Any

logger = logging.getLogger("medassist.environmental_health")

# Default coordinates for Maharashtra / central India (Chhatrapati Sambhajinagar / Aurangabad region)
DEFAULT_LAT = 19.8762
DEFAULT_LON = 75.3433

# Indian City Coordinate Mapping
CITY_COORDINATES: Dict[str, Tuple[float, float]] = {
    "mumbai": (19.0760, 72.8777),
    "pune": (18.5204, 73.8567),
    "aurangabad": (19.8762, 75.3433),
    "chhatrapati sambhajinagar": (19.8762, 75.3433),
    "nagpur": (21.1458, 79.0882),
    "nashik": (19.9975, 73.7898),
    "thane": (19.2183, 72.9781),
    "solapur": (17.6599, 75.9064),
    "kolhapur": (16.7050, 74.2433),
    "amravati": (20.9374, 77.7796),
    "nanded": (19.1383, 77.3210),
    "jalgaon": (21.0077, 75.5626),
    "delhi": (28.6139, 77.2090),
    "new delhi": (28.6139, 77.2090),
    "bengaluru": (12.9716, 77.5946),
    "bangalore": (12.9716, 77.5946),
    "hyderabad": (17.3850, 78.4867),
    "kolkata": (22.5726, 88.3639),
    "chennai": (13.0827, 80.2707),
    "ahmedabad": (23.0225, 72.5714),
    "jaipur": (26.9124, 75.7873),
    "lucknow": (26.8467, 80.9462),
    "patna": (25.5941, 85.1376),
    "bhopal": (23.2599, 77.4126),
    "indore": (22.7196, 75.8577),
}

# 30-minute in-memory cache: (lat, lon) -> (timestamp, data_dict)
_ATMOSPHERIC_CACHE: Dict[Tuple[float, float], Tuple[float, Dict[str, Any]]] = {}
CACHE_TTL_SECONDS = 1800  # 30 minutes


def _resolve_coordinates(
    lat: Optional[float] = None,
    lon: Optional[float] = None,
    city: Optional[str] = None
) -> Tuple[float, float]:
    """Resolves coordinates from explicit lat/lon, city name, or regional default."""
    if lat is not None and lon is not None:
        try:
            return float(lat), float(lon)
        except (ValueError, TypeError):
            pass

    if city and isinstance(city, str):
        clean_city = city.strip().lower()
        if clean_city in CITY_COORDINATES:
            return CITY_COORDINATES[clean_city]

    return DEFAULT_LAT, DEFAULT_LON


def _get_aqi_category(us_aqi: Optional[float], language: str = "en") -> str:
    """Classifies US AQI into standard categories with localized descriptions."""
    lang = (language or "en").strip().lower()
    if us_aqi is None:
        return "Unknown"

    val = float(us_aqi)
    if val <= 50:
        if lang in ("mr", "roman_mr"):
            return "उत्तम (Good)"
        elif lang in ("hi", "hinglish"):
            return "अच्छा (Good)"
        return "Good"
    elif val <= 100:
        if lang in ("mr", "roman_mr"):
            return "समाधानकारक (Moderate)"
        elif lang in ("hi", "hinglish"):
            return "मध्यम (Moderate)"
        return "Moderate"
    elif val <= 200:
        if lang in ("mr", "roman_mr"):
            return "अस्वस्थकारक (Poor)"
        elif lang in ("hi", "hinglish"):
            return "खराब (Poor)"
        return "Poor / Sensitive"
    elif val <= 300:
        if lang in ("mr", "roman_mr"):
            return "अत्यंत वाईट (Very Poor)"
        elif lang in ("hi", "hinglish"):
            return "बहुत खराब (Very Poor)"
        return "Very Poor"
    else:
        if lang in ("mr", "roman_mr"):
            return "धोकादायक (Severe)"
        elif lang in ("hi", "hinglish"):
            return "गंभीर (Severe)"
        return "Severe"


def _get_seasonal_fallback(month: Optional[int] = None) -> Dict[str, Any]:
    """Provides representative Indian meteorological and AQI defaults by season."""
    m = month if month is not None else datetime.now().month

    if m in (11, 12, 1, 2):  # Winter (Nov-Feb)
        return {
            "temperature_c": 22.0,
            "humidity_pct": 45.0,
            "pm2_5": 62.0,
            "pm10": 110.0,
            "aqi": 154,
            "aqi_category": "Poor / Sensitive",
            "season": "winter",
            "source": "seasonal_fallback",
            "is_fallback": True,
        }
    elif m in (7, 8, 9, 10):  # Monsoon / Post-Monsoon (Jul-Oct)
        return {
            "temperature_c": 27.5,
            "humidity_pct": 84.0,
            "pm2_5": 22.0,
            "pm10": 38.0,
            "aqi": 48,
            "aqi_category": "Good",
            "season": "monsoon",
            "source": "seasonal_fallback",
            "is_fallback": True,
        }
    else:  # Summer (Mar-Jun)
        return {
            "temperature_c": 36.5,
            "humidity_pct": 28.0,
            "pm2_5": 38.0,
            "pm10": 85.0,
            "aqi": 98,
            "aqi_category": "Moderate",
            "season": "summer",
            "source": "seasonal_fallback",
            "is_fallback": True,
        }


def get_atmospheric_context(
    lat: Optional[float] = None,
    lon: Optional[float] = None,
    city: Optional[str] = None,
    timeout: float = 2.0
) -> Dict[str, Any]:
    """
    Fetches real-time weather and air quality metrics from Open-Meteo.
    Uses in-memory cache with 30-minute TTL.
    Gracefully falls back to regional seasonal defaults on failure.
    """
    c_lat, c_lon = _resolve_coordinates(lat, lon, city)
    cache_key = (round(c_lat, 2), round(c_lon, 2))
    now = time.time()

    # Check cache
    if cache_key in _ATMOSPHERIC_CACHE:
        cached_time, cached_data = _ATMOSPHERIC_CACHE[cache_key]
        if now - cached_time < CACHE_TTL_SECONDS:
            return dict(cached_data)

    temp_c = None
    humidity = None
    pm2_5 = None
    pm10 = None
    us_aqi = None

    headers = {"User-Agent": "MedAssist-AI-Atmospheric-Engine/1.0"}

    # 1. Fetch current weather (temperature, relative humidity)
    try:
        weather_url = (
            f"https://api.open-meteo.com/v1/forecast?"
            f"latitude={c_lat}&longitude={c_lon}&current=temperature_2m,relative_humidity_2m"
        )
        req = urllib.request.Request(weather_url, headers=headers)
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            current = data.get("current", {})
            temp_c = current.get("temperature_2m")
            humidity = current.get("relative_humidity_2m")
    except Exception as ex:
        logger.debug("Open-Meteo weather query failed (%s): %s", weather_url, ex)

    # 2. Fetch current air quality (pm2_5, pm10, us_aqi)
    try:
        aqi_url = (
            f"https://air-quality-api.open-meteo.com/v1/air-quality?"
            f"latitude={c_lat}&longitude={c_lon}&current=pm10,pm2_5,us_aqi"
        )
        req = urllib.request.Request(aqi_url, headers=headers)
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            current = data.get("current", {})
            pm2_5 = current.get("pm2_5")
            pm10 = current.get("pm10")
            us_aqi = current.get("us_aqi")
    except Exception as ex:
        logger.debug("Open-Meteo AQI query failed (%s): %s", aqi_url, ex)

    # Determine season
    m = datetime.now().month
    if m in (11, 12, 1, 2):
        season_name = "winter"
    elif m in (7, 8, 9, 10):
        season_name = "monsoon"
    else:
        season_name = "summer"

    # If both queries failed, return seasonal fallback
    if temp_c is None and us_aqi is None:
        fallback = _get_seasonal_fallback(m)
        fallback["latitude"] = c_lat
        fallback["longitude"] = c_lon
        _ATMOSPHERIC_CACHE[cache_key] = (now, fallback)
        return dict(fallback)

    # Merge partial or complete live response with defaults
    seasonal = _get_seasonal_fallback(m)
    resolved_temp = temp_c if temp_c is not None else seasonal["temperature_c"]
    resolved_hum = humidity if humidity is not None else seasonal["humidity_pct"]
    resolved_pm25 = pm2_5 if pm2_5 is not None else seasonal["pm2_5"]
    resolved_pm10 = pm10 if pm10 is not None else seasonal["pm10"]
    resolved_aqi = int(us_aqi) if us_aqi is not None else seasonal["aqi"]

    result = {
        "temperature_c": float(resolved_temp),
        "humidity_pct": float(resolved_hum),
        "pm2_5": float(resolved_pm25),
        "pm10": float(resolved_pm10),
        "aqi": resolved_aqi,
        "aqi_category": _get_aqi_category(resolved_aqi, "en"),
        "season": season_name,
        "latitude": c_lat,
        "longitude": c_lon,
        "source": "open-meteo" if (temp_c is not None or us_aqi is not None) else "seasonal_fallback",
        "is_fallback": (temp_c is None and us_aqi is None),
    }

    _ATMOSPHERIC_CACHE[cache_key] = (now, result)
    return dict(result)


def clear_atmospheric_cache() -> None:
    """Clears in-memory weather and AQI cache (useful for testing)."""
    _ATMOSPHERIC_CACHE.clear()


RESPIRATORY_KEYWORDS = (
    "respirat", "cough", "cold", "asthma", "throat", "breath", "breathing", "flu", "viral", "allergy", "allergies",
    "sardi", "khokla", "khansi", "jukam", "zukaam", "ghasa", "shwas", "saans",
    "खोकला", "दमा", "सर्दी", "घसा", "श्वास", "खांसी", "जुकाम", "सांस", "गले", "एलर्जी"
)
GASTRO_KEYWORDS = (
    "gastro", "stomach", "vomit", "diarrhea", "loose", "acidity",
    "पोट", "मळमळ", "उलटी", "जुलाब", "पित्त", "पेट", "उल्टी", "दस्त", "एसिडिटी"
)
FEVER_KEYWORDS = (
    "fever", "dengue", "malaria", "typhoid", "flu", "viral",
    "tap", "taap", "bukhar",
    "ताप", "हिवताप", "डेंग्यू", "मलेरिया", "बुखार"
)
HEAT_KEYWORDS = (
    "heat", "heatstroke", "heat_stroke", "dehydration", "sunstroke",
    "उष्माघात", "लू"
)
AIR_QUALITY_KEYWORDS = (
    "weather", "pollution", "aqi", "smog", "air quality", "air_quality", "dust",
    "हवामान", "प्रदूषण", "मौसम"
)
WEATHER_ADVISORY_KEYWORDS = (
    RESPIRATORY_KEYWORDS + FEVER_KEYWORDS + HEAT_KEYWORDS + AIR_QUALITY_KEYWORDS
    + ("breathing_problem", "sore_throat")
)
ENVIRONMENTAL_KEYWORDS = WEATHER_ADVISORY_KEYWORDS

_HIGH_SEVERITY_RED_FLAG_PATTERNS = [
    re.compile(r"\b(?:chest\s*pain|chestpain|crushing\s+chest|chhati\s*dukh|seene\s*mein\s*dard|छातीत\s*दुख|सीने\s*में\s*दर्द)\b", re.IGNORECASE),
    re.compile(r"\b(?:severe\s+breathing|difficulty\s+breathing|shortness\s+of\s+breath|can'?t\s+breathe|unable\s+to\s+breathe|shwas\s*ghyayla\s*tras|saans\s*lene\s*mein\s*takleef)\b", re.IGNORECASE),
    re.compile(r"(?:श्वास\s*(?:घेण्यास|घेता)|सांस\s*लेने\s*में\s*तकलीफ)", re.IGNORECASE),
    re.compile(r"\b(?:unconscious|unresponsive|passed\s+out|fainting|collapsed|seizure|convulsion|beshuddh|behosh|बेशुद्ध|बेहोश)\b", re.IGNORECASE),
    re.compile(r"\b(?:heavy|severe|uncontrolled|profuse)\s+bleeding\b|\bbleeding\s+(?:heavily|a\s+lot|won'?t\s+stop)\b|\b(?:vomit|cough)(?:ing)?\s+blood\b", re.IGNORECASE),
    re.compile(r"(?:रक्ताची\s*उलटी|खून\s*की\s*उल्टी|खूप\s*रक्त|बहुत\s*खून)", re.IGNORECASE),
    re.compile(r"\b(?:suicide|suicidal|self[-\s]?harm|kill\s+myself|want\s+to\s+die|mental\s+(?:distress|breakdown|crisis)|severe\s+(?:depression|panic|anxiety))\b", re.IGNORECASE),
    re.compile(r"(?:आत्महत्या|खुदकुशी|मरावेसे\s*वाटत)", re.IGNORECASE),
    # High persistent fever > 103 F or >= 39.5 C
    re.compile(r"\b(?:10[3-9]|110)\s*(?:°?\s*f|fahrenheit)?\b.*\b(?:fever|temp|temperature|tap|bukhar)\b|\b(?:fever|temp|temperature|tap|bukhar)\b.*\b(?:10[3-9]|110)\s*(?:°?\s*f|fahrenheit)?\b", re.IGNORECASE),
    re.compile(r"\b(?:39\.[5-9]|4[0-2](?:\.\d+)?)\s*(?:°?\s*c|celsius)\b.*\b(?:fever|temp|temperature|tap|bukhar)\b", re.IGNORECASE),
    re.compile(r"\b(?:persistent\s+high\s+fever|very\s+high\s+fever|fever\s*(?:>|above|over)\s*103)\b", re.IGNORECASE),
]


def _matches_advisory_keywords(text: str, keywords: Tuple[str, ...]) -> bool:
    """Matches environmental/viral keywords using word boundaries for Latin words and substring for Devanagari."""
    if not text:
        return False
    lower = str(text).lower().replace("_", " ")
    for kw in keywords:
        if any("\u0900" <= ch <= "\u097F" for ch in kw):
            if kw in lower:
                return True
        else:
            kw_clean = kw.replace("_", " ")
            if kw_clean == "respirat":
                if re.search(r"\brespirat", lower):
                    return True
            elif re.search(r"(?<![a-z0-9])" + re.escape(kw_clean) + r"(?![a-z0-9])", lower):
                return True
    return False


def is_high_severity_or_red_flag(text: str, safety_flag: Optional[str] = None) -> bool:
    """
    Returns True ONLY when high-severity / emergency / red-flag symptoms or
    mental health distress are detected (for statutory helpline routing).
    """
    if safety_flag in ("emergency", "crisis"):
        return True
    if not text or not isinstance(text, str):
        return False
    for pat in _HIGH_SEVERITY_RED_FLAG_PATTERNS:
        if pat.search(text):
            return True
    return False


def get_regional_health_alert(
    condition_category: Optional[str] = None,
    month: Optional[int] = None,
    weather_data: Optional[Dict[str, Any]] = None,
    language: str = "en"
) -> str:
    """
    Formulates a localized epidemiological surveillance advisory based on NCDC / IDSP
    guidelines, seasonal disease profiles, and live atmospheric conditions.
    """
    lang = (language or "en").strip().lower()
    m = month if month is not None else datetime.now().month
    weather = weather_data or _get_seasonal_fallback(m)

    temp = weather.get("temperature_c", 25.0)
    aqi = weather.get("aqi", 100)
    hum = weather.get("humidity_pct", 50.0)

    clean_category = str(condition_category or "").strip().lower()

    # Determine seasonal bracket
    # 1. Winter (Nov-Feb: 11, 12, 1, 2) -> ARI, Smog, Seasonal Influenza (H1N1)
    if m in (11, 12, 1, 2):
        if any(k in clean_category for k in RESPIRATORY_KEYWORDS):
            if lang in ("mr", "roman_mr"):
                return (
                    f"एनसीडीसी (NCDC) व आयडीएसपी मार्गदर्शक सूचना: हिवाळ्यातील तापमान घसरण ({temp:.1f}°C) "
                    f"आणि हवेतील वाढते धुलिकण (AQI {aqi}) यामुळे श्वसनविकार, दमा व तीव्र श्वसन संसर्गाचा (ARI/H1N1) धोका वाढतो. "
                    f"सकाळी धुक्यात फिरणे टाळा, बाहेर पडताना मास्क वापरा आणि कोमट पाणी प्या."
                )
            elif lang in ("hi", "hinglish"):
                return (
                    f"एनसीडीसी (NCDC) एवं आईडीएसपी स्वास्थ्य परामर्श: सर्दियों में तापमान में गिरावट ({temp:.1f}°C) "
                    f"और वायु प्रदूषण (AQI {aqi}) से सांस की नली में सूजन, दमा और मौसमी इन्फ्लूएंजा (H1N1) का खतरा बढ़ता है। "
                    f"सुबह की धुंध में मास्क लगाएं, गुनगुने पानी का सेवन करें और ठंड से सीने का बचाव करें।"
                )
            return (
                f"NCDC / IDSP Seasonal Advisory: Winter thermal inversion and particulate load (AQI {aqi}, {temp:.1f}°C) "
                f"significantly trigger Acute Respiratory Infections (ARI), asthma flares, and seasonal influenza (H1N1). "
                f"Wear a protective mask outdoors, stay hydrated with warm fluids, and avoid strenuous early morning activity."
            )
        else:
            if lang in ("mr", "roman_mr"):
                return (
                    f"एनसीडीसी/आयडीएसपी मोसमी विषाणू सूचना (हिवाळा): हवेतील कोरडेपणा व तापमानातील चढ-उतारामुळे ({temp:.1f}°C) व्हायरल ताप आणि अंगदुखीचे प्रमाण वाढते. "
                    f"शरीर उबदार ठेवा आणि पुरेशी विश्रांती घ्या."
                )
            elif lang in ("hi", "hinglish"):
                return (
                    f"एनसीडीसी/आईडीएसपी मौसमी स्वास्थ्य सूचना (शीतकालीन): तापमान में उतार-चढ़ाव ({temp:.1f}°C) से मौसमी फ्लू और बुखार तेजी से फैलता है। "
                    f"गर्म कपड़े पहनें और पर्याप्त आराम करें।"
                )
            return (
                f"NCDC / IDSP Winter Health Notice: Temperature drop ({temp:.1f}°C) promotes viral pathogen survival and seasonal fever spikes. "
                f"Ensure adequate hydration and warm resting conditions."
            )

    # 2. Monsoon / Post-Monsoon (Jul-Oct: 7, 8, 9, 10) -> Vector-Borne & Waterborne
    elif m in (7, 8, 9, 10):
        if any(k in clean_category for k in GASTRO_KEYWORDS):
            if lang in ("mr", "roman_mr"):
                return (
                    f"जलजन्य आजार सतर्कता (IDSP): पावसाळ्यातील दमट हवामानात ({hum:.0f}% आर्द्रता) गॅस्ट्रो, टायफॉईड व दूषित पाण्यामुळे होणारे पोटाचे संसर्ग वाढतात. "
                    f"फक्त उकळलेले पाणी प्या, ताजे अन्न खा आणि डिहायड्रेशन टाळण्यासाठी ओआरएस (ORS) चा वापर करा."
                )
            elif lang in ("hi", "hinglish"):
                return (
                    f"जल-जनित रोग चेतावनी (IDSP): मानसून के दौरान दूषित जल एवं उच्च आर्द्रता ({hum:.0f}%) से गैस्ट्रोएंटेराइटिस और टायफाइड का प्रकोप बढ़ता है। "
                    f"पानी उबालकर पिएं, बाहर के खुले खाने से बचें और निर्जलीकरण होने पर ओआरएस का घोल लें।"
                )
            return (
                f"IDSP Waterborne Disease Advisory: High monsoon humidity ({hum:.0f}%) elevates risk of acute gastroenteritis, foodborne illnesses, and typhoid. "
                f"Drink boiled/filtered water, practice hand hygiene, and use Oral Rehydration Salts (ORS) at the first sign of dehydration."
            )
        else:
            if lang in ("mr", "roman_mr"):
                return (
                    f"कीटकजन्य आजार दक्षता (NVBDCP/NCDC): पावसाळ्यात डासांची पैदास वाढल्यामुळे डेंग्यू, चिकुनगुनिया व हिवताप (मलेरिया) चा फैलाव होतो. "
                    f"घराभोवती पाणी साचू देऊ नका, डास प्रतिबंधक वापरा आणि थंडी वाजून तीव्र ताप आल्यास तात्काळ रक्ततपासणी करा."
                )
            elif lang in ("hi", "hinglish"):
                return (
                    f"वेक्टर-जनित रोग चेतावनी (NVBDCP/NCDC): बरसात में जलभराव के कारण डेंगू, चिकनगुनिया और मलेरिया के मामलों में वृद्धि होती है। "
                    f"मच्छरदानी का प्रयोग करें, गमलों या कूलरों में पानी जमा न होने दें और तेज बुखार आने पर तुरंत डॉक्टर से संपर्क करें।"
                )
            return (
                f"NVBDCP / NCDC Vector-Borne Alert: Stagnant water and high humidity ({hum:.0f}%) accelerate mosquito breeding (Dengue, Chikungunya, Malaria). "
                f"Eliminate open water stagnation, use mosquito nets/repellents, and seek diagnostic evaluation if high fever with body ache occurs."
            )

    # 3. Summer (Mar-Jun: 3, 4, 5, 6) -> Heat Stress, Dehydration, Heat Exhaustion
    else:
        if lang in ("mr", "roman_mr"):
            return (
                f"उष्माघात व आरोग्य सूचना (NDMA): उन्हाळ्यातील उच्च तापमान ({temp:.1f}°C) डिहायड्रेशन, उष्माघात (Heat Stroke) व थकव्याचा धोका वाढवते. "
                f"दिवसभरात भरपूर पाणी, ताक, शहाळ्याचे पाणी प्या. दुपारी १२ ते ४ उन्हात फिरणे टाळा व डोक्यावर रुमाल/टोपी वापरा."
            )
        elif lang in ("hi", "hinglish"):
            return (
                f"लू एवं उष्माघात परामर्श (NDMA): अत्यधिक तापमान ({temp:.1f}°C) से डिहाइड्रेशन, हीट क्रैम्प्स और लू लगने का जोखिम रहता है। "
                f"पर्याप्त मात्रा में पानी, नीबू-पानी या छाछ पिएं, दोपहर 12 से 4 के बीच तेज धूप से बचें और बाहर जाते समय सिर ढकें।"
            )
        return (
            f"NDMA Heat Stress Advisory: High ambient temperature ({temp:.1f}°C) increases vulnerability to heat exhaustion and dehydration. "
            f"Hydrate aggressively with electrolytes/fluids, minimize direct midday sun exposure (12 PM - 4 PM), and wear light cotton clothing."
        )


def get_citizen_advisory(condition_category: Optional[str] = None, language: str = "en") -> str:
    """
    Formulates statutory Indian citizen healthcare navigation advisories:
    - Helplines: 108 (Ambulance), 104 (Medical Advice), 112 (Universal), 14416 (Tele-MANAS).
    - Public Facilities: Primary Health Centres (PHC), Rural Hospitals, Civil Hospitals.
    - Scheme Awareness: Ayushman Bharat PM-JAY / Mahatma Jyotirao Phule Jan Arogya Yojana.
    """
    lang = (language or "en").strip().lower()

    if lang in ("mr", "roman_mr"):
        return (
            "🏛️ **सार्वजनिक आरोग्य व शासकीय मदत कक्ष:**\n"
            "• **रुग्णवाहिका / आपत्कालीन वैद्यकीय सेवा:** **१०८** (टोल-फ्री, २४x७)\n"
            "• **आरोग्य सल्ला व मार्गदर्शन हेल्पलाइन:** **१०४** डायल करा\n"
            "• **मानसिक स्वास्थ्य हेल्पलाइन (Tele-MANAS):** **१४४१६** वर संपर्क साधा\n"
            "• **शासकीय आरोग्य सुविधा:** आपल्या नजीकच्या प्राथमिक आरोग्य केंद्र (PHC), ग्रामीण रुग्णालय किंवा जिल्हा शासकीय रुग्णालयात विनामूल्य वैद्यकीय तपासणी व आवश्यक औषधे उपलब्ध असतात.\n"
            "• **आयुष्मान भारत / महात्मा जोतीराव फुले जन आरोग्य योजना:** गंभीर आजार व हॉस्पिटलायझेशनच्या प्रसंगी अधिकृत रुग्णालयांमध्ये ५ लाख रुपयांपर्यंत मोफत कॅशलेस उपचारांची सुविधा उपलब्ध आहे."
        )
    elif lang in ("hi", "hinglish"):
        return (
            "🏛️ **नागरिक स्वास्थ्य एवं सरकारी हेल्पलाइन:**\n"
            "• **आपातकालीन एम्बुलेंस सेवा:** **108** पर कॉल करें (निःशुल्क, 24x7)\n"
            "• **स्वास्थ्य परामर्श हेल्पलाइन:** चिकित्सकीय सलाह के लिए **104** डायल करें\n"
            "• **मानसिक स्वास्थ्य सहायता (Tele-MANAS):** **14416** पर संपर्क करें\n"
            "• **सरकारी स्वास्थ्य सेवाएं:** निकटतम प्राथमिक स्वास्थ्य केंद्र (PHC), सामुदायिक स्वास्थ्य केंद्र (CHC) या जिला अस्पताल में मुफ्त परामर्श और दवाएं उपलब्ध हैं।\n"
            "• **आयुष्मान भारत (PM-JAY):** अस्पताल में भर्ती की आवश्यकता होने पर पात्र परिवारों को ₹5 लाख तक का कैशलेस इलाज पैनलबद्ध अस्पतालों में मिलता है."
        )

    return (
        "🏛️ **Public Healthcare Navigation & Citizen Helplines:**\n"
        "• **Emergency Ambulance:** Dial **108** (Toll-Free, 24x7 across India)\n"
        "• **Health Advice & Tele-Consultation:** Dial **104** for state medical assistance\n"
        "• **Mental Health Support (Tele-MANAS):** Dial **14416** for free psychological support\n"
        "• **Public Health Centres:** Free clinical consultations and generic medicines are accessible at your nearest Primary Health Centre (PHC), Rural Hospital, or District Civil Hospital.\n"
        "• **Ayushman Bharat (PM-JAY):** For conditions requiring hospitalization, eligible families receive up to ₹5 Lakh cashless treatment at empaneled healthcare facilities."
    )


def format_atmospheric_advisory_block(
    symptoms: Any,
    lat: Optional[float] = None,
    lon: Optional[float] = None,
    city: Optional[str] = None,
    language: str = "en",
    include_helplines: bool = False
) -> Optional[str]:
    """
    Synthesizes atmospheric intelligence ONLY when the query involves viral/seasonal
    illnesses, fever, cold, cough, flu, allergies, asthma, heatstroke, or air quality.
    Citizen helplines (108, 104, 14416) are attached ONLY when include_helplines=True.
    """
    lang = (language or "en").strip().lower()

    # Convert symptoms to normalized string
    sym_list = []
    if isinstance(symptoms, list):
        sym_list = [str(s).lower().replace("_", " ").replace("-", " ") for s in symptoms]
    elif isinstance(symptoms, str):
        sym_list = [s.strip().lower().replace("_", " ") for s in symptoms.split(",")]

    sym_text = " ".join(sym_list)

    # Environmental triggers check across English, Marathi, Hindi, and Romanized scripts
    relevant = _matches_advisory_keywords(sym_text, WEATHER_ADVISORY_KEYWORDS)

    if not relevant:
        return None

    # Determine condition category
    if _matches_advisory_keywords(sym_text, RESPIRATORY_KEYWORDS):
        category = "respiratory"
    elif _matches_advisory_keywords(sym_text, FEVER_KEYWORDS):
        category = "fever"
    elif _matches_advisory_keywords(sym_text, HEAT_KEYWORDS):
        category = "heat"
    else:
        category = "general"

    # Fetch weather and generate alerts
    weather = get_atmospheric_context(lat=lat, lon=lon, city=city)
    epi_alert = get_regional_health_alert(category, weather_data=weather, language=lang)

    temp_val = weather.get("temperature_c", 25.0)
    aqi_val = weather.get("aqi", 100)
    aqi_cat = weather.get("aqi_category", "Moderate")

    if lang in ("mr", "roman_mr"):
        header = f"🌦️ **स्थानिक हवामान व आरोग्य सूचना (तापमान: {temp_val:.1f}°C | AQI: {aqi_val} - {aqi_cat}):**"
    elif lang in ("hi", "hinglish"):
        header = f"🌦️ **स्थानीय मौसम एवं स्वास्थ्य परामर्श (तापमान: {temp_val:.1f}°C | AQI: {aqi_val} - {aqi_cat}):**"
    else:
        header = f"🌦️ **Local Weather & Health Advisory (Temp: {temp_val:.1f}°C | AQI: {aqi_val} - {aqi_cat}):**"

    if include_helplines:
        citizen_adv = get_citizen_advisory(category, language=lang)
        return f"{header}\n{epi_alert}\n\n{citizen_adv}"

    return f"{header}\n{epi_alert}"
