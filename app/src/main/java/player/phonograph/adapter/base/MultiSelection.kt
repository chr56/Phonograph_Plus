/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.adapter.base

import lib.phonograph.cab.CabStatus
import player.phonograph.util.debug
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.appcompat.widget.Toolbar
import android.util.Log


interface MultiSelectionAdapterContract<I> {
    fun getItems(): Iterable<I>
    fun getItem(datasetPosition: Int): I
    fun notifyItemChanged(datasetPosition: Int)
    fun notifyDataSetChanged()
}

@Suppress("MemberVisibilityCanBePrivate")
class MultiSelectionController<I>(
    val linkedAdapter: MultiSelectionAdapterContract<I>,
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
        get() = cabController?.cab != null && cabController.cab.status == CabStatus.STATUS_ACTIVE


    fun selectAll() {
        selected.clearAll()
        for (item in linkedAdapter.getItems()) {
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
    }

    fun isSelected(item: I): Boolean = selected.contains(item)


    private var onBackPressedDispatcherRegistered = false
    private fun updateCab() {
        val activity = linkedActivity() ?: return

        cabController?.showContent(activity, selected.size)//todo: context

        if (!onBackPressedDispatcherRegistered && cabController != null) {
            onBackPressedDispatcherRegistered = true
            activity.onBackPressedDispatcher.addCallback {
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

class MultiSelectionBin<I>(
    private val checkedList: MutableList<I>,
) : Collection<I> by checkedList {

    fun toggle(item: I): Boolean =
        if (!checkedList.remove(item)) checkedList.add(item) else true

    fun clearAll() = checkedList.clear()

    fun add(item: I) = checkedList.add(item)

    fun remove(item: I) = checkedList.remove(item)

}