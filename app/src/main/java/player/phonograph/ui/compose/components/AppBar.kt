/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose.components

import player.phonograph.ui.compose.isColorLight
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Composable
internal fun PhonographAppBar(
    title: @Composable () -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    contentColor: Color = defaultContentColor(backgroundColor),
    navigationIcon: @Composable () -> Unit = { DefaultNavigationIcon() },
    actions: @Composable RowScope.() -> Unit = { DefaultActions() },
    elevation: Dp = AppBarDefaults.TopAppBarElevation
) {
    TopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        elevation = elevation
    )
}

@Composable
fun DefaultNavigationIcon(onBackClick: () -> Unit = {}) {
    IconButton(onClick = onBackClick) {
        Icon(Icons.Default.ArrowBack, contentDescription = null)
    }
}

@Composable
fun DefaultActions() {
}

internal fun defaultContentColor(backgroundColor: Color) =
    if (backgroundColor.isColorLight()) Color.Black else Color.White