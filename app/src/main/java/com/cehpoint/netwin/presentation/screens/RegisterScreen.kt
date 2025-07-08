package com.cehpoint.netwin.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cehpoint.netwin.R
import com.cehpoint.netwin.presentation.navigation.ScreenRoutes
import com.cehpoint.netwin.presentation.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun RegisterScreenUI(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var selectedOption by remember { mutableStateOf<RegistrationOption?>(null) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }
    
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Handle authentication success
    LaunchedEffect(isAuthenticated, currentUser) {
        if (isAuthenticated && currentUser != null) {
            // Navigate to tournaments screen - profile completeness will be checked there
            navController.navigate(ScreenRoutes.TournamentsScreen) {
                popUpTo("register") { inclusive = true }
            }
        }
    }

    // Show error messages
    LaunchedEffect(error) {
        error?.let { errorMsg ->
            coroutineScope.launch {
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "NetWin Logo",
            modifier = Modifier
                .size(120.dp)
                .shadow(16.dp, shape = CircleShape)
        )
        
        Spacer(Modifier.height(32.dp))
        
        Text(
            "Create Your Account",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        )
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            "Choose your preferred registration method",
            color = Color.Gray,
            fontSize = 16.sp
        )
        
        Spacer(Modifier.height(32.dp))

        // Registration Options
        if (selectedOption == null) {
            // Show option selection
            RegistrationOptionCard(
                title = "Email & Password",
                subtitle = "Sign up with email and password",
                icon = Icons.Default.Email,
                onClick = { selectedOption = RegistrationOption.EMAIL_PASSWORD }
            )
            
            Spacer(Modifier.height(16.dp))
            
            RegistrationOptionCard(
                title = "Phone Number",
                subtitle = "Sign up with phone number and OTP",
                icon = Icons.Default.Phone,
                onClick = { selectedOption = RegistrationOption.PHONE_OTP }
            )
        } else {
            // Show selected registration form
            when (selectedOption) {
                RegistrationOption.EMAIL_PASSWORD -> {
                    EmailPasswordRegistration(
                        email = email,
                        password = password,
                        confirmPassword = confirmPassword,
                        termsAccepted = termsAccepted,
                        isLoading = isLoading,
                        onEmailChange = { email = it },
                        onPasswordChange = { password = it },
                        onConfirmPasswordChange = { confirmPassword = it },
                        onTermsAccepted = { termsAccepted = it },
                        onBack = { selectedOption = null },
                        onSubmit = {
                            if (password == confirmPassword && termsAccepted) {
                                viewModel.signUp(email, password)
                            }
                        }
                    )
                }
                RegistrationOption.PHONE_OTP -> {
                    PhoneOtpRegistration(
                        phone = phone,
                        otp = otp,
                        otpSent = otpSent,
                        isLoading = isLoading,
                        onPhoneChange = { phone = it },
                        onOtpChange = { otp = it },
                        onSendOtp = { 
                            viewModel.sendOtpForLogin(phone, navController)
                            otpSent = true
                        },
                        onVerifyOtp = { viewModel.verifyOtpForLogin(otp, navController) },
                        onBack = { selectedOption = null }
                    )
                }
                null -> {
                    // This should never happen since we check for null above
                }
            }

        }
        
        Spacer(Modifier.height(24.dp))
        
        // Login link
        TextButton(
            onClick = { navController.navigate(ScreenRoutes.LoginScreen) }
        ) {
            Text(
                "Already have an account? Sign In",
                color = Color.Cyan
            )
        }
    }
}

enum class RegistrationOption {
    EMAIL_PASSWORD,
    PHONE_OTP
}

@Composable
fun RegistrationOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF00BCD4),
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(Modifier.width(16.dp))
            
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = subtitle,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun EmailPasswordRegistration(
    email: String,
    password: String,
    confirmPassword: String,
    termsAccepted: Boolean,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTermsAccepted: (Boolean) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Sign up with Email",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
                )

        Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = email,
            onValueChange = onEmailChange,
                    label = { Text("Email", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Cyan,
                unfocusedBorderColor = Color.Gray
                    ),
                    enabled = !isLoading
                )
        
        Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
            onValueChange = onPasswordChange,
                    label = { Text("Password", color = Color.White) },
            visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Cyan,
                unfocusedBorderColor = Color.Gray
                    ),
                    enabled = !isLoading
                )
        
        Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
                    label = { Text("Confirm Password", color = Color.White) },
            visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Cyan,
                unfocusedBorderColor = Color.Gray
                    ),
                    enabled = !isLoading
                )

        Spacer(Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = termsAccepted,
                onCheckedChange = onTermsAccepted,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color.Cyan,
                    uncheckedColor = Color.Gray
                )
            )
            Text(
                "I agree to Terms & Privacy Policy",
                color = Color.White,
                fontSize = 14.sp
            )
        }
        
        Spacer(Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onBack,
                enabled = !isLoading
            ) {
                Text("Back", color = Color.White)
            }
            
            Button(
                onClick = onSubmit,
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank() && 
                         password == confirmPassword && termsAccepted,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Text("Sign Up", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PhoneOtpRegistration(
    phone: String,
    otp: String,
    otpSent: Boolean,
    isLoading: Boolean,
    onPhoneChange: (String) -> Unit,
    onOtpChange: (String) -> Unit,
    onSendOtp: () -> Unit,
    onVerifyOtp: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Sign up with Phone",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
        
        Spacer(Modifier.height(24.dp))
        
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Phone Number", color = Color.White) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.Cyan,
                unfocusedBorderColor = Color.Gray
            ),
            enabled = !isLoading && !otpSent
        )
        
        if (!otpSent) {
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = onSendOtp,
                enabled = !isLoading && phone.isNotBlank() && phone.length >= 10,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                    } else {
                    Text("Send OTP", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
        } else {
            Spacer(Modifier.height(16.dp))
            
            OutlinedTextField(
                value = otp,
                onValueChange = onOtpChange,
                label = { Text("Enter OTP", color = Color.White) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.Cyan,
                    unfocusedBorderColor = Color.Gray
                ),
                    enabled = !isLoading
            )
            
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = onVerifyOtp,
                enabled = !isLoading && otp.isNotBlank() && otp.length == 6,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Text("Verify OTP", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        OutlinedButton(
            onClick = onBack,
            enabled = !isLoading
        ) {
            Text("Back", color = Color.White)
        }
    }
} 