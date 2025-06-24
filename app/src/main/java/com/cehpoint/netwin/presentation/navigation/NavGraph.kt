package com.cehpoint.netwin.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.cehpoint.netwin.data.remote.FirebaseManager
import com.cehpoint.netwin.presentation.screens.*
import com.cehpoint.netwin.presentation.viewmodels.AuthViewModel

@Composable
fun NavGraph(firebaseManager: FirebaseManager) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    
    val items = listOf(
        bottomNavigationItem(name = "Tournaments", icon = Icons.Outlined.EmojiEvents),
        bottomNavigationItem(name = "Wallet", icon = Icons.Outlined.AccountBalanceWallet),
        bottomNavigationItem(name = "Leaderboard", icon = Icons.Outlined.Leaderboard),
        bottomNavigationItem(name = "Alerts", icon = Icons.Outlined.AddAlert),
        bottomNavigationItem(name = "More", icon = Icons.Outlined.Menu)
    )

    var selectedItemIndex by remember { mutableStateOf(0) }
    val currentDestinationAsState = navController.currentBackStackEntryAsState()
    val currentDestination = currentDestinationAsState.value?.destination?.route
    val shouldShowBottomBar = remember { mutableStateOf(true) }

    // Handle authentication state changes
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            navController.navigate(SubNavigation.HomeNavGraph) {
                popUpTo(SubNavigation.AuthNavGraph) { inclusive = true }
            }
        }
    }

    // Handle bottom bar visibility based on current destination
    LaunchedEffect(currentDestination) {
        shouldShowBottomBar.value = when (currentDestination) {
            Screen.TournamentDetails.route -> false
            else -> true
        }
    }
     Box {
         Scaffold(
             modifier = Modifier.fillMaxSize(),
             bottomBar = {
                 if (shouldShowBottomBar.value && isAuthenticated) {
                     NavigationBar(
                         containerColor = Color.Black,
                         tonalElevation = 0.dp,
                         windowInsets = NavigationBarDefaults.windowInsets,
                         modifier = Modifier.background(Color.Black)
                     ) {
                         items.forEachIndexed { index, bottomNavigationItem ->
                             val isSelected = selectedItemIndex == index
                             NavigationBarItem(
                                 selected = isSelected,
                                 onClick = {
                                     selectedItemIndex = index
                                     when (selectedItemIndex) {
                                         0 -> navController.navigate(ScreenRoutes.TournamentsScreen)
                                         1 -> navController.navigate(ScreenRoutes.WalletScreen)
                                         2 -> navController.navigate(ScreenRoutes.LeaderboardScreen)
                                         3 -> navController.navigate(ScreenRoutes.AlertsScreen)
                                         4 -> navController.navigate(ScreenRoutes.MoreScreen)
                                     }
                                 },
                                 icon = {
                                     Icon(
                                         imageVector = bottomNavigationItem.icon,
                                         contentDescription = bottomNavigationItem.name,
                                         tint = if (isSelected) Color.Cyan else Color.White
                                     )
                                 },
                                 label = {
                                     Text(
                                         text = bottomNavigationItem.name,
                                         color = if (isSelected) Color.Cyan else Color.White
                                     )
                                 }
                             )
                         }
                     }
                 }
             }
         ) { innerPadding ->
//             Box(
//                 modifier = Modifier
//                     .fillMaxSize()
//                     .background(Color.Black)
//                     .padding(
//                         bottom = if (shouldShowBottomBar.value) innerPadding.calculateBottomPadding() else 0.dp
//                     )
//
//             ) {
                 NavHost(
                     navController = navController,
                     startDestination = if (isAuthenticated) SubNavigation.HomeNavGraph else SubNavigation.AuthNavGraph
                 ) {
                     navigation<SubNavigation.AuthNavGraph>(startDestination = ScreenRoutes.LoginScreen) {
                         composable<ScreenRoutes.LoginScreen> {
                             LoginScreenUI(
                                 navController = navController,
                                 firebaseManager = firebaseManager
                             )
                         }
                         composable<ScreenRoutes.RegisterScreen> {
                             RegisterScreenUI(navController = navController)
                         }
                     }

                     navigation<SubNavigation.HomeNavGraph>(startDestination = ScreenRoutes.TournamentsScreen) {
                         composable<ScreenRoutes.TournamentsScreen> {
                             TournamentsScreenUI(navController = navController)
                         }
                         composable<ScreenRoutes.WalletScreen> {
                             WalletScreen(navController = navController)
                         }
                         composable<ScreenRoutes.LeaderboardScreen> {
                             LeaderboardScreenUI()
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
                     }
                 }
//             }
         }
     }
}

data class bottomNavigationItem(val name: String, val icon: ImageVector)



