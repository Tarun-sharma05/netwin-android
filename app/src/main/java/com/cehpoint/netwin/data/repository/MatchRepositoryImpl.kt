package com.cehpoint.netwin.data.repository

import com.cehpoint.netwin.data.model.Match
import com.cehpoint.netwin.data.model.MatchResult
import com.cehpoint.netwin.data.model.MatchStatus
import com.cehpoint.netwin.data.remote.FirebaseManager
import com.cehpoint.netwin.domain.repository.MatchRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MatchRepositoryImpl @Inject constructor(
    private val firebaseManager: FirebaseManager
) : MatchRepository {

    private val matchesCollection = firebaseManager.firestore.collection("matches")

    override fun getMatchesByTournament(tournamentId: String): Flow<List<Match>> = callbackFlow {
        val subscription = matchesCollection
            .whereEqualTo("tournamentId", tournamentId)
            .orderBy("matchNumber")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val matches = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Match::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                trySend(matches)
            }
        
        awaitClose { subscription.remove() }
    }

    override fun getMatchById(id: String): Flow<Match?> = callbackFlow {
        val subscription = matchesCollection.document(id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val match = snapshot?.toObject(Match::class.java)?.copy(id = snapshot.id)
                trySend(match)
            }
        
        awaitClose { subscription.remove() }
    }

    override suspend fun createMatch(match: Match): Result<String> = try {
        val docRef = matchesCollection.add(match).await()
        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateMatch(match: Match): Result<Unit> = try {
        matchesCollection.document(match.id)
            .set(match)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteMatch(id: String): Result<Unit> = try {
        matchesCollection.document(id)
            .delete()
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateMatchStatus(id: String, status: MatchStatus): Result<Unit> = try {
        matchesCollection.document(id)
            .update("status", status)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun addMatchResult(matchId: String, result: MatchResult): Result<Unit> = try {
        val matchRef = matchesCollection.document(matchId)
        
        firebaseManager.firestore.runTransaction { transaction ->
            val match = transaction.get(matchRef).toObject(Match::class.java)
                ?: throw Exception("Match not found")

            val updatedResults = match.results.toMutableList()
            updatedResults.add(result)
            
            transaction.update(matchRef, "results", updatedResults)
        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateMatchResult(matchId: String, result: MatchResult): Result<Unit> = try {
        val matchRef = matchesCollection.document(matchId)
        
        firebaseManager.firestore.runTransaction { transaction ->
            val match = transaction.get(matchRef).toObject(Match::class.java)
                ?: throw Exception("Match not found")

            val updatedResults = match.results.toMutableList()
            val index = updatedResults.indexOfFirst { it.userId == result.userId }
            
            if (index != -1) {
                updatedResults[index] = result
                transaction.update(matchRef, "results", updatedResults)
            } else {
                throw Exception("Result not found for user")
            }
        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun removeMatchResult(matchId: String, userId: String): Result<Unit> = try {
        val matchRef = matchesCollection.document(matchId)
        
        firebaseManager.firestore.runTransaction { transaction ->
            val match = transaction.get(matchRef).toObject(Match::class.java)
                ?: throw Exception("Match not found")

            val updatedResults = match.results.filter { it.userId != userId }
            transaction.update(matchRef, "results", updatedResults)
        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
} 