package com.cehpoint.netwin.data.repository

import android.util.Log
import com.cehpoint.netwin.data.model.*
import com.cehpoint.netwin.data.remote.FirebaseManager
import com.cehpoint.netwin.domain.repository.WalletRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


class WalletRepositoryImpl @Inject constructor(
    private val firebaseManager: FirebaseManager
) : WalletRepository {
    
    companion object {
        private const val TAG = "WalletRepositoryImpl"
    }

    private val walletsCollection = firebaseManager.firestore.collection("wallets")
    private val transactionsCollection = firebaseManager.firestore.collection("transactions")
    private val pendingDepositsCollection = firebaseManager.firestore.collection("pending_deposits")

    override suspend fun submitManualDeposit(deposit: ManualUpiDeposit): Result<String> {
        return try {
            // Validate 12-digit UPI transaction ID - same as web app
            if (!isValidUpiTransactionId(deposit.upiTransactionId)) {
                throw IllegalArgumentException("UPI Transaction ID must be exactly 12 digits")
            }

            // Check for duplicate transaction ID - same logic as web app
            val existing = pendingDepositsCollection
                .whereEqualTo("upiTransactionId", deposit.upiTransactionId)
                .get()
                .await()

            if (!existing.isEmpty) {
                throw IllegalArgumentException("This UPI Transaction ID has already been submitted")
            }

            // Create pending deposit - same collection as web app
            val docRef = pendingDepositsCollection.document()
            val depositWithId = deposit.copy(id = docRef.id)
            
            // DEBUG: Log the data being written
            Log.d(TAG, "📝 Attempting to write deposit:")
            Log.d(TAG, "  Document ID: ${docRef.id}")
            Log.d(TAG, "  userId: ${depositWithId.userId}")
            Log.d(TAG, "  amount: ${depositWithId.amount}")
            Log.d(TAG, "  status: ${depositWithId.status}")
            Log.d(TAG, "  upiTransactionId: ${depositWithId.upiTransactionId}")
            
            docRef.set(depositWithId).await()

            Log.d(TAG, "Manual UPI deposit submitted successfully: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to submit manual UPI deposit", e)
            Result.failure(e)
        }
    }

    override suspend fun getWalletBalance(userId: String): Flow<Double> {
        return callbackFlow {
            val listener = walletsCollection.document(userId)
                .addSnapshotListener { document, error ->
                    if (error != null) {
                        Log.e(TAG, "Error getting wallet balance", error)
                        close(error)
                        return@addSnapshotListener
                    }

                    val balance = document?.toObject(Wallet::class.java)?.balance ?: 0.0
                    trySend(balance)
                }

            awaitClose { listener.remove() }
        }
    }

    override suspend fun getPendingDeposits(userId: String): Flow<List<ManualUpiDeposit>> {
        return callbackFlow {
            val listener = pendingDepositsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", TransactionStatus.PENDING.name)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error getting pending deposits", error)
                        close(error)
                        return@addSnapshotListener
                    }

                    val deposits = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(ManualUpiDeposit::class.java)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse pending deposit", e)
                            null
                        }
                    } ?: emptyList()

                    trySend(deposits)
                }

            awaitClose { listener.remove() }
        }
    }

    override suspend fun getTransactionHistory(userId: String): Flow<List<Transaction>> {
        return callbackFlow {
            val listener = transactionsCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error getting transaction history", error)
                        close(error)
                        return@addSnapshotListener
                    }

                    val transactions = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(Transaction::class.java)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse transaction", e)
                            null
                        }
                    } ?: emptyList()

                    trySend(transactions)
                }

            awaitClose { listener.remove() }
        }
    }

    override suspend fun createPendingDeposit(deposit: PendingDeposit): Result<String> {
        return try {
            val docRef = pendingDepositsCollection.document()
            val depositWithId = deposit.copy(requestId = docRef.id)
            docRef.set(depositWithId).await()
            Log.d(TAG, "Pending deposit created successfully: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create pending deposit", e)
            Result.failure(e)
        }
    }

    override suspend fun createWithdrawalRequest(request: WithdrawalRequest): Result<String> {
        return try {
            val docRef = transactionsCollection.document()
            val requestWithId = request.copy(requestId = docRef.id)
            docRef.set(requestWithId).await()
            Log.d(TAG, "Withdrawal request created successfully: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create withdrawal request", e)
            Result.failure(e)
        }
    }

    override suspend fun getWithdrawalRequests(userId: String): Flow<List<WithdrawalRequest>> {
        return callbackFlow {
            val listener = transactionsCollection
                .whereEqualTo("userId", userId)
                .whereIn("type", listOf("WITHDRAWAL", "MANUAL_WITHDRAWAL", "UPI_WITHDRAWAL", "BANK_TRANSFER_WITHDRAWAL"))
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error getting withdrawal requests", error)
                        close(error)
                        return@addSnapshotListener
                    }

                    val requests = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(WithdrawalRequest::class.java)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse withdrawal request", e)
                            null
                        }
                    } ?: emptyList()

                    trySend(requests)
                }

            awaitClose { listener.remove() }
        }
    }

    override suspend fun getWithdrawableBalance(userId: String): Flow<Double> {
        return callbackFlow {
            val listener = walletsCollection.document(userId)
                .addSnapshotListener { document, error ->
                    if (error != null) {
                        Log.e(TAG, "Error getting withdrawable balance", error)
                        close(error)
                        return@addSnapshotListener
                    }

                    val withdrawableBalance = document?.toObject(Wallet::class.java)?.withdrawableBalance ?: 0.0
                    Log.d(TAG, "Withdrawable balance from wallet document: $withdrawableBalance")
                    trySend(withdrawableBalance)
                }

            awaitClose { listener.remove() }
        }
    }

    override suspend fun getBonusBalance(userId: String): Flow<Double> {
        return callbackFlow {
            val listener = walletsCollection.document(userId)
                .addSnapshotListener { document, error ->
                    if (error != null) {
                        Log.e(TAG, "Error getting bonus balance", error)
                        close(error)
                        return@addSnapshotListener
                    }

                    val bonusBalance = document?.toObject(Wallet::class.java)?.bonusBalance ?: 0.0
                    Log.d(TAG, "Bonus balance from wallet document: $bonusBalance")
                    trySend(bonusBalance)
                }

            awaitClose { listener.remove() }
        }
    }

    override suspend fun getTransactionsPaginated(
        userId: String,
        limit: PaginationParams,
        lastDocument: String?
    ): Flow<PaginatedResult<Transaction>> {
        return callbackFlow {
            var query = transactionsCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.pageSize.toLong())

            if (lastDocument != null) {
                val lastDoc = transactionsCollection.document(lastDocument).get().await()
                query = query.startAfter(lastDoc)
            }

            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error getting paginated transactions", error)
                    close(error)
                    return@addSnapshotListener
                }

                val transactions = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Transaction::class.java)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse transaction", e)
                        null
                    }
                } ?: emptyList()

                val hasNext = snapshot?.documents?.size == limit.pageSize
                val lastDocId = snapshot?.documents?.lastOrNull()?.id

                trySend(PaginatedResult(transactions, hasNext, lastDocId))
            }

            awaitClose { listener.remove() }
        }
    }

    override suspend fun getTransactions(userId: String): Flow<List<Transaction>> {
        return getTransactionHistory(userId)
    }

    private fun isValidUpiTransactionId(transactionId: String): Boolean {
        return transactionId.matches(Regex("^[0-9]{12}$"))
    }

    override suspend fun uploadDepositScreenshot(uri: android.net.Uri, userId: String): Result<String> {
        return try {
            Log.d(TAG, "Uploading deposit screenshot for user: $userId")
            
            val timestamp = System.currentTimeMillis()
            val fileName = "deposit_${userId}_${timestamp}.jpg"
            val storageRef = firebaseManager.storage.reference
                .child("deposit_screenshots")
                .child(fileName)
            
            // Upload file to Firebase Storage
            val uploadTask = storageRef.putFile(uri).await()
            
            // Get download URL
            val downloadUrl = storageRef.downloadUrl.await().toString()
            
            Log.d(TAG, "Screenshot uploaded successfully: $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload screenshot", e)
            Result.failure(e)
        }
    }

    override suspend fun createNgnDepositRequest(
        userId: String,
        amount: Double,
        transactionReference: String,
        screenshotUrl: String,
        currency: String
    ): Result<String> {
        return try {
            Log.d(TAG, "Creating NGN deposit request for user: $userId, amount: $amount")
            
            // Check for duplicate transaction reference
            val existing = pendingDepositsCollection
                .whereEqualTo("transactionReference", transactionReference)
                .whereEqualTo("currency", currency)
                .get()
                .await()

            if (!existing.isEmpty) {
                throw IllegalArgumentException("This transaction reference has already been submitted")
            }

            // Create pending deposit document
            val docRef = pendingDepositsCollection.document()
            val deposit = PendingDeposit(
                requestId = docRef.id,
                userId = userId,
                amount = amount,
                currency = currency,
                upiRefId = transactionReference, // Store transaction reference here
                userUpiId = "PAYSTACK", // Payment method identifier
                screenshotUrl = screenshotUrl,
                status = DepositStatus.PENDING,
                paymentMethod = PaymentMethod.BANK_TRANSFER, // For NGN Paystack
                userCountry = "NG",
                transactionReference = transactionReference,
                paymentProvider = "PAYSTACK"
            )
            
            docRef.set(deposit).await()
            
            Log.d(TAG, "NGN deposit request created successfully: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create NGN deposit request", e)
            Result.failure(e)
        }
    }
}