package com.cehpoint.netwin.utils

import com.cehpoint.netwin.data.model.PaymentMethod
import com.cehpoint.netwin.data.model.TransactionType
import com.cehpoint.netwin.data.model.Wallet

object NGNTransactionUtils {
    
    // Nigerian Banks with their codes
    val NIGERIAN_BANKS = mapOf(
        "GTB" to "GTBANK",
        "ZENITH" to "ZENITH_BANK", 
        "ACCESS" to "ACCESS_BANK",
        "FIRST" to "FIRST_BANK",
        "UBA" to "UBA",
        "FIDELITY" to "FIDELITY_BANK",
        "UNION" to "UNION_BANK",
        "WEMA" to "WEMA_BANK",
        "POLARIS" to "POLARIS_BANK",
        "STANBIC" to "STANBIC_BANK"
    )
    
    // Payment providers
    val PAYMENT_PROVIDERS = mapOf(
        "FLUTTERWAVE" to "Flutterwave",
        "PAYSTACK" to "Paystack", 
        "INTERSWITCH" to "Interswitch",
        "REMITA" to "Remita"
    )
    
    fun getPaymentMethodForCountry(country: String, method: String): PaymentMethod {
        return when {
            country == "NG" -> when (method.uppercase()) {
                "FLUTTERWAVE" -> PaymentMethod.FLUTTERWAVE
                "PAYSTACK" -> PaymentMethod.PAYSTACK
                "INTERSWITCH" -> PaymentMethod.INTERSWITCH
                "GTBANK", "GTB" -> PaymentMethod.GTBANK
                "ZENITH" -> PaymentMethod.ZENITH_BANK
                "ACCESS" -> PaymentMethod.ACCESS_BANK
                "FIRST" -> PaymentMethod.FIRST_BANK
                "UBA" -> PaymentMethod.UBA
                "MOBILE_MONEY" -> PaymentMethod.MOBILE_MONEY_NG
                "BANK_ACCOUNT" -> PaymentMethod.BANK_ACCOUNT_NG
                else -> PaymentMethod.BANK_TRANSFER
            }
            else -> when (method.uppercase()) {
                "UPI" -> PaymentMethod.UPI
                "BANK_TRANSFER" -> PaymentMethod.BANK_TRANSFER
                "CARD" -> PaymentMethod.CREDIT_CARD
                else -> PaymentMethod.UPI
            }
        }
    }
    
    fun getTransactionTypeForPaymentMethod(paymentMethod: PaymentMethod, isDeposit: Boolean): TransactionType {
        return when (paymentMethod) {
            PaymentMethod.UPI -> if (isDeposit) TransactionType.UPI_DEPOSIT else TransactionType.UPI_WITHDRAWAL
            PaymentMethod.BANK_TRANSFER -> if (isDeposit) TransactionType.BANK_TRANSFER_DEPOSIT else TransactionType.BANK_TRANSFER_WITHDRAWAL
            PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD -> TransactionType.CARD_PAYMENT
            PaymentMethod.MOBILE_MONEY_NG -> TransactionType.MOBILE_MONEY
            else -> if (isDeposit) TransactionType.DEPOSIT else TransactionType.WITHDRAWAL
        }
    }
    
    fun formatNGNAmount(amount: Double): String {
        return "₦${String.format("%,.2f", amount)}"
    }
    
    fun formatINRAmount(amount: Double): String {
        return "₹${String.format("%,.2f", amount)}"
    }
    
    fun formatAmount(amount: Double, currency: String): String {
        return when (currency.uppercase()) {
            "NGN" -> formatNGNAmount(amount)
            "INR" -> formatINRAmount(amount)
            "USD" -> "$${String.format("%,.2f", amount)}"
            "EUR" -> "€${String.format("%,.2f", amount)}"
            "GBP" -> "£${String.format("%,.2f", amount)}"
            else -> "$currency ${String.format("%,.2f", amount)}"
        }
    }
    
    fun getCurrencySymbol(currency: String): String {
        return when (currency.uppercase()) {
            "NGN" -> "₦"
            "INR" -> "₹"
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            else -> currency
        }
    }

    /**
     * NEW: Formats a currency amount with the correct symbol and removes unnecessary trailing decimals.
     * This is the new standard function for displaying money in the UI.
     * Example: formatAmountTidy(5000.0, "INR") -> "₹5000"
     * Example: formatAmountTidy(10.50, "NGN") -> "₦10.5"
     */
    fun formatAmountTidy(amount: Double, currency: String): String {
        // First, format with symbol, commas, and two decimal places.
        val formatted = formatAmount(amount, currency) // e.g., "₹5,000.00" or "₦10.50"
        // Then, remove trailing ".00" only if it's there.
        return formatted.replace(Regex("(?<=\\d)\\.00(?!\\d)"), "")
    }

