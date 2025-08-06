package com.cehpoint.netwin

import android.app.Application
import android.util.Log
import com.google.firebase.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.cehpoint.netwin.data.local.DataStoreManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class NetWinApplication : Application() {

//        FirebaseApp.initializeApp(this)

        override fun onCreate() {
                super.onCreate()
                Log.d("NetWinApplication", "=== NetWinApplication onCreate STARTED ===")
                Log.d("NetWinApplication", "Process ID: ${android.os.Process.myPid()}")
                Log.d("NetWinApplication", "Thread ID: ${Thread.currentThread().id}")
                
                try {
                // Initialize Firebase
                FirebaseApp.initializeApp(this)
                        Log.d("NetWinApplication", "Firebase initialized")
                        
                        // Configure FirebaseAuth persistence
                        try {
                                val auth = FirebaseAuth.getInstance()
                                Log.d("NetWinApplication", "FirebaseAuth persistence configuration:")
                                Log.d("NetWinApplication", "  - Default persistence: ${auth.app.options.projectId}")
                                Log.d("NetWinApplication", "  - App context: ${auth.app.applicationContext}")
                                Log.d("NetWinApplication", "  - App data directory: ${auth.app.applicationContext.filesDir}")
                                Log.d("NetWinApplication", "FirebaseAuth persistence configured for LOCAL storage")
                                Log.d("NetWinApplication", "FirebaseAuth instance: $auth")
                                Log.d("NetWinApplication", "Current user after Firebase init: ${auth.currentUser}")
                                Log.d("NetWinApplication", "Current user UID after Firebase init: ${auth.currentUser?.uid}")

                                // Debug FirebaseAuth persistence
                                try {
                                        Log.d("NetWinApplication", "FirebaseAuth persistence debug:")
                                        Log.d("NetWinApplication", "  - App: ${auth.app.name}")
                                        Log.d("NetWinApplication", "  - Current user: ${auth.currentUser}")
                                        Log.d("NetWinApplication", "  - Current user UID: ${auth.currentUser?.uid}")
                                        Log.d("NetWinApplication", "  - Current user email: ${auth.currentUser?.email}")
                                        Log.d("NetWinApplication", "  - Current user display name: ${auth.currentUser?.displayName}")
                                        Log.d("NetWinApplication", "  - Current user is email verified: ${auth.currentUser?.isEmailVerified}")
                                        Log.d("NetWinApplication", "  - Current user metadata: ${auth.currentUser?.metadata}")
                                        Log.d("NetWinApplication", "  - Current user provider data: ${auth.currentUser?.providerData}")
                                        Log.d("NetWinApplication", "  - Current user tenant ID: ${auth.currentUser?.tenantId}")
                                        Log.d("NetWinApplication", "  - Current user is anonymous: ${auth.currentUser?.isAnonymous}")
                                } catch (e: Exception) {
                                        Log.e("NetWinApplication", "Error debugging FirebaseAuth persistence", e)
                                }

                                // MIUI Session Restoration Fallback
                                if (auth.currentUser == null) {
                                        Log.d("NetWinApplication", "FirebaseAuth has no current user, checking DataStore for session restoration")
                                        // Use runBlocking for immediate execution in this context
                                        runBlocking {
                                                try {
                                                        val dataStoreManager = DataStoreManager(this@NetWinApplication)
                                                        val storedUserId = dataStoreManager.getUserId()
                                                        val storedUserEmail = dataStoreManager.getUserEmail()
                                                        
                                                        Log.d("NetWinApplication", "DataStore session check:")
                                                        Log.d("NetWinApplication", "  - Stored userId: $storedUserId")
                                                        Log.d("NetWinApplication", "  - Stored userEmail: $storedUserEmail")
                                                        
                                                        if (storedUserId.isNotEmpty() && storedUserEmail.isNotEmpty()) {
                                                                Log.d("NetWinApplication", "WARNING: DataStore has user data but FirebaseAuth has no current user")
                                                                Log.d("NetWinApplication", "This indicates MIUI battery optimization may have cleared FirebaseAuth session")
                                                                Log.d("NetWinApplication", "Consider enabling 'No restrictions' in MIUI battery settings for NetWin")
                                                        } else {
                                                                Log.d("NetWinApplication", "DataStore also has no user data - this is a fresh install or complete logout")
                                                        }
                                                } catch (e: Exception) {
                                                        Log.e("NetWinApplication", "Error checking DataStore for session restoration", e)
                                                }
                                        }
                                } else {
                                        Log.d("NetWinApplication", "FirebaseAuth session restored successfully")
                                }

                        } catch (e: Exception) {
                                Log.e("NetWinApplication", "Error configuring FirebaseAuth persistence", e)
                        }
                        
                        // Initialize App Check
                        try {
                                val appCheck = FirebaseAppCheck.getInstance()
                                if (BuildConfig.DEBUG) {
                                        appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
                                        Log.d("NetWinApplication", "Firebase App Check initialized with DEBUG provider")
                                } else {
                                        appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
                                        Log.d("NetWinApplication", "Firebase App Check initialized with PLAY INTEGRITY provider")
                                }
                        } catch (e: Exception) {
                                Log.e("NetWinApplication", "Error initializing App Check", e)
                        }

                } catch (e: Exception) {
                        Log.e("NetWinApplication", "Error in NetWinApplication onCreate", e)
                }
                
                Log.d("NetWinApplication", "=== NetWinApplication onCreate COMPLETED ===")
        }
} 