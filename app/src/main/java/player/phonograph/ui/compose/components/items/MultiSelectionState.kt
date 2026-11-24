/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.compose.components.items

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.setValue

class MultiSelectionState<T>(private val key: (T) -> Any) {

    val selectionMode: Boolean get() = _selected.isNotEmpty()
    val selectedCount: Int get() = _selected.size

    // Map equality ignores iteration order, which determines selection order.
    private var _selected by mutableStateOf<Map<Any, T>>(emptyMap(), referentialEqualityPolicy())
    val selected: List<T> get() = _selected.values.toList()

    fun isSelected(item: T): Boolean = _selected.containsKey(key(item))

    fun toggle(item: T) {
        val itemKey = key(item)
        _selected = _selected.toMutableMap().also {
            if (it.containsKey(itemKey)) it.remove(itemKey) else it[itemKey] = item
        }
    }

    fun select(items: Iterable<T>) {
        _selected = items.associateBy(key)
    }

    fun unselect(item: T) {
        _selected = _selected - key(item)
    }

    fun clear() {
        _selected = emptyMap()
    }
}
