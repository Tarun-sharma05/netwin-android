package com.cehpoint.netwin.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cehpoint.netwin.data.remote.FirebaseManager
import com.cehpoint.netwin.domain.model.User
import com.cehpoint.netwin.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        val userId = firebaseAuth.currentUser?.uid
        Log.d("MoreViewModel", "init: currentUser uid = $userId")
        if (userId != null) {
            fetchUser(userId)
        } else {
            Log.e("MoreViewModel", "init: currentUser is null")
        }
    }

    fun fetchUser(userId: String) {
        Log.d("MoreViewModel", "fetchUser called with userId = $userId")
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = userRepository.getUser(userId)
                Log.d("MoreViewModel", "getUser result: $result")
                _user.value = result.getOrNull()
                if (_user.value == null) {
                    Log.e("MoreViewModel", "User not found in Firestore for userId = $userId")
                } else {
                    Log.d("MoreViewModel", "User loaded: ${_user.value}")
                }
            } catch (e: Exception) {
                Log.e("MoreViewModel", "Error fetching user: ${e.message}", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                firebaseAuth.signOut()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to logout"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshUserData() {
        val userId = firebaseAuth.currentUser?.uid
        Log.d("MoreViewModel", "refreshUserData called, currentUser uid = $userId")
        if (userId != null) {
            fetchUser(userId)
        } else {
            Log.e("MoreViewModel", "refreshUserData: currentUser is null")
        }
    }
} 