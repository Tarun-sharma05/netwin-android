package com.cehpoint.netwin.data.model

import com.google.firebase.firestore.DocumentId
 
data class Wallet(
    @DocumentId
    val userId: String = "",
    val balance: Double = 0.0, // total balance (legacy)
    val withdrawableBalance: Double = 0.0,
    val bonusBalance: Double = 0.0,
    val currency: String = "INR", // Multi-currency support: INR, NGN, USD, etc.
    val currencySymbol: String = "₹", // Currency symbol for display
    val exchangeRate: Double = 1.0, // Exchange rate to base currency (optional)
    val lastUpdated: Long = System.currentTimeMillis() // Last balance update timestamp
) {
    companion object {
        // Supported currencies with their symbols and validation rules
        val SUPPORTED_CURRENCIES = mapOf(
            "INR" to CurrencyInfo("₹", "Indian Rupee", 2),
            "NGN" to CurrencyInfo("₦", "Nigerian Naira", 2),
            "USD" to CurrencyInfo("$", "US Dollar", 2),
            "EUR" to CurrencyInfo("€", "Euro", 2),
            "GBP" to CurrencyInfo("£", "British Pound", 2)
        )
        
        fun isValidCurrency(currency: String): Boolean {
            return SUPPORTED_CURRENCIES.containsKey(currency.uppercase())
        }
        
        fun getCurrencyInfo(currency: String): CurrencyInfo? {
            return SUPPORTED_CURRENCIES[currency.uppercase()]
        }
        
        fun getDefaultCurrencyForCountry(countryCode: String): String {
            return when (countryCode.uppercase()) {
                "IN" -> "INR"
                "NG" -> "NGN"
                "US" -> "USD"
                "GB" -> "GBP"
                "DE", "FR", "IT", "ES" -> "EUR"
                else -> "INR" // Default fallback
            }
        }
    }
}

data class CurrencyInfo(
    val symbol: String,
    val name: String,
    val decimalPlaces: Int
)