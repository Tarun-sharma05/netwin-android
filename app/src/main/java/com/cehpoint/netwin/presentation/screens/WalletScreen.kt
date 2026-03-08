package com.cehpoint.netwin.presentation.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.cehpoint.netwin.data.model.ManualUpiDeposit
import com.cehpoint.netwin.data.model.PendingDeposit
import com.cehpoint.netwin.data.model.Transaction
import com.cehpoint.netwin.data.model.TransactionStatus
import com.cehpoint.netwin.data.model.TransactionType
import com.cehpoint.netwin.data.model.UserDetails
import com.cehpoint.netwin.data.model.WithdrawalRequest
import com.cehpoint.netwin.presentation.components.StatusChip
import com.cehpoint.netwin.presentation.components.statusBarPadding
import com.cehpoint.netwin.presentation.navigation.ScreenRoutes
import com.cehpoint.netwin.presentation.theme.NetwinTokens
import com.cehpoint.netwin.presentation.viewmodels.AuthViewModel
import com.cehpoint.netwin.presentation.viewmodels.WalletViewModel
import com.cehpoint.netwin.ui.theme.DarkBackground
import com.cehpoint.netwin.ui.theme.DarkCard
import com.cehpoint.netwin.ui.theme.DarkSurface
import com.cehpoint.netwin.ui.theme.ErrorRed
import com.cehpoint.netwin.ui.theme.NetWinCyan
import com.cehpoint.netwin.ui.theme.NetWinPink
import com.cehpoint.netwin.ui.theme.NetWinPurple
import com.cehpoint.netwin.ui.theme.SuccessGreen
import com.cehpoint.netwin.ui.theme.WarningYellow
import com.cehpoint.netwin.utils.NGNTransactionUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var selectedAmount by rememberSaveable { mutableStateOf(0) }
    var manualAmount by rememberSaveable { mutableStateOf("") }
    var upiResultMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showWithdrawDialog by rememberSaveable { mutableStateOf(false) }
    var showNGNDepositDialog by rememberSaveable { mutableStateOf(false) }
    val presetAmounts = listOf(100, 200, 500)
    val presetNGNAmounts = listOf(1000, 2000, 5000)
    val amountSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var withdrawSuccess by rememberSaveable { mutableStateOf(false) }

    // Get user country and currency
    var userCountry by rememberSaveable { mutableStateOf("IN") }
    var userCurrency by rememberSaveable { mutableStateOf("INR") }
    
    var kycStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var showKycDialog by rememberSaveable { mutableStateOf(false) }
    
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("wallet_payment_state", Context.MODE_PRIVATE)


    // Backup mechanism for payment proof dialog
    LaunchedEffect(upiResultMessage) {
        if (upiResultMessage != null) {
            sharedPreferences.edit()
                .putString("upi_result_message", upiResultMessage)
                .putInt("selected_amount", selectedAmount)
                .putString("manual_amount", manualAmount)
                .putString("user_currency", userCurrency)
                .apply()
        } else {
            sharedPreferences.edit()
                .remove("upi_result_message")
                .apply()
        }
    }
    
    // Restore state if needed
    LaunchedEffect(Unit) {
        if (upiResultMessage == null) {
            val savedMessage = sharedPreferences.getString("upi_result_message", null)
            if (savedMessage != null) {
                upiResultMessage = savedMessage
                selectedAmount = sharedPreferences.getInt("selected_amount", 0)
                manualAmount = sharedPreferences.getString("manual_amount", "") ?: ""
                userCurrency = sharedPreferences.getString("user_currency", "INR") ?: "INR"
                
                // Clear the backup after successful restore to prevent duplicate restores
                sharedPreferences.edit()
                    .remove("upi_result_message")
                    .apply()
            }
        }
    }
    
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
        Log.d("WalletScreen", "UPI Result - resultCode: ${result.resultCode}, data: ${result.data}")
        
        val data: Intent? = result.data
        val response = data?.getStringExtra("response")
        val txnId = data?.getStringExtra("txnId")
        val status = data?.getStringExtra("Status")
        
        Log.d("WalletScreen", "UPI Response - response: $response, txnId: $txnId, status: $status")
        
        // Check for successful payment using multiple indicators
        val isPaymentSuccessful = when {
            // Check explicit success status
            status.equals("SUCCESS", ignoreCase = true) -> {
                Log.d("WalletScreen", "Payment successful detected via status: $status")
                true
            }
            // Check response for success indicators
            response != null && (
                response.contains("SUCCESS", ignoreCase = true) ||
                response.contains("success", ignoreCase = true) ||
                response.contains("COMPLETED", ignoreCase = true) ||
                response.contains("completed", ignoreCase = true)
            ) -> {
                Log.d("WalletScreen", "Payment successful detected via response: $response")
                true
            }
            // Check if we have a transaction ID (indicates payment was processed)
            !txnId.isNullOrBlank() -> {
                Log.d("WalletScreen", "Payment successful detected via transaction ID: $txnId")
                true
            }
            // Special case: Some UPI apps return resultCode 0 with successful payments
            // If user reports successful payment but we get resultCode 0 and null data,
            // we should still consider it potentially successful
            result.resultCode == Activity.RESULT_CANCELED && data == null -> {
                Log.d("WalletScreen", "Potential successful payment - resultCode 0 with null data (some UPI apps do this)")
                // For now, we'll be cautious and not auto-navigate, but we'll show a helpful message
                false
            }
            // Default to failed if no clear success indicators
            else -> {
                Log.d("WalletScreen", "Payment NOT successful - status: '$status', response: '$response', txnId: '$txnId'")
                false
            }
        }
        
        if (isPaymentSuccessful) {
            Log.d("WalletScreen", "UPI Payment successful - processing...")
            Log.d("WalletScreen", "Selected amount: $selectedAmount, Manual amount: '$manualAmount'")
            
            // Get the payment amount from the selected amount or manual amount
            val paymentAmount = if (selectedAmount > 0) selectedAmount else manualAmount.toIntOrNull() ?: 0
            
            Log.d("WalletScreen", "Detected payment amount: $paymentAmount, User currency: $userCurrency")
            
            if (paymentAmount > 0) {
                // Navigate to payment proof screen
                Log.d("WalletScreen", "Attempting to navigate to payment proof screen...")
                try {
                    val upiAppPackage = sharedPreferences.getString("last_upi_app_package", null)
                    Log.d("WalletScreen", "Retrieved UPI app package: $upiAppPackage")
                    navController.navigate(ScreenRoutes.PaymentProofScreen(amount = paymentAmount.toDouble(), currency = userCurrency, upiAppPackage = upiAppPackage))
                    Log.d("WalletScreen", "Navigation to payment proof screen initiated successfully")
                } catch (e: Exception) {
                    Log.e("WalletScreen", "Failed to navigate to payment proof screen", e)
                    upiResultMessage = "Payment successful! Please go to wallet to submit proof."
                }
            } else {
                // Fallback to showing success message if amount is not available
                Log.w("WalletScreen", "Payment amount is 0 or invalid, showing success message instead")
                upiResultMessage = "Payment successful! Your wallet will be updated shortly."
            }
            
            // TODO: Credit wallet, update Firestore, etc.
        } else {
            // Handle the case where payment might have been successful but we can't detect it
            if (result.resultCode == Activity.RESULT_CANCELED && data == null) {
                Log.d("WalletScreen", "Payment completed but confirmation unclear - showing manual verification message")
                upiResultMessage = "Payment may have been successful! If you completed the payment, please check your email/SMS for confirmation and contact support if needed."
            } else {
                Log.d("WalletScreen", "UPI Payment failed or cancelled")
                upiResultMessage = "Payment failed or cancelled. Please try again."
            }
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
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
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
                HorizontalDivider(thickness = 1.dp, color = Color(0xFFFFE082))
            }
            // Improved Top Bar with better hierarchy
            ImprovedWalletTopBar(totalBalance = walletBalance, currency = userCurrency)

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
                    // Enhanced Balance Card with better visual hierarchy
                    item {
                        EnhancedBalanceCard(
                            totalBalance = walletBalance,
                            withdrawableBalance = withdrawableBalance,
                            bonusBalance = bonusBalance,
                            currency = userCurrency,
                            onWithdrawClick = { showWithdrawDialog = true },
                            enabled = (kycStatus?.lowercase() == "verified"),
                        )
                    }
                    // Enhanced Quick Actions
                    item {
                        EnhancedQuickActions(
                            onDepositClick = { 
                                if (userCountry.equals("Nigeria", ignoreCase = true) || userCountry.equals("NG", ignoreCase = true)) {
                                    showNGNDepositDialog = true
                                } else {
                                    // Navigate to new Manual UPI deposit Screen
                                    navController.navigate(ScreenRoutes.ManualUpiDepositScreen)
                                }
                            },
                            onWithdrawClick = { showWithdrawDialog = true },
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
                            EnhancedTransactionItem(transaction, userCurrency)
                        }
                    }
                }
            }
        }
    }

    // Indian UPI Payment Sheet
    if (showAmountSheet) {
        var upiId by remember { mutableStateOf("") }
        var merchantName by remember { mutableStateOf("NetWin") }
        
        // Fetch UPI settings from Firestore
        LaunchedEffect(Unit) {
            Log.w("WalletScreen", "=== UPI FETCH STARTED ===")
            try {
                Log.w("WalletScreen", "Starting UPI settings fetch...")
                val firestore = FirebaseFirestore.getInstance()
                val upiConfigRef = firestore.collection("admin_config").document("upi_settings")
                Log.w("WalletScreen", "Fetching from path: admin_config/upi_settings")
                
                val doc = upiConfigRef.get().await()
                
                Log.w("WalletScreen", "Document exists: ${doc.exists()}")
                Log.w("WalletScreen", "Document data: ${doc.data}")
                
                if (doc.exists()) {
                    // Try different possible field names
                    val possibleUpiFields = listOf("upiId", "upi_id", "UPI_ID", "upi")
                    val possibleNameFields = listOf("merchantName", "merchant_name", "name", "businessName", "displayName")
                    
                    var fetchedUpiId: String? = null
                    var fetchedMerchantName: String? = null
                    
                    // Try to find UPI ID with different field names
                    for (field in possibleUpiFields) {
                        fetchedUpiId = doc.getString(field)
                        if (!fetchedUpiId.isNullOrBlank()) {
                            Log.w("WalletScreen", "Found UPI ID in field '$field': '$fetchedUpiId'")
                            break
                        }
                    }
                    
                    // Try to find merchant name with different field names
                    for (field in possibleNameFields) {
                        fetchedMerchantName = doc.getString(field)
                        if (!fetchedMerchantName.isNullOrBlank()) {
                            Log.w("WalletScreen", "Found merchant name in field '$field': '$fetchedMerchantName'")
                            break
                        }
                    }
                    
                    Log.w("WalletScreen", "Final fetched UPI ID: '$fetchedUpiId'")
                    Log.w("WalletScreen", "Final fetched Merchant Name: '$fetchedMerchantName'")
                    
                    if (!fetchedUpiId.isNullOrBlank()) {
                        upiId = fetchedUpiId.trim()
                        merchantName = fetchedMerchantName?.trim() ?: "NetWin"
                        Log.w("WalletScreen", "✅ SUCCESS - UPI ID: '$upiId', Merchant: '$merchantName'")
                    } else {
                        Log.e("WalletScreen", "❌ ERROR - No valid UPI ID found in any field")
                        upiId = ""
                    }
                } else {
                    Log.e("WalletScreen", "❌ ERROR - UPI settings document does not exist at admin_config/upi_settings")
                    upiId = ""
                }
            } catch (e: Exception) {
                Log.e("WalletScreen", "❌ ERROR - Failed to fetch UPI settings: ${e.message}", e)
                upiId = ""
            }
            Log.w("WalletScreen", "=== UPI FETCH COMPLETED ===")
        }
        
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
                Spacer(modifier = Modifier.height(16.dp))
//
//                // Debug info for UPI ID loading
//                if (upiId.isBlank()) {
//                    Text(
//                        "Loading UPI settings...",
//                        color = Color.Gray,
//                        fontSize = 12.sp,
//                        modifier = Modifier.padding(bottom = 8.dp)
//                    )
//                } else {
//                    Text(
//                        "UPI ID: $upiId",
//                        color = Color.Gray,
//                        fontSize = 12.sp,
//                        modifier = Modifier.padding(bottom = 8.dp)
//                    )
//                }
//
                Spacer(modifier = Modifier.height(8.dp))
                
                val currentAmount = if (selectedAmount > 0) selectedAmount else manualAmount.toIntOrNull() ?: 0
                val isAmountValid = currentAmount > 0
                val isUpiReady = upiId.isNotBlank()
                
                Button(
                    onClick = {
                        val amount = if (selectedAmount > 0) selectedAmount else manualAmount.toIntOrNull() ?: 0
                        Log.d("WalletScreen", "Payment - Amount: $amount, Currency: $userCurrency")
                        
                        if (amount > 0) {
                            coroutineScope.launch {
                                showAmountSheet = false
                                

                            }
                        } else {
                            Log.e("WalletScreen", "Invalid payment amount: $amount")
                        }
                    },
                    enabled = isAmountValid && (userCurrency == "INR" || isUpiReady),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAmountValid && (userCurrency == "INR" || isUpiReady)) Color(0xFF00BCD4) else Color.Gray,
                        disabledContainerColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when {
                            !isAmountValid -> "Enter Amount"
                            userCurrency == "INR" -> "Pay with Razorpay"
                            !isUpiReady -> "Loading..."
                            else -> "Pay with UPI"
                        },
                        color = if (isAmountValid && (userCurrency == "INR" || isUpiReady)) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold
                    )
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
            onDeposit = { amount, paymentMethod, transactionReference, screenshotUriString ->
                // Handle NGN deposit with screenshot upload
                if (isAuthenticated && currentUser != null && screenshotUriString != null && transactionReference != null) {
                    coroutineScope.launch {
                        try {
                            Log.d("WalletScreen", "Starting NGN deposit: amount=$amount, reference=$transactionReference")
                            
                            // 1. Upload screenshot to Firebase Storage
                            val screenshotUri = Uri.parse(screenshotUriString)
                            val uploadResult = walletViewModel.uploadDepositScreenshot(
                                uri = screenshotUri,
                                userId = currentUser!!.uid
                            )
                            
                            if (uploadResult.isSuccess) {
                                val screenshotUrl = uploadResult.getOrNull()!!
                                Log.d("WalletScreen", "Screenshot uploaded successfully: $screenshotUrl")
                                
                                // 2. Create NGN deposit request in Firestore
                                walletViewModel.createNgnDepositRequest(
                                    userId = currentUser!!.uid,
                                    amount = amount,
                                    transactionReference = transactionReference,
                                    screenshotUrl = screenshotUrl,
                                    currency = "NGN"
                                )
                                
                                // 3. Show success message
                                Toast.makeText(
                                    context,
                                    "Deposit request submitted! Your balance will update after admin approval.",
                                    Toast.LENGTH_LONG
                                ).show()
                                
                                Log.d("WalletScreen", "NGN deposit request created successfully")
                            } else {
                                val error = uploadResult.exceptionOrNull()?.message ?: "Failed to upload screenshot"
                                Log.e("WalletScreen", "Screenshot upload failed: $error")
                                Toast.makeText(context, "Failed to upload screenshot: $error", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Log.e("WalletScreen", "Error processing NGN deposit", e)
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
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
                    walletViewModel.createWithdrawalRequest(
                        userId = request.userId,
                        amount = request.amount,
                        paymentMethod = request.paymentMethod,
                        bankName = request.bankName,
                        accountNumber = request.accountNumber,
                        accountName = request.accountName
                    )
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
        val isUncertainSuccess = msg.contains("may have been successful", ignoreCase = true) || 
                               msg.contains("Payment completed but confirmation unclear", ignoreCase = true)
        
        AlertDialog(
            onDismissRequest = { upiResultMessage = null },
            title = { Text("UPI Payment") },
            text = { 
                Column {
                    Text(msg)
                    if (isUncertainSuccess) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "If you're sure the payment was successful, you can proceed to submit proof.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isUncertainSuccess) {
                        TextButton(
                            onClick = {
                                // Get the payment amount and navigate to payment proof screen
                                val paymentAmount = if (selectedAmount > 0) selectedAmount else manualAmount.toIntOrNull() ?: 0
                                if (paymentAmount > 0) {
                                    try {
                                        val upiAppPackage = sharedPreferences.getString("last_upi_app_package", null)
                                        navController.navigate(ScreenRoutes.PaymentProofScreen(amount = paymentAmount.toDouble(), currency = userCurrency, upiAppPackage = upiAppPackage))
                                        Log.d("WalletScreen", "Manual verification - navigating to payment proof screen")
                                    } catch (e: Exception) {
                                        Log.e("WalletScreen", "Failed to navigate to payment proof screen", e)
                                    }
                                }
                                upiResultMessage = null
                            }
                        ) {
                            Text("Submit Proof", color = Color(0xFF00BCD4))
                        }
                    }
                    Button(
                        onClick = { upiResultMessage = null }
                    ) {
                        Text("OK")
                    }
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
    icon: ImageVector,
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
private fun PendingDepositItem(deposit: ManualUpiDeposit, currency: String) {
    Log.d("PendingDepositItem", "Rendering deposit: ${deposit.upiTransactionId}, amount: ${deposit.amount}, status: ${deposit.status}")
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
                        text = "Pending deposit",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "UPI Txn ID: ${deposit.upiTransactionId}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    if (deposit.userUpiId.isNotBlank()) {
                        Text(
                            text = "Your UPI ID: ${deposit.userUpiId}",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    if (!deposit.paymentScreenshot.isNullOrBlank()) {
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

private fun formatDate(timestamp: Timestamp?): String {
    if (timestamp == null) return "Unknown"
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}

// Enhanced Components for MVP-Ready Wallet Screen

@Composable
private fun ImprovedWalletTopBar(totalBalance: Double, currency: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(DarkBackground)
           // .statusBarPadding()
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
        // Simplified top balance - less prominent than main balance card
//        Text(
//            NGNTransactionUtils.formatAmount(totalBalance, currency),
//            color = Color.Gray,
//            fontSize = 14.sp,
//            fontWeight = FontWeight.Medium
//        )
        Surface (
//            color = NetwinTokens.SurfaceAlt,
//            shape = RoundedCornerShape(12.dp),
//            border = BorderStroke(1.dp, NetwinTokens.Primary.copy(alpha = 0.24f))
            shape = RoundedCornerShape(12.dp),
            border =  BorderStroke(
                1.dp,
                Brush.horizontalGradient(listOf(NetWinPurple, NetWinPink, NetWinCyan))
            )
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
                    NGNTransactionUtils.formatAmountTidy(totalBalance, currency),
                    modifier = Modifier.padding(start = 6.dp),
                    color = NetwinTokens.TextPrimary,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize
                )
            }
        }

    }
}

@Composable
private fun EnhancedBalanceCard(
    totalBalance: Double,
    withdrawableBalance: Double,
    bonusBalance: Double,
    currency: String,
    onWithdrawClick: () -> Unit,
    enabled: Boolean
) {
    // Log raw values received
    Log.d("EnhancedBalanceCard", "RAW VALUES - withdrawableBalance: $withdrawableBalance, bonusBalance: $bonusBalance, currency: $currency")
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkCard
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Tabbed Balance Display - Esports Style
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // User Added Balance Tab
                Log.d("Wallet Screen","EnhancedBalanceCardUser Added Balance: ${NGNTransactionUtils.formatAmountTidy(withdrawableBalance, currency)}")
                EsportsBalanceTab(
                    label = "User Added Balance",
                    amount = NGNTransactionUtils.formatAmountTidy(withdrawableBalance, currency),
                    color = NetWinCyan,
                    isSelected = true,
                    modifier = Modifier.weight(1f)
                )

                // Bonus Winnings Tab
                Log.d("Wallet Screen", "EnhancedBalanceCardBonus Balance: ${NGNTransactionUtils.formatAmountTidy(bonusBalance, currency)}")
                EsportsBalanceTab(
                    label = "Bonus Winnings",
                    amount = NGNTransactionUtils.formatAmountTidy(bonusBalance, currency),
                    color = WarningYellow,
                    isSelected = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BalanceInfoItem(
    label: String,
    amount: String,
    color: Color,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = amount,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EnhancedQuickActions(
    onDepositClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    userCountry: String,
    enabled: Boolean
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Quick Actions",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Add Cash Button - Green
            EsportsActionButton(
                icon = Icons.Default.Add,
                label = "Add Cash",
                onClick = onDepositClick,
                enabled = enabled,
                backgroundColor = SuccessGreen,
                modifier = Modifier.weight(1f)
            )
            
            // Withdraw Button - Red
            EsportsActionButton(
                icon = Icons.Default.GetApp,
                label = "Withdraw",
                onClick = onWithdrawClick,
                enabled = enabled,
                backgroundColor = ErrorRed,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EnhancedActionButton(
    icon: ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean,
    gradient: List<Color>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(60.dp)
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (enabled) Brush.horizontalGradient(gradient) 
                           else Brush.horizontalGradient(listOf(Color.Gray, Color.Gray)),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun EnhancedTransactionItem(transaction: Transaction, currency: String) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Enhanced transaction icon with gradient background
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = when (transaction.type) {
                                        // User Added Deposits (Green)
                                        TransactionType.DEPOSIT, TransactionType.MANUAL_DEPOSIT, 
                                        TransactionType.UPI_DEPOSIT, TransactionType.BANK_TRANSFER_DEPOSIT,
                                        TransactionType.CARD_PAYMENT, TransactionType.MOBILE_MONEY -> 
                                            listOf(SuccessGreen, SuccessGreen.copy(alpha = 0.7f))
                                        
                                        // Withdrawals (Red)
                                        TransactionType.WITHDRAWAL, TransactionType.MANUAL_WITHDRAWAL,
                                        TransactionType.UPI_WITHDRAWAL, TransactionType.BANK_TRANSFER_WITHDRAWAL -> 
                                            listOf(ErrorRed, ErrorRed.copy(alpha = 0.7f))
                                        
                                        // Tournament Entry/Fees (Purple)
                                        TransactionType.TOURNAMENT_ENTRY, TransactionType.ENTRY_FEE -> 
                                            listOf(NetWinPurple, NetWinPink)
                                        
                                        // Winnings/Bonuses (Yellow/Gold)
                                        TransactionType.TOURNAMENT_WINNING, TransactionType.BONUS_CREDIT, 
                                        TransactionType.REFUND -> 
                                            listOf(WarningYellow, WarningYellow.copy(alpha = 0.7f))
                                        
                                        else -> listOf(Color.Gray, Color.Gray.copy(alpha = 0.7f))
                                    }
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (transaction.type) {
                                // Deposits
                                TransactionType.DEPOSIT, TransactionType.MANUAL_DEPOSIT,
                                TransactionType.UPI_DEPOSIT, TransactionType.BANK_TRANSFER_DEPOSIT,
                                TransactionType.CARD_PAYMENT, TransactionType.MOBILE_MONEY -> Icons.Default.Add
                                
                                // Withdrawals
                                TransactionType.WITHDRAWAL, TransactionType.MANUAL_WITHDRAWAL,
                                TransactionType.UPI_WITHDRAWAL, TransactionType.BANK_TRANSFER_WITHDRAWAL -> Icons.Default.Remove
                                
                                // Tournament Entry
                                TransactionType.TOURNAMENT_ENTRY, TransactionType.ENTRY_FEE -> Icons.Default.PlayArrow
                                
                                // Winnings/Bonuses
                                TransactionType.TOURNAMENT_WINNING -> Icons.Default.EmojiEvents
                                TransactionType.BONUS_CREDIT -> Icons.Default.Star
                                TransactionType.REFUND -> Icons.Default.Refresh
                                
                                else -> Icons.Default.Info
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = transaction.description,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 1
                        )
                        Text(
                            text = formatTransactionDate(transaction.createdAt),
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    // Determine if transaction is credit (+) or debit (-)
                    val isCredit = transaction.type in listOf(
                        TransactionType.DEPOSIT, TransactionType.MANUAL_DEPOSIT,
                        TransactionType.UPI_DEPOSIT, TransactionType.BANK_TRANSFER_DEPOSIT,
                        TransactionType.CARD_PAYMENT, TransactionType.MOBILE_MONEY,
                        TransactionType.TOURNAMENT_WINNING, TransactionType.BONUS_CREDIT,
                        TransactionType.REFUND
                    )
                    
                    Text(
                        text = "${if (isCredit) "+" else "-"}${NGNTransactionUtils.formatAmount(transaction.amount, currency)}",
                        color = when {
                            // Credits (Green for deposits)
                            transaction.type in listOf(
                                TransactionType.DEPOSIT, TransactionType.MANUAL_DEPOSIT,
                                TransactionType.UPI_DEPOSIT, TransactionType.BANK_TRANSFER_DEPOSIT,
                                TransactionType.CARD_PAYMENT, TransactionType.MOBILE_MONEY
                            ) -> SuccessGreen
                            
                            // Winnings/Bonuses (Yellow/Gold)
                            transaction.type in listOf(
                                TransactionType.TOURNAMENT_WINNING, TransactionType.BONUS_CREDIT,
                                TransactionType.REFUND
                            ) -> WarningYellow
                            
                            // Debits (Red for withdrawals/entries)
                            transaction.type in listOf(
                                TransactionType.WITHDRAWAL, TransactionType.MANUAL_WITHDRAWAL,
                                TransactionType.UPI_WITHDRAWAL, TransactionType.BANK_TRANSFER_WITHDRAWAL,
                                TransactionType.TOURNAMENT_ENTRY, TransactionType.ENTRY_FEE
                            ) -> ErrorRed
                            
                            else -> Color.Gray
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Enhanced status chip
                    Box(
                        modifier = Modifier
                            .background(
                                color = when (transaction.status) {
                                    TransactionStatus.COMPLETED -> SuccessGreen.copy(alpha = 0.2f)
                                    TransactionStatus.PENDING -> WarningYellow.copy(alpha = 0.2f)
                                    TransactionStatus.FAILED -> ErrorRed.copy(alpha = 0.2f)
                                    else -> Color.Gray.copy(alpha = 0.2f)
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = transaction.status.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = when (transaction.status) {
                                TransactionStatus.COMPLETED -> SuccessGreen
                                TransactionStatus.PENDING -> WarningYellow
                                TransactionStatus.FAILED -> ErrorRed
                                else -> Color.Gray
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Expand/Collapse indicator
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Divider(color = Color.Gray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Transaction Details",
                        color = NetWinCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TransactionDetailRow("Type", transaction.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() })
                    TransactionDetailRow("Amount", NGNTransactionUtils.formatAmount(transaction.amount, currency))
                    TransactionDetailRow("Status", transaction.status.name.lowercase().replaceFirstChar { it.uppercase() })
                    TransactionDetailRow("Date", formatTransactionDate(transaction.createdAt))
                }
            }
        }
    }
}

@Composable
private fun TransactionDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatTransactionDate(timestamp: Timestamp?): String {
    if (timestamp == null) return "Unknown"
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}

// New Esports-Style Components

@Composable
private fun EsportsBalanceTab(
    label: String,
    amount: String,
    color: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Log.d("EsportsBalanceTab", "Rendering: label=$label, amount=$amount, isSelected=$isSelected")
    
    Card(
        modifier = modifier
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) DarkCard else DarkSurface
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) BorderStroke(
            1.dp, 
            Brush.horizontalGradient(listOf(NetWinPurple, NetWinPink, NetWinCyan))
        ) else BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = amount,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (isSelected) {
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = NetWinCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun EsportsActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EsportsTransactionItem(
    transaction: Transaction,
    currency: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Transaction Type Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = when (transaction.type) {
                                TransactionType.TOURNAMENT_ENTRY -> NetWinPurple.copy(alpha = 0.2f)
                                TransactionType.TOURNAMENT_WINNING -> WarningYellow.copy(alpha = 0.2f)
                                TransactionType.DEPOSIT, TransactionType.UPI_DEPOSIT -> SuccessGreen.copy(alpha = 0.2f)
                                TransactionType.WITHDRAWAL, TransactionType.UPI_WITHDRAWAL -> ErrorRed.copy(alpha = 0.2f)
                                else -> Color.Gray.copy(alpha = 0.2f)
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (transaction.type) {
                            TransactionType.TOURNAMENT_ENTRY -> Icons.Default.PlayArrow
                            TransactionType.TOURNAMENT_WINNING -> Icons.Default.EmojiEvents
                            TransactionType.DEPOSIT, TransactionType.UPI_DEPOSIT -> Icons.Default.Add
                            TransactionType.WITHDRAWAL, TransactionType.UPI_WITHDRAWAL -> Icons.Default.Remove
                            else -> Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = when (transaction.type) {
                            TransactionType.TOURNAMENT_ENTRY -> NetWinPurple
                            TransactionType.TOURNAMENT_WINNING -> WarningYellow
                            TransactionType.DEPOSIT, TransactionType.UPI_DEPOSIT -> SuccessGreen
                            TransactionType.WITHDRAWAL, TransactionType.UPI_WITHDRAWAL -> ErrorRed
                            else -> Color.Gray
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = transaction.description,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Text(
                        text = formatEsportsDate(transaction.createdAt),
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
                        TransactionType.DEPOSIT, TransactionType.UPI_DEPOSIT, TransactionType.TOURNAMENT_WINNING -> SuccessGreen
                        TransactionType.WITHDRAWAL, TransactionType.UPI_WITHDRAWAL, TransactionType.TOURNAMENT_ENTRY -> ErrorRed
                        else -> Color.Gray
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                
                // Status Badge
                Box(
                    modifier = Modifier
                        .background(
                            color = when (transaction.status) {
                                TransactionStatus.COMPLETED -> SuccessGreen
                                TransactionStatus.PENDING -> WarningYellow
                                TransactionStatus.FAILED -> ErrorRed
                                else -> Color.Gray
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = transaction.status.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatEsportsDate(timestamp: Timestamp?): String {
    if (timestamp == null) return "Unknown"
    val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}