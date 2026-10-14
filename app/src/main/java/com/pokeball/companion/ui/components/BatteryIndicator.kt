package com.pokeball.companion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pokeball.companion.ui.theme.PokeGreen
import com.pokeball.companion.ui.theme.PokeOrange
import com.pokeball.companion.ui.theme.PokeRed

@Composable
fun BatteryIndicator(
    batteryPercent: Int,
    modifier: Modifier = Modifier
) {
    val (color, text) = when {
        batteryPercent < 0 -> Color(0xFF757575) to "--%"
        batteryPercent > 50 -> PokeGreen to "$batteryPercent%"
        batteryPercent > 20 -> PokeOrange to "$batteryPercent%"
        else -> PokeRed to "$batteryPercent%"
    }

    Row(
        modifier = modifier
            .background(Color(0xFF2C2F33), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(5.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "🔋 $text",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
