package com.cehpoint.netwin.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for RegistrationStepData validate() methods
 * Focuses on step-scoped validation rules for REVIEW and DETAILS steps
 */
class RegistrationStepDataValidationTest {

    @Test
    fun `validate REVIEW step with valid tournament ID should pass`() {
        val stepData = RegistrationStepData(
            tournamentId = "valid-tournament-123",
            inGameId = "", // Empty is OK for REVIEW step
            teamName = "", // Empty is OK for REVIEW step
            paymentMethod = "wallet",
            termsAccepted = false // False is OK for REVIEW step
        )

        val result = stepData.validate(RegistrationStep.REVIEW)

        assertNull("REVIEW validation should pass with valid tournament ID", result)
    }

    @Test
    fun `validate REVIEW step with empty tournament ID should fail`() {
        val stepData = RegistrationStepData(
            tournamentId = "", // Invalid
            inGameId = "ValidPlayer123",
            teamName = "ValidTeam",
            paymentMethod = "wallet",
            termsAccepted = true
        )

        val result = stepData.validate(RegistrationStep.REVIEW)

        assertEquals("REVIEW validation should fail with empty tournament ID", 
            "Tournament ID is required", result)
    }

    @Test
    fun `validate REVIEW step should ignore other fields`() {
        val stepData = RegistrationStepData(
            tournamentId = "valid-tournament-123",
            inGameId = "", // Empty but should be ignored for REVIEW
            teamName = "", // Empty but should be ignored for REVIEW
            paymentMethod = "invalid", // Invalid but should be ignored for REVIEW
            termsAccepted = false // False but should be ignored for REVIEW
        )

        val result = stepData.validate(RegistrationStep.REVIEW)

        assertNull("REVIEW validation should ignore fields not relevant to this step", result)
    }

    @Test
    fun `validate DETAILS step with valid data should pass`() {
        val stepData = RegistrationStepData(
            inGameId = "ValidPlayer123",
            teamName = "ValidTeam",
            tournamentId = "valid-tournament-123",
            paymentMethod = "wallet", // Should be ignored for DETAILS step
            termsAccepted = false // Should be ignored for DETAILS step
        )

        val result = stepData.validate(RegistrationStep.DETAILS)

        assertNull("DETAILS validation should pass with valid data", result)
    }

    @Test
    fun `validate DETAILS step with empty inGameId should fail`() {
        val stepData = RegistrationStepData(
            inGameId = "", // Invalid
            teamName = "ValidTeam",
            tournamentId = "valid-tournament-123",
            paymentMethod = "wallet",
            termsAccepted = true
        )

        val result = stepData.validate(RegistrationStep.DETAILS)

        assertEquals("DETAILS validation should fail with empty inGameId", 
            "In-game ID is required", result)
    }

    @Test
    fun `validate DETAILS step with short inGameId should fail`() {
        val stepData = RegistrationStepData(
            inGameId = "ab", // Too short (less than 3 characters)
            teamName = "ValidTeam",
            tournamentId = "valid-tournament-123",
            paymentMethod = "wallet",
            termsAccepted = true
        )

        val result = stepData.validate(RegistrationStep.DETAILS)

        assertEquals("DETAILS validation should fail with short inGameId", 
            "In-game ID must be at least 3 characters", result)
    }

    @Test
    fun `validate DETAILS step with empty teamName should fail`() {
        val stepData = RegistrationStepData(
            inGameId = "ValidPlayer123",
            teamName = "", // Invalid
            tournamentId = "valid-tournament-123",
            paymentMethod = "wallet",
            termsAccepted = true
        )

        val result = stepData.validate(RegistrationStep.DETAILS)

        assertEquals("DETAILS validation should fail with empty teamName", 
            "Team name is required", result)
    }

    @Test
    fun `validate DETAILS step with short teamName should fail`() {
        val stepData = RegistrationStepData(
            inGameId = "ValidPlayer123",
            teamName = "A", // Too short (less than 2 characters)
            tournamentId = "valid-tournament-123",
            paymentMethod = "wallet",
            termsAccepted = true
        )

        val result = stepData.validate(RegistrationStep.DETAILS)

        assertEquals("DETAILS validation should fail with short teamName", 
            "Team name must be at least 2 characters", result)
    }

