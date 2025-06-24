package com.cehpoint.netwin.data.repository

import com.cehpoint.netwin.data.remote.FirebaseManager
import com.cehpoint.netwin.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseManager: FirebaseManager
) : AuthRepository {

    override val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseManager.auth.addAuthStateListener(listener)
        awaitClose { firebaseManager.auth.removeAuthStateListener(listener) }
    }

    override val isAuthenticated: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser != null)
        }
        firebaseManager.auth.addAuthStateListener(listener)
        awaitClose { firebaseManager.auth.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> =
        firebaseManager.signIn(email, password)

    override suspend fun signUp(email: String, password: String): Result<Unit> =
        firebaseManager.signUp(email, password)

    override suspend fun signOut(): Result<Unit> =
        firebaseManager.signOut()
} 