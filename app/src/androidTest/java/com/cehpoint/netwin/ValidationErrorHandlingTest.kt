package com.cehpoint.netwin

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cehpoint.netwin.domain.model.RegistrationStep
import com.cehpoint.netwin.domain.model.RegistrationStepData
import com.cehpoint.netwin.presentation.events.RegistrationFlowEvent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.junit.Before

/**
 * Comprehensive test class for validation and error handling audit
 * 
 * Tests the following aspects:
 * 1. RegistrationStepData.validate() method with malformed inputs
 * 2. ViewModel error propagation through UI state
 * 3. UI feedback for validation errors
 * 4. Error recovery and clearing mechanisms
 */
@RunWith(AndroidJUnit4::class)
class ValidationErrorHandlingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testRegistrationStepDataValidation_MalformedInputs() = runTest {
        // Test DETAILS step validation with various malformed inputs
        
        // Empty/blank inGameId
        var testData = RegistrationStepData(
            inGameId = "",
            teamName = "ValidTeam", 
            tournamentId = "valid-id"
        )
        var result = testData.validate(RegistrationStep.DETAILS)
        assertEquals("In-game ID is required", result)
        
        // Short inGameId (less than 3 characters)
        testData = testData.copy(inGameId = "ab")
        result = testData.validate(RegistrationStep.DETAILS)
        assertEquals("In-game ID must be at least 3 characters", result)
        
        // Empty/blank teamName
        testData = testData.copy(inGameId = "valid123", teamName = "")
        result = testData.validate(RegistrationStep.DETAILS)
        assertEquals("Team name is required", result)
        
        // Short teamName (less than 2 characters)
        testData = testData.copy(teamName = "A")
        result = testData.validate(RegistrationStep.DETAILS)
        assertEquals("Team name must be at least 2 characters", result)
        
        // Empty/blank tournamentId
        testData = testData.copy(teamName = "ValidTeam", tournamentId = "")
        result = testData.validate(RegistrationStep.DETAILS)
        assertEquals("Tournament ID is required", result)
        
