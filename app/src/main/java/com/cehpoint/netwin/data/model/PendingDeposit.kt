package com.cehpoint.netwin.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class PendingDeposit(
    @DocumentId
    val requestId: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val currency: String = "INR",
    val upiRefId: String = "",
    val userUpiId: String = "",
    val screenshotUrl: String? = null,
    val adminNotes: String? = null,
    val status: DepositStatus = DepositStatus.PENDING,
    val verifiedBy: String? = null,
    val verifiedAt: Timestamp? = null,
    val rejectionReason: String? = null,
    val fee: Double? = null,
    val netAmount: Double? = null,
    // Nigerian payment specific fields
    val paymentMethod: PaymentMethod = PaymentMethod.UPI,
    val bankName: String? = null,
    val accountNumber: String? = null,
    val accountName: String? = null,
    val transactionReference: String? = null,
    val paymentProvider: String? = null, // FLUTTERWAVE, PAYSTACK, etc.
    val userCountry: String = "IN", // IN for India, NG for Nigeria
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

enum class DepositStatus {
    PENDING, APPROVED, REJECTED
} 