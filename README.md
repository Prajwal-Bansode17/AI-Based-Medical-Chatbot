# MedAssist AI — ChatGPT-Like Medical Assistant

MedAssist AI is an intelligent, clinically safe, cross-platform medical communication system combining a modern native Android application (Jetpack Compose, Kotlin Coroutines, Retrofit, Supabase Auth) with an upgraded Flask microservice backend (Google Gemini multimodal vision, local Ollama LLM, SQLite with WAL mode, and Scikit-Learn intent classification).

---

## 🌟 Key Features

### 1. Context-Aware Conversational Engine
- **Multi-Turn Continuity:** Tracks symptoms, duration, location, and severity across multiple conversational turns.
- **Reference & Pronoun Resolution:** Resolves ambiguous anaphora (e.g., *"Can I take it with food?"*, *"What are its side effects?"*, *"Will that get worse?"*) using recent conversation context.
- **Clinical Ambiguity Clarification:** Proactively asks focused, clarifying follow-up questions when presented with underspecified symptoms (e.g., vague headache, cough, stomach pain, fever) before providing guidance.
- **Native & Transliterated Multilingual Support:** First-class support for English, Devanagari Marathi (`mr`), Devanagari Hindi (`hi`), Hinglish, and Romanized Marathi (`roman_mr`).

### 2. User-Scoped Long-Term Memory
- **Explicit Consent & Isolation:** Long-term memory is **OFF by default**. Memory read/write is strictly restricted to consented, authenticated accounts. Guest mode and Temporary Chat mode never read or write long-term memory.
- **Strict Clinical Safety Filter:** Strictly rejects sensitive clinical entities from long-term memory:
  - Clinical diagnoses & conditions (e.g., diabetes, hypertension, asthma, cancer)
  - Prescription drugs & dosages (e.g., Metformin 500mg, Paracetamol)
  - Allergies & adverse reactions
  - Clinical vitals & lab results (BP, SpO2, blood sugar)
  - Credentials & secrets (passwords, tokens, API keys)
- **User Inspectability & Control:** Users can inspect stored preferences, correct existing items, delete individual items, or wipe all memories at any time via Profile Settings.
- **Prompt Injection as Background Context:** Retrieved memories are injected into LLM prompts clearly labeled as *User Background Context*, never as system instructions.

### 3. Grounded Medical Image Understanding
- **Format & Payload Validation:** Validates image format via magic bytes (JPEG, PNG, WEBP) and caps payloads at **5 MB**. Corrupted base64 or invalid file types return immediate `400 Bad Request`.
- **Loose Pill Safety Refusal:** If an unpackaged pill or capsule is submitted without packaging, MedAssist AI **refuses to identify it by appearance alone**, explains the grave clinical risks of visual lookalikes and counterfeit medications, and requests original blister packaging or an in-person pharmacist review.
- **10-Point Structured Response:** Legible packaging and prescription labels are analyzed using a standardized 10-point clinical format:
  1. Product / Brand Name
  2. Generic Name & Active Ingredients
  3. Strength & Concentration
  4. Indications & Therapeutic Uses
  5. Dosage Disclaimer (physician/pharmacist determination required)
  6. General Administration Guidance
  7. Common Side Effects
  8. Serious Adverse Reactions (When to Seek Urgent Care)
  9. Contraindications & Warnings
  10. Storage & Pharmacist Consultation
- **Educational Safety Disclaimer:** Every vision response clearly states that image analysis is an educational aid and not a definitive medical evaluation.

### 4. Comprehensive Medical Safety Layer
- **Red-Flag Emergency Triage:** Detects acute, life-threatening symptoms (crushing chest pain radiating to arm/jaw, signs of stroke via FAST, respiratory failure, severe hemorrhaging, anaphylaxis) and routes users immediately to emergency services (**112 / 108**). Home remedies or waiting are strictly prohibited.
- **Mental Health & Crisis Triage:** Identifies suicidal ideation and self-harm expressions in English, Hindi, and Marathi, responding with immediate, compassionate crisis support and directing to **Tele-MANAS (14416)**, **Kiran (1800-599-0019)**, and **988**.
- **Dosage Safety:** The system never invents or prescribes personalized prescription dosages.
- **Prompt Injection Defense:** Neutralizes "DAN" jailbreaks, system prompt extraction attempts, and instruction overrides.

