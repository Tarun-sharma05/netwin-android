package com.cehpoint.netwin.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PullRefreshComponent(
    isRefreshing: Boolean,
    refreshError: String?,
    refreshSuccess: Boolean,
    onRefresh: () -> Unit,
    onClearRefreshSuccess: () -> Unit,
    onClearRefreshError: () -> Unit,
    content: @Composable () -> Unit
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )

    // Animated alpha for success/error feedback
    val feedbackAlpha by animateFloatAsState(
        targetValue = if (refreshSuccess || refreshError != null) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "feedback_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pullRefresh(pullRefreshState)
    ) {
        content()

        // Custom PullRefreshIndicator with better styling
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = Color(0xFF00BCD4),
            contentColor = Color.White,
            scale = true
        )

        // Success/Error feedback overlay
        if (refreshSuccess || refreshError != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(feedbackAlpha)
                    .padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (refreshSuccess) Color(0xFF4CAF50) else Color(0xFFF44336)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (refreshSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = if (refreshSuccess) "Tournaments updated!" else "Refresh failed: $refreshError",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Auto-clear feedback after delay
    LaunchedEffect(refreshSuccess, refreshError) {
        if (refreshSuccess || refreshError != null) {
            delay(2000)
            if (refreshSuccess) {
                onClearRefreshSuccess()
            } else {
                onClearRefreshError()
            }
        }
    }
}

@Composable
fun RefreshIndicator(
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    if (isRefreshing) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = Color(0xFF00BCD4),
                modifier = Modifier.size(24.dp)
            )
        }
    }
} 