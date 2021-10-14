package com.kabouzeid.gramophone.loader

import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.MediaStore.Audio.AudioColumns
import com.kabouzeid.gramophone.loader.SongLoader2.SongConst.BASE_PROJECTION
import com.kabouzeid.gramophone.loader.SongLoader2.SongConst.BASE_SELECTION
import com.kabouzeid.gramophone.model.Song
import com.kabouzeid.gramophone.provider.BlacklistStore
import com.kabouzeid.gramophone.util.PreferenceUtil
import java.util.ArrayList

object SongLoader2 {

    /**
     * Const values about MediaStore of Audio
     */
    object SongConst {
        const val BASE_SELECTION =
            "${AudioColumns.IS_MUSIC} =1 AND ${AudioColumns.TITLE} != '' "
        val BASE_PROJECTION = arrayOf(
            BaseColumns._ID, // 0
            AudioColumns.TITLE, // 1
            AudioColumns.TRACK, // 2
            AudioColumns.YEAR, // 3
            AudioColumns.DURATION, // 4
            AudioColumns.DATA, // 5
            AudioColumns.DATE_MODIFIED, // 6
            AudioColumns.ALBUM_ID, // 7
            AudioColumns.ALBUM, // 8
            AudioColumns.ARTIST_ID, // 9
            AudioColumns.ARTIST, // 10
        )
    }

    fun getAllSongs(context: Context): List<Song?> {
        val cursor = makeSongCursor(context, null, null)
        return getSongs(cursor)
    }

    fun getSongs(context: Context, query: String): List<Song> {
        val cursor = makeSongCursor(
            context, AudioColumns.TITLE + " LIKE ?", arrayOf("%$query%")
        )
        return getSongs(cursor)
    }
    fun getSongs(cursor: Cursor?): List<Song> {
        val songs: MutableList<Song> = ArrayList()
        if (cursor != null && cursor.moveToFirst()) {
            do {
                songs.add(getSongFromCursor(cursor))
            } while (cursor.moveToNext())
            cursor.close()
        }
        return songs
    }

    fun getSong(context: Context, queryId: Long): Song {
        val cursor =
            makeSongCursor(context, "${AudioColumns._ID} =? ", arrayOf(queryId.toString()))
        return getSong(cursor)
    }
    fun getSong(cursor: Cursor?): Song {
        if (cursor != null) {
            val song: Song =
                if (cursor.moveToFirst()) {
                    getSongFromCursor(cursor)
                } else {
                    Song.EMPTY_SONG
                }
            cursor.close()
            return song
        }
        return Song.EMPTY_SONG
    }

    private fun getSongFromCursor(cursor: Cursor): Song {
        val id = cursor.getLong(0)
        val title = cursor.getString(1)
        val trackNumber = cursor.getInt(2)
        val year = cursor.getInt(3)
        val duration = cursor.getLong(4)
        val data = cursor.getString(5)
        val dateModified = cursor.getLong(6)
        val albumId = cursor.getLong(7)
        val albumName = cursor.getString(8)
        val artistId = cursor.getLong(9)
        val artistName = cursor.getString(10)
        return Song(
            id, title, trackNumber, year, duration, data, dateModified, albumId, albumName, artistId, artistName
        )
    }

    fun makeSongCursor(
        context: Context,
        selection: String?,
        selectionValues: Array<String>?
    ): Cursor? {
        return makeSongCursor(
            context, selection, selectionValues, PreferenceUtil.getInstance(context).songSortOrder
        )
    }
    @Suppress("LocalVariableName")
    fun makeSongCursor(
        context: Context,
        selection: String?,
        selectionValues: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        var realSelection = BASE_SELECTION
        var realSelectionValues = selectionValues

        if (selection != null && realSelection.trim { it <= ' ' } != "")
            realSelection += "AND $selection "

        // Blacklist 
        val paths: List<String> = BlacklistStore.getInstance(context).paths
        if (paths.isNotEmpty()) {
            realSelection = appendBlacklistForSelection(realSelection, paths.size)
            realSelectionValues = appendBlacklistForSelectionValues(realSelectionValues, paths)
        }

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                BASE_PROJECTION, realSelection, realSelectionValues, sortOrder
            )
        } catch (e: SecurityException) {
        }

        return cursor
    }

    /**
     * append blacklist filter in (database query) "selection"
     *
     * use with [appendBlacklistForSelectionValues]
     * @param selection target
     * @param pathCount the number of blacklist entries
     */
    private fun appendBlacklistForSelection(selection: String?, pathCount: Int): String {
        var realSelection =
            if (selection != null && selection.trim { it <= ' ' } != "") "$selection AND "
            else "" // why you pass null selection

        realSelection += AudioColumns.DATA + " NOT LIKE ?"

        for (i in 0 until pathCount - 1) {
            realSelection += " AND " + AudioColumns.DATA + " NOT LIKE ?"
        }
        return realSelection
    }

    /**
     * append blacklist filter in (database query) "selectionValues"
     *
     * use with [appendBlacklistForSelection]
     * @param selectionValues target
     * @param paths the blacklist path would added to
     */
    private fun appendBlacklistForSelectionValues(
        selectionValues: Array<String>?,
        paths: List<String>
    ): Array<String> {
        val realSelectionValues: Array<String> =
            Array<String>((selectionValues?.size ?: 0) + paths.size) { index ->
                // Todo: Check 
                if (index < (selectionValues?.size ?: 0)) selectionValues?.get(index) ?: ""
                else paths[index - (selectionValues?.size ?: 0)] + "%"
            }
        return realSelectionValues
    }
}
