package com.cehpoint.netwin.data.model

import com.google.firebase.Timestamp
import java.util.UUID

data class OfflineOperation(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val operationType: OfflineOperationType,
    val data: Map<String, Any>,
    val createdAt: Timestamp = Timestamp.now(),
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val status: OfflineOperationStatus = OfflineOperationStatus.PENDING
)

enum class OfflineOperationType {
    CREATE_DEPOSIT,
    CREATE_WITHDRAWAL,
    UPDATE_TRANSACTION_STATUS,
    SYNC_USER_DATA
}

enum class OfflineOperationStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    RETRY
} 