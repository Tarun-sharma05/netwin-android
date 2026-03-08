# NetWin Android - Architecture Documentation

## Overview
NetWin is a PUBG/BGMI tournament application built using **Clean Architecture** principles with **MVVM** pattern, ensuring separation of concerns, testability, and maintainability.

## Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Dependency Injection**: Hilt (Dagger)
- **Backend**: Firebase (Firestore, Auth, Storage)
- **Navigation**: Jetpack Navigation Compose
- **Async**: Kotlin Coroutines + Flow
- **Image Loading**: Coil
- **Pagination**: Paging 3

---

## Architecture Diagram

```mermaid
graph TB
    subgraph "Presentation Layer"
        UI[Compose UI Screens]
        VM[ViewModels]
        UI --> VM
        
        subgraph "Key Screens"
            TournamentList[Tournament List Screen]
            TournamentDetails[Tournament Details]
            Registration[Multi-Step Registration]
            Wallet[Wallet Screen]
            MyTournaments[My Tournaments Screen]
            Profile[Profile Screen]
        end
    end

    subgraph "Domain Layer"
        Repo[Repository Interfaces]
        UseCases[Use Cases/Business Logic]
        Models[Domain Models]
        
        VM --> Repo
        UseCases --> Repo
    end

    subgraph "Data Layer"
        RepoImpl[Repository Implementations]
        RemoteDS[Remote Data Source]
        LocalDS[Local Data Source]
        
        Repo -.implements.-> RepoImpl
        RepoImpl --> RemoteDS
        RepoImpl --> LocalDS
        
        subgraph "Remote Sources"
            Firebase[Firebase Manager]
            Firestore[(Firestore DB)]
            FireAuth[Firebase Auth]
            FireStorage[Firebase Storage]
            
            Firebase --> Firestore
            Firebase --> FireAuth
            Firebase --> FireStorage
        end
        
        subgraph "Local Sources"
            DataStore[DataStore Manager]
            SharedPrefs[Shared Preferences]
        end
        
        RemoteDS --> Firebase
        LocalDS --> DataStore
        LocalDS --> SharedPrefs
    end

    subgraph "Dependency Injection"
        HiltApp[Hilt Application]
        Modules[DI Modules]
        
        Modules -.provides.-> VM
        Modules -.provides.-> RepoImpl
        Modules -.provides.-> Firebase
    end

    subgraph "Utils & Helpers"
        NetworkMonitor[Network State Monitor]
        KYCMonitor[KYC Monitor]
        OfflineQueue[Offline Queue Manager]
        CurrencyUtils[Currency Utils]
        TransactionUtils[Transaction Utils]
    end

    VM --> NetworkMonitor
    VM --> KYCMonitor
    VM --> OfflineQueue

    style UI fill:#e1f5ff
    style VM fill:#b3e5fc
    style Repo fill:#fff9c4
    style RepoImpl fill:#fff59d
    style Firebase fill:#ffccbc
    style Firestore fill:#ff8a65
```

---

## Layer Details

### 1. Presentation Layer
**Location**: `app/src/main/java/com/cehpoint/netwin/presentation/`

#### ViewModels
- **TournamentViewModel**: Tournament list, filters, pagination
- **WalletViewModel**: Wallet operations, deposits, withdrawals, transactions
- **UserViewModel**: User profile, authentication, KYC
- **RegistrationViewModel**: Multi-step tournament registration flow

#### UI Screens
- **TournamentListScreen**: Browse tournaments with filters and pagination
- **TournamentDetailsScreen**: View tournament details and rules
- **MultiStepRegistrationScreen**: 4-step registration process
  1. Tournament Review & KYC Check
  2. Payment
  3. In-Game Details
  4. Confirmation
- **WalletScreen**: Wallet management, deposits, withdrawals
- **MyTournamentsScreen**: User's registered tournaments with room credentials
- **ProfileScreen**: User profile and settings

### 2. Domain Layer
**Location**: `app/src/main/java/com/cehpoint/netwin/domain/`

