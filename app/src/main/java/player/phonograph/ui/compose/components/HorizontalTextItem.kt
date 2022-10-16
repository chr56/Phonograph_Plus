/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HorizontalTextItem(tag: String = "KeyName", value: String = "KeyValue") {
    Box {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Text(
                text = tag,
                style = TextStyle(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .align(Alignment.Top)
                    .defaultMinSize(minWidth = 64.dp),
            )
            SelectionContainer(modifier = Modifier.align(Alignment.Top)) {
                Text(text = value, modifier = Modifier.align(Alignment.Top))
            }
        }
    }
}