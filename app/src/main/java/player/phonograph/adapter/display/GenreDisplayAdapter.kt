/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter.display

import androidx.appcompat.app.AppCompatActivity
import player.phonograph.interfaces.MultiSelectionCabProvider
import player.phonograph.mediastore.sort.SortRef
import player.phonograph.model.Genre
import player.phonograph.settings.Setting

class GenreDisplayAdapter(
    activity: AppCompatActivity,
    host: MultiSelectionCabProvider?,
    dataSet: List<Genre>,
    layoutRes: Int,
    cfg: (DisplayAdapter<Genre>.() -> Unit)?
) : DisplayAdapter<Genre>(
    activity, host,
    dataSet,
    layoutRes, cfg
) {

    override fun getSectionNameImp(position: Int): String {
        return when (Setting.instance.genreSortMode.sortRef) {
            SortRef.GENRE_NAME -> dataset[position].name ?: "UNKNOWN"
            SortRef.SONG_COUNT -> dataset[position].songCount.toString()
            else -> ""
        }
    }
}
