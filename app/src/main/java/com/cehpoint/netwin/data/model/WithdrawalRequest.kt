package com.cehpoint.netwin.data.model

data class WithdrawalRequest(
    val requestId: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val currency: String = "INR",
    val upiId: String = "",
    val status: String = "PENDING", // or enum
    val rejectionReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val verifiedAt: Long? = null,
    val userDetails: UserDetails = UserDetails(),
    // Nigerian payment specific fields
    val paymentMethod: PaymentMethod = PaymentMethod.UPI,
    val bankName: String? = null,
    val accountNumber: String? = null,
    val accountName: String? = null,
    val userCountry: String = "IN", // IN for India, NG for Nigeria
    val bankCode: String? = null, // For Nigerian banks
    val recipientCode: String? = null // For Paystack transfers
)

data class UserDetails(
    val email: String = "",
    val name: String = "",
    val username: String = "",
    val userId: String = ""
) 