#### Repository Interfaces
```kotlin
interface TournamentRepository
interface WalletRepository
interface UserRepository
```

#### Key Domain Models
- **Tournament**: Tournament data model
- **User**: User profile model
- **Transaction**: Financial transaction model
- **PendingDeposit**: Pending deposit request
- **WithdrawalRequest**: Withdrawal request model
- **KycStatus**: KYC verification status

### 3. Data Layer
**Location**: `app/src/main/java/com/cehpoint/netwin/data/`

#### Repository Implementations
- **TournamentRepositoryImpl**: Firebase-based tournament operations
- **WalletRepositoryImpl**: Wallet and transaction operations
- **UserRepositoryImpl**: User management and authentication

#### Firebase Collections
```
Firestore Structure:
├── users/
│   ├── {userId}/
│   │   ├── profile data
│   │   ├── kycStatus
│   │   └── wallet balance
├── tournaments/
│   ├── {tournamentId}/
│   │   ├── details
│   │   ├── rules
│   │   └── registrations/
├── transactions/
│   ├── {transactionId}/
│   │   ├── type (DEPOSIT/WITHDRAWAL)
│   │   ├── amount
│   │   ├── status
│   │   └── timestamp
├── pendingDeposits/
└── withdrawalRequests/
```

---

## Key Features Architecture

### Multi-Currency Support
```mermaid
graph LR
    CurrencyUtils[Currency Utils]
    INR[INR - India]
    NGN[NGN - Nigeria]
    
    CurrencyUtils --> INR
    CurrencyUtils --> NGN
    
    INR --> UPI[UPI Payment]
    NGN --> BankTransfer[Bank Transfer]
    NGN --> MobileMoney[Mobile Money]
```

**Supported Currencies**:
- **INR** (India): UPI payments
- **NGN** (Nigeria): Bank transfer, Mobile money, Card payments

### Multi-Step Registration Flow
```mermaid
stateDiagram-v2
    [*] --> Step1_Review
    Step1_Review --> Step2_Payment: KYC Valid
    Step1_Review --> [*]: Cancel
    Step2_Payment --> Step3_InGameDetails: Payment Success
    Step2_Payment --> Step1_Review: Back
    Step3_InGameDetails --> Step4_Confirmation: Details Submitted
    Step3_InGameDetails --> Step2_Payment: Back
    Step4_Confirmation --> [*]: Complete
```

**Steps**:
1. **Tournament Review & KYC**: Verify user eligibility and KYC status
2. **Payment**: Handle entry fee payment (multi-currency)
3. **In-Game Details**: Collect PUBG/BGMI player details
4. **Confirmation**: Show registration summary

### Wallet System
```mermaid
graph TB
    Wallet[Wallet System]
    
    Wallet --> Balance[Balance Management]
    Wallet --> Deposits[Deposit System]
    Wallet --> Withdrawals[Withdrawal System]
    Wallet --> Transactions[Transaction History]
    
    Balance --> Withdrawable[Withdrawable Balance]
    Balance --> Bonus[Bonus Balance]
    
    Deposits --> UPIDeposit[UPI Deposit - INR]
    Deposits --> BankDeposit[Bank Transfer - NGN]
    Deposits --> PendingReview[Pending Review]
    
    Withdrawals --> BankWithdraw[Bank Withdrawal]
    Withdrawals --> UPIWithdraw[UPI Withdrawal]
    Withdrawals --> RequestQueue[Withdrawal Queue]
    
    Transactions --> Paginated[Paginated Results]
    Transactions --> Filtered[Filtered by Type]
```

**Features**:
- Dual balance system (Withdrawable + Bonus)
- Multi-currency deposit/withdrawal
- UPI app relaunch for screenshot proof
- Offline queue for failed operations
- Transaction history with pagination

### Offline Support
```mermaid
graph LR
    Operation[User Operation]
    NetworkCheck{Network Available?}
    Execute[Execute Immediately]
    Queue[Add to Offline Queue]
    Sync[Sync When Online]
    
    Operation --> NetworkCheck
    NetworkCheck -->|Yes| Execute
    NetworkCheck -->|No| Queue
    Queue --> Sync
    Sync --> Execute
```

