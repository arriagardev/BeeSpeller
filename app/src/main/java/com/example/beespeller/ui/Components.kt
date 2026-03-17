package com.example.beespeller.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MasteryStars(level: Int) {
    Row {
        repeat(5) { index ->
            Icon(
                imageVector = if (index < level) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = if (index < level) Color(0xFFFFD700) else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
