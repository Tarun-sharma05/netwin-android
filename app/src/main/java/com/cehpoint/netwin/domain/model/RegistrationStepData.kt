package com.cehpoint.netwin.domain.model

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class RegistrationStepData(
    val inGameId: String = "",
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
        return when (step) {
            RegistrationStep.DETAILS -> {
                when {
                    inGameId.isBlank() -> "In-game ID is required"
                    inGameId.length < 3 -> "In-game ID must be at least 3 characters"
                    teamName.isBlank() -> "Team name is required"
                    teamName.length < 2 -> "Team name must be at least 2 characters"
                    tournamentId.isBlank() -> "Tournament ID is required"
                    else -> null
                }
            }
            RegistrationStep.PAYMENT -> {
                when {
                    paymentMethod.isBlank() -> "Payment method is required"
                    paymentMethod !in listOf("wallet", "upi", "card") -> "Invalid payment method"
                    else -> null
                }
            }
            RegistrationStep.REVIEW -> {
                // Validate all previous steps
                validate(RegistrationStep.DETAILS) ?: validate(RegistrationStep.PAYMENT)
            }
            RegistrationStep.CONFIRM -> {
                when {
                    !termsAccepted -> "You must accept the terms and conditions"
                    else -> validate(RegistrationStep.REVIEW)
                }
            }
        }
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
     * Checks if the entire registration flow is complete
     * @return true if all steps are valid, false otherwise
     */
    fun isComplete(): Boolean {
        return validate(RegistrationStep.CONFIRM) == null
    }
    
    // Parcelable implementation
    override fun describeContents(): Int = 0
    
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(inGameId)
        parcel.writeString(teamName)
        parcel.writeString(paymentMethod)
        parcel.writeByte(if (termsAccepted) 1 else 0)
        parcel.writeString(tournamentId)
    }
    
    companion object CREATOR : Parcelable.Creator<RegistrationStepData> {
        override fun createFromParcel(parcel: Parcel): RegistrationStepData {
            return RegistrationStepData(
                inGameId = parcel.readString() ?: "",
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
