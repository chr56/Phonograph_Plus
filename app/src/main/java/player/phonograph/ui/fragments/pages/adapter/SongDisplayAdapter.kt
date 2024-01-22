/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments.pages.adapter

import coil.size.ViewSizeResolver
import player.phonograph.R
import player.phonograph.actions.ClickActionProviders
import player.phonograph.actions.menu.ActionMenuProviders
import player.phonograph.coil.loadImage
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.model.Song
import player.phonograph.model.getReadableDurationString
import player.phonograph.model.getYearString
import player.phonograph.model.sort.SortRef
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.adapter.DisplayConfig
import player.phonograph.util.text.dateTextShortText
import player.phonograph.util.text.makeSectionName
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup

open class SongDisplayAdapter(
    activity: AppCompatActivity,
    displayConfig: DisplayConfig,
) : DisplayAdapter<Song>(activity, displayConfig) {


    override fun getSectionNameImp(position: Int): String {
        val song = dataset[position]
        val sortMode = Setting(activity).Composites[Keys.songSortMode].data
        val sectionName: String =
            when (sortMode.sortRef) {
                SortRef.SONG_NAME         -> makeSectionName(song.title)
                SortRef.ARTIST_NAME       -> makeSectionName(song.artistName)
                SortRef.ALBUM_NAME        -> makeSectionName(song.albumName)
                SortRef.ALBUM_ARTIST_NAME -> makeSectionName(song.albumArtistName)
                SortRef.COMPOSER          -> makeSectionName(song.composer)
                SortRef.YEAR              -> getYearString(song.year)
                SortRef.DURATION          -> getReadableDurationString(song.duration)
                SortRef.MODIFIED_DATE     -> dateTextShortText(song.dateModified)
                SortRef.ADDED_DATE        -> dateTextShortText(song.dateAdded)
                else                      -> ""
            }
        return sectionName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder<Song> =
        SongViewHolder(inflatedView(parent, viewType))

    class SongViewHolder(itemView: View) : DisplayViewHolder<Song>(itemView) {
        override fun setImage(item: Song, usePalette: Boolean) {
            val context = itemView.context
            image?.let { view ->
                view.visibility = View.VISIBLE
                loadImage(context) {
                    data(item)
                    size(ViewSizeResolver(view))
                    target(
                        PaletteTargetBuilder(context)
                            .onStart {
                                view.setImageResource(R.drawable.default_album_art)
                                setPaletteColors(context.getColor(R.color.defaultFooterColor))
                            }
                            .withConditionalYield { attached }
                            .onResourceReady { result, palette ->
                                view.setImageDrawable(result)
                                if (usePalette) setPaletteColors(palette)
                            }
                            .build()
                    )
                }
            }
        }

        override val clickActionProvider: ClickActionProviders.ClickActionProvider<Song>
            get() = ClickActionProviders.SongClickActionProvider()

        override val menuProvider: ActionMenuProviders.ActionMenuProvider<Song>
            get() = ActionMenuProviders.SongActionMenuProvider(showPlay = false)
    }
}
