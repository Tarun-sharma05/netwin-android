package com.cehpoint.netwin.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.cehpoint.netwin.data.model.KycStatus
import com.cehpoint.netwin.data.remote.FirebaseManager
import com.cehpoint.netwin.domain.model.Tournament
import com.cehpoint.netwin.domain.repository.TournamentRepository
import com.cehpoint.netwin.domain.model.TournamentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlinx.coroutines.flow.update
import com.cehpoint.netwin.domain.repository.UserRepository
import com.cehpoint.netwin.data.model.User
import com.cehpoint.netwin.domain.repository.WalletRepository

data class TournamentState(
    val tournaments: List<Tournament> = emptyList(),
    val selectedFilter: TournamentFilter = TournamentFilter.ALL,
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class TournamentFilter {
    ALL, UPCOMING, ONGOING, COMPLETED
}

sealed class TournamentEvent {
    object LoadTournaments : TournamentEvent()
    data class FilterTournaments(val filter: TournamentFilter) : TournamentEvent()
    data class CreateTournament(val tournament: Tournament) : TournamentEvent()
    data class RefreshTournaments(val force: Boolean = false) : TournamentEvent()
    data class RegisterForTournament(val tournament: Tournament, val inGameId: String) : TournamentEvent()
}

@HiltViewModel
class TournamentViewModel @Inject constructor(
    private val repository: TournamentRepository,
    private val firebaseManager: FirebaseManager,
    private val userRepository: UserRepository,
    private val walletRepository: WalletRepository
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

    init {
        setState(TournamentState())
        loadTournaments()
        val userId = firebaseManager.auth.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch {
                walletRepository.getWalletBalance(userId).collect { balance ->
                    _walletBalance.value = balance
                }
            }
            viewModelScope.launch {
                val userResult = userRepository.getUser(userId)
                val user = userResult.getOrNull()
                _userName.value = user?.username?.takeIf { it.isNotBlank() }
                    ?: user?.displayName?.takeIf { it.isNotBlank() }
                    ?: "Gamer"
            }
        }
    }

    override fun handleEvent(event: TournamentEvent) {
        when (event) {
            is TournamentEvent.LoadTournaments -> loadTournaments()
            is TournamentEvent.FilterTournaments -> filterTournaments(event.filter)
            is TournamentEvent.CreateTournament -> createTournament(event.tournament)
            is TournamentEvent.RefreshTournaments -> refreshTournaments(event.force)
            is TournamentEvent.RegisterForTournament -> {
                val currentUser = firebaseManager.auth.currentUser
                if (currentUser != null) {
                    viewModelScope.launch {
                        val userResult = userRepository.getUser(currentUser.uid)
                        val user = userResult.getOrNull()
                        if (user?.kycStatus == "${KycStatus.VERIFIED}") {
                           Log.d("TournamentViewModel", "User is verified, proceeding with registration ${user.kycStatus}")
                            registerForTournament(
                                tournamentId = event.tournament.id,
                                userId = currentUser.uid,
                                displayName = currentUser.displayName ?: "Player",
                                teamName = "",
                                inGameId = event.inGameId
                            )
                        } else {
                            _showKycRequiredDialog.value = true
                        }
                    }
                } else {
                    _registrationState.value = Result.failure(Exception("You must be logged in to register."))
                }
            }
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
                        TournamentFilter.UPCOMING -> tournaments.filter { it.status.contains(TournamentStatus.UPCOMING) }
                        TournamentFilter.ONGOING -> tournaments.filter { it.status.contains(TournamentStatus.ONGOING) }
                        TournamentFilter.COMPLETED -> tournaments.filter { it.status.contains(TournamentStatus.COMPLETED) }
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
        if (force || (state.value?.tournaments?.isEmpty() ?: true)) {
            loadTournaments()
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

    fun registerForTournament(tournamentId: String, userId: String, displayName: String, teamName: String, inGameId: String) {
        viewModelScope.launch {
            _registrationState.value = null
            _registrationState.value = repository.registerForTournament(tournamentId, userId, displayName, teamName, inGameId)
        }
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
