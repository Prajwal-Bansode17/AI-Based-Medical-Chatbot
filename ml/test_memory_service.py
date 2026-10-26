"""
Automated Test Suite for User-Scoped Long-Term Memory Service (C1-C8, H1, T2)
Tests:
- Strict User A vs User B isolation (no leakage across accounts)
- Guest mode rejection (guests never read or write long-term memory)
- Temporary chat bypass (never reads or writes long-term memory)
- Consent management (OFF by default, enables/disables memory access)
- Acceptance Test C7: Non-sensitive context recalled across chats without diagnosing
- Safety filter strictly rejecting diagnoses, medications, allergies, vitals, credentials
- Memory update, correction, single deletion, and clear all
"""
import unittest
import sqlite3
import uuid
from pathlib import Path
import sys

BASE_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(BASE_DIR))

import memory_service
from memory_service import (
    init_memory_schema,
    get_user_consent,
    set_user_consent,
    get_user_memories,
    save_user_memory,
    update_user_memory,
    delete_user_memory,
    clear_all_user_memories,
    retrieve_relevant_user_memories,
    format_memory_context_block,
    extract_safe_preferences_from_text,
    is_safe_for_memory,
    is_guest_user,
)


class TestMemoryService(unittest.TestCase):

    def setUp(self):
        # Use an isolated test database
        self.test_db = BASE_DIR / "data" / f"test_memory_{uuid.uuid4().hex[:8]}.db"
        memory_service.DB_FILE = self.test_db
        init_memory_schema(self.test_db)

        self.user_a = f"user_alpha_{uuid.uuid4().hex[:6]}"
        self.user_b = f"user_beta_{uuid.uuid4().hex[:6]}"

        # Explicitly enable consent for test users
        set_user_consent(self.user_a, True)
        set_user_consent(self.user_b, True)

    def tearDown(self):
        # Clean up test database file
        try:
            if self.test_db.exists():
                self.test_db.unlink()
        except Exception:
            pass

    def test_user_isolation(self):
        # User A saves communication preference
        save_user_memory(
            user_id=self.user_a,
            memory_key="comm_style",
            content="Prefers short bullet points",
            category="COMMUNICATION"
        )

        # User B saves dietary preference
        save_user_memory(
            user_id=self.user_b,
            memory_key="diet_pref",
            content="Follows strict vegetarian diet",
            category="DIET"
        )

        mems_a = get_user_memories(self.user_a)
        mems_b = get_user_memories(self.user_b)

        # Verify User A only sees A's memories
        contents_a = [m["content"] for m in mems_a]
        self.assertIn("Prefers short bullet points", contents_a)
        self.assertNotIn("Follows strict vegetarian diet", contents_a)

        # Verify User B only sees B's memories
        contents_b = [m["content"] for m in mems_b]
        self.assertIn("Follows strict vegetarian diet", contents_b)
        self.assertNotIn("Prefers short bullet points", contents_b)

    def test_guest_cannot_save_or_read_memory(self):
        # None, empty, or 'guest' user
        self.assertTrue(is_guest_user(None))
        self.assertTrue(is_guest_user("guest"))
        self.assertTrue(is_guest_user("guest_12345"))

        saved = save_user_memory(
            user_id="guest",
            memory_key="guest_note",
            content="Guest lifestyle note",
            category="LIFESTYLE"
        )
        self.assertFalse(saved)
        self.assertEqual(get_user_memories("guest"), [])

    def test_temporary_chat_never_reads_or_writes(self):
        saved = save_user_memory(
            user_id=self.user_a,
            memory_key="temp_key",
            content="Temporary sensitive note",
            category="PREFERENCE",
            is_temporary=True
        )
        self.assertFalse(saved)

        # Even with existing memory, temporary mode returns empty
        save_user_memory(
            user_id=self.user_a,
            memory_key="real_key",
            content="Saved preference",
            category="PREFERENCE",
            is_temporary=False
        )
        self.assertEqual(get_user_memories(self.user_a, is_temporary=True), [])
        self.assertEqual(retrieve_relevant_user_memories(self.user_a, "preference", is_temporary=True), [])

    def test_consent_defaults_to_off(self):
        new_user = f"new_user_{uuid.uuid4().hex[:6]}"
        self.assertFalse(get_user_consent(new_user))

        # When consent is off, saving and retrieval fail
        saved = save_user_memory(
            user_id=new_user,
            memory_key="test",
            content="Some preference",
            category="PREFERENCE"
        )
        self.assertFalse(saved)
        self.assertEqual(get_user_memories(new_user), [])

    def test_acceptance_c7_cross_chat_recall_without_diagnosis(self):
        # Chat 1: User mentions non-sensitive sleep routine & evening tea preference
        save_user_memory(
            user_id=self.user_a,
            memory_key="sleep_routine",
            content="Works late night shifts and drinks chamomile tea before bed",
            category="LIFESTYLE"
        )

        # Chat 2: User asks for relaxation advice
        relevant = retrieve_relevant_user_memories(self.user_a, "I feel tired and have headache in mornings")
        self.assertTrue(len(relevant) > 0)
        formatted_context = format_memory_context_block(relevant)

        # Must contain the background context
        self.assertIn("chamomile tea", formatted_context)
        # Must clearly state this is background context, NOT system instruction, and NOT a diagnosis
        self.assertIn("user background context", formatted_context.lower())
        self.assertNotIn("diagnosed with insomnia", formatted_context.lower())

    def test_safety_filter_rejects_sensitive_data(self):
        # Diagnoses
        self.assertFalse(is_safe_for_memory("Patient was diagnosed with type 2 diabetes"))
        self.assertFalse(is_safe_for_memory("Suffering from chronic depression"))
        self.assertFalse(is_safe_for_memory("I have coronary artery disease"))

        # Medications & dosages
        self.assertFalse(is_safe_for_memory("Take paracetamol 500mg twice daily"))
        self.assertFalse(is_safe_for_memory("Prescribed metformin 1000mg"))
        self.assertFalse(is_safe_for_memory("Needs insulin injection"))

        # Allergies
        self.assertFalse(is_safe_for_memory("Severely allergic to penicillin and sulfa drugs"))

        # Vitals and labs
        self.assertFalse(is_safe_for_memory("Blood pressure is 145/95 mmHg"))
        self.assertFalse(is_safe_for_memory("Fasting blood sugar 160 mg/dl"))

        # Credentials
        self.assertFalse(is_safe_for_memory("User password is SuperSecret123!"))
        self.assertFalse(is_safe_for_memory("Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.token"))

        # Safe preference passes
        self.assertTrue(is_safe_for_memory("Prefers explanations in simple bullet points"))
        self.assertTrue(is_safe_for_memory("Drinks 2 liters of water daily"))

    def test_memory_update_and_unauthorized_protection(self):
        save_user_memory(
            user_id=self.user_a,
            memory_key="water_goal",
            content="Aims for 2 liters water daily",
            category="GOAL"
        )
        memories = get_user_memories(self.user_a)
        mem_id = memories[0]["id"]

        # User A updates their own memory
        updated = update_user_memory(self.user_a, mem_id, "Aims for 3 liters water daily")
        self.assertTrue(updated)
        updated_mems = get_user_memories(self.user_a)
        self.assertEqual(updated_mems[0]["content"], "Aims for 3 liters water daily")

        # User B attempts to update User A's memory -> rejected
        unauth_update = update_user_memory(self.user_b, mem_id, "Hacked by User B")
        self.assertFalse(unauth_update)
        # Content remains unchanged
        verified_mems = get_user_memories(self.user_a)
        self.assertEqual(verified_mems[0]["content"], "Aims for 3 liters water daily")

    def test_memory_delete_and_clear_all(self):
        save_user_memory(self.user_a, "m1", "First note", "PREFERENCE")
        save_user_memory(self.user_a, "m2", "Second note", "PREFERENCE")
        mems = get_user_memories(self.user_a)
        self.assertEqual(len(mems), 2)

        # Delete single memory
        deleted = delete_user_memory(self.user_a, mems[0]["id"])
        self.assertTrue(deleted)
        remaining = get_user_memories(self.user_a)
        self.assertEqual(len(remaining), 1)

        # Clear all
        clear_all_user_memories(self.user_a)
        self.assertEqual(get_user_memories(self.user_a), [])


if __name__ == "__main__":
    unittest.main()
