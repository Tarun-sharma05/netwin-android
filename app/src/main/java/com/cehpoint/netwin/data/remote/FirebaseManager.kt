package com.cehpoint.netwin.data.remote

import android.util.Log
import com.cehpoint.netwin.data.model.Tournament
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseManager @Inject constructor(
    internal val firebaseAuth: FirebaseAuth,
    internal val firestore: FirebaseFirestore,
    internal val storage: FirebaseStorage
) {
    companion object {
        private const val TAG = "FirebaseManager"
        
        // Firestore Collections
        object Collections {
            const val TOURNAMENTS = "tournaments"
            const val USERS = "users"
            const val MATCHES = "matches"
            const val TRANSACTIONS = "transactions"
            const val KYC_DOCUMENTS = "kyc_documents"
        }
    }

    // Use the injected instances instead of creating new ones
    internal val auth = firebaseAuth

    init {
        // Initialize Firebase App Check
        val appCheck = FirebaseAppCheck.getInstance()
        appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )
        
        // Log App Check state
        appCheck.getAppCheckToken(false).addOnSuccessListener { token ->
            Log.d(TAG, "App Check token obtained successfully")
            Log.d(TAG, "Token: ${token.token.take(10)}...")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to get App Check token: ${e.message}")
        }
    }

    // Authentication methods
    suspend fun signIn(email: String, password: String): Result<Unit> = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        if (result.user != null) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Authentication failed"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun signUp(email: String, password: String): Result<Unit> = try {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        if (result.user != null) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Failed to create user"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun signOut(): Result<Unit> = try {
        auth.signOut()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

//    // Add this method inside your FirebaseManager class
//    suspend fun updateDocumentField(
//        collection: String,
//        documentId: String,
//        field: String,
//        value: Any
//    ): Result<Unit> = try {
//        firestore.collection(collection)
//            .document(documentId)
//            .update(field, value)
//            .await()
//        Result.success(Unit)
//    } catch (e: Exception) {
//        Result.failure(e)
//    }


    // Firestore methods
    suspend fun <T> addDocument(collection: String, document: T): Result<String> = try {
        val docRef = firestore.collection(collection).document()
        docRef.set(document as Any).await()
        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun <T> getDocument(collection: String, documentId: String, type: Class<T>): Result<T?> = try {
        val doc = firestore.collection(collection).document(documentId).get().await()
        Result.success(doc.toObject(type))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun <T> updateDocument(collection: String, documentId: String, data: T): Result<Unit> = try {
        firestore.collection(collection).document(documentId).set(data as Any).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteDocument(collection: String, documentId: String): Result<Unit> = try {
        firestore.collection(collection).document(documentId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Storage methods
    suspend fun uploadFile(path: String, data: ByteArray): Result<String> = try {
        val ref = storage.reference.child(path)
        ref.putBytes(data).await()
        Result.success(ref.downloadUrl.await().toString())
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteFile(path: String): Result<Unit> = try {
        storage.reference.child(path).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Query methods
    suspend fun <T> queryDocuments(
        collection: String,
        type: Class<T>,
        field: String,
        value: Any
    ): Result<List<T>> = try {
        val documents = firestore.collection(collection)
            .whereEqualTo(field, value)
            .get()
            .await()
        val data = documents.toObjects(type)
        Result.success(data)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun <T> queryDocumentsWithOrder(
        collection: String,
        type: Class<T>,
        field: String,
        direction: Query.Direction = Query.Direction.DESCENDING
    ): Result<List<T>> = try {
        val documents = firestore.collection(collection)
            .orderBy(field, direction)
            .get()
            .await()
        val data = documents.toObjects(type)
        Result.success(data)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun getTournaments(callback: (List<Tournament>) -> Unit) {
        Log.d(TAG, "Fetching tournaments from collection: ${Collections.TOURNAMENTS}")
        firestore.collection(Collections.TOURNAMENTS)
            .orderBy("startTime", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Successfully fetched ${documents.size()} tournaments")
                val tournaments = documents.mapNotNull { doc ->
                    try {
                        val tournament = Tournament.fromFirestore(
                            id = doc.id,
                            title = doc.getString("name") ?: "",
                            description = doc.getString("description") ?: "",
                            gameMode = doc.getString("gameType") ?: "",
                            matchType = doc.getString("matchType") ?: "",
                            map = doc.getString("map") ?: "",
                            startDate = doc.getTimestamp("startTime") ?: return@mapNotNull null,
                            entryFee = doc.getDouble("entryFee") ?: 0.0,
                            prizePool = doc.getDouble("prizePool") ?: 0.0,
                            maxTeams = doc.getLong("maxTeams")?.toInt() ?: 0,
                            registeredTeams = doc.getLong("registeredTeams")?.toInt() ?: 0,
                            status = doc.getString("status") ?: "upcoming",
                            rules = doc.get("rules") as? List<String>,
                            image = doc.getString("bannerImage"),
                            rewardsDistribution = doc.get("rewardsDistribution") as? List<Map<String, Any>>,
                            createdAt = doc.getTimestamp("createdAt") ?: return@mapNotNull null,
                            killReward = doc.getDouble("killReward"),
                            roomId = doc.getString("roomId"),
                            roomPassword = doc.getString("roomPassword"),
                            actualStartTime = doc.getTimestamp("actualStartTime")
                        )
                        Log.d(TAG, "Successfully parsed tournament: ${tournament.title}")
                        tournament
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing tournament document ${doc.id}", e)
                        null
                    }
                }
                Log.d(TAG, "Sending ${tournaments.size} tournaments to callback")
                callback(tournaments)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching tournaments", e)
                callback(emptyList())
            }
    }

    suspend fun getTournamentById(tournamentId: String): Result<Tournament?> = try {
        Log.d(TAG, "Fetching tournament with ID: $tournamentId")
        val document = firestore.collection(Collections.TOURNAMENTS)
            .document(tournamentId)
            .get()
            .await()

        if (document != null && document.exists()) {
            try {
                val tournament = Tournament.fromFirestore(
                    id = document.id,
                    title = document.getString("name") ?: "",
                    description = document.getString("description") ?: "",
                    gameMode = document.getString("gameType") ?: "",
                    matchType = document.getString("matchType") ?: "",
                    map = document.getString("map") ?: "",
                    startDate = document.getTimestamp("startTime") ?: return Result.success(null),
                    entryFee = document.getDouble("entryFee") ?: 0.0,
                    prizePool = document.getDouble("prizePool") ?: 0.0,
                    maxTeams = document.getLong("maxTeams")?.toInt() ?: 0,
                    registeredTeams = document.getLong("registeredTeams")?.toInt() ?: 0,
                    status = document.getString("status") ?: "upcoming",
                    rules = document.get("rules") as? List<String>,
                    image = document.getString("bannerImage"),
                    rewardsDistribution = document.get("rewardsDistribution") as? List<Map<String, Any>>,
                    createdAt = document.getTimestamp("createdAt") ?: return Result.success(null),
                    killReward = document.getDouble("killReward"),
                    roomId = document.getString("roomId"),
                    roomPassword = document.getString("roomPassword"),
                    actualStartTime = document.getTimestamp("actualStartTime")
                )
                Log.d(TAG, "Successfully fetched tournament: ${tournament.title}")
                Result.success(tournament)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing tournament document", e)
                Result.failure(e)
            }
        } else {
            Log.d(TAG, "No tournament found with ID: $tournamentId")
            Result.success(null)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching tournament", e)
        Result.failure(e)
    }
} 