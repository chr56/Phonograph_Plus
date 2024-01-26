/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.activities

import player.phonograph.actions.ClickActionProviders
import player.phonograph.model.Song
import player.phonograph.model.buildInfoString
import player.phonograph.model.getReadableDurationString
import player.phonograph.ui.adapter.ConstDisplayConfig
import player.phonograph.ui.adapter.DisplayConfig.Companion.IMAGE_TYPE_TEXT
import player.phonograph.ui.adapter.ItemLayoutStyle
import player.phonograph.ui.fragments.pages.adapter.SongDisplayAdapter
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup

class AlbumSongDisplayAdapter(
    activity: AppCompatActivity,
) : SongDisplayAdapter(activity, ConstDisplayConfig(ItemLayoutStyle.LIST, imageType = IMAGE_TYPE_TEXT, usePalette = false)) {

    override fun getSectionNameImp(position: Int): String = getTrackNumber(dataset[position])

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder<Song> {
        return AlbumSongViewHolder(inflatedView(parent, viewType))
    }

    open class AlbumSongViewHolder(itemView: View) : DisplayViewHolder<Song>(itemView) {
        override fun getRelativeOrdinalText(item: Song): String =
            getTrackNumber(item)

        override fun getDescription(item: Song): CharSequence? =
            buildInfoString(getReadableDurationString(item.duration), item.artistName)

        override val clickActionProvider: ClickActionProviders.ClickActionProvider<Song>
            get() = ClickActionProviders.SongClickActionProvider()
    }

    companion object {
        private fun getTrackNumber(item: Song): String {
            val num = toFixedTrackNumber(item.trackNumber)
            return if (num > 0) num.toString() else "-"
        }

        // iTunes uses for example 1002 for track 2 CD1 or 3011 for track 11 CD3.
        // this method converts those values to normal track numbers
        private fun toFixedTrackNumber(trackNumberToFix: Int): Int = trackNumberToFix % 1000

    }
}
