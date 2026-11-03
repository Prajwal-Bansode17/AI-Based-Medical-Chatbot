package com.example.ai_based_medical_chatbot

import com.example.ai_based_medical_chatbot.helpers.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class HealthTipReminderTest {

    private lateinit var fakePrefs: FakeSharedPreferences

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
    }

    @Test
    fun reminderCreationAndRetrievalSucceeds() {
        val reminder = HealthTipReminder(
            id = "rem_1",
            tipId = 5,
            title = "Morning Warm Water Routine",
            description = "Drink 2 glasses of warm water right after waking up.",
            time = "07:30 AM",
            frequency = HealthReminderFrequency.DAILY,
            enabled = true
        )

        HealthTipReminderRepository.save(fakePrefs, reminder)

        val retrieved = HealthTipReminderRepository.getById(fakePrefs, "rem_1")
        assertNotNull("Saved reminder should be retrieved by ID", retrieved)
        assertEquals("Morning Warm Water Routine", retrieved?.title)
        assertEquals("07:30 AM", retrieved?.time)
        assertEquals(HealthReminderFrequency.DAILY, retrieved?.frequency)
        assertTrue(retrieved?.enabled == true)
        assertEquals(5, retrieved?.tipId)
    }

    @Test
    fun reminderEditingUpdatesRecordInPlace() {
        val reminder = HealthTipReminder(
            id = "rem_edit",
            tipId = 2,
            title = "Amla Morning Dose",
            description = "Take fresh amla juice.",
            time = "08:00 AM",
            frequency = HealthReminderFrequency.DAILY
        )

        HealthTipReminderRepository.save(fakePrefs, reminder)
        assertEquals(1, HealthTipReminderRepository.getAll(fakePrefs).size)

        // Edit title, time and frequency
        val updated = reminder.copy(
            title = "Amla & Honey Dose",
            time = "08:30 AM",
            frequency = HealthReminderFrequency.WEEKLY,
            selectedDays = listOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY)
        )
        HealthTipReminderRepository.save(fakePrefs, updated)

        val list = HealthTipReminderRepository.getAll(fakePrefs)
        assertEquals("List size should remain 1 after editing", 1, list.size)

        val retrieved = HealthTipReminderRepository.getById(fakePrefs, "rem_edit")
        assertEquals("Amla & Honey Dose", retrieved?.title)
        assertEquals("08:30 AM", retrieved?.time)
        assertEquals(HealthReminderFrequency.WEEKLY, retrieved?.frequency)
        assertEquals(3, retrieved?.selectedDays?.size)
    }

    @Test
    fun reminderDeletionRemovesItemFromRepository() {
        val rem1 = HealthTipReminder(id = "del_1", title = "Habit 1")
        val rem2 = HealthTipReminder(id = "del_2", title = "Habit 2")

        HealthTipReminderRepository.save(fakePrefs, rem1)
        HealthTipReminderRepository.save(fakePrefs, rem2)
        assertEquals(2, HealthTipReminderRepository.getAll(fakePrefs).size)

        val deleted = HealthTipReminderRepository.delete(fakePrefs, "del_1")
        assertTrue("Deletion should report success", deleted)

        val list = HealthTipReminderRepository.getAll(fakePrefs)
        assertEquals(1, list.size)
        assertNull("Deleted reminder should return null", HealthTipReminderRepository.getById(fakePrefs, "del_1"))
        assertNotNull("Remaining reminder should exist", HealthTipReminderRepository.getById(fakePrefs, "del_2"))
    }

    @Test
    fun duplicatePreventionReusesExistingRecordWhenTitleAndTimeMatch() {
        val first = HealthTipReminder(
            id = "first_id",
            title = "Drink Water",
            time = "09:00 AM",
            description = "Stay hydrated"
        )
        HealthTipReminderRepository.save(fakePrefs, first)
        assertEquals(1, HealthTipReminderRepository.getAll(fakePrefs).size)

        // Second reminder with different ID but identical title and time
        val duplicate = HealthTipReminder(
            id = "second_id",
            title = "Drink Water",
            time = "09:00 AM",
            description = "Updated description"
        )
        HealthTipReminderRepository.save(fakePrefs, duplicate)

        val list = HealthTipReminderRepository.getAll(fakePrefs)
        assertEquals("Duplicate title+time should not create duplicate entries", 1, list.size)
        assertEquals("Updated description", list.first().description)
    }

    @Test
    fun enableAndDisableToggleWorksProperly() {
        val reminder = HealthTipReminder(
            id = "toggle_1",
            title = "Evening Walk",
            enabled = true
        )
        HealthTipReminderRepository.save(fakePrefs, reminder)

        assertTrue(HealthTipReminderRepository.getById(fakePrefs, "toggle_1")?.enabled == true)
        assertEquals(1, HealthTipReminderRepository.getEnabled(fakePrefs).size)

        HealthTipReminderRepository.updateEnabled(fakePrefs, "toggle_1", false)
        assertFalse(HealthTipReminderRepository.getById(fakePrefs, "toggle_1")?.enabled == true)
        assertEquals(0, HealthTipReminderRepository.getEnabled(fakePrefs).size)

        HealthTipReminderRepository.updateEnabled(fakePrefs, "toggle_1", true)
        assertTrue(HealthTipReminderRepository.getById(fakePrefs, "toggle_1")?.enabled == true)
        assertEquals(1, HealthTipReminderRepository.getEnabled(fakePrefs).size)
    }

    @Test
    fun dailyScheduleCalculatesNextTriggerCorrectly() {
        // Set reference time: 2026-11-03 10:00:00
        val baseCalendar = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 3, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val refMillis = baseCalendar.timeInMillis

        // 1. Reminder for 11:30 AM today (in the future)
        val futureToday = HealthTipReminder(
            time = "11:30 AM",
            frequency = HealthReminderFrequency.DAILY
        )
        val nextFuture = HealthTipReminderRepository.calculateNextTriggerMillis(futureToday, refMillis)
        assertNotNull(nextFuture)

        val targetFutureCal = Calendar.getInstance().apply { timeInMillis = nextFuture!! }
        assertEquals(3, targetFutureCal.get(Calendar.DAY_OF_MONTH)) // Today
        assertEquals(11, targetFutureCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, targetFutureCal.get(Calendar.MINUTE))

        // 2. Reminder for 08:00 AM today (already passed) -> should schedule for tomorrow 08:00 AM
        val passedToday = HealthTipReminder(
            time = "08:00 AM",
            frequency = HealthReminderFrequency.DAILY
        )
        val nextTomorrow = HealthTipReminderRepository.calculateNextTriggerMillis(passedToday, refMillis)
        assertNotNull(nextTomorrow)

        val targetTomorrowCal = Calendar.getInstance().apply { timeInMillis = nextTomorrow!! }
        assertEquals(4, targetTomorrowCal.get(Calendar.DAY_OF_MONTH)) // Tomorrow
        assertEquals(8, targetTomorrowCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, targetTomorrowCal.get(Calendar.MINUTE))
    }

    @Test
    fun weeklyScheduleTriggersOnlyOnSelectedWeekdays() {
        // Reference time: Tuesday, 2026-11-03 at 10:00 AM
        val baseCalendar = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 3, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val refMillis = baseCalendar.timeInMillis
        assertEquals(Calendar.TUESDAY, baseCalendar.get(Calendar.DAY_OF_WEEK))

        // Reminder scheduled for Fridays at 06:00 PM
        val fridayReminder = HealthTipReminder(
            time = "06:00 PM",
            frequency = HealthReminderFrequency.WEEKLY,
            selectedDays = listOf(Calendar.FRIDAY)
        )
        val trigger = HealthTipReminderRepository.calculateNextTriggerMillis(fridayReminder, refMillis)
        assertNotNull(trigger)

        val resultCal = Calendar.getInstance().apply { timeInMillis = trigger!! }
        assertEquals("Target day of week should be FRIDAY", Calendar.FRIDAY, resultCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(18, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCal.get(Calendar.MINUTE))
        assertTrue("Trigger must be strictly in the future", trigger!! > refMillis)
    }

    @Test
    fun getNextScheduledFindsClosestUpcomingReminder() {
        // Reference time: 2026-11-03 10:00:00
        val baseCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 3, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val refMillis = baseCal.timeInMillis

        val remLater = HealthTipReminder(
            id = "rem_later",
            title = "Afternoon Walk",
            time = "04:00 PM",
            frequency = HealthReminderFrequency.DAILY,
            enabled = true
        )
        val remSooner = HealthTipReminder(
            id = "rem_sooner",
            title = "Hydration Check",
            time = "11:00 AM",
            frequency = HealthReminderFrequency.DAILY,
            enabled = true
        )
        val remDisabled = HealthTipReminder(
            id = "rem_disabled",
            title = "Ignored Routine",
            time = "10:30 AM",
            frequency = HealthReminderFrequency.DAILY,
            enabled = false
        )

        HealthTipReminderRepository.save(fakePrefs, remLater)
        HealthTipReminderRepository.save(fakePrefs, remSooner)
        HealthTipReminderRepository.save(fakePrefs, remDisabled)

        val nextPair = HealthTipReminderRepository.getNextScheduled(fakePrefs, refMillis)
        assertNotNull("Next scheduled pair should not be null", nextPair)
        assertEquals("rem_sooner", nextPair?.first?.id)
        assertEquals("Hydration Check", nextPair?.first?.title)
    }

    @Test
    fun jsonRoundTripPreservesAllFields() {
        val original = HealthTipReminder(
            id = "json_test_id",
            tipId = 14,
            title = "Sleep Hygiene Habit",
            description = "Dim lights 30 minutes before bed.",
            time = "10:15 PM",
            frequency = HealthReminderFrequency.WEEKLY,
            selectedDays = listOf(Calendar.SATURDAY, Calendar.SUNDAY),
            enabled = true,
            createdAt = 1729000000000L
        )

        val json = original.toJson()
        val restored = HealthTipReminder.fromJson(json)

        assertEquals(original.id, restored.id)
        assertEquals(original.tipId, restored.tipId)
        assertEquals(original.title, restored.title)
        assertEquals(original.description, restored.description)
        assertEquals(original.time, restored.time)
        assertEquals(original.frequency, restored.frequency)
        assertEquals(original.selectedDays, restored.selectedDays)
        assertEquals(original.enabled, restored.enabled)
        assertEquals(original.createdAt, restored.createdAt)
    }
}
