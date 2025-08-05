package com.cehpoint.netwin.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.cehpoint.netwin.data.local.DataStoreManager
import com.cehpoint.netwin.data.remote.FirebaseManager
import com.cehpoint.netwin.domain.model.Tournament
import com.cehpoint.netwin.domain.model.TournamentStatus
import com.cehpoint.netwin.domain.repository.TournamentRepository
import com.cehpoint.netwin.domain.repository.UserRepository
import com.cehpoint.netwin.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

data class TournamentState(
    val tournaments: List<Tournament> = emptyList(),
    val selectedFilter: TournamentFilter = TournamentFilter.ALL,
    val isLoading: Boolean = false,
    val error: String? = null,
    val countdowns: Map<String, String> = emptyMap() // NEW: tournamentId -> remainingTime
)

// Add this data class
data class RegistrationStepData(
    val inGameId: String = "",
    val teamName: String = "",
    val paymentMethod: String = "wallet",
    val termsAccepted: Boolean = false,
    val tournamentId: String = ""
)

enum class TournamentFilter {
    ALL, UPCOMING, ONGOING, COMPLETED
}

sealed class TournamentEvent {
    object LoadTournaments : TournamentEvent()
    data class FilterTournaments(val filter: TournamentFilter) : TournamentEvent()
    data class CreateTournament(val tournament: Tournament) : TournamentEvent()
    data class RefreshTournaments(val force: Boolean = false) : TournamentEvent()
}

