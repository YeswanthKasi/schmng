package com.ecorvi.schmng.ui.utils

object AdmissionUtils {
    private val ADMISSION_REGEX = Regex("^ADM-(\\d{4})-(\\d{6})$")

    fun isValidAdmissionNumber(value: String): Boolean {
        return ADMISSION_REGEX.matches(value)
    }

    fun getAdmissionYear(value: String): Int? {
        val m = ADMISSION_REGEX.matchEntire(value) ?: return null
        return m.groupValues.getOrNull(1)?.toIntOrNull()
    }

    fun currentYearPrefix(currentYear: Int): String = "ADM-$currentYear-"

    fun isValidAadhar(value: String): Boolean {
        val digits = value.filter { it.isDigit() }
        return digits.length == 12
    }
}



