/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.components

import player.phonograph.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp


@Composable
fun ColorPalettePicker(
    groupColors: List<Color>,
    allColors: List<List<Color>>,
    selected: Color?,
    onSelected: (Int, Int, Color) -> Unit,
) {
    var currentGroupIndex by rememberSaveable { mutableIntStateOf(-1) }
    if (currentGroupIndex == -1) {
        ColorPaletteGrid(
            groupColors,
            selected = null,
            withBackArrow = false,
            onColorSelected = { index, _ -> currentGroupIndex = index },
            onBackPressed = {}
        )
    } else {
        ColorPaletteGrid(
            allColors[currentGroupIndex],
            selected = selected,
            withBackArrow = true,
            onColorSelected = { index, color -> onSelected(currentGroupIndex, index, color) },
            onBackPressed = { currentGroupIndex = -1 }
        )
    }
}


@Composable
private fun ColorPaletteGrid(
    colors: List<Color>,
    selected: Color?,
    withBackArrow: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    onColorSelected: (index: Int, Color) -> Unit,
    onBackPressed: () -> Unit,
) {
    LazyVerticalGrid(
        modifier = modifier.fillMaxWidth(),
        columns = GridCells.Fixed(4),
        contentPadding = contentPadding,
    ) {
        if (withBackArrow) item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_exit),
                    modifier = Modifier
                        .clickable(onClick = onBackPressed)
                        .align(Alignment.Center)
                )
            }
        }
        items(colors.size) { index ->
            val color = colors[index]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                ColorCircle(
                    color = color,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxSize(0.85f),
                    selected = color == selected,
                ) { onColorSelected.invoke(index, color) }
            }
        }
    }
}
