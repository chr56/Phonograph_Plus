/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.adapter

import lib.phonograph.cab.ToolbarCab
import lib.phonograph.cab.ToolbarCab.Companion.STATUS_ACTIVE
import lib.phonograph.cab.initToolbarCab
import player.phonograph.R
import player.phonograph.mechanism.actions.MultiSelectionToolbarMenuProviders
import player.phonograph.model.IPaletteColorProvider
import player.phonograph.util.debug
import player.phonograph.util.reportError
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.primaryColor
import util.theme.color.darkenColor
import util.theme.color.isColorLight
import util.theme.color.lightenColor
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
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
        lastSelectedPosition = datasetPosition
        return true
    }

    private var lastSelectedPosition = -1
    fun rangeTo(datasetPosition: Int): Boolean {
        if (lastSelectedPosition > 0 && datasetPosition != lastSelectedPosition) {
            val range =
                if (datasetPosition < lastSelectedPosition) IntRange(datasetPosition, lastSelectedPosition)
                else IntRange(lastSelectedPosition, datasetPosition)
            for (i in range) {
                val item = linkedAdapter.getItem(i)
                if (item != null && !_selected.contains(item)) {
                    _selected.add(item)
                }
            }
            linkedAdapter.notifyDataSetChanged()
            updateCab()
            return true
        } else {
            return false
        }
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

        val size = _selected.size
        val currentCab = cab

        if (currentCab != null) {
            if (size > 0) {
                currentCab.titleText = currentCab.toolbar.resources.getString(R.string.x_selected, size)
                currentCab.setupMenu()
                currentCab.show()
            } else {
                currentCab.hide()
            }

            if (!onBackPressedDispatcherRegistered) {
                onBackPressedDispatcherRegistered = true
                activity.onBackPressedDispatcher.addCallback(activity, backPressedCallback)
                debug {
                    Log.v(TAG, "onBackPressedDispatcher Callback registered")
                }
            }
        }

    }

    val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            cab?.hide()
            unselectedAll()
            remove()
            onBackPressedDispatcherRegistered = false
        }
    }


    @get:ColorInt
    val cabColor: Int
        get() {
            var color =
                (activity as? IPaletteColorProvider)?.paletteColor?.value ?: activity.primaryColor()
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
                        reportError(e, TAG, "Failed to create cab")
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
        hide()
    }

    private fun ToolbarCab.setupMenu() {
        menuHandler = {
            MultiSelectionToolbarMenuProviders.inflate(it.menu, activity, this@MultiSelectionController)
        }
    }

    fun registerClicking(itemView: View, bindingAdapterPosition: Int, normalClick: () -> Boolean) {
        registerOnClickListener(itemView, bindingAdapterPosition, normalClick)
        registerOnLongClickListener(itemView, bindingAdapterPosition)
    }

    fun registerOnClickListener(itemView: View, bindingAdapterPosition: Int, normalClick: () -> Boolean) {
        itemView.setOnClickListener {
            if (this.isInQuickSelectMode) {
                this.toggle(bindingAdapterPosition)
            } else {
                normalClick()
            }
        }
    }

    fun registerOnLongClickListener(itemView: View, bindingAdapterPosition: Int) {
        itemView.setOnLongClickListener {
            when (this.isInQuickSelectMode) {
                false -> this.toggle(bindingAdapterPosition)
                true  -> this.rangeTo(bindingAdapterPosition)
            }
            true
        }
    }

}

private const val TAG = "MultiSelectionController"