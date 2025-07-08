package com.cehpoint.netwin.presentation.screens

import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cehpoint.netwin.data.remote.FirebaseManager
import com.cehpoint.netwin.presentation.navigation.Screen
import com.cehpoint.netwin.presentation.navigation.ScreenRoutes
import com.cehpoint.netwin.presentation.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreenUI(navController: NavController, viewModel: AuthViewModel = hiltViewModel(), firebaseManager: FirebaseManager) {
    var userInput by remember { mutableStateOf("") } // email or phone
    var password by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    val isLoading by viewModel.isLoading.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Helper to detect if input is email or phone
    val isEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(userInput).matches()
    val isPhone = userInput.trim().let { it.isNotEmpty() && it.all { c -> c.isDigit() } && it.length >= 8 }

    // Remove conflicting navigation logic - let NavGraph handle all navigation
    // The NavGraph will automatically handle navigation based on auth state

    LaunchedEffect(error) {
        if (error != null) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(error.orEmpty())
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "NETWIN",
                    color = Color.Cyan,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = userInput,
                    onValueChange = {
                        userInput = it
                        viewModel.clearError()
                        otpSent = false
                        password = ""
                        otp = ""
                    },
                    label = { Text("Email or Phone Number", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Cyan,
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = Color.Cyan
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    enabled = !isLoading
                )

                if (isEmail) {
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        viewModel.clearError()
                    },
                    label = { Text("Password", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Cyan,
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = Color.Cyan
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    enabled = !isLoading
                )
                Button(
                    onClick = {
                            viewModel.signIn(userInput, password)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Cyan
                    ),
                        enabled = !isLoading && userInput.isNotEmpty() && password.isNotEmpty()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text("Sign In", color = Color.Black)
                        }
                    }
                } else if (isPhone) {
                    if (!otpSent) {
                        Button(
                            onClick = {
                                viewModel.sendOtpForLogin(userInput, navController)
                                otpSent = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Cyan
                            ),
                            enabled = !isLoading && userInput.isNotEmpty()
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White
                                )
                            } else {
                                Text("Send OTP", color = Color.Black)
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = otp,
                            onValueChange = {
                                otp = it
                                viewModel.clearError()
                            },
                            label = { Text("OTP", color = Color.White) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.Cyan,
                                unfocusedBorderColor = Color.Gray,
                                cursorColor = Color.Cyan
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            enabled = !isLoading
                        )
                        Button(
                            onClick = {
                                viewModel.verifyOtpForLogin(otp, navController)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Cyan
                            ),
                            enabled = !isLoading && otp.isNotEmpty()
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White
                                )
                            } else {
                                Text("Verify OTP", color = Color.Black)
                            }
                        }
                    }
                }

                TextButton(
                    onClick = { navController.navigate(route = ScreenRoutes.RegisterScreen) },
                    enabled = !isLoading
                ) {
                    Text("Don't have an account? Sign Up", color = Color.Cyan)
                }
            }
        }
    }
} 