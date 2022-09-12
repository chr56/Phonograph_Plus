/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose.base

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import player.phonograph.R

// @Composable
// fun HorizontalTextItem(tag: String = "KeyName", value: String = "KeyValue") {
//    Box {
//        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
//            Text(
//                text = tag,
//                style = TextStyle(fontWeight = FontWeight.Bold),
//                modifier = Modifier
//                    .padding(end = 8.dp)
//                    .align(Alignment.Top)
//                    .defaultMinSize(minWidth = 64.dp),
//            )
//            SelectionContainer(modifier = Modifier.align(Alignment.Top)) {
//                Text(text = value, modifier = Modifier.align(Alignment.Top))
//            }
//        }
//    }
// }

@Composable
fun VerticalTextItem(title: String, value: String) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalArrangement = Arrangement.SpaceEvenly) {
        // title
        Text(
            text = title,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            ),
            modifier = Modifier
                .align(Alignment.Start),
        )
        // content
        SelectionContainer() {
            Text(
                text = value,
                style = TextStyle(
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.92f),
                    fontSize = 14.sp,
                ),
                modifier = Modifier.align(Alignment.Start)
            )
        }
    }
}

@Composable
fun Title(
    title: String,
    color: Color = MaterialTheme.colors.onSurface,
    horizontalPadding: Dp = 8.dp,
) {
    Text(
        title,
        style = TextStyle(fontWeight = FontWeight.Bold, color = color),
        modifier = Modifier.padding(horizontal = horizontalPadding)
    )
}

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
// @Composable
// fun TextFieldItem(tag: String = "KeyName", value: String = "KeyValue") {
//    BaseTextFieldItem(tag, value, false)
// }
// @Composable
// fun ReadonlyTextFieldItem(tag: String = "KeyName", value: String = "KeyValue") {
//    BaseTextFieldItem(tag, value, true)
// }
//

@Composable
fun TailTextField(hint: String, onValueChange: (String) -> Unit = {}) {
    var value by remember { mutableStateOf("") }
    TextField(
        value = value,
        placeholder = {
            Text(text = hint)
        },
        onValueChange = {
            value = it
            onValueChange(it)
        },
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = MaterialTheme.colors.background,
            textColor = MaterialTheme.colors.onSurface,
            focusedIndicatorColor = MaterialTheme.colors.onSurface.copy(alpha = TextFieldDefaults.IconOpacity),
            unfocusedIndicatorColor = Color.Transparent,

            ),
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .fillMaxWidth()
    )
}
//
// @Composable
// private fun BaseTextFieldItem(tag: String = "KeyName", value: String = "KeyValue", readonly: Boolean = false) {
//    TextField(
//        readOnly = readonly,
//        leadingIcon = {
//            Text(
//                text = tag,
//                style = TextStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colors.onSurface),
//                modifier = Modifier
//                    .padding(horizontal = 8.dp)
//                    .defaultMinSize(minWidth = 64.dp),
//            )
//        },
//        value = value,
//        placeholder = { Text("-") },
//        onValueChange = {},
//        colors = TextFieldDefaults.textFieldColors(
//            backgroundColor = MaterialTheme.colors.background,
//            textColor = MaterialTheme.colors.onSurface,
//            focusedIndicatorColor = MaterialTheme.colors.onSurface.copy(alpha = TextFieldDefaults.IconOpacity),
//            unfocusedIndicatorColor = Color.Transparent,
//
//        ),
//        modifier = Modifier
//            .padding(horizontal = 8.dp, vertical = 8.dp)
//            .fillMaxWidth()
//    )
// }