    fun validateCurrency(currency: String): Boolean {
        return Wallet.SUPPORTED_CURRENCIES.containsKey(currency.uppercase())
    }
    
    fun validateAmountForCurrency(amount: Double, currency: String): Boolean {
        return when (currency.uppercase()) {
            "NGN" -> amount >= 100.0 // Minimum NGN 100
            "INR" -> amount >= 10.0  // Minimum INR 10
            "USD" -> amount >= 1.0   // Minimum USD 1
            "EUR" -> amount >= 1.0   // Minimum EUR 1
            "GBP" -> amount >= 1.0   // Minimum GBP 1
            else -> amount > 0.0
        }
    }
    
    fun getMinimumAmountForCurrency(currency: String): Double {
        return when (currency.uppercase()) {
            "NGN" -> 100.0
            "INR" -> 10.0
            "USD" -> 1.0
            "EUR" -> 1.0
            "GBP" -> 1.0
            else -> 1.0
        }
    }
    
    fun validateNigerianBankAccount(accountNumber: String): Boolean {
        // Nigerian bank account numbers are typically 10 digits
        return accountNumber.length == 10 && accountNumber.all { it.isDigit() }
    }
    
    fun validateNigerianPhoneNumber(phoneNumber: String): Boolean {
        // Nigerian phone numbers start with +234 and are 11 digits
        val cleanNumber = phoneNumber.replace("+234", "").replace("234", "")
        return cleanNumber.length == 10 && cleanNumber.all { it.isDigit() }
    }
    
    fun getNigerianPaymentDescription(paymentMethod: PaymentMethod, amount: Double): String {
        val formattedAmount = formatNGNAmount(amount)
        return when (paymentMethod) {
            PaymentMethod.FLUTTERWAVE -> "Deposit via Flutterwave ($formattedAmount)"
            PaymentMethod.PAYSTACK -> "Deposit via Paystack ($formattedAmount)"
            PaymentMethod.INTERSWITCH -> "Deposit via Interswitch ($formattedAmount)"
            PaymentMethod.GTBANK -> "Bank transfer to GTBank ($formattedAmount)"
            PaymentMethod.ZENITH_BANK -> "Bank transfer to Zenith Bank ($formattedAmount)"
            PaymentMethod.ACCESS_BANK -> "Bank transfer to Access Bank ($formattedAmount)"
            PaymentMethod.FIRST_BANK -> "Bank transfer to First Bank ($formattedAmount)"
            PaymentMethod.UBA -> "Bank transfer to UBA ($formattedAmount)"
            PaymentMethod.MOBILE_MONEY_NG -> "Mobile money transfer ($formattedAmount)"
            PaymentMethod.BANK_ACCOUNT_NG -> "Bank account transfer ($formattedAmount)"
            else -> "Deposit ($formattedAmount)"
        }
    }
    
    fun getIndianPaymentDescription(paymentMethod: PaymentMethod, amount: Double): String {
        val formattedAmount = formatINRAmount(amount)
        return when (paymentMethod) {
            PaymentMethod.UPI -> "UPI payment ($formattedAmount)"
            PaymentMethod.BANK_TRANSFER -> "Bank transfer ($formattedAmount)"
            PaymentMethod.CREDIT_CARD -> "Credit card payment ($formattedAmount)"
            PaymentMethod.DEBIT_CARD -> "Debit card payment ($formattedAmount)"
            else -> "Payment ($formattedAmount)"
        }
    }
    
    fun getPaymentDescription(paymentMethod: PaymentMethod, amount: Double, currency: String): String {
        return when (currency.uppercase()) {
            "NGN" -> getNigerianPaymentDescription(paymentMethod, amount)
            "INR" -> getIndianPaymentDescription(paymentMethod, amount)
            else -> "Payment (${formatAmount(amount, currency)})"
        }
    }
    
    fun convertCurrency(amount: Double, fromCurrency: String, toCurrency: String, exchangeRate: Double = 1.0): Double {
        if (fromCurrency.uppercase() == toCurrency.uppercase()) {
            return amount
        }
        return amount * exchangeRate
    }
    
    fun getCurrencyDisplayInfo(currency: String): String {
        val currencyInfo = Wallet.getCurrencyInfo(currency)
        return if (currencyInfo != null) {
            "${currencyInfo.symbol} ${currencyInfo.name}"
        } else {
            currency
        }
    }
} 