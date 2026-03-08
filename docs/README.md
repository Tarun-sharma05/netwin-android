# NetWin Tournament App - Documentation Index

**Version:** 1.0.0  
**Last Updated:** 11 Oct 2025  
**Status:** Pre-Production / Final Phase

---

## 📚 Documentation Overview

This directory contains comprehensive documentation for the NetWin Tournament Android application. The documentation is organized into focused sections for easy navigation.

---

## 📖 Documentation Files

### Core Documentation

1. **[01_PROJECT_OVERVIEW.md](./01_PROJECT_OVERVIEW.md)**
   - Project purpose and goals
   - Tech stack
   - Project structure
   - Color theme and branding
   - Related applications (web app, admin app)

2. **[02_ARCHITECTURE.md](./02_ARCHITECTURE.md)**
   - Clean Architecture implementation
   - MVVM pattern
   - Layer responsibilities
   - Data flow
   - Design principles

3. **[03_FIREBASE_COLLECTIONS.md](./03_FIREBASE_COLLECTIONS.md)**
   - Complete database schema
   - All Firestore collections with fields
   - Security rules for each collection
   - Example documents
   - Usage patterns

### Layer Documentation

4. **[04_DATA_LAYER.md](./04_DATA_LAYER.md)**
   - Data models
   - Repository implementations
   - Firebase integration
   - Data transformations
   - Error handling

5. **[05_DOMAIN_LAYER.md](./05_DOMAIN_LAYER.md)**
   - Repository interfaces
   - Business rules
   - Domain models
   - Use cases (future)

6. **[06_PRESENTATION_LAYER.md](./06_PRESENTATION_LAYER.md)**
   - ViewModels
   - Screens
   - UI components
   - State management
   - Theme and colors

### System Documentation

7. **[07_NAVIGATION.md](./07_NAVIGATION.md)**
   - Navigation graph
   - Route definitions
   - Navigation flows
   - Bottom navigation
   - Deep linking

8. **[08_FIREBASE_CONFIGURATION.md](./08_FIREBASE_CONFIGURATION.md)**
   - Firebase services setup
   - Security rules
   - Storage configuration
   - Offline capabilities
   - Performance optimization

9. **[09_REMAINING_TASKS.md](./09_REMAINING_TASKS.md)**
   - Tasks for final submission
   - UI/UX updates needed
   - Testing checklist
   - Deployment steps

10. **[10_DEPENDENCY_INJECTION.md](./10_DEPENDENCY_INJECTION.md)**
    - Hilt setup
    - Module definitions
    - Injection patterns
    - Testing with Hilt

### Process Documentation

11. **[DEPOSIT_APPROVAL_PROCESS.md](./DEPOSIT_APPROVAL_PROCESS.md)**
    - How deposit approval works
    - Admin workflow
    - Firebase changes after approval
    - Troubleshooting

---

## 🚀 Quick Start Guide

### For New Developers

**Step 1: Read Project Overview**
- Start with `01_PROJECT_OVERVIEW.md`
- Understand the app purpose
- Review tech stack

**Step 2: Understand Architecture**
- Read `02_ARCHITECTURE.md`
- Learn the layer structure
- Understand data flow

**Step 3: Explore Database**
- Review `03_FIREBASE_COLLECTIONS.md`
- Understand data models
- Review security rules

**Step 4: Dive into Code**
- Follow layer documentation (04-06)
- Review code in order: Data → Domain → Presentation

**Step 5: Setup Environment**
- Clone repository
- Add `google-services.json`
- Build and run

---

## 🏗️ Project Structure Quick Reference

```
netwin-android/
├── app/
│   ├── src/main/java/com/cehpoint/netwin/
│   │   ├── data/              # Data layer
│   │   │   ├── model/         # Data models
│   │   │   ├── repository/    # Repository implementations
│   │   │   └── remote/        # Firebase managers
│   │   ├── domain/            # Domain layer
│   │   │   └── repository/    # Repository interfaces
│   │   ├── presentation/      # Presentation layer
│   │   │   ├── viewmodels/    # ViewModels
│   │   │   ├── screens/       # Composable screens
│   │   │   ├── components/    # Reusable UI components
│   │   │   ├── navigation/    # Navigation setup
│   │   │   └── theme/         # UI theme
│   │   ├── di/                # Dependency injection modules
│   │   └── utils/             # Utility classes
├── docs/                      # This directory
├── firestore.rules            # Firestore security rules
├── storage.rules              # Storage security rules
└── google-services.json       # Firebase config (DO NOT COMMIT)
```

