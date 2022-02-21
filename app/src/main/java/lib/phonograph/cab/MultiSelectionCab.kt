/*
 * Copyright (c) 2022 chr_56 & Aidan Follestad (@afollestad) (original designer/author)
 */

@file:Suppress("unused")

package lib.phonograph.cab

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewStub
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.annotation.StyleRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import player.phonograph.R

fun createMultiSelectionCab(
    activity: Activity,
    @IdRes stubId: Int,
    @IdRes inflatedId: Int,
    applyCfg: CabCfg
): MultiSelectionCab {
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
    return MultiSelectionCab(activity, toolbar, applyCfg)
}

// todo leak check
class MultiSelectionCab internal constructor(
    private var activityField: Activity?,
    private var toolbarField: Toolbar?,
    var applyCfg: CabCfg?
) {

    private val activity: Activity
        get() = activityField ?: throw IllegalStateException("Cab has already destroyed or not created!")
    private val toolbar: Toolbar
        get() = toolbarField ?: throw IllegalStateException("Cab has already destroyed or not created!!")

    @Synchronized
    private fun init(applyCfg: CabCfg?) {
        status = CabStatus.STATUS_INITIATING
        initCallback.forEach { it.invoke(this) }
        applyCfg?.invoke(this)
        refresh()
        status = CabStatus.STATUS_AVAILABLE
    }

    fun show() {
        if (status != CabStatus.STATUS_AVAILABLE) init(applyCfg)
        toolbar.run {
            showCallbacks.forEach { it.invoke(this@MultiSelectionCab, menu) }
            visibility = View.VISIBLE
            bringToFront()
        }
        status = CabStatus.STATUS_ACTIVE
    }

    fun hide() = toolbar.run {
        visibility = View.INVISIBLE
        hideCallbacks.forEach {
            it.invoke(this@MultiSelectionCab)
        }
        status = CabStatus.STATUS_AVAILABLE
    }

    fun refresh() = toolbar.run {
        translationY = 0f
        alpha = 1f

        setBackgroundColor(backgroundColor)

        navigationIcon = closeDrawable
        setNavigationOnClickListener(closeClickListener)

        title = titleText
        setTitleTextColor(titleTextColor)

        subtitle = subtitleText
        setSubtitleTextColor(subtitleTextColor)

        popupTheme = themeRes

        menu.clear()
        if (menuRes != 0) {
            inflateMenu(menuRes)
            if (autoTintMenuIcon) {
                overflowIcon =
                    AppCompatResources.getDrawable(activity, androidx.appcompat.R.drawable.abc_ic_menu_overflow_material)?.let { it.setTint(titleTextColor); it }
            }
            setOnMenuItemClickListener(menuClickListener)
        } else {
            setOnMenuItemClickListener(null)
        }
    }

    var status: CabStatus = CabStatus.STATUS_INITIATING // default
        private set

    var titleText: CharSequence = ""
    @ColorInt
    var titleTextColor: Int = Color.WHITE

    var subtitleText: CharSequence = ""
    @ColorInt
    var subtitleTextColor: Int = titleTextColor

    @StyleRes
    var themeRes: Int = R.style.ThemeOverlay_AppCompat_DayNight_ActionBar

    /** use [closeDrawableColor] to set color first **/
    var closeDrawable: Drawable =
        ContextCompat.getDrawable(activity, R.drawable.ic_close_white_24dp)!!.let {
            if (autoTintMenuIcon) it.setTint(closeDrawableColor)
            it
        }

    @ColorInt
    var closeDrawableColor: Int = titleTextColor

    @ColorInt
    var backgroundColor: Int = Color.GRAY

    @MenuRes
    var menuRes: Int = 0

    val menu: Menu? get() = toolbar.menu

    var autoTintMenuIcon = true

    private var menuClickListener = Toolbar.OnMenuItemClickListener { item ->
        selectCallbacks.forEach {
            return@OnMenuItemClickListener it.invoke(item)
        }
        return@OnMenuItemClickListener false
    }

    private var closeClickListener = View.OnClickListener { _ ->
        closeCallbacks.forEach {
            return@OnClickListener it.invoke(this)
        }
        return@OnClickListener
    }
    /** call this function in cab's host OnDestroy **/
    @Synchronized
    fun destroy(): Boolean {
        if (status == CabStatus.STATUS_DESTROYED) return false
        status = CabStatus.STATUS_DESTROYING

        destroyCallbacks.forEach {
            if (!it.invoke(this)) {
                status = CabStatus.STATUS_AVAILABLE
                return false
            }
        }
        toolbar.visibility = View.GONE
        toolbarField = null
        activityField = null

        status = CabStatus.STATUS_DESTROYED
        return true
    }

    private var initCallback = mutableListOf<InitCallback>()
    private var showCallbacks = mutableListOf<ShowCallback>()
    private var selectCallbacks = mutableListOf<SelectCallback>()
    private var closeCallbacks = mutableListOf<CloseCallback>()
    private var hideCallbacks = mutableListOf<HideCallback>()
    private var destroyCallbacks = mutableListOf<DestroyCallback>()

    fun onInit(callback: InitCallback?) { callback?.let { initCallback.add(callback) } }
    fun onShow(callback: ShowCallback?) { callback?.let { showCallbacks.add(callback) } }
    fun onSelection(callback: SelectCallback?) { callback?.let { selectCallbacks.add(callback) } }
    fun onClose(callback: HideCallback?) { callback?.let { closeCallbacks.add(callback) } }
    fun onHide(callback: HideCallback?) { callback?.let { hideCallbacks.add(callback) } }
    fun onDestroy(callback: DestroyCallback?) { callback?.let { destroyCallbacks.add(callback) } }
}
typealias InitCallback = (cab: MultiSelectionCab) -> Unit
typealias ShowCallback = (cab: MultiSelectionCab, menu: Menu) -> Unit
typealias SelectCallback = (item: MenuItem) -> Boolean
typealias CloseCallback = (cab: MultiSelectionCab) -> Unit
typealias HideCallback = (cab: MultiSelectionCab) -> Unit
typealias DestroyCallback = (cab: MultiSelectionCab) -> Boolean

typealias CabCfg = MultiSelectionCab.() -> Unit

@Suppress("ClassName")
sealed class CabStatus { // todo
    object STATUS_INITIATING : CabStatus()
    object STATUS_AVAILABLE : CabStatus()
    object STATUS_ACTIVE : CabStatus()
    object STATUS_DESTROYING : CabStatus()
    object STATUS_DESTROYED : CabStatus()
}