@HiltViewModel
class TournamentViewModel @Inject constructor(
    private val repository: TournamentRepository,
    private val firebaseManager: FirebaseManager,
    private val userRepository: UserRepository,
    private val walletRepository: WalletRepository,
    private val dataStoreManager: DataStoreManager
) : BaseViewModel<TournamentState, TournamentEvent>() {

    // Tournament Details State
    private val _selectedTournament = MutableStateFlow<Tournament?>(null)
    val selectedTournament: StateFlow<Tournament?> = _selectedTournament.asStateFlow()

    private val _isLoadingDetails = MutableStateFlow(false)
    val isLoadingDetails: StateFlow<Boolean> = _isLoadingDetails.asStateFlow()

    private val _detailsError = MutableStateFlow<String?>(null)
    val detailsError: StateFlow<String?> = _detailsError.asStateFlow()

    // Registration State
    private val _registrationState = MutableStateFlow<Result<Unit>?>(null)
    val registrationState: StateFlow<Result<Unit>?> = _registrationState.asStateFlow()

    // Registration Status State
    private val _registrationStatus = MutableStateFlow<Boolean?>(null)
    val registrationStatus: StateFlow<Boolean?> = _registrationStatus.asStateFlow()

    private val _showKycRequiredDialog = MutableStateFlow(false)
    val showKycRequiredDialog: StateFlow<Boolean> = _showKycRequiredDialog.asStateFlow()

    private val _walletBalance = MutableStateFlow(0.0)
    val walletBalance: StateFlow<Double> = _walletBalance.asStateFlow()

    private val _userName = MutableStateFlow("Gamer")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _lastProcessedTournamentId = MutableStateFlow<String?>(null)
    val lastProcessedTournamentId: StateFlow<String?> = _lastProcessedTournamentId.asStateFlow()

    // NEW: Dedicated refresh state
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError: StateFlow<String?> = _refreshError.asStateFlow()

    private val _refreshSuccess = MutableStateFlow(false)
    val refreshSuccess: StateFlow<Boolean> = _refreshSuccess.asStateFlow()

    // Debounce mechanism for refresh
    private var lastRefreshTime = 0L
    private val refreshDebounceTime = 2000L // 2 seconds

    // NEW: Registration loading and error states
    private val _isRegistering = MutableStateFlow(false)
    val isRegistering: StateFlow<Boolean> = _isRegistering.asStateFlow()

    private val _registrationError = MutableStateFlow<String?>(null)
    val registrationError: StateFlow<String?> = _registrationError.asStateFlow()

    // Registration Flow State Management
    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _stepData = MutableStateFlow(RegistrationStepData())
    val stepData: StateFlow<RegistrationStepData> = _stepData.asStateFlow()

    private val _stepError = MutableStateFlow<String?>(null)
    val stepError: StateFlow<String?> = _stepError.asStateFlow()


    // Add these functions
    fun updateStepData(data: RegistrationStepData) {
        _stepData.value = data
    }

    fun nextStep() {
        if (_currentStep.value < 4) {
            _currentStep.value += 1
            _stepError.value = null
        }
    }

    fun previousStep() {
        if (_currentStep.value > 1) {
            _currentStep.value -= 1
            _stepError.value = null
        }
    }

    fun setStepError(error: String?) {
        _stepError.value = error
    }

    fun resetRegistrationFlow() {
        _currentStep.value = 1
        _stepData.value = RegistrationStepData()
        _stepError.value = null
    }

    // NEW: Countdown job
    private var countdownJob: Job? = null


    fun clearLastProcessedTournamentId() {
        _lastProcessedTournamentId.value = null
    }

    fun clearRefreshSuccess() {
        _refreshSuccess.value = false
    }

    fun clearRefreshError() {
        _refreshError.value = null
    }

    fun canRefresh(): Boolean {
        val currentTime = System.currentTimeMillis()
        return currentTime - lastRefreshTime > refreshDebounceTime
    }

    fun clearRegistrationError() {
        _registrationError.value = null
    }

    fun clearRegistrationLoading() {
        _isRegistering.value = false
    }


    init {
        setState(TournamentState())
        loadTournaments()
        startCountdownTimer()
        val userId = firebaseManager.auth.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch {
                walletRepository.getWalletBalance(userId).collect { balance ->
                    _walletBalance.value = balance
                }
            }
            viewModelScope.launch {
                try {
                    // Get user data from DataStore (pre-fetched during splash screen)
                    val userNameFromDataStore = dataStoreManager.userName.first()

                    if (userNameFromDataStore.isNotBlank()) {
                        Log.d("TournamentViewModel", "Using pre-fetched username from DataStore: $userNameFromDataStore")
                        _userName.value = userNameFromDataStore
                    } else {
                        // Fallback to Firestore if DataStore doesn't have the data
                        Log.d("TournamentViewModel", "DataStore empty, fetching from Firestore")
                val userResult = userRepository.getUser(userId)
                val user = userResult.getOrNull()
                _userName.value = user?.username?.takeIf { it.isNotBlank() }
                    ?: user?.displayName?.takeIf { it.isNotBlank() }
                    ?: "Gamer"
                    }
                } catch (e: Exception) {
                    Log.e("TournamentViewModel", "Error fetching user data", e)
                    _userName.value = "Gamer"
                }
            }
        }
    }

    private fun startCountdownTimer() {
        countdownJob?.cancel() // Cancel existing timer if any
        countdownJob = viewModelScope.launch {
            while (true) {
                updateAllCountdowns()
                delay(1000) // Update every second
            }
        }
    }

    private fun updateAllCountdowns() {
        val currentState = state.value ?: return
        val tournaments = currentState.tournaments
        val newCountdowns = mutableMapOf<String, String>()

        tournaments.forEach { tournament ->
            if (tournament.computedStatus in listOf(TournamentStatus.UPCOMING, TournamentStatus.ONGOING)) {
                val countdown = calculateRemainingTime(tournament)
                if (countdown.isNotEmpty()) {
                    newCountdowns[tournament.id] = countdown
                }
            }
        }

        // Only update state if countdowns changed (optimization)
        if (newCountdowns != currentState.countdowns) {
            setState(currentState.copy(countdowns = newCountdowns))
        }
    }

    private fun calculateRemainingTime(tournament: Tournament): String {
        val currentTime = System.currentTimeMillis()

        return when (tournament.computedStatus) {
            TournamentStatus.UPCOMING -> {
                val timeDiff = (tournament.startTime ?: 0L) - currentTime
                if (timeDiff <= 0) {
                    "Starting..."
                } else {
                    formatTimeRemaining(timeDiff)
                }
            }
            TournamentStatus.ONGOING -> {
                val timeDiff = (tournament.completedAt ?: 0L) - currentTime
                if (timeDiff <= 0) {
                    "Ending..."
                } else {
                    formatTimeRemaining(timeDiff)
                }
            }
            else -> ""
        }
    }

    private fun formatTimeRemaining(timeDiff: Long): String {
        val hours = timeDiff / (60 * 60 * 1000)
        val minutes = (timeDiff % (60 * 60 * 1000)) / (60 * 1000)
        val seconds = (timeDiff % (60 * 1000)) / 1000

        return when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
            minutes > 0 -> String.format("%02d:%02d", minutes, seconds)
            else -> String.format("%02ds", seconds)
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }

    override fun handleEvent(event: TournamentEvent) {
        when (event) {
            is TournamentEvent.LoadTournaments -> loadTournaments()
            is TournamentEvent.FilterTournaments -> filterTournaments(event.filter)
            is TournamentEvent.CreateTournament -> createTournament(event.tournament)
            is TournamentEvent.RefreshTournaments -> refreshTournaments(event.force)

        }
    }

    fun loadTournaments() {
        viewModelScope.launch {
            Log.d("TournamentViewModel", "Starting to load tournaments")
            super.setLoading(true)
            super.setError(null)
            try {
                repository.getTournaments().collect { tournaments ->
                    Log.d("TournamentViewModel", "Received ${tournaments.size} tournaments from repository")
                    tournaments.forEach { tournament ->
                        Log.d("TournamentViewModel", "Tournament: ${tournament.name}, ID: ${tournament.id}, Status: ${tournament.status}")
                    }
                    val currentState = state.value ?: TournamentState()
                    setState(currentState.copy(tournaments = tournaments))
                    Log.d("TournamentViewModel", "Updated state with ${tournaments.size} tournaments")
                }
            } catch (e: Exception) {
                Log.e("TournamentViewModel", "Error loading tournaments", e)
                super.setError(e.message ?: "Failed to load tournaments")
            } finally {
                super.setLoading(false)
                Log.d("TournamentViewModel", "Finished loading tournaments")
            }
        }
    }

    private fun filterTournaments(filter: TournamentFilter) {
        viewModelScope.launch {
            super.setLoading(true)
            super.setError(null)
            try {
                repository.getTournaments().collect { tournaments ->
                    val filteredTournaments = when (filter) {
                        TournamentFilter.ALL -> tournaments
                        TournamentFilter.UPCOMING -> tournaments.filter { it.computedStatus == TournamentStatus.UPCOMING }
                        TournamentFilter.ONGOING -> tournaments.filter { it.computedStatus == TournamentStatus.ONGOING }
                        TournamentFilter.COMPLETED -> tournaments.filter { it.computedStatus == TournamentStatus.COMPLETED }
                    }
                    val currentState = state.value ?: TournamentState()
                    setState(currentState.copy(
                        tournaments = filteredTournaments,
                        selectedFilter = filter
                    ))
                }
            } catch (e: Exception) {
                super.setError(e.message ?: "Failed to filter tournaments")
            } finally {
                super.setLoading(false)
            }
        }
    }

    private fun createTournament(tournament: Tournament) {
        viewModelScope.launch {
            super.setLoading(true)
            super.setError(null)
            try {
                repository.createTournament(tournament)
                    .onSuccess { loadTournaments() }
                    .onFailure { super.setError(it.message ?: "Failed to create tournament") }
            } catch (e: Exception) {
                super.setError(e.message ?: "Failed to create tournament")
            } finally {
                super.setLoading(false)
            }
        }
    }

    private fun refreshTournaments(force: Boolean) {
        // Check if we can refresh (debounce)
        if (!canRefresh()) {
            Log.d("TournamentViewModel", "Refresh debounced - too soon since last refresh")
            return
        }
        
        viewModelScope.launch {
            lastRefreshTime = System.currentTimeMillis()
            _isRefreshing.value = true
            _refreshError.value = null
            _refreshSuccess.value = false
            Log.d("TournamentViewModel", "Refresh state: isRefreshing = true")
            
            try {
                Log.d("TournamentViewModel", "Starting refresh tournaments")
                
                // Add minimum loading time for better UX
                val startTime = System.currentTimeMillis()
                val minimumLoadingTime = 1000L // 1 second minimum
                
                // Force a fresh fetch from Firestore - take only the first emission
                val tournaments = repository.getTournaments().take(1).first()
                Log.d("TournamentViewModel", "Refresh received ${tournaments.size} tournaments")
                
                val currentState = state.value ?: TournamentState()
                setState(currentState.copy(tournaments = tournaments))
                
                // Ensure minimum loading time for better UX
                val elapsedTime = System.currentTimeMillis() - startTime
                if (elapsedTime < minimumLoadingTime) {
                    val remainingTime = minimumLoadingTime - elapsedTime
                    Log.d("TournamentViewModel", "Adding ${remainingTime}ms delay for minimum loading time")
                    kotlinx.coroutines.delay(remainingTime)
                }
                
                _refreshSuccess.value = true
                Log.d("TournamentViewModel", "Refresh completed successfully")
            } catch (e: Exception) {
                Log.e("TournamentViewModel", "Error during refresh", e)
                _refreshError.value = e.message ?: "Failed to refresh tournaments"
            } finally {
                _isRefreshing.value = false
                Log.d("TournamentViewModel", "Refresh state: isRefreshing = false")
                
                // Clear success/error states after a delay
                kotlinx.coroutines.delay(2000)
                _refreshSuccess.value = false
                _refreshError.value = null
            }
        }
    }

    fun getTournamentById(tournamentId: String) {
        Log.d("TournamentViewModel", "Getting tournament details for ID: $tournamentId, ${selectedTournament.value?.name}, ${selectedTournament.value?.id}, ${selectedTournament.value?.status}")
        viewModelScope.launch {
            _isLoadingDetails.value = true
            _detailsError.value = null
            _selectedTournament.value = null

            try {
                val tournament = repository.getTournamentById(tournamentId)
                Log.d("TournamentViewModel", "Received tournament from repository: ${tournament?.name}")
                if (tournament != null) {
                    _selectedTournament.value = tournament
                } else {
                    _detailsError.value = "Tournament not found"
                    Log.e("TournamentViewModel", "Tournament not found for ID: $tournamentId")
                }
            } catch (e: Exception) {
                Log.e("TournamentViewModel", "Error loading tournament details", e)
                _detailsError.value = "Error loading tournament details: ${e.message}"
            } finally {
                _isLoadingDetails.value = false
            }
        }
    }

    fun clearSelectedTournament() {
        _selectedTournament.value = null
        _detailsError.value = null
    }

    fun clearRegistrationState() {
        _registrationState.value = null
    }


    fun checkRegistrationStatus(tournamentId: String, userId: String) {
        viewModelScope.launch {
            _registrationStatus.value = null
            _registrationStatus.value = repository.isUserRegisteredForTournament(tournamentId, userId)
        }
    }

    fun clearKycDialog() {
        _showKycRequiredDialog.value = false
    }
}
