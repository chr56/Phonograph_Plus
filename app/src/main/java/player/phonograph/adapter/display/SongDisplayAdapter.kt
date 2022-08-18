/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter.display

import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import player.phonograph.R
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.coil.loadImage
import player.phonograph.coil.target.PhonographColoredTarget
import player.phonograph.model.Song
import player.phonograph.model.getReadableDurationString
import player.phonograph.model.getYearString
import player.phonograph.model.sort.SortRef
import player.phonograph.settings.Setting
import player.phonograph.util.MusicUtil
import java.text.SimpleDateFormat
import java.util.*

open class SongDisplayAdapter(
    activity: AppCompatActivity,
    cabController: MultiSelectionCabController?,
    dataSet: List<Song>,
    layoutRes: Int,
    cfg: (DisplayAdapter<Song>.() -> Unit)?,
) : DisplayAdapter<Song>(activity, cabController, dataSet, layoutRes, cfg) {

    override fun setImage(holder: DisplayViewHolder, position: Int) {
        holder.image?.let {
            loadImage(context)
                .from(dataset[position])
                .into(
                    object : PhonographColoredTarget() {
                        override fun onStart(placeholder: Drawable?) {
                            it.setImageResource(R.drawable.default_album_art)
                            setPaletteColors(defaultFooterColor, holder)
                        }

                        override fun onResourcesReady(drawable: Drawable) {
                            it.setImageDrawable(drawable)
                        }

                        override fun onColorReady(color: Int) {
                            if (usePalette) setPaletteColors(color, holder)
                        }
                    }
                )
                .enqueue()
        }
    }

    override fun getSectionNameImp(position: Int): String {
        val song = dataset[position]
        val sectionName: String =
            when (Setting.instance.songSortMode.sortRef) {
                SortRef.SONG_NAME -> MusicUtil.getSectionName(song.title)
                SortRef.ARTIST_NAME -> MusicUtil.getSectionName(song.artistName)
                SortRef.ALBUM_NAME -> MusicUtil.getSectionName(song.albumName)
                SortRef.YEAR -> getYearString(song.year)
                SortRef.DURATION -> getReadableDurationString(song.duration)
                SortRef.MODIFIED_DATE ->
                    SimpleDateFormat("yy.MM.dd", Locale.getDefault()).format(song.dateModified * 1000)
                SortRef.ADDED_DATE ->
                    SimpleDateFormat("yy.MM.dd", Locale.getDefault()).format(song.dateAdded * 1000)
                else -> ""
            }
        return sectionName
    }
}
