/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.fragments.pages.util

import player.phonograph.R
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.adapter.ViewHolderLayout
import player.phonograph.util.debug
import player.phonograph.util.ui.isLandscape
import android.content.Context
import android.content.res.Resources
import android.util.Log

sealed class PageDisplayConfig(context: Context) {

    protected val res: Resources = context.resources
    protected val isLandscape: Boolean get() = isLandscape(res)


    val layout: ViewHolderLayout get() = layout(gridSize)
    fun layout(size: Int): ViewHolderLayout =
        if (gridMode(size)) ViewHolderLayout.GRID else listLayout

    protected abstract val listLayout: ViewHolderLayout


    fun gridMode(size: Int): Boolean = size > maxGridSizeForList
    protected abstract val maxGridSizeForList: Int

    abstract val maxGridSize: Int

    abstract var gridSize: Int

    // todo valid input
    abstract var sortMode: SortMode
    abstract val availableSortRefs: Array<SortRef>

    abstract var colorFooter: Boolean

    open val allowColoredFooter: Boolean = true
    open val allowRevertSort: Boolean = true

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

    protected val setting = Setting(context)
}

sealed class ImagePageDisplayConfig(context: Context) : PageDisplayConfig(context) {

    override val listLayout: ViewHolderLayout = ViewHolderLayout.LIST

    override val maxGridSizeForList: Int
        get() = if (isLandscape) res.getInteger(R.integer.default_list_columns_land)
        else res.getInteger(R.integer.default_list_columns)

    override val maxGridSize: Int
        get() = if (isLandscape) res.getInteger(R.integer.max_columns_land)
        else res.getInteger(R.integer.max_columns)

}

class SongPageDisplayConfig(context: Context) : ImagePageDisplayConfig(context) {

    override val availableSortRefs: Array<SortRef>
        get() = arrayOf(
            SortRef.SONG_NAME,
            SortRef.ALBUM_NAME,
            SortRef.ARTIST_NAME,
            SortRef.ALBUM_ARTIST_NAME,
            SortRef.COMPOSER,
            SortRef.YEAR,
            SortRef.ADDED_DATE,
            SortRef.MODIFIED_DATE,
            SortRef.DURATION,
        )

    override var gridSize: Int
        get() = if (isLandscape) setting[Keys.songGridSizeLand].data else setting[Keys.songGridSize].data
        set(value) {
            if (value <= 0) return
            if (isLandscape) setting[Keys.songGridSizeLand].data = value
            else setting[Keys.songGridSize].data = value
        }
    override var sortMode: SortMode
        get() = setting.Composites[Keys.songSortMode].data
        set(value) {
            setting.Composites[Keys.songSortMode].data = value
        }
    override var colorFooter: Boolean
        get() = setting[Keys.songColoredFooters].data
        set(value) {
            setting[Keys.songColoredFooters].data = value
        }

}

class AlbumPageDisplayConfig(context: Context) : ImagePageDisplayConfig(context) {

    override val availableSortRefs: Array<SortRef>
        get() = arrayOf(
            SortRef.ALBUM_NAME,
            SortRef.ARTIST_NAME,
            SortRef.YEAR,
            SortRef.SONG_COUNT,
        )

    override var gridSize: Int
        get() = if (isLandscape) setting[Keys.albumGridSizeLand].data else setting[Keys.albumGridSize].data
        set(value) {
            if (value <= 0) return
            if (isLandscape) setting[Keys.albumGridSizeLand].data = value
            else setting[Keys.albumGridSize].data = value
        }
    override var sortMode: SortMode
        get() = setting.Composites[Keys.albumSortMode].data
        set(value) {
            setting.Composites[Keys.albumSortMode].data = value
        }
    override var colorFooter: Boolean
        get() = setting[Keys.albumColoredFooters].data
        set(value) {
            setting[Keys.albumColoredFooters].data = value
        }

}

class ArtistPageDisplayConfig(context: Context) : ImagePageDisplayConfig(context) {

    override val availableSortRefs: Array<SortRef>
        get() = arrayOf(
            SortRef.ARTIST_NAME,
            SortRef.ALBUM_COUNT,
            SortRef.SONG_COUNT,
        )

    override var gridSize: Int
        get() = if (isLandscape) setting[Keys.artistGridSizeLand].data else setting[Keys.artistGridSize].data
        set(value) {
            if (value <= 0) return
            if (isLandscape) setting[Keys.artistGridSizeLand].data = value
            else setting[Keys.artistGridSize].data = value
        }
    override var sortMode: SortMode
        get() = setting.Composites[Keys.artistSortMode].data
        set(value) {
            setting.Composites[Keys.artistSortMode].data = value
        }
    override var colorFooter: Boolean
        get() = setting[Keys.albumColoredFooters].data
        set(value) {
            setting[Keys.artistColoredFooters].data = value
        }

}

class PlaylistPageDisplayConfig(context: Context) : PageDisplayConfig(context) {

    override val availableSortRefs: Array<SortRef>
        get() = arrayOf(
            SortRef.DISPLAY_NAME,
            SortRef.PATH,
            SortRef.ADDED_DATE,
            SortRef.MODIFIED_DATE,
        )

    override val listLayout: ViewHolderLayout = ViewHolderLayout.LIST_SINGLE_ROW

    override val maxGridSize: Int get() = if (isLandscape) 4 else 2
    override val maxGridSizeForList: Int = Int.MAX_VALUE

    override var gridSize: Int
        get() = if (isLandscape) setting[Keys.playlistGridSizeLand].data else setting[Keys.playlistGridSize].data
        set(value) {
            if (value <= 0) return
            if (isLandscape) setting[Keys.playlistGridSizeLand].data = value
            else setting[Keys.playlistGridSize].data = value
        }
    override var sortMode: SortMode
        get() = setting.Composites[Keys.playlistSortMode].data
        set(value) {
            setting.Composites[Keys.playlistSortMode].data = value
        }
    override var colorFooter: Boolean = false
    override val allowColoredFooter: Boolean
        get() = false
}

class GenrePageDisplayConfig(context: Context) : PageDisplayConfig(context) {

    override val availableSortRefs: Array<SortRef>
        get() = arrayOf(
            SortRef.DISPLAY_NAME,
            SortRef.SONG_COUNT,
        )

    override val listLayout: ViewHolderLayout = ViewHolderLayout.LIST_NO_IMAGE

    override val maxGridSizeForList: Int = Int.MAX_VALUE

    override val maxGridSize: Int get() = if (isLandscape) 6 else 4

    override var gridSize: Int
        get() = if (isLandscape) setting[Keys.genreGridSizeLand].data else setting[Keys.genreGridSize].data
        set(value) {
            if (value <= 0) return
            if (isLandscape) setting[Keys.genreGridSizeLand].data = value
            else setting[Keys.genreGridSize].data = value
        }
    override var sortMode: SortMode
        get() = setting.Composites[Keys.genreSortMode].data
        set(value) {
            setting.Composites[Keys.genreSortMode].data = value
        }
    override var colorFooter: Boolean = false
    override val allowColoredFooter: Boolean
        get() = false
}