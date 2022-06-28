/*
 * Copyright (c) 2022 chr_56 & Aidan Follestad (@afollestad) (original designer/author)
 */

@file:Suppress("unused")

package lib.phonograph.cab

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.Menu
import android.view.View
import android.view.ViewStub
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.annotation.StyleRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.iterator
import player.phonograph.R
import player.phonograph.util.PhonographColorUtil
import util.mdcolor.pref.ThemeColor

fun createToolbarCab(
    activity: Activity,
    @IdRes stubId: Int,
    @IdRes inflatedId: Int,
    cfg: CabCfg2 = {}
): ToolbarCab {
    val toolbar: Toolbar =
        when (val stub = activity.findViewById<View>(stubId)) {
            is ViewStub -> {
                stub.inflatedId = inflatedId
                stub.layoutResource = R.layout.stub_toolbar
                stub.inflate() as Toolbar
            }
            else -> {
                throw IllegalStateException(
                    "Unable to attach to ${activity.resources.getResourceName(stubId)}, it's not a ViewStub"
                )
            }
        }
    toolbar.visibility = View.GONE
    return ToolbarCab(activity, toolbar, cfg)
}
typealias CabCfg2 = ToolbarCab.() -> Unit

class ToolbarCab internal constructor(
    val activity: Activity,
    val toolbar: Toolbar,
    applyCfg: CabCfg2
) {

    var status: ToolbarCabStatus2 = ToolbarCabStatus2.STATUS_INACTIVE // default
        private set

    init {
        toolbar.run {
            translationY = 0f
            alpha = 1f

            setBackgroundColor(backgroundColor)

            navigationIcon = navigationIcon
            setNavigationOnClickListener(closeClickListener)

            title = titleText
            setTitleTextColor(titleTextColor)

            subtitle = subtitleText
            setSubtitleTextColor(subtitleTextColor)

            popupTheme = popThemeRes
            setUpMenu()
        }
        applyCfg.invoke(this)
    }

    private fun setUpMenu() = toolbar.run {
        menu.clear()
        if (menuRes != 0) {
            inflateMenu(menuRes)
            overflowIcon =
                AppCompatResources.getDrawable(activity, androidx.appcompat.R.drawable.abc_ic_menu_overflow_material)?.apply { setTint(titleTextColor) }
            for (item in menu) {
                item.icon = item.icon?.apply { setTint(titleTextColor) }
            }
            setOnMenuItemClickListener(menuItemClickListener)
        } else {
            setOnMenuItemClickListener(null)
        }
    }

    @ColorInt
    var backgroundColor: Int = Color.GRAY
        set(value) {
            field = value
            toolbar.setBackgroundColor(value)
        }

    var titleText: CharSequence = ""
        set(value) {
            field = value
            toolbar.title = value
        }

    @ColorInt
    var titleTextColor: Int = Color.WHITE
        set(value) {
            field = value
            toolbar.setTitleTextColor(value)
        }

    var subtitleText: CharSequence = ""
        set(value) {
            field = value
            toolbar.subtitle = value
        }

    @ColorInt
    var subtitleTextColor: Int = titleTextColor
        set(value) {
            field = value
            toolbar.setSubtitleTextColor(value)
        }

    @StyleRes
    var popThemeRes: Int = R.style.ThemeOverlay_AppCompat_DayNight_ActionBar
        set(value) {
            field = value
            toolbar.popupTheme = value
        }

    var navigationIcon: Drawable =
        ContextCompat.getDrawable(activity, R.drawable.ic_close_white_24dp)!!.apply {
            setTint(titleTextColor)
        }
        set(value) {
                field = value
                toolbar.navigationIcon = value
            }

    @MenuRes
    var menuRes: Int = 0
        set(value) {
            field = value
            setUpMenu()
        }

    var menuItemClickListener = Toolbar.OnMenuItemClickListener {
        return@OnMenuItemClickListener false
    }
        set(value) {
            field = value
            toolbar.setOnMenuItemClickListener(value)
        }

    var closeClickListener = View.OnClickListener {
        return@OnClickListener
    }
        set(value) {
            field = value
            toolbar.setNavigationOnClickListener(value)
        }

    @Synchronized
    fun show() = toolbar.run {
        visibility = View.VISIBLE
        bringToFront()
        status = ToolbarCabStatus2.STATUS_ACTIVE
    }

    @Synchronized
    fun hide() = toolbar.run {
        visibility = View.INVISIBLE
        status = ToolbarCabStatus2.STATUS_INACTIVE
    }

    val menu: Menu? get() = toolbar.menu

    // todo
    /** call this function in cab's host OnDestroy **/
    @Synchronized
    fun destroy(): Boolean {
        if (status == ToolbarCabStatus2.STATUS_DESTROYED) return false
        status = ToolbarCabStatus2.STATUS_DESTROYING

        toolbar.visibility = View.GONE

        status = ToolbarCabStatus2.STATUS_DESTROYED
        return true
    }
}

@Suppress("ClassName")
sealed class ToolbarCabStatus2 { // todo

    object STATUS_INACTIVE : ToolbarCabStatus2()
    object STATUS_ACTIVE : ToolbarCabStatus2()

    object STATUS_DESTROYING : ToolbarCabStatus2()
    object STATUS_DESTROYED : ToolbarCabStatus2()
}

class MultiSelectionCabController(val cab: ToolbarCab) {
    fun showContent(context: Context, checkedListSize: Int, @MenuRes menuRes: Int): Boolean {
        return if (cab.status != ToolbarCabStatus2.STATUS_DESTROYED || cab.status != ToolbarCabStatus2.STATUS_DESTROYING) {
            if (checkedListSize < 1) cab.hide()

            cab.backgroundColor = PhonographColorUtil.shiftBackgroundColorForLightText(ThemeColor.primaryColor(context))
            cab.titleText = context.getString(R.string.x_selected, checkedListSize)
            cab.titleTextColor = ThemeColor.textColorPrimary(context)
            cab.menuRes = menuRes

            cab.show()
            true
        } else {
            false
        }
    }

    fun dismiss(): Boolean {
        if (cab.status == ToolbarCabStatus2.STATUS_ACTIVE) {
            cab.hide()
            return true
        }
        return false
    }

    fun distroy(): Boolean = cab.destroy()
}
