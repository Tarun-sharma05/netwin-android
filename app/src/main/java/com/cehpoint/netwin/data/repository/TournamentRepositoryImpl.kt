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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        Log.d("TournamentRepository", "Starting to fetch tournaments from collection: "+
            "${FirebaseManager.Companion.Collections.TOURNAMENTS}")
        
        val subscription = tournamentsCollection
            .orderBy("startTime")
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
                
                // Offload mapping to background thread
                GlobalScope.launch(Dispatchers.Default) {
                val tournaments = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                            Log.d("TournamentRepository", "Raw document data for ${doc.id}: $data")

                            // Robust mapping for all known schema variants
                            val title = data["title"] as? String ?: data["name"] as? String ?: ""
                            val gameMode = data["gameMode"] as? String ?: data["gameType"] as? String ?: ""
                            val maxTeams = (data["maxTeams"] as? Number)?.toInt() ?: 0
                            val registeredTeams = (data["registeredTeams"] as? Number)?.toInt() ?: 0
                            val image = data["image"] as? String ?: data["bannerImage"] as? String
                            val startDate = when (val st = data["startDate"] ?: data["startTime"]) {
                                is com.google.firebase.Timestamp -> st
                                is String -> try { com.google.firebase.Timestamp(java.util.Date.from(java.time.Instant.parse(st))) } catch (e: Exception) { null }
                                else -> null
                            }
                            val endDate = data["endDate"] as? com.google.firebase.Timestamp ?: data["completedAt"] as? com.google.firebase.Timestamp

                            Tournament.fromFirestore(
                            id = doc.id,
                                title = title,
                                description = data["description"] as? String,
                                gameMode = gameMode,
                                entryFee = (data["entryFee"] as? Number)?.toDouble() ?: 0.0,
                                prizePool = (data["prizePool"] as? Number)?.toDouble() ?: 0.0,
                                maxTeams = maxTeams,
                                registeredTeams = registeredTeams,
                                status = data["status"] as? String ?: "upcoming",
                                startDate = startDate,
                                endDate = endDate,
                                rules = data["rules"] as? String,
                                image = image,
                                createdAt = data["createdAt"] as? com.google.firebase.Timestamp,
                                updatedAt = data["updatedAt"] as? com.google.firebase.Timestamp,
                                // Extra fields for app logic
                                matchType = data["matchType"] as? String,
                                map = data["map"] as? String,
                                rewardsDistribution = data["rewardsDistribution"] as? List<Map<String, Any>>,
                                killReward = (data["killReward"] as? Number)?.toDouble() ?: (data["perKillReward"] as? Number)?.toDouble(),
                                roomId = data["roomId"] as? String,
                                roomPassword = data["roomPassword"] as? String,
                                actualStartTime = data["actualStartTime"] as? com.google.firebase.Timestamp
                            ).toDomain()
                    } catch (e: Exception) {
                        Log.e("TournamentRepository", "Error parsing tournament document ${doc.id}: ${e.message}")
                        Log.e("TournamentRepository", "Document data: ${doc.data}")
                        null
                    }
                } ?: emptyList()
                
                    withContext(Dispatchers.Main) {
                Log.d("TournamentRepository", "Sending ${tournaments.size} tournaments to UI")
                trySend(tournaments)
                    }
                }
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
                title = data["title"] as? String ?: "",
                description = data["description"] as? String,
                gameMode = data["gameMode"] as? String ?: "",
                entryFee = (data["entryFee"] as? Number)?.toDouble() ?: 0.0,
                prizePool = (data["prizePool"] as? Number)?.toDouble() ?: 0.0,
                maxTeams = (data["maxTeams"] as? Number)?.toInt() ?: 0,
                registeredTeams = (data["registeredTeams"] as? Number)?.toInt() ?: 0,
                status = data["status"] as? String ?: "upcoming",
                startDate = data["startDate"] as? Timestamp,
                endDate = data["endDate"] as? Timestamp,
                rules = data["rules"] as? String,
                image = data["image"] as? String,
                createdAt = data["createdAt"] as? Timestamp,
                updatedAt = data["updatedAt"] as? Timestamp,
                // Extra fields for app logic
                matchType = data["matchType"] as? String,
                map = data["map"] as? String,
                rewardsDistribution = data["rewardsDistribution"] as? List<Map<String, Any>>,
                killReward = (data["killReward"] as? Number)?.toDouble(),
                roomId = data["roomId"] as? String,
                roomPassword = data["roomPassword"] as? String,
                actualStartTime = data["actualStartTime"] as? Timestamp
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

            if (tournament.registeredTeams >= tournament.maxTeams) {
                throw Exception("Tournament is full")
            }

            transaction.update(tournamentRef, "registeredTeams", tournament.registeredTeams + 1)
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

            if (tournament.registeredTeams <= 0) {
                throw Exception("No players in tournament")
            }

            transaction.update(tournamentRef, "registeredTeams", tournament.registeredTeams - 1)
        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateTournamentStatus(id: String, status: TournamentStatus): Result<Unit> = try {
        tournamentsCollection.document(id)
            .update("status", status.name.lowercase())
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
        val registrationRef = firebaseManager.firestore.collection("tournament_registrations").document("${tournamentId}_${userId}")
        val userTransactionsRef = firebaseManager.firestore.collection("users").document(userId).collection("transactions")
        val userRef = firebaseManager.firestore.collection("users").document(userId)

        firebaseManager.firestore.runTransaction { transaction ->
            // 1. Get current state
            val tournamentSnapshot = transaction.get(tournamentRef)
            val walletSnapshot = transaction.get(userWalletRef)
            val registrationSnapshot = transaction.get(registrationRef)
            val userSnapshot = transaction.get(userRef)

            // Use manual deserialization for tournament to handle Timestamp -> Long
            val data = tournamentSnapshot.data
            val tournament = if (data != null) {
                Tournament.fromFirestore(
                    id = tournamentSnapshot.id,
                    title = data["title"] as? String ?: "",
                    description = data["description"] as? String,
                    gameMode = data["gameMode"] as? String ?: "",
                    entryFee = (data["entryFee"] as? Number)?.toDouble() ?: 0.0,
                    prizePool = (data["prizePool"] as? Number)?.toDouble() ?: 0.0,
                    maxTeams = (data["maxTeams"] as? Number)?.toInt() ?: 0,
                    registeredTeams = (data["registeredTeams"] as? Number)?.toInt() ?: 0,
                    status = data["status"] as? String ?: "upcoming",
                    startDate = data["startTime"] as? Timestamp, // <-- FIXED: use startTime from Firestore
                    endDate = data["endDate"] as? Timestamp,
                    rules = data["rules"] as? String,
                    image = data["image"] as? String,
                    createdAt = data["createdAt"] as? Timestamp,
                    updatedAt = data["updatedAt"] as? Timestamp,
                    // Extra fields for app logic
                    matchType = data["matchType"] as? String,
                    map = data["map"] as? String,
                    rewardsDistribution = data["rewardsDistribution"] as? List<Map<String, Any>>,
                    killReward = (data["killReward"] as? Number)?.toDouble(),
                    roomId = data["roomId"] as? String,
                    roomPassword = data["roomPassword"] as? String,
                    actualStartTime = data["actualStartTime"] as? Timestamp
                )
            } else {
                null
            }
            if (tournament == null) {
                throw Exception("Tournament not found. It might have been deleted.")
            }
            val wallet = walletSnapshot.toObject(com.cehpoint.netwin.data.model.Wallet::class.java)
                ?: throw Exception("User wallet not found.")
            val user = userSnapshot.toObject(com.cehpoint.netwin.data.model.User::class.java)
                ?: throw Exception("User not found.")

            // 2. Perform validation checks
            if (registrationSnapshot.exists()) {
                throw Exception("You are already registered for this tournament.")
            }
            if (tournament.registeredTeams >= tournament.maxTeams) {
                throw Exception("Sorry, this tournament is already full.")
            }
            
            // Check total balance (bonus + withdrawable)
            val totalBalance = wallet.bonusBalance + wallet.withdrawableBalance
            if (totalBalance < tournament.entryFee) {
                throw Exception("Insufficient balance. Please add funds to your wallet.")
            }
            
            // KYC check (if required)
//            if (tournament.entryFee > 0 && user.kycStatus != "verified") {
            if(tournament.entryFee > 0 && user?.kycStatus?.equals("verified", ignoreCase = true) == false) {
                throw Exception("KYC verification required to register for this tournament.")
            }
            // Registration window check (allow registration any time before start)
            val now = System.currentTimeMillis()
            val startMillis = tournament.startDate?.toDate()?.time ?: 0L
            Log.d("TournamentRepoImpl", "Registration check: now=$now, startMillis=$startMillis, status=${tournament.status}")
            if (now > startMillis) {
                throw Exception("Registration is closed.")
            }
            // Status check: Only allow registration if tournament is upcoming
            if (tournament.status != "upcoming") {
                throw Exception("Registration is closed for this tournament.")
            }

            // 3. All checks passed, perform the writes
            val entryFee = tournament.entryFee
            
            // Deduct from bonus balance first, then from withdrawable balance
            val bonusUsed = minOf(wallet.bonusBalance, entryFee)
            val withdrawableUsed = entryFee - bonusUsed
            
            val newBonusBalance = wallet.bonusBalance - bonusUsed
            val newWithdrawableBalance = wallet.withdrawableBalance - withdrawableUsed
            val newTotalBalance = newBonusBalance + newWithdrawableBalance
            val newPlayerCount = tournament.registeredTeams + 1

            // Update wallet with new balances
            transaction.update(userWalletRef, "bonusBalance", newBonusBalance)
            transaction.update(userWalletRef, "withdrawableBalance", newWithdrawableBalance)
            transaction.update(userWalletRef, "balance", newTotalBalance)
            
            // Update user's walletBalance for display
            transaction.update(userRef, "walletBalance", newTotalBalance)

            // Update tournament with new player count
            transaction.update(tournamentRef, "registeredTeams", newPlayerCount)

            // Create registration document
            val registration = TournamentRegistration(
                tournamentId = tournamentId,
                userId = userId,
                teamName = teamName,
                paymentStatus = "completed",
                registeredAt = Timestamp.now()
            )
            transaction.set(registrationRef, registration)

            // Create transaction record
            val transactionRecord = hashMapOf(
                "type" to "tournament_registration",
                "amount" to -entryFee,
                "description" to "Entry fee for ${tournament.title}",
                "timestamp" to Timestamp.now(),
                "status" to "completed",
                "tournamentId" to tournamentId
            )
            transaction.set(userTransactionsRef.document(), transactionRecord)
        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("TournamentRepoImpl", "Error in registerForTournament transaction", e)
        Result.failure(e)
    }

    override suspend fun isUserRegisteredForTournament(tournamentId: String, userId: String): Boolean = try {
        val registrationRef = firebaseManager.firestore
            .collection("tournament_registrations")
            .document("${tournamentId}_${userId}")

        registrationRef.get().await().exists()
    } catch (e: Exception) {
        Log.e("TournamentRepoImpl", "Error checking registration status: ${e.message}", e)
        false
    }
} 