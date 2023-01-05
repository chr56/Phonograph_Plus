/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import player.phonograph.ui.compose.components.VerticalTextItem
import androidx.compose.runtime.Composable

@Composable
internal fun TagItem(tag: String, value: String?, hideIfEmpty: Boolean = false) {
    if (hideIfEmpty) {
        if (!value.isNullOrEmpty()) {
            Item(tag, value)
        }
    } else {
        Item(tag, value ?: "-")
    }
}

@Composable
internal fun Item(tag: String = "KeyName", value: String = "KeyValue") {
    VerticalTextItem(tag, value)
}