package player.phonograph.util

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import player.phonograph.R
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.notification.ErrorNotification
import java.io.File
import java.io.IOException
import java.util.*

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
@Suppress("unused")
object MusicUtil {

    fun getMediaStoreAlbumCoverUri(albumId: Long): Uri =
        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)

    fun getSongFileUri(songId: Long): Uri {
        return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
    }

    fun createShareSongFileIntent(song: Song, context: Context): Intent {
        return try {
            Intent()
                .setAction(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, context.applicationContext.packageName, File(song.data)))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .setType("audio/*")
        } catch (e: IllegalArgumentException) {
            // the path is most likely not like /storage/emulated/0/... but something like /storage/28C7-75B0/...
            ErrorNotification.postErrorNotification(e, "Physical external SD card is not fully support!")
            Intent()
        }
    }

    fun getArtistInfoString(context: Context, artist: Artist): String =
        buildInfoString(
            getAlbumCountString(context, artist.albumCount),
            getSongCountString(context, artist.songCount)
        )

    fun getAlbumInfoString(context: Context, album: Album): String =
        buildInfoString(
            album.artistName,
            getSongCountString(context, album.songCount)
        )

    fun getSongInfoString(song: Song): String {
        return buildInfoString(
            song.artistName,
            song.albumName
        )
    }

    fun getGenreInfoString(context: Context, genre: Genre): String =
        getSongCountString(context, genre.songCount)

    fun getPlaylistInfoString(context: Context, songs: List<Song>): String {
        val duration = getTotalDuration(context, songs)
        return buildInfoString(
            getSongCountString(context, songs.size),
            getReadableDurationString(duration)
        )
    }

    fun getSongCountString(context: Context, songCount: Int): String =
        "$songCount ${if (songCount == 1) context.resources.getString(R.string.song) else context.resources.getString(R.string.songs)}"

    fun getAlbumCountString(context: Context, albumCount: Int): String {
        val albumString = if (albumCount == 1) context.resources.getString(R.string.album) else context.resources.getString(R.string.albums)
        return "$albumCount $albumString"
    }

    fun getYearString(year: Int): String = if (year > 0) year.toString() else "-"

    fun getTotalDuration(context: Context, songs: List<Song>): Long =
        songs.fold(0L) { acc: Long, song: Song -> acc + song.duration }

    fun getReadableDurationString(songDurationMillis: Long): String {
        var minutes = songDurationMillis / 1000 / 60
        val seconds = songDurationMillis / 1000 % 60
        return if (minutes < 60) {
            String.format(Locale.getDefault(), "%01d:%02d", minutes, seconds)
        } else {
            val hours = minutes / 60
            minutes %= 60
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        }
    }

    /**
     * Build a concatenated string from the provided arguments
     * The intended purpose is to show extra annotations
     * to a music library item.
     * Ex: for a given album --> buildInfoString(album.artist, album.songCount)
     */
    fun buildInfoString(string1: String?, string2: String?): String =
        when {
            string1.isNullOrEmpty() && !string2.isNullOrEmpty() -> string2
            !string1.isNullOrEmpty() && string2.isNullOrEmpty() -> string1
            !string1.isNullOrEmpty() && !string2.isNullOrEmpty() -> "$string1  •  $string2"
            else -> ""
        }

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