    @Test
    fun `validate DETAILS step with empty tournamentId should fail`() {
        val stepData = RegistrationStepData(
            inGameId = "ValidPlayer123",
            teamName = "ValidTeam",
            tournamentId = "", // Invalid
            paymentMethod = "wallet",
            termsAccepted = true
        )

        val result = stepData.validate(RegistrationStep.DETAILS)

        assertEquals("DETAILS validation should fail with empty tournamentId", 
            "Tournament ID is required", result)
    }

    @Test
    fun `validate DETAILS step should ignore payment fields`() {
        val stepData = RegistrationStepData(
            inGameId = "ValidPlayer123",
            teamName = "ValidTeam",
            tournamentId = "valid-tournament-123",
            paymentMethod = "invalid-method", // Should be ignored for DETAILS step
            termsAccepted = false // Should be ignored for DETAILS step
        )

        val result = stepData.validate(RegistrationStep.DETAILS)

        assertNull("DETAILS validation should ignore payment-related fields", result)
    }

    @Test
    fun `validate PAYMENT step with valid payment method should pass`() {
        val stepData = RegistrationStepData(
            paymentMethod = "wallet",
            inGameId = "", // Should be ignored for PAYMENT step
            teamName = "", // Should be ignored for PAYMENT step
            tournamentId = "valid-tournament-123",
            termsAccepted = false // Should be ignored for PAYMENT step
        )

        val result = stepData.validate(RegistrationStep.PAYMENT)

        assertNull("PAYMENT validation should pass with valid payment method", result)
    }

    @Test
    fun `validate PAYMENT step with empty payment method should fail`() {
        val stepData = RegistrationStepData(
            paymentMethod = "", // Invalid
            inGameId = "ValidPlayer123",
            teamName = "ValidTeam",
            tournamentId = "valid-tournament-123",
            termsAccepted = true
        )

        val result = stepData.validate(RegistrationStep.PAYMENT)

        assertEquals("PAYMENT validation should fail with empty payment method", 
            "Payment method is required", result)
    }

    @Test
    fun `validate PAYMENT step with invalid payment method should fail`() {
        val stepData = RegistrationStepData(
            paymentMethod = "bitcoin", // Invalid payment method
            inGameId = "ValidPlayer123",
            teamName = "ValidTeam",
            tournamentId = "valid-tournament-123",
            termsAccepted = true
        )

        val result = stepData.validate(RegistrationStep.PAYMENT)

        assertEquals("PAYMENT validation should fail with invalid payment method", 
            "Invalid payment method", result)
    }

    @Test
    fun `validate CONFIRM step with valid terms acceptance should pass`() {
        val stepData = RegistrationStepData(
            termsAccepted = true,
            inGameId = "ValidPlayer123", // Should be ignored for CONFIRM step validation
            teamName = "ValidTeam", // Should be ignored for CONFIRM step validation
            paymentMethod = "wallet", // Should be ignored for CONFIRM step validation
            tournamentId = "valid-tournament-123"
        )

        val result = stepData.validate(RegistrationStep.CONFIRM)

        assertNull("CONFIRM validation should pass with accepted terms", result)
    }

    @Test
    fun `validate CONFIRM step with terms not accepted should fail`() {
        val stepData = RegistrationStepData(
            termsAccepted = false, // Invalid
            inGameId = "ValidPlayer123",
            teamName = "ValidTeam",
            paymentMethod = "wallet",
            tournamentId = "valid-tournament-123"
        )

        val result = stepData.validate(RegistrationStep.CONFIRM)

        assertEquals("CONFIRM validation should fail with terms not accepted", 
            "You must accept the terms and conditions", result)
    }

    @Test
    fun `validateAll with all valid data should pass`() {
        val stepData = RegistrationStepData(
            inGameId = "ValidPlayer123",
            teamName = "ValidTeam",
            paymentMethod = "wallet",
            termsAccepted = true,
            tournamentId = "valid-tournament-123"
        )

        val result = stepData.validateAll()

        assertNull("validateAll should pass with all valid data", result)
    }

