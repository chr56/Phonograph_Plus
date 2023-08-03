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
 * @param linkedActivity produce an activity that [updateCab] requires
 * @param multiSelectMenuHandler creates menu
 * @param I selectable item type
 */
class MultiSelectionController<I>(
    private val linkedAdapter: IMultiSelectableAdapter<I>,
    val cabController: MultiSelectionCabController?,
    val linkedActivity: () -> ComponentActivity?,
    multiSelectMenuHandler: ((Toolbar) -> Boolean)?,
) {
    val selected: MultiSelectionBin<I> = MultiSelectionBin(mutableListOf())

    init {
        cabController?.onDismiss = ::unselectedAll
        cabController?.menuHandler = multiSelectMenuHandler
    }

    fun toggle(datasetPosition: Int): Boolean {
        val item = linkedAdapter.getItem(datasetPosition) ?: return false
        selected.toggle(item)
        linkedAdapter.notifyItemChanged(datasetPosition)
        updateCab()
        return true
    }

    val isInQuickSelectMode: Boolean
        get() = cabController?.cab != null && cabController.cab.status == STATUS_ACTIVE


    fun selectAll() {
        selected.clearAll()
        for (i in 0 until linkedAdapter.getItemCount() ) {
            val item = linkedAdapter.getItem(i)
            if (item != null) {
                selected.add(item)
            }
        }
        linkedAdapter.notifyDataSetChanged()
        updateCab()
    }

    fun unselectedAll() {
        selected.clearAll()
        linkedAdapter.notifyDataSetChanged()
        updateCab()
    }

    fun isSelected(item: I): Boolean = selected.contains(item)

    private var onBackPressedDispatcherRegistered = false
    private fun updateCab() {

        cabController?.showContent(selected.size)//todo: context

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
    }

}

/**
 * contains selected items
 * @param I selectable item type
 */
class MultiSelectionBin<I>(
    private val checkedList: MutableList<I>,
) : Collection<I> by checkedList {

    fun toggle(item: I): Boolean =
        if (!checkedList.remove(item)) checkedList.add(item) else true

    fun clearAll() = checkedList.clear()

    fun add(item: I) = checkedList.add(item)

    fun remove(item: I) = checkedList.remove(item)

}