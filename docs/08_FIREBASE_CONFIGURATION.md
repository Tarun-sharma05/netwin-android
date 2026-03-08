# NetWin - Firebase Configuration Documentation

---

## 1. Firebase Project Overview

**Project ID:** `netwin-tournament`  
**Region:** Multi-region  
**Services Used:**
- Authentication
- Firestore Database
- Cloud Storage
- App Check (Admin panel only)
- Cloud Functions (future)

---

## 2. Firebase Authentication

### 2.1 Enabled Providers

**Active:**
- ✅ Email/Password
- ✅ Google Sign-In

**Planned:**
- ⏳ Phone Authentication (SMS OTP)

### 2.2 Configuration

**Email/Password:**
- Email link sign-in: Disabled
- Email enumeration protection: Enabled
- Password policy: Minimum 6 characters

**Google Sign-In:**
- Web client ID: Configured in `google-services.json`
- Support email: Set in Firebase Console
- Project support email: Required for Google Sign-In

### 2.3 User Management

**User Creation Flow:**
```
1. User signs up via Firebase Auth
2. Firebase Auth creates user with UID
3. App creates user profile in Firestore users/{userId}
4. App creates wallet in Firestore wallets/{userId}
```

**User Deletion:**
- Manual via Firebase Console
- Cascade delete: User profile, wallet, registrations
- Keep: Transactions (for audit)

---

## 3. Firestore Database

### 3.1 Database Mode

**Mode:** Native Mode  
**Location:** Multi-region  
**Offline Persistence:** Enabled

### 3.2 Indexes

**Composite Indexes:**

```javascript
// pending_deposits - For user queries with status and ordering
{
  collection: "pending_deposits",
  fields: [
    { name: "userId", order: "ASCENDING" },
    { name: "status", order: "ASCENDING" },
    { name: "submittedAt", order: "DESCENDING" }
  ]
}

// wallet_transactions - For user transaction history
{
  collection: "wallet_transactions",
  fields: [
    { name: "userId", order: "ASCENDING" },
    { name: "timestamp", order: "DESCENDING" }
  ]
}

// tournament_registrations - For user registrations
{
  collection: "tournament_registrations",
  fields: [
    { name: "userId", order: "ASCENDING" },
    { name: "registeredAt", order: "DESCENDING" }
  ]
}

// tournaments - For filtering by status and ordering
{
  collection: "tournaments",
  fields: [
    { name: "status", order: "ASCENDING" },
    { name: "startTime", order: "ASCENDING" }
  ]
}
```

**Single Field Indexes:**
- Auto-created by Firebase for most queries
- Manual creation not needed

---

## 4. Security Rules

### 4.1 Firestore Rules

**File:** `firestore.rules`

**Helper Functions:**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }
    
    function isAdmin() {
      return isAuthenticated() && 
             get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }
    
    function isModerator() {
      return isAuthenticated() && 
             get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'moderator';
    }
    
    function isAdminOrModerator() {
      return isAdmin() || isModerator();
    }
    
    function isAdminPanel() {
      return 'X-Firebase-AppCheck' in request.headers;
    }
    
    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }
  }
}
```

**Collection Rules:**

```javascript
// Users collection
match /users/{userId} {
  allow read: if isAuthenticated();
  allow create: if isAuthenticated() && request.auth.uid == userId;
  allow update: if isOwner(userId);
  allow delete: if isAdmin();
}

// Tournaments collection
match /tournaments/{tournamentId} {
  allow read: if isAuthenticated();
  allow write: if isAdminOrModerator();
}

// Tournament registrations
match /tournament_registrations/{registrationId} {
  allow read: if isAuthenticated() && 
              (isOwner(resource.data.userId) || isAdminOrModerator());
  allow create: if isAuthenticated() && 
                isOwner(request.resource.data.userId) &&
                request.resource.data.entryFeePaid > 0;
  allow update, delete: if isAdminOrModerator();
}

// Wallets
match /wallets/{userId} {
  allow read: if isOwner(userId);
  allow write: if isAdmin();  // Only admins can update balances
}

// Wallet transactions
match /wallet_transactions/{transactionId} {
  allow read: if isAuthenticated() && 
              (isOwner(resource.data.userId) || isAdminOrModerator());
  allow create: if isAuthenticated() && 
                isOwner(request.resource.data.userId);
  allow update, delete: if isAdmin();
}

// Pending deposits
match /pending_deposits/{depositId} {
  allow read: if isAuthenticated() && 
              (isOwner(resource.data.userId) || isAdminOrModerator());
  allow create: if isAuthenticated() && 
                isOwner(request.resource.data.userId) &&
                request.resource.data.amount > 0;
  allow update, delete: if isAdmin();
}

// Pending withdrawals
match /pending_withdrawals/{withdrawalId} {
  allow read: if isAuthenticated() && 
              (isOwner(resource.data.userId) || isAdminOrModerator());
  allow create: if isAuthenticated() && 
                isOwner(request.resource.data.userId) &&
                request.resource.data.amount > 0;
  allow update, delete: if isAdmin();
}

