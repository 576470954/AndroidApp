package com.example.mytestapplication.ui.common

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun VerticalDivider(
    color: Color = Color.LightGray,
    modifier: Modifier = Modifier
) {
    Divider(
        color = color,
        modifier = modifier
            .fillMaxHeight()
            .width(1.dp)
    )
}
