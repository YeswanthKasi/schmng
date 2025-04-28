package com.ecorvi.schmng.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testInvalidEmailShowsError() {
        composeTestRule.setContent { LoginScreen(navController = rememberNavController()) }
        composeTestRule.onNodeWithText("Email").performTextInput("invalidemail")
        composeTestRule.onNodeWithText("Password").performTextInput("123456")
        composeTestRule.onNodeWithText("Sign In").performClick()
        composeTestRule.onNodeWithText("Invalid email address").assertIsDisplayed()
    }

    @Test
    fun testShortPasswordShowsError() {
        composeTestRule.setContent { LoginScreen(navController = rememberNavController()) }
        composeTestRule.onNodeWithText("Email").performTextInput("test@email.com")
        composeTestRule.onNodeWithText("Password").performTextInput("123")
        composeTestRule.onNodeWithText("Sign In").performClick()
        composeTestRule.onNodeWithText("Password must be at least 6 characters").assertIsDisplayed()
    }

    @Test
    fun testEmptyFieldsShowError() {
        composeTestRule.setContent { LoginScreen(navController = rememberNavController()) }
        composeTestRule.onNodeWithText("Sign In").performClick()
        composeTestRule.onNodeWithText("Please enter email and password").assertIsDisplayed()
    }
} 