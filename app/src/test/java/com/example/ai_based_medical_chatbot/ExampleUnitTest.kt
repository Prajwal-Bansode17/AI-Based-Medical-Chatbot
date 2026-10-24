package com.example.ai_based_medical_chatbot

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testFormatMeasurement_decimalsAndTrailingZeros() {
        // At most 2 decimal places
        assertEquals("182.38", ui.BMIRepository.formatMeasurement(182.37537))
        assertEquals("77.21", ui.BMIRepository.formatMeasurement(77.213806))

        // No unnecessary trailing zeros
        assertEquals("75", ui.BMIRepository.formatMeasurement(75.0))
        assertEquals("75.2", ui.BMIRepository.formatMeasurement(75.20))
        assertEquals("75.25", ui.BMIRepository.formatMeasurement(75.25))

        // Float overloads and common clinical numbers
        assertEquals("172", ui.BMIRepository.formatMeasurement(172.0f))
        assertEquals("68.5", ui.BMIRepository.formatMeasurement(68.5f))
        assertEquals("", ui.BMIRepository.formatMeasurement(0.0f))
    }
}
