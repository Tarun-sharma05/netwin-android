package com.cehpoint.netwin.presentation.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cehpoint.netwin.R
import com.cehpoint.netwin.domain.model.Tournament
import com.cehpoint.netwin.domain.model.TournamentMode
import com.cehpoint.netwin.domain.model.TournamentStatus
import com.cehpoint.netwin.presentation.components.PullRefreshComponent
import com.cehpoint.netwin.presentation.components.TournamentCapacityIndicator
import com.cehpoint.netwin.presentation.components.statusBarPadding
import com.cehpoint.netwin.presentation.navigation.Screen
import com.cehpoint.netwin.presentation.navigation.ScreenRoutes
import com.cehpoint.netwin.presentation.navigation.TournamentRegistration
import com.cehpoint.netwin.presentation.viewmodels.TournamentEvent
import com.cehpoint.netwin.presentation.viewmodels.TournamentFilter
import com.cehpoint.netwin.presentation.viewmodels.TournamentState
import com.cehpoint.netwin.presentation.viewmodels.TournamentViewModel
import com.cehpoint.netwin.utils.NGNTransactionUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun TournamentsScreenUI(navController: NavController, viewModel: TournamentViewModel = hiltViewModel()) {
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
                isRefreshing = false // Don't show refresh indicator in top bar
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
                    .background(Color(0xFF121212))
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
                            CircularProgressIndicator(color = Color(0xFF00BCD4))
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

@Composable
fun TournamentsTopBar(walletBalance: Int, currency: String, isRefreshing: Boolean = false) {
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
                color = Color(0xFF00BCD4), // Cyan color
                fontWeight = FontWeight.Bold
            )
            
            // Show refresh indicator next to title when refreshing
            if (isRefreshing) {
                Spacer(modifier = Modifier.width(8.dp))
                CircularProgressIndicator(
                    color = Color(0xFF00BCD4),
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = Color(0xFF00BCD4), // Cyan color
                modifier = Modifier.size(24.dp)
            )
            Text(
                    NGNTransactionUtils.formatAmount(walletBalance.toDouble(), currency),
                modifier = Modifier.padding(start = 4.dp),
                color = Color(0xFF00BCD4), // Cyan color
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize
            )
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
            contentDescription = null,
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
                    contentDescription = null,
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
            .background(Color(0xFF121212)) // Explicit dark background
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
            .background(Color(0xFF121212)) // Explicit dark background
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
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF00BCD4) else Color(0xFF2A2A2A) // Cyan or dark gray
        ),
        modifier = Modifier.height(40.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color.Black else Color.White, // Black on cyan, white on dark gray
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text,
            color = if (selected) Color.Black else Color.White, // Black on cyan, white on dark gray
            fontSize = 14.sp
        )
    }
}


