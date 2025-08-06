package com.cehpoint.netwin.utils

import android.util.Log
import com.cehpoint.netwin.data.model.Wallet

object CurrencyTestUtils {
    private const val TAG = "CurrencyTestUtils"
    
    /**
     * Test multi-currency functionality
     */
    fun testMultiCurrencySupport() {
        Log.d(TAG, "=== Testing Multi-Currency Support ===")
        
        // Test supported currencies
        val testCurrencies = listOf("INR", "NGN", "USD", "EUR", "GBP")
        
        testCurrencies.forEach { currency ->
            Log.d(TAG, "Testing currency: $currency")
            
            // Test currency validation
            val isValid = NGNTransactionUtils.validateCurrency(currency)
            Log.d(TAG, "  - Currency validation: $isValid")
            
            // Test currency symbol
            val symbol = NGNTransactionUtils.getCurrencySymbol(currency)
            Log.d(TAG, "  - Currency symbol: $symbol")
            
            // Test amount formatting
            val testAmount = 1000.0
            val formattedAmount = NGNTransactionUtils.formatAmount(testAmount, currency)
            Log.d(TAG, "  - Formatted amount: $formattedAmount")
            
            // Test minimum amount validation
            val minAmount = NGNTransactionUtils.getMinimumAmountForCurrency(currency)
            Log.d(TAG, "  - Minimum amount: ${NGNTransactionUtils.formatAmount(minAmount, currency)}")
            
            // Test amount validation
            val isValidAmount = NGNTransactionUtils.validateAmountForCurrency(testAmount, currency)
            Log.d(TAG, "  - Amount validation: $isValidAmount")
            
            Log.d(TAG, "  ---")
        }
        
        // Test country to currency mapping
        val testCountries = mapOf(
            "IN" to "INR",
            "NG" to "NGN", 
            "US" to "USD",
            "GB" to "GBP",
            "DE" to "EUR"
        )
        
        Log.d(TAG, "Testing country to currency mapping:")
        testCountries.forEach { (country, expectedCurrency) ->
            val actualCurrency = Wallet.getDefaultCurrencyForCountry(country)
            val isCorrect = actualCurrency == expectedCurrency
            Log.d(TAG, "  - $country -> $actualCurrency (Expected: $expectedCurrency, Correct: $isCorrect)")
        }
        
        // Test currency conversion
        Log.d(TAG, "Testing currency conversion:")
        val testAmount = 100.0
        val exchangeRate = 0.012 // 1 INR = 0.012 USD (example rate)
        val convertedAmount = NGNTransactionUtils.convertCurrency(testAmount, "INR", "USD", exchangeRate)
        Log.d(TAG, "  - $testAmount INR = ${NGNTransactionUtils.formatAmount(convertedAmount, "USD")} (Rate: $exchangeRate)")
        
        Log.d(TAG, "=== Multi-Currency Test Complete ===")
    }
    
    /**
     * Test NGN-specific functionality
     */
    fun testNGNCurrencySupport() {
        Log.d(TAG, "=== Testing NGN Currency Support ===")
        
        // Test NGN formatting
        val ngnAmount = 50000.0
        val formattedNGN = NGNTransactionUtils.formatAmount(ngnAmount, "NGN")
        Log.d(TAG, "NGN formatting: $formattedNGN")
        
        // Test NGN minimum amount
        val minNGN = NGNTransactionUtils.getMinimumAmountForCurrency("NGN")
        Log.d(TAG, "NGN minimum amount: ${NGNTransactionUtils.formatAmount(minNGN, "NGN")}")
        
        // Test NGN validation
        val validAmount = 1000.0
        val invalidAmount = 50.0
        val isValidValid = NGNTransactionUtils.validateAmountForCurrency(validAmount, "NGN")
        val isValidInvalid = NGNTransactionUtils.validateAmountForCurrency(invalidAmount, "NGN")
        Log.d(TAG, "NGN validation - Valid amount ($validAmount): $isValidValid")
        Log.d(TAG, "NGN validation - Invalid amount ($invalidAmount): $isValidInvalid")
        
        // Test Nigerian bank account validation
        val validAccount = "1234567890"
        val invalidAccount = "123456789"
        val isValidAccount = NGNTransactionUtils.validateNigerianBankAccount(validAccount)
        val isInvalidAccount = NGNTransactionUtils.validateNigerianBankAccount(invalidAccount)
        Log.d(TAG, "Nigerian bank account validation - Valid: $isValidAccount, Invalid: $isInvalidAccount")
        
        // Test Nigerian phone number validation
        val validPhone = "+2348012345678"
        val invalidPhone = "1234567890"
        val isValidPhone = NGNTransactionUtils.validateNigerianPhoneNumber(validPhone)
        val isInvalidPhone = NGNTransactionUtils.validateNigerianPhoneNumber(invalidPhone)
        Log.d(TAG, "Nigerian phone validation - Valid: $isValidPhone, Invalid: $isInvalidPhone")
        
        Log.d(TAG, "=== NGN Currency Test Complete ===")
    }
    
    /**
     * Create a test wallet with specified currency
     */
    fun createTestWallet(userId: String, currency: String): Wallet {
        val currencyInfo = Wallet.getCurrencyInfo(currency)
        return Wallet(
            userId = userId,
            balance = 0.0,
            withdrawableBalance = 0.0,
            bonusBalance = 0.0,
            currency = currency,
            currencySymbol = currencyInfo?.symbol ?: currency,
            exchangeRate = 1.0,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    /**
     * Validate wallet currency configuration
     */
    fun validateWalletCurrency(wallet: Wallet): Boolean {
        val currencyInfo = Wallet.getCurrencyInfo(wallet.currency)
        if (currencyInfo == null) {
            Log.e(TAG, "Invalid currency: ${wallet.currency}")
            return false
        }
        
        if (wallet.currencySymbol != currencyInfo.symbol) {
            Log.e(TAG, "Currency symbol mismatch. Expected: ${currencyInfo.symbol}, Got: ${wallet.currencySymbol}")
            return false
        }
        
        Log.d(TAG, "Wallet currency validation passed for ${wallet.currency}")
        return true
    }
} 