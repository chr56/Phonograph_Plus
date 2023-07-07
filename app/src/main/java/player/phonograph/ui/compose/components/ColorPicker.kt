/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


@Composable
fun PresetColorPicker(
    colors: List<Color>,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    onColorClick: (Int, Color) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 48.dp),
            contentPadding = contentPadding,
        ) {
            items(colors.size) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) {
                    ColorCircle(
                        color = colors[index],
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxSize(0.85f),
                    ) { onColorClick.invoke(index, colors[index]) }
                }
            }
        }
    }
}
