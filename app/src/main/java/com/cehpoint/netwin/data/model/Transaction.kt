package com.cehpoint.netwin.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Transaction(
    val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val currency: String = "INR",
    val type: TransactionType = TransactionType.DEPOSIT,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val description: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.UPI,
    val gateway: String? = null, // e.g., RAZORPAY, PAYSTACK
    val gatewayReference: String? = null, // order_id or reference
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
//    DEPOSIT,
//    WITHDRAWAL,
//    TOURNAMENT_ENTRY,
//    TOURNAMENT_WINNING,
//    KILL_REWARD,
//    REFUND,
//    UPI_DEPOSIT,
//    UPI_WITHDRAWAL,
//    BANK_TRANSFER_DEPOSIT,
//    BANK_TRANSFER_WITHDRAWAL,
//    CARD_PAYMENT,
//    MOBILE_MONEY

    @PropertyName("deposit")
    DEPOSIT,

    @PropertyName("withdrawal")
    WITHDRAWAL,

    @PropertyName("tournament_entry")
    TOURNAMENT_ENTRY,

    @PropertyName("entry_fee")
    ENTRY_FEE,

    @PropertyName("tournament_winning")
    TOURNAMENT_WINNING,

    @PropertyName("kill_reward")
    KILL_REWARD,

    @PropertyName("refund")
    REFUND,

    @PropertyName("upi_deposit")
    UPI_DEPOSIT,

    @PropertyName("upi_withdrawal")
    UPI_WITHDRAWAL,

    @PropertyName("bank_transfer_deposit")
    BANK_TRANSFER_DEPOSIT,

    @PropertyName("bank_transfer_withdrawal")
    BANK_TRANSFER_WITHDRAWAL,

    @PropertyName("card_payment")
    CARD_PAYMENT,

    @PropertyName("mobile_money")
    MOBILE_MONEY
}

enum class TransactionStatus {
//    PENDING,
//    COMPLETED,
//    FAILED,
//    REFUNDED,
//    CANCELLED
    @PropertyName("pending")
    PENDING,

    @PropertyName("completed")
    COMPLETED,

    @PropertyName("failed")
    FAILED,

    @PropertyName("refunded")
    REFUNDED,

    @PropertyName("cancelled")
    CANCELLED,

    @PropertyName("verified")
    VERIFIED

}

enum class PaymentMethod {
//    UPI,
//    BANK_TRANSFER,
//    WALLET,
//    CASH,
//    CREDIT_CARD,
//    DEBIT_CARD,
//    // Nigerian payment methods
//    FLUTTERWAVE,
//    PAYSTACK,
//    INTERSWITCH,
//    GTBANK,
//    ZENITH_BANK,
//    ACCESS_BANK,
//    FIRST_BANK,
//    UBA,
//    MOBILE_MONEY_NG,
//    BANK_ACCOUNT_NG

    //NEW
    @PropertyName("upi")
    UPI,

    @PropertyName("bank_transfer")
    BANK_TRANSFER,

    @PropertyName("wallet")
    WALLET,

    @PropertyName("cash")
    CASH,

    @PropertyName("credit_card")
    CREDIT_CARD,

    @PropertyName("debit_card")
    DEBIT_CARD,

// Nigerian payment methods
    @PropertyName("flutterwave")
    FLUTTERWAVE,

    @PropertyName("razorpay")
    RAZORPAY,

    @PropertyName("paystack")
    PAYSTACK,

    @PropertyName("interswitch")
    INTERSWITCH,

    @PropertyName("gtbank")
    GTBANK,

    @PropertyName("zenith_bank")
    ZENITH_BANK,

    @PropertyName("access_bank")
    ACCESS_BANK,

    @PropertyName("first_bank")
    FIRST_BANK,

    @PropertyName("uba")
    UBA,

    @PropertyName("mobile_money_ng")
    MOBILE_MONEY_NG,

    @PropertyName("bank_account_ng")
    BANK_ACCOUNT_NG
} 