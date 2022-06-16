/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter.display

import androidx.appcompat.app.AppCompatActivity
import player.phonograph.interfaces.MultiSelectionCabProvider
import player.phonograph.model.Song
import player.phonograph.util.MusicUtil

class AlbumSongDisplayAdapter(
    activity: AppCompatActivity,
    host: MultiSelectionCabProvider?,
    dataSet: List<Song>,
    layoutRes: Int,
    cfg: (DisplayAdapter<Song>.() -> Unit)?,
) : SongDisplayAdapter(
    activity, host,
    dataSet,
    layoutRes, cfg
) {
    override fun getDescription(item: Song): CharSequence {
        val song = (item as? Song) ?: Song.EMPTY_SONG
        return "${MusicUtil.getReadableDurationString(song.duration)} · ${song.artistName}"
    }
}
