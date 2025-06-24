package com.cehpoint.netwin.domain.repository

import com.cehpoint.netwin.ResultState
import com.cehpoint.netwin.domain.model.User
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<FirebaseUser?>
    val isAuthenticated: Flow<Boolean>


    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
} 