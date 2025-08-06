package com.cehpoint.netwin.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cehpoint.netwin.data.model.KycStatus
import com.cehpoint.netwin.data.model.OfflineOperation
import com.cehpoint.netwin.data.model.PaginationParams
import com.cehpoint.netwin.data.model.PaymentMethod
import com.cehpoint.netwin.data.model.PendingDeposit
import com.cehpoint.netwin.data.model.Transaction
import com.cehpoint.netwin.data.model.WithdrawalRequest
import com.cehpoint.netwin.data.repository.WalletRepositoryImpl
import com.cehpoint.netwin.domain.repository.WalletRepository
import com.cehpoint.netwin.utils.KYCMonitor
import com.cehpoint.netwin.utils.NetworkStateMonitor
import com.cehpoint.netwin.utils.OfflineQueueManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val repository: WalletRepository,
    private val networkStateMonitor: NetworkStateMonitor,
    private val offlineQueueManager: OfflineQueueManager,
    private val kycMonitor: KYCMonitor
) : ViewModel() {
    
    // State flows
    private val _walletBalance = MutableStateFlow(0.0)
    val walletBalance: StateFlow<Double> = _walletBalance.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    // Pagination state
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMoreTransactions = MutableStateFlow(true)
    val hasMoreTransactions: StateFlow<Boolean> = _hasMoreTransactions.asStateFlow()

    private val _lastTransactionDocument = MutableStateFlow<com.google.firebase.firestore.DocumentSnapshot?>(null)
    val lastTransactionDocument: StateFlow<com.google.firebase.firestore.DocumentSnapshot?> = _lastTransactionDocument.asStateFlow()

    private val _pendingDeposits = MutableStateFlow<List<PendingDeposit>>(emptyList())
    val pendingDeposits: StateFlow<List<PendingDeposit>> = _pendingDeposits.asStateFlow()

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

    init {
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
                    val result = repository.createPendingDeposit(deposit)
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
                _error.value = "Failed to create deposit request: ${e.message}"
                Log.e("WalletViewModel", "Error creating deposit request", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadWithdrawalRequests(userId: String) {
        viewModelScope.launch {
            repository.getWithdrawalRequests(userId)
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
                    val result = repository.createWithdrawalRequest(withdrawal)
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
                _error.value = "Failed to create withdrawal request: ${e.message}"
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
                    repository.createPendingDeposit(deposit)
                },
                onWithdrawalCreated = { withdrawal ->
                    repository.createWithdrawalRequest(withdrawal)
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
            repository.getWalletBalance(userId)
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
            repository.getPendingDeposits(userId)
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
        if (repository is WalletRepositoryImpl) {
            listeners.add(viewModelScope.launch {
                repository.getWithdrawableBalance(userId)
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
                repository.getBonusBalance(userId)
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
                repository.getWithdrawalRequests(userId)
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
                
                val result = repository.getTransactionsPaginated(userId, params)
                result.onSuccess { paginationResult ->
                    val currentTransactions = _transactions.value.toMutableList()
                    currentTransactions.addAll(paginationResult.items)
                    _transactions.value = currentTransactions
                    _hasMoreTransactions.value = paginationResult.hasMore
                    _lastTransactionDocument.value = paginationResult.lastDocument
                    
                    Log.d("WalletViewModel", "Loaded ${paginationResult.items.size} more transactions. Total: ${currentTransactions.size}, hasMore: ${paginationResult.hasMore}")
                }.onFailure { e ->
                    _error.value = "Failed to load more transactions: ${e.message}"
                    Log.e("WalletViewModel", "Failed to load more transactions", e)
                }
            } catch (e: Exception) {
                _error.value = "Failed to load more transactions: ${e.message}"
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
            
            val result = repository.getTransactionsPaginated(userId, params)
            result.onSuccess { paginationResult ->
                _transactions.value = paginationResult.items
                _hasMoreTransactions.value = paginationResult.hasMore
                _lastTransactionDocument.value = paginationResult.lastDocument
                
                Log.d("WalletViewModel", "Initial transactions loaded: ${paginationResult.items.size} items, hasMore: ${paginationResult.hasMore}")
            }.onFailure { e ->
                _error.value = "Failed to load transactions: ${e.message}"
                Log.e("WalletViewModel", "Failed to load initial transactions", e)
            }
        } catch (e: Exception) {
            _error.value = "Failed to load transactions: ${e.message}"
            Log.e("WalletViewModel", "Error loading initial transactions", e)
        }
    }

    private fun setupRealtimeListeners(userId: String) {
        val listeners = mutableListOf<kotlinx.coroutines.Job>()

        // Balance updates with error handling
        listeners.add(viewModelScope.launch {
            repository.getWalletBalance(userId)
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
            repository.getTransactions(userId)
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
            repository.getPendingDeposits(userId)
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
        if (repository is WalletRepositoryImpl) {
            listeners.add(viewModelScope.launch {
                repository.getWithdrawableBalance(userId)
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
                repository.getBonusBalance(userId)
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
                repository.getWithdrawalRequests(userId)
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
    }

    private fun clearListeners() {
        walletListeners.forEach { it.cancel() }
        walletListeners = emptyList()
        Log.d("WalletViewModel", "All wallet listeners cleared")
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        clearListeners()
        currentUserId = null
        super.onCleared()
        Log.d("WalletViewModel", "WalletViewModel cleared")
    }
} 