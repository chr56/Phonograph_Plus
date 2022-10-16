/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import player.phonograph.R

@Composable
fun BaseListEntry(
    image: @Composable () -> Unit,
    content: @Composable () -> Unit,
    showMenu: Boolean,
    onMenuClick: () -> Unit,
) {
    Column {
        Row {
            Box(Modifier
                .width(40.dp)
                .fillMaxHeight()) { image() }
            Box(Modifier.fillMaxHeight()) { content() }
            if (showMenu) {
                Button(onClick = onMenuClick, modifier = Modifier
                    .size(48.dp)
                    .padding(8.dp)
                    .fillMaxHeight()) {
                    Icon(painter = painterResource(id = R.drawable.ic_more_vert_white_24dp),
                        contentDescription = "Menu",
                        tint = MaterialTheme.colors.onSurface)
                }
            }
        }
        // todo: separator line
    }
}