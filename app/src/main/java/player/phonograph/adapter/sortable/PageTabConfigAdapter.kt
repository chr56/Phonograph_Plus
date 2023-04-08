/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.adapter.sortable

import player.phonograph.model.pages.PageConfig
import player.phonograph.model.pages.Pages
import player.phonograph.util.warning
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class PageTabConfigAdapter(private val pageConfig: PageConfig) : SortableListAdapter<String>() {


    override fun fetchDataset(): SortableList<String> {
        val all = PageConfig.DEFAULT_CONFIG.toMutableSet()
        all.removeAll(pageConfig.tabList.toSet()).report("Strange PageConfig: $pageConfig")
        val visible = pageConfig.map { SortableList.Item(it, true) }
        val invisible = all.toList().map { SortableList.Item(it, false) }
        return SortableList(visible + invisible)
    }

    override fun onCreateContentView(parent: ViewGroup, viewType: Int): View {
        return TextView(parent.context).apply {
            textSize = 16f
            gravity = Gravity.CENTER_VERTICAL
            textAlignment = View.TEXT_ALIGNMENT_TEXT_START
        }
    }

    override fun onBindContentView(contentView: View, position: Int) {
        require(contentView is TextView) { "Receive ${contentView.javaClass.name}" }
        contentView.text = Pages.getDisplayName(dataset[position].content, contentView.context)
    }

    val currentConfig: PageConfig
        get() = PageConfig.from(
            dataset.visibleItems().map { it.content }
        )

    companion object {
        private const val TAG = "PageTabConfigAdapter"
        private fun Boolean.report(msg: String): Boolean {
            if (!this) warning(TAG, msg)
            return this
        }
    }
}