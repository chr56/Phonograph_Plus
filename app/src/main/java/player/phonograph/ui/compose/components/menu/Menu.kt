/*
 *  Copyright (c) 2023~2026 chr_56
 */

package player.phonograph.ui.compose.components.menu

import player.phonograph.model.menu.ActionEntry
import player.phonograph.ui.compose.components.actions.ActionIconButton
import player.phonograph.ui.resource.ActionSemantics
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier


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
