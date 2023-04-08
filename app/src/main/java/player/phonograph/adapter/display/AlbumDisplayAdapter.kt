/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter.display

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import coil.size.ViewSizeResolver
import player.phonograph.R
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.coil.loadImage
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.model.Album
import player.phonograph.model.getYearString
import player.phonograph.model.sort.SortRef
import player.phonograph.settings.Setting
import player.phonograph.util.StringUtil

class AlbumDisplayAdapter(
    activity: AppCompatActivity,
    cabController: MultiSelectionCabController?,
    dataSet: List<Album>,
    layoutRes: Int,
    cfg: (DisplayAdapter<Album>.() -> Unit)?,
) : DisplayAdapter<Album>(activity, cabController, dataSet, layoutRes, cfg) {

    override fun setImage(holder: DisplayViewHolder, position: Int) {
        holder.image?.let { view ->
            loadImage(activity) {
                data(dataset[position].safeGetFirstSong())
                size(ViewSizeResolver(view))
                target(
                    PaletteTargetBuilder(context)
                        .onStart {
                            view.setImageResource(R.drawable.default_album_art)
                            setPaletteColors(context.getColor(R.color.defaultFooterColor), holder)
                        }
                        .onResourceReady { result, palette ->
                            view.setImageDrawable(result)
                            if (usePalette) setPaletteColors(palette, holder)
                        }
                        .build()
                )
            }
        }
    }

    override fun getSectionNameImp(position: Int): String {
        val album = dataset[position]
        val sectionName: String =
            when (Setting.instance.albumSortMode.sortRef) {
                SortRef.ALBUM_NAME -> StringUtil.getSectionName(album.title)
                SortRef.ARTIST_NAME -> StringUtil.getSectionName(album.artistName)
                SortRef.YEAR -> getYearString(album.year)
                SortRef.SONG_COUNT -> album.songCount.toString()
                else -> ""
            }
        return sectionName
    }

    override fun getRelativeOrdinalText(item: Album): String = item.songCount.toString()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder {
        return AlbumViewHolder(
            LayoutInflater.from(activity).inflate(layoutRes, parent, false)
        )
    }

    inner class AlbumViewHolder(itemView: View) : DisplayViewHolder(itemView) {
        init {
            setImageTransitionName(itemView.context.getString(R.string.transition_album_art))
        }
    }
}
