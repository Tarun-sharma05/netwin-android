package com.cehpoint.netwin

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NetWinApplication : Application() {

//        FirebaseApp.initializeApp(this)

        override fun onCreate() {
                super.onCreate()
                // Initialize Firebase
                FirebaseApp.initializeApp(this)
                
                // Initialize Firebase App Check with debug provider
                val firebaseAppCheck = FirebaseAppCheck.getInstance()
                firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
        }
} 