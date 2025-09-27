package com.cehpoint.netwin.presentation.util

import com.cehpoint.netwin.BuildConfig

/**
 * Centralized feature flags for UI experiments.
 *
 * Build-time flag: BuildConfig.TOURNAMENT_UI_V2 (set in build.gradle.kts)
 * Optional runtime flag can be wired to Remote Config or local settings later.
 */
object FeatureFlags {
    // Optional runtime toggle; wire to Remote Config or preferences as needed.
    @Volatile
    var runtimeTournamentUiV2: Boolean = false

    fun isTournamentUiV2(): Boolean {
        return BuildConfig.TOURNAMENT_UI_V2 || runtimeTournamentUiV2
    }

    // Payments feature flags
    @Volatile
    var paymentsInrEnabled: Boolean = false // enable after Razorpay is ready

    @Volatile
    var paymentsNgnEnabled: Boolean = true // Paystack path is enabled
}
