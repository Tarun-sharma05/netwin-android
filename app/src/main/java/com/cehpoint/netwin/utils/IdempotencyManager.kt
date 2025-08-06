package com.cehpoint.netwin.utils

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.idempotencyDataStore: DataStore<Preferences> by preferencesDataStore(name = "idempotency_keys")

@Singleton
class IdempotencyManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "IdempotencyManager"
        private const val IDEMPOTENCY_KEYS_KEY = "idempotency_keys"
    }
    
    private val idempotencyKeysKey = stringPreferencesKey(IDEMPOTENCY_KEYS_KEY)
    
    data class IdempotencyKey(
        val key: String,
        val operationType: String,
        val userId: String,
        val dataHash: String,
        val createdAt: Timestamp = Timestamp.now(),
        val expiresAt: Timestamp = Timestamp.now().apply { 
            // Expire after 24 hours
            val seconds = seconds + (24 * 60 * 60)
        }
    )
    
    // Generate idempotency key for operation
    fun generateIdempotencyKey(
        operationType: String,
        userId: String,
        data: Map<String, Any>
    ): String {
        val dataString = data.entries
            .sortedBy { it.key }
            .joinToString("|") { "${it.key}=${it.value}" }
        
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(dataString.toByteArray())
            .joinToString("") { "%02x".format(it) }
        
        return "${operationType}_${userId}_${hash.take(16)}"
    }
    
    // Check if operation is duplicate
    suspend fun isDuplicateOperation(
        operationType: String,
        userId: String,
        data: Map<String, Any>
    ): Boolean {
        val key = generateIdempotencyKey(operationType, userId, data)
        val existingKeys = getCurrentIdempotencyKeys()
        
        val existingKey = existingKeys.find { it.key == key }
        if (existingKey != null) {
            // Check if key is expired
            val now = Timestamp.now()
            if (now.seconds < existingKey.expiresAt.seconds) {
                Log.d(TAG, "Duplicate operation detected: $key")
                return true
            } else {
                // Remove expired key
                removeIdempotencyKey(key)
            }
        }
        
        return false
    }
    
    // Store idempotency key
    suspend fun storeIdempotencyKey(
        operationType: String,
        userId: String,
        data: Map<String, Any>
    ) {
        val key = generateIdempotencyKey(operationType, userId, data)
        val dataString = data.entries
            .sortedBy { it.key }
            .joinToString("|") { "${it.key}=${it.value}" }
        
        val dataHash = MessageDigest.getInstance("SHA-256")
            .digest(dataString.toByteArray())
            .joinToString("") { "%02x".format(it) }
        
        val idempotencyKey = IdempotencyKey(
            key = key,
            operationType = operationType,
            userId = userId,
            dataHash = dataHash
        )
        
        val currentKeys = getCurrentIdempotencyKeys().toMutableList()
        currentKeys.add(idempotencyKey)
        saveIdempotencyKeys(currentKeys)
        
        Log.d(TAG, "Stored idempotency key: $key for operation: $operationType")
    }
    
    // Remove idempotency key
    suspend fun removeIdempotencyKey(key: String) {
        val currentKeys = getCurrentIdempotencyKeys().toMutableList()
        currentKeys.removeAll { it.key == key }
        saveIdempotencyKeys(currentKeys)
        
        Log.d(TAG, "Removed idempotency key: $key")
    }
    
    // Clean up expired keys
    suspend fun cleanupExpiredKeys() {
        val now = Timestamp.now()
        val currentKeys = getCurrentIdempotencyKeys().toMutableList()
        val expiredKeys = currentKeys.filter { it.expiresAt.seconds < now.seconds }
        
        if (expiredKeys.isNotEmpty()) {
            currentKeys.removeAll { it.expiresAt.seconds < now.seconds }
            saveIdempotencyKeys(currentKeys)
            Log.d(TAG, "Cleaned up ${expiredKeys.size} expired idempotency keys")
        }
    }
    
    // Get all idempotency keys
    fun getIdempotencyKeys(): Flow<List<IdempotencyKey>> {
        return context.idempotencyDataStore.data.map { preferences ->
            val keysJson = preferences[idempotencyKeysKey] ?: "[]"
            try {
                // Use simple string parsing for now - Firebase handles complex objects
                parseIdempotencyKeysFromString(keysJson)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse idempotency keys", e)
                emptyList()
            }
        }
    }
    
    private suspend fun getCurrentIdempotencyKeys(): List<IdempotencyKey> {
        return try {
            val keysJson = context.idempotencyDataStore.data.map { it[idempotencyKeysKey] ?: "[]" }.first()
            parseIdempotencyKeysFromString(keysJson)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current idempotency keys", e)
            emptyList()
        }
    }
    
    private suspend fun saveIdempotencyKeys(keys: List<IdempotencyKey>) {
        try {
            context.idempotencyDataStore.edit { preferences ->
                preferences[idempotencyKeysKey] = keys.joinToString("|") { "${it.key}:${it.operationType}:${it.userId}:${it.dataHash}:${it.createdAt.seconds}:${it.expiresAt.seconds}" }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save idempotency keys", e)
        }
    }
    
    private fun parseIdempotencyKeysFromString(keysString: String): List<IdempotencyKey> {
        return if (keysString == "[]" || keysString.isEmpty()) {
            emptyList()
        } else {
            try {
                keysString.split("|").mapNotNull { keyString ->
                    val parts = keyString.split(":")
                    if (parts.size >= 6) {
                        IdempotencyKey(
                            key = parts[0],
                            operationType = parts[1],
                            userId = parts[2],
                            dataHash = parts[3],
                            createdAt = Timestamp(parts[4].toLong(), 0),
                            expiresAt = Timestamp(parts[5].toLong(), 0)
                        )
                    } else null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse idempotency key string", e)
                emptyList()
            }
        }
    }
} 