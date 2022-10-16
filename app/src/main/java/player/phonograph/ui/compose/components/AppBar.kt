/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import player.phonograph.ui.compose.isColorLight

/**
 *
 */
@Composable
internal fun PhonographAppBar(
    title: @Composable () -> Unit,
    backgroundColor: Color,
    backClick: (() -> Unit) = { /* Empty*/ },
    actions: @Composable (RowScope.() -> Unit) = { /* Empty*/ },
) {
    AppBar(
        title = title,
        actions = actions,
        backClick = backClick,
        backgroundColor = backgroundColor,
        textColor = if (backgroundColor.isColorLight()) Color.Black else Color.White
    )
}

@Composable
internal fun AppBar(
    title: @Composable () -> Unit,
    backgroundColor: Color,
    textColor: Color,
    backClick: (() -> Unit) = { /* Empty*/ },
    actions: @Composable (RowScope.() -> Unit) = { /* Empty*/ },
) {
    TopAppBar(
        title = title,
        navigationIcon = {
            IconButton(onClick = backClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }
        },
        actions = actions,
        backgroundColor = backgroundColor,
        contentColor = textColor
    )
}