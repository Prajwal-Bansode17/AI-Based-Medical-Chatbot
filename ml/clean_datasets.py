"""Permanent clinical dataset sanitization script for MedAssist AI.

Removes non-clinical hospital portal logs, faculty rosters, and campus admin
records from fine-tuning JSONL splits and rebuilds MEDIQ retrieval CSVs from
verified clinical datasets.
"""

import json
import re
import sys
from pathlib import Path
import pandas as pd

BASE_ML_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(BASE_ML_DIR))

from lab_report_analyzer import BIOMARKER_DATABASE

# Strict removal patterns for faculty rosters, campus admin, KGMU, hospital ads, etc.
NOISE_PATTERNS = [
    r"\bkgmu\b",
    r"king\s*george",
    r"\bvice\s*dean\b",
    r"\badditional\s+professor\b",
    r"\bassociate\s+professor\b",
    r"\bassistant\s+professor\b",
    r"\bhead\s+of\s+(?:the\s+)?department\b",
    r"\bfaculty\s+(?:members?|directory|list|roster|schedule)\b",
    r"\bon\s+lien\b",
    r"\b(?:doctors?|professors?|staff)\s+(?:directory|list|roster|schedule)\b",
    r"\blist\s+of\s+(?:doctors|professors|faculty|specialists|surgeons|physicians)\b",
    r"\bdepartment\s+of\s+[a-z\s,&]+\s+(?:doctors?\s+list|faculty|professors?)\b",
    r"\bopd\s+(?:doctor\s+schedule|timings?|counter|card|registration)\b",
    r"\b(?:vice[-\s]?)?chancellor\b",
    r"\bconvocation\b",
    r"\bfoundation\s+day\b",
    r"\badmission\s+(?:opening|form|process|fee)\b",
    r"\badmit\s+card\b",
    r"\bexamination\s+cell\b",
    r"\bdigital\s+lectures\b",
    r"\bvirtual\s+classroom\b",
    r"\b(?:ward|room)\s*(?:no\.?|number)\s*\d+\b",
    r"\binternship\b",
    r"\bfellowship\b",
    r"\bmbbs\s+admission\b",
    r"\bin\s+the\s+faculty\s+directory\b",
    r"\blucknow\b",
    r"\buniversity\s+administration\b",
    r"\bvasantotsav\b",
    r"\bcme[-\s]?workshop\b",
    r"0522-",
    r"ors\.gov\.in",
    r"medassist\.ai",
    r"\bhostel\s+(?:fee|allotment|room)\b",
    r"\bnotice\s+board\b",
    r"\banti[-\s]?ragging\b",
    r"\bunited\s+hospital\b",
]
NOISE_RE = re.compile(r"(?i)(?:" + "|".join(NOISE_PATTERNS) + r")")

PURE_CLINICAL_CATEGORIES = {
    "symptom_guidance",
    "general_health",
    "term_explanation",
    "emergency_triage",
    "medication_guidance",
    "chronic_care",
    "pediatric_guidance",
    "womens_health",
    "disease_information",
}


def has_noise(text: str) -> bool:
    """Return True if text matches any non-clinical hospital/faculty pattern."""
    return bool(NOISE_RE.search(text or ""))


def clean_finetune_file(path: Path) -> tuple[int, int, int]:
    """Filter non-clinical records from a JSONL split and write back cleaned records."""
    with open(path, "r", encoding="utf-8") as f:
        records = [json.loads(line) for line in f if line.strip()]

    total = len(records)
    cleaned = []
    for obj in records:
        cat = str(obj.get("category") or "").strip()
        lang = str(obj.get("language") or "").strip().lower()
        instr = str(obj.get("instruction") or "")
        out = str(obj.get("output") or "")
        combined = f"{instr}\n{out}"

        # Exclude noisy hospital portal patient_dialogue (all en/hi logs from KGMU portal)
        if cat == "patient_dialogue" and lang != "mr":
            continue

        # Strictly exclude any record matching noise patterns
        if has_noise(combined):
            continue

        # Accept curated clinical categories + clean Marathi clinical medication dialogue
        if cat in PURE_CLINICAL_CATEGORIES or (cat == "patient_dialogue" and lang == "mr"):
            cleaned.append(obj)

    with open(path, "w", encoding="utf-8", newline="\n") as f:
        for obj in cleaned:
            f.write(json.dumps(obj, ensure_ascii=False) + "\n")

    removed = total - len(cleaned)
    return total, removed, len(cleaned)


