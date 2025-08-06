package com.cehpoint.netwin.utils

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cehpoint.netwin.data.model.OfflineOperation
import com.cehpoint.netwin.data.model.OfflineOperationStatus
import com.cehpoint.netwin.data.model.OfflineOperationType
import com.cehpoint.netwin.data.model.PaymentMethod
import com.cehpoint.netwin.data.model.PendingDeposit
import com.cehpoint.netwin.data.model.WithdrawalRequest
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "offline_queue")

@Singleton
class OfflineQueueManager @Inject constructor(
    private val context: Context,
    private val networkStateMonitor: NetworkStateMonitor
) {
    
    companion object {
        private const val TAG = "OfflineQueueManager"
        private const val OFFLINE_OPERATIONS_KEY = "offline_operations"
    }
    
    private val offlineOperationsKey = stringPreferencesKey(OFFLINE_OPERATIONS_KEY)
    
    // Get all pending offline operations
    fun getPendingOperations(): Flow<List<OfflineOperation>> {
        return context.dataStore.data.map { preferences ->
            val operationsJson = preferences[offlineOperationsKey] ?: "[]"
            try {
                parseOfflineOperationsFromString(operationsJson)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse offline operations", e)
                emptyList()
            }
        }
    }
    
    // Add operation to offline queue
    suspend fun addOperation(operation: OfflineOperation) {
        try {
            val currentOperations = getCurrentOperations().toMutableList()
            currentOperations.add(operation)
            saveOperations(currentOperations)
            
            Log.d(TAG, "Added offline operation: ${operation.operationType} for user ${operation.userId}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add offline operation", e)
        }
    }
    
    // Remove completed operation from queue
    suspend fun removeOperation(operationId: String) {
        try {
            val currentOperations = getCurrentOperations().toMutableList()
            currentOperations.removeAll { it.id == operationId }
            saveOperations(currentOperations)
            
            Log.d(TAG, "Removed offline operation: $operationId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove offline operation", e)
        }
    }
    
    // Update operation status
    suspend fun updateOperationStatus(operationId: String, status: OfflineOperationStatus) {
        try {
            val currentOperations = getCurrentOperations().toMutableList()
            val operationIndex = currentOperations.indexOfFirst { it.id == operationId }
            if (operationIndex != -1) {
                currentOperations[operationIndex] = currentOperations[operationIndex].copy(
                    status = status,
                    retryCount = if (status == OfflineOperationStatus.RETRY) {
                        currentOperations[operationIndex].retryCount + 1
                    } else {
                        currentOperations[operationIndex].retryCount
                    }
                )
                saveOperations(currentOperations)
                
                Log.d(TAG, "Updated operation $operationId status to $status")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update operation status", e)
        }
    }
    
    // Create offline deposit operation
    suspend fun queueDepositOperation(deposit: PendingDeposit) {
        val operation = OfflineOperation(
            userId = deposit.userId,
            operationType = OfflineOperationType.CREATE_DEPOSIT,
            data = mapOf(
                "amount" to deposit.amount,
                "currency" to deposit.currency,
                "upiRefId" to deposit.upiRefId,
                "userUpiId" to deposit.userUpiId,
                "screenshotUrl" to (deposit.screenshotUrl ?: ""),
                "paymentMethod" to deposit.paymentMethod.name,
                "userCountry" to deposit.userCountry
            )
        )
        addOperation(operation)
    }
    
    // Create offline withdrawal operation
    suspend fun queueWithdrawalOperation(withdrawal: WithdrawalRequest) {
        val operation = OfflineOperation(
            userId = withdrawal.userId,
            operationType = OfflineOperationType.CREATE_WITHDRAWAL,
            data = mapOf(
                "amount" to withdrawal.amount,
                "currency" to withdrawal.currency,
                "paymentMethod" to withdrawal.paymentMethod.name,
                "bankName" to (withdrawal.bankName ?: ""),
                "accountNumber" to (withdrawal.accountNumber ?: ""),
                "accountName" to (withdrawal.accountName ?: ""),
                "userCountry" to withdrawal.userCountry
            )
        )
        addOperation(operation)
    }
    
    // Process offline operations when network is available
    suspend fun processOfflineOperations(
        onDepositCreated: suspend (PendingDeposit) -> Result<String>,
        onWithdrawalCreated: suspend (WithdrawalRequest) -> Result<String>
    ) {
        val pendingOperations = getCurrentOperations().filter { 
            it.status == OfflineOperationStatus.PENDING || 
            (it.status == OfflineOperationStatus.RETRY && it.retryCount < it.maxRetries)
        }
        
        if (pendingOperations.isEmpty()) {
            Log.d(TAG, "No pending offline operations to process")
            return
        }
        
        Log.d(TAG, "Processing ${pendingOperations.size} offline operations")
        
        for (operation in pendingOperations) {
            try {
                updateOperationStatus(operation.id, OfflineOperationStatus.PROCESSING)
                
                when (operation.operationType) {
                    OfflineOperationType.CREATE_DEPOSIT -> {
                        val deposit = PendingDeposit(
                            userId = operation.userId,
                            amount = (operation.data["amount"] as? Number)?.toDouble() ?: 0.0,
                            currency = operation.data["currency"] as? String ?: "INR",
                            upiRefId = operation.data["upiRefId"] as? String ?: "",
                            userUpiId = operation.data["userUpiId"] as? String ?: "",
                            screenshotUrl = operation.data["screenshotUrl"] as? String,
                            paymentMethod = try {
                                PaymentMethod.valueOf(operation.data["paymentMethod"] as? String ?: "UPI")
                            } catch (e: Exception) {
                                PaymentMethod.UPI
                            },
                            userCountry = operation.data["userCountry"] as? String ?: "IN"
                        )
                        
                        val result = onDepositCreated(deposit)
                        if (result.isSuccess) {
                            updateOperationStatus(operation.id, OfflineOperationStatus.COMPLETED)
                            removeOperation(operation.id)
                            Log.d(TAG, "Successfully processed offline deposit operation")
                        } else {
                            updateOperationStatus(operation.id, OfflineOperationStatus.RETRY)
                            Log.e(TAG, "Failed to process offline deposit operation: ${result.exceptionOrNull()?.message}")
                        }
                    }
                    
                    OfflineOperationType.CREATE_WITHDRAWAL -> {
                        val withdrawal = WithdrawalRequest(
                            userId = operation.userId,
                            amount = (operation.data["amount"] as? Number)?.toDouble() ?: 0.0,
                            currency = operation.data["currency"] as? String ?: "INR",
                            paymentMethod = try {
                                PaymentMethod.valueOf(operation.data["paymentMethod"] as? String ?: "UPI")
                            } catch (e: Exception) {
                                PaymentMethod.UPI
                            },
                            bankName = operation.data["bankName"] as? String,
                            accountNumber = operation.data["accountNumber"] as? String,
                            accountName = operation.data["accountName"] as? String,
                            userCountry = operation.data["userCountry"] as? String ?: "IN"
                        )
                        
                        val result = onWithdrawalCreated(withdrawal)
                        if (result.isSuccess) {
                            updateOperationStatus(operation.id, OfflineOperationStatus.COMPLETED)
                            removeOperation(operation.id)
                            Log.d(TAG, "Successfully processed offline withdrawal operation")
                        } else {
                            updateOperationStatus(operation.id, OfflineOperationStatus.RETRY)
                            Log.e(TAG, "Failed to process offline withdrawal operation: ${result.exceptionOrNull()?.message}")
                        }
                    }
                    
                    else -> {
                        Log.w(TAG, "Unsupported offline operation type: ${operation.operationType}")
                        updateOperationStatus(operation.id, OfflineOperationStatus.FAILED)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing offline operation ${operation.id}", e)
                updateOperationStatus(operation.id, OfflineOperationStatus.RETRY)
            }
        }
    }
    
    private suspend fun getCurrentOperations(): List<OfflineOperation> {
        return try {
            val operationsJson = context.dataStore.data.map { it[offlineOperationsKey] ?: "[]" }.first()
            parseOfflineOperationsFromString(operationsJson)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current operations", e)
            emptyList()
        }
    }
    
    private suspend fun saveOperations(operations: List<OfflineOperation>) {
        try {
            context.dataStore.edit { preferences ->
                preferences[offlineOperationsKey] = operations.joinToString("|") { operation ->
                    "${operation.id}:${operation.userId}:${operation.operationType.name}:${operation.status.name}:${operation.retryCount}:${operation.maxRetries}:${operation.createdAt.seconds}:${operation.data.entries.joinToString(",") { "${it.key}=${it.value}" }}"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save operations", e)
        }
    }
    
    private fun parseOfflineOperationsFromString(operationsString: String): List<OfflineOperation> {
        return if (operationsString == "[]" || operationsString.isEmpty()) {
            emptyList()
        } else {
            try {
                operationsString.split("|").mapNotNull { operationString ->
                    val parts = operationString.split(":")
                    if (parts.size >= 8) {
                        val dataString = parts[7]
                        val data = dataString.split(",").associate { pair ->
                            val keyValue = pair.split("=")
                            if (keyValue.size == 2) keyValue[0] to keyValue[1] else pair to ""
                        }
                        
                        OfflineOperation(
                            id = parts[0],
                            userId = parts[1],
                            operationType = OfflineOperationType.valueOf(parts[2]),
                            data = data,
                            createdAt = Timestamp(parts[6].toLong(), 0),
                            retryCount = parts[4].toInt(),
                            maxRetries = parts[5].toInt(),
                            status = OfflineOperationStatus.valueOf(parts[3])
                        )
                    } else null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse offline operations string", e)
                emptyList()
            }
        }
    }
} 