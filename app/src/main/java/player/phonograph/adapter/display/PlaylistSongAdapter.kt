/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.adapter.display

import androidx.appcompat.app.AppCompatActivity
import player.phonograph.interfaces.MultiSelectionCabProvider
import player.phonograph.model.Song

class PlaylistSongAdapter(
    activity: AppCompatActivity,
    host: MultiSelectionCabProvider?,
    dataSet: List<Song>,
    layoutRes: Int,
    cfg: (DisplayAdapter<Song>.() -> Unit)?
) : SongDisplayAdapter(activity, host, dataSet, layoutRes, cfg) {
    override fun getSectionNameImp(position: Int): String = (position + 1).toString()
}
