/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.foundation.mediastore

import player.phonograph.model.Genre
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.BaseColumns
import android.provider.MediaStore.Audio.Genres

val BASE_GENRE_PROJECTION = arrayOf(
    Genres._ID, // 0
    Genres.NAME, // 1
)

/**
 * query genre via MediaStore
 */
fun queryGenre(
    context: Context,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
): Cursor? =
    context.contentResolver.query(
        mediastoreUriGenresExternal(),
        BASE_GENRE_PROJECTION,
        selection,
        selectionArgs,
        null
    )

/**
 * query genre songs via MediaStore
 */
fun queryGenreSongs(context: Context, genreId: Long): Cursor? =
    context.contentResolver.query(
        mediastoreUriGenreMembersExternal(genreId),
        BASE_SONG_PROJECTION,
        BASE_AUDIO_SELECTION,
        null,
        null
    )

/**
 * query genre for a song via MediaStore
 */
fun querySongGenre(context: Context, songId: Long): Cursor? =
    context.contentResolver.query(
        mediastoreUriGenreForSongExternal(songId),
        BASE_GENRE_PROJECTION,
        null,
        null,
        null
    )


/**
 * read cursor as [Genre]
 *
 * **Requirement:**
 * - [cursor] is queried from **[BASE_GENRE_PROJECTION]**
 * - [cursor] is **not empty**
 *
 */
fun readGenre(context: Context, cursor: Cursor): Genre? {
    val id = cursor.getLong(0)
    val name = cursor.getString(1)
    val count = queryGenreSongCount(context, id)
    if (count > 0) {
        return Genre(id = id, name = name, songCount = count)
    } else {
        removeEmptyGenre(context, id)
        return null
    }
}

private fun queryGenreSongCount(context: Context, genreId: Long): Int =
    context.contentResolver.query(
        mediastoreUriGenreMembersExternal(genreId),
        arrayOf(BaseColumns._ID),
        null,
        null,
        null
    )?.use { it.count } ?: 0

private fun removeEmptyGenre(context: Context, genreId: Long) {
    // try to remove the empty genre from the media store
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        try {
            context.contentResolver.delete(
                mediastoreUriGenresExternal(),
                "${Genres._ID} == $genreId",
                null
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * consume this cursor (read & close) and convert into a genre that at top of cursor
 */
fun Cursor?.intoFirstGenre(context: Context): Genre? =
    this?.use {
        if (moveToFirst()) readGenre(context, this) else null
    }

/**
 * consume this cursor (read & close) and convert into a genre list
 */
fun Cursor?.intoGenres(context: Context): List<Genre> {
    return this?.use {
        val genres = mutableListOf<Genre>()
        if (moveToFirst()) {
            do {
                val genre = readGenre(context, this)
                if (genre != null) genres.add(genre)
            } while (moveToNext())
        }
        genres
    } ?: emptyList()
}