package com.cehpoint.netwin.domain.repository

import com.cehpoint.netwin.ResultState
import com.cehpoint.netwin.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {

    suspend fun createUser(user: User): Result<User>
    suspend fun getUser(userId: String): Result<User>
    fun getUserFlow(userId: String): Flow<User?>
    suspend fun updateUser(user: User): Result<User>
    suspend fun updateUserField(userId: String, field: String, value: Any): Result<Unit>
    suspend fun getUserByUsername(username: String): Boolean
} 