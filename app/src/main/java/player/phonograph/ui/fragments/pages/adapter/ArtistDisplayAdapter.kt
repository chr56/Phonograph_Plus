/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.fragments.pages.adapter

import coil.size.ViewSizeResolver
import player.phonograph.R
import player.phonograph.coil.loadImage
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.model.Artist
import player.phonograph.model.Displayable
import player.phonograph.model.sort.SortRef
import player.phonograph.settings.Setting
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.util.text.makeSectionName
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup

class ArtistDisplayAdapter(
    activity: AppCompatActivity,
    dataSet: List<Artist>,
    layoutRes: Int,
    cfg: (DisplayAdapter<Artist>.() -> Unit)?,
) : DisplayAdapter<Artist>(activity, dataSet, layoutRes, cfg) {

    override fun getSectionNameImp(position: Int): String {
        val artist = dataset[position]
        val sectionName: String =
            when (Setting.instance.artistSortMode.sortRef) {
                SortRef.ARTIST_NAME -> makeSectionName(artist.name)
                SortRef.ALBUM_COUNT -> artist.albumCount.toString()
                SortRef.SONG_COUNT  -> artist.songCount.toString()
                else                -> ""
            }
        return makeSectionName(sectionName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder {
        return ArtistViewHolder(inflatedView(layoutRes, parent))
    }

    class ArtistViewHolder(itemView: View) : DisplayViewHolder(itemView) {
        init {
            setImageTransitionName(itemView.context.getString(R.string.transition_artist_image))
        }

        override fun <I : Displayable> getRelativeOrdinalText(item: I): String {
            return (item as Artist).songCount.toString()
        }

        override val defaultIcon: Drawable?
            get() = AppCompatResources.getDrawable(itemView.context, R.drawable.default_artist_image)

        override fun <I : Displayable> setImage(
            position: Int,
            dataset: List<I>,
            usePalette: Boolean,
        ) {
            super.setImage(position, dataset, usePalette)
            val context = itemView.context
            image?.let { view ->
                loadImage(itemView.context) {
                    data(dataset[position])
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
