/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun DialogContent(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints {
        Column(
            modifier = modifier
                .wrapContentSize()
                .verticalScroll(rememberScrollState()),
            content = content
        )
    }
}