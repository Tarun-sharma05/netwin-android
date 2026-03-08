# NetWin - Dependency Injection Documentation

**Framework:** Hilt (Dagger Hilt)  
**Location:** `app/src/main/java/com/cehpoint/netwin/di/`

---

## 1. Overview

### 1.1 Why Hilt?

**Benefits:**
- Compile-time dependency injection
- Automatic ViewModel injection
- Scoped dependencies
- Easy testing
- Android-optimized

**Alternative Considered:**
- Koin (runtime DI) - Rejected for better type safety

---

## 2. Module Structure

```
di/
├── AppModule.kt           # Application-level dependencies
├── RepositoryModule.kt    # Repository bindings
├── FirebaseModule.kt      # Firebase instances
├── ViewModelModule.kt     # ViewModel dependencies (if needed)
└── DataStoreModule.kt     # DataStore preferences
```

---

## 3. Application Setup

### 3.1 Application Class

**Location:** `app/src/main/java/com/cehpoint/netwin/NetwinApplication.kt`

```kotlin
package com.cehpoint.netwin

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NetwinApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase (if needed)
        // Additional app-level initialization
    }
}
```

**AndroidManifest.xml:**
```xml
<application
    android:name=".NetwinApplication"
    ...>
</application>
```

---

### 3.2 MainActivity

**Location:** `app/src/main/java/com/cehpoint/netwin/MainActivity.kt`

```kotlin
package com.cehpoint.netwin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            NetwinApp()
        }
    }
}
```

---

## 4. Modules

### 4.1 AppModule.kt

**Purpose:** Provide application-level dependencies

```kotlin
package com.cehpoint.netwin.di

import android.content.Context
import com.cehpoint.netwin.NetwinApplication
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideApplication(@ApplicationContext context: Context): NetwinApplication {
        return context as NetwinApplication
    }
    
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
}
```

---

### 4.2 FirebaseModule.kt

**Purpose:** Provide Firebase service instances

```kotlin
package com.cehpoint.netwin.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance().apply {
            firestoreSettings = firestoreSettings {
                isPersistenceEnabled = true
                cacheSizeBytes = FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED
            }
        }
    }
    
    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }
}
```

**Scope:** `SingletonComponent` - Lives for entire app lifetime

---

### 4.3 RepositoryModule.kt

**Purpose:** Bind repository implementations to interfaces

```kotlin
package com.cehpoint.netwin.di

import com.cehpoint.netwin.data.repository.*
import com.cehpoint.netwin.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindWalletRepository(
        walletRepositoryImpl: WalletRepositoryImpl
    ): WalletRepository
    
    @Binds
    @Singleton
    abstract fun bindTournamentRepository(
        tournamentRepositoryImpl: TournamentRepositoryImpl
    ): TournamentRepository
    
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
    
    @Binds
    @Singleton
    abstract fun bindAdminConfigRepository(
        adminConfigRepositoryImpl: AdminConfigRepositoryImpl
    ): AdminConfigRepository
    
    @Binds
    @Singleton
    abstract fun bindKycRepository(
        kycRepositoryImpl: KycRepositoryImpl
    ): KycRepository
}
```

**Note:** Uses `@Binds` instead of `@Provides` for better performance

---

### 4.4 DataStoreModule.kt

**Purpose:** Provide DataStore for preferences

```kotlin
package com.cehpoint.netwin.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "netwin_preferences"
)

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    
    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return context.dataStore
    }
}
```

---

## 5. Repository Injection

### 5.1 Repository Implementation

**WalletRepositoryImpl.kt:**
```kotlin
package com.cehpoint.netwin.data.repository

import com.cehpoint.netwin.domain.repository.WalletRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) : WalletRepository {
    
    // Implementation...
}
```

**Key Points:**
- `@Inject constructor` tells Hilt to inject dependencies
- `@Singleton` ensures single instance
- Dependencies injected automatically

---

### 5.2 How It Works

```
1. Hilt sees WalletRepositoryImpl has @Inject constructor
2. Looks for FirebaseFirestore, FirebaseStorage, FirebaseAuth
3. Finds them in FirebaseModule
4. Creates instances (or reuses singletons)
5. Injects into WalletRepositoryImpl
6. Binds to WalletRepository interface (from RepositoryModule)
7. Ready to inject into ViewModels
```

---

## 6. ViewModel Injection

### 6.1 ViewModel with Hilt

