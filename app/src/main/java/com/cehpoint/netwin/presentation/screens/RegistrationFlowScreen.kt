package com.cehpoint.netwin.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cehpoint.netwin.domain.model.Tournament
import com.cehpoint.netwin.domain.model.RegistrationStepData
import com.cehpoint.netwin.domain.model.RegistrationStep
import com.cehpoint.netwin.presentation.events.RegistrationFlowEvent
import com.cehpoint.netwin.presentation.viewmodels.TournamentViewModel

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
                // Navigate back to tournament list or show success
                navController.navigateUp()
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

            // Error display
            registrationUiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                RegistrationStep.REVIEW -> RegistrationStep1(
                    tournament = selectedTournament,
                    onNext = { viewModel.onRegistrationEvent(RegistrationFlowEvent.Next) },
                    viewModel = viewModel
                )
                RegistrationStep.PAYMENT -> RegistrationStep2(
                    stepData = registrationUiState.data,
                    onDataUpdate = { transform ->
                        viewModel.onRegistrationEvent(RegistrationFlowEvent.UpdateData { transform(this) })
                    },
                    onNext = { viewModel.onRegistrationEvent(RegistrationFlowEvent.Next) },
                    isLoading = registrationUiState.loading
                )
                RegistrationStep.DETAILS -> RegistrationStep3(
                    stepData = registrationUiState.data,
                    onDataUpdate = { transform ->
                        viewModel.onRegistrationEvent(RegistrationFlowEvent.UpdateData { transform(this) })
                    },
                    onNext = { viewModel.onRegistrationEvent(RegistrationFlowEvent.Next) },
                    isLoading = registrationUiState.loading
                )
                RegistrationStep.CONFIRM -> RegistrationStep4(
                    tournament = selectedTournament,
                    stepData = registrationUiState.data,
                    onComplete = { viewModel.onRegistrationEvent(RegistrationFlowEvent.Submit) },
                    isLoading = isRegistering
                )
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
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..totalSteps) {
            // Step circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .then(
                        if (i <= stepIndex) {
                            Modifier
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (i < stepIndex) {
                    // Completed step
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = Color.Green,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    // Current or future step
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = if (i == stepIndex) Color.Cyan else Color.Gray,
                        border = BorderStroke(2.dp, if (i == stepIndex) Color.Cyan else Color.Gray)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = i.toString(),
                                color = if (i == stepIndex) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Connecting line (except for last step)
            if (i < totalSteps) {
                Divider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    color = if (i < stepIndex) Color.Green else Color.Gray,
                    thickness = 2.dp
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

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

// Step 1: Tournament Review & KYC/Wallet Check
@Composable
fun RegistrationStep1(
    tournament: Tournament?,
    onNext: () -> Unit,
    viewModel: TournamentViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Tournament Details",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        tournament?.let { t ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    InfoItem("Tournament", t.name)
                    InfoItem("Entry Fee", "₹${t.entryFee}")
                    InfoItem("Prize Pool", "₹${t.prizePool}")
                    InfoItem("Max Teams", "${t.maxTeams}")
                    InfoItem("Game Mode", t.matchType.ifEmpty { "Battle Royale" })
                    InfoItem("Map", t.map.ifEmpty { "Erangel" })
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // KYC and Wallet Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Requirements Check",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // KYC Status
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.Green,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("KYC Verified", color = Color.White)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Wallet Status
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.Green,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sufficient Balance", color = Color.White)
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Continue Button
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Continue to Payment",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black,
                fontWeight = FontWeight.Bold
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            color = color,
            style = MaterialTheme.typography.bodyMedium,
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
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Select Payment Method",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Payment Method Options
        PaymentMethodOption(
            methodName = "Wallet Balance",
            description = "Pay using your NetWin wallet",
            isSelected = selectedPaymentMethod == "wallet",
            onClick = { 
                selectedPaymentMethod = "wallet"
                onDataUpdate { it.copy(paymentMethod = "wallet") }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        PaymentMethodOption(
            methodName = "UPI Payment",
            description = "Pay using UPI (PhonePe, GPay, etc.)",
            isSelected = selectedPaymentMethod == "upi",
            onClick = { 
                selectedPaymentMethod = "upi"
                onDataUpdate { it.copy(paymentMethod = "upi") }
            },
            enabled = false // Temporarily disabled
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        PaymentMethodOption(
            methodName = "Credit/Debit Card",
            description = "Pay using your card",
            isSelected = selectedPaymentMethod == "card",
            onClick = { 
                selectedPaymentMethod = "card"
                onDataUpdate { it.copy(paymentMethod = "card") }
            },
            enabled = false // Temporarily disabled
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Continue Button
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = selectedPaymentMethod.isNotBlank() && !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan),
            shape = RoundedCornerShape(12.dp)
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
    stepData: RegistrationStepData,
    onDataUpdate: ((RegistrationStepData) -> RegistrationStepData) -> Unit,
    onNext: () -> Unit,
    isLoading: Boolean
) {
    var inGameId by remember(stepData.inGameId) { mutableStateOf(stepData.inGameId) }
    var teamName by remember(stepData.teamName) { mutableStateOf(stepData.teamName) }
    var termsAccepted by remember(stepData.termsAccepted) { mutableStateOf(stepData.termsAccepted) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Game Details",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // In-Game ID Field
        OutlinedTextField(
            value = inGameId,
            onValueChange = { 
                inGameId = it
                onDataUpdate { stepData -> stepData.copy(inGameId = it) }
            },
            label = { Text("In-Game ID", color = Color.Gray) },
            placeholder = { Text("Enter your PUBG/BGMI ID", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.Cyan,
                unfocusedBorderColor = Color.Gray
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Team Name Field
        OutlinedTextField(
            value = teamName,
            onValueChange = { 
                teamName = it
                onDataUpdate { stepData -> stepData.copy(teamName = it) }
            },
            label = { Text("Team Name", color = Color.Gray) },
            placeholder = { Text("Enter your team name", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.Cyan,
                unfocusedBorderColor = Color.Gray
            )
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Terms and Conditions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Terms & Conditions",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "• Entry fee is non-refundable\n• Follow fair play rules\n• No cheating or hacking allowed\n• Tournament organizer's decision is final",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        termsAccepted = !termsAccepted
                        onDataUpdate { it.copy(termsAccepted = !termsAccepted) }
                    }
                ) {
                    Checkbox(
                        checked = termsAccepted,
                        onCheckedChange = { 
                            termsAccepted = it
                            onDataUpdate { stepData -> stepData.copy(termsAccepted = it) }
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color.Cyan,
                            uncheckedColor = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "I accept the terms and conditions",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Continue Button
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = inGameId.isNotBlank() && teamName.isNotBlank() && termsAccepted && !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.Black
                )
            } else {
                Text(
                    text = "Review & Submit",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Step 4: Confirmation & Summary
@Composable
fun RegistrationStep4(
    tournament: Tournament?,
    stepData: RegistrationStepData,
    onComplete: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Registration Summary",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Tournament Summary
        tournament?.let { t ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Tournament Details",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Cyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoItem("Tournament", t.name)
                    InfoItem("Entry Fee", "₹${t.entryFee}")
                    InfoItem("Prize Pool", "₹${t.prizePool}")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Registration Details
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Your Details",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Cyan,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                InfoItem("In-Game ID", stepData.inGameId)
                InfoItem("Team Name", stepData.teamName)
                InfoItem("Payment Method", stepData.paymentMethod.replaceFirstChar { it.uppercase() })
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Final Submit Button
        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Processing...",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "Complete Registration",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
