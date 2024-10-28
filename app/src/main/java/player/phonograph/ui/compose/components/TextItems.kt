/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@Composable
fun TextItem(label: String, value: String?) {
    if (!value.isNullOrEmpty()) {
        TextItem(label) {
            ValueText(value)
        }
    }
}

@Composable
fun TextItem(label: String, values: Collection<String>?) {
    if (!values.isNullOrEmpty()) {
        TextItem(label) {
            Column {
                for (value in values) {
                    ValueText(value)
                }
            }
        }
    }
}

@Composable
fun TextItem(label: String, content: @Composable () -> Unit) {
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
        style = MaterialTheme.typography.body2,
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.92f),
        modifier = Modifier.wrapContentSize()
    )
}


@Composable
fun HorizontalTextItem(
    label: String,
    modifier: Modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
    content: @Composable RowScope.() -> Unit,
) {
    Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .weight(2f)
                .align(Alignment.CenterVertically)
        )
        Box(Modifier.weight(9f)) {
            this@Row.content()
        }
    }
}

@Composable
fun HorizontalTextItem(label: String, value: String) {
    HorizontalTextItem(
        label = label,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        SelectionContainer(modifier = Modifier.align(Alignment.Top)) {
            Text(
                text = value,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.align(Alignment.Top),
            )
        }
    }
}

@Composable
fun VerticalTextItem(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // label
        Text(
            text = label,
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start),
        )
        // content
        Box {
            this@Column.content()
        }
    }
}

@Composable
fun VerticalTextItem(label: String, value: String) {
    VerticalTextItem(
        label = label,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        SelectionContainer {
            Text(
                text = value,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.92f),
                modifier = Modifier.align(Alignment.Start)
            )
        }
    }
}