def make_mediq_row(idx: int, question: str, answer: str, category: str, intent: str = "information_seeking", relevance: str = "high") -> dict:
    """Construct a single clean MEDIQ-schema row preserving the 37-column layout."""
    return {
        "id": f"clean_clinical_{idx:05d}",
        "date": "",
        "question": question.strip(),
        "answer": answer.strip(),
        "timestamp": "2026-01-01T00:00:00.000Z",
        "consent": True,
        "consentTimestamp": "2026-01-01T00:00:00.000Z",
        "language_detected": "en",
        "code_mix_ratio": 0,
        "category": category,
        "question_type": "clinical_guidance",
        "response_type": "direct_answer",
        "department_mentioned": "",
        "query_complexity": "moderate",
        "bot_answered": True,
        "session_id": "clinical_kb",
        "is_followup": False,
        "question_length_chars": len(question.strip()),
        "question_length_words": len(question.strip().split()),
        "answer_length_chars": len(answer.strip()),
        "answer_length_words": len(answer.strip().split()),
        "hour_utc": 12,
        "hour_ist": 17,
        "day_of_week": "Monday",
        "is_weekend": False,
        "answer_has_links": False,
        "answer_has_list": False,
        "ai_language": "en",
        "ai_language_confidence": 1.0,
        "ai_intent": intent,
        "ai_medical_relevance": relevance,
        "ai_sentiment": "neutral",
        "ai_question_translated_en": question.strip(),
        "ai_answer_quality": "comprehensive",
        "ai_topic_tags": category,
        "ai_requires_followup": False,
        "ai_user_type_guess": "patient",
    }


