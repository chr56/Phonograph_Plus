package player.phonograph.mediastore

import player.phonograph.model.Song
import android.content.Context
import android.provider.MediaStore

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object SongLoader {

    @JvmStatic
    fun all(context: Context): List<Song> = querySongs(context).getSongs()


    @JvmStatic
    fun id(context: Context, queryId: Long): Song =
        querySongs(
            context, "${MediaStore.Audio.AudioColumns._ID} =? ", arrayOf(queryId.toString())
        ).getFirstSong()

    @JvmStatic
    fun path(context: Context, path: String): Song =
        querySongs(
            context, "${MediaStore.Audio.AudioColumns.DATA} =? ", arrayOf(path)
        ).getFirstSong()

    @JvmStatic
    fun searchByPath(context: Context, path: String): List<Song> =
        querySongs(
            context, "${MediaStore.Audio.AudioColumns.DATA} LIKE ? ", arrayOf(path)
        ).getSongs()

    @JvmStatic
    fun searchSongs(context: Context, title: String): List<Song> {
        val cursor = querySongs(
            context, "${MediaStore.Audio.AudioColumns.TITLE} LIKE ?", arrayOf("%$title%")
        )
        return cursor.getSongs()
    }
}
