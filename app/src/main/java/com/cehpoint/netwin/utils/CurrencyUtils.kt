package com.cehpoint.netwin.utils

object CurrencyUtils {
    
    fun getDefaultCurrencyForCountry(countryCode: String): String {
        return when (countryCode.uppercase()) {
            "IN", "INDIA" -> "INR"
            "NG", "NIGERIA" -> "NGN"
            "US", "USA" -> "USD"
            "GB", "UK" -> "GBP"
            "DE", "GERMANY" -> "EUR"
            else -> "INR" // Default to INR
        }
    }
    
    data class CurrencyInfo(
        val code: String,
        val symbol: String,
        val name: String
    )
    
    fun getCurrencyInfo(currencyCode: String): CurrencyInfo {
        return when (currencyCode.uppercase()) {
            "INR" -> CurrencyInfo("INR", "₹", "Indian Rupee")
            "NGN" -> CurrencyInfo("NGN", "₦", "Nigerian Naira")
            "USD" -> CurrencyInfo("USD", "$", "US Dollar")
            "GBP" -> CurrencyInfo("GBP", "£", "British Pound")
            "EUR" -> CurrencyInfo("EUR", "€", "Euro")
            else -> CurrencyInfo("INR", "₹", "Indian Rupee")
        }
    }
}
