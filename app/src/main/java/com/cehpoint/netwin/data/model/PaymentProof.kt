package com.cehpoint.netwin.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * Enhanced payment proof model for comprehensive verification
 */
data class PaymentProof(
    val id: String = "",
    val userId: String = "",
    val depositId: String = "",
    val currency: String = "INR",
    val amount: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.UPI,
    
    // Common fields
    val screenshotUrl: String? = null,
    val receiptUrl: String? = null,
    val bankStatementUrl: String? = null,
    
    // NGN/Paystack specific fields
    val paystackReference: String? = null,
    val paystackEmail: String? = null,
    val bankTransactionId: String? = null,
    val bankName: String? = null,
    val accountNumber: String? = null,
    val accountName: String? = null,
    
    // INR/UPI specific fields  
    val upiTransactionId: String? = null,
    val upiReferenceNumber: String? = null,
    val senderUpiId: String? = null,
    val receiverUpiId: String? = null,
    val upiAppName: String? = null, // GPay, PhonePe, Paytm, etc.
    
    // Verification fields
    val isVerified: Boolean = false,
    val verificationStatus: PaymentVerificationStatus = PaymentVerificationStatus.PENDING,
    val verifiedBy: String? = null,
    val verifiedAt: Timestamp? = null,
    val rejectionReason: String? = null,
    val adminNotes: String? = null,
    
    // Timestamps
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

enum class PaymentVerificationStatus {
    @PropertyName("pending")
    PENDING,
    
    @PropertyName("under_review")
    UNDER_REVIEW,
    
    @PropertyName("verified")
    VERIFIED,
    
    @PropertyName("rejected")
    REJECTED,
    
    @PropertyName("requires_additional_info")
    REQUIRES_ADDITIONAL_INFO
}

/**
 * Payment proof validation requirements by country/method
 */
data class PaymentProofRequirements(
    val currency: String,
    val paymentMethod: PaymentMethod,
    val requiredFields: List<String>,
    val optionalFields: List<String>,
    val requiredDocuments: List<PaymentDocumentType>
)

enum class PaymentDocumentType {
    SCREENSHOT,
    BANK_RECEIPT,
    BANK_STATEMENT,
    UPI_RECEIPT,
    PAYSTACK_RECEIPT
}
