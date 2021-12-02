package player.phonograph.helper

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import player.phonograph.loader.SongLoader
import player.phonograph.model.Song
import java.util.*

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object SearchQueryHelper {
    private const val TITLE_SELECTION = "lower(" + MediaStore.Audio.AudioColumns.TITLE + ") = ?"
    private const val ALBUM_SELECTION = "lower(" + MediaStore.Audio.AudioColumns.ALBUM + ") = ?"
    private const val ARTIST_SELECTION = "lower(" + MediaStore.Audio.AudioColumns.ARTIST + ") = ?"
    private const val AND = " AND "

    fun getSongs(context: Context, extras: Bundle): List<Song?> {
        val query = extras.getString(SearchManager.QUERY, null)
        val artistName = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST, null)
        val albumName = extras.getString(MediaStore.EXTRA_MEDIA_ALBUM, null)
        val titleName = extras.getString(MediaStore.EXTRA_MEDIA_TITLE, null)

        var songs: List<Song> = ArrayList()

        if (artistName != null && albumName != null && titleName != null) {
            songs = SongLoader.getSongs(
                SongLoader.makeSongCursor(
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
            songs = SongLoader.getSongs(
                SongLoader.makeSongCursor(
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
            songs = SongLoader.getSongs(
                SongLoader.makeSongCursor(
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
            songs = SongLoader.getSongs(
                SongLoader.makeSongCursor(
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
            songs = SongLoader.getSongs(
                SongLoader.makeSongCursor(
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
            songs = SongLoader.getSongs(
                SongLoader.makeSongCursor(
                    context,
                    TITLE_SELECTION,
                    arrayOf(titleName.lowercase(Locale.getDefault()).trim { it <= ' ' })
                )
            )
        }
        if (songs.isNotEmpty()) {
            return songs
        }

        songs = SongLoader.getSongs(
            SongLoader.makeSongCursor(
                context,
                ARTIST_SELECTION,
                arrayOf(query.lowercase(Locale.getDefault()).trim { it <= ' ' })
            )
        )
        if (songs.isNotEmpty()) {
            return songs
        }

        songs = SongLoader.getSongs(
            SongLoader.makeSongCursor(
                context,
                ALBUM_SELECTION,
                arrayOf(query.lowercase(Locale.getDefault()).trim { it <= ' ' })
            )
        )
        if (songs.isNotEmpty()) {
            return songs
        }

        songs = SongLoader.getSongs(
            SongLoader.makeSongCursor(
                context,
                TITLE_SELECTION,
                arrayOf(query.lowercase(Locale.getDefault()).trim { it <= ' ' })
            )
        )
        return if (songs.isNotEmpty()) {
            songs
        } else SongLoader.getSongs(context, query)
    }
}
