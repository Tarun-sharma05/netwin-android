package com.cehpoint.netwin.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class TournamentRegistration(
    @DocumentId
    val id: String = "",
    val tournamentId: String = "",
    val userId: String = "",
    val teamName: String = "",
    val teamMembers: List<TeamMember> = emptyList(),
    val paymentStatus: String = "pending",
    val registeredAt: Timestamp = Timestamp.now()
) {
    companion object {
        fun fromFirestore(
            id: String,
            tournamentId: String,
            userId: String,
            teamName: String,
            teamMembers: List<Map<String, String>>? = null,
            paymentStatus: String = "pending",
            registeredAt: Timestamp
        ): TournamentRegistration {
            return TournamentRegistration(
                id = id,
                tournamentId = tournamentId,
                userId = userId,
                teamName = teamName,
                teamMembers = teamMembers?.map { TeamMember.fromMap(it) } ?: emptyList(),
                paymentStatus = paymentStatus,
                registeredAt = registeredAt
            )
        }
    }

    fun toFirestore(): Map<String, Any> {
        return mapOf(
            "tournamentId" to tournamentId,
            "userId" to userId,
            "teamName" to teamName,
            "teamMembers" to teamMembers.map { it.toMap() },
            "paymentStatus" to paymentStatus,
            "registeredAt" to registeredAt
        )
    }
}

data class TeamMember(
    val username: String,
    val inGameId: String
) {
    fun toMap(): Map<String, String> {
        return mapOf(
            "username" to username,
            "inGameId" to inGameId
        )
    }

    companion object {
        fun fromMap(map: Map<String, String>): TeamMember {
            return TeamMember(
                username = map["username"] ?: "",
                inGameId = map["inGameId"] ?: ""
            )
        }
    }
} 