package com.cehpoint.netwin.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class ManualUpiDeposit(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val currency: String = "INR",
    val upiTransactionId: String = "", // 12-digit user-entered transaction ID
    val userUpiId: String = "", // User's UPI ID (optional)
    val netwinUpiId: String = "", // NetWin's UPI ID from admin_config (where user sent money)
    val merchantDisplayName: String = "", // NetWin's display name from admin_config
    val paymentScreenshot: String? = null, // Optional payment proof
    val status: TransactionStatus = TransactionStatus.PENDING,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val verifiedAt: Timestamp? = null,
    val verifiedBy: String? = null,
    val adminNotes: String = "",
    val rejectionReason: String? = null,

    // User details for admin reference
    val userDetails: UserDetails? = null
)