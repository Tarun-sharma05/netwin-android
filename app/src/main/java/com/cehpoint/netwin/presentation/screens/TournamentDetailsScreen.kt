package com.cehpoint.netwin.presentation.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cehpoint.netwin.domain.model.Tournament
import com.cehpoint.netwin.domain.model.TournamentMode
import com.cehpoint.netwin.domain.model.TournamentStatus
import com.cehpoint.netwin.presentation.components.PrizeInfo
import com.cehpoint.netwin.presentation.components.buttons.GradientPrimaryButton
import com.cehpoint.netwin.presentation.navigation.ScreenRoutes
import com.cehpoint.netwin.presentation.theme.NetwinTokens
import com.cehpoint.netwin.presentation.viewmodels.TournamentViewModel
import com.cehpoint.netwin.utils.NGNTransactionUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Helper functions
internal fun formatDate(timestamp: Long?): String {
    if (timestamp == null) return "N/A"
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}


@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = NetwinTokens.TextSecondary, fontSize = 14.sp)
        Text(
            text = value,
            color = NetwinTokens.TextPrimary,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Composable
fun InfoChip(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(NetwinTokens.Surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = NetwinTokens.TextSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            color = valueColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun RuleItem(rule: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowRight,
            contentDescription = null,
            tint = NetwinTokens.Accent,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = rule, color = NetwinTokens.TextPrimary, fontSize = 14.sp)
    }
}


/**
 * NEW: A composable to display a single row in the prize distribution list.
 */
@Composable
fun RewardItem(position: Int, prizeAmount: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Rank",
                tint = NetwinTokens.Accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Rank #$position",
                color = NetwinTokens.TextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        }
        Text(
            text = prizeAmount,
//            color = NetwinTokens.Primary,
            color =   Color(0xFFFFC107),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}


@Composable
fun DetailsCountdownBadge(targetTimeMillis: Long, modifier: Modifier = Modifier) {
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

    Text(
        text = remainingTime,
        color = NetwinTokens.TextPrimary,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
    )
}

@Composable
fun ModeChip(mode: TournamentMode) {
    Surface(
        color = NetwinTokens.Surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = mode.name.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) },
            color = NetwinTokens.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun TournamentDetailsContent(
    tournament: Tournament,
    viewModel: TournamentViewModel,
    navController: NavController
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val showKycRequiredDialog by viewModel.showKycRequiredDialog.collectAsState()
    val userCurrency by viewModel.userCurrency.collectAsState()

    // Main content column - don't set fillMaxSize() or scrolling here
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
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
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                ModeChip(mode = tournament.mode)
                Spacer(modifier = Modifier.height(4.dp))
                if (tournament.map.isNotBlank()) {
                    Surface(
                        color = NetwinTokens.Surface,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = tournament.map,
                            color = NetwinTokens.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                NetwinTokens.Background.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = tournament.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = NetwinTokens.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            color = when (tournament.computedStatus) {
                                TournamentStatus.ONGOING -> NetwinTokens.Accent
                                TournamentStatus.ROOM_OPEN -> NetwinTokens.Primary.copy(alpha = 0.9f)
                                TournamentStatus.STARTS_SOON -> NetwinTokens.Accent.copy(alpha = 0.85f)
                                TournamentStatus.COMPLETED -> NetwinTokens.TextSecondary
                                else -> NetwinTokens.Primary
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
                        colors = CardDefaults.cardColors(containerColor = NetwinTokens.Surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Room Details",
                                color = NetwinTokens.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Room Code: ${tournament.roomId}",
                                    color = NetwinTokens.TextPrimary,
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
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy Room Code",
                                        tint = NetwinTokens.TextPrimary
                                    )
                                }
                            }
                            if (!tournament.roomPassword.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Password: ${tournament.roomPassword}",
                                        color = NetwinTokens.TextPrimary,
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
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Copy Password",
                                            tint = NetwinTokens.TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Scrollable content area
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            // Prize Pool and Entry Fee Section (restore icon chips in Card)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PrizeInfo(
                        label = "Prize Pool",
                        value = NGNTransactionUtils.formatAmountTidy(
                            tournament.prizePool.toDouble(),
                            userCurrency
                        ),
                        icon = Icons.Default.EmojiEvents,
                        color = Color(0xFFFFC107)
                    )
                    PrizeInfo(
                        label = "Entry Fee",
                        value = if (tournament.entryFee > 0)
                            NGNTransactionUtils.formatAmountTidy(
                                tournament.entryFee.toDouble(),
                                userCurrency
                            )
                        else "Free",
                        icon = Icons.Default.AccountBalanceWallet,
                        color = if (tournament.entryFee > 0) Color(0xFFFF7043) else Color(0xFF4CAF50)
                    )
                    PrizeInfo(
                        label = "Per Kill",
                        value = NGNTransactionUtils.formatAmountTidy(
                            tournament.killReward ?: 0.0,
                            userCurrency
                        ),
                        icon = Icons.Default.EmojiEvents,
                        color = Color(0xFF26C6DA)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Countdown Section
            if (tournament.computedStatus == TournamentStatus.UPCOMING || tournament.computedStatus == TournamentStatus.STARTS_SOON || tournament.computedStatus == TournamentStatus.ONGOING) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = NetwinTokens.SurfaceAlt)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (tournament.computedStatus) {
                                TournamentStatus.UPCOMING, TournamentStatus.STARTS_SOON -> "Starts In"
                                TournamentStatus.ONGOING -> "Time Remaining"
                                else -> ""
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = NetwinTokens.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailsCountdownBadge(
                            targetTimeMillis = if (tournament.computedStatus == TournamentStatus.ONGOING) (tournament.completedAt
                                ?: tournament.startTime) else tournament.startTime
                        )
                    }
                }
            }
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
}

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
    val registrationStatus by viewModel.registrationStatus.collectAsState()
    val user = FirebaseAuth.getInstance().currentUser
    val userCurrency by viewModel.userCurrency.collectAsState() // <-- Get the currency here
    LaunchedEffect(tournamentId) {
        viewModel.getTournamentById(tournamentId)
    }

    LaunchedEffect(selectedTournament?.id, user?.uid) {
        selectedTournament?.id?.let { tourneyId ->
            user?.uid?.let { userId ->
                viewModel.checkRegistrationStatus(tourneyId, userId)
            }
        }
    }

    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        selectedTournament?.name ?: "Tournament Details",
                        color = NetwinTokens.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NetwinTokens.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NetwinTokens.Background,
                    titleContentColor = NetwinTokens.TextPrimary,
                    navigationIconContentColor = NetwinTokens.TextPrimary
                )
            )
        },
        containerColor = NetwinTokens.Background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Everything scrolls as one, including the register button.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = NetwinTokens.Primary)
                        }
                    }

                    error != null -> {
                        Text(
                            text = error ?: "Unknown error",
                            color = NetwinTokens.Accent,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    selectedTournament != null -> {
                        // Tournament Details (image, info, countdown)
                        TournamentDetailsContent(
                            tournament = selectedTournament!!,
                            viewModel = viewModel,
                            navController = navController
                        )
                        // Add gap after banner before next card
                        Spacer(modifier = Modifier.height(16.dp))

                        // Tournament Info Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = NetwinTokens.SurfaceAlt)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Tournament Information",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NetwinTokens.TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                InfoRow("Teams", "${selectedTournament!!.registeredTeams}/${selectedTournament!!.maxTeams}")
                                InfoRow("Map", selectedTournament!!.map.ifBlank { "Unknown" })
                                InfoRow("Game Mode", selectedTournament!!.mode.name.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) })
                                if (selectedTournament!!.gameType.isNotBlank()) {
                                    InfoRow("Platform", selectedTournament!!.gameType)
                                }
                                InfoRow("Start Date", formatDate(selectedTournament!!.startTime))
                                selectedTournament!!.completedAt?.let { InfoRow("End Date", formatDate(it)) }
                            }
                        }

                        // Rules Card - appears only once
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = NetwinTokens.SurfaceAlt)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Rules & Regulations",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NetwinTokens.TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Log.d("Tournament Rules", "${selectedTournament!!.rules}")

