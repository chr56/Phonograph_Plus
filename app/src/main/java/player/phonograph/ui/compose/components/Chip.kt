/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun Chip(text: String, modifier: Modifier = Modifier) {
    Chip(modifier) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
        )
    }
}

@Composable
fun Chip(modifier: Modifier = Modifier, color: Color = Color.LightGray, content: @Composable () -> Unit) {
    SelectionContainer {
        Surface(
            modifier = modifier
                .padding(4.dp)
                .wrapContentSize(),
            shape = RoundedCornerShape(16.dp),
            color = color
        ) {
            content()
        }
    }
}