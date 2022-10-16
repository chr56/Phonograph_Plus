/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose.components

import androidx.compose.runtime.Composable

@Composable
fun ReadonlyTextFieldItem(tag: String = "KeyName", value: String = "KeyValue") {
    BaseTextFieldItem(tag, value, true)
}