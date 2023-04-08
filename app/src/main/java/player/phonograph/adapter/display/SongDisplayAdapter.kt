/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter.display

import coil.size.ViewSizeResolver
import player.phonograph.R
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.coil.loadImage
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.model.Song
import player.phonograph.model.getReadableDurationString
import player.phonograph.model.getYearString
import player.phonograph.model.sort.SortRef
import player.phonograph.settings.Setting
import player.phonograph.util.text.makeSectionName
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Locale

open class SongDisplayAdapter(
    activity: AppCompatActivity,
    cabController: MultiSelectionCabController?,
    dataSet: List<Song>,
    layoutRes: Int,
    cfg: (DisplayAdapter<Song>.() -> Unit)?,
) : DisplayAdapter<Song>(activity, cabController, dataSet, layoutRes, cfg) {

    override fun setImage(holder: DisplayViewHolder, position: Int) {
        holder.image?.let { view ->
            loadImage(context) {
                data(dataset[position])
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
        val song = dataset[position]
        val sectionName: String =
            when (Setting.instance.songSortMode.sortRef) {
                SortRef.SONG_NAME -> makeSectionName(song.title)
                SortRef.ARTIST_NAME -> makeSectionName(song.artistName)
                SortRef.ALBUM_NAME -> makeSectionName(song.albumName)
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
