package player.phonograph.loader

import android.content.Context
import android.database.Cursor
import player.phonograph.model.Song
import player.phonograph.util.MediaStoreUtil
import player.phonograph.util.MediaStoreUtil.querySongs

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object SongLoader {

    @JvmStatic
    fun getAllSongs(context: Context): List<Song?> = MediaStoreUtil.getAllSongs(context)

    @JvmStatic
    fun getSongs(cursor: Cursor?): List<Song> = MediaStoreUtil.getSongs(cursor)

    @JvmStatic
    fun makeSongCursor(context: Context, selection: String?, selectionValues: Array<String>?): Cursor? =
        querySongs(context, selection, selectionValues)
}
