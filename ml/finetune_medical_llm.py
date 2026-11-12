"""
MedAssist-Custom-AI: Instruction-Tuning Dataset Builder & LoRA Fine-Tuning Pipeline
Target base model: Qwen/Qwen2.5-0.5B-Instruct -> quantized 4-bit GGUF (~350-450 MB).

Usage:
  # 1. Build clean JSONL dataset only (CPU-safe, zero GPU required):
  python ml/finetune_medical_llm.py --prepare-only

  # 2. Run LoRA fine-tuning + GGUF export (requires transformers, peft, trl, torch):
  python ml/finetune_medical_llm.py --train --export-gguf
"""

import argparse
import csv
import json
import logging
import os
import subprocess
import sys
from pathlib import Path
from typing import Dict, List

BASE_DIR = Path(__file__).resolve().parent
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

from conversation_engine import is_faculty_or_staff_roster

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("medassist.finetune")

TRAIN_JSONL = BASE_DIR / "data" / "finetune" / "train.jsonl"
RAW_KB_FILE = BASE_DIR / "data" / "raw" / "medical_knowledge.csv"
RAW_KB_DIR = BASE_DIR / "data" / "raw" / "medical_knowledge"
OUTPUT_DATASET = BASE_DIR / "data" / "finetune" / "custom_instruct_train.jsonl"
MODELS_DIR = BASE_DIR / "models"
LORA_OUT_DIR = MODELS_DIR / "medassist_qwen_lora"
MERGED_OUT_DIR = MODELS_DIR / "medassist_qwen_merged"
GGUF_OUT_FILE = MODELS_DIR / "medassist_custom.gguf"

DEFAULT_BASE_MODEL = os.getenv("CUSTOM_BASE_MODEL", "Qwen/Qwen2.5-0.5B-Instruct")

CURATED_MULTILINGUAL_QA: List[Dict[str, str]] = [
    {
        "instruction": "mala khup tap ani sardi ahe kay karu",
        "output": (
            "Tumhala **tap (fever)** ani **sardi** aslyamule he samanya **viral infection** kiva seasonal flu asnyachi shakyaata aahe.\n\n"
            "**Aata Kay Karal:**\n"
            "- **Koshmat Garam Pani & Fluids:** Koshmat pani, soup kiva ORS pyat raha.\n"
            "- **Vaf (Steam):** Divsatun 2 vela sadhya panyachi vaf ghetlyas naak mokale hote.\n"
            "- **Vishranti:** Pureshi jhop ghya ani doctoranchya sallyashivay antibiotics gheu naka.\n\n"
            "**Chhota prashna:** Ha tap ani sardi kiti divsanpasun aahe?"
        ),
        "language": "roman_mr",
    },
    {
        "instruction": "koshmat garam pani mhanje kay?",
        "output": (
            "**Koshmat garam pani** mhanje **lukewarm water** — je khup ukalalele/garam nahi ani thandahi nahi (sadharantah 37°C–42°C, haatala sukhad vatnare).\n\n"
            "- **Fayde:** Ghasyachi khavkhav kami karte, pachan sudharte ani tap/sardit hydration thevte.\n"
            "- **Kase Pyave:** Sadhe pani ukalun thode thand hou dya ani ghot-ghot pya."
        ),
        "language": "roman_mr",
    },
    {
        "instruction": "vaf(steam) kasa gheu?",
        "output": (
            "**Vaf (Steam Inhalation)** ghenyachi sopi ani surakshit paddhat:\n\n"
            "1. **Pani Garam Kara:** Eka bhandyat sadhe pani ukalun table-var surakshit theva (chalu gas-var nahi).\n"
            "2. **Antar Theva:** Chehra panyapasun **8–12 inch** dur theva ani dokyavar towel ghya.\n"
            "3. **Shwas Ghya:** 5–10 minite naakane haluvar vaf odhun tondane soda.\n"
            "4. **Kalji:** Atishay javal jau naka (bhajanyacha dhoka talnyasathi); lahan mulansathi vishesh kalji ghya."
        ),
        "language": "roman_mr",
    },
    {
        "instruction": "How do I take steam inhalation safely for cold?",
        "output": (
            "**Safe Steam Inhalation Steps:**\n\n"
            "- **Prepare:** Pour hot (not actively boiling) plain water into a sturdy bowl on a flat table.\n"
            "- **Position:** Keep your face **8–12 inches** above the water and drape a towel loosely over your head.\n"
            "- **Duration:** Breathe slowly through your nose for **5–10 minutes** (once or twice daily).\n"
            "- **Safety:** Keep eyes closed and avoid leaning too close to prevent burns."
        ),
        "language": "en",
    },
    {
        "instruction": "bukhar aur sardi mein kya karein?",
        "output": (
            "**Bukhar aur sardi** aam taur par **viral infection** ya mausam badalne ki wajah se hoti hai.\n\n"
            "**Abhi Kya Karein:**\n"
            "- **Gunguna Pani & ORS:** Thode-thode waqt par gunguna pani ya soup piyein.\n"
            "- **Bhaap (Steam):** Din mein 2 baar saade pani ki bhaap lein.\n"
            "- **Aaram & Paracetamol:** Poora aaram karein; doctor ki salah se Paracetamol li ja sakti hai.\n\n"
            "**Chhota sawaal:** Bukhar kitne dino se hai aur kya gale mein dard bhi hai?"
        ),
        "language": "hinglish",
    },
]


