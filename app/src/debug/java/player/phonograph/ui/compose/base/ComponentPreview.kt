/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose.base

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import player.phonograph.ui.compose.base.TailTextField
import player.phonograph.ui.compose.base.VerticalTextItem

@Preview(showBackground = true)
@Composable
fun VerticalTextItemPreview(title: String = "KeyName", value: String = "KeyValue") {
    VerticalTextItem(title, value)
}

@Preview(showBackground = true)
@Composable
fun TailTextFieldPreview(hint: String = "New Value", onValueChange: (String) -> Unit = {}) {
    TailTextField(hint, onValueChange)
}