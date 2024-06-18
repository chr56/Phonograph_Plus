/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.model.playlist

import player.phonograph.R
import androidx.annotation.IntDef
import androidx.annotation.StringRes
import android.content.res.Resources

const val PLAYLIST_TYPE_FILE = 1

const val PLAYLIST_TYPE_FAVORITE = 2
const val PLAYLIST_TYPE_LAST_ADDED = 4
const val PLAYLIST_TYPE_HISTORY = 8
const val PLAYLIST_TYPE_MY_TOP_TRACK = 16
const val PLAYLIST_TYPE_RANDOM = 32

@IntDef(
    value = [PLAYLIST_TYPE_FILE,
        PLAYLIST_TYPE_FAVORITE,
        PLAYLIST_TYPE_LAST_ADDED,
        PLAYLIST_TYPE_HISTORY,
        PLAYLIST_TYPE_MY_TOP_TRACK,
        PLAYLIST_TYPE_RANDOM],
)
@Retention(AnnotationRetention.SOURCE)
annotation class PlaylistType

fun playlistTypeName(resources: Resources, @PlaylistType type: Int): CharSequence =
    resources.getString(playlistTypeNameRes(type))

@StringRes
fun playlistTypeNameRes(@PlaylistType type: Int): Int = when (type) {
    PLAYLIST_TYPE_FILE         -> R.string.file
    PLAYLIST_TYPE_FAVORITE     -> R.string.favorites
    PLAYLIST_TYPE_LAST_ADDED   -> R.string.last_added
    PLAYLIST_TYPE_HISTORY      -> R.string.history
    PLAYLIST_TYPE_MY_TOP_TRACK -> R.string.my_top_tracks
    PLAYLIST_TYPE_RANDOM       -> R.string.action_shuffle_all
    else                       -> R.string.playlists
}