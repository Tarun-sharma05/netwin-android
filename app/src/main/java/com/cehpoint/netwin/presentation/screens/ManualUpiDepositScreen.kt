package com.cehpoint.netwin.presentation.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cehpoint.netwin.presentation.components.*
import com.cehpoint.netwin.presentation.viewmodels.AuthViewModel
import com.cehpoint.netwin.presentation.viewmodels.WalletViewModel
import com.cehpoint.netwin.presentation.theme.NetwinTokens
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ManualUpiDepositScreen(
    onNavigateBack: () -> Unit,
    walletViewModel: WalletViewModel = hiltViewModel()
) {
    val uiState by walletViewModel.uiState.collectAsState()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var localScreenshotUri by remember { mutableStateOf<Uri?>(null) }
    
    // Debug logging for UPI state
    LaunchedEffect(uiState.upiSettings, uiState.isLoadingUpiSettings) {
        Log.d("ManualUpiDepositScreen", "=== UPI STATE DEBUG ===")
        Log.d("ManualUpiDepositScreen", "Loading: ${uiState.isLoadingUpiSettings}")
        Log.d("ManualUpiDepositScreen", "UPI Settings: ${uiState.upiSettings}")
        uiState.upiSettings?.let { settings ->
            Log.d("ManualUpiDepositScreen", "UPI ID: '${settings.upiId}'")
            Log.d("ManualUpiDepositScreen", "Display Name: '${settings.displayName}'")
            Log.d("ManualUpiDepositScreen", "Is Active: ${settings.isActive}")
            Log.d("ManualUpiDepositScreen", "QR Enabled: ${settings.qrCodeEnabled}")
        } ?: Log.d("ManualUpiDepositScreen", "UPI Settings is NULL")
        Log.d("ManualUpiDepositScreen", "======================")
    }
    
    val screenShotLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        localScreenshotUri = uri
        walletViewModel.updatePaymentScreenshot(uri, userId)
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header Card
            DepositHeaderCard()
        }
        
        item {
            // UPI ID Display with Copy Button - Dynamic from admin_config
            if (uiState.isLoadingUpiSettings) {
                Log.d("ManualUpiDepositScreen", "Showing LoadingUpiCard")
                LoadingUpiCard()
            } else {
                val upiSettings = uiState.upiSettings
                Log.d("ManualUpiDepositScreen", "UPI Settings check - Settings: $upiSettings, Active: ${upiSettings?.isActive}")
                if (upiSettings?.isActive == true) {
                    Log.d("ManualUpiDepositScreen", "Showing UpiIdCard with ID: ${upiSettings.upiId}")
                    UpiIdCard(
                        upiId = upiSettings.upiId,
                        displayName = upiSettings.displayName
                    )
                } else {
                    Log.d("ManualUpiDepositScreen", "Showing UpiUnavailableCard - Reason: ${if (upiSettings == null) "Settings is null" else "isActive = ${upiSettings.isActive}"}")
                    UpiUnavailableCard()
                }
            }
        }
        
        item {
            // QR Code Generation - Dynamic from admin_config
            val upiSettings = uiState.upiSettings
            if (upiSettings?.isActive == true) {
                QrCodeCard(
                    amount = uiState.selectedAmount,
                    upiId = upiSettings.upiId,
                    displayName = upiSettings.displayName
                )
            }
        }
        
        item {
            // Quick Amount Selection - exact amounts from web app
            QuickAmountSelection(
                amounts = listOf(100.0, 200.0, 500.0, 1000.0, 2000.0),
                selectedAmount = uiState.selectedAmount,
                onAmountSelected = walletViewModel::updateSelectedAmount
            )
        }
        
        item {
            // Custom Amount Input - with ₹10 minimum like web app
            CustomAmountInput(
                amount = uiState.customAmount,
                onAmountChange = walletViewModel::updateCustomAmount,
                minAmount = 10.0
            )
        }
        
        item {
            // 12-digit UPI Transaction ID Input - exact validation from web app
            UpiTransactionIdInput(
                transactionId = uiState.upiTransactionId,
                onTransactionIdChange = walletViewModel::updateUpiTransactionId,
                isError = uiState.upiTransactionIdError != null,
                errorMessage = uiState.upiTransactionIdError
            )
        }
        
        item {
            // Payment Screenshot Upload - NEW
            PaymentScreenshotUploadCard(
                screenshotUri = localScreenshotUri,
                isUploading = uiState.isSubmittingDeposit,
                onUploadClick = { 
                    screenShotLauncher.launch("image/*")
                },
                onScreenshotPicked = { uri ->
                    localScreenshotUri = uri
                    walletViewModel.updatePaymentScreenshot(uri, userId)
                }
            )
        }
        
        item {
            // Submit Button - matches web app behavior
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SubmitDepositButton(
                    enabled = uiState.canSubmitDeposit,
                    isLoading = uiState.isSubmittingDeposit,
                    onClick = walletViewModel::submitManualDeposit
                )
                
                // Show helper text if submit is disabled
                if (!uiState.canSubmitDeposit && !uiState.isSubmittingDeposit) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when {
                            !uiState.upiTransactionId.matches(Regex("^[0-9]{12}$")) && uiState.paymentScreenshot.isNullOrBlank() -> 
                                "Please enter UPI Transaction ID and upload screenshot"
                            !uiState.upiTransactionId.matches(Regex("^[0-9]{12}$")) -> 
                                "Please enter a valid 12-digit UPI Transaction ID"
                            uiState.paymentScreenshot.isNullOrBlank() -> 
                                "Please upload payment screenshot to continue"
                            else -> ""
                        },
                        color = NetwinTokens.ErrorRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
        
        item {
            // Instructions Card - user guidance
            InstructionsCard()
        }
    }
}

@Composable
fun PaymentScreenshotUploadCard(
    screenshotUri: Uri?,
    isUploading: Boolean,
    onUploadClick: () -> Unit,
    onScreenshotPicked: (Uri?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.align(Alignment.Start),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Payment Screenshot ",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "*",
                    color = Color.Red,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Upload a screenshot of your UPI payment for verification",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onUploadClick,
                enabled = !isUploading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (screenshotUri == null) NetwinTokens.Accent else Color(0xFF3A3A3A)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (screenshotUri == null) Icons.Default.Upload else Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isUploading) {
                        "Uploading..."
                    } else if (screenshotUri == null) {
                        "Upload Screenshot"
                    } else {
                        "Change Screenshot"
                    },
                    color = if (screenshotUri == null) Color.Black else Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Screenshot Preview
            screenshotUri?.let { uri ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3A3A3A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        Image(
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = "Screenshot Preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Remove button
                        TextButton(
                            onClick = { 
                                onScreenshotPicked(null)
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Remove Screenshot", color = Color.Red)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingUpiCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = NetwinTokens.Accent,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Loading payment details...",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun UpiUnavailableCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2D1B1B)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⚠️ UPI Payments Unavailable",
                color = NetwinTokens.ErrorRed,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "UPI deposits are temporarily disabled. Please try again later or contact support.",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Preview
@Composable
fun ManualUpiDepositScreenPreview() {
    // This preview will use a mock/fake ViewModel if one is provided for previews,
    // or the actual hiltViewModel if the project is configured for it in previews.
    ManualUpiDepositScreen(onNavigateBack = {})
}