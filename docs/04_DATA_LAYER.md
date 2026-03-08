# NetWin - Data Layer Documentation

**Location:** `app/src/main/java/com/cehpoint/netwin/data/`

---

## 1. Structure

```
data/
├── model/                    # Data models
│   ├── User.kt
│   ├── Tournament.kt
│   ├── TournamentRegistration.kt
│   ├── Wallet.kt
│   ├── Transaction.kt
│   ├── PendingDeposit.kt
│   ├── WithdrawalRequest.kt
│   ├── KycVerification.kt
│   ├── ManualUpiDeposit.kt
│   ├── PaginatedResult.kt
│   └── ...
├── repository/               # Repository implementations
│   ├── WalletRepositoryImpl.kt
│   ├── TournamentRepositoryImpl.kt
│   ├── AuthRepositoryImpl.kt
│   ├── AdminConfigRepositoryImpl.kt
│   ├── KycRepositoryImpl.kt
│   └── ...
└── remote/                   # Firebase managers
    └── FirebaseManager.kt
```

---

## 2. Data Models

### 2.1 User.kt

**Purpose:** User profile data model

```kotlin
package com.cehpoint.netwin.data.model

import com.google.firebase.Timestamp

data class User(
    val userId: String = "",
    val email: String? = null,
    val username: String = "",
    val country: String = "IN",
    val currency: String = "INR",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val isActive: Boolean = true,
    val role: String = "user"
)
```

**Mapping:**
- **From Firestore:** `users/{userId}` document
- **To UI:** ProfileScreen, WalletScreen (currency)
- **Used By:** AuthViewModel, ProfileViewModel

---

### 2.2 Tournament.kt

**Purpose:** Tournament data model

```kotlin
package com.cehpoint.netwin.data.model

import com.google.firebase.Timestamp

data class Tournament(
    val id: String = "",
    val title: String = "",
    val game: String = "",
    val description: String = "",
    val bannerImageUrl: String = "",
    val entryFee: Double = 0.0,
    val prizePool: Double = 0.0,
    val maxParticipants: Int = 100,
    val currentParticipants: Int = 0,
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val registrationDeadline: Timestamp? = null,
    val status: String = "upcoming",
    val rules: String = "",
    val gameMode: String = "",
    val map: String = "",
    val platform: String = "Mobile",
    val roomId: String? = null,
    val roomPassword: String? = null,
    val completedAt: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val createdBy: String = ""
)
```

**Mapping:**
- **From Firestore:** `tournaments/{tournamentId}` document
- **To UI:** TournamentsScreen, TournamentDetailsScreen, MyTournamentsScreen
- **Used By:** TournamentViewModel

**Status Values:**
- `"upcoming"` - Before start time
- `"live"` - Between start and end time
- `"completed"` - After end time
- `"cancelled"` - Cancelled by admin

---

### 2.3 TournamentRegistration.kt

**Purpose:** User tournament registration data

```kotlin
package com.cehpoint.netwin.data.model

import com.google.firebase.Timestamp

data class TournamentRegistration(
    val id: String = "",
    val tournamentId: String = "",
    val userId: String = "",
    val username: String = "",
    val teamName: String? = null,
    val playerIds: List<String> = emptyList(),
    val inGameName: String = "",
    val inGameId: String = "",
    val phoneNumber: String = "",
    val entryFeePaid: Double = 0.0,
    val status: String = "pending",
    val registeredAt: Timestamp? = null,
    val paymentStatus: String = "paid",
    val discordUsername: String? = null
)
```

**Mapping:**
- **From Firestore:** `tournament_registrations/{registrationId}` document
- **To UI:** RegistrationFlowScreen, MyTournamentsScreen, VictoryPassScreen
- **Used By:** TournamentViewModel

---

### 2.4 Wallet.kt

**Purpose:** User wallet balance data

```kotlin
package com.cehpoint.netwin.data.model

import com.google.firebase.Timestamp

data class Wallet(
    val userId: String = "",
    val balance: Double = 0.0,
    val withdrawableBalance: Double = 0.0,
    val bonusBalance: Double = 0.0,
    val currency: String = "INR",
    val lastUpdated: Timestamp? = null
) {
    // Validation
    fun isValid(): Boolean {
        return balance == withdrawableBalance + bonusBalance
    }
}
```

**Mapping:**
- **From Firestore:** `wallets/{userId}` document
- **To UI:** WalletScreen (balance display)
- **Used By:** WalletViewModel

---

### 2.5 Transaction.kt

**Purpose:** Wallet transaction data

