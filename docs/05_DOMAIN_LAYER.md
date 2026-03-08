# NetWin - Domain Layer Documentation

**Location:** `app/src/main/java/com/cehpoint/netwin/domain/`

---

## 1. Structure

```
domain/
├── repository/           # Repository interfaces
│   ├── WalletRepository.kt
│   ├── TournamentRepository.kt
│   ├── AuthRepository.kt
│   ├── AdminConfigRepository.kt
│   ├── KycRepository.kt
│   └── ...
└── usecase/             # Business logic (future)
    └── ...
```

---

## 2. Repository Interfaces

### 2.1 WalletRepository.kt

**Location:** `app/src/main/java/com/cehpoint/netwin/domain/repository/WalletRepository.kt`

**Purpose:** Define wallet operations contract

```kotlin
package com.cehpoint.netwin.domain.repository

import com.cehpoint.netwin.data.model.*
import kotlinx.coroutines.flow.Flow

interface WalletRepository {
    
    // User management
    fun getCurrentUserId(): String
    
    // Wallet operations
    fun getWallet(userId: String): Flow<Result<Wallet>>
    suspend fun getWithdrawableBalance(userId: String): Double
    suspend fun getBonusBalance(userId: String): Double
    
    // Deposit operations
    suspend fun submitManualDeposit(deposit: ManualUpiDeposit): Result<String>
    fun getPendingDeposits(userId: String): Flow<List<PendingDeposit>>
    
    // Withdrawal operations
    suspend fun createWithdrawalRequest(request: WithdrawalRequest): Result<String>
    fun getWithdrawalRequests(userId: String): Flow<List<WithdrawalRequest>>
    
    // Transaction operations
    fun getTransactions(userId: String): Flow<List<Transaction>>
    suspend fun getTransactionsPaginated(
        userId: String,
        limit: Int = 20,
        lastDocument: Any? = null
    ): PaginatedResult<Transaction>
}
```

**Implemented By:** `WalletRepositoryImpl`

**Used By:** `WalletViewModel`

---

### 2.2 TournamentRepository.kt

**Location:** `app/src/main/java/com/cehpoint/netwin/domain/repository/TournamentRepository.kt`

**Purpose:** Define tournament operations contract

```kotlin
package com.cehpoint.netwin.domain.repository

import com.cehpoint.netwin.data.model.Tournament
import com.cehpoint.netwin.data.model.TournamentRegistration
import kotlinx.coroutines.flow.Flow

interface TournamentRepository {
    
    // Browse tournaments
    fun getTournaments(): Flow<List<Tournament>>
    suspend fun getTournamentById(id: String): Tournament?
    fun getTournamentsByStatus(status: String): Flow<List<Tournament>>
    
    // Registration
    suspend fun registerForTournament(registration: TournamentRegistration): Result<String>
    fun getUserRegistrations(userId: String): Flow<List<TournamentRegistration>>
    suspend fun getRegistrationByTournamentId(
        userId: String, 
        tournamentId: String
    ): TournamentRegistration?
    
    // Validation
    suspend fun canUserRegister(userId: String, tournamentId: String): Boolean
    suspend fun hasUserRegistered(userId: String, tournamentId: String): Boolean
}
```

**Implemented By:** `TournamentRepositoryImpl`

**Used By:** `TournamentViewModel`

---

### 2.3 AuthRepository.kt

**Location:** `app/src/main/java/com/cehpoint/netwin/domain/repository/AuthRepository.kt`

**Purpose:** Define authentication operations contract

```kotlin
package com.cehpoint.netwin.domain.repository

import com.cehpoint.netwin.data.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    
    // Authentication
    suspend fun signUp(email: String, password: String): Result<String>
    suspend fun signIn(email: String, password: String): Result<String>
    suspend fun signOut()
    suspend fun resetPassword(email: String): Result<Unit>
    
    // User state
    fun getCurrentUser(): Flow<User?>
    fun isUserLoggedIn(): Boolean
    fun getCurrentUserId(): String
    
    // User profile
    suspend fun createUserProfile(user: User): Result<Unit>
    suspend fun updateUserProfile(user: User): Result<Unit>
    suspend fun getUserProfile(userId: String): User?
}
```

**Implemented By:** `AuthRepositoryImpl`

**Used By:** `AuthViewModel`

---

### 2.4 AdminConfigRepository.kt

**Location:** `app/src/main/java/com/cehpoint/netwin/domain/repository/AdminConfigRepository.kt`

**Purpose:** Define admin config operations contract

```kotlin
package com.cehpoint.netwin.domain.repository

import com.cehpoint.netwin.data.model.UpiSettings
import kotlinx.coroutines.flow.Flow

interface AdminConfigRepository {
    
    // UPI settings
    suspend fun getUpiSettings(currency: String): UpiSettings?
    fun observeUpiSettings(currency: String): Flow<UpiSettings?>
    
    // Payment links
    suspend fun getPaymentLink(currency: String): String?
    
    // Configuration
    suspend fun getMinDepositAmount(currency: String): Double
    suspend fun getMaxDepositAmount(currency: String): Double
    suspend fun getMinWithdrawalAmount(currency: String): Double
    suspend fun getMaxWithdrawalAmount(currency: String): Double
}
```

