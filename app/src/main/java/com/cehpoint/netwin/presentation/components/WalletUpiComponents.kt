package com.cehpoint.netwin.presentation.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cehpoint.netwin.utils.UpiQrGenerator
import androidx.compose.foundation.text.KeyboardOptions
import com.cehpoint.netwin.presentation.theme.NetwinTokens

/**
 * Manual UPI Deposit UI Components
 * Matching web app design and functionality
 * Now using centralized NetwinTokens for theme consistency
 */

@Composable
fun DepositHeaderCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E2F)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Gradient icon background
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(NetwinTokens.PrimaryGradientHorizontal),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = "Wallet",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Add Money via UPI",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Send money to our UPI ID and submit the transaction details",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun UpiIdCard(upiId: String, displayName: String) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E2F)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pay to UPI ID:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = NetwinTokens.Accent
                )
                
                OutlinedButton(
                    onClick = {
                        copyToClipboard(context, upiId, "UPI ID copied to clipboard")
                    },
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NetwinTokens.Accent
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = NetwinTokens.PrimaryGradientHorizontal
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = upiId,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = displayName,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun QrCodeCard(amount: Double, upiId: String, displayName: String) {
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(amount) {
        if (amount > 0) {
            try {
                qrBitmap = UpiQrGenerator.generateUpiQrCode(
                    upiId = upiId,
                    displayName = displayName,
                    amount = amount,
                    description = "Add money to wallet"
                )
            } catch (e: Exception) {
                qrBitmap = null
            }
        } else {
            try {
                qrBitmap = UpiQrGenerator.generateUpiQrCodeWithoutAmount(
                    upiId = upiId,
                    displayName = displayName
                )
            } catch (e: Exception) {
                qrBitmap = null
            }
        }
    }
    
    if (qrBitmap != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E2F)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (amount > 0) "Scan QR Code to Pay ₹$amount" else "Scan QR Code to Pay",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // QR Code with white background
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(12.dp)
                ) {
                    Image(
                        bitmap = qrBitmap!!.asImageBitmap(),
                        contentDescription = "UPI QR Code",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Scan with any UPI app",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun QuickAmountSelection(
    amounts: List<Double>,
    selectedAmount: Double,
    onAmountSelected: (Double) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E2F)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Quick Select Amount",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                amounts.forEach { amount ->
                    val isSelected = selectedAmount == amount
                    
                    OutlinedButton(
                        onClick = { onAmountSelected(amount) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) NetwinTokens.Accent.copy(alpha = 0.2f) else Color.Transparent,
                            contentColor = if (isSelected) NetwinTokens.Accent else Color.Gray
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = if (isSelected) {
                                NetwinTokens.PrimaryGradientHorizontal
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(Color.Gray, Color.Gray)
                                )
                            }
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "₹${amount.toInt()}",
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Tap any amount to generate a QR code",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun CustomAmountInput(
    amount: String,
    onAmountChange: (String) -> Unit,
    minAmount: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E2F)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Or Enter Custom Amount",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = amount,
                onValueChange = { newValue ->
                    // Only allow numbers
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        onAmountChange(newValue)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter amount", color = Color.Gray) },
                leadingIcon = {
                    Text(
                        text = "₹",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = NetwinTokens.Accent
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NetwinTokens.Accent,
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = NetwinTokens.Accent
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Minimum amount is ₹${minAmount.toInt()}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun UpiTransactionIdInput(
    transactionId: String,
    onTransactionIdChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E2F)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "UPI Transaction ID",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = transactionId,
                onValueChange = { newValue ->
                    // Only allow numbers, max 12 digits
                    if (newValue.length <= 12 && (newValue.isEmpty() || newValue.all { it.isDigit() })) {
                        onTransactionIdChange(newValue)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter 12-digit transaction ID", color = Color.Gray) },
                isError = isError,
                supportingText = {
                    if (isError && errorMessage != null) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = "Enter exactly 12 digits from your UPI payment confirmation",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                },
                trailingIcon = {
                    if (transactionId.length == 12 && !isError) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Valid",
                            tint = Color.Green
                        )
                    } else if (isError) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else NetwinTokens.Accent,
                    unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else Color.Gray,
                    cursorColor = NetwinTokens.Accent
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

@Composable
fun SubmitDepositButton(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(vertical = 8.dp),
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (enabled && !isLoading) {
                        NetwinTokens.PrimaryGradientHorizontal
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(Color.Gray, Color.Gray)
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Submit Deposit Request",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun InstructionsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A3A5C).copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = NetwinTokens.Accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "How to Add Money",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InstructionStep(
                number = "1",
                text = "Scan the QR code or use the UPI ID above"
            )
            
            InstructionStep(
                number = "2",
                text = "Complete the payment in your UPI app"
            )
            
            InstructionStep(
                number = "3",
                text = "Note down the 12-digit transaction ID"
            )
            
            InstructionStep(
                number = "4",
                text = "Enter the transaction ID and submit"
            )
            
            InstructionStep(
                number = "5",
                text = "Admin will verify and credit your wallet (5-30 minutes)"
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.Yellow.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color.Yellow,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Please ensure the transaction ID is correct. Wrong information may delay verification.",
                    fontSize = 12.sp,
                    color = Color.White,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun InstructionStep(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NetwinTokens.PrimaryGradientHorizontal),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.9f),
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

// Helper function to copy text to clipboard
private fun copyToClipboard(context: Context, text: String, message: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("UPI ID", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
