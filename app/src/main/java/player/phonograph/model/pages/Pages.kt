/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.pages

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.graphics.BlendModeCompat
import player.phonograph.R
import player.phonograph.util.ImageUtil.getTintedDrawable

object Pages {
    const val EMPTY = "EMPTY"
    const val SONG = "SONG"
    const val ALBUM = "ALBUM"
    const val ARTIST = "ARTIST"
    const val PLAYLIST = "PLAYLIST"
    const val GENRE = "GENRE"
    const val FOLDER = "FOLDER"

    fun getDisplayName(pager: String?, context: Context): String {
        return when (pager) {
            SONG -> context.getString(R.string.songs)
            ALBUM -> context.getString(R.string.albums)
            ARTIST -> context.getString(R.string.artists)
            PLAYLIST -> context.getString(R.string.playlists)
            GENRE -> context.getString(R.string.genres)
            FOLDER -> context.getString(R.string.folders)
            EMPTY -> context.getString(R.string.empty)
            else -> "UNKNOWN"
        }
    }

    fun getTintedIcon(
        pager: String?,
        color: Int,
        context: Context,
        mode: BlendModeCompat = BlendModeCompat.SRC_IN
    ): Drawable? {
        val id = when (pager) {
            SONG -> R.drawable.ic_music_note_white_24dp
            ALBUM -> R.drawable.ic_album_white_24dp
            ARTIST -> R.drawable.ic_person_white_24dp
            PLAYLIST -> R.drawable.ic_queue_music_white_24dp
            GENRE -> R.drawable.ic_bookmark_music_white_24dp
            FOLDER -> R.drawable.ic_folder_white_24dp
            else -> R.drawable.ic_library_music_white_24dp
        }
        return context.getTintedDrawable(id, color, mode)
    }
}