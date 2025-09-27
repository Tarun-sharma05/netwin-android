package com.cehpoint.netwin.domain.model

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class RegistrationStepData(
    val playerIds: List<String> = listOf(""),
    val teamName: String = "",
    val paymentMethod: String = "wallet",
    val termsAccepted: Boolean = false,
    val tournamentId: String = ""
) : Parcelable {

    /**
     * Validates the registration data for a specific step
     * @param step The registration step to validate
     * @return Error message string if validation fails, null if valid
     */
    fun validate(step: RegistrationStep): String? {
        android.util.Log.d("RegistrationStepData", "=== validate() DOMAIN LAYER ENTRY ===")
        android.util.Log.d("RegistrationStepData", "VALIDATION STEP: $step")
        android.util.Log.d("RegistrationStepData", "CURRENT DATA: playerIds='$playerIds', teamName='$teamName', paymentMethod='$paymentMethod', termsAccepted=$termsAccepted, tournamentId='$tournamentId'")

        // Only validate fields relevant to the current step
        val result = when (step) {
            RegistrationStep.DETAILS -> {
                android.util.Log.d("RegistrationStepData", "Validating DETAILS step...")
                when {
                    playerIds.any { it.isBlank() } -> {
                        android.util.Log.w("RegistrationStepData", "DETAILS validation FAILED: one or more playerIds are blank")
                        "All player in-game IDs are required"
                    }
                    playerIds.any { it.length < 3 } -> {
                        android.util.Log.w("RegistrationStepData", "DETAILS validation FAILED: one or more playerIds are too short")
                        "All in-game IDs must be at least 3 characters"
                    }
                    // Team name validation - only required for non-SOLO tournaments
                    // For SOLO tournaments, team name can be empty or auto-filled
                    teamName.isBlank() && !isSoloTournament() -> {
                        android.util.Log.w("RegistrationStepData", "DETAILS validation FAILED: teamName is blank for non-SOLO tournament")
                        "Team name is required for team tournaments"
                    }
                    teamName.isNotBlank() && teamName.length < 2 -> {
                        android.util.Log.w("RegistrationStepData", "DETAILS validation FAILED: teamName length ${teamName.length} < 2")
                        "Team name must be at least 2 characters"
                    }
                    tournamentId.isBlank() -> {
                        android.util.Log.w("RegistrationStepData", "DETAILS validation FAILED: tournamentId is blank")
                        "Tournament ID is required"
                    }
                    else -> {
                        android.util.Log.d("RegistrationStepData", "DETAILS validation PASSED")
                        null
                    }
                }
            }
            RegistrationStep.PAYMENT -> {
                android.util.Log.d("RegistrationStepData", "Validating PAYMENT step...")
                when {
                    paymentMethod.isBlank() -> {
                        android.util.Log.w("RegistrationStepData", "PAYMENT validation FAILED: paymentMethod is blank")
                        "Payment method is required"
                    }
                    paymentMethod !in listOf("wallet", "upi", "card") -> {
                        android.util.Log.w("RegistrationStepData", "PAYMENT validation FAILED: paymentMethod '$paymentMethod' not in allowed list")
                        "Invalid payment method"
                    }
                    else -> {
                        android.util.Log.d("RegistrationStepData", "PAYMENT validation PASSED")
                        null
                    }
                }
            }
            RegistrationStep.REVIEW -> {
                android.util.Log.d("RegistrationStepData", "Validating REVIEW step - checking prerequisites only...")
                // REVIEW step (Step 1) should only validate tournament selection and prerequisites
                // DO NOT validate user input fields like inGameId, teamName - those are for DETAILS step
                android.util.Log.d("RegistrationStepData", "REVIEW: Tournament and prerequisites validated")
                when {
                    tournamentId.isBlank() -> {
                        android.util.Log.w("RegistrationStepData", "REVIEW validation FAILED: tournamentId is blank")
                        "Tournament selection is required"
                    }
                    // TODO: Add KYC verification check when available
                    // TODO: Add wallet balance sufficiency check when available
                    else -> {
                        android.util.Log.d("RegistrationStepData", "REVIEW validation PASSED - No user input validation required at this step")
                        null
                    }
                }
            }
            RegistrationStep.CONFIRM -> {
                android.util.Log.d("RegistrationStepData", "Validating CONFIRM step...")
                when {
                    !termsAccepted -> {
                        android.util.Log.w("RegistrationStepData", "CONFIRM validation FAILED: terms not accepted")
                        "You must accept the terms and conditions"
                    }
                    else -> {
                        android.util.Log.d("RegistrationStepData", "CONFIRM validation PASSED - step-specific validation only")
                        // Only validate CONFIRM step requirements (terms acceptance)
                        // The ViewModel should ensure all previous steps are valid before reaching CONFIRM
                        null
                    }
                }
            }
        }
        
        android.util.Log.d("RegistrationStepData", "VALIDATION RESULT: ${if (result == null) "VALID" else "INVALID - '$result'"}")
        android.util.Log.d("RegistrationStepData", "=== validate() DOMAIN LAYER EXIT ===")
        return result
    }
    
    /**
     * Checks if all required fields for a specific step are filled
     * @param step The registration step to check
     * @return true if all required fields are filled, false otherwise
     */
    fun isStepComplete(step: RegistrationStep): Boolean {
        return validate(step) == null
    }
    
    /**
     * Validates all steps comprehensively (used for final submission)
     * @return Error message string if any step validation fails, null if all valid
     */
    fun validateAll(): String? {
        android.util.Log.d("RegistrationStepData", "=== validateAll() ENTRY - Comprehensive validation ===")
        
        // Validate all steps in order - return first error found
        val reviewResult = validate(RegistrationStep.REVIEW)
        if (reviewResult != null) {
            android.util.Log.w("RegistrationStepData", "validateAll() FAILED at REVIEW: '$reviewResult'")
            return reviewResult
        }
        
        val paymentResult = validate(RegistrationStep.PAYMENT)
        if (paymentResult != null) {
            android.util.Log.w("RegistrationStepData", "validateAll() FAILED at PAYMENT: '$paymentResult'")
            return paymentResult
        }
        
        val detailsResult = validate(RegistrationStep.DETAILS)
        if (detailsResult != null) {
            android.util.Log.w("RegistrationStepData", "validateAll() FAILED at DETAILS: '$detailsResult'")
            return detailsResult
        }
        
        val confirmResult = validate(RegistrationStep.CONFIRM)
        if (confirmResult != null) {
            android.util.Log.w("RegistrationStepData", "validateAll() FAILED at CONFIRM: '$confirmResult'")
            return confirmResult
        }
        
        android.util.Log.d("RegistrationStepData", "validateAll() PASSED - all steps valid")
        android.util.Log.d("RegistrationStepData", "=== validateAll() EXIT ===")
        return null
    }
    
    /**
     * Checks if the entire registration flow is complete
     * @return true if all steps are valid, false otherwise
     */
    fun isComplete(): Boolean {
        return validate(RegistrationStep.CONFIRM) == null
    }
    
    /**
     * Helper method to determine if this is a SOLO tournament
     * For now, we'll use a simple heuristic - in the future this should be based on tournament data
     * @return true if tournament is SOLO mode, false otherwise
     */
    private fun isSoloTournament(): Boolean {
        // TODO: This should be determined from actual tournament data passed to this class
        // For now, we'll make team name optional by default to handle SOLO tournaments
        // In the future, add a tournamentMode field to RegistrationStepData
        return true // Temporarily treat all tournaments as allowing optional team names
    }
    
    // Parcelable implementation
    override fun describeContents(): Int = 0
    
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeStringList(playerIds)
        parcel.writeString(teamName)
        parcel.writeString(paymentMethod)
        parcel.writeByte(if (termsAccepted) 1 else 0)
        parcel.writeString(tournamentId)
    }

    companion object CREATOR : Parcelable.Creator<RegistrationStepData> {
        override fun createFromParcel(parcel: Parcel): RegistrationStepData {
            val playerIds = mutableListOf<String>()
            parcel.readStringList(playerIds)
            return RegistrationStepData(
                playerIds = playerIds,
                teamName = parcel.readString() ?: "",
                paymentMethod = parcel.readString() ?: "wallet",
                termsAccepted = parcel.readByte() != 0.toByte(),
                tournamentId = parcel.readString() ?: ""
            )
        }
        
        override fun newArray(size: Int): Array<RegistrationStepData?> {
            return arrayOfNulls(size)
        }
    }
}
