/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import player.phonograph.BaseApp
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef

class SortOrderSettings(context: Context) {
    private val mPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)
    private val editor: SharedPreferences.Editor get() = mPreferences.edit()

    // List-SortMode
    private var _songSortMode: String
        get() = readStringPref(SONG_SORT_MODE, SortMode(SortRef.ID, false).serialize())
        set(value) = writeStringPref(SONG_SORT_MODE, value)
    var songSortMode: SortMode
        get() = SortMode.deserialize(_songSortMode)
        set(value) {
            _songSortMode = value.serialize()
        }

    private var _albumSortMode: String
        get() = readStringPref(ALBUM_SORT_MODE, SortMode(SortRef.ID, false).serialize())
        set(value) = writeStringPref(ALBUM_SORT_MODE, value)
    var albumSortMode: SortMode
        get() = SortMode.deserialize(_albumSortMode)
        set(value) {
            _albumSortMode = value.serialize()
        }

    private var _artistSortMode: String
        get() = readStringPref(ARTIST_SORT_MODE, SortMode(SortRef.ID, false).serialize())
        set(value) = writeStringPref(ARTIST_SORT_MODE, value)
    var artistSortMode: SortMode
        get() = SortMode.deserialize(_artistSortMode)
        set(value) {
            _artistSortMode = value.serialize()
        }

    private var _genreSortMode: String
        get() = readStringPref(GENRE_SORT_MODE, SortMode(SortRef.ID, false).serialize())
        set(value) = writeStringPref(GENRE_SORT_MODE, value)
    var genreSortMode: SortMode
        get() = SortMode.deserialize(_genreSortMode)
        set(value) {
            _genreSortMode = value.serialize()
        }

    companion object {
        //
        // Singleton
        //
        private var singleton: SortOrderSettings? = null
        val instance: SortOrderSettings
            get() {
                if (singleton == null) singleton = SortOrderSettings(BaseApp.instance)
                return singleton!!
            }

        // List-SortMode
        private const val SONG_SORT_MODE = "song_sort_mode"
        private const val ALBUM_SORT_MODE = "album_sort_mode"
        private const val ARTIST_SORT_MODE = "artist_sort_mode"
        private const val GENRE_SORT_MODE = "genre_sort_mode"
        private const val FILE_SORT_MODE = "file_sort_mode"
    }

    private fun readStringPref(keyName: String, defaultValue: String): String =
        mPreferences.getString(keyName, defaultValue) ?: defaultValue

    private fun writeStringPref(keyName: String, newValue: String) =
        editor.putString(keyName, newValue).apply()
}
