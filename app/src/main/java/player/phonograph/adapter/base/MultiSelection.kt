/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.adapter.base

import lib.phonograph.cab.ToolbarCab.Companion.STATUS_ACTIVE
import player.phonograph.util.debug
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.appcompat.widget.Toolbar
import android.util.Log

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
 * @param cabController [MultiSelectionCabController] that is interacted with
 * @param multiSelectMenuHandler creates menu
 * @param I selectable item type
 */
class MultiSelectionController<I>(
    private val linkedAdapter: IMultiSelectableAdapter<I>,
    val cabController: MultiSelectionCabController?,
    private val multiSelectMenuHandler: ((Toolbar) -> Boolean)?,
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
        get() = cabController?.cab != null && cabController.cab.status == STATUS_ACTIVE


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

    fun isSelected(item: I): Boolean = _selected.contains(item)

    private var onBackPressedDispatcherRegistered = false
    private fun updateCab() {

        cabController?.showContent(_selected.size)//todo: context

        if (!onBackPressedDispatcherRegistered && cabController != null) {
            onBackPressedDispatcherRegistered = true
            val activity = cabController.cab.activity as? ComponentActivity
            activity?.onBackPressedDispatcher?.addCallback {
                cabController.dismiss()
                debug {
                    Log.v("MultiSelectAdapterCallback", "isInQuickSelectMode: $isInQuickSelectMode")
                }
                isEnabled = isInQuickSelectMode
            }
            debug {
                Log.v("onBackPressedDispatcher", "onBackPressedDispatcher Callback registered")
            }
        }

        cabController?.onDismiss = ::unselectedAll
        cabController?.menuHandler = multiSelectMenuHandler
    }

}