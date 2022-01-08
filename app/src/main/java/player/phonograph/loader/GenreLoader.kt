package player.phonograph.loader

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.util.MediaStoreUtil
import player.phonograph.util.PreferenceUtil.Companion.getInstance
import java.lang.Exception
import java.util.ArrayList

@SuppressLint("Recycle")
object GenreLoader {
    @JvmStatic
    fun getAllGenres(context: Context): List<Genre> {
        return getGenresFromCursor(context, makeGenreCursor(context))
    }

    fun getSongs(context: Context, genreId: Long): List<Song> {
        return MediaStoreUtil.getSongs(makeGenreSongCursor(context, genreId))
    }

    private fun getGenresFromCursor(context: Context, cursor: Cursor?): List<Genre> {
        val genres: MutableList<Genre> = ArrayList()
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val genre = getGenreFromCursor(context, cursor)
                    if (genre.songCount > 0) {
                        genres.add(genre)
                    } else {
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
                } while (cursor.moveToNext())
            }
            cursor.close()
        }
        return genres
    }

    private fun getGenreFromCursor(context: Context, cursor: Cursor): Genre {
        val id = cursor.getLong(0)
        val name = cursor.getString(1)
        val songs = getSongs(context, id).size
        return Genre(id, name, songs)
    }

    private fun makeGenreSongCursor(context: Context, genreId: Long): Cursor? {
        return try {
            context.contentResolver.query(
                MediaStore.Audio.Genres.Members.getContentUri("external", genreId),
                MediaStoreUtil.SongConst.BASE_PROJECTION,
                MediaStoreUtil.SongConst.BASE_AUDIO_SELECTION,
                null,
                getInstance(context).songSortOrder
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
                projection, null, null, getInstance(context).genreSortOrder
            )
        } catch (e: SecurityException) {
            null
        }
    }
}
