package com.cehpoint.netwin.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cehpoint.netwin.data.local.DataStoreManager
import com.cehpoint.netwin.data.remote.FirebaseManager
import com.cehpoint.netwin.domain.model.Tournament
import com.cehpoint.netwin.domain.model.TournamentStatus
import com.cehpoint.netwin.domain.model.RegistrationStepData
import com.cehpoint.netwin.domain.model.RegistrationStep
import com.cehpoint.netwin.domain.repository.TournamentRepository
import com.cehpoint.netwin.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.cehpoint.netwin.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import com.cehpoint.netwin.presentation.events.RegistrationFlowEvent
import com.cehpoint.netwin.utils.NetworkStateMonitor
import com.cehpoint.netwin.utils.RetryUtils
import kotlinx.coroutines.flow.update

data class TournamentState(
    val tournaments: List<Tournament> = emptyList(),
    val selectedFilter: TournamentFilter = TournamentFilter.ALL,
    val isLoading: Boolean = false,
    val error: String? = null,
    val countdowns: Map<String, String> = emptyMap() // NEW: tournamentId -> remainingTime
)

// RegistrationStepData is now defined in domain.model package

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
    private val dataStoreManager: DataStoreManager,
    private val savedStateHandle: SavedStateHandle,
    private val networkStateMonitor: NetworkStateMonitor
) : BaseViewModel<TournamentState, TournamentEvent>() {

    companion object {
        private const val KEY_CURRENT_STEP = "current_step"
        private const val KEY_STEP_DATA = "step_data"
    }

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

    // User-specific data
    private val _userCountry = MutableStateFlow("India")
    val userCountry: StateFlow<String> = _userCountry.asStateFlow()

    private val _userCurrency = MutableStateFlow("INR")
    val userCurrency: StateFlow<String> = _userCurrency.asStateFlow()

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
    private val _currentStep = savedStateHandle.getStateFlow(KEY_CURRENT_STEP, RegistrationStep.REVIEW)
    val currentStep: StateFlow<RegistrationStep> = _currentStep

    private val _stepData = savedStateHandle.getStateFlow(KEY_STEP_DATA, RegistrationStepData())
    val stepData: StateFlow<RegistrationStepData> = _stepData

    private val _currentError = MutableStateFlow<String?>(null)
    val currentError: StateFlow<String?> = _currentError.asStateFlow()

    val isLastStep = _currentStep.map { it == RegistrationStep.CONFIRM }

    // Network State Monitoring
    private val _networkAvailable = MutableStateFlow(true)
    val networkAvailable: StateFlow<Boolean> = _networkAvailable.asStateFlow()

    // State for My Tournaments screen
    private val _myTournamentsUiState = MutableStateFlow(MyTournamentsUiState())
    val myTournamentsUiState: StateFlow<MyTournamentsUiState> = _myTournamentsUiState.asStateFlow()

    // Retry State Management
    private val _retryCount = MutableStateFlow(0)
    val retryCount: StateFlow<Int> = _retryCount.asStateFlow()

    private val _isRetrying = MutableStateFlow(false)
    val isRetrying: StateFlow<Boolean> = _isRetrying.asStateFlow()

    // RegistrationUiState data class for exposing immutable UI state
        data class MyTournamentsUiState(
        val tournaments: List<Tournament> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    )

    data class RegistrationUiState(
        val step: RegistrationStep,
        val data: RegistrationStepData,
        val error: String?,
        val loading: Boolean,
        val networkAvailable: Boolean = true,
        val retryCount: Int = 0,
        val isRetrying: Boolean = false
    )

    // Combine internal flows to provide single source of truth for UI
    val registrationUiState: StateFlow<RegistrationUiState> = combine(
        _currentStep,
        _stepData,
        _currentError,
        _isRegistering,
        _networkAvailable,
        _retryCount,
        _isRetrying
    ) { values ->
        val step = values[0] as RegistrationStep
        val data = values[1] as RegistrationStepData
        val error = values[2] as String?
        val loading = values[3] as Boolean
        val networkAvailable = values[4] as Boolean
        val retryCount = values[5] as Int
        val isRetrying = values[6] as Boolean
        
        RegistrationUiState(
            step = step,
            data = data,
            error = error,
            loading = loading || isRetrying,
            networkAvailable = networkAvailable,
            retryCount = retryCount,
            isRetrying = isRetrying
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RegistrationUiState(
            step = RegistrationStep.REVIEW,
            data = RegistrationStepData(),
            error = null,
            loading = false,
            networkAvailable = true,
            retryCount = 0,
            isRetrying = false
        )
    )



    fun nextStep() {
        val currentStepValue = _currentStep.value
        val nextStep = when (currentStepValue) {
            RegistrationStep.REVIEW -> RegistrationStep.PAYMENT
            RegistrationStep.PAYMENT -> RegistrationStep.DETAILS
            RegistrationStep.DETAILS -> RegistrationStep.CONFIRM
            RegistrationStep.CONFIRM -> RegistrationStep.CONFIRM // Stay at last step
        }
        if (nextStep != currentStepValue) {
            savedStateHandle[KEY_CURRENT_STEP] = nextStep
            _currentError.value = null
        }
    }

    fun previousStep() {
        val currentStepValue = _currentStep.value
        val previousStep = when (currentStepValue) {
            RegistrationStep.CONFIRM -> RegistrationStep.DETAILS
            RegistrationStep.DETAILS -> RegistrationStep.PAYMENT
            RegistrationStep.PAYMENT -> RegistrationStep.REVIEW
            RegistrationStep.REVIEW -> RegistrationStep.REVIEW // Stay at first step
        }
        if (previousStep != currentStepValue) {
            savedStateHandle[KEY_CURRENT_STEP] = previousStep
            _currentError.value = null
        }
    }

    fun setStepError(error: String?) {
        _currentError.value = error
    }

    fun resetRegistrationFlow() {
        savedStateHandle[KEY_CURRENT_STEP] = RegistrationStep.REVIEW
        savedStateHandle[KEY_STEP_DATA] = RegistrationStepData()
        _currentError.value = null
    }

    /**
     * Main event handler for RegistrationFlowEvent
     * Handles all registration flow events with proper validation and step management
     */
    fun onRegistrationEvent(event: RegistrationFlowEvent) {
        Log.d("TournamentViewModel", "=== onRegistrationEvent ENTRY ===")
        Log.d("TournamentViewModel", "Event type: ${event::class.simpleName}")
        Log.d("TournamentViewModel", "Current step: ${_currentStep.value}")
        Log.d("TournamentViewModel", "Current data: playerIds='${_stepData.value.playerIds.joinToString()}', teamName='${_stepData.value.teamName}', paymentMethod='${_stepData.value.paymentMethod}', termsAccepted=${_stepData.value.termsAccepted}")
        
        when (event) {
            is RegistrationFlowEvent.UpdateData -> {
                Log.d("TournamentViewModel", "Processing UpdateData event")
                // Update step data via functional update
                val currentData = _stepData.value
                val updatedData = event.transform(currentData)
                savedStateHandle[KEY_STEP_DATA] = updatedData
                Log.d("TournamentViewModel", "Data updated: ${updatedData}")
                // Clear current error when data is updated
                _currentError.value = null
                Log.d("TournamentViewModel", "Error cleared due to data update")
            }
            
            is RegistrationFlowEvent.Next -> {
                Log.d("TournamentViewModel", "Processing Next event - VALIDATING BEFORE ADVANCE")
                // Validate current step before advancing
                val currentData = _stepData.value
                val currentStepValue = _currentStep.value
                Log.d("TournamentViewModel", "About to call validate() for step: $currentStepValue")
                val validationError = validate()
                Log.d("TournamentViewModel", "Validation result: ${if (validationError == null) "PASSED" else "FAILED with error: '$validationError'"}")
                
                if (validationError != null) {
                    Log.e("TournamentViewModel", "VALIDATION FAILED - Setting error and blocking navigation")
                    _currentError.value = validationError
                } else {
                    Log.d("TournamentViewModel", "VALIDATION PASSED - Proceeding to next step")
                    // Clear error and move to next step
                    _currentError.value = null
                    moveToNextStep()
                }
            }
            
            is RegistrationFlowEvent.Previous -> {
                Log.d("TournamentViewModel", "Processing Previous event - No validation required")
                // Move to previous step (no validation needed)
                _currentError.value = null
                moveToPreviousStep()
            }
            
            is RegistrationFlowEvent.Reset -> {
                Log.d("TournamentViewModel", "Processing Reset event")
                resetRegistrationFlow()
            }
            
            is RegistrationFlowEvent.Submit -> {
                Log.d("TournamentViewModel", "Processing Submit event - FINAL COMPREHENSIVE VALIDATION")
                // Final comprehensive validation of all steps before submission
                val currentData = _stepData.value
                val validationError = currentData.validateAll()
                Log.d("TournamentViewModel", "Final validateAll() result: ${if (validationError == null) "PASSED" else "FAILED with error: '$validationError'"}")
                if (validationError != null) {
                    Log.e("TournamentViewModel", "FINAL COMPREHENSIVE VALIDATION FAILED - Blocking submission")
                    _currentError.value = validationError
                    return
                }
                
                Log.d("TournamentViewModel", "FINAL COMPREHENSIVE VALIDATION PASSED - Proceeding with registration")
                // Clear error and proceed with tournament registration
                _currentError.value = null
                performTournamentRegistration()
            }
        }
        Log.d("TournamentViewModel", "=== onRegistrationEvent EXIT ===")
    }

    /**
     * Validates current step data
     * @return Error message if validation fails, null if valid
     */
    private fun validate(): String? {
        Log.d("TournamentViewModel", "=== validate() ENTRY ===")
        val currentData = _stepData.value
        val currentStepValue = _currentStep.value
        Log.d("TournamentViewModel", "Validating step: $currentStepValue")
        Log.d("TournamentViewModel", "Data being validated: $currentData")
        Log.d("TournamentViewModel", "About to call RegistrationStepData.validate($currentStepValue)")
        
        val validationResult = currentData.validate(currentStepValue)
        
        Log.d("TournamentViewModel", "RegistrationStepData.validate() returned: ${if (validationResult == null) "null (VALID)" else "'$validationResult' (INVALID)"}")
        Log.d("TournamentViewModel", "=== validate() EXIT ===")
        
        return validationResult
    }

    /**
     * Moves to the next step using ordered enum values
     */
    private fun moveToNextStep() {
        val orderedSteps = RegistrationStep.values()
        val currentStepValue = _currentStep.value
        val currentIndex = orderedSteps.indexOf(currentStepValue)
        
        if (currentIndex < orderedSteps.size - 1) {
            savedStateHandle[KEY_CURRENT_STEP] = orderedSteps[currentIndex + 1]
        }
    }

    /**
     * Moves to the previous step using ordered enum values
     */
    private fun moveToPreviousStep() {
        val orderedSteps = RegistrationStep.values()
        val currentStepValue = _currentStep.value
        val currentIndex = orderedSteps.indexOf(currentStepValue)
        
        if (currentIndex > 0) {
            savedStateHandle[KEY_CURRENT_STEP] = orderedSteps[currentIndex - 1]
        }
    }

    /**
     * Performs the actual tournament registration with retry logic and network monitoring
     */
    private fun performTournamentRegistration() {
        val currentData = _stepData.value
        val userId = firebaseManager.auth.currentUser?.uid
        val displayName = _userName.value
        
        if (userId == null) {
            _registrationState.value = Result.failure(Exception("User not authenticated"))
            return
        }
        
        if (currentData.tournamentId.isBlank()) {
            _registrationState.value = Result.failure(Exception("Tournament ID is required"))
            return
        }
        
        viewModelScope.launch {
            _isRegistering.value = true
            _registrationError.value = null
            _retryCount.value = 0
            
            // Monitor network state during registration
            val networkJob = launch {
                networkStateMonitor.observeNetworkState().collect { isConnected ->
                    _networkAvailable.value = isConnected
                    Log.d("TournamentViewModel", "Network state changed: $isConnected")
                    
                    if (!isConnected) {
                        Log.w("TournamentViewModel", "Network disconnected during registration")
                        _currentError.value = "Network connection lost. Retrying when connection is restored..."
                    } else if (_currentError.value?.contains("Network") == true) {
                        _currentError.value = null
                    }
                }
            }
            
            try {
                // For business rule errors (like registration closed), don't retry
                // Only retry for network/timeout issues
                var lastException: Exception? = null
                var shouldRetry = true
                var attemptCount = 0
                
                while (shouldRetry && attemptCount < 3) {
                    attemptCount++
                    _retryCount.value = attemptCount
                    _isRetrying.value = attemptCount > 1
                    
                    Log.d("TournamentViewModel", "Registration attempt $attemptCount")
                    
                    try {
                        // Check network availability before each attempt
                        if (!networkStateMonitor.isNetworkAvailable()) {
                            throw Exception("Network unavailable")
                        }
                        
                        // Call repository method with timeout
                        val registrationResult = RetryUtils.withTimeout(10000L) {
                            repository.registerForTournament(
                                tournamentId = currentData.tournamentId,
                                userId = userId,
                                displayName = displayName,
                                teamName = currentData.teamName,
                                playerIds = currentData.playerIds
                            )
                        }
                        
                        // Check if the Result itself failed and throw if needed
                        registrationResult.getOrThrow()
                        
                        // If we reach here, registration was successful
                        shouldRetry = false
                        lastException = null
                        
                    } catch (e: Exception) {
                        lastException = e
                        Log.w("TournamentViewModel", "Registration attempt $attemptCount failed: ${e.message}")
                        
                        // Don't retry for business rule errors
                        shouldRetry = when {
                            e.message?.contains("Registration is closed", ignoreCase = true) == true -> false
                            e.message?.contains("already registered", ignoreCase = true) == true -> false
                            e.message?.contains("insufficient", ignoreCase = true) == true -> false
                            e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true -> false
                            e.message?.contains("Missing or insufficient permissions", ignoreCase = true) == true -> false
                            e.message?.contains("network", ignoreCase = true) == true -> attemptCount < 3
                            e.message?.contains("timeout", ignoreCase = true) == true -> attemptCount < 3
                            RetryUtils.isRetryableException(e) -> attemptCount < 3
                            else -> false // Unknown errors don't retry
                        }
                        
                        if (shouldRetry && attemptCount < 3) {
                            Log.d("TournamentViewModel", "Will retry registration in ${1000L * attemptCount}ms")
                            delay(1000L * attemptCount) // Exponential backoff
                        }
                    }
                }
                
                // If we have an exception, throw it
                if (lastException != null) {
                    throw lastException
                }
                
                // If we reach here, registration was successful
                Log.d("TournamentViewModel", "Registration successful after ${_retryCount.value} attempts")
                _registrationState.value = Result.success(Unit)
                resetRegistrationFlow()
                _retryCount.value = 0
                
            } catch (e: Exception) {
                Log.e("TournamentViewModel", "Registration failed after ${_retryCount.value} attempts: ${e.message}", e)
                
                // Set appropriate error message based on exception type
                val errorMessage = when {
                    e.message?.contains("Registration is closed", ignoreCase = true) == true ->
                        "Registration is closed for this tournament"
                    e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ->
                        "Permission denied. Please check your account permissions or contact support."
                    e.message?.contains("Missing or insufficient permissions", ignoreCase = true) == true ->
                        "Permission denied. Please check your account permissions or contact support."
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Network error. Please check your connection and try again."
                    e.message?.contains("timeout", ignoreCase = true) == true ->
                        "Request timed out. Please try again."
                    e.message?.contains("insufficient", ignoreCase = true) == true ->
                        "Insufficient wallet balance for this tournament"
                    e.message?.contains("already registered", ignoreCase = true) == true ->
                        "You are already registered for this tournament"
                    RetryUtils.isRetryableException(e) ->
                        "Connection issue. Please try again."
                    else -> e.message ?: "Registration failed. Please try again."
                }
                
                Log.d("TournamentViewModel", "Setting error message: $errorMessage")
                _currentError.value = errorMessage
                _registrationState.value = Result.failure(e)
                
            } finally {
                networkJob.cancel() // Stop network monitoring
                _isRegistering.value = false
                _isRetrying.value = false
                
                Log.d("TournamentViewModel", "Registration process completed")
            }
        }
    }
    
    /**
     * Retry registration manually (for UI retry buttons)
     */
    fun retryRegistration() {
        Log.d("TournamentViewModel", "Manual retry registration triggered")
        _currentError.value = null
        _retryCount.value = 0
        performTournamentRegistration()
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

    /**
     * Fetches tournaments that the current user has registered for
     */
    fun getRegisteredTournaments() {
        viewModelScope.launch {
            try {
                _myTournamentsUiState.update { it.copy(isLoading = true, error = null) }
                
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                if (userId.isBlank()) {
                    _myTournamentsUiState.update { it.copy(
                        isLoading = false,
                        error = "User not authenticated"
                    ) }
                    return@launch
                }
                
                // Get registered tournament IDs for the user
                val registeredTournamentIds = repository.getUserTournamentRegistrations(userId)
                
                if (registeredTournamentIds.isEmpty()) {
                    _myTournamentsUiState.update { it.copy(
                        tournaments = emptyList(),
                        isLoading = false,
                        error = null
                    ) }
                    return@launch
                }
                
                // Fetch full tournament details for each registered tournament
                val tournaments = registeredTournamentIds.mapNotNull { tournamentId ->
                    repository.getTournamentById(tournamentId)
                }.sortedBy { it.startTime } // Sort by start time
                
                _myTournamentsUiState.update { it.copy(
                    tournaments = tournaments,
                    isLoading = false,
                    error = null
                ) }
                
            } catch (e: Exception) {
                _myTournamentsUiState.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load tournaments"
                ) }
            }
        }
    }
    
    /**
     * Refreshes the list of registered tournaments
     */
    fun refreshTournaments() {
        viewModelScope.launch {
            _isRefreshing.value = true
            getRegisteredTournaments()
            _isRefreshing.value = false
        }
    }


    init {
        // Log state restoration for debugging
        Log.d("TournamentViewModel", "Initializing ViewModel")
        Log.d("TournamentViewModel", "Restored currentStep: ${_currentStep.value}")
        Log.d("TournamentViewModel", "Restored stepData: ${_stepData.value}")
        
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

                // Set country and currency from the fetched user data
                val country = user?.country ?: "India"
                _userCountry.value = country
                _userCurrency.value = if (country.equals("Nigeria", true) || country.equals("NG", true)) "NGN" else "INR"
                Log.d("TournamentViewModel", "Fetched user details. Country: ${user?.country}, Currency: ${_userCurrency.value}")
                    }
                } catch (e: Exception) {
                    Log.e("TournamentViewModel", "Error fetching user data", e)
                    _userName.value = "Gamer"
                    _userCountry.value = "India"
                    _userCurrency.value = "INR"
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
