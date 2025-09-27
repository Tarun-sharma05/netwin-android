package com.cehpoint.netwin.presentation.components.buttons

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cehpoint.netwin.presentation.theme.NetwinTokens

@Composable
fun GradientPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = NetwinTokens.Primary,
            disabledContainerColor = NetwinTokens.Primary.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(NetwinTokens.RadiusSm)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}
