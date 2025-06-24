package com.cehpoint.netwin.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cehpoint.netwin.domain.model.Tournament
import com.cehpoint.netwin.domain.repository.TournamentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val featuredTournaments: List<Tournament> = emptyList()
)

sealed class HomeEvent {
    object LoadFeaturedTournaments : HomeEvent()
    object RefreshTournaments : HomeEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val tournamentRepository: TournamentRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun handleEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.LoadFeaturedTournaments -> loadFeaturedTournaments()
            is HomeEvent.RefreshTournaments -> refreshTournaments()
        }
    }

    private fun loadFeaturedTournaments() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val tournaments = tournamentRepository.getFeaturedTournaments()
                _state.value = _state.value.copy(featuredTournaments = tournaments)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load tournaments"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun refreshTournaments() {
        loadFeaturedTournaments()
    }
} 