package com.cehpoint.netwin.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object NetwinTokens {
    // Colors
    val Background = Color(0xFF121212)
    val Surface = Color(0xFF1A1B26)
    val SurfaceAlt = Color(0xFF1E1E2F)
    val Primary = Color(0xFF6C3AFF) // neon purple
    val Accent = Color(0xFFFF3A8C)  // pink (exact web pink)
    val Tertiary = Color(0xFF3AFFDC) // cyan, use sparingly for small accents
    val TextPrimary = Color(0xFFEFEFFF)
    val TextSecondary = Color(0xFFB0B3C3)
    val Divider = Color(0xFF2A2C39)
    
    // Gradients - NetWin Brand Gradient
    val PrimaryGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF6C3AFF), // Purple
            Color(0xFFFF3A8C), // Pink
            Color(0xFF3AFFDC)  // Cyan
        )
    )
    
    val PrimaryGradientHorizontal = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF6C3AFF), // Purple
            Color(0xFFFF3A8C), // Pink
            Color(0xFF3AFFDC)  // Cyan
        )
    )
    
    // Status Colors (Web App Aligned)
    val StatusUpcoming = Color(0xFFFFC107) // Yellow
    val StatusLive = Color(0xFFF44336)     // Red
    val StatusCompleted = Color(0xFF4CAF50) // Green
    val SuccessGreen = Color(0xFF4CAF50)
    val ErrorRed = Color(0xFFF44336)
    val WarningYellow = Color(0xFFFFC107)


    // Spacing
    val Sp4 = 4.dp
    val Sp6 = 6.dp
    val Sp8 = 8.dp
    val Sp10 = 10.dp
    val Sp12 = 12.dp
    val Sp14 = 14.dp
    val Sp16 = 16.dp
    val Sp20 = 20.dp
    val Sp24 = 24.dp

    // Radii
    val RadiusSm = 8.dp
    val RadiusMd = 12.dp
    val RadiusLg = 14.dp
}
