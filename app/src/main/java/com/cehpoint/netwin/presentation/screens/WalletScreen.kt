package com.cehpoint.netwin.presentation.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cehpoint.netwin.data.model.Transaction
import com.cehpoint.netwin.data.model.TransactionStatus
import com.cehpoint.netwin.data.model.TransactionType
import com.cehpoint.netwin.data.model.UserDetails
import com.cehpoint.netwin.data.model.WithdrawalRequest
import com.cehpoint.netwin.presentation.components.StatusChip
import com.cehpoint.netwin.presentation.components.statusBarPadding
import com.cehpoint.netwin.presentation.viewmodels.WalletViewModel
import com.cehpoint.netwin.presentation.viewmodels.AuthViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    navController: NavController,
    walletViewModel: WalletViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val walletBalance by walletViewModel.walletBalance.collectAsState()
    val transactions by walletViewModel.transactions.collectAsState()
    val pendingDeposits by walletViewModel.pendingDeposits.collectAsState()
    val isLoading by walletViewModel.isLoading.collectAsState()
    val error by walletViewModel.error.collectAsState()
    
    val currentUser by authViewModel.currentUser.collectAsState()
    val withdrawableBalance by walletViewModel.withdrawableBalance.collectAsState(initial = 0.0)
    val bonusBalance by walletViewModel.bonusBalance.collectAsState(initial = 0.0)
    val withdrawalRequests by walletViewModel.withdrawalRequests.collectAsState(initial = emptyList())

    var showAmountSheet by remember { mutableStateOf(false) }
    var selectedAmount by remember { mutableStateOf(0) }
    var manualAmount by remember { mutableStateOf("") }
    var upiResultMessage by remember { mutableStateOf<String?>(null) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    val presetAmounts = listOf(100, 200, 500)
    val amountSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var withdrawSuccess by remember { mutableStateOf(false) }

    // UPI Intent launcher
    val upiLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val response = data?.getStringExtra("response")
            if (response != null && response.contains("SUCCESS", ignoreCase = true)) {
                upiResultMessage = "Payment successful!"
                // TODO: Credit wallet, update Firestore, etc.
            } else {
                upiResultMessage = "Payment failed or cancelled."
            }
        } else {
            upiResultMessage = "Payment cancelled."
        }
    }

    // Load wallet data when screen is first displayed or when user changes
    LaunchedEffect(currentUser) {
        if (currentUser != null && !isLoading) {
            walletViewModel.loadWalletData(currentUser!!.uid)
            walletViewModel.loadWithdrawalRequests(currentUser!!.uid)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)) // Dark background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .statusBarPadding()
        ) {
            // Top Bar
            WalletTopBar(walletBalance = walletBalance)

            if (currentUser == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Please log in to view wallet",
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(
                            onClick = { /* Navigate to login */ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00BCD4)
                            )
                        ) {
                            Text("Login", color = Color.Black)
                        }
                    }
                }
            } else if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF00BCD4))
                }
            } else if (error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error ?: "Unknown error",
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(
                            onClick = { 
                                walletViewModel.clearError()
                                if (currentUser != null) {
                                    walletViewModel.loadWalletData(currentUser!!.uid)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00BCD4)
                            )
                        ) {
                            Text("Retry", color = Color.Black)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp), // Space for bottom nav
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Balance Card
                    item {
                        BalanceCard(
                            withdrawableBalance = withdrawableBalance,
                            bonusBalance = bonusBalance,
                            onWithdrawClick = { showWithdrawDialog = true }
                        )
                    }

                    // Quick Actions
                    item {
                        QuickActions(
                            onDepositClick = { showAmountSheet = true }
                        )
                    }

                    // Pending Deposits Section
                    if (pendingDeposits.isNotEmpty()) {
                        item {
                            Text(
                                text = "Pending Deposits",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        items(pendingDeposits) { deposit ->
                            PendingDepositItem(deposit)
                        }
                    }

                    // Withdrawal Requests Section
                    if (withdrawalRequests.isNotEmpty()) {
                        item {
                            Text(
                                text = "Withdrawal Requests",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        items(withdrawalRequests) { request ->
                            WithdrawalRequestItem(request = request)
                        }
                    }

                    // Transaction History Header
                    item {
                        Text(
                            text = "Transaction History",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    // Transaction List
                    if (transactions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No transactions yet",
                                    color = Color.Gray,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    } else {
                        items(transactions) { transaction ->
                            TransactionItem(transaction)
                        }
                    }
                }
            }
        }
    }

    if (showAmountSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAmountSheet = false },
            sheetState = amountSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Select Amount", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    presetAmounts.forEach { amt ->
                        Button(
                            onClick = {
                                selectedAmount = amt
                                manualAmount = ""
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedAmount == amt) Color(0xFF00BCD4) else Color(0xFF2A2A2A)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("₹$amt", color = if (selectedAmount == amt) Color.Black else Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Or enter custom amount", color = Color.Gray, fontSize = 14.sp)
                OutlinedTextField(
                    value = manualAmount,
                    onValueChange = {
                        manualAmount = it.filter { c -> c.isDigit() }
                        selectedAmount = 0
                    },
                    label = { Text("Amount (₹)", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00BCD4),
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = Color(0xFF00BCD4),
                        focusedLabelColor = Color(0xFF00BCD4),
                        unfocusedLabelColor = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val amount = if (selectedAmount > 0) selectedAmount else manualAmount.toIntOrNull() ?: 0
                        if (amount > 0) {
                            // Build UPI URI with correct values
                            val upiId = "netwin@upi" // Set your actual UPI ID here
                            val name = "NetWin" // Set your app name here
                            val upiUri = Uri.parse(
                                "upi://pay?pa=$upiId&pn=$name&am=$amount&cu=INR&tn=Wallet+Deposit"
                            )
                            val intent = Intent(Intent.ACTION_VIEW, upiUri)
                            coroutineScope.launch {
                                showAmountSheet = false
                                upiLauncher.launch(intent)
                            }
                        }
                    },
                    enabled = (selectedAmount > 0) || (manualAmount.toIntOrNull() ?: 0 > 0),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Proceed", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { showAmountSheet = false }) {
                    Text("Cancel", color = Color(0xFF00BCD4))
                }
            }
        }
    }

    if (showWithdrawDialog) {
        WithdrawDialog(
            maxAmount = withdrawableBalance,
            isLoading = isLoading,
            onDismiss = { showWithdrawDialog = false },
            onWithdraw = { amount, upiId ->
                if (currentUser != null) {
                    val userDetails = UserDetails(
                        email = currentUser!!.email ?: "",
                        name = currentUser!!.displayName ?: "",
                        username = currentUser!!.email ?: "",
                        userId = currentUser!!.uid
                    )
                    val request = WithdrawalRequest(
                        userId = currentUser!!.uid,
                        amount = amount,
                        upiId = upiId,
                        userDetails = userDetails
                    )
                    walletViewModel.createWithdrawalRequest(request)
                    withdrawSuccess = true
                }
                showWithdrawDialog = false
            }
        )
    }

    // Show UPI result message
    upiResultMessage?.let { msg ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { upiResultMessage = null },
            title = { Text("UPI Payment") },
            text = { Text(msg) },
            confirmButton = {
                Button(onClick = { upiResultMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    // Show withdrawal success/error Snackbar
    LaunchedEffect(withdrawSuccess, error) {
        if (withdrawSuccess && error == null) {
            snackbarHostState.showSnackbar("Withdrawal request submitted successfully!")
            withdrawSuccess = false
        } else if (error != null) {
            error?.let { snackbarHostState.showSnackbar(it) }
            walletViewModel.clearError()
        }
    }

    Box(Modifier.fillMaxSize()) {
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun WalletTopBar(walletBalance: Double) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF121212))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Wallet",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = Color(0xFF00BCD4),
                modifier = Modifier.size(24.dp)
            )
            Text(
                "₹$walletBalance",
                modifier = Modifier.padding(start = 4.dp),
                color = Color(0xFF00BCD4),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BalanceCard(
    withdrawableBalance: Double,
    bonusBalance: Double,
    onWithdrawClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Withdrawable Balance",
                color = Color.Gray,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "₹$withdrawableBalance",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Bonus Balance",
                color = Color.Gray,
                fontSize = 14.sp
            )
            Text(
                text = "₹$bonusBalance",
                color = Color(0xFF00BCD4),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onWithdrawClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Withdraw to Bank", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun QuickActions(
    onDepositClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickActionButton(
            icon = Icons.Default.Add,
            label = "Add Money",
            onClick = onDepositClick
        )
        QuickActionButton(
            icon = Icons.Default.History,
            label = "History",
            onClick = { /* TODO: Navigate to detailed history */ }
        )
        QuickActionButton(
            icon = Icons.Default.Settings,
            label = "Settings",
            onClick = { /* TODO: Navigate to wallet settings */ }
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2A2A))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF00BCD4),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PendingDepositItem(deposit: com.cehpoint.netwin.data.model.PendingDeposit) {
    Log.d("PendingDepositItem", "Rendering deposit: ${deposit.upiRefId}, amount: ${deposit.amount}, status: ${deposit.status}")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFC107)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Pending Deposit",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "UPI Ref: ${deposit.upiRefId}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Your UPI ID: ${deposit.userUpiId}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    if (!deposit.screenshotUrl.isNullOrBlank()) {
                        Text(
                            text = "Screenshot uploaded",
                            color = Color(0xFF00BCD4),
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = formatDate(deposit.createdAt),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "₹${deposit.amount}",
                    color = Color(0xFFFFC107),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = deposit.status.name.lowercase().capitalize(Locale.ROOT),
                    color = Color(0xFFFFC107),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun TransactionItem(transaction: Transaction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            when (transaction.type) {
                                TransactionType.DEPOSIT, TransactionType.UPI_DEPOSIT -> Color(0xFF4CAF50)
                                TransactionType.WITHDRAWAL, TransactionType.UPI_WITHDRAWAL -> Color(0xFFE57373)
                                TransactionType.TOURNAMENT_ENTRY -> Color(0xFF2196F3)
                                TransactionType.TOURNAMENT_WINNING -> Color(0xFFFFC107)
                                else -> Color.Gray
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (transaction.type) {
                            TransactionType.DEPOSIT, TransactionType.UPI_DEPOSIT -> Icons.Default.Add
                            TransactionType.WITHDRAWAL, TransactionType.UPI_WITHDRAWAL -> Icons.Default.Remove
                            TransactionType.TOURNAMENT_ENTRY -> Icons.Default.PlayArrow
                            TransactionType.TOURNAMENT_WINNING -> Icons.Default.EmojiEvents
                            else -> Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = transaction.description,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatDate(transaction.createdAt),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${if (transaction.type in listOf(TransactionType.DEPOSIT, TransactionType.UPI_DEPOSIT, TransactionType.TOURNAMENT_WINNING)) "+" else "-"}₹${transaction.amount}",
                    color = when (transaction.type) {
                        TransactionType.DEPOSIT, TransactionType.UPI_DEPOSIT, TransactionType.TOURNAMENT_WINNING -> Color(0xFF4CAF50)
                        TransactionType.WITHDRAWAL, TransactionType.UPI_WITHDRAWAL, TransactionType.TOURNAMENT_ENTRY -> Color(0xFFE57373)
                        else -> Color.Gray
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = transaction.status.name.lowercase().capitalize(Locale.ROOT),
                    color = when (transaction.status) {
                        TransactionStatus.COMPLETED -> Color(0xFF4CAF50)
                        TransactionStatus.PENDING -> Color(0xFFFFC107)
                        TransactionStatus.FAILED -> Color(0xFFE57373)
                        else -> Color.Gray
                    },
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun WithdrawDialog(
    maxAmount: Double,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onWithdraw: (Double, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Withdraw Funds") },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it.filter { c -> c.isDigit() || c == '.' }
                        error = null
                    },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = upiId,
                    onValueChange = {
                        upiId = it
                        error = null
                    },
                    label = { Text("Your UPI ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Max withdrawable: ₹$maxAmount", color = Color.Gray, fontSize = 12.sp)
                if (error != null) {
                    Text(error ?: "", color = Color.Red, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt <= 0.0) {
                        error = "Enter a valid amount"
                        return@Button
                    }
                    if (amt > maxAmount) {
                        error = "Amount exceeds withdrawable balance"
                        return@Button
                    }
                    if (upiId.isBlank() || !upiId.contains("@")) {
                        error = "Enter a valid UPI ID"
                        return@Button
                    }
                    onWithdraw(amt, upiId)
                    amount = ""
                    upiId = ""
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("Withdraw")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun WithdrawalRequestItem(request: WithdrawalRequest) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF232323)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("₹${request.amount}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(request.upiId, color = Color.Gray, fontSize = 14.sp)
                StatusChip(request.status)
                if (!request.rejectionReason.isNullOrBlank()) {
                    Text("Reason: ${request.rejectionReason}", color = Color.Red, fontSize = 12.sp)
                }
            }
            Text(
                text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(request.createdAt)),
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

private fun formatDate(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return "Unknown"
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}