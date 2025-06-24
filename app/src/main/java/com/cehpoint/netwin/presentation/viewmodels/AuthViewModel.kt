package com.cehpoint.netwin.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cehpoint.netwin.domain.model.User
import com.cehpoint.netwin.domain.repository.AuthRepository
import com.cehpoint.netwin.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        _currentUser.value = firebaseAuth.currentUser
        _isAuthenticated.value = firebaseAuth.currentUser != null

        firebaseAuth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            _isAuthenticated.value = firebaseAuth.currentUser != null
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                authRepository.signIn(email, password).fold(
                    onSuccess = {
                        _isAuthenticated.value = true
                    },
                    onFailure = { e ->
                        _error.value = when {
                            e.message?.contains("password is invalid") == true -> 
                                "Invalid email or password"
                            e.message?.contains("no user record") == true -> 
                                "No account found with this email"
                            else -> "Authentication failed. Please try again."
                        }
                        _isAuthenticated.value = false
                    }
                )
            } catch (e: Exception) {
                _error.value = "An unexpected error occurred. Please try again."
                _isAuthenticated.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                authRepository.signUp(email, password).fold(
                    onSuccess = {
                        val firebaseUser = firebaseAuth.currentUser
                        if (firebaseUser != null) {
                            val user = User(
                                id = firebaseUser.uid,
                                displayName = firebaseUser.displayName ?: "",
                                username = firebaseUser.displayName ?: "",
                                email = email,
                                country = "India",
                                currency = "INR",
                                walletBalance = 0.0,
                                profilePictureUrl = firebaseUser.photoUrl?.toString() ?: "",
                                role = "user",
                                phoneNumber = firebaseUser.phoneNumber ?: "",
                                gameId = "",
                                gameMode = "PUBG",
                                matchesPlayed = 0,
                                matchesWon = 0,
                                totalKills = 0,
                                totalEarnings = 0.0,
                                tournamentsJoined = 0,
                                kycStatus = "pending",
                                kycDocumentType = "",
                                kycDocumentNumber = "",
                                kycRejectedReason = "",
                                isBanned = false,
                                banReason = "",
                                adminNotes = "",
                                notificationsEnabled = true,
                                preferredLanguage = "en",
                                deviceType = "Android",
                                appVersion = "1.0.0"
                            )

                            userRepository.createUser(user).fold(
                                onSuccess = {
                                    _isAuthenticated.value = true
                                },
                                onFailure = { e ->
                                    _error.value = "Failed to create user profile. Please try again."
                                    firebaseUser.delete()
                                    _isAuthenticated.value = false
                                }
                            )
                        }
                    },
                    onFailure = { e ->
                        _error.value = when {
                            e.message?.contains("email address is already in use") == true ->
                                "An account with this email already exists"
                            e.message?.contains("badly formatted") == true ->
                                "Invalid email format"
                            e.message?.contains("password is invalid") == true ->
                                "Password must be at least 6 characters"
                            else -> "Failed to create account. Please try again."
                        }
                        _isAuthenticated.value = false
                    }
                )
            } catch (e: Exception) {
                _error.value = "An unexpected error occurred. Please try again."
                _isAuthenticated.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                authRepository.signOut()
                _currentUser.value = null
                _isAuthenticated.value = false
            } catch (e: Exception) {
                _error.value = "Failed to sign out. Please try again."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
} 