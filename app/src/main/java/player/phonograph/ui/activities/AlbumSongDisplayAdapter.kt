/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.activities

import player.phonograph.model.Displayable
import player.phonograph.model.Song
import player.phonograph.model.getReadableDurationString
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.fragments.pages.adapter.SongDisplayAdapter
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup

class AlbumSongDisplayAdapter(
    activity: AppCompatActivity,
    dataSet: List<Song>,
    layoutRes: Int,
    cfg: (DisplayAdapter<Song>.() -> Unit)?,
) : SongDisplayAdapter(
    activity, dataSet,
    layoutRes,
    cfg
) {

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder {
        return AlbumSongDisplayViewHolder(inflatedView(layoutRes, parent))
    }

    open inner class AlbumSongDisplayViewHolder(itemView: View) : DisplayViewHolder(itemView) {
        override fun <I : Displayable> getRelativeOrdinalText(item: I): String {
            return getTrackNumber(item as Song)
        }

        override fun <I : Displayable> getDescription(item: I): CharSequence? {
            val song = item as Song
            return "${getReadableDurationString(song.duration)} · ${song.artistName}"
        }
    }
}
