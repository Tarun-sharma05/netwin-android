package com.cehpoint.netwin.presentation.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cehpoint.netwin.BuildConfig
import com.cehpoint.netwin.R
import com.cehpoint.netwin.domain.model.Tournament
import com.cehpoint.netwin.domain.model.TournamentMode
import com.cehpoint.netwin.domain.model.TournamentStatus
import com.cehpoint.netwin.presentation.components.PrizeInfo
import com.cehpoint.netwin.presentation.components.PullRefreshComponent
import com.cehpoint.netwin.presentation.components.TournamentCapacityIndicator
import com.cehpoint.netwin.presentation.components.statusBarPadding
import com.cehpoint.netwin.presentation.navigation.Screen
import com.cehpoint.netwin.presentation.navigation.ScreenRoutes
import com.cehpoint.netwin.presentation.theme.NetwinTokens
import com.cehpoint.netwin.presentation.util.FeatureFlags
import com.cehpoint.netwin.presentation.viewmodels.TournamentEvent
import com.cehpoint.netwin.presentation.viewmodels.TournamentFilter
import com.cehpoint.netwin.presentation.viewmodels.TournamentState
import com.cehpoint.netwin.presentation.viewmodels.TournamentViewModel
import com.cehpoint.netwin.ui.theme.NetWinCyan
import com.cehpoint.netwin.ui.theme.NetWinPink
import com.cehpoint.netwin.ui.theme.NetWinPurple
import com.cehpoint.netwin.utils.NGNTransactionUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

// Debug/preview toggle: when true, use brand token accents instead of hard-coded accent colors
private const val USE_ALT_ACCENTS: Boolean = false


