package com.cehpoint.netwin.domain.repository

import com.cehpoint.netwin.data.model.Transaction
import com.cehpoint.netwin.data.model.TransactionStatus
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getTransactionsByUser(userId: String): Flow<List<Transaction>>
    fun getTransactionById(id: String): Flow<Transaction?>
    suspend fun createTransaction(transaction: Transaction): Result<String>
    suspend fun updateTransaction(transaction: Transaction): Result<Unit>
    suspend fun deleteTransaction(id: String): Result<Unit>
    suspend fun updateTransactionStatus(id: String, status: TransactionStatus): Result<Unit>
    suspend fun getWalletBalance(userId: String): Flow<Double>
    suspend fun deposit(userId: String, amount: Double, paymentMethod: String): Result<String>
    suspend fun withdraw(userId: String, amount: Double, paymentMethod: String): Result<String>
} 