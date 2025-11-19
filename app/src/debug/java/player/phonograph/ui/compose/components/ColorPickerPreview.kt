/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun ColorPickerPreview() {
    Box(
        Modifier.Companion
            .padding(96.dp)
            .fillMaxSize()
    ) {
        ColorPicker(Color(10, 103, 172, 255))
    }
}