/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter.display

import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.glide.PhonographColoredTarget
import player.phonograph.glide.SongGlideRequest
import player.phonograph.mediastore.sort.SortRef
import player.phonograph.model.Song
import player.phonograph.settings.Setting
import player.phonograph.util.MusicUtil

open class SongDisplayAdapter(
    activity: AppCompatActivity,
    cabController: MultiSelectionCabController?,
    dataSet: List<Song>,
    layoutRes: Int,
    cfg: (DisplayAdapter<Song>.() -> Unit)?
) : DisplayAdapter<Song>(activity, cabController, dataSet, layoutRes, cfg) {

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
        val song = dataset[position]
        val sectionName: String =
            when (Setting.instance.songSortMode.sortRef) {
                SortRef.SONG_NAME -> MusicUtil.getSectionName(song.title)
                SortRef.ARTIST_NAME -> MusicUtil.getSectionName(song.artistName)
                SortRef.ALBUM_NAME -> MusicUtil.getSectionName(song.albumName)
                SortRef.YEAR -> MusicUtil.getYearString(song.year)
                SortRef.DURATION -> MusicUtil.getReadableDurationString(song.duration)
                SortRef.MODIFIED_DATE -> SimpleDateFormat("yy.MM.dd", Locale.getDefault()).format(song.dateModified * 1000)
                SortRef.ADDED_DATE -> SimpleDateFormat("yy.MM.dd", Locale.getDefault()).format(song.dateAdded * 1000)
                else -> ""
            }
        return sectionName
    }
}
