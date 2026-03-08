package com.cehpoint.netwin.data.model

/**
 * Payment methods supported by the app
 * Matches web app payment method enum
 */
enum class PaymentMethod {
    UPI,
    UPI_DEPOSIT,
    UPI_WITHDRAWAL,
    BANK_TRANSFER,
    BANK_TRANSFER_DEPOSIT,
    BANK_TRANSFER_WITHDRAWAL,
    CARD_PAYMENT,
    CREDIT_CARD,
    DEBIT_CARD,
    MOBILE_MONEY,
    MOBILE_MONEY_NG,
    BANK_ACCOUNT_NG,
    PAYSTACK,
    RAZORPAY,
    FLUTTERWAVE,
    INTERSWITCH,
    GTBANK,
    ZENITH_BANK,
    ACCESS_BANK,
    FIRST_BANK,
    UBA,
    MANUAL_UPI;
    
    companion object {
        fun fromString(value: String?): PaymentMethod {
            return when (value?.uppercase()) {
                "UPI" -> UPI
                "UPI_DEPOSIT" -> UPI_DEPOSIT
                "UPI_WITHDRAWAL" -> UPI_WITHDRAWAL
                "BANK_TRANSFER" -> BANK_TRANSFER
                "BANK_TRANSFER_DEPOSIT" -> BANK_TRANSFER_DEPOSIT
                "BANK_TRANSFER_WITHDRAWAL" -> BANK_TRANSFER_WITHDRAWAL
                "CARD_PAYMENT" -> CARD_PAYMENT
                "MOBILE_MONEY" -> MOBILE_MONEY
                "PAYSTACK" -> PAYSTACK
                "RAZORPAY" -> RAZORPAY
                "MANUAL_UPI" -> MANUAL_UPI
                else -> UPI // Default
            }
        }
    }
}