```kotlin
package com.cehpoint.netwin.data.model

import com.google.firebase.Timestamp

data class Transaction(
    val id: String = "",
    val userId: String = "",
    val type: TransactionType = TransactionType.DEPOSIT,
    val amount: Double = 0.0,
    val status: String = "completed",
    val description: String = "",
    val currency: String = "INR",
    val timestamp: Timestamp? = null,
    val tournamentId: String? = null,
    val referenceId: String? = null
)

enum class TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TOURNAMENT_ENTRY,
    PRIZE_WINNING,
    BONUS,
    REFUND,
    BANK_TRANSFER_DEPOSIT,
    BANK_TRANSFER_WITHDRAWAL,
    CARD_PAYMENT,
    MOBILE_MONEY
}
```

**Mapping:**
- **From Firestore:** `wallet_transactions/{transactionId}` document
- **To UI:** WalletScreen (transaction history)
- **Used By:** WalletViewModel

---

### 2.6 PendingDeposit.kt

**Purpose:** Pending deposit request data

```kotlin
package com.cehpoint.netwin.data.model

import com.google.firebase.Timestamp

data class PendingDeposit(
    val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val currency: String = "INR",
    val upiTransactionId: String = "",
    val paymentScreenshot: String = "",
    val netwinUpiId: String = "",
    val merchantDisplayName: String = "",
    val status: String = "PENDING",
    val submittedAt: Timestamp? = null,
    val processedAt: Timestamp? = null,
    val processedBy: String? = null,
    val rejectionReason: String? = null
)
```

**Mapping:**
- **From Firestore:** `pending_deposits/{depositId}` document
- **To UI:** ManualUpiDepositScreen, WalletScreen (pending deposits)
- **Used By:** WalletViewModel

---

### 2.7 ManualUpiDeposit.kt

**Purpose:** Local state for UPI deposit submission

```kotlin
package com.cehpoint.netwin.data.model

data class ManualUpiDeposit(
    val amount: Double = 0.0,
    val upiTransactionId: String = "",
    val screenshotUri: String = "",
    val netwinUpiId: String = "",
    val merchantDisplayName: String = ""
)
```

**Mapping:**
- **To Firestore:** Converts to `PendingDeposit` before submission
- **From UI:** ManualUpiDepositScreen form inputs
- **Used By:** WalletViewModel

---

### 2.8 PaginatedResult.kt

**Purpose:** Generic pagination wrapper

```kotlin
package com.cehpoint.netwin.data.model

data class PaginatedResult<T>(
    val items: List<T> = emptyList(),
    val hasMore: Boolean = false,
    val lastDocument: Any? = null
)
```

**Usage:**
```kotlin
// Transaction history pagination
val result: PaginatedResult<Transaction> = 
    repository.getTransactionsPaginated(limit = 20)
```

---

## 3. Repository Implementations

### 3.1 WalletRepositoryImpl.kt

**Location:** `app/src/main/java/com/cehpoint/netwin/data/repository/WalletRepositoryImpl.kt`

**Purpose:** Implements wallet operations

**Key Methods:**

