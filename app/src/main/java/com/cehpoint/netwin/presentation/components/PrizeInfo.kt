package com.cehpoint.netwin.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.cehpoint.netwin.presentation.theme.NetwinTokens

@Composable
fun PrizeInfo(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    Box(
        modifier = modifier
            .padding(horizontal = 2.dp, vertical = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .defaultMinSize(minWidth = 90.dp)
                .sizeIn(minWidth = 90.dp, minHeight = 70.dp)
        ) {
            // Icon with background
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Value text
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            // Label text - single line with proper truncation
            if (showLabel) {
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 2.dp, top = 1.dp, end = 2.dp, bottom = 0.dp)
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
fun PrizeInfoPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Single line examples
                Text("Single Line Examples", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrizeInfo(
                        label = "Prize Pool",
                        value = "₹50,000",
                        icon = Icons.Default.EmojiEvents,
                        color = Color(0xFFFFC107) // Gold
                    )
                    PrizeInfo(
                        label = "Entry Fee",
                        value = "Free",
                        icon = Icons.Default.AccountBalanceWallet,
                        color = Color(0xFF4CAF50) // Green
                    )
                    PrizeInfo(
                        label = "Per Kill",
                        value = "₹50",
                        icon = Icons.Default.Star,
                        color = Color(0xFF26C6DA) // Teal
                    )
                }

                // Long text handling
                Text("Long Text Handling", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrizeInfo(
                        label = "Tournament Prize Pool",
                        value = "₹1,50,000",
                        icon = Icons.Default.EmojiEvents,
                        color = Color(0xFFFFC107)
                    )
                    PrizeInfo(
                        label = "Registration Fee",
                        value = "₹199",
                        icon = Icons.Default.AccountBalanceWallet,
                        color = Color(0xFF4CAF50)
                    )
                }

                // Without label example
                Text("Without Label", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrizeInfo(
                        label = "",
                        value = "Solo",
                        icon = Icons.Default.Person,
                        color = Color(0xFF9C27B0),
                        showLabel = false
                    )
                    PrizeInfo(
                        label = "",
                        value = "Duo",
                        icon = Icons.Default.People,
                        color = Color(0xFF3F51B5),
                        showLabel = false
                    )
                }
            }
        }
    }
}
