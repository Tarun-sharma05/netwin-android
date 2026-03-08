# NetWin - Firebase Collections Schema

Complete database schema with field definitions, security rules, and usage patterns.

---

## Collection Index

1. [users](#1-users) - User profiles
2. [tournaments](#2-tournaments) - Tournament definitions
3. [tournament_registrations](#3-tournament_registrations) - User registrations
4. [wallets](#4-wallets) - User wallet balances
5. [wallet_transactions](#5-wallet_transactions) - Transaction history
6. [pending_deposits](#6-pending_deposits) - Pending deposits
7. [pending_withdrawals](#7-pending_withdrawals) - Withdrawal requests
8. [kyc_verifications](#8-kyc_verifications) - KYC documents
9. [admin_config](#9-admin_config) - Admin settings
10. [prize_distributions](#10-prize_distributions) - Prize payouts
11. [leaderboards](#11-leaderboards) - Tournament rankings

---

## 1. users

**Path:** `users/{userId}`  
**Purpose:** Store user profile information

**Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| userId | string | ✅ | Firebase Auth UID |
| email | string | ❌ | User email address |
| username | string | ✅ | Display name |
| country | string | ✅ | "IN" or "NG" |
| currency | string | ✅ | "INR", "NGN", or "USD" |
| createdAt | timestamp | ✅ | Account creation time |
| updatedAt | timestamp | ✅ | Last update time |
| isActive | boolean | ✅ | Account status |
| role | string | ✅ | "user", "admin", or "moderator" |

**Example Document:**
```json
{
  "userId": "INyrJG4Fb7UQ4qwmcc2jkkArSXW2",
  "email": "tarundadhich05@gmail.com",
  "username": "gamerX",
  "country": "IN",
  "currency": "INR",
  "createdAt": "2025-09-15T10:30:00Z",
  "updatedAt": "2025-10-09T06:33:26Z",
  "isActive": true,
  "role": "user"
}
```

**Security Rules:**
```javascript
match /users/{userId} {
  allow read: if isAuthenticated();
  allow create: if isAuthenticated() && request.auth.uid == userId;
  allow update: if isAuthenticated() && request.auth.uid == userId;
}
```

**Used By:**
- `AuthViewModel` - Authentication state
- `ProfileScreen` - Display/edit profile
- `WalletViewModel` - Currency information

---

## 2. tournaments

**Path:** `tournaments/{tournamentId}`  
**Purpose:** Tournament definitions and details

**Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | string | ✅ | Tournament ID |
| title | string | ✅ | Tournament name |
| game | string | ✅ | "BGMI", "FREE FIRE", etc. |
| description | string | ✅ | Tournament description |
| bannerImageUrl | string | ✅ | Banner image URL |
| entryFee | number | ✅ | Entry fee amount |
| prizePool | number | ✅ | Total prize money |
| maxParticipants | number | ✅ | Maximum slots |
| currentParticipants | number | ✅ | Current registrations |
| startTime | timestamp | ✅ | Tournament start time |
| endTime | timestamp | ✅ | Tournament end time |
| registrationDeadline | timestamp | ✅ | Registration cutoff |
| status | string | ✅ | "upcoming", "live", "completed", "cancelled" |
| rules | string | ✅ | Tournament rules |
| gameMode | string | ✅ | "Solo", "Duo", "Squad" |
| map | string | ✅ | "Erangel", "Miramar", etc. |
| platform | string | ✅ | "Mobile", "PC", "Console" |
| roomId | string | ❌ | Game room ID (revealed after registration) |
| roomPassword | string | ❌ | Game room password |
| completedAt | timestamp | ❌ | Completion time |
| createdAt | timestamp | ✅ | Creation time |
| createdBy | string | ✅ | Admin user ID |

**Example Document:**
```json
{
  "id": "tour_123abc",
  "title": "BGMI Friday Night Cup",
  "game": "BGMI",
  "description": "Weekly competitive tournament with ₹5000 prize pool",
  "bannerImageUrl": "https://...",
  "entryFee": 50.0,
  "prizePool": 5000.0,
  "maxParticipants": 100,
  "currentParticipants": 45,
  "startTime": "2025-09-09T21:00:00Z",
  "endTime": "2025-09-09T22:30:00Z",
  "registrationDeadline": "2025-09-09T20:00:00Z",
  "status": "completed",
  "rules": "Standard BGMI rules apply. TPP mode, Erangel map.",
  "gameMode": "Squad",
  "map": "Erangel",
  "platform": "Mobile",
  "roomId": "12345678",
  "roomPassword": "netwin123",
  "completedAt": "2025-09-09T22:00:24Z",
  "createdAt": "2025-09-01T10:00:00Z",
  "createdBy": "admin_user_id"
}
```

**Security Rules:**
```javascript
match /tournaments/{tournamentId} {
  allow read: if isAuthenticated();
  allow write: if isAdmin() || isModerator();
}
```

**Used By:**
- `TournamentsScreen` - Browse tournaments
- `TournamentDetailsScreen` - View details
- `MyTournamentsScreen` - Registered tournaments
- `VictoryPassScreen` - Room ID/password display

---

## 3. tournament_registrations

**Path:** `tournament_registrations/{registrationId}`  
**Purpose:** Track user tournament registrations

**Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | string | ✅ | Registration ID |
| tournamentId | string | ✅ | Tournament reference |
| userId | string | ✅ | User reference |
| username | string | ✅ | User display name |
| teamName | string | ❌ | Team name (for squad mode) |
| playerIds | array | ❌ | Team member IDs |
| inGameName | string | ✅ | In-game character name |
| inGameId | string | ✅ | In-game user ID |
| phoneNumber | string | ✅ | Contact number |
| entryFeePaid | number | ✅ | Amount paid |
| status | string | ✅ | "pending", "confirmed", "cancelled" |
| registeredAt | timestamp | ✅ | Registration time |
| paymentStatus | string | ✅ | "paid", "pending", "refunded" |
| discordUsername | string | ❌ | Discord contact |

**Example Document:**
```json
{
  "id": "reg_xyz789",
  "tournamentId": "tour_123abc",
  "userId": "INyrJG4Fb7UQ4qwmcc2jkkArSXW2",
  "username": "gamerX",
  "teamName": "Elite Squad",
  "playerIds": ["player1", "player2", "player3", "player4"],
  "inGameName": "ProGamer99",
  "inGameId": "5123456789",
  "phoneNumber": "+919876543210",
  "entryFeePaid": 50.0,
  "status": "confirmed",
  "registeredAt": "2025-09-08T15:30:00Z",
  "paymentStatus": "paid",
  "discordUsername": "gamerX#1234"
}
```

**Security Rules:**
```javascript
match /tournament_registrations/{registrationId} {
  allow read: if isAuthenticated() && 
              (request.auth.uid == resource.data.userId || isAdmin());
  allow create: if isAuthenticated() && 
                request.auth.uid == request.resource.data.userId;
  allow update, delete: if isAdmin() || isModerator();
}
```

**Important Notes:**
- When user registers, **TWO operations** occur:
  1. Create `tournament_registrations` document
  2. Create `wallet_transactions` document (type: "tournament_entry")
- Entry fee is deducted from wallet balance
- Registration cannot be cancelled after tournament starts

**Used By:**
- `RegistrationFlowScreen` - Create registration
- `MyTournamentsScreen` - List user registrations
- `VictoryPassScreen` - Display after registration
- `TournamentViewModel` - Manage registration state

---

## 4. wallets

**Path:** `wallets/{userId}`  
**Purpose:** Store user wallet balance

**Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| userId | string | ✅ | User reference |
| balance | number | ✅ | Total balance (withdrawable + bonus) |
| withdrawableBalance | number | ✅ | Real money (can withdraw) |
| bonusBalance | number | ✅ | Bonus winnings |
| currency | string | ✅ | "INR", "NGN", "USD" |
| lastUpdated | timestamp | ✅ | Last balance update |

**Calculation:**
```javascript
balance = withdrawableBalance + bonusBalance
```

**Example Document:**
```json
{
  "userId": "INyrJG4Fb7UQ4qwmcc2jkkArSXW2",
  "balance": 1401.0,
  "withdrawableBalance": 600.0,
  "bonusBalance": 801.0,
  "currency": "INR",
  "lastUpdated": "2025-10-09T06:33:26Z"
}
```

**Security Rules:**
```javascript
match /wallets/{userId} {
  allow read: if isAuthenticated() && request.auth.uid == userId;
  allow write: if isAdmin();
}
```

**Used By:**
- `WalletScreen` - Display balances
- `WalletViewModel` - Manage wallet state
- Tournament entry validation
- Withdrawal eligibility check

**Balance Types:**
- **Withdrawable Balance:** User deposits, can be withdrawn anytime
- **Bonus Balance:** Tournament winnings/bonuses, may have withdrawal restrictions

---

## 5. wallet_transactions

**Path:** `wallet_transactions/{transactionId}`  
**Purpose:** Complete transaction history and audit trail

**Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | string | ✅ | Transaction ID |
| userId | string | ✅ | User reference |
| type | string | ✅ | Transaction type |
| amount | number | ✅ | Amount (negative for deductions) |
| status | string | ✅ | "pending", "completed", "failed" |
| description | string | ✅ | Human-readable description |
| currency | string | ✅ | "INR", "NGN", "USD" |
| timestamp | timestamp | ✅ | Transaction time |
| tournamentId | string | ❌ | Tournament reference (for entries) |
| referenceId | string | ❌ | External reference ID |

**Transaction Types:**
- `deposit` - Money added to wallet
- `withdrawal` - Money withdrawn
- `tournament_entry` - Entry fee deducted
- `prize_winning` - Prize money credited
- `bonus` - Bonus credited
- `refund` - Refund credited

**Example (Deposit):**
```json
{
  "id": "txn_deposit_001",
  "userId": "INyrJG4Fb7UQ4qwmcc2jkkArSXW2",
  "type": "deposit",
  "amount": 500.0,
  "status": "completed",
  "description": "UPI Deposit",
  "currency": "INR",
  "timestamp": "2025-10-05T14:20:00Z",
  "referenceId": "563064706380"
}
```

**Example (Tournament Entry):**
```json
{
  "id": "txn_entry_002",
  "userId": "INyrJG4Fb7UQ4qwmcc2jkkArSXW2",
  "type": "tournament_entry",
  "amount": -50.0,
  "status": "completed",
  "description": "Entry fee for BGMI Friday Night Cup",
  "currency": "INR",
  "timestamp": "2025-09-08T15:30:00Z",
  "tournamentId": "tour_123abc",
  "referenceId": "reg_xyz789"
}
```

**Security Rules:**
```javascript
match /wallet_transactions/{transactionId} {
  allow read: if isAuthenticated() && 
              (request.auth.uid == resource.data.userId || isAdmin());
  allow create: if isAuthenticated() && 
                request.auth.uid == request.resource.data.userId;
  allow update, delete: if isAdmin();
}
```

**Used By:**
- `WalletScreen` - Transaction history display
- `WalletViewModel` - Load and display transactions
- Created automatically for all wallet operations

---

## 6. pending_deposits

**Path:** `pending_deposits/{depositId}`  
**Purpose:** Store pending manual UPI deposit requests for admin approval

**Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | string | ✅ | Deposit ID |
| userId | string | ✅ | User reference |
| amount | number | ✅ | Deposit amount |
| currency | string | ✅ | "INR", "NGN", "USD" |
| upiTransactionId | string | ✅ | 12-digit UPI transaction ID |
| paymentScreenshot | string | ✅ | Firebase Storage URL |
| netwinUpiId | string | ✅ | Admin's UPI ID used |
| merchantDisplayName | string | ✅ | Merchant name |
| status | string | ✅ | "PENDING", "APPROVED", "REJECTED" |
| submittedAt | timestamp | ✅ | Submission time |
| processedAt | timestamp | ❌ | Processing time |
| processedBy | string | ❌ | Admin user ID |
| rejectionReason | string | ❌ | Reason for rejection |

**Example Document:**
```json
{
  "id": "BwQde3Ih5GX1WqkWn8jw",
  "userId": "INyrJG4Fb7UQ4qwmcc2jkkArSXW2",
  "amount": 1.0,
  "currency": "INR",
  "upiTransactionId": "563064706380",
  "paymentScreenshot": "https://firebasestorage.googleapis.com/.../deposit_screenshots/...",
  "netwinUpiId": "sakshipuria@okaxis",
  "merchantDisplayName": "Netwin Gaming",
  "status": "PENDING",
  "submittedAt": "2025-10-09T06:34:16Z",
  "processedAt": null,
  "processedBy": null,
  "rejectionReason": null
}
```

**Security Rules:**
```javascript
match /pending_deposits/{depositId} {
  allow read: if isAuthenticated() && 
              (request.auth.uid == resource.data.userId || isAdmin());
  allow create: if isAuthenticated() && 
                request.auth.uid == request.resource.data.userId &&
                request.resource.data.amount > 0;
  allow update, delete: if isAdmin();
}
```

**Approval Process:**
1. User submits deposit via `ManualUpiDepositScreen`
2. Admin reviews in Admin App
3. Admin approves/rejects
4. **If approved:**
   - Create `wallet_transactions` document (type: "deposit")
   - Update `wallets/{userId}` balance
   - Update status to "APPROVED"
5. **If rejected:**
   - Update status to "REJECTED"
   - Add rejection reason

**Used By:**
- `ManualUpiDepositScreen` - Submit deposit
- `WalletScreen` - Show pending deposits
- `WalletViewModel` - Load pending deposits
- Admin App - Approve/reject

---

## 7. pending_withdrawals

**Path:** `pending_withdrawals/{withdrawalId}`  
**Purpose:** Store withdrawal requests for admin processing

**Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | string | ✅ | Withdrawal ID |
| userId | string | ✅ | User reference |
| amount | number | ✅ | Withdrawal amount |
| currency | string | ✅ | "INR", "NGN", "USD" |
| bankAccountNumber | string | ✅ | Bank account number |
| ifscCode | string | ✅ | IFSC code (India) |
| accountHolderName | string | ✅ | Account holder name |
| bankName | string | ✅ | Bank name |
| status | string | ✅ | "PENDING", "APPROVED", "REJECTED", "PROCESSING" |
| requestedAt | timestamp | ✅ | Request time |
| processedAt | timestamp | ❌ | Processing time |
| processedBy | string | ❌ | Admin user ID |
| rejectionReason | string | ❌ | Reason for rejection |
| transactionReferenceId | string | ❌ | Bank transfer reference |

**Security Rules:**
```javascript
match /pending_withdrawals/{withdrawalId} {
  allow read: if isAuthenticated() && 
              (request.auth.uid == resource.data.userId || isAdmin());
  allow create: if isAuthenticated() && 
                request.auth.uid == request.resource.data.userId &&
                request.resource.data.amount > 0;
  allow update, delete: if isAdmin();
}
```

**Used By:**
- `WithdrawalScreen` - Submit withdrawal
- `WalletScreen` - Show pending withdrawals
- Admin App - Process withdrawals

---

## 8. kyc_verifications

**Path:** `kyc_verifications/{userId}`  
**Purpose:** Store KYC documents and verification status

**Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| userId | string | ✅ | User reference |
| status | string | ✅ | "NOT_SUBMITTED", "PENDING", "VERIFIED", "REJECTED" |
| documentType | string | ✅ | "aadhaar", "pan", "passport", "nin" |
| documentNumber | string | ✅ | Document number |
| fullName | string | ✅ | Full name on document |
| dateOfBirth | string | ✅ | Date of birth |
| address | string | ✅ | Residential address |
| frontImageUrl | string | ✅ | Front image URL |
| backImageUrl | string | ❌ | Back image URL |
| selfieImageUrl | string | ✅ | Selfie image URL |
| submittedAt | timestamp | ❌ | Submission time |
| verifiedAt | timestamp | ❌ | Verification time |
| verifiedBy | string | ❌ | Admin user ID |
| rejectionReason | string | ❌ | Reason for rejection |
| withdrawalLimit | number | ✅ | Daily withdrawal limit after verification |

**Security Rules:**
```javascript
match /kyc_verifications/{userId} {
  allow read: if isAuthenticated() && 
              (request.auth.uid == userId || isAdmin());
  allow create, update: if isAuthenticated() && request.auth.uid == userId;
  allow update: if isAdmin();  // For verification
}
```

**Used By:**
- `KycScreen` - Submit documents
- `WalletScreen` - Check KYC status for withdrawals
- `KYCMonitor` - Monitor status changes
- Admin App - Verify documents

---

## 9. admin_config

**Path:** `admin_config/wallet_config`  
**Purpose:** Store admin-controlled settings (UPI IDs, payment links)

**Document Structure:**
```json
{
  "INR": {
    "upiId": "sakshipuria@okaxis",
    "displayName": "Netwin Gaming",
    "isActive": true,
    "qrCodeEnabled": true,
    "minAmount": 10.0,
    "maxAmount": 100000.0,
    "updatedAt": "2025-01-07T12:30:00Z",
    "updatedBy": "admin-1"
  },
  "NGN": {
    "paymentLink": "https://paystack.shop/pay/netwin-pay",
    "displayName": "",
    "isActive": true,
    "updatedAt": "2024-12-18T08:15:00Z",
    "updatedBy": "admin-1"
  }
}
```

**Security Rules:**
```javascript
match /admin_config/{document} {
  allow read: if isAuthenticated();
  allow write: if isAdmin();
}
```

**Used By:**
- `AdminConfigRepository` - Load settings
- `WalletViewModel` - Load UPI settings
- `ManualUpiDepositScreen` - Display UPI ID and QR code

---

## 10. prize_distributions

**Path:** `prize_distributions/{distributionId}`  
**Purpose:** Track prize payouts to winners

**Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | string | ✅ | Distribution ID |
| tournamentId | string | ✅ | Tournament reference |
| userId | string | ✅ | Winner user ID |
| username | string | ✅ | Winner username |
| position | number | ✅ | 1st, 2nd, 3rd, etc. |
| prizeAmount | number | ✅ | Prize money |
| prizeType | string | ✅ | "cash", "bonus" |
| status | string | ✅ | "pending", "distributed" |
| distributedAt | timestamp | ❌ | Distribution time |
| createdAt | timestamp | ✅ | Creation time |

**Security Rules:**
```javascript
match /prize_distributions/{distributionId} {
  allow read: if isAuthenticated() && 
              (request.auth.uid == resource.data.userId || isAdmin());
  allow write: if isAdmin() || isModerator();
}
```

---

## 11. leaderboards

**Path:** `leaderboards/{tournamentId}/participants/{userId}`  
**Purpose:** Store tournament rankings and scores

**Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| userId | string | ✅ | Player reference |
| username | string | ✅ | Display name |
| inGameName | string | ✅ | In-game name |
| kills | number | ✅ | Total kills |
| points | number | ✅ | Total points |
| rank | number | ✅ | Current rank |
| updatedAt | timestamp | ✅ | Last update |

**Security Rules:**
```javascript
match /leaderboards/{tournamentId}/participants/{userId} {
  allow read: if isAuthenticated();
  allow write: if isAdmin() || isModerator();
}
```

---

## Summary

**Total Collections:** 11  
**Security Model:** Role-based (user, admin, moderator)  
**Relationships:** Denormalized for performance  
**Real-time Updates:** Enabled via Firestore listeners
