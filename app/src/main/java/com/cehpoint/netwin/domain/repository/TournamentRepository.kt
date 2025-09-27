package com.cehpoint.netwin.domain.repository

import com.cehpoint.netwin.domain.model.Tournament
import com.cehpoint.netwin.domain.model.TournamentStatus
import kotlinx.coroutines.flow.Flow


interface TournamentRepository {
    suspend fun getFeaturedTournaments(): List<Tournament>
    suspend fun getTournaments(): Flow<List<Tournament>>
    suspend fun getTournamentById(id: String): Tournament?
    suspend fun createTournament(tournament: Tournament): Result<Tournament>
    suspend fun updateTournament(tournament: Tournament): Result<Tournament>
    
    /**
     * Fetches the list of tournament IDs that the user has registered for
     * @param userId The ID of the user
     * @return List of tournament IDs that the user has registered for
     */
    suspend fun getUserTournamentRegistrations(userId: String): List<String>
    suspend fun deleteTournament(id: String): Result<Unit>
    suspend fun joinTournament(tournamentId: String, userId: String): Result<Unit>
    suspend fun leaveTournament(tournamentId: String, userId: String): Result<Unit>
    suspend fun updateTournamentStatus(id: String, status: TournamentStatus): Result<Unit>
    suspend fun registerForTournament(
        tournamentId: String,
        userId: String,
        displayName: String,
        teamName: String,
        playerIds: List<String>
    ): Result<Unit>
    suspend fun isUserRegisteredForTournament(tournamentId: String, userId: String): Boolean
} 