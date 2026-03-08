# NetWin - Navigation Documentation

**Location:** `app/src/main/java/com/cehpoint/netwin/presentation/navigation/`

---

## 1. Navigation Structure

```
navigation/
├── NavGraph.kt              # Main navigation graph
└── NavigationRoutes.kt      # Route definitions
```

---

## 2. NavigationRoutes.kt

**Purpose:** Define all screen routes using type-safe navigation

```kotlin
package com.cehpoint.netwin.presentation.navigation

import kotlinx.serialization.Serializable

sealed class ScreenRoutes {
    
    // Auth Screens
    @Serializable
    data object LoginScreen
    
    @Serializable
    data object RegisterScreen
    
    // Main Screens
    @Serializable
    data object TournamentsScreen
    
    @Serializable
    data object MyTournamentsScreen
    
    @Serializable
    data object WalletScreen
    
    @Serializable
    data object LeaderboardScreen
    
    @Serializable
    data object AlertsScreen
    
    @Serializable
    data object MoreScreen
    
    // Detail Screens with parameters
    @Serializable
    data class TournamentDetailsScreen(val tournamentId: String)
    
    @Serializable
    data class RegistrationFlowScreen(val tournamentId: String)
    
    @Serializable
    data class VictoryPassScreen(val registrationId: String)
    
    // Wallet Screens
    @Serializable
    data object ManualUpiDepositScreen
    
    @Serializable
    data object TransactionHistoryScreen
    
    @Serializable
    data object WithdrawalScreen
    
    // Profile Screens
    @Serializable
    data object ProfileScreen
    
    @Serializable
    data object KycScreen
}
```

**Usage:**
```kotlin
// Navigate to tournament details
navController.navigate(ScreenRoutes.TournamentDetailsScreen(tournamentId = "tour_123"))

// Navigate with arguments
navController.navigate(ScreenRoutes.VictoryPassScreen(registrationId = "reg_456"))

// Navigate and clear back stack
navController.navigate(ScreenRoutes.TournamentsScreen) {
    popUpTo(ScreenRoutes.LoginScreen) { inclusive = true }
}
```

---

## 3. NavGraph.kt

**Purpose:** Main navigation graph with bottom navigation

### 3.1 Structure

```kotlin
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: ScreenRoutes
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth screens
        composable<ScreenRoutes.LoginScreen> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(ScreenRoutes.TournamentsScreen) {
                        popUpTo(ScreenRoutes.LoginScreen) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(ScreenRoutes.RegisterScreen)
                }
            )
        }
        
        composable<ScreenRoutes.RegisterScreen> {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(ScreenRoutes.TournamentsScreen) {
                        popUpTo(ScreenRoutes.LoginScreen) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
        
        // Main screens (with bottom navigation)
        composable<ScreenRoutes.TournamentsScreen> {
            TournamentsScreen(
                onTournamentClick = { tournamentId ->
                    navController.navigate(
                        ScreenRoutes.TournamentDetailsScreen(tournamentId)
                    )
                }
            )
        }
        
        composable<ScreenRoutes.MyTournamentsScreen> {
            MyTournamentsScreen(
                onTournamentClick = { tournamentId ->
                    navController.navigate(
                        ScreenRoutes.TournamentDetailsScreen(tournamentId)
                    )
                },
                onVictoryPassClick = { registrationId ->
                    navController.navigate(
                        ScreenRoutes.VictoryPassScreen(registrationId)
                    )
                }
            )
        }
        
        composable<ScreenRoutes.WalletScreen> {
            WalletScreen(
                onAddCash = {
                    navController.navigate(ScreenRoutes.ManualUpiDepositScreen)
                },
                onWithdraw = {
                    navController.navigate(ScreenRoutes.WithdrawalScreen)
                },
                onViewTransactions = {
                    navController.navigate(ScreenRoutes.TransactionHistoryScreen)
                }
            )
        }
        
        // Detail screens
        composable<ScreenRoutes.TournamentDetailsScreen> { backStackEntry ->
            val args = backStackEntry.toRoute<ScreenRoutes.TournamentDetailsScreen>()
            TournamentDetailsScreen(
                tournamentId = args.tournamentId,
                onRegisterClick = {
                    navController.navigate(
                        ScreenRoutes.RegistrationFlowScreen(args.tournamentId)
                    )
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        composable<ScreenRoutes.RegistrationFlowScreen> { backStackEntry ->
            val args = backStackEntry.toRoute<ScreenRoutes.RegistrationFlowScreen>()
            RegistrationFlowScreen(
                tournamentId = args.tournamentId,
                onRegistrationSuccess = { registrationId ->
                    navController.navigate(
                        ScreenRoutes.VictoryPassScreen(registrationId)
                    ) {
                        popUpTo(ScreenRoutes.TournamentDetailsScreen(args.tournamentId)) {
                            inclusive = true
                        }
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        composable<ScreenRoutes.VictoryPassScreen> { backStackEntry ->
            val args = backStackEntry.toRoute<ScreenRoutes.VictoryPassScreen>()
            VictoryPassScreen(
                registrationId = args.registrationId,
                onViewMyTournaments = {
                    navController.navigate(ScreenRoutes.MyTournamentsScreen) {
                        popUpTo(ScreenRoutes.TournamentsScreen) { inclusive = false }
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        composable<ScreenRoutes.ManualUpiDepositScreen> {
            ManualUpiDepositScreen(
                onSuccess = {
                    navController.popBackStack()
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
```

