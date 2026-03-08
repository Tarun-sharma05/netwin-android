package com.cehpoint.netwin.domain.repository

import android.net.Uri
import com.cehpoint.netwin.data.model.ManualUpiDeposit
import com.cehpoint.netwin.data.model.Transaction
import com.cehpoint.netwin.data.model.WithdrawalRequest
import com.cehpoint.netwin.data.model.PaginatedResult
import com.cehpoint.netwin.data.model.PaginationParams
import com.cehpoint.netwin.data.model.PendingDeposit
import kotlinx.coroutines.flow.Flow

interface WalletRepository {
    suspend fun submitManualDeposit(deposit: ManualUpiDeposit): Result<String>
    suspend fun getWalletBalance(userId: String): Flow<Double>
    suspend fun getPendingDeposits(userId: String): Flow<List<ManualUpiDeposit>>
    suspend fun getTransactionHistory(userId: String): Flow<List<Transaction>>
    
    // Missing methods that WalletViewModel is calling
    suspend fun createPendingDeposit(deposit: PendingDeposit): Result<String>
    suspend fun createWithdrawalRequest(request: WithdrawalRequest): Result<String>
    suspend fun getWithdrawalRequests(userId: String): Flow<List<WithdrawalRequest>>
    suspend fun getWithdrawableBalance(userId: String): Flow<Double>
    suspend fun getBonusBalance(userId: String): Flow<Double>
    suspend fun getTransactionsPaginated(userId: String, limit: PaginationParams, lastDocument: String? = null): Flow<PaginatedResult<Transaction>>
    suspend fun getTransactions(userId: String): Flow<List<Transaction>>
    
    // Firebase Storage upload for payment screenshots
    suspend fun uploadDepositScreenshot(uri: Uri, userId: String): Result<String>
    
    // Create NGN deposit request with Paystack reference
    suspend fun createNgnDepositRequest(
        userId: String,
        amount: Double,
        transactionReference: String,
        screenshotUrl: String,
        currency: String = "NGN"
    ): Result<String>
}