package com.cehpoint.netwin.presentation.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.cehpoint.netwin.data.model.PaymentMethod
import com.cehpoint.netwin.data.model.PaymentProof
import com.cehpoint.netwin.data.model.PaymentVerificationStatus
import com.cehpoint.netwin.ui.theme.*

/**
 * Enhanced Payment Proof Screen for comprehensive verification
 * Supports both NGN (Nigerian) and INR (Indian) payment methods
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedPaymentProofScreen(
    currency: String,
    amount: Double,
    paymentMethod: PaymentMethod,
    upiAppPackage: String? = null,
    onSubmitProof: (PaymentProof) -> Unit,
    onDismiss: () -> Unit
) {
    var currentStep by rememberSaveable { mutableStateOf(1) }
    var paymentProof by rememberSaveable { mutableStateOf(PaymentProof(currency = currency, amount = amount, paymentMethod = paymentMethod, upiAppName = upiAppPackage)) }
    var validationErrors by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    
    val isNigerian = currency == "NGN"
    val totalSteps = if (isNigerian) 4 else 3
    
    // Log screen initialization
    Log.d("EnhancedPaymentProofScreen", "Screen initialized - Currency: $currency, Amount: $amount, Step: $currentStep")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Header with progress
        PaymentProofHeader(
            currentStep = currentStep,
            totalSteps = totalSteps,
            currency = currency,
            amount = amount,
            onDismiss = onDismiss
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Validation errors display
        if (validationErrors.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Please fix the following:",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    validationErrors.forEach { error ->
                        Text(
                            text = "• $error",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Screenshot instruction card
        if (currentStep == 1) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A5F)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Need to take screenshot?",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isNigerian) {
                                "If you haven't taken a screenshot yet, go back to your Paystack/bank app and capture the payment confirmation screen."
                            } else {
                                "If you haven't taken a screenshot yet, go back to your UPI app and capture the payment confirmation screen."
                            },
                            color = Color(0xFFB0BEC5),
                            fontSize = 14.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Step content
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                when (currentStep) {
                    1 -> PaymentDetailsStep(
                        paymentProof = paymentProof,
                        isNigerian = isNigerian,
                        onUpdate = { paymentProof = it }
                    )
                    2 -> DocumentUploadStep(
                        paymentProof = paymentProof,
                        isNigerian = isNigerian,
                        onUpdate = { paymentProof = it }
                    )
                    3 -> if (isNigerian) {
                        BankDetailsStep(
                            paymentProof = paymentProof,
                            onUpdate = { paymentProof = it }
                        )
                    } else {
                        ReviewAndSubmitStep(
                            paymentProof = paymentProof,
                            validationErrors = validationErrors
                        )
                    }
                    4 -> ReviewAndSubmitStep(
                        paymentProof = paymentProof,
                        validationErrors = validationErrors
                    )
                }
            }
        }
        
        // Navigation buttons
        PaymentProofNavigation(
            currentStep = currentStep,
            totalSteps = totalSteps,
            paymentProof = paymentProof,
            isNigerian = isNigerian,
            onNext = { 
                val errors = validateCurrentStep(currentStep, paymentProof, isNigerian)
                Log.d("EnhancedPaymentProofScreen", "Next clicked - Step: $currentStep, Total Steps: $totalSteps")
                Log.d("EnhancedPaymentProofScreen", "Is Nigerian: $isNigerian")
                Log.d("EnhancedPaymentProofScreen", "UPI Transaction ID: '${paymentProof.upiTransactionId}' (length: ${paymentProof.upiTransactionId?.length ?: 0})")
                Log.d("EnhancedPaymentProofScreen", "Bank Name: '${paymentProof.bankName}' (length: ${paymentProof.bankName?.length ?: 0})")
                Log.d("EnhancedPaymentProofScreen", "UPI App: '${paymentProof.upiAppName}' (length: ${paymentProof.upiAppName?.length ?: 0})")
                Log.d("EnhancedPaymentProofScreen", "Account Number: '${paymentProof.accountNumber}' (length: ${paymentProof.accountNumber?.length ?: 0})")
                Log.d("EnhancedPaymentProofScreen", "Validation Errors: $errors")
                
                if (errors.isEmpty()) {
                    Log.d("EnhancedPaymentProofScreen", "Validation passed, moving to next step")
                    if (currentStep < totalSteps) {
                        currentStep++
                        Log.d("EnhancedPaymentProofScreen", "Current step incremented to: $currentStep")
                    } else {
                        Log.d("EnhancedPaymentProofScreen", "Submitting proof")
                        onSubmitProof(paymentProof)
                    }
                } else {
                    Log.d("EnhancedPaymentProofScreen", "Validation failed: $errors")
                    validationErrors = errors
                }
            },
            onPrevious = { 
                if (currentStep > 1) currentStep-- 
            },
            onSubmit = { onSubmitProof(paymentProof) }
        )
    }
}

@Composable
private fun PaymentProofHeader(
    currentStep: Int,
    totalSteps: Int,
    currency: String,
    amount: Double,
    onDismiss: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Payment Verification",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Amount: ${if (currency == "NGN") "₦" else "₹"}${String.format("%.2f", amount)}",
            color = NetWinCyan,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress indicator
        LinearProgressIndicator(
            progress = currentStep.toFloat() / totalSteps.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = NetWinCyan,
            trackColor = Color.Gray.copy(alpha = 0.3f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Step $currentStep of $totalSteps",
            color = Color.Gray,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun PaymentDetailsStep(
    paymentProof: PaymentProof,
    isNigerian: Boolean,
    onUpdate: (PaymentProof) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            StepHeader(
                icon = Icons.Default.Payment,
                title = "Payment Details",
                subtitle = if (isNigerian) "Enter your Paystack/Bank transaction details" else "Enter details from your payment confirmation"
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (isNigerian) {
                // Nigerian payment fields
                EnhancedTextField(
                    value = paymentProof.paystackReference ?: "",
                    onValueChange = { onUpdate(paymentProof.copy(paystackReference = it)) },
                    label = "Paystack Reference Number",
                    placeholder = "e.g., T123456789",
                    icon = Icons.Default.Receipt
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                EnhancedTextField(
                    value = paymentProof.paystackEmail ?: "",
                    onValueChange = { onUpdate(paymentProof.copy(paystackEmail = it)) },
                    label = "Email Used for Payment",
                    placeholder = "email@example.com",
                    icon = Icons.Default.Email
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                EnhancedTextField(
                    value = paymentProof.bankTransactionId ?: "",
                    onValueChange = { onUpdate(paymentProof.copy(bankTransactionId = it)) },
                    label = "Bank Transaction ID (if available)",
                    placeholder = "Optional",
                    icon = Icons.Default.AccountBalance
                )
            } else {
                // Indian UPI fields
                EnhancedTextField(
                    value = paymentProof.upiTransactionId ?: "",
                    onValueChange = { onUpdate(paymentProof.copy(upiTransactionId = it)) },
                    label = "UPI Transaction ID",
                    placeholder = "e.g., 123456789012",
                    icon = Icons.Default.Receipt
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                EnhancedTextField(
                    value = paymentProof.upiReferenceNumber ?: "",
                    onValueChange = { onUpdate(paymentProof.copy(upiReferenceNumber = it)) },
                    label = "UPI Reference Number (UTR) - Optional",
                    placeholder = "If available in your payment confirmation",
                    icon = Icons.Default.Numbers
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                EnhancedTextField(
                    value = paymentProof.senderUpiId ?: "",
                    onValueChange = { onUpdate(paymentProof.copy(senderUpiId = it)) },
                    label = "Your UPI ID - Optional",
                    placeholder = "If shown in payment details",
                    icon = Icons.Default.AccountCircle
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                EnhancedTextField(
                    value = paymentProof.bankName ?: "",
                    onValueChange = { onUpdate(paymentProof.copy(bankName = it)) },
                    label = "Bank Name",
                    placeholder = "e.g., HDFC Bank, SBI, ICICI",
                    icon = Icons.Default.AccountBalance
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                EnhancedTextField(
                    value = paymentProof.accountNumber ?: "",
                    onValueChange = { onUpdate(paymentProof.copy(accountNumber = it)) },
                    label = "Account Last 4 Digits - Optional",
                    placeholder = "e.g., 1234",
                    icon = Icons.Default.Numbers
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // UPI App selection
                UpiAppSelector(
                    selectedApp = paymentProof.upiAppName ?: "",
                    onAppSelected = { onUpdate(paymentProof.copy(upiAppName = it)) }
                )
            }
        }
    }
}

@Composable
private fun DocumentUploadStep(
    paymentProof: PaymentProof,
    isNigerian: Boolean,
    onUpdate: (PaymentProof) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Function to relaunch UPI app for screenshot
    val relaunchUpiApp = {
        val upiAppName = paymentProof.upiAppName
        if (!upiAppName.isNullOrBlank()) {
            try {
                Log.d("EnhancedPaymentProofScreen", "Attempting to relaunch UPI app: $upiAppName")
                
                // Try to launch the UPI app by package name
                val packageManager = context.packageManager
                val intent = packageManager.getLaunchIntentForPackage(upiAppName)
                
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    Log.d("EnhancedPaymentProofScreen", "Successfully launched UPI app: $upiAppName")
                } else {
                    // Fallback: Try to open UPI apps with common package names
                    val commonUpiApps = listOf(
                        "com.google.android.apps.nbu.paisa.user", // Google Pay
                        "com.phonepe.app", // PhonePe
                        "net.one97.paytm", // Paytm
                        "in.amazon.mShop.android.shopping", // Amazon Pay (in Amazon app)
                        "com.csam.icici.bank.imobile", // iMobile (ICICI)
                        "com.sbi.upi", // SBI Pay
                        "com.whatsapp", // WhatsApp (has UPI)
                        "in.org.npci.upiapp" // BHIM UPI
                    )
                    
                    var launched = false
                    for (packageName in commonUpiApps) {
                        val fallbackIntent = packageManager.getLaunchIntentForPackage(packageName)
                        if (fallbackIntent != null) {
                            fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(fallbackIntent)
                            Log.d("EnhancedPaymentProofScreen", "Launched fallback UPI app: $packageName")
                            launched = true
                            break
                        }
                    }
                    
                    if (!launched) {
                        Log.e("EnhancedPaymentProofScreen", "No UPI app found for relaunch")
                        // Show error message to user
                        Toast.makeText(context, "No UPI app found. Please open your UPI app manually.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("EnhancedPaymentProofScreen", "Error launching UPI app", e)
                Toast.makeText(context, "Unable to open UPI app. Please check if the app is installed.", Toast.LENGTH_LONG).show()
            }
        } else {
            Log.w("EnhancedPaymentProofScreen", "No UPI app name stored, cannot relaunch")
            Toast.makeText(context, "Please complete a UPI payment first to enable quick relaunch.", Toast.LENGTH_SHORT).show()
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            StepHeader(
                icon = Icons.Default.Upload,
                title = "Upload Documents",
                subtitle = "Provide proof of your payment transaction"
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Screenshot upload (required)
            DocumentUploadCard(
                title = "Payment Screenshot",
                subtitle = if (isNigerian) "Screenshot of Paystack confirmation" else "Screenshot of UPI transaction",
                documentUrl = paymentProof.screenshotUrl,
                onDocumentSelected = { uri -> onUpdate(paymentProof.copy(screenshotUrl = uri)) },
                isRequired = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // UPI App Relaunch (for Indian users)
            if (!isNigerian) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A5F)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF3AFFDC))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color(0xFF3AFFDC),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Need to take screenshot?",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Quickly reopen your UPI app",
                            color = Color(0xFFB0BEC5),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { relaunchUpiApp() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3AFFDC),
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reopen UPI App", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Receipt upload (optional but recommended)
            DocumentUploadCard(
                title = if (isNigerian) "Bank Receipt" else "UPI Receipt",
                subtitle = if (isNigerian) "Bank transaction receipt (if available)" else "UPI app receipt/confirmation",
                documentUrl = paymentProof.receiptUrl,
                onDocumentSelected = { uri -> onUpdate(paymentProof.copy(receiptUrl = uri)) },
                isRequired = false
            )
            
            if (isNigerian) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Bank statement (for large amounts)
                DocumentUploadCard(
                    title = "Bank Statement",
                    subtitle = "Bank statement showing the transaction (for amounts > ₦50,000)",
                    documentUrl = paymentProof.bankStatementUrl,
                    onDocumentSelected = { uri -> onUpdate(paymentProof.copy(bankStatementUrl = uri)) },
                    isRequired = paymentProof.amount > 50000
                )
            }
        }
    }
}

@Composable
private fun BankDetailsStep(
    paymentProof: PaymentProof,
    onUpdate: (PaymentProof) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            StepHeader(
                icon = Icons.Default.AccountBalance,
                title = "Bank Information",
                subtitle = "Provide your bank account details for verification"
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            EnhancedTextField(
                value = paymentProof.bankName ?: "",
                onValueChange = { onUpdate(paymentProof.copy(bankName = it)) },
                label = "Bank Name",
                placeholder = "e.g., First Bank of Nigeria",
                icon = Icons.Default.AccountBalance
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            EnhancedTextField(
                value = paymentProof.accountNumber ?: "",
                onValueChange = { onUpdate(paymentProof.copy(accountNumber = it)) },
                label = "Account Number",
                placeholder = "10-digit account number",
                icon = Icons.Default.Numbers
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            EnhancedTextField(
                value = paymentProof.accountName ?: "",
                onValueChange = { onUpdate(paymentProof.copy(accountName = it)) },
                label = "Account Holder Name",
                placeholder = "Name as it appears on bank account",
                icon = Icons.Default.Person
            )
        }
    }
}

@Composable
private fun ReviewAndSubmitStep(
    paymentProof: PaymentProof,
    validationErrors: List<String>
) {
    Column {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                StepHeader(
                    icon = Icons.Default.CheckCircle,
                    title = "Review & Submit",
                    subtitle = "Please review your information before submitting"
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Payment summary
                PaymentSummaryCard(paymentProof)
            }
        }
        
        if (validationErrors.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Please fix the following issues:",
                            color = ErrorRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    validationErrors.forEach { error ->
                        Text(
                            "• $error",
                            color = ErrorRed,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepHeader(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(NetWinCyan.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = NetWinCyan,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun EnhancedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    isRequired: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { 
            Text(
                "$label${if (isRequired) " *" else ""}",
                color = if (isRequired) Color.White else Color.Gray
            )
        },
        placeholder = { Text(placeholder, color = Color.Gray) },
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = NetWinCyan)
        },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NetWinCyan,
            unfocusedBorderColor = Color.Gray,
            cursorColor = NetWinCyan,
            focusedLabelColor = NetWinCyan,
            unfocusedLabelColor = Color.Gray,
            focusedContainerColor = DarkSurface,
            unfocusedContainerColor = DarkSurface,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

@Composable
private fun UpiAppSelector(
    selectedApp: String,
    onAppSelected: (String) -> Unit
) {
    val upiApps = listOf("GPay", "PhonePe", "Paytm", "BHIM", "Amazon Pay", "Other")
    
    Column {
        Text(
            "UPI App Used *",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(
            modifier = Modifier.height(200.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(upiApps) { app ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAppSelected(app) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedApp == app) NetWinCyan.copy(alpha = 0.2f) else DarkSurface
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedApp == app,
                            onClick = { onAppSelected(app) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = NetWinCyan,
                                unselectedColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            app,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentUploadCard(
    title: String,
    subtitle: String,
    documentUrl: String?,
    onDocumentSelected: (String) -> Unit,
    isRequired: Boolean
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { onDocumentSelected(it.toString()) }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { launcher.launch("image/*") },
        colors = CardDefaults.cardColors(
            containerColor = if (documentUrl != null) SuccessGreen.copy(alpha = 0.1f) else DarkSurface
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "$title${if (isRequired) " *" else ""}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        subtitle,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                
                Icon(
                    if (documentUrl != null) Icons.Default.CheckCircle else Icons.Default.Upload,
                    contentDescription = null,
                    tint = if (documentUrl != null) SuccessGreen else NetWinCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            if (documentUrl != null) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Image(
                    painter = rememberAsyncImagePainter(documentUrl),
                    contentDescription = "Uploaded document",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun PaymentSummaryCard(paymentProof: PaymentProof) {
    Column {
        Text(
            "Payment Information",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (paymentProof.currency == "NGN") {
            SummaryRow("Paystack Reference", paymentProof.paystackReference ?: "Not provided")
            SummaryRow("Email", paymentProof.paystackEmail ?: "Not provided")
            if (!paymentProof.bankTransactionId.isNullOrBlank()) {
                SummaryRow("Bank Transaction ID", paymentProof.bankTransactionId!!)
            }
        } else {
            SummaryRow("UPI Transaction ID", paymentProof.upiTransactionId ?: "Not provided")
            SummaryRow("UPI Reference", paymentProof.upiReferenceNumber ?: "Not provided")
            SummaryRow("UPI ID", paymentProof.senderUpiId ?: "Not provided")
            SummaryRow("UPI App", paymentProof.upiAppName ?: "Not provided")
        }
        
        Divider(color = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))
        
        Text(
            "Documents",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        SummaryRow("Screenshot", if (paymentProof.screenshotUrl != null) "✓ Uploaded" else "✗ Missing")
        SummaryRow("Receipt", if (paymentProof.receiptUrl != null) "✓ Uploaded" else "Not provided")
        
        if (paymentProof.currency == "NGN") {
            SummaryRow("Bank Statement", if (paymentProof.bankStatementUrl != null) "✓ Uploaded" else "Not provided")
        }
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
        Text(
            label,
            color = Color.Gray,
            fontSize = 14.sp
        )
        Text(
            value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PaymentProofNavigation(
    currentStep: Int,
    totalSteps: Int,
    paymentProof: PaymentProof,
    isNigerian: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSubmit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (currentStep > 1) {
            OutlinedButton(
                onClick = onPrevious,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color.Gray)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Previous")
            }
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }
        
        Button(
            onClick = if (currentStep == totalSteps) onSubmit else onNext,
            colors = ButtonDefaults.buttonColors(containerColor = NetWinCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                if (currentStep == totalSteps) "Submit Proof" else "Next",
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
            if (currentStep < totalSteps) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Black)
            }
        }
    }
}

private fun validateCurrentStep(step: Int, paymentProof: PaymentProof, isNigerian: Boolean): List<String> {
    val errors = mutableListOf<String>()
    Log.d("EnhancedPaymentProofScreen", "Validating step $step for ${if (isNigerian) "Nigerian" else "Indian"} payment")
    
    when (step) {
        1 -> {
            if (isNigerian) {
                if (paymentProof.paystackReference.isNullOrBlank()) {
                    errors.add("Paystack reference number is required")
                }
                if (paymentProof.paystackEmail.isNullOrBlank()) {
                    errors.add("Email used for payment is required")
                }
            } else {
                // For UPI payments, only require the Transaction ID since that's what users actually have
                val upiTxnId = paymentProof.upiTransactionId?.trim()
                Log.d("EnhancedPaymentProofScreen", "Validating UPI Transaction ID: '$upiTxnId'")
                
                if (upiTxnId.isNullOrBlank()) {
                    errors.add("UPI Transaction ID is required")
                } else if (upiTxnId.length < 3) {
                    errors.add("UPI Transaction ID appears too short (minimum 3 characters)")
                }
                // All other UPI fields are optional since not all UPI apps show them
                Log.d("EnhancedPaymentProofScreen", "UPI Transaction ID validation completed. Errors: ${if (errors.isEmpty()) "None" else errors.joinToString()}")
            }
        }
        2 -> {
            if (paymentProof.screenshotUrl.isNullOrBlank()) {
                errors.add("Payment screenshot is required")
            }
            if (isNigerian && paymentProof.amount > 50000 && paymentProof.bankStatementUrl.isNullOrBlank()) {
                errors.add("Bank statement is required for amounts above ₦50,000")
            }
        }
        3 -> {
            if (isNigerian) {
                if (paymentProof.bankName.isNullOrBlank()) {
                    errors.add("Bank name is required")
                }
                if (paymentProof.accountNumber.isNullOrBlank()) {
                    errors.add("Account number is required")
                }
                if (paymentProof.accountName.isNullOrBlank()) {
                    errors.add("Account holder name is required")
                }
            }
        }
    }
    
    return errors
}
