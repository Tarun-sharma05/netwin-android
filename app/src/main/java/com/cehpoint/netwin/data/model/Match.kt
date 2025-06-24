package com.cehpoint.netwin.data.model

data class Match(
    val id: String = "",
    val tournamentId: String = "",
    val matchNumber: Int = 0,
    val map: String = "",
    val startTime: Long = 0,
    val status: MatchStatus = MatchStatus.UPCOMING,
    val results: List<MatchResult> = emptyList(),
    val roomId: String = "",
    val roomPassword: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val spectatorUrl: String? = null,
    val matchType: String? = null
)

data class MatchResult(
    val userId: String = "",
    val userName: String = "",
    val pubgId: String = "",
    val position: Int = 0,
    val kills: Int = 0,
    val damage: Int = 0,
    val points: Int = 0,
    val prize: Double = 0.0
)

enum class MatchStatus {
    UPCOMING,
    LIVE,
    COMPLETED,
    CANCELLED
} 