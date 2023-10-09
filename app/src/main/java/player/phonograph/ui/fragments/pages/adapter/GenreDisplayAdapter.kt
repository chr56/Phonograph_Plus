/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.fragments.pages.adapter

import player.phonograph.model.Genre
import player.phonograph.model.sort.SortRef
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.adapter.ConstDisplayConfig
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.adapter.ItemLayoutStyle
import player.phonograph.util.text.makeSectionName
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup

class GenreDisplayAdapter(
    activity: AppCompatActivity,
) : DisplayAdapter<Genre>(activity, ConstDisplayConfig(ItemLayoutStyle.LIST_NO_IMAGE)) {

    override fun getSectionNameImp(position: Int): String {
        val sortMode = Setting(activity).Composites[Keys.genreSortMode].data
        return when (sortMode.sortRef) {
            SortRef.DISPLAY_NAME -> makeSectionName(dataset[position].name)
            SortRef.SONG_COUNT   -> dataset[position].songCount.toString()
            else                 -> ""
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder<Genre> =
        GenreViewHolder(inflatedView(parent, viewType))

    class GenreViewHolder(itemView: View) : DisplayViewHolder<Genre>(itemView)

}
