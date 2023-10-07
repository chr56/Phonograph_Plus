/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.components.popup

import player.phonograph.R
import player.phonograph.databinding.PopupWindowMainBinding
import player.phonograph.model.sort.SortRef
import player.phonograph.ui.fragments.pages.util.PageDisplayConfig
import player.phonograph.util.ui.isLandscape
import androidx.annotation.IdRes
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.core.view.iterator
import android.content.Context
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.RadioButton

class ListOptionsPopup private constructor(
    val viewBinding: PopupWindowMainBinding,
    width: Int,
    height: Int,
) : OptionsPopup(viewBinding, width, height) {

    var onDismiss: (ListOptionsPopup) -> Unit = { }
    var onShow: (ListOptionsPopup) -> Unit = { }

    constructor(
        context: Context,
        width: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
        height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    ) : this(PopupWindowMainBinding.inflate(LayoutInflater.from(context)), width, height)

    override fun onShow() {
        super.onShow()
        hideAllPopupItems()
        updateColor()
        onShow(this)
    }

    override fun dismiss() {
        super.dismiss()
        onDismiss(this)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun hideAllPopupItems() {
        with(viewBinding) {
            groupSortOrderMethod.visibility = GONE
            groupSortOrderMethod.clearCheck()
            titleSortOrderMethod.visibility = GONE

            groupSortOrderRef.visibility = GONE
            groupSortOrderRef.clearCheck()
            titleSortOrderRef.visibility = GONE

            groupGridSize.visibility = GONE
            groupGridSize.clearCheck()
            titleGridSize.visibility = GONE

            actionColoredFooters.visibility = GONE
        }
    }

    fun setup(displayConfig: PageDisplayConfig) {
        viewBinding.titleGridSize.text =
            if (isLandscape(resources)) {
                resources.getText(R.string.action_grid_size_land)
            } else {
                resources.getText(R.string.action_grid_size)
            }
        maxGridSize = displayConfig.maxGridSize
        gridSize = displayConfig.gridSize

        colorFooterVisibility = displayConfig.allowColoredFooter
        if (displayConfig.allowColoredFooter) {
            colorFooterEnability = displayConfig.gridMode(displayConfig.gridSize) // available in grid mode
            colorFooter = displayConfig.colorFooter
        }

        if (displayConfig.availableSortRefs.isNotEmpty()) {

            val currentSortMode = displayConfig.sortMode

            sortRef = currentSortMode.sortRef
            sortRefAvailable = displayConfig.availableSortRefs

            allowRevert = displayConfig.allowRevertSort
            revert = currentSortMode.revert
        }
    }

    /**
     * widget color
     */
    private fun updateColor() {
        // color
        prepareColors(contentView.context)
        with(viewBinding) {
            // text color
            this.titleGridSize.setTextColor(accentColor)
            this.titleSortOrderMethod.setTextColor(accentColor)
            this.titleSortOrderRef.setTextColor(accentColor)
            // checkbox color
            this.actionColoredFooters.buttonTintList = widgetColor
            this.useLegacyListFiles.buttonTintList = widgetColor
            this.showFileImagines.buttonTintList = widgetColor
            // radioButton
            for (i in 0 until this.groupGridSize.childCount)
                (this.groupGridSize.getChildAt(i) as RadioButton).buttonTintList = widgetColor
            for (i in 0 until this.groupSortOrderRef.childCount)
                (this.groupSortOrderRef.getChildAt(i) as RadioButton).buttonTintList = widgetColor
            for (i in 0 until this.groupSortOrderMethod.childCount)
                (this.groupSortOrderMethod.getChildAt(i) as RadioButton).buttonTintList = widgetColor
        }
    }

    var revert: Boolean
        get() =
            when (viewBinding.groupSortOrderMethod.checkedRadioButtonId) {
                R.id.sort_method_a_z -> false
                R.id.sort_method_z_a -> true
                else                 -> true
            }
        set(value) {
            viewBinding.apply {
                groupSortOrderMethod.visibility = VISIBLE
                titleSortOrderMethod.visibility = VISIBLE
                groupSortOrderMethod.clearCheck()
                check(
                    if (value) sortMethodZA
                    else sortMethodAZ
                )
            }
        }
    var allowRevert: Boolean
        get() = viewBinding.sortMethodAZ.visibility == VISIBLE && viewBinding.sortMethodZA.visibility == VISIBLE
        set(value) {
            with(viewBinding) {
                if (value) {
                    titleSortOrderMethod.visibility = VISIBLE
                    sortMethodAZ.visibility = VISIBLE
                    sortMethodZA.visibility = VISIBLE
                } else {
                    titleSortOrderMethod.visibility = GONE
                    sortMethodAZ.visibility = GONE
                    sortMethodZA.visibility = GONE
                }
            }
        }

    var sortRef: SortRef
        get() = getSortOrderById(viewBinding.groupSortOrderRef.checkedRadioButtonId)
        set(ref) {
            viewBinding.apply {
                titleSortOrderRef.visibility = VISIBLE
                titleSortOrderRef.visibility = VISIBLE
                groupSortOrderRef.clearCheck()
                check(
                    findSortOrderButton(ref)
                )
            }
        }

    var sortRefAvailable: Array<SortRef> = emptyArray()
        set(value) {
            field = value
            if (value.isNotEmpty()) {
                viewBinding.groupSortOrderRef.visibility = VISIBLE // container
                viewBinding.titleSortOrderRef.visibility = VISIBLE // title
            }
            for (v in viewBinding.groupSortOrderRef.iterator()) v.visibility = GONE // hide all
            for (ref in value) findSortOrderButton(ref)?.visibility = VISIBLE // show selected
        }

    var gridSize: Int
        get() {
            var gridSizeSelected = 0
            for (i in 0 until maxGridSize) {
                if ((viewBinding.groupGridSize.getChildAt(i) as RadioButton).isChecked) {
                    gridSizeSelected = i + 1
                    break
                }
            }
            return gridSizeSelected
        }
        set(value) {
            (viewBinding.groupGridSize[value - 1] as RadioButton).isChecked = true
        }

    var maxGridSize: Int
        get() {
            var index = -1
            viewBinding.groupGridSize.forEach { view ->
                index++
                if ((view as RadioButton).visibility == GONE) return index
            }
            return index
        }
        set(max) {
            val visibility = if (max <= 0) GONE else VISIBLE
            viewBinding.groupGridSize.visibility = visibility
            viewBinding.titleGridSize.visibility = visibility
            viewBinding.groupGridSize.clearCheck()
            if (max > 0) {
                for (i in 0 until max) viewBinding.groupGridSize.getChildAt(i).visibility = VISIBLE
            }
        }

    var colorFooter: Boolean
        get() = viewBinding.actionColoredFooters.isChecked
        set(value) {
            viewBinding.actionColoredFooters.isChecked = value
        }

    var colorFooterVisibility: Boolean
        get() = viewBinding.actionColoredFooters.visibility == VISIBLE
        set(value) {
            viewBinding.actionColoredFooters.visibility = if (value) VISIBLE else GONE
        }
    var colorFooterEnability: Boolean
        get() = viewBinding.actionColoredFooters.isEnabled
        set(value) {
            viewBinding.actionColoredFooters.isEnabled = value
        }

    var useLegacyListFiles: Boolean
        get() = viewBinding.useLegacyListFiles.isChecked
        set(value) {
            viewBinding.useLegacyListFiles.isChecked = value
        }

    var showFilesImages: Boolean
        get() = viewBinding.showFileImagines.isChecked
        set(value) {
            viewBinding.showFileImagines.isChecked = value
        }

    var showFileOption: Boolean
        get() = viewBinding.useLegacyListFiles.visibility == VISIBLE
        set(value) {
            viewBinding.useLegacyListFiles.visibility = if (value) VISIBLE else GONE
            viewBinding.showFileImagines.visibility = if (value) VISIBLE else GONE
        }
    /*
    * Utils
    */

    private fun getSortOrderById(@IdRes id: Int): SortRef =
        when (id) {
            R.id.sort_order_song          -> SortRef.SONG_NAME
            R.id.sort_order_album         -> SortRef.ALBUM_NAME
            R.id.sort_order_artist        -> SortRef.ARTIST_NAME
            R.id.sort_order_album_artist  -> SortRef.ALBUM_ARTIST_NAME
            R.id.sort_order_composer      -> SortRef.COMPOSER
            R.id.sort_order_year          -> SortRef.YEAR
            R.id.sort_order_date_added    -> SortRef.ADDED_DATE
            R.id.sort_order_date_modified -> SortRef.MODIFIED_DATE
            R.id.sort_order_duration      -> SortRef.DURATION
            R.id.sort_order_name_plain    -> SortRef.DISPLAY_NAME
            R.id.sort_order_song_count    -> SortRef.SONG_COUNT
            R.id.sort_order_album_count   -> SortRef.ALBUM_COUNT
            R.id.sort_order_size          -> SortRef.SIZE
            R.id.sort_order_path          -> SortRef.PATH
            else                          -> SortRef.ID
        }

    private fun findSortOrderButton(ref: SortRef): RadioButton? =
        when (ref) {
            SortRef.SONG_NAME         -> viewBinding.sortOrderSong
            SortRef.ALBUM_NAME        -> viewBinding.sortOrderAlbum
            SortRef.ARTIST_NAME       -> viewBinding.sortOrderArtist
            SortRef.ALBUM_ARTIST_NAME -> viewBinding.sortOrderAlbumArtist
            SortRef.COMPOSER          -> viewBinding.sortOrderComposer
            SortRef.YEAR              -> viewBinding.sortOrderYear
            SortRef.ADDED_DATE        -> viewBinding.sortOrderDateAdded
            SortRef.MODIFIED_DATE     -> viewBinding.sortOrderDateModified
            SortRef.DURATION          -> viewBinding.sortOrderDuration
            SortRef.DISPLAY_NAME      -> viewBinding.sortOrderNamePlain
            SortRef.SONG_COUNT        -> viewBinding.sortOrderSongCount
            SortRef.ALBUM_COUNT       -> viewBinding.sortOrderAlbumCount
            SortRef.SIZE              -> viewBinding.sortOrderSize
            SortRef.PATH              -> viewBinding.sortOrderPath
            else                      -> null
        }

    private fun check(radioButton: RadioButton?) {
        radioButton?.isChecked = true
    }
}
