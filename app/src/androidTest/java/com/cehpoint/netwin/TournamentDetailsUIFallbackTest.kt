package com.cehpoint.netwin

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cehpoint.netwin.presentation.screens.TournamentDetailsScreenUI
import com.cehpoint.netwin.presentation.viewmodels.TournamentViewModel
import com.cehpoint.netwin.domain.model.Tournament
import com.cehpoint.netwin.domain.model.TournamentStatus
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class TournamentDetailsUIFallbackTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    private lateinit var mockViewModel: TournamentViewModel
    private lateinit var mockNavController: NavController

    // Mock StateFlows
    private val selectedTournamentFlow = MutableStateFlow<Tournament?>(null)
    private val isLoadingFlow = MutableStateFlow(false)
    private val errorFlow = MutableStateFlow<String?>(null)

    @Before
    fun setup() {
        hiltRule.inject()
        mockViewModel = mock()
        mockNavController = mock()

        // Setup mock StateFlow behaviors
        whenever(mockViewModel.selectedTournament).thenReturn(selectedTournamentFlow)
        whenever(mockViewModel.isLoadingDetails).thenReturn(isLoadingFlow)
        whenever(mockViewModel.detailsError).thenReturn(errorFlow)
    }

    @Test
    fun testLoadingStateDisplaysProgressIndicator() {
        // Setup loading state
        isLoadingFlow.value = true
        selectedTournamentFlow.value = null
        errorFlow.value = null

        composeTestRule.setContent {
            TournamentDetailsScreenUI(
                tournamentId = "test123",
                navController = mockNavController,
                viewModel = mockViewModel
            )
        }

        // Verify loading indicator is displayed
        composeTestRule.onNodeWithContentDescription("Loading")
            .assertIsDisplayed()

        // Verify other states are not displayed
        composeTestRule.onNodeWithText("Unknown error")
            .assertDoesNotExist()
        composeTestRule.onNodeWithText("No tournament data available")
            .assertDoesNotExist()
    }

    @Test
    fun testErrorStateDisplaysErrorMessage() {
        val errorMessage = "Network connection failed"
        
        // Setup error state
        isLoadingFlow.value = false
        selectedTournamentFlow.value = null
        errorFlow.value = errorMessage

        composeTestRule.setContent {
            TournamentDetailsScreenUI(
                tournamentId = "test123",
                navController = mockNavController,
                viewModel = mockViewModel
            )
        }

        // Verify error message is displayed
        composeTestRule.onNodeWithText(errorMessage)
            .assertIsDisplayed()

        // Verify loading indicator is not displayed
        composeTestRule.onNodeWithContentDescription("Loading")
            .assertDoesNotExist()
    }

    @Test
    fun testNullTournamentDisplaysFallbackMessage() {
        // Setup null tournament state (no error, not loading)
        isLoadingFlow.value = false
        selectedTournamentFlow.value = null
        errorFlow.value = null

        composeTestRule.setContent {
            TournamentDetailsScreenUI(
                tournamentId = "test123",
                navController = mockNavController,
                viewModel = mockViewModel
            )
        }

        // Verify fallback message is displayed
        composeTestRule.onNodeWithText("No tournament data available")
            .assertIsDisplayed()

        // Verify other states are not displayed
        composeTestRule.onNodeWithContentDescription("Loading")
            .assertDoesNotExist()
    }

    @Test
    fun testSuccessfulTournamentDataDisplay() {
        val tournament = Tournament(
            id = "test123",
            name = "Test Tournament",
            description = "Test Description",
            status = TournamentStatus.UPCOMING,
            entryFee = 10.0,
            prizePool = 100.0,
            maxParticipants = 16,
            registeredParticipants = 5,
            startTime = System.currentTimeMillis() + 3600000,
            endTime = System.currentTimeMillis() + 7200000,
            gameMode = "Battle Royale",
            rules = "Standard tournament rules apply",
            imageUrl = "https://example.com/tournament.jpg"
        )
        
        // Setup success state
        isLoadingFlow.value = false
        selectedTournamentFlow.value = tournament
        errorFlow.value = null

        composeTestRule.setContent {
            TournamentDetailsScreenUI(
                tournamentId = "test123",
                navController = mockNavController,
                viewModel = mockViewModel
            )
        }

        // Verify tournament content is displayed
        composeTestRule.onNodeWithText("Test Tournament")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Test Description")
            .assertIsDisplayed()

        // Verify fallback states are not displayed
        composeTestRule.onNodeWithContentDescription("Loading")
            .assertDoesNotExist()
        composeTestRule.onNodeWithText("No tournament data available")
            .assertDoesNotExist()
    }

    @Test
    fun testSlowNetworkScenario() {
        // Start with loading state
        isLoadingFlow.value = true
        selectedTournamentFlow.value = null
        errorFlow.value = null

        composeTestRule.setContent {
            TournamentDetailsScreenUI(
                tournamentId = "test123",
                navController = mockNavController,
                viewModel = mockViewModel
            )
        }

        // Verify loading is shown initially
        composeTestRule.onNodeWithContentDescription("Loading")
            .assertIsDisplayed()

        // Simulate slow network - loading continues
        Thread.sleep(2000) // Simulate 2 second delay
        
        composeTestRule.onNodeWithContentDescription("Loading")
            .assertIsDisplayed()

        // Finally complete with tournament data
        val tournament = Tournament(
            id = "test123",
            name = "Loaded Tournament",
            description = "Finally loaded",
            status = TournamentStatus.UPCOMING,
            entryFee = 10.0,
            prizePool = 100.0,
            maxParticipants = 16,
            registeredParticipants = 5,
            startTime = System.currentTimeMillis() + 3600000,
            endTime = System.currentTimeMillis() + 7200000,
            gameMode = "Battle Royale",
            rules = "Standard tournament rules apply",
            imageUrl = "https://example.com/tournament.jpg"
        )
        
        // Update states to show tournament loaded
        isLoadingFlow.value = false
        selectedTournamentFlow.value = tournament

        // Verify tournament is now displayed
        composeTestRule.onNodeWithText("Loaded Tournament")
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Loading")
            .assertDoesNotExist()
    }

    @Test
    fun testTransitionFromLoadingToError() {
        // Start with loading
        isLoadingFlow.value = true
        selectedTournamentFlow.value = null
        errorFlow.value = null

        composeTestRule.setContent {
            TournamentDetailsScreenUI(
                tournamentId = "test123",
                navController = mockNavController,
                viewModel = mockViewModel
            )
        }

        // Verify loading state
        composeTestRule.onNodeWithContentDescription("Loading")
            .assertIsDisplayed()

        // Transition to error state
        isLoadingFlow.value = false
        errorFlow.value = "Failed to load tournament"

        // Verify error state
        composeTestRule.onNodeWithText("Failed to load tournament")
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Loading")
            .assertDoesNotExist()
    }

    @Test
    fun testBackButtonFunctionality() {
        // Setup any state
        isLoadingFlow.value = false
        selectedTournamentFlow.value = null
        errorFlow.value = null

        composeTestRule.setContent {
            TournamentDetailsScreenUI(
                tournamentId = "test123",
                navController = mockNavController,
                viewModel = mockViewModel
            )
        }

        // Verify back button exists and is clickable
        composeTestRule.onNodeWithContentDescription("Back")
            .assertIsDisplayed()
            .performClick()

        // Verify navigation was triggered (mockito verification would need proper setup)
        verify(mockNavController, timeout(1000)).navigateUp()
    }
}
