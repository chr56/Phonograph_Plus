package player.phonograph.mediastore

import player.phonograph.model.Song
import android.content.Context
import android.provider.MediaStore

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
    fun getSong(context: Context, path: String): Song {
        val cursor =
            querySongs(
                context, "${MediaStore.Audio.AudioColumns.DATA} =? ", arrayOf(path)
            )
        return cursor.getFirstSong()
    }

    @JvmStatic
    fun searchSongs(context: Context, title: String): List<Song> {
        val cursor = querySongs(
            context, "${MediaStore.Audio.AudioColumns.TITLE} LIKE ?", arrayOf("%$title%")
        )
        return cursor.getSongs()
    }
}
