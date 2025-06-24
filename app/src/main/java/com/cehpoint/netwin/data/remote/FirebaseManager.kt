package com.cehpoint.netwin.data.remote

import android.util.Log
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.cehpoint.netwin.data.model.Tournament
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query

@Singleton
class FirebaseManager @Inject constructor(
    internal val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "FirebaseManager"
        
        // Firestore Collections
        object Collections {
            const val TOURNAMENTS = "sampletournaments"
            const val USERS = "users"
            const val MATCHES = "matches"
            const val TRANSACTIONS = "transactions"
            const val KYC_DOCUMENTS = "kyc_documents"
        }
    }

    // Make these properties internal so they can be accessed within the same module
    internal val auth = FirebaseAuth.getInstance()
    internal val storage = FirebaseStorage.getInstance()

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
            .orderBy("startDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Successfully fetched ${documents.size()} tournaments")
                val tournaments = documents.mapNotNull { doc ->
                    try {
                        val tournament = Tournament.fromFirestore(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            description = doc.getString("description") ?: "",
                            startDate = doc.getTimestamp("startDate") ?: return@mapNotNull null,
                            endDate = doc.getTimestamp("endDate") ?: return@mapNotNull null,
                            maxPlayers = doc.getLong("maxPlayers")?.toInt() ?: 0,
                            currentPlayers = doc.getLong("currentPlayers")?.toInt() ?: 0,
                            entryFee = doc.getLong("entryFee")?.toInt() ?: 0,
                            perKillPrize = doc.getLong("perKillPrize")?.toInt() ?: 0,
                            prizePool = doc.getLong("prizePool")?.toInt() ?: 0,
                            status = doc.get("status") as? List<String> ?: emptyList(),
                            isFeatured = doc.getBoolean("isFeatured") ?: false,
                            gameId = doc.getString("gameId") ?: "",
                            createdBy = doc.getString("createdBy") ?: "",
                            createdAt = doc.getTimestamp("createdAt") ?: return@mapNotNull null,
                            imageUrl = doc.getString("imageUrl") ?: ""
                        )
                        Log.d(TAG, "Successfully parsed tournament: ${tournament.name}")
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
                    name = document.getString("name") ?: "",
                    description = document.getString("description") ?: "",
                    startDate = document.getTimestamp("startDate") ?: return Result.success(null),
                    endDate = document.getTimestamp("endDate") ?: return Result.success(null),
                    maxPlayers = document.getLong("maxPlayers")?.toInt() ?: 0,
                    currentPlayers = document.getLong("currentPlayers")?.toInt() ?: 0,
                    entryFee = document.getLong("entryFee")?.toInt() ?: 0,
                    perKillPrize = document.getLong("perKillPrize")?.toInt() ?: 0,
                    prizePool = document.getLong("prizePool")?.toInt() ?: 0,
                    status = document.get("status") as? List<String> ?: emptyList(),
                    isFeatured = document.getBoolean("isFeatured") ?: false,
                    gameId = document.getString("gameId") ?: "",
                    createdBy = document.getString("createdBy") ?: "",
                    createdAt = document.getTimestamp("createdAt") ?: return Result.success(null),
                    imageUrl = document.getString("imageUrl") ?: ""
                )
                Log.d(TAG, "Successfully fetched tournament: ${tournament.name}")
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