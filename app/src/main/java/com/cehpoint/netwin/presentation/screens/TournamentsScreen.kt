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
import androidx.compose.material.icons.outlined.EmojiEvents
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import com.cehpoint.netwin.presentation.components.statusBarPadding
import com.cehpoint.netwin.presentation.navigation.Screen
import com.cehpoint.netwin.presentation.viewmodels.TournamentEvent
import com.cehpoint.netwin.presentation.viewmodels.TournamentFilter
import com.cehpoint.netwin.presentation.viewmodels.TournamentState
import com.cehpoint.netwin.presentation.viewmodels.TournamentViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentsScreenUI(navController: NavController, viewModel: TournamentViewModel = hiltViewModel()) {
    val scrollBehaviour = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val state by viewModel.state.collectAsState()
    val registrationState by viewModel.registrationState.collectAsState()
    val walletBalance by viewModel.walletBalance.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val context = LocalContext.current

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
        Log.d("TournamentsScreen", "LaunchedEffect triggered - Loading tournaments")
        viewModel.handleEvent(TournamentEvent.LoadTournaments)
    }

    Scaffold(
//        modifier = Modifier.fillMaxSize()
//            .nestedScroll(scrollBehaviour.nestedScrollConnection),
        topBar = { TournamentsTopBar(walletBalance = walletBalance.toInt()) },
        // Uncomment and implement if you have a bottom nav bar
        // bottomBar = { BottomNavBar(navController) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(16.dp)) }
            item { WelcomeCard(userName = userName, onJoinClick = { /* TODO: Handle join click */ }) }
            item { Spacer(Modifier.height(25.dp)) }
            item {
                Text(
                    text = "Available Tournaments",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item { Spacer(Modifier.height(14.dp)) }
            item {
                TournamentsFilters(
                    selectedFilter = tournamentState.selectedFilter.name,
                    onFilterChange = { filter ->
                        viewModel.handleEvent(TournamentEvent.FilterTournaments(TournamentFilter.valueOf(filter)))
                    }
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
            // Loading/Error
                if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
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
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = error,
                            color = Color.Red,
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
                        navController = navController
                    )
                }
            }
        }
    }
}

@Composable
fun TournamentsTopBar(walletBalance: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF121212)) // Explicit dark background
            .statusBarPadding()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "NETWIN",
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF00BCD4), // Cyan color
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = Color(0xFF00BCD4), // Cyan color
                modifier = Modifier.size(24.dp)
            )
            Text(
                "₹$walletBalance",
                modifier = Modifier.padding(start = 4.dp),
                color = Color(0xFF00BCD4), // Cyan color
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun WelcomeCard(
    userName: String,
    onJoinClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.DarkGray)
    ) {
        // Background image (replace with your own image in res/drawable)
        Image(
            painter = painterResource(id = R.drawable.esport), // <-- your image here
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        // Optional: overlay for better text contrast
        Box(
            Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Welcome Back, $userName!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Ready to compete? Join a tournament and showcase your skills!",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
            Button(
                onClick = onJoinClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(40.dp)

            ) {
                Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = Color.Black)
                Spacer(Modifier.width(8.dp))
                Text("Join Tournament", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TournamentsFilters(
    selectedFilter: String,
    onFilterChange: (String) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF121212)) // Explicit dark background
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        FilterChip(
            text = "Filter",
            icon = Icons.Default.FilterList,
            selected = selectedFilter == "Filter",
            onClick = { onFilterChange("Filter") }
        )
        Spacer(Modifier.width(8.dp))
        FilterChip(
            text = "All Games",
            icon = Icons.Default.EmojiEvents,
            selected = selectedFilter == "All Games",
            onClick = { onFilterChange("All Games") }
        )
        Spacer(Modifier.width(8.dp))
        FilterChip(
            text = "All Maps",
            icon = Icons.Default.Map,
            selected = selectedFilter == "All Maps",
            onClick = { onFilterChange("All Maps") }
        )
    }

    Spacer(Modifier.width(8.dp))

    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF121212)) // Explicit dark background
            .padding(horizontal = 16.dp, vertical = 8.dp)
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
        modifier = Modifier.height(36.dp),
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
fun TournamentList(
    tournaments: List<Tournament>,
    onJoin: (String) -> Unit,
    navController: NavController
) {
    Log.d("TournamentsScreen", "TournamentList composable called with ${tournaments.size} tournaments")
    Log.d("TournamentsScreen", "TournamentList - First tournament: ${tournaments.firstOrNull()?.name}")
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)) // Explicit dark background
    ) {
        if (tournaments.isEmpty()) {
            Log.d("TournamentsScreen", "TournamentList - No tournaments available")
            Text(
                text = "No tournaments available",
                color = Color.White, // Explicit white text
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF121212)), // Explicit dark background
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(tournaments) { tournament ->
                    Log.d("TournamentsScreen", "Rendering tournament card for: ${tournament.name}")
                    TournamentCard(
                        tournament = tournament,
                        viewModel = viewModel(),
                        onCardClick = {
                            Log.d("TournamentsScreen", "Navigating to tournament details: ${tournament.id}")
                            navController.navigate(Screen.TournamentDetails.createRoute(tournament.id))
                        },
                        navController = navController
                    )
                }
            }
        }
    }
}

