/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.main.pages

import player.phonograph.R
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.model.ui.ItemLayoutStyle
import player.phonograph.settings.Keys
import player.phonograph.settings.Settings
import player.phonograph.util.ui.isLandscape
import android.content.Context
import android.content.res.Resources

// todo valid input
sealed class PageDisplayConfig(context: Context) {

    protected val res: Resources = context.resources
    protected val isLandscape: Boolean get() = isLandscape(res)

    abstract var sortMode: SortMode
    abstract val availableSortRefs: Array<SortRef>

    abstract var layout: ItemLayoutStyle
    abstract val availableLayouts: Array<ItemLayoutStyle>

    abstract var gridSize: Int
    abstract val maxGridSize: Int

    abstract var colorFooter: Boolean

    open val allowColoredFooter: Boolean = true
    open val allowRevertSort: Boolean = true

    protected val settings = Settings(context)
}

sealed class ImagePageDisplayConfig(context: Context) : PageDisplayConfig(context) {
    override val availableLayouts: Array<ItemLayoutStyle>
        get() = arrayOf(
            ItemLayoutStyle.LIST,
            ItemLayoutStyle.LIST_EXTENDED,
            ItemLayoutStyle.LIST_NO_IMAGE,
            ItemLayoutStyle.LIST_3L,
            ItemLayoutStyle.LIST_3L_EXTENDED,
            ItemLayoutStyle.LIST_3L_NO_IMAGE,
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
        get() = if (isLandscape) settings[Keys.songItemLayoutLand].data else settings[Keys.songItemLayout].data
        set(value) {
            if (isLandscape) settings[Keys.songItemLayoutLand].data = value
            else settings[Keys.songItemLayout].data = value
        }
    override var gridSize: Int
        get() = if (isLandscape) settings[Keys.songGridSizeLand].data else settings[Keys.songGridSize].data
        set(value) {
            if (value <= 0) return
            if (isLandscape) settings[Keys.songGridSizeLand].data = value
            else settings[Keys.songGridSize].data = value
        }
    override var sortMode: SortMode
        get() = settings[Keys.songSortMode].data
        set(value) {
            settings[Keys.songSortMode].data = value
        }
    override var colorFooter: Boolean
        get() = settings[Keys.songColoredFooters].data
        set(value) {
            settings[Keys.songColoredFooters].data = value
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
        get() = if (isLandscape) settings[Keys.albumItemLayoutLand].data else settings[Keys.albumItemLayout].data
        set(value) {
            if (isLandscape) settings[Keys.albumItemLayoutLand].data = value
            else settings[Keys.albumItemLayout].data = value
        }
    override var gridSize: Int
        get() = if (isLandscape) settings[Keys.albumGridSizeLand].data else settings[Keys.albumGridSize].data
        set(value) {
            if (value <= 0) return
            if (isLandscape) settings[Keys.albumGridSizeLand].data = value
            else settings[Keys.albumGridSize].data = value
        }
    override var sortMode: SortMode
        get() = settings[Keys.albumSortMode].data
        set(value) {
            settings[Keys.albumSortMode].data = value
        }
    override var colorFooter: Boolean
        get() = settings[Keys.albumColoredFooters].data
        set(value) {
            settings[Keys.albumColoredFooters].data = value
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
        get() = if (isLandscape) settings[Keys.artistItemLayoutLand].data else settings[Keys.artistItemLayout].data
        set(value) {
            if (isLandscape) settings[Keys.artistItemLayoutLand].data = value
            else settings[Keys.artistItemLayout].data = value
        }
    override var gridSize: Int
        get() = if (isLandscape) settings[Keys.artistGridSizeLand].data else settings[Keys.artistGridSize].data
        set(value) {
            if (value <= 0) return
            if (isLandscape) settings[Keys.artistGridSizeLand].data = value
            else settings[Keys.artistGridSize].data = value
        }
    override var sortMode: SortMode
        get() = settings[Keys.artistSortMode].data
        set(value) {
            settings[Keys.artistSortMode].data = value
        }
    override var colorFooter: Boolean
        get() = settings[Keys.artistColoredFooters].data
        set(value) {
            settings[Keys.artistColoredFooters].data = value
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
        get() = if (isLandscape) settings[Keys.playlistGridSizeLand].data else settings[Keys.playlistGridSize].data
        set(value) {
            if (value <= 0) return
            if (isLandscape) settings[Keys.playlistGridSizeLand].data = value
            else settings[Keys.playlistGridSize].data = value
        }
    override var sortMode: SortMode
        get() = settings[Keys.playlistSortMode].data
        set(value) {
            settings[Keys.playlistSortMode].data = value
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
        get() = if (isLandscape) settings[Keys.genreGridSizeLand].data else settings[Keys.genreGridSize].data
        set(value) {
            if (value <= 0) return
            if (isLandscape) settings[Keys.genreGridSizeLand].data = value
            else settings[Keys.genreGridSize].data = value
        }
    override var sortMode: SortMode
        get() = settings[Keys.genreSortMode].data
        set(value) {
            settings[Keys.genreSortMode].data = value
        }
    override var colorFooter: Boolean = false
    override val allowColoredFooter: Boolean
        get() = false
}

class FolderPageDisplayConfig(context: Context) : PageDisplayConfig(context) {
    override val availableSortRefs: Array<SortRef> get() = arrayOf(SortRef.DISPLAY_NAME) //todo: support SortRef.ADDED_DATE, SortRef.MODIFIED_DATE, SortRef.SIZE)

    override val availableLayouts: Array<ItemLayoutStyle>
        get() = arrayOf(
            ItemLayoutStyle.LIST,
            ItemLayoutStyle.LIST_EXTENDED,
            ItemLayoutStyle.LIST_3L,
            ItemLayoutStyle.LIST_3L_EXTENDED,
        )
    override var layout: ItemLayoutStyle
        get() =
            if (isLandscape) {
                settings[Keys.folderItemLayoutLand].data
            } else {
                settings[Keys.folderItemLayout].data
            }
        set(value) {
            if (isLandscape) {
                settings[Keys.folderItemLayoutLand].data = value
            } else {
                settings[Keys.folderItemLayout].data = value
            }
        }

    override val maxGridSize: Int get() = if (isLandscape) 4 else 2
    override var gridSize: Int
        get() = if (isLandscape) settings[Keys.folderGridSizeLand].data else settings[Keys.folderGridSize].data
        set(value) {
            if (value <= 0) return
            if (isLandscape) settings[Keys.folderGridSizeLand].data = value
            else settings[Keys.folderGridSize].data = value
        }

    override var sortMode: SortMode
        get() = settings[Keys.collectionSortMode].data
        set(value) {
            settings[Keys.collectionSortMode].data = value
        }
    override var colorFooter: Boolean = false
    override val allowColoredFooter: Boolean get() = false

}