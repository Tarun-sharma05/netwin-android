package com.cehpoint.netwin.domain.repository

import com.cehpoint.netwin.data.model.PaginationParams
import com.cehpoint.netwin.data.model.PaginationResult
import com.cehpoint.netwin.data.model.PendingDeposit
import com.cehpoint.netwin.data.model.Transaction
import com.cehpoint.netwin.data.model.WithdrawalRequest
import kotlinx.coroutines.flow.Flow

interface WalletRepository {
    fun getWalletBalance(userId: String): Flow<Double>
    fun getWithdrawableBalance(userId: String): Flow<Double>
    fun getBonusBalance(userId: String): Flow<Double>
    
    // Transaction methods with pagination support
    fun getTransactions(userId: String): Flow<List<Transaction>>
    suspend fun getTransactionsPaginated(
        userId: String, 
        params: PaginationParams
    ): Result<PaginationResult<Transaction>>
    
    fun getPendingDeposits(userId: String): Flow<List<PendingDeposit>>
    
    suspend fun createPendingDeposit(deposit: PendingDeposit): Result<String>
    suspend fun verifyDeposit(depositId: String, adminId: String): Result<Unit>
    suspend fun rejectDeposit(depositId: String, adminId: String, reason: String): Result<Unit>
    
    suspend fun createTransaction(transaction: Transaction): Result<String>
    suspend fun updateTransactionStatus(transactionId: String, status: String): Result<Unit>
    
    suspend fun checkBalance(userId: String, amount: Double): Boolean
    suspend fun updateBalance(userId: String, amount: Double): Result<Unit>

    // Withdrawal
    suspend fun createWithdrawalRequest(request: WithdrawalRequest): Result<String>
    fun getWithdrawalRequests(userId: String): Flow<List<WithdrawalRequest>>
} 