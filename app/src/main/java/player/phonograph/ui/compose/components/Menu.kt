/*
 *  Copyright (c) 2023 chr_56
 */

package player.phonograph.ui.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DropDownMenuContent(
    list: List<Pair<String, Function0<Unit>>>,
) {
    Column {
        for ((str, block) in list) {
            MenuItem(text = str, onClick = block)
        }
    }
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit) {
    Box {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        )
    }
}