```kotlin
class WalletRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) : WalletRepository {

    // Get current user ID
    override fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: ""
    }

    // Get wallet data
    override fun getWallet(userId: String): Flow<Result<Wallet>> {
        return callbackFlow {
            val listener = firestore.collection("wallets")
                .document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    val wallet = snapshot?.toObject(Wallet::class.java)
                    trySend(Result.success(wallet ?: Wallet()))
                }
            awaitClose { listener.remove() }
        }
    }

    // Get withdrawable balance
    override suspend fun getWithdrawableBalance(userId: String): Double {
        return try {
            val doc = firestore.collection("wallets")
                .document(userId)
                .get()
                .await()
            doc.toObject(Wallet::class.java)?.withdrawableBalance ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    // Get bonus balance
    override suspend fun getBonusBalance(userId: String): Double {
        return try {
            val doc = firestore.collection("wallets")
                .document(userId)
                .get()
                .await()
            doc.toObject(Wallet::class.java)?.bonusBalance ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    // Submit manual UPI deposit
    override suspend fun submitManualDeposit(
        deposit: ManualUpiDeposit
    ): Result<String> {
        return try {
            val userId = getCurrentUserId()
            
            // 1. Upload screenshot
            val screenshotUrl = uploadDepositScreenshot(
                userId = userId,
                screenshotUri = deposit.screenshotUri
            )

            // 2. Create pending deposit document
            val pendingDeposit = PendingDeposit(
                userId = userId,
                amount = deposit.amount,
                currency = "INR",
                upiTransactionId = deposit.upiTransactionId,
                paymentScreenshot = screenshotUrl,
                netwinUpiId = deposit.netwinUpiId,
                merchantDisplayName = deposit.merchantDisplayName,
                status = "PENDING",
                submittedAt = Timestamp.now()
            )

            val docRef = firestore.collection("pending_deposits")
                .add(pendingDeposit)
                .await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Upload screenshot to Firebase Storage
    private suspend fun uploadDepositScreenshot(
        userId: String,
        screenshotUri: String
    ): String {
        val filename = "deposit_${userId}_${System.currentTimeMillis()}.jpg"
        val ref = storage.reference
            .child("deposit_screenshots")
            .child(filename)

        val uri = Uri.parse(screenshotUri)
        ref.putFile(uri).await()

        return ref.downloadUrl.await().toString()
    }

    // Get pending deposits
    override fun getPendingDeposits(userId: String): Flow<List<PendingDeposit>> {
        return callbackFlow {
            val listener = firestore.collection("pending_deposits")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "PENDING")
                .orderBy("submittedAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val deposits = snapshot?.toObjects(PendingDeposit::class.java) 
                        ?: emptyList()
                    trySend(deposits)
                }
            awaitClose { listener.remove() }
        }
    }

    // Get transactions
    override fun getTransactions(userId: String): Flow<List<Transaction>> {
        return callbackFlow {
            val listener = firestore.collection("wallet_transactions")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val transactions = snapshot?.toObjects(Transaction::class.java) 
                        ?: emptyList()
                    trySend(transactions)
                }
            awaitClose { listener.remove() }
        }
    }

    // Get transactions paginated
    override suspend fun getTransactionsPaginated(
        userId: String,
        limit: Int,
        lastDocument: Any?
    ): PaginatedResult<Transaction> {
        return try {
            var query = firestore.collection("wallet_transactions")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())

            if (lastDocument != null && lastDocument is DocumentSnapshot) {
                query = query.startAfter(lastDocument)
            }

            val snapshot = query.get().await()
            val transactions = snapshot.toObjects(Transaction::class.java)
            val hasMore = transactions.size == limit

            PaginatedResult(
                items = transactions,
                hasMore = hasMore,
                lastDocument = snapshot.documents.lastOrNull()
            )
        } catch (e: Exception) {
            PaginatedResult()
        }
    }

    // Create withdrawal request
    override suspend fun createWithdrawalRequest(
        request: WithdrawalRequest
    ): Result<String> {
        return try {
            val docRef = firestore.collection("pending_withdrawals")
                .add(request)
                .await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get withdrawal requests
    override fun getWithdrawalRequests(userId: String): Flow<List<WithdrawalRequest>> {
        return callbackFlow {
            val listener = firestore.collection("pending_withdrawals")
                .whereEqualTo("userId", userId)
                .orderBy("requestedAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val requests = snapshot?.toObjects(WithdrawalRequest::class.java) 
                        ?: emptyList()
                    trySend(requests)
                }
            awaitClose { listener.remove() }
        }
    }
}
```

**Data Flow:**
```
User Action (ManualUpiDepositScreen)
    ↓
WalletViewModel.submitManualDeposit()
    ↓
WalletRepository.submitManualDeposit()
    ↓
WalletRepositoryImpl:
  1. Upload screenshot → Storage
  2. Get download URL
  3. Create PendingDeposit document → Firestore
  4. Return deposit ID
    ↓
ViewModel updates UI state
    ↓
Screen shows success message
```

---

### 3.2 TournamentRepositoryImpl.kt

**Location:** `app/src/main/java/com/cehpoint/netwin/data/repository/TournamentRepositoryImpl.kt`

**Purpose:** Implements tournament operations

**Key Methods:**

```kotlin
class TournamentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : TournamentRepository {

    // Get all tournaments
    override fun getTournaments(): Flow<List<Tournament>> {
        return callbackFlow {
            val listener = firestore.collection("tournaments")
                .orderBy("startTime", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val tournaments = snapshot?.toObjects(Tournament::class.java) 
                        ?: emptyList()
                    trySend(tournaments)
                }
            awaitClose { listener.remove() }
        }
    }

    // Get tournament by ID
    override suspend fun getTournamentById(id: String): Tournament? {
        return try {
            firestore.collection("tournaments")
                .document(id)
                .get()
                .await()
                .toObject(Tournament::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Register for tournament
    override suspend fun registerForTournament(
        registration: TournamentRegistration
    ): Result<String> {
        return try {
            // Create registration document
            val docRef = firestore.collection("tournament_registrations")
                .add(registration)
                .await()

            // TODO: Create wallet transaction for entry fee
            // TODO: Update tournament currentParticipants count

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get user's registrations
    override fun getUserRegistrations(userId: String): Flow<List<TournamentRegistration>> {
        return callbackFlow {
            val listener = firestore.collection("tournament_registrations")
                .whereEqualTo("userId", userId)
                .orderBy("registeredAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val registrations = snapshot?.toObjects(TournamentRegistration::class.java) 
                        ?: emptyList()
                    trySend(registrations)
                }
            awaitClose { listener.remove() }
        }
    }
}
```