//                                selectedTournament!!.rules.forEach { rule ->
//                                    RuleItem(rule = rule)
//                                }
                                // ADDED: Check if the rules list is empty
                                if (selectedTournament!!.rules.isNotEmpty()) {
                                    selectedTournament!!.rules.forEach { rule ->
                                        RuleItem(rule = rule)
                                    }
                                } else {
                                    // Display a message when no rules are available
                                    Text(
                                        text = "No specific rules have been listed for this tournament.",
                                        color = NetwinTokens.TextSecondary,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // NEW: Prize Distribution Card
                        if (selectedTournament!!.rewardsDistribution.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = NetwinTokens.SurfaceAlt)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Prize Distribution",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = NetwinTokens.TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    val totalPrizePool = selectedTournament!!.prizePool
                                    selectedTournament!!.rewardsDistribution.forEach { distribution ->
                                        val prizeForRank = (distribution.percentage / 100.0) * totalPrizePool
                                        RewardItem(
                                            position = distribution.position,
                                            prizeAmount = NGNTransactionUtils.formatAmountTidy(prizeForRank, userCurrency)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        Text(
                            text = "No tournament data available",
                            color = NetwinTokens.TextPrimary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Register Now Button - must come after the rules card
                if (selectedTournament != null) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.Black.copy(alpha = 0.1f))
                    )
                    GradientPrimaryButton(
                        text = when {
                            registrationStatus == true -> "Registered"
                            registrationStatus == false -> "Register Now"
                            else -> "Checking..."
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        enabled = registrationStatus == false,
                        onClick = {
                            navController.navigate(
                                ScreenRoutes.TournamentRegistration(
                                    tournamentId = tournamentId,
                                    stepIndex = 1
                                )
                            )
                        }
                    )
                    Spacer(
                        modifier = Modifier.height(32.dp)
                    )
                }
                // add a little extra space for nav bar safety for all content anyway
                Spacer(modifier = Modifier.height(16.dp))
            }
            // Add snackbar host outside of the scroll (recommended)
            SnackbarHost(hostState = snackbarHostState)
        }
    }
}
