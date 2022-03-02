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
import player.phonograph.model.Song
import player.phonograph.util.MusicUtil
import player.phonograph.settings.PreferenceUtil
import java.text.SimpleDateFormat
import java.util.*

class SongDisplayAdapter(
    activity: AppCompatActivity,
    host: MultiSelectionCabProvider?,
    dataSet: List<Song>,
    layoutRes: Int,
    cfg: (DisplayAdapter<Song>.() -> Unit)?
) : DisplayAdapter<Song>(activity, host, dataSet, layoutRes, cfg) {

    override fun setImage(holder: DisplayViewHolder, position: Int) {
        holder.image?.let {
            SongGlideRequest.Builder.from(Glide.with(activity), dataset[position])
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
        val sectionName: String =
            when (PreferenceUtil.getInstance(activity).songSortOrder) {
                SortOrder.SongSortOrder.SONG_A_Z, SortOrder.SongSortOrder.SONG_Z_A ->
                    MusicUtil.getSectionName(dataset[position].title)
                SortOrder.SongSortOrder.SONG_ALBUM, SortOrder.SongSortOrder.SONG_ALBUM_REVERT ->
                    MusicUtil.getSectionName(dataset[position].albumName)
                SortOrder.SongSortOrder.SONG_ARTIST, SortOrder.SongSortOrder.SONG_ARTIST_REVERT ->
                    MusicUtil.getSectionName(dataset[position].artistName)
                SortOrder.SongSortOrder.SONG_DURATION, SortOrder.SongSortOrder.SONG_DURATION_REVERT ->
                    MusicUtil.getReadableDurationString(dataset[position].duration)
                SortOrder.SongSortOrder.SONG_DATE_MODIFIED, SortOrder.SongSortOrder.SONG_DATE_MODIFIED_REVERT ->
                    SimpleDateFormat("yy.MM.dd", Locale.getDefault()).format(dataset[position].dateModified * 1000)
                SortOrder.SongSortOrder.SONG_YEAR, SortOrder.SongSortOrder.SONG_YEAR_REVERT ->
                    MusicUtil.getYearString(dataset[position].year)
                else -> { "" }
            }
        return sectionName
    }
}
