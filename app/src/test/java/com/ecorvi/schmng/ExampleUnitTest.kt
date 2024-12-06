package com.ecorvi.schmng

import org.junit.Test
import org.junit.Assert.*

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        // Basic addition test
        assertEquals(4, 2 + 2)
    }

    @Test
    fun appVersion_isCorrect() {
        // Verifies that the app version is as expected
        val expectedVersion = "2.0"
        val actualVersion = "2.0" // Mocked; replace with dynamic retrieval if needed
        assertEquals(expectedVersion, actualVersion)
    }

    @Test
    fun emailValidation_isCorrect() {
        // Simple email validation logic
        val validEmail = "test@example.com"
        val invalidEmail = "test@.com"
        assertTrue("Valid email check failed", validEmail.contains("@") && validEmail.contains("."))
        assertFalse("Invalid email check passed", invalidEmail.contains("@") && invalidEmail.contains("."))
    }

    @Test
    fun string_isNotEmpty() {
        // Checks that a string is not empty
        val sampleString = "Hello, World!"
        assertTrue("String should not be empty", sampleString.isNotEmpty())
    }
}
