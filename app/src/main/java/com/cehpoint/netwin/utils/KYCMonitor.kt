package com.cehpoint.netwin.utils

import android.util.Log
import com.cehpoint.netwin.data.model.KycStatus
import com.cehpoint.netwin.data.model.User
import com.cehpoint.netwin.data.remote.FirebaseManager
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KYCMonitor @Inject constructor(
    private val firebaseManager: FirebaseManager
) {
    
    companion object {
        private const val TAG = "KYCMonitor"
        
        // Withdrawal limits based on KYC status
        private const val UNVERIFIED_DAILY_LIMIT = 1000.0
        private const val PENDING_DAILY_LIMIT = 5000.0
        private const val VERIFIED_DAILY_LIMIT = 50000.0
        private const val REJECTED_DAILY_LIMIT = 0.0
    }
    
    private val usersCollection = firebaseManager.firestore.collection("users")
    private var kycListener: ListenerRegistration? = null
    
    // Helper function to convert string KYC status to enum
    private fun stringToKycStatus(status: String): KycStatus {
        return when (status.uppercase()) {
            "VERIFIED", "APPROVED" -> KycStatus.VERIFIED
            "PENDING" -> KycStatus.PENDING
            "REJECTED" -> KycStatus.REJECTED
            else -> KycStatus.PENDING // Default to PENDING for unknown statuses
        }
    }
    
    // Monitor KYC status changes for a user
    fun monitorKYCStatus(userId: String): Flow<KycStatus> = callbackFlow {
        Log.d(TAG, "Starting KYC status monitoring for user: $userId")
        
        kycListener = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error monitoring KYC status for user $userId", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                val user = snapshot?.toObject(User::class.java)
                val kycStatusString = user?.kycStatus ?: "not_submitted"
                val kycStatus = stringToKycStatus(kycStatusString)
                
                Log.d(TAG, "KYC status updated for user $userId: $kycStatusString -> $kycStatus")
                trySend(kycStatus)
            }
        
        awaitClose { 
            kycListener?.remove()
            kycListener = null
            Log.d(TAG, "KYC status monitoring stopped for user: $userId")
        }
    }
    
    // Get withdrawal limit based on KYC status
    fun getWithdrawalLimit(kycStatus: KycStatus): Double {
        return when (kycStatus) {
            KycStatus.PENDING -> PENDING_DAILY_LIMIT
            KycStatus.VERIFIED -> VERIFIED_DAILY_LIMIT
            KycStatus.REJECTED -> REJECTED_DAILY_LIMIT
        }
    }
    
    // Check if withdrawal amount is within limit
    fun isWithdrawalAllowed(kycStatus: KycStatus, amount: Double): Boolean {
        val limit = getWithdrawalLimit(kycStatus)
        val isAllowed = amount <= limit
        
        Log.d(TAG, "Withdrawal check - KYC: $kycStatus, Amount: $amount, Limit: $limit, Allowed: $isAllowed")
        
        return isAllowed
    }
    
    // Get KYC status description
    fun getKYCStatusDescription(kycStatus: KycStatus): String {
        return when (kycStatus) {
            KycStatus.PENDING -> "KYC verification is pending. You can withdraw up to ₹5,000 daily"
            KycStatus.VERIFIED -> "KYC verified. You can withdraw up to ₹50,000 daily"
            KycStatus.REJECTED -> "KYC verification was rejected. Please contact support"
        }
    }
    
    // Get withdrawal limit message
    fun getWithdrawalLimitMessage(kycStatus: KycStatus, amount: Double): String {
        val limit = getWithdrawalLimit(kycStatus)
        
        return when {
            kycStatus == KycStatus.REJECTED -> "Withdrawals are blocked due to rejected KYC"
            amount > limit -> "Amount exceeds daily limit of ₹${limit.toInt()}. Please reduce the amount"
            else -> "Withdrawal limit: ₹${limit.toInt()} daily"
        }
    }
    
    // Stop monitoring
    fun stopMonitoring() {
        kycListener?.remove()
        kycListener = null
        Log.d(TAG, "KYC monitoring stopped")
    }
} 