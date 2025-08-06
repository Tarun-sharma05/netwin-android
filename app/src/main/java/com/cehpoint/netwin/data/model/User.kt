package com.cehpoint.netwin.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val id: String = "",
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val displayName: String? = null,
    val photoURL: String? = null,
    val phoneNumber: String? = null,
    val gameId: String? = null,
    val gameMode: String? = null,
    val country: String = "",
    val countryCode: String? = null,
    val currency: String = "INR",
    val walletBalance: Double = 0.0,
    val kycStatus: String = "not_submitted",
    val role: String = "user",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    // Optional/admin/Firestore-only fields
    val lastLogin: Timestamp? = null,
    val isVerified: Boolean = false,
    val kycDocuments: KycDocuments? = null,
    val status: String = "active",
    val location: String? = null
) {
    companion object {
        fun fromFirestore(
            id: String,
            uid: String,
            email: String,
            username: String,
            displayName: String? = null,
            photoURL: String? = null,
            phoneNumber: String? = null,
            gameId: String? = null,
            gameMode: String? = null,
            country: String = "",
            countryCode: String? = null,
            currency: String = "INR",
            walletBalance: Double = 0.0,
            kycStatus: String = "not_submitted",
            role: String = "user",
            createdAt: Timestamp,
            updatedAt: Timestamp? = null,
            lastLogin: Timestamp? = null,
            isVerified: Boolean = false,
            kycDocuments: Map<String, String>? = null,
            status: String = "active",
            location: String? = null
        ): User {
            return User(
                id = id,
                uid = uid,
                email = email,
                username = username,
                displayName = displayName,
                photoURL = photoURL,
                phoneNumber = phoneNumber,
                gameId = gameId,
                gameMode = gameMode,
                country = country,
                countryCode = countryCode,
                currency = currency,
                walletBalance = walletBalance,
                kycStatus = kycStatus,
                role = role,
                createdAt = createdAt,
                updatedAt = updatedAt,
                lastLogin = lastLogin,
                isVerified = isVerified,
                kycDocuments = kycDocuments?.let { KycDocuments.fromMap(it) },
                status = status,
                location = location
            )
        }
    }

    fun toFirestore(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "email" to email,
            "username" to username,
            "displayName" to displayName,
            "photoURL" to photoURL,
            "phoneNumber" to phoneNumber,
            "gameId" to gameId,
            "gameMode" to gameMode,
            "country" to country,
            "countryCode" to countryCode,
            "currency" to currency,
            "walletBalance" to walletBalance,
            "kycStatus" to kycStatus,
            "role" to role,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "lastLogin" to lastLogin,
            "isVerified" to isVerified,
            "kycDocuments" to kycDocuments?.toMap(),
            "status" to status,
            "location" to location
        ).filterValues { it != null } as Map<String, Any>
    }
}

data class KycDocuments(
    val idProof: String? = null,
    val addressProof: String? = null,
    val selfie: String? = null
) {
    fun toMap(): Map<String, String?> {
        return mapOf(
            "idProof" to idProof,
            "addressProof" to addressProof,
            "selfie" to selfie
        )
    }

    companion object {
        fun fromMap(map: Map<String, String>): KycDocuments {
            return KycDocuments(
                idProof = map["idProof"],
                addressProof = map["addressProof"],
                selfie = map["selfie"]
            )
        }
    }
} 