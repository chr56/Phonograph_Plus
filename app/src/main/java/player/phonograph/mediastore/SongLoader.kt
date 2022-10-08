package player.phonograph.mediastore

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import player.phonograph.model.Song

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object SongLoader {

    @JvmStatic
    fun getAllSongs(context: Context): List<Song> = querySongs(context).getSongs()


    @JvmStatic
    fun getSong(context: Context, queryId: Long): Song {
        val cursor =
            querySongs(
                context, "${MediaStore.Audio.AudioColumns._ID} =? ", arrayOf(queryId.toString())
            )
        return cursor.getFirstSong()
    }

    @JvmStatic
    fun getSongs(context: Context, title: String): List<Song> {
        val cursor = querySongs(
            context, "${MediaStore.Audio.AudioColumns.TITLE} LIKE ?", arrayOf("%$title%")
        )
        return cursor.getSongs()
    }

    fun getSongs(cursor: Cursor?): List<Song> = cursor.getSongs() //todo

    @JvmStatic
    fun makeSongCursor(
        context: Context,
        selection: String,
        selectionValues: Array<String>,
        sortOrder: String? = null,
    ): Cursor? =
        querySongs(context, selection, selectionValues, sortOrder)
}