@Composable
fun TournamentCard(
    tournament: Tournament,
    viewModel: TournamentViewModel,
    onCardClick: () -> Unit,
    navController: NavController,
    currency: String
) {
    // ✅ IMPROVED: Use ViewModel states + local fallback for better UX
    val registrationStatus by viewModel.registrationStatus.collectAsState()
    val showKycRequiredDialog by viewModel.showKycRequiredDialog.collectAsState()
    val isRegistering by viewModel.isRegistering.collectAsState() // ✅ Use ViewModel state
    val registrationError by viewModel.registrationError.collectAsState() // ✅ Add error handling
    var localIsRegistering by remember { mutableStateOf(false) } // ✅ Keep local state as backup
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser

    // ✅ FIXED: Remove nullable access
    val tournamentState by viewModel.state.collectAsState()
    val remainingTime = tournamentState?.countdowns[tournament.id] ?: ""

    // Existing registration status check
    LaunchedEffect(tournament.id, user?.uid) {
        if (user != null) {
            viewModel.checkRegistrationStatus(tournament.id, user.uid)
        }
    }

    // ✅ NEW: Error handling with Toast
    registrationError?.let { error ->
        LaunchedEffect(error) {
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.clearRegistrationError()
        }
    }

    // ✅ IMPROVED: Better registration completion handling
    LaunchedEffect(viewModel.lastProcessedTournamentId.collectAsState().value) {
        val processedId = viewModel.lastProcessedTournamentId.value
        if (processedId == tournament.id) {
            localIsRegistering = false
            viewModel.clearLastProcessedTournamentId()
        }
    }

    // KYC Dialog
    if (showKycRequiredDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.clearKycDialog() },
            title = { Text("KYC Required") },
            text = { Text("You must complete KYC verification to register for tournaments.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearKycDialog()
                    navController.navigate(ScreenRoutes.KycScreen)
                }) {
                    Text("Go to KYC")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.clearKycDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ✅ KEEP: Your complete Card UI structure
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column { // Main content column for the card
            // ✅ KEEP: IMAGE / OVERLAY SECTION
            Box(
                modifier = Modifier
                    .height(140.dp)
                    .fillMaxWidth()
            ) {
                // Background placeholder or Image
                if (tournament.bannerImage.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF2A2A2A), Color(0xFF1E1E1E))
                                )
                            )
                    )
                    // Centered Info when no image
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = "Tournament Event",
                                tint = Color(0xFF00BCD4),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = tournament.name,
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tournament.map,
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    AsyncImage(
                        model = tournament.bannerImage,
                        contentDescription = "Tournament Banner: ${tournament.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Overlays on top of the image/placeholder
                TournamentStatusBadge(
                    status = tournament.computedStatus,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
                ModeChip(
                    tournament.mode,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
                if (remainingTime.isNotBlank()) {
                    Text(
                        text = remainingTime,
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                RoundedCornerShape(topEnd = 8.dp, bottomStart = 12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // ✅ KEEP: INFO SECTION BELOW IMAGE
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tournament Name and Map
                Text(
                    text = tournament.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.PinDrop,
                        contentDescription = "Map",
                        tint = Color(0xFF00BCD4),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = tournament.map,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // ✅ KEEP: Info Chips (Prize, Kill, Fee) - This was missing in my version!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoChip("Prize Pool", NGNTransactionUtils.formatAmount(tournament.prizePool.toDouble(), currency))
                    InfoChip("Per Kill", NGNTransactionUtils.formatAmount(tournament.killReward ?: 0.0, currency))
                    InfoChip("Entry Fee", if (tournament.entryFee > 0) NGNTransactionUtils.formatAmount(tournament.entryFee.toDouble(), currency) else "Free")
                }

                // ✅ KEEP: Tournament Capacity Indicator
                TournamentCapacityIndicator(
                    registeredTeams = tournament.registeredTeams,
                    maxTeams = tournament.maxTeams,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )

                // ✅ IMPROVED: Registration Button with better state management
                val canRegister = tournament.computedStatus == TournamentStatus.UPCOMING &&
                        tournament.maxTeams > tournament.registeredTeams &&
                        registrationStatus != true

                val isFull = tournament.maxTeams <= tournament.registeredTeams
                val showSpinner = isRegistering || localIsRegistering // ✅ Use either state

                if (tournament.computedStatus == TournamentStatus.UPCOMING ||
                    tournament.computedStatus == TournamentStatus.ROOM_OPEN ||
                    tournament.computedStatus == TournamentStatus.STARTS_SOON) {

                    Button(
//                        onClick = {
//                            if (canRegister && !showSpinner) {
//                                localIsRegistering = true // ✅ Set local state immediately for UI feedback
//                                viewModel.handleEvent(
//                                    TournamentEvent.RegisterForTournament(
//                                        tournament = tournament,
//                                        inGameId = ""
//                                    )
//                                )
//                            }
//                        },
                        // Replace the existing register button onClick with:
                        onClick = {
                            if (canRegister && !showSpinner) {
                                // Navigate to registration flow instead of direct registration
                                navController.navigate(
                                    TournamentRegistration(
                                        tournamentId = tournament.id,
                                        stepIndex = 1
                                    )
                                )
                            }
                        },

                                modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = canRegister && !showSpinner, // ✅ Use combined state
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                registrationStatus == true -> Color(0xFF4CAF50)
                                isFull -> Color.DarkGray
                                else -> Color(0xFF00BCD4)
                            },
                            contentColor = Color.White, // ✅ Fixed: Use white text for better contrast
                            disabledContainerColor = Color.DarkGray.copy(alpha = 0.5f),
                            disabledContentColor = Color.White.copy(alpha = 0.5f)
                        )
                    ) {
                        if (showSpinner) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White, // ✅ Fixed: White spinner
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Registering...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        } else {
                            Text(
                                text = when {
                                    registrationStatus == true -> "✓ Registered"
                                    isFull -> "Tournament Full"
                                    else -> "Join Tournament"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                } else if (tournament.computedStatus == TournamentStatus.COMPLETED) {
                    Text(
                        textAlign = TextAlign.Center,
                        text = "Tournament Ended",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}




//
//
//@Composable
//fun TournamentCard(
//    tournament: Tournament,
//    viewModel: TournamentViewModel,
//    onCardClick: () -> Unit,
//    navController: NavController,
//    currency: String
//) {
//    // ✅ IMPROVED: Use ViewModel states + local fallback for better UX
//    val registrationStatus by viewModel.registrationStatus.collectAsState()
//    val showKycRequiredDialog by viewModel.showKycRequiredDialog.collectAsState()
//    val isRegistering by viewModel.isRegistering.collectAsState() // ✅ Use ViewModel state
//    val registrationError by viewModel.registrationError.collectAsState() // ✅ Add error handling
//    var localIsRegistering by remember { mutableStateOf(false) } // ✅ Keep local state as backup
//    val context = LocalContext.current
//    val user = FirebaseAuth.getInstance().currentUser
//
//    // ✅ FIXED: Remove nullable access
//    val tournamentState by viewModel.state.collectAsState()
//    val remainingTime = tournamentState?.countdowns[tournament.id] ?: ""
//
//    // Existing registration status check
//    LaunchedEffect(tournament.id, user?.uid) {
//        if (user != null) {
//            viewModel.checkRegistrationStatus(tournament.id, user.uid)
//        }
//    }
//
//    // ✅ NEW: Error handling with Toast
//    registrationError?.let { error ->
//        LaunchedEffect(error) {
//            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
//            viewModel.clearRegistrationError()
//        }
//    }
//
//    // ✅ IMPROVED: Better registration completion handling
//    LaunchedEffect(viewModel.lastProcessedTournamentId.collectAsState().value) {
//        val processedId = viewModel.lastProcessedTournamentId.value
//        if (processedId == tournament.id) {
//            localIsRegistering = false
//            viewModel.clearLastProcessedTournamentId()
//        }
//    }
//
//    // KYC Dialog
//    if (showKycRequiredDialog) {
//        AlertDialog(
//            onDismissRequest = { viewModel.clearKycDialog() },
//            title = { Text("KYC Required") },
//            text = { Text("You must complete KYC verification to register for tournaments.") },
//            confirmButton = {
//                Button(onClick = {
//                    viewModel.clearKycDialog()
//                    navController.navigate(ScreenRoutes.KycScreen)
//                }) {
//                    Text("Go to KYC")
//                }
//            },
//            dismissButton = {
//                OutlinedButton(onClick = { viewModel.clearKycDialog() }) {
//                    Text("Cancel")
//                }
//            }
//        )
//    }
//
//    // ✅ KEEP: Your complete Card UI structure
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 16.dp)
//            .clickable(onClick = onCardClick),
//        shape = RoundedCornerShape(12.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = Color(0xFF1E1E1E)
//        ),
//        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
//    ) {
//        Column { // Main content column for the card
//            // ✅ KEEP: IMAGE / OVERLAY SECTION
//            Box(
//                modifier = Modifier
//                    .height(140.dp)
//                    .fillMaxWidth()
//            ) {
//                // Background placeholder or Image
//                if (tournament.bannerImage.isNullOrEmpty()) {
//                    Box(
//                        modifier = Modifier
//                            .fillMaxSize()
//                            .background(
//                                brush = Brush.verticalGradient(
//                                    colors = listOf(Color(0xFF2A2A2A), Color(0xFF1E1E1E))
//                                )
//                            )
//                    )
//                    // Centered Info when no image
//                    Box(
//                        modifier = Modifier.fillMaxSize(),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Column(
//                            horizontalAlignment = Alignment.CenterHorizontally,
//                            verticalArrangement = Arrangement.Center,
//                            modifier = Modifier.padding(8.dp)
//                        ) {
//                            Icon(
//                                Icons.Default.EmojiEvents,
//                                contentDescription = "Tournament Event",
//                                tint = Color(0xFF00BCD4),
//                                modifier = Modifier.size(40.dp)
//                            )
//                            Spacer(modifier = Modifier.height(8.dp))
//                            Text(
//                                text = tournament.name,
//                                color = Color.White,
//                                style = MaterialTheme.typography.titleSmall,
//                                fontWeight = FontWeight.Bold,
//                                textAlign = TextAlign.Center,
//                                maxLines = 2,
//                                overflow = TextOverflow.Ellipsis
//                            )
//                            Spacer(modifier = Modifier.height(4.dp))
//                            Text(
//                                text = tournament.map,
//                                color = Color.White.copy(alpha = 0.7f),
//                                style = MaterialTheme.typography.bodySmall,
//                                textAlign = TextAlign.Center
//                            )
//                        }
//                    }
//                } else {
//                    AsyncImage(
//                        model = tournament.bannerImage,
//                        contentDescription = "Tournament Banner: ${tournament.name}",
//                        contentScale = ContentScale.Crop,
//                        modifier = Modifier.fillMaxSize()
//                    )
//                }
//
//                // Overlays on top of the image/placeholder
//                TournamentStatusBadge(
//                    status = tournament.computedStatus,
//                    modifier = Modifier
//                        .align(Alignment.TopStart)
//                        .padding(8.dp)
//                )
//                ModeChip(
//                    tournament.mode,
//                    modifier = Modifier
//                        .align(Alignment.TopEnd)
//                        .padding(8.dp)
//                )
//                if (remainingTime.isNotBlank()) {
//                    Text(
//                        text = remainingTime,
//                        color = Color.White,
//                        style = MaterialTheme.typography.bodySmall,
//                        fontWeight = FontWeight.Bold,
//                        modifier = Modifier
//                            .align(Alignment.BottomStart)
//                            .background(
//                                Color.Black.copy(alpha = 0.5f),
//                                RoundedCornerShape(topEnd = 8.dp, bottomStart = 12.dp)
//                            )
//                            .padding(horizontal = 8.dp, vertical = 4.dp)
//                    )
//                }
//            }
//
//            // ✅ KEEP: INFO SECTION BELOW IMAGE
//            Column(
//                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
//                verticalArrangement = Arrangement.spacedBy(10.dp)
//            ) {
//                // Tournament Name and Map
//                Text(
//                    text = tournament.name,
//                    style = MaterialTheme.typography.titleMedium,
//                    color = Color.White,
//                    fontWeight = FontWeight.Bold,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(4.dp)
//                ) {
//                    Icon(
//                        Icons.Default.PinDrop,
//                        contentDescription = "Map",
//                        tint = Color(0xFF00BCD4),
//                        modifier = Modifier.size(16.dp)
//                    )
//                    Text(
//                        text = tournament.map,
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = Color.White.copy(alpha = 0.8f)
//                    )
//                }
//
//                // ✅ KEEP: Info Chips (Prize, Kill, Fee) - This was missing in my version!
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    InfoChip("Prize Pool", NGNTransactionUtils.formatAmount(tournament.prizePool.toDouble(), currency))
//                    InfoChip("Per Kill", NGNTransactionUtils.formatAmount(tournament.killReward ?: 0.0, currency))
//                    InfoChip("Entry Fee", if (tournament.entryFee > 0) NGNTransactionUtils.formatAmount(tournament.entryFee.toDouble(), currency) else "Free")
//                }
//
//                // ✅ KEEP: Tournament Capacity Indicator
//                TournamentCapacityIndicator(
//                    registeredTeams = tournament.registeredTeams,
//                    maxTeams = tournament.maxTeams,
//                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
//                )
//
//                // ✅ IMPROVED: Registration Button with better state management
//                val canRegister = tournament.computedStatus == TournamentStatus.UPCOMING &&
//                        tournament.maxTeams > tournament.registeredTeams &&
//                        registrationStatus != true
//
//                val isFull = tournament.maxTeams <= tournament.registeredTeams
//                val showSpinner = isRegistering || localIsRegistering // ✅ Use either state
//
//                if (tournament.computedStatus == TournamentStatus.UPCOMING ||
//                    tournament.computedStatus == TournamentStatus.ROOM_OPEN ||
//                    tournament.computedStatus == TournamentStatus.STARTS_SOON) {
//
//                    Button(
//                        onClick = {
//                            if (canRegister && !showSpinner) {
//                                localIsRegistering = true // ✅ Set local state immediately for UI feedback
//                                viewModel.handleEvent(
//                                    TournamentEvent.RegisterForTournament(
//                                        tournament = tournament,
//                                        inGameId = ""
//                                    )
//                                )
//                            }
//                        },
//                        modifier = Modifier.fillMaxWidth().height(48.dp),
//                        enabled = canRegister && !showSpinner, // ✅ Use combined state
//                        shape = RoundedCornerShape(8.dp),
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = when {
//                                registrationStatus == true -> Color(0xFF4CAF50)
//                                isFull -> Color.DarkGray
//                                else -> Color(0xFF00BCD4)
//                            },
//                            contentColor = Color.White, // ✅ Fixed: Use white text for better contrast
//                            disabledContainerColor = Color.DarkGray.copy(alpha = 0.5f),
//                            disabledContentColor = Color.White.copy(alpha = 0.5f)
//                        )
//                    ) {
//                        if (showSpinner) {
//                            Row(
//                                verticalAlignment = Alignment.CenterVertically,
//                                horizontalArrangement = Arrangement.spacedBy(8.dp)
//                            ) {
//                                CircularProgressIndicator(
//                                    modifier = Modifier.size(20.dp),
//                                    color = Color.White, // ✅ Fixed: White spinner
//                                    strokeWidth = 2.dp
//                                )
//                                Text(
//                                    text = "Registering...",
//                                    fontWeight = FontWeight.Bold,
//                                    fontSize = 16.sp
//                                )
//                            }
//                        } else {
//                            Text(
//                                text = when {
//                                    registrationStatus == true -> "✓ Registered"
//                                    isFull -> "Tournament Full"
//                                    else -> "Join Tournament"
//                                },
//                                fontWeight = FontWeight.Bold,
//                                fontSize = 16.sp
//                            )
//                        }
//                    }
//                } else if (tournament.computedStatus == TournamentStatus.COMPLETED) {
//                    Text(
//                        textAlign = TextAlign.Center,
//                        text = "Tournament Ended",
//                        color = Color.White.copy(alpha = 0.7f),
//                        style = MaterialTheme.typography.bodyMedium,
//                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
//                    )
//                }
//            }
//        }
//    }
//}
//
//


//
//@Composable
//fun TournamentCard(
//    tournament: Tournament,
//    viewModel: TournamentViewModel,
//    onCardClick: () -> Unit,
//    navController: NavController,
//    currency: String
//) {
//    // Collect registration state specifically for THIS tournament if your state is granular.
//    // For this example, using a local 'isRegistering' state for the spinner.
//    // You'll need to observe a state from the ViewModel that indicates when registration *completes*
//    // for this specific tournament, or a global registration completion event.
//    var isRegistering by remember { mutableStateOf(false) }
//
//    // Existing state collections
//    val registrationStatus by viewModel.registrationStatus.collectAsState() // Keep this for overall status updates if used elsewhere
//    val showKycRequiredDialog by viewModel.showKycRequiredDialog.collectAsState()
//    val context = LocalContext.current
//    val user = FirebaseAuth.getInstance().currentUser
//
//    // Existing LaunchedEffects for checking registration and remaining time
//    LaunchedEffect(tournament.id, user?.uid) {
//        if (user != null) {
//            viewModel.checkRegistrationStatus(tournament.id, user.uid)
//        }
//    }
//
////    var remainingTime by remember { mutableStateOf("") }
////    LaunchedEffect(tournament.startTime, tournament.completedAt, tournament.computedStatus) { // Added computedStatus
////        while (true) {
////            if (tournament.computedStatus == TournamentStatus.COMPLETED || tournament.computedStatus == null) {
////                remainingTime = "" // Clear if completed or status is null
////                break
////            }
////            val currentTime = System.currentTimeMillis()
////            remainingTime = when (tournament.computedStatus) {
////                TournamentStatus.UPCOMING -> {
////                    val timeDiff = (tournament.startTime ?: 0L) - currentTime
////                    if (timeDiff <= 0) {
////                        "Starting..."
////                    } else {
////                        val hours = timeDiff / (60 * 60 * 1000)
////                        val minutes = (timeDiff % (60 * 60 * 1000)) / (60 * 1000)
////                        val seconds = (timeDiff % (60 * 1000)) / 1000
////                        when {
////                            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
////                            minutes > 0 -> String.format("%02d:%02d", minutes, seconds)
////                            else -> String.format("%02ds", seconds) // Shorter "s" for seconds
////                        }
////                    }
////                }
////                TournamentStatus.ONGOING -> {
////                    val timeDiff = (tournament.completedAt ?: 0L) - currentTime
////                    if (timeDiff <= 0) {
////                        "Ending..."
////                    } else {
////                        val hours = timeDiff / (60 * 60 * 1000)
////                        val minutes = (timeDiff % (60 * 60 * 1000)) / (60 * 1000)
////                        val seconds = (timeDiff % (60 * 1000)) / 1000
////                        when {
////                            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
////                            minutes > 0 -> String.format("%02d:%02d", minutes, seconds)
////                            else -> String.format("%02ds", seconds) // Shorter "s" for seconds
////                        }
////                    }
////                }
////                // Handle other statuses if necessary, or break
////                else -> {
////                    remainingTime = "" // Clear for other statuses like COMPLETED
////                    break
////                }
////            }
////            if (remainingTime == "Starting..." || remainingTime == "Ending...") {
////                // Potentially refresh tournament data or re-evaluate status after a short delay
////                delay(5000) // Delay then loop will re-evaluate, or trigger a refresh from VM
////                // For MVP, simply showing the text might be enough
////            }
////            delay(1000) // Update every second
////        }
////    }
//
//    val tournamentState by viewModel.state.collectAsState()
//    val remainingTime = tournamentState?.countdowns[tournament.id] ?: ""
//
//
//
//    // This LaunchedEffect will listen to a hypothetical state from your ViewModel
//    // that tells when a registration attempt has finished (succeeded or failed).
//    // You need to implement this in your ViewModel.
//    // For example, viewModel.tournamentRegistrationCompleteFlow: Flow<String?>
//    // where String is the tournamentId that just finished registration.
//    LaunchedEffect(viewModel.lastProcessedTournamentId.collectAsState().value) {
//        val processedId = viewModel.lastProcessedTournamentId.value
//        if (processedId == tournament.id) {
//            isRegistering = false
//            viewModel.clearLastProcessedTournamentId() // Reset the signal in ViewModel
//        }
//    }
//
//
//    if (showKycRequiredDialog) {
//        AlertDialog(
//            onDismissRequest = { viewModel.clearKycDialog() },
//            title = { Text("KYC Required") },
//            text = { Text("You must complete KYC verification to register for tournaments.") },
//            confirmButton = {
//                Button(onClick = {
//                    viewModel.clearKycDialog()
//                    navController.navigate(ScreenRoutes.KycScreen)
//                }) {
//                    Text("Go to KYC")
//                }
//            },
//            dismissButton = {
//                OutlinedButton(onClick = { viewModel.clearKycDialog() }) {
//                    Text("Cancel")
//                }
//            }
//        )
//    }
//
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 16.dp) // Keep vertical padding in LazyColumn item
//            .clickable(onClick = onCardClick),
//        shape = RoundedCornerShape(12.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = Color(0xFF1E1E1E) // Specified dark theme card color
//        ),
//        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Subtle elevation
//    ) {
//        Column { // Main content column for the card
//            // --- IMAGE / OVERLAY SECTION ---
//            Box(
//                modifier = Modifier
//                    .height(140.dp) // Consider making this dynamic or aspect ratio based
//                    .fillMaxWidth()
//            ) {
//                // Background placeholder or Image
//                if (tournament.bannerImage.isNullOrEmpty()) {
//                    Box( // Gradient Placeholder
//                        modifier = Modifier
//                            .fillMaxSize()
//                            .background(
//                                brush = Brush.verticalGradient(
//                                    colors = listOf(Color(0xFF2A2A2A), Color(0xFF1E1E1E))
//                                )
//                            )
//                    )
//                    // Centered Info when no image
//                    Box(
//                        modifier = Modifier.fillMaxSize(),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Column(
//                            horizontalAlignment = Alignment.CenterHorizontally,
//                            verticalArrangement = Arrangement.Center,
//                            modifier = Modifier.padding(8.dp)
//                        ) {
//                            Icon(
//                                Icons.Default.EmojiEvents, // Using a relevant icon
//                                contentDescription = "Tournament Event",
//                                tint = Color(0xFF00BCD4), // Accent color
//                                modifier = Modifier.size(40.dp)
//                            )
//                            Spacer(modifier = Modifier.height(8.dp))
//                            Text(
//                                text = tournament.name,
//                                color = Color.White,
//                                style = MaterialTheme.typography.titleSmall, // Adjusted for overlay
//                                fontWeight = FontWeight.Bold,
//                                textAlign = TextAlign.Center,
//                                maxLines = 2,
//                                overflow = TextOverflow.Ellipsis
//                            )
//                            Spacer(modifier = Modifier.height(4.dp))
//                            Text(
//                                text = tournament.map,
//                                color = Color.White.copy(alpha = 0.7f),
//                                style = MaterialTheme.typography.bodySmall,
//                                textAlign = TextAlign.Center
//                            )
//                        }
//                    }
//                } else {
//                    AsyncImage(
//                        model = tournament.bannerImage,
//                        contentDescription = "Tournament Banner: ${tournament.name}",
//                        contentScale = ContentScale.Crop,
//                        modifier = Modifier.fillMaxSize()
//                    )
//                }
//
//                // Overlays on top of the image/placeholder (Status, Mode, Time)
//                TournamentStatusBadge(
//                    status = tournament.computedStatus,
//                    modifier = Modifier
//                        .align(Alignment.TopStart)
//                        .padding(8.dp) // Adjusted padding
//                )
//                ModeChip(
//                    tournament.mode,
//                    modifier = Modifier
//                        .align(Alignment.TopEnd)
//                        .padding(8.dp) // Adjusted padding
//                )
//                if (remainingTime.isNotBlank()) {
//                    Text(
//                        text = remainingTime,
//                        color = Color.White,
//                        style = MaterialTheme.typography.bodySmall,
//                        fontWeight = FontWeight.Bold,
//                        modifier = Modifier
//                            .align(Alignment.BottomStart)
//                            .background(
//                                Color.Black.copy(alpha = 0.5f),
//                                RoundedCornerShape(topEnd = 8.dp, bottomStart = 12.dp) // Match card corner
//                            )
//                            .padding(horizontal = 8.dp, vertical = 4.dp)
//                    )
//                }
//            }
//
//            // --- INFO SECTION BELOW IMAGE ---
//            Column(
//                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), // Consistent padding
//                verticalArrangement = Arrangement.spacedBy(10.dp) // Consistent spacing
//            ) {
//                // Tournament Name and Map (Improved Hierarchy)
//                Text(
//                    text = tournament.name,
//                    style = MaterialTheme.typography.titleMedium, // More prominent
//                    color = Color.White,
//                    fontWeight = FontWeight.Bold,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(4.dp)
//                ) {
//                    Icon(
//                        Icons.Default.PinDrop,
//                        contentDescription = "Map",
//                        tint = Color(0xFF00BCD4), // Accent color
//                        modifier = Modifier.size(16.dp)
//                    )
//                    Text(
//                        text = tournament.map,
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = Color.White.copy(alpha = 0.8f)
//                    )
//                }
//
//                // Info Chips (Prize, Kill, Fee)
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween // Or SpaceEvenly
//                ) {
//                    InfoChip("Prize Pool", NGNTransactionUtils.formatAmount(tournament.prizePool.toDouble(), currency))
//                    InfoChip("Per Kill", NGNTransactionUtils.formatAmount(tournament.killReward ?: 0.0, currency))
//                    InfoChip("Entry Fee", if (tournament.entryFee > 0) NGNTransactionUtils.formatAmount(tournament.entryFee.toDouble(), currency) else "Free")
//                }
//
//                // ----- NEW: TOURNAMENT CAPACITY INDICATOR -----
//                TournamentCapacityIndicator(
//                    registeredTeams = tournament.registeredTeams,
//                    maxTeams = tournament.maxTeams,
//                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp) // Add some spacing around it
//                )
//                // ----- END OF CAPACITY INDICATOR -----
//
//
//                // ----- MODIFIED: DYNAMIC BUTTON WITH LOADING SPINNER -----
////                val canRegister = tournament.computedStatus == TournamentStatus.UPCOMING && tournament.maxTeams > tournament.registeredTeams && !tournament.isRegistered // Add isRegistered check if available
//                val canRegister = tournament.computedStatus == TournamentStatus.UPCOMING &&
//                        tournament.maxTeams > tournament.registeredTeams &&
//                        registrationStatus != true
//
//                val isFull = tournament.maxTeams <= tournament.registeredTeams
//
//                if (tournament.computedStatus == TournamentStatus.UPCOMING || tournament.computedStatus == TournamentStatus.ROOM_OPEN || tournament.computedStatus == TournamentStatus.STARTS_SOON) { // Show button for these statuses
//                    Button(
//                        onClick = {
//                            if (canRegister && !isRegistering) {
//                                isRegistering = true // Show spinner
//                                // Consider passing a callback to the ViewModel to reset isRegistering
//                                // once the operation completes, or use a SharedFlow/StateFlow from VM.
//                                viewModel.handleEvent(
//                                    TournamentEvent.RegisterForTournament(
//                                        tournament = tournament, // Pass the whole tournament if needed by VM
//                                        inGameId = "" // Or get this from user input if required
//                                    )
//                                )
//                            }
//                        },
//                        modifier = Modifier.fillMaxWidth().height(48.dp), // Standard button height
//                        enabled = canRegister && !isRegistering, // Disable if cannot register or already registering
//                        shape = RoundedCornerShape(8.dp),
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = if (isFull && registrationStatus != true) Color.DarkGray else Color(0xFF00BCD4), // Use accent or Gray if full
//                            contentColor = Color.Black, // Text color for the button
//                            disabledContainerColor = Color.DarkGray.copy(alpha = 0.5f),
//                            disabledContentColor = Color.White.copy(alpha = 0.5f)
//                        )
//                    ) {
//                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
//                            if (isRegistering) {
//                                CircularProgressIndicator(
//                                    modifier = Modifier.size(24.dp),
//                                    color = Color.Black, // Spinner color matching button text
//                                    strokeWidth = 2.dp
//                                )
//                            } else {
//                                Text(
//                                    text = when {
//                                        registrationStatus == true -> "Registered" // If you have this state
//                                        isFull -> "Full"
//                                        else -> "Join Now"
//                                    },
//                                    fontWeight = FontWeight.Bold,
//                                    fontSize = 16.sp // Readable button text
//                                )
//                            }
//                        }
//                    }
//                } else if (tournament.computedStatus == TournamentStatus.COMPLETED){
//                    Text(
//                        textAlign = TextAlign.Center,
//                        text = "Tournament Ended",
//                        color = Color.White.copy(alpha = 0.7f),
//                        style = MaterialTheme.typography.bodyMedium,
//                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
//                    )
//                }
//                // Else: No button or specific message for other statuses like ONGOING (unless you want one e.g. "View Details")
//            }
//        }
//    }
//}

// Keep your existing TournamentStatusBadge, ModeChip, and InfoChip composables.
// Make sure they use colors that are visible on your Color(0xFF1E1E1E) card background.
// Example for InfoChip (ensure its colors are theme-aware or explicitly set for dark):
@Composable
fun InfoChip(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp) // Add some padding between chips
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f), // Muted for dark theme
            style = MaterialTheme.typography.labelSmall // Use Material styles
        )
        Text(
            text = value,
            color = Color.White, // Prominent for dark theme
            style = MaterialTheme.typography.bodyMedium, // Use Material styles
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TournamentStatusBadge(
    status: TournamentStatus,
    modifier: Modifier = Modifier
) {
    val (color, icon, label) = when (status) {
        TournamentStatus.ONGOING -> Triple(Color(0xFFD32F2F), Icons.Filled.Videocam, "LIVE")
        TournamentStatus.UPCOMING -> Triple(Color(0xFF1976D2), Icons.Filled.Event, "UPCOMING")
        TournamentStatus.STARTS_SOON -> Triple(Color(0xFFFFA000), Icons.Filled.Schedule, "STARTS SOON")
        TournamentStatus.ROOM_OPEN -> Triple(Color(0xFF388E3C), Icons.Filled.LockOpen, "ROOM OPEN")
        TournamentStatus.COMPLETED -> Triple(Color(0xFF9E9E9E), Icons.Filled.CheckCircle, "COMPLETED")
    }
    Surface(
        modifier = modifier,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .background(color, shape = RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun ModeChip(mode: TournamentMode, modifier: Modifier = Modifier) {
    val (bgColor, text) = when (mode) {
        TournamentMode.SOLO -> Pair(Color(0xFF42A5F5), "Solo")
        TournamentMode.DUO -> Pair(Color(0xFFAB47BC), "Duo")
        TournamentMode.SQUAD -> Pair(Color(0xFF26A69A), "Squad")
        TournamentMode.TRIO -> Pair(Color(0xFFFF7043), "Trio")
        TournamentMode.CUSTOM -> Pair(Color(0xFFFFCA28), "Custom")
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Cyan,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

