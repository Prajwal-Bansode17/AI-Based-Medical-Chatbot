"""
MedAssist AI - Lightweight Live Web Medical Search Fallback
Uses DuckDuckGo Instant Answer API, Wikipedia REST Summary API, and localized clinical formatting
(stdlib urllib, 2.0s timeout) to retrieve factual medical definitions and guidance without API keys.
"""

import json
import logging
import re
import urllib.parse
import urllib.request
from typing import Optional

logger = logging.getLogger("medassist.web_search")

_HEADERS = {"User-Agent": "MedAssist-AI/1.0 (clinical-info-lookup)"}

# Clinical translation map for Roman Marathi / Hinglish terms to high-precision search terms
_VERNACULAR_TO_CLINICAL = [
    (r"\b(koshmat|koshmat\s+pani|garam\s+pani)\b", "drinking warm water health benefits"),
    (r"\b(vaf|vaaf|bhaf|steam)\b", "steam inhalation for cold and congestion procedure"),
    (r"\b(tap|taap|bukhar|fever)\b", "fever symptoms causes and home care"),
    (r"\b(sardi|zukam|cold)\b", "common cold symptoms relief and care"),
    (r"\b(khokla|khansi|cough)\b", "cough causes relief and treatment"),
    (r"\b(angdukhi|badan\s+dard|body\s+pain)\b", "body ache fatigue causes and home care"),
    (r"\b(dokedukhi|sar\s+dard|headache)\b", "headache types causes and relief"),
    (r"\b(pot\s+dukhi|pet\s+dard|stomach\s+pain)\b", "stomach ache causes and relief"),
    (r"\b(acidity|pitta|heartburn)\b", "acidity heartburn symptoms and relief"),
    (r"\b(kamzori|ashaktpana|weakness)\b", "weakness and fatigue management"),
]

# Quick medical advice templates for vernacular guidance when web definitions are retrieved
_VERNACULAR_ADVICE = {
    "warm_water": {
        "mr": "कोमट पाणी (Lukewarm Water) पिण्यामुळे घशाची खवखव कमी होते, पचनक्रिया सुधारते आणि शरीर हायड्रेटेड राहण्यास मदत होते. दिवसभरात २-३ लिटर कोमट पाणी पिणे फायदेशीर ठरते.",
        "hi": "हल्का गर्म (गुनगुना) पानी पीने से गले की खराश कम होती है, पाचन में सुधार होता है और शरीर हाइड्रेटेड रहता है.",
        "en": "Drinking lukewarm water helps soothe throat irritation, aids digestion, and keeps the body well hydrated.",
    },
    "steam": {
        "mr": "वाफ (Steam Inhalation) घेतल्याने बंद नाक मोकळे होते आणि छातीतील कफ पातळ होण्यास मदत होते. एका भांड्यात गरम पाणी घेऊन टॉवेल पांघरून ५ ते १० मिनिटे सावकाश श्वास घ्यावा.",
        "hi": "भाप (Steam Inhalation) लेने से बंद नाक खुलती है और बलगम आसानी से निकलता है. 5 से 10 मिनट तक भाप लेना लाभकारी होता है.",
        "en": "Steam inhalation helps relieve nasal congestion and soothes the airway. Inhale steam safely for 5-10 minutes.",
    },
    "fever_cold": {
        "mr": "ताप आणि सर्दीसाठी भरपूर विश्रांती घ्या, कोमट पाणी प्या, वाफ घ्या आणि हलका सकस आहार घ्या. ताप १०२°F पेक्षा जास्त असल्यास किंवा ३ दिवसांपक्षा जास्त राहिल्यास डॉक्टरांचा सल्ला घ्या.",
        "hi": "बुखार और सर्दी में पर्याप्त आराम करें, गुनगुना पानी पिएं और भाप लें. यदि बुखार 3 दिन से अधिक रहे तो डॉक्टर से संपर्क करें.",
        "en": "For fever and cold, get adequate rest, stay hydrated with warm fluids, and take steam. Consult a doctor if fever persists over 3 days.",
    },
}


def _fetch_json(url: str, timeout: float = 2.0) -> Optional[dict]:
    try:
        req = urllib.request.Request(url, headers=_HEADERS)
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read().decode("utf-8", errors="replace")
            return json.loads(raw)
    except Exception as ex:
        logger.debug("Web medical search request failed (%s): %s", url, ex)
        return None


def _map_query_to_clinical_search(query: str) -> str:
    q_low = query.lower()
    for pattern, replacement in _VERNACULAR_TO_CLINICAL:
        if re.search(pattern, q_low):
            return replacement
    # Clean filler Marathi/Hindi words (mhanje kay, kasa gheu, kya hota hai, kaise kare)
    cleaned = re.sub(
        r"\b(mhanje|kay|kasa|gheu|kashi|karu|aahe|ahe|kya|hai|hota|kare|kaise|madat|kar|na|please)\b",
        "",
        q_low,
        flags=re.IGNORECASE,
    )
    cleaned = re.sub(r"\s+", " ", cleaned).strip()
    return cleaned if len(cleaned) >= 3 else query


def search_medical_web(query: str, language: str = "en", timeout: float = 2.0) -> Optional[str]:
    """
    Attempts a fast DuckDuckGo Instant Answer or Wikipedia summary lookup
    for a concise medical/health query. Returns clean text or localized guidance.
    """
    if not query or not isinstance(query, str):
        return None
    cleaned = re.sub(r"\s+", " ", query.strip())
    if len(cleaned) < 3:
        return None

    # Check for direct vernacular clinical query match (e.g., steam, warm water, cold/fever)
    q_low = cleaned.lower()
    lang_key = (
        "mr"
        if language in ("mr", "marathi") or any(w in q_low for w in ["mhanje", "kay", "kasa", "pani", "gheu", "ahe", "tap", "sardi"])
        else ("hi" if language in ("hi", "hindi") else "en")
    )

    if any(k in q_low for k in ["koshmat", "garam pani", "warm water", "lukewarm"]):
        return _VERNACULAR_ADVICE["warm_water"].get(lang_key, _VERNACULAR_ADVICE["warm_water"]["en"])
    if any(k in q_low for k in ["vaf", "vaaf", "bhaf", "steam"]):
        return _VERNACULAR_ADVICE["steam"].get(lang_key, _VERNACULAR_ADVICE["steam"]["en"])
    if any(k in q_low for k in ["tap ani sardi", "taap ani sardi", "bukhar aur sardi", "fever and cold"]):
        return _VERNACULAR_ADVICE["fever_cold"].get(lang_key, _VERNACULAR_ADVICE["fever_cold"]["en"])

    search_target = _map_query_to_clinical_search(cleaned)
    encoded = urllib.parse.quote_plus(search_target)

    # 1. DuckDuckGo Instant Answer API
    ddg = _fetch_json(f"https://api.duckduckgo.com/?q={encoded}&format=json&no_html=1&skip_disambig=1", timeout=timeout)
    if isinstance(ddg, dict):
        abstract = (ddg.get("AbstractText") or "").strip()
        if abstract and len(abstract) > 25:
            return abstract

    # 2. Wikipedia REST Summary API (clean factual fallback for medical terms)
    wiki_slug = urllib.parse.quote(search_target.replace(" ", "_"))
    wiki = _fetch_json(f"https://en.wikipedia.org/api/rest_v1/page/summary/{wiki_slug}", timeout=timeout)
    if isinstance(wiki, dict) and wiki.get("type") == "standard":
        extract = (wiki.get("extract") or "").strip()
        if extract and len(extract) > 25:
            return extract

    return None
