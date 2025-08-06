package com.cehpoint.netwin.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class User(
    @DocumentId
    val id: String = "", // Firestore document ID (maps to uid)
    val displayName: String = "",
    val username: String = "",
    val email: String = "",
    val country: String = "",
    val currency: String = "",
    val walletBalance: Double = 0.0,
    val profilePictureUrl: String = "", // maps to photoURL
    val role: String = "user",
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val lastLoginAt: Long? = null,
    val loginCount: Int = 0,
    val phoneNumber: String = "",
    val gameId: String = "",
    val gameMode: String = "",
    val matchesPlayed: Int = 0,
    val matchesWon: Int = 0,
    val totalKills: Int = 0,
    val totalEarnings: Double = 0.0,
    val tournamentsJoined: Int = 0,
    val kycStatus: String = "pending",
    val kycVerifiedAt: Long? = null,
    val kycDocumentType: String = "",
    val kycDocumentNumber: String = "",
    val kycRejectedReason: String = "",
    val isBanned: Boolean = false,
    val banReason: String = "",
    val adminNotes: String = "",
    val notificationsEnabled: Boolean = true,
    val preferredLanguage: String = "en",
    val deviceType: String = "Android",
    val appVersion: String = ""
)