@Composable
fun TournamentCard(
    tournament: Tournament,
    viewModel: TournamentViewModel,
    onCardClick: () -> Unit,
    navController: NavController
) {
    val registrationStatus by viewModel.registrationStatus.collectAsState()
    val showKycRequiredDialog by viewModel.showKycRequiredDialog.collectAsState()
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(tournament.id, user?.uid) {
        if (user != null) {
            viewModel.checkRegistrationStatus(tournament.id, user.uid)
        }
    }

    var remainingTime by remember { mutableStateOf("") }

    LaunchedEffect(tournament.startDate, tournament.endDate) {
        while (true) {
            val currentTime = System.currentTimeMillis()
            remainingTime = when (tournament.computedStatus) {
                TournamentStatus.UPCOMING -> {
            val timeDiff = tournament.startDate - currentTime
                    val hours = timeDiff / (60 * 60 * 1000)
                    val minutes = (timeDiff % (60 * 60 * 1000)) / (60 * 1000)
                    val seconds = (timeDiff % (60 * 1000)) / 1000
                    when {
                        hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
                        minutes > 0 -> String.format("%02d:%02d", minutes, seconds)
                        else -> String.format("%02d seconds", seconds)
                    }
                }
                TournamentStatus.STARTS_SOON -> "Starts Soon"
                TournamentStatus.ROOM_OPEN -> "Room Open"
                TournamentStatus.ONGOING -> {
                    val timeDiff = tournament.endDate - currentTime
                    val hours = timeDiff / (60 * 60 * 1000)
                    val minutes = (timeDiff % (60 * 60 * 1000)) / (60 * 1000)
                    val seconds = (timeDiff % (60 * 1000)) / 1000
                    when {
                        hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
                        minutes > 0 -> String.format("%02d:%02d", minutes, seconds)
                        else -> String.format("%02d seconds", seconds)
                    }
                }
                TournamentStatus.COMPLETED -> "Completed"
            }
            delay(1000)
        }
    }

    if (showKycRequiredDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.clearKycDialog() },
            title = { Text("KYC Required") },
            text = { Text("You must complete KYC verification to register for tournaments.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearKycDialog()
                    navController.navigate(com.cehpoint.netwin.presentation.navigation.ScreenRoutes.KycScreen)
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)) // Dark surface color
    ) {
        Column {
            Box(
                modifier = Modifier
                    .height(150.dp)
                    .fillMaxWidth()
            ) {
                AsyncImage(
                    model = tournament.imageUrl,
                    contentDescription = "Tournament Poster",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // You can add status badges here on top of the image
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
                            .padding(8.dp))
//                    Text(
//                        text = remainingTime,
//                        color = Color.White,
//                        fontSize = 14.sp,
//                        fontWeight = FontWeight.Bold
//                    )

            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tournament.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PinDrop,
                            contentDescription = "Map",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    Text(
                            text = tournament.map,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoChip("Prize Pool", "₹${tournament.prizePool}")
                    InfoChip("Per Kill", "₹${tournament.perKillPrize}")
                    InfoChip("Entry Fee", "₹${tournament.entryFee}")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Dynamic button based on tournament status
                when (tournament.computedStatus) {
                    TournamentStatus.UPCOMING -> {
                        Button(
                            onClick = {
                                viewModel.handleEvent(TournamentEvent.RegisterForTournament(tournament, inGameId = ""))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Join Now", color = Color.Black)
                        }
                    }
                    // Add cases for other statuses like ONGOING, COMPLETED if needed
                    else -> {
                        // Display something else, or nothing
                    }
                }
            }
        }
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

@Composable
fun InfoChip(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
} 