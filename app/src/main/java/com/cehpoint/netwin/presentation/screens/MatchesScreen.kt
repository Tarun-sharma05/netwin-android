package com.cehpoint.netwin.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.cehpoint.netwin.presentation.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreenUI(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Matches") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // TODO: Implement matches list
            Text("Matches Screen")
        }
    }
} 