/*
 *  Copyright (c) 2023 chr_56
 */

package player.phonograph.ui.compose.components

import player.phonograph.ui.compose.isColorLight
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ColorCircle(color: Color, modifier: Modifier = Modifier, selected: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(1f)

    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize()
                .clickable(onClick = onClick)
                .clip(CircleShape)
                .border(1.dp, if (MaterialTheme.colors.isLight) Color.Gray else Color.White, CircleShape)
                .background(color)
        )
        if (selected)
            Box(modifier = Modifier.align(Alignment.Center)) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = Icons.Default.Check.name,
                    tint = if (color.isColorLight()) Color.Black else Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
    }
}