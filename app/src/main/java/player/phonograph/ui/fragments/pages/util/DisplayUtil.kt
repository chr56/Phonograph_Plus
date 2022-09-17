/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.pages.util

import player.phonograph.App
import player.phonograph.R
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.settings.Setting
import player.phonograph.settings.SortOrderSettings
import player.phonograph.ui.fragments.pages.AbsDisplayPage
import player.phonograph.ui.fragments.pages.AlbumPage
import player.phonograph.ui.fragments.pages.ArtistPage
import player.phonograph.ui.fragments.pages.GenrePage
import player.phonograph.ui.fragments.pages.SongPage
import player.phonograph.util.Util

class DisplayUtil(private val page: AbsDisplayPage<*, *, *>) {
    private val isLandscape: Boolean
        get() = Util.isLandscape(page.resources)

    val maxGridSize: Int
        get() = if (isLandscape) App.instance.resources.getInteger(R.integer.max_columns_land) else
            App.instance.resources.getInteger(R.integer.max_columns)
    val maxGridSizeForList: Int
        get() = if (isLandscape) App.instance.resources.getInteger(R.integer.default_list_columns_land) else
            App.instance.resources.getInteger(R.integer.default_list_columns)

    var sortMode: SortMode
        get() {
            val pref = SortOrderSettings.instance
            return when (page) {
                is SongPage -> {
                    pref.songSortMode
                }
                is AlbumPage -> {
                    pref.albumSortMode
                }
                is ArtistPage -> {
                    pref.artistSortMode
                }
                is GenrePage -> {
                    pref.genreSortMode
                }
                else -> SortMode(SortRef.ID)
            }
        }
        set(value) {
            val pref = SortOrderSettings.instance
            when (page) {
                is SongPage -> {
                    pref.songSortMode = value
                }
                is AlbumPage -> {
                    pref.albumSortMode = value
                }
                is ArtistPage -> {
                    pref.artistSortMode = value
                }
                is GenrePage -> {
                    pref.genreSortMode = value
                }
                else -> {}
            }
        }

    var gridSize: Int
        get() {
            val pref = Setting.instance

            return when (page) {
                is SongPage -> {
                    if (isLandscape) pref.songGridSizeLand
                    else pref.songGridSize
                }
                is AlbumPage -> {
                    if (isLandscape) pref.albumGridSizeLand
                    else pref.albumGridSize
                }
                is ArtistPage -> {
                    if (isLandscape) pref.artistGridSizeLand
                    else pref.artistGridSize
                }
                is GenrePage -> {
                    if (isLandscape) pref.genreGridSizeLand
                    else pref.genreGridSize
                }
                else -> 1
            }
        }
        set(value) {
            if (value <= 0) return
            val pref = Setting.instance
            // todo valid input
            when (page) {
                is SongPage -> {
                    if (isLandscape) pref.songGridSizeLand = value
                    else pref.songGridSize = value
                }
                is AlbumPage -> {
                    if (isLandscape) pref.albumGridSizeLand = value
                    else pref.albumGridSize = value
                }
                is ArtistPage -> {
                    if (isLandscape) pref.artistGridSizeLand = value
                    else pref.artistGridSize = value
                }
                is GenrePage -> {
                    if (isLandscape) pref.genreGridSizeLand = value
                    else pref.genreGridSize = value
                }
                else -> false
            }
        }
    var colorFooter: Boolean
        get() {
            val pref = Setting.instance
            return when (page) {
                is SongPage -> {
                    pref.songColoredFooters
                }
                is AlbumPage -> {
                    pref.albumColoredFooters
                }
                is ArtistPage -> {
                    pref.artistColoredFooters
                }
                else -> false
            }
        }
        set(value) {
            val pref = Setting.instance
            // todo valid input
            when (page) {
                is SongPage -> {
                    pref.songColoredFooters = value
                }
                is AlbumPage -> {
                    pref.albumColoredFooters = value
                }
                is ArtistPage -> {
                    pref.artistColoredFooters = value
                }
                is GenrePage -> {
                    // do noting
                }
                else -> false
            }
        }
}