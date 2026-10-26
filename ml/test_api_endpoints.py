"""
Automated Test Suite for API Endpoints (T5, B5, H1, H3)
Tests:
- POST /predict with text-only query
- POST /predict with image (valid base64 JPEG)
- POST /predict with oversized and corrupted image rejection (400)
- POST /predict loose pill refusal by appearance alone
- GET /api/v1/memory, POST /api/v1/memory, PUT /api/v1/memory/<id>, DELETE /api/v1/memory/<id>, DELETE /api/v1/memory
- GET /api/v1/memory/consent & POST /api/v1/memory/consent
- GET /api/v1/conversations for authenticated user
- Security: 401 Unauthorized for missing auth, 403 Forbidden for guests
- Security: Safety filter blocks clinical data (diagnosis, medications, passwords) from memory (400)
- Security: 429 Rate limiting enforcement
"""

import unittest
import base64
import json
import os
import sys
import uuid
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(BASE_DIR))

# Disable Ollama network calls during unit test
os.environ["OLLAMA_ENABLED"] = "0"

from api import app, check_rate_limit, _RATE_LIMIT_BUCKET, MAX_REQUESTS_PER_MINUTE
import memory_service


class TestApiEndpoints(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        # Configure test database
        cls.test_db = BASE_DIR / "data" / f"test_api_{uuid.uuid4().hex[:8]}.db"
        memory_service.DB_FILE = cls.test_db
        memory_service.init_memory_schema(cls.test_db)

        # Valid small JPEG payload for image tests
        cls.valid_jpeg_bytes = b"\xFF\xD8\xFF\xE0\x00\x10JFIF\x00\x01\x01\x01\x00`\x00`\x00\x00" + b"\x00" * 64
        cls.valid_jpeg_b64 = base64.b64encode(cls.valid_jpeg_bytes).decode("utf-8")

    @classmethod
    def tearDownClass(cls):
        if cls.test_db.exists():
            try:
                cls.test_db.unlink()
            except Exception:
                pass

    def setUp(self):
        self.client = app.test_client()
        self.user_token = f"user_{uuid.uuid4().hex[:8]}"
        self.auth_headers = {"Authorization": f"Bearer {self.user_token}"}
        # Enable consent for test user
        memory_service.set_user_consent(self.user_token, True)

    # ------------------------------------------------------------------
    # 1. /predict ENDPOINT TESTS
    # ------------------------------------------------------------------
    def test_predict_text_only_query(self):
        """POST /predict with normal medical query returns 200 and medical answer."""
        resp = self.client.post("/predict", json={
            "question": "I have mild fever and sore throat",
            "session_id": f"sess_{uuid.uuid4().hex[:6]}"
        })
        self.assertEqual(resp.status_code, 200)
        data = resp.get_json()
        self.assertEqual(data["status"], "success")
        self.assertIn("answer", data)
        self.assertTrue(len(data["answer"]) > 0)
        self.assertIn("fever", data.get("symptoms", []))

    def test_predict_with_valid_image(self):
        """POST /predict with valid base64 image payload returns 200."""
        resp = self.client.post("/predict", json={
            "question": "Can you check this medication package?",
            "image": self.valid_jpeg_b64,
            "session_id": f"sess_img_{uuid.uuid4().hex[:6]}"
        })
        self.assertEqual(resp.status_code, 200)
        data = resp.get_json()
        self.assertEqual(data["status"], "success")
        self.assertIn("answer", data)

    def test_predict_rejects_oversized_image(self):
        """POST /predict with image > 5MB returns 400 Bad Request."""
        oversized = base64.b64encode(self.valid_jpeg_bytes + b"\x00" * (5 * 1024 * 1024 + 100)).decode("utf-8")
        resp = self.client.post("/predict", json={
            "question": "Inspect this image",
            "image": oversized,
        })
        self.assertEqual(resp.status_code, 400)
        data = resp.get_json()
        self.assertEqual(data["status"], "error")
        self.assertIn("exceeds maximum limit", data["message"])

    def test_predict_rejects_corrupted_base64_image(self):
        """POST /predict with invalid base64 image returns 400."""
        resp = self.client.post("/predict", json={
            "question": "Inspect this image",
            "image": "!!CorruptedBase64&&*",
        })
        self.assertEqual(resp.status_code, 400)
        data = resp.get_json()
        self.assertEqual(data["status"], "error")

    def test_predict_loose_pill_refusal(self):
        """POST /predict asking to identify loose pill by appearance returns clinical refusal."""
        resp = self.client.post("/predict", json={
            "question": "What is this loose white round pill?",
            "image": self.valid_jpeg_b64,
        })
        self.assertEqual(resp.status_code, 200)
        data = resp.get_json()
        self.assertEqual(data["status"], "success")
        self.assertIn("Cannot Identify Medication by Appearance Alone", data["answer"])
        self.assertIn("blister strip", data["answer"].lower())

    # ------------------------------------------------------------------
    # 2. LONG-TERM MEMORY REST API CRUD (C1-C8)
    # ------------------------------------------------------------------
    def test_memory_crud_lifecycle(self):
        """Full lifecycle: get empty -> add -> inspect -> update -> delete -> clear."""
        # 1. Initial list should be empty
        get_res = self.client.get("/api/v1/memory", headers=self.auth_headers)
        self.assertEqual(get_res.status_code, 200)
        self.assertEqual(get_res.get_json()["count"], 0)

        # 2. Add safe memory item
        add_res = self.client.post("/api/v1/memory", headers=self.auth_headers, json={
            "key": "dietary_preference",
            "content": "Patient prefers a strictly vegetarian diet",
            "category": "PREFERENCE"
        })
        self.assertEqual(add_res.status_code, 201)
        mem_id = add_res.get_json().get("id") or "dietary_preference"

        # 3. Retrieve and inspect
        get_res2 = self.client.get("/api/v1/memory", headers=self.auth_headers)
        self.assertEqual(get_res2.status_code, 200)
        memories = get_res2.get_json()["memories"]
        self.assertEqual(len(memories), 1)
        self.assertIn("vegetarian", memories[0]["content"])
        created_id = memories[0]["id"]

        # 4. Update memory item
        up_res = self.client.put(f"/api/v1/memory/{created_id}", headers=self.auth_headers, json={
            "content": "Patient prefers a vegan plant-based diet"
        })
        self.assertEqual(up_res.status_code, 200)

        # Verify updated content
        get_res3 = self.client.get("/api/v1/memory", headers=self.auth_headers)
        self.assertIn("vegan", get_res3.get_json()["memories"][0]["content"])

        # 5. Delete single memory
        del_res = self.client.delete(f"/api/v1/memory/{created_id}", headers=self.auth_headers)
        self.assertEqual(del_res.status_code, 200)
        get_res4 = self.client.get("/api/v1/memory", headers=self.auth_headers)
        self.assertEqual(get_res4.get_json()["count"], 0)

        # 6. Add two memories then clear all
        self.client.post("/api/v1/memory", headers=self.auth_headers, json={
            "key": "pref1", "content": "Prefers morning appointments", "category": "PREFERENCE"
        })
        self.client.post("/api/v1/memory", headers=self.auth_headers, json={
            "key": "pref2", "content": "Speaks English and Marathi", "category": "PREFERENCE"
        })
        self.assertEqual(self.client.get("/api/v1/memory", headers=self.auth_headers).get_json()["count"], 2)

        clear_res = self.client.delete("/api/v1/memory", headers=self.auth_headers)
        self.assertEqual(clear_res.status_code, 200)
        self.assertEqual(self.client.get("/api/v1/memory", headers=self.auth_headers).get_json()["count"], 0)

    def test_memory_safety_filter_blocks_sensitive_content(self):
        """Clinical safety filter strictly rejects diagnoses, medications, and credentials."""
        # Diagnosis
        res1 = self.client.post("/api/v1/memory", headers=self.auth_headers, json={
            "key": "diag", "content": "Diagnosed with acute bronchitis", "category": "CLINICAL"
        })
        self.assertEqual(res1.status_code, 400)
        self.assertIn("Safety filter rejected", res1.get_json()["message"])

        # Medication prescription
        res2 = self.client.post("/api/v1/memory", headers=self.auth_headers, json={
            "key": "med", "content": "Prescribed 500mg Metformin tablet daily", "category": "CLINICAL"
        })
        self.assertEqual(res2.status_code, 400)

        # Credentials
        res3 = self.client.post("/api/v1/memory", headers=self.auth_headers, json={
            "key": "cred", "content": "User account password is secret_token_123", "category": "CREDENTIAL"
        })
        self.assertEqual(res3.status_code, 400)

    # ------------------------------------------------------------------
    # 3. CONSENT ENDPOINTS
    # ------------------------------------------------------------------
    def test_memory_consent_get_and_set(self):
        """GET /api/v1/memory/consent and POST /api/v1/memory/consent."""
        test_user = f"user_consent_{uuid.uuid4().hex[:6]}"
        headers = {"Authorization": f"Bearer {test_user}"}

        # Consent is OFF by default for fresh user
        get_res = self.client.get("/api/v1/memory/consent", headers=headers)
        self.assertEqual(get_res.status_code, 200)
        self.assertFalse(get_res.get_json()["consent"])

        # Enable consent
        post_res = self.client.post("/api/v1/memory/consent", headers=headers, json={"consent": True})
        self.assertEqual(post_res.status_code, 200)
        self.assertTrue(post_res.get_json()["consent"])

        # Verify persisted
        get_res2 = self.client.get("/api/v1/memory/consent", headers=headers)
        self.assertTrue(get_res2.get_json()["consent"])

    # ------------------------------------------------------------------
    # 4. AUTHENTICATION & ACCESS CONTROL (H1)
    # ------------------------------------------------------------------
    def test_unauthenticated_requests_rejected(self):
        """Missing Authorization header must return 401 Unauthorized for user endpoints."""
        endpoints = [
            ("GET", "/api/v1/memory"),
            ("POST", "/api/v1/memory"),
            ("DELETE", "/api/v1/memory"),
            ("GET", "/api/v1/conversations"),
        ]
        for method, ep in endpoints:
            if method == "GET":
                res = self.client.get(ep)
            elif method == "POST":
                res = self.client.post(ep, json={"content": "test"})
            elif method == "DELETE":
                res = self.client.delete(ep)
            self.assertEqual(res.status_code, 401, f"{method} {ep} should require auth")

    def test_guest_access_forbidden(self):
        """Guest users must receive 403 Forbidden on memory write endpoints."""
        guest_headers = {"Authorization": "Bearer guest"}
        res = self.client.get("/api/v1/memory", headers=guest_headers)
        self.assertEqual(res.status_code, 403)

        res_post = self.client.post("/api/v1/memory", headers=guest_headers, json={"content": "test"})
        self.assertEqual(res_post.status_code, 403)

    # ------------------------------------------------------------------
    # 5. CONVERSATIONS LIST ENDPOINT
    # ------------------------------------------------------------------
    def test_get_conversations_authenticated(self):
        """GET /api/v1/conversations returns 200 with list of conversations."""
        res = self.client.get("/api/v1/conversations", headers=self.auth_headers)
        self.assertEqual(res.status_code, 200)
        data = res.get_json()
        self.assertEqual(data["status"], "success")
        self.assertIsInstance(data["conversations"], list)

    # ------------------------------------------------------------------
    # 6. RATE LIMITING ENFORCEMENT (H3)
    # ------------------------------------------------------------------
    def test_rate_limiting_enforcement(self):
        """Rate limiting should reject excessive requests with 429."""
        # Use an isolated test IP client
        test_ip = "192.0.2.99"
        # Pre-fill bucket to exceed limit
        _RATE_LIMIT_BUCKET[test_ip] = [1000000000.0] * MAX_REQUESTS_PER_MINUTE
        # In check_rate_limit:
        is_allowed = check_rate_limit(test_ip)
        # Should now be allowed because timestamps are in the past (< now - 60)
        self.assertTrue(is_allowed)

        # Now fill with current time
        import time
        now = time.time()
        _RATE_LIMIT_BUCKET[test_ip] = [now] * MAX_REQUESTS_PER_MINUTE
        is_blocked = check_rate_limit(test_ip)
        self.assertFalse(is_blocked)


    # ------------------------------------------------------------------
    # 7. CROSS-USER ISOLATION & TOKEN VERIFICATION (Priority 1)
    # ------------------------------------------------------------------
    def test_cross_user_isolation(self):
        """User A's memories must never be visible to User B."""
        user_a = f"user_a_{uuid.uuid4().hex[:6]}"
        user_b = f"user_b_{uuid.uuid4().hex[:6]}"
        headers_a = {"Authorization": f"Bearer {user_a}"}
        headers_b = {"Authorization": f"Bearer {user_b}"}

        # Enable consent for both
        self.client.post("/api/v1/memory/consent", headers=headers_a, json={"consent": True})
        self.client.post("/api/v1/memory/consent", headers=headers_b, json={"consent": True})

        # User A saves memory
        res_a = self.client.post("/api/v1/memory", headers=headers_a, json={
            "key": "diet_veg",
            "content": "Follows a strict vegetarian diet",
            "category": "DIET"
        })
        self.assertEqual(res_a.status_code, 201)

        # User B queries memories: should NOT see User A's memory
        res_b = self.client.get("/api/v1/memory", headers=headers_b)
        self.assertEqual(res_b.status_code, 200)
        data_b = res_b.get_json()
        self.assertEqual(data_b["count"], 0)
        self.assertEqual(data_b["memories"], [])

        # User A queries memories: sees own memory
        res_a_get = self.client.get("/api/v1/memory", headers=headers_a)
        self.assertEqual(res_a_get.status_code, 200)
        data_a = res_a_get.get_json()
        self.assertEqual(data_a["count"], 1)
        self.assertEqual(data_a["memories"][0]["memory_key"], "diet_veg")

    def test_token_user_id_mismatch_forbidden(self):
        """Client-supplied user_id that differs from token must return 403 Forbidden."""
        attacker_headers = {"Authorization": "Bearer user_attacker_99"}
        # Attempt to inject memory for victim
        res = self.client.post("/api/v1/memory", headers=attacker_headers, json={
            "user_id": "user_victim_88",
            "key": "spoofed_pref",
            "content": "Prefers concise bullet points"
        })
        self.assertEqual(res.status_code, 403)
        self.assertIn("Forbidden", res.get_json()["message"])

        # Attempt to read victim's memories
        res_get = self.client.get("/api/v1/memory?user_id=user_victim_88", headers=attacker_headers)
        self.assertEqual(res_get.status_code, 403)

    def test_expired_jwt_token_rejected(self):
        """Expired JWT token must return 401 Unauthorized."""
        import time
        import base64
        import json
        header = base64.urlsafe_b64encode(b'{"alg":"HS256","typ":"JWT"}').decode().rstrip("=")
        # Expired 1 hour ago
        payload_data = {"sub": "user_expired_1", "exp": int(time.time()) - 3600}
        payload = base64.urlsafe_b64encode(json.dumps(payload_data).encode()).decode().rstrip("=")
        token = f"{header}.{payload}.signature"

        res = self.client.get("/api/v1/memory", headers={"Authorization": f"Bearer {token}"})
        self.assertEqual(res.status_code, 401)
        self.assertIn("expired", res.get_json()["message"].lower())

    def test_temporary_chat_bypasses_memory(self):
        """Temporary chat mode must not persist memories even with consent."""
        temp_user = f"user_temp_{uuid.uuid4().hex[:6]}"
        headers = {"Authorization": f"Bearer {temp_user}"}
        # Opt in
        self.client.post("/api/v1/memory/consent", headers=headers, json={"consent": True})

        # Send temporary predict query with non-sensitive preference
        res = self.client.post("/predict", headers=headers, json={
            "text": "I follow a vegetarian diet. What should I eat for breakfast?",
            "is_temporary": True,
            "session_id": f"temp_sess_{uuid.uuid4().hex[:6]}"
        })
        self.assertEqual(res.status_code, 200)

        # Check memory store: should remain empty because it was temporary chat
        res_mem = self.client.get("/api/v1/memory", headers=headers)
        self.assertEqual(res_mem.status_code, 200)
        self.assertEqual(res_mem.get_json()["count"], 0)

    def test_correlation_id_propagation_in_header_and_body(self):
        """Correlation ID from request header or body must be preserved and returned."""
        custom_cid = "req_custom_9876"
        res = self.client.post("/predict", headers={"X-Correlation-ID": custom_cid}, json={
            "question": "What is ibuprofen used for?",
            "session_id": f"sess_cid_{uuid.uuid4().hex[:6]}"
        })
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res.headers.get("X-Correlation-ID"), custom_cid)
        data = res.get_json()
        self.assertEqual(data.get("correlation_id"), custom_cid)

    def test_correlation_id_generated_if_absent(self):
        """When correlation ID is not supplied, backend auto-generates a valid req_ ID."""
        res = self.client.post("/predict", json={
            "question": "I have a cough and cold",
            "session_id": f"sess_nocid_{uuid.uuid4().hex[:6]}"
        })
        self.assertEqual(res.status_code, 200)
        cid = res.headers.get("X-Correlation-ID")
        self.assertIsNotNone(cid)
        self.assertTrue(cid.startswith("req_"))
        data = res.get_json()
        self.assertEqual(data.get("correlation_id"), cid)

    def test_ollama_circuit_breaker_and_fast_skip(self):
        """Ollama circuit breaker marks failures, increases cooldown, and skips inference instantly."""
        from api import _ollama_state, _ollama_mark_down, _ollama_in_cooldown, ollama_chat
        import time

        initial_failures = _ollama_state["consecutive_failures"]
        _ollama_mark_down("Simulated network timeout")
        self.assertTrue(_ollama_in_cooldown())
        self.assertGreater(_ollama_state["consecutive_failures"], initial_failures)

        # In cooldown: ollama_chat must immediately return None in < 50ms without hanging
        start = time.time()
        result = ollama_chat("system prompt", "user query")
        elapsed = time.time() - start
        self.assertIsNone(result)
        self.assertLess(elapsed, 0.05)


if __name__ == "__main__":
    unittest.main()
