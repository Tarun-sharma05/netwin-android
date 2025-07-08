package com.cehpoint.netwin

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.cehpoint.netwin.data.remote.FirebaseManager
import com.cehpoint.netwin.presentation.navigation.NavGraph
import com.cehpoint.netwin.ui.theme.NetWinTheme
import dagger.hilt.android.AndroidEntryPoint

import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var firebaseManager: FirebaseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("MainActivity", "=== MainActivity onCreate STARTED ===")
        Log.d("MainActivity", "MainActivity - savedInstanceState: $savedInstanceState")
        Log.d("MainActivity", "MainActivity - Process ID: ${android.os.Process.myPid()}")
        Log.d("MainActivity", "MainActivity - Thread ID: ${Thread.currentThread().id}")
        
        // Enable edge-to-edge
        enableEdgeToEdge()
        
        // Make the app draw behind the system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        super.onCreate(savedInstanceState)
        
        Log.d("MainActivity", "MainActivity - super.onCreate completed")
        Log.d("MainActivity", "MainActivity - firebaseManager: $firebaseManager")

        setContent {
            Log.d("MainActivity", "MainActivity - setContent started")
            NetWinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph(firebaseManager = firebaseManager)
                }
            }
            Log.d("MainActivity", "MainActivity - setContent completed")
        }
        
        Log.d("MainActivity", "=== MainActivity onCreate COMPLETED ===")
    }
    
    override fun onStart() {
        super.onStart()
        Log.d("MainActivity", "=== MainActivity onStart ===")
    }
    
    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "=== MainActivity onResume ===")
    }
    
    override fun onPause() {
        super.onPause()
        Log.d("MainActivity", "=== MainActivity onPause ===")
    }
    
    override fun onStop() {
        super.onStop()
        Log.d("MainActivity", "=== MainActivity onStop ===")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "=== MainActivity onDestroy ===")
    }
}

