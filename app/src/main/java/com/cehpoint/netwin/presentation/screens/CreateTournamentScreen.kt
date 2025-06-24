package com.cehpoint.netwin.presentation.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import java.time.LocalDate
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTournamentScreen(navController: NavController) {
    var tournamentName by remember { mutableStateOf("") }
    var gameType by remember { mutableStateOf("") }
    var prizePool by remember { mutableStateOf("") }
    var entryFee by remember { mutableStateOf("") }
    var maxTeams by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusDays(7)) }
    var description by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedImageUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Tournament") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tournament Name
            OutlinedTextField(
                value = tournamentName,
                onValueChange = { tournamentName = it },
                label = { Text("Tournament Name") },
                modifier = Modifier.fillMaxWidth()
            )

            // Game Type
            OutlinedTextField(
                value = gameType,
                onValueChange = { gameType = it },
                label = { Text("Game Type") },
                modifier = Modifier.fillMaxWidth()
            )

            // Prize Pool
            OutlinedTextField(
                value = prizePool,
                onValueChange = { prizePool = it },
                label = { Text("Prize Pool (₹)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Entry Fee
            OutlinedTextField(
                value = entryFee,
                onValueChange = { entryFee = it },
                label = { Text("Entry Fee (₹)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Max Teams
            OutlinedTextField(
                value = maxTeams,
                onValueChange = { maxTeams = it },
                label = { Text("Maximum Teams") },
                modifier = Modifier.fillMaxWidth()
            )

            // Start Date
            OutlinedTextField(
                value = startDate.toString(),
                onValueChange = { },
                label = { Text("Start Date") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true
            )

            // End Date
            OutlinedTextField(
                value = endDate.toString(),
                onValueChange = { },
                label = { Text("End Date") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            // Image Picker Button
            Button(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Pick Tournament Image")
            }

            // Image Preview
            selectedImageUri?.let { uri ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = "Tournament Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Create Button
            Button(
                onClick = {
                    // TODO: Upload image to Firebase Storage and get download URL
                    // TODO: Implement tournament creation with imageUrl
                    navController.navigateUp()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Tournament")
            }
        }
    }
} 