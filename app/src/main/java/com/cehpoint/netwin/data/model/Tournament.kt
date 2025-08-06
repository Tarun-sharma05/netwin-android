package com.cehpoint.netwin.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.cehpoint.netwin.domain.model.Tournament as DomainTournament
import com.cehpoint.netwin.domain.model.TournamentStatus
import com.cehpoint.netwin.domain.model.TournamentMode

data class Tournament(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val description: String? = null,
    val gameMode: String = "",
    val entryFee: Double = 0.0,
    val prizePool: Double = 0.0,
//    val maxParticipants: Int = 0,
//    val currentParticipants: Int = 0,
    val maxTeams: Int = 0,
    val registeredTeams: Int = 0,
    val status: String = "upcoming",
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    val rules: String? = null,
    val image: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    // Extra fields for app logic (optional)
    val matchType: String? = null,
    val map: String? = null,
    val rewardsDistribution: List<RewardDistribution> = emptyList(),
    val killReward: Double? = null,
    val roomId: String? = null,
    val roomPassword: String? = null,
    val actualStartTime: Timestamp? = null
) {
    companion object {
        fun fromFirestore(
            id: String,
            title: String,
            description: String? = null,
            gameMode: String,
            entryFee: Double,
            prizePool: Double,
//            maxParticipants: Int,
//            currentParticipants: Int,
            maxTeams: Int,
            registeredTeams: Int,
            status: String,
            startDate: Timestamp? = null,
            endDate: Timestamp? = null,
            rules: String? = null,
            image: String? = null,
            createdAt: Timestamp? = null,
            updatedAt: Timestamp? = null,
            // Extra fields for app logic
            matchType: String? = null,
            map: String? = null,
            rewardsDistribution: List<Map<String, Any>>? = null,
            killReward: Double? = null,
            roomId: String? = null,
            roomPassword: String? = null,
            actualStartTime: Timestamp? = null
        ): Tournament {
            return Tournament(
                id = id,
                title = title,
                description = description,
                gameMode = gameMode,
                entryFee = entryFee,
                prizePool = prizePool,
                maxTeams = maxTeams,
                registeredTeams = registeredTeams,
                status = status,
                startDate = startDate,
                endDate = endDate,
                rules = rules,
                image = image,
                createdAt = createdAt,
                updatedAt = updatedAt,
                matchType = matchType,
                map = map,
                rewardsDistribution = rewardsDistribution?.map { RewardDistribution.fromMap(it) } ?: emptyList(),
                killReward = killReward,
                roomId = roomId,
                roomPassword = roomPassword,
                actualStartTime = actualStartTime
            )
        }
    }

    fun toDomain(): DomainTournament {
        return DomainTournament(
            id = id,
            name = title,
            description = description ?: "",
            gameType = gameMode,
            matchType = matchType ?: "",
            map = map ?: "",
            startTime = startDate?.toDate()?.time ?: 0,
            entryFee = entryFee,
            prizePool = prizePool,
            maxTeams = maxTeams,
            registeredTeams = registeredTeams,
            status = status,
            rules = rules,
            bannerImage = image,
            rewardsDistribution = rewardsDistribution.map {
                com.cehpoint.netwin.domain.model.RewardDistribution(
                    position = it.position,
                    percentage = it.percentage
                )
            },
            createdAt = createdAt?.toDate()?.time ?: 0,
            killReward = killReward,
            roomId = roomId,
            roomPassword = roomPassword,
            actualStartTime = actualStartTime?.toDate()?.time,
            completedAt = endDate?.toDate()?.time
        )
    }

    fun toFirestore(): Map<String, Any> {
        return mapOf(
            "title" to title,
            "description" to description,
            "gameMode" to gameMode,
            "entryFee" to entryFee,
            "prizePool" to prizePool,
            "maxTeams" to maxTeams,
            "registeredTeams" to registeredTeams,
            "status" to status,
            "startDate" to startDate,
            "endDate" to endDate,
            "rules" to rules,
            "image" to image,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "matchType" to matchType,
            "map" to map,
            "rewardsDistribution" to rewardsDistribution.map { it.toMap() },
            "killReward" to killReward,
            "roomId" to roomId,
            "roomPassword" to roomPassword,
            "actualStartTime" to actualStartTime
        ).filterValues { it != null } as Map<String, Any>
    }
}

data class RewardDistribution(
    val position: Int,
    val percentage: Double
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "position" to position,
            "percentage" to percentage
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): RewardDistribution {
            return RewardDistribution(
                position = (map["position"] as? Number)?.toInt() ?: 0,
                percentage = (map["percentage"] as? Number)?.toDouble() ?: 0.0
            )
        }
    }
}

fun DomainTournament.toData(): Tournament {
    return Tournament(
        id = id,
        title = name,
        description = description,
        gameMode = gameType,
        entryFee = entryFee,
        prizePool = prizePool,
        maxTeams = maxTeams,
        registeredTeams = registeredTeams,
        status = status,
        startDate = if (startTime != 0L) Timestamp(startTime, 0) else null,
        endDate = if (completedAt != null) Timestamp(completedAt, 0) else null,
        rules = rules,
        image = bannerImage,
        createdAt = if (createdAt != 0L) Timestamp(createdAt, 0) else null,
        updatedAt = null,
        matchType = matchType,
        map = map,
        rewardsDistribution = rewardsDistribution.map {
            RewardDistribution(
                position = it.position,
                percentage = it.percentage
            )
        },
        killReward = killReward,
        roomId = roomId,
        roomPassword = roomPassword,
        actualStartTime = if (actualStartTime != null) Timestamp(actualStartTime, 0) else null
    )
}
