# NetWin - Remaining Tasks for Final Submission

**Target:** Tonight (11 Oct 2025)  
**Goal:** Complete MVP for client submission (AAB/APK)

---

## Task Checklist

### ✅ Completed
- [x] Manual UPI deposit system
- [x] Deposit approval workflow (via Admin App)
- [x] Firebase security rules
- [x] Tournament browsing
- [x] Tournament details
- [x] Tournament registration flow
- [x] Victory Pass component
- [x] My Tournaments screen
- [x] Wallet balance display
- [x] Transaction history

---

## 🔴 HIGH PRIORITY - Tonight's Tasks

### 1. KYC Screen UI/UX Update

**Current State:**
- Basic form with input fields
- Functional but doesn't match color theme
- No gradient elements

**Required Changes:**

**A. Color Theme Alignment:**
```kotlin
// Current (Cyan-based)
Color(0xFF00BCD4)  // Old cyan

// Target (Gradient theme)
val netwinGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF6C3AFF),  // Purple
        Color(0xFFFF3A8C),  // Pink
        Color(0xFF3AFFDC)   // Cyan
    )
)
```

**B. Component Updates:**
- Replace solid cyan with gradient borders
- Update button styles to gradient background
- Add gradient indicators for upload status
- Modernize document upload cards

**C. Research & Analysis:**
- **India KYC:**
  - Current: Aadhaar, PAN, Passport
  - Keep existing documents ✅
  
- **Nigeria KYC:**
  - Current: NIN (National Identity Number)
  - Research additional documents:
    - Driver's License
    - Voter's Card
    - International Passport
  - **Action:** Add dropdown for Nigerian document types

**Files to Update:**
- `app/src/main/java/com/cehpoint/netwin/presentation/screens/KycScreen.kt`
- Add gradient theme utilities if needed

**Reference:**
- Web app KYC flow (analyze design patterns)
- Existing gradient implementations in `VictoryPassScreen.kt`

---

### 2. More Screen & Profile Screen UI Update

**Current State:**
- List-based layout
- Basic styling
- Some features may be unused

**Required Actions:**

**A. Analysis Phase:**
- Review current features in More screen
- Identify unused/unnecessary features
- Determine which features to keep/remove/update

**B. Design Updates:**
- Apply gradient theme
- Update icons with Lucide-style
- Add gradient dividers between sections
- Modern card-based layout

**C. Profile Screen Updates:**
- User avatar with gradient border
- Stat cards with gradient accents
- Edit button with gradient background
- Match web app profile design

**Features to Review:**
- Settings
- Help & Support
- Terms & Conditions
- Privacy Policy
- About Us
- Logout

**Decision Framework:**
- **Keep:** Essential for MVP
- **Update:** Needs UI improvement
- **Remove:** Not needed for MVP

**Files to Update:**
- `app/src/main/java/com/cehpoint/netwin/presentation/screens/MoreScreen.kt`
- `app/src/main/java/com/cehpoint/netwin/presentation/screens/ProfileScreen.kt`

---

### 3. Tournament Registration Steps UI Update

**Current State:**
- 4-step registration flow
- Basic form styling
- Functional but visually basic

**Required Changes:**

**A. Step Indicator:**
```kotlin
// Current: Simple progress bar
// Target: Gradient progress indicator with step numbers

Row {
    Step1() // Purple
    Connector() // Gradient
    Step2() // Pink
    Connector()
    Step3() // Pink-Cyan
    Connector()
    Step4() // Cyan
}
```

**B. Form Field Updates:**
- Gradient border on focused fields
- Gradient checkboxes/radio buttons
- Modern dropdown selectors
- Gradient submit button

**C. Step-by-Step Updates:**

**Step 1: Game Information**
- In-game name input with gradient focus
- In-game ID input with gradient focus
- Character validation with gradient indicators

**Step 2: Team Details (Squad mode)**
- Team name with gradient card
- Player cards with gradient borders
- Add player button with gradient

**Step 3: Contact Information**
- Phone number with gradient focus
- Discord username (optional) with gradient focus

**Step 4: Review & Confirm**
- Summary card with gradient border
- Entry fee display with gradient accent
- Confirm button with gradient background

**Files to Update:**
- `app/src/main/java/com/cehpoint/netwin/presentation/screens/RegistrationFlowScreen.kt`
- Extract reusable gradient components

---

### 4. My Tournaments Screen Polish

**Current State:**
- Shows registered tournaments
- Basic card layout
- Links to Victory Pass

**Required Actions:**

**A. Bug Check:**
- Test tournament card display
- Verify status indicators
- Check date/time formatting
- Test navigation to Victory Pass

**B. UI Improvements:**
- Tournament cards with gradient borders
- Status badges with proper colors:
  - Upcoming: Yellow gradient
  - Live: Red gradient
  - Completed: Green gradient
