/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.fragments.pages.adapter

import coil.size.ViewSizeResolver
import player.phonograph.R
import player.phonograph.actions.ClickActionProviders
import player.phonograph.coil.loadImage
import player.phonograph.coil.target.PaletteTargetBuilder
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

        override fun loadImage(item: Album, usePalette: Boolean) {
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

        override val clickActionProvider: ClickActionProviders.ClickActionProvider<Album>
            get() = ClickActionProviders.AlbumClickActionProvider()
    }
}
