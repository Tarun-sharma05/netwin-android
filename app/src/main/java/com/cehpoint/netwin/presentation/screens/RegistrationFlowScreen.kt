package com.cehpoint.netwin.presentation.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cehpoint.netwin.domain.model.RegistrationStep
import com.cehpoint.netwin.domain.model.RegistrationStepData
import com.cehpoint.netwin.domain.model.Tournament
import com.cehpoint.netwin.presentation.events.RegistrationFlowEvent
import com.cehpoint.netwin.presentation.viewmodels.TournamentViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationFlowScreen(
    tournamentId: String,
    stepIndex: Int,
    navController: NavController,
    viewModel: TournamentViewModel = hiltViewModel()
) {
    // Collect the new combined UI state
    val registrationUiState by viewModel.registrationUiState.collectAsState()
    val selectedTournament by viewModel.selectedTournament.collectAsState()
    val isRegistering by viewModel.isRegistering.collectAsState()
    val registrationState by viewModel.registrationState.collectAsState()

    // Load tournament details if not already loaded
    LaunchedEffect(tournamentId) {
        if (selectedTournament?.id != tournamentId) {
            viewModel.getTournamentById(tournamentId)
        }
        // Update step data with tournament ID
        viewModel.onRegistrationEvent(
            RegistrationFlowEvent.UpdateData { copy(tournamentId = tournamentId) }
        )
    }

    // Handle registration completion
    LaunchedEffect(registrationState) {
        registrationState?.let { result ->
            if (result.isSuccess) {
                // Navigate to Victory Pass screen to show room credentials
                navController.navigate("victory_pass/$tournamentId") {
                    popUpTo("tournament_details/$tournamentId") { inclusive = false }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tournament Registration") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (registrationUiState.step != RegistrationStep.REVIEW) {
                            viewModel.onRegistrationEvent(RegistrationFlowEvent.Previous)
                        } else {
                            navController.navigateUp()
                        }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Progress indicator
            RegistrationProgressIndicator(
                currentStep = registrationUiState.step,
                totalSteps = 4
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error display - only show errors relevant to current step
            registrationUiState.error?.let { error ->
                android.util.Log.e("RegistrationFlowScreen", "VALIDATION ERROR DISPLAYED IN UI: '$error' on step ${registrationUiState.step}")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("error_card"),
                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, Color.Red)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = Color.Red
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Step content
            when (registrationUiState.step) {
                RegistrationStep.REVIEW -> {
                    android.util.Log.d("RegistrationFlowScreen", "UI consuming ValidationState - Current Step: REVIEW, Error: ${registrationUiState.error}")
                    RegistrationStep1(
                        tournament = selectedTournament,
                        onNext = { 
                            android.util.Log.d("RegistrationFlowScreen", "BREAKPOINT 1: User clicked Next from REVIEW step - triggering validation")
                            viewModel.onRegistrationEvent(RegistrationFlowEvent.Next) 
                        },
                        viewModel = viewModel
                    )
                }
                RegistrationStep.PAYMENT -> {
                    android.util.Log.d("RegistrationFlowScreen", "UI consuming ValidationState - Current Step: PAYMENT, Error: ${registrationUiState.error}")
                    RegistrationStep2(
                        stepData = registrationUiState.data,
                        onDataUpdate = { transform ->
                            viewModel.onRegistrationEvent(RegistrationFlowEvent.UpdateData { transform(this) })
                        },
                        onNext = { 
                            android.util.Log.d("RegistrationFlowScreen", "BREAKPOINT 2: User clicked Next from PAYMENT step - triggering validation")
                            viewModel.onRegistrationEvent(RegistrationFlowEvent.Next) 
                        },
                        isLoading = registrationUiState.loading
                    )
                }
                RegistrationStep.DETAILS -> {
                    android.util.Log.d("RegistrationFlowScreen", "UI consuming ValidationState - Current Step: DETAILS, Error: ${registrationUiState.error}")
                    RegistrationStep3(
                        tournament = selectedTournament,
                        stepData = registrationUiState.data,
                        onDataUpdate = { transform ->
                            viewModel.onRegistrationEvent(RegistrationFlowEvent.UpdateData { transform(this) })
                        },
                        onNext = { 
                            android.util.Log.d("RegistrationFlowScreen", "BREAKPOINT 3: User clicked Next from DETAILS step - triggering validation")
                            viewModel.onRegistrationEvent(RegistrationFlowEvent.Next) 
                        },
                        isLoading = registrationUiState.loading
                    )
                }
                RegistrationStep.CONFIRM -> {
                    android.util.Log.d("RegistrationFlowScreen", "UI consuming ValidationState - Current Step: CONFIRM, Error: ${registrationUiState.error}")
                    RegistrationStep4(
                        tournament = selectedTournament,
                        stepData = registrationUiState.data,
                        onComplete = { 
                            android.util.Log.d("RegistrationFlowScreen", "BREAKPOINT 4: User clicked Submit from CONFIRM step - triggering final validation")
                            viewModel.onRegistrationEvent(RegistrationFlowEvent.Submit) 
                        },
                        isLoading = isRegistering
                    )
                }
            }
        }
    }
}

@Composable
fun RegistrationProgressIndicator(
    currentStep: RegistrationStep,
    totalSteps: Int
) {
    val stepIndex = when (currentStep) {
        RegistrationStep.REVIEW -> 1
        RegistrationStep.PAYMENT -> 2
        RegistrationStep.DETAILS -> 3
        RegistrationStep.CONFIRM -> 4
    }
    val rawProgress = (stepIndex.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = rawProgress, animationSpec = tween(450), label = "reg_progress")

    Column(modifier = Modifier.fillMaxWidth()) {
        // Animated gradient progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF263238))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = animatedProgress)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF00E5FF),
                                Color(0xFF00FFB3),
                                Color(0xFF66FF99)
                            )
                        )
                    )
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Step title
        val stepTitle = when (currentStep) {
            RegistrationStep.REVIEW -> "Review Tournament"
            RegistrationStep.PAYMENT -> "Payment Method"
            RegistrationStep.DETAILS -> "Game Details"
            RegistrationStep.CONFIRM -> "Confirmation"
        }
        Text(
            text = stepTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

// Step 1: Tournament Review & Prerequisites Check
@Composable
fun RegistrationStep1(
    tournament: Tournament?,
    onNext: () -> Unit,
    viewModel: TournamentViewModel
) {
    val scrollState = rememberScrollState()
    val walletBalance by viewModel.walletBalance.collectAsState()
    
    // Collect user data for KYC status
    var userKycStatus by remember { mutableStateOf("pending") }
    var isLoadingUserData by remember { mutableStateOf(true) }
    
    // Load user data to get KYC status
    LaunchedEffect(Unit) {
        try {
            // Use a method from ViewModel to get user data
            // For now, assume KYC is verified - this should be exposed through ViewModel
            userKycStatus = "VERIFIED"
            isLoadingUserData = false
        } catch (e: Exception) {
            isLoadingUserData = false
        }
    }
    
    // Calculate prerequisites
    val entryFee = tournament?.entryFee?.toDouble() ?: 0.0
    val hasSufficientBalance = walletBalance >= entryFee
    val isKycVerified = userKycStatus.equals("VERIFIED", ignoreCase = true)
    val registrationOpen = tournament?.let { 
        val now = System.currentTimeMillis()
        // Time-based window: use explicit registration window if present, else fallback to startTime
        val regStart = it.registrationStartTime ?: Long.MIN_VALUE
        val regEndOrStart = it.registrationEndTime ?: it.startTime
        now >= regStart && now < regEndOrStart
    } ?: false
    
    val slotsAvailable = tournament?.let { 
        it.registeredTeams < it.maxTeams 
    } ?: false
    
    val allRequirementsMet = hasSufficientBalance && isKycVerified && registrationOpen && slotsAvailable

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "Tournament Registration",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Review tournament details and check requirements",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tournament Information Card
            tournament?.let { t ->
                // Log: Tournament Details (matches the UI below)
                Log.d("TAG_TOURNAMENT_DETAILS", "=== TOURNAMENT DETAILS ===")
                Log.d("TAG_TOURNAMENT_DETAILS", "Name: ${t.name}")
                Log.d("TAG_TOURNAMENT_DETAILS", "ID: ${t.id}")
                Log.d("TAG_TOURNAMENT_DETAILS", "Status: ${t.computedStatus.name}")
                Log.d("TAG_TOURNAMENT_DETAILS", "Entry Fee: ${t.entryFee} | Prize Pool: ${t.prizePool}")
                Log.d("TAG_TOURNAMENT_DETAILS", "Mode: ${t.matchType.ifEmpty { "SQUAD" }} | Map: ${t.map.ifEmpty { "Erangel" }}")
                Log.d("TAG_TOURNAMENT_DETAILS", "Participants: ${t.registeredTeams}/${t.maxTeams}")
                Log.d("TAG_TOURNAMENT_DETAILS", "StartTime(ms): ${t.startTime} | Date: ${try { java.util.Date(t.startTime).toString() } catch (e: Exception) { "Invalid" }}")
                Log.d("TAG_TOURNAMENT_DETAILS", "==================================")
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tournament Details",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Cyan,
                                fontWeight = FontWeight.Bold
                            )

                            // Status Badge
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = when (t.computedStatus.name) {
                                        "UPCOMING" -> Color.Green.copy(alpha = 0.2f)
                                        "ONGOING" -> Color.Yellow.copy(alpha = 0.2f)
                                        else -> Color.Gray.copy(alpha = 0.2f)
                                    }
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = t.computedStatus.name,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (t.computedStatus.name) {
                                        "UPCOMING" -> Color.Green
                                        "ONGOING" -> Color.Yellow
                                        else -> Color.Gray
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Tournament Info Grid
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            InfoItem("Tournament", t.name)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    InfoItem("Entry Fee", "₹${t.entryFee.toInt()}", Color.Cyan)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    InfoItem("Prize Pool", "₹${t.prizePool.toInt()}", Color.Green)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    InfoItem("Mode", t.matchType.ifEmpty { "SQUAD" })
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    InfoItem("Map", t.map.ifEmpty { "Erangel" })
                                }
                            }

                            InfoItem(
                                "Participants",
                                "${t.registeredTeams}/${t.maxTeams} teams",
                                if (slotsAvailable) Color.Green else Color.Red
                            )

                            // Start Time
                            val startTimeText = remember(t.startTime) {
                                try {
                                    val date = java.util.Date(t.startTime)
                                    val formatter = java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", java.util.Locale.getDefault())
                                    formatter.format(date)
                                } catch (e: Exception) {
                                    "TBD"
                                }
                            }
                            InfoItem("Start Time", startTimeText)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Prerequisites Check Card
            Log.d("TAG_REG_REQUIREMENTS", "=== REGISTRATION REQUIREMENTS ===")
            Log.d("TAG_REG_REQUIREMENTS", "Wallet Balance: $walletBalance")
            Log.d("TAG_REG_REQUIREMENTS", "Entry Fee: $entryFee")
            Log.d("TAG_REG_REQUIREMENTS", "KYC Status: ${if (isKycVerified) "COMPLETED" else "PENDING"}")
            Log.d("TAG_REG_REQUIREMENTS", "Registration Window: ${if (registrationOpen) "OPEN" else "CLOSED"}")
            Log.d("TAG_REG_REQUIREMENTS", "Slots Available: ${if (slotsAvailable) "YES" else "NO"}")
            Log.d("TAG_REG_REQUIREMENTS", "==================================")
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("RegistrationRequirementsCard"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Registration Requirements",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Cyan,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // KYC Status
                    PrerequisiteItem(
                        title = "KYC Verification",
                        description = when {
                            isLoadingUserData -> "Checking status..."
                            isKycVerified -> "Your identity is verified"
                            userKycStatus == "pending" -> "KYC verification pending"
                            else -> "KYC verification required"
                        },
                        isValid = isKycVerified,
                        isLoading = isLoadingUserData
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Wallet Balance
                    PrerequisiteItem(
                        title = "Wallet Balance",
                        description = if (hasSufficientBalance) {
                            "₹${walletBalance.toInt()} available (₹${entryFee.toInt()} required)"
                        } else {
                            "Insufficient balance: ₹${walletBalance.toInt()} (₹${entryFee.toInt()} required)"
                        },
                        isValid = hasSufficientBalance,
                        isLoading = false
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Registration Window
                    PrerequisiteItem(
                        title = "Registration Window",
                        description = tournament?.let {
                            val now = System.currentTimeMillis()
                            val cutoff = (it.registrationEndTime ?: it.startTime)
                            val opensAt = it.registrationStartTime
                            val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())

                            when {
                                opensAt != null && now < opensAt -> "Opens ${dateFormat.format(Date(opensAt))}"
                                now < cutoff -> "Open until ${dateFormat.format(Date(cutoff))}"
                                else -> "Registration closed"
                            }
                        } ?: "Loading...",
                        isValid = registrationOpen,
                        isLoading = tournament == null
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Available Slots
                    PrerequisiteItem(
                        title = "Available Slots",
                        description = if (slotsAvailable) {
                            "${tournament?.maxTeams?.minus(tournament.registeredTeams) ?: 0} slots remaining"
                        } else {
                            "Tournament is full"
                        },
                        isValid = slotsAvailable,
                        isLoading = false
                    )
                }
            }
        }

        // Fixed button area
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = allRequirementsMet,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00E5FF),
                contentColor = Color.Black,
                disabledContainerColor = Color(0xFF3A3A3A),
                disabledContentColor = Color(0xFFBDBDBD)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 6.dp)
        ) {
            if (allRequirementsMet) {
                Text(
                    text = "Continue to Payment",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "Requirements Not Met",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (!allRequirementsMet) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Please complete all requirements above to continue",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PrerequisiteItem(
    title: String,
    description: String,
    isValid: Boolean,
    isLoading: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.Cyan,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (isValid) Color.Green else Color.Red,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isValid) Color.Green else if (isLoading) Color.Gray else Color.Red
            )
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String,
    color: Color = Color.White
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

// Step 2: Payment Method Selection
@Composable
fun RegistrationStep2(
    stepData: RegistrationStepData,
    onDataUpdate: ((RegistrationStepData) -> RegistrationStepData) -> Unit,
    onNext: () -> Unit,
    isLoading: Boolean
) {
    var selectedPaymentMethod by remember(stepData.paymentMethod) { 
        mutableStateOf(stepData.paymentMethod) 
    }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "Select Payment Method",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Payment Method Options - Only Wallet Available
            PaymentMethodOption(
                methodName = "Wallet Balance",
                description = "Pay using your NetWin wallet",
                isSelected = selectedPaymentMethod == "wallet",
                onClick = { 
                    selectedPaymentMethod = "wallet"
                    onDataUpdate { it.copy(paymentMethod = "wallet") }
                }
            )
        }
        
        // Fixed button area
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = selectedPaymentMethod.isNotBlank() && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00E5FF),
                contentColor = Color.Black,
                disabledContainerColor = Color(0xFF3A3A3A),
                disabledContentColor = Color(0xFFBDBDBD)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 6.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.Black
                )
            } else {
                Text(
                    text = "Continue to Details",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PaymentMethodOption(
    methodName: String,
    description: String = "",
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color.Cyan.copy(alpha = 0.2f) else Color(0xFF1E1E1E)
        ),
        border = BorderStroke(2.dp, if (isSelected) Color.Cyan else Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = null, // Handled by Card click
                enabled = enabled,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color.Cyan,
                    unselectedColor = Color.Gray
                )
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = methodName,
                    color = if (enabled) Color.White else Color.Gray,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        color = if (enabled) Color.Gray else Color.Gray.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// Step 3: Game Details & Terms
@Composable
fun RegistrationStep3(
    tournament: Tournament?,
    stepData: RegistrationStepData,
    onDataUpdate: ((RegistrationStepData) -> RegistrationStepData) -> Unit,
    onNext: () -> Unit,
    isLoading: Boolean
) {
    val scrollState = rememberScrollState()
    val termsAccepted by remember(stepData.termsAccepted) { mutableStateOf(stepData.termsAccepted) }
    val maxPlayers = tournament?.teamSize ?: 1
    val canContinue = stepData.teamName.isNotBlank() && stepData.playerIds.all { it.isNotBlank() } && termsAccepted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "Game Details & Rules",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Team Name
            OutlinedTextField(
                value = stepData.teamName,
                onValueChange = { onDataUpdate { data -> data.copy(teamName = it) } },
                label = { Text("Team Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Player IDs
            stepData.playerIds.forEachIndexed { index, playerId ->
                OutlinedTextField(
                    value = playerId,
                    onValueChange = {
                        val newPlayerIds = stepData.playerIds.toMutableList()
                        newPlayerIds[index] = it
                        onDataUpdate { data -> data.copy(playerIds = newPlayerIds) }
                    },
                    label = { Text("Player ${index + 1} In-Game ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Add/Remove Player Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        if (stepData.playerIds.size < maxPlayers) {
                            onDataUpdate { it.copy(playerIds = it.playerIds + "") }
                        }
                    },
                    enabled = stepData.playerIds.size < maxPlayers
                ) {
                    Text("Add Player")
                }
                Button(
                    onClick = {
                        if (stepData.playerIds.size > 1) {
                            onDataUpdate { it.copy(playerIds = it.playerIds.dropLast(1)) }
                        }
                    },
                    enabled = stepData.playerIds.size > 1
                ) {
                    Text("Remove Player")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tournament Rules
            if (tournament?.rules?.isNotEmpty() == true) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Tournament Rules",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Cyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = tournament.rules.joinToString(separator = "\n") { "• $it" },
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Terms and Conditions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDataUpdate { data -> data.copy(termsAccepted = !data.termsAccepted) } }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = termsAccepted,
                    onCheckedChange = { onDataUpdate { data -> data.copy(termsAccepted = it) } }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("I agree to the tournament rules and terms of service.", color = Color.White)
            }
        }

        // Fixed button area
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = canContinue && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00E5FF),
                contentColor = Color.Black,
                disabledContainerColor = Color(0xFF3A3A3A),
                disabledContentColor = Color(0xFFBDBDBD)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
            } else {
                Text("Continue to Confirmation", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// Step 4: Confirmation
@Composable
fun RegistrationStep4(
    tournament: Tournament?,
    stepData: RegistrationStepData,
    onComplete: () -> Unit,
    isLoading: Boolean
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "Confirm Registration",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SummaryRow("Tournament", tournament?.name ?: "N/A")
                    SummaryRow("Team Name", stepData.teamName)
                    SummaryRow("Entry Fee", "₹${tournament?.entryFee?.toInt() ?: 0}")
                    SummaryRow("Payment Method", stepData.paymentMethod.replaceFirstChar { it.uppercase() })
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("Players", style = MaterialTheme.typography.titleSmall, color = Color.Cyan)
                    stepData.playerIds.forEachIndexed { index, id ->
                        SummaryRow("Player ${index + 1}", id)
                    }
                }
            }
        }

        // Fixed button area
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00E5FF),
                contentColor = Color.Black,
                disabledContainerColor = Color(0xFF3A3A3A),
                disabledContentColor = Color(0xFFBDBDBD)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
            } else {
                Text("Submit Registration", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
