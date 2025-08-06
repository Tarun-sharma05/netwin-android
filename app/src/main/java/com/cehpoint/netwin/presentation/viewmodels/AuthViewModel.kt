package com.cehpoint.netwin.presentation.viewmodels

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cehpoint.netwin.domain.model.User
import com.cehpoint.netwin.domain.repository.AuthRepository
import com.cehpoint.netwin.domain.repository.UserRepository
import com.cehpoint.netwin.data.local.DataStoreManager
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _isAuthenticated = MutableStateFlow(firebaseAuth.currentUser != null)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _currentUser = MutableStateFlow(firebaseAuth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val  splashShowFlow = MutableStateFlow(false)
    val isSplashShow = splashShowFlow.asStateFlow()



    fun splashScreen() {
        Log.d("AuthViewModel", "=== SPLASH SCREEN STARTED ===")
        viewModelScope.launch {
            val delayJob = async { delay(3000) }
            val fetchJob = async {
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    try {
                        withTimeoutOrNull(5000) {  // 5 second timeout
                            val userDoc = FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(currentUser.uid)
                                .get()
                                .await()
                            if (userDoc.exists()) {
                                val userData = userDoc.data
                                userData?.let { data ->
                                    val nameToStore = data["username"] as? String
                                        ?: data["displayName"] as? String
                                        ?: ""
                                    dataStoreManager.setUserName(nameToStore)
                                    Log.d("AuthViewModel", "Splash screen - User name stored in DataStore: $nameToStore")
                                }
                            }
                        } ?: Log.w("Splash", "User data fetch timed out")
                    } catch (e: Exception) {
                        Log.w("AuthViewModel", "Splash screen - Failed to pre-fetch user data", e)
                    }
                }
            }
            delayJob.await()
            fetchJob.await()
            Log.d("AuthViewModel", "Splash screen - 3 second delay and fetch completed, setting splashShowFlow to true")
            splashShowFlow.value = true
            Log.d("AuthViewModel", "=== SPLASH SCREEN COMPLETED ===")
        }
    }


    // Add authentication state initialization
    private val _isAuthStateInitialized = MutableStateFlow(false)
    val isAuthStateInitialized: StateFlow<Boolean> = _isAuthStateInitialized.asStateFlow()

    // RegistrationStep enum for multi-step registration
    enum class RegistrationStep {
        Welcome,
        EmailPassword,
        UsernameDisplayName,
        Country,
        Phone,
        ProfilePicture,
        Review
    }

    // RegistrationState data class for holding all registration fields and validation
     data class RegistrationState(
        val step: RegistrationStep = RegistrationStep.Welcome,
        val email: String = "",
        val emailError: String? = null,
        val password: String = "",
        val passwordError: String? = null,
        val confirmPassword: String = "",
        val confirmPasswordError: String? = null,
        val passwordStrength: Int = 0, // 0-100
        val username: String = "",
        val usernameError: String? = null,
        val isUsernameAvailable: Boolean? = null,
        val displayName: String = "",
        val displayNameError: String? = null,
        val country: String = "",
        val countryError: String? = null,
        val phone: String = "",
        val phoneError: String? = null,
        val otp: String = "",
        val otpError: String? = null,
        val isPhoneVerified: Boolean = false,
        val profilePictureUri: String? = null,
        val profilePictureError: String? = null,
        val termsAccepted: Boolean = false,
        val termsError: String? = null,
        val isLoading: Boolean = false,
        val globalError: String? = null
    )

    // 3. Add MutableStateFlow for registration state
    private val _registrationState = MutableStateFlow(RegistrationState())
    val registrationState: StateFlow<RegistrationState> = _registrationState.asStateFlow()

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    // --- Phone Login State for LoginScreen ---
    private var loginVerificationId: String? = null
    private var loginResendToken: PhoneAuthProvider.ForceResendingToken? = null



    init {
        Log.d("AuthViewModel", "=== AuthViewModel INIT STARTED ===")
        Log.d("AuthViewModel", "FirebaseAuth instance: $firebaseAuth")
        Log.d("AuthViewModel", "Current user before init: ${firebaseAuth.currentUser}")
        Log.d("AuthViewModel", "Current user UID before init: ${firebaseAuth.currentUser?.uid}")
        Log.d("AuthViewModel", "Current user email before init: ${firebaseAuth.currentUser?.email}")
        Log.d("AuthViewModel", "Current user display name before init: ${firebaseAuth.currentUser?.displayName}")
        Log.d("AuthViewModel", "Current user phone before init: ${firebaseAuth.currentUser?.phoneNumber}")
        Log.d("AuthViewModel", "Current user is email verified before init: ${firebaseAuth.currentUser?.isEmailVerified}")
        
        // Start splash screen timer
        splashScreen()
        
        // Debug Firebase Auth state
        debugAuthState()
        
        // Initialize authentication state
        initializeAuthState()
        
        // Add auth state listener for real-time updates
        firebaseAuth.addAuthStateListener { auth ->
            Log.d("AuthViewModel", "=== AUTH STATE LISTENER TRIGGERED ===")
            Log.d("AuthViewModel", "FirebaseAuth current user: ${auth.currentUser}")
            Log.d("AuthViewModel", "User UID: ${auth.currentUser?.uid}")
            Log.d("AuthViewModel", "User email: ${auth.currentUser?.email}")
            Log.d("AuthViewModel", "User display name: ${auth.currentUser?.displayName}")
            Log.d("AuthViewModel", "User phone: ${auth.currentUser?.phoneNumber}")
            Log.d("AuthViewModel", "User is email verified: ${auth.currentUser?.isEmailVerified}")

            _currentUser.value = auth.currentUser
            _isAuthenticated.value = auth.currentUser != null
            _isAuthStateInitialized.value = true
            
            Log.d("AuthViewModel", "Updated state - isAuthenticated: ${_isAuthenticated.value}")
            Log.d("AuthViewModel", "Updated state - isAuthStateInitialized: ${_isAuthStateInitialized.value}")
            Log.d("AuthViewModel", "Updated state - currentUser: ${_currentUser.value}")
        }
        
        Log.d("AuthViewModel", "=== AuthViewModel INIT COMPLETED ===")
    }

    private fun initializeAuthState() {
        Log.d("AuthViewModel", "=== initializeAuthState STARTED ===")
        viewModelScope.launch {
            try {
                // Check if user is already authenticated
                val currentUser = firebaseAuth.currentUser
                Log.d("AuthViewModel", "initializeAuthState - Current user: $currentUser")
                Log.d("AuthViewModel", "initializeAuthState - User UID: ${currentUser?.uid}")
                Log.d("AuthViewModel", "initializeAuthState - User email: ${currentUser?.email}")
                Log.d("AuthViewModel", "initializeAuthState - User display name: ${currentUser?.displayName}")
                Log.d("AuthViewModel", "initializeAuthState - User phone: ${currentUser?.phoneNumber}")
                Log.d("AuthViewModel", "initializeAuthState - User is email verified: ${currentUser?.isEmailVerified}")
                
                // If user exists, assume they are authenticated initially
                if (currentUser != null) {
                    Log.d("AuthViewModel", "initializeAuthState - User found, setting authenticated state")
                    _currentUser.value = currentUser
                        _isAuthenticated.value = true
                    Log.d("AuthViewModel", "initializeAuthState - User authenticated: ${currentUser.uid}")
                    
                    // Try to refresh token in background, but don't force sign out on failure
                    try {
                        Log.d("AuthViewModel", "initializeAuthState - Attempting token validation")
                        val tokenResult = currentUser.getIdToken(false).await() // Don't force refresh
                        if (tokenResult.token != null) {
                            Log.d("AuthViewModel", "initializeAuthState - Token validation successful")
                            Log.d("AuthViewModel", "initializeAuthState - Token: ${tokenResult.token?.take(20)}...")
                        } else {
                            Log.w("AuthViewModel", "initializeAuthState - Token validation failed, but keeping user logged in")
                        }
                    } catch (e: Exception) {
                        // Token refresh failed, but don't sign out - just log the error
                        Log.w("AuthViewModel", "initializeAuthState - Token refresh failed, but keeping user logged in", e)
                    }
                } else {
                    Log.d("AuthViewModel", "initializeAuthState - No user found, setting unauthenticated state")
                    _currentUser.value = null
                    _isAuthenticated.value = false
                    Log.d("AuthViewModel", "initializeAuthState - No user found")
                }
                
                _isAuthStateInitialized.value = true
                Log.d("AuthViewModel", "initializeAuthState - Final state - isAuthenticated: ${_isAuthenticated.value}")
                Log.d("AuthViewModel", "initializeAuthState - Final state - isAuthStateInitialized: ${_isAuthStateInitialized.value}")
                Log.d("AuthViewModel", "initializeAuthState - Final state - currentUser: ${_currentUser.value}")
                Log.d("AuthViewModel", "=== initializeAuthState COMPLETED ===")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "initializeAuthState - Error during initialization", e)
                _isAuthStateInitialized.value = true
            }
        }
    }

    fun signIn(email: String, password: String, onResult: (Boolean) -> Unit = {}) {
        Log.d("AuthViewModel", "=== SIGN IN ATTEMPT ===")
        Log.d("AuthViewModel", "Email: $email")
        Log.d("AuthViewModel", "Password length: ${password.length}")
        
        _isLoading.value = true
        _error.value = null
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    Log.d("AuthViewModel", "Sign in successful")
                    Log.d("AuthViewModel", "User UID: ${firebaseAuth.currentUser?.uid}")
                    Log.d("AuthViewModel", "User email: ${firebaseAuth.currentUser?.email}")
                    _isAuthenticated.value = true
                    _currentUser.value = firebaseAuth.currentUser
                    onResult(true)
                } else {
                    Log.e("AuthViewModel", "Sign in failed")
                    Log.e("AuthViewModel", "Exception: ${task.exception}")
                    Log.e("AuthViewModel", "Exception type: ${task.exception?.javaClass?.simpleName}")
                    Log.e("AuthViewModel", "Exception message: ${task.exception?.message}")
                    Log.e("AuthViewModel", "Localized message: ${task.exception?.localizedMessage}")
                    
                    _isAuthenticated.value = false
                    _error.value = task.exception?.localizedMessage ?: "Authentication failed. Please try again."
                    onResult(false)
                }
            }
    }

    fun signUp(email: String, password: String, onResult: (Boolean) -> Unit = {}) {
        _isLoading.value = true
        _error.value = null
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    _isAuthenticated.value = true
                    _currentUser.value = firebaseAuth.currentUser
                    onResult(true)
                } else {
                    _isAuthenticated.value = false
                    _error.value = task.exception?.localizedMessage ?: "Failed to create account. Please try again."
                    onResult(false)
                }
            }
    }

    fun signOut() {
        Log.d("AuthViewModel", "=== SIGN OUT STARTED ===")
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Sign out - Current user: ${_currentUser.value}")
                
                // Sign out from Firebase Auth
                firebaseAuth.signOut()
                
                // Clear user data from DataStore
                clearUserDataFromDataStore()
                
                // Update state
                _currentUser.value = null
                _isAuthenticated.value = false
                _error.value = null

                Log.d("AuthViewModel", "Sign out - User signed out and data cleared from DataStore")
                
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign out - Error", e)
                _error.value = e.message ?: "Sign out failed"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    // Method to check if user is authenticated and profile is complete
    fun isUserFullyAuthenticated(): Boolean {
        return _isAuthenticated.value && _currentUser.value != null
    }

    // 4. Add onStepChange and field update functions
    fun onStepChange(step: RegistrationStep) {
        _registrationState.value = _registrationState.value.copy(step = step)
    }

    fun onEmailChange(email: String) {
        _registrationState.value = _registrationState.value.copy(email = email, emailError = null)
    }

    fun onPasswordChange(password: String) {
        _registrationState.value = _registrationState.value.copy(password = password, passwordError = null)
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _registrationState.value = _registrationState.value.copy(confirmPassword = confirmPassword, confirmPasswordError = null)
    }

    fun onUsernameChange(username: String) {
        _registrationState.value = _registrationState.value.copy(username = username, usernameError = null)
    }

    fun onDisplayNameChange(displayName: String) {
        val error = if (displayName.length < 2) "At least 2 characters" else null
        _registrationState.value = _registrationState.value.copy(displayName = displayName, displayNameError = error)
    }

    fun onCountrySelect(country: String) {
        _registrationState.value = _registrationState.value.copy(country = country, countryError = null)
    }

    fun onPhoneChange(phone: String) {
        _registrationState.value = _registrationState.value.copy(phone = phone, phoneError = null)
    }

    fun onOtpChange(otp: String) {
        _registrationState.value = _registrationState.value.copy(otp = otp, otpError = null)
    }

    fun onProfilePictureSelected(uri: String) {
        _registrationState.value = _registrationState.value.copy(profilePictureUri = uri, profilePictureError = null)
    }

    fun onTermsAccepted(accepted: Boolean) {
        _registrationState.value = _registrationState.value.copy(termsAccepted = accepted, termsError = null)
    }

    fun checkUsernameAvailability(username: String) {
        viewModelScope.launch {
            _registrationState.value = _registrationState.value.copy(isUsernameAvailable = null, usernameError = null)
            try {
                val exists = FirebaseFirestore.getInstance()
                    .collection("users")
                    .whereEqualTo("username", username)
                    .get()
                    .await()
                    .isEmpty.not()
                _registrationState.value = _registrationState.value.copy(
                    isUsernameAvailable = !exists,
                    usernameError = if (exists) "Username taken" else null
                )
            } catch (e: Exception) {
                _registrationState.value = _registrationState.value.copy(
                    isUsernameAvailable = null,
                    usernameError = "Error checking username"
                )
            }
        }
    }

    fun sendOtp(activity: Activity) {
        val phone = _registrationState.value.phone
        val country = _registrationState.value.country
        val phoneNumber = if (country == "IN") "+91$phone" else "+234$phone"
        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    viewModelScope.launch {
                        signInWithPhoneAuthCredential(credential)
                    }
                }
                override fun onVerificationFailed(e: FirebaseException) {
                    _registrationState.value = _registrationState.value.copy(otpError = e.localizedMessage)
                }
                override fun onCodeSent(vid: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = vid
                    resendToken = token
                    // UI: show OTP input
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp() {
        val otp = _registrationState.value.otp
        val vid = verificationId ?: return
        val credential = PhoneAuthProvider.getCredential(vid, otp)
        viewModelScope.launch {
            signInWithPhoneAuthCredential(credential)
        }
    }

    private suspend fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        try {
            FirebaseAuth.getInstance().signInWithCredential(credential).await()
            _registrationState.value = _registrationState.value.copy(isPhoneVerified = true, otpError = null)
        } catch (e: Exception) {
            _registrationState.value = _registrationState.value.copy(otpError = e.localizedMessage)
        }
    }

    fun submitRegistration() {
        val state = _registrationState.value
        viewModelScope.launch {
            try {
                // 1. Create user in Firebase Auth (email/password)
                if (FirebaseAuth.getInstance().currentUser == null) {
                    val result = authRepository.signUp(state.email, state.password)
                    if (result.isFailure) {
                        _registrationState.value = _registrationState.value.copy(globalError = result.exceptionOrNull()?.localizedMessage)
                        return@launch
                    }
                }
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                // 2. Upload profile picture to Firebase Storage (if present)
                var profilePictureUrl: String? = null
                state.profilePictureUri?.let { uri ->
                    // TODO: Upload to Firebase Storage and get download URL
                    // profilePictureUrl = ...
                }
                // 3. Create user profile in Firestore (ALWAYS use registration state, not just Auth fields)
                val userProfile = hashMapOf(
                    "id" to uid,
                    "email" to state.email,
                    "username" to state.username,
                    "displayName" to state.displayName,
                    "country" to state.country,
                    "currency" to if (state.country == "IN") "INR" else "NGN",
                    "phone" to state.phone,
                    "profilePictureUrl" to (profilePictureUrl ?: state.profilePictureUri ?: ""),
                    "kycStatus" to "pending",
                    "createdAt" to com.google.firebase.Timestamp.now()
                    // ...add other fields as needed
                )
                FirebaseFirestore.getInstance().collection("users").document(uid).set(userProfile).await()
                // 4. Optionally, create wallet document, send verification email, etc.
                // 5. Update state to show success (navigate to next screen, show confetti, etc.)
            } catch (e: Exception) {
                _registrationState.value = _registrationState.value.copy(globalError = e.localizedMessage)
            }
        }
    }

    fun sendOtpForLogin(phone: String, navController: androidx.navigation.NavController) {
        val phoneNumber = if (phone.startsWith("+")) phone else "+91$phone" // Default to India, adjust as needed
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(navController.context as? android.app.Activity ?: return)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    viewModelScope.launch {
                        signInWithPhoneAuthCredentialForLogin(credential)
                    }
                }
                override fun onVerificationFailed(e: FirebaseException) {
                    _error.value = e.localizedMessage
                }
                override fun onCodeSent(vid: String, token: PhoneAuthProvider.ForceResendingToken) {
                    loginVerificationId = vid
                    loginResendToken = token
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtpForLogin(otp: String, navController: androidx.navigation.NavController) {
        val vid = loginVerificationId ?: return
        val credential = PhoneAuthProvider.getCredential(vid, otp)
        viewModelScope.launch {
            signInWithPhoneAuthCredentialForLogin(credential)
        }
    }

    private suspend fun signInWithPhoneAuthCredentialForLogin(credential: PhoneAuthCredential) {
        try {
            firebaseAuth.signInWithCredential(credential).await()
            _isAuthenticated.value = true
            _error.value = null
        } catch (e: Exception) {
            _error.value = e.localizedMessage
            _isAuthenticated.value = false
        }
    }



    // --- Link email/password to phone-auth user ---
    fun linkEmailPasswordToPhoneUser(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        val user = firebaseAuth.currentUser
        val credential = EmailAuthProvider.getCredential(email, password)
        user?.linkWithCredential(credential)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.localizedMessage)
                }
            }
    }

    // Method to manually check Firebase Auth state for debugging
    fun debugAuthState() {
        Log.d("AuthViewModel", "=== DEBUG AUTH STATE ===")
        Log.d("AuthViewModel", "FirebaseAuth instance: $firebaseAuth")
        Log.d("AuthViewModel", "Current user: ${firebaseAuth.currentUser}")
        Log.d("AuthViewModel", "Current user UID: ${firebaseAuth.currentUser?.uid}")
        Log.d("AuthViewModel", "Current user email: ${firebaseAuth.currentUser?.email}")
        Log.d("AuthViewModel", "Current user display name: ${firebaseAuth.currentUser?.displayName}")
        Log.d("AuthViewModel", "Current user phone: ${firebaseAuth.currentUser?.phoneNumber}")
        Log.d("AuthViewModel", "Current user is email verified: ${firebaseAuth.currentUser?.isEmailVerified}")
        Log.d("AuthViewModel", "Current user metadata: ${firebaseAuth.currentUser?.metadata}")
        Log.d("AuthViewModel", "Current user provider data: ${firebaseAuth.currentUser?.providerData}")
        Log.d("AuthViewModel", "Current user tenant ID: ${firebaseAuth.currentUser?.tenantId}")
        Log.d("AuthViewModel", "Current user is anonymous: ${firebaseAuth.currentUser?.isAnonymous}")
        
        // Check if user has any tokens
        firebaseAuth.currentUser?.let { user ->
            user.getIdToken(false).addOnSuccessListener { tokenResult ->
                Log.d("AuthViewModel", "User has valid ID token: ${tokenResult.token != null}")
                Log.d("AuthViewModel", "Token expiration: ${tokenResult.expirationTimestamp}")
            }.addOnFailureListener { e ->
                Log.d("AuthViewModel", "Failed to get ID token: ${e.message}")
            }
        }
        
        Log.d("AuthViewModel", "=== END DEBUG AUTH STATE ===")
    }

    // Method to manually re-check auth state
    fun recheckAuthState() {
        Log.d("AuthViewModel", "=== RECHECK AUTH STATE ===")
        viewModelScope.launch {
            try {
                val currentUser = firebaseAuth.currentUser
                Log.d("AuthViewModel", "Recheck - Current user: $currentUser")
                Log.d("AuthViewModel", "Recheck - User UID: ${currentUser?.uid}")
                
                if (currentUser != null) {
                    Log.d("AuthViewModel", "Recheck - User found, updating state")
                    _currentUser.value = currentUser
                                    _isAuthenticated.value = true
                } else {
                    Log.d("AuthViewModel", "Recheck - No user found")
                    _currentUser.value = null
                                    _isAuthenticated.value = false
                                }
                
                _isAuthStateInitialized.value = true
                Log.d("AuthViewModel", "Recheck - Final state - isAuthenticated: ${_isAuthenticated.value}")
                Log.d("AuthViewModel", "=== RECHECK AUTH STATE COMPLETED ===")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Recheck - Error rechecking auth state", e)
            }
        }
    }

    // Method to test Firebase Auth persistence
    fun testAuthPersistence() {
        Log.d("AuthViewModel", "=== TEST AUTH PERSISTENCE ===")
        viewModelScope.launch {
            try {
                // Check current state
                val currentUser = firebaseAuth.currentUser
                Log.d("AuthViewModel", "Test - Current user before test: $currentUser")
                
                if (currentUser != null) {
                    Log.d("AuthViewModel", "Test - User already exists, testing token refresh")
                    try {
                        val tokenResult = currentUser.getIdToken(true).await() // Force refresh
                        Log.d("AuthViewModel", "Test - Token refresh successful: ${tokenResult.token != null}")
                        Log.d("AuthViewModel", "Test - Token expiration: ${tokenResult.expirationTimestamp}")
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Test - Token refresh failed", e)
                    }
                } else {
                    Log.d("AuthViewModel", "Test - No current user, cannot test persistence")
                }
                
                Log.d("AuthViewModel", "=== TEST AUTH PERSISTENCE COMPLETED ===")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Test - Error testing auth persistence", e)
            }
        }
    }

    // Method to manually check and restore user session from persistence
    private fun checkAndRestoreUserSession() {
        Log.d("AuthViewModel", "=== CHECK AND RESTORE USER SESSION ===")
        viewModelScope.launch {
            try {
                val currentUser = firebaseAuth.currentUser
                Log.d("AuthViewModel", "Check restore - Current user: $currentUser")
                
                if (currentUser != null) {
                    Log.d("AuthViewModel", "Check restore - Found user in Firebase Auth: ${currentUser.uid}")
                    Log.d("AuthViewModel", "Check restore - User email: ${currentUser.email}")
                    Log.d("AuthViewModel", "Check restore - User display name: ${currentUser.displayName}")
                    
                    // Save user data to DataStore
                    saveUserDataToDataStore(currentUser)
                    
                    // Try to get a fresh token to verify the session is still valid
                    try {
                        val tokenResult = currentUser.getIdToken(false).await() // Don't force refresh
                        Log.d("AuthViewModel", "Check restore - Token obtained successfully")
                        Log.d("AuthViewModel", "Check restore - Token: ${tokenResult.token?.take(20)}...")
                        
                        // Update the state with the current user
                        _currentUser.value = currentUser
                        _isAuthenticated.value = true
                        _isAuthStateInitialized.value = true
                        
                        Log.d("AuthViewModel", "Check restore - User session restored successfully")
                        
                    } catch (tokenException: Exception) {
                        Log.e("AuthViewModel", "Check restore - Failed to get token", tokenException)
                        Log.d("AuthViewModel", "Check restore - Token is invalid, user needs to re-authenticate")
                        
                        // Token is invalid, sign out the user
                        firebaseAuth.signOut()
                        clearUserDataFromDataStore()
                        _currentUser.value = null
                        _isAuthenticated.value = false
                        _isAuthStateInitialized.value = true
                    }
                } else {
                    Log.d("AuthViewModel", "Check restore - No user found in Firebase Auth, checking DataStore")
                    
                    // Check DataStore for user data
                    val storedUserId = dataStoreManager.userId.first()
                    val storedUserToken = dataStoreManager.userToken.first()
                    val storedUserEmail = dataStoreManager.userEmail.first()
                    
                    Log.d("AuthViewModel", "Check restore - Stored user ID: $storedUserId")
                    Log.d("AuthViewModel", "Check restore - Stored user token: ${storedUserToken.take(20)}...")
                    
                    if (storedUserId.isNotEmpty() && storedUserToken.isNotEmpty()) {
                        Log.d("AuthViewModel", "Check restore - Found user data in DataStore")
                        Log.d("AuthViewModel", "Check restore - User data exists but Firebase Auth session is lost")
                        Log.d("AuthViewModel", "Check restore - This indicates a session persistence issue")
                        
                        // Don't clear DataStore data immediately - let the user try to restore
                        // We'll keep the DataStore data and let the user attempt to restore their session
                        // This way, if they have a valid token, they can still access the app
                        
                        // For now, we'll set the user as authenticated based on DataStore data
                        // This is a temporary solution until we implement proper token validation
                        Log.d("AuthViewModel", "Check restore - Setting user as authenticated based on DataStore data")
                        
                        // Create a minimal user object from DataStore data
                        val storedUserName = dataStoreManager.userName.first()
                        
                        // Note: This is a workaround. In a production app, you should validate the token
                        // and potentially implement a refresh token mechanism
                        _currentUser.value = null // We don't have a FirebaseUser object
                        _isAuthenticated.value = true // But we have valid DataStore data
                        _isAuthStateInitialized.value = true
                        
                        Log.d("AuthViewModel", "Check restore - User authenticated from DataStore data")
                        Log.d("AuthViewModel", "Check restore - Note: Firebase Auth session needs to be restored")
                        
                    } else {
                        Log.d("AuthViewModel", "Check restore - No user data found in DataStore")
                        _currentUser.value = null
                        _isAuthenticated.value = false
                        _isAuthStateInitialized.value = true
                    }
                    
                }
                
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Check restore - Error checking user session", e)
                _currentUser.value = null
                _isAuthenticated.value = false
                _isAuthStateInitialized.value = true
            }
        }
    }

    // Method to check if user session is being maintained
    fun checkUserSessionPersistence() {
        Log.d("AuthViewModel", "=== CHECK USER SESSION PERSISTENCE ===")
        viewModelScope.launch {
            try {
                val currentUser = firebaseAuth.currentUser
                Log.d("AuthViewModel", "Check session - Current user: $currentUser")
                
                if (currentUser != null) {
                    Log.d("AuthViewModel", "Check session - Found user: ${currentUser.uid}")
                    Log.d("AuthViewModel", "Check session - User email: ${currentUser.email}")
                    Log.d("AuthViewModel", "Check session - User display name: ${currentUser.displayName}")
                    
                    // Check if user session is still valid
                    try {
                        val tokenResult = currentUser.getIdToken(false).await()
                        Log.d("AuthViewModel", "Check session - Token obtained successfully")
                        Log.d("AuthViewModel", "Check session - Token: ${tokenResult.token?.take(20)}...")
                        
                        // Update the state with the current user
                        _currentUser.value = currentUser
                        _isAuthenticated.value = true
                        _isAuthStateInitialized.value = true
                        
                        Log.d("AuthViewModel", "Check session - User session maintained")
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Check session - Failed to get token", e)
                        Log.d("AuthViewModel", "Check session - Token is invalid, user needs to re-authenticate")
                        
                        // Token is invalid, sign out the user
                        firebaseAuth.signOut()
                        _currentUser.value = null
                        _isAuthenticated.value = false
                        _isAuthStateInitialized.value = true
                    }
                } else {
                    Log.d("AuthViewModel", "Check session - No user found in session")
                    _currentUser.value = null
                    _isAuthenticated.value = false
                    _isAuthStateInitialized.value = true
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Check session - Error checking user session", e)
                _currentUser.value = null
                _isAuthenticated.value = false
                _isAuthStateInitialized.value = true
            }
        }
    }

    // Method to check if app data is being cleared
    fun checkAppDataPersistence() {
        Log.d("AuthViewModel", "=== CHECK APP DATA PERSISTENCE ===")
        Log.d("AuthViewModel", "Check app data - Skipping app data persistence check (context not available)")
    }

    // Method to check if Firebase Auth tokens are being stored in local storage
    fun checkTokenStorage() {
        Log.d("AuthViewModel", "=== CHECK TOKEN STORAGE ===")
        viewModelScope.launch {
            try {
                val currentUser = firebaseAuth.currentUser
                Log.d("AuthViewModel", "Check token storage - Current user: $currentUser")
                
                if (currentUser != null) {
                    Log.d("AuthViewModel", "Check token storage - Found user: ${currentUser.uid}")
                    
                    // Check if we can get a token (this tests local storage)
                    try {
                        val tokenResult = currentUser.getIdToken(false).await() // Don't force refresh
                        Log.d("AuthViewModel", "Check token storage - Token obtained from storage: ${tokenResult.token?.take(20)}...")
                        Log.d("AuthViewModel", "Check token storage - Token claims: ${tokenResult.claims}")
                        Log.d("AuthViewModel", "Check token storage - Token expiration: ${tokenResult.expirationTimestamp}")
                        
                        // Check if token is expired
                        val currentTime = System.currentTimeMillis() / 1000
                        val tokenExpiration = tokenResult.expirationTimestamp
                        val isExpired = tokenExpiration < currentTime
                        
                        Log.d("AuthViewModel", "Check token storage - Current time: $currentTime")
                        Log.d("AuthViewModel", "Check token storage - Token expiration: $tokenExpiration")
                        Log.d("AuthViewModel", "Check token storage - Token is expired: $isExpired")
                        
                        if (isExpired) {
                            Log.w("AuthViewModel", "Check token storage - Token is expired, user needs to re-authenticate")
                        } else {
                            Log.d("AuthViewModel", "Check token storage - Token is valid")
                        }
                        
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Check token storage - Failed to get token from storage", e)
                        Log.d("AuthViewModel", "Check token storage - This indicates token is not stored locally")
                    }
                } else {
                    Log.d("AuthViewModel", "Check token storage - No user found, no tokens to check")
                }
                
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Check token storage - Error checking token storage", e)
            }
        }
    }

    // Method to check SharedPreferences and other local storage
    fun checkLocalStorage() {
        Log.d("AuthViewModel", "=== CHECK LOCAL STORAGE ===")
        Log.d("AuthViewModel", "Check local storage - Skipping local storage check (context not available)")
    }

    fun saveUserDataToDataStore(user: FirebaseUser) {
        Log.d("AuthViewModel", "=== SAVE USER DATA TO DATASTORE ===")
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Save DataStore - User ID: ${user.uid}")
                Log.d("AuthViewModel", "Save DataStore - User email: ${user.email}")
                Log.d("AuthViewModel", "Save DataStore - User display name: ${user.displayName}")
                Log.d("AuthViewModel", "Save DataStore - User phone: ${user.phoneNumber}")
                
                // Save user data to DataStore
                dataStoreManager.setUserId(user.uid)
                dataStoreManager.setUserEmail(user.email ?: "")
                dataStoreManager.setUserName(user.displayName ?: "")
                dataStoreManager.setUserPhone(user.phoneNumber ?: "")
                dataStoreManager.setUserProfilePic(user.photoUrl?.toString() ?: "")
                dataStoreManager.setUserRole("user")
                dataStoreManager.setUserStatus("active")
                dataStoreManager.setUserCreatedAt(user.metadata?.creationTimestamp?.toString() ?: "")
                dataStoreManager.setUserUpdatedAt(user.metadata?.lastSignInTimestamp?.toString() ?: "")
                dataStoreManager.setUserLastLogin(user.metadata?.lastSignInTimestamp?.toString() ?: "")
                
                // Get and save the token
                try {
                    val tokenResult = user.getIdToken(false).await()
                    dataStoreManager.setUserToken(tokenResult.token ?: "")
                    Log.d("AuthViewModel", "Save DataStore - Token saved: ${tokenResult.token?.take(20)}...")
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Save DataStore - Failed to get token", e)
                }
                
                Log.d("AuthViewModel", "Save DataStore - User data saved successfully")
                
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Save DataStore - Error saving user data", e)
            }
        }
    }

    fun clearUserDataFromDataStore() {
        Log.d("AuthViewModel", "=== CLEAR USER DATA FROM DATASTORE ===")
        viewModelScope.launch {
            try {
                // Clear all user data from DataStore
                dataStoreManager.setUserId("")
                dataStoreManager.setUserEmail("")
                dataStoreManager.setUserName("")
                dataStoreManager.setUserPhone("")
                dataStoreManager.setUserProfilePic("")
                dataStoreManager.setUserToken("")
                dataStoreManager.setUserRole("")
                dataStoreManager.setUserStatus("")
                dataStoreManager.setUserCreatedAt("")
                dataStoreManager.setUserUpdatedAt("")
                dataStoreManager.setUserLastLogin("")
                
                Log.d("AuthViewModel", "Clear DataStore - User data cleared successfully")
                
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Clear DataStore - Error clearing user data", e)
            }
        }
    }

    // Method to check DataStore data
    fun checkDataStoreData() {
        Log.d("AuthViewModel", "=== CHECK DATASTORE DATA ===")
        viewModelScope.launch {
            try {
                // Use the debug method from DataStoreManager
                dataStoreManager.debugDataStoreData()
                
                // Also check individual values for comparison
                val userId = dataStoreManager.userId.first()
                val userEmail = dataStoreManager.userEmail.first()
                val userName = dataStoreManager.userName.first()
                val userToken = dataStoreManager.userToken.first()
                val userRole = dataStoreManager.userRole.first()
                val userStatus = dataStoreManager.userStatus.first()
                
                Log.d("AuthViewModel", "Check DataStore - User ID: $userId")
                Log.d("AuthViewModel", "Check DataStore - User email: $userEmail")
                Log.d("AuthViewModel", "Check DataStore - User name: $userName")
                Log.d("AuthViewModel", "Check DataStore - User token: ${userToken.take(20)}...")
                Log.d("AuthViewModel", "Check DataStore - User role: $userRole")
                Log.d("AuthViewModel", "Check DataStore - User status: $userStatus")
                
                val hasDataStoreData = userId.isNotEmpty() && userToken.isNotEmpty()
                Log.d("AuthViewModel", "Check DataStore - Has user data: $hasDataStoreData")
                
                // Check if this should affect authentication state
                if (hasDataStoreData && !_isAuthenticated.value) {
                    Log.d("AuthViewModel", "Check DataStore - Found DataStore data but user not authenticated, considering session restore")
                    _isAuthStateInitialized.value = true
                }
                
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Check DataStore - Error checking DataStore data", e)
            }
        }
    }

    // Method to get user ID from DataStore when Firebase session is lost
    suspend fun getUserIdFromDataStore(): String? {
        return try {
            val storedUserId = dataStoreManager.userId.first()
            if (storedUserId.isNotEmpty()) {
                Log.d("AuthViewModel", "getUserIdFromDataStore - Found user ID: $storedUserId")
                storedUserId
            } else {
                Log.d("AuthViewModel", "getUserIdFromDataStore - No user ID found in DataStore")
                null
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "getUserIdFromDataStore - Error getting user ID from DataStore", e)
            null
        }
    }

    fun createTestUser() {
        Log.d("AuthViewModel", "=== CREATING TEST USER ===")
        signUp("test@example.com", "testpassword123") { success ->
            if (success) {
                Log.d("AuthViewModel", "Test user created successfully")
            } else {
                Log.e("AuthViewModel", "Failed to create test user")
            }
        }
    }

    fun testSignIn() {
        Log.d("AuthViewModel", "=== TESTING SIGN IN ===")
        signIn("test@example.com", "testpassword123") { success ->
            if (success) {
                Log.d("AuthViewModel", "Test sign in successful")
            } else {
                Log.e("AuthViewModel", "Test sign in failed")
            }
        }
    }
} 