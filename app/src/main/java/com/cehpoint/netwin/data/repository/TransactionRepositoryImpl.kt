package com.cehpoint.netwin.data.repository

import com.cehpoint.netwin.data.model.Transaction
import com.cehpoint.netwin.data.model.TransactionStatus
import com.cehpoint.netwin.data.remote.FirebaseManager
import com.cehpoint.netwin.domain.model.User
import com.cehpoint.netwin.domain.repository.TransactionRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val firebaseManager: FirebaseManager
) : TransactionRepository {

    private val transactionsCollection = firebaseManager.firestore.collection("transactions")
    private val usersCollection = firebaseManager.firestore.collection("users")

    override fun getTransactionsByUser(userId: String): Flow<List<Transaction>> = callbackFlow {
        val subscription = transactionsCollection
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
        
        awaitClose { subscription.remove() }
    }

    override fun getTransactionById(id: String): Flow<Transaction?> = callbackFlow {
        val subscription = transactionsCollection.document(id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val transaction = snapshot?.toObject(Transaction::class.java)?.copy(id = snapshot.id)
                trySend(transaction)
            }
        
        awaitClose { subscription.remove() }
    }

    override suspend fun createTransaction(transaction: Transaction): Result<String> = try {
        val docRef = transactionsCollection.add(transaction).await()
        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateTransaction(transaction: Transaction): Result<Unit> = try {
        transactionsCollection.document(transaction.id)
            .set(transaction)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteTransaction(id: String): Result<Unit> = try {
        transactionsCollection.document(id)
            .delete()
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateTransactionStatus(id: String, status: TransactionStatus): Result<Unit> = try {
        transactionsCollection.document(id)
            .update("status", status)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getWalletBalance(userId: String): Flow<Double> = callbackFlow {
        val subscription = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val user = snapshot?.toObject(User::class.java)
                val balance = user?.walletBalance ?: 0.0
                trySend(balance)
            }
        
        awaitClose { subscription.remove() }
    }

    override suspend fun deposit(userId: String, amount: Double, paymentMethod: String): Result<String> = try {
        val transaction = Transaction(
            userId = userId,
            amount = amount,
            type = com.cehpoint.netwin.data.model.TransactionType.DEPOSIT,
            status = TransactionStatus.PENDING,
            description = "Deposit via $paymentMethod",
            paymentMethod = com.cehpoint.netwin.data.model.PaymentMethod.valueOf(paymentMethod)
        )

        val docRef = transactionsCollection.add(transaction).await()
        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun withdraw(userId: String, amount: Double, paymentMethod: String): Result<String> = try {
        // First, check if user has sufficient balance
        val userRef = usersCollection.document(userId)
        val user = userRef.get().await().toObject(User::class.java)
            ?: throw Exception("User not found")

        if (user.walletBalance < amount) {
            throw Exception("Insufficient balance")
        }

        // Create withdrawal transaction
        val withdrawal = Transaction(
            userId = userId,
            amount = amount,
            type = com.cehpoint.netwin.data.model.TransactionType.WITHDRAWAL,
            status = TransactionStatus.PENDING,
            description = "Withdrawal via $paymentMethod",
            paymentMethod = com.cehpoint.netwin.data.model.PaymentMethod.valueOf(paymentMethod)
        )

        // Add transaction to Firestore
        val docRef = transactionsCollection.add(withdrawal).await()

        // Update user's wallet balance
        userRef.update("walletBalance", user.walletBalance - amount).await()

        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }
} 