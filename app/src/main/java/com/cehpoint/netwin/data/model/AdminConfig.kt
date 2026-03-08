package com.cehpoint.netwin.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Admin configuration for payment settings
 * Maps to admin_config/upi_settings/wallet_config document in Firestore
 */
data class AdminConfig(
    @DocumentId
    val id: String = "",
    val walletConfig: Map<String, CurrencyConfig> = emptyMap()
)

/**
 * Currency-specific payment configuration
 * Maps to admin_config/upi_settings/wallet_config/{currency} in Firestore
 */
data class CurrencyConfig(
    val displayName: String = "",
    val isActive: Boolean = false,
    val upiId: String? = null,           // For INR - NetWin's UPI ID
    val paymentLink: String? = null,     // For NGN/USD - Payment gateway links
    val updatedAt: Timestamp? = null,
    val updatedBy: String = ""
)

/**
 * UPI-specific settings for INR transactions
 */
data class UpiSettings(
    val upiId: String = "",
    val displayName: String = "",
    val isActive: Boolean = false,
    val qrCodeEnabled: Boolean = true,
    val minAmount: Double = 10.0,
    val maxAmount: Double = 100000.0,
    val updatedAt: Timestamp? = null,
    val updatedBy: String = ""
)