- Add subtle animations on card tap

**C. Flow Verification:**
```
Registration Complete
    ↓
Victory Pass Screen (Room ID/Password)
    ↓
Navigate to My Tournaments
    ↓
View Tournament Card
    ↓
Tap to see Victory Pass again
```

**D. Empty State:**
- Add gradient illustration
- "No tournaments yet" message
- Browse tournaments button with gradient

**E. Tournament Card Details:**
- Tournament title
- Game mode and map
- Entry fee (with strikethrough if paid)
- Status indicator
- Start time countdown
- View Details / Victory Pass button

**Files to Update:**
- `app/src/main/java/com/cehpoint/netwin/presentation/screens/MyTournamentsScreen.kt`
- Tournament card component

---

## 📋 Task Execution Order

**Priority Order:**
1. **KYC Screen** (45 mins)
   - Color theme update
   - Document type research
   
2. **Registration Flow Steps** (60 mins)
   - Step indicator
   - Form field styling
   - All 4 steps
   
3. **My Tournaments Screen** (30 mins)
   - Bug testing
   - UI polish
   - Flow verification
   
4. **More & Profile Screens** (45 mins)
   - Analysis
   - UI updates
   - Feature review

**Total Estimated Time:** ~3 hours

---

## 🎨 Design Reference Checklist

Before starting each task, analyze:
- [ ] Web app equivalent screen
- [ ] Color theme usage
- [ ] Gradient patterns
- [ ] Component spacing
- [ ] Typography
- [ ] Icon usage
- [ ] Animation patterns

---

## 🧪 Testing Checklist

After completing each task:
- [ ] Build successful
- [ ] No compilation errors
- [ ] Visual inspection on device
- [ ] Navigation flow works
- [ ] Color theme consistent
- [ ] No UI glitches
- [ ] Responsive on different screen sizes

---

## 📦 Pre-Submission Checklist

Before generating AAB/APK:
- [ ] All UI tasks completed
- [ ] All colors match web app theme
- [ ] No solid cyan remnants
- [ ] Gradient theme applied consistently
- [ ] All navigation flows tested
- [ ] No crashes or critical bugs
- [ ] Firebase rules deployed
- [ ] Test deposit → approve → balance update
- [ ] Test tournament registration → Victory Pass
- [ ] Test KYC submission flow
- [ ] Version number updated in `build.gradle`
- [ ] Release notes prepared

---

## 🚀 Build & Release Steps

**1. Clean Build:**
```bash
./gradlew clean
./gradlew build
```

**2. Generate Release AAB:**
```bash
./gradlew bundleRelease
```
Output: `app/build/outputs/bundle/release/app-release.aab`

**3. Generate Release APK:**
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

**4. Sign APK/AAB:**
- Use Android Studio: Build → Generate Signed Bundle/APK
- Or command line with keystore

**5. Test Release Build:**
- Install on physical device
- Test critical flows
- Verify no debug artifacts

---

## 📝 Documentation Updates

After completing tasks:
- [ ] Update this document with progress
- [ ] Add screenshots of updated screens
- [ ] Document any new bugs found
- [ ] Update version history
- [ ] Update README if needed

---

## 🐛 Known Issues to Address

### Minor Issues (If Time Permits):
1. Transaction history shows "No transactions yet" even with completed deposits
   - Check if `wallet_transactions` are created after approval
   - Verify query in `WalletRepositoryImpl`

2. Tournament status logs appearing frequently
   - Reduce log frequency
   - Move to debug build only

3. Multiple screenshots in Storage from testing
   - Not critical
   - Can clean up later

---

## 🎯 Success Criteria

**MVP is ready when:**
- ✅ All screens use gradient theme
- ✅ No solid cyan colors
- ✅ Registration flow is polished
- ✅ Victory Pass displays correctly
- ✅ Deposit system works end-to-end
- ✅ KYC submission works
- ✅ My Tournaments shows registered tournaments
- ✅ App builds without errors
- ✅ No critical bugs
- ✅ Visual design matches web app

---

## 📱 Client Submission Package

**Include:**
1. **app-release.aab** - For Play Store upload
2. **app-release.apk** - For direct installation/testing
3. **Release Notes** - List of features
4. **Screenshots** - Key screens
5. **Test Account Credentials** - For client testing
6. **Admin App URL** - For deposit/withdrawal management
7. **Firebase Console Access** - If needed

---

## Next Phase (Post-MVP)

After client approval:
- Cloud Functions for automated deposit approval
- Push notifications
- Analytics integration
- Performance optimization
- Localization (Hindi, etc.)
- Payment gateway integration (automated)
- More games support
- Chat/messaging system
- Referral system
