package com.cehpoint.netwin.presentation.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.Rule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Support
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cehpoint.netwin.domain.model.User
import com.cehpoint.netwin.presentation.navigation.ScreenRoutes
import com.cehpoint.netwin.presentation.viewmodels.MoreViewModel
import com.cehpoint.netwin.presentation.theme.NetwinTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreenUI(navController: NavController, viewModel: MoreViewModel = hiltViewModel()) {
    val user by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NetwinTokens.Accent)
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = error ?: "Unknown error",
                        color = Color.Red
                    )
                }
            }
            user != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Profile Section
                    ProfileSection(
                        user = user!!,
                        onEditProfile = { navController.navigate(ScreenRoutes.ProfileScreen) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Quick Stats
                    QuickStatsSection(user!!)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Account & Settings
                    SectionTitle("Account & Settings")
                    MoreItem(
                        icon = Icons.Outlined.Person,
                        title = "Edit Profile",
                        onClick = { navController.navigate(ScreenRoutes.ProfileScreen) }
                    )
                    MoreItem(
                        icon = Icons.Outlined.PrivacyTip,
                        title = "KYC Verification",
                        onClick = { navController.navigate(ScreenRoutes.KycScreen) }
                    )
                    MoreItem(
                        icon = Icons.Outlined.Settings,
                        title = "Account Settings",
                        onClick = { /* TODO: Navigate to account settings */ }
                    )
                    MoreItem(
                        icon = Icons.Outlined.Notifications,
                        title = "Notification Preferences",
                        onClick = { /* TODO: Navigate to notification settings */ }
                    )
                    MoreItem(
                        icon = Icons.Outlined.Language,
                        title = "Language",
                        onClick = { /* TODO: Navigate to language settings */ }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Tournament Management
                    SectionTitle("Tournament Management")
                    MoreItem(
                        icon = Icons.Outlined.EmojiEvents,
                        title = "My Tournaments",
                        onClick = { /* TODO: Navigate to my tournaments */ }
                    )
                    MoreItem(
                        icon = Icons.Outlined.History,
                        title = "Tournament History",
                        onClick = { /* TODO: Navigate to tournament history */ }
                    )
                    MoreItem(
                        icon = Icons.Outlined.Bookmark,
                        title = "Saved Tournaments",
                        onClick = { /* TODO: Navigate to saved tournaments */ }
                    )
                    MoreItem(
                        icon = Icons.AutoMirrored.Outlined.Rule,
                        title = "Tournament Rules",
                        onClick = { /* TODO: Navigate to tournament rules */ }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Financial Section
                    SectionTitle("Financial")
                    MoreItem(
                        icon = Icons.Outlined.AccountBalanceWallet,
                        title = "Transaction History",
                        onClick = { /* TODO: Navigate to transaction history */ }
                    )
                    MoreItem(
                        icon = Icons.Outlined.Payment,
                        title = "Payment Methods",
                        onClick = { /* TODO: Navigate to payment methods */ }
                    )
                    MoreItem(
                        icon = Icons.Outlined.AccountBalance,
                        title = "Bank Details",
                        onClick = { /* TODO: Navigate to bank details */ }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Support & Help
                    SectionTitle("Support & Help")
                    MoreItem(
                        icon = Icons.AutoMirrored.Outlined.Help,
                        title = "FAQ",
                        onClick = { /* TODO: Navigate to FAQ */ }
                    )
                    MoreItem(
                        icon = Icons.Outlined.Support,
                        title = "Contact Support",
                        onClick = { /* TODO: Navigate to support */ }
                    )
                    MoreItem(
                        icon = Icons.Outlined.Report,
                        title = "Report an Issue",
                        onClick = { /* TODO: Navigate to report issue */ }
                    )
                    MoreItem(
                        icon = Icons.Outlined.Description,
                        title = "Terms & Conditions",
                        onClick = { /* TODO: Navigate to terms */ }
                    )
                    MoreItem(
                        icon = Icons.Outlined.PrivacyTip,
                        title = "Privacy Policy",
                        onClick = { /* TODO: Navigate to privacy policy */ }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Social & Community
                    SectionTitle("Social & Community")
                    MoreItem(
                        icon = Icons.Outlined.PersonAdd,
                        title = "Invite Friends",
                        onClick = {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Join me on NetWin! Download the app now.")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                        }
                    )
                    MoreItem(
                        icon = Icons.Outlined.Share,
                        title = "Share App",
                        onClick = {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Check out NetWin - The Ultimate Gaming Tournament Platform!")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                        }
                    )
                    MoreItem(
                        icon = Icons.Outlined.Forum,
                        title = "Join Discord",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/netwin"))
                            context.startActivity(intent)
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // App Information
                    SectionTitle("App Information")
                    MoreItem(
                        icon = Icons.Outlined.Info,
                        title = "About Us",
                        onClick = { /* TODO: Navigate to about us */ }
                    )
                    MoreItem(
                        icon = Icons.Outlined.Star,
                        title = "Rate App",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("market://details?id=${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    )
                    MoreItem(
                        icon = Icons.AutoMirrored.Outlined.Logout,
                        title = "Logout",
                        onClick = { viewModel.logout() }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Version 1.0.0",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileSection(
    user: User,
    onEditProfile: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = onEditProfile),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Picture
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // User Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.username,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun QuickStatsSection(user: User) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(
            icon = Icons.Outlined.EmojiEvents,
            value = user.tournamentsJoined.toString(),
            label = "Tournaments"
        )
        StatItem(
            icon = Icons.Outlined.Star,
            value = user.matchesWon.toString(),
            label = "Wins"
        )
        StatItem(
            icon = Icons.Outlined.AccountBalanceWallet,
            value = "₹${user.totalEarnings}",
            label = "Earnings"
        )
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NetwinTokens.Accent,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun MoreItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
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
                tint = NetwinTokens.Accent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}