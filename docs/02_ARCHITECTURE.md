# NetWin - Architecture Documentation

---

## 1. Clean Architecture Overview

```
┌─────────────────────────────────────────┐
│        Presentation Layer               │
│  (ViewModels, Screens, Components)      │
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│          Domain Layer                   │
│     (Use Cases, Repository Interfaces)  │
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│           Data Layer                    │
│  (RepositoryImpl, Firebase, Models)     │
└─────────────────────────────────────────┘
```

---

## 2. MVVM Pattern

**Data Flow:**
```kotlin
User Action → Screen → ViewModel → Repository → Firebase
Firebase → Repository → ViewModel (StateFlow) → Screen → UI Update
```

**Example Flow:**
```kotlin
// 1. User clicks "Submit Deposit" button
ManualUpiDepositScreen {
    Button(onClick = { walletViewModel.submitManualDeposit() })
}

// 2. ViewModel processes
WalletViewModel {
    fun submitManualDeposit() {
        walletRepository.submitManualDeposit(deposit)
            .onSuccess { /* Update UI state */ }
    }
}

// 3. Repository writes to Firebase
WalletRepositoryImpl {
    override suspend fun submitManualDeposit(deposit: ManualUpiDeposit) {
        firestore.collection("pending_deposits").add(deposit)
    }
}

// 4. UI updates via StateFlow
@Composable
fun ManualUpiDepositScreen(viewModel: WalletViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    if (uiState.depositSubmitted) { /* Show success */ }
}
```

---

## 3. Key Architectural Principles

### 3.1 Single Source of Truth
- ViewModels hold UI state in `StateFlow`
- All screens observe ViewModel state
- No local state duplication

### 3.2 Unidirectional Data Flow
- **Data flows down:** ViewModel → Screen
- **Events flow up:** Screen → ViewModel

### 3.3 Separation of Concerns
- **Presentation:** UI logic only
- **Domain:** Business rules
- **Data:** Data access and Firebase

### 3.4 Dependency Inversion
- Domain layer defines interfaces
- Data layer implements interfaces
- Presentation depends on abstractions, not implementations

---

## 4. Layer Responsibilities

### 4.1 Presentation Layer
**What it does:**
- Display UI
- Handle user interactions
- Observe ViewModel state
- Navigate between screens

**What it doesn't do:**
- Direct Firebase calls
- Business logic
- Data manipulation

**Key Classes:**
- Screens (Composable functions)
- ViewModels (manage UI state)
- UI Components (reusable)

### 4.2 Domain Layer
**What it does:**
- Define repository interfaces
- Business logic (use cases - future)
- Domain models

**What it doesn't do:**
- UI logic
- Firebase implementation

**Key Classes:**
- `WalletRepository` (interface)
- `TournamentRepository` (interface)
- `AuthRepository` (interface)

### 4.3 Data Layer
**What it does:**
- Implement repository interfaces
- Firebase operations
- Data model definitions
- Data transformations

**What it doesn't do:**
- UI logic
- Business rules

**Key Classes:**
- `WalletRepositoryImpl`
- `TournamentRepositoryImpl`
- Data models (`User`, `Tournament`, etc.)
- `FirebaseManager`

---

## 5. Dependency Injection (Hilt)

**Module Structure:**
```
di/
├── AppModule.kt          # App-level dependencies
├── RepositoryModule.kt   # Repository bindings
├── FirebaseModule.kt     # Firebase instances
└── ViewModelModule.kt    # ViewModel dependencies
```

**Example:**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideWalletRepository(
        firestore: FirebaseFirestore,
        storage: FirebaseStorage
    ): WalletRepository {
        return WalletRepositoryImpl(firestore, storage)
    }
}
```

**Benefits:**
- Easy testing (can swap implementations)
- Loose coupling
- Clear dependencies

---

## 6. State Management

### 6.1 ViewModel State Pattern

```kotlin
// UI State data class
data class WalletUiState(
    val balance: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val transactions: List<Transaction> = emptyList()
)

// ViewModel
class WalletViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()
    
    fun loadWalletData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Load data...
            _uiState.update { it.copy(isLoading = false, balance = 1000.0) }
        }
    }
}
```

### 6.2 Screen State Collection

```kotlin
@Composable
fun WalletScreen(viewModel: WalletViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    when {
        uiState.isLoading -> LoadingIndicator()
        uiState.error != null -> ErrorMessage(uiState.error)
        else -> WalletContent(uiState)
    }
}
```

---

## 7. Error Handling

**Pattern:**
```kotlin
// Repository returns Result
suspend fun loadData(): Result<Data> {
    return try {
        val data = firestore.collection("data").get()
        Result.success(data)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// ViewModel handles Result
fun loadData() {
    viewModelScope.launch {
        repository.loadData()
            .onSuccess { data -> _uiState.update { it.copy(data = data) } }
            .onFailure { error -> _uiState.update { it.copy(error = error.message) } }
    }
}
```

---

## 8. Firebase Integration

**Manager Pattern:**
```kotlin
class FirebaseManager {
    val firestore: FirebaseFirestore = Firebase.firestore
    val storage: FirebaseStorage = Firebase.storage
    val auth: FirebaseAuth = Firebase.auth
    
    // Helper methods
    suspend fun <T> safeFirestoreCall(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: FirebaseException) {
            Result.failure(e)
        }
    }
}
```

---

## 9. Testing Strategy

### 9.1 Unit Tests
- ViewModels (business logic)
- Utility functions
- Data transformations

### 9.2 Integration Tests
- Repository implementations
- Firebase operations (with emulator)

### 9.3 UI Tests
- Screen navigation
- User interactions
- State updates

---

## 10. Performance Considerations

### 10.1 LazyLoading
- Transaction history pagination
- Tournament list lazy loading
- Image loading with Coil

### 10.2 Caching
- Firestore offline persistence enabled
- ViewModel state survives configuration changes
- DataStore for preferences

### 10.3 Coroutines
- All async operations use coroutines
- Proper cancellation handling
- Structured concurrency

---

## 11. Security Architecture

### 11.1 Authentication
- Firebase Authentication
- Token-based security
- Auto token refresh

### 11.2 Authorization
- Firestore security rules
- Role-based access (admin, moderator, user)
- User-specific data access

### 11.3 Data Validation
- Client-side validation (ViewModel)
- Server-side validation (Firestore rules)
- Input sanitization

---

## 12. Scalability Considerations

### 12.1 Current Architecture Supports:
- Thousands of concurrent users
- Real-time updates
- Multi-currency support
- Multi-region deployment (future)

### 12.2 Future Enhancements:
- Cloud Functions for server-side logic
- BigQuery for analytics
- CDN for static assets
- Caching layer (Redis)
