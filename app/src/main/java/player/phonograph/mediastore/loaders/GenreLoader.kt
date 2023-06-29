/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mediastore.loaders

import player.phonograph.App
import player.phonograph.mediastore.internal.BASE_AUDIO_SELECTION
import player.phonograph.mediastore.internal.BASE_SONG_PROJECTION
import player.phonograph.mediastore.internal.intoSongs
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.model.sort.SortRef
import player.phonograph.settings.Setting
import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore

@SuppressLint("Recycle")
object GenreLoader {

    fun all(context: Context): List<Genre> =
        makeGenreCursor(context)?.getGenresFromCursor(context)?.sortAll() ?: emptyList()

    fun genreSongs(context: Context, genreId: Long): List<Song> {
        return makeSongCursor(context, genreId).intoSongs()
    }

    private fun Cursor.getGenresFromCursor(context: Context): List<Genre> = this.use {
        val genres = mutableListOf<Genre>()
        if (moveToFirst()) {
            do {
                val genre = this.extractGenre(context)
                if (genre.songCount > 0) {
                    genres.add(genre)
                } else {
                    removeEmptyGenre(context, genre)
                }
            } while (moveToNext())
        }
        genres
    }

    private fun Cursor.extractGenre(context: Context): Genre {
        val id = getLong(0)
        return Genre(id = id, name = getString(1), songCount = genreSongs(context, id).size)
    }

    private fun makeSongCursor(context: Context, genreId: Long): Cursor? =
        context.contentResolver.query(
            MediaStore.Audio.Genres.Members.getContentUri("external", genreId),
            BASE_SONG_PROJECTION,
            BASE_AUDIO_SELECTION,
            null,
            null
        )

    private fun makeGenreCursor(context: Context): Cursor? = context.contentResolver.query(
        MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI, arrayOf(
            MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME
        ), null, null, null
    )

    private fun removeEmptyGenre(context: Context, genre: Genre) {
        // try to remove the empty genre from the media store
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            try {
                context.contentResolver.delete(
                    MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI, MediaStore.Audio.Genres._ID + " == " + genre.id, null
                )
            } catch (e: Exception) {
                e.printStackTrace()
                // nothing we can do then
            }
        }
    }

    fun List<Genre>.allGenreSongs(): List<Song> = this.flatMap { genreSongs(App.instance, it.id) }

    private fun List<Genre>.sortAll(): List<Genre> {
        val revert = Setting.instance.genreSortMode.revert
        return when (Setting.instance.genreSortMode.sortRef) {
            SortRef.DISPLAY_NAME -> this.sort(revert) { it.name }
            SortRef.SONG_COUNT   -> this.sort(revert) { it.songCount }
            else                 -> this
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
