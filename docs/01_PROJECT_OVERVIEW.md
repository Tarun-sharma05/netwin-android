# NetWin Tournament App - Project Overview

**Version:** 1.0.0  
**Last Updated:** 11 Oct 2025  
**Status:** Pre-Production / Final Phase  

---

## 1. App Purpose

**NetWin** is an esports tournament platform for PUBG/BGMI competitive gaming.

**Core Features:**
- Browse and discover tournaments
- Register for tournaments with entry fees
- Deposit money via UPI (India) or other payment methods
- Compete and win prizes
- Withdraw winnings to bank accounts

---

## 2. Target Audience

- **Primary:** Competitive mobile gamers (18-35 years)
- **Regions:** India (INR/UPI), Nigeria (NGN)
- **Platform:** Android mobile app

---

## 3. Tech Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Architecture:** Clean Architecture (MVVM + Repository Pattern)
- **Backend:** Firebase (Firestore, Storage, Authentication)
- **Dependency Injection:** Hilt
- **Navigation:** Jetpack Navigation Compose
- **Async:** Kotlin Coroutines + Flow
- **Image Loading:** Coil

---

## 4. Project Structure

```
netwin-android/
├── app/src/main/java/com/cehpoint/netwin/
│   ├── data/              # Data layer (models, repositories)
│   ├── domain/            # Domain layer (use cases, interfaces)
│   ├── presentation/      # UI layer (screens, viewmodels, components)
│   ├── di/                # Dependency injection modules
│   └── utils/             # Utility classes
├── firestore.rules        # Firestore security rules
├── storage.rules          # Storage security rules
└── docs/                  # Project documentation
```

---

## 5. Related Applications

### 5.1 User Web App
**Location:** `netwin-tournaments-app-web/netwin-tournament-app/`
- User-facing web interface
- Same features as mobile app
- Responsive design

### 5.2 Admin Web App
**Location:** `netwin-tournaments-admin/netwin-tournament-admin/`
- Admin dashboard
- Wallet transaction management
- Tournament management
- User KYC verification
- Analytics and reports

---

## 6. Color Theme

**Brand Identity:**
- **Primary Gradient:** #6C3AFF (Purple) → #FF3A8C (Pink) → #3AFFDC (Cyan)
- **Background:** #121212 (Dark), #1E1E2F (Cards)
- **Success:** #4CAF50 (Green)
- **Error:** #F44336 (Red)
- **Warning:** #FFC107 (Yellow)
- **Text:** #F8F8F8 (Primary), #Gray-400 (Secondary)

---

## 7. Documentation Structure

1. **01_PROJECT_OVERVIEW.md** - This file
2. **02_ARCHITECTURE.md** - System architecture
3. **03_FIREBASE_COLLECTIONS.md** - Complete database schema
4. **04_DATA_LAYER.md** - Data models and repositories
5. **05_DOMAIN_LAYER.md** - Business logic
6. **06_PRESENTATION_LAYER.md** - UI components and screens
7. **07_NAVIGATION.md** - App navigation structure
8. **08_FIREBASE_CONFIG.md** - Firebase setup and security
9. **09_REMAINING_TASKS.md** - Tasks for final submission
10. **10_DEPLOYMENT.md** - Deployment checklist