---

## 🎨 Key Features

### Completed Features ✅
- Manual UPI deposit system
- Tournament browsing and details
- Tournament registration (4-step flow)
- Victory Pass (Room ID/Password display)
- My Tournaments screen
- Wallet balance display
- Transaction history
- Pending deposits tracking
- KYC submission
- Profile management

### In Progress 🔄
- UI color theme alignment (cyan → gradient)
- KYC screen UI polish
- Registration flow UI improvements
- More screen feature review
- Profile screen enhancements

### Planned Features 📋
- Push notifications
- Cloud Functions for automation
- Leaderboard system
- Chat/messaging
- Referral system
- Multiple game support

---

## 🎯 Color Theme

**NetWin Gradient:**
- Purple: `#6C3AFF`
- Pink: `#FF3A8C`
- Cyan: `#3AFFDC`

**Background:**
- Primary: `#121212`
- Cards: `#1E1E2F`

**Status Colors:**
- Upcoming: `#FFC107` (Yellow)
- Live: `#F44336` (Red)
- Completed: `#4CAF50` (Green)

**Action Colors:**
- Success/Deposit: `#4CAF50` (Green)
- Error/Withdrawal: `#F44336` (Red)

---

## 🔐 Firebase Collections

### Primary Collections
1. **users** - User profiles
2. **tournaments** - Tournament definitions
3. **tournament_registrations** - User registrations
4. **wallets** - User wallet balances
5. **wallet_transactions** - Transaction history
6. **pending_deposits** - Pending UPI deposits
7. **pending_withdrawals** - Withdrawal requests
8. **kyc_verifications** - KYC documents
9. **admin_config** - Admin settings
10. **prize_distributions** - Prize payouts
11. **leaderboards** - Tournament rankings

See `03_FIREBASE_COLLECTIONS.md` for complete schema.

---

## 🔨 Development Workflow

### Making Changes

**1. Identify Layer:**
- Data change? → `data/`
- Business logic? → `domain/`
- UI change? → `presentation/`

**2. Follow Architecture:**
```
Data Model → Repository Interface → Repository Implementation → ViewModel → Screen
```

**3. Update Documentation:**
- Update relevant documentation file
- Add comments to code
- Update this README if needed

### Testing Changes

**1. Local Testing:**
- Build and run on device/emulator
- Test critical flows
- Check Firebase Console for data

**2. Integration Testing:**
- Test with real Firebase data
- Verify security rules
- Check error handling

**3. UI Testing:**
- Visual inspection
- Different screen sizes
- Light/dark mode (if applicable)

---

## 🐛 Troubleshooting

### Common Issues

**Build Fails:**
1. Clean project: Build → Clean Project
2. Invalidate caches: File → Invalidate Caches / Restart
3. Check `google-services.json` is present
4. Sync Gradle files

**Firebase Permission Denied:**
1. Check security rules in Firebase Console
2. Verify user is authenticated
3. Check userId in request matches auth
4. Review `08_FIREBASE_CONFIGURATION.md`

**Dependency Injection Issues:**
1. Verify `@HiltAndroidApp` on Application class
2. Check `@AndroidEntryPoint` on Activity
3. Rebuild project
4. Review `10_DEPENDENCY_INJECTION.md`

**Navigation Issues:**
1. Check route definitions in `NavigationRoutes.kt`
2. Verify NavGraph setup
3. Check navigation calls in screens
4. Review `07_NAVIGATION.md`

---

## 📱 Building for Release

### 1. Preparation
- [ ] All features tested
- [ ] UI matches design
- [ ] No debug logs in production
- [ ] Version number updated
- [ ] Release notes prepared

### 2. Build AAB
```bash
./gradlew clean
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

### 3. Build APK
```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

### 4. Sign
- Use Android Studio: Build → Generate Signed Bundle/APK
- Or use command line with keystore