def build_instruction_dataset(output_path: Path = OUTPUT_DATASET) -> int:
    """
    Aggregates verified clinical records from train.jsonl, medical_knowledge CSVs,
    and curated Roman Marathi / Hinglish / English Q&A into a clean instruction-tuning JSONL.
    """
    records: List[Dict[str, str]] = []
    seen_prompts = set()

    def _add_record(instr: str, out: str, lang: str = "en") -> None:
        clean_i = " ".join((instr or "").split()).strip()
        clean_o = (out or "").strip()
        if not clean_i or not clean_o:
            return
        if is_faculty_or_staff_roster(clean_o, clean_i):
            return
        key = clean_i.lower()
        if key in seen_prompts:
            return
        seen_prompts.add(key)
        records.append({
            "instruction": clean_i,
            "output": clean_o,
            "language": lang,
        })

    # 1. Curated multilingual clinical Q&A
    for item in CURATED_MULTILINGUAL_QA:
        _add_record(item["instruction"], item["output"], item.get("language", "en"))

    # 2. Cleaned finetune/train.jsonl
    if TRAIN_JSONL.exists():
        with TRAIN_JSONL.open("r", encoding="utf-8", errors="replace") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                try:
                    obj = json.loads(line)
                    _add_record(
                        obj.get("instruction") or obj.get("prompt") or "",
                        obj.get("output") or obj.get("response") or "",
                        obj.get("language") or "en",
                    )
                except json.JSONDecodeError:
                    continue

    # 3. Medical Knowledge CSVs
    csv_files = []
    if RAW_KB_FILE.exists():
        csv_files.append(RAW_KB_FILE)
    if RAW_KB_DIR.exists():
        csv_files.extend(sorted(RAW_KB_DIR.glob("*.csv")))

    for csv_path in csv_files:
        try:
            with csv_path.open("r", encoding="utf-8", errors="replace") as f:
                reader = csv.DictReader(f)
                for row in reader:
                    topic = (row.get("symptom") or row.get("topic") or row.get("title") or "").strip()
                    q_type = (row.get("question_type") or "information").strip()
                    resp = (row.get("response") or row.get("answer") or "").strip()
                    if topic and resp:
                        _add_record(f"Clinical {q_type} for {topic.replace('_', ' ')}", resp, "en")
        except Exception as ex:
            logger.warning("Skipping %s: %s", csv_path, ex)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8") as f:
        for rec in records:
            f.write(json.dumps(rec, ensure_ascii=False) + "\n")

    logger.info("Built clean custom instruction dataset: %d records -> %s", len(records), output_path)
    return len(records)


