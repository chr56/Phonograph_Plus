/*
 * Copyright (c) 2022-2023 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter.display

import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.model.Song
import player.phonograph.model.getReadableDurationString
import androidx.appcompat.app.AppCompatActivity

class AlbumSongDisplayAdapter(
    activity: AppCompatActivity,
    cabController: MultiSelectionCabController?,
    dataSet: List<Song>,
    layoutRes: Int,
    cfg: (DisplayAdapter<Song>.() -> Unit)?,
) : SongDisplayAdapter(
    activity, cabController,
    dataSet,
    layoutRes, cfg
) {
    override fun getDescription(item: Song): CharSequence {
        val song = (item as? Song) ?: Song.EMPTY_SONG
        return "${getReadableDurationString(song.duration)} · ${song.artistName}"
    }

    override fun getRelativeOrdinalText(item: Song): String = getTrackNumber(item)

    override fun getSectionNameImp(position: Int): String {
        return getTrackNumber(dataset[position])
    }

    private fun getTrackNumber(item: Song): String {
        val num = toFixedTrackNumber(item.trackNumber)
        return if (num > 0) num.toString() else "-"
    }

    // iTunes uses for example 1002 for track 2 CD1 or 3011 for track 11 CD3.
    // this method converts those values to normal track numbers
    private fun toFixedTrackNumber(trackNumberToFix: Int): Int = trackNumberToFix % 1000
}