See `09_REMAINING_TASKS.md` for complete checklist.

---

## 👥 Team Roles

### Developer
- Implement features
- Fix bugs
- Write tests
- Update documentation

### Admin (Firebase Console)
- Approve deposits
- Verify KYC
- Manage tournaments
- Monitor analytics

### QA
- Test features
- Report bugs
- Verify fixes
- User acceptance testing

---

## 📞 Support

### For Developers
- Read relevant documentation
- Check code comments
- Review Firebase Console
- Check logs (Logcat)

### For Admins
- `DEPOSIT_APPROVAL_PROCESS.md` - Deposit workflow
- Firebase Console - Data management
- Admin Web App - Transaction management

---

## 🔄 Documentation Updates

### When to Update

**After Feature Implementation:**
- Update relevant layer documentation
- Add to feature list in this README
- Update remaining tasks if applicable

**After Bug Fix:**
- Update troubleshooting section
- Add to common issues if recurring

**After Architecture Change:**
- Update architecture documentation
- Update affected layer docs
- Update diagrams if any

---

## 📈 Version History

### Version 1.0.0 (Current)
- Initial MVP release
- Manual UPI deposit system
- Tournament registration flow
- Wallet management
- KYC submission
- Firebase integration

### Upcoming Versions
- 1.1.0 - UI polish, color theme alignment
- 1.2.0 - Cloud Functions integration
- 1.3.0 - Push notifications
- 2.0.0 - Additional games support

---

## 🎓 Learning Resources

### Jetpack Compose
- [Official Compose Documentation](https://developer.android.com/jetpack/compose)
- [Compose Pathway](https://developer.android.com/courses/pathways/compose)

### Firebase
- [Firebase Documentation](https://firebase.google.com/docs)
- [Firestore Security Rules](https://firebase.google.com/docs/firestore/security/get-started)

### Hilt
- [Hilt Documentation](https://developer.android.com/training/dependency-injection/hilt-android)
- [Dependency Injection Principles](https://developer.android.com/training/dependency-injection)

### Clean Architecture
- [Android App Architecture](https://developer.android.com/topic/architecture)
- [Guide to app architecture](https://developer.android.com/topic/architecture/intro)

---

## 📝 Contributing Guidelines

### Code Style
- Follow Kotlin conventions
- Use meaningful variable names
- Add KDoc comments for public APIs
- Keep functions small and focused

### Commit Messages
```
feat: Add tournament filtering
fix: Resolve deposit submission issue
docs: Update Firebase configuration guide
refactor: Extract payment logic to use case
```

### Pull Request Process
1. Create feature branch
2. Implement changes
3. Test thoroughly
4. Update documentation
5. Submit PR with description

---

## ⚖️ License

Proprietary - NetWin Tournament Platform  
All rights reserved.

---

## 📧 Contact

**Project Lead:** [Name]  
**Email:** [Email]  
**Repository:** [Repository URL]

---

## 🙏 Acknowledgments

- Firebase for backend services
- Jetpack Compose for modern UI
- Hilt for dependency injection
- Coil for image loading
- Android community for libraries and support

---

**Last Updated:** 11 Oct 2025  
**Next Review:** After MVP submission

---

## 📍 Navigation Map

```
START HERE
    ↓
01_PROJECT_OVERVIEW.md (What is NetWin?)
    ↓
02_ARCHITECTURE.md (How is it built?)
    ↓
03_FIREBASE_COLLECTIONS.md (What data do we store?)
    ↓
04_DATA_LAYER.md → 05_DOMAIN_LAYER.md → 06_PRESENTATION_LAYER.md
(How do we access data? → What are the rules? → How do we show it?)
    ↓
07_NAVIGATION.md (How do users navigate?)
    ↓
08_FIREBASE_CONFIGURATION.md (How is Firebase configured?)
    ↓
10_DEPENDENCY_INJECTION.md (How do we wire things together?)
    ↓
09_REMAINING_TASKS.md (What's left to do?)
    ↓
DEPOSIT_APPROVAL_PROCESS.md (How do admins approve deposits?)
```

---

**Happy Coding! 🚀**
