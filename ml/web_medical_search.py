"""
MedAssist AI - Lightweight Live Web Medical Search Fallback
Uses DuckDuckGo Instant Answer API and Wikipedia REST Summary API (stdlib urllib, 2.0s timeout)
to retrieve concise factual medical definitions when primary LLMs are unavailable.
"""

import json
import logging
import re
import urllib.parse
import urllib.request
from typing import Optional

logger = logging.getLogger("medassist.web_search")

_HEADERS = {"User-Agent": "MedAssist-AI/1.0 (clinical-info-lookup)"}


def _fetch_json(url: str, timeout: float = 2.0) -> Optional[dict]:
    try:
        req = urllib.request.Request(url, headers=_HEADERS)
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read().decode("utf-8", errors="replace")
            return json.loads(raw)
    except Exception as ex:
        logger.debug("Web medical search request failed (%s): %s", url, ex)
        return None


def search_medical_web(query: str, language: str = "en", timeout: float = 2.0) -> Optional[str]:
    """
    Attempts a fast DuckDuckGo Instant Answer or Wikipedia summary lookup
    for a concise medical/health query. Returns clean text or None.
    """
    if not query or not isinstance(query, str):
        return None
    cleaned = re.sub(r"\s+", " ", query.strip())
    if len(cleaned) < 3:
        return None

    encoded = urllib.parse.quote_plus(cleaned)
    # 1. DuckDuckGo Instant Answer API
    ddg = _fetch_json(f"https://api.duckduckgo.com/?q={encoded}&format=json&no_html=1&skip_disambig=1", timeout=timeout)
    if isinstance(ddg, dict):
        abstract = (ddg.get("AbstractText") or "").strip()
        if abstract and len(abstract) > 25:
            return abstract

    # 2. Wikipedia REST Summary API (clean factual fallback for medical terms)
    wiki_slug = urllib.parse.quote(cleaned.replace(" ", "_"))
    wiki = _fetch_json(f"https://en.wikipedia.org/api/rest_v1/page/summary/{wiki_slug}", timeout=timeout)
    if isinstance(wiki, dict) and wiki.get("type") == "standard":
        extract = (wiki.get("extract") or "").strip()
        if extract and len(extract) > 25:
            return extract

    return None
