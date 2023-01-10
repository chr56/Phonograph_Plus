/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.components

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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp


interface SortableConfigContract {
    fun size(): Int
    fun itemsAt(position: Int): Item
    fun allItems(): List<Item>
    fun move(from: Int, to: Int)
    fun toggle(position: Int, newValue: Boolean)
    fun toggle(item: Item, newValue: Boolean)
    interface Item {
        fun text(): String
        fun enabled(): Boolean
        fun id(): Int
    }
}

@Composable
fun SortableConfigScreen(adapter: SortableConfigContract) {
    var data by remember { mutableStateOf(adapter.allItems()) }
    val state = rememberReorderableLazyListState(
        onMove = { from, to ->
            adapter.move(from.index, to.index)
            data = data.toMutableList().apply { add(to.index, removeAt(from.index)) }
        })
    LazyColumn(
        modifier = Modifier
            .padding(16.dp)
            .reorderable(state)
            .detectReorderAfterLongPress(state),
        state = state.listState
    ) {
        items(data/* */) { item ->
            ReorderableItem(reorderableState = state, key = item.id()) {
                val enabled = remember { mutableStateOf(item.enabled()) }
                val onToggle = { newValue: Boolean ->
                    enabled.value = newValue
                    adapter.toggle(item, newValue)
                }
                Item(itemName = item.text(), enabled = enabled, onCheckedChange = onToggle)
            }
        }
    }
}

@Composable
private fun Item(
    itemName: String,
    enabled: MutableState<Boolean>,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.padding(horizontal = 8.dp)
    ) {
        Checkbox(
            checked = enabled.value,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = itemName,
            modifier = Modifier.align(Alignment.CenterVertically),
            textAlign = TextAlign.Start
        )
    }
}

class SortableConfigAdapter(val data: MutableList<Item>) : SortableConfigContract {

    override fun size(): Int = data.size

    override fun itemsAt(position: Int): Item = data[position]

    override fun allItems(): List<Item> = data

    override fun move(from: Int, to: Int) {
        data.add(to, data.removeAt(from))
    }

    override fun toggle(position: Int, newValue: Boolean) {
        data[position].enabled = newValue
    }

    override fun toggle(item: SortableConfigContract.Item, newValue: Boolean) {
        data.find { item.id() == it.id }?.enabled = newValue
    }

    class Item(val text: String, var enabled: Boolean, val id: Int) : SortableConfigContract.Item {
        override fun text(): String = text
        override fun enabled(): Boolean = enabled
        override fun id(): Int = id
    }
}