def clean_and_rebuild_mediq(train_jsonl_path: Path):
    """Purge portal logs from mediq_full.csv and rebuild from pure clinical sources."""
    mediq_path = BASE_ML_DIR / "data" / "raw" / "mediq" / "ProxyAyush-MEDIQ-00091b1" / "data" / "mediq_full.csv"
    orig_df = pd.read_csv(mediq_path)
    orig_count = len(orig_df)
    columns = list(orig_df.columns)

    # All 17,598 original rows are KGMU hospital admin logs (OPD/admissions/exams/faculty/departments)
    # Build pure clinical MEDIQ retrieval entries from verified local clinical sources
    rows = []
    seen_q = set()
    idx = 1

    def add_row(q: str, a: str, cat: str, intent: str = "information_seeking", rel: str = "high"):
        nonlocal idx
        q_clean = " ".join(str(q).split()).strip()
        a_clean = str(a).strip()
        if not q_clean or not a_clean:
            return
        if has_noise(f"{q_clean} {a_clean}"):
            return
        key = q_clean.lower()
        if key in seen_q:
            return
        seen_q.add(key)
        rows.append(make_mediq_row(idx, q_clean, a_clean, cat, intent=intent, relevance=rel))
        idx += 1

    # 1. Medical Knowledge CSV & sub-CSVs
    kb_files = [BASE_ML_DIR / "data" / "raw" / "medical_knowledge.csv"] + sorted(
        (BASE_ML_DIR / "data" / "raw" / "medical_knowledge").glob("*.csv")
    )
    for kf in kb_files:
        if not kf.exists():
            continue
        kdf = pd.read_csv(kf)
        for _, r in kdf.iterrows():
            sym = str(r.get("symptom") or "").replace("_", " ").strip()
            qtype = str(r.get("question_type") or "information").strip()
            resp = str(r.get("response") or "").strip()
            aliases = [a.strip() for a in str(r.get("aliases") or "").split("|") if a.strip()]
            if qtype == "assistance":
                add_row(f"What should I do if I have {sym}?", resp, "symptom_guidance")
            else:
                add_row(f"What are the symptoms and causes of {sym}?", resp, "symptom_information")
            for al in aliases[:2]:
                add_row(f"Information about {al}", resp, "symptom_information")

    # 2. Clean English clinical records from train.jsonl
    with open(train_jsonl_path, "r", encoding="utf-8") as f:
        for line in f:
            if not line.strip():
                continue
            obj = json.loads(line)
            if str(obj.get("language") or "").lower() == "en":
                add_row(
                    obj.get("instruction", ""),
                    obj.get("output", ""),
                    obj.get("category", "general_health"),
                )

    # 3. Clinical Lab Biomarker reference entries
    for key, info in BIOMARKER_DATABASE.items():
        name = info.get("canonical_name", key)
        ref_str = info.get("ref_str", "")
        what = info.get("what_it_is", {}).get("en", "")
        low_exp = info.get("explanations", {}).get("low", {}).get("en", "")
        high_exp = info.get("explanations", {}).get("high", {}).get("en", "")
        diet = info.get("dietary_advice", {}).get("en", "")
        ans = (
            f"{what} Standard Reference Range: {ref_str}.\n"
            f"- Low level: {low_exp}\n"
            f"- High level: {high_exp}\n"
            f"- Dietary & Lifestyle guidance: {diet}\n\n"
            f"Disclaimer: Consult a qualified physician for formal diagnosis and treatment."
        )
        add_row(f"What is {name} and its normal lab test range?", ans, "lab_biomarkers")

    # 4. Official Indian Public Health Helplines & Jan Aushadhi Guidance
    public_health_records = [
        (
            "What is the emergency ambulance number in India?",
            "In India, dial **108** (Emergency Medical Ambulance Service) or **112** (National Emergency Number) immediately for critical medical emergencies such as chest pain, stroke symptoms, severe trauma, breathing distress, or unconsciousness.",
            "emergency_triage",
        ),
        (
            "What is the 104 health helpline in India?",
            "The **104 Health Helpline** is an official state government toll-free medical helpline across India providing free clinical health advice, symptom triage guidance, maternal/child health support, and public health grievance assistance.",
            "general_health",
        ),
        (
            "What is the 14416 mental health helpline in India?",
            "Tele-MANAS (**14416** or **1800-891-4416**) is the Government of India's free 24/7 toll-free tele-mental health helpline offering confidential psychological counseling and psychiatric support in multiple regional languages.",
            "general_health",
        ),
        (
            "What is Ayushman Bharat PM-JAY scheme?",
            "Ayushman Bharat Pradhan Mantri Jan Arogya Yojana (**AB PM-JAY**) is India's national public health insurance scheme providing up to ₹5 lakh per family per year for secondary and tertiary hospitalization care at empaneled public and private hospitals. Dial **14555** or check eligibility via official PM-JAY portals.",
            "general_health",
        ),
        (
            "What are Jan Aushadhi generic medicines?",
            "Pradhan Mantri Bhartiya Janaushadhi Pariyojana (**PMBJP / Jan Aushadhi**) Kendras provide quality-tested generic medicines at significantly lower prices (50% to 90% cheaper) than branded equivalents with identical active pharmaceutical ingredients and therapeutic efficacy.",
            "medication_guidance",
        ),
    ]
    for q, a, cat in public_health_records:
        add_row(q, a, cat)

    clean_df = pd.DataFrame(rows, columns=columns)
    clean_df.to_csv(mediq_path, index=False, encoding="utf-8")
    return orig_count, orig_count, len(clean_df)