---

### 3.3 AdminConfigRepositoryImpl.kt

**Location:** `app/src/main/java/com/cehpoint/netwin/data/repository/AdminConfigRepositoryImpl.kt`

**Purpose:** Load admin configuration (UPI settings, etc.)

**Key Methods:**

```kotlin
class AdminConfigRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : AdminConfigRepository {

    // Get UPI settings for currency
    override suspend fun getUpiSettings(currency: String): UpiSettings? {
        return try {
            val doc = firestore.collection("admin_config")
                .document("wallet_config")
                .get()
                .await()

            val data = doc.get(currency) as? Map<*, *> ?: return null

            UpiSettings(
                upiId = data["upiId"] as? String ?: "",
                displayName = data["displayName"] as? String ?: "",
                isActive = data["isActive"] as? Boolean ?: false,
                qrCodeEnabled = data["qrCodeEnabled"] as? Boolean ?: true,
                minAmount = (data["minAmount"] as? Number)?.toDouble() ?: 10.0,
                maxAmount = (data["maxAmount"] as? Number)?.toDouble() ?: 100000.0
            )
        } catch (e: Exception) {
            null
        }
    }
}
```

**Used By:**
- WalletViewModel - Load UPI ID
- ManualUpiDepositScreen - Display UPI settings

---

## 4. Remote Layer

### 4.1 FirebaseManager.kt

**Location:** `app/src/main/java/com/cehpoint/netwin/data/remote/FirebaseManager.kt`

**Purpose:** Centralized Firebase instance management

```kotlin
package com.cehpoint.netwin.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseManager @Inject constructor() {
    
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    val storage: FirebaseStorage = FirebaseStorage.getInstance()
    
    init {
        // Enable offline persistence
        firestore.firestoreSettings = firestoreSettings {
            isPersistenceEnabled = true
        }
    }
}
```

---

## 5. Data Transformation

### From Firestore to UI

```kotlin
// 1. Firestore Document
val doc = firestore.collection("wallets").document(userId).get()

// 2. Convert to Data Model
val wallet = doc.toObject(Wallet::class.java)

// 3. ViewModel processes
_uiState.update { it.copy(balance = wallet.balance) }

// 4. UI displays
Text("₹${balance.formatCurrency()}")
```

### From UI to Firestore

```kotlin
// 1. UI Input
val amount = amountInput.toDoubleOrNull() ?: 0.0

// 2. Create Data Model
val deposit = ManualUpiDeposit(
    amount = amount,
    upiTransactionId = upiIdInput,
    screenshotUri = selectedImageUri
)

// 3. Repository transforms
val pendingDeposit = PendingDeposit(
    userId = getCurrentUserId(),
    amount = deposit.amount,
    ...
)

// 4. Write to Firestore
firestore.collection("pending_deposits").add(pendingDeposit)
```

---

## 6. Error Handling

**Pattern:**
```kotlin
suspend fun fetchData(): Result<Data> {
    return try {
        val data = firestore.collection("data").get().await()
        Result.success(data)
    } catch (e: FirebaseException) {
        Log.e(TAG, "Firebase error", e)
        Result.failure(e)
    } catch (e: Exception) {
        Log.e(TAG, "Unexpected error", e)
        Result.failure(e)
    }
}
```

---

## 7. Logging

**Debug Logging:**
```kotlin
private val TAG = "WalletRepositoryImpl"

Log.d(TAG, "Screenshot uploaded successfully: $url")
Log.d(TAG, "📝 Attempting to write deposit:")
Log.d(TAG, "  Document ID: $depositId")
Log.d(TAG, "  userId: $userId")
Log.d(TAG, "  amount: $amount")
```

**Production:** All debug logs automatically removed by ProGuard

---

## 8. Performance Optimizations

### 8.1 Offline Persistence
```kotlin
// Enabled in FirebaseManager
firestore.firestoreSettings = firestoreSettings {
    isPersistenceEnabled = true
}
```

### 8.2 Pagination
```kotlin
// Load transactions in batches
val result = repository.getTransactionsPaginated(
    userId = userId,
    limit = 20,
    lastDocument = lastDoc
)
```

### 8.3 Real-time Listeners
```kotlin
// Use Flow for real-time updates
override fun getWallet(userId: String): Flow<Result<Wallet>> {
    return callbackFlow {
        val listener = firestore.collection("wallets")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                // Handle updates
            }
        awaitClose { listener.remove() }
    }
}
```

---

## Summary

**Data Layer Responsibilities:**
- ✅ Define data models
- ✅ Implement repository interfaces
- ✅ Handle Firebase operations
- ✅ Transform data between layers
- ✅ Manage error handling
- ✅ Provide caching/offline support
