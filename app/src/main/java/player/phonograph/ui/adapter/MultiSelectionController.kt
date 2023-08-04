/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.adapter

import lib.phonograph.cab.ToolbarCab
import lib.phonograph.cab.ToolbarCab.Companion.STATUS_ACTIVE
import lib.phonograph.cab.initToolbarCab
import mt.pref.ThemeColor
import mt.util.color.darkenColor
import mt.util.color.isColorLight
import mt.util.color.lightenColor
import player.phonograph.R
import player.phonograph.actions.menu.multiItemsToolbar
import player.phonograph.misc.IPaletteColorProvider
import player.phonograph.util.debug
import player.phonograph.util.theme.getTintedDrawable
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.annotation.ColorInt
import android.graphics.Color
import android.util.Log
import android.view.View

/**
 * indicate a multi-selectable adapter
 * @param I selectable item type
 */
interface IMultiSelectableAdapter<I> {
    fun getItemCount(): Int
    fun getItem(datasetPosition: Int): I
    fun notifyItemChanged(datasetPosition: Int)
    fun notifyDataSetChanged()
}

/**
 * @param linkedAdapter adapter applied
 * @param activity Activity hosts [ToolbarCab], must have ViewStub with [R.id.cab_stub]
 * @param I selectable item type
 */
class MultiSelectionController<I>(
    private val linkedAdapter: IMultiSelectableAdapter<I>,
    private val activity: ComponentActivity,
    private val enable: Boolean,
) {
    private val _selected: MutableList<I> = mutableListOf()
    val selected: List<I> get() = _selected.toList()

    fun toggle(datasetPosition: Int): Boolean {
        val item = linkedAdapter.getItem(datasetPosition) ?: return false
        if (!_selected.remove(item)) _selected.add(item)
        linkedAdapter.notifyItemChanged(datasetPosition)
        updateCab()
        return true
    }

    val isInQuickSelectMode: Boolean
        get() = enable && cab != null && cab?.status == STATUS_ACTIVE

    fun selectAll() {
        _selected.clear()
        for (i in 0 until linkedAdapter.getItemCount()) {
            val item = linkedAdapter.getItem(i)
            if (item != null) {
                _selected.add(item)
            }
        }
        linkedAdapter.notifyDataSetChanged()
        updateCab()
    }

    fun unselectedAll() {
        _selected.clear()
        linkedAdapter.notifyDataSetChanged()
        updateCab()
    }

    fun invertSelected() {
        val previousSelected = _selected.toMutableList() //copy
        _selected.clear()
        for (i in 0 until linkedAdapter.getItemCount()) {
            val item = linkedAdapter.getItem(i)
            if (item != null && item !in previousSelected) {
                _selected.add(item)
            }
        }
        linkedAdapter.notifyDataSetChanged()
        updateCab()
    }

    fun isSelected(item: I): Boolean = _selected.contains(item)

    private var onBackPressedDispatcherRegistered = false
    private fun updateCab() {

        updateCab(_selected.size)

        if (!onBackPressedDispatcherRegistered && cab != null) {
            onBackPressedDispatcherRegistered = true
            val activity = activity as? ComponentActivity
            activity?.onBackPressedDispatcher?.addCallback {
                cab?.hide()
                unselectedAll()
                debug {
                    Log.v("MultiSelectAdapterCallback", "isInQuickSelectMode: $isInQuickSelectMode")
                }
                isEnabled = isInQuickSelectMode
            }
            debug {
                Log.v("onBackPressedDispatcher", "onBackPressedDispatcher Callback registered")
            }
        }
    }


    @get:ColorInt
    val cabColor: Int
        get() {
            var color =
                (activity as? IPaletteColorProvider)?.paletteColor?.value ?: ThemeColor.primaryColor(activity)
            if (isColorLight(color)) {
                // light to dark
                for (it in 0 until 3) {
                    color = darkenColor(color)
                }
            } else {
                // dark to light
                for (it in 0 until 3) {
                    color = lightenColor(color)
                }
            }
            return color
        }

    @get:ColorInt
    val textColor: Int
        get() = if (isColorLight(cabColor)) Color.BLACK else Color.WHITE

    private var _cab: ToolbarCab? = null
    val cab: ToolbarCab?
        get() {
            return if (!enable) null
            else {
                val targetId = R.id.cab_stub
                val inflatedId = R.id.multi_selection_cab
                if (_cab != null) {
                    _cab
                } else {
                    _cab = try {
                        initToolbarCab(activity, targetId, inflatedId).apply { prepare() }
                    } catch (e: IllegalStateException) {
                        Log.e("Cab", "Failed to create cab", e)
                        null
                    }
                    _cab
                }
            }
        }

    private fun ToolbarCab.prepare() {
        titleText = toolbar.resources.getString(R.string.x_selected, 0)

        titleTextColor = textColor
        backgroundColor = cabColor

        navigationIcon = activity.getTintedDrawable(R.drawable.ic_close_white_24dp, textColor)!!

        setupMenu()
        closeClickListener = View.OnClickListener {
            hide()
            unselectedAll()
        }
    }

    private fun ToolbarCab.setupMenu() {
        menuHandler = {
            multiItemsToolbar(it.menu, activity, this@MultiSelectionController)
        }
    }
    /**
     * @param size selected size
     */
    fun updateCab(size: Int) {
        updateCountText(size)
        cab?.let { cab ->
            if (size > 0) {
                cab.setupMenu()
                cab.show()
            } else {
                cab.hide()
            }
        }
    }

    /**
     * @param size selected size
     */
    private fun updateCountText(size: Int) {
        cab?.let { cab ->
            cab.titleText = cab.toolbar.resources.getString(R.string.x_selected, size)
        }
    }

}