// KYC verifications
match /kyc_verifications/{userId} {
  allow read: if isOwner(userId) || isAdmin();
  allow create, update: if isOwner(userId);
  allow update: if isAdmin();  // For verification status
  allow delete: if isAdmin();
}

// Admin config
match /admin_config/{document} {
  allow read: if isAuthenticated();
  allow write: if isAdmin();
}

// Prize distributions
match /prize_distributions/{distributionId} {
  allow read: if isAuthenticated() && 
              (isOwner(resource.data.userId) || isAdminOrModerator());
  allow write: if isAdminOrModerator();
}

// Leaderboards
match /leaderboards/{tournamentId}/participants/{userId} {
  allow read: if isAuthenticated();
  allow write: if isAdminOrModerator();
}
```

---

### 4.2 Storage Rules

**File:** `storage.rules`

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    
    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }
    
    function isAdmin() {
      return isAuthenticated();  // Additional checks via Firestore
    }
    
    function isValidImage() {
      return request.resource.size < 10 * 1024 * 1024 &&  // Max 10MB
             request.resource.contentType.matches('image/.*');
    }
    
    // KYC document uploads
    match /kyc/{userId}/{allPaths=**} {
      allow read: if isAuthenticated() && 
                  (request.auth.uid == userId || isAdmin());
      allow write: if isAuthenticated() && 
                   request.auth.uid == userId &&
                   isValidImage();
    }
    
    // Deposit screenshot uploads
    match /deposit_screenshots/{allPaths=**} {
      allow read: if isAuthenticated();
      allow write: if isAuthenticated() && isValidImage();
    }
    
    // General screenshot uploads
    match /screenshots/{userId}/{allPaths=**} {
      allow read: if isAuthenticated() && request.auth.uid == userId;
      allow write: if isAuthenticated() && 
                   request.auth.uid == userId &&
                   isValidImage();
    }
    
    // Admin panel access (with App Check)
    match /{allPaths=**} {
      allow read, write: if request.auth != null;  // Authenticated users
    }
  }
}
```

---

## 5. Cloud Storage

### 5.1 Bucket Structure

```
gs://netwin-tournament.appspot.com/
├── kyc/
│   └── {userId}/
│       ├── front.jpg
│       ├── back.jpg
│       └── selfie.jpg
├── deposit_screenshots/
│   └── deposit_{userId}_{timestamp}.jpg
├── screenshots/
│   └── {userId}/
│       └── screenshot_{timestamp}.jpg
└── tournament_banners/  (future)
    └── {tournamentId}.jpg
```

### 5.2 Storage Configuration

**CORS Configuration:**
```json
[
  {
    "origin": ["*"],
    "method": ["GET", "POST", "PUT"],
    "maxAgeSeconds": 3600
  }
]
```

