package com.cehpoint.netwin.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cehpoint.netwin.presentation.navigation.ScreenRoutes
import com.cehpoint.netwin.presentation.viewmodels.ProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenUI(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var editName by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                title = { Text("Profile", color = Color.White, style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    IconButton(onClick = { navController.navigate(ScreenRoutes.SettingsScreen) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xCC181A20)),
                modifier = Modifier.shadow(4.dp)
            )
        },
        containerColor = Color(0xFF121212),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.Cyan)
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error: $error", color = Color.Red, modifier = Modifier.padding(16.dp))
                }
            }
            user != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF121212))
                        .padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        // Profile Header Card
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF181A20))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Profile Picture with Gradient Border and Camera FAB
                                Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.wrapContentSize()) {
                                    GradientProfilePicture(
                                        imageUrl = user!!.profilePictureUrl,
                                        size = 96.dp,
                                        contentDescription = "Profile picture of ${user!!.displayName}",
                                        onClick = { /* TODO: Show image picker/camera dialog */ }
                                    )
                                    FloatingActionButton(
                                        onClick = { /* TODO: Show image picker/camera dialog */ },
                                        containerColor = Color(0xFF181A20),
                                        contentColor = Color.Cyan,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .semantics { contentDescription = "Change profile picture" }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                // Name (editable)
                                AnimatedNameField(
                                    name = user!!.displayName,
                                    editMode = editName,
                                    newName = newName,
                                    onNameChange = { newName = it },
                                    onEditClick = {
                                        newName = user!!.displayName
                                        editName = true
                                    },
                                    onSave = {
                                        if (newName.isNotBlank()) {
                                            viewModel.updateUserField("displayName", newName)
                                            editName = false
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Profile updated")
                                            }
                                        }
                                    },
                                    onCancel = { editName = false }
                                )
                                // Username
                                if (user!!.username.isNotBlank()) {
                                    Text(
                                        text = "@${user!!.username}",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                // Email
                                Text(
                                    user!!.email ?: "No Email",
                                    color = Color.Gray,
                                    fontSize = 15.sp,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(16.dp))
                                // KYC Status
                                KycStatusSection(
                                    kycStatus = user!!.kycStatus,
                                    onCompleteKyc = { navController.navigate(ScreenRoutes.KycScreen) }
                                )
                            }
                        }
                    }
                    item {
                        // Stats Section
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF181A20))
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Gaming Stats", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(16.dp))
                                StatsGrid(
                                    matches = user!!.matchesPlayed,
                                    wins = user!!.matchesWon,
                                    earnings = user!!.totalEarnings,
                                    tournaments = user!!.tournamentsJoined
                                )
                            }
                        }
                    }
                    item {
                        // Action Buttons
                        Spacer(Modifier.height(16.dp))
                        ActionButtons(
                            isEditing = editName,
                            onEdit = { editName = true },
                            onSave = {
                                if (newName.isNotBlank()) {
                                    viewModel.updateUserField("displayName", newName)
                                    editName = false
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Profile updated")
                                    }
                                }
                            },
                            onAchievements = { /* TODO: Navigate to achievements */ },
                            onCancel = { editName = false }
                        )
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No user data available.", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun GradientProfilePicture(
    imageUrl: String,
    size: Dp,
    contentDescription: String,
    onClick: () -> Unit
) {
    // Sweep gradient border
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Transparent)
            .semantics { this.contentDescription = contentDescription }
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(Color(0xFF00E5FF), Color(0xFFAA00FF), Color(0xFF00E5FF))
                ),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Box(
            modifier = Modifier
                .padding(6.dp)
                .clip(CircleShape)
                .background(Color.DarkGray)
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(size * 0.6f)
                )
            }
        }
    }
}

@Composable
private fun AnimatedNameField(
    name: String,
    editMode: Boolean,
    newName: String,
    onNameChange: (String) -> Unit,
    onEditClick: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    if (editMode) {
        OutlinedTextField(
            value = newName,
            onValueChange = onNameChange,
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(onClick = onSave) { Text("Save Changes") }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                name.ifBlank { "No Name" },
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineMedium
            )
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = "Edit display name", tint = Color.Cyan)
            }
        }
    }
}

@Composable
private fun KycStatusSection(kycStatus: String, onCompleteKyc: () -> Unit) {
    val (color, label) = when (kycStatus.lowercase()) {
        "verified" -> Pair(Color(0x3300C853), "Verified")
        "pending" -> Pair(Color(0x33FFAB00), "Pending")
        "not_submitted" -> Pair(Color(0x33D50000), "Not Submitted")
        "rejected" -> Pair(Color(0x33D50000), "Rejected")
        "in_review" -> Pair(Color(0x332979FF), "In Review")
        else -> Pair(Color(0x33D50000), kycStatus.capitalize())
    }
    val isClickable = kycStatus.lowercase() != "verified"
    Surface(
        color = color,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
            .semantics { contentDescription = "KYC status bar: $label" }
            .then(if (isClickable) Modifier.clickable(onClick = onCompleteKyc) else Modifier)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // KYC Icon
            Icon(
                imageVector = Icons.Filled.VerifiedUser, // Use a shield or ID card icon
                contentDescription = "KYC",
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            // KYC Label
            Text("KYC Status:", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            // Status Chip
            StatusChip(label)
            if (kycStatus.lowercase() != "verified") {
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = onCompleteKyc,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    contentPadding = ButtonDefaults.ContentPadding,
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Complete KYC", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun StatusChip(label: String) {
    Surface(
        color = when (label.lowercase()) {
            "verified" -> Color(0x3300C853)
            "pending" -> Color(0x33FFAB00)
            "not_submitted" -> Color(0x33D50000)
            "rejected" -> Color(0x33D50000)
            "in_review" -> Color(0x332979FF)
            else -> Color(0x33D50000)
        },
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
private fun StatsGrid(matches: Int, wins: Int, earnings: Double, tournaments: Int) {
    val stats = listOf(
        Triple("Matches Played", matches, Icons.Default.Gamepad),
        Triple("Wins", wins, Icons.Default.EmojiEvents),
        Triple("Total Earnings", earnings, Icons.Default.MonetizationOn),
        Triple("Tournaments", tournaments, Icons.Default.CalendarToday)
    )
    // Responsive: 2 columns on phone, 4 on tablet
    val columns = if (LocalDensity.current.run { 600.dp.toPx() } < androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp) 4 else 2
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        stats.forEach { (label, value, icon) ->
            StatCard(label, value.toString(), icon)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF23272F)),
        modifier = Modifier
            .padding(8.dp)
            .width(90.dp)
            .height(90.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF23272F))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Text(label, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ActionButtons(
    isEditing: Boolean,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onAchievements: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = if (isEditing) onSave else onEdit,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Brush.horizontalGradient(
                    listOf(Color(0xFF00E5FF), Color(0xFFAA00FF))
                ).toBrushColor()
            )
        ) {
            Text(if (isEditing) "Save Changes" else "Edit Profile", color = Color.White, fontWeight = FontWeight.Medium)
        }
        OutlinedButton(
            onClick = onAchievements,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            border = BorderStroke(1.dp, Color(0xFF00E5FF)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF))
        ) {
            Text("View Achievements", color = Color(0xFF00E5FF), fontWeight = FontWeight.Medium)
        }
        // Optionally add a Logout button here if desired
    }
}

// Helper to convert Brush to Color for Button background
private fun Brush.toBrushColor(): Color = Color.Unspecified // Compose limitation: use solid color or custom background