---

## 4. Bottom Navigation

### 4.1 Implementation

```kotlin
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { index ->
                    selectedTab = index
                    when (index) {
                        0 -> navController.navigate(ScreenRoutes.TournamentsScreen)
                        1 -> navController.navigate(ScreenRoutes.MyTournamentsScreen)
                        2 -> navController.navigate(ScreenRoutes.WalletScreen)
                        3 -> navController.navigate(ScreenRoutes.LeaderboardScreen)
                        4 -> navController.navigate(ScreenRoutes.MoreScreen)
                    }
                }
            )
        }
    ) { paddingValues ->
        NavGraph(
            navController = navController,
            startDestination = ScreenRoutes.TournamentsScreen,
            modifier = Modifier.padding(paddingValues)
        )
    }
}
```

### 4.2 Bottom Navigation Bar

**Current Issue (from memory):**
- Uses solid Color.Cyan
- Missing gradient indicators
- Should use gradient for active states

**Target Implementation:**
```kotlin
@Composable
fun BottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF1E1E2F),
        contentColor = Color.White
    ) {
        bottomNavItems.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (selectedTab == index) {
                            Color.Transparent  // Use gradient
                        } else {
                            Color.Gray
                        }
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = if (selectedTab == index) {
                            TextStyle(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF6C3AFF),
                                        Color(0xFFFF3A8C),
                                        Color(0xFF3AFFDC)
                                    )
                                )
                            )
                        } else {
                            TextStyle(color = Color.Gray)
                        }
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIndicatorColor = Color.Transparent,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

data class BottomNavItem(
    val icon: ImageVector,
    val label: String,
    val route: ScreenRoutes
)

val bottomNavItems = listOf(
    BottomNavItem(Icons.Default.Home, "Home", ScreenRoutes.TournamentsScreen),
    BottomNavItem(Icons.Default.EmojiEvents, "My Tournaments", ScreenRoutes.MyTournamentsScreen),
    BottomNavItem(Icons.Default.AccountBalanceWallet, "Wallet", ScreenRoutes.WalletScreen),
    BottomNavItem(Icons.Default.Leaderboard, "Leaderboard", ScreenRoutes.LeaderboardScreen),
    BottomNavItem(Icons.Default.MoreHoriz, "More", ScreenRoutes.MoreScreen)
)
```

---

## 5. Navigation Flows

### 5.1 Tournament Registration Flow

```
TournamentsScreen
    ↓ (Click tournament)
TournamentDetailsScreen
    ↓ (Click register)
RegistrationFlowScreen (Steps 1-4)
    ↓ (Submit registration)
VictoryPassScreen (Room ID/Password)
    ↓ (View My Tournaments)
MyTournamentsScreen
```

**Code:**
```kotlin
// In TournamentsScreen
onTournamentClick = { tournamentId ->
    navController.navigate(ScreenRoutes.TournamentDetailsScreen(tournamentId))
}

// In TournamentDetailsScreen
onRegisterClick = {
    navController.navigate(ScreenRoutes.RegistrationFlowScreen(tournamentId))
}

// In RegistrationFlowScreen
onRegistrationSuccess = { registrationId ->
    navController.navigate(ScreenRoutes.VictoryPassScreen(registrationId)) {
        popUpTo(ScreenRoutes.TournamentDetailsScreen(tournamentId)) {
            inclusive = true
        }
    }
}

// In VictoryPassScreen
onViewMyTournaments = {
    navController.navigate(ScreenRoutes.MyTournamentsScreen) {
        popUpTo(ScreenRoutes.TournamentsScreen) { inclusive = false }
    }
}
```

---

### 5.2 Deposit Flow

```
WalletScreen
    ↓ (Click Add Cash)
ManualUpiDepositScreen
    ↓ (Submit deposit)
WalletScreen (Shows pending deposit)
```

**Code:**
```kotlin
// In WalletScreen
onAddCash = {
    navController.navigate(ScreenRoutes.ManualUpiDepositScreen)
}

// In ManualUpiDepositScreen
onSuccess = {
    navController.popBackStack()
    // WalletScreen automatically updates via Flow
}
```

---

### 5.3 Authentication Flow

```
LoginScreen
    ↓ (Login success)
TournamentsScreen (Clear back stack)

OR

LoginScreen
    ↓ (Click register)
RegisterScreen
    ↓ (Register success)
TournamentsScreen (Clear back stack)
```

