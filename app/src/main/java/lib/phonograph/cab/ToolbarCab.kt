/*
 * Copyright (c) 2022 chr_56 & Aidan Follestad (@afollestad) (original designer/author)
 */

@file:Suppress("unused")

package lib.phonograph.cab

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.Menu
import android.view.View
import android.view.ViewStub
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.iterator
import player.phonograph.R
import player.phonograph.util.ImageUtil.getTintedDrawable

fun createToolbarCab(
    activity: Activity,
    @IdRes stubId: Int,
    @IdRes inflatedId: Int,
    cfg: CabCfg = {},
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
typealias CabCfg = ToolbarCab.() -> Unit

class ToolbarCab internal constructor(
    val activity: Activity,
    val toolbar: Toolbar,
    applyCfg: CabCfg,
) {

    var status: CabStatus = CabStatus.STATUS_INACTIVE // default
        private set

    init {
        toolbar.run {
            translationY = 0f
            alpha = 1f

            setBackgroundColor(backgroundColor)

            navigationIcon = this@ToolbarCab.navigationIcon
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
        if (menuHandler != null) {
            menuHandler!!.invoke(this)
            // tint
            overflowIcon = activity.getTintedDrawable(androidx.appcompat.R.drawable.abc_ic_menu_overflow_material, titleTextColor)
            for (item in menu) {
                item.icon = item.icon?.apply { setTint(titleTextColor) }
            }
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

    /**
     * handle menu creating & callbacks
     */
    var menuHandler: ((Toolbar) -> Boolean)? = null
        set(value) {
            field = value
            setUpMenu()
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
        status = CabStatus.STATUS_ACTIVE
    }

    @Synchronized
    fun hide() = toolbar.run {
        visibility = View.INVISIBLE
        status = CabStatus.STATUS_INACTIVE
    }

    val menu: Menu? get() = toolbar.menu
}

@Suppress("ClassName")
sealed class CabStatus {
    object STATUS_INACTIVE : CabStatus()
    object STATUS_ACTIVE : CabStatus()
}
