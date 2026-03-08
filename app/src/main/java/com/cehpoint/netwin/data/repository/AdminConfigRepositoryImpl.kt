package com.cehpoint.netwin.data.repository

import android.util.Log
import com.cehpoint.netwin.data.model.CurrencyConfig
import com.cehpoint.netwin.data.model.UpiSettings
import com.cehpoint.netwin.domain.repository.AdminConfigRepository
import com.cehpoint.netwin.data.remote.FirebaseManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminConfigRepositoryImpl @Inject constructor(
    private val firebaseManager: FirebaseManager
) : AdminConfigRepository {

    companion object {
        private const val TAG = "AdminConfigRepository"
        private const val ADMIN_CONFIG_COLLECTION = "admin_config"
        private const val UPI_SETTINGS_DOCUMENT = "upi_settings"
        private const val WALLET_CONFIG_FIELD = "wallet_config"
    }

    private val adminConfigCollection = firebaseManager.firestore.collection(ADMIN_CONFIG_COLLECTION)

    override suspend fun getCurrencyConfig(currency: String): Flow<CurrencyConfig?> {
        return callbackFlow {
            val listener = adminConfigCollection
                .document("wallet_config")
                .addSnapshotListener { document, error ->
                    if (error != null) {
                        Log.e(TAG, "Error getting currency config for $currency", error)
                        trySend(null)
                        return@addSnapshotListener
                    }

                    try {
                        val currencyData = document?.get(currency) as? Map<String, Any>
                        
                        if (currencyData != null) {
                            val config = CurrencyConfig(
                                displayName = currencyData["displayName"] as? String ?: "",
                                isActive = currencyData["isActive"] as? Boolean ?: false,
                                upiId = currencyData["upiId"] as? String,
                                paymentLink = currencyData["paymentLink"] as? String,
                                updatedBy = currencyData["updatedBy"] as? String ?: ""
                            )
                            
                            Log.d(TAG, "Currency config for $currency: $config")
                            trySend(config)
                        } else {
                            Log.w(TAG, "No configuration found for currency: $currency")
                            trySend(null)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing currency config for $currency", e)
                        trySend(null)
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    override suspend fun getUpiSettings(): Flow<UpiSettings?> {
        Log.d(TAG, "=== ADMIN CONFIG REPOSITORY - GET UPI SETTINGS ===")
        return callbackFlow {
            Log.d(TAG, "Setting up Firestore listener for admin_config/wallet_config")
            val listener = adminConfigCollection
                .document("wallet_config")
                .addSnapshotListener { document, error ->
                    if (error != null) {
                        Log.e(TAG, "❌ Firestore listener error", error)
                        close(error)
                        return@addSnapshotListener
                    }

                    Log.d(TAG, "📄 Document received - exists: ${document?.exists()}")
                    
                    try {
                        Log.d(TAG, "📋 Document data: ${document?.data}")
                        Log.d(TAG, "🕐 Document timestamp: ${document?.getTimestamp("updatedAt")}")
                        Log.d(TAG, "🔄 Document from cache: ${document?.metadata?.isFromCache}")
                        
                        // Read INR config directly from the document root
                        val inrConfig = document?.get("INR") as? Map<String, Any>
                        Log.d(TAG, "🇮🇳 INR config: $inrConfig")
                        
                        if (inrConfig != null) {
                            val upiId = inrConfig["upiId"] as? String ?: ""
                            val displayName = inrConfig["displayName"] as? String ?: "NetWin Gaming"
                            val isActive = inrConfig["isActive"] as? Boolean ?: false
                            val qrCodeEnabled = inrConfig["qrCodeEnabled"] as? Boolean ?: true
                            val minAmount = (inrConfig["minAmount"] as? Number)?.toDouble() ?: 10.0
                            val maxAmount = (inrConfig["maxAmount"] as? Number)?.toDouble() ?: 100000.0
                            
                            Log.d(TAG, "🔍 Parsed values:")
                            Log.d(TAG, "   UPI ID: '$upiId'")
                            Log.d(TAG, "   Display Name: '$displayName'")
                            Log.d(TAG, "   Is Active: $isActive")
                            Log.d(TAG, "   QR Enabled: $qrCodeEnabled")
                            
                            val upiSettings = UpiSettings(
                                upiId = upiId,
                                displayName = displayName,
                                isActive = isActive,
                                qrCodeEnabled = qrCodeEnabled,
                                minAmount = minAmount,
                                maxAmount = maxAmount
                            )
                            Log.d(TAG, "✅ UPI settings created: $upiSettings")
                            trySend(upiSettings)
                        } else {
                            Log.w(TAG, "⚠️ INR config not found in document")
                            Log.w(TAG, "Available keys in document: ${document?.data?.keys}")
                            trySend(null)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error parsing UPI settings", e)
                        trySend(null)
                    }
                }

            awaitClose { 
                Log.d(TAG, "🔚 Closing Firestore listener")
                listener.remove() 
            }
        }
    }


    override suspend fun isCurrencyActive(currency: String): Flow<Boolean> {
        return callbackFlow {
            val listener = adminConfigCollection
                .document("wallet_config")
                .addSnapshotListener { document, error ->
                    if (error != null) {
                        Log.e(TAG, "Error checking currency active status for $currency", error)
                        trySend(false)
                        return@addSnapshotListener
                    }

                    try {
                        val currencyData = document?.get(currency) as? Map<String, Any>
                        val isActive = currencyData?.get("isActive") as? Boolean ?: false
                        
                        Log.d(TAG, "Currency $currency active status: $isActive")
                        trySend(isActive)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing currency active status for $currency", e)
                        trySend(false)
                    }
                }

            awaitClose { listener.remove() }
        }
    }
}