**WalletViewModel.kt:**
```kotlin
package com.cehpoint.netwin.presentation.viewmodels

import androidx.lifecycle.ViewModel
import com.cehpoint.netwin.domain.repository.WalletRepository
import com.cehpoint.netwin.domain.repository.AdminConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val adminConfigRepository: AdminConfigRepository
) : ViewModel() {
    
    // ViewModel implementation...
}
```

**Key Points:**
- `@HiltViewModel` annotation required
- `@Inject constructor` for dependency injection
- No need for ViewModelFactory

---

### 6.2 ViewModel Usage in Composables

```kotlin
@Composable
fun WalletScreen(
    viewModel: WalletViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // UI implementation...
}
```

**Magic:**
- `hiltViewModel()` automatically provides ViewModel
- Dependencies injected by Hilt
- ViewModel scoped to Navigation entry

---

## 7. Scopes

### 7.1 Available Scopes

```kotlin
@InstallIn(SingletonComponent::class)
// Lives for entire app lifetime
// Use for: Repositories, Firebase instances, DataStore

@InstallIn(ActivityComponent::class)
// Lives for Activity lifetime
// Use for: Activity-specific services

@InstallIn(ViewModelComponent::class)
// Lives for ViewModel lifetime
// Use for: ViewModel-specific dependencies

@InstallIn(ActivityRetainedComponent::class)
// Survives configuration changes
// Use for: Data that should survive rotation
```

---

### 7.2 Choosing the Right Scope

**SingletonComponent:**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    // Repositories - need to be singletons
}
```

**ViewModelComponent:**
```kotlin
@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {
    // ViewModel-specific dependencies
}
```

---

## 8. Qualifiers

### 8.1 When to Use Qualifiers

**Problem:** Multiple implementations of same type

**Example:**
```kotlin
// Two different Retrofit instances
@Provides
fun provideMainApi(): Retrofit { ... }

@Provides
fun provideAuthApi(): Retrofit { ... }

// How to distinguish?
```

**Solution: Qualifiers**
```kotlin
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthApi

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    @MainApi
    fun provideMainApi(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.netwin.com/")
            .build()
    }
    
    @Provides
    @Singleton
    @AuthApi
    fun provideAuthApi(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://auth.netwin.com/")
            .build()
    }
}

// Usage
class SomeRepository @Inject constructor(
    @MainApi private val mainApi: Retrofit,
    @AuthApi private val authApi: Retrofit
)
```

---

### 8.2 Built-in Qualifiers

**ApplicationContext:**
```kotlin
@Provides
fun provideDataStore(
    @ApplicationContext context: Context
): DataStore<Preferences> {
    return context.dataStore
}
```

**ActivityContext:**
```kotlin
@Provides
fun provideLayoutInflater(
    @ActivityContext context: Context
): LayoutInflater {
    return LayoutInflater.from(context)
}
```

---

## 9. Testing with Hilt

### 9.1 Test Setup

**build.gradle:**
```gradle
androidTestImplementation 'com.google.dagger:hilt-android-testing:2.48'
kaptAndroidTest 'com.google.dagger:hilt-android-compiler:2.48'
```

---

### 9.2 Unit Test with Hilt

```kotlin
@HiltAndroidTest
class WalletViewModelTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var walletRepository: WalletRepository
    
    private lateinit var viewModel: WalletViewModel
    
    @Before
    fun init() {
        hiltRule.inject()
        viewModel = WalletViewModel(walletRepository)
    }
    
    @Test
    fun testLoadWallet() {
        // Test implementation
    }
}
```

---

### 9.3 Replace Dependencies for Testing

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object TestRepositoryModule {
    
    @Provides
    @Singleton
    fun provideFakeWalletRepository(): WalletRepository {
        return FakeWalletRepository()
    }
}

@UninstallModules(RepositoryModule::class)
@HiltAndroidTest
class WalletScreenTest {
    // Uses FakeWalletRepository instead of real one
}
```

---

## 10. Common Patterns

### 10.1 Repository with Multiple Dependencies

```kotlin
@Singleton
class TournamentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val walletRepository: WalletRepository,  // Can inject other repos
    private val dataStore: DataStore<Preferences>
) : TournamentRepository {
    // Implementation
}
```

---

### 10.2 ViewModel with Use Case (Future)

```kotlin
@HiltViewModel
class TournamentViewModel @Inject constructor(
    private val registerForTournamentUseCase: RegisterForTournamentUseCase,
    private val getTournamentsUseCase: GetTournamentsUseCase
) : ViewModel() {
    // Implementation
}

// Use case also injected
class RegisterForTournamentUseCase @Inject constructor(
    private val tournamentRepository: TournamentRepository,
    private val walletRepository: WalletRepository
) {
    // Implementation
}
```

