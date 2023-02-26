/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.adapter.sortable

import player.phonograph.model.ImageSource
import player.phonograph.model.config.ImageSourceConfig
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class ImageSourceConfigAdapter(private val sourceConfig: ImageSourceConfig) :
        SortableListAdapter<ImageSource>() {


    override fun fetchDataset(): SortableList<ImageSource> {
        return SortableList(
            sourceConfig.sources.map {
                SortableList.Item(it.imageSource, it.enabled)
            }
        )
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
        contentView.text = dataset[position].content.displayString(contentView.context)
    }

    val currentConfig: ImageSourceConfig
        get() = ImageSourceConfig.from(
            dataset.allItems.map { ImageSourceConfig.Item(it.content.key, it.visible) }
        )

    companion object {
        private const val TAG = "ImageSourceConfigAdapter"
    }
}