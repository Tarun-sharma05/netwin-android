package com.cehpoint.netwin.presentation.navigation

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.cehpoint.netwin.data.remote.FirebaseManager
import com.cehpoint.netwin.presentation.screens.*
import com.cehpoint.netwin.presentation.viewmodels.AuthViewModel
import com.cehpoint.netwin.presentation.viewmodels.ProfileViewModel
import kotlinx.coroutines.delay

@Composable
fun NavGraph(firebaseManager: FirebaseManager) {
    Log.d("NavGraph", "=== NavGraph COMPOSABLE STARTED ===")
    
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    val isAuthStateInitialized by authViewModel.isAuthStateInitialized.collectAsState()
    val shouldRecheckProfile by profileViewModel.shouldRecheckProfile.collectAsState()
    
    Log.d("NavGraph", "NavGraph - isAuthenticated: $isAuthenticated")
    Log.d("NavGraph", "NavGraph - isAuthStateInitialized: $isAuthStateInitialized")
    Log.d("NavGraph", "NavGraph - shouldRecheckProfile: $shouldRecheckProfile")
    
    // Simple state management
    var profileComplete by rememberSaveable { mutableStateOf<Boolean?>(null) }
    
    Log.d("NavGraph", "NavGraph - profileComplete: $profileComplete")
    
    val items = listOf(
        bottomNavigationItem(name = "Tournaments", icon = Icons.Outlined.EmojiEvents),
        bottomNavigationItem(name = "My Tournaments", icon = Icons.Outlined.SportsCricket),
        bottomNavigationItem(name = "Wallet", icon = Icons.Outlined.AccountBalanceWallet),
        bottomNavigationItem(name = "Alerts", icon = Icons.Outlined.AddAlert),
        bottomNavigationItem(name = "More", icon = Icons.Outlined.Menu)
    )

    val currentDestinationAsState = navController.currentBackStackEntryAsState()
    val currentDestination = currentDestinationAsState.value?.destination?.route
    val shouldShowBottomBar = remember { mutableStateOf(true) }
    
    // Debug logging for route detection
    LaunchedEffect(currentDestination) {
        Log.d("NavGraph", "Current destination: $currentDestination")
    }

    // Single LaunchedEffect to handle all auth and profile logic
    LaunchedEffect(isAuthenticated, isAuthStateInitialized, shouldRecheckProfile) {
        Log.d("NavGraph", "=== LaunchedEffect TRIGGERED ===")
        Log.d("NavGraph", "LaunchedEffect - isAuthenticated: $isAuthenticated")
        Log.d("NavGraph", "LaunchedEffect - isAuthStateInitialized: $isAuthStateInitialized")
        Log.d("NavGraph", "LaunchedEffect - shouldRecheckProfile: $shouldRecheckProfile")
        
        if (!isAuthStateInitialized) {
            Log.d("NavGraph", "LaunchedEffect - Auth state not initialized yet, waiting...")
            // Still loading
            return@LaunchedEffect
        }
        
        if (!isAuthenticated) {
            Log.d("NavGraph", "LaunchedEffect - User not authenticated, resetting profile complete")
            // User not authenticated
            profileComplete = null
            return@LaunchedEffect
        }
        
        // User is authenticated, check profile completeness
        if (profileComplete == null || shouldRecheckProfile) {
            Log.d("NavGraph", "LaunchedEffect - Checking profile completeness")
            
            // Add a small delay to ensure NavHost is ready
            delay(100)
            
            profileViewModel.isProfileCompleteAsync { complete ->
                profileComplete = complete
                Log.d("NavGraph", "LaunchedEffect - Profile completeness result: $complete")
                
                // Navigate to ProfileSetupScreen if profile is incomplete
                if (complete == false) {
                    Log.d("NavGraph", "LaunchedEffect - Profile incomplete, navigating to ProfileSetupScreen")
                    navController.navigate(ScreenRoutes.ProfileSetupScreen) {
                        popUpTo(SubNavigation.HomeNavGraph) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                
                if (shouldRecheckProfile) {
                    Log.d("NavGraph", "LaunchedEffect - Resetting recheck profile flag")
                    profileViewModel.resetRecheckProfile()
                }
            }
        } else {
            Log.d("NavGraph", "LaunchedEffect - Profile already checked, no need to recheck")
        }
    }

    // Handle bottom bar visibility
    LaunchedEffect(currentDestination) {
        val hideBottomBarRoutes = listOf(
            "TournamentDetails",
            "ProfileSetupScreen",
            "KycScreen",
            "VictoryPass"
        )
        shouldShowBottomBar.value = hideBottomBarRoutes.none { route ->
            currentDestination?.contains(route) == true
        } && isAuthenticated
    }

     Box {
         Scaffold(
             modifier = Modifier.fillMaxSize(),
             bottomBar = {
                Log.d("NavGraph", "Bottom bar visibility check:")
                Log.d("NavGraph", "  - shouldShowBottomBar: ${shouldShowBottomBar.value}")
                Log.d("NavGraph", "  - isAuthenticated: $isAuthenticated")
                Log.d("NavGraph", "  - profileComplete: $profileComplete")
                Log.d("NavGraph", "  - Will show bottom bar: ${shouldShowBottomBar.value && isAuthenticated}")
                
                 if (shouldShowBottomBar.value && isAuthenticated) {
                     NavigationBar(
                         containerColor = Color.Black,
                         tonalElevation = 0.dp,
                         windowInsets = NavigationBarDefaults.windowInsets,
                         modifier = Modifier
                             .background(Color.Black)
                             .height(70.dp)
                     ) {
                         items.forEachIndexed { index, bottomNavigationItem ->
                            val isSelected = when (index) {
                                0 -> currentDestination?.contains("TournamentsScreen") == true && !currentDestination.contains("MyTournamentsScreen")
                                1 -> currentDestination?.contains("MyTournamentsScreen") == true
                                2 -> currentDestination?.contains("WalletScreen") == true
                                3 -> currentDestination?.contains("AlertsScreen") == true
                                4 -> currentDestination?.contains("MoreScreen") == true
                                else -> false
                            }

                             NavigationBarItem(
                                 selected = isSelected,
                                 onClick = {
                                     when (index) {
                                        0 -> navController.navigate(ScreenRoutes.TournamentsScreen) {
                                            popUpTo(ScreenRoutes.TournamentsScreen) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                        1 -> navController.navigate(ScreenRoutes.MyTournamentsScreen) {
                                            popUpTo(ScreenRoutes.MyTournamentsScreen) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                        2 -> navController.navigate(ScreenRoutes.WalletScreen) {
                                            popUpTo(ScreenRoutes.WalletScreen) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                        3 -> navController.navigate(ScreenRoutes.AlertsScreen) {
                                            popUpTo(ScreenRoutes.AlertsScreen) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                        4 -> navController.navigate(ScreenRoutes.MoreScreen) {
                                            popUpTo(ScreenRoutes.MoreScreen) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                   }
                                 },
//                                 icon = {
//                                     Column(
//                                         horizontalAlignment = Alignment.CenterHorizontally,
//                                         verticalArrangement = Arrangement.Center
//                                     ) {
//                                         if (isSelected) {
//                                             Spacer(modifier = Modifier.height(0.dp))
//                                             Box(
//                                                 modifier = Modifier
//                                                     .width(60.dp)  // Set width to icon size or a bit wider
//                                                     .height(30.dp)  // Bar thickness
//                                                     .padding(bottom = 26.dp)
//                                                     .background(
//                                                         color = Color.Cyan,
//                                                         shape = RoundedCornerShape(2.dp) // Rounded ends
//                                                     )
//                                             )
////                                             Spacer(modifier = Modifier.height(26.dp))
//                                         } else {
//                                             Spacer(modifier = Modifier.height(8.dp)) // Keep all icons aligned
//                                         }
//                                         Icon(
//                                             imageVector = bottomNavigationItem.icon,
//                                             contentDescription = bottomNavigationItem.name,
//                                             tint = if (isSelected) Color.Cyan else Color.White,
//                                             modifier = Modifier.size(24.dp)
//                                         )
//                                     }
//                                 },


                                 icon = {
                                     Box(
                                         modifier = Modifier
                                             .fillMaxWidth()
                                             .height(70.dp) // same height as NavigationBar
                                     ) {
                                         if (isSelected) {
                                             Box(
                                                 modifier = Modifier
                                                     .fillMaxWidth()
                                                     .height(40.dp) // Reaches from top down to icon
                                                     .align(Alignment.TopCenter)
                                                     .background(
                                                         brush = Brush.verticalGradient(
                                                             colors = listOf(
                                                                 Color.Cyan.copy(alpha = 0.5f),
                                                                 Color.Cyan.copy(alpha = 0.2f),
                                                                 Color.Transparent
                                                             )
                                                         )
                                                     )
                                             )


                                             Box(
                                                 modifier = Modifier
                                                     .align(Alignment.TopCenter)
//                                                     .width(40.dp)
                                                     .fillMaxWidth()
                                                     .height(4.dp)
                                                     .background(
                                                         color = Color.Cyan,
                                                         shape = RoundedCornerShape(2.dp)
                                                     )
                                             )
                                         }

                                         Icon(
                                             imageVector = bottomNavigationItem.icon,
                                             contentDescription = bottomNavigationItem.name,
                                             tint = if (isSelected) Color.Cyan else Color.White,
                                             modifier = Modifier
                                                 .align(Alignment.Center)
                                                 .size(24.dp)
                                         )
                                     }
                                 },

                                 alwaysShowLabel = false, // Hide labels for a cleaner look

                                 colors = NavigationBarItemDefaults.colors(
                                     indicatorColor = Color.Transparent
                                 ) // removes the gray background
                             )






                         }
                     }
                 }
             }
         ) { innerPadding ->
            if (!isAuthStateInitialized) {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.Cyan,
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else {
                val startDestination = when {
                    !isAuthenticated -> {
                        Log.d("NavGraph", "NavHost - Start destination: AuthNavGraph (not authenticated)")
                        Log.d("NavGraph", "NavHost - Reason: isAuthenticated = $isAuthenticated")
                        SubNavigation.AuthNavGraph
                    }
                    else -> {
                        // If user is authenticated, always go to HomeNavGraph
                        // Profile completeness will be checked and handled within HomeNavGraph
                        Log.d("NavGraph", "NavHost - Start destination: HomeNavGraph (authenticated)")
                        Log.d("NavGraph", "NavHost - Reason: isAuthenticated = $isAuthenticated, profileComplete = $profileComplete")
                        SubNavigation.HomeNavGraph
                    }
                }
                
                Log.d("NavGraph", "NavHost - Final start destination: $startDestination")
                
                 NavHost(
                     navController = navController,
                    startDestination = startDestination
                 ) {
                     navigation<SubNavigation.AuthNavGraph>(startDestination = ScreenRoutes.LoginScreen) {
                        composable<ScreenRoutes.LoginScreen> {
                            LoginScreenUI(
                                navController = navController,
                                firebaseManager = firebaseManager,
//                                onLoginSuccess = {
//                                    // Navigate to Tournaments screen after successful login
//                                    navController.navigate(ScreenRoutes.TournamentsScreen) {
//                                        popUpTo(SubNavigation.AuthNavGraph) { inclusive = true }
//                                    }
//                                }
                            )
                        }
                        composable<ScreenRoutes.RegisterScreen> {
                            RegisterScreenUI(
                                navController = navController,
//                                onRegisterSuccess = {
//                                    // Navigate to Tournaments screen after successful registration
//                                    navController.navigate(ScreenRoutes.TournamentsScreen) {
//                                        popUpTo(SubNavigation.AuthNavGraph) { inclusive = true }
//                                    }
//                                }
                            )
                        }
                     }
//                     }

                     navigation<SubNavigation.HomeNavGraph>(startDestination = ScreenRoutes.TournamentsScreen) {
                         composable<ScreenRoutes.TournamentsScreen> {
                             LegacyTournamentsScreenUI(
                                 navController = navController,
//                                 onNavigateToMyTournaments = {
//                                     navController.navigate(ScreenRoutes.MyTournamentsScreen) {
//                                         launchSingleTop = true
//                                     }
//                                 }
                             )
                         }
                         
                         composable<ScreenRoutes.MyTournamentsScreen> {
                            MyTournamentsScreen(
                                onNavigateToTournaments = { 
                                       navController.navigate(ScreenRoutes.TournamentsScreen) {
                                        popUpTo(ScreenRoutes.TournamentsScreen) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToVictoryPass = { tournamentId ->
                                    navController.navigate(Screen.VictoryPass.createRoute(tournamentId)) {
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToTournamentDetails = { tournamentId ->
                                    navController.navigate(Screen.TournamentDetails.createRoute(tournamentId)) {
                                        launchSingleTop = true
                                    }
                                },
                                onBackClick = { navController.popBackStack() }
                            )
                        } 
                         composable<ScreenRoutes.WalletScreen> {
                            WalletScreen(navController = navController)
                        }
                        composable<ScreenRoutes.TransactionHistoryScreen> {
                            TransactionHistoryScreen()
                        }
                        composable<ScreenRoutes.PaymentProofScreen> { backStackEntry ->
                            val paymentProofScreen: ScreenRoutes.PaymentProofScreen = backStackEntry.toRoute()
                            EnhancedPaymentProofScreen(
                                currency = paymentProofScreen.currency,
                                amount = paymentProofScreen.amount,
                                paymentMethod = com.cehpoint.netwin.data.model.PaymentMethod.UPI,
                                upiAppPackage = paymentProofScreen.upiAppPackage,
                                onSubmitProof = { proof ->
                                    // Handle proof submission
                                    navController.popBackStack()
                                },
                                onDismiss = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable<ScreenRoutes.AlertsScreen> {
                            AlertsScreenUI(navController = navController)
                        }
                         composable<ScreenRoutes.MoreScreen> {
                             MoreScreenUI(navController = navController)
                         }
                         composable<ScreenRoutes.ProfileScreen> {
                             ProfileScreenUI(navController = navController)
                         }
                         composable<ScreenRoutes.KycScreen> {
                             KycScreen(navController = navController)
                         }
                        composable<ScreenRoutes.ProfileSetupScreen> {
                            ProfileSetupScreenUI(navController = navController)
                        }
                         // Tournament Details Screen
//                         composable(
//                             route = Screen.TournamentDetails.route,
//                             arguments = listOf(navArgument("tournamentId") { type = NavType.StringType })
//                         ) { backStackEntry ->
//                             val tournamentId = backStackEntry.arguments?.getString("tournamentId") ?: return@composable
//                             TournamentDetailsScreenUI(
//                                 tournamentId = tournamentId,
//                                 navController = navController,
//                                 onBackClick = { navController.popBackStack() }
//                             )
//                         }
                         composable(
                             route = Screen.TournamentDetails.route,
                             arguments = listOf(
                                 navArgument("tournamentId") {
                                     type = androidx.navigation.NavType.StringType
                                 }
                             )
                         ) { backStackEntry ->
                             val tournamentId = backStackEntry.arguments?.getString("tournamentId")
                             if (tournamentId != null) {
                                 TournamentDetailsScreenUI(
                                     tournamentId = tournamentId,
                                     navController = navController
                                 )
                             }
                         }
                         
                         // Victory Pass Screen
                         composable(
                             route = Screen.VictoryPass.route,
                             arguments = listOf(navArgument("tournamentId") { type = NavType.StringType })
                         ) { backStackEntry ->
                             val tournamentId = backStackEntry.arguments?.getString("tournamentId") ?: return@composable
                             VictoryPassScreen(
                                 tournamentId = tournamentId,
                                 onBackClick = { navController.popBackStack() },
                                 onNavigateToRules = { /* TODO: Navigate to rules */ }
                             )
                         }


                         // Add this nested navigation inside your HomeNavGraph
                         navigation<SubNavigation.RegistrationNavGraph>(
                             startDestination = ScreenRoutes.TournamentRegistration("", 1)

                         ) {
                             composable<ScreenRoutes.TournamentRegistration> { backStackEntry ->
                                 val args = backStackEntry.toRoute<ScreenRoutes.TournamentRegistration>()
                                 RegistrationFlowScreen(
                                     tournamentId = args.tournamentId,
                                     stepIndex = args.stepIndex,
                                     navController = navController
                                 )
                             }
                         }

                     }
                 }
            }
         }
     }
}

data class bottomNavigationItem(val name: String, val icon: ImageVector)