**Components**:
- **NetworkStateMonitor**: Real-time network status
- **OfflineQueueManager**: Queue operations for retry
- Automatic sync when connection restored

---

## Dependency Injection Structure

```mermaid
graph TB
    subgraph "Hilt Modules"
        AppModule[App Module]
        FirebaseModule[Firebase Module]
        RepositoryModule[Repository Module]
        TournamentModule[Tournament Module]
        WalletModule[Wallet Module]
        NetworkModule[Network Module]
    end
    
    AppModule -.-> Application
    FirebaseModule -.-> FirebaseManager
    RepositoryModule -.-> Repositories
    TournamentModule -.-> TournamentRepo
    WalletModule -.-> WalletRepo
    NetworkModule -.-> NetworkMonitor
    
    Repositories --> ViewModels
    FirebaseManager --> Repositories
```

**Key Modules**:
- **AppModule**: Application-level dependencies
- **FirebaseModule**: Firebase SDK configuration
- **RepositoryModule**: Base repository bindings
- **WalletModule**: Wallet-specific dependencies
- **TournamentModule**: Tournament-specific dependencies
- **NetworkModule**: Network monitoring utilities

---

## Navigation Structure

```mermaid
graph TB
    NavGraph[Nav Graph]
    
    NavGraph --> Auth[Auth Flow]
    NavGraph --> Main[Main Flow]
    
    Auth --> Login[Login Screen]
    Auth --> Signup[Signup Screen]
    
    Main --> BottomNav[Bottom Navigation]
    
    BottomNav --> Tournaments[Tournaments Tab]
    BottomNav --> MyTournamentsTab[My Tournaments Tab]
    BottomNav --> WalletTab[Wallet Tab]
    BottomNav --> ProfileTab[Profile Tab]
    
    Tournaments --> TournamentDetails
    TournamentDetails --> Registration
    Registration --> PaymentProof[Payment Proof Screen]
```

**Bottom Navigation** (4 tabs):
1. **Tournaments**: Browse and search tournaments
2. **My Tournaments**: Registered tournaments + room credentials
3. **Wallet**: Wallet management
4. **Profile**: User settings and KYC

---

## Data Flow Example: Tournament Registration

```mermaid
sequenceDiagram
    participant UI as Registration Screen
    participant VM as RegistrationViewModel
    participant TR as TournamentRepository
    participant WR as WalletRepository
    participant FB as Firebase
    
    UI->>VM: Start Registration
    VM->>WR: Check Wallet Balance
    WR->>FB: Query User Wallet
    FB-->>WR: Return Balance
    WR-->>VM: Balance Data
    
    VM->>TR: Check KYC Status
    TR->>FB: Query KYC Status
    FB-->>TR: KYC Verified
    TR-->>VM: KYC Valid
    
    VM-->>UI: Show Step 1 (Review)
    UI->>VM: Proceed to Payment
    VM-->>UI: Show Step 2 (Payment)
    
    UI->>VM: Submit Payment
    VM->>WR: Deduct Entry Fee
    WR->>FB: Create Transaction
    FB-->>WR: Success
    
    VM->>TR: Register for Tournament
    TR->>FB: Create Registration
    FB-->>TR: Registration ID
    TR-->>VM: Success
    VM-->>UI: Show Confirmation
```

---

## Testing Strategy

### Unit Tests
- ViewModels business logic
- Repository implementations
- Utility functions (Currency, Transaction)

### Integration Tests
- Firebase operations
- Multi-step registration flow
- Wallet transactions

### UI Tests
- Screen navigation
- Form validation
- Multi-step flows

---

## Performance Optimizations

