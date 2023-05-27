/*
 *  Copyright (c) 2023 chr_56
 */

package player.phonograph.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

@Composable
fun ColorCircus(colorState: MutableState<Color>) {
    Box {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(0.6f)
                .clip(CircleShape)
                .background(
                    if (MaterialTheme.colors.isLight) Color.Gray else Color.White
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(0.55f)
                .clip(CircleShape)
                .background(colorState.value)
        )
    }
}