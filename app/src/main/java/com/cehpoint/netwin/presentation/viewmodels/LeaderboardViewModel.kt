package com.cehpoint.netwin.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cehpoint.netwin.presentation.screens.LeaderboardEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LeaderboardViewModel @Inject constructor() : ViewModel() {
    private val _leaderboardState = MutableStateFlow(LeaderboardState())
    val leaderboardState: StateFlow<LeaderboardState> = _leaderboardState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadLeaderboard()
    }

    private fun loadLeaderboard() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Remove sample/mock data
                _leaderboardState.value = LeaderboardState(
                    allPlayers = emptyList()
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load leaderboard"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshLeaderboard() {
        loadLeaderboard()
    }
}

data class LeaderboardState(
    val allPlayers: List<LeaderboardEntry> = emptyList()
) 