package com.cehpoint.netwin

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cehpoint.netwin.data.local.DataStoreManager
import com.cehpoint.netwin.data.remote.FirebaseManager
import com.cehpoint.netwin.domain.model.RegistrationStep
import com.cehpoint.netwin.domain.model.RegistrationStepData
import com.cehpoint.netwin.domain.repository.TournamentRepository
import com.cehpoint.netwin.domain.repository.UserRepository
import com.cehpoint.netwin.domain.repository.WalletRepository
import com.cehpoint.netwin.presentation.events.RegistrationFlowEvent
import com.cehpoint.netwin.presentation.viewmodels.TournamentViewModel
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for TournamentViewModel state persistence using SavedStateHandle.
 * These tests simulate process death and recreation to ensure registration flow state is preserved.
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class TournamentViewModelPersistenceTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    // Mock dependencies
    private lateinit var mockRepository: TournamentRepository
    private lateinit var mockFirebaseManager: FirebaseManager
    private lateinit var mockUserRepository: UserRepository
    private lateinit var mockWalletRepository: WalletRepository
    private lateinit var mockDataStoreManager: DataStoreManager

    @Before
    fun setUp() {
        hiltRule.inject()
        mockRepository = mockk()
        mockFirebaseManager = mockk()
        mockUserRepository = mockk()
        mockWalletRepository = mockk()
        mockDataStoreManager = mockk()
    }

    @Test
    fun testCurrentStepPersistenceAfterProcessDeath() = runTest {
        // Create a Bundle to simulate saved state from the system
        val savedStateBundle = Bundle()
        
        // First instance: Create ViewModel and advance to PAYMENT step
        val firstSavedStateHandle = SavedStateHandle(savedStateBundle)
        val firstViewModel = TournamentViewModel(
            repository = mockRepository,
            firebaseManager = mockFirebaseManager,
            userRepository = mockUserRepository,
            walletRepository = mockWalletRepository,
            dataStoreManager = mockDataStoreManager,
            savedStateHandle = firstSavedStateHandle
        )
        
        // Initial state should be REVIEW
        assertThat(firstViewModel.currentStep.value).isEqualTo(RegistrationStep.REVIEW)
        
        // Advance to PAYMENT step
        firstViewModel.nextStep()
        assertThat(firstViewModel.currentStep.value).isEqualTo(RegistrationStep.PAYMENT)
        
        // Simulate the system saving the state
        firstSavedStateHandle.setSavedStateProvider("current_step") {
            Bundle().apply {
                putSerializable("current_step", firstViewModel.currentStep.value)
            }
        }
        
        // Second instance: Create new ViewModel with the saved state (simulating process recreation)
        val secondSavedStateHandle = SavedStateHandle().apply {
            set("current_step", RegistrationStep.PAYMENT)
        }
        
        val secondViewModel = TournamentViewModel(
            repository = mockRepository,
            firebaseManager = mockFirebaseManager,
            userRepository = mockUserRepository,
            walletRepository = mockWalletRepository,
            dataStoreManager = mockDataStoreManager,
            savedStateHandle = secondSavedStateHandle
        )
        
        // Verify that the step was restored
        assertThat(secondViewModel.currentStep.value).isEqualTo(RegistrationStep.PAYMENT)
    }
    
    @Test
    fun testStepDataPersistenceAfterProcessDeath() = runTest {
        // Create test data
        val testStepData = RegistrationStepData(
            inGameId = "TestPlayer123",
            teamName = "TestTeam",
            paymentMethod = "wallet",
            termsAccepted = true,
            tournamentId = "tournament-123"
        )
        
        // First instance: Create ViewModel and update step data
        val firstSavedStateHandle = SavedStateHandle()
        val firstViewModel = TournamentViewModel(
            repository = mockRepository,
            firebaseManager = mockFirebaseManager,
            userRepository = mockUserRepository,
            walletRepository = mockWalletRepository,
            dataStoreManager = mockDataStoreManager,
            savedStateHandle = firstSavedStateHandle
        )
        
        // Update step data using the event handler
        firstViewModel.onRegistrationEvent(
            RegistrationFlowEvent.UpdateData { testStepData }
        )
        
        // Verify data was updated
        assertThat(firstViewModel.stepData.value.inGameId).isEqualTo("TestPlayer123")
        assertThat(firstViewModel.stepData.value.teamName).isEqualTo("TestTeam")
        assertThat(firstViewModel.stepData.value.tournamentId).isEqualTo("tournament-123")
        
        // Second instance: Create new ViewModel with the saved state
        val secondSavedStateHandle = SavedStateHandle().apply {
            set("step_data", testStepData)
        }
        
        val secondViewModel = TournamentViewModel(
            repository = mockRepository,
            firebaseManager = mockFirebaseManager,
            userRepository = mockUserRepository,
            walletRepository = mockWalletRepository,
            dataStoreManager = mockDataStoreManager,
            savedStateHandle = secondSavedStateHandle
        )
        
        // Verify that the step data was restored
        assertThat(secondViewModel.stepData.value.inGameId).isEqualTo("TestPlayer123")
        assertThat(secondViewModel.stepData.value.teamName).isEqualTo("TestTeam")
        assertThat(secondViewModel.stepData.value.tournamentId).isEqualTo("tournament-123")
        assertThat(secondViewModel.stepData.value.termsAccepted).isTrue()
    }
    
    @Test
    fun testCompleteRegistrationFlowStateAfterProcessDeath() = runTest {
        // Create test data
        val testStepData = RegistrationStepData(
            inGameId = "PlayerX",
            teamName = "TeamAwesome",
            paymentMethod = "wallet",
            termsAccepted = false,
            tournamentId = "epic-tournament"
        )
        
        // First instance: Create ViewModel and simulate user interaction
        val firstSavedStateHandle = SavedStateHandle()
        val firstViewModel = TournamentViewModel(
            repository = mockRepository,
            firebaseManager = mockFirebaseManager,
            userRepository = mockUserRepository,
            walletRepository = mockWalletRepository,
            dataStoreManager = mockDataStoreManager,
            savedStateHandle = firstSavedStateHandle
        )
        
        // Simulate user progressing through registration
        firstViewModel.onRegistrationEvent(RegistrationFlowEvent.UpdateData { testStepData })
        firstViewModel.onRegistrationEvent(RegistrationFlowEvent.Next)
        firstViewModel.onRegistrationEvent(RegistrationFlowEvent.Next)
        
        // Should be at DETAILS step
        assertThat(firstViewModel.currentStep.value).isEqualTo(RegistrationStep.DETAILS)
        
        // Second instance: Simulate process death and recreation
        val secondSavedStateHandle = SavedStateHandle().apply {
            set("current_step", RegistrationStep.DETAILS)
            set("step_data", testStepData)
        }
        
        val secondViewModel = TournamentViewModel(
            repository = mockRepository,
            firebaseManager = mockFirebaseManager,
            userRepository = mockUserRepository,
            walletRepository = mockWalletRepository,
            dataStoreManager = mockDataStoreManager,
            savedStateHandle = secondSavedStateHandle
        )
        
        // Verify complete state restoration
        assertThat(secondViewModel.currentStep.value).isEqualTo(RegistrationStep.DETAILS)
        assertThat(secondViewModel.stepData.value.inGameId).isEqualTo("PlayerX")
        assertThat(secondViewModel.stepData.value.teamName).isEqualTo("TeamAwesome")
        assertThat(secondViewModel.stepData.value.tournamentId).isEqualTo("epic-tournament")
        
        // Continue with the flow to verify it still works after restoration
        secondViewModel.onRegistrationEvent(
            RegistrationFlowEvent.UpdateData { it.copy(termsAccepted = true) }
        )
        secondViewModel.onRegistrationEvent(RegistrationFlowEvent.Next)
        
        // Should now be at CONFIRM step
        assertThat(secondViewModel.currentStep.value).isEqualTo(RegistrationStep.CONFIRM)
        assertThat(secondViewModel.stepData.value.termsAccepted).isTrue()
    }
    
    @Test
    fun testDefaultStateWhenNoSavedStateExists() = runTest {
        // Create ViewModel with empty SavedStateHandle (no saved state)
        val emptySavedStateHandle = SavedStateHandle()
        val viewModel = TournamentViewModel(
            repository = mockRepository,
            firebaseManager = mockFirebaseManager,
            userRepository = mockUserRepository,
            walletRepository = mockWalletRepository,
            dataStoreManager = mockDataStoreManager,
            savedStateHandle = emptySavedStateHandle
        )
        
        // Verify default state
        assertThat(viewModel.currentStep.value).isEqualTo(RegistrationStep.REVIEW)
        assertThat(viewModel.stepData.value).isEqualTo(RegistrationStepData())
        assertThat(viewModel.stepData.value.inGameId).isEmpty()
        assertThat(viewModel.stepData.value.teamName).isEmpty()
        assertThat(viewModel.stepData.value.tournamentId).isEmpty()
        assertThat(viewModel.stepData.value.termsAccepted).isFalse()
    }
}