**Implemented By:** `AdminConfigRepositoryImpl`

**Used By:** `WalletViewModel`

---

### 2.5 KycRepository.kt

**Location:** `app/src/main/java/com/cehpoint/netwin/domain/repository/KycRepository.kt`

**Purpose:** Define KYC operations contract

```kotlin
package com.cehpoint.netwin.domain.repository

import com.cehpoint.netwin.data.model.KycVerification
import kotlinx.coroutines.flow.Flow

interface KycRepository {
    
    // KYC status
    fun getKycStatus(userId: String): Flow<KycVerification?>
    suspend fun hasVerifiedKyc(userId: String): Boolean
    
    // KYC submission
    suspend fun submitKycDocuments(kyc: KycVerification): Result<Unit>
    suspend fun uploadKycDocument(
        userId: String,
        documentType: String,
        imageUri: String
    ): String // Returns download URL
    
    // Withdrawal limits
    suspend fun getWithdrawalLimit(userId: String): Double
}
```

**Implemented By:** `KycRepositoryImpl`

**Used By:** `KycViewModel`, `WalletViewModel`

---

## 3. Business Rules (Currently in Repositories)

### 3.1 Wallet Business Rules

**Deposit Validation:**
```kotlin
// In WalletViewModel
fun validateDeposit(amount: Double, upiId: String, screenshot: String): Boolean {
    return amount > 0.0 &&
           upiId.length == 12 &&
           screenshot.isNotEmpty()
}
```

**Withdrawal Validation:**
```kotlin
// In WalletViewModel
fun canWithdraw(amount: Double, withdrawableBalance: Double, kycVerified: Boolean): Boolean {
    return kycVerified &&
           amount > 0.0 &&
           amount <= withdrawableBalance &&
           amount >= MIN_WITHDRAWAL_AMOUNT
}
```

**Balance Calculation:**
```kotlin
// In Wallet model
fun Wallet.isValid(): Boolean {
    return balance == withdrawableBalance + bonusBalance
}
```

---

### 3.2 Tournament Business Rules

**Registration Eligibility:**
```kotlin
// In TournamentViewModel
suspend fun canRegisterForTournament(
    tournament: Tournament,
    userBalance: Double
): Boolean {
    val now = System.currentTimeMillis()
    val deadline = tournament.registrationDeadline?.toDate()?.time ?: 0
    
    return tournament.status == "upcoming" &&
           tournament.currentParticipants < tournament.maxParticipants &&
           now < deadline &&
           userBalance >= tournament.entryFee &&
           !hasUserRegistered(tournament.id)
}
```

**Entry Fee Deduction:**
```kotlin
// When registering
suspend fun registerForTournament(tournament: Tournament) {
    // 1. Check balance
    val balance = walletRepository.getWithdrawableBalance(userId)
    if (balance < tournament.entryFee) {
        return Result.failure(InsufficientBalanceException())
    }
    
    // 2. Create registration
    val registration = TournamentRegistration(...)
    tournamentRepository.registerForTournament(registration)
    
    // 3. Create transaction (entry fee deduction)
    val transaction = Transaction(
        type = TransactionType.TOURNAMENT_ENTRY,
        amount = -tournament.entryFee,
        tournamentId = tournament.id
    )
    walletRepository.createTransaction(transaction)
}
```

---

### 3.3 KYC Business Rules

**Withdrawal Restrictions:**
```kotlin
fun canWithdraw(kycStatus: String): Boolean {
    return kycStatus.lowercase() == "verified"
}
```

**Document Requirements:**
```kotlin
// India
val indiaDocumentTypes = listOf("aadhaar", "pan", "passport")

// Nigeria
val nigeriaDocumentTypes = listOf("nin", "drivers_license", "voters_card", "passport")
```

---

## 4. Future: Use Case Layer

**Purpose:** Extract complex business logic from ViewModels

### 4.1 Example Use Cases

