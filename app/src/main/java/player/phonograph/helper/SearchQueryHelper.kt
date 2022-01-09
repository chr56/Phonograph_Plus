package player.phonograph.helper

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import player.phonograph.database.mediastore.Converter
import player.phonograph.database.mediastore.MusicDatabase
import player.phonograph.model.Song
import player.phonograph.util.MediaStoreUtil
import java.util.*
import kotlin.collections.ArrayList
import player.phonograph.database.mediastore.Song as SongEntity

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
// todo test new database
object SearchQueryHelper {
    private const val TITLE_SELECTION = "lower(" + MediaStore.Audio.AudioColumns.TITLE + ") = ?"
    private const val ALBUM_SELECTION = "lower(" + MediaStore.Audio.AudioColumns.ALBUM + ") = ?"
    private const val ARTIST_SELECTION = "lower(" + MediaStore.Audio.AudioColumns.ARTIST + ") = ?"
    private const val AND = " AND "

    @Deprecated("use query()")
    fun getSongs(context: Context, extras: Bundle): List<Song?> {
        val query = extras.getString(SearchManager.QUERY, null)
        val artistName = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST, null)
        val albumName = extras.getString(MediaStore.EXTRA_MEDIA_ALBUM, null)
        val titleName = extras.getString(MediaStore.EXTRA_MEDIA_TITLE, null)

        var songs: List<Song> = ArrayList()

        if (artistName != null && albumName != null && titleName != null) {
            songs = MediaStoreUtil.getSongs(
                MediaStoreUtil.querySongs(
                    context,
                    ARTIST_SELECTION + AND + ALBUM_SELECTION + AND + TITLE_SELECTION,
                    arrayOf(
                        artistName.lowercase(Locale.getDefault()).trim { it <= ' ' },
                        albumName.lowercase(Locale.getDefault()).trim { it <= ' ' },
                        titleName.lowercase(Locale.getDefault()).trim { it <= ' ' }
                    )
                )
            )
        }
        if (songs.isNotEmpty()) {
            return songs
        }

        if (artistName != null && titleName != null) {
            songs = MediaStoreUtil.getSongs(
                MediaStoreUtil.querySongs(
                    context,
                    ARTIST_SELECTION + AND + TITLE_SELECTION,
                    arrayOf(
                        artistName.lowercase(Locale.getDefault()).trim { it <= ' ' },
                        titleName.lowercase(Locale.getDefault()).trim { it <= ' ' }
                    )
                )
            )
        }
        if (songs.isNotEmpty()) {
            return songs
        }

        if (albumName != null && titleName != null) {
            songs = MediaStoreUtil.getSongs(
                MediaStoreUtil.querySongs(
                    context,
                    ALBUM_SELECTION + AND + TITLE_SELECTION,
                    arrayOf(
                        albumName.lowercase(Locale.getDefault()).trim { it <= ' ' },
                        titleName.lowercase(Locale.getDefault()).trim { it <= ' ' }
                    )
                )
            )
        }
        if (songs.isNotEmpty()) {
            return songs
        }

        if (artistName != null) {
            songs = MediaStoreUtil.getSongs(
                MediaStoreUtil.querySongs(
                    context,
                    ARTIST_SELECTION,
                    arrayOf(artistName.lowercase(Locale.getDefault()).trim { it <= ' ' })
                )
            )
        }
        if (songs.isNotEmpty()) {
            return songs
        }

        if (albumName != null) {
            songs = MediaStoreUtil.getSongs(
                MediaStoreUtil.querySongs(
                    context,
                    ALBUM_SELECTION,
                    arrayOf(albumName.lowercase(Locale.getDefault()).trim { it <= ' ' })
                )
            )
        }
        if (songs.isNotEmpty()) {
            return songs
        }

        if (titleName != null) {
            songs = MediaStoreUtil.getSongs(
                MediaStoreUtil.querySongs(
                    context,
                    TITLE_SELECTION,
                    arrayOf(titleName.lowercase(Locale.getDefault()).trim { it <= ' ' })
                )
            )
        }
        if (songs.isNotEmpty()) {
            return songs
        }

        songs = MediaStoreUtil.getSongs(
            MediaStoreUtil.querySongs(
                context,
                ARTIST_SELECTION,
                arrayOf(query.lowercase(Locale.getDefault()).trim { it <= ' ' })
            )
        )
        if (songs.isNotEmpty()) {
            return songs
        }

        songs = MediaStoreUtil.getSongs(
            MediaStoreUtil.querySongs(
                context,
                ALBUM_SELECTION,
                arrayOf(query.lowercase(Locale.getDefault()).trim { it <= ' ' })
            )
        )
        if (songs.isNotEmpty()) {
            return songs
        }

        songs = MediaStoreUtil.getSongs(
            MediaStoreUtil.querySongs(
                context,
                TITLE_SELECTION,
                arrayOf(query.lowercase(Locale.getDefault()).trim { it <= ' ' })
            )
        )
        return if (songs.isNotEmpty()) {
            songs
        } else MediaStoreUtil.getSongs(context, query)
    }

    fun query(extras: Bundle): List<Song> {
        val query = extras.getString(SearchManager.QUERY, null)
        val titleName: String? = extras.getString(MediaStore.EXTRA_MEDIA_TITLE, null)
        val albumName: String? = extras.getString(MediaStore.EXTRA_MEDIA_ALBUM, null)
        val artistName: String? = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST, null)

        var songs: List<SongEntity> = MusicDatabase.songsDataBase
            .SongDao().findSong(
                title = titleName?.let { "%$titleName%" },
                album = albumName?.let { "%$albumName%" },
                artist = artistName?.let { "%$artistName%" },
            )
        if (songs.isNotEmpty()) return Converter.convertSong(songs)

        if (query == null) return ArrayList()

        if (query.isNotBlank())
            songs = MusicDatabase.songsDataBase.SongDao().findSong(
                title = "%$query%",
                album = "%$query%",
                artist = "%$query%",
            )

        return if (songs.isNotEmpty()) Converter.convertSong(songs) else ArrayList()
    }
}
