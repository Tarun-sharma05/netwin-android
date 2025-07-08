package com.cehpoint.netwin.data.repository

import com.cehpoint.netwin.data.model.DepositStatus
import com.cehpoint.netwin.data.model.PaymentMethod
import com.cehpoint.netwin.data.model.PendingDeposit
import com.cehpoint.netwin.data.model.Transaction
import com.cehpoint.netwin.data.model.TransactionStatus
import com.cehpoint.netwin.data.model.TransactionType
import com.cehpoint.netwin.data.model.WithdrawalRequest
import com.cehpoint.netwin.data.model.UserDetails
import com.cehpoint.netwin.data.remote.FirebaseManager
import com.cehpoint.netwin.domain.repository.WalletRepository
import com.cehpoint.netwin.utils.NGNTransactionUtils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class WalletRepositoryImpl @Inject constructor(
    private val firebaseManager: FirebaseManager
) : WalletRepository {

    private val usersCollection = firebaseManager.firestore.collection("users")
    private val walletsCollection = firebaseManager.firestore.collection("wallets")
    private val transactionsCollection = firebaseManager.firestore.collection("transactions")
    private val pendingDepositsCollection = firebaseManager.firestore.collection("pending_deposits")
    private val pendingWithdrawalsCollection = firebaseManager.firestore.collection("pending_withdrawals")

    override fun getWalletBalance(userId: String): Flow<Double> = callbackFlow {
        val listener = walletsCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val wallet = snapshot?.toObject(com.cehpoint.netwin.data.model.Wallet::class.java)
                val balance = (wallet?.withdrawableBalance ?: 0.0) + (wallet?.bonusBalance ?: 0.0)
                trySend(balance)
            }
        
        awaitClose { listener.remove() }
    }

    override fun getWithdrawableBalance(userId: String): Flow<Double> = callbackFlow {
        val listener = walletsCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val wallet = snapshot?.toObject(com.cehpoint.netwin.data.model.Wallet::class.java)
                trySend(wallet?.withdrawableBalance ?: 0.0)
            }
        
        awaitClose { listener.remove() }
    }

    override fun getBonusBalance(userId: String): Flow<Double> = callbackFlow {
        val listener = walletsCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val wallet = snapshot?.toObject(com.cehpoint.netwin.data.model.Wallet::class.java)
                trySend(wallet?.bonusBalance ?: 0.0)
            }
        
        awaitClose { listener.remove() }
    }

    override fun getTransactions(userId: String): Flow<List<Transaction>> = callbackFlow {
        val listener = transactionsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val transactions = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(transactions)
            }
        
        awaitClose { listener.remove() }
    }

    override fun getPendingDeposits(userId: String): Flow<List<PendingDeposit>> = callbackFlow {
        val listener = pendingDepositsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", DepositStatus.PENDING)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val deposits = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PendingDeposit::class.java)?.copy(requestId = doc.id)
                } ?: emptyList()
                trySend(deposits)
            }
        
        awaitClose { listener.remove() }
    }

    override suspend fun createPendingDeposit(deposit: PendingDeposit): Result<String> = try {
        val docRef = pendingDepositsCollection.add(deposit).await()
        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun verifyDeposit(depositId: String, adminId: String): Result<Unit> = try {
        val depositRef = pendingDepositsCollection.document(depositId)
        val deposit = depositRef.get().await().toObject(PendingDeposit::class.java)
            ?: throw Exception("Deposit not found")

        firebaseManager.firestore.runTransaction { transaction ->
            // Update deposit status
            transaction.update(depositRef, "status", DepositStatus.APPROVED)
            transaction.update(depositRef, "verifiedBy", adminId)
            transaction.update(depositRef, "verifiedAt", com.google.firebase.Timestamp.now())

            // Update wallet balance
            val walletRef = walletsCollection.document(deposit.userId)
            val wallet = transaction.get(walletRef).toObject(com.cehpoint.netwin.data.model.Wallet::class.java)
                ?: throw Exception("Wallet not found")

            val newWithdrawableBalance = wallet.withdrawableBalance + deposit.amount
            transaction.update(walletRef, "withdrawableBalance", newWithdrawableBalance)
            transaction.update(walletRef, "balance", newWithdrawableBalance + wallet.bonusBalance)

            // Update user's walletBalance for display
            val userRef = usersCollection.document(deposit.userId)
            transaction.update(userRef, "walletBalance", newWithdrawableBalance + wallet.bonusBalance)

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

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun rejectDeposit(depositId: String, adminId: String, reason: String): Result<Unit> = try {
        pendingDepositsCollection.document(depositId)
            .update(
                mapOf(
                    "status" to DepositStatus.REJECTED,
                    "rejectedBy" to adminId,
                    "rejectedAt" to com.google.firebase.Timestamp.now(),
                    "rejectionReason" to reason
                )
            ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun createTransaction(transaction: Transaction): Result<String> = try {
        val docRef = transactionsCollection.add(transaction).await()
        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateTransactionStatus(transactionId: String, status: String): Result<Unit> = try {
        transactionsCollection.document(transactionId)
            .update("status", status)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun checkBalance(userId: String, amount: Double): Boolean = try {
        val wallet = walletsCollection.document(userId).get().await().toObject(com.cehpoint.netwin.data.model.Wallet::class.java)
        val totalBalance = (wallet?.withdrawableBalance ?: 0.0) + (wallet?.bonusBalance ?: 0.0)
        totalBalance >= amount
    } catch (e: Exception) {
        false
    }

    override suspend fun updateBalance(userId: String, amount: Double): Result<Unit> = try {
        val walletRef = walletsCollection.document(userId)
        val wallet = walletRef.get().await().toObject(com.cehpoint.netwin.data.model.Wallet::class.java)
            ?: throw Exception("Wallet not found")

        val newWithdrawableBalance = wallet.withdrawableBalance + amount
        val newTotalBalance = newWithdrawableBalance + wallet.bonusBalance

        walletRef.update(
            mapOf(
                "withdrawableBalance" to newWithdrawableBalance,
                "balance" to newTotalBalance
            )
        ).await()

        // Update user's walletBalance for display
        usersCollection.document(userId).update("walletBalance", newTotalBalance).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun createWithdrawalRequest(request: WithdrawalRequest): Result<String> = try {
        val wallet = walletsCollection.document(request.userId).get().await().toObject(com.cehpoint.netwin.data.model.Wallet::class.java)
            ?: throw Exception("Wallet not found")

        if (wallet.withdrawableBalance < request.amount) {
            throw Exception("Insufficient withdrawable balance")
        }

        firebaseManager.firestore.runTransaction { transaction ->
            // Create withdrawal request
            val withdrawalRef = pendingWithdrawalsCollection.document()
            transaction.set(withdrawalRef, request)

            // Deduct from wallet
            val newWithdrawableBalance = wallet.withdrawableBalance - request.amount
            val newTotalBalance = newWithdrawableBalance + wallet.bonusBalance
            
            transaction.update(walletsCollection.document(request.userId), 
                mapOf(
                    "withdrawableBalance" to newWithdrawableBalance,
                    "balance" to newTotalBalance
                )
            )
            
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

        Result.success("Withdrawal request created successfully")
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun getWithdrawalRequests(userId: String): Flow<List<WithdrawalRequest>> = callbackFlow {
        val listener = pendingWithdrawalsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(WithdrawalRequest::class.java)?.copy(requestId = doc.id)
                } ?: emptyList()
                trySend(requests)
            }
        
        awaitClose { listener.remove() }
    }
} 