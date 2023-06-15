/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.mediastore

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import player.phonograph.App
import player.phonograph.model.sort.SortRef
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.settings.Setting

@SuppressLint("Recycle")
object GenreLoader {

    fun all(context: Context): List<Genre> {
        return getGenresFromCursor(context, makeGenreCursor(context)).sortAll()
    }

    fun getSongs(context: Context, genreId: Long): List<Song> {
        return makeSongCursor(context, genreId).getSongs()
    }

    private fun getGenresFromCursor(context: Context, cursor: Cursor?): List<Genre> {
        if (cursor == null) return ArrayList()

        val genres: MutableList<Genre> = ArrayList()

        cursor.use {
            if (cursor.moveToFirst()) {
                do {
                    val genre = getGenreFromCursor(context, cursor)
                    if (genre.songCount > 0) {
                        genres.add(genre)
                    } else {
                        removeEmptyGenre(context, genre)
                    }
                } while (cursor.moveToNext())
            }
        }
        return genres
    }

    private fun getGenreFromCursor(context: Context, cursor: Cursor): Genre {
        val id = cursor.getLong(0)
        val name = cursor.getString(1)
        val songCount = getSongs(context, id).size
        return Genre(id, name, songCount)
    }

    private fun makeSongCursor(context: Context, genreId: Long): Cursor? {
        return try {
            context.contentResolver.query(
                MediaStore.Audio.Genres.Members.getContentUri("external", genreId),
                BASE_SONG_PROJECTION,
                BASE_AUDIO_SELECTION,
                null, null
            )
        } catch (e: SecurityException) {
            null
        }
    }

    private fun makeGenreCursor(context: Context): Cursor? {
        val projection = arrayOf(
            MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME
        )
        return try {
            context.contentResolver.query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                projection, null, null, null
            )
        } catch (e: SecurityException) {
            null
        }
    }

    private fun removeEmptyGenre(context: Context, genre: Genre) {
        // try to remove the empty genre from the media store
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            try {
                context.contentResolver.delete(
                    MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                    MediaStore.Audio.Genres._ID + " == " + genre.id,
                    null
                )
            } catch (e: Exception) {
                e.printStackTrace()
                // nothing we can do then
            }
        }
    }

    fun List<Genre>.allGenreSongs(): List<Song> =
        this.flatMap { getSongs(App.instance, it.id) }

    private fun List<Genre>.sortAll(): List<Genre> {
        val revert = Setting.instance.genreSortMode.revert
        return when (Setting.instance.genreSortMode.sortRef) {
            SortRef.DISPLAY_NAME -> this.sort(revert) { it.name }
            SortRef.SONG_COUNT -> this.sort(revert) { it.songCount }
            else -> this
        }
    }

    private inline fun List<Genre>.sort(
        revert: Boolean,
        crossinline selector: (Genre) -> Comparable<*>?,
    ): List<Genre> {
        return if (revert) this.sortedWith(compareByDescending(selector))
        else this.sortedWith(compareBy(selector))
    }
}
