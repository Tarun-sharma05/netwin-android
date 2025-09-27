package com.cehpoint.netwin.utils

import com.cehpoint.netwin.data.model.PaymentMethod
import com.cehpoint.netwin.data.model.PaymentProof
import java.util.regex.Pattern

/**
 * Utility class for validating payment proofs and extracting verification data
 */
object PaymentProofValidator {
    
    // UPI Transaction ID patterns (12 digits)
    private val UPI_TRANSACTION_ID_PATTERN = Pattern.compile("^[0-9]{12}$")
    
    // UPI Reference Number patterns (12 digits)
    private val UPI_REFERENCE_PATTERN = Pattern.compile("^[0-9]{12}$")
    
    // UPI ID patterns
    private val UPI_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+$")
    
    // Paystack reference patterns (starts with T followed by numbers)
    private val PAYSTACK_REFERENCE_PATTERN = Pattern.compile("^T[0-9]+$")
    
    // Email pattern
    private val EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    
    // Nigerian account number pattern (10 digits)
    private val NGN_ACCOUNT_PATTERN = Pattern.compile("^[0-9]{10}$")
    
    /**
     * Comprehensive validation for payment proofs
     */
    fun validatePaymentProof(proof: PaymentProof): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Basic validations
        if (proof.amount <= 0) {
            errors.add("Amount must be greater than 0")
        }
        
        if (proof.userId.isBlank()) {
            errors.add("User ID is required")
        }
        
        // Currency-specific validations
        when (proof.currency) {
            "NGN" -> validateNGNPayment(proof, errors, warnings)
            "INR" -> validateINRPayment(proof, errors, warnings)
            else -> errors.add("Unsupported currency: ${proof.currency}")
        }
        
