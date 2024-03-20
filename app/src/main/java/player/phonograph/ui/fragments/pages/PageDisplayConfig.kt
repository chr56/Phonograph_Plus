/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.fragments.pages

import player.phonograph.R
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.adapter.ItemLayoutStyle
import player.phonograph.util.debug
import player.phonograph.util.ui.isLandscape
import android.content.Context
import android.content.res.Resources
import android.util.Log

sealed class PageDisplayConfig(context: Context) {

    protected val res: Resources = context.resources
    protected val isLandscape: Boolean get() = isLandscape(res)

    // todo valid input
    abstract var sortMode: SortMode
    abstract val availableSortRefs: Array<SortRef>

    abstract var layout: ItemLayoutStyle
    abstract val availableLayouts: Array<ItemLayoutStyle>

    abstract var gridSize: Int
    abstract val maxGridSize: Int

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
    /**
     * @return true if success
     */
    fun updateItemLayout(itemLayoutStyle: ItemLayoutStyle): Boolean =
        if (itemLayoutStyle != layout) {
            debug { Log.d("DisplayConfig", "Layout $layout -> $itemLayoutStyle") }
            layout = itemLayoutStyle
            true
        } else {
            false
        }

    protected val setting = Setting(context)
}

sealed class ImagePageDisplayConfig(context: Context) : PageDisplayConfig(context) {
    override val availableLayouts: Array<ItemLayoutStyle>
        get() = arrayOf(
            ItemLayoutStyle.LIST,
            ItemLayoutStyle.LIST_EXTENDED,
            ItemLayoutStyle.LIST_3L,
            ItemLayoutStyle.LIST_3L_EXTENDED,
            ItemLayoutStyle.LIST_NO_IMAGE,
            ItemLayoutStyle.GRID,
        )
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

    override var layout: ItemLayoutStyle
        get() = if (isLandscape) setting.Composites[Keys.songItemLayoutLand].data else setting.Composites[Keys.songItemLayout].data
        set(value) {
            if (isLandscape) setting.Composites[Keys.songItemLayoutLand].data = value
            else setting.Composites[Keys.songItemLayout].data = value
        }
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

    override var layout: ItemLayoutStyle
        get() = if (isLandscape) setting.Composites[Keys.albumItemLayoutLand].data else setting.Composites[Keys.albumItemLayout].data
        set(value) {
            if (isLandscape) setting.Composites[Keys.albumItemLayoutLand].data = value
            else setting.Composites[Keys.albumItemLayout].data = value
        }
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

    override var layout: ItemLayoutStyle
        get() = if (isLandscape) setting.Composites[Keys.artistItemLayoutLand].data else setting.Composites[Keys.artistItemLayout].data
        set(value) {
            if (isLandscape) setting.Composites[Keys.artistItemLayoutLand].data = value
            else setting.Composites[Keys.artistItemLayout].data = value
        }
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

    override var layout: ItemLayoutStyle = ItemLayoutStyle.LIST_SINGLE_ROW
    override val availableLayouts: Array<ItemLayoutStyle> get() = arrayOf(ItemLayoutStyle.LIST_SINGLE_ROW)

    override val maxGridSize: Int get() = if (isLandscape) 4 else 2

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

    override var layout: ItemLayoutStyle = ItemLayoutStyle.LIST_NO_IMAGE
    override val availableLayouts: Array<ItemLayoutStyle> get() = arrayOf(ItemLayoutStyle.LIST_NO_IMAGE)

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