package com.cehpoint.netwin.presentation.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.cehpoint.netwin.R
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import com.cehpoint.netwin.presentation.viewmodels.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.cehpoint.netwin.presentation.viewmodels.AuthViewModel

@Composable
fun ProfileSetupScreenUI(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onComplete: (() -> Unit)? = null
) {
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var profilePicUri by remember { mutableStateOf<Uri?>(null) }
    var displayNameError by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val profileSetupState by viewModel.profileSetupState.collectAsState()

    // --- Email/password for phone-auth users ---
    val currentUser = FirebaseAuth.getInstance().currentUser
    val isPhoneAuthOnly = currentUser?.providerData?.any { it.providerId == "phone" } == true && currentUser.email.isNullOrBlank()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var linkLoading by remember { mutableStateOf(false) }
    var linkError by remember { mutableStateOf<String?>(null) }
    var linkSuccess by remember { mutableStateOf(false) }

    // Username uniqueness check
    LaunchedEffect(username) {
        if (username.isNotBlank()) {
            viewModel.checkUsernameAvailability(username)
        }
    }

    // Show error snackbar
    LaunchedEffect(profileSetupState.error) {
        profileSetupState.error?.let { errorMsg ->
            coroutineScope.launch { snackbarHostState.showSnackbar(errorMsg) }
        }
    }
    LaunchedEffect(linkError) {
        linkError?.let { errorMsg ->
            coroutineScope.launch { snackbarHostState.showSnackbar(errorMsg) }
        }
    }
    LaunchedEffect(linkSuccess) {
        if (linkSuccess) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Email/password linked!") }
        }
    }

    // On success, let NavGraph handle navigation
    LaunchedEffect(profileSetupState.saveSuccess) {
        if (profileSetupState.saveSuccess) {
            // Profile is now complete, NavGraph will automatically navigate to home
            // No need to manually navigate here
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Complete Your Profile",
            color = Color.Cyan,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Profile Picture
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.DarkGray)
                .clickable {
                    // TODO: Launch image picker
                },
            contentAlignment = Alignment.Center
        ) {
            if (profilePicUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(profilePicUri),
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Default Profile Picture",
                    modifier = Modifier.size(60.dp)
                )
            }
        }
        TextButton(onClick = { /* TODO: Launch image picker */ }) {
            Text("Choose Profile Picture", color = Color.Cyan)
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Username
        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                // Uniqueness check is handled by LaunchedEffect
            },
            label = { Text("Username", color = Color.White) },
            isError = profileSetupState.isUsernameAvailable == false,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !profileSetupState.isSaving
        )
        if (profileSetupState.isUsernameAvailable == false) {
            Text("Username taken", color = Color.Red, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Display Name
        OutlinedTextField(
            value = displayName,
            onValueChange = {
                displayName = it
                displayNameError = null
            },
            label = { Text("Display Name", color = Color.White) },
            isError = displayNameError != null,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !profileSetupState.isSaving
        )
        if (displayNameError != null) {
            Text(displayNameError!!, color = Color.Red, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))

        // --- Email/password for phone-auth users ---
        if (isPhoneAuthOnly) {
            Text("Add Email & Password (optional, for account recovery)", color = Color.White, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; linkError = null },
                label = { Text("Email", color = Color.White) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !linkLoading
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; linkError = null },
                label = { Text("Password", color = Color.White) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !linkLoading,
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    linkLoading = true
                    authViewModel.linkEmailPasswordToPhoneUser(email, password) { success, errorMsg ->
                        linkLoading = false
                        linkSuccess = success
                        linkError = errorMsg
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !linkLoading && email.isNotBlank() && password.isNotBlank()
            ) {
                if (linkLoading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Link Email & Password", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Button(
            onClick = {
                if (username.isBlank()) {
                    coroutineScope.launch { snackbarHostState.showSnackbar("Username required") }
                } else if (profileSetupState.isUsernameAvailable == false) {
                    coroutineScope.launch { snackbarHostState.showSnackbar("Username is already taken") }
                } else if (displayName.isBlank()) {
                    displayNameError = "Display name required"
                } else {
                    // Save profile without country - country will be handled in KYC screen
                    viewModel.saveProfile(username, displayName, "", profilePicUri?.toString())
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan),
            modifier = Modifier.fillMaxWidth(),
            enabled = !profileSetupState.isSaving && profileSetupState.isUsernameAvailable != false
        ) {
            if (profileSetupState.isSaving) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Complete Profile", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        SnackbarHost(hostState = snackbarHostState)
    }
} 