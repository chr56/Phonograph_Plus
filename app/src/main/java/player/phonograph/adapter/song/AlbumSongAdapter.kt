package player.phonograph.adapter.song

import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import player.phonograph.R
import player.phonograph.interfaces.CabHolder
import player.phonograph.model.Song
import player.phonograph.util.MusicUtil

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class AlbumSongAdapter(
    activity: AppCompatActivity,
    dataSet: List<Song>,
    @LayoutRes itemLayoutRes: Int,
    usePalette: Boolean,
    cabHolder: CabHolder?
) : SongAdapter(
    activity, dataSet, itemLayoutRes, usePalette, cabHolder
) {
    override fun createViewHolder(view: View): SongAdapter.ViewHolder {
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongAdapter.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val song = dataSet[position]
        holder.imageText?.let {
            val trackNumber = MusicUtil.getFixedTrackNumber(song.trackNumber)
            val trackNumberString =
                if (trackNumber > 0) trackNumber.toString() else "-"
            it.text = trackNumberString
        }
    }

    override fun getSongText(song: Song): String {
        return MusicUtil.getReadableDurationString(song.duration)
    }

    inner class ViewHolder(itemView: View) : SongAdapter.ViewHolder(itemView) {
        init {
            if (imageText != null) {
                imageText!!.visibility = View.VISIBLE
            }
            if (image != null) {
                image!!.visibility = View.GONE
            }
        }

        override val menuRes: Int
            get() = R.menu.menu_item_song_short
    }

    override fun loadAlbumCover(song: Song, holder: SongAdapter.ViewHolder) {
        // We don't want to load it in this adapter
    }
}