### 5. Modular Provider Routing
- **Multimodal Routing:** Multimodal image requests are routed exclusively to vision-capable models (e.g., Gemini 3.5/3.8 Flash Vision), never sent to text-only LLMs.
- **Resilient Fallback Cascade:** Automatically cascades from local Ollama to Google Gemini, and gracefully falls back to offline retrieval and clinical knowledge bases.

---

## 🗄️ Database Schema & Storage

The backend persists conversation history and long-term memory in SQLite (`ml/data/chat_history.db`) with Write-Ahead Logging (`WAL` mode) enabled for high concurrency.

### Schema Details:
```sql
-- 1. Conversation History (Additive migration)
CREATE TABLE IF NOT EXISTS chat_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id TEXT NOT NULL,
    role TEXT NOT NULL,
    message TEXT NOT NULL,
    symptoms TEXT,
    created_at TEXT NOT NULL,
    user_id TEXT DEFAULT 'guest',
    conversation_id TEXT DEFAULT '',
    is_deleted INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_chat_user_conv ON chat_history (user_id, session_id);

-- 2. User-Scoped Long-Term Memory
CREATE TABLE IF NOT EXISTS user_long_term_memory (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    memory_key TEXT NOT NULL,
    content TEXT NOT NULL,
    category TEXT NOT NULL,
    source TEXT NOT NULL DEFAULT 'user_chat',
    status TEXT NOT NULL DEFAULT 'user-reported',
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    is_deleted INTEGER DEFAULT 0,
    UNIQUE(user_id, memory_key)
);
CREATE INDEX IF NOT EXISTS idx_user_memory_user ON user_long_term_memory (user_id, is_deleted);
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_memory_unique ON user_long_term_memory (user_id, memory_key);

-- 3. User Consent Store
CREATE TABLE IF NOT EXISTS user_memory_consent (
    user_id TEXT PRIMARY KEY,
    consent_enabled INTEGER NOT NULL DEFAULT 0,
    updated_at TEXT NOT NULL
);
```

---

## 🚀 Setup & Installation

### Backend Setup (Python Flask)
1. **Navigate to backend and create virtual environment:**
   ```bash
   cd ml
   python -m venv ..\venv
   ..\venv\Scripts\activate
   pip install -r requirements.txt
   ```
2. **Configure Environment:**
   Copy `.env.example` to `.env` and set your API keys:
   ```bash
   cp .env.example .env
   ```
3. **Run the Flask API Server:**
   ```bash
   python api.py
   ```
   Server starts on `http://0.0.0.0:5000`.

### Android Client Setup
1. Open the project root in Android Studio (Jellyfish / Koala or newer).
2. Sync Gradle with project files.
3. In `RetrofitClient.kt` or via the in-app server settings dialog, ensure `BASE_URL` points to your development machine's local IP address (e.g., `http://192.168.1.X:5000/`).

---

## 🧪 Automated Test Verification

All unit and integration test suites can be executed independently:

| Test Suite | Purpose | Execution Command | Status |
| :--- | :--- | :--- | :--- |
| **T1: Conversation Engine** | Pronoun resolution, ambiguity clarification, dialect classification, prompt injection | `python ml/test_conversation_engine.py` | ✅ **14/14 Passed** |
| **T2: Memory Service** | User isolation, consent gate, guest rejection, safety filter, CRUD | `python ml/test_memory_service.py` | ✅ **8/8 Passed** |
| **T3: Vision Service** | Magic bytes, 5MB limit, loose pill refusal, 10-point format, OCR fallback | `python ml/test_vision_service.py` | ✅ **8/8 Passed** |
| **T4: Safety Layer** | Emergency red flags, crisis triage, helplines, dosage safety | `python ml/test_safety_layer.py` | ✅ **12/12 Passed** |
| **T5: API Endpoints** | REST endpoints (`/predict`, `/api/v1/memory`, consent, auth, rate limit) | `python ml/test_api_endpoints.py` | ✅ **12/12 Passed** |
| **T6: Android Tests** | Android JVM unit tests (`ChatMemoryTest`, UI/data tests) | `.\gradlew.bat testDebugUnitTest` | Verified via Gradle |
