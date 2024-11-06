/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.main.pages.adapter

import player.phonograph.R
import player.phonograph.mechanism.actions.ClickActionProviders
import player.phonograph.model.Album
import player.phonograph.model.getYearString
import player.phonograph.model.sort.SortRef
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.adapter.DisplayConfig
import player.phonograph.util.text.makeSectionName
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup

open class AlbumDisplayAdapter(
    activity: AppCompatActivity,
    config: DisplayConfig,
) : DisplayAdapter<Album>(activity, config) {

    override fun getSectionNameImp(position: Int): String {
        val album = dataset[position]
        val sortMode = Setting(activity).Composites[Keys.albumSortMode].data
        val sectionName: String =
            when (sortMode.sortRef) {
                SortRef.ALBUM_NAME -> makeSectionName(album.title)
                SortRef.ARTIST_NAME -> makeSectionName(album.artistName)
                SortRef.YEAR -> getYearString(album.year)
                SortRef.SONG_COUNT -> album.songCount.toString()
                else -> ""
            }
        return sectionName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder<Album> {
        return AlbumViewHolder(inflatedView(parent, viewType))
    }

    class AlbumViewHolder(itemView: View) : DisplayViewHolder<Album>(itemView) {
        init {
            setImageTransitionName(itemView.context.getString(R.string.transition_album_art))
        }

        override fun getRelativeOrdinalText(item: Album): String = item.songCount.toString()

        override val clickActionProvider: ClickActionProviders.ClickActionProvider<Album>
            get() = ClickActionProviders.AlbumClickActionProvider()
    }
}
