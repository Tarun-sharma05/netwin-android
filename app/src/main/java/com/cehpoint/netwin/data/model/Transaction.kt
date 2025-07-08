package com.cehpoint.netwin.data.model

import com.google.firebase.Timestamp

data class Transaction(
    val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val currency: String = "INR",
    val type: TransactionType = TransactionType.DEPOSIT,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val description: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.UPI,
    val metadata: Map<String, Any> = emptyMap(),
    val tournamentId: String? = null,
    val tournamentTitle: String? = null,
    val upiRefId: String? = null,
    val userUpiId: String? = null,
    val adminNotes: String? = null,
    val fee: Double? = null,
    val netAmount: Double? = null,
    val rejectionReason: String? = null,
    val depositRequestId: String? = null,
    val verifiedBy: String? = null,
    val verifiedAt: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

enum class TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TOURNAMENT_ENTRY,
    TOURNAMENT_WINNING,
    KILL_REWARD,
    REFUND,
    UPI_DEPOSIT,
    UPI_WITHDRAWAL,
    BANK_TRANSFER_DEPOSIT,
    BANK_TRANSFER_WITHDRAWAL,
    CARD_PAYMENT,
    MOBILE_MONEY
}

enum class TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED,
    CANCELLED
}

enum class PaymentMethod {
    UPI,
    BANK_TRANSFER,
    WALLET,
    CASH,
    CREDIT_CARD,
    DEBIT_CARD,
    // Nigerian payment methods
    FLUTTERWAVE,
    PAYSTACK,
    INTERSWITCH,
    GTBANK,
    ZENITH_BANK,
    ACCESS_BANK,
    FIRST_BANK,
    UBA,
    MOBILE_MONEY_NG,
    BANK_ACCOUNT_NG
} 