package com.cehpoint.netwin.presentation.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.cehpoint.netwin.presentation.navigation.ScreenRoutes
import com.cehpoint.netwin.presentation.viewmodels.WalletViewModel
import com.cehpoint.netwin.presentation.viewmodels.AuthViewModel
import com.cehpoint.netwin.utils.NGNTransactionUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter

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
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    val withdrawableBalance by walletViewModel.withdrawableBalance.collectAsState(initial = 0.0)
    val bonusBalance by walletViewModel.bonusBalance.collectAsState(initial = 0.0)
    val withdrawalRequests by walletViewModel.withdrawalRequests.collectAsState(initial = emptyList())

    var showAmountSheet by remember { mutableStateOf(false) }
    var selectedAmount by remember { mutableStateOf(0) }
    var manualAmount by remember { mutableStateOf("") }
    var upiResultMessage by remember { mutableStateOf<String?>(null) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showNGNDepositDialog by remember { mutableStateOf(false) }
    val presetAmounts = listOf(100, 200, 500)
    val presetNGNAmounts = listOf(1000, 2000, 5000)
    val amountSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var withdrawSuccess by remember { mutableStateOf(false) }
    
    // Get user country and currency
    var userCountry by remember { mutableStateOf("IN") }
    var userCurrency by remember { mutableStateOf("INR") }
    
    var kycStatus by remember { mutableStateOf<String?>(null) }
    var showKycDialog by remember { mutableStateOf(false) }
    
    val uriHandler = LocalUriHandler.current
    
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            try {
                val userDoc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.uid)
                    .get()
                    .await()
                val country = userDoc.getString("country") ?: "India"
                userCountry = country
                // Handle both country names and codes
                userCurrency = when {
                    country.equals("Nigeria", ignoreCase = true) || country.equals("NG", ignoreCase = true) -> "NGN"
                    country.equals("India", ignoreCase = true) || country.equals("IN", ignoreCase = true) -> "INR"
                    else -> "INR" // Default to INR
                }
                kycStatus = userDoc.getString("kycStatus") ?: "pending"
                showKycDialog = kycStatus?.lowercase() != "verified"
            } catch (e: Exception) {
                userCountry = "India"
                userCurrency = "INR"
                kycStatus = "pending"
                showKycDialog = true
            }
        }
    }

    // UPI Intent launcher (for Indian users)
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
    LaunchedEffect(currentUser, isAuthenticated) {
        if (isAuthenticated && !isLoading) {
            // Use currentUser.uid if available, otherwise get from DataStore
            val userId = currentUser?.uid
            if (userId != null) {
                Log.d("WalletScreen", "Loading wallet data with Firebase user ID: $userId")
                Log.d("WalletScreen", "User country: $userCountry, Currency: $userCurrency")
                walletViewModel.loadWalletData(userId)
                walletViewModel.loadWithdrawalRequests(userId)
                
                // Check if wallet document exists in separate collection
                coroutineScope.launch {
                    try {
                        val walletDoc = FirebaseFirestore.getInstance()
                            .collection("wallets")
                            .document(userId)
                            .get()
                            .await()
                        Log.d("WalletScreen", "Wallet document exists: ${walletDoc.exists()}")
                        if (walletDoc.exists()) {
                            val walletData = walletDoc.data
                            Log.d("WalletScreen", "Wallet data: $walletData")
                        } else {
                            Log.d("WalletScreen", "Wallet document does not exist, creating one...")
                            // Create wallet document if it doesn't exist
                            val walletData = mapOf(
                                "userId" to userId,
                                "balance" to 0.0,
                                "withdrawableBalance" to 0.0,
                                "bonusBalance" to 0.0
                            )
                            FirebaseFirestore.getInstance()
                                .collection("wallets")
                                .document(userId)
                                .set(walletData)
                                .await()
                            Log.d("WalletScreen", "Wallet document created successfully")
                        }
                    } catch (e: Exception) {
                        Log.e("WalletScreen", "Error checking/creating wallet document: ${e.message}")
                    }
                }
            } else {
                // If Firebase Auth session is lost but user is authenticated from DataStore,
                // get the user ID from DataStore
                Log.d("WalletScreen", "User authenticated but Firebase session lost, getting user ID from DataStore")
                val dataStoreUserId = authViewModel.getUserIdFromDataStore()
                if (dataStoreUserId != null) {
                    Log.d("WalletScreen", "Loading wallet data with DataStore user ID: $dataStoreUserId")
                    walletViewModel.loadWalletData(dataStoreUserId)
                    walletViewModel.loadWithdrawalRequests(dataStoreUserId)
                } else {
                    Log.e("WalletScreen", "Failed to get user ID from DataStore")
                }
            }
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
            // KYC Banner at the very top
            if (kycStatus?.lowercase() != "verified") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF8E1), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFF57C00),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Complete KYC to use wallet features.",
                            color = Color(0xFFF57C00),
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { navController.navigate(ScreenRoutes.KycScreen) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("KYC", color = Color(0xFFF57C00), fontSize = 13.sp)
                        }
                    }
                }
                Divider(color = Color(0xFFFFE082), thickness = 1.dp)
            }
            // Top Bar
            WalletTopBar(walletBalance = walletBalance, currency = userCurrency)

            if (!isAuthenticated) {
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
                                if (isAuthenticated && currentUser != null) {
                                    walletViewModel.loadWalletData(currentUser!!.uid)
                                } else if (isAuthenticated && currentUser == null) {
                                    Log.d("WalletScreen", "Retry clicked but Firebase session lost")
                                    // You might want to trigger session restoration here
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
                            currency = userCurrency,
                            onWithdrawClick = { showWithdrawDialog = true },
                            enabled = (kycStatus?.lowercase() == "verified")
                        )
                    }

                    // Quick Actions
                    item {
                        QuickActions(
                            onDepositClick = { 
                                if (userCountry.equals("Nigeria", ignoreCase = true) || userCountry.equals("NG", ignoreCase = true)) {
                                    showNGNDepositDialog = true
                                } else {
                                    showAmountSheet = true
                                }
                            },
                            userCountry = userCountry,
                            enabled = (kycStatus?.lowercase() == "verified")
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
                            PendingDepositItem(deposit, userCurrency)
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
                            WithdrawalRequestItem(request = request, currency = userCurrency)
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
                            TransactionItem(transaction, userCurrency)
                        }
                    }
                }
            }
        }
    }

    // Indian UPI Payment Sheet
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

    // Nigerian Deposit Dialog
    if (showNGNDepositDialog) {
        NGNDepositDialog(
            onDismiss = { showNGNDepositDialog = false },
            onDeposit = { amount, paymentMethod, bankDetails, screenshotUrl ->
                // Handle NGN deposit
                if (isAuthenticated && currentUser != null) {
                    // Create pending deposit for NGN
                    // This would typically integrate with Flutterwave/Paystack
                    Log.d("WalletScreen", "Creating NGN deposit: $amount via $paymentMethod")
                    // TODO: Implement actual NGN payment integration
                }
                showNGNDepositDialog = false
            }
        )
    }

    if (showWithdrawDialog) {
        WithdrawDialog(
            maxAmount = withdrawableBalance,
            isLoading = isLoading,
            currency = userCurrency,
            userCountry = userCountry,
            onDismiss = { showWithdrawDialog = false },
            onWithdraw = { amount, paymentDetails ->
                if (isAuthenticated && currentUser != null) {
                    val userDetails = UserDetails(
                        email = currentUser!!.email ?: "",
                        name = currentUser!!.displayName ?: "",
                        username = currentUser!!.email ?: "",
                        userId = currentUser!!.uid
                    )
                    val request = WithdrawalRequest(
                        userId = currentUser!!.uid,
                        amount = amount,
                        currency = userCurrency,
                        upiId = if (userCountry.equals("India", ignoreCase = true) || userCountry.equals("IN", ignoreCase = true)) paymentDetails else "",
                        userDetails = userDetails,
                        paymentMethod = if (userCountry.equals("Nigeria", ignoreCase = true) || userCountry.equals("NG", ignoreCase = true)) {
                            NGNTransactionUtils.getPaymentMethodForCountry("NG", paymentDetails)
                        } else {
                            NGNTransactionUtils.getPaymentMethodForCountry("IN", "UPI")
                        },
                        bankName = if (userCountry.equals("Nigeria", ignoreCase = true) || userCountry.equals("NG", ignoreCase = true)) paymentDetails else null,
                        userCountry = userCountry
                    )
                    walletViewModel.createWithdrawalRequest(request)
                    withdrawSuccess = true
                } else if (isAuthenticated && currentUser == null) {
                    // Handle case where user is authenticated from DataStore but Firebase session is lost
                    Log.d("WalletScreen", "User authenticated but Firebase session lost during withdrawal")
                    // You might want to show an error message or trigger session restoration
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
private fun WalletTopBar(walletBalance: Double, currency: String) {
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
                NGNTransactionUtils.formatAmount(walletBalance, currency),
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
    currency: String,
    onWithdrawClick: () -> Unit,
    enabled: Boolean
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
                text = NGNTransactionUtils.formatAmount(withdrawableBalance, currency),
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
                text = NGNTransactionUtils.formatAmount(bonusBalance, currency),
                color = Color(0xFF00BCD4),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onWithdrawClick,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (currency == "NGN") "Withdraw to Bank" else "Withdraw to UPI", 
                    color = Color.Black, 
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun QuickActions(
    onDepositClick: () -> Unit,
    userCountry: String,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickActionButton(
            icon = Icons.Default.Add,
            label = if (userCountry.equals("Nigeria", ignoreCase = true) || userCountry.equals("NG", ignoreCase = true)) "Add Money (NGN)" else "Add Money",
            onClick = onDepositClick,
            enabled = enabled
        )
        QuickActionButton(
            icon = Icons.Default.History,
            label = "History",
            onClick = { /* TODO: Navigate to detailed history */ },
            enabled = true
        )
        QuickActionButton(
            icon = Icons.Default.Settings,
            label = "Settings",
            onClick = { /* TODO: Navigate to wallet settings */ },
            enabled = true
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
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
            color = if (enabled) Color.White else Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PendingDepositItem(deposit: com.cehpoint.netwin.data.model.PendingDeposit, currency: String) {
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
                    text = NGNTransactionUtils.formatAmount(deposit.amount, currency),
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
private fun TransactionItem(transaction: Transaction, currency: String) {
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
                    text = "${if (transaction.type in listOf(TransactionType.DEPOSIT, TransactionType.UPI_DEPOSIT, TransactionType.TOURNAMENT_WINNING)) "+" else "-"}${NGNTransactionUtils.formatAmount(transaction.amount, currency)}",
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
fun NGNDepositDialog(
    onDismiss: () -> Unit,
    onDeposit: (Double, String, String?, String?) -> Unit
) {
    var selectedAmount by remember { mutableStateOf(0) }
    var manualAmount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var paystackLink by remember { mutableStateOf("") }
    var paystackReference by remember { mutableStateOf("") }
    var screenshotUri by remember { mutableStateOf<Uri?>(null) }
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val presetAmounts = listOf(1000, 2000, 5000, 10000)

    // Image picker launcher
    val screenshotPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        screenshotUri = uri
    }

    // Fetch Paystack link from Firestore when dialog is shown
    LaunchedEffect(Unit) {
        val firestore = FirebaseFirestore.getInstance()
        val walletConfigRef = firestore.collection("admin_config").document("wallet_config")
        try {
            val doc = walletConfigRef.get().await()
            val ngnConfig = doc.get("NGN") as? Map<*, *>
            paystackLink = ngnConfig?.get("paymentLink") as? String ?: ""
        } catch (_: Exception) {
            paystackLink = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(20.dp),
        title = { 
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00BCD4)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Add Money to Wallet",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Select amount and pay with Paystack",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Amount Selection Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color(0xFF00BCD4),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Select Amount",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Preset amounts
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(80.dp)
                        ) {
                            items(presetAmounts) { amt ->
                                Button(
                                    onClick = {
                                        selectedAmount = amt
                                        manualAmount = ""
                                        error = null
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedAmount == amt) Color(0xFF00BCD4) else Color(0xFF3A3A3A)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "₦$amt",
                                        color = if (selectedAmount == amt) Color.Black else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Or enter custom amount",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = manualAmount,
                            onValueChange = {
                                manualAmount = it.filter { c -> c.isDigit() }
                                selectedAmount = 0
                                error = null
                            },
                            label = { Text("Amount (₦)", color = Color.Gray) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00BCD4),
                                unfocusedBorderColor = Color.Gray,
                                cursorColor = Color(0xFF00BCD4),
                                focusedLabelColor = Color(0xFF00BCD4),
                                unfocusedLabelColor = Color.Gray,
                                focusedContainerColor = Color(0xFF3A3A3A),
                                unfocusedContainerColor = Color(0xFF3A3A3A)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Paystack Payment Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Payment,
                                contentDescription = null,
                                tint = Color(0xFF00BCD4),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Paystack Payment",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            "Pay securely with Paystack",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (paystackLink.isNotBlank()) {
                            Button(
                                onClick = { uriHandler.openUri(paystackLink) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open Paystack Payment Page", color = Color.Black, fontWeight = FontWeight.Medium)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "After payment, enter your transaction reference and upload a screenshot.",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            OutlinedTextField(
                                value = paystackReference,
                                onValueChange = { paystackReference = it },
                                label = { Text("Transaction Reference", color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00BCD4),
                                    unfocusedBorderColor = Color.Gray,
                                    cursorColor = Color(0xFF00BCD4),
                                    focusedLabelColor = Color(0xFF00BCD4),
                                    unfocusedLabelColor = Color.Gray,
                                    focusedContainerColor = Color(0xFF3A3A3A),
                                    unfocusedContainerColor = Color(0xFF3A3A3A)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Button(
                                onClick = { screenshotPickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A3A3A)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    if (screenshotUri == null) Icons.Default.Upload else Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (screenshotUri == null) "Upload Screenshot" else "Change Screenshot",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            screenshotUri?.let { uri ->
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3A3A3A)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(uri),
                                        contentDescription = "Screenshot Preview",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        } else {
                            Text(
                                "Unable to load Paystack link. Please try again later.",
                                color = Color.Red,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (error != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF3A2A2A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                error ?: "",
                                color = Color.Red,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = if (selectedAmount > 0) selectedAmount else manualAmount.toIntOrNull() ?: 0
                    if (amount < 100) {
                        error = "Minimum amount is ₦100"
                        return@Button
                    }
                    if (amount > 1000000) {
                        error = "Maximum amount is ₦1,000,000"
                        return@Button
                    }
                    if (paystackReference.isBlank()) {
                        error = "Please enter your Paystack transaction reference"
                        return@Button
                    }
                    if (screenshotUri == null) {
                        error = "Please upload a screenshot of your payment"
                        return@Button
                    }
                    onDeposit(
                        amount.toDouble(),
                        "PAYSTACK",
                        paystackReference,
                        screenshotUri?.toString()
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Proceed with Payment", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF00BCD4))
            ) {
                Text("Cancel", fontWeight = FontWeight.Medium)
            }
        }
    )
}

@Composable
fun WithdrawDialog(
    maxAmount: Double,
    isLoading: Boolean,
    currency: String,
    userCountry: String,
    onDismiss: () -> Unit,
    onWithdraw: (Double, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var paymentDetails by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    val isNigerian = userCountry.equals("Nigeria", ignoreCase = true) || userCountry.equals("NG", ignoreCase = true)
    
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
                    label = { Text("Amount (${NGNTransactionUtils.getCurrencySymbol(currency)})") },
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
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = paymentDetails,
                    onValueChange = {
                        paymentDetails = it
                        error = null
                    },
                    label = { 
                        Text(
                            if (isNigerian) "Bank Name (e.g., GTB, ZENITH)" else "Your UPI ID",
                            color = Color.Gray
                        ) 
                    },
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Max withdrawable: ${NGNTransactionUtils.formatAmount(maxAmount, currency)}", 
                    color = Color.Gray, 
                    fontSize = 12.sp
                )
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
                    if (isNigerian) {
                        if (paymentDetails.isBlank()) {
                            error = "Please enter bank name"
                            return@Button
                        }
                    } else {
                        if (paymentDetails.isBlank() || !paymentDetails.contains("@")) {
                            error = "Enter a valid UPI ID"
                            return@Button
                        }
                    }
                    onWithdraw(amt, paymentDetails)
                    amount = ""
                    paymentDetails = ""
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
fun WithdrawalRequestItem(request: WithdrawalRequest, currency: String) {
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
                Text(
                    NGNTransactionUtils.formatAmount(request.amount, currency), 
                    color = Color.White, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 18.sp
                )
                Text(
                    if (request.userCountry.equals("Nigeria", ignoreCase = true) || request.userCountry.equals("NG", ignoreCase = true)) (request.bankName ?: "Bank Transfer") else request.upiId, 
                    color = Color.Gray, 
                    fontSize = 14.sp
                )
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