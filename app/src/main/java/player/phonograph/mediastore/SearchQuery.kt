/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mediastore

import player.phonograph.mediastore.internal.intoSongs
import player.phonograph.mediastore.internal.querySongs
import player.phonograph.model.Song
import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import java.util.Locale

private const val TITLE_SELECTION = "lower(" + MediaStore.Audio.AudioColumns.TITLE + ") = ?"
private const val ALBUM_SELECTION = "lower(" + MediaStore.Audio.AudioColumns.ALBUM + ") = ?"
private const val ARTIST_SELECTION = "lower(" + MediaStore.Audio.AudioColumns.ARTIST + ") = ?"
private const val AND = " AND "

fun processQuery(context: Context, extras: Bundle): List<Song> {
    val query = extras.getString(SearchManager.QUERY, null)
    val artistName = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST, null)
    val albumName = extras.getString(MediaStore.EXTRA_MEDIA_ALBUM, null)
    val titleName = extras.getString(MediaStore.EXTRA_MEDIA_TITLE, null)

    var songs: List<Song> = ArrayList()

    if (artistName != null && albumName != null && titleName != null) {
        songs =
            querySongs(
                context,
                ARTIST_SELECTION + AND + ALBUM_SELECTION + AND + TITLE_SELECTION,
                arrayOf(
                    artistName.lowercase(Locale.getDefault()).trim { it <= ' ' },
                    albumName.lowercase(Locale.getDefault()).trim { it <= ' ' },
                    titleName.lowercase(Locale.getDefault()).trim { it <= ' ' }
                )
            ).intoSongs()
    }
    if (songs.isNotEmpty()) {
        return songs
    }

    if (artistName != null && titleName != null) {
        songs =
            querySongs(
                context,
                ARTIST_SELECTION + AND + TITLE_SELECTION,
                arrayOf(
                    artistName.lowercase(Locale.getDefault()).trim { it <= ' ' },
                    titleName.lowercase(Locale.getDefault()).trim { it <= ' ' }
                )
            ).intoSongs()
    }
    if (songs.isNotEmpty()) {
        return songs
    }

    if (albumName != null && titleName != null) {
        songs =
            querySongs(
                context,
                ALBUM_SELECTION + AND + TITLE_SELECTION,
                arrayOf(
                    albumName.lowercase(Locale.getDefault()).trim { it <= ' ' },
                    titleName.lowercase(Locale.getDefault()).trim { it <= ' ' }
                )
            ).intoSongs()
    }
    if (songs.isNotEmpty()) {
        return songs
    }

    if (artistName != null) {
        songs =
            querySongs(
                context,
                ARTIST_SELECTION,
                arrayOf(artistName.lowercase(Locale.getDefault()).trim { it <= ' ' })
            ).intoSongs()
    }
    if (songs.isNotEmpty()) {
        return songs
    }

    if (albumName != null) {
        songs =
            querySongs(
                context,
                ALBUM_SELECTION,
                arrayOf(albumName.lowercase(Locale.getDefault()).trim { it <= ' ' })
            ).intoSongs()
    }
    if (songs.isNotEmpty()) {
        return songs
    }

    if (titleName != null) {
        songs =
            querySongs(
                context,
                TITLE_SELECTION,
                arrayOf(titleName.lowercase(Locale.getDefault()).trim { it <= ' ' })
            ).intoSongs()
    }
    if (songs.isNotEmpty()) {
        return songs
    }

    songs =
        querySongs(
            context,
            ARTIST_SELECTION,
            arrayOf(query.lowercase(Locale.getDefault()).trim { it <= ' ' })
        ).intoSongs()
    if (songs.isNotEmpty()) {
        return songs
    }

    songs =
        querySongs(
            context,
            ALBUM_SELECTION,
            arrayOf(query.lowercase(Locale.getDefault()).trim { it <= ' ' })
        ).intoSongs()
    if (songs.isNotEmpty()) {
        return songs
    }

    songs =
        querySongs(
            context,
            TITLE_SELECTION,
            arrayOf(query.lowercase(Locale.getDefault()).trim { it <= ' ' })
        ).intoSongs()

    if (songs.isNotEmpty()) {
        return songs
    }

    return emptyList()
}
