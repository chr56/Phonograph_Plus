package player.phonograph.loader

import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns
import android.provider.MediaStore
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
    fun getSongs(context: Context, query: String): List<Song> = MediaStoreUtil.getSongs(context, query)

    @JvmStatic
    fun getSong(context: Context, queryId: Long): Song = MediaStoreUtil.getSong(context, queryId)

    @JvmStatic
    fun getSongs(cursor: Cursor?): List<Song> = MediaStoreUtil.getSongs(cursor)

    @JvmStatic
    fun getSong(cursor: Cursor?): Song = MediaStoreUtil.getSong(cursor)

    @JvmStatic
    fun makeSongCursor(context: Context, selection: String?, selectionValues: Array<String>?): Cursor? =
        querySongs(context, selection, selectionValues)

    @JvmStatic
    fun makeSongCursor(context: Context, selection: String?, selectionValues: Array<String>?, sortOrder: String?): Cursor? =
        querySongs(context, selection, selectionValues, sortOrder!!)
}