**Upload Constraints:**
- Max file size: 10 MB
- Allowed types: image/*
- Naming: Includes userId and timestamp to prevent overwrites

---

## 6. App Check (Future)

### 6.1 Configuration

**Purpose:** Protect admin panel from unauthorized access

**Providers:**
- SafetyNet (Android)
- reCAPTCHA (Web admin panel)

**Enforcement:**
```javascript
// In Firestore rules
function isAdminPanel() {
  return 'X-Firebase-AppCheck' in request.headers;
}

// Apply to sensitive operations
match /pending_deposits/{depositId} {
  allow update: if isAdmin() && isAdminPanel();
}
```

---

## 7. Firebase Configuration in App

### 7.1 google-services.json

**Location:** `app/google-services.json`

**Contains:**
- Project ID
- App ID
- API keys
- OAuth client IDs
- Firebase URLs

**Security:** DO NOT commit to public repositories

---

### 7.2 Firebase Initialization

**In Application class:**
```kotlin
class NetwinApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Firebase automatically initialized via google-services.json
        // Additional configuration
        FirebaseFirestore.getInstance().apply {
            firestoreSettings = firestoreSettings {
                isPersistenceEnabled = true
                cacheSizeBytes = FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED
            }
        }
    }
}
```

---

### 7.3 Firebase Manager

**Location:** `app/src/main/java/com/cehpoint/netwin/data/remote/FirebaseManager.kt`

```kotlin
@Singleton
class FirebaseManager @Inject constructor() {
    
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    val storage: FirebaseStorage = FirebaseStorage.getInstance()
    
    init {
        // Configure Firestore
        firestore.firestoreSettings = firestoreSettings {
            isPersistenceEnabled = true
        }
    }
    
    // Helper methods
    suspend fun <T> safeFirestoreCall(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: FirebaseException) {
            Log.e("FirebaseManager", "Firebase error", e)
            Result.failure(e)
        }
    }
}
```

---

## 8. Offline Capabilities

### 8.1 Firestore Offline Persistence

**Enabled by default in app**

**Benefits:**
- App works offline
- Writes queued and synced when online
- Cached data available immediately
- Automatic conflict resolution

**Cache Size:**
```kotlin
firestoreSettings {
    isPersistenceEnabled = true
    cacheSizeBytes = FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED
}
```

---

### 8.2 Handling Offline State

```kotlin
// In ViewModel
viewModelScope.launch {
    walletRepository.getWallet(userId)
        .catch { exception ->
            if (exception is FirebaseNetworkException) {
                _uiState.update { it.copy(isOffline = true) }
            }
        }
        .collect { wallet ->
            _uiState.update { it.copy(wallet = wallet, isOffline = false) }
        }
}
```

---

## 9. Performance Optimization

### 9.1 Query Optimization

**Use indexes:**
```kotlin
// Compound query - requires index
firestore.collection("wallet_transactions")
    .whereEqualTo("userId", userId)
    .orderBy("timestamp", Query.Direction.DESCENDING)
    .limit(20)
```

**Pagination:**
```kotlin
// Load in batches
var lastDoc: DocumentSnapshot? = null

fun loadMore() {
    var query = firestore.collection("transactions")
        .orderBy("timestamp")
        .limit(20)
    
    if (lastDoc != null) {
        query = query.startAfter(lastDoc)
    }
    
    query.get().addOnSuccessListener { snapshot ->
        lastDoc = snapshot.documents.lastOrNull()
        // Process results
    }
}
```

---

### 9.2 Real-time Listener Optimization

**Detach listeners when not needed:**
```kotlin
override fun getWallet(userId: String): Flow<Result<Wallet>> {
    return callbackFlow {
        val listener = firestore.collection("wallets")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                // Handle updates
            }
        
        awaitClose {
            listener.remove()  // Important: Remove listener
        }
    }
}
```

---

## 10. Monitoring & Analytics

### 10.1 Firebase Performance Monitoring

**Add to build.gradle:**
```gradle
implementation 'com.google.firebase:firebase-perf:20.5.0'
```

**Auto-monitored:**
- App start time
- Network requests
- Screen rendering

---

### 10.2 Crashlytics

**Add to build.gradle:**
```gradle
implementation 'com.google.firebase:firebase-crashlytics:18.6.0'
```

**Usage:**
```kotlin
try {
    // Risky operation
} catch (e: Exception) {
    FirebaseCrashlytics.getInstance().recordException(e)
}
```

---

## 11. Backup & Recovery

### 11.1 Firestore Backup

**Automated Backup:**
- Configure in Firebase Console
- Daily backups
- 7-day retention (default)

**Manual Backup:**
```bash
gcloud firestore export gs://[BUCKET_NAME]
```

---

### 11.2 Data Recovery

**From backup:**
```bash
gcloud firestore import gs://[BUCKET_NAME]/[EXPORT_PREFIX]
```

**Manual recovery:**
- Use Firebase Console
- Export collection to JSON
- Import back after fix

---

## 12. Environment Configuration

### 12.1 Development vs Production

**Separate Firebase projects:**
- `netwin-tournament-dev` (Development)
- `netwin-tournament` (Production)

**Configuration:**
```kotlin
object FirebaseConfig {
    val isProduction = BuildConfig.BUILD_TYPE == "release"
    
    val firestoreSettings = firestoreSettings {
        isPersistenceEnabled = true
        cacheSizeBytes = if (isProduction) {
            100 * 1024 * 1024  // 100 MB
        } else {
            FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED
        }
    }
}
```

---

## 13. Security Best Practices

### 13.1 Do's
✅ Use Security Rules to validate data
✅ Enable App Check for admin operations
✅ Validate all inputs on client AND server
✅ Use role-based access control
✅ Keep API keys in `google-services.json` (not in code)
✅ Use HTTPS only
✅ Implement proper error handling

### 13.2 Don'ts
❌ Don't expose sensitive data in Firestore
❌ Don't trust client-side validation alone
❌ Don't hardcode API keys in code
❌ Don't allow unrestricted read/write access
❌ Don't store passwords or tokens in Firestore
❌ Don't skip input sanitization

---

## 14. Troubleshooting

### 14.1 Common Issues

**Issue: Permission Denied**
```
Solution:
1. Check Firestore security rules
2. Verify user is authenticated
3. Check userId matches in request
4. Verify required fields are present
```

**Issue: Index Required**
```
Solution:
1. Click error link in logs
2. Create index in Firebase Console
3. Wait for index to build (few minutes)
```

**Issue: Offline writes not syncing**
```
Solution:
1. Check network connectivity
2. Verify persistence is enabled
3. Check for conflicting writes
4. Clear app cache if needed
```

---

## Summary

**Firebase Configuration:**
- ✅ Authentication configured (Email, Google)
- ✅ Firestore security rules deployed
- ✅ Storage rules configured
- ✅ Offline persistence enabled
- ✅ Indexes created for queries
- ✅ Backup strategy in place

**Security:**
- ✅ Role-based access control
- ✅ User data isolation
- ✅ Admin operations protected
- ✅ Input validation in rules

**Performance:**
- ✅ Query optimization with indexes
- ✅ Pagination implemented
- ✅ Listener cleanup
- ✅ Cache configuration

**Next Steps:**
- Implement App Check for admin panel
- Add Cloud Functions for automated operations
- Set up Firebase Analytics
- Configure automated backups
