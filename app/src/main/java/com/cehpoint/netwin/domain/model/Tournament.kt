package com.cehpoint.netwin.domain.model

data class Tournament(
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
    @Deprecated("Use computedStatus instead")
    val status: List<TournamentStatus> = emptyList(),
    val isFeatured: Boolean = false,
    val gameId: String = "",
    val createdBy: String = "",
    val createdAt: Long = 0,
    val imageUrl: String = "",
    val roomCode: String? = null,
    val roomPassword: String? = null,
    val roomInstructions: String? = null,
    val mode: TournamentMode = TournamentMode.SQUAD,
    val map: String = ""
) {
    val computedStatus: TournamentStatus
        get() {
            val now = System.currentTimeMillis()
            val debugInfo = """
                Tournament: $name
                Current time: $now (${java.util.Date(now)})
                Start date: $startDate (${java.util.Date(startDate)})
                End date: $endDate (${java.util.Date(endDate)})
                Room code: $roomCode
                Time diff: ${startDate - now}
            """.trimIndent()
            android.util.Log.d("TournamentStatus", debugInfo)
            
            return when {
                now < startDate - 10 * 60 * 1000 -> {
                    android.util.Log.d("TournamentStatus", "$name -> UPCOMING")
                    TournamentStatus.UPCOMING
                }
                now in (startDate - 10 * 60 * 1000) until startDate -> {
                    android.util.Log.d("TournamentStatus", "$name -> STARTS_SOON")
                    TournamentStatus.STARTS_SOON
                }
                roomCode != null && now >= startDate && now < endDate -> {
                    android.util.Log.d("TournamentStatus", "$name -> ROOM_OPEN")
                    TournamentStatus.ROOM_OPEN
                }
                now in startDate until endDate -> {
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