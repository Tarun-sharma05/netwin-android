package com.cehpoint.netwin.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
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
    data object MyTournamentsScreen // NEW: For registered tournaments


    @Serializable
    data object WalletScreen

    // Transactions
    @Serializable
    data object TransactionHistoryScreen

    @Serializable
    data object LeaderboardScreen

    @Serializable
    data object AlertsScreen

    @Serializable
    data object MoreScreen

    @Serializable
    data object ProfileScreen

    @Serializable
    data object SettingsScreen

    // KYC Screen
    @Serializable
    data object KycScreen

    @Serializable
    data object ProfileSetupScreen

    // Payment Proof Screen
    @Serializable
    data class PaymentProofScreen(val amount: Double, val currency: String, val upiAppPackage: String? = null)

    // NEW: Route for the Victory Pass screen
    @Serializable
    data class VictoryPassScreen(val tournamentId: String)

    @Serializable
    data class TournamentRegistration(val tournamentId: String, val stepIndex: Int = 1)
}


sealed class SubNavigation {
    @Serializable
    data object AuthNavGraph : SubNavigation()

    @Serializable
    data object HomeNavGraph : SubNavigation()

    //For Multi step tournament registration flow
    @Serializable
    data object RegistrationNavGraph : SubNavigation() // Add this


}


sealed class Screen(val route: String) {
    // Tournament Details
    object TournamentDetails : Screen("tournament_details/{tournamentId}") {
        fun createRoute(tournamentId: String) = "tournament_details/$tournamentId"
    }
    
    // Victory Pass
    object VictoryPass : Screen("victory_pass/{tournamentId}") {
        fun createRoute(tournamentId: String) = "victory_pass/$tournamentId"
    }
}

