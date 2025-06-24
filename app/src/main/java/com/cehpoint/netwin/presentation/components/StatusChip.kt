package com.cehpoint.netwin.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatusChip(status: String, modifier: Modifier = Modifier) {
    val (color, label) = when (status.uppercase()) {
        "VERIFIED" -> Color(0xFF4CAF50) to "Verified"
        "REJECTED" -> Color(0xFFE57373) to "Rejected"
        "PENDING" -> Color(0xFFFFC107) to "Pending"
        else -> Color.Gray to status.capitalize()
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
} 