def audit_csv(path: Path) -> tuple[int, int, int]:
    """Audit a CSV file and strip any rows matching NOISE_RE."""
    df = pd.read_csv(path)
    orig = len(df)
    keep_mask = []
    for _, row in df.iterrows():
        txt = " ".join(str(v) for v in row.values)
        keep_mask.append(not has_noise(txt))
    clean_df = df[keep_mask].copy()
    removed = orig - len(clean_df)
    if removed > 0:
        clean_df.to_csv(path, index=False, encoding="utf-8")
    return orig, removed, len(clean_df)


def main():
    """Run the full dataset cleanup and verification report across ml/data."""
    print("=" * 72)
    print("MEDASSIST AI - PERMANENT DATASET CLEANUP SUMMARY")
    print("=" * 72)

    train_jsonl = BASE_ML_DIR / "data" / "finetune" / "train.jsonl"
    val_jsonl = BASE_ML_DIR / "data" / "finetune" / "val.jsonl"

    t_orig, t_rem, t_clean = clean_finetune_file(train_jsonl)
    v_orig, v_rem, v_clean = clean_finetune_file(val_jsonl)
    print(f"train.jsonl         : original={t_orig:5d} | removed={t_rem:5d} | clean_remaining={t_clean:5d}")
    print(f"val.jsonl           : original={v_orig:5d} | removed={v_rem:5d} | clean_remaining={v_clean:5d}")

    m_orig, m_rem, m_clean = clean_and_rebuild_mediq(train_jsonl)
    print(f"mediq_full.csv      : original={m_orig:5d} | purged={m_rem:5d} | clean_rebuilt={m_clean:5d}")

    # Re-run preprocess.py logic to rebuild processed/train.csv and processed/test.csv from clean mediq_full.csv
    import preprocess
    mediq_clean_df = preprocess.load_dataset()
    prep_df = preprocess.prepare_dataset(mediq_clean_df)
    proc_train_orig = len(pd.read_csv(BASE_ML_DIR / "data" / "processed" / "train.csv"))
    proc_test_orig = len(pd.read_csv(BASE_ML_DIR / "data" / "processed" / "test.csv"))
    proc_train_df, proc_test_df = preprocess.create_splits(prep_df)
    preprocess.PROCESSED_DIR.mkdir(parents=True, exist_ok=True)
    proc_train_df.to_csv(preprocess.TRAIN_FILE, index=False, encoding="utf-8")
    proc_test_df.to_csv(preprocess.TEST_FILE, index=False, encoding="utf-8")
    print(f"processed/train.csv : original={proc_train_orig:5d} | purged={proc_train_orig:5d} | clean_rebuilt={len(proc_train_df):5d}")
    print(f"processed/test.csv  : original={proc_test_orig:5d} | purged={proc_test_orig:5d} | clean_rebuilt={len(proc_test_df):5d}")

    # Audit remaining CSVs
    audit_targets = [
        BASE_ML_DIR / "data" / "raw" / "medical_knowledge.csv",
        BASE_ML_DIR / "data" / "raw" / "medical_intents.csv",
        BASE_ML_DIR / "data" / "raw" / "medical_knowledge" / "diseases.csv",
        BASE_ML_DIR / "data" / "raw" / "medical_knowledge" / "general_health.csv",
        BASE_ML_DIR / "data" / "raw" / "medical_knowledge" / "medicines.csv",
        BASE_ML_DIR / "data" / "raw" / "medical_knowledge" / "symptoms.csv",
        BASE_ML_DIR / "data" / "processed" / "intent_split" / "train.csv",
        BASE_ML_DIR / "data" / "processed" / "intent_split" / "test.csv",
        BASE_ML_DIR / "data" / "processed" / "intent_split" / "validation.csv",
    ]
    for p in audit_targets:
        if p.exists():
            o, r, c = audit_csv(p)
            rel_name = str(p.relative_to(BASE_ML_DIR))
            print(f"{rel_name:40s} : original={o:5d} | removed={r:5d} | clean={c:5d}")

    print("=" * 72)


if __name__ == "__main__":
    main()
