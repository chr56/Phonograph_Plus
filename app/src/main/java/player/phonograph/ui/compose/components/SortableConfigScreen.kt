/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.components

import org.burnoutcrew.reorderable.ItemPosition
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

class SortableConfigModel(init: List<Item>) {
    private var _items: Items = ArrayList(init)
    val items: Items @Synchronized get() = _items

    fun refresh(newItems: List<Item>) {
        synchronized(_items) {
            _items = ArrayList(newItems)
        }
    }

    fun move(from: Int, to: Int) {
        synchronized(_items) {
            _items.add(to, _items.removeAt(from))
        }
    }

    fun toggle(item: Item, enabled: Boolean) {
        item.enabled = enabled
    }

    interface Item {
        fun name(): String
        fun id(): String
        var enabled: Boolean
    }
}

typealias Items = ArrayList<SortableConfigModel.Item>

@Composable
fun SortableConfigScreen(model: SortableConfigModel) {
    val onMove: (ItemPosition, ItemPosition) -> Unit =
        { from, to -> model.move(from.index, to.index) }
    val onToggleEnabled: (SortableConfigModel.Item, Boolean) -> Unit =
        { item, enabled -> model.toggle(item, enabled) }
    SortableConfigList(items = model.items, onMove = onMove, onToggleEnabled = onToggleEnabled)
}

@Composable
fun SortableConfigList(
    items: Items,
    onMove: (ItemPosition, ItemPosition) -> Unit,
    onToggleEnabled: (SortableConfigModel.Item, Boolean) -> Unit
) {
    val state = rememberReorderableLazyListState(onMove = onMove)
    LazyColumn(
        modifier = Modifier
            .padding(16.dp)
            .reorderable(state)
            .detectReorderAfterLongPress(state),
        state = state.listState
    ) {
        items(items, { it.id() }) { item ->
            ReorderableItem(state, key = item.id()) {
                Item(sortableConfigItem = item) { onToggleEnabled(item, it) }
            }
        }
    }

}

@Composable
private fun Item(
    sortableConfigItem: SortableConfigModel.Item,
    onCheckedChange: (Boolean) -> Unit
) {
    val enabled = remember { mutableStateOf(sortableConfigItem.enabled) }
    Row(
        Modifier.padding(horizontal = 8.dp)
    ) {
        Checkbox(
            checked = enabled.value,
            onCheckedChange = {
                onCheckedChange(it)
                enabled.value = it
            }
        )
        Text(
            text = sortableConfigItem.name(),
            modifier = Modifier.align(Alignment.CenterVertically),
            textAlign = TextAlign.Start
        )
    }
}