1. **Pagination**: Paging 3 for large lists (tournaments, transactions)
2. **Lazy Loading**: Images loaded with Coil + caching
3. **State Management**: Immutable states with sealed classes
4. **Flow Optimization**: Cold flows for one-time operations, hot flows for real-time updates
5. **Compose Optimization**: Remember, derivedStateOf for expensive calculations

---

## Security Considerations

1. **Firebase Security Rules**: Server-side validation
2. **KYC Verification**: Required before withdrawals
3. **Transaction Verification**: Admin approval for deposits/withdrawals
4. **Secure Storage**: Sensitive data in encrypted DataStore
5. **API Keys**: Not hardcoded, stored securely

---

## Future Enhancements

1. **Push Notifications**: Firebase Cloud Messaging
2. **Analytics**: Firebase Analytics integration
3. **Crashlytics**: Error monitoring
4. **Remote Config**: Feature flags
5. **Performance Monitoring**: Firebase Performance
6. **In-App Updates**: Play Core library
7. **Biometric Auth**: Fingerprint/Face authentication

---

## Project Structure

```
app/
├── src/main/java/com/cehpoint/netwin/
│   ├── data/
│   │   ├── local/
│   │   │   └── DataStoreManager.kt
│   │   ├── model/
│   │   │   ├── Tournament.kt
│   │   │   ├── Transaction.kt
│   │   │   ├── User.kt
│   │   │   └── ...
│   │   ├── remote/
│   │   │   └── FirebaseManager.kt
│   │   └── repository/
│   │       ├── TournamentRepositoryImpl.kt
│   │       ├── WalletRepositoryImpl.kt
│   │       └── UserRepositoryImpl.kt
│   ├── domain/
│   │   └── repository/
│   │       ├── TournamentRepository.kt
│   │       ├── WalletRepository.kt
│   │       └── UserRepository.kt
│   ├── presentation/
│   │   ├── screens/
│   │   │   ├── tournament/
│   │   │   ├── wallet/
│   │   │   ├── profile/
│   │   │   └── registration/
│   │   └── viewmodels/
│   │       ├── TournamentViewModel.kt
│   │       ├── WalletViewModel.kt
│   │       └── UserViewModel.kt
│   ├── di/
│   │   ├── AppModule.kt
│   │   ├── FirebaseModule.kt
│   │   ├── RepositoryModule.kt
│   │   ├── TournamentModule.kt
│   │   └── WalletModule.kt
│   ├── navigation/
│   │   ├── NavGraph.kt
│   │   └── NavigationRoutes.kt
│   └── utils/
│       ├── NetworkStateMonitor.kt
│       ├── KYCMonitor.kt
│       ├── OfflineQueueManager.kt
│       ├── CurrencyUtils.kt
│       └── TransactionUtils.kt
```

---

## Design Patterns Used

1. **MVVM**: Separation of UI and business logic
2. **Repository Pattern**: Abstract data sources
3. **Dependency Injection**: Hilt for loose coupling
4. **Observer Pattern**: Kotlin Flows for reactive data
5. **Factory Pattern**: ViewModelFactory with Hilt
6. **Singleton Pattern**: Firebase manager, network monitor
7. **State Pattern**: Sealed classes for UI states
8. **Strategy Pattern**: Different payment methods per currency

---

## Key Decisions & Rationale

### Why Clean Architecture?
- **Separation of Concerns**: Each layer has a single responsibility
- **Testability**: Easy to unit test each layer independently
- **Maintainability**: Changes in one layer don't affect others
- **Scalability**: Easy to add new features

### Why Jetpack Compose?
- **Modern UI**: Declarative UI paradigm
- **Less Boilerplate**: No XML layouts
- **Type Safety**: Compile-time UI checks
- **Better Performance**: Smart recomposition

### Why Firebase?
- **Real-time Data**: Instant updates across devices
- **Scalability**: Managed infrastructure
- **Cost-Effective**: Pay-as-you-go pricing
- **Rich Features**: Auth, DB, Storage, Analytics in one platform

---

## Contact & Support
For questions or contributions, please refer to the project repository.

---

**Last Updated**: October 2025
**Version**: 1.0.0
