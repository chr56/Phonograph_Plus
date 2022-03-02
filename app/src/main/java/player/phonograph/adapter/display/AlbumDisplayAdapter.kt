/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter.display

import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import player.phonograph.glide.PhonographColoredTarget
import player.phonograph.glide.SongGlideRequest
import player.phonograph.helper.SortOrder
import player.phonograph.interfaces.MultiSelectionCabProvider
import player.phonograph.model.Album
import player.phonograph.util.MusicUtil
import player.phonograph.settings.PreferenceUtil

class AlbumDisplayAdapter(
    activity: AppCompatActivity,
    host: MultiSelectionCabProvider?,
    dataSet: List<Album>,
    layoutRes: Int,
    cfg: (DisplayAdapter<Album>.() -> Unit)?
) : DisplayAdapter<Album>(activity, host, dataSet, layoutRes, cfg) {

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
        val sectionName =
            when (PreferenceUtil.getInstance(activity).albumSortOrder) {
                SortOrder.AlbumSortOrder.ALBUM_A_Z, SortOrder.AlbumSortOrder.ALBUM_Z_A ->
                    MusicUtil.getSectionName(dataset[position].title)
                SortOrder.AlbumSortOrder.ALBUM_ARTIST, SortOrder.AlbumSortOrder.ALBUM_ARTIST_REVERT ->
                    MusicUtil.getSectionName(dataset[position].artistName)
                SortOrder.AlbumSortOrder.ALBUM_YEAR, SortOrder.AlbumSortOrder.ALBUM_YEAR_REVERT ->
                    MusicUtil.getSectionName(MusicUtil.getYearString(dataset[position].year))
                else -> { "" }
            }
        return sectionName
    }
}
