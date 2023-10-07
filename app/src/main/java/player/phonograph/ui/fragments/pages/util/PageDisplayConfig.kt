/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.fragments.pages.util

import player.phonograph.R
import player.phonograph.model.sort.SortMode
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.adapter.ViewHolderType
import player.phonograph.ui.adapter.ViewHolderTypes
import player.phonograph.util.debug
import player.phonograph.util.ui.isLandscape
import android.content.Context
import android.content.res.Resources
import android.util.Log

sealed class PageDisplayConfig(context: Context) {

    protected val res: Resources = context.resources
    protected val isLandscape: Boolean get() = isLandscape(res)

    @ViewHolderType
    val layoutType: Int get() = layoutType(gridSize)

    @ViewHolderType
    fun layoutType(size: Int): Int =
        if (gridMode(size)) ViewHolderTypes.GRID else listLayoutType


    @ViewHolderType
    protected abstract val listLayoutType: Int


    fun gridMode(size: Int): Boolean = size > maxGridSizeForList
    protected abstract val maxGridSizeForList: Int

    val maxGridSize: Int get() = if (isLandscape) res.getInteger(R.integer.max_columns_land) else res.getInteger(R.integer.max_columns)


    // todo valid input
    abstract var gridSize: Int
    abstract var sortMode: SortMode
    abstract var colorFooter: Boolean

    /**
     * @return true if success
     */
    fun updateSortMode(mode: SortMode): Boolean =
        if (mode != sortMode) {
            debug { Log.d("DisplayConfig", "SortMode $sortMode -> $mode") }
            sortMode = mode
            true
        } else {
            false
        }

    internal val pref = DisplaySetting(context)
    protected val setting = Setting(context).Composites

}

sealed class ImagePageDisplayConfig(context: Context) : PageDisplayConfig(context) {

    override val listLayoutType: Int = ViewHolderTypes.LIST

    override val maxGridSizeForList: Int
        get() {
            return if (isLandscape) res.getInteger(R.integer.default_list_columns_land) else res.getInteger(R.integer.default_list_columns)
        }

}

class SongPageDisplayConfig(context: Context) : ImagePageDisplayConfig(context) {

    override var gridSize: Int
        get() = if (isLandscape) pref.songGridSizeLand else pref.songGridSize
        set(value) {
            if (value <= 0) return
            if (isLandscape) pref.songGridSizeLand = value
            else pref.songGridSize = value
        }
    override var sortMode: SortMode
        get() = setting[Keys.songSortMode].data
        set(value) {
            setting[Keys.songSortMode].data = value
        }
    override var colorFooter: Boolean
        get() = pref.songColoredFooters
        set(value) {
            pref.songColoredFooters = value
        }

}

class AlbumPageDisplayConfig(context: Context) : ImagePageDisplayConfig(context) {

    override var gridSize: Int
        get() = if (isLandscape) pref.albumGridSizeLand else pref.albumGridSize
        set(value) {
            if (value <= 0) return
            if (isLandscape) pref.albumGridSizeLand = value
            else pref.albumGridSize = value
        }
    override var sortMode: SortMode
        get() = setting[Keys.albumSortMode].data
        set(value) {
            setting[Keys.albumSortMode].data = value
        }
    override var colorFooter: Boolean
        get() = pref.albumColoredFooters
        set(value) {
            pref.albumColoredFooters = value
        }

}

class ArtistPageDisplayConfig(context: Context) : ImagePageDisplayConfig(context) {

    override var gridSize: Int
        get() = if (isLandscape) pref.artistGridSizeLand else pref.artistGridSize
        set(value) {
            if (value <= 0) return
            if (isLandscape) pref.artistGridSizeLand = value
            else pref.artistGridSize = value
        }
    override var sortMode: SortMode
        get() = setting[Keys.artistSortMode].data
        set(value) {
            setting[Keys.artistSortMode].data = value
        }
    override var colorFooter: Boolean
        get() = pref.artistColoredFooters
        set(value) {
            pref.artistColoredFooters = value
        }

}

class PlaylistPageDisplayConfig(context: Context) : PageDisplayConfig(context) {

    override val listLayoutType: Int = ViewHolderTypes.LIST_SINGLE_ROW

    override val maxGridSizeForList: Int = Int.MAX_VALUE

    override var gridSize: Int = 1
    override var sortMode: SortMode
        get() = setting[Keys.playlistSortMode].data
        set(value) {
            setting[Keys.playlistSortMode].data = value
        }
    override var colorFooter: Boolean = false
}

class GenrePageDisplayConfig(context: Context) : PageDisplayConfig(context) {

    override val listLayoutType: Int = ViewHolderTypes.LIST_NO_IMAGE

    override val maxGridSizeForList: Int = Int.MAX_VALUE

    override var gridSize: Int
        get() = if (isLandscape) pref.genreGridSizeLand else pref.genreGridSize
        set(value) {
            if (value <= 0) return
            if (isLandscape) pref.genreGridSizeLand = value
            else pref.genreGridSize = value
        }
    override var sortMode: SortMode
        get() = setting[Keys.genreSortMode].data
        set(value) {
            setting[Keys.genreSortMode].data = value
        }
    override var colorFooter: Boolean = false
}