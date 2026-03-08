package com.cehpoint.netwin.domain.repository

import com.cehpoint.netwin.data.model.CurrencyConfig
import com.cehpoint.netwin.data.model.UpiSettings
import kotlinx.coroutines.flow.Flow

interface AdminConfigRepository {
    /**
     * Get currency-specific payment configuration
     * @param currency Currency code (INR, NGN, USD)
     * @return Flow of currency configuration
     */
    suspend fun getCurrencyConfig(currency: String): Flow<CurrencyConfig?>
    
    /**
     * Get UPI settings for INR transactions
     * @return Flow of UPI settings including NetWin's UPI ID
     */
    suspend fun getUpiSettings(): Flow<UpiSettings?>
    
    /**
     * Check if a currency is active for payments
     * @param currency Currency code
     * @return Flow of boolean indicating if currency is active
     */
    suspend fun isCurrencyActive(currency: String): Flow<Boolean>
}
