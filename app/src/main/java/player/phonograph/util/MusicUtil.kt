package player.phonograph.util

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.notification.ErrorNotification
import java.io.File
import java.io.IOException

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object MusicUtil {

    fun getMediaStoreAlbumCoverUri(albumId: Long): Uri =
        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)

    fun getSongFileUri(songId: Long): Uri {
        return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
    }

    fun getYearString(year: Int): String = if (year > 0) year.toString() else "-"

    // iTunes uses for example 1002 for track 2 CD1 or 3011 for track 11 CD3.
    // this method converts those values to normal track numbers
    fun getFixedTrackNumber(trackNumberToFix: Int): Int = trackNumberToFix % 1000

    fun insertAlbumArt(context: Context, albumId: Long, path: String?) {
        val contentResolver = context.contentResolver
        val artworkUri = Uri.parse("content://media/external/audio/albumart")
        contentResolver.delete(ContentUris.withAppendedId(artworkUri, albumId), null, null)
        val values = ContentValues()
        values.put("album_id", albumId)
        values.put("_data", path)
        contentResolver.insert(artworkUri, values)
        contentResolver.notifyChange(artworkUri, null)
    }

    fun deleteAlbumArt(context: Context, albumId: Long) {
        val localUri = Uri.parse("content://media/external/audio/albumart")
        context.contentResolver.apply {
            delete(ContentUris.withAppendedId(localUri, albumId), null, null)
            notifyChange(localUri, null)
        }
    }

    fun createAlbumArtFile() =
        File(createAlbumArtDir(), System.currentTimeMillis().toString())

    fun createAlbumArtDir(): File {
        val albumArtDir = File(Environment.getExternalStorageDirectory(), "/albumthumbs/")
        if (!albumArtDir.exists()) {
            albumArtDir.mkdirs()
            try {
                File(albumArtDir, ".nomedia").createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return albumArtDir
    }

    fun isArtistNameUnknown(artistName: String?): Boolean = when {
        artistName.isNullOrBlank() -> false // not certain
        artistName == Artist.UNKNOWN_ARTIST_DISPLAY_NAME -> true
        artistName.trim().lowercase() == "unknown" -> true
        artistName.trim().lowercase() == "<unknown>" -> true
        else -> false
    }

    fun getSectionName(reference: String?): String {
        if (reference.isNullOrBlank()) return ""
        var str = reference.trim { it <= ' ' }.lowercase()
        str = when {
            str.startsWith("the ") -> str.substring(4)
            str.startsWith("a ") -> str.substring(2)
            else -> str
        }
        return if (str.isEmpty()) "" else str[0].uppercase()
    }

    /**
     * convert a timestamp to a readable String
     *
     * @param t timeStamp to parse (Unit: milliseconds)
     * @return human-friendly time
     */
    fun parseTimeStamp(t: Int): String {
        val ms = (t % 1000).toLong()
        val s = (t % (1000 * 60) / 1000).toLong()
        val m = (t - s * 1000 - ms) / (1000 * 60)
        return String.format("%d:%02d.%03d", m, s, ms)
    }
}
