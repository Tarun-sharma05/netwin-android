package com.cehpoint.netwin.domain.repository

import com.cehpoint.netwin.data.model.Match
import com.cehpoint.netwin.data.model.MatchResult
import com.cehpoint.netwin.data.model.MatchStatus
import kotlinx.coroutines.flow.Flow

interface MatchRepository {
    fun getMatchesByTournament(tournamentId: String): Flow<List<Match>>
    fun getMatchById(id: String): Flow<Match?>
    suspend fun createMatch(match: Match): Result<String>
    suspend fun updateMatch(match: Match): Result<Unit>
    suspend fun deleteMatch(id: String): Result<Unit>
    suspend fun updateMatchStatus(id: String, status: MatchStatus): Result<Unit>
    suspend fun addMatchResult(matchId: String, result: MatchResult): Result<Unit>
    suspend fun updateMatchResult(matchId: String, result: MatchResult): Result<Unit>
    suspend fun removeMatchResult(matchId: String, userId: String): Result<Unit>
} 