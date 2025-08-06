package com.cehpoint.netwin.utils

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

object RetryUtils {
    private const val TAG = "RetryUtils"
    
    /**
     * Retry a suspend function with exponential backoff
     * @param maxAttempts Maximum number of retry attempts
     * @param initialDelay Initial delay in milliseconds
     * @param maxDelay Maximum delay in milliseconds
     * @param factor Exponential backoff factor
     * @param block The suspend function to retry
     * @return Result of the operation
     */
    suspend fun <T> retryWithBackoff(
        maxAttempts: Int = 3,
        initialDelay: Long = 1000,
        maxDelay: Long = 10000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): Result<T> {
        var currentDelay = initialDelay
        repeat(maxAttempts) { attempt ->
            try {
                return Result.success(block())
            } catch (e: Exception) {
                Log.w(TAG, "Attempt ${attempt + 1} failed: ${e.message}")
                
                if (attempt == maxAttempts - 1) {
                    Log.e(TAG, "All $maxAttempts attempts failed", e)
                    return Result.failure(e)
                }
                
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
        return Result.failure(Exception("Retry failed after $maxAttempts attempts"))
    }
    
    /**
     * Retry a suspend function with timeout
     * @param timeoutMs Timeout in milliseconds
     * @param block The suspend function to retry
     * @return Result of the operation
     */
    suspend fun <T> withTimeout(
        timeoutMs: Long = 10000,
        block: suspend () -> T
    ): Result<T> {
        return try {
            val result = kotlinx.coroutines.withTimeout(timeoutMs) {
                block()
            }
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Operation timed out after ${timeoutMs}ms", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if an exception is retryable
     * @param exception The exception to check
     * @return True if the exception is retryable
     */
    fun isRetryableException(exception: Exception): Boolean {
        return when {
            exception.message?.contains("network", ignoreCase = true) == true -> true
            exception.message?.contains("timeout", ignoreCase = true) == true -> true
            exception.message?.contains("connection", ignoreCase = true) == true -> true
            exception.message?.contains("unavailable", ignoreCase = true) == true -> true
            else -> false
        }
    }
} 