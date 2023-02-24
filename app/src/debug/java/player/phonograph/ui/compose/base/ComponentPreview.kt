/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose.base

import player.phonograph.ui.compose.components.VerticalTextFieldItem
import player.phonograph.ui.compose.components.VerticalTextItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun VerticalTextItemPreview(title: String = "KeyName", value: String = "KeyValue") {
    VerticalTextItem(title, value)
}

@Preview(showBackground = true)
@Composable
fun VerticalTextFieldItemPreview(title: String = "KeyName", hint: String = "KeyValue") {
    VerticalTextFieldItem(title, hint, hint, {})
}