/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.components

import lib.phonograph.misc.ColorPalette
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.os.Build.VERSION_CODES


@Composable
fun PresetColorPicker(
    colors: List<Color>,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    onColorClick: (Int, Color) -> Unit,
) {
    Box(Modifier.fillMaxWidth()) {
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

@RequiresApi(VERSION_CODES.S)
@Composable
fun MonetColorPicker(
    onColorClick: (Int, Int) -> Unit,
) {
    val context = LocalContext.current
    val group = remember { ColorPalette.dynamicColors(context).map { Color(it) } }
    var selectedGroup: Int by remember { mutableStateOf(0) }
    val colors = remember { ColorPalette.allDynamicColors(context) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            PresetColorPicker(colors = group) { index, _ ->
                selectedGroup = index
            }
        }
        Box(
            modifier = Modifier
                .height(2.dp)
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.85f)
                .background(Color.Gray)
        )
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            PresetColorPicker(colors = colors[selectedGroup].map { Color(it) }) { index, _ ->
                val type = 1 shl (selectedGroup + 1)
                val depth = (index + 1) * 100
                onColorClick.invoke(type, depth)
            }
        }
    }
}