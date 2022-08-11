/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter.display

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import player.phonograph.R
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.glide.PhonographColoredTarget
import player.phonograph.glide.SongGlideRequest
import player.phonograph.model.sort.SortRef
import player.phonograph.model.Album
import player.phonograph.model.getYearString
import player.phonograph.settings.Setting
import player.phonograph.util.MusicUtil

class AlbumDisplayAdapter(
    activity: AppCompatActivity,
    cabController: MultiSelectionCabController?,
    dataSet: List<Album>,
    layoutRes: Int,
    cfg: (DisplayAdapter<Album>.() -> Unit)?,
) : DisplayAdapter<Album>(activity, cabController, dataSet, layoutRes, cfg) {

    override fun setImage(holder: DisplayViewHolder, position: Int) {
        holder.image?.let {
            SongGlideRequest.Builder.from(Glide.with(activity), dataset[position].safeGetFirstSong())
                .checkIgnoreMediaStore(activity)
                .generatePalette(activity).build()
                .into(object : PhonographColoredTarget(holder.image) {
                    override fun onLoadCleared(placeholder: Drawable?) {
                        super.onLoadCleared(placeholder)
                        setPaletteColors(defaultFooterColor, holder)
                    }

                    override fun onColorReady(color: Int) {
                        if (usePalette) setPaletteColors(color, holder)
                        else setPaletteColors(defaultFooterColor, holder)
                    }
                })
        }
    }

    override fun getSectionNameImp(position: Int): String {
        val album = dataset[position]
        val sectionName: String =
            when (Setting.instance.albumSortMode.sortRef) {
                SortRef.ALBUM_NAME -> MusicUtil.getSectionName(album.title)
                SortRef.ARTIST_NAME -> MusicUtil.getSectionName(album.artistName)
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
