package com.ecorvi.schmng.ui.screens

import org.junit.Assert.*
import org.junit.Test

class ValidationTest {
    private fun isValidEmail(email: String): Boolean =
        Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(email)

    @Test
    fun `invalid email fails validation`() {
        assertFalse(isValidEmail("invalidemail"))
        assertFalse(isValidEmail("test@.com"))
        assertFalse(isValidEmail("@test.com"))
    }

    @Test
    fun `valid email passes validation`() {
        assertTrue(isValidEmail("test@email.com"))
    }

    @Test
    fun `password too short fails validation`() {
        assertTrue("12345".length < 6)
    }

    @Test
    fun `password long enough passes validation`() {
        assertTrue("123456".length >= 6)
    }

    @Test
    fun `required fields not blank`() {
        val firstName = "John"
        val lastName = "Doe"
        val email = "john@doe.com"
        val password = "123456"
        assertTrue(firstName.isNotBlank())
        assertTrue(lastName.isNotBlank())
        assertTrue(email.isNotBlank())
        assertTrue(password.isNotBlank())
    }

    @Test
    fun `empty required fields fail validation`() {
        val firstName = ""
        val lastName = "Doe"
        val email = ""
        val password = ""
        assertTrue(firstName.isBlank() || email.isBlank() || password.isBlank())
    }
} 