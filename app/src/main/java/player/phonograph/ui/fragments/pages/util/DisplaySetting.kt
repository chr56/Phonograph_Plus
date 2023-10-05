/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.fragments.pages.util

import player.phonograph.R
import player.phonograph.settings.dataStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import android.content.Context
import android.content.res.Resources
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

internal class DisplaySetting(context: Context) {

    private val store = context.dataStore
    private val resources: Resources = context.resources

    // List-Appearance
    var albumGridSize: Int by IntPref(
        ALBUM_GRID_SIZE,
        resources.getInteger(R.integer.default_grid_columns)
    )
    var songGridSize: Int by IntPref(
        SONG_GRID_SIZE,
        resources.getInteger(R.integer.default_list_columns)
    )
    var artistGridSize: Int by IntPref(
        ARTIST_GRID_SIZE,
        resources.getInteger(R.integer.default_list_columns)
    )
    var genreGridSize: Int by IntPref(
        GENRE_GRID_SIZE,
        resources.getInteger(R.integer.default_list_columns)
    )
    var albumGridSizeLand: Int by IntPref(
        ALBUM_GRID_SIZE_LAND,
        resources.getInteger(R.integer.default_grid_columns_land)
    )
    var songGridSizeLand: Int by IntPref(
        SONG_GRID_SIZE_LAND,
        resources.getInteger(R.integer.default_grid_columns_land)
    )
    var artistGridSizeLand: Int by IntPref(
        ARTIST_GRID_SIZE_LAND,
        resources.getInteger(R.integer.default_grid_columns_land)
    )
    var genreGridSizeLand: Int by IntPref(
        GENRE_GRID_SIZE_LAND,
        resources.getInteger(R.integer.default_grid_columns_land)
    )
    var albumColoredFooters by BooleanPref(ALBUM_COLORED_FOOTERS, true)
    var songColoredFooters by BooleanPref(SONG_COLORED_FOOTERS, true)
    var artistColoredFooters by BooleanPref(ARTIST_COLORED_FOOTERS, true)

    @Suppress("FunctionName", "SameParameterValue")
    private fun BooleanPref(key: String, defaultValue: Boolean) =
        Delegate(booleanPreferencesKey(key), defaultValue, store)

    @Suppress("FunctionName")
    private fun IntPref(key: String, defaultValue: Int) =
        Delegate(intPreferencesKey(key), defaultValue, store)

    private class Delegate<T>(
        private val key: Preferences.Key<T>,
        private val defaultValue: T,
        private val dataStore: DataStore<Preferences>,
    ) : ReadWriteProperty<Any?, T> {

        override fun getValue(thisRef: Any?, property: KProperty<*>): T = runBlocking {
            dataStore.data.first()[key] ?: defaultValue
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            runBlocking {
                dataStore.edit { mutablePreferences ->
                    mutablePreferences[key] = value
                }
            }
        }
    }

    companion object {
        // List Size
        private const val ALBUM_GRID_SIZE = "album_grid_size"
        private const val ALBUM_GRID_SIZE_LAND = "album_grid_size_land"
        private const val SONG_GRID_SIZE = "song_grid_size"
        private const val SONG_GRID_SIZE_LAND = "song_grid_size_land"
        private const val ARTIST_GRID_SIZE = "artist_grid_size"
        private const val ARTIST_GRID_SIZE_LAND = "artist_grid_size_land"
        private const val GENRE_GRID_SIZE = "genre_grid_size"
        private const val GENRE_GRID_SIZE_LAND = "genre_grid_size_land"

        // Colored Footers
        private const val ALBUM_COLORED_FOOTERS = "album_colored_footers"
        private const val SONG_COLORED_FOOTERS = "song_colored_footers"
        private const val ARTIST_COLORED_FOOTERS = "artist_colored_footers"
    }
}