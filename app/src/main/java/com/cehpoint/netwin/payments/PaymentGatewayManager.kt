package com.cehpoint.netwin.payments

import android.app.Activity
import android.content.Context

interface PaymentGatewayManager {
    fun initialize(context: Context)
    fun startPayment(activity: Activity, amountMinorUnits: Long, currency: String, orderId: String? = null, customerEmail: String? = null, customerPhone: String? = null, metadata: Map<String, String> = emptyMap(), onResult: (success: Boolean, transactionId: String?, errorMessage: String?) -> Unit)
}


