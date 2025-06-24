package com.cehpoint.netwin.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cehpoint.netwin.data.model.PendingDeposit
import com.cehpoint.netwin.data.model.Transaction
import com.cehpoint.netwin.data.model.WithdrawalRequest
import com.cehpoint.netwin.data.model.UserDetails
import com.cehpoint.netwin.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val repository: WalletRepository
) : ViewModel() {
    private val _walletBalance = MutableStateFlow(0.0)
    val walletBalance: StateFlow<Double> = _walletBalance.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

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

    fun loadWalletData(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                repository.getWalletBalance(userId)
                    .catch { e -> _error.value = e.message }
                    .firstOrNull()?.let { _walletBalance.value = it }
                repository.getTransactions(userId)
                    .catch { e -> _error.value = e.message }
                    .firstOrNull()?.let { _transactions.value = it }
                repository.getPendingDeposits(userId)
                    .catch { e -> _error.value = e.message }
                    .firstOrNull()?.let { _pendingDeposits.value = it }
                // Real-time listeners
                setupRealtimeListeners(userId)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun setupRealtimeListeners(userId: String) {
        viewModelScope.launch {
            repository.getWalletBalance(userId)
                .catch { e -> _error.value = e.message }
                .collect { _walletBalance.value = it }
        }
        viewModelScope.launch {
            repository.getTransactions(userId)
                .catch { e -> _error.value = e.message }
                .collect { _transactions.value = it }
        }
        viewModelScope.launch {
            repository.getPendingDeposits(userId)
                .catch { e -> _error.value = e.message }
                .collect { _pendingDeposits.value = it }
        }
        viewModelScope.launch {
            repository.getWalletBalance(userId)
                .catch { e -> _error.value = e.message }
                .collect { _withdrawableBalance.value = it }
        }
        viewModelScope.launch {
            if (repository is com.cehpoint.netwin.data.repository.WalletRepositoryImpl) {
                repository.getBonusBalance(userId)
                    .catch { e -> _error.value = e.message }
                    .collect { _bonusBalance.value = it }
            }
        }
    }

    fun createDepositRequest(userId: String, amount: Double, upiRefId: String, userUpiId: String, screenshotUrl: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val deposit = PendingDeposit(
                    userId = userId,
                    amount = amount,
                    upiRefId = upiRefId,
                    userUpiId = userUpiId,
                    screenshotUrl = screenshotUrl
                )
                repository.createPendingDeposit(deposit)
                    .onFailure { e -> _error.value = e.message }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadWithdrawalRequests(userId: String) {
        viewModelScope.launch {
            repository.getWithdrawalRequests(userId)
                .catch { e -> _error.value = e.message }
                .collect { _withdrawalRequests.value = it }
        }
    }

    fun createWithdrawalRequest(request: WithdrawalRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (request.amount > _withdrawableBalance.value) {
                    _error.value = "Withdrawal amount exceeds withdrawable balance."
                    _isLoading.value = false
                    return@launch
                }
                repository.createWithdrawalRequest(request)
                    .onFailure { e -> _error.value = e.message }
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
} 