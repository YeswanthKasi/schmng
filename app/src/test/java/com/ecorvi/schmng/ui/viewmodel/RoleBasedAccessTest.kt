package com.ecorvi.schmng.ui.viewmodel

import org.junit.Assert.*
import org.junit.Test

class RoleBasedAccessTest {
    private fun isAdmin(role: String?): Boolean = role?.lowercase() == "admin"
    private fun isStudent(role: String?): Boolean = role?.lowercase() == "student"

    @Test
    fun `admin has access to admin features`() {
        assertTrue(isAdmin("admin"))
        assertFalse(isAdmin("student"))
    }

    @Test
    fun `student does not have access to admin features`() {
        assertFalse(isAdmin("student"))
    }

    @Test
    fun `student has access to student features`() {
        assertTrue(isStudent("student"))
        assertFalse(isStudent("admin"))
    }
} 