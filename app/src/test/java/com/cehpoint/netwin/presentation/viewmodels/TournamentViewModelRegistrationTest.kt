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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class TournamentViewModelRegistrationTest {

    private lateinit var viewModel: TournamentViewModel
    private lateinit var tournamentRepository: TournamentRepository
    private lateinit var userRepository: UserRepository
    private lateinit var walletRepository: WalletRepository
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var networkStateMonitor: com.cehpoint.netwin.utils.NetworkStateMonitor

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
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

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updateStepData modifies data without side effects`() = runTest {
        val initialData = viewModel.stepData.value
        val newInGameId = "TestUser123"

        viewModel.onRegistrationEvent(RegistrationFlowEvent.UpdateData { copy(inGameId = newInGameId) })

        val updatedData = viewModel.stepData.value
        assertEquals(newInGameId, updatedData.inGameId)
        assertEquals(initialData.teamName, updatedData.teamName) // Ensure other fields are unchanged
        assertNull(viewModel.currentError.value) // No error should be set
    }

    @Test
    fun `next validationFails blocksNavigation`() = runTest {
        // Given we update step data to be invalid and attempt to navigate
        viewModel.onRegistrationEvent(RegistrationFlowEvent.UpdateData { 
            copy(
                inGameId = "", // Invalid - blank
                teamName = "Valid Team",
                tournamentId = "test123"
            )
        })
        
        // Navigate to DETAILS step where validation will happen
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Next) // To PAYMENT
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Next) // To DETAILS
        
        // Attempt to go to next step which should be blocked
        val currentStepBefore = viewModel.currentStep.value
        viewModel.onRegistrationEvent(RegistrationFlowEvent.Next)

        // Then navigation should be blocked and an error message shown
        assertEquals(currentStepBefore, viewModel.currentStep.value) // Should not advance
        assertNotNull(viewModel.currentError.value)
    }

    @Test
    fun `submit validationSuccess completesRegistration`() = runTest {
        // Given all steps are valid
        viewModel.onRegistrationEvent(RegistrationFlowEvent.UpdateData {
            copy(
                inGameId = "TestUser123",
                teamName = "MyTeam",
                paymentMethod = "wallet",
                termsAccepted = true,
                tournamentId = "tourney123"
            )
        })
        whenever(tournamentRepository.registerForTournament(any(), any(), any(), any(), any())).thenReturn(Result.success(Unit))

        viewModel.onRegistrationEvent(RegistrationFlowEvent.Submit)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then registration should be successful
        assertNull(viewModel.registrationError.value)
        verify(tournamentRepository).registerForTournament(
            tournamentId = "tourney123",
            userId = "test_user",
            displayName = "TestUser",
            teamName = "MyTeam",
            inGameId = "TestUser123"
        )
    }

    @Test
    fun `submit repositoryError propagatesError`() = runTest {
        // Given all steps are valid, but the repository fails
        viewModel.onRegistrationEvent(RegistrationFlowEvent.UpdateData {
            copy(
                inGameId = "TestUser123",
                teamName = "MyTeam",
                paymentMethod = "wallet",
                termsAccepted = true,
                tournamentId = "tourney123"
            )
        })
        val errorMessage = "Network error"
        whenever(tournamentRepository.registerForTournament(any(), any(), any(), any(), any())).thenReturn(Result.failure(Exception(errorMessage)))

        viewModel.onRegistrationEvent(RegistrationFlowEvent.Submit)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then the error should be propagated to the UI state
        assertEquals(errorMessage, viewModel.registrationState.value?.exceptionOrNull()?.message)
        verify(tournamentRepository).registerForTournament(
            tournamentId = "tourney123",
            userId = "test_user",
            displayName = "TestUser",
            teamName = "MyTeam",
            inGameId = "TestUser123"
        )
    }
}
