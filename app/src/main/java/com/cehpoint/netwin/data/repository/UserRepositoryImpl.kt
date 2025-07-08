package com.cehpoint.netwin.data.repository

import android.util.Log
import com.cehpoint.netwin.ResultState
import com.cehpoint.netwin.data.model.KycStatus
import com.cehpoint.netwin.domain.model.User
import com.cehpoint.netwin.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firebaseFirestore: FirebaseFirestore, private val firebaseAuth: FirebaseAuth
) : UserRepository {

    private val usersCollection = firebaseFirestore.collection("users")



    override suspend fun createUser(user: User): Result<User> {
        return try {
            Log.d("UserRepositoryImpl", "Starting user creation process")
            Log.d("UserRepositoryImpl", "User ID: ${user.id}")
            Log.d("UserRepositoryImpl", "User email: ${user.email}")

            if (user.id.isBlank()) {
                Log.e("UserRepositoryImpl", "User ID is blank")
                return Result.failure(Exception("User ID cannot be blank"))
            }

            // Create the document with the user's ID
            val docRef = usersCollection.document(user.id)
            Log.d("UserRepositoryImpl", "Created document reference with ID: ${docRef.id}")

            // Set the user data
            docRef.set(user).await()
            Log.d("UserRepositoryImpl", "User document created successfully")

            // Verify the document was created
            val createdDoc = docRef.get().await()
            if (createdDoc.exists()) {
                Log.d("UserRepositoryImpl", "Verified document exists in Firestore")
                Result.success(user)
            } else {
                Log.e("UserRepositoryImpl", "Document does not exist after creation")
                Result.failure(Exception("Failed to create user document"))
            }
        } catch (e: Exception) {
            Log.e("UserRepositoryImpl", "Failed to create user document", e)
            Log.e("UserRepositoryImpl", "Error message: ${e.message}")
            Log.e("UserRepositoryImpl", "Error cause: ${e.cause}")
            Result.failure(e)
        }
    }

    override suspend fun getUser(userId: String): Result<User> = try {
        Log.d("UserRepositoryImpl", "=== getUser STARTED ===")
        Log.d("UserRepositoryImpl", "getUser - Called with userId = $userId")
        Log.d("UserRepositoryImpl", "getUser - FirebaseAuth current user: ${firebaseAuth.currentUser}")
        Log.d("UserRepositoryImpl", "getUser - FirebaseAuth current user UID: ${firebaseAuth.currentUser?.uid}")

        val docSnapshot = usersCollection.document(userId).get().await()
        Log.d("UserRepositoryImpl", "getUser - Firestore document fetched: exists = ${docSnapshot.exists()}")
        Log.d("UserRepositoryImpl", "getUser - Document data: ${docSnapshot.data}")
        
        val user = docSnapshot.toObject(User::class.java)
        Log.d("UserRepositoryImpl", "getUser - Parsed user object: $user")
        
        if (user != null) {
            Log.d("UserRepositoryImpl", "getUser - User found successfully")
            Log.d("UserRepositoryImpl", "getUser - User ID: ${user.id}")
            Log.d("UserRepositoryImpl", "getUser - User email: ${user.email}")
            Log.d("UserRepositoryImpl", "getUser - User username: ${user.username}")
            Log.d("UserRepositoryImpl", "getUser - User displayName: ${user.displayName}")
            Log.d("UserRepositoryImpl", "getUser - User country: ${user.country}")
            Log.d("UserRepositoryImpl", "getUser - User kycStatus: ${user.kycStatus}")
            Log.d("UserRepositoryImpl", "=== getUser COMPLETED SUCCESS ===")
            Result.success(user)
        } else {
            Log.e("UserRepositoryImpl", "getUser - User not found for userId = $userId")
            Log.d("UserRepositoryImpl", "=== getUser COMPLETED FAILURE - USER NOT FOUND ===")
            Result.failure(Exception("User not found"))
        }
    } catch (e: Exception) {
        Log.e("UserRepositoryImpl", "getUser - Error fetching user: ${e.message}", e)
        Log.d("UserRepositoryImpl", "=== getUser COMPLETED FAILURE - EXCEPTION ===")
        Result.failure(e)
    }

    override fun getUserFlow(userId: String): Flow<User?> = flow {
        try {
            val docSnapshot = usersCollection.document(userId).get().await()
            emit(docSnapshot.toObject(User::class.java))
        } catch (e: Exception) {
            emit(null)
        }
    }

    override suspend fun updateUser(user: User): Result<User> = try {
        usersCollection.document(user.id).set(user).await()
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateUserField(userId: String, field: String, value: Any): Result<Unit> = try {
        usersCollection.document(userId).update(field, value).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteUser(userId: String): Result<Unit> = try {
        usersCollection.document(userId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun getAllUsers(): Flow<List<User>> = flow {
        try {
            val snapshot = usersCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val users = snapshot.toObjects(User::class.java)
            emit(users)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    suspend fun getUserByEmail(email: String): Result<User?> = try {
        val snapshot = usersCollection
            .whereEqualTo("email", email)
            .get()
            .await()
        val user = snapshot.documents.firstOrNull()?.toObject(User::class.java)
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getUserByUsername(username: String): Boolean {
        return try {
            val snapshot = usersCollection
                .whereEqualTo("username", username)
                .get()
                .await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            false // treat as not found on error
        }
    }
} 