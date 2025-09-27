package com.cehpoint.netwin.utils

import android.content.Context
import android.os.Environment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Persistent Logger that saves logs to a file for debugging
 * Logs are preserved even when the app is killed and restarted
 */
object Logger {
    private const val LOG_FILE_NAME = "netwin_debug_logs.txt"
    private const val MAX_LOG_ENTRIES = 1000
    
    /**
     * Log a message with timestamp and save to persistent storage
     */
    fun log(context: Context, tag: String, message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val logEntry = "[$timestamp] $tag: $message\n"
        
        // Also log to Android logcat
        android.util.Log.d(tag, message)
        
        // Save to persistent file
        saveLogToFile(context, logEntry)
    }
    
    /**
     * Save log entry to file
     */
    private fun saveLogToFile(context: Context, logEntry: String) {
        try {
            val logFile = getLogFile(context)
            
            // Read existing logs
            val existingLogs = if (logFile.exists()) {
                logFile.readLines()
            } else {
                emptyList()
            }
            
            // Add new log entry
            val updatedLogs = (existingLogs + logEntry)
                .takeLast(MAX_LOG_ENTRIES) // Keep only recent logs
            
            // Write back to file
            FileWriter(logFile, false).use { writer ->
                updatedLogs.forEach { line ->
                    writer.write(line)
                    writer.write("\n")
                }
            }
        } catch (e: IOException) {
            android.util.Log.e("Logger", "Failed to save log to file", e)
        }
    }
    
    /**
     * Get the log file
     */
    private fun getLogFile(context: Context): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val logFile = File(downloadsDir, LOG_FILE_NAME)
        
        // Create file if it doesn't exist
        if (!logFile.exists()) {
            try {
                logFile.createNewFile()
            } catch (e: IOException) {
                // Fallback to internal storage
                val internalFile = File(context.filesDir, LOG_FILE_NAME)
                if (!internalFile.exists()) {
                    internalFile.createNewFile()
                }
                return internalFile
            }
        }
        
        return logFile
    }
    
    /**
     * Read all logs from file
     */
    fun readLogs(context: Context): String {
        return try {
            val logFile = getLogFile(context)
            if (logFile.exists()) {
                logFile.readText()
            } else {
                "No logs found"
            }
        } catch (e: IOException) {
            "Error reading logs: ${e.message}"
        }
    }
    
    /**
     * Clear all logs
     */
    fun clearLogs(context: Context) {
        try {
            val logFile = getLogFile(context)
            if (logFile.exists()) {
                logFile.writeText("")
            }
        } catch (e: IOException) {
            android.util.Log.e("Logger", "Failed to clear logs", e)
        }
    }
}

/**
 * Composable helper for easy logging in Compose functions
 */
@Composable
fun rememberLogger(): (String, String) -> Unit {
    val context = LocalContext.current
    return remember { { tag: String, message: String ->
        Logger.log(context, tag, message)
    } }
}
