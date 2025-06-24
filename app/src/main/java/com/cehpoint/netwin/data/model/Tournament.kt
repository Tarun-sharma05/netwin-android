package com.cehpoint.netwin.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.cehpoint.netwin.domain.model.Tournament as DomainTournament
import com.cehpoint.netwin.domain.model.TournamentStatus
import com.cehpoint.netwin.domain.model.TournamentMode

data class Tournament(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val startDate: Long = 0,
    val endDate: Long = 0,
    val maxPlayers: Int = 0,
    val currentPlayers: Int = 0,
    val entryFee: Int = 0,
    val perKillPrize: Int = 0,
    val prizePool: Int = 0,
    val status: List<String> = emptyList(),
    val isFeatured: Boolean = false,
    val gameId: String = "",
    val createdBy: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long? = null,
    val rulesUrl: String? = null,
    val platform: String? = null,
    val entryType: String? = null,
    val isActive: Boolean = true,
    val imageUrl: String = "",
    val roomCode: String? = null,
    val roomPassword: String? = null,
    val roomInstructions: String? = null,
    val mode: TournamentMode = TournamentMode.SQUAD,
    val map: String = ""
) {
    companion object {
        fun fromFirestore(
            id: String,
            name: String,
            description: String,
            startDate: Timestamp,
            endDate: Timestamp,
            maxPlayers: Int,
            currentPlayers: Int,
            entryFee: Int,
            perKillPrize: Int,
            prizePool: Int,
            status: List<String>,
            isFeatured: Boolean,
            gameId: String,
            createdBy: String,
            createdAt: Timestamp,
            imageUrl: String,
            roomCode: String? = null,
            roomPassword: String? = null,
            roomInstructions: String? = null,
            updatedAt: Timestamp? = null,
            rulesUrl: String? = null,
            platform: String? = null,
            entryType: String? = null,
            isActive: Boolean = true,
            mode: String? = null,
            map: String? = null
        ): Tournament {
            return Tournament(
                id = id,
                name = name,
                description = description,
                startDate = startDate.toDate().time,
                endDate = endDate.toDate().time,
                maxPlayers = maxPlayers,
                currentPlayers = currentPlayers,
                entryFee = entryFee,
                perKillPrize = perKillPrize,
                prizePool = prizePool,
                status = status,
                isFeatured = isFeatured,
                gameId = gameId,
                createdBy = createdBy,
                createdAt = createdAt.toDate().time,
                imageUrl = imageUrl,
                roomCode = roomCode,
                roomPassword = roomPassword,
                roomInstructions = roomInstructions,
                updatedAt = updatedAt?.toDate()?.time,
                rulesUrl = rulesUrl,
                platform = platform,
                entryType = entryType,
                isActive = isActive,
                mode = mode?.let { TournamentMode.valueOf(it) } ?: TournamentMode.SQUAD,
                map = map ?: ""
            )
        }
    }

    fun toDomain(): DomainTournament {
        return DomainTournament(
            id = id,
            name = name,
            description = description,
            startDate = startDate,
            endDate = endDate,
            maxPlayers = maxPlayers,
            currentPlayers = currentPlayers,
            entryFee = entryFee,
            perKillPrize = perKillPrize,
            prizePool = prizePool,
            status = status.map { TournamentStatus.valueOf(it) },
            isFeatured = isFeatured,
            gameId = gameId,
            createdBy = createdBy,
            createdAt = createdAt,
            imageUrl = imageUrl,
            roomCode = roomCode,
            roomPassword = roomPassword,
            roomInstructions = roomInstructions,
            mode = mode,
            map = map
        )
    }

    fun toFirestore(): Map<String, Any> {
        return mapOf(
            "name" to name,
            "description" to description,
            "startDate" to Timestamp(startDate, 0),
            "endDate" to Timestamp(endDate, 0),
            "maxPlayers" to maxPlayers,
            "currentPlayers" to currentPlayers,
            "entryFee" to entryFee,
            "perKillPrize" to perKillPrize,
            "prizePool" to prizePool,
            "status" to status,
            "isFeatured" to isFeatured,
            "gameId" to gameId,
            "createdBy" to createdBy,
            "createdAt" to Timestamp(createdAt, 0),
            "imageUrl" to imageUrl,
            "roomCode" to roomCode,
            "roomPassword" to roomPassword,
            "roomInstructions" to roomInstructions,
            "mode" to mode.name,
            "map" to map
        ).filterValues { it != null } as Map<String, Any>
    }
}

fun DomainTournament.toData(): Tournament {
    return Tournament(
        id = id,
        name = name,
        description = description,
        startDate = startDate,
        endDate = endDate,
        maxPlayers = maxPlayers,
        currentPlayers = currentPlayers,
        entryFee = entryFee,
        perKillPrize = perKillPrize,
        prizePool = prizePool,
        status = status.map { it.name },
        isFeatured = isFeatured,
        gameId = gameId,
        createdBy = createdBy,
        createdAt = createdAt,
        imageUrl = imageUrl,
        roomCode = roomCode,
        roomPassword = roomPassword,
        roomInstructions = roomInstructions,
        mode = mode,
        map = map
    )
}
