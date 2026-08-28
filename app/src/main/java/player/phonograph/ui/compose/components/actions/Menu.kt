/*
 *  Copyright (c) 2023~2026 chr_56
 */

package player.phonograph.ui.compose.components.actions

import player.phonograph.model.menu.ActionEntry
import player.phonograph.ui.resource.ActionSemantics
import player.phonograph.ui.resource.interpreter
import player.phonograph.ui.resource.painter
import player.phonograph.ui.resource.text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch


@Composable
fun DropDownMenuContent(
    actions: List<ActionEntry>,
    withIcon: Boolean = false,
) {
    Column {
        for (action in actions) {
            MenuItem(item = action, withIcon = withIcon)
        }
    }
}


@Composable
fun CollapsedActionMenu(actions: List<ActionEntry>, withIcon: Boolean, modifier: Modifier = Modifier) {
    var showMenu by remember { mutableStateOf(false) }
    Box(modifier) {
        ActionIconButton(
            ActionEntry.ToolbarEntry(
                semantic = ActionSemantics.More, action = { showMenu = true }
            )
        )
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropDownMenuContent(actions, withIcon = withIcon)
        }
    }
}


@Composable
fun MenuItem(item: ActionEntry, withIcon: Boolean) {
    val scope = rememberCoroutineScope()
    val semantic = remember(item) { item.semantic.interpreter() }
    val onClick: () -> Unit = { scope.launch { item.action() } }
    if (withIcon) {
        IconTextMenuItem(icon = semantic.painter(), text = semantic.text(), onClick = onClick)
    } else {
        TextMenuItem(text = semantic.text(), onClick = onClick)
    }
}

@Composable
private fun TextMenuItem(
    text: String,
    onClick: () -> Unit,
) {
    Box {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        )
    }
}

@Composable
private fun IconTextMenuItem(
    icon: Painter,
    text: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 16.dp)
            .clickable(onClick = onClick)
    ) {
        Icon(
            icon,
            contentDescription = text,
            tint = MaterialTheme.colors.onSurface,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        Text(
            text = text,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .align(Alignment.CenterVertically)
        )
    }
}

