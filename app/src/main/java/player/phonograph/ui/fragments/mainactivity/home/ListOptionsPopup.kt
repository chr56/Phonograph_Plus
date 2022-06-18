/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.mainactivity.home

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.RadioButton
import androidx.core.view.iterator
import player.phonograph.R
import player.phonograph.databinding.PopupWindowMainBinding
import player.phonograph.mediastore.sort.SortRef
import player.phonograph.util.PhonographColorUtil
import util.mdcolor.pref.ThemeColor

class ListOptionsPopup private constructor(
    private val context: Context,
    val viewBinding: PopupWindowMainBinding,
    width: Int,
    height: Int,
) : PopupWindow(viewBinding.root, width, height, true) {

    var onDismiss: (ListOptionsPopup) -> Unit = { }
    var onShow: (ListOptionsPopup) -> Unit = { }

    constructor(
        context: Context,
        width: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
        height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    ) : this(
        context,
        PopupWindowMainBinding.inflate(LayoutInflater.from(context)),
        width, height
    )

    init {
        // background
        setBackgroundDrawable(
            ColorDrawable(PhonographColorUtil.getCorrectBackgroundColor(context))
        )
        // animate
        this.animationStyle = android.R.style.Animation_Dialog
        setUpColor()
    }

    override fun dismiss() {
        super.dismiss()
        onDismiss(this)
    }

    override fun showAtLocation(parent: View?, gravity: Int, x: Int, y: Int) {
        super.showAtLocation(parent, gravity, x, y)
        onShow()
    }

    override fun showAsDropDown(anchor: View?, xoff: Int, yoff: Int, gravity: Int) {
        super.showAsDropDown(anchor, xoff, yoff, gravity)
        onShow()
    }

    private fun onShow() {
        hideAllPopupItems()
        onShow(this)
        setBackgroundDrawable(
            ColorDrawable(PhonographColorUtil.getCorrectBackgroundColor(context))
        )
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun hideAllPopupItems() {
        viewBinding.apply {
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

    /**
     * widget color
     */
    private fun setUpColor() {
        // color
        val accentColor = ThemeColor.accentColor(context)
        val textColor = ThemeColor.textColorSecondary(context)
        val widgetColor = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_enabled),
                intArrayOf(android.R.attr.state_selected),
                intArrayOf()
            ),
            intArrayOf(accentColor, accentColor, textColor)
        )
        viewBinding.apply {
            // text color
            this.titleGridSize.setTextColor(accentColor)
            this.titleSortOrderMethod.setTextColor(accentColor)
            this.titleSortOrderRef.setTextColor(accentColor)
            // checkbox color
            this.actionColoredFooters.buttonTintList = widgetColor
            // radioButton
            for (i in 0 until this.groupGridSize.childCount) (this.groupGridSize.getChildAt(i) as RadioButton).buttonTintList =
                widgetColor
            for (i in 0 until this.groupSortOrderRef.childCount) (this.groupSortOrderRef.getChildAt(i) as RadioButton).buttonTintList =
                widgetColor
            for (i in 0 until this.groupSortOrderMethod.childCount) (this.groupSortOrderMethod.getChildAt(i) as RadioButton).buttonTintList =
                widgetColor
        }
    }

    var revert: Boolean
        get() =
            when (viewBinding.groupSortOrderMethod.checkedRadioButtonId) {
                R.id.sort_method_a_z -> false
                R.id.sort_method_z_a -> true
                else -> true
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
                    sortMethodAZ.visibility = VISIBLE
                    sortMethodZA.visibility = VISIBLE
                } else {
                    sortMethodAZ.visibility = GONE
                    sortMethodZA.visibility = GONE
                }
            }
        }

    var sortRef: SortRef
        get() =
            when (viewBinding.groupSortOrderRef.checkedRadioButtonId) {
                R.id.sort_order_song -> SortRef.SONG_NAME
                R.id.sort_order_album -> SortRef.ALBUM_NAME
                R.id.sort_order_artist -> SortRef.ARTIST_NAME
                R.id.sort_order_year -> SortRef.YEAR
                R.id.sort_order_date_added -> SortRef.ADDED_DATE
                R.id.sort_order_date_modified -> SortRef.MODIFIED_DATE
                R.id.sort_order_duration -> SortRef.DURATION
                R.id.sort_order_name_plain -> SortRef.DISPLAY_NAME
                R.id.sort_order_song_count -> SortRef.SONG_COUNT
                R.id.sort_order_album_count -> SortRef.ALBUM_COUNT
                R.id.sort_order_size -> SortRef.SIZE
                else -> { SortRef.ID }
            }
        set(ref) {
            viewBinding.apply {
                titleSortOrderRef.visibility = VISIBLE
                titleSortOrderRef.visibility = VISIBLE
                groupSortOrderRef.clearCheck()
                check(
                    when (ref) {
                        SortRef.SONG_NAME -> sortOrderSong
                        SortRef.ALBUM_NAME -> sortOrderAlbum
                        SortRef.ARTIST_NAME -> sortOrderArtist
                        SortRef.YEAR -> sortOrderYear
                        SortRef.ADDED_DATE -> sortOrderDateAdded
                        SortRef.MODIFIED_DATE -> sortOrderDateModified
                        SortRef.DURATION -> sortOrderDuration
                        SortRef.DISPLAY_NAME -> sortOrderNamePlain
                        SortRef.SONG_COUNT -> sortOrderSongCount
                        SortRef.ALBUM_COUNT -> sortOrderAlbumCount
                        SortRef.SIZE -> sortOrderSize
                        else -> null
                    }
                )
            }
        }

    var sortRefAvailable: Array<SortRef> = emptyArray()
        set(value) {
            field = value
            if (value.isNotEmpty()) viewBinding.groupSortOrderRef.visibility = VISIBLE
            for (v in viewBinding.groupSortOrderRef.iterator()) v.visibility = GONE // hide all
            for (ref in value) with(viewBinding) {
                val v = when (ref) {
                    SortRef.SONG_NAME -> sortOrderSong
                    SortRef.ALBUM_NAME -> sortOrderAlbum
                    SortRef.ARTIST_NAME -> sortOrderArtist
                    SortRef.YEAR -> sortOrderYear
                    SortRef.ADDED_DATE -> sortOrderDateAdded
                    SortRef.MODIFIED_DATE -> sortOrderDateModified
                    SortRef.DURATION -> sortOrderDuration
                    SortRef.DISPLAY_NAME -> sortOrderNamePlain
                    SortRef.SONG_COUNT -> sortOrderSongCount
                    SortRef.ALBUM_COUNT -> sortOrderAlbumCount
                    SortRef.SIZE -> sortOrderSize
                    else -> return
                }
                v.visibility = VISIBLE
            }
        }

    private fun check(radioButton: RadioButton?) {
        radioButton?.isChecked = true
    }
    private fun RadioButton.toggleOn() { isChecked = true }
}
