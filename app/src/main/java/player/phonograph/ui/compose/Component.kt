/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true)
@Composable
fun TextItem(tag: String = "KeyName", value: String = "KeyValue") {
    Box {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
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

@Preview(showBackground = true)
@Composable
fun FieldItem(tag: String = "KeyName", value: String = "KeyValue") {
    TextField(
        readOnly = true,
        leadingIcon = {
            Text(
                text = tag,
                style = TextStyle(fontWeight = FontWeight.Bold, color = Color.Black),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .defaultMinSize(minWidth = 64.dp),
            )
        },
        value = value,
        placeholder = { Text("-") },
        onValueChange = {},
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = MaterialTheme.colors.background
        ),
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .fillMaxWidth()
    )
}
