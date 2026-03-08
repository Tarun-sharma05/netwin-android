package com.cehpoint.netwin.presentation.viewmodels


import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cehpoint.netwin.data.model.KycStatus
import com.cehpoint.netwin.data.model.ManualUpiDeposit
import com.cehpoint.netwin.data.model.OfflineOperation
import com.cehpoint.netwin.data.model.PaginationParams
import com.cehpoint.netwin.data.model.PaymentMethod
import com.cehpoint.netwin.data.model.PendingDeposit
import com.cehpoint.netwin.data.model.Transaction
import com.cehpoint.netwin.data.model.UpiSettings
import com.cehpoint.netwin.data.model.WithdrawalRequest
import com.cehpoint.netwin.data.repository.WalletRepositoryImpl
import com.cehpoint.netwin.domain.repository.AdminConfigRepository
import com.cehpoint.netwin.domain.repository.WalletRepository
import com.cehpoint.netwin.utils.KYCMonitor
import com.cehpoint.netwin.utils.NetworkStateMonitor
import com.cehpoint.netwin.utils.OfflineQueueManager
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// Dialog state for controlling payment-related UI across configuration changes and process death
@Immutable
sealed class PaymentDialogState {
    object Closed : PaymentDialogState()
    data class Error(val message: String) : PaymentDialogState()
    data class NgnDeposit(val amount: Double) : PaymentDialogState()
    data class Withdraw(val amount: Double) : PaymentDialogState()
    data class UpiPayment(
        val amount: Double,
        val upiId: String?,
        val merchantName: String?,
        val lastLaunchedAppPackage: String? = null
    ) : PaymentDialogState()
}

