package com.cehpoint.netwin.payments

import android.app.Activity
import android.content.Context
import android.util.Log
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject

class RazorpayManager : PaymentGatewayManager, PaymentResultListener {
    private var onPaymentResult: ((Boolean, String?, String?) -> Unit)? = null
    
    override fun initialize(context: Context) {
        Checkout.preload(context.applicationContext)
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
            this.onPaymentResult = onResult
            
            // Set the key for Razorpay (this should be configured properly)
            Checkout().setKeyID("rzp_test_1DP5mmOlF5G5ag") // Test key, replace with production key
            
            val checkout = Checkout()
            val options = JSONObject().apply {
                put("currency", currency)
                put("amount", amountMinorUnits) // in subunits
                if (orderId != null) put("order_id", orderId)
                put("prefill", JSONObject().apply {
                    if (!customerEmail.isNullOrBlank()) put("email", customerEmail)
                    if (!customerPhone.isNullOrBlank()) put("contact", customerPhone)
                })
                if (metadata.isNotEmpty()) put("notes", JSONObject(metadata))
                put("theme", JSONObject().apply {
                    put("color", "#6C3AFF") // NetWin brand color
                })
            }
            
            // Set this instance as the payment result listener
            checkout.setKeyID("rzp_test_1DP5mmOlF5G5ag")
            
            // For Compose, we need to handle this differently since we can't directly implement PaymentResultListener
            // We'll use the activity's onActivityResult mechanism
            checkout.open(activity, options)
            
        } catch (e: Exception) {
            Log.e("RazorpayManager", "Failed to start payment", e)
            onResult(false, null, e.message)
        }
    }
    
    override fun onPaymentSuccess(razorpayPaymentId: String) {
        Log.d("RazorpayManager", "Payment successful - ID: $razorpayPaymentId")
        onPaymentResult?.invoke(true, razorpayPaymentId, null)
        onPaymentResult = null
    }
    
    override fun onPaymentError(code: Int, response: String) {
        Log.e("RazorpayManager", "Payment failed - Code: $code, Response: $response")
        onPaymentResult?.invoke(false, null, response)
        onPaymentResult = null
    }
}


