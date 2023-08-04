/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.fragments.pages.adapter

import coil.size.ViewSizeResolver
import player.phonograph.R
import player.phonograph.coil.loadImage
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.model.Album
import player.phonograph.model.Displayable
import player.phonograph.model.getYearString
import player.phonograph.model.sort.SortRef
import player.phonograph.settings.Setting
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.util.text.makeSectionName
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup

open class AlbumDisplayAdapter(
    activity: AppCompatActivity,
    dataSet: List<Album>,
    layoutRes: Int,
) : DisplayAdapter<Album>(activity, dataSet, layoutRes) {

    override fun getSectionNameImp(position: Int): String {
        val album = dataset[position]
        val sectionName: String =
            when (Setting.instance.albumSortMode.sortRef) {
                SortRef.ALBUM_NAME -> makeSectionName(album.title)
                SortRef.ARTIST_NAME -> makeSectionName(album.artistName)
                SortRef.YEAR -> getYearString(album.year)
                SortRef.SONG_COUNT -> album.songCount.toString()
                else -> ""
            }
        return sectionName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder {
        return AlbumViewHolder(inflatedView(layoutRes, parent))
    }

    inner class AlbumViewHolder(itemView: View) : DisplayViewHolder(itemView) {
        init {
            setImageTransitionName(itemView.context.getString(R.string.transition_album_art))
        }

        override fun <I : Displayable> getRelativeOrdinalText(item: I): String = (item as Album).songCount.toString()

        override fun <I : Displayable> setImage(
            position: Int,
            dataset: List<I>,
            usePalette: Boolean
        ) {
            val context = itemView.context
            image?.let { view ->
                loadImage(context) {
                    data((dataset[position] as Album).safeGetFirstSong())
                    size(ViewSizeResolver(view))
                    target(
                        PaletteTargetBuilder(context)
                            .onStart {
                                view.setImageResource(R.drawable.default_album_art)
                                setPaletteColors(context.getColor(R.color.defaultFooterColor))
                            }
                            .onResourceReady { result, palette ->
                                view.setImageDrawable(result)
                                if (usePalette) setPaletteColors(palette)
                            }
                            .build()
                    )
                }
            }
        }
    }
}
