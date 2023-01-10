/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model


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