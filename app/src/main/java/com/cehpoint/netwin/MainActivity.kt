package com.cehpoint.netwin

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.cehpoint.netwin.data.remote.FirebaseManager
import com.cehpoint.netwin.presentation.navigation.NavGraph
import com.cehpoint.netwin.ui.theme.NetWinTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cehpoint.netwin.presentation.viewmodels.AuthViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var firebaseManager: FirebaseManager

    private val viewModel by viewModels<AuthViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the splash screen transition
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            val shouldKeepSplash = !viewModel.isSplashShow.value
            Log.d("MainActivity", "Splash screen condition check: shouldKeepSplash = $shouldKeepSplash, isSplashShow = ${viewModel.isSplashShow.value}")
            shouldKeepSplash
        }
//        val splashScreen = installSplashScreen()
//        // TEMP: Keep splash for 2 seconds to test visibility
//        var keepSplash = true
//        splashScreen.setKeepOnScreenCondition { keepSplash }
//        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
//            keepSplash = false
//        }, 2000)
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
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    //color = MaterialTheme.colorScheme.background,
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPaddin ->
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