def run_lora_finetuning(
    dataset_path: Path = OUTPUT_DATASET,
    base_model: str = DEFAULT_BASE_MODEL,
    lora_out: Path = LORA_OUT_DIR,
    merged_out: Path = MERGED_OUT_DIR,
    epochs: int = 2,
) -> bool:
    """
    Runs LoRA fine-tuning of Qwen/Qwen2.5-0.5B-Instruct using transformers + peft + trl.
    Requires GPU/CPU training dependencies (torch, transformers, peft, datasets, trl).
    """
    try:
        import torch
        from datasets import load_dataset
        from peft import LoraConfig, get_peft_model
        from transformers import AutoModelForCausalLM, AutoTokenizer, TrainingArguments
        from trl import SFTTrainer
    except ImportError as ex:
        logger.error("Training dependencies missing (%s). Install torch, transformers, peft, datasets, trl to train.", ex)
        return False

    if not dataset_path.exists():
        build_instruction_dataset(dataset_path)

    tokenizer = AutoTokenizer.from_pretrained(base_model, trust_remote_code=True)
    if tokenizer.pad_token is None:
        tokenizer.pad_token = tokenizer.eos_token

    dtype = torch.float16 if torch.cuda.is_available() else torch.float32
    model = AutoModelForCausalLM.from_pretrained(base_model, torch_dtype=dtype, device_map="auto")

    lora_cfg = LoraConfig(
        r=16,
        lora_alpha=32,
        target_modules=["q_proj", "k_proj", "v_proj", "o_proj"],
        lora_dropout=0.05,
        bias="none",
        task_type="CAUSAL_LM",
    )
    model = get_peft_model(model, lora_cfg)

    ds = load_dataset("json", data_files=str(dataset_path), split="train")

    def _format_row(example):
        return {
            "text": (
                f"<|im_start|>system\nYou are MedAssist-Custom-AI, a concise multilingual clinical assistant.<|im_end|>\n"
                f"<|im_start|>user\n{example['instruction']}<|im_end|>\n"
                f"<|im_start|>assistant\n{example['output']}<|im_end|>"
            )
        }

    ds = ds.map(_format_row)
    lora_out.mkdir(parents=True, exist_ok=True)
    args = TrainingArguments(
        output_dir=str(lora_out),
        per_device_train_batch_size=4,
        gradient_accumulation_steps=4,
        num_train_epochs=epochs,
        learning_rate=2e-4,
        fp16=torch.cuda.is_available(),
        logging_steps=20,
        save_strategy="epoch",
        report_to="none",
    )
    trainer = SFTTrainer(
        model=model,
        train_dataset=ds,
        dataset_text_field="text",
        max_seq_length=512,
        tokenizer=tokenizer,
        args=args,
    )
    trainer.train()

    merged_out.mkdir(parents=True, exist_ok=True)
    merged_model = model.merge_and_unload()
    merged_model.save_pretrained(str(merged_out))
    tokenizer.save_pretrained(str(merged_out))
    logger.info("Saved merged fine-tuned model to %s", merged_out)
    return True


def export_to_gguf(merged_dir: Path = MERGED_OUT_DIR, gguf_out: Path = GGUF_OUT_FILE, quant: str = "q4_k_m") -> bool:
    """Converts merged HF weights to quantized 4-bit GGUF using llama.cpp tools when available."""
    convert_script = os.getenv("LLAMA_CPP_CONVERT_SCRIPT", "convert_hf_to_gguf.py")
    quantize_bin = os.getenv("LLAMA_QUANTIZE_BIN", "llama-quantize")
    if not merged_dir.exists():
        logger.error("Merged model directory not found: %s", merged_dir)
        return False

    gguf_out.parent.mkdir(parents=True, exist_ok=True)
    f16_gguf = gguf_out.with_suffix(".f16.gguf")
    try:
        subprocess.run([sys.executable, convert_script, str(merged_dir), "--outfile", str(f16_gguf)], check=True)
        subprocess.run([quantize_bin, str(f16_gguf), str(gguf_out), quant], check=True)
        if f16_gguf.exists():
            f16_gguf.unlink()
        logger.info("Exported 4-bit quantized GGUF model to %s", gguf_out)
        return True
    except Exception as ex:
        logger.error("GGUF export failed (ensure llama.cpp convert_hf_to_gguf.py & llama-quantize are installed): %s", ex)
        return False


def main():
    parser = argparse.ArgumentParser(description="MedAssist-Custom-AI LoRA Fine-Tuning & GGUF Pipeline")
    parser.add_argument("--prepare-only", action="store_true", help="Build clean JSONL dataset only")
    parser.add_argument("--train", action="store_true", help="Run LoRA fine-tuning")
    parser.add_argument("--export-gguf", action="store_true", help="Export merged model to 4-bit GGUF")
    parser.add_argument("--epochs", type=int, default=2, help="Number of training epochs")
    args = parser.parse_args()

    count = build_instruction_dataset(OUTPUT_DATASET)
    print(f"Dataset ready: {count} clean clinical records at {OUTPUT_DATASET}")

    if args.train:
        ok = run_lora_finetuning(OUTPUT_DATASET, epochs=args.epochs)
        if ok and args.export_gguf:
            export_to_gguf()


if __name__ == "__main__":
    main()