        // Document validations
        validateDocuments(proof, errors, warnings)
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            riskScore = calculateRiskScore(proof, errors, warnings)
        )
    }
    
    private fun validateNGNPayment(proof: PaymentProof, errors: MutableList<String>, warnings: MutableList<String>) {
        // Paystack reference validation
        if (proof.paystackReference.isNullOrBlank()) {
            errors.add("Paystack reference number is required for NGN payments")
        } else if (!PAYSTACK_REFERENCE_PATTERN.matcher(proof.paystackReference).matches()) {
            warnings.add("Paystack reference format appears unusual: ${proof.paystackReference}")
        } else if (proof.paystackReference.length < 3) {
            errors.add("Paystack reference number is too short")
        }
        
        // Email validation
        if (proof.paystackEmail.isNullOrBlank()) {
            errors.add("Email used for payment is required")
        } else if (!EMAIL_PATTERN.matcher(proof.paystackEmail).matches()) {
            errors.add("Invalid email format: ${proof.paystackEmail}")
        }
        
        // Bank transaction ID validation (if provided)
        if (!proof.bankTransactionId.isNullOrBlank()) {
            if (proof.bankTransactionId.length < 6) {
                warnings.add("Bank transaction ID appears too short")
            }
        }
        
        // Bank details validation (if provided)
        if (!proof.accountNumber.isNullOrBlank()) {
            if (!NGN_ACCOUNT_PATTERN.matcher(proof.accountNumber).matches()) {
                errors.add("Nigerian account numbers must be 10 digits")
            }
        }
        
        if (!proof.accountName.isNullOrBlank()) {
            if (proof.accountName.length < 3) {
                errors.add("Account holder name must be at least 3 characters")
            }
            if (proof.accountName.length > 100) {
                warnings.add("Account name is unusually long")
            }
        }
        
        if (!proof.bankName.isNullOrBlank() && proof.bankName.length < 2) {
            errors.add("Bank name must be at least 2 characters")
        }
        
        // Amount-based validations
        if (proof.amount > 1000000) {
            warnings.add("Large amount transaction (>₦1,000,000) requires additional verification")
        }
        
        if (proof.amount > 50000 && proof.bankStatementUrl.isNullOrBlank()) {
            warnings.add("Bank statement required for NGN amounts above ₦50,000")
        }
        
        if (proof.amount < 100) {
            warnings.add("Amount appears unusually low for NGN transactions")
        }
    }
    
    private fun validateINRPayment(proof: PaymentProof, errors: MutableList<String>, warnings: MutableList<String>) {
        // UPI Transaction ID validation
        if (proof.upiTransactionId.isNullOrBlank()) {
            errors.add("UPI Transaction ID is required for INR payments")
        } else if (!UPI_TRANSACTION_ID_PATTERN.matcher(proof.upiTransactionId).matches()) {
            warnings.add("UPI Transaction ID format appears unusual: ${proof.upiTransactionId}")
        } else if (proof.upiTransactionId.length < 6) {
            errors.add("UPI Transaction ID is too short")
        }
        
        // UPI Reference Number validation
        if (proof.upiReferenceNumber.isNullOrBlank()) {
            errors.add("UPI Reference Number (UTR) is required")
        } else if (!UPI_REFERENCE_PATTERN.matcher(proof.upiReferenceNumber).matches()) {
            warnings.add("UPI Reference Number format appears unusual: ${proof.upiReferenceNumber}")
        } else if (proof.upiReferenceNumber.length < 6) {
            errors.add("UPI Reference Number is too short")
        }
        
        // UPI ID validation
        if (proof.senderUpiId.isNullOrBlank()) {
            errors.add("Sender UPI ID is required")
        } else if (!UPI_ID_PATTERN.matcher(proof.senderUpiId).matches()) {
            errors.add("Invalid UPI ID format: ${proof.senderUpiId}")
        } else if (proof.senderUpiId.length > 50) {
            warnings.add("UPI ID is unusually long")
        }
        
        // Receiver UPI ID validation (if provided)
        if (!proof.receiverUpiId.isNullOrBlank()) {
            if (!UPI_ID_PATTERN.matcher(proof.receiverUpiId).matches()) {
                errors.add("Invalid receiver UPI ID format: ${proof.receiverUpiId}")
            }
        }
        
        // UPI App validation
        if (proof.upiAppName.isNullOrBlank()) {
            warnings.add("UPI app name not specified")
        } else if (proof.upiAppName.length < 2) {
            warnings.add("UPI app name appears too short")
        }
        
        // Amount-based validations
        if (proof.amount > 100000) {
            warnings.add("Large amount transaction (>₹100,000) requires additional verification")
        }
        
        if (proof.amount < 10) {
            warnings.add("Amount appears unusually low for INR transactions")
        }
        
        // Check if transaction IDs match (if both provided)
        if (!proof.upiTransactionId.isNullOrBlank() && !proof.upiReferenceNumber.isNullOrBlank()) {
            if (proof.upiTransactionId != proof.upiReferenceNumber) {
                warnings.add("UPI Transaction ID and Reference Number don't match")
            }
        }
    }
    
    private fun validateDocuments(proof: PaymentProof, errors: MutableList<String>, warnings: MutableList<String>) {
        // Screenshot is always required
        if (proof.screenshotUrl.isNullOrBlank()) {
            errors.add("Payment screenshot is required")
        } else if (!proof.screenshotUrl.startsWith("http") && !proof.screenshotUrl.startsWith("/")) {
            warnings.add("Screenshot URL format appears unusual")
        }
        
        // Receipt is recommended
        if (proof.receiptUrl.isNullOrBlank()) {
            warnings.add("Payment receipt not provided (recommended for faster verification)")
        } else if (!proof.receiptUrl.startsWith("http") && !proof.receiptUrl.startsWith("/")) {
            warnings.add("Receipt URL format appears unusual")
        }
        
        // Currency-specific document requirements
        when (proof.currency) {
            "NGN" -> {
                if (proof.amount > 50000 && proof.bankStatementUrl.isNullOrBlank()) {
                    warnings.add("Bank statement required for NGN amounts above ₦50,000")
                } else if (!proof.bankStatementUrl.isNullOrBlank() && !proof.bankStatementUrl.startsWith("http") && !proof.bankStatementUrl.startsWith("/")) {
                    warnings.add("Bank statement URL format appears unusual")
                }
                
                if (proof.amount > 200000 && proof.receiptUrl.isNullOrBlank()) {
                    warnings.add("Payment receipt strongly recommended for NGN amounts above ₦200,000")
                }
            }
            "INR" -> {
                if (proof.amount > 50000 && proof.receiptUrl.isNullOrBlank()) {
                    warnings.add("Payment receipt recommended for INR amounts above ₹50,000")
                }
                
                if (proof.amount > 100000 && proof.receiptUrl.isNullOrBlank()) {
                    warnings.add("Payment receipt required for INR amounts above ₹100,000")
                }
            }
        }
    }
    
    private fun calculateRiskScore(proof: PaymentProof, errors: List<String>, warnings: List<String>): Int {
        var riskScore = 0
        
        // Base risk from errors and warnings
        riskScore += errors.size * 30
        riskScore += warnings.size * 10
        
        // Amount-based risk
        when (proof.currency) {
            "NGN" -> {
                when {
                    proof.amount > 500000 -> riskScore += 25
                    proof.amount > 100000 -> riskScore += 15
                    proof.amount > 50000 -> riskScore += 10
                    proof.amount < 1000 -> riskScore += 5 // Suspiciously low amounts
                }
            }
            "INR" -> {
                when {
                    proof.amount > 100000 -> riskScore += 25
                    proof.amount > 50000 -> riskScore += 15
                    proof.amount > 25000 -> riskScore += 10
                    proof.amount < 100 -> riskScore += 5 // Suspiciously low amounts
                }
            }
        }
        
        // Missing optional documents increase risk
        if (proof.receiptUrl.isNullOrBlank()) riskScore += 10
        if (proof.currency == "NGN" && proof.bankStatementUrl.isNullOrBlank() && proof.amount > 50000) {
            riskScore += 20
        }
        
        return minOf(riskScore, 100) // Cap at 100
    }
    
    /**
     * Extract verification hints from payment proof data
     */
    fun getVerificationHints(proof: PaymentProof): List<VerificationHint> {
        val hints = mutableListOf<VerificationHint>()
        
        when (proof.currency) {
            "NGN" -> {
                hints.add(VerificationHint(
                    type = "paystack_reference",
                    title = "Verify Paystack Reference",
                    description = "Check if reference ${proof.paystackReference} exists in Paystack dashboard",
                    priority = VerificationPriority.HIGH
                ))
                
                hints.add(VerificationHint(
                    type = "email_match",
                    title = "Verify Email Match",
                    description = "Confirm email ${proof.paystackEmail} matches user account",
                    priority = VerificationPriority.MEDIUM
                ))
                
                if (!proof.bankTransactionId.isNullOrBlank()) {
                    hints.add(VerificationHint(
                        type = "bank_transaction",
                        title = "Cross-check Bank Transaction",
                        description = "Verify bank transaction ID ${proof.bankTransactionId}",
                        priority = VerificationPriority.HIGH
                    ))
                }
            }
            
            "INR" -> {
                hints.add(VerificationHint(
                    type = "upi_transaction",
                    title = "Verify UPI Transaction",
                    description = "Check UPI transaction ID ${proof.upiTransactionId} and UTR ${proof.upiReferenceNumber}",
                    priority = VerificationPriority.HIGH
                ))
                
                hints.add(VerificationHint(
                    type = "upi_id_match",
                    title = "Verify UPI ID",
                    description = "Confirm UPI ID ${proof.senderUpiId} belongs to user",
                    priority = VerificationPriority.MEDIUM
                ))
                
                if (!proof.upiAppName.isNullOrBlank()) {
                    hints.add(VerificationHint(
                        type = "upi_app_consistency",
                        title = "Check UPI App Consistency",
                        description = "Verify transaction details match ${proof.upiAppName} format",
                        priority = VerificationPriority.LOW
                    ))
                }
            }
        }
        
        // Document verification hints
        if (!proof.screenshotUrl.isNullOrBlank()) {
            hints.add(VerificationHint(
                type = "screenshot_analysis",
                title = "Analyze Payment Screenshot",
                description = "Check screenshot for transaction details, amount, and timestamp",
                priority = VerificationPriority.HIGH
            ))
        }
        
        return hints.sortedByDescending { it.priority.ordinal }
    }
    
    /**
     * Generate admin checklist for manual verification
     */
    fun generateVerificationChecklist(proof: PaymentProof): List<ChecklistItem> {
        val checklist = mutableListOf<ChecklistItem>()
        
        // Common checks
        checklist.add(ChecklistItem(
            id = "amount_match",
            description = "Amount in proof matches deposit request (${if (proof.currency == "NGN") "₦" else "₹"}${String.format("%.2f", proof.amount)})",
            category = "Basic Verification"
        ))
        
        checklist.add(ChecklistItem(
            id = "timestamp_reasonable",
            description = "Transaction timestamp is reasonable (within expected timeframe)",
            category = "Basic Verification"
        ))
        
        // Currency-specific checks
        when (proof.currency) {
            "NGN" -> {
                checklist.add(ChecklistItem(
                    id = "paystack_reference_valid",
                    description = "Paystack reference ${proof.paystackReference} is valid and exists",
                    category = "Payment Verification"
                ))
                
                checklist.add(ChecklistItem(
                    id = "email_matches_user",
                    description = "Email ${proof.paystackEmail} matches user account or is authorized",
                    category = "User Verification"
                ))
                
                if (!proof.bankName.isNullOrBlank()) {
                    checklist.add(ChecklistItem(
                        id = "bank_details_consistent",
                        description = "Bank details are consistent (${proof.bankName}, ${proof.accountNumber})",
                        category = "Bank Verification"
                    ))
                }
            }
            
            "INR" -> {
                checklist.add(ChecklistItem(
                    id = "upi_transaction_valid",
                    description = "UPI Transaction ID ${proof.upiTransactionId} is valid",
                    category = "Payment Verification"
                ))
                
                checklist.add(ChecklistItem(
                    id = "utr_matches",
                    description = "UTR ${proof.upiReferenceNumber} matches transaction",
                    category = "Payment Verification"
                ))
                
                checklist.add(ChecklistItem(
                    id = "upi_id_belongs_to_user",
                    description = "UPI ID ${proof.senderUpiId} belongs to the user",
                    category = "User Verification"
                ))
            }
        }
        
        // Document checks
        if (!proof.screenshotUrl.isNullOrBlank()) {
            checklist.add(ChecklistItem(
                id = "screenshot_clear_readable",
                description = "Screenshot is clear, readable, and shows complete transaction details",
                category = "Document Verification"
            ))
        }
        
        if (!proof.receiptUrl.isNullOrBlank()) {
            checklist.add(ChecklistItem(
                id = "receipt_authentic",
                description = "Receipt appears authentic and matches other provided information",
                category = "Document Verification"
            ))
        }
        
        return checklist.groupBy { it.category }.flatMap { (category, items) ->
            listOf(ChecklistItem(id = "header_$category", description = "=== $category ===", category = "Header")) + items
        }.filter { it.category != "Header" || it.description.startsWith("===") }
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>,
    val riskScore: Int
)

data class VerificationHint(
    val type: String,
    val title: String,
    val description: String,
    val priority: VerificationPriority
)

enum class VerificationPriority {
    LOW, MEDIUM, HIGH
}

data class ChecklistItem(
    val id: String,
    val description: String,
    val category: String,
    val isCompleted: Boolean = false
)
