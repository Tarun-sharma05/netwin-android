package com.cehpoint.netwin.presentation.components


import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cehpoint.netwin.domain.model.TournamentStatus
import com.cehpoint.netwin.presentation.theme.NetwinTokens

@Composable
fun TournamentStatusBadge(
    status: TournamentStatus,
    modifier: Modifier = Modifier
) {
    val (text, backgroundColor, textColor) = when (status) {
        TournamentStatus.UPCOMING -> Triple(
            "UPCOMING",
            Color(0xFFFFA000).copy(alpha = 0.2f),
            Color(0xFFFFC107)
        )

        TournamentStatus.STARTS_SOON -> Triple(
            "STARTS SOON",
            Color(0xFF2196F3).copy(alpha = 0.2f),
            Color(0xFF2196F3)
        )

        TournamentStatus.ROOM_OPEN -> Triple(
            "ROOM OPEN",
            Color(0xFF4CAF50).copy(alpha = 0.2f),
            Color(0xFF4CAF50)
        )

        TournamentStatus.ONGOING -> Triple(
            "LIVE",
            Color(0xFFF44336).copy(alpha = 0.2f),
            Color(0xFFF44336)
        )

        TournamentStatus.COMPLETED -> Triple(
            "COMPLETED",
            Color(0xFF9E9E9E).copy(alpha = 0.2f),
            Color(0xFF9E9E9E)
        )
    }

    // Only animate if status is LIVE
    val infiniteTransition = rememberInfiniteTransition()
    val scale by if (status == TournamentStatus.ONGOING) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Surface(
        shape = RoundedCornerShape(NetwinTokens.RadiusSm),
        color = backgroundColor,
        modifier = modifier.scale(scale)
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
