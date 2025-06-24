package com.cehpoint.netwin.data.repository

import android.util.Log
import com.cehpoint.netwin.data.model.Tournament
import com.cehpoint.netwin.data.model.toData
import com.cehpoint.netwin.data.remote.FirebaseManager
import com.cehpoint.netwin.domain.model.TournamentStatus
import com.cehpoint.netwin.domain.repository.TournamentRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.cehpoint.netwin.domain.model.Tournament as DomainTournament
import com.cehpoint.netwin.data.model.TournamentRegistration

class TournamentRepositoryImpl @Inject constructor(
    private val firebaseManager: FirebaseManager
) : TournamentRepository {

    private val tournamentsCollection = firebaseManager.firestore.collection(FirebaseManager.Companion.Collections.TOURNAMENTS)

    override suspend fun getFeaturedTournaments(): List<DomainTournament> = try {
        Log.d("TournamentRepository", "Fetching featured tournaments from collection: ${FirebaseManager.Companion.Collections.TOURNAMENTS}")
        val snapshot = tournamentsCollection
            .whereEqualTo("isFeatured", true)
            .get()
            .await()
        
        Log.d("TournamentRepository", "Found ${snapshot.documents.size} featured tournaments")
        snapshot.documents.mapNotNull { doc ->
            doc.toObject(Tournament::class.java)?.toDomain()
        }
    } catch (e: Exception) {
        Log.e("TournamentRepository", "Error fetching featured tournaments: ${e.message}", e)
        emptyList()
    }

    override suspend fun getTournaments(): Flow<List<DomainTournament>> = callbackFlow {
        Log.d("TournamentRepository", "Starting to fetch tournaments from collection: ${FirebaseManager.Companion.Collections.TOURNAMENTS}")
        
        val subscription = tournamentsCollection
            .orderBy("startDate")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("TournamentRepository", "Error fetching tournaments: ${error.message}")
                    Log.e("TournamentRepository", "Error code: ${error.code}")
                    Log.e("TournamentRepository", "Error details: ${error.cause}")
                    close(error)
                    return@addSnapshotListener
                }

                Log.d("TournamentRepository", "Received snapshot with ${snapshot?.documents?.size ?: 0} tournaments")
                
                // Log raw document data
                snapshot?.documents?.forEach { doc ->
                    Log.d("TournamentRepository", "Raw document data for ${doc.id}: ${doc.data}")
                }
                
                val tournaments = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                        val tournament = Tournament.fromFirestore(
                            id = doc.id,
                            name = data["name"] as? String ?: "",
                            description = data["description"] as? String ?: "",
                            startDate = data["startDate"] as? Timestamp ?: Timestamp.now(),
                            endDate = data["endDate"] as? Timestamp ?: Timestamp.now(),
                            maxPlayers = (data["maxPlayers"] as? Number)?.toInt() ?: 0,
                            currentPlayers = (data["currentPlayers"] as? Number)?.toInt() ?: 0,
                            entryFee = (data["entryFee"] as? Number)?.toInt() ?: 0,
                            perKillPrize = (data["perKillPrize"] as? Number)?.toInt() ?: 0,
                            prizePool = (data["prizePool"] as? Number)?.toInt() ?: 0,
                            status = (data["status"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                            isFeatured = data["isFeatured"] as? Boolean ?: false,
                            gameId = data["gameId"] as? String ?: "",
                            createdBy = data["createdBy"] as? String ?: "",
                            createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
                            imageUrl = data["imageUrl"] as? String ?: "",
                            updatedAt = data["updatedAt"] as? Timestamp ?: Timestamp.now(),
                            rulesUrl = data["rulesUrl"] as? String ?: "",
                            platform = data["platform"] as? String ?: "",
                            entryType = data["entryType"] as? String ?: "",
                            isActive = data["isActive"] as? Boolean ?: true,
                            mode = data["mode"] as? String,
                            map = data["map"] as? String
                        )
                        Log.d("TournamentRepository", "Successfully parsed tournament: ${tournament.name}")
                        tournament.toDomain()
                    } catch (e: Exception) {
                        Log.e("TournamentRepository", "Error parsing tournament document ${doc.id}: ${e.message}")
                        Log.e("TournamentRepository", "Document data: ${doc.data}")
                        null
                    }
                } ?: emptyList()
                
                Log.d("TournamentRepository", "Sending ${tournaments.size} tournaments to UI")
                trySend(tournaments)
            }
        
        awaitClose { 
            Log.d("TournamentRepository", "Closing tournament subscription")
            subscription.remove() 
        }
    }

    override suspend fun getTournamentById(id: String): DomainTournament? = try {
        val doc = tournamentsCollection.document(id).get().await()
        val data = doc.data
        if (data != null) {
            val tournament = Tournament.fromFirestore(
                id = doc.id,
                name = data["name"] as? String ?: "",
                description = data["description"] as? String ?: "",
                startDate = data["startDate"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now(),
                endDate = data["endDate"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now(),
                maxPlayers = (data["maxPlayers"] as? Number)?.toInt() ?: 0,
                currentPlayers = (data["currentPlayers"] as? Number)?.toInt() ?: 0,
                entryFee = (data["entryFee"] as? Number)?.toInt() ?: 0,
                perKillPrize = (data["perKillPrize"] as? Number)?.toInt() ?: 0,
                prizePool = (data["prizePool"] as? Number)?.toInt() ?: 0,
                status = (data["status"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                isFeatured = data["isFeatured"] as? Boolean ?: false,
                gameId = data["gameId"] as? String ?: "",
                createdBy = data["createdBy"] as? String ?: "",
                createdAt = data["createdAt"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now(),
                imageUrl = data["imageUrl"] as? String ?: "",
                updatedAt = data["updatedAt"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now(),
                rulesUrl = data["rulesUrl"] as? String ?: "",
                platform = data["platform"] as? String ?: "",
                entryType = data["entryType"] as? String ?: "",
                isActive = data["isActive"] as? Boolean ?: true,
                mode = data["mode"] as? String,
                map = data["map"] as? String
            )
            tournament.toDomain()
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }

    override suspend fun createTournament(tournament: DomainTournament): Result<DomainTournament> = try {
        val dataTournament = tournament.toData()
        val docRef = tournamentsCollection.add(dataTournament).await()
        val createdTournament = dataTournament.copy(id = docRef.id).toDomain()
        Result.success(createdTournament)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateTournament(tournament: DomainTournament): Result<DomainTournament> = try {
        val dataTournament = tournament.toData()
        tournamentsCollection.document(tournament.id)
            .set(dataTournament)
            .await()
        Result.success(tournament)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteTournament(id: String): Result<Unit> = try {
        tournamentsCollection.document(id)
            .delete()
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun joinTournament(tournamentId: String, userId: String): Result<Unit> = try {
        val tournamentRef = tournamentsCollection.document(tournamentId)
        
        firebaseManager.firestore.runTransaction { transaction ->
            val tournament = transaction.get(tournamentRef).toObject(Tournament::class.java)
                ?: throw Exception("Tournament not found")

            if (tournament.currentPlayers >= tournament.maxPlayers) {
                throw Exception("Tournament is full")
            }

            transaction.update(tournamentRef, "currentPlayers", tournament.currentPlayers + 1)
        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun leaveTournament(tournamentId: String, userId: String): Result<Unit> = try {
        val tournamentRef = tournamentsCollection.document(tournamentId)
        
        firebaseManager.firestore.runTransaction { transaction ->
            val tournament = transaction.get(tournamentRef).toObject(Tournament::class.java)
                ?: throw Exception("Tournament not found")

            if (tournament.currentPlayers <= 0) {
                throw Exception("No players in tournament")
            }

            transaction.update(tournamentRef, "currentPlayers", tournament.currentPlayers - 1)
        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateTournamentStatus(id: String, status: TournamentStatus): Result<Unit> = try {
        tournamentsCollection.document(id)
            .update("status", status.name)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun registerForTournament(
        tournamentId: String,
        userId: String,
        displayName: String,
        teamName: String,
        inGameId: String
    ): Result<Unit> = try {
        val tournamentRef = firebaseManager.firestore.collection("tournaments").document(tournamentId)
        val userWalletRef = firebaseManager.firestore.collection("wallets").document(userId)
        val registrationRef = tournamentRef.collection("registrations").document(userId)
        val userTransactionsRef = firebaseManager.firestore.collection("users").document(userId).collection("transactions")
        val userRef = firebaseManager.firestore.collection("users").document(userId)

        firebaseManager.firestore.runTransaction { transaction ->
            // 1. Get current state
            val tournamentSnapshot = transaction.get(tournamentRef)
            val walletSnapshot = transaction.get(userWalletRef)
            val registrationSnapshot = transaction.get(registrationRef)
            val userSnapshot = transaction.get(userRef)

            val tournament = tournamentSnapshot.toObject(com.cehpoint.netwin.data.model.Tournament::class.java)
                ?: throw Exception("Tournament not found. It might have been deleted.")
            val wallet = walletSnapshot.toObject(com.cehpoint.netwin.data.model.Wallet::class.java)
                ?: throw Exception("User wallet not found.")
            val user = userSnapshot.toObject(com.cehpoint.netwin.data.model.User::class.java)
                ?: throw Exception("User not found.")

            // 2. Perform validation checks
            if (registrationSnapshot.exists()) {
                throw Exception("You are already registered for this tournament.")
            }
            if (tournament.currentPlayers >= tournament.maxPlayers) {
                throw Exception("Sorry, this tournament is already full.")
            }
            if (wallet.balance < tournament.entryFee) {
                throw Exception("Insufficient balance. Please add funds to your wallet.")
            }
            // KYC check (if required)
            if (tournament.entryFee > 0 && user.kycStatus != "verified") {
                throw Exception("KYC verification required to register for this tournament.")
            }
            // Registration window check (if required)
            val now = System.currentTimeMillis()
            if (now < tournament.startDate - 60 * 60 * 1000) { // Example: registration opens 1 hour before start
                throw Exception("Registration is not open yet.")
            }
            if (now > tournament.startDate) {
                throw Exception("Registration is closed.")
            }
            // Status check: Only allow registration if tournament is UPCOMING or STARTS_SOON
            val statusList = tournament.status
            if (statusList.contains("ONGOING") || statusList.contains("COMPLETED")) {
                throw Exception("Registration is closed for this tournament.")
            }

            // 3. All checks passed, perform the writes
            val newBalance = wallet.balance - tournament.entryFee
            val newPlayerCount = tournament.currentPlayers + 1

            // Update wallet
            transaction.update(userWalletRef, "balance", newBalance)
            // Update tournament player count
            transaction.update(tournamentRef, "currentPlayers", newPlayerCount)
            // Create registration document
            val registrationRecord = com.cehpoint.netwin.data.model.TournamentRegistration(
                tournamentId = tournamentId,
                userId = userId,
                displayName = displayName,
                teamName = teamName,
                inGameId = inGameId,
                status = "registered",
                registeredAt = com.google.firebase.Timestamp.now()
            )
            transaction.set(registrationRef, registrationRecord)
            // Create a transaction record for the user
            val transactionRecord = com.cehpoint.netwin.data.model.Transaction(
                amount = tournament.entryFee.toDouble(),
                type = com.cehpoint.netwin.data.model.TransactionType.TOURNAMENT_ENTRY,
                status = com.cehpoint.netwin.data.model.TransactionStatus.COMPLETED,
                description = "Entry fee for ${tournament.name}",
                tournamentId = tournamentId,
                tournamentTitle = tournament.name,
                createdAt = com.google.firebase.Timestamp.now()
            )
            transaction.set(userTransactionsRef.document(), transactionRecord)
            null // A successful transaction returns null
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("TournamentRepoImpl", "Error registering for tournament: ${e.message}", e)
        Result.failure(e)
    }

    override suspend fun isUserRegisteredForTournament(tournamentId: String, userId: String): Boolean = try {
        val registrationRef = firebaseManager.firestore
            .collection("tournaments")
            .document(tournamentId)
            .collection("registrations")
            .document(userId)

        registrationRef.get().await().exists()
    } catch (e: Exception) {
        Log.e("TournamentRepoImpl", "Error checking registration status: ${e.message}", e)
        false
    }
} 