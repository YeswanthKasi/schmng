package com.ecorvi.schmng.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import org.junit.Rule
import org.junit.Test

class PasswordResetScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testEmptyEmailShowsError() {
        composeTestRule.setContent { PasswordResetScreen(navController = rememberNavController()) }
        composeTestRule.onNodeWithText("Send Reset Link").performClick()
        composeTestRule.onNodeWithText("Please enter your email").assertIsDisplayed()
    }

    @Test
    fun testInvalidEmailShowsError() {
        composeTestRule.setContent { PasswordResetScreen(navController = rememberNavController()) }
        composeTestRule.onNodeWithText("Email").performTextInput("invalidemail")
        composeTestRule.onNodeWithText("Send Reset Link").performClick()
        composeTestRule.onNodeWithText("Invalid email address").assertIsDisplayed()
    }
} 