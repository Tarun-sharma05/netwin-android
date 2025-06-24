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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class WalletRepositoryImpl @Inject constructor(
    private val firebaseManager: FirebaseManager
) : WalletRepository {

    private val usersCollection = firebaseManager.firestore.collection("users")
    private val transactionsCollection = firebaseManager.firestore.collection("transactions")
    private val pendingDepositsCollection = firebaseManager.firestore.collection("pending_deposits")
    private val pendingWithdrawalsCollection = firebaseManager.firestore.collection("pending_withdrawals")

    override fun getWalletBalance(userId: String): Flow<Double> = callbackFlow {
        val subscription = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val withdrawable = snapshot?.getDouble("withdrawableBalance")
                val legacy = snapshot?.getDouble("walletBalance") ?: 0.0
                val balance = withdrawable ?: legacy
                trySend(balance)
            }
        awaitClose { subscription.remove() }
    }

    fun getBonusBalance(userId: String): Flow<Double> = callbackFlow {
        val subscription = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val bonus = snapshot?.getDouble("bonusBalance") ?: 0.0
                trySend(bonus)
            }
        awaitClose { subscription.remove() }
    }

    override fun getTransactions(userId: String): Flow<List<Transaction>> = callbackFlow {
        val subscription = transactionsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val transactions = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val transaction = Transaction(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            amount = doc.getDouble("amount") ?: 0.0,
                            currency = doc.getString("currency") ?: "INR",
                            type = TransactionType.valueOf(doc.getString("type") ?: "DEPOSIT"),
                            status = TransactionStatus.valueOf(doc.getString("status") ?: "PENDING"),
                            description = doc.getString("description") ?: "",
                            paymentMethod = PaymentMethod.valueOf(doc.getString("paymentMethod") ?: "UPI"),
                            metadata = doc.get("metadata") as? Map<String, Any> ?: emptyMap(),
                            tournamentId = doc.getString("tournamentId"),
                            tournamentTitle = doc.getString("tournamentTitle"),
                            upiRefId = doc.getString("upiRefId"),
                            userUpiId = doc.getString("userUpiId"),
                            adminNotes = doc.getString("adminNotes"),
                            fee = doc.getDouble("fee"),
                            netAmount = doc.getDouble("netAmount"),
                            rejectionReason = doc.getString("rejectionReason"),
                            depositRequestId = doc.getString("depositRequestId"),
                            verifiedBy = doc.getString("verifiedBy"),
                            verifiedAt = doc.getTimestamp("verifiedAt"),
                            createdAt = doc.getTimestamp("createdAt"),
                            updatedAt = doc.getTimestamp("updatedAt")
                        )
                        transaction
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(transactions)
            }
        awaitClose { subscription.remove() }
    }

    override fun getPendingDeposits(userId: String): Flow<List<PendingDeposit>> = callbackFlow {
        android.util.Log.d("WalletRepositoryImpl", "Querying pending deposits for user: $userId")
        val subscription = pendingDepositsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", DepositStatus.PENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("WalletRepositoryImpl", "Error fetching pending deposits: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                val deposits = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        PendingDeposit(
                            requestId = doc.id,
                            userId = doc.getString("userId") ?: "",
                            amount = doc.getDouble("amount") ?: 0.0,
                            currency = doc.getString("currency") ?: "INR",
                            upiRefId = doc.getString("upiRefId") ?: "",
                            userUpiId = doc.getString("userUpiId") ?: "",
                            screenshotUrl = doc.getString("screenshotUrl"),
                            adminNotes = doc.getString("adminNotes"),
                            status = DepositStatus.valueOf(doc.getString("status") ?: "PENDING"),
                            verifiedBy = doc.getString("verifiedBy"),
                            verifiedAt = doc.getTimestamp("verifiedAt"),
                            rejectionReason = doc.getString("rejectionReason"),
                            createdAt = doc.getTimestamp("createdAt"),
                            updatedAt = doc.getTimestamp("updatedAt")
                        )
                    } catch (e: Exception) {
                        null
                    }
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                android.util.Log.d("WalletRepositoryImpl", "Fetched pending deposits: ${deposits.size}")
                trySend(deposits)
            }
        awaitClose { 
            android.util.Log.d("WalletRepositoryImpl", "Pending deposits listener closed for user: $userId")
            subscription.remove() 
        }
    }

    override suspend fun createPendingDeposit(deposit: PendingDeposit): Result<String> = try {
        val depositData = mapOf(
            "userId" to deposit.userId,
            "amount" to deposit.amount,
            "currency" to deposit.currency,
            "upiRefId" to deposit.upiRefId,
            "userUpiId" to deposit.userUpiId,
            "screenshotUrl" to deposit.screenshotUrl,
            "adminNotes" to deposit.adminNotes,
            "status" to deposit.status.name,
            "verifiedBy" to deposit.verifiedBy,
            "verifiedAt" to deposit.verifiedAt,
            "rejectionReason" to deposit.rejectionReason,
            "createdAt" to deposit.createdAt,
            "updatedAt" to deposit.updatedAt
        )
        val docRef = pendingDepositsCollection.add(depositData).await()
        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun verifyDeposit(depositId: String, adminId: String): Result<Unit> = try {
        val deposit = pendingDepositsCollection.document(depositId).get().await()
            .toObject(PendingDeposit::class.java) ?: throw Exception("Deposit not found")

        if (deposit.status != DepositStatus.PENDING) {
            throw Exception("Deposit is not in pending state")
        }

        // Update deposit status
        pendingDepositsCollection.document(depositId)
            .update(
                mapOf(
                    "status" to DepositStatus.APPROVED,
                    "verifiedBy" to adminId,
                    "verifiedAt" to com.google.firebase.Timestamp.now()
                )
            ).await()

        // Create transaction
        val transaction = Transaction(
            userId = deposit.userId,
            amount = deposit.amount,
            type = TransactionType.UPI_DEPOSIT,
            status = TransactionStatus.COMPLETED,
            description = "UPI Deposit",
            upiRefId = deposit.upiRefId,
            userUpiId = deposit.userUpiId,
            adminNotes = deposit.adminNotes,
            fee = deposit.fee,
            netAmount = deposit.netAmount,
            rejectionReason = deposit.rejectionReason,
            depositRequestId = depositId,
            verifiedBy = adminId,
            verifiedAt = com.google.firebase.Timestamp.now()
        )
        val transactionResult = createTransaction(transaction)
        if (transactionResult.isFailure) {
            android.util.Log.e("WalletRepositoryImpl", "Failed to create transaction: ${transactionResult.exceptionOrNull()?.message}")
            throw transactionResult.exceptionOrNull() ?: Exception("Unknown error creating transaction")
        }

        // Update wallet balance
        val balanceResult = updateBalance(deposit.userId, deposit.amount)
        if (balanceResult.isFailure) {
            android.util.Log.e("WalletRepositoryImpl", "Failed to update balance: ${balanceResult.exceptionOrNull()?.message}")
            throw balanceResult.exceptionOrNull() ?: Exception("Unknown error updating balance")
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun rejectDeposit(depositId: String, adminId: String, reason: String): Result<Unit> = try {
        pendingDepositsCollection.document(depositId)
            .update(
                mapOf(
                    "status" to DepositStatus.REJECTED,
                    "verifiedBy" to adminId,
                    "verifiedAt" to com.google.firebase.Timestamp.now(),
                    "rejectionReason" to reason
                )
            ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun createTransaction(transaction: Transaction): Result<String> = try {
        val transactionData = mapOf(
            "userId" to transaction.userId,
            "amount" to transaction.amount,
            "currency" to transaction.currency,
            "type" to transaction.type.name,
            "status" to transaction.status.name,
            "description" to transaction.description,
            "paymentMethod" to transaction.paymentMethod.name,
            "metadata" to transaction.metadata,
            "tournamentId" to transaction.tournamentId,
            "tournamentTitle" to transaction.tournamentTitle,
            "upiRefId" to transaction.upiRefId,
            "userUpiId" to transaction.userUpiId,
            "adminNotes" to transaction.adminNotes,
            "fee" to transaction.fee,
            "netAmount" to transaction.netAmount,
            "rejectionReason" to transaction.rejectionReason,
            "depositRequestId" to transaction.depositRequestId,
            "verifiedBy" to transaction.verifiedBy,
            "verifiedAt" to transaction.verifiedAt,
            "createdAt" to com.google.firebase.Timestamp.now(),
            "updatedAt" to com.google.firebase.Timestamp.now()
        )
        
        val docRef = transactionsCollection.add(transactionData).await()
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
        val user = usersCollection.document(userId).get().await()
        val balance = user.getDouble("walletBalance") ?: 0.0
        balance >= amount
    } catch (e: Exception) {
        false
    }

    override suspend fun updateBalance(userId: String, amount: Double): Result<Unit> = try {
        val userRef = usersCollection.document(userId)
        val user = userRef.get().await()
        val currentBalance = user.getDouble("walletBalance") ?: 0.0
        userRef.update("walletBalance", currentBalance + amount).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun createWithdrawalRequest(request: WithdrawalRequest): Result<String> = try {
        val data = hashMapOf(
            "userId" to request.userId,
            "amount" to request.amount,
            "currency" to request.currency,
            "upiId" to request.upiId,
            "status" to request.status,
            "rejectionReason" to request.rejectionReason,
            "createdAt" to request.createdAt,
            "updatedAt" to request.updatedAt,
            "verifiedAt" to request.verifiedAt,
            "userDetails" to hashMapOf(
                "email" to request.userDetails.email,
                "name" to request.userDetails.name,
                "username" to request.userDetails.username,
                "userId" to request.userDetails.userId
            )
        )
        val docRef = pendingWithdrawalsCollection.add(data).await()
        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun getWithdrawalRequests(userId: String) = callbackFlow {
        val subscription = pendingWithdrawalsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val userDetailsMap = doc.get("userDetails") as? Map<*, *>
                        WithdrawalRequest(
                            requestId = doc.id,
                            userId = doc.getString("userId") ?: "",
                            amount = doc.getDouble("amount") ?: 0.0,
                            currency = doc.getString("currency") ?: "INR",
                            upiId = doc.getString("upiId") ?: "",
                            status = doc.getString("status") ?: "PENDING",
                            rejectionReason = doc.getString("rejectionReason"),
                            createdAt = doc.getLong("createdAt") ?: 0L,
                            updatedAt = doc.getLong("updatedAt"),
                            verifiedAt = doc.getLong("verifiedAt"),
                            userDetails = UserDetails(
                                email = userDetailsMap?.get("email") as? String ?: "",
                                name = userDetailsMap?.get("name") as? String ?: "",
                                username = userDetailsMap?.get("username") as? String ?: "",
                                userId = userDetailsMap?.get("userId") as? String ?: ""
                            )
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(requests)
            }
        awaitClose { subscription.remove() }
    }
} 