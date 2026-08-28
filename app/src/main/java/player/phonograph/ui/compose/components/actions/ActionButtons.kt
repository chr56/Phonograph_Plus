/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.compose.components.actions

import player.phonograph.model.menu.ActionEntry
import player.phonograph.ui.resource.interpreter
import player.phonograph.ui.resource.painter
import player.phonograph.ui.resource.text
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import kotlinx.coroutines.launch


@Composable
fun ActionIconButton(item: ActionEntry, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val semantic = remember(item) { item.semantic.interpreter() }
    ActionIconButton(
        icon = semantic.painter(),
        tint = MaterialTheme.colors.onPrimary,
        modifier = modifier,
        text = semantic.text(),
        onClick = { scope.launch { item.action() } },
    )
}

@Composable
fun ActionIconButton(
    icon: Painter,
    tint: Color,
    modifier: Modifier = Modifier,
    text: String? = null,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            icon,
            contentDescription = text,
            tint = tint,
            modifier = modifier,
        )
    }
}

