package com.cehpoint.netwin.presentation.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.cehpoint.netwin.presentation.viewmodels.TournamentViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.cehpoint.netwin.domain.model.Tournament
import com.cehpoint.netwin.domain.model.TournamentStatus
import kotlinx.coroutines.delay
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.cehpoint.netwin.domain.model.TournamentMode
import com.cehpoint.netwin.presentation.viewmodels.TournamentEvent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import com.cehpoint.netwin.presentation.navigation.TournamentRegistration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentDetailsScreenUI(
    tournamentId: String,
    navController: NavController,
    viewModel: TournamentViewModel = hiltViewModel()
) {
    val selectedTournament by viewModel.selectedTournament.collectAsState()
    val isLoading by viewModel.isLoadingDetails.collectAsState()
    val error by viewModel.detailsError.collectAsState()

    Log.d("TournamentDetailsScreen", "Tournament ID: $tournamentId")
    Log.d("TournamentDetailsScreen", "Loading: $isLoading")
    Log.d("TournamentDetailsScreen", "Error: $error")
    Log.d("TournamentDetailsScreen", "Selected Tournament: ${selectedTournament?.name}")

    LaunchedEffect(tournamentId) {
        Log.d("TournamentDetailsScreen", "LaunchedEffect triggered - Loading tournament details")
        viewModel.getTournamentById(tournamentId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tournament Details") },
                navigationIcon = {
                    IconButton(onClick = { 
                        //viewModel.clearSelectedTournament()
                        navController.navigateUp() 
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White

                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.Cyan
                    )
                }
                error != null -> {
                    Text(
                        text = error ?: "Unknown error",
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                selectedTournament != null -> {
                    TournamentDetailsContent(
                        tournamentId = tournamentId,
                        tournament = selectedTournament!!,
                        modifier = Modifier.fillMaxSize(),
                        viewModel = viewModel,
                        navController = navController
                    )
                }
                else -> {
                    Text(
                        text = "No tournament data available",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun TournamentDetailsContent(
    tournamentId: String,
    tournament: Tournament,
    modifier: Modifier = Modifier,
    viewModel: TournamentViewModel = hiltViewModel(),
    navController: NavController
) {
    var remainingTime by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val registrationStatus by viewModel.registrationStatus.collectAsState()
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    var showRegistrationDialog by remember { mutableStateOf(false) }
    var inGameId by remember { mutableStateOf("") }
    var teamName by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }
    val showKycRequiredDialog by viewModel.showKycRequiredDialog.collectAsState()

    Log.d("TournamentDetailsContent", "Document ID ${tournamentId}, TournamentName:  ${tournament.name}, Start Time: ${tournament.startTime}, Completed At: ${tournament.completedAt}")
    LaunchedEffect(tournament.startTime, tournament.completedAt) {
        while (true) {
            val currentTime = System.currentTimeMillis()
            remainingTime = when (tournament.computedStatus) {
                TournamentStatus.UPCOMING -> {
                    val timeDiff = tournament.startTime - currentTime
                    val hours = timeDiff / (60 * 60 * 1000)
                    val minutes = (timeDiff % (60 * 60 * 1000)) / (60 * 1000)
                    val seconds = (timeDiff % (60 * 1000)) / 1000
                    when {
                        hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
                        minutes > 0 -> String.format("%02d:%02d", minutes, seconds)
                        else -> String.format("%02d seconds", seconds)
                    }
                }
                TournamentStatus.ONGOING -> {
                    val timeDiff = (tournament.completedAt ?: 0L) - currentTime
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
                TournamentStatus.STARTS_SOON -> "Starts Soon"
                TournamentStatus.ROOM_OPEN -> "Room Open"
            }
            delay(1000)
        }
    }

    LaunchedEffect(tournament.id, user?.uid) {
        if (user != null) {
            Log.d("TournamentDetailsScreen", "Checking registration status for user: ${user.uid} and tournament: ${tournament.id}")
            viewModel.checkRegistrationStatus(tournament.id, user.uid)
        } else {
            Log.d("TournamentDetailsScreen", "No user logged in, skipping registration status check.")
        }
    }

    // Registration error/success feedback
    val registrationState by viewModel.registrationState.collectAsState()
    LaunchedEffect(registrationState) {
        registrationState?.let {
            if (it.isSuccess) {
                Log.d("TournamentDetailsScreen", "Registration successful for user: ${user?.uid} in tournament: ${tournament.id}")
            } else if (it.isFailure) {
                Log.e("TournamentDetailsScreen", "Registration failed: ${it.exceptionOrNull()?.message}")
                Toast.makeText(context, "Registration failed: ${it.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Hero Image Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            AsyncImage(
                model = tournament.bannerImage,
                contentDescription = "Tournament Poster",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Mode Chip (top right)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                ModeChip(
                    mode = tournament.mode
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (tournament.map.isNotBlank()) {
                    Surface(
                        color = Color(0xFF424242),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = tournament.map,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
            
            // Tournament Name and Status
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = tournament.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Status Badge
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            color = when (tournament.computedStatus) {
                                TournamentStatus.ONGOING -> Color(0xFF4CAF50)
                                TournamentStatus.ROOM_OPEN -> Color(0xFF388E3C)
                                TournamentStatus.STARTS_SOON -> Color(0xFFFFA000)
                                TournamentStatus.COMPLETED -> Color(0xFF9E9E9E)
                                else -> Color(0xFF1976D2)
                            }
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Color.Transparent
                ) {
                    Text(
                        text = when (tournament.computedStatus) {
                            TournamentStatus.ONGOING -> "Ongoing"
                            TournamentStatus.ROOM_OPEN -> "Room Open"
                            TournamentStatus.STARTS_SOON -> "Starts Soon"
                            TournamentStatus.COMPLETED -> "Completed"
                            else -> "Upcoming"
                        },
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (tournament.computedStatus == TournamentStatus.ROOM_OPEN && tournament.roomId != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF388E3C)),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Room Details",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Room Code: ${tournament.roomId}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        clipboard.setText(AnnotatedString(tournament.roomId))
                                        snackbarHostState.showSnackbar("Room code copied!")
                                    }
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Room Code", tint = Color.White)
                                }
                            }
                            if (!tournament.roomPassword.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Password: ${tournament.roomPassword}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(onClick = {
                                        coroutineScope.launch {
                                            clipboard.setText(AnnotatedString(tournament.roomPassword))
                                            snackbarHostState.showSnackbar("Password copied!")
                                        }
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Password", tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Prize Pool and Entry Fee Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E1E)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PrizeInfo(
                        label = "Prize Pool",
                        value = "₹${tournament.prizePool}",
                        icon = Icons.Default.EmojiEvents,
                        color = Color.Green
                    )
                    PrizeInfo(
                        label = "Entry Fee",
                        value = "₹${tournament.entryFee}",
                        icon = Icons.Default.AccountBalanceWallet,
                        color = Color.Cyan
                    )
                    PrizeInfo(
                        label = "Per Kill",
                        value = "₹${tournament.killReward?.toInt() ?: 0}",
                        icon = Icons.Default.EmojiEvents,
                        color = Color.Yellow
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tournament Info Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E1E)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Tournament Information",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    InfoRow("Teams", "${tournament.registeredTeams}/${tournament.maxTeams}")
                    InfoRow("Map", tournament.map.ifBlank { "Unknown" })
                    InfoRow("Game Mode", when (tournament.mode) {
                        TournamentMode.SOLO -> "Solo"
                        TournamentMode.DUO -> "Duo"
                        TournamentMode.SQUAD -> "Squad"
                        TournamentMode.TRIO -> "Trio"
                        TournamentMode.CUSTOM -> "Custom"
                    })
                    InfoRow("Start Date", formatDate(tournament.startTime))
                    InfoRow("End Date", formatDate(tournament.completedAt))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Countdown Section (for upcoming tournaments)
            if (tournament.computedStatus != TournamentStatus.COMPLETED) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E1E1E)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (tournament.computedStatus) {
                                TournamentStatus.UPCOMING -> "Starts In"
                                TournamentStatus.STARTS_SOON -> "Starts Soon"
                                TournamentStatus.ROOM_OPEN -> "Room Open"
                                TournamentStatus.ONGOING -> "Time Remaining"
                                TournamentStatus.COMPLETED -> "Completed"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = remainingTime,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.Cyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rules Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E1E)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Tournament Rules",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    RuleItem("Team Size: 4 players")
                    RuleItem("Platform: Mobile")
                    RuleItem("Server: Asia")
                    RuleItem("Match Type: TPP")
                    RuleItem("Custom Room: Yes")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Join/Register Button
            Button(
                onClick = {
                    // Navigate to registration flow instead of showing dialog
                    navController.navigate(
                        com.cehpoint.netwin.presentation.navigation.TournamentRegistration(
                            tournamentId = tournamentId,
                            stepIndex = 1
                        )
                    )
                },
                enabled = registrationStatus == false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Cyan
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = when {
                        registrationStatus == true -> "Registered"
                        registrationStatus == false -> "Register Now"
                        else -> "Checking..."
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
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

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Add the SnackbarHost at the root of the screen
        Box(Modifier.fillMaxSize()) {
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun PrizeInfo(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = color,
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
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RuleItem(rule: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = null,
            tint = Color.Cyan,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = rule,
            color = Color.White,
            fontSize = 14.sp
        )
    }
}

private fun formatDate(timestamp: Long?): String {
    if (timestamp == null) return "TBD"
    val date = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
    return format.format(date)
} 