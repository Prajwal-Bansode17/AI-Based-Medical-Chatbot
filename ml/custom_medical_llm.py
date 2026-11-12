"""
MedAssist-Custom-AI: Self-Hosted Quantized GGUF Inference Engine
Uses llama-cpp-python (optional) with a strict RAM-aware auto-guard so it
never causes Out-Of-Memory (OOM) crashes on 512 MB Render free instances.
"""

import ctypes
import logging
import os
import sys
from pathlib import Path
from typing import Any, Dict, List, Optional

logger = logging.getLogger("medassist.custom_llm")

BASE_DIR = Path(__file__).resolve().parent
DEFAULT_GGUF_PATH = Path(os.getenv("CUSTOM_GGUF_PATH", str(BASE_DIR / "models" / "medassist_custom.gguf")))
MIN_AVAILABLE_RAM_GB = 1.2

try:
    from llama_cpp import Llama  # type: ignore
except ImportError:
    Llama = None


def get_available_ram_gb() -> float:
    """
    Returns currently available system RAM in GB using psutil, /proc/meminfo (Linux),
    or GlobalMemoryStatusEx (Windows). Returns 0.0 if unreadable.
    """
    try:
        import psutil  # type: ignore
        return round(psutil.virtual_memory().available / (1024 ** 3), 3)
    except Exception:
        pass

    # Linux / Render container (/proc/meminfo)
    meminfo_path = Path("/proc/meminfo")
    if meminfo_path.exists():
        try:
            text = meminfo_path.read_text(encoding="utf-8", errors="ignore")
            for line in text.splitlines():
                if line.startswith("MemAvailable:"):
                    kb = int(line.split()[1])
                    return round(kb / (1024 ** 2), 3)
            for line in text.splitlines():
                if line.startswith("MemFree:"):
                    kb = int(line.split()[1])
                    return round(kb / (1024 ** 2), 3)
        except Exception:
            pass

    # Windows GlobalMemoryStatusEx
    if sys.platform.startswith("win"):
        try:
            class MEMORYSTATUSEX(ctypes.Structure):
                _fields_ = [
                    ("dwLength", ctypes.c_ulong),
                    ("dwMemoryLoad", ctypes.c_ulong),
                    ("ullTotalPhys", ctypes.c_ulonglong),
                    ("ullAvailPhys", ctypes.c_ulonglong),
                    ("ullTotalPageFile", ctypes.c_ulonglong),
                    ("ullAvailPageFile", ctypes.c_ulonglong),
                    ("ullTotalVirtual", ctypes.c_ulonglong),
                    ("ullAvailVirtual", ctypes.c_ulonglong),
                    ("sullAvailExtendedVirtual", ctypes.c_ulonglong),
                ]

            stat = MEMORYSTATUSEX()
            stat.dwLength = ctypes.sizeof(MEMORYSTATUSEX)
            if ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(stat)):
                return round(stat.ullAvailPhys / (1024 ** 3), 3)
        except Exception:
            pass

    return 0.0


class CustomTrainedMedicalLLM:
    """
    Self-hosted quantized 4-bit GGUF medical LLM with RAM-aware auto-guard.
    """

    def __init__(
        self,
        model_path: Optional[Path] = None,
        min_ram_gb: float = MIN_AVAILABLE_RAM_GB,
    ):
        self.model_path = Path(model_path) if model_path else DEFAULT_GGUF_PATH
        self.min_ram_gb = float(min_ram_gb)
        self._llm = None
        self._load_attempted = False
        self.skip_reason: Optional[str] = None

    def can_load(self) -> bool:
        force_load = os.getenv("FORCE_LOAD_GGUF", "").strip().lower() in ("1", "true", "yes")
        if Llama is None:
            self.skip_reason = "llama_cpp_not_installed"
            return False
        if not self.model_path.exists():
            self.skip_reason = "gguf_file_missing"
            return False
        avail_gb = get_available_ram_gb()
        if not force_load and avail_gb < self.min_ram_gb:
            self.skip_reason = f"insufficient_ram ({avail_gb:.2f}GB < {self.min_ram_gb:.2f}GB)"
            logger.info(
                "RAM Guard: skipping GGUF load (%s) to protect low-memory environment",
                self.skip_reason,
            )
            return False
        return True

    def ensure_loaded(self) -> bool:
        if self._llm is not None:
            return True
        if self._load_attempted:
            return False
        self._load_attempted = True
        if not self.can_load():
            return False
        try:
            self._llm = Llama(
                model_path=str(self.model_path),
                n_ctx=1536,
                n_threads=max(1, min(4, os.cpu_count() or 2)),
                verbose=False,
            )
            logger.info("Loaded CustomTrainedMedicalLLM from %s", self.model_path)
            return True
        except Exception as ex:
            self.skip_reason = f"load_error: {ex}"
            logger.warning("Failed to load GGUF model %s: %s", self.model_path, ex)
            self._llm = None
            return False

    def generate(
        self,
        user_query: str,
        retrieved_context: str = "",
        history: Optional[List[Dict[str, Any]]] = None,
        language: str = "en",
        dialect: str = "",
    ) -> Optional[str]:
        if not user_query or not self.ensure_loaded():
            return None

        script_hint = (
            "Respond in natural Romanized Marathi matching the user's script."
            if dialect == "roman_mr"
            else "Respond in natural Hinglish matching the user's script."
            if dialect == "hinglish"
            else f"Respond in {language}."
        )
        system_msg = (
            "You are MedAssist-Custom-AI, a concise, accurate clinical assistant. "
            "Use clean ChatGPT-style Markdown bullets with bold lead-ins. "
            f"{script_hint}"
        )

        messages = [{"role": "system", "content": system_msg}]
        for turn in (history or [])[-4:]:
            role = "assistant" if str(turn.get("role", "")).lower() == "assistant" else "user"
            content = str(turn.get("message") or turn.get("content") or "").strip()
            if content:
                messages.append({"role": role, "content": content})

        prompt_body = user_query.strip()
        if retrieved_context and retrieved_context.strip():
            prompt_body = f"{prompt_body}\n\n[Verified Clinical Context: {retrieved_context.strip()}]"
        messages.append({"role": "user", "content": prompt_body})

        try:
            out = self._llm.create_chat_completion(
                messages=messages,
                temperature=0.3,
                max_tokens=380,
            )
            choices = out.get("choices") or []
            if choices:
                text = (choices[0].get("message") or {}).get("content") or ""
                if text.strip():
                    return text.strip()
        except Exception as ex:
            logger.warning("CustomTrainedMedicalLLM inference error: %s", ex)
        return None


_custom_llm_singleton: Optional[CustomTrainedMedicalLLM] = None


def get_custom_medical_llm() -> CustomTrainedMedicalLLM:
    global _custom_llm_singleton
    if _custom_llm_singleton is None:
        _custom_llm_singleton = CustomTrainedMedicalLLM()
    return _custom_llm_singleton
