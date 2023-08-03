/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.fragments.pages.adapter

import player.phonograph.model.Genre
import player.phonograph.model.sort.SortRef
import player.phonograph.settings.Setting
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.util.text.makeSectionName
import androidx.appcompat.app.AppCompatActivity

class GenreDisplayAdapter(
    activity: AppCompatActivity,
    dataSet: List<Genre>,
    layoutRes: Int,
    cfg: (DisplayAdapter<Genre>.() -> Unit)?
) : DisplayAdapter<Genre>(
    activity, dataSet,
    layoutRes,
    cfg
) {

    override fun getSectionNameImp(position: Int): String {
        return when (Setting.instance.genreSortMode.sortRef) {
            SortRef.DISPLAY_NAME -> makeSectionName(dataset[position].name)
            SortRef.SONG_COUNT -> dataset[position].songCount.toString()
            else -> ""
        }
    }
}
