package com.cehpoint.netwin

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cehpoint.netwin.presentation.activities.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class TournamentRegistrationUITest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testRegistrationFlow() {
        // Note: This test assumes that the user is already logged in
        // and that there is at least one tournament available to register for.

        // 1. Click on a tournament to start the registration flow
        composeTestRule.onNodeWithText("Join Tournament").performClick()

        // 2. Step 1: Review - simply continue
        composeTestRule.onNodeWithText("Continue to Payment").performClick()

        // 3. Step 2: Payment - simply continue
        composeTestRule.onNodeWithText("Continue to Details").performClick()

        // 4. Step 3: Details - fill in the form
        composeTestRule.onNodeWithText("In-Game ID").performTextInput("MyGameID")
        composeTestRule.onNodeWithText("Team Name").performTextInput("MyTeamName")
        composeTestRule.onNodeWithText("I accept the terms and conditions").performClick()
        composeTestRule.onNodeWithText("Review & Submit").performClick()

        // 5. Step 4: Confirm - submit the registration
        composeTestRule.onNodeWithText("Complete Registration").performClick()

        // 6. Verify that the registration was successful
        composeTestRule.onNodeWithText("Successfully registered for tournament!").assertIsDisplayed()
    }
}

