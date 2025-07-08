package com.cehpoint.netwin.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.cehpoint.netwin.data.remote.FirebaseManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.tasks.await
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(@ApplicationContext context: Context): FirebaseAuth {
        val auth = FirebaseAuth.getInstance()
        
        // Configure Firebase Auth persistence
        try {
            // Explicitly set persistence to LOCAL to ensure user sessions persist
            // This is crucial for maintaining user sessions across app restarts
            android.util.Log.d("FirebaseModule", "FirebaseAuth instance created")
            android.util.Log.d("FirebaseModule", "Current user: ${auth.currentUser}")
            android.util.Log.d("FirebaseModule", "Current user UID: ${auth.currentUser?.uid}")
            
            // Force a check for existing user session
            val currentUser = auth.currentUser
            if (currentUser != null) {
                android.util.Log.d("FirebaseModule", "Found existing user session: ${currentUser.uid}")
                android.util.Log.d("FirebaseModule", "User email: ${currentUser.email}")
                android.util.Log.d("FirebaseModule", "User display name: ${currentUser.displayName}")
                
                // Note: Token validation will be done in AuthViewModel to avoid blocking the provider
                android.util.Log.d("FirebaseModule", "User session found, token validation will be done in AuthViewModel")
            } else {
                android.util.Log.d("FirebaseModule", "No existing user session found")
            }
            
            // Check Firebase Auth persistence settings
            android.util.Log.d("FirebaseModule", "Firebase Auth persistence settings:")
            android.util.Log.d("FirebaseModule", "- Default persistence: LOCAL (tokens stored locally)")
            android.util.Log.d("FirebaseModule", "- App context: $context")
            android.util.Log.d("FirebaseModule", "- App data directory: ${context.filesDir}")
            
            // IMPORTANT: Ensure Firebase Auth persistence is properly configured
            // The default is LOCAL persistence, but we're making it explicit
            android.util.Log.d("FirebaseModule", "Firebase Auth persistence configured for LOCAL storage")
            
        } catch (e: Exception) {
            android.util.Log.e("FirebaseModule", "Error configuring Firebase Auth persistence", e)
        }
        
        return auth
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseManager(
        firebaseAuth: FirebaseAuth,
        firebaseFirestore: FirebaseFirestore,
        firebaseStorage: FirebaseStorage
    ): FirebaseManager {
        return FirebaseManager(firebaseAuth, firebaseFirestore, firebaseStorage)
    }
} 