//// Formats a currency string by trimming unnecessary trailing decimals like ".00" while preserving symbols and separators.
//private fun tidyMoney(input: String): String {
//    // Examples:
//    // "₹1,000.00" -> "₹1,000"
//    // "INR 1,000.00" -> "INR 1,000"
//    // "1,000.50" stays as is
//    return input.replace(Regex("(?<=\\d)\\.00(?!\\d)"), "")
//}
//
///**
// * A utility function to format prize/fee amounts by removing unnecessary decimals.
// */
//private fun formatAmount(amount: Double): String {
//    return if (amount == amount.toInt().toDouble()) {
//        amount.toInt().toString()
//    } else {
//        amount.toString()
//    }
//}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun LegacyTournamentsScreenUI(navController: NavController, viewModel: TournamentViewModel = hiltViewModel()) {
    Log.d("TournamentsScreen", "=== TournamentsScreenUI COMPOSABLE STARTED ===")

    val state by viewModel.state.collectAsState()
    val registrationState by viewModel.registrationState.collectAsState()
    val walletBalance by viewModel.walletBalance.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val refreshError by viewModel.refreshError.collectAsState()
    val refreshSuccess by viewModel.refreshSuccess.collectAsState()
    val context = LocalContext.current

    // Get user country and currency
    var userCountry by remember { mutableStateOf("India") }
    var userCurrency by remember { mutableStateOf("INR") }

    LaunchedEffect(Unit) {
        // Get current user to determine country
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            try {
                // Get user country from Firestore or use default
                val userDoc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.uid)
                    .get()
                    .await()

                userCountry = userDoc.getString("country") ?: "India"
                userCurrency = if (userCountry.equals("Nigeria", ignoreCase = true) || userCountry.equals("NG", ignoreCase = true)) "NGN" else "INR"
            } catch (e: Exception) {
                Log.e("TournamentsScreen", "Error getting user country: ${e.message}")
                userCountry = "India"
                userCurrency = "INR"
            }
        }
    }

    val tournamentState = state as? TournamentState ?: TournamentState()
    val isLoading = tournamentState.isLoading
    val error = tournamentState.error
    val tournaments = tournamentState.tournaments

    Log.d("TournamentsScreen", "Tournament state updated:")
    Log.d("TournamentsScreen", "Loading: $isLoading")
    Log.d("TournamentsScreen", "Error: $error")
    Log.d("TournamentsScreen", "Number of tournaments: ${tournaments.size}")
    tournaments.forEach { tournament ->
        Log.d("TournamentsScreen", "Tournament in UI: ${tournament.name}, ID: ${tournament.id}, Status: ${tournament.computedStatus}, Mode: ${tournament.mode}, Map: ${tournament.map}")
    }

    LaunchedEffect(registrationState) {
        registrationState?.let { result ->
            val message = result.fold(
                onSuccess = { "Successfully registered for tournament!" },
                onFailure = { it.message ?: "Registration failed." }
            )
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearRegistrationState()
        }
    }

    LaunchedEffect(Unit) {
        Log.d("TournamentsScreen", "=== LaunchedEffect TRIGGERED ===")
        Log.d("TournamentsScreen", "LaunchedEffect - Loading tournaments")
        viewModel.handleEvent(TournamentEvent.LoadTournaments)
        Log.d("TournamentsScreen", "LaunchedEffect - Tournament load event sent")
    }

    Scaffold(
        topBar = {
            TournamentsTopBar(
                walletBalance = walletBalance.toInt(),
                currency = userCurrency,
                isRefreshing = false, // Don't show refresh indicator in top bar
                onToggleUi = if (BuildConfig.DEBUG) {
                    {
                        FeatureFlags.runtimeTournamentUiV2 = !FeatureFlags.runtimeTournamentUiV2
                    }
                } else null,
                navController = navController
            )
        },
        // Uncomment and implement if you have a bottom nav bar
        // bottomBar = { BottomNavBar(navController) }
    ) { paddingValues ->

        PullRefreshComponent(
            isRefreshing = isRefreshing,
            refreshError = refreshError,
            refreshSuccess = refreshSuccess,
            onRefresh = {
                viewModel.handleEvent(TournamentEvent.RefreshTournaments(force = true))
            },
            onClearRefreshSuccess = {
                viewModel.clearRefreshSuccess()
            },
            onClearRefreshError = {
                viewModel.clearRefreshError()
            }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NetwinTokens.Background)
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    bottom = 48.dp,
                    top = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { WelcomeCard(userName = userName) }
                item {
                    Text(
                        text = "Available Tournaments",
                        color = Color.White,
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                item {
                    TournamentsFilters(
                        selectedFilter = tournamentState.selectedFilter.name,
                        onFilterChange = { filter ->
                            viewModel.handleEvent(TournamentEvent.FilterTournaments(TournamentFilter.valueOf(filter)))
                        },
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            viewModel.handleEvent(TournamentEvent.RefreshTournaments(force = true))
                        }
                    )
                }

                if (isLoading && !isRefreshing) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = NetwinTokens.Accent)
                        }
                    }
                } else if (error != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = error,
                                color = Color.Red,
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(tournaments) { tournament ->
                        TournamentCard(
                            tournament = tournament,
                            viewModel = viewModel,
                            onCardClick = {
                                Log.d("TournamentsScreen", "Navigating to tournament details: ${tournament.id}")
                                navController.navigate(Screen.TournamentDetails.createRoute(tournament.id))
                            },
                            navController = navController,
                            currency = userCurrency
                        )
                    }
                }
            }
        }
    }
}

// Shimmer utilities and placeholder
@Composable
private fun ShimmerTournamentCardPlaceholder() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "x"
    )
    val shimmerColors = listOf(
        NetwinTokens.Surface.copy(alpha = 0.9f),
        NetwinTokens.SurfaceAlt.copy(alpha = 0.6f),
        NetwinTokens.Surface.copy(alpha = 0.9f)
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(shimmerX, 0f),
        end = Offset(shimmerX + 200f, 0f)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NetwinTokens.Sp16),
        shape = RoundedCornerShape(NetwinTokens.RadiusLg),
        colors = CardDefaults.cardColors(containerColor = NetwinTokens.Surface)
    ) {
        Column(modifier = Modifier.padding(NetwinTokens.Sp14)) {
            // Title line
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush)
            )
            Spacer(Modifier.height(NetwinTokens.Sp8))
            // Chips row
            Row(horizontalArrangement = Arrangement.spacedBy(NetwinTokens.Sp4)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(70.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(brush)
                    )
                }
            }
            Spacer(Modifier.height(NetwinTokens.Sp8))
            // Capacity bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush)
            )
            Spacer(Modifier.height(NetwinTokens.Sp8))
            // Buttons row
            Row(horizontalArrangement = Arrangement.spacedBy(NetwinTokens.Sp4)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .clip(RoundedCornerShape(NetwinTokens.RadiusSm))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .clip(RoundedCornerShape(NetwinTokens.RadiusSm))
                        .background(brush)
                )
            }
        }
    }
}

// ...

@Composable
fun TournamentCardV2(
    t: Tournament,
    onDetailsClick: () -> Unit,
    currency: String, // Added currency parameter
    onRegisterClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NetwinTokens.Sp8)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        NetwinTokens.Primary.copy(alpha = 0.12f),
                        NetwinTokens.Accent.copy(alpha = 0.12f)
                    )
                ),
                shape = RoundedCornerShape(NetwinTokens.RadiusLg)
            )
            .clip(RoundedCornerShape(NetwinTokens.RadiusLg)),
        colors = CardDefaults.cardColors(containerColor = NetwinTokens.SurfaceAlt)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = NetwinTokens.Sp4)
        ) {
            // Banner with overlay and countdown (if available)
            if (!t.bannerImage.isNullOrBlank()) {
                // TODO: Implement banner image handling
            }

            Spacer(Modifier.height(NetwinTokens.Sp8))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = NetwinTokens.Sp4),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
//                PrizeInfo(
//                    label = "Prize Pool",
//                    value = tidyMoney(NGNTransactionUtils.formatAmount(t.prizePool.toDouble(), "INR")),
//                    icon = Icons.Default.EmojiEvents,
//                    color = Color(0xFFFFC107)
//                )
//                PrizeInfo(
//                    label = "Entry Fee",
//                    value = if (t.entryFee > 0) tidyMoney(NGNTransactionUtils.formatAmount(t.entryFee.toDouble(), "INR")) else "Free",
//                    icon = Icons.Default.AccountBalanceWallet,
//                    color = if (t.entryFee > 0) Color(0xFFFF7043) else Color(0xFF4CAF50)
//                )
//                PrizeInfo(
//                    label = "Per Kill",
//                    value = tidyMoney(NGNTransactionUtils.formatAmount(t.killReward ?: 0.0, "INR")),
//                    icon = Icons.Default.EmojiEvents,
//                    color = Color(0xFF26C6DA)
//                )

                // UPDATED to use the centralized, tidy formatting function
                PrizeInfo(
                    label = "Prize Pool",
                    value = NGNTransactionUtils.formatAmountTidy(t.prizePool, currency),
                    icon = Icons.Default.EmojiEvents,
                    color = Color(0xFFFFC107)
                )
                PrizeInfo(
                    label = "Entry Fee",
                    value = if (t.entryFee > 0) NGNTransactionUtils.formatAmountTidy(t.entryFee, currency) else "Free",
                    icon = Icons.Default.AccountBalanceWallet,
                    color = if (t.entryFee > 0) Color(0xFFFF7043) else Color(0xFF4CAF50)
                )
                PrizeInfo(
                    label = "Per Kill",
                    value = NGNTransactionUtils.formatAmountTidy(t.killReward ?: 0.0, currency),
                    icon = Icons.Default.EmojiEvents,
                    color = Color(0xFF26C6DA)
                )
            }
            // Countdown between currency row and capacity
            if (t.computedStatus == TournamentStatus.UPCOMING || t.computedStatus == TournamentStatus.STARTS_SOON) {
                Spacer(Modifier.height(NetwinTokens.Sp4))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(modifier = Modifier.scale(0.8f)) { // subtle size reduction for visual balance
                        DetailsCountdownBadge(targetTimeMillis = t.startTime)
                    }
                }
            }
            // Subtle divider for hierarchy
            Divider(
                color = NetwinTokens.Primary.copy(alpha = 0.08f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = NetwinTokens.Sp4)
            )
            TournamentCapacityIndicator(
                registeredTeams = t.registeredTeams,
                maxTeams = t.maxTeams
            )
            Spacer(Modifier.height(NetwinTokens.Sp8))
            Row(horizontalArrangement = Arrangement.spacedBy(NetwinTokens.Sp4)) {
                OutlinedButton(
                    onClick = onDetailsClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(NetwinTokens.RadiusSm),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NetwinTokens.Primary),
                    border = BorderStroke(1.dp, NetwinTokens.Primary)
                ) { Text("Details", fontWeight = FontWeight.SemiBold, color = NetwinTokens.Primary) }

                GradientPrimaryButton(
                    text = "Register",
                    modifier = Modifier.weight(1f),
                    onClick = onRegisterClick
                )
            }
        }
    }
}

