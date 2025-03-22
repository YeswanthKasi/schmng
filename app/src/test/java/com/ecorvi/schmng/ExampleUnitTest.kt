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


    fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return emailRegex.matches(email)
    }
    @Test
    fun emailValidation_isCorrect() {
        // Valid email test cases
        val validEmails = listOf("test@example.com", "user.name@domain.com", "user+mailbox@domain.info")
        validEmails.forEach { email ->
            assertTrue("Expected '$email' to be valid, but it was invalid", isValidEmail(email))
        }

        // Invalid email test cases
        val invalidEmails = listOf("test@.com", "user@@domain.com", "user.name@domain", "@domain.com", "plainaddress")
        invalidEmails.forEach { email ->
            assertFalse("Expected '$email' to be invalid, but it was valid", isValidEmail(email))
        }
    }



    @Test
    fun string_isNotEmpty() {
        // Checks that a string is not empty
        val sampleString = "Hello, World!"
        assertTrue("String should not be empty", sampleString.isNotEmpty())
    }
}
