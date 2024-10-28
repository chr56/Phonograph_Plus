/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.MutableStateFlow

@Preview(showBackground = true)
@Composable
fun VerticalTextFieldItemPreview1() {
    VerticalTextFieldItem(
        title = "VerticalTextFieldItem-Title",
        value = "VerticalTextFieldItem-Value",
        hint = "VerticalTextFieldItem-Hint",
        onTextChanged = {}
    )
}

@Preview(showBackground = true)
@Composable
fun VerticalTextFieldItemPreview2() {
    VerticalTextFieldItem(
        title = "VerticalTextFieldItem-Title",
        value = MutableStateFlow("VerticalTextFieldItem-Value"),
        hint = "VerticalTextFieldItem-Hint",
    )
}