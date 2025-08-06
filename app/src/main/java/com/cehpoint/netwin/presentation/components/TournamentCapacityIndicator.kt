package com.cehpoint.netwin.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Define colors for the progress bar and text based on fullness, suitable for a dark theme
// Your project uses Color(0xFF121212) as a base and Color(0xFF1E1E1E) for cards.
// These colors should offer good contrast.

val ProgressGreen = Color(0xFF4CAF50)       // Standard Green (good visibility on dark)
val ProgressOrange = Color(0xFFFF9800)      // Standard Orange (good visibility on dark)
val ProgressRed = Color(0xFFF44336)         // Standard Red (good visibility on dark)

// Text colors for dark theme
val TextColorDefaultDark = Color.White.copy(alpha = 0.87f) // Primary text on dark
val TextColorMutedDark = Color.White.copy(alpha = 0.60f)  // Secondary/muted text on dark
val TextColorStatusHighlightDark = Color.White // For "Full" or "Filling Fast" when not using red

@Composable
fun TournamentCapacityIndicator(
    modifier: Modifier = Modifier, // Allow passing modifiers from the caller
    registeredTeams: Int,
    maxTeams: Int
) {
    // Gracefully handle invalid maxTeams (e.g., 0 or negative) to prevent division by zero
    // and to show a sensible default or placeholder.
    if (maxTeams <= 0) {
        Text(
            text = "Capacity: N/A",
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp), // Using labelMedium for consistency
            color = TextColorMutedDark,
            modifier = modifier.padding(vertical = 4.dp) // Apply padding if this is the only element
        )
        return
    }

    // Calculate progress and percentage, memoizing results for efficiency
    val progress = remember(registeredTeams, maxTeams) {
        (registeredTeams.toFloat() / maxTeams.toFloat()).coerceIn(0f, 1f)
    }
    val percentage = remember(progress) { (progress * 100).toInt() }

    // Determine progress bar color based on percentage
    val progressColor = remember(percentage) {
        when {
            percentage > 90 -> ProgressRed   // Red for >90%
            percentage >= 70 -> ProgressOrange // Orange for 70-90%
            else -> ProgressGreen          // Green for <70%
        }
    }

    // Determine status text based on percentage
    val statusText = remember(percentage, registeredTeams, maxTeams) {
        when {
            percentage >= 100 -> "Full"
            percentage > 70 -> "Filling Fast" // As per requirement: "Filling Fast" for >70% (which includes the >90% case if not "Full")
            else -> "$registeredTeams/$maxTeams Players"
        }
    }

    // Determine status text color. "Full" should be red. "Filling Fast" can be orange or white for emphasis.
    val statusTextColor = remember(percentage) {
        when {
            percentage >= 100 -> ProgressRed // "Full" is red
            percentage > 70 -> TextColorStatusHighlightDark // "Filling Fast" can be highlighted
            else -> TextColorDefaultDark
        }
    }

    val statusFontWeight = remember(percentage) {
        if (percentage > 70) FontWeight.Bold else FontWeight.Normal
    }

    Column(modifier = modifier) { // Apply the passed modifier to the Column
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Capacity:",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp), // Consistent small text
                color = TextColorMutedDark,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp), // Consistent small text
                color = statusTextColor,
                fontWeight = statusFontWeight // Bold if "Full" or "Filling Fast"
            )
        }
        Spacer(modifier = Modifier.height(4.dp)) // Small space between text and progress bar
        LinearProgressIndicator(
            progress = { progress }, // Material 3 progress lambda
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp), // Slightly thicker for better visibility
            color = progressColor, // Dynamic color based on fullness
            trackColor = Color.DarkGray.copy(alpha = 0.3f) // Track color visible on Color(0xFF1E1E1E)
        )
    }
}