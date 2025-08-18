package com.cehpoint.netwin.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import com.cehpoint.netwin.data.local.DataStoreManager
import com.cehpoint.netwin.data.remote.FirebaseManager
import com.cehpoint.netwin.domain.model.Tournament
import com.cehpoint.netwin.domain.model.TournamentStatus
import com.cehpoint.netwin.domain.repository.TournamentRepository
import com.cehpoint.netwin.domain.repository.UserRepository
import com.cehpoint.netwin.domain.repository.WalletRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class TournamentSelectedTournamentTest {

    private lateinit var viewModel: TournamentViewModel
    private lateinit var tournamentRepository: TournamentRepository
    private lateinit var userRepository: UserRepository
    private lateinit var walletRepository: WalletRepository
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var savedStateHandle: SavedStateHandle
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
        firebaseManager = mock()
        savedStateHandle = SavedStateHandle()
        firebaseAuth = mock()
        firebaseUser = mock()
        networkStateMonitor = mock()

        whenever(firebaseManager.auth).thenReturn(firebaseAuth)
        whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
        whenever(firebaseUser.uid).thenReturn("test_user")
        whenever(walletRepository.getWalletBalance("test_user")).thenReturn(flowOf(100.0))
        // Mock DataStoreManager as interface or create a test double
        // For now, we'll stub the necessary methods
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
    fun `getTournamentById - fetches tournament successfully - only called once`() = runTest {
        val tournamentId = "tournament123"
        val expectedTournament = createTestTournament(tournamentId)
        whenever(tournamentRepository.getTournamentById(tournamentId)).thenReturn(expectedTournament)

        // Initial loading state verification
        assertTrue(viewModel.selectedTournament.value == null)
        assertFalse(viewModel.isLoadingDetails.value)
        assertNull(viewModel.detailsError.value)

        // Trigger fetch
        viewModel.getTournamentById(tournamentId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify repository called only once
        verify(tournamentRepository, times(1)).getTournamentById(tournamentId)

        // Verify final state
        assertEquals(expectedTournament, viewModel.selectedTournament.value)
        assertFalse(viewModel.isLoadingDetails.value)
        assertNull(viewModel.detailsError.value)
    }

    @Test
    fun `getTournamentById - multiple rapid calls - only fetches once per call`() = runTest {
        val tournamentId = "tournament123"
        val expectedTournament = createTestTournament(tournamentId)
        whenever(tournamentRepository.getTournamentById(tournamentId)).thenReturn(expectedTournament)

        // Make multiple rapid calls
        viewModel.getTournamentById(tournamentId)
        viewModel.getTournamentById(tournamentId)
        viewModel.getTournamentById(tournamentId)
        
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify repository called for each request (as per current implementation)
        verify(tournamentRepository, times(3)).getTournamentById(tournamentId)
        assertEquals(expectedTournament, viewModel.selectedTournament.value)
    }

    @Test
    fun `getTournamentById - handles loading states correctly`() = runTest {
        val tournamentId = "tournament123"
        val expectedTournament = createTestTournament(tournamentId)
        
        // Mock with delay to verify loading state
        whenever(tournamentRepository.getTournamentById(tournamentId)).thenAnswer {
            kotlinx.coroutines.runBlocking {
                delay(100) // Simulate network delay
            }
            expectedTournament
        }

        // Initial state
        assertFalse(viewModel.isLoadingDetails.value)
        assertNull(viewModel.selectedTournament.value)
        assertNull(viewModel.detailsError.value)

        // Trigger fetch
        viewModel.getTournamentById(tournamentId)
        
        // Advance to start of loading
        testDispatcher.scheduler.advanceTimeBy(1)
        
        // Verify loading state
        assertTrue(viewModel.isLoadingDetails.value)
        assertNull(viewModel.selectedTournament.value)
        assertNull(viewModel.detailsError.value)

        // Complete the operation
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify final state
        assertFalse(viewModel.isLoadingDetails.value)
        assertEquals(expectedTournament, viewModel.selectedTournament.value)
        assertNull(viewModel.detailsError.value)
    }

    @Test
    fun `getTournamentById - handles error states correctly`() = runTest {
        val tournamentId = "tournament123"
        val errorMessage = "Network error"
        whenever(tournamentRepository.getTournamentById(tournamentId)).thenThrow(RuntimeException(errorMessage))

        // Initial state
        assertFalse(viewModel.isLoadingDetails.value)
        assertNull(viewModel.selectedTournament.value)
        assertNull(viewModel.detailsError.value)

        // Trigger fetch
        viewModel.getTournamentById(tournamentId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify error state
        assertFalse(viewModel.isLoadingDetails.value)
        assertNull(viewModel.selectedTournament.value)
        assertEquals("Error loading tournament details: $errorMessage", viewModel.detailsError.value)
    }

    @Test
    fun `getTournamentById - handles null tournament (not found)`() = runTest {
        val tournamentId = "tournament123"
        whenever(tournamentRepository.getTournamentById(tournamentId)).thenReturn(null)

        // Trigger fetch
        viewModel.getTournamentById(tournamentId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify not found state
        assertFalse(viewModel.isLoadingDetails.value)
        assertNull(viewModel.selectedTournament.value)
        assertEquals("Tournament not found", viewModel.detailsError.value)
    }

    @Test
    fun `getTournamentById - slow network simulation - handles loading timeout gracefully`() = runTest {
        val tournamentId = "tournament123"
        val expectedTournament = createTestTournament(tournamentId)
        
        // Mock with long delay to simulate slow network
        whenever(tournamentRepository.getTournamentById(tournamentId)).thenAnswer {
            kotlinx.coroutines.runBlocking {
                delay(5000) // Simulate very slow network
            }
            expectedTournament
        }

        // Trigger fetch
        viewModel.getTournamentById(tournamentId)
        
        // Verify loading state persists during slow network
        testDispatcher.scheduler.advanceTimeBy(1000)
        assertTrue(viewModel.isLoadingDetails.value)
        assertNull(viewModel.selectedTournament.value)
        
        testDispatcher.scheduler.advanceTimeBy(2000)
        assertTrue(viewModel.isLoadingDetails.value)
        assertNull(viewModel.selectedTournament.value)
        
        // Complete the slow operation
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify successful completion
        assertFalse(viewModel.isLoadingDetails.value)
        assertEquals(expectedTournament, viewModel.selectedTournament.value)
        assertNull(viewModel.detailsError.value)
    }

    @Test
    fun `clearSelectedTournament - resets all tournament detail states`() = runTest {
        val tournamentId = "tournament123"
        val expectedTournament = createTestTournament(tournamentId)
        whenever(tournamentRepository.getTournamentById(tournamentId)).thenReturn(expectedTournament)

        // First populate the tournament
        viewModel.getTournamentById(tournamentId)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(expectedTournament, viewModel.selectedTournament.value)

        // Clear the tournament
        viewModel.clearSelectedTournament()

        // Verify all states are cleared
        assertNull(viewModel.selectedTournament.value)
        assertNull(viewModel.detailsError.value)
    }

    @Test
    fun `getTournamentById - state reset before new fetch`() = runTest {
        val tournamentId = "tournament123"
        val expectedTournament = createTestTournament(tournamentId)
        whenever(tournamentRepository.getTournamentById(tournamentId)).thenReturn(expectedTournament)

        // First, simulate an error state
        whenever(tournamentRepository.getTournamentById("bad_id")).thenReturn(null)
        viewModel.getTournamentById("bad_id")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify error state
        assertEquals("Tournament not found", viewModel.detailsError.value)

        // Now fetch a valid tournament - states should reset
        viewModel.getTournamentById(tournamentId)
        
        // Verify states reset immediately when new fetch starts
        testDispatcher.scheduler.advanceTimeBy(1)
        assertTrue(viewModel.isLoadingDetails.value)
        assertNull(viewModel.selectedTournament.value)
        assertNull(viewModel.detailsError.value)
        
        // Complete the operation
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(expectedTournament, viewModel.selectedTournament.value)
    }

    private fun createTestTournament(id: String = "test123") = Tournament(
        id = id,
        name = "Test Tournament",
        description = "Test Description",
        status = "upcoming",
        entryFee = 10.0,
        prizePool = 100.0,
        maxTeams = 16,
        registeredTeams = 5,
        startTime = System.currentTimeMillis() + 3600000, // 1 hour from now
        gameType = "Battle Royale",
        matchType = "SQUAD",
        map = "Sanhok",
        rules = "Standard tournament rules apply",
        bannerImage = "https://example.com/tournament.jpg"
    )
}