data class WalletUiState(
    val balance: Double = 0.0,
    val selectedAmount: Double = 0.0,
    val customAmount: String = "",
    val upiTransactionId: String = "",
    val upiTransactionIdError: String? = null,
    val isSubmittingDeposit: Boolean = false,
    val depositSubmitted: Boolean = false,
    val error: String? = null,
    val pendingDeposits: List<ManualUpiDeposit> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val paymentScreenshot: String? = "",
    // UPI Settings from admin_config
    val upiSettings: UpiSettings? = null,
    val isLoadingUpiSettings: Boolean = true
) {
    val canSubmitDeposit: Boolean
        get() {
            val amount = customAmount.toDoubleOrNull() ?: selectedAmount
            return upiTransactionId.matches(Regex("^[0-9]{12}$")) && 
                  !paymentScreenshot.isNullOrBlank() &&
                  !isSubmittingDeposit &&
                  upiSettings?.isActive == true &&
                  amount > 0.0
        }
}

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val adminConfigRepository: AdminConfigRepository,
    private val networkStateMonitor: NetworkStateMonitor,
    private val offlineQueueManager: OfflineQueueManager,
    private val kycMonitor: KYCMonitor
) : ViewModel() {

    private val _walletBalance = MutableStateFlow(0.0)
    val walletBalance: StateFlow<Double> = _walletBalance.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    // Pagination state
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMoreTransactions = MutableStateFlow(true)
    val hasMoreTransactions: StateFlow<Boolean> = _hasMoreTransactions.asStateFlow()

    private val _lastTransactionDocument = MutableStateFlow<String?>(null)
    val lastTransactionDocument: StateFlow<String?> = _lastTransactionDocument.asStateFlow()

    private val _pendingDeposits = MutableStateFlow<List<ManualUpiDeposit>>(emptyList())
    val pendingDeposits: StateFlow<List<ManualUpiDeposit>> = _pendingDeposits.asStateFlow()

    private val _withdrawalRequests = MutableStateFlow<List<WithdrawalRequest>>(emptyList())
    val withdrawalRequests: StateFlow<List<WithdrawalRequest>> = _withdrawalRequests.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _withdrawableBalance = MutableStateFlow(0.0)
    val withdrawableBalance: StateFlow<Double> = _withdrawableBalance.asStateFlow()

    private val _bonusBalance = MutableStateFlow(0.0)
    val bonusBalance: StateFlow<Double> = _bonusBalance.asStateFlow()

    // UPI Settings from admin_config
    private val _upiSettings = MutableStateFlow<UpiSettings?>(null)
    val upiSettings: StateFlow<UpiSettings?> = _upiSettings.asStateFlow()

    // Payment dialog state to survive lifecycle changes
    private val _paymentDialogState = MutableStateFlow<PaymentDialogState>(PaymentDialogState.Closed)
    val paymentDialogState: StateFlow<PaymentDialogState> = _paymentDialogState.asStateFlow()

    // Network state
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    // KYC state
    private val _kycStatus = MutableStateFlow(KycStatus.PENDING)
    val kycStatus: StateFlow<KycStatus> = _kycStatus.asStateFlow()

    private val _withdrawalLimit = MutableStateFlow(0.0)
    val withdrawalLimit: StateFlow<Double> = _withdrawalLimit.asStateFlow()

    // Offline operations state
    private val _pendingOfflineOperations = MutableStateFlow<List<OfflineOperation>>(emptyList())
    val pendingOfflineOperations: StateFlow<List<OfflineOperation>> = _pendingOfflineOperations.asStateFlow()

    // Listener management for proper cleanup
    private var walletListeners: List<kotlinx.coroutines.Job> = emptyList()
    private var currentUserId: String? = null

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    init {
        // Load UPI settings from admin_config
        loadUpiSettings()
        
        // Monitor network state
        viewModelScope.launch {
            networkStateMonitor.observeNetworkState()
                .catch { e ->
                    Log.e("WalletViewModel", "Network state monitoring error", e)
                    _isOnline.value = false
                }
                .collect { isConnected ->
                    _isOnline.value = isConnected
                    Log.d("WalletViewModel", "Network state changed: $isConnected")

                    // Show offline message if network is lost
                    if (!isConnected) {
                        _error.value = "You are currently offline. Some features may be unavailable."
                    } else {
                        // Clear offline error when back online
                        if (_error.value?.contains("offline") == true) {
                            _error.value = null
                        }

                        // Process offline operations when back online
                        processOfflineOperations()
                    }
                }
        }

        // Monitor offline operations
        viewModelScope.launch {
            offlineQueueManager.getPendingOperations()
                .catch { e ->
                    Log.e("WalletViewModel", "Offline operations monitoring error", e)
                }
                .collect { operations ->
                    _pendingOfflineOperations.value = operations
                    Log.d("WalletViewModel", "Pending offline operations: ${operations.size}")
                }
        }
    }

    /**
     * Load UPI settings from admin_config/upi_settings
     */
    private fun loadUpiSettings() {
        Log.d("WalletViewModel", "=== STARTING UPI SETTINGS LOAD ===")
        viewModelScope.launch {
            try {
                Log.d("WalletViewModel", "Calling adminConfigRepository.getUpiSettings()")
                adminConfigRepository.getUpiSettings()
                    .catch { e ->
                        Log.e("WalletViewModel", "❌ Error in UPI settings flow", e)
                        _upiSettings.value = null
                        updateUiState { it.copy(isLoadingUpiSettings = false) }
                    }
                    .collect { settings ->
                        Log.d("WalletViewModel", "✅ UPI settings received from repository: $settings")
                        if (settings != null) {
                            Log.d("WalletViewModel", "UPI Details - ID: '${settings.upiId}', Active: ${settings.isActive}, DisplayName: '${settings.displayName}'")
                        } else {
                            Log.w("WalletViewModel", "⚠️ UPI settings is NULL")
                        }
                        _upiSettings.value = settings
                        updateUiState { 
                            it.copy(
                                upiSettings = settings,
                                isLoadingUpiSettings = false
                            )
                        }
                        Log.d("WalletViewModel", "UI State updated with UPI settings")
                    }
            } catch (e: Exception) {
                Log.e("WalletViewModel", "❌ Exception loading UPI settings", e)
                _upiSettings.value = null
                updateUiState { it.copy(isLoadingUpiSettings = false) }
            }
        }
    }

    /**
     * Helper method to update UI state
     */
    private fun updateUiState(update: (WalletUiState) -> WalletUiState) {
        _uiState.update(update)
    }

    private fun mapExceptionToUserMessage(throwable: Throwable): String {
        val rawMessage = throwable.message?.lowercase() ?: ""
        return when {
            "network" in rawMessage || "offline" in rawMessage || "internet" in rawMessage ->
                "Network issue detected. Please check your internet connection and try again."
            "timeout" in rawMessage ->
                "Request timed out. Please try again."
            "duplicate" in rawMessage ->
                "This looks like a duplicate request. Please wait a moment and check your wallet."
            "insufficient" in rawMessage ->
                throwable.message ?: "Insufficient balance for this operation."
            "unsupported currency" in rawMessage ->
                "Selected currency is not supported for this operation."
            else -> throwable.message ?: "Something went wrong. Please try again."
        }
    }

    fun createDepositRequest(userId: String, amount: Double, upiRefId: String, userUpiId: String, screenshotUrl: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val deposit = PendingDeposit(
                    userId = userId,
                    amount = amount,
                    currency = "INR",
                    upiRefId = upiRefId,
                    userUpiId = userUpiId,
                    screenshotUrl = screenshotUrl,
                    paymentMethod = PaymentMethod.UPI,
                    userCountry = "IN"
                )

                if (_isOnline.value) {
                    // Online: Try to create deposit directly
                    val result = walletRepository.createPendingDeposit(deposit)
                    result.onSuccess { id ->
                        Log.d("WalletViewModel", "Deposit request created successfully: $id")
                    }.onFailure { e ->
                        _error.value = "Failed to create deposit request: ${e.message}"
                        Log.e("WalletViewModel", "Failed to create deposit request", e)
                    }
                } else {
                    // Offline: Queue the operation
                    offlineQueueManager.queueDepositOperation(deposit)
                    _error.value = "Deposit request queued for when you're back online"
                    Log.d("WalletViewModel", "Deposit request queued for offline processing")
                }
            } catch (e: Exception) {
                _error.value = "Failed to create deposit request: ${mapExceptionToUserMessage(e)}"
                Log.e("WalletViewModel", "Error creating deposit request", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadWithdrawalRequests(userId: String) {
        viewModelScope.launch {
            walletRepository.getWithdrawalRequests(userId)
                .catch { e ->
                    _error.value = "Failed to load withdrawal requests: ${e.message}"
                    Log.e("WalletViewModel", "Failed to load withdrawal requests", e)
                }
                .collect { _withdrawalRequests.value = it }
        }
    }

    fun createWithdrawalRequest(userId: String, amount: Double, paymentMethod: PaymentMethod, bankName: String?, accountNumber: String?, accountName: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val withdrawal = WithdrawalRequest(
                    userId = userId,
                    amount = amount,
                    currency = "INR",
                    paymentMethod = paymentMethod,
                    bankName = bankName,
                    accountNumber = accountNumber,
                    accountName = accountName,
                    userCountry = "IN"
                )

                if (_isOnline.value) {
                    // Online: Try to create withdrawal directly
                    val result = walletRepository.createWithdrawalRequest(withdrawal)
                    result.onSuccess { id ->
                        Log.d("WalletViewModel", "Withdrawal request created successfully: $id")
                    }.onFailure { e ->
                        _error.value = "Failed to create withdrawal request: ${e.message}"
                        Log.e("WalletViewModel", "Failed to create withdrawal request", e)
                    }
                } else {
                    // Offline: Queue the operation
                    offlineQueueManager.queueWithdrawalOperation(withdrawal)
                    _error.value = "Withdrawal request queued for when you're back online"
                    Log.d("WalletViewModel", "Withdrawal request queued for offline processing")
                }
            } catch (e: Exception) {
                _error.value = "Failed to create withdrawal request: ${mapExceptionToUserMessage(e)}"
                Log.e("WalletViewModel", "Error creating withdrawal request", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun processOfflineOperations() {
        try {
            Log.d("WalletViewModel", "Processing offline operations")
            offlineQueueManager.processOfflineOperations(
                onDepositCreated = { deposit ->
                    walletRepository.createPendingDeposit(deposit)
                },
                onWithdrawalCreated = { withdrawal ->
                    walletRepository.createWithdrawalRequest(withdrawal)
                }
            )
        } catch (e: Exception) {
            Log.e("WalletViewModel", "Error processing offline operations", e)
        }
    }

    fun loadWalletData(userId: String) {
        if (currentUserId == userId) {
            Log.d("WalletViewModel", "Wallet data already loaded for user $userId")
            return
        }

        clearListeners()
        currentUserId = userId
        _isLoading.value = true
        _error.value = null

        // Reset pagination state
        _transactions.value = emptyList()
        _hasMoreTransactions.value = true
        _lastTransactionDocument.value = null
        _isLoadingMore.value = false

        Log.d("WalletViewModel", "Loading wallet data for user $userId")

        val listeners = mutableListOf<kotlinx.coroutines.Job>()

        // Balance updates with error handling
        listeners.add(viewModelScope.launch {
            walletRepository.getWalletBalance(userId)
                .catch { e ->
                    _error.value = "Failed to load wallet balance: ${e.message}"
                    Log.e("WalletViewModel", "Wallet balance listener error", e)
                }
                .collect { balance ->
                    _walletBalance.value = balance
                    Log.d("WalletViewModel", "Wallet balance updated: $balance")
                }
        })

        // Load initial transactions with pagination
        listeners.add(viewModelScope.launch {
            loadInitialTransactions(userId)
        })

        // Pending deposits with error handling
        listeners.add(viewModelScope.launch {
            walletRepository.getPendingDeposits(userId)
                .catch { e ->
                    _error.value = "Failed to load pending deposits: ${e.message}"
                    Log.e("WalletViewModel", "Pending deposits listener error", e)
                }
                .collect { deposits ->
                    _pendingDeposits.value = deposits
                    Log.d("WalletViewModel", "Pending deposits updated: ${deposits.size} items")
                }
        })

        // KYC status monitoring
        listeners.add(viewModelScope.launch {
            kycMonitor.monitorKYCStatus(userId)
                .catch { e ->
                    _error.value = "Failed to load KYC status: ${e.message}"
                    Log.e("WalletViewModel", "KYC status listener error", e)
                }
                .collect { kycStatus ->
                    _kycStatus.value = kycStatus
                    _withdrawalLimit.value = kycMonitor.getWithdrawalLimit(kycStatus)
                    Log.d("WalletViewModel", "KYC status updated: $kycStatus, limit: ${kycMonitor.getWithdrawalLimit(kycStatus)}")
                }
        })

        // Withdrawable & bonus balances, and withdrawal requests
        if (walletRepository is WalletRepositoryImpl) {
            listeners.add(viewModelScope.launch {
                walletRepository.getWithdrawableBalance(userId)
                    .catch { e ->
                        _error.value = "Failed to load withdrawable balance: ${e.message}"
                        Log.e("WalletViewModel", "Withdrawable balance listener error", e)
                    }
                    .collect { balance ->
                        _withdrawableBalance.value = balance
                        Log.d("WalletViewModel", "Withdrawable balance updated: $balance")
                    }
            })

            listeners.add(viewModelScope.launch {
                walletRepository.getBonusBalance(userId)
                    .catch { e ->
                        _error.value = "Failed to load bonus balance: ${e.message}"
                        Log.e("WalletViewModel", "Bonus balance listener error", e)
                    }
                    .collect { balance ->
                        _bonusBalance.value = balance
                        Log.d("WalletViewModel", "Bonus balance updated: $balance")
                    }
            })

            listeners.add(viewModelScope.launch {
                walletRepository.getWithdrawalRequests(userId)
                    .catch { e ->
                        _error.value = "Failed to load withdrawal requests: ${e.message}"
                        Log.e("WalletViewModel", "Withdrawal requests listener error", e)
                    }
                    .collect { requests ->
                        _withdrawalRequests.value = requests
                        Log.d("WalletViewModel", "Withdrawal requests updated: ${requests.size} items")
                    }
            })
        }

        walletListeners = listeners
        _isLoading.value = false
    }

    fun loadMoreTransactions() {
        val userId = currentUserId ?: return
        if (_isLoadingMore.value || !_hasMoreTransactions.value) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            _error.value = null

            try {
                val params = PaginationParams(
                    pageSize = 20,
                    lastDocument = _lastTransactionDocument.value,
                    loadMore = true
                )

                walletRepository.getTransactionsPaginated(
                    userId,
                    params,
                    _lastTransactionDocument.value
                )
                    .catch { e ->
                        _error.value =
                            "Failed to load more transactions: ${mapExceptionToUserMessage(e)}"
                        Log.e("WalletViewModel", "Failed to load more transactions", e)
                    }
                    .collect { paginationResult ->
                        val currentTransactions = _transactions.value.toMutableList()
                        currentTransactions.addAll(paginationResult.items)
                        _transactions.value = currentTransactions
                        _hasMoreTransactions.value = paginationResult.hasMore
                        _lastTransactionDocument.value = paginationResult.lastDocument

                        Log.d(
                            "WalletViewModel",
                            "Loaded ${paginationResult.items.size} more transactions. Total: ${currentTransactions.size}, hasMore: ${paginationResult.hasMore}"
                        )
                    }
            } catch (e: Exception) {
                _error.value = "Failed to load more transactions: ${mapExceptionToUserMessage(e)}"
                Log.e("WalletViewModel", "Error loading more transactions", e)
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    private suspend fun loadInitialTransactions(userId: String) {
        try {
            val params = PaginationParams(
                pageSize = 20,
                lastDocument = null,
                loadMore = false
            )

            walletRepository.getTransactionsPaginated(userId, params)
                .catch { e ->
                    _error.value =
                        "Failed to load transactions: ${mapExceptionToUserMessage(e)}"
                    Log.e("WalletViewModel", "Failed to load initial transactions", e)
                }
                .collect { paginationResult ->
                    _transactions.value = paginationResult.items
                    _hasMoreTransactions.value = paginationResult.hasMore
                    _lastTransactionDocument.value = paginationResult.lastDocument

                    Log.d(
                        "WalletViewModel",
                        "Initial transactions loaded: ${paginationResult.items.size} items, hasMore: ${paginationResult.hasMore}"
                    )
                }
        } catch (e: Exception) {
            _error.value = "Failed to load transactions: ${mapExceptionToUserMessage(e)}"
            Log.e("WalletViewModel", "Error loading initial transactions", e)
        }
    }

    private fun setupRealtimeListeners(userId: String) {
        val listeners = mutableListOf<kotlinx.coroutines.Job>()

        // Balance updates with error handling
        listeners.add(viewModelScope.launch {
            walletRepository.getWalletBalance(userId)
                .catch { e ->
                    _error.value = "Failed to load wallet balance: ${e.message}"
                    Log.e("WalletViewModel", "Wallet balance listener error", e)
                }
                .collect { balance ->
                    _walletBalance.value = balance
                    Log.d("WalletViewModel", "Wallet balance updated: $balance")
                }
        })

        // Transaction history with error handling
        listeners.add(viewModelScope.launch {
            walletRepository.getTransactions(userId)
                .catch { e ->
                    _error.value = "Failed to load transactions: ${e.message}"
                    Log.e("WalletViewModel", "Transactions listener error", e)
                }
                .collect { transactions ->
                    _transactions.value = transactions
                    Log.d("WalletViewModel", "Transactions updated: ${transactions.size} items")
                }
        })

        // Pending deposits with error handling
        listeners.add(viewModelScope.launch {
            walletRepository.getPendingDeposits(userId)
                .catch { e ->
                    _error.value = "Failed to load pending deposits: ${e.message}"
                    Log.e("WalletViewModel", "Pending deposits listener error", e)
                }
                .collect { deposits ->
                    _pendingDeposits.value = deposits
                    Log.d("WalletViewModel", "Pending deposits updated: ${deposits.size} items")
                }
        })

        // Withdrawable & bonus balances, and withdrawal requests
        if (walletRepository is WalletRepositoryImpl) {
            listeners.add(viewModelScope.launch {
                walletRepository.getWithdrawableBalance(userId)
                    .catch { e ->
                        _error.value = "Failed to load withdrawable balance: ${e.message}"
                        Log.e("WalletViewModel", "Withdrawable balance listener error", e)
                    }
                    .collect { balance ->
                        _withdrawableBalance.value = balance
                        Log.d("WalletViewModel", "Withdrawable balance updated: $balance")
                    }
            })

            listeners.add(viewModelScope.launch {
                walletRepository.getBonusBalance(userId)
                    .catch { e ->
                        _error.value = "Failed to load bonus balance: ${e.message}"
                        Log.e("WalletViewModel", "Bonus balance listener error", e)
                    }
                    .collect { balance ->
                        _bonusBalance.value = balance
                        Log.d("WalletViewModel", "Bonus balance updated: $balance")
                    }
            })

            listeners.add(viewModelScope.launch {
                walletRepository.getWithdrawalRequests(userId)
                    .catch { e ->
                        _error.value = "Failed to load withdrawal requests: ${e.message}"
                        Log.e("WalletViewModel", "Withdrawal requests listener error", e)
                    }
                    .collect { requests ->
                        _withdrawalRequests.value = requests
                        Log.d(
                            "WalletViewModel",
                            "Withdrawal requests updated: ${requests.size} items"
                        )
                    }
            })
        }

        walletListeners = listeners
    }

    private fun clearListeners() {
        walletListeners.forEach { it.cancel() }
        walletListeners = emptyList()
        Log.d("WalletViewModel", "All wallet listeners cleared")
    }

    fun clearError() {
        _error.value = null
    }

    // Exposed helpers for UI to control dialog state safely
    fun openUpiPaymentDialog(
        amount: Double,
        upiId: String?,
        merchantName: String?,
        lastAppPackage: String?
    ) {
        _paymentDialogState.value = PaymentDialogState.UpiPayment(
            amount = amount,
            upiId = upiId,
            merchantName = merchantName,
            lastLaunchedAppPackage = lastAppPackage
        )
    }

    fun openNgnDepositDialog(amount: Double) {
        _paymentDialogState.value = PaymentDialogState.NgnDeposit(amount)
    }

    fun openWithdrawDialog(amount: Double) {
        _paymentDialogState.value = PaymentDialogState.Withdraw(amount)
    }

    fun closePaymentDialog() {
        _paymentDialogState.value = PaymentDialogState.Closed
    }

    fun setPaymentDialogError(message: String) {
        _paymentDialogState.value = PaymentDialogState.Error(message)
    }

    fun updateSelectedAmount(amount: Double) {
        _uiState.update { it.copy(selectedAmount = amount, customAmount = "") }
    }

    fun updateCustomAmount(amount: String) {
        _uiState.update { it.copy(customAmount = amount, selectedAmount = 0.0) }
    }
    
    fun updateUpiTransactionId(transactionId: String) {
        _uiState.update { currentState ->
            currentState.copy(
                upiTransactionId = transactionId,
                upiTransactionIdError = validateUpiTransactionId(transactionId)
            )
        }
    }
    
    fun updatePaymentScreenshot(uri: android.net.Uri?, userId: String) {
        viewModelScope.launch {
            if (uri != null) {
                _isLoading.value = true
                try {
                    Log.d("WalletViewModel", "Uploading payment screenshot for INR deposit")
                    val uploadResult = walletRepository.uploadDepositScreenshot(uri, userId)
                    
                    if (uploadResult.isSuccess) {
                        val screenshotUrl = uploadResult.getOrNull()
                        _uiState.update { it.copy(paymentScreenshot = screenshotUrl) }
                        Log.d("WalletViewModel", "Screenshot uploaded successfully: $screenshotUrl")
                    } else {
                        _error.value = "Failed to upload screenshot: ${uploadResult.exceptionOrNull()?.message}"
                        Log.e("WalletViewModel", "Screenshot upload failed", uploadResult.exceptionOrNull())
                    }
                } catch (e: Exception) {
                    _error.value = "Error uploading screenshot: ${e.message}"
                    Log.e("WalletViewModel", "Error uploading screenshot", e)
                } finally {
                    _isLoading.value = false
                }
            } else {
                _uiState.update { it.copy(paymentScreenshot = null) }
            }
        }
    }

    fun submitManualDeposit() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSubmittingDeposit = true, error = null) }

                val upiSettings = _upiSettings.value
                if (upiSettings == null || !upiSettings.isActive) {
                    _uiState.update {
                        it.copy(
                            isSubmittingDeposit = false,
                            error = "UPI payments are currently unavailable. Please try again later."
                        )
                    }
                    return@launch
                }

                // Calculate final amount: use custom amount if entered, otherwise selected amount
                val finalAmount = _uiState.value.customAmount.toDoubleOrNull() 
                    ?: _uiState.value.selectedAmount

                val deposit = ManualUpiDeposit(
                    userId = getCurrentUserId(),
                    amount = finalAmount,
                    upiTransactionId = _uiState.value.upiTransactionId,
                    paymentScreenshot = _uiState.value.paymentScreenshot,
                    netwinUpiId = upiSettings.upiId, // ✅ Dynamic from admin_config
                    merchantDisplayName = upiSettings.displayName // ✅ Dynamic from admin_config
                )

                Log.d("WalletViewModel", "Submitting deposit with NetWin UPI ID: ${upiSettings.upiId}")

                walletRepository.submitManualDeposit(deposit)
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                isSubmittingDeposit = false,
                                depositSubmitted = true,
                                selectedAmount = 0.0,
                                customAmount = "",
                                upiTransactionId = ""
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isSubmittingDeposit = false,
                                error = error.message
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmittingDeposit = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun loadWalletDataForManualUpi(userId: String) {
        viewModelScope.launch {
            // Load wallet balance
            walletRepository.getWalletBalance(userId)
                .catch { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
                .collect { balance ->
                    _uiState.update { it.copy(balance = balance) }
                }
        }

        viewModelScope.launch {
            // Load pending deposits
            walletRepository.getPendingDeposits(userId)
                .catch { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
                .collect { deposits ->
                    _uiState.update { it.copy(pendingDeposits = deposits) }
                }
        }   

        viewModelScope.launch {
            // Load transaction history
            walletRepository.getTransactionHistory(userId)
                .catch { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
                .collect { transactions ->
                    _uiState.update { it.copy(transactions = transactions) }
                }
        }
    }

    private fun validateUpiTransactionId(transactionId: String): String? {
        return when {
            transactionId.isEmpty() -> null
            !transactionId.matches(Regex("^[0-9]{12}$")) ->
                "UPI Transaction ID must be exactly 12 digits"

            else -> null
        }
    }

    private fun getCurrentUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }
    
    // Upload deposit screenshot to Firebase Storage
    suspend fun uploadDepositScreenshot(uri: android.net.Uri, userId: String): Result<String> {
        return try {
            Log.d("WalletViewModel", "Uploading deposit screenshot")
            walletRepository.uploadDepositScreenshot(uri, userId)
        } catch (e: Exception) {
            Log.e("WalletViewModel", "Error uploading screenshot", e)
            Result.failure(e)
        }
    }
    
    // Create NGN deposit request with Paystack reference
    fun createNgnDepositRequest(
        userId: String,
        amount: Double,
        transactionReference: String,
        screenshotUrl: String,
        currency: String = "NGN"
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val result = walletRepository.createNgnDepositRequest(
                    userId = userId,
                    amount = amount,
                    transactionReference = transactionReference,
                    screenshotUrl = screenshotUrl,
                    currency = currency
                )
                
                if (result.isSuccess) {
                    _error.value = null
                    Log.d("WalletViewModel", "NGN deposit request created: ${result.getOrNull()}")
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to create deposit request"
                    Log.e("WalletViewModel", "Failed to create NGN deposit", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                _error.value = "Failed to create deposit request: ${mapExceptionToUserMessage(e)}"
                Log.e("WalletViewModel", "Error creating NGN deposit request", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun mapExceptionToUserMessage(exception: Exception): String {
        return when (exception) {
            is java.net.SocketTimeoutException,
            is java.net.ConnectException -> "Network connection error. Please check your internet connection."

            is java.io.IOException -> "Network error occurred. Please try again."
            is kotlinx.coroutines.CancellationException -> "Operation was cancelled."
            else -> exception.message ?: "An unknown error occurred. Please try again."
        }
    }

    override fun onCleared() {
        clearListeners()
        currentUserId = null
        super.onCleared()
        Log.d("WalletViewModel", "WalletViewModel cleared")
    }
}