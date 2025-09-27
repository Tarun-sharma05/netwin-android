package com.cehpoint.netwin.domain.model

data class Tournament(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val gameType: String = "",
    val matchType: String = "",
    val map: String = "",
    val startTime: Long = 0,
    val entryFee: Double = 0.0,
    val prizePool: Double = 0.0,
    val maxTeams: Int = 0,
    val registeredTeams: Int = 0,
    val status: String = "upcoming",
    val rules: List<String> = emptyList(),
    val bannerImage: String? = null,
    val rewardsDistribution: List<RewardDistribution> = emptyList(),
    val createdAt: Long = 0,
    val killReward: Double? = null,
    val roomId: String? = null,
    val roomPassword: String? = null,
    val actualStartTime: Long? = null,
    val completedAt: Long? = null,
    val country: String? = null,
    val registrationStartTime: Long? = null,
    val registrationEndTime: Long? = null
) {
    val mode: TournamentMode
        get() = when (matchType.uppercase()) {
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
            TournamentMode.CUSTOM -> maxTeams // Or some other logic for custom
        }
    val computedStatus: TournamentStatus
        get() {
            val now = System.currentTimeMillis()
            val debugInfo = """
                Tournament: $name
                Current time: $now (${java.util.Date(now)})
                Start time: $startTime (${java.util.Date(startTime)})
                Completed at: $completedAt (${completedAt?.let { java.util.Date(it) }})
                Room ID: $roomId
                Time diff: ${startTime - now}
            """.trimIndent()
            android.util.Log.d("TournamentStatus", debugInfo)

            return when {
                now < startTime - 10 * 60 * 1000 -> {
                    android.util.Log.d("TournamentStatus", "$name -> UPCOMING")
                    TournamentStatus.UPCOMING
                }

                now in (startTime - 10 * 60 * 1000) until startTime -> {
                    android.util.Log.d("TournamentStatus", "$name -> STARTS_SOON")
                    TournamentStatus.STARTS_SOON
                }

                roomId != null && now >= startTime && completedAt == null -> {
                    android.util.Log.d("TournamentStatus", "$name -> ROOM_OPEN")
                    TournamentStatus.ROOM_OPEN
                }

                now in startTime until (completedAt ?: (startTime + 24 * 60 * 60 * 1000)) -> {
                    android.util.Log.d("TournamentStatus", "$name -> ONGOING")
                    TournamentStatus.ONGOING
                }

                else -> {
                    android.util.Log.d("TournamentStatus", "$name -> COMPLETED")
                    TournamentStatus.COMPLETED
                }
            }
        }
}

data class RewardDistribution(
    val position: Int,
    val percentage: Double
)


enum class TournamentStatus {
    UPCOMING,
    STARTS_SOON,
    ROOM_OPEN,
    ONGOING,
    COMPLETED
}

enum class TournamentMode {
    SOLO, DUO, SQUAD, TRIO, CUSTOM
}