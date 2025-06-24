package com.cehpoint.netwin.data.model

import com.google.firebase.Timestamp

data class TournamentRegistration(
    val tournamentId: String = "",
    val userId: String = "",
    val displayName: String = "",
    val teamName: String = "",
    val inGameId: String = "",
    val status: String = "registered",
    val registeredAt: Timestamp = Timestamp.now()
) 