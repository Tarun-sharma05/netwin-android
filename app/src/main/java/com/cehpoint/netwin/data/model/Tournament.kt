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
    // New: platform (BGMI/PUBG/FF etc.)
    val gameType: String? = null,
    val entryFee: Double = 0.0,
    val prizePool: Double = 0.0,
//    val maxParticipants: Int = 0,
//    val currentParticipants: Int = 0,
    val maxTeams: Int = 0,
    val registeredTeams: Int = 0,
    val status: String = "upcoming",
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    val rules: List<String> = emptyList(),
    val image: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    // Extra fields for app logic (optional)
    val matchType: String? = null,
    val map: String? = null,
    // New: country code/name
    val country: String? = null,
    // New: registration window
    val registrationStartTime: Timestamp? = null,
    val registrationEndTime: Timestamp? = null,
    val rewardsDistribution: List<RewardDistribution> = emptyList(),
    val killReward: Double? = null,
    val roomId: String? = null,
    val roomPassword: String? = null,
    val actualStartTime: Timestamp? = null
) {
    val mode: TournamentMode
        get() = when ((matchType ?: gameMode).uppercase()) {
            "SOLO" -> TournamentMode.SOLO
            "DUO" -> TournamentMode.DUO
            "SQUAD" -> TournamentMode.SQUAD
            "TRIO" -> TournamentMode.TRIO
            "CUSTOM" -> TournamentMode.CUSTOM
            else -> TournamentMode.SQUAD
        }

    val teamSize: Int
        get() = when (mode) {
            TournamentMode.SOLO -> 1
            TournamentMode.DUO -> 2
            TournamentMode.TRIO -> 3
            TournamentMode.SQUAD -> 4
            TournamentMode.CUSTOM -> maxTeams
        }

    companion object {
        fun fromFirestore(
            id: String,
            title: String,
            description: String? = null,
            gameMode: String,
            gameType: String? = null,
            entryFee: Double,
            prizePool: Double,
//            maxParticipants: Int,
//            currentParticipants: Int,
            maxTeams: Int,
            registeredTeams: Int,
            status: String,
            startDate: Timestamp? = null,
            endDate: Timestamp? = null,
            rules: List<String>? = null,
            image: String? = null,
            createdAt: Timestamp? = null,
            updatedAt: Timestamp? = null,
            // Extra fields for app logic
            matchType: String? = null,
            map: String? = null,
            country: String? = null,
            registrationStartTime: Timestamp? = null,
            registrationEndTime: Timestamp? = null,
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
                gameType = gameType,
                entryFee = entryFee,
                prizePool = prizePool,
                maxTeams = maxTeams,
                registeredTeams = registeredTeams,
                status = status,
                startDate = startDate,
                endDate = endDate,
                rules = rules ?: emptyList(),
                image = image,
                createdAt = createdAt,
                updatedAt = updatedAt,
                matchType = matchType,
                map = map,
                country = country,
                registrationStartTime = registrationStartTime,
                registrationEndTime = registrationEndTime,
                rewardsDistribution = rewardsDistribution?.map { RewardDistribution.fromMap(it) } ?: emptyList(),
                killReward = killReward,
                roomId = roomId,
                roomPassword = roomPassword,
                actualStartTime = actualStartTime
            )
        }
    }

    fun toDomain(): DomainTournament {
        // Calculate start time; do not fabricate if missing
        val startTimeMs = startDate?.toDate()?.time ?: 0L
        
        // Log the mapping for debugging
        android.util.Log.d("TournamentMapper", "Mapping tournament: $title")
        android.util.Log.d("TournamentMapper", "startDate: $startDate")
        android.util.Log.d("TournamentMapper", "startTimeMs: $startTimeMs (${java.util.Date(startTimeMs)})")
        
        return DomainTournament(
            id = id,
            name = title,
            description = description ?: "",
            // Prefer explicit platform gameType, fallback to gameMode for backward-compat
            gameType = gameType ?: gameMode,
            // Prefer matchType (team mode). If absent, fallback to gameMode so UI reflects updates to gameMode.
            matchType = (matchType?.takeIf { it.isNotBlank() } ?: (gameMode ?: "")),
            map = map ?: "",
            startTime = startTimeMs,
            entryFee = entryFee,
            prizePool = prizePool,
            maxTeams = maxTeams,
            registeredTeams = registeredTeams,
            status = status,
            rules = rules ?: emptyList(),
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
            completedAt = endDate?.toDate()?.time,
            country = country,
            registrationStartTime = registrationStartTime?.toDate()?.time,
            registrationEndTime = registrationEndTime?.toDate()?.time
        )
    }

    fun toFirestore(): Map<String, Any> {
        return mapOf(
            "title" to title,
            "description" to description,
            "gameMode" to gameMode,
            "gameType" to gameType,
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
            "country" to country,
            "registrationStartTime" to registrationStartTime,
            "registrationEndTime" to registrationEndTime,
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
        fun fromMap(map: Map<String, Any?>): RewardDistribution {
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
        // Preserve legacy: gameMode kept as team mode if provided via matchType; else fallback to gameType
        gameMode = if (matchType.isNotBlank()) matchType else gameType,
        gameType = gameType,
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
        country = country,
        registrationStartTime = registrationStartTime?.let { Timestamp(it, 0) },
        registrationEndTime = registrationEndTime?.let { Timestamp(it, 0) },
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