@Composable
fun TournamentsTopBar(
    walletBalance: Int,
    currency: String,
    isRefreshing: Boolean = false,
    onToggleUi: (() -> Unit)? = null,
    navController: NavController
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF121212)) // Explicit dark background
            .statusBarPadding()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "NETWIN",
                style = MaterialTheme.typography.titleLarge,
                color = NetwinTokens.TextPrimary,
                fontWeight = FontWeight.Bold
            )

            // Show refresh indicator next to title when refreshing
            if (isRefreshing) {
                Spacer(modifier = Modifier.width(8.dp))
                CircularProgressIndicator(
                    color = NetwinTokens.Primary,
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            }
        }

        Surface(
            color = NetwinTokens.SurfaceAlt,
            shape = RoundedCornerShape(12.dp),
//            border = BorderStroke(1.dp, NetwinTokens.Primary.copy(alpha = 0.24f))
            border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(NetWinPurple, NetWinPink, NetWinCyan))),
            onClick = { navController.navigate(ScreenRoutes.WalletScreen) }
            ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    // Use the new tidy formatter here as well for consistency
                    NGNTransactionUtils.formatAmountTidy(walletBalance.toDouble(), currency),
                    modifier = Modifier.padding(start = 6.dp),
                    color = NetwinTokens.TextPrimary,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize
                )

            // Debug-only feature toggle button
//            if (BuildConfig.DEBUG && onToggleUi != null) {
//                Spacer(modifier = Modifier.width(12.dp))
//                IconButton(onClick = onToggleUi) {
//                    // Reuse an icon to avoid extra imports; color indicates current state
//                    Icon(
//                        imageVector = Icons.Default.FilterList,
//                        contentDescription = "Toggle Tournament UI V2",
//                        tint = if (FeatureFlags.isTournamentUiV2()) Color.White else Color(0xFF757575)
//                    )
//                }
//            }
            }
        }
    }
}

@Composable
fun WelcomeCard(userName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Background image for welcome card
        Image(
            painter = painterResource(id = R.drawable.esport),
            contentDescription = "Welcome background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Overlay for better text contrast
        Box(
            Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.4f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = "Trophy",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Welcome Back, $userName!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.headlineMedium.fontSize
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ready to compete? Join a tournament and showcase your skills!",
                    color = Color.White.copy(alpha = 0.8f),
                fontSize = MaterialTheme.typography.bodyMedium.fontSize
            )
        }
    }
}

@Composable
fun TournamentsFilters(
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(NetwinTokens.Background)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        FilterChip(
            text = "Filter",
            icon = Icons.Default.FilterList,
            selected = selectedFilter == "Filter",
            onClick = { onFilterChange("Filter") }
        )
        Spacer(Modifier.width(12.dp))
        FilterChip(
            text = "All Games",
            icon = Icons.Default.EmojiEvents,
            selected = selectedFilter == "All Games",
            onClick = { onFilterChange("All Games") }
        )
        Spacer(Modifier.width(12.dp))
        FilterChip(
            text = "All Maps",
            icon = Icons.Default.Map,
            selected = selectedFilter == "All Maps",
            onClick = { onFilterChange("All Maps") }
        )
        // Refresh chip/button removed as requested
    }

    Spacer(Modifier.width(12.dp))

    Row(
        Modifier
            .fillMaxWidth()
            .background(NetwinTokens.Background)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        FilterChip(
            text = "Entry Fee",
            icon = Icons.Default.AccountBalanceWallet,
            selected = selectedFilter == "Entry Fee",
            onClick = { onFilterChange("Entry Fee") }
        )
    }
}

@Composable
fun FilterChip(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) NetwinTokens.Primary else NetwinTokens.Primary.copy(alpha = 0.2f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = NetwinTokens.Surface,
            contentColor = if (selected) NetwinTokens.TextPrimary else NetwinTokens.TextSecondary
        ),
        modifier = Modifier.height(36.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) NetwinTokens.Primary else NetwinTokens.TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            fontSize = 14.sp
        )
    }
}
//
//@Composable
//fun TournamentCard(
//    tournament: Tournament,
//    viewModel: TournamentViewModel,
//    onCardClick: () -> Unit,
//    navController: NavController,
//    currency: String
//) {
//    // ... existing code ...
//
//    val interactionSource = remember { MutableInteractionSource() }
//    val isPressed by interactionSource.collectIsPressedAsState()
//    val scale by animateFloatAsState(
//        targetValue = if (isPressed) 0.99f else 1f,
//        animationSpec = spring(
//            dampingRatio = Spring.DampingRatioMediumBouncy,
//            stiffness = Spring.StiffnessLow
//        )
//    )
//
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 16.dp, vertical = 8.dp)
//            .shadow(
//                elevation = 4.dp,
//                shape = RoundedCornerShape(12.dp),
//                spotColor = NetwinTokens.Primary.copy(alpha = 0.1f)
//            )
//            .graphicsLayer {
//                scaleX = scale
//                scaleY = scale
//            }
//            .clickable(
//                interactionSource = interactionSource,
//                onClick = onCardClick,
//                role = Role.Button,
//                indication = ripple(
//                    bounded = true,
//                    radius = 300.dp,
//                    color = NetwinTokens.Primary
//                )
//            ),
//        shape = RoundedCornerShape(12.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = if (isPressed)
//                NetwinTokens.SurfaceAlt.copy(alpha = 0.9f)
//            else
//                NetwinTokens.SurfaceAlt
//        ),
//        elevation = CardDefaults.cardElevation(
//            defaultElevation = 2.dp,
//            pressedElevation = 1.dp
//        )
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//        ) {
//            // 1. Banner Box (140dp) - Image + Overlays ONLY
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(140.dp)
//            ) {
//                // Background Image with fallback color
//                if (tournament.bannerImage != null) {
//                    AsyncImage(
//                        model = tournament.bannerImage,
//                        contentDescription = "Tournament banner",
//                        contentScale = ContentScale.Crop,
//                        modifier = Modifier.fillMaxSize()
//                    )
//                } else {
//                    Box(
//                        modifier = Modifier
//                            .fillMaxSize()
//                            .background(NetwinTokens.SurfaceAlt)
//                    )
//                }
//
//                // Dark overlay for text contrast
//                Box(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .background(
//                            brush = Brush.verticalGradient(
//                                colors = listOf(
//                                    Color.Transparent,
//                                    Color.Black.copy(alpha = 0.7f)
//                                ),
//                                startY = 0f,
//                                endY = Float.POSITIVE_INFINITY
//                            )
//                        )
//                )
//
//                // Tournament name and status row (top overlay)
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(16.dp),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.Top
//                ) {
//                    Text(
//                        text = tournament.name,
//                        color = Color.White,
//                        fontWeight = FontWeight.Bold,
//                        fontSize = 18.sp,
//                        maxLines = 2,
//                        overflow = TextOverflow.Ellipsis,
//                        modifier = Modifier.weight(1f)
//                    )
//
//                    TournamentStatusBadge(
//                        status = tournament.computedStatus,
//                        modifier = Modifier.padding(start = 8.dp)
//                    )
//                }
//
//                // Map location (bottom overlay)
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(16.dp)
//                        .align(Alignment.BottomStart),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.PinDrop,
//                        contentDescription = "Location",
//                        tint = Color.White,
//                        modifier = Modifier.size(16.dp)
//                    )
//                    Spacer(modifier = Modifier.width(4.dp))
//                    Text(
//                        text = tournament.map,
//                        color = Color.White,
//                        fontSize = 14.sp,
//                        maxLines = 1,
//                        overflow = TextOverflow.Ellipsis
//                    )
//                }
//            }
//
//            // 2. Content Column - Tournament details
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(16.dp)
//            ) {
//                // Prize pool, entry fee, etc.
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(vertical = 8.dp),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    InfoChip(
//                        label = "Prize Pool",
//                        value = "${tidyMoney(currency)}${tidyMoney(tournament.prizePool.toString())}",
//                        valueColor = NetwinTokens.Primary
//                    )
//
//                    InfoChip(
//                        label = "Per Kill",
//                        value = "${tidyMoney(currency)}${tidyMoney(tournament.killReward.toString())}",
//                        valueColor = NetwinTokens.Accent
//                    )
//
//                    InfoChip(
//                        label = "Entry",
//                        value = if (tournament.entryFee > 0)
//                            "${tidyMoney(currency)}${tidyMoney(tournament.entryFee.toString())}"
//                        else "Free",
//                        valueColor = if (tournament.entryFee > 0)
//                            NetwinTokens.TextPrimary
//                        else NetwinTokens.Tertiary
//                    )
//                }
//
//                Spacer(modifier = Modifier.height(12.dp))
//
//                // Mode and team size
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    // Game mode chip
//                    ModeChip(mode = tournament.mode)
//
//                    // Team size
//                    Row(verticalAlignment = Alignment.CenterVertically) {
//                        Icon(
//                            imageVector = Icons.Default.People,
//                            contentDescription = "Team size",
//                            tint = NetwinTokens.TextSecondary,
//                            modifier = Modifier.size(16.dp)
//                        )
//                        Spacer(modifier = Modifier.width(4.dp))
//                        Text(
//                            text = "${tournament.teamSize}v${tournament.teamSize}",
//                            color = NetwinTokens.TextSecondary,
//                            fontSize = 14.sp
//                        )
//                    }
//
//                    // Registration status
//                    Text(
//                        text = "${tournament.registeredTeams}/${tournament.maxTeams} Teams",
//                        color = if (tournament.registeredTeams >= tournament.maxTeams)
//                            NetwinTokens.Accent
//                        else NetwinTokens.Primary,
//                        fontWeight = FontWeight.Medium,
//                        fontSize = 14.sp
//                    )
//                }
//
//                // 3. Divider
//                Divider(
//                    color = NetwinTokens.SurfaceAlt.copy(alpha = 0.5f),
//                    modifier = Modifier.padding(vertical = 12.dp)
//                )
//
//                // 4. Registration button
//                val isFull = tournament.registeredTeams >= tournament.maxTeams
//                val statusAllows = tournament.computedStatus == TournamentStatus.UPCOMING ||
//                                tournament.computedStatus == TournamentStatus.STARTS_SOON ||
//                                tournament.computedStatus == TournamentStatus.ONGOING ||
//                                tournament.computedStatus == TournamentStatus.ROOM_OPEN
//
//                Button(
//                    onClick = {
//                        if (statusAllows && !isFull) {
//                            navController.navigate(
//                                TournamentRegistration(
//                                    tournamentId = tournament.id,
//                                    stepIndex = 0
//                                )
//                            )
//                        } else {
//                            onCardClick()
//                        }
//                    },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(48.dp),
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = if (isFull || !statusAllows)
//                            NetwinTokens.Surface
//                        else NetwinTokens.Primary,
//                        contentColor = if (isFull || !statusAllows)
//                            NetwinTokens.TextSecondary
//                        else Color.White,
//                        disabledContainerColor = NetwinTokens.SurfaceAlt,
//                        disabledContentColor = NetwinTokens.TextSecondary
//                    ),
//                    shape = RoundedCornerShape(8.dp),
//                    enabled = !isFull && statusAllows
//                ) {
//                    Text(
//                        text = when {
//                            isFull -> "Tournament Full"
//                            tournament.computedStatus == TournamentStatus.ONGOING -> "Join Now"
//                            else -> "Register Now"
//                        },
//                        fontWeight = FontWeight.SemiBold,
//                        fontSize = 16.sp
//                    )
//                }
//            }
//        }
//    }
//}


/**
 * The final, pixel-perfect implementation of the Tournament Card, matching the provided screenshots.
 *
 * @param tournament The data object containing all tournament information.
 * @param onCardClick Lambda function to be invoked when the card is clicked.
 * @param navController The navigation controller for handling navigation events.
 */
@Composable
fun TournamentCard(
    tournament: Tournament,
    onCardClick: () -> Unit,
    viewModel: TournamentViewModel,
    currency: String,
    navController: NavController
) {
    // 2. CREATE A DYNAMIC CURRENCY SYMBOL
    val currencySymbol = when (currency) {
        "NGN" -> "₦"
        else -> "₹" // Default to INR
    }

    // Define the exact colors from the screenshot for clarity
    val cardBackgroundColor = Color(0xFF2C2B3E)
    val prizeColor = Color(0xFFFFD600) // Yellowish gold
    val killColor = Color(0xFF00E5FF)   // Cyan/Blue
    val feeColor = Color(0xFFFF4D6D)     // Reddish pink
    val buttonGradient = Brush.horizontalGradient(listOf(Color(0xFF6A11CB), Color(0xFF2575FC)))
    // Define the gradient colors based on the analyzed imagek
    val gradientColors = listOf(
        Color(0xFF903AE3), // Left - Purple
        Color(0xFFA970B4), // Center - Purple-Pink blend
        Color(0xFFDD3BA8)  // Right - Pink
    )
    val detailsButtonOutlineColor = Color(0xFF625F83)
    val capacityProgressColor = Color(0xFF00E676) // Bright Green

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // No ripple effect
                onClick = onCardClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Banner Image with Status Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                AsyncImage(
                    model = tournament.bannerImage,
                    contentDescription = "Tournament Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
                // Status badge positioned at the top start
                TournamentStatusBadge(
                    status = tournament.computedStatus,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                )
            }

            // 2. Content Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title and Sub-details (Mode, Map)
                Column {
                    Text(
                        text = tournament.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ModeChip(mode = tournament.mode)
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Map",
                            tint = NetwinTokens.Primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tournament.map,
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                    }
                }

                // Prize, Kill, and Entry Fee Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
//                    InfoColumn(label = "Prize Pool", value = "₹${formatAmount(tournament.prizePool)}", valueColor = prizeColor)
//                    InfoColumn(label = "Per Kill", value = "₹${formatAmount(tournament.killReward ?: 0.0)}", valueColor = killColor)
//                    InfoColumn(label = "Entry Fee", value = "₹${formatAmount(tournament.entryFee)}", valueColor = feeColor)
                    PrizeInfo(
                        label = "Prize Pool",
                        value = NGNTransactionUtils.formatAmountTidy(tournament.prizePool, currency),
                        icon = Icons.Default.EmojiEvents,
                        color = prizeColor,
                        showLabel = true
                    )
                    PrizeInfo(
                        label = "Per Kill",
                        value = NGNTransactionUtils.formatAmountTidy(tournament.killReward ?: 0.0, currency),
                        icon = Icons.Default.EmojiEvents,
                        color = killColor,
                        showLabel = true
                    )
                    PrizeInfo(
                        label = "Entry Fee",
                        value = if (tournament.entryFee > 0) NGNTransactionUtils.formatAmountTidy(tournament.entryFee, currency) else "Free",
                        icon = Icons.Default.AccountBalanceWallet,
                        color = if (tournament.entryFee > 0) feeColor else Color(0xFF4CAF50),
                        showLabel = true
                    )
                }



                // UPDATED: Countdown Section is now a separate, decorated composable on the left
                if (tournament.computedStatus == TournamentStatus.UPCOMING || tournament.computedStatus == TournamentStatus.STARTS_SOON) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        CountdownBadge(targetTimeMillis = tournament.startTime)
                    }
                }


                // Capacity Indicator
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Capacity:", color = Color.LightGray, fontSize = 14.sp)
                        Text(
                            "${tournament.registeredTeams}/${tournament.maxTeams} Players",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (tournament.registeredTeams.toFloat() / tournament.maxTeams.toFloat()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = capacityProgressColor,
                        trackColor = detailsButtonOutlineColor
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                               /* Handle Details Click */
                               navController.navigate(Screen.TournamentDetails.createRoute(tournament.id))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, detailsButtonOutlineColor)
                    ) {
                        Text("Details", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                        /* Handle Register Click */
                            navController.navigate(
                                ScreenRoutes.TournamentRegistration(
                                    tournamentId = tournament.id,
                                    stepIndex = 1
                                )
                            )

                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(), // Remove default padding
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Box(
//                            modifier = Modifier
//                                .fillMaxSize()
//                                .background(buttonGradient)
//                                .padding(horizontal = 16.dp, vertical = 8.dp),
//                            contentAlignment = Alignment.Center
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(gradientColors),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Register", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * NEW: A decorated, standalone badge that displays a countdown timer.
 */
@Composable
fun CountdownBadge(targetTimeMillis: Long, modifier: Modifier = Modifier) {
    var remainingTime by remember { mutableStateOf("") }

    LaunchedEffect(targetTimeMillis) {
        while (true) {
            val now = System.currentTimeMillis()
            val diff = targetTimeMillis - now

            if (diff <= 0) {
                remainingTime = "00:00:00"
                break
            }

            val hours = diff / (1000 * 60 * 60)
            val minutes = (diff % (1000 * 60 * 60)) / (1000 * 60)
            val seconds = (diff % (1000 * 60)) / 1000

            remainingTime = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            delay(1000)
        }
    }

    if (remainingTime.isNotEmpty() && remainingTime != "00:00:00") {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF6A11CB).copy(alpha = 0.1f)
            ),
            border = BorderStroke(1.dp, Color(0xFF6A11CB))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Starts In",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Countdown",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = remainingTime,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}


/**
 * A composable for displaying a piece of info with a label and a colored value.
 */
@Composable
private fun InfoColumn(label: String, value: String, valueColor: Color) {
    Column {
        Text(text = label, color = Color.LightGray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


/**
 * A badge with improved visibility that displays the tournament status.
 */
@Composable
fun TournamentStatusBadge(status: TournamentStatus, modifier: Modifier = Modifier) {
    val (text, color) = when (status) {
        TournamentStatus.COMPLETED -> "Completed" to Color(0xFF616161) // Dark Grey
        TournamentStatus.UPCOMING -> "Upcoming" to Color(0xFFD84315)   // Deep Orange
        TournamentStatus.STARTS_SOON -> "Starts Soon" to Color(0xFF0277BD) // Light Blue
        TournamentStatus.ONGOING -> "Live" to Color(0xFF2E7D32)         // Dark Green
        TournamentStatus.ROOM_OPEN -> "Room Open" to Color(0xFFC62828) // Red
    }

    Surface(
        color = color, // Use a solid, vibrant color for the background
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Status",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text.uppercase(),
                color = Color.White, // White text provides good contrast
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
        }
    }
}

/**
 * A chip to display the tournament mode (e.g., Solo, Duo, Squad).
 */
@Composable
fun ModeChip(mode: TournamentMode, modifier: Modifier = Modifier) {
    val text = when (mode) {
        TournamentMode.SOLO -> "Solo"
        TournamentMode.DUO -> "Duo"
        TournamentMode.SQUAD -> "Squad"
        TournamentMode.TRIO -> "Trio"
        TournamentMode.CUSTOM -> "Custom"
    }
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF625F83)),
        modifier = modifier
    ) {
        Text(
            text = text,
            color = Color.Gray,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}



@Composable
fun GradientPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = NetwinTokens.Primary,
            disabledContainerColor = NetwinTokens.Primary.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(NetwinTokens.RadiusSm)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

//@Composable
//fun ModeChip(mode: TournamentMode, modifier: Modifier = Modifier) {
//    val text = when (mode) {
//        TournamentMode.SOLO -> "Solo"
//        TournamentMode.DUO -> "Duo"
//        TournamentMode.SQUAD -> "Squad"
//        TournamentMode.TRIO -> "Trio"
//        TournamentMode.CUSTOM -> "Custom"
//    }
//    Surface(
//        color = NetwinTokens.Surface,
//        shape = RoundedCornerShape(10.dp),
//        border = BorderStroke(1.dp, NetwinTokens.Primary.copy(alpha = 0.35f)),
//        modifier = modifier
//    ) {
//        Text(
//            text = text,
//            color = Color.White,
//            fontWeight = FontWeight.Bold,
//            fontSize = 12.sp,
//            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
//        )
//    }
//}

// -------------------------
// Compose Previews (no backend)
// -------------------------

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun Preview_TournamentsTopBar() {
//    TournamentsTopBar(walletBalance = 1250, currency = "INR", isRefreshing = false, onToggleUi = {}, navController = )
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun Preview_InfoChip() {
    Row(
        modifier = Modifier
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        InfoChip(
            label = "Prize Pool", value = "₹5,000",
            valueColor = TODO()
        )
        Spacer(Modifier.width(12.dp))
        InfoChip(
            label = "Per Kill", value = "₹10",
            valueColor = TODO()
        )
        Spacer(Modifier.width(12.dp))
        InfoChip(
            label = "Entry Fee", value = "Free",
            valueColor = TODO()
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun Preview_TournamentStatusBadge() {
    Row(
        modifier = Modifier
            .background(Color(0xFF121212))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TournamentStatusBadge(TournamentStatus.UPCOMING)
        TournamentStatusBadge(TournamentStatus.STARTS_SOON)
        TournamentStatusBadge(TournamentStatus.ONGOING)
        TournamentStatusBadge(TournamentStatus.ROOM_OPEN)
        TournamentStatusBadge(TournamentStatus.COMPLETED)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun Preview_ModeChip() {
    Row(
        modifier = Modifier
            .background(Color(0xFF121212))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ModeChip(TournamentMode.SOLO)
        ModeChip(TournamentMode.DUO)
        ModeChip(TournamentMode.SQUAD)
        ModeChip(TournamentMode.TRIO)
        ModeChip(TournamentMode.CUSTOM)
    }
}

