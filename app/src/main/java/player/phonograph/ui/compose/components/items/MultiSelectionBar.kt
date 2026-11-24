/*
 * Copyright (c) 2026 chr_56
 */

package player.phonograph.ui.compose.components.items

import player.phonograph.R
import player.phonograph.model.menu.ActionEntry
import player.phonograph.model.menu.ActionEntry.ToolbarEntry.Companion.ALWAYS
import player.phonograph.model.menu.ActionEntry.ToolbarEntry.Companion.IF_ROOM
import player.phonograph.ui.compose.components.actions.ActionIconButton
import androidx.compose.foundation.layout.Box
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

@Composable
fun <T> MultiSelectionBar(
    state: MultiSelectionState<T>,
    entries: List<ActionEntry>,
    modifier: Modifier = Modifier,
    maxVisibleActions: Int = 2,
) {
    val (inlineEntries, overflowEntries) = remember(entries, maxVisibleActions) { splitOverflowEntries(entries, maxVisibleActions) }
    var expanded by remember(state) { mutableStateOf(false) }
    TopAppBar(
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = state::clear) {
                Icon(Icons.Default.Clear, contentDescription = stringResource(android.R.string.cancel))
            }
        },
        title = { Text(stringResource(R.string.tips_x_selected, state.selectedCount)) },
        actions = {
            for (entry in inlineEntries) {
                ActionIconButton(entry)
            }
            if (overflowEntries.isNotEmpty()) {
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, stringResource(R.string.action_more))
                    }
                    ItemMenu(
                        expanded = expanded,
                        entries = overflowEntries,
                        onDismiss = { expanded = false },
                    )
                }
            }
        },
        backgroundColor = MaterialTheme.colors.primary,
        contentColor = MaterialTheme.colors.onPrimary,
    )
}

private fun splitOverflowEntries(entries: List<ActionEntry>, limitation: Int) = run {
    val alwaysCount = entries.count {
        it is ActionEntry.ToolbarEntry && it.visibility == ALWAYS
    }
    var remaining = (limitation - alwaysCount).coerceAtLeast(0)

    val inline = mutableListOf<ActionEntry.ToolbarEntry>()
    val overflow = mutableListOf<ActionEntry>()
    for (entry in entries) {
        if (entry is ActionEntry.ToolbarEntry) {
            when {
                entry.visibility == ALWAYS                   -> inline.add(entry)
                entry.visibility == IF_ROOM && remaining > 0 -> {
                    inline.add(entry)
                    remaining--
                }

                else                                         -> overflow.add(entry)
            }
        } else {
            // non toolbar entries are hidden always
            overflow.add(entry)
        }
    }
    inline to overflow
}
