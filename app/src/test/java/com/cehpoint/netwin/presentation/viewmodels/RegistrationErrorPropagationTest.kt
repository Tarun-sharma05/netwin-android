package com.cehpoint.netwin.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import com.cehpoint.netwin.data.local.DataStoreManager
import com.cehpoint.netwin.data.remote.FirebaseManager
import com.cehpoint.netwin.domain.model.RegistrationStep
import com.cehpoint.netwin.domain.model.RegistrationStepData
import com.cehpoint.netwin.domain.repository.TournamentRepository
import com.cehpoint.netwin.domain.repository.UserRepository
import com.cehpoint.netwin.domain.repository.WalletRepository
import com.cehpoint.netwin.presentation.events.RegistrationFlowEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Test class specifically focused on error propagation in the ViewModel
 * Tests how validation errors flow from domain model through ViewModel to UI state
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegistrationErrorPropagationTest {

    private lateinit var tournamentRepository: TournamentRepository
    private lateinit var userRepository: UserRepository
    private lateinit var walletRepository: WalletRepository
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var networkStateMonitor: com.cehpoint.netwin.utils.NetworkStateMonitor
    private lateinit var viewModel: TournamentViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Initialize mocks
        tournamentRepository = mock()
        userRepository = mock()
        walletRepository = mock()
        dataStoreManager = mock()
        savedStateHandle = SavedStateHandle()
        firebaseManager = mock()
        firebaseAuth = mock()
        firebaseUser = mock()
        networkStateMonitor = mock()

        // Setup mocks
        whenever(firebaseManager.auth).thenReturn(firebaseAuth)
        whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
        whenever(firebaseUser.uid).thenReturn("test_user")
        whenever(walletRepository.getWalletBalance(any())).thenReturn(flowOf(100.0))
        whenever(dataStoreManager.userName).thenReturn(flowOf("TestUser"))
        whenever(networkStateMonitor.observeNetworkState()).thenReturn(flowOf(true))
        whenever(networkStateMonitor.isNetworkAvailable()).thenReturn(true)

        viewModel = TournamentViewModel(
            repository = tournamentRepository,
            firebaseManager = firebaseManager,
            userRepository = userRepository,
            walletRepository = walletRepository,
            dataStoreManager = dataStoreManager,
            savedStateHandle = savedStateHandle,
            networkStateMonitor = networkStateMonitor
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testErrorPropagation_InvalidInGameId() = runTest {
        // Set up invalid data
        viewModel.onRegistrationEvent(
            RegistrationFlowEvent.UpdateData {
                copy(
                    inGameId = "", // Invalid - empty
                    teamName = "ValidTeam",
                    tournamentId = "valid-tournament"
                )
            }
        )

        // Navigate to DETAILS step where validation occurs
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Next) // To PAYMENT
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Next) // To DETAILS

        // Try to advance (should fail validation)
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Next)

        // Verify error propagation
        val uiState = viewModel.registrationUiState.value
        assertEquals("Should remain on DETAILS step", RegistrationStep.DETAILS, uiState.step)
        assertEquals("Should show validation error", "In-game ID is required", uiState.error)
        assertFalse("Should not be loading", uiState.loading)
    }

    @Test
    fun testErrorPropagation_InvalidPaymentMethod() = runTest {
        // Set up invalid payment data
        viewModel.onRegistrationEvent(
            RegistrationFlowEvent.UpdateData {
                copy(paymentMethod = "bitcoin") // Invalid payment method
            }
        )

        // Navigate to PAYMENT step
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Next) // To PAYMENT

        // Try to advance (should fail validation)
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Next)

        // Verify error propagation
        val uiState = viewModel.registrationUiState.value
        assertEquals("Should remain on PAYMENT step", RegistrationStep.PAYMENT, uiState.step)
        assertEquals("Should show payment validation error", "Invalid payment method", uiState.error)
    }

    @Test
    fun testErrorPropagation_TermsNotAccepted() = runTest {
        // Set up valid data but terms not accepted
        viewModel.onRegistrationEvent(
            RegistrationFlowEvent.UpdateData {
                copy(
                    inGameId = "ValidPlayer123",
                    teamName = "ValidTeam",
                    paymentMethod = "wallet",
                    termsAccepted = false, // Invalid
                    tournamentId = "valid-tournament"
                )
            }
        )

        // Navigate to CONFIRM step
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Next) // To PAYMENT
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Next) // To DETAILS
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Next) // To CONFIRM

        // Try to submit (should fail validation)
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Submit)

        // Verify error propagation
        val uiState = viewModel.registrationUiState.value
        assertEquals("Should remain on CONFIRM step", RegistrationStep.CONFIRM, uiState.step)
        assertEquals("Should show terms validation error", "You must accept the terms and conditions", uiState.error)
    }

    @Test
    fun testErrorClearing_OnDataUpdate() = runTest {
        // First create an error condition
        viewModel.onRegistrationEvent(
            RegistrationFlowEvent.UpdateData {
                copy(inGameId = "") // Invalid
            }
        )

        // Navigate to trigger validation error
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Next) // To PAYMENT
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Next) // To DETAILS
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Next) // Should fail and set error

        // Verify error is present
        assertNotNull("Error should be present", viewModel.registrationUiState.value.error)

        // Update data to fix the error
        viewModel.onRegistrationEvent(
            RegistrationFlowEvent.UpdateData {
                copy(inGameId = "ValidPlayer123") // Fix the error
            }
        )

        // Verify error is cleared
        assertNull("Error should be cleared after data update", viewModel.registrationUiState.value.error)
    }

    @Test
    fun testErrorClearing_OnStepNavigation() = runTest {
        // Create an error condition
        viewModel.onRegistrationEvent(
            RegistrationFlowEvent.UpdateData {
                copy(paymentMethod = "invalid") // Invalid payment method
            }
        )

        // Navigate to payment step and trigger error
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Next) // To PAYMENT
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Next) // Should fail and set error

        // Verify error is present
        assertNotNull("Error should be present", viewModel.registrationUiState.value.error)

        // Navigate back
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Previous)

        // Verify error is cleared on navigation
        assertNull("Error should be cleared on navigation", viewModel.registrationUiState.value.error)
    }

    @Test
    fun testCascadingValidation_ReviewStep() = runTest {
        // Set up data with error in DETAILS step
        viewModel.onRegistrationEvent(
            RegistrationFlowEvent.UpdateData {
                copy(
                    inGameId = "ab", // Invalid - too short
                    teamName = "ValidTeam",
                    paymentMethod = "wallet",
                    tournamentId = "valid-tournament"
                )
            }
        )

        // Navigate to REVIEW step (should fail due to invalid DETAILS)
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Next) // To PAYMENT

        // At REVIEW step, try to advance (should fail due to cascading validation)
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Next) // Should fail

        // Verify cascading error propagation
        val uiState = viewModel.registrationUiState.value
        assertEquals("Should remain on PAYMENT step due to cascading validation", RegistrationStep.PAYMENT, uiState.step)
        assertEquals("Should show cascaded error from DETAILS", "In-game ID must be at least 3 characters", uiState.error)
    }

    @Test
    fun testValidationBypass_PreviousNavigation() = runTest {
        // Set up invalid data
        viewModel.onRegistrationEvent(
            RegistrationFlowEvent.UpdateData {
                copy(inGameId = "") // Invalid
            }
        )

        // Navigate forward to DETAILS step
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Next) // To PAYMENT
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Next) // To DETAILS

        // Navigate backward (should work regardless of validation)
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Previous)

        // Verify backward navigation works
        val uiState = viewModel.registrationUiState.value
        assertEquals("Should move back to PAYMENT step", RegistrationStep.PAYMENT, uiState.step)
        assertNull("Error should be cleared on backward navigation", uiState.error)
    }

    @Test
    fun testComplexValidationScenario() = runTest {
        // Test multiple validation errors in sequence
        
        // Start with completely invalid data
        viewModel.onRegistrationEvent(
            RegistrationFlowEvent.UpdateData {
                copy(
                    inGameId = "", // Invalid
                    teamName = "", // Invalid
                    paymentMethod = "invalid", // Invalid
                    termsAccepted = false, // Invalid
                    tournamentId = "" // Invalid
                )
            }
        )

        // Try to advance through each step and verify appropriate errors
        
        // Try to go to PAYMENT (should fail on current step - REVIEW)
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Next)
        var uiState = viewModel.registrationUiState.value
        assertNotNull("Should have validation error", uiState.error)
        assertTrue("Error should be about missing details", 
            uiState.error!!.contains("required") || uiState.error!!.contains("characters"))

        // Fix DETAILS issues but keep PAYMENT invalid
        viewModel.onRegistrationEvent(
            RegistrationFlowEvent.UpdateData {
                copy(
                    inGameId = "ValidPlayer123",
                    teamName = "ValidTeam",
                    tournamentId = "valid-tournament"
                    // paymentMethod still invalid
                )
            }
        )

        // Try to advance again
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Next) // To PAYMENT
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Next) // Should fail on invalid payment
        
        uiState = viewModel.registrationUiState.value
        assertEquals("Should remain on PAYMENT step", RegistrationStep.PAYMENT, uiState.step)
        assertEquals("Should show payment error", "Invalid payment method", uiState.error)
    }

    @Test 
    fun testUiStateConsistency() = runTest {
        // Verify UI state remains consistent during error conditions
        
        viewModel.onRegistrationEvent(
            RegistrationFlowEvent.UpdateData {
                copy(
                    inGameId = "ValidPlayer",
                    teamName = "ValidTeam",
                    paymentMethod = "wallet",
                    tournamentId = "valid-id"
                )
            }
        )

        // Navigate through all steps and verify consistency
        val steps = listOf(RegistrationStep.REVIEW, RegistrationStep.PAYMENT, RegistrationStep.DETAILS, RegistrationStep.CONFIRM)
        
        for (i in 1 until steps.size) {
            viewModel.onRegistrationEvent(RegistrationFlowEvent.Next)
            val uiState = viewModel.registrationUiState.value
            
            assertEquals("Step should match expected", steps[i], uiState.step)
            assertEquals("Data should be consistent", "ValidPlayer", uiState.data.inGameId)
            assertEquals("Data should be consistent", "ValidTeam", uiState.data.teamName)
            assertNull("No error should be present with valid data", uiState.error)
        }
    }

    @Test
    fun testErrorPersistence_AcrossSteps() = runTest {
        // Verify that errors don't persist inappropriately across steps
        
        // Create error on PAYMENT step
        viewModel.onRegistrationEvent(
            RegistrationFlowEvent.UpdateData {
                copy(paymentMethod = "invalid")
            }
        )
        
        // Navigate to PAYMENT step and trigger error
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Next)
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Next)
        
        // Verify error exists
        assertNotNull("Error should exist", viewModel.registrationUiState.value.error)
        
        // Fix payment method and navigate forward
        viewModel.onRegistrationEvent(
            RegistrationFlowEvent.UpdateData {
                copy(paymentMethod = "wallet")
            }
        )
        
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Next) // To DETAILS
        
        // Error should be cleared
        assertNull("Error should be cleared after fixing and navigating", viewModel.registrationUiState.value.error)
    }
}
