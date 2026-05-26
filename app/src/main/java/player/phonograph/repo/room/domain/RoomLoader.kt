/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.domain

import player.phonograph.model.sort.SortMode
import player.phonograph.repo.room.MusicDatabase
import player.phonograph.settings.Keys
import player.phonograph.settings.Settings
import android.content.Context

sealed class RoomLoader {
    protected val db get() = MusicDatabase.koinInstance

    protected fun songSortMode(context: Context): SortMode = Settings(context)[Keys.songSortMode].data

    protected fun albumSortMode(context: Context): SortMode = Settings(context)[Keys.albumSortMode].data

    protected fun artistSortMode(context: Context): SortMode = Settings(context)[Keys.artistSortMode].data

    protected fun genreSortMode(context: Context): SortMode = Settings(context)[Keys.genreSortMode].data
}
