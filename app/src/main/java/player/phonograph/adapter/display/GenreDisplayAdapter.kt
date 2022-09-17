/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter.display

import androidx.appcompat.app.AppCompatActivity
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.model.sort.SortRef
import player.phonograph.model.Genre
import player.phonograph.settings.SortOrderSettings
import player.phonograph.util.MusicUtil

class GenreDisplayAdapter(
    activity: AppCompatActivity,
    cabController: MultiSelectionCabController?,
    dataSet: List<Genre>,
    layoutRes: Int,
    cfg: (DisplayAdapter<Genre>.() -> Unit)?
) : DisplayAdapter<Genre>(
    activity, cabController,
    dataSet,
    layoutRes, cfg
) {

    override fun getSectionNameImp(position: Int): String {
        return when (SortOrderSettings.instance.genreSortMode.sortRef) {
            SortRef.DISPLAY_NAME -> MusicUtil.getSectionName(dataset[position].name)
            SortRef.SONG_COUNT -> dataset[position].songCount.toString()
            else -> ""
        }
    }
}
