/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.compose.components.input

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun VerticalTextFieldItemPreview1() {
    VerticalTextFieldItem(
        title = "VerticalTextFieldItem-Title",
        state = rememberTextFieldState(initialText = "VerticalTextFieldItem-Value"),
        hint = "VerticalTextFieldItem-Hint",
    )
}

@Preview(showBackground = true)
@Composable
fun VerticalTextFieldItemPreview2() {
    VerticalTextFieldItem(
        title = "VerticalTextFieldItem-Title",
        state = rememberTextFieldState(initialText = "VerticalTextFieldItem-Value"),
        hint = "VerticalTextFieldItem-Hint",
        allowReset = true,
    )
}