package com.cehpoint.netwin.payments

object PaymentGatewayFactory {
    fun forCurrency(currency: String): PaymentGatewayManager? {
        return when (currency.uppercase()) {
            "INR" -> RazorpayManager()
            "NGN" -> PaystackManager()
            else -> null
        }
    }
}