    @Test
    fun `validateAll should return first validation error found`() {
        val stepData = RegistrationStepData(
            inGameId = "", // Invalid - will fail DETAILS validation
            teamName = "", // Also invalid - but should return first error
            paymentMethod = "invalid", // Also invalid
            termsAccepted = false, // Also invalid
            tournamentId = "" // Also invalid - will fail REVIEW validation first
        )

        val result = stepData.validateAll()

        assertEquals("validateAll should return first validation error", 
            "Tournament ID is required", result) // REVIEW step is validated first
    }

    @Test
    fun `isStepComplete should return true for valid step data`() {
        val stepData = RegistrationStepData(
            inGameId = "ValidPlayer123",
            teamName = "ValidTeam",
            tournamentId = "valid-tournament-123"
        )

        val isCompleteDetails = stepData.isStepComplete(RegistrationStep.DETAILS)
        val isCompleteReview = stepData.isStepComplete(RegistrationStep.REVIEW)

        assertTrue("DETAILS step should be complete with valid data", isCompleteDetails)
        assertTrue("REVIEW step should be complete with valid data", isCompleteReview)
    }

    @Test
    fun `isStepComplete should return false for invalid step data`() {
        val stepData = RegistrationStepData(
            inGameId = "", // Invalid for DETAILS
            teamName = "ValidTeam",
            tournamentId = "" // Invalid for REVIEW
        )

        val isCompleteDetails = stepData.isStepComplete(RegistrationStep.DETAILS)
        val isCompleteReview = stepData.isStepComplete(RegistrationStep.REVIEW)

        assertFalse("DETAILS step should not be complete with invalid inGameId", isCompleteDetails)
        assertFalse("REVIEW step should not be complete with invalid tournamentId", isCompleteReview)
    }

    @Test
    fun `isComplete should return true only when all steps are valid`() {
        val validStepData = RegistrationStepData(
            inGameId = "ValidPlayer123",
            teamName = "ValidTeam",
            paymentMethod = "wallet",
            termsAccepted = true,
            tournamentId = "valid-tournament-123"
        )

        val invalidStepData = RegistrationStepData(
            inGameId = "", // Invalid
            teamName = "ValidTeam",
            paymentMethod = "wallet",
            termsAccepted = true,
            tournamentId = "valid-tournament-123"
        )

        assertTrue("isComplete should return true for all valid data", validStepData.isComplete())
        assertFalse("isComplete should return false for any invalid data", invalidStepData.isComplete())
    }

    @Test
    fun `validate should handle edge cases correctly`() {
        // Test minimum valid lengths
        val edgeCaseData = RegistrationStepData(
            inGameId = "abc", // Exactly 3 characters (minimum)
            teamName = "AB", // Exactly 2 characters (minimum)
            paymentMethod = "upi", // Valid alternative payment method
            termsAccepted = true,
            tournamentId = "t" // Single character should be valid
        )

        assertNull("REVIEW should pass with single character tournament ID", 
            edgeCaseData.validate(RegistrationStep.REVIEW))
        assertNull("DETAILS should pass with minimum valid lengths", 
            edgeCaseData.validate(RegistrationStep.DETAILS))
        assertNull("PAYMENT should pass with valid alternative payment method", 
            edgeCaseData.validate(RegistrationStep.PAYMENT))
        assertNull("CONFIRM should pass with accepted terms", 
            edgeCaseData.validate(RegistrationStep.CONFIRM))
    }

    @Test
    fun `validate PAYMENT step with all valid payment methods`() {
        val validPaymentMethods = listOf("wallet", "upi", "card")
        
        validPaymentMethods.forEach { method ->
            val stepData = RegistrationStepData(
                paymentMethod = method,
                tournamentId = "valid-tournament-123"
            )
            
            val result = stepData.validate(RegistrationStep.PAYMENT)
            assertNull("PAYMENT validation should pass with $method", result)
        }
    }
}