---

### 10.3 Assisted Injection (For runtime parameters)

**Not commonly needed, but useful for:**

```kotlin
class CustomRepository @AssistedInject constructor(
    @Assisted private val userId: String,
    private val firestore: FirebaseFirestore
) {
    @AssistedFactory
    interface Factory {
        fun create(userId: String): CustomRepository
    }
}

// Usage
class SomeViewModel @Inject constructor(
    private val customRepositoryFactory: CustomRepository.Factory
) : ViewModel() {
    
    fun loadDataForUser(userId: String) {
        val repo = customRepositoryFactory.create(userId)
        // Use repo
    }
}
```

---

## 11. Hilt Component Hierarchy

```
SingletonComponent (App lifetime)
    ↓
ActivityRetainedComponent (Survives config changes)
    ↓
ViewModelComponent (ViewModel lifetime)
    ↓
ActivityComponent (Activity lifetime)
    ↓
FragmentComponent (Fragment lifetime)
    ↓
ViewComponent (View lifetime)
```

**Rule:** Child components can access parent component dependencies

---

## 12. Performance Considerations

### 12.1 Lazy Injection

**Problem:** Expensive object creation

**Solution:**
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val expensiveService: Lazy<ExpensiveService>
) : ViewModel() {
    
    fun someFunction() {
        // Only created when first accessed
        expensiveService.get().doSomething()
    }
}
```

---

### 12.2 Provider Injection

**Problem:** Need new instance every time

**Solution:**
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repositoryProvider: Provider<Repository>
) : ViewModel() {
    
    fun someFunction() {
        // New instance each time
        val repo = repositoryProvider.get()
    }
}
```

---

## 13. Troubleshooting

### 13.1 Common Errors

**Error: Cannot find symbol**
```
Solution:
1. Make sure @HiltAndroidApp is on Application class
2. Rebuild project
3. Invalidate caches and restart
```

**Error: Missing binding**
```
Solution:
1. Check if @Provides or @Binds is present
2. Verify @InstallIn annotation
3. Check if interface is bound to implementation
```

**Error: Dependency cycle**
```
Solution:
1. Review dependency graph
2. Break cycle by using Provider or Lazy
3. Restructure dependencies
```

---

## 14. Best Practices

### 14.1 Do's
✅ Use `@Binds` instead of `@Provides` for interfaces
✅ Scope appropriately (don't overuse Singleton)
✅ Use qualifiers when multiple instances needed
✅ Inject dependencies in constructor
✅ Use Lazy or Provider when needed

### 14.2 Don'ts
❌ Don't inject Android classes directly (use Context)
❌ Don't create manual instances of @Inject classes
❌ Don't forget @HiltAndroidApp annotation
❌ Don't use field injection unless necessary
❌ Don't create circular dependencies

---

## 15. Migration from Manual DI

### 15.1 Before (Manual)

```kotlin
class WalletViewModel(
    private val repository: WalletRepository
) : ViewModel()

class WalletViewModelFactory(
    private val repository: WalletRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WalletViewModel(repository) as T
    }
}

// In Activity
val repository = WalletRepositoryImpl(firestore, storage, auth)
val factory = WalletViewModelFactory(repository)
val viewModel = ViewModelProvider(this, factory)[WalletViewModel::class.java]
```

---

### 15.2 After (Hilt)

```kotlin
@HiltViewModel
class WalletViewModel @Inject constructor(
    private val repository: WalletRepository
) : ViewModel()

// In Composable
val viewModel: WalletViewModel = hiltViewModel()
```

**Benefits:**
- No manual factory
- No manual instantiation
- Automatic lifecycle management
- Easy testing

---

## Summary

**Dependency Injection Setup:**
- ✅ Hilt configured
- ✅ Application class annotated
- ✅ Modules created for Firebase, Repositories
- ✅ ViewModels use @HiltViewModel
- ✅ Proper scoping applied
- ✅ Testing support configured

**Key Components:**
- `@HiltAndroidApp` - Application class
- `@AndroidEntryPoint` - Activity/Fragment
- `@HiltViewModel` - ViewModels
- `@Inject` - Constructor injection
- `@Provides` / `@Binds` - Dependency provision
- `@InstallIn` - Component scope

**Benefits:**
- Reduced boilerplate
- Type-safe injection
- Easy testing
- Automatic lifecycle management
- Compile-time validation