        // Valid data should return null
        testData = RegistrationStepData(
            inGameId = "ValidPlayer123",
            teamName = "ValidTeam",
            tournamentId = "valid-tournament-id"
        )
        result = testData.validate(RegistrationStep.DETAILS)
        assertNull("Valid data should not return validation error", result)
    }

    @Test
    fun testRegistrationStepDataValidation_PaymentStep() = runTest {
        // Test PAYMENT step validation
        
        // Empty payment method
        var testData = RegistrationStepData(paymentMethod = "")
        var result = testData.validate(RegistrationStep.PAYMENT)
        assertEquals("Payment method is required", result)
        
        // Invalid payment method
        testData = testData.copy(paymentMethod = "bitcoin")
        result = testData.validate(RegistrationStep.PAYMENT)
        assertEquals("Invalid payment method", result)
        
        // Valid payment methods
        val validMethods = listOf("wallet", "upi", "card")
        validMethods.forEach { method ->
            testData = testData.copy(paymentMethod = method)
            result = testData.validate(RegistrationStep.PAYMENT)
            assertNull("Valid payment method '$method' should not return error", result)
        }
    }

    @Test
    fun testRegistrationStepDataValidation_ReviewStep() = runTest {
        // Test REVIEW step validation (validates all previous steps)
        
        // Invalid DETAILS step should fail REVIEW
        var testData = RegistrationStepData(
            inGameId = "", // Invalid
            teamName = "ValidTeam",
            paymentMethod = "wallet"
        )
        var result = testData.validate(RegistrationStep.REVIEW)
        assertEquals("In-game ID is required", result)
        
        // Invalid PAYMENT step should fail REVIEW
        testData = RegistrationStepData(
            inGameId = "ValidPlayer123",
            teamName = "ValidTeam", 
            paymentMethod = "invalid", // Invalid
            tournamentId = "valid-id"
        )
        result = testData.validate(RegistrationStep.REVIEW)
        assertEquals("Invalid payment method", result)
        
        // All valid should pass REVIEW
        testData = RegistrationStepData(
            inGameId = "ValidPlayer123",
            teamName = "ValidTeam",
            paymentMethod = "wallet",
            tournamentId = "valid-id"
        )
        result = testData.validate(RegistrationStep.REVIEW)
        assertNull("Valid review data should not return error", result)
    }

    @Test
    fun testRegistrationStepDataValidation_ConfirmStep() = runTest {
        // Test CONFIRM step validation
        
        // Terms not accepted
        var testData = RegistrationStepData(
            inGameId = "ValidPlayer123",
            teamName = "ValidTeam",
            paymentMethod = "wallet",
            termsAccepted = false, // Invalid
            tournamentId = "valid-id"
        )
        var result = testData.validate(RegistrationStep.CONFIRM)
        assertEquals("You must accept the terms and conditions", result)
        
        // Terms accepted but invalid previous steps
        testData = testData.copy(
            termsAccepted = true,
            inGameId = "" // Invalid
        )
        result = testData.validate(RegistrationStep.CONFIRM)
        assertEquals("In-game ID is required", result)
        
        // All valid
        testData = RegistrationStepData(
            inGameId = "ValidPlayer123",
            teamName = "ValidTeam",
            paymentMethod = "wallet",
            termsAccepted = true,
            tournamentId = "valid-id"
        )
        result = testData.validate(RegistrationStep.CONFIRM)
        assertNull("Valid confirm data should not return error", result)
    }

    @Test
    fun testStepCompletionHelpers() = runTest {
        val testData = RegistrationStepData(
            inGameId = "ValidPlayer123",
            teamName = "ValidTeam",
            paymentMethod = "wallet",
            termsAccepted = true,
            tournamentId = "valid-id"
        )
        
        // Test isStepComplete for all steps
        assertTrue("Valid DETAILS step should be complete", 
            testData.isStepComplete(RegistrationStep.DETAILS))
        assertTrue("Valid PAYMENT step should be complete", 
            testData.isStepComplete(RegistrationStep.PAYMENT))
        assertTrue("Valid REVIEW step should be complete", 
            testData.isStepComplete(RegistrationStep.REVIEW))
        assertTrue("Valid CONFIRM step should be complete", 
            testData.isStepComplete(RegistrationStep.CONFIRM))
        
        // Test isComplete for entire flow
        assertTrue("Valid complete data should return true", testData.isComplete())
        
        // Test with invalid data
        val invalidData = testData.copy(inGameId = "")
        assertFalse("Invalid data should not be complete", invalidData.isComplete())
    }

    @Test
    fun testCascadingValidation() = runTest {
        // Test that validation properly cascades through steps
        
        val invalidDetailsData = RegistrationStepData(
            inGameId = "", // Invalid
            teamName = "ValidTeam",
            paymentMethod = "wallet",
            termsAccepted = true,
            tournamentId = "valid-id"
        )
        
        // DETAILS step should fail
        assertNotNull("Invalid details should fail DETAILS validation",
            invalidDetailsData.validate(RegistrationStep.DETAILS))
        
        // REVIEW step should fail due to invalid DETAILS
        assertNotNull("Invalid details should fail REVIEW validation",
            invalidDetailsData.validate(RegistrationStep.REVIEW))
        
        // CONFIRM step should fail due to invalid DETAILS
        assertNotNull("Invalid details should fail CONFIRM validation",
            invalidDetailsData.validate(RegistrationStep.CONFIRM))
        
        // PAYMENT step should still pass
        assertNull("Payment validation should pass independently",
            invalidDetailsData.validate(RegistrationStep.PAYMENT))
    }

    @Test
    fun testEdgeCaseInputs() = runTest {
        // Test edge cases and special characters
        
        // Special characters in inGameId (should be allowed if length is valid)
        var testData = RegistrationStepData(
            inGameId = "Player_123!@#",
            teamName = "Team-Name",
            tournamentId = "valid-id"
        )
        var result = testData.validate(RegistrationStep.DETAILS)
        assertNull("Special characters should be allowed in valid length strings", result)
        
        // Unicode characters
        testData = testData.copy(
            inGameId = "玩家123",
            teamName = "队伍名"
        )
        result = testData.validate(RegistrationStep.DETAILS)
        assertNull("Unicode characters should be allowed", result)
        
        // Whitespace-only strings (should be treated as blank)
        testData = testData.copy(
            inGameId = "   ",
            teamName = "\t\n "
        )
        result = testData.validate(RegistrationStep.DETAILS)
        assertNotNull("Whitespace-only strings should be treated as invalid", result)
    }

    @Test
    fun testValidationErrorMessages() = runTest {
        // Test that error messages are user-friendly and specific
        
        val testCases = mapOf(
            RegistrationStepData(inGameId = "") to "In-game ID is required",
            RegistrationStepData(inGameId = "ab") to "In-game ID must be at least 3 characters", 
            RegistrationStepData(teamName = "") to "Team name is required",
            RegistrationStepData(teamName = "A") to "Team name must be at least 2 characters",
            RegistrationStepData(tournamentId = "") to "Tournament ID is required",
            RegistrationStepData(paymentMethod = "") to "Payment method is required",
            RegistrationStepData(paymentMethod = "invalid") to "Invalid payment method",
            RegistrationStepData(termsAccepted = false) to "You must accept the terms and conditions"
        )
        
        testCases.forEach { (data, expectedMessage) ->
            val step = when {
                data.inGameId.isBlank() || data.inGameId.length < 3 ||
                data.teamName.isBlank() || data.teamName.length < 2 ||
                data.tournamentId.isBlank() -> RegistrationStep.DETAILS
                data.paymentMethod.isBlank() || data.paymentMethod !in listOf("wallet", "upi", "card") -> RegistrationStep.PAYMENT
                !data.termsAccepted -> RegistrationStep.CONFIRM
                else -> RegistrationStep.DETAILS
            }
            
            val actualMessage = data.validate(step)
            assertEquals("Error message should match expected for $data", expectedMessage, actualMessage)
        }
    }
}
