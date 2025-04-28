package com.ecorvi.schmng.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import org.junit.Rule
import org.junit.Test

class AddPersonScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testEmptyFieldsShowError() {
        composeTestRule.setContent { AddPersonScreen(navController = rememberNavController(), personType = "student") }
        composeTestRule.onNodeWithText("Add Student").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            try {
                val node = composeTestRule.onNodeWithTag("error_message")
                node.assertIsDisplayed()
                node.assertTextEquals("Please fill in all required fields")
                true
            } catch (e: AssertionError) {
                false
            }
        }
        composeTestRule.onNodeWithTag("error_message").assertExists()
    }

    @Test
    fun testInvalidEmailShowsError() {
        composeTestRule.setContent { AddPersonScreen(navController = rememberNavController(), personType = "student") }
        composeTestRule.onNodeWithText("First Name").performTextInput("John")
        composeTestRule.onNodeWithText("Last Name").performTextInput("Doe")
        composeTestRule.onNodeWithText("Password").performTextInput("123456")
        composeTestRule.onNodeWithText("Mobile No").performTextInput("1234567890")
        composeTestRule.onNodeWithText("Email ID").performTextInput("invalidemail")
        composeTestRule.onNodeWithText("Add Student").performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            try {
                val node = composeTestRule.onNodeWithTag("error_message")
                node.assertIsDisplayed()
                node.assertTextEquals("Please enter a valid email address")
                true
            } catch (e: AssertionError) {
                false
            }
        }
        composeTestRule.onNodeWithTag("error_message").assertExists()
    }

    @Test
    fun testShortPasswordShowsError() {
        composeTestRule.setContent { AddPersonScreen(navController = rememberNavController(), personType = "student") }
        composeTestRule.onNodeWithText("First Name").performTextInput("John")
        composeTestRule.onNodeWithText("Last Name").performTextInput("Doe")
        composeTestRule.onNodeWithText("Email ID").performTextInput("john.doe@email.com")
        composeTestRule.onNodeWithText("Mobile No").performTextInput("1234567890")
        composeTestRule.onNodeWithText("Password").performTextInput("123")
        composeTestRule.onNodeWithText("Add Student").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            try {
                val node = composeTestRule.onNodeWithTag("error_message")
                node.assertIsDisplayed()
                node.assertTextEquals("Password must be at least 6 characters")
                true
            } catch (e: AssertionError) {
                false
            }
        }
        composeTestRule.onNodeWithTag("error_message").assertExists()
    }
} 