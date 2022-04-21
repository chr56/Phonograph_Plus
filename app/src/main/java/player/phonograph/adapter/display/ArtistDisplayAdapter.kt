/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter.display

import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import player.phonograph.glide.ArtistGlideRequest
import player.phonograph.glide.PhonographColoredTarget
import player.phonograph.interfaces.MultiSelectionCabProvider
import player.phonograph.mediastore.sort.SortRef
import player.phonograph.model.Artist
import player.phonograph.settings.Setting
import player.phonograph.util.MusicUtil

class ArtistDisplayAdapter(
    activity: AppCompatActivity,
    host: MultiSelectionCabProvider?,
    dataSet: List<Artist>,
    layoutRes: Int,
    cfg: (DisplayAdapter<Artist>.() -> Unit)?
) : DisplayAdapter<Artist>(activity, host, dataSet, layoutRes, cfg) {

    override fun setImage(holder: DisplayViewHolder, position: Int) {
        holder.image?.let {
            ArtistGlideRequest.Builder.from(Glide.with(activity), dataset[position])
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
        val artist = dataset[position]
        val sectionName: String =
            when (Setting.instance.artistSortMode.sortRef) {
                SortRef.ARTIST_NAME -> MusicUtil.getSectionName(artist.name)
                SortRef.ALBUM_COUNT -> artist.albumCount.toString()
                SortRef.SONG_COUNT -> artist.songCount.toString()
                else -> { "" }
            }
        return MusicUtil.getSectionName(sectionName)
    }
}
