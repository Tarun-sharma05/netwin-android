package com.cehpoint.netwin.payments

import android.app.Activity
import android.content.Context
import android.util.Log
import co.paystack.android.PaystackSdk

class PaystackManager : PaymentGatewayManager {
    override fun initialize(context: Context) {
        PaystackSdk.initialize(context.applicationContext)
    }

    override fun startPayment(
        activity: Activity,
        amountMinorUnits: Long,
        currency: String,
        orderId: String?,
        customerEmail: String?,
        customerPhone: String?,
        metadata: Map<String, String>,
        onResult: (success: Boolean, transactionId: String?, errorMessage: String?) -> Unit
    ) {
        try {
            // Full implementation will be added; this is a stub to unblock integration
            // You will need to create a Charge object and call PaystackSdk.chargeCard
            onResult(false, null, "Not yet implemented")
        } catch (e: Exception) {
            Log.e("PaystackManager", "Failed to start payment", e)
            onResult(false, null, e.message)
        }
    }
}