**RegisterForTournamentUseCase.kt**
```kotlin
class RegisterForTournamentUseCase @Inject constructor(
    private val tournamentRepository: TournamentRepository,
    private val walletRepository: WalletRepository
) {
    suspend operator fun invoke(
        userId: String,
        tournamentId: String,
        registration: TournamentRegistration
    ): Result<String> {
        return try {
            // 1. Validate tournament eligibility
            val tournament = tournamentRepository.getTournamentById(tournamentId)
                ?: return Result.failure(TournamentNotFoundException())
            
            // 2. Check balance
            val balance = walletRepository.getWithdrawableBalance(userId)
            if (balance < tournament.entryFee) {
                return Result.failure(InsufficientBalanceException())
            }
            
            // 3. Check if already registered
            if (tournamentRepository.hasUserRegistered(userId, tournamentId)) {
                return Result.failure(AlreadyRegisteredException())
            }
            
            // 4. Create registration
            val registrationId = tournamentRepository
                .registerForTournament(registration)
                .getOrThrow()
            
            // 5. Create transaction
            val transaction = Transaction(
                userId = userId,
                type = TransactionType.TOURNAMENT_ENTRY,
                amount = -tournament.entryFee,
                description = "Entry fee for ${tournament.title}",
                tournamentId = tournamentId,
                referenceId = registrationId
            )
            walletRepository.createTransaction(transaction)
            
            Result.success(registrationId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**ProcessDepositApprovalUseCase.kt** (Admin-side)
```kotlin
class ProcessDepositApprovalUseCase @Inject constructor(
    private val walletRepository: WalletRepository
) {
    suspend operator fun invoke(
        depositId: String,
        approved: Boolean,
        rejectionReason: String? = null
    ): Result<Unit> {
        return try {
            if (approved) {
                // 1. Update pending deposit status
                walletRepository.updateDepositStatus(depositId, "APPROVED")
                
                // 2. Get deposit details
                val deposit = walletRepository.getDepositById(depositId)
                    ?: return Result.failure(DepositNotFoundException())
                
                // 3. Create transaction
                val transaction = Transaction(
                    userId = deposit.userId,
                    type = TransactionType.DEPOSIT,
                    amount = deposit.amount,
                    description = "UPI Deposit",
                    referenceId = deposit.upiTransactionId
                )
                walletRepository.createTransaction(transaction)
                
                // 4. Update wallet balance
                walletRepository.updateBalance(
                    userId = deposit.userId,
                    amount = deposit.amount,
                    balanceType = "withdrawable"
                )
            } else {
                // Reject
                walletRepository.updateDepositStatus(
                    depositId, 
                    "REJECTED", 
                    rejectionReason
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## 5. Domain Models vs Data Models

**Current Approach:** Using Data Models directly in Domain layer

**Future Enhancement:** Separate domain models

**Example:**
```kotlin
// Domain Model (business logic focused)
data class TournamentRegistrationRequest(
    val tournamentId: String,
    val inGameDetails: InGameDetails,
    val contactInfo: ContactInfo,
    val teamInfo: TeamInfo?
)

// Data Model (Firebase structure focused)
data class TournamentRegistration(
    val id: String,
    val tournamentId: String,
    val inGameName: String,
    val inGameId: String,
    // ... flat structure
)

// Mapper
fun TournamentRegistrationRequest.toDataModel(): TournamentRegistration {
    return TournamentRegistration(
        tournamentId = this.tournamentId,
        inGameName = this.inGameDetails.name,
        inGameId = this.inGameDetails.id,
        // ...
    )
}
```

---

## 6. Dependency Flow

```
Presentation Layer (ViewModels)
    ↓ depends on
Domain Layer (Repository Interfaces)
    ↑ implemented by
Data Layer (Repository Implementations)
    ↓ uses
Remote Layer (Firebase)
```

**Key Principle:** Dependency Inversion
- High-level modules (ViewModels) don't depend on low-level modules (RepositoryImpl)
- Both depend on abstractions (Repository interfaces)
- Abstractions don't depend on details
- Details depend on abstractions

---

## 7. Benefits of Domain Layer

### 7.1 Testability
```kotlin
// Easy to mock repositories for testing
class WalletViewModelTest {
    private val mockRepository = mockk<WalletRepository>()
    private lateinit var viewModel: WalletViewModel
    
    @Before
    fun setup() {
        viewModel = WalletViewModel(mockRepository)
    }
    
    @Test
    fun `test deposit submission success`() {
        coEvery { mockRepository.submitManualDeposit(any()) } 
            returns Result.success("deposit123")
        
        viewModel.submitDeposit(...)
        
        assertTrue(viewModel.uiState.value.depositSubmitted)
    }
}
```

### 7.2 Flexibility
```kotlin
// Easy to swap implementations
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    fun provideWalletRepository(): WalletRepository {
        // Can return different implementations
        return if (BuildConfig.DEBUG) {
            MockWalletRepositoryImpl()  // For testing
        } else {
            WalletRepositoryImpl()  // Production
        }
    }
}
```

### 7.3 Separation of Concerns
- **Domain Layer:** What the app does (business rules)
- **Data Layer:** How data is stored/retrieved
- **Presentation Layer:** How it's displayed to users

---

## Summary

**Domain Layer Responsibilities:**
- ✅ Define repository interfaces
- ✅ Define business rules (in use cases - future)
- ✅ Provide abstractions for data access
- ✅ Enable testability and flexibility
- ✅ Enforce dependency inversion principle

**Currently:** Business logic is in ViewModels and Repository implementations

**Future:** Extract to Use Cases for better separation and reusability