**Code:**
```kotlin
// In LoginScreen
onLoginSuccess = {
    navController.navigate(ScreenRoutes.TournamentsScreen) {
        popUpTo(ScreenRoutes.LoginScreen) { inclusive = true }
    }
}

// In RegisterScreen
onRegisterSuccess = {
    navController.navigate(ScreenRoutes.TournamentsScreen) {
        popUpTo(ScreenRoutes.LoginScreen) { inclusive = true }
    }
}
```

---

## 6. Deep Linking

### 6.1 Configuration

**AndroidManifest.xml:**
```xml
<activity android:name=".MainActivity">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        
        <!-- Tournament details -->
        <data
            android:scheme="netwin"
            android:host="tournament"
            android:pathPrefix="/details" />
        
        <!-- Victory Pass -->
        <data
            android:scheme="netwin"
            android:host="registration"
            android:pathPrefix="/victory-pass" />
    </intent-filter>
</activity>
```

**Usage:**
```kotlin
// Deep link: netwin://tournament/details?id=tour_123
composable<ScreenRoutes.TournamentDetailsScreen>(
    deepLinks = listOf(
        navDeepLink<ScreenRoutes.TournamentDetailsScreen>(
            basePath = "netwin://tournament/details"
        )
    )
) { backStackEntry ->
    // Handle deep link
}
```

---

## 7. Navigation State Management

### 7.1 Back Stack Management

**Clear back stack:**
```kotlin
navController.navigate(ScreenRoutes.TournamentsScreen) {
    popUpTo(ScreenRoutes.LoginScreen) { inclusive = true }
}
```

**Pop to specific screen:**
```kotlin
navController.navigate(ScreenRoutes.MyTournamentsScreen) {
    popUpTo(ScreenRoutes.TournamentsScreen) { inclusive = false }
}
```

**Single top navigation:**
```kotlin
navController.navigate(ScreenRoutes.TournamentsScreen) {
    launchSingleTop = true
}
```

---

### 7.2 Save and Restore State

**Auto-saved by Navigation Compose:**
- ViewModel state
- Scroll position
- User input

**Manual state restoration:**
```kotlin
@Composable
fun MyScreen(
    navController: NavController,
    savedStateHandle: SavedStateHandle
) {
    val scrollState = rememberSaveable(saver = ScrollState.Saver) {
        ScrollState(savedStateHandle.get<Int>("scroll") ?: 0)
    }
    
    DisposableEffect(Unit) {
        onDispose {
            savedStateHandle["scroll"] = scrollState.value
        }
    }
}
```

---

## 8. Navigation Testing

### 8.1 Test Navigation Flow

```kotlin
@Test
fun testTournamentRegistrationFlow() {
    val navController = TestNavHostController(
        ApplicationProvider.getApplicationContext()
    )
    
    composeTestRule.setContent {
        NavGraph(navController = navController)
    }
    
    // Start at tournaments screen
    navController.assertCurrentRouteName(ScreenRoutes.TournamentsScreen::class.simpleName)
    
    // Click tournament
    composeTestRule.onNodeWithText("Tournament 1").performClick()
    navController.assertCurrentRouteName(ScreenRoutes.TournamentDetailsScreen::class.simpleName)
    
    // Click register
    composeTestRule.onNodeWithText("Register").performClick()
    navController.assertCurrentRouteName(ScreenRoutes.RegistrationFlowScreen::class.simpleName)
}
```

---

## 9. Navigation Best Practices

### 9.1 Do's
✅ Use type-safe navigation with sealed classes
✅ Clear back stack when appropriate
✅ Handle deep links for important screens
✅ Test navigation flows
✅ Use `launchSingleTop` for bottom nav items

### 9.2 Don'ts
❌ Don't hardcode route strings
❌ Don't keep unnecessary screens in back stack
❌ Don't navigate in ViewModel
❌ Don't pass large objects as arguments (use IDs)
❌ Don't create circular navigation loops

---

## 10. Navigation Extensions

### 10.1 Helper Functions

```kotlin
// Navigate with result
fun NavController.navigateForResult(
    route: ScreenRoutes,
    key: String,
    value: Any
) {
    currentBackStackEntry?.savedStateHandle?.set(key, value)
    navigate(route)
}

// Get result
fun NavController.getNavigationResult<T>(key: String): T? {
    return currentBackStackEntry?.savedStateHandle?.get<T>(key)
}

// Safe navigation
fun NavController.navigateSafe(route: ScreenRoutes) {
    try {
        navigate(route)
    } catch (e: Exception) {
        Log.e("Navigation", "Navigation failed", e)
    }
}
```

---

## Summary

**Navigation Responsibilities:**
- ✅ Define all app routes
- ✅ Handle screen transitions
- ✅ Manage back stack
- ✅ Support deep linking
- ✅ Save and restore state
- ✅ Provide type-safe navigation
- ✅ Integrate with bottom navigation

**Current Navigation Issue:**
- Bottom nav uses solid cyan instead of gradient
- Needs UI update to match web app theme
