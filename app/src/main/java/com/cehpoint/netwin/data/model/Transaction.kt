package com.cehpoint.netwin.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Transaction(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val currency: String = "INR",
    val type: TransactionType = TransactionType.MANUAL_DEPOSIT,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val description: String = "",
    val paymentMethod: String = "UPI",
    val upiTransactionId: String = "", // User-entered 12-digit ID
    val userUpiId: String = "", // User's UPI ID (optional)
    val netwinUpiId: String = "", // NetWin's UPI ID from admin_config (where user sent money)
    val merchantDisplayName: String = "", // NetWin's display name from admin_config
    val adminNotes: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val verifiedAt: Timestamp? = null,
    val verifiedBy: String? = null,
    val rejectionReason: String? = null,
    val tournamentId: String? = null
)

enum class TransactionType {
    MANUAL_DEPOSIT,
    MANUAL_WITHDRAWAL,
    TOURNAMENT_ENTRY,
    TOURNAMENT_WINNING,
    BONUS_CREDIT,
    REFUND,
    DEPOSIT,
    WITHDRAWAL,
    UPI_DEPOSIT,
    UPI_WITHDRAWAL,
    BANK_TRANSFER_DEPOSIT,
    BANK_TRANSFER_WITHDRAWAL,
    CARD_PAYMENT,
    MOBILE_MONEY,
    ENTRY_FEE
}

enum class TransactionStatus {
    PENDING,     // User submitted, awaiting admin verification
    VERIFIED,    // Admin verified the UPI payment
    COMPLETED,   // Amount credited to wallet
    REJECTED,    // Admin rejected (duplicate/invalid transaction)
    FAILED       // Transaction failed
}