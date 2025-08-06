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
    data object WalletScreen

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


}
   @Serializable
   data class TournamentRegistration(val tournamentId: String, val stepIndex: Int = 1)


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

}

