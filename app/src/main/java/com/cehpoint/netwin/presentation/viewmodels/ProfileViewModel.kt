package com.cehpoint.netwin.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cehpoint.netwin.domain.model.User
import com.cehpoint.netwin.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth,
    private val dataStoreManager: com.cehpoint.netwin.data.local.DataStoreManager
) : ViewModel() {
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _profileSetupState = MutableStateFlow(ProfileSetupState())
    val profileSetupState: StateFlow<ProfileSetupState> = _profileSetupState.asStateFlow()
    
    // Add state to trigger profile completeness re-check
    private val _shouldRecheckProfile = MutableStateFlow(false)
    val shouldRecheckProfile: StateFlow<Boolean> = _shouldRecheckProfile.asStateFlow()

    data class ProfileSetupState(
        val isUsernameAvailable: Boolean? = null,
        val isSaving: Boolean = false,
        val saveSuccess: Boolean = false,
        val error: String? = null
    )

    init {
        viewModelScope.launch {
            var userId = firebaseAuth.currentUser?.uid
            if (userId == null) {
                userId = dataStoreManager.userId.first()
            }
            if (!userId.isNullOrEmpty()) {
                fetchUser(userId)
            }
        }
    }

    fun fetchUser(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = userRepository.getUser(userId)
                _user.value = result.getOrNull()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateUserField(field: String, value: Any) {
        viewModelScope.launch {
            var userId = firebaseAuth.currentUser?.uid
            if (userId == null) {
                userId = dataStoreManager.userId.first()
            }
            if (userId.isNullOrEmpty()) return@launch
            
            _isLoading.value = true
            try {
                userRepository.updateUserField(userId, field, value)
                fetchUser(userId)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
    
    fun resetRecheckProfile() {
        _shouldRecheckProfile.value = false
    }

    fun checkUsernameAvailability(username: String) {
        viewModelScope.launch {
            _profileSetupState.value = _profileSetupState.value.copy(isUsernameAvailable = null, error = null)
            try {
                val exists = userRepository.getUserByUsername(username)
                _profileSetupState.value = _profileSetupState.value.copy(
                    isUsernameAvailable = !exists,
                    error = if (exists) "Username taken" else null
                )
            } catch (e: Exception) {
                _profileSetupState.value = _profileSetupState.value.copy(
                    isUsernameAvailable = null,
                    error = "Error checking username"
                )
            }
        }
    }

    fun saveProfile(username: String, displayName: String, country: String, profilePicUrl: String?) {
        viewModelScope.launch {
            var userId = firebaseAuth.currentUser?.uid
            if (userId == null) {
                userId = dataStoreManager.userId.first()
            }
            if (userId.isNullOrEmpty()) return@launch
            
            val user = firebaseAuth.currentUser
            _profileSetupState.value = _profileSetupState.value.copy(isSaving = true, error = null, saveSuccess = false)
            try {
                val userProfile = hashMapOf(
                    "uid" to userId,
                    "email" to (user?.email ?: ""),
                    "username" to username,
                    "displayName" to displayName,
                    "country" to country,
                    "phone" to (user?.phoneNumber ?: ""),
                    "profileImage" to (profilePicUrl ?: ""),
                    "kycStatus" to "pending",
                    "status" to "active",
                    "walletBalance" to 0,
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "lastLogin" to com.google.firebase.Timestamp.now(),
                    "provider" to (user?.providerData?.firstOrNull()?.providerId ?: "phone")
                )
                // Overwrite or merge with existing doc
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .set(userProfile, SetOptions.merge())
                    .await()
                _profileSetupState.value = _profileSetupState.value.copy(isSaving = false, saveSuccess = true)
                fetchUser(userId)
                
                // Trigger profile completeness check for NavGraph
                android.util.Log.d("ProfileViewModel", "Profile saved successfully, triggering completeness check")
                _shouldRecheckProfile.value = true
            } catch (e: Exception) {
                _profileSetupState.value = _profileSetupState.value.copy(isSaving = false, error = e.message)
            }
        }
    }

    suspend fun isProfileComplete(): Boolean {
        android.util.Log.d("ProfileViewModel", "=== isProfileComplete STARTED ===")
        var userId = firebaseAuth.currentUser?.uid
        android.util.Log.d("ProfileViewModel", "isProfileComplete - Current user ID: $userId")
        android.util.Log.d("ProfileViewModel", "isProfileComplete - FirebaseAuth current user: ${firebaseAuth.currentUser}")
        
        // If Firebase Auth doesn't have user, check DataStore
        if (userId == null) {
            userId = dataStoreManager.userId.first()
            android.util.Log.d("ProfileViewModel", "isProfileComplete - User ID from DataStore: $userId")
        }
        
        if (userId.isNullOrEmpty()) {
            android.util.Log.d("ProfileViewModel", "isProfileComplete - No user ID found, returning false")
            return false
        }
        
        android.util.Log.d("ProfileViewModel", "isProfileComplete - Checking profile completeness for user: $userId")
        
        val result = userRepository.getUser(userId)
        android.util.Log.d("ProfileViewModel", "isProfileComplete - Repository result: $result")
        
        val user = result.getOrNull()
        android.util.Log.d("ProfileViewModel", "isProfileComplete - User object: $user")
        
        if (user == null) {
            android.util.Log.d("ProfileViewModel", "isProfileComplete - User not found in database")
            return false
        }
        
        val hasUsername = !user.username.isNullOrBlank()
        val hasDisplayName = !user.displayName.isNullOrBlank()
        
        android.util.Log.d("ProfileViewModel", "isProfileComplete - Profile check details:")
        android.util.Log.d("ProfileViewModel", "isProfileComplete - Username: $hasUsername (${user.username})")
        android.util.Log.d("ProfileViewModel", "isProfileComplete - DisplayName: $hasDisplayName (${user.displayName})")
        android.util.Log.d("ProfileViewModel", "isProfileComplete - User email: ${user.email}")
        android.util.Log.d("ProfileViewModel", "isProfileComplete - User country: ${user.country}")
        
        // Country is now handled in KYC screen, so only check username and displayName
        val isComplete = hasUsername && hasDisplayName
        android.util.Log.d("ProfileViewModel", "isProfileComplete - Profile completeness result: $isComplete")
        android.util.Log.d("ProfileViewModel", "=== isProfileComplete COMPLETED ===")
        
        return isComplete
    }

    // Non-suspend version for UI calls
    fun isProfileCompleteAsync(callback: (Boolean) -> Unit) {
        android.util.Log.d("ProfileViewModel", "=== isProfileCompleteAsync STARTED ===")
        viewModelScope.launch {
            val isComplete = isProfileComplete()
            android.util.Log.d("ProfileViewModel", "isProfileCompleteAsync - Calling callback with result: $isComplete")
            callback(isComplete)
            android.util.Log.d("ProfileViewModel", "=== isProfileCompleteAsync COMPLETED ===")
        }
    }
} 