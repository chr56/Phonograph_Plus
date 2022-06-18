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
import player.phonograph.R
import player.phonograph.databinding.PopupWindowMainBinding
import player.phonograph.util.PhonographColorUtil
import util.mdcolor.pref.ThemeColor

class ListOptionsPopup private constructor(
    private val context: Context,
    val viewBinding: PopupWindowMainBinding,
    width: Int,
    height: Int,
) : PopupWindow(viewBinding.root, width, height, true) {

    var onDismiss: (PopupWindowMainBinding) -> Unit = { }
    var onShow: (PopupWindowMainBinding) -> Unit = { }

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
        onDismiss(viewBinding)
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
        onShow(viewBinding)
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

    var sortMethod: Boolean
        get() =
            when (viewBinding.groupSortOrderMethod.checkedRadioButtonId) {
                R.id.sort_method_a_z -> true
                R.id.sort_method_z_a -> false
                else -> true
            }
        set(value) {
            viewBinding.apply {
                groupSortOrderMethod.visibility = VISIBLE
                titleSortOrderMethod.visibility = VISIBLE
                groupSortOrderMethod.clearCheck()
                check(
                    if (value) sortMethodAZ
                    else sortMethodZA
                )
            }
        }

    var sortRef: Ref
        get() =
            when (viewBinding.groupSortOrderRef.checkedRadioButtonId) {
                R.id.sort_order_song -> Ref.SONG
                R.id.sort_order_album -> Ref.ALBUM
                R.id.sort_order_artist -> Ref.ARTIST
                R.id.sort_order_year -> Ref.YEAR
                R.id.sort_order_date_added -> Ref.DADE_ADDED
                R.id.sort_order_date_modified -> Ref.DATE_MODIFIED
                R.id.sort_order_duration -> Ref.DURATION
                R.id.sort_order_name_plain -> Ref.NAME_PLAIN
                R.id.sort_order_song_count -> Ref.SONG_COUNT
                R.id.sort_order_album_count -> Ref.ALBUM_COUNT
                R.id.sort_order_size -> Ref.SIZE
                else -> { Ref.NA }
            }
        set(ref) {
            viewBinding.apply {
                titleSortOrderRef.visibility = VISIBLE
                titleSortOrderRef.visibility = VISIBLE
                groupSortOrderRef.clearCheck()
                check(
                    when (ref) {
                        Ref.SONG -> sortOrderSong
                        Ref.ALBUM -> sortOrderAlbum
                        Ref.ARTIST -> sortOrderArtist
                        Ref.YEAR -> sortOrderYear
                        Ref.DADE_ADDED -> sortOrderDateAdded
                        Ref.DATE_MODIFIED -> sortOrderDateModified
                        Ref.DURATION -> sortOrderDuration
                        Ref.NAME_PLAIN -> sortOrderNamePlain
                        Ref.SONG_COUNT -> sortOrderSongCount
                        Ref.ALBUM_COUNT -> sortOrderAlbumCount
                        Ref.SIZE -> sortOrderSize
                        Ref.NA -> {
                            null
                        }
                    }
                )
            }
        }

    enum class Ref {
        NA, SONG, ALBUM, ARTIST, YEAR, DADE_ADDED, DATE_MODIFIED, DURATION, NAME_PLAIN, SONG_COUNT, ALBUM_COUNT, SIZE,
    }

    private fun check(radioButton: RadioButton?) {
        radioButton?.isChecked = true
    }
    private fun RadioButton.toggleOn() { isChecked = true }
}
