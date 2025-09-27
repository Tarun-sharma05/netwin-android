package com.cehpoint.netwin.data.repository

import android.util.Log
import com.cehpoint.netwin.data.model.DepositStatus
import com.cehpoint.netwin.data.model.KycStatus
import com.cehpoint.netwin.data.model.PaginationParams
import com.cehpoint.netwin.data.model.PaginationResult
import com.cehpoint.netwin.data.model.PaymentMethod
import com.cehpoint.netwin.data.model.PendingDeposit
import com.cehpoint.netwin.data.model.Transaction
import com.cehpoint.netwin.data.model.TransactionStatus
import com.cehpoint.netwin.data.model.TransactionType
import com.cehpoint.netwin.data.model.User
import com.cehpoint.netwin.data.model.Wallet
import com.cehpoint.netwin.data.model.WithdrawalRequest
import com.cehpoint.netwin.data.model.UserDetails
import com.cehpoint.netwin.data.remote.FirebaseManager
import com.cehpoint.netwin.domain.repository.WalletRepository
import com.cehpoint.netwin.utils.IdempotencyManager
import com.cehpoint.netwin.utils.KYCMonitor
import com.cehpoint.netwin.utils.NGNTransactionUtils
import com.cehpoint.netwin.utils.OfflineQueueManager
import com.cehpoint.netwin.utils.RetryUtils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class WalletRepositoryImpl @Inject constructor(
    private val firebaseManager: FirebaseManager,
    private val offlineQueueManager: OfflineQueueManager,
    private val idempotencyManager: IdempotencyManager,
    private val kycMonitor: KYCMonitor
) : WalletRepository {

    companion object {
        private const val TAG = "WalletRepositoryImpl"
    }

    private val usersCollection = firebaseManager.firestore.collection("users")
    private val walletsCollection = firebaseManager.firestore.collection("wallets")
    private val transactionsCollection = firebaseManager.firestore.collection("wallet_transactions")
    private val pendingDepositsCollection = firebaseManager.firestore.collection("pending_deposits")
    private val pendingWithdrawalsCollection = firebaseManager.firestore.collection("pending_withdrawals")

    override fun getWalletBalance(userId: String): Flow<Double> = callbackFlow {
        val listener = walletsCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error getting wallet balance for user $userId", error)
                    close(error)
                    return@addSnapshotListener                                                      
                }
                
                val wallet = snapshot?.toObject(Wallet::class.java)
                val balance = (wallet?.withdrawableBalance ?: 0.0) + (wallet?.bonusBalance ?: 0.0)
                Log.d(TAG, "Wallet balance updated for user $userId: $balance")
                trySend(balance)
            }
        
        awaitClose { 
            listener.remove()
            Log.d(TAG, "Wallet balance listener removed for user $userId")
        }
    }

    override fun getWithdrawableBalance(userId: String): Flow<Double> = callbackFlow {
        val listener = walletsCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error getting withdrawable balance for user $userId", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                val wallet = snapshot?.toObject(Wallet::class.java)
                val withdrawableBalance = wallet?.withdrawableBalance ?: 0.0
                Log.d(TAG, "Withdrawable balance updated for user $userId: $withdrawableBalance")
                trySend(withdrawableBalance)
            }
        
        awaitClose { 
            listener.remove()
            Log.d(TAG, "Withdrawable balance listener removed for user $userId")
        }
    }

    override fun getBonusBalance(userId: String): Flow<Double> = callbackFlow {
        val listener = walletsCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error getting bonus balance for user $userId", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                val wallet = snapshot?.toObject(Wallet::class.java)
                val bonusBalance = wallet?.bonusBalance ?: 0.0
                Log.d(TAG, "Bonus balance updated for user $userId: $bonusBalance")
                trySend(bonusBalance)
            }
        
        awaitClose { 
            listener.remove()
            Log.d(TAG, "Bonus balance listener removed for user $userId")
        }
    }

    override fun getTransactions(userId: String): Flow<List<Transaction>> = callbackFlow {
        Log.d(TAG, "Starting transaction listeners for user $userId")
        
        // Listen to users/{userId}/transactions subcollection first (where your existing transaction is)
        val userListener = usersCollection.document(userId)
            .collection("transactions")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error getting user transactions for user $userId", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                val userTransactions = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Log.d(TAG, "Processing user transaction ${doc.id} for user $userId")
                        doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to deserialize user transaction ${doc.id}: ${e.message}")
                        null
                    }
                } ?: emptyList()
                
                Log.d(TAG, "User transactions loaded for user $userId: ${userTransactions.size} items")
                trySend(userTransactions)
            }
        
        awaitClose { 
            userListener.remove()
            Log.d(TAG, "User transaction listener removed for user $userId")
        }
    }

    override suspend fun getTransactionsPaginated(
        userId: String, 
        params: PaginationParams
    ): Result<PaginationResult<Transaction>> = try {
        Log.d(TAG, "Loading paginated transactions for user $userId, pageSize: ${params.pageSize}, loadMore: ${params.loadMore}")
        
        val result = RetryUtils.retryWithBackoff(maxAttempts = 3) {
            var query = transactionsCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(params.pageSize.toLong())
            
            // If loading more, start after the last document
            if (params.loadMore && params.lastDocument != null) {
                query = query.startAfter(params.lastDocument)
            }
            
            val snapshot = query.get().await()
            val transactions = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to deserialize transaction ${doc.id}: ${e.message}")
                    null
                }
            }
            
            val hasMore = transactions.size == params.pageSize
            val lastDocument = if (transactions.isNotEmpty()) snapshot.documents.last() else null
            
            Log.d(TAG, "Paginated transactions loaded for user $userId: ${transactions.size} items, hasMore: $hasMore")
            
            PaginationResult(
                items = transactions,
                hasMore = hasMore,
                lastDocument = lastDocument,
                totalCount = transactions.size
            )
        }
        
        result.onSuccess { paginationResult ->
            Log.d(TAG, "Paginated transactions loaded successfully for user $userId")
        }.onFailure { e ->
            Log.e(TAG, "Failed to load paginated transactions for user $userId after retries", e)
        }
        
        result
    } catch (e: Exception) {
        Log.e(TAG, "Failed to load paginated transactions for user $userId", e)
        Result.failure(e)
    }

    override fun getPendingDeposits(userId: String): Flow<List<PendingDeposit>> = callbackFlow {
        val listener = pendingDepositsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", DepositStatus.PENDING)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error getting pending deposits for user $userId", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                val deposits = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(PendingDeposit::class.java)?.copy(requestId = doc.id)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to deserialize deposit ${doc.id}: ${e.message}")
                        null
                    }
                } ?: emptyList()

                Log.d(TAG, "Pending deposits updated for user $userId: ${deposits.size} items")
                trySend(deposits)
            }
        
        awaitClose { 
            listener.remove()
            Log.d(TAG, "Pending deposits listener removed for user $userId")
        }
    }

    override suspend fun createPendingDeposit(deposit: PendingDeposit): Result<String> = try {
        Log.d(TAG, "Creating pending deposit for user ${deposit.userId}, amount: ${deposit.amount}, currency: ${deposit.currency}")
        
        // Validate deposit data
        if (deposit.amount <= 0) {
            throw IllegalArgumentException("Deposit amount must be greater than 0")
        }
        
        if (deposit.userId.isBlank()) {
            throw IllegalArgumentException("User ID cannot be empty")
        }
        
        // Validate currency
        if (!NGNTransactionUtils.validateCurrency(deposit.currency)) {
            throw IllegalArgumentException("Unsupported currency: ${deposit.currency}")
        }
        
        // Validate amount for currency
        if (!NGNTransactionUtils.validateAmountForCurrency(deposit.amount, deposit.currency)) {
            val minAmount = NGNTransactionUtils.getMinimumAmountForCurrency(deposit.currency)
            throw IllegalArgumentException("Amount too low for ${deposit.currency}. Minimum: ${NGNTransactionUtils.formatAmount(minAmount, deposit.currency)}")
        }

        // Check for duplicate operation using idempotency
        val operationData = mapOf(
            "amount" to deposit.amount,
            "currency" to deposit.currency,
            "upiRefId" to deposit.upiRefId,
            "userUpiId" to deposit.userUpiId,
            "paymentMethod" to deposit.paymentMethod.name,
            "userCountry" to deposit.userCountry
        )
        
        if (idempotencyManager.isDuplicateOperation("CREATE_DEPOSIT", deposit.userId, operationData)) {
            throw IllegalArgumentException("Duplicate deposit request detected. Please wait before trying again.")
        }

        // Use retry logic for critical operations
        val result = RetryUtils.retryWithBackoff(maxAttempts = 3) {
            val docRef = pendingDepositsCollection.add(deposit).await()
            docRef.id
        }
        
        result.onSuccess { id ->
            // Store idempotency key on success
            idempotencyManager.storeIdempotencyKey("CREATE_DEPOSIT", deposit.userId, operationData)
            Log.d(TAG, "Pending deposit created successfully with ID: $id")
        }.onFailure { e ->
            Log.e(TAG, "Failed to create pending deposit after retries", e)
        }
        
        result
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create pending deposit", e)
        Result.failure(e)
    }

    override suspend fun verifyDeposit(depositId: String, adminId: String): Result<Unit> = try {
        Log.d(TAG, "Verifying deposit $depositId by admin $adminId")
        
        val result = RetryUtils.retryWithBackoff(maxAttempts = 3) {
            firebaseManager.firestore.runTransaction { transaction ->
                val depositRef = pendingDepositsCollection.document(depositId)
                val deposit = transaction.get(depositRef).toObject(PendingDeposit::class.java)
                    ?: throw Exception("Deposit not found")

                // Check if deposit is already processed
                if (deposit.status != DepositStatus.PENDING) {
                    throw Exception("Deposit is already ${deposit.status.name.lowercase()}")
                }

                // Update deposit status
                transaction.update(depositRef, "status", DepositStatus.APPROVED)
                transaction.update(depositRef, "verifiedBy", adminId)
                transaction.update(depositRef, "verifiedAt", com.google.firebase.Timestamp.now())

                // Update wallet balance atomically
                val walletRef = walletsCollection.document(deposit.userId)
                val wallet = transaction.get(walletRef).toObject(Wallet::class.java)
                    ?: throw Exception("Wallet not found")

                val newWithdrawableBalance = wallet.withdrawableBalance + deposit.amount
                val newTotalBalance = newWithdrawableBalance + wallet.bonusBalance
                
                transaction.update(walletRef, mapOf(
                    "withdrawableBalance" to newWithdrawableBalance,
                    "balance" to newTotalBalance,
                    "currency" to (wallet.currency ?: "INR") // Ensure currency is set
                ))

                // Update user's walletBalance for display
                val userRef = usersCollection.document(deposit.userId)
                transaction.update(userRef, "walletBalance", newTotalBalance)

                // Create transaction record with proper description
                val transactionRecord = Transaction(
                    userId = deposit.userId,
                    amount = deposit.amount,
                    currency = deposit.currency,
                    type = NGNTransactionUtils.getTransactionTypeForPaymentMethod(deposit.paymentMethod, true),
                    status = TransactionStatus.COMPLETED,
                    description = NGNTransactionUtils.getPaymentDescription(deposit.paymentMethod, deposit.amount, deposit.currency),
                    paymentMethod = deposit.paymentMethod,
                    createdAt = com.google.firebase.Timestamp.now()
                )
                val transactionDocRef = transactionsCollection.document()
                transaction.set(transactionDocRef, transactionRecord)
            }.await()
        }
        
        result.onSuccess {
            Log.d(TAG, "Deposit $depositId verified successfully")
        }.onFailure { e ->
            Log.e(TAG, "Failed to verify deposit $depositId after retries", e)
        }
        
        result.map { Unit }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to verify deposit $depositId", e)
        Result.failure(e)
    }

    override suspend fun rejectDeposit(depositId: String, adminId: String, reason: String): Result<Unit> = try {
        Log.d(TAG, "Rejecting deposit $depositId by admin $adminId, reason: $reason")
        
        val result = RetryUtils.retryWithBackoff(maxAttempts = 3) {
            pendingDepositsCollection.document(depositId)
                .update(
                    mapOf(
                        "status" to DepositStatus.REJECTED,
                        "rejectedBy" to adminId,
                        "rejectedAt" to com.google.firebase.Timestamp.now(),
                        "rejectionReason" to reason
                    )
                ).await()
        }
        
        result.onSuccess {
            Log.d(TAG, "Deposit $depositId rejected successfully")
        }.onFailure { e ->
            Log.e(TAG, "Failed to reject deposit $depositId after retries", e)
        }
        
        result.map { Unit }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to reject deposit $depositId", e)
        Result.failure(e)
    }

    override suspend fun createTransaction(transaction: Transaction): Result<String> = try {
        Log.d(TAG, "Creating transaction for user ${transaction.userId}, amount: ${transaction.amount}")
        
        // Validate transaction data
        if (transaction.amount <= 0) {
            throw IllegalArgumentException("Transaction amount must be greater than 0")
        }
        
        if (transaction.userId.isBlank()) {
            throw IllegalArgumentException("User ID cannot be empty")
        }

        val result = RetryUtils.retryWithBackoff(maxAttempts = 3) {
            val docRef = transactionsCollection.add(transaction).await()
            docRef.id
        }
        
        result.onSuccess { id ->
            Log.d(TAG, "Transaction created successfully with ID: $id")
        }.onFailure { e ->
            Log.e(TAG, "Failed to create transaction after retries", e)
        }
        
        result
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create transaction", e)
        Result.failure(e)
    }

    override suspend fun updateTransactionStatus(transactionId: String, status: String): Result<Unit> = try {
        Log.d(TAG, "Updating transaction $transactionId status to $status")
        
        val result = RetryUtils.retryWithBackoff(maxAttempts = 3) {
            transactionsCollection.document(transactionId)
                .update("status", status)
                .await()
        }
        
        result.onSuccess {
            Log.d(TAG, "Transaction $transactionId status updated successfully")
        }.onFailure { e ->
            Log.e(TAG, "Failed to update transaction $transactionId status after retries", e)
        }
        
        result.map { Unit }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to update transaction $transactionId status", e)
        Result.failure(e)
    }

    override suspend fun checkBalance(userId: String, amount: Double): Boolean = try {
        Log.d(TAG, "Checking balance for user $userId, required amount: $amount")
        
        val result = RetryUtils.retryWithBackoff(maxAttempts = 3) {
            val wallet = walletsCollection.document(userId).get().await().toObject(Wallet::class.java)
            val totalBalance = (wallet?.withdrawableBalance ?: 0.0) + (wallet?.bonusBalance ?: 0.0)
            totalBalance >= amount
        }
        
        result.onSuccess { hasSufficientBalance ->
            Log.d(TAG, "Balance check result for user $userId: $hasSufficientBalance")
        }.onFailure { e ->
            Log.e(TAG, "Failed to check balance for user $userId after retries", e)
        }
        
        result.getOrElse { false }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to check balance for user $userId", e)
        false
    }

    override suspend fun updateBalance(userId: String, amount: Double): Result<Unit> = try {
        Log.d(TAG, "Updating balance for user $userId, amount: $amount")
        
        val result = RetryUtils.retryWithBackoff(maxAttempts = 3) {
            firebaseManager.firestore.runTransaction { transaction ->
                val walletRef = walletsCollection.document(userId)
                val wallet = transaction.get(walletRef).toObject(Wallet::class.java)
                    ?: throw Exception("Wallet not found")

                val newWithdrawableBalance = wallet.withdrawableBalance + amount
                val newTotalBalance = newWithdrawableBalance + wallet.bonusBalance

                transaction.update(walletRef, mapOf(
                    "withdrawableBalance" to newWithdrawableBalance,
                    "balance" to newTotalBalance,
                    "currency" to (wallet.currency ?: "INR") // Ensure currency is set
                ))

                // Update user's walletBalance for display
                val userRef = usersCollection.document(userId)
                transaction.update(userRef, "walletBalance", newTotalBalance)
            }.await()
        }
        
        result.onSuccess {
            Log.d(TAG, "Balance updated successfully for user $userId")
        }.onFailure { e ->
            Log.e(TAG, "Failed to update balance for user $userId after retries", e)
        }
        
        result.map { Unit }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to update balance for user $userId", e)
        Result.failure(e)
    }

    override suspend fun createWithdrawalRequest(request: WithdrawalRequest): Result<String> = try {
        Log.d(TAG, "Creating withdrawal request for user ${request.userId}, amount: ${request.amount}, currency: ${request.currency}")
        
        // Validate withdrawal request
        if (request.amount <= 0) {
            throw IllegalArgumentException("Withdrawal amount must be greater than 0")
        }
        
        if (request.userId.isBlank()) {
            throw IllegalArgumentException("User ID cannot be empty")
        }
        
        // Validate currency
        if (!NGNTransactionUtils.validateCurrency(request.currency)) {
            throw IllegalArgumentException("Unsupported currency: ${request.currency}")
        }
        
        // Validate amount for currency
        if (!NGNTransactionUtils.validateAmountForCurrency(request.amount, request.currency)) {
            val minAmount = NGNTransactionUtils.getMinimumAmountForCurrency(request.currency)
            throw IllegalArgumentException("Amount too low for ${request.currency}. Minimum: ${NGNTransactionUtils.formatAmount(minAmount, request.currency)}")
        }

        // Check KYC status and withdrawal limits
        val user = usersCollection.document(request.userId).get().await().toObject(User::class.java)
        val kycStatusString = user?.kycStatus ?: "not_submitted"
        val kycStatus = when (kycStatusString.uppercase()) {
            "VERIFIED", "APPROVED" -> KycStatus.VERIFIED
            "PENDING" -> KycStatus.PENDING
            "REJECTED" -> KycStatus.REJECTED
            else -> KycStatus.PENDING // Default to PENDING for unknown statuses
        }
        
        if (!kycMonitor.isWithdrawalAllowed(kycStatus, request.amount)) {
            val limitMessage = kycMonitor.getWithdrawalLimitMessage(kycStatus, request.amount)
            throw IllegalArgumentException(limitMessage)
        }

        // Check for duplicate operation using idempotency
        val operationData = mapOf(
            "amount" to request.amount,
            "currency" to request.currency,
            "paymentMethod" to request.paymentMethod.name,
            "bankName" to (request.bankName ?: ""),
            "accountNumber" to (request.accountNumber ?: ""),
            "accountName" to (request.accountName ?: ""),
            "userCountry" to request.userCountry
        )
        
        if (idempotencyManager.isDuplicateOperation("CREATE_WITHDRAWAL", request.userId, operationData)) {
            throw IllegalArgumentException("Duplicate withdrawal request detected. Please wait before trying again.")
        }

        val result = RetryUtils.retryWithBackoff(maxAttempts = 3) {
            firebaseManager.firestore.runTransaction { transaction ->
                // Get current wallet state atomically
                val walletRef = walletsCollection.document(request.userId)
                val wallet = transaction.get(walletRef).toObject(Wallet::class.java)
                    ?: throw Exception("Wallet not found")

                // Check if wallet currency matches request currency
                if ((wallet.currency ?: "INR").uppercase() != request.currency.uppercase()) {
                    throw Exception("Currency mismatch. Wallet currency: ${wallet.currency}, Request currency: ${request.currency}")
                }

                // Check balance atomically
                if (wallet.withdrawableBalance < request.amount) {
                    throw Exception("Insufficient withdrawable balance. Available: ${NGNTransactionUtils.formatAmount(wallet.withdrawableBalance, wallet.currency)}, Requested: ${NGNTransactionUtils.formatAmount(request.amount, request.currency)}")
                }

                // Create withdrawal request
                val withdrawalRef = pendingWithdrawalsCollection.document()
                transaction.set(withdrawalRef, request)

                // Deduct from wallet atomically
                val newWithdrawableBalance = wallet.withdrawableBalance - request.amount
                val newTotalBalance = newWithdrawableBalance + wallet.bonusBalance
                
                transaction.update(walletRef, mapOf(
                    "withdrawableBalance" to newWithdrawableBalance,
                    "balance" to newTotalBalance,
                    "currency" to wallet.currency,
                    "currencySymbol" to wallet.currencySymbol,
                    "lastUpdated" to System.currentTimeMillis()
                ))
                
                // Update user's walletBalance for display
                transaction.update(usersCollection.document(request.userId), "walletBalance", newTotalBalance)

                // Create transaction record
                val transactionRecord = Transaction(
                    userId = request.userId,
                    amount = request.amount,
                    currency = request.currency,
                    type = NGNTransactionUtils.getTransactionTypeForPaymentMethod(request.paymentMethod, false),
                    status = TransactionStatus.PENDING,
                    description = NGNTransactionUtils.getPaymentDescription(request.paymentMethod, request.amount, request.currency),
                    paymentMethod = request.paymentMethod,
                    createdAt = com.google.firebase.Timestamp.now()
                )
                val transactionDocRef = transactionsCollection.document()
                transaction.set(transactionDocRef, transactionRecord)
            }.await()
        }
        
        result.onSuccess {
            // Store idempotency key on success
            idempotencyManager.storeIdempotencyKey("CREATE_WITHDRAWAL", request.userId, operationData)
            Log.d(TAG, "Withdrawal request created successfully for user ${request.userId}")
        }.onFailure { e ->
            Log.e(TAG, "Failed to create withdrawal request for user ${request.userId} after retries", e)
        }
        
        result.map { "Withdrawal request created successfully" }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create withdrawal request for user ${request.userId}", e)
        Result.failure(e)
    }

    override fun getWithdrawalRequests(userId: String): Flow<List<WithdrawalRequest>> = callbackFlow {
        val listener = pendingWithdrawalsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error getting withdrawal requests for user $userId", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(WithdrawalRequest::class.java)?.copy(requestId = doc.id)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to deserialize withdrawal request ${doc.id}: ${e.message}")
                        null
                    }
                } ?: emptyList()
                
                Log.d(TAG, "Withdrawal requests updated for user $userId: ${requests.size} items")
                trySend(requests)
            }
        
        awaitClose { 
            listener.remove()
            Log.d(TAG, "Withdrawal requests listener removed for user $userId")
        }
    }
} 