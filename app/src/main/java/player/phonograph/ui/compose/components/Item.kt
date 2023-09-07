/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Item(label: String, value: String?) {
    if (!value.isNullOrEmpty()) {
        Item(label) {
            ValueText(value)
        }
    }
}
@Composable
fun Item(label: String, values: Collection<String>?) {
    if (!values.isNullOrEmpty()) {
        Item(label) {
            Column {
                for (value in values) {
                    ValueText(value)
                }
            }
        }
    }
}
@Composable
fun Item(label: String, content: @Composable () -> Unit) {
    LabeledItemLayout(
        Modifier.padding(vertical = 4.dp),
        label = label,
        labelModifier = Modifier.padding(end = 12.dp)
    ) {
        SelectionContainer {
            content()
        }
    }
}
@Composable
private fun ValueText(value: String) {
    Text(
        text = value,
        style = TextStyle(
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.92f),
            fontSize = 14.sp,
        ),
        modifier = Modifier.wrapContentSize()
    )
}