package com.cehpoint.netwin.payments

object PaymentGatewayFactory {
    /**
     * Returns null for all currencies as payment gateway managers have been removed
     * in favor of manual UPI deposit system
     */
    fun forCurrency(currency: String): Nothing? {
        return null
    }
}


