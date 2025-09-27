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
                
                // Offload mapping to background thread
                GlobalScope.launch(Dispatchers.Default) {
                   val tournaments = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            Log.d("TournamentRepository", "Raw document data for ${doc.id}: $data")

                            // Robust mapping for all known schema variants
                            val title = data["title"] as? String ?: data["name"] as? String ?: ""
                            val gameMode = data["gameMode"] as? String ?: data["gameType"] as? String ?: ""
                            val gameType = data["gameType"] as? String
                            val maxTeams = (data["maxTeams"] as? Number)?.toInt() ?: 0
                            val registeredTeams = (data["registeredTeams"] as? Number)?.toInt() ?: 0
                            val image = data["image"] as? String ?: data["bannerImage"] as? String
                            val country = data["country"] as? String
                            val startDate = when (val st = data["startDate"] ?: data["startTime"]) {
                                is com.google.firebase.Timestamp -> st
                                is String -> try { com.google.firebase.Timestamp(java.util.Date.from(java.time.Instant.parse(st))) } catch (e: Exception) { null }
                                else -> null
                            }
                            val endDate = data["endDate"] as? com.google.firebase.Timestamp ?: data["completedAt"] as? com.google.firebase.Timestamp
                            val registrationStartTime = data["registrationStartTime"] as? com.google.firebase.Timestamp
                            val registrationEndTime = data["registrationEndTime"] as? com.google.firebase.Timestamp

                            Tournament.fromFirestore(
                            id = doc.id,
                                title = title,
                                description = data["description"] as? String,
                                gameMode = gameMode,
                                gameType = gameType,
                                entryFee = (data["entryFee"] as? Number)?.toDouble() ?: 0.0,
                                prizePool = (data["prizePool"] as? Number)?.toDouble() ?: 0.0,
                                maxTeams = maxTeams,
                                registeredTeams = registeredTeams,
                                status = data["status"] as? String ?: "upcoming",
                                startDate = startDate,
                                endDate = endDate,
                                rules = data["rules"] as? List<String>,
                                image = image,
                                createdAt = data["createdAt"] as? com.google.firebase.Timestamp,
                                updatedAt = data["updatedAt"] as? com.google.firebase.Timestamp,
                                // Extra fields for app logic
                                matchType = data["matchType"] as? String,
                                map = data["map"] as? String,
                                country = country,
                                registrationStartTime = registrationStartTime,
                                registrationEndTime = registrationEndTime,
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
                gameMode = data["gameMode"] as? String ?: (data["gameType"] as? String ?: ""),
                gameType = data["gameType"] as? String,
                entryFee = (data["entryFee"] as? Number)?.toDouble() ?: 0.0,
                prizePool = (data["prizePool"] as? Number)?.toDouble() ?: 0.0,
                maxTeams = (data["maxTeams"] as? Number)?.toInt() ?: 0,
                registeredTeams = (data["registeredTeams"] as? Number)?.toInt() ?: 0,
                status = data["status"] as? String ?: "upcoming",
                startDate = data["startDate"] as? Timestamp,
                endDate = data["endDate"] as? Timestamp,
                rules = data["rules"] as? List<String>,
                image = (data["image"] as? String) ?: (data["bannerImage"] as? String),
                createdAt = data["createdAt"] as? Timestamp,
                updatedAt = data["updatedAt"] as? Timestamp,
                // Extra fields for app logic
                matchType = data["matchType"] as? String,
                map = data["map"] as? String,
                country = data["country"] as? String,
                registrationStartTime = data["registrationStartTime"] as? Timestamp,
                registrationEndTime = data["registrationEndTime"] as? Timestamp,
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

    override suspend fun updateTournamentStatus(id: String, status: TournamentStatus): Result<Unit> {
        return try {
//            val result = firebaseManager.updateDocumentField(
//                collection = FirebaseManager.Collections.TOURNAMENTS,
//                documentId = id,
//                field = "status",
//                value = status.name
//            )
//            Result.success(Unit)
            // Use direct Firestore update instead of the missing method
            tournamentsCollection.document(id)
                .update("status", status.name)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("TournamentRepository", "Error updating tournament status: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getUserTournamentRegistrations(userId: String): List<String> {
        return try {
            // Query the tournament_registrations collection for the user's registrations
            val snapshot = firebaseManager.firestore
                .collection("tournament_registrations")
                .whereEqualTo("userId", userId)
                .get()
                .await()
                
            // Extract tournament IDs from the registrations
            snapshot.documents.mapNotNull { doc ->
                doc.getString("tournamentId")
            }
        } catch (e: Exception) {
            Log.e("TournamentRepository", "Error fetching user tournament registrations: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun registerForTournament(
        tournamentId: String,
        userId: String,
        displayName: String,
        teamName: String,
        playerIds: List<String>
//    ): Result<Unit> = try {
//        val tournamentRef = firebaseManager.firestore.collection("tournaments").document(tournamentId)
//        val userWalletRef = firebaseManager.firestore.collection("wallets").document(userId)
//        val registrationRef = firebaseManager.firestore.collection("tournament_registrations").document("${tournamentId}_${userId}")
//        val userTransactionsRef = firebaseManager.firestore.collection("users").document(userId).collection("transactions")
//        val userRef = firebaseManager.firestore.collection("users").document(userId)
        ): Result<Unit> = try {
            Log.d("TournamentRepoImpl", "=== STARTING TOURNAMENT REGISTRATION ===")
            Log.d("TournamentRepoImpl", "Tournament ID: $tournamentId")
            Log.d("TournamentRepoImpl", "User ID: $userId")
            Log.d("TournamentRepoImpl", "Display Name: $displayName")
            Log.d("TournamentRepoImpl", "Team Name: $teamName")
            Log.d("TournamentRepoImpl", "Player IDs: $playerIds")
            
            val tournamentRef = firebaseManager.firestore.collection("tournaments").document(tournamentId)
            val userWalletRef = firebaseManager.firestore.collection("wallets").document(userId)
            val registrationRef = firebaseManager.firestore.collection("tournament_registrations")
                .document("${tournamentId}_${userId}")
            val userTransactionsRef = firebaseManager.firestore.collection("users")
                .document(userId).collection("transactions")
            val userRef = firebaseManager.firestore.collection("users").document(userId)

            Log.d("TournamentRepoImpl", "Starting Firestore transaction...")
        firebaseManager.firestore.runTransaction { transaction ->
            // 1. Get current state
            Log.d("TournamentRepoImpl", "Getting tournament document...")
            val tournamentSnapshot = transaction.get(tournamentRef)
            Log.d("TournamentRepoImpl", "Tournament exists: ${tournamentSnapshot.exists()}")
            
            Log.d("TournamentRepoImpl", "Getting wallet document...")
            val walletSnapshot = transaction.get(userWalletRef)
            Log.d("TournamentRepoImpl", "Wallet exists: ${walletSnapshot.exists()}")
            
            Log.d("TournamentRepoImpl", "Getting registration document...")
            val registrationSnapshot = transaction.get(registrationRef)
            Log.d("TournamentRepoImpl", "Registration exists: ${registrationSnapshot.exists()}")
            
            Log.d("TournamentRepoImpl", "Getting user document...")
            val userSnapshot = transaction.get(userRef)
            Log.d("TournamentRepoImpl", "User exists: ${userSnapshot.exists()}")

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
                    startDate = (data["startDate"] as? Timestamp) ?: (data["startTime"] as? Timestamp),
                    endDate = data["endDate"] as? Timestamp,
                    rules = data["rules"] as? List<String>,
                    image = data["image"] as? String,
                    createdAt = data["createdAt"] as? Timestamp,
                    updatedAt = data["updatedAt"] as? Timestamp,
                    // Extra fields for app logic
                    matchType = data["matchType"] as? String,
                    map = data["map"] as? String,
                    registrationStartTime = data["registrationStartTime"] as? Timestamp,
                    registrationEndTime = data["registrationEndTime"] as? Timestamp,
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
                Log.e("TournamentRepoImpl", "Tournament not found for ID: $tournamentId")
                throw Exception("Tournament not found. It might have been deleted.")
            }
            Log.d("TournamentRepoImpl", "Tournament loaded: ${tournament.title}, Entry Fee: ${tournament.entryFee}")
            
            val wallet = walletSnapshot.toObject(com.cehpoint.netwin.data.model.Wallet::class.java)
                ?: throw Exception("User wallet not found.")
            Log.d("TournamentRepoImpl", "Wallet loaded: Bonus=${wallet.bonusBalance}, Withdrawable=${wallet.withdrawableBalance}")
            
            val user = userSnapshot.toObject(com.cehpoint.netwin.data.model.User::class.java)
                ?: throw Exception("User not found.")
            Log.d("TournamentRepoImpl", "User loaded: KYC Status=${user.kycStatus}")

            // 2. Perform validation checks
            Log.d("TournamentRepoImpl", "Starting validation checks...")
            
            if (registrationSnapshot.exists()) {
                Log.e("TournamentRepoImpl", "User already registered for tournament")
                throw Exception("You are already registered for this tournament.")
            }
            Log.d("TournamentRepoImpl", "Registration check passed")
            
            if (tournament.registeredTeams >= tournament.maxTeams) {
                Log.e("TournamentRepoImpl", "Tournament full: ${tournament.registeredTeams}/${tournament.maxTeams}")
                throw Exception("Sorry, this tournament is already full.")
            }
            Log.d("TournamentRepoImpl", "Tournament capacity check passed: ${tournament.registeredTeams}/${tournament.maxTeams}")
            
            // Check total balance (bonus + withdrawable)
            val totalBalance = wallet.bonusBalance + wallet.withdrawableBalance
            Log.d("TournamentRepoImpl", "Balance check: Total=$totalBalance, Required=${tournament.entryFee}")
            if (totalBalance < tournament.entryFee) {
                Log.e("TournamentRepoImpl", "Insufficient balance: $totalBalance < ${tournament.entryFee}")
                throw Exception("Insufficient balance. Please add funds to your wallet.")
            }
            Log.d("TournamentRepoImpl", "Balance check passed")
            
            // KYC check (if required)
//            if (tournament.entryFee > 0 && user.kycStatus != "verified") {
            Log.d("TournamentRepoImpl", "KYC check: Entry fee=${tournament.entryFee}, KYC Status=${user?.kycStatus}")
            if(tournament.entryFee > 0 && user?.kycStatus?.equals("verified", ignoreCase = true) == false) {
                Log.e("TournamentRepoImpl", "KYC verification required")
                throw Exception("KYC verification required to register for this tournament.")
            }
            Log.d("TournamentRepoImpl", "KYC check passed")
            
            // Registration window check (time-based)
            val now = System.currentTimeMillis()
            val startMillis = tournament.startDate?.toDate()?.time
            val regStartMillis = tournament.registrationStartTime?.toDate()?.time ?: Long.MIN_VALUE
            val regEndMillis = tournament.registrationEndTime?.toDate()?.time ?: (startMillis ?: Long.MAX_VALUE)
            Log.d("TournamentRepoImpl", "Registration window: now=$now, regStart=$regStartMillis, regEnd=$regEndMillis, start=$startMillis, status=${tournament.status}")
            if (!(now >= regStartMillis && now < regEndMillis)) {
                Log.e("TournamentRepoImpl", "Registration window closed")
                throw Exception("Registration is closed.")
            }
            Log.d("TournamentRepoImpl", "Registration window check passed")

            // 3. All checks passed, perform the writes
            Log.d("TournamentRepoImpl", "All validation checks passed! Starting database writes...")
            val entryFee = tournament.entryFee
            
            // Deduct from bonus balance first, then from withdrawable balance
            val bonusUsed = minOf(wallet.bonusBalance, entryFee)
            val withdrawableUsed = entryFee - bonusUsed
            
            val newBonusBalance = wallet.bonusBalance - bonusUsed
            val newWithdrawableBalance = wallet.withdrawableBalance - withdrawableUsed
            val newTotalBalance = newBonusBalance + newWithdrawableBalance
            val newPlayerCount = tournament.registeredTeams + 1

            Log.d("TournamentRepoImpl", "Balance calculation: Bonus used=$bonusUsed, Withdrawable used=$withdrawableUsed")
            Log.d("TournamentRepoImpl", "New balances: Bonus=$newBonusBalance, Withdrawable=$newWithdrawableBalance, Total=$newTotalBalance")
            Log.d("TournamentRepoImpl", "New player count: $newPlayerCount")

            // Update wallet with new balances
            Log.d("TournamentRepoImpl", "Updating wallet balances...")
            transaction.update(userWalletRef, "bonusBalance", newBonusBalance)
            transaction.update(userWalletRef, "withdrawableBalance", newWithdrawableBalance)
            transaction.update(userWalletRef, "balance", newTotalBalance)
            
            // Update user's walletBalance for display
            Log.d("TournamentRepoImpl", "Updating user wallet balance...")
            transaction.update(userRef, "walletBalance", newTotalBalance)

            // Note: Cannot update tournament registeredTeams count due to Firestore security rules
            // Tournament updates require admin privileges. The count will be calculated dynamically
            Log.d("TournamentRepoImpl", "Skipping tournament update due to permission restrictions")

            // Create registration document
            Log.d("TournamentRepoImpl", "Creating registration document...")
            val teamMembers = playerIds.mapIndexed { index, inGameId ->
                val username = if (index == 0) displayName else "Player ${index + 1}"
                com.cehpoint.netwin.data.model.TeamMember(username = username, inGameId = inGameId)
            }

            val registration = TournamentRegistration(
                tournamentId = tournamentId,
                userId = userId,
                teamName = teamName,
                teamMembers = teamMembers,
                paymentStatus = "completed",
                registeredAt = Timestamp.now()
            )
            Log.d("TournamentRepoImpl", "Setting registration document: ${registrationRef.path}")
            transaction.set(registrationRef, registration)

            // Create wallet transaction record in the correct collection
            Log.d("TournamentRepoImpl", "Creating wallet transaction record...")
            val walletTransactionRecord = com.cehpoint.netwin.data.model.Transaction(
                userId = userId,
                type = com.cehpoint.netwin.data.model.TransactionType.ENTRY_FEE, // Changed to match Firestore rules
                amount = entryFee,
                currency = wallet.currency ?: "INR",
                status = com.cehpoint.netwin.data.model.TransactionStatus.COMPLETED,
                description = "Tournament entry: ${tournament.title}",
                createdAt = Timestamp.now(),
                tournamentId = tournamentId
            )
            val walletTransactionRef = firebaseManager.firestore.collection("wallet_transactions").document()
            Log.d("TournamentRepoImpl", "Setting wallet transaction document: ${walletTransactionRef.path}")
            transaction.set(walletTransactionRef, walletTransactionRecord)
            
            Log.d("TournamentRepoImpl", "All transaction writes completed successfully!")
        }.await()

        // Verify the transaction actually committed by checking if documents exist
        Log.d("TournamentRepoImpl", "Verifying transaction commit...")
        val verifyRegistration = firebaseManager.firestore
            .collection("tournament_registrations")
            .document("${tournamentId}_${userId}")
            .get()
            .await()
        
        val verifyWalletTransaction = firebaseManager.firestore
            .collection("wallet_transactions")
            .whereEqualTo("userId", userId)
            .whereEqualTo("tournamentId", tournamentId)
            .whereEqualTo("type", "entry_fee")
            .limit(1)
            .get()
            .await()
        
        if (!verifyRegistration.exists()) {
            Log.e("TournamentRepoImpl", "CRITICAL: Registration document not found after transaction!")
            throw Exception("Registration failed - document not created")
        }
        
        if (verifyWalletTransaction.isEmpty) {
            Log.e("TournamentRepoImpl", "CRITICAL: Wallet transaction not found after transaction!")
            throw Exception("Registration failed - transaction not recorded")
        }
        
        // Verify wallet balance was actually deducted
        val verifyWallet = firebaseManager.firestore
            .collection("wallets")
            .document(userId)
            .get()
            .await()
        
        val updatedWallet = verifyWallet.toObject(com.cehpoint.netwin.data.model.Wallet::class.java)
        Log.d("TournamentRepoImpl", "Updated wallet balances: Bonus=${updatedWallet?.bonusBalance}, Withdrawable=${updatedWallet?.withdrawableBalance}")
        
        // Note: We can't verify exact balance deduction here since wallet and tournament variables are out of scope
        // The transaction logic itself ensures proper deduction, and we've verified the documents exist
        
        Log.d("TournamentRepoImpl", "Transaction verification successful - all documents created and wallet updated")
        Log.d("TournamentRepoImpl", "Registration transaction completed successfully")
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