package player.phonograph.mediastore

import player.phonograph.model.Song
import player.phonograph.model.file.Location
import android.content.Context
import android.provider.MediaStore

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object SongLoader {

    @JvmStatic
    fun all(context: Context): List<Song> = querySongs(context).intoSongs()


    @JvmStatic
    fun id(context: Context, queryId: Long): Song =
        querySongs(
            context, "${MediaStore.Audio.AudioColumns._ID} =? ", arrayOf(queryId.toString())
        ).intoFirstSong()

    @JvmStatic
    fun path(context: Context, path: String): Song =
        querySongs(
            context, "${MediaStore.Audio.AudioColumns.DATA} =? ", arrayOf(path)
        ).intoFirstSong()

    @JvmStatic
    fun searchByPath(context: Context, path: String): List<Song> =
        querySongs(
            context, "${MediaStore.Audio.AudioColumns.DATA} LIKE ? ", arrayOf(path)
        ).intoSongs()

    @JvmStatic
    fun searchByLocation(context: Context, currentLocation: Location): List<Song> =
        searchByPath(context, "%${currentLocation.absolutePath}%")

    @JvmStatic
    fun searchByTitle(context: Context, title: String): List<Song> {
        val cursor = querySongs(
            context, "${MediaStore.Audio.AudioColumns.TITLE} LIKE ?", arrayOf("%$title%")
        )
        return cursor.intoSongs()
    }
}
