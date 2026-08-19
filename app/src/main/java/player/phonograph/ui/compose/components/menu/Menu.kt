/*
 *  Copyright (c) 2023~2026 chr_56
 */

package player.phonograph.ui.compose.components.menu

import player.phonograph.R
import player.phonograph.ui.compose.components.actions.ActionIconButton
import player.phonograph.ui.compose.components.actions.ActionItem
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.DropdownMenu
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun DropDownMenuContent(
    list: List<Pair<String, Function0<Unit>>>,
) {
    Column {
        for ((str, block) in list) {
            TextMenuItem(text = str, onClick = block)
        }
    }
}


@Composable
fun DropDownMenuContent(
    actions: List<ActionItem>,
    withIcon: Boolean = false,
) {
    Column {
        for (action in actions) {
            MenuItem(item = action, withIcon = withIcon)
        }
    }
}


@Composable
fun CollapsedActionMenu(actions: List<ActionItem>, withIcon: Boolean, modifier: Modifier = Modifier) {
    var showMenu by remember { mutableStateOf(false) }
    Box(modifier) {
        ActionIconButton(
            ActionItem(imageVector = Icons.Default.MoreVert, textRes = R.string.action_more) { showMenu = true }
        )
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropDownMenuContent(actions, withIcon = withIcon)